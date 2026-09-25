package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The transfer rule's equivalence: what may trade a gear score with what.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class TransferKeyTest {

    @Test
    void everyFIGHTINGClassKeepsItsOwnKey_soAMageCannotFeedARanger() {
        assertEquals(TransferKey.MELEE, TransferKey.of(GearClass.MELEE, null));
        assertEquals(TransferKey.RANGER, TransferKey.of(GearClass.RANGER, null));
        assertEquals(TransferKey.MAGE, TransferKey.of(GearClass.MAGE, null));
        assertEquals(TransferKey.SHIELD, TransferKey.of(GearClass.SHIELD, null));

        // THE ARMOUR SLOT IS IGNORED FOR EVERY NON-ARMOUR KIND, which is what lets the adapter pass
        // null rather than inventing a slot for a sword. Staged with a REAL slot rather than null,
        // because "ignored" and "must be null" are different claims and only one of them is true.
        assertEquals(TransferKey.MELEE, TransferKey.of(GearClass.MELEE, ArmorSlot.HEAD),
                "a sword's key does not depend on a slot it does not have");
    }

    @Test
    void ARMOURSplitsFOURWays_theHalfGearClassCannotExpress() {
        // *** THIS IS THE WHOLE REASON THIS ENUM EXISTS. *** GearClass.ARMOR is ONE constant, so a
        // rule written on it would let a pair of boots hand its score to a helmet. Ben's rule is
        // "helmets to helmets", and only a four-way split can say it.
        assertEquals(TransferKey.ARMOR_HEAD, TransferKey.of(GearClass.ARMOR, ArmorSlot.HEAD));
        assertEquals(TransferKey.ARMOR_CHEST, TransferKey.of(GearClass.ARMOR, ArmorSlot.CHEST));
        assertEquals(TransferKey.ARMOR_LEGS, TransferKey.of(GearClass.ARMOR, ArmorSlot.LEGS));
        assertEquals(TransferKey.ARMOR_FEET, TransferKey.of(GearClass.ARMOR, ArmorSlot.FEET));

        // AND THE FOUR ARE PAIRWISE DIFFERENT, swept rather than spot-checked: the equality rows
        // above are individually satisfied by a build that returns ARMOR_HEAD for everything only
        // if three of them are also wrong, but a sweep says so in one assertion and keeps saying it
        // when a fifth slot lands.
        Set<TransferKey> armour = new HashSet<>();
        int checked = 0;
        for (ArmorSlot slot : ArmorSlot.values()) {
            armour.add(TransferKey.of(GearClass.ARMOR, slot));
            checked++;
        }
        assertEquals(ArmorSlot.values().length, armour.size(),
                "every armour slot must get its OWN key -- collapsing any two lets one piece feed "
                        + "another: " + armour);
        assertEquals(4, checked, "the sweep has to have actually run");
        // Mutation MUT13A-KEYARMOR: collapse all four ofArmor arms to ARMOR_HEAD.
        //
        // *** THIS ROW IS THE SOLE GUARD OF THE ARMOUR SPLIT, AND THAT WAS MEASURED RATHER THAN
        // ASSUMED. *** The mutation was predicted to redden here AND on
        // AnvilTransferTest.BOOTSCannotFeedAHELMET. It reddened ONLY here: that row stages its two
        // sides as TransferKey constants directly and never calls of(), so it tests the COMPARISON
        // and is blind to the DERIVATION.
        //
        // So deleting this row removes the only thing standing between a boots-feeds-helmet build
        // and a green suite -- with the helmet row still passing, which is what would make the loss
        // invisible. It is load-bearing for something its neighbours look like they cover.
    }

    @Test
    void allEIGHTKeysAreDISTINCT_soNoTwoKindsCanTradeByAccident() {
        // The enum's whole contract is an equivalence, and an equivalence with a duplicate in it
        // silently merges two classes of gear. Java gives distinctness for enum constants, so this
        // row is really about the SIZE: eight kinds are named, and nobody has quietly dropped one.
        Set<TransferKey> all = new HashSet<>(Set.of(TransferKey.values()));
        assertEquals(8, all.size(), "three fighting classes, a shield, and four armour slots");

        // AND NO ARMOUR KEY EQUALS A WEAPON KEY. Stated because the failure it guards against is a
        // reordering or a rename that makes two constants alias in some future refactor.
        assertNotEquals(TransferKey.ARMOR_HEAD, TransferKey.MELEE);
        assertNotEquals(TransferKey.ARMOR_FEET, TransferKey.ARMOR_HEAD);
    }

    @Test
    void everyKeyNamesItselfInThePLURAL_becauseTheSingularDoesNotSurviveBootsAndLeggings() {
        assertEquals("helmets", TransferKey.ARMOR_HEAD.plural());
        assertEquals("chestplates", TransferKey.ARMOR_CHEST.plural());
        assertEquals("leggings", TransferKey.ARMOR_LEGS.plural());
        assertEquals("boots", TransferKey.ARMOR_FEET.plural());
        assertEquals("shields", TransferKey.SHIELD.plural());
        assertEquals("melee weapons", TransferKey.MELEE.plural());
        assertEquals("ranged weapons", TransferKey.RANGER.plural());
        assertEquals("magic weapons", TransferKey.MAGE.plural());

        // EVERY KEY HAS ONE, AND NO TWO SHARE IT -- the sweep, so an added key cannot ship with a
        // blank or a duplicated noun and render "Both items must be ." to a player.
        Set<String> plurals = new HashSet<>();
        int checked = 0;
        for (TransferKey key : TransferKey.values()) {
            assertTrue(key.plural() != null && !key.plural().isBlank(),
                    key + " must name itself -- the mismatch sentence reads 'Both items must be "
                            + "<this>.'");
            plurals.add(key.plural());
            checked++;
        }
        assertEquals(TransferKey.values().length, plurals.size(),
                "two keys sharing a noun would render one refusal for two different rules");
        assertEquals(8, checked, "the sweep has to have actually run");
    }

    // --- the three throwing arms, each FED ITS BAD INPUT ON PURPOSE -----------------------------

    /**
     * *** THIS IS THE ROW THAT STOPS THE TOOL ARM BEING A DEAD GUARD. ***
     *
     * <p>Ben ruled the arm a throw rather than a null, because <b>a null key makes two tools compare
     * EQUAL</b> and tool-to-tool transfer becomes legal by the exact mechanism the rule forbids.
     * Nothing in production can reach it -- {@code carriesScore} refuses every tool first -- and
     * {@code CLAUDE.md}'s rule is that <i>a guard with no instances is not automatically a guard
     * that cannot fire</i>. So it is fired here.
     *
     * <p><b>The ORDERING that makes it unreachable is proven elsewhere</b>, by
     * {@code AnvilTransferTest}'s row requiring {@code NotScoreable} and not {@code KeyMismatch}.
     * This row proves the arm works; that one proves nothing arrives at it.
     */
    @Test
    void aTOOLTHROWSRatherThanReturningNoKey_becauseTwoAbsentKeysWouldCompareEQUAL() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> TransferKey.of(GearClass.TOOL, null));
        assertTrue(thrown.getMessage().contains("ordering was violated"),
                "the message must say the INVARIANT broke, not that the input was bad -- this is an"
                        + " assertion, not error handling: " + thrown.getMessage());

        // AND A SLOT DOES NOT RESCUE IT. A caller passing a slot alongside TOOL is still violating
        // the ordering, and an arm that quietly answered would be the null in a different costume.
        assertThrows(IllegalArgumentException.class,
                () -> TransferKey.of(GearClass.TOOL, ArmorSlot.HEAD));
    }

    @Test
    void anItemThatIsNONEOFOURSThrows_ratherThanGettingAKeyThatMatchesAnother() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> TransferKey.of(null, null));
        assertTrue(thrown.getMessage().contains("Unscoreable"),
                "the message must point at the state such an item should have been given: "
                        + thrown.getMessage());
    }

    @Test
    void armourWithNOSLOTThrows_becauseEveryArmorDefinitionCarriesOne() {
        // A null slot on ARMOR is a programming error rather than bad data -- EnchantCost's
        // asymmetry, applied here: data clamps, programming errors throw. The alternative would be
        // to pick a slot, and picking one would let a helmet feed boots.
        assertThrows(IllegalArgumentException.class,
                () -> TransferKey.of(GearClass.ARMOR, null));
    }

    /**
     * *** THE COMPILER IS THE GUARD FOR AN ACCESSORY, AND THIS ROW IS ONLY THE REMINDER. ***
     *
     * <p>{@code TransferKey.of} switches over {@link GearClass} with no default arm, so adding
     * {@code ACCESSORY} to that enum <b>fails to compile</b> until someone states its rule. There is
     * no test that can assert a compile error, so this row asserts the thing that makes the compile
     * error possible: that every constant alive today is handled, with no arm falling through.
     *
     * <p><b>It would go red for the right reason if an arm were replaced by a default</b> -- a
     * {@code default -> MELEE} would make a shield trade with a sword and this sweep would see it.
     */
    @Test
    void everyGEARCLASSAliveTodayResolvesOrThrows_noArmFallsThrough() {
        int checked = 0;
        for (GearClass kind : GearClass.values()) {
            if (kind == GearClass.TOOL || kind == GearClass.ACCESSORY) {
                assertThrows(IllegalArgumentException.class, () -> TransferKey.of(kind, null),
                        kind + " carries no gear score, so it has no key");
            } else if (kind == GearClass.ARMOR) {
                assertEquals(TransferKey.ARMOR_HEAD, TransferKey.of(kind, ArmorSlot.HEAD));
            } else {
                assertTrue(TransferKey.of(kind, null) != null, kind + " must resolve to a key");
            }
            checked++;
        }
        assertEquals(GearClass.values().length, checked, "the sweep has to have actually run");
        assertEquals(7, checked, "seven gear classes today -- ACCESSORY landed here AND at a "
                + "compile error in TransferKey.of, which is the guard that matters");
    }
}
