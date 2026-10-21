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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.SentenceResult
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Praise
import net.bodhya.speak.domain.FeedbackSelector
import net.bodhya.speak.speech.EngineState

/** What the practice screen offers. */
class PracticeActions(
    val toggleRecording: () -> Unit,
    val listen: (slow: Boolean) -> Unit,
    val home: () -> Unit,
    val students: () -> Unit,
    val help: () -> Unit,
)

/**
 * The pilot's practice screen: the sentence with Listen and Slowly on the
 * left, the mic on the right (milestone D6). Space speaks, L listens, S
 * listens slowly.
 */
@Composable
fun PracticeScreen(state: AppState, mic: Listening.State, recognition: RecognitionState, lesson: Lesson, index: Int, actions: PracticeActions) {
    val learner = state.learner ?: return
    val sentence = lesson.sentences[index]
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton(Icons.close, "Close and go home", actions.home, plain = true)
            // Which sentence of the lesson this is, as the pilot's progress bar shows it.
            Row(Modifier.weight(1f).semantics { contentDescription = "Sentence ${index + 1} of ${lesson.sentences.size}" }, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (i in lesson.sentences.indices) {
                    Box(Modifier.weight(1f).height(10.dp).background(if (i <= index) Colors.primary else Colors.line, RoundedCornerShape(5.dp)))
                }
            }
            WhoPill(learner, actions.students)
            HelpButton(actions.help)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Line("${lesson.titleEn}, sentence ${index + 1} (वाक्य ${index + 1})", color = Colors.muted)
                Column {
                    BasicText(sentence.text, style = textStyle(46.sp, weight = FontWeight.Bold, lineHeight = 1.15f))
                    BasicText(sentence.hi, style = textStyle(24.sp, Colors.muted))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PilotButton("Listen", "सुनिए", { actions.listen(false) }, Modifier.weight(1f), leading = { Icon(Icons.speaker, 24.dp, Colors.primaryInk) })
                    PilotButton("Slowly", "धीरे सुनिए", { actions.listen(true) }, Modifier.weight(1f), leading = { SpeedTag("0.6×") })
                }
            }
            Column(Modifier.weight(1f).background(Colors.surface, RoundedCornerShape(28.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MicButton(mic, recognition, actions.toggleRecording)
                state.message?.let { MessageBox(it.kind, it.message, onPanel = true) }
            }
        }
        KeyHints("Space" to "speak", "L" to "listen", "S" to "slowly")
    }
}

/** The pilot's "0.6×" tag on the Slowly buttons. */
@Composable
fun SpeedTag(text: String) {
    Box(Modifier.background(Colors.primarySoft, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        BasicText(text, style = textStyle(15.sp, Colors.primaryInk))
    }
}

/**
 * The pilot's mic button: orange, a stop square while listening, with a
 * ring that grows with the voice. It can't be pressed while the model loads
 * or the answer is being checked, and says so.
 */
@Composable
fun MicButton(mic: Listening.State, recognition: RecognitionState, onClick: () -> Unit) {
    val ready = recognition.engine is EngineState.Ready
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(172.dp), contentAlignment = Alignment.Center) {
            val ring = if (mic.listening) 10.dp + 8.dp * mic.level else 10.dp
            Box(
                Modifier.size(136.dp + ring * 2).focusProperties { canFocus = false }.testTag("mic")
                    .background(Colors.rewardSoft, CircleShape)
                    .clickable(enabled = ready && !mic.working, onClick = onClick)
                    .semantics { contentDescription = if (mic.listening) "Stop listening" else "Press and speak" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(136.dp).background(if (ready) Colors.reward else Colors.muted, CircleShape), contentAlignment = Alignment.Center) {
                    if (mic.listening) Icon(Icons.stop, 48.dp, Colors.onPrimary) else Icon(Icons.mic, 62.dp, Colors.onPrimary)
                }
            }
        }
        when {
            recognition.engine is EngineState.Loading -> Bilingual("Loading the speech model…", "आवाज़ पहचानने वाला मॉडल खुल रहा है…", color = Colors.muted)
            recognition.engine is EngineState.Failed -> Bilingual(
                "The speech model didn't load. Tell the facilitator.", "आवाज़ पहचानने वाला मॉडल नहीं खुला। सहायक को बताइए।", color = Colors.badInk,
            )
            mic.listening -> Bilingual("Listening…", "सुन रहा है… बोलना बंद करते ही रुक जाएगा", size = 20.sp, bold = true)
            mic.working -> Bilingual("Checking…", "जाँच हो रही है…", size = 20.sp, color = Colors.muted)
            else -> Bilingual("Press and speak", "दबाइए और बोलिए", size = 20.sp, bold = true)
        }
    }
}

