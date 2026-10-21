package com.caveadventure.entity;

import com.badlogic.gdx.Input.Keys;
import com.caveadventure.TestKeys;
import com.caveadventure.engine.InputHandler;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Tile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStepTest {

    private static final int MAP_SIZE = 5;

    private static GameMap openMap() {
        Tile[][] tiles = new Tile[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++)
            for (int y = 0; y < MAP_SIZE; y++)
                tiles[x][y] = Tile.FLOOR;
        return new GameMap(tiles, MAP_SIZE, MAP_SIZE);
    }

    @Test
    void oneCompletedMoveSignalsOneStep() {
        Player player = new Player(1, 1);
        InputHandler input = new InputHandler();
        GameMap map = openMap();

        TestKeys.press(input, Keys.RIGHT);
        player.handleInput(input, map, 0.01f);
        TestKeys.release(input, Keys.RIGHT);
        assertTrue(player.isMoving());

        player.update(0.01f); // mid-move
        assertFalse(player.consumeStepCompleted());

        player.update(1f); // move finishes
        assertEquals(2, player.getGridX());
        assertTrue(player.consumeStepCompleted());
        assertFalse(player.consumeStepCompleted());
    }

    @Test
    void teleportCancelsTheMoveWithoutAStep() {
        Player player = new Player(1, 1);
        InputHandler input = new InputHandler();

        TestKeys.press(input, Keys.RIGHT);
        player.handleInput(input, openMap(), 0.01f);
        player.update(0.01f);
        player.teleport(3, 3);
        player.update(1f);

        assertFalse(player.isMoving());
        assertEquals(3, player.getGridX());
        assertEquals(3, player.getGridY());
        assertEquals(3 * GameMap.TILE_SIZE, player.getPixelX());
        assertFalse(player.consumeStepCompleted());
    }
}
