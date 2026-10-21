package net.bodhya.speak.desktop.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Writes that never leave a half-written file (docs/desktop-plan.md, D5):
 * the data goes to a temporary file next to [file], is flushed to the disk,
 * and then replaces [file] in one step. A power cut or crash leaves either
 * the old file or the new one.
 */
object SafeFiles {

    fun write(file: File, bytes: ByteArray) {
        file.parentFile.mkdirs()
        val tmp = File(file.parentFile, ".${file.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun write(file: File, text: String) = write(file, text.toByteArray(Charsets.UTF_8))
}
