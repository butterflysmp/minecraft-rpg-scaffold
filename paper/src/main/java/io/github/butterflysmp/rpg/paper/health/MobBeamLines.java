package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobBeam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Knell's beam's log lines (PLAN-dragon-mobs.md step R5; the brief's ruling 7). Pure, so {@code MobBeamLinesTest}
 * pins each whole text; {@code GATE-dragon-beam.md} reads the beam from them. Positions print to three places,
 * directions to six. Every line starts {@code BEAM }.
 */
final class MobBeamLines {

    private MobBeamLines() {}

    /** At the lock: the Knell, its score, whether the instrument forced it, the server tick, its age, the line. */
    static String telegraph(String mobId, UUID id, int gs, boolean forced, long tick, int lived, Vec3 at,
                            String targetKind, UUID target, Vec3 from, Vec3 to, Vec3 dir, double range, int ticks) {
        return "BEAM telegraph mob=" + mobId + " id=" + id + " gs=" + gs + " forced=" + forced + " tick=" + tick
                + " lived=" + lived + " at=" + p(at) + " target=" + targetKind + " " + target + " from=" + p(from)
                + " to=" + p(to) + " dir=" + d(dir) + String.format(Locale.ROOT, " range=%.2f", range)
                + " ticks=" + ticks;
    }

    /**
     * At the fire: the priced damage ({@code base}, before defence), the line as fired ({@code length} after a block's
     * clip), each tested creature as {@code <kind> <uuid> gap=<blocks> <outcome>} in the order the rule kept, and what
     * was dealt before defence, or {@code none}. {@code kinds} is parallel to {@code tested}.
     */
    static String fire(String mobId, UUID id, int gs, double base, long tick, Vec3 at, Vec3 from, Vec3 dir,
                       double length, boolean clippedByBlock, double tolerance, List<MobBeam.Tested> tested,
                       List<String> kinds, boolean hit) {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < tested.size(); i++) {
            MobBeam.Tested t = tested.get(i);
            String gap = Double.isNaN(t.gap()) ? "none" : String.format(Locale.ROOT, "%.3f", t.gap());
            entries.add(kinds.get(i) + " " + t.id() + " gap=" + gap + " " + t.outcome().name().toLowerCase(Locale.ROOT));
        }
        return "BEAM fire mob=" + mobId + " id=" + id + " gs=" + gs + String.format(Locale.ROOT, " base=%.3f", base)
                + " tick=" + tick + " at=" + p(at) + " from=" + p(from) + " dir=" + d(dir)
                + String.format(Locale.ROOT, " length=%.3f", length) + " clip=" + (clippedByBlock ? "block" : "range")
                + String.format(Locale.ROOT, " tolerance=%.2f", tolerance) + " tested=" + tested.size() + " "
                + "[" + String.join(", ", entries) + "]"
                + " dealt=" + (hit ? String.format(Locale.ROOT, "%.3f", base) : "none");
    }

    /** One sound the beam played: when ({@code telegraph} or {@code fire}), the server tick, and what was passed. */
    static String sound(String mobId, UUID id, String when, long tick, io.github.butterflysmp.rpg.core.mob.BeamSound s) {
        return "BEAM sound mob=" + mobId + " id=" + id + " when=" + when + " tick=" + tick + " key=" + s.key()
                + String.format(Locale.ROOT, " volume=%.2f pitch=%.2f", s.volume(), s.pitch());
    }

    /** The E5 seam's line: raised on a hit only. */
    static String report(String mobId, UUID id, String targetKind, UUID target, long tick, Vec3 from, Vec3 to) {
        return "BEAM report mob=" + mobId + " id=" + id + " target=" + targetKind + " " + target + " tick=" + tick
                + " from=" + p(from) + " to=" + p(to);
    }

    /** The Knell was gone (dead, removed, unloaded) during its telegraph: nothing fires. */
    static String cancelled(String mobId, UUID id, int elapsed, int telegraphTicks) {
        return "BEAM cancelled mob=" + mobId + " id=" + id + " elapsed=" + elapsed + "/" + telegraphTicks
                + " reason=gone";
    }

    /** A FORCED attempt found no one in range. The live cycle's empty attempts log nothing. */
    static String none(String mobId, UUID id, long tick, double range) {
        return "BEAM none mob=" + mobId + " id=" + id + " forced=true tick=" + tick
                + String.format(Locale.ROOT, " range=%.2f", range) + " reason=no-target";
    }

    private static String p(Vec3 v) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", v.x(), v.y(), v.z());
    }

    private static String d(Vec3 v) {
        return String.format(Locale.ROOT, "%.6f %.6f %.6f", v.x(), v.y(), v.z());
    }
}
