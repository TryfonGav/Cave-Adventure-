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
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;

/**
 * Shop UI for buying items from the shopkeeper NPC.
 */
public class ShopUI {

    public static class ShopItem {
        public final Item.ItemType type;
        public final int price; // in gold (treasure items as currency)
        public final String description;

        public ShopItem(Item.ItemType type, int price, String desc) {
            this.type = type;
            this.price = price;
            this.description = desc;
        }
    }

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;

    private boolean visible;
    private int selectedIndex;
    private float animProgress;
    private String statusMessage;
    private float statusTimer;

    // Shop inventory (regenerated each floor)
    private ShopItem[] shopItems;

    private static final ShopItem[] ALL_SHOP_ITEMS = {
            new ShopItem(Item.ItemType.HEALTH_POTION, 2, "Restores 30 HP"),
            new ShopItem(Item.ItemType.BREAD, 1, "Restores 25 hunger"),
            new ShopItem(Item.ItemType.ANTIDOTE, 2, "Cures poison"),
            new ShopItem(Item.ItemType.TORCH, 1, "Extends light radius"),
            new ShopItem(Item.ItemType.IRON_SWORD, 5, "+8 ATK weapon"),
            new ShopItem(Item.ItemType.STEEL_SWORD, 8, "+15 ATK weapon"),
            new ShopItem(Item.ItemType.CHAIN_MAIL, 6, "+5 DEF armor"),
            new ShopItem(Item.ItemType.PLATE_ARMOR, 10, "+10 DEF armor"),
            new ShopItem(Item.ItemType.ELIXIR, 6, "Full HP restore"),
            new ShopItem(Item.ItemType.MEAT, 3, "Restores 50 hunger"),
    };

    public ShopUI(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.visible = false;
        this.selectedIndex = 0;
        this.animProgress = 0;
        generateShopItems();
    }

    public void generateShopItems() {
        // Pick 5 random items for this shop
        java.util.List<ShopItem> available = new java.util.ArrayList<>(java.util.Arrays.asList(ALL_SHOP_ITEMS));
        java.util.Collections.shuffle(available);
        shopItems = new ShopItem[Math.min(5, available.size())];
        for (int i = 0; i < shopItems.length; i++) {
            shopItems[i] = available.get(i);
        }
    }

    public void open() {
        visible = true;
        selectedIndex = 0;
        statusMessage = null;
    }

