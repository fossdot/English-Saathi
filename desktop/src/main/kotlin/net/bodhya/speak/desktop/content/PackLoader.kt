package net.bodhya.speak.desktop.content

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import net.bodhya.speak.domain.HintTemplate
import net.bodhya.speak.domain.Hints
import net.bodhya.speak.domain.Lesson

/**
 * A lesson pack: lessons that can be added to the app without a new
 * version of it (docs/decisions.md, "Lessons are kept separate from the
 * app"). [id] stays the same across versions; a higher [version] replaces a
 * lower one, never the other way round. Packs are placed in the unlock
 * order by [order], then [id]. Its lessons' and sentences' ids carry the
 * pack's id ([PackLoader.qualified]).
 */
data class LessonPack(
    val id: String,
    val version: Int,
    val order: Int,
    val titleEn: String,
    val titleHi: String,
    val lessons: List<Lesson>,
    /** Tip templates for this pack's lessons: the app's, with the pack's own over them. */
    val hints: Hints,
    /** The pack's own tip templates, for packs that build on this one. */
    val templates: List<HintTemplate>,
) {
    val sentenceCount: Int get() = lessons.sumOf { it.sentences.size }
}

/** A loaded pack, and what the checks flagged in it (it still works, but someone should look). */
data class LoadedPack(val pack: LessonPack, val warnings: List<String>)

/** A pack written in a newer format than this version of the app reads. */
class NewerFormatException(message: String, val format: Int) : Exception(message)

/**
 * Reads lesson packs, format 1: one UTF-8 JSON file,
 *
 * ```
 * { "format": 1, "id": "starter", "version": 1, "order": 0,
 *   "title_en": "First lessons", "title_hi": "पहले पाठ",
 *   "hints": { "v_w": { "text_hi": "…", "reviewed": false } },
 *   "lessons": [ { …a lesson, as LessonLoader reads it… } ] }
 * ```
 *
 * Every lesson goes through [LessonLoader]'s checks. On top of those, a
 * pack is unusable ([ContentException]) when a field is missing or wrong,
 * when it has no lessons, when lesson or sentence ids repeat in it (ignoring
 * case: a Mac's disk does) or aren't safe as file names (a sentence id
 * names its recordings), or when lists and objects nest deeper than any
 * lesson needs. An unknown field at the top of the pack is only a warning,
 * so a later format can add one. A pack in a later [FORMAT] throws
 * [NewerFormatException].
 *
 * The app keeps every lesson and sentence id with the pack's id in front
 * ([qualified]: `starter.bus_01`), so two packs can never clash
 * (docs/decisions.md, "Lesson review: owner answers").
 */
object PackLoader {
    /** The pack format this version of the app reads. */
    const val FORMAT = 1

    /** Pack ids are file names in the data folder. */
    val PACK_ID = Regex("[a-z0-9][a-z0-9_-]{0,39}")

    /** Lesson and sentence ids as a pack writes them: sentence ids name recordings (`recordings/<sentence>.wav`). */
    val ITEM_ID = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")

    /** How deep lists and objects may nest: a lesson needs 6. Much deeper is not a lesson pack, and could overflow the JSON reader. */
    const val MAX_DEPTH = 32

    private val PACK_KEYS = setOf("format", "id", "version", "order", "title_en", "title_hi", "hints", "lessons")

    /** The id the app keeps for lesson or sentence [id] of pack [pack]: `starter.bus_01`. Neither part has a dot. */
    fun qualified(pack: String, id: String) = "$pack.$id"

