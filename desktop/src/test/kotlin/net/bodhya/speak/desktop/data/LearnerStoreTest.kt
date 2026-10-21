package net.bodhya.speak.desktop.data

import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.LeitnerEntry
import net.bodhya.speak.domain.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LearnerStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val dataDir by lazy { tmp.newFolder("English Saathi") }
    private var instant = Instant.parse("2026-10-21T08:15:30Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("Asia/Kolkata")
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = instant
    }
    private val problems = mutableListOf<String>()
    private val store by lazy { LearnerStore(dataDir, clock) { problems += it } }

    private val blueFish = Card(CardColour.BLUE, CardSymbol.FISH)
    private val redSun = Card(CardColour.RED, CardSymbol.SUN)

    private fun record(time: String, heard: String = "where is the bus stop", result: AttemptResult = AttemptResult.SCORED) =
        AttemptRecord(time, "bus_stand", "bus_01", AttemptRecord.SENTENCE, "Where is the bus stop?", heard, 5, 5, 3, "tiny.en", result, 420)

    @Test
    fun `learners keep their card and name, oldest first, across a restart`() {
        val anita = store.add("  Anita   Kumari ", blueFish)
        instant = instant.plusSeconds(60)
        store.add("Ravi", redSun)
        assertEquals("Anita Kumari", anita.name)
        assertEquals("2026-10-21T13:45:30+05:30", anita.created)
        val again = LearnerStore(dataDir, clock)
        assertEquals(listOf("Anita Kumari", "Ravi"), again.learners().map { it.name })
        assertEquals(blueFish, again.learners()[0].card)
    }

    @Test
    fun `names and cards are unique`() {
        store.add("Anita", blueFish)
        assertEquals(NameProblem.TAKEN, store.checkName(" anita "))
        assertEquals(NameProblem.MISSING, store.checkName("   "))
        assertEquals(NameProblem.TOO_LONG, store.checkName("x".repeat(41)))
        assertNull(store.checkName("Ravi"))
        assertEquals(setOf(blueFish), store.takenCards())
        assertTrue(runCatching { store.add("Ravi", blueFish) }.isFailure)
        assertTrue(runCatching { store.add("ANITA", redSun) }.isFailure)
    }

    @Test
    fun `a rename keeps the learner's own name free for them`() {
        val anita = store.add("Anita", blueFish)
        assertNull(store.checkName("ANITA", exceptId = anita.id))
        assertEquals("ANITA", store.rename(anita.id, "ANITA").name)
        assertEquals("ANITA", store.learner(anita.id)!!.name)
    }

    @Test
    fun `progress round-trips with the Leitner box in the order words entered it`() {
        val id = store.add("Anita", blueFish).id
        val day = LocalDate.of(2026, 10, 21)
        val progress = LearnerProgress(
            xpTotal = 75,
            xpByDay = mapOf(day to 75),
            doneByDay = mapOf(day to listOf("bus_03", "bus_01")),
            bestStars = mapOf("bus_01" to 3, "bus_03" to 1),
            streak = StreakState(4, day, null),
            // Not alphabetical: "the" entered the box first.
            leitner = linkedMapOf("the" to LeitnerEntry(2, day.plusDays(3)), "bus" to LeitnerEntry(1, day.plusDays(1)), "ticket" to LeitnerEntry(1, day.plusDays(1))),
            goalCelebratedOn = day,
        )
        store.saveProgress(id, progress)
        val back = LearnerStore(dataDir, clock).progress(id)
        assertEquals(progress, back)
        assertEquals(listOf("the", "bus", "ticket"), back.leitner.keys.toList())
        assertEquals(LearnerProgress(), store.progress(store.add("Ravi", redSun).id))
    }

    @Test
    fun `a damaged progress file is kept aside and reported, not overwritten`() {
        val id = store.add("Anita", blueFish).id
        val file = File(dataDir, "learners/$id/progress.json")
        file.writeText("{\"xpTotal\": 7")
        assertEquals(LearnerProgress(), store.progress(id))
        assertEquals(1, problems.size)
        assertFalse(file.exists())
        assertEquals("{\"xpTotal\": 7", File(dataDir, "learners/$id").listFiles()!!.single { it.name.startsWith("progress.damaged-") }.readText())
    }

    @Test
    fun `attempts are logged per day and read back in order`() {
        val anita = store.add("Anita", blueFish)
        val ravi = store.add("Ravi", redSun)
        store.logAttempt(anita.id, record("2026-10-21T13:45:30+05:30"))
        store.logAttempt(ravi.id, record("2026-10-21T13:50:00+05:30", heard = "", result = AttemptResult.NOT_HEARD).copy(correct = null, total = null, stars = null, model = null, waitMs = null))
        store.logAttempt(anita.id, record("2026-10-22T09:00:00+05:30"))
        assertEquals(listOf("2026-10-21.jsonl", "2026-10-22.jsonl"), File(dataDir, "learners/${anita.id}/attempts").list()!!.sorted())
        assertEquals(2, store.attempts(anita.id).size)
        val all = LearnerStore(dataDir, clock).allAttempts()
        assertEquals(listOf("Anita", "Ravi", "Anita"), all.map { it.learner.name })
        assertEquals(AttemptResult.NOT_HEARD, all[1].record.result)
        assertNull(all[1].record.correct)
    }

    @Test
    fun `a row that can't be read is reported and the others are kept`() {
        val id = store.add("Anita", blueFish).id
        store.logAttempt(id, record("2026-10-21T13:45:30+05:30"))
        val file = File(dataDir, "learners/$id/attempts/2026-10-21.jsonl")
        file.appendText("{\"time\": \"2026-10-21T13:4\n")
        store.logAttempt(id, record("2026-10-21T13:46:30+05:30"))
        assertEquals(2, store.attempts(id).size)
        assertEquals(problems.toString(), 1, problems.size)
    }

    @Test
    fun `deleting a student removes their progress, attempts and recordings`() {
        val anita = store.add("Anita", blueFish)
        val ravi = store.add("Ravi", redSun)
        store.saveProgress(anita.id, LearnerProgress(xpTotal = 10))
        store.logAttempt(anita.id, record("2026-10-21T13:45:30+05:30"))
        store.logAttempt(ravi.id, record("2026-10-21T13:46:30+05:30"))
        val wav = store.saveRecording(anita.id, "bus_01", byteArrayOf(1, 2, 3))
        assertTrue(wav.isFile)
        store.delete(anita.id)
        assertEquals(listOf("Ravi"), store.learners().map { it.name })
        assertEquals(listOf("Ravi"), store.allAttempts().map { it.learner.name })
        assertFalse(wav.exists())
        assertEquals(listOf(ravi.id), File(dataDir, "learners").list()!!.toList())
        assertTrue("the card and name are free again", store.checkName("Anita") == null && blueFish !in store.takenCards())
    }

    @Test
    fun `a delete cut short is finished at the next start`() {
        val id = store.add("Anita", blueFish).id
        val half = File(dataDir, "learners/.deleting-$id")
        File(dataDir, "learners/$id").renameTo(half)
        LearnerStore(dataDir, clock)
        assertFalse(half.exists())
    }

    @Test
    fun `no file is ever half-written`() {
        val id = store.add("Anita", blueFish).id
        store.saveProgress(id, LearnerProgress(xpTotal = 10))
        store.saveProgress(id, LearnerProgress(xpTotal = 20))
        // Only finished files; the temporary file of each write is gone.
        assertEquals(listOf("learner.json", "progress.json"), File(dataDir, "learners/$id").list()!!.sorted())
    }

    @Test
    fun `only one copy of the app may use the data folder`() {
        // Another process, as a second copy of the app would be.
        val java = File(System.getProperty("java.home"), "bin/java").path
        val other = ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), LockHolder::class.java.name, dataDir.path).start()
        try {
            assertEquals("locked", other.inputStream.bufferedReader().readLine())
            assertNull(AppLock.acquire(dataDir))
        } finally {
            other.outputStream.close() // LockHolder exits when its input closes
            other.waitFor()
        }
        val lock = AppLock.acquire(dataDir)
        assertNotNull("free again once the other copy has quit", lock)
        assertNull("and this process can't take it twice", AppLock.acquire(dataDir))
        lock!!.close()
    }
}

/** Holds the data folder's lock in its own process until its input closes. */
object LockHolder {
    @JvmStatic
    fun main(args: Array<String>) {
        val lock = AppLock.acquire(File(args[0]))
        println(if (lock != null) "locked" else "not locked")
        System.out.flush()
        System.`in`.read()
        lock?.close()
    }
}
