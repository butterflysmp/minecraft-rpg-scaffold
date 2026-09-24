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
 * <h2>Two layouts, both pinned</h2>
 * The defense field renders only when there is defense, so there are two shapes to cover: 3 children
 * unarmored (health, gap, mana) and 5 armored (health, gap, defense, gap, mana), with mana at index
 * 2 or 4 accordingly. The tests below are split into those two groups.
 *
 * <p><b>An honest note about what the old tests did and did not catch.</b> Pass 2 predicted that
 * adding the defense field would redden the child-index assertions here. That assumed an
 * unconditional field. Because the field is conditional, every pre-existing test -- all of which pass
 * defense 0 -- kept its indices and stayed green once the new parameter was threaded through; the
 * only forced signal was a compile break. Their greenness is therefore evidence about the UNARMORED
 * bar only. The armored layout is covered by the second group, written for it. Do not delete those
 * on the grounds that the first group already passes.
 *
 * Every expected string below was produced by EXECUTING the expression, not by reasoning about
 * Math.round -- including the 0.49/0.5 visibility boundary. Each test names the mutation it forces
 * red.
 */
class StatsBarTextTest {

    private static TextComponent part(Component bar, int index) {
        List<Component> parts = bar.children();
        return (TextComponent) parts.get(index);
    }

    // --- The unarmored bar: two fields, three children -----------------------------------------

    @Test
    void healthFieldLeadsInRed() {
        TextComponent health = part(StatsBarText.of(100, 100, 0, 0, 0, 40, 100), 0);

        assertEquals("❤ 100/100", health.content(), "the health field leads, icon first");
        assertEquals(NamedTextColor.RED, health.color(), "and the whole field is red");
        // Mutation: swap the fields, or drop HEALTH_COLOR -> reddens.
    }

    @Test
    void manaFieldFollowsInBlue() {
        TextComponent mana = part(StatsBarText.of(100, 100, 0, 0, 0, 40, 100), 2);

        assertEquals("✦ 40/100", mana.content(), "the mana field follows, icon first");
        assertEquals(NamedTextColor.BLUE, mana.color(), "and the whole field is blue");
        // Mutation: swap the fields, or drop MANA_COLOR -> reddens.
    }

    @Test
    void theTwoFieldsAreSeparatedBySpacing() {
        Component bar = StatsBarText.of(100, 100, 0, 0, 0, 40, 100);

        assertEquals(3, bar.children().size(), "health, gap, mana -- three parts with no defense");
        assertEquals(StatsBarText.FIELD_GAP, part(bar, 1).content(),
                "spacing is the ONLY thing separating the fields; there is no divider glyph");
        // Mutation: delete the gap -> the fields collide into "❤ 100/100✦ 40/100" -> reddens.
    }

    @Test
    void aFullBarReadsFullOnBothStats() {
        Component bar = StatsBarText.of(100, 100, 0, 0, 0, 100, 100);

        assertEquals("❤ 100/100", part(bar, 0).content());
        assertEquals("✦ 100/100", part(bar, 2).content(), "a full pool reads cur == max");
        // Mutation: an off-by-one in either cur or max -> reddens.
    }

    @Test
    void aPartFullBarReadsItsOwnNumbersOnEachStat() {
        // Deliberately different on both stats, and neither equal to the other's max: a bar that
        // read the wrong stat, or reused one field's numbers for both, would still pass if the two
        // fields happened to match.
        Component bar = StatsBarText.of(250, 400, 0, 0, 0, 40, 100);

        assertEquals("❤ 250/400", part(bar, 0).content(), "health is part-full against ITS max");
        assertEquals("✦ 40/100", part(bar, 2).content(), "mana is part-full against ITS max");
        // Mutation: render mana from the health numbers (or vice versa) -> reddens.
    }

    @Test
    void maxHealthIsNotAssumedToBeTheHundredBase() {
        // Gear raises max HP above the 100 base. A bar that hardcoded DEFAULT_PLAYER_BASE would
        // show 100 here and be wrong for every player wearing +HP.
        assertEquals("❤ 400/400", part(StatsBarText.of(400, 400, 0, 0, 0, 100, 100), 0).content());
        // Mutation: hardcode 100 as the health max -> reddens.
    }

