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
import com.caveadventure.ui.CaveUIStyle;

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

    /**
     * What a choice does. Rewards scale with the floor (see {@link Balance}): HEAL_30
     * heals 30 % of max HP, XP_BONUS and GIVE_GOLD grow per floor, GIVE_WEAPON gives
     * the floor band's weapon. Result texts show the real amounts through {@code %d}/{@code %s}.
     */
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
                    new EventChoice("Touch symbols", "A surge of power! +25% damage for 3 battles!",
                            EventEffect.BUFF_ATTACK),
                    new EventChoice("Leave it", "Wise caution. You move on.", EventEffect.NOTHING)),

            new GameEvent("Trapped Merchant",
                    "A merchant is trapped under fallen rocks. They promise a reward if freed...",
                    new Color(0.7f, 0.6f, 0.2f, 1f),
                    new EventChoice("Free them", "The merchant gives you %d gold nuggets!", EventEffect.GIVE_GOLD),
                    new EventChoice("Demand payment first", "They toss you a key before you help.",
                            EventEffect.GIVE_KEY)),

            new GameEvent("Strange Mushrooms",
                    "Bioluminescent mushrooms grow in a cluster. They look edible... maybe.",
                    new Color(0.3f, 0.8f, 0.4f, 1f),
                    new EventChoice("Eat them", "They're healing mushrooms! +%d HP!", EventEffect.HEAL_30),
                    new EventChoice("Avoid them", "Probably wise. Who eats cave mushrooms?", EventEffect.NOTHING)),

            new GameEvent("Hollow Echo",
                    "You hear a hollow echo behind a thin wall. Something is hidden there...",
                    new Color(0.4f, 0.5f, 0.6f, 1f),
                    new EventChoice("Break the wall", "You find a hidden %s!", EventEffect.GIVE_WEAPON),
                    new EventChoice("Listen closer", "You learn from the echoes. +%d XP!", EventEffect.XP_BONUS)),

            new GameEvent("Poison Gas",
                    "Green gas seeps from cracks in the floor. It burns your lungs...",
                    new Color(0.4f, 0.7f, 0.2f, 1f),
                    new EventChoice("Push through", "You're poisoned but find treasure!", EventEffect.POISON),
                    new EventChoice("Find another way", "You avoid the gas safely.", EventEffect.NOTHING)),

            new GameEvent("Ancient Spirit",
                    "A translucent spirit appears before you, its eyes filled with wisdom...",
                    new Color(0.6f, 0.7f, 0.9f, 1f),
                    new EventChoice("Ask for blessing", "The spirit shields you! -25% damage taken for 3 battles!", EventEffect.BUFF_DEFENSE),
                    new EventChoice("Ask for knowledge", "Ancient knowledge flows in. +%d XP!", EventEffect.XP_BONUS)),

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

    /** Chance that a completed exploration step starts an event. */
    public static final float EVENT_CHANCE_PER_STEP = 0.008f;
    /** Seconds of exploration after an event before another can start. */
    public static final float EVENT_COOLDOWN_SECONDS = 30f;
    /** Seconds after arriving on a floor before the first event can start. */
    public static final float EVENT_GRACE_SECONDS = 10f;
    /** Battles an attack or defense blessing lasts. */
    public static final int BUFF_BATTLES = 3;

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
    private int floor = 1;
    private EventEffect lastEffect;
    private Companion.PetType lastCompanionType;
    private String lastEventTitle;

    public RandomEventManager(CaveAdventure game) {
        this(game, new Random());
    }

    /**
     * Test seam: event manager whose rolls come from the given {@link Random}.
     */
    RandomEventManager(CaveAdventure game, Random random) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.glayout = new GlyphLayout();
        this.random = random;
        this.active = false;
        this.eventCooldown = 0;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Counts the cooldown down. Call every exploration frame.
     */
    public void tick(float delta) {
        if (eventCooldown > 0)
            eventCooldown = Math.max(0f, eventCooldown - delta);
    }

    /**
     * Closes any open event and starts the arrival grace period. Call on a new game
     * and on every floor load, so an event can't start the moment the player spawns.
     * {@code floor} sets the scale of event rewards.
     */
    public void reset(int floor) {
        this.floor = Math.max(1, floor);
        active = false;
        showingResult = false;
        currentEvent = null;
        eventCooldown = EVENT_GRACE_SECONDS;
    }

    /**
     * Rolls for an event; call once per completed step. Companion events are left out
     * of the pool when the player already has a companion. Returns true if one started.
     */
    public boolean tryTrigger(boolean hasCompanion) {
        if (eventCooldown > 0 || active)
            return false;
        if (random.nextFloat() < EVENT_CHANCE_PER_STEP) {
            List<GameEvent> pool = new ArrayList<>();
            for (GameEvent event : ALL_EVENTS) {
                if (!hasCompanion || !offersCompanion(event))
                    pool.add(event);
            }
            currentEvent = pool.get(random.nextInt(pool.size()));
            choiceSelection = 0;
            active = true;
            showingResult = false;
            return true;
        }
        return false;
    }

    /** True if one of the event's choices gives the player a companion. */
    static boolean offersCompanion(GameEvent event) {
        for (EventChoice choice : event.choices) {
            if (choice.effect == EventEffect.SPAWN_COMPANION)
                return true;
        }
        return false;
    }

    /** Seconds until the next event can start (exposed for tests). */
    float getEventCooldown() {
        return eventCooldown;
    }

    /** The event being shown, or the last one if none is open (exposed for tests). */
    GameEvent getCurrentEvent() {
        return currentEvent;
    }

    public void update(InputHandler input, Player player, float delta) {
        if (!active)
            return;

        if (showingResult) {
            if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
                lastEffect = pendingEffect;
                lastCompanionType = resolveCompanionType(currentEvent);
                lastEventTitle = currentEvent.title;
                applyEffect(pendingEffect, player);
                active = false;
                eventCooldown = EVENT_COOLDOWN_SECONDS;
            }
            return;
        }

        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            choiceSelection = (choiceSelection - 1 + currentEvent.choices.length) % currentEvent.choices.length;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            choiceSelection = (choiceSelection + 1) % currentEvent.choices.length;

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            EventChoice choice = currentEvent.choices[choiceSelection];
            resultText = resultTextFor(choice, player);
            pendingEffect = choice.effect;
            showingResult = true;
        }
    }

    /** The choice's result text with the real reward amount for this floor filled in. */
    String resultTextFor(EventChoice choice, Player player) {
        switch (choice.effect) {
            case HEAL_30:
                return String.format(choice.result, Balance.eventHeal(player.getMaxHealth()));
            case XP_BONUS:
                return String.format(choice.result, Balance.eventXp(floor));
            case GIVE_GOLD:
                return String.format(choice.result, Balance.eventGold(floor));
            case GIVE_WEAPON:
                return String.format(choice.result, Balance.bandWeapon(floor).displayName);
            default:
                return choice.result;
        }
    }

    /** Applies a choice's effect to the player (package-private for tests). */
    void applyEffect(EventEffect effect, Player player) {
        switch (effect) {
            case HEAL_30:
                player.heal(Balance.eventHeal(player.getMaxHealth()));
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
                player.getInventory().addItem(new Item(Item.ItemType.GOLD_NUGGET, Balance.eventGold(floor)));
                break;
            case GIVE_WEAPON:
                player.getInventory().addItem(new Item(Balance.bandWeapon(floor)));
                break;
            case BUFF_ATTACK:
                player.grantAttackBuff(BUFF_BATTLES);
                break;
            case BUFF_DEFENSE:
                player.grantDefenseBuff(BUFF_BATTLES);
                break;
            case POISON:
                player.applyPoison(5f);
                player.getInventory().addItem(new Item(Item.ItemType.GOLD_NUGGET, 2));
                break;
            case XP_BONUS:
                player.addXP(Balance.eventXp(floor));
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
    public String getLastEventTitle() { return lastEventTitle; }

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

        CaveUIStyle.drawStonePanel(game.shapeRenderer, px, py, panelW, panelH, 0.96f);
        game.shapeRenderer.setColor(currentEvent.color);
        game.shapeRenderer.rect(px + 12, py + panelH - 36, panelW - 24, 4);

        if (!showingResult) {
            for (int i = 0; i < currentEvent.choices.length; i++) {
                float cy = py + panelH - 140 - i * 35;
                if (i == choiceSelection) {
                    CaveUIStyle.drawSelection(game.shapeRenderer, px + 20, cy - 5, panelW - 40, 30, 1f);
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
            nf.setColor(CaveUIStyle.TEXT);
            nf.draw(game.batch, resultText, px + 20, py + panelH - 60, panelW - 40, -1, true);
            sf.setColor(CaveUIStyle.MUTED_TEXT);
            sf.draw(game.batch, "Press ENTER to continue", px + 20, py + 20);
        } else {
            sf.setColor(CaveUIStyle.TEXT);
            sf.draw(game.batch, currentEvent.description, px + 20, py + panelH - 50, panelW - 40, -1, true);

            for (int i = 0; i < currentEvent.choices.length; i++) {
                float cy = py + panelH - 130 - i * 35;
                nf.setColor(i == choiceSelection ? CaveUIStyle.GOLD : CaveUIStyle.TEXT);
                nf.draw(game.batch, (i == choiceSelection ? "> " : "  ") + currentEvent.choices[i].text, px + 25,
                        cy + 15);
            }
        }

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
