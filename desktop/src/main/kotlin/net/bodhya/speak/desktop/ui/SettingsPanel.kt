package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.speech.SpeechModel

/**
 * The facilitator's settings (⌘,; no button on the student screens): the
 * speech model, the microphone input, the 1-second microphone check, and
 * test clips run through the same recorder.
 */
@Composable
fun SettingsPanel(
    model: SpeechModel,
    input: String?,
    inputs: List<String>,
    clips: List<String>,
    onModel: (SpeechModel) -> Unit,
    onInput: (String?) -> Unit,
    onCheckMicrophone: () -> Unit,
    onClip: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Colors.bg).verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Bilingual("Settings", "सेटिंग", size = 26.sp, bold = true)
        Line("Speech model (every result is logged with its model)", bold = true)
        Row(horizontalArrangement = Gap) {
            for (m in SpeechModel.entries) {
                PilotButton(m.id + if (m == model) " ✓" else "", if (m == SpeechModel.TINY_EN) "तेज़" else "ज़्यादा सही", { onModel(m) }, primary = m == model)
            }
        }
        Line("Microphone", bold = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PilotButton("Mac's default" + if (input == null) " ✓" else "", "Mac का अपना माइक", { onInput(null) }, primary = input == null)
            for (name in inputs.filter { it != "Default Audio Device" }) {
                PilotButton(name + if (name == input) " ✓" else "", "यह माइक", { onInput(name) }, primary = name == input)
            }
        }
        PilotButton("Check the microphone (1 second)", "माइक जाँचिए", onCheckMicrophone)
        if (clips.isNotEmpty()) {
            Line("Test clips, through the same recorder", bold = true)
            Row(horizontalArrangement = Gap) { for (c in clips) PilotButton(c.removeSuffix(".wav"), "चलाइए", { onClip(c) }) }
        }
    }
}
