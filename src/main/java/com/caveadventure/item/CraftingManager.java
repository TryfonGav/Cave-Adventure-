package com.caveadventure.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CraftingManager {
    private final List<Recipe> recipes;

    public CraftingManager() {
        this.recipes = defaultRecipes();
    }

    public boolean craft(int index, Inventory inventory) {
        if (index < 0 || index >= recipes.size()) {
            return false;
        }
        return recipes.get(index).craft(inventory);
    }

    public List<Recipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public static List<Recipe> defaultRecipes() {
        List<Recipe> list = new ArrayList<>();
        list.add(Recipe.of("crystal_blade", "Crystal Blade", new Item(Item.ItemType.CRYSTAL_BLADE),
                Item.ItemType.CRYSTAL_SHARD, 4,
                Item.ItemType.IRON_SWORD, 1));
        list.add(Recipe.of("ember_axe", "Fire Axe", new Item(Item.ItemType.FIRE_AXE),
                Item.ItemType.EMBER_CORE, 3,
                Item.ItemType.STEEL_SWORD, 1));
        list.add(Recipe.of("shadow_charm", "Shadow Charm", new Item(Item.ItemType.SHADOW_CHARM),
                Item.ItemType.SHADOW_ESSENCE, 3,
                Item.ItemType.ANCIENT_RELIC, 1));
        list.add(Recipe.of("frost_boots", "Froststep Boots", new Item(Item.ItemType.FROSTSTEP_BOOTS),
                Item.ItemType.FROST_ORE, 3,
                Item.ItemType.GLOWCAP_SPORE, 2));
        list.add(Recipe.of("toxin_kit", "Toxin Kit", new Item(Item.ItemType.TOXIN_KIT),
                Item.ItemType.TOXIN_SAC, 2,
                Item.ItemType.ANTIDOTE, 1));
        return list;
    }
}
