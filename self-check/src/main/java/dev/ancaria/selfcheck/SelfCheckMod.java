package dev.ancaria.selfcheck;

import dev.ancaria.coderpack.api.Game;
import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.entity.Creature;
import dev.ancaria.coderpack.api.event.Amount;
import dev.ancaria.coderpack.api.event.Attribute;
import dev.ancaria.coderpack.api.event.AttributeChanged;
import dev.ancaria.coderpack.api.event.AttributePointsChanged;
import dev.ancaria.coderpack.api.event.CombatArt;
import dev.ancaria.coderpack.api.event.CombatArtChanged;
import dev.ancaria.coderpack.api.event.Console;
import dev.ancaria.coderpack.api.event.Damage;
import dev.ancaria.coderpack.api.event.Death;
import dev.ancaria.coderpack.api.event.Despawn;
import dev.ancaria.coderpack.api.event.Discovery;
import dev.ancaria.coderpack.api.event.Drink;
import dev.ancaria.coderpack.api.event.Equip;
import dev.ancaria.coderpack.api.event.Experience;
import dev.ancaria.coderpack.api.event.ExperienceChanged;
import dev.ancaria.coderpack.api.event.Gold;
import dev.ancaria.coderpack.api.event.GoldChanged;
import dev.ancaria.coderpack.api.event.HealthChanged;
import dev.ancaria.coderpack.api.event.Hero;
import dev.ancaria.coderpack.api.event.Kill;
import dev.ancaria.coderpack.api.event.LevelUp;
import dev.ancaria.coderpack.api.event.Load;
import dev.ancaria.coderpack.api.event.Loot;
import dev.ancaria.coderpack.api.event.MaxHealthChanged;
import dev.ancaria.coderpack.api.event.MobDeath;
import dev.ancaria.coderpack.api.event.MobHit;
import dev.ancaria.coderpack.api.event.Moved;
import dev.ancaria.coderpack.api.event.NearDeath;
import dev.ancaria.coderpack.api.event.Pickup;
import dev.ancaria.coderpack.api.event.Position;
import dev.ancaria.coderpack.api.event.Quest;
import dev.ancaria.coderpack.api.event.Region;
import dev.ancaria.coderpack.api.event.Resurrection;
import dev.ancaria.coderpack.api.event.Save;
import dev.ancaria.coderpack.api.event.Sector;
import dev.ancaria.coderpack.api.event.Skill;
import dev.ancaria.coderpack.api.event.SkillChanged;
import dev.ancaria.coderpack.api.event.SkillPointsChanged;
import dev.ancaria.coderpack.api.event.Spawn;
import dev.ancaria.coderpack.api.event.Stored;
import dev.ancaria.coderpack.api.event.Trade;
import dev.ancaria.coderpack.api.event.Unknown;
import dev.ancaria.coderpack.api.event.World;
import dev.ancaria.selfcheck.model.HeroInfo;
import dev.ancaria.selfcheck.view.SelfCheckWindow;
import dev.ancaria.selfcheck.view.Ui;
import dev.ancaria.selfcheck.viewmodel.SelfCheckModel;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Subscribes to everything and shows what arrived.
 *
 * <p>Two questions get answered by running this: does each event reach a mod at
 * all, and for the events a mod may rewrite, does the rewrite reach the game.
 * The second one is why the mutating scenarios change something rather than only
 * reporting: an event that arrives and is then ignored proves half a pipe.
 *
 * <p>The changes are deliberately the smallest ones that are still real. One
 * point of damage, one gold, one point of experience. Where there is no harmless
 * change to make (a skill value, an attribute, an item being picked up) the
 * rewrite sets the value the game already had, which proves the round trip
 * without touching the save.
 */
public final class SelfCheckMod extends SacredMod {

    /** How often the hero panel asks again while a hero is loaded. */
    private static final long HERO_EVERY_SECONDS = 2;

    private SelfCheckModel model;
    private boolean probed;
    /**
     * The one thread that asks the game anything. A command is a round trip
     * through the host and can take two seconds; the bus thread must never be
     * the one waiting for it.
     */
    private ScheduledExecutorService prober;
    /** Touched only on the prober thread. */
    private boolean heroRead;
    private boolean heroFailed;
    private boolean heroMissing;

