package com.caveadventure.engine;

import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Enemy.EnemyType;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import com.caveadventure.item.Item.ItemType;
import com.caveadventure.item.LootTable;
import com.caveadventure.world.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2-T1 / P2-T4 / decision D3: anchor points of the combat curve on all three
 * difficulties, so future tuning can't silently break it. Uses the real
 * {@link Player} and {@link Enemy} formulas with average rolls (no crits, no skills,
 * no items), the player's level from {@link XpPacingTest}'s measured pacing, and
 * the gear of the floor's band.
 */
class BalanceCurveTest {

    /** One fight: an enemy type on a floor, met at the player level the XP pacing gives there. */
    private record Anchor(int floor, int level, EnemyType enemy) {
        boolean boss() {
            return enemy.isBoss();
        }
    }

    /** Player level at the start of each floor, from XpPacingTest (rounded). */
    private static final List<Anchor> ANCHORS = List.of(
            new Anchor(1, 1, EnemyType.SKELETON),
            new Anchor(3, 7, EnemyType.SKELETON),
            new Anchor(5, 11, EnemyType.BOSS_GOLEM),
            new Anchor(6, 13, EnemyType.ICE_DRAKE),
            new Anchor(9, 18, EnemyType.SKELETON),
            new Anchor(9, 18, EnemyType.ICE_DRAKE),
            new Anchor(10, 19, EnemyType.BOSS_WYRM));

    /** Hits to kill the enemy and hits the player survives, at average rolls. */
    private record Fight(double hitsToKill, double hitsToDie) {
    }

    @AfterEach
    void restore() {
        Difficulty.setCurrent(Difficulty.NORMAL);
        LootTable.setRandomForTest(null);
    }

    /** Weapon and armor for the floor's band (shop gear in band 2, Crystal gear from floor 7). */
    private static ItemType[] bandGear(int floor) {
        switch (Balance.floorBand(floor)) {
            case 0:
                return new ItemType[] { ItemType.IRON_SWORD, ItemType.LEATHER_ARMOR };
            case 1:
                return new ItemType[] { ItemType.STEEL_SWORD, ItemType.PLATE_ARMOR };
            default:
                return new ItemType[] { ItemType.CRYSTAL_BLADE, ItemType.CRYSTAL_ARMOR };
        }
    }

    private static Player playerAt(int level, int floor) {
        Player player = new Player(1, 1);
        while (player.getLevel() < level)
            player.addXP(player.getXPToNextLevel() - player.getXP());
        for (ItemType type : bandGear(floor)) {
            player.getInventory().addItem(new Item(type));
            player.getInventory().equipItem(indexOf(player, type));
        }
        return player;
    }

