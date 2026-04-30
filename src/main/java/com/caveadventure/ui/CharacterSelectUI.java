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
import com.caveadventure.entity.CharacterAppearance;

/**
 * Character setup shown before a new run.
 */
public class CharacterSelectUI {

    public static final int NONE = 0;
    public static final int START = 1;
    public static final int BACK = 2;

    private static final String[] FIELDS = { "Tunic", "Skin", "Hair", "Pants", "Boots", "Cape" };
    private static final Color[] TUNIC_COLORS = {
            new Color(0.18f, 0.56f, 0.90f, 1f),
            new Color(0.76f, 0.24f, 0.12f, 1f),
            new Color(0.24f, 0.55f, 0.30f, 1f),
            new Color(0.43f, 0.72f, 0.92f, 1f),
            new Color(0.45f, 0.28f, 0.72f, 1f),
            new Color(0.82f, 0.62f, 0.20f, 1f)
    };
    private static final Color[] SKIN_COLORS = {
            new Color(0.86f, 0.66f, 0.48f, 1f),
            new Color(0.78f, 0.55f, 0.36f, 1f),
            new Color(0.63f, 0.45f, 0.30f, 1f),
            new Color(0.82f, 0.68f, 0.55f, 1f),
            new Color(0.70f, 0.48f, 0.36f, 1f),
            new Color(0.52f, 0.35f, 0.24f, 1f)
    };
    private static final Color[] HAIR_COLORS = {
            new Color(0.16f, 0.10f, 0.07f, 1f),
            new Color(0.28f, 0.13f, 0.05f, 1f),
            new Color(0.08f, 0.12f, 0.07f, 1f),
            new Color(0.80f, 0.82f, 0.78f, 1f),
            new Color(0.05f, 0.04f, 0.08f, 1f),
            new Color(0.50f, 0.34f, 0.12f, 1f)
    };
    private static final Color[] PANTS_COLORS = {
            new Color(0.10f, 0.16f, 0.26f, 1f),
            new Color(0.18f, 0.12f, 0.10f, 1f),
            new Color(0.14f, 0.22f, 0.16f, 1f),
            new Color(0.13f, 0.18f, 0.27f, 1f),
            new Color(0.12f, 0.10f, 0.18f, 1f),
            new Color(0.22f, 0.20f, 0.18f, 1f)
    };
    private static final Color[] BOOT_COLORS = {
            new Color(0.08f, 0.06f, 0.05f, 1f),
            new Color(0.10f, 0.06f, 0.04f, 1f),
            new Color(0.07f, 0.07f, 0.05f, 1f),
            new Color(0.09f, 0.10f, 0.13f, 1f),
            new Color(0.05f, 0.04f, 0.06f, 1f),
            new Color(0.17f, 0.14f, 0.10f, 1f)
    };
    private static final Color[] CAPE_COLORS = {
            new Color(0.09f, 0.05f, 0.11f, 1f),
            new Color(0.34f, 0.08f, 0.04f, 1f),
            new Color(0.08f, 0.18f, 0.10f, 1f),
            new Color(0.12f, 0.22f, 0.34f, 1f),
            new Color(0.18f, 0.08f, 0.26f, 1f),
            new Color(0.28f, 0.22f, 0.12f, 1f)
    };

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;

    private int selectedPreset;
    private boolean customizing;
    private int selectedField;
    private int tunicIndex;
    private int skinIndex;
    private int hairIndex;
    private int pantsIndex;
    private int bootIndex;
    private int capeIndex;
    private float animTimer;
    private CharacterAppearance customAppearance;

    public CharacterSelectUI(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        reset();
    }

    public void reset() {
        selectedPreset = 0;
        customizing = false;
        selectedField = 0;
        seedCustomFromPreset(0);
        animTimer = 0f;
    }

