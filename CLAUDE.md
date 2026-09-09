# Mods

## Repository role

This repository owns the four official Sacred Gold mods and the default mod
registry. Each mod is an ordinary Gradle project that uses the
`dev.ancaria.coderpack` plugin. The launcher reads
`sacred.mods.repository.json`, displays the available mods, and installs a
selected jar into `<Sacred Gold>/mods`.

The registry follows Sacred Repository Mod Layout, or SRML, version 1. Any Git
repository with `sacred.mods.repository.json` at its root can use this layout.
The launcher keeps this repository's clone URL in `registry.Official` as its
default source. The launcher payload contains no mod jars.

## Repository layout

| Path | Ownership |
|---|---|
| `sacred.mods.repository.json` | Generated and committed SRML index. Never edit it by hand. |
| `registry.toml` | Hand-written registry name, description, URL, icon, and release URL template. |
| `icon.png` | The registry icon, currently 256 × 256 pixels. |
| `self-check/` | Self Check build, source, README, and icon. |
| `tracer/` | Tracer build, source, README, and icon. |
| `old-huge-potions/` | Old Huge Potions build, source, README, and icon. |
| `all-my-runes/` | All My Runes build, source, README, and icon. |
| `tools/icons.py` | Pillow script that draws all five icons. It is not part of the build. |
| `settings.gradle.kts` | One multi-project build that includes all four mods. |
| `.github/workflows/build.yml` | CI build, verification, index regeneration, and per-mod release workflow. |

A multi-mod directory name must equal the mod id. `coderpack index` uses that
match to set `source` and `icon`. If the names differ, the generated entry has
neither field and the generator does not report the mismatch.

## Mod boundaries and behavior

### Self Check

`self-check` subscribes to every supported event and tracks 20 scenarios in a
JavaFX window. `SelfCheckMod` owns event subscriptions and the two off-thread
`Game` probes. `Checks` owns the scenario catalogue. The `model`, `viewmodel`,
and `view` packages own data, observable state, and the read-only window.

Vetoable checks exercise the return path. Damage preserves 1 extra HP when
possible. Gold gains and experience totals increase by 1. Skill, attribute,
and player-pickup verdicts return the current value. Gold spending and
non-player pickups remain unchanged.

Keep `Game.typeName(9)` and `Game.uiString("UI_STATS_VICTORY")` on the daemon
thread named `self-check-probe`. A command can block for up to two seconds.
Waiting inside an event listener would pause `sal-dispatch`.

The window uses JavaFX 21.0.9 with Windows-classified base, graphics, and
controls jars bundled into the mod. UI changes must go through the JavaFX
application thread. `Platform.setImplicitExit(false)` keeps the toolkit alive
after the window closes, so the zygote must continue to terminate the JVM on
`BYE` or end of pipe.

### Tracer

`tracer` writes every event to
`<Sacred Gold>/logs/logs-<yyyyMMdd-HHmmss>.txt`. `TracerMod` opens the sink and
registers the shutdown hook. `Recorder` owns the single base-`Event` listener
and line formatting. `Ring` owns the fixed 8,192-line buffer. `Sink` owns the
daemon writer thread and batched disk flushes.

The listener must stay at `Priority.MONITOR`. It records the accumulated
verdict after `FIRST`, `NORMAL`, and `LAST`, while the event bus discards any
rewrite attempted by a monitor listener. The event thread must never perform
file I/O or wait for ring capacity. When the ring fills, it overwrites the
oldest pending lines and reports the loss in the next batch.

### Old Huge Potions

`old-huge-potions` changes a player-picked potion to the largest type in the
same naming family. `PotionsMod` owns event handling. `Upgrades` derives the
mapping from names returned by the running game. Do not add hard-coded potion
ids or a fixed potion list.

Build the table after the first `Hero` event, when a world exists, and never
from a vetoable callback. The supported patterns are
`SMALL` or `MEDIUM` to `LARGE`, and `MINOR` or `MAJOR` to `FULL`. Missing
targets, already-large potions, unmatched names, and non-player pickups stay
unchanged.

