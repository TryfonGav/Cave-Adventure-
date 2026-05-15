package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.engine.Difficulty;
import com.caveadventure.world.GameMap;

import java.util.*;

/**
 * Enemy entity with AI behavior: patrol, chase, and attack.
 */
public class Enemy extends Entity {

    public enum EnemyType {
        BAT("Bat", 25, 2.5f, 5, 8, new Color(0.5f, 0.2f, 0.6f, 1f), 30),
        SLIME("Slime", 40, 1.0f, 8, 12, new Color(0.2f, 0.8f, 0.3f, 1f), 50),
        SKELETON("Skeleton", 60, 1.5f, 12, 18, new Color(0.85f, 0.85f, 0.8f, 1f), 80),
        GOBLIN("Goblin", 45, 1.8f, 10, 15, new Color(0.4f, 0.6f, 0.2f, 1f), 60),
        CAVE_SPIDER("Spider", 30, 2.2f, 7, 10, new Color(0.3f, 0.15f, 0.1f, 1f), 40),
        NECROMANCER("Necromancer", 55, 1.2f, 14, 20, new Color(0.4f, 0.1f, 0.5f, 1f), 110),
        SHADOW("Shadow", 35, 3.0f, 10, 16, new Color(0.15f, 0.1f, 0.2f, 1f), 90),
        ICE_DRAKE("Ice Drake", 90, 1.4f, 18, 28, new Color(0.5f, 0.7f, 0.9f, 1f), 140),
        BOSS_GOLEM("Stone Golem", 200, 0.8f, 25, 35, new Color(0.5f, 0.45f, 0.4f, 1f), 300);

        public final String name;
        public final int maxHealth;
        public final float speed;
        public final int minDamage;
        public final int maxDamage;
        public final Color color;
        public final int xpReward;

        EnemyType(String name, int maxHealth, float speed, int minDamage, int maxDamage, Color color, int xpReward) {
            this.name = name;
            this.maxHealth = maxHealth;
            this.speed = speed;
            this.minDamage = minDamage;
            this.maxDamage = maxDamage;
            this.color = color;
            this.xpReward = xpReward;
        }
    }

    public enum AIState {
        IDLE, PATROL, CHASE, ATTACK, FLEE
    }

    private final EnemyType type;
    private AIState aiState;
    private final Random random = new Random();
    private final Color tmpDarkColor = new Color();
    private final List<Direction> shuffledPatrolDirections = new ArrayList<>(4);

    private static final Direction[] CARDINAL_DIRECTIONS = Direction.cardinals();

    // Movement
    private boolean isMoving;
    private float moveProgress;
    private int targetGridX, targetGridY;
    private float startPixelX, startPixelY;
    private float moveDuration;

    // AI timers
    private float idleTimer;
    private float idleDuration;
    private float attackCooldown;
    private static final float ATTACK_COOLDOWN_TIME = 0.8f;

    // Detection
    private static final int DETECTION_RANGE = 8;
    private static final int ATTACK_RANGE = 1;
    private static final int FLEE_HEALTH_PERCENT = 20;

    // Animation
    private float animTimer;
    private int animFrame;
    private float damageFlashTimer;
    private float deathTimer;

    // Patrol path
    private int patrolOriginX, patrolOriginY;
    private static final int PATROL_RADIUS = 5;

    // Scaled combat stats (set when battle begins)
    private int scaledMinDamage;
    private int scaledMaxDamage;

    public Enemy(int gridX, int gridY, EnemyType type) {
        super(gridX, gridY, type.maxHealth, type.speed);
        this.type = type;
        this.aiState = AIState.IDLE;
        this.idleTimer = 0;
        this.idleDuration = 1f + random.nextFloat() * 2f;
        this.attackCooldown = 0;
        this.patrolOriginX = gridX;
        this.patrolOriginY = gridY;
        this.moveDuration = 0.3f / type.speed;
        this.targetGridX = gridX;
        this.targetGridY = gridY;
        this.scaledMinDamage = type.minDamage;
        this.scaledMaxDamage = type.maxDamage;
    }

    /**
     * Scale this enemy's HP and damage to the player's current level.
     * Call once at the start of each battle.
     */
    public void scaleToPlayer(int playerLevel) {
        scaleToPlayer(playerLevel, 1);
    }

