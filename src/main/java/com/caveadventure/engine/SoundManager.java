package com.caveadventure.engine;

/**
 * Sound manager framework — stub for future audio.
 * LibGDX requires wav/ogg/mp3 files. When audio files are added to
 * src/main/resources/sounds/,
 * load them in init() and play them via the named methods.
 */
public class SoundManager {

    private static SoundManager instance;
    private boolean enabled;
    private float volume;

    private SoundManager() {
        this.enabled = true;
        this.volume = 0.7f;
    }

    public static SoundManager getInstance() {
        if (instance == null)
            instance = new SoundManager();
        return instance;
    }

    public void init() {
        // Load sounds here when audio files are available:
        // hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hit.wav"));
    }

    // Stubs — called from game code, will play audio once files are added
    public void playHit() {
        /* hitSound.play(volume); */ }

    public void playMiss() {
        /* missSound.play(volume); */ }

    public void playCritical() {
        /* critSound.play(volume); */ }

    public void playDeath() {
        /* deathSound.play(volume); */ }

    public void playChestOpen() {
        /* chestSound.play(volume); */ }

    public void playDoorUnlock() {
        /* doorSound.play(volume); */ }

    public void playFootstep() {
        /* stepSound.play(volume * 0.3f); */ }

    public void playBattleStart() {
        /* battleStartSound.play(volume); */ }

    public void playVictory() {
        /* victorySound.play(volume); */ }

    public void playLevelUp() {
        /* levelUpSound.play(volume); */ }

    public void playTrapTrigger() {
        /* trapSound.play(volume); */ }

    public void playShopBuy() {
        /* shopBuySound.play(volume); */ }

    public void playAchievement() {
        /* achievementSound.play(volume); */ }

    public void playAmbient() {
        /* ambientLoop.play(volume * 0.4f); */ }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public void dispose() {
        // Dispose all loaded sounds
    }
}
