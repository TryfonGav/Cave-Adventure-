package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.engine.InputHandler;
import com.caveadventure.item.Inventory;
import com.caveadventure.world.GameMap;
import com.badlogic.gdx.Input;

/**
 * The player character with smooth grid movement (including diagonals),
 * combat stats, inventory, and survival mechanics.
 */
public class Player extends Entity {

    private int hunger;
    private int maxHunger;
    private int level;
    private int xp;
    private int xpToNextLevel;
    private float stamina;
    private float maxStamina;

    private final Inventory inventory;

    // Movement
    private boolean isMoving;
    private float moveProgress;
    private int targetGridX;
    private int targetGridY;
    private float startPixelX;
    private float startPixelY;
    private static final float MOVE_DURATION = 0.15f;

    // Animation
    private float animTimer;
    private int animFrame;
    private float damageFlashTimer;

    // Survival
    private float hungerTimer;
    private static final float HUNGER_TICK_INTERVAL = 8.0f;
    private boolean poisoned;
    private float poisonTimer;
    private float poisonDamageTimer;
    private float torchDuration;
    private float baseLightRadius = 4.5f;
    private float torchLightBonus = 3.0f;
    private static final float BASE_STAMINA = 100f;
    private static final float STAMINA_PER_LEVEL = 10f;
    private static final float STAMINA_REGEN_PER_SEC = 12f;

    // Skill: Berserker kill stacks (persist across battles within a floor)
    private int berserkerKillCount = 0;

    // Passive regen outside of battle (REGEN skill)
    private float passiveRegenTimer = 0f;
    private static final float PASSIVE_REGEN_INTERVAL = 4.0f;
    private static final int   PASSIVE_REGEN_AMOUNT   = 1;

    // Mana Crystal: reduce stamina costs until end of next battle
    private boolean manaCrystalActive = false;

    // Companion ability: used once per battle
    private boolean companionAbilityUsed = false;
    private CharacterAppearance appearance;

    // Visual
    private static final int SIZE = GameMap.TILE_SIZE;
    private static final Color OUTLINE_COLOR = new Color(0.05f, 0.06f, 0.09f, 1f);
    private static final Color EYE_COLOR = new Color(1f, 1f, 1f, 1f);
    private static final Color PUPIL_COLOR = new Color(0.1f, 0.1f, 0.15f, 1f);
    private static final Color POISON_TINT = new Color(0.3f, 0.8f, 0.2f, 1f);

    private final Color tmpBodyColor = new Color();

    public Player(int gridX, int gridY) {
        super(gridX, gridY, 100, 1.0f);
        this.hunger = 100;
        this.maxHunger = 100;
        this.level = 1;
        this.xp = 0;
        this.xpToNextLevel = 100;
        this.maxStamina = BASE_STAMINA;
        this.stamina = maxStamina;
        this.targetGridX = gridX;
        this.targetGridY = gridY;
        this.inventory = new Inventory();
        this.torchDuration = 30f;
        this.appearance = CharacterAppearance.defaultAppearance();
    }

    /**
     * Handle movement input — supports 8-directional movement (diagonals).
     */
    public void handleInput(InputHandler input, GameMap map, float delta) {
        if (isMoving)
            return;

        boolean up = input.isKeyDown(Input.Keys.UP) || input.isKeyDown(Input.Keys.W);
        boolean down = input.isKeyDown(Input.Keys.DOWN) || input.isKeyDown(Input.Keys.S);
        boolean left = input.isKeyDown(Input.Keys.LEFT) || input.isKeyDown(Input.Keys.A);
        boolean right = input.isKeyDown(Input.Keys.RIGHT) || input.isKeyDown(Input.Keys.D);

        // Cancel opposite directions
        if (up && down) {
            up = false;
            down = false;
        }
        if (left && right) {
            left = false;
            right = false;
        }

        Direction moveDir = null;

        // Diagonal first (two keys pressed)
        if (up && right)
            moveDir = Direction.NORTH_EAST;
        else if (up && left)
            moveDir = Direction.NORTH_WEST;
        else if (down && right)
            moveDir = Direction.SOUTH_EAST;
        else if (down && left)
            moveDir = Direction.SOUTH_WEST;
        // Cardinal
        else if (up)
            moveDir = Direction.NORTH;
        else if (down)
            moveDir = Direction.SOUTH;
        else if (left)
            moveDir = Direction.WEST;
        else if (right)
            moveDir = Direction.EAST;

        if (moveDir != null) {
            facing = moveDir;

            int newX = getGridX() + moveDir.dx;
            int newY = getGridY() + moveDir.dy;

            // For diagonals, check that both axis components are passable
            // (prevents cutting through wall corners)
            if (moveDir.dx != 0 && moveDir.dy != 0) {
                boolean horizPassable = map.isPassable(getGridX() + moveDir.dx, getGridY());
                boolean vertPassable = map.isPassable(getGridX(), getGridY() + moveDir.dy);
                if (map.isPassable(newX, newY) && horizPassable && vertPassable) {
                    startMoveTo(newX, newY);
                }
            } else {
                if (map.isPassable(newX, newY)) {
                    startMoveTo(newX, newY);
                }
            }
        }
    }

