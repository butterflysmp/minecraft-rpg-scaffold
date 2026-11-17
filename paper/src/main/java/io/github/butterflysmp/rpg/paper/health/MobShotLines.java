package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Locale;
import java.util.UUID;

/**
 * THE PHALANX'S SHOT, AS THE LOG READS IT (dragon-mobs step R7a, the seat's ruling 9): one line at the fire, one at
 * each ending, so a gate reads a shot's whole life from the server. Pure, so {@code MobShotLinesTest} pins each text.
 * Every position is to three places; {@code flown} is the straight line from the shot's ORIGIN, the quantity its range
 * limit is measured in.
 */
public final class MobShotLines {

    private MobShotLines() {}

    private static String v(Vec3 p) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", p.x(), p.y(), p.z());
    }

    private static String head(String what, String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin) {
        return String.format(Locale.ROOT, "SHOT %s mob=%s id=%s shot=%s age=%d at=%s flown=%.3f", what, mobId, mob,
                shot, age, v(at), Math.sqrt(origin.distanceSquared(at)));
    }

    public static String fired(String mobId, UUID mob, int gs, boolean forced, long tick, UUID shot, String targetKind,
                               UUID target, Vec3 from, Vec3 aim, double dealt) {
        return String.format(Locale.ROOT, "SHOT fired mob=%s id=%s gs=%d forced=%s tick=%d shot=%s target=%s %s from=%s"
                        + " aim=%s dealt=%.3f", mobId, mob, gs, forced, tick, shot, targetKind, target, v(from), v(aim),
                dealt);
    }

    /** A forced attempt that found no one within the acquire range (the Phalanx's own empty attempts log nothing). */
    public static String none(String mobId, UUID mob, long tick, double range) {
        return String.format(Locale.ROOT, "SHOT none mob=%s id=%s tick=%d range=%.1f", mobId, mob, tick, range);
    }

    /** The instrument's read of a live shot (the PROBE_SHOT step): where it is, its age, how far from its origin. */
    public static String probe(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, Vec3 velocity,
                               io.github.butterflysmp.rpg.core.mob.BeamBox box,
                               io.github.butterflysmp.rpg.core.mob.BeamBox shape) {
        // The server's HITTABLE box (the carrier's) and the visible SHAPE (the display's block, where the server puts
        // it), and whether the shape lies wholly inside the box: the seat's ruling 2 on #201, read as numbers.
        boolean inside = shape.min().x() >= box.min().x() && shape.min().y() >= box.min().y()
                && shape.min().z() >= box.min().z() && shape.max().x() <= box.max().x()
                && shape.max().y() <= box.max().y() && shape.max().z() <= box.max().z();
        return head("at", mobId, mob, shot, age, at, origin) + " v=" + v(velocity) + " box=" + v(box.min()) + ".."
                + v(box.max()) + " shape=" + v(shape.min()) + ".." + v(shape.max()) + " inside=" + inside;
    }

    /** Vanilla damage that is not an attack (a wall, fire, the void), refused: logged once per cause per shot. */
    public static String refused(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, String cause) {
        return head("refused", mobId, mob, shot, age, at, origin) + " cause=" + cause;
    }

    public static String hit(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, String victimKind,
                             UUID victim, double dealt) {
        return head("hit", mobId, mob, shot, age, at, origin)
                + String.format(Locale.ROOT, " victim=%s %s dealt=%.3f", victimKind, victim, dealt);
    }

    public static String destroyed(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, String by) {
        return head("destroyed", mobId, mob, shot, age, at, origin) + " by=" + by;
    }

    /** {@code limit} is {@code life} or {@code range}. */
    public static String expired(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, String limit) {
        return head("expired", mobId, mob, shot, age, at, origin) + " limit=" + limit;
    }

    public static String blocked(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, String block) {
        return head("blocked", mobId, mob, shot, age, at, origin) + " block=" + block;
    }

    /** Any other end: its Phalanx gone, the carrier removed or unloaded, a world change, a refused move, a stop. */
    public static String ended(String mobId, UUID mob, UUID shot, int age, Vec3 at, Vec3 origin, String reason) {
        return head("ended", mobId, mob, shot, age, at, origin) + " reason=" + reason;
    }

    /** Its target was lost and it turned to another, or to none (then it flies on straight). Logged on the change. */
    public static String retarget(String mobId, UUID mob, UUID shot, int age, String targetKind, UUID target) {
        return String.format(Locale.ROOT, "SHOT retarget mob=%s id=%s shot=%s age=%d target=%s", mobId, mob, shot, age,
                target == null ? "none" : targetKind + " " + target);
    }

    /** An entity marked as a shot that no live shot owns: removed where it appeared (a chunk load, the enable sweep). */
    public static String orphan(String type, UUID id) {
        return "SHOT orphan removed type=" + type + " uuid=" + id;
    }

    /**
     * The Phalanx's blink, ONE line (step R7b): what triggered it (or none, the instrument's force), where it stood, every
     * attempt in order with the check that refused it (NO_FOOTING with how many heights did not fit and how many had no
     * ground) or where it stands (OK), and where it landed, or none (it stays).
     */
    public static String blink(String mobId, UUID mob, long tick, boolean forced, String triggerKind, UUID trigger, Vec3 from,
                               io.github.butterflysmp.rpg.core.mob.MobBlink.Decision d) {
        StringBuilder tried = new StringBuilder();
        for (var v : d.tried()) {
            if (tried.length() > 0) tried.append(", ");
            tried.append(String.format(Locale.ROOT, "%.3f %.3f %s", v.draw().angleDegrees(), v.draw().distance(), v.outcome()));
            switch (v.outcome()) {
                case NO_FOOTING -> tried.append(" blocked=").append(v.blocked()).append(" ungrounded=").append(v.ungrounded());
                case OK, NO_SIGHT -> tried.append(" at=").append(v(v.landing().orElseThrow()));
                default -> { }
            }
        }
        return String.format(Locale.ROOT, "BLINK mob=%s id=%s tick=%d forced=%s trigger=%s from=%s tried=%d [%s] landed=%s",
                mobId, mob, tick, forced, trigger == null ? "none" : triggerKind + " " + trigger, v(from), d.tried().size(),
                tried, d.landing().map(MobShotLines::v).orElse("none"));
    }

    /** A Phalanx's bow shot, cancelled (the seat's ruling 7): the arrow never enters the world. */
    public static String bowSuppressed(String mobId, UUID mob, long tick) {
        return "BOW suppressed mob=" + mobId + " id=" + mob + " tick=" + tick;
    }
}
