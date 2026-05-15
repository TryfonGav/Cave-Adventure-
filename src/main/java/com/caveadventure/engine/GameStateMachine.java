package com.caveadventure.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple state machine to register and switch between {@link GameStateHandler}s.
 * GameScreen can create one and register handlers to start decoupling logic.
 */
public class GameStateMachine {
    private final Map<String, GameStateHandler> handlers = new HashMap<>();
    private String current;
    private GameStateHandler currentHandler;

    public void register(String name, GameStateHandler handler) {
        handlers.put(name, handler);
    }

    public String getCurrent() { return current; }

    public void changeTo(String name) {
        if (name == null) return;
        if (name.equals(current)) return;
        if (currentHandler != null) {
            try { currentHandler.onExit(); } catch (Throwable ignored) {}
        }
        current = name;
        currentHandler = handlers.get(name);
        if (currentHandler != null) {
            try { currentHandler.onEnter(); } catch (Throwable ignored) {}
        }
    }

    public void update(InputHandler input, float delta) {
        if (currentHandler != null) currentHandler.update(input, delta);
    }

    public void render(UiRenderContext ui) {
        if (currentHandler != null) currentHandler.render(ui);
    }
}
