package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The armor content model and the refusals its constructor makes.
 *
 * The headline is {@link #aNaNDefenseIsRefusedAndNotWavedThroughByTheRangeCheck}: the guard is
 * written as a NEGATED range precisely so NaN cannot pass, because every comparison against NaN is
 * false and the obvious {@code defense < 0} spelling would mint a piece whose tooltip reads
 * "Defense: NaN". The loader catches a RuntimeException and turns it into a named, skipped file, so
 * a refusal here is the difference between a boot-log line and a broken item nobody can explain.
 *
 * Worth restating because it is the thing most likely to be misread from this record: the
 * {@code defense} value is DISPLAY-ONLY in this slice. The mitigation a worn piece actually
 * contributes is read off vanilla by {@code DefenseModifierItems}. These tests guard what gets
 * PRINTED, and {@code ArmorConsistency} at boot is what guards the two agreeing.
 *
 * Each test names the mutation it forces red.
 */
class ArmorDefinitionTest {

    private static final double EPS = 1e-9;

    private static ArmorDefinition armor(double defense) {
        return new ArmorDefinition("diamond_helmet", "Diamond Helmet", Rarity.UNCOMMON,
                "diamond_helmet", ArmorSlot.HEAD, defense, List.of());
    }

    // --- What it carries ------------------------------------------------------------------------

    @Test
    void aPieceCarriesItsIdentityAndItsDisplayDefense() {
        ArmorDefinition a = armor(3);
        assertEquals("diamond_helmet", a.id());
        assertEquals("Diamond Helmet", a.displayName());
        assertEquals(Rarity.UNCOMMON, a.rarity());
        assertEquals("diamond_helmet", a.material());
        assertEquals(ArmorSlot.HEAD, a.slot());
        assertEquals(3, a.defense(), EPS);
        // Mutation: swap the material and displayName components in the record header -> reddens.
    }

    @Test
    void aPieceMayDeclareNoDefenseAtAll() {
        // Zero is legal content. Vanilla has wearable head items worth nothing -- a carved pumpkin,
        // a mob head -- and a cosmetic piece must load and then contribute nothing rather than
        // being refused as malformed.
        assertEquals(0, armor(0).defense(), EPS);
        // Mutation: reject 0 in the constructor -> a legal cosmetic piece becomes a skipped file
        // -> reddens.
    }

    // --- The refusals ---------------------------------------------------------------------------

    @Test
    void aNegativeDefenseIsRefusedRatherThanLoaded() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> armor(-1));
        assertEquals(true, ex.getMessage().contains("diamond_helmet"),
                "the message must name the piece so the boot log is actionable: " + ex.getMessage());
        // Mutation: drop the defense guard entirely -> reddens.
    }

    @Test
    void aNaNDefenseIsRefusedAndNotWavedThroughByTheRangeCheck() {
        // THE HEADLINE. `defense < 0` is false for NaN, so the naive spelling accepts it. The
        // negated range `!(defense >= 0)` is true for NaN and refuses. A YAML value of `.nan`, or
        // any arithmetic a future loader does on a missing key, reaches here.
        assertThrows(IllegalArgumentException.class, () -> armor(Double.NaN));
        // Mutation: rewrite the guard as `if (defense < 0) throw` -> NaN loads, the item mints,
        // and its tooltip reads "Defense: NaN" -> reddens.
    }

    @Test
    void everyIdentityFieldIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new ArmorDefinition(
                null, "Diamond Helmet", Rarity.UNCOMMON, "diamond_helmet", ArmorSlot.HEAD, 3, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArmorDefinition(
                "  ", "Diamond Helmet", Rarity.UNCOMMON, "diamond_helmet", ArmorSlot.HEAD, 3, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArmorDefinition(
                "diamond_helmet", " ", Rarity.UNCOMMON, "diamond_helmet", ArmorSlot.HEAD, 3, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArmorDefinition(
                "diamond_helmet", "Diamond Helmet", null, "diamond_helmet", ArmorSlot.HEAD, 3, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArmorDefinition(
                "diamond_helmet", "Diamond Helmet", Rarity.UNCOMMON, "", ArmorSlot.HEAD, 3, List.of()));
        // Mutation: delete any one of the five guards -> reddens.
    }

    @Test
    void theSlotIsRequiredBecauseThereIsNoSafeDefaultForIt() {
        // Unlike material or rarity, a missing slot has no sensible fallback: defaulting to HEAD
        // would mint a chestplate that reconciles into the wrong map key and shows the wrong footer
        // noun, and both would look plausible.
        assertThrows(IllegalArgumentException.class, () -> new ArmorDefinition(
                "diamond_helmet", "Diamond Helmet", Rarity.UNCOMMON, "diamond_helmet", null, 3, List.of()));
        // Mutation: default a null slot to HEAD instead of throwing -> reddens.
    }

    // --- Defensive copying ----------------------------------------------------------------------

    @Test
    void theFlavourListIsCopiedSoALaterEditCannotReachTheLoadedPiece() {
        List<String> authored = new ArrayList<>(List.of("Cut from a single lattice."));
        ArmorDefinition a = new ArmorDefinition("diamond_helmet", "Diamond Helmet", Rarity.UNCOMMON,
                "diamond_helmet", ArmorSlot.HEAD, 3, authored);
        authored.add("appended after construction");
        assertEquals(1, a.flavor().size(), "the piece kept its own copy");
        // Mutation: assign `flavor = flavor` instead of List.copyOf -> reddens.
    }

    @Test
    void aNullFlavourListBecomesEmptyRatherThanExploding() {
        // The loader passes whatever the YAML held, and `flavor:` is an optional key.
        assertEquals(List.of(), armor(3).flavor());
        assertEquals(List.of(), new ArmorDefinition("x", "X", Rarity.COMMON, "m",
                ArmorSlot.FEET, 1, null).flavor());
        // Mutation: drop the null branch -> a file with no flavor key NPEs the whole loader
        // -> reddens.
    }
}