    /**
     * Scale this enemy's HP and damage to the player's current level AND
     * the current floor (for difficulty floor-scaling).
     */
    public void scaleToPlayer(int playerLevel, int floor) {
        Difficulty diff = Difficulty.getCurrent();
        float levelFactor = 1.0f + (playerLevel - 1) * 0.12f;
        float hpFactor    = levelFactor * diff.getEnemyHealthForFloor(floor);
        float dmgFactor   = levelFactor * diff.getEnemyDamageForFloor(floor);

        this.maxHealth        = Math.round(type.maxHealth * hpFactor);
        this.health           = this.maxHealth;
        this.scaledMinDamage  = Math.round(type.minDamage  * dmgFactor);
        this.scaledMaxDamage  = Math.round(type.maxDamage  * dmgFactor);
    }

    public int getScaledMinDamage() { return scaledMinDamage; }
    public int getScaledMaxDamage() { return scaledMaxDamage; }

    /**
     * AI behavior tick. Called each frame with player position.
     */
    public void updateAI(Player player, GameMap map, float delta) {
        if (!alive) {
            deathTimer += delta;
            return;
        }

        attackCooldown = Math.max(0, attackCooldown - delta);

        int playerGX = player.getGridX();
        int playerGY = player.getGridY();
        int dist = Math.abs(getGridX() - playerGX) + Math.abs(getGridY() - playerGY);

        // State transitions
        if (health < maxHealth * FLEE_HEALTH_PERCENT / 100f && type != EnemyType.BOSS_GOLEM) {
            aiState = AIState.FLEE;
        } else if (dist <= ATTACK_RANGE && attackCooldown <= 0) {
            aiState = AIState.ATTACK;
        } else if (dist <= DETECTION_RANGE) {
            aiState = AIState.CHASE;
        } else if (aiState == AIState.CHASE || aiState == AIState.ATTACK) {
            aiState = AIState.PATROL;
        }

        // Behavior
        switch (aiState) {
            case IDLE:
                idleTimer += delta;
                if (idleTimer >= idleDuration) {
                    aiState = AIState.PATROL;
                    idleTimer = 0;
                }
                break;

            case PATROL:
                if (!isMoving) {
                    // Pick a random adjacent passable tile near origin
                    tryMoveRandom(map);
                    if (random.nextFloat() < 0.2f) {
                        aiState = AIState.IDLE;
                        idleDuration = 1f + random.nextFloat() * 2f;
                    }
                }
                break;

            case CHASE:
                if (!isMoving) {
                    moveTowards(playerGX, playerGY, map);
                }
                break;

            case ATTACK:
                // Turn-based: don't do real-time damage, just face player
                // Battle is triggered by CombatManager proximity check
                attackCooldown = ATTACK_COOLDOWN_TIME;
                aiState = AIState.IDLE;
                idleDuration = 0.5f;
                break;

            case FLEE:
                if (!isMoving) {
                    moveAwayFrom(playerGX, playerGY, map);
                }
                // Recover from flee if health restored
                if (health > maxHealth * 0.3f) {
                    aiState = AIState.PATROL;
                }
                break;
        }
    }

    private void tryMoveRandom(GameMap map) {
        shuffledPatrolDirections.clear();
        Collections.addAll(shuffledPatrolDirections, CARDINAL_DIRECTIONS);
        Collections.shuffle(shuffledPatrolDirections, random);

        for (Direction dir : shuffledPatrolDirections) {
            int nx = getGridX() + dir.dx;
            int ny = getGridY() + dir.dy;
            int distFromOrigin = Math.abs(nx - patrolOriginX) + Math.abs(ny - patrolOriginY);
            if (map.isPassable(nx, ny) && distFromOrigin <= PATROL_RADIUS) {
                startMoveTo(nx, ny);
                facing = dir;
                return;
            }
        }
    }

