package io.github.butterflysmp.rpg.core.summon;

import java.util.List;

/**
 * THE STAT max_summons: how many summons a player may have alive at once (the seat's PR 1, 2026-11-24).
 *
 * <p>Ben, 2026-11-24: <i>"Default is 1 max summon, it will be a new stat in player profile. Certain gear will increase
 * this number too."</i> So the effective value is the player's BASE (the profile's stored value, absent = {@link #DEFAULT})
 * plus every grant from his active gear. <b>No gear grants it in this slice</b>; the sum exists so the first piece that
 * does needs no new arithmetic. The stored base is written only by the dev command (the selftest's override).
 */
public final class MaxSummons {

    private MaxSummons() {}

    /** Ben's default, 2026-11-24. The one copy: {@code storage}'s profile reads it from here. */
    public static final int DEFAULT = 1;

    /**
     * The effective cap: {@code base + sum(grants)}, never below 0.
     *
     * @param storedBaseOrNull the profile's stored base; {@code null} (a profile written before the stat, or one never
     *                         overridden) reads as {@link #DEFAULT}
     * @param gearGrants       each active gear piece's grant; empty in this slice
     */
    public static int effective(Integer storedBaseOrNull, List<Integer> gearGrants) {
        int sum = storedBaseOrNull == null ? DEFAULT : storedBaseOrNull;
        for (int g : gearGrants) sum += g;
        return Math.max(0, sum);
    }
}
