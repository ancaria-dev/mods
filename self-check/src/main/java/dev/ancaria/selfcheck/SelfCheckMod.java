package dev.ancaria.selfcheck;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.Game;
import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Attribute;
import dev.ancaria.coderpack.api.event.Damage;
import dev.ancaria.coderpack.api.event.Death;
import dev.ancaria.coderpack.api.event.Equip;
import dev.ancaria.coderpack.api.event.Experience;
import dev.ancaria.coderpack.api.event.Gold;
import dev.ancaria.coderpack.api.event.Hero;
import dev.ancaria.coderpack.api.event.LevelUp;
import dev.ancaria.coderpack.api.event.MobDeath;
import dev.ancaria.coderpack.api.event.MobHit;
import dev.ancaria.coderpack.api.event.Moved;
import dev.ancaria.coderpack.api.event.NearDeath;
import dev.ancaria.coderpack.api.event.Pickup;
import dev.ancaria.coderpack.api.event.Position;
import dev.ancaria.coderpack.api.event.Skill;
import dev.ancaria.coderpack.api.event.Stored;
import dev.ancaria.coderpack.api.event.Unknown;
import dev.ancaria.coderpack.api.event.World;
import dev.ancaria.selfcheck.view.SelfCheckWindow;
import dev.ancaria.selfcheck.view.Ui;
import dev.ancaria.selfcheck.viewmodel.SelfCheckModel;
import javafx.application.Platform;

/**
 * Subscribes to everything and shows what arrived.
 *
 * <p>Two questions get answered by running this: does each event reach a mod at
 * all, and for the events a mod may rewrite, does the rewrite reach the game.
 * The second one is why the mutating scenarios change something rather than only
 * reporting -- an event that arrives and is then ignored proves half a pipe.
 *
 * <p>The changes are deliberately the smallest ones that are still real. One
 * point of damage, one gold, one point of experience. Where there is no harmless
 * change to make (a skill value, an attribute, an item being picked up) the
 * rewrite sets the value the game already had, which proves the round trip
 * without touching the save.
 */
public final class SelfCheckMod implements SacredMod {

    private Context context;
    private SelfCheckModel model;
    private boolean probed;

    @Override
    public void onLoad(Context context) {
        this.context = context;
        this.model = new SelfCheckModel(Checks.ALL);
        context.events().register(this);

        if (!Ui.boot()) {
            context.log("JavaFX is unavailable in this JVM; Self Check will run without a window");
            return;
        }
        Platform.runLater(() -> new SelfCheckWindow(model).show());
        context.log("Window opened; waiting for events");
    }

    // ---- session ---------------------------------------------------------

    @Subscribe
    public void onWorld(World event) {
        String phase = event.phase().name().toLowerCase();
        model.event("world " + phase);
        model.pass(Checks.WORLD, phase);
    }

    @Subscribe
    public void onHero(Hero event) {
        model.pass(Checks.HERO, event.className() + ", level " + event.level());
        probe();
    }

    /**
     * The two Game calls, off the bus thread. A command is a round trip through
     * the host, and asking for one from inside a listener is how a mod ends up
     * waiting for a reply that cannot arrive until the listener returns.
     */
    private void probe() {
        if (probed) {
            return;
        }
        probed = true;
        Thread worker = new Thread(() -> {
            Game game = context.game();
            String type = game.typeName(9);
            if (type != null && type.startsWith("TYPE_")) {
                model.pass(Checks.TYPE_NAME, "typeName(9) = " + type);
            } else {
                model.fail(Checks.TYPE_NAME, "typeName(9) returned " + type);
            }
            String label = game.uiString("UI_STATS_VICTORY");
            if (label != null && !label.isBlank()) {
                model.pass(Checks.UI_STRING, "UI_STATS_VICTORY = " + label);
            } else {
                model.fail(Checks.UI_STRING, "The dictionary returned nothing");
            }
        }, "self-check-probe");
        worker.setDaemon(true);
        worker.start();
    }

