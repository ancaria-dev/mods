# Old Huge Potions

Sacred did not always split potions into sizes. When the hero picks up a
supported small, medium, minor, or major potion, this mod changes its type to
the largest version of the same kind.

Each change is a line in `<Sacred Gold>/logs/mods.log`:

```
[2026-09-23 14:05:31.058] [old-huge-potions]: 16 potion types will be upgraded
[2026-09-23 14:06:02.914] [old-huge-potions]: TYPE_OBJECT_POTION_SMALL_RED → TYPE_OBJECT_POTION_LARGE_RED
```

## How potion types are found

The mod has no hard-coded potion IDs or fixed list of potion names. After the
first hero event, it asks the running game for every type whose name starts
with `TYPE_OBJECT_POTION_`. It then builds upgrade pairs from these two naming
patterns:

| Family | Names | Upgrade |
|---|---|---|
| Coloured | `SMALL`, `MEDIUM`, or `LARGE` with `RED`, `YELLOW`, `GREEN`, `BLUE`, or `BLACK` | Replace the size with `LARGE` |
| Old | `HEALTH`, `MANA`, or `STAMINA` with `MINOR`, `MAJOR`, or `FULL` | Replace the suffix with `FULL` |

The game data uses `BLACK`, not `PURPLE`. The known build contains 24 potion
types, of which 16 have a larger form.

A new colour or kind also works when both names follow one of these patterns.
If a name does not match a pattern, or the matching `LARGE` or `FULL` type is
missing, the mod leaves that potion alone. Potions that are already `LARGE` or
`FULL` are unchanged.

## When the change happens

The potion keeps its original type while it lies on the ground. The mod changes
the object during the hero’s pickup, before it reaches the belt. Pickups by
other creatures are ignored.

The change stays on the object after the event. If the hero drops the potion,
it still has the upgraded type. The mod does not change potions when they
spawn because that path runs about 3,000 times during a world load and is not
used for object edits.

## What the mod does not change

Changing an item’s type changes its name and appearance only. The mod does not
copy the larger potion’s price or modifiers, so the potion keeps the original
price and effect. In other words, the current version is a visual and naming
change.

An item’s effect lives in its modifier list, available through
`Item.getModifiers()`. The mod does not rewrite that list.
