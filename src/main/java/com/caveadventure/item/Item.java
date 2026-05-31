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
                GREEN_BLADE("Green Blade", Category.WEAPON, "Shiny Green Blade",
                                new Color(0.0f, 1.00f, 0.0f, 1f), 18, 0, 0),
                CRYSTAL_BLADE("Crystal Blade", Category.WEAPON, "Gleams with inner light",
                                new Color(0.5f, 0.8f, 1f, 1f), 22, 0, 0),
                FIRE_AXE("Fire Axe", Category.WEAPON, "Burns on contact",
                                new Color(0.9f, 0.4f, 0.1f, 1f), 28, 0, 0),
                SHADOW_BLADE("Shadow Blade", Category.WEAPON, "Saps enemy strength",
                                new Color(0.2f, 0.2f, 0.25f, 1f), 20, 0, 0),

                // Armor (DEF bonus)
                LEATHER_ARMOR("Leather Armor", Category.ARMOR, "Basic protection",
                                new Color(0.55f, 0.35f, 0.15f, 1f), 0, 5, 0),
                CHAIN_MAIL("Chain Mail", Category.ARMOR, "Interlocking metal rings",
                                new Color(0.6f, 0.6f, 0.65f, 1f), 0, 8, 0),
                PLATE_ARMOR("Plate Armor", Category.ARMOR, "Heavy full plate protection",
                                new Color(0.65f, 0.65f, 0.7f, 1f), 0, 15, 0),
                CRYSTAL_ARMOR("Crystal Armor", Category.ARMOR, "Shimmers with protection",
                                new Color(0.4f, 0.7f, 0.9f, 1f), 0, 22, 0),
                FROST_MAIL("Frost Mail", Category.ARMOR, "Insulated armor for frozen vaults",
                                new Color(0.55f, 0.82f, 1f, 1f), 0, 26, 0),

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
                                new Color(0.7f, 0.4f, 1f, 1f), 0, 0, 0),

                // Scrolls / Runes (single-use specials)
                SMOKE_BOMB("Smoke Bomb", Category.SCROLL, "Guarantees escape from any battle",
                                new Color(0.4f, 0.4f, 0.4f, 1f), 0, 0, 0),
                STAMINA_ELIXIR("Stamina Elixir", Category.SCROLL, "Fully restores stamina",
                                new Color(0.3f, 0.7f, 1f, 1f), 0, 0, 0),
                POISON_VIAL("Poison Vial", Category.SCROLL, "Throw at enemy to poison them",
                                new Color(0.2f, 0.85f, 0.3f, 1f), 0, 0, 0),
                ICE_SHARD("Ice Shard", Category.SCROLL, "Deals 20 dmg + stuns enemy for 1 turn",
                                new Color(0.5f, 0.8f, 1f, 1f), 0, 0, 20),
                MANA_CRYSTAL("Mana Crystal", Category.SCROLL, "Reduce stamina costs by 30% this battle",
                                new Color(0.6f, 0.3f, 1f, 1f), 0, 0, 0),
                FIRE_RUNE("Fire Rune", Category.SCROLL, "Imbues your weapon with fire for 3 turns",
                                new Color(1f, 0.45f, 0.1f, 1f), 10, 0, 0),
                SHADOW_RUNE("Shadow Rune", Category.SCROLL, "+20% crit chance for 3 turns",
                                new Color(0.3f, 0.15f, 0.4f, 1f), 0, 0, 0),
                FROST_RUNE("Frost Rune", Category.SCROLL, "Slows enemy — skip their next turn",
                                new Color(0.6f, 0.85f, 1f, 1f), 0, 0, 0),

                CRYSTAL_SHARD("Crystal Shard", Category.MATERIAL, "Crafting material from crystal caves",
                                new Color(0.55f, 0.85f, 1f, 1f), 0, 0, 0),
                GLOWCAP_SPORE("Glowcap Spore", Category.MATERIAL, "Softly glowing mushroom spores",
                                new Color(0.45f, 1f, 0.35f, 1f), 0, 0, 0),
                EMBER_CORE("Ember Core", Category.MATERIAL, "A coal-hot crafting core",
                                new Color(1f, 0.35f, 0.08f, 1f), 0, 0, 0),
                SHADOW_ESSENCE("Shadow Essence", Category.MATERIAL, "A vial of condensed gloom",
                                new Color(0.18f, 0.12f, 0.26f, 1f), 0, 0, 0),
                FROST_ORE("Frost Ore", Category.MATERIAL, "Blue-white ore that never warms",
                                new Color(0.58f, 0.86f, 1f, 1f), 0, 0, 0),
                TOXIN_SAC("Toxin Sac", Category.MATERIAL, "Mire toxin used by alchemists",
                                new Color(0.36f, 0.95f, 0.22f, 1f), 0, 0, 0),
                SHADOW_CHARM("Shadow Charm", Category.ACCESSORY, "+10% critical chance",
                                new Color(0.45f, 0.28f, 0.62f, 1f), 0, 0, 0,
                                EquipmentSlot.ACCESSORY, 0f, 0f, 0.10f),
                LANTERN_CHARM("Lantern Charm", Category.ACCESSORY, "Adds light radius",
                                new Color(1f, 0.76f, 0.26f, 1f), 0, 0, 0,
                                EquipmentSlot.ACCESSORY, 0f, 1.4f, 0f),
                FROSTSTEP_BOOTS("Froststep Boots", Category.BOOTS, "Faster movement on rough ground",
                                new Color(0.62f, 0.88f, 1f, 1f), 0, 2, 0,
                                EquipmentSlot.BOOTS, 0.16f, 0f, 0f),
                TOXIN_KIT("Toxin Kit", Category.CONSUMABLE, "Cures poison and restores 20 HP",
                                new Color(0.35f, 0.95f, 0.25f, 1f), 0, 0, 20);

                public final String displayName;
                public final Category category;
                public final String description;
                public final Color color;
                public final int attackBonus;
                public final int defenseBonus;
                public final int restoreAmount;
                public final EquipmentSlot equipmentSlot;
                public final float speedBonus;
                public final float lightBonus;
                public final float critBonus;

                ItemType(String displayName, Category category, String description, Color color,
                                int attackBonus, int defenseBonus, int restoreAmount) {
                        this(displayName, category, description, color, attackBonus, defenseBonus, restoreAmount,
                                        null, 0f, 0f, 0f);
                }

                ItemType(String displayName, Category category, String description, Color color,
                                int attackBonus, int defenseBonus, int restoreAmount,
                                EquipmentSlot equipmentSlot, float speedBonus, float lightBonus, float critBonus) {
                        this.displayName = displayName;
                        this.category = category;
                        this.description = description;
                        this.color = color;
                        this.attackBonus = attackBonus;
                        this.defenseBonus = defenseBonus;
                        this.restoreAmount = restoreAmount;
                        this.equipmentSlot = equipmentSlot != null ? equipmentSlot : defaultSlotFor(category);
                        this.speedBonus = speedBonus;
                        this.lightBonus = lightBonus;
                        this.critBonus = critBonus;
                }
        }

        public enum Category {
                WEAPON, ARMOR, ACCESSORY, BOOTS, CONSUMABLE, KEY, TREASURE, SCROLL, MATERIAL
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
                this.quantity = Math.max(0, q);
        }

        public void addQuantity(int amount) {
                this.quantity = Math.max(0, this.quantity + amount);
        }

        public void removeQuantity(int amount) {
                this.quantity = Math.max(0, this.quantity - amount);
        }

        public boolean isStackable() {
                return type.category == Category.CONSUMABLE ||
                                type.category == Category.KEY ||
                                type.category == Category.TREASURE ||
                                type.category == Category.SCROLL ||
                                type.category == Category.MATERIAL;
        }

        public boolean isEquippable() {
                return type.equipmentSlot != EquipmentSlot.NONE;
        }

        public boolean isUsable() {
                return type.category == Category.CONSUMABLE || type.category == Category.SCROLL;
        }

        /** Returns true if this scroll must be used inside a battle (can't be used in exploration). */
        public boolean isBattleOnly() {
                return type == ItemType.POISON_VIAL || type == ItemType.ICE_SHARD
                        || type == ItemType.FIRE_RUNE || type == ItemType.SHADOW_RUNE
                        || type == ItemType.FROST_RUNE || type == ItemType.SMOKE_BOMB;
        }

        private static EquipmentSlot defaultSlotFor(Category category) {
                if (category == Category.WEAPON)
                        return EquipmentSlot.WEAPON;
                if (category == Category.ARMOR)
                        return EquipmentSlot.ARMOR;
                if (category == Category.ACCESSORY)
                        return EquipmentSlot.ACCESSORY;
                if (category == Category.BOOTS)
                        return EquipmentSlot.BOOTS;
                return EquipmentSlot.NONE;
        }
}
