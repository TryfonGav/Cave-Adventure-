package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.engine.InputHandler;
import com.caveadventure.item.CraftingManager;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;
import com.caveadventure.item.Recipe;

import java.util.List;
import java.util.Map;

public class CraftingUI {
    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final CraftingManager craftingManager;
    private boolean visible;
    private int selection;
    private String message;
    private float messageTimer;
    private final GlyphLayout ingredientLayout;

    public CraftingUI(CaveAdventure game, CraftingManager craftingManager) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.craftingManager = craftingManager;
        this.ingredientLayout = new GlyphLayout();
    }

    public void toggle() {
        visible = !visible;
        selection = 0;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean update(InputHandler input, Inventory inventory, float delta) {
        if (!visible)
            return false;
        if (messageTimer > 0)
            messageTimer -= delta;
        List<Recipe> recipes = craftingManager.getRecipes();
        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            selection = (selection - 1 + recipes.size()) % recipes.size();
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            selection = (selection + 1) % recipes.size();
        if (input.isKeyJustPressed(Input.Keys.ESCAPE) || input.isKeyJustPressed(Input.Keys.R)) {
            visible = false;
            return false;
        }
        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            Recipe recipe = recipes.get(selection);
            boolean crafted = recipe.craft(inventory);
            message = crafted ? "Crafted " + recipe.getResult().getType().displayName : "Missing ingredients";
            messageTimer = 2.2f;
            return crafted;
        }
        return false;
    }

    public void render(Inventory inventory) {
        if (!visible)
            return;
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float panelW = 560;
        float panelH = 410;
        float px = sw / 2f - panelW / 2f;
        float py = sh / 2f - panelH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        game.shapeRenderer.rect(0, 0, sw, sh);
        CaveUIStyle.drawStonePanel(game.shapeRenderer, px, py, panelW, panelH, 0.96f);
        CaveUIStyle.drawCarvedSeparator(game.shapeRenderer, px + 18, py + panelH - 44, panelW - 36, 1f);
        for (int i = 0; i < craftingManager.getRecipes().size(); i++) {
            if (i == selection)
                CaveUIStyle.drawSelection(game.shapeRenderer, px + 18, py + panelH - 92 - i * 52, panelW - 36, 42, 1f);
        }
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;
        nf.setColor(CaveUIStyle.GOLD);
        nf.draw(game.batch, "CRAFTING", px + 20, py + panelH - 14);
        sf.setColor(CaveUIStyle.MUTED_TEXT);
        sf.draw(game.batch, "Enter crafts. R / Esc closes.", px + 20, py + panelH - 38);
        for (int i = 0; i < craftingManager.getRecipes().size(); i++) {
            Recipe recipe = craftingManager.getRecipes().get(i);
            float y = py + panelH - 70 - i * 52;
            boolean canCraft = recipe.canCraft(inventory);
            nf.setColor(i == selection ? CaveUIStyle.GOLD : canCraft ? CaveUIStyle.TEXT : CaveUIStyle.DISABLED_TEXT);
            nf.draw(game.batch, recipe.getName(), px + 28, y);
            if (i == selection) {
                float textX = px + 28;
                for (Map.Entry<Item.ItemType, Integer> ingredient : recipe.getIngredients().entrySet()) {
                    if (textX > px + panelW - 36) {
                        break;
                    }
                    if (textX > px + 28) {
                        String separator = ", ";
                        sf.setColor(CaveUIStyle.MUTED_TEXT);
                        sf.draw(game.batch, separator, textX, y - 20);
                        ingredientLayout.setText(sf, separator);
                        textX += ingredientLayout.width;
                    }
                    int required = ingredient.getValue();
                    int owned = inventory != null ? inventory.count(ingredient.getKey()) : 0;
                    String ingredientText = required + "x " + ingredient.getKey().displayName;
                    sf.setColor(owned >= required ? CaveUIStyle.GOOD : CaveUIStyle.DANGER);
                    sf.draw(game.batch, ingredientText, textX, y - 20);
                    ingredientLayout.setText(sf, ingredientText);
                    textX += ingredientLayout.width;
                }
            } else {
                sf.setColor(canCraft ? CaveUIStyle.MUTED_TEXT : CaveUIStyle.DISABLED_TEXT);
                sf.draw(game.batch, recipe.ingredientText(), px + 28, y - 20, panelW - 56, -1, true);
            }
        }
        if (message != null && messageTimer > 0) {
            nf.setColor(CaveUIStyle.GOLD);
            nf.draw(game.batch, message, px + 20, py + 24);
        }
        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
