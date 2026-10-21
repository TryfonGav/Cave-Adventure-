package com.caveadventure.ui;

import com.badlogic.gdx.Input.Keys;
import com.caveadventure.TestKeys;
import com.caveadventure.engine.InputHandler;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShopTest {

    private static final int GOLD = 50; // more than any shop price
    private static final int INVENTORY_SLOTS = 20;

    private final InputHandler input = new InputHandler();

    /** Player with a gold stack plus {@code fillers} non-stackable swords the shop never sells. */
    private static Player playerWith(int fillers) {
        Player player = new Player(0, 0);
        player.getInventory().addItem(new Item(Item.ItemType.GOLD_COINS, GOLD));
        for (int i = 0; i < fillers; i++)
            player.getInventory().addItem(new Item(Item.ItemType.RUSTY_SWORD));
        return player;
    }

    private static int gold(Player player) {
        return player.getInventory().count(Item.ItemType.GOLD_COINS);
    }

    private boolean buySelected(ShopUI shop, Player player) {
        shop.open();
        boolean[] purchased = new boolean[1];
        TestKeys.tap(input, Keys.ENTER, () -> purchased[0] = shop.update(input, player, 0.016f));
        return purchased[0];
    }

    @Test
    void fullInventoryPurchaseKeepsTheGold() {
        Player player = playerWith(INVENTORY_SLOTS - 1);
        ShopUI shop = new ShopUI(null);

        assertFalse(buySelected(shop, player));

        assertEquals(GOLD, gold(player));
        assertEquals(INVENTORY_SLOTS, player.getInventory().getSize());
    }

    @Test
    void purchaseWithRoomChargesAndAddsTheItem() {
        Player player = playerWith(0);
        ShopUI shop = new ShopUI(null);

        assertTrue(buySelected(shop, player));

        assertTrue(gold(player) < GOLD);
        assertEquals(2, player.getInventory().getSize());
    }
}
