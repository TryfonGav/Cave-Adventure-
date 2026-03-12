package com.caveadventure;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.caveadventure.engine.GameScreen;
import com.caveadventure.entity.Companion;
import com.caveadventure.item.Item;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone sandbox launcher for quickly testing arbitrary in-game states.
 *
 * Edit the values inside TestConfig, then run this class instead of Main.
 */
public class TestMain {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("CaveAdventure Sandbox");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setResizable(true);

        writeSandboxSave();
        new Lwjgl3Application(new SandboxGame(), config);
    }

    private static void writeSandboxSave() {
        List<String> lines = new ArrayList<>();

        lines.add("floor=" + TestConfig.floor);
        lines.add("health=" + TestConfig.health);
        lines.add("maxHealth=" + TestConfig.maxHealth);
        lines.add("hunger=" + TestConfig.hunger);
        lines.add("level=" + TestConfig.level);
        lines.add("xp=" + TestConfig.xp);
        lines.add("xpNext=" + TestConfig.xpNext);
        lines.add("stamina=" + TestConfig.stamina);
        lines.add("enemiesKilled=" + TestConfig.enemiesKilled);
        lines.add("finalBossDefeated=" + TestConfig.finalBossDefeated);
        lines.add("poisonRemaining=" + TestConfig.poisonRemaining);
        lines.add("torch=" + TestConfig.torchDuration);
        lines.add("inventorySize=" + TestConfig.items.length);

        for (ItemStack stack : TestConfig.items) {
            lines.add("item=" + stack.type.name() + "," + stack.quantity);
        }

        if (TestConfig.equippedWeapon != null) {
            lines.add("weapon=" + TestConfig.equippedWeapon.name());
        }
        if (TestConfig.equippedArmor != null) {
            lines.add("armor=" + TestConfig.equippedArmor.name());
        }
        if (TestConfig.companionType != null) {
            lines.add("companionType=" + TestConfig.companionType.name());
            lines.add("companionHealth=" + TestConfig.companionHealth);
        }

        try {
            Files.writeString(Path.of("cave_adventure_save.dat"), String.join("\n", lines) + "\n",
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write sandbox save", e);
        }
    }

    private static class SandboxGame extends CaveAdventure {
        @Override
        public void create() {
            super.create();

            try {
                Screen currentScreen = getScreen();
                if (currentScreen instanceof GameScreen gameScreen) {
                    Method loadSavedGame = GameScreen.class.getDeclaredMethod("loadSavedGame");
                    loadSavedGame.setAccessible(true);
                    loadSavedGame.invoke(gameScreen);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to auto-load sandbox state", e);
            }
        }
    }

    private record ItemStack(Item.ItemType type, int quantity) {
    }

    private static final class TestConfig {
        private static final int floor = 10;
        private static final int health = 180;
        private static final int maxHealth = 180;
        private static final int hunger = 100;
        private static final int level = 10;
        private static final int xp = 0;
        private static final int xpNext = 745;
        private static final float stamina = 160f;
        private static final int enemiesKilled = 0;
        private static final boolean finalBossDefeated = false;
        private static final float poisonRemaining = 0f;
        private static final float torchDuration = 120f;

        private static final Item.ItemType equippedWeapon = Item.ItemType.CRYSTAL_BLADE;
        private static final Item.ItemType equippedArmor = Item.ItemType.CRYSTAL_ARMOR;

        private static final Companion.PetType companionType = Companion.PetType.CAVE_WOLF;
        private static final int companionHealth = 40;

        private static final ItemStack[] items = new ItemStack[] {
                new ItemStack(Item.ItemType.CRYSTAL_BLADE, 1),
                new ItemStack(Item.ItemType.CRYSTAL_ARMOR, 1),
                new ItemStack(Item.ItemType.ELIXIR, 3),
                new ItemStack(Item.ItemType.LARGE_HEALTH_POTION, 5),
                new ItemStack(Item.ItemType.GOLD_COINS, 20),
                new ItemStack(Item.ItemType.GOLD_NUGGET, 10),
                new ItemStack(Item.ItemType.TORCH, 5)
        };
    }
}