package net.bodhya.speak.desktop.voice

/** A message in English, with its Hindi below (docs/decisions.md, "Labels"). */
data class Message(val en: String, val hi: String)

/**
 * What to tell the facilitator when a voice is missing, with the steps to
 * download one for this Mac's macOS version (Apple's own wording of the
 * menus). The Hindi lines are drafted by us and need the teacher review.
 */
object VoiceHelp {

    /** The System Settings path to the voice list, as Apple names it on [macOsVersion] (e.g. "26.5.1"). */
    fun settingsPath(macOsVersion: String): String = when (majorVersion(macOsVersion)) {
        in 26..Int.MAX_VALUE -> "System Settings › Accessibility › Read & Speak › System voice › ⓘ"
        15 -> "System Settings › Accessibility › Spoken Content › System voice › ⓘ"
        else -> "System Settings › Accessibility › Spoken Content › System voice › Manage Voices"
    }

    /** No Indian English voice: sentences use [fallback] (null: none at all) until one is added. */
    fun englishMissing(macOsVersion: String, fallback: Voice?): Message {
        val path = settingsPath(macOsVersion)
        val instead = fallback?.let { " Sentences use ${it.name} instead." } ?: " Sentences can't be read aloud."
        return Message(
            en = "No Indian English voice is installed on this Mac.$instead To add one: $path, then English (India), Rishi.",
            hi = "इस Mac में भारतीय अंग्रेज़ी की आवाज़ नहीं है। जोड़ने के लिए: $path, फिर English (India) में Rishi चुनिए।",
        )
    }

    /** No Hindi voice: instructions can't be read aloud until one is added. */
    fun hindiMissing(macOsVersion: String): Message {
        val path = settingsPath(macOsVersion)
        return Message(
            en = "No Hindi voice is installed on this Mac, so instructions can't be read aloud. To add one: $path, then Hindi, Lekha.",
            hi = "इस Mac में हिंदी आवाज़ नहीं है, इसलिए निर्देश बोलकर नहीं सुनाए जा सकते। जोड़ने के लिए: $path, फिर Hindi में Lekha चुनिए।",
        )
    }

    /**
     * The macOS major version. Java built with an older SDK may see macOS 26
     * as "16.x" (Apple's compatibility number), so 16 counts as 26.
     */
    fun majorVersion(macOsVersion: String): Int {
        val major = macOsVersion.substringBefore('.').toIntOrNull() ?: return 0
        return if (major == 16) 26 else major
    }
}
