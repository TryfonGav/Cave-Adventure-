package com.caveadventure.engine;

import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.world.Biome;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Tile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated floors (P1-T13): every procedural floor, 20 fixed seeds each.
 * Floors 10 and 14 are hand-built boss arenas and are not covered here.
 */
class LevelGenTest {

    private static final int SEEDS = 20;

    static IntStream proceduralFloors() {
        return IntStream.rangeClosed(1, 13).filter(f -> f != 10);
    }

    /** One generated floor plus its spawn point and walking distances from it. */
    private static final class Floor {
        final LevelManager levels;
        final GameMap map;
        final int[] spawn;
        final int[][] steps;

        Floor(int floor, long seed) {
            levels = new LevelManager(new Random(seed));
            spawn = levels.generateFloor(floor, Biome.forFloor(floor), null);
            map = levels.getCurrentMap();
            steps = map.stepDistancesFrom(spawn[0], spawn[1]); // locked doors closed
        }

        int[] stairs() {
            int[] found = null;
            for (int x = 0; x < map.getWidth(); x++)
                for (int y = 0; y < map.getHeight(); y++)
                    if (map.getTile(x, y) == Tile.STAIRS_DOWN) {
                        assertNull(found, "more than one staircase");
                        found = new int[] { x, y };
                    }
            assertNotNull(found, "no staircase");
            return found;
        }

        int farthestReachable() {
            int max = 0;
            for (int[] col : steps)
                for (int d : col)
                    max = Math.max(max, d);
            return max;
        }
    }

    private static long seed(int floor, int i) {
        return floor * 1000L + i;
    }

    @ParameterizedTest
    @MethodSource("proceduralFloors")
    void stairsAreReachableWithEveryLockedDoorClosed(int floor) {
        for (int i = 0; i < SEEDS; i++) {
            Floor f = new Floor(floor, seed(floor, i));
            int[] stairs = f.stairs();
            assertTrue(f.steps[stairs[0]][stairs[1]] > 0,
                    "floor " + floor + " seed " + seed(floor, i) + ": stairs need a key");
        }
    }

    @ParameterizedTest
    @MethodSource("proceduralFloors")
    void stairsAreInTheFarHalfOfTheFloor(int floor) {
        for (int i = 0; i < SEEDS; i++) {
            Floor f = new Floor(floor, seed(floor, i));
            int[] stairs = f.stairs();
            int stairsDist = f.steps[stairs[0]][stairs[1]];
            assertTrue(stairsDist * 2 >= f.farthestReachable(),
                    "floor " + floor + " seed " + seed(floor, i) + ": stairs " + stairsDist
                            + " steps away, farthest tile " + f.farthestReachable());
        }
    }

    @ParameterizedTest
    @MethodSource("proceduralFloors")
    void noEnemyNearSpawn(int floor) {
        for (int i = 0; i < SEEDS; i++) {
            Floor f = new Floor(floor, seed(floor, i));
            for (Enemy e : f.levels.getCombatManager().getEnemies()) {
                int d = Math.abs(e.getGridX() - f.spawn[0]) + Math.abs(e.getGridY() - f.spawn[1]);
                assertTrue(d > LevelManager.ENEMY_SPAWN_CLEARANCE,
                        "floor " + floor + " seed " + seed(floor, i) + ": " + e.getType()
                                + " spawned " + d + " tiles from the player");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("proceduralFloors")
    void everyTrapTileIsArmedAndSpawnIsSafe(int floor) {
        for (int i = 0; i < SEEDS; i++) {
            Floor f = new Floor(floor, seed(floor, i));
            assertEquals(Tile.FLOOR, f.map.getTile(f.spawn[0], f.spawn[1]));
            for (int x = 0; x < f.map.getWidth(); x++) {
                for (int y = 0; y < f.map.getHeight(); y++) {
                    Tile tile = f.map.getTile(x, y);
                    if (!tile.name().startsWith("TRAP_"))
                        continue;
                    Player player = new Player(x, y);
                    assertTrue(f.levels.checkTraps(player) > 0,
                            "floor " + floor + ": " + tile + " at " + x + "," + y + " does nothing");
                }
            }
        }
    }
}
