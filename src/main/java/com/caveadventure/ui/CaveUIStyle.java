package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.caveadventure.world.Palette;

/**
 * Shared old-school cave adventure UI primitives. Colours are {@link Palette} entries.
 * <p>
 * Panels, insets, selections and bar frames are drawn from the pixel {@link UiSkin} when one
 * is installed with {@link #useSkin}. Callers keep passing their active {@link ShapeRenderer}:
 * the skin is drawn by briefly pausing it, so draw order and the call sites stay unchanged.
 * Without a skin (headless tests, missing files) everything falls back to shape drawing.
 */
public final class CaveUIStyle {

    public static final Color BACKDROP = Palette.alpha(Palette.VOID, 0.74f);
    public static final Color CAVE_TOP = new Color(Palette.NIGHT);
    public static final Color CAVE_BOTTOM = new Color(Palette.VOID);
    public static final Color STONE_DARK = new Color(Palette.INK);
    public static final Color STONE = new Color(Palette.CHARCOAL);
    public static final Color STONE_LIGHT = new Color(Palette.STONE_DARK);
    public static final Color STONE_EDGE = new Color(Palette.STONE_LIGHT);
    public static final Color EMBER = new Color(Palette.ORANGE);
    public static final Color GOLD = new Color(Palette.GOLD);
    public static final Color TEXT = new Color(Palette.BONE);
    public static final Color MUTED_TEXT = new Color(Palette.STONE_PALE);
    public static final Color DISABLED_TEXT = Palette.alpha(Palette.STONE_LIGHT, 0.72f);
    public static final Color DANGER = new Color(Palette.RED);
    public static final Color GOOD = new Color(Palette.GREEN);

    /** A selection narrower than this many cursor widths (a tab) is drawn without the cursor. */
    private static final int CURSOR_MIN_WIDTH_FACTOR = 8;

    private static UiSkin skin;
    private static SpriteBatch skinBatch;
    private static final Matrix4 savedProjection = new Matrix4();
    private static final Matrix4 savedTransform = new Matrix4();

    private CaveUIStyle() {
    }

    /**
     * Installs the pixel skin and the batch it is drawn with; {@code null} restores shape drawing.
     */
    public static void useSkin(UiSkin uiSkin, SpriteBatch batch) {
        skin = uiSkin;
        skinBatch = batch;
    }

    /** The installed skin, or {@code null}. */
    public static UiSkin getSkin() {
        return skin;
    }

    public static void drawCaveBackground(ShapeRenderer r, float width, float height, float animTimer) {
        r.setColor(CAVE_BOTTOM);
        r.rect(0, 0, width, height * 0.52f);
        r.setColor(CAVE_TOP);
        r.rect(0, height * 0.52f, width, height * 0.48f);

        for (int i = 0; i < 20; i++) {
            float x = i * (width / 20f);
            float w = width / 20f + 2f;
            float h = 34f + (float) Math.sin(i * 1.7f) * 22f + (i % 4) * 10f;
            r.setColor(Palette.VOID);
            r.triangle(x, height, x + w, height, x + w * 0.48f, height - h);
        }

        for (int i = 0; i < 18; i++) {
            float x = i * (width / 18f);
            float h = 18f + (float) Math.sin(i * 1.3f + animTimer * 0.3f) * 8f + (i % 3) * 5f;
            r.setColor(Palette.NIGHT);
            r.triangle(x, 0, x + width / 18f, 0, x + width / 36f, h);
        }
    }

    public static void drawDust(ShapeRenderer r, float[] x, float[] y, float[] size, float animTimer) {
        for (int i = 0; i < x.length; i++) {
            float alpha = 0.18f + (float) Math.sin(animTimer * 1.7f + i) * 0.08f;
            r.setColor(EMBER.r, EMBER.g, EMBER.b, alpha);
            r.rect(x[i], y[i], size[i], size[i]);
        }
    }

