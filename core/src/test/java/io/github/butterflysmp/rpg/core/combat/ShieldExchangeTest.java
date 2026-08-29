package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.enchant.Bulwark;
import io.github.butterflysmp.rpg.core.enchant.Riposte;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE test this class was extracted to make possible.
 *
 * <p>Riposte's one load-bearing rule -- that it reflects a fraction of the PRE-MITIGATION blow -- used
 * to live entirely inside {@code RpgListeners.onMobMeleeAttack}, which cannot be unit-tested. The
 * natural way to write that rider is to reduce a local and then reflect, and reflecting off the
 * reduced local is a silent wrong answer worth 2.25 instead of 4.5 at the shipped numbers.
 *
 * <p>Because {@link ShieldExchange#of} derives BOTH numbers from one input, that mistake is now a
 * red test rather than a boot-gate observation. Every assertion below that names both
 * {@code applied} and {@code reflected} in the same breath is guarding exactly that.
 */
class ShieldExchangeTest {

    private static final double EPS = 1e-9;

    /** The shipped roundshield and the shipped mob. */
    private static final double MOB = 15.0;
    private static final double ROUNDSHIELD = 0.5;

    @Test
    void bothNumbersComeOffTheSameRawBlow() {
        // THE assertion. 7.5 reaches the player and 4.5 goes back, and the 4.5 is 30% of the RAW 15.0
        // -- not 30% of the 7.5 that got through. Reflecting off the reduced figure yields 2.25 and
        // reddens this line.
        ShieldExchange exchange = ShieldExchange.of(MOB, true, ROUNDSHIELD, 30);

        assertEquals(7.5, exchange.applied(), EPS, "half of a 15.0 hit reaches the player");
        assertEquals(4.5, exchange.reflected(), EPS,
                "30% of the RAW 15.0 goes back -- 2.25 here means it reflected off the pass-through");

        // Stated as the relationship rather than as two constants, so it survives a tuning change:
        // the reflect is a fraction of the input, and is NOT a fraction of the output.
        assertEquals(MOB * 0.30, exchange.reflected(), EPS);
        assertNotEquals(exchange.applied() * 0.30, exchange.reflected(), EPS);
    }

    @Test
    void theReflectIsUNCHANGEDByHowGoodTheShieldIs() {
        // The orthogonality, and the property the boot gate reads as "still 5 with Bulwark III on".
        // Sweep every block fraction: what reaches the player moves, what goes back does not.
        double previousApplied = Double.MAX_VALUE;
        for (double dr = 0.0; dr <= 1.0; dr += 0.05) {
            ShieldExchange exchange = ShieldExchange.of(MOB, true, dr, 30);

            assertEquals(4.5, exchange.reflected(), EPS,
                    "the reflect moved when block_dr changed to " + dr);
            assertTrue(exchange.applied() <= previousApplied + EPS,
                    "a better shield let more through at dr=" + dr);
            previousApplied = exchange.applied();
        }
    }

    @Test
    void bulwarkMovesWhatLandsAndLeavesWhatComesBackAlone() {
        // The same property stated through the real Bulwark composition rather than a bare fraction,
        // because that is the pair a player actually equips.
        for (double bulwark : new double[]{Bulwark.NONE, 5, 10, 15}) {
            double dr = Bulwark.effectiveDr(ROUNDSHIELD, bulwark);
            ShieldExchange exchange = ShieldExchange.of(MOB, true, dr, 30);

            assertEquals(4.5, exchange.reflected(), EPS,
                    "Bulwark " + bulwark + "% changed the reflect");
            assertEquals(Shield.applyBlock(MOB, dr), exchange.applied(), EPS);
        }

        // And the executed ladder the gate reads: Bulwark III blocks harder, Riposte III unchanged.
        assertEquals(5.25, ShieldExchange.of(MOB, true, Bulwark.effectiveDr(ROUNDSHIELD, 15), 30)
                .applied(), EPS);
        assertEquals(4.5, ShieldExchange.of(MOB, true, Bulwark.effectiveDr(ROUNDSHIELD, 15), 30)
                .reflected(), EPS);
    }

    @Test
    void anUnblockedHitLandsWholeAndSendsNothingBack() {
        // ONE arm covers every not-blocked reason: vanilla did not block, a hit from behind, an
        // untagged or dangling shield, and a BROKEN one. None of them may reflect -- which is the
        // "one predicate, all three effects" property the shield gate was built around.
        ShieldExchange exchange = ShieldExchange.of(MOB, false, ROUNDSHIELD, 30);

        assertEquals(MOB, exchange.applied(), "an unblocked hit is not reduced");
        assertEquals(Riposte.NONE, exchange.reflected(), "and nothing goes back");
        assertFalse(Riposte.reflects(exchange.reflected()));
    }

    @Test
    void aBlockedHitWithNoRiposteSendsNothingBackButIsStillReduced() {
        // Every shield shipped before this slice. The block must be untouched by Riposte's arrival.
        ShieldExchange exchange = ShieldExchange.of(MOB, true, ROUNDSHIELD, Riposte.NONE);

        assertEquals(7.5, exchange.applied(), EPS);
        assertEquals(Riposte.NONE, exchange.reflected());
    }

    @Test
    void aTotalBlockStillReflectsOffTheWholeBlow() {
        // The sharpest statement of pre-mitigation: nothing at all got through, and the reflect is
        // still 30% of the blow. Under the rejected reading this would be exactly 0.
        ShieldExchange exchange = ShieldExchange.of(MOB, true, Shield.FULL, 30);

        assertEquals(0.0, exchange.applied(), EPS, "a full block stops everything");
        assertEquals(4.5, exchange.reflected(), EPS,
                "and the reflect is unchanged -- 0.0 here means it reflected off the pass-through");
    }

    @Test
    void theGuardsAreInheritedRatherThanRestated() {
        // Shield.clamp and Riposte's formula are unchanged and separately tested; this only fixes
        // what they are fed. Pinned so a future edit cannot quietly add a second clamp here.
        assertEquals(MOB, ShieldExchange.of(MOB, true, -1000, 0).applied(), EPS);
        assertEquals(0.0, ShieldExchange.of(MOB, true, 2.0, 0).applied(), EPS);

        // A negative blow reflects negative -- and the RIDER is what refuses to deal it, via
        // Riposte.reflects. The exchange reports honestly rather than clamping here.
        assertTrue(ShieldExchange.of(-20.0, true, ROUNDSHIELD, 30).reflected() < 0);
        assertFalse(Riposte.reflects(ShieldExchange.of(-20.0, true, ROUNDSHIELD, 30).reflected()));
    }
}
