package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a body field becomes an attribute's BASE value. Both rules are pure, and both are written so that applying
 * them twice gives the same answer as applying them once: a body is re-applied when a stage changes (R2 onward),
 * and a rule that read the CURRENT value would compound.
 */
class MobBodiesTest {

    /** A movement-speed attribute: a fixed per-type default, and a base we may write. */
    private static final class FakeAttribute implements MobBodies.BodyAttribute {
        final double defaultValue;
        double base;
        FakeAttribute(double defaultValue) { this.defaultValue = defaultValue; this.base = defaultValue; }
        @Override public double defaultValue() { return defaultValue; }
        @Override public double baseValue() { return base; }
        @Override public void setBaseValue(double v) { base = v; }
    }

    @Test
    void speedIsAMultiplierOfTheTypesDefaultNotAnAbsolute() {
        FakeAttribute speed = new FakeAttribute(0.25);   // a wither skeleton's default
        MobBodies.applySpeed(speed, 0.6);
        assertEquals(0.25 * 0.6, speed.base, 0.0);
    }

    /** THE row a wrong implementation fails: base x multiplier compounds to 0.25 x 0.6 x 0.6 on the second call. */
    @Test
    void reapplyingTheSpeedDoesNotStack() {
        FakeAttribute speed = new FakeAttribute(0.25);
        MobBodies.applySpeed(speed, 0.6);
        MobBodies.applySpeed(speed, 0.6);
        assertEquals(0.25 * 0.6, speed.base, 0.0, "applied twice, still default x 0.6");
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
