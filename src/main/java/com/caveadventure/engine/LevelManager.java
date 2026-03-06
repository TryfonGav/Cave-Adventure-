package com.caveadventure.engine;

import com.caveadventure.entity.Player;
import com.caveadventure.entity.Shopkeeper;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;
import com.caveadventure.world.CaveGenerator;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Tile;

import java.util.*;

/**
 * Manages multi-level cave progression. Each floor gets harder with more
 * enemies, traps, and features an optional shopkeeper.
 */
public class LevelManager {

    private int currentFloor;
    private GameMap currentMap;
    private CombatManager combatManager;
    private Shopkeeper shopkeeper;

    private static final int BASE_ENEMIES = 12;
    private static final int ENEMIES_PER_FLOOR = 4;
    private static final int MAX_FLOORS = 10;

    private final Random random = new Random();

    public LevelManager() {
        this.currentFloor = 1;
    }

    /**
     * Generate a new floor. Returns the spawn position {x, y}.
     */
    public int[] generateFloor(int floor) {
        this.currentFloor = floor;

        int width = 80 + (floor - 1) * 5;
        int height = 60 + (floor - 1) * 3;
        float wallDensity = 0.43f + floor * 0.005f;

        CaveGenerator generator = new CaveGenerator(width, height, wallDensity, 5);
        Tile[][] tiles = generator.generate();
        this.currentMap = new GameMap(tiles, width, height);

        // Setup combat
        this.combatManager = new CombatManager(currentMap);
        int enemyCount = BASE_ENEMIES + (floor - 1) * ENEMIES_PER_FLOOR;
        combatManager.spawnEnemies(enemyCount);

        // Spawn boss on every 3rd floor
        if (floor % 3 == 0) {
            int[] bossPos = findDistantFloorTile(tiles, width, height);
            if (bossPos != null) {
                combatManager.spawnBoss(bossPos[0], bossPos[1]);
            }
        }

        // Place locked doors
        if (floor >= 2)
            placeLocks(tiles, width, height);

        // Place traps (more on deeper floors)
        placeTraps(tiles, width, height, floor);

        // Spawn shopkeeper on every 2nd floor
        shopkeeper = null;
        if (floor % 2 == 0) {
            spawnShopkeeper(tiles, width, height);
        }

        return findSpawnPoint(tiles, width, height);
    }

    private void placeTraps(Tile[][] tiles, int width, int height, int floor) {
        int trapCount = 3 + floor * 2;
        int placed = 0;
        List<int[]> candidates = new ArrayList<>();

        for (int x = 5; x < width - 5; x++) {
            for (int y = 5; y < height - 5; y++) {
                if (tiles[x][y] == Tile.FLOOR) {
                    candidates.add(new int[] { x, y });
                }
            }
        }
        Collections.shuffle(candidates, random);

        for (int[] pos : candidates) {
            if (placed >= trapCount)
                break;
            // Don't place traps near other traps or special tiles
            boolean safe = true;
            for (int dx = -1; dx <= 1 && safe; dx++) {
                for (int dy = -1; dy <= 1 && safe; dy++) {
                    Tile neighbor = tiles[pos[0] + dx][pos[1] + dy];
                    if (neighbor != Tile.FLOOR && neighbor != Tile.WALL && neighbor != Tile.WALL_DARK) {
                        safe = false;
                    }
                }
            }
            if (safe) {
                // 40% chance of visible spikes, 30% hidden, 30% arrow trap
                float roll = random.nextFloat();
                if (roll < 0.4f)
                    tiles[pos[0]][pos[1]] = Tile.TRAP_SPIKES;
                else if (roll < 0.7f)
                    tiles[pos[0]][pos[1]] = Tile.TRAP_HIDDEN;
                else
                    tiles[pos[0]][pos[1]] = Tile.TRAP_ARROW;
                placed++;
            }
        }
    }

    private void spawnShopkeeper(Tile[][] tiles, int width, int height) {
        // Find a floor tile in a more central area
        List<int[]> candidates = new ArrayList<>();
        int cx = width / 2;
        int cy = height / 2;
        for (int x = cx - 15; x < cx + 15; x++) {
            for (int y = cy - 10; y < cy + 10; y++) {
                if (x > 2 && x < width - 2 && y > 2 && y < height - 2 && tiles[x][y] == Tile.FLOOR) {
                    candidates.add(new int[] { x, y });
                }
            }
        }
        if (!candidates.isEmpty()) {
            int[] pos = candidates.get(random.nextInt(candidates.size()));
            shopkeeper = new Shopkeeper(pos[0], pos[1]);
            tiles[pos[0]][pos[1]] = Tile.SHOP_FLOOR;
        }
    }