The current implementation rewrites only the item type. It changes the name
and appearance but does not copy the larger potion's price or modifiers. Its
effect therefore remains the original potion's effect. Do not describe this
version as restoring full-size healing behavior.

### All My Runes

`all-my-runes` replaces a foreign rune with a randomly selected rune of the
hero's class that it has already seen during the current session. `RunesMod`
owns event handling. `Owners` maps rune types to classes. `Mine` keeps one
template per observed player-rune type and chooses a template.

The replacement copies the template's type, price, level, minimum level, and
modifiers. A type-only rewrite is wrong because the combat art lives in the
modifier list. Preserve the safe failure cases. Unknown rune types,
non-player pickups, and foreign runes seen before a player-rune template must
remain unchanged. Treat `VAMPIRESS_FORM` as `VAMPIRESS`.

`Owners` copies its bundled table to
`<Sacred Gold>/mods/all-my-runes.txt` on first use, then reads the external
file once per loader run. It resolves both `TYPE_SMOVE_UPGRADE_` and
`TYPE_SPELL_UPGRADE_` names after a `Hero` event. The bundled table assigns 79
of 140 known rune names and leaves 61 unassigned. Keep player edits outside the
jar and require a loader restart after changes.

## Conflicts

`self-check` declares conflicts with `old-huge-potions` and `all-my-runes`.
All three can write the pickup verdict, and the last listener wins. Keep both
declarations. They are the live example of the `conflicts` field and prevent
the diagnostic from undoing or obscuring the pickup mods.

Use `conflicts` only when two mods write the same field on the same event and
cannot work together. The launcher removes both sides of a conflict between
offered mods. It also removes a candidate that conflicts with an installed
mod, or that an installed mod names as a conflict. A careless declaration can
make a mod disappear from the Available list.

`tracer` declares no conflicts. Its monitor listener cannot alter a verdict.

## SRML index generation

Run these commands from the repository root:

```
coderpack index
coderpack index --check
```

The first command writes `sacred.mods.repository.json`. The second writes
nothing and exits with status 1 when the committed file differs from a newly
generated index.

Neither has to be run by hand for a push to master. CI regenerates the index
from the jars it just built and commits the result itself, before the release
step, so a tag always points at a commit whose index describes the jars being
released. Running `coderpack index` locally and committing it is still correct
and leaves CI nothing to do. On a pull request nothing can be pushed, so there
CI runs `--check` and fails on a mismatch as it always did.

The regeneration commit is made with the workflow token, and a push made with
that token starts no workflow run of its own, so this does not loop.

The generator verifies every jar first and refuses to index a jar that fails
the linter. It reads `id`, `name`, `version`, `description`, `api`, optional
`loader`, `authors`, `website`, and `conflicts` from
`META-INF/declaration.toml` inside the built jar. It derives `source` and
`icon` from the repository layout. It derives `file`, `size`, and `sha256`
from the jar, then expands `url` from the `registry.toml` release template.

`registry.toml` must provide nonblank `name`, `url`, and `releases` values.
The release template must contain `{file}` and may use `{id}` and `{version}`.
Do not add a timestamp to the generated index. A timestamp would make
`coderpack index --check` fail on every run.

Never create a hand-written `META-INF/declaration.toml`. The Gradle plugin
generates it from the mod's `sacred { }` block. A second copy can disagree with
the jar version and other build metadata.

## Versions and releases

Each mod currently has its own `version = "0.99.0"` in
`<id>/build.gradle.kts`. The plugin and API dependency are also currently
`0.99.0`. The generated descriptor uses the project version. The API contract
range is `[1,2)`, which is distinct from the `0.99.0` artifact version.

For every mod version change, update its `build.gradle.kts`, rebuild the jar,
run `coderpack index`, and commit the version change with the regenerated
index. Never reuse a published version for different bytes. `tools/version.ps1`
does the first step for all four mods at once (it refuses to run if they are
not already on the same version) but not the rebuild or the index. Those
still need `assembleSacredMod` and `coderpack index` by hand afterward.

CI creates one GitHub release per mod on `master`, tagged
`<id>-v<version>`, and uploads `<id>/build/sacred-mod/<file>`. The workflow
checks the remote tag first. An existing tag prevents another release, and a
push with no new version publishes nothing. The URL template in
`registry.toml` must continue to match this tag and asset layout.

