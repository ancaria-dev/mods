<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![SRML](https://img.shields.io/badge/SRML-1-C8A45C?style=for-the-badge&labelColor=1C1410)](#srml)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[Русский](README.md) · [Deutsch](README.DE.md)

</div>

# mods

This is the official Sacred Gold mod repository and the source tree for four
mods. The launcher knows its address from the start. Open the `Available` tab,
press `Install`, and the verified JAR lands in `<Sacred Gold>/mods`. No manual
download or file copying is needed.

You can add other repositories. The launcher accepts HTTPS clone URLs ending
in `.git` and reads `sacred.mods.repository.json` from the repository root.
Private repositories can use an access token.

## What is here

| Mod | What it does |
|---|---|
| [`self-check`](self-check) | Subscribes to every loader event and opens a window with 20 scenarios. White means unseen, green means passed, and red means an unexpected result. For events that accept an answer, the mod checks the full trip from the game to the JVM and back. |
| [`tracer`](tracer) | Writes every event to `<Sacred Gold>/logs/logs-<time>.txt`, one file per run. All listeners use `MONITOR` priority, so Tracer records the final answer and cannot change it. |
| [`old-huge-potions`](old-huge-potions) | Replaces each potion picked up by the player with the full-size type of the same kind. It builds the mapping from type names reported by the running game. The item's price stays unchanged, while the effect on healing has yet to be confirmed. |
| [`all-my-runes`](all-my-runes) | Replaces another class's rune with a copy of one of the hero's runes that the mod has already seen. Pick up one of your own first. Unknown runes are left unchanged. |

All four mods work together and none of them declares a conflict.

`self-check` used to name `old-huge-potions` and `all-my-runes`. All three
write an item's type at pickup, the last listener to run decides, and
`self-check` ran last: a potion came back as its original type instead of the
large one, and a rune came back with its original type alongside the copied
one's price, level and modifiers. `self-check` now stands down when another
mod has already written the field, so there is nothing left to declare.

## How to build

Put a JDK on `PATH`. The plugin compiles the mods for Java 21.

```
gradlew assembleSacredMod
```

This builds all four mods, checks every JAR with the linter, and writes the
results to `<mod>/build/sacred-mod/`. To install them directly into the game,
run:

```
gradlew installSacredMod -PsacredDir="C:/Games/Sacred Gold"
```

The `dev.ancaria.coderpack` plugin and API resolve as published dependencies,
not from sibling directories. Neither has had its first release, so this build
currently looks in Maven Local. Run `publishToMavenLocal` in the `build` and
`coderpack` repositories first. CI does the same in separate checkouts.

## SRML

SRML stands for Sacred Repository Mod Layout. Version 1 identifies a repository
by `sacred.mods.repository.json` at its root. This repository has the following
shape:

```
sacred.mods.repository.json   repository index, generated
registry.toml                 repository metadata, written by hand
icon.png                      repository icon
<id>/                         mod code, build.gradle.kts, and icon.png
```

A mod remains an ordinary Gradle project. In a repository with several mods,
the convention is to name each directory after the mod ID. `coderpack index`
then adds relative `source` and `icon` paths. If the directory has another
name, those fields are omitted. A single-mod repository may use its root
`icon.png` as the mod icon.

Do not edit the index by hand. Mod ID, name, version, description, API and
loader ranges, authors, website, and conflicts come from
`META-INF/declaration.toml` in each built JAR. Optional fields stay absent when
the descriptor or layout does not provide them. The generator takes the
repository name, description, site, icon, and release URL template from
`registry.toml`. It calculates the JAR filename, byte size, SHA-256, and
download URL from the artifact and template.

```
coderpack index           regenerate it
coderpack index --check   verify it without writing
```

Raising a version is enough. On `master`, CI writes the index and commits it,
so you never have to remember the command. It indexes a mod that is already
released from the JAR on that release, and one whose version has no tag yet from
the JAR it just built and is about to publish. That way the checksum in the
index always describes the file the download URL serves, which is what a
launcher checks before it installs anything.

On a pull request, where nothing can be pushed, CI generates the index and does
not compare it. Generation still refuses a JAR that fails the mod verifier, an
unreadable `registry.toml`, or two mods claiming one id.

## Releases

Each mod has its own GitHub release and a tag in the form
`<id>-v<version>`. The release contains that mod's JAR. On `master`, the
workflow builds all four mods, regenerates and commits the index, and creates
a release only when the corresponding tag is absent. A run with unchanged
versions publishes nothing.

The index records each JAR's URL, size, and SHA-256. Before installation, the
launcher checks the hash, reads the descriptor from the downloaded JAR, and
verifies its ID and loader compatibility. A failed download does not remain in
the mods directory.

You can publish another repository the same way. Set its name, site, and
release URL template in `registry.toml`, build the JARs, and run
`coderpack index`. Players can then add its HTTPS clone URL to the launcher.
