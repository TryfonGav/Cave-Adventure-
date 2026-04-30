package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;

import java.util.Locale;

/**
 * Visual identity for the player sprite.
 */
public class CharacterAppearance {

    private String name;
    private Color tunicColor;
    private Color skinColor;
    private Color hairColor;
    private Color pantsColor;
    private Color bootColor;
    private Color capeColor;

    private static final CharacterAppearance[] PRESETS = {
            new CharacterAppearance("Cave Scout",
                    new Color(0.18f, 0.56f, 0.90f, 1f),
                    new Color(0.86f, 0.66f, 0.48f, 1f),
                    new Color(0.16f, 0.10f, 0.07f, 1f),
                    new Color(0.10f, 0.16f, 0.26f, 1f),
                    new Color(0.08f, 0.06f, 0.05f, 1f),
                    new Color(0.09f, 0.05f, 0.11f, 1f)),
            new CharacterAppearance("Ember Ranger",
                    new Color(0.76f, 0.24f, 0.12f, 1f),
                    new Color(0.78f, 0.55f, 0.36f, 1f),
                    new Color(0.28f, 0.13f, 0.05f, 1f),
                    new Color(0.18f, 0.12f, 0.10f, 1f),
                    new Color(0.10f, 0.06f, 0.04f, 1f),
                    new Color(0.34f, 0.08f, 0.04f, 1f)),
            new CharacterAppearance("Moss Warden",
                    new Color(0.24f, 0.55f, 0.30f, 1f),
                    new Color(0.63f, 0.45f, 0.30f, 1f),
                    new Color(0.08f, 0.12f, 0.07f, 1f),
                    new Color(0.14f, 0.22f, 0.16f, 1f),
                    new Color(0.07f, 0.07f, 0.05f, 1f),
                    new Color(0.08f, 0.18f, 0.10f, 1f)),
            new CharacterAppearance("Frost Delver",
                    new Color(0.43f, 0.72f, 0.92f, 1f),
                    new Color(0.82f, 0.68f, 0.55f, 1f),
                    new Color(0.80f, 0.82f, 0.78f, 1f),
                    new Color(0.13f, 0.18f, 0.27f, 1f),
                    new Color(0.09f, 0.10f, 0.13f, 1f),
                    new Color(0.12f, 0.22f, 0.34f, 1f)),
            new CharacterAppearance("Violet Rogue",
                    new Color(0.45f, 0.28f, 0.72f, 1f),
                    new Color(0.70f, 0.48f, 0.36f, 1f),
                    new Color(0.05f, 0.04f, 0.08f, 1f),
                    new Color(0.12f, 0.10f, 0.18f, 1f),
                    new Color(0.05f, 0.04f, 0.06f, 1f),
                    new Color(0.18f, 0.08f, 0.26f, 1f))
    };

    public CharacterAppearance(String name, Color tunicColor, Color skinColor, Color hairColor,
            Color pantsColor, Color bootColor, Color capeColor) {
        this.name = name;
        this.tunicColor = new Color(tunicColor);
        this.skinColor = new Color(skinColor);
        this.hairColor = new Color(hairColor);
        this.pantsColor = new Color(pantsColor);
        this.bootColor = new Color(bootColor);
        this.capeColor = new Color(capeColor);
    }

    public static CharacterAppearance defaultAppearance() {
        return getPreset(0);
    }

    public static CharacterAppearance getPreset(int index) {
        int safeIndex = Math.max(0, Math.min(index, PRESETS.length - 1));
        return PRESETS[safeIndex].copy();
    }

    public static int presetCount() {
        return PRESETS.length;
    }

    public CharacterAppearance copy() {
        return new CharacterAppearance(name, tunicColor, skinColor, hairColor, pantsColor, bootColor, capeColor);
    }

    public Color getTunicColor() {
        return tunicColor;
    }

    public Color getTunicAltColor() {
        return scaledColor(tunicColor, 0.78f, tunicColor.a);
    }

    public Color getSkinColor() {
        return skinColor;
    }

    public Color getHairColor() {
        return hairColor;
    }

    public Color getPantsColor() {
        return pantsColor;
    }

    public Color getBootColor() {
        return bootColor;
    }

    public Color getCapeColor() {
        return capeColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null || name.isBlank() ? "Custom Delver" : name;
    }

    public void setTunicColor(Color color) {
        this.tunicColor = new Color(color);
    }

    public void setSkinColor(Color color) {
        this.skinColor = new Color(color);
    }

    public void setHairColor(Color color) {
        this.hairColor = new Color(color);
    }

    public void setPantsColor(Color color) {
        this.pantsColor = new Color(color);
    }

    public void setBootColor(Color color) {
        this.bootColor = new Color(color);
    }

    public void setCapeColor(Color color) {
        this.capeColor = new Color(color);
    }

    public static Color scaledColor(Color color, float scale, float alpha) {
        return new Color(Math.min(1f, color.r * scale), Math.min(1f, color.g * scale),
                Math.min(1f, color.b * scale), alpha);
    }

    public static String colorToString(Color color) {
        return String.format(Locale.US, "%.3f,%.3f,%.3f", color.r, color.g, color.b);
    }

    public static Color colorFromString(String value, Color fallback) {
        try {
            String[] parts = value.split(",");
            if (parts.length < 3)
                return new Color(fallback);
            return new Color(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]), 1f);
        } catch (Exception ignored) {
            return new Color(fallback);
        }
    }
}
