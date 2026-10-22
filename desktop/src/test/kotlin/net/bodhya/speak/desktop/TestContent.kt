package net.bodhya.speak.desktop

import net.bodhya.speak.desktop.app.SentenceRef
import net.bodhya.speak.desktop.content.LessonLibrary
import net.bodhya.speak.desktop.content.LessonPack
import net.bodhya.speak.desktop.content.PackLoader
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Hints
import net.bodhya.speak.domain.Lesson
import java.io.File

/** The app's own lesson pack (content/packs/starter.json), as the tests use it. */
object TestContent {
    val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    val builtInDir = File(projectDir, "content/packs")

    val starter: LessonPack by lazy { PackLoader.load("starter.json", File(builtInDir, "starter.json").readText(), emptyList()).pack }

    /** Bus stand, the first lesson. */
    val lesson: Lesson get() = starter.lessons.first()
    val hints: Hints get() = starter.hints

    /** Sentence [index] of [lesson] (Bus stand), as the practice screens name it. */
    fun at(index: Int, lesson: Lesson = this.lesson) = SentenceRef(lesson, index)

    fun library(dataDir: File, builtIn: File = builtInDir): LessonLibrary = LessonLibrary.load(builtIn, dataDir, GameConfig())

    /** A small lesson in the agreed format: [sentences] are (id, text); no word entries, so tips are generic. */
    fun lessonJson(id: String, titleEn: String, titleHi: String, sentences: List<Pair<String, String>>): String =
        """{ "id": "$id", "title_en": "$titleEn", "title_hi": "$titleHi", "words": {},
             "sentences": [ ${sentences.joinToString(",") { (sid, text) -> """{ "id": "$sid", "text": "$text", "hi": "", "chunks": [] }""" }} ] }"""

    /** A lesson pack in format 1 around [lessons] (lesson JSON objects). */
    fun packJson(id: String, version: Int, order: Int, vararg lessons: String, title: String = "Pack $id", format: Int = 1, extra: String = ""): String =
        """{ "format": $format, "id": "$id", "version": $version, "order": $order, "title_en": "$title", "title_hi": "पैक $id" $extra,
             "lessons": [ ${lessons.joinToString(",")} ] }"""

    /** Market: two lessons' worth of sentences for packs in tests. */
    val market = lessonJson("market", "Market", "बाज़ार", listOf("mkt_01" to "How much are the bananas?", "mkt_02" to "Two kilos of rice, please."))
    val bank = lessonJson("bank", "Bank", "बैंक", listOf("bnk_01" to "I want to open an account."))
}
