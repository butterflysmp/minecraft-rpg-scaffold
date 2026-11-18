package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN'S BLINK RANGE (step R8, ruled 2026-11-17: "Say 12-24 blocks", replacing X5's 6 to 16), and what it does to the
 * Phalanx's next shot. The shipped numbers: blink trigger 5, near 12, far 24, level tolerance 3; shot acquire 30,
 * re-acquire 35.
 */
class MobBlinkRangeTest {

    private static MobBlink blink() {
        return new MobBlink(5, 12, 24, 8, 3, List.of(), "squid_ink", 12, 240);
    }

    private static MobShot shot() {
        return new MobShot(120, 30, 35, 0.2, 0.0513, 240, 35, 1.2, 40,
                new MobShot.ShotLook("amethyst_block", 0.4, 0.8, 0xC88CFF, 0xC88CFF, 1.5, 0xFF69B4, 2.0, 4),
                List.of(), List.of());
    }

    /** A generator whose every nextDouble is {@code v}. */
    private static RandomGenerator always(double v) {
        return new RandomGenerator() {
            @Override public long nextLong() { throw new UnsupportedOperationException(); }
            @Override public double nextDouble() { return v; }
        };
    }

    /**
     * THE DRAW CANNOT LAND NEARER THAN 12, BY CONSTRUCTION: the uniform source is near + u x (far - near) with u in
     * [0, 1). Its two ends, and 100 000 draws from a seeded generator.
     */
    @Test
    void theUniformDrawIsTwelveToTwentyFourAndNeverNearer() {
        MobBlink b = blink();
        assertEquals(12.0, MobBlink.uniform(always(0.0)).next(b).distance(), 1e-12);
        double top = MobBlink.uniform(always(Math.nextDown(1.0))).next(b).distance();
        assertTrue(top <= 24.0 && top > 23.999999, "the far end, both ends included (12 + nextDown(1) x 12 rounds to 24.0, measured): " + top);
        assertEquals(18.0, MobBlink.uniform(always(0.5)).next(b).distance(), 1e-12);
        SplittableRandom r = new SplittableRandom(20261117L);
        MobBlink.Source s = MobBlink.uniform(r);
        double min = Double.MAX_VALUE;
        double max = 0;
        for (int i = 0; i < 100_000; i++) {
            double d = s.next(b).distance();
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        assertTrue(min >= 12.0 && max <= 24.0, min + " to " + max);
    }

    /**
     * HOW FAR THE TRIGGER CAN BE FROM THE PHALANX'S EYE AFTER A BLINK. A combatant triggers when its box grown by 5
     * holds the Phalanx's feet: a player (0.6 wide, 1.8 tall, eye 1.62: READ, EntityType) is then within 5.3 on x and
     * z, and his feet within 6.8 below to 5 above. The landing is up to {@code far} out horizontally, and within 3
     * whole blocks of floor(feet.y). The Phalanx's eye is its feet + 2.1 (READ from the boot of 45bc33ce: every
     * {@code SHOT fired} line's {@code from} is its feet + 2.100). The worst case is the far landing directly away
     * from his far corner, at the height that most separates the two eyes.
     *
     * <p>At X5's 16 that is 25.6, inside the acquire range of 30: the Phalanx always had a target after a blink. AT BEN'S
     * 24 IT IS 33.1, OUTSIDE 30: it could blink to where no body is in its acquire range.
     */
    @Test
    void afterABlinkTheTriggerCanBeThirtyThreeAwayAt24AndTwentySixAt16() {
        assertEquals(25.6, worst(16), 0.05);
        assertEquals(33.1, worst(24), 0.05);
        MobShot s = shot();
        assertTrue(worst(16) <= s.acquireRange(), "at 16 the acquire range always held it");
        assertTrue(worst(24) > s.acquireRange(), "at 24 it does not");
        assertTrue(worst(24) <= s.reacquireRange(), "the re-acquire range does: " + worst(24));
    }

    private static double worst(double far) {
        double horizontal = far + (5 + 0.3) * Math.sqrt(2);
        double up = (5 + 1.62) - (-1 - 3 + 2.1);               // his eye at his highest, its eye at its lowest
        double down = (3 + 2.1) - (-6.8 + 1.62);               // its eye at its highest, his at his lowest
        return Math.hypot(horizontal, Math.max(up, down));
    }

    /**
     * SO A PHALANX THAT HAS BLINKED KEEPS A TARGET (step R8, CHOSEN, put to Ben): from a blink that lands until its next
     * shot, who it may fire at is measured out to the RE-ACQUIRE range, 35, not the acquire range, 30. 35 holds the
     * worst case above with 1.9 to spare. Its first shot after the blink puts it back on 30.
     */
    @Test
    void untilItsNextShotABlinkedPhalanxChoosesOutToTheReacquireRange() {
        MobShot s = shot();
        assertEquals(30.0, s.chooseRange(false), 1e-12);
        assertEquals(35.0, s.chooseRange(true), 1e-12);
        io.github.butterflysmp.rpg.core.Vec3 eye = new io.github.butterflysmp.rpg.core.Vec3(0, 65.1, 0);
        io.github.butterflysmp.rpg.core.Vec3 at33 = new io.github.butterflysmp.rpg.core.Vec3(33, 65.1, 0);
        io.github.butterflysmp.rpg.core.Vec3 at36 = new io.github.butterflysmp.rpg.core.Vec3(36, 65.1, 0);
        assertTrue(!s.inChooseRange(eye, at33, false) && s.inChooseRange(eye, at33, true));
        assertTrue(!s.inChooseRange(eye, at36, true), "never past 35");
        // A re-acquire range authored below the acquire range never SHRINKS the choice.
        MobShot odd = new MobShot(120, 30, 20, 0.2, 0.0513, 240, 35, 1.2, 40, s.look(), List.of(), List.of());
        assertEquals(30.0, odd.chooseRange(true), 1e-12);
    }
}
