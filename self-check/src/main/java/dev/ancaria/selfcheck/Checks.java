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
    static final String HERO_INFO = "hero-info";
    static final String WORLD = "world";
    static final String LOAD = "load";
    static final String SAVE = "save";
    static final String POSITION = "position";
    static final String REGION = "region";
    static final String SECTOR = "sector";
    static final String DISCOVERY = "discovery";
    static final String HEALTH = "health";
    static final String HEALTH_CHANGED = "health-changed";
    static final String MAX_HEALTH = "max-health";
    static final String DEATH = "death";
    static final String NEAR_DEATH = "near-death";
    static final String RESURRECTION = "resurrection";
    static final String SPAWN = "spawn";
    static final String DESPAWN = "despawn";
    static final String MOB_HIT = "mob-hit";
    static final String MOB_DEATH = "mob-death";
    static final String KILL = "kill";
    static final String LOOT_CHEST = "loot-chest";
    static final String LOOT_DROP = "loot-drop";
    static final String GOLD = "gold";
    static final String GOLD_CHANGED = "gold-changed";
    static final String EXPERIENCE = "experience";
    static final String EXPERIENCE_CHANGED = "experience-changed";
    static final String SKILL = "skill";
    static final String SKILL_CHANGED = "skill-changed";
    static final String SKILL_POINTS = "skill-points";
    static final String ATTRIBUTE = "attribute";
    static final String ATTRIBUTE_CHANGED = "attribute-changed";
    static final String ATTRIBUTE_POINTS = "attribute-points";
    static final String COMBAT_ART = "combat-art";
    static final String COMBAT_ART_CHANGED = "combat-art-changed";
    static final String PICKUP = "pickup";
    static final String STORED = "stored";
    static final String EQUIP = "equip";
    static final String MOVED = "moved";
    static final String DRINK = "drink";
    static final String BUY = "buy";
    static final String SELL = "sell";
    static final String LEVEL = "level";
    static final String QUEST_START = "quest-start";
    static final String QUEST_END = "quest-end";
    static final String CONSOLE = "console";
    static final String TYPE_NAME = "type-name";
    static final String TYPE_ID = "type-id";
    static final String UI_STRING = "ui-string";
    static final String CREATURES = "creatures";
    static final String UNKNOWN = "unknown";

    /** What a player types in the game's console to ask Self Check how it is doing. */
    static final String COMMAND = "selfcheck";

    static final List<Scenario> ALL = List.of(
            Scenario.seen(WORLD, "World loads and unloads"),
            Scenario.seen(LOAD, "Save is loaded"),
            Scenario.seen(HERO, "Hero is captured"),
            Scenario.seen(TYPE_NAME, "Game answers getTypeName"),
            Scenario.seen(TYPE_ID, "Game answers getTypeId"),
            Scenario.seen(UI_STRING, "Game answers getUiString"),
            Scenario.seen(HERO_INFO, "Hero is read through the API"),
            Scenario.seen(CREATURES, "World lists its creatures"),
            Scenario.seen(POSITION, "Player moves"),
            Scenario.seen(SECTOR, "Hero enters a sector"),
            Scenario.seen(REGION, "Hero changes region"),
            Scenario.seen(DISCOVERY, "An area is discovered"),
            Scenario.mutates(HEALTH, "Damage softened by 1 HP"),
            Scenario.seen(HEALTH_CHANGED, "Health changes"),
            Scenario.seen(MAX_HEALTH, "Maximum health changes"),
            Scenario.seen(NEAR_DEATH, "Near death is announced"),
            Scenario.seen(DEATH, "Death is announced"),
            Scenario.seen(RESURRECTION, "Hero is resurrected"),
            Scenario.seen(SPAWN, "A creature spawns"),
            Scenario.seen(DESPAWN, "A creature despawns"),
            Scenario.seen(MOB_HIT, "A creature is hurt"),
            Scenario.seen(MOB_DEATH, "A creature dies"),
            Scenario.seen(KILL, "A kill is counted"),
            Scenario.seen(LOOT_DROP, "A creature drops loot"),
            Scenario.seen(LOOT_CHEST, "A chest is looted"),
            Scenario.mutates(GOLD, "Gold gain raised by 1"),
            Scenario.seen(GOLD_CHANGED, "Gold total changes"),
            Scenario.mutates(EXPERIENCE, "Experience raised by 1"),
            Scenario.seen(EXPERIENCE_CHANGED, "Experience total changes"),
            Scenario.seen(LEVEL, "Level goes up"),
            Scenario.seen(SKILL_POINTS, "Skill points change"),
            Scenario.mutates(SKILL, "Skill write is answered"),
            Scenario.seen(SKILL_CHANGED, "Skill level changes"),
            Scenario.seen(ATTRIBUTE_POINTS, "Attribute points change"),
            Scenario.mutates(ATTRIBUTE, "Attribute spend is answered"),
            Scenario.seen(ATTRIBUTE_CHANGED, "Attribute value changes"),
            Scenario.mutates(COMBAT_ART, "Combat art raise is answered"),
            Scenario.seen(COMBAT_ART_CHANGED, "Combat art level changes"),
            Scenario.mutates(PICKUP, "Pickup is answered"),
            Scenario.seen(STORED, "Item goes into the bag"),
            Scenario.seen(EQUIP, "Item is equipped"),
            Scenario.seen(MOVED, "Item is dragged"),
            Scenario.seen(DRINK, "Hero drinks a potion"),
            Scenario.seen(BUY, "Item is bought"),
            Scenario.seen(SELL, "Item is sold"),
            Scenario.seen(QUEST_START, "A quest starts"),
            Scenario.seen(QUEST_END, "A quest ends"),
            Scenario.seen(SAVE, "Game is saved"),
            Scenario.mutates(CONSOLE, "Console command is claimed"),
            Scenario.seen(UNKNOWN, "An event the SDK has no class for"));

    private Checks() {
    }
}
