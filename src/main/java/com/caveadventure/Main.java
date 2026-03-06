package com.caveadventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Desktop launcher for CaveAdventure.
 */
public class Main {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("CaveAdventure");
        config.setWindowedMode(960, 640);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setResizable(true);

        // Force initial viewport update on first frame
        config.setInitialVisible(true);

        new Lwjgl3Application(new CaveAdventure(), config);
    }
}
