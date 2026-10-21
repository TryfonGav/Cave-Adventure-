package com.caveadventure.engine;

import com.caveadventure.entity.Player;
import com.caveadventure.entity.Shopkeeper;
import com.caveadventure.entity.Enemy;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;
import com.caveadventure.world.CaveGenerator;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Biome;
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
    private Biome currentBiome;
    private int finalExitX = -1;
    private int finalExitY = -1;

    private static final int BASE_ENEMIES = 12;
    private static final int ENEMIES_PER_FLOOR = 4;
    private static final int MAX_FLOORS = 14;
    // Trap damage tuning - base values and per-floor increments
    private static final int BASE_TRAP_SPIKES = 8;
    private static final int TRAP_SPIKES_PER_FLOOR = 3;
    private static final int BASE_TRAP_ARROW = 5;
    private static final int TRAP_ARROW_PER_FLOOR = 2;
    private static final float TRAP_ARROW_POISON_CHANCE = 0.3f;
    /** Stairs go on a random tile among this share of reachable floor tiles farthest from spawn. */
    private static final float STAIRS_FARTHEST_FRACTION = 0.10f;
    /**
     * No enemy spawns within this many tiles (Manhattan) of the player's spawn:
     * enemy detection range 8 + patrol radius 5 + 1, so none starts a chase on arrival.
     */
    static final int ENEMY_SPAWN_CLEARANCE = 14;
    private static final int MAX_LOCKED_DOORS = 2;
    private static final float LOCK_CHANCE = 0.08f;

    private final Random random;

    public LevelManager() {
        this(new Random());
    }

    /**
     * Test seam: level manager whose generation is driven by the given {@link Random}.
     * Seeds for the cave generator and combat manager are drawn from it, so a seeded
     * {@code Random} reproduces the same floors.
     */
    LevelManager(Random random) {
        this.random = random;
        this.currentFloor = 1;
    }

    /**
     * Generate a new floor. Returns the spawn position {x, y}.
     */
    public int[] generateFloor(int floor) {
        return generateFloor(floor, Biome.forFloor(floor), Difficulty.getCurrent());
    }

    public int[] generateFloor(int floor, Biome biome, Difficulty difficulty) {
        this.currentFloor = floor;
        this.currentBiome = biome == null ? Biome.forFloor(floor) : biome;
        if (difficulty != null)
            Difficulty.setCurrent(difficulty);

        if (floor == 10 || floor == MAX_FLOORS) {
            return generateFinalBossFloor();
        }

        int width = 80 + (floor - 1) * 5;
        int height = 60 + (floor - 1) * 3;
        float wallDensity = 0.43f + floor * 0.005f;

        CaveGenerator generator = new CaveGenerator(width, height, wallDensity, 5, currentBiome, random.nextLong());
        Tile[][] tiles = generator.generate();
        this.currentMap = new GameMap(tiles, width, height);
        int[] spawn = findSpawnPoint(tiles, width, height);

        // Stairs far from spawn, then locked doors that never cut them off
        int[] stairs = placeStairs(tiles, width, height, spawn);
        if (floor >= 2)
            placeLocks(tiles, width, height, spawn, stairs);
        int[][] stepsFromSpawn = currentMap.stepDistancesFrom(spawn[0], spawn[1]);

        // Setup combat, keeping the area around spawn clear
        this.combatManager = new CombatManager(currentMap, new Random(random.nextLong()));
        int enemyCount = enemyCountForFloor(floor);
        combatManager.spawnEnemies(enemyCount, floor, currentBiome, spawn[0], spawn[1], ENEMY_SPAWN_CLEARANCE);

        // Spawn boss on every 5th floor
        if (floor % 5 == 0) {
            int[] bossPos = findDistantFloorTile(tiles, width, height);
            if (bossPos != null) {
                combatManager.spawnBoss(bossPos[0], bossPos[1], floor >= 10
                        ? Enemy.EnemyType.BOSS_WYRM : Enemy.EnemyType.BOSS_GOLEM);
            }
        }

        // Place traps (more on deeper floors)
        placeTraps(tiles, width, height, floor, spawn);

        // Spawn shopkeeper on every 2nd floor
        shopkeeper = null;
        if (floor % 2 == 0) {
            spawnShopkeeper(tiles, width, height, stepsFromSpawn);
        }

        return spawn;
    }

    /** Enemies spawned on a regular floor (before the spawn-clearance filter). */
    static int enemyCountForFloor(int floor) {
        return BASE_ENEMIES + (floor - 1) * ENEMIES_PER_FLOOR;
    }

    private int[] generateFinalBossFloor() {
        int width = 31;
        int height = 41;
        Tile[][] tiles = new Tile[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = Tile.WALL_DARK;
            }
        }

        int centerX = width / 2;
        int spawnY = 4;
        int chestY = height / 2;
        int bossY = height - 6;
        int stairsY = height - 3;
        finalExitX = centerX;
        finalExitY = stairsY;

        // Main vertical corridor.
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int y = 1; y < height - 1; y++) {
                tiles[x][y] = Tile.FLOOR;
            }
        }

        // Player spawn chamber.
        for (int x = centerX - 4; x <= centerX + 4; x++) {
            for (int y = 1; y <= 7; y++) {
                tiles[x][y] = Tile.FLOOR;
            }
        }

        // Mid-floor chest chamber.
        for (int x = centerX - 6; x <= centerX + 6; x++) {
            for (int y = chestY - 1; y <= chestY + 1; y++) {
                tiles[x][y] = Tile.FLOOR;
            }
        }

        // Boss dais near the top with room behind for exit stairs.
        for (int x = centerX - 4; x <= centerX + 4; x++) {
            for (int y = bossY - 1; y <= height - 2; y++) {
                tiles[x][y] = Tile.FLOOR;
            }
        }

        // Decorative side notches so the room feels intentional, not procedural.
        tiles[centerX - 7][chestY] = Tile.FLOOR;
        tiles[centerX + 7][chestY] = Tile.FLOOR;

        // Sparse arena markers matching the requested layout.
        tiles[centerX - 6][chestY] = Tile.CHEST;
        tiles[centerX + 6][chestY] = Tile.CHEST;
        tiles[centerX][stairsY] = Tile.FLOOR;

        this.currentMap = new GameMap(tiles, width, height);
        this.combatManager = new CombatManager(currentMap, new Random(random.nextLong()));
        this.currentBiome = Biome.TOXIC_MIRE;
        this.combatManager.spawnBoss(centerX, bossY, Enemy.EnemyType.BOSS_WYRM);
        this.shopkeeper = null;

        return new int[] { centerX, spawnY };
    }

    public void unlockFinalBossExit() {
        if ((currentFloor == 10 || currentFloor == MAX_FLOORS) && finalExitX >= 0 && finalExitY >= 0) {
            currentMap.setTile(finalExitX, finalExitY, Tile.STAIRS_DOWN);
        }
    }

    private void placeTraps(Tile[][] tiles, int width, int height, int floor, int[] spawn) {
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
            if (pos[0] == spawn[0] && pos[1] == spawn[1])
                continue;
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

    private void spawnShopkeeper(Tile[][] tiles, int width, int height, int[][] stepsFromSpawn) {
        // Find a reachable floor tile (not the spawn tile) in a more central area
        List<int[]> candidates = new ArrayList<>();
        int cx = width / 2;
        int cy = height / 2;
        for (int x = cx - 15; x < cx + 15; x++) {
            for (int y = cy - 10; y < cy + 10; y++) {
                if (x > 2 && x < width - 2 && y > 2 && y < height - 2 && tiles[x][y] == Tile.FLOOR
                        && stepsFromSpawn[x][y] > 0) {
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
            int base = BASE_TRAP_SPIKES + currentFloor * TRAP_SPIKES_PER_FLOOR;
            int dmg = scaleTrapDamage(base);
            player.takeDamage(dmg);
            currentMap.setTile(px, py, Tile.FLOOR); // Trap is used up
            return dmg;
        }
        if (tile == Tile.TRAP_ARROW) {
            int base = BASE_TRAP_ARROW + currentFloor * TRAP_ARROW_PER_FLOOR;
            int dmg = scaleTrapDamage(base);
            player.takeDamage(dmg);
            if (random.nextFloat() < TRAP_ARROW_POISON_CHANCE)
                player.applyPoison(3f);
            currentMap.setTile(px, py, Tile.FLOOR);
            return dmg;
        }
        return 0;
    }

    public int checkHazards(Player player) {
        int px = player.getGridX();
        int py = player.getGridY();
        Tile tile = currentMap.getTile(px, py);
        int damage = tile.getHazardDamage();
        if (damage <= 0)
            return 0;
        int scaled = scaleTrapDamage(damage);
        player.takeTrueDamage(scaled); // terrain hazards ignore armor
        if (tile == Tile.TOXIC_MIST)
            player.applyPoison(2.5f);
        return scaled;
    }

    /**
     * Scales a trap's base damage by the current difficulty's trapDamageMultiplier.
     * Ensures damage is at least 1 after scaling.
     */
    private int scaleTrapDamage(int baseDamage) {
        float mult = Difficulty.getCurrent().trapDamageMultiplier;
        int scaled = Math.max(1, Math.round(baseDamage * mult));
        return scaled;
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

    /**
     * Put the stairs on a random tile among the {@link #STAIRS_FARTHEST_FRACTION} of
     * floor tiles farthest (in walking steps) from spawn. Returns its position, or
     * null if nothing else is reachable.
     */
    private int[] placeStairs(Tile[][] tiles, int width, int height, int[] spawn) {
        int[][] dist = currentMap.stepDistancesFrom(spawn[0], spawn[1]);
        List<int[]> reachable = new ArrayList<>();
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                if (tiles[x][y] == Tile.FLOOR && dist[x][y] > 0) {
                    reachable.add(new int[] { x, y, dist[x][y] });
                }
            }
        }
        if (reachable.isEmpty())
            return null;
        reachable.sort((a, b) -> Integer.compare(b[2], a[2]));
        int pool = Math.max(1, Math.round(reachable.size() * STAIRS_FARTHEST_FRACTION));
        int[] pos = reachable.get(random.nextInt(pool));
        tiles[pos[0]][pos[1]] = Tile.STAIRS_DOWN;
        return new int[] { pos[0], pos[1] };
    }

    /**
     * Lock up to {@link #MAX_LOCKED_DOORS} one-tile corridors. A door that would cut
     * the stairs off from spawn is reverted, so doors only guard optional pockets;
     * each kept door gets a chest in the pocket it seals, if the pocket has room.
     */
    private void placeLocks(Tile[][] tiles, int width, int height, int[] spawn, int[] stairs) {
        if (stairs == null)
            return;
        int lockCount = 0;
        for (int x = 3; x < width - 3 && lockCount < MAX_LOCKED_DOORS; x++) {
            for (int y = 3; y < height - 3 && lockCount < MAX_LOCKED_DOORS; y++) {
                if (tiles[x][y] == Tile.FLOOR && (x != spawn[0] || y != spawn[1])) {
                    boolean vert = tiles[x - 1][y].isSolid() && tiles[x + 1][y].isSolid()
                            && !tiles[x][y - 1].isSolid() && !tiles[x][y + 1].isSolid();
                    boolean horiz = tiles[x][y - 1].isSolid() && tiles[x][y + 1].isSolid()
                            && !tiles[x - 1][y].isSolid() && !tiles[x + 1][y].isSolid();
                    if ((vert || horiz) && random.nextFloat() < LOCK_CHANCE) {
                        tiles[x][y] = Tile.DOOR_LOCKED;
                        int[][] dist = currentMap.stepDistancesFrom(spawn[0], spawn[1]);
                        if (dist[stairs[0]][stairs[1]] < 0) {
                            tiles[x][y] = Tile.FLOOR; // would cut off the stairs
                        } else {
                            placeChestBehindDoor(tiles, dist, x, y);
                            lockCount++;
                        }
                    }
                }
            }
        }
    }

    /**
     * If the locked door at (doorX, doorY) seals off a pocket (tiles unreachable from
     * spawn per {@code distFromSpawn}), turn the pocket's floor tile farthest from the
     * door into a chest.
     */
    private void placeChestBehindDoor(Tile[][] tiles, int[][] distFromSpawn, int doorX, int doorY) {
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : dirs) {
            int sx = doorX + d[0], sy = doorY + d[1];
            if (!currentMap.isPassable(sx, sy) || distFromSpawn[sx][sy] >= 0)
                continue;
            int[][] pocket = currentMap.stepDistancesFrom(sx, sy);
            int[] best = null;
            int bestDist = -1;
            for (int x = 0; x < currentMap.getWidth(); x++) {
                for (int y = 0; y < currentMap.getHeight(); y++) {
                    if (pocket[x][y] > bestDist && tiles[x][y] == Tile.FLOOR) {
                        bestDist = pocket[x][y];
                        best = new int[] { x, y };
                    }
                }
            }
            if (best != null)
                tiles[best[0]][best[1]] = Tile.CHEST;
            return;
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
        return currentFloor == 10 || currentFloor >= MAX_FLOORS;
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

    public Biome getCurrentBiome() {
        return currentBiome;
    }
}
