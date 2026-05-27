package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.Player;

/**
 * Stats screen displaying detailed player statistics.
 */
public class StatsScreen {

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;

    // Tracked stats
    public int totalDamageDealt;
    public int totalDamageTaken;
    public int stepsTaken;
    public int trapsTriggered;
    public int itemsUsed;
    public int chestsOpened;
    public int doorsUnlocked;
    public int goldSpent;
    public int eventsCompleted;
    public int battlesFought;
    public int timesFled;
    public int questsCompleted;
    public int itemsCrafted;
    public int bossesKilled;

    private boolean visible;

    public StatsScreen(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
    }

    public void toggle() {
        visible = !visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(Player player, int enemiesKilled, int floor) {
        if (!visible)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        float panelW = 350, panelH = 470;
        float px = sw / 2 - panelW / 2, py = sh / 2 - panelH / 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(0, 0, 0, 0.6f);
        game.shapeRenderer.rect(0, 0, sw, sh);

        CaveUIStyle.drawStonePanel(game.shapeRenderer, px, py, panelW, panelH, 0.96f);
        CaveUIStyle.drawCarvedSeparator(game.shapeRenderer, px + 16, py + panelH - 38, panelW - 32, 1f);

        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;

        nf.setColor(CaveUIStyle.GOLD);
        nf.draw(game.batch, "STATISTICS", px + 15, py + panelH - 10);

        float sx = px + 20;
        float sy = py + panelH - 45;
        float gap = 20;

        sf.setColor(CaveUIStyle.TEXT);
        drawStat(sf, sx, sy, "Level", "" + player.getLevel());
        drawStat(sf, sx, sy - gap, "Floor", "" + floor);
        drawStat(sf, sx, sy - gap * 2, "HP", player.getHealth() + "/" + player.getMaxHealth());
        drawStat(sf, sx, sy - gap * 3, "Total ATK", "" + player.getTotalAttack());

        sf.setColor(CaveUIStyle.DANGER);
        drawStat(sf, sx, sy - gap * 5, "Enemies Killed", "" + enemiesKilled);
        drawStat(sf, sx, sy - gap * 6, "Damage Dealt", "" + totalDamageDealt);
        drawStat(sf, sx, sy - gap * 7, "Damage Taken", "" + totalDamageTaken);
        drawStat(sf, sx, sy - gap * 8, "Battles Fought", "" + battlesFought);
        drawStat(sf, sx, sy - gap * 9, "Times Fled", "" + timesFled);

        sf.setColor(CaveUIStyle.GOLD);
        drawStat(sf, sx, sy - gap * 11, "Steps Taken", "" + stepsTaken);
        drawStat(sf, sx, sy - gap * 12, "Chests Opened", "" + chestsOpened);
        drawStat(sf, sx, sy - gap * 13, "Traps Triggered", "" + trapsTriggered);
        drawStat(sf, sx, sy - gap * 14, "Items Used", "" + itemsUsed);
        drawStat(sf, sx, sy - gap * 15, "Gold Spent", "" + goldSpent);
        drawStat(sf, sx, sy - gap * 16, "Quests Completed", "" + questsCompleted);
        drawStat(sf, sx, sy - gap * 17, "Items Crafted", "" + itemsCrafted);
        drawStat(sf, sx, sy - gap * 18, "Bosses Killed", "" + bossesKilled);

        sf.setColor(CaveUIStyle.MUTED_TEXT);
        sf.draw(game.batch, "Press P or ESC to close", px + 15, py + 12);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawStat(BitmapFont font, float x, float y, String label, String value) {
        font.draw(game.batch, label, x, y);
        layout.setText(font, value);
        font.draw(game.batch, value, x + 280 - layout.width, y);
    }

    public java.util.Map<String, Integer> toSaveMap() {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        stats.put("totalDamageDealt", totalDamageDealt);
        stats.put("totalDamageTaken", totalDamageTaken);
        stats.put("stepsTaken", stepsTaken);
        stats.put("trapsTriggered", trapsTriggered);
        stats.put("itemsUsed", itemsUsed);
        stats.put("chestsOpened", chestsOpened);
        stats.put("doorsUnlocked", doorsUnlocked);
        stats.put("goldSpent", goldSpent);
        stats.put("eventsCompleted", eventsCompleted);
        stats.put("battlesFought", battlesFought);
        stats.put("timesFled", timesFled);
        stats.put("questsCompleted", questsCompleted);
        stats.put("itemsCrafted", itemsCrafted);
        stats.put("bossesKilled", bossesKilled);
        return stats;
    }

    public void restore(java.util.Map<String, Integer> stats) {
        if (stats == null)
            return;
        totalDamageDealt = stats.getOrDefault("totalDamageDealt", totalDamageDealt);
        totalDamageTaken = stats.getOrDefault("totalDamageTaken", totalDamageTaken);
        stepsTaken = stats.getOrDefault("stepsTaken", stepsTaken);
        trapsTriggered = stats.getOrDefault("trapsTriggered", trapsTriggered);
        itemsUsed = stats.getOrDefault("itemsUsed", itemsUsed);
        chestsOpened = stats.getOrDefault("chestsOpened", chestsOpened);
        doorsUnlocked = stats.getOrDefault("doorsUnlocked", doorsUnlocked);
        goldSpent = stats.getOrDefault("goldSpent", goldSpent);
        eventsCompleted = stats.getOrDefault("eventsCompleted", eventsCompleted);
        battlesFought = stats.getOrDefault("battlesFought", battlesFought);
        timesFled = stats.getOrDefault("timesFled", timesFled);
        questsCompleted = stats.getOrDefault("questsCompleted", questsCompleted);
        itemsCrafted = stats.getOrDefault("itemsCrafted", itemsCrafted);
        bossesKilled = stats.getOrDefault("bossesKilled", bossesKilled);
    }

    public void reset() {
        totalDamageDealt = totalDamageTaken = stepsTaken = trapsTriggered = itemsUsed = chestsOpened = 0;
        doorsUnlocked = goldSpent = eventsCompleted = battlesFought = timesFled = questsCompleted = 0;
        itemsCrafted = bossesKilled = 0;
        visible = false;
    }
}
