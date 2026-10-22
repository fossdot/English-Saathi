package net.bodhya.speak.desktop

import net.bodhya.speak.desktop.speech.ModelFiles
import net.bodhya.speak.desktop.speech.SpeechModel
import java.io.File

/**
 * Where the app's own files are: the lesson packs that come with it, the
 * speech models, the speech library and the speech check's test clips. When
 * run from Gradle they are the repo's; in English Saathi.app they are inside
 * the bundle (docs/packaging.md). The app never writes here, so an update
 * that replaces the app loses nothing: everything a learner does is in the
 * data folder.
 */
class AppFiles(
    /** The lesson packs that come with the app. */
    val packs: File,
    /** The speech library, one folder per processor (osx-x64, osx-aarch64). */
    val native: File,
    private val modelDirs: Map<SpeechModel, File>,
    /** Test clips for the speech check, by file name. */
    val clips: List<File>,
    /** Running as English Saathi.app (not from Gradle). */
    val packaged: Boolean,
) {
    fun model(model: SpeechModel): ModelFiles = model.files(modelDirs.getValue(model))

    companion object {
        /** The packaged app's launcher sets this to the bundle's resources folder (Compose's appResourcesRootDir). */
        const val RESOURCES_PROPERTY = "compose.application.resources.dir"

        /**
         * The repo's files. The bus-stand clips are made locally with the macOS
         * voices, whose licence doesn't allow sharing recordings of them, so
         * they are only ever here, never in the app.
         */
        fun repo(projectDir: File) = AppFiles(
            packs = File(projectDir, "content/packs"),
            native = File(projectDir, "desktop/native"),
            modelDirs = mapOf(
                SpeechModel.TINY_EN to File(projectDir, "app/src/main/assets/asr/whisper-tiny.en"),
                SpeechModel.SMALL_EN to File(projectDir, "desktop/models/whisper-small.en"),
            ),
            clips = (listOf(File(projectDir, "app/src/androidTest/assets/asr-test/librispeech-0.wav")) +
                (1..5).map { File(projectDir, "app/src/debug/assets/asr-test/bus_0$it.wav") }).filter { it.isFile },
            packaged = false,
        )

        /** The app bundle's files, laid out by desktop/build.gradle.kts's bundle task. */
        fun bundle(resources: File) = AppFiles(
            packs = File(resources, "content/packs"),
            native = File(resources, "native"),
            modelDirs = SpeechModel.entries.associateWith { File(resources, "models/whisper-${it.id}") },
            clips = listOf(File(resources, "clips/librispeech-0.wav")).filter { it.isFile },
            packaged = true,
        )

        /** The bundle's files when running as the app, otherwise the repo given as the first argument (or the current folder). */
        fun find(args: Array<String>): AppFiles {
            val resources = System.getProperty(RESOURCES_PROPERTY)?.let(::File)
            return if (resources != null && File(resources, "content/packs").isDirectory) bundle(resources)
            else repo(File(args.firstOrNull { !it.startsWith("--") } ?: ".").absoluteFile)
        }
    }
}
