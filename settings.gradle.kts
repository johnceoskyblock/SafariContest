pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins { id("dev.kikugie.stonecutter") version "0.9.8" }

rootProject.name = "safari-contest"
stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    create(rootProject) {
        versions("26.1.2", "26.2", "26.3")
        vcsVersion = "26.1.2"
    }
}
