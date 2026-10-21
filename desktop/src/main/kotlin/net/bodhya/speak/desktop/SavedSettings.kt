package net.bodhya.speak.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.bodhya.speak.desktop.speech.SpeechModel
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The facilitator's choices in Settings, kept for the next start. */
data class SavedSettings(
    /** tiny.en until the MacBook trip measures small.en (docs/decisions.md, Q3). */
    val model: SpeechModel = SpeechModel.TINY_EN,
    /** A Java Sound input name; null is the Mac's default. */
    val input: String? = null,
)

/**
 * [SavedSettings] in the data folder, as `{"model": "small.en", "input": null}`.
 * A missing file gives the defaults; so does a damaged one, which is
 * reported to [read]'s caller and replaced at the next change.
 */
class SettingsFile(val file: File) {

    fun read(problem: (String) -> Unit = {}): SavedSettings {
        if (!file.isFile) return SavedSettings()
        return try {
            val json = Json.parseToJsonElement(file.readText()).jsonObject
            val id = json["model"]?.jsonPrimitive?.contentOrNull
            val model = SpeechModel.entries.firstOrNull { it.id == id }
            if (model == null) problem("unknown model \"$id\" in $file")
            SavedSettings(model ?: SavedSettings().model, json["input"]?.jsonPrimitive?.contentOrNull)
        } catch (e: Exception) {
            problem("$file could not be read: ${e.message ?: e}")
            SavedSettings()
        }
    }

    /** Changes one choice and keeps the others. Thread-safe. */
    @Synchronized
    fun update(change: (SavedSettings) -> SavedSettings) {
        val s = change(read())
        val json = JsonObject(mapOf("model" to JsonPrimitive(s.model.id), "input" to (s.input?.let(::JsonPrimitive) ?: JsonNull)))
        file.parentFile.mkdirs()
        // Written next to the file and moved over it, so a crash mid-write can't leave half a file.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.toString() + "\n")
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
