package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The Phage's split on death (PLAN-dragon-mobs.md step R2). Ben: "Only the 5th stage Phage splits"; a killed stage 5
 * splits into two stage 1s (G4); "Any deaths". The rule answers ONE question: given a mob's definition, its stage at
 * death, and whether this mob has already split, how many children and at what stage.
 */
class MobSplitTest {

    private static MobStages fiveStages() {
        return new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160));
    }

    private static MobDefinition phage(MobSplit split) {
        return new MobDefinition("phage", "creeper", "Phage", 80, MobBody.VANILLA, "void", fiveStages(), split);
    }

    @Test
    void aStageFiveDeathGivesTwoChildrenAtStageOne() {
        MobDefinition phage = phage(new MobSplit(2, 1, 0.5));
        assertEquals(new MobSplit.Children(2, 1), MobSplit.childrenFor(phage, 5, false));
    }

    @Test
    void stagesOneToFourGiveNone() {
        MobDefinition phage = phage(new MobSplit(2, 1, 0.5));
        for (int stage = 1; stage <= 4; stage++) {
            assertEquals(MobSplit.Children.NONE, MobSplit.childrenFor(phage, stage, false), "stage " + stage);
        }
    }

    @Test
    void aMobWithNoSplitBlockGivesNone() {
        assertEquals(MobSplit.Children.NONE, MobSplit.childrenFor(phage(null), 5, false));
        MobDefinition knell = new MobDefinition("knell", "wither_skeleton", "Knell", 360);
        assertEquals(MobSplit.Children.NONE, MobSplit.childrenFor(knell, 1, false));
        assertEquals(MobSplit.Children.NONE, MobSplit.childrenFor(null, 5, false), "a vanilla mob: no definition");
    }

    /** THE ONCE-ONLY GUARD: a second death event for a mob that has already split adds none. */
    @Test
    void aMobThatHasAlreadySplitAddsNone() {
        MobDefinition phage = phage(new MobSplit(2, 1, 0.5));
        assertEquals(new MobSplit.Children(2, 1), MobSplit.childrenFor(phage, 5, false), "the first delivery splits");
        assertEquals(MobSplit.Children.NONE, MobSplit.childrenFor(phage, 5, true), "the second adds none");
    }

    /** The count and the child stage come from CONTENT: author 3 at stage 2 and that is what arrives. */
    @Test
    void theCountAndTheChildStageComeFromContent() {
        MobDefinition phage = phage(new MobSplit(3, 2, 0.5));
        assertEquals(new MobSplit.Children(3, 2), MobSplit.childrenFor(phage, 5, false));
    }

    /** "Half a block either side": two children at -0.5 and +0.5; three at -1, 0, +1. Along one axis. */
    @Test
    void theOffsetsSpreadEvenlyAroundTheParent() {
        assertArrayEquals(new double[]{-0.5, 0.5}, new MobSplit(2, 1, 0.5).offsets(), 0.0);
        assertArrayEquals(new double[]{-1.0, 0.0, 1.0}, new MobSplit(3, 1, 0.5).offsets(), 0.0);
        assertArrayEquals(new double[]{0.0}, new MobSplit(1, 1, 0.5).offsets(), 0.0);
    }

    /** A child is the same shape as its parent at a different height: width scales with height. */
    @Test
    void aChildsWidthFollowsItsHeight() {
        // a stage-5 creeper: 3 tall, 0.6 x 3/1.7 wide; its stage-1 child is 1 tall, so 0.6 / 1.7 wide.
        double parentWidth = 0.6 * 3.0 / 1.7;
        assertEquals(0.6 / 1.7, MobSplit.childWidth(parentWidth, 3.0, 1.0), 1e-12);
    }

    @Test
    void aSplitWithNoChildrenOrANegativeSpreadIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new MobSplit(0, 1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new MobSplit(2, 0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new MobSplit(2, 1, -0.1));
    }

    /** A split names a child stage, so it needs stages; and the child stage must be one of them. */
    @Test
    void aSplitOnAnUnstagedMobOrToAStageThatDoesNotExistIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("knell", "wither_skeleton", "Knell",
                360, MobBody.VANILLA, null, null, new MobSplit(2, 1, 0.5)));
        assertThrows(IllegalArgumentException.class, () -> phage(new MobSplit(2, 6, 0.5)));
    }
}
