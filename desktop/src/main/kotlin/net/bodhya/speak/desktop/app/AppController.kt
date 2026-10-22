package net.bodhya.speak.desktop.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.ShownMessage
import net.bodhya.speak.desktop.check.SpeechCheckController
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.content.LessonLibrary
import net.bodhya.speak.desktop.content.PackCheck
import net.bodhya.speak.desktop.content.PackInUse
import net.bodhya.speak.desktop.data.AttemptRecord
import net.bodhya.speak.desktop.data.AttemptResult
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.data.LearnerLessons
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.desktop.data.LoggedAttempt
import net.bodhya.speak.desktop.data.Results
import net.bodhya.speak.desktop.data.SafeFiles
import net.bodhya.speak.desktop.data.StudentSummary
import net.bodhya.speak.desktop.data.NameProblem
import net.bodhya.speak.desktop.practice.Heard
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.desktop.voice.Speeds
import net.bodhya.speak.domain.AfterSentence
import net.bodhya.speak.domain.Alignment
import net.bodhya.speak.domain.FeedbackSelector
import net.bodhya.speak.domain.FixOutcome
import net.bodhya.speak.domain.FixState
import net.bodhya.speak.domain.FixingLoop
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Hints
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.Leitner
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.Practice
import net.bodhya.speak.domain.Score
import net.bodhya.speak.domain.Sentence
import net.bodhya.speak.domain.SentenceAttempt
import net.bodhya.speak.domain.StreakView
import net.bodhya.speak.domain.Streaks
import net.bodhya.speak.domain.Tip
import net.bodhya.speak.domain.WordToFix
import net.bodhya.speak.speech.Recording
import net.bodhya.speak.speech.WavWriter
import java.io.File
import java.time.Clock
import java.time.LocalDate

/** The main window's screens, as in the pilot's flow. */
sealed interface Screen {
    /** "Who is practising?": where the app always starts (docs/decisions.md, P7). */
    data object Students : Screen

    /** The learner chooses a card; the facilitator types the name. */
    data object NewStudent : Screen

    data object Home : Screen

    /** Practising [ref]'s sentence: listen, then speak. */
    data class Practice(val ref: SentenceRef) : Screen

    /** How [ref]'s sentence was said ([AppState.result]). */
    data class Feedback(val ref: SentenceRef) : Screen

    /** The five-step fixing loop for the word to fix of [ref]'s sentence ([AppState.fix]). */
    data class Fix(val ref: SentenceRef) : Screen

    /** The daily goal was just reached: shown once per learner per day. */
    data object GoalDone : Screen

    /** All students' results and the CSV, for the facilitator (milestone D8). */
    data object Results : Screen

    /** Milestone D3's speech check, for the facilitator (Settings). */
    data object SpeechCheck : Screen
}

/**
 * What the Results screen shows: every row of the attempt log, the summary
 * and the latest attempts. [read] false: the log couldn't be read (the
 * screen shows why).
 */
data class ResultsData(val rows: List<LoggedAttempt>, val summary: List<StudentSummary>, val recent: List<LoggedAttempt>, val read: Boolean = true)

/** A learner as the picker shows them: their card, name and streak (and, for Settings, whether all lessons are open for them). */
data class StudentCard(val learner: Learner, val streak: StreakView, val openAll: Boolean = false)

/**
 * Sentence [index] of [lesson]: what the practice, feedback and fixing
 * screens show. Usually of the current lesson; a sentence brought back for
 * revision can be of another open lesson.
 */
data class SentenceRef(val lesson: Lesson, val index: Int) {
    init {
        require(index in lesson.sentences.indices) { "lesson ${lesson.id} has no sentence $index" }
    }

    val sentence: Sentence get() = lesson.sentences[index]
}

/** A scored sentence attempt, for the feedback screen. */
data class SentenceResult(
    val ref: SentenceRef,
    val alignment: Alignment,
    val score: Score,
    val xp: Int,
    /** What the recognizer returned. */
    val heard: String,
    /** The first scored word said wrong (or else missed); null when every scored word was right. */
    val toFix: WordToFix?,
    /** [toFix] as written in the sentence ("Where", not "where"; docs/decisions.md, "Show the fix word as written"). */
    val toFixWritten: String?,
    val tip: Tip?,
    /** Lessons this attempt opened (every sentence of the lesson before them now said). */
    val opened: List<Lesson> = emptyList(),
)

/** A lesson as Home lists it. */
data class LessonRow(val lesson: Lesson, val open: Boolean, val current: Boolean, val said: Int)

/** The fixing loop as the screen shows it. */
data class FixView(
    val state: FixState,
    /** The word as written in the sentence ("Where"). */
    val written: String,
    /** Step 2: the word's syllables, its Hindi sound hint and the tip for its sound. */
    val syllables: List<String>,
    val soundHi: String?,
    val tipHi: String,
)