    private void startMoveTo(int newGridX, int newGridY) {
        isMoving = true;
        moveProgress = 0;
        targetGridX = newGridX;
        targetGridY = newGridY;
        startPixelX = pixelX;
        startPixelY = pixelY;
    }

    @Override
    public void update(float delta) {
        if (isMoving) {
            moveProgress += delta / MOVE_DURATION;
            if (moveProgress >= 1.0f) {
                moveProgress = 1.0f;
                x = targetGridX;
                y = targetGridY;
                pixelX = targetGridX * SIZE;
                pixelY = targetGridY * SIZE;
                isMoving = false;
            } else {
                float t = easeOutQuad(moveProgress);
                pixelX = startPixelX + (targetGridX * SIZE - startPixelX) * t;
                pixelY = startPixelY + (targetGridY * SIZE - startPixelY) * t;
            }
        }

        animTimer += delta;
        if (animTimer > 0.2f) {
            animTimer = 0;
            animFrame = (animFrame + 1) % 4;
        }

        if (damageFlashTimer > 0)
            damageFlashTimer -= delta;

        // Hunger
        hungerTimer += delta;
        if (hungerTimer >= HUNGER_TICK_INTERVAL) {
            hungerTimer = 0;
            if (hunger > 0)
                hunger--;
            else
                takeDamage(2);
        }

        // Poison
        if (poisoned) {
            poisonTimer -= delta;
            poisonDamageTimer += delta;
            if (poisonDamageTimer >= 1.5f) {
                poisonDamageTimer = 0;
                takeDamage(3);
            }
            if (poisonTimer <= 0)
                poisoned = false;
        }

        // Torch
        if (torchDuration > 0)
            torchDuration -= delta;

        stamina = Math.min(maxStamina, stamina + STAMINA_REGEN_PER_SEC * delta);

        inventory.updateMessageTimer(delta);
    }

