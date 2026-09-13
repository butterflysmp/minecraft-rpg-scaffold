package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.QuiverSize;
import io.github.butterflysmp.rpg.core.combat.stat.HealthState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Expanded Quiver: the enchant that binds {@link EnchantEffect#QUIVER_SIZE} to the already-shipped
 * {@link QuiverSize} mechanism.
 *
 * <h2>WHAT THIS FILE GUARDS THAT NOTHING ELSE DOES</h2>
 *
 * The arithmetic is {@code QuiverSizeTest}'s and is not re-asserted here. What is new with this
 * enchant is that quiver size acquires a SECOND source, and both defects that creates are properties
 * of {@code Stat} rather than of any Bukkit scan -- so they are pinned in core, where a unit test can
 * reach them, exactly as {@code GrowthTest} pins the same pair for max health.
 *
 * <ul>
 *   <li>{@link #twoSourcesInOneSlotMustNotShareAKeyOrOneSILENTLYERASESTheOther} -- MUT-PREFIX.
 *   <li>{@link #bothSourcesMustSurviveOneReconcileRatherThanTwo} -- MUT-MERGE.
 * </ul>
 *
 * <h2>THE STAGING IS CHOSEN SO NO TWO QUANTITIES COLLIDE</h2>
 *
 * Authored <b>8</b> (the Boltor), enchant <b>+2</b>, instrument <b>+19</b>, resolving to <b>10</b>,
 * <b>27</b> and <b>29</b>. Every number a row reads differs from every other, so a transposition
 * between any two has nowhere to hide -- the rule {@code CLAUDE.md} states for expected values, not
 * only for fixture inputs. In particular {@code 8 + 2 != 8 + 19} and neither equals the sum, so a row
 * cannot pass by reading the wrong source.
 *
 * <p>Each test names the mutation it forces red.
 */
class ExpandedQuiverTest {

    private static final double EPS = 1e-9;

    /** The Boltor's authored magazine. */
    private static final int AUTHORED = 8;
    /** The enchant's contribution at the tier under test. */
    private static final int ENCHANT_ARROWS = 2;
    /** {@code QuiverSizeModifierItems.DEFAULT_BOOST}, the instrument's. Deliberately unequal. */
    private static final int INSTRUMENT_ARROWS = 19;

    // --- The binding content depends on ------------------------------------------------------------

    @Test
    void theContentNameResolvesToTheConstant() {
        // expanded_quiver.yml says `effect: quiver_size`. If this name does not resolve, EnchantLoader
        // throws and the file becomes a named, skipped enchant -- which looks exactly like an enchant
        // that was never authored.
        assertSame(EnchantEffect.QUIVER_SIZE, EnchantEffect.fromName("quiver_size"));
        assertSame(EnchantEffect.QUIVER_SIZE, EnchantEffect.fromName("QUIVER_SIZE"));
        // Mutation: rename the constant, or make fromName case-sensitive -> reddens.
    }

    // --- THE SIGN TRAP ------------------------------------------------------------------------------

    @Test
    void theDirectionIsPOSITIVESoZeroAndNegativeBothDeclareNothing() {
        // Expanded Quiver is an INCREASE-ONLY stat: more arrows is better. Its gate is `> NONE`,
        // the same as Growth and ManaBank -- and deliberately NOT ReloadTime's `!= NONE`, whose sign
        // is inverted because more ticks is worse. Slice A2 shipped that confusion once, copying
        // `> NONE` into ReloadTime, which would have abolished reload-speed gear silently.
        assertFalse(QuiverSize.boosts(QuiverSize.NONE), "zero declares nothing");
        assertFalse(QuiverSize.boosts(-1), "NEGATIVE declares nothing -- content cannot shrink a quiver");
        assertTrue(QuiverSize.boosts(ENCHANT_ARROWS), "a positive bonus declares");
        // Mutation: `>` -> `!=` (the ReloadTime form) -> the negative row reddens, which is the
        // whole point: under `!=` a negative would become a modifier and shrink the magazine.
        // Mutation: `>` -> `>=` -> the zero row reddens; an unenchanted weapon would write a
        // no-op source on every scan.
    }

    // --- MUT-PREFIX: the headline, and it is a property of Stat -------------------------------------

    @Test
    void twoSourcesInOneSlotMustNotShareAKeyOrOneSILENTLYERASESTheOther() {
        // Stat.putModifier is put-or-REPLACE. QuiverSizeModifierItems keys by a BARE slot name and
        // walks ALL slots; the Expanded Quiver scan reads the MAIN HAND. A player holding a
        // quiver_size_boost instrument in the same hand as an Expanded Quiver weapon is ONE SLOT
        // producing TWO sources, and unprefixed they collide on "HAND".
        var collided = new HealthState(100, true);
        collided.setQuiverSizeModifier("HAND", INSTRUMENT_ARROWS);   // the instrument
        collided.setQuiverSizeModifier("HAND", ENCHANT_ARROWS);      // the enchant, same key
        assertEquals(ENCHANT_ARROWS, collided.quiverSizeBonusValue(), EPS,
                "same key REPLACES -- the instrument's 19 is gone and nothing said so");
        assertNotEquals(INSTRUMENT_ARROWS + ENCHANT_ARROWS, collided.quiverSizeBonusValue(), EPS,
                "and it is NOT the sum, which is what the player is owed");

        // Namespaced, both survive and sum: the player really is carrying two things that each add
        // arrows, and the resolved magazine must reflect both.
        var namespaced = new HealthState(100, true);
        namespaced.setQuiverSizeModifier("quiversize:HAND", INSTRUMENT_ARROWS);
        namespaced.setQuiverSizeModifier("expandedquiver:HAND", ENCHANT_ARROWS);
        assertEquals(INSTRUMENT_ARROWS + ENCHANT_ARROWS, namespaced.quiverSizeBonusValue(), EPS,
                "both sources contribute");
        assertEquals(29, QuiverSize.resolve(AUTHORED, namespaced.quiverSizeBonusValue()),
                "8 authored + 19 instrument + 2 enchant");
        // Mutation: alias the enchant scanner's SOURCE_PREFIX to "quiversize:" -> the live scan
        // produces the first case; this test is what says why that is wrong.
    }

    // --- MUT-MERGE: two scans, ONE reconcile --------------------------------------------------------

    @Test
    void bothSourcesMustSurviveOneReconcileRatherThanTwo() {
        // ModifierReconciler removes every applied source ABSENT from the map it is handed. So
        // reconciling the item scan and the enchant scan separately would have each wipe the other's
        // -- the stat would hold whichever ran last, silently and forever. QuiverSizeModifierItems'
        // own javadoc predicted this before the second source existed.
        //
        // Modelled here at the Stat level, which is where the erasure actually happens: a source that
        // is cleared because it was missing from someone else's map is gone exactly as if it had
        // never been written.
        var state = new HealthState(100, true);
        state.setQuiverSizeModifier("quiversize:HAND", INSTRUMENT_ARROWS);
        state.setQuiverSizeModifier("expandedquiver:HAND", ENCHANT_ARROWS);
        assertEquals(2, state.quiverSizeModifierCount(), "both sources applied");

        // What a SECOND reconcile against a map holding only the item scan would do.
        state.clearQuiverSizeModifier("expandedquiver:HAND");
        assertEquals(INSTRUMENT_ARROWS, state.quiverSizeBonusValue(), EPS,
                "the enchant's contribution is gone -- and the magazine silently loses 2 arrows");
        assertEquals(27, QuiverSize.resolve(AUTHORED, state.quiverSizeBonusValue()),
                "27, not the 29 the player is carrying the gear for");
        // Mutation: drop the putAll in PlayerHealthSystem so the two scans reconcile separately
        // -> the live stat behaves like the second half of this row. Only a staging that holds BOTH
        // sources at once can see it; either alone passes under the broken wiring.
    }

    // --- The resolved capacity, end to end ----------------------------------------------------------

    @Test
    void theEnchantAloneAddsWholeArrowsToTheAuthoredMagazine() {
        var state = new HealthState(100, true);
        state.setQuiverSizeModifier("expandedquiver:HAND", QuiverSize.contribution(ENCHANT_ARROWS));
        assertEquals(10, QuiverSize.resolve(AUTHORED, state.quiverSizeBonusValue()),
                "8 + 2, and 10 differs from every other quantity this file stages");
        // Mutation: make contribution() return 0, or resolve() ignore the bonus -> reddens.
    }
}
