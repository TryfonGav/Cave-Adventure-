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

    // Visual
    private static final int SIZE = GameMap.TILE_SIZE;
    private static final Color BODY_COLOR = new Color(0.2f, 0.6f, 0.9f, 1f);
    private static final Color BODY_COLOR_2 = new Color(0.15f, 0.5f, 0.8f, 1f);
    private static final Color EYE_COLOR = new Color(1f, 1f, 1f, 1f);
    private static final Color PUPIL_COLOR = new Color(0.1f, 0.1f, 0.15f, 1f);
    private static final Color POISON_TINT = new Color(0.3f, 0.8f, 0.2f, 1f);

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
        float bobOffset = isMoving ? (float) Math.sin(moveProgress * Math.PI) * 2 : 0;

        boolean flashWhite = damageFlashTimer > 0 && ((int) (damageFlashTimer * 10)) % 2 == 0;

        // Shadow
        renderer.setColor(0, 0, 0, 0.3f);
        renderer.ellipse(px + 4, py - 2, SIZE - 8, 8);

        Color bodyColor;
        if (flashWhite)
            bodyColor = Color.WHITE;
        else if (poisoned)
            bodyColor = animFrame % 2 == 0 ? POISON_TINT : new Color(0.25f, 0.65f, 0.3f, 1f);
        else
            bodyColor = animFrame % 2 == 0 ? BODY_COLOR : BODY_COLOR_2;

        // Body
        renderer.setColor(bodyColor);
        renderer.rect(px + 4, py + 4 + bobOffset, SIZE - 8, SIZE - 8);
        renderer.rect(px + 2, py + 6 + bobOffset, SIZE - 4, SIZE - 12);

        // Helmet
        renderer.setColor(bodyColor.r * 0.8f, bodyColor.g * 0.8f, bodyColor.b * 0.8f, 1f);
        renderer.rect(px + 6, py + SIZE - 8 + bobOffset, SIZE - 12, 6);

        // Weapon indicator
        if (inventory.getEquippedWeapon() != null) {
            Color wc = inventory.getEquippedWeapon().getType().color;
            renderer.setColor(wc);
            // Show weapon on the correct side based on cardinal direction of facing
            int fdx = facing.dx;
            int fdy = facing.dy;
            if (fdx > 0)
                renderer.rect(px + SIZE - 2, py + 10 + bobOffset, 4, 12);
            else if (fdx < 0)
                renderer.rect(px - 2, py + 10 + bobOffset, 4, 12);
            else if (fdy > 0)
                renderer.rect(px + SIZE - 6, py + SIZE + bobOffset, 4, 8);
            else
                renderer.rect(px + 4, py - 4 + bobOffset, 4, 8);
        }

        // Armor indicator
        if (inventory.getEquippedArmor() != null) {
            Color ac = inventory.getEquippedArmor().getType().color;
            renderer.setColor(ac.r, ac.g, ac.b, 0.7f);
            renderer.rect(px + 2, py + SIZE - 10 + bobOffset, 4, 6);
            renderer.rect(px + SIZE - 6, py + SIZE - 10 + bobOffset, 4, 6);
        }

        // Eyes
        float eyeOffsetX = facing.dx * 2;
        float eyeOffsetY = facing.dy * -2;
        // Only skip eyes if facing directly away
        if (facing != Direction.NORTH) {
            renderer.setColor(EYE_COLOR);
            renderer.rect(px + 8 + eyeOffsetX, py + 16 + bobOffset + eyeOffsetY, 5, 5);
            renderer.rect(px + 19 + eyeOffsetX, py + 16 + bobOffset + eyeOffsetY, 5, 5);
            renderer.setColor(PUPIL_COLOR);
            renderer.rect(px + 9 + eyeOffsetX, py + 17 + bobOffset + eyeOffsetY, 3, 3);
            renderer.rect(px + 20 + eyeOffsetX, py + 17 + bobOffset + eyeOffsetY, 3, 3);
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
}
