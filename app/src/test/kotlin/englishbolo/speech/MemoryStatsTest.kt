package englishbolo.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class MemoryStatsTest {

    private fun status(text: String) = File.createTempFile("status", null).apply {
        deleteOnExit()
        writeText(text)
    }

    @Test
    fun `reads resident and peak memory`() {
        val file = status("Name:\tapp\nVmPeak:\t 2000000 kB\nVmHWM:\t  312340 kB\nVmRSS:\t  250000 kB\nThreads:\t20\n")
        assertEquals(MemoryStats(residentKb = 250_000, peakKb = 312_340), MemoryStats.read(file))
    }

    @Test
    fun `null when the file or a value is missing`() {
        assertNull(MemoryStats.read(File("/no/such/file")))
        assertNull(MemoryStats.read(status("VmRSS:\t 1 kB\n")))
    }
}
