# Tracer

Tracer records every event SAL dispatches in
`<Sacred Gold>/logs/logs-<time>.txt`. The filename uses the load time down to
the second. The file is opened in append mode, so loads in the same second
would share it.

```
21:44:07.311  World.LOADED
21:44:07.480  Hero                 cls=9 clsName=Daemon level=142 hp=27127 maxHp=27127 gold=104233 exp=1904772311
21:44:19.902  Pickup               ref=8814 type=1204 name=TYPE_OBJECT_RING_FIRE01 level=30 min=22 atk=0 prot=0 pct=7 player=1
21:44:19.905  Stored               ref=8814 type=1204 name=TYPE_OBJECT_RING_FIRE01 ... player=1
21:44:31.887  Damage               kind=damage damage=553 prev=19849 next=19296 max=26999   → next=19849
```

Four classes divide the work:

- `TracerMod` opens the log, registers the recorder, and installs the shutdown
  hook.
- `Recorder` contains the single `@Subscribe` method and formats each line.
- `Ring` stores pending lines. `add` performs no file I/O and never waits for
  free capacity.
- `Sink` drains the ring on a daemon thread and flushes each batch to disk.

## Why one listener covers everything

SAL dispatches to listeners registered for an event’s class or any of its
superclasses. A method that accepts the base `Event` class therefore receives
every event:

```java
@Subscribe(priority = Priority.MONITOR)
public void onAny(Event event) { ... }
```

Events without an SDK class arrive as `Unknown`. The tracer uses their wire
name and records all raw fields.

`MONITOR` runs after `FIRST`, `NORMAL`, and `LAST`. A vetoable event is recorded
with the accumulated verdict, such as `→ canceled` or `→ delta=1000`. Changes
made by a monitor listener are discarded, so the tracer cannot alter the
verdict.

## Overflow

The ring holds 8,192 pending lines. If it fills, new entries overwrite the
oldest ones. The next written batch marks the loss:

```text
  … Dropped 12 events because the tracer fell behind
```

This keeps the most recent history, which is usually the useful end of a trace
after a failure.
