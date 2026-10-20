import com.android.build.api.artifact.SingleArtifact
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // The package ID. It can never change once the app is on Google Play.
    namespace = "net.bodhya.speak"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.bodhya.speak"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        // The speech library also ships x86 builds; phones need only these two.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":speech"))
    // sherpa-onnx has no Maven package; scripts/fetch_deps.sh downloads the release file.
    implementation(files("libs/sherpa-onnx-static-link-onnxruntime-1.13.8.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}

/**
 * The speech library and model from scripts/fetch_deps.sh must be present
 * and be exactly the tested files: a wrong or damaged model makes sherpa-onnx
 * end the app instead of reporting an error.
 */
abstract class CheckFetchedDeps : DefaultTask() {
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
        /** The (path, sha256) pairs the app's build needs: the library and the model. */
        fun expected(hashList: File): List<Pair<String, String>> = hashList.readLines()
            .filter { it.isNotBlank() }
            .map { line -> line.split(Regex("\\s+"), limit = 2).let { (hash, path) -> path to hash } }
            .filter { (path, _) -> path.startsWith("app/libs/") || path.startsWith("app/src/main/") }
    }
}

val depsHashList = rootProject.file("scripts/deps.sha256")
val checkFetchedDeps = tasks.register<CheckFetchedDeps>("checkFetchedDeps") {
    hashList.set(depsHashList)
    checkedFiles.from(CheckFetchedDeps.expected(depsHashList).map { (path, _) -> rootProject.file(path) })
    repoDir.set(rootProject.layout.projectDirectory)
    stamp.set(layout.buildDirectory.file("checkFetchedDeps.txt"))
}
tasks.named("preBuild") { dependsOn(checkFetchedDeps) }

/**
 * The app must work fully offline. This step sits in the merged-manifest
 * pipeline, so every APK, bundle and install of every variant passes through it.
 */
abstract class CheckNoInternetPermission : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val checkedManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val manifest = mergedManifest.get().asFile
        if ("android.permission.INTERNET" in manifest.readText()) {
            throw GradleException("$manifest asks for android.permission.INTERNET; the app must stay offline.")
        }
        manifest.copyTo(checkedManifest.get().asFile, overwrite = true)
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val check = tasks.register<CheckNoInternetPermission>("check${name}NoInternetPermission")
        variant.artifacts.use(check)
            .wiredWithFiles(CheckNoInternetPermission::mergedManifest, CheckNoInternetPermission::checkedManifest)
            .toTransform(SingleArtifact.MERGED_MANIFEST)
    }
}