    @Override
    public void onLoad() {
        this.model = new SelfCheckModel(Checks.ALL);
        this.prober = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "self-check-probe");
            thread.setDaemon(true);
            return thread;
        });
        getContext().getRegistry().getEventRegistry().register(this);
        prober.scheduleWithFixedDelay(this::readHero, HERO_EVERY_SECONDS, HERO_EVERY_SECONDS,
                                      TimeUnit.SECONDS);

        if (!Ui.boot()) {
            getContext().log("JavaFX is unavailable in this JVM; Self Check will run without a window");
            return;
        }
        Platform.runLater(() -> new SelfCheckWindow(model).show());
        getContext().log("Window opened; waiting for events");
    }

    @Override
    public void onUnload() {
        if (prober != null) {
            prober.shutdownNow();
        }
    }

    // ---- session ---------------------------------------------------------

    /**
     * Whether another mod has already decided this number.
     *
     * <p>Every deciding check here stands down when one has. Self Check exists
     * to prove that a decision reaches the game, and a probe that overrides
     * somebody else's real one proves the opposite of what it claims.
     *
     * <p>This used to have to read a map of pending rewrites, because an
     * event's getters answered with the number off the wire and never with
     * what the mod before had asked for. Now {@code getValue()} is the fold and
     * {@code getInitial()} is the wire, so the question is just whether they
     * still agree.
     */
    private static boolean taken(Amount event) {
        return event.getValue() != event.getInitial();
    }

    @Subscribe
    public void onWorld(World event) {
        String phase = event.getPhase().name().toLowerCase();
        model.event("world " + phase);
        model.pass(Checks.WORLD, phase);
    }

    @Subscribe
    public void onHero(Hero event) {
        model.pass(Checks.HERO, event.getClassName() + ", level " + event.getLevel());
        probe();
        // Straight away rather than at the next tick: the panel should not sit
        // empty for two seconds after the hero appears.
        prober.execute(this::readHero);
    }

    /**
     * The one-off Game calls, off the bus thread. A command is a round trip through
     * the host, and asking for one from inside a listener is how a mod ends up
     * waiting for a reply that cannot arrive until the listener returns.
     */
    private void probe() {
        if (probed) {
            return;
        }
        probed = true;
        prober.execute(() -> {
            Game game = getContext().getGame();
            String type = game.getTypeRegistry().getTypeName(9);
            if (type != null && type.startsWith("TYPE_")) {
                model.pass(Checks.TYPE_NAME, "getTypeName(9) = " + type);
            } else {
                model.fail(Checks.TYPE_NAME, "getTypeName(9) returned " + type);
            }
            // Back the other way: the name just read has to find the same id.
            if (type != null) {
                int id = game.getTypeRegistry().getTypeId(type);
                if (id == 9) {
                    model.pass(Checks.TYPE_ID, "getTypeId(" + type + ") = 9");
                } else {
                    model.fail(Checks.TYPE_ID, "getTypeId(" + type + ") returned " + id);
                }
            }
            String label = game.getUiString("UI_STATS_VICTORY");
            if (label != null && !label.isBlank()) {
                model.pass(Checks.UI_STRING, "UI_STATS_VICTORY = " + label);
            } else {
                model.fail(Checks.UI_STRING, "The dictionary returned nothing");
            }
            creatures(game);
        });
    }

    /** The world's creature list, and one of them asked for again by ref. */
    private void creatures(Game game) {
        List<Creature> all = game.getWorld().getEntityRegistry().getCreatures();
        if (all.isEmpty()) {
            model.fail(Checks.CREATURES, "getCreatures() came back empty");
            return;
        }
        Creature some = all.get(0);
        Creature again = game.getWorld().getEntityRegistry().getCreature(some.getRef());
        if (again == null) {
            model.fail(Checks.CREATURES, all.size() + " creatures, but getCreature("
                                         + some.getRef() + ") found nothing");
            return;
        }
        model.pass(Checks.CREATURES, all.size() + " creatures; getCreature("
                                     + some.getRef() + ") = " + again);
    }

    /**
     * One reading of the hero for the panel, on the prober thread. It passes
     * its scenario once, on the first reading that came back whole; later
     * readings only refresh the panel, so the log is not a line every two
     * seconds.
     */
    private void readHero() {
        HeroProbe.Reading reading;
        try {
            reading = HeroProbe.read(getContext().getGame());
        } catch (RuntimeException failure) {
            // A scheduled task that throws is never run again. One bad reading
            // must not end the panel.
            model.hero(HeroInfo.waiting("Reading failed: " + failure));
            if (!heroRead && !heroFailed) {
                heroFailed = true;
                model.fail(Checks.HERO_INFO, "Reading the hero threw " + failure);
            }
            return;
        }
        if (reading == null) {
            if (!heroMissing) {
                heroMissing = true;
                model.hero(HeroInfo.waiting("No hero is loaded"));
            }
            return;
        }
        heroMissing = false;
        model.hero(reading.info());
        if (heroRead) {
            return;
        }
        if (reading.complete()) {
            heroRead = true;
            model.pass(Checks.HERO_INFO, reading.summary());
        } else if (!heroFailed) {
            heroFailed = true;
            model.fail(Checks.HERO_INFO, "Some answers came back empty; asking again");
        }
    }

    @Subscribe
    public void onSaveLoaded(Load event) {
        String what = event.isFresh() ? "a new game" : "slot " + event.getSlot();
        if (!event.isDone()) {
            model.event("Loading " + what + " from " + event.getPath());
            return;
        }
        model.pass(Checks.LOAD, "Loaded " + what + " from " + event.getPath());
    }

    @Subscribe
    public void onSave(Save event) {
        String where = "slot " + event.getSlot() + " “" + event.getName() + "”, "
                       + event.getPath();
        if (event.isOk()) {
            model.pass(Checks.SAVE, "Saved " + where);
        } else {
            model.fail(Checks.SAVE, "The game reported the save to " + where + " as failed");
        }
    }

    @Subscribe
    public void onQuest(Quest event) {
        if (event.isStarted()) {
            model.pass(Checks.QUEST_START, "Quest " + event.getNumber());
        } else {
            model.pass(Checks.QUEST_END, "Quest " + event.getNumber()
                                         + ", end flag " + event.getEndFlag());
        }
    }

    /**
     * Claims one console line as this mod's own and answers it in the console.
     *
     * <p>The veto is the claim: the game never sees the line and prints no
     * error. The answer is a command, so it goes out from the prober thread;
     * this is a deciding listener and the game thread is waiting on it.
     */
    @Subscribe
    public Console.Mutation onConsole(Console event) {
        String line = event.getText().strip();
        model.event("Console: " + line);
        if (!line.equalsIgnoreCase(Checks.COMMAND)) {
            return Console.Mutation.none();
        }
        prober.execute(() -> {
            String answer = "Self Check: " + model.passedCount() + " of " + model.total()
                            + " scenarios passed";
            getContext().getGame().getConsole().print(answer);
            model.pass(Checks.CONSOLE, "Claimed “" + line + "” and answered: " + answer);
        });
        return Console.Mutation.veto();
    }

    // ---- world -----------------------------------------------------------

    @Subscribe
    public void onRegion(Region event) {
        if (event.isEntered()) {
            model.pass(Checks.REGION, "Entered region " + event.getId() + " from " + event.getFrom());
        } else {
            model.event("Left region " + event.getId());
        }
    }

    @Subscribe
    public void onSector(Sector event) {
        model.tally(Checks.SECTOR, "Sector " + event.getX() + ", " + event.getY());
    }

    @Subscribe
    public void onDiscovery(Discovery event) {
        model.pass(Checks.DISCOVERY, event.getAreas() + " areas discovered");
    }

    @Subscribe
    public void onSpawn(Spawn event) {
        Creature creature = event.getCreature();
        model.tally(Checks.SPAWN, creature + " at " + creature.getX() + ", " + creature.getY());
    }

    @Subscribe
    public void onDespawn(Despawn event) {
        model.tally(Checks.DESPAWN, event.getCreature().toString());
    }

    @Subscribe
    public void onPosition(Position event) {
        model.pass(Checks.POSITION, event.getHudX() + ", " + event.getHudY());
    }

    // ---- health ----------------------------------------------------------

    @Subscribe
    public Damage.Mutation onDamage(Damage event) {
        long before = event.getValue();
        model.event(event.getKind() + " " + event.getHp() + " → " + before
                    + " (" + event.getDamage() + " damage)");
        if (!"damage".equals(event.getKind())) {
            return Damage.Mutation.none();
        }
        long softened = Math.min(before + 1, event.getMaxHp());
        if (softened == before) {
            model.pass(Checks.HEALTH, "Nothing to soften at " + before + " HP");
            return Damage.Mutation.none();
        }
        if (taken(event)) {
            model.pass(Checks.HEALTH, "Another mod is already deciding this hit");
            return Damage.Mutation.none();
        }
        model.change("Damage softened: " + before + " → " + softened);
        model.pass(Checks.HEALTH, "Kept 1 HP back (" + before + " → " + softened + ")");
        return Damage.Mutation.change(softened);
    }

    @Subscribe
    public void onHealthChanged(HealthChanged event) {
        // Regeneration moves this every few frames, so it is tallied.
        model.tally(Checks.HEALTH_CHANGED, event.getKind() + " " + event.getPrevious() + " → "
                                           + event.getHp() + " of " + event.getMaxHp());
    }

    @Subscribe
    public void onMaxHealth(MaxHealthChanged event) {
        model.pass(Checks.MAX_HEALTH, event.getPrevious() + " → " + event.getMaxHp());
    }

    @Subscribe
    public void onResurrection(Resurrection event) {
        model.pass(Checks.RESURRECTION, "Resurrection number " + event.getCount());
    }

    @Subscribe
    public void onNearDeath(NearDeath event) {
        model.pass(Checks.NEAR_DEATH, event.getHp() + " HP left, " + event.getPercent() + "%");
    }

    @Subscribe
    public void onDeath(Death event) {
        model.pass(Checks.DEATH, "Killed by a " + event.getBlow() + "-point blow");
    }

    @Subscribe
    public void onMobHit(MobHit event) {
        // MobDeath extends MobHit, so this fires for both. The death handler
        // below settles its own scenario.
        model.pass(Checks.MOB_HIT, event.getTypeName() + " " + event.getHp() + " → " + event.getNext());
    }

    @Subscribe
    public void onMobDeath(MobDeath event) {
        model.pass(Checks.MOB_DEATH, event.getTypeName() + " at level " + event.getLevel());
    }

    @Subscribe
    public void onKill(Kill event) {
        model.pass(Checks.KILL, event.getTypeName() + ", kill number " + event.getTotal());
    }

    @Subscribe
    public void onLoot(Loot event) {
        List<Loot.Drop> items = event.getItems();
        String what = items.size() + (items.size() == 1 ? " item" : " items") + " from "
                      + (event.getSourceTypeName() == null ? "#" + event.getSourceRef()
                                                           : event.getSourceTypeName());
        if (!items.isEmpty()) {
            what += ": " + items.get(0).getTypeName() + (items.size() > 1 ? " and more" : "");
        }
        model.pass(event.isChest() ? Checks.LOOT_CHEST : Checks.LOOT_DROP, what);
    }

    // ---- progression -----------------------------------------------------

    @Subscribe
    public Gold.Mutation onGold(Gold event) {
        long delta = event.getValue();
        model.event((event.isSpending() ? "Spent " : "Found ") + Math.abs(delta)
                    + " gold, had " + event.getCurrent());
        if (event.isSpending()) {
            return Gold.Mutation.none();
        }
        if (taken(event)) {
            model.pass(Checks.GOLD, "Another mod is already deciding this gain");
            return Gold.Mutation.none();
        }
        model.change("Gold delta " + delta + " → " + (delta + 1));
        model.pass(Checks.GOLD, "Asked for one more than the game offered");
        return Gold.Mutation.change(delta + 1);
    }

    @Subscribe
    public Experience.Mutation onExperience(Experience event) {
        long total = event.getValue();
        if (taken(event)) {
            model.pass(Checks.EXPERIENCE, "Another mod is already deciding this award");
            return Experience.Mutation.none();
        }
        model.change("Experience total " + total + " → " + (total + 1));
        model.pass(Checks.EXPERIENCE, "+" + event.getGain() + " XP; asked for one more");
        return Experience.Mutation.change(total + 1);
    }

    @Subscribe
    public Skill.Mutation onSkill(Skill event) {
        if (taken(event)) {
            model.pass(Checks.SKILL, "Slot " + event.getSlot() + "; another mod is setting it");
            return Skill.Mutation.none();
        }
        // Same value on purpose. A skill point is not ours to spend, and a
        // mutation back to the number that arrived says nothing on the wire.
        model.pass(Checks.SKILL, "Slot " + event.getSlot() + " → " + event.getValue()
                                 + "; answered with the same value");
        return Skill.Mutation.change(event.getValue());
    }

    @Subscribe
    public Attribute.Mutation onAttribute(Attribute event) {
        if (taken(event)) {
            model.pass(Checks.ATTRIBUTE, event.getName() + "; another mod is setting it");
            return Attribute.Mutation.none();
        }
        model.pass(Checks.ATTRIBUTE, event.getName() + " → " + event.getValue()
                                     + "; answered with the same value");
        return Attribute.Mutation.change(event.getValue());
    }

    @Subscribe
    public void onGoldChanged(GoldChanged event) {
        model.pass(Checks.GOLD_CHANGED, "Now " + event.getGold() + " ("
                                        + (event.getDelta() >= 0 ? "+" : "") + event.getDelta() + ")");
    }

    @Subscribe
    public void onExperienceChanged(ExperienceChanged event) {
        model.pass(Checks.EXPERIENCE_CHANGED, event.getPrevious() + " → " + event.getExp());
    }

    @Subscribe
    public void onSkillChanged(SkillChanged event) {
        model.pass(Checks.SKILL_CHANGED, "Slot " + event.getSlot() + " is now " + event.getLevel());
    }

    @Subscribe
    public void onSkillPoints(SkillPointsChanged event) {
        model.pass(Checks.SKILL_POINTS, event.getPrevious() + " → " + event.getPoints()
                                        + (event.isGranted() ? ", granted" : ", spent"));
    }

    @Subscribe
    public void onAttributeChanged(AttributeChanged event) {
        model.pass(Checks.ATTRIBUTE_CHANGED, event.getName() + " " + event.getPrevious()
                                             + " → " + event.getValue());
    }

    @Subscribe
    public void onAttributePoints(AttributePointsChanged event) {
        model.pass(Checks.ATTRIBUTE_POINTS, event.getPrevious() + " → " + event.getPoints()
                                            + (event.isGranted() ? ", granted" : ", spent"));
    }

    @Subscribe
    public CombatArt.Mutation onCombatArt(CombatArt event) {
        String art = "Art #" + event.getArtId() + "/" + event.getAspect();
        if (taken(event)) {
            model.pass(Checks.COMBAT_ART, art + "; another mod is setting it");
            return CombatArt.Mutation.none();
        }
        // The same value, as with skills: a rune is the player's to spend, and
        // this only proves the verdict travels.
        model.pass(Checks.COMBAT_ART, art + " " + event.getPrevious() + " → "
                                      + event.getValue() + "; answered with the same value");
        return CombatArt.Mutation.change(event.getValue());
    }

    @Subscribe
    public void onCombatArtChanged(CombatArtChanged event) {
        model.pass(Checks.COMBAT_ART_CHANGED, "Art #" + event.getArtId() + "/" + event.getAspect()
                                              + " " + event.getPrevious() + " → "
                                              + event.getLevel());
    }

    @Subscribe
    public void onLevel(LevelUp event) {
        model.pass(Checks.LEVEL, event.getPrevious() + " → " + event.getLevel());
    }

    // ---- items -----------------------------------------------------------

    @Subscribe
    public Pickup.Mutation onPickup(Pickup event) {
        String name = event.getItem().getTypeName();
        model.event("Picking up " + name);
        if (!event.isPlayer()) {
            return Pickup.Mutation.none();
        }
        // The one that used to break other mods. old-huge-potions turns a small
        // potion into a large one and all-my-runes copies a rune onto another,
        // and this probe ran last and put the original back over the top of
        // them. It asks now, which it could not do before: isEdited() is the fold
        // answering, not a map of pending writes.
        if (event.isEdited()) {
            model.pass(Checks.PICKUP, name + "; another mod is editing it, left alone");
            return Pickup.Mutation.none();
        }
        // Retyping to the type it already has: the verdict travels the whole way
        // and the item is exactly what it was.
        model.pass(Checks.PICKUP, name + "; answered without changing it");
        return Pickup.Mutation.retype(event.getItem().getTypeId());
    }

    @Subscribe
    public void onStored(Stored event) {
        model.pass(Checks.STORED, event.getItem().getTypeName());
    }

    @Subscribe
    public void onEquip(Equip event) {
        // An unequip carries no item, only the slot it left.
        model.pass(Checks.EQUIP, event.isOff()
                ? "Unequipped slot " + event.getSlot()
                : "Equipped " + event.getItem().getTypeName() + " in slot " + event.getSlot());
    }

    @Subscribe
    public void onDrink(Drink event) {
        if (!event.isPlayer()) {
            model.event("A creature drank " + event.getTypeName());
            return;
        }
        model.pass(Checks.DRINK, event.getTypeName());
    }

    @Subscribe
    public void onTrade(Trade event) {
        if (event.isBought()) {
            model.pass(Checks.BUY, event.getTypeName() + " for " + event.getPrice() + " gold");
        } else {
            model.pass(Checks.SELL, event.getTypeName() + " for " + event.getPrice() + " gold, "
                                    + (event.isQuick() ? "by Shift+click" : "by dragging"));
        }
    }

    @Subscribe
    public void onMoved(Moved event) {
        model.pass(Checks.MOVED, event.getFrom() + " → " + event.getTo());
    }

    // ---- everything else -------------------------------------------------

    @Subscribe
    public void onUnknown(Unknown event) {
        model.pass(Checks.UNKNOWN, event.getName());
    }
}
