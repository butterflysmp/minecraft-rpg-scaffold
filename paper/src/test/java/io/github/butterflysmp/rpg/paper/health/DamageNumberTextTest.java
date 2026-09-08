package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.CritState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The damage-number format is pure, so it is pinned here rather than boot-witnessed (the wire format
 * that carries it is the boot surface). Rounded integer, in one of TWO styles since crit. Each test
 * names the mutation it forces red.
 */
class DamageNumberTextTest {

    @Test
    void showsTheRoundedAmountInWhite() {
        TextComponent text = (TextComponent) DamageNumberText.of(8.0);
        assertEquals("8", text.content(), "an 8-damage hit reads '8'");
        assertEquals(NamedTextColor.WHITE, text.color(), "one colour this pass -- white");
        // Mutation: change the colour / append units / format the raw double -> reddens.
    }

    @Test
    void roundsFractionalDamageToAWholeNumber() {
        assertEquals("13", ((TextComponent) DamageNumberText.of(12.7)).content(),
                "12.7 rounds to 13 -- integer display, no decimals");
        assertEquals("12", ((TextComponent) DamageNumberText.of(12.4)).content(), "12.4 rounds to 12");
        // Mutation: truncate instead of round -> 12.7 -> "12" -> reddens.
    }

    @Test
    void largeCustomNumbersAreCapFree() {
        assertEquals("5000", ((TextComponent) DamageNumberText.of(5000)).content(),
                "custom damage is cap-free, like custom health");
        // Mutation: clamp to a vanilla-scale number -> reddens.
    }

    // --- Crit styling: the only in-game evidence that the roll fired ---

    @Test
    void aCritIsYellowAndTheNumberItselfIsUnchanged() {
        TextComponent text = (TextComponent) DamageNumberText.of(16.0, CritState.CRIT);
        assertEquals("16", text.content(), "the number stays a number -- no marker, no decoration");
        assertEquals(NamedTextColor.YELLOW, text.color());
        // Mutation: leave the crit WHITE -> the popup stops distinguishing a crit at all, and since
        // the text is now identical between the two styles the colour is the only thing that can
        // say so -> reddens on the colour row.
    }

    @Test
    void aNormalHitIsUnchangedByTheCritBranchExisting() {
        TextComponent text = (TextComponent) DamageNumberText.of(8.0, CritState.NORMAL);
        assertEquals("8", text.content(), "no marker on a normal hit");
        assertEquals(NamedTextColor.WHITE, text.color());
        assertEquals(text.content(), ((TextComponent) DamageNumberText.of(8.0)).content(),
                "and the one-arg overload still means 'not a crit'");
        // Mutation: invert the branch -> every normal hit reads as a crit and the 15% signal becomes
        // meaningless noise -> reddens.
    }

    @Test
    void aCritRoundsTheSameWayANormalHitDoes() {
        assertEquals("13", ((TextComponent) DamageNumberText.of(12.7, CritState.CRIT)).content());
        assertEquals(((TextComponent) DamageNumberText.of(12.7)).content(),
                ((TextComponent) DamageNumberText.of(12.7, CritState.CRIT)).content(),
                "crit and normal differ ONLY by colour, so their text must be identical");
        // Mutation: format the crit's double raw while rounding the normal one -> "12.7" -> reddens.
    }

    // --- The glyph, and the two channels staying separate ----------------------------------------

    /** The glyph an element ships: a mark carrying its OWN colour, as fire.yml's damage_symbol does. */
    private static Component glyph() {
        return Component.text("*", NamedTextColor.GOLD);
    }

    private static Component firstChild(Component root) {
        return root.children().get(0);
    }

    private static Component lastChild(Component root) {
        return root.children().get(root.children().size() - 1);
    }

    @Test
    void theGLYPHKeepsItsOWNColourBesideAWHITENumberInTheSAMETree() {
        // THE ROW THE WHOLE THIRD-SHAPE DECISION RESTS ON, and it must assert BOTH colours in ONE
        // tree. Asserting them in two separate components would pass against an implementation that
        // colours the assembled root, because each piece is correct in isolation and only their
        // COMPOSITION is wrong.
        //
        // Adventure lets a child keep an explicit colour, so this works by inheritance rules -- which
        // is exactly why it is pinned. "Probably works by inheritance rules" is not a witness, and the
        // failure is silent: the glyph would simply turn yellow on every crit, undoing the reason the
        // glyph was chosen over colouring the number.
        Component root = DamageNumberText.of(28.0, CritState.NORMAL, glyph());

        assertNull(root.color(),
                "the ROOT wears no colour of its own -- that is what lets the children keep theirs");
        assertEquals(NamedTextColor.GOLD, firstChild(root).color(), "the glyph stays the ELEMENT's gold");
        assertEquals(NamedTextColor.WHITE, lastChild(root).color(), "the number stays white");
        assertEquals("28", ((TextComponent) lastChild(root)).content(), "and it is still the number");
        // Mutation: colour the assembled ROOT with the number's colour instead of the number child ->
        // root.color() is no longer null -> reddens on the first assertion, and the glyph would have
        // inherited it in game.
    }

    @Test
    void aCRITTurnsTheNUMBERYellowAndLEAVESTheGLYPHAlone() {
        // The pair to the row above, and the one that proves the two channels are independent rather
        // than merely both present. Same glyph, only the crit differs -- so a mutation that couples
        // them cannot pass by accident of the fixture.
        Component root = DamageNumberText.of(56.0, CritState.CRIT, glyph());

        assertEquals(NamedTextColor.GOLD, firstChild(root).color(),
                "a fire crit is still marked as FIRE -- the element does not vanish when it matters most");
        assertEquals(NamedTextColor.YELLOW, lastChild(root).color(), "and the number carries the crit");
        // Mutation: apply crit.isCrit() ? YELLOW : WHITE to the root -> the glyph reads yellow, this
        // row's first assertion reddens, and colour is back to meaning two things at once.
    }

    @Test
    void aNullGlyphRendersEXACTLYWhatItDidBeforeGlyphsExisted() {
        // Every elementless path -- a thorns reflect, fall damage, scorch's own burn tick -- and the
        // six shipped elements that have not been given a glyph yet. A bare number, no wrapper, no
        // leading space: the same component the two-argument form returns.
        Component bare = DamageNumberText.of(28.0, CritState.NORMAL, null);

        assertTrue(bare.children().isEmpty(), "no wrapper, no gap child -- just the number");
        assertEquals(NamedTextColor.WHITE, bare.color());
        assertEquals("28", ((TextComponent) bare).content());
        assertEquals(((TextComponent) DamageNumberText.of(28.0)).content(),
                ((TextComponent) bare).content(), "identical to the no-element form");
        // Mutation: always wrap, even with a null symbol -> children() is non-empty -> reddens, and
        // every non-elemental number in the game would gain a stray leading space.
    }
}
