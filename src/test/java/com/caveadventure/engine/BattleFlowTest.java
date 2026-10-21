package com.caveadventure.engine;

import com.badlogic.gdx.Input.Keys;
import com.caveadventure.TestKeys;
import com.caveadventure.engine.BattleScreen.BattleState;
import com.caveadventure.engine.BattleScreen.StatusEffect;
import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BattleFlowTest {

    // Slightly longer than each state's delay in BattleScreen.update
    private static final float INTRO_SECONDS = 1.6f;
    private static final float PLAYER_ATTACK_SECONDS = 0.9f;
    private static final float ENEMY_TURN_SECONDS = 0.6f;
    private static final float ENEMY_ATTACK_SECONDS = 1.1f;
    private static final float MESSAGE_SECONDS = 1.6f;
    private static final float END_SECONDS = 2.1f;
    private static final float FRAME = 0.01f;

    private final InputHandler input = new InputHandler();

    private BattleScreen newBattle(Player player, Enemy enemy) {
        return newBattle(player, enemy, new Random(1), null);
    }

    private BattleScreen newBattle(Player player, Enemy enemy, Random random, SkillTree skillTree) {
        BattleScreen battle = new BattleScreen(null, random);
        battle.startBattle(player, enemy, 1, skillTree, null);
        battle.update(input, INTRO_SECONDS);
        assertEquals(BattleState.PLAYER_TURN, battle.getState());
        return battle;
    }

    private void tap(BattleScreen battle, int key) {
        TestKeys.tap(input, key, () -> battle.update(input, FRAME));
    }

    /** From PLAYER_TURN with "Attack" selected: attack, ending in PLAYER_ATTACK. */
    private void attack(BattleScreen battle) {
        tap(battle, Keys.ENTER);
        assertEquals(BattleState.PLAYER_ATTACK, battle.getState());
    }

    /** From PLAYER_ATTACK on a surviving enemy: through the enemy's turn, ending in ENEMY_ATTACK. */
    private void enemyTurn(BattleScreen battle) {
        battle.update(input, PLAYER_ATTACK_SECONDS);
        assertEquals(BattleState.ENEMY_TURN, battle.getState());
        battle.update(input, ENEMY_TURN_SECONDS);
        assertEquals(BattleState.ENEMY_ATTACK, battle.getState());
    }

    /** Index of the given item type among the usable items, as listed in the battle item menu. */
    private static int usableIndex(Player player, Item.ItemType type) {
        List<Item.ItemType> usable = player.getInventory().getItems().stream()
                .filter(Item::isUsable).map(Item::getType).collect(Collectors.toList());
        return usable.indexOf(type);
    }

    /** From PLAYER_TURN: open the item menu and use the given item. */
    private void useItem(BattleScreen battle, Player player, Item.ItemType type) {
        tap(battle, Keys.DOWN); // Attack -> Items
        tap(battle, Keys.ENTER);
        assertEquals(BattleState.PLAYER_ITEM, battle.getState());
        for (int i = usableIndex(player, type); i > 0; i--)
            tap(battle, Keys.DOWN);
        tap(battle, Keys.ENTER);
    }

    /** Random whose nextFloat() is always 0: every chance roll succeeds (crits, freezes). */
    private static Random alwaysProcs() {
        return new Random(1) {
            @Override
            public float nextFloat() {
                return 0f;
            }
        };
    }

    @Test
    void introReachesPlayerTurn() {
        BattleScreen battle = new BattleScreen(null, new Random(1));
        battle.startBattle(new Player(1, 1), new Enemy(2, 1, Enemy.EnemyType.SLIME), 1);
        assertEquals(BattleState.INTRO, battle.getState());

        battle.update(input, INTRO_SECONDS);

        assertEquals(BattleState.PLAYER_TURN, battle.getState());
    }

    // --- P1-T9: end-of-round messages ---

    @Test
    void poisonTickAtRoundEndIsShown() {
        Player player = new Player(1, 1);
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));
        attack(battle);
        battle.getPlayerStatus().apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);
        enemyTurn(battle);
        int hpBeforeTick = player.getHealth();

        battle.update(input, ENEMY_ATTACK_SECONDS);

        assertEquals(BattleState.MESSAGE, battle.getState());
        assertTrue(battle.getMessage().contains("Poison: -3 HP"), battle.getMessage());
        assertEquals(hpBeforeTick - 3, player.getHealth());
        battle.update(input, MESSAGE_SECONDS);
        assertEquals(BattleState.PLAYER_TURN, battle.getState());
    }

    @Test
    void quietRoundEndGoesStraightToPlayerTurn() {
        BattleScreen battle = newBattle(new Player(1, 1), new Enemy(2, 1, Enemy.EnemyType.SLIME));
        attack(battle);
        enemyTurn(battle);

        battle.update(input, ENEMY_ATTACK_SECONDS);

        assertEquals(BattleState.PLAYER_TURN, battle.getState());
        assertEquals("What will you do?", battle.getMessage());
    }

    // --- P1-T8: Regeneration ---

    @Test
    void regenerationHealIsShownAtRoundEnd() {
        Player player = new Player(1, 1);
        SkillTree skills = new SkillTree(null);
        skills.restoreUnlockedSkills(EnumSet.of(SkillTree.Skill.REGEN));
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME), new Random(1), skills);
        attack(battle);
        player.setHealth(50);
        enemyTurn(battle);
        int hpBeforeRegen = player.getHealth();

        battle.update(input, ENEMY_ATTACK_SECONDS);

        int expected = player.getBattleRegenAmount();
        assertEquals(BattleState.MESSAGE, battle.getState());
        assertTrue(battle.getMessage().contains("Regen: +" + expected + " HP"), battle.getMessage());
        assertEquals(hpBeforeRegen + expected, player.getHealth());
    }

    // --- P1-T2: turn-based status ---

    @Test
    void iceDrakeStunSkipsOnlyTheNextPlayerTurn() {
        Player player = new Player(1, 1);
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.ICE_DRAKE), alwaysProcs(), null);

        // Round N: the drake freezes the player
        attack(battle);
        enemyTurn(battle);
        assertTrue(battle.getMessage().contains("FROZEN"), battle.getMessage());
        assertEquals(StatusEffect.STUN, battle.getPlayerStatus().current());
        battle.update(input, ENEMY_ATTACK_SECONDS);
        assertEquals(BattleState.PLAYER_TURN, battle.getState());

        // Round N+1: the player's turn is skipped; the drake's second freeze is blocked
        battle.update(input, FRAME);
        assertEquals(BattleState.MESSAGE, battle.getState());
        assertTrue(battle.getMessage().contains("stunned"), battle.getMessage());
        battle.update(input, MESSAGE_SECONDS);
        assertEquals(BattleState.ENEMY_TURN, battle.getState());
        battle.update(input, ENEMY_TURN_SECONDS);
        assertEquals(BattleState.ENEMY_ATTACK, battle.getState());
        assertFalse(battle.getMessage().contains("FROZEN"), battle.getMessage());
        battle.update(input, ENEMY_ATTACK_SECONDS);

        // Round N+2: the player acts
        battle.update(input, FRAME);
        assertEquals(BattleState.PLAYER_TURN, battle.getState());
        attack(battle);
    }

    @Test
    void enemyStunAppliedOnPlayerTurnSkipsEnemyAttack() {
        Player player = new Player(1, 1);
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));
        attack(battle);
        battle.getEnemyStatus().apply(StatusEffect.STUN, BattleStatus.STUN_TURNS);

        enemyTurn(battle);

        assertTrue(battle.getMessage().contains("is stunned"), battle.getMessage());
        assertEquals(player.getMaxHealth(), player.getHealth());
    }

    @Test
    void antidoteCuresBattlePoison() {
        Player player = new Player(1, 1);
        player.getInventory().addItem(new Item(Item.ItemType.ANTIDOTE, 1));
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));
        battle.getPlayerStatus().apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);

        useItem(battle, player, Item.ItemType.ANTIDOTE);

        assertEquals(BattleState.ENEMY_TURN, battle.getState());
        assertEquals(StatusEffect.NONE, battle.getPlayerStatus().current());
    }

    // --- P1-T3: deaths from damage over time and items ---

    @Test
    void poisonTickKillsEnemyBeforeItAttacks() {
        Player player = new Player(1, 1);
        Enemy enemy = new Enemy(2, 1, Enemy.EnemyType.SLIME);
        BattleScreen battle = newBattle(player, enemy);
        attack(battle);
        enemy.setHealth(3);
        battle.getEnemyStatus().apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);
        battle.update(input, PLAYER_ATTACK_SECONDS);
        assertEquals(BattleState.ENEMY_TURN, battle.getState());

        battle.update(input, ENEMY_TURN_SECONDS);

        assertEquals(BattleState.MESSAGE, battle.getState());
        assertTrue(battle.getMessage().contains("defeated"), battle.getMessage());
        assertFalse(battle.getMessage().contains("attacks"), battle.getMessage());
        assertEquals(player.getMaxHealth(), player.getHealth());
        battle.update(input, MESSAGE_SECONDS);
        assertEquals(BattleState.VICTORY, battle.getState());
        battle.update(input, END_SECONDS);
        assertEquals(BattleScreen.Outcome.VICTORY, battle.getOutcome());
    }

    @Test
    void poisonTickKillsPlayerAtRoundEnd() {
        Player player = new Player(1, 1);
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));
        attack(battle);
        player.setHealth(2);
        battle.getPlayerStatus().apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);
        battle.getEnemyStatus().apply(StatusEffect.STUN, BattleStatus.STUN_TURNS); // so only the tick hurts
        enemyTurn(battle);

        battle.update(input, ENEMY_ATTACK_SECONDS);

        assertFalse(player.isAlive());
        assertEquals(BattleState.MESSAGE, battle.getState());
        assertTrue(battle.getMessage().contains("defeated"), battle.getMessage());
        battle.update(input, MESSAGE_SECONDS);
        assertEquals(BattleState.DEFEAT, battle.getState());
        battle.update(input, END_SECONDS);
        assertFalse(battle.isActive());
        assertEquals(BattleScreen.Outcome.DEFEAT, battle.getOutcome());
    }

    @Test
    void iceShardKillIsAVictory() {
        Player player = new Player(1, 1);
        player.getInventory().addItem(new Item(Item.ItemType.ICE_SHARD, 1));
        Enemy enemy = new Enemy(2, 1, Enemy.EnemyType.SLIME);
        BattleScreen battle = newBattle(player, enemy);
        enemy.setHealth(5);

        useItem(battle, player, Item.ItemType.ICE_SHARD);
        assertEquals(BattleState.PLAYER_ATTACK, battle.getState());
        battle.update(input, PLAYER_ATTACK_SECONDS);

        assertEquals(BattleState.MESSAGE, battle.getState());
        assertTrue(battle.getMessage().contains("defeated"), battle.getMessage());
    }

    // --- P1-T6: Focus ---

    @Test
    void focusReducesNextHitAndKeepsAttackText() {
        Player player = new Player(1, 1);
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));
        tap(battle, Keys.RIGHT); // Attack -> Skills
        tap(battle, Keys.ENTER);
        assertEquals(BattleState.PLAYER_SKILL, battle.getState());
        tap(battle, Keys.DOWN); // Power Hit -> Focus
        tap(battle, Keys.ENTER);
        assertEquals(BattleState.ENEMY_TURN, battle.getState());
        assertTrue(battle.getMessage().contains("1/3"), battle.getMessage());

        battle.update(input, ENEMY_TURN_SECONDS);

        assertTrue(battle.getMessage().contains("attacks for"), battle.getMessage());
        assertTrue(battle.getMessage().contains("Braced"), battle.getMessage());
        int slimeMaxHit = new Enemy(0, 0, Enemy.EnemyType.SLIME).getScaledMaxDamage();
        assertTrue(player.getMaxHealth() - player.getHealth() <= slimeMaxHit / 3 + 1);
    }

    // --- P1-T14: outcome ---

    @Test
    void smokeBombEndsAsFled() {
        Player player = new Player(1, 1);
        player.getInventory().addItem(new Item(Item.ItemType.SMOKE_BOMB, 1));
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));

        useItem(battle, player, Item.ItemType.SMOKE_BOMB);
        assertEquals(BattleState.RUN_AWAY, battle.getState());
        battle.update(input, END_SECONDS);

        assertFalse(battle.isActive());
        assertEquals(BattleScreen.Outcome.FLED, battle.getOutcome());
    }

    // --- P1-T7: event buffs ---

    /** Enemy HP lost to one basic attack, same seed each time. */
    private int damageOfOneAttack(Player player) {
        Enemy enemy = new Enemy(2, 1, Enemy.EnemyType.SKELETON);
        BattleScreen battle = newBattle(player, enemy);
        int before = enemy.getHealth();
        attack(battle);
        return before - enemy.getHealth();
    }

    /** Player HP lost to one enemy attack, same seed each time. */
    private int damageOfOneEnemyHit(Player player) {
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SKELETON));
        attack(battle);
        int before = player.getHealth();
        enemyTurn(battle);
        return before - player.getHealth();
    }

    @Test
    void attackBuffRaisesDamageDealt() {
        int plain = damageOfOneAttack(new Player(1, 1));
        Player buffed = new Player(1, 1);
        buffed.grantAttackBuff(3);

        assertTrue(damageOfOneAttack(buffed) > plain);
    }

    @Test
    void defenseBuffLowersDamageTaken() {
        int plain = damageOfOneEnemyHit(new Player(1, 1));
        Player buffed = new Player(1, 1);
        buffed.grantDefenseBuff(3);

        assertTrue(damageOfOneEnemyHit(buffed) < plain);
    }

    @Test
    void battleEndUsesUpOneBuffCharge() {
        Player player = new Player(1, 1);
        player.grantAttackBuff(3);
        player.grantDefenseBuff(3);
        player.getInventory().addItem(new Item(Item.ItemType.SMOKE_BOMB, 1));
        BattleScreen battle = newBattle(player, new Enemy(2, 1, Enemy.EnemyType.SLIME));

        useItem(battle, player, Item.ItemType.SMOKE_BOMB);
        battle.update(input, END_SECONDS);

        assertFalse(battle.isActive());
        assertEquals(2, player.getAttackBuffBattles());
        assertEquals(2, player.getDefenseBuffBattles());
    }
}
