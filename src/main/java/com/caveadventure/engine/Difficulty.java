package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Difficulty settings affecting enemy stats, trap frequency, and loot quality.
 *
 * enemyDamageMultiplier  – scales all enemy attack damage
 * enemyHealthMultiplier  – scales enemy max HP at battle start
 * lootMultiplier         – scales item/gold drop rates
 * trapDamageMultiplier   – scales trap hit damage
 * playerDamageMultiplier – scales player's outgoing damage
 * floorScaling           – enemy HP and damage grow by this fraction per floor
 *                          (bosses get {@link Balance#BOSS_FLOOR_SCALING_SHARE} of it);
 *                          this, not a flat multiplier, carries most of the difficulty
 * poisonPersists         – if true, battle poison lingers after combat ends
 *
 * Values are decision D3 of the improvement plan (Phase 2).
 */
public enum Difficulty {
    //                    name      eDmg  eHP   loot  trap  pDmg  floorScale  poison
    EASY  ("Easy",   0.70f, 0.85f, 1.3f, 0.5f, 1.20f, 0.12f, false),
    NORMAL("Normal", 1.00f, 1.00f, 1.0f, 1.0f, 1.00f, 0.18f, false),
    HARD  ("Hard",   1.40f, 1.25f, 0.8f, 1.5f, 0.85f, 0.25f, true);

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
     * Per-floor growth of enemy stats: {@code 1 + floorScaling·(floor−1)}, with only
     * {@link Balance#BOSS_FLOOR_SCALING_SHARE} of the scaling for bosses.
     */
    public float floorFactor(int floor, boolean boss) {
        float perFloor = boss ? floorScaling * Balance.BOSS_FLOOR_SCALING_SHARE : floorScaling;
        return 1f + perFloor * (Math.max(1, floor) - 1);
    }

    /**
     * Returns the combined enemy damage multiplier for a given floor.
     */
    public float getEnemyDamageForFloor(int floor, boolean boss) {
        return enemyDamageMultiplier * floorFactor(floor, boss);
    }

    /**
     * Returns the combined enemy health multiplier for a given floor.
     */
    public float getEnemyHealthForFloor(int floor, boolean boss) {
        return enemyHealthMultiplier * floorFactor(floor, boss);
    }

    private static Difficulty current = NORMAL;

    public static void setCurrent(Difficulty d) {
        current = d == null ? NORMAL : d;
        saveCurrent();
    }
    public static Difficulty getCurrent()       { return current; }

    public static void loadCurrent() {
        if (Gdx.app == null)
            return;
        Preferences prefs = Gdx.app.getPreferences("CaveAdventureSettings");
        try {
            current = Difficulty.valueOf(prefs.getString("difficulty", NORMAL.name()));
        } catch (IllegalArgumentException ex) {
            current = NORMAL;
        }
    }

    private static void saveCurrent() {
        if (Gdx.app == null)
            return;
        Preferences prefs = Gdx.app.getPreferences("CaveAdventureSettings");
        prefs.putString("difficulty", current.name());
        prefs.flush();
    }
}
