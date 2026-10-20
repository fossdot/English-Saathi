package net.bodhya.speak.desktop.check

import net.bodhya.speak.desktop.voice.SayRenderer
import net.bodhya.speak.desktop.voice.Speaker
import net.bodhya.speak.desktop.voice.VoiceHelp
import net.bodhya.speak.desktop.voice.VoiceRole
import kotlin.system.exitProcess

/** The pilot's speeds (CONFIG.normalRate, CONFIG.slowRate, and its Hindi help). */
private const val NORMAL = 0.9f
private const val SLOW = 0.6f
private const val HINDI_HELP = 0.95f

/**
 * Milestone D2's check (docs/desktop-plan.md): which voices this Mac has,
 * which the app picks, and how long each speech lasts at each speed. Plays
 * them unless the argument `--silent` is given.
 */
fun main(args: Array<String>) {
    val silent = "--silent" in args
    val macOs = System.getProperty("os.version")
    val renderer = SayRenderer()
    val voices = renderer.listVoices()
    println("macOS $macOs: ${voices.all.size} voices; English (India): ${voices.all.filter { it.locale == "en_IN" }.joinToString { it.name }}; " +
        "Hindi: ${voices.all.filter { it.locale == "hi_IN" }.joinToString { it.name }}")

    val english = voices.choose(VoiceRole.ENGLISH)
    val hindi = voices.choose(VoiceRole.HINDI)
    if (english == null) VoiceHelp.englishMissing(macOs, voices.englishFallback()).let { println("${it.en}\n${it.hi}") }
    if (hindi == null) VoiceHelp.hindiMissing(macOs).let { println("${it.en}\n${it.hi}") }
    println("chosen: English ${english?.name ?: "-"}, Hindi ${hindi?.name ?: "-"}")

    val speaker = Speaker(renderer)
    val speeches = buildList {
        (english ?: voices.englishFallback())?.let { voice ->
            add(Triple(voice, "Where is the bus stop?", NORMAL))
            add(Triple(voice, "Where is the bus stop?", SLOW))
            add(Triple(voice, "ticket", SLOW))
        }
        hindi?.let { add(Triple(it, "पहले वाक्य सुनिए। फिर नारंगी माइक बटन दबाइए और वाक्य बोलिए।", HINDI_HELP)) }
    }
    for ((voice, text, speed) in speeches) {
        val start = System.nanoTime()
        val speech = speaker.prepare(voice, text, speed)
        val readyMs = (System.nanoTime() - start) / 1_000_000
        val plain = speaker.prepare(voice, text, 1f)
        println("${voice.name} at ${speed}×: ${speech.durationMs} ms (${plain.durationMs} ms at 1×), ready after $readyMs ms: $text")
        if (!silent) speaker.play(speech)
    }
    exitProcess(if (english == null || hindi == null) 1 else 0)
}
