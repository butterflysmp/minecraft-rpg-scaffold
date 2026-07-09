package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.element.Element;
import java.util.*;

/** A whole Minecraft world, in forty lines, with no Minecraft. */
public final class FakeWorld implements CombatWorld {
    public final List<Combatant> entities = new ArrayList<>();
    public final List<String> presented = new ArrayList<>();
    private final Deque<Runnable> pending = new ArrayDeque<>();

    @Override public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        double r2 = radius * radius;
        return entities.stream()
                .filter(c -> c.position().distanceSquared(center) <= r2)
                .toList();
    }

    @Override public void schedule(Vec3 near, int delayTicks, Runnable task) { pending.add(task); }

    @Override public void present(Vec3 at, String visualId) { presented.add(visualId); }

    /** Drain the scheduler deterministically. No sleeping, no flakiness. */
    public void runScheduled(int maxIterations) {
        for (int i = 0; i < maxIterations && !pending.isEmpty(); i++) pending.poll().run();
    }

    public static final class Dummy implements Combatant {
        private final UUID id = UUID.randomUUID();
        private final Vec3 pos;
        public double health = 100;
        public Element shield;
        public final List<String> statuses = new ArrayList<>();

        public Dummy(Vec3 pos) { this.pos = pos; }

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
