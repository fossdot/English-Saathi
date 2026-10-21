package net.bodhya.speak.desktop.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.ShownMessage
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.desktop.data.NameProblem
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Practice
import net.bodhya.speak.domain.StreakView
import net.bodhya.speak.domain.Streaks
import java.time.Clock
import java.time.LocalDate

/** The main window's screens, as in the pilot's flow. */
sealed interface Screen {
    /** "Who is practising?": where the app always starts (docs/decisions.md, P7). */
    data object Students : Screen

    /** The learner chooses a card; the facilitator types the name. */
    data object NewStudent : Screen

    data object Home : Screen

    /** Practising sentence [sentence] of the lesson (D6). */
    data class Practice(val sentence: Int) : Screen

    /** Milestone D3's speech check, for the facilitator (Settings). */
    data object SpeechCheck : Screen
}

/** A learner as the picker shows them: their card, name and streak. */
data class StudentCard(val learner: Learner, val streak: StreakView)

data class AppState(
    val screen: Screen = Screen.Students,
    val students: List<StudentCard> = emptyList(),
    /** Who is practising; null on the picker. */
    val learner: Learner? = null,
    val progress: LearnerProgress = LearnerProgress(),
    val today: LocalDate,
    val message: ShownMessage? = null,
)

/**
 * The flow between the student picker, the new-student card and home
 * (milestone D5), without the window. Every change to a learner's progress
 * is saved at once, so quitting or a power cut loses nothing.
 */
class AppController(
    private val store: LearnerStore,
    val lesson: Lesson,
    private val output: SpeechOutput,
    val config: GameConfig = GameConfig(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val log: (String) -> Unit = {},
) {
    private val _state = MutableStateFlow(AppState(today = today()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    /** The sentence last shown, for choosing the next one (0 when none has been shown, as in the pilot). */
    private var current = 0

    init {
        showStudents()
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    fun showStudents() {
        output.stop()
        val today = today()
        _state.update { AppState(Screen.Students, cards(today), today = today) }
    }

    private fun cards(today: LocalDate) = store.learners().map { StudentCard(it, Streaks.view(store.progress(it.id).streak, today, config)) }

    fun newStudent() {
        output.stop()
        _state.update { it.copy(screen = Screen.NewStudent, message = null) }
    }

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
        output.stop()
        val learner = store.learner(id) ?: return showStudents()
        val today = today()
        val stored = store.progress(id)
        val progress = Practice.openApp(stored, today, config)
        if (progress != stored) store.saveProgress(id, progress)
        current = 0
        _state.update { AppState(Screen.Home, cards(today), learner, progress, today) }
    }

    fun home() {
        output.stop()
        _state.update { it.copy(screen = Screen.Home, today = today(), message = null) }
    }

    /** "Start practice" / "Practise more" on home (Enter). */
    fun startPractice() {
        val s = _state.value
        if (s.learner == null) return
        val index = Practice.startPractice(s.progress, lesson.sentences, current, today(), config)
        openSentence(index)
    }

    /** A sentence row on home. */
    fun openSentence(index: Int) {
        require(index in lesson.sentences.indices)
        output.stop()
        current = index
        _state.update { it.copy(screen = Screen.Practice(index), today = today(), message = null) }
    }

    /** The facilitator's speech check, from Settings. */
    fun speechCheck() {
        output.stop()
        _state.update { it.copy(screen = Screen.SpeechCheck, message = null) }
    }

    /** Hindi help: the current screen's instruction, spoken. */
    fun hindiHelp() = output.hindi(helpText(_state.value.screen))

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

    companion object {
        // Messages drafted for the desktop app; they need the teacher review.

        /** The pilot's wording. */
        val NAME_MISSING = Message("Type the student’s name first.", "पहले नाम लिखिए।")
        val NAME_TOO_LONG = Message("Use a shorter name, up to ${LearnerStore.MAX_NAME} letters.", "छोटा नाम लिखिए।")
        val CARD_MISSING = Message("Choose a colour and a picture first.", "पहले रंग और चित्र चुनिए।")
        val CARD_TAKEN = Message("Another student has this card. Choose another colour or picture.", "यह कार्ड किसी और का है। दूसरा रंग या चित्र चुनिए।")

        fun nameTaken(name: String) = Message(
            "There is already a student called $name. Add something to tell them apart, such as the first letter of the surname.",
            "इस नाम का छात्र पहले से है। नाम में कुछ और जोड़िए।",
        )

        /** What Hindi help says on each screen: the pilot's lines where it has the screen, drafted ones otherwise. */
        fun helpText(screen: Screen): String = when (screen) {
            Screen.Students -> "अपना कार्ड चुनिए। नए छात्र हों तो ‘नया छात्र’ दबाइए।"
            Screen.NewStudent -> "अपना रंग और अपना चित्र चुनिए। फिर सहायक आपका नाम लिखेंगे।"
            Screen.Home -> "नमस्ते! आज के पाँच वाक्य पूरे कीजिए। शुरू करने के लिए बड़ा बटन दबाइए।"
            is Screen.Practice, Screen.SpeechCheck -> "पहले वाक्य सुनिए। फिर नारंगी माइक बटन दबाइए और वाक्य बोलिए।"
        }
    }
}
