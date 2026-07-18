package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.core.combat.stat.HealthState;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The mob health nameplate: the SECOND display on the {@link HealthChange} seam (the player heart bar
 * is the first). It shows {@code <name> <cur>/<max> ❤} with the CUSTOM cur/max, gated per viewer by
 * line of sight, sent via per-viewer packets. The mob's real server-side name is never touched --
 * name text is a per-viewer metadata override, so death messages and /data stay clean.
 *
 * Three roles:
 *  - as a {@link HealthListener}: on a mob health change, rebuild the cached name text and bump its
 *    version ({@link #onChange}). Pure -- no Bukkit, any thread. Drives WHAT the name says.
 *  - mob lifecycle ({@link #onMobAppear} / {@link #onMobRemove}, from RpgListeners' entity events):
 *    bootstrap custom HP from vanilla max on appearance, cache the nameplate; drop it on removal so no
 *    state leaks past death/despawn.
 *  - per-viewer LOS loop ({@link #onViewerJoin}): each viewer runs its own loop on its own thread --
 *    where getNearbyEntities and hasLineOfSight are legal -- re-asserting visibility each cycle and
 *    resending text only when it changed. Drives WHO can see it.
 */
public final class MobNameplateManager implements HealthListener {

    /** Rescan cadence. Small so nameplates pop in/out with movement; the scale check tunes it. */
    private static final int NAMEPLATE_PERIOD_TICKS = 4;
    /** Range to scan per viewer -- the vanilla name-render cap, beyond which the client draws no name anyway. */
    private static final double VIEW_RADIUS = 64.0;

    private final Scheduler scheduler;
    private final NameplateSender sender;
    private final Keys keys;
    private final Map<UUID, Nameplate> nameplates = new ConcurrentHashMap<>();
    private CombatantStats stats;

    public MobNameplateManager(Scheduler scheduler, NameplateSender sender, Keys keys) {
        this.scheduler = scheduler;
        this.sender = sender;
        this.keys = keys;
    }

    /** Wire the store; called once in onEnable after the store is built (breaks the listener/store cycle). */
    public void bind(CombatantStats stats) {
        this.stats = stats;
    }

    // --- HealthListener: rebuild the cached text on a mob health change -----------------------------

    @Override
    public void onChange(HealthChange change) {
        if (change.targetIsPlayer()) return;          // the heart bar handles players
        Nameplate nameplate = nameplates.get(change.target());
        if (nameplate == null) return;                // not a nameplated mob (unregistered); nothing to update
        nameplate.update(NameplateText.of(nameplate.baseName(), change.newCurrent(), change.max()));
    }

    // --- Mob lifecycle (driven by RpgListeners' entity add/remove events) --------------------------

    /**
     * A mob appeared (spawn or chunk-load) on its own thread. Bootstrap its custom max from vanilla and
     * cache the initial nameplate. Skips players, armor stands (utility/markers), and the reserved
     * opt-out flag.
     */
    public void onMobAppear(LivingEntity mob) {
        if (mob instanceof Player || mob instanceof ArmorStand) return;
        if (mob.getPersistentDataContainer().has(keys.nameplateOptOut, PersistentDataType.BYTE)) return;
        UUID id = mob.getUniqueId();
        HealthState state = stats.bootstrapIfAbsent(id, maxHealthOf(mob), false);
        // Register-if-absent, NOT replace -- mirrors bootstrapIfAbsent (the store half). onMobAppear
        // runs once on spawn, but EVERY /rpg mobdamage cast re-calls it. A replace would build a fresh
        // Nameplate at version 1 each cast; the NEXT TICK's applyDamage bump (1->2) then races the
        // viewer's 4-tick LOS sample, so some casts are missed ("every-other-cast"). The version must
        // climb monotonically for ViewerNameplateState.decide() to resend. Real combat never re-appears
        // a mob, so it was always monotonic there -- only the dev command re-appeared per hit.
        registerIfAbsent(nameplates, id, mob.name(), state.current(), state.max());
    }

    /**
     * The pure map half of {@link #onMobAppear}: create the plate only if absent, and return its
     * resulting version. Bukkit-free and {@code static} so it is unit-testable on a plain map -- no
     * manager instance, scheduler, sender, or {@code LivingEntity} needed. The player / armor-stand /
     * opt-out filtering stays in {@code onMobAppear}, so this is reached only for a mob that should be
     * plated.
     */
    static long registerIfAbsent(Map<UUID, Nameplate> plates, UUID id, Component baseName,
                                 double current, double max) {
        return plates.computeIfAbsent(id, k ->
                new Nameplate(baseName, NameplateText.of(baseName, current, max))).version();
    }

    /** A mob was removed (death, despawn, chunk-unload). Drop its nameplate and health state -- no leak. */
    public void onMobRemove(UUID id) {
        nameplates.remove(id);
        stats.clear(id);
    }

    // --- Per-viewer LOS loop ------------------------------------------------------------------------

    /** Start this viewer's nameplate loop. Self-cancels when the player leaves (EntityTaskTarget inactive). */
    public void onViewerJoin(Player viewer) {
        EntityTaskTarget target = new EntityTaskTarget(viewer, scheduler);
        ViewerNameplateState state = new ViewerNameplateState();
        RepeatingTask.start(target, NAMEPLATE_PERIOD_TICKS, () -> {
            tickViewer(viewer, state);
            return true;                              // runs until the viewer is gone
        }, () -> { });
    }

    /**
     * One cycle for one viewer, on the viewer's thread. Cheapest-first, mirroring the EntityMoveEvent
     * hot path: the 64-block getNearbyEntities bound comes before the per-mob hasLineOfSight raycast,
     * and the name Component is only re-encoded when its version changed.
     */
    private void tickViewer(Player viewer, ViewerNameplateState state) {
        Set<UUID> inRange = new HashSet<>();
        for (Entity entity : viewer.getNearbyEntities(VIEW_RADIUS, VIEW_RADIUS, VIEW_RADIUS)) {
            if (!(entity instanceof LivingEntity mob) || entity instanceof Player || entity instanceof ArmorStand) {
                continue;
            }
            Nameplate nameplate = nameplates.get(entity.getUniqueId());
            if (nameplate == null) continue;          // not registered yet; self-heals after its appear event
            UUID id = entity.getUniqueId();
            inRange.add(id);
            var snapshot = nameplate.snapshot();
            boolean los = viewer.hasLineOfSight(mob);
            var decision = state.decide(id, snapshot.version(), los);
            sender.send(viewer, mob.getEntityId(),
                    decision.includeName() ? Optional.of(snapshot.text()) : Optional.empty(),
                    decision.visible());
        }
        state.retainInRange(inRange);
    }

    private static double maxHealthOf(LivingEntity mob) {
        AttributeInstance attr = mob.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : mob.getHealth();
    }

    /**
     * A mob's cached nameplate: its base name (fixed), and the current text + version held atomically so
     * a viewer loop reads a consistent (text, version) pair even while onChange rebuilds from another
     * thread. Version bumps on each rebuild so viewers know to resend the text.
     */
    static final class Nameplate {   // package-private so the register-if-absent test can read it

        private record Versioned(Component text, long version) {}

        private final Component baseName;
        private final AtomicReference<Versioned> current;

        Nameplate(Component baseName, Component text) {
            this.baseName = baseName;
            this.current = new AtomicReference<>(new Versioned(text, 1));
        }

        Component baseName() {
            return baseName;
        }

        void update(Component text) {
            current.updateAndGet(v -> new Versioned(text, v.version() + 1));
        }

        /** Atomic (text, version) pair -- the viewer loop reads both consistently across a concurrent update. */
        Versioned snapshot() {
            return current.get();
        }

        /** For tests: the current version / text (not the atomic pair the loop needs). */
        long version() {
            return current.get().version();
        }

        Component text() {
            return current.get().text();
        }
    }
}
