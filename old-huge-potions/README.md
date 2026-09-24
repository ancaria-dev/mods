# Old Huge Potions

Old Huge Potions turns every small or medium potion you pick up into the
largest one of its kind.

Sacred didn't always split potions into sizes. With this mod, a small red
potion becomes a large red one the moment you pick it up. The change is in name
and look only: the potion keeps its original price and effect.

## Getting started

1. Install the loader and open the launcher, as the
   [mods README](../README.EN.md) describes.
2. Install Old Huge Potions from the Available tab and press Play.
3. Pick up a potion.

Each change appears as a line in `<Sacred Gold>/logs/mods.log`:

```
[2026-09-23 14:05:31.058] [old-huge-potions]: 16 potion types will be upgraded
[2026-09-23 14:06:02.914] [old-huge-potions]: TYPE_OBJECT_POTION_SMALL_RED → TYPE_OBJECT_POTION_LARGE_RED
```

## Which potions change

The mod keeps no list of potions. Once your hero enters the world, it asks the
game for every type whose name starts with `TYPE_OBJECT_POTION_` and pairs them
by name:

| Family | Names | Becomes |
|---|---|---|
| Coloured | `SMALL`, `MEDIUM` or `LARGE` with `RED`, `YELLOW`, `GREEN`, `BLUE` or `BLACK` | The `LARGE` version |
| Old | `HEALTH`, `MANA` or `STAMINA` with `MINOR`, `MAJOR` or `FULL` | The `FULL` version |

The game calls the purple potion `BLACK`. The known build has 24 potion types,
and 16 of them have a larger form. A new colour or kind works too, as long as
its names follow one of these patterns.

These stay as they are:

- potions already `LARGE` or `FULL`;
- names that match no pattern, or whose larger type is missing;
- potions picked up by other creatures.

## When the change happens

A potion keeps its type while it lies on the ground. The mod changes it as your
hero picks it up, before it reaches the belt. The change stays: drop the potion
and it's still the large one.

The mod doesn't touch potions when they spawn. That path runs about 3,000 times
during a world load and isn't meant for editing objects.

## What stays the same

Changing an item's type changes its name and appearance. The mod doesn't copy
the larger potion's price or modifiers, and the effect lives in the modifier
list (`Item.getModifiers()`). So a small potion turned large still costs and
heals like a small one.

## Building

Run these from the `mods` repository root:

```
gradlew :old-huge-potions:assembleSacredMod
gradlew :old-huge-potions:installSacredMod -PsacredDir="C:/Games/..."
```

The first command writes the jar to `old-huge-potions/build/sacred-mod/`. The
second copies it into `<Sacred Gold>/mods`.

## License

MIT, see [LICENSE](../LICENSE).
