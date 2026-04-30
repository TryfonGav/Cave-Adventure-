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
        float panelW = 350, panelH = 400;
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
}
