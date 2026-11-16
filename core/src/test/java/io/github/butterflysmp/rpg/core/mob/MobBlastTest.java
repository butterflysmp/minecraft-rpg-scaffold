package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phage's blast (PLAN-dragon-mobs.md step R3). Ben: "for the explosion keep vanilla creeper numbers for
 * everything except the damage". The seat's provisional P-a to P-c: 20 at stage 1, 50 at stage 5, linear between;
 * times gear score / 100; full damage to every victim inside the radius. Victims are "Only players and Summons"
 * (Ben, G), through the one named predicate {@link MobHostility}.
 */
class MobBlastTest {

    private static final MobStages STAGES =
            new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160));
    private static final MobBlast BLAST = new MobBlast(3.0, 20.0, 50.0, 30);
    private static final Vec3 AT = new Vec3(0, 64, 0);
    private static final UUID SELF = UUID.randomUUID();

    // --- the damage table ------------------------------------------------------------------------------------------

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

    // --- the radius ------------------------------------------------------------------------------------------------

    /** INCLUSIVE at exactly 3.0, and not just beyond. FEET to FEET, a sphere. */
    @Test
    void exactlyThreeBlocksIsInsideAndJustBeyondIsNot() {
        assertTrue(BLAST.inRadius(AT, new Vec3(3.0, 64, 0)));
        assertFalse(BLAST.inRadius(AT, new Vec3(3.0001, 64, 0)));
        assertFalse(BLAST.inRadius(AT, new Vec3(3.05, 64, 0)));
    }

    /** A sphere, not a cube: (2, 2, 2) is 3.46 away. Height counts like any axis. */
    @Test
    void theRadiusIsASphere() {
        assertFalse(BLAST.inRadius(AT, new Vec3(2, 66, 2)));
        assertTrue(BLAST.inRadius(AT, new Vec3(0, 67, 0)));
        assertFalse(BLAST.inRadius(AT, new Vec3(0, 67.01, 0)));
    }

    // --- the predicate (N3) ----------------------------------------------------------------------------------------

    @Test
    void mobsAreHostileToPlayersOnly() {
        assertTrue(MobHostility.hostileTo(MobHostility.Side.PLAYER, false), "a survival player");
        assertFalse(MobHostility.hostileTo(MobHostility.Side.MOB, false), "a mob, the Phage's own kind included");
        assertFalse(MobHostility.hostileTo(MobHostility.Side.PLAYER, true), "a creative or spectator player");
    }

    // --- who the blast reaches -------------------------------------------------------------------------------------

    private static MobBlast.Bystander player(double x) {
        return new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.PLAYER, false, "world", new Vec3(x, 64, 0));
    }

    @Test
    void theBlastHitsPlayersInsideTheRadiusAndSparesEveryoneElseInside() {
        MobBlast.Bystander near = player(1.0);
        MobBlast.Bystander edge = player(3.0);
        MobBlast.Bystander beyond = player(3.05);
        MobBlast.Bystander mob = new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.MOB, false, "world",
                new Vec3(0, 64, 1));
        MobBlast.Bystander creative = new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.PLAYER, true,
                "world", new Vec3(0, 64, -1));
        MobBlast.Bystander nether = new MobBlast.Bystander(UUID.randomUUID(), MobHostility.Side.PLAYER, false,
                "world_nether", AT);
        MobBlast.Bystander itself = new MobBlast.Bystander(SELF, MobHostility.Side.MOB, false, "world", AT);

        MobBlast.Reach reach = BLAST.reach(SELF, "world", AT,
                List.of(near, mob, edge, beyond, creative, nether, itself));

        assertEquals(List.of(near, edge), reach.victims(), "players inside, in the order given");
        assertEquals(List.of(mob, creative), reach.spared(), "inside, and not a victim: named, never hurt");
    }

    @Test
    void anEmptyRadiusReachesNobody() {
        MobBlast.Reach reach = BLAST.reach(SELF, "world", AT, List.of(player(4.0)));
        assertTrue(reach.victims().isEmpty());
        assertTrue(reach.spared().isEmpty());
    }

    // --- the numbers are content -----------------------------------------------------------------------------------

    @Test
    void aBadBlastIsRefusedByName() {
        assertThrows(IllegalArgumentException.class, () -> new MobBlast(0, 20, 50, 30), "radius 0");
        assertThrows(IllegalArgumentException.class, () -> new MobBlast(Double.NaN, 20, 50, 30), "radius NaN");
        assertThrows(IllegalArgumentException.class, () -> new MobBlast(3, -1, 50, 30), "negative damage");
        assertThrows(IllegalArgumentException.class, () -> new MobBlast(3, 20, Double.POSITIVE_INFINITY, 30));
        assertThrows(IllegalArgumentException.class, () -> new MobBlast(3, 20, 50, 0), "a fuse of 0 ticks");
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
}
