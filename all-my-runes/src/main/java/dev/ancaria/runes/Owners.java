package dev.ancaria.runes;

import dev.ancaria.coderpack.api.TypeRegistry;
import dev.ancaria.coderpack.api.entity.HeroClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which rune belongs to which class, as type ids.
 *
 * <p>The table is a text file the player owns, not a constant: on first run the
 * seed shipped in the jar is copied to {@code <Sacred Gold>/mods/} and read
 * from there afterwards, so completing it does not mean rebuilding a mod.
 *
 * <p>This says only WHOSE a rune is. What a rune becomes is not built from
 * here (see {@link Mine}) because the table names types, and a type is only
 * a rune's name.
 *
 * <p>The seed covers 79 of the game's 140 runes: the ones whose type name
 * says the class outright, through a {@code DWR_/DEM_/DE_/VL_/ARROW_} prefix or
 * a {@code _SERA/_GLAD/_DELF/_VAMP/_WELF} suffix. The remaining 61 are spell
 * runes whose names carry no class, and they are left commented out. A rune
 * that is not in the file is never touched: silence means "unknown", not "not
 * yours", and swapping a rune away from a player who could have used it is the
 * one mistake worth designing against.
 */
final class Owners {

    static final String FILE = "all-my-runes.txt";
    private static final String SEED = "/runes.txt";

    private final Map<Integer, HeroClass> owner = new HashMap<>();

    private int unresolved;

    /** Reads the table, writing the seed first if the player has no copy yet. */
    static Owners load(Path directory, TypeRegistry types) throws IOException {
        Path file = directory.resolve(FILE);
        if (!Files.exists(file)) {
            try (InputStream seed = Owners.class.getResourceAsStream(SEED)) {
                if (seed != null) {
                    Files.createDirectories(directory);
                    Files.write(file, seed.readAllBytes());
                }
            }
        }
        Owners owners = new Owners();
        if (Files.exists(file)) {
            owners.parse(Files.readAllLines(file, StandardCharsets.UTF_8), ids(types));
        }
        return owners;
    }

    /** Both rune families, resolved to ids in two round-trips. */
    private static Map<String, Integer> ids(TypeRegistry types) {
        Map<String, Integer> all = new HashMap<>(types.types("TYPE_SMOVE_UPGRADE_"));
        all.putAll(types.types("TYPE_SPELL_UPGRADE_"));
        return all;
    }

    private void parse(List<String> lines, Map<String, Integer> ids) {
        for (String raw : lines) {
            String line = raw.strip();
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment).strip();
            }
            String[] parts = line.split("\\s+");
            if (parts.length != 2) {
                continue;
            }
            HeroClass heroClass;
            try {
                heroClass = HeroClass.valueOf(parts[0]);
            } catch (IllegalArgumentException notAClass) {
                continue;
            }
            Integer id = ids.get(parts[1]);
            if (id == null) {
                // In the file but not in this build: worth counting, not worth
                // a line of noise each.
                unresolved++;
                continue;
            }
            owner.put(id, heroClass);
        }
    }

    /** Null when nothing is known about this type, including "not a rune". */
    HeroClass ownerOf(int typeId) {
        return owner.get(typeId);
    }

    int known() {
        return owner.size();
    }

    int unresolved() {
        return unresolved;
    }
}
