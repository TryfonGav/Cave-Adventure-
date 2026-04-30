package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.ui.CaveUIStyle;

import java.util.*;

/**
 * Skill tree with 3 paths: Offensive, Defensive, Utility.
 * Player chooses one skill on each level-up.
 */
public class SkillTree {

    public enum SkillPath {
        OFFENSIVE, DEFENSIVE, UTILITY
    }

    public enum Skill {
        // Offensive
        POWER_SURGE("Power Surge", SkillPath.OFFENSIVE, "+20% damage", new Color(0.9f, 0.3f, 0.2f, 1f)),
        CRITICAL_MASTER("Critical Master", SkillPath.OFFENSIVE, "+15% crit chance", new Color(1f, 0.4f, 0.1f, 1f)),
        FURY("Fury", SkillPath.OFFENSIVE, "Below 30% HP: +50% damage", new Color(1f, 0.2f, 0.1f, 1f)),
        BERSERKER("Berserker", SkillPath.OFFENSIVE, "Each kill: +5% damage (resets)", new Color(0.9f, 0.1f, 0.1f, 1f)),

        // Defensive
        IRON_SKIN("Iron Skin", SkillPath.DEFENSIVE, "-15% damage taken", new Color(0.4f, 0.6f, 0.8f, 1f)),
        REGEN("Regeneration", SkillPath.DEFENSIVE, "Heal 2HP per turn in battle", new Color(0.3f, 0.8f, 0.4f, 1f)),
        STATUS_RESIST("Status Resist", SkillPath.DEFENSIVE, "50% chance to resist status",
                new Color(0.5f, 0.7f, 0.3f, 1f)),
        LAST_STAND("Last Stand", SkillPath.DEFENSIVE, "Survive lethal hit once", new Color(0.3f, 0.5f, 0.9f, 1f)),

        // Utility
        TREASURE_SENSE("Treasure Sense", SkillPath.UTILITY, "See chests on minimap", new Color(0.9f, 0.8f, 0.2f, 1f)),
        TRAP_DETECT("Trap Detect", SkillPath.UTILITY, "See hidden traps", new Color(0.7f, 0.6f, 0.2f, 1f)),
        LUCKY("Lucky", SkillPath.UTILITY, "+25% better loot", new Color(0.8f, 0.7f, 0.1f, 1f)),
        HAGGLER("Haggler", SkillPath.UTILITY, "Shop prices -30%", new Color(0.6f, 0.8f, 0.3f, 1f));

        public final String name;
        public final SkillPath path;
        public final String description;
        public final Color color;

        Skill(String name, SkillPath path, String desc, Color color) {
            this.name = name;
            this.path = path;
            this.description = desc;
            this.color = color;
        }
    }

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;
    private final Set<Skill> unlockedSkills;

    private boolean showingPicker;
    private boolean showingViewer;
    private Skill[] choices;
    private int selection;
    private int viewerSelection;

    public SkillTree(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.unlockedSkills = new HashSet<>();
        this.showingPicker = false;
        this.showingViewer = false;
    }

    public boolean hasSkill(Skill skill) {
        return unlockedSkills.contains(skill);
    }

    public boolean isShowingPicker() {
        return showingPicker;
    }

    public boolean isViewingTree() {
        return showingViewer;
    }

    public void toggleViewer() {
        if (showingPicker)
            return;
        showingViewer = !showingViewer;
        viewerSelection = Math.max(0, Math.min(viewerSelection, Skill.values().length - 1));
    }

    public void closeViewer() {
        showingViewer = false;
    }

    public void clearUnlockedSkills() {
        unlockedSkills.clear();
        showingPicker = false;
        showingViewer = false;
        choices = null;
    }

    public Set<Skill> getUnlockedSkills() {
        return new HashSet<>(unlockedSkills);
    }

    public void restoreUnlockedSkills(Collection<Skill> skills) {
        unlockedSkills.clear();
        if (skills != null)
            unlockedSkills.addAll(skills);
    }

