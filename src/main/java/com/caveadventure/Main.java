package com.caveadventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences;

import java.io.File;

/**
 * Desktop launcher for CaveAdventure.
 */
public class Main {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("CaveAdventure");
        
        // Load preferences directly to apply before window creation
        String prefsDir = ".prefs/";
        Lwjgl3Preferences prefs = new Lwjgl3Preferences("CaveAdventureSettings", prefsDir);
        
        boolean fullscreen = prefs.getBoolean("fullscreen", false);
        boolean vsync = prefs.getBoolean("vsync", true);
        int resX = prefs.getInteger("resX", 1920);
        int resY = prefs.getInteger("resY", 1080);
        int fps = prefs.getInteger("fps", 144);

        if (fullscreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            config.setWindowedMode(resX, resY);
        }
        
        config.useVsync(vsync);
        config.setForegroundFPS(fps);
        config.setResizable(true);

        // Force initial viewport update on first frame
        config.setInitialVisible(true);

        new Lwjgl3Application(new CaveAdventure(), config);
    }
}
