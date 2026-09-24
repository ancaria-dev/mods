# Tracer

Tracer writes every event the loader receives to a log file, one file per run.

Nothing changes in the game. You get a plain-text trace you can read after a
session or hand to someone chasing a bug:

```
21:44:07.311  World.LOADED
21:44:07.480  Hero                 cls=9 clsName=Daemon level=142 hp=27127 maxHp=27127 gold=104233 exp=1904772311
21:44:19.902  Pickup               ref=8814 type=1204 name=TYPE_OBJECT_RING_FIRE01 level=30 min=22 atk=0 prot=0 pct=7 player=1
21:44:19.905  Stored               ref=8814 type=1204 name=TYPE_OBJECT_RING_FIRE01 ... player=1
21:44:31.887  Damage               kind=damage damage=553 prev=19849 next=19296 max=26999   → 19296 → 19849
```

## Getting started

1. Install the loader and open the launcher, as the
   [mods README](../README.EN.md) describes.
2. Install Tracer from the Available tab and press Play.
3. After the session, open `<Sacred Gold>/logs/logs-<yyyyMMdd-HHmmss>.txt`.

The file name uses the time the mod loaded, down to the second. `mods.log`
gets one line saying where the trace goes.

## What a line shows

Each line holds the time, the event name and its fields. Events the API has no
class for still appear, under their wire name with every raw field.

When a mod can decide an event, Tracer records the final verdict after every
other mod has had its say: `→ vetoed`, or the number the game proposed and the
number it gets back, such as `→ 250 → 1000`. Tracer only watches. It can't
change a verdict.

If Tracer falls behind, it keeps the newest 8,192 lines, drops the oldest, and
marks the gap:

```text
  … Dropped 12 events because the tracer fell behind
```

The newest history is usually the useful end of a trace after a failure.

## How it works

One listener covers every event. The loader delivers an event to listeners of
its class and of every superclass, so a method that takes the base `Event`
receives them all:

```java
@Subscribe(priority = Priority.MONITOR)
public void onAny(Event event) { ... }
```

`MONITOR` runs after `FIRST`, `NORMAL` and `LAST`, which is why the line shows
the accumulated verdict.

Four classes split the work:

- `TracerMod` opens the file and registers the recorder. Its `onUnload` closes
  the file, so the last batch reaches the disk when the game exits, the host's
  pipe closes, or the mod is unregistered.
- `Recorder` holds the listener and formats each line.
- `Ring` buffers pending lines. Adding a line never touches the disk and never
  waits.
- `Sink` drains the ring on a daemon thread and writes batches to disk.

The game thread may be waiting on a decidable event, so the listener does no
file I/O. The trace lives in its own file rather than in `mods.log`: thousands
of event lines would bury every other mod's output.

## Building

Run these from the `mods` repository root:

```
gradlew :tracer:assembleSacredMod
gradlew :tracer:installSacredMod -PsacredDir="C:/Games/..."
```

The first command writes the jar to `tracer/build/sacred-mod/`. The second
copies it into `<Sacred Gold>/mods`.

## License

MIT, see [LICENSE](../LICENSE).
