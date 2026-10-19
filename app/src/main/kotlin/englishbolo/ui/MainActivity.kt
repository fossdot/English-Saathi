package englishbolo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/** The app's single activity. */
class MainActivity : ComponentActivity() {
    private val speechCheck: SpeechCheckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draws behind the system bars (the screen pads itself) with readable bar icons.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    SpeechCheckScreen(speechCheck)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app (Home, a call, the screen turning off) stops listening; turning the phone doesn't.
        if (!isChangingConfigurations) speechCheck.stopIfListening()
    }
}
