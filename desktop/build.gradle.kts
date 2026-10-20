import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":speech"))
    // sherpa-onnx's Java library has no Maven Central package; scripts/fetch_deps.sh downloads the release file.
    implementation(files("libs/sherpa-onnx-jvm-1.13.8.jar"))

    testImplementation(libs.junit)
}

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
    // The recognizer tests load the real models from the project folder.
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
