package englishbolo.domain

/**
 * Every tunable number of the practice rules, in one place. Defaults match
 * the web pilot's `CONFIG`.
 */
data class GameConfig(
    /** Different sentences to complete each day. */
    val dailyGoal: Int = 5,
    /** XP for every recognised sentence attempt, whatever the result. */
    val attemptXp: Int = 10,
    /** Extra XP per star on a sentence attempt. */
    val starXp: Int = 5,
    /** XP for each passed recording step (3–5) of the fixing loop. */
    val fixStepXp: Int = 5,
    /** Share of correct words needed for 2 stars. */
    val twoStarRatio: Double = 0.6,
    /** Failed recordings at one fixing step before dropping back a step. */
    val fixFailsBeforeDropBack: Int = 2,
    /** Days until a word in Leitner box 1, 2, 3, … is due again. */
    val leitnerIntervalsDays: List<Int> = listOf(1, 3, 7),
    /** Missed days in a row that reset the streak; fewer only pause it. */
    val streakResetMissedDays: Int = 3,
) {
    init {
        require(dailyGoal > 0) { "dailyGoal must be positive" }
        require(twoStarRatio in 0.0..1.0) { "twoStarRatio must be between 0 and 1" }
        require(fixFailsBeforeDropBack > 0) { "fixFailsBeforeDropBack must be positive" }
        require(leitnerIntervalsDays.isNotEmpty() && leitnerIntervalsDays.all { it > 0 }) {
            "leitnerIntervalsDays must be positive day counts"
        }
        require(streakResetMissedDays > 0) { "streakResetMissedDays must be positive" }
    }
}
