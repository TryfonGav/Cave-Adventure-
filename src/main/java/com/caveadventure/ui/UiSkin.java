package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * The pixel UI skin (P5-T8): 9-patch panel, inset and selection frames plus the menu
 * cursor, drawn at a whole-number UI scale with Nearest filtering. The art is the Kenney
 * Pixel UI Pack "Ancient" frame and arrow recoloured to the palette by
 * {@code scripts/build_pixel_assets.py}; {@code ui/skin.json} holds the regions and splits.
 */
public class UiSkin implements Disposable {

    public static final String IMAGE = "ui/skin.png";
    public static final String LAYOUT = "ui/skin.json";

    private final Texture texture;
    private final NinePatch panelBase;
    private final NinePatch insetBase;
    private final NinePatch selectionBase;
    private final TextureRegion cursor;
    private NinePatch panel;
    private NinePatch inset;
    private NinePatch selection;
    private int scale;

    private UiSkin(Texture texture, JsonValue layout) {
        this.texture = texture;
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.panelBase = ninePatch(layout.get("panel"));
        this.insetBase = ninePatch(layout.get("inset"));
        this.selectionBase = ninePatch(layout.get("selection"));
        this.cursor = region(layout.get("cursor"));
        setScale(1);
    }

    /**
     * Loads the skin from the classpath, or returns {@code null} when its files are missing
     * (callers then fall back to shape drawing).
     */
    public static UiSkin load() {
        FileHandle image = Gdx.files.internal(IMAGE);
        FileHandle layout = Gdx.files.internal(LAYOUT);
        if (!image.exists() || !layout.exists())
            return null;
        try {
            return new UiSkin(new Texture(image), new JsonReader().parse(layout));
        } catch (RuntimeException e) {
            Gdx.app.error("UiSkin", "could not load the UI skin, using shape drawing", e);
            return null;
        }
    }

    /** Sets the UI scale: every skin pixel becomes {@code scale} x {@code scale} screen pixels. */
    public void setScale(int scale) {
        int s = Math.max(1, scale);
        if (s == this.scale)
            return;
        this.scale = s;
        panel = scaled(panelBase, s);
        inset = scaled(insetBase, s);
        selection = scaled(selectionBase, s);
    }

    public int getScale() {
        return scale;
    }

    /** Raised stone panel frame. */
    public void drawPanel(SpriteBatch batch, float x, float y, float w, float h) {
        draw(batch, panel, x, y, w, h);
    }

    /** Sunken frame for wells, bars and hint strips. */
    public void drawInset(SpriteBatch batch, float x, float y, float w, float h) {
        draw(batch, inset, x, y, w, h);
    }

    /** Gold-rimmed frame behind a selected row or card. */
    public void drawSelection(SpriteBatch batch, float x, float y, float w, float h) {
        draw(batch, selection, x, y, w, h);
    }

    /** The ▶ cursor with its lower-left corner at (x, y), snapped to whole pixels. */
    public void drawCursor(SpriteBatch batch, float x, float y) {
        batch.draw(cursor, Math.round(x), Math.round(y), getCursorWidth(), getCursorHeight());
    }

    public int getCursorWidth() {
        return cursor.getRegionWidth() * scale;
    }

    public int getCursorHeight() {
        return cursor.getRegionHeight() * scale;
    }

    /** Width of the panel rim in screen pixels at the current scale. */
    public int getRimWidth() {
        return 2 * scale;
    }

    private static void draw(SpriteBatch batch, NinePatch patch, float x, float y, float w, float h) {
        int minW = Math.round(patch.getLeftWidth() + patch.getRightWidth());
        int minH = Math.round(patch.getTopHeight() + patch.getBottomHeight());
        patch.draw(batch, Math.round(x), Math.round(y), Math.max(minW, Math.round(w)), Math.max(minH, Math.round(h)));
    }

    private NinePatch ninePatch(JsonValue r) {
        return new NinePatch(region(r), r.getInt("left"), r.getInt("right"), r.getInt("top"), r.getInt("bottom"));
    }

    private TextureRegion region(JsonValue r) {
        return new TextureRegion(texture, r.getInt("x"), r.getInt("y"), r.getInt("w"), r.getInt("h"));
    }

    private static NinePatch scaled(NinePatch base, int scale) {
        NinePatch copy = new NinePatch(base);
        copy.scale(scale, scale);
        return copy;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
