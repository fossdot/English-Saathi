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
import net.bodhya.speak.desktop.app.StudentCard
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.content.PackCheck
import net.bodhya.speak.desktop.content.PackInUse
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.voice.Message
import java.io.File

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
    /** "Open all lessons" for one student. */
    val setOpenAll: (id: String, open: Boolean) -> Unit,
    /** The macOS Open dialog for a lesson pack; null if cancelled. */
    val choosePack: () -> File?,
    /** Checks a pack file; changes nothing. */
    val checkPack: (File) -> PackCheck,
    /** Adds a checked pack; returns what to tell the facilitator. */
    val installPack: (PackCheck.Ready) -> Message,
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
 * students (rename, delete, "Open all lessons"), the lessons and Add
 * lessons, the speech model, the microphone input and its 1-second check,
 * the voices found, the data folder, and the speech check with its test
 * clips (milestone D8).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    model: SpeechModel,
    input: String?,
    inputs: List<String>,
    /** The Mac's built-in microphone, labelled so in the list. */
    builtIn: String?,
    clips: List<String>,
    students: List<StudentCard>,
    packs: List<PackInUse>,
    info: SettingsInfo,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxSize().background(Colors.bg).verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Bilingual("Settings", "सेटिंग", size = 26.sp, bold = true)
        MessageBox(MessageKind.INFO, TEACHER_NOTE)
        Line("Students", bold = true)
        if (students.isEmpty()) Line("No students yet.", color = Colors.muted)
        for (card in students) StudentRow(card.learner, card.openAll, actions)
        Line("Lessons", bold = true)
        for (p in packs) {
            Line(
                "${p.pack.titleEn} · ${p.pack.titleHi}: version ${p.pack.version}, ${p.pack.lessons.size} " +
                    (if (p.pack.lessons.size == 1) "lesson" else "lessons") + if (p.builtIn) " (comes with the app)" else " (added)",
                color = Colors.muted,
            )
        }
        AddLessons(actions)
        Line("Speech model (every result is logged with its model)", bold = true)
        Row(horizontalArrangement = Gap) {
            for (m in SpeechModel.entries) {
                PilotButton(m.id + if (m == model) " ✓" else "", if (m == SpeechModel.TINY_EN) "तेज़" else "ज़्यादा सही", { actions.chooseModel(m) }, primary = m == model)
            }
        }
        Line("Microphone", bold = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // The built-in microphone first: it is the default, and can't switch to an iPhone or headphones on its own.
            for (name in inputs.filter { it != "Default Audio Device" }.sortedByDescending { it == builtIn }) {
                val label = if (name == builtIn) "$name (built-in)" else name
                PilotButton(label + if (name == input) " ✓" else "", if (name == builtIn) "Mac में लगा माइक" else "यह माइक", { actions.chooseInput(name) }, primary = name == input)
            }
            PilotButton("Mac's default" + if (input == null) " ✓" else "", "Mac का अपना माइक", { actions.chooseInput(null) }, primary = input == null)
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

/**
 * Settings › Add lessons: the facilitator picks a pack file; it is checked
 * (nothing changes yet), what it adds is shown in English and Hindi, and
 * only then installed. A pack that can't be added says why.
 */
@Composable
private fun AddLessons(actions: SettingsActions) {
    var check by remember { mutableStateOf<PackCheck?>(null) }
    var done by remember { mutableStateOf<Message?>(null) }
    PilotButton("Add lessons…", "पाठ जोड़िए", {
        done = null
        check = actions.choosePack()?.let(actions.checkPack)
    })
    done?.let { MessageBox(MessageKind.OK, it) }
    when (val c = check) {
        null -> Unit
        is PackCheck.Rejected -> MessageBox(MessageKind.PROBLEM, c.message)
        is PackCheck.Ready -> Column(
            Modifier.fillMaxWidth().background(Colors.surface, RoundedCornerShape(16.dp)).padding(14.dp).testTag("pack-preview"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val p = c.preview
            Bilingual(
                "“${p.titleEn}”, version ${p.version}" + (p.replaces?.let { " (replaces version $it)" } ?: ""),
                "“${p.titleHi}”, संस्करण ${p.version}" + (p.replaces?.let { " (संस्करण $it की जगह)" } ?: ""),
                bold = true,
            )
            for (line in p.lessons) {
                val tag = if (line.new) "new" to "नया" else "updated" to "बदला"
                Bilingual(
                    "${line.titleEn}: ${line.sentences} ${if (line.sentences == 1) "sentence" else "sentences"} (${tag.first})",
                    "${line.titleHi}: ${line.sentences} वाक्य (${tag.second})",
                    size = 16.sp,
                )
            }
            for (line in p.removed) {
                Bilingual("${line.titleEn} is no longer in this pack", "${line.titleHi} अब इस पैक में नहीं है", size = 16.sp, color = Colors.badInk)
            }
            // Drafted for the desktop app; needs the teacher review.
            Bilingual("Students’ progress is kept.", "छात्रों की प्रगति बनी रहेगी।", size = 16.sp, color = Colors.muted)
            Row(horizontalArrangement = Gap) {
                PilotButton("Add these lessons", "ये पाठ जोड़िए", {
                    done = actions.installPack(c)
                    check = null
                }, primary = true)
                PilotButton("Cancel", "रहने दें", { check = null })
            }
        }
    }
}

/** One student: rename, delete after a confirmation (docs/decisions.md, "Deleting a student"), or open all lessons. */
@Composable
private fun StudentRow(learner: Learner, openAll: Boolean, actions: SettingsActions) {
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
                PilotButton("Open all lessons" + if (openAll) " ✓" else "", "सारे पाठ खोलिए", { actions.setOpenAll(learner.id, !openAll) }, primary = openAll)
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
