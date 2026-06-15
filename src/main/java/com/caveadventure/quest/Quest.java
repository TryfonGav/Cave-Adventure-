package com.caveadventure.quest;

import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Inventory;
import com.caveadventure.item.Item;
import com.caveadventure.world.Biome;

public class Quest {
    private final QuestDefinition definition;
    private QuestState state;
    private int progress;

    public Quest(QuestDefinition definition) {
        this(definition, QuestState.AVAILABLE, 0);
    }

    public Quest(QuestDefinition definition, QuestState state, int progress) {
        this.definition = definition;
        this.state = state == null ? QuestState.AVAILABLE : state;
        this.progress = Math.max(0, progress);
        refreshState();
    }

    public void accept() {
        if (state == QuestState.AVAILABLE) {
            state = QuestState.ACTIVE;
        }
    }

    public void updateFetch(Inventory inventory) {
        if (state != QuestState.ACTIVE || definition.objectiveType() != QuestObjectiveType.FETCH) {
            return;
        }
        progress = Math.min(definition.requiredAmount(), inventory.count(definition.targetItem()));
        refreshState();
    }

    public void recordKill(Enemy.EnemyType type) {
        if (state != QuestState.ACTIVE || definition.objectiveType() != QuestObjectiveType.KILL) {
            return;
        }
        if (definition.targetEnemy() == null || definition.targetEnemy() == type) {
            progress++;
            refreshState();
        }
    }

    public void recordReach(int floor, Biome biome) {
        if (state != QuestState.ACTIVE || definition.objectiveType() != QuestObjectiveType.REACH) {
            return;
        }
        boolean floorOk = definition.targetFloor() <= 0 || floor >= definition.targetFloor();
        boolean biomeOk = definition.targetBiome() == null || definition.targetBiome() == biome;
        if (floorOk && biomeOk) {
            progress = definition.requiredAmount();
            refreshState();
        }
    }

    public boolean claim(Player player) {
        if (state != QuestState.READY_TO_CLAIM || player == null) {
            return false;
        }
        if (definition.objectiveType() == QuestObjectiveType.FETCH && definition.targetItem() != null) {
            player.getInventory().removeItems(definition.targetItem(), definition.requiredAmount());
        }
        player.addXP(definition.xpReward());
        if (definition.rewardItem() != null) {
            player.getInventory().addItem(new Item(definition.rewardItem().getType(), definition.rewardItem().getQuantity()));
        }
        state = QuestState.COMPLETED;
        return true;
    }

    private void refreshState() {
        if (state == QuestState.ACTIVE && progress >= definition.requiredAmount()) {
            progress = definition.requiredAmount();
            state = QuestState.READY_TO_CLAIM;
        }
    }

    public String trackerText() {
        if (state == QuestState.READY_TO_CLAIM) {
            return definition.title() + ": return or open Quest Log";
        }
        return definition.title() + ": " + progress + "/" + definition.requiredAmount();
    }

    public String serialize() {
        return definition.id() + "," + state.name() + "," + progress;
    }

    public QuestDefinition getDefinition() {
        return definition;
    }

    public QuestState getState() {
        return state;
    }

    public int getProgress() {
        return progress;
    }
}
