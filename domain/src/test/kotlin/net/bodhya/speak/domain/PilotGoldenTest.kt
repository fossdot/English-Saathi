package net.bodhya.speak.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Replays cases recorded by running the web pilot's own code with the
 * owner-agreed changes applied on top (`tools/pilot-golden/`), and checks that
 * the Kotlin port gives exactly the same results, including whole learner
 * histories over many days.
 */
class PilotGoldenTest {

    private val content = load("content.json")

    /** The pilot's CONFIG, mapped onto [GameConfig]. */
    private val config = content.obj("config").let { c ->
        GameConfig(
            dailyGoal = c.int("goal"),
            attemptXp = c.int("xpAttempt"),
            starXp = c.int("xpStar"),
            fixStepXp = c.int("xpFixStep"),
            twoStarRatio = c.getValue("twoStarRatio").jsonPrimitive.double,
            leitnerIntervalsDays = c.getValue("leitnerDays").jsonArray.map { it.jsonPrimitive.int },
        )
    }

    /** The pilot looks words up in one global table, so every sentence gets all of it. */
    private val allWords: Map<String, WordInfo> = content.obj("words").mapValues { (_, v) ->
        val w = v.jsonObject
        WordInfo(
            syllables = w.strings("syl"),
            hintHi = w.str("hi"),
            hintTag = w.str("tag"),
            accept = if ("accept" in w) w.strings("accept") else emptyList(),
        )
    }

    private val sentences: List<Sentence> = content.obj("lesson").getValue("sentences").jsonArray.map {
        val s = it.jsonObject
        Sentence(id = s.str("id")!!, text = s.str("text")!!, hi = s.str("hi")!!, chunks = s.strings("chunks"), words = allWords)
    }

    @Test
    fun normalization() {
        for ((i, c) in cases("normalize.json").withIndex()) {
            val input = c.str("input")!!
            assertEquals("normalize #$i <$input>", c.strings("agreed"), Normalizer.normalize(input))
            assertEquals("normalizeHeard #$i <$input>", c.strings("heard"), Normalizer.normalizeHeard(input))
        }
    }

