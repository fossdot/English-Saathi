package net.bodhya.speak.desktop

import net.bodhya.speak.desktop.mic.JavaMicCheck
import net.bodhya.speak.desktop.mic.MicAccess
import net.bodhya.speak.desktop.mic.RestoredInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MacHelpersTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `reads the hardened runtime flag from codesign`() {
        assertEquals(true, MicAccess.parseHardened("CodeDirectory v=20500 size=769 flags=0x10000(runtime) hashes=13+7 location=embedded"))
        assertEquals(false, MicAccess.parseHardened("CodeDirectory v=20400 size=462 flags=0x2(adhoc) hashes=4+7 location=embedded"))
        assertEquals(true, MicAccess.parseHardened("flags=0x10002(adhoc,runtime)"))
        assertEquals(false, MicAccess.parseHardened("code object is not signed at all"))
    }

    @Test
    fun `a hardened Java needs the audio entitlement to record`() {
        assertEquals(false, JavaMicCheck("java", "Eclipse Adoptium", hardened = true, hasAudioEntitlement = false).canRecord)
        assertEquals(true, JavaMicCheck("java", "Amazon.com Inc.", hardened = true, hasAudioEntitlement = true).canRecord)
        assertEquals(true, JavaMicCheck("java", "Homebrew", hardened = false, hasAudioEntitlement = true).canRecord)
    }

    @Test
    fun `the saved microphone is used when connected, otherwise the built-in one`() {
        val inputs = listOf("Default Audio Device", "MacBook Pro Microphone", "USB PnP Sound Device")
        val builtIn = "MacBook Pro Microphone"
        assertEquals(RestoredInput("MacBook Pro Microphone"), MicAccess.restoreInput(MicChoice.BuiltIn, inputs, builtIn))
        assertEquals(RestoredInput(null), MicAccess.restoreInput(MicChoice.MacDefault, inputs, builtIn))
        assertEquals(RestoredInput("USB PnP Sound Device"), MicAccess.restoreInput(MicChoice.Named("USB PnP Sound Device"), inputs, builtIn))
        val airPods = MicChoice.Named("AirPods")
        assertEquals(RestoredInput("MacBook Pro Microphone", airPods), MicAccess.restoreInput(airPods, inputs, builtIn))
        // No built-in microphone (a Mac mini), or the lid is closed: the Mac's default, and the app says so.
        assertEquals(RestoredInput(null, airPods), MicAccess.restoreInput(airPods, inputs, null))
        val lidClosed = listOf("Default Audio Device", "USB PnP Sound Device")
        assertEquals(RestoredInput(null, airPods), MicAccess.restoreInput(airPods, lidClosed, builtIn))
        assertEquals(RestoredInput(null, MicChoice.BuiltIn), MicAccess.restoreInput(MicChoice.BuiltIn, lidClosed, builtIn))
    }

    @Test
    fun `finds the built-in microphone in system_profiler's list`() {
        // An M4 MacBook Pro's list, shortened: an iPhone microphone first, then the built-in microphone and speakers.
        val json = """
            {"SPAudioDataType": [{"_name": "coreaudio_device", "_items": [
              {"_name": "iPhone Microphone", "coreaudio_device_input": 1, "coreaudio_device_transport": "coreaudio_device_type_unknown"},
              {"_name": "MacBook Pro Speakers", "coreaudio_device_output": 2, "coreaudio_device_transport": "coreaudio_device_type_builtin"},
              {"_name": "MacBook Pro Microphone", "coreaudio_device_input": 1, "coreaudio_device_transport": "coreaudio_device_type_builtin"}
            ]}]}
        """
        assertEquals("MacBook Pro Microphone", MicAccess.parseBuiltInInput(json))
        assertEquals(null, MicAccess.parseBuiltInInput("""{"SPAudioDataType": [{"_items": []}]}"""))
        assertEquals(null, MicAccess.parseBuiltInInput("not json"))
    }

    @Test
    fun `the data folder is kept out of Time Machine`() {
        assumeTrue("needs macOS's tmutil", File("/usr/bin/tmutil").canExecute())
        val dir = File(tmp.root, "English Saathi")
        assertTrue(DataFolder.prepare(dir))
        assertTrue(DataFolder.isExcluded(dir))
    }

    @Test
    fun `the log appends timestamped lines`() {
        val log = SessionLog(File(tmp.root, "logs/check.log"))
        log.write("one")
        log.write("two")
        val lines = log.file.readLines()
        assertEquals(2, lines.size)
        assertTrue(lines[1], Regex("""\d{4}-\d\d-\d\d \d\d:\d\d:\d\d \| two""").matches(lines[1]))
    }
}
