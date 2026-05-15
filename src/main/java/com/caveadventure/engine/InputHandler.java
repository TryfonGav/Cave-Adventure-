package com.caveadventure.engine;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntSet;

/**
 * Handles keyboard input with support for held keys and just-pressed detection.
 */
public class InputHandler implements InputProcessor {

    private final IntSet keysDown = new IntSet();
    private final IntSet previousKeysDown = new IntSet();
    private final IntSet justPressedKeys = new IntSet();

    /**
     * Call once per frame AFTER processing input to update just-pressed states.
     */
    public void update() {
        justPressedKeys.clear();

        IntSet.IntSetIterator current = keysDown.iterator();
        while (current.hasNext) {
            int keycode = current.next();
            if (!previousKeysDown.contains(keycode)) {
                justPressedKeys.add(keycode);
            }
        }

        previousKeysDown.clear();
        previousKeysDown.addAll(keysDown);
    }

    /**
     * True if the key is currently held down.
     */
    public boolean isKeyDown(int keycode) {
        if (keycode < 0)
            return false;
        return keysDown.contains(keycode);
    }

    /**
     * True only on the first frame the key is pressed.
     */
    public boolean isKeyJustPressed(int keycode) {
        if (keycode < 0)
            return false;
        return justPressedKeys.contains(keycode);
    }

    // --- InputProcessor methods ---

    @Override
    public boolean keyDown(int keycode) {
        if (keycode >= 0) {
            keysDown.add(keycode);
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode >= 0) {
            keysDown.remove(keycode);
            justPressedKeys.remove(keycode);
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
