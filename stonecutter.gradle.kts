@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.13.6" apply false
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.141" apply false
}

stonecutter active file(".sc_active_version")

stonecutter parameters {
    // Defines `fabric` / `forge` / `neoforge` constants for `//? loader {` guards,
    // derived from the entry id suffix (e.g. "1.20.1-forge" -> forge).
    constants.match(current.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
}

tasks.register("runActiveClient") {
    group = "stonecutter"
    description = "Run client for the active Stonecutter version"
    dependsOn(stonecutter.current!!.project + ":runClient")
}

tasks.register("buildAll") {
    group = "build"
    description = "Build every supported Enough Folders target"
    dependsOn(
        ":1.21.1-neoforge:build",
        ":1.21.4-neoforge:build",
        ":1.20.1-forge:build",
        ":1.20.1-fabric:build",
        ":1.19.2-forge:build",
        ":1.19.2-fabric:build",
    )
}
