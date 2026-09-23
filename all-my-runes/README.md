# All My Runes

A rune for another class becomes a copy of one of your own runes when you pick
it up. The mod copies the type, price, level, minimum level, and modifiers from
a rune it has already seen, so the replacement upgrades the copied rune’s
combat art instead of merely taking its name and appearance.

Pick up one rune for your class first. The mod remembers one rune of each type
for the current session, then chooses at random from those remembered runes
when it replaces a foreign one.

It says what it did in `<Sacred Gold>/logs/mods.log`:

```
[2026-09-23 14:05:31.042] [all-my-runes]: 79 runes are known
[2026-09-23 14:07:12.610] [all-my-runes]: Leaving the DAEMON rune unchanged until you pick up one of your own to copy
[2026-09-23 14:09:48.217] [all-my-runes]: Changed TYPE_SMOVE_UPGRADE_DEM_ATTACKE from a DAEMON rune to TYPE_SMOVE_UPGRADE_HARDHIT_SERA
```

The earlier type-only version changed a rune’s name and appearance without
changing the combat art it upgraded. The read-only probe used to trace that
problem is `artifacts/probes/item_probe.py` in the research repository.

## Rune table

On first use, the mod copies its bundled table to
`<Sacred Gold>/mods/all-my-runes.txt`. Later runs read that file, so you can add
missing owners without rebuilding the mod. Restart the loader after editing it
because the table is loaded once per loader run.

```
SERAPHIM    TYPE_SMOVE_UPGRADE_HARDHIT_SERA
DAEMON      TYPE_SMOVE_UPGRADE_DEM_ATTACKE
?           TYPE_SPELL_UPGRADE_LIGHTNINGSTRIKE
```

The bundled table assigns 79 of the game’s 140 rune type names. These are the
types whose names identify a class through a `DWR_`, `DEM_`, `DE_`, `VL_`, or
`ARROW_` prefix, or a `_SERA`, `_GLAD`, `_DELF`, `_VAMP`, or `_WELF` suffix.

| Class | Assigned runes |
|---|---|
| Dwarf | 16 |
| Dark Elf | 16 |
| Vampiress | 16 |
| Daemon | 13 |
| Wood Elf | 9 |
| Seraphim | 5 |
| Gladiator | 4 |
| *unassigned* | **61** |

The 61 unassigned lines contain 17 generic `TYPE_SMOVE_UPGRADE_*` names and 44
`TYPE_SPELL_UPGRADE_*` names. Their names do not identify a class. They appear
at the bottom of the file with `?` in place of the owner. Replace `?` with one
of the class names listed in the file, then restart the loader. Invalid class
names, malformed lines, comments, and rune types absent from the running game
are ignored.

## Limits

A rune missing from the table is left alone. The mod also leaves a listed
foreign rune alone until it has seen one of your own, and it does not change
runes picked up by creatures.

The Vampiress and her vampire form share the same rune owner. Changing form
does not make the mod treat her runes as foreign.

The replacement edits the item itself. If you drop that rune and pick it up
again, it remains the copied rune.
