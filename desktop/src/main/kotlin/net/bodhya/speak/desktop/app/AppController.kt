package net.bodhya.speak.desktop.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.ShownMessage
import net.bodhya.speak.desktop.check.SpeechCheckController
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.data.AttemptRecord
import net.bodhya.speak.desktop.data.AttemptResult
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.desktop.data.NameProblem
import net.bodhya.speak.desktop.practice.Heard
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.desktop.voice.Speeds
import net.bodhya.speak.domain.AfterSentence
import net.bodhya.speak.domain.Alignment
import net.bodhya.speak.domain.FeedbackSelector
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Hints
import net.bodhya.speak.domain.LearnerProgress
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
import java.time.Clock
import java.time.LocalDate

/** The main window's screens, as in the pilot's flow. */
sealed interface Screen {
    /** "Who is practising?": where the app always starts (docs/decisions.md, P7). */
    data object Students : Screen

    /** The learner chooses a card; the facilitator types the name. */
    data object NewStudent : Screen

    data object Home : Screen

    /** Practising sentence [sentence] of the lesson: listen, then speak. */
    data class Practice(val sentence: Int) : Screen

    /** How sentence [sentence] was said ([AppState.result]). */
    data class Feedback(val sentence: Int) : Screen

    /** Fixing the word to fix of sentence [sentence] (D7). */
    data class Fix(val sentence: Int) : Screen

    /** The daily goal was just reached (D7). */
    data object GoalDone : Screen

    /** Milestone D3's speech check, for the facilitator (Settings). */
    data object SpeechCheck : Screen
}

/** A learner as the picker shows them: their card, name and streak. */
data class StudentCard(val learner: Learner, val streak: StreakView)

/** A scored sentence attempt, for the feedback screen. */
data class SentenceResult(
    val sentence: Int,
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
)

/**
 * The flow between the student picker, home, practice and feedback
 * (milestones D5 and D6), without the window, following the pilot with the
 * agreed changes (docs/decisions.md). Every change to a learner's progress
 * is saved at once, so quitting or a power cut loses nothing, and every
 * recording of a sentence is added to the attempt log, "Didn't hear you"
 * included.
 */
