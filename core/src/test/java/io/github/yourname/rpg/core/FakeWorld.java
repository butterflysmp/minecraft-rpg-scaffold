package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.combat.RayHit;
import io.github.yourname.rpg.core.element.Element;
import java.util.*;

/** A whole Minecraft world, in eighty lines, with no Minecraft. */
public final class FakeWorld implements CombatWorld {
    public final List<Combatant> entities = new ArrayList<>();
    public final List<String> presented = new ArrayList<>();
    private final Deque<Runnable> pending = new ArrayDeque<>();

    /** How close a ray must pass to a combatant to strike it. A hitbox, roughly. */
    public double hitRadius = 0.6;

    /**
     * A wall this far along EVERY segment, measured from that segment's start.
     * Convenient for "there is a block right in front of you". Infinity is open sky.
     */
    public double blockDistance = Double.POSITIVE_INFINITY;

    /**
     * A wall standing at this x coordinate. Unlike blockDistance this is fixed in
     * the world, so a projectile can fly toward it for several ticks and then hit
     * it. Infinity is open sky.
     */
    public double wallX = Double.POSITIVE_INFINITY;

    @Override public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        double r2 = radius * radius;
        return entities.stream()
                .filter(c -> c.position().distanceSquared(center) <= r2)
                .toList();
    }

    /**
     * Nearest combatant whose centre lies within hitRadius of the segment, or the
     * wall at blockDistance, whichever comes first.
     */
    @Override public Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId) {
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length == 0) return Optional.empty();
        Vec3 direction = along.scale(1 / length);

        Combatant nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        for (Combatant candidate : entities) {
            if (candidate.id().equals(ignoreId)) continue;

            Vec3 toCandidate = candidate.position().subtract(from);
            double projected = toCandidate.dot(direction);       // distance along the ray
            if (projected < 0 || projected > length) continue;   // behind us, or past the end

            double perpendicularSquared = toCandidate.lengthSquared() - projected * projected;
            if (perpendicularSquared > hitRadius * hitRadius) continue; // ray passes wide

            if (projected < nearestDistance) {
                nearestDistance = projected;
                nearest = candidate;
            }
        }

        // Distance along this segment at which we meet a wall, if any.
        double wallAt = blockDistance;
        if (Double.isFinite(wallX) && direction.x() != 0) {
            double toPlane = (wallX - from.x()) / direction.x();
            if (toPlane >= 0) wallAt = Math.min(wallAt, toPlane);
        }

        if (wallAt <= Math.min(nearestDistance, length)) {
            return Optional.of(RayHit.ofBlock(from.add(direction.scale(wallAt))));
        }
        if (nearest == null) return Optional.empty();
        return Optional.of(RayHit.ofCombatant(nearest.position(), nearest));
    }

    @Override public void schedule(Vec3 near, int delayTicks, Runnable task) { pending.add(task); }

    @Override public void present(Vec3 at, String visualId) { presented.add(visualId); }

    /** Drain the scheduler deterministically. No sleeping, no flakiness. */
    public void runScheduled(int maxIterations) {
        for (int i = 0; i < maxIterations && !pending.isEmpty(); i++) pending.poll().run();
    }

    public int pendingTasks() { return pending.size(); }

    public static final class Dummy implements Combatant {
        private final UUID id = UUID.randomUUID();
        private Vec3 pos;
        public double health = 100;
        public Element shield;
        public final List<String> statuses = new ArrayList<>();

        public Dummy(Vec3 pos) { this.pos = pos; }

        public void moveTo(Vec3 to) { this.pos = to; }

        @Override public UUID id() { return id; }
        @Override public Vec3 position() { return pos; }
        @Override public boolean isAlive() { return health > 0; }
        @Override public Element shieldElement() { return shield; }
        @Override public void applyDamage(double a, Element e) { health -= a; }
        @Override public void applyHeal(double a) { health += a; }
        @Override public void applyKnockback(Vec3 d, double s) { }
        @Override public void applyStatus(String id, int dur, int amp) { statuses.add(id); }
    }
}
