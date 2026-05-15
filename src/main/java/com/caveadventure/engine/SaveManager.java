package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.graphics.Color;
import com.caveadventure.entity.CharacterAppearance;
import com.caveadventure.entity.Companion;
import com.caveadventure.entity.Player;
import com.caveadventure.engine.SkillTree.Skill;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Save/load game state to a JSON-like text file.
 */
public class SaveManager {

    private static final String SAVE_FILE = "cave_adventure_save.dat";
    private static final Logger LOGGER = Logger.getLogger(SaveManager.class.getName());

    private static final int MIN_FLOOR = 1;
    private static final int MAX_FLOOR = 10;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 200;
    private static final int MIN_HEALTH = 0;
    private static final int MAX_HEALTH = 9999;
    private static final int MIN_HUNGER = 0;
    private static final int MAX_HUNGER = 100;
    private static final int MAX_INVENTORY_SLOTS = 20;
    private static final int MIN_ITEM_QUANTITY = 1;
    private static final int MAX_ITEM_QUANTITY = 999;
    private static final int MIN_COMPANION_STAT = 0;
    private static final int MAX_COMPANION_STAT = 100;
    private static final int MIN_COMPANION_HEALTH = -1;
    private static final int MAX_COMPANION_HEALTH = 9999;
    private static final float MIN_STAMINA = 0f;
    private static final float MAX_STAMINA = 1000f;
    private static final float MIN_DURATION = 0f;
    private static final float MAX_DURATION = 86400f;
    private static final float MIN_COOLDOWN = 0f;
    private static final float MAX_COOLDOWN = 3600f;
    private static final int MAX_CHARACTER_NAME_LENGTH = 32;