    /**
     * Loads the pack in [text] (from the file [name], for messages). Its tips
     * fall back to [base], the templates of the packs that come with the app;
     * together they must have the generic tip.
     */
    fun load(name: String, text: String, base: List<HintTemplate>): LoadedPack {
        val warnings = mutableListOf<String>()
        checkDepth(name, text)
        val root = try {
            Json.parseToJsonElement(text) as? JsonObject ?: throw ContentException("$name: the file must be a JSON object")
        } catch (e: SerializationException) {
            throw ContentException("$name: not valid JSON (${e.message?.lineSequence()?.firstOrNull()})", e)
        }
        val format = root.int(name, "format")
        if (format > FORMAT) throw NewerFormatException("$name: lesson pack format $format; this version of English Saathi reads format $FORMAT", format)
        if (format < 1) throw ContentException("$name: \"format\" must be $FORMAT")
        for (key in root.keys - PACK_KEYS) warnings += "unknown field \"$key\" is ignored"

        val id = root.text(name, "id")
        if (!PACK_ID.matches(id)) throw ContentException("$name: pack id \"$id\" must be lower-case letters, digits, - or _ (up to 40)")
        val version = root.int(name, "version")
        if (version < 1) throw ContentException("$name: \"version\" must be 1 or more")
        val order = root.int(name, "order")
        if (order < 0) throw ContentException("$name: \"order\" must be 0 or more")
        val titleEn = root.text(name, "title_en").also { if (it.isBlank()) throw ContentException("$name: \"title_en\" is empty") }
        val titleHi = root.text(name, "title_hi").also { if (it.isBlank()) throw ContentException("$name: \"title_hi\" is empty") }

        val own = when (val h = root["hints"]) {
            null, is JsonNull -> emptyList()
            is JsonObject -> LessonLoader.loadHintTemplates("$name hints", h.toString())
            else -> throw ContentException("$name: \"hints\" must be an object")
        }
        val merged = (base + own).associateBy { it.tag }.values
        val hints = try {
            Hints(merged)
        } catch (e: IllegalArgumentException) {
            throw ContentException("$name: there is no \"generic\" tip, which every word without its own tip needs", e)
        }

        val items = root["lessons"] as? JsonArray ?: throw ContentException("$name: \"lessons\" must be a list")
        if (items.isEmpty()) throw ContentException("$name: the pack has no lessons")
        val lessons = items.mapIndexed { i, item ->
            val loaded = LessonLoader.loadLesson("$name lesson ${i + 1}", item.toString(), hints)
            warnings += loaded.warnings
            loaded.lesson
        }
        checkIds(name, lessons)
        val kept = lessons.map { lesson ->
            lesson.copy(id = qualified(id, lesson.id), sentences = lesson.sentences.map { it.copy(id = qualified(id, it.id)) })
        }
        return LoadedPack(LessonPack(id, version, order, titleEn, titleHi, kept, hints, own), warnings.map { if (it.startsWith(name)) it else "$name: $it" })
    }

    /** Refuses lists and objects nested deeper than [MAX_DEPTH], before the JSON reader recurses into them. */
    private fun checkDepth(name: String, text: String) {
        var depth = 0
        var inString = false
        var escaped = false
        for (c in text) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{', '[' -> if (++depth > MAX_DEPTH) throw ContentException("$name: lists and objects nest more than $MAX_DEPTH deep")
                    '}', ']' -> depth--
                }
            }
        }
    }

    private fun checkIds(name: String, lessons: List<Lesson>) {
        val lessonIds = mutableSetOf<String>()
        val sentenceIds = mutableSetOf<String>()
        for (lesson in lessons) {
            if (!ITEM_ID.matches(lesson.id)) throw ContentException("$name: lesson id \"${lesson.id}\" must be letters, digits, - or _")
            if (!lessonIds.add(lesson.id.lowercase())) throw ContentException("$name: lesson id \"${lesson.id}\" is used twice")
            for (s in lesson.sentences) {
                if (!ITEM_ID.matches(s.id)) throw ContentException("$name: sentence id \"${s.id}\" must be letters, digits, - or _")
                if (!sentenceIds.add(s.id.lowercase())) throw ContentException("$name: sentence id \"${s.id}\" is used twice")
            }
        }
    }

    private fun JsonObject.int(name: String, key: String): Int {
        val v = this[key] as? JsonPrimitive ?: throw ContentException("$name: missing \"$key\"")
        if (v.isString) throw ContentException("$name: \"$key\" must be a whole number")
        return v.intOrNull ?: throw ContentException("$name: \"$key\" must be a whole number")
    }

    private fun JsonObject.text(name: String, key: String): String {
        val v = this[key] as? JsonPrimitive ?: throw ContentException("$name: missing \"$key\"")
        if (!v.isString) throw ContentException("$name: \"$key\" must be text")
        return v.content
    }
}
