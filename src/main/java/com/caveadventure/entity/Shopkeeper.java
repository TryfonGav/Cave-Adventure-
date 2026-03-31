package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.world.GameMap;

/**
 * NPC shopkeeper entity. Stands in place and sells items when interacted with.
 */
public class Shopkeeper extends Entity {

    private float animTimer;
    private int animFrame;
    private boolean active;

    public Shopkeeper(int gridX, int gridY) {
        super(gridX, gridY, 999, 0);
        this.active = true;
    }

    @Override
    public void update(float delta) {
        animTimer += delta;
        if (animTimer > 0.4f) {
            animTimer = 0;
            animFrame = (animFrame + 1) % 4;
        }
    }

    @Override
    public void render(ShapeRenderer renderer) {
        if (!active)
            return;

        float px = pixelX;
        float py = pixelY;
        int size = GameMap.TILE_SIZE;

        // Shadow
        renderer.setColor(0, 0, 0, 0.3f);
        renderer.ellipse(px + 4, py - 2, size - 8, 8);

        // Body (warm merchant colors)
        renderer.setColor(0.7f, 0.4f, 0.15f, 1f);
        renderer.rect(px + 6, py + 4, size - 12, size - 8);
        renderer.rect(px + 4, py + 8, size - 8, size - 16);

        // Hood
        renderer.setColor(0.5f, 0.25f, 0.1f, 1f);
        renderer.rect(px + 8, py + size - 10, size - 16, 8);
        renderer.rect(px + 6, py + size - 8, size - 12, 6);

        // Face
        renderer.setColor(0.85f, 0.7f, 0.55f, 1f);
        renderer.rect(px + 10, py + 14, size - 20, 10);

        // Eyes (friendly)
        renderer.setColor(0.2f, 0.15f, 0.1f, 1f);
        renderer.rect(px + 12, py + 18, 3, 3);
        renderer.rect(px + size - 15, py + 18, 3, 3);

        // Smile
        renderer.setColor(0.3f, 0.15f, 0.1f, 1f);
        renderer.rect(px + 13, py + 14, size - 26, 2);

        // Shop bag
        renderer.setColor(0.6f, 0.5f, 0.3f, 1f);
        renderer.rect(px + size - 6, py + 6, 6, 14);
        renderer.rect(px + size - 8, py + 8, 2, 10);

        // Floating indicator (animated)
        if (animFrame % 2 == 0) {
            float floatY = (float) Math.sin(animTimer * 3) * 2;
            renderer.setColor(0.9f, 0.8f, 0.2f, 0.8f);
            renderer.rect(px + size / 2f - 3, py + size + 5 + floatY, 6, 6);
            renderer.setColor(0.7f, 0.6f, 0.1f, 0.6f);
            renderer.rect(px + size / 2f - 1, py + size + 7 + floatY, 2, 2);
        }
    }

    @Override
    public boolean takeDamage(int amount) {
        return false; // Invincible
    }

    public boolean isActive() {
        return active;
    }

    public String getGreeting(int currentFloor) {
        String[] greetings = {
            "Welcome, traveler! Need supplies?",
            "Ah, a customer! Take a look.",
            "Dangerous down here, better stock up.",
            "Gold for goods, that's the rule.",
            "I've got what you need to survive."
        };
        
        String greeting = greetings[new java.util.Random().nextInt(greetings.length)];
        String hint = "";
        
        if (currentFloor <= 3) {
            hint = " Watch out for Goblin patrols early on.";
        } else if (currentFloor <= 6) {
            hint = " Spiders and Slimes are venomous. Bring antidotes!";
        } else if (currentFloor <= 8) {
            hint = " Magic enemies ahead! Try to stun or freeze them.";
        } else {
            hint = " The Golem at the bottom is almost invincible. Use everything you have.";
        }
        
        return greeting + hint;
    }
}