    /**
     * Show skill picker with 3 random choices (one from each path).
     */
    public void showPicker() {
        List<Skill> available = new ArrayList<>();
        for (Skill s : Skill.values()) {
            if (!unlockedSkills.contains(s))
                available.add(s);
        }
        if (available.isEmpty())
            return;

        // Try to pick one from each path
        choices = new Skill[Math.min(3, available.size())];
        List<Skill> off = new ArrayList<>(), def = new ArrayList<>(), util = new ArrayList<>();
        for (Skill s : available) {
            switch (s.path) {
                case OFFENSIVE:
                    off.add(s);
                    break;
                case DEFENSIVE:
                    def.add(s);
                    break;
                case UTILITY:
                    util.add(s);
                    break;
            }
        }
        Random r = new Random();
        int idx = 0;
        if (!off.isEmpty() && idx < choices.length)
            choices[idx++] = off.get(r.nextInt(off.size()));
        if (!def.isEmpty() && idx < choices.length)
            choices[idx++] = def.get(r.nextInt(def.size()));
        if (!util.isEmpty() && idx < choices.length)
            choices[idx++] = util.get(r.nextInt(util.size()));

        // Fill remaining with any
        while (idx < choices.length) {
            Skill s = available.get(r.nextInt(available.size()));
            boolean dup = false;
            for (int i = 0; i < idx; i++)
                if (choices[i] == s)
                    dup = true;
            if (!dup)
                choices[idx++] = s;
        }

        showingPicker = true;
        showingViewer = false;
        selection = 0;
    }