    /**
     * Save current game state.
     */
    public static void saveGame(Player player, Companion companion, int floor, int enemiesKilled,
            boolean finalBossDefeated, java.util.Collection<Skill> unlockedSkills) {
        if (player == null) {
            LOGGER.warning("saveGame aborted: player is null.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("floor=").append(clampInt(floor, MIN_FLOOR, MAX_FLOOR)).append("\n");
        sb.append("health=").append(clampInt(player.getHealth(), MIN_HEALTH, MAX_HEALTH)).append("\n");
        sb.append("maxHealth=").append(clampInt(player.getMaxHealth(), 1, MAX_HEALTH)).append("\n");
        sb.append("hunger=").append(clampInt(player.getHunger(), MIN_HUNGER, MAX_HUNGER)).append("\n");
        sb.append("level=").append(clampInt(player.getLevel(), MIN_LEVEL, MAX_LEVEL)).append("\n");
        sb.append("xp=").append(Math.max(0, player.getXP())).append("\n");
        sb.append("xpNext=").append(Math.max(1, player.getXPToNextLevel())).append("\n");
        sb.append("stamina=").append(clampFloat(player.getStamina(), MIN_STAMINA, MAX_STAMINA)).append("\n");
        sb.append("enemiesKilled=").append(Math.max(0, enemiesKilled)).append("\n");
        sb.append("finalBossDefeated=").append(finalBossDefeated).append("\n");
        sb.append("poisonRemaining=").append(clampFloat(player.getPoisonRemaining(), MIN_DURATION, MAX_DURATION)).append("\n");
        sb.append("torch=").append(clampFloat(player.getTorchDuration(), MIN_DURATION, MAX_DURATION)).append("\n");
        appendAppearance(sb, player.getAppearance());

        // Inventory
        Inventory inv = player.getInventory();
        int inventorySize = inv != null ? inv.getSize() : 0;
        sb.append("inventorySize=").append(clampInt(inventorySize, 0, MAX_INVENTORY_SLOTS)).append("\n");
        for (int i = 0; i < inv.getSize(); i++) {
            Item item = inv.getItem(i);
            if (item == null || item.getType() == null) {
                LOGGER.warning("Skipping null inventory item during save at index " + i + ".");
                continue;
            }
            sb.append("item=").append(item.getType().name())
                    .append(",").append(clampInt(item.getQuantity(), MIN_ITEM_QUANTITY, MAX_ITEM_QUANTITY)).append("\n");
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
                if (skill == null) {
                    LOGGER.warning("Skipping null unlocked skill during save.");
                    continue;
                }
                sb.append("skillUnlocked=").append(skill.name()).append("\n");
            }
        }

        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            file.writeString(sb.toString(), false);
        } catch (GdxRuntimeException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save game to " + SAVE_FILE, ex);
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
            int declaredInventorySize = -1;

            for (int lineNo = 0; lineNo < lines.length; lineNo++) {
                String line = lines[lineNo];
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length < 2) {
                    LOGGER.warning("Ignoring malformed save line " + (lineNo + 1) + ": " + line);
                    continue;
                }

                String key = parts[0];
                String value = parts[1];

                switch (key) {
                    case "floor":
                        data.floor = parseIntBounded("floor", value, MIN_FLOOR, MAX_FLOOR, data.floor);
                        break;
                    case "health":
                        data.health = parseIntBounded("health", value, MIN_HEALTH, MAX_HEALTH, data.health);
                        break;
                    case "maxHealth":
                        data.maxHealth = parseIntBounded("maxHealth", value, 1, MAX_HEALTH, data.maxHealth);
                        break;
                    case "hunger":
                        data.hunger = parseIntBounded("hunger", value, MIN_HUNGER, MAX_HUNGER, data.hunger);
                        break;
                    case "level":
                        data.level = parseIntBounded("level", value, MIN_LEVEL, MAX_LEVEL, data.level);
                        break;
                    case "xp":
                        data.xp = parseIntBounded("xp", value, 0, Integer.MAX_VALUE, data.xp);
                        break;
                    case "xpNext":
                        data.xpNext = parseIntBounded("xpNext", value, 1, Integer.MAX_VALUE, data.xpNext);
                        break;
                    case "stamina":
                        data.stamina = parseFloatBounded("stamina", value, MIN_STAMINA, MAX_STAMINA, data.stamina);
                        break;
                    case "enemiesKilled":
                        data.enemiesKilled = parseIntBounded("enemiesKilled", value, 0, Integer.MAX_VALUE,
                                data.enemiesKilled);
                        break;
                    case "finalBossDefeated":
                        data.finalBossDefeated = parseBoolean("finalBossDefeated", value, data.finalBossDefeated);
                        break;
                    case "poisonRemaining":
                        data.poisonRemaining = parseFloatBounded("poisonRemaining", value, MIN_DURATION, MAX_DURATION,
                                data.poisonRemaining);
                        break;
                    case "torch":
                        data.torchDuration = parseFloatBounded("torch", value, MIN_DURATION, MAX_DURATION,
                                data.torchDuration);
                        break;
                    case "inventorySize":
                        declaredInventorySize = parseIntBounded("inventorySize", value, 0, MAX_INVENTORY_SLOTS,
                                declaredInventorySize < 0 ? 0 : declaredInventorySize);
                        break;
                    case "characterName":
                        data.characterAppearance.setName(sanitizeCharacterName(value));
                        break;
                    case "characterTunic":
                        data.characterAppearance
                                .setTunicColor(parseColor("characterTunic", value, data.characterAppearance.getTunicColor()));
                        break;
                    case "characterSkin":
                        data.characterAppearance
                                .setSkinColor(parseColor("characterSkin", value, data.characterAppearance.getSkinColor()));
                        break;
                    case "characterHair":
                        data.characterAppearance
                                .setHairColor(parseColor("characterHair", value, data.characterAppearance.getHairColor()));
                        break;
                    case "characterPants":
                        data.characterAppearance
                                .setPantsColor(parseColor("characterPants", value, data.characterAppearance.getPantsColor()));
                        break;
                    case "characterBoots":
                        data.characterAppearance
                                .setBootColor(parseColor("characterBoots", value, data.characterAppearance.getBootColor()));
                        break;
                    case "characterCape":
                        data.characterAppearance
                                .setCapeColor(parseColor("characterCape", value, data.characterAppearance.getCapeColor()));
                        break;
                    case "item":
                        parseItemEntry(data, value);
                        break;
                    case "weapon":
                        data.equippedWeapon = parseItemType("weapon", value, Item.Category.WEAPON);
                        break;
                    case "armor":
                        data.equippedArmor = parseItemType("armor", value, Item.Category.ARMOR);
                        break;
                    case "companionType":
                        data.companionType = parseCompanionType("companionType", value, data.companionType);
                        break;
                    case "companionHealth":
                        data.companionHealth = parseIntBounded("companionHealth", value,
                                MIN_COMPANION_HEALTH, MAX_COMPANION_HEALTH, data.companionHealth);
                        break;
                    case "companionLove":
                        data.companionLove = parseFloatBounded("companionLove", value,
                                MIN_COMPANION_STAT, MAX_COMPANION_STAT, data.companionLove);
                        break;
                    case "companionHunger":
                        data.companionHunger = parseFloatBounded("companionHunger", value,
                                MIN_COMPANION_STAT, MAX_COMPANION_STAT, data.companionHunger);
                        break;
                    case "companionHappiness":
                        data.companionHappiness = parseFloatBounded("companionHappiness", value,
                                MIN_COMPANION_STAT, MAX_COMPANION_STAT, data.companionHappiness);
                        break;
                    case "companionFatigue":
                        data.companionFatigue = parseFloatBounded("companionFatigue", value,
                                MIN_COMPANION_STAT, MAX_COMPANION_STAT, data.companionFatigue);
                        break;
                    case "companionPetCooldown":
                        data.companionPetCooldown = parseFloatBounded("companionPetCooldown", value,
                                MIN_COOLDOWN, MAX_COOLDOWN, data.companionPetCooldown);
                        break;
                    case "skillUnlocked":
                        parseSkillEntry(data, value);
                        break;
                    default:
                        LOGGER.fine("Ignoring unknown save key: " + key);
                        break;
                }
            }

            if (declaredInventorySize >= 0 && data.items.size() > declaredInventorySize) {
                LOGGER.warning("Save inventory exceeds declared size. Trimming to declared capacity.");
                while (data.items.size() > declaredInventorySize) {
                    data.items.remove(data.items.size() - 1);
                }
            }

            return sanitize(data);
        } catch (GdxRuntimeException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load save from " + SAVE_FILE, ex);
            return null;
        }
    }

    public static boolean hasSave() {
        try {
            return Gdx.files.local(SAVE_FILE).exists();
        } catch (GdxRuntimeException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Failed checking save existence for " + SAVE_FILE, ex);
            return false;
        }
    }

    public static void deleteSave() {
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            if (file.exists())
                file.delete();
        } catch (GdxRuntimeException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Failed deleting save file " + SAVE_FILE, ex);
        }
    }

    private static void appendAppearance(StringBuilder sb, CharacterAppearance appearance) {
        CharacterAppearance safeAppearance = appearance == null ? CharacterAppearance.defaultAppearance() : appearance;
        sb.append("characterName=").append(sanitizeCharacterName(safeAppearance.getName())).append("\n");
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

    private static Color parseColor(String key, String value, Color fallback) {
        try {
            return CharacterAppearance.colorFromString(value, fallback);
        } catch (IllegalArgumentException | NullPointerException ex) {
            LOGGER.log(Level.WARNING, "Invalid color for key '" + key + "': " + value + ". Using fallback.", ex);
            return fallback;
        }
    }

    private static int parseIntBounded(String key, String value, int min, int max, int fallback) {
        if (value == null) {
            LOGGER.warning("Null integer value for key '" + key + "'. Using fallback " + fallback + ".");
            return fallback;
        }

        String trimmed = value.trim();
        try {
            int parsed = Integer.parseInt(trimmed);
            if (parsed < min || parsed > max) {
                LOGGER.warning("Out-of-range integer for key '" + key + "': " + parsed
                        + " (allowed " + min + ".." + max + "). Clamping.");
            }
            return clampInt(parsed, min, max);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING,
                    "Invalid integer for key '" + key + "': '" + value + "'. Using fallback " + fallback + ".", ex);
            return fallback;
        }
    }

    private static float parseFloatBounded(String key, String value, float min, float max, float fallback) {
        if (value == null) {
            LOGGER.warning("Null float value for key '" + key + "'. Using fallback " + fallback + ".");
            return fallback;
        }

        String trimmed = value.trim();
        try {
            float parsed = Float.parseFloat(trimmed);
            if (!Float.isFinite(parsed)) {
                LOGGER.warning("Non-finite float for key '" + key + "'. Using fallback " + fallback + ".");
                return fallback;
            }
            if (parsed < min || parsed > max) {
                LOGGER.warning("Out-of-range float for key '" + key + "': " + parsed
                        + " (allowed " + min + ".." + max + "). Clamping.");
            }
            return clampFloat(parsed, min, max);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING,
                    "Invalid float for key '" + key + "': '" + value + "'. Using fallback " + fallback + ".", ex);
            return fallback;
        }
    }

    private static boolean parseBoolean(String key, String value, boolean fallback) {
        if (value == null) {
            LOGGER.warning("Null boolean for key '" + key + "'. Using fallback " + fallback + ".");
            return fallback;
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed))
            return true;
        if ("false".equalsIgnoreCase(trimmed))
            return false;
        LOGGER.warning("Invalid boolean for key '" + key + "': '" + value + "'. Using fallback " + fallback + ".");
        return fallback;
    }

    private static Companion.PetType parseCompanionType(String key, String value, Companion.PetType fallback) {
        if (value == null || value.isBlank()) {
            LOGGER.warning("Blank companion type for key '" + key + "'. Using fallback.");
            return fallback;
        }
        try {
            return Companion.PetType.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Invalid companion type for key '" + key + "': " + value + ".", ex);
            return fallback;
        }
    }

    private static Item.ItemType parseItemType(String key, String value, Item.Category expectedCategory) {
        if (value == null || value.isBlank()) {
            LOGGER.warning("Blank item type for key '" + key + "'.");
            return null;
        }
        try {
            Item.ItemType parsed = Item.ItemType.valueOf(value.trim());
            if (parsed.category != expectedCategory) {
                LOGGER.warning("Item type '" + parsed.name()
                        + "' does not match expected category for key '" + key + "'. Ignoring.");
                return null;
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Invalid item type for key '" + key + "': " + value + ".", ex);
            return null;
        }
    }

    private static void parseItemEntry(SaveData data, String value) {
        if (value == null || value.isBlank()) {
            LOGGER.warning("Ignoring blank item entry in save.");
            return;
        }

        String[] itemParts = value.split(",", 2);
        String typeRaw = itemParts[0].trim();
        if (typeRaw.isEmpty()) {
            LOGGER.warning("Ignoring item with empty type in save entry: " + value);
            return;
        }

        Item.ItemType type;
        try {
            type = Item.ItemType.valueOf(typeRaw);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Ignoring unknown item type in save entry: " + typeRaw, ex);
            return;
        }

        int qty = MIN_ITEM_QUANTITY;
        if (itemParts.length > 1) {
            qty = parseIntBounded("item.quantity", itemParts[1], MIN_ITEM_QUANTITY, MAX_ITEM_QUANTITY,
                    MIN_ITEM_QUANTITY);
        }

        if (data.items.size() >= MAX_INVENTORY_SLOTS) {
            LOGGER.warning("Inventory capacity exceeded while loading save. Extra items ignored.");
            return;
        }

        data.items.add(new Item(type, qty));
    }

    private static void parseSkillEntry(SaveData data, String value) {
        if (value == null || value.isBlank()) {
            LOGGER.warning("Ignoring blank skill entry in save.");
            return;
        }
        try {
            data.unlockedSkills.add(Skill.valueOf(value.trim()));
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Ignoring unknown unlocked skill in save: " + value, ex);
        }
    }

    private static SaveData sanitize(SaveData in) {
        SaveData out = new SaveData();
        if (in == null) {
            LOGGER.warning("sanitize called with null SaveData. Returning defaults.");
            return out;
        }

        out.floor = clampInt(in.floor, MIN_FLOOR, MAX_FLOOR);
        out.maxHealth = clampInt(in.maxHealth, 1, MAX_HEALTH);
        out.health = clampInt(in.health, MIN_HEALTH, out.maxHealth);
        out.hunger = clampInt(in.hunger, MIN_HUNGER, MAX_HUNGER);
        out.level = clampInt(in.level, MIN_LEVEL, MAX_LEVEL);
        out.xp = clampInt(in.xp, 0, Integer.MAX_VALUE);
        out.xpNext = clampInt(in.xpNext, 1, Integer.MAX_VALUE);
        out.stamina = clampFloat(in.stamina, MIN_STAMINA, MAX_STAMINA);
        out.enemiesKilled = clampInt(in.enemiesKilled, 0, Integer.MAX_VALUE);
        out.finalBossDefeated = in.finalBossDefeated;
        out.poisonRemaining = clampFloat(in.poisonRemaining, MIN_DURATION, MAX_DURATION);
        out.torchDuration = clampFloat(in.torchDuration, MIN_DURATION, MAX_DURATION);

        CharacterAppearance appearance = in.characterAppearance != null
                ? in.characterAppearance.copy()
                : CharacterAppearance.defaultAppearance();
        appearance.setName(sanitizeCharacterName(appearance.getName()));
        appearance.setTunicColor(parseColor("characterTunic", CharacterAppearance.colorToString(appearance.getTunicColor()),
                CharacterAppearance.defaultAppearance().getTunicColor()));
        appearance.setSkinColor(parseColor("characterSkin", CharacterAppearance.colorToString(appearance.getSkinColor()),
                CharacterAppearance.defaultAppearance().getSkinColor()));
        appearance.setHairColor(parseColor("characterHair", CharacterAppearance.colorToString(appearance.getHairColor()),
                CharacterAppearance.defaultAppearance().getHairColor()));
        appearance.setPantsColor(parseColor("characterPants", CharacterAppearance.colorToString(appearance.getPantsColor()),
                CharacterAppearance.defaultAppearance().getPantsColor()));
        appearance.setBootColor(parseColor("characterBoots", CharacterAppearance.colorToString(appearance.getBootColor()),
                CharacterAppearance.defaultAppearance().getBootColor()));
        appearance.setCapeColor(parseColor("characterCape", CharacterAppearance.colorToString(appearance.getCapeColor()),
                CharacterAppearance.defaultAppearance().getCapeColor()));
        out.characterAppearance = appearance;

        List<Item> sanitizedItems = new ArrayList<>(Math.min(MAX_INVENTORY_SLOTS, in.items.size()));
        for (Item item : in.items) {
            if (item == null || item.getType() == null) {
                LOGGER.warning("Discarding null item during sanitize.");
                continue;
            }
            if (sanitizedItems.size() >= MAX_INVENTORY_SLOTS) {
                LOGGER.warning("Discarding extra items beyond inventory capacity during sanitize.");
                break;
            }
            int qty = clampInt(item.getQuantity(), MIN_ITEM_QUANTITY, MAX_ITEM_QUANTITY);
            sanitizedItems.add(new Item(item.getType(), qty));
        }
        out.items = sanitizedItems;

        out.equippedWeapon = isTypePresent(out.items, in.equippedWeapon, Item.Category.WEAPON) ? in.equippedWeapon : null;
        out.equippedArmor = isTypePresent(out.items, in.equippedArmor, Item.Category.ARMOR) ? in.equippedArmor : null;

        out.companionType = in.companionType;
        out.companionHealth = clampInt(in.companionHealth, MIN_COMPANION_HEALTH, MAX_COMPANION_HEALTH);
        out.companionLove = clampFloat(in.companionLove, MIN_COMPANION_STAT, MAX_COMPANION_STAT);
        out.companionHunger = clampFloat(in.companionHunger, MIN_COMPANION_STAT, MAX_COMPANION_STAT);
        out.companionHappiness = clampFloat(in.companionHappiness, MIN_COMPANION_STAT, MAX_COMPANION_STAT);
        out.companionFatigue = clampFloat(in.companionFatigue, MIN_COMPANION_STAT, MAX_COMPANION_STAT);
        out.companionPetCooldown = clampFloat(in.companionPetCooldown, MIN_COOLDOWN, MAX_COOLDOWN);

        if (out.companionType == null) {
            out.companionHealth = -1;
            out.companionLove = 60f;
            out.companionHunger = 35f;
            out.companionHappiness = 55f;
            out.companionFatigue = 0f;
            out.companionPetCooldown = 0f;
        }

        Set<Skill> uniqueSkills = new LinkedHashSet<>();
        for (Skill skill : in.unlockedSkills) {
            if (skill != null) {
                uniqueSkills.add(skill);
            } else {
                LOGGER.warning("Discarding null skill during sanitize.");
            }
        }
        out.unlockedSkills = new ArrayList<>(uniqueSkills);

        return out;
    }

    private static boolean isTypePresent(List<Item> items, Item.ItemType type, Item.Category expectedCategory) {
        if (type == null || type.category != expectedCategory)
            return false;
        for (Item item : items) {
            if (item.getType() == type)
                return true;
        }
        return false;
    }

    private static String sanitizeCharacterName(String raw) {
        if (raw == null)
            return "Adventurer";
        String cleaned = raw.replace('\n', ' ').replace('\r', ' ').replace('=', '-').trim();
        if (cleaned.isEmpty())
            cleaned = "Adventurer";
        if (cleaned.length() > MAX_CHARACTER_NAME_LENGTH)
            cleaned = cleaned.substring(0, MAX_CHARACTER_NAME_LENGTH);
        return cleaned;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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
