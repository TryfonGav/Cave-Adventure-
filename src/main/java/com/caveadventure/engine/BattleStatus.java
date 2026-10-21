package com.caveadventure.engine;

import com.caveadventure.engine.BattleScreen.StatusEffect;

/**
 * Turn-based status effect of one battle participant (player or enemy).
 * Holds one effect at a time; applying a new one replaces the old.
 *
 * <p>POISON and BURN count down in {@link #endRound()}. STUN is never counted
 * down by rounds: it stays until {@link #consumeStun()} uses it up at the start
 * of the side's turn, after which the side cannot be stunned again until its
 * following turn starts (stun-lock guard).
 */
public class BattleStatus {

    /** Rounds a poison lasts. */
    public static final int POISON_TURNS = 3;
    /** Rounds a burn lasts. */
    public static final int BURN_TURNS = 2;
    /** Turns a stun skips. */
    public static final int STUN_TURNS = 1;

    private StatusEffect effect = StatusEffect.NONE;
    private int turnsLeft;
    private boolean stunImmune;

    /**
     * Apply an effect for the given number of turns, refreshing the duration if it
     * is already active. Returns false (and changes nothing) if a stun is applied
     * while the side is stun-immune.
     */
    public boolean apply(StatusEffect newEffect, int turns) {
        if (newEffect == StatusEffect.STUN && stunImmune)
            return false;
        effect = newEffect;
        turnsLeft = newEffect == StatusEffect.NONE ? 0 : Math.max(1, turns);
        return true;
    }

    /** The active effect, or NONE. */
    public StatusEffect current() {
        return effect;
    }

    /** Turns the active effect has left (0 when there is none). */
    public int turnsLeft() {
        return turnsLeft;
    }

    /**
     * Call at the start of this side's turn. If stunned, removes the stun, makes the
     * side stun-immune until its next turn starts and returns true (the turn is
     * lost). Otherwise clears the stun immunity and returns false.
     */
    public boolean consumeStun() {
        if (effect == StatusEffect.STUN) {
            clear();
            stunImmune = true;
            return true;
        }
        stunImmune = false;
        return false;
    }

    /** Call once per full round: counts POISON and BURN down. STUN is left alone. */
    public void endRound() {
        if (effect == StatusEffect.POISON || effect == StatusEffect.BURN) {
            turnsLeft--;
            if (turnsLeft <= 0)
                clear();
        }
    }

    /** Remove the active effect. Stun immunity is kept. */
    public void clear() {
        effect = StatusEffect.NONE;
        turnsLeft = 0;
    }

    /** Remove the active effect and the stun immunity (new battle). */
    public void reset() {
        clear();
        stunImmune = false;
    }

    @Override
    public String toString() {
        return effect == StatusEffect.NONE ? "NONE" : effect + "(" + turnsLeft + ")";
    }
}
