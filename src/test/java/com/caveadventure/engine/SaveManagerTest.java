package com.caveadventure.engine;

import com.caveadventure.item.Item;
import com.caveadventure.ui.AchievementManager;
import com.caveadventure.world.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaveManagerTest {
    @Test
    void loadSaveDataRestoresExpansionFieldsWithoutMutatingDifficulty() {
        String save = String.join("\n",
                "version=2",
                "profile=profile2",
                "difficulty=HARD",
                "biome=TOXIC_MIRE",
                "floor=13",
                "skillPoints=2",
                "item=SHADOW_CHARM,1",
                "accessory=SHADOW_CHARM",
                "skillUnlocked=POWER_SURGE",
                "quest=fetch_toxin_sacs,ACTIVE,1",
                "achievement=CRAFTER",
                "stat=itemsCrafted,4");

        Difficulty before = Difficulty.getCurrent();
        SaveManager.SaveData data = SaveManager.loadSaveData(save);
        assertEquals(before, Difficulty.getCurrent());

        assertEquals(2, data.version);
        assertEquals("profile2", data.profile);
        assertEquals(Difficulty.HARD, data.difficulty);
        assertEquals(Biome.TOXIC_MIRE, data.biome);
        assertEquals(13, data.floor);
        assertEquals(2, data.skillPoints);
        assertEquals(Item.ItemType.SHADOW_CHARM, data.equippedAccessory);
        assertTrue(data.unlockedSkills.contains(SkillTree.Skill.POWER_SURGE));
        assertEquals("fetch_toxin_sacs,ACTIVE,1", data.questLines.get(0));
        assertTrue(data.unlockedAchievements.contains(AchievementManager.Achievement.CRAFTER));
        assertEquals(4, data.stats.get("itemsCrafted"));
    }
}
