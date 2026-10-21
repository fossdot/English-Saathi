package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.voice.Message

/** A screen in the pilot's laptop layout: up to 1080 wide, centred, scrolling when the window is short. */
@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Colors.bg).verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 1080.dp).fillMaxWidth().padding(horizontal = 40.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content,
        )
    }
}

/** A message box: a problem, a success, or plain information ([onPanel]: on a grey panel, as in the pilot's `.panel .msg`). */
@Composable
fun MessageBox(kind: MessageKind, message: Message, modifier: Modifier = Modifier, onPanel: Boolean = false) {
    val (bg, fg) = when (kind) {
        MessageKind.PROBLEM -> Colors.badSoft to Colors.badInk
        MessageKind.OK -> Colors.okSoft to Colors.okInk
        MessageKind.INFO -> (if (onPanel) Colors.bg else Colors.surface) to Colors.ink
    }
    Box(modifier.fillMaxWidth().background(bg, RoundedCornerShape(16.dp)).padding(14.dp).testTag("message")) {
        Bilingual(message.en, message.hi, size = 17.sp, color = fg)
    }
}

/** The pilot's round icon button (`.iconbtn`), 48 across; [plain] is the grey variant. */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, plain: Boolean = false) {
    Box(
        Modifier.size(48.dp).focusProperties { canFocus = false }
            .background(if (plain) Colors.surface else Colors.primarySoft, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, 24.dp, if (plain) Colors.ink else Colors.primaryInk) }
}

/** Hindi help: the speaker button on every student screen. */
@Composable
fun HelpButton(onClick: () -> Unit) = RoundIconButton(Icons.speaker, "Hear instructions in Hindi", onClick)

/** The pilot's big button (`.btn`), English above Hindi. */
@Composable
fun BigButton(
    en: String,
    hi: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Colors.primary,
    foreground: Color = Colors.onPrimary,
    enabled: Boolean = true,
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 68.dp).focusProperties { canFocus = false }
            .background(if (enabled) background else Colors.line, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val fg = if (enabled) foreground else Colors.muted
            BasicText(en, style = textStyle(21.sp, fg, FontWeight.Bold, lineHeight = 1.15f))
            BasicText(hi, style = textStyle(17.sp, fg.copy(alpha = 0.8f), lineHeight = 1.15f))
        }
    }
}

/** A rounded label (`.pill`), with an optional icon before the text. */
@Composable
fun Pill(text: String, background: Color, foreground: Color, icon: ImageVector? = null, below: String? = null) {
    Row(
        Modifier.background(background, RoundedCornerShape(24.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(it, 20.dp, foreground) }
        Column {
            BasicText(text, style = textStyle(17.sp, foreground, FontWeight.SemiBold, lineHeight = 1.2f))
            below?.let { BasicText(it, style = textStyle(14.sp, foreground, lineHeight = 1.2f)) }
        }
    }
}

/** Who is practising: their card and name. Pressing it goes back to the picker (the pilot's "Change student"). */
@Composable
fun WhoPill(learner: Learner, onClick: () -> Unit) {
    Row(
        Modifier.focusProperties { canFocus = false }
            .background(Colors.surface, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Change student" }
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardBadge(learner.card, 36.dp)
        BasicText(learner.name, style = textStyle(17.sp, Colors.ink, FontWeight.SemiBold))
    }
}

/** The keyboard hints under a screen (`.kbd-hints`): each key in a little box, then what it does. */
@Composable
fun KeyHints(vararg items: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        for ((key, action) in items) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.background(Colors.surface, RoundedCornerShape(6.dp)).border(1.dp, Colors.line, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp)) {
                    BasicText(key, style = textStyle(15.sp, Colors.ink, FontWeight.SemiBold))
                }
                BasicText(action, style = textStyle(15.sp, Colors.muted))
            }
        }
    }
}

/** "3 days", "1 day". */
fun daysText(n: Int): String = if (n == 1) "1 day" else "$n days"
