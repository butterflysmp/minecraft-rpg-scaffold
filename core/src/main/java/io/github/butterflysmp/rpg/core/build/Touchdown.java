package io.github.butterflysmp.rpg.core.build;

/**
 * HOW A LEAP'S MARK ENDED, AND WHETHER THAT ENDING SLAMS (Sunder, PLAN-melee-class.md section 8.2, option S1; the
 * seat's ruling 4). Read off ONE {@link SafeLanding#step}: the state before it and the state after.
 *
 * <p>Ben, Q2: <i>"yes it slams where ever you land."</i> The seat's ruling 4 turns that into one rule per ending:
 * <ul>
 *   <li>{@link #LANDED}: the first ground contact after take-off. <b>Slams</b> at the live feet.</li>
 *   <li>{@link #ABSORBED}: a fall that had started was absorbed mid-air (water, a ladder, a cobweb). <b>That IS a
 *       landing</b>, and it slams at the live feet.</li>
 *   <li>{@link #NO_TAKE_OFF}: the leap never left the ground within {@link SafeLanding#TAKE_OFF_TICKS} (a
 *       ceiling). <b>Slams IN PLACE</b>, which is where the live feet still are.</li>
 *   <li>{@link #BACKSTOP}: {@link SafeLanding#BACKSTOP_TICKS} ran out. <b>No slam</b>, and reaching it is a
 *       finding (a leap airborne for 10 s is not a leap anyone designed).</li>
 * </ul>
 * The paper side adds the endings no step can see: {@link #FORGOTTEN} (death or quit mid-leap: <b>no slam</b>, no
 * posthumous burst) and {@link #SUPERSEDED} (a second leap re-armed the mark before the first came down: <b>no
 * slam</b> for the first).
 *
 * <p>{@link #NONE} is every step that is not an ending, and the clearing tick AFTER a landing, whose slam has
 * already happened on the LANDED step.
 */
public enum Touchdown {
    NONE, LANDED, ABSORBED, NO_TAKE_OFF, BACKSTOP, FORGOTTEN, SUPERSEDED;

    /** Whether this ending detonates the leap's {@code on_land}. */
    public boolean slams() {
        return this == LANDED || this == ABSORBED || this == NO_TAKE_OFF;
    }

    /**
     * The {@code /rpg mobtrace} line for this ending, beside PLAYERHIT: {@code LANDING caster=<uuid> source=<id>
     * how=<ending> slam=<true|false> at=<x y z>}, or {@code at=-} when there are no feet (an ending that does not
     * slam). Formatted here, in core, so it is a unit row rather than a boot read.
     */
    public String line(java.util.UUID casterId, String source, io.github.butterflysmp.rpg.core.Vec3 feet) {
        String at = feet == null ? "-"
                : String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", feet.x(), feet.y(), feet.z());
        return "LANDING caster=" + casterId + " source=" + (source == null ? "-" : source) + " how=" + name()
                + " slam=" + slams() + " at=" + at;
    }

    /** The ending one step produced, if any. {@code after} must be {@code before.step(...)}. */
    public static Touchdown of(SafeLanding before, SafeLanding after) {
        SafeLanding.Phase was = before.phase();
        // Already over, or the clearing tick after a landing: the slam (if any) has happened.
        if (was == SafeLanding.Phase.CLEARED || was == SafeLanding.Phase.LANDED) return NONE;
        if (after.phase() == SafeLanding.Phase.LANDED) return LANDED;
        if (after.phase() != SafeLanding.Phase.CLEARED) return NONE;
        // CHECKED BEFORE THE PHASE: step() clears on the backstop from ANY phase, and an AIRBORNE mark cleared by it
        // must not read as ABSORBED -- that would slam a leap the ruling says does not.
        if (after.ticks() > SafeLanding.BACKSTOP_TICKS) return BACKSTOP;
        return was == SafeLanding.Phase.ARMED ? NO_TAKE_OFF : ABSORBED;
    }
}
