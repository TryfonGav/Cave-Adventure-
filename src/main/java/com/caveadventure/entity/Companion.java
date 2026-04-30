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
    private float love = 60f;
    private float hunger = 35f;
    private float happiness = 55f;
    private float fatigue = 0f;
    private float petCooldownTimer = 0f;
    private float careTickTimer = 0f;
    private float careMessageTimer = 0f;
    private String careMessage = "Ready";

    private static final Color OUTLINE_COLOR = new Color(0.04f, 0.04f, 0.06f, 1f);
    private static final Color SHADOW_COLOR = new Color(0f, 0f, 0f, 0.28f);
    private static final Color WARM_EYE = new Color(1f, 0.86f, 0.24f, 1f);
    private static final Color SPRITE_CORE = new Color(1f, 0.96f, 0.48f, 1f);
    private static final Color CAT_EYE = new Color(0.42f, 1f, 0.45f, 1f);
    private static final float CARE_TICK_SECONDS = 20f;
    private static final float PET_COOLDOWN_SECONDS = 25f;
    private static final float LOW_LOVE_THRESHOLD = 25f;
    private static final float HIGH_LOVE_THRESHOLD = 75f;

    public Companion(int gridX, int gridY, PetType type) {
        super(gridX, gridY, type.maxHp, 2f);
        this.petType = type;
        this.followX = pixelX;
        this.followY = pixelY;
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
        updateCare(delta);
    }

    private void updateCare(float delta) {
        petCooldownTimer = Math.max(0f, petCooldownTimer - delta);
        careMessageTimer = Math.max(0f, careMessageTimer - delta);

        careTickTimer += delta;
        while (careTickTimer >= CARE_TICK_SECONDS) {
            careTickTimer -= CARE_TICK_SECONDS;
            hunger = clampCare(hunger + 2.5f);
            happiness = clampCare(happiness - 2.0f);
            fatigue = clampCare(fatigue - 4.0f);

            if (hunger > 72f || happiness < 28f) {
                love = clampCare(love - 2.0f);
            } else if (hunger < 40f && happiness > 65f && fatigue < 70f) {
                love = clampCare(love + 0.75f);
            }
        }
    }

    public boolean feed() {
        hunger = clampCare(hunger - 32f);
        happiness = clampCare(happiness + 3f);
        love = clampCare(love + 8f);
        setCareMessage(petType.name + " munches happily.");
        return true;
    }

    public boolean play() {
        if (fatigue >= 85f) {
            happiness = clampCare(happiness - 3f);
            love = clampCare(love - 2f);
            setCareMessage(petType.name + " is too tired.");
            return false;
        }

        happiness = clampCare(happiness + 26f);
        fatigue = clampCare(fatigue + 24f);
        love = clampCare(love + 10f);
        setCareMessage(petType.name + " had fun.");
        return true;
    }

    public boolean pet() {
        if (petCooldownTimer > 0f) {
            setCareMessage("Pet again in " + (int) Math.ceil(petCooldownTimer) + "s.");
            return false;
        }

        happiness = clampCare(happiness + 7f);
        love = clampCare(love + 5f);
        petCooldownTimer = PET_COOLDOWN_SECONDS;
        setCareMessage(petType.name + " feels loved.");
        return true;
    }

    private void setCareMessage(String message) {
        careMessage = message;
        careMessageTimer = 3f;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        if (!alive)
            return;

        float px = pixelX;
        float py = pixelY;
        float bob = (float) Math.sin(animTimer * 4.5f) * 2f;
        float twitch = (float) Math.sin(animTimer * 8f) * 1.5f;

        renderer.setColor(SHADOW_COLOR);
        renderer.ellipse(px + 2, py - 3, GameMap.TILE_SIZE - 8, 8);

        switch (petType) {
            case CAVE_WOLF:
                renderCaveWolf(renderer, px, py + bob, twitch);
                break;

            case FIRE_SPRITE:
                renderFireSprite(renderer, px, py + bob, twitch);
                break;

            case SHADOW_CAT:
                renderShadowCat(renderer, px, py + bob, twitch);
                break;
        }
    }

    private void renderCaveWolf(ShapeRenderer r, float px, float py, float twitch) {
        Color base = petType.color;
        Color dark = scaledColor(base, 0.58f, 1f);
        Color light = scaledColor(base, 1.22f, 1f);

        r.setColor(OUTLINE_COLOR);
        r.triangle(px + 8, py + 15, px, py + 20 + twitch, px + 7, py + 10);
        r.rect(px + 5, py + 7, 19, 12);
        r.rect(px + 18, py + 13, 10, 10);
        r.triangle(px + 20, py + 22, px + 22, py + 29, px + 25, py + 22);
        r.triangle(px + 25, py + 22, px + 27, py + 28, px + 29, py + 22);
        r.rect(px + 7, py + 2, 5, 7);
        r.rect(px + 18, py + 2, 5, 7);

        r.setColor(dark);
        r.triangle(px + 8, py + 15, px + 2, py + 18 + twitch, px + 8, py + 11);
        r.rect(px + 6, py + 8, 17, 10);
        r.rect(px + 19, py + 14, 8, 8);
        r.rect(px + 8, py + 3, 3, 6);
        r.rect(px + 19, py + 3, 3, 6);

        r.setColor(base);
        r.rect(px + 8, py + 10, 15, 8);
        r.rect(px + 20, py + 15, 7, 7);
        r.triangle(px + 21, py + 22, px + 22, py + 27, px + 24, py + 22);
        r.triangle(px + 25, py + 22, px + 27, py + 27, px + 28, py + 22);

        r.setColor(light);
        r.rect(px + 10, py + 16, 11, 3);
        r.rect(px + 22, py + 18, 4, 2);
        r.setColor(0.86f, 0.78f, 0.62f, 1f);
        r.rect(px + 23, py + 14, 5, 3);
        r.rect(px + 11, py + 8, 8, 2);

        r.setColor(WARM_EYE);
        r.rect(px + 23, py + 18, 2, 2);
        r.setColor(0.02f, 0.02f, 0.02f, 1f);
        r.rect(px + 28, py + 15, 2, 2);
        r.setColor(0.92f, 0.90f, 0.78f, 1f);
        r.rect(px + 25, py + 13, 2, 3);
    }

    private void renderFireSprite(ShapeRenderer r, float px, float py, float twitch) {
        Color base = petType.color;
        Color dark = scaledColor(base, 0.68f, 0.95f);
        Color light = scaledColor(base, 1.28f, 1f);
        float flicker = (float) Math.sin(animTimer * 11f) * 2f;

        r.setColor(1f, 0.28f, 0.05f, 0.16f);
        r.ellipse(px - 4, py + 1, 34, 28);
        r.setColor(1f, 0.62f, 0.12f, 0.20f);
        r.ellipse(px + 1, py + 4, 24, 23);

        r.setColor(OUTLINE_COLOR);
        r.triangle(px + 15, py + 30 + flicker, px + 3, py + 8, px + 27, py + 8);
        r.rect(px + 7, py + 5, 18, 15);
        r.triangle(px + 7, py + 15, px + 1, py + 23 + twitch, px + 10, py + 19);
        r.triangle(px + 23, py + 15, px + 31, py + 22 - twitch, px + 22, py + 19);

        r.setColor(dark);
        r.triangle(px + 15, py + 27 + flicker, px + 5, py + 9, px + 25, py + 9);
        r.rect(px + 8, py + 6, 16, 14);
        r.setColor(base);
        r.triangle(px + 15, py + 25 + flicker, px + 7, py + 10, px + 23, py + 10);
        r.rect(px + 10, py + 8, 12, 12);
        r.setColor(light);
        r.triangle(px + 15, py + 21 + flicker * 0.5f, px + 10, py + 10, px + 20, py + 10);
        r.rect(px + 12, py + 9, 8, 7);

        r.setColor(SPRITE_CORE);
        r.rect(px + 11, py + 14, 3, 3);
        r.rect(px + 18, py + 14, 3, 3);
        r.setColor(0.22f, 0.06f, 0.02f, 0.82f);
        r.rect(px + 12, py + 13, 1, 2);
        r.rect(px + 19, py + 13, 1, 2);
        r.rect(px + 14, py + 10, 5, 1);

        r.setColor(1f, 0.82f, 0.22f, 0.70f);
        r.rect(px + 2 + twitch, py + 3, 3, 3);
        r.rect(px + 27 - twitch, py + 7, 2, 2);
        r.rect(px + 24, py + 25 - twitch, 2, 2);
    }

    private void renderShadowCat(ShapeRenderer r, float px, float py, float twitch) {
        Color base = petType.color;
        Color dark = scaledColor(base, 0.52f, 1f);
        Color light = scaledColor(base, 1.35f, 0.88f);

        r.setColor(base.r, base.g, base.b, 0.24f);
        r.rect(px + 3, py + 7, 19, 8);
        r.rect(px + 16, py + 13, 12, 8);

        r.setColor(OUTLINE_COLOR);
        r.rect(px + 5, py + 7, 18, 10);
        r.rect(px + 17, py + 14, 10, 9);
        r.triangle(px + 18, py + 22, px + 20, py + 29, px + 23, py + 22);
        r.triangle(px + 24, py + 22, px + 27, py + 28, px + 28, py + 22);
        r.rect(px + 2, py + 12 + twitch, 6, 4);
        r.rect(px - 1, py + 15 + twitch, 5, 4);
        r.rect(px + 7, py + 2, 4, 7);
        r.rect(px + 18, py + 2, 4, 7);

        r.setColor(dark);
        r.rect(px + 6, py + 8, 16, 8);
        r.rect(px + 18, py + 15, 8, 7);
        r.rect(px + 3, py + 13 + twitch, 5, 3);
        r.rect(px, py + 16 + twitch, 4, 3);
        r.rect(px + 8, py + 3, 2, 6);
        r.rect(px + 19, py + 3, 2, 6);

        r.setColor(base);
        r.rect(px + 8, py + 10, 13, 6);
        r.rect(px + 19, py + 16, 7, 6);
        r.triangle(px + 19, py + 22, px + 20, py + 27, px + 22, py + 22);
        r.triangle(px + 24, py + 22, px + 26, py + 27, px + 27, py + 22);
        r.setColor(light);
        r.rect(px + 10, py + 15, 8, 2);
        r.rect(px + 20, py + 20, 4, 1);

        r.setColor(CAT_EYE);
        r.rect(px + 20, py + 18, 2, 3);
        r.rect(px + 24, py + 18, 2, 3);
        r.setColor(0.02f, 0.03f, 0.02f, 1f);
        r.rect(px + 20, py + 19, 1, 2);
        r.rect(px + 24, py + 19, 1, 2);
        r.setColor(0.62f, 1f, 0.66f, 0.48f);
        r.rect(px + 18, py + 18, 10, 1);
        r.rect(px + 18, py + 16, 10, 1);
    }

    private Color scaledColor(Color color, float scale, float alpha) {
        return new Color(Math.min(1f, color.r * scale), Math.min(1f, color.g * scale),
                Math.min(1f, color.b * scale), alpha);
    }

    public int rollDamage() {
        java.util.Random r = new java.util.Random();
        int damage = petType.minDmg + r.nextInt(petType.maxDmg - petType.minDmg + 1);
        return Math.max(1, Math.round(damage * getBattleStatMultiplier()));
    }

    public int scaleAssistValue(int value) {
        return Math.max(1, Math.round(value * getBattleStatMultiplier()));
    }

    public boolean canAssistInBattle() {
        return alive && love >= LOW_LOVE_THRESHOLD;
    }

    public boolean hasLoveStatBonus() {
        return love > HIGH_LOVE_THRESHOLD;
    }

    public float getBattleStatMultiplier() {
        return hasLoveStatBonus() ? 1.2f : 1f;
    }

    public float getRunChanceBonus(float baseBonus) {
        if (!canAssistInBattle())
            return 0f;
        return baseBonus * getBattleStatMultiplier();
    }

    public String getMoodLabel() {
        if (love < LOW_LOVE_THRESHOLD)
            return "Neglected";
        if (love > HIGH_LOVE_THRESHOLD)
            return "Devoted";
        if (hunger > 70f)
            return "Hungry";
        if (happiness < 35f)
            return "Lonely";
        return "Content";
    }

    public void restoreCareState(float love, float hunger, float happiness, float fatigue, float petCooldownTimer) {
        this.love = clampCare(love);
        this.hunger = clampCare(hunger);
        this.happiness = clampCare(happiness);
        this.fatigue = clampCare(fatigue);
        this.petCooldownTimer = Math.max(0f, petCooldownTimer);
    }

    private float clampCare(float value) {
        return Math.max(0f, Math.min(100f, value));
    }

    public void setCurrentHealth(int value) {
        health = Math.max(0, Math.min(maxHealth, value));
        alive = health > 0;
    }

    public float getLove() {
        return love;
    }

    public float getHunger() {
        return hunger;
    }

    public float getHappiness() {
        return happiness;
    }

    public float getFatigue() {
        return fatigue;
    }

    public float getPetCooldownTimer() {
        return petCooldownTimer;
    }

    public String getCareMessage() {
        return careMessageTimer > 0f ? careMessage : getMoodLabel();
    }
}
