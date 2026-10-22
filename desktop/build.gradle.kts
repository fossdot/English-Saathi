import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":speech"))
    // sherpa-onnx's Java library has no Maven Central package; scripts/fetch_deps.sh downloads the release file.
    implementation(files("libs/sherpa-onnx-jvm-1.13.8.jar"))
    // Lesson and hint files (content/) and the saved data.
    implementation(libs.kotlinx.serialization.json)
    // The window. currentOs is this Mac's Compose desktop build; both carry the Intel and Apple silicon graphics libraries.
    implementation(compose.desktop.currentOs)
    implementation(libs.cmp.foundation)
    // Dispatchers.Main on the Swing thread: Compose 1.10.3 doesn't bring it.
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(libs.junit)
    testImplementation(libs.cmp.ui.test.junit4)
}

/**
 * English Saathi.app (docs/packaging.md): scripts/package_mac.sh builds it with
 * -Pdistribution, on an Intel Corretto 17 under Rosetta, so Compose takes the
 * Intel (x86_64) Skiko and speech library. Run from Gradle, the app reads the
 * repo's files instead (AppFiles.repo).
 */
val distribution = providers.gradleProperty("distribution").isPresent
val appVersion = "1.0.0"

/** What goes inside the app: Compose copies common/ and the folder for the build's processor into Contents/app/resources. */
val bundleResources = tasks.register<Sync>("bundleResources") {
    dependsOn("checkDesktopDeps")
    into(layout.buildDirectory.dir("bundle-resources"))
    from(rootProject.file("content/packs")) { include("*.json"); into("common/content/packs") }
    from(rootProject.file("app/src/main/assets/asr/whisper-tiny.en")) { into("common/models/whisper-tiny.en") }
    from(rootProject.file("desktop/models/whisper-small.en")) { into("common/models/whisper-small.en") }
    // The speech check's test clip and the self-test's (CC BY 4.0). The bus-stand clips are made with the macOS
    // voices, whose licence doesn't allow sharing recordings of them, so they never go in the app.
    from(rootProject.file("app/src/androidTest/assets/asr-test/librispeech-0.wav")) { into("common/clips") }
    from(file("packaging")) { include("THIRD-PARTY-NOTICES.txt", "onnxruntime-LICENSE.txt", "onnxruntime-ThirdPartyNotices.txt"); into("common") }
    from(file("native/osx-x64")) { into("macos-x64/native/osx-x64") }
    from(file("native/osx-aarch64")) { into("macos-arm64/native/osx-aarch64") }
}

