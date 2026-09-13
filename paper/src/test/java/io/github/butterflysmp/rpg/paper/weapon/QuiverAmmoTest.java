package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.CollectPlan;
import io.github.butterflysmp.rpg.paper.health.QuiverSizeModifierItems;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice E, the PAPER half that a unit test can reach: which material is ammo, and that the draw
 * planner behaves the way the reload needs it to.
 *
 * <h2>WHAT THIS FILE CANNOT REACH, SAID RATHER THAN LEFT</h2>
 *
 * <p>{@code QuiverAmmo.supply} and {@code consume} need a live {@code Player} — one for
 * {@code getGameMode()}, both for the inventory — and no unit test in this project holds one. <b>So
 * the game-mode arm and the debit are BOOT-ONLY</b>, and {@code GATE-quiver-ammo.md} rows 1 and 4
 * carry them. Do not read this file's green as coverage of either.
 *
 * <p>What IS reachable is the part that was a comment until Slice E and is now a fact: the two
 * materials.
 *
 * <h2>AND EVERY ROW READS THE REAL CONSTANT</h2>
 *
 * <p>Slice D shipped two assertions that modelled their defects with the values written out as
 * literals; both mutations reddened <b>nothing</b>. So nothing here restates a material by name —
 * every row reads {@code QuiverAmmo.AMMO} and {@code QuiverSizeModifierItems.INSTRUMENT_MATERIAL}
 * from the classes that use them.
 */
class QuiverAmmoTest {

    // --- THE SPECTRAL TRAP, which is a MECHANISM and not a taste ------------------------------------

    /**
     * THE HEADLINE. The dev instrument that ENLARGES a magazine must never be eaten by a reload.
     *
     * <p>{@code QuiverSizeModifierItems} mints {@code quiver_size_boost} as a SPECTRAL arrow, and it
     * is {@code /rpg give}-able, so a player testing this slice is holding one in the same inventory
     * as their ammo. A family match — {@code endsWith("ARROW")}, or a tag lookup — would consume the
     * gear that made the quiver bigger, to fill the quiver it enlarged.
     */
    @Test
    void theAmmoMaterialIsNOTTheQuiverSizeInstrumentsMaterial() {
        assertNotEquals(QuiverSizeModifierItems.INSTRUMENT_MATERIAL, QuiverAmmo.AMMO,
                "a reload would consume the item that enlarges the magazine -- and the player would "
                        + "watch their quiver-size gear disappear into their own quiver");
        // Mutation: point QuiverAmmo.AMMO at SPECTRAL_ARROW, or re-mint the instrument as ARROW
        // -> reddens. Before this row, both edits were silent.
    }

    @Test
    void theAmmoIsPLAINArrowExactlyAndNoOtherArrowIsAmmo() {
        assertSame(Material.ARROW, QuiverAmmo.AMMO, "ruling 2: vanilla ARROW");
        // The other two members of the family, named so a future family match is a visible edit here
        // rather than an invisible one at the call site.
        assertNotEquals(Material.SPECTRAL_ARROW, QuiverAmmo.AMMO);
        assertNotEquals(Material.TIPPED_ARROW, QuiverAmmo.AMMO);
        // Mutation: any widening of AMMO to a family -> reddens.
    }

    // --- THE DRAW PLAN, which is what decides how many arrows a reload takes -------------------------

    /**
     * The mapping onto {@code CollectPlan} is exercised the way {@code supply} uses it: a cursor of
     * ZERO and a maximum of ROUNDS NEEDED. That pairing is the whole of the reuse, and it is the part
     * most likely to be got wrong by someone reading {@code plan}'s own parameter names.
     */
    @Test
    void aPartialSupplyDrawsEVERYTHINGAvailableAndStops() {
        // RULING 5, at the planner: 7 arrows, 8 rounds of room.
        List<CollectPlan.Draw> draws = CollectPlan.plan(
                List.of(new CollectPlan.Source(CollectPlan.TIER_INVENTORY, 3, 4),
                        new CollectPlan.Source(CollectPlan.TIER_INVENTORY, 9, 3)),
                0, 8);
        assertEquals(7, CollectPlan.total(draws), "it takes all 7 -- a partial load, not a refusal");
        assertEquals(2, draws.size(), "from both stacks");
        // Mutation: pass `needed` as the cursor rather than the maximum -> room becomes negative and
        // the plan is empty -> reddens.
    }

    @Test
    void aSupplyNEVERTakesMoreThanTheRoundsNeeded() {
        // The row that stops a top-up emptying a player's whole quiver stack. 1 round of room,
        // 64 arrows to hand, and the numbers are deliberately far apart so a swap is visible.
        List<CollectPlan.Draw> draws = CollectPlan.plan(
                List.of(new CollectPlan.Source(CollectPlan.TIER_INVENTORY, 0, 64)), 0, 1);
        assertEquals(1, CollectPlan.total(draws), "one round of room costs exactly one arrow");
        // Mutation: swap the last two arguments -> 64 taken for one round of room -> reddens, and
        // that mutation is the realistic typo at the call site.
    }

    @Test
    void needingNothingDrawsNothing() {
        assertTrue(CollectPlan.plan(
                        List.of(new CollectPlan.Source(CollectPlan.TIER_INVENTORY, 0, 64)), 0, 0)
                .isEmpty(), "a full magazine costs no arrows");
        // supply() short-circuits this case before planning, so this row pins the planner's own
        // answer rather than the guard in front of it -- the two must agree, or removing the
        // short-circuit would start charging players for nothing.
    }

    // --- The mode axis, enumerated here because the ARM cannot be reached ----------------------------

    /**
     * ALL FOUR MODES EXIST AND ARE DISTINCT — which is the only part of the game-mode decision a unit
     * test can hold.
     *
     * <p>The arm itself is in {@code QuiverAmmo.supply} behind {@code player.getGameMode()} and is
     * unreachable without a server. <b>This row is not a substitute for gate row 4</b>; it pins the
     * axis the arm switches over, so a Paper release adding a fifth mode is visible here as well as
     * being a compile error there.
     */
    @Test
    void thereAreFourGameModesAndTheArmMustCoverAllOfThem() {
        assertEquals(4, GameMode.values().length,
                "a fifth game mode means QuiverAmmo.supply's switch needs an arm -- it has no "
                        + "default, so it will not compile until someone decides what the mode does");
    }
}
