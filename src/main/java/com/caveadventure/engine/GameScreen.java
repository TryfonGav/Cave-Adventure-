package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.Companion;
import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.entity.Shopkeeper;
import com.caveadventure.ui.*;
import com.caveadventure.world.Biome;
import com.caveadventure.world.GameMap;

/**
 * The primary game screen managing all game states and sub-systems.
 */
public class GameScreen extends ScreenAdapter {

    public enum GameState {
        MENU, SETTINGS, PLAYING, BATTLE, SHOP, GAME_OVER, FLOOR_TRANSITION,
        EVENT, SKILL_PICK
    }

    private final CaveAdventure game;
    private Camera2D camera;
    private final InputHandler inputHandler;
    private Viewport viewport;

    // Game objects
    private Player player;
    private GameMap gameMap;
    private CombatManager combatManager;
    private LevelManager levelManager;
    private Companion companion;

    // Systems
    private final ParticleSystem particles;
    private final ScreenTransition transition;
    private final SkillTree skillTree;
    private final RandomEventManager eventManager;

    // UI
    private final HUD hud;
    private final InventoryUI inventoryUI;
    private final MainMenu mainMenu;
    private final GameOverScreen gameOverScreen;
    private final BattleScreen battleScreen;
    private final Minimap minimap;
    private final AchievementManager achievements;
    private final ShopUI shopUI;
    private final Bestiary bestiary;
    private final StatsScreen statsScreen;
    private final SettingsMenu settingsMenu;

    // State
    private GameState state;
    private int enemiesKilledTotal;
    private boolean bossKilledThisFloor;
    private float floorTransitionTimer;
    private int transitionToFloor;
    private float battleEncounterCooldown;
    private String trapMessage;
    private float trapMessageTimer;
    private float particleTimer;
    private int lastPlayerLevel;
    private Biome currentBiome;

    public GameScreen(CaveAdventure game) {
        this.game = game;

        this.inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(inputHandler);

        OrthographicCamera cam = new OrthographicCamera();
        this.viewport = new FitViewport(960, 640, cam);
        this.camera = new Camera2D(cam, 80 * GameMap.TILE_SIZE, 60 * GameMap.TILE_SIZE);

        // Systems
        this.particles = new ParticleSystem();
        this.transition = new ScreenTransition(game);
        this.skillTree = new SkillTree(game);
        this.eventManager = new RandomEventManager(game);

        // UI
        this.hud = new HUD(game);
        this.inventoryUI = new InventoryUI(game);
        this.mainMenu = new MainMenu(game);
        this.gameOverScreen = new GameOverScreen(game);
        this.battleScreen = new BattleScreen(game);
        this.minimap = new Minimap(game);
        this.achievements = new AchievementManager(game);
        this.shopUI = new ShopUI(game);
        this.bestiary = new Bestiary(game);
        this.statsScreen = new StatsScreen(game);
        this.settingsMenu = new SettingsMenu(game);
        this.levelManager = new LevelManager();

        this.state = GameState.MENU;
        this.mainMenu.setHasSave(SaveManager.hasSave());
    }

    @Override
    public void show() {
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
    }

    // --- Game Setup ---

    private void startNewGame() {
        enemiesKilledTotal = 0;
        bossKilledThisFloor = false;
        lastPlayerLevel = 1;
        companion = null;
        setupFloor(1);
    }

