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
class AppLock private constructor(private val channel: FileChannel, private val lock: FileLock) : AutoCloseable {

    override fun close() {
        lock.release()
        channel.close()
    }

    companion object {
        /** Locks [dataDir], or returns null when another copy of the app holds it. */
        fun acquire(dataDir: File): AppLock? {
            dataDir.mkdirs()
            val channel = FileChannel.open(File(dataDir, "app.lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            val lock = try {
                channel.tryLock()
            } catch (e: OverlappingFileLockException) {
                null // held by this same process
            }
            if (lock == null) {
                channel.close()
                return null
            }
            return AppLock(channel, lock)
        }
    }
}
