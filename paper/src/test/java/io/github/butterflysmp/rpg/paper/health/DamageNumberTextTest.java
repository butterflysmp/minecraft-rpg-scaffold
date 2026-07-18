package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The damage-number format is pure, so it is pinned here rather than boot-witnessed (the wire format
 * that carries it is the boot surface). One colour, rounded integer. Each test names the mutation it
 * forces red.
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
}
