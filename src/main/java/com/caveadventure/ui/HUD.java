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

import java.util.ArrayList;
import java.util.List;

/**
 * Heads-Up Display — health, hunger, XP, floor, enemy count, and controls.
 */
public class HUD {

    private final CaveAdventure game;
    private final OrthographicCamera hudCamera;
    private final GlyphLayout layout;

    private static final float BAR_WIDTH = 220;
    private static final float BAR_HEIGHT = 22;
    private static final float PADDING = 12;
    private static final float BAR_GAP = 10;
    private static final float TOP_CLEARANCE = 24;

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
        render(player, enemyCount, floor, maxFloors, null);
    }

    public void render(Player player, int enemyCount, int floor, int maxFloors, String questText) {
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
        float panelY = screenH - panelHeight - PADDING - TOP_CLEARANCE;

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
        float hintW = Math.min(screenW - PADDING * 2, 560);
        float hintH = 25;
        float hintX = screenW / 2 - hintW / 2;

        // --- Left side quest tracker (auto-sizing) ---
        boolean hasQuestTracker = questText != null && !"No active quest".equals(questText);
        String questTitle = "";
        String[] questDescriptionLines = new String[0];
        float questPanelX = PADDING;
        float questPanelY = 0f;
        float questPanelW = 0f;
        float questPanelH = 0f;
        if (hasQuestTracker) {
            String[] questLines = questText.split("\\n", 2);
            questTitle = questLines[0].trim();
            String description = questLines.length > 1 ? questLines[1].trim() : "";
            BitmapFont titleFont = game.font;
            BitmapFont questFont = game.fontSmall != null ? game.fontSmall : game.font;
            float titleLineHeight = titleFont.getLineHeight();
            float descLineHeight = questFont.getLineHeight();
            float maxQuestTextWidth = Math.min(360f, screenW - PADDING * 2 - 36f);
            questDescriptionLines = wrapQuestText(questFont, description, maxQuestTextWidth);

            float widestLine = 0f;
            layout.setText(titleFont, "• " + questTitle);
            widestLine = Math.max(widestLine, layout.width);
            for (String line : questDescriptionLines) {
                layout.setText(questFont, line);
                widestLine = Math.max(widestLine, layout.width + 12f);
            }

            float maxPanelW = Math.min(420f, screenW - PADDING * 2);
            questPanelW = Math.max(220f, Math.min(maxPanelW, widestLine + 26f));
            questPanelH = 16f + titleLineHeight + 6f + questDescriptionLines.length * descLineHeight + 6f;
            float minQuestY = PADDING + hintH + 14f;
            questPanelY = Math.max(minQuestY, panelY - questPanelH - 10f);

            CaveUIStyle.drawStonePanel(game.shapeRenderer, questPanelX, questPanelY, questPanelW, questPanelH, 0.72f);
        }

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
        drawTextWithShadow(game.font, "Lv." + player.getLevel(), panelX + PADDING, panelY + panelHeight - PADDING, TEXT_COLOR);

        // HP
        drawTextWithShadow(game.font, "HP", barX + BAR_WIDTH + 6, barY + BAR_HEIGHT - 2, LABEL_COLOR);
        String hp = player.getHealth() + "/" + player.getMaxHealth();
        layout.setText(game.font, hp);
        drawTextWithShadow(game.font, hp, barX + BAR_WIDTH / 2 - layout.width / 2, barY + BAR_HEIGHT - 3, TEXT_COLOR);

        // Hunger
        drawTextWithShadow(game.font, "HNG", barX + BAR_WIDTH + 6, hungerY + BAR_HEIGHT - 2, LABEL_COLOR);
        String hng = player.getHunger() + "/" + player.getMaxHunger();
        layout.setText(game.font, hng);
        drawTextWithShadow(game.font, hng, barX + BAR_WIDTH / 2 - layout.width / 2, hungerY + BAR_HEIGHT - 3, TEXT_COLOR);

        // Stamina
        drawTextWithShadow(game.font, "STM", barX + BAR_WIDTH + 6, staminaY + BAR_HEIGHT - 2, LABEL_COLOR);
        String stm = (int) player.getStamina() + "/" + (int) player.getMaxStamina();
        layout.setText(game.font, stm);
        drawTextWithShadow(game.font, stm, barX + BAR_WIDTH / 2 - layout.width / 2, staminaY + BAR_HEIGHT - 3, TEXT_COLOR);

        // XP
        drawTextWithShadow(game.font, "XP " + player.getXP() + "/" + player.getXPToNextLevel(),
                barX + 4, xpY + BAR_HEIGHT * 0.7f - 2, LABEL_COLOR);

        // Floor + Enemy count (right panel)
        drawTextWithShadow(game.font, "Floor " + floor + "/" + maxFloors, infoX + 10, infoY + infoH - 12, CaveUIStyle.GOLD);
        drawTextWithShadow(game.font, "Enemies: " + enemyCount, infoX + 10, infoY + infoH - 38, CaveUIStyle.DANGER);

        // Quest tracker on the left side as one bullet per quest
        if (hasQuestTracker) {
            float titleY = questPanelY + questPanelH - 10f;
            drawTextWithShadow(game.font, "• " + questTitle, questPanelX + 10f, titleY, CaveUIStyle.MUTED_TEXT);
            BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;
            float descriptionY = titleY - game.font.getLineHeight() - 2f;
            for (int i = 0; i < questDescriptionLines.length; i++) {
                drawTextWithShadow(smallFont, questDescriptionLines[i], questPanelX + 22f,
                        descriptionY - i * smallFont.getLineHeight(), CaveUIStyle.MUTED_TEXT);
            }
        }

        // Poison text
        if (player.isPoisoned()) {
            drawTextWithShadow(game.font, "POISON", panelX + panelWidth + 14, panelY + panelHeight - 8, new Color(0.4f, 1f, 0.3f, 1f));
        }

        // Controls
        String controls = "Move  F:Use  TAB:Inv  R:Craft  J:Quests  K:Skills  ESC:Menu";
        layout.setText(game.font, controls);
        drawTextWithShadow(game.font, controls, hintX + hintW / 2 - layout.width / 2,
                PADDING + hintH / 2 + layout.height / 2, CaveUIStyle.MUTED_TEXT);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBorder(ShapeRenderer r, float x, float y, float w, float h) {
        r.setColor(CaveUIStyle.STONE_EDGE);
        CaveUIStyle.drawFrame(r, x, y, w, h, 2);
    }

    private void drawTextWithShadow(BitmapFont font, String text, float x, float y, Color color) {
        Color shadow = new Color(0f, 0f, 0f, 0.7f);
        font.setColor(shadow);
        font.draw(game.batch, text, x - 1f, y - 1f);
        font.draw(game.batch, text, x + 1f, y - 1f);
        font.draw(game.batch, text, x - 1f, y + 1f);
        font.draw(game.batch, text, x + 1f, y + 1f);
        font.setColor(color);
        font.draw(game.batch, text, x, y);
    }

    private String[] wrapQuestText(BitmapFont font, String text, float maxWidth) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return new String[] { "" };
        }

        String[] words = trimmed.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            layout.setText(font, candidate);
            if (layout.width <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    lines.add(fitWordToWidth(font, word, maxWidth));
                }
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        if (lines.isEmpty()) {
            lines.add(trimmed);
        }

        return lines.toArray(new String[0]);
    }

    private String fitWordToWidth(BitmapFont font, String word, float maxWidth) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        String candidate = word;
        layout.setText(font, candidate);
        while (layout.width > maxWidth && candidate.length() > 1) {
            candidate = candidate.substring(0, candidate.length() - 1);
            layout.setText(font, candidate + "…");
        }
        return candidate.length() < word.length() ? candidate + "…" : candidate;
    }

    private void drawBar(ShapeRenderer r, float x, float y, float w, float h,
            float pct, Color fill, Color bg) {
        CaveUIStyle.drawBar(r, x, y, w, h, pct, fill, bg);
    }

    public void dispose() {
    }
}
