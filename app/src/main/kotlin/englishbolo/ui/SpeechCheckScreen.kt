package englishbolo.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import englishbolo.domain.WordStatus
import englishbolo.speech.EndReason

/**
 * English on top, Hindi below in smaller, paler text, as on every screen of
 * the app. The Hindi is the surrounding text colour at reduced strength: grey
 * on the page, a lighter tint on a coloured button.
 */
@Composable
fun BilingualText(en: String, hi: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Column(modifier) {
        Text(en, fontSize = 18.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(hi, fontSize = 16.sp, color = LocalContentColor.current.copy(alpha = 0.75f))
    }
}

/** Temporary screen for milestone b; milestone c replaces it with the practice screen. */
@Composable
fun SpeechCheckScreen(viewModel: SpeechCheckViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun micAllowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    var micDenied by rememberSaveable { mutableStateOf(false) }
    // The learner may allow the microphone in Settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (micAllowed()) micDenied = false }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        if (granted) viewModel.record()
    }
    val ready = state.model is ModelState.Ready
    val busy = state.listening || state.working

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BilingualText("Speech check", "आवाज़ की जाँच", bold = true)
        when (val model = state.model) {
            ModelState.Loading -> BilingualText("Loading the speech model…", "आवाज़ पहचानने वाला मॉडल लोड हो रहा है…")
            is ModelState.Ready -> Text("Model ready (loaded in ${model.loadMs} ms)", fontSize = 16.sp)
            is ModelState.Failed -> Text("Model failed to load: ${model.message}", fontSize = 16.sp, color = Color(0xFFB00020))
        }

        BilingualText("Choose a sentence", "एक वाक्य चुनिए")
        CHECK_SENTENCES.forEachIndexed { i, s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = !busy, role = Role.RadioButton) { viewModel.selectSentence(i) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.sentence == i, onClick = null)
                BilingualText(s.en, s.hi, Modifier.padding(start = 8.dp))
            }
        }

        Button(
            onClick = {
                when {
                    viewModel.stopIfListening() -> Unit
                    micAllowed() -> viewModel.record()
                    else -> askMic.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = ready && !state.working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        ) {
            if (state.listening) BilingualText("Stop", "रोकिए") else BilingualText("Speak", "बोलिए")
        }
        if (state.listening) {
            BilingualText("Listening…", "सुन रहे हैं…")
            LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth().height(12.dp))
        }
        if (state.working) BilingualText("Checking…", "जाँच रहे हैं…")
        state.error?.let {
            BilingualText(it.en, it.hi, bold = true)
            Text(it.detail, fontSize = 16.sp, color = Color(0xFFB00020))
        }
        state.result?.let { ResultCard(it) }
        if (micDenied) MicExplanation(onRetry = { askMic.launch(Manifest.permission.RECORD_AUDIO) }, onSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        })

        if (state.testClips.isNotEmpty()) {
            BilingualText("Test recordings", "जाँच के लिए रिकॉर्डिंग")
            state.testClips.forEach { clip ->
                OutlinedButton(
                    onClick = { viewModel.runTestClip(clip) },
                    enabled = ready && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(clip, fontSize = 16.sp) }
            }
        }

    }
}

@Composable
private fun MicExplanation(onRetry: () -> Unit, onSettings: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BilingualText(
                "The app needs the microphone to hear you speak English. Your voice stays on this phone.",
                "अंग्रेज़ी बोलते समय आपकी आवाज़ सुनने के लिए ऐप को माइक्रोफ़ोन चाहिए। आपकी आवाज़ इसी फ़ोन में रहती है।",
            )
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                BilingualText("Allow the microphone", "माइक्रोफ़ोन की अनुमति दीजिए")
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                BilingualText("Open settings", "सेटिंग्स खोलिए")
            }
        }
    }
}

@Composable
private fun ResultCard(result: CheckResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val score = result.score
            if (result.transcript == null || score == null) {
                BilingualText("Didn't hear you. Please speak again.", "आपकी आवाज़ सुनाई नहीं दी। फिर से बोलिए।", bold = true)
            } else {
                BilingualText(
                    "${score.correct} of ${score.total} words correct",
                    "${score.total} में से ${score.correct} शब्द सही",
                    bold = true,
                )
                Text("Heard: \"${result.transcript}\"", fontSize = 16.sp)
                result.words.forEach { w ->
                    val (mark, label) = when (w.status) {
                        WordStatus.CORRECT -> "✓" to "correct"
                        WordStatus.WRONG -> "↻" to "try again"
                        WordStatus.MISSING -> "–" to "missing"
                    }
                    OutlinedCard(border = BorderStroke(if (w.status == WordStatus.CORRECT) 1.dp else 2.dp, MaterialTheme.colorScheme.outline)) {
                        Text("$mark ${w.word} ($label)", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                result.recognized?.let { "Recognizer returned: \"$it\"" } ?: "Recognizer not run: nothing loud enough to be speech",
                fontSize = 16.sp,
            )
            val end = when (result.endReason) {
                EndReason.SILENCE_AFTER_SPEECH -> "ended after the speaking stopped"
                EndReason.MAX_LENGTH -> "ended at the 8 s limit"
                EndReason.STOPPED -> "ended when Stop was tapped"
            }
            Text("${result.source}: ${result.audioMs} ms of audio, $end", fontSize = 16.sp)
            result.resultMs?.let { Text("Result ready $it ms after the recording ended", fontSize = 16.sp) }
            result.memory?.let { Text("Memory: $it", fontSize = 16.sp) }
        }
    }
}