    @Test
    fun alignment() {
        for ((i, c) in cases("align.json").withIndex()) {
            val accept = c.obj("accept").mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.content }.toSet() }
            val alignment = Aligner.align(c.strings("target"), c.strings("heard"), accept)
            val label = "#$i ${c.strings("target")} vs ${c.strings("heard")} accept=$accept"
            assertEquals(label, c.strings("statuses"), alignment.words.map { it.status.name })
            assertEquals(label, c.strings("extra"), alignment.extra)
            assertEquals(label, Score(c.int("correct"), c.int("total"), c.int("stars")), Scorer.score(alignment, config))
        }
    }

    @Test
    fun sentenceAttempts() {
        val today = LocalDate.of(2026, 10, 19)
        for ((i, c) in cases("attempts.json").withIndex()) {
            val sentence = sentences.single { it.id == c.str("sentenceId") }
            val heard = c.str("heard")!!
            val label = "#$i ${sentence.id} <$heard>"
            assertEquals(label, c.strings("targetWords"), sentence.targetWords)
            val r = Practice.attemptSentence(LearnerProgress(), sentence, heard, today, config)
            if (c.bool("nothingHeard")) {
                assertEquals(label, SentenceAttempt.NothingHeard, r)
                continue
            }
            r as SentenceAttempt.Scored
            assertEquals(label, c.strings("statuses"), r.alignment.words.map { it.status.name })
            assertEquals(label, c.strings("extra"), r.alignment.extra)
            assertEquals(label, Score(c.int("correct"), c.int("total"), c.int("stars")), r.score)
            assertEquals(label, c.int("xp"), r.xp)
            val fix = FeedbackSelector.wordToFix(r.alignment)
            assertEquals(label, c.intOrNull("wordToFixIndex"), fix?.index)
            assertEquals(label, c.str("wordToFix"), fix?.word)
        }
    }

    @Test
    fun edgeCaseProbes() {
        val today = LocalDate.of(2026, 10, 19)
        for ((i, c) in cases("probes.json").withIndex()) {
            val text = c.str("text")!!
            val heard = c.str("heard")!!
            val label = "#$i <$text> <$heard>"
            val chunks = if ("chunks" in c) c.strings("chunks") else listOf(text)
            // A probe's own accept lists replace those words' entries, as the generator does.
            val accept = (c["accept"] as? JsonObject).orEmpty()
                .mapValues { (w, v) -> WordInfo(listOf(w), accept = v.jsonArray.map { it.jsonPrimitive.content }) }
            val sentence = Sentence(id = "probe", text = text, hi = "", chunks = chunks, words = allWords + accept)
            assertEquals(label, c.strings("targetWords"), sentence.targetWords)
            val r = Practice.attemptSentence(LearnerProgress(), sentence, heard, today, config)
            if (c.bool("nothingHeard")) {
                assertEquals(label, SentenceAttempt.NothingHeard, r)
                continue
            }
            r as SentenceAttempt.Scored
            assertEquals(label, c.strings("statuses"), r.alignment.words.map { it.status.name })
            assertEquals(label, c.strings("extra"), r.alignment.extra)
            assertEquals(label, c.int("stars"), r.score.stars)
            val fix = FeedbackSelector.wordToFix(r.alignment)
            assertEquals(label, c.str("wordToFix"), fix?.word)
            if ("fixChunk" in c) {
                val state = FixingLoop.start(sentence, fix!!.index)
                assertEquals(label, c.intOrNull("fixWordIndex"), state.wordIndex)
                assertEquals(label, c.str("fixChunk"), state.chunk)
            }
        }
    }

    @Test
    fun sentencePicking() {
        val today = LocalDate.of(2026, 10, 19)
        for ((i, c) in cases("picker.json").withIndex()) {
            val done = c.strings("doneToday")
            val current = c.int("current")
            val box = c.strings("dueWords").associateWith { LeitnerEntry(1, today) } +
                c.strings("notDueWords").associateWith { LeitnerEntry(1, today.plusDays(1)) }
            val progress = LearnerProgress(doneByDay = mapOf(today to done), leitner = box)
            val due = Leitner.due(box, today, config).toSet()
            assertEquals("#$i next", c.int("next"), SentencePicker.next(sentences, current, done, due))
            assertEquals("#$i start", c.int("start"), Practice.startPractice(progress, sentences, current, today, config))
        }
    }

    /**
     * Whole learner histories: the app opened on many days (sometimes with the
     * phone's date going back), attempts, nothing heard, fixing loops, skips
     * and the lesson-done screen. After every event the learner's XP, done
     * list, best stars, streak and Leitner box (in order) must match.
     */
    @Test
    fun learnerTraces() {
        var events = 0
        for ((t, trace) in cases("traces.json").withIndex()) {
            var progress = LearnerProgress()
            var current = 0
            var lastScored: SentenceAttempt.Scored? = null
            var fix: FixState? = null

            for ((e, ev) in trace.getValue("events").jsonArray.map { it.jsonObject }.withIndex()) {
                events++
                val label = "trace $t event $e (${ev.str("type")})"
                val day = ev.str("day")?.let(LocalDate::parse)

                fun afterSentence(today: LocalDate) {
                    val after = Practice.afterSentence(progress, sentences, current, today, config)
                    progress = after.progress
                    val next = after.next
                    if (next is AfterSentence.Next) current = next.index
                    assertEquals(label, ev.str("screen"), if (next == AfterSentence.GoalComplete) "done" else "practice")
                    assertEquals(label, ev.int("index"), current)
                }

                when (ev.str("type")) {
                    "openApp" -> {
                        progress = Practice.openApp(progress, day!!, config)
                        assertEquals(label, ev.int("streak"), progress.streak.days)
                        assertEquals(label, ev.str("pausedSince"), progress.streak.pausedSince?.toString())
                        assertView(label, ev.obj("view"), Streaks.view(progress.streak, day, config))
                    }
                    "attempt" -> {
                        current = ev.int("sentenceIndex")
                        val r = Practice.attemptSentence(progress, sentences[current], ev.str("heard")!!, day!!, config)
                        fix = null
                        if (ev.bool("nothingHeard")) {
                            assertEquals(label, SentenceAttempt.NothingHeard, r)
                        } else {
                            r as SentenceAttempt.Scored
                            progress = r.progress
                            lastScored = r
                            assertEquals(label, ev.int("stars"), r.score.stars)
                            assertEquals(label, ev.intOrNull("wordToFixIndex"), FeedbackSelector.wordToFix(r.alignment)?.index)
                        }
                    }
                    "next", "skip" -> afterSentence(day!!)
                    "startFix" -> {
                        val toFix = requireNotNull(FeedbackSelector.wordToFix(lastScored!!.alignment)) { label }
                        fix = FixingLoop.start(sentences[current], toFix.index)
                        assertEquals(label, ev.str("word"), fix.word)
                        assertEquals(label, ev.int("wordIndex"), fix.wordIndex)
                        assertEquals(label, ev.str("chunk"), fix.chunk)
                    }
                    "fixNext" -> fix = FixingLoop.next(fix!!)
                    "fixSay" -> {
                        assertEquals(label, ev.str("target"), FixingLoop.target(fix!!))
                        val r = Practice.attemptFixStep(progress, fix, sentences[current], ev.str("heard")!!, day!!, config)
                        progress = r.progress
                        fix = r.result.state
                        val outcome = r.result.outcome
                        assertEquals(label, ev.bool("nothingHeard"), outcome == FixOutcome.NOTHING_HEARD)
                        assertEquals(label, ev.bool("passed"), outcome == FixOutcome.PASSED || outcome == FixOutcome.SENTENCE_FIXED)
                        if (outcome == FixOutcome.SENTENCE_FIXED) {
                            afterSentence(day)
                        } else {
                            assertEquals(label, ev.str("screen"), "fix")
                            assertEquals(label, ev.int("index"), current)
                        }
                    }
                    else -> error("unknown event $label")
                }
                if ("step" in ev) {
                    assertEquals("$label step", ev.int("step"), fix!!.step)
                    assertEquals("$label fails", ev.int("fails"), fix.fails)
                }
                ev["after"]?.let { assertSnapshot(label, it.jsonObject, progress, day!!) }
            }

            val final = trace.obj("final")
            assertEquals("trace $t xpByDay", final.obj("xpByDay").mapValues { it.value.jsonPrimitive.int },
                progress.xpByDay.mapKeys { it.key.toString() })
            assertEquals("trace $t doneByDay", final.obj("doneByDay").mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.content } },
                progress.doneByDay.mapKeys { it.key.toString() })
        }
        assertTrue("too few trace events: $events", events > 1000)
    }

    private fun assertSnapshot(label: String, snap: JsonObject, progress: LearnerProgress, today: LocalDate) {
        assertEquals("$label xpTotal", snap.int("xpTotal"), progress.xpTotal)
        assertEquals("$label xpToday", snap.int("xpToday"), progress.xpOn(today))
        assertEquals("$label doneToday", snap.strings("doneToday"), progress.doneOn(today))
        assertEquals("$label streak", snap.int("streak"), progress.streak.days)
        assertEquals("$label lastDay", snap.str("lastDay"), progress.streak.lastPracticeDay?.toString())
        assertEquals("$label pausedSince", snap.str("pausedSince"), progress.streak.pausedSince?.toString())
        assertEquals("$label celebratedOn", snap.str("celebratedOn"), progress.goalCelebratedOn?.toString())
        assertEquals("$label best", snap.obj("best").mapValues { it.value.jsonPrimitive.int }, progress.bestStars)
        assertEquals(
            "$label revise",
            snap.getValue("revise").jsonArray.map { r -> r.jsonArray.map { it.jsonPrimitive.content } },
            progress.leitner.map { (word, entry) -> listOf(word, entry.box.toString(), entry.due.toString()) },
        )
        assertEquals("$label tomorrow", snap.strings("tomorrow"), Practice.wordsForTomorrow(progress, today, config))
        assertView(label, snap.obj("view"), Streaks.view(progress.streak, today, config))
    }

    private fun assertView(label: String, expected: JsonObject, actual: StreakView) {
        assertEquals("$label view", expected.int("days") to expected.str("status")!!.uppercase(), actual.days to actual.status.name)
    }

    // ---------- JSON helpers ----------

    private fun load(name: String): JsonObject {
        val url = requireNotNull(javaClass.getResource("/pilot-golden/$name")) { "missing golden file $name" }
        return Json.parseToJsonElement(url.readText()).jsonObject
    }

    private fun cases(name: String): List<JsonObject> = load(name).getValue("cases").jsonArray.map { it.jsonObject }

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject
    private fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int
    private fun JsonObject.intOrNull(key: String): Int? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int
    private fun JsonObject.bool(key: String): Boolean = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.boolean ?: false
    private fun JsonObject.str(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray ?: JsonArray(emptyList())).map { it.jsonPrimitive.content }
}
