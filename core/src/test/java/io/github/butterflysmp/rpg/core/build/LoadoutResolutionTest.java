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

    /**
     * THE SEAT'S A3 (2026-11-27), ON THE SHIPPED FIRE MAGE POOL'S SHAPE: Smolder replaced {@code ultimate_placeholder_mage}.
     * A loadout SAVED naming the deleted id resolves to an EMPTY Q (never Smolder, never the default slot by slot), its
     * Actives as saved, and {@link LoadoutResolution#unoffered} names exactly that one slot, which is what is logged once.
     * A player with NOTHING saved gets Smolder by the default, and nothing is unoffered.
     */
    @Test
    void aSavedLoadoutNamingTheDeletedPlaceholderReadsAnEmptyUltimateAndNamesItOnce() {
        PoolDefinition fireMage = new PoolDefinition(new CellKey("mage", "fire"), "Fire Mage",
                List.of("smolder"), List.of("ember_step", "solar_grenade", "solar_lance"),
                new Loadout("smolder", "ember_step", "solar_grenade"));
        Optional<Saved> saved = Optional.of(new Saved("ultimate_placeholder_mage", "solar_lance", "ember_step"));

        Equipped e = LoadoutResolution.resolve(fireMage, saved);
        assertEquals(Optional.empty(), e.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(Optional.of("solar_lance"), e.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals(Optional.of("ember_step"), e.idFor(LoadoutSlot.ACTIVE_2));
        assertEquals(List.of(new LoadoutResolution.Unoffered(LoadoutSlot.ULTIMATE, "ultimate_placeholder_mage")),
                LoadoutResolution.unoffered(fireMage, saved));

        Equipped fresh = LoadoutResolution.resolve(fireMage, Optional.empty());
        assertEquals(Optional.of("smolder"), fresh.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(List.of(), LoadoutResolution.unoffered(fireMage, Optional.empty()));
    }

    /** Unoffered names every slot read empty for want of an offer, in slot order, and NOT a slot saved empty. */
    @Test
    void unofferedNamesEachDroppedIdButNotASlotSavedEmpty() {
        assertEquals(List.of(new LoadoutResolution.Unoffered(LoadoutSlot.ULTIMATE, "act_a"),
                        new LoadoutResolution.Unoffered(LoadoutSlot.ACTIVE_1, "ult_a")),
                LoadoutResolution.unoffered(POOL, Optional.of(new Saved("act_a", "ult_a", null))));
        assertEquals(List.of(new LoadoutResolution.Unoffered(LoadoutSlot.ACTIVE_2, "gone")),
                LoadoutResolution.unoffered(POOL, Optional.of(new Saved(null, "act_b", "gone"))));
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

    // ------------------------------------------------------------------ fragments (slice 4)

    private static final PoolDefinition WITH_FRAGMENTS = new PoolDefinition(new CellKey("ranger", "fire"),
            "Fire Ranger", List.of("ult_a"), List.of("act_a", "act_b"), new Loadout("ult_a", "act_a", "act_b"),
            List.of("frag_a", "frag_b", "frag_c"));

    @Test
    void nothingSavedIsFourEmptyFragmentSlots() {
        assertEquals(java.util.Arrays.asList(null, null, null, null), LoadoutResolution.fragments(WITH_FRAGMENTS, null));
    }

    /** A saved fragment the pool still offers is equipped in its own slot; one it no longer offers reads empty. */
    @Test
    void aSavedFragmentIsEquippedOnlyIfThePoolStillOffersIt() {
        assertEquals(java.util.Arrays.asList("frag_c", null, "frag_a", null),
                LoadoutResolution.fragments(WITH_FRAGMENTS, java.util.Arrays.asList("frag_c", "gone", "frag_a", null)));
    }

    /** Ruling 11: one of each. A hand-edited duplicate keeps its FIRST slot; the rest read empty. */
    @Test
    void aDuplicateFragmentKeepsItsFirstSlot() {
        assertEquals(java.util.Arrays.asList(null, "frag_b", null, null),
                LoadoutResolution.fragments(WITH_FRAGMENTS, java.util.Arrays.asList(null, "frag_b", "frag_b", "frag_b")));
    }

    @Test
    void aPoolListingAFragmentTwiceIsRefused() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(
                new CellKey("ranger", "fire"), "x", List.of("ult_a"), List.of("act_a", "act_b"),
                new Loadout("ult_a", "act_a", "act_b"), List.of("frag_a", "frag_a")));
        assertEquals(List.of(), POOL.fragments(), "the five-argument form is a pool with no fragments");
    }

    // ------------------------------------------------------------------ aspects (slice 5)

    @Test
    void aspectsFollowTheFragmentRuleInTwoSlots() {
        PoolDefinition pool = new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger", List.of("ult_a"),
                List.of("act_a", "act_b"), new Loadout("ult_a", "act_a", "act_b"), List.of(), List.of("asp_a", "asp_b"));
        assertEquals(java.util.Arrays.asList(null, null), LoadoutResolution.aspects(pool, null));
        assertEquals(java.util.Arrays.asList("asp_b", null),
                LoadoutResolution.aspects(pool, java.util.Arrays.asList("asp_b", "gone")), "an unoffered id reads empty");
        assertEquals(java.util.Arrays.asList("asp_a", null),
                LoadoutResolution.aspects(pool, java.util.Arrays.asList("asp_a", "asp_a")), "the same aspect twice: once");
        assertEquals(List.of(), POOL.aspects(), "the five-argument form is a pool with no aspects");
    }
}
