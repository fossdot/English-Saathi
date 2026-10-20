package net.bodhya.speak.domain

import java.time.LocalDate

/**
 * Everything the practice rules remember about one learner, as in the pilot's
 * per-student record.
 *
 * [doneByDay] lists the different sentence ids completed each day, in the
 * order they were first completed. [bestStars] is the best result per
 * sentence. [goalCelebratedOn] is the last day the lesson-done screen was shown.
 */
data class LearnerProgress(
    val xpTotal: Int = 0,
    val xpByDay: Map<LocalDate, Int> = emptyMap(),
    val doneByDay: Map<LocalDate, List<String>> = emptyMap(),
    val bestStars: Map<String, Int> = emptyMap(),
    val streak: StreakState = StreakState(),
    val leitner: Map<String, LeitnerEntry> = emptyMap(),
    val goalCelebratedOn: LocalDate? = null,
) {
    fun xpOn(day: LocalDate): Int = xpByDay[day] ?: 0

    fun doneOn(day: LocalDate): List<String> = doneByDay[day] ?: emptyList()
}

/** Result of one recording of a whole sentence. */
sealed interface SentenceAttempt {
    /** No words were heard ("Didn't hear you"): nothing is scored or recorded. */
    data object NothingHeard : SentenceAttempt

    /** The attempt was scored; [progress] includes its XP, done mark, streak and Leitner changes. */
    data class Scored(
        val progress: LearnerProgress,
        val alignment: Alignment,
        val score: Score,
        val xp: Int,
    ) : SentenceAttempt
}

/** Result of one recording in the fixing loop, and the learner's progress afterwards. */
data class FixStepAttempt(val progress: LearnerProgress, val result: FixResult)

/** What happens after a sentence, and the learner's progress afterwards. */
data class AfterSentenceResult(val progress: LearnerProgress, val next: AfterSentence)

/**
 * Applies a learner's actions to their progress: the pilot's `handleAttempt()`,
 * `handleFix()` and `afterSentence()` with the agreed changes
 * (docs/decisions.md). A sentence's word data (accepted spellings, unscored
 * words) comes from its [Lesson].
 *
 * `current` parameters are the index of the sentence last shown, or 0 when
 * none has been shown yet (as in the pilot).
 */
object Practice {

    /** The app was opened (or came back) on [today]: notes missed days for the streak. */
    fun openApp(progress: LearnerProgress, today: LocalDate, config: GameConfig): LearnerProgress =
        progress.copy(streak = Streaks.onAppOpened(progress.streak, today, config))

    /**
     * A recording of [sentence], one of [lesson]'s. If words were heard: score
     * it, add XP, mark the sentence done today (updating the streak) and update
     * the Leitner box. Unscored words are aligned but neither counted nor put
     * in the box.
     */
    fun attemptSentence(
        progress: LearnerProgress,
        lesson: Lesson,
        sentence: Sentence,
        heard: String,
        today: LocalDate,
        config: GameConfig,
    ): SentenceAttempt {
        val heardWords = Normalizer.normalizeHeard(heard)
        if (heardWords.isEmpty()) return SentenceAttempt.NothingHeard
        val alignment = Aligner.align(sentence.targetWords, heardWords, lesson.accept).withUnscored(lesson.unscoredIn(sentence))
        val score = Scorer.score(alignment, config)
        val xp = Scorer.sentenceAttemptXp(score.stars, config)
        val updated = progress
            .addXp(xp, today)
            .markDone(sentence.id, score.stars, today, config)
            .let { it.copy(leitner = Leitner.update(it.leitner, alignment.scoredWords(), today, config)) }
        return SentenceAttempt.Scored(updated, alignment, score, xp)
    }

    /**
     * One recording at fixing step 3, 4 or 5 of [sentence], one of [lesson]'s.
     * A pass earns [GameConfig.fixStepXp]; passing step 5 also updates the
     * Leitner box with the step-5 result for every scored word of the
     * sentence.
     */
    fun attemptFixStep(
        progress: LearnerProgress,
        state: FixState,
        lesson: Lesson,
        sentence: Sentence,
        heard: String,
        today: LocalDate,
        config: GameConfig,
    ): FixStepAttempt {
        require(state.sentenceId == sentence.id) { "fixing ${state.sentenceId}, but got sentence ${sentence.id}" }
        val result = FixingLoop.submit(state, heard, lesson.accept, config)
        var updated = progress
        if (result.outcome == FixOutcome.PASSED || result.outcome == FixOutcome.SENTENCE_FIXED) {
            updated = updated.addXp(result.xp, today)
        }
        if (result.outcome == FixOutcome.SENTENCE_FIXED) {
            val sentenceResult = result.alignment!!.withUnscored(lesson.unscoredIn(sentence))
            updated = updated.copy(leitner = Leitner.update(updated.leitner, sentenceResult.scoredWords(), today, config))
        }
        return FixStepAttempt(updated, result)
    }

    /**
     * Leaving a sentence: Next, Skip, or the end of a fix. The first time each
     * day the daily goal is reached, the lesson-done screen is shown (and
     * remembered in the progress); otherwise the next sentence is chosen by
     * [SentencePicker.next].
     */
    fun afterSentence(
        progress: LearnerProgress,
        sentences: List<Sentence>,
        current: Int,
        today: LocalDate,
        config: GameConfig,
    ): AfterSentenceResult {
        val done = progress.doneOn(today)
        if (done.size >= config.dailyGoal && progress.goalCelebratedOn != today) {
            return AfterSentenceResult(progress.copy(goalCelebratedOn = today), AfterSentence.GoalComplete)
        }
        val due = Leitner.due(progress.leitner, today, config).toSet()
        return AfterSentenceResult(progress, AfterSentence.Next(SentencePicker.next(sentences, current, done, due)))
    }

    /** "Start practice" / "One more sentence": the index of the sentence to open. */
    fun startPractice(
        progress: LearnerProgress,
        sentences: List<Sentence>,
        current: Int,
        today: LocalDate,
        config: GameConfig,
    ): Int = SentencePicker.start(sentences, current, progress.doneOn(today), Leitner.due(progress.leitner, today, config).toSet())

    /** Words for the lesson-done screen's "Practise again tomorrow" list. */
    fun wordsForTomorrow(progress: LearnerProgress, today: LocalDate, config: GameConfig): List<String> =
        Leitner.forTomorrow(progress.leitner, today, config)

    /** Unscored words never enter the Leitner box. */
    private fun Alignment.scoredWords(): List<WordResult> = words.filter { it.scored }

    private fun LearnerProgress.addXp(xp: Int, today: LocalDate): LearnerProgress =
        copy(xpTotal = xpTotal + xp, xpByDay = xpByDay + (today to xpOn(today) + xp))

    private fun LearnerProgress.markDone(sentenceId: String, stars: Int, today: LocalDate, config: GameConfig): LearnerProgress {
        val done = doneOn(today)
        return copy(
            doneByDay = doneByDay + (today to if (sentenceId in done) done else done + sentenceId),
            bestStars = bestStars + (sentenceId to maxOf(bestStars[sentenceId] ?: 0, stars)),
            streak = Streaks.recordPractice(streak, today, config),
        )
    }
}