data class AppState(
    val screen: Screen = Screen.Students,
    val students: List<StudentCard> = emptyList(),
    /** Who is practising; null on the picker. */
    val learner: Learner? = null,
    val progress: LearnerProgress = LearnerProgress(),
    val today: LocalDate,
    val message: ShownMessage? = null,
    val result: SentenceResult? = null,
    val fix: FixView? = null,
    /** The learner's current lesson: where Start practice continues (a sentence brought back for revision can be of another). */
    val lesson: Lesson? = null,
    /** Every lesson in the unlock order, open or locked, for Home. */
    val lessons: List<LessonRow> = emptyList(),
    val learnerLessons: LearnerLessons = LearnerLessons(),
)

/**
 * The flow between the student picker, home, practice, feedback, the
 * fixing loop and the goal screen (milestones D5–D7), without the window,
 * following the pilot with the agreed changes (docs/decisions.md), over
 * every lesson of the [LessonLibrary]: lessons open in order; Start
 * practice and Next bring back sentences with words due for revision (also
 * from other open lessons), then the current lesson's sentences never said;
 * and once every sentence of the current lesson has been said, the next
 * lesson becomes current. Every change to a learner's progress is saved at
 * once, so quitting or a power cut loses nothing, and every recording of a
 * sentence or a fixing step is added to the attempt log, "Didn't hear you"
 * included.
 */
