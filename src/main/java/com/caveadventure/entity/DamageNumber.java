package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Floating damage number that rises and fades out.
 */
public class DamageNumber {

    private float x, y;
    private final String text;
    private final Color color;
    private float timer;
    private static final float DURATION = 1.0f;
    private static final float RISE_SPEED = 40f;

    public DamageNumber(float x, float y, int amount, boolean isCritical) {
        this.x = x;
        this.y = y;
        this.text = isCritical ? amount + "!" : String.valueOf(amount);
        this.color = isCritical
                ? new Color(1f, 0.9f, 0.1f, 1f) // Gold for crits
                : new Color(1f, 0.3f, 0.2f, 1f); // Red for normal
        this.timer = 0;
    }

    public DamageNumber(float x, float y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = new Color(color);
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

        float scale = timer < 0.1f ? 1f + (0.1f - timer) * 5f : 1f; // Pop effect
        font.getData().setScale(scale * 1.3f);
        font.setColor(color.r, color.g, color.b, alpha);

        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, text, x - layout.width / 2, y);

        font.getData().setScale(1.2f); // Reset scale
    }

    public boolean isExpired() {
        return timer >= DURATION;
    }
}
