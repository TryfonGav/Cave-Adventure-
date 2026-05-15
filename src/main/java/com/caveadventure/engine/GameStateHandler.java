package com.caveadventure.engine;

/**
 * Handler for a single game state. Implementations encapsulate update
 * and render logic for a state so `GameScreen` can delegate work.
 */
public interface GameStateHandler {
    /** Called once when the state becomes active. */
    void onEnter();

    /** Called once when the state is about to be left. */
    void onExit();

    /**
     * Update tick. Implementations should be allocation-free on the hot path.
     * @param input shared input handler
     * @param delta frame delta
     */
    void update(InputHandler input, float delta);

    /** Render UI/world using the provided context (reusable objects). */
    void render(UiRenderContext ui);
}
