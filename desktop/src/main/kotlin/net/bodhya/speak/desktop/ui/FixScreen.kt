package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.FixView
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.domain.FixingLoop
import net.bodhya.speak.domain.StreakStatus
import net.bodhya.speak.domain.WordStatus

/** What the fixing screen offers. */
class FixActions(
    val next: () -> Unit,
    val toggleRecording: () -> Unit,
    val listen: (slow: Boolean) -> Unit,
    val back: () -> Unit,
    val skip: () -> Unit,
    val students: () -> Unit,
    val help: () -> Unit,
)

/** The pilot's step titles. */
private val STEP_TITLES = listOf(
    "Listen slowly" to "यह शब्द धीरे सुनिए",
    "Break it into parts" to "टुकड़ों में सुनिए",
    "Say the word" to "सिर्फ़ यह शब्द बोलिए",
    "Say the phrase" to "यह हिस्सा बोलिए",
    "Say the full sentence" to "पूरा वाक्य बोलिए",
)

/**
 * The pilot's fixing screen (milestone D7): five steps for the word to fix,
 * with the step list on the right and Skip always there. Steps 1 and 2 only
 * need Next; steps 3–5 are recorded. Two failed tries at a step go back one
 * step.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixScreen(state: AppState, fix: FixView, mic: Listening.State, recognition: RecognitionState, actions: FixActions) {
    val learner = state.learner ?: return
    val f = fix.state
    val (title, titleHi) = STEP_TITLES[f.step - 1]
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton(Icons.back, "Back to feedback", actions.back, plain = true)
            Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "Step ${f.step} of ${FixingLoop.STEPS}" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in 1..FixingLoop.STEPS) StepDot(n, f.step)
            }
            Box(Modifier.weight(1f))
            WhoPill(learner, actions.students)
            HelpButton(actions.help)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Bilingual("Step ${f.step}: $title", titleHi, size = 24.sp, bold = true)
                Column(
                    Modifier.fillMaxWidth().background(Colors.surface, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (f.step <= 3) {
                        BasicText(fix.written, style = textStyle(72.sp, weight = FontWeight.Bold, lineHeight = 1.1f), modifier = Modifier.testTag("big-word"))
                        fix.soundHi?.let { BasicText(it, style = textStyle(26.sp, Colors.muted)) }
                        if (f.step == 2) {
                            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                                for (syllable in fix.syllables) {
                                    Box(Modifier.background(Colors.bg, RoundedCornerShape(12.dp)).border(2.dp, Colors.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                        BasicText(syllable, style = textStyle(28.sp, Colors.ink, FontWeight.SemiBold))
                                    }
                                }
                            }
                            BasicText(fix.tipHi, style = textStyle(18.sp).copy(textAlign = TextAlign.Center), modifier = Modifier.padding(top = 8.dp))
                        }
                        PilotButton("Listen slowly", "धीरे सुनिए", { actions.listen(true) }, Modifier.padding(top = 12.dp), leading = { SpeedTag("0.6×") })
                    } else {
                        BasicText(
                            FixingLoop.target(f)!!,
                            style = textStyle(46.sp, weight = FontWeight.Bold, lineHeight = 1.15f).copy(textAlign = TextAlign.Center),
                            modifier = Modifier.testTag("target"),
                        )
                        PilotButton("Listen", "सुनिए", { actions.listen(false) }, Modifier.padding(top = 12.dp), leading = { Icon(Icons.speaker, 24.dp, Colors.primaryInk) })
                    }
                }
                state.message?.let { MessageBox(it.kind, it.message) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.fillMaxWidth().background(Colors.surface, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    STEP_TITLES.forEachIndexed { i, (en, hi) ->
                        val n = i + 1
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StepDot(n, f.step)
                            Bilingual(en, hi, size = 17.sp, color = if (n > f.step) Colors.muted else Colors.ink, bold = n == f.step)
                        }
                    }
                }
                if (FixingLoop.isRecordingStep(f.step)) {
                    MicButton(mic, recognition, actions.toggleRecording)
                } else {
                    BigButton("Next", "आगे", actions.next)
                }
                GhostButton("Skip for now", "अभी आगे बढ़ें", actions.skip)
            }
        }
        if (FixingLoop.isRecordingStep(f.step)) KeyHints("Space" to "speak", "L" to "listen", "S" to "slowly")
        else KeyHints("Enter" to "next", "L" to "listen")
    }
}

/** A step's dot: a check when done, the number otherwise; blue for the current step. */
@Composable
private fun StepDot(n: Int, current: Int) {
    val (bg, fg) = when {
        n < current -> Colors.ok to Colors.bg
        n == current -> Colors.primary to Colors.onPrimary
        else -> Colors.line to Colors.muted
    }
    Box(Modifier.size(30.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        if (n < current) Icon(Icons.check, 16.dp, fg) else BasicText("$n", style = textStyle(15.sp, fg, FontWeight.Bold, lineHeight = 1f))
    }
}

/** What the goal screen offers. */
class GoalActions(
    val oneMore: () -> Unit,
    val nextStudent: () -> Unit,
    val done: () -> Unit,
    val help: () -> Unit,
)

/**
 * The pilot's goal screen, once per learner per day (milestone D7): XP
 * today, the streak, and the words to practise again tomorrow, as written.
 * Enter is One more sentence, as in the pilot.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalScreen(state: AppState, streakDays: Int, streakStatus: StreakStatus, tomorrow: List<String>, actions: GoalActions) {
    val learner = state.learner ?: return
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f))
            WhoPill(learner, actions.nextStudent)
            HelpButton(actions.help)
        }
        Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().align(Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Stars(3, 56.dp)
                BasicText("Goal complete, ${learner.name}!", style = textStyle(28.sp, weight = FontWeight.Bold, lineHeight = 1.2f).copy(textAlign = TextAlign.Center))
                BasicText("आज का लक्ष्य पूरा!", style = textStyle(20.sp, Colors.muted))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("+${state.progress.xpOn(state.today)}", "XP today", "आज", Colors.primarySoft, Colors.primaryInk, Modifier.weight(1f))
                // The streak as home shows it: after today's practice it is always active.
                val days = if (streakStatus == StreakStatus.ACTIVE) streakDays else 0
                Stat("$days", if (days == 1) "day" else "days in a row", "लगातार दिन", Colors.rewardSoft, Colors.rewardInk, Modifier.weight(1f))
            }
            if (tomorrow.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().background(Colors.surface, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bilingual("Practise again tomorrow", "ये शब्द कल फिर आएँगे", bold = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (word in tomorrow) WordChip(word, WordStatus.WRONG, 19.sp, marked = false)
                    }
                }
            }
            BigButton("One more sentence", "एक वाक्य और", actions.oneMore)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Next student", "अगला छात्र", actions.nextStudent, Modifier.weight(1f))
                GhostButton("Done for today", "आज के लिए बस", actions.done, Modifier.weight(1f))
            }
        }
    }
}

/** One of the goal screen's two numbers (`.stat`). */
@Composable
private fun Stat(value: String, en: String, hi: String, bg: Color, fg: Color, modifier: Modifier) {
    Column(modifier.background(bg, RoundedCornerShape(18.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(value, style = textStyle(32.sp, fg, FontWeight.Bold, lineHeight = 1.2f))
        BasicText(en, style = textStyle(17.sp, fg))
        BasicText(hi, style = textStyle(16.sp, fg.copy(alpha = 0.8f)))
    }
}
