package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

/**
 * Floating damage number that rises and fades out.
 */
public class DamageNumber {

    private float x, y;
    private final String text;
    private final float colorR;
    private final float colorG;
    private final float colorB;
    private final float colorA;
    private float timer;
    private static final float DURATION = 1.0f;
    private static final float RISE_SPEED = 40f;

    private static final float CRIT_R = 1f;
    private static final float CRIT_G = 0.9f;
    private static final float CRIT_B = 0.1f;
    private static final float NORMAL_R = 1f;
    private static final float NORMAL_G = 0.3f;
    private static final float NORMAL_B = 0.2f;
    private static final float BASE_SCALE = 1.3f;

    private static final GlyphLayout SHARED_LAYOUT = new GlyphLayout();
    private static final Matrix4 ORIGINAL_TRANSFORM = new Matrix4();
    private static final Matrix4 SCALED_TRANSFORM = new Matrix4();

    public DamageNumber(float x, float y, int amount, boolean isCritical) {
        this.x = x;
        this.y = y;
        this.text = isCritical ? amount + "!" : String.valueOf(amount);
        if (isCritical) {
            this.colorR = CRIT_R;
            this.colorG = CRIT_G;
            this.colorB = CRIT_B;
        } else {
            this.colorR = NORMAL_R;
            this.colorG = NORMAL_G;
            this.colorB = NORMAL_B;
        }
        this.colorA = 1f;
        this.timer = 0;
    }

    public DamageNumber(float x, float y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        Color safeColor = color == null ? Color.WHITE : color;
        this.colorR = safeColor.r;
        this.colorG = safeColor.g;
        this.colorB = safeColor.b;
        this.colorA = safeColor.a;
        this.timer = 0;
    }

    public void update(float delta) {
        timer += delta;
        y += RISE_SPEED * delta;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        float alpha = 1f - (timer / DURATION);
        if (alpha <= 0)
            return;

        float popScale = timer < 0.1f ? 1f + (0.1f - timer) * 5f : 1f;
        float drawScale = popScale * BASE_SCALE;

        SHARED_LAYOUT.setText(font, text);
        float drawX = x - SHARED_LAYOUT.width * 0.5f * drawScale;
        float drawY = y;

        ORIGINAL_TRANSFORM.set(batch.getTransformMatrix());
        SCALED_TRANSFORM.set(ORIGINAL_TRANSFORM);
        SCALED_TRANSFORM.translate(drawX, drawY, 0f);
        SCALED_TRANSFORM.scale(drawScale, drawScale, 1f);
        batch.setTransformMatrix(SCALED_TRANSFORM);

        font.setColor(colorR, colorG, colorB, colorA * alpha);
        font.draw(batch, text, 0f, 0f);

        batch.setTransformMatrix(ORIGINAL_TRANSFORM);
    }

    public boolean isExpired() {
        return timer >= DURATION;
    }
}
