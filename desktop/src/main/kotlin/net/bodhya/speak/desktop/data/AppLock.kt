package net.bodhya.speak.desktop.data

import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption

/**
 * Only one copy of the app may use the data folder: two copies would
 * overwrite each other's progress (docs/desktop-plan.md, D5). macOS releases
 * the lock when the process ends, however it ends, so a crash never leaves
 * the app locked out.
 */
class AppLock private constructor(private val channel: FileChannel, private val lock: FileLock, private val path: String) : AutoCloseable {

    override fun close() = synchronized(Companion) {
        lock.release()
        channel.close()
        held -= path
    }

    companion object {
        /**
         * Lock files this process holds. macOS file locks belong to the
         * process, and closing any channel to the file releases them, so a
         * second try from this process must not even open the file.
         */
        private val held = mutableSetOf<String>()

        /** Locks [dataDir], or returns null when another copy of the app (or this one) holds it. */
        fun acquire(dataDir: File): AppLock? = synchronized(Companion) {
            dataDir.mkdirs()
            val file = File(dataDir, "app.lock").canonicalFile
            if (file.path in held) return null
            val channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            val lock = try {
                channel.tryLock()
            } catch (e: OverlappingFileLockException) {
                // Locked elsewhere in this process; closing the channel would release that lock, so it is left open.
                return null
            }
            if (lock == null) {
                channel.close()
                return null
            }
            held += file.path
            return AppLock(channel, lock, file.path)
        }
    }
}
