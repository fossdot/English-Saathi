package net.bodhya.speak.desktop.data

import kotlinx.serialization.json.Json
import net.bodhya.speak.domain.LearnerProgress
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Why a name can't be used for a learner. */
enum class NameProblem { MISSING, TOO_LONG, TAKEN }

/** One attempt-log row with the learner it belongs to. */
data class LoggedAttempt(val learner: Learner, val record: AttemptRecord)

/**
 * The learners' saved data in the data folder (docs/desktop-plan.md, D5),
 * one folder per learner so that deleting a student removes everything of
 * theirs at once:
 *
 * ```
 * learners/<id>/learner.json            name and card
 * learners/<id>/progress.json           XP, done sentences, best stars, streak, Leitner box
 * learners/<id>/lessons.json           which lessons are open, and the current one
 * learners/<id>/attempts/<day>.jsonl    the attempt log, one row per line
 * learners/<id>/recordings/<sentence>.wav  the latest recording of each sentence (starter.bus_01.wav)
 * ```
 *
 * Every write replaces a whole file safely ([SafeFiles]). A file that can't
 * be read is reported to [problem] and never silently overwritten: a damaged
 * progress file is moved aside before the learner starts afresh. Thread-safe.
 */
class LearnerStore(
    dataDir: File,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val problem: (String) -> Unit = {},
) {
    private val root = File(dataDir, "learners")

    init {
        // A delete interrupted by a crash: the folder was already renamed, so finish it.
        root.listFiles { f -> f.isDirectory && f.name.startsWith(DELETING) }?.forEach { it.deleteRecursively() }
    }

    /**
     * All learners, oldest first (the order of the picker). A learner whose
     * file can't be read, or whose folder isn't named by its id (a folder
     * copied in Finder, "s1 copy", would show the learner twice and count
     * their attempts twice), is reported and left out.
     */
    @Synchronized
    fun learners(): List<Learner> = (root.listFiles { f -> f.isDirectory && !f.name.startsWith(".") } ?: emptyArray())
        .mapNotNull { dir ->
            val file = File(dir, LEARNER)
            if (!file.isFile) return@mapNotNull null
            runCatching {
                val learner = pretty.decodeFromString<Learner>(file.readText())
                require(learner.id == dir.name && ID.matches(dir.name)) { "the folder isn't named by the learner's id \"${learner.id}\"" }
                learner to OffsetDateTime.parse(learner.created).toInstant()
            }
                .onFailure { problem("$file could not be read: ${it.message ?: it}") }
                .getOrNull()
        }
        .sortedWith(compareBy({ it.second }, { it.first.id }))
        .map { it.first }

    fun learner(id: String): Learner? = learners().firstOrNull { it.id == id }

    /** Whether [name] can be used, ignoring [exceptId]'s own name. Names are compared without case or extra spaces. */
    @Synchronized
    fun checkName(name: String, exceptId: String? = null): NameProblem? {
        val clean = cleanName(name)
        return when {
            clean.isEmpty() -> NameProblem.MISSING
            clean.length > MAX_NAME -> NameProblem.TOO_LONG
            learners().any { it.id != exceptId && it.name.equals(clean, ignoreCase = true) } -> NameProblem.TAKEN
            else -> null
        }
    }

    /** The cards in use, so the picker can offer only free ones. */
    fun takenCards(): Set<Card> = learners().map { it.card }.toSet()

    @Synchronized
    fun add(name: String, card: Card): Learner {
        checkName(name)?.let { throw IllegalArgumentException("name \"$name\": $it") }
        require(card !in takenCards()) { "card $card is taken" }
        var stamp = clock.millis()
        while (File(root, "s" + stamp.toString(36)).exists()) stamp++
        val learner = Learner("s" + stamp.toString(36), cleanName(name), card.colour, card.symbol, now(clock))
        SafeFiles.write(File(dirOf(learner.id), LEARNER), pretty.encodeToString(learner) + "\n")
        return learner
    }

    @Synchronized
    fun rename(id: String, name: String): Learner {
        checkName(name, exceptId = id)?.let { throw IllegalArgumentException("name \"$name\": $it") }
        val renamed = requireNotNull(learner(id)) { "no learner $id" }.copy(name = cleanName(name))
        SafeFiles.write(File(dirOf(id), LEARNER), pretty.encodeToString(renamed) + "\n")
        return renamed
    }

    /**
     * Removes the learner's progress, attempt-log rows and recordings
     * (docs/decisions.md, "Deleting a student"). The folder is renamed first,
     * so the learner disappears at once even if deleting the files is cut short.
     */
    @Synchronized
    fun delete(id: String) {
        val dir = dirOf(id)
        if (!dir.isDirectory) return
        val doomed = File(root, "$DELETING$id")
        check(dir.renameTo(doomed)) { "could not delete $dir" }
        doomed.deleteRecursively()
    }

    @Synchronized
    fun progress(id: String): LearnerProgress {
        val file = File(dirOf(id), PROGRESS)
        if (!file.isFile) return LearnerProgress()
        return try {
            pretty.decodeFromString<ProgressJson>(file.readText()).toProgress()
        } catch (e: Exception) {
            val aside = File(file.parentFile, "progress.damaged-${clock.millis()}.json")
            file.renameTo(aside)
            problem("$file could not be read (${e.message ?: e}); it was kept as ${aside.name} and the learner starts afresh")
            LearnerProgress()
        }
    }

    @Synchronized
    fun saveProgress(id: String, progress: LearnerProgress) {
        require(dirOf(id).isDirectory) { "no learner $id" }
        SafeFiles.write(File(dirOf(id), PROGRESS), pretty.encodeToString(ProgressJson.of(progress)) + "\n")
    }

    /** Which lessons the learner may practise. A missing file is a learner from before lesson packs: nothing opened yet. */
    @Synchronized
    fun lessons(id: String): LearnerLessons {
        val file = File(dirOf(id), LESSONS)
        if (!file.isFile) return LearnerLessons()
        return try {
            pretty.decodeFromString<LearnerLessons>(file.readText()).let { l ->
                l.copy(current = l.current?.let(SavedIds::qualify), opened = l.opened.map(SavedIds::qualify).distinct())
            }
        } catch (e: Exception) {
            val aside = File(file.parentFile, "lessons.damaged-${clock.millis()}.json")
            file.renameTo(aside)
            problem("$file could not be read (${e.message ?: e}); it was kept as ${aside.name}, and the learner's open lessons are worked out again")
            LearnerLessons()
        }
    }

    @Synchronized
    fun saveLessons(id: String, lessons: LearnerLessons) {
        require(dirOf(id).isDirectory) { "no learner $id" }
        SafeFiles.write(File(dirOf(id), LESSONS), pretty.encodeToString(lessons) + "\n")
    }

    /** Adds a row to the learner's log for the day of [AttemptRecord.time]. */
    @Synchronized
    fun logAttempt(id: String, record: AttemptRecord) {
        require(dirOf(id).isDirectory) { "no learner $id" }
        val file = File(dirOf(id), "$ATTEMPTS/${OffsetDateTime.parse(record.time).toLocalDate()}.jsonl")
        val old = if (file.isFile) file.readText() else ""
        // A file whose last line lost its line break (edited by hand) would otherwise swallow this row.
        val separator = if (old.isEmpty() || old.endsWith("\n")) "" else "\n"
        SafeFiles.write(file, old + separator + oneLine.encodeToString(record) + "\n")
    }

    /**
     * The learner's log, oldest first. Rows that can't be read, or whose time
     * isn't a time, are reported and left out. Ids saved before they carried
     * their pack's id are read in full ([SavedIds]).
     */
    @Synchronized
    fun attempts(id: String): List<AttemptRecord> {
        val files = File(dirOf(id), ATTEMPTS).listFiles { f -> f.isFile && f.name.endsWith(".jsonl") }?.sortedBy { it.name } ?: return emptyList()
        return files.flatMap { file ->
            file.readLines().withIndex().filter { it.value.isNotBlank() }.mapNotNull { (i, text) ->
                runCatching {
                    oneLine.decodeFromString<AttemptRecord>(text)
                        .also { OffsetDateTime.parse(it.time) }
                        .let { it.copy(lesson = SavedIds.qualify(it.lesson), sentence = SavedIds.qualify(it.sentence)) }
                }
                    .onFailure { problem("$file line ${i + 1} could not be read: ${it.message ?: it}") }
                    .getOrNull()
            }
        }
    }

    /** Every learner's log, oldest first. */
    @Synchronized
    fun allAttempts(): List<LoggedAttempt> = learners()
        .flatMap { learner -> attempts(learner.id).map { LoggedAttempt(learner, it) to OffsetDateTime.parse(it.time).toInstant() } }
        .sortedBy { it.second }
        .map { it.first }

    /** Keeps [wav] as the latest recording of [sentenceId] (sentence attempts only, docs/decisions.md). */
    @Synchronized
    fun saveRecording(id: String, sentenceId: String, wav: ByteArray): File {
        require(dirOf(id).isDirectory) { "no learner $id" }
        return recordingFile(id, sentenceId).also { SafeFiles.write(it, wav) }
    }

    fun recordingFile(id: String, sentenceId: String): File = File(dirOf(id), "recordings/$sentenceId.wav")

    private fun dirOf(id: String): File {
        require(ID.matches(id)) { "bad learner id \"$id\"" }
        return File(root, id)
    }

    companion object {
        /** The pilot's limit. */
        const val MAX_NAME = 40

        private const val LEARNER = "learner.json"
        private const val PROGRESS = "progress.json"
        private const val LESSONS = "lessons.json"
        private const val ATTEMPTS = "attempts"
        private const val DELETING = ".deleting-"
        private val ID = Regex("[a-z0-9]+")

        private val pretty = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
        private val oneLine = Json { encodeDefaults = true; ignoreUnknownKeys = true }

        fun cleanName(name: String): String = name.trim().replace(Regex("\\s+"), " ")

        /** The Mac's local time with its offset, to the second: 2026-10-21T13:45:30+05:30. */
        fun now(clock: Clock): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS))

        /** The day of a saved time, in its own offset. */
        fun dayOf(time: String): LocalDate = OffsetDateTime.parse(time).toLocalDate()
    }
}
