package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.StudentCard
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.MicStatus
import net.bodhya.speak.desktop.check.SpeechCheckState
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.domain.StreakStatus

/** What the picker offers. */
class StudentsActions(
    val pick: (id: String) -> Unit,
    val newStudent: () -> Unit,
    val help: () -> Unit,
)

/**
 * "Who is practising?": each learner's card, name and streak (the pilot's
 * student screen). [facilitator] shows what the facilitator should know
 * before a student starts: the speech model and any problem found at start.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentsScreen(state: AppState, facilitator: @Composable () -> Unit, actions: StudentsActions) {
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Gap) {
            Bilingual("Who is practising?", "कौन सीख रहा है?", Modifier.weight(1f), size = 28.sp, bold = true)
            HelpButton(actions.help)
        }
        facilitator()
        if (state.students.isEmpty()) {
            MessageBox(MessageKind.INFO, Message("No students yet. Press New student to add one.", "अभी कोई छात्र नहीं है। ‘नया छात्र’ दबाइए।"))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (card in state.students) StudentButton(card) { actions.pick(card.learner.id) }
            }
        }
        BigButton("New student", "नया छात्र", actions.newStudent, Modifier.width(360.dp), background = Colors.primarySoft, foreground = Colors.primaryInk)
    }
}

@Composable
private fun StudentButton(card: StudentCard, onClick: () -> Unit) {
    Column(
        Modifier.width(170.dp).focusProperties { canFocus = false }
            .background(Colors.surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("student-${card.learner.id}")
            .padding(horizontal = 10.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardBadge(card.learner.card, 72.dp)
        BasicText(card.learner.name, style = textStyle(19.sp, Colors.ink, FontWeight.SemiBold, lineHeight = 1.2f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when (card.streak.status) {
                StreakStatus.NONE -> BasicText("New · नया", style = textStyle(15.sp, Colors.muted))
                else -> {
                    Icon(Icons.flame, 16.dp, Colors.muted)
                    val paused = if (card.streak.status == StreakStatus.PAUSED) " · paused" else ""
                    BasicText(daysText(card.streak.days) + paused, style = textStyle(15.sp, Colors.muted))
                }
            }
        }
    }
}

/**
 * What the facilitator should know before a student starts, at the top of
 * the picker: the speech model, problems found at start (a missing voice,
 * a Java that can't record, a microphone that isn't connected), the
 * start-up microphone check, and saved files that couldn't be read.
 */
@Composable
fun StartNotes(recognition: RecognitionState, notices: List<Message>, check: SpeechCheckState, dataProblems: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModelBadge(recognition)
        for (notice in notices) MessageBox(MessageKind.PROBLEM, notice)
        if (check.mic is MicStatus.Silent || check.mic is MicStatus.Unavailable) check.message?.let { MessageBox(it.kind, it.message) }
        if (dataProblems.isNotEmpty()) {
            // Drafted for the desktop app; needs the teacher review.
            MessageBox(
                MessageKind.PROBLEM,
                Message(
                    "Some saved data could not be read (${dataProblems.size}). Nothing was deleted; the details are in the log (Settings shows where).",
                    "कुछ सेव किया हुआ डेटा पढ़ा नहीं जा सका। कुछ भी मिटाया नहीं गया।",
                ),
            )
        }
    }
}

/** What the new-student screen offers. */
class NewStudentActions(
    /** Returns what is wrong, or null once the student is added. */
    val add: (name: String, card: Card?) -> Message?,
    val back: () -> Unit,
    val help: () -> Unit,
)

/**
 * A new learner chooses a colour and a picture they will recognise; the
 * facilitator types the name for Results and the CSV (docs/decisions.md,
 * P1). Cards other learners have are faded and can't be chosen.
 */
@Composable
fun NewStudentScreen(taken: Set<Card>, message: Message?, actions: NewStudentActions) {
    var colour by remember { mutableStateOf<CardColour?>(null) }
    var symbol by remember { mutableStateOf<CardSymbol?>(null) }
    var name by remember { mutableStateOf("") }
    val card = if (colour != null && symbol != null) Card(colour!!, symbol!!) else null
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Gap) {
            RoundIconButton(Icons.back, "Back to students", actions.back, plain = true)
            Bilingual("New student", "नया छात्र", Modifier.weight(1f), size = 28.sp, bold = true)
            HelpButton(actions.help)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Bilingual("Choose your colour", "अपना रंग चुनिए", size = 22.sp, bold = true)
                // Two rows of four, like the pictures below.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in CardColour.entries.chunked(4)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (c in row) {
                                // A colour whose every picture is taken can't be chosen.
                                val free = CardSymbol.entries.any { Card(c, it) !in taken }
                                Choice(selected = c == colour, enabled = free, description = "colour ${c.name.lowercase()}", onClick = {
                                    colour = c
                                    if (symbol != null && Card(c, symbol!!) in taken) symbol = null
                                }) { Box(Modifier.size(80.dp).background(Cards.colour(c), CircleShape)) }
                            }
                        }
                    }
                }
                Bilingual("Choose your picture", "अपना चित्र चुनिए", size = 22.sp, bold = true)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in CardSymbol.entries.chunked(4)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (s in row) {
                                val free = colour == null || Card(colour!!, s) !in taken
                                Choice(selected = s == symbol, enabled = free, description = "picture ${s.name.lowercase()}", onClick = { symbol = s }) {
                                    Box(
                                        Modifier.size(80.dp).background(colour?.let(Cards::colour) ?: Colors.muted, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Cards.symbol(s), 48.dp, Colors.onPrimary) }
                                }
                            }
                        }
                    }
                }
            }
            Column(
                Modifier.weight(1f).background(Colors.surface, RoundedCornerShape(28.dp)).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (card != null) {
                    CardBadge(card, 120.dp)
                } else {
                    Box(Modifier.size(120.dp).border(3.dp, Colors.line, CircleShape))
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bilingual("Name, typed by the facilitator", "नाम (सहायक लिखेंगे)", bold = true)
                    NameField(name, { name = it }, onEnter = { actions.add(name, card) })
                }
                message?.let { MessageBox(MessageKind.PROBLEM, it, onPanel = true) }
                BigButton("Add student", "जोड़ें", { actions.add(name, card) })
            }
        }
    }
}

/** A colour or picture to choose: ringed when chosen, faded when another learner has it. */
@Composable
private fun Choice(selected: Boolean, enabled: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.focusProperties { canFocus = false }
            .alpha(if (enabled) 1f else 0.25f)
            .border(4.dp, if (selected) Colors.ink else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .padding(5.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description + if (enabled) "" else " (taken)" },
    ) { content() }
}

/** The pilot's name field: Enter adds the student. */
@Composable
private fun NameField(value: String, onChange: (String) -> Unit, onEnter: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= LearnerStore.MAX_NAME + 10) onChange(it.replace("\n", "")) },
        singleLine = true,
        textStyle = textStyle(20.sp),
        cursorBrush = SolidColor(Colors.primary),
        modifier = Modifier.fillMaxWidth().testTag("name")
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) { onEnter(); true } else false
            },
        decorationBox = { field ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .background(Colors.bg, RoundedCornerShape(16.dp))
                    .border(2.dp, if (focused) Colors.primary else Colors.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) BasicText("Name · नाम", style = textStyle(20.sp, Colors.muted))
                field()
            }
        },
    )
}
