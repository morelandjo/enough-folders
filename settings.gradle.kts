pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.2"
}

stonecutter {
    create(rootProject) {
        fun entry(mc: String, loader: String) {
            version("$mc-$loader", mc).buildscript = "build.$loader.gradle.kts"
        }
        // Targets. The active/vcs version is the one sources are authored against;
        // every other target is reached via `//? loader {` / `//? >=1.21 {` guards.
        entry("1.21.1", "neoforge")
        entry("1.20.1", "forge")
        entry("1.20.1", "fabric")
        entry("1.19.2", "forge")
        entry("1.19.2", "fabric")

        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "enough-folders"
