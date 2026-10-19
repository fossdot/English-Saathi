package net.bodhya.speak.domain

/** Stars for one attempt: [correct] of [total] target words were right. */
data class Score(val correct: Int, val total: Int, val stars: Int)

/** Stars and XP, as in the pilot's `score()` and `handleAttempt()`. */
object Scorer {

    /**
     * 3 stars when every target word is correct, 2 when at least
     * [GameConfig.twoStarRatio] of them are, otherwise 1. Extra words never
     * cost stars. An empty target counts as all correct.
     */
    fun score(alignment: Alignment, config: GameConfig): Score {
        val correct = alignment.correctCount
        val total = alignment.total
        val stars = when {
            correct == total -> 3
            correct.toDouble() / total >= config.twoStarRatio -> 2
            else -> 1
        }
        return Score(correct, total, stars)
    }

    /** XP for one recognised sentence attempt: effort plus a bonus per star. */
    fun sentenceAttemptXp(stars: Int, config: GameConfig): Int = config.attemptXp + config.starXp * stars
}
