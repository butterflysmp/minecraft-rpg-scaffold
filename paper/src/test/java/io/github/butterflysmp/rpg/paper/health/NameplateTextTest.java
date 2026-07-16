package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The nameplate text format is pure, so it is pinned here rather than boot-witnessed. It shows the
 * custom cur/max with a red heart, and the numbers are cap-free (a boss reading 5000/5000 is fine).
 * Each test names the mutation it forces red.
 */
class NameplateTextTest {

    @Test
    void showsNameSlashAndRedHeart() {
        Component text = NameplateText.of(Component.text("Zombie"), 20, 20);
        List<Component> parts = text.children();

        assertEquals("Zombie", ((TextComponent) parts.get(0)).content(), "the mob name leads");
        assertEquals(" 20/20 ", ((TextComponent) parts.get(1)).content(), "then cur/max as integers");
        TextComponent heart = (TextComponent) parts.get(2);
        assertEquals("❤", heart.content(), "then the U+2764 heart");
        assertEquals(NamedTextColor.RED, heart.color(), "and the heart is red");
        // Mutation: drop the RED color / use a different glyph -> reddens.
    }

    @Test
    void customNumbersAreCapFree() {
        Component text = NameplateText.of(Component.text("Boss"), 5000, 5000);
        assertEquals(" 5000/5000 ", ((TextComponent) text.children().get(1)).content(),
                "5000 is representable -- the whole reason health is custom (vanilla caps at 1024)");
        // Mutation: clamp the number to 1024 -> reddens.
    }

    @Test
    void numbersRoundToWholeHearts() {
        Component text = NameplateText.of(Component.text("Cow"), 9, 10);
        assertEquals(" 9/10 ", ((TextComponent) text.children().get(1)).content(),
                "integer display, no trailing decimals");
        // Mutation: format the raw double -> " 9.0/10.0 " -> reddens.
    }
}