    public void close() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns true if a purchase was made (for achievement tracking).
     */
    public boolean update(InputHandler input, Player player, float delta) {
        if (!visible)
            return false;

        boolean purchased = false;

        if (statusTimer > 0)
            statusTimer -= delta;
        if (statusTimer <= 0)
            statusMessage = null;

        animProgress += (1f - animProgress) * delta * 10f;

        // Navigate
        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            selectedIndex = (selectedIndex - 1 + shopItems.length) % shopItems.length;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            selectedIndex = (selectedIndex + 1) % shopItems.length;

        // Buy
        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.E)) {
            ShopItem item = shopItems[selectedIndex];
            int gold = countGold(player);
            if (gold >= item.price) {
                // Remove gold
                removeGold(player, item.price);
                // Add item
                player.getInventory().addItem(new Item(item.type));
                statusMessage = "Bought " + item.type.displayName + "!";
                statusTimer = 2f;
                purchased = true;
            } else {
                statusMessage = "Not enough gold! (" + gold + "/" + item.price + ")";
                statusTimer = 2f;
            }
        }

        // Close
        if (input.isKeyJustPressed(Input.Keys.ESCAPE) || input.isKeyJustPressed(Input.Keys.TAB)
                || input.isKeyJustPressed(Input.Keys.F)) {
            visible = false;
        }

        return purchased;
    }

    private int countGold(Player player) {
        int gold = 0;
        for (Item item : player.getInventory().getItems()) {
            if (item.getType() == Item.ItemType.GOLD_COINS)
                gold += item.getQuantity();
            if (item.getType() == Item.ItemType.GOLD_NUGGET)
                gold += item.getQuantity();
            if (item.getType() == Item.ItemType.RUBY)
                gold += item.getQuantity() * 3;
            if (item.getType() == Item.ItemType.DIAMOND)
                gold += item.getQuantity() * 5;
        }
        return gold;
    }

    private void removeGold(Player player, int cost) {
        int remaining = cost;
        // Remove cheapest currency first
        java.util.List<Item> items = player.getInventory().getItems();
        for (int i = items.size() - 1; i >= 0 && remaining > 0; i--) {
            Item item = items.get(i);
            if (item.getType() == Item.ItemType.GOLD_COINS || item.getType() == Item.ItemType.GOLD_NUGGET) {
                int take = Math.min(remaining, item.getQuantity());
                item.removeQuantity(take);
                remaining -= take;
                if (item.getQuantity() <= 0)
                    items.remove(i);
            }
        }
        // Then rubies (worth 3)
        for (int i = items.size() - 1; i >= 0 && remaining > 0; i--) {
            Item item = items.get(i);
            if (item.getType() == Item.ItemType.RUBY) {
                int take = Math.min((remaining + 2) / 3, item.getQuantity());
                item.removeQuantity(take);
                remaining -= take * 3;
                if (item.getQuantity() <= 0)
                    items.remove(i);
            }
        }
        // Then diamonds (worth 5)
        for (int i = items.size() - 1; i >= 0 && remaining > 0; i--) {
            Item item = items.get(i);
            if (item.getType() == Item.ItemType.DIAMOND) {
                int take = Math.min((remaining + 4) / 5, item.getQuantity());
                item.removeQuantity(take);
                remaining -= take * 5;
                if (item.getQuantity() <= 0)
                    items.remove(i);
            }
        }
    }

    public void render() {
        if (!visible)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float panelW = 380;
        float panelH = 300;
        float panelX = screenW / 2 - panelW / 2;
        float panelY = screenH / 2 - panelH / 2;
        float alpha = animProgress;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Dim background
        game.shapeRenderer.setColor(0, 0, 0, 0.5f * alpha);
        game.shapeRenderer.rect(0, 0, screenW, screenH);

        // Panel
        game.shapeRenderer.setColor(0.08f, 0.07f, 0.05f, 0.95f * alpha);
        game.shapeRenderer.rect(panelX, panelY, panelW, panelH);

        // Border
        game.shapeRenderer.setColor(0.5f, 0.4f, 0.2f, 0.7f * alpha);
        game.shapeRenderer.rect(panelX, panelY, panelW, 2);
        game.shapeRenderer.rect(panelX, panelY + panelH - 2, panelW, 2);
        game.shapeRenderer.rect(panelX, panelY, 2, panelH);
        game.shapeRenderer.rect(panelX + panelW - 2, panelY, 2, panelH);

        // Header separator
        game.shapeRenderer.setColor(0.4f, 0.3f, 0.15f, 0.5f * alpha);
        game.shapeRenderer.rect(panelX + 10, panelY + panelH - 35, panelW - 20, 1);

        // Item slots
        for (int i = 0; i < shopItems.length; i++) {
            float slotY = panelY + panelH - 55 - i * 40;
            if (i == selectedIndex) {
                game.shapeRenderer.setColor(0.3f, 0.25f, 0.1f, 0.6f * alpha);
                game.shapeRenderer.rect(panelX + 8, slotY, panelW - 16, 35);
            }

            // Item color dot
            game.shapeRenderer.setColor(shopItems[i].type.color.r, shopItems[i].type.color.g,
                    shopItems[i].type.color.b, alpha);
            game.shapeRenderer.rect(panelX + 14, slotY + 12, 8, 12);
        }

        game.shapeRenderer.end();

        // Text
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        // Title
        normalFont.setColor(0.9f, 0.75f, 0.3f, alpha);
        normalFont.draw(game.batch, "SHOP", panelX + 14, panelY + panelH - 10);

        // Gold display
        smallFont.setColor(0.7f, 0.6f, 0.3f, alpha);
        // We can't easily access player here, so show price info only

        // Items
        for (int i = 0; i < shopItems.length; i++) {
            ShopItem item = shopItems[i];
            float slotY = panelY + panelH - 55 - i * 40;

            if (i == selectedIndex)
                normalFont.setColor(1f, 0.9f, 0.4f, alpha);
            else
                normalFont.setColor(0.85f, 0.8f, 0.7f, alpha);

            normalFont.draw(game.batch, item.type.displayName, panelX + 30, slotY + 30);

            smallFont.setColor(0.6f, 0.55f, 0.4f, alpha);
            smallFont.draw(game.batch, item.description, panelX + 30, slotY + 12);

            // Price
            normalFont.setColor(0.9f, 0.8f, 0.2f, alpha);
            String price = item.price + "g";
            layout.setText(normalFont, price);
            normalFont.draw(game.batch, price, panelX + panelW - 25 - layout.width, slotY + 25);
        }

        // Status message
        if (statusMessage != null) {
            normalFont.setColor(0.9f, 0.85f, 0.3f, Math.min(1, statusTimer));
            layout.setText(normalFont, statusMessage);
            normalFont.draw(game.batch, statusMessage, panelX + panelW / 2 - layout.width / 2, panelY + 25);
        }

        // Controls hint
        smallFont.setColor(0.5f, 0.45f, 0.35f, alpha * 0.7f);
        smallFont.draw(game.batch, "W/S:Browse  E:Buy  ESC:Close", panelX + 10, panelY + 12);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
