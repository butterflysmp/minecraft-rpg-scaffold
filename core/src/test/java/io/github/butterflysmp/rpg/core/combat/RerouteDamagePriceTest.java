package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.From;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.Resolved;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The CHOICE between the two prices a rerouted hit can take: M16's flat mob price, or
 * {@link DamageScale}'s proportion. ONE of them, never both.
 *
 * <p><b>This file does take a max, and that is its job.</b> {@code MobDamagePricingTest} proves the mob
 * price has no max in it; this proves the choice does not reintroduce one by chaining the two. Every
 * mob row is asserted at max 100 AND max 400 -- at max 100 the flat price and the share-of-max price
 * are the same number (x5), so a row at 100 alone passes with the old pricing still in place.
 */
class RerouteDamagePriceTest {

    private static final double EPS = 1e-9;
    private static final Optional<Resolved> GS300_SKELETON =
            Optional.of(new Resolved(From.DIRECT, false, true, 300));

    @Test
    void aMobHitOnAPlayerIsFlatAtEveryMax() {
        // G9 as a unit row. Old pricing: 4 x 100/20 = 20, then 4 x 400/20 = 80. Stacked: 300, then 1,200.
        assertEquals(60, RerouteDamagePrice.of(4, GS300_SKELETON, true, 100, 20, true), EPS);
        assertEquals(60, RerouteDamagePrice.of(4, GS300_SKELETON, true, 400, 20, true), EPS,
                "the player's max must not move a mob's hit (M16). "
                        + "Mutation: the mob branch also applies DamageScale -> 300 / 1200 -> reddens");
    }

    @Test
    void anEnvironmentalHitOnAPlayerStaysAShareOfMax() {
        // A fall names no mob: DamageScale, unchanged -- "10% of max health regardless" (its javadoc).
        assertEquals(20, RerouteDamagePrice.of(4, Optional.empty(), true, 100, 20, true), EPS);
        assertEquals(80, RerouteDamagePrice.of(4, Optional.empty(), true, 400, 20, true), EPS);
    }

    @Test
    void aMobHitOnAMobStaysProportional() {
        // F12: a skeleton's arrow in a GS 300 zombie is priced by the VICTIM's bar (300 / 20), with no
        // attacker GS. The mob-to-mob pass decides otherwise, not this slice.
        assertEquals(60, RerouteDamagePrice.of(4, GS300_SKELETON, false, 300, 20, false), EPS);
        assertEquals(20, RerouteDamagePrice.of(4, GS300_SKELETON, false, 100, 20, false), EPS,
                "at victim max 100 the attacker's GS 300 must NOT appear (it would read 60)");
    }
}
