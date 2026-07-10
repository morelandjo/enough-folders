plugins {
    id("java")
    id("fabric-loom")
}

val modId = property("mod_id") as String

version = property("mod_version") as String
group = property("mod_group") as String
base.archivesName = "$modId-${stonecutter.current.project}"

java.toolchain.languageVersion = JavaLanguageVersion.of((property("java_version") as String).toInt())

sourceSets.main {
    java.setSrcDirs(listOf(rootProject.file("src/${property("deps.minecraft")}/java")))
    resources.setSrcDirs(listOf(rootProject.file("src/${property("deps.minecraft")}/resources")))
    java.exclude("com/vodmordia/enoughfolders/forge/**", "com/vodmordia/enoughfolders/neoforge/**")
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com/") { name = "Jared" }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
}

loom {
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "$modId.refmap.json"
    }
    runs {
        named("client") {
            client()
            runDir = "run"
            programArgs("--username=Dev")
        }
        named("server") {
            server()
            runDir = "run"
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("dev.architectury:architectury-fabric:${property("deps.architectury")}")
    modImplementation("mezz.jei:jei-${property("deps.minecraft")}-fabric:${property("deps.jei")}")
}

val metadataProps = mapOf(
    "mod_id" to modId,
    "mod_name" to property("mod_name"),
    "mod_version" to property("mod_version"),
    "mod_author" to property("mod_author"),
    "mod_authors" to property("mod_author"),
    "mod_description" to property("mod_description"),
    "mod_license" to property("mod_license"),
    "minecraft_version" to (property("deps.minecraft") as String),
    "fabric_loader_version" to (property("deps.fabric_loader") as String),
    "java_compat" to (property("java_compat") as String),
    "pack_format" to (property("pack_format") as String),
)

tasks.processResources {
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
    val props = metadataProps
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "pack.mcmeta", "*.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
