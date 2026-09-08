# Self Check

Self Check subscribes to every event the loader can deliver, records what
arrives, and answers events that a mod may rewrite.

The window tracks twenty scenarios. ⬜ means waiting, ✅ means passed, and ❌
means failed. Keep it open while you play to see which events and rewrites make
the full trip between the game and the mod.

Run these commands from the `mods` repository root:

```
gradlew :self-check:assembleSacredMod
gradlew :self-check:installSacredMod -PsacredDir="C:/Games/..."
python self-check/verify.py
java -cp self-check/build/sacred-mod/self-check-0.99.0.jar dev.ancaria.selfcheck.view.Preview
```

The build writes `self-check/build/sacred-mod/self-check-0.99.0.jar`, and the
install task copies that jar to `<Sacred Gold>/mods`.

`python self-check/verify.py` needs the built mod jar and Coderpack’s `api` and `zygote`
jars in Maven Local. It starts the zygote, acts as the host, checks all seven
vetoable verdicts, and then exits.

## What it changes

Receiving an event proves that the game-to-mod path works. The vetoable
scenarios also send a verdict back:

| Event | What Self Check does |
|---|---|
| damage | raises the remaining health by 1 HP, capped at maximum health |
| gold gained | raises the gain by one coin |
| experience | raises the new total by one point |
| skill, attribute, player pickup | answers with the value the game already supplied |

Damage at maximum health passes without sending a rewrite because there is
nothing to soften. Skill, attribute, and player-pickup verdicts return the
current value, exercising the round trip without changing the save. Pickups
that do not belong to the player are logged but do not pass the pickup
scenario.

Gold spent is left unchanged.

## Code structure

| Package | Job |
|---|---|
| `model` | Plain data for scenarios, statuses, and log lines |
| `viewmodel` | Observable state and the operations that update it |
| `view` | The read-only window |
| root package | `SelfCheckMod`, event subscriptions, and the scenario catalogue |

The loader dispatches events on `sal-dispatch`. `SelfCheckModel` moves every UI
update onto the JavaFX application thread, so listeners never mutate JavaFX
state directly.

## JavaFX choices

**Toolkit startup.** The mod calls `Platform.startup` through `Ui.boot()` and
does not use an `Application` subclass. If JavaFX cannot start, the mod logs the
failure and continues checking events without a window.

**Bundled runtime.** The jar includes JavaFX 21.0.9 with the Windows classifier,
classes, and native libraries. The loader starts a plain Java 21 JVM without a
module path, so the mod must carry JavaFX itself. This accounts for most of the
jar’s size.

**`setImplicitExit(false)`.** Closing the window leaves the JavaFX toolkit
running. Its non-daemon thread would also keep the loader JVM alive after the
host disconnects, so the zygote calls `System.exit(0)` when it receives `BYE`
or reaches the end of the host pipe. Shutdown hooks still run.

**Bounded, styled log.** The log uses a `ListView` of styled lines and keeps the
newest 500 entries. New entries scroll into view automatically.

## The two `Game` calls

The first `Hero` event starts one daemon thread named `self-check-probe`.
That thread calls `typeName(9)` and `uiString("UI_STATS_VICTORY")`, then checks
that the first result starts with `TYPE_` and the second is not blank.

Each command blocks its caller for up to two seconds while the zygote’s reader
thread receives the reply. The loader supports commands from event listeners,
but waiting there would pause `sal-dispatch`. The probe thread keeps event
delivery moving while both round trips complete.
