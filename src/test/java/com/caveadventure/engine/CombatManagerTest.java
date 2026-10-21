package com.caveadventure.engine;

import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Tile;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CombatManagerTest {

    /** All-floor map, too small for any tile to be more than 40 steps from the player. */
    private static GameMap smallOpenMap(int size) {
        Tile[][] tiles = new Tile[size][size];
        for (Tile[] col : tiles)
            Arrays.fill(col, Tile.FLOOR);
        return new GameMap(tiles, size, size);
    }

    @Test
    void fledEnemyIsMovedAwayAndStaysThere() {
        GameMap map = smallOpenMap(10);
        CombatManager combat = new CombatManager(map, new Random(1));
        Player player = new Player(0, 0);
        Enemy enemy = new Enemy(3, 3, Enemy.EnemyType.SLIME);
        enemy.updateAI(player, map, 0.01f); // in detection range: starts chasing
        assertTrue(enemy.isMoving(), "precondition: a move is in progress");

        combat.relocateEnemy(enemy, player);

        assertEquals(9, enemy.getGridX());
        assertEquals(9, enemy.getGridY());
        assertFalse(enemy.isMoving());
        assertEquals(Enemy.AIState.IDLE, enemy.getAIState());
        enemy.update(1f);
        assertEquals(9, enemy.getGridX());
        assertEquals(9, enemy.getGridY());
    }
}
