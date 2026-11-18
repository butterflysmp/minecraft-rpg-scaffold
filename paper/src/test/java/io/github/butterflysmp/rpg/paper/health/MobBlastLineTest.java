package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The BLAST line's exact text (dragon-mobs step R3; the push of the follow-up of 2026-11-17; and, since the void
 * implosion of the same day, the PULL that replaced it). A gate predicts this line verbatim, so it is pinned here rather
 * than read off a boot. Pure: no server.
 */
class MobBlastLineTest {

    private static final UUID PHAGE = UUID.fromString("4c30b659-a0dd-4c32-a998-e4e45e8c49c7");
    private static final UUID BEN = UUID.fromString("b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5");
    private static final UUID CHILD = UUID.fromString("50302ad8-568e-4e27-9496-ff7666f001fc");

    /** The head names the pull multiplier and the visual, after the reach. */
    @Test
    void theHeadNamesThePullMultiplierAndTheVisual() {
        assertEquals("BLAST mob=phage id=" + PHAGE + " stage=1 gs=100 base=60.000 reach=6.00 pull=1.25"
                        + " visual=void_implosion",
                MobBlaster.head("phage", PHAGE, 1, 100, 60.0, 6.0, 1.25, "void_implosion"));
    }

    /**
     * A VICTIM's entry: its distance, the damage's factor and the damage dealt (before defence), then the pull: the
     * linear factor ({@code knock}, 1 - d / reach), the victim's explosion knockback resistance, its HORIZONTAL distance
     * ({@code h}, the cap's), whether it stood on the ground, the per-tick multiplier the model used ({@code m}), the
     * uncapped {@code strength}, the {@code cap}, the speed applied ({@code pull}) and whether the cap bound. Distinct
     * figures in every field that can differ, so a transposition between two of them is red: a victim 1.2 blocks above
     * the Phage at d = 2 stands 1.6 away horizontally, so its cap, (1.6 - 0.5) x 0.454, is not the flat d = 2 one.
     */
    @Test
    void aCappedVictimsEntryCarriesTheDamageAndThePullAndTheCap() {
        assertEquals("player " + BEN + " d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=0.000 h=1.600 ground=true"
                        + " m=0.546000 strength=0.833333 cap=0.499400 pull=0.499400 capped=true",
                MobBlaster.entry("player", BEN, 2.0, 0.5555555555555556, 33.33333333333333, 0.6666666666666667, 0.0, 1.6,
                        true, 0.546000063419342, 0.8333333333333334, 0.4993999302387238, 0.4993999302387238, true));
    }

    /** Uncapped (d = 3, res 0.5, on stone): the strength is applied whole, so {@code pull} equals {@code strength}. */
    @Test
    void anUncappedVictimsEntryAppliesTheStrengthWhole() {
        assertEquals("player " + BEN + " d=3.000 factor=0.375000 dealt=22.500 knock=0.500000 res=0.500 h=3.000 ground=true"
                        + " m=0.546000 strength=0.312500 cap=1.135000 pull=0.312500 capped=false",
                MobBlaster.entry("player", BEN, 3.0, 0.375, 22.5, 0.5, 0.5, 3.0, true, 0.546000063419342, 0.3125,
                        1.134999841452, 0.3125, false));
    }

    /** In the air (d = 3, res 0.5) the air's weaker drag gives a tighter cap, and it binds. */
    @Test
    void anAirborneVictimIsCappedByTheAirsWeakerDrag() {
        assertEquals("player " + BEN + " d=3.000 factor=0.375000 dealt=22.500 knock=0.500000 res=0.500 h=3.000 ground=false"
                        + " m=0.910000 strength=0.312500 cap=0.225000 pull=0.225000 capped=true",
                MobBlaster.entry("player", BEN, 3.0, 0.375, 22.5, 0.5, 0.5, 3.0, false, 0.9100000262260437, 0.3125,
                        0.224999934435, 0.224999934435, true));
    }

    /** A SPARED creature's entry carries no dealt and no pull: it is neither hurt nor pulled. */
    @Test
    void aSparedEntryCarriesNeitherDamageNorPull() {
        assertEquals("creeper/phage " + CHILD + " d=0.737 factor=0.823294",
                MobBlaster.entry("creeper/phage", CHILD, 0.737, 0.823294, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN, false, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false));
    }
}