    private void moveTowards(int targetX, int targetY, GameMap map) {
        int bestDx = 0, bestDy = 0;
        int bestDist = Integer.MAX_VALUE;

        int[][] moves = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] m : moves) {
            int nx = getGridX() + m[0];
            int ny = getGridY() + m[1];
            if (map.isPassable(nx, ny)) {
                int dist = Math.abs(nx - targetX) + Math.abs(ny - targetY);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestDx = m[0];
                    bestDy = m[1];
                }
            }
        }

        if (bestDx != 0 || bestDy != 0) {
            int nx = getGridX() + bestDx;
            int ny = getGridY() + bestDy;
            startMoveTo(nx, ny);
            updateFacing(bestDx, bestDy);
        }
    }

    private void moveAwayFrom(int fleeX, int fleeY, GameMap map) {
        int bestDx = 0, bestDy = 0;
        int bestDist = -1;

        int[][] moves = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] m : moves) {
            int nx = getGridX() + m[0];
            int ny = getGridY() + m[1];
            if (map.isPassable(nx, ny)) {
                int dist = Math.abs(nx - fleeX) + Math.abs(ny - fleeY);
                if (dist > bestDist) {
                    bestDist = dist;
                    bestDx = m[0];
                    bestDy = m[1];
                }
            }
        }

        if (bestDx != 0 || bestDy != 0) {
            startMoveTo(getGridX() + bestDx, getGridY() + bestDy);
            updateFacing(bestDx, bestDy);
        }
    }

    private void updateFacing(int dx, int dy) {
        if (dy > 0)
            facing = Direction.NORTH;
        else if (dy < 0)
            facing = Direction.SOUTH;
        else if (dx > 0)
            facing = Direction.EAST;
        else if (dx < 0)
            facing = Direction.WEST;
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
        if (!alive)
            return;

        // Smooth movement interpolation
        if (isMoving) {
            moveProgress += delta / moveDuration;
            if (moveProgress >= 1.0f) {
                x = targetGridX;
                y = targetGridY;
                pixelX = targetGridX * GameMap.TILE_SIZE;
                pixelY = targetGridY * GameMap.TILE_SIZE;
                isMoving = false;
            } else {
                float t = moveProgress;
                pixelX = startPixelX + (targetGridX * GameMap.TILE_SIZE - startPixelX) * t;
                pixelY = startPixelY + (targetGridY * GameMap.TILE_SIZE - startPixelY) * t;
            }
        }

        // Animation
        animTimer += delta;
        if (animTimer > 0.25f) {
            animTimer = 0;
            animFrame = (animFrame + 1) % 4;
        }

        if (damageFlashTimer > 0)
            damageFlashTimer -= delta;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        if (!alive) {
            // Death fade-out
            if (deathTimer < 0.5f) {
                float alpha = 1f - deathTimer / 0.5f;
                renderer.setColor(type.color.r, type.color.g, type.color.b, alpha);
                renderer.rect(pixelX + 8, pixelY + 4, GameMap.TILE_SIZE - 16, GameMap.TILE_SIZE - 8);
            }
            return;
        }

        float px = pixelX;
        float py = pixelY;
        int size = GameMap.TILE_SIZE;

        boolean flash = damageFlashTimer > 0 && ((int) (damageFlashTimer * 10)) % 2 == 0;
        Color bodyColor = flash ? Color.WHITE : type.color;
        Color darkColor = CharacterAppearance.scaledColorInto(bodyColor, 0.7f, 1f, tmpDarkColor);

        // Shadow
        renderer.setColor(0, 0, 0, 0.25f);
        renderer.ellipse(px + 4, py - 1, size - 8, 6);

        switch (type) {
            case BAT:
                renderBat(renderer, px, py, size, bodyColor, darkColor);
                break;
            case SLIME:
                renderSlime(renderer, px, py, size, bodyColor, darkColor);
                break;
            case SKELETON:
                renderSkeleton(renderer, px, py, size, bodyColor, darkColor);
                break;
            case GOBLIN:
                renderGoblin(renderer, px, py, size, bodyColor, darkColor);
                break;
            case CAVE_SPIDER:
                renderSpider(renderer, px, py, size, bodyColor, darkColor);
                break;
            case NECROMANCER:
                renderNecromancer(renderer, px, py, size, bodyColor, darkColor);
                break;
            case SHADOW:
                renderShadow(renderer, px, py, size, bodyColor, darkColor);
                break;
            case ICE_DRAKE:
                renderIceDrake(renderer, px, py, size, bodyColor, darkColor);
                break;
            case BOSS_GOLEM:
                renderGolem(renderer, px, py, size, bodyColor, darkColor);
                break;
        }

        // Health bar above enemy (only when damaged)
        if (health < maxHealth) {
            float barWidth = size - 4;
            float barHeight = 3;
            float barX = px + 2;
            float barY = py + size + 2;
            float healthPercent = (float) health / maxHealth;

            renderer.setColor(0.2f, 0.05f, 0.05f, 0.8f);
            renderer.rect(barX, barY, barWidth, barHeight);
            renderer.setColor(0.9f, 0.15f, 0.15f, 1f);
            renderer.rect(barX, barY, barWidth * healthPercent, barHeight);
        }
    }

    private void renderBat(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float flap = (float) Math.sin(animTimer * 18f) * 4f;
        r.setColor(0.04f, 0.03f, 0.06f, 1f);
        r.triangle(px + 15, py + 16, px + 2, py + 25 + flap, px + 4, py + 9 - flap);
        r.triangle(px + 17, py + 16, px + s - 2, py + 25 - flap, px + s - 4, py + 9 + flap);
        r.setColor(dark);
        r.triangle(px + 15, py + 16, px + 5, py + 22 + flap, px + 7, py + 11 - flap);
        r.triangle(px + 17, py + 16, px + s - 5, py + 22 - flap, px + s - 7, py + 11 + flap);
        r.setColor(body);
        r.rect(px + 11, py + 10, 10, 13);
        r.rect(px + 9, py + 14, 14, 7);
        r.setColor(dark);
        r.triangle(px + 11, py + 22, px + 13, py + 27, px + 15, py + 22);
        r.triangle(px + 17, py + 22, px + 19, py + 27, px + 21, py + 22);
        r.setColor(1f, 0.22f, 0.12f, 1f);
        r.rect(px + 12, py + 17, 3, 3);
        r.rect(px + 18, py + 17, 3, 3);
        r.setColor(0.95f, 0.88f, 0.75f, 1f);
        r.rect(px + 14, py + 12, 2, 3);
        r.rect(px + 17, py + 12, 2, 3);
    }

    private void renderSlime(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float squish = (float) Math.sin(animTimer * 6f) * 2f;
        r.setColor(0.03f, 0.08f, 0.04f, 0.85f);
        r.rect(px + 3, py + 3 - squish, s - 6, s - 9 + squish);
        r.setColor(body);
        r.rect(px + 5, py + 4 - squish, s - 10, s - 11 + squish);
        r.rect(px + 8, py + s - 9, s - 16, 5);
        r.setColor(body.r * 0.65f, body.g * 0.65f, body.b * 0.65f, 1f);
        r.rect(px + 6, py + 4 - squish, s - 12, 5);
        r.setColor(1f, 1f, 1f, 0.32f);
        r.rect(px + 9, py + s - 12, 7, 4);
        r.rect(px + 18, py + s - 16, 4, 3);
        r.setColor(0.02f, 0.03f, 0.02f, 0.95f);
        r.rect(px + 10, py + 14, 4, 5);
        r.rect(px + 19, py + 14, 4, 5);
        r.setColor(0.75f, 1f, 0.65f, 0.8f);
        r.rect(px + 11, py + 18, 2, 1);
        r.rect(px + 20, py + 18, 2, 1);
    }

    private void renderSkeleton(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        r.setColor(0.08f, 0.07f, 0.06f, 1f);
        r.rect(px + 7, py + 15, 18, 14);
        r.rect(px + 10, py + 5, 12, 12);
        r.setColor(body);
        r.rect(px + 8, py + 16, 16, 12);
        r.rect(px + 10, py + 14, 12, 3);
        r.setColor(dark);
        r.rect(px + 11, py + 20, 4, 4);
        r.rect(px + 18, py + 20, 4, 4);
        r.rect(px + 13, py + 15, 6, 2);
        r.setColor(body);
        r.rect(px + 15, py + 6, 3, 9);
        r.rect(px + 9, py + 11, 14, 2);
        r.rect(px + 9, py + 8, 14, 2);
        r.rect(px + 7, py + 5, 3, 8);
        r.rect(px + 22, py + 5, 3, 8);
        r.rect(px + 11, py + 2, 4, 5);
        r.rect(px + 18, py + 2, 4, 5);
        r.setColor(0.7f, 0.68f, 0.58f, 1f);
        r.rect(px + 25, py + 8, 2, 16);
    }

    private void renderGoblin(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        r.setColor(0.04f, 0.08f, 0.03f, 1f);
        r.triangle(px + 9, py + 22, px + 1, py + 26, px + 9, py + 17);
        r.triangle(px + 23, py + 22, px + s - 1, py + 26, px + 23, py + 17);
        r.rect(px + 6, py + 6, 20, 18);
        r.setColor(body);
        r.triangle(px + 9, py + 22, px + 3, py + 24, px + 9, py + 18);
        r.triangle(px + 23, py + 22, px + s - 3, py + 24, px + 23, py + 18);
        r.rect(px + 8, py + 15, 16, 11);
        r.setColor(0.35f, 0.20f, 0.12f, 1f);
        r.rect(px + 8, py + 5, 16, 11);
        r.setColor(dark);
        r.rect(px + 10, py + 13, 12, 3);
        r.setColor(1f, 0.88f, 0.12f, 1f);
        r.rect(px + 11, py + 20, 4, 3);
        r.rect(px + 18, py + 20, 4, 3);
        r.setColor(0.08f, 0.04f, 0f, 1f);
        r.rect(px + 12, py + 20, 2, 2);
        r.rect(px + 19, py + 20, 2, 2);
        r.setColor(0.55f, 0.55f, 0.52f, 1f);
        r.rect(px + 24, py + 7, 3, 14);
        r.rect(px + 23, py + 19, 6, 3);
    }

    private void renderSpider(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float legMove = (float) Math.sin(animTimer * 12f) * 2f;
        r.setColor(0.05f, 0.02f, 0.01f, 1f);
        for (int i = 0; i < 4; i++) {
            float y = py + 7 + i * 4;
            float offset = (i % 2 == 0) ? legMove : -legMove;
            r.rect(px + 1, y + offset, 9, 2);
            r.rect(px + s - 10, y - offset, 9, 2);
        }
        r.setColor(dark);
        r.rect(px + 7, py + 7, 18, 16);
        r.setColor(body);
        r.rect(px + 9, py + 9, 14, 14);
        r.rect(px + 11, py + 5, 10, 9);
        r.setColor(Math.min(1f, body.r * 1.18f), Math.min(1f, body.g * 1.18f),
                Math.min(1f, body.b * 1.18f), 1f);
        r.rect(px + 12, py + 18, 8, 3);
        r.setColor(1f, 0.05f, 0.02f, 1f);
        r.rect(px + 10, py + 19, 2, 2);
        r.rect(px + 14, py + 21, 2, 2);
        r.rect(px + 18, py + 21, 2, 2);
        r.rect(px + 22, py + 19, 2, 2);
        r.rect(px + 12, py + 16, 2, 2);
        r.rect(px + 18, py + 16, 2, 2);
    }

    private void renderGolem(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        r.setColor(0.08f, 0.07f, 0.06f, 1f);
        r.rect(px + 2, py + 3, s - 4, s - 6);
        r.setColor(body);
        r.rect(px + 5, py + 5, 22, 20);
        r.rect(px + 8, py + 22, 16, 7);
        r.rect(px + 1, py + 9, 7, 12);
        r.rect(px + 24, py + 9, 7, 12);
        r.setColor(dark);
        r.rect(px + 6, py + 20, 2, 7);
        r.rect(px + 21, py + 22, 2, 5);
        r.rect(px + 10, py + 7, 12, 3);
        r.setColor(Math.min(1f, body.r * 1.15f), Math.min(1f, body.g * 1.15f),
                Math.min(1f, body.b * 1.15f), 1f);
        r.rect(px + 6, py + 23, 15, 2);
        float glow = (float) Math.sin(animTimer * 4f) * 0.2f + 0.8f;
        r.setColor(glow, glow * 0.32f, 0.04f, 1f);
        r.rect(px + 9, py + 18, 5, 4);
        r.rect(px + 18, py + 18, 5, 4);
        r.setColor(0.75f, 0.35f, 1f, 0.9f);
        r.rect(px + 12, py + 11, 8, 7);
    }

    @Override
    public boolean takeDamage(int amount) {
        damageFlashTimer = 0.3f;
        return super.takeDamage(amount);
    }

    // --- Getters ---
    public EnemyType getType() {
        return type;
    }

    public AIState getAIState() {
        return aiState;
    }

    public boolean isDeathAnimationDone() {
        return !alive && deathTimer > 0.5f;
    }

    // --- New enemy renders ---

    private void renderNecromancer(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float sway = (float) Math.sin(animTimer * 4f);
        r.setColor(0.05f, 0.02f, 0.07f, 1f);
        r.rect(px + 5, py + 2, 20, 27);
        r.setColor(body);
        r.rect(px + 7, py + 4, 16, 22);
        r.triangle(px + 7, py + 4, px + 16, py + 14, px + 23, py + 4);
        r.setColor(dark);
        r.rect(px + 8, py + 21, 16, 7);
        r.triangle(px + 8, py + 21, px + 16, py + 30, px + 24, py + 21);
        r.setColor(0.10f, 0.03f, 0.12f, 1f);
        r.rect(px + 11, py + 17, 10, 6);
        r.setColor(0.85f, 0.30f, 1f, 1f);
        r.rect(px + 12, py + 19, 3, 2);
        r.rect(px + 18, py + 19, 3, 2);
        r.setColor(0.58f, 0.34f, 0.18f, 1f);
        r.rect(px + 26 + sway, py + 3, 3, 25);
        r.setColor(0.75f, 0.20f, 1f, 0.9f);
        r.rect(px + 24 + sway, py + 26, 7, 5);
        r.setColor(0.95f, 0.65f, 1f, 0.45f);
        r.rect(px + 25 + sway, py + 27, 5, 3);
    }

    private void renderShadow(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float wave = (float) Math.sin(animTimer * 8f) * 2f;
        r.setColor(body.r, body.g, body.b, 0.28f);
        r.rect(px + 4, py + 5 + wave, 24, 18);
        r.rect(px + 8, py + 1 + wave, 16, 25);
        r.setColor(body.r, body.g, body.b, 0.62f);
        r.triangle(px + 8, py + 4 + wave, px + 16, py + 28 + wave, px + 24, py + 4 + wave);
        r.rect(px + 9, py + 8 + wave, 14, 15);
        r.setColor(0.03f, 0.01f, 0.06f, 0.86f);
        r.rect(px + 11, py + 10 + wave, 10, 11);
        r.setColor(0.88f, 0.92f, 1f, 0.95f);
        r.rect(px + 11, py + 18 + wave, 4, 3);
        r.rect(px + 18, py + 18 + wave, 4, 3);
        r.setColor(body.r, body.g, body.b, 0.35f);
        r.rect(px + 4, py + 10 + wave * 2f, 4, 8);
        r.rect(px + s - 8, py + 14 - wave, 4, 7);
        r.rect(px + 15, py + 1 - wave, 3, 7);
    }

    private void renderIceDrake(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        r.setColor(0.04f, 0.09f, 0.12f, 1f);
        r.rect(px + 3, py + 5, 23, 18);
        r.triangle(px + 5, py + 12, px - 3, py + 16, px + 5, py + 18);
        r.setColor(body);
        r.rect(px + 5, py + 6, 20, 16);
        r.triangle(px + 5, py + 11, px - 4, py + 15, px + 5, py + 18);
        r.setColor(dark);
        r.rect(px + 8, py + 20, 15, 7);
        r.rect(px + 20, py + 15, 7, 6);
        r.setColor(0.72f, 0.92f, 1f, 0.95f);
        r.triangle(px + 9, py + 26, px + 11, py + 32, px + 13, py + 26);
        r.triangle(px + 15, py + 26, px + 17, py + 32, px + 19, py + 26);
        r.triangle(px + 21, py + 24, px + 24, py + 30, px + 25, py + 23);
        r.rect(px + 7, py + 9, 16, 2);
        r.setColor(0.26f, 0.48f, 1f, 1f);
        r.rect(px + 11, py + 22, 4, 3);
        r.rect(px + 20, py + 20, 3, 3);
        r.setColor(0.85f, 0.98f, 1f, 0.4f);
        r.rect(px + 9, py + 15, 10, 3);
    }
}
