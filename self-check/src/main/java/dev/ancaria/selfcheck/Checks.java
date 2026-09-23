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
            Scenario.seen(WORLD, "World loads and unloads",
                    "Load a save or start a new game"),
            Scenario.seen(LOAD, "Save is loaded",
                    "Load a saved game from the main menu"),
            Scenario.seen(HERO, "Hero is captured",
                    "Enter the game with any character"),
            Scenario.seen(TYPE_NAME, "Game answers getTypeName",
                    "Nothing to do: asked once the hero is in the game"),
            Scenario.seen(TYPE_ID, "Game answers getTypeId",
                    "Nothing to do: asked once the hero is in the game"),
            Scenario.seen(UI_STRING, "Game answers getUiString",
                    "Nothing to do: asked once the hero is in the game"),
            Scenario.seen(HERO_INFO, "Hero is read through the API",
                    "Nothing to do: read every two seconds, shown in the Hero panel"),
            Scenario.seen(CREATURES, "World lists its creatures",
                    "Nothing to do: asked once the hero is in the game"),
            Scenario.seen(POSITION, "Player moves",
                    "Walk a few steps"),
            Scenario.seen(SECTOR, "Hero enters a sector",
                    "Walk on until the hero crosses into the next map sector"),
            Scenario.seen(REGION, "Hero changes region",
                    "Travel to another region, through a gate, a portal or a teleporter"),
            Scenario.seen(DISCOVERY, "An area is discovered",
                    "Explore until the game reports a newly discovered area"),
            Scenario.mutates(HEALTH, "Damage softened by 1 HP",
                    "Let a monster hit you"),
            Scenario.seen(HEALTH_CHANGED, "Health changes",
                    "Take damage, or drink a healing potion"),
            Scenario.seen(MAX_HEALTH, "Maximum health changes",
                    "Level up, or equip or remove an item that changes maximum health"),
            Scenario.seen(NEAR_DEATH, "Near death is announced",
                    "Let your health fall close to zero"),
            Scenario.seen(DEATH, "Death is announced",
                    "Let the hero die"),
            Scenario.seen(RESURRECTION, "Hero is resurrected",
                    "Die and come back at a resurrection point"),
            Scenario.seen(SPAWN, "A creature spawns",
                    "Walk towards monsters or into a town"),
            Scenario.seen(DESPAWN, "A creature despawns",
                    "Walk far away from creatures you have seen, or kill one"),
            Scenario.seen(MOB_HIT, "A creature is hurt",
                    "Hit any monster"),
            Scenario.seen(MOB_DEATH, "A creature dies",
                    "Kill any monster"),
            Scenario.seen(KILL, "A kill is counted",
                    "Defeat an opponent, so the statistics count goes up"),
            Scenario.seen(LOOT_DROP, "A creature drops loot",
                    "Kill monsters until one drops an item"),
            Scenario.seen(LOOT_CHEST, "A chest is looted",
                    "Open any chest or barrel"),
            Scenario.mutates(GOLD, "Gold gain raised by 1",
                    "Pick up gold"),
            Scenario.seen(GOLD_CHANGED, "Gold total changes",
                    "Pick up, spend or earn gold"),
            Scenario.mutates(EXPERIENCE, "Experience raised by 1",
                    "Kill a monster to earn experience"),
            Scenario.seen(EXPERIENCE_CHANGED, "Experience total changes",
                    "Earn experience"),
            Scenario.seen(LEVEL, "Level goes up",
                    "Earn enough experience to reach the next level"),
            Scenario.seen(SKILL_POINTS, "Skill points change",
                    "Level up, or spend a skill point"),
            Scenario.mutates(SKILL, "Skill write is answered",
                    "Spend a skill point in the character screen"),
            Scenario.seen(SKILL_CHANGED, "Skill level changes",
                    "Spend a skill point in the character screen"),
            Scenario.seen(ATTRIBUTE_POINTS, "Attribute points change",
                    "Level up, or spend an attribute point"),
            Scenario.mutates(ATTRIBUTE, "Attribute spend is answered",
                    "Spend an attribute point in the character screen"),
            Scenario.seen(ATTRIBUTE_CHANGED, "Attribute value changes",
                    "Spend an attribute point in the character screen"),
            Scenario.mutates(COMBAT_ART, "Combat art raise is answered",
                    "Read a rune of your class to learn or raise a combat art"),
            Scenario.seen(COMBAT_ART_CHANGED, "Combat art level changes",
                    "Read a rune of your class to learn or raise a combat art"),
            Scenario.mutates(PICKUP, "Pickup is answered",
                    "Pick up any item"),
            Scenario.seen(STORED, "Item goes into the bag",
                    "Pick up an item into your inventory"),
            Scenario.seen(EQUIP, "Item is equipped",
                    "Equip or take off a weapon, a piece of armour or a ring"),
            Scenario.seen(MOVED, "Item is dragged",
                    "Drag an item to another inventory slot"),
            Scenario.seen(DRINK, "Hero drinks a potion",
                    "Drink any potion"),
            Scenario.seen(BUY, "Item is bought",
                    "Buy an item from a merchant"),
            Scenario.seen(SELL, "Item is sold",
                    "Sell an item to a merchant by dragging it or with Shift+click"),
            Scenario.seen(QUEST_START, "A quest starts",
                    "Accept a quest from a character with a quest marker"),
            Scenario.seen(QUEST_END, "A quest ends",
                    "Finish a quest and hand it in"),
            Scenario.seen(SAVE, "Game is saved",
                    "Save the game"),
            Scenario.mutates(CONSOLE, "Console command is claimed",
                    "Open the game console and type selfcheck"),
            Scenario.seen(UNKNOWN, "An event the SDK has no class for",
                    "Nothing to do: passes on an event this API has no class for"));

    private Checks() {
    }
}
