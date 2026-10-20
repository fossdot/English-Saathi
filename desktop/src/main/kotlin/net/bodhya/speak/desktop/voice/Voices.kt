package net.bodhya.speak.desktop.voice

/** How good a macOS voice sounds: the download sizes Apple offers. */
enum class VoiceQuality { STANDARD, ENHANCED, PREMIUM }

/**
 * One line of `say -v '?'`: the name to pass to `say -v` (it can contain
 * spaces and parentheses, e.g. "Rishi (Enhanced)" or "Aman (English
 * (India))"), and its locale, e.g. en_IN.
 */
data class Voice(val name: String, val locale: String) {
    val quality: VoiceQuality
        get() = when {
            name.endsWith(" (Premium)") -> VoiceQuality.PREMIUM
            name.endsWith(" (Enhanced)") -> VoiceQuality.ENHANCED
            else -> VoiceQuality.STANDARD
        }

    /** The name without any bracketed suffix: "Rishi" for "Rishi (Enhanced)". */
    val baseName: String get() = name.substringBefore(" (")
}

/** What a voice is for: English sentences and words, or Hindi instructions. */
enum class VoiceRole(val locale: String, val preferred: List<String>) {
    /** English (India). Rishi first: it is the voice the test clips use, and every Mac has a basic version. */
    ENGLISH("en_IN", listOf("Rishi", "Isha", "Veena", "Sangeeta", "Aman", "Tara")),

    /** Hindi. Lekha first: every Mac has a basic version. */
    HINDI("hi_IN", listOf("Lekha", "Kiyara", "Neel")),
}

/**
 * The voices `say` has on this Mac. `say` never reports a missing voice: it
 * quietly speaks with another one, so the app must check the list before
 * naming a voice.
 */
class Voices(val all: List<Voice>) {

    /**
     * The voice for [role]: the best quality installed for its locale
     * (Premium, then Enhanced), then the preferred names in order. Null if
     * the Mac has no voice for that locale.
     */
    fun choose(role: VoiceRole): Voice? = all
        .filter { it.locale == role.locale }
        .sortedWith(
            compareByDescending<Voice> { it.quality }
                .thenBy { role.preferred.indexOf(it.baseName).let { i -> if (i < 0) role.preferred.size else i } }
                .thenBy { it.name },
        )
        .firstOrNull()

    /**
     * When no Indian English voice is installed, sentences are still read,
     * with another English voice (en_GB first, then en_US, then any), as the
     * pilot did. Null if the Mac has no English voice at all.
     */
    fun englishFallback(): Voice? =
        listOf("en_GB", "en_US").firstNotNullOfOrNull { locale -> all.firstOrNull { it.locale == locale } }
            ?: all.firstOrNull { it.locale.startsWith("en_") }

    companion object {
        /** `say -v '?'` lines are `printf("%-19s %-8s # %s", name, locale, sample)`. */
        private val LINE = Regex("""^(.*?\S)\s+([a-z]{2,3}_[A-Z0-9]{2,3})\s+# (.*)$""")

        /** Parses `say -v '?'` output. Lines that don't match are skipped; repeated names count once. */
        fun parse(sayList: String): Voices = Voices(
            sayList.lineSequence()
                .mapNotNull { LINE.matchEntire(it.trimEnd()) }
                .map { Voice(it.groupValues[1], it.groupValues[2]) }
                .distinct()
                .toList(),
        )
    }
}
