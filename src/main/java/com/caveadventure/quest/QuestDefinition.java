package com.caveadventure.quest;

import com.caveadventure.entity.Enemy;
import com.caveadventure.item.Item;
import com.caveadventure.world.Biome;

public record QuestDefinition(
        String id,
        String title,
        String description,
        QuestObjectiveType objectiveType,
        Item.ItemType targetItem,
        Enemy.EnemyType targetEnemy,
        Biome targetBiome,
        int targetFloor,
        int requiredAmount,
        int xpReward,
        Item rewardItem) {
}