/** What the feedback screen offers. */
class FeedbackActions(
    val listen: () -> Unit,
    val fix: () -> Unit,
    val next: () -> Unit,
    val students: () -> Unit,
    val help: () -> Unit,
)

/**
 * The pilot's feedback screen: stars, praise, "n of m words correct", the
 * words as chips, what was heard, the tip for the word to fix, XP, and Fix
 * or Next (milestone D6). Enter is Fix, or Next when every word was right.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(learner: Learner, result: SentenceResult, lesson: Lesson, actions: FeedbackActions) {
    val sentence = lesson.sentences[result.sentence]
    val score = result.score
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Line("${lesson.titleEn}, sentence ${result.sentence + 1}", color = Colors.muted)
            Box(Modifier.weight(1f))
            WhoPill(learner, actions.students)
            HelpButton(actions.help)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Stars(score.stars, 48.dp)
                val (en, hi) = when (FeedbackSelector.praise(score.stars)) {
                    Praise.EXCELLENT -> "Excellent!" to "शानदार!"
                    Praise.VERY_GOOD -> "Very good!" to "बहुत अच्छा!"
                    Praise.GOOD_TRY -> "Good try!" to "अच्छी कोशिश!"
                }
                Bilingual(en, hi, size = 28.sp, bold = true)
                Bilingual("${score.correct} of ${score.total} words correct", "${score.total} में से ${score.correct} शब्द सही", size = 19.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WordChips(sentence.text, result.alignment.words)
                }
                Line("We heard: “${result.heard.trim()}” (हमने सुना)", size = 16.sp, color = Colors.muted)
                // Saying the last unsaid sentence of a lesson opens the next one.
                for (opened in result.opened) {
                    MessageBox(MessageKind.OK, Message("New lesson open: ${opened.titleEn}", "नया पाठ खुला: ${opened.titleHi}"))
                }
            }
            Column(Modifier.weight(1f).background(Colors.surface, RoundedCornerShape(28.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                result.tip?.let { tip ->
                    Column(Modifier.fillMaxWidth().background(Colors.rewardSoft, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BasicText("Tip · सुझाव", style = textStyle(17.sp, Colors.rewardInk, FontWeight.Bold))
                        val word = result.toFixWritten ?: tip.word
                        BasicText(
                            word + (tip.soundHi?.let { " ($it)" } ?: "") + ": " + tip.template.textHi,
                            style = textStyle(19.sp),
                            modifier = Modifier.testTag("tip"),
                        )
                    }
                }
                PilotButton("Hear the correct sentence", "सही वाक्य सुनिए", actions.listen, Modifier.fillMaxWidth(), leading = { Icon(Icons.play, 22.dp, Colors.primaryInk) })
                BasicText("+${result.xp} XP", style = textStyle(17.sp, Colors.primaryInk, FontWeight.SemiBold))
                if (result.toFix != null) {
                    BigButton("Fix “${result.toFixWritten}”", "इसे ठीक करें", actions.fix)
                    GhostButton("Skip for now", "अभी आगे बढ़ें", actions.next)
                } else {
                    BigButton("Next sentence", "अगला वाक्य", actions.next)
                }
            }
        }
        KeyHints("Enter" to if (result.toFix != null) "fix it" else "next", "L" to "listen")
    }
}

/** The pilot's quiet text button (`.ghost`), English above Hindi (docs/decisions.md, P3). */
@Composable
fun GhostButton(en: String, hi: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp).focusProperties { canFocus = false }.clickable(onClick = onClick).padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(en, style = textStyle(17.sp, Colors.muted, FontWeight.SemiBold, lineHeight = 1.2f))
            BasicText(hi, style = textStyle(16.sp, Colors.muted, lineHeight = 1.2f))
        }
    }
}
