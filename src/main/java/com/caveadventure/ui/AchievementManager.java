package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;

import java.util.*;

/**
 * Achievement system with unlock tracking and popup notifications.
 */
public class AchievementManager {

    public enum Achievement {
        FIRST_BLOOD("First Blood", "Defeat your first enemy", new Color(0.8f, 0.2f, 0.15f, 1f)),
        SLAYER_10("Slayer", "Defeat 10 enemies", new Color(0.9f, 0.3f, 0.1f, 1f)),
        SLAYER_50("Massacre", "Defeat 50 enemies", new Color(1f, 0.2f, 0.2f, 1f)),
        SLAYER_100("Exterminator", "Defeat 100 enemies", new Color(1f, 0.1f, 0.1f, 1f)),
        FLOOR_3("Delver", "Reach floor 3", new Color(0.3f, 0.6f, 0.8f, 1f)),
        FLOOR_5("Deep Diver", "Reach floor 5", new Color(0.2f, 0.5f, 0.9f, 1f)),
        FLOOR_10("Abyss Walker", "Reach floor 10", new Color(0.1f, 0.3f, 1f, 1f)),
        LEVEL_5("Seasoned", "Reach level 5", new Color(0.3f, 0.8f, 0.3f, 1f)),
        LEVEL_10("Veteran", "Reach level 10", new Color(0.2f, 0.9f, 0.2f, 1f)),
        BOSS_SLAYER("Boss Slayer", "Defeat a boss", new Color(0.9f, 0.7f, 0.1f, 1f)),
        SHOPAHOLIC("Shopaholic", "Buy an item from a shop", new Color(0.8f, 0.6f, 0.2f, 1f)),
        SURVIVOR("Survivor", "Survive below 10 HP", new Color(0.7f, 0.15f, 0.15f, 1f)),
        COLLECTOR("Collector", "Have 15 items at once", new Color(0.6f, 0.4f, 0.8f, 1f)),
        VICTORY("Conqueror", "Complete the cave", new Color(1f, 0.85f, 0.2f, 1f));

        public final String title;
        public final String description;
        public final Color color;

        Achievement(String title, String desc, Color color) {
            this.title = title;
            this.description = desc;
            this.color = color;
        }
    }

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;
    private final Set<Achievement> unlocked;

    // Popup queue
    private final Queue<Achievement> popupQueue;
    private Achievement currentPopup;
    private float popupTimer;
    private static final float POPUP_DURATION = 3.0f;
    private static final float POPUP_SLIDE_TIME = 0.4f;

    public AchievementManager(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.unlocked = new HashSet<>();
        this.popupQueue = new LinkedList<>();
    }

    public boolean tryUnlock(Achievement achievement) {
        if (unlocked.contains(achievement))
            return false;
        unlocked.add(achievement);
        popupQueue.add(achievement);
        return true;
    }

    /**
     * Check various conditions for automatic achievement unlocking.
     */
    public void checkConditions(int enemiesKilled, int floor, int level, int hp, int inventorySize,
            boolean bossKilled) {
        if (enemiesKilled >= 1)
            tryUnlock(Achievement.FIRST_BLOOD);
        if (enemiesKilled >= 10)
            tryUnlock(Achievement.SLAYER_10);
        if (enemiesKilled >= 50)
            tryUnlock(Achievement.SLAYER_50);
        if (enemiesKilled >= 100)
            tryUnlock(Achievement.SLAYER_100);
        if (floor >= 3)
            tryUnlock(Achievement.FLOOR_3);
        if (floor >= 5)
            tryUnlock(Achievement.FLOOR_5);
        if (floor >= 10)
            tryUnlock(Achievement.FLOOR_10);
        if (level >= 5)
            tryUnlock(Achievement.LEVEL_5);
        if (level >= 10)
            tryUnlock(Achievement.LEVEL_10);
        if (bossKilled)
            tryUnlock(Achievement.BOSS_SLAYER);
        if (hp > 0 && hp < 10)
            tryUnlock(Achievement.SURVIVOR);
        if (inventorySize >= 15)
            tryUnlock(Achievement.COLLECTOR);
    }

    public void update(float delta) {
        if (currentPopup != null) {
            popupTimer += delta;
            if (popupTimer >= POPUP_DURATION) {
                currentPopup = null;
            }
        }

        if (currentPopup == null && !popupQueue.isEmpty()) {
            currentPopup = popupQueue.poll();
            popupTimer = 0;
        }
    }

    public void render() {
        if (currentPopup == null)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Slide animation
        float slideIn = Math.min(1f, popupTimer / POPUP_SLIDE_TIME);
        float slideOut = popupTimer > POPUP_DURATION - POPUP_SLIDE_TIME
                ? (POPUP_DURATION - popupTimer) / POPUP_SLIDE_TIME
                : 1f;
        float slide = Math.min(slideIn, Math.max(0, slideOut));
        float alpha = slide;

        float boxW = 300;
        float boxH = 55;
        float boxX = screenW / 2 - boxW / 2;
        float boxY = screenH - 20 - boxH * slide;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        CaveUIStyle.drawStonePanel(game.shapeRenderer, boxX, boxY, boxW, boxH, alpha);

        // Accent bar (left side, achievement color)
        game.shapeRenderer.setColor(currentPopup.color.r, currentPopup.color.g,
                currentPopup.color.b, alpha);
        game.shapeRenderer.rect(boxX, boxY, 4, boxH);

        // Trophy icon (simple)
        game.shapeRenderer.setColor(currentPopup.color.r, currentPopup.color.g,
                currentPopup.color.b, 0.8f * alpha);
        game.shapeRenderer.rect(boxX + 15, boxY + 18, 18, 20);
        game.shapeRenderer.rect(boxX + 12, boxY + 32, 24, 6);
        game.shapeRenderer.rect(boxX + 19, boxY + 12, 10, 6);

        game.shapeRenderer.end();

        // Text
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        // "Achievement Unlocked"
        smallFont.setColor(CaveUIStyle.MUTED_TEXT.r, CaveUIStyle.MUTED_TEXT.g, CaveUIStyle.MUTED_TEXT.b, alpha);
        smallFont.draw(game.batch, "Achievement Unlocked!", boxX + 45, boxY + boxH - 8);

        // Achievement title
        normalFont.setColor(currentPopup.color.r, currentPopup.color.g,
                currentPopup.color.b, alpha);
        normalFont.draw(game.batch, currentPopup.title, boxX + 45, boxY + boxH - 26);

        // Description
        smallFont.setColor(CaveUIStyle.TEXT.r, CaveUIStyle.TEXT.g, CaveUIStyle.TEXT.b, alpha * 0.8f);
        smallFont.draw(game.batch, currentPopup.description, boxX + 45, boxY + 14);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public int getUnlockedCount() {
        return unlocked.size();
    }

    public int getTotalCount() {
        return Achievement.values().length;
    }
}
