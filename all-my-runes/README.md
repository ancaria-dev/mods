# All My Runes

All My Runes turns every rune for another class into a rune for your own class
when you pick it up.

The mod needs a rune of your class to copy, so pick one up first. From then on,
each foreign rune becomes a copy of a random rune of yours seen this session.
The copy takes over the type, price, level, minimum level and modifiers, so
reading it raises your combat art, not just a look-alike.

## Getting started

1. Install the loader and open the launcher, as the
   [mods README](../README.EN.md) describes.
2. Install All My Runes from the Available tab and press Play.
3. Pick up one rune of your own class, then collect runes as usual.

The mod reports what it does in `<Sacred Gold>/logs/mods.log`:

```
[2026-09-23 14:05:31.042] [all-my-runes]: 79 runes are known
[2026-09-23 14:07:12.610] [all-my-runes]: Leaving the DAEMON rune unchanged until you pick up one of your own to copy
[2026-09-23 14:09:48.217] [all-my-runes]: Changed TYPE_SMOVE_UPGRADE_DEM_ATTACKE from a DAEMON rune to TYPE_SMOVE_UPGRADE_HARDHIT_SERA
```

## What stays the same

- A rune missing from the rune table stays as it is.
- A foreign rune stays as it is until the mod has seen one of yours.
- Runes picked up by other creatures don't change.
- The Vampiress and her vampire form count as one class, so changing form
  doesn't make her own runes foreign.

The change is made on the item itself. Drop a converted rune and pick it up
again, and it stays the copy.

## Configuration

The mod decides which class owns a rune from a table. On first use it copies
its bundled table to `<Sacred Gold>/mods/all-my-runes.txt`, and after that it
reads your copy. Each line names a class and a rune type:

```
SERAPHIM    TYPE_SMOVE_UPGRADE_HARDHIT_SERA
DAEMON      TYPE_SMOVE_UPGRADE_DEM_ATTACKE
?           TYPE_SPELL_UPGRADE_LIGHTNINGSTRIKE
```

The bundled table assigns 79 of the game's 140 rune types, the ones whose
names give away their class: a `DWR_`, `DEM_`, `DE_`, `VL_` or `ARROW_`
prefix, or a `_SERA`, `_GLAD`, `_DELF`, `_VAMP` or `_WELF` suffix.

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

The 61 unassigned runes (17 generic `TYPE_SMOVE_UPGRADE_*` and 44
`TYPE_SPELL_UPGRADE_*`) sit at the bottom of the file with `?` as the owner. To
assign one, replace `?` with a class name listed in the file, then restart the
loader: the mod reads the table once per run. Unknown class names, malformed
lines, comments and rune types the game doesn't have are ignored.

## Building

Run these from the `mods` repository root:

```
gradlew :all-my-runes:assembleSacredMod
gradlew :all-my-runes:installSacredMod -PsacredDir="C:/Games/..."
```

The first command writes the jar to `all-my-runes/build/sacred-mod/`. The
second copies it into `<Sacred Gold>/mods`.

An earlier version changed only a rune's type. That changed its name and look
but not the combat art it raised, because the combat art lives in the modifier
list. The read-only probe that traced this is
`artifacts/probes/item_probe.py` in the
[research](https://github.com/ancaria-dev/research) repository.

## License

MIT, see [LICENSE](../LICENSE).
