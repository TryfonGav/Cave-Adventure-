package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.caveadventure.entity.Enemy;
import com.caveadventure.world.Biome;

/**
 * Sound manager for loading and playing SFX/music assets under assets/audio.
 * Missing files are skipped to keep the game running during asset iteration.
 */
public class SoundManager {

    private static SoundManager instance;
    private boolean enabled;
    private float volume;
    private float musicVolume;
    private String currentMusic;
    private String currentAmbient;
    private boolean initialized;

    private final ObjectMap<String, Sound> sfx = new ObjectMap<>();
    private final ObjectMap<String, Music> music = new ObjectMap<>();
    private Music activeMusic;

    private SoundManager() {
        this.enabled = true;
        this.volume = 0.7f;
        this.musicVolume = 0.55f;
    }

    public static SoundManager getInstance() {
        if (instance == null)
            instance = new SoundManager();
        return instance;
    }

    public void init() {
        if (initialized)
            return;
        initialized = true;

        loadMusic("audio/ambient/crystal_caves.ogg");
        loadMusic("audio/ambient/mushroom_grotto.ogg");
        loadMusic("audio/ambient/lava_caverns.ogg");
        loadMusic("audio/ambient/shadow_abyss.ogg");
        loadMusic("audio/ambient/frost_vaults.ogg");
        loadMusic("audio/ambient/toxic_mire.ogg");
        loadMusic("audio/music/boss_stone_golem.ogg");
        loadMusic("audio/music/boss_mire_wyrm.ogg");

        loadSfx("hit", "audio/sfx/hit.ogg");
        loadSfx("miss", "audio/sfx/miss.ogg");
        loadSfx("critical", "audio/sfx/critical.ogg");
        loadSfx("death", "audio/sfx/death.ogg");
        loadSfx("chest_open", "audio/sfx/chest_open.ogg");
        loadSfx("door_unlock", "audio/sfx/door_unlock.ogg");
        loadSfx("footstep", "audio/sfx/footstep.ogg");
        loadSfx("battle_start", "audio/sfx/battle_start.ogg");
        loadSfx("victory", "audio/sfx/victory.ogg");
        loadSfx("level_up", "audio/sfx/level_up.ogg");
        loadSfx("trap_trigger", "audio/sfx/trap_trigger.ogg");
        loadSfx("shop_buy", "audio/sfx/shop_buy.ogg");
        loadSfx("achievement_unlock", "audio/sfx/achievement_unlock.ogg");
        loadSfx("craft", "audio/sfx/craft.ogg");

        if (currentAmbient == null)
            currentAmbient = "audio/ambient/crystal_caves.ogg";
    }

    public void playHit() {
        playSfx("hit");
    }

    public void playMiss() {
        playSfx("miss");
    }

    public void playCritical() {
        playSfx("critical");
    }

    public void playDeath() {
        playSfx("death");
    }

    public void playChestOpen() {
        playSfx("chest_open");
    }

    public void playDoorUnlock() {
        playSfx("door_unlock");
    }

    public void playFootstep() {
        playSfx("footstep", volume * 0.3f);
    }

    public void playBattleStart() {
        playSfx("battle_start");
    }

    public void playVictory() {
        playSfx("victory");
    }

    public void playLevelUp() {
        playSfx("level_up");
    }

    public void playTrapTrigger() {
        playSfx("trap_trigger");
    }

    public void playShopBuy() {
        playSfx("shop_buy");
    }

    public void playAchievement() {
        playSfx("achievement_unlock");
    }

    public void playAmbient() {
        init();
        if (currentAmbient != null)
            playMusic(currentAmbient, true);
    }

    public void playAmbient(Biome biome) {
        init();
        if (!enabled || biome == null)
            return;
        currentAmbient = biome.ambientTrack;
        currentMusic = currentAmbient;
        playMusic(currentAmbient, true);
    }

    public void playBossTheme(Enemy.EnemyType bossType) {
        init();
        if (!enabled || bossType == null)
            return;
        currentMusic = bossType == Enemy.EnemyType.BOSS_WYRM
                ? "audio/music/boss_mire_wyrm.ogg"
                : "audio/music/boss_stone_golem.ogg";
        playMusic(currentMusic, true);
    }

    public void playSfx(String id) {
        playSfx(id, volume);
    }

    public void playSfx(String id, float gain) {
        init();
        if (!enabled || id == null)
            return;
        Sound sound = sfx.get(id);
        if (sound != null)
            sound.play(Math.max(0f, Math.min(1f, gain)));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && activeMusic != null)
            activeMusic.stop();
        if (enabled && currentMusic != null)
            playMusic(currentMusic, true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVolume(float v) {
        this.volume = Math.max(0, Math.min(1, v));
    }

    public float getVolume() {
        return volume;
    }

    public void setMusicVolume(float v) {
        this.musicVolume = Math.max(0, Math.min(1, v));
        if (activeMusic != null)
            activeMusic.setVolume(this.musicVolume);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public String getCurrentMusic() {
        return currentMusic;
    }

    public String getCurrentAmbient() {
        return currentAmbient;
    }

    public void dispose() {
        for (Sound sound : sfx.values()) {
            sound.dispose();
        }
        sfx.clear();

        for (Music track : music.values()) {
            track.dispose();
        }
        music.clear();

        activeMusic = null;
        initialized = false;
    }

    private void loadSfx(String id, String path) {
        try {
            FileHandle file = Gdx.files.internal(path);
            if (file.exists())
                sfx.put(id, Gdx.audio.newSound(file));
        } catch (Exception ignored) {
            // Keep running when optional assets are missing.
        }
    }

    private void loadMusic(String path) {
        try {
            FileHandle file = Gdx.files.internal(path);
            if (file.exists())
                music.put(path, Gdx.audio.newMusic(file));
        } catch (Exception ignored) {
            // Keep running when optional assets are missing.
        }
    }

    private void playMusic(String path, boolean looping) {
        if (!enabled || path == null)
            return;

        Music next = music.get(path);
        if (next == null)
            return;

        if (activeMusic != null && activeMusic != next)
            activeMusic.stop();

        activeMusic = next;
        activeMusic.setLooping(looping);
        activeMusic.setVolume(musicVolume);
        if (!activeMusic.isPlaying())
            activeMusic.play();
    }
}
