package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.build.LandingHook;
import io.github.butterflysmp.rpg.core.build.SafeLanding;
import io.github.butterflysmp.rpg.core.build.Touchdown;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is carrying a leap's fall-damage mark (ruling 31; PLAN-build-system.md section 7.6). The rule is core's
 * {@link SafeLanding}; this holds one mark per entity and steps it once a tick with what the server reports.
 *
 * <p><b>A MARK MAY CARRY A SLAM</b> (Sunder; PLAN-melee-class.md section 8.2, S1, and the seat's rulings 4 and 5).
 * The same one mark: immune to the landing's fall damage, and its ending calls the {@link LandingHook} EXACTLY
 * ONCE -- {@code landed} at the live feet for a slamming {@link Touchdown}, {@code ended} otherwise.
 *
 * <p><b>TWO PATHS SEE A LANDING, AND WHICHEVER COMES FIRST SLAMS.</b> A landing high enough to deal fall damage
 * raises a FALL event on the landing move, and that event's {@link #consumeFall} spends the mark; the per-tick step
 * reads the ground contact on its own tick. Their order was never traced (SafeLanding's javadoc). So both detonate:
 * {@link #consumeFall} slams an unfired mark it removes, and the step slams on its LANDED transition and marks the
 * mark fired, so the FALL event that may follow on the same tick only cancels. A low leap raises no FALL event at
 * all, and the step is its only witness. Without the FALL arm, every Sunder landing from above vanilla's fall
 * threshold could have been eaten by the immunity before the step saw the ground -- no slam, and nothing red.
 *
 * <p><b>THREADING.</b> A mark is armed, stepped and read on the entity's own thread: {@link #arm} from the dash's
 * {@code onEntity} task, the step by {@code Scheduler.onEntityLater}, {@link #consumeFall} from the FALL event, and
 * {@link #forget} from the death and quit events, all of which run on the thread that owns the entity. So a hook
 * is always called there. The map is concurrent only because different entities live on different regions under
 * Folia.
 *
 * <p>Per entity, in memory, cleared on death and quit (CLAUDE.md invariant 3: no persistent player state).
 */
public final class SafeLandings {

    /**
     * The token tells a stale tick task from the current one: a second leap re-arms with a new token. {@code hook}
     * is null for a bare safe landing; {@code fired} says its slam already went off, on the LANDED step.
     */
    private record Mark(Object token, SafeLanding state, LandingHook hook, boolean fired) {

        boolean pending() {
            return hook != null && !fired;
        }
    }

    private final Map<UUID, Mark> marks = new ConcurrentHashMap<>();

    /** Arm (or RE-ARM) a bare mark, and start stepping it. Must run on the entity's own thread. */
    public void arm(LivingEntity entity, Scheduler scheduler) {
        arm(entity, scheduler, null);
    }

    /**
     * Arm (or RE-ARM) a mark carrying {@code hook} (null for none), and start stepping it. Must run on the entity's
     * own thread. A re-arm over a slam that has not gone off ends that slam as {@link Touchdown#SUPERSEDED}.
     */
    public void arm(LivingEntity entity, Scheduler scheduler, LandingHook hook) {
        Object token = new Object();
        Mark previous = marks.put(entity.getUniqueId(), new Mark(token, SafeLanding.armed(), hook, false));
        if (previous != null && previous.pending()) previous.hook().ended(Touchdown.SUPERSEDED);
        scheduler.onEntityLater(entity, () -> step(entity, scheduler, token), 1);
    }

    private void step(LivingEntity entity, Scheduler scheduler, Object token) {
        UUID id = entity.getUniqueId();
        Mark mark = marks.get(id);
        if (mark == null || mark.token() != token) return;   // cleared, or a newer leap's task owns it now
        SafeLanding next = mark.state().step(entity.isOnGround(), entity.getFallDistance());
        Touchdown how = Touchdown.of(mark.state(), next);
        if (next.cleared()) {
            // Claimed BEFORE the hook runs, so a FALL event cannot fire it a second time.
            if (marks.remove(id, mark) && mark.pending()) end(mark.hook(), entity, how);
            return;
        }
        boolean slamNow = mark.pending() && how.slams();      // the LANDED transition; the mark stays, immune
        Mark stepped = new Mark(token, next, mark.hook(), mark.fired() || slamNow);
        if (marks.replace(id, mark, stepped)) {
            if (slamNow) mark.hook().landed(feet(entity), how);
            scheduler.onEntityLater(entity, () -> step(entity, scheduler, token), 1);
        }
    }

    /**
     * The FALL arm: is this fall the protected landing? If so the mark is spent -- one landing each -- and the
     * caller cancels the event. A FALL event IS a ground contact, so an unfired slam goes off here, at the feet.
     */
    public boolean consumeFall(LivingEntity entity) {
        Mark mark = marks.remove(entity.getUniqueId());
        if (mark == null) return false;
        if (mark.pending()) mark.hook().landed(feet(entity), Touchdown.LANDED);
        return mark.state().immune();
    }

    public boolean armed(UUID id) {
        return marks.containsKey(id);
    }

    /** Death or quit mid-leap: the mark goes, and an unfired slam ends as {@link Touchdown#FORGOTTEN} -- no burst. */
    public void forget(UUID id) {
        Mark mark = marks.remove(id);
        if (mark != null && mark.pending()) mark.hook().ended(Touchdown.FORGOTTEN);
    }

    /** A cleared mark's ending: slam at the live feet (absorbed, a failed take-off), or say why not (the backstop). */
    private static void end(LandingHook hook, LivingEntity entity, Touchdown how) {
        if (how.slams()) hook.landed(feet(entity), how);
        else hook.ended(how);
    }

    private static Vec3 feet(LivingEntity entity) {
        Location l = entity.getLocation();
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
