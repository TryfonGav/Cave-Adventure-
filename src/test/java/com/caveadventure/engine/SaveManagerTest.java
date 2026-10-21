package com.caveadventure.engine;

import com.badlogic.gdx.files.FileHandle;
import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import com.caveadventure.ui.AchievementManager;
import com.caveadventure.world.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaveManagerTest {
    private static final String LEGACY_FILE = "cave_adventure_save.dat";

    @TempDir
    File saveDir;

    @AfterEach
    void restoreDefaults() {
        SaveManager.setSaveDirForTest(null);
        SaveManager.setActiveProfile("profile1");
    }

    private void useTempSaveDir(String profile) {
        SaveManager.setSaveDirForTest(new FileHandle(saveDir));
        SaveManager.setActiveProfile(profile);
    }

    private File file(String path) {
        return new File(saveDir, path);
    }

    private void write(String path, String content) throws Exception {
        File f = file(path);
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), content);
    }

    @Test
    void saveThenLoadGoesThroughTheRealLoadPath() {
        useTempSaveDir("profile2");
        Player player = new Player(5, 5);
        player.setHealth(42);
        player.grantAttackBuff(2);
        player.getInventory().addItem(new Item(Item.ItemType.BREAD, 3));

        SaveManager.saveGame(player, null, 6, 9, false, List.of());
        SaveManager.SaveData data = SaveManager.loadGame();

        assertNotNull(data);
        assertEquals(6, data.floor);
        assertEquals(42, data.health);
        assertEquals(9, data.enemiesKilled);
        assertEquals(2, data.attackBuffBattles);
        assertTrue(data.items.stream().anyMatch(i -> i.getType() == Item.ItemType.BREAD && i.getQuantity() == 3));
        assertTrue(file("profiles/profile2.dat").exists());
        assertFalse(file("profiles/profile2.dat.tmp").exists(), "temp file is moved into place");
    }

    @Test
    void saveReplacesTheExistingFile() {
        useTempSaveDir("profile1");
        Player player = new Player(5, 5);
        SaveManager.saveGame(player, null, 2, 0, false, List.of());
        SaveManager.saveGame(player, null, 3, 0, false, List.of());

        assertEquals(3, SaveManager.loadGame().floor);
        assertFalse(file("profiles/profile1.dat.tmp").exists());
    }

    @Test
    void legacySaveIsMigratedToProfile1OnFirstLoad() throws Exception {
        useTempSaveDir("profile1");
        write(LEGACY_FILE, "floor=3\nhealth=50\n");
        assertTrue(SaveManager.hasSave());

        assertEquals(3, SaveManager.loadGame().floor);
        assertFalse(file(LEGACY_FILE).exists(), "legacy file is moved, not copied");
        assertTrue(file("profiles/profile1.dat").exists());
        assertEquals(50, SaveManager.loadGame().health);
    }

    @Test
    void legacySaveIsIgnoredForOtherProfiles() throws Exception {
        useTempSaveDir("profile2");
        write(LEGACY_FILE, "floor=3\n");

        assertFalse(SaveManager.hasSave());
        assertNull(SaveManager.loadGame());
        assertTrue(file(LEGACY_FILE).exists());
    }

    @Test
    void deleteSaveAlsoRemovesTheLegacyFileForProfile1() throws Exception {
        useTempSaveDir("profile1");
        write(LEGACY_FILE, "floor=1\n");
        write("profiles/profile1.dat", "floor=4\n");

        SaveManager.deleteSave();

        assertFalse(SaveManager.hasSave(), "a dead run can't be continued");
        assertFalse(file(LEGACY_FILE).exists());
        assertFalse(file("profiles/profile1.dat").exists());
    }

    @Test
    void parserTrimsInventoryToDeclaredSize() {
        SaveManager.SaveData data = SaveManager.parseSaveText(
                "inventorySize=1\nitem=BREAD,1\nitem=HEALTH_POTION,2\nnot a key value line\nunknownKey=1");
        assertEquals(1, data.items.size());
        assertEquals(Item.ItemType.BREAD, data.items.get(0).getType());
    }

    @Test
    void parseSaveTextRestoresExpansionFields() {
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

        SaveManager.SaveData data = SaveManager.parseSaveText(save);

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

    @Test
    void eventBuffsAreRestoredAndDefaultToZero() {
        SaveManager.SaveData withBuffs = SaveManager.parseSaveText("floor=3\nattackBuff=2\ndefenseBuff=1");
        assertEquals(2, withBuffs.attackBuffBattles);
        assertEquals(1, withBuffs.defenseBuffBattles);

        SaveManager.SaveData oldSave = SaveManager.parseSaveText("version=2\nfloor=3");
        assertEquals(0, oldSave.attackBuffBattles);
        assertEquals(0, oldSave.defenseBuffBattles);
    }
}
