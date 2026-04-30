package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.engine.InputHandler;
import com.caveadventure.entity.Companion;

public class CompanionCareUI {

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;
    private final String[] actions = { "Feed", "Play", "Pet" };
    private int selection;
    private float animTimer;

    private static final Color DEVICE_BODY = new Color(0.72f, 0.60f, 0.42f, 1f);
    private static final Color DEVICE_DARK = new Color(0.19f, 0.15f, 0.12f, 1f);
    private static final Color DEVICE_LIGHT = new Color(0.93f, 0.82f, 0.58f, 1f);
    private static final Color LCD_BG = new Color(0.53f, 0.63f, 0.46f, 1f);
    private static final Color LCD_DARK = new Color(0.08f, 0.13f, 0.10f, 1f);
    private static final Color BUTTON = new Color(0.30f, 0.16f, 0.18f, 1f);
    private static final Color BUTTON_SELECTED = new Color(0.72f, 0.24f, 0.23f, 1f);

    public CompanionCareUI(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
    }

    public boolean update(InputHandler input, Companion companion, float delta) {
        animTimer += delta;

        if (input.isKeyJustPressed(Input.Keys.ESCAPE) || input.isKeyJustPressed(Input.Keys.C)
                || input.isKeyJustPressed(Input.Keys.TAB)) {
            return true;
        }

        if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A)) {
            selection = (selection - 1 + actions.length) % actions.length;
        }
        if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D)) {
            selection = (selection + 1) % actions.length;
        }

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            switch (selection) {
                case 0:
                    companion.feed();
                    break;
                case 1:
                    companion.play();
                    break;
                case 2:
                    companion.pet();
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    public void render(Companion companion) {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float deviceW = Math.min(430f, sw - 60f);
        float deviceH = Math.min(560f, sh - 48f);
        float x = sw / 2f - deviceW / 2f;
        float y = sh / 2f - deviceH / 2f;
        float lcdX = x + 44f;
        float lcdY = y + deviceH * 0.38f;
        float lcdW = deviceW - 88f;
        float lcdH = deviceH * 0.42f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(0f, 0f, 0f, 0.58f);
        game.shapeRenderer.rect(0, 0, sw, sh);

        drawDevice(game.shapeRenderer, x, y, deviceW, deviceH);
        drawLcd(game.shapeRenderer, lcdX, lcdY, lcdW, lcdH);
        drawPet(game.shapeRenderer, companion, lcdX + lcdW * 0.50f, lcdY + lcdH * 0.42f);
        drawMeters(game.shapeRenderer, companion, x + 46f, y + 118f, deviceW - 92f);
        drawButtons(game.shapeRenderer, x, y, deviceW);

        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        drawText(companion, x, y, deviceW, deviceH, lcdX, lcdY, lcdW, lcdH);
        game.batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawDevice(ShapeRenderer r, float x, float y, float w, float h) {
        r.setColor(0f, 0f, 0f, 0.36f);
        r.rect(x + 10, y - 10, w, h);
        r.setColor(DEVICE_DARK);
        r.rect(x, y, w, h);
        r.setColor(DEVICE_BODY);
        r.rect(x + 8, y + 8, w - 16, h - 16);
        r.setColor(DEVICE_LIGHT);
        r.rect(x + 18, y + h - 26, w - 36, 7);
        r.rect(x + 18, y + 18, 7, h - 36);
        r.setColor(DEVICE_DARK);
        r.rect(x + 18, y + 18, w - 36, 6);
        r.rect(x + w - 25, y + 18, 7, h - 36);
        r.setColor(0.42f, 0.27f, 0.20f, 0.95f);
        CaveUIStyle.drawFrame(r, x + 16, y + 16, w - 32, h - 32, 3);
    }

    private void drawLcd(ShapeRenderer r, float x, float y, float w, float h) {
        r.setColor(DEVICE_DARK);
        r.rect(x - 8, y - 8, w + 16, h + 16);
        r.setColor(LCD_BG);
        r.rect(x, y, w, h);
        r.setColor(0.35f, 0.44f, 0.32f, 0.55f);
        for (int i = 0; i < 8; i++) {
            r.rect(x + 8, y + 12 + i * 22f, w - 16, 1);
        }
        for (int i = 0; i < 10; i++) {
            r.rect(x + 10 + i * 28f, y + 8, 1, h - 16);
        }
        r.setColor(0.09f, 0.13f, 0.10f, 0.85f);
        CaveUIStyle.drawFrame(r, x, y, w, h, 2);
    }

    private void drawMeters(ShapeRenderer r, Companion companion, float x, float y, float w) {
        drawMeter(r, x, y + 68, w, "LUV", companion.getLove() / 100f, CaveUIStyle.GOOD,
                new Color(0.12f, 0.23f, 0.12f, 1f));
        drawMeter(r, x, y + 44, w, "HNG", companion.getHunger() / 100f, CaveUIStyle.DANGER,
                new Color(0.25f, 0.08f, 0.06f, 1f));
        drawMeter(r, x, y + 20, w, "JOY", companion.getHappiness() / 100f, CaveUIStyle.GOLD,
                new Color(0.24f, 0.17f, 0.06f, 1f));
        drawMeter(r, x, y - 4, w, "TIR", companion.getFatigue() / 100f, new Color(0.38f, 0.62f, 0.88f, 1f),
                new Color(0.06f, 0.11f, 0.18f, 1f));
    }

    private void drawMeter(ShapeRenderer r, float x, float y, float w, String label, float pct, Color fill, Color bg) {
        r.setColor(DEVICE_DARK);
        r.rect(x, y, 38, 16);
        CaveUIStyle.drawBar(r, x + 44, y, w - 44, 16, pct, fill, bg);
    }

    private void drawButtons(ShapeRenderer r, float x, float y, float w) {
        float gap = 24f;
        float bw = (w - 96f - gap * 2f) / 3f;
        float by = y + 48f;
        for (int i = 0; i < actions.length; i++) {
            float bx = x + 48f + i * (bw + gap);
            r.setColor(i == selection ? BUTTON_SELECTED : BUTTON);
            r.ellipse(bx, by, bw, 42f);
            r.setColor(i == selection ? DEVICE_LIGHT : new Color(0.12f, 0.06f, 0.07f, 1f));
            CaveUIStyle.drawFrame(r, bx + 7, by + 8, bw - 14, 26, 2);
        }
    }

    private void drawPet(ShapeRenderer r, Companion companion, float cx, float cy) {
        float bob = (float) Math.sin(animTimer * 4f) * 3f;
        float blink = (int) (animTimer * 2.2f) % 5 == 0 ? 1f : 0f;
        r.setColor(LCD_DARK.r, LCD_DARK.g, LCD_DARK.b, 0.25f);
        r.rect(cx - 42, cy - 30, 84, 8);
        r.setColor(LCD_DARK);

        switch (companion.getPetType()) {
            case CAVE_WOLF:
                r.rect(cx - 37, cy - 10 + bob, 48, 25);
                r.rect(cx + 3, cy + 8 + bob, 30, 26);
                r.triangle(cx + 6, cy + 31 + bob, cx + 12, cy + 50 + bob, cx + 18, cy + 31 + bob);
                r.triangle(cx + 22, cy + 31 + bob, cx + 30, cy + 48 + bob, cx + 34, cy + 31 + bob);
                r.rect(cx - 29, cy - 29 + bob, 9, 22);
                r.rect(cx - 2, cy - 29 + bob, 9, 22);
                r.rect(cx + 19, cy + 20 + bob, 8, blink > 0 ? 2 : 8);
                break;
            case FIRE_SPRITE:
                r.triangle(cx, cy + 54 + bob, cx - 30, cy - 14 + bob, cx + 30, cy - 14 + bob);
                r.rect(cx - 18, cy - 22 + bob, 36, 34);
                r.triangle(cx - 18, cy + 6 + bob, cx - 40, cy + 32 + bob, cx - 8, cy + 19 + bob);
                r.triangle(cx + 18, cy + 6 + bob, cx + 42, cy + 30 + bob, cx + 8, cy + 19 + bob);
                r.setColor(LCD_BG);
                r.rect(cx - 11, cy + 4 + bob, 7, blink > 0 ? 2 : 8);
                r.rect(cx + 5, cy + 4 + bob, 7, blink > 0 ? 2 : 8);
                r.setColor(LCD_DARK);
                break;
            case SHADOW_CAT:
                r.rect(cx - 38, cy - 8 + bob, 48, 24);
                r.rect(cx + 2, cy + 10 + bob, 31, 27);
                r.triangle(cx + 6, cy + 34 + bob, cx + 12, cy + 51 + bob, cx + 18, cy + 34 + bob);
                r.triangle(cx + 23, cy + 34 + bob, cx + 31, cy + 49 + bob, cx + 35, cy + 34 + bob);
                r.rect(cx - 51, cy + 8 + bob, 20, 8);
                r.rect(cx - 60, cy + 17 + bob, 16, 8);
                r.rect(cx - 31, cy - 30 + bob, 9, 23);
                r.rect(cx - 3, cy - 30 + bob, 9, 23);
                r.setColor(LCD_BG);
                r.rect(cx + 12, cy + 24 + bob, 7, blink > 0 ? 2 : 9);
                r.rect(cx + 26, cy + 24 + bob, 7, blink > 0 ? 2 : 9);
                r.setColor(LCD_DARK);
                break;
        }
    }

    private void drawText(Companion companion, float x, float y, float w, float h,
            float lcdX, float lcdY, float lcdW, float lcdH) {
        BitmapFont font = game.font;
        BitmapFont small = game.fontSmall != null ? game.fontSmall : game.font;

        font.setColor(DEVICE_DARK);
        drawCentered(font, "CAVE PET", x, y + h - 40, w);

        small.setColor(LCD_DARK);
        drawCentered(small, companion.getPetType().name.toUpperCase(), lcdX, lcdY + lcdH - 14, lcdW);
        drawCentered(small, companion.getMoodLabel().toUpperCase(), lcdX, lcdY + 22, lcdW);

        small.setColor(DEVICE_LIGHT);
        small.draw(game.batch, "LUV", x + 52, y + 199);
        small.draw(game.batch, "HNG", x + 52, y + 175);
        small.draw(game.batch, "JOY", x + 52, y + 151);
        small.draw(game.batch, "TIR", x + 52, y + 127);

        small.setColor(DEVICE_DARK);
        drawCentered(small, companion.getCareMessage(), x + 34, y + 98, w - 68);

        for (int i = 0; i < actions.length; i++) {
            float gap = 24f;
            float bw = (w - 96f - gap * 2f) / 3f;
            float bx = x + 48f + i * (bw + gap);
            small.setColor(i == selection ? Color.WHITE : CaveUIStyle.TEXT);
            drawCentered(small, actions[i], bx, y + 75f, bw);
        }

        small.setColor(DEVICE_DARK.r, DEVICE_DARK.g, DEVICE_DARK.b, 0.76f);
        String hint = companion.getPetCooldownTimer() > 0f
                ? "Pet cooldown: " + (int) Math.ceil(companion.getPetCooldownTimer()) + "s"
                : "Left/Right select   Enter act   C/Esc close";
        drawCentered(small, hint, x + 24f, y + 26f, w - 48f);
    }

    private void drawCentered(BitmapFont font, String text, float x, float baselineY, float width) {
        layout.setText(font, text);
        font.draw(game.batch, text, x + width / 2f - layout.width / 2f, baselineY);
    }
}
