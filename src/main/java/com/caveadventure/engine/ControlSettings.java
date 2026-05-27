package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class ControlSettings {
    public enum Preset {
        WASD("WASD"),
        ARROWS("Arrows");

        public final String label;

        Preset(String label) {
            this.label = label;
        }
    }

    private static final String PREF_NAME = "CaveAdventureControls";
    private static Preset currentPreset = Preset.WASD;

    public static void load() {
        if (Gdx.app == null) {
            return;
        }
        Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
        try {
            currentPreset = Preset.valueOf(prefs.getString("preset", Preset.WASD.name()));
        } catch (IllegalArgumentException ex) {
            currentPreset = Preset.WASD;
        }
    }

    public static void cyclePreset() {
        Preset[] values = Preset.values();
        currentPreset = values[(currentPreset.ordinal() + 1) % values.length];
        save();
    }

    public static void save() {
        if (Gdx.app == null) {
            return;
        }
        Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
        prefs.putString("preset", currentPreset.name());
        prefs.flush();
    }

    public static Preset getCurrentPreset() {
        return currentPreset;
    }

    public static int upKey() {
        return currentPreset == Preset.ARROWS ? Input.Keys.UP : Input.Keys.W;
    }

    public static int downKey() {
        return currentPreset == Preset.ARROWS ? Input.Keys.DOWN : Input.Keys.S;
    }

    public static int leftKey() {
        return currentPreset == Preset.ARROWS ? Input.Keys.LEFT : Input.Keys.A;
    }

    public static int rightKey() {
        return currentPreset == Preset.ARROWS ? Input.Keys.RIGHT : Input.Keys.D;
    }
}
