package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shield content model and the refusals its constructor makes.
 *
 * The headline is {@link #anOutOfRangeBlockDrIsRefusedRatherThanLoaded} -- the loader catches a
 * RuntimeException and turns it into a NAMED, SKIPPED file in the boot log, so a refusal here is
 * the difference between "shield 'x' skipped: block_dr 2.0 must be between 0 and 1" and a shield
 * that silently heals whoever holds it.
 *
 * That refusal is deliberately NOT the same guard as {@code Shield.clamp}, and the pair is not
 * redundant: this one guards CONTENT at load time and refuses; the clamp guards the ARITHMETIC for
 * an item already sitting in someone's inventory, where no loader will ever run again.
 *
 * Each test names the mutation it forces red.
 */
class ShieldDefinitionTest {

    private static final double EPS = 1e-9;

    private static ShieldDefinition shield(double blockDr) {
        return new ShieldDefinition("roundshield", "Roundshield", Rarity.COMMON,
                ShieldDefinition.DEFAULT_MATERIAL, blockDr, List.of());
    }

    // --- What it carries ------------------------------------------------------------------------

    @Test
    void aShieldCarriesItsBlockFractionAndDefersTheArithmeticToShield() {
        ShieldDefinition s = shield(0.5);
        assertEquals(0.5, s.blockDr(), EPS);
        assertEquals(0.5, s.passThrough(), EPS, "half stopped means half through");
        assertTrue(s.blocks());
        // Mutation: have passThrough() return blockDr() directly -> it agrees at 0.5 BY ACCIDENT
        // and nowhere else; see theHalfShieldIsTheOneValueWhereBlockedAndPassedCoincide.
    }

    @Test
    void theHalfShieldIsTheOneValueWhereBlockedAndPassedCoincide() {
        // Written because the shipped shield is 0.5, where "fraction stopped" and "fraction passed"
        // are the SAME NUMBER. Any confusion between the two readings is invisible at exactly the
        // value this slice ships and wrong everywhere else, so it has to be pinned off 0.5.
        assertEquals(0.75, shield(0.25).passThrough(), EPS, "a quarter stopped, three quarters through");
        assertEquals(0.1, shield(0.9).passThrough(), EPS, "nine tenths stopped, one tenth through");
        // Mutation: return blockDr() from passThrough() -> 0.25 and 0.9 come back unflipped
        // -> reddens, where a test written only against 0.5 would not.
    }

    @Test
    void aShieldMayDeclareNoBlockAtAll() {
        // Zero is legal content, not a malformed file: it is how somebody authors a cosmetic or
        // placeholder shield. It must load and then do nothing.
        ShieldDefinition s = shield(0.0);
        assertFalse(s.blocks(), "zero declares no block");
        assertEquals(1.0, s.passThrough(), EPS, "so everything gets through");
        // Mutation: reject 0 in the constructor -> a legal shield becomes a skipped file
        // -> reddens.
    }

    @Test
    void aTotalBlockIsLegalContentEvenThoughNothingShipsIt() {
        // 1.0 is the inclusive upper bound. Pinned so that tightening the range to exclusive is a
        // deliberate edit somebody has to red a test to make, rather than an off-by-one.
        ShieldDefinition s = shield(1.0);
        assertTrue(s.blocks());
        assertEquals(0.0, s.passThrough(), EPS, "nothing gets through a total block");
        // Mutation: make the upper bound exclusive (blockDr < FULL) -> a legal shield is refused
        // -> reddens.
    }

    // --- The refusals ---------------------------------------------------------------------------

    @Test
    void anOutOfRangeBlockDrIsRefusedRatherThanLoaded() {
        // THE headline. Both directions are catastrophes and the messages must name the value, so
        // the boot log points at the typo rather than at the file in general.
        IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                () -> shield(2.0));
        assertTrue(high.getMessage().contains("2.0"),
                "the refusal must quote the offending value, got: " + high.getMessage());
        assertTrue(high.getMessage().contains("roundshield"),
                "and name the shield, got: " + high.getMessage());

        assertThrows(IllegalArgumentException.class, () -> shield(-0.5),
                "a negative fraction would DOUBLE the hit");
        assertThrows(IllegalArgumentException.class, () -> shield(1.0001),
                "and just over one would make the damage negative");
        // Mutation: drop the range check entirely -> all three load and the clamp in Shield becomes
        // the only thing between content and a healing hit -> reddens.
    }

    @Test
    void aNaNBlockDrIsRefusedToo() {
        // The range check is written as a NEGATED range for exactly this input. Every comparison
        // against NaN is false, so the natural spelling -- blockDr < 0 || blockDr > 1 -- waves NaN
        // straight through into a shield whose damage is NaN.
        assertThrows(IllegalArgumentException.class, () -> shield(Double.NaN));
        // Mutation: rewrite the guard as `if (blockDr < Shield.NONE || blockDr > Shield.FULL)`
        // -> NaN loads -> reddens.
    }

    @Test
    void theIdentityFieldsAreAllRequired() {
        assertThrows(IllegalArgumentException.class, () -> new ShieldDefinition(
                " ", "Roundshield", Rarity.COMMON, "shield", 0.5, List.of()), "blank id");
        assertThrows(IllegalArgumentException.class, () -> new ShieldDefinition(
                "roundshield", " ", Rarity.COMMON, "shield", 0.5, List.of()), "blank display_name");
        assertThrows(IllegalArgumentException.class, () -> new ShieldDefinition(
                "roundshield", "Roundshield", null, "shield", 0.5, List.of()), "no rarity");
        assertThrows(IllegalArgumentException.class, () -> new ShieldDefinition(
                "roundshield", "Roundshield", Rarity.COMMON, " ", 0.5, List.of()), "blank material");
        // Mutation: drop any one guard -> that constructor call returns a definition whose item
        // would mint with a blank name or no material -> reddens.
    }

    // --- Defensive copying ----------------------------------------------------------------------

    @Test
    void theFlavorListIsCopiedSoALoaderCannotMutateALoadedShield() {
        // The loader hands over a list it still holds. Without the copy, a shield already in the
        // registry would change under the tooltip that already rendered it.
        List<String> authored = new ArrayList<>(List.of("Plain oak, banded in iron."));
        ShieldDefinition s = new ShieldDefinition("roundshield", "Roundshield", Rarity.COMMON,
                "shield", 0.5, authored);
        authored.add("smuggled in after load");
        assertEquals(1, s.flavor().size(), "the loaded shield must not see the later addition");
        // Mutation: assign `flavor` straight through without List.copyOf -> the shield grows a
        // flavor line it was never authored with -> reddens.
    }

    @Test
    void anAbsentFlavorIsAnEmptyListRatherThanNull() {
        // Every consumer iterates it. Null here would be a NullPointerException at mint time, in
        // paper, where there is no test to catch it.
        ShieldDefinition s = new ShieldDefinition("roundshield", "Roundshield", Rarity.COMMON,
                "shield", 0.5, null);
        assertTrue(s.flavor().isEmpty());
        // Mutation: drop the null branch -> a shield with no flavor: key NPEs when its lore is
        // built -> reddens.
    }

    @Test
    void theDefaultMaterialIsTheVanillaShieldBecauseNothingElseBlocks() {
        // Load-bearing, not cosmetic: vanilla's shield item is what supplies the raise animation,
        // the block sound, the 90-degree arc and the frontal validity this slice RIDES rather than
        // re-derives. A shield authored onto another material mints and renders fine and then never
        // blocks anything.
        assertEquals("shield", ShieldDefinition.DEFAULT_MATERIAL);
        // Mutation: change the default to any other material -> every shield authored without an
        // explicit material silently stops blocking -> reddens.
    }
}
