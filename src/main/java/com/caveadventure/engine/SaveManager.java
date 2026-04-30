package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.caveadventure.entity.CharacterAppearance;
import com.caveadventure.entity.Companion;
import com.caveadventure.entity.Player;
import com.caveadventure.engine.SkillTree.Skill;
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
    public static void saveGame(Player player, Companion companion, int floor, int enemiesKilled,
            boolean finalBossDefeated, java.util.Collection<Skill> unlockedSkills) {
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
        sb.append("finalBossDefeated=").append(finalBossDefeated).append("\n");
        sb.append("poisonRemaining=").append(player.getPoisonRemaining()).append("\n");
        sb.append("torch=").append(player.getTorchDuration()).append("\n");
        appendAppearance(sb, player.getAppearance());

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
            sb.append("companionLove=").append(companion.getLove()).append("\n");
            sb.append("companionHunger=").append(companion.getHunger()).append("\n");
            sb.append("companionHappiness=").append(companion.getHappiness()).append("\n");
            sb.append("companionFatigue=").append(companion.getFatigue()).append("\n");
            sb.append("companionPetCooldown=").append(companion.getPetCooldownTimer()).append("\n");
        }

        if (unlockedSkills != null) {
            for (Skill skill : unlockedSkills) {
                sb.append("skillUnlocked=").append(skill.name()).append("\n");
            }
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
                    case "finalBossDefeated":
                        data.finalBossDefeated = Boolean.parseBoolean(value);
                        break;
                    case "poisonRemaining":
                        data.poisonRemaining = Float.parseFloat(value);
                        break;
                    case "torch":
                        data.torchDuration = Float.parseFloat(value);
                        break;
                    case "characterName":
                        data.characterAppearance.setName(value);
                        break;
                    case "characterTunic":
                        data.characterAppearance.setTunicColor(parseColor(value, data.characterAppearance.getTunicColor()));
                        break;
                    case "characterSkin":
                        data.characterAppearance.setSkinColor(parseColor(value, data.characterAppearance.getSkinColor()));
                        break;
                    case "characterHair":
                        data.characterAppearance.setHairColor(parseColor(value, data.characterAppearance.getHairColor()));
                        break;
                    case "characterPants":
                        data.characterAppearance.setPantsColor(parseColor(value, data.characterAppearance.getPantsColor()));
                        break;
                    case "characterBoots":
                        data.characterAppearance.setBootColor(parseColor(value, data.characterAppearance.getBootColor()));
                        break;
                    case "characterCape":
                        data.characterAppearance.setCapeColor(parseColor(value, data.characterAppearance.getCapeColor()));
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
                    case "companionLove":
                        data.companionLove = Float.parseFloat(value);
                        break;
                    case "companionHunger":
                        data.companionHunger = Float.parseFloat(value);
                        break;
                    case "companionHappiness":
                        data.companionHappiness = Float.parseFloat(value);
                        break;
                    case "companionFatigue":
                        data.companionFatigue = Float.parseFloat(value);
                        break;
                    case "companionPetCooldown":
                        data.companionPetCooldown = Float.parseFloat(value);
                        break;
                    case "skillUnlocked":
                        try {
                            data.unlockedSkills.add(Skill.valueOf(value));
                        } catch (Exception ignored) {
                        }
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

    private static void appendAppearance(StringBuilder sb, CharacterAppearance appearance) {
        CharacterAppearance safeAppearance = appearance == null ? CharacterAppearance.defaultAppearance() : appearance;
        sb.append("characterName=").append(safeAppearance.getName()).append("\n");
        appendColor(sb, "characterTunic", safeAppearance.getTunicColor());
        appendColor(sb, "characterSkin", safeAppearance.getSkinColor());
        appendColor(sb, "characterHair", safeAppearance.getHairColor());
        appendColor(sb, "characterPants", safeAppearance.getPantsColor());
        appendColor(sb, "characterBoots", safeAppearance.getBootColor());
        appendColor(sb, "characterCape", safeAppearance.getCapeColor());
    }

    private static void appendColor(StringBuilder sb, String key, Color color) {
        sb.append(key).append("=").append(CharacterAppearance.colorToString(color)).append("\n");
    }

    private static Color parseColor(String value, Color fallback) {
        return CharacterAppearance.colorFromString(value, fallback);
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
        public boolean finalBossDefeated = false;
        public float poisonRemaining = 0f;
        public float torchDuration = 30f;
        public java.util.List<Item> items = new java.util.ArrayList<>();
        public Item.ItemType equippedWeapon = null;
        public Item.ItemType equippedArmor = null;
        public Companion.PetType companionType = null;
        public int companionHealth = -1;
        public float companionLove = 60f;
        public float companionHunger = 35f;
        public float companionHappiness = 55f;
        public float companionFatigue = 0f;
        public float companionPetCooldown = 0f;
        public java.util.List<Skill> unlockedSkills = new java.util.ArrayList<>();
        public CharacterAppearance characterAppearance = CharacterAppearance.defaultAppearance();
    }
}