    public int update(InputHandler input, float delta) {
        animTimer += delta;

        if (input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (customizing) {
                customizing = false;
                return NONE;
            }
            return BACK;
        }

        if (input.isKeyJustPressed(Input.Keys.C)) {
            if (!customizing)
                seedCustomFromPreset(selectedPreset);
            customizing = !customizing;
            return NONE;
        }

        if (customizing) {
            if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
                selectedField = (selectedField - 1 + FIELDS.length) % FIELDS.length;
            if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
                selectedField = (selectedField + 1) % FIELDS.length;
            if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A))
                changeCustomColor(-1);
            if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D))
                changeCustomColor(1);
        } else {
            if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A))
                selectedPreset = (selectedPreset - 1 + CharacterAppearance.presetCount()) % CharacterAppearance.presetCount();
            if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D))
                selectedPreset = (selectedPreset + 1) % CharacterAppearance.presetCount();
        }

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE))
            return START;

        return NONE;
    }

    public CharacterAppearance getSelectedAppearance() {
        return customizing ? customAppearance.copy() : CharacterAppearance.getPreset(selectedPreset);
    }

    public void render() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        CharacterAppearance active = getSelectedAppearance();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawBackground(sw, sh);
        drawPreviewPanel(sw, sh, active);
        drawPresetStrip(sw, sh);
        if (customizing)
            drawCustomizer(sw, sh);
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        drawText(sw, sh, active);
        game.batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void seedCustomFromPreset(int presetIndex) {
        CharacterAppearance preset = CharacterAppearance.getPreset(presetIndex);
        tunicIndex = findClosest(TUNIC_COLORS, preset.getTunicColor());
        skinIndex = findClosest(SKIN_COLORS, preset.getSkinColor());
        hairIndex = findClosest(HAIR_COLORS, preset.getHairColor());
        pantsIndex = findClosest(PANTS_COLORS, preset.getPantsColor());
        bootIndex = findClosest(BOOT_COLORS, preset.getBootColor());
        capeIndex = findClosest(CAPE_COLORS, preset.getCapeColor());
        applyCustomColors();
    }

    private void changeCustomColor(int delta) {
        switch (selectedField) {
            case 0:
                tunicIndex = wrap(tunicIndex + delta, TUNIC_COLORS.length);
                break;
            case 1:
                skinIndex = wrap(skinIndex + delta, SKIN_COLORS.length);
                break;
            case 2:
                hairIndex = wrap(hairIndex + delta, HAIR_COLORS.length);
                break;
            case 3:
                pantsIndex = wrap(pantsIndex + delta, PANTS_COLORS.length);
                break;
            case 4:
                bootIndex = wrap(bootIndex + delta, BOOT_COLORS.length);
                break;
            case 5:
                capeIndex = wrap(capeIndex + delta, CAPE_COLORS.length);
                break;
            default:
                break;
        }
        applyCustomColors();
    }

    private void applyCustomColors() {
        customAppearance = new CharacterAppearance("Custom Delver",
                TUNIC_COLORS[tunicIndex], SKIN_COLORS[skinIndex], HAIR_COLORS[hairIndex],
                PANTS_COLORS[pantsIndex], BOOT_COLORS[bootIndex], CAPE_COLORS[capeIndex]);
    }

    private int wrap(int value, int length) {
        return (value + length) % length;
    }

    private int findClosest(Color[] colors, Color target) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < colors.length; i++) {
            float dr = colors[i].r - target.r;
            float dg = colors[i].g - target.g;
            float db = colors[i].b - target.b;
            float distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private void drawBackground(float sw, float sh) {
        CaveUIStyle.drawCaveBackground(game.shapeRenderer, sw, sh, animTimer);

        for (int i = 0; i < 26; i++) {
            float x = (i * 73f + 19f) % sw;
            float y = (i * 41f + animTimer * (12f + i % 5)) % sh;
            float alpha = 0.16f + (float) Math.sin(animTimer * 2f + i) * 0.06f;
            game.shapeRenderer.setColor(CaveUIStyle.EMBER.r, CaveUIStyle.EMBER.g, CaveUIStyle.EMBER.b, alpha);
            game.shapeRenderer.rect(x, y, 2 + i % 3, 2 + i % 3);
        }
    }

    private void drawPreviewPanel(float sw, float sh, CharacterAppearance active) {
        float panelX = sw * 0.08f;
        float panelY = sh * 0.17f;
        float panelW = sw * 0.37f;
        float panelH = sh * 0.66f;

        CaveUIStyle.drawStonePanel(game.shapeRenderer, panelX, panelY, panelW, panelH, 0.94f);

        game.shapeRenderer.setColor(0f, 0f, 0f, 0.24f);
        game.shapeRenderer.ellipse(panelX + panelW * 0.5f - 88, panelY + 62, 176, 34);
        drawCharacterPreview(game.shapeRenderer, panelX + panelW * 0.5f - 82, panelY + 86, 5.1f, active);
    }

    private void drawPresetStrip(float sw, float sh) {
        float startX = sw * 0.50f;
        float y = sh * 0.17f;
        float slotW = sw * 0.075f;
        float slotH = 104f;
        float gap = sw * 0.014f;

        for (int i = 0; i < CharacterAppearance.presetCount(); i++) {
            float x = startX + i * (slotW + gap);
            boolean selected = !customizing && i == selectedPreset;
            CaveUIStyle.drawStonePanel(game.shapeRenderer, x, y, slotW, slotH, selected ? 1f : 0.74f);
            if (selected)
                CaveUIStyle.drawSelection(game.shapeRenderer, x + 4, y + 4, slotW - 8, slotH - 8, 1f);
            drawCharacterPreview(game.shapeRenderer, x + slotW / 2f - 24, y + 34, 1.5f,
                    CharacterAppearance.getPreset(i));
        }
    }

    private void drawCustomizer(float sw, float sh) {
        float panelX = sw * 0.50f;
        float panelY = sh * 0.35f;
        float panelW = sw * 0.40f;
        float panelH = sh * 0.38f;

        CaveUIStyle.drawStonePanel(game.shapeRenderer, panelX, panelY, panelW, panelH, 0.94f);

        float rowY = panelY + panelH - 78;
        float rowGap = 31f;
        drawSwatches(panelX + 104, rowY, TUNIC_COLORS, tunicIndex, selectedField == 0);
        drawSwatches(panelX + 104, rowY - rowGap, SKIN_COLORS, skinIndex, selectedField == 1);
        drawSwatches(panelX + 104, rowY - rowGap * 2, HAIR_COLORS, hairIndex, selectedField == 2);
        drawSwatches(panelX + 104, rowY - rowGap * 3, PANTS_COLORS, pantsIndex, selectedField == 3);
        drawSwatches(panelX + 104, rowY - rowGap * 4, BOOT_COLORS, bootIndex, selectedField == 4);
        drawSwatches(panelX + 104, rowY - rowGap * 5, CAPE_COLORS, capeIndex, selectedField == 5);
    }

    private void drawSwatches(float x, float y, Color[] colors, int selectedIndex, boolean activeRow) {
        for (int i = 0; i < colors.length; i++) {
            float sx = x + i * 28;
            game.shapeRenderer.setColor(colors[i]);
            game.shapeRenderer.rect(sx, y, 20, 20);
            if (i == selectedIndex || activeRow) {
                game.shapeRenderer.setColor(i == selectedIndex ? CaveUIStyle.GOLD.r : CaveUIStyle.STONE_EDGE.r,
                        i == selectedIndex ? CaveUIStyle.GOLD.g : CaveUIStyle.STONE_EDGE.g,
                        i == selectedIndex ? CaveUIStyle.GOLD.b : CaveUIStyle.STONE_EDGE.b,
                        i == selectedIndex ? 1f : 0.7f);
                drawFrame(sx - 2, y - 2, 24, 24, i == selectedIndex ? 2 : 1);
            }
        }
    }

    private void drawText(float sw, float sh, CharacterAppearance active) {
        BitmapFont titleFont = game.fontLarge != null ? game.fontLarge : game.font;
        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        titleFont.setColor(CaveUIStyle.GOLD);
        layout.setText(titleFont, "Choose Your Adventurer");
        titleFont.draw(game.batch, "Choose Your Adventurer", sw / 2f - layout.width / 2f, sh - 58);

        normalFont.setColor(CaveUIStyle.TEXT);
        layout.setText(normalFont, active.getName());
        normalFont.draw(game.batch, active.getName(), sw * 0.265f - layout.width / 2f, sh * 0.79f);

        smallFont.setColor(CaveUIStyle.MUTED_TEXT);
        String modeText = customizing ? "Customize Mode" : "Preset Selection";
        layout.setText(smallFont, modeText);
        smallFont.draw(game.batch, modeText, sw * 0.265f - layout.width / 2f, sh * 0.735f);

        for (int i = 0; i < CharacterAppearance.presetCount(); i++) {
            CharacterAppearance preset = CharacterAppearance.getPreset(i);
            float slotW = sw * 0.075f;
            float gap = sw * 0.014f;
            float x = sw * 0.50f + i * (slotW + gap);
            smallFont.setColor(!customizing && i == selectedPreset ? CaveUIStyle.GOLD : CaveUIStyle.MUTED_TEXT);
            drawCentered(smallFont, preset.getName(), x, sh * 0.17f + 22, slotW);
        }

        if (customizing)
            drawCustomizerText(sw, sh, smallFont, normalFont);

        smallFont.setColor(CaveUIStyle.MUTED_TEXT);
        String hints = customizing
                ? "Up/Down: Field   Left/Right: Color   C: Presets   Enter: Begin   Esc: Back"
                : "Left/Right: Choose   C: Customize   Enter: Begin   Esc: Back";
        layout.setText(smallFont, hints);
        smallFont.draw(game.batch, hints, sw / 2f - layout.width / 2f, 38);
    }

    private void drawCustomizerText(float sw, float sh, BitmapFont smallFont, BitmapFont normalFont) {
        float panelX = sw * 0.50f;
        float panelY = sh * 0.35f;
        float panelH = sh * 0.38f;
        float rowY = panelY + panelH - 62;
        float rowGap = 31f;

        normalFont.setColor(CaveUIStyle.GOLD);
        normalFont.draw(game.batch, "Customize", panelX + 18, panelY + panelH - 22);

        for (int i = 0; i < FIELDS.length; i++) {
            smallFont.setColor(i == selectedField ? CaveUIStyle.GOLD : CaveUIStyle.TEXT);
            smallFont.draw(game.batch, (i == selectedField ? "> " : "  ") + FIELDS[i], panelX + 20,
                    rowY - i * rowGap + 17);
        }
    }

    private void drawCentered(BitmapFont font, String text, float x, float y, float width) {
        layout.setText(font, text);
        font.draw(game.batch, text, x + width / 2f - layout.width / 2f, y);
    }

    private void drawCharacterPreview(ShapeRenderer r, float x, float y, float scale, CharacterAppearance a) {
        Color outline = new Color(0.04f, 0.05f, 0.07f, 1f);
        Color tunic = a.getTunicColor();
        Color tunicDark = a.getTunicAltColor();

        r.setColor(0f, 0f, 0f, 0.30f);
        r.ellipse(x + 5 * scale, y - 2 * scale, 24 * scale, 8 * scale);

        r.setColor(outline);
        r.rect(x + 8 * scale, y + 3 * scale, 6 * scale, 10 * scale);
        r.rect(x + 18 * scale, y + 3 * scale, 6 * scale, 10 * scale);
        r.setColor(a.getPantsColor());
        r.rect(x + 9 * scale, y + 5 * scale, 4 * scale, 8 * scale);
        r.rect(x + 19 * scale, y + 5 * scale, 4 * scale, 8 * scale);
        r.setColor(a.getBootColor());
        r.rect(x + 7 * scale, y + 2 * scale, 8 * scale, 4 * scale);
        r.rect(x + 17 * scale, y + 2 * scale, 8 * scale, 4 * scale);

        r.setColor(a.getCapeColor().r, a.getCapeColor().g, a.getCapeColor().b, 0.9f);
        r.triangle(x + 8 * scale, y + 12 * scale, x + 16 * scale, y + 27 * scale,
                x + 5 * scale, y + 8 * scale);

        r.setColor(outline);
        r.rect(x + 7 * scale, y + 10 * scale, 18 * scale, 15 * scale);
        r.rect(x + 4 * scale, y + 11 * scale, 5 * scale, 12 * scale);
        r.rect(x + 23 * scale, y + 11 * scale, 5 * scale, 12 * scale);
        r.setColor(tunic);
        r.rect(x + 8 * scale, y + 11 * scale, 16 * scale, 13 * scale);
        r.rect(x + 5 * scale, y + 12 * scale, 4 * scale, 10 * scale);
        r.rect(x + 23 * scale, y + 12 * scale, 4 * scale, 10 * scale);
        r.setColor(tunicDark);
        r.rect(x + 8 * scale, y + 11 * scale, 16 * scale, 3 * scale);
        r.setColor(0.72f, 0.52f, 0.20f, 1f);
        r.rect(x + 8 * scale, y + 13 * scale, 16 * scale, 2 * scale);

        r.setColor(outline);
        r.rect(x + 8 * scale, y + 21 * scale, 16 * scale, 10 * scale);
        r.rect(x + 10 * scale, y + 19 * scale, 12 * scale, 3 * scale);
        r.setColor(a.getSkinColor());
        r.rect(x + 9 * scale, y + 21 * scale, 14 * scale, 8 * scale);
        r.rect(x + 11 * scale, y + 19 * scale, 10 * scale, 3 * scale);
        r.setColor(a.getHairColor());
        r.rect(x + 8 * scale, y + 27 * scale, 16 * scale, 4 * scale);
        r.rect(x + 8 * scale, y + 24 * scale, 3 * scale, 5 * scale);
        r.setColor(1f, 1f, 1f, 1f);
        r.rect(x + 11 * scale, y + 24 * scale, 4 * scale, 3 * scale);
        r.rect(x + 18 * scale, y + 24 * scale, 4 * scale, 3 * scale);
        r.setColor(0.06f, 0.08f, 0.12f, 1f);
        r.rect(x + 12 * scale, y + 24 * scale, 2 * scale, 2 * scale);
        r.rect(x + 19 * scale, y + 24 * scale, 2 * scale, 2 * scale);
    }

    private void drawFrame(float x, float y, float width, float height, float thickness) {
        game.shapeRenderer.rect(x, y, width, thickness);
        game.shapeRenderer.rect(x, y + height - thickness, width, thickness);
        game.shapeRenderer.rect(x, y, thickness, height);
        game.shapeRenderer.rect(x + width - thickness, y, thickness, height);
    }
}
