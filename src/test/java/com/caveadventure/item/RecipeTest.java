package com.caveadventure.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {
    @Test
    void craftConsumesIngredientsAndAddsResult() {
        Inventory inventory = new Inventory();
        inventory.addItem(new Item(Item.ItemType.CRYSTAL_SHARD, 4));
        inventory.addItem(new Item(Item.ItemType.IRON_SWORD));
        Recipe recipe = CraftingManager.defaultRecipes().stream()
                .filter(r -> r.getId().equals("crystal_blade"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.canCraft(inventory));
        assertTrue(recipe.craft(inventory));
        assertEquals(0, inventory.count(Item.ItemType.CRYSTAL_SHARD));
        assertEquals(1, inventory.count(Item.ItemType.CRYSTAL_BLADE));
    }

    @Test
    void craftFailsWhenIngredientsAreMissing() {
        Inventory inventory = new Inventory();
        Recipe recipe = CraftingManager.defaultRecipes().get(0);

        assertFalse(recipe.canCraft(inventory));
        assertFalse(recipe.craft(inventory));
    }
}
