package com.caveadventure.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerBuffTest {

    @Test
    void buffsCountDownOncePerBattleAndStopAtZero() {
        Player player = new Player(0, 0);
        player.grantAttackBuff(3);
        player.grantDefenseBuff(1);

        player.consumeBattleBuffs();
        assertEquals(2, player.getAttackBuffBattles());
        assertEquals(0, player.getDefenseBuffBattles());

        player.consumeBattleBuffs();
        player.consumeBattleBuffs();
        player.consumeBattleBuffs();
        assertEquals(0, player.getAttackBuffBattles());
        assertEquals(0, player.getDefenseBuffBattles());
    }

    @Test
    void regrantingKeepsTheLongerDuration() {
        Player player = new Player(0, 0);
        player.grantAttackBuff(3);
        player.consumeBattleBuffs();
        player.grantAttackBuff(1);
        assertEquals(2, player.getAttackBuffBattles());

        player.grantAttackBuff(3);
        assertEquals(3, player.getAttackBuffBattles());
    }
}