    @Test
    void numbersRenderAsIntegersNotRawDoubles() {
        Component bar = StatsBarText.of(9.4, 10, 0, 0, 0, 0.6, 100);

        assertEquals("❤ 9/10", part(bar, 0).content(), "9.4 rounds to 9, and no trailing .0 survives");
        assertEquals("✦ 1/100", part(bar, 2).content(), "0.6 rounds up to 1");
        // Mutation: format the raw double -> "❤ 9.4/10.0" -> reddens.
    }

    @Test
    void roundingIsHalfUpSoAlmostFullDisplaysAsFull() {
        // 99.5 renders as 100/100 while the store holds 99.5. This is display, not truth -- the same
        // rule the heart bar and the nameplate follow. Pinned so the behaviour is a decision rather
        // than a surprise: a player reading a full bar may still be a half point down.
        assertEquals("❤ 100/100", part(StatsBarText.of(99.5, 100, 0, 0, 0, 100, 100), 0).content());
        assertEquals("✦ 74/100", part(StatsBarText.of(100, 100, 0, 0, 0, 73.5, 100), 2).content(),
                "73.5 rounds half-up to 74");
        // Mutation: switch to Math.floor / a cast to int -> 99 and 73 -> reddens.
    }

    // --- The armored bar: three fields, five children ------------------------------------------

    @Test
    void theDefenseFieldSitsBetweenHealthAndManaInLime() {
        // The layout decision: defense is the MIDDLE field, not appended after mana. Lime is
        // Adventure's GREEN; Adventure's DARK_GREEN is Minecraft's darker green. Picking the wrong
        // one of the two ships the wrong colour, and only an assertion catches it.
        Component bar = StatsBarText.of(100, 100, 0, 0, 20, 40, 100);

        TextComponent defense = part(bar, 2);
        assertEquals("⛨ 20", defense.content(), "the defense field, icon first, between the other two");
        assertEquals(NamedTextColor.GREEN, defense.color(), "in lime -- Adventure GREEN is #55FF55");
        // Mutation: append defense after mana, or use DARK_GREEN -> reddens.
    }

    @Test
    void anArmoredBarHasFiveChildrenAndPushesManaToIndexFour() {
        // THE armored-layout pin, and the one the pre-existing tests do NOT cover: every one of them
        // passes defense 0 and so never builds this shape at all.
        Component bar = StatsBarText.of(100, 100, 0, 0, 20, 40, 100);

        assertEquals(5, bar.children().size(), "health, gap, defense, gap, mana");
        assertEquals("❤ 100/100", part(bar, 0).content(), "health still leads");
        assertEquals("✦ 40/100", part(bar, 4).content(), "and mana has moved to index 4");
        assertEquals(NamedTextColor.BLUE, part(bar, 4).color(), "still blue after the move");
        // Mutation: keep mana at index 2 with defense inserted -> the fields swap places -> reddens.
    }

    @Test
    void everyPairOfFieldsIsSeparatedOnTheArmoredBarToo() {
        Component bar = StatsBarText.of(100, 100, 0, 0, 20, 40, 100);

        assertEquals(StatsBarText.FIELD_GAP, part(bar, 1).content(), "health | defense");
        assertEquals(StatsBarText.FIELD_GAP, part(bar, 3).content(), "defense | mana");
        // Mutation: emit one gap for the whole bar -> the defense field collides with mana -> reddens.
    }

    @Test
    void theDefenseFieldIsAbsentEntirelyRatherThanReadingZero() {
        // The bar refused to carry a placeholder defense field before the stat existed, on the
        // grounds that a readout showing 0 when nothing is measured is indistinguishable from a
        // working one that measured zero. Now the stat exists, an unarmored player genuinely
        // measures 0 -- and the field still does not render, because the number would say nothing.
        Component bar = StatsBarText.of(100, 100, 0, 0, 0, 40, 100);

        assertEquals(3, bar.children().size(), "no defense, no field, no gap for it");
        for (Component child : bar.children()) {
            assertFalse(((TextComponent) child).content().contains(StatsBarText.SHIELD),
                    "the shield glyph appears nowhere on an unarmored bar");
        }
        // Mutation: render the field unconditionally -> "⛨ 0" appears and the size is 5 -> reddens.
    }

