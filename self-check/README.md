# Self Check

Self Check subscribes to every event the loader can deliver, records what
arrives, answers events that a mod may rewrite, and reads the hero through the
direct API.

The window tracks fifty scenarios. ⬜ means waiting, ✅ means passed, and ❌
means failed. Under each scenario's name a short line says what to do in the
game to pass it, for example "Open any chest or barrel" or "Sell an item to a
merchant by dragging it or with Shift+click". Keep the window open while you
play to see which events, rewrites and commands make the full trip between the
game and the mod.

Run these commands from the `mods` repository root:

```
gradlew :self-check:assembleSacredMod
gradlew :self-check:installSacredMod -PsacredDir="C:/Games/..."
python self-check/verify.py
java -cp self-check/build/sacred-mod/self-check-0.200.0.jar dev.ancaria.selfcheck.view.Preview
```

The build writes `self-check/build/sacred-mod/self-check-0.200.0.jar`, and the
install task copies that jar to `<Sacred Gold>/mods`.

`python self-check/verify.py` needs the built mod jar and Coderpack’s `api` and
`zygote` jars in Maven Local. It starts the zygote, acts as the host, sends one
frame for every scenario, answers the mod's commands, checks all ten decided
verdicts and the console answer, and then exits.

## Scenarios

| Scenario | How to pass it |
|---|---|
| World loads and unloads | Load a save or start a new game |
| Save is loaded | Load a saved game from the main menu |
| Hero is captured | Enter the game with any character |
| Game answers getTypeName | Nothing to do: asked once the hero is in the game |
| Game answers getTypeId | Nothing to do: asked once the hero is in the game |
| Game answers getUiString | Nothing to do: asked once the hero is in the game |
| Hero is read through the API | Nothing to do: read every two seconds, shown in the Hero panel |
| World lists its creatures | Nothing to do: asked once the hero is in the game |
| Player moves | Walk a few steps |
| Hero enters a sector | Walk on until the hero crosses into the next map sector |
| Hero changes region | Travel to another region, through a gate, a portal or a teleporter |
| An area is discovered | Explore until the game reports a newly discovered area |
| Damage softened by 1 HP | Let a monster hit you |
| Health changes | Take damage, or drink a healing potion |
| Maximum health changes | Level up, or equip or remove an item that changes maximum health |
| Near death is announced | Let your health fall close to zero |
| Death is announced | Let the hero die |
| Hero is resurrected | Die and come back at a resurrection point |
| A creature spawns | Walk towards monsters or into a town |
| A creature despawns | Walk far away from creatures you have seen, or kill one |
| A creature is hurt | Hit any monster |
| A creature dies | Kill any monster |
| A kill is counted | Defeat an opponent, so the statistics count goes up |
| A creature drops loot | Kill monsters until one drops an item |
| A chest is looted | Open any chest or barrel |
| Gold gain raised by 1 | Pick up gold |
| Gold total changes | Pick up, spend or earn gold |
| Experience raised by 1 | Kill a monster to earn experience |
| Experience total changes | Earn experience |
| Level goes up | Earn enough experience to reach the next level |
| Skill points change | Level up, or spend a skill point |
| Skill write is answered | Spend a skill point in the character screen |
| Skill level changes | Spend a skill point in the character screen |
| Attribute points change | Level up, or spend an attribute point |
| Attribute spend is answered | Spend an attribute point in the character screen |
| Attribute value changes | Spend an attribute point in the character screen |
| Combat art raise is answered | Read a rune of your class to learn or raise a combat art |
| Combat art level changes | Read a rune of your class to learn or raise a combat art |
| Pickup is answered | Pick up any item |
| Item goes into the bag | Pick up an item into your inventory |
| Item is equipped | Equip or take off a weapon, a piece of armour or a ring |
| Item is dragged | Drag an item to another inventory slot |
| Hero drinks a potion | Drink any potion |
| Item is bought | Buy an item from a merchant |
| Item is sold | Sell an item to a merchant by dragging it or with Shift+click |
| A quest starts | Accept a quest from a character with a quest marker |
| A quest ends | Finish a quest and hand it in |
| Game is saved | Save the game |
| Console command is claimed | Open the game console and type selfcheck |
| An event the SDK has no class for | Nothing to do: passes on an event this API has no class for |

