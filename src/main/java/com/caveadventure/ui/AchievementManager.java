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
        WYRM_SLAYER("Wyrm Slayer", "Defeat the Mire Wyrm", new Color(0.45f, 0.95f, 0.25f, 1f)),
        QUEST_HELPER("Quest Helper", "Complete 3 quests", new Color(0.45f, 0.75f, 1f, 1f)),
        CRAFTER("Crafter", "Craft your first item", new Color(0.75f, 0.55f, 0.28f, 1f)),
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
        com.caveadventure.engine.SoundManager.getInstance().playAchievement();
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

        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        float boxW = 320;
        float boxH = Math.max(82f, smallFont.getLineHeight() * 2f + normalFont.getLineHeight() + 20f);
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
        float iconY = boxY + boxH / 2f - 10f;
        game.shapeRenderer.rect(boxX + 15, iconY, 18, 20);
        game.shapeRenderer.rect(boxX + 12, iconY + 14, 24, 6);
        game.shapeRenderer.rect(boxX + 19, iconY - 6, 10, 6);

        game.shapeRenderer.end();

        // Text
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // "Achievement Unlocked"
        float textX = boxX + 45f;
        float headerY = boxY + boxH - 10f;
        smallFont.setColor(CaveUIStyle.MUTED_TEXT.r, CaveUIStyle.MUTED_TEXT.g, CaveUIStyle.MUTED_TEXT.b, alpha);
        smallFont.draw(game.batch, "Achievement Unlocked!", textX, headerY);

        // Achievement title
        float titleY = headerY - smallFont.getLineHeight() - 2f;
        normalFont.setColor(currentPopup.color.r, currentPopup.color.g,
                currentPopup.color.b, alpha);
        normalFont.draw(game.batch, currentPopup.title, textX, titleY);

        // Description
        float descriptionY = titleY - normalFont.getLineHeight() - 2f;
        String description = fitText(smallFont, currentPopup.description, boxW - 56f);
        smallFont.setColor(CaveUIStyle.TEXT.r, CaveUIStyle.TEXT.g, CaveUIStyle.TEXT.b, alpha * 0.8f);
        smallFont.draw(game.batch, description, textX, descriptionY);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public int getUnlockedCount() {
        return unlocked.size();
    }

    public int getTotalCount() {
        return Achievement.values().length;
    }

    public Set<Achievement> getUnlocked() {
        return new HashSet<>(unlocked);
    }

    public void restoreUnlocked(Collection<Achievement> achievements) {
        unlocked.clear();
        if (achievements != null)
            unlocked.addAll(achievements);
        popupQueue.clear();
        currentPopup = null;
    }

    private String fitText(BitmapFont font, String text, float maxWidth) {
        if (text == null)
            return "";
        String candidate = text;
        layout.setText(font, candidate);
        while (layout.width > maxWidth && candidate.length() > 3) {
            candidate = candidate.substring(0, candidate.length() - 1).trim();
            layout.setText(font, candidate + "...");
        }
        if (!candidate.equals(text)) {
            candidate += "...";
        }
        return candidate;
    }
}
