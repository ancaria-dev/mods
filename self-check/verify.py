"""Runs self-check against a scripted session, with no game and no host.

Same trick as tests/replay.py: this script IS the host, so it answers commands
as well as sending events. What it adds is a pause before shutdown, because the
thing being checked is a window and a window is worth looking at.

    python verify.py            run, assert, quit
    python verify.py --hold 20  keep the window up for twenty seconds
"""
import pathlib
import queue
import subprocess
import sys
import threading
import time

HERE = pathlib.Path(__file__).resolve().parent
M2 = pathlib.Path.home() / ".m2" / "repository" / "dev" / "ancaria" / "coderpack"
CODERPACK = "0.200.0"
JAR = HERE / "build" / "sacred-mod" / "self-check-0.200.0.jar"
MODS = HERE / "build" / "verify-mods"

# The host's half of the commands the mod makes: the probes after the first
# hero, the readings behind the hero panel, asked again every two seconds, and
# the console answer.
ANSWERS = {
    "type.name": "ok=1 name=TYPE_NPC_DAEMONIN",
    "type.find": "ok=1 id=9",
    "ui.string": "ok=1 text=Opponents%20Defeated",
    "world.creatures": "creatures=4101:50:5:40:40:222900:137600:0;"
                       "4102:9:34:980:1200:222850:137608:1 "
                       "names=50%3DTYPE_NPC_GHUL01,9%3DTYPE_NPC_DAEMONIN",
    "world.creature": "ok=1 ref=4101 type=50 name=TYPE_NPC_GHUL01 level=5 "
                      "hp=40 maxHp=40 x=222900 y=137600 player=0",
    "console.print": "ok=1",
    "player.attributes": "values=141,90,120,30,40,25 points=4",
    "player.skills": "levels=61,40,0,22,0,0,0,0 points=2",
    "player.arts": "arts=0:12:0:15:3;1:14:1:8:0;2:20:0:0:0",
    "player.stats": "kills=4210 resurrections=3 areas=188 graph=2 "
                    "playMillis=51000000 sinceDeath=600000 survival=1.25",
    "player.sheet": "armor=62 attackSpeed=140 move=120 resist=30,45,20,50",
    "world.state": "region=12 sx=40 sy=77",
}

