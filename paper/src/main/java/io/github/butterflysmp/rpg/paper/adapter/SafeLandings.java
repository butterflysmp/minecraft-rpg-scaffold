package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.build.SafeLanding;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is carrying a leap's fall-damage mark (ruling 31; PLAN-build-system.md section 7.6). The rule is core's
 * {@link SafeLanding}; this holds one mark per entity and steps it once a tick with what the server reports.
 *
 * <p><b>THREADING.</b> A mark is armed, stepped and read on the entity's own thread: {@link #arm} from the dash's
 * {@code onEntity} task, the step by {@code Scheduler.onEntityLater}, and {@link #consumeFall} from the FALL
 * event, which runs on the thread that owns the entity. The map is concurrent only because different entities
 * live on different regions under Folia.
 *
 * <p>Per entity, in memory, cleared on death and quit (CLAUDE.md invariant 3: no persistent player state).
 */
public final class SafeLandings {

    /** The token tells a stale tick task from the current one: a second leap re-arms with a new token. */
    private record Mark(Object token, SafeLanding state) {}

    private final Map<UUID, Mark> marks = new ConcurrentHashMap<>();

    /** Arm (or RE-ARM) the mark, and start stepping it. Must run on the entity's own thread. */
    public void arm(LivingEntity entity, Scheduler scheduler) {
        Object token = new Object();
        marks.put(entity.getUniqueId(), new Mark(token, SafeLanding.armed()));
        scheduler.onEntityLater(entity, () -> step(entity, scheduler, token), 1);
    }

    private void step(LivingEntity entity, Scheduler scheduler, Object token) {
        UUID id = entity.getUniqueId();
        Mark mark = marks.get(id);
        if (mark == null || mark.token() != token) return;   // cleared, or a newer leap's task owns it now
        SafeLanding next = mark.state().step(entity.isOnGround(), entity.getFallDistance());
        if (next.cleared()) {
            marks.remove(id, mark);
            return;
        }
        if (marks.replace(id, mark, new Mark(token, next))) {
            scheduler.onEntityLater(entity, () -> step(entity, scheduler, token), 1);
        }
    }

    /**
     * The FALL arm: is this fall the protected landing? If so the mark is spent -- one landing each -- and the
     * caller cancels the event.
     */
    public boolean consumeFall(UUID id) {
        Mark mark = marks.remove(id);
        return mark != null && mark.state().immune();
    }

    public boolean armed(UUID id) {
        return marks.containsKey(id);
    }

    public void forget(UUID id) {
        marks.remove(id);
    }
}
