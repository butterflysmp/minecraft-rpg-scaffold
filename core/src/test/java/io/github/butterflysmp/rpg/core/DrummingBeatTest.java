package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.enchant.DrumBeats;
import io.github.butterflysmp.rpg.core.enchant.DrumBeats.Decision;
import io.github.butterflysmp.rpg.core.enchant.DrumBeats.Result;
import io.github.butterflysmp.rpg.core.enchant.DrumBeats.Streak;
import io.github.butterflysmp.rpg.core.enchant.DrumStreaks;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DRUMMING BEAT'S PURE DECISION AS A TABLE (the seat's M3 to M5, 2026-11-26; Ben: "every consecutive hit adds 3/4/5
 * critical chance. Same conditions" / "No there is no bonus ceiling, landing a crit doesn't reset it"). Every row: the
 * streak before, the hit's tick, whether it was fully charged, the level (0: Drumming Beat not active on the club in hand)
 * -> the n it is rolled at, the bonus added to the chance, the result token, the streak after. Every expected figure that
 * is a double was EXECUTED (the bonus is step x n / 100.0, run in this test's own assertions and in jshell, 2026-11-26).
 */
class DrummingBeatTest {

    static final EnchantBehaviour.DrummingBeat BEAT = new EnchantBehaviour.DrummingBeat("bone_club", 100,
            List.of(3.0, 4.0, 5.0), "block.note_block.basedrum", "block.bone_block.break");

    private static void row(Streak before, long now, boolean charged, int level,
                            int n, double bonus, Result result, Streak after) {
        Decision d = DrumBeats.decide(before, now, charged, level < 1 ? null : BEAT, level);
        String at = "streak " + before + " at " + now + " charged=" + charged + " level " + level;
        assertEquals(n, d.n(), at + ": n");
        assertEquals(bonus, d.bonus(), 0.0, at + ": bonus, EXACT");
        assertEquals(result, d.result(), at + ": result");
        assertEquals(after, d.after(), at + ": after");
    }

    /**
     * THE TABLE. Mutations it kills, each named: MUT-GAPEQ (> to >=: the 100-tick row reads reset:gap), MUT-NOFF (bonus
     * at n + 1: the first hit has a bonus), MUT-UNCHCOUNT (an uncharged hit counts), MUT-UNCHKEEP (an uncharged hit keeps n), MUT-PLAINKEEP (a plain
     * hit keeps the streak), MUT-STEP (the level's step read one level off).
     */
    @Test
    void theDecisionTable() {
        Streak none = Streak.NONE;
        // the first hit of a streak: no bonus; it is counted, and the streak is 1, stamped at its tick
        row(none, 1000, true, 3, 0, 0.0, Result.COUNTED, new Streak(1, 1000));
        // the second, 40 ticks later: one step (III: 5 points)
        row(new Streak(1, 1000), 1040, true, 3, 1, 0.05, Result.COUNTED, new Streak(2, 1040));
        // the fifth: four steps
        row(new Streak(4, 1120), 1160, true, 3, 4, 0.2, Result.COUNTED, new Streak(5, 1160));
        // a gap of EXACTLY 100 from the LAST hit continues (MUT-GAPEQ)
        row(new Streak(2, 1050), 1150, true, 3, 2, 0.1, Result.COUNTED, new Streak(3, 1150));
        // 101: the streak resets and THIS hit is its first (rolled at 0, counted as 1)
        row(new Streak(2, 1050), 1151, true, 3, 0, 0.0, Result.RESET_GAP, new Streak(1, 1151));
        // below full charge: reset, rolled with no bonus, NOT counted (MUT-UNCHCOUNT, MUT-UNCHKEEP)
        row(new Streak(3, 1000), 1010, false, 3, 3, 0.0, Result.RESET_UNCHARGED, Streak.NONE);
        // uncharged on no streak: still a reset (nothing to lose), still not counted
        row(none, 1010, false, 3, 0, 0.0, Result.RESET_UNCHARGED, Streak.NONE);
        // the club without Drumming Beat active (level 0), or another weapon: plain, and the streak is gone (M5c)
        row(new Streak(3, 1000), 1010, true, 0, 3, 0.0, Result.PLAIN, Streak.NONE);
        row(none, 1010, false, 0, 0, 0.0, Result.PLAIN, Streak.NONE);
        // the steps by level (MUT-STEP): I 3 points, II 4, at n = 1 and n = 2
        row(new Streak(1, 1000), 1040, true, 1, 1, 0.03, Result.COUNTED, new Streak(2, 1040));
        row(new Streak(2, 1040), 1080, true, 1, 2, 0.06, Result.COUNTED, new Streak(3, 1080));
        row(new Streak(1, 1000), 1040, true, 2, 1, 0.04, Result.COUNTED, new Streak(2, 1040));
        row(new Streak(2, 1040), 1080, true, 2, 2, 0.08, Result.COUNTED, new Streak(3, 1080));
    }

    /**
     * NO CEILING (BEN): the bonus keeps growing past 100%; it is clamped only at the roll (Crit.chance). Every value of the
     * twenty-hit truth row, EXECUTED: III from the base 0.15, the n-th hit (1-based) is rolled at 0.15 + 0.05 x (n - 1).
     * The 18th is the first at or above 1.000; that sum is EXACTLY 1.0 in binary floating point (executed here: 0.15 + 0.85).
     */
    @Test
    void thereIsNoCeilingAndTheEighteenthHitReachesOne() {
        Streak s = Streak.NONE;
        long tick = 0;
        double[] chance = new double[21];
        for (int hit = 1; hit <= 20; hit++) {
            Decision d = DrumBeats.decide(s, tick, true, BEAT, 3);
            assertEquals(hit - 1, d.n());
            chance[hit] = 0.15 + d.bonus();
            s = d.after();
            tick += 30;
        }
        // EXECUTED (this test's first run, 2026-11-26): 0.15 + 0.8 is 0.9500000000000001 in binary floating point, not 0.95.
        assertEquals(0.9500000000000001, 0.15 + 0.8, 0.0, "the 17th's sum, as executed");
        assertEquals(0.9500000000000001, chance[17], 0.0);
        assertTrue(chance[17] < 1.0, "the 17th can miss");
        assertEquals(1.0, chance[18], 0.0, "the 18th is EXACTLY 1.0: it always crits (Crit.crits: roll < 1.0)");
        assertEquals(1.05, chance[19], 1e-12);
        assertEquals(1.1, chance[20], 1e-12, "past 100%, unclamped (no ceiling)");
        assertEquals(20, s.n());
        assertEquals(1.0, io.github.butterflysmp.rpg.core.combat.Crit.chance(chance[20]), 0.0, "clamped only at the roll");
        assertTrue(io.github.butterflysmp.rpg.core.combat.Crit.crits(chance[18], Math.nextDown(1.0)),
                "the largest roll nextDouble can draw still crits at 1.0");
    }

    /** A CRIT DOES NOT RESET (BEN): the decision takes no crit at all, so it cannot. Pinned by the signature: (streak, tick, charged, beat, level). */
    @Test
    void aCritCannotReachTheDecision() throws NoSuchMethodException {
        var m = DrumBeats.class.getMethod("decide", Streak.class, long.class, boolean.class,
                EnchantBehaviour.DrummingBeat.class, int.class);
        assertEquals(Decision.class, m.getReturnType());
        assertEquals(5, m.getParameterCount());
    }

    /** M7 (SEAT PLACEHOLDER): the drum's pitch rises with the streak after the hit (the old game's 0.7 + 0.2 x count) to 2.0. */
    @Test
    void thePitchRisesToTheLimit() {
        assertEquals(0.9f, DrumBeats.pitch(1), 1e-6f);
        assertEquals(1.1f, DrumBeats.pitch(2), 1e-6f);
        assertEquals(1.9f, DrumBeats.pitch(6), 1e-6f);
        assertEquals(2.0f, DrumBeats.pitch(7), 0.0f, "0.7 + 1.4 = 2.1 is capped at 2.0");
        assertEquals(2.0f, DrumBeats.pitch(40), 0.0f);
    }

    /** The trace token of each result (M8), pinned so the gate can quote them. */
    @Test
    void theResultTokens() {
        assertEquals("counted", Result.COUNTED.token());
        assertEquals("reset:uncharged", Result.RESET_UNCHARGED.token());
        assertEquals("reset:gap", Result.RESET_GAP.token());
        assertEquals("plain", Result.PLAIN.token());
    }

    /** M8: the line, whole. */
    @Test
    void theTraceLine() {
        Decision d = DrumBeats.decide(new Streak(4, 1000), 1040, true, BEAT, 3);
        assertEquals("STREAK n=4 step=5 chance=0.350 charged=true result=counted", DrumBeats.line(d, 5.0, 0.15 + d.bonus(), true));
        Decision u = DrumBeats.decide(new Streak(4, 1000), 1010, false, BEAT, 3);
        assertEquals("STREAK n=4 step=5 chance=0.150 charged=false result=reset:uncharged",
                DrumBeats.line(u, 5.0, 0.15 + u.bonus(), false));
        Decision p = DrumBeats.decide(Streak.NONE, 1010, true, null, 0);
        assertEquals("STREAK n=0 step=0 chance=0.150 charged=true result=plain", DrumBeats.line(p, 0.0, 0.15, true));
    }

    /** The per-player store: put, read, forget (quit, death, a slot change ...), and per player. */
    @Test
    void theStoreForgetsPerPlayer() {
        DrumStreaks store = new DrumStreaks();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertEquals(Streak.NONE, store.of(a));
        store.put(a, new Streak(3, 10));
        store.put(b, new Streak(1, 10));
        store.forget(a);
        assertEquals(Streak.NONE, store.of(a));
        assertEquals(new Streak(1, 10), store.of(b));
        store.put(b, Streak.NONE);
        assertEquals(0, store.held(), "a NONE is not held");
    }

    /** The record refuses what it can see. */
    @Test
    void theRecordRefusesNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.DrummingBeat("", 100, List.of(3.0), "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.DrummingBeat("w", 0, List.of(3.0), "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.DrummingBeat("w", 100, List.of(), "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.DrummingBeat("w", 100, List.of(-1.0), "a", "b"));
        assertNull(new EnchantBehaviour.DrummingBeat("w", 100, List.of(3.0), " ", null).beatSound(), "blank is none");
    }
}
