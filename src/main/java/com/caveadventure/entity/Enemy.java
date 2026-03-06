package com.caveadventure.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.world.GameMap;

import java.util.*;

/**
 * Enemy entity with AI behavior: patrol, chase, and attack.
 */
public class Enemy extends Entity {

    public enum EnemyType {
        BAT("Bat", 25, 2.5f, 5, 8, new Color(0.5f, 0.2f, 0.6f, 1f), 15),
        SLIME("Slime", 40, 1.0f, 8, 12, new Color(0.2f, 0.8f, 0.3f, 1f), 25),
        SKELETON("Skeleton", 60, 1.5f, 12, 18, new Color(0.85f, 0.85f, 0.8f, 1f), 40),
        GOBLIN("Goblin", 45, 1.8f, 10, 15, new Color(0.4f, 0.6f, 0.2f, 1f), 30),
        CAVE_SPIDER("Spider", 30, 2.2f, 7, 10, new Color(0.3f, 0.15f, 0.1f, 1f), 20),
        NECROMANCER("Necromancer", 55, 1.2f, 14, 20, new Color(0.4f, 0.1f, 0.5f, 1f), 55),
        SHADOW("Shadow", 35, 3.0f, 10, 16, new Color(0.15f, 0.1f, 0.2f, 1f), 45),
        ICE_DRAKE("Ice Drake", 90, 1.4f, 18, 28, new Color(0.5f, 0.7f, 0.9f, 1f), 70),
        BOSS_GOLEM("Stone Golem", 200, 0.8f, 25, 35, new Color(0.5f, 0.45f, 0.4f, 1f), 150);

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
    }

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
        Direction[] dirs = Direction.cardinals();
        List<Direction> shuffled = new ArrayList<>(Arrays.asList(dirs));
        Collections.shuffle(shuffled, random);

        for (Direction dir : shuffled) {
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
        Color darkColor = new Color(bodyColor.r * 0.7f, bodyColor.g * 0.7f, bodyColor.b * 0.7f, 1f);

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
        float wingOffset = (float) Math.sin(animTimer * 15) * 4;
        // Wings
        r.setColor(dark);
        r.rect(px + 2, py + 12 + wingOffset, 10, 8);
        r.rect(px + s - 12, py + 12 - wingOffset, 10, 8);
        // Body
        r.setColor(body);
        r.rect(px + 10, py + 8, s - 20, s - 12);
        // Eyes (red)
        r.setColor(1f, 0.2f, 0.1f, 1f);
        r.rect(px + 11, py + 16, 3, 3);
        r.rect(px + 18, py + 16, 3, 3);
    }

    private void renderSlime(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float squish = (float) Math.sin(animTimer * 5) * 2;
        // Body (blob shape)
        r.setColor(body);
        r.rect(px + 4, py + 2 - squish, s - 8, s - 6 + squish);
        r.rect(px + 6, py + s - 8, s - 12, 6);
        // Shine
        r.setColor(1f, 1f, 1f, 0.3f);
        r.rect(px + 8, py + s - 10, 6, 4);
        // Eyes
        r.setColor(0, 0, 0, 0.9f);
        r.rect(px + 10, py + 14, 4, 5);
        r.rect(px + 18, py + 14, 4, 5);
    }

    private void renderSkeleton(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        // Skull
        r.setColor(body);
        r.rect(px + 8, py + 14, s - 16, s - 16);
        r.rect(px + 6, py + 18, s - 12, s - 22);
        // Eye sockets
        r.setColor(0.1f, 0.05f, 0.05f, 1f);
        r.rect(px + 10, py + 20, 4, 4);
        r.rect(px + 18, py + 20, 4, 4);
        // Jaw
        r.setColor(dark);
        r.rect(px + 10, py + 14, s - 20, 4);
        // Ribs
        r.setColor(body);
        r.rect(px + 10, py + 4, 3, 10);
        r.rect(px + 15, py + 4, 3, 10);
        r.rect(px + 20, py + 4, 3, 10);
    }

    private void renderGoblin(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        // Body
        r.setColor(body);
        r.rect(px + 6, py + 4, s - 12, s - 8);
        // Head
        r.setColor(body);
        r.rect(px + 8, py + s - 12, s - 16, 10);
        // Ears (pointy)
        r.setColor(dark);
        r.rect(px + 4, py + s - 8, 4, 6);
        r.rect(px + s - 8, py + s - 8, 4, 6);
        // Eyes (yellow, menacing)
        r.setColor(1f, 0.9f, 0.1f, 1f);
        r.rect(px + 11, py + 20, 4, 3);
        r.rect(px + 19, py + 20, 4, 3);
        // Pupils
        r.setColor(0.1f, 0.05f, 0f, 1f);
        r.rect(px + 12, py + 20, 2, 2);
        r.rect(px + 20, py + 20, 2, 2);
    }

