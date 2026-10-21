package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.LessonRow
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.StreakStatus
import net.bodhya.speak.domain.Streaks

/** What home offers. */
class HomeActions(
    val start: () -> Unit,
    val open: (sentence: Int) -> Unit,
    /** An open lesson in the list: it becomes the current lesson. */
    val chooseLesson: (id: String) -> Unit,
    val students: () -> Unit,
    val help: () -> Unit,
)

/**
 * The learner's home (the pilot's home screen): today's goal, the current
 * [lesson]'s sentences with their best stars, the streak and XP. With
 * several lessons, every lesson follows in the unlock order: open ones can
 * be chosen, later ones show with a lock and their names (docs/decisions.md,
 * "Lessons unlock in order"). With one lesson, the places to come show as
 * locked, as in the pilot.
 */
@Composable
fun HomeScreen(state: AppState, lesson: Lesson, config: GameConfig, actions: HomeActions) {
    val learner = state.learner ?: return
    val progress = state.progress
    val done = progress.doneOn(state.today).size
    val streak = Streaks.view(progress.streak, state.today, config)
    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WhoPill(learner, actions.students)
            // Paused: the count, marked "paused" (docs/decisions.md, P4); broken: 0 until the next practice.
            Pill(
                daysText(streak.days), Colors.rewardSoft, Colors.rewardInk, Icons.flame,
                below = if (streak.status == StreakStatus.PAUSED) "paused · रुका हुआ" else null,
            )
            Pill("${progress.xpTotal} XP", Colors.primarySoft, Colors.primaryInk)
            Spacer(Modifier.weight(1f))
            HelpButton(actions.help)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1.15f)) { Goal(done, config.dailyGoal, actions.start) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Bilingual(lesson.titleEn, lesson.titleHi, size = 20.sp, bold = true)
                lesson.sentences.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth().focusProperties { canFocus = false }
                            .background(Colors.surface, RoundedCornerShape(18.dp))
                            .clickable { actions.open(i) }
                            .testTag("sentence-$i")
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(s.text, style = textStyle(17.sp, Colors.ink, FontWeight.SemiBold, lineHeight = 1.25f))
                            BasicText(s.hi, style = textStyle(16.sp, Colors.muted, lineHeight = 1.25f))
                        }
                        progress.bestStars[s.id]?.let { Stars(it, 18.dp) }
                    }
                }
                if (state.lessons.size > 1) {
                    Bilingual("Lessons", "पाठ", Modifier.padding(top = 8.dp), size = 20.sp, bold = true)
                    for (row in state.lessons) LessonButton(row) { actions.chooseLesson(row.lesson.id) }
                } else {
                    // The places to come (docs/decisions.md, P2), until lesson packs bring real lessons.
                    for ((en, hi) in LOCKED) LockedRow(en, hi)
                }
            }
        }
        KeyHints("Enter" to "start")
    }
}

/** A lesson in Home's list: the current one marked, open ones with how many sentences were said, locked ones with a lock. */
@Composable
private fun LessonButton(row: LessonRow, onClick: () -> Unit) {
    if (!row.open) return LockedRow(row.lesson.titleEn, row.lesson.titleHi)
    val total = row.lesson.sentences.size
    Row(
        Modifier.fillMaxWidth().focusProperties { canFocus = false }
            .background(if (row.current) Colors.primarySoft else Colors.surface, RoundedCornerShape(18.dp))
            .clickable(enabled = !row.current, onClick = onClick)
            .testTag("lesson-${row.lesson.id}")
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Bilingual(row.lesson.titleEn, row.lesson.titleHi, Modifier.weight(1f), size = 17.sp, bold = row.current)
        Bilingual("${row.said} of $total said", "$total में से ${row.said} बोले", size = 16.sp, color = Colors.muted)
    }
}

@Composable
private fun LockedRow(en: String, hi: String) {
    Row(
        Modifier.fillMaxWidth().background(Colors.locked, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Bilingual(en, hi, Modifier.weight(1f), size = 17.sp, color = Colors.lockedInk)
        Icon(Icons.lock, 22.dp, Colors.lockedInk, description = "locked")
    }
}

/** The pilot's goal panel: done of goal, one bar per sentence, and the big start button. */
@Composable
private fun Goal(done: Int, goal: Int, start: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Colors.primary, RoundedCornerShape(24.dp)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Bilingual("Today’s goal", "आज का लक्ष्य", Modifier.weight(1f), size = 22.sp, color = Colors.onPrimary, bold = true)
            BasicText("${minOf(done, goal)} / $goal", style = textStyle(32.sp, Colors.onPrimary, FontWeight.Bold), modifier = Modifier.testTag("goal"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until goal) {
                Box(Modifier.weight(1f).height(12.dp).background(if (i < done) Colors.reward else Colors.onPrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp)))
            }
        }
        if (done >= goal) {
            BigButton("Practise more", "और अभ्यास करें", start, background = Colors.onPrimary, foreground = Colors.primary)
        } else {
            BigButton("Start practice", "अभ्यास शुरू करें", start, background = Colors.onPrimary, foreground = Colors.primary)
        }
    }
}

private val LOCKED = listOf("Market" to "बाज़ार", "Bank" to "बैंक", "Hospital" to "अस्पताल")
