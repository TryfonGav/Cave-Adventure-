package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.caveadventure.entity.Companion;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;

/**
 * Save/load game state to a JSON-like text file.
 */
public class SaveManager {

    private static final String SAVE_FILE = "cave_adventure_save.dat";

    /**
     * Save current game state.
     */
    public static void saveGame(Player player, Companion companion, int floor, int enemiesKilled) {
        StringBuilder sb = new StringBuilder();
        sb.append("floor=").append(floor).append("\n");
        sb.append("health=").append(player.getHealth()).append("\n");
        sb.append("maxHealth=").append(player.getMaxHealth()).append("\n");
        sb.append("hunger=").append(player.getHunger()).append("\n");
        sb.append("level=").append(player.getLevel()).append("\n");
        sb.append("xp=").append(player.getXP()).append("\n");
        sb.append("xpNext=").append(player.getXPToNextLevel()).append("\n");
        sb.append("stamina=").append(player.getStamina()).append("\n");
        sb.append("enemiesKilled=").append(enemiesKilled).append("\n");
        sb.append("poisoned=").append(player.isPoisoned()).append("\n");
        sb.append("torch=").append(player.getTorchDuration()).append("\n");

        // Inventory
        Inventory inv = player.getInventory();
        sb.append("inventorySize=").append(inv.getSize()).append("\n");
        for (int i = 0; i < inv.getSize(); i++) {
            Item item = inv.getItem(i);
            sb.append("item=").append(item.getType().name())
                    .append(",").append(item.getQuantity()).append("\n");
        }

        // Equipment
        if (inv.getEquippedWeapon() != null) {
            sb.append("weapon=").append(inv.getEquippedWeapon().getType().name()).append("\n");
        }
        if (inv.getEquippedArmor() != null) {
            sb.append("armor=").append(inv.getEquippedArmor().getType().name()).append("\n");
        }

        if (companion != null) {
            sb.append("companionType=").append(companion.getPetType().name()).append("\n");
            sb.append("companionHealth=").append(companion.getHealth()).append("\n");
        }

        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            file.writeString(sb.toString(), false);
        } catch (Exception e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }

    /**
     * Load game state. Returns null if no save exists.
     */
    public static SaveData loadGame() {
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            if (!file.exists())
                return null;

            String content = file.readString();
            String[] lines = content.split("\n");

            SaveData data = new SaveData();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length < 2)
                    continue;

                String key = parts[0];
                String value = parts[1];

                switch (key) {
                    case "floor":
                        data.floor = Integer.parseInt(value);
                        break;
                    case "health":
                        data.health = Integer.parseInt(value);
                        break;
                    case "maxHealth":
                        data.maxHealth = Integer.parseInt(value);
                        break;
                    case "hunger":
                        data.hunger = Integer.parseInt(value);
                        break;
                    case "level":
                        data.level = Integer.parseInt(value);
                        break;
                    case "xp":
                        data.xp = Integer.parseInt(value);
                        break;
                    case "xpNext":
                        data.xpNext = Integer.parseInt(value);
                        break;
                    case "stamina":
                        data.stamina = Float.parseFloat(value);
                        break;
                    case "enemiesKilled":
                        data.enemiesKilled = Integer.parseInt(value);
                        break;
                    case "poisoned":
                        data.poisoned = Boolean.parseBoolean(value);
                        break;
                    case "torch":
                        data.torchDuration = Float.parseFloat(value);
                        break;
                    case "item":
                        String[] itemParts = value.split(",");
                        try {
                            Item.ItemType type = Item.ItemType.valueOf(itemParts[0]);
                            int qty = itemParts.length > 1 ? Integer.parseInt(itemParts[1]) : 1;
                            data.items.add(new Item(type, qty));
                        } catch (Exception ignored) {
                        }
                        break;
                    case "weapon":
                        try {
                            data.equippedWeapon = Item.ItemType.valueOf(value);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "armor":
                        try {
                            data.equippedArmor = Item.ItemType.valueOf(value);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "companionType":
                        try {
                            data.companionType = Companion.PetType.valueOf(value);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "companionHealth":
                        data.companionHealth = Integer.parseInt(value);
                        break;
                }
            }

            return data;
        } catch (Exception e) {
            System.err.println("Failed to load: " + e.getMessage());
            return null;
        }
    }

    public static boolean hasSave() {
        return Gdx.files.local(SAVE_FILE).exists();
    }

    public static void deleteSave() {
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            if (file.exists())
                file.delete();
        } catch (Exception ignored) {
        }
    }

    /**
     * Container for loaded save data.
     */
    public static class SaveData {
        public int floor = 1;
        public int health = 100;
        public int maxHealth = 100;
        public int hunger = 100;
        public int level = 1;
        public int xp = 0;
        public int xpNext = 100;
        public float stamina = 100f;
        public int enemiesKilled = 0;
        public boolean poisoned = false;
        public float torchDuration = 30f;
        public java.util.List<Item> items = new java.util.ArrayList<>();
        public Item.ItemType equippedWeapon = null;
        public Item.ItemType equippedArmor = null;
        public Companion.PetType companionType = null;
        public int companionHealth = -1;
    }
}
