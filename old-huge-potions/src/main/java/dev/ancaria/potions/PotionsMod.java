package dev.ancaria.potions;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.entity.Item;
import dev.ancaria.coderpack.api.event.Hero;
import dev.ancaria.coderpack.api.event.Pickup;

/**
 * Sacred did not always split potions into sizes. This puts that back: every
 * potion becomes the full-size one of its own kind.
 *
 * <p>It happens at pickup rather than where the potion drops, so a small potion
 * lies on the ground as a small potion and is a large one by the time it
 * reaches the belt. Pickup is a cold, confirmed hook with a verdict channel;
 * the spawn path runs three thousand times during a world load and is not
 * somewhere to be editing objects yet.
 */
public final class PotionsMod implements SacredMod {

    private Context context;
    private Upgrades upgrades;

    @Override
    public void onLoad(Context context) {
        this.context = context;
        context.events().register(this);
    }

    /**
     * The table is built here and not in {@code onLoad} for two reasons: there
     * is no world yet at load time, and building it asks the game a question --
     * which is fine on an ordinary event and not fine inside a veto, where the
     * game thread is waiting on this one.
     */
    @Subscribe
    public void onHero(Hero event) {
        if (upgrades != null) {
            return;
        }
        upgrades = Upgrades.build(context.game());
        context.log(upgrades.size() + (upgrades.size() == 1
                ? " potion type will be upgraded"
                : " potion types will be upgraded"));
    }

    @Subscribe
    public void onPickup(Pickup event) {
        if (upgrades == null || !event.player()) {
            return;
        }
        Item item = event.item();
        int better = upgrades.upgradeFor(item.typeId());
        if (better == 0) {
            return;
        }
        // Retyping edits the object in the world and outlives this pickup,
        // so it is something done to the game rather than a verdict the
        // game is waiting on.
        context.game().retype(item.ref(), better);
        context.log(item.typeName() + " → " + upgrades.name(better));
    }
}
