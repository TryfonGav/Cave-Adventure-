package com.caveadventure.engine;

import com.caveadventure.entity.Player;
import com.caveadventure.item.Item;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomEventManagerTest {

    /** Random whose nextFloat() is fixed, so the per-step chance roll is controlled; nextInt stays seeded. */
    private static Random rollAlways(float value, long seed) {
        return new Random(seed) {
            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    @Test
    void tickCountsTheCooldownDownToZero() {
        RandomEventManager events = new RandomEventManager(null, rollAlways(0f, 1));
        events.reset(1);
        assertEquals(RandomEventManager.EVENT_GRACE_SECONDS, events.getEventCooldown());

        events.tick(4f);
        assertEquals(RandomEventManager.EVENT_GRACE_SECONDS - 4f, events.getEventCooldown(), 1e-4);

        events.tick(100f);
        assertEquals(0f, events.getEventCooldown());
    }

    @Test
    void noEventDuringTheGracePeriodAfterReset() {
        RandomEventManager events = new RandomEventManager(null, rollAlways(0f, 1));
        events.reset(1);

        assertFalse(events.tryTrigger(false));
        events.tick(RandomEventManager.EVENT_GRACE_SECONDS - 0.1f);
        assertFalse(events.tryTrigger(false));

        events.tick(0.2f);
        assertTrue(events.tryTrigger(false));
        assertTrue(events.isActive());
    }

    // --- P2-T3: rewards scale with the floor ---

    @Test
    void eventRewardsScaleWithTheFloor() {
        RandomEventManager events = new RandomEventManager(null, new Random(1));
        events.reset(7);
        Player player = new Player(1, 1);

        events.applyEffect(RandomEventManager.EventEffect.XP_BONUS, player);
        assertEquals(143, Balance.eventXp(7)); // 75 × 1.9
        assertEquals(2, player.getLevel());
        assertEquals(143 - Balance.FIRST_LEVEL_XP, player.getXP());

        events.applyEffect(RandomEventManager.EventEffect.GIVE_GOLD, player);
        assertEquals(Balance.eventGold(7), player.getInventory().count(Item.ItemType.GOLD_NUGGET));
        assertEquals(5, Balance.eventGold(7));

        events.applyEffect(RandomEventManager.EventEffect.GIVE_WEAPON, player);
        assertEquals(1, player.getInventory().count(Item.ItemType.CRYSTAL_BLADE));

        player.setHealth(1);
        events.applyEffect(RandomEventManager.EventEffect.HEAL_30, player);
        assertEquals(1 + Math.round(player.getMaxHealth() * Balance.EVENT_HEAL_FRACTION), player.getHealth());
    }

    @Test
    void resultTextShowsTheRealAmounts() {
        RandomEventManager events = new RandomEventManager(null, new Random(1));
        events.reset(1);
        Player player = new Player(1, 1);
        RandomEventManager.EventChoice xp = new RandomEventManager.EventChoice("Listen", "+%d XP!",
                RandomEventManager.EventEffect.XP_BONUS);
        RandomEventManager.EventChoice weapon = new RandomEventManager.EventChoice("Break", "A hidden %s!",
                RandomEventManager.EventEffect.GIVE_WEAPON);

        assertEquals("+75 XP!", events.resultTextFor(xp, player));
        assertEquals("A hidden Iron Sword!", events.resultTextFor(weapon, player));
        events.reset(10);
        assertEquals("+176 XP!", events.resultTextFor(xp, player));
        assertEquals("A hidden Fire Axe!", events.resultTextFor(weapon, player));
    }

    @Test
    void failedRollStartsNoEvent() {
        RandomEventManager events = new RandomEventManager(null, rollAlways(RandomEventManager.EVENT_CHANCE_PER_STEP, 1));

        assertFalse(events.tryTrigger(false));
        assertFalse(events.isActive());
    }

    @Test
    void noCompanionEventWhenThePlayerHasACompanion() {
        for (long seed = 0; seed < 200; seed++) {
            RandomEventManager events = new RandomEventManager(null, rollAlways(0f, seed));
            assertTrue(events.tryTrigger(true));
            assertFalse(RandomEventManager.offersCompanion(events.getCurrentEvent()),
                    events.getCurrentEvent().title);
        }
    }

    @Test
    void companionEventsStillRollWithoutACompanion() {
        Set<String> titles = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            RandomEventManager events = new RandomEventManager(null, rollAlways(0f, seed));
            assertTrue(events.tryTrigger(false));
            if (RandomEventManager.offersCompanion(events.getCurrentEvent()))
                titles.add(events.getCurrentEvent().title);
        }
        assertEquals(3, titles.size(), titles.toString());
    }
}