    private float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    @Override
    public void render(ShapeRenderer renderer) {
        float px = pixelX;
        float py = pixelY;
        float bobOffset = isMoving ? (float) Math.sin(moveProgress * Math.PI) * 3f : 0;
        float step = isMoving ? (float) Math.sin(moveProgress * Math.PI * 2f) * 1.5f : 0;

        boolean flashWhite = damageFlashTimer > 0 && ((int) (damageFlashTimer * 10)) % 2 == 0;

        // Shadow
        renderer.setColor(0, 0, 0, 0.3f);
        renderer.ellipse(px + 5, py - 2, SIZE - 10, 8);

        Color bodyColor;
        if (flashWhite)
            bodyColor = Color.WHITE;
        else if (poisoned)
            bodyColor = animFrame % 2 == 0
                    ? POISON_TINT
                    : CharacterAppearance.scaledColorInto(POISON_TINT, 0.8333333f, 1f, tmpBodyColor);
        else {
            Color tunicColor = appearance.getTunicColor();
            bodyColor = animFrame % 2 == 0
                    ? tunicColor
                    : CharacterAppearance.scaledColorInto(tunicColor, 0.78f, tunicColor.a, tmpBodyColor);
        }

        // Legs and boots
        renderer.setColor(OUTLINE_COLOR);
        renderer.rect(px + 8, py + 3 + bobOffset - step, 6, 10);
        renderer.rect(px + 18, py + 3 + bobOffset + step, 6, 10);
        renderer.setColor(appearance.getPantsColor());
        renderer.rect(px + 9, py + 5 + bobOffset - step, 4, 8);
        renderer.rect(px + 19, py + 5 + bobOffset + step, 4, 8);
        renderer.setColor(appearance.getBootColor());
        renderer.rect(px + 7, py + 2 + bobOffset - step, 8, 4);
        renderer.rect(px + 17, py + 2 + bobOffset + step, 8, 4);

        // Torso, shoulders, and arms
        renderer.setColor(OUTLINE_COLOR);
        renderer.rect(px + 7, py + 10 + bobOffset, 18, 15);
        renderer.rect(px + 4, py + 11 + bobOffset, 5, 12);
        renderer.rect(px + 23, py + 11 + bobOffset, 5, 12);
        renderer.setColor(bodyColor);
        renderer.rect(px + 8, py + 11 + bobOffset, 16, 13);
        renderer.rect(px + 5, py + 12 + bobOffset, 4, 10);
        renderer.rect(px + 23, py + 12 + bobOffset, 4, 10);
        renderer.setColor(Math.min(1f, bodyColor.r * 1.18f), Math.min(1f, bodyColor.g * 1.18f),
                Math.min(1f, bodyColor.b * 1.18f), 1f);
        renderer.rect(px + 10, py + 20 + bobOffset, 12, 3);
        renderer.setColor(bodyColor.r * 0.65f, bodyColor.g * 0.65f, bodyColor.b * 0.65f, 1f);
        renderer.rect(px + 8, py + 11 + bobOffset, 16, 3);

        // Armor indicator
        if (inventory.getEquippedArmor() != null) {
            Color ac = inventory.getEquippedArmor().getType().color;
            renderer.setColor(ac.r * 0.65f, ac.g * 0.65f, ac.b * 0.65f, 0.9f);
            renderer.rect(px + 9, py + 13 + bobOffset, 14, 8);
            renderer.setColor(ac.r, ac.g, ac.b, 0.75f);
            renderer.rect(px + 11, py + 18 + bobOffset, 10, 2);
            renderer.rect(px + 11, py + 14 + bobOffset, 10, 2);
        }

        // Head, hair, and face
        renderer.setColor(OUTLINE_COLOR);
        renderer.rect(px + 8, py + 21 + bobOffset, 16, 10);
        renderer.rect(px + 10, py + 19 + bobOffset, 12, 3);
        renderer.setColor(appearance.getSkinColor());
        renderer.rect(px + 9, py + 21 + bobOffset, 14, 8);
        renderer.rect(px + 11, py + 19 + bobOffset, 10, 3);
        renderer.setColor(appearance.getHairColor());
        renderer.rect(px + 8, py + 27 + bobOffset, 16, 4);
        renderer.rect(px + 8, py + 24 + bobOffset, 3, 5);

        // Belt
        renderer.setColor(0.13f, 0.08f, 0.04f, 1f);
        renderer.rect(px + 8, py + 13 + bobOffset, 16, 2);
        renderer.setColor(0.86f, 0.66f, 0.24f, 1f);
        renderer.rect(px + 15, py + 13 + bobOffset, 3, 2);

        drawEquippedWeapon(renderer, px, py, bobOffset);

        // Eyes and face direction
        float eyeOffsetX = facing.dx * 2;
        float eyeOffsetY = facing.dy * -2;
        if (facing != Direction.NORTH) {
            renderer.setColor(EYE_COLOR);
            renderer.rect(px + 11 + eyeOffsetX, py + 24 + bobOffset + eyeOffsetY, 4, 3);
            renderer.rect(px + 18 + eyeOffsetX, py + 24 + bobOffset + eyeOffsetY, 4, 3);
            renderer.setColor(PUPIL_COLOR);
            renderer.rect(px + 12 + eyeOffsetX, py + 24 + bobOffset + eyeOffsetY, 2, 2);
            renderer.rect(px + 19 + eyeOffsetX, py + 24 + bobOffset + eyeOffsetY, 2, 2);
            renderer.setColor(0.35f, 0.16f, 0.12f, 1f);
            renderer.rect(px + 15 + eyeOffsetX, py + 22 + bobOffset + eyeOffsetY, 4, 1);
        } else {
            renderer.setColor(appearance.getHairColor());
            renderer.rect(px + 10, py + 23 + bobOffset, 12, 4);
        }
    }

    private void drawEquippedWeapon(ShapeRenderer renderer, float px, float py, float bobOffset) {
        if (inventory.getEquippedWeapon() == null)
            return;

        Color weaponColor = inventory.getEquippedWeapon().getType().color;
        renderer.setColor(0.10f, 0.07f, 0.04f, 1f);
        int fdx = facing.dx;
        int fdy = facing.dy;
        if (fdx < 0) {
            renderer.rect(px + 1, py + 11 + bobOffset, 4, 8);
            renderer.setColor(weaponColor);
            renderer.rect(px - 2, py + 17 + bobOffset, 4, 13);
            renderer.rect(px - 4, py + 28 + bobOffset, 8, 3);
        } else if (fdx > 0) {
            renderer.rect(px + SIZE - 5, py + 11 + bobOffset, 4, 8);
            renderer.setColor(weaponColor);
            renderer.rect(px + SIZE - 2, py + 17 + bobOffset, 4, 13);
            renderer.rect(px + SIZE - 4, py + 28 + bobOffset, 8, 3);
        } else if (fdy > 0) {
            renderer.rect(px + 23, py + 20 + bobOffset, 4, 8);
            renderer.setColor(weaponColor);
            renderer.rect(px + 25, py + 27 + bobOffset, 3, 10);
            renderer.rect(px + 23, py + 35 + bobOffset, 7, 3);
        } else {
            renderer.rect(px + 24, py + 8 + bobOffset, 4, 8);
            renderer.setColor(weaponColor);
            renderer.rect(px + 26, py - 2 + bobOffset, 3, 12);
            renderer.rect(px + 24, py - 4 + bobOffset, 7, 3);
        }
    }

