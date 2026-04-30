package com.caveadventure.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Shared old-school cave adventure UI primitives.
 */
public final class CaveUIStyle {

    public static final Color BACKDROP = new Color(0.01f, 0.01f, 0.015f, 0.74f);
    public static final Color CAVE_TOP = new Color(0.035f, 0.030f, 0.050f, 1f);
    public static final Color CAVE_BOTTOM = new Color(0.020f, 0.023f, 0.032f, 1f);
    public static final Color STONE_DARK = new Color(0.050f, 0.052f, 0.060f, 1f);
    public static final Color STONE = new Color(0.115f, 0.105f, 0.095f, 1f);
    public static final Color STONE_LIGHT = new Color(0.220f, 0.205f, 0.175f, 1f);
    public static final Color STONE_EDGE = new Color(0.350f, 0.285f, 0.180f, 1f);
    public static final Color EMBER = new Color(0.95f, 0.55f, 0.18f, 1f);
    public static final Color GOLD = new Color(0.95f, 0.76f, 0.34f, 1f);
    public static final Color TEXT = new Color(0.88f, 0.82f, 0.68f, 1f);
    public static final Color MUTED_TEXT = new Color(0.58f, 0.54f, 0.45f, 1f);
    public static final Color DISABLED_TEXT = new Color(0.29f, 0.27f, 0.23f, 0.72f);
    public static final Color DANGER = new Color(0.82f, 0.22f, 0.14f, 1f);
    public static final Color GOOD = new Color(0.34f, 0.78f, 0.36f, 1f);

    private CaveUIStyle() {
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
            r.setColor(0.022f, 0.020f, 0.032f, 1f);
            r.triangle(x, height, x + w, height, x + w * 0.48f, height - h);
        }

        for (int i = 0; i < 18; i++) {
            float x = i * (width / 18f);
            float h = 18f + (float) Math.sin(i * 1.3f + animTimer * 0.3f) * 8f + (i % 3) * 5f;
            r.setColor(0.030f, 0.027f, 0.034f, 1f);
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
        r.setColor(0f, 0f, 0f, 0.34f * alpha);
        r.rect(x + 7, y - 7, w, h);

        r.setColor(STONE_DARK.r, STONE_DARK.g, STONE_DARK.b, 0.97f * alpha);
        r.rect(x, y, w, h);

        r.setColor(STONE.r, STONE.g, STONE.b, 0.98f * alpha);
        r.rect(x + 5, y + 5, w - 10, h - 10);

        r.setColor(STONE_LIGHT.r, STONE_LIGHT.g, STONE_LIGHT.b, 0.42f * alpha);
        r.rect(x + 5, y + h - 8, w - 10, 3);
        r.rect(x + 5, y + 5, 3, h - 10);

        r.setColor(0.018f, 0.017f, 0.020f, 0.85f * alpha);
        r.rect(x + 5, y + 5, w - 10, 3);
        r.rect(x + w - 8, y + 5, 3, h - 10);

        r.setColor(STONE_EDGE.r, STONE_EDGE.g, STONE_EDGE.b, 0.74f * alpha);
        drawFrame(r, x, y, w, h, 2);

        drawStoneChips(r, x + 10, y + 10, w - 20, h - 20, alpha);
    }

    public static void drawInsetPanel(ShapeRenderer r, float x, float y, float w, float h, float alpha) {
        r.setColor(0.035f, 0.034f, 0.040f, 0.90f * alpha);
        r.rect(x, y, w, h);
        r.setColor(0.18f, 0.16f, 0.13f, 0.78f * alpha);
        drawFrame(r, x, y, w, h, 2);
        r.setColor(0f, 0f, 0f, 0.30f * alpha);
        r.rect(x + 3, y + 3, w - 6, 3);
        r.rect(x + 3, y + 3, 3, h - 6);
    }

    public static void drawSelection(ShapeRenderer r, float x, float y, float w, float h, float alpha) {
        r.setColor(0.58f, 0.34f, 0.10f, 0.55f * alpha);
        r.rect(x, y, w, h);
        r.setColor(GOLD.r, GOLD.g, GOLD.b, 0.90f * alpha);
        r.rect(x, y, 4, h);
        r.rect(x + w - 4, y, 2, h);
    }

    public static void drawCarvedSeparator(ShapeRenderer r, float x, float y, float w, float alpha) {
        r.setColor(0.025f, 0.022f, 0.020f, 0.80f * alpha);
        r.rect(x, y - 1, w, 2);
        r.setColor(STONE_LIGHT.r, STONE_LIGHT.g, STONE_LIGHT.b, 0.28f * alpha);
        r.rect(x, y + 1, w, 1);
    }

    public static void drawTorch(ShapeRenderer r, float x, float y, float scale, float alpha, float animTimer) {
        r.setColor(0.30f, 0.16f, 0.07f, alpha);
        r.rect(x + 4 * scale, y, 4 * scale, 19 * scale);
        r.setColor(0.95f, 0.32f, 0.08f, 0.26f * alpha);
        r.ellipse(x - 7 * scale, y + 11 * scale, 26 * scale, 31 * scale);
        float pulse = 0.85f + (float) Math.sin(animTimer * 8f) * 0.15f;
        r.setColor(0.95f, 0.28f, 0.06f, alpha);
        r.triangle(x + 2 * scale, y + 15 * scale, x + 10 * scale, y + 15 * scale,
                x + 6 * scale, y + 31 * scale * pulse);
        r.setColor(1f, 0.78f, 0.24f, alpha);
        r.triangle(x + 4 * scale, y + 15 * scale, x + 8 * scale, y + 15 * scale,
                x + 6 * scale, y + 25 * scale * pulse);
    }

    public static void drawBar(ShapeRenderer r, float x, float y, float w, float h, float pct,
            Color fill, Color bg) {
        float safePct = Math.max(0f, Math.min(1f, pct));
        r.setColor(0.020f, 0.018f, 0.016f, 0.95f);
        r.rect(x, y, w, h);
        r.setColor(bg);
        r.rect(x + 2, y + 2, w - 4, h - 4);
        r.setColor(fill);
        r.rect(x + 3, y + 3, (w - 6) * safePct, h - 6);
        r.setColor(1f, 0.86f, 0.52f, 0.12f);
        r.rect(x + 3, y + h / 2f, (w - 6) * safePct, h / 2f - 3);
        r.setColor(STONE_EDGE.r, STONE_EDGE.g, STONE_EDGE.b, 0.7f);
        drawFrame(r, x, y, w, h, 1);
    }

    public static void drawFrame(ShapeRenderer r, float x, float y, float w, float h, float thickness) {
        r.rect(x, y, w, thickness);
        r.rect(x, y + h - thickness, w, thickness);
        r.rect(x, y, thickness, h);
        r.rect(x + w - thickness, y, thickness, h);
    }

    private static void drawStoneChips(ShapeRenderer r, float x, float y, float w, float h, float alpha) {
        r.setColor(0.22f, 0.20f, 0.17f, 0.22f * alpha);
        for (int i = 0; i < 9; i++) {
            float cx = x + ((i * 47) % Math.max(1f, w - 14));
            float cy = y + ((i * 29) % Math.max(1f, h - 8));
            r.rect(cx, cy, 10 + (i % 3) * 4, 1);
        }
    }
}