    @Test
    void theFieldAppearsExactlyWhenItsRoundedValueReachesOne() {
        // The visibility gate is on the ROUNDED value, not the raw one, so the bar can never print
        // "⛨ 0". The boundary was executed, not reasoned: Math.round(0.49) is 0 and Math.round(0.5)
        // is 1. Gating on the raw value instead would render "⛨ 0" for anything in (0, 0.5).
        assertEquals(3, StatsBarText.of(100, 100, 0, 0, 0.49, 40, 100).children().size(),
                "0.49 would print as 0, so no field");
        assertFalse(StatsBarText.showsDefense(0.49), "and the gate agrees");

        Component justShowing = StatsBarText.of(100, 100, 0, 0, 0.5, 40, 100);
        assertEquals(5, justShowing.children().size(), "0.5 rounds to 1, so the field appears");
        assertEquals("⛨ 1", part(justShowing, 2).content(), "showing the 1 it rounded to");
        assertTrue(StatsBarText.showsDefense(0.5), "and the gate agrees");
        // Mutation: gate on `defense > 0` instead of the rounded value -> 0.49 renders "⛨ 0" -> reddens.
    }

    @Test
    void theDefenseFieldShowsRawPointsNotTheReductionFraction() {
        // The number is the INPUT (armor points you can raise); the armor bar beside it is the
        // EFFECT (the reduction those points buy). A field that showed 17% here would duplicate the
        // bar and leave the player no way to read what they actually carry.
        assertEquals("⛨ 20", part(StatsBarText.of(100, 100, 0, 0, 20, 40, 100), 2).content(),
                "full diamond reads 20 points, NOT the ~17% reduction it buys");
        assertEquals("⛨ 3", part(StatsBarText.of(100, 100, 0, 0, 3, 40, 100), 2).content(),
                "a lone helmet reads 3");
        // Mutation: render damageReduction()*100 -> "⛨ 17" -> reddens.
    }

    @Test
    void defenseHasNoMaximumSoItRendersAsASingleNumber() {
        // Health and mana are cur/max pools; defense is not. Rendering it through the pool formatter
        // would invent a ceiling the stat does not have.
        String content = part(StatsBarText.of(100, 100, 0, 0, 20, 40, 100), 2).content();

        assertFalse(content.contains("/"), "no slash: defense is a single value, not a pool");
        assertEquals("⛨ 20", content);
        // Mutation: reuse the cur/max formatter -> "⛨ 20/20" -> reddens.
    }

    // --- The quiver field: present/absent, crossed with defense --------------------------------
    //
    // FOUR LAYOUTS NOW, NOT TWO, and every row below names which one it is in. Every expected value
    // was produced by EXECUTING the expression and reading the children back -- the same rule the
    // class javadoc states, and it is what established that the two 5-child layouts put DIFFERENT
    // fields at index 2.

    @Test
    void theQuiverFieldSitsBetweenHealthAndDefenseInAqua() {
        // Layout: defense AND quiver -- seven children, the widest bar there is.
        Component bar = StatsBarText.of(100, 100, 8, 25, 20, 40, 100);

        assertEquals(7, bar.children().size(), "health gap quiver gap defense gap mana");
        TextComponent quiver = part(bar, 2);
        assertEquals("➹ 8/25", quiver.content(), "the quiver field, icon first, loaded/max");
        assertEquals(NamedTextColor.AQUA, quiver.color(), "and the whole field is aqua");
        assertEquals("⛨ 20", part(bar, 4).content(), "defense FOLLOWS the quiver -- Ben's order");
        assertEquals("✦ 40/100", part(bar, 6).content(), "mana is last, as it always is");
        // Mutation: render the quiver after defense -> indices 2 and 4 swap -> reddens.
    }

    @Test
    void theQuiverFieldStillSitsAtIndexTwoWithNoDefense() {
        // Layout: quiver, NO defense -- five children. This is the fresh-spawn bar with a Boltor.
        Component bar = StatsBarText.of(100, 100, 8, 25, 0, 40, 100);

        assertEquals(5, bar.children().size(), "health gap quiver gap mana");
        assertEquals("➹ 8/25", part(bar, 2).content(), "the quiver closes up against health");
        assertEquals("✦ 40/100", part(bar, 4).content(), "and mana follows it directly");
        // Mutation: gate the quiver on defense being shown -> 3 children -> reddens.
    }

