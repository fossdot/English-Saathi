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

// The Android app: recording, speech recognition and the screens.
include(":app")
