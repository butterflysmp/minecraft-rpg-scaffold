package io.github.butterflysmp.rpg.paper.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reddening tests for the action-bar stats line. The format is pure, so it is pinned here rather
 * than boot-witnessed: a swapped field, a dropped colour, or a raw double reaching the string fails
 * in the fast suite instead of surfacing as a wrong bar on someone's screen.
 *
 * The child-index assertions ARE the field-order pin. When pass 3 inserts the defense field between
 * health and mana, these reddening -- and being made to look at them -- is the intended behaviour.
 *
 * Every expected string below was produced by EXECUTING the expression, not by reasoning about
 * Math.round. Each test names the mutation it forces red.
 */
class StatsBarTextTest {

    private static TextComponent part(Component bar, int index) {
        List<Component> parts = bar.children();
        return (TextComponent) parts.get(index);
    }

    // --- Field order and colour. Redden by swapping the two fields or dropping a colour. ---

    @Test
    void healthFieldLeadsInRed() {
        TextComponent health = part(StatsBarText.of(100, 100, 40, 100), 0);

        assertEquals("❤ 100/100", health.content(), "the health field leads, icon first");
        assertEquals(NamedTextColor.RED, health.color(), "and the whole field is red");
        // Mutation: swap the two fields, or drop HEALTH_COLOR -> reddens.
    }

    @Test
    void manaFieldFollowsInBlue() {
        TextComponent mana = part(StatsBarText.of(100, 100, 40, 100), 2);

        assertEquals("✦ 40/100", mana.content(), "the mana field follows, icon first");
        assertEquals(NamedTextColor.BLUE, mana.color(), "and the whole field is blue");
        // Mutation: swap the two fields, or drop MANA_COLOR -> reddens.
    }

    @Test
    void theTwoFieldsAreSeparatedBySpacing() {
        Component bar = StatsBarText.of(100, 100, 40, 100);

        assertEquals(3, bar.children().size(), "health, gap, mana -- exactly three parts today");
        assertEquals(StatsBarText.FIELD_GAP, part(bar, 1).content(),
                "spacing is the ONLY thing separating the fields; there is no divider glyph");
        // Mutation: delete the gap -> the fields collide into "❤ 100/100✦ 40/100" -> reddens.
    }

    // --- The numbers. Redden by hardcoding a max or formatting the raw double. ---

    @Test
    void aFullBarReadsFullOnBothStats() {
        Component bar = StatsBarText.of(100, 100, 100, 100);

        assertEquals("❤ 100/100", part(bar, 0).content());
        assertEquals("✦ 100/100", part(bar, 2).content(), "a full pool reads cur == max");
        // Mutation: an off-by-one in either cur or max -> reddens.
    }

    @Test
    void aPartFullBarReadsItsOwnNumbersOnEachStat() {
        // Deliberately different on both stats, and neither equal to the other's max: a bar that
        // read the wrong stat, or reused one field's numbers for both, would still pass if the two
        // fields happened to match.
        Component bar = StatsBarText.of(250, 400, 40, 100);

        assertEquals("❤ 250/400", part(bar, 0).content(), "health is part-full against ITS max");
        assertEquals("✦ 40/100", part(bar, 2).content(), "mana is part-full against ITS max");
        // Mutation: render mana from the health numbers (or vice versa) -> reddens.
    }

    @Test
    void maxHealthIsNotAssumedToBeTheHundredBase() {
        // Gear raises max HP above the 100 base. A bar that hardcoded DEFAULT_PLAYER_BASE would
        // show 100 here and be wrong for every player wearing +HP.
        assertEquals("❤ 400/400", part(StatsBarText.of(400, 400, 100, 100), 0).content());
        // Mutation: hardcode 100 as the health max -> reddens.
    }

    @Test
    void numbersRenderAsIntegersNotRawDoubles() {
        Component bar = StatsBarText.of(9.4, 10, 0.6, 100);

        assertEquals("❤ 9/10", part(bar, 0).content(), "9.4 rounds to 9, and no trailing .0 survives");
        assertEquals("✦ 1/100", part(bar, 2).content(), "0.6 rounds up to 1");
        // Mutation: format the raw double -> "❤ 9.4/10.0" -> reddens.
    }

    @Test
    void roundingIsHalfUpSoAlmostFullDisplaysAsFull() {
        // 99.5 renders as 100/100 while the store holds 99.5. This is display, not truth -- the same
        // rule the heart bar and the nameplate follow. Pinned so the behaviour is a decision rather
        // than a surprise: a player reading a full bar may still be a half point down.
        assertEquals("❤ 100/100", part(StatsBarText.of(99.5, 100, 100, 100), 0).content());
        assertEquals("✦ 74/100", part(StatsBarText.of(100, 100, 73.5, 100), 2).content(),
                "73.5 rounds half-up to 74");
        // Mutation: switch to Math.floor / a cast to int -> 99 and 73 -> reddens.
    }
}
