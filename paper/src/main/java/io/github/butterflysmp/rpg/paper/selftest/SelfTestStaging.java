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

    /**
     * WHERE {@code hold:} STAGES A WEAPON THAT IS NOT IN THE HOTBAR (2026-11-21, the seat's brief, PR B item 3).
     *
     * <p>A {@code record} is a small immutable value class: its fields, {@code equals} and {@code toString} come free.
     * {@code from} is the weapon's slot (9-35), {@code to} the hotbar slot (0-8); both are -1 for {@link Kind#NONE}.
     */
    record HotbarPlan(Kind kind, int from, int to) {
        /** MOVE into an empty hotbar slot; SWAP with an occupied one that is not the stone; NONE, nothing to do. */
        enum Kind { MOVE, SWAP, NONE }
    }

    /**
     * The hotbar slot a weapon at {@code from} is staged into. An EMPTY slot first, as before, so nothing is displaced
     * when it need not be. With none empty, the first slot that does NOT hold the Ability Stone is SWAPPED: the stone's
     * slot is the player's choice and is never given up. NONE when the weapon is not in the inventory ({@code from < 0})
     * or every hotbar slot holds a stone.
     *
     * @param hotbarEmpty  the nine hotbar slots, true where empty
     * @param hotbarStone  the nine hotbar slots, true where the Ability Stone is
     */
    static HotbarPlan hotbarPlan(int from, boolean[] hotbarEmpty, boolean[] hotbarStone) {
        if (from < 0) return new HotbarPlan(HotbarPlan.Kind.NONE, -1, -1);
        for (int i = 0; i < 9; i++) if (hotbarEmpty[i]) return new HotbarPlan(HotbarPlan.Kind.MOVE, from, i);
        for (int i = 0; i < 9; i++) if (!hotbarStone[i]) return new HotbarPlan(HotbarPlan.Kind.SWAP, from, i);
        return new HotbarPlan(HotbarPlan.Kind.NONE, -1, -1);
    }

    /**
     * {@code HOLD_ONLY}'s plan (2026-11-22, the seat's brief, PR 1): where each hotbar item that is NOT the kept weapon
     * goes, so the weapon is the only thing left in the hotbar. Pure, so it is a unit row.
     *
     * @param hotbarOccupied which of hotbar slots 0-8 hold anything
     * @param keep           the hotbar slot holding the weapon, which stays; {@code -1} when the weapon is in the main
     *                       inventory (it is then moved into slot 0 by the caller, after this plan has emptied slot 0)
     * @param mainEmpty      which of the 27 main-inventory slots (index 0 is inventory slot 9) are empty. The weapon's own
     *                       main slot, if it is there, is NOT empty and is not used
     * @return {@code dest[i]}, the inventory slot (9-35) hotbar slot {@code i} moves to, or {@code -1} when it stays
     *         (empty, or the weapon); EMPTY when there are fewer empty main slots than items to move, so nothing at all is
     *         moved and nothing is ever dropped
     */
    static java.util.Optional<int[]> hotbarOnlyPlan(boolean[] hotbarOccupied, int keep, boolean[] mainEmpty) {
        int[] dest = new int[9];
        java.util.Arrays.fill(dest, -1);
        int next = 0;
        for (int i = 0; i < 9; i++) {
            if (!hotbarOccupied[i] || i == keep) continue;
            while (next < mainEmpty.length && !mainEmpty[next]) next++;
            if (next >= mainEmpty.length) return java.util.Optional.empty();
            dest[i] = 9 + next;
            next++;
        }
        return java.util.Optional.of(dest);
    }

    /** The reach a {@code probe:} with no number reads, as every row before 2026-11-23 did. */
    static final int PROBE_DEFAULT_REACH = 16;

    /** The furthest a {@code probe:} may be told to read: the row-start clean's own 48 is not the limit, 64 is. */
    static final int PROBE_MAX_REACH = 64;

    /** A probe's tag and the reach it reads within. */
    record ProbeArg(String tag, int reach) {}

    /**
     * {@code "<tag>"} or {@code "<tag> <reach>"} (the seat's rule 4, 2026-11-23); empty for anything else, which the
     * loader refuses by name. A whole number 1 to {@value #PROBE_MAX_REACH}.
     */
    static java.util.Optional<ProbeArg> probeArg(String arg) {
        String[] f = arg.trim().split("\\s+");
        if (f.length == 0 || f[0].isEmpty() || f.length > 2) return java.util.Optional.empty();
        if (f.length == 1) return java.util.Optional.of(new ProbeArg(f[0], PROBE_DEFAULT_REACH));
        if (!f[1].matches("\\d{1,2}")) return java.util.Optional.empty();
        int reach = Integer.parseInt(f[1]);
        if (reach < 1 || reach > PROBE_MAX_REACH) return java.util.Optional.empty();
        return java.util.Optional.of(new ProbeArg(f[0], reach));
    }

    /**
     * {@code CLEAR_INVENTORY}'s plan (the seat's rule 1, 2026-11-23): the storage slots (0-35) to empty, ascending. Every
     * occupied one but the Nexus item's: a row's gives then land in his hotbar, whatever he carried. Pure, so it is a
     * unit row.
     */
    static int[] clearPlan(boolean[] occupied, boolean[] nexus) {
        int n = 0;
        int[] out = new int[occupied.length];
        for (int i = 0; i < occupied.length; i++) if (occupied[i] && !nexus[i]) out[n++] = i;
        return java.util.Arrays.copyOf(out, n);
    }

    /** The ticks after a row's last step before its first drain (unchanged since before P-R2). */
    static final int ROW_END_TICKS = 2;

    /** P-R2 (2026-11-25): the ONE tick between a row's first drain and its second, after which DONE is logged. */
    static final int ROW_END_REDRAIN_TICKS = 1;

    /**
     * THE SAVED-BLOCKS READ-BACK (2026-11-26): after a restore, how many of the restored states the world now holds again,
     * by {@code matches} (paper: the block's data equals the saved state's). Logged against the count restored, so a gate that
     * places blocks PROVES they went back; GATE-broken-bottle BB8's "no water or glass left" could only infer it.
     */
    static <T> int readBack(java.util.List<T> restored, java.util.function.Predicate<T> matches) {
        int n = 0;
        for (T t : restored) if (matches.test(t)) n++;
        return n;
    }

    /** {@code blocksBack=<matched>/<restored>}, the read-back's field on the CLEAN and RESTORED lines. */
    static String readBackField(int matched, int restored) {
        return "blocksBack=" + matched + "/" + restored;
    }

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

    /**
     * THE PROBE LAG (dragon-mobs step R8; found by DB6R3 at the boot of 45bc33ce). PROBE_PAIR and PROBE_SHOT read their
     * mob on ITS OWN scheduler ({@code Scheduler.onEntity}: never another thread's state, the Folia rule), which runs the
     * read on the mob's next tick. A kill in the step after it runs first, in the same tick, and the read is of a dead
     * mob: DB6R3's PAIR lines read {@code stage=none} for a live pair. The reads stay where they are, so every reading
     * already taken keeps its tick; a row must WAIT {@link #PROBE_LAG_TICKS} between such a probe and a kill instead,
     * and the loader refuses one that does not.
     */
    static final int PROBE_LAG_TICKS = 2;

    /** Why a row's steps are refused for the probe lag, or empty. Only {@code wait:} steps count toward the 2 ticks. */
    static java.util.Optional<String> probeLag(List<Scenario.Step> steps) {
        Scenario.Verb pending = null;
        long waited = 0;
        for (Scenario.Step step : steps) {
            Scenario.Verb v = step.verb();
            if (v == Scenario.Verb.PROBE_PAIR || v == Scenario.Verb.PROBE_SHOT) {
                pending = v;
                waited = 0;
            } else if (pending != null && v == Scenario.Verb.WAIT) {
                waited += Long.parseLong(step.arg().trim());
                if (waited >= PROBE_LAG_TICKS) pending = null;
            } else if (pending != null && isKill(step)) {
                return java.util.Optional.of("'" + step.arg().trim() + "' comes " + waited + " tick(s) after "
                        + pending.name().toLowerCase(java.util.Locale.ROOT) + ", which reads on the mob's own scheduler a tick"
                        + " later; wait " + PROBE_LAG_TICKS + " before the kill (the probe lag)");
            }
        }
        return java.util.Optional.empty();
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

    /**
     * C2, CLOSED HERE (dragon-mobs step R7b): the radius the row-start clean ends what can OUTLIVE a row in. 48, the
     * reach of every shot and probe step. The boot of 3dc0e184 read SH7R2's shot destroyed by SH4R2's grenade field (an
     * ability area field, age 9), because the clean removed tagged MOBS and nothing else.
     */
    static final int LINGER_RADIUS = 48;

    /**
     * WHAT CAN OUTLIVE A ROW, besides its mobs (measured from the code, step R7b): a point-scheduled chain of core work
     * (an area field's pulses, a projectile's flight, a ray's steps, an ember's tracking, an Ignite fuse:
     * {@code PendingWork}), and the ENTITIES those chains move: a vanilla or ability projectile (an arrow, a fireball
     * body), an area-effect cloud, and a marker of ours (an Item or a fireball carrying {@code marker_entity}). The clean
     * ends the work FIRST (so no chain re-spawns what it removes) and then each such entity. Never a player.
     * Entity-bound work (a status's ticks, {@code scheduleOn}) dies with its mob, which the tagged-mob clean removes.
     */
    static boolean lingering(boolean projectile, boolean cloud, boolean marker, boolean player) {
        return !player && (projectile || cloud || marker);
    }

    /** PROBE_INVISIBILITY's line (the Blink Tome's gate). {@code ticks} is the remaining duration, -1 infinite, 0 none. */
    static String invisibilityLine(boolean present, int ticks, int amplifier, boolean particles, boolean blinked) {
        return "INVIS present=" + present + " ticks=" + ticks + " amp=" + amplifier + " particles=" + particles
                + " blinked=" + blinked;
    }

    /** One line after the clean's linger pass, beside LEFTOVER. A row predicts {@code LINGER work=0 entities=0 radius=48}. */
    static String lingerLine(int radius, int work, List<String> ended) {
        return "LINGER work=" + work + " entities=" + ended.size() + " radius=" + radius
                + (ended.isEmpty() ? "" : " [" + String.join(", ", ended) + "]");
    }

    /** The DIFFICULTY step's argument; empty for anything but the four (the loader refuses it by name). */
    static java.util.Optional<org.bukkit.Difficulty> difficulty(String arg) {
        try {
            return java.util.Optional.of(org.bukkit.Difficulty.valueOf(arg.trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    /** The SATURATION step's argument: a number in vanilla's range, 0 to 20. */
    static boolean saturationArg(String arg) {
        try {
            float f = Float.parseFloat(arg.trim());
            return f >= 0 && f <= 20;
        } catch (NumberFormatException e) {
            return false;
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
            case STRIKE_AT_AGE -> SelfTestStrike.parseAtAge(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("strike_at_age takes '<tag> <age> <timeout ticks>', got '" + arg + "'");
            case BLINK -> SelfTestBlink.parse(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("blink takes '<tag> <angle> <distance>', got '" + arg + "'");
            case WATCH_SWINGS -> arg.trim().equals("on") || arg.trim().equals("off") ? java.util.Optional.empty()
                    : java.util.Optional.of("watch_swings takes on or off, got '" + arg + "'");
            case TELL -> arg.isBlank() ? java.util.Optional.of("tell takes the text to show, got nothing")
                    : java.util.Optional.empty();
            case AWAIT_FIGHT -> SelfTestAwaitFight.parse(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("await_fight takes '<ceiling ticks>', a whole number of at least 1, got '"
                            + arg + "'");
            case PROBE -> probeArg(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("probe takes '<tag>' or '<tag> <reach 1-" + PROBE_MAX_REACH + ">', got '"
                            + arg + "'");
            case CLASS -> java.util.List.of("melee", "ranger", "mage", "none").contains(arg.trim()) ? java.util.Optional.empty()
                    : java.util.Optional.of("class takes melee, ranger, mage or none, got '" + arg + "'");
            case ACCESSORY -> arg.trim().matches("clear|equip [0-3]") ? java.util.Optional.empty()
                    : java.util.Optional.of("accessory takes 'equip <0-3>' or 'clear', got '" + arg + "'");
            case SATURATION -> saturationArg(arg) ? java.util.Optional.empty()
                    : java.util.Optional.of("saturation takes a number from 0 to 20, got '" + arg + "'");
            case CLEAR_INVENTORY, CLEAR_STATE, PAD, PROBE_REGEN, PROBE_INVISIBILITY -> arg.isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(verb.name().toLowerCase(java.util.Locale.ROOT) + " takes nothing, got '"
                            + arg + "'");
            case PREFLIGHT -> SelfTestPreflight.parse(arg).isPresent() ? java.util.Optional.empty()
                    : java.util.Optional.of("preflight takes '<east from> <south from> <east to> <south to>', whole"
                            + " blocks within 64, from <= to, got '" + arg + "'");
            case DETONATE, IGNITE, PROBE_FUSE, THAW, PROBE_MOTION, BEAM, PROBE_AT, SHOT, PROBE_SHOT, AIM_SHAPE, STRIKE_LOOK,
                    SHOOT_ARROW, PROBE_SHIELD, POUNCE, PROBE_PAIR, LIGHT -> SelfTestBlast.oneTag(arg) ? java.util.Optional.empty()
                    : java.util.Optional.of(verb.name().toLowerCase(java.util.Locale.ROOT) + " takes one tag, got '"
                            + arg + "'");
            default -> java.util.Optional.empty();
        };
    }

    /**
     * Why the run may not go on, or empty ({@code SelfTestRun.invalidated}, asked before every pass).
     *
     * <p><b>A FIGHT'S OWN WORLD COUNTS AS THE RUN'S</b> (GATE-fight-arena.md; the seat's ruling 2a, 2026-11-18). The
     * arena run aborted inside AE1 three times ("the player left the dev world"), because every arena row takes him
     * into the fight's world. A run a row took there must not abort; any OTHER world still does.
     */
    static java.util.Optional<String> invalid(boolean online, boolean dead, boolean inRunWorld, boolean inFightWorld,
                                              int onlinePlayers) {
        if (!online) return java.util.Optional.of("the player left");
        if (dead) return java.util.Optional.of("the player died");
        if (!inRunWorld && !inFightWorld) return java.util.Optional.of("the player left the dev world");
        if (onlinePlayers != 1) return java.util.Optional.of(onlinePlayers + " players are online");
        return java.util.Optional.empty();
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
