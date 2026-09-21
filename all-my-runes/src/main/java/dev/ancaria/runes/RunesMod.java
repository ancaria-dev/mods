package dev.ancaria.runes;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.entity.HeroClass;
import dev.ancaria.coderpack.api.entity.Item;
import dev.ancaria.coderpack.api.event.Hero;
import dev.ancaria.coderpack.api.event.Pickup;

/**
 * A rune for somebody else's class becomes one of yours, at pickup.
 *
 * <p>It does that by copying a rune of yours that it has already seen, rather
 * than by writing a type id. The first attempt did the latter and produced a
 * renamed rune that still upgraded the art it always had: a rune's type is its
 * name, and what it does is in its {@link Item#modifiers()}, where the id is a
 * combat art nobody has a table for. A rune you picked up carries all of that
 * already paired up correctly, so it is the template.
 *
 * <p>Which means the mod is quiet until it has seen one of your runes. Pick
 * up a single rune of your own class and it starts working. Quiet is the right
 * failure here: assembling a rune out of half-known fields is exactly how the
 * first version destroyed the runes it could not recognise. An unlisted rune is
 * left alone for the same reason.
 */
public final class RunesMod implements SacredMod {

    private Context context;
    private Owners owners;
    private final Mine mine = new Mine();
    private HeroClass heroClass = HeroClass.UNKNOWN;

    @Override
    public void onLoad(Context context) {
        this.context = context;
        context.events().register(this);
    }

    /**
     * The Vampiress has two class ids, 6 for the knight and 7 for the vampire
     * form, and they are the same character with the same runes. Without this
     * she turns into a vampire and every rune she owns starts reading as
     * somebody else's.
     */
    private static HeroClass same(HeroClass value) {
        return value == HeroClass.VAMPIRESS_FORM ? HeroClass.VAMPIRESS : value;
    }

    @Subscribe
    public void onHero(Hero event) {
        heroClass = same(event.heroClass());
        if (owners != null) {
            return;
        }
        try {
            // Reading the table asks the game for type ids, so it waits for a
            // world, and for an ordinary event, never a veto.
            owners = Owners.load(context.gameDir().resolve("mods"), context.game());
            context.log(owners.known() + (owners.known() == 1 ? " rune is known" : " runes are known")
                        + (owners.unresolved() > 0
                           ? "; this game build does not contain "
                             + owners.unresolved() + (owners.unresolved() == 1
                               ? " rune named in " : " runes named in ") + Owners.FILE
                           : ""));
        } catch (Exception failure) {
            context.log("Could not load the rune table: " + failure);
        }
    }

    @Subscribe
    public void onPickup(Pickup event) {
        if (owners == null || !event.player() || heroClass == HeroClass.UNKNOWN) {
            return;
        }
        Item item = event.item();
        HeroClass owner = owners.ownerOf(item.typeId());
        // Not in the table at all: leave it alone.  Silence means "unknown",
        // not "not yours", and the table is incomplete by construction.
        if (owner == null) {
            return;
        }
        if (owner == heroClass) {
            mine.remember(item);
            return;
        }
        Item template = mine.any();
        if (template == null) {
            context.log("Leaving the " + owner + " rune unchanged until you pick up "
                        + "one of your own to copy");
            return;
        }
        context.game().reshape(item.ref(), template);
        context.log("Changed " + item.typeName() + " from a " + owner + " rune to "
                    + template.typeName());
    }
}
