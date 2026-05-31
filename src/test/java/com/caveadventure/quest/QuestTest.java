package com.caveadventure.quest;

import com.caveadventure.entity.Enemy;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestTest {
    @Test
    void fetchQuestTracksInventoryProgress() {
        Quest quest = new Quest(QuestManager.defaultDefinitions().stream()
                .filter(q -> q.id().equals("fetch_crystal_shards"))
                .findFirst()
                .orElseThrow());
        Inventory inventory = new Inventory();
        quest.accept();
        inventory.addItem(new Item(Item.ItemType.CRYSTAL_SHARD, 3));

        quest.updateFetch(inventory);

        assertEquals(QuestState.READY_TO_CLAIM, quest.getState());
        assertEquals(3, quest.getProgress());
    }

    @Test
    void killQuestTransitionsWhenTargetKillsReachRequirement() {
        Quest quest = new Quest(QuestManager.defaultDefinitions().stream()
                .filter(q -> q.id().equals("kill_shadows"))
                .findFirst()
                .orElseThrow());
        quest.accept();

        quest.recordKill(Enemy.EnemyType.SHADOW);
        quest.recordKill(Enemy.EnemyType.BAT);
        quest.recordKill(Enemy.EnemyType.SHADOW);
        quest.recordKill(Enemy.EnemyType.SHADOW);

        assertEquals(QuestState.READY_TO_CLAIM, quest.getState());
        assertEquals(3, quest.getProgress());
    }
}
