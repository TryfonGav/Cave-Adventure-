Audio assets checklist for Cave Adventure

Place files under: src/main/resources/assets/audio/

Current required files (hardcoded paths)

- audio/ambient/crystal_caves.ogg
- audio/ambient/mushroom_grotto.ogg
- audio/ambient/lava_caverns.ogg
- audio/ambient/shadow_abyss.ogg
- audio/ambient/frost_vaults.ogg
- audio/ambient/toxic_mire.ogg
- audio/music/boss_stone_golem.ogg
- audio/music/boss_mire_wyrm.ogg

Suggested SFX filename map (for SoundManager stubs)

- playChestOpen() -> audio/sfx/chest_open.ogg
- playDoorUnlock() -> audio/sfx/door_unlock.ogg
- playTrapTrigger() -> audio/sfx/trap_trigger.ogg
- playLevelUp() -> audio/sfx/level_up.ogg
- playBattleStart() -> audio/sfx/battle_start.ogg
- playVictory() -> audio/sfx/victory.ogg
- playAchievement() -> audio/sfx/achievement_unlock.ogg
- playSfx("craft") -> audio/sfx/craft.ogg

Optional future SFX map (methods already present but not currently called everywhere)

- playHit() -> audio/sfx/hit.ogg
- playMiss() -> audio/sfx/miss.ogg
- playCritical() -> audio/sfx/critical.ogg
- playDeath() -> audio/sfx/death.ogg
- playFootstep() -> audio/sfx/footstep.ogg
- playShopBuy() -> audio/sfx/shop_buy.ogg

Suggested folder structure

- audio/
	- ambient/
		- crystal_caves.ogg
		- mushroom_grotto.ogg
		- lava_caverns.ogg
		- shadow_abyss.ogg
		- frost_vaults.ogg
		- toxic_mire.ogg
	- music/
		- boss_stone_golem.ogg
		- boss_mire_wyrm.ogg
	- sfx/
		- chest_open.ogg
		- door_unlock.ogg
		- trap_trigger.ogg
		- level_up.ogg
		- battle_start.ogg
		- victory.ogg
		- achievement_unlock.ogg
		- craft.ogg
		- hit.ogg
		- miss.ogg
		- critical.ogg
		- death.ogg
		- footstep.ogg
		- shop_buy.ogg

Notes

- Prefer .ogg for looped ambient/music and short SFX in LibGDX projects.
- Keep filenames lowercase_with_underscores to match the current naming style.
- After adding files, wire load/play/dispose logic in SoundManager.