    public static void drawStonePanel(ShapeRenderer r, float x, float y, float w, float h, float alpha) {
        if (beginSkin(r, alpha)) {
            skin.drawPanel(skinBatch, x, y, w, h);
            endSkin(r);
            return;
        }
        r.setColor(Palette.VOID.r, Palette.VOID.g, Palette.VOID.b, 0.34f * alpha);
        r.rect(x + 7, y - 7, w, h);

        r.setColor(STONE_DARK.r, STONE_DARK.g, STONE_DARK.b, 0.97f * alpha);
        r.rect(x, y, w, h);

        r.setColor(STONE.r, STONE.g, STONE.b, 0.98f * alpha);
        r.rect(x + 5, y + 5, w - 10, h - 10);

        r.setColor(STONE_LIGHT.r, STONE_LIGHT.g, STONE_LIGHT.b, 0.42f * alpha);
        r.rect(x + 5, y + h - 8, w - 10, 3);
        r.rect(x + 5, y + 5, 3, h - 10);

        r.setColor(Palette.VOID.r, Palette.VOID.g, Palette.VOID.b, 0.85f * alpha);
        r.rect(x + 5, y + 5, w - 10, 3);
        r.rect(x + w - 8, y + 5, 3, h - 10);

        r.setColor(STONE_EDGE.r, STONE_EDGE.g, STONE_EDGE.b, 0.74f * alpha);
        drawFrame(r, x, y, w, h, 2);
    }

    public static void drawInsetPanel(ShapeRenderer r, float x, float y, float w, float h, float alpha) {
        if (beginSkin(r, alpha)) {
            skin.drawInset(skinBatch, x, y, w, h);
            endSkin(r);
            return;
        }
        r.setColor(Palette.NIGHT.r, Palette.NIGHT.g, Palette.NIGHT.b, 0.90f * alpha);
        r.rect(x, y, w, h);
        r.setColor(Palette.STONE_DARK.r, Palette.STONE_DARK.g, Palette.STONE_DARK.b, 0.78f * alpha);
        drawFrame(r, x, y, w, h, 2);
    }

    /**
     * Highlight behind a selected row: the skin's gold-rimmed frame with the ▶ cursor just left
     * of it, or a shape fill and gold edge without a skin. Small selections (tabs) get no cursor.
     */
    public static void drawSelection(ShapeRenderer r, float x, float y, float w, float h, float alpha) {
        if (beginSkin(r, 0.85f * alpha)) {
            skin.drawSelection(skinBatch, x, y, w, h);
            skinBatch.setColor(1f, 1f, 1f, alpha);
            int cw = skin.getCursorWidth();
            int ch = skin.getCursorHeight();
            if (h >= ch && w >= cw * CURSOR_MIN_WIDTH_FACTOR)
                skin.drawCursor(skinBatch, x - cw - skin.getScale(), y + (h - ch) / 2f);
            endSkin(r);
            return;
        }
        r.setColor(Palette.RUST.r, Palette.RUST.g, Palette.RUST.b, 0.55f * alpha);
        r.rect(x, y, w, h);
        r.setColor(GOLD.r, GOLD.g, GOLD.b, 0.90f * alpha);
        r.rect(x, y, 4, h);
        r.rect(x + w - 4, y, 2, h);
    }

    public static void drawCarvedSeparator(ShapeRenderer r, float x, float y, float w, float alpha) {
        int px = skin != null ? skin.getScale() : 1;
        r.setColor(Palette.INK.r, Palette.INK.g, Palette.INK.b, 0.80f * alpha);
        r.rect(Math.round(x), Math.round(y) - px, Math.round(w), px);
        r.setColor(STONE_EDGE.r, STONE_EDGE.g, STONE_EDGE.b, 0.5f * alpha);
        r.rect(Math.round(x), Math.round(y), Math.round(w), px);
    }

    public static void drawTorch(ShapeRenderer r, float x, float y, float scale, float alpha, float animTimer) {
        r.setColor(Palette.UMBER.r, Palette.UMBER.g, Palette.UMBER.b, alpha);
        r.rect(x + 4 * scale, y, 4 * scale, 19 * scale);
        r.setColor(Palette.RED.r, Palette.RED.g, Palette.RED.b, 0.26f * alpha);
        r.ellipse(x - 7 * scale, y + 11 * scale, 26 * scale, 31 * scale);
        float pulse = 0.85f + (float) Math.sin(animTimer * 8f) * 0.15f;
        r.setColor(Palette.RED.r, Palette.RED.g, Palette.RED.b, alpha);
        r.triangle(x + 2 * scale, y + 15 * scale, x + 10 * scale, y + 15 * scale,
                x + 6 * scale, y + 31 * scale * pulse);
        r.setColor(Palette.GOLD.r, Palette.GOLD.g, Palette.GOLD.b, alpha);
        r.triangle(x + 4 * scale, y + 15 * scale, x + 8 * scale, y + 15 * scale,
                x + 6 * scale, y + 25 * scale * pulse);
    }

