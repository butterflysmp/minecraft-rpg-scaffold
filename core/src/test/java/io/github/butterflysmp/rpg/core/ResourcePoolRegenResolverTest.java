package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.MaxResolver;
import io.github.butterflysmp.rpg.core.combat.RegenResolver;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PER-OWNER regeneration RATE -- the second half of the lift {@code MaxResolver} started.
 *
 * <p>{@code ResourcePoolTest} is again left byte-identical, and so are all nine construction sites,
 * which is what makes this change provably behaviour-preserving. {@code ResourcePoolMaxResolverTest}
 * owns the ceiling and the shared arity test. This file owns the rate.
 *
 * <p>The headline is {@link #aRateChangeREPRICESElapsedTicksUnlessTheCallerPINS}: unlike health
 * regeneration, which pays eagerly and can never re-price anything, this pool INTEGRATES over elapsed
 * ticks. That is the whole reason a rate change needs the same pin a ceiling change needs.
 *
 * Each test names the mutation it forces red.
 */
class ResourcePoolRegenResolverTest {

    private static final String MANA = "mana";
    private static final double EPS = 1e-9;

    private static final UUID PLAIN = UUID.randomUUID();
    private static final UUID ENCHANTED = UUID.randomUUID();

    /** 1.0/tick for everyone, 2.0/tick for one owner -- a mana-regen piece, in effect. */
    private static RegenResolver perOwner() {
        return (owner, resourceId) -> ENCHANTED.equals(owner) ? 2.0 : 1.0;
    }

    private static ResourcePool pool(AtomicLong tick, RegenResolver regen) {
        return new ResourcePool(tick::get, MaxResolver.fixed(100), regen);
    }

    // --- The rate is the OWNER'S ------------------------------------------------------------------

    @Test
    void twoOwnersRegenerateAtTheirOWNRateRatherThanOneShared() {
        AtomicLong tick = new AtomicLong(0);
        ResourcePool pool = pool(tick, perOwner());
        assertTrue(pool.tryConsume(PLAIN, MANA, 100), "empty the plain player");
        assertTrue(pool.tryConsume(ENCHANTED, MANA, 100), "and the enchanted one");

        tick.set(10);
        assertEquals(10, pool.current(PLAIN, MANA), EPS, "10 ticks at 1.0");
        assertEquals(20, pool.current(ENCHANTED, MANA), EPS, "10 ticks at 2.0");
        // Mutation: ignore the owner in the resolver -> both read the same -> reddens.
    }

    @Test
    void theRateResolverIsAskedPerRESOURCEAndNotOnlyPerOwner() {
        // The same property the ceiling has, for the same reason: the pool is keyed by
        // (owner, resourceId) and promises "mana, and whatever else content asks for", so a resolver
        // ignoring the id would speed up every future resource at once.
        AtomicLong tick = new AtomicLong(0);
        ResourcePool pool = pool(tick, (owner, resourceId) -> MANA.equals(resourceId) ? 2.0 : 1.0);
        assertEquals(2.0, pool.regen(ENCHANTED, MANA), EPS);
        assertEquals(1.0, pool.regen(ENCHANTED, "grenade_charges"), EPS);
        // Mutation: drop the resourceId from the resolver signature -> the two agree -> reddens.
    }

    @Test
    void regenExposesTheResolvedRateSoADisplayNeedNotReDeriveIt() {
        // The accessor that did not exist before this slice, because before this the rate was a
        // constant nothing could ask about. It is the sibling of max(owner, resourceId), and it is
        // what keeps a stat sheet from composing base + bonus a second time and drifting.
        ResourcePool pool = pool(new AtomicLong(0), perOwner());
        assertEquals(1.0, pool.regen(PLAIN, MANA), EPS);
        assertEquals(2.0, pool.regen(ENCHANTED, MANA), EPS);
        // Mutation: have regen() return a constant -> the enchanted row reddens.
    }

    // --- THE HEADLINE: lazy integration re-prices the past --------------------------------------

    @Test
    void aRateChangeREPRICESElapsedTicksUnlessTheCallerPINS() {
        // THE reason this slice needed a decision rather than a copy of the health-regen shape.
        //
        // regenerated() computes amount + elapsed * rate. The rate it uses is TODAY'S, but elapsed
        // reaches back to the last write -- so raising the rate pays the new rate for ticks that
        // already happened at the old one. Equipping an item retroactively regenerates your past.
        //
        // This is the exact free-on-equip defect setCurrent exists to prevent for the ceiling. The
        // pool cannot fix it alone: it has no idea a rate changed. The caller pins -- see
        // ManaTransition.
        AtomicLong tick = new AtomicLong(0);
        Map<UUID, Double> rate = new HashMap<>();
        rate.put(ENCHANTED, 1.0);
        ResourcePool pool = pool(tick, (owner, resourceId) -> rate.getOrDefault(owner, 1.0));

        assertTrue(pool.tryConsume(ENCHANTED, MANA, 100), "spend to empty at tick 0");
        tick.set(20);
        assertEquals(20, pool.current(ENCHANTED, MANA), EPS, "20 ticks at 1.0 = 20 mana");

        // UNPINNED: the rate doubles and the twenty ticks already elapsed are re-priced.
        rate.put(ENCHANTED, 2.0);
        assertEquals(40, pool.current(ENCHANTED, MANA), EPS,
                "20 mana became 40 without a tick passing -- the free-on-equip defect");

        // PINNED, which is what the caller actually does: read first, change, write the reading back.
        rate.put(ENCHANTED, 1.0);
        double before = pool.current(ENCHANTED, MANA);
        rate.put(ENCHANTED, 2.0);
        pool.setCurrent(ENCHANTED, MANA, before);
        assertEquals(20, pool.current(ENCHANTED, MANA), EPS,
                "pinned: the past keeps its old rate and nothing appears from equipping");

        tick.set(30);
        assertEquals(40, pool.current(ENCHANTED, MANA), EPS,
                "and the NEW rate applies forward only -- 10 more ticks at 2.0");
        // Mutation: skip the pin -> the third assertion reads 40 -> reddens. This is the test that
        // separates "a faster rate from now" from "a faster rate retroactively".
    }

    // --- Arity on the read path -------------------------------------------------------------------

    @Test
    void currentDoesNotAskTheRateResolverAtALLWhenTheEntryIsABSENT() {
        // The absent branch returns the ceiling; it never touches the rate. And this is the HOT read
        // -- StatsBarSystem calls current() twice a second for every online player, most often on
        // exactly this branch, because a player who has not cast has no entry.
        //
        // Asymmetric with the ceiling ON PURPOSE: the ceiling IS the absent branch's answer, so it
        // must be resolved unconditionally. The rate is not, so it is not.
        AtomicInteger regenAsked = new AtomicInteger();
        ResourcePool pool = new ResourcePool(new AtomicLong(0)::get, MaxResolver.fixed(100),
                (owner, resourceId) -> { regenAsked.incrementAndGet(); return 1.0; });

        assertEquals(100, pool.current(PLAIN, MANA), EPS, "absent, so full");
        assertEquals(0, regenAsked.get(), "and the rate was never asked for -- nothing would use it");

        assertTrue(pool.tryConsume(PLAIN, MANA, 40), "now there is an entry");
        regenAsked.set(0);
        assertEquals(60, pool.current(PLAIN, MANA), EPS);
        assertEquals(1, regenAsked.get(), "with an entry present it is asked exactly once");
        // Mutation: hoist the rate resolve above the ternary in current() -> the absent case asks
        // once -> reddens on the 0.
    }

    // --- Construction -----------------------------------------------------------------------------

    @Test
    void theRateGuardSTILLREJECTSANegativeConstantAfterMovingOffTheSharedPath() {
        // The check used to live in the resolver constructor, where every construction passed a
        // double. It moved to the constant-taking overload, which is now the only place a number
        // arrives -- so this test exists to prove the move did not quietly drop it. A negative rate
        // would make regenerated() DRAIN the pool as ticks passed.
        assertThrows(IllegalArgumentException.class,
                () -> new ResourcePool(new AtomicLong(0)::get, MaxResolver.fixed(100), -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new ResourcePool(new AtomicLong(0)::get, 100, -1.0));
        // A resolver, by contrast, is NOT validated -- it can return anything at any time, so there
        // is no single moment at which to check it. Stated rather than left as an omission.
        assertThrows(IllegalArgumentException.class,
                () -> new ResourcePool(new AtomicLong(0)::get, MaxResolver.fixed(100), (RegenResolver) null));
        // Mutation: drop requireNonNegative from the double overload -> the first two rows redden.
    }
}
