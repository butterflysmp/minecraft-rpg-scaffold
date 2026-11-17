package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The BLAST line's exact text (dragon-mobs step R3, and the follow-up of 2026-11-17, which adds the push). A gate
 * predicts this line verbatim, so it is pinned here rather than read off a boot. Pure: no server.
 */
class MobBlastLineTest {

    private static final UUID PHAGE = UUID.fromString("4c30b659-a0dd-4c32-a998-e4e45e8c49c7");
    private static final UUID BEN = UUID.fromString("b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5");
    private static final UUID CHILD = UUID.fromString("50302ad8-568e-4e27-9496-ff7666f001fc");

    /** The head names the multiplier the blast pushes with, after the reach. */
    @Test
    void theHeadNamesTheKnockbackMultiplier() {
        assertEquals("BLAST mob=phage id=" + PHAGE + " stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00",
                MobBlaster.head("phage", PHAGE, 1, 100, 60.0, 6.0, 1.0));
    }

    /**
     * A VICTIM's entry: its distance, the damage's factor and the damage dealt (before defence), then the push's
     * factor ({@code knock}, 1 - d / reach), the victim's explosion knockback resistance and the strength applied
     * ({@code push}). Distinct figures in every field, so a transposition between two of them is red.
     */
    @Test
    void aVictimsEntryCarriesTheDamageAndThePush() {
        assertEquals("player " + BEN + " d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=0.250 push=0.500000",
                MobBlaster.entry("player", BEN, 2.0, 0.5555555555555556, 33.33333333333333, 0.6666666666666667, 0.25,
                        0.5));
    }

    /** A SPARED creature's entry carries no dealt and no push: it is neither hurt nor pushed. */
    @Test
    void aSparedEntryCarriesNeitherDamageNorPush() {
        assertEquals("creeper/phage " + CHILD + " d=0.737 factor=0.823294",
                MobBlaster.entry("creeper/phage", CHILD, 0.737, 0.823294, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN));
    }
}
