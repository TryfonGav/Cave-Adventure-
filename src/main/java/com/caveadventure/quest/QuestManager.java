package com.caveadventure.quest;

import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import com.caveadventure.world.Biome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class QuestManager {
    private static final Logger LOGGER = Logger.getLogger(QuestManager.class.getName());

    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private String availableQuestId;
    private String lastMessage;

    public QuestManager() {
        reset();
    }

    public void reset() {
        quests.clear();
        for (QuestDefinition definition : defaultDefinitions()) {
            quests.put(definition.id(), new Quest(definition));
        }
        availableQuestId = null;
        lastMessage = null;
    }

    public void offerQuestForBiome(Biome biome, int floor) {
        if (biome == null) {
            return;
        }
        String id = switch (biome) {
            case CRYSTAL_CAVES -> "fetch_crystal_shards";
            case MUSHROOM_GROTTO -> "fetch_glowcaps";
            case LAVA_CAVERNS -> "kill_lava_guardians";
            case SHADOW_ABYSS -> "kill_shadows";
            case FROST_VAULTS -> "reach_frost_vaults";
            case TOXIC_MIRE -> "fetch_toxin_sacs";
        };
        Quest quest = quests.get(id);
        if (quest != null && quest.getState() == QuestState.AVAILABLE) {
            availableQuestId = id;
            lastMessage = "Quest available: " + quest.getDefinition().title();
        }
    }

    public boolean acceptAvailableQuest() {
        Quest quest = quests.get(availableQuestId);
        if (quest == null || quest.getState() != QuestState.AVAILABLE) {
            lastMessage = "No new quest right now.";
            return false;
        }
        quest.accept();
        lastMessage = "Accepted: " + quest.getDefinition().title();
        availableQuestId = null;
        return true;
    }

    public int claimReady(Player player) {
        int claimed = 0;
        for (Quest quest : quests.values()) {
            if (quest.claim(player)) {
                claimed++;
                lastMessage = "Completed: " + quest.getDefinition().title();
            }
        }
        return claimed;
    }

    public void updateFetch(Player player) {
        if (player == null) {
            return;
        }
        for (Quest quest : quests.values()) {
            quest.updateFetch(player.getInventory());
        }
    }

    public void recordKill(Enemy.EnemyType enemyType) {
        for (Quest quest : quests.values()) {
            quest.recordKill(enemyType);
        }
    }

    public void recordReach(int floor, Biome biome) {
        for (Quest quest : quests.values()) {
            quest.recordReach(floor, biome);
        }
    }

    public void restore(Collection<String> serialized) {
        if (serialized == null) {
            return;
        }
        for (String raw : serialized) {
            String[] parts = raw.split(",", 3);
            if (parts.length != 3) {
                continue;
            }
            QuestDefinition definition = quests.containsKey(parts[0]) ? quests.get(parts[0]).getDefinition() : null;
            if (definition == null) {
                continue;
            }
            try {
                quests.put(parts[0], new Quest(definition, QuestState.valueOf(parts[1]), Integer.parseInt(parts[2])));
            } catch (IllegalArgumentException ex) {
                LOGGER.warning("Ignoring invalid quest save entry: " + raw);
            }
        }
    }

    public List<String> serialize() {
        List<String> out = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.getState() != QuestState.AVAILABLE) {
                out.add(quest.serialize());
            }
        }
        return out;
    }

    public List<Quest> getVisibleQuests() {
        List<Quest> out = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.getState() != QuestState.AVAILABLE) {
                out.add(quest);
            }
        }
        return out;
    }

    public String getTrackerText() {
        for (Quest quest : quests.values()) {
            if (quest.getState() == QuestState.READY_TO_CLAIM) {
                return quest.getDefinition().title() + ": ready to claim";
            }
            if (quest.getState() == QuestState.ACTIVE) {
                return quest.trackerText();
            }
        }
        Quest available = quests.get(availableQuestId);
        return available != null ? "Talk to NPC: " + available.getDefinition().title() : "No active quest";
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public static List<QuestDefinition> defaultDefinitions() {
        List<QuestDefinition> definitions = new ArrayList<>();
        definitions.add(new QuestDefinition("fetch_crystal_shards", "A Smith's Spark",
                "Bring crystal shards to the cave smith.", QuestObjectiveType.FETCH,
                Item.ItemType.CRYSTAL_SHARD, null, null, 0, 3, 70,
                new Item(Item.ItemType.HEALTH_POTION, 2)));
        definitions.add(new QuestDefinition("fetch_glowcaps", "Grotto Medicine",
                "Gather glowcap spores for a healer.", QuestObjectiveType.FETCH,
                Item.ItemType.GLOWCAP_SPORE, null, null, 0, 2, 80,
                new Item(Item.ItemType.ANTIDOTE, 2)));
        definitions.add(new QuestDefinition("kill_lava_guardians", "Cool the Forge",
                "Defeat lava-cavern guardians.", QuestObjectiveType.KILL,
                null, Enemy.EnemyType.SKELETON, null, 0, 3, 110,
                new Item(Item.ItemType.EMBER_CORE)));
        definitions.add(new QuestDefinition("kill_shadows", "Lanterns in the Dark",
                "Banish shadows from the abyss.", QuestObjectiveType.KILL,
                null, Enemy.EnemyType.SHADOW, null, 0, 3, 130,
                new Item(Item.ItemType.SHADOW_ESSENCE, 2)));
        definitions.add(new QuestDefinition("reach_frost_vaults", "Scout the Vaults",
                "Reach the frost vaults and report back.", QuestObjectiveType.REACH,
                null, null, Biome.FROST_VAULTS, 11, 1, 160,
                new Item(Item.ItemType.FROST_ORE, 2)));
        definitions.add(new QuestDefinition("fetch_toxin_sacs", "A Bitter Cure",
                "Gather toxin sacs from the mire.", QuestObjectiveType.FETCH,
                Item.ItemType.TOXIN_SAC, null, null, 0, 2, 170,
                new Item(Item.ItemType.LARGE_HEALTH_POTION)));
        return definitions;
    }
}
