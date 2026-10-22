package net.bodhya.speak.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/** The self-test the packaged app runs (docs/packaging.md), here on the repo's files. */
class SelfTestTest {
    @Test
    fun `every check passes on this Mac`() {
        val out = ByteArrayOutputStream()
        val code = SelfTest.run(AppFiles.repo(TestContent.projectDir), PrintStream(out, true, Charsets.UTF_8))
        val report = out.toString(Charsets.UTF_8)
        println(report)
        assertEquals(report, 0, code)
        for (line in listOf("the signature lets it record", "lessons: 1 pack(s), 1 lesson(s), 5 sentences", "speech model tiny.en: the LibriSpeech clip word for word",
            "speech model small.en: the LibriSpeech clip word for word", "window drawing and font", "all checks passed")) {
            assertTrue("missing \"$line\":\n$report", line in report)
        }
    }
}
