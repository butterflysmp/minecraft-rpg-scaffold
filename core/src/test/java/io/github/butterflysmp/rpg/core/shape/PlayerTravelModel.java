package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/**
 * A PLAYER ON STONE, AS HIS CLIENT MOVES HIM, against an {@link ImplosionDrag} run as the server runs it, WITH THE
 * SERVER'S VIEW OF HIM LAGGING. Test-only. Every constant is READ from the pinned jars ({@code javap -c -p}; the client
 * {@code .minecraft/versions/26.1.2/26.1.2.jar}, the server {@code run/versions/26.1.2/paper-26.1.2.jar}):
 * <ul>
 *   <li><b>THE ADD (2026-11-18, feat/drag-additive):</b> the drag reaches a player as a {@code ClientboundExplodePacket}
 *       whose {@code playerKnockback} the client hands to {@code Entity#addDeltaMovement}, which is
 *       {@code setDeltaMovement(getDeltaMovement().add(v))} ({@code ClientPacketListener#handleExplosion}; no override
 *       in {@code LocalPlayer}, {@code AbstractClientPlayer}, {@code Player} or {@code LivingEntity}). So it is ADDED to
 *       his momentum, whatever it is, and it is applied BEFORE his next tick ({@code Minecraft#runTick}:
 *       {@code processQueuedPackets} before {@code tick}).</li>
 *   <li><b>THE CUT:</b> at the start of {@code LivingEntity#aiStep}, a PLAYER whose horizontal speed squared is under
 *       {@code 9.0E-6} has it zeroed ({@code ldc2_w 9.0E-6d}, gated on {@code EntityType.PLAYER}).</li>
 *   <li>His tick then ADDS his input ({@code Entity#moveRelative}), MOVES, and multiplies the horizontal by
 *       {@code friction x 0.91f} ({@code LivingEntity#travelInAir}).</li>
 *   <li>The input: a held key is 1, times {@code 0.98f} ({@code LocalPlayer#modifyInput}) and {@code 0.98f} again
 *       ({@code LivingEntity#applyInput}); the speed on the ground is {@code getSpeed() x 0.21600002f / f^3}
 *       ({@code LivingEntity#getFrictionInfluencedSpeed}); a player's speed {@code 0.10000000149011612}
 *       ({@code Player#createAttributes}), sprint {@code +0.30000001192092896} {@code ADD_MULTIPLIED_TOTAL}.</li>
 * </ul>
 * <b>THE LAG, the one thing this model adds to the jar:</b> {@code lag.applyAsInt(n)} ticks. The drag's tick {@code n}
 * reads him where he stood {@code lag} client ticks before the tick its add lands in, so 0 is a server that reads him
 * now and reaches him at once, and each tick of delay anywhere in the loop (his move packet waiting for the server's
 * tick, the network either way) is one more. It may differ tick to tick (jitter, batching).
 *
 * <p><b>What it leaves out, stated:</b> the vertical (a level floor), sprint's start and stop rules, collisions. The
 * geometry: the origin at {@link #ORIGIN}; he stands WEST of it, so "toward" is +x and "away" is -x.
 */
final class PlayerTravelModel {

    static final Vec3 ORIGIN = new Vec3(0, 64, 0);
    static final double EYE = 1.62;
    static final float STONE = 0.6f;
    /** The horizontal multiplier a tick on stone: {@code 0.6f x 0.91f}, a float product. */
    static final double M = Implosion.tickMultiplier(true, STONE);

    private static final float INPUT = 0.98f * 0.98f;

    /** READ, {@code LivingEntity#aiStep}: a player's horizontal speed squared under this is zeroed. */
    static final double CUT = 9.0E-6;

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

    /**
     * What one run read. {@code seen}: his distance as the SERVER saw it at each of the drag's ticks 0..duration (what
     * the DRAG line's {@code end} prints); {@code rest} where he comes to rest; {@code minimum} the nearest he ever
     * truly came; {@code exitTick} the first drag tick whose server view put him outside the reach (-1: never);
     * {@code pushes} the adds sent.
     */
    record Run(List<Double> seen, double rest, double minimum, ImplosionDrag.Summary summary, int pushes,
               int exitTick, ImplosionDrag.Summary second) {
        double end() {
            return seen.get(seen.size() - 1);
        }
    }

    static Run run(Implosion shape, double h0, double away, double u0, int lag) {
        return run(shape, h0, away, u0, n -> lag, null, 0);
    }

    /**
     * Run one drag on one player who starts {@code h0} blocks WEST of the origin, holding a key AWAY from it with
     * acceleration {@code away} (0: standing still), already moving away at {@code u0} BLOCKS PER TICK, the server's
     * view lagging {@code lag(n)} ticks at its tick n; then 60 ticks of slide. Optionally a second drag at
     * {@code second}, started {@code secondAfter} ticks later.
     */
    static Run run(Implosion shape, double h0, double away, double u0, IntUnaryOperator lag, Vec3 second,
                   int secondAfter) {
        UUID id = UUID.nameUUIDFromBytes(("player" + h0).getBytes());
        DragMotion motion = new DragMotion();
        ImplosionDrag drag = new ImplosionDrag(ORIGIN, shape, motion);
        ImplosionDrag other = second == null ? null : new ImplosionDrag(second, shape, motion);
        int total = shape.durationTicks() + 1 + secondAfter + 60;
        double[] hist = new double[total + 1];   // his x before each client tick
        double x = ORIGIN.x() - h0;
        double vx = -u0;
        List<Double> seen = new ArrayList<>();
        double min = Double.MAX_VALUE;
        int pushes = 0;
        int exit = -1;
        long serverTick = 1000;
        for (int n = 0; n < total; n++, serverTick++) {
            hist[n] = x;
            double viewX = hist[Math.max(0, n - Math.max(0, lag.applyAsInt(n)))];
            Vec3 feet = new Vec3(viewX, ORIGIN.y(), ORIGIN.z());
            Implosion.Footing f = new Implosion.Footing(feet, EYE, true, STONE, 0.0);
            List<ImplosionDrag.Push> out = new ArrayList<>();
            if (!drag.finished()) {
                out.addAll(drag.tick(serverTick, List.of(new ImplosionDrag.Sighting(id, f, null)), Set.of()));
                double d = Math.abs(ORIGIN.x() - viewX);
                seen.add(d);
                if (exit < 0 && !shape.inReach(ORIGIN, feet)) exit = seen.size() - 1;
            }
            if (other != null && n >= secondAfter && !other.finished()) {
                out.addAll(other.tick(serverTick, List.of(new ImplosionDrag.Sighting(id, f, null)), Set.of()));
            }
            for (ImplosionDrag.Push p : out) {
                if (p.mode() != ImplosionDrag.Mode.ADD) throw new AssertionError("a player is only ever ADDED to");
                vx += p.velocity().x();   // Entity#addDeltaMovement, before his tick
                pushes++;
            }
            if (vx * vx < CUT) vx = 0;    // LivingEntity#aiStep, a player
            vx += -away;                  // his input
            x += vx;
            vx *= M;
            min = Math.min(min, ORIGIN.x() - x);   // SIGNED: past the origin is negative
        }
        return new Run(seen, ORIGIN.x() - x, min, drag.summaries().get(0), pushes, exit,
                other == null || other.summaries().isEmpty() ? null : other.summaries().get(0));
    }

    private PlayerTravelModel() {}
}
