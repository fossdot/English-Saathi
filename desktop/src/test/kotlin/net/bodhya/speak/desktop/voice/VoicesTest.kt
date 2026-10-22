package net.bodhya.speak.desktop.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicesTest {

    /** Lines as `say -v '?'` prints them on macOS 26 (the Siri voices appear twice). */
    private val list = """
        Aman (English (India)) en_IN    # Hello! My name is Aman.
        Aman (English (India)) en_IN    # Hi, I’m Siri!
        Daniel (Enhanced)   en_GB    # Hello! My name is Daniel.
        Lekha               hi_IN    # नमस्ते, मेरा नाम लेखा है।
        Rishi               en_IN    # Hello! My name is Rishi.
        Samantha            en_US    # Hello! My name is Samantha.
        Tara (English (India)) en_IN    # Hello! My name is Tara.
        not a voice line
    """.trimIndent()

    private val voices = Voices.parse(list)

    @Test
    fun `parses names with spaces and brackets, once each`() {
        assertEquals(
            listOf("Aman (English (India))", "Daniel (Enhanced)", "Lekha", "Rishi", "Samantha", "Tara (English (India))"),
            voices.all.map { it.name },
        )
        assertEquals("hi_IN", voices.all.single { it.name == "Lekha" }.locale)
    }

    @Test
    fun `knows the quality and base name from the name`() {
        assertEquals(VoiceQuality.ENHANCED, Voice("Daniel (Enhanced)", "en_GB").quality)
        assertEquals(VoiceQuality.PREMIUM, Voice("Isha (Premium)", "en_IN").quality)
        assertEquals(VoiceQuality.STANDARD, Voice("Aman (English (India))", "en_IN").quality)
        assertEquals("Aman", Voice("Aman (English (India))", "en_IN").baseName)
    }

    @Test
    fun `picks the preferred voice for each role`() {
        assertEquals("Rishi", voices.choose(VoiceRole.ENGLISH)?.name)
        assertEquals("Lekha", voices.choose(VoiceRole.HINDI)?.name)
    }

    @Test
    fun `a better quality voice wins over the preferred name`() {
        val withIsha = Voices(voices.all + Voice("Isha (Enhanced)", "en_IN") + Voice("Rishi (Enhanced)", "en_IN"))
        assertEquals("Rishi (Enhanced)", withIsha.choose(VoiceRole.ENGLISH)?.name)
        assertEquals("Isha (Premium)", Voices(withIsha.all + Voice("Isha (Premium)", "en_IN")).choose(VoiceRole.ENGLISH)?.name)
    }

    @Test
    fun `unknown names of the right language still count`() {
        assertEquals("Zoya", Voices(listOf(Voice("Zoya", "hi_IN"))).choose(VoiceRole.HINDI)?.name)
    }

    @Test
    fun `missing voices are null, and English falls back to another English voice`() {
        val noIndia = Voices(voices.all.filter { it.locale != "en_IN" && it.locale != "hi_IN" })
        assertNull(noIndia.choose(VoiceRole.ENGLISH))
        assertNull(noIndia.choose(VoiceRole.HINDI))
        assertEquals("Daniel (Enhanced)", noIndia.englishFallback()?.name)
        assertNull(Voices(emptyList()).englishFallback())
    }

    @Test
    fun `missing-voice messages give the steps for the Mac's macOS version`() {
        assertTrue(VoiceHelp.hindiMissing("26.5.1").en.contains("Accessibility › Read & Speak › System voice › ⓘ"))
        assertTrue(VoiceHelp.hindiMissing("16.0").hi.contains("Read & Speak"))
        assertTrue(VoiceHelp.hindiMissing("15.6").en.contains("Spoken Content › System voice › ⓘ"))
        assertTrue(VoiceHelp.hindiMissing("14.7").en.contains("Spoken Content › System voice › Manage Voices"))
        assertTrue(VoiceHelp.hindiMissing("12.7.6").en.contains("System Preferences › Accessibility › Spoken Content › System Voice › Customize"))
        val english = VoiceHelp.englishMissing("26.0", Voice("Daniel (Enhanced)", "en_GB"))
        assertTrue(english.en.contains("Sentences use Daniel (Enhanced) instead."))
        assertTrue(english.hi.contains("English (India) में Rishi"))
    }
}
