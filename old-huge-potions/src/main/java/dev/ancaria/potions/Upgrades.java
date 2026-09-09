package dev.ancaria.potions;

import dev.ancaria.coderpack.api.Game;

import java.util.HashMap;
import java.util.Map;

/**
 * Which potion type becomes which, as ids.
 *
 * <p>Nothing here is a list of potions. The table is derived from the type
 * names the running game reports, by the one rule the names already encode:
 * a size word swapped for the largest one. Sacred has two potion families and
 * they name their sizes differently ({@code SMALL/MEDIUM/LARGE_<colour>} and
 * {@code <kind>_MINOR/MAJOR/FULL}) so both spellings are handled, and a
 * colour or kind nobody has seen yet is picked up for free.
 */
final class Upgrades {

    private static final String PREFIX = "TYPE_OBJECT_POTION_";

    private final Map<Integer, Integer> upgrade = new HashMap<>();
    private final Map<Integer, String> names = new HashMap<>();

    /** One round-trip. Call it when a world exists, never from a veto. */
    static Upgrades build(Game game) {
        Upgrades table = new Upgrades();
        Map<String, Integer> potions = game.types(PREFIX);
        for (Map.Entry<String, Integer> potion : potions.entrySet()) {
            String best = largest(potion.getKey());
            Integer target = best == null ? null : potions.get(best);
            if (target == null || target.equals(potion.getValue())) {
                continue;
            }
            table.upgrade.put(potion.getValue(), target);
            table.names.put(potion.getValue(), potion.getKey());
            table.names.put(target, best);
        }
        return table;
    }

    /** The full-size name of a potion, or null when it already is one. */
    static String largest(String name) {
        if (name.contains("_SMALL_")) {
            return name.replace("_SMALL_", "_LARGE_");
        }
        if (name.contains("_MEDIUM_")) {
            return name.replace("_MEDIUM_", "_LARGE_");
        }
        if (name.endsWith("_MINOR")) {
            return name.substring(0, name.length() - "_MINOR".length()) + "_FULL";
        }
        if (name.endsWith("_MAJOR")) {
            return name.substring(0, name.length() - "_MAJOR".length()) + "_FULL";
        }
        return null;
    }

    /** 0 when this type is not a potion, or is already the full-size one. */
    int upgradeFor(int typeId) {
        return upgrade.getOrDefault(typeId, 0);
    }

    String name(int typeId) {
        return names.getOrDefault(typeId, "type" + typeId);
    }

    int size() {
        return upgrade.size();
    }
}
