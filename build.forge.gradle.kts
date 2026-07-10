plugins {
    id("java")
    id("net.neoforged.moddev.legacyforge")
}

val modId = property("mod_id") as String

version = property("mod_version") as String
group = property("mod_group") as String
base.archivesName = "$modId-${stonecutter.current.project}"

java.toolchain.languageVersion = JavaLanguageVersion.of((property("java_version") as String).toInt())

sourceSets.main {
    java.setSrcDirs(listOf(rootProject.file("src/${property("deps.minecraft")}/java")))
    resources.setSrcDirs(listOf(rootProject.file("src/${property("deps.minecraft")}/resources")))
    java.exclude("com/vodmordia/enoughfolders/fabric/**", "com/vodmordia/enoughfolders/neoforge/**")
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com/") { name = "Jared" }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

legacyForge {
    version = "${property("deps.minecraft")}-${property("deps.forge")}"

    runs {
        register("client") {
            client()
            gameDirectory = file("run/")
            programArgument("--username=Dev")
        }
        register("server") {
            server()
            gameDirectory = file("run/")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    compileOnly("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    implementation("dev.architectury:architectury-forge:${property("deps.architectury")}")
    implementation("mezz.jei:jei-${property("deps.minecraft")}-forge:${property("deps.jei")}")
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
    "forge_version" to (property("deps.forge") as String),
    "forge_version_range" to "[${property("deps.forge")},)",
    "forge_loader_version_range" to "[${(property("deps.forge") as String).substringBefore('.')},)",
    "minecraft_version_range" to "[${property("deps.minecraft")},${(property("deps.minecraft") as String).substringBeforeLast('.')}.${(property("deps.minecraft") as String).substringAfterLast('.').toInt() + 1})",
    "java_compat" to (property("java_compat") as String),
    "pack_format" to (property("pack_format") as String),
)

tasks.processResources {
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
    val props = metadataProps
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "*.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}
