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

    /**
     * A body in its death animation: a living entity that is dead and still near the player. The run waits for these
     * to leave.
     *
     * <p><b>"Still in the world" is the CALLER's job, not a term here</b> (the ST9 fix, 2026-11-14): the run asks only
     * about entities {@code getNearbyEntities} returns, and a body removed from its level is no longer in the level's
     * entity lookup, so it is never presented. What a dying mob reports between death and removal, read from the
     * pinned server jar (Paper 26.1.2 build 74, {@code javap -c -p}): {@code CraftEntity#isDead} is
     * {@code !entity.isAlive()}, true from the moment health reaches 0 ({@code LivingEntity#isAlive} is
     * {@code !isRemoved() && getHealth() > 0 && !dead}); {@code LivingEntity#baseTick} calls {@code tickDeath} while
     * {@code isDeadOrDying()}, and {@code tickDeath} removes the body (KILLED) on the tick {@code ++deathTime} reaches
     * 20. The old third term was {@code CraftEntity#isValid}, which is {@code entity.isAlive() && entity.valid}: false
     * for EVERY dead body, so {@code dead && valid} could never hold and every wait returned at {@code ticks=0}.
     */
    static boolean dying(boolean living, boolean dead) {
        return living && dead;
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
     * How long after the row-start clean the LEFTOVER count is taken. The clean's removals are not immediate: each is
     * scheduled on the mob's OWN scheduler ({@code PaperScheduler.onEntity} -> {@code FoliaEntityScheduler#run} ->
     * {@code runDelayed(…, 1L)} -> {@code EntityScheduler#schedule}, keyed {@code tickCount + 1}, run by that entity's
     * next {@code executeTick}; read from the server jar of build 74). So a count in the same tick would count the mobs
     * the clean has just removed. Two ticks covers the entity's next tick whichever order the server ticks the player
     * and the mob in.
     */
    static final int LEFTOVER_WAIT_TICKS = 2;

    /** The radius the LEFTOVER count covers: PROBE_ENTITIES' and PROBE_BODY's 16. */
    static final int LEFTOVER_RADIUS = 16;

    /**
     * A body the row-start clean should have left no trace of (GATE-dragon-fix.md, C3): living, not the player, not an
     * armour stand (scenery, never staged), and not already dead. Anything else a row did not spawn and finds beside
     * it is named, so a row never starts dirty without saying so.
     */
    static boolean leftover(boolean living, boolean player, boolean armourStand, boolean dead) {
        return living && !player && !armourStand && !dead;
    }

    /** One line, logged after every row-start clean. The gate predicts {@code LEFTOVER living=0 radius=16}. */
    static String leftoverLine(int radius, List<String> found) {
        return "LEFTOVER living=" + found.size() + " radius=" + radius
                + (found.isEmpty() ? "" : " [" + String.join(", ", found) + "]");
    }

    /** The DIFFICULTY step's argument; empty for anything but the four (the loader refuses it by name). */
    static java.util.Optional<org.bukkit.Difficulty> difficulty(String arg) {
        try {
            return java.util.Optional.of(org.bukkit.Difficulty.valueOf(arg.trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    /** Why a step's argument is refused at load, or empty. Only the verbs with a grammar of their own are checked. */
    static java.util.Optional<String> refusal(Scenario.Verb verb, String arg) {
        return switch (verb) {
            case SPAWN -> SelfTestSpawn.parse(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("spawn takes '<tag> <frozen|live> <mob> [stage <n>] [<gs>]', got '" + arg + "'");
            case ASK -> SelfTestAsk.parse(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("ask takes '<n> <timeout ticks> <question>', got '" + arg + "'");
            case DIFFICULTY -> difficulty(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("difficulty takes peaceful, easy, normal or hard, got '" + arg + "'");
            case TELEPORT_DRAW -> SelfTestBeam.parseDraw(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("teleport_draw takes '<tag> <right|left> <degrees>', got '" + arg + "'");
            case AWAIT_FUSE -> SelfTestBlast.parseAwait(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("await_fuse takes '<tag> <min fuse> <timeout ticks>', got '" + arg + "'");
            case DETONATE, IGNITE, PROBE_FUSE, THAW, PROBE_MOTION, BEAM, PROBE_AT -> SelfTestBlast.oneTag(arg) ? java.util.Optional.empty()
                    : java.util.Optional.of(verb.name().toLowerCase(java.util.Locale.ROOT) + " takes one tag, got '"
                            + arg + "'");
            default -> java.util.Optional.empty();
        };
    }

    /** The longest a {@code death_rules} row waits for its player to respawn before the run aborts. */
    static final int RESPAWN_CAP_TICKS = 200;

    /**
     * Whether the run waits instead of aborting: only a row that declared {@code death_rules} may lose its player,
     * and only for as long as the player is actually dead. Every other death still aborts the run, as it did before
     * (runs 1 and 2 of the {@code 61e722f2} boot).
     */
    static boolean waitForRespawn(boolean deathExpected, boolean dead) {
        return deathExpected && dead;
    }

    /**
     * The game rules ONE ROW changed, each with the value the row found. The FIRST value recorded is the one restored,
     * so setting a rule twice in a row cannot overwrite what the world had. Drained at the row's end.
     */
    static final class RowRules<R> {
        private final Map<R, Boolean> before = new LinkedHashMap<>();

        void record(R rule, Boolean found) {
            if (!before.containsKey(rule)) before.put(rule, found);   // not putIfAbsent: a null found value is real
        }

        /** The rules to put back, each with its found value, and the store empties. */
        Map<R, Boolean> drain() {
            Map<R, Boolean> out = new LinkedHashMap<>(before);
            before.clear();
            return out;
        }

        boolean active() { return !before.isEmpty(); }
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
