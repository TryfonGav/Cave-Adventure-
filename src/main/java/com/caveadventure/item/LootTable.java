package com.caveadventure.item;

import com.caveadventure.entity.Enemy;

import java.util.*;

/**
 * Generates loot drops from enemies and chests based on weighted tables.
 */
public class LootTable {

    private static final Random RANDOM = new Random();

    /**
     * Generate loot from killing an enemy.
     */
    public static List<Item> getEnemyDrop(Enemy.EnemyType type, int floor) {
        List<Item> loot = new ArrayList<>();

        switch (type) {
            case BAT:
                if (chance(30))
                    loot.add(new Item(Item.ItemType.COOKED_MEAT));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.HEALTH_POTION));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.GOLD_COINS));
                break;

            case SLIME:
                if (chance(25))
                    loot.add(new Item(Item.ItemType.HEALTH_POTION));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.ANTIDOTE));
                if (chance(20))
                    loot.add(new Item(Item.ItemType.GOLD_COINS));
                break;

            case CAVE_SPIDER:
                if (chance(20))
                    loot.add(new Item(Item.ItemType.ANTIDOTE));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.COOKED_MEAT));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.GOLD_COINS));
                break;

            case GOBLIN:
                if (chance(35))
                    loot.add(new Item(Item.ItemType.GOLD_COINS));
                if (chance(20))
                    loot.add(new Item(Item.ItemType.FOOD_RATION));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.RUSTY_SWORD));
                if (chance(5))
                    loot.add(new Item(Item.ItemType.LEATHER_ARMOR));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.SMOKE_BOMB));
                break;

            case SKELETON:
                if (chance(30))
                    loot.add(new Item(Item.ItemType.GOLD_COINS));
                if (chance(20))
                    loot.add(new Item(Item.ItemType.HEALTH_POTION));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.IRON_SWORD));
                if (chance(8))
                    loot.add(new Item(Item.ItemType.CHAIN_MAIL));
                if (chance(5))
                    loot.add(new Item(Item.ItemType.BRONZE_KEY));
                break;

            case NECROMANCER:
                if (chance(40))
                    loot.add(new Item(Item.ItemType.HEALTH_POTION));
                if (chance(25))
                    loot.add(new Item(Item.ItemType.ANTIDOTE));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.ANCIENT_RELIC));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.GOLD_NUGGET, 2));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.FIRE_RUNE));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.SHADOW_RUNE));
                break;

            case SHADOW:
                if (chance(30))
                    loot.add(new Item(Item.ItemType.GOLD_NUGGET, 2));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.GEMSTONE));
                if (chance(8) && floor > 3 && floor <= 7)
                    loot.add(new Item(Item.ItemType.GREEN_BLADE));
                if (chance(10))
                    loot.add(new Item(Item.ItemType.DIAMOND));
                if (chance(12))
                    loot.add(new Item(Item.ItemType.SMOKE_BOMB));
                if (chance(5))
                    loot.add(new Item(Item.ItemType.SHADOW_BLADE));
                break;

            case ICE_DRAKE:
                if (chance(40))
                    loot.add(new Item(Item.ItemType.LARGE_HEALTH_POTION));
                if (chance(20))
                    loot.add(new Item(Item.ItemType.GOLD_NUGGET, 3));
                if (chance(8) && floor > 5)
                    loot.add(new Item(Item.ItemType.CRYSTAL_BLADE));
                if (chance(8))
                    loot.add(new Item(Item.ItemType.CRYSTAL_ARMOR));
                if (chance(5))
                    loot.add(new Item(Item.ItemType.SILVER_KEY));
                if (chance(20))
                    loot.add(new Item(Item.ItemType.FROST_RUNE));
                if (chance(15))
                    loot.add(new Item(Item.ItemType.ICE_SHARD));
                break;

            case BOSS_GOLEM:
                loot.add(new Item(Item.ItemType.LARGE_HEALTH_POTION, 2));
                loot.add(new Item(Item.ItemType.GOLD_COINS, 5));
                loot.add(new Item(Item.ItemType.ANCIENT_RELIC));
                if (chance(50) && floor > 5)
                    loot.add(new Item(Item.ItemType.CRYSTAL_BLADE));
                if (chance(50))
                    loot.add(new Item(Item.ItemType.CRYSTAL_ARMOR));
                loot.add(new Item(Item.ItemType.GOLD_KEY));
                loot.add(new Item(Item.ItemType.STAMINA_ELIXIR));
                loot.add(new Item(Item.ItemType.MANA_CRYSTAL));
                break;
        }

        return loot;
    }

    /**
     * Generate loot from opening a chest.
     */
    public static List<Item> getChestLoot(int floor) {
        List<Item> loot = new ArrayList<>();

        // Always some gold
        loot.add(new Item(Item.ItemType.GOLD_COINS, 1 + RANDOM.nextInt(3)));

        // Random additional items
        float roll = RANDOM.nextFloat();
        if (roll < 0.20f) {
            loot.add(new Item(Item.ItemType.HEALTH_POTION));
        } else if (roll < 0.35f) {
            loot.add(new Item(Item.ItemType.FOOD_RATION));
        } else if (roll < 0.45f) {
            loot.add(new Item(Item.ItemType.LARGE_HEALTH_POTION));
        } else if (roll < 0.52f) {
            loot.add(new Item(Item.ItemType.IRON_SWORD));
        } else if (roll < 0.58f) {
            loot.add(new Item(Item.ItemType.CHAIN_MAIL));
        } else if (roll < 0.65f) {
            loot.add(new Item(Item.ItemType.GEMSTONE));
        } else if (roll < 0.70f) {
            loot.add(new Item(Item.ItemType.BRONZE_KEY));
        } else if (roll < 0.75f) {
            loot.add(new Item(Item.ItemType.TORCH, 2));
        } else if (roll < 0.80f) {
            loot.add(new Item(Item.ItemType.ANTIDOTE));
        } else if (roll < 0.85f) {
            loot.add(new Item(Item.ItemType.POISON_VIAL));
        } else if (roll < 0.90f) {
            loot.add(new Item(Item.ItemType.SMOKE_BOMB));
        } else if (roll < 0.94f) {
            if (floor <= 3) {
                loot.add(new Item(Item.ItemType.FIRE_AXE));
            } else if (floor <= 7) {
                loot.add(new Item(Item.ItemType.GREEN_BLADE));
            } else {
                if (chance(50))
                    loot.add(new Item(Item.ItemType.CRYSTAL_BLADE));
                else
                    loot.add(new Item(Item.ItemType.CRYSTAL_ARMOR));
            }
        } else if (roll < 0.98f) {
            loot.add(new Item(Item.ItemType.MANA_CRYSTAL));
        } else {
            if (chance(50))
                loot.add(new Item(Item.ItemType.FIRE_RUNE));
            else
                loot.add(new Item(Item.ItemType.FROST_RUNE));
        }
        
        return loot;
    }

    /**
     * Generate enemy loot with LUCKY skill bonus (+25% probability on all drops).
     */
    public static List<Item> getEnemyDrop(Enemy.EnemyType type, int floor, boolean lucky) {
        List<Item> loot = getEnemyDrop(type, floor);
        if (lucky) {
            if (chance(25)) loot.add(new Item(Item.ItemType.HEALTH_POTION));
            if (chance(20)) loot.add(new Item(Item.ItemType.GOLD_COINS));
            if (chance(10) && floor > 4) loot.add(new Item(Item.ItemType.ANTIDOTE));
        }
        return loot;
    }

    /**
     * Generate chest loot with LUCKY skill bonus.
     */
    public static List<Item> getChestLoot(int floor, boolean lucky) {
        List<Item> loot = getChestLoot(floor);
        if (lucky) {
            loot.add(new Item(Item.ItemType.GOLD_COINS));
            if (chance(40)) loot.add(new Item(Item.ItemType.HEALTH_POTION));
        }
        return loot;
    }

    private static boolean chance(int percent) {
        return RANDOM.nextInt(100) < percent;
    }
}
