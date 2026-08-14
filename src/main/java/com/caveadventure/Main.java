package com.caveadventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences;
import com.badlogic.gdx.Graphics;

import java.io.File;

/**d
 * Desktop launcher for CaveAdventure.
 */
public class Main {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("CaveAdventure");
        
        // Load preferences directly to apply before window creation
        String prefsDir = ".prefs/";
        Lwjgl3Preferences prefs = new Lwjgl3Preferences("CaveAdventureSettings", prefsDir);

        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        
        boolean fullscreen = prefs.getBoolean("fullscreen", true);
        boolean vsync = prefs.getBoolean("vsync", true);
        int resX = prefs.getInteger("resX", displayMode.width);
        int resY = prefs.getInteger("resY", displayMode.height);
        int fps = prefs.getInteger("fps", 144);

        if (fullscreen) {
            config.setFullscreenMode(displayMode);
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
