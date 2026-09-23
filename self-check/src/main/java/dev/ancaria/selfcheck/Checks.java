package dev.ancaria.selfcheck;

import dev.ancaria.selfcheck.model.Scenario;

import java.util.List;

/**
 * Everything the loader can deliver, in the order a session tends to deliver it.
 *
 * <p>Keys are short handles rather than wire names: two scenarios watch the same
 * wire event (a mob hit and a mob death both arrive as entity events) and one
 * watches no event at all.
 */
final class Checks {

    static final String HERO = "hero";
    static final String WORLD = "world";
    static final String POSITION = "position";
    static final String HEALTH = "health";
    static final String DEATH = "death";
    static final String NEAR_DEATH = "near-death";
    static final String MOB_HIT = "mob-hit";
    static final String MOB_DEATH = "mob-death";
    static final String GOLD = "gold";
    static final String EXPERIENCE = "experience";
    static final String SKILL = "skill";
    static final String ATTRIBUTE = "attribute";
    static final String PICKUP = "pickup";
    static final String STORED = "stored";
    static final String EQUIP = "equip";
    static final String MOVED = "moved";
    static final String LEVEL = "level";
    static final String TYPE_NAME = "type-name";
    static final String UI_STRING = "ui-string";
    static final String UNKNOWN = "unknown";

    static final List<Scenario> ALL = List.of(
            Scenario.seen(WORLD, "World loads and unloads"),
            Scenario.seen(HERO, "Hero is captured"),
            Scenario.seen(TYPE_NAME, "Game answers getTypeName"),
            Scenario.seen(UI_STRING, "Game answers getUiString"),
            Scenario.seen(POSITION, "Player moves"),
            Scenario.mutates(HEALTH, "Damage softened by 1 HP"),
            Scenario.seen(NEAR_DEATH, "Near death is announced"),
            Scenario.seen(DEATH, "Death is announced"),
            Scenario.seen(MOB_HIT, "A creature is hurt"),
            Scenario.seen(MOB_DEATH, "A creature dies"),
            Scenario.mutates(GOLD, "Gold gain raised by 1"),
            Scenario.mutates(EXPERIENCE, "Experience raised by 1"),
            Scenario.mutates(SKILL, "Skill write is answered"),
            Scenario.mutates(ATTRIBUTE, "Attribute spend is answered"),
            Scenario.mutates(PICKUP, "Pickup is answered"),
            Scenario.seen(STORED, "Item goes into the bag"),
            Scenario.seen(EQUIP, "Item is equipped"),
            Scenario.seen(MOVED, "Item is dragged"),
            Scenario.seen(LEVEL, "Level goes up"),
            Scenario.seen(UNKNOWN, "An event the SDK has no class for"));

    private Checks() {
    }
}
