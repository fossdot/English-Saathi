package net.bodhya.speak.desktop.mic

import java.io.File
import java.util.concurrent.TimeUnit

/** macOS's answer to "may this app use the microphone?", for the app that started Gradle (Terminal). */
enum class MicPermission {
    /** Not asked yet: the first recording shows the macOS prompt. */
    NOT_ASKED,

    /** Blocked by a device policy. */
    RESTRICTED,

    /** Turned off in System Settings › Privacy & Security › Microphone. */
    DENIED,
    ALLOWED,

    /** Couldn't be read. */
    UNKNOWN,
}

/** Whether the Java running the app may use the microphone at all (docs/desktop-plan.md, "Why Corretto"). */
data class JavaMicCheck(val javaPath: String, val vendor: String, val hardened: Boolean, val hasAudioEntitlement: Boolean) {
    /** A hardened-runtime Java without the audio-input entitlement only ever records silence (Temurin 17). */
    val canRecord: Boolean get() = !hardened || hasAudioEntitlement
}

/**
 * What the app can find out about the microphone on macOS without native
 * code. None of it replaces the zero-sample rule: these are hints for the
 * messages and the log.
 */
object MicAccess {

    /** The recording inputs Java Sound sees (the first is usually "Default Audio Device"). */
    fun inputs(): List<String> = recordingMixers().map { it.name }

    /**
     * Asks macOS for the microphone permission with a one-line script, which
     * never shows the prompt. Run from Terminal, the answer is Terminal's.
     */
    fun permission(): MicPermission {
        val out = run(
            "osascript", "-l", "JavaScript", "-e",
            "ObjC.import('AVFoundation'); $.NSClassFromString('AVCaptureDevice').authorizationStatusForMediaType($('soun'))",
        )
        return when (out?.trim()) {
            "0" -> MicPermission.NOT_ASKED
            "1" -> MicPermission.RESTRICTED
            "2" -> MicPermission.DENIED
            "3" -> MicPermission.ALLOWED
            else -> MicPermission.UNKNOWN
        }
    }

    /** Reads the signature of this JVM's `java` with `codesign`, which every Mac has. */
    fun checkJava(javaHome: String = System.getProperty("java.home")): JavaMicCheck {
        val java = File(javaHome, "bin/java").path
        val details = run("codesign", "-dv", java).orEmpty()
        val entitlements = run("codesign", "-d", "--entitlements", "-", "--xml", java).orEmpty()
        return JavaMicCheck(
            javaPath = java,
            vendor = System.getProperty("java.vendor"),
            hardened = parseHardened(details),
            hasAudioEntitlement = "com.apple.security.device.audio-input" in entitlements,
        )
    }

    /** `codesign -dv` prints e.g. "CodeDirectory v=20500 size=769 flags=0x10000(runtime) …". */
    fun parseHardened(codesignDetails: String): Boolean =
        Regex("""flags=0x[0-9a-f]+\(([^)]*)\)""").find(codesignDetails)?.groupValues?.get(1)?.split(',')?.contains("runtime") == true

    private fun run(vararg command: String): String? = runCatching {
        val p = ProcessBuilder(*command).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            null
        } else {
            out
        }
    }.getOrNull()
}
