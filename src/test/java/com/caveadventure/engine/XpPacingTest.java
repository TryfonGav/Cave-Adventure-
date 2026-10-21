package com.caveadventure.engine;

import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.quest.QuestDefinition;
import com.caveadventure.quest.QuestManager;
import com.caveadventure.world.Biome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2-T2 / decision D3b: about two level-ups per floor on floors 1–9. Plays real
 * generated floors (real enemy count and type mix), kills 70 % of each floor's
 * enemies plus the floor-5 boss, and adds quest and event XP.
 */
class XpPacingTest {

    private static final int SEEDS = 10;
    private static final int LAST_FLOOR = 9;
    private static final float KILL_SHARE = 0.7f;
    /** Floor on which each quest is assumed done (quests are offered at shops, floors 2, 4, ...). */
    private static final Map<String, Integer> QUEST_FLOOR = Map.of(
            "fetch_crystal_shards", 2,
            "fetch_glowcaps", 4,
            "kill_lava_guardians", 4,
            "kill_shadows", 8);

    /** Average level-ups per floor (index 0 = floor 1) over {@link #SEEDS} runs. */
    static double[] simulateLevelUpsPerFloor() {
        List<QuestDefinition> quests = QuestManager.defaultDefinitions();
        double[] levelUps = new double[LAST_FLOOR];
        for (long seed = 0; seed < SEEDS; seed++) {
            Player player = new Player(1, 1);
            for (int floor = 1; floor <= LAST_FLOOR; floor++) {
                int before = player.getLevel();
                LevelManager levels = new LevelManager(new Random(seed * 100 + floor));
                levels.generateFloor(floor, Biome.forFloor(floor), null);

                List<Enemy> enemies = levels.getCombatManager().getEnemies();
                int regular = (int) enemies.stream().filter(e -> !e.getType().isBoss()).count();
                int toKill = Math.round(regular * KILL_SHARE);
                for (Enemy e : enemies) {
                    if (e.getType().isBoss()) {
                        player.addXP(e.getXpReward(floor));
                    } else if (toKill > 0) {
                        player.addXP(e.getXpReward(floor));
                        toKill--;
                    }
                }
                for (QuestDefinition q : quests) {
                    if (QUEST_FLOOR.getOrDefault(q.id(), -1) == floor)
                        player.addXP(q.xpReward());
                }
                if (floor % 2 == 1) // one XP event every other floor
                    player.addXP(Balance.eventXp(floor));

                levelUps[floor - 1] += player.getLevel() - before;
            }
        }
        for (int i = 0; i < levelUps.length; i++)
            levelUps[i] /= SEEDS;
        return levelUps;
    }

    @Test
    void aboutTwoLevelUpsPerFloor() {
        double[] perFloor = simulateLevelUpsPerFloor();
        double total = 0;
        StringBuilder report = new StringBuilder("Level-ups per floor:");
        for (int i = 0; i < perFloor.length; i++) {
            total += perFloor[i];
            report.append(String.format(" F%d=%.1f", i + 1, perFloor[i]));
        }
        double average = total / perFloor.length;
        report.append(String.format("  average=%.2f  level after F%d=%.1f", average, LAST_FLOOR, 1 + total));
        System.out.println(report);

        assertEquals(2.0, average, 0.5, report.toString());
        for (int i = 0; i < perFloor.length; i++)
            assertTrue(perFloor[i] >= 0.75 && perFloor[i] <= 3.5, report.toString());
    }

    @Test
    void enemyXpGrowsWithTheFloor() {
        assertEquals(80, Balance.enemyXp(Enemy.EnemyType.SKELETON.xpReward, 1));
        assertEquals(176, Balance.enemyXp(Enemy.EnemyType.SKELETON.xpReward, 9)); // 80 × 2.2
        Enemy skeleton = new Enemy(0, 0, Enemy.EnemyType.SKELETON);
        assertEquals(Balance.enemyXp(80, 5), skeleton.getXpReward(5));
    }
}
