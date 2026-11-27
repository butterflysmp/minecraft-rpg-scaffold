package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.build.LoadoutResolution;
import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The A3 line (2026-11-27): a saved id the pool no longer offers, logged once. Its shape, pinned. */
class StonesUnofferedLineTest {

    @Test
    void theUnofferedLine() {
        assertEquals("BUILD unoffered player=00000000-0000-0000-0000-000000000001 cell=mage/fire slot=ULTIMATE"
                        + " id=ultimate_placeholder_mage reads=empty kept=file",
                Stones.unofferedLine(new UUID(0, 1), "mage", "fire",
                        new LoadoutResolution.Unoffered(LoadoutSlot.ULTIMATE, "ultimate_placeholder_mage")));
    }
}
