package com.caveadventure.engine;

import com.caveadventure.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillTreeTest {

    @Test
    void multiLevelXpGainGrantsOnePointPerLevel() {
        Player player = new Player(0, 0);
        SkillTree skills = new SkillTree(null);
        int levelBefore = player.getLevel();

        player.addXP(400); // 100 + 125 + 156 -> level 4

        assertEquals(4, player.getLevel());
        skills.grantSkillPoints(player.getLevel() - levelBefore);
        assertEquals(3, skills.getSkillPoints());
    }

    @Test
    void negativeGrantIsIgnored() {
        SkillTree skills = new SkillTree(null);
        skills.grantSkillPoints(-2);
        assertEquals(0, skills.getSkillPoints());
    }
}
