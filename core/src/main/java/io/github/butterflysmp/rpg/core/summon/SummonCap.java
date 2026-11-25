package io.github.butterflysmp.rpg.core.summon;

import java.util.OptionalInt;
import java.util.Set;

/**
 * D6, THE CAST AND THE CAP, as a pure decision (the seat's 3a, 2026-11-24). Ben: "each summon weapon can only summon one";
 * a recast replaces it fresh at full cost ("Correct"); at the cap, "Cast refused".
 *
 * <p>Counted BY WEAPON ID, whichever item of that weapon he holds. A cast is REFUSED when his summons from his OTHER
 * weapons, plus this one, would exceed {@code max_summons}, or when the stat has no reading (his profile is loading).
 * Otherwise it REPLACES this weapon's living summon, or makes a NEW one. A refusal costs no mana and starts no cooldown
 * ({@link Verdict#charges}); the caller sends its one notice.
 */
public final class SummonCap {

    private SummonCap() {}

    /** What a summon cast does. */
    public enum Verdict {
        NEW, REPLACE, REFUSED_FULL, REFUSED_UNKNOWN;

        /** Does the cast spend its mana and start its cooldown? Only an allowed one. */
        public boolean charges() {
            return this == NEW || this == REPLACE;
        }
    }

    /**
     * @param weaponId    the summon weapon being cast
     * @param liveWeapons the weapon ids whose summon (his) is alive now, this one included if it lives
     * @param maxSummons  his effective max_summons ({@code ProfileService.maxSummons}); empty when unknown
     */
    public static Verdict decide(String weaponId, Set<String> liveWeapons, OptionalInt maxSummons) {
        if (maxSummons.isEmpty()) return Verdict.REFUSED_UNKNOWN;
        int others = liveWeapons.size() - (liveWeapons.contains(weaponId) ? 1 : 0);
        if (others + 1 > maxSummons.getAsInt()) return Verdict.REFUSED_FULL;
        return liveWeapons.contains(weaponId) ? Verdict.REPLACE : Verdict.NEW;
    }
}
