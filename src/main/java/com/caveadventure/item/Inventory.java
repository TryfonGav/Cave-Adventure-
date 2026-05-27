package com.caveadventure.item;

import com.caveadventure.entity.Player;

import java.util.*;

/**
 * Player inventory with equipment slots, item management, and capacity limits.
 */
public class Inventory {

    public static final int MAX_SLOTS = 20;

    private final List<Item> items;
    private Item equippedWeapon;
    private Item equippedArmor;
    private Item equippedAccessory;
    private Item equippedBoots;

    // Loot message
    private String lastMessage;
    private float messageTimer;

    public Inventory() {
        this.items = new ArrayList<>();
        this.lastMessage = null;
        this.messageTimer = 0;
    }

    /**
     * Add an item to inventory. Stacks if stackable and already present.
     * Returns false if inventory is full.
     */
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }

        if (item.getQuantity() <= 0) {
            return false;
        }

        // Try to stack with existing
        if (item.isStackable()) {
            for (Item existing : items) {
                if (existing.getType() == item.getType()) {
                    existing.addQuantity(item.getQuantity());
                    setMessage("+" + item.getQuantity() + " " + item.getType().displayName);
                    return true;
                }
            }
        }

        if (items.size() >= MAX_SLOTS) {
            setMessage("Inventory full!");
            return false;
        }

        items.add(item);
        setMessage("Found: " + item.getType().displayName);
        return true;
    }

    /**
     * Remove an item (or reduce stack count).
     */
    public boolean removeItem(Item item) {
        if (item.isStackable() && item.getQuantity() > 1) {
            item.addQuantity(-1);
            return true;
        }
        return items.remove(item);
    }

    /**
     * Use a consumable item on the player.
     */
    public boolean useItem(int slotIndex, Player player) {
        if (slotIndex < 0 || slotIndex >= items.size())
            return false;
        Item item = items.get(slotIndex);

        if (!item.isUsable())
            return false;

        Item.ItemType type = item.getType();

        switch (type) {
            case HEALTH_POTION:
            case LARGE_HEALTH_POTION:
                player.heal(type.restoreAmount);
                setMessage("Healed " + type.restoreAmount + " HP");
                break;
            case ELIXIR:
                player.heal(player.getMaxHealth());
                setMessage("Fully restored HP!");
                break;
            case FOOD_RATION:
            case COOKED_MEAT:
                player.modifyHunger(type.restoreAmount);
                setMessage("Restored " + type.restoreAmount + " hunger");
                break;
            case ANTIDOTE:
                player.clearPoison();
                setMessage("Poison cured!");
                break;
            case TORCH:
                player.addTorchDuration(60f); // 60 seconds of extra light
                setMessage("Torch lit! +60s light");
                break;
            case STAMINA_ELIXIR:
                player.setStamina(player.getMaxStamina());
                setMessage("Stamina fully restored!");
                break;
            case MANA_CRYSTAL:
                player.activateManaCrystal();
                setMessage("Mana Crystal! Stamina costs -30% this battle.");
                break;
            case TOXIN_KIT:
                player.clearPoison();
                player.heal(type.restoreAmount);
                setMessage("Toxin kit used! +" + type.restoreAmount + " HP");
                break;
            default:
                if (item.isBattleOnly()) {
                    setMessage("Use " + type.displayName + " in battle!");
                    return false; // don't consume
                }
                return false;
        }

        removeItem(item);
        return true;
    }

    /**
     * Equip a weapon or armor.
     */
    public void equipItem(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= items.size())
            return;
        Item item = items.get(slotIndex);

        EquipmentSlot slot = item.getType().equipmentSlot;
        if (slot == EquipmentSlot.WEAPON) {
            equippedWeapon = item;
            setMessage("Equipped: " + item.getType().displayName);
        } else if (slot == EquipmentSlot.ARMOR) {
            equippedArmor = item;
            setMessage("Equipped: " + item.getType().displayName);
        } else if (slot == EquipmentSlot.ACCESSORY) {
            equippedAccessory = item;
            setMessage("Equipped: " + item.getType().displayName);
        } else if (slot == EquipmentSlot.BOOTS) {
            equippedBoots = item;
            setMessage("Equipped: " + item.getType().displayName);
        }
    }

    /**
     * Drop an item from inventory.
     */
    public Item dropItem(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= items.size())
            return null;
        Item item = items.get(slotIndex);

        // Unequip if it was equipped
        if (item == equippedWeapon)
            equippedWeapon = null;
        if (item == equippedArmor)
            equippedArmor = null;
        if (item == equippedAccessory)
            equippedAccessory = null;
        if (item == equippedBoots)
            equippedBoots = null;

        items.remove(slotIndex);
        setMessage("Dropped: " + item.getType().displayName);
        return item;
    }

    /**
     * Check if player has a specific key type.
     */
    public boolean hasKey(Item.ItemType keyType) {
        for (Item item : items) {
            if (item.getType() == keyType)
                return true;
        }
        return false;
    }

    /**
     * Remove a key after use.
     */
    public boolean useKey(Item.ItemType keyType) {
        Iterator<Item> it = items.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            if (item.getType() == keyType) {
                if (item.getQuantity() > 1) {
                    item.addQuantity(-1);
                } else {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }

    public int getTotalAttackBonus() {
        return statSum(StatType.ATTACK);
    }

    public int getTotalDefenseBonus() {
        return statSum(StatType.DEFENSE);
    }

    public float getTotalSpeedBonus() {
        float bonus = 0f;
        for (Item item : equippedItems()) {
            if (item != null)
                bonus += item.getType().speedBonus;
        }
        return bonus;
    }

    public float getTotalLightBonus() {
        float bonus = 0f;
        for (Item item : equippedItems()) {
            if (item != null)
                bonus += item.getType().lightBonus;
        }
        return bonus;
    }

    public float getTotalCritBonus() {
        float bonus = 0f;
        for (Item item : equippedItems()) {
            if (item != null)
                bonus += item.getType().critBonus;
        }
        return bonus;
    }

    public int count(Item.ItemType type) {
        int total = 0;
        for (Item item : items) {
            if (item.getType() == type) {
                total += item.getQuantity();
            }
        }
        return total;
    }

    public boolean removeItems(Item.ItemType type, int amount) {
        if (amount <= 0)
            return true;
        if (count(type) < amount)
            return false;
        int remaining = amount;
        Iterator<Item> it = items.iterator();
        while (it.hasNext() && remaining > 0) {
            Item item = it.next();
            if (item.getType() != type)
                continue;
            int take = Math.min(remaining, item.getQuantity());
            item.removeQuantity(take);
            remaining -= take;
            if (item.getQuantity() <= 0) {
                if (item == equippedWeapon)
                    equippedWeapon = null;
                if (item == equippedArmor)
                    equippedArmor = null;
                if (item == equippedAccessory)
                    equippedAccessory = null;
                if (item == equippedBoots)
                    equippedBoots = null;
                it.remove();
            }
        }
        return true;
    }

    private enum StatType {
        ATTACK, DEFENSE
    }

    private int statSum(StatType statType) {
        int total = 0;
        for (Item item : equippedItems()) {
            if (item == null)
                continue;
            total += statType == StatType.ATTACK ? item.getType().attackBonus : item.getType().defenseBonus;
        }
        return total;
    }

    private Item[] equippedItems() {
        return new Item[] { equippedWeapon, equippedArmor, equippedAccessory, equippedBoots };
    }

    private void setMessage(String msg) {
        this.lastMessage = msg;
        this.messageTimer = 2.5f;
    }

    public void updateMessageTimer(float delta) {
        if (messageTimer > 0) {
            messageTimer = Math.max(0f, messageTimer - delta);
        }
    }

    // --- Getters ---
    public List<Item> getItems() {
        return items;
    }

    public int getSize() {
        return items.size();
    }

    public Item getItem(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    public Item getEquippedWeapon() {
        return equippedWeapon;
    }

    public Item getEquippedArmor() {
        return equippedArmor;
    }

    public Item getEquippedAccessory() {
        return equippedAccessory;
    }

    public Item getEquippedBoots() {
        return equippedBoots;
    }

    public String getLastMessage() {
        return messageTimer > 0 ? lastMessage : null;
    }

    public float getMessageTimer() {
        return messageTimer;
    }
}
