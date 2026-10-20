package net.bodhya.speak.desktop.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileNotFoundException

class NativeLibrariesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun picksSherpaOnnxsFolderForEachMacProcessor() {
        assertEquals("osx-x64", NativeLibraries.folderFor("x86_64"))
        assertEquals("osx-aarch64", NativeLibraries.folderFor("aarch64"))
    }

    @Test
    fun rejectsOtherProcessors() {
        assertThrows(IllegalStateException::class.java) { NativeLibraries.folderFor("ppc64") }
    }

    @Test
    fun aMissingLibraryIsReportedBeforeSherpaOnnxTriesToLoadIt() {
        val dir = tmp.newFolder("osx-x64")
        dir.resolve("libonnxruntime.dylib").writeText("")
        val e = assertThrows(FileNotFoundException::class.java) { NativeLibraries.use(tmp.root, "x86_64") }
        assertEquals(true, e.message!!.contains("libsherpa-onnx-jni.dylib"))
    }
}
