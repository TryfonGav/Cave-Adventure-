package com.caveadventure.engine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.entity.*;
import com.caveadventure.item.*;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Tile;

import java.util.*;

/**
 * Manages enemies on the map and proximity-based battle encounters
 * (turn-based).
 */
public class CombatManager {

    private final List<Enemy> enemies;
    private final List<DamageNumber> damageNumbers;
    private final GameMap gameMap;
    private final Random random = new Random();

    public CombatManager(GameMap gameMap) {
        this.enemies = new ArrayList<>();
        this.damageNumbers = new ArrayList<>();
        this.gameMap = gameMap;
    }

    public void spawnEnemies(int count, int floor) {
        Tile[][] tiles = gameMap.getTiles();
        List<int[]> floorTiles = new ArrayList<>();

        for (int x = 3; x < gameMap.getWidth() - 3; x++) {
            for (int y = 3; y < gameMap.getHeight() - 3; y++) {
                if (tiles[x][y] == Tile.FLOOR) {
                    floorTiles.add(new int[] { x, y });
                }
            }
        }

        Collections.shuffle(floorTiles, random);

        for (int i = 0; i < Math.min(count, floorTiles.size()); i++) {
            int[] pos = floorTiles.get(i);
            Enemy.EnemyType type = pickEnemyType(floor);
            enemies.add(new Enemy(pos[0], pos[1], type));
        }
    }

    public void spawnBoss(int x, int y) {
        enemies.add(new Enemy(x, y, Enemy.EnemyType.BOSS_GOLEM));
    }

    private Enemy.EnemyType pickEnemyType(int floor) {
        float roll = random.nextFloat();
        if (floor > 5) {
            // After floor 5: rare early enemies, common late enemies
            if (roll < 0.10f)
                return Enemy.EnemyType.BAT;
            else if (roll < 0.18f)
                return Enemy.EnemyType.SLIME;
            else if (roll < 0.26f)
                return Enemy.EnemyType.CAVE_SPIDER;
            else if (roll < 0.36f)
                return Enemy.EnemyType.GOBLIN;
            else if (roll < 0.48f)
                return Enemy.EnemyType.SKELETON;
            else if (roll < 0.66f)
                return Enemy.EnemyType.NECROMANCER;
            else if (roll < 0.83f)
                return Enemy.EnemyType.SHADOW;
            else
                return Enemy.EnemyType.ICE_DRAKE;
        } else {
            if (roll < 0.25f)
                return Enemy.EnemyType.BAT;
            else if (roll < 0.45f)
                return Enemy.EnemyType.SLIME;
            else if (roll < 0.60f)
                return Enemy.EnemyType.CAVE_SPIDER;
            else if (roll < 0.80f)
                return Enemy.EnemyType.GOBLIN;
            else
                return Enemy.EnemyType.SKELETON;
        }
    }

    /**
     * Update enemies (no real-time combat — battles are turn-based encounters).
     */
    public void update(Player player, float delta) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            enemy.updateAI(player, gameMap, delta);
            enemy.update(delta);
            if (enemy.isDeathAnimationDone()) {
                it.remove();
            }
        }

        damageNumbers.removeIf(DamageNumber::isExpired);
        for (DamageNumber dn : damageNumbers) {
            dn.update(delta);
        }
    }

    /**
     * Check if an enemy is adjacent to the player (triggers battle encounter).
     * Returns the enemy to battle, or null if no enemy is nearby.
     */
    public Enemy checkBattleEncounter(Player player) {
        int px = player.getGridX();
        int py = player.getGridY();

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive())
                continue;
            int dist = Math.abs(enemy.getGridX() - px) + Math.abs(enemy.getGridY() - py);
            if (dist <= 1) {
                return enemy;
            }
        }
        return null;
    }

    /**
     * Remove a defeated enemy from the map.
     */
    public void removeEnemy(Enemy enemy) {
        enemies.remove(enemy);
    }

    /**
     * Handle opening chests.
     */
    public boolean tryOpenChest(Player player, int floor) {
        return tryOpenChest(player, floor, false);
    }

    public boolean tryOpenChest(Player player, int floor, boolean lucky) {
        int px = player.getGridX();
        int py = player.getGridY();
        Tile tile = gameMap.getTile(px, py);

        if (tile == Tile.CHEST) {
            List<Item> loot = LootTable.getChestLoot(floor, lucky);
            for (Item item : loot) {
                player.getInventory().addItem(item);
            }
            gameMap.setTile(px, py, Tile.FLOOR);

            damageNumbers.add(new DamageNumber(
                    player.getPixelX() + GameMap.TILE_SIZE / 2f,
                    player.getPixelY() + GameMap.TILE_SIZE + 10,
                    "Chest opened!",
                    new Color(1f, 0.85f, 0.2f, 1f)));
            return true;
        }
        return false;
    }

    public void renderEnemies(ShapeRenderer renderer) {
        for (Enemy enemy : enemies) {
            enemy.render(renderer);
        }
    }

    public void renderDamageNumbers(SpriteBatch batch, BitmapFont font) {
        for (DamageNumber dn : damageNumbers) {
            dn.render(batch, font);
        }
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public int getEnemyCount() {
        return (int) enemies.stream().filter(Entity::isAlive).count();
    }

    /**
     * Relocate an enemy to a random distant floor tile (used when player flees
     * battle).
     */
    public void relocateEnemy(Enemy enemy, Player player) {
        com.caveadventure.world.Tile[][] tiles = gameMap.getTiles();
        java.util.List<int[]> farTiles = new java.util.ArrayList<>();

        for (int x = 3; x < gameMap.getWidth() - 3; x++) {
            for (int y = 3; y < gameMap.getHeight() - 3; y++) {
                if (tiles[x][y] == com.caveadventure.world.Tile.FLOOR) {
                    int dist = Math.abs(x - player.getGridX()) + Math.abs(y - player.getGridY());
                    if (dist > 40) { // Only pick tiles far from the player
                        farTiles.add(new int[] { x, y });
                    }
                }
            }
        }

        if (!farTiles.isEmpty()) {
            int[] pos = farTiles.get(random.nextInt(farTiles.size()));
            enemy.setPosition(pos[0], pos[1]);
        }
    }
}
