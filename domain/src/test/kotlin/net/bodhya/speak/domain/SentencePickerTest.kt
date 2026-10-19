package net.bodhya.speak.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SentencePickerTest {

    private val sentences = BusStand.lesson.sentences // bus_01 … bus_05
    private val none = emptySet<String>()

    @Test
    fun `next goes on in lesson order, skipping sentences done today`() {
        assertEquals(1, SentencePicker.next(sentences, 0, emptyList(), none))
        assertEquals(3, SentencePicker.next(sentences, 1, listOf("bus_03"), none))
        assertEquals(0, SentencePicker.next(sentences, 4, listOf("bus_05"), none)) // wraps round
        assertEquals(1, SentencePicker.next(sentences, 3, listOf("bus_03", "bus_04", "bus_05", "bus_01"), none))
    }

    @Test
    fun `next with everything done today is simply the following sentence`() {
        val all = sentences.map { it.id }
        assertEquals(3, SentencePicker.next(sentences, 2, all, none))
        assertEquals(0, SentencePicker.next(sentences, 4, all, none))
    }

    @Test
    fun `sentences with a word due for revision come first`() {
        // "bettiah" is only in bus_05; "ticket" is in bus_03 and bus_04.
        assertEquals(4, SentencePicker.next(sentences, 0, emptyList(), setOf("bettiah")))
        assertEquals(2, SentencePicker.next(sentences, 3, emptyList(), setOf("ticket")))
        assertEquals(3, SentencePicker.next(sentences, 0, listOf("bus_03"), setOf("ticket")))
    }

    @Test
    fun `among sentences with due words, lesson order wins over the current position`() {
        // "bus" is in bus_01, bus_02 and bus_05; we are on bus_03.
        assertEquals(0, SentencePicker.next(sentences, 2, listOf("bus_03"), setOf("bus")))
    }

    @Test
    fun `due words without teaching data count too`() {
        // "go" has no word entry and is only in bus_05.
        assertEquals(4, SentencePicker.next(sentences, 0, emptyList(), setOf("go")))
    }

    @Test
    fun `due words in sentences already done today, or in no sentence, are ignored`() {
        assertEquals(1, SentencePicker.next(sentences, 0, listOf("bus_05"), setOf("bettiah")))
        assertEquals(1, SentencePicker.next(sentences, 0, emptyList(), setOf("zebra")))
    }

    @Test
    fun `start opens the first sentence not done today`() {
        assertEquals(0, SentencePicker.start(sentences, 3, emptyList(), none))
        // bus_02 was opened but not said: Start again opens it (the pilot skipped to bus_03).
        assertEquals(1, SentencePicker.start(sentences, 1, listOf("bus_01"), none))
        assertEquals(4, SentencePicker.start(sentences, 0, listOf("bus_01", "bus_02", "bus_03", "bus_04"), none))
    }

    @Test
    fun `start prefers a sentence with a due word`() {
        assertEquals(2, SentencePicker.start(sentences, 0, emptyList(), setOf("patna")))
    }

    @Test
    fun `start with everything done today opens the sentence after the current one`() {
        assertEquals(3, SentencePicker.start(sentences, 2, sentences.map { it.id }, setOf("bus")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the current index must be a sentence of the lesson`() {
        SentencePicker.next(sentences, 5, emptyList(), none)
    }
}
