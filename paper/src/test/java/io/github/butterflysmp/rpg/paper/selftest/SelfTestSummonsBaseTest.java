package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C1's fix (the seat, 2026-11-24): not captured is never written; a captured null is written as null. */
class SelfTestSummonsBaseTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    @Test
    void aProfileNotLoadedAtTheStartIsNotCapturedAndNeverWritten() {
        SelfTestSummonsBase none = SelfTestSummonsBase.capture(null);
        assertFalse(none.captured());
        assertFalse(none.writes(), "writing its null would erase a real base");
    }

    @Test
    void aCapturedNullIsWrittenBackAsNullAndACapturedValueAsItself() {
        SelfTestSummonsBase never = SelfTestSummonsBase.capture(PlayerProfile.fresh(ID));
        assertTrue(never.writes());
        assertNull(never.base(), "never set: restored as absence, which reads 1");
        SelfTestSummonsBase zero = SelfTestSummonsBase.capture(PlayerProfile.fresh(ID).withMaxSummonsBase(0));
        assertTrue(zero.writes());
        assertEquals(0, zero.base());
    }
}