    @Subscribe
    public void onPosition(Position event) {
        model.pass(Checks.POSITION, event.hudX() + ", " + event.hudY());
    }

    // ---- health ----------------------------------------------------------

    @Subscribe
    public void onDamage(Damage event) {
        long before = event.next();
        model.event(event.kind() + " " + event.hp() + " → " + before
                    + " (" + event.damage() + " damage)");
        if (!"damage".equals(event.kind())) {
            return;
        }
        long softened = Math.min(before + 1, event.maxHp());
        if (softened == before) {
            model.pass(Checks.HEALTH, "Nothing to soften at " + before + " HP");
            return;
        }
        event.next(softened);
        model.change("Damage softened: " + before + " → " + softened);
        model.pass(Checks.HEALTH, "Kept 1 HP back (" + before + " → " + softened + ")");
    }

    @Subscribe
    public void onNearDeath(NearDeath event) {
        model.pass(Checks.NEAR_DEATH, event.hp() + " HP left, " + event.percent() + "%");
    }

    @Subscribe
    public void onDeath(Death event) {
        model.pass(Checks.DEATH, "Killed by a " + event.blow() + "-point blow");
    }

    @Subscribe
    public void onMobHit(MobHit event) {
        // MobDeath extends MobHit, so this fires for both; the death handler
        // below settles its own scenario.
        model.pass(Checks.MOB_HIT, event.typeName() + " " + event.hp() + " → " + event.next());
    }

    @Subscribe
    public void onMobDeath(MobDeath event) {
        model.pass(Checks.MOB_DEATH, event.typeName() + " at level " + event.level());
    }

    // ---- progression -----------------------------------------------------

    @Subscribe
    public void onGold(Gold event) {
        long delta = event.delta();
        model.event((event.spending() ? "Spent " : "Found ") + Math.abs(delta)
                    + " gold, had " + event.current());
        if (event.spending()) {
            return;
        }
        event.delta(delta + 1);
        model.change("Gold delta " + delta + " → " + (delta + 1));
        model.pass(Checks.GOLD, "Asked for one more than the game offered");
    }

    @Subscribe
    public void onExperience(Experience event) {
        long total = event.next();
        event.next(total + 1);
        model.change("Experience total " + total + " → " + (total + 1));
        model.pass(Checks.EXPERIENCE, "+" + event.gain() + " XP; asked for one more");
    }

    @Subscribe
    public void onSkill(Skill event) {
        // Same value on purpose. A skill point is not ours to spend.
        event.next(event.next());
        model.pass(Checks.SKILL, "Slot " + event.slot() + " → " + event.next()
                                 + "; answered with the same value");
    }

    @Subscribe
    public void onAttribute(Attribute event) {
        event.next(event.next());
        model.pass(Checks.ATTRIBUTE, event.name() + " → " + event.next()
                                     + "; answered with the same value");
    }

    @Subscribe
    public void onLevel(LevelUp event) {
        model.pass(Checks.LEVEL, event.previous() + " → " + event.level());
    }

    // ---- items -----------------------------------------------------------

    @Subscribe
    public void onPickup(Pickup event) {
        String name = event.item().typeName();
        model.event("Picking up " + name);
        if (!event.player()) {
            return;
        }
        // Retyping to the type it already has: the verdict travels the whole way
        // and the item is exactly what it was.
        event.type(event.item().typeId());
        model.pass(Checks.PICKUP, name + "; answered without changing it");
    }

    @Subscribe
    public void onStored(Stored event) {
        model.pass(Checks.STORED, event.item().typeName());
    }

    @Subscribe
    public void onEquip(Equip event) {
        model.pass(Checks.EQUIP, (event.off() ? "Unequipped " : "Equipped ")
                                 + event.item().typeName() + " in slot " + event.slot());
    }

    @Subscribe
    public void onMoved(Moved event) {
        model.pass(Checks.MOVED, event.from() + " → " + event.to());
    }

    // ---- everything else -------------------------------------------------

    @Subscribe
    public void onUnknown(Unknown event) {
        model.pass(Checks.UNKNOWN, event.name());
    }
}