    /**
     * A bar on whole UI pixels: a one-pixel ink frame with a stone corner light, a flat palette
     * fill and a one-pixel highlight row. Thin enough for the short battle bars too.
     */
    public static void drawBar(ShapeRenderer r, float x, float y, float w, float h, float pct,
            Color fill, Color bg) {
        float safePct = Math.max(0f, Math.min(1f, pct));
        if (skin != null) {
            int px = skin.getScale();
            int bx = Math.round(x);
            int by = Math.round(y);
            int bw = Math.round(w);
            int bh = Math.round(h);
            r.setColor(Palette.INK);
            r.rect(bx, by, bw, bh);
            r.setColor(STONE_EDGE);
            r.rect(bx + px, by + bh - px, bw - 2 * px, px);
            int ix = bx + px;
            int iy = by + px;
            int iw = bw - 2 * px;
            int ih = bh - 2 * px - (bh >= 5 * px ? px : 0);
            if (iw <= 0 || ih <= 0)
                return;
            r.setColor(bg);
            r.rect(ix, iy, iw, ih);
            int fw = Math.round(iw * safePct);
            r.setColor(fill);
            r.rect(ix, iy, fw, ih);
            if (ih >= 3 * px) {
                r.setColor(Palette.WHITE.r, Palette.WHITE.g, Palette.WHITE.b, 0.22f);
                r.rect(ix, iy + ih - px, fw, px);
            }
            return;
        }
        r.setColor(Palette.VOID.r, Palette.VOID.g, Palette.VOID.b, 0.95f);
        r.rect(x, y, w, h);
        r.setColor(bg);
        r.rect(x + 2, y + 2, w - 4, h - 4);
        r.setColor(fill);
        r.rect(x + 3, y + 3, (w - 6) * safePct, h - 6);
        r.setColor(STONE_EDGE.r, STONE_EDGE.g, STONE_EDGE.b, 0.7f);
        drawFrame(r, x, y, w, h, 1);
    }

    public static void drawFrame(ShapeRenderer r, float x, float y, float w, float h, float thickness) {
        r.rect(x, y, w, thickness);
        r.rect(x, y + h - thickness, w, thickness);
        r.rect(x, y, thickness, h);
        r.rect(x + w - thickness, y, thickness, h);
    }

    // --- Skin drawing inside an active ShapeRenderer ---

    private static ShapeRenderer.ShapeType pausedType;
    private static boolean pausedBlend;

    /**
     * Pauses {@code r} and starts the skin batch with the renderer's matrices, or returns false
     * (draw shapes instead) when no skin is installed or the batch is already in use.
     */
    private static boolean beginSkin(ShapeRenderer r, float alpha) {
        if (skin == null || skinBatch == null || skinBatch.isDrawing() || !r.isDrawing())
            return false;
        pausedType = r.getCurrentType();
        pausedBlend = Gdx.gl.glIsEnabled(GL20.GL_BLEND);
        r.end();
        savedProjection.set(skinBatch.getProjectionMatrix());
        savedTransform.set(skinBatch.getTransformMatrix());
        skinBatch.setProjectionMatrix(r.getProjectionMatrix());
        skinBatch.setTransformMatrix(r.getTransformMatrix());
        skinBatch.begin();
        skinBatch.setColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
        return true;
    }

    /** Ends the skin batch, restores its matrices and blending, and resumes {@code r}. */
    private static void endSkin(ShapeRenderer r) {
        skinBatch.end();
        skinBatch.setColor(Color.WHITE);
        skinBatch.setProjectionMatrix(savedProjection);
        skinBatch.setTransformMatrix(savedTransform);
        if (pausedBlend)
            Gdx.gl.glEnable(GL20.GL_BLEND);
        r.begin(pausedType);
    }
}