# One frame per scenario, in the order a session would produce them.
SCRIPT = [
    ("EVT 0 session.load_start slot=3 fresh=0 path=SAVE/GAME03.PAK", None),
    ("EVT 0 session.world_loaded", None),
    ("EVT 0 session.load_done slot=3 fresh=0 path=SAVE/GAME03.PAK", None),
    ("EVT 0 hero.captured cls=9 clsName=Daemon level=34 hp=980 maxHp=1200 "
     "gold=51230 exp=9400000", None),
    ("EVT 0 pos.changed x=222850 y=137608 uiX=4152 uiY=2564 src=hero", None),
    ("EVT 0 world.sector_enter x=40 y=77", None),
    ("EVT 0 world.region_exit id=11", None),
    ("EVT 0 world.region_enter id=12 from=11", None),
    ("EVT 0 journal.discovery areas=189", None),
    ("EVT 0 entity.spawn ref=4101 type=50 name=TYPE_NPC_GHUL01 level=5 hp=40 "
     "maxHp=40 x=222900 y=137600 player=0", None),

    # Decidable, and the one place self-check changes a number that matters.
    ("ASK 1 health.damage kind=damage damage=120 prev=980 next=860 max=1200",
     "END 1 set.next=861"),
    ("EVT 0 health.changed kind=damage prev=980 next=861 max=1200 damage=119",
     None),
    ("EVT 0 health.max_changed prev=1200 next=1260", None),
    ("EVT 0 health.near_death next=150 prev=400 max=1200 percent=12", None),
    ("EVT 0 health.death prev=150 max=1200 blow=150", None),
    ("EVT 0 journal.resurrection count=4", None),
    ("EVT 0 entity.damage type=50 name=TYPE_NPC_GHUL01 level=5 prev=40 "
     "next=12 max=40 damage=28 kind=damage", None),
    ("EVT 0 entity.death type=50 name=TYPE_NPC_GHUL01 level=5 prev=12 next=0 "
     "max=40 damage=12 kind=lethal", None),
    ("EVT 0 journal.kill total=4211 type=50 name=TYPE_NPC_GHUL01", None),
    ("EVT 0 loot.drop source=4101 type=50 name=TYPE_NPC_GHUL01 chest=0 "
     "items=5120:5171:TYPE_OBJECT_POTION_SMALL_RED", None),
    ("EVT 0 entity.despawn ref=4101 type=50 name=TYPE_NPC_GHUL01 level=5 hp=0 "
     "maxHp=40 x=222900 y=137600 player=0", None),
    ("EVT 0 loot.drop source=7730 type=880 name=TYPE_OBJECT_CHEST01 chest=1 "
     "items=5121:1204:TYPE_OBJECT_RING_FIRE01;5122:5171:TYPE_OBJECT_POTION_SMALL_RED",
     None),

    ("ASK 2 gold.delta delta=250 current=51230 dir=gain", "END 2 set.delta=251"),
    ("EVT 0 gold.changed next=51481 delta=251", None),
    ("ASK 3 gold.delta delta=-90 current=51480 dir=spend", "END 3 ok=1"),
    ("EVT 0 trade.buy quick=0 price=90 ref=5130 type=5171 "
     "name=TYPE_OBJECT_POTION_SMALL_RED", None),
    ("EVT 0 trade.sell quick=1 price=40 ref=5121 type=1204 "
     "name=TYPE_OBJECT_RING_FIRE01", None),
    ("ASK 4 exp.gain gain=1180 prev=9400000 next=9401180",
     "END 4 set.next=9401181"),
    ("EVT 0 exp.changed prev=9400000 next=9401181", None),
    # Answered with the value the game proposed, which folds to no change,
    # so the bus sends a plain ok rather than a set of the same number.
    ("ASK 5 skill.change slot=2 delta=1 prev=61 next=62", "END 5 ok=1"),
    ("EVT 0 skill.changed slot=2 next=62", None),
    ("EVT 0 skillpoints.changed prev=3 next=2", None),
    ("ASK 6 attr.spend attr=0 name=Strength prev=141 next=142",
     "END 6 ok=1"),
    ("EVT 0 attr.changed attr=0 name=Strength prev=141 next=142", None),
    ("EVT 0 attrpoints.changed prev=4 next=3", None),
    ("ASK 8 art.raise index=0 id=12 aspect=0 prev=15 next=16 step=1",
     "END 8 ok=1"),
    ("EVT 0 art.changed index=0 id=12 aspect=0 prev=15 next=16", None),
    ("ASK 7 item.pickup ref=1083 type=5171 name=TYPE_OBJECT_POTION_SMALL_RED "
     "level=0 min=0 atk=0 prot=0 pct=20 player=1", "END 7 set.type=5171"),

    ("EVT 0 item.stored ref=1083 type=5171 "
     "name=TYPE_OBJECT_POTION_SMALL_RED player=1", None),
    ("EVT 0 item.equip slot=10 ref=8814 type=1204 "
     "name=TYPE_OBJECT_RING_FIRE01 level=30 off=0 player=1", None),
    ("EVT 0 item.moved from=4 to=17", None),
    ("EVT 0 item.drink ref=1083 type=5171 name=TYPE_OBJECT_POTION_SMALL_RED "
     "player=1", None),
    ("EVT 0 level.changed prev=34 next=35", None),
    ("EVT 0 quest.start number=112", None),
    ("EVT 0 quest.end number=112 flag=1", None),
    ("EVT 0 session.saved slot=3 name=Before%20the%20bridge "
     "path=SAVE/GAME03.PAK ok=1", None),
    # Not the mod's line: the game gets it back. Then the mod's own, claimed.
    ("ASK 9 console.line text=help", "END 9 ok=1"),
    ("ASK 10 console.line text=selfcheck", "END 10 cancel=1"),
    # No SDK class covers this one, and it still has to reach the mod.
    ("EVT 0 weather.rain_start intensity=3", None),
]


