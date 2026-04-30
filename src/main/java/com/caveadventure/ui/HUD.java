package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.Player;

/**
 * Heads-Up Display — health, hunger, XP, floor, enemy count, and controls.
 */
public class HUD {

    private final CaveAdventure game;
    private final OrthographicCamera hudCamera;
    private final GlyphLayout layout;

    private static final float BAR_WIDTH = 200;
    private static final float BAR_HEIGHT = 18;
    private static final float PADDING = 12;
    private static final float BAR_GAP = 8;

    private static final Color HEALTH_COLOR = new Color(0.8f, 0.15f, 0.15f, 1f);
    private static final Color HEALTH_BG = new Color(0.3f, 0.05f, 0.05f, 0.8f);
    private static final Color HUNGER_COLOR = new Color(0.85f, 0.6f, 0.1f, 1f);
    private static final Color HUNGER_BG = new Color(0.3f, 0.2f, 0.05f, 0.8f);
    private static final Color STAMINA_COLOR = new Color(0.35f, 0.62f, 0.95f, 1f);
    private static final Color STAMINA_BG = new Color(0.07f, 0.10f, 0.17f, 0.85f);
    private static final Color XP_COLOR = new Color(0.25f, 0.68f, 0.30f, 1f);
    private static final Color XP_BG = new Color(0.04f, 0.16f, 0.08f, 0.85f);
    private static final Color TEXT_COLOR = CaveUIStyle.TEXT;
    private static final Color LABEL_COLOR = CaveUIStyle.MUTED_TEXT;

    public HUD(CaveAdventure game) {
        this.game = game;
        this.hudCamera = new OrthographicCamera();
        this.layout = new GlyphLayout();
    }

    public void render(Player player, int enemyCount) {
        render(player, enemyCount, 1, 10);
    }

