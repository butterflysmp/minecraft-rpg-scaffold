package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.mining.BlockPos;
import io.github.butterflysmp.rpg.core.mining.FaceFrame;
import io.github.butterflysmp.rpg.core.mining.PatchPoint;
import io.github.butterflysmp.rpg.core.mining.Spot;
import io.github.butterflysmp.rpg.core.mining.SpotBox;
import io.github.butterflysmp.rpg.core.mining.WallFace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mining verbs' aim, cast exactly as the server reads it: from the player's EYE (feet + 1.62), along Bukkit's
 * direction for the FLOAT yaw and pitch {@link SelfTestAim} returns ({@code Location#getDirection}), through the real
 * {@link FaceFrame#worldToPatch} that {@code MiningService.onBlockDamage} calls, into the real {@link SpotBox}.
 *
 * <p><b>THE STAGING IT ASSUMES IS GATE-mining-auto.md's</b>: the player at a block centre, facing south; the wall's
 * NORTH face 1.5 blocks ahead (the wall is the blocks at dz = 2); the slot anchored on the block at (0, 0, 2) from the
 * player's block. If a scenario moves the wall, this test is the one that must move with it.
 */
class SelfTestMiningAimTest {

    private static final double EYE = 1.62;
    /** The player's block is (0, 64, 0); the feet stand at its centre. */
    private static final double FX = 0.5, FY = 64, FZ = 0.5;
    /** Vanilla's survival block-interaction range: the attribute's default, and the DIG verb's reach. */
    private static final double REACH = 4.5;

    /** The four shipped vein sizes, width x height in blocks (content/vein_sizes). */
    private static final int[][] SIZES = {{3, 2}, {5, 3}, {7, 3}, {7, 4}};

    private static FaceFrame frame(int w, int h) {
        return new FaceFrame(0, 64, 2, WallFace.NORTH, w, h);
    }

    // --- the arguments --------------------------------------------------------------------------------------------

    @Test
    void aFaceCentreIsTheMiddleOfThatFace() {
        assertArrayEquals(new double[] {0.5, 64.5, 2.0}, SelfTestMiningAim.faceCentre(0, 64, 2, 0, 0, -1), "NORTH");
        assertArrayEquals(new double[] {0.5, 64.5, 3.0}, SelfTestMiningAim.faceCentre(0, 64, 2, 0, 0, 1), "SOUTH");
        assertArrayEquals(new double[] {1.0, 64.5, 2.5}, SelfTestMiningAim.faceCentre(0, 64, 2, 1, 0, 0), "EAST");
        assertArrayEquals(new double[] {0.0, 64.5, 2.5}, SelfTestMiningAim.faceCentre(0, 64, 2, -1, 0, 0), "WEST");
        assertArrayEquals(new double[] {0.5, 64.0, 1.5}, SelfTestMiningAim.faceCentre(0, 63, 1, 0, 1, 0), "UP");
    }

    @Test
    void theTargetsAreBoxCentresAndPixelCentres() {
        List<Spot> spots = List.of(new Spot(10, 12), new Spot(30, 20), new Spot(40, 5));
        FaceFrame small = frame(3, 2);
        assertArrayEquals(new double[] {30.5, 20.5}, SelfTestMiningAim.target("live", small, spots, 1));
        assertArrayEquals(new double[] {40.5, 5.5}, SelfTestMiningAim.target("spot 3", small, spots, 0), "1-based, as the show line counts");
        assertArrayEquals(new double[] {7.5, 9.5}, SelfTestMiningAim.target("pixel 7 9", small, spots, 0));
        assertArrayEquals(new double[] {35.5, 20.5}, SelfTestMiningAim.target("beside", small, spots, 1), "5 px to the right");
        // The left fallback: 40 + 0.5 + 5 = 45.5 is still on a 48 px patch, so right; 44 + 5.5 = 49.5 is off it.
        assertArrayEquals(new double[] {45.5, 5.5}, SelfTestMiningAim.target("beside", small, spots, 2));
        assertArrayEquals(new double[] {39.5, 20.5},
                SelfTestMiningAim.target("beside", small, List.of(new Spot(44, 20)), 0), "off the right edge: 5 px left");
    }

    @Test
    void aBadArgumentIsRefusedByNameNotGuessed() {
        FaceFrame small = frame(3, 2);
        List<Spot> spots = List.of(new Spot(10, 12));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SelfTestMiningAim.target("spot 2", small, spots, 0))
                .getMessage().contains("no spot 2 in a patch of 1"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SelfTestMiningAim.target("pixel 48 0", small, spots, 0))
                .getMessage().contains("off a 48 x 32 patch"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SelfTestMiningAim.target("centre", small, spots, 0))
                .getMessage().contains("wants live"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SelfTestMiningAim.target("live", small, List.of(), -1))
                .getMessage().contains("no spot 0"), "a broken patch has no live spot");
    }

    // --- the ray ----------------------------------------------------------------------------------------------------

    @Test
    void everyLegalSpotOfEverySizeIsHitInItsBoxAndBesideItIsAMissOnThePatch() {
        int cast = 0;
        double worstReach = 0;
        for (int[] size : SIZES) {
            FaceFrame f = frame(size[0], size[1]);
            int w = size[0] * 16, h = size[1] * 16;
            // Every centre the layout can roll: [2, w - 3] x [2, h - 3] (SpotLayout's edge rule).
            for (int cu = 2; cu <= w - 3; cu++) {
                for (int cv = 2; cv <= h - 3; cv++) {
                    Spot s = new Spot(cu, cv);
                    double[] liveUv = SelfTestMiningAim.target("live", f, List.of(s), 0);
                    PatchPoint at = cast(f, liveUv);
                    assertTrue(SpotBox.contains(s, at), "live " + cu + "," + cv + " on " + w + "x" + h + " read " + at);
                    // Within 0.005 px of the box centre, so the click line's two-decimal aim prints cu.50,cv.50.
                    assertEquals(liveUv[0], at.u(), 0.005, "u");
                    assertEquals(liveUv[1], at.v(), 0.005, "v");
                    worstReach = Math.max(worstReach, reach(f, liveUv));
                    assertTrue(f.footprint().contains(blockHit(f, liveUv)), "the dig's block is the patch's");

                    double[] besideUv = SelfTestMiningAim.target("beside", f, List.of(s), 0);
                    PatchPoint off = cast(f, besideUv);
                    assertFalse(SpotBox.contains(s, off), "beside " + cu + "," + cv + " read " + off);
                    assertTrue(off.u() >= 0 && off.u() < w && off.v() >= 0 && off.v() < h, "beside stays ON the patch: " + off);
                    assertTrue(f.footprint().contains(blockHit(f, besideUv)), "so its block is the patch's, never outside");
                    worstReach = Math.max(worstReach, reach(f, besideUv));
                    cast += 2;
                }
            }
        }
        assertEquals(2 * (44 * 28 + 76 * 44 + 108 * 44 + 108 * 60), cast, "every legal centre of every size, twice");
        assertTrue(worstReach < REACH, "the farthest aim is " + worstReach + " from the eye: inside the dig's reach");
    }

    @Test
    void theWallFaceAndTheOutsideBlockAreWithinReachAndOnTheirOwnBlocks() {
        // A1-A12's placements aim at (0, 0, 2) NORTH; A11 clicks the top corner (3, 3, 2) of an extra_large slot.
        for (int[] b : new int[][] {{0, 0, 2}, {3, 3, 2}, {-3, 3, 2}}) {
            double[] t = SelfTestMiningAim.faceCentre(b[0], 64 + b[1], b[2], 0, 0, -1);
            double d = Math.sqrt(sq(t[0] - FX) + sq(t[1] - (FY + EYE)) + sq(t[2] - FZ));
            assertTrue(d < REACH, "block " + b[0] + "," + b[1] + "," + b[2] + " is " + d + " from the eye");
        }
    }

    @Test
    void controlTheFeetAnchorMissesTheSpotThatTheEyeAnchorHits() {
        // The positive control: the same cast from a rotation computed at the FEET (eye height 0) lands 1.62 blocks
        // (about 26 px) off, outside the box. If this ever reads IN, the sweep above proves nothing.
        FaceFrame f = frame(7, 4);
        Spot s = new Spot(56, 32);
        double[] w = f.patchToWorld(s.cu() + 0.5, s.cv() + 0.5, 0.0);
        SelfTestAim.Rotation fromFeet = SelfTestAim.at(FX, FY, FZ, 0, w[0], w[1], w[2]);
        Optional<PatchPoint> read = f.worldToPatch(FX, FY + EYE, FZ, dx(fromFeet), dy(fromFeet), dz(fromFeet));
        assertFalse(SpotBox.contains(s, read.orElseThrow()), "read " + read);
    }

    /** The patch point the server reads for an eye turned to {@code uv}: rotation as floats, then Bukkit's direction. */
    private static PatchPoint cast(FaceFrame f, double[] uv) {
        double[] w = f.patchToWorld(uv[0], uv[1], 0.0);
        SelfTestAim.Rotation r = SelfTestAim.at(FX, FY, FZ, EYE, w[0], w[1], w[2]);
        return f.worldToPatch(FX, FY + EYE, FZ, dx(r), dy(r), dz(r)).orElseThrow();
    }

    /** The block the dig's ray meets: the wall block holding the point, which is never on a block edge. */
    private static BlockPos blockHit(FaceFrame f, double[] uv) {
        double[] w = f.patchToWorld(uv[0], uv[1], 0.0);
        for (double c : new double[] {w[0], w[1]}) {
            double frac = c - Math.floor(c);
            assertTrue(frac > 1.0 / 64 && frac < 1 - 1.0 / 64, "a target sits on a block edge: " + c);
        }
        return new BlockPos((int) Math.floor(w[0]), (int) Math.floor(w[1]), 2);
    }

    private static double reach(FaceFrame f, double[] uv) {
        double[] w = f.patchToWorld(uv[0], uv[1], 0.0);
        return Math.sqrt(sq(w[0] - FX) + sq(w[1] - (FY + EYE)) + sq(w[2] - FZ));
    }

    // Location#getDirection, from the float yaw and pitch the teleport carries.
    private static double dx(SelfTestAim.Rotation r) { return -Math.cos(Math.toRadians(r.pitch())) * Math.sin(Math.toRadians(r.yaw())); }
    private static double dy(SelfTestAim.Rotation r) { return -Math.sin(Math.toRadians(r.pitch())); }
    private static double dz(SelfTestAim.Rotation r) { return Math.cos(Math.toRadians(r.pitch())) * Math.cos(Math.toRadians(r.yaw())); }

    private static double sq(double v) { return v * v; }
}
