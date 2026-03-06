package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;
import com.caveadventure.entity.Player;

/**
 * Inventory panel UI — opens with TAB/I key, shows items with selection and
 * actions.
 */
public class InventoryUI {

    private final CaveAdventure game;
    private final OrthographicCamera uiCamera;
    private final GlyphLayout layout;

    private boolean visible;
    private int selectedIndex;
    private float animProgress; // 0 = closed, 1 = open

    // Layout
    private static final float PANEL_WIDTH = 350;
    private static final float PANEL_HEIGHT = 450;
    private static final float SLOT_HEIGHT = 28;
    private static final float SLOT_PADDING = 2;
    private static final float PADDING = 12;

    // Colors
    private static final Color BG_COLOR = new Color(0.06f, 0.06f, 0.1f, 0.92f);
    private static final Color BORDER_COLOR = new Color(0.35f, 0.35f, 0.4f, 0.7f);
    private static final Color SELECTED_COLOR = new Color(0.2f, 0.35f, 0.5f, 0.7f);
    private static final Color EQUIPPED_COLOR = new Color(0.4f, 0.3f, 0.1f, 0.4f);
    private static final Color HEADER_COLOR = new Color(0.8f, 0.75f, 0.6f, 1f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.88f, 0.82f, 1f);
    private static final Color DIM_COLOR = new Color(0.5f, 0.5f, 0.45f, 1f);
    private static final Color ACTION_COLOR = new Color(0.6f, 0.8f, 0.6f, 1f);

    public InventoryUI(CaveAdventure game) {
        this.game = game;
        this.uiCamera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.visible = false;
        this.selectedIndex = 0;
        this.animProgress = 0;
    }

    public void toggle() {
        visible = !visible;
        if (visible)
            selectedIndex = 0;
    }

    public boolean isVisible() {
        return visible;
    }

    public void update(float delta) {
        // Smooth open/close animation
        float target = visible ? 1f : 0f;
        animProgress += (target - animProgress) * delta * 10f;
        if (Math.abs(animProgress - target) < 0.01f)
            animProgress = target;
    }

