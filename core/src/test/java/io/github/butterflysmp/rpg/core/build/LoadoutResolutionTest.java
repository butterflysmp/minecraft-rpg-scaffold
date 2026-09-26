package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Saved;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadoutResolutionTest {

    /** Every id is distinct, so a transposition between any two slots has nowhere to hide. */
    private static final PoolDefinition POOL = new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger",
            List.of("ult_a", "ult_b"), List.of("act_a", "act_b", "act_c"),
            new Loadout("ult_a", "act_a", "act_b"));

    @Test
    void nothingSavedCastsThePoolDefault() {
        Equipped e = LoadoutResolution.resolve(POOL, Optional.empty());
        assertEquals(Optional.of("ult_a"), e.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(Optional.of("act_a"), e.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals(Optional.of("act_b"), e.idFor(LoadoutSlot.ACTIVE_2));
        assertTrue(e.fromDefault());
    }

    /** THE ROW THE "resolve from the default even when saved" MUTATION REDDENS: every slot differs from it. */
    @Test
    void aSavedLoadoutIsCastInsteadOfTheDefault() {
        Equipped e = LoadoutResolution.resolve(POOL, Optional.of(new Saved("ult_b", "act_c", "act_a")));
        assertEquals(Optional.of("ult_b"), e.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(Optional.of("act_c"), e.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals(Optional.of("act_a"), e.idFor(LoadoutSlot.ACTIVE_2));
        assertFalse(e.fromDefault());
        assertEquals(List.of("ult_b", "act_c", "act_a"), e.ids());
    }

    /** An id the pool no longer offers reads EMPTY -- and does not fall back to the default slot by slot. */
    @Test
    void anIdMissingFromThePoolReadsEmptyNotTheDefault() {
        Equipped e = LoadoutResolution.resolve(POOL, Optional.of(new Saved("gone", "act_c", "gone_too")));
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(Optional.of("act_c"), e.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ACTIVE_2));
        assertEquals(List.of("act_c"), e.ids(), "empties are skipped in the castable set");
    }

    /** An id in the wrong ROLE is not offered there: an Active saved as the Ultimate reads empty. */
    @Test
    void anIdInTheWrongRoleReadsEmpty() {
        Equipped e = LoadoutResolution.resolve(POOL, Optional.of(new Saved("act_a", "ult_a", "act_b")));
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals(Optional.of("act_b"), e.idFor(LoadoutSlot.ACTIVE_2));
    }

    @Test
    void savedEmptySlotsStayEmpty() {
        Equipped e = LoadoutResolution.resolve(POOL, Optional.of(new Saved(null, null, "act_b")));
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(List.of("act_b"), e.ids());
    }

    @Test
    void theSameActiveTwiceKeepsTheFirst() {
        Equipped e = LoadoutResolution.resolve(POOL, Optional.of(new Saved("ult_a", "act_c", "act_c")));
        assertEquals(Optional.of("act_c"), e.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ACTIVE_2));
    }
}
