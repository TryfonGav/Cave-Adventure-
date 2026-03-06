package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.world.GameMap;

/**
 * Companion pet that follows the player and assists in battle.
 * Found on specific floors via rescue events.
 */
public class Companion extends Entity {

    public enum PetType {
        CAVE_WOLF("Cave Wolf", 40, 5, 8, new Color(0.55f, 0.45f, 0.35f, 1f)),
        FIRE_SPRITE("Fire Sprite", 25, 8, 12, new Color(1f, 0.6f, 0.2f, 1f)),
        SHADOW_CAT("Shadow Cat", 30, 6, 10, new Color(0.3f, 0.25f, 0.35f, 1f));

        public final String name;
        public final int maxHp;
        public final int minDmg;
        public final int maxDmg;
        public final Color color;

        PetType(String name, int hp, int min, int max, Color color) {
            this.name = name;
            this.maxHp = hp;
            this.minDmg = min;
            this.maxDmg = max;
            this.color = color;
        }
    }

    private final PetType petType;
    private float animTimer;
    private float followX, followY;

    public Companion(int gridX, int gridY, PetType type) {
        super(gridX, gridY, type.maxHp, 2f);
        this.petType = type;
    }

    public PetType getPetType() {
        return petType;
    }

    public void followPlayer(float playerPixelX, float playerPixelY, float delta) {
        float targetX = playerPixelX - 20;
        float targetY = playerPixelY - 10;
        followX += (targetX - followX) * delta * 4f;
        followY += (targetY - followY) * delta * 4f;
        pixelX = followX;
        pixelY = followY;
    }

    @Override
    public void update(float delta) {
        animTimer += delta;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        if (!alive)
            return;

        float px = pixelX;
        float py = pixelY;
        float bob = (float) Math.sin(animTimer * 3) * 2;

        // Shadow
        renderer.setColor(0, 0, 0, 0.2f);
        renderer.ellipse(px + 2, py - 2, 16, 6);

        switch (petType) {
            case CAVE_WOLF:
                renderer.setColor(petType.color);
                renderer.rect(px + 2, py + 2 + bob, 16, 10);
                renderer.rect(px + 4, py + 10 + bob, 12, 8);
                // Ears
                renderer.setColor(petType.color.r * 0.8f, petType.color.g * 0.8f, petType.color.b * 0.8f, 1f);
                renderer.rect(px + 4, py + 16 + bob, 4, 4);
                renderer.rect(px + 12, py + 16 + bob, 4, 4);
                // Eyes
                renderer.setColor(0.9f, 0.8f, 0.2f, 1f);
                renderer.rect(px + 6, py + 12 + bob, 2, 2);
                renderer.rect(px + 12, py + 12 + bob, 2, 2);
                break;

            case FIRE_SPRITE:
                // Glow
                renderer.setColor(1f, 0.5f, 0.1f, 0.2f);
                renderer.rect(px - 2, py - 2 + bob, 24, 24);
                renderer.setColor(petType.color);
                renderer.rect(px + 4, py + 2 + bob, 12, 14);
                renderer.rect(px + 6, py + 14 + bob, 8, 4);
                // Face
                renderer.setColor(1f, 1f, 0.8f, 1f);
                renderer.rect(px + 6, py + 10 + bob, 2, 2);
                renderer.rect(px + 12, py + 10 + bob, 2, 2);
                break;

            case SHADOW_CAT:
                renderer.setColor(petType.color);
                renderer.rect(px + 3, py + 2 + bob, 14, 8);
                renderer.rect(px + 5, py + 8 + bob, 10, 8);
                // Ears
                renderer.rect(px + 5, py + 14 + bob, 3, 5);
                renderer.rect(px + 12, py + 14 + bob, 3, 5);
                // Eyes (glowing)
                renderer.setColor(0.5f, 1f, 0.5f, 0.9f);
                renderer.rect(px + 7, py + 10 + bob, 2, 3);
                renderer.rect(px + 11, py + 10 + bob, 2, 3);
                // Tail
                renderer.setColor(petType.color);
                renderer.rect(px - 3, py + 4 + bob, 6, 2);
                break;
        }
    }

    public int rollDamage() {
        java.util.Random r = new java.util.Random();
        return petType.minDmg + r.nextInt(petType.maxDmg - petType.minDmg + 1);
    }
}
