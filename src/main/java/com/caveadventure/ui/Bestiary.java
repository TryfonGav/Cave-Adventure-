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
import com.caveadventure.entity.Enemy;

import java.util.*;

/**
 * Bestiary — tracks discovered enemies and displays their info.
 */
public class Bestiary {

    public static class EnemyEntry {
        public final Enemy.EnemyType type;
        public int killCount;
        public boolean discovered;

        public EnemyEntry(Enemy.EnemyType type) {
            this.type = type;
            this.killCount = 0;
            this.discovered = false;
        }
    }

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;
    private final Map<Enemy.EnemyType, EnemyEntry> entries;

    private boolean visible;
    private int selectedIndex;
    private List<Enemy.EnemyType> orderedTypes;

    public Bestiary(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.entries = new LinkedHashMap<>();
        this.orderedTypes = new ArrayList<>();

        for (Enemy.EnemyType type : Enemy.EnemyType.values()) {
            entries.put(type, new EnemyEntry(type));
            orderedTypes.add(type);
        }
    }

    public void discover(Enemy.EnemyType type) {
        entries.get(type).discovered = true;
    }

    public void recordKill(Enemy.EnemyType type) {
        EnemyEntry e = entries.get(type);
        e.discovered = true;
        e.killCount++;
    }

    public void toggle() {
        visible = !visible;
        selectedIndex = 0;
    }

    public boolean isVisible() {
        return visible;
    }

    public void update(InputHandler input) {
        if (!visible)
            return;
        int count = orderedTypes.size();
        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            selectedIndex = (selectedIndex - 1 + count) % count;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            selectedIndex = (selectedIndex + 1) % count;
    }

    public void render() {
        if (!visible)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        float panelW = 500, panelH = 400;
        float px = sw / 2 - panelW / 2, py = sh / 2 - panelH / 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(0, 0, 0, 0.6f);
        game.shapeRenderer.rect(0, 0, sw, sh);

        game.shapeRenderer.setColor(0.07f, 0.07f, 0.1f, 0.95f);
        game.shapeRenderer.rect(px, py, panelW, panelH);

        game.shapeRenderer.setColor(0.3f, 0.3f, 0.35f, 0.6f);
        game.shapeRenderer.rect(px, py, panelW, 2);
        game.shapeRenderer.rect(px, py + panelH - 2, panelW, 2);
        game.shapeRenderer.rect(px, py, 2, panelH);
        game.shapeRenderer.rect(px + panelW - 2, py, 2, panelH);

        // List panel (left)
        float listW = 180;
        game.shapeRenderer.setColor(0.06f, 0.06f, 0.09f, 0.9f);
        game.shapeRenderer.rect(px + 5, py + 5, listW, panelH - 40);

        for (int i = 0; i < orderedTypes.size(); i++) {
            EnemyEntry entry = entries.get(orderedTypes.get(i));
            float ey = py + panelH - 50 - i * 28;
            if (ey < py + 10)
                break;

            if (i == selectedIndex) {
                game.shapeRenderer.setColor(0.2f, 0.25f, 0.35f, 0.7f);
                game.shapeRenderer.rect(px + 7, ey - 2, listW - 4, 26);
            }

            if (entry.discovered) {
                game.shapeRenderer.setColor(entry.type.color.r, entry.type.color.g, entry.type.color.b, 0.8f);
                game.shapeRenderer.rect(px + 10, ey + 6, 8, 12);
            }
        }

        // Details panel (right)
        if (selectedIndex < orderedTypes.size()) {
            EnemyEntry sel = entries.get(orderedTypes.get(selectedIndex));
            if (sel.discovered) {
                // Enemy color preview
                game.shapeRenderer.setColor(sel.type.color);
                game.shapeRenderer.rect(px + listW + 30, py + panelH - 130, 50, 50);
                game.shapeRenderer.setColor(sel.type.color.r * 0.7f, sel.type.color.g * 0.7f, sel.type.color.b * 0.7f,
                        1f);
                game.shapeRenderer.rect(px + listW + 35, py + panelH - 140, 40, 15);
            }
        }

        game.shapeRenderer.end();

        // Text
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;

        nf.setColor(0.8f, 0.75f, 0.6f, 1f);
        nf.draw(game.batch, "BESTIARY", px + 15, py + panelH - 10);

        // Enemy list
        for (int i = 0; i < orderedTypes.size(); i++) {
            EnemyEntry entry = entries.get(orderedTypes.get(i));
            float ey = py + panelH - 45 - i * 28;
            if (ey < py + 10)
                break;

            if (entry.discovered) {
                sf.setColor(i == selectedIndex ? new Color(1f, 0.9f, 0.3f, 1f) : new Color(0.7f, 0.65f, 0.55f, 1f));
                sf.draw(game.batch, entry.type.name, px + 22, ey + 16);
            } else {
                sf.setColor(0.3f, 0.3f, 0.3f, 0.6f);
                sf.draw(game.batch, "???", px + 22, ey + 16);
            }
        }

        // Details
        if (selectedIndex < orderedTypes.size()) {
            EnemyEntry sel = entries.get(orderedTypes.get(selectedIndex));
            float dx = px + listW + 20;

            if (sel.discovered) {
                nf.setColor(sel.type.color);
                nf.draw(game.batch, sel.type.name, dx + 10, py + panelH - 50);

                sf.setColor(0.7f, 0.65f, 0.55f, 1f);
                sf.draw(game.batch, "HP: " + sel.type.maxHealth, dx + 10, py + panelH - 155);
                sf.draw(game.batch, "DMG: " + sel.type.minDamage + "-" + sel.type.maxDamage, dx + 10,
                        py + panelH - 175);
                sf.draw(game.batch, "Speed: " + sel.type.speed, dx + 10, py + panelH - 195);
                sf.draw(game.batch, "XP Reward: " + sel.type.xpReward, dx + 10, py + panelH - 215);

                nf.setColor(0.8f, 0.75f, 0.6f, 1f);
                nf.draw(game.batch, "Kills: " + sel.killCount, dx + 10, py + panelH - 245);
            } else {
                nf.setColor(0.4f, 0.4f, 0.4f, 1f);
                nf.draw(game.batch, "Unknown Creature", dx + 10, py + panelH - 50);
                sf.setColor(0.35f, 0.35f, 0.3f, 0.7f);
                sf.draw(game.batch, "Defeat this enemy to", dx + 10, py + panelH - 80);
                sf.draw(game.batch, "reveal its stats.", dx + 10, py + panelH - 98);
            }
        }

        sf.setColor(0.4f, 0.4f, 0.35f, 0.6f);
        sf.draw(game.batch, "W/S:Navigate  B/ESC:Close", px + 15, py + 10);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
