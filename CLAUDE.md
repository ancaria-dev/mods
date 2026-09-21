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

Deciding checks exercise the return path. Damage preserves 1 extra HP when
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
from a deciding callback. The supported patterns are
`SMALL` or `MEDIUM` to `LARGE`, and `MINOR` or `MAJOR` to `FULL`. Missing
targets, already-large potions, unmatched names, and non-player pickups stay
unchanged.

The current implementation answers with `Pickup.Mutation.retype` and changes
only the item type. It changes the name and appearance but does not copy the larger potion's price
or modifiers. Its effect therefore remains the original potion's effect. Do not
describe this version as restoring full-size healing behavior.

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

**No mod here declares one, and that is the finished state rather than an
oversight.**

`self-check` used to name `old-huge-potions` and `all-my-runes`. All three
wrote `type` into the pickup verdict, that verdict was a map, and jars load in
alphabetical order, so `self-check` ran last and won. What that looked like: a
small potion came back as its original type instead of the large one, and a
rune came back with the original type but the copied one's price, level and
modifiers, which is an item nobody designed. `self-check` reported its own
check as passed the whole time, because it verifies that the verdict travels
and not what became of it.

The clash cannot happen now, and the reason came out of the API rather than
out of a declaration: the event is folded between listeners. `Pickup.edited()`
says whether somebody has already asked to change the object, and `value()` on
a numeric event is what the mods before this one decided rather than what the
game proposed. Asking was simply not possible before.

`self-check` therefore stands down whenever an earlier listener has decided,
through `edited()` on a pickup and `taken()` on a number. All three run
together. Do not put the declarations back without first making that probe
unsafe again.

Retyping stayed in the pickup verdict, and that was not the first answer. It
moved to `Game.retype` for a while, because it edits an object in the world and
outlives the event, which is a good reason. The reason it came back is timing:
a command is a round trip through the host while the game thread is stopped
waiting for the verdict, and the edit has to land before the game picks the
item up. `Game.retype` and `Game.reshape` are still there for an edit that is
not racing a pickup.

Before reaching for `conflicts`, check whether the clash is really between the
two mods. A listener that reads `value()`, decides from it and answers with a
mutation composes with whatever ran before it. One that answers with
`initial()` throws that work away, and that is a bug in the listener, not a
pair that cannot coexist.

What a declaration now does: the launcher offers both sides anyway and draws
`Clash.Sentence` in amber under each of them. It hides nothing. So a
declaration is a sentence shown to a player, and it should be true.

`tracer` declares no conflicts either, and cannot need one. Its monitor
listener cannot alter a verdict.

## SRML index generation

Run these commands from the repository root:

```
coderpack index
coderpack index --check
```

The first command writes `sacred.mods.repository.json`. The second writes
nothing and exits with status 1 when the committed file differs from a newly
generated index.

Neither has to be run by hand for a push to master. CI writes the index and
commits it, before the release step, so a tag points at a commit whose index
describes the jar being released.

What the index has to describe is the jar behind each download URL, not the jar
a run happened to build. The launcher refuses a download whose sha256 is not the
one the index published (`registry/install.go`), so the two are not
interchangeable. A mod whose tag `<id>-v<version>` already exists is therefore
indexed from the asset on that release, downloaded in the workflow; only a
version with no tag yet is indexed from the jar just built, which is the jar the
release step is about to attach to it. Change a mod without raising its version
and nothing published moves, which is what not raising it asked for.

The staged jars go to `coderpack index --jars` rather than the generated file
being edited afterwards, so the file is always written by the generator and its
formatting cannot drift.

Running `coderpack index` locally and committing it is still allowed but rarely
matches: `self-check` packs a jar that comes out a few bytes apart on a
developer machine and on the runner. That difference is exactly why the workflow
no longer compares anything.

On a pull request nothing can be pushed, so there CI only generates the index
and throws it away. Comparing it would ask a contributor to reproduce the
runner's jars byte for byte, which is the thing that cannot be relied on.
Generating still fails on what a pull request can genuinely get wrong: an
unreadable `registry.toml`, a duplicate mod id, or a jar the linter refuses.

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

Each mod has its own `version` in `<id>/build.gradle.kts`, and they do not
have to agree. `self-check` is on `0.99.2` and the other three on `0.99.1`,
because the pickup probe was fixed in that one mod and republishing three
unchanged jars to keep a number tidy is not a reason to publish anything.
`tools/version.ps1` refuses to run while they differ, which is the script
working as designed rather than a state to undo: align them by hand the next
time all four genuinely move together. The plugin and API dependency are pinned separately in
`gradle/libs.versions.toml` and are a different number. The generated descriptor
uses the project version. The API contract range is `[1,2)`, which is distinct
from any artifact version.

For every mod version change, update its `build.gradle.kts` and push. CI
rebuilds, writes the index, and cuts the release. Never reuse a published
version for different bytes: a change that reaches players is a version change,
and one that does not raise the version simply stays unpublished.

`tools/version.ps1` moves all four at once and refuses to run if they are not
already on the same version. It rewrites two shapes and only those: the
`version = "..."` line in each build script, and the file name
`self-check-<version>.jar` where README.md, verify.py and Preview.java spell it
out. It used to replace every occurrence of the old number in those files,
which also moved verify.py's `CODERPACK = "..."`, the coderpack artifact
version it resolves api and zygote by. Keep the rules that narrow, not a
blanket match on the number.

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
`old-huge-potions`, and `all-my-runes`, then compares nine decided verdicts
and the 19 traced event names. It stages but does not enable `self-check`.

That used to be because Self Check overwrote the behaviour under test, which
is fixed. It is still left out for a duller reason: with it enabled it adds
its own rewrite to every event the other three leave alone, so six of the nine
expected verdicts would have to be rewritten to describe a fourth mod rather
than the three under test. Enabling it by hand is the way to check the
standing-down behaviour, and `END 3` and `END 9` are the two lines to read:
a potion that stays upgraded and a rune that keeps its whole copy.

Self Check also has focused commands:

```
python self-check/verify.py
java -cp self-check/build/sacred-mod/self-check-0.99.1.jar dev.ancaria.selfcheck.view.Preview
```

`self-check/verify.py` needs the built mod jar plus Coderpack `api` and `zygote`
jars in Maven Local. It starts the zygote, acts as the host, and checks all
seven decided verdicts without the game. The preview command opens the UI
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
- Do not move `Game` calls into deciding event handlers. They can wait on the
  same pipeline that is waiting for the handler to return.
- Do not perform disk I/O from Tracer's event listener. A decidable event keeps
  the game thread waiting until dispatch completes.
