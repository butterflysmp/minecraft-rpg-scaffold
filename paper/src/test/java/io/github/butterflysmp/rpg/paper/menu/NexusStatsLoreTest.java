package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.combat.StatsSheetValues;
import io.github.butterflysmp.rpg.paper.hud.StatsSheet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Nexus stats head's TEXT: what the tooltip says, which is the half a unit test can reach.
 *
 * <p>The head itself needs a live {@code Player}, a {@code SkullMeta} and
 * {@code Bukkit.createInventory} -- boot-gate-only, {@code GATE-nexus.md}'s slice 3 rows. Everything
 * below is a pure function of a {@link StatsSheetValues}.
 *
 * <p><b>FIXTURE VALUES ARE ALL DISTINCT, DELIBERATELY.</b> No two quantities this file reads are
 * equal, so a transposition between any two of them has nowhere to hide -- a row staged where two
 * numbers happen to agree survives swapping them and is measuring the fixture.
 *
 * <p>Each test names the mutation it forces red.
 */
class NexusStatsLoreTest {

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** Every number distinct, and none of them a default. */
    private static StatsSheetValues.Builder base() {
        return StatsSheetValues.builder()
                .maxHealth(137.5)
                .healthRegenPerSecond(0.2)
                .maxMana(211)
                .manaRegenPerSecond(1.3)
                .defense(43)
                .damage(19.75)
                .critChance(0.31)
                .critDamageBonus(0.87);
    }

    private static StatsSheetValues bare() {
        return base().build();
    }

    /** The same player holding a quiver weapon: 8 rounds, 60 ticks -- distinct from every stat. */
    private static StatsSheetValues withQuiver() {
        return base().quiver(8, 60).build();
    }

    private static List<Component> flatten(Component c) {
        List<Component> all = new ArrayList<>();
        all.add(c);
        for (Component child : c.children()) all.addAll(flatten(child));
        return all;
    }