    private static int indexOf(Player player, ItemType type) {
        List<Item> items = player.getInventory().getItems();
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).getType() == type)
                return i;
        return -1;
    }

    private static Fight fight(Difficulty difficulty, Anchor anchor) {
        Difficulty.setCurrent(difficulty);
        Player player = playerAt(anchor.level(), anchor.floor());
        Enemy enemy = new Enemy(0, 0, anchor.enemy());
        enemy.scaleForFloor(anchor.floor());

        double playerHit = (player.getTotalAttack() + 2.5) * difficulty.playerDamageMultiplier; // basic attack rolls +0..5
        int rawEnemyHit = Math.round((enemy.getScaledMinDamage() + enemy.getScaledMaxDamage()) / 2f);
        int enemyHit = Balance.applyArmor(rawEnemyHit, player.getInventory().getTotalDefenseBonus());
        return new Fight(enemy.getMaxHealth() / playerHit, player.getMaxHealth() / (double) enemyHit);
    }

    @Test
    void anchorsOnEveryDifficulty() {
        StringBuilder table = new StringBuilder("Balance anchors (hits to kill / hits to die):\n");
        for (Anchor a : ANCHORS) {
            Fight easy = fight(Difficulty.EASY, a);
            Fight normal = fight(Difficulty.NORMAL, a);
            Fight hard = fight(Difficulty.HARD, a);
            table.append(String.format("  F%-2d L%-2d %-11s Easy %4.1f / %4.1f   Normal %4.1f / %4.1f   Hard %4.1f / %4.1f%n",
                    a.floor(), a.level(), a.enemy().name, easy.hitsToKill(), easy.hitsToDie(),
                    normal.hitsToKill(), normal.hitsToDie(), hard.hitsToKill(), hard.hitsToDie()));
        }
        System.out.print(table);

        for (Anchor a : ANCHORS) {
            Fight easy = fight(Difficulty.EASY, a);
            Fight normal = fight(Difficulty.NORMAL, a);
            Fight hard = fight(Difficulty.HARD, a);
            String where = a + "\n" + table;

            // Difficulties are ordered on every anchor
            assertTrue(easy.hitsToKill() < normal.hitsToKill() && normal.hitsToKill() < hard.hitsToKill(), where);
            assertTrue(easy.hitsToDie() > normal.hitsToDie() && normal.hitsToDie() > hard.hitsToDie(), where);

            if (a.boss()) {
                assertTrue(normal.hitsToKill() <= 12, where);
                assertTrue(normal.hitsToDie() >= 4, where);
                assertTrue(hard.hitsToKill() <= 20, where);
                assertTrue(hard.hitsToDie() >= 2.5, where);
            } else {
                assertTrue(normal.hitsToKill() >= 1.5 && normal.hitsToKill() <= 4.5, where);
                assertTrue(normal.hitsToDie() >= 5, where);
                assertTrue(hard.hitsToKill() <= 8, where);
                assertTrue(hard.hitsToDie() >= 3, where);
            }
        }
    }

    @Test
    void levelUpsMakeThePlayerStrongerNotTheEnemy() {
        Enemy enemy = new Enemy(0, 0, EnemyType.SKELETON);
        enemy.scaleForFloor(4);
        int enemyHp = enemy.getMaxHealth();

        Player low = playerAt(5, 4);
        Player high = playerAt(6, 4);
        assertEquals(low.getTotalAttack() + Balance.PLAYER_ATTACK_PER_LEVEL, high.getTotalAttack());
        assertEquals(low.getMaxHealth() + Balance.PLAYER_HP_PER_LEVEL, high.getMaxHealth());

        enemy.scaleForFloor(4); // the enemy doesn't know the player's level
        assertEquals(enemyHp, enemy.getMaxHealth());
    }

    @Test
    void bossesGetHalfTheFloorScaling() {
        Difficulty d = Difficulty.NORMAL;
        assertEquals(1f + d.floorScaling * 9, d.floorFactor(10, false), 1e-5);
        assertEquals(1f + d.floorScaling * 9 / 2, d.floorFactor(10, true), 1e-5);
    }

    @Test
    void armorIsAPercentageAndDamageOverTimeIgnoresIt() {
        assertEquals(50, Balance.applyArmor(100, 50));
        assertEquals(66, Balance.applyArmor(100, 26)); // Frost Mail: -34 %
        assertEquals(100, Balance.applyArmor(100, 0));
        assertEquals(1, Balance.applyArmor(1, 26));

        Player player = playerAt(1, 7); // Crystal Armor, DEF 22
        int hp = player.getHealth();
        player.takeDamage(30);
        assertEquals(hp - Balance.applyArmor(30, 22), player.getHealth());
        hp = player.getHealth();
        player.takeTrueDamage(3); // a poison tick
        assertEquals(hp - 3, player.getHealth());
    }

    @Test
    void poisonPersistsOnlyOnHard() {
        assertFalse(Difficulty.EASY.poisonPersists);
        assertFalse(Difficulty.NORMAL.poisonPersists);
        assertTrue(Difficulty.HARD.poisonPersists);
    }

    @Test
    void startingTorchLastsNinetySeconds() {
        assertEquals(90f, new Player(1, 1).getTorchDuration());
    }

    // --- P2-T4: item power curve ---

    /** Enemy types that can spawn on the floor (CombatManager.pickEnemyType) plus the floor's boss. */
    private static Set<EnemyType> enemiesOn(int floor) {
        Set<EnemyType> types = floor > 5
                ? EnumSet.of(EnemyType.BAT, EnemyType.SLIME, EnemyType.CAVE_SPIDER, EnemyType.GOBLIN,
                        EnemyType.SKELETON, EnemyType.NECROMANCER, EnemyType.SHADOW, EnemyType.ICE_DRAKE)
                : EnumSet.of(EnemyType.BAT, EnemyType.SLIME, EnemyType.CAVE_SPIDER, EnemyType.GOBLIN,
                        EnemyType.SKELETON);
        if (floor == 5)
            types.add(EnemyType.BOSS_GOLEM);
        if (floor == 10)
            types.add(EnemyType.BOSS_WYRM);
        return types;
    }

    /** Highest weapon ATK from chests, enemy drops and the weapon event on the floor (2000 rolls each). */
    private static int bestWeaponFromDrops(int floor) {
        LootTable.setRandomForTest(new Random(floor));
        int best = Balance.bandWeapon(floor).attackBonus; // Hollow Echo event
        for (int i = 0; i < 2000; i++) {
            best = Math.max(best, bestWeapon(LootTable.getChestLoot(floor, true, Biome.forFloor(floor))));
            for (EnemyType type : enemiesOn(floor))
                best = Math.max(best, bestWeapon(LootTable.getEnemyDrop(type, floor, true)));
        }
        return best;
    }

    private static int bestWeapon(List<Item> loot) {
        int best = 0;
        for (Item item : loot)
            if (item.getType().category == Item.Category.WEAPON)
                best = Math.max(best, item.getType().attackBonus);
        return best;
    }

    @Test
    void bestObtainableWeaponFollowsTheFloorBands() {
        Difficulty.setCurrent(Difficulty.EASY); // highest drop chances: the worst case for the curve
        int[] cap = { 0, 8, 8, 8, 18, 18, 20, 22, 22, 22, 28 }; // index = floor
        StringBuilder report = new StringBuilder("Best weapon ATK from drops:");
        for (int floor = 1; floor <= 10; floor++) {
            int best = bestWeaponFromDrops(floor);
            report.append(" F").append(floor).append('=').append(best);
            assertTrue(best <= cap[floor], report.toString());
        }
        System.out.println(report);
        assertEquals(ItemType.FIRE_AXE.attackBonus, bestWeaponFromDrops(10));
        assertTrue(bestWeaponFromDrops(3) < ItemType.STEEL_SWORD.attackBonus);
    }

    @Test
    void lootMultiplierScalesDropChances() {
        int[] counts = new int[3];
        Difficulty[] order = { Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD };
        for (int d = 0; d < order.length; d++) {
            Difficulty.setCurrent(order[d]);
            LootTable.setRandomForTest(new Random(42));
            for (int i = 0; i < 5000; i++)
                counts[d] += LootTable.getEnemyDrop(EnemyType.BAT, 1, false).size();
        }
        assertTrue(counts[0] > counts[1] && counts[1] > counts[2],
                "Easy/Normal/Hard drops: " + counts[0] + "/" + counts[1] + "/" + counts[2]);
    }
}
