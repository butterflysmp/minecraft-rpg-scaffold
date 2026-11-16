package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a body field becomes an attribute's BASE value. Both rules are pure, and both are written so that applying
 * them twice gives the same answer as applying them once: a body is re-applied when a stage changes (R2 onward),
 * and a rule that read the CURRENT value would compound.
 */
class MobBodiesTest {

    /**
     * A movement-speed attribute: the ENTITY TYPE's own base speed (0.25 for a creeper or a wither skeleton, 0.3 for
     * a spider, as vanilla's createAttributes sets them), and a base we may write. The port names that quantity,
     * {@code typeBase()}, so a fake cannot stand in for the attribute's registry default (0.7), which is what the
     * 2026-11-15 boot found the applier reading. Which Bukkit call supplies it is pinned in paper, by
     * DragonFixWiringTest, because no core row can see it.
     */
    private static final class FakeAttribute implements MobBodies.BodyAttribute {
        final double typeBase;
        double base;
        FakeAttribute(double typeBase) { this.typeBase = typeBase; this.base = typeBase; }
        @Override public double typeBase() { return typeBase; }
        @Override public double baseValue() { return base; }
        @Override public void setBaseValue(double v) { base = v; }
    }

    @Test
    void speedIsAMultiplierOfTheTypesDefaultNotAnAbsolute() {
        FakeAttribute speed = new FakeAttribute(0.25);   // a wither skeleton's type base
        MobBodies.applySpeed(speed, 0.6);
        assertEquals(0.25 * 0.6, speed.base, 0.0);
    }

    /**
     * A body already written (a grown Phage: stage 1's 1.5x is on its base) re-applied at stage 2 lands on the TYPE's
     * base x 1.25, not on the written base x 1.25. The type base is the one input a second apply cannot have moved.
     */
    @Test
    void aGrownBodyIsRescaledFromTheTypesBaseNotFromItsOwn() {
        FakeAttribute speed = new FakeAttribute(0.25);   // a creeper's type base
        MobBodies.applySpeed(speed, 1.5);
        assertEquals(0.375, speed.base, 0.0, "stage 1");
        MobBodies.applySpeed(speed, 1.25);
        assertEquals(0.3125, speed.base, 0.0, "stage 2: 0.25 x 1.25, not 0.375 x 1.25 = 0.46875");
    }

    /** THE row a wrong implementation fails: base x multiplier compounds to 0.25 x 0.6 x 0.6 on the second call. */
    @Test
    void reapplyingTheSpeedDoesNotStack() {
        FakeAttribute speed = new FakeAttribute(0.25);
        MobBodies.applySpeed(speed, 0.6);
        MobBodies.applySpeed(speed, 0.6);
        assertEquals(0.25 * 0.6, speed.base, 0.0, "applied twice, still the type base x 0.6");
    }

    /** A creeper is 1.7 tall at scale 1; one block tall is scale 1/1.7, three blocks 3/1.7. */
    @Test
    void aHeightBecomesTheScaleThatProducesIt() {
        assertEquals(1.0 / 1.7, MobBodies.scaleForHeight(1.0, 1.7, 1.0), 0.0);
        assertEquals(3.0 / 1.7, MobBodies.scaleForHeight(3.0, 1.7, 1.0), 0.0);
    }

    /**
     * Re-applied to a body already scaled, the rule recovers the unscaled height from the CURRENT height and scale,
     * so the result does not compound. Asserted within 1e-12 because the round trip divides and multiplies.
     */
    @Test
    void reapplyingAHeightDoesNotStack() {
        double first = MobBodies.scaleForHeight(3.0, 1.7, 1.0);
        double heightNow = 1.7 * first;
        double second = MobBodies.scaleForHeight(3.0, heightNow, first);
        assertEquals(first, second, 1e-12);
    }
}
