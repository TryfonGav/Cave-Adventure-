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
import com.caveadventure.entity.Companion;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;

import java.util.*;

/**
 * Random story events triggered during exploration.
 */
public class RandomEventManager {

    public static class EventChoice {
        public final String text;
        public final String result;
        public final EventEffect effect;

        public EventChoice(String text, String result, EventEffect effect) {
            this.text = text;
            this.result = result;
            this.effect = effect;
        }
    }

    public enum EventEffect {
        HEAL_30, HEAL_FULL, DAMAGE_20, GIVE_POTION, GIVE_KEY, GIVE_GOLD,
        GIVE_WEAPON, BUFF_ATTACK, BUFF_DEFENSE, POISON, NOTHING, XP_BONUS,
        SPAWN_COMPANION
    }

    public static class GameEvent {
        public final String title;
        public final String description;
        public final EventChoice[] choices;
        public final Color color;

        public GameEvent(String title, String desc, Color color, EventChoice... choices) {
            this.title = title;
            this.description = desc;
            this.color = color;
            this.choices = choices;
        }
    }

    private static final GameEvent[] ALL_EVENTS = {
            new GameEvent("Wounded Traveler",
                    "A wounded traveler lies against the wall, gasping for breath. They reach out to you...",
                    new Color(0.6f, 0.4f, 0.3f, 1f),
                    new EventChoice("Help them", "The traveler thanks you and gives you a healing potion!",
                            EventEffect.GIVE_POTION),
                    new EventChoice("Ignore", "You walk past. The cave echoes with silence.", EventEffect.NOTHING)),

            new GameEvent("Mysterious Altar",
                    "A glowing altar hums with ancient energy. Strange symbols pulse on its surface...",
                    new Color(0.5f, 0.3f, 0.8f, 1f),
                    new EventChoice("Pray", "Divine energy surges through you! Full HP restored!",
                            EventEffect.HEAL_FULL),
                    new EventChoice("Touch symbols", "A surge of power! Your attack increases!",
                            EventEffect.BUFF_ATTACK),
                    new EventChoice("Leave it", "Wise caution. You move on.", EventEffect.NOTHING)),

            new GameEvent("Trapped Merchant",
                    "A merchant is trapped under fallen rocks. They promise a reward if freed...",
                    new Color(0.7f, 0.6f, 0.2f, 1f),
                    new EventChoice("Free them", "The merchant gives you gold nuggets!", EventEffect.GIVE_GOLD),
                    new EventChoice("Demand payment first", "They toss you a key before you help.",
                            EventEffect.GIVE_KEY)),

            new GameEvent("Strange Mushrooms",
                    "Bioluminescent mushrooms grow in a cluster. They look edible... maybe.",
                    new Color(0.3f, 0.8f, 0.4f, 1f),
                    new EventChoice("Eat them", "They're healing mushrooms! +30 HP!", EventEffect.HEAL_30),
                    new EventChoice("Avoid them", "Probably wise. Who eats cave mushrooms?", EventEffect.NOTHING)),

            new GameEvent("Hollow Echo",
                    "You hear a hollow echo behind a thin wall. Something is hidden there...",
                    new Color(0.4f, 0.5f, 0.6f, 1f),
                    new EventChoice("Break the wall", "You find a hidden weapon stash!", EventEffect.GIVE_WEAPON),
                    new EventChoice("Listen closer", "You learn from the echoes. +50 XP!", EventEffect.XP_BONUS)),

            new GameEvent("Poison Gas",
                    "Green gas seeps from cracks in the floor. It burns your lungs...",
                    new Color(0.4f, 0.7f, 0.2f, 1f),
                    new EventChoice("Push through", "You're poisoned but find treasure!", EventEffect.POISON),
                    new EventChoice("Find another way", "You avoid the gas safely.", EventEffect.NOTHING)),

            new GameEvent("Ancient Spirit",
                    "A translucent spirit appears before you, its eyes filled with wisdom...",
                    new Color(0.6f, 0.7f, 0.9f, 1f),
                    new EventChoice("Ask for blessing", "The spirit grants you defense!", EventEffect.BUFF_DEFENSE),
                    new EventChoice("Ask for knowledge", "Ancient knowledge flows in. +75 XP!", EventEffect.XP_BONUS)),

            new GameEvent("Whimpering Wolf",
                    "A wounded cave wolf pup lies curled near a rock, growling softly. It eyes you cautiously...",
                    new Color(0.55f, 0.45f, 0.35f, 1f),
                    new EventChoice("Tend its wounds", "The wolf pup nuzzles your hand. A companion joins you!", EventEffect.SPAWN_COMPANION),
                    new EventChoice("Leave it alone", "You walk on. The wolf watches you go.", EventEffect.NOTHING)),

            new GameEvent("Fire Sprite",
                    "A tiny fire sprite flickers in a crevice, trapped behind a stone. It sparks at you desperately...",
                    new Color(1f, 0.6f, 0.2f, 1f),
                    new EventChoice("Free the sprite", "The sprite dances around you joyfully. A companion joins you!", EventEffect.SPAWN_COMPANION),
                    new EventChoice("Ignore it", "The sprite dims sadly as you pass.", EventEffect.NOTHING)),

            new GameEvent("Shadow Cat",
                    "A shadow cat slinks from the darkness, its luminous eyes fixed on you. It seems lost...",
                    new Color(0.3f, 0.25f, 0.35f, 1f),
                    new EventChoice("Offer your hand", "The shadow cat accepts you. A companion joins you!", EventEffect.SPAWN_COMPANION),
                    new EventChoice("Back away slowly", "The cat vanishes into the shadows.", EventEffect.NOTHING)),
    };

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout glayout;
    private final Random random;

