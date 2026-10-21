package com.caveadventure.item;

import com.caveadventure.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    @ParameterizedTest
    @EnumSource(value = Item.ItemType.class, names = { "BREAD", "MEAT", "FOOD_RATION", "COOKED_MEAT" })
    void foodRestoresHungerAndIsConsumed(Item.ItemType food) {
        Player player = new Player(0, 0);
        player.modifyHunger(-90);
        Inventory inventory = player.getInventory();
        inventory.addItem(new Item(food));

        assertTrue(inventory.useItem(0, player));

        assertEquals(10 + food.restoreAmount, player.getHunger());
        assertEquals(0, inventory.getSize());
    }

    @Test
    void addItemFailsWhenAllSlotsAreFull() {
        Inventory inventory = new Inventory();
        for (int i = 0; i < 20; i++)
            assertTrue(inventory.addItem(new Item(Item.ItemType.RUSTY_SWORD)));

        assertFalse(inventory.addItem(new Item(Item.ItemType.IRON_SWORD)));
        assertEquals(20, inventory.getSize());
    }
}
