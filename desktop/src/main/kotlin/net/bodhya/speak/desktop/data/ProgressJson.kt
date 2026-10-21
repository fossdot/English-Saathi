package net.bodhya.speak.desktop.data

import kotlinx.serialization.Serializable
import net.bodhya.speak.domain.LeitnerEntry
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.StreakState
import java.time.LocalDate

/**
 * [LearnerProgress] as saved in `progress.json`. Days are ISO dates
 * (2026-10-21). The Leitner box is a list, in the order the words entered
 * it, because "Practise again tomorrow" lists them in that order
 * (docs/decisions.md, "Keep the Leitner box order in storage").
 */
@Serializable
internal data class ProgressJson(
    val xpTotal: Int = 0,
    val xpByDay: Map<String, Int> = emptyMap(),
    val doneByDay: Map<String, List<String>> = emptyMap(),
    val bestStars: Map<String, Int> = emptyMap(),
    val streak: StreakJson = StreakJson(),
    val leitner: List<LeitnerJson> = emptyList(),
    val goalCelebratedOn: String? = null,
) {
    fun toProgress() = LearnerProgress(
        xpTotal = xpTotal,
        xpByDay = xpByDay.mapKeys { LocalDate.parse(it.key) },
        doneByDay = doneByDay.mapKeys { LocalDate.parse(it.key) },
        bestStars = bestStars,
        streak = StreakState(streak.days, streak.lastPracticeDay?.let(LocalDate::parse), streak.pausedSince?.let(LocalDate::parse)),
        leitner = leitner.associate { it.word to LeitnerEntry(it.box, LocalDate.parse(it.due)) },
        goalCelebratedOn = goalCelebratedOn?.let(LocalDate::parse),
    )

    companion object {
        fun of(p: LearnerProgress) = ProgressJson(
            xpTotal = p.xpTotal,
            xpByDay = p.xpByDay.mapKeys { it.key.toString() },
            doneByDay = p.doneByDay.mapKeys { it.key.toString() },
            bestStars = p.bestStars,
            streak = StreakJson(p.streak.days, p.streak.lastPracticeDay?.toString(), p.streak.pausedSince?.toString()),
            leitner = p.leitner.map { (word, e) -> LeitnerJson(word, e.box, e.due.toString()) },
            goalCelebratedOn = p.goalCelebratedOn?.toString(),
        )
    }
}

@Serializable
internal data class StreakJson(val days: Int = 0, val lastPracticeDay: String? = null, val pausedSince: String? = null)

@Serializable
internal data class LeitnerJson(val word: String, val box: Int, val due: String)
