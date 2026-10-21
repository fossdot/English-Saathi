package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.ResultsData
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.data.AttemptResult
import net.bodhya.speak.desktop.data.Results
import net.bodhya.speak.desktop.voice.Message

/** What the Results screen offers. */
class ResultsActions(
    val back: () -> Unit,
    /** Opens the macOS Save dialog for the CSV. */
    val save: () -> Unit,
    val help: () -> Unit,
)

/** The note that the English and Hindi lines still need a teacher (docs/decisions.md, P5): on Results and Settings only. */
val TEACHER_NOTE = Message("To be checked by an English teacher", "अंग्रेज़ी शिक्षक की जाँच बाकी है")

/**
 * The pilot's Results screen (milestone D8), for the facilitator: a line per
 * student, then every attempt, newest first, and the CSV through the macOS
 * Save dialog. [data] is null while the attempt log is being read.
 */
@Composable
fun ResultsScreen(state: AppState, data: ResultsData?, sentences: Int, actions: ResultsActions) {
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Gap) {
            RoundIconButton(Icons.back, "Back to students", actions.back, plain = true)
            Bilingual("Results", "नतीजे", Modifier.weight(1f), size = 28.sp, bold = true)
            if (data != null && data.rows.isNotEmpty()) {
                PilotButton("Save as CSV…", "फ़ाइल सेव करें", actions.save)
            }
            HelpButton(actions.help)
        }
        MessageBox(MessageKind.INFO, TEACHER_NOTE)
        state.message?.let { MessageBox(it.kind, it.message) }
        when {
            data == null -> Line("Reading the results…", color = Colors.muted)
            !data.read -> Unit // the message above says why
            data.rows.isEmpty() -> MessageBox(MessageKind.INFO, Message("No attempts yet. Results appear here after students practise.", "अभी कोई नतीजा नहीं है।"))
            else -> {
                Table(
                    listOf("Student" to 2f, "Sentence attempts" to 1.3f, "Not heard" to 1f, "Sentences tried" to 1.2f, "Sentences with 3 stars" to 1.4f, "Words correct" to 1.3f),
                    data.summary.map { s ->
                        listOf(s.name, "${s.attempts}", "${s.notHeard}", "${s.sentencesTried} / $sentences", "${s.threeStars}", "${s.correct} of ${s.total}")
                    },
                    Modifier.testTag("summary"),
                )
                Bilingual("Every attempt (newest first)", "हर कोशिश", size = 20.sp, bold = true)
                Table(
                    listOf("Time" to 1.3f, "Student" to 1.4f, "Step" to 1f, "Target" to 2.2f, "We heard" to 2.2f, "Correct" to 0.9f),
                    data.recent.map { (learner, r) ->
                        listOf(
                            Results.shortTime(r.time), learner.name, r.mode, r.target,
                            r.heard.ifEmpty { "–" },
                            if (r.result == AttemptResult.NOT_HEARD) "not heard" else "${r.correct} / ${r.total}",
                        )
                    },
                    Modifier.testTag("attempts"),
                )
                if (data.rows.size > data.recent.size) {
                    Line("The ${data.recent.size} newest of ${data.rows.size} attempts. The CSV has them all.", color = Colors.muted)
                }
            }
        }
    }
}

/** A plain table like the pilot's: a grey header row, a line under each row, columns sized by weight. */
@Composable
private fun Table(columns: List<Pair<String, Float>>, rows: List<List<String>>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(shape).border(1.dp, Colors.line, shape)) {
        Row(Modifier.fillMaxWidth().background(Colors.surface)) {
            for ((title, weight) in columns) Cell(title, weight, bold = true)
        }
        for (row in rows) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.line))
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { i, text -> Cell(text, columns[i].second) }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float, bold: Boolean = false) {
    BasicText(
        text,
        style = textStyle(15.sp, Colors.ink, if (bold) FontWeight.SemiBold else FontWeight.Normal, lineHeight = 1.3f),
        modifier = Modifier.weight(weight).padding(horizontal = 10.dp, vertical = 8.dp),
    )
}
