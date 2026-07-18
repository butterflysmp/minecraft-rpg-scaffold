package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CombatantHandle;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.paper.content.StatusDefinition;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Wraps a Bukkit LivingEntity as the two halves of the core port.
 *
 * The snapshot is read HERE, on the thread that owns the entity, and frozen. The handle
 * only dispatches: every mutator hops onto the entity's own thread and returns nothing.
 * That asymmetry is why the port is two types -- a reader must return a value, and you
 * cannot hop a thread and still return synchronously.
 */
public final class BukkitCombatant {

    private BukkitCombatant() {}

    /** State to read plus a handle to act on. MUST be called on the thread owning {@code entity}. */
    public static Combatant of(LivingEntity entity, AdapterContext ctx) {
        return new Combatant(snapshot(entity), new Handle(entity, ctx));
    }

    /**
     * Freeze everything core needs to know. MUST be called on the thread owning
     * {@code entity} -- enforced, not merely asked for.
     */
    public static CombatantSnapshot snapshot(LivingEntity entity) {
        Regions.requireOwned(entity);
        var location = entity.getLocation();
        return new CombatantSnapshot(
                entity.getUniqueId(),
                new Vec3(location.getX(), location.getY(), location.getZ()),
                !entity.isDead(),
                entity instanceof Player);
    }

    /** Dispatches onto the entity's own thread. Never reads the world, never returns state. */
    private record Handle(LivingEntity entity, AdapterContext ctx) implements CombatantHandle {

        @Override public UUID id() { return entity.getUniqueId(); }

        /**
         * The one custom-damage path -- BOTH basic attacks (weapon swings) and ability payloads
         * (Ember Step, Rekindle) land here (via EffectApplier). It drains CUSTOM HP, the source of
         * truth, and fires the {@code HealthChange} seam that drives the nameplate / hearts / (later)
         * the popup and death. It does NOT deal vanilla damage: vanilla health is a puppet, not truth.
         *
         * <p>Flash is ABILITY-PATH ONLY here, and gated so it never overlaps melee. A weapon swing
         * already fires a vanilla event that flashes the mob (see {@code RpgListeners}'
         * player-melee handler, which tokens it and cancels its knockback); that event sets vanilla
         * i-frames as a side effect, and it runs BEFORE this deferred packet-path call. So when
         * {@code noDamageTicks > 0} a vanilla event just flashed this target -- skip the manual flash,
         * no double. When it is 0 (an ability, which fires no vanilla event) play the hurt animation
         * ourselves. Do NOT reset i-frames here: that reset is exactly what would defeat the gate.
         *
         * <p>The amount arrives already multiplied by the elemental matrix; EffectApplier did that
         * against the snapshot's shield. All this port carries is a number and a culprit.
         */
        @Override public void applyDamage(double amount, UUID sourceId) {
            ctx.scheduler().onEntity(entity, () -> {
                // Drain custom HP + fire the seam. dealerIsPlayer reuses the source's faction bit;
                // the nameplate ignores the dealer this phase, the popup (1b) will need it.
                Entity source = Attribution.attributableSource(
                        sourceId, entity.getWorld()::getEntity, Bukkit::isOwnedByCurrentRegion);
                boolean dealerIsPlayer = source instanceof Player;
                ctx.stats().damage(entity.getUniqueId(), amount, sourceId, dealerIsPlayer);

                // Aggro-on-hit: the target turns on its attacker -- vanilla's expected default.
                // Ability damage flashes without a vanilla hit, so it would otherwise provoke
                // nothing (mobs wander off while you whittle them); a mob caught in Rekindle's burst
                // must turn on the caster, who has already dashed away -- correct for a kite tool.
                // Melee already aggros via its tokened vanilla event, so re-targeting the same player
                // here is harmless. Unresolvable/cross-region sources (null) simply don't aggro.
                if (entity instanceof Mob mob && source instanceof LivingEntity attacker) {
                    mob.setTarget(attacker);
                }

                // Manual red hurt flash for the ability path (no vanilla event fired). The i-frame
                // gate keeps melee (flashed by its own vanilla event) from flashing twice.
                if (entity.getNoDamageTicks() == 0) {
                    entity.playHurtAnimation(0f);
                }
            });
        }

