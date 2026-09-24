<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![SRML](https://img.shields.io/badge/SRML-1-C8A45C?style=for-the-badge&labelColor=1C1410)](#srml)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[Русский](README.md) · [Deutsch](README.DE.md)

</div>

# mods

The official Sacred Gold mod repository and the source of the four mods in it.

The launcher connects to this repository on its own, with nothing to set up.
You pick a mod, and the launcher downloads it, checks it, and puts it in
`<Sacred Gold>/mods`.

A mod repository is an ordinary Git repository with an index at its root. You
can start one for your own mods, and players add it by its URL.

## Getting started

### Install a mod

1. Open the launcher and go to the Available tab.
2. Press Install next to the mod you want.
3. Press Play.

### Add another repository

The launcher takes an HTTPS clone URL ending in `.git`. For a private
repository, add an access token.

### Share your mod

Publish your mod in your own repository using the [SRML](#srml) layout.
Players can add it right away. To list it in the catalog on the site, press
Submit your mod on [ancaria.dev/mods](https://ancaria.dev/mods).

## Mods

| Mod | What it does |
|---|---|
| [`self-check`](self-check) | Checks that the loader works. It subscribes to every event and opens a window with 50 scenarios, each with a hint on how to pass it. White means not seen yet, green means passed, red means an unexpected result. A side panel shows live hero data. |
| [`tracer`](tracer) | Writes every event to `<Sacred Gold>/logs/logs-<time>.txt`, one file per run. It sees the final answer from all mods but can't change it. |
| [`old-huge-potions`](old-huge-potions) | Turns each potion you pick up into the full-size one of the same kind. Only the type changes, so the price and effect stay the same. |
| [`all-my-runes`](all-my-runes) | Turns another class's rune into a copy of one of your hero's runes. Pick up at least one of your own first. Runes the mod doesn't know stay as they are. |

All four mods work together without conflicts. Each one sees what the mods
before it decided, and `self-check` stays out of an event another mod has
already decided.

## SRML

SRML stands for Sacred Repository Mod Layout, version 1. Any Git repository
with `sacred.mods.repository.json` at its root counts as a mod repository.
This one looks like this:

```
sacred.mods.repository.json   the index, generated
registry.toml                 repository details, written by hand
icon.png                      the repository icon
<id>/                         a mod: code, build.gradle.kts, icon.png
```

Each mod stays an ordinary Gradle project. Name its folder after the mod ID,
and `coderpack index` adds `source` and `icon` paths to the index. With any
other name, those fields are left out. In a single-mod repository, the root
`icon.png` doubles as the mod's icon.

Don't edit the index by hand. The generator reads each mod's details from
`META-INF/declaration.toml` in the built jar: ID, name, version, description,
compatibility, authors, website, and conflicts. It takes the repository name,
site, icon, and release URL template from `registry.toml`. It works out the
file name, size, SHA-256, and download URL itself.

```
coderpack index           regenerate the index
coderpack index --check   check it without writing
```

Your own repository works the same way. Fill in `registry.toml`, build your
mods, and run `coderpack index`.

## Building

Put a JDK on `PATH`. The mods compile for Java 21.

```
gradlew assembleSacredMod
```

This builds all four mods, runs the linter on them, and writes the jars to
`<mod>/build/sacred-mod/`. To install them straight into the game:

```
gradlew installSacredMod -PsacredDir="C:/Games/Sacred Gold"
```

The `dev.ancaria.coderpack` plugin comes from the Gradle Plugin Portal and the
API from Maven Central. `gradle/libs.versions.toml` pins both. Maven Local
comes first, so `publishToMavenLocal` in `build` or `coderpack` lets you try an
unreleased change.

## Releases

Each mod has its own releases, tagged `<id>-v<version>`. To release a mod,
raise its version in `build.gradle.kts` and push to `master`. CI builds the
mods, regenerates and commits the index, and publishes a release when that tag
doesn't exist yet.

The index always describes the exact jar behind its download URL. Before
installing, the launcher checks the SHA-256, reads the descriptor, and checks
compatibility. A file that fails never reaches the mods folder.

## License

MIT, see [LICENSE](LICENSE).
