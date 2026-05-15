package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.CharacterAppearance;
import com.caveadventure.entity.Companion;
import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import com.caveadventure.item.LootTable;
import com.caveadventure.ui.CaveUIStyle;

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

    // Cached list of usable items to avoid allocations each frame
    private final List<Item> cachedUsables = new ArrayList<>();
    private boolean usablesDirty = true;

    // Reusable Color instances to avoid transient allocations in render()
    private final Color BAR_HP_GREEN = new Color(0.2f, 0.8f, 0.3f, 1f);
    private final Color BAR_HP_YELLOW = new Color(0.9f, 0.7f, 0.1f, 1f);
    private final Color BAR_HP_RED = new Color(0.9f, 0.2f, 0.15f, 1f);
    private final Color BAR_BG = new Color(0.15f, 0.06f, 0.05f, 1f);
    private final Color STAMINA_FG = new Color(0.35f, 0.60f, 0.95f, 1f);
    private final Color STAMINA_BG = new Color(0.06f, 0.08f, 0.14f, 1f);
    private final Color XP_FG = new Color(0.25f, 0.62f, 0.30f, 1f);
    private final Color XP_BG = new Color(0.05f, 0.12f, 0.06f, 1f);
    private final Color MENU_SELECTED = new Color(1f, 0.9f, 0.3f, 1f);
    private final Color MENU_DEFAULT = new Color(0.75f, 0.7f, 0.6f, 1f);
    private final Color SKILL_UNAFFORDABLE = new Color(0.9f, 0.35f, 0.3f, 1f);
    private final Color SKILL_SELECTED = new Color(1f, 0.9f, 0.3f, 1f);
    private final Color SKILL_DEFAULT = new Color(0.7f, 0.65f, 0.55f, 1f);
    private final Color ITEM_SELECTED = new Color(1f, 0.9f, 0.3f, 1f);
    private final Color ITEM_DEFAULT = new Color(0.7f, 0.65f, 0.55f, 1f);
    private final Color LOOT_TITLE = new Color(0.9f, 0.8f, 0.3f, 1f);
    private final Color LOOT_TEXT = new Color(0.8f, 0.75f, 0.6f, 1f);
    private final Color DEFAULT_WEAPON_COLOR = new Color(0.76f, 0.80f, 0.86f, 1f);
    private final Color ATTACK_FLASH_COLOR = new Color(1f, 0.20f, 0.10f, 1f);

    // A small pool of temporary Color objects used for scaled variants
    private final Color tmpColor1 = new Color();
    private final Color tmpColor2 = new Color();

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

    // Companion
    private Companion companion;
    // Rune turn counters
    private int fireRuneTurns   = 0;
    private int shadowRuneTurns = 0;

    public BattleScreen(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.battleActive = false;
    }

    public void startBattle(Player player, Enemy enemy, int floor) {
        startBattle(player, enemy, floor, null, null);
    }

    public void startBattle(Player player, Enemy enemy, int floor, SkillTree skillTree, Companion companion) {
        this.player = player;
        this.enemy = enemy;
        this.currentFloor = floor;
        this.skillTree = skillTree;
        this.companion = companion;
        player.setCompanionAbilityUsed(false);
        this.fireRuneTurns = 0;
        this.shadowRuneTurns = 0;
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
        enemy.scaleToPlayer(player.getLevel(), floor);
        updateSkills();
        this.usablesDirty = true;
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
            } else if (wt == Item.ItemType.GREEN_BLADE) {
                currentSkills = new String[] { "Venom Slice", "Poison Jab", "Focus" };
            } else if (wt == Item.ItemType.SHADOW_BLADE) {
                currentSkills = new String[] { "Umbral Cut", "Soul Rend", "Focus" };
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
        if (canCompanionAssist() && companion.hasLoveStatBonus())
            damage = Math.max(1, Math.round(damage / companion.getBattleStatMultiplier()));
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
                    if (canCompanionAssist() && companion.getPetType() == Companion.PetType.CAVE_WOLF && !player.isCompanionAbilityUsed()) {
                        int compDmg = companion.rollDamage();
                        enemy.takeDamage(compDmg);
                        player.setCompanionAbilityUsed(true);
                        showMessage("Cave Wolf attacks for " + compDmg + " dmg!", BattleState.PLAYER_TURN);
                    } else if (companion != null && companion.getPetType() == Companion.PetType.CAVE_WOLF
                            && !companion.canAssistInBattle()) {
                        player.setCompanionAbilityUsed(true);
                        showMessage("Cave Wolf refuses to fight.", BattleState.PLAYER_TURN);
                    } else {
                        state = BattleState.PLAYER_TURN;
                        stateTimer = 0;
                        currentMessage = "What will you do?";
                    }
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
                        this.usablesDirty = true;
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
                        if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.REGEN) && player.isAlive())
                            player.heal(5); // Buffed: 5 HP per turn (was 2)
                        
                        if (canCompanionAssist() && companion.getPetType() == Companion.PetType.FIRE_SPRITE && turnCount % 3 == 0 && player.isAlive()) {
                            int heal = companion.scaleAssistValue(15);
                            player.heal(heal);
                            currentMessage += " [Sprite Heal: +" + heal + " HP]";
                        }

                        if (fireRuneTurns > 0) fireRuneTurns--;
                        if (shadowRuneTurns > 0) shadowRuneTurns--;

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
                    // Persistent poison: carry battle poison into exploration on Hard Mode
                    if (Difficulty.getCurrent().poisonPersists && playerStatus == StatusEffect.POISON) {
                        player.applyPoison(6f);
                    }
                    if (player.isManaCrystalActive()) {
                        player.consumeManaCrystal();
                    }
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
                    if (item.getType() == Item.ItemType.SMOKE_BOMB) {
                    player.getInventory().useKey(Item.ItemType.SMOKE_BOMB);
                    usablesDirty = true;
                    currentMessage = "Used Smoke Bomb! Escaped successfully!";
                    state = BattleState.RUN_AWAY;
                    stateTimer = 0;
                } else if (item.getType() == Item.ItemType.POISON_VIAL) {
                    player.getInventory().useKey(Item.ItemType.POISON_VIAL);
                    usablesDirty = true;
                    enemyStatus = StatusEffect.POISON;
                    enemyStatusTimer = 4f;
                    currentMessage = "Threw Poison Vial! Enemy poisoned!";
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                } else if (item.getType() == Item.ItemType.ICE_SHARD) {
                    player.getInventory().useKey(Item.ItemType.ICE_SHARD);
                    usablesDirty = true;
                    enemy.takeDamage(20);
                    enemyStatus = StatusEffect.STUN;
                    enemyStatusTimer = 1f;
                    currentMessage = "Threw Ice Shard! 20 dmg + Stunned!";
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                } else if (item.getType() == Item.ItemType.FIRE_RUNE) {
                    player.getInventory().useKey(Item.ItemType.FIRE_RUNE);
                    usablesDirty = true;
                    fireRuneTurns = 3;
                    currentMessage = "Used Fire Rune! Weapon inflamed for 3 turns!";
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                } else if (item.getType() == Item.ItemType.SHADOW_RUNE) {
                    player.getInventory().useKey(Item.ItemType.SHADOW_RUNE);
                    usablesDirty = true;
                    shadowRuneTurns = 3;
                    currentMessage = "Used Shadow Rune! Critical chance up for 3 turns!";
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                } else if (item.getType() == Item.ItemType.FROST_RUNE) {
                    player.getInventory().useKey(Item.ItemType.FROST_RUNE);
                    usablesDirty = true;
                    enemyStatus = StatusEffect.STUN;
                    enemyStatusTimer = 1f;
                    currentMessage = "Used Frost Rune! Enemy slowed (stunned)!";
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                } else {
                    player.getInventory().useItem(idx, player);
                    usablesDirty = true;
                    currentMessage = "Used " + item.getType().displayName + "!";
                    state = BattleState.ENEMY_TURN;
                    stateTimer = 0;
                }
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
        if (fireRuneTurns > 0) baseDmg += 10;
        
        float critChance = 0.12f;
        if (shadowRuneTurns > 0) critChance += 0.20f;
        
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
        if (fireRuneTurns > 0) baseDmg += 10;
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

            case "Venom Slice":
                dmg = (int) (baseDmg * 1.2f) + random.nextInt(7);
                enemy.takeDamage(dmg);
                if (random.nextFloat() < 0.45f) {
                    enemyStatus = StatusEffect.POISON;
                    enemyStatusTimer = 4f;
                    currentMessage = "Venom Slice! " + dmg + " dmg + POISONED!";
                } else {
                    currentMessage = "Venom Slice! " + dmg + " damage!";
                }
                triggerAttackAnim(true);
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

            case "Umbral Cut":
                dmg = (int) (baseDmg * 1.15f) + random.nextInt(8);
                enemy.takeDamage(dmg);
                if (random.nextFloat() < 0.45f) {
                    enemyStatus = StatusEffect.STUN;
                    enemyStatusTimer = 1f;
                    currentMessage = "Umbral Cut! " + dmg + " dmg + STUNNED!";
                } else {
                    currentMessage = "Umbral Cut! " + dmg + " damage!";
                }
                triggerAttackAnim(true);
                break;

            case "Soul Rend":
                dmg = (int) (baseDmg * 1.3f) + random.nextInt(6);
                enemy.takeDamage(dmg);
                int drain = Math.max(1, dmg / 3);
                player.heal(drain);
                triggerAttackAnim(true);
                currentMessage = "Soul Rend! " + dmg + " dmg and drained " + drain + " HP!";
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
        int cost;
        switch (skill) {
            case "Power Hit":
                cost = 20; break;
            case "Heavy Slash":
                cost = 24; break;
            case "Cleave":
                cost = 22; break;
            case "Venom Slice":
                cost = 21; break;
            case "Pierce":
                cost = 18; break;
            case "Burn Strike":
                cost = 26; break;
            case "Stun Slash":
                cost = 25; break;
            case "Guard Break":
                cost = 20; break;
            case "Poison Jab":
                cost = 18; break;
            case "Umbral Cut":
                cost = 20; break;
            case "Soul Rend":
                cost = 26; break;
            case "Focus":
                cost = 12; break;
            default:
                cost = 15; break;
        }
        
        if (player.isManaCrystalActive()) {
            cost = (int) (cost * 0.7f);
        }
        return cost;
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
        // Necromancer burn
        } else if (enemy.getType() == Enemy.EnemyType.NECROMANCER && random.nextFloat() < 0.30f) {
            if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.STATUS_RESIST)
                    && random.nextFloat() < 0.5f) {
                currentMessage += "Necromancer casts Curse! " + damage + " dmg (Resisted burn!)";
            } else {
                playerStatus = StatusEffect.BURN;
                playerStatusTimer = 2f;
                currentMessage += "Necromancer casts Curse Flame! " + damage + " dmg + BURN!";
            }
        // Ice Drake freeze
        } else if (enemy.getType() == Enemy.EnemyType.ICE_DRAKE && random.nextFloat() < 0.35f) {
            if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.STATUS_RESIST)
                    && random.nextFloat() < 0.5f) {
                currentMessage += "Ice Drake breathes frost! " + damage + " dmg (Resisted stun!)";
            } else {
                playerStatus = StatusEffect.STUN;
                playerStatusTimer = 1f;
                currentMessage += "Ice Drake uses Frost Breath! " + damage + " dmg + FROZEN (skip turn)!";
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
        if (canCompanionAssist() && companion.getPetType() == Companion.PetType.SHADOW_CAT) {
            runChance += companion.getRunChanceBonus(0.35f);
        }
        
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

    private boolean canCompanionAssist() {
        return companion != null && companion.canAssistInBattle();
    }

    private boolean hasUsableItems() {
        return !getUsableItems().isEmpty();
    }

    private List<Item> getUsableItems() {
        if (!usablesDirty) return cachedUsables;
        cachedUsables.clear();
        for (Item item : player.getInventory().getItems()) {
            if (item.isUsable()) cachedUsables.add(item);
        }
        usablesDirty = false;
        return cachedUsables;
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

        CaveUIStyle.drawCaveBackground(game.shapeRenderer, sw, sh, stateTimer);
        game.shapeRenderer.setColor(0.16f, 0.12f, 0.08f, 1f);
        game.shapeRenderer.rect(0, sh * 0.35f - 2, sw, 4);

        // Enemy platform
        float enemyPlatX = sw * 0.6f;
        float enemyPlatY = sh * 0.45f;
        game.shapeRenderer.setColor(0.12f, 0.09f, 0.065f, 1f);
        game.shapeRenderer.ellipse(enemyPlatX - 60, enemyPlatY - 15, 160, 30);

        // Player platform
        float playerPlatX = sw * 0.2f;
        float playerPlatY = sh * 0.38f;
        game.shapeRenderer.setColor(0.12f, 0.09f, 0.065f, 1f);
        game.shapeRenderer.ellipse(playerPlatX - 55, playerPlatY - 15, companion != null ? 185 : 140, 30);

        // Sprites
        float eSh = shakeTargetEnemy > 0 && shakeTimer > 0 ? (float) Math.sin(shakeTimer * 40) * 5 : 0;
        boolean eFlash = attackFlashTarget && attackFlashTimer > 0 && ((int) (attackFlashTimer * 15)) % 2 == 0;
        if (enemy.isAlive())
            drawBattleEnemy(game.shapeRenderer, enemyPlatX + eSh, enemyPlatY + 10, eFlash);

        float pSh = shakeTargetEnemy < 0 && shakeTimer > 0 ? (float) Math.sin(shakeTimer * 40) * 5 : 0;
        boolean pFlash = !attackFlashTarget && attackFlashTimer > 0 && ((int) (attackFlashTimer * 15)) % 2 == 0;
        if (companion != null && companion.isAlive())
            drawBattleCompanion(game.shapeRenderer, playerPlatX + pSh + 78, playerPlatY + 5);
        drawBattlePlayer(game.shapeRenderer, playerPlatX + pSh, playerPlatY + 5, pFlash);

        // Enemy HP box
        float ehpX = sw * 0.55f, ehpY = sh * 0.78f, ehpW = sw * 0.38f, ehpH = 60;
        CaveUIStyle.drawStonePanel(game.shapeRenderer, ehpX, ehpY, ehpW, ehpH, 0.92f);

        float hpPct = (float) enemy.getHealth() / enemy.getMaxHealth();
        Color hpC = hpPct > 0.5f ? BAR_HP_GREEN : hpPct > 0.2f ? BAR_HP_YELLOW : BAR_HP_RED;
        CaveUIStyle.drawBar(game.shapeRenderer, ehpX + 10, ehpY + 10, ehpW - 20, 12,
            Math.max(0, hpPct), hpC, BAR_BG);

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
        CaveUIStyle.drawStonePanel(game.shapeRenderer, phpX, phpY, phpW, phpH, 0.92f);

        float pHp = (float) player.getHealth() / player.getMaxHealth();
        Color pC = pHp > 0.5f ? BAR_HP_GREEN : pHp > 0.2f ? BAR_HP_YELLOW : BAR_HP_RED;
        CaveUIStyle.drawBar(game.shapeRenderer, phpX + 10, phpY + 42, phpW - 20, 12,
            Math.max(0, pHp), pC, BAR_BG);

        // Stamina bar
        float stmPct = player.getStamina() / player.getMaxStamina();
        CaveUIStyle.drawBar(game.shapeRenderer, phpX + 10, phpY + 29, phpW - 20, 8,
            Math.max(0, stmPct), STAMINA_FG, STAMINA_BG);

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
        CaveUIStyle.drawBar(game.shapeRenderer, phpX + 10, phpY + 16, phpW - 20, 8,
            xpPct, XP_FG, XP_BG);

        // Message/Action box
        float msgX = sw * 0.03f, msgY = 10, msgW = sw * 0.94f, msgH = sh * 0.22f;
        CaveUIStyle.drawStonePanel(game.shapeRenderer, msgX, msgY, msgW, msgH, 0.96f);

        // Menu highlights
        if (state == BattleState.PLAYER_TURN) {
            float menuX = msgX + msgW * 0.55f;
            float menuW = msgW * 0.42f;
            CaveUIStyle.drawInsetPanel(game.shapeRenderer, menuX, msgY + 5, menuW, msgH - 10, 0.92f);

            float optH = (msgH - 20) / 2f;
            int row = menuSelection / 2;
            int col = menuSelection % 2;
            CaveUIStyle.drawSelection(game.shapeRenderer, menuX + 5 + col * (menuW / 2f),
                    msgY + msgH - 15 - row * optH - optH, menuW / 2f - 10, optH - 4, 1f);
        }

        // Skill/Item sub-menu
        if (state == BattleState.PLAYER_SKILL || state == BattleState.PLAYER_ITEM) {
            float subX = msgX + msgW * 0.55f;
            float subW = msgW * 0.42f;
            CaveUIStyle.drawInsetPanel(game.shapeRenderer, subX, msgY + 5, subW, msgH - 10, 0.92f);

            String[] items = state == BattleState.PLAYER_SKILL ? currentSkills : getUsableItemNames();
            for (int i = 0; i < Math.min(items.length, 5); i++) {
                if (i == subSelection) {
                    CaveUIStyle.drawSelection(game.shapeRenderer, subX + 5, msgY + msgH - 25 - i * 22,
                            subW - 10, 20, 1f);
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
                nf.setColor(i == menuSelection ? MENU_SELECTED : MENU_DEFAULT);
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
                if (!affordable)
                    nf.setColor(SKILL_UNAFFORDABLE);
                else if (i == subSelection)
                    nf.setColor(SKILL_SELECTED);
                else
                    nf.setColor(SKILL_DEFAULT);
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
                nf.setColor(i == subSelection ? ITEM_SELECTED : ITEM_DEFAULT);
                nf.draw(game.batch, (i == subSelection ? "> " : "  ") + usable.get(i).getType().displayName, subX + 10,
                    ty);
            }
            sf.setColor(0.5f, 0.5f, 0.45f, 0.7f);
            sf.draw(game.batch, "ESC: Back", subX + 10, msgY + 15);
        }

        // Victory loot
        if (state == BattleState.VICTORY && lootDrops != null && !lootDrops.isEmpty()) {
            sf.setColor(LOOT_TITLE);
            sf.draw(game.batch, "Loot:", msgX + 20, msgY + msgH - 45);
            for (int i = 0; i < lootDrops.size(); i++) {
                sf.setColor(LOOT_TEXT);
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
        float size = enemy.getType() == Enemy.EnemyType.BOSS_GOLEM ? 116f
                : enemy.getType() == Enemy.EnemyType.ICE_DRAKE ? 106f : 92f;
        Color base = flash ? Color.WHITE : enemy.getType().color;
        scaledColorInto(base, 0.55f, 1f, tmpColor1);
        scaledColorInto(base, 1.25f, 1f, tmpColor2);
        Color dark = tmpColor1;
        Color light = tmpColor2;

        drawBattleStatusAura(r, cx, cy, size);
        r.setColor(0f, 0f, 0f, 0.34f);
        r.ellipse(cx - size * 0.45f, cy - 10, size * 0.90f, 22);

        switch (enemy.getType()) {
            case BAT:
                drawBattleBat(r, cx, cy, size, base, dark, flash);
                break;
            case SLIME:
                drawBattleSlime(r, cx, cy, size, base, dark, light);
                break;
            case SKELETON:
                drawBattleSkeleton(r, cx, cy, size, base, dark);
                break;
            case GOBLIN:
                drawBattleGoblin(r, cx, cy, size, base, dark);
                break;
            case CAVE_SPIDER:
                drawBattleSpider(r, cx, cy, size, base, dark);
                break;
            case NECROMANCER:
                drawBattleNecromancer(r, cx, cy, size, base, dark);
                break;
            case SHADOW:
                drawBattleShadow(r, cx, cy, size, base);
                break;
            case ICE_DRAKE:
                drawBattleIceDrake(r, cx, cy, size, base, dark, light);
                break;
            case BOSS_GOLEM:
                drawBattleGolem(r, cx, cy, size, base, dark, light);
                break;
        }
    }

    private void drawBattlePlayer(ShapeRenderer r, float cx, float cy, boolean flash) {
        float size = 82f;
        float px = cx - size * 0.44f;
        float bob = (float) Math.sin(stateTimer * 3.2f) * 1.5f;
        CharacterAppearance appearance = player.getAppearance();
        Color tunic = flash ? Color.WHITE : appearance.getTunicColor();
        Color darkTunic;
        if (flash) {
            scaledColorInto(tunic, 0.58f, 1f, tmpColor1);
            darkTunic = tmpColor1;
        } else {
            darkTunic = appearance.getTunicAltColor();
        }
        Color armor = player.getInventory().getEquippedArmor() != null
            ? player.getInventory().getEquippedArmor().getType().color : null;
        Color weapon = player.getInventory().getEquippedWeapon() != null
            ? player.getInventory().getEquippedWeapon().getType().color : DEFAULT_WEAPON_COLOR;

        r.setColor(0f, 0f, 0f, 0.36f);
        r.ellipse(cx - size * 0.35f, cy - 8, size * 0.72f, 18);

        // Boots and legs
        r.setColor(0.05f, 0.05f, 0.07f, 1f);
        r.rect(px + 18, cy + 2 + bob, 10, 22);
        r.rect(px + 36, cy + 2 - bob, 10, 22);
        r.setColor(appearance.getPantsColor());
        r.rect(px + 20, cy + 6 + bob, 7, 18);
        r.rect(px + 37, cy + 6 - bob, 7, 18);
        r.setColor(appearance.getBootColor());
        r.rect(px + 16, cy, 14, 6);
        r.rect(px + 35, cy, 14, 6);

        // Cape and torso
        Color cape = appearance.getCapeColor();
        r.setColor(cape.r, cape.g, cape.b, 0.92f);
        r.triangle(px + 13, cy + 21, px + 22, cy + 65, px + 4, cy + 18);
        r.setColor(0.04f, 0.05f, 0.08f, 1f);
        r.rect(px + 15, cy + 21, 36, 32);
        r.setColor(tunic);
        r.rect(px + 18, cy + 24, 28, 26);
        r.setColor(darkTunic);
        r.rect(px + 18, cy + 24, 28, 7);
        r.setColor(0.74f, 0.54f, 0.20f, 1f);
        r.rect(px + 19, cy + 31, 26, 3);

        if (armor != null) {
            scaledColorInto(armor, 0.72f, 0.95f, tmpColor1);
            r.setColor(tmpColor1);
            r.rect(px + 20, cy + 33, 24, 15);
            scaledColorInto(armor, 1.22f, 0.75f, tmpColor2);
            r.setColor(tmpColor2);
            r.rect(px + 23, cy + 43, 17, 3);
        }

        // Arms
        r.setColor(tunic);
        r.rect(px + 8, cy + 25, 11, 23);
        r.rect(px + 45, cy + 27, 10, 21);
        r.setColor(appearance.getSkinColor());
        r.rect(px + 9, cy + 20, 9, 7);
        r.rect(px + 49, cy + 22, 8, 7);

        // Head and hair, angled toward enemy
        r.setColor(0.05f, 0.05f, 0.07f, 1f);
        r.rect(px + 21, cy + 50, 26, 25);
        r.setColor(appearance.getSkinColor());
        r.rect(px + 23, cy + 52, 22, 20);
        r.setColor(appearance.getHairColor());
        r.rect(px + 21, cy + 68, 26, 7);
        r.rect(px + 21, cy + 59, 5, 10);
        r.setColor(1f, 1f, 1f, 1f);
        r.rect(px + 34, cy + 61, 5, 4);
        r.setColor(0.06f, 0.08f, 0.12f, 1f);
        r.rect(px + 36, cy + 61, 2, 3);

        // Weapon held toward the enemy
        r.setColor(0.12f, 0.08f, 0.04f, 1f);
        r.rect(px + 55, cy + 26, 5, 18);
        r.setColor(weapon);
        r.rect(px + 60, cy + 38, 6, 36);
        r.rect(px + 56, cy + 68, 14, 5);
        scaledColorInto(weapon, 1.25f, 0.65f, tmpColor1);
        r.setColor(tmpColor1);
        r.rect(px + 62, cy + 45, 2, 22);
    }

    private void drawBattleCompanion(ShapeRenderer r, float cx, float cy) {
        float bob = (float) Math.sin(stateTimer * 4.4f) * 2f;
        float twitch = (float) Math.sin(stateTimer * 8.0f) * 3f;
        Color base = companion.getPetType().color;
        scaledColorInto(base, 0.55f, 1f, tmpColor1);
        scaledColorInto(base, 1.25f, 1f, tmpColor2);
        Color dark = tmpColor1;
        Color light = tmpColor2;

        r.setColor(0f, 0f, 0f, 0.31f);
        r.ellipse(cx - 30, cy - 7, 62, 15);

        switch (companion.getPetType()) {
            case CAVE_WOLF:
                drawBattleCaveWolf(r, cx, cy + bob, base, dark, light, twitch);
                break;
            case FIRE_SPRITE:
                drawBattleFireSprite(r, cx, cy + bob, base, dark, light, twitch);
                break;
            case SHADOW_CAT:
                drawBattleShadowCat(r, cx, cy + bob, base, dark, light, twitch);
                break;
        }
    }

    private void drawBattleCaveWolf(ShapeRenderer r, float cx, float cy, Color base, Color dark, Color light,
            float twitch) {
        r.setColor(0.04f, 0.04f, 0.06f, 1f);
        r.triangle(cx - 16, cy + 28, cx - 44, cy + 42 + twitch, cx - 18, cy + 17);
        r.rect(cx - 27, cy + 13, 45, 26);
        r.rect(cx + 7, cy + 28, 25, 24);
        r.triangle(cx + 10, cy + 50, cx + 15, cy + 67, cx + 20, cy + 50);
        r.triangle(cx + 24, cy + 50, cx + 30, cy + 65, cx + 34, cy + 50);
        r.rect(cx - 22, cy, 11, 17);
        r.rect(cx + 3, cy, 11, 17);

        r.setColor(dark);
        r.triangle(cx - 16, cy + 27, cx - 38, cy + 38 + twitch, cx - 17, cy + 19);
        r.rect(cx - 24, cy + 16, 40, 21);
        r.rect(cx + 10, cy + 31, 19, 18);
        r.rect(cx - 19, cy + 3, 7, 15);
        r.rect(cx + 5, cy + 3, 7, 15);

        r.setColor(base);
        r.rect(cx - 19, cy + 20, 34, 17);
        r.rect(cx + 13, cy + 34, 16, 14);
        r.triangle(cx + 12, cy + 50, cx + 15, cy + 62, cx + 18, cy + 50);
        r.triangle(cx + 25, cy + 50, cx + 30, cy + 62, cx + 32, cy + 50);

        r.setColor(light);
        r.rect(cx - 13, cy + 34, 25, 5);
        r.rect(cx + 17, cy + 41, 8, 4);
        r.setColor(0.86f, 0.78f, 0.62f, 1f);
        r.rect(cx + 20, cy + 31, 14, 8);
        r.rect(cx - 8, cy + 18, 18, 5);

        r.setColor(1f, 0.86f, 0.24f, 1f);
        r.rect(cx + 21, cy + 41, 5, 5);
        r.setColor(0.02f, 0.02f, 0.02f, 1f);
        r.rect(cx + 32, cy + 34, 5, 4);
        r.setColor(0.92f, 0.90f, 0.78f, 1f);
        r.rect(cx + 27, cy + 29, 4, 7);
    }

    private void drawBattleFireSprite(ShapeRenderer r, float cx, float cy, Color base, Color dark, Color light,
            float twitch) {
        float flicker = (float) Math.sin(stateTimer * 10.5f) * 4f;

        r.setColor(1f, 0.22f, 0.03f, 0.18f);
        r.ellipse(cx - 34, cy + 4, 70, 62);
        r.setColor(1f, 0.62f, 0.12f, 0.23f);
        r.ellipse(cx - 24, cy + 10, 50, 50);

        r.setColor(0.04f, 0.04f, 0.06f, 1f);
        r.triangle(cx, cy + 72 + flicker, cx - 29, cy + 19, cx + 29, cy + 19);
        r.rect(cx - 19, cy + 10, 38, 36);
        r.triangle(cx - 18, cy + 33, cx - 36, cy + 55 + twitch, cx - 10, cy + 43);
        r.triangle(cx + 18, cy + 33, cx + 39, cy + 53 - twitch, cx + 10, cy + 43);

        r.setColor(dark);
        r.triangle(cx, cy + 66 + flicker, cx - 24, cy + 22, cx + 24, cy + 22);
        r.rect(cx - 16, cy + 13, 32, 31);
        r.setColor(base);
        r.triangle(cx, cy + 60 + flicker, cx - 18, cy + 23, cx + 18, cy + 23);
        r.rect(cx - 12, cy + 17, 24, 25);
        r.setColor(light);
        r.triangle(cx, cy + 50 + flicker * 0.45f, cx - 10, cy + 22, cx + 10, cy + 22);
        r.rect(cx - 8, cy + 18, 16, 17);

        r.setColor(1f, 0.96f, 0.48f, 1f);
        r.rect(cx - 10, cy + 34, 7, 7);
        r.rect(cx + 4, cy + 34, 7, 7);
        r.setColor(0.21f, 0.05f, 0.02f, 0.82f);
        r.rect(cx - 7, cy + 33, 2, 5);
        r.rect(cx + 7, cy + 33, 2, 5);
        r.rect(cx - 5, cy + 26, 11, 3);

        r.setColor(1f, 0.82f, 0.22f, 0.74f);
        r.rect(cx - 35 + twitch, cy + 7, 6, 6);
        r.rect(cx + 32 - twitch, cy + 18, 5, 5);
        r.rect(cx + 22, cy + 60 - twitch, 4, 4);
    }

    private void drawBattleShadowCat(ShapeRenderer r, float cx, float cy, Color base, Color dark, Color light,
            float twitch) {
        r.setColor(base.r, base.g, base.b, 0.24f);
        r.rect(cx - 26, cy + 16, 47, 18);
        r.rect(cx + 9, cy + 30, 28, 21);

        r.setColor(0.04f, 0.04f, 0.06f, 1f);
        r.rect(cx - 27, cy + 14, 45, 24);
        r.rect(cx + 8, cy + 30, 28, 25);
        r.triangle(cx + 11, cy + 53, cx + 17, cy + 70, cx + 23, cy + 53);
        r.triangle(cx + 26, cy + 53, cx + 33, cy + 68, cx + 37, cy + 53);
        r.rect(cx - 38, cy + 28 + twitch, 16, 8);
        r.rect(cx - 48, cy + 35 + twitch, 14, 9);
        r.rect(cx - 22, cy, 9, 17);
        r.rect(cx + 4, cy, 9, 17);

        r.setColor(dark);
        r.rect(cx - 23, cy + 18, 38, 17);
        r.rect(cx + 12, cy + 34, 21, 18);
        r.rect(cx - 36, cy + 30 + twitch, 13, 6);
        r.rect(cx - 45, cy + 37 + twitch, 10, 6);
        r.rect(cx - 19, cy + 3, 5, 15);
        r.rect(cx + 6, cy + 3, 5, 15);

        r.setColor(base);
        r.rect(cx - 17, cy + 23, 30, 13);
        r.rect(cx + 15, cy + 38, 17, 13);
        r.triangle(cx + 14, cy + 53, cx + 17, cy + 65, cx + 21, cy + 53);
        r.triangle(cx + 27, cy + 53, cx + 32, cy + 65, cx + 35, cy + 53);
        r.setColor(light);
        r.rect(cx - 12, cy + 35, 20, 4);
        r.rect(cx + 18, cy + 48, 10, 3);

        r.setColor(0.42f, 1f, 0.45f, 1f);
        r.rect(cx + 18, cy + 43, 6, 7);
        r.rect(cx + 29, cy + 43, 6, 7);
        r.setColor(0.01f, 0.03f, 0.01f, 1f);
        r.rect(cx + 20, cy + 45, 2, 5);
        r.rect(cx + 31, cy + 45, 2, 5);
        r.setColor(0.62f, 1f, 0.66f, 0.45f);
        r.rect(cx + 13, cy + 45, 27, 2);
        r.rect(cx + 15, cy + 39, 24, 2);
    }

    private void drawBattleStatusAura(ShapeRenderer r, float cx, float cy, float size) {
        if (enemyStatus == StatusEffect.NONE)
            return;

        if (enemyStatus == StatusEffect.POISON)
            r.setColor(0.25f, 0.95f, 0.20f, 0.16f);
        else if (enemyStatus == StatusEffect.BURN)
            r.setColor(1f, 0.35f, 0.05f, 0.18f);
        else
            r.setColor(1f, 0.95f, 0.20f, 0.14f);
        r.ellipse(cx - size * 0.50f, cy - 5, size, size * 0.70f);
    }

    private void drawBattleBat(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark, boolean flash) {
        float flap = (float) Math.sin(stateTimer * 12f) * 8f;
        float px = cx - size * 0.5f;
        r.setColor(0.03f, 0.02f, 0.05f, 1f);
        r.triangle(cx - 4, cy + 42, px - 30, cy + 76 + flap, px - 8, cy + 18 - flap);
        r.triangle(cx + 4, cy + 42, px + size + 30, cy + 76 - flap, px + size + 8, cy + 18 + flap);
        r.setColor(dark);
        r.triangle(cx - 4, cy + 40, px - 18, cy + 67 + flap, px + 6, cy + 22 - flap);
        r.triangle(cx + 4, cy + 40, px + size + 18, cy + 67 - flap, px + size - 6, cy + 22 + flap);
        r.setColor(base);
        r.rect(cx - 17, cy + 24, 34, 42);
        r.rect(cx - 25, cy + 37, 50, 18);
        r.setColor(dark);
        r.triangle(cx - 15, cy + 64, cx - 8, cy + 82, cx - 2, cy + 64);
        r.triangle(cx + 2, cy + 64, cx + 8, cy + 82, cx + 15, cy + 64);
        r.setColor(flash ? Color.YELLOW : ATTACK_FLASH_COLOR);
        r.rect(cx - 11, cy + 45, 7, 7);
        r.rect(cx + 5, cy + 45, 7, 7);
        r.setColor(0.96f, 0.88f, 0.76f, 1f);
        r.rect(cx - 5, cy + 31, 4, 8);
        r.rect(cx + 2, cy + 31, 4, 8);
    }

    private void drawBattleSlime(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark, Color light) {
        float squish = (float) Math.sin(stateTimer * 5f) * 4f;
        r.setColor(0.02f, 0.06f, 0.03f, 0.9f);
        r.rect(cx - 45, cy + 8 - squish, 90, 66 + squish);
        r.setColor(base);
        r.rect(cx - 40, cy + 12 - squish, 80, 58 + squish);
        r.rect(cx - 28, cy + 65, 56, 14);
        r.setColor(dark);
        r.rect(cx - 35, cy + 12 - squish, 70, 12);
        r.setColor(light.r, light.g, light.b, 0.34f);
        r.rect(cx - 24, cy + 55, 20, 10);
        r.rect(cx + 10, cy + 48, 12, 7);
        r.setColor(0.02f, 0.03f, 0.02f, 1f);
        r.rect(cx - 19, cy + 36, 10, 14);
        r.rect(cx + 14, cy + 36, 10, 14);
    }

    private void drawBattleSkeleton(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark) {
        r.setColor(0.08f, 0.07f, 0.06f, 1f);
        r.rect(cx - 25, cy + 48, 50, 36);
        r.rect(cx - 16, cy + 19, 32, 33);
        r.setColor(base);
        r.rect(cx - 22, cy + 51, 44, 30);
        r.rect(cx - 15, cy + 45, 30, 8);
        r.setColor(dark);
        r.rect(cx - 14, cy + 62, 10, 10);
        r.rect(cx + 5, cy + 62, 10, 10);
        r.rect(cx - 9, cy + 49, 18, 4);
        r.setColor(base);
        r.rect(cx - 4, cy + 21, 8, 29);
        r.rect(cx - 24, cy + 38, 48, 6);
        r.rect(cx - 18, cy + 29, 36, 5);
        r.rect(cx - 31, cy + 22, 8, 25);
        r.rect(cx + 23, cy + 22, 8, 25);
        r.rect(cx - 16, cy + 2, 10, 23);
        r.rect(cx + 6, cy + 2, 10, 23);
        r.setColor(0.72f, 0.72f, 0.68f, 1f);
        r.rect(cx + 34, cy + 20, 5, 52);
        r.rect(cx + 28, cy + 67, 18, 5);
    }

    private void drawBattleGoblin(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark) {
        r.setColor(0.04f, 0.08f, 0.03f, 1f);
        r.triangle(cx - 27, cy + 62, cx - 60, cy + 75, cx - 29, cy + 46);
        r.triangle(cx + 27, cy + 62, cx + 60, cy + 75, cx + 29, cy + 46);
        r.rect(cx - 35, cy + 18, 70, 52);
        r.setColor(base);
        r.triangle(cx - 27, cy + 61, cx - 52, cy + 71, cx - 29, cy + 50);
        r.triangle(cx + 27, cy + 61, cx + 52, cy + 71, cx + 29, cy + 50);
        r.rect(cx - 28, cy + 45, 56, 32);
        r.setColor(0.34f, 0.19f, 0.10f, 1f);
        r.rect(cx - 31, cy + 13, 62, 34);
        r.setColor(dark);
        r.rect(cx - 22, cy + 42, 44, 8);
        r.setColor(1f, 0.88f, 0.08f, 1f);
        r.rect(cx - 17, cy + 61, 10, 6);
        r.rect(cx + 8, cy + 61, 10, 6);
        r.setColor(0.08f, 0.04f, 0f, 1f);
        r.rect(cx - 14, cy + 61, 4, 4);
        r.rect(cx + 11, cy + 61, 4, 4);
        r.setColor(0.52f, 0.52f, 0.50f, 1f);
        r.rect(cx + 39, cy + 20, 8, 48);
        r.rect(cx + 32, cy + 64, 23, 8);
    }

    private void drawBattleSpider(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark) {
        float legMove = (float) Math.sin(stateTimer * 9f) * 5f;
        r.setColor(0.04f, 0.02f, 0.01f, 1f);
        for (int i = 0; i < 4; i++) {
            float y = cy + 18 + i * 13f;
            float offset = (i % 2 == 0) ? legMove : -legMove;
            r.rect(cx - 66, y + offset, 38, 5);
            r.rect(cx + 28, y - offset, 38, 5);
        }
        r.setColor(dark);
        r.rect(cx - 40, cy + 16, 80, 56);
        r.setColor(base);
        r.rect(cx - 33, cy + 22, 66, 46);
        r.rect(cx - 22, cy + 10, 44, 28);
        scaledColorInto(base, 1.2f, 1f, tmpColor1);
        r.setColor(tmpColor1);
        r.rect(cx - 19, cy + 56, 38, 7);
        r.setColor(1f, 0.04f, 0.02f, 1f);
        r.rect(cx - 23, cy + 55, 6, 6);
        r.rect(cx - 9, cy + 60, 6, 6);
        r.rect(cx + 4, cy + 60, 6, 6);
        r.rect(cx + 18, cy + 55, 6, 6);
        r.rect(cx - 13, cy + 49, 6, 5);
        r.rect(cx + 8, cy + 49, 6, 5);
    }

    private void drawBattleNecromancer(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark) {
        float sway = (float) Math.sin(stateTimer * 3.5f) * 3f;
        r.setColor(0.04f, 0.01f, 0.06f, 1f);
        r.rect(cx - 30, cy + 5, 60, 78);
        r.setColor(base);
        r.rect(cx - 24, cy + 10, 48, 65);
        r.triangle(cx - 24, cy + 10, cx, cy + 45, cx + 24, cy + 10);
        r.setColor(dark);
        r.rect(cx - 25, cy + 61, 50, 20);
        r.triangle(cx - 25, cy + 61, cx, cy + 92, cx + 25, cy + 61);
        r.setColor(0.09f, 0.02f, 0.12f, 1f);
        r.rect(cx - 16, cy + 52, 32, 18);
        r.setColor(0.82f, 0.24f, 1f, 1f);
        r.rect(cx - 12, cy + 59, 8, 5);
        r.rect(cx + 5, cy + 59, 8, 5);
        r.setColor(0.55f, 0.33f, 0.16f, 1f);
        r.rect(cx + 42 + sway, cy + 7, 6, 82);
        r.setColor(0.75f, 0.18f, 1f, 0.9f);
        r.rect(cx + 35 + sway, cy + 84, 20, 13);
        r.setColor(0.95f, 0.60f, 1f, 0.35f);
        r.ellipse(cx + 31 + sway, cy + 80, 28, 22);
    }

    private void drawBattleShadow(ShapeRenderer r, float cx, float cy, float size, Color base) {
        float wave = (float) Math.sin(stateTimer * 6f) * 5f;
        r.setColor(base.r, base.g, base.b, 0.25f);
        r.ellipse(cx - 50, cy + 8 + wave, 100, 84);
        r.setColor(base.r, base.g, base.b, 0.58f);
        r.triangle(cx - 30, cy + 12 + wave, cx, cy + 90 + wave, cx + 30, cy + 12 + wave);
        r.rect(cx - 25, cy + 30 + wave, 50, 44);
        r.setColor(0.02f, 0.01f, 0.05f, 0.84f);
        r.rect(cx - 15, cy + 37 + wave, 30, 30);
        r.setColor(0.88f, 0.92f, 1f, 0.95f);
        r.rect(cx - 17, cy + 58 + wave, 10, 6);
        r.rect(cx + 8, cy + 58 + wave, 10, 6);
        r.setColor(base.r, base.g, base.b, 0.36f);
        r.rect(cx - 50, cy + 36 + wave * 1.4f, 13, 28);
        r.rect(cx + 37, cy + 30 - wave, 13, 34);
        r.rect(cx - 5, cy + 2 - wave, 10, 18);
    }

    private void drawBattleIceDrake(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark, Color light) {
        r.setColor(0.04f, 0.08f, 0.12f, 1f);
        r.triangle(cx - 58, cy + 34, cx - 96, cy + 48, cx - 57, cy + 60);
        r.rect(cx - 48, cy + 16, 88, 52);
        r.rect(cx + 30, cy + 45, 36, 26);
        r.setColor(base);
        r.triangle(cx - 55, cy + 35, cx - 88, cy + 49, cx - 55, cy + 58);
        r.rect(cx - 43, cy + 20, 78, 45);
        r.rect(cx + 28, cy + 48, 31, 20);
        r.setColor(dark);
        r.rect(cx - 38, cy + 18, 70, 10);
        r.rect(cx + 43, cy + 38, 16, 14);
        r.setColor(light);
        r.triangle(cx + 31, cy + 67, cx + 38, cy + 91, cx + 45, cy + 67);
        r.triangle(cx + 47, cy + 68, cx + 55, cy + 90, cx + 60, cy + 66);
        r.triangle(cx - 17, cy + 64, cx - 8, cy + 84, cx, cy + 64);
        r.rect(cx - 27, cy + 33, 54, 5);
        r.setColor(0.22f, 0.45f, 1f, 1f);
        r.rect(cx + 38, cy + 56, 8, 7);
        r.rect(cx + 55, cy + 51, 6, 6);
        r.setColor(0.85f, 0.98f, 1f, 0.35f);
        r.rect(cx - 30, cy + 47, 46, 8);
    }

    private void drawBattleGolem(ShapeRenderer r, float cx, float cy, float size, Color base, Color dark, Color light) {
        r.setColor(0.07f, 0.06f, 0.05f, 1f);
        r.rect(cx - 46, cy + 8, 92, 85);
        r.rect(cx - 63, cy + 26, 22, 47);
        r.rect(cx + 41, cy + 26, 22, 47);
        r.setColor(base);
        r.rect(cx - 39, cy + 13, 78, 69);
        r.rect(cx - 27, cy + 74, 54, 25);
        r.rect(cx - 58, cy + 30, 17, 39);
        r.rect(cx + 41, cy + 30, 17, 39);
        r.setColor(dark);
        r.rect(cx - 31, cy + 73, 5, 24);
        r.rect(cx + 24, cy + 77, 5, 17);
        r.rect(cx - 20, cy + 28, 41, 8);
        r.rect(cx - 36, cy + 57, 14, 4);
        r.rect(cx + 17, cy + 53, 19, 4);
        r.setColor(light.r, light.g, light.b, 0.62f);
        r.rect(cx - 30, cy + 82, 48, 5);
        r.rect(cx - 32, cy + 50, 16, 4);
        float glow = (float) Math.sin(stateTimer * 4f) * 0.2f + 0.8f;
        r.setColor(glow, glow * 0.32f, 0.05f, 1f);
        r.rect(cx - 21, cy + 65, 13, 10);
        r.rect(cx + 9, cy + 65, 13, 10);
        r.setColor(0.76f, 0.33f, 1f, 0.95f);
        r.rect(cx - 12, cy + 39, 24, 20);
        r.setColor(0.95f, 0.65f, 1f, 0.32f);
        r.rect(cx - 7, cy + 44, 14, 10);
    }

    private void scaledColorInto(Color color, float scale, float alpha, Color out) {
        out.set(Math.min(1f, color.r * scale), Math.min(1f, color.g * scale),
                Math.min(1f, color.b * scale), alpha);
    }

    private void drawBorder(ShapeRenderer r, float x, float y, float w, float h) {
        r.setColor(0.35f, 0.35f, 0.4f, 0.7f);
        r.rect(x, y, w, 2);
        r.rect(x, y + h - 2, w, 2);
        r.rect(x, y, 2, h);
        r.rect(x + w - 2, y, 2, h);
    }
}
