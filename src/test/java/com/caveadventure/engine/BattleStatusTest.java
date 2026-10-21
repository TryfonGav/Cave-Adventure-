package com.caveadventure.engine;

import com.caveadventure.engine.BattleScreen.StatusEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleStatusTest {

    @Test
    void stunIsConsumedOnce() {
        BattleStatus status = new BattleStatus();
        assertTrue(status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS));

        assertTrue(status.consumeStun());
        assertEquals(StatusEffect.NONE, status.current());
        assertFalse(status.consumeStun());
    }

    @Test
    void endRoundDoesNotRemoveStun() {
        BattleStatus status = new BattleStatus();
        status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS);

        status.endRound();
        status.endRound();

        assertEquals(StatusEffect.STUN, status.current());
    }

    @Test
    void poisonLastsExactlyItsTurns() {
        BattleStatus status = new BattleStatus();
        status.apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);

        for (int i = 0; i < BattleStatus.POISON_TURNS - 1; i++) {
            status.endRound();
            assertEquals(StatusEffect.POISON, status.current(), "after round " + (i + 1));
        }
        status.endRound();

        assertEquals(StatusEffect.NONE, status.current());
    }

    @Test
    void reapplyingRefreshesDuration() {
        BattleStatus status = new BattleStatus();
        status.apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);
        status.endRound();
        status.endRound();
        assertEquals(1, status.turnsLeft());

        status.apply(StatusEffect.POISON, BattleStatus.POISON_TURNS);

        assertEquals(BattleStatus.POISON_TURNS, status.turnsLeft());
    }

    @Test
    void consumedStunGivesImmunityUntilNextTurn() {
        BattleStatus status = new BattleStatus();
        status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS);
        status.consumeStun();

        assertFalse(status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS), "immune right after a stun");
        status.endRound();
        assertFalse(status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS), "still immune before its next turn");

        assertFalse(status.consumeStun(), "next turn is not skipped");
        assertTrue(status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS), "immunity ends when that turn starts");
    }

    @Test
    void immunityDoesNotBlockOtherEffects() {
        BattleStatus status = new BattleStatus();
        status.apply(StatusEffect.STUN, BattleStatus.STUN_TURNS);
        status.consumeStun();

        assertTrue(status.apply(StatusEffect.POISON, BattleStatus.POISON_TURNS));
        assertEquals("POISON(3)", status.toString());
    }
}
