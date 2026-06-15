package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.engine.InputHandler;
import com.caveadventure.entity.Player;
import com.caveadventure.quest.Quest;
import com.caveadventure.quest.QuestManager;

public class QuestLogUI {
    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private boolean visible;

    public QuestLogUI(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
    }

    public void toggle() {
        visible = !visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void update(InputHandler input) {
        update(input, null, null);
    }

    public int update(InputHandler input, QuestManager questManager, Player player) {
        if (input.isKeyJustPressed(Input.Keys.J) || input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            visible = false;
            return 0;
        }
        if (questManager != null && player != null
                && (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE))) {
            return questManager.claimReady(player);
        }
        return 0;
    }

    public void render(QuestManager questManager) {
        if (!visible)
            return;
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float panelW = 500;
        float panelH = 360;
        float px = sw / 2f - panelW / 2f;
        float py = sh / 2f - panelH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.62f);
        game.shapeRenderer.rect(0, 0, sw, sh);
        CaveUIStyle.drawStonePanel(game.shapeRenderer, px, py, panelW, panelH, 0.96f);
        CaveUIStyle.drawCarvedSeparator(game.shapeRenderer, px + 18, py + panelH - 42, panelW - 36, 1f);
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;
        nf.setColor(CaveUIStyle.GOLD);
        nf.draw(game.batch, "QUEST LOG", px + 18, py + panelH - 12);
        sf.setColor(CaveUIStyle.MUTED_TEXT);
        sf.draw(game.batch, "Talk to NPCs for new quests. Enter claims ready rewards. J / Esc closes.",
                px + 18, py + panelH - 36);

        float y = py + panelH - 72;
        if (questManager.getVisibleQuests().isEmpty()) {
            sf.setColor(CaveUIStyle.MUTED_TEXT);
            sf.draw(game.batch, "No accepted quests yet.", px + 24, y);
        }
        for (Quest quest : questManager.getVisibleQuests()) {
            nf.setColor(quest.getState().name().contains("COMPLETED") ? CaveUIStyle.DISABLED_TEXT : CaveUIStyle.TEXT);
            nf.draw(game.batch, quest.getDefinition().title(), px + 24, y);
            sf.setColor(CaveUIStyle.MUTED_TEXT);
            sf.draw(game.batch, quest.trackerText() + "  [" + quest.getState().name() + "]",
                    px + 24, y - 20, panelW - 48, -1, true);
            y -= 58;
        }
        if (questManager.getLastMessage() != null) {
            sf.setColor(CaveUIStyle.GOLD);
            sf.draw(game.batch, questManager.getLastMessage(), px + 24, py + 20);
        }
        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
