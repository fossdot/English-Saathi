pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "english-saathi"

// Pure Kotlin domain logic (no Android imports): normalizer, aligner, scorer,
// feedback selector, fixing loop, streak and Leitner rules.
include(":domain")

// Speech code that doesn't depend on Android: recording, end-of-speech
// detection, the speech engine and its settings. Shared by the Android and
// desktop apps.
include(":speech")

// The Android app: recording, speech recognition and the screens.
include(":app")

// The macOS desktop app (docs/desktop-plan.md). For now: the JVM speech
// recognizer and the D1 speech check.
include(":desktop")