    /**
     * *** THE ROW THAT EXISTS BECAUSE 5 CHILDREN NO LONGER MEANS "ARMORED". ***
     *
     * <p>Both five-child layouts put mana at index 4, so no mana assertion can tell them apart, and
     * both were "the armored bar" to every row written before the quiver field. <b>Index 2 is the
     * only thing that differs</b>, and a bar that rendered the wrong one of the two conditional
     * fields would satisfy every other assertion in this file.
     */
    @Test
    void theTwoFiveChildLayoutsAreDifferentBarsAndIndexTwoIsWhatSeparatesThem() {
        Component quiverOnly = StatsBarText.of(100, 100, 8, 25, 0, 40, 100);
        Component defenseOnly = StatsBarText.of(100, 100, 0, 0, 20, 40, 100);

        assertEquals(quiverOnly.children().size(), defenseOnly.children().size(),
                "the premise of this row: the two layouts are INDISTINGUISHABLE by child count");
        assertEquals("✦ 40/100", part(quiverOnly, 4).content(), "and by mana's index, too");
        assertEquals("✦ 40/100", part(defenseOnly, 4).content());

        assertEquals("➹ 8/25", part(quiverOnly, 2).content());
        assertEquals("⛨ 20", part(defenseOnly, 2).content());
        assertNotEquals(part(quiverOnly, 2).content(), part(defenseOnly, 2).content(),
                "index 2 is the ONLY place these two bars differ -- read it, not the count");
        // Mutation: render the quiver with the SHIELD icon, or vice versa -> reddens here and
        // nowhere else, because every count and every other index still matches.
    }

    @Test
    void noQuiverMeansNoField_soANonQuiverWeaponLeavesTheBarUnchanged() {
        // capacity 0 is the "this weapon has no magazine" contract, matching WeaponLoreLines.quiverLine.
        assertEquals(3, StatsBarText.of(100, 100, 0, 0, 0, 40, 100).children().size(),
                "no defense and no quiver is the original two-field bar");
        assertEquals(5, StatsBarText.of(100, 100, 0, 0, 20, 40, 100).children().size(),
                "no quiver with defense is the pre-existing armored bar, unchanged");
        assertFalse(StatsBarText.showsQuiver(0), "capacity 0 -> no field");
        assertFalse(StatsBarText.showsQuiver(-1), "a negative capacity is not a magazine either");
        // Mutation: render the field unconditionally -> "➹ 0/0" appears on every player -> reddens.
    }

    /**
     * *** AN EMPTY MAGAZINE SHOWS, AND THIS IS THE OPPOSITE RULE TO DEFENSE. ***
     *
     * <p>{@code showsDefense} hides at zero because zero defense is nothing to report. A quiver at
     * zero is the most important thing on the bar -- it is WHY the weapon will not fire. Hiding it
     * would remove the readout exactly when the player is looking for it.
     *
     * <p>The natural extension of {@code showsDefense} to this field is the defect: one line, reads
     * consistent, and it deletes the field at the only moment it matters.
     */
    @Test
    void anEmptyMagazineRendersZeroRatherThanVanishing() {
        Component bar = StatsBarText.of(100, 100, 0, 25, 20, 40, 100);

        assertEquals(7, bar.children().size(), "the field is still there at 0 loaded");
        assertEquals("➹ 0/25", part(bar, 2).content(), "an empty quiver READS empty");
        assertTrue(StatsBarText.showsQuiver(25), "presence keys on CAPACITY, never on the count");
        // Mutation: gate showsQuiver on the loaded count -> the field vanishes when empty -> reddens.
    }

    @Test
    void theQuiverCountIsNotRoundedThroughADouble() {
        // The magazine is integral at the source (QuiverState.roundsRemaining/capacity are int), and
        // a caller reading 8/25 must never see 8/25 arrive as 7 or 26 via a float path.
        assertEquals("➹ 1/1", part(StatsBarText.of(100, 100, 1, 1, 20, 40, 100), 2).content());
        assertEquals("➹ 12/12", part(StatsBarText.of(100, 100, 12, 12, 20, 40, 100), 2).content());
        assertEquals("➹ 7/8", part(StatsBarText.of(100, 100, 7, 8, 20, 40, 100), 2).content(),
                "the Boltor's partial load -- GATE-quiver-ammo row 1's own 7/8");
        // Mutation: route the pair through field(String, double, double) -> still passes, which is
        // why this row is about the VALUES and the int overload is argued for in the source instead.
    }
}