Every scenario passes on a real event or a real answer from the game. None of
them is ticked by a timer. A save the game reports as failed, a type name that
does not map back to its id, or a hero reading that comes back empty marks its
row ❌ instead.

## What it changes

Receiving an event proves that the game-to-mod path works. The deciding
scenarios also send a verdict back:

| Event | What Self Check does |
|---|---|
| damage | raises the remaining health by 1 HP, capped at maximum health |
| gold gained | raises the gain by one coin |
| experience | raises the new total by one point |
| skill, attribute, combat art, player pickup | answers with the value the game already supplied |
| console line `selfcheck` | vetoes it and answers in the console |

Damage at maximum health passes without sending a rewrite because there is
nothing to soften. Skill, attribute, combat-art and player-pickup verdicts
return the current value, exercising the listener's return path without
changing the save. Pickups that do not belong to the player are logged but do
not pass the pickup scenario.

Gold spent is left unchanged.

Self Check stands down when another mod has already decided the event. A
number whose `getValue()` no longer equals its `getInitial()`, or a pickup
whose `isEdited()` is true, passes its scenario without a rewrite. This keeps it
from undoing Old Huge Potions or All My Runes.

## The console command

Type `selfcheck` in the game console. Self Check vetoes the line, so the game
never sees it and prints no error, and then answers with one line such as
`Self Check: 31 of 50 scenarios passed` through
`getContext().getGame().getConsole().print(...)`. Any other line is left to
the game.

The veto is returned from the listener at once. The answer is a command, so it
is sent afterwards from the probe thread rather than from the listener that the
game thread is waiting on.

## The hero panel

The right-hand column shows the hero as the direct API reads it: class, level,
HP and maximum HP, gold, experience, position, region and sector, the six
attributes and the points left to spend, skill slots and skill points, learned
combat arts with their base level and gear bonus, the journal statistics (kills,
resurrections, discovered areas, play time, time since the last death, survival
bonus), and the character sheet (armour, attack speed, movement speed, and the
four resistances).

It is read again every two seconds while a hero is loaded, and straight away on
each `Hero` event. The first reading that comes back whole passes the “Hero is
read through the API” scenario. Later readings only refresh the panel.

## Code structure

| Package | Job |
|---|---|
| `model` | Plain data for scenarios, statuses, log lines, and hero readings |
| `viewmodel` | Observable state and the operations that update it |
| `view` | The read-only window |
| root package | `SelfCheckMod` and its event subscriptions, `HeroProbe`, and the scenario catalogue in `Checks` |

The loader dispatches events on `sal-dispatch`. `SelfCheckModel` moves every UI
update onto the JavaFX application thread, so listeners never mutate JavaFX
state directly. Events that can arrive tens of times a second (spawns,
despawns, sectors, health changes) are tallied and shown in one UI update
however many arrived meanwhile, and only their first pass is written to the
log.

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

## The probe thread

Every call to the game goes through one daemon thread named
`self-check-probe`. After the first `Hero` event it calls
`getTypeRegistry().getTypeName(9)` and checks that the answer starts with
`TYPE_`, asks `getTypeId` for that name and expects 9 back, reads
`getUiString("UI_STATS_VICTORY")`, and lists the world's creatures and asks for
one of them again by ref. The same thread reads the hero for the panel and
sends the console answer.

Each command blocks its caller for up to two seconds while the zygote’s reader
thread receives the reply. Waiting inside an event listener would pause
`sal-dispatch`, and inside a deciding listener it would hold the game thread as
well. The probe thread keeps event delivery moving while the round trips
complete. `onUnload` stops it.
