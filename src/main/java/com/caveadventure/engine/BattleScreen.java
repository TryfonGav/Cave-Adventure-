package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import com.caveadventure.item.LootTable;

import java.util.*;

/**
 * Pokemon-style turn-based battle with special weapon moves, status effects,
 * and boss abilities.
 */
public class BattleScreen {

    public enum BattleState {
        INTRO, PLAYER_TURN, PLAYER_ITEM, PLAYER_SKILL,
        PLAYER_ATTACK, ENEMY_TURN, ENEMY_ATTACK,
        MESSAGE, VICTORY, DEFEAT, RUN_AWAY
    }

    // Status effects
    public enum StatusEffect {
        NONE, POISON, BURN, STUN
    }

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final Random random = new Random();

    private Player player;
    private Enemy enemy;
    private BattleState state;
    private float stateTimer;
    private boolean battleActive;
    private boolean battleResult;
    private boolean bossKilled;

    // Menu
    private int menuSelection;
    private final String[] mainMenu = { "Attack", "Skills", "Items", "Run" };
    private int subSelection;

    // Messages
    private String currentMessage;
    private BattleState nextState;

    // Combat
    private boolean playerDefending;
    private int turnCount;
    private StatusEffect enemyStatus;
    private float enemyStatusTimer;
    private StatusEffect playerStatus;
    private float playerStatusTimer;

    // Animation
    private float attackFlashTimer;
    private boolean attackFlashTarget;
    private float shakeTimer;
    private float shakeTargetEnemy;

    // Skills (based on weapon type)
    private String[] currentSkills;

    // Skill tree (unlockable passive skills)
    private SkillTree skillTree;
    private boolean lastStandAvailable;

    // Loot
    private List<Item> lootDrops;
    private int currentFloor;

    public BattleScreen(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.battleActive = false;
    }

    public void startBattle(Player player, Enemy enemy, int floor) {
        startBattle(player, enemy, floor, null);
    }

    public void startBattle(Player player, Enemy enemy, int floor, SkillTree skillTree) {
        this.player = player;
        this.enemy = enemy;
        this.currentFloor = floor;
        this.skillTree = skillTree;
        this.lastStandAvailable = skillTree != null && skillTree.hasSkill(SkillTree.Skill.LAST_STAND);
        this.state = BattleState.INTRO;
        this.stateTimer = 0;
        this.battleActive = true;
        this.battleResult = false;
        this.bossKilled = false;
        this.menuSelection = 0;
        this.subSelection = 0;
        this.playerDefending = false;
        this.turnCount = 0;
        this.attackFlashTimer = 0;
        this.shakeTimer = 0;
        this.shakeTargetEnemy = 0;
        this.lootDrops = null;
        this.enemyStatus = StatusEffect.NONE;
        this.enemyStatusTimer = 0;
        this.playerStatus = StatusEffect.NONE;
        this.playerStatusTimer = 0;
        this.currentMessage = "A wild " + enemy.getType().name + " appeared!";
        enemy.scaleToPlayer(player.getLevel());
        updateSkills();
    }

    private void updateSkills() {
        Item weapon = player.getInventory().getEquippedWeapon();
        if (weapon == null) {
            currentSkills = new String[] { "Power Hit", "Focus" };
        } else {
            Item.ItemType wt = weapon.getType();
            if (wt == Item.ItemType.FIRE_AXE) {
                currentSkills = new String[] { "Cleave", "Burn Strike", "Focus" };
            } else if (wt == Item.ItemType.CRYSTAL_BLADE) {
                currentSkills = new String[] { "Pierce", "Stun Slash", "Focus" };
            } else if (wt == Item.ItemType.STEEL_SWORD) {
                currentSkills = new String[] { "Heavy Slash", "Guard Break", "Focus" };
            } else {
                currentSkills = new String[] { "Power Hit", "Poison Jab", "Focus" };
            }
        }
    }