        @Override public void applyHeal(double amount) {
            ctx.scheduler().onEntity(entity, () -> {
                var attr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double max = attr == null ? 20.0 : attr.getValue();
                entity.setHealth(Math.min(max, entity.getHealth() + amount));
            });
        }

        @Override public void applyKnockback(Vec3 direction, double strength) {
            ctx.scheduler().onEntity(entity, () -> {
                Vector v = new Vector(direction.x(), direction.y(), direction.z());
                if (v.lengthSquared() > 0) v.normalize().multiply(strength);
                entity.setVelocity(entity.getVelocity().add(v));
            });
        }

        /**
         * The dash impulse. REPLACES velocity rather than adding to it (unlike knockback), so a
         * dash covers its intended distance no matter what momentum the caster was already
         * carrying -- direction*speed is a whole velocity, computed core-side. Vanilla physics
         * takes it from here: walls stop them, ledges drop them.
         */
        @Override public void applyImpulse(Vec3 velocity) {
            ctx.scheduler().onEntity(entity, () ->
                    entity.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z())));
        }

        /**
         * duration and amplifier come from the ability; the status definition only says
         * what kind of thing a "scorch" is. An unknown id is a content mistake, not a
         * programming error: warn once and let the rest of the detonation land.
         */
        @Override public void applyStatus(String statusId, int durationTicks, int amplifier) {
            StatusDefinition def = ctx.statuses().find(statusId).orElse(null);
            if (def == null) {
                ctx.warnOnce("Unknown status_id '" + statusId + "'; no status applied");
                return;
            }
            ctx.scheduler().onEntity(entity, () -> {
                switch (def) {
                    // Extend a burn, never shorten it. A 2s scorch pulse must not stomp
                    // the 10s burn someone already took from flint and steel.
                    case StatusDefinition.Fire ignored ->
                            entity.setFireTicks(Math.max(entity.getFireTicks(), durationTicks));

                    case StatusDefinition.Potion potion -> {
                        PotionEffectType type = potionEffect(potion.potionType());
                        if (type == null) {
                            ctx.warnOnce("Unknown potion_type '" + potion.potionType()
                                    + "' for status '" + statusId + "'; no status applied");
                            return;
                        }
                        // PotionEffect's amplifier is 0-based: 0 is level I.
                        entity.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
                    }

                    // Mob-only for now. A player would fight the client's own movement
                    // prediction and get a broken half-immobilize; skip rather than half-work.
                    // MOVEMENT_SPEED=0 kills the AI drive; velocity-zero kills knockback/jumps.
                    // Rooted and Freeze are the two configurations: Freeze uses its own instance
                    // and key (so all three movement modifiers coexist) and adds attack
                    // suppression -- the RpgListeners cancel a frozen mob's attacks while
                    // ctx.freeze().isImmobilized(id) holds.
                    case StatusDefinition.Immobilize immobilize -> {
                        if (entity instanceof Player) return;
                        RepeatingTaskTarget target = new EntityTaskTarget(entity, ctx.scheduler());
                        // Captured ONCE here, on the entity thread. A refresh (re-apply) reuses the
                        // running task and its original anchor -- the mob is already pinned there, so
                        // it can't have walked between casts. holdInPlace pins position while the AI
                        // keeps running (so a rooted archer still shoots and tracks you).
                        Location anchor = entity.getLocation().clone();
                        if (immobilize.suppressAttacks()) {
                            SpeedAttribute speed = new EntitySpeedAttribute(entity, ctx.keys().freeze);
                            ctx.freeze().apply(entity.getUniqueId(), target, speed, durationTicks, () -> {
                                holdInPlace(entity, anchor, ctx.anchorDrift());
                                // A frozen creeper does not explode: pause its swell each tick so it
                                // never sits primed and detonates the instant it unfreezes. The
                                // ExplosionPrimeEvent cancel in RpgListeners is the guaranteed backstop.
                                if (entity instanceof Creeper creeper) {
                                    creeper.setIgnited(false);
                                    creeper.setFuseTicks(creeper.getMaxFuseTicks());
                                }
                            });
                        } else {
                            SpeedAttribute speed = new EntitySpeedAttribute(entity, ctx.keys().rooted);
                            ctx.immobilize().apply(entity.getUniqueId(), target, speed, durationTicks,
                                    () -> holdInPlace(entity, anchor, ctx.anchorDrift()));
                        }
                    }

                    // Stacking slow. Mob-only for the same reason as Immobilize. The stack +
                    // cleanup logic lives in SoakedStatus; here we only bind the two seams.
                    case StatusDefinition.Soaked ignored -> {
                        if (entity instanceof Player) return;
                        RepeatingTaskTarget target = new EntityTaskTarget(entity, ctx.scheduler());
                        SpeedAttribute speed = new EntitySpeedAttribute(entity, ctx.keys().soaked);
                        ctx.soaked().apply(entity.getUniqueId(), target, speed, durationTicks);
                    }
                }
            });
        }

        /**
         * The key's syntax was validated at load, so this cannot throw. A null means the
         * key is well-formed but names no effect -- which ContentValidator already warned
         * about at startup. MOB_EFFECT, not the obsolete EFFECT alias.
         */
        private static PotionEffectType potionEffect(NamespacedKey key) {
            return Registry.MOB_EFFECT.get(key);
        }

        /**
         * Pin an immobilized mob to its anchor each tick: lock X/Z (walk, strafe), cap Y (slime
         * hop, climb; falling still allowed), then zero velocity (knockback). The teleport keeps
         * the mob's CURRENT yaw/pitch -- live facing, not the anchor's -- so a rooted archer keeps
         * tracking and can still shoot you. The pure decision is ImmobilizePhysics.correction; this
         * is its Bukkit binding, run on the entity's own thread from the immobilize's per-tick.
         * (Fliers are not specially handled -- an accepted compromise; see DESIGN-status-effects.md.)
         *
         * <p>These are the per-tick belts behind immobilization. DO NOT delete them as "redundant"
         * with the EntityMoveEvent veto (RpgListeners.onImmobilizedMove) -- that veto is the PRIMARY
         * stop, but it fires for every entity (a cost) and could miss an edge case; these are the
         * defense that keeps a mob still if the veto ever doesn't fire, and each stops a DIFFERENT
         * motion source. Delete them and the strafe creep comes back. The layers, and why each exists:
         *
         * <ul>
         *   <li>EntityMoveEvent veto (elsewhere): vetoes AI-committed translation before it commits.
         *   <li>MOVEMENT_SPEED=0 (the speed modifier, in ImmobilizeStatus): kills the AI's movement DRIVE.
         *   <li>stopPathfinding (here): cancels the navigation approach path so there's less to veto.
         *   <li>velocity-zero (here): stops momentum/knockback velocity ACCUMULATING behind the veto,
         *       so a mob doesn't lurch when it unfreezes or if one move slips.
         *   <li>the position anchor (here): the SAFETY NET -- if a move ever slips the event, snap
         *       back. With the veto working it never triggers (the mob never drifts), so it stays
         *       dormant.
         * </ul>
         */
        private static void holdInPlace(LivingEntity entity, Location anchor, double drift) {
            if (entity instanceof Mob mob) mob.getPathfinder().stopPathfinding();
            Location cur = entity.getLocation();
            double[] fix = ImmobilizePhysics.correction(cur.getX(), cur.getY(), cur.getZ(),
                    anchor.getX(), anchor.getY(), anchor.getZ(), drift * drift);
            if (fix != null) {
                entity.teleport(new Location(cur.getWorld(), fix[0], fix[1], fix[2],
                        cur.getYaw(), cur.getPitch()));
            }
            entity.setVelocity(new Vector(0, 0, 0));
        }
    }
}