    @Override
    public boolean takeDamage(int amount) {
        int defense = inventory.getTotalDefenseBonus();
        int reduced = Math.max(1, amount - defense / 3);
        damageFlashTimer = 0.4f;
        return super.takeDamage(reduced);
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void applyPoison(float duration) {
        poisoned = true;
        poisonTimer = duration;
        poisonDamageTimer = 0;
    }

    public void clearPoison() {
        poisoned = false;
        poisonTimer = 0;
    }

    /**
     * Call each frame during exploration (not during battle).
     * Heals a small amount if the REGEN skill is active.
     */
    public void tickPassiveRegen(com.caveadventure.engine.SkillTree skillTree, float delta) {
        if (skillTree == null || !skillTree.hasSkill(com.caveadventure.engine.SkillTree.Skill.REGEN))
            return;
        if (!isAlive() || health >= maxHealth)
            return;
        passiveRegenTimer += delta;
        if (passiveRegenTimer >= PASSIVE_REGEN_INTERVAL) {
            passiveRegenTimer = 0f;
            heal(PASSIVE_REGEN_AMOUNT);
        }
    }

    public void addTorchDuration(float seconds) {
        torchDuration += seconds;
    }

    public float getLightRadius() {
        float radius = baseLightRadius;
        if (torchDuration > 0)
            radius += torchLightBonus;
        if (hunger < 20)
            radius -= 1.0f;
        return Math.max(2f, radius);
    }

    public void modifyHunger(int amount) {
        hunger = Math.max(0, Math.min(maxHunger, hunger + amount));
    }

    public void addXP(int amount) {
        xp += amount;
        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel;
            level++;
            xpToNextLevel = (int) (xpToNextLevel * 1.25f);
            maxHealth += 10;
            health = maxHealth;
            maxStamina = BASE_STAMINA + (level - 1) * STAMINA_PER_LEVEL;
            stamina = maxStamina;
        }
    }

    public boolean useStamina(float amount) {
        if (stamina < amount)
            return false;
        stamina -= amount;
        return true;
    }

    public void restoreProgressFromSave(int savedLevel, int savedXp, int savedXpToNext,
            int savedMaxHealth, int savedHealth) {
        this.level = Math.max(1, savedLevel);
        this.xp = Math.max(0, savedXp);
        this.xpToNextLevel = Math.max(1, savedXpToNext);
        this.maxHealth = Math.max(1, savedMaxHealth);
        this.health = Math.max(0, Math.min(this.maxHealth, savedHealth));
        this.alive = this.health > 0;

        this.maxStamina = BASE_STAMINA + (this.level - 1) * STAMINA_PER_LEVEL;
        this.stamina = Math.min(this.stamina, this.maxStamina);
    }

    public void setStamina(float value) {
        stamina = Math.max(0f, Math.min(maxStamina, value));
    }

    // --- Getters ---
    public int getHunger() {
        return hunger;
    }

    public int getMaxHunger() {
        return maxHunger;
    }

    public int getLevel() {
        return level;
    }

    public int getXP() {
        return xp;
    }

    public int getXPToNextLevel() {
        return xpToNextLevel;
    }

    public float getStamina() {
        return stamina;
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public CharacterAppearance getAppearance() {
        return appearance;
    }

    public void setAppearance(CharacterAppearance appearance) {
        this.appearance = appearance == null ? CharacterAppearance.defaultAppearance() : appearance.copy();
    }

    public boolean isPoisoned() {
        return poisoned;
    }

    public int getBerserkerKillCount() { return berserkerKillCount; }
    public void addBerserkerKill() { berserkerKillCount++; }
    public void resetBerserkerKills() { berserkerKillCount = 0; }

    public float getPoisonRemaining() {
        return poisoned ? Math.max(0f, poisonTimer) : 0f;
    }

    public float getTorchDuration() {
        return torchDuration;
    }

    public int getTotalAttack() {
        return 10 + inventory.getTotalAttackBonus();
    }

    // --- Mana Crystal ---
    public void activateManaCrystal()  { manaCrystalActive = true; }
    public boolean isManaCrystalActive() { return manaCrystalActive; }
    /** Call at end of battle to consume the effect. */
    public void consumeManaCrystal()   { manaCrystalActive = false; }

    // --- Companion Ability ---
    public boolean isCompanionAbilityUsed()  { return companionAbilityUsed; }
    public void setCompanionAbilityUsed(boolean v) { companionAbilityUsed = v; }
}