    /**
     * Apply damage to the player, accounting for IRON_SKIN reduction and
     * LAST_STAND survival.
     */
    private void applyDamageToPlayer(int damage) {
        if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.IRON_SKIN))
            damage = Math.max(1, (int)(damage * 0.85f));
        player.takeDamage(damage);
        if (!player.isAlive() && lastStandAvailable) {
            player.setHealth(1);
            lastStandAvailable = false;
            currentMessage += " [Last Stand activates!]";
        }
    }

    /** Apply skill-tree offensive multipliers to a base damage value. */
    private int applyOffensiveSkills(int baseDmg) {
        if (skillTree == null) return baseDmg;
        if (skillTree.hasSkill(SkillTree.Skill.POWER_SURGE))
            baseDmg = (int)(baseDmg * 1.2f);
        if (skillTree.hasSkill(SkillTree.Skill.FURY)
                && player.getHealth() < player.getMaxHealth() * 0.3f)
            baseDmg = (int)(baseDmg * 1.5f);
        if (skillTree.hasSkill(SkillTree.Skill.BERSERKER) && player.getBerserkerKillCount() > 0)
            baseDmg = (int)(baseDmg * (1f + player.getBerserkerKillCount() * 0.05f));
        return baseDmg;
    }

    public boolean isActive() {
        return battleActive;
    }

    public boolean getResult() {
        return battleResult;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public boolean wasBossKilled() {
        return bossKilled;
    }

    public void update(InputHandler input, float delta) {
        if (!battleActive)
            return;

        stateTimer += delta;
        if (attackFlashTimer > 0)
            attackFlashTimer -= delta;
        if (shakeTimer > 0)
            shakeTimer -= delta;

        // Status ticks
        if (enemyStatus != StatusEffect.NONE) {
            enemyStatusTimer -= delta;
            if (enemyStatusTimer <= 0)
                enemyStatus = StatusEffect.NONE;
        }
        if (playerStatus != StatusEffect.NONE) {
            playerStatusTimer -= delta;
            if (playerStatusTimer <= 0)
                playerStatus = StatusEffect.NONE;
        }

        switch (state) {
            case INTRO:
                if (stateTimer > 1.5f) {
                    state = BattleState.PLAYER_TURN;
                    stateTimer = 0;
                    currentMessage = "What will you do?";
                }
                break;

            case PLAYER_TURN:
                if (playerStatus == StatusEffect.STUN) {
                    currentMessage = "You are stunned and can't move!";
                    playerStatus = StatusEffect.NONE;
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                    break;
                }
                handleMainMenu(input);
                break;

            case PLAYER_SKILL:
                handleSkillMenu(input);
                break;

            case PLAYER_ITEM:
                handleItemMenu(input);
                break;

            case PLAYER_ATTACK:
                if (stateTimer > 0.8f) {
                    if (!enemy.isAlive()) {
                        int xpReward = enemy.getType().xpReward;
                        player.addXP(xpReward);
                        boolean lucky = skillTree != null && skillTree.hasSkill(SkillTree.Skill.LUCKY);
                        lootDrops = LootTable.getEnemyDrop(enemy.getType(), currentFloor, lucky);
                        for (Item item : lootDrops)
                            player.getInventory().addItem(item);
                        if (enemy.getType() == Enemy.EnemyType.BOSS_GOLEM)
                            bossKilled = true;
                        if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.BERSERKER))
                            player.addBerserkerKill();
                        showMessage(enemy.getType().name + " defeated! +" + xpReward + " XP", BattleState.VICTORY);
                    } else {
                        state = BattleState.ENEMY_TURN;
                        stateTimer = 0;
                    }
                }
                break;

            case ENEMY_TURN:
                if (stateTimer > 0.5f) {
                    if (enemyStatus == StatusEffect.STUN) {
                        currentMessage = enemy.getType().name + " is stunned!";
                        enemyStatus = StatusEffect.NONE;
                        state = BattleState.ENEMY_ATTACK;
                        stateTimer = 0;
                        break;
                    }
                    performEnemyAttack();
                }
                break;

            case ENEMY_ATTACK:
                if (stateTimer > 1.0f) {
                    if (!player.isAlive()) {
                        showMessage("You were defeated...", BattleState.DEFEAT);
                    } else {
                        // Poison/burn tick damage
                        if (playerStatus == StatusEffect.POISON) {
                            applyDamageToPlayer(3);
                            currentMessage += " [Poison: -3 HP]";
                        } else if (playerStatus == StatusEffect.BURN) {
                            applyDamageToPlayer(5);
                            currentMessage += " [Burn: -5 HP]";
                        }
                        // REGEN: heal 2 HP per turn
                        if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.REGEN) && player.isAlive())
                            player.heal(2);
                        state = BattleState.PLAYER_TURN;
                        stateTimer = 0;
                        currentMessage = "What will you do?";
                        turnCount++;
                    }
                }
                break;

            case MESSAGE:
                if (stateTimer > 1.5f || input.isKeyJustPressed(Input.Keys.ENTER) ||
                        input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (nextState != null) {
                        state = nextState;
                        stateTimer = 0;
                        nextState = null;
                    }
                }
                break;

            case VICTORY:
                if (stateTimer > 1.5f || input.isKeyJustPressed(Input.Keys.ENTER)) {
                    battleActive = false;
                    battleResult = true;
                }
                break;

            case DEFEAT:
                if (stateTimer > 2.0f || input.isKeyJustPressed(Input.Keys.ENTER)) {
                    battleActive = false;
                    battleResult = false;
                }
                break;

            case RUN_AWAY:
                if (stateTimer > 1.0f) {
                    battleActive = false;
                    battleResult = false;
                }
                break;
        }
    }

    // --- Menus ---

    private void handleMainMenu(InputHandler input) {
        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            menuSelection = (menuSelection + 2) % 4;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            menuSelection = (menuSelection + 2) % 4;
        if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A))
            menuSelection = (menuSelection - 1 + 4) % 4;
        if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D))
            menuSelection = (menuSelection + 1) % 4;

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            switch (menuSelection) {
                case 0:
                    performBasicAttack();
                    break;
                case 1:
                    state = BattleState.PLAYER_SKILL;
                    subSelection = 0;
                    currentMessage = "Choose a skill:";
                    break;
                case 2:
                    if (hasUsableItems()) {
                        state = BattleState.PLAYER_ITEM;
                        subSelection = 0;
                        currentMessage = "Choose an item:";
                    } else {
                        currentMessage = "No usable items!";
                    }
                    break;
                case 3:
                    attemptRun();
                    break;
            }
        }
    }

    private void handleSkillMenu(InputHandler input) {
        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            subSelection = (subSelection - 1 + currentSkills.length) % currentSkills.length;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            subSelection = (subSelection + 1) % currentSkills.length;

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            performSkill(currentSkills[subSelection]);
        }
        if (input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = BattleState.PLAYER_TURN;
            currentMessage = "What will you do?";
        }
    }

    private void handleItemMenu(InputHandler input) {
        List<Item> usable = getUsableItems();
        if (usable.isEmpty()) {
            state = BattleState.PLAYER_TURN;
            currentMessage = "What will you do?";
            return;
        }

        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            subSelection = (subSelection - 1 + usable.size()) % usable.size();
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            subSelection = (subSelection + 1) % usable.size();

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            Item item = usable.get(subSelection);
            int idx = player.getInventory().getItems().indexOf(item);
            if (idx >= 0) {
                player.getInventory().useItem(idx, player);
                currentMessage = "Used " + item.getType().displayName + "!";
                state = BattleState.ENEMY_TURN;
                stateTimer = 0;
            }
        }
        if (input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = BattleState.PLAYER_TURN;
            currentMessage = "What will you do?";
        }
    }

    // --- Actions ---

    private void performBasicAttack() {
        int baseDmg = applyOffensiveSkills(player.getTotalAttack());
        float critChance = 0.12f;
        if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.CRITICAL_MASTER))
            critChance += 0.15f;
        boolean crit = random.nextFloat() < critChance;
        int dmg = baseDmg + random.nextInt(6);
        if (crit)
            dmg = (int) (dmg * 1.8f);

        enemy.takeDamage(dmg);
        triggerAttackAnim(true);
        currentMessage = crit ? "Critical hit! " + dmg + " damage!" : "You attack for " + dmg + " damage!";
        state = BattleState.PLAYER_ATTACK;
        stateTimer = 0;
    }

    private void performSkill(String skill) {
        int staminaCost = getSkillStaminaCost(skill);
        if (!player.useStamina(staminaCost)) {
            currentMessage = "Not enough stamina! (" + (int) player.getStamina() + "/" + staminaCost + ")";
            return;
        }

        int baseDmg = applyOffensiveSkills(player.getTotalAttack());
        int dmg;

        switch (skill) {
            case "Power Hit":
                dmg = (int) (baseDmg * 1.5f) + random.nextInt(8);
                enemy.takeDamage(dmg);
                triggerAttackAnim(true);
                currentMessage = "Power Hit! " + dmg + " damage!";
                break;

            case "Heavy Slash":
                dmg = (int) (baseDmg * 1.6f) + random.nextInt(10);
                enemy.takeDamage(dmg);
                triggerAttackAnim(true);
                currentMessage = "Heavy Slash! " + dmg + " damage!";
                break;

            case "Cleave":
                dmg = (int) (baseDmg * 1.4f) + random.nextInt(12);
                enemy.takeDamage(dmg);
                triggerAttackAnim(true);
                currentMessage = "Cleave! " + dmg + " damage!";
                break;

            case "Pierce":
                dmg = baseDmg + 15 + random.nextInt(5); // ignores some defense
                enemy.takeDamage(dmg);
                triggerAttackAnim(true);
                currentMessage = "Pierce! " + dmg + " armor-piercing damage!";
                break;

            case "Burn Strike":
                dmg = baseDmg + random.nextInt(8);
                enemy.takeDamage(dmg);
                enemyStatus = StatusEffect.BURN;
                enemyStatusTimer = 3f;
                triggerAttackAnim(true);
                currentMessage = "Burn Strike! " + dmg + " dmg + BURN applied!";
                break;

            case "Stun Slash":
                dmg = (int) (baseDmg * 0.8f) + random.nextInt(5);
                enemy.takeDamage(dmg);
                if (random.nextFloat() < 0.6f) {
                    enemyStatus = StatusEffect.STUN;
                    enemyStatusTimer = 1f;
                    currentMessage = "Stun Slash! " + dmg + " dmg + STUNNED!";
                } else {
                    currentMessage = "Stun Slash! " + dmg + " dmg (stun missed)";
                }
                triggerAttackAnim(true);
                break;

            case "Guard Break":
                dmg = baseDmg + 10;
                enemy.takeDamage(dmg);
                triggerAttackAnim(true);
                currentMessage = "Guard Break! " + dmg + " dmg + defense shattered!";
                break;

            case "Poison Jab":
                dmg = (int) (baseDmg * 0.7f) + random.nextInt(4);
                enemy.takeDamage(dmg);
                enemyStatus = StatusEffect.POISON;
                enemyStatusTimer = 4f;
                triggerAttackAnim(true);
                currentMessage = "Poison Jab! " + dmg + " dmg + POISONED!";
                break;

            case "Focus":
                playerDefending = true;
                currentMessage = "You focus your energy! Next attack +50% damage!";
                state = BattleState.ENEMY_TURN;
                stateTimer = 0;
                return;

            default:
                performBasicAttack();
                return;
        }

        state = BattleState.PLAYER_ATTACK;
        stateTimer = 0;
    }

    private int getSkillStaminaCost(String skill) {
        switch (skill) {
            case "Power Hit":
                return 20;
            case "Heavy Slash":
                return 24;
            case "Cleave":
                return 22;
            case "Pierce":
                return 18;
            case "Burn Strike":
                return 26;
            case "Stun Slash":
                return 25;
            case "Guard Break":
                return 20;
            case "Poison Jab":
                return 18;
            case "Focus":
                return 12;
            default:
                return 15;
        }
    }

    private void performEnemyAttack() {
        int minDmg = enemy.getScaledMinDamage();
        int maxDmg = enemy.getScaledMaxDamage();
        int damage = minDmg + random.nextInt(maxDmg - minDmg + 1);

        // Enemy status ticks
        if (enemyStatus == StatusEffect.POISON) {
            enemy.takeDamage(4);
            currentMessage = "[Poison ticks -4 HP on " + enemy.getType().name + "] ";
        } else if (enemyStatus == StatusEffect.BURN) {
            enemy.takeDamage(6);
            currentMessage = "[Burn ticks -6 HP on " + enemy.getType().name + "] ";
        } else {
            currentMessage = "";
        }

        // Boss special moves
        if (enemy.getType() == Enemy.EnemyType.BOSS_GOLEM) {
            performBossAttack(damage);
            return;
        }

        // Spider poison
        if (enemy.getType() == Enemy.EnemyType.CAVE_SPIDER && random.nextFloat() < 0.25f) {
            if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.STATUS_RESIST)
                    && random.nextFloat() < 0.5f) {
                currentMessage += enemy.getType().name + " bites! " + damage + " dmg (Resisted poison!)";
            } else {
                playerStatus = StatusEffect.POISON;
                playerStatusTimer = 3f;
                currentMessage += enemy.getType().name + " bites! " + damage + " dmg + POISON!";
            }
        } else {
            currentMessage += enemy.getType().name + " attacks for " + damage + " damage!";
        }

        if (playerDefending) {
            damage = damage / 3;
            playerDefending = false;
            currentMessage = "You focused! Reduced to " + damage + " damage!";
        }

        applyDamageToPlayer(damage);
        triggerAttackAnim(false);
        state = BattleState.ENEMY_ATTACK;
        stateTimer = 0;
    }

    private void performBossAttack(int baseDamage) {
        float roll = random.nextFloat();

        if (roll < 0.25f) {
            // AoE Slam
            int dmg = (int) (baseDamage * 1.4f);
            if (playerDefending) {
                dmg /= 3;
                playerDefending = false;
            }
            applyDamageToPlayer(dmg);
            currentMessage += "Stone Golem uses GROUND SLAM! " + dmg + " damage!";
        } else if (roll < 0.4f) {
            // Self heal
            int heal = 20 + random.nextInt(15);
            enemy.heal(heal);
            currentMessage += "Stone Golem regenerates " + heal + " HP!";
        } else if (roll < 0.6f) {
            // Multi-hit (2 hits)
            int hit1 = baseDamage / 2 + random.nextInt(5);
            int hit2 = baseDamage / 2 + random.nextInt(5);
            if (playerDefending) {
                hit1 /= 3;
                hit2 /= 3;
                playerDefending = false;
            }
            applyDamageToPlayer(hit1);
            applyDamageToPlayer(hit2);
            currentMessage += "Stone Golem DOUBLE STRIKE! " + hit1 + "+" + hit2 + " damage!";
        } else if (roll < 0.75f) {
            // Stun attack
            int dmg = baseDamage;
            if (playerDefending) {
                dmg /= 3;
                playerDefending = false;
            }
            applyDamageToPlayer(dmg);
            playerStatus = StatusEffect.STUN;
            playerStatusTimer = 1f;
            currentMessage += "Stone Golem PETRIFY STRIKE! " + dmg + " dmg + STUNNED!";
        } else {
            // Normal attack
            int dmg = baseDamage;
            if (playerDefending) {
                dmg /= 3;
                playerDefending = false;
            }
            applyDamageToPlayer(dmg);
            currentMessage += "Stone Golem attacks for " + dmg + " damage!";
        }

        triggerAttackAnim(false);
        state = BattleState.ENEMY_ATTACK;
        stateTimer = 0;
    }

    private void attemptRun() {
        if (enemy.getType() == Enemy.EnemyType.BOSS_GOLEM) {
            currentMessage = "Can't escape from a boss!";
            return;
        }
        float runChance = 0.5f + (turnCount * 0.1f);
        if (random.nextFloat() < runChance) {
            currentMessage = "Got away safely!";
            state = BattleState.RUN_AWAY;
            stateTimer = 0;
        } else {
            currentMessage = "Couldn't escape!";
            state = BattleState.ENEMY_TURN;
            stateTimer = 0;
        }
    }

    private void triggerAttackAnim(boolean targetEnemy) {
        attackFlashTimer = 0.4f;
        attackFlashTarget = targetEnemy;
        shakeTimer = 0.3f;
        shakeTargetEnemy = targetEnemy ? 1 : -1;
    }

    private boolean hasUsableItems() {
        return !getUsableItems().isEmpty();
    }

    private List<Item> getUsableItems() {
        List<Item> usable = new ArrayList<>();
        for (Item item : player.getInventory().getItems()) {
            if (item.isUsable())
                usable.add(item);
        }
        return usable;
    }

    private void showMessage(String msg, BattleState next) {
        currentMessage = msg;
        state = BattleState.MESSAGE;
        stateTimer = 0;
        nextState = next;
    }

    // ========== RENDERING ==========

    public void render() {
        if (!battleActive)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Battle background
        game.shapeRenderer.setColor(0.08f, 0.07f, 0.12f, 1f);
        game.shapeRenderer.rect(0, sh * 0.35f, sw, sh * 0.65f);
        game.shapeRenderer.setColor(0.12f, 0.10f, 0.08f, 1f);
        game.shapeRenderer.rect(0, 0, sw, sh * 0.35f);
        game.shapeRenderer.setColor(0.2f, 0.17f, 0.13f, 1f);
        game.shapeRenderer.rect(0, sh * 0.35f - 2, sw, 4);

        // Cave details
        for (int i = 0; i < 15; i++) {
            float sx = i * (sw / 15f);
            float sH = 20 + (float) Math.sin(i * 2.1) * 30;
            game.shapeRenderer.setColor(0.06f, 0.05f, 0.09f, 1f);
            game.shapeRenderer.rect(sx, sh - sH, sw / 15f + 2, sH);
        }

        // Enemy platform
        float enemyPlatX = sw * 0.6f;
        float enemyPlatY = sh * 0.45f;
        game.shapeRenderer.setColor(0.15f, 0.12f, 0.1f, 1f);
        game.shapeRenderer.ellipse(enemyPlatX - 60, enemyPlatY - 15, 160, 30);

        // Player platform
        float playerPlatX = sw * 0.2f;
        float playerPlatY = sh * 0.28f;
        game.shapeRenderer.setColor(0.15f, 0.12f, 0.1f, 1f);
        game.shapeRenderer.ellipse(playerPlatX - 50, playerPlatY - 15, 140, 30);

        // Sprites
        float eSh = shakeTargetEnemy > 0 && shakeTimer > 0 ? (float) Math.sin(shakeTimer * 40) * 5 : 0;
        boolean eFlash = attackFlashTarget && attackFlashTimer > 0 && ((int) (attackFlashTimer * 15)) % 2 == 0;
        if (enemy.isAlive())
            drawBattleEnemy(game.shapeRenderer, enemyPlatX + eSh, enemyPlatY + 10, eFlash);

        float pSh = shakeTargetEnemy < 0 && shakeTimer > 0 ? (float) Math.sin(shakeTimer * 40) * 5 : 0;
        boolean pFlash = !attackFlashTarget && attackFlashTimer > 0 && ((int) (attackFlashTimer * 15)) % 2 == 0;
        drawBattlePlayer(game.shapeRenderer, playerPlatX + pSh, playerPlatY + 5, pFlash);

        // Enemy HP box
        float ehpX = sw * 0.55f, ehpY = sh * 0.78f, ehpW = sw * 0.38f, ehpH = 60;
        game.shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.9f);
        game.shapeRenderer.rect(ehpX, ehpY, ehpW, ehpH);
        drawBorder(game.shapeRenderer, ehpX, ehpY, ehpW, ehpH);

        float hpPct = (float) enemy.getHealth() / enemy.getMaxHealth();
        Color hpC = hpPct > 0.5f ? new Color(0.2f, 0.8f, 0.3f, 1f)
                : hpPct > 0.2f ? new Color(0.9f, 0.7f, 0.1f, 1f) : new Color(0.9f, 0.2f, 0.15f, 1f);
        game.shapeRenderer.setColor(0.15f, 0.1f, 0.1f, 1f);
        game.shapeRenderer.rect(ehpX + 10, ehpY + 10, ehpW - 20, 12);
        game.shapeRenderer.setColor(hpC);
        game.shapeRenderer.rect(ehpX + 11, ehpY + 11, (ehpW - 22) * Math.max(0, hpPct), 10);

        // Status icons on enemy
        if (enemyStatus == StatusEffect.POISON) {
            game.shapeRenderer.setColor(0.3f, 0.8f, 0.2f, 0.8f);
            game.shapeRenderer.rect(ehpX + ehpW - 35, ehpY + ehpH - 20, 25, 14);
        } else if (enemyStatus == StatusEffect.BURN) {
            game.shapeRenderer.setColor(0.9f, 0.4f, 0.1f, 0.8f);
            game.shapeRenderer.rect(ehpX + ehpW - 35, ehpY + ehpH - 20, 25, 14);
        } else if (enemyStatus == StatusEffect.STUN) {
            game.shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 0.8f);
            game.shapeRenderer.rect(ehpX + ehpW - 35, ehpY + ehpH - 20, 25, 14);
        }

        // Player HP box
        float phpX = sw * 0.05f, phpY = sh * 0.35f - 80, phpW = sw * 0.38f, phpH = 80;
        game.shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.9f);
        game.shapeRenderer.rect(phpX, phpY, phpW, phpH);
        drawBorder(game.shapeRenderer, phpX, phpY, phpW, phpH);

        float pHp = (float) player.getHealth() / player.getMaxHealth();
        Color pC = pHp > 0.5f ? new Color(0.2f, 0.8f, 0.3f, 1f)
                : pHp > 0.2f ? new Color(0.9f, 0.7f, 0.1f, 1f) : new Color(0.9f, 0.2f, 0.15f, 1f);
        game.shapeRenderer.setColor(0.15f, 0.1f, 0.1f, 1f);
        game.shapeRenderer.rect(phpX + 10, phpY + 42, phpW - 20, 12);
        game.shapeRenderer.setColor(pC);
        game.shapeRenderer.rect(phpX + 11, phpY + 43, (phpW - 22) * Math.max(0, pHp), 10);

        // Stamina bar
        float stmPct = player.getStamina() / player.getMaxStamina();
        game.shapeRenderer.setColor(0.08f, 0.1f, 0.2f, 1f);
        game.shapeRenderer.rect(phpX + 10, phpY + 29, phpW - 20, 8);
        game.shapeRenderer.setColor(0.2f, 0.5f, 0.95f, 1f);
        game.shapeRenderer.rect(phpX + 11, phpY + 30, (phpW - 22) * Math.max(0, stmPct), 6);

        // Player status
        if (playerStatus == StatusEffect.POISON) {
            game.shapeRenderer.setColor(0.3f, 0.8f, 0.2f, 0.8f);
            game.shapeRenderer.rect(phpX + phpW - 35, phpY + phpH - 20, 25, 14);
        } else if (playerStatus == StatusEffect.STUN) {
            game.shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 0.8f);
            game.shapeRenderer.rect(phpX + phpW - 35, phpY + phpH - 20, 25, 14);
        }

        // XP bar
        float xpPct = (float) player.getXP() / player.getXPToNextLevel();
        game.shapeRenderer.setColor(0.1f, 0.15f, 0.1f, 1f);
        game.shapeRenderer.rect(phpX + 10, phpY + 16, phpW - 20, 8);
        game.shapeRenderer.setColor(0.2f, 0.6f, 0.3f, 1f);
        game.shapeRenderer.rect(phpX + 11, phpY + 17, (phpW - 22) * xpPct, 6);

        // Message/Action box
        float msgX = sw * 0.03f, msgY = 10, msgW = sw * 0.94f, msgH = sh * 0.22f;
        game.shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.95f);
        game.shapeRenderer.rect(msgX, msgY, msgW, msgH);
        drawBorder(game.shapeRenderer, msgX, msgY, msgW, msgH);

        // Menu highlights
        if (state == BattleState.PLAYER_TURN) {
            float menuX = msgX + msgW * 0.55f;
            float menuW = msgW * 0.42f;
            game.shapeRenderer.setColor(0.1f, 0.1f, 0.14f, 0.95f);
            game.shapeRenderer.rect(menuX, msgY + 5, menuW, msgH - 10);

            float optH = (msgH - 20) / 2f;
            int row = menuSelection / 2;
            int col = menuSelection % 2;
            game.shapeRenderer.setColor(0.2f, 0.3f, 0.5f, 0.6f);
            game.shapeRenderer.rect(menuX + 5 + col * (menuW / 2f), msgY + msgH - 15 - row * optH - optH,
                    menuW / 2f - 10, optH - 4);
        }

        // Skill/Item sub-menu
        if (state == BattleState.PLAYER_SKILL || state == BattleState.PLAYER_ITEM) {
            float subX = msgX + msgW * 0.55f;
            float subW = msgW * 0.42f;
            game.shapeRenderer.setColor(0.1f, 0.1f, 0.14f, 0.95f);
            game.shapeRenderer.rect(subX, msgY + 5, subW, msgH - 10);

            String[] items = state == BattleState.PLAYER_SKILL ? currentSkills : getUsableItemNames();
            for (int i = 0; i < Math.min(items.length, 5); i++) {
                if (i == subSelection) {
                    game.shapeRenderer.setColor(0.2f, 0.3f, 0.5f, 0.6f);
                    game.shapeRenderer.rect(subX + 5, msgY + msgH - 25 - i * 22, subW - 10, 20);
                }
            }
        }

        game.shapeRenderer.end();

        // --- Text ---
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont nf = game.font;
        BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;

        // Enemy info
        nf.setColor(1f, 1f, 1f, 1f);
        nf.draw(game.batch, enemy.getType().name, ehpX + 10, ehpY + ehpH - 8);
        sf.setColor(0.7f, 0.7f, 0.65f, 1f);
        sf.draw(game.batch, "HP " + enemy.getHealth() + "/" + enemy.getMaxHealth(), ehpX + 10, ehpY + 30);

        // Status text
        if (enemyStatus == StatusEffect.POISON) {
            sf.setColor(0.3f, 1f, 0.3f, 1f);
            sf.draw(game.batch, "PSN", ehpX + ehpW - 32, ehpY + ehpH - 8);
        }
        if (enemyStatus == StatusEffect.BURN) {
            sf.setColor(1f, 0.5f, 0.2f, 1f);
            sf.draw(game.batch, "BRN", ehpX + ehpW - 32, ehpY + ehpH - 8);
        }
        if (enemyStatus == StatusEffect.STUN) {
            sf.setColor(1f, 1f, 0.3f, 1f);
            sf.draw(game.batch, "STN", ehpX + ehpW - 32, ehpY + ehpH - 8);
        }

        // Player info
        nf.setColor(1f, 1f, 1f, 1f);
        nf.draw(game.batch, "You  Lv." + player.getLevel(), phpX + 10, phpY + phpH - 8);
        sf.setColor(0.7f, 0.7f, 0.65f, 1f);
        sf.draw(game.batch, "HP " + player.getHealth() + "/" + player.getMaxHealth(), phpX + 10, phpY + phpH - 26);
        sf.draw(game.batch, "STM " + (int) player.getStamina() + "/" + (int) player.getMaxStamina(), phpX + 10,
            phpY + phpH - 40);

        if (playerStatus == StatusEffect.POISON) {
            sf.setColor(0.3f, 1f, 0.3f, 1f);
            sf.draw(game.batch, "PSN", phpX + phpW - 32, phpY + phpH - 8);
        }
        if (playerStatus == StatusEffect.STUN) {
            sf.setColor(1f, 1f, 0.3f, 1f);
            sf.draw(game.batch, "STN", phpX + phpW - 32, phpY + phpH - 8);
        }

        // Message
        nf.setColor(1f, 1f, 1f, 1f);
        if (currentMessage != null)
            nf.draw(game.batch, currentMessage, msgX + 20, msgY + msgH - 20, msgW * 0.5f, -1, true);

        // Menu text
        if (state == BattleState.PLAYER_TURN) {
            float menuX = msgX + msgW * 0.55f;
            float menuW = msgW * 0.42f;
            float optH = (msgH - 20) / 2f;
            for (int i = 0; i < mainMenu.length; i++) {
                int row = i / 2;
                int col = i % 2;
                float tx = menuX + 15 + col * (menuW / 2f);
                float ty = msgY + msgH - 15 - row * optH;
                nf.setColor(i == menuSelection ? new Color(1f, 0.9f, 0.3f, 1f) : new Color(0.75f, 0.7f, 0.6f, 1f));
                nf.draw(game.batch, (i == menuSelection ? "> " : "  ") + mainMenu[i], tx, ty);
            }
        }

        // Sub-menu text
        if (state == BattleState.PLAYER_SKILL) {
            float subX = msgX + msgW * 0.55f;
            for (int i = 0; i < currentSkills.length; i++) {
                float ty = msgY + msgH - 18 - i * 22;
                int cost = getSkillStaminaCost(currentSkills[i]);
                boolean affordable = player.getStamina() >= cost;
                Color color;
                if (!affordable)
                    color = new Color(0.9f, 0.35f, 0.3f, 1f);
                else if (i == subSelection)
                    color = new Color(1f, 0.9f, 0.3f, 1f);
                else
                    color = new Color(0.7f, 0.65f, 0.55f, 1f);
                nf.setColor(color);
                nf.draw(game.batch, (i == subSelection ? "> " : "  ") + currentSkills[i] + " [" + cost + " STM]",
                        subX + 10, ty);
            }
            sf.setColor(0.5f, 0.5f, 0.45f, 0.7f);
            sf.draw(game.batch, "ESC: Back", subX + 10, msgY + 15);
        }

        if (state == BattleState.PLAYER_ITEM) {
            float subX = msgX + msgW * 0.55f;
            List<Item> usable = getUsableItems();
            for (int i = 0; i < Math.min(usable.size(), 5); i++) {
                float ty = msgY + msgH - 18 - i * 22;
                nf.setColor(i == subSelection ? new Color(1f, 0.9f, 0.3f, 1f) : new Color(0.7f, 0.65f, 0.55f, 1f));
                nf.draw(game.batch, (i == subSelection ? "> " : "  ") + usable.get(i).getType().displayName, subX + 10,
                        ty);
            }
            sf.setColor(0.5f, 0.5f, 0.45f, 0.7f);
            sf.draw(game.batch, "ESC: Back", subX + 10, msgY + 15);
        }

        // Victory loot
        if (state == BattleState.VICTORY && lootDrops != null && !lootDrops.isEmpty()) {
            sf.setColor(0.9f, 0.8f, 0.3f, 1f);
            sf.draw(game.batch, "Loot:", msgX + 20, msgY + msgH - 45);
            for (int i = 0; i < lootDrops.size(); i++) {
                sf.setColor(0.8f, 0.75f, 0.6f, 1f);
                sf.draw(game.batch, "  " + lootDrops.get(i).getType().displayName, msgX + 20,
                        msgY + msgH - 60 - 14 * i);
            }
        }

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private String[] getUsableItemNames() {
        List<Item> usable = getUsableItems();
        String[] names = new String[usable.size()];
        for (int i = 0; i < usable.size(); i++)
            names[i] = usable.get(i).getType().displayName;
        return names;
    }

    private void drawBattleEnemy(ShapeRenderer r, float cx, float cy, boolean flash) {
        float size = 80;
        float px = cx - size / 2;
        Color base = flash ? Color.WHITE : enemy.getType().color;
        Color dark = new Color(base.r * 0.7f, base.g * 0.7f, base.b * 0.7f, 1f);

        r.setColor(0, 0, 0, 0.3f);
        r.ellipse(px + 10, cy - 8, size - 20, 16);

        r.setColor(base);
        r.rect(px + 10, cy + 5, size - 20, size - 15);
        r.rect(px + 5, cy + 15, size - 10, size - 30);

        r.setColor(dark);
        r.rect(px + 15, cy + size - 20, size - 30, 8);

        r.setColor(flash ? Color.YELLOW : new Color(1f, 0.2f, 0.1f, 1f));
        r.rect(px + 20, cy + size / 2 + 5, 12, 12);
        r.rect(px + size - 32, cy + size / 2 + 5, 12, 12);
        r.setColor(0, 0, 0, 1f);
        r.rect(px + 23, cy + size / 2 + 7, 6, 8);
        r.rect(px + size - 29, cy + size / 2 + 7, 6, 8);
    }

    private void drawBattlePlayer(ShapeRenderer r, float cx, float cy, boolean flash) {
        float size = 70;
        float px = cx - size / 2;
        Color base = flash ? Color.WHITE : new Color(0.2f, 0.6f, 0.9f, 1f);
        Color dark = new Color(base.r * 0.75f, base.g * 0.75f, base.b * 0.75f, 1f);

        r.setColor(0, 0, 0, 0.3f);
        r.ellipse(px + 8, cy - 6, size - 16, 14);

        r.setColor(base);
        r.rect(px + 10, cy + 5, size - 20, size - 10);
        r.rect(px + 5, cy + 10, size - 10, size - 25);

        r.setColor(dark);
        r.rect(px + 12, cy + size - 12, size - 24, 10);

        if (player.getInventory().getEquippedArmor() != null) {
            Color ac = player.getInventory().getEquippedArmor().getType().color;
            r.setColor(ac.r, ac.g, ac.b, 0.8f);
            r.rect(px + 15, cy, size - 30, size - 10);
        }

        if (player.getInventory().getEquippedWeapon() != null) {
            Color wc = player.getInventory().getEquippedWeapon().getType().color;
            r.setColor(wc);
            r.rect(px + size - 5, cy + 15, 8, 35);
            r.rect(px + size - 2, cy + 50, 14, 5);
        }
    }

    private void drawBorder(ShapeRenderer r, float x, float y, float w, float h) {
        r.setColor(0.35f, 0.35f, 0.4f, 0.7f);
        r.rect(x, y, w, 2);
        r.rect(x, y + h - 2, w, 2);
        r.rect(x, y, 2, h);
        r.rect(x + w - 2, y, 2, h);
    }
}
