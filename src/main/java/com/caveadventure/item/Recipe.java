package com.caveadventure.item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Recipe {
    private final String id;
    private final String name;
    private final Map<Item.ItemType, Integer> ingredients;
    private final Item result;

    public Recipe(String id, String name, Map<Item.ItemType, Integer> ingredients, Item result) {
        this.id = id;
        this.name = name;
        this.ingredients = Collections.unmodifiableMap(new EnumMap<>(ingredients));
        this.result = result;
    }

    public static Recipe of(String id, String name, Item result, Object... ingredientPairs) {
        Map<Item.ItemType, Integer> ingredients = new EnumMap<>(Item.ItemType.class);
        for (int i = 0; i + 1 < ingredientPairs.length; i += 2) {
            ingredients.put((Item.ItemType) ingredientPairs[i], (Integer) ingredientPairs[i + 1]);
        }
        return new Recipe(id, name, ingredients, result);
    }

    public boolean canCraft(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (Map.Entry<Item.ItemType, Integer> entry : ingredients.entrySet()) {
            if (inventory.count(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean craft(Inventory inventory) {
        if (!canCraft(inventory)) {
            return false;
        }
        for (Map.Entry<Item.ItemType, Integer> entry : ingredients.entrySet()) {
            inventory.removeItems(entry.getKey(), entry.getValue());
        }
        return inventory.addItem(new Item(result.getType(), result.getQuantity()));
    }

    public String ingredientText() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Item.ItemType, Integer> entry : ingredients.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append("x ").append(entry.getKey().displayName);
        }
        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<Item.ItemType, Integer> getIngredients() {
        return ingredients;
    }

    public Item getResult() {
        return result;
    }
}