    /**
     * Check if player stepped on a trap. Returns damage dealt (0 = no trap).
     */
    public int checkTraps(Player player) {
        int px = player.getGridX();
        int py = player.getGridY();
        Tile tile = currentMap.getTile(px, py);

        if (tile == Tile.TRAP_SPIKES || tile == Tile.TRAP_HIDDEN) {
            int dmg = 8 + currentFloor * 3;
            player.takeDamage(dmg);
            currentMap.setTile(px, py, Tile.FLOOR); // Trap is used up
            return dmg;
        }
        if (tile == Tile.TRAP_ARROW) {
            int dmg = 5 + currentFloor * 2;
            player.takeDamage(dmg);
            if (random.nextFloat() < 0.3f)
                player.applyPoison(3f);
            currentMap.setTile(px, py, Tile.FLOOR);
            return dmg;
        }
        return 0;
    }

    /**
     * Check if the player is adjacent to a shopkeeper.
     */
    public boolean isNearShopkeeper(Player player) {
        if (shopkeeper == null)
            return false;
        int dx = Math.abs(player.getGridX() - shopkeeper.getGridX());
        int dy = Math.abs(player.getGridY() - shopkeeper.getGridY());
        return Math.max(dx, dy) <= 1;
    }

    private int[] findSpawnPoint(Tile[][] tiles, int width, int height) {
        int cx = width / 2;
        int cy = height / 2;
        for (int r = 0; r < Math.max(width, height); r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    int x = cx + dx;
                    int y = cy + dy;
                    if (x >= 0 && x < width && y >= 0 && y < height && tiles[x][y] == Tile.FLOOR) {
                        return new int[] { x, y };
                    }
                }
            }
        }
        return new int[] { 2, 2 };
    }

    private int[] findDistantFloorTile(Tile[][] tiles, int width, int height) {
        for (int r = 0; r < 20; r++) {
            int x = width - 5 - random.nextInt(10);
            int y = height - 5 - random.nextInt(10);
            if (x > 0 && y > 0 && x < width && y < height && tiles[x][y] == Tile.FLOOR) {
                return new int[] { x, y };
            }
        }
        return null;
    }

    private void placeLocks(Tile[][] tiles, int width, int height) {
        int lockCount = 0;
        for (int x = 3; x < width - 3 && lockCount < 2; x++) {
            for (int y = 3; y < height - 3 && lockCount < 2; y++) {
                if (tiles[x][y] == Tile.FLOOR) {
                    boolean vert = tiles[x - 1][y].isSolid() && tiles[x + 1][y].isSolid()
                            && !tiles[x][y - 1].isSolid() && !tiles[x][y + 1].isSolid();
                    boolean horiz = tiles[x][y - 1].isSolid() && tiles[x][y + 1].isSolid()
                            && !tiles[x - 1][y].isSolid() && !tiles[x + 1][y].isSolid();
                    if ((vert || horiz) && random.nextFloat() < 0.08f) {
                        tiles[x][y] = Tile.DOOR_LOCKED;
                        lockCount++;
                    }
                }
            }
        }
    }

    public boolean isOnStairs(Player player) {
        return currentMap.getTile(player.getGridX(), player.getGridY()) == Tile.STAIRS_DOWN;
    }

    public boolean tryUnlockDoor(Player player) {
        int px = player.getGridX() + player.getFacing().dx;
        int py = player.getGridY() + player.getFacing().dy;
        if (currentMap.getTile(px, py) == Tile.DOOR_LOCKED) {
            Inventory inv = player.getInventory();
            if (inv.useKey(Item.ItemType.GOLD_KEY) || inv.useKey(Item.ItemType.SILVER_KEY)
                    || inv.useKey(Item.ItemType.BRONZE_KEY)) {
                currentMap.setTile(px, py, Tile.DOOR_OPEN);
                return true;
            }
        }
        return false;
    }

    public boolean isFinalFloor() {
        return currentFloor >= MAX_FLOORS;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public GameMap getCurrentMap() {
        return currentMap;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public int getMaxFloors() {
        return MAX_FLOORS;
    }

    public Shopkeeper getShopkeeper() {
        return shopkeeper;
    }
}
