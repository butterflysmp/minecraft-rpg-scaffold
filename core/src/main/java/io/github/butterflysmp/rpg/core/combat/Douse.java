package io.github.butterflysmp.rpg.core.combat;

import java.util.Locale;
import java.util.Optional;

/**
 * WATER PUTS OUT SCORCH (Ben, 2026-11-27: <i>"Water should be able to put out every fire ability that scorches and
 * ignites"</i>; the seat's Y1 to Y7 and E1 to E4). The pure decisions; the adapter's one reading of the server
 * ({@code Entity.isInWater()} and {@code Entity.isInRain()}) is the only server-facing part.
 *
 * <h2>ONE WET DECISION, USED AT THREE SEAMS</h2>
 *
 * <p>{@link #wet} answers "is this body wet NOW", from the two booleans the adapter reads. The same answer is asked:
 * <ul>
 *   <li>at each Scorch APPLICATION (Y3): a wet body gains no Scorch, and no flame;</li>
 *   <li>at each Scorch BURN TICK, before its damage (Y2): a wet body is doused, that tick deals nothing, the burn
 *       ends;</li>
 *   <li>at a DEATH (E1): a wet body does not ignite, by EITHER of Ignite's clauses ({@link #atDeath}).</li>
 * </ul>
 *
 * <p>A splash water bottle ({@link Cause#SPLASH}, Ben's D) is a MOMENT, not a state (E2): it douses a Scorch at the
 * splash's event and leaves no lasting wetness, so it is never an answer of {@link #wet}.
 *
 * <p>Soaked (Ben's C) is NOT a douser: <i>"we'll come back to C when we do our Water Element pass"</i>.
 */
public final class Douse {

    private Douse() {}

    /** What put the fire out. The lower-case name is the trace line's {@code cause=}. */
    public enum Cause {
        WATER, RAIN, SPLASH;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * THE ONE WET DECISION. Water wins over rain when both hold (CC'S CALL: a body standing in water in the rain names
     * the water; the alternative, rain first, changes only the {@code cause=} on the line, never whether it is wet).
     */
    public static Optional<Cause> wet(boolean inWater, boolean inRain) {
        if (inWater) return Optional.of(Cause.WATER);
        if (inRain) return Optional.of(Cause.RAIN);
        return Optional.empty();
    }

    /** What a death does, by Ignite's two clauses and the wet decision (E1). */
    public enum Ignition {
        /** Clause 1: it died while scorched, and is dry. The death listener detonates. */
        CLAUSE_1,
        /** Clause 2: a fire hit killed it, unscorched, and it is dry. The killing blow detonates. */
        CLAUSE_2,
        /** It would have ignited by either clause, and it is wet: no detonation, an IGNITE refused line. */
        REFUSED,
        /** Neither clause holds: not scorched, and the killing blow was not fire. Nothing, and no line. */
        NONE
    }

    /**
     * E1'S DECISION TABLE. {@code scorched} is "scorched at its death"; {@code fireKill} is "the killing blow was a fire
     * hit that accrues Scorch". A scorched body is clause 1's whatever killed it (the death listener owns it), so
     * clause 2 is asked only of an unscorched one, exactly as {@code BukkitCombatant}'s {@code wasScorched} guard
     * keeps the two from both firing.
     */
    public static Ignition atDeath(boolean scorched, boolean fireKill, Optional<Cause> wet) {
        if (!scorched && !fireKill) return Ignition.NONE;
        if (wet.isPresent()) return Ignition.REFUSED;
        return scorched ? Ignition.CLAUSE_1 : Ignition.CLAUSE_2;
    }
}
