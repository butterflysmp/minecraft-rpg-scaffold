package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.From;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.MobFacts;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.Resolved;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M16: a mob's arrows, blasts and magic hit FLAT -- vanilla x 5 x GS/100, the same as its melee.
 *
 * <p><b>NO ROW IN THIS FILE TAKES A PLAYER MAX, AND THAT ABSENCE IS THE RULE.</b> The price replaces
 * {@code DamageScale}'s share-of-player-max conversion; a player max appearing here would be the old
 * pricing coming back. {@link #noPublicMethodCanTakeAPlayerMax} pins it structurally, because a row
 * can only show what a number IS, never what a signature cannot accept.
 */
class MobDamagePricingTest {

    private static final double EPS = 1e-9;

    private static MobFacts hostile(int gs) {
        return new MobFacts(false, true, OptionalInt.of(gs));
    }

    // --- price: flat, x5 x GS/100 --------------------------------------------------------------

    @Test
    void aGs300SkeletonsArrowIsFifteenTimesVanilla() {
        Resolved skeleton = new Resolved(From.DIRECT, false, true, 300);
        assertEquals(60, MobDamagePricing.price(4, skeleton), EPS, "4 x 5 x 3.00 (M16)");
        // G9's own number: applied / vanilla = 15, whatever the victim's max.
    }

    @Test
    void theGridIsTheMeleeGrid() {
        // M16's "the same as melee": one account of the rule, not two that agree today.
        for (int gs : new int[] {1, 100, 137, 300, 500}) {
            for (boolean custom : new boolean[] {false, true}) {
                assertEquals(MobScaling.attackDamage(7, custom, true, gs),
                        MobDamagePricing.price(7, new Resolved(From.CAUSING, custom, true, gs)), EPS,
                        "gs " + gs + " custom " + custom);
            }
        }
    }

    @Test
    void aPassiveSourceIsFiveTimesWithNoGs() {
        // A llama's spit: passive (M21), so x5 and no GS -- whatever number rode in the gs slot.
        assertEquals(10, MobDamagePricing.price(2, new Resolved(From.CAUSING, false, false, 500)), EPS);
    }

    @Test
    void aCustomSourceTakesNoVanillaFive() {
        assertEquals(20, MobDamagePricing.price(10, new Resolved(From.DIRECT, true, true, 200)), EPS, "M4");
    }

    @Test
    void anAbsentGsIsFiveTimesNeverOneAndNeverZero() {
        // The plan's third arm: "x5 with no GS, and a WARN line". Here it is the GS_BASELINE score.
        Resolved absent = MobDamagePricing.resolve(new MobFacts(false, true, OptionalInt.empty()), null, false)
                .orElseThrow();
        assertEquals(From.ABSENT, absent.from());
        assertEquals(15, MobDamagePricing.price(3, absent), EPS,
                "Mutation: the absent arm returns 0 -> reddens; returns x1 -> 3 -> reddens");
    }

    @Test
    void nonPositiveVanillaDealsNothing() {
        Resolved r = new Resolved(From.DIRECT, false, true, 300);
        assertEquals(0, MobDamagePricing.price(0, r), EPS);
        assertEquals(0, MobDamagePricing.price(-2, r), EPS);
        assertEquals(0, MobDamagePricing.price(Double.NaN, r), EPS);
    }

    // --- resolve: WHERE the score comes from -----------------------------------------------------

    @Test
    void theProjectilesStampBeatsTheShootersCurrentScore() {
        // The shooter re-rolled (or a /summon rewrote it) between launch and impact. The arrow was
        // fired at GS 300 and prices at 300. Mutation: causing before direct -> 140 -> reddens.
        Resolved r = MobDamagePricing.resolve(hostile(300), hostile(140), false).orElseThrow();
        assertEquals(From.DIRECT, r.from());
        assertEquals(300, r.gearScore());
    }

    @Test
    void aShooterThatIsGoneStillPricesAtItsStamp() {
        // G10: the skeleton is /kill-ed while the arrow flies, so the source names no causing entity.
        Resolved r = MobDamagePricing.resolve(hostile(300), null, false).orElseThrow();
        assertEquals(From.DIRECT, r.from());
        assertEquals(300, r.gearScore());
    }

    @Test
    void anUnstampedDirectEntityFallsToTheCausingMob() {
        // Evoker fangs are not a projectile and are not stamped; the evoker is the causing entity.
        Resolved r = MobDamagePricing.resolve(null, hostile(220), false).orElseThrow();
        assertEquals(From.CAUSING, r.from());
        assertEquals(220, r.gearScore());
    }

    @Test
    void anInvalidStampIsSkippedNotTrusted() {
        // Only a /summon ... BukkitValues can write 900 on an arrow; the causing mob's score wins.
        Resolved r = MobDamagePricing.resolve(hostile(900), hostile(140), false).orElseThrow();
        assertEquals(From.CAUSING, r.from());
        assertEquals(140, r.gearScore());
    }

    @Test
    void aPassiveCausingMobIsNotAbsent() {
        // A llama is passive by ruling, which is a decided x5 -- not a missing score to WARN about.
        Resolved r = MobDamagePricing.resolve(null, new MobFacts(false, false, OptionalInt.empty()), false)
                .orElseThrow();
        assertEquals(From.CAUSING, r.from());
        assertFalse(r.hostile());
    }

    @Test
    void aHostileCausingMobWithNoScoreIsAbsent() {
        Resolved r = MobDamagePricing.resolve(null, new MobFacts(true, true, OptionalInt.empty()), false)
                .orElseThrow();
        assertEquals(From.ABSENT, r.from());
        assertTrue(r.custom(), "custom-ness survives an absent score: a custom mob is still x1, not x5");
        assertEquals(MobScaling.GS_BASELINE, r.gearScore());
    }

    @Test
    void nothingMobShapedIsNotMobSourced() {
        // A fall, lava, a DoT tick (F5): no direct mob, no causing mob. DamageScale keeps these.
        assertEquals(Optional.empty(), MobDamagePricing.resolve(null, null, false));
    }

    @Test
    void aPlayerCauseIsNeverMobSourcedEvenThroughAStampedProjectile() {
        // A player batting a ghast's fireball back: the fireball still carries the ghast's stamp, but
        // the player is now its owner. Player-vs-player is a later rules decision, never M16's.
        assertEquals(Optional.empty(), MobDamagePricing.resolve(hostile(300), null, true));
    }

    // --- the structural half of "no player max" ------------------------------------------------

    @Test
    void noPublicMethodCanTakeAPlayerMax() {
        // price(vanilla, resolved) has exactly one double: the vanilla amount. A second double on any
        // public method is the only place a player max could enter.
        int checked = 0;
        for (Method m : MobDamagePricing.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) continue;
            long doubles = java.util.Arrays.stream(m.getParameterTypes()).filter(t -> t == double.class).count();
            assertTrue(doubles <= 1, m.getName() + " takes " + doubles + " doubles");
            checked++;
        }
        assertTrue(checked >= 2, "positive control: price and resolve were found, so the scan is not blind");
    }
}
