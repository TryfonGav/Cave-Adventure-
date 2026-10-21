package com.caveadventure.quest;

import com.caveadventure.engine.LevelManager;
import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import com.caveadventure.world.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestFlowTest {

    private static final int LAST_FLOOR = 14;
    private static final String REACH_QUEST = "reach_frost_vaults";

    /** Walks floors 1..14 the way the game does (biome per LevelManager), entering each floor's quests. */
    private static List<String> runAllFloors(QuestManager quests, int[] reachAcceptedOnFloor) {
        LevelManager levels = new LevelManager();
        List<String> accepted = new ArrayList<>();
        for (int floor = 1; floor <= LAST_FLOOR; floor++) {
            levels.generateFloor(floor);
            Biome biome = levels.getCurrentBiome();
            boolean reachWasAvailable = quests.getQuest(REACH_QUEST).getState() == QuestState.AVAILABLE;
            accepted.addAll(quests.enterFloor(floor, biome));
            if (reachWasAvailable && quests.getQuest(REACH_QUEST).getState() != QuestState.AVAILABLE)
                reachAcceptedOnFloor[0] = floor;
        }
        return accepted;
    }

    @Test
    void everyQuestIsAcceptedOnTheWayDown() {
        QuestManager quests = new QuestManager();
        int[] reachFloor = { -1 };

        List<String> accepted = runAllFloors(quests, reachFloor);

        assertEquals(QuestManager.defaultDefinitions().size(), accepted.size(), accepted.toString());
        for (QuestDefinition definition : QuestManager.defaultDefinitions())
            assertNotEquals(QuestState.AVAILABLE, quests.getQuest(definition.id()).getState(), definition.id());
    }

    @Test
    void reachQuestIsAGoalBeforeItsTargetAndCompletesOnArrival() {
        QuestManager quests = new QuestManager();
        int[] reachFloor = { -1 };

        runAllFloors(quests, reachFloor);

        int target = quests.getQuest(REACH_QUEST).getDefinition().targetFloor();
        assertEquals(target - QuestManager.REACH_QUEST_LEAD_FLOORS, reachFloor[0]);
        assertEquals(QuestState.READY_TO_CLAIM, quests.getQuest(REACH_QUEST).getState());
    }

    @Test
    void allSixQuestsCanBeCompleted() {
        QuestManager quests = new QuestManager();
        runAllFloors(quests, new int[] { -1 });

        Player player = new Player(0, 0);
        for (QuestDefinition definition : QuestManager.defaultDefinitions()) {
            if (definition.objectiveType() == QuestObjectiveType.KILL) {
                for (int i = 0; i < definition.requiredAmount(); i++)
                    quests.recordKill(definition.targetEnemy());
            } else if (definition.objectiveType() == QuestObjectiveType.FETCH) {
                player.getInventory().addItem(new Item(definition.targetItem(), definition.requiredAmount()));
            }
        }
        quests.updateFetch(player);

        for (QuestDefinition definition : QuestManager.defaultDefinitions())
            assertEquals(QuestState.READY_TO_CLAIM, quests.getQuest(definition.id()).getState(), definition.id());
        assertEquals(QuestManager.defaultDefinitions().size(), quests.claimReady(player));
    }

    @Test
    void enteringAFloorAgainAcceptsNothingNew() {
        QuestManager quests = new QuestManager();
        assertEquals(1, quests.enterFloor(1, Biome.CRYSTAL_CAVES).size());
        assertTrue(quests.enterFloor(2, Biome.CRYSTAL_CAVES).isEmpty());
        quests.recordKill(Enemy.EnemyType.SKELETON); // not accepted yet: no progress
        assertEquals(0, quests.getQuest("kill_lava_guardians").getProgress());
    }
}
