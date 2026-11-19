package io.github.butterflysmp.rpg.core.fight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * THE REVIVE'S PROGRESS (step F9e c; Ben, PLAN-dragon-fight.md section 1, 0d: "Another player has to hold
 * shift/crouch on top of that players copy for 3 seconds to respawn them").
 *
 * <p>Once a tick, for each copy, the Paper side offers every LIVING PARTICIPANT with his distance to the copy and
 * whether he is sneaking. A reviver progresses while he sneaks within the radius. Each reviver has HIS OWN count: two
 * revivers do not stack, and the first to finish revives. A BREAK resets his count to zero:
 * <ul>
 *   <li>he stops sneaking;</li>
 *   <li>he is beyond the radius (he left it, or the copy moved out of it);</li>
 *   <li>he is not offered at all (he went down, or left).</li>
 * </ul>
 * Taking damage is NOT a break (Ben: "No"): a hit is not an input here at all. Pure; not thread-safe.
 */
public final class ReviveTracker {

    /** One living participant's state against one copy on this tick. */
    public record Attempt(UUID reviver, double distanceBlocks, boolean sneaking) {}

    public enum Outcome {
        /** Nobody is reviving. */
        NONE,
        /** Someone is reviving and nobody has finished. */
        PROGRESS,
        /** {@link Result#reviver()} finished on this tick. */
        DONE
    }

    /**
     * @param outcome  what this tick came to
     * @param reviver  who finished (DONE), else null
     * @param progress every reviver's count after this tick, in the order offered
     * @param broken   who broke on this tick (their count reset), in the order they had started
     */
    public record Result(Outcome outcome, UUID reviver, Map<UUID, Integer> progress, List<UUID> broken) {
        /** His count after this tick; 0 when he has none. */
        public int progressTicks(UUID id) {
            return progress.getOrDefault(id, 0);
        }
    }

    private final int needTicks;
    private final double radiusBlocks;
    private final Map<UUID, LinkedHashMap<UUID, Integer>> byCopy = new HashMap<>();

    /**
     * @param needTicks    ticks of unbroken sneaking that revive ({@code revive_seconds} x 20); 1 or more
     * @param radiusBlocks how near the copy a reviver must be ({@code revive_radius_blocks}); more than 0
     */
    public ReviveTracker(int needTicks, double radiusBlocks) {
        if (needTicks < 1) throw new IllegalArgumentException("a revive needs at least 1 tick: " + needTicks);
        if (!(radiusBlocks > 0.0)) throw new IllegalArgumentException("the revive radius must be above 0: " + radiusBlocks);
        this.needTicks = needTicks;
        this.radiusBlocks = radiusBlocks;
    }

    public int needTicks() { return needTicks; }

    /** One tick for one copy. */
    public Result tick(UUID copy, List<Attempt> attempts) {
        LinkedHashMap<UUID, Integer> was = byCopy.getOrDefault(copy, new LinkedHashMap<>());
        LinkedHashMap<UUID, Integer> now = new LinkedHashMap<>();
        UUID finished = null;
        for (Attempt a : attempts) {
            if (!a.sneaking() || a.distanceBlocks() > radiusBlocks) continue;
            int n = was.getOrDefault(a.reviver(), 0) + 1;
            now.put(a.reviver(), n);
        }
        List<UUID> broken = new ArrayList<>();
        for (UUID id : was.keySet()) if (!now.containsKey(id)) broken.add(id);
        // The first to finish: the longest count reaches the need first; on one tick, the first offered.
        for (Attempt a : attempts) {
            Integer n = now.get(a.reviver());
            if (n != null && n >= needTicks) {
                finished = a.reviver();
                break;
            }
        }
        if (now.isEmpty()) byCopy.remove(copy);
        else byCopy.put(copy, now);
        Outcome outcome = finished != null ? Outcome.DONE : now.isEmpty() ? Outcome.NONE : Outcome.PROGRESS;
        if (finished != null) byCopy.remove(copy);
        return new Result(outcome, finished, Map.copyOf(now), List.copyOf(broken));
    }

    /** The copy is gone (revived, or the fight ended): its progress is dropped. */
    public void forget(UUID copy) {
        byCopy.remove(copy);
    }
}
