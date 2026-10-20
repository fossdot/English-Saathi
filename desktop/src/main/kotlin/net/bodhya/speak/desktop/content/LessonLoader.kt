package net.bodhya.speak.desktop.content

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.HintTemplate
import net.bodhya.speak.domain.Hints
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.Sentence
import net.bodhya.speak.domain.WordInfo

/** A lesson or hint file that can't be used. [message] names the file and the problem. */
class ContentException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A loaded lesson, and what the checks flagged in it (it still works, but someone should look). */
data class LoadedLesson(val lesson: Lesson, val warnings: List<String>)

/**
 * Reads lesson files in the format agreed in docs/decisions.md ("Word data
 * scope"): one word table per lesson, keyed by normalized word, and
 * sentences that may turn scoring off for a word (`"unscored"`).
 *
 * Errors make the file unusable ([ContentException]): broken JSON, a missing
 * or wrong field, what the domain rejects (no sentences, repeated sentence
 * ids, a sentence without English words, word keys that aren't normalized
 * words), and a sentence with nothing left to score. Everything else
 * docs/decisions.md asks the loader to flag is a warning
 * ([LoadedLesson.warnings]).
 */
object LessonLoader {

    private val WORD_KEYS = setOf("syllables", "hint_hi", "hint_tag", "accept", "score", "reviewed")
    private val SENTENCE_KEYS = setOf("id", "text", "hi", "chunks", "picture", "unscored")
    private val LESSON_KEYS = setOf("id", "title_en", "title_hi", "words", "sentences")

    /**
     * Loads the lesson in [text] (from the file [name], for messages). [hints]
     * and [config] are checked against it.
     */
    fun loadLesson(name: String, text: String, hints: Hints, config: GameConfig): LoadedLesson {
        val warnings = mutableListOf<String>()
        val lesson = parse(name) {
            val root = parseRoot(text)
            unknownKeys(root, LESSON_KEYS, "lesson", warnings)
            val words = root.obj("words").mapValues { (word, v) -> word(word, v.asObject("word \"$word\""), warnings) }
            val sentences = root.array("sentences").mapIndexed { i, v -> sentence(v.asObject("sentence ${i + 1}"), warnings) }
            Lesson(root.string("id"), root.string("title_en"), root.string("title_hi"), sentences, words)
                .also { check(it, hints, config, warnings) }
        }
        return LoadedLesson(lesson, warnings.map { "$name: $it" })
    }

    /** Loads the tip templates: `{ "tag": { "text_hi": "…", "reviewed": false } }`. */
    fun loadHints(name: String, text: String): Hints = parse(name) {
        Hints(
            parseRoot(text).map { (tag, v) ->
                val o = v.asObject("hint \"$tag\"")
                HintTemplate(tag, o.string("text_hi"), o.boolean("reviewed") ?: false)
            },
        )
    }

    private fun word(key: String, o: JsonObject, warnings: MutableList<String>): WordInfo {
        unknownKeys(o, WORD_KEYS, "word \"$key\"", warnings)
        return WordInfo(
            syllables = o.strings("syllables"),
            hintHi = o.stringOrNull("hint_hi"),
            hintTag = o.stringOrNull("hint_tag"),
            accept = o.stringsOrEmpty("accept"),
            score = o.boolean("score") ?: true,
        )
    }

    private fun sentence(o: JsonObject, warnings: MutableList<String>): Sentence {
        val id = o.string("id")
        unknownKeys(o, SENTENCE_KEYS, "sentence $id", warnings)
        return Sentence(
            id = id,
            text = o.string("text"),
            hi = o.string("hi"),
            chunks = o.strings("chunks"),
            picture = o.stringOrNull("picture"),
            unscored = o.stringsOrEmpty("unscored").toSet(),
        )
    }

    /** The checks docs/decisions.md lists under "Check content when it is loaded" and "Unscored words". */
    private fun check(lesson: Lesson, hints: Hints, config: GameConfig, warnings: MutableList<String>) {
        val used = lesson.sentences.flatMap { it.targetWords }.toSet()
        for ((word, info) in lesson.words) {
            for (entry in info.accept) {
                if (Normalizer.normalize(entry).size != 1) warnings += "word \"$word\": accept entry \"$entry\" is not one word, so it is ignored"
            }
            if (word !in used) warnings += "word \"$word\" is in no sentence"
            if (!info.score && info.accept.isNotEmpty()) warnings += "word \"$word\" is not scored, so its accept list has no effect"
            if (info.hintTag != null && hints.forTag(info.hintTag).tag != info.hintTag) {
                warnings += "word \"$word\": hint tag \"${info.hintTag}\" has no template, so it gets the generic tip"
            }
        }
        for (s in lesson.sentences) {
            for (word in s.targetWords.distinct()) {
                if (word !in lesson.words) warnings += "sentence ${s.id}: \"$word\" has no word entry, so its tip is generic with no sound hint"
            }
            for (word in s.unscored) {
                if (word !in s.targetWords) warnings += "sentence ${s.id}: unscored word \"$word\" is not in the sentence"
            }
            if (s.targetWords.all { it in lesson.unscoredIn(s) }) {
                throw ContentException("lesson ${lesson.id}: sentence ${s.id} has no scored word, so it can't be scored")
            }
        }
        if (config.dailyGoal > lesson.sentences.size) {
            warnings += "the daily goal (${config.dailyGoal}) is more than the ${lesson.sentences.size} sentences, so it can't be reached"
        }
    }

    // ---------- JSON helpers: every problem names the field ----------

    private fun <T> parse(name: String, block: () -> T): T = try {
        block()
    } catch (e: ContentException) {
        throw ContentException("$name: ${e.message}", e)
    } catch (e: SerializationException) {
        throw ContentException("$name: not valid JSON (${e.message?.lineSequence()?.firstOrNull()})", e)
    } catch (e: IllegalArgumentException) {
        // What the domain rejects: no sentences, repeated ids, unnormalized keys, …
        throw ContentException("$name: ${e.message}", e)
    }

    private fun parseRoot(text: String): JsonObject = Json.parseToJsonElement(text).asObject("the file")

    private fun unknownKeys(o: JsonObject, known: Set<String>, what: String, warnings: MutableList<String>) {
        for (key in o.keys - known) warnings += "$what: unknown field \"$key\" is ignored"
    }

    private fun JsonElement.asObject(what: String): JsonObject = this as? JsonObject ?: throw ContentException("$what must be an object")

    private fun JsonObject.field(key: String): JsonElement = this[key]?.takeUnless { it is JsonNull } ?: throw ContentException("missing \"$key\"")

    private fun JsonObject.obj(key: String): JsonObject = field(key).asObject("\"$key\"")

    private fun JsonObject.array(key: String): JsonArray = field(key) as? JsonArray ?: throw ContentException("\"$key\" must be a list")

    private fun JsonObject.string(key: String): String =
        (field(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: throw ContentException("\"$key\" must be text")

    private fun JsonObject.stringOrNull(key: String): String? = if (this[key] == null || this[key] is JsonNull) null else string(key)

    private fun JsonObject.strings(key: String): List<String> = array(key).map {
        (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content ?: throw ContentException("\"$key\" must be a list of text")
    }

    private fun JsonObject.stringsOrEmpty(key: String): List<String> = if (this[key] == null) emptyList() else strings(key)

    private fun JsonObject.boolean(key: String): Boolean? {
        val v = this[key] ?: return null
        return (v as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: throw ContentException("\"$key\" must be true or false")
    }
}
