package com.caveadventure.engine;

/**
 * Difficulty settings affecting enemy stats, trap frequency, and loot quality.
 */
public enum Difficulty {
    EASY("Easy", 0.7f, 1.3f, 0.5f, 1.3f),
    NORMAL("Normal", 1.0f, 1.0f, 1.0f, 1.0f),
    HARD("Hard", 1.5f, 0.7f, 1.5f, 0.7f);

    public final String name;
    public final float enemyDamageMultiplier;
    public final float lootMultiplier;
    public final float trapDamageMultiplier;
    public final float playerDamageMultiplier;

    Difficulty(String name, float enemyDmg, float loot, float trapDmg, float playerDmg) {
        this.name = name;
        this.enemyDamageMultiplier = enemyDmg;
        this.lootMultiplier = loot;
        this.trapDamageMultiplier = trapDmg;
        this.playerDamageMultiplier = playerDmg;
    }

    private static Difficulty current = NORMAL;

    public static void setCurrent(Difficulty d) {
        current = d;
    }

    public static Difficulty getCurrent() {
        return current;
    }
}