    public void render(Player player, int enemyCount, int floor, int maxFloors) {
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(hudCamera.combined);

        // --- Stats panel (top-left) ---
        float panelWidth = BAR_WIDTH + PADDING * 3 + 50;
        float panelHeight = (BAR_HEIGHT + BAR_GAP) * 4 + PADDING * 2 + 20;
        float panelX = PADDING;
        float panelY = screenH - panelHeight - PADDING;

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        CaveUIStyle.drawStonePanel(game.shapeRenderer, panelX, panelY, panelWidth, panelHeight, 0.82f);

        // Bars
        float barX = panelX + PADDING;
        float barY = panelY + panelHeight - PADDING - BAR_HEIGHT - 15;
        drawBar(game.shapeRenderer, barX, barY, BAR_WIDTH, BAR_HEIGHT,
                (float) player.getHealth() / player.getMaxHealth(), HEALTH_COLOR, HEALTH_BG);

        float hungerY = barY - BAR_HEIGHT - BAR_GAP;
        drawBar(game.shapeRenderer, barX, hungerY, BAR_WIDTH, BAR_HEIGHT,
                (float) player.getHunger() / player.getMaxHunger(), HUNGER_COLOR, HUNGER_BG);

        float staminaY = hungerY - BAR_HEIGHT - BAR_GAP;
        drawBar(game.shapeRenderer, barX, staminaY, BAR_WIDTH, BAR_HEIGHT,
            player.getStamina() / player.getMaxStamina(), STAMINA_COLOR, STAMINA_BG);

        float xpY = staminaY - BAR_HEIGHT - BAR_GAP;
        drawBar(game.shapeRenderer, barX, xpY, BAR_WIDTH, BAR_HEIGHT * 0.7f,
                (float) player.getXP() / player.getXPToNextLevel(), XP_COLOR, XP_BG);

        // --- Right side info panel ---
        float infoW = 150;
        float infoH = 70;
        float infoX = screenW - infoW - PADDING;
        float infoY = screenH - infoH - PADDING;

        CaveUIStyle.drawStonePanel(game.shapeRenderer, infoX, infoY, infoW, infoH, 0.82f);

        // --- Controls hint ---
        float hintW = 380;
        float hintH = 25;
        float hintX = screenW / 2 - hintW / 2;

        CaveUIStyle.drawInsetPanel(game.shapeRenderer, hintX, PADDING, hintW, hintH, 0.65f);

        // Poison indicator
        if (player.isPoisoned()) {
            game.shapeRenderer.setColor(0.2f, 0.5f, 0.1f, 0.6f);
            game.shapeRenderer.rect(panelX + panelWidth + 8, panelY + panelHeight - 25, 70, 22);
        }

        game.shapeRenderer.end();

        // --- Text ---
        game.batch.setProjectionMatrix(hudCamera.combined);
        game.batch.begin();

        // Level
        game.font.setColor(TEXT_COLOR);
        game.font.draw(game.batch, "Lv." + player.getLevel(), panelX + PADDING, panelY + panelHeight - PADDING);

        // HP
        game.font.setColor(LABEL_COLOR);
        game.font.draw(game.batch, "HP", barX + BAR_WIDTH + 6, barY + BAR_HEIGHT - 2);
        game.font.setColor(TEXT_COLOR);
        String hp = player.getHealth() + "/" + player.getMaxHealth();
        layout.setText(game.font, hp);
        game.font.draw(game.batch, hp, barX + BAR_WIDTH / 2 - layout.width / 2, barY + BAR_HEIGHT - 3);

        // Hunger
        game.font.setColor(LABEL_COLOR);
        game.font.draw(game.batch, "HNG", barX + BAR_WIDTH + 6, hungerY + BAR_HEIGHT - 2);
        game.font.setColor(TEXT_COLOR);
        String hng = player.getHunger() + "/" + player.getMaxHunger();
        layout.setText(game.font, hng);
        game.font.draw(game.batch, hng, barX + BAR_WIDTH / 2 - layout.width / 2, hungerY + BAR_HEIGHT - 3);

        // Stamina
        game.font.setColor(LABEL_COLOR);
        game.font.draw(game.batch, "STM", barX + BAR_WIDTH + 6, staminaY + BAR_HEIGHT - 2);
        game.font.setColor(TEXT_COLOR);
        String stm = (int) player.getStamina() + "/" + (int) player.getMaxStamina();
        layout.setText(game.font, stm);
        game.font.draw(game.batch, stm, barX + BAR_WIDTH / 2 - layout.width / 2, staminaY + BAR_HEIGHT - 3);

        // XP
        game.font.setColor(LABEL_COLOR);
        game.font.draw(game.batch, "XP " + player.getXP() + "/" + player.getXPToNextLevel(),
                barX + 4, xpY + BAR_HEIGHT * 0.7f - 2);

        // Floor + Enemy count (right panel)
        game.font.setColor(CaveUIStyle.GOLD);
        game.font.draw(game.batch, "Floor " + floor + "/" + maxFloors, infoX + 10, infoY + infoH - 12);

        game.font.setColor(CaveUIStyle.DANGER);
        game.font.draw(game.batch, "Enemies: " + enemyCount, infoX + 10, infoY + infoH - 38);

        // Poison text
        if (player.isPoisoned()) {
            game.font.setColor(0.4f, 1f, 0.3f, 1f);
            game.font.draw(game.batch, "POISON", panelX + panelWidth + 14, panelY + panelHeight - 8);
        }

        // Controls
        game.font.setColor(CaveUIStyle.MUTED_TEXT);
        String controls = "WASD:Move  SPACE:Attack  TAB:Inventory  K:Skills  F:Interact  ESC:Menu";
        layout.setText(game.font, controls);
        game.font.draw(game.batch, controls, hintX + hintW / 2 - layout.width / 2,
                PADDING + hintH / 2 + layout.height / 2);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBorder(ShapeRenderer r, float x, float y, float w, float h) {
        r.setColor(CaveUIStyle.STONE_EDGE);
        CaveUIStyle.drawFrame(r, x, y, w, h, 2);
    }

    private void drawBar(ShapeRenderer r, float x, float y, float w, float h,
            float pct, Color fill, Color bg) {
        CaveUIStyle.drawBar(r, x, y, w, h, pct, fill, bg);
    }

    public void dispose() {
    }
}
