package com.caveadventure.engine;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * Handles keyboard input with support for held keys and just-pressed detection.
 */
public class InputHandler implements InputProcessor {

    private final boolean[] keys = new boolean[256];
    private final boolean[] justPressedKeys = new boolean[256];
    private final boolean[] previousKeys = new boolean[256];

    /**
     * Call once per frame AFTER processing input to update just-pressed states.
     */
    public void update() {
        for (int i = 0; i < keys.length; i++) {
            justPressedKeys[i] = keys[i] && !previousKeys[i];
            previousKeys[i] = keys[i];
        }
    }

    /**
     * True if the key is currently held down.
     */
    public boolean isKeyDown(int keycode) {
        if (keycode < 0 || keycode >= keys.length)
            return false;
        return keys[keycode];
    }

    /**
     * True only on the first frame the key is pressed.
     */
    public boolean isKeyJustPressed(int keycode) {
        if (keycode < 0 || keycode >= keys.length)
            return false;
        return justPressedKeys[keycode];
    }

    // --- InputProcessor methods ---

    @Override
    public boolean keyDown(int keycode) {
        if (keycode >= 0 && keycode < keys.length) {
            keys[keycode] = true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode >= 0 && keycode < keys.length) {
            keys[keycode] = false;
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
