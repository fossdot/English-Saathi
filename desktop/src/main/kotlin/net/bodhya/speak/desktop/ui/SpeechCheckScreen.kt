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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.check.CheckResult
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.MicStatus
import net.bodhya.speak.desktop.check.SpeechCheckState
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.WordResult
import net.bodhya.speak.domain.WordStatus
import net.bodhya.speak.speech.EngineState

/** What the facilitator should know about this Mac, shown under the check. */
data class CheckInfo(
    val english: String?,
    val hindi: String?,
    /** Missing voices, a Java that can't use the microphone, …: shown at the top. */
    val notices: List<Message>,
    val logPath: String,
    val speechPrepared: Boolean,
)

/** The actions the screen offers; the controller does the work. */
class CheckActions(
    val selectSentence: (Int) -> Unit,
    val listen: (slow: Boolean) -> Unit,
    val hindiHelp: () -> Unit,
    val toggleRecording: () -> Unit,
)

/** Milestone D3's speech check, in the pilot's laptop layout: the sentence on the left, the mic and result on the right. */
@Composable
fun SpeechCheckScreen(lesson: Lesson, state: SpeechCheckState, recognition: RecognitionState, info: CheckInfo, actions: CheckActions) {
    val sentence = lesson.sentences[state.sentence]
    Column(Modifier.fillMaxSize().background(Colors.bg).verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Gap) {
            Bilingual("Speech check", "बोलकर जाँचिए", Modifier.weight(1f), size = 28.sp, bold = true)
            PilotButton("Hindi help", "हिंदी में सुनिए", actions.hindiHelp, enabled = info.hindi != null)
        }
        for (notice in info.notices) MessageBox(MessageKind.PROBLEM, notice)
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Gap) {
                Line("${lesson.titleEn}, sentence ${state.sentence + 1} (वाक्य ${state.sentence + 1})", color = Colors.muted)
                BasicText(sentence.text, style = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Colors.ink))
                BasicText(sentence.hi, style = TextStyle(fontSize = 24.sp, color = Colors.muted))
                Row(horizontalArrangement = Gap) {
                    PilotButton("Listen", "सुनिए", { actions.listen(false) }, Modifier.weight(1f))
                    PilotButton("Slowly 0.6×", "धीरे सुनिए", { actions.listen(true) }, Modifier.weight(1f))
                }
                Line("Other sentences · दूसरे वाक्य", color = Colors.muted)
                lesson.sentences.forEachIndexed { i, s ->
                    Box(
                        Modifier.fillMaxWidth().focusProperties { canFocus = false }
                            .background(if (i == state.sentence) Colors.primarySoft else Colors.surface, RoundedCornerShape(18.dp))
                            .clickable { actions.selectSentence(i) }.padding(14.dp),
                    ) { Bilingual(s.text, s.hi, size = 17.sp) }
                }
            }
            Column(Modifier.weight(1f).background(Colors.surface, RoundedCornerShape(28.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MicBlock(state, recognition, actions)
                state.message?.let { MessageBox(it.kind, it.message) }
                state.result?.let { ResultBlock(lesson, it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Line("Space speak", color = Colors.muted)
            Line("L listen", color = Colors.muted)
            Line("S slowly", color = Colors.muted)
            Line("Enter speak", color = Colors.muted)
        }
        FacilitatorLine(state, recognition, info)
    }
}

@Composable
private fun MicBlock(state: SpeechCheckState, recognition: RecognitionState, actions: CheckActions) {
    val ready = recognition.engine is EngineState.Ready
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Gap) {
        Box(
            Modifier.size(136.dp).focusProperties { canFocus = false }.testTag("mic")
                .background(if (state.listening) Colors.bad else Colors.reward, CircleShape)
                .border(10.dp, Colors.rewardSoft, CircleShape)
                .clickable(enabled = ready && !state.working) { actions.toggleRecording() },
            contentAlignment = Alignment.Center,
        ) { BasicText(if (state.listening) "■" else "🎤", style = TextStyle(fontSize = 44.sp, color = Colors.onPrimary)) }
        when {
            recognition.engine is EngineState.Loading -> Bilingual("Loading the speech model…", "आवाज़ पहचानने वाला मॉडल खुल रहा है…", color = Colors.muted)
            recognition.engine is EngineState.Failed -> Bilingual(
                "The speech model didn't load: ${(recognition.engine as EngineState.Failed).error.message}",
                "आवाज़ पहचानने वाला मॉडल नहीं खुला।", color = Colors.badInk,
            )
            state.listening -> Bilingual("Listening…", "सुन रहा है… बोलना बंद करते ही रुक जाएगा", bold = true)
            state.working -> Bilingual("Checking…", "जाँच हो रही है…", color = Colors.muted)
            else -> Bilingual("Press and speak", "दबाइए और बोलिए", bold = true)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).background(Colors.line, RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth(state.level).height(10.dp).background(Colors.primary, RoundedCornerShape(5.dp)))
        }
    }
}

