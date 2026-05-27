package com.caveadventure.item;

public enum EquipmentSlot {
    NONE("None"),
    WEAPON("Weapon"),
    ARMOR("Armor"),
    ACCESSORY("Accessory"),
    BOOTS("Boots");

    public final String label;

    EquipmentSlot(String label) {
        this.label = label;
    }
}
