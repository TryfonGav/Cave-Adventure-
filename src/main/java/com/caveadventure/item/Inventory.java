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
            default:
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

        if (item.getType().category == Item.Category.WEAPON) {
            if (equippedWeapon != null) {
                setMessage("Equipped: " + item.getType().displayName +
                        " (was: " + equippedWeapon.getType().displayName + ")");
            } else {
                setMessage("Equipped: " + item.getType().displayName);
            }
            equippedWeapon = item;
        } else if (item.getType().category == Item.Category.ARMOR) {
            if (equippedArmor != null) {
                setMessage("Equipped: " + item.getType().displayName);
            } else {
                setMessage("Equipped: " + item.getType().displayName);
            }
            equippedArmor = item;
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
        return equippedWeapon != null ? equippedWeapon.getType().attackBonus : 0;
    }

    public int getTotalDefenseBonus() {
        return equippedArmor != null ? equippedArmor.getType().defenseBonus : 0;
    }

    private void setMessage(String msg) {
        this.lastMessage = msg;
        this.messageTimer = 2.5f;
    }

    public void updateMessageTimer(float delta) {
        if (messageTimer > 0)
            messageTimer -= delta;
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

    public String getLastMessage() {
        return messageTimer > 0 ? lastMessage : null;
    }

    public float getMessageTimer() {
        return messageTimer;
    }
}
