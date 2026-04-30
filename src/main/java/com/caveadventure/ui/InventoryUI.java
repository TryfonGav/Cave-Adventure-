package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;

/**
 * Inventory panel UI. Opens with TAB/I, shows items with selection and actions.
 */
public class InventoryUI {

    private final CaveAdventure game;
    private final OrthographicCamera uiCamera;
    private final GlyphLayout layout;

    private boolean visible;
    private int selectedIndex;
    private int currentPage;
    private float animProgress; // 0 = closed, 1 = open

    // Minecraft-inspired layout
    private static final float PANEL_WIDTH = 620;
    private static final float PANEL_HEIGHT = 420;
    private static final float PADDING = 26;
    private static final int GRID_COLUMNS = 5;
    private static final int GRID_ROWS = 4;
    private static final int GRID_CAPACITY = GRID_COLUMNS * GRID_ROWS;
    private static final float SLOT_SIZE = 54;
    private static final float SLOT_GAP = 7;
    private static final float DETAIL_WIDTH = 238;
    private static final float SIDE_GAP = 22;

    // Colors
    private static final Color BACKDROP_COLOR = new Color(0.01f, 0.01f, 0.02f, 0.72f);
    private static final Color PANEL_FILL = new Color(0.13f, 0.15f, 0.20f, 0.98f);
    private static final Color PANEL_INNER = new Color(0.06f, 0.08f, 0.12f, 1f);
    private static final Color PANEL_DARK = new Color(0.02f, 0.03f, 0.05f, 1f);
    private static final Color PANEL_LIGHT = new Color(0.32f, 0.36f, 0.44f, 1f);
    private static final Color SLOT_INSET = new Color(0.06f, 0.07f, 0.10f, 1f);
    private static final Color SLOT_MID = new Color(0.16f, 0.18f, 0.23f, 1f);
    private static final Color SLOT_HIGHLIGHT = new Color(0.34f, 0.39f, 0.49f, 1f);
    private static final Color SELECTED_EDGE = new Color(1f, 0.98f, 0.58f, 1f);
    private static final Color EQUIPPED_EDGE = new Color(0.48f, 0.90f, 0.48f, 1f);
    private static final Color DETAIL_BG = new Color(0.12f, 0.12f, 0.16f, 0.94f);
    private static final Color TEXT_COLOR = new Color(0.90f, 0.92f, 0.96f, 1f);
    private static final Color DIM_TEXT = new Color(0.58f, 0.62f, 0.70f, 1f);
    private static final Color TOOLTIP_TEXT = new Color(0.94f, 0.92f, 0.86f, 1f);
    private static final Color ACTION_COLOR = new Color(0.70f, 0.86f, 0.68f, 1f);

    public InventoryUI(CaveAdventure game) {
        this.game = game;
        this.uiCamera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.visible = false;
        this.selectedIndex = 0;
        this.currentPage = 0;
        this.animProgress = 0;
    }

