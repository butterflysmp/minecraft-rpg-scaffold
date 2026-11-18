package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The BLAST line's exact text (dragon-mobs step R3; since the void implosion of 2026-11-17, the PULL, and since the
 * seat's ruling 2 on #204, its LIFT, the flight model's travel factor, the apex, the rest point and the safe landing).
 * A gate predicts this line verbatim, so it is pinned here rather than read off a boot. Pure: no server.
 */
class MobBlastLineTest {

    private static final UUID PHAGE = UUID.fromString("4c30b659-a0dd-4c32-a998-e4e45e8c49c7");
    private static final UUID BEN = UUID.fromString("b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5");
    private static final UUID CHILD = UUID.fromString("50302ad8-568e-4e27-9496-ff7666f001fc");

    /** The head names the stage's reach, the pull and the lift multipliers, and the visual. */
    @Test
    void theHeadNamesTheStagesReachThePullTheLiftAndTheVisual() {
        assertEquals("BLAST mob=phage id=" + PHAGE + " stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00"
                        + " visual=void_implosion",
                MobBlaster.head("phage", PHAGE, 1, 100, 60.0, 2.0, 1.25, 1.0, "void_implosion"));
    }

    /**
     * A VICTIM's entry, every field from a hand-built pull whose fields are ALL DIFFERENT, so a transposition between
     * any two printed fields is red: d 2.0, h 1.6 (the cap's), resistance 0.25, travel factor 5.0, strength 0.6, cap
     * 0.22, applied 0.22 (capped), lift 0.4, apex 1.25, so the rest point is 0.22 x 5 = 1.1.
     */
    @Test
    void aVictimsEntryCarriesTheDamageThePullTheLiftAndTheRestPoint() {
        Implosion.Pull p = new Implosion.Pull(2.0, 1.6, 0.555556, 0.5, 0.45, 0.35, 0.6, 0.4, 0.25, true, 5.0, 0.22,
                0.22, true, 1.25, new Vec3(-0.22, 0.4, 0));
        assertEquals("player " + BEN + " d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=0.250 h=1.600"
                        + " ground=true tf=5.000000 strength=0.600000 cap=0.220000 pull=0.220000 capped=true"
                        + " lift=0.400000 apex=1.250 rest=1.100 safe=false",
                MobBlaster.entry("player", BEN, 2.0, 0.5555555555555556, 33.33333333333333, 0.6666666666666667, p));
    }

    /** The real shape at d = 2 on stone (ImplosionTest's figures): the rest point is exactly 1.5, half a block short. */
    @Test
    void theRealPullAtTwoBlocksPrintsItsRestPointHalfABlockShort() {
        Implosion shape = new Implosion(6, Implosion.Falloff.VANILLA_EXPLOSION, 1.25, 1.0,
                Implosion.Resistance.EXPLOSION_KNOCKBACK);
        Implosion.Pull p = shape.pull(new Vec3(0, 64, 0),
                new Implosion.Footing(new Vec3(2, 64, 0), 1.62, true, 0.6f, 0.0)).orElseThrow();
        assertEquals("player " + BEN + " d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=0.000 h=2.000"
                        + " ground=true tf=5.343010 strength=0.647553 cap=0.280741 pull=0.280741 capped=true"
                        + " lift=0.419614 apex=1.250 rest=1.500 safe=false",
                MobBlaster.entry("player", BEN, 2.0, 0.5555555555555556, 33.33333333333333, 0.6666666666666667, p));
    }

    /** A victim exactly at the reach has no pull: every pull field prints as 0. */
    @Test
    void aVictimWithNoPullPrintsZeros() {
        assertEquals("player " + BEN + " d=6.000 factor=0.000000 dealt=0.000 knock=0.000000 res=0.000 h=0.000"
                        + " ground=false tf=0.000000 strength=0.000000 cap=0.000000 pull=0.000000 capped=false"
                        + " lift=0.000000 apex=0.000 rest=0.000 safe=false",
                MobBlaster.entry("player", BEN, 6.0, 0.0, 0.0, 0.0, null));
    }

    /** A SPARED creature's entry carries no dealt and no pull: it is neither hurt nor pulled. */
    @Test
    void aSparedEntryCarriesNeitherDamageNorPull() {
        assertEquals("creeper/phage " + CHILD + " d=0.737 factor=0.823294",
                MobBlaster.spared("creeper/phage", CHILD, 0.737, 0.823294));
    }
}
