package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 4b's "DAMAGED HIM" FEED (the seat, 2026-11-24): fed from the store's own health changes, which every damage to him
 * passes, with the dealer the store was given. READ: a mob's projectile is attributed to its CAUSING entity (the shooter,
 * {@code RpgListeners.attributableId}), so it counts as that mob's damage.
 */
class SummonFeedsTest {

    private static final UUID HIM = new UUID(0, 1), MOB = new UUID(0, 2), OTHER = new UUID(0, 3);

    private static HealthChange damage(UUID target, boolean targetIsPlayer, UUID dealer, boolean dealerIsPlayer,
                                       double amount) {
        return new HealthChange(target, targetIsPlayer, HealthChange.Kind.DAMAGE, amount, dealer, dealerIsPlayer,
                90, 100, false, false, null, amount);
    }

    @Test
    void aMobsDamageToAPlayerIsRecordedAtItsTick() {
        AtomicLong tick = new AtomicLong(500);
        SummonFeeds feeds = new SummonFeeds(tick::get);
        feeds.onChange(damage(HIM, true, MOB, false, 10));
        assertEquals(OptionalLong.of(500), feeds.damagedAt(HIM, MOB));
        tick.set(560);
        feeds.onChange(damage(HIM, true, MOB, false, 4));
        assertEquals(OptionalLong.of(560), feeds.damagedAt(HIM, MOB), "the latest hit");
    }

    @Test
    void onlyAMobDamagingAPlayerCounts() {
        SummonFeeds feeds = new SummonFeeds(() -> 500);
        feeds.onChange(damage(HIM, true, OTHER, true, 10));
        assertEquals(OptionalLong.empty(), feeds.damagedAt(HIM, OTHER), "a player's hit on him: not a trigger");
        feeds.onChange(damage(MOB, false, HIM, true, 10));
        assertEquals(OptionalLong.empty(), feeds.damagedAt(MOB, HIM), "his hit on a mob: not a trigger");
        // THE ROW MUTFEEDTARGET FOUND MISSING (2026-11-24): the line above has a PLAYER dealer too, so it never isolated
        // the target check. A mob hitting a MOB, both non-players: only the target check refuses it.
        feeds.onChange(damage(OTHER, false, MOB, false, 10));
        assertEquals(OptionalLong.empty(), feeds.damagedAt(OTHER, MOB), "a mob's hit on a mob: not a trigger");
        feeds.onChange(damage(HIM, true, MOB, false, 0));
        assertEquals(OptionalLong.empty(), feeds.damagedAt(HIM, MOB), "nothing dealt: not a hit");
        feeds.onChange(damage(HIM, true, HIM, false, 5));
        assertEquals(OptionalLong.empty(), feeds.damagedAt(HIM, HIM), "self-sourced (a fall) is no mob's");
        feeds.onChange(new HealthChange(HIM, true, HealthChange.Kind.HEAL, 5, MOB, false, 95, 100, false, false, null, 5));
        assertEquals(OptionalLong.empty(), feeds.damagedAt(HIM, MOB), "a heal is not damage");
    }

    @Test
    void forgettingHimForgetsEverything() {
        SummonFeeds feeds = new SummonFeeds(() -> 500);
        feeds.onChange(damage(HIM, true, MOB, false, 10));
        feeds.forget(HIM);
        assertEquals(OptionalLong.empty(), feeds.damagedAt(HIM, MOB));
    }
}
