package com.caveadventure.engine;

import com.caveadventure.item.Item;

/**
 * Gameplay tuning values in one place (Phase 2 of the improvement plan, decisions
 * D3 / D3b). Per-difficulty multipliers live in {@link Difficulty}; everything that
 * is the same on every difficulty lives here.
 */
public final class Balance {

    private Balance() {
    }

    // --- Player progression ---

    /** Player attack at level 1, before the weapon. */
    public static final int PLAYER_BASE_ATTACK = 10;
    /** Attack gained per level. */
    public static final int PLAYER_ATTACK_PER_LEVEL = 1;
    /** Max HP gained per level. */
    public static final int PLAYER_HP_PER_LEVEL = 10;
    /** XP needed to go from level 1 to level 2. */
    public static final int FIRST_LEVEL_XP = 100;
    /** Each level needs this many times the XP of the one before. */
    public static final float XP_CURVE = 1.25f;

    /** Player attack at the given level, without equipment. */
    public static int playerAttackForLevel(int level) {
        return PLAYER_BASE_ATTACK + PLAYER_ATTACK_PER_LEVEL * (Math.max(1, level) - 1);
    }

    // --- Enemy XP (D3b: floor multiplier) ---

    /** Enemy XP grows by this fraction of its base value per floor below the first. */
    public static final float ENEMY_XP_PER_FLOOR = 0.15f;

    /** XP multiplier on the given floor: {@code 1 + 0.15·(floor−1)}. */
    public static float floorXpMultiplier(int floor) {
        return 1f + ENEMY_XP_PER_FLOOR * (Math.max(1, floor) - 1);
    }

    /** XP for killing an enemy with the given base reward on the given floor. */
    public static int enemyXp(int baseXp, int floor) {
        return Math.round(baseXp * floorXpMultiplier(floor));
    }

    // --- Enemy floor scaling ---

    /** Bosses get this share of the difficulty's per-floor scaling (their base stats already reflect depth). */
    public static final float BOSS_FLOOR_SCALING_SHARE = 0.5f;

    // --- Armor ---

    /** Defense at which armor halves incoming damage: {@code damage × (1 − DEF/(DEF+50))}. */
    public static final int ARMOR_HALF_POINT = 50;

    /** Damage left after armor with the given defense; at least 1. */
    public static int applyArmor(int damage, int defense) {
        if (defense <= 0)
            return Math.max(1, damage);
        float kept = 1f - defense / (float) (defense + ARMOR_HALF_POINT);
        return Math.max(1, Math.round(damage * kept));
    }

    // --- Survival ---

    /** Seconds of torch light a new character starts with. */
    public static final float STARTING_TORCH_SECONDS = 90f;

    // --- Random event rewards (scale with floor) ---

    /** XP from an XP event on floor 1; later floors use {@link #floorXpMultiplier}. */
    public static final int EVENT_XP_BASE = 75;
    /** Share of max HP the mushroom event heals. */
    public static final float EVENT_HEAL_FRACTION = 0.30f;
    /** Gold nuggets from the merchant event on floor 1. */
    public static final int EVENT_GOLD_BASE = 3;
    /** One extra nugget every this many floors. */
    public static final int EVENT_GOLD_FLOORS_PER_NUGGET = 3;

    /** XP an XP event gives on the given floor. */
    public static int eventXp(int floor) {
        return enemyXp(EVENT_XP_BASE, floor);
    }

    /** HP the healing event restores for a player with the given max HP. */
    public static int eventHeal(int maxHealth) {
        return Math.max(1, Math.round(maxHealth * EVENT_HEAL_FRACTION));
    }

    /** Gold nuggets the merchant event gives on the given floor. */
    public static int eventGold(int floor) {
        return EVENT_GOLD_BASE + Math.max(1, floor) / EVENT_GOLD_FLOORS_PER_NUGGET;
    }

    // --- Item power curve (floor bands) ---

    /** Rare weapon per floor band: floors 1–3, 4–6, 7–9, 10+. */
    private static final Item.ItemType[] BAND_WEAPONS = {
            Item.ItemType.IRON_SWORD, Item.ItemType.GREEN_BLADE,
            Item.ItemType.CRYSTAL_BLADE, Item.ItemType.FIRE_AXE
    };

    /** Floor band: 0 for floors 1–3, 1 for 4–6, 2 for 7–9, 3 for 10 and deeper. */
    public static int floorBand(int floor) {
        return Math.min(BAND_WEAPONS.length - 1, (Math.max(1, floor) - 1) / 3);
    }

    /** The rare weapon chests and the weapon event give on the given floor. */
    public static Item.ItemType bandWeapon(int floor) {
        return BAND_WEAPONS[floorBand(floor)];
    }
}