    private void renderSpider(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float legMove = (float) Math.sin(animTimer * 10) * 2;
        // Legs
        r.setColor(dark);
        for (int i = 0; i < 4; i++) {
            float offset = (i % 2 == 0) ? legMove : -legMove;
            r.rect(px + 2, py + 6 + i * 5 + offset, 6, 2);
            r.rect(px + s - 8, py + 6 + i * 5 - offset, 6, 2);
        }
        // Body
        r.setColor(body);
        r.rect(px + 8, py + 4, s - 16, s - 8);
        r.rect(px + 10, py + 2, s - 20, s - 4);
        // Eyes (8 red dots)
        r.setColor(1f, 0f, 0f, 1f);
        r.rect(px + 10, py + 20, 2, 2);
        r.rect(px + 14, py + 22, 2, 2);
        r.rect(px + 18, py + 22, 2, 2);
        r.rect(px + 22, py + 20, 2, 2);
        r.rect(px + 11, py + 17, 2, 2);
        r.rect(px + 15, py + 18, 2, 2);
        r.rect(px + 19, py + 18, 2, 2);
        r.rect(px + 21, py + 17, 2, 2);
    }

    private void renderGolem(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        // Large body
        r.setColor(body);
        r.rect(px + 2, py + 2, s - 4, s - 4);
        r.rect(px, py + 6, s, s - 12);
        // Face cracks
        r.setColor(dark);
        r.rect(px + 6, py + s - 12, 2, 8);
        r.rect(px + s - 8, py + s - 10, 2, 6);
        // Glowing eyes
        float glow = (float) Math.sin(animTimer * 3) * 0.2f + 0.8f;
        r.setColor(glow, glow * 0.3f, 0, 1f);
        r.rect(px + 8, py + 20, 5, 5);
        r.rect(px + 19, py + 20, 5, 5);
        // Crystal core
        r.setColor(0.8f, 0.4f, 1f, 0.8f);
        r.rect(px + 12, py + 10, 8, 8);
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
        // Robe
        r.setColor(body);
        r.rect(px + 6, py + 2, s - 12, s - 6);
        r.rect(px + 8, py + s - 8, s - 16, 6);
        // Hood
        r.setColor(dark);
        r.rect(px + 8, py + s - 10, s - 16, 8);
        // Face (shadowed)
        r.setColor(0.15f, 0.05f, 0.15f, 1f);
        r.rect(px + 10, py + 14, s - 20, 8);
        // Eyes (glowing purple)
        r.setColor(0.8f, 0.3f, 1f, 1f);
        r.rect(px + 12, py + 18, 3, 3);
        r.rect(px + s - 15, py + 18, 3, 3);
        // Staff
        r.setColor(0.6f, 0.4f, 0.2f, 1f);
        r.rect(px + s - 5, py + 2, 3, s - 2);
        r.setColor(0.7f, 0.2f, 1f, 0.8f);
        r.rect(px + s - 7, py + s - 4, 7, 5);
    }

    private void renderShadow(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        float wave = (float) Math.sin(animTimer * 8) * 2;
        // Translucent body
        r.setColor(body.r, body.g, body.b, 0.5f);
        r.rect(px + 6, py + 4 + wave, s - 12, s - 8);
        r.rect(px + 8, py + 2 + wave, s - 16, s - 4);
        // Dark core
        r.setColor(0.05f, 0.02f, 0.08f, 0.7f);
        r.rect(px + 10, py + 8 + wave, s - 20, s - 16);
        // Eyes (eerie glow)
        r.setColor(0.9f, 0.9f, 1f, 0.9f);
        r.rect(px + 11, py + 16 + wave, 4, 4);
        r.rect(px + s - 15, py + 16 + wave, 4, 4);
        // Wisps
        r.setColor(body.r, body.g, body.b, 0.3f);
        r.rect(px + 4, py + 10 + wave * 2, 4, 8);
        r.rect(px + s - 8, py + 14 - wave, 4, 6);
    }

    private void renderIceDrake(ShapeRenderer r, float px, float py, int s, Color body, Color dark) {
        // Body
        r.setColor(body);
        r.rect(px + 4, py + 4, s - 8, s - 10);
        r.rect(px + 6, py + 2, s - 12, s - 4);
        // Head
        r.setColor(dark);
        r.rect(px + 6, py + s - 10, s - 12, 8);
        // Frost crown
        r.setColor(0.7f, 0.9f, 1f, 0.9f);
        r.rect(px + 8, py + s - 4, 3, 5);
        r.rect(px + 14, py + s - 4, 3, 6);
        r.rect(px + 20, py + s - 4, 3, 4);
        // Eyes
        r.setColor(0.3f, 0.5f, 1f, 1f);
        r.rect(px + 10, py + 18, 4, 4);
        r.rect(px + s - 14, py + 18, 4, 4);
        // Tail
        r.setColor(body.r * 0.8f, body.g * 0.8f, body.b * 0.9f, 1f);
        r.rect(px - 4, py + 6, 8, 4);
        r.rect(px - 6, py + 8, 4, 3);
    }
}
