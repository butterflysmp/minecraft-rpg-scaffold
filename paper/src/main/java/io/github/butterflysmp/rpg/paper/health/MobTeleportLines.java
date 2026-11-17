package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamTeleport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Knell's teleport's log lines (PLAN-dragon-mobs.md step R6; the brief's ruling 7): ONE line per decision, and one
 * when a due move does not happen. Pure, so {@code MobTeleportLinesTest} pins each whole text;
 * {@code GATE-dragon-teleport.md} reads the teleport from them. Positions and angles print to three places. Every line
 * starts {@code TELEPORT }.
 */
final class MobTeleportLines {

    private MobTeleportLines() {}

    /**
     * The decision: the Knell and its feet ({@code from}), the target and its feet ({@code center}), the horizontal
     * {@code radius} in blocks, the Knell's {@code bearing} from the target in degrees (0 = east, 90 = south), the side
     * and angle drawn, every candidate tried in order with its verdict, and the landing or {@code none}, with whether
     * {@code Entity#teleport} reported the move ({@code moved=-} when there was no landing).
     */
    static String decision(String mobId, UUID id, String targetKind, UUID target, long tick, Vec3 from,
                           BeamTeleport.Decision d, Vec3 center, double firstAngle, String moved) {
        List<String> entries = new ArrayList<>();
        for (BeamTeleport.Verdict v : d.tried()) entries.add(verdict(v));
        return "TELEPORT mob=" + mobId + " id=" + id + " target=" + targetKind + " " + target + " tick=" + tick
                + " from=" + p(from) + " center=" + p(center)
                + String.format(Locale.ROOT, " radius=%.3f bearing=%.3f", d.radius(), d.bearingDegrees())
                + " side=" + side(d.firstSide()) + String.format(Locale.ROOT, " first=%.3f", firstAngle)
                + " tried=" + d.tried().size() + " [" + String.join(", ", entries) + "]"
                + " landed=" + d.landing().map(MobTeleportLines::p).orElse("none") + " moved=" + moved;
    }

    /** {@code <side> <angle> <verdict>}: {@code ok y=<h> at=<x y z>}, {@code no-sight y=<h>}, {@code unloaded}, or
     * {@code no-footing blocked=<n> ungrounded=<n>}. */
    static String verdict(BeamTeleport.Verdict v) {
        String head = side(v.candidate().side()) + String.format(Locale.ROOT, " %.3f ", v.candidate().angle());
        return head + switch (v.outcome()) {
            case OK -> String.format(Locale.ROOT, "ok y=%.3f at=", v.landing().orElseThrow().y())
                    + p(v.landing().orElseThrow());
            case NO_SIGHT -> String.format(Locale.ROOT, "no-sight y=%.3f", v.landing().orElseThrow().y());
            case UNLOADED -> "unloaded";
            case NO_FOOTING -> "no-footing blocked=" + v.blocked() + " ungrounded=" + v.ungrounded();
        };
    }

    /** A move that was due and did not happen, and why. */
    static String skipped(String mobId, UUID id, UUID target, BeamTeleport.Skip why) {
        return "TELEPORT skipped mob=" + mobId + " id=" + id + " target=" + target + " reason="
                + why.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** A second hit while a move is still pending (only a forced beam could do it: the cycle's minimum exceeds the delay). */
    static String ignored(String mobId, UUID id) {
        return "TELEPORT ignored mob=" + mobId + " id=" + id + " reason=pending";
    }

    static String side(BeamTeleport.Side s) {
        return s == BeamTeleport.Side.RIGHT ? "right" : "left";
    }

    private static String p(Vec3 v) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", v.x(), v.y(), v.z());
    }
}
