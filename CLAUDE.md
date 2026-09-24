# mods

Workspace rules, pins, and the no-game-assets rule: see `../CLAUDE.md`.

## Repository

- This is the default SRML repository (`registry.Official` in the launcher) and the source of the official mods. Each mod is a Gradle project using `dev.ancaria.coderpack`.
- A mod directory name must equal the mod id. Otherwise `coderpack index` silently omits `source` and `icon`.
- The build reads no sibling. The plugin and API resolve from the Plugin Portal and Central, with `mavenLocal()` first.
- To test an unreleased coordinate, run `./gradlew publishToMavenLocal` in `../coderpack` and in `../build/gradle`.
- Keep `plugin` and `api` as two entries in `gradle/libs.versions.toml`. Two repositories publish them and they move independently.
- `dependencies.json` pins the `build` release whose `coderpack-*.zip` CI uses for `coderpack index`.
- Never add mod jars to the launcher payload.

## Commands

- `gradlew assembleSacredMod` builds and verifies every mod into `<id>/build/sacred-mod/`.
- `gradlew installSacredMod -PsacredDir="<Sacred Gold>"` copies them into the game's `mods`.
- `python self-check/verify.py` drives Self Check without the game. It needs the built jar and coderpack `api` and `zygote` in Maven Local at the version its `CODERPACK` names.
- The cross-repository replay test lives in coderpack: `cd ../coderpack && ./gradlew jar && python tests/replay.py`. It stages but never enables `self-check`, whose extra rewrites would change most expected verdicts.
- `python tools/icons.py` (Pillow) draws every icon. Keep icons square and under 256 KiB; the launcher rejects larger ones and SVG.

## Index and releases

- Never edit `sacred.mods.repository.json`. CI writes and commits it before the release step, so a tag points at a commit whose index describes the released jar.
- CI indexes a mod whose `<id>-v<version>` tag exists from that release's asset, and a new version from the fresh jar. The launcher refuses a download whose SHA-256 differs from the index.
- A local `coderpack index` rarely matches CI: `self-check` packs a jar whose bytes differ between machines. On pull requests CI generates the index without comparing or committing it.
- `registry.toml` needs nonblank `name`, `url`, and `releases`. The releases template must contain `{file}` and may use `{id}` and `{version}`; it must match the `<id>-v<version>` tag and asset layout.
- Never add a timestamp to the index.
- Never write `META-INF/declaration.toml` by hand. The plugin generates it from `sacred { }`.
- Each mod has its own version in `<id>/build.gradle.kts`. Raise only the mod that changed.
- Never reuse a published version for different bytes. A change that should reach players needs a version bump; without one it stays unpublished.
- `tools/version.ps1` moves every mod at once and refuses while their versions differ; align them by hand only when all move together. It rewrites only `version = "..."` lines and `self-check-<version>.jar` spellings. Never widen it to a blanket match on the number; that once moved `CODERPACK` in `verify.py`.

## Listeners and conflicts

- Never move `Game` calls (`getWorld()`, `getTypeRegistry()`, `getConsole()` included) into deciding handlers. They wait on the pipeline that is waiting for the handler.
- No mod here declares `conflicts`. Never add one back while self-check's stand-down still works.
- `self-check` stands down when an earlier listener decided: `isEdited()` on a pickup, `getValue()` against `getInitial()` on a number.
- Before declaring a conflict, check the listeners. One that decides from `getValue()` composes; one that answers from `getInitial()` discards earlier work and is a bug.
- A declaration only shows `Clash.Sentence` under both mods in the launcher. Keep it true.
- Retyping stays in the pickup verdict, not `TypeRegistry.retype`. A command is a host round trip while the game thread waits, and the edit must land before the pickup.

## Self Check

- A scenario passes only on a real event or a real game answer. Add one to `Checks` with its hint, pass it from its event's listener, and add a frame to `verify.py`.
- Keep every `Game` call on the `self-check-probe` daemon thread. A command can block up to two seconds; inside a listener it stalls `sal-dispatch`, inside a deciding one the game thread too. `onUnload` stops the thread.
- Route hot events (`Spawn`, `Despawn`, `Sector`, `HealthChanged`) through `SelfCheckModel.tally`. Never switch them to `pass`, which costs an FX task and a log line per call.
- Touch the UI only on the JavaFX application thread. `Platform.setImplicitExit(false)` keeps the toolkit alive, so zygote must keep terminating the JVM on `BYE` or end of pipe.
- Stay on the JavaFX 21 LTS line while the loader targets Java 21. Newer JavaFX class files do not load on 21.
- Keep `exclude("META-INF/substrate/**")` on the Self Check `shadowJar`. The JavaFX jars carry duplicate native-image configuration and the plugin uses `INCLUDE`.

## Tracer

- Keep its listener at `Priority.MONITOR`. It records the final verdict, and the bus discards a monitor's rewrite.
- Never do file I/O or wait for ring capacity on the event thread. A decidable event holds the game thread until dispatch finishes. A full ring overwrites the oldest lines and reports the loss in the next batch.
- The trace goes to its own `<Sacred Gold>/logs/logs-<timestamp>.txt`, not `mods.log`. `onUnload` closes the sink.

## Old Huge Potions

- Never hardcode potion ids or a potion list. `Upgrades` derives the table from names the running game returns.
- Build the table after the first `Hero` event, never inside a deciding callback.
- It answers with `Pickup.Mutation.retype`: only the type changes, not price, modifiers, or effect. Never describe it as restoring full-size healing.
- Leave unchanged: missing targets, already-large potions, unmatched names, non-player pickups.

## All My Runes

- Copy the template's type, price, level, minimum level, and modifiers. A type-only rewrite is wrong; the combat art lives in the modifiers.
- Leave unchanged: unknown rune types, non-player pickups, and foreign runes seen before any player-rune template.
- Treat `VAMPIRESS_FORM` as `VAMPIRESS`.
- `Owners` copies its table to `<Sacred Gold>/mods/all-my-runes.txt` on first use and reads that file once per loader run. Keep player edits outside the jar; they need a loader restart.
