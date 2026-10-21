package com.caveadventure.entity;

import com.caveadventure.engine.SkillTree;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRegenTest {

    private static SkillTree regenSkills() {
        SkillTree skills = new SkillTree(null);
        skills.restoreUnlockedSkills(EnumSet.of(SkillTree.Skill.REGEN));
        return skills;
    }

    @Test
    void battleRegenIsFivePercentWithMinimumFive() {
        Player player = new Player(0, 0);
        assertEquals(100, player.getMaxHealth());
        assertEquals(5, player.getBattleRegenAmount());

        player.addXP(100_000); // many level-ups raise max HP
        assertEquals(Math.round(player.getMaxHealth() * 0.05f), player.getBattleRegenAmount());
        assertTrue(player.getBattleRegenAmount() > 5);
    }

    @Test
    void passiveRegenHealsEveryTwoSeconds() {
        Player player = new Player(0, 0);
        player.setHealth(50);
        SkillTree skills = regenSkills();

        player.tickPassiveRegen(skills, 1.9f);
        assertEquals(50, player.getHealth());
        player.tickPassiveRegen(skills, 0.1f);
        assertEquals(50 + player.getPassiveRegenAmount(), player.getHealth());
    }

    @Test
    void noRegenWithoutTheSkill() {
        Player player = new Player(0, 0);
        player.setHealth(50);

        player.tickPassiveRegen(new SkillTree(null), 10f);

        assertEquals(50, player.getHealth());
    }
}
