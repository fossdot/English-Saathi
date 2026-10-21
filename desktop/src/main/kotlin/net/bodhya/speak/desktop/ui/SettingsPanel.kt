package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.voice.Message

/** What Settings can do. */
class SettingsActions(
    val chooseModel: (SpeechModel) -> Unit,
    val chooseInput: (String?) -> Unit,
    val checkMicrophone: () -> Unit,
    val openSpeechCheck: () -> Unit,
    val runClip: (String) -> Unit,
    /** Returns what is wrong with the name, or null once renamed. */
    val rename: (id: String, name: String) -> Message?,
    val delete: (id: String) -> Unit,
    /** Shows the data folder in Finder. */
    val openDataFolder: () -> Unit,
)

/** What Settings shows about this Mac. */
class SettingsInfo(
    val english: String?,
    val hindi: String?,
    /** Where to download clearer voices, in Apple's words for this Mac's macOS. */
    val voicesPath: String,
    val dataFolder: String,
    val logFile: String,
)

/**
 * The facilitator's settings (⌘,; no button on the student screens): the
 * students, the speech model, the microphone input and its 1-second check,
 * the voices found, the data folder, and the speech check with its test
 * clips (milestone D8).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    model: SpeechModel,
    input: String?,
    inputs: List<String>,
    clips: List<String>,
    students: List<Learner>,
    info: SettingsInfo,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxSize().background(Colors.bg).verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Bilingual("Settings", "सेटिंग", size = 26.sp, bold = true)
        MessageBox(MessageKind.INFO, TEACHER_NOTE)
        Line("Students", bold = true)
        if (students.isEmpty()) Line("No students yet.", color = Colors.muted)
        for (learner in students) StudentRow(learner, actions)
        Line("Speech model (every result is logged with its model)", bold = true)
        Row(horizontalArrangement = Gap) {
            for (m in SpeechModel.entries) {
                PilotButton(m.id + if (m == model) " ✓" else "", if (m == SpeechModel.TINY_EN) "तेज़" else "ज़्यादा सही", { actions.chooseModel(m) }, primary = m == model)
            }
        }
        Line("Microphone", bold = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PilotButton("Mac's default" + if (input == null) " ✓" else "", "Mac का अपना माइक", { actions.chooseInput(null) }, primary = input == null)
            for (name in inputs.filter { it != "Default Audio Device" }) {
                PilotButton(name + if (name == input) " ✓" else "", "यह माइक", { actions.chooseInput(name) }, primary = name == input)
            }
        }
        PilotButton("Check the microphone (1 second)", "माइक जाँचिए", actions.checkMicrophone)
        Line("Voices", bold = true)
        Line("English: ${info.english ?: "missing"} · Hindi: ${info.hindi ?: "missing"}", color = if (info.english == null || info.hindi == null) Colors.badInk else Colors.ink)
        Line("Clearer voices are a free download: ${info.voicesPath}", color = Colors.muted)
        Line("Data folder", bold = true)
        Line(info.dataFolder, color = Colors.muted)
        Line("Log: ${info.logFile}", color = Colors.muted)
        PilotButton("Open the data folder", "डेटा फ़ोल्डर खोलिए", actions.openDataFolder)
        Line("Speech check (milestone D3): record any sentence and see what the model heard", bold = true)
        PilotButton("Open the speech check", "बोलकर जाँचिए", actions.openSpeechCheck)
        if (clips.isNotEmpty()) {
            Line("Test clips, through the same recorder (shown in the speech check)", bold = true)
            // A wrapping row: in a plain row the last buttons were squeezed until "bus_05" broke over two lines.
            FlowRow(horizontalArrangement = Gap, verticalArrangement = Gap) { for (c in clips) PilotButton(c.removeSuffix(".wav"), "चलाइए", { actions.runClip(c) }) }
        }
    }
}

/** One student: rename, or delete after a confirmation (docs/decisions.md, "Deleting a student"). */
@Composable
private fun StudentRow(learner: Learner, actions: SettingsActions) {
    var mode by remember(learner.id) { mutableStateOf(RowMode.SHOW) }
    var name by remember(learner.id) { mutableStateOf(learner.name) }
    var problem by remember(learner.id) { mutableStateOf<Message?>(null) }
    Column(
        Modifier.fillMaxWidth().background(Colors.surface, RoundedCornerShape(16.dp)).padding(12.dp).testTag("settings-student-${learner.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Gap) {
            CardBadge(learner.card, 40.dp)
            BasicText(learner.name, style = textStyle(18.sp, Colors.ink, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (mode == RowMode.SHOW) {
                PilotButton("Rename", "नाम बदलें", { name = learner.name; problem = null; mode = RowMode.RENAME })
                PilotButton("Delete…", "मिटाएँ", { mode = RowMode.CONFIRM_DELETE })
            }
        }
        when (mode) {
            RowMode.SHOW -> Unit
            RowMode.RENAME -> {
                BasicTextField(
                    name, { name = it.replace("\n", "") }, singleLine = true, textStyle = textStyle(18.sp), cursorBrush = SolidColor(Colors.primary),
                    modifier = Modifier.fillMaxWidth().testTag("rename"),
                    decorationBox = { field ->
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).background(Colors.bg, RoundedCornerShape(12.dp))
                                .border(2.dp, Colors.primary, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { field() }
                    },
                )
                problem?.let { MessageBox(MessageKind.PROBLEM, it) }
                Row(horizontalArrangement = Gap) {
                    PilotButton("Save", "सेव करें", {
                        problem = actions.rename(learner.id, name)
                        if (problem == null) mode = RowMode.SHOW
                    }, primary = true)
                    PilotButton("Cancel", "रहने दें", { mode = RowMode.SHOW })
                }
            }
            RowMode.CONFIRM_DELETE -> {
                // Drafted for the desktop app; needs the teacher review.
                MessageBox(
                    MessageKind.PROBLEM,
                    Message(
                        "Delete ${learner.name}? Their progress, attempts and recordings are removed from this Mac, and their rows leave Results and the CSV. This can't be undone.",
                        "${learner.name} का सारा डेटा इस Mac से मिट जाएगा। यह वापस नहीं आएगा।",
                    ),
                )
                Row(horizontalArrangement = Gap) {
                    PilotButton("Delete ${learner.name}", "हाँ, मिटाएँ", { actions.delete(learner.id) }, primary = true)
                    PilotButton("Cancel", "रहने दें", { mode = RowMode.SHOW })
                }
            }
        }
    }
}

private enum class RowMode { SHOW, RENAME, CONFIRM_DELETE }
