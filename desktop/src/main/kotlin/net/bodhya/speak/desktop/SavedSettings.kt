package net.bodhya.speak.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.bodhya.speak.desktop.data.SafeFiles
import net.bodhya.speak.desktop.speech.SpeechModel
import java.io.File

/** The microphone to record from, as chosen in Settings. */
sealed interface MicChoice {
    /**
     * Nothing chosen, or the built-in microphone chosen: the Mac's own
     * microphone, so the input can't switch to an iPhone or headphones on
     * its own (owner, 2026-10-21).
     */
    data object BuiltIn : MicChoice

    /** "Mac's default": whatever input macOS is using. */
    data object MacDefault : MicChoice

    /** One input by its Java Sound name ("USB PnP Sound Device"). */
    data class Named(val name: String) : MicChoice
}

/** The facilitator's choices in Settings, kept for the next start. */
data class SavedSettings(
    /** small.en, with tiny.en as the fallback in Settings (owner, 2026-10-21). */
    val model: SpeechModel = SpeechModel.SMALL_EN,
    val microphone: MicChoice = MicChoice.BuiltIn,
)

/**
 * [SavedSettings] in the data folder, as
 * `{"model": "small.en", "microphone": "built-in"}` (or `"mac-default"`, or
 * `"named"` with `"microphone_name"`). A missing file gives the defaults;
 * so does a damaged one, which is reported to [read]'s caller and replaced
 * at the next change.
 *
 * Files from before 2026-10-21 have `"input"` instead: a name there was a
 * choice and is kept; `null` was written even when nothing had been chosen,
 * so it now means the built-in microphone.
 */
class SettingsFile(val file: File) {

    fun read(problem: (String) -> Unit = {}): SavedSettings {
        if (!file.isFile) return SavedSettings()
        return try {
            val json = Json.parseToJsonElement(file.readText()).jsonObject
            val id = json["model"]?.jsonPrimitive?.contentOrNull
            val model = SpeechModel.entries.firstOrNull { it.id == id }
            if (model == null) problem("unknown model \"$id\" in $file")
            SavedSettings(model ?: SavedSettings().model, microphone(json, problem))
        } catch (e: Exception) {
            problem("$file could not be read: ${e.message ?: e}")
            SavedSettings()
        }
    }

    private fun microphone(json: JsonObject, problem: (String) -> Unit): MicChoice {
        if ("microphone" !in json) {
            return json["input"]?.jsonPrimitive?.contentOrNull?.let { MicChoice.Named(it) } ?: MicChoice.BuiltIn
        }
        return when (val kind = json["microphone"]?.jsonPrimitive?.contentOrNull) {
            BUILT_IN -> MicChoice.BuiltIn
            MAC_DEFAULT -> MicChoice.MacDefault
            NAMED -> json["microphone_name"]?.jsonPrimitive?.contentOrNull?.let { MicChoice.Named(it) }
                ?: MicChoice.BuiltIn.also { problem("$file names no microphone; using the built-in one") }
            else -> MicChoice.BuiltIn.also { problem("unknown microphone \"$kind\" in $file; using the built-in one") }
        }
    }

    /** Changes one choice and keeps the others. Thread-safe. */
    @Synchronized
    fun update(change: (SavedSettings) -> SavedSettings) {
        val s = change(read())
        val mic = when (val m = s.microphone) {
            MicChoice.BuiltIn -> mapOf("microphone" to JsonPrimitive(BUILT_IN))
            MicChoice.MacDefault -> mapOf("microphone" to JsonPrimitive(MAC_DEFAULT))
            is MicChoice.Named -> mapOf("microphone" to JsonPrimitive(NAMED), "microphone_name" to JsonPrimitive(m.name))
        }
        SafeFiles.write(file, JsonObject(mapOf("model" to JsonPrimitive(s.model.id)) + mic).toString() + "\n")
    }

    private companion object {
        const val BUILT_IN = "built-in"
        const val MAC_DEFAULT = "mac-default"
        const val NAMED = "named"
    }
}
