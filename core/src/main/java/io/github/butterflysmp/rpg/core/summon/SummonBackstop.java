package io.github.butterflysmp.rpg.core.summon;

/**
 * D7's BACKSTOP, and D1(a)'s GUARD (the seat, 2026-11-24): every {@link #PERIOD_TICKS}, each tagged summon body is
 * judged. A body the store does not own, or whose owner is not online in its world, is REMOVED. Otherwise, if its goal set
 * is not ours (a goal came back: D1(a)'s UNVERIFIED "Paper re-adding goals"), it is RESTRIPPED and logged
 * {@code SUMMON goals restored}. Removal is judged before a restrip: a body that is leaving needs no goals fixed.
 */
public final class SummonBackstop {

    private SummonBackstop() {}

    /** The seat's 20 ticks. */
    public static final int PERIOD_TICKS = 20;

    public enum Verdict { KEEP, REMOVE, RESTRIP }

    /**
     * @param owned          the store owns this body
     * @param ownerHere      its owner is online and in the body's world
     * @param foreignGoals   goals on the body that are not ours (in 3b ours is none: a 3b summon does nothing)
     */
    /**
     * O3 (the seat, 2026-11-24): the goals that are NOT OURS, identified BY KEY, never by count, so PR 4's own goals on
     * the body never read as foreign. {@code goalNamespaces} is each goal's key namespace; ours is the plugin's.
     */
    public static int foreignGoals(java.util.Collection<String> goalNamespaces, String ourNamespace) {
        int n = 0;
        for (String ns : goalNamespaces) if (!ourNamespace.equals(ns)) n++;
        return n;
    }

    public static Verdict decide(boolean owned, boolean ownerHere, int foreignGoals) {
        if (!owned || !ownerHere) return Verdict.REMOVE;
        return foreignGoals > 0 ? Verdict.RESTRIP : Verdict.KEEP;
    }
}
