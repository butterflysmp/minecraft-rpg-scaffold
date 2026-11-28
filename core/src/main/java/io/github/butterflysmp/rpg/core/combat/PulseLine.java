package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Locale;
import java.util.UUID;

/**
 * ONE PULSE TRACE LINE (Smolder, the seat's Z10, 2026-11-27), written while trace is on. Core builds it; the adapter
 * supplies the two things only the server knows, the target's TYPE and the TICK, and gates it on the trace switch.
 *
 * <p>The label is the ability's id upper-cased ({@code smolder} is {@code SMOLDER}), so no ability id is written in Java.
 * The four shapes, pinned by {@code PulseLineTest}; {@code caster=} is added to the pulse and hit lines, and
 * {@code tick=} to the cancelled line, beside the seat's fields (CC'S CALL: a log with two casters must say whose):
 *
 * <pre>
 * SMOLDER cast caster=&lt;uuid&gt; tick=&lt;t&gt;
 * SMOLDER pulse n=&lt;1..count&gt; tick=&lt;t&gt; origin=&lt;x y z&gt; caster=&lt;uuid&gt;
 * SMOLDER hit n=&lt;n&gt; target=&lt;type uuid&gt; dist=&lt;horizontal&gt; tick=&lt;t&gt; sent=&lt;amount&gt; caster=&lt;uuid&gt;
 * SMOLDER cancelled caster=&lt;uuid&gt; remaining=&lt;n&gt; cause=&lt;cause&gt; tick=&lt;t&gt;
 * </pre>
 *
 * <p>A projectile that BURSTS (Solar Flare, the seat's F13, 2026-11-27) writes the same cast, hit and cancelled lines,
 * and two of its own in place of the pulse line, {@code caster=} added as above:
 *
 * <pre>
 * SOLAR_FLARE shot n=&lt;1..count&gt; dir=&lt;yaw pitch&gt; tick=&lt;t&gt; caster=&lt;uuid&gt;
 * SOLAR_FLARE burst n=&lt;n&gt; cause=&lt;mob|block|range&gt; origin=&lt;x y z&gt; tick=&lt;t&gt; caster=&lt;uuid&gt;
 * </pre>
 *
 * <p>A shot's {@code dir} is in the server's own convention (yaw 0 is south, -90 east, yaw growing to the RIGHT;
 * pitch positive DOWN), from the direction vector the line carries in {@code origin}'s place: the one place a
 * direction is turned into degrees, so a gate reads the fan as the player sees it.
 */
public record PulseLine(String label, Event event, UUID caster, int n, Vec3 origin, UUID target, double dist,
                        double sent, int remaining, String cause) {

    /** Which line. */
    public enum Event { CAST, PULSE, HIT, CANCELLED, SHOT, BURST }

    /** A shot of a bursting fan: {@code direction} rides in {@code origin}'s place, read only by {@link #format}. */
    public static PulseLine shot(String label, UUID caster, int n, Vec3 direction) {
        return new PulseLine(label, Event.SHOT, caster, n, direction, null, 0, 0, 0, null);
    }

    /** Where shot {@code n} burst, and why it stopped there: {@code mob}, {@code block} or {@code range}. */
    public static PulseLine burst(String label, UUID caster, int n, String cause, Vec3 origin) {
        return new PulseLine(label, Event.BURST, caster, n, origin, null, 0, 0, 0, cause);
    }

    /** The server's yaw of a direction, in degrees: 0 south, -90 east, 90 west (SelfTestAim's convention). */
    public static double yawOf(Vec3 d) {
        return Math.toDegrees(Math.atan2(-d.x(), d.z()));
    }

    /** The server's pitch of a direction, in degrees, positive DOWN. */
    public static double pitchOf(Vec3 d) {
        return -Math.toDegrees(Math.atan2(d.y(), Math.sqrt(d.x() * d.x() + d.z() * d.z())));
    }

    public static PulseLine cast(String label, UUID caster) {
        return new PulseLine(label, Event.CAST, caster, 0, null, null, 0, 0, 0, null);
    }

    public static PulseLine pulse(String label, UUID caster, int n, Vec3 origin) {
        return new PulseLine(label, Event.PULSE, caster, n, origin, null, 0, 0, 0, null);
    }

    public static PulseLine hit(String label, UUID caster, int n, UUID target, double dist, double sent) {
        return new PulseLine(label, Event.HIT, caster, n, null, target, dist, sent, 0, null);
    }

    public static PulseLine cancelled(String label, UUID caster, int remaining, String cause) {
        return new PulseLine(label, Event.CANCELLED, caster, 0, null, null, 0, 0, remaining, cause);
    }

    /** The line, with the target's type ({@code zombie}; ignored but for a hit) and the tick it is written on. */
    public String format(String targetType, long tick) {
        return switch (event) {
            case CAST -> label + " cast caster=" + caster + " tick=" + tick;
            case PULSE -> String.format(Locale.ROOT, "%s pulse n=%d tick=%d origin=%.3f %.3f %.3f caster=%s",
                    label, n, tick, origin.x(), origin.y(), origin.z(), caster);
            case HIT -> String.format(Locale.ROOT, "%s hit n=%d target=%s %s dist=%.3f tick=%d sent=%.3f caster=%s",
                    label, n, targetType, target, dist, tick, sent, caster);
            case CANCELLED -> label + " cancelled caster=" + caster + " remaining=" + remaining + " cause=" + cause
                    + " tick=" + tick;
            case SHOT -> String.format(Locale.ROOT, "%s shot n=%d dir=%.3f %.3f tick=%d caster=%s",
                    label, n, yawOf(origin), pitchOf(origin), tick, caster);
            case BURST -> String.format(Locale.ROOT, "%s burst n=%d cause=%s origin=%.3f %.3f %.3f tick=%d caster=%s",
                    label, n, cause, origin.x(), origin.y(), origin.z(), tick, caster);
        };
    }
}
