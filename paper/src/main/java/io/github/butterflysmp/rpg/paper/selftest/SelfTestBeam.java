package io.github.butterflysmp.rpg.paper.selftest;

import java.util.Locale;
import java.util.UUID;

/**
 * The selftest's lines for the Knell's beam (PLAN-dragon-mobs.md step R5; GATE-dragon-beam.md). Pure, so
 * {@code SelfTestBeamStepsTest} pins each exact text. It reads; it never judges.
 *
 * <ul>
 *   <li><b>BEAM</b> {@code <tag>}: each tagged beam mob within {@link #REACH} blocks is made to begin a beam NOW
 *       ({@code MobBeamer.force}, run on the mob's own thread). The beam's own {@code [Rpg] BEAM ...} lines follow from
 *       the mob's loop: the next tick's attempt, then the fire.</li>
 *   <li><b>PROBE_AT</b> {@code <tag>}: each tagged mob's feet, to three places.</li>
 * </ul>
 */
final class SelfTestBeam {

    /** The beam step's reach, BLOCKS: wider than the other steps' 16, so a row can force a Knell beyond the beam's 40. */
    static final double REACH = 48.0;

    private SelfTestBeam() {}

    static String driveLine(String tag, UUID id) {
        return "DRIVE beam tag=" + tag + " " + id;
    }

    /** The force did not take: the mob has no beam loop, or is mid-telegraph. */
    static String refusedLine(String tag, UUID id, String why) {
        return "DRIVE beam tag=" + tag + " " + id + " refused=" + why;
    }

    static String noneLine(String tag, String what, int reach) {
        return what + " tag=" + tag + " none within " + reach + " blocks";
    }

    static String atLine(String tag, UUID id, double x, double y, double z) {
        return String.format(Locale.ROOT, "AT tag=%s %s x=%.3f y=%.3f z=%.3f", tag, id, x, y, z);
    }

    /** TELEPORT_DRAW's argument (dragon-mobs step R6). */
    record Draw(String tag, io.github.butterflysmp.rpg.core.mob.BeamTeleport.Draw draw) {}

    /** {@code "<tag> <right|left> <degrees>"}; a finite angle, any value (the arc is not checked here). */
    static java.util.Optional<Draw> parseDraw(String arg) {
        String[] w = arg.trim().split("\\s+");
        if (w.length != 3 || w[0].isEmpty()) return java.util.Optional.empty();
        io.github.butterflysmp.rpg.core.mob.BeamTeleport.Side side = switch (w[1]) {
            case "right" -> io.github.butterflysmp.rpg.core.mob.BeamTeleport.Side.RIGHT;
            case "left" -> io.github.butterflysmp.rpg.core.mob.BeamTeleport.Side.LEFT;
            default -> null;
        };
        if (side == null) return java.util.Optional.empty();
        try {
            double a = Double.parseDouble(w[2]);
            if (!Double.isFinite(a)) return java.util.Optional.empty();
            return java.util.Optional.of(new Draw(w[0], new io.github.butterflysmp.rpg.core.mob.BeamTeleport.Draw(side, a)));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    static String drawLine(String tag, UUID id, io.github.butterflysmp.rpg.core.mob.BeamTeleport.Draw d) {
        return String.format(Locale.ROOT, "DRIVE teleport_draw tag=%s %s side=%s first=%.3f", tag, id,
                d.side() == io.github.butterflysmp.rpg.core.mob.BeamTeleport.Side.RIGHT ? "right" : "left",
                d.firstAngle());
    }
}