    private GameEvent currentEvent;
    private int choiceSelection;
    private boolean active;
    private boolean showingResult;
    private String resultText;
    private EventEffect pendingEffect;
    private float eventCooldown;
    private EventEffect lastEffect;
    private Companion.PetType lastCompanionType;

    public RandomEventManager(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.glayout = new GlyphLayout();
        this.random = new Random();
        this.active = false;
        this.eventCooldown = 0;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Random chance to trigger an event when player moves.
     */
    public boolean tryTrigger() {
        if (eventCooldown > 0 || active)
            return false;
        if (random.nextFloat() < 0.008f) { // ~0.8% per step
            currentEvent = ALL_EVENTS[random.nextInt(ALL_EVENTS.length)];
            choiceSelection = 0;
            active = true;
            showingResult = false;
            return true;
        }
        return false;
    }

    public void update(InputHandler input, Player player, float delta) {
        if (!active) {
            if (eventCooldown > 0)
                eventCooldown -= delta;
            return;
        }

        if (showingResult) {
            if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
                lastEffect = pendingEffect;
                lastCompanionType = resolveCompanionType(currentEvent);
                applyEffect(pendingEffect, player);
                active = false;
                eventCooldown = 30f; // 30s cooldown
            }
            return;
        }

        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            choiceSelection = (choiceSelection - 1 + currentEvent.choices.length) % currentEvent.choices.length;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            choiceSelection = (choiceSelection + 1) % currentEvent.choices.length;

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            EventChoice choice = currentEvent.choices[choiceSelection];
            resultText = choice.result;
            pendingEffect = choice.effect;
            showingResult = true;
        }
    }

    private void applyEffect(EventEffect effect, Player player) {
        switch (effect) {
            case HEAL_30:
                player.heal(30);
                break;
            case HEAL_FULL:
                player.heal(999);
                break;
            case DAMAGE_20:
                player.takeDamage(20);
                break;
            case GIVE_POTION:
                player.getInventory().addItem(new Item(Item.ItemType.HEALTH_POTION, 2));
                break;
            case GIVE_KEY:
                player.getInventory().addItem(new Item(Item.ItemType.BRONZE_KEY));
                break;
            case GIVE_GOLD:
                player.getInventory().addItem(new Item(Item.ItemType.GOLD_NUGGET, 3));
                break;
            case GIVE_WEAPON:
                player.getInventory().addItem(new Item(Item.ItemType.STEEL_SWORD));
                break;
            case BUFF_ATTACK:
                break; // handled by temporary buff system
            case BUFF_DEFENSE:
                break;
            case POISON:
                player.applyPoison(5f);
                player.getInventory().addItem(new Item(Item.ItemType.GOLD_NUGGET, 2));
                break;
            case XP_BONUS:
                player.addXP(75);
                break;
            case SPAWN_COMPANION:
                break; // handled by GameScreen
            case NOTHING:
                break;
        }
    }

    private Companion.PetType resolveCompanionType(GameEvent event) {
        if (event.title.contains("Wolf")) return Companion.PetType.CAVE_WOLF;
        if (event.title.contains("Sprite")) return Companion.PetType.FIRE_SPRITE;
        if (event.title.contains("Cat")) return Companion.PetType.SHADOW_CAT;
        return Companion.PetType.values()[random.nextInt(Companion.PetType.values().length)];
    }

    public EventEffect getLastEffect() { return lastEffect; }
    public Companion.PetType getLastCompanionType() { return lastCompanionType; }

    public void render() {
        if (!active)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float panelW = 450, panelH = 280;
        float px = sw / 2 - panelW / 2, py = sh / 2 - panelH / 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(0, 0, 0, 0.6f);
        game.shapeRenderer.rect(0, 0, sw, sh);

        game.shapeRenderer.setColor(0.08f, 0.07f, 0.1f, 0.95f);
        game.shapeRenderer.rect(px, py, panelW, panelH);

        // Accent
        game.shapeRenderer.setColor(currentEvent.color);
        game.shapeRenderer.rect(px, py + panelH - 3, panelW, 3);
        game.shapeRenderer.rect(px, py, panelW, 3);
        game.shapeRenderer.rect(px, py, 3, panelH);
        game.shapeRenderer.rect(px + panelW - 3, py, 3, panelH);

        if (!showingResult) {
            for (int i = 0; i < currentEvent.choices.length; i++) {
                float cy = py + panelH - 140 - i * 35;
                if (i == choiceSelection) {
                    game.shapeRenderer.setColor(0.2f, 0.25f, 0.35f, 0.7f);
                    game.shapeRenderer.rect(px + 20, cy - 5, panelW - 40, 30);
                }
            }
        }

        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;

        // Title
        nf.setColor(currentEvent.color);
        nf.draw(game.batch, currentEvent.title, px + 20, py + panelH - 15);

        if (showingResult) {
            nf.setColor(0.9f, 0.85f, 0.7f, 1f);
            nf.draw(game.batch, resultText, px + 20, py + panelH - 60, panelW - 40, -1, true);
            sf.setColor(0.5f, 0.5f, 0.45f, 0.7f);
            sf.draw(game.batch, "Press ENTER to continue", px + 20, py + 20);
        } else {
            sf.setColor(0.7f, 0.65f, 0.55f, 0.9f);
            sf.draw(game.batch, currentEvent.description, px + 20, py + panelH - 50, panelW - 40, -1, true);

            for (int i = 0; i < currentEvent.choices.length; i++) {
                float cy = py + panelH - 130 - i * 35;
                nf.setColor(i == choiceSelection ? new Color(1f, 0.9f, 0.3f, 1f) : new Color(0.7f, 0.65f, 0.55f, 1f));
                nf.draw(game.batch, (i == choiceSelection ? "> " : "  ") + currentEvent.choices[i].text, px + 25,
                        cy + 15);
            }
        }

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
