package com.caveadventure.entity;

import com.caveadventure.item.Item;
import com.caveadventure.world.GameMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerFloorTransitionTest {

    @Test
    void progressCarriesOverAndQuarterHpIsHealed() {
        Player player = new Player(1, 1);
        player.addXP(40);
        player.getInventory().addItem(new Item(Item.ItemType.IRON_SWORD));
        player.getInventory().equipItem(0);
        player.setHealth(50);
        player.setStamina(30f);

        player.enterNewFloor(7, 9);

        assertEquals(7, player.getGridX());
        assertEquals(9, player.getGridY());
        assertEquals(1, player.getLevel());
        assertEquals(40, player.getXP());
        assertEquals(Item.ItemType.IRON_SWORD, player.getInventory().getEquippedWeapon().getType());
        assertEquals(75, player.getHealth()); // 50 + 25% of 100
        assertEquals(30f, player.getStamina());
    }

    @Test
    void floorHealIsCappedAtMaxHp() {
        Player player = new Player(1, 1);
        player.setHealth(90);

        player.enterNewFloor(2, 2);

        assertEquals(player.getMaxHealth(), player.getHealth());
    }

    @Test
    void berserkerStacksResetPerFloor() {
        Player player = new Player(1, 1);
        player.addBerserkerKill();
        player.addBerserkerKill();

        player.enterNewFloor(2, 2);

        assertEquals(0, player.getBerserkerKillCount());
    }

    @Test
    void companionSnapsBesideThePlayerWithoutGliding() {
        Player player = new Player(1, 1);
        Companion companion = new Companion(1, 1, Companion.PetType.CAVE_WOLF);

        player.enterNewFloor(20, 30);
        companion.snapTo(20, 30);
        float x = companion.getPixelX(), y = companion.getPixelY();
        companion.followPlayer(player.getPixelX(), player.getPixelY(), 0.016f);

        assertEquals(x, companion.getPixelX(), 1e-3);
        assertEquals(y, companion.getPixelY(), 1e-3);
        assertTrue(Math.abs(x - player.getPixelX()) < GameMap.TILE_SIZE); // beside the player, not on the old floor
        assertTrue(Math.abs(y - player.getPixelY()) < GameMap.TILE_SIZE);
    }
}
