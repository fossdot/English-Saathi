package net.bodhya.speak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizerTest {

    private fun words(text: String) = Normalizer.normalize(text)
    private fun heard(text: String) = Normalizer.normalizeHeard(text)

    @Test
    fun `lowercase, punctuation stripped, spaces collapsed`() {
        assertEquals(listOf("where", "is", "the", "bus", "stop"), words("  Where   is the BUS stop? "))
        assertEquals(listOf("one", "ticket", "to", "patna", "please"), words("One ticket to Patna, please."))
    }

    @Test
    fun `hyphenated and spaced compounds are equal, in lesson text and in what was heard`() {
        assertEquals(words("bus stop"), words("bus-stop"))
        assertEquals(words("Where is the bus stop?"), heard("where is the bus-stop"))
        assertEquals(listOf("twenty", "one"), words("twenty-one"))
    }

    @Test
    fun `apostrophes and quote marks are removed`() {
        val dont = listOf("dont")
        assertEquals(dont, words("don't"))
        assertEquals(dont, words("dont"))
        assertEquals(dont, words("Don${Char(0x2019)}t"))
        assertEquals(listOf("patna"), words("'Patna'"))
        assertEquals(listOf("its"), words("it's"))
    }

    @Test
    fun `stray spaces and lone punctuation make no words`() {
        assertEquals(listOf("where", "is", "it"), words("Where is it ?"))
        assertEquals(listOf("one", "ticket", "to", "patna", "please"), words("One ticket to Patna ${Char(0x2014)} please."))
        assertEquals(emptyList<String>(), words(" ? ! ... "))
        assertEquals(emptyList<String>(), words(""))
    }

    @Test
    fun `every number word is spelled right`() {
        val small = ("zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen " +
            "fifteen sixteen seventeen eighteen nineteen").split(" ")
        for (n in 0..19) assertEquals("$n", listOf(small[n]), words("$n"))
        val tens = "twenty thirty forty fifty sixty seventy eighty ninety".split(" ")
        for ((k, t) in tens.withIndex()) assertEquals("${(k + 2) * 10}", listOf(t), words("${(k + 2) * 10}"))
        assertEquals(words("Forty rupees"), heard("40 rupees"))
    }

    @Test
    fun `leading zeros don't count`() {
        assertEquals(listOf("seven"), words("007"))
        assertEquals(listOf("zero"), words("00"))
        assertEquals(listOf("one"), words("0000000001")) // the limit is on the value, not the length
    }

    @Test
    fun `numbers above ten are spelled out without and`() {
        assertEquals(listOf("twenty", "one"), words("21"))
        assertEquals(listOf("ninety", "nine"), words("99"))
        assertEquals(listOf("one", "hundred"), words("100"))
        assertEquals(listOf("one", "hundred", "one"), words("101"))
        assertEquals(listOf("one", "hundred", "ten"), words("110"))
        assertEquals(listOf("one", "hundred", "twenty"), words("120"))
        assertEquals(listOf("nine", "hundred", "ninety", "nine"), words("999"))
        assertEquals(listOf("one", "thousand"), words("1000"))
        assertEquals(listOf("one", "thousand"), words("1,000"))
        assertEquals(listOf("one", "thousand", "one"), words("1001"))
        assertEquals(listOf("one", "thousand", "one", "hundred"), words("1100"))
        assertEquals(listOf("twenty", "one", "thousand"), words("21000"))
        assertEquals(listOf("one", "hundred", "thousand"), words("100000"))
        assertEquals(listOf("one", "million"), words("1000000"))
        assertEquals(listOf("two", "million", "five", "thousand"), words("2005000"))
        assertEquals(
            "nine hundred ninety nine million nine hundred ninety nine thousand nine hundred ninety nine".split(" "),
            words("999999999"),
        )
    }

    @Test
    fun `digits match spelled-out numbers either way`() {
        assertEquals(words("Fifty rupees, please."), heard("50 rupees please"))
        assertEquals(words("1 ticket"), words("One ticket"))
    }

    @Test
    fun `very long numbers and mixed tokens stay as written`() {
        assertEquals(listOf("1000000000"), words("1000000000"))
        assertEquals(listOf("2nd"), words("2nd"))
        assertEquals(listOf("a1"), words("A1"))
    }

    @Test
    fun `accented letters and other scripts are dropped, as in the pilot`() {
        assertEquals(listOf("caf"), words("Caf${Char(0xE9)}"))
        assertEquals(emptyList<String>(), words("${Char(0x092C)}${Char(0x0938)}"))
    }

    @Test
    fun `every JavaScript whitespace character separates words`() {
        val spaces = listOf(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20, 0xA0, 0x1680, 0x2000, 0x2005, 0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF)
        for (cp in spaces) assertEquals("U+%04X".format(cp), listOf("bus", "stop"), words("bus${Char(cp)}stop"))
    }

    @Test
    fun `non-speech tags from the recognizer are ignored`() {
        assertEquals(emptyList<String>(), heard("[BLANK_AUDIO]"))
        assertEquals(words("where is the bus stop"), heard("(music) where is the bus stop [ Silence ]"))
        assertEquals(listOf("bus", "stop"), heard("[MUSIC] bus stop [MUSIC]"))
        assertEquals(listOf("bus", "stop"), heard("(music) bus stop (music)"))
        assertEquals(listOf("music"), words("(music)")) // lesson text keeps its words
    }

    @Test
    fun `a removed tag never joins the words around it`() {
        assertEquals(listOf("bus", "stop"), heard("bus[BLANK_AUDIO]stop"))
    }

    @Test
    fun `nested tags go, and an unclosed bracket doesn't swallow speech`() {
        assertEquals(emptyList<String>(), heard("(nested (tags))"))
        assertEquals(words("where is the bus stop"), heard("[ where is the bus stop [BLANK_AUDIO]"))
        assertEquals(words("where is the bus stop"), heard("(where is the bus stop (music)"))
        assertEquals(listOf("blankaudio"), heard("[BLANK_AUDIO")) // not a whole tag
    }

    @Test
    fun `nothing heard`() {
        for (text in listOf("", "   ", "...", "[BLANK_AUDIO]", "(upbeat music)")) {
            assertTrue("<$text>", heard(text).isEmpty())
        }
        assertEquals(listOf("bus"), heard("[BLANK_AUDIO] bus"))
    }

    @Test
    fun `tokens keep the written text for word chips`() {
        assertEquals(
            listOf(
                TextToken("Where", listOf("where")),
                TextToken("is", listOf("is")),
                TextToken("the", listOf("the")),
                TextToken("bus-stop?", listOf("bus", "stop")),
            ),
            Normalizer.tokens("Where is the bus-stop?"),
        )
        assertEquals(TextToken("${Char(0x2014)}", emptyList()), Normalizer.tokens("Patna ${Char(0x2014)} please")[1])
        assertEquals(listOf(TextToken("21", listOf("twenty", "one"))), Normalizer.tokens(" 21 "))
    }

    @Test
    fun `the tokens' words are the sentence's words`() {
        for (text in listOf("Where is the bus-stop?", "One ticket to Patna ${Char(0x2014)} please.", "Bus 21, please!")) {
            assertEquals(words(text), Normalizer.tokens(text).flatMap { it.words })
        }
    }
}
