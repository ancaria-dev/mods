package dev.ancaria.selfcheck;

import dev.ancaria.coderpack.api.Game;
import dev.ancaria.coderpack.api.Realm;
import dev.ancaria.coderpack.api.entity.Attributes;
import dev.ancaria.coderpack.api.entity.CombatArts;
import dev.ancaria.coderpack.api.entity.Player;
import dev.ancaria.coderpack.api.entity.Sheet;
import dev.ancaria.coderpack.api.entity.Skills;
import dev.ancaria.coderpack.api.entity.Stats;
import dev.ancaria.selfcheck.model.HeroInfo;
import dev.ancaria.selfcheck.model.HeroInfo.Field;
import dev.ancaria.selfcheck.model.HeroInfo.Section;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the hero through the direct API, the half of Coderpack no event
 * carries: asking the game rather than being told.
 *
 * <p>Every call past {@code getPlayer()} and the cached numbers is a round trip
 * through the host, up to two seconds each when the game is slow to answer, so
 * this runs on the probe thread and never on the bus.
 */
final class HeroProbe {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** What one reading came to. */
    record Reading(HeroInfo info, boolean complete, String summary) {
    }

    private HeroProbe() {
    }

    /** Null while no hero is loaded. */
    static Reading read(Game game) {
        Realm world = game.getWorld();
        Player player = world.getEntityRegistry().getPlayer();
        if (player == null) {
            return null;
        }
        Attributes attributes = player.getAttributes();
        Skills skills = player.getSkills();
        CombatArts arts = player.getCombatArts();
        Stats stats = player.getStats();
        Sheet sheet = player.getSheet();
        int region = world.getRegion();
        int sectorX = world.getSectorX();
        int sectorY = world.getSectorY();

        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Hero", List.of(
                new Field("Class", pretty(player.getHeroClass().name())),
                new Field("Level", Integer.toString(player.getLevel())),
                new Field("HP", player.getHp() + " / " + player.getMaxHp()),
                new Field("Gold", Long.toString(player.getGold())),
                new Field("Experience", Long.toString(player.getExp())),
                new Field("Position", player.getX() + ", " + player.getY()),
                new Field("Region", Integer.toString(region)),
                new Field("Sector", sectorX + ", " + sectorY))));

        List<Field> attributeFields = new ArrayList<>();
        for (Attributes.Entry entry : attributes) {
            attributeFields.add(new Field(pretty(entry.getKind().name()),
                                          Integer.toString(entry.getValue())));
        }
        attributeFields.add(new Field("Points to spend", Integer.toString(attributes.getPoints())));
        sections.add(new Section("Attributes", attributeFields));

        List<Field> skillFields = new ArrayList<>();
        for (Skills.Slot slot : skills) {
            if (!slot.isEmpty()) {
                skillFields.add(new Field("Slot " + slot.getIndex(), Integer.toString(slot.getLevel())));
            }
        }
        skillFields.add(new Field("Points to spend", Integer.toString(skills.getPoints())));
        sections.add(new Section("Skills", skillFields));

        List<String> learned = new ArrayList<>();
        for (CombatArts.Art art : arts) {
            if (art.getTotal() > 0) {
                learned.add("#" + art.getArtId() + "/" + art.getAspect() + " " + art.getLevel()
                            + (art.getBonus() == 0 ? "" : "+" + art.getBonus()));
            }
        }
        sections.add(new Section("Combat arts", List.of(
                new Field("Learned", learned.size() + " of " + arts.size()),
                new Field("Levels", learned.isEmpty() ? "none" : String.join(",  ", learned)))));

        sections.add(new Section("Statistics", List.of(
                new Field("Kills", Long.toString(stats.getKills())),
                new Field("Resurrections", Long.toString(stats.getResurrections())),
                new Field("Areas discovered", Long.toString(stats.getDiscoveredAreas())),
                new Field("Play time", clock(stats.getPlayTime())),
                new Field("Since last death", clock(stats.getSinceDeath())),
                new Field("Survival bonus", String.format(Locale.ROOT, "%.2f", stats.getSurvivalBonus())))));

        sections.add(new Section("Character sheet", List.of(
                new Field("Armor", sheet.getArmorPercent() + "%"),
                new Field("Attack speed", Integer.toString(sheet.getAttackSpeed())),
                new Field("Movement speed", Integer.toString(sheet.getMovementSpeed())),
                new Field("Resistances", "physical " + sheet.getResistance(Sheet.Element.PHYSICAL)
                        + ",  fire " + sheet.getResistance(Sheet.Element.FIRE)
                        + ",  magic " + sheet.getResistance(Sheet.Element.MAGIC)
                        + ",  poison " + sheet.getResistance(Sheet.Element.POISON)))));

        // A failed command is an empty answer rather than an exception, so an
        // attribute table of zeroes is what a lost round trip looks like. Every
        // hero has some strength.
        boolean complete = attributes.get(Attributes.Kind.STRENGTH) > 0 && skills.size() > 0;
        String summary = pretty(player.getHeroClass().name()) + ", level " + player.getLevel()
                         + ", " + player.getHp() + "/" + player.getMaxHp() + " HP, region " + region;
        HeroInfo info = new HeroInfo("Read at " + LocalTime.now().format(CLOCK)
                                     + (complete ? "" : ", partly: some answers came back empty"),
                                     sections);
        return new Reading(info, complete, summary);
    }

    /** DARK_ELF as Dark Elf. */
    static String pretty(String constant) {
        StringBuilder out = new StringBuilder(constant.length());
        for (String word : constant.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static String clock(Duration duration) {
        long seconds = duration.toSeconds();
        return String.format(Locale.ROOT, "%d:%02d:%02d",
                             seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