    public void toggle() {
        visible = !visible;
        if (visible) {
            selectedIndex = 0;
            currentPage = 0;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void update(float delta) {
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
        if (itemCount == 0) {
            selectedIndex = 0;
            currentPage = 0;
            return "none";
        }

        clampSelection(itemCount);

        if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.A)) {
            moveSelection(-1, itemCount);
        } else if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.D)) {
            moveSelection(1, itemCount);
        } else if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.W)) {
            moveSelection(-GRID_COLUMNS, itemCount);
        } else if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.S)) {
            moveSelection(GRID_COLUMNS, itemCount);
        } else if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.PAGE_UP)) {
            jumpPage(-1, itemCount);
        } else if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.PAGE_DOWN)) {
            jumpPage(1, itemCount);
        }

        if (input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
                input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {
            Item item = inventory.getItem(selectedIndex);
            return getActionForItem(item);
        }

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

        int itemCount = inventory.getSize();
        int pageCount = getPageCount(itemCount);
        clampSelection(itemCount);

        float panelX = screenW / 2 - PANEL_WIDTH / 2;
        float panelY = screenH / 2 - PANEL_HEIGHT / 2 + (1f - animProgress) * 26f;
        float alpha = animProgress;

        float gridWidth = GRID_COLUMNS * SLOT_SIZE + (GRID_COLUMNS - 1) * SLOT_GAP;
        float gridHeight = GRID_ROWS * SLOT_SIZE + (GRID_ROWS - 1) * SLOT_GAP;
        float gridX = panelX + PADDING;
        float gridY = panelY + 96;
        float detailX = gridX + gridWidth + SIDE_GAP;
        float detailY = gridY;
        float detailHeight = gridHeight;
        int pageStart = currentPage * GRID_CAPACITY;
        int visibleEnd = Math.min(pageStart + GRID_CAPACITY, itemCount);

        java.util.List<Item> items = inventory.getItems();
        Item selected = getSelectedItem(inventory);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(BACKDROP_COLOR.r, BACKDROP_COLOR.g, BACKDROP_COLOR.b, BACKDROP_COLOR.a * alpha);
        game.shapeRenderer.rect(0, 0, screenW, screenH);

        drawBeveledPanel(game.shapeRenderer, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, alpha);
        drawHeaderStrip(game.shapeRenderer, panelX, panelY + PANEL_HEIGHT - 68, PANEL_WIDTH, 44, alpha);
        drawSlotBay(game.shapeRenderer, gridX - 10, gridY - 10, gridWidth + 20, gridHeight + 20, alpha);

        drawInventorySlots(game.shapeRenderer, inventory, items, pageStart, visibleEnd, gridX, gridY, alpha);

        drawTooltipBox(game.shapeRenderer, detailX, detailY, DETAIL_WIDTH, detailHeight, alpha);
        drawFooterBar(game.shapeRenderer, panelX + PADDING, panelY + 20, PANEL_WIDTH - PADDING * 2, 46, alpha);

        if (pageCount > 1) {
            drawPagePips(game.shapeRenderer, panelX + PANEL_WIDTH - PADDING - 48, panelY + 58, pageCount, alpha);
        }

        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        drawTitleText(panelX, panelY, itemCount, pageCount, alpha);
        drawSlotText(items, pageStart, visibleEnd, gridX, gridY, alpha);
        drawDetailPanel(game.batch, inventory, selected, detailX + 14, detailY + detailHeight - 18, alpha);

        if (items.isEmpty()) {
            game.font.setColor(DIM_TEXT.r, DIM_TEXT.g, DIM_TEXT.b, alpha);
            drawCentered("Empty", gridX, gridY + gridHeight / 2f + 8, gridWidth);
        }

        drawActionHints(selected, panelX, panelY, alpha);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void clampSelection(int itemCount) {
        if (itemCount <= 0) {
            selectedIndex = 0;
            currentPage = 0;
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, itemCount - 1));
        currentPage = selectedIndex / GRID_CAPACITY;
    }

    private Item getSelectedItem(Inventory inventory) {
        clampSelection(inventory.getSize());
        return inventory.getItem(selectedIndex);
    }

    private void moveSelection(int delta, int itemCount) {
        selectedIndex = Math.max(0, Math.min(selectedIndex + delta, itemCount - 1));
        currentPage = selectedIndex / GRID_CAPACITY;
    }

    private void jumpPage(int deltaPages, int itemCount) {
        int pageCount = getPageCount(itemCount);
        if (pageCount <= 1)
            return;

        currentPage = Math.max(0, Math.min(currentPage + deltaPages, pageCount - 1));
        int targetIndex = currentPage * GRID_CAPACITY;
        selectedIndex = Math.max(0, Math.min(targetIndex, itemCount - 1));
    }

    private int getPageCount(int itemCount) {
        return Math.max(1, (itemCount + GRID_CAPACITY - 1) / GRID_CAPACITY);
    }

    private String getActionForItem(Item item) {
        if (item == null)
            return "none";
        if (item.isUsable())
            return "use";
        if (item.isEquippable())
            return "equip";
        return "none";
    }

    private String buildActionHints(Item selected) {
        String hints = "Move: WASD/Arrows   Page: PgUp/PgDn   Close: TAB/I";
        if (selected != null) {
            String action = "";
            if (selected.isUsable())
                action = "Use: E";
            if (selected.isEquippable())
                action = "Equip: E";
            hints += "\n" + (action.isEmpty() ? "" : action + "   ") + "Drop: Q";
        }
        return hints;
    }

    private void drawBeveledPanel(ShapeRenderer renderer, float x, float y, float width, float height, float alpha) {
        renderer.setColor(PANEL_DARK.r, PANEL_DARK.g, PANEL_DARK.b, PANEL_DARK.a * alpha);
        renderer.rect(x, y, width, height);
        renderer.setColor(PANEL_LIGHT.r, PANEL_LIGHT.g, PANEL_LIGHT.b, PANEL_LIGHT.a * alpha);
        renderer.rect(x + 3, y + height - 7, width - 6, 4);
        renderer.rect(x + 3, y + 3, 4, height - 6);
        renderer.setColor(0.14f, 0.15f, 0.17f, alpha);
        renderer.rect(x + 3, y + 3, width - 6, 4);
        renderer.rect(x + width - 7, y + 3, 4, height - 6);
        renderer.setColor(PANEL_FILL.r, PANEL_FILL.g, PANEL_FILL.b, PANEL_FILL.a * alpha);
        renderer.rect(x + 8, y + 8, width - 16, height - 16);
        renderer.setColor(PANEL_INNER.r, PANEL_INNER.g, PANEL_INNER.b, PANEL_INNER.a * alpha);
        drawFrame(renderer, x + 14, y + 14, width - 28, height - 28, 2);
    }

    private void drawHeaderStrip(ShapeRenderer renderer, float x, float y, float width, float height, float alpha) {
        renderer.setColor(0.17f, 0.19f, 0.25f, 0.94f * alpha);
        renderer.rect(x + 18, y, width - 36, height);
        renderer.setColor(0.35f, 0.39f, 0.48f, 0.95f * alpha);
        drawFrame(renderer, x + 18, y, width - 36, height, 2);
    }

    private void drawSlotBay(ShapeRenderer renderer, float x, float y, float width, float height, float alpha) {
        renderer.setColor(0.09f, 0.11f, 0.16f, 0.94f * alpha);
        renderer.rect(x, y, width, height);
        renderer.setColor(0.25f, 0.29f, 0.38f, 0.9f * alpha);
        drawFrame(renderer, x, y, width, height, 2);
    }

    private void drawFooterBar(ShapeRenderer renderer, float x, float y, float width, float height, float alpha) {
        renderer.setColor(0.05f, 0.07f, 0.10f, 0.86f * alpha);
        renderer.rect(x, y, width, height);
        renderer.setColor(0.25f, 0.30f, 0.38f, 0.8f * alpha);
        drawFrame(renderer, x, y, width, height, 2);
    }

    private void drawTooltipBox(ShapeRenderer renderer, float x, float y, float width, float height, float alpha) {
        renderer.setColor(0.08f, 0.08f, 0.10f, 0.42f * alpha);
        renderer.rect(x + 5, y - 5, width, height);
        renderer.setColor(DETAIL_BG.r, DETAIL_BG.g, DETAIL_BG.b, DETAIL_BG.a * alpha);
        renderer.rect(x, y, width, height);
        renderer.setColor(0.36f, 0.36f, 0.44f, 0.95f * alpha);
        drawFrame(renderer, x, y, width, height, 2);
        renderer.setColor(0.21f, 0.22f, 0.27f, 0.7f * alpha);
        renderer.rect(x + 8, y + height - 38, width - 16, 1);
    }

    private void drawInventorySlots(ShapeRenderer renderer, Inventory inventory, java.util.List<Item> items,
                                    int pageStart, int visibleEnd, float gridX, float gridY, float alpha) {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int slotIndex = pageStart + row * GRID_COLUMNS + col;
                float slotX = gridX + col * (SLOT_SIZE + SLOT_GAP);
                float slotY = gridY + (GRID_ROWS - 1 - row) * (SLOT_SIZE + SLOT_GAP);
                boolean hasItem = slotIndex < visibleEnd;
                boolean isSelected = slotIndex == selectedIndex && visible;
                boolean isEquipped = hasItem && (items.get(slotIndex) == inventory.getEquippedWeapon() ||
                        items.get(slotIndex) == inventory.getEquippedArmor());

                drawSlot(renderer, slotX, slotY, isSelected, isEquipped, alpha);

                if (hasItem) {
                    drawItemIcon(renderer, items.get(slotIndex), slotX, slotY, alpha);
                }
            }
        }
    }

    private void drawSlot(ShapeRenderer renderer, float x, float y, boolean selected, boolean equipped, float alpha) {
        renderer.setColor(SLOT_MID.r, SLOT_MID.g, SLOT_MID.b, SLOT_MID.a * alpha);
        renderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        renderer.setColor(0.02f, 0.03f, 0.05f, alpha);
        renderer.rect(x + 2, y + 2, SLOT_SIZE - 4, SLOT_SIZE - 4);
        renderer.setColor(SLOT_INSET.r, SLOT_INSET.g, SLOT_INSET.b, SLOT_INSET.a * alpha);
        renderer.rect(x + 5, y + 5, SLOT_SIZE - 10, SLOT_SIZE - 10);
        renderer.setColor(SLOT_HIGHLIGHT.r, SLOT_HIGHLIGHT.g, SLOT_HIGHLIGHT.b, SLOT_HIGHLIGHT.a * alpha);
        renderer.rect(x + 2, y + SLOT_SIZE - 4, SLOT_SIZE - 4, 2);
        renderer.rect(x + 2, y + 2, 2, SLOT_SIZE - 4);
        renderer.setColor(0.01f, 0.02f, 0.03f, alpha);
        renderer.rect(x + 2, y + 2, SLOT_SIZE - 4, 2);
        renderer.rect(x + SLOT_SIZE - 4, y + 2, 2, SLOT_SIZE - 4);

        if (equipped) {
            renderer.setColor(EQUIPPED_EDGE.r, EQUIPPED_EDGE.g, EQUIPPED_EDGE.b, EQUIPPED_EDGE.a * alpha);
            drawFrame(renderer, x + 4, y + 4, SLOT_SIZE - 8, SLOT_SIZE - 8, 2);
        }

        if (selected) {
            renderer.setColor(SELECTED_EDGE.r, SELECTED_EDGE.g, SELECTED_EDGE.b, SELECTED_EDGE.a * alpha);
            drawFrame(renderer, x - 2, y - 2, SLOT_SIZE + 4, SLOT_SIZE + 4, 3);
            renderer.setColor(1f, 1f, 1f, 0.12f * alpha);
            renderer.rect(x + 5, y + 5, SLOT_SIZE - 10, SLOT_SIZE - 10);
        }
    }

    private void drawItemIcon(ShapeRenderer renderer, Item item, float slotX, float slotY, float alpha) {
        Color color = item.getType().color;
        float cx = slotX + SLOT_SIZE / 2f;
        float cy = slotY + SLOT_SIZE / 2f;

        renderer.setColor(0f, 0f, 0f, 0.25f * alpha);
        renderer.rect(cx - 14, cy - 16, 28, 4);

        renderer.setColor(color.r * 0.72f, color.g * 0.72f, color.b * 0.72f, 0.95f * alpha);
        switch (item.getType().category) {
            case WEAPON:
                renderer.rect(cx - 3, cy - 15, 6, 31);
                renderer.rect(cx - 13, cy - 6, 26, 5);
                renderer.setColor(0.85f, 0.78f, 0.48f, 0.95f * alpha);
                renderer.rect(cx - 2, cy - 20, 4, 11);
                break;
            case ARMOR:
                renderer.rect(cx - 13, cy - 11, 26, 24);
                renderer.rect(cx - 19, cy - 5, 8, 16);
                renderer.rect(cx + 11, cy - 5, 8, 16);
                renderer.setColor(1f, 1f, 1f, 0.18f * alpha);
                renderer.rect(cx - 8, cy + 4, 16, 4);
                break;
            case CONSUMABLE:
                renderer.rect(cx - 9, cy - 13, 18, 25);
                renderer.rect(cx - 5, cy + 12, 10, 7);
                renderer.setColor(1f, 1f, 1f, 0.18f * alpha);
                renderer.rect(cx - 5, cy - 8, 4, 16);
                break;
            case KEY:
                renderer.ellipse(cx - 11, cy - 4, 18, 18);
                renderer.rect(cx + 2, cy + 2, 20, 5);
                renderer.rect(cx + 14, cy - 6, 5, 8);
                renderer.setColor(SLOT_INSET.r, SLOT_INSET.g, SLOT_INSET.b, 0.75f * alpha);
                renderer.ellipse(cx - 6, cy + 1, 8, 8);
                break;
            case TREASURE:
                renderer.triangle(cx, cy + 18, cx - 17, cy - 2, cx + 17, cy - 2);
                renderer.triangle(cx - 17, cy - 2, cx, cy - 18, cx + 17, cy - 2);
                renderer.setColor(1f, 1f, 1f, 0.22f * alpha);
                renderer.triangle(cx, cy + 12, cx - 7, cy - 1, cx + 5, cy - 1);
                break;
            case SCROLL:
                renderer.rect(cx - 15, cy - 14, 30, 28);
                renderer.setColor(0.86f, 0.77f, 0.54f, 0.95f * alpha);
                renderer.rect(cx - 11, cy - 8, 22, 3);
                renderer.rect(cx - 11, cy, 18, 3);
                renderer.rect(cx - 11, cy + 8, 22, 3);
                break;
            default:
                renderer.rect(cx - 13, cy - 13, 26, 26);
                break;
        }

        renderer.setColor(1f, 1f, 1f, 0.12f * alpha);
        renderer.rect(cx - 14, cy + 12, 28, 3);
    }

    private void drawPagePips(ShapeRenderer renderer, float x, float y, int pageCount, float alpha) {
        for (int i = 0; i < pageCount; i++) {
            boolean active = i == currentPage;
            renderer.setColor(active ? 0.15f : 0.42f, active ? 0.30f : 0.43f, active ? 0.18f : 0.45f,
                    (active ? 1f : 0.7f) * alpha);
            renderer.rect(x + i * 12f, y, 8, 8);
        }
    }

    private void drawTitleText(float panelX, float panelY, int itemCount, int pageCount, float alpha) {
        game.font.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
        game.font.draw(game.batch, "Inventory", panelX + PADDING, panelY + PANEL_HEIGHT - 39);

        String slotText = itemCount + "/" + Inventory.MAX_SLOTS;
        layout.setText(game.font, slotText);
        game.font.setColor(DIM_TEXT.r, DIM_TEXT.g, DIM_TEXT.b, alpha);
        game.font.draw(game.batch, slotText, panelX + PANEL_WIDTH - PADDING - layout.width,
                panelY + PANEL_HEIGHT - 39);

        if (pageCount > 1) {
            String pageText = "Page " + (currentPage + 1) + "/" + pageCount;
            layout.setText(game.font, pageText);
            game.font.draw(game.batch, pageText, panelX + PANEL_WIDTH - PADDING - layout.width,
                    panelY + PANEL_HEIGHT - 58);
        }
    }

    private void drawSlotText(java.util.List<Item> items, int pageStart, int visibleEnd, float gridX, float gridY,
                              float alpha) {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int slotIndex = pageStart + row * GRID_COLUMNS + col;
                if (slotIndex >= visibleEnd)
                    continue;

                Item item = items.get(slotIndex);
                float slotX = gridX + col * (SLOT_SIZE + SLOT_GAP);
                float slotY = gridY + (GRID_ROWS - 1 - row) * (SLOT_SIZE + SLOT_GAP);

                if (item.getQuantity() > 1) {
                    String quantity = String.valueOf(item.getQuantity());
                    layout.setText(game.font, quantity);
                    game.font.setColor(1f, 1f, 1f, alpha);
                    game.font.draw(game.batch, quantity, slotX + SLOT_SIZE - layout.width - 7, slotY + 15);
                }
            }
        }
    }

    private void drawActionHints(Item selected, float panelX, float panelY, float alpha) {
        game.font.setColor(ACTION_COLOR.r, ACTION_COLOR.g, ACTION_COLOR.b, alpha * 0.92f);
        game.font.draw(game.batch, buildActionHints(selected), panelX + PADDING + 8, panelY + 53,
                PANEL_WIDTH - PADDING * 2 - 16, Align.left, true);
    }

    private void drawCentered(String text, float x, float baselineY, float width) {
        layout.setText(game.font, text);
        game.font.draw(game.batch, text, x + width / 2f - layout.width / 2f, baselineY);
    }

    private void drawFrame(ShapeRenderer renderer, float x, float y, float width, float height, float thickness) {
        renderer.rect(x, y, width, thickness);
        renderer.rect(x, y + height - thickness, width, thickness);
        renderer.rect(x, y, thickness, height);
        renderer.rect(x + width - thickness, y, thickness, height);
    }

    private String fitText(String text, float maxWidth) {
        if (text == null)
            return "";
        String candidate = text;
        layout.setText(game.font, candidate);
        while (layout.width > maxWidth && candidate.length() > 3) {
            candidate = candidate.substring(0, candidate.length() - 1).trim();
            layout.setText(game.font, candidate + "...");
        }
        if (!candidate.equals(text))
            candidate = candidate + "...";
        return candidate;
    }

    private void drawDetailPanel(com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Inventory inventory, Item selected,
                                 float x, float topY, float alpha) {
        float lineY = topY;
        game.font.setColor(0.86f, 0.86f, 0.92f, alpha);
        game.font.draw(batch, "Selected", x, lineY);
        lineY -= 27;

        if (selected == null) {
            game.font.setColor(0.62f, 0.62f, 0.68f, alpha);
            game.font.draw(batch, "No item selected", x, lineY);
            return;
        }

        game.font.setColor(selected.getType().color.r, selected.getType().color.g,
                selected.getType().color.b, alpha);
        game.font.draw(batch, fitText(selected.getType().displayName, DETAIL_WIDTH - 28), x, lineY);
        lineY -= 23;

        game.font.setColor(0.63f, 0.64f, 0.70f, alpha);
        game.font.draw(batch, selected.getType().category.name(), x, lineY);
        lineY -= 25;

        game.font.setColor(TOOLTIP_TEXT.r, TOOLTIP_TEXT.g, TOOLTIP_TEXT.b, alpha);
        game.font.draw(batch, selected.getType().description, x, lineY, DETAIL_WIDTH - 28, Align.left, true);
        lineY -= 64;

        if (selected.isStackable() && selected.getQuantity() > 1) {
            game.font.setColor(0.92f, 0.90f, 0.70f, alpha);
            game.font.draw(batch, "Stack x" + selected.getQuantity(), x, lineY);
            lineY -= 22;
        }

        if (selected == inventory.getEquippedWeapon() || selected == inventory.getEquippedArmor()) {
            game.font.setColor(0.54f, 1f, 0.54f, alpha);
            game.font.draw(batch, "Equipped", x, lineY);
            lineY -= 22;
        }

        if (selected.isEquippable()) {
            game.font.setColor(0.74f, 0.95f, 0.72f, alpha);
            game.font.draw(batch, "ATK +" + selected.getType().attackBonus + "  DEF +" + selected.getType().defenseBonus,
                    x, lineY);
            lineY -= 22;
        } else if (selected.getType().restoreAmount > 0) {
            game.font.setColor(0.74f, 0.95f, 0.72f, alpha);
            game.font.draw(batch, "Restore: " + selected.getType().restoreAmount, x, lineY);
            lineY -= 22;
        }

        if (selected.isBattleOnly()) {
            game.font.setColor(0.95f, 0.74f, 0.44f, alpha);
            game.font.draw(batch, "Battle only", x, lineY);
            lineY -= 22;
        }

        String action = getActionForItem(selected);
        if (!"none".equals(action)) {
            game.font.setColor(0.86f, 0.86f, 0.92f, alpha);
            game.font.draw(batch, "Action: " + action.toUpperCase(), x, lineY);
        }
    }
}
