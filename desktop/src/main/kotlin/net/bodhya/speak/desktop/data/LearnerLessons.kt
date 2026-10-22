package net.bodhya.speak.desktop.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bodhya.speak.domain.Lesson

/**
 * Which lessons a learner may practise, saved as `lessons.json` in their
 * folder (docs/decisions.md, "Lessons unlock in order").
 */
@Serializable
data class LearnerLessons(
    /** Where Start practice continues; null at first (the first open lesson). */
    val current: String? = null,
    /** Lessons opened so far, in the order they opened. Kept even when a pack update would lock one again. */
    val opened: List<String> = emptyList(),
    /** Settings › Students › "Open all lessons": every lesson open, for testing or for a student who knows the basics. */
    @SerialName("open_all") val openAll: Boolean = false,
    /**
     * The student chose [current] on Home, so it stays current even when all of
     * it has been said. A lesson reached by moving on gives way to the next one
     * once it is all said, also when that next lesson comes later (a new pack).
     */
    val chosen: Boolean = false,
) {
    fun isOpen(lessonId: String): Boolean = openAll || lessonId in opened

    /**
     * The lessons open after the learner's latest attempt, from [order] (every
     * lesson, in the unlock order) and [said] (sentences said at least once,
     * any stars): the first lesson always, and each next lesson once every
     * sentence of the one before it has been said. Lessons already opened
     * stay open, also ones a pack update has since removed (they come back
     * open if the lesson comes back).
     */
    fun unlocked(order: List<Lesson>, said: Set<String>): LearnerLessons {
        val now = opened.toMutableList()
        order.firstOrNull()?.let { if (it.id !in now) now += it.id }
        for (k in 0 until order.size - 1) {
            val lesson = order[k]
            if (lesson.id in now && lesson.sentences.all { it.id in said } && order[k + 1].id !in now) now += order[k + 1].id
        }
        return copy(opened = now)
    }
}