    private void loadSavedGame() {
        SaveManager.SaveData data = SaveManager.loadGame();
        if (data == null) {
            startNewGame();
            return;
        }

        setupFloor(data.floor);
        player.restoreProgressFromSave(data.level, data.xp, data.xpNext, data.maxHealth, data.health);
        player.modifyHunger(data.hunger - player.getHunger());
        lastPlayerLevel = player.getLevel();

        for (com.caveadventure.item.Item item : data.items) {
            player.getInventory().addItem(item);
        }
        if (data.equippedWeapon != null) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (player.getInventory().getItem(i).getType() == data.equippedWeapon) {
                    player.getInventory().equipItem(i);
                    break;
                }
            }
        }
        if (data.equippedArmor != null) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (player.getInventory().getItem(i).getType() == data.equippedArmor) {
                    player.getInventory().equipItem(i);
                    break;
                }
            }
        }
        if (data.poisoned)
            player.applyPoison(5f);
        player.addTorchDuration(data.torchDuration - player.getTorchDuration());
        player.setStamina(data.stamina);
        companion = null;
        if (data.companionType != null) {
            companion = new Companion(player.getGridX(), player.getGridY(), data.companionType);
            if (data.companionHealth >= 0)
                companion.setCurrentHealth(data.companionHealth);
        }
        enemiesKilledTotal = data.enemiesKilled;
    }

    private void setupFloor(int floor) {
        int[] spawn = levelManager.generateFloor(floor);
        this.gameMap = levelManager.getCurrentMap();
        this.combatManager = levelManager.getCombatManager();
        this.player = new Player(spawn[0], spawn[1]);
        this.currentBiome = Biome.forFloor(floor);

        OrthographicCamera cam = new OrthographicCamera();
        this.viewport = new FitViewport(960, 640, cam);
        this.camera = new Camera2D(cam, gameMap.getWidth() * GameMap.TILE_SIZE,
                gameMap.getHeight() * GameMap.TILE_SIZE);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        minimap.init(gameMap.getWidth(), gameMap.getHeight());
        shopUI.generateShopItems();
        particles.clear();
        bossKilledThisFloor = false;

        state = GameState.PLAYING;
        battleEncounterCooldown = 1.0f;
        transition.fadeIn(0.8f);
        SaveManager.saveGame(player, companion, floor, enemiesKilledTotal);
    }

    private void transitionToNextFloor() {
        int nextFloor = levelManager.getCurrentFloor() + 1;
        if (nextFloor > levelManager.getMaxFloors()) {
            achievements.tryUnlock(AchievementManager.Achievement.VICTORY);
            gameOverScreen.setup(true, player.getLevel(), levelManager.getCurrentFloor(), enemiesKilledTotal);
            state = GameState.GAME_OVER;
            SaveManager.deleteSave();
            return;
        }
        transitionToFloor = nextFloor;
        floorTransitionTimer = 0;
        state = GameState.FLOOR_TRANSITION;
        transition.fadeOut(0.5f);
    }

    // --- Main Loop ---

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);

        achievements.update(delta);
        transition.update(delta);
        particles.update(delta);

        switch (state) {
            case MENU:
                updateMenu(delta);
                drawMenu();
                break;
            case SETTINGS:
                updateSettings(delta);
                drawSettings();
                break;
            case PLAYING:
                updatePlaying(delta);
                drawPlaying();
                break;
            case BATTLE:
                updateBattle(delta);
                drawBattle();
                break;
            case SHOP:
                updateShop(delta);
                drawPlaying();
                shopUI.render();
                break;
            case EVENT:
                updateEvent(delta);
                drawPlaying();
                eventManager.render();
                break;
            case SKILL_PICK:
                updateSkillPick(delta);
                drawPlaying();
                skillTree.render();
                break;
            case FLOOR_TRANSITION:
                updateTransition(delta);
                drawTransition();
                break;
            case GAME_OVER:
                updateGameOver(delta);
                drawGameOver();
                break;
        }

        // Overlays (on top of everything)
        if (state != GameState.MENU) {
            achievements.render();
            transition.render();
        }

        inputHandler.update();
    }

    // --- Menu ---

    private void updateMenu(float delta) {
        // Difficulty cycling with D key on menu
        if (inputHandler.isKeyJustPressed(Input.Keys.D)) {
            Difficulty[] diffs = Difficulty.values();
            int idx = (Difficulty.getCurrent().ordinal() + 1) % diffs.length;
            Difficulty.setCurrent(diffs[idx]);
        }

        int result = mainMenu.update(inputHandler, delta);
        if (result == 0)
            startNewGame();
        else if (result == 1)
            loadSavedGame();
        else if (result == 2)
            state = GameState.SETTINGS;
        else if (result == 3)
            Gdx.app.exit();
    }

    private void drawMenu() {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mainMenu.render();

        // Difficulty indicator
        OrthographicCamera uiCam = new OrthographicCamera();
        uiCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCam.update();
        game.batch.setProjectionMatrix(uiCam.combined);
        game.batch.begin();
        com.badlogic.gdx.graphics.g2d.BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;
        sf.setColor(0.6f, 0.55f, 0.45f, 0.8f);
        sf.draw(game.batch, "Difficulty: " + Difficulty.getCurrent().name + "  [D to change]", 10, 25);
        game.batch.end();
    }

    // --- Settings ---
    private void updateSettings(float delta) {
        if (settingsMenu.update(inputHandler, delta)) {
            state = GameState.MENU;
        }
    }

    private void drawSettings() {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        settingsMenu.render();
    }

    // --- Playing (Exploration) ---

    private void updatePlaying(float delta) {
        // UI toggles
        if (inputHandler.isKeyJustPressed(Input.Keys.TAB) || inputHandler.isKeyJustPressed(Input.Keys.I)) {
            inventoryUI.toggle();
            return;
        }
        if (inputHandler.isKeyJustPressed(Input.Keys.B)) {
            bestiary.toggle();
            return;
        }
        if (inputHandler.isKeyJustPressed(Input.Keys.P) && !inventoryUI.isVisible()) {
            statsScreen.toggle();
            return;
        }

        if (inputHandler.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (bestiary.isVisible()) {
                bestiary.toggle();
                return;
            }
            if (statsScreen.isVisible()) {
                statsScreen.toggle();
                return;
            }
            if (inventoryUI.isVisible()) {
                inventoryUI.toggle();
                return;
            }
            SaveManager.saveGame(player, companion, levelManager.getCurrentFloor(), enemiesKilledTotal);
            state = GameState.MENU;
            mainMenu.setHasSave(true);
            return;
        }

        // UI-specific handling
        if (bestiary.isVisible()) {
            bestiary.update(inputHandler);
            return;
        }
        if (statsScreen.isVisible()) {
            return;
        }

        inventoryUI.update(delta);

        if (inventoryUI.isVisible()) {
            String action = inventoryUI.handleInput(inputHandler, player.getInventory());
            int idx = inventoryUI.getSelectedIndex();
            switch (action) {
                case "use":
                    player.getInventory().useItem(idx, player);
                    statsScreen.itemsUsed++;
                    break;
                case "equip":
                    player.getInventory().equipItem(idx);
                    break;
                case "drop":
                    player.getInventory().dropItem(idx);
                    break;
            }
        } else {
            // Player movement
            int prevX = player.getGridX(), prevY = player.getGridY();
            player.handleInput(inputHandler, gameMap, delta);

            // Track steps
            if (player.getGridX() != prevX || player.getGridY() != prevY) {
                statsScreen.stepsTaken++;
                // Footstep dust
                particles.emitDust(player.getPixelX() + 16, player.getPixelY());
            }

            // Interactions
            if (inputHandler.isKeyJustPressed(Input.Keys.F) || inputHandler.isKeyJustPressed(Input.Keys.ENTER)) {
                if (levelManager.isNearShopkeeper(player)) {
                    state = GameState.SHOP;
                    shopUI.open();
                    return;
                }
                if (combatManager.tryOpenChest(player, levelManager.getCurrentFloor())) {
                    statsScreen.chestsOpened++;
                } else if (levelManager.tryUnlockDoor(player)) {
                    statsScreen.doorsUnlocked++;
                } else if (levelManager.isOnStairs(player)) {
                    transitionToNextFloor();
                    return;
                }
            }
        }

        player.update(delta);
        combatManager.update(player, delta);

        // Companion
        if (companion != null) {
            companion.followPlayer(player.getPixelX(), player.getPixelY(), delta);
            companion.update(delta);
        }

        // Trap check
        if (!player.isMoving()) {
            int trapDmg = levelManager.checkTraps(player);
            if (trapDmg > 0) {
                trapMessage = "Trap! -" + trapDmg + " HP!";
                trapMessageTimer = 2.0f;
                statsScreen.trapsTriggered++;
                particles.emitImpact(player.getPixelX() + 16, player.getPixelY() + 16,
                        new com.badlogic.gdx.graphics.Color(0.9f, 0.3f, 0.1f, 1f));
            }
        }

        // Minimap
        minimap.updateExplored(player.getGridX(), player.getGridY());

        // Trap message timer
        if (trapMessageTimer > 0)
            trapMessageTimer -= delta;

        // Particle effects: torch + ambient
        particleTimer += delta;
        if (particleTimer > 0.1f) {
            particleTimer = 0;
            particles.emitTorch(player.getPixelX() + 16, player.getPixelY() + 28);
            if (currentBiome != null && Math.random() < 0.3) {
                float ax = player.getPixelX() + (float) (Math.random() - 0.5) * 200;
                float ay = player.getPixelY() + (float) (Math.random() - 0.5) * 200;
                particles.emitAmbient(ax, ay, currentBiome.ambientColor);
            }
            // Poison particles
            if (player.isPoisoned()) {
                particles.emitPoison(player.getPixelX() + 16, player.getPixelY() + 16);
            }
        }

        // Random events
        if (!player.isMoving() && !inventoryUI.isVisible()) {
            if (eventManager.tryTrigger()) {
                state = GameState.EVENT;
                return;
            }
        }

        // Level-up detection (skill tree)
        if (player.getLevel() > lastPlayerLevel) {
            lastPlayerLevel = player.getLevel();
            skillTree.showPicker();
            state = GameState.SKILL_PICK;
            particles.emitLevelUp(player.getPixelX() + 16, player.getPixelY() + 16);
            return;
        }

        // Battle encounter
        if (battleEncounterCooldown > 0) {
            battleEncounterCooldown -= delta;
        } else {
            Enemy encountered = combatManager.checkBattleEncounter(player);
            if (encountered != null && !player.isMoving()) {
                battleScreen.startBattle(player, encountered, levelManager.getCurrentFloor());
                bestiary.discover(encountered.getType());
                state = GameState.BATTLE;
                statsScreen.battlesFought++;
                transition.flash(0.3f);
                return;
            }
        }

        // Death check
        if (!player.isAlive()) {
            gameOverScreen.setup(false, player.getLevel(), levelManager.getCurrentFloor() - 1, enemiesKilledTotal);
            state = GameState.GAME_OVER;
            SaveManager.deleteSave();
            return;
        }

        // Achievements
        achievements.checkConditions(enemiesKilledTotal, levelManager.getCurrentFloor(),
                player.getLevel(), player.getHealth(), player.getInventory().getSize(), bossKilledThisFloor);

        // Shopkeeper update
        Shopkeeper shop = levelManager.getShopkeeper();
        if (shop != null)
            shop.update(delta);

        camera.follow(player.getPixelX() + GameMap.TILE_SIZE / 2f,
                player.getPixelY() + GameMap.TILE_SIZE / 2f, delta);
        camera.update();
    }

    private void drawPlaying() {
        // Biome-themed background
        float br = currentBiome != null ? currentBiome.floorColor.r * 0.2f : 0.05f;
        float bg = currentBiome != null ? currentBiome.floorColor.g * 0.2f : 0.05f;
        float bb = currentBiome != null ? currentBiome.floorColor.b * 0.2f : 0.08f;
        Gdx.gl.glClearColor(br, bg, bb, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(camera.getCamera().combined);
        game.shapeRenderer.setProjectionMatrix(camera.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Map with biome colors
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        gameMap.render(game.shapeRenderer, camera);
        game.shapeRenderer.end();

        // Entities
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        combatManager.renderEnemies(game.shapeRenderer);
        Shopkeeper shop = levelManager.getShopkeeper();
        if (shop != null)
            shop.render(game.shapeRenderer);
        if (companion != null)
            companion.render(game.shapeRenderer);
        game.shapeRenderer.end();

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        player.render(game.shapeRenderer);
        game.shapeRenderer.end();

        // Particles (world space)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        particles.render(game.shapeRenderer);
        game.shapeRenderer.end();

        renderFogOfWar();

        // Damage numbers
        game.batch.setProjectionMatrix(camera.getCamera().combined);
        game.batch.begin();
        combatManager.renderDamageNumbers(game.batch, game.font);
        game.batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // HUD
        hud.render(player, combatManager.getEnemyCount(), levelManager.getCurrentFloor(), levelManager.getMaxFloors());
        inventoryUI.render(player.getInventory(), player);
        minimap.render(gameMap, player, combatManager.getEnemies());
        bestiary.render();
        statsScreen.render(player, enemiesKilledTotal, levelManager.getCurrentFloor());
        renderLootMessage();
        renderTrapMessage();
        renderBiomeLabel();
    }

    private void renderBiomeLabel() {
        if (currentBiome == null)
            return;
        OrthographicCamera uiCam = new OrthographicCamera();
        uiCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCam.update();
        game.batch.setProjectionMatrix(uiCam.combined);
        game.batch.begin();
        com.badlogic.gdx.graphics.g2d.BitmapFont sf = game.fontSmall != null ? game.fontSmall : game.font;
        sf.setColor(currentBiome.accentColor.r, currentBiome.accentColor.g, currentBiome.accentColor.b, 0.5f);
        sf.draw(game.batch, currentBiome.name + " - Floor " + levelManager.getCurrentFloor(), 10,
                Gdx.graphics.getHeight() - 10);
        game.batch.end();
    }

    // --- Shop ---

    private void updateShop(float delta) {
        boolean purchased = shopUI.update(inputHandler, player, delta);
        if (purchased) {
            achievements.tryUnlock(AchievementManager.Achievement.SHOPAHOLIC);
            statsScreen.goldSpent++;
        }
        if (!shopUI.isVisible())
            state = GameState.PLAYING;
    }

    // --- Random Event ---

    private void updateEvent(float delta) {
        eventManager.update(inputHandler, player, delta);
        if (!eventManager.isActive()) {
            state = GameState.PLAYING;
            statsScreen.eventsCompleted++;
            if (eventManager.getLastEffect() == RandomEventManager.EventEffect.SPAWN_COMPANION
                    && companion == null) {
                companion = new Companion(player.getGridX(), player.getGridY(),
                        eventManager.getLastCompanionType());
            }
        }
    }

    // --- Skill Pick ---

    private void updateSkillPick(float delta) {
        skillTree.update(inputHandler);
        if (!skillTree.isShowingPicker()) {
            state = GameState.PLAYING;
        }
    }

    // --- Battle ---

    private void updateBattle(float delta) {
        battleScreen.update(inputHandler, delta);

        if (!battleScreen.isActive()) {
            if (battleScreen.getResult()) {
                combatManager.removeEnemy(battleScreen.getEnemy());
                enemiesKilledTotal++;
                bestiary.recordKill(battleScreen.getEnemy().getType());
                if (battleScreen.wasBossKilled()) {
                    bossKilledThisFloor = true;
                    achievements.tryUnlock(AchievementManager.Achievement.BOSS_SLAYER);
                }
            } else if (battleScreen.getEnemy().isAlive()) {
                combatManager.relocateEnemy(battleScreen.getEnemy(), player);
                statsScreen.timesFled++;
            }
            state = GameState.PLAYING;
            battleEncounterCooldown = 1.5f;
            transition.fadeIn(0.3f);
        }
    }

    private void drawBattle() {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        battleScreen.render();
    }

    // --- Floor Transition ---

    private void updateTransition(float delta) {
        floorTransitionTimer += delta;
        if (floorTransitionTimer >= 2.0f) {
            com.caveadventure.item.Inventory oldInv = player.getInventory();
            int oldHunger = player.getHunger();
            int oldLevel = player.getLevel();
            boolean oldPoisoned = player.isPoisoned();
            float oldTorch = player.getTorchDuration();

            setupFloor(transitionToFloor);

            for (int i = 0; i < oldInv.getSize(); i++) {
                player.getInventory().addItem(oldInv.getItem(i));
            }
            if (oldInv.getEquippedWeapon() != null) {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    if (player.getInventory().getItem(i).getType() == oldInv.getEquippedWeapon().getType()) {
                        player.getInventory().equipItem(i);
                        break;
                    }
                }
            }
            if (oldInv.getEquippedArmor() != null) {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    if (player.getInventory().getItem(i).getType() == oldInv.getEquippedArmor().getType()) {
                        player.getInventory().equipItem(i);
                        break;
                    }
                }
            }
            while (player.getLevel() < oldLevel)
                player.addXP(player.getXPToNextLevel());
            lastPlayerLevel = player.getLevel();
            player.modifyHunger(oldHunger - player.getHunger());
            player.addTorchDuration(oldTorch - player.getTorchDuration());
            if (oldPoisoned)
                player.applyPoison(3f);
        }
    }

    private void drawTransition() {
        Gdx.gl.glClearColor(0, 0, 0, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        OrthographicCamera uiCam = new OrthographicCamera();
        uiCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCam.update();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        game.batch.setProjectionMatrix(uiCam.combined);
        game.batch.begin();

        com.badlogic.gdx.graphics.g2d.BitmapFont titleFont = game.fontLarge != null ? game.fontLarge : game.font;
        float alpha = Math.min(1f, floorTransitionTimer / 0.5f);
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();

        Biome nextBiome = Biome.forFloor(transitionToFloor);

        titleFont.setColor(nextBiome.accentColor.r, nextBiome.accentColor.g, nextBiome.accentColor.b, alpha);
        GlyphLayout layout = new GlyphLayout(titleFont, "Floor " + transitionToFloor);
        titleFont.draw(game.batch, "Floor " + transitionToFloor, sw / 2 - layout.width / 2, sh / 2 + 20);

        game.font.setColor(0.5f, 0.45f, 0.35f, alpha * 0.7f);
        layout.setText(game.font, nextBiome.name);
        game.font.draw(game.batch, nextBiome.name, sw / 2 - layout.width / 2, sh / 2 - 25);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // --- Game Over ---

    private void updateGameOver(float delta) {
        if (gameOverScreen.update(inputHandler, delta)) {
            state = GameState.MENU;
            mainMenu.setHasSave(SaveManager.hasSave());
        }
    }

    private void drawGameOver() {
        if (player != null && gameMap != null)
            drawPlaying();
        gameOverScreen.render();
    }

    // --- Rendering Helpers ---

    private void renderFogOfWar() {
        float lightRadius = player.getLightRadius() * GameMap.TILE_SIZE;
        float cx = player.getPixelX() + GameMap.TILE_SIZE / 2f;
        float cy = player.getPixelY() + GameMap.TILE_SIZE / 2f;

        game.shapeRenderer.setProjectionMatrix(camera.getCamera().combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int[] range = camera.getVisibleTileRange(GameMap.TILE_SIZE, gameMap.getWidth(), gameMap.getHeight());
        for (int x = range[0]; x <= range[2]; x++) {
            for (int y = range[1]; y <= range[3]; y++) {
                float tx = x * GameMap.TILE_SIZE + GameMap.TILE_SIZE / 2f;
                float ty = y * GameMap.TILE_SIZE + GameMap.TILE_SIZE / 2f;
                float dist = (float) Math.sqrt((tx - cx) * (tx - cx) + (ty - cy) * (ty - cy));

                if (dist > lightRadius) {
                    game.shapeRenderer.setColor(0, 0, 0, 0.85f);
                    game.shapeRenderer.rect(x * GameMap.TILE_SIZE, y * GameMap.TILE_SIZE,
                            GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                } else if (dist > lightRadius * 0.7f) {
                    float t = (dist - lightRadius * 0.7f) / (lightRadius * 0.3f);
                    game.shapeRenderer.setColor(0, 0, 0, t * 0.7f);
                    game.shapeRenderer.rect(x * GameMap.TILE_SIZE, y * GameMap.TILE_SIZE,
                            GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                }
            }
        }
        game.shapeRenderer.end();
    }

    private void renderLootMessage() {
        String msg = player.getInventory().getLastMessage();
        if (msg == null)
            return;
        float alpha = Math.min(1f, player.getInventory().getMessageTimer());
        OrthographicCamera uiCam = new OrthographicCamera();
        uiCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCam.update();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        game.batch.setProjectionMatrix(uiCam.combined);
        game.batch.begin();
        game.font.setColor(1f, 0.9f, 0.3f, alpha);
        GlyphLayout layout = new GlyphLayout(game.font, msg);
        game.font.draw(game.batch, msg, Gdx.graphics.getWidth() / 2f - layout.width / 2, 80);
        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderTrapMessage() {
        if (trapMessage == null || trapMessageTimer <= 0)
            return;
        float alpha = Math.min(1f, trapMessageTimer);
        OrthographicCamera uiCam = new OrthographicCamera();
        uiCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCam.update();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        game.batch.setProjectionMatrix(uiCam.combined);
        game.batch.begin();
        game.font.setColor(1f, 0.3f, 0.2f, alpha);
        GlyphLayout layout = new GlyphLayout(game.font, trapMessage);
        game.font.draw(game.batch, trapMessage, Gdx.graphics.getWidth() / 2f - layout.width / 2, 110);
        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        if (player != null) {
            camera.getCamera().position.set(
                    player.getPixelX() + GameMap.TILE_SIZE / 2f,
                    player.getPixelY() + GameMap.TILE_SIZE / 2f, 0);
        }
    }

    @Override
    public void dispose() {
        hud.dispose();
    }
}
