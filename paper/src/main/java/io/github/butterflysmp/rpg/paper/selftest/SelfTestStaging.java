package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * THE FOUR STAGING DECISIONS THE RUN MAKES BETWEEN ROWS, kept pure so each has a unit test. Every one of them answers
 * a leak the {@code 61e722f2} boot's readings found (seat, 2026-11-10; GATE-selftest.md ST8-ST11 are their live rows).
 *
 * <ol>
 *   <li><b>Row start removes every selftest-tagged entity</b> ({@link #removedAtRowStart}). WS3's tag-{@code n}
 *       neighbour was never removed, so it took a hit in SU2.</li>
 *   <li><b>After a kill, the run waits until no dying body is left</b> ({@link #dying}, {@link #isKill}).
 *       {@code /kill} leaves a mob in its death animation for about a second. WS2c's control zombie's apply hit the
 *       dying wither skeleton killed a moment before ("Refused: wither_skeleton is immune").</li>
 *   <li><b>Placed blocks are put back at the next unchained row, not at the end of the run</b> ({@link RowBlocks}).
 *       Run 2 drowned its player because each Sunder row staged from the floor the last one left him on.</li>
 *   <li><b>The inventory is put back at every gate boundary</b> ({@link #gateChanged}). In run 3, DH1 found "no
 *       hotbar slot holds boltor" after eight gates' gives.</li>
 * </ol>
 *
 * <p>A CHAINED row (its first step is {@code after: <row>}) reads the state the row before it left: its blocks, its
 * mobs, its held item. So {@link #chained} rows get none of the row-start work. Only three rows chain: DH3, DH6, WB3.
 */
final class SelfTestStaging {

    private SelfTestStaging() {}

    /** The longest the run waits for a killed body to leave; vanilla's death animation is 20 ticks. */
    static final int SETTLE_CAP_TICKS = 40;

    /** A row whose first step is {@code after:} continues the row before it, so its staging is left alone. */
    static boolean chained(Scenario scenario) {
        return !scenario.steps().isEmpty() && scenario.steps().get(0).verb() == Scenario.Verb.AFTER;
    }

    /** Row start removes this entity: anything the instrument tagged, never a player. */
    static boolean removedAtRowStart(boolean isPlayer, boolean taggedBySelftest) {
        return !isPlayer && taggedBySelftest;
    }

    /** A body in its death animation: still in the world (valid), already dead. The run waits for these to leave. */
    static boolean dying(boolean living, boolean dead, boolean valid) {
        return living && dead && valid;
    }

    /** A step that can leave a dying body behind it. */
    static boolean isKill(Scenario.Step step) {
        return step.verb() == Scenario.Verb.CMD && step.arg().trim().startsWith("kill ");
    }

    /** The next scenario belongs to a different gate than the last one, so the inventory goes back first. */
    static boolean gateChanged(String previousGate, String nextGate) {
        return previousGate != null && !previousGate.equals(nextGate);
    }

    /**
     * The blocks a chain of rows has saved, first save wins. The run drains them, newest first, at the next unchained
     * row, so the restore undoes the saves in the reverse order they were taken.
     */
    static final class RowBlocks<K, S> {
        private final Map<K, S> saved = new LinkedHashMap<>();
        private int total;

        /** The FIRST state saved at a position is the one restored: a later save of the same block is ignored. */
        void save(K position, S state) {
            if (saved.putIfAbsent(position, state) == null) total++;
        }

        /** Every state saved since the last drain, newest first, and the store empties. */
        List<S> drainNewestFirst() {
            List<S> out = new ArrayList<>(saved.values());
            Collections.reverse(out);
            saved.clear();
            return out;
        }

        int pending() { return saved.size(); }

        /** Every block ever saved in this run: the RESTORED line's {@code blocks=}, unchanged in meaning. */
        int total() { return total; }
    }
}
