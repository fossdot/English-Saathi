package net.bodhya.speak.desktop.voice

import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Sentence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class SpeakerTest {

    private val voice = Voice("Rishi", "en_IN")
    private val rendered: MutableList<String> = Collections.synchronizedList(mutableListOf())

    /** Stands in for `say`: 0.1 s of silence per character, recording what it was asked. */
    private val speaker = Speaker { _, text -> rendered += text; ShortArray(text.length * SayRenderer.SAMPLE_RATE / 10) }

    private val lesson = Lesson(
        "l", "L", "एल",
        listOf(
            Sentence("s1", "Where is the bus stop?", "", listOf("Where is", "the bus stop")),
            Sentence("s2", "When will the bus come?", "", listOf("When will", "the bus come")),
        ),
    )

    @Test
    fun `a lesson's sentences, chunks and words are prepared at their speeds`() {
        val requests = LessonSpeech.requests(lesson)
        assertTrue(SpeechRequest("Where is the bus stop?", Speeds.NORMAL) in requests)
        assertTrue(SpeechRequest("Where is the bus stop?", Speeds.SLOW) in requests)
        assertTrue(SpeechRequest("the bus come", Speeds.SLOW) in requests)
        assertTrue(SpeechRequest("bus", Speeds.SLOW) in requests)
        assertEquals("sentences come first", SpeechRequest("Where is the bus stop?", Speeds.NORMAL), requests.first())
        assertEquals(requests.distinct(), requests)
    }

    @Test
    fun `preparing a lesson renders each text once, and then nothing waits for say`() {
        speaker.prepareLesson(voice, lesson)
        val texts = LessonSpeech.requests(lesson).map { it.text }.distinct()
        assertEquals(texts.sorted(), rendered.sorted())

        rendered.clear()
        for (r in LessonSpeech.requests(lesson)) speaker.prepare(voice, r.text, r.speed)
        assertEquals(emptyList<String>(), rendered)
    }

    @Test
    fun `slow speech is stretched from the same rendering`() {
        speaker.prepareLesson(voice, lesson)
        val normal = speaker.prepare(voice, "bus", 1f)
        val slow = speaker.prepare(voice, "bus", Speeds.SLOW)
        assertEquals(listOf("bus"), rendered.filter { it == "bus" })
        assertEquals(1.0 / Speeds.SLOW, slow.durationMs.toDouble() / normal.durationMs, 0.05)
    }

    @Test
    fun `opening another lesson replaces the previous one's speech`() {
        speaker.prepareLesson(voice, lesson)
        speaker.prepareLesson(voice, Lesson("m", "M", "एम", listOf(Sentence("t1", "How much is the ticket?", "", emptyList()))))
        rendered.clear()
        speaker.prepare(voice, "Where is the bus stop?", Speeds.NORMAL)
        assertEquals(listOf("Where is the bus stop?"), rendered)
    }
}
