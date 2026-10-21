package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.data.SafeFiles
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.HintTemplate
import net.bodhya.speak.domain.Hints
import net.bodhya.speak.domain.Lesson
import java.io.File
import java.io.IOException

/** A pack in use: from inside the app, or added in Settings (a file in the data folder). */
data class PackInUse(val pack: LessonPack, val builtIn: Boolean, val file: File)

/** One lesson, with the pack it came from (its tips come from the pack). */
data class LessonEntry(val lesson: Lesson, val pack: LessonPack) {
    val hints: Hints get() = pack.hints
}

/** What an added pack would change, for the facilitator to confirm (in English and Hindi). */
data class PackPreview(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val version: Int,
    /** The version in use now, when this pack updates one. */
    val replaces: Int?,
    val lessons: List<LessonLine>,
    /** Lessons the version in use has and this one doesn't (progress for them is kept). */
    val removed: List<LessonLine>,
    /** What the checks flagged (written to the log); the pack still works. */
    val warnings: List<String>,
)

/** A lesson in a preview: titles, sentences, and whether it is new or an update of one in use. */
data class LessonLine(val titleEn: String, val titleHi: String, val sentences: Int, val new: Boolean)

/** The result of checking a pack file before adding it. */
sealed interface PackCheck {
    /** It can't be added; nothing was changed. */
    data class Rejected(val message: Message) : PackCheck

    /** It can be added; [preview] says what it changes. */
    data class Ready(val preview: PackPreview, val pack: LessonPack, val text: String) : PackCheck
}

/**
 * The lessons the app can use (docs/decisions.md, "Lessons are kept
 * separate from the app"): the packs inside the app ([builtInDir]) and the
 * packs added in Settings, kept in the data folder as `lessons/<pack id>.json`.
 *
 * For each pack id the highest version is used; on a tie the app's own
 * copy. The packs that come with the app go first in the unlock order (the
 * first lesson pack stays first), then the added ones by their `order`, then
 * id. A pack whose
 * lesson or sentence ids another pack already uses is left out and
 * reported, never a pack that comes with the app; so is an added file that
 * can't be read. Nothing is ever written inside the app: an app update
 * replaces [builtInDir] and leaves the data folder alone.
 */
