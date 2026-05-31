package com.caveadventure;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.engine.GameScreen;

/**
 * Main game class — initializes shared rendering resources and starts the game.
 */
public class CaveAdventure extends Game {

    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public BitmapFont font;
    public BitmapFont fontSmall;
    public BitmapFont fontLarge;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Generate crisp fonts using FreeType
        try {
            // Use Arial font (available on Windows) for a cleaner UI look
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                    Gdx.files.absolute("C:/Windows/Fonts/arial.ttf"));

            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.color = Color.WHITE;
            param.borderWidth = 1f;
            param.borderColor = new Color(0, 0, 0, 0.6f);
            param.shadowOffsetX = 2;
            param.shadowOffsetY = 2;
            param.shadowColor = new Color(0, 0, 0, 0.35f);
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;

            // Normal font (generated at 2x size for high-res)
            param.size = 32;
            font = generator.generateFont(param);
            font.getData().setScale(0.5f);

            // Small font
            param.size = 26;
            fontSmall = generator.generateFont(param);
            fontSmall.getData().setScale(0.5f);

            // Large font
            param.size = 52;
            param.borderWidth = 2f;
            fontLarge = generator.generateFont(param);
            fontLarge.getData().setScale(0.5f);

            generator.dispose();
        } catch (Exception e) {
            // Fallback to default BitmapFont
            font = new BitmapFont();
            fontSmall = new BitmapFont();
            fontLarge = new BitmapFont();
            fontLarge.getData().setScale(2f);
        }

        setScreen(new GameScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (font != null)
            font.dispose();
        if (fontSmall != null)
            fontSmall.dispose();
        if (fontLarge != null)
            fontLarge.dispose();
    }
}
