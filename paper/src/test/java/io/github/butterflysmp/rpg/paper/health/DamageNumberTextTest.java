package io.github.butterflysmp.rpg.paper.health;

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
        TextComponent text = (TextComponent) DamageNumberText.of(16.0, true);
        assertEquals("16", text.content(), "the number stays a number -- no marker, no decoration");
        assertEquals(NamedTextColor.YELLOW, text.color());
        // Mutation: leave the crit WHITE -> the popup stops distinguishing a crit at all, and since
        // the text is now identical between the two styles the colour is the only thing that can
        // say so -> reddens on the colour row.
    }

    @Test
    void aNormalHitIsUnchangedByTheCritBranchExisting() {
        TextComponent text = (TextComponent) DamageNumberText.of(8.0, false);
        assertEquals("8", text.content(), "no marker on a normal hit");
        assertEquals(NamedTextColor.WHITE, text.color());
        assertEquals(text.content(), ((TextComponent) DamageNumberText.of(8.0)).content(),
                "and the one-arg overload still means 'not a crit'");
        // Mutation: invert the branch -> every normal hit reads as a crit and the 15% signal becomes
        // meaningless noise -> reddens.
    }

    @Test
    void aCritRoundsTheSameWayANormalHitDoes() {
        assertEquals("13", ((TextComponent) DamageNumberText.of(12.7, true)).content());
        assertEquals(((TextComponent) DamageNumberText.of(12.7)).content(),
                ((TextComponent) DamageNumberText.of(12.7, true)).content(),
                "crit and normal differ ONLY by colour, so their text must be identical");
        // Mutation: format the crit's double raw while rounding the normal one -> "12.7" -> reddens.
    }
}
