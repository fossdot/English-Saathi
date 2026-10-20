package net.bodhya.speak.desktop

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * The app's one data folder, ~/Library/Application Support/English Saathi
 * (docs/desktop-plan.md). It is kept out of Time Machine backups, as the
 * Android app's data is kept out of backups (docs/decisions.md, Q4).
 */
object DataFolder {
    val default: File = File(System.getProperty("user.home"), "Library/Application Support/English Saathi")

    /**
     * Creates [dir] if needed and excludes it from Time Machine. Returns
     * whether the exclusion is in place (`tmutil` marks the folder itself, so
     * it stays excluded if the folder is moved; no administrator needed).
     */
    fun prepare(dir: File = default): Boolean {
        dir.mkdirs()
        if (isExcluded(dir)) return true
        run("tmutil", "addexclusion", dir.path)
        return isExcluded(dir)
    }

    fun isExcluded(dir: File): Boolean = run("tmutil", "isexcluded", dir.path)?.contains("[Excluded]") == true

    private fun run(vararg command: String): String? = runCatching {
        val p = ProcessBuilder(*command).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        if (p.waitFor(10, TimeUnit.SECONDS)) out else null.also { p.destroyForcibly() }
    }.getOrNull()
}

/** Appends timestamped lines to a log file in the data folder. Thread-safe. */
class SessionLog(val file: File) {
    init {
        file.parentFile.mkdirs()
    }

    @Synchronized
    fun write(line: String) {
        file.appendText("${LocalDateTime.now().format(TIME)} | $line\n")
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
