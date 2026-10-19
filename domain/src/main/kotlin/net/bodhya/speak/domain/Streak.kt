package net.bodhya.speak.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A learner's streak as stored. [days] is the count as of [lastPracticeDay];
 * [pausedSince] is the first missed day while the streak is paused. Show the
 * streak with [Streaks.view], which also covers a broken streak.
 */
data class StreakState(
    val days: Int = 0,
    val lastPracticeDay: LocalDate? = null,
    val pausedSince: LocalDate? = null,
)

enum class StreakStatus {
    /** Never practised. */
    NONE,

    /** Practised today or yesterday. */
    ACTIVE,

    /** Days missed, but fewer than [GameConfig.streakResetMissedDays]: the count is kept. */
    PAUSED,

    /** [GameConfig.streakResetMissedDays] or more days missed: shown as 0 until the next practice. */
    BROKEN,
}

/** What the learner sees: the number of days and whether the streak is paused or broken. */
data class StreakView(val days: Int, val status: StreakStatus)

/**
 * Streak rules: the pilot's numbers when practising, plus `pausedSince` and
 * the paused/broken display agreed with the owner (docs/decisions.md).
 */
object Streaks {

    /**
     * A sentence was completed on [today]. The first practice day counts 1;
     * each later practice day adds 1 when fewer than
     * [GameConfig.streakResetMissedDays] days were missed, otherwise the
     * streak starts again at 1. Practising clears [StreakState.pausedSince].
     * Practising again the same day, or on a date before the last practice day
     * (the phone's date went back), leaves the count alone.
     */
    fun recordPractice(state: StreakState, today: LocalDate, config: GameConfig): StreakState {
        val last = state.lastPracticeDay
        if (last != null && !today.isAfter(last)) return state.copy(pausedSince = null)
        val days = when {
            last == null -> 1
            missedDays(last, today) < config.streakResetMissedDays -> state.days + 1
            else -> 1
        }
        return StreakState(days = days, lastPracticeDay = today, pausedSince = null)
    }

    /**
     * The app was opened on [today]. After missed days the streak is paused:
     * [StreakState.pausedSince] is set to the first missed day. Once
     * [GameConfig.streakResetMissedDays] or more days are missed the streak is
     * broken, not paused, so `pausedSince` is cleared. The stored count is
     * kept either way; [view] shows 0 for a broken streak, and the next
     * practice starts again at 1. Keeping it means a phone date that was
     * briefly wrong costs nothing.
     */
    fun onAppOpened(state: StreakState, today: LocalDate, config: GameConfig): StreakState {
        val last = state.lastPracticeDay ?: return state
        val missed = missedDays(last, today)
        return when {
            missed <= 0 -> state
            missed < config.streakResetMissedDays -> state.copy(pausedSince = last.plusDays(1))
            else -> state.copy(pausedSince = null)
        }
    }

    /** What to show on [today]. */
    fun view(state: StreakState, today: LocalDate, config: GameConfig): StreakView {
        val last = state.lastPracticeDay ?: return StreakView(0, StreakStatus.NONE)
        val missed = missedDays(last, today)
        return when {
            missed <= 0 -> StreakView(state.days, StreakStatus.ACTIVE)
            missed < config.streakResetMissedDays -> StreakView(state.days, StreakStatus.PAUSED)
            else -> StreakView(0, StreakStatus.BROKEN)
        }
    }

    /** Whole days between [last] and [today] with no practice (negative if the date went back). */
    private fun missedDays(last: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(last, today) - 1
}
