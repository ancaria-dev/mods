package dev.ancaria.runes;

import dev.ancaria.coderpack.api.entity.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Runes of the hero's own class that have actually been seen this session.
 *
 * <p>These are the templates a foreign rune is turned into, and taking them
 * from real runes rather than assembling one from a type id is the whole
 * point. A rune's type is only its name. What it upgrades lives in its
 * modifier list, and the id there is a combat art nobody has a table for. A
 * rune that was picked up carries a correct pairing of all of it, so copying
 * one wholesale needs no such table.
 *
 * <p>The cost is that the mod does nothing until it has seen one of your runes.
 * That is the right trade: doing nothing is always safe, and inventing a rune
 * out of half-known fields is not.
 */
final class Mine {

    private final List<Item> templates = new ArrayList<>();
    private final Set<Integer> types = new HashSet<>();
    private final Random random = new Random();

    /** Remembers one rune per type. The same rune twice teaches nothing. */
    void remember(Item rune) {
        if (types.add(rune.getTypeId())) {
            templates.add(rune);
        }
    }

    /** Null until one of the hero's own runes has been picked up. */
    Item any() {
        return templates.isEmpty()
                ? null
                : templates.get(random.nextInt(templates.size()));
    }

    int size() {
        return templates.size();
    }
}