@Composable
private fun MessageBox(kind: MessageKind, message: Message) {
    val (bg, fg) = when (kind) {
        MessageKind.PROBLEM -> Colors.badSoft to Colors.badInk
        MessageKind.OK -> Colors.okSoft to Colors.okInk
        MessageKind.INFO -> Colors.bg to Colors.ink
    }
    Box(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(16.dp)).padding(14.dp).testTag("message")) {
        Bilingual(message.en, message.hi, size = 17.sp, color = fg)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultBlock(lesson: Lesson, r: CheckResult) {
    val sentence = lesson.sentences.first { it.id == r.sentenceId }
    Column(verticalArrangement = Gap) {
        r.score?.let { Bilingual("${it.correct} of ${it.total} words correct · ${it.stars} stars", "${it.total} में से ${it.correct} शब्द सही", bold = true) }
        r.words?.let { words -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Chips(sentence.text, words) } }
        r.heard?.let { Bilingual("We heard: “${it.trim()}”", "हमने सुना") }
        Line(
            "${r.model?.id ?: "not run"} · ${r.source} · recording ${"%.1f".format(r.audioMs / 1000.0)} s (${r.endReason.name.lowercase().replace('_', ' ')})" +
                (r.waitMs?.let { " · result after ${"%.2f".format(it / 1000.0)} s" } ?: ""),
            size = 16.sp, color = Colors.muted,
        )
    }
}

/** One chip per word as written ("Patna"), coloured by how it was said; unscored words are neutral. */
@Composable
private fun Chips(text: String, words: List<WordResult>) {
    var next = 0
    for (token in Normalizer.tokens(text)) {
        if (token.words.isEmpty()) continue
        val mine = words.subList(next, minOf(words.size, next + token.words.size))
        next += token.words.size
        val scored = mine.any { it.scored }
        val status = when {
            mine.any { it.status == WordStatus.WRONG } -> WordStatus.WRONG
            mine.any { it.status == WordStatus.MISSING } -> WordStatus.MISSING
            else -> WordStatus.CORRECT
        }
        Chip(token.text.trim('.', ',', '?', '!'), if (scored) status else null)
    }
}

@Composable
private fun Chip(label: String, status: WordStatus?) {
    val shape = RoundedCornerShape(12.dp)
    val (bg, fg, icon) = when (status) {
        WordStatus.CORRECT -> Triple(Colors.okSoft, Colors.okInk, "✓ ")
        WordStatus.WRONG -> Triple(Colors.badSoft, Colors.badInk, "↻ ")
        WordStatus.MISSING -> Triple(Color.Transparent, Colors.badInk, "– ")
        null -> Triple(Colors.bg, Colors.muted, "")
    }
    Box(
        Modifier.background(bg, shape)
            .then(if (status == WordStatus.WRONG) Modifier.border(2.dp, Colors.bad, shape) else Modifier)
            .then(if (status == WordStatus.MISSING) Modifier.drawBehind {
                drawRoundRect(Colors.bad, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))), cornerRadius = CornerRadius(12.dp.toPx()))
            } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { BasicText(icon + label, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = fg)) }
}

@Composable
private fun FacilitatorLine(state: SpeechCheckState, recognition: RecognitionState, info: CheckInfo) {
    val model = when (val e = recognition.engine) {
        EngineState.Loading -> "${recognition.model.id} loading"
        is EngineState.Ready -> "${recognition.model.id} (loaded in ${e.loadMs} ms)"
        is EngineState.Failed -> "${recognition.model.id} failed"
    }
    val mic = when (val m = state.mic) {
        MicStatus.NotChecked -> "not checked"
        MicStatus.Checking -> "checking…"
        is MicStatus.Working -> "working (loudest ${"%.0f".format(m.levelDbfs)} dBFS)"
        is MicStatus.Silent -> "silent (permission: ${m.permission.name.lowercase().replace('_', ' ')})"
        is MicStatus.Unavailable -> "unavailable"
    }
    Column(Modifier.fillMaxWidth().background(Colors.surface, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Line("For the facilitator · ⌘, opens Settings", size = 16.sp, bold = true)
        Line("Model $model · microphone ${state.input ?: "Mac's default"}: $mic", color = Colors.muted)
        Line("Voices: English ${info.english ?: "missing"}, Hindi ${info.hindi ?: "missing"} · lesson speech ${if (info.speechPrepared) "ready" else "being prepared"}", color = Colors.muted)
        Line("Log: ${info.logPath}", color = Colors.muted)
    }
}
