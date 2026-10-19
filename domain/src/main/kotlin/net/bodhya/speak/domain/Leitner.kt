package net.bodhya.speak.domain

import java.time.LocalDate

/** A word waiting for revision: its box (1-based) and the day it is due. */
data class LeitnerEntry(val box: Int, val due: LocalDate) {
    init {
        require(box >= 1) { "Leitner boxes start at 1, got $box" }
    }
}

/**
 * Simple Leitner box for words to revise, as in the pilot's `updateRevision()`.
 *
 * - A word said wrong or missed goes to box 1, due after the first interval
 *   (tomorrow), whatever box it was in.
 * - A word said correctly while due (due today or earlier) moves up one box and
 *   is due after that box's interval, counted from today. Correct after the
 *   last box, it leaves the box ("learned").
 * - A word said correctly before it is due, or not in the box, is unchanged.
 *
 * Words are kept in the order they entered the box.
 */
object Leitner {

    /**
     * Applies one attempt's [results] in sentence order. A word that appears
     * twice in a sentence is updated twice, in order. Due dates written while
     * the phone's date was wrong are pulled back first (see [pullBack]).
     */
    fun update(
        box: Map<String, LeitnerEntry>,
        results: List<WordResult>,
        today: LocalDate,
        config: GameConfig,
    ): Map<String, LeitnerEntry> {
        val intervals = config.leitnerIntervalsDays
        val next = pullBack(box, today, config).toMutableMap()
        for (r in results) {
            val current = next[r.word]
            if (r.status != WordStatus.CORRECT) {
                next[r.word] = LeitnerEntry(1, today.plusDays(intervals[0].toLong()))
            } else if (current != null && !current.due.isAfter(today)) {
                val newBox = current.box + 1
                if (newBox > intervals.size) {
                    next.remove(r.word)
                } else {
                    next[r.word] = LeitnerEntry(newBox, today.plusDays(intervals[newBox - 1].toLong()))
                }
            }
        }
        return next
    }

    /**
     * Pulls back due dates further ahead than their box's interval from
     * [today], which only happens when the phone's date was wrong. [update]
     * stores the result; [due] and [forTomorrow] only read through it.
     */
    fun pullBack(box: Map<String, LeitnerEntry>, today: LocalDate, config: GameConfig): Map<String, LeitnerEntry> =
        box.mapValues { (_, entry) ->
            val intervals = config.leitnerIntervalsDays
            val latest = today.plusDays(intervals[(entry.box - 1).coerceIn(intervals.indices)].toLong())
            if (entry.due.isAfter(latest)) entry.copy(due = latest) else entry
        }

    /** Words due on [day] or earlier, as seen on [today] (after [pullBack]). */
    private fun dueBy(box: Map<String, LeitnerEntry>, day: LocalDate, today: LocalDate, config: GameConfig): List<String> =
        pullBack(box, today, config).filterValues { !it.due.isAfter(day) }.keys.toList()

    /** Words due for revision today. */
    fun due(box: Map<String, LeitnerEntry>, today: LocalDate, config: GameConfig): List<String> =
        dueBy(box, today, today, config)

    /** "Practise again tomorrow": words due tomorrow or earlier. */
    fun forTomorrow(box: Map<String, LeitnerEntry>, today: LocalDate, config: GameConfig): List<String> =
        dueBy(box, today.plusDays(1), today, config)
}