    @Test
    void theLoreIsSTATSSHEETSOWNLINES_soTheHubCannotBecomeAThirdStatRenderer() {
        // THIS ROW LOOKS TAUTOLOGICAL AND IS THE MOST IMPORTANT ONE IN THE FILE. It pins the
        // DELEGATION. The defect it guards is not a wrong number today -- it is somebody later
        // "simplifying" this into its own loop over StatsSheetValues, at which point the hub and
        // /rpg stats become two answers to one question and drift silently, months apart, with no
        // test in a position to see it.
        assertEquals(StatsSheet.statLines(bare()), NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty()),
                "the head's lore must BE the stat sheet's lines, not a re-rendering of them");
        assertEquals(StatsSheet.statLines(withQuiver()),
                NexusStatsLore.lore(Optional.of(withQuiver()), OptionalLong.empty()),
                "and that holds with a quiver weapon held");
        // Mutation MUT-RERENDER: reimplement lore() with its own line loop -> reddens here.
    }

    @Test
    void theHeaderIsTHENAME_andIsNotAlsoALoreLine() {
        // A TOOLTIP IS NOTHING BUT FRAME, so it must not state its own name twice. The header
        // survives as the item's DISPLAY NAME, which is where a tooltip's title belongs.
        assertEquals(StatsSheetLines.HEADER, plain(NexusStatsLore.name()),
                "the head is named with the same constant the chat sheet heads with");

        for (Component line : NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty())) {
            assertFalse(plain(line).contains(StatsSheetLines.HEADER),
                    "no lore line may repeat the header: " + plain(line));
        }

        // AND THE HEADER IS THE ONLY DIFFERENCE -- the other half, without which "no header" is
        // equally consistent with the lore having dropped a stat line as well.
        List<Component> chat = StatsSheet.build(bare());
        List<Component> lore = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty());
        assertEquals(chat.size() - 1, lore.size(), "exactly ONE line fewer than the chat sheet");
        assertEquals(chat.subList(1, chat.size()), lore,
                "and it is the FIRST line -- the header -- that is missing, not some other one");
        // Mutation MUT-HEADER: lore() calls StatsSheet.build instead of statLines -> reddens here.
    }

    @Test
    void theQUIVERPairRidesAlong_presentWhenHeldAndABSENTWhenNot() {
        // DROPPING THE CONDITIONAL PAIR WOULD REINTRODUCE AT THE PRESENTATION LAYER THE DRIFT THE
        // PROJECTION ELIMINATES AT THE DATA LAYER: one input path, two different answers. And the
        // boot row comparing the two surfaces would pass whenever no quiver weapon is held, which
        // is most of the time -- so the gate could not catch it either.
        List<Component> without = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty());
        List<Component> with = NexusStatsLore.lore(Optional.of(withQuiver()), OptionalLong.empty());

        assertEquals(8, without.size(), "the eight build stats, and nothing else");
        assertEquals(10, with.size(), "the eight, plus the quiver pair");

        assertTrue(with.stream().anyMatch(c -> plain(c).contains(StatsSheetLines.QUIVER_SIZE_LABEL)),
                "the capacity line is present when a quiver weapon is held");
        assertTrue(with.stream().anyMatch(c -> plain(c).contains(StatsSheetLines.RELOAD_TIME_LABEL)),
                "and so is the reload line");
        assertFalse(without.stream().anyMatch(c -> plain(c).contains(StatsSheetLines.QUIVER_SIZE_LABEL)),
                "and ABSENT when none is -- 'Quiver 0' reads as a broken magazine");
        // Mutation MUT-QUIVER: drop the hasQuiver branch from StatsSheet.statLines -> reddens here.
    }

    @Test
    void anUNTRACKEDPlayerIsTOLDSo_andIsNeverShownZEROES() {
        // THE STATE A FRESHLY-JOINED PLAYER MEETS, and the hub is the surface they meet it on --
        // /rpg stats has to be typed, the head is just there. A readout showing 0 when nothing was
        // counted is indistinguishable from a working readout that measured zero, which is
        // MenuIcons.placeholder's own argument, in a place a player can see.
        List<Component> lore = NexusStatsLore.lore(Optional.empty(), OptionalLong.empty());

        assertEquals(1, lore.size(), "one line, not a zeroed sheet");
        assertEquals(StatsSheetLines.UNTRACKED, plain(lore.get(0)),
                "and it says what /rpg stats says, word for word, from the same constant");

        assertFalse(plain(lore.get(0)).matches(".*\\d.*"),
                "NO DIGIT may appear: any number here would read as a measurement");
        // Mutation MUT-UNTRACKED: return StatsSheet.statLines(builder().build()) -- a zeroed sheet
        // -- for the empty case -> reddens on all three assertions. APPLIED AND MEASURED.
    }

    @Test
    void everyLoreLineIsNONITALIC_orTheTooltipShipsEntirelyItalic() {
        // THE CONSTRAINT THAT MAKES REUSING A CHAT RENDERER FOR LORE LEGAL AT ALL. Minecraft
        // renders lore italic by DEFAULT, so a line built with a bare Component.text is left italic
        // via NOT_SET -- correct in chat, wrong in a tooltip, and wrong in the consumer nobody is
        // looking at while they write it. StatsSheet's javadoc records this as a requirement; this
        // is the check that it stays true.
        //
        // Walks CHILDREN too: a line is three appended components and only the root would be
        // caught by a shallow check.
        for (List<Component> lore : List.of(NexusStatsLore.lore(Optional.of(withQuiver()), OptionalLong.empty()),
                                            NexusStatsLore.lore(Optional.empty(), OptionalLong.empty()))) {
            for (Component line : lore) {
                for (Component node : flatten(line)) {
                    assertEquals(TextDecoration.State.FALSE, node.decoration(TextDecoration.ITALIC),
                            "italic must be explicitly FALSE, not NOT_SET: '" + plain(node) + "'");
                }
            }
        }
        assertEquals(TextDecoration.State.FALSE,
                NexusStatsLore.name().decoration(TextDecoration.ITALIC),
                "and the display name too");
        // Mutation MUT-ITALIC: build the untracked line with Component.text(..., RED) instead of
        // MenuIcons.line -> reddens here. APPLIED AND MEASURED.
    }

    // ------------------------------------------------------------ the progression block (slice 9)

    private static final long LEVEL_13 = 19_980L;
    private static final long LEVEL_99 = 11_642_250L;

    @Test
    void theProgressionBlockLeadsTheTooltip_andTheStatLinesFollowUnchanged() {
        List<Component> lore = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.of(LEVEL_13));
        List<Component> statsOnly = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty());

        assertEquals(3, lore.size() - statsOnly.size(), "three progression lines were added");
        assertEquals(statsOnly, lore.subList(3, lore.size()),
                "AND THE STAT LINES ARE UNTOUCHED BENEATH THEM -- without this, the block could be "
                        + "prepended by a method that also re-rendered the stats");

        assertTrue(plain(lore.get(0)).startsWith("Level"), "level leads: " + plain(lore.get(0)));
        assertTrue(plain(lore.get(1)).startsWith("Lifetime XP"));
        assertTrue(plain(lore.get(2)).startsWith("To Next"));
        // Mutation MUTBLOCK-APPEND: add the progression lines AFTER the stats -> reddens here.
    }

    @Test
    void theFIGURESAreTheCurvesAndShareTheStatSHEETSColumn() {
        List<Component> lore = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.of(LEVEL_13));

        assertEquals("Level        13", plain(lore.get(0)), "the grindstone's gate, padded to 13");
        assertEquals("Lifetime XP  19,980", plain(lore.get(1)), "thousands separator, Locale.ROOT");
        assertEquals("To Next      2,770", plain(lore.get(2)),
                "rung 13 -- the whole rung remains when you land exactly on the threshold");

        // THE COLUMN IS SHARED WITH THE EIGHT COMBAT LINES, and that is guaranteed by construction
        // rather than here: PlayerLevelLines has NO padder of its own and these lines pad through
        // StatsSheetLines.label, which PlayerLevelLinesTest pins. What is left to check is that the
        // three progression values line up with EACH OTHER.
        //
        // (A first draft located the stat column by searching a rendered stat line for "100". The
        // control guarding that lookup tripped -- the fixture's max health is not 100 -- and it is
        // removed rather than re-pointed, because the claim it was making is already made above.)
        assertEquals(plain(lore.get(0)).indexOf("13"), plain(lore.get(1)).indexOf("19,980"),
                "both progression values start in the same column");
    }

    @Test
    void atTheCAPThereIsNoToNextLine_andTheLevelCarriesTheMarkerInstead() {
        // *** THE PREDECESSOR PRINTED Long.MAX_VALUE HERE. *** The line is ABSENT rather than
        // carrying a placeholder: its column's subject is an amount remaining, and there is none.
        List<Component> lore = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.of(LEVEL_99));
        List<Component> statsOnly = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty());

        assertEquals(2, lore.size() - statsOnly.size(), "TWO lines at the cap, not three");
        assertEquals("Level        99 (MAX)", plain(lore.get(0)));
        assertEquals("Lifetime XP  11,642,250", plain(lore.get(1)));
        for (Component line : lore) {
            assertFalse(plain(line).contains("To Next"),
                    "no 'to next' line at the cap: " + plain(line));
            assertFalse(plain(line).contains("9223372036854775807"),
                    "and absolutely not the predecessor's sentinel");
        }

        // ONE SHORT OF THE CAP STILL HAS ALL THREE -- the boundary, without which "two lines" is
        // equally consistent with the line having been dropped everywhere.
        assertEquals(3, NexusStatsLore.lore(Optional.of(bare()), OptionalLong.of(LEVEL_99 - 1)).size()
                - statsOnly.size(), "level 98 still has a next level");
        // Mutation MUTCAP-ALWAYS: drop the isMaxed branch -> throws, reddening here.
    }

    @Test
    void anABSENTProfileRendersNOProgressionLines_ratherThanLevelONE() {
        // THE TWO SOURCES FAIL INDEPENDENTLY AND NEITHER EMPTY CASE MAY INVENT THE OTHER'S DATA.
        // A level-40 player whose profile failed to read must not be told they are level 1 by a
        // screen that otherwise looks correct -- the untracked notice's argument, one store over.
        List<Component> lore = NexusStatsLore.lore(Optional.of(bare()), OptionalLong.empty());
        for (Component line : lore) {
            assertFalse(plain(line).startsWith("Level"), "no level line: " + plain(line));
            assertFalse(plain(line).startsWith("Lifetime"), "no lifetime line: " + plain(line));
        }
        assertEquals(StatsSheet.statLines(bare()), lore, "the stat half is unaffected by it");

        // AND THE MIRROR: a loaded profile with UNTRACKED stats still renders progression.
        List<Component> noStats = NexusStatsLore.lore(Optional.empty(), OptionalLong.of(LEVEL_13));
        assertEquals(4, noStats.size(), "three progression lines plus the untracked notice");
        assertEquals("Level        13", plain(noStats.get(0)));
        assertTrue(plain(noStats.get(3)).contains(StatsSheetLines.UNTRACKED),
                "the untracked notice survives beneath them");
    }
}
