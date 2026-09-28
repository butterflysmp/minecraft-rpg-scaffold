package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

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

    // --- the gear score (M6, M7, M22) ----------------------------------------------------------

    /**
     * "[500] Zombie 500/500 ❤" -- M6's own example. The [GS] is AQUA (M22); the name and the numbers
     * carry NO colour of their own, so an aqua that leaks from the prefix onto the name reddens here;
     * the heart stays red.
     */
    @Test
    void aHostileMobReadsGsNameNumbersHeartWithOnlyTheGsInAqua() {
        Component plate = NameplateText.of(
                NameplateText.nameWithScore(OptionalInt.of(500), Component.text("Zombie")), 500, 500);

        assertEquals("[500] Zombie 500/500 ❤", PlainTextComponentSerializer.plainText().serialize(plate),
                "M6's example, verbatim");

        Component named = plate.children().get(0);
        assertNull(named.color(), "the parent of prefix+name is unstyled, so nothing inherits a colour");
        TextComponent prefix = (TextComponent) named.children().get(0);
        TextComponent name = (TextComponent) named.children().get(1);
        assertEquals("[500] ", prefix.content());
        assertEquals(NamedTextColor.AQUA, prefix.color(), "M22. Mutation: AQUA -> none reddens");
        assertEquals("Zombie", name.content());
        assertNull(name.color(), "the name keeps its own colour -- the aqua must not leak onto it");

        TextComponent numbers = (TextComponent) plate.children().get(1);
        assertNull(numbers.color(), "the numbers are unchanged (M22)");
        assertEquals(NamedTextColor.RED, plate.children().get(2).color(), "and the heart stays red");
    }

    @Test
    void aPassiveMobShowsNoGearScoreAtAll() {
        Component name = Component.text("Cow");
        assertSame(name, NameplateText.nameWithScore(OptionalInt.empty(), name),
                "M7: no GS on a passive mob's plate -- not an empty '[] ', nothing");
        assertEquals("Cow 50/50 ❤", PlainTextComponentSerializer.plainText().serialize(
                NameplateText.of(NameplateText.nameWithScore(OptionalInt.empty(), name), 50, 50)));
    }

    /**
     * The score is in the FIXED half: a health change rebuilds from the registered base name, so the
     * [GS] must survive the rebuild without being passed again.
     */
    @Test
    void theGearScoreSurvivesAHealthRebuild() {
        Component base = NameplateText.nameWithScore(OptionalInt.of(200), Component.text("Spider"));
        assertEquals("[200] Spider 97/160 ❤",
                PlainTextComponentSerializer.plainText().serialize(NameplateText.of(base, 97, 160)));
    }

    @Test
    void numbersRoundToWholeHearts() {
        Component text = NameplateText.of(Component.text("Cow"), 9, 10);
        assertEquals(" 9/10 ", ((TextComponent) text.children().get(1)).content(),
                "integer display, no trailing decimals");
        // Mutation: format the raw double -> " 9.0/10.0 " -> reddens.
    }
}
