package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
