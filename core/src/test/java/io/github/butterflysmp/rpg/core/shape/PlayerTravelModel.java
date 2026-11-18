package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A PLAYER ON STONE, AS HIS CLIENT MOVES HIM, against an {@link ImplosionDrag} run as the server runs it. Test-only.
 * Every constant is READ from the pinned jars (2026-11-18, {@code javap -c -p}; the client
 * {@code .minecraft/versions/26.1.2/26.1.2.jar}, the server {@code run/versions/26.1.2/paper-26.1.2.jar}):
 * <ul>
 *   <li>A motion packet REPLACES his velocity ({@code ClientPacketListener#handleSetEntityMotion} ->
 *       {@code Entity#lerpMotion} -> {@code setDeltaMovement}), and it is applied BEFORE his next tick
 *       ({@code Minecraft#runTick}: {@code processQueuedPackets} before {@code tick}).</li>
 *   <li>His tick ADDS his input ({@code Entity#moveRelative}: the input vector, not normalised under length 1, times
 *       the speed, turned to his facing), MOVES, then multiplies the horizontal by {@code friction x 0.91f}
 *       ({@code LivingEntity#travelInAir}).</li>
 *   <li>The input: a held key is 1, times {@code 0.98f} in {@code LocalPlayer#modifyInput} and {@code 0.98f} again in
 *       {@code LivingEntity#applyInput}.</li>
 *   <li>The speed on the ground: {@code getSpeed() x 0.21600002f / (f x f x f)}, f the block's friction
 *       ({@code LivingEntity#getFrictionInfluencedSpeed}); a player's movement speed is {@code 0.10000000149011612}
 *       ({@code Player#createAttributes}), and sprinting adds {@code 0.30000001192092896} as
 *       {@code ADD_MULTIPLIED_TOTAL} ({@code LivingEntity.SPEED_MODIFIER_SPRINTING}).</li>
 * </ul>
 * <b>What it leaves out, stated:</b> the network (each packet lands the tick after it is sent), the vertical (a level
 * floor: he stays on it, so a drag, which has no lift, never moves him up or down), sprint's start and stop rules.
 *
 * <p>The geometry: the origin at {@link #ORIGIN}; he stands WEST of it, so "toward" is +x and "away" is -x.
 */
final class PlayerTravelModel {

    static final Vec3 ORIGIN = new Vec3(0, 64, 0);
    static final double EYE = 1.62;
    static final float STONE = 0.6f;
    /** The horizontal multiplier a tick on stone: {@code 0.6f x 0.91f}, a float product. */
    static final double M = Implosion.tickMultiplier(true, STONE);

    private static final float INPUT = 0.98f * 0.98f;

    /** BLOCKS PER TICK added by a held key on stone at movement speed {@code speed}. */
    static double accel(float speed) {
        return (double) (speed * (0.21600002f / (STONE * STONE * STONE))) * INPUT;
    }

    static final double WALK = accel((float) 0.10000000149011612);
    static final double SPRINT = accel((float) (0.10000000149011612 * (1 + 0.30000001192092896)));

    /** His terminal speed, BLOCKS PER TICK moved, holding a key with acceleration {@code a}: {@code a / (1 - M)}. */
    static double terminal(double a) {
        return a / (1 - M);
    }

    /** What one run read: the distance at each server tick 0..duration, his rest distance, the drag's summary. */
    record Run(List<Double> seen, double rest, double minimum, ImplosionDrag.Summary summary, int pushes) {
        double end() {
            return seen.get(seen.size() - 1);
        }
    }

    /**
     * Run one drag on one player who starts {@code h0} blocks WEST of the origin, holding a key AWAY from it with
     * acceleration {@code away} (0: standing still), already moving away at {@code u0} BLOCKS PER TICK, then 60 ticks
     * of slide after the drag. Optionally a second drag at {@code second}, started {@code secondAfter} ticks later.
     */
    static Run run(Implosion shape, double h0, double away, double u0) {
        return run(shape, h0, away, u0, null, 0);
    }

    static Run run(Implosion shape, double h0, double away, double u0, Vec3 second, int secondAfter) {
        UUID id = UUID.nameUUIDFromBytes(("player" + h0).getBytes());
        DragMotion motion = new DragMotion();
        ImplosionDrag drag = new ImplosionDrag(ORIGIN, shape, motion);
        ImplosionDrag other = second == null ? null : new ImplosionDrag(second, shape, motion);
        double x = ORIGIN.x() - h0;
        double vx = -u0;            // his velocity, carried into the next tick
        List<Double> seen = new ArrayList<>();
        double min = Double.MAX_VALUE;
        int pushes = 0;
        long serverTick = 1000;
        for (int t = 0; t < shape.durationTicks() + 1 + secondAfter + 60; t++, serverTick++) {
            Vec3 feet = new Vec3(x, ORIGIN.y(), ORIGIN.z());
            Implosion.Footing f = new Implosion.Footing(feet, EYE, true, STONE, 0.0);
            List<ImplosionDrag.Push> out = new ArrayList<>();
            if (!drag.finished()) {
                out.addAll(drag.tick(serverTick, List.of(new ImplosionDrag.Sighting(id, f, null)), Set.of()));
                seen.add(Math.abs(ORIGIN.x() - x));
            }
            if (other != null && t >= secondAfter && !other.finished()) {
                out.addAll(other.tick(serverTick, List.of(new ImplosionDrag.Sighting(id, f, null)), Set.of()));
            }
            if (!out.isEmpty()) {
                vx = out.get(out.size() - 1).velocity().x();   // the tick's last send carries every add
                pushes++;
            }
            // His client's tick: the input, the move, the friction.
            vx += -away;
            x += vx;
            vx *= M;
            min = Math.min(min, Math.abs(ORIGIN.x() - x));
        }
        return new Run(seen, Math.abs(ORIGIN.x() - x), min, drag.summaries().get(0), pushes);
    }

    private PlayerTravelModel() {}
}
