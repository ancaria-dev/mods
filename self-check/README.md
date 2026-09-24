# Self Check

Self Check shows, in its own window, which parts of the loader work in your
game right now.

It listens to every event the loader delivers and tracks fifty scenarios. Each
row tells you what to do in the game to pass it, such as “Open any chest or
barrel”. ⬜ means waiting, ✅ means passed, ❌ means failed. A panel beside the
list shows your hero as the mod reads it through the API.

## Getting started

1. Install the loader and open the launcher, as the
   [mods README](../README.EN.md) describes.
2. Install Self Check from the Available tab and press Play.
3. Keep the window open while you play and follow the hints under each
   scenario.

Type `selfcheck` in the game console to get a one-line summary, such as
`Self Check: 31 of 50 scenarios passed`.

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

A scenario passes only on a real event or a real answer from the game, never
on a timer. A failed save, a type name that doesn't map back to its id, or an
empty hero reading marks its row ❌.

## What it changes

Most scenarios only prove that an event reaches the mod. The deciding ones
also send an answer back, to prove the return path works:

| Event | What Self Check answers |
|---|---|
| Damage | Leaves you 1 HP more, up to maximum health |
| Gold gained | One extra coin |
| Experience | One extra point |
| Skill, attribute, combat art, your pickup | The value the game already proposed, so nothing changes |
| Console line `selfcheck` | Vetoes the line and prints the summary |

Spending gold stays unchanged. Damage at full health sends no rewrite, since
there is nothing to soften. Pickups by other creatures are logged but don't
pass the pickup scenario.

Self Check steps aside when another mod has already decided an event. If a
number's `getValue()` no longer equals its `getInitial()`, or a pickup's
`isEdited()` is true, the scenario passes without a rewrite. That's why it runs
safely next to Old Huge Potions and All My Runes.

## The hero panel

The right-hand column shows the hero as the direct API reads it:

- class, level, HP and maximum HP, gold, experience;
- position, region and sector;
- the six attributes and the points left to spend;
- skill slots and skill points;
- learned combat arts with their base level and gear bonus;
- journal statistics: kills, resurrections, discovered areas, play time, time
  since the last death, survival bonus;
- the character sheet: armour, attack speed, movement speed and the four
  resistances.

The panel refreshes every two seconds while a hero is loaded, and straight
away on each `Hero` event. The first complete reading passes “Hero is read
through the API”.

## How it works

`SelfCheckMod` owns the event subscriptions, the console command and the probe
thread. `HeroProbe` reads the hero. `Checks` holds the scenario catalogue and
every hint. The `model`, `viewmodel` and `view` packages hold the data, the
observable state and the read-only window.

Every call to the game runs on one daemon thread, `self-check-probe`. A call
can block for up to two seconds while the reply travels back. Inside an event
listener that wait would stall `sal-dispatch`, and inside a deciding listener
the game thread too. So the listener returns the console veto at once, and the
probe thread prints the answer afterwards. `onUnload` stops the thread.

After the first `Hero` event, the probe thread asks for
`getTypeName(9)` and checks it starts with `TYPE_`, asks `getTypeId` for that
name and expects 9 back, reads `getUiString("UI_STATS_VICTORY")`, and lists the
world's creatures, then looks one up again by ref.

Events that arrive tens of times a second (spawns, despawns, sectors, health
changes) are tallied into one UI update. Only their first pass reaches the log,
which keeps the newest 500 lines.

The window uses JavaFX 21.0.9 for Windows, bundled into the jar, because the
loader starts a plain JVM without JavaFX. That's most of the jar's size. If
JavaFX can't start, the mod keeps checking events without a window. Closing the
window leaves the toolkit running, so the loader ends the JVM itself when the
game disconnects.

## Building

Run these from the `mods` repository root:

```
gradlew :self-check:assembleSacredMod
gradlew :self-check:installSacredMod -PsacredDir="C:/Games/..."
```

The first command writes `self-check/build/sacred-mod/self-check-0.200.0.jar`.
The second copies it into `<Sacred Gold>/mods`.

Two commands test the mod without the game:

```
python self-check/verify.py
java -cp self-check/build/sacred-mod/self-check-0.200.0.jar dev.ancaria.selfcheck.view.Preview
```

`verify.py` needs the built jar plus Coderpack's `api` and `zygote` jars in
Maven Local. It starts the zygote, plays the host, sends one frame per
scenario, answers the mod's commands, and checks all ten decided verdicts and
the console answer. `Preview` opens the window with no game session.

## License

MIT, see [LICENSE](../LICENSE).
