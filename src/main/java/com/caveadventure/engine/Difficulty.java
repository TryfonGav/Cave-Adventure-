package com.caveadventure.engine;

/**
 * Difficulty settings affecting enemy stats, trap frequency, and loot quality.
 *
 * enemyDamageMultiplier  – scales all enemy attack damage
 * enemyHealthMultiplier  – scales enemy max HP at battle start
 * lootMultiplier         – scales item/gold drop rates
 * trapDamageMultiplier   – scales trap hit damage
 * playerDamageMultiplier – scales player's outgoing damage
 * floorScaling           – extra per-floor compound multiplier applied ON TOP
 *                          of the base scaling (higher = harder late game)
 * poisonPersists         – if true, battle poison lingers after combat ends
 */
public enum Difficulty {
    //                    name      eDmg  eHP   loot  trap  pDmg  floorScale  poison
    EASY  ("Easy",   0.65f, 0.80f, 1.4f, 0.5f, 1.4f, 0.08f, false),
    NORMAL("Normal", 1.00f, 1.00f, 1.0f, 1.0f, 1.0f, 0.12f, false),
    HARD  ("Hard",   2.00f, 1.30f, 0.7f, 2.0f, 0.6f, 0.18f, true);

    public final String name;
    public final float enemyDamageMultiplier;
    public final float enemyHealthMultiplier;
    public final float lootMultiplier;
    public final float trapDamageMultiplier;
    public final float playerDamageMultiplier;
    /** Extra fractional multiplier added per floor (compounding). */
    public final float floorScaling;
    /** Whether battle-applied poison persists a few seconds after combat. */
    public final boolean poisonPersists;

    Difficulty(String name, float eDmg, float eHP, float loot,
               float trapDmg, float pDmg, float floorScale, boolean poison) {
        this.name = name;
        this.enemyDamageMultiplier = eDmg;
        this.enemyHealthMultiplier = eHP;
        this.lootMultiplier = loot;
        this.trapDamageMultiplier = trapDmg;
        this.playerDamageMultiplier = pDmg;
        this.floorScaling = floorScale;
        this.poisonPersists = poison;
    }

    /**
     * Returns the combined enemy damage multiplier for a given floor,
     * compounding the per-floor scaling on top of the base.
     */
    public float getEnemyDamageForFloor(int floor) {
        return enemyDamageMultiplier * (1f + floorScaling * (floor - 1));
    }

    /**
     * Returns the combined enemy health multiplier for a given floor.
     */
    public float getEnemyHealthForFloor(int floor) {
        return enemyHealthMultiplier * (1f + floorScaling * (floor - 1));
    }

    private static Difficulty current = NORMAL;

    public static void setCurrent(Difficulty d) { current = d; }
    public static Difficulty getCurrent()       { return current; }
}