compose.desktop {
    application {
        mainClass = "net.bodhya.speak.desktop.MainKt"
        // scripts/run_desktop.sh sets JAVA_HOME to Corretto 17: the app must run on a Java that may use the microphone.
        // scripts/package_mac.sh sets it to Corretto 17 for Intel, whose runtime goes inside the app.
        System.getenv("JAVA_HOME")?.let { javaHome = it }
        // The repo, for a run from Gradle; the app finds its files in its bundle (and must not carry this Mac's paths).
        if (!distribution) args(rootProject.projectDir.absolutePath)
        jvmArgs(
            "-Dapple.awt.application.name=English Saathi",
            "-Xdock:name=English Saathi",
            // On a 2019 MacBook Pro Skiko's "auto" picks the AMD GPU, which costs battery.
            "-Dskiko.gpu.priority=integrated",
            // Prints the graphics card Skiko uses, for the MacBook checklist.
            "-Dskiko.hardwareInfo.enabled=true",
        )
        nativeDistributions {
            packageName = "English Saathi"
            packageVersion = appVersion
            description = "Spoken English practice for Hindi speakers, offline"
            if (distribution) appResourcesRootDir.set(layout.buildDirectory.dir("bundle-resources"))
            // suggestRuntimeModules finds java.instrument and jdk.unsupported; the rest are cheap insurance against
            // something loaded by reflection that jdeps can't see. The self-test exercises the runtime.
            modules("java.instrument", "jdk.unsupported", "java.naming", "java.prefs", "jdk.charsets", "jdk.localedata", "jdk.zipfs")
            macOS {
                bundleID = "net.bodhya.speak"
                dockName = "English Saathi"
                // Corretto 17.0.20's Intel programs and libraries are built for macOS 11.0 or later.
                minimumSystemVersion = "11.0"
                iconFile.set(project.file("packaging/English Saathi.icns"))
                appCategory = "public.app-category.education"
                infoPlist {
                    // Shown in macOS's microphone prompt, which names English Saathi (docs/packaging.md). Hindi drafted.
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>English Saathi listens while a student says a sentence, to check the words. Recordings stay on this Mac. · वाक्य बोलते समय English Saathi सुनता है ताकि शब्द जाँच सके। रिकॉर्डिंग इसी Mac पर रहती है।</string>
                    """.trimIndent()
                }
            }
        }
    }
}

tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(bundleResources) }

/**
 * The speech library, native libraries and models from scripts/fetch_deps.sh
 * must be present and be exactly the tested files: a damaged model makes
 * sherpa-onnx end the whole process instead of reporting an error. The
 * desktop app also uses the Android app's tiny.en files.
 */
abstract class CheckDesktopDeps : DefaultTask() {
    @get:InputFile
    abstract val hashList: RegularFileProperty

    @get:InputFiles
    abstract val checkedFiles: ConfigurableFileCollection

    @get:Internal
    abstract val repoDir: DirectoryProperty

    @get:OutputFile
    abstract val stamp: RegularFileProperty

    @TaskAction
    fun check() {
        val problems = expected(hashList.get().asFile).mapNotNull { (path, hash) ->
            val file = repoDir.get().file(path).asFile
            when {
                !file.isFile -> "$path is missing"
                sha256(file) != hash -> "$path is not the tested file"
                else -> null
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(problems.joinToString("\n") + "\nRun scripts/fetch_deps.sh.")
        }
        stamp.get().asFile.writeText("checked\n")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** The (path, sha256) pairs the desktop app needs: its own files and the tiny.en model. */
        fun expected(hashList: File): List<Pair<String, String>> = hashList.readLines()
            .filter { it.isNotBlank() }
            .map { line -> line.split(Regex("\\s+"), limit = 2).let { (hash, path) -> path to hash } }
            .filter { (path, _) -> path.startsWith("desktop/") || path.startsWith("app/src/main/assets/asr/") }
    }
}

val depsHashList = rootProject.file("scripts/deps.sha256")
val checkDesktopDeps = tasks.register<CheckDesktopDeps>("checkDesktopDeps") {
    hashList.set(depsHashList)
    checkedFiles.from(CheckDesktopDeps.expected(depsHashList).map { (path, _) -> rootProject.file(path) })
    repoDir.set(rootProject.layout.projectDirectory)
    stamp.set(layout.buildDirectory.file("checkDesktopDeps.txt"))
}
tasks.named("compileKotlin") { dependsOn(checkDesktopDeps) }

tasks.test {
    // The recognizer and content tests read the real models and lesson files from the project folder.
    systemProperty("projectDir", rootProject.projectDir.absolutePath)
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

/*
 * D1's speech check (docs/desktop-plan.md): every model × thread count runs
 * in its own JVM, one after another, and appends to
 * build/speech-check/<os.arch>.log (x86_64 for Intel Java, also under Rosetta).
 *
 *   ./gradlew :desktop:speechCheck                          # both models, 4 threads
 *   ./gradlew :desktop:speechCheck -Pthreads=2,4,6
 *   ./gradlew :desktop:speechCheck -PcheckJava=/path/to/x64/jdk/bin/java   # e.g. Intel Java under Rosetta
 */
val checkModels = (findProperty("models") as String? ?: "tiny.en,small.en").split(",")
val checkThreads = (findProperty("threads") as String? ?: "4").split(",")
val checkJava = findProperty("checkJava") as String?
val checkLogDir = layout.buildDirectory.dir("speech-check")
var previousCheck: TaskProvider<JavaExec>? = null
val speechChecks = checkModels.flatMap { model -> checkThreads.map { threads -> model to threads } }.map { (model, threads) ->
    val after = previousCheck
    tasks.register<JavaExec>("speechCheck-$model-${threads}t") {
        group = "verification"
        description = "Runs the D1 speech check with $model and $threads threads."
        dependsOn(checkDesktopDeps)
        after?.let { mustRunAfter(it) }
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("net.bodhya.speak.desktop.check.SpeechCheckKt")
        checkJava?.let { setExecutable(it) }
        args(rootProject.projectDir.absolutePath, model, threads, checkLogDir.get().asFile.absolutePath)
    }.also { previousCheck = it }
}
tasks.register("speechCheck") {
    group = "verification"
    description = "Runs the D1 speech check for every model and thread count."
    dependsOn(speechChecks)
}

/*
 * D2's voice check (docs/desktop-plan.md): lists the voices, shows which the
 * app picks and how long each speech lasts, and plays them.
 *
 *   ./gradlew :desktop:voiceCheck
 *   ./gradlew :desktop:voiceCheck -Psilent      # measure without playing
 */
tasks.register<JavaExec>("voiceCheck") {
    group = "verification"
    description = "Runs the D2 voice check."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.bodhya.speak.desktop.check.VoiceCheckKt")
    args(rootProject.projectDir.absolutePath)
    if (hasProperty("silent")) args("--silent")
}
