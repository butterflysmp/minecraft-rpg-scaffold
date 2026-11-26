package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scenarios the jar carries, against the gates they claim to drive.
 *
 * <p><b>THE PAIRING IS THE GUARD.</b> A scenario keyed {@code WB2} must name a {@code ### WB2 } heading in
 * {@code GATE-withered-shortbow.md} at the repo root, or a renamed row would be driven under its old name and its
 * readings filed against a row that no longer says what was predicted.
 */
class SelfTestScenariosTest {

    private static ScenarioLoader.Loaded shipped() {
        return ScenarioLoader.load(path -> SelfTestScenariosTest.class.getClassLoader().getResourceAsStream(path));
    }

    /**
     * GATE-broken-bottle's ONE command, against the SHIPPED gates: it parses with no refusal, and queues the bottle's setup
     * and nine rows, then summoner's setup, SM9R and SM16R2, then armor-sets' setup and AS15R. Fifteen, and nothing else.
     */
    @Test
    void theBottlesOneCommandQueuesItsGateAndTheThreeOwedRows() {
        var gates = shipped().gates();
        var parsed = SelfTestGateList.parse("broken-bottle+summoner.SM9R.SM16R2+armor-sets.AS15R", gates);
        assertEquals(null, parsed.refusal());
        List<String> queued = SelfTestGateList.queue(parsed, gates).stream().map(s -> s.gate() + "#" + s.row()).toList();
        assertEquals(List.of("broken-bottle#setup", "broken-bottle#BB0", "broken-bottle#BB1", "broken-bottle#BB2",
                "broken-bottle#BB3", "broken-bottle#BB4", "broken-bottle#BB5", "broken-bottle#BB6", "broken-bottle#BB7",
                "broken-bottle#BB8", "summoner#setup", "summoner#SM9R", "summoner#SM16R2", "armor-sets#setup",
                "armor-sets#AS15R"), queued);
    }

    @Test
    void everyShippedScenarioLoadsWithNoRefusal() {
        ScenarioLoader.Loaded loaded = shipped();
        assertEquals(List.of(), loaded.refusals());
        assertEquals(List.of("melee-cell", "level", "nexus-polish", "legacy-a", "legacy-b", "wither-status",
                "withered-shortbow", "sunder", "deft-hands", "mining-auto", "nexus-back", "dragon-bodies",
                "dragon-split", "dragon-growth", "dragon-blast", "dragon-beam", "dragon-teleport",
                        "dragon-shot", "dragon-blink", "dragon-skreel", "drag-additive", "fight-site", "fight-arena",
                        "fight-dragon", "fight-rules", "fight-end", "fight-down", "fight-revive", "fight-template",
                        "loot-roll", "sword-tiers", "sword-smithing",
                        "sword-convert", "remove-dev-weapons", "crystal-shard-staff", "knell-beam", "living-lantern", "breacher", "breacher-rework",
                        "vanilla-bows", "boltor-recipe", "unique-enchant-slot", "enchant-columns", "enchant-changes", "shatter-caps",
                        "focused-beam", "mage-guaranteed", "summoner", "armor-sets", "broken-bottle"),
                List.copyOf(loaded.gates().keySet()),
                "the fifty gates, in boot order (broken-bottle LAST, 2026-11-25, a NEW gate after armor-sets; armor-sets before it, 2026-11-25, a NEW gate after summoner; summoner before it,"
                        + " 2026-11-25, a NEW gate after mage-guaranteed;"
                        + " mage-guaranteed before it, the seat's brief of 2026-11-24, a NEW gate after focused-beam;"
                        + " focused-beam before it, the weapon filter and Focused Beam, 2026-11-24, a NEW gate"
                        + " after shatter-caps; shatter-caps before it, the Breacher's reach and Shatter Caps, 2026-11-24, a NEW gate"
                        + " after enchant-changes; enchant-changes before it, Ben's five changes to Airburst Flames and Lapis"
                        + " Crystal, 2026-11-23, a NEW gate after enchant-columns; enchant-columns before it, the enchant columns"
                        + " re-cut, 2026-11-23, a NEW gate after"
                        + " unique-enchant-slot; unique-enchant-slot before it, the unique enchant slot, 2026-11-23, a NEW gate"
                        + " after boltor-recipe; boltor-recipe before it, the Boltor's recipe, 2026-11-22, a NEW gate after"
                        + " vanilla-bows; vanilla-bows before it, the vanilla Bow and Crossbow and the quiver in the Nexus"
                        + " slot, 2026-11-22, a NEW gate after breacher-rework;"
                        + " breacher-rework before it, the Breacher reworked, 2026-11-22, a NEW gate after"
                        + " breacher; breacher before it, the Breacher, 2026-11-22, a NEW gate after living-lantern;"
                        + " living-lantern before it, the Living Lantern, 2026-11-22, a NEW gate after"
                        + " knell-beam; knell-beam before it, the Knell Beam, 2026-11-21, a NEW gate after"
                        + " crystal-shard-staff; crystal-shard-staff before it, the Crystal Shard Staff, 2026-11-21, a NEW gate after"
                        + " remove-dev-weapons; remove-dev-weapons before it, the four dev weapons deleted, 2026-11-21, a"
                        + " NEW gate after sword-convert; sword-convert before it, the sword ladder part h, 2026-11-20, a NEW gate"
                        + " after sword-smithing; sword-smithing before it, the sword ladder part g, 2026-11-20, a NEW gate"
                        + " after sword-tiers; sword-tiers before it, the sword ladder, 2026-11-20, a NEW gate after"
                        + " loot-roll; loot-roll before it, the seat's queue item 6, 2026-11-19, a NEW gate after"
                        + " fight-template, rebased onto it by the seat's part 2; fight-template before it, the seat's queue"
                        + " item 2, 2026-11-19, a NEW gate"
                        + " after fight-revive; fight-revive before it, step F9e, 2026-11-19, a NEW gate after fight-down;"
                        + " fight-down before it, step F9d, 2026-11-19, a NEW gate after fight-end;"
                        + " fight-end before it, step F9c, 2026-11-19, a NEW gate after fight-rules;"
                        + " fight-rules before it, step F9b, 2026-11-19, a NEW gate after fight-dragon;"
                        + " fight-dragon before it, step F9, 2026-11-18, a NEW gate after fight-arena;"
                        + " fight-arena before it, step F8b, 2026-11-18, a NEW gate after"
                        + " fight-site; fight-site before it, step F8a, 2026-11-18, in fight-mage's place: it"
                        + " re-predicts every row there; fight-mage LAST before it, steps F6 + F7, 2026-11-18, in fight-ring's place:"
                        + " it re-predicts every FR row; fight-ring before it, steps F4 + F5 of the dragon fight, 2026-11-18;"
                        + " drag-additive before it, the additive drag, 2026-11-18, in"
                        + " implosion-drag's place: it re-predicts every one of its rows; implosion-drag LAST before it, the drag, 2026-11-18; before it dragon-skreel, step R8, 2026-11-17; before it dragon-blink, step R7b, after dragon-shot, step R7a, 2026-11-17); nexus-back after mining-auto (2026-11-14), then dragon-bodies,"
                        + " dragon-split and dragon-growth (2026-11-15), then dragon-blast (step R3, 2026-11-15), then dragon-beam (step R5) and dragon-teleport LAST (step R6, 2026-11-17)");
    }

    /** The seat's CORE rows, one scenario each (2026-10-05). R0d is a boot log line and has none. */
    @Test
    void theScenariosAreExactlyTheCoreRows() {
        // Map.of takes at most ten pairs; the eleventh gate (nexus-back, 2026-11-14) is put after it.
        Map<String, List<String>> expected = new java.util.LinkedHashMap<>(Map.of(
                "melee-cell", List.of("MC1", "MC2", "MC3"),
                "level", List.of("CP2", "BN1", "WD1", "WD3", "LS1"),
                "nexus-polish", List.of("D1", "G1", "G3", "CM1", "CM2"),
                "legacy-a", List.of("SB2"),
                "legacy-b", List.of("LB2", "LB5"),
                "wither-status", List.of("WS-C", "WS1", "WS2b", "WS2c", "WS3"),
                "withered-shortbow", List.of("WB1", "WB2", "WB3"),
                "sunder", List.of("SU1", "SU2", "SU4", "SU5", "SU6", "SU7", "SU8", "SU9", "SU10", "SU11", "SU12"),
                // DH6 RETIRED 2026-11-21: its weapon, quiver_stone, was deleted (the seat's brief, ruling 1).
                "deft-hands", List.of("DH1", "DH3"),
                "mining-auto", List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12")));
        expected.put("nexus-back", List.of("NB1", "NB2", "NB3", "NB4", "NB5", "NB6", "NB7"));
        // The three dragon gates' rows are GATE-dragon-fix.md's since the 2026-11-15 boot: each re-predicts the old
        // row it is named for (DB8 and DB9 read as predicted and are not re-run).
        // STEP R3 TOOK EIGHT OF THEM OUT (the seat's brief of 2026-11-16; GATE-dragon-fix.md's dated note): DB10R2 and
        // DQ1 retired (a live Phage now blows), and DS1R2, DS2R2, DS5R2, DS6R2, DG6R2 and DG9R2 re-predicted as R3
        // rows in GATE-dragon-blast.md, where their split children are born frozen (or, in DS1R3, thawed on purpose).
        expected.put("dragon-bodies", List.of("DB1R2", "DB2R2", "DB3R2", "DB4R2", "DB5R2",
                "DB11R2"));
        expected.put("dragon-split", List.of("DS3R2", "DS4R2"));
        expected.put("dragon-growth", List.of("DG1R2", "DG2R2", "DG3R2", "DG4R2", "DG5R2", "DG7R2", "DG8R2"));
        // Step R3's rows, RE-PREDICTED by the follow-up of 2026-11-17 (GATE-dragon-blast-2.md): each is a new row
        // named for the GATE-dragon-blast.md row it supersedes, with the next suffix. BQ1R2 is Ben's three questions.
        // Then RE-PREDICTED again by the void implosion (GATE-void-implosion.md, 2026-11-17): the next suffix for every
        // row, the implosion's new rows IM1-IM4, and the Knell's slab row KS1 (F19), in the same file so one run reads all.
        // Then THE DRAG (GATE-implosion-drag.md, 2026-11-18) MOVED every row that blows a Phage to its own file,
        // re-predicted with the next suffix, and retired BQ1R3: what stays here blows nothing.
        expected.put("dragon-blast", List.of("BL5R3", "BL8R3", "KS1", "DS2R5", "DS5R5", "DS6R5", "DG6R5", "DG9R5"));
        // Then THE ADDITIVE DRAG (GATE-drag-additive.md, 2026-11-18) re-predicted every one of implosion-drag's rows with
        // the next suffix (their staging unchanged), added SD5 and CS1, and replaced BQ2 with BQ3; implosion-drag retired.
        expected.put("drag-additive", List.of("BL1R5", "BL2R5", "BL3R5", "BL4R5", "BL6R5", "BL7R5", "BL9R5", "SD5",
                "CS1", "IM1R3", "IM2R3", "IM3R3", "IM4R3", "IM5R3", "IM6R3", "FZ1R3", "FZ3R3", "FZ5R3", "DS1R7",
                "DR1R2", "DR2R2", "BQ3"));
        // Step R5 (GATE-dragon-beam.md, 2026-11-17): eleven AUTO rows and Ben's three questions, in one.
        // Step R6 (GATE-dragon-teleport.md) moved KB10 and KB11 out, re-predicted there as KB10R2 and KB11R2.
        // The Knell follow-up (GATE-dragon-knell-2.md, 2026-11-17) re-predicted every row of both Knell files, each
        // renamed with the next suffix; KB2R2 is the root read on a WALKING Knell, and TP14 is new.
        // Ben's re-timing (GATE-dragon-knell-3.md, 2026-11-17) re-predicted every row of both Knell files again, each
        // renamed with the next suffix; WA1 (the Knell walks again after a beam) is new, and KQ1R3 asks two questions.
        // Ben's second bell (GATE-dragon-knell-4.md, 2026-11-17): every beam row prints the sound lines, so every one
        // is renamed with the next suffix; KQ1R4 beams once and asks the two bell questions.
        // The shot in the middle (GATE-dragon-knell-5.md, 2026-11-17): a 24-tick telegraph and a 24-tick delay, the
        // first bell back at 1.0; every row of both files renamed with the next suffix.
        expected.put("dragon-beam", List.of("KB1R5", "KB2R5", "WA1R3", "KB3R5", "KB4R5", "KB5R5", "KB6R5", "KB7R5",
                "KB8R5", "KB9R5", "KQ1R5"));
        // Step R6 (GATE-dragon-teleport.md, 2026-11-17, stacked on R5): twelve new AUTO rows, the two re-predictions,
        // and Ben's three questions in one.
        // Ben's one more number and his second bell (GATE-dragon-knell-4.md, 2026-11-17): every teleport row is renamed
        // with the next suffix (the move at fire + 28, and the new sound lines in every row).
        expected.put("dragon-teleport", List.of("KB10R6", "TP2R5", "TP3R5", "TP4R5", "TP5R5", "TP6R5", "TP7R5", "TP8R5",
                "TP9R5", "TP10R5", "TP11R5", "TP12R5", "TP13R5", "TP14R4", "KB11R6", "TQ1R5"));
        // Step R7a (GATE-dragon-shot.md, 2026-11-17): the Phalanx's shot, twelve AUTO rows; Ben's three questions; and the
        // Knell's one flash, re-asked (KQ1R6).
        // Then the re-prediction after the boot of 8ec16e89 (the seat's rulings on #201): the rows that changed take the next
        // suffix, the new rows are SH13-SH16, and the rows read as predicted (SH5, SH6, SH8, SH10, SH11, KQ1R6) leave the file.
        expected.put("dragon-shot", List.of("SH1R2", "SH2R2", "SH3R2", "SH4R2", "SH7R2", "SH9R2", "SH12R2", "SH13", "SH14",
                "SH15", "SH16", "SQ1R2"));
        // Step R7b (GATE-dragon-blink.md): the shot's four carried items (BC1-BC4), the blink (BB1-BB5), the shield
        // (BS1-BS4) and Ben's row.
        expected.put("dragon-blink", List.of("BC1X", "BC1Z", "BC2A", "BC2B", "BC3", "BC4A", "BC4B", "BB1", "BB2", "BB3",
                "BB4", "BB5", "BS1", "BS2", "BS3", "BS4", "BQ1"));
        // Step R8 (GATE-dragon-skreel.md): DB6R3 and DB7R3 (re-predicting the bodies gate's DB6R2 and DB7R2, moved out of it),
        // the pair (PS3-PS8), the Phalanx's cooldown and R3 (PB1-PB3), and Ben's two rows.
        // Then its re-prediction after the boot of 45bc33ce: the rows that failed or change, re-named (DB6R4, PS6R2, PB1R2,
        // PB2R2, and the blink gate's BB4 as BB4R2), the band edge's two new rows (PS9, PS10), Ben's far end (PB4) and his
        // two rows (PQ3, PQ4). The rows that read as predicted and do not change left the file.
        expected.put("dragon-skreel", List.of("DB6R4", "PS6R2", "PS9", "PS10", "PS11", "PB1R2", "PB2R2", "PB4", "BB4R2", "PQ3",
                "PQ4"));
        ScenarioLoader.Loaded loaded = shipped();
        for (var gate : expected.entrySet()) {
            List<String> rows = new ArrayList<>();
            for (Scenario s : loaded.gates().get(gate.getKey())) if (!s.row().equals(Scenario.SETUP)) rows.add(s.row());
            assertEquals(gate.getValue(), rows, gate.getKey());
        }
        // 24 CORE rows on the seat's run sheet + WS-C (made CORE 2026-10-05) - R0d (a boot line, no scenario). Then +10, GATE-sunder.md, and +3, GATE-deft-hands.md (2026-11-06).
        // Then +1, SU8's death half (the seat's ruling, 2026-11-10). Then +12, GATE-mining-auto.md (2026-11-14). Then +7, GATE-nexus-back.md (2026-11-14).
        // Then GATE-dragon-bodies.md (2026-11-15). RECOUNTED from the files, not adjusted: in selftest/, the index's
        // gates (grep -cE '^  - ' index.yml) printed 12; their top-level keys (^[A-Za-z0-9-]+:) 80; their setups 12;
        // so 80 - 12 = 68. The same three commands at master 5b71e24b print 11, 68, 11: 57, this line's old value.
        // Then GATE-dragon-split.md (2026-11-15), RECOUNTED the same way: 13 gates, 87 keys, 13 setups, so 74. At
        // feat/dragon-bodies 974e4357 the same commands print 12, 80, 12: 68, this line's old value.
        // Then GATE-dragon-growth.md (2026-11-15), RECOUNTED the same way: 14 gates, 97 keys, 14 setups, so 83. At
        // feat/dragon-split 56a8bb00 the same commands print 13, 87, 13: 74, this line's old value.
        // Then GATE-dragon-fix.md (2026-11-15), RECOUNTED the same way: 14 gates, 96 keys, 14 setups, so 82. At
        // feat/dragon-growth 39bb2e90 the same commands print 14, 97, 14: 83, this line's old value.
        // Then GATE-dragon-blast.md (step R3, 2026-11-15), RECOUNTED the same way: 15 gates, 106 keys, 15 setups, so 91.
        // At master 94915476 the same commands print 14, 96, 14: 82, this line's old value.
        // Then BL9, vanilla's blast shape (the seat's brief of 2026-11-16), RECOUNTED the same way: 15 gates, 107 keys,
        // 15 setups, so 92. At feat/dragon-blast 58165392 the same commands print 15, 106, 15: 91, this line's old value.
        // Then the R3 audit of the older dragon rows (the seat's brief of 2026-11-16), RECOUNTED the same way: 15 gates,
        // 105 keys, 15 setups, so 90. At feat/dragon-blast ba74df59 the same commands print 15, 107, 15: 92, this
        // line's old value.
        // Then the follow-up of 2026-11-17 (GATE-dragon-blast-2.md), which RENAMES the blast gate's sixteen rows and adds
        // none, RECOUNTED the same way: 15 gates, 105 keys, 15 setups, so 90. At master ca0ae38a the same commands
        // print 15, 105, 15: 90, the same.
        // Then GATE-dragon-beam.md (step R5, 2026-11-17), RECOUNTED the same way: 16 gates, 118 keys, 16 setups, so 102.
        // At master 8671ab16 the same commands print 15, 105, 15: 90, this line's old value.
        // Then GATE-dragon-teleport.md (step R6, 2026-11-17), RECOUNTED the same way: 17 gates, 132 keys, 17 setups,
        // so 115. At feat/dragon-beam 1bfee281 the same commands print 16, 118, 16: 102, this line's old value.
        // Then GATE-dragon-knell-2.md (the Knell follow-up, 2026-11-17), RECOUNTED the same way: 17 gates, 133 keys, 17
        // setups, so 116. At master 7a3e9985 the same commands print 17, 132, 17: 115, this line's old value.
        // Then GATE-dragon-knell-3.md (Ben's re-timing, 2026-11-17), RECOUNTED the same way: 17 gates, 134 keys, 17
        // setups, so 117. At master 232f82b4 the same commands print 17, 133, 17: 116, this line's old value.
        // Then GATE-dragon-knell-4.md (Ben's one more number, 2026-11-17), RECOUNTED: 17 gates, 134 keys, 17 setups,
        // so 117, unchanged: it renames rows and adds none. At master 6b587faa the same commands print the same, so
        // this line cannot tell the two apart; the row lists above can.
        // Then GATE-dragon-shot.md (step R7a, 2026-11-17), RECOUNTED the same way: 18 gates, 149 keys, 18 setups, so 131.
        // At feat/dragon-shot 08e5f27e (master 8365e7f6 plus a docs commit) the same commands print 17, 134, 17: 117.
        // Then the re-prediction after the boot of 8ec16e89, RECOUNTED the same way: 18 gates, 147 keys, 18 setups, so 129. At
        // feat/dragon-shot ef242e95 the same commands print 18, 149, 18: 131, this line's old value.
        // Then GATE-dragon-blink.md (step R7b, 2026-11-17), RECOUNTED the same way: 19 gates, 165 keys, 19 setups, so 146.
        // At feat/dragon-blink be7bf586 (master 275e9d0d plus a docs commit) the same commands print 18, 147, 18: 129.
        // Then GATE-dragon-skreel.md (step R8, 2026-11-17), RECOUNTED the same way: 20 gates, 177 keys, 20 setups, so 157.
        // At master 185a6437 the same commands print 19, 165, 19: 146.
        // Then its re-prediction after the boot of 45bc33ce, RECOUNTED the same way: 20 gates, 174 keys, 20 setups, so
        // 154. At feat/dragon-skreel 1c18e862 the same commands print 20, 177, 20: 157, this line's old value.
        // Then PS11 (the diagonal, the seat's third brief), RECOUNTED the same way: 20 gates, 175 keys, 20 setups, so 155.
        // At feat/dragon-skreel 4ba51506 the same commands print 20, 174, 20: 154, this line's old value.
        // Then the void implosion (GATE-void-implosion.md, 2026-11-17), RECOUNTED the same way: 20 gates, 180 keys, 20
        // setups, so 160. At master f6a596ea the same commands print 20, 175, 20: 155, this line's old value.
        // Then Part C and the lift (the seat's two rulings on #204), RECOUNTED the same way: 20 gates, 185 keys, 20
        // setups, so 165: the stage edges IM5 and IM6 and the fuse rows FZ1, FZ3 and FZ5. 160 was this line's old value.
        // Then THE DRAG (GATE-implosion-drag.md, 2026-11-18), RECOUNTED the same way: 21 gates, 188 keys, 21 setups, so
        // 167: eighteen rows left dragon-blast (seventeen moved, BQ1R3 retired) and implosion-drag holds twenty (the
        // seventeen re-predicted, DR1, DR2 and BQ2). At master 24eb52e7 the same commands print 20, 185, 20: 165.
        // Then THE ADDITIVE DRAG (GATE-drag-additive.md, 2026-11-18), RECOUNTED the same way: 21 gates, 190 keys, 21
        // setups, so 169: implosion-drag's twenty retired and drag-additive's twenty-two in their place. At master
        // 401216aa the same commands print 21, 188, 21: 167, this line's old value.
        // Then THE FIGHT RING (GATE-fight-ring.md, steps F4 + F5, 2026-11-18), RECOUNTED the same way: 22 gates, 206 keys,
        // 22 setups, so 184: FR1-FR9 and BQ1-BQ6. At master efc29809 the same commands print 21, 190, 21: 169, this
        // line's old value.
        // Then THE MAGE (GATE-fight-mage.md, steps F6 + F7, 2026-11-18), RECOUNTED the same way: 22 gates, 215 keys,
        // 22 setups, so 193: fight-ring's 15 rows retired and fight-mage's 24 in their place (FR1R2-FR9R2, MG1-MG8,
        // MQ1-MQ7). At master 8c9a41b6 the same commands print 22, 206, 22: 184, this line's old value.
        // Then ONE RUNTIME AT A SITE (GATE-fight-site.md, step F8a, 2026-11-18), RECOUNTED the same way on this branch's
        // tree: 22 gates, 212 keys, 22 setups, so 190: fight-mage's 24 rows retired and fight-site's 21 in their place
        // (FR1R3-FR9R3, WS1, IP1, IA1, MG1R2-MG8R2, MQ1R2). The control: 215 - 25 + 22 = 212. 193 was this line's value.
        // Then THE ARENA (GATE-fight-arena.md, step F8b, 2026-11-18), a new gate, RECOUNTED the same way on this branch's
        // tree: 23 gates, 223 keys, 23 setups, so 200: fight-arena's 10 rows (AE1-AE7, BA1-BA3) added. The control:
        // 212 + 11 = 223. 190 was this line's value.
        // Then THE DRAGON (GATE-fight-dragon.md, step F9, 2026-11-18), a new gate, RECOUNTED the same way on this
        // branch's tree: 24 gates, 234 keys, 24 setups, so 210: fight-dragon's 10 rows (DA1, DA2, DA3, DA5-DA9, DB1, DB2)
        // added. The control: 223 + 11 = 234. 200 was this line's value.
        // Then THE FIGHT'S RULES (GATE-fight-rules.md, step F9b, 2026-11-19), a new gate, RECOUNTED the same way on this
        // branch's tree: 25 gates, 240 keys, 25 setups, so 215: fight-rules' 5 rows (RA1-RA5) added. The control:
        // 234 + 6 = 240. 210 was this line's value.
        // Then THE SECOND BOOT'S fight-rules (the seat's fix brief of 2026-11-19), RECOUNTED the same way on this branch's
        // tree: 25 gates, 239 keys, 25 setups, so 214: RA1-RA5 retired (read), RA3R2, RA4R2, RA5R2 and RB1 in their
        // place. The control: 240 - 5 + 4 = 239. 215 was this line's value.
        // Then AN END-TYPE FIGHT WORLD (GATE-fight-end.md, step F9c, 2026-11-19, stacked on fight-rules), a new gate,
        // RECOUNTED the same way on this branch's tree after its rebase onto the fix: 26 gates, 246 keys, 26 setups, so
        // 220: fight-end's 6 rows (EA1, EB1-EB5) added. The control: 239 + 7 = 246. 214 was this line's value.
        // Then THE SECOND BOOT'S fight-end (the seat's fix brief of 2026-11-19), RECOUNTED the same way on this branch's
        // tree: 26 gates, 243 keys, 26 setups, so 217: EA1 and EB1-EB5 retired (read or aborted), EB3R2, EB4R2 and EB5R2
        // in their place. The control: 246 - 7 + 4 = 243. 220 was this line's value.
        // Then DOWN, ALL DOWN AND THE ENTRY MODE (GATE-fight-down.md, step F9d, 2026-11-19), a new gate, RECOUNTED the
        // same way on this branch's tree: 27 gates, 248 keys, 27 setups, so 221: FD1, FD2, FD3, FD4 added. The control:
        // 243 + 5 = 248. 217 was this line's value.
        // Then THE COPY AND THE REVIVE (GATE-fight-revive.md, step F9e, 2026-11-19, stacked on fight-down), a new gate,
        // RECOUNTED the same way on this branch's tree: 28 gates, 256 keys, 28 setups, so 228: RV1-RV4 and RVB1-RVB3
        // added. The control: 248 + 8 = 256. 221 was this line's value.
        // Then THE TEMPLATE-FILE ARENA SOURCE (GATE-fight-template.md, the seat's queue item 2, 2026-11-19), a new gate,
        // RECOUNTED the same way on this branch's tree: 29 gates, 258 keys, 29 setups, so 229: TA1 added. The control:
        // 256 + 2 = 258. 228 was this line's value.
        // Then EB4R3 (GATE-fight-end.md, the seat's queue item 4, 2026-11-19), RECOUNTED: 29 gates, 259 keys, 29 setups,
        // so 230. The control: 258 + 1 = 259. 229 was this line's value.
        // Then FD2R2 (GATE-fight-down.md, Ben's void ruling and the seat's part 2, 2026-11-19): FD2 retired (read at
        // cb821675) and FD2R2 in its place, RECOUNTED: 29 gates, 259 keys, 29 setups, so 230. The control: 259 - 1 + 1 =
        // 259. 230 was this line's value, unchanged.
        // Then THE DEV ROLL COMMAND (GATE-loot-roll.md, the seat's queue item 6, 2026-11-19), a new gate, rebased onto
        // fight-template by the seat's part 2 and RECOUNTED on this branch's tree: 30 gates, 265 keys, 30 setups, so 235:
        // LR1 to LR5 added. The control: 259 + 6 = 265. 230 was this line's value.
        // Then THE SEVEN SWORDS AS CONTENT (GATE-sword-tiers.md, feat/sword-tiers, 2026-11-20), a new gate, RECOUNTED from
        // index.yml and every gate file it names: 31 gates, 271 keys, 31 setups, so 240: STA1 to STA5 added. The control:
        // 265 + 6 = 271. 235 was this line's value.
        // Then NETHERITE AT THE SMITHING TABLE (GATE-sword-smithing.md, feat/sword-smithing, 2026-11-20), a new gate,
        // RECOUNTED from index.yml: 32 gates, 274 keys, 32 setups, so 242: SSA1 and SSA2 added. The control: 271 + 3 = 274.
        // 240 was this line's value.
        // Then CONVERT ON ARRIVAL (GATE-sword-convert.md, feat/sword-convert, 2026-11-20), a new gate, RECOUNTED from
        // index.yml: 33 gates, 283 keys, 33 setups, so 250: SCA1-SCA5 and SCB1-SCB3 added. The control: 274 + 9 = 283.
        // 242 was this line's value.
        // Then THE FOUR DEV WEAPONS DELETED (feat/remove-dev-weapons, 2026-11-21): STA5 and DH6 RETIRED (their weapons
        // are gone), and GATE-remove-dev-weapons.md a NEW gate (setup, DW1, DW2), RECOUNTED from index.yml and every
        // gate file it names: 34 gates, 284 keys, 34 setups, so 250. The control: 283 - 2 + 3 = 284.
        // 250 was this line's value too, by coincidence (two retired, two added): the gate list above tells them apart.
        // Then HS1 (feat/dev-weapons-owed, PR B, 2026-11-21: the full-hotbar staging row), RECOUNTED: 34 gates, 285 keys,
        // 34 setups, so 251. The control: 284 + 1 = 285. 250 was this line's value.
        // Then GATE-crystal-shard-staff.md (feat/crystal-shard-staff, 2026-11-21), RECOUNTED: 35 gates, 290 keys, 35
        // setups, so 255. The control: 285 + 5 (setup, CS1-CS4) = 290. 251 was this line's value.
        // Then ES1 (feat/ember-staff-deleted, PR B, 2026-11-21: ember_staff refused), RECOUNTED: 35 gates, 291 keys, 35
        // setups, so 256. The control: 290 + 1 = 291. 255 was this line's value.
        // Then GATE-knell-beam.md (feat/knell-beam, 2026-11-21), RECOUNTED: 36 gates, 302 keys, 36 setups, so 266. The
        // control: 291 + 11 (setup, KB1-KB8, CS2R2, HF1R2) = 302. 256 was this line's value.
        // Then KB0, the spot check (the seat's follow-up, 2026-11-21), RECOUNTED: 36 gates, 303 keys, 36 setups, so 267.
        // The control: 302 + 1 = 303. 266 was this line's value.
        // Then GATE-living-lantern.md (feat/living-lantern, 2026-11-22), RECOUNTED: 37 gates, 317 keys, 37 setups, so 280.
        // The control: 303 + 14 (setup, LL0-LL9, KB2R2, CR3, HF1R3) = 317. 267 was this line's value.
        // Then GATE-breacher.md (feat/breacher, 2026-11-22), RECOUNTED: 38 gates, 331 keys, 38 setups, so 293. The
        // control: 317 + 14 (setup, BR0-BR11, HF2) = 331. 280 was this line's value.
        // Then GATE-breacher-rework.md (feat/breacher-rework, 2026-11-22), RECOUNTED: 39 gates, 342 keys, 39 setups, so 303.
        // The control: 331 + 11 (setup, RW0-RW5, BR6R2, BR7R2, LL9R2, RW9) = 342. 293 was this line's value.
        // Then GATE-vanilla-bows.md (feat/vanilla-bows-content, 2026-11-22), RECOUNTED: 40 gates, 365 keys, 40 setups, so
        // 325. The control: 342 + 23 (setup, VB0-VB3, VM1-VM10, VX1-VX7, VX9) = 365. 303 was this line's value.
        // Then the seat's follow-ups 2 and 3 (their rows on feat/vanilla-bows-content, 2026-11-22), RECOUNTED: 40 gates, 366 keys, 40
        // setups, so 326. The control: 365 + 1 (VX10; VX5 became VX5R2, a rename, not a row) = 366. 325 was this line's value.
        // Then GATE-boltor-recipe.md (feat/boltor-recipe, 2026-11-22), RECOUNTED: 41 gates, 371 keys, 41 setups, so 330. The
        // control: 366 + 5 (setup, BR1-BR4) = 371; VB3 became VB3R2 on feat/vb3r2, a rename. 326 was this line's value.
        // Then GATE-unique-enchant-slot.md (feat/info-top-right, 2026-11-23), RECOUNTED: 42 gates, 380 keys, 42 setups, so
        // 338. The control: 371 + 9 (setup, US1-US8) = 380. 330 was this line's value.
        // Then BX1 and BX2 (feat/boltor-sound-flint-text, 2026-11-23, in the same gate), RECOUNTED: 42 gates, 382 keys,
        // 42 setups, so 340. The control: 380 + 2 = 382. 338 was this line's value.
        // Then GATE-enchant-columns.md (feat/enchant-columns-gate, 2026-11-23), RECOUNTED (cat GATE-*.yml | tr -d CR |
        // grep -cE '^[A-Za-z0-9_-]+:', and '^setup:'): 43 gates, 394 keys, 43 setups, so 351. The control: 382 - 7 (US1-US7
        // retired from unique-enchant-slot, never run) + 19 (setup, EC0-EC17) = 394. 340 was this line's value.
        // Then LC1-LC4 (feat/lapis-crystal-gate, 2026-11-23, the same gate; EC1 renamed EC1R2), RECOUNTED the same way: 43 gates,
        // 398 keys, 43 setups, so 355. The control: 394 + 4 = 398 (a rename adds none). 351 was this line's value.
        // Then THE INSTRUMENT FIXED (feat/enchant-columns-instrument, 2026-11-23, after the boot of e774d4b8): the 22 rows
        // that ran there (EC0, EC1R2, EC2-EC17, LC1-LC4) left the scenario and 18 self-contained rows came in (EC3R2-EC16R2,
        // LC1R2-LC4R2; EC0's counts, EC1R2, EC2 and EC17's counts read as predicted and are not re-run), RECOUNTED the same
        // way: 43 gates, 394 keys, 43 setups, so 351. The control: 398 - 22 + 18 = 394. 355 was this line's value.
        // Then GATE-enchant-changes.md (feat/lapis-crystal-block, 2026-11-23, Ben's five changes), RECOUNTED the same way:
        // 44 gates, 403 keys, 44 setups, so 359. The control: 394 + 9 (its setup and AF1-AF4, LB1-LB4) = 403. 351 was this
        // line's value.
        // Then its SECOND FORM (feat/lapis-crystal-look, 2026-11-23, after the run of 78826389): AF1-AF4 (read) and LB1-LB4
        // (MISS) left; LB1R2, LB4R2, LB2R2, LB3R2 and LB5 came in. RECOUNTED the same way: 44 gates, 400 keys, 44 setups, so
        // 356. The control: 403 - 8 + 5 = 400. 359 was this line's value.
        // Then GATE-shatter-caps.md (feat/shatter-caps, 2026-11-24), RECOUNTED the same way: 45 gates, 410 keys, 45 setups,
        // so 365. The control: 400 + 10 (its setup and SR1, SR2, SC1, SC3, SC2, SF1, SF2, ST1, ST2) = 410. 356 was this
        // line's value.
        // Then GATE-focused-beam.md (feat/focused-beam, 2026-11-24), RECOUNTED the same way (`ls GATE-*.yml | wc -l`;
        // `cat GATE-*.yml | tr -d '\r' | grep -cE '^[A-Za-z0-9_-]+:'`; the same for '^setup:'): 46 gates, 421 keys, 46
        // setups, so 375. The control: 410 + 11 (its setup and FB0, FB1, FB2, FB3, FBP, FBR, FBR0, FBT, AFR, CSS) = 421.
        // 365 was this line's value.
        // Then GATE-mage-guaranteed.md (feat/mage-guaranteed-gate, 2026-11-24), RECOUNTED the same way: 47 gates, 437 keys,
        // 47 setups, so 390. The control: 421 + 16 (its setup and MG1-MG4, MG5, MG5C, MG6, MG7a-c, MG8, MG9a-b, MG10, MG11)
        // = 437. 375 was this line's value.
        // Then its extension (feat/mage-guaranteed-gate-2, 2026-11-24), RECOUNTED the same way: 47 gates, 443 keys, 47
        // setups, so 396. The control: 437 - 1 (MG2, replaced) + 7 (MG2R, MB1, MB2, MB3, MB4a, MB4b, MB5) = 443. 390 was
        // this line's value.
        // Then PT1 (feat/mage-guaranteed-gate-3, P10, 2026-11-24), RECOUNTED the same way: 47 gates, 444 keys, 47 setups,
        // so 397. The control: 443 + 1 (PT1) = 444. 396 was this line's value.
        // Then GATE-summoner.md (feat/summoner-gate, 2026-11-25), RECOUNTED the same way: 48 gates, 458 keys, 48 setups,
        // so 410. The control: 444 + 14 (its setup and SM1, SM2, SM3A, SM3B, SM3C, SM4-SM11) = 458. 397 was this line's value.
        // Then its rework (the seat's X1-X11, 2026-11-25), RECOUNTED the same way: 48 gates, 467 keys, 48 setups, so 419. The
        // control: 444 + 23 (its setup and SM1, SM2, SM3A-SM3D, SM4-SM7, SM8A-SM8C, SM9-SM17) = 467. 410 was this line's value.
        // Then GATE-armor-sets.md (feat/armor-sets-gate, 2026-11-25), RECOUNTED: 49 gates, 485 keys, 49 setups, so 436. The
        // control: 467 + 18 (its setup and AS0-AS16) = 485; SM7 became SM7R, a rename, no count. 419 was this line's value.
        // Then AS17-AS19 (the seat's re-rulings, 2026-11-25), RECOUNTED: 49 gates, 488 keys, 49 setups, so 439. Control:
        // 485 + 3 = 488. 436 was this line's value.
        // Then AS15R, SM9R and SM16R2 (feat/armor-sets-owed, 2026-11-25), RECOUNTED: 49 gates, 491 keys (cat GATE-*.yml |
        // grep -cE '^[A-Za-z0-9_-]+:'), 49 setups, so 442. Control: 488 + 3 = 491. 439 was this line's value.
        // Then GATE-broken-bottle.md (feat/broken-bottle-gate, 2026-11-25), RECOUNTED: 50 gates, 501 keys, 50 setups, so
        // 451. Control: 491 + 10 (its setup and BB0-BB8) = 501. 442 was this line's value.
        assertEquals(451, loaded.rows());
    }

    /**
     * NO DRAGON ROW OUTSIDE GATE-dragon-blast.yml LEAVES A LIVE PHAGE BESIDE THE PLAYER (the seat's brief of
     * 2026-11-16, item 4). From step R3 a live Phage within 3 blocks swells and BLOWS, so a row that never asked for
     * one would read a blast its prediction does not name. Stated on the FILES, in two halves:
     * <ol>
     *   <li>every SPAWN of a {@code phage} outside the blast gate is {@code frozen}, and no CMD there sets
     *       {@code NoAI} to anything but {@code 1b} (the only way a step could thaw one);</li>
     *   <li>a split child of a frozen tagged parent is born frozen ({@code SelfTestSnapshot.freezeSplitChild},
     *       {@code SelfTestSplitChildTest}), so every Phage those spawns can make is frozen too.</li>
     * </ol>
     * A frozen Phage never swells: NoAI stops its goals ({@code Mob.isEffectiveAi}) and only its swell goal (or an
     * ignite, which ours refuses) turns its swell up. <b>The positive control</b> is the blast gate itself: the same
     * scan must FIND its three live spawns and DS1R4's one THAW there (since 2026-11-17), or the scan is blind and its silence elsewhere
     * proves nothing.
     */
    @Test
    void noScenarioOutsideTheBlastGateLeavesALivePhage() {
        List<String> blastGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            List<String> found = new ArrayList<>();
            for (Scenario s : gate.getValue()) {
                for (Scenario.Step step : s.steps()) {
                    if (livePhage(step)) found.add(s.row() + " " + step.arg());
                }
            }
            // Since the drag (2026-11-18) the blast rows live in TWO files: the rows left in dragon-blast, and the
            // moved and new ones in implosion-drag.
            // Since the additive drag (2026-11-18), drag-additive in implosion-drag's place.
            if (Set.of("dragon-blast", "drag-additive").contains(gate.getKey())) blastGate.addAll(found);
            else assertEquals(List.of(), found, gate.getKey() + " has a live Phage; it belongs in the blast gate");
        }
        // Since 2026-11-17 (GATE-dragon-blast-2.md): BQ1R2's two Phages are FROZEN and blown, and DS1R4 thaws with the
        // instrument's THAW step instead of a data merge.
        // Since the void implosion (GATE-void-implosion.md): the same four, renamed with the next suffix, and since its
        // Part C the fuse rows' three and Ben's two, each held at speed 0.
        // Since the drag: BL5R3 stays in dragon-blast; the rest moved, renamed, to implosion-drag; BQ1R3's two are
        // retired; and BQ2's LIGHT (the instrument's forced fuse, which swells a FROZEN Phage) is counted as live too.
        // Since the additive drag: the same, renamed with the next suffix; BQ3 lights nothing (BQ2's swell question was
        // answered "yes" in both runs of 39c0b341), so it has no live Phage.
        assertEquals(List.of("BL5R3 bl5 live phage 100", "BL1R5 bl1 live phage 100",
                        "BL2R5 bl2 live phage stage 5 60", "FZ1R3 fz1 live phage 100",
                        "FZ3R3 fz3 live phage stage 3 60", "FZ5R3 fz5 live phage stage 5 60", "DS1R7 ds1"),
                blastGate, "the control: the scan must find every live Phage the blast gates stage on purpose");
    }

    /**
     * A step that puts a live Phage in the world: a live phage SPAWN, a CMD that sets NoAI to anything but 1b, or a
     * THAW (which switches AI on for whatever carries its tag).
     */
    private static boolean livePhage(Scenario.Step step) {
        if (step.verb() == Scenario.Verb.THAW) return true;
        // The instrument's LIGHT (2026-11-18) swells and blows even a FROZEN Phage: a live one, for this scan.
        if (step.verb() == Scenario.Verb.LIGHT) return true;
        if (step.verb() == Scenario.Verb.SPAWN) {
            SelfTestSpawn.Spec spec = SelfTestSpawn.parse(step.arg()).orElseThrow();
            return spec.mob().equals("phage") && !spec.frozen();
        }
        if (step.verb() == Scenario.Verb.CMD) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("NoAI:([^,}\\]]*)").matcher(step.arg());
            while (m.find()) if (!m.group(1).equals("1b")) return true;
        }
        return false;
    }

    @Test
    void everyScenarioNamesARowOfItsGate() throws IOException {
        for (var gate : shipped().gates().entrySet()) {
            Path md = Path.of("..", "docs", "gates", "GATE-" + docOf(gate.getKey()) + ".md");
            Set<String> headings = ScenarioLoader.headings(Files.readString(md, StandardCharsets.UTF_8));
            assertTrue(headings.size() >= 7, md + " read as " + headings + " -- the heading pattern found too few rows");
            assertEquals(List.of(), ScenarioLoader.unpaired(gate.getValue(), headings), md.toString());
        }
    }

    /**
     * Which gate file a scenario file's rows are headings in. Its own, except the three dragon gates, whose rows since
     * the 2026-11-15 boot are re-predictions held in GATE-dragon-fix.md, the stack top (its brief: the old
     * predictions and readings stay as they are, and every re-run row is a new row named for the old one).
     * NAMED, not a prefix: {@code dragon-blast} (step R3) is a dragon gate with its OWN file, and a prefix test sent it
     * to GATE-dragon-fix.md.
     */
    static String docOf(String gate) {
        // re-predicted 2026-11-17 (the -2 gate), then again by the void implosion the same day.
        if (gate.equals("dragon-blast")) return "void-implosion";
        // Ben's re-timing of 2026-11-17 (KNELL 3): both Knell files' rows are GATE-dragon-knell-3.md's re-predictions
        // (before it, GATE-dragon-knell-2.md's).
        // Then KNELL 4 (Ben's 28 and his second bell), then KNELL 5 (the shot in the middle): both files' rows are
        // GATE-dragon-knell-5.md's.
        if (Set.of("dragon-beam", "dragon-teleport").contains(gate)) return "dragon-knell-5";
        return Set.of("dragon-bodies", "dragon-split", "dragon-growth").contains(gate) ? "dragon-fix" : gate;
    }

    /**
     * NO ROW OUTSIDE GATE-dragon-beam.yml LEAVES A KNELL WITH ITS AI ON (the R5 audit, GATE-dragon-beam.md, THE ROWS
     * ALREADY MERGED). Since step R5 a Knell with AI begins a beam of its own 100 to 200 ticks after it appears; a
     * frozen (NoAI) one never does (I23). So an older row's Knell must be spawned frozen, or by {@code rpg spawn knell}
     * followed at once by a {@code data merge} that sets {@code NoAI:1b}, or be a spawn the command refuses (DB11R2's
     * {@code stage 2}). The control: the beam gate's own two live Knells (KB2, KB11) ARE found.
     */
    @Test
    void noScenarioOutsideTheBeamGateLeavesALiveKnell() {
        List<String> live = new ArrayList<>();
        List<String> beamGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            for (Scenario s : gate.getValue()) {
                List<Scenario.Step> steps = s.steps();
                for (int i = 0; i < steps.size(); i++) {
                    Scenario.Step st = steps.get(i);
                    String a = st.arg().trim();
                    boolean liveSpawn = st.verb() == Scenario.Verb.SPAWN && a.matches("\\S+ live knell\\b.*");
                    boolean bareCmd = st.verb() == Scenario.Verb.CMD && a.matches("rpg spawn knell( \\d+)?");
                    boolean frozenAtOnce = i + 1 < steps.size() && steps.get(i + 1).verb() == Scenario.Verb.CMD
                            && steps.get(i + 1).arg().contains("NoAI:1b");
                    if (!(liveSpawn || (bareCmd && !frozenAtOnce))) continue;
                    String where = gate.getKey() + "#" + s.row() + " " + st.verb() + " " + a;
                    if (Set.of("dragon-beam", "dragon-teleport").contains(gate.getKey())) beamGate.add(where); else live.add(where);
                }
            }
        }
        assertEquals(List.of(), live, "a live Knell outside the beam gate would beam the player at a time no row predicts");
        assertEquals(3, beamGate.size(), "the control: the beam file's KB2R5 and WA1R3 and the teleport file's KB11R6 are "
                + "seen: " + beamGate);
    }

    /**
     * NO ROW OUTSIDE GATE-dragon-shot.yml MAKES A PHALANX FIRE (step R7a, the seat's ruling 12). A Phalanx with its AI
     * on fires of its own 120 ticks after it appears; a frozen one never does, but the SHOT step makes one fire. So
     * outside the shot gate a Phalanx must be spawned frozen, or by {@code rpg spawn phalanx} followed at once by a
     * {@code data merge} that sets {@code NoAI:1b}; and no SHOT step appears. The control: the shot gate's twelve SHOT steps
     * ARE found, and the shared live-spawn pattern matches a live spawn written in the test (none is shipped now).
     */
    @Test
    void noScenarioOutsideTheShotGateMakesAPhalanxFire() {
        List<String> outside = new ArrayList<>();
        List<String> shotGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            for (Scenario s : gate.getValue()) {
                List<Scenario.Step> steps = s.steps();
                for (int i = 0; i < steps.size(); i++) {
                    Scenario.Step st = steps.get(i);
                    String a = st.arg().trim();
                    boolean liveSpawn = st.verb() == Scenario.Verb.SPAWN && livePhalanxSpawn(a);
                    boolean bareCmd = st.verb() == Scenario.Verb.CMD && a.matches("rpg spawn phalanx( \\d+)?");
                    boolean frozenAtOnce = i + 1 < steps.size() && steps.get(i + 1).verb() == Scenario.Verb.CMD
                            && steps.get(i + 1).arg().contains("NoAI:1b");
                    boolean forced = st.verb() == Scenario.Verb.SHOT;
                    if (!(liveSpawn || forced || (bareCmd && !frozenAtOnce))) continue;
                    String where = gate.getKey() + "#" + s.row() + " " + st.verb() + " " + a;
                    // Step R7b: the blink gate is a shot gate too (its C1-C4 rows force shots; BB4 and BQ1 are live).
                    if (Set.of("dragon-shot", "dragon-blink", "dragon-skreel").contains(gate.getKey())) shotGate.add(where); else outside.add(where);
                }
            }
        }
        assertEquals(List.of(), outside, "a firing Phalanx outside the shot gates would hit the player at a time no row predicts");
        // Since the re-prediction after the boot of 8ec16e89 the shot gate stages no LIVE Phalanx (SH10 and SH11 read as
        // predicted and left the file); the blink gate stages two, BB4's (gone before its first shot) and Ben's BQ1.
        assertEquals(List.of("dragon-blink#BB4 SPAWN bb4 live phalanx 100", "dragon-blink#BQ1 SPAWN bq1 live phalanx 100",
                        "dragon-skreel#PB1R2 SPAWN pb1r2 live phalanx 100", "dragon-skreel#PB2R2 SPAWN pb2r2 live phalanx 100",
                        "dragon-skreel#BB4R2 SPAWN bb4r2 live phalanx 100", "dragon-skreel#PQ4 SPAWN pq4 live phalanx 100"),
                shotGate.stream().filter(w -> w.contains(" SPAWN ")).toList(), "the live Phalanxes: " + shotGate);
        assertEquals(List.of("dragon-skreel#PB4 SHOT pb4", "dragon-skreel#PB4 SHOT pb4"),
                shotGate.stream().filter(w -> w.startsWith("dragon-skreel#") && w.contains(" SHOT ")).toList(),
                "and the Skreel gate's two, PB4's (step R8, the target a blinked Phalanx keeps): " + shotGate);
        assertEquals(12, shotGate.stream().filter(w -> w.startsWith("dragon-shot#") && w.contains(" SHOT ")).count(),
                "the control: the shot gate's twelve SHOT steps: " + shotGate);
        assertEquals(7, shotGate.stream().filter(w -> w.startsWith("dragon-blink#") && w.contains(" SHOT ")).count(),
                "and the blink gate's seven (BC1X, BC1Z, BC2B, BC3, BC4A, BC4B, BB5): " + shotGate);
        Scenario.Step live = new Scenario.Step(Scenario.Verb.SPAWN, "x live phalanx 100");
        assertTrue(livePhalanxSpawn(live.arg().trim()), "the control: the scan's own pattern matches a live Phalanx spawn");
        assertFalse(livePhalanxSpawn("x frozen phalanx 100"), "and not a frozen one");
    }

    /**
     * NO ROW OUTSIDE GATE-dragon-skreel.yml LEAVES A LIVE SKREEL PAIR, OR A LIVE MOUNT, BESIDE THE PLAYER (step R8). A live
     * pair winds up of its own 120 ticks after it appears and lands 55 on whoever is in its band; a frozen pair never does,
     * but the POUNCE step makes one. So outside the Skreel gate a {@code skreel} or {@code withered_mount} is spawned
     * frozen, or by {@code rpg spawn} followed at once by a {@code data merge} setting {@code NoAI:1b}, and no POUNCE step
     * appears. The control: the Skreel gate's own live spawns and POUNCE steps ARE found, and the scan's pattern matches a
     * live spawn of each written in the test.
     */
    @Test
    void noScenarioOutsideTheSkreelGateLeavesALivePair() {
        List<String> outside = new ArrayList<>();
        List<String> skreelGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            for (Scenario s : gate.getValue()) {
                List<Scenario.Step> steps = s.steps();
                for (int i = 0; i < steps.size(); i++) {
                    Scenario.Step st = steps.get(i);
                    String a = st.arg().trim();
                    boolean liveSpawn = st.verb() == Scenario.Verb.SPAWN && livePairSpawn(a);
                    boolean bareCmd = st.verb() == Scenario.Verb.CMD && a.matches("rpg spawn (skreel|withered_mount)( \\d+)?");
                    boolean frozenAtOnce = i + 1 < steps.size() && steps.get(i + 1).verb() == Scenario.Verb.CMD
                            && steps.get(i + 1).arg().contains("NoAI:1b");
                    boolean forced = st.verb() == Scenario.Verb.POUNCE;
                    if (!(liveSpawn || forced || (bareCmd && !frozenAtOnce))) continue;
                    String where = gate.getKey() + "#" + s.row() + " " + st.verb() + " " + a;
                    if (gate.getKey().equals("dragon-skreel")) skreelGate.add(where); else outside.add(where);
                }
            }
        }
        assertEquals(List.of(), outside, "a live Skreel pair outside its gate would pounce at a time no row predicts");
        assertEquals(List.of("dragon-skreel#PS6R2 SPAWN ps6r2 live skreel 100", "dragon-skreel#PS9 SPAWN ps9 live skreel 100",
                        "dragon-skreel#PS10 SPAWN ps10 live skreel 100", "dragon-skreel#PS11 SPAWN ps11 live skreel 100",
                        "dragon-skreel#PQ3 SPAWN pq3 live skreel 100"),
                skreelGate.stream().filter(w -> w.contains(" SPAWN ")).toList(), "the control, its live pairs: " + skreelGate);
        // Since the re-prediction after the boot of 45bc33ce no shipped row forces a pounce (PS3, PS4 and PS6 left the
        // file), so the POUNCE half of this scan has no shipped hit: it is the verb's equality above, and nothing more.
        assertEquals(0, skreelGate.stream().filter(w -> w.contains(" POUNCE ")).count(), "no POUNCE step: " + skreelGate);
        assertTrue(livePairSpawn("x live skreel 100") && livePairSpawn("x live withered_mount"),
                "the control: the scan's own pattern matches a live spawn of each");
        assertFalse(livePairSpawn("x frozen skreel 100"), "and not a frozen one");
    }

    private static boolean livePairSpawn(String arg) {
        return arg.matches("\\S+ live (skreel|withered_mount)\\b.*");
    }

    /** The scan's one pattern for a live Phalanx SPAWN, shared with its control. */
    private static boolean livePhalanxSpawn(String arg) {
        return arg.matches("\\S+ live phalanx\\b.*");
    }

    /** The dragon gates declare the difficulty their rows need, first thing in their setup (GATE-dragon-fix.md, C5). */
    @Test
    void theDragonGatesDeclareNormalFirstInTheirSetup() {
        for (String gate : List.of("dragon-bodies", "dragon-split", "dragon-growth", "dragon-blast", "dragon-beam",
                "dragon-teleport", "dragon-shot", "dragon-blink")) {
            Scenario setup = shipped().gates().get(gate).getFirst();
            assertEquals(Scenario.SETUP, setup.row(), gate);
            assertEquals(new Scenario.Step(Scenario.Verb.DIFFICULTY, "normal"), setup.steps().getFirst(), gate);
        }
    }

    /**
     * THE SCENARIO LINT OVER EVERY COMMITTED SCENARIO (the seat's brief A, 2026-11-23; {@link ScenarioLint}). REQUIRED
     * GREEN for {@code GATE-enchant-columns.yml}. Every other gate's findings are REPORTED, one line each, to
     * {@code paper/target/scenario-lint.txt} and stdout, and not fixed here: those rows ran, and their readings stand on
     * the scenario as it was.
     *
     * <p><b>The control:</b> the lint is not blind over the shipped files. {@code deft-hands}' DH3 chains on DH1 (one of
     * the three chained rows {@code SelfTestStaging} names), so the SELF_STAGED rule MUST find it there.
     */
    @Test
    void theLintIsGreenForEnchantColumnsAndReportsEveryOtherGate() throws IOException {
        Map<String, List<ScenarioLint.Finding>> byGate = new java.util.LinkedHashMap<>();
        for (var gate : shipped().gates().entrySet()) byGate.put(gate.getKey(), ScenarioLint.lint(gate.getKey(), gate.getValue()));
        StringBuilder report = new StringBuilder();
        int total = 0;
        for (var e : byGate.entrySet()) {
            Map<ScenarioLint.Rule, Long> perRule = new java.util.EnumMap<>(ScenarioLint.Rule.class);
            for (ScenarioLint.Finding f : e.getValue()) perRule.merge(f.rule(), 1L, Long::sum);
            report.append("GATE ").append(e.getKey()).append(" findings=").append(e.getValue().size()).append(' ')
                    .append(perRule).append('\n');
            for (ScenarioLint.Finding f : e.getValue()) report.append("  ").append(f.line()).append('\n');
            total += e.getValue().size();
        }
        report.append("TOTAL findings=").append(total).append(" gates=").append(byGate.size()).append('\n');
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "scenario-lint.txt"), report.toString(), StandardCharsets.UTF_8);
        System.out.print(report);

        assertEquals(List.of(), byGate.get("enchant-columns").stream().filter(f -> f.rule() != ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "GATE-enchant-columns.yml must lint clean");
        assertEquals(List.of(), byGate.get("enchant-changes").stream().filter(f -> f.rule() != ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "GATE-enchant-changes.yml must lint clean (2026-11-23, written under the same rules)");
        assertEquals(List.of(), byGate.get("shatter-caps").stream().filter(f -> f.rule() != ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "GATE-shatter-caps.yml must lint clean (2026-11-24, written under the same rules)");
        assertEquals(List.of(), byGate.get("focused-beam").stream().filter(f -> f.rule() != ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "GATE-focused-beam.yml must lint clean (2026-11-24, written under the same rules)");
        assertEquals(List.of(), byGate.get("mage-guaranteed").stream().filter(f -> f.rule() != ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "GATE-mage-guaranteed.yml must lint clean (2026-11-24, written under the same rules)");
        assertEquals(List.of(), byGate.get("summoner").stream().filter(f -> f.rule() != ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "GATE-summoner.yml must lint clean (2026-11-25, written under the same rules)");
        // GATE-broken-bottle (2026-11-25): clean on EVERY rule, LEVEL_UNSTAGED included (no row moves his level), with the
        // control that it loaded at all.
        assertTrue(byGate.containsKey("broken-bottle"), "control: the bottle's gate was linted");
        assertEquals(List.of(), byGate.get("broken-bottle").stream().map(ScenarioLint.Finding::line).toList(),
                "GATE-broken-bottle.yml must lint clean on every rule");
        // L3 (the seat's ruling, 2026-11-24): HELD_READ_THEN_CHANGE over every gate, pinned EXACTLY. Old rows of other gates
        // are not edited; these four are owed dated notes (PLAN-enchant-columns section 33). STA4 is the one that cost a
        // reading: its iron half was recorded UNREAD when both shows answered for the netherite sword. The other three
        // give a DIFFERENT item, which fills an empty slot, so the late durability write or show still met the held weapon.
        assertEquals(List.of(
                        "LINT sword-tiers#STA4 HELD_READ_THEN_CHANGE 'rpg give netherite_sword' after 'rpg enchant show' with no wait:"
                                + " the read runs a tick later (scheduler.onEntity) and would see the changed hand",
                        "LINT sword-smithing#SSA1 HELD_READ_THEN_CHANGE 'give @s minecraft:netherite_upgrade_smithing_template 1'"
                                + " after 'rpg enchant show' with no wait: the read runs a tick later (scheduler.onEntity) and would see the changed hand",
                        "LINT vanilla-bows#VX1 HELD_READ_THEN_CHANGE 'give @s minecraft:arrow 4' after 'rpg durability set 0' with no wait:"
                                + " the read runs a tick later (scheduler.onEntity) and would see the changed hand",
                        "LINT vanilla-bows#VX5R2 HELD_READ_THEN_CHANGE 'give @s minecraft:arrow 2' after 'rpg durability set 999' with no"
                                + " wait: the read runs a tick later (scheduler.onEntity) and would see the changed hand"),
                byGate.values().stream().flatMap(List::stream)
                        .filter(f -> f.rule() == ScenarioLint.Rule.HELD_READ_THEN_CHANGE).map(ScenarioLint.Finding::line).toList(),
                "every HELD_READ_THEN_CHANGE in the shipped scenarios, and no more");
        // THE LEVEL SEAM (the seat, 2026-11-25): LEVEL_UNSTAGED over every gate, pinned EXACTLY. Old rows are not edited;
        // these are REPORTED (PLAN-armor-sets-owed.md). armor-sets' five are the seam the boot of 732c4e02 ran into, and
        // they are the rule's control on the shipped files. AS16 is not here: AS15R, ahead of it, stages level 1 again.
        String seam = "LEVEL_UNSTAGED starts at level '13 levels', left by AS13, not the setup's '1 levels', and stages none of its own";
        assertEquals(List.of(
                        "LINT mage-guaranteed#MB1R LEVEL_UNSTAGED starts at level '1 levels', left by MG11, not the setup's"
                                + " 'null', and stages none of its own",
                        "LINT armor-sets#AS14 " + seam, "LINT armor-sets#AS15 " + seam, "LINT armor-sets#AS17 " + seam,
                        "LINT armor-sets#AS18 " + seam, "LINT armor-sets#AS19 " + seam),
                byGate.values().stream().flatMap(List::stream)
                        .filter(f -> f.rule() == ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::line).toList(),
                "every LEVEL_UNSTAGED in the shipped scenarios, and no more");
        // The three rows written under the rule lint clean on EVERY rule.
        for (String[] r : new String[][] {{"armor-sets", "AS15R"}, {"summoner", "SM9R"}, {"summoner", "SM16R2"}}) {
            assertEquals(List.of(), byGate.get(r[0]).stream().filter(f -> f.row().equals(r[1]))
                    .map(ScenarioLint.Finding::line).toList(), r[1] + " must lint clean");
            assertTrue(shipped().gates().get(r[0]).stream().anyMatch(s -> s.row().equals(r[1])),
                    "the control: " + r[1] + " is loaded, so its empty list is not a missing row");
        }
        assertTrue(byGate.get("deft-hands").stream().anyMatch(f -> f.row().equals("DH3")
                        && f.rule() == ScenarioLint.Rule.SELF_STAGED && f.detail().startsWith("chains on DH1")),
                "the control: DH3's chain on DH1 is found, so the lint reads the shipped files: " + byGate.get("deft-hands"));
    }

    /**
     * The enchant-columns gate stands on the pad and measures it before any row: its setup's FIRST step is the pad and
     * its SECOND the preflight (Ben's ruling A and the seat's P5, P8, 2026-11-23). No other gate opts in (P9).
     */
    @Test
    void theEnchantColumnsSetupStandsOnThePadThenPreflights() {
        Scenario setup = shipped().gates().get("enchant-columns").getFirst();
        assertEquals(Scenario.SETUP, setup.row());
        assertEquals(Scenario.Verb.PAD, setup.steps().get(0).verb(), setup.steps().toString());
        assertEquals(Scenario.Verb.PREFLIGHT, setup.steps().get(1).verb(), setup.steps().toString());
        List<String> padGates = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            for (Scenario s : gate.getValue()) {
                for (Scenario.Step st : s.steps()) if (st.verb() == Scenario.Verb.PAD) padGates.add(gate.getKey() + "#" + s.row());
            }
        }
        assertEquals(List.of("enchant-columns#setup", "enchant-changes#setup", "shatter-caps#setup", "focused-beam#setup",
                        "mage-guaranteed#setup", "summoner#setup", "armor-sets#setup", "broken-bottle#setup"),
                padGates, "only enchant-columns, (2026-11-23) enchant-changes, (2026-11-24) shatter-caps and (2026-11-24)"
                        + " focused-beam and (2026-11-24) mage-guaranteed opt in, each in its setup");
        Scenario changes = shipped().gates().get("enchant-changes").getFirst();
        assertEquals(Scenario.Verb.PAD, changes.steps().get(0).verb(), changes.steps().toString());
        assertEquals(Scenario.Verb.PREFLIGHT, changes.steps().get(1).verb(), changes.steps().toString());
    }

    @Test
    void controlAnUnpairedKeyAndAnUnknownVerbAreBothCaught() throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                WB2:
                  - wait: 5
                WB9:
                  - wait: 5
                WB3:
                  - teleport: "~ ~ ~"
                """);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("withered-shortbow", file, refusals);
        assertEquals(1, refusals.size(), refusals.toString());
        assertTrue(refusals.getFirst().contains("unknown verb 'teleport'"), refusals.getFirst());
        assertEquals(List.of("WB2", "WB9"), scenarios.stream().map(Scenario::row).toList(), "WB3 is refused whole");
        assertEquals(List.of("WB9"), ScenarioLoader.unpaired(scenarios, Set.of("WB2", "WB3")));
    }

    /**
     * THE PROBE LAG REACHES THE LOADER (step R8, PLAN F23): a row that kills within 2 ticks of probe_pair is refused WHOLE
     * at boot, by name; the same row with its `wait: 2` loads. Without this row, the loader's call to
     * SelfTestStaging.probeLag could be dropped with every shipped scenario still loading (PLAGHOOK survived until it).
     */
    @Test
    void aRowThatKillsInsideTheProbeLagIsRefusedAtLoadAndItsWaitedTwinLoads() throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                DB6R3:
                  - probe_pair: db6
                  - cmd: "kill @e[tag=db6]"
                DB6R4:
                  - probe_pair: db6r4
                  - wait: 2
                  - cmd: "kill @e[tag=db6r4]"
                """);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("dragon-skreel", file, refusals);
        assertEquals(1, refusals.size(), refusals.toString());
        assertTrue(refusals.getFirst().startsWith("Refusing selftest/GATE-dragon-skreel.yml#DB6R3: ")
                && refusals.getFirst().endsWith("(the probe lag)"), refusals.getFirst());
        assertEquals(List.of("DB6R4"), scenarios.stream().map(Scenario::row).toList(), "DB6R3 is refused whole");
    }

    @Test
    void setupRunsFirstWhereverItIsWritten() throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                WB1:
                  - note: "row"
                setup:
                  - note: "setup"
                """);
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, new ArrayList<>());
        assertEquals(List.of(Scenario.SETUP, "WB1"), scenarios.stream().map(Scenario::row).toList());
    }

    @Test
    void theHeadingPatternReadsRowIdsAndNotSectionTitles() {
        String md = "## R0\n### R0a — the build\n### WS-C — negative\n#### not a row\n### WB4b — cap\n";
        assertEquals(List.of("R0a", "WS-C", "WB4b"), List.copyOf(ScenarioLoader.headings(md)));
    }
}
