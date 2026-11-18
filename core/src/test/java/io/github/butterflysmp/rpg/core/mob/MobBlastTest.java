package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phage's blast (PLAN-dragon-mobs.md step R3). Ben: "for the explosion keep vanilla creeper numbers for
 * everything except the damage", and, on its shape, "keep vanillas shap". The seat's provisional P-a and P-b: 20 at
 * stage 1, 50 at stage 5, linear between, times gear score / 100, as the damage at ZERO distance; vanilla's distance
 * curve, normalised, scales it out to the reach. Victims are "Only players and Summons" (Ben, G), through the one
 * named predicate {@link MobHostility}.
 */
class MobBlastTest {

    private static final MobStages STAGES =
            new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160));
    private static final MobBlast BLAST = new MobBlast(StagedDistance.byHeight(6), StagedDistance.byHeight(3),
            StagedDistance.byHeight(7), 30, 20.0, 50.0, 1.25, 1.0, "void_implosion");
    private static final Vec3 AT = new Vec3(0, 64, 0);
    private static final UUID SELF = UUID.randomUUID();

    // --- the damage table, at zero distance ------------------------------------------------------------------------

    /**
     * THE TABLE, every stage, the whole grid: 20, 27.5, 35, 42.5, 50. Each value is 20 + 30 x (stage - 1) / 4, and
     * every step is a quarter, which a double holds exactly; the test runs the expression rather than trusting that.
     */
    @Test
    void theDamageStepsEvenlyFromTheFirstStageToTheLast() {
        double[] expected = {20.0, 27.5, 35.0, 42.5, 50.0};
        for (int stage = 1; stage <= 5; stage++) {
            assertEquals(expected[stage - 1], BLAST.damageAt(stage, STAGES), 0.0, "stage " + stage);
        }
    }

    @Test
    void aStageOutsideTheMobsStagesIsRefusedNotClamped() {
        assertThrows(IllegalArgumentException.class, () -> BLAST.damageAt(0, STAGES));
        assertThrows(IllegalArgumentException.class, () -> BLAST.damageAt(6, STAGES));
    }

    /**
     * P-b: x gear score / 100, as its health is, and never x 5 (a custom mob, M4). The same product
     * {@link MobScaling#attackDamage} gives a custom mob's melee, not a second copy of it.
     */
    @Test
    void theDamageIsPricedByGearScoreAsACustomMobsHitIs() {
        assertEquals(20.0, BLAST.price(1, STAGES, true, 100), 1e-9, "stage 1 at GS 100");
        assertEquals(60.0, BLAST.price(1, STAGES, true, 300), 1e-9, "stage 1 at GS 300: x3, not x15");
        assertEquals(150.0, BLAST.price(5, STAGES, true, 300), 1e-9, "stage 5 at GS 300");
        assertEquals(87.5, BLAST.price(3, STAGES, true, 250), 1e-9, "stage 3 at GS 250");
        assertEquals(MobScaling.attackDamage(42.5, true, true, 400), BLAST.price(4, STAGES, true, 400), 0.0,
                "the one pricing rule, not a copy");
    }

    // --- vanilla's curve, normalised -------------------------------------------------------------------------------

    /**
     * THE CURVE AGAINST THE JAR. {@code ExplosionDamageCalculator#getEntityDamageAmount} (server jar, build 74) returns
     * {@code (x*x + x) / 2 * 7 * f + 1} with {@code x = (1 - d/f) * exposure} and {@code f = 2 x power}. At exposure 1,
     * with the {@code + 1} and the {@code * 7 * f} taken away, the shape is {@code (x*x + x) / 2}: 1 at zero distance,
     * 0 at the reach. Every expected value below was EXECUTED in jshell from the jar's expression at power 3
     * (f = 6), {@code (vanilla(d) - 1) / (7 * f)}: 1.000000000, 0.656250000, 0.555555571 (float), 0.375000000,
     * 0.156250000, 0.008472221 (float), 0.000000000. Ours is the double form of the same, so the float rows are
     * compared to their exact value.
     */
    @Test
    void theFalloffIsVanillasCurveNormalisedToOneAtZeroAndZeroAtTheReach() {
        assertEquals(1.0, MobBlast.falloff(0.0, 6.0), 0.0, "zero distance: the full damage");
        assertEquals(0.65625, MobBlast.falloff(1.5, 6.0), 1e-12, "a quarter of the reach");
        assertEquals(5.0 / 9.0, MobBlast.falloff(2.0, 6.0), 1e-12, "a third");
        assertEquals(0.375, MobBlast.falloff(3.0, 6.0), 1e-12, "HALF THE REACH: three eighths");
        assertEquals(0.15625, MobBlast.falloff(4.5, 6.0), 1e-12, "three quarters");
        assertEquals(0.0084722222222222, MobBlast.falloff(5.9, 6.0), 1e-12, "just inside the reach");
        assertEquals(0.0, MobBlast.falloff(6.0, 6.0), 0.0, "at the reach: nothing");
        assertEquals(0.0, MobBlast.falloff(6.05, 6.0), 0.0, "beyond the reach: nothing, never negative");
    }

    /** The curve is a SHAPE, so it scales with the reach: half of any reach is three eighths. */
    @Test
    void theCurveFollowsTheReach() {
        assertEquals(0.375, MobBlast.falloff(5.0, 10.0), 1e-12);
        assertEquals(0.65625, MobBlast.falloff(0.75, 3.0), 1e-12);
    }

    // --- the reach -------------------------------------------------------------------------------------------------

    /** INCLUSIVE at exactly the reach, as vanilla's {@code d / f > 1} skip is; not just beyond. FEET to FEET. */
    @Test
    void exactlyTheReachIsInsideAndJustBeyondIsNot() {
        assertTrue(MobBlast.inReach(AT, new Vec3(6.0, 64, 0), 6.0));
        assertFalse(MobBlast.inReach(AT, new Vec3(6.0001, 64, 0), 6.0));
        assertFalse(MobBlast.inReach(AT, new Vec3(6.05, 64, 0), 6.0));
    }

    /** A sphere, not a cube: (4, 4, 4) is 6.93 away. Height counts like any axis. */
    @Test
    void theReachIsASphere() {
        assertFalse(MobBlast.inReach(AT, new Vec3(4, 68, 4), 6.0));
        assertTrue(MobBlast.inReach(AT, new Vec3(0, 70, 0), 6.0));
        assertFalse(MobBlast.inReach(AT, new Vec3(0, 70.01, 0), 6.0));
    }

    // --- the predicate (N3) ----------------------------------------------------------------------------------------

    @Test
    void mobsAreHostileToPlayersOnly() {
        assertTrue(MobHostility.hostileTo(MobHostility.Side.PLAYER, false), "a survival player");
        assertFalse(MobHostility.hostileTo(MobHostility.Side.MOB, false), "a mob, the Phage's own kind included");
        assertFalse(MobHostility.hostileTo(MobHostility.Side.PLAYER, true), "a creative or spectator player");
    }

    // --- who the blast reaches, and how hard -----------------------------------------------------------------------

    private static MobBlast.Bystander player(double x) {
        return new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.PLAYER, false, "world", new Vec3(x, 64, 0));
    }

    @Test
    void theBlastHitsPlayersInsideTheReachAndSparesEveryoneElseInside() {
        MobBlast.Bystander near = player(3.0);
        MobBlast.Bystander edge = player(6.0);
        MobBlast.Bystander beyond = player(6.05);
        MobBlast.Bystander mob = new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.MOB, false, "world",
                new Vec3(0, 64, 1));
        MobBlast.Bystander creative = new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.PLAYER, true,
                "world", new Vec3(0, 64, -2));
        MobBlast.Bystander nether = new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.PLAYER, false,
                "world_nether", AT);
        MobBlast.Bystander itself = new MobBlast.Bystander(SELF, MobHostility.Side.MOB, false, "world", AT);

        MobBlast.Reach reach = MobBlast.reach(SELF, "world", AT, 6.0,
                List.of(near, mob, edge, beyond, creative, nether, itself));

        assertEquals(List.of(new MobBlast.Hit(near, 3.0, 0.375), new MobBlast.Hit(edge, 6.0, 0.0)), reach.victims(),
                "players inside the reach, the edge included (for nothing), with each one's distance and factor");
        assertEquals(List.of(new MobBlast.Hit(mob, 1.0, MobBlast.falloff(1.0, 6.0)),
                        new MobBlast.Hit(creative, 2.0, MobBlast.falloff(2.0, 6.0))), reach.spared(),
                "inside, and not a victim: named with the factor they were spared, never hurt");
    }

    @Test
    void anEmptyReachReachesNobody() {
        MobBlast.Reach reach = MobBlast.reach(SELF, "world", AT, 6.0, List.of(player(7.0)));
        assertTrue(reach.victims().isEmpty());
        assertTrue(reach.spared().isEmpty());
    }

    // --- the numbers are content -----------------------------------------------------------------------------------
    /** A blast with the Phage's distances (6, 3, 7 at the last stage, each earlier stage's by its height). */
    private static MobBlast blast(double reach, double first, double last, int fuse, double pull, double lift,
                                  String visual) {
        return new MobBlast(StagedDistance.byHeight(reach), StagedDistance.byHeight(3), StagedDistance.byHeight(7),
                fuse, first, last, pull, lift, visual);
    }

    @Test
    void aBadBlastIsRefusedByName() {
        assertThrows(IllegalArgumentException.class, () -> blast(0, 20, 50, 30, 1.0, 1, "v"), "reach 0");
        assertThrows(IllegalArgumentException.class, () -> blast(Double.NaN, 20, 50, 30, 1.0, 1, "v"), "reach NaN");
        assertThrows(IllegalArgumentException.class, () -> blast(6, -1, 50, 30, 1.0, 1, "v"), "negative damage");
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, Double.POSITIVE_INFINITY, 30, 1.0, 1, "v"));
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, 50, 0, 1.0, 1, "v"), "a fuse of 0 ticks");
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, 50, 30, -0.5, 1, "v"), "a negative pull");
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, 50, 30, Double.NaN, 1, "v"), "pull NaN");
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, 50, 30, 1.0, -1, "v"), "a negative lift");
        assertEquals(0.0, blast(6, 20, 50, 30, 0.0, 1, "v").pull(), 0.0, "0 is allowed: it turns the pull off");
        assertEquals(0.0, blast(6, 20, 50, 30, 1.0, 0, "v").lift(), 0.0, "0 is allowed: it keeps them grounded");
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, 50, 30, 1.0, 1, null), "no visual");
        assertThrows(IllegalArgumentException.class, () -> blast(6, 20, 50, 30, 1.0, 1, " "), "a blank visual");
        assertThrows(IllegalArgumentException.class, () -> new MobBlast(StagedDistance.byHeight(6), null,
                StagedDistance.byHeight(7), 30, 60, 150, 1.25, 1, "v"), "no trigger");
    }

    /** A blast prices by STAGE, so it needs stages, as a split does. */
    @Test
    void aBlastNeedsStages() {
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("p", "creeper", "P", 80,
                MobBody.VANILLA, "void", null, null, null, BLAST));
        MobDefinition staged = new MobDefinition("p", "creeper", "P", 80, MobBody.VANILLA, "void", STAGES, null, null,
                BLAST);
        assertEquals(BLAST, staged.blast());
        assertEquals(null, new MobDefinition("k", "wither_skeleton", "K", 360).blast(), "absent by default");
    }

    /** A cancel that is not beyond its trigger at some stage would reset the tick it starts: refused at the definition. */
    @Test
    void aCancelNotBeyondTheTriggerAtAnyStageIsRefused() {
        MobBlast inverted = new MobBlast(StagedDistance.byHeight(6), StagedDistance.byHeight(7),
                StagedDistance.byHeight(3), 30, 60, 150, 1.25, 1, "v");
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("p", "creeper", "P", 80,
                MobBody.VANILLA, "void", STAGES, null, null, inverted));
        // Beyond at the last stage but not the first: a tuned first stage (cancel 1, trigger 1.5) is caught too.
        MobBlast tuned = new MobBlast(StagedDistance.byHeight(6), new StagedDistance(1.5, 3.0),
                new StagedDistance(1.0, 7.0), 30, 60, 150, 1.25, 1, "v");
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("p", "creeper", "P", 80,
                MobBody.VANILLA, "void", STAGES, null, null, tuned));
    }

    // --- the distances by stage (the seat's ruling 1 on #204; StagedDistanceTest holds the rule) -----------------------

    @Test
    void theReachTriggerAndCancelFollowTheStageHeight() {
        MobBlast b = blast(6, 60, 150, 30, 1.25, 1, "void_implosion");
        double[][] want = {{2, 1, 7.0 / 3}, {3, 1.5, 3.5}, {4, 2, 14.0 / 3}, {5, 2.5, 35.0 / 6}, {6, 3, 7}};
        for (int s = 1; s <= 5; s++) {
            assertEquals(want[s - 1][0], b.reachAt(s, STAGES), 1e-12, "reach at stage " + s);
            assertEquals(want[s - 1][1], b.triggerAt(s, STAGES), 1e-12, "trigger at stage " + s);
            assertEquals(want[s - 1][2], b.cancelAt(s, STAGES), 1e-12, "cancel at stage " + s);
        }
    }

    // --- the PULL, the void implosion (core.shape.Implosion) --------------------------------------------------------

    /**
     * THE BLAST IS ONE USE OF THE SHAPE, AT ITS STAGE: the stage's reach, vanilla's explosion falloff, the pull, the
     * lift, and the explosion knockback resistance. A stage-1 blast's implosion reaches 2; a stage 5's, 6.
     */
    @Test
    void theBlastsImplosionIsTheStagesReachWithThePullTheLiftAndTheExplosionResistance() {
        MobBlast b = blast(6, 60, 150, 30, 1.25, 1, "void_implosion");
        Implosion five = b.implosion(5, STAGES);
        assertEquals(6.0, five.reach(), 1e-12);
        assertEquals(Implosion.Falloff.VANILLA_EXPLOSION, five.falloff());
        assertEquals(1.25, five.pull(), 0.0);
        assertEquals(1.0, five.lift(), 0.0);
        assertEquals(Implosion.Resistance.EXPLOSION_KNOCKBACK, five.resistance());
        assertEquals(2.0, b.implosion(1, STAGES).reach(), 1e-12, "stage 1's reach, its own");
        assertEquals(0.5, blast(6, 60, 150, 30, 0.5, 0, "v").implosion(5, STAGES).pull(), 0.0, "its OWN pull");
        assertEquals(0.0, blast(6, 60, 150, 30, 0.5, 0, "v").implosion(5, STAGES).lift(), 0.0, "its OWN lift");
    }

    /** The reach rule takes the stage's reach: a stage-1 Phage's sphere is 2 blocks. */
    @Test
    void aStageOnesBlastReachesTwo() {
        MobBlast.Reach two = MobBlast.reach(SELF, "world", AT, BLAST.reachAt(1, STAGES), List.of(player(2.0)));
        assertEquals(1, two.victims().size());
        assertEquals(0.0, two.victims().get(0).factor(), 0.0, "at the reach: listed for nothing");
        assertTrue(MobBlast.reach(SELF, "world", AT, BLAST.reachAt(1, STAGES), List.of(player(2.05))).victims()
                .isEmpty());
    }

    /** The falloff the BLAST line prints is the shape's, one home. */
    @Test
    void theBlastsFalloffIsTheShapesVanillaShare() {
        for (double d : new double[]{0, 1.5, 3, 5.9, 6, 7}) {
            assertEquals(Implosion.vanillaShare(d, 6.0), MobBlast.falloff(d, 6.0), 0.0, "d=" + d);
        }
    }
}
