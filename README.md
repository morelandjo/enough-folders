# Enough Folders

Enough Folders organizes JEI ingredients into user-defined folders. This branch uses Stonecutter
to build all supported Minecraft and loader combinations from one Gradle workspace.

| Target | Java | Source set |
|---|---:|---|
| `1.21.1-neoforge` | 21 | `src/main` |
| `1.20.1-forge` | 17 | `src/1.20.1` |
| `1.20.1-fabric` | 17 | `src/1.20.1` |
| `1.19.2-forge` | 17 | `src/1.19.2` |
| `1.19.2-fabric` | 17 | `src/1.19.2` |

The 1.20.1 and 1.19.2 source sets are shared between Forge and Fabric; loader-only packages are
selected by the corresponding build script. The newer 1.21.1 implementation remains NeoForge-only.

## Commands

```sh
./gradlew buildAll
./gradlew :1.21.1-neoforge:runClient
./gradlew :1.20.1-forge:build
./gradlew "Set active project to 1.21.1-neoforge"
```

Built jars are written beneath `versions/<target>/build/libs/`.
