package englishbolo.domain

import englishbolo.domain.StreakStatus.ACTIVE
import englishbolo.domain.StreakStatus.BROKEN
import englishbolo.domain.StreakStatus.NONE
import englishbolo.domain.StreakStatus.PAUSED
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {

    private val config = GameConfig()
    private val day0 = LocalDate.of(2026, 10, 19)
    private fun day(n: Long): LocalDate = day0.plusDays(n)

    private fun practiseOn(vararg offsets: Long): StreakState =
        offsets.fold(StreakState()) { state, d -> Streaks.recordPractice(state, day(d), config) }

    @Test
    fun `the first practice day counts 1 and the same day adds nothing`() {
        assertEquals(StreakState(1, day0), practiseOn(0))
        assertEquals(StreakState(1, day0), practiseOn(0, 0))
    }

    @Test
    fun `consecutive days add up`() {
        assertEquals(4, practiseOn(0, 1, 2, 3).days)
    }

    @Test
    fun `one or two missed days pause the streak instead of resetting it`() {
        assertEquals(3, practiseOn(0, 1, 3).days) // missed day 2
        assertEquals(3, practiseOn(0, 1, 4).days) // missed days 2 and 3
    }

    @Test
    fun `three missed days reset the streak`() {
        assertEquals(StreakState(1, day(5)), practiseOn(0, 1, 5)) // missed days 2, 3 and 4
        assertEquals(1, practiseOn(0, 1, 30).days)
    }

    @Test
    fun `a phone date before the last practice day is ignored`() {
        assertEquals(StreakState(2, day(1)), practiseOn(0, 1, -5))
        assertEquals(3, practiseOn(0, 1, -5, 2).days)
    }

    @Test
    fun `opening the app after missed days stores pausedSince, then practice clears it`() {
        val practised = practiseOn(0, 1)
        assertEquals(practised, Streaks.onAppOpened(practised, day(2), config)) // nothing missed yet

        val paused = Streaks.onAppOpened(practised, day(3), config)
        assertEquals(StreakState(2, day(1), pausedSince = day(2)), paused)
        assertEquals(paused, Streaks.onAppOpened(paused, day(4), config)) // still the first missed day

        assertEquals(StreakState(3, day(4)), Streaks.recordPractice(paused, day(4), config))
    }

    @Test
    fun `practising on the same day or an earlier date clears pausedSince but keeps the count`() {
        // Only possible with a wrong phone date: the app saw "missed days" that weren't real.
        val paused = Streaks.onAppOpened(practiseOn(0, 1), day(3), config)
        assertEquals(StreakState(2, day(1)), Streaks.recordPractice(paused, day(1), config))
        assertEquals(StreakState(2, day(1)), Streaks.recordPractice(paused, day(-2), config))
    }

    @Test
    fun `opening the app on an earlier date changes nothing`() {
        val practised = practiseOn(0, 1)
        assertEquals(practised, Streaks.onAppOpened(practised, day(-3), config))
    }

    @Test
    fun `after three missed days the streak shows 0 but keeps its stored count`() {
        val broken = Streaks.onAppOpened(practiseOn(0, 1), day(5), config)
        assertEquals(StreakState(2, day(1)), broken)
        assertEquals(StreakView(0, BROKEN), Streaks.view(broken, day(5), config))
        assertEquals(StreakState(1, day(5)), Streaks.recordPractice(broken, day(5), config))
    }

    @Test
    fun `a broken streak is not paused`() {
        val paused = Streaks.onAppOpened(practiseOn(0, 1), day(3), config)
        assertEquals(StreakState(2, day(1)), Streaks.onAppOpened(paused, day(5), config))
    }

    @Test
    fun `a phone date briefly set ahead costs nothing`() {
        val state = practiseOn(0, 1, 2, 3, 4) // 5 days, last on day 4
        val wrong = Streaks.onAppOpened(state, day(14), config) // opened with the date 10 days ahead
        assertEquals(StreakView(0, BROKEN), Streaks.view(wrong, day(14), config))
        // The date is corrected: the count is still there, and practice adds to it.
        assertEquals(StreakView(5, ACTIVE), Streaks.view(wrong, day(5), config))
        assertEquals(6, Streaks.recordPractice(wrong, day(5), config).days)
    }

    @Test
    fun `what learners see`() {
        val state = practiseOn(0, 1, 2) // 3 days, last on day 2
        assertEquals(StreakView(0, NONE), Streaks.view(StreakState(), day0, config))
        assertEquals(StreakView(3, ACTIVE), Streaks.view(state, day(2), config)) // practised today
        assertEquals(StreakView(3, ACTIVE), Streaks.view(state, day(3), config)) // practised yesterday
        assertEquals(StreakView(3, PAUSED), Streaks.view(state, day(4), config)) // 1 missed day
        assertEquals(StreakView(3, PAUSED), Streaks.view(state, day(5), config)) // 2 missed days
        assertEquals(StreakView(0, BROKEN), Streaks.view(state, day(6), config)) // 3 missed days
        assertEquals(StreakView(3, ACTIVE), Streaks.view(state, day(-4), config)) // date went back
    }

    @Test
    fun `the reset threshold comes from the config`() {
        val strict = GameConfig(streakResetMissedDays = 1)
        val state = Streaks.recordPractice(StreakState(), day0, strict)
        assertEquals(2, Streaks.recordPractice(state, day(1), strict).days)
        assertEquals(1, Streaks.recordPractice(state, day(2), strict).days)
        assertEquals(StreakView(0, BROKEN), Streaks.view(state, day(2), strict))
        assertEquals(StreakState(1, day0), Streaks.onAppOpened(state, day(2), strict)) // broken, so not paused

        val lenient = GameConfig(streakResetMissedDays = 5)
        assertEquals(day(1), Streaks.onAppOpened(state, day(5), lenient).pausedSince) // 4 missed days: paused
    }
}