The launcher downloads to a temporary `.part` file, checks the SHA-256, reads
the descriptor, verifies the offered id and supported API or loader ranges,
removes an older installed jar with the same id, and only then moves the new
jar into `<Sacred Gold>/mods`. A stale hash makes that release impossible to
install.

## Build and test commands

A JDK must be on `PATH`. The plugin compiles with `options.release = 21`.

```
gradlew assembleSacredMod
gradlew installSacredMod -PsacredDir="C:/Games/..."
```

`assembleSacredMod` builds and verifies all four jars, then writes them under
`<id>/build/sacred-mod/`. `installSacredMod` copies the verified jars into the
configured `<Sacred Gold>/mods` directory.

The cross-repository replay test belongs to `coderpack`:

```
cd ../coderpack && ./gradlew jar && python tests/replay.py
```

It stages this repository's built jars, enables `tracer`,
`old-huge-potions`, and `all-my-runes`, then compares nine vetoable verdicts
and the 19 traced event names. It stages but does not enable `self-check`
because Self Check answers pickups and would overwrite the behavior under
test.

Self Check also has focused commands:

```
python self-check/verify.py
java -cp self-check/build/sacred-mod/self-check-0.99.0.jar dev.ancaria.selfcheck.view.Preview
```

`self-check/verify.py` needs the built mod jar plus Coderpack `api` and `zygote`
jars in Maven Local. It starts the zygote, acts as the host, and checks all
seven vetoable verdicts without the game. The preview command opens the UI
without a game session.

## Repository boundaries

This build does not read sibling source directories. The Gradle plugin and the
API resolve from the Gradle Plugin Portal and Maven Central, per
`settings.gradle.kts`. `gradle/libs.versions.toml` holds the one `coderpack`
version number both the root `build.gradle.kts` (`alias(libs.plugins.coderpack)`)
and every mod's `sacred { apiVersion = libs.versions.coderpack.get() }` read,
so raising it moves every mod together.

The `coderpack` command line is a different kind of dependency: not a Maven
coordinate, but the `coderpack-*.zip` asset on an `ancaria-dev/build`
release, downloaded by CI and used for `coderpack index`. `dependencies.json`
pins the exact release tag CI downloads, never "latest", so a bad `build`
release cannot break this repository's CI on its own schedule. Bump the pin
there when there is a reason to move.

To test an unreleased Gradle-coordinate change, publish it to Maven Local
first, which `settings.gradle.kts` checks ahead of the portal and Central:

```
cd ../coderpack && ./gradlew publishToMavenLocal
cd ../build/gradle && ./gradlew publishToMavenLocal
```

The `launcher` reads this repository over HTTPS at run time. Do not add mod
jars to the launcher payload or `launcher/tools/build.ps1`. Players choose and
install mods from an SRML repository.

## Icons and game assets

Generate all icons with:

```
python tools/icons.py
```

The script requires Pillow. Keep icons square and below 256 KiB. The launcher
fetches at most `256 << 10` bytes, rejects non-images and SVG, then embeds
accepted icons as data URIs. Never put a game asset in an icon or elsewhere in
this repository. No Ancaria repository redistributes game content.

## Build warnings

- `self-check` must remain on the JavaFX 21 LTS line while the loader targets
  Java 21. JavaFX 26 uses class-file version 68 and cannot load on Java 21.
- Keep `exclude("META-INF/substrate/**")` on the Self Check `shadowJar`. The
  three JavaFX jars contain duplicate GraalVM native-image configuration, and
  the plugin deliberately uses `DuplicatesStrategy.INCLUDE`.
- A clean build needs network access for uncached Gradle and Maven artifacts,
  including JavaFX from Maven Central. Maven Local is checked before the
  Gradle Plugin Portal and Maven Central for the project-owned artifacts.
- Do not move `Game` calls into vetoable event handlers. They can wait on the
  same pipeline that is waiting for the handler to return.
- Do not perform disk I/O from Tracer's event listener. A vetoable event keeps
  the game thread waiting until dispatch completes.
