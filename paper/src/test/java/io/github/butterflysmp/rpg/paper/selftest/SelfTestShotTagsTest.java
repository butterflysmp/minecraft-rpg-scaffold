package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A tagged Phalanx's shot takes {@code selftest} and each other tag with {@code _shot}; an untagged one's takes none. */
class SelfTestShotTagsTest {

    @Test
    void aTaggedPhalanxsShotIsNamedAfterItAndRemovedAtRowStart() {
        assertEquals(Set.of("selftest", "sh2_shot", "t_shot"),
                SelfTestSnapshot.tagsForShot(Set.of("selftest", "sh2", "t")));
    }

    @Test
    void aPhalanxTheInstrumentNeverTaggedGivesNothing() {
        assertEquals(Set.of(), SelfTestSnapshot.tagsForShot(Set.of("sh2", "t")));
    }
}
