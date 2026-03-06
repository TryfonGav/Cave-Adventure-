package com.caveadventure.item;

import com.badlogic.gdx.graphics.Color;

/**
 * Represents an item in the game world or inventory.
 */
public class Item {

        public enum ItemType {
                // Weapons (ATK bonus)
                RUSTY_SWORD("Rusty Sword", Category.WEAPON, "A worn but serviceable blade",
                                new Color(0.6f, 0.55f, 0.5f, 1f), 5, 0, 0),
                IRON_SWORD("Iron Sword", Category.WEAPON, "A reliable iron blade",
                                new Color(0.7f, 0.72f, 0.75f, 1f), 8, 0, 0),
                STEEL_SWORD("Steel Sword", Category.WEAPON, "Forged steel, razor sharp",
                                new Color(0.8f, 0.82f, 0.85f, 1f), 15, 0, 0),
                CRYSTAL_BLADE("Crystal Blade", Category.WEAPON, "Gleams with inner light",
                                new Color(0.5f, 0.8f, 1f, 1f), 22, 0, 0),
                FIRE_AXE("Fire Axe", Category.WEAPON, "Burns on contact",
                                new Color(0.9f, 0.4f, 0.1f, 1f), 28, 0, 0),

                // Armor (DEF bonus)
                LEATHER_ARMOR("Leather Armor", Category.ARMOR, "Basic protection",
                                new Color(0.55f, 0.35f, 0.15f, 1f), 0, 5, 0),
                CHAIN_MAIL("Chain Mail", Category.ARMOR, "Interlocking metal rings",
                                new Color(0.6f, 0.6f, 0.65f, 1f), 0, 8, 0),
                PLATE_ARMOR("Plate Armor", Category.ARMOR, "Heavy full plate protection",
                                new Color(0.65f, 0.65f, 0.7f, 1f), 0, 15, 0),
                CRYSTAL_ARMOR("Crystal Armor", Category.ARMOR, "Shimmers with protection",
                                new Color(0.4f, 0.7f, 0.9f, 1f), 0, 22, 0),

                // Consumables
                HEALTH_POTION("Health Potion", Category.CONSUMABLE, "Restores 30 HP",
                                new Color(0.9f, 0.15f, 0.15f, 1f), 0, 0, 30),
                LARGE_HEALTH_POTION("Large HP Potion", Category.CONSUMABLE, "Restores 60 HP",
                                new Color(0.95f, 0.1f, 0.2f, 1f), 0, 0, 60),
                ELIXIR("Elixir", Category.CONSUMABLE, "Full HP restore",
                                new Color(1f, 0.3f, 0.5f, 1f), 0, 0, 999),
                FOOD_RATION("Food Ration", Category.CONSUMABLE, "Restores 40 hunger",
                                new Color(0.7f, 0.5f, 0.2f, 1f), 0, 0, 40),
                BREAD("Bread", Category.CONSUMABLE, "Restores 25 hunger",
                                new Color(0.8f, 0.65f, 0.3f, 1f), 0, 0, 25),
                COOKED_MEAT("Cooked Meat", Category.CONSUMABLE, "Restores 35 hunger",
                                new Color(0.65f, 0.3f, 0.15f, 1f), 0, 0, 35),
                MEAT("Meat", Category.CONSUMABLE, "Restores 50 hunger",
                                new Color(0.7f, 0.25f, 0.12f, 1f), 0, 0, 50),
                ANTIDOTE("Antidote", Category.CONSUMABLE, "Cures poison",
                                new Color(0.3f, 0.9f, 0.4f, 1f), 0, 0, 0),
                TORCH("Torch", Category.CONSUMABLE, "Increases light radius",
                                new Color(1f, 0.7f, 0.2f, 1f), 0, 0, 0),

                // Keys
                BRONZE_KEY("Bronze Key", Category.KEY, "Opens bronze locks",
                                new Color(0.75f, 0.55f, 0.2f, 1f), 0, 0, 0),
                SILVER_KEY("Silver Key", Category.KEY, "Opens silver locks",
                                new Color(0.8f, 0.8f, 0.85f, 1f), 0, 0, 0),
                GOLD_KEY("Gold Key", Category.KEY, "Opens the deepest doors",
                                new Color(1f, 0.85f, 0.2f, 1f), 0, 0, 0),

                // Treasures (used as currency in shops)
                GOLD_COINS("Gold Coins", Category.TREASURE, "Shiny gold coins",
                                new Color(1f, 0.85f, 0.1f, 1f), 0, 0, 0),
                GOLD_NUGGET("Gold Nugget", Category.TREASURE, "Worth 1 gold",
                                new Color(1f, 0.8f, 0.15f, 1f), 0, 0, 0),
                RUBY("Ruby", Category.TREASURE, "Worth 3 gold",
                                new Color(0.9f, 0.1f, 0.2f, 1f), 0, 0, 0),
                DIAMOND("Diamond", Category.TREASURE, "Worth 5 gold",
                                new Color(0.7f, 0.9f, 1f, 1f), 0, 0, 0),
                GEMSTONE("Gemstone", Category.TREASURE, "A precious gem",
                                new Color(0.3f, 0.9f, 0.6f, 1f), 0, 0, 0),
                ANCIENT_RELIC("Ancient Relic", Category.TREASURE, "A mysterious artifact",
                                new Color(0.7f, 0.4f, 1f, 1f), 0, 0, 0);

                public final String displayName;
                public final Category category;
                public final String description;
                public final Color color;
                public final int attackBonus;
                public final int defenseBonus;
                public final int restoreAmount;

                ItemType(String displayName, Category category, String description, Color color,
                                int attackBonus, int defenseBonus, int restoreAmount) {
                        this.displayName = displayName;
                        this.category = category;
                        this.description = description;
                        this.color = color;
                        this.attackBonus = attackBonus;
                        this.defenseBonus = defenseBonus;
                        this.restoreAmount = restoreAmount;
                }
        }

        public enum Category {
                WEAPON, ARMOR, CONSUMABLE, KEY, TREASURE
        }

        private final ItemType type;
        private int quantity;

        public Item(ItemType type) {
                this(type, 1);
        }

        public Item(ItemType type, int quantity) {
                this.type = type;
                this.quantity = quantity;
        }

        public ItemType getType() {
                return type;
        }

        public int getQuantity() {
                return quantity;
        }

        public void setQuantity(int q) {
                this.quantity = q;
        }

        public void addQuantity(int amount) {
                this.quantity += amount;
        }

        public void removeQuantity(int amount) {
                this.quantity = Math.max(0, this.quantity - amount);
        }

        public boolean isStackable() {
                return type.category == Category.CONSUMABLE ||
                                type.category == Category.KEY ||
                                type.category == Category.TREASURE;
        }

        public boolean isEquippable() {
                return type.category == Category.WEAPON || type.category == Category.ARMOR;
        }

        public boolean isUsable() {
                return type.category == Category.CONSUMABLE;
        }
}
