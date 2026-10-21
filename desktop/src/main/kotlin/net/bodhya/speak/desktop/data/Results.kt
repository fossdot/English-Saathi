package net.bodhya.speak.desktop.data

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One learner's line in the Results summary (the pilot's first table). */
data class StudentSummary(
    val name: String,
    /** Sentence attempts that were heard and scored. */
    val attempts: Int,
    /** "Didn't hear you" sentence attempts (docs/decisions.md, "Results summary"). */
    val notHeard: Int,
    /** Different sentences with a scored attempt. */
    val sentencesTried: Int,
    /** Different sentences said with 3 stars at least once. */
    val threeStars: Int,
    /** Scored words said right, of all scored words, over the scored attempts. */
    val correct: Int,
    val total: Int,
)

/**
 * The Results screen and the CSV from the attempt log (milestone D8), as
 * in the pilot with the agreed changes (docs/decisions.md, P6 and "Desktop
 * screens: owner answers").
 */
object Results {

    /** Per learner, in [rows]' learner order, from sentence attempts only, as in the pilot. */
    fun summary(rows: List<LoggedAttempt>): List<StudentSummary> =
        rows.filter { it.record.mode == AttemptRecord.SENTENCE }
            .groupBy { it.learner.id }
            .map { (_, mine) ->
                val scored = mine.map { it.record }.filter { it.result == AttemptResult.SCORED }
                StudentSummary(
                    name = mine.first().learner.name,
                    attempts = scored.size,
                    notHeard = mine.count { it.record.result == AttemptResult.NOT_HEARD },
                    sentencesTried = scored.map { it.sentence }.distinct().size,
                    threeStars = scored.filter { it.stars == 3 }.map { it.sentence }.distinct().size,
                    correct = scored.sumOf { it.correct ?: 0 },
                    total = scored.sumOf { it.total ?: 0 },
                )
            }

    /** The pilot's second table: the latest [limit] attempts of any kind, newest first. */
    fun recent(rows: List<LoggedAttempt>, limit: Int = 200): List<LoggedAttempt> = rows.takeLast(limit).asReversed()

    /** The pilot's time column on screen: "21 Oct, 01:45 PM". */
    fun shortTime(time: String): String = SHORT.format(OffsetDateTime.parse(time))

    /** The CSV's columns: the pilot's nine, then model, result and wait_ms (docs/decisions.md, P6). */
    val COLUMNS = listOf("time", "student", "sentence", "mode", "target", "heard", "correct", "total", "stars", "model", "result", "wait_ms")

    /**
     * The CSV of [rows], oldest first: UTF-8 with a byte-order mark so Excel
     * reads Hindi names, quoted as in the pilot, and the time in the Mac's
     * local time ("2026-10-21 13:45:30"), which Excel reads as a date and time.
     * "Didn't hear you" rows have empty counts.
     */
    fun csv(rows: List<LoggedAttempt>): String {
        val lines = rows.map { (learner, r) ->
            listOf(
                LOCAL.format(OffsetDateTime.parse(r.time)), learner.name, r.sentence, r.mode, r.target, r.heard,
                r.correct, r.total, r.stars, r.model, r.result.text, r.waitMs,
            ).joinToString(",") { quote(it?.toString() ?: "") }
        }
        return "﻿" + COLUMNS.joinToString(",") + "\n" + lines.joinToString("") { it + "\n" }
    }

    /** The file name the Save dialog suggests. */
    fun fileName(today: LocalDate): String = "english-saathi-results-$today.csv"

    private fun quote(s: String): String = if (s.any { it == '"' || it == ',' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private val LOCAL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, hh:mm a", Locale.ENGLISH)
}
