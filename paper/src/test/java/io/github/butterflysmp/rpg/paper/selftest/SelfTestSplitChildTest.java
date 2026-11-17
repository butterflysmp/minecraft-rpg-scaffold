package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The instrument's split-child rule (PLAN-dragon-mobs.md step R2): a child of a mob the instrument tagged takes ALL its
 * parent's tags, so the row-start clean and the restore remove it and the row can probe it; a child of anything else
 * takes none, so production is untouched.
 */
class SelfTestSplitChildTest {

    @Test
    void aChildOfATaggedParentTakesEveryTag() {
        assertEquals(Set.of("ds1", "t", SelfTestSnapshot.MOB_TAG),
                SelfTestSnapshot.tagsForSplitChild(Set.of("ds1", "t", SelfTestSnapshot.MOB_TAG)));
    }

    @Test
    void aChildOfAnUntaggedParentTakesNothing() {
        assertEquals(Set.of(), SelfTestSnapshot.tagsForSplitChild(Set.of("ds1", "t")), "no instrument tag, no tags");
        assertEquals(Set.of(), SelfTestSnapshot.tagsForSplitChild(Set.of()));
    }

    /**
     * Step R3 (the seat's brief of 2026-11-16): a child of a FROZEN parent the instrument tagged is born frozen, so no
     * row that never asked for a live Phage gets one beside the player. A creeper's swell rises every tick whatever its
     * AI ({@code Creeper.tick}) and only its swell goal turns it up, so a child frozen at BIRTH never swells, where one
     * frozen later keeps the swell it had and still blows. All four corners, so a rule that ignores either input reds.
     */
    @Test
    void aChildOfAFrozenTaggedParentIsBornFrozenAndNoOtherIs() {
        assertTrue(SelfTestSnapshot.freezeSplitChild(Set.of("ds2", "t", SelfTestSnapshot.MOB_TAG), false),
                "tagged and frozen: frozen");
        assertFalse(SelfTestSnapshot.freezeSplitChild(Set.of("bl9", "t", SelfTestSnapshot.MOB_TAG), true),
                "tagged but live: the child keeps its AI, as the row asked of its parent");
        assertFalse(SelfTestSnapshot.freezeSplitChild(Set.of("ds2", "t"), false),
                "untagged: production is untouched, whatever the parent's AI");
        assertFalse(SelfTestSnapshot.freezeSplitChild(Set.of(), true));
    }
}
