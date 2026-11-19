package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.FakeWorld;
import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * F9b, fix B (the seat's brief of 2026-11-19): NO FIGHT DAMAGE OUTSIDE THE FIGHT. A fight's hit is decided on one tick
 * and applied on a later one (the adapter defers it to the victim's own thread), so the fight's condition, "he is a
 * participant of THIS fight and in its world", must be asked when the damage is APPLIED, not when it is priced. The
 * first boot's RA3: two bites priced while he was in the fight landed after he had been returned, and killed him.
 *
 * <p>This pins the interface's default (a synchronous handle asks at once). The deferring adapter asks inside its
 * deferral ({@code FightRulesSourceTest}).
 */
class ApplyDamageIfTest {

    @Test
    void aHoldingConditionDealsTheHitThroughTheOrdinaryEntry() {
        FakeWorld.Dummy d = new FakeWorld.Dummy(new Vec3(0, 0, 0));
        AtomicInteger refused = new AtomicInteger();
        d.applyDamageIf(() -> true, refused::incrementAndGet, 25.0, UUID.randomUUID(), CritState.NORMAL,
                DefenseRule.APPLIES);
        assertEquals(1, d.damageCalls);
        assertEquals(0, refused.get());
    }

    @Test
    void aFailedConditionDealsNothingAndSaysSo() {
        FakeWorld.Dummy d = new FakeWorld.Dummy(new Vec3(0, 0, 0));
        AtomicInteger refused = new AtomicInteger();
        double before = d.health;
        d.applyDamageIf(() -> false, refused::incrementAndGet, 25.0, UUID.randomUUID(), CritState.NORMAL,
                DefenseRule.APPLIES);
        assertEquals(0, d.damageCalls, "no damage at all");
        assertEquals(before, d.health, 0.0);
        assertEquals(1, refused.get(), "and the refusal is told, once");
    }

    @Test
    void theConditionIsAskedOncePerHit() {
        FakeWorld.Dummy d = new FakeWorld.Dummy(new Vec3(0, 0, 0));
        AtomicInteger asked = new AtomicInteger();
        d.applyDamageIf(() -> asked.incrementAndGet() > 0, () -> { }, 5.0, UUID.randomUUID(), CritState.NORMAL,
                DefenseRule.BYPASSED);
        assertEquals(1, asked.get());
    }
}