    public void update(InputHandler input) {
        if (showingPicker && choices != null) {
            if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A))
                selection = (selection - 1 + choices.length) % choices.length;
            if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D))
                selection = (selection + 1) % choices.length;

            if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
                unlockedSkills.add(choices[selection]);
                showingPicker = false;
            }
            return;
        }

        if (!showingViewer)
            return;

        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            viewerSelection = (viewerSelection - 1 + Skill.values().length) % Skill.values().length;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            viewerSelection = (viewerSelection + 1) % Skill.values().length;
        if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A))
            viewerSelection = moveViewerHorizontal(-1);
        if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D))
            viewerSelection = moveViewerHorizontal(1);

        if (input.isKeyJustPressed(Input.Keys.K) || input.isKeyJustPressed(Input.Keys.ESCAPE)
                || input.isKeyJustPressed(Input.Keys.TAB)) {
            showingViewer = false;
        }
    }

    private int moveViewerHorizontal(int direction) {
        Skill[] skills = Skill.values();
        Skill current = skills[viewerSelection];
        int row = getRowInPath(current);
        SkillPath[] paths = SkillPath.values();
        int nextPathIndex = (current.path.ordinal() + direction + paths.length) % paths.length;
        SkillPath nextPath = paths[nextPathIndex];

        Skill fallback = null;
        for (Skill skill : skills) {
            if (skill.path != nextPath)
                continue;
            if (fallback == null)
                fallback = skill;
            if (getRowInPath(skill) == row)
                return skill.ordinal();
        }
        return fallback != null ? fallback.ordinal() : viewerSelection;
    }

    private int getRowInPath(Skill skill) {
        int row = 0;
        for (Skill candidate : Skill.values()) {
            if (candidate.path == skill.path) {
                if (candidate == skill)
                    return row;
                row++;
            }
        }
        return 0;
    }

    public void render() {
        if (showingPicker && choices != null) {
            renderPicker();
            return;
        }

        if (!showingViewer)
            return;

        renderViewer();
    }

    private void renderPicker() {

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Overlay
        game.shapeRenderer.setColor(0, 0, 0, 0.7f);
        game.shapeRenderer.rect(0, 0, sw, sh);

        // Cards
        float cardW = 180;
        float cardH = 200;
        float totalW = choices.length * cardW + (choices.length - 1) * 20;
        float startX = sw / 2 - totalW / 2;
        float cardY = sh / 2 - cardH / 2;

        for (int i = 0; i < choices.length; i++) {
            float cx = startX + i * (cardW + 20);
            boolean sel = (i == selection);

            CaveUIStyle.drawStonePanel(game.shapeRenderer, cx, cardY, cardW, cardH, sel ? 1f : 0.88f);
            if (sel)
                CaveUIStyle.drawSelection(game.shapeRenderer, cx + 6, cardY + 6, cardW - 12, cardH - 12, 1f);

            // Path color bar
            game.shapeRenderer.setColor(choices[i].color.r, choices[i].color.g, choices[i].color.b, 0.8f);
            game.shapeRenderer.rect(cx + 5, cardY + cardH - 30, cardW - 10, 25);

            // Icon placeholder
            game.shapeRenderer.setColor(choices[i].color.r, choices[i].color.g, choices[i].color.b, 0.6f);
            game.shapeRenderer.rect(cx + cardW / 2 - 20, cardY + 80, 40, 40);
        }

        game.shapeRenderer.end();

        // Text
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;
        BitmapFont lf = game.fontLarge != null ? game.fontLarge : game.font;

        // Title
        lf.setColor(CaveUIStyle.GOLD);
        layout.setText(lf, "LEVEL UP!");
        lf.draw(game.batch, "LEVEL UP!", sw / 2 - layout.width / 2, cardY + cardH + 50);

        nf.setColor(CaveUIStyle.MUTED_TEXT);
        layout.setText(nf, "Choose a skill:");
        nf.draw(game.batch, "Choose a skill:", sw / 2 - layout.width / 2, cardY + cardH + 22);

        for (int i = 0; i < choices.length; i++) {
            float cx = startX + i * (cardW + 20);
            boolean sel = (i == selection);

            // Path name on bar
            sf.setColor(0, 0, 0, 1f);
            sf.draw(game.batch, choices[i].path.name(), cx + 10, cardY + cardH - 10);

            // Skill name
            nf.setColor(sel ? choices[i].color : CaveUIStyle.TEXT);
            nf.draw(game.batch, choices[i].name, cx + 10, cardY + 70);

            // Description
            sf.setColor(CaveUIStyle.MUTED_TEXT);
            sf.draw(game.batch, choices[i].description, cx + 10, cardY + 45, cardW - 20, -1, true);
        }

        // Hint
        sf.setColor(CaveUIStyle.MUTED_TEXT);
        layout.setText(sf, "A/D: Select   Enter: Confirm");
        sf.draw(game.batch, "A/D: Select   Enter: Confirm", sw / 2 - layout.width / 2, cardY - 15);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderViewer() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(0f, 0f, 0f, 0.72f);
        game.shapeRenderer.rect(0, 0, sw, sh);

        float panelX = 40;
        float panelY = 40;
        float panelW = sw - 80;
        float panelH = sh - 80;
        CaveUIStyle.drawStonePanel(game.shapeRenderer, panelX, panelY, panelW, panelH, 0.96f);
        CaveUIStyle.drawCarvedSeparator(game.shapeRenderer, panelX + 24, panelY + panelH - 58, panelW - 48, 1f);

        float gridTop = panelY + panelH - 90;
        float cardW = 210;
        float cardH = 72;
        float colGap = 20;
        float rowGap = 14;
        float startX = panelX + 30;

        for (SkillPath path : SkillPath.values()) {
            for (Skill skill : Skill.values()) {
                if (skill.path != path)
                    continue;
                int col = path.ordinal();
                int row = getRowInPath(skill);
                float cx = startX + col * (cardW + colGap);
                float cy = gridTop - row * (cardH + rowGap) - cardH;
                boolean selected = viewerSelection == skill.ordinal();
                boolean unlocked = unlockedSkills.contains(skill);

                CaveUIStyle.drawInsetPanel(game.shapeRenderer, cx, cy, cardW, cardH, unlocked ? 0.92f : 0.74f);
                if (selected)
                    CaveUIStyle.drawSelection(game.shapeRenderer, cx, cy, cardW, cardH, 1f);

                game.shapeRenderer.setColor(skill.color.r, skill.color.g, skill.color.b, unlocked ? 0.9f : 0.35f);
                game.shapeRenderer.rect(cx + 8, cy + cardH - 12, cardW - 16, 4);
            }
        }

        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;
        BitmapFont lf = game.fontLarge != null ? game.fontLarge : game.font;

        lf.setColor(CaveUIStyle.GOLD);
        lf.draw(game.batch, "SKILL TREE", panelX + 24, panelY + panelH - 18);
        sf.setColor(CaveUIStyle.MUTED_TEXT);
        sf.draw(game.batch, "Unlocked skills stay with your character. Browse with WASD. K / ESC closes.",
                panelX + 26, panelY + panelH - 42);

        for (SkillPath path : SkillPath.values()) {
            float headerX = startX + path.ordinal() * (cardW + colGap);
            nf.setColor(path == SkillPath.OFFENSIVE ? new Color(0.95f, 0.4f, 0.25f, 1f)
                    : path == SkillPath.DEFENSIVE ? new Color(0.45f, 0.75f, 0.95f, 1f)
                            : new Color(0.85f, 0.85f, 0.35f, 1f));
            nf.draw(game.batch, path.name(), headerX, gridTop + 24);

            for (Skill skill : Skill.values()) {
                if (skill.path != path)
                    continue;
                int row = getRowInPath(skill);
                float cx = headerX;
                float cy = gridTop - row * (cardH + rowGap) - cardH;
                boolean selected = viewerSelection == skill.ordinal();
                boolean unlocked = unlockedSkills.contains(skill);

                nf.setColor(selected ? skill.color : unlocked ? CaveUIStyle.TEXT : CaveUIStyle.MUTED_TEXT);
                nf.draw(game.batch, skill.name, cx + 10, cy + cardH - 16);

                sf.setColor(unlocked ? new Color(0.36f, 0.95f, 0.45f, 1f) : new Color(0.85f, 0.4f, 0.4f, 1f));
                sf.draw(game.batch, unlocked ? "UNLOCKED" : "LOCKED", cx + 10, cy + 22);
            }
        }

        Skill selectedSkill = Skill.values()[viewerSelection];
        float detailX = panelX + panelW - 250;
        float detailY = panelY + 40;
        float detailW = 200;
        nf.setColor(selectedSkill.color);
        nf.draw(game.batch, selectedSkill.name, detailX, detailY + 180);
        sf.setColor(CaveUIStyle.MUTED_TEXT);
        sf.draw(game.batch, selectedSkill.path.name(), detailX, detailY + 156);
        sf.setColor(CaveUIStyle.TEXT);
        sf.draw(game.batch, selectedSkill.description, detailX, detailY + 126, detailW, -1, true);
        sf.setColor(unlockedSkills.contains(selectedSkill) ? new Color(0.36f, 0.95f, 0.45f, 1f)
                : new Color(0.85f, 0.4f, 0.4f, 1f));
        sf.draw(game.batch, unlockedSkills.contains(selectedSkill) ? "Owned" : "Not unlocked yet", detailX,
                detailY + 88);
        sf.setColor(CaveUIStyle.MUTED_TEXT);
        sf.draw(game.batch, "Unlocked: " + unlockedSkills.size() + " / " + Skill.values().length, detailX,
                detailY + 56);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // --- Stat Query Methods ---
    public float getDamageMultiplier(float hpPercent) {
        float mult = 1f;
        if (hasSkill(Skill.POWER_SURGE))
            mult += 0.20f;
        if (hasSkill(Skill.FURY) && hpPercent < 0.3f)
            mult += 0.50f;
        return mult * Difficulty.getCurrent().playerDamageMultiplier;
    }

    public float getCritBonus() {
        return hasSkill(Skill.CRITICAL_MASTER) ? 0.15f : 0f;
    }

    public float getDamageReduction() {
        return hasSkill(Skill.IRON_SKIN) ? 0.15f : 0f;
    }

    public int getRegenPerTurn() {
        return hasSkill(Skill.REGEN) ? 2 : 0;
    }

    public boolean canResistStatus() {
        return hasSkill(Skill.STATUS_RESIST) && new Random().nextFloat() < 0.5f;
    }

    public boolean hasLastStand() {
        return hasSkill(Skill.LAST_STAND);
    }

    public boolean hasTrapDetect() {
        return hasSkill(Skill.TRAP_DETECT);
    }

    public boolean hasTreasureSense() {
        return hasSkill(Skill.TREASURE_SENSE);
    }

    public float getShopDiscount() {
        return hasSkill(Skill.HAGGLER) ? 0.3f : 0f;
    }

    public float getLootBonus() {
        return hasSkill(Skill.LUCKY) ? 0.25f : 0f;
    }
}
