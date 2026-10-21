package com.caveadventure;

import com.caveadventure.engine.InputHandler;

/**
 * Key helpers for headless tests that drive screens through an {@link InputHandler}.
 */
public final class TestKeys {

    private TestKeys() {
    }

    /**
     * Presses a key so that {@code isKeyJustPressed(keycode)} is true until the next
     * {@code input.update()}. Pair with {@link #release} before pressing it again.
     */
    public static void press(InputHandler input, int keycode) {
        input.keyDown(keycode);
        input.update();
    }

    /**
     * Releases a key and clears its just-pressed state.
     */
    public static void release(InputHandler input, int keycode) {
        input.keyUp(keycode);
        input.update();
    }

    /**
     * Presses and releases a key around one call of {@code action}, e.g. a screen update.
     */
    public static void tap(InputHandler input, int keycode, Runnable action) {
        press(input, keycode);
        action.run();
        release(input, keycode);
    }
}