def stage():
    """A mods directory holding nothing but the jar under test."""
    MODS.mkdir(parents=True, exist_ok=True)
    for stale in MODS.glob("*.jar"):
        stale.unlink()
    if not JAR.is_file():
        raise SystemExit(f"{JAR} is missing. Run gradlew assembleSacredMod")
    (MODS / JAR.name).write_bytes(JAR.read_bytes())


def main():
    hold = 6
    if "--hold" in sys.argv:
        hold = int(sys.argv[sys.argv.index("--hold") + 1])
    stage()

    # From the local Maven repository, which is where `publishToMavenLocal`
    # in the coderpack checkout puts them. No sibling directory to get wrong.
    jars = [M2 / part / CODERPACK / f"{part}-{CODERPACK}.jar"
            for part in ("api", "zygote")]
    missing = [j for j in jars if not j.is_file()]
    if missing:
        raise SystemExit("run `gradlew publishToMavenLocal` in coderpack first; "
                         "missing: " + ", ".join(str(j) for j in missing))
    classpath = ";".join(str(j) for j in jars)
    zygote = subprocess.Popen(
        ["java", "-cp", classpath, "dev.ancaria.coderpack.zygote.Main",
         "--mods", str(MODS), "--enable", "self-check"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=1)

    verdicts = queue.Queue()
    noise = []
    printed = []

    def pump():
        for line in zygote.stdout:
            line = line.strip()
            if not line:
                continue
            parts = line.split(" ")
            if parts[0] == "CMD":
                if parts[2] == "console.print":
                    printed.append(line)
                reply = ANSWERS.get(parts[2], "err=unknown%20command")
                zygote.stdin.write(f"RES {parts[1]} {reply}\n")
                zygote.stdin.flush()
            elif parts[0] == "END":
                verdicts.put(line)
            else:
                noise.append(line)

    threading.Thread(target=pump, daemon=True).start()

    for line, _ in SCRIPT:
        zygote.stdin.write(line + "\n")
        zygote.stdin.flush()
        # Slowly enough that the window has something to animate, and slowly
        # enough that a screenshot catches a full list rather than an empty one.
        time.sleep(0.05)

    asks = [line.split(" ")[1] for line, _ in SCRIPT if line.startswith("ASK")]
    got = {}
    for _ in asks:
        try:
            reply = verdicts.get(timeout=15)
        except queue.Empty:
            break
        got[reply.split(" ")[1]] = reply

    print(f"holding the window for {hold}s")
    time.sleep(hold)
    zygote.stdin.write("BYE\n")
    zygote.stdin.flush()
    try:
        zygote.wait(timeout=15)
    except subprocess.TimeoutExpired:
        zygote.kill()

    failures = 0
    for line, want in SCRIPT:
        if not line.startswith("ASK"):
            continue
        seq = line.split(" ")[1]
        have = got.get(seq, "(nothing)")
        ok = have == want
        failures += 0 if ok else 1
        print(f"  {'ok  ' if ok else 'FAIL'}  {have:<28}"
              f"{'' if ok else ' expected ' + want}")

    # The claimed line has to be answered in the console, from the probe
    # thread, after the verdict went back.
    answered = any("Self%20Check:" in line for line in printed)
    failures += 0 if answered else 1
    print(f"  {'ok  ' if answered else 'FAIL'}  console answer "
          f"{printed[0].split(' ', 3)[3] if printed else '(nothing printed)'}")

    for line in noise:
        print("  " + line)
    broken = [line for line in noise
              if "Exception" in line or "Error" in line or "no JavaFX" in line]
    failures += len(broken)

    print(f"\n{len(asks) - failures}/{len(asks)} verdicts correct")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