class AppController(
    private val store: LearnerStore,
    library: LessonLibrary,
    private val output: SpeechOutput,
    /** Records and recognizes answers; null where nothing is recorded (tests of the picker). */
    private val listening: Listening? = null,
    /** Whether the speech model has loaded, so the mic can be used. */
    private val micReady: () -> Boolean = { true },
    val config: GameConfig = GameConfig(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val log: (String) -> Unit = {},
    /** The current lesson changed (its speech can be prepared). */
    private val onLessonChange: (Lesson) -> Unit = {},
    /** The app macOS asks about the microphone ("English Saathi", or "Terminal" when run from Gradle). */
    private val permissionApp: String = "Terminal",
) {
    private val _state = MutableStateFlow(AppState(today = today()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    /** The lessons the app can use; replaced when a pack is added. */
    var library: LessonLibrary = library
        private set

    /** The learner's current lesson (the first lesson on screens without a learner). */
    val lesson: Lesson get() = _state.value.lesson ?: library.lessons.first().lesson

    /**
     * The current lesson's sentence last shown, for choosing the next one
     * there; null when none has been shown (the pilot's 0). A sentence
     * brought back from another lesson leaves it alone.
     */
    private var cursor: SentenceRef? = null

    /** Changes with every screen; a recording made on an earlier screen is dropped. */
    private var screenToken = 0

    init {
        showStudents()
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    /** Moves to another screen: speech and listening stop, and any answer still being recognized is dropped. */
    private fun go(change: (AppState) -> AppState) {
        output.stop()
        listening?.stop()
        screenToken++
        _state.update(change)
    }

    fun showStudents() {
        val today = today()
        go { AppState(Screen.Students, cards(today), today = today) }
    }

    private fun cards(today: LocalDate) = store.learners().map {
        StudentCard(it, Streaks.view(store.progress(it.id).streak, today, config), store.lessons(it.id).openAll)
    }

    fun newStudent() = go { it.copy(screen = Screen.NewStudent, message = null) }

    /** The cards no learner has yet. */
    fun takenCards(): Set<Card> = store.takenCards()

    /**
     * Adds a learner and opens their home screen. Returns what is wrong
     * instead when the name or card can't be used; nothing is saved then.
     */
    fun addStudent(name: String, card: Card?): Message? {
        val problem = when (store.checkName(name)) {
            NameProblem.MISSING -> NAME_MISSING
            NameProblem.TOO_LONG -> NAME_TOO_LONG
            NameProblem.TAKEN -> nameTaken(LearnerStore.cleanName(name))
            null -> when {
                card == null -> CARD_MISSING
                card in store.takenCards() -> CARD_TAKEN
                else -> null
            }
        }
        if (problem != null) {
            _state.update { it.copy(message = ShownMessage(MessageKind.PROBLEM, problem)) }
            return problem
        }
        val learner = store.add(name, card!!)
        log("student added: ${learner.id} ${learner.card}")
        pick(learner.id)
        return null
    }

    /** A learner chose their card: their home screen, with missed days noted for the streak and their lessons opened. */
    fun pick(id: String) {
        val learner = store.learner(id) ?: return showStudents()
        val today = today()
        val stored = store.progress(id)
        val progress = Practice.openApp(stored, today, config)
        if (progress != stored) store.saveProgress(id, progress)
        val lessons = movedOn(id, openLessons(id, store.lessons(id), progress), progress)
        val current = currentLesson(lessons, progress)
        cursor = null
        go { AppState(Screen.Home, cards(today), learner, progress, today, lesson = current, lessons = rows(progress, lessons, current), learnerLessons = lessons) }
        onLessonChange(current)
    }

    fun home() = go { it.copy(screen = Screen.Home, today = today(), message = null, result = null, fix = null) }

    /** An open lesson on Home: it becomes the current lesson. */
    fun chooseLesson(id: String) {
        val s = _state.value
        val learner = s.learner ?: return
        val lesson = library.lesson(id)?.lesson ?: return
        if (!s.learnerLessons.isOpen(id) || lesson.id == s.lesson?.id) return
        switchLesson(learner, lesson)
    }

    /** "Start practice" / "Practise more" on home (Enter), and on the goal screen: the sentence [choose] picks. */
    fun startPractice() {
        val s = _state.value
        if (s.learner == null) return
        open(choose(s.progress, Practice.startPractice(s.progress, lesson.sentences, cursorIn(lesson), today(), config)))
    }

    /**
     * The sentence to practise next (docs/decisions.md, "Lesson review: owner
     * answers"): one not done today with a word due for revision (scored
     * there), in the current lesson first, then in the earlier open lessons
     * ([revisionLessons]); then the current lesson's first sentence never
     * said; otherwise [fallback], the pilot's choice in the current lesson.
     */
    private fun choose(progress: LearnerProgress, fallback: Int): SentenceRef {
        val today = today()
        val current = lesson
        val due = Leitner.due(progress.leitner, today, config).toSet()
        if (due.isNotEmpty()) {
            val done = progress.doneOn(today).toSet()
            for (l in revisionLessons()) {
                val i = l.sentences.indexOfFirst { s -> s.id !in done && s.targetWords.any { it in due && it !in l.unscoredIn(s) } }
                if (i >= 0) return SentenceRef(l, i)
            }
        }
        val unsaid = current.sentences.indexOfFirst { it.id !in progress.bestStars }
        return SentenceRef(current, if (unsaid >= 0) unsaid else fallback)
    }

    /**
     * Where words due for revision come back from: the current lesson, then the
     * open lessons before it in the unlock order (the owner: "a sentence from
     * an earlier open lesson"), never a later one the student hasn't reached.
     */
    private fun revisionLessons(): List<Lesson> {
        val current = lesson
        val opened = _state.value.learnerLessons
        return listOf(current) + library.lessons.map { it.lesson }.takeWhile { it.id != current.id }.filter { opened.isOpen(it.id) }
    }

    /** Where the pilot's sentence order continues in [lesson]: after the sentence of it last shown (0 at first). */
    private fun cursorIn(lesson: Lesson): Int = cursor?.takeIf { it.lesson.id == lesson.id && it.index in lesson.sentences.indices }?.index ?: 0

    /** The student chose [lesson] on Home: it stays current, even once all of it has been said. */
    private fun switchLesson(learner: Learner, lesson: Lesson) {
        val lessons = _state.value.learnerLessons.copy(current = lesson.id, chosen = true)
        store.saveLessons(learner.id, lessons)
        cursor = null
        _state.update { it.copy(lesson = lesson, learnerLessons = lessons, lessons = rows(it.progress, lessons, lesson)) }
        onLessonChange(lesson)
    }

    /** The learner's open lessons after [progress], saved when that opened one. */
    private fun openLessons(id: String, stored: LearnerLessons, progress: LearnerProgress): LearnerLessons {
        val now = stored.unlocked(library.lessons.map { it.lesson }, progress.bestStars.keys)
        if (now != stored) store.saveLessons(id, now)
        return now
    }

    /**
     * The lesson to continue: the saved current one while it exists and is
     * open; otherwise (at first, after a pack update dropped it, or after
     * "Open all lessons" was turned off) the first open lesson with a
     * sentence never said, or else the furthest open one.
     */
    private fun currentLesson(lessons: LearnerLessons, progress: LearnerProgress): Lesson {
        val all = library.lessons.map { it.lesson }
        val open = all.filter { lessons.isOpen(it.id) }
        return open.firstOrNull { it.id == lessons.current } ?: open.firstOrNull { !allSaid(it, progress) } ?: open.lastOrNull() ?: all.first()
    }

    /**
     * Moving on outside an attempt, at start and when the lessons change: the
     * saved current lesson is all said and wasn't chosen on Home, and an open
     * lesson after it has a sentence never said (it came later, with a new
     * pack or a pack update). That lesson becomes current, saved.
     */
    private fun movedOn(id: String, lessons: LearnerLessons, progress: LearnerProgress): LearnerLessons {
        val saved = lessons.current?.let { library.lesson(it)?.lesson } ?: return lessons
        if (lessons.chosen || !lessons.isOpen(saved.id) || !allSaid(saved, progress)) return lessons
        val next = nextLesson(saved, lessons, progress) ?: return lessons
        log("student $id: every sentence of ${saved.id} said, on to ${next.id}")
        return lessons.copy(current = next.id).also { store.saveLessons(id, it) }
    }

    /** The lesson after [done] to move on to: the first open one with a sentence never said; null to stay. */
    private fun nextLesson(done: Lesson, lessons: LearnerLessons, progress: LearnerProgress): Lesson? {
        val all = library.lessons.map { it.lesson }
        return all.drop(all.indexOfFirst { it.id == done.id } + 1).firstOrNull { lessons.isOpen(it.id) && !allSaid(it, progress) }
    }

    private fun allSaid(lesson: Lesson, progress: LearnerProgress) = lesson.sentences.all { it.id in progress.bestStars }

    /** The tips for [lesson]'s words: its pack's. */
    private fun hintsFor(lesson: Lesson): Hints = (library.lesson(lesson.id) ?: library.lessons.first()).hints

    private fun rows(progress: LearnerProgress, lessons: LearnerLessons, current: Lesson): List<LessonRow> =
        library.lessons.map { e ->
            LessonRow(e.lesson, lessons.isOpen(e.lesson.id), e.lesson.id == current.id, e.lesson.sentences.count { it.id in progress.bestStars })
        }

    /** A sentence row on home: a sentence of the current lesson. */
    fun openSentence(index: Int) = open(SentenceRef(lesson, index))

    private fun open(ref: SentenceRef) {
        if (ref.lesson.id == lesson.id) cursor = ref
        go { it.copy(screen = Screen.Practice(ref), today = today(), message = null, result = null, fix = null) }
    }

    /** Listen (L) and Slowly (S) on the practice screen; "Hear the correct sentence" (L) on feedback. */
    fun listen(slow: Boolean) {
        val ref = when (val screen = _state.value.screen) {
            is Screen.Practice -> screen.ref
            is Screen.Feedback -> screen.ref
            else -> return
        }
        output.english(ref.sentence.text, if (slow) Speeds.SLOW else Speeds.NORMAL)
    }

    /** The mic button and Space on the practice screen and fixing steps 3–5: start recording, or stop early. */
    fun toggleRecording() {
        val s = _state.value
        val ref = when (val screen = s.screen) {
            is Screen.Practice -> screen.ref
            is Screen.Fix -> screen.ref.takeIf { s.fix != null && FixingLoop.isRecordingStep(s.fix.state.step) } ?: return
            else -> return
        }
        val l = listening ?: return
        if (l.state.value.listening) return l.stop()
        if (l.state.value.busy || !micReady()) return
        output.stop()
        _state.update { it.copy(message = null) }
        val token = screenToken
        val fixing = s.fix?.state.takeIf { s.screen is Screen.Fix }
        l.start { heard ->
            when {
                token != screenToken -> log("dropped a result for ${ref.sentence.id}: the screen moved on")
                fixing != null -> onFixHeard(ref, fixing, heard)
                else -> onSentenceHeard(ref, heard)
            }
        }
    }

    private fun onSentenceHeard(ref: SentenceRef, heard: Heard) {
        val s = _state.value
        val learner = s.learner ?: return
        val today = today()
        val lesson = ref.lesson
        val sentence = ref.sentence
        when (heard) {
            is Heard.Words -> {
                saveRecording(learner, sentence, heard.recording)
                when (val attempt = Practice.attemptSentence(s.progress, lesson, sentence, heard.text, today, config)) {
                    SentenceAttempt.NothingHeard -> {
                        logAttempt(learner, ref, heard.text, AttemptResult.NOT_HEARD, model = heard.model.id, waitMs = heard.waitMs, recording = heard.recording)
                        show(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR)
                    }
                    is SentenceAttempt.Scored -> {
                        store.saveProgress(learner.id, attempt.progress)
                        logAttempt(
                            learner, ref, heard.text, AttemptResult.SCORED, attempt.score.correct, attempt.score.total, attempt.score.stars,
                            heard.model.id, heard.waitMs, heard.recording,
                        )
                        // Saying the last unsaid sentence of a lesson opens the next one; of the current lesson, it also moves the learner on.
                        val before = s.learnerLessons
                        var lessons = openLessons(learner.id, before, attempt.progress)
                        val opened = library.lessons.map { it.lesson }.filter { it.id in lessons.opened && it.id !in before.opened && !before.openAll }
                        var current = this.lesson
                        if (lesson.id == current.id && !allSaid(current, s.progress) && allSaid(current, attempt.progress)) {
                            nextLesson(current, lessons, attempt.progress)?.let { next ->
                                lessons = lessons.copy(current = next.id, chosen = false)
                                store.saveLessons(learner.id, lessons)
                                log("student ${learner.id}: every sentence of ${current.id} said, on to ${next.id}")
                                current = next
                                cursor = null
                            }
                        }
                        val toFix = FeedbackSelector.wordToFix(attempt.alignment)
                        val result = SentenceResult(
                            ref, attempt.alignment, attempt.score, attempt.xp, heard.text, toFix,
                            toFix?.let { writtenWord(sentence, it.index) }, toFix?.let { FeedbackSelector.tip(it.word, lesson, hintsFor(lesson)) },
                            opened,
                        )
                        val moved = current.id != this.lesson.id
                        go {
                            it.copy(
                                screen = Screen.Feedback(ref), progress = attempt.progress, today = today, result = result, message = null,
                                lesson = current, learnerLessons = lessons, lessons = rows(attempt.progress, lessons, current),
                            )
                        }
                        if (moved) onLessonChange(current)
                    }
                }
            }
            is Heard.NothingLoud -> {
                logAttempt(learner, ref, "", AttemptResult.NOT_HEARD, recording = heard.recording)
                show(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR)
            }
            is Heard.Silent -> {
                logAttempt(learner, ref, "", AttemptResult.NOT_HEARD, recording = heard.recording)
                show(MessageKind.PROBLEM, SpeechCheckController.silent(heard.permission, permissionApp))
            }
            is Heard.NoMicrophone -> show(MessageKind.PROBLEM, SpeechCheckController.unavailable(heard.detail))
            is Heard.Failed -> show(MessageKind.PROBLEM, SOMETHING_WRONG)
        }
    }

    /**
     * Next sentence, or Skip: the goal screen the first time today's goal is
     * reached, otherwise the sentence [choose] picks.
     */
    fun next() {
        val s = _state.value
        val learner = s.learner ?: return
        val after = Practice.afterSentence(s.progress, lesson.sentences, cursorIn(lesson), today(), config)
        if (after.progress != s.progress) store.saveProgress(learner.id, after.progress)
        _state.update { it.copy(progress = after.progress) }
        when (val next = after.next) {
            AfterSentence.GoalComplete -> go { it.copy(screen = Screen.GoalDone, today = today(), message = null, result = null, fix = null) }
            is AfterSentence.Next -> open(choose(after.progress, next.index))
        }
    }

    /** Fix "word" on the feedback screen: step 1 of the fixing loop for the word to fix. */
    fun fix() {
        val s = _state.value
        val screen = s.screen as? Screen.Feedback ?: return
        val result = s.result ?: return
        val toFix = result.toFix ?: return
        val ref = screen.ref
        val tip = result.tip ?: FeedbackSelector.tip(toFix.word, ref.lesson, hintsFor(ref.lesson))
        val view = FixView(
            FixingLoop.start(ref.sentence, toFix.index), result.toFixWritten ?: toFix.word,
            ref.lesson.syllablesOf(toFix.word), tip.soundHi, tip.template.textHi,
        )
        go { it.copy(screen = Screen.Fix(ref), fix = view, message = null) }
    }

    /** "Next" on fixing steps 1 and 2, and Enter there (steps 3–5 use the mic). */
    fun fixNext() {
        val view = _state.value.fix ?: return
        if (_state.value.screen !is Screen.Fix || FixingLoop.isRecordingStep(view.state.step)) return
        output.stop()
        _state.update { it.copy(fix = view.copy(state = FixingLoop.next(view.state)), message = null) }
    }

    /** The back arrow on the fixing screen: the feedback again. */
    fun backToFeedback() {
        val screen = _state.value.screen as? Screen.Fix ?: return
        go { it.copy(screen = Screen.Feedback(screen.ref), fix = null, message = null) }
    }

    /**
     * L and S while fixing, as in the pilot: on steps 1–3 both play the word
     * slowly; on steps 4 and 5, the phrase or sentence at normal speed (L) or
     * slowly (S). The prepared lesson speech has words in their normalized
     * form, so Listen is instant.
     */
    fun fixListen(slow: Boolean) {
        val view = _state.value.fix ?: return
        val f = view.state
        if (f.step <= 3) output.english(f.word, Speeds.SLOW)
        else output.english(FixingLoop.target(f)!!, if (slow) Speeds.SLOW else Speeds.NORMAL)
    }

    private fun onFixHeard(ref: SentenceRef, fixing: FixState, heard: Heard) {
        val s = _state.value
        val learner = s.learner ?: return
        val view = s.fix ?: return
        val lesson = ref.lesson
        val sentence = ref.sentence
        val target = FixingLoop.target(fixing)!!
        // The word itself is logged as written ("Where"); the phrase and sentence are the lesson's text.
        val shownTarget = if (fixing.step == 3) view.written else target
        val mode = AttemptRecord.fixStep(fixing.step)
        val today = today()
        fun notHeard(text: String, model: String? = null, waitMs: Long? = null, recording: Recording?, message: ShownMessage) {
            logAttempt(learner, ref, text, AttemptResult.NOT_HEARD, model = model, waitMs = waitMs, recording = recording, mode = mode, target = shownTarget)
            _state.update { it.copy(message = message) }
        }
        when (heard) {
            is Heard.Words -> {
                val attempt = Practice.attemptFixStep(s.progress, fixing, lesson, sentence, heard.text, today, config)
                val r = attempt.result
                if (r.outcome == FixOutcome.NOTHING_HEARD) {
                    return notHeard(heard.text, heard.model.id, heard.waitMs, heard.recording, ShownMessage(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR))
                }
                if (attempt.progress != s.progress) store.saveProgress(learner.id, attempt.progress)
                // Words the recognizer can't be expected to hear don't count in the phrase or sentence either.
                val counted = r.alignment!!.withUnscored(lesson.unscoredIn(sentence))
                val passed = r.outcome == FixOutcome.PASSED || r.outcome == FixOutcome.SENTENCE_FIXED
                logAttempt(
                    learner, ref, heard.text, if (passed) AttemptResult.PASSED else AttemptResult.FAILED, counted.correctCount, counted.total,
                    model = heard.model.id, waitMs = heard.waitMs, recording = heard.recording, mode = mode, target = shownTarget,
                )
                val message = when (r.outcome) {
                    FixOutcome.PASSED -> ShownMessage(MessageKind.OK, Message("Correct! +${r.xp} XP", "सही! अगला कदम"))
                    FixOutcome.TRY_AGAIN -> ShownMessage(MessageKind.PROBLEM, Message("Try once more. We heard: “${heard.text.trim()}”", "एक बार और कोशिश कीजिए।"))
                    FixOutcome.DROPPED_BACK -> ShownMessage(MessageKind.PROBLEM, Message("Let’s go back one step.", "एक कदम पीछे चलते हैं।"))
                    else -> null
                }
                _state.update { it.copy(progress = attempt.progress, today = today, fix = view.copy(state = r.state), message = message) }
                // Step 5 passed: the sentence is fixed, and the pilot goes straight on.
                if (r.outcome == FixOutcome.SENTENCE_FIXED) next()
            }
            is Heard.NothingLoud -> notHeard("", recording = heard.recording, message = ShownMessage(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR))
            is Heard.Silent -> notHeard("", recording = heard.recording, message = ShownMessage(MessageKind.PROBLEM, SpeechCheckController.silent(heard.permission, permissionApp)))
            is Heard.NoMicrophone -> show(MessageKind.PROBLEM, SpeechCheckController.unavailable(heard.detail))
            is Heard.Failed -> show(MessageKind.PROBLEM, SOMETHING_WRONG)
        }
    }

    /**
     * Words for the goal screen's "Practise again tomorrow", as written in the
     * lessons, in the order they entered the box: only words that can come
     * back, scored in a sentence of the current lesson or an earlier open one
     * ([revisionLessons]); not ones only in a lesson a pack update dropped,
     * for example.
     */
    fun wordsForTomorrow(): List<String> {
        val s = _state.value
        val revisable = revisionLessons().flatMap { l -> l.sentences.flatMap { st -> st.targetWords.filter { it !in l.unscoredIn(st) } } }.toSet()
        return Practice.wordsForTomorrow(s.progress, s.today, config).filter { it in revisable }.map { writtenInLesson(it) }
    }

    /**
     * [word] as the lessons write it: an occurrence inside a sentence if
     * there is one, so a capital only at the start isn't copied. The current
     * lesson first; a word from another lesson is found there.
     */
    private fun writtenInLesson(word: String): String {
        val lessons = listOf(lesson) + library.lessons.map { it.lesson }.filter { it.id != lesson.id }
        val places = lessons.flatMap { it.sentences }.flatMap { sentence -> sentence.targetWords.withIndex().filter { it.value == word }.map { sentence to it.index } }
        val (sentence, index) = places.firstOrNull { it.second > 0 } ?: places.firstOrNull() ?: return word
        return writtenWord(sentence, index)
    }

    /** Enter on the feedback screen: Fix when there is a word to fix, otherwise Next sentence (as in the pilot). */
    fun feedbackPrimary() = if (_state.value.result?.toFix != null) fix() else next()

    /** Leaving the window (another app comes to the front) stops listening and speech. */
    fun leave() {
        listening?.stop()
        output.stop()
    }

    /** The facilitator's speech check, from Settings. */
    fun speechCheck() = go { it.copy(screen = Screen.SpeechCheck, message = null) }

    /** Results, from the picker. */
    fun results() = go { it.copy(screen = Screen.Results, message = null) }

    /** Reads the whole attempt log. Blocking: call it off the main thread. */
    fun loadResults(): ResultsData = try {
        val rows = store.allAttempts()
        ResultsData(rows, Results.summary(rows), Results.recent(rows))
    } catch (e: Exception) {
        log("results could not be read: $e")
        show(MessageKind.PROBLEM, Message("The results couldn’t be read: ${e.message ?: e}", "नतीजे पढ़े नहीं जा सके।"))
        ResultsData(emptyList(), emptyList(), emptyList(), read = false)
    }

    /** The name the Save dialog suggests for the CSV. */
    fun csvFileName(): String = Results.fileName(today())

    /** Sentences in all lessons, for Results' "Sentences tried". */
    fun sentenceCount(): Int = library.sentenceCount

    /** The packs in use, for Settings. */
    fun packs(): List<PackInUse> = library.packs

    /** Settings › Add lessons: checks a pack file and says what it would add. Changes nothing. */
    fun checkPack(file: File): PackCheck = library.check(file).also { check ->
        when (check) {
            is PackCheck.Rejected -> log("lesson pack ${file.name} refused: ${check.message.en}")
            is PackCheck.Ready -> check.preview.warnings.forEach { log("lesson pack warning: $it") }
        }
    }

    /**
     * Adds a pack [checkPack] found ready, and uses the new lessons at once.
     * Returns what to tell the facilitator. A learner in the middle of a
     * sentence goes back to their home screen, as its lesson may have changed.
     */
    fun installPack(ready: PackCheck.Ready): Message {
        val message = try {
            library = library.install(ready)
            val p = ready.preview
            log("lesson pack added: ${p.id} version ${p.version} (${p.lessons.size} lessons)")
            Message(
                "Added “${p.titleEn}” (version ${p.version}): ${p.lessons.size} ${if (p.lessons.size == 1) "lesson" else "lessons"}.",
                "“${p.titleHi}” जुड़ गया: ${p.lessons.size} पाठ।",
            )
        } catch (e: Exception) {
            log("lesson pack ${ready.preview.id} not added: $e")
            return Message("The lesson pack couldn’t be added: ${e.message ?: e}. Nothing was changed.", "पैक नहीं जुड़ सका। कुछ नहीं बदला।")
        }
        lessonsChanged(_state.value.learnerLessons, libraryChanged = true)
        return message
    }

    /** Settings › Students › "Open all lessons" for one learner. */
    fun setOpenAll(id: String, open: Boolean) {
        val lessons = store.lessons(id).copy(openAll = open)
        store.saveLessons(id, lessons)
        log("student $id: all lessons ${if (open) "open" else "back to the unlock order"}")
        if (_state.value.learner?.id == id) lessonsChanged(lessons, libraryChanged = false)
        _state.update { it.copy(students = cards(today())) }
    }

    /**
     * The lessons ([libraryChanged]) or which are open for the learner
     * changed: their open lessons and current lesson again. From a sentence
     * whose lesson may have changed, or is no longer open, back home.
     */
    private fun lessonsChanged(stored: LearnerLessons, libraryChanged: Boolean) {
        val s = _state.value
        val learner = s.learner ?: return _state.update { it.copy(lesson = null, lessons = emptyList()) }
        val lessons = movedOn(learner.id, openLessons(learner.id, stored, s.progress), s.progress)
        val current = currentLesson(lessons, s.progress)
        val onScreen = when (val screen = s.screen) {
            is Screen.Practice -> screen.ref
            is Screen.Feedback -> screen.ref
            is Screen.Fix -> screen.ref
            else -> null
        }
        if (onScreen != null && (libraryChanged || !lessons.isOpen(onScreen.lesson.id))) {
            go { it.copy(screen = Screen.Home, message = null, result = null, fix = null) }
        }
        if (libraryChanged || current.id != s.lesson?.id) cursor = null
        _state.update { it.copy(lesson = current, learnerLessons = lessons, lessons = rows(it.progress, lessons, current)) }
        if (current != s.lesson) onLessonChange(current)
    }

    /**
     * Writes the whole attempt log as the CSV to [file], the place chosen in
     * the Save dialog, and says whether it worked. The log is read again
     * here, so a student deleted or renamed meanwhile in Settings is
     * exported as they are now. Blocking: call it off the main thread.
     */
    fun saveCsv(file: File) {
        val message = try {
            val rows = store.allAttempts()
            SafeFiles.write(file, Results.csv(rows))
            log("results saved: ${rows.size} rows to $file")
            ShownMessage(MessageKind.OK, Message("Results saved: ${file.name}", "नतीजे सेव हो गए।"))
        } catch (e: Exception) {
            log("results not saved to $file: $e")
            ShownMessage(MessageKind.PROBLEM, Message("The file couldn’t be saved: ${e.message ?: e}", "फ़ाइल सेव नहीं हुई।"))
        }
        _state.update { it.copy(message = message) }
    }

    /** Hindi help: the current screen's instruction, spoken. */
    fun hindiHelp() = output.hindi(helpText(_state.value))

    /** Renames a learner (Settings). Returns what is wrong, or null when done. */
    fun renameStudent(id: String, name: String): Message? {
        when (store.checkName(name, exceptId = id)) {
            NameProblem.MISSING -> return NAME_MISSING
            NameProblem.TOO_LONG -> return NAME_TOO_LONG
            NameProblem.TAKEN -> return nameTaken(LearnerStore.cleanName(name))
            null -> Unit
        }
        val renamed = store.rename(id, name)
        log("student renamed: $id")
        _state.update { s ->
            s.copy(
                students = s.students.map { if (it.learner.id == id) it.copy(learner = renamed) else it },
                learner = if (s.learner?.id == id) renamed else s.learner,
            )
        }
        return null
    }

    /**
     * Deletes a learner's progress, attempt-log rows and recordings
     * (Settings, after a confirmation). Back to the picker if they were
     * practising.
     */
    fun deleteStudent(id: String) {
        store.delete(id)
        log("student deleted: $id")
        if (_state.value.learner?.id == id || _state.value.screen == Screen.Students) showStudents()
        else _state.update { it.copy(students = cards(today())) }
    }

    /** All learners, for Settings. */
    fun students(): List<Learner> = store.learners()

    private fun show(kind: MessageKind, message: Message) = _state.update { it.copy(message = ShownMessage(kind, message)) }

    private fun logAttempt(
        learner: Learner,
        ref: SentenceRef,
        heard: String,
        result: AttemptResult,
        correct: Int? = null,
        total: Int? = null,
        stars: Int? = null,
        model: String? = null,
        waitMs: Long? = null,
        recording: Recording? = null,
        mode: String = AttemptRecord.SENTENCE,
        target: String = ref.sentence.text,
    ) {
        val record = AttemptRecord(LearnerStore.now(clock), ref.lesson.id, ref.sentence.id, mode, target, heard, correct, total, stars, model, result, waitMs)
        store.logAttempt(learner.id, record)
        // The session log also gets what the endpointer saw, for tuning the silence rules on real audio.
        log("attempt | learner=${learner.id} | ${record.mode} | sentence=${record.sentence} | ${result.text} | heard=$heard | " +
            "score=${correct ?: "-"}/${total ?: "-"} | model=${model ?: "-"} | wait_ms=${waitMs ?: "-"} | ${recording?.stats ?: "-"}")
    }

    /** The latest recording of each sentence per learner, when the recognizer ran (sentence attempts only, docs/decisions.md). */
    private fun saveRecording(learner: Learner, sentence: Sentence, recording: Recording) {
        runCatching { store.saveRecording(learner.id, sentence.id, WavWriter.pcm16Mono(recording.samples, recording.sampleRate)) }
            .onFailure { log("recording not saved: $it") }
    }

    companion object {
        // Messages drafted for the desktop app; they need the teacher review.

        /** The pilot's wording. */
        val NAME_MISSING = Message("Type the student’s name first.", "पहले नाम लिखिए।")
        val NAME_TOO_LONG = Message("Use a shorter name, up to ${LearnerStore.MAX_NAME} letters.", "छोटा नाम लिखिए।")
        val CARD_MISSING = Message("Choose a colour and a picture first.", "पहले रंग और चित्र चुनिए।")
        val CARD_TAKEN = Message("Another student has this card. Choose another colour or picture.", "यह कार्ड किसी और का है। दूसरा रंग या चित्र चुनिए।")

        /** The pilot's wording for a recording that went wrong. */
        val SOMETHING_WRONG = Message("Something stopped the recording. Press the mic and try again.", "माइक दबाकर फिर कोशिश कीजिए।")

        fun nameTaken(name: String) = Message(
            "There is already a student called $name. Add something to tell them apart, such as the first letter of the surname.",
            "इस नाम का छात्र पहले से है। नाम में कुछ और जोड़िए।",
        )

        /** The word at [index] of [sentence]'s words as written there, without punctuation ("Patna" for "Patna,"). */
        fun writtenWord(sentence: Sentence, index: Int): String {
            var seen = 0
            for (token in Normalizer.tokens(sentence.text)) {
                val n = token.words.size
                if (index < seen + n) {
                    val parts = token.text.split('-').filter { Normalizer.tokenWords(it).isNotEmpty() }
                    val text = if (parts.size == n) parts[index - seen] else token.text
                    return text.trim { !it.isLetterOrDigit() }
                }
                seen += n
            }
            return sentence.targetWords[index]
        }

        /** What Hindi help says on each screen: the pilot's lines where it has the screen, drafted ones otherwise. */
        fun helpText(state: AppState): String = when (state.screen) {
            Screen.Students -> "अपना कार्ड चुनिए। नए छात्र हों तो ‘नया छात्र’ दबाइए।"
            Screen.NewStudent -> "अपना रंग और अपना चित्र चुनिए। फिर सहायक आपका नाम लिखेंगे।"
            Screen.Home -> "नमस्ते! आज के पाँच वाक्य पूरे कीजिए। शुरू करने के लिए बड़ा बटन दबाइए।"
            is Screen.Practice, Screen.SpeechCheck -> "पहले वाक्य सुनिए। फिर नारंगी माइक बटन दबाइए और वाक्य बोलिए।"
            is Screen.Feedback -> state.result?.let { r ->
                "${r.score.total} में से ${r.score.correct} शब्द सही। " + (r.tip?.template?.textHi ?: "")
            }.orEmpty().trim()
            // The pilot's line for each step.
            is Screen.Fix -> when (state.fix?.state?.step) {
                1 -> "यह शब्द धीरे सुनिए, फिर आगे दबाइए।"
                2 -> "शब्द को टुकड़ों में देखिए और सुनिए।"
                3 -> "माइक दबाइए और सिर्फ़ यह शब्द बोलिए।"
                4 -> "माइक दबाइए और यह हिस्सा बोलिए।"
                else -> "माइक दबाइए और पूरा वाक्य बोलिए।"
            }
            Screen.GoalDone -> "बहुत बढ़िया! आज का लक्ष्य पूरा हुआ। कल फिर आइए।"
            Screen.Results -> "यहाँ सभी छात्रों के नतीजे हैं।"
        }
    }
}
