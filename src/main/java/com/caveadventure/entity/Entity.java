package com.caveadventure.entity;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.world.GameMap;

/**
 * Base class for all game entities (player, enemies, NPCs).
 */
public abstract class Entity {

    protected float x; // Grid position X
    protected float y; // Grid position Y
    protected float pixelX; // Smooth pixel position X
    protected float pixelY; // Smooth pixel position Y
    protected int health;
    protected int maxHealth;
    protected float speed;
    protected Direction facing;
    protected boolean alive;

    public Entity(int gridX, int gridY, int maxHealth, float speed) {
        this.x = gridX;
        this.y = gridY;
        this.pixelX = gridX * GameMap.TILE_SIZE;
        this.pixelY = gridY * GameMap.TILE_SIZE;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.facing = Direction.SOUTH;
        this.alive = true;
    }

    /**
     * Update entity state each frame.
     */
    public abstract void update(float delta);

    /**
     * Render the entity.
     */
    public abstract void render(ShapeRenderer renderer);

    /**
     * Take damage. Returns true if the entity died.
     */
    public boolean takeDamage(int amount) {
        health = Math.max(0, health - amount);
        if (health <= 0) {
            alive = false;
            return true;
        }
        return false;
    }

    /**
     * Heal the entity.
     */
    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    // --- Getters ---
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getPixelX() {
        return pixelX;
    }

    public float getPixelY() {
        return pixelY;
    }

    public int getGridX() {
        return Math.round(x);
    }

    public int getGridY() {
        return Math.round(y);
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public Direction getFacing() {
        return facing;
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * Directly set health (used by Last Stand to revive from death).
     */
    public void setHealth(int h) {
        this.health = Math.max(1, Math.min(maxHealth, h));
        this.alive = true;
    }

    /**
     * Teleport entity to a new grid position.
     */
    public void setPosition(int gridX, int gridY) {
        this.x = gridX;
        this.y = gridY;
        this.pixelX = gridX * GameMap.TILE_SIZE;
        this.pixelY = gridY * GameMap.TILE_SIZE;
    }
}