class LessonLibrary private constructor(
    val packs: List<PackInUse>,
    /** Added packs that were left out, and why: shown to the facilitator. */
    val problems: List<String>,
    /** What the checks flagged in the packs in use: written to the log. */
    val warnings: List<String>,
    private val builtInDir: File,
    private val addedDir: File,
    private val builtInTemplates: List<HintTemplate>,
    private val config: GameConfig,
) {
    /** Every lesson, in the unlock order. */
    val lessons: List<LessonEntry> = packs.flatMap { p -> p.pack.lessons.map { LessonEntry(it, p.pack) } }

    private val byLesson = lessons.associateBy { it.lesson.id }
    private val bySentence = lessons.flatMap { e -> e.lesson.sentences.map { it.id to e } }.toMap()

    fun lesson(id: String): LessonEntry? = byLesson[id]

    /** The lesson a sentence belongs to. */
    fun lessonOf(sentenceId: String): LessonEntry? = bySentence[sentenceId]

    val sentenceCount: Int get() = lessons.sumOf { it.lesson.sentences.size }

    /**
     * Checks [file] as a pack to add: it must load, be in a format this app
     * reads, be newer than any version in use, and use no lesson or sentence
     * id another pack has. Changes nothing.
     */
    fun check(file: File): PackCheck {
        val size = file.length()
        if (size > MAX_PACK_BYTES) {
            return PackCheck.Rejected(rejection("This file is too big to be a lesson pack (${size / 1_000_000} MB).", "यह फ़ाइल पाठों का पैक होने के लिए बहुत बड़ी है।"))
        }
        val text = try {
            file.readText()
        } catch (e: IOException) {
            return PackCheck.Rejected(rejection("The file couldn't be read: ${e.message ?: e}", "फ़ाइल पढ़ी नहीं जा सकी।"))
        }
        val loaded = try {
            PackLoader.load(file.name, text, builtInTemplates, config)
        } catch (e: NewerFormatException) {
            return PackCheck.Rejected(rejection(
                "This lesson pack needs a newer version of English Saathi. Install the newest English Saathi first.",
                "इस पैक के लिए English Saathi का नया संस्करण चाहिए। पहले नया English Saathi लगाइए।",
            ))
        } catch (e: ContentException) {
            return PackCheck.Rejected(rejection(
                "This file isn't a lesson pack English Saathi can use: ${e.message}",
                "यह फ़ाइल पाठों का ऐसा पैक नहीं है जिसे English Saathi इस्तेमाल कर सके।",
            ))
        }
        val pack = loaded.pack
        val inUse = packs.firstOrNull { it.pack.id == pack.id }
        if (inUse != null && pack.version == inUse.pack.version) {
            return PackCheck.Rejected(rejection(
                "This pack is already installed (“${pack.titleEn}”, version ${pack.version}).",
                "यह पैक (“${pack.titleHi}”) पहले से लगा है।",
            ))
        }
        if (inUse != null && pack.version < inUse.pack.version) {
            return PackCheck.Rejected(rejection(
                "This pack is older (version ${pack.version}) than the one in use (version ${inUse.pack.version}).",
                "यह पैक पुराना है (संस्करण ${pack.version}); इससे नया (संस्करण ${inUse.pack.version}) पहले से लगा है।",
            ))
        }
        conflict(pack, packs.filter { it.pack.id != pack.id })?.let { (what, other) ->
            return PackCheck.Rejected(rejection(
                "$what is already in the pack “${other.titleEn}”, so this pack can't be added. Ask whoever made it for a corrected copy.",
                "इस पैक का कुछ हिस्सा पहले से “${other.titleHi}” में है, इसलिए यह नहीं जुड़ सकता।",
            ))
        }
        val oldIds = inUse?.pack?.lessons?.map { it.id }?.toSet().orEmpty()
        val newIds = pack.lessons.map { it.id }.toSet()
        val preview = PackPreview(
            pack.id, pack.titleEn, pack.titleHi, pack.version, inUse?.pack?.version,
            pack.lessons.map { LessonLine(it.titleEn, it.titleHi, it.sentences.size, it.id !in oldIds) },
            inUse?.pack?.lessons.orEmpty().filter { it.id !in newIds }.map { LessonLine(it.titleEn, it.titleHi, it.sentences.size, new = false) },
            loaded.warnings,
        )
        return PackCheck.Ready(preview, pack, text)
    }

    /**
     * Adds a pack [check] found ready: its file goes into the data folder
     * (written safely) and the lessons are read again. Returns the new library.
     */
    fun install(ready: PackCheck.Ready): LessonLibrary {
        SafeFiles.write(File(addedDir, "${ready.pack.id}.json"), ready.text)
        return load(builtInDir, addedDir.parentFile, config)
    }

    companion object {
        /** Far more than 100 text lessons (about 300 KB); anything bigger is not a lesson pack. */
        const val MAX_PACK_BYTES = 20_000_000L

        /** Where added packs live in the data folder. */
        fun addedDir(dataDir: File) = File(dataDir, "lessons")

        /**
         * Reads the packs inside the app ([builtInDir]: every `.json`) and the
         * packs added to [dataDir]. A pack that comes with the app must load
         * (it is part of the app; otherwise this throws); an added one that
         * doesn't is reported in [problems] and left out.
         */
        fun load(builtInDir: File, dataDir: File, config: GameConfig): LessonLibrary {
            val warnings = mutableListOf<String>()
            val problems = mutableListOf<String>()
            val builtIn = mutableListOf<PackInUse>()
            val templates = mutableListOf<HintTemplate>()
            for (file in builtInDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }.orEmpty().sortedBy { it.name }) {
                val loaded = PackLoader.load(file.name, file.readText(), templates.toList(), config)
                warnings += loaded.warnings
                templates += loaded.pack.templates
                builtIn += PackInUse(loaded.pack, builtIn = true, file)
            }
            if (builtIn.isEmpty()) throw ContentException("no lesson packs in $builtInDir")

            val addedDir = addedDir(dataDir)
            val added = addedDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }.orEmpty().sortedBy { it.name }.mapNotNull { file ->
                try {
                    val loaded = PackLoader.load(file.name, file.readText(), templates, config)
                    warnings += loaded.warnings
                    PackInUse(loaded.pack, builtIn = false, file)
                } catch (e: Exception) {
                    problems += "the added lesson pack ${file.name} could not be read, so it is left out: ${e.message ?: e}"
                    null
                }
            }

            // For each pack id, the highest version; on a tie, the app's own copy.
            val chosen = (builtIn + added).groupBy { it.pack.id }.values.map { same ->
                same.maxWith(compareBy<PackInUse> { it.pack.version }.thenBy { it.builtIn })
            }
            // The app's packs first, so a clash can only ever leave out an added pack.
            val accepted = mutableListOf<PackInUse>()
            for (p in chosen.sortedWith(compareByDescending<PackInUse> { it.builtIn }.thenBy { it.pack.order }.thenBy { it.pack.id })) {
                val clash = conflict(p.pack, accepted)
                if (clash == null) {
                    accepted += p
                } else {
                    problems += "the lesson pack “${p.pack.titleEn}” is left out: ${clash.first.replaceFirstChar { it.lowercase() }} is already in “${clash.second.titleEn}”"
                }
            }
            // The app's packs (also a newer added copy of one) first, then the added ones.
            val appIds = builtIn.map { it.pack.id }.toSet()
            val inOrder = accepted.sortedWith(compareByDescending<PackInUse> { it.pack.id in appIds }.thenBy { it.pack.order }.thenBy { it.pack.id })
            return LessonLibrary(inOrder, problems, warnings, builtInDir, addedDir, templates, config)
        }

        /** The first lesson or sentence id of [pack] that one of [others] already uses (ignoring case, as a Mac's disk does). */
        private fun conflict(pack: LessonPack, others: List<PackInUse>): Pair<String, LessonPack>? {
            for (other in others) {
                val lessonIds = other.pack.lessons.map { it.id.lowercase() }.toSet()
                val sentenceIds = other.pack.lessons.flatMap { l -> l.sentences.map { it.id.lowercase() } }.toSet()
                for (lesson in pack.lessons) {
                    if (lesson.id.lowercase() in lessonIds) return "The lesson “${lesson.titleEn}” (${lesson.id})" to other.pack
                    for (s in lesson.sentences) {
                        if (s.id.lowercase() in sentenceIds) return "The sentence “${s.text}” (${s.id})" to other.pack
                    }
                }
            }
            return null
        }

        /** Every rejection ends the same way: the facilitator should know nothing changed. */
        private fun rejection(en: String, hi: String) = Message("$en Nothing was changed.", "$hi कुछ नहीं बदला।")
    }
}