class AppController(
    private val store: LearnerStore,
    val lesson: Lesson,
    private val hints: Hints,
    private val output: SpeechOutput,
    /** Records and recognizes answers; null where nothing is recorded (tests of the picker). */
    private val listening: Listening? = null,
    /** Whether the speech model has loaded, so the mic can be used. */
    private val micReady: () -> Boolean = { true },
    val config: GameConfig = GameConfig(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val log: (String) -> Unit = {},
) {
    private val _state = MutableStateFlow(AppState(today = today()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    /** The sentence last shown, for choosing the next one (0 when none has been shown, as in the pilot). */
    private var current = 0

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

    private fun cards(today: LocalDate) = store.learners().map { StudentCard(it, Streaks.view(store.progress(it.id).streak, today, config)) }

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

    /** A learner chose their card: their home screen, with missed days noted for the streak. */
    fun pick(id: String) {
        val learner = store.learner(id) ?: return showStudents()
        val today = today()
        val stored = store.progress(id)
        val progress = Practice.openApp(stored, today, config)
        if (progress != stored) store.saveProgress(id, progress)
        current = 0
        go { AppState(Screen.Home, cards(today), learner, progress, today) }
    }

    fun home() = go { it.copy(screen = Screen.Home, today = today(), message = null, result = null) }

    /** "Start practice" / "Practise more" on home (Enter). */
    fun startPractice() {
        val s = _state.value
        if (s.learner == null) return
        openSentence(Practice.startPractice(s.progress, lesson.sentences, current, today(), config))
    }

    /** A sentence row on home, or the next sentence. */
    fun openSentence(index: Int) {
        require(index in lesson.sentences.indices)
        current = index
        go { it.copy(screen = Screen.Practice(index), today = today(), message = null, result = null) }
    }

    /** Listen (L) and Slowly (S) on the practice screen; "Hear the correct sentence" (L) on feedback. */
    fun listen(slow: Boolean) {
        val sentence = when (val screen = _state.value.screen) {
            is Screen.Practice -> lesson.sentences[screen.sentence]
            is Screen.Feedback -> lesson.sentences[screen.sentence]
            else -> return
        }
        output.english(sentence.text, if (slow) Speeds.SLOW else Speeds.NORMAL)
    }

    /** The mic button and Space on the practice screen: start recording, or stop early. */
    fun toggleRecording() {
        val screen = _state.value.screen as? Screen.Practice ?: return
        val l = listening ?: return
        if (l.state.value.listening) return l.stop()
        if (l.state.value.busy || !micReady()) return
        output.stop()
        _state.update { it.copy(message = null) }
        val token = screenToken
        val sentence = lesson.sentences[screen.sentence]
        l.start { heard ->
            if (token != screenToken) log("dropped a result for ${sentence.id}: the screen moved on")
            else onSentenceHeard(screen.sentence, sentence, heard)
        }
    }

    private fun onSentenceHeard(index: Int, sentence: Sentence, heard: Heard) {
        val s = _state.value
        val learner = s.learner ?: return
        val today = today()
        when (heard) {
            is Heard.Words -> {
                saveRecording(learner, sentence, heard.recording)
                when (val attempt = Practice.attemptSentence(s.progress, lesson, sentence, heard.text, today, config)) {
                    SentenceAttempt.NothingHeard -> {
                        logAttempt(learner, sentence, heard.text, AttemptResult.NOT_HEARD, model = heard.model.id, waitMs = heard.waitMs, recording = heard.recording)
                        show(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR)
                    }
                    is SentenceAttempt.Scored -> {
                        store.saveProgress(learner.id, attempt.progress)
                        logAttempt(
                            learner, sentence, heard.text, AttemptResult.SCORED, attempt.score.correct, attempt.score.total, attempt.score.stars,
                            heard.model.id, heard.waitMs, heard.recording,
                        )
                        val toFix = FeedbackSelector.wordToFix(attempt.alignment)
                        val result = SentenceResult(
                            index, attempt.alignment, attempt.score, attempt.xp, heard.text, toFix,
                            toFix?.let { writtenWord(sentence, it.index) }, toFix?.let { FeedbackSelector.tip(it.word, lesson, hints) },
                        )
                        go { it.copy(screen = Screen.Feedback(index), progress = attempt.progress, today = today, result = result, message = null) }
                    }
                }
            }
            is Heard.NothingLoud -> {
                logAttempt(learner, sentence, "", AttemptResult.NOT_HEARD, recording = heard.recording)
                show(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR)
            }
            is Heard.Silent -> {
                logAttempt(learner, sentence, "", AttemptResult.NOT_HEARD, recording = heard.recording)
                show(MessageKind.PROBLEM, SpeechCheckController.silent(heard.permission))
            }
            is Heard.NoMicrophone -> show(MessageKind.PROBLEM, SpeechCheckController.unavailable(heard.detail))
            is Heard.Failed -> show(MessageKind.PROBLEM, SOMETHING_WRONG)
        }
    }

    /** Next sentence, or Skip: the goal screen the first time today's goal is reached, otherwise the next sentence. */
    fun next() {
        val s = _state.value
        val learner = s.learner ?: return
        val after = Practice.afterSentence(s.progress, lesson.sentences, current, today(), config)
        if (after.progress != s.progress) store.saveProgress(learner.id, after.progress)
        _state.update { it.copy(progress = after.progress) }
        when (val next = after.next) {
            AfterSentence.GoalComplete -> go { it.copy(screen = Screen.GoalDone, message = null, result = null) }
            is AfterSentence.Next -> openSentence(next.index)
        }
    }

    /** Fix "word" on the feedback screen (D7). */
    fun fix() {
        val screen = _state.value.screen as? Screen.Feedback ?: return
        if (_state.value.result?.toFix == null) return
        go { it.copy(screen = Screen.Fix(screen.sentence), message = null) }
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
        sentence: Sentence,
        heard: String,
        result: AttemptResult,
        correct: Int? = null,
        total: Int? = null,
        stars: Int? = null,
        model: String? = null,
        waitMs: Long? = null,
        recording: Recording? = null,
    ) {
        val record = AttemptRecord(
            LearnerStore.now(clock), lesson.id, sentence.id, AttemptRecord.SENTENCE, sentence.text, heard, correct, total, stars, model, result, waitMs,
        )
        store.logAttempt(learner.id, record)
        // The session log also gets what the endpointer saw, for tuning the silence rules on real audio.
        log("attempt | learner=${learner.id} | ${record.mode} | sentence=${sentence.id} | ${result.text} | heard=$heard | " +
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
            is Screen.Fix -> "माइक दबाइए और सिर्फ़ यह शब्द बोलिए।"
            Screen.GoalDone -> "बहुत बढ़िया! आज का लक्ष्य पूरा हुआ। कल फिर आइए।"
        }
    }
}
