package net.bodhya.speak.desktop.speech

import java.io.File
import java.io.FileNotFoundException

/**
 * Points sherpa-onnx at its native libraries for this Mac's processor, in
 * desktop/native/<os-arch>/ (unpacked and checksummed by
 * scripts/fetch_deps.sh). Without this, sherpa-onnx unpacks about 37 MB
 * into $TMPDIR on every start.
 */
object NativeLibraries {
    /** sherpa-onnx's LibraryLoader reads this before anything else. */
    const val PATH_PROPERTY = "sherpa_onnx.native.path"
    val FILES = listOf("libonnxruntime.dylib", "libsherpa-onnx-jni.dylib")

    /** sherpa-onnx's folder name for a JVM `os.arch`. */
    fun folderFor(osArch: String): String = when (osArch) {
        "x86_64", "amd64" -> "osx-x64"
        "aarch64", "arm64" -> "osx-aarch64"
        else -> throw IllegalStateException("no speech library for processor \"$osArch\"")
    }

    /**
     * Sets [PATH_PROPERTY] to the folder for this processor inside [root].
     * Call it once, before the first recognizer is created. Throws
     * [FileNotFoundException] if a library is missing.
     */
    fun use(root: File, osArch: String = System.getProperty("os.arch")): File {
        val dir = File(root, folderFor(osArch))
        for (name in FILES) {
            val file = File(dir, name)
            if (!file.isFile) throw FileNotFoundException("$file is missing; run scripts/fetch_deps.sh")
        }
        System.setProperty(PATH_PROPERTY, dir.absolutePath)
        return dir
    }
}
