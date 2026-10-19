package net.bodhya.speak.speech

import java.io.File

/** This process's memory, from /proc/self/status, in kB. */
data class MemoryStats(
    /** Resident memory now (VmRSS). */
    val residentKb: Long,
    /** Highest resident memory since the process started (VmHWM). */
    val peakKb: Long,
) {
    override fun toString() = "resident ${residentKb / 1024} MB, peak ${peakKb / 1024} MB"

    companion object {
        /** Null where /proc is not readable. */
        fun read(statusFile: File = File("/proc/self/status")): MemoryStats? {
            val lines = runCatching { statusFile.readLines() }.getOrNull() ?: return null
            fun kb(key: String): Long? = lines.firstOrNull { it.startsWith("$key:") }
                ?.substringAfter(':')?.trim()?.removeSuffix("kB")?.trim()?.toLongOrNull()
            return MemoryStats(kb("VmRSS") ?: return null, kb("VmHWM") ?: return null)
        }
    }
}