    /**
     * Handle input while inventory is open.
     * Returns action performed: "none", "use", "equip", "drop"
     */
    public String handleInput(com.caveadventure.engine.InputHandler input, Inventory inventory) {
        if (!visible)
            return "none";

        int itemCount = inventory.getSize();
        if (itemCount == 0)
            return "none";

        // Navigate
        if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.W)) {
            selectedIndex = (selectedIndex - 1 + itemCount) % itemCount;
        }
        if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.S)) {
            selectedIndex = (selectedIndex + 1) % itemCount;
        }

        // Use / Equip (Enter or E)
        if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {
            Item item = inventory.getItem(selectedIndex);
            if (item != null) {
                if (item.isUsable())
                    return "use";
                if (item.isEquippable())
                    return "equip";
            }
        }

        // Drop (Q)
        if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.Q)) {
            return "drop";
        }

        return "none";
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void render(Inventory inventory, Player player) {
        if (animProgress < 0.01f)
            return;

        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Panel position (centered, slides from right)
        float panelX = screenW / 2 - PANEL_WIDTH / 2 + (1f - animProgress) * 200;
        float panelY = screenH / 2 - PANEL_HEIGHT / 2;
        float alpha = animProgress;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        game.shapeRenderer.setColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, BG_COLOR.a * alpha);
        game.shapeRenderer.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Border
        game.shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b, BORDER_COLOR.a * alpha);
        game.shapeRenderer.rect(panelX, panelY, PANEL_WIDTH, 2);
        game.shapeRenderer.rect(panelX, panelY + PANEL_HEIGHT - 2, PANEL_WIDTH, 2);
        game.shapeRenderer.rect(panelX, panelY, 2, PANEL_HEIGHT);
        game.shapeRenderer.rect(panelX + PANEL_WIDTH - 2, panelY, 2, PANEL_HEIGHT);

        // Header separator
        float headerY = panelY + PANEL_HEIGHT - 35;
        game.shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b, 0.5f * alpha);
        game.shapeRenderer.rect(panelX + PADDING, headerY, PANEL_WIDTH - PADDING * 2, 1);

        // Equipment section separator
        float equipY = headerY - 55;
        game.shapeRenderer.rect(panelX + PADDING, equipY, PANEL_WIDTH - PADDING * 2, 1);

        // Item slots
        float slotStartY = equipY - SLOT_PADDING - 5;
        java.util.List<Item> items = inventory.getItems();

        for (int i = 0; i < items.size(); i++) {
            float slotY = slotStartY - (i * (SLOT_HEIGHT + SLOT_PADDING));
            if (slotY < panelY + 35)
                break; // Don't draw below panel

            Item item = items.get(i);
            boolean isSelected = (i == selectedIndex) && visible;
            boolean isEquipped = (item == inventory.getEquippedWeapon() || item == inventory.getEquippedArmor());

            // Slot background
            if (isSelected) {
                game.shapeRenderer.setColor(SELECTED_COLOR.r, SELECTED_COLOR.g, SELECTED_COLOR.b,
                        SELECTED_COLOR.a * alpha);
            } else if (isEquipped) {
                game.shapeRenderer.setColor(EQUIPPED_COLOR.r, EQUIPPED_COLOR.g, EQUIPPED_COLOR.b,
                        EQUIPPED_COLOR.a * alpha);
            } else {
                game.shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.4f * alpha);
            }
            game.shapeRenderer.rect(panelX + PADDING, slotY, PANEL_WIDTH - PADDING * 2, SLOT_HEIGHT);

            // Item color indicator
            game.shapeRenderer.setColor(item.getType().color.r, item.getType().color.g,
                    item.getType().color.b, alpha);
            game.shapeRenderer.rect(panelX + PADDING + 2, slotY + 4, 6, SLOT_HEIGHT - 8);
        }

        game.shapeRenderer.end();

        // --- Text ---
        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        // Title
        game.font.setColor(HEADER_COLOR.r, HEADER_COLOR.g, HEADER_COLOR.b, alpha);
        game.font.draw(game.batch, "INVENTORY", panelX + PADDING, panelY + PANEL_HEIGHT - PADDING);

        // Slot count
        game.font.setColor(DIM_COLOR.r, DIM_COLOR.g, DIM_COLOR.b, alpha);
        String slotText = items.size() + "/" + Inventory.MAX_SLOTS;
        layout.setText(game.font, slotText);
        game.font.draw(game.batch, slotText, panelX + PANEL_WIDTH - PADDING - layout.width,
                panelY + PANEL_HEIGHT - PADDING);

        // Equipment display
        game.font.setColor(DIM_COLOR.r, DIM_COLOR.g, DIM_COLOR.b, alpha);
        game.font.draw(game.batch, "Weapon: ", panelX + PADDING, headerY - 8);
        game.font.draw(game.batch, "Armor:  ", panelX + PADDING, headerY - 28);

        Item weapon = inventory.getEquippedWeapon();
        Item armor = inventory.getEquippedArmor();

        if (weapon != null) {
            game.font.setColor(weapon.getType().color.r, weapon.getType().color.g,
                    weapon.getType().color.b, alpha);
            game.font.draw(game.batch, weapon.getType().displayName + " (+" + weapon.getType().attackBonus + " ATK)",
                    panelX + PADDING + 75, headerY - 8);
        } else {
            game.font.setColor(0.4f, 0.4f, 0.35f, alpha);
            game.font.draw(game.batch, "None", panelX + PADDING + 75, headerY - 8);
        }

        if (armor != null) {
            game.font.setColor(armor.getType().color.r, armor.getType().color.g,
                    armor.getType().color.b, alpha);
            game.font.draw(game.batch, armor.getType().displayName + " (+" + armor.getType().defenseBonus + " DEF)",
                    panelX + PADDING + 75, headerY - 28);
        } else {
            game.font.setColor(0.4f, 0.4f, 0.35f, alpha);
            game.font.draw(game.batch, "None", panelX + PADDING + 75, headerY - 28);
        }

        // Item list
        for (int i = 0; i < items.size(); i++) {
            float slotY = slotStartY - (i * (SLOT_HEIGHT + SLOT_PADDING));
            if (slotY < panelY + 35)
                break;

            Item item = items.get(i);
            boolean isEquipped = (item == inventory.getEquippedWeapon() || item == inventory.getEquippedArmor());

            // Item name
            game.font.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
            String name = item.getType().displayName;
            if (isEquipped)
                name = "[E] " + name;
            if (item.isStackable() && item.getQuantity() > 1)
                name += " x" + item.getQuantity();
            game.font.draw(game.batch, name, panelX + PADDING + 14, slotY + SLOT_HEIGHT - 6);

            // Category tag
            game.font.setColor(DIM_COLOR.r, DIM_COLOR.g, DIM_COLOR.b, alpha * 0.7f);
            String cat = item.getType().category.name();
            layout.setText(game.font, cat);
            game.font.draw(game.batch, cat, panelX + PANEL_WIDTH - PADDING - layout.width - 4,
                    slotY + SLOT_HEIGHT - 6);
        }

        // Action hints at bottom
        if (visible && !items.isEmpty()) {
            float hintY = panelY + 20;
            game.font.setColor(ACTION_COLOR.r, ACTION_COLOR.g, ACTION_COLOR.b, alpha * 0.8f);

            Item selected = inventory.getItem(selectedIndex);
            String hints = "W/S:Navigate";
            if (selected != null) {
                if (selected.isUsable())
                    hints += "  E:Use";
                if (selected.isEquippable())
                    hints += "  E:Equip";
                hints += "  Q:Drop";
            }
            hints += "  TAB:Close";
            game.font.draw(game.batch, hints, panelX + PADDING, hintY);
        }

        // Empty inventory message
        if (items.isEmpty()) {
            game.font.setColor(DIM_COLOR.r, DIM_COLOR.g, DIM_COLOR.b, alpha);
            game.font.draw(game.batch, "Inventory is empty",
                    panelX + PANEL_WIDTH / 2 - 60, panelY + PANEL_HEIGHT / 2);
        }

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
