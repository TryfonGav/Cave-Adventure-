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
            // Use system Consolas font (available on Windows)
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                    Gdx.files.absolute("C:/Windows/Fonts/consola.ttf"));

            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.color = Color.WHITE;
            param.borderWidth = 0.5f;
            param.borderColor = new Color(0, 0, 0, 0.6f);
            param.shadowOffsetX = 1;
            param.shadowOffsetY = 1;
            param.shadowColor = new Color(0, 0, 0, 0.35f);
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;

            // Normal font
            param.size = 16;
            font = generator.generateFont(param);

            // Small font
            param.size = 13;
            fontSmall = generator.generateFont(param);

            // Large font
            param.size = 26;
            param.borderWidth = 1f;
            fontLarge = generator.generateFont(param);

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
