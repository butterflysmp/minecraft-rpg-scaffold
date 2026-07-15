package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.ChunkTraversal;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.ProjectileFlight;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.core.combat.SweptLine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns an aim into an impact, then applies the ability's effects there.
 *
 * This is the half of a cast that reads the world, so every entry point MUST
 * already be on the thread that owns the region containing the aim's origin.
 * On Paper that means inside Scheduler.onRegion(...). AbilityService.cast()
 * deliberately does none of this.
 *
 * The caster arrives as a snapshot and is thereafter referred to by UUID alone. Nothing
 * here holds a live handle across a tick: a projectile's fuse and a lingering area both
 * outlive the frame that started them.
 */
public final class CastExecutor {

    private final CombatWorld world;
    private final EffectApplier effects;

    public CastExecutor(CombatWorld world) {
        this.world = world;
        this.effects = new EffectApplier(world);
    }

    public void execute(AbilityService.CastResult.Success success) {
        AbilityDefinition ability = success.ability();
        CombatantSnapshot caster = success.caster();
        Aim aim = success.aim();

        switch (ability.cast()) {
            // The caster is their own target: heals, buffs, self-detonations. Their handle
            // is fetched here rather than carried in the Success, which holds a snapshot.
            // The detonation lands at their FEET -- caster.position(), not the aim's
            // origin, which in production is an eye a metre and a half higher.
            case CastSpec.Self ignored ->
                    detonate(ability, caster.id(), self(caster), caster.position());

            case CastSpec.Melee melee -> {
                Combatant target = meleeTarget(caster, aim, melee);
                Vec3 impact = target != null ? target.state().position() : aim.pointAt(melee.reach());
                detonate(ability, caster.id(), target, impact);
            }

            case CastSpec.Ray ray -> launchRay(ability, caster.id(), aim, ray.range());

            case CastSpec.Projectile projectile -> launch(ability, caster.id(), aim, projectile);

            case CastSpec.Dash dash -> dash(ability, caster, aim, dash);
        }
    }

    /**
     * How generous the dash is about "in the way" -- the perpendicular reach of the swept
     * line. Wide enough that clipping past a mob's shoulder still counts, not so wide it sweeps
     * bystanders a lane over. A hit-generosity constant, not a feel number, so it lives here
     * rather than in the yml -- and it is what the swept-line unit test mutates.
     */
    private static final double DASH_HIT_RADIUS = 1.5;

    /**
     * Move the caster, then hit whoever the intended line ran through.
     *
     * The impulse is fetched through the caster's live handle -- the only other arm that
     * touches the caster's own entity is Self, the same way. Direction arrives already resolved
     * (WASD or look) as the aim; core neither knows nor cares which it was. The hit-set is the
     * INTENDED line from the caster's feet, not the ballistic path physics will actually carry
     * them down -- see SweptLine. The payload reuses the same EffectApplier the grenade does:
     * the caster is excluded, players are excluded (mob-only), any visual fires once.
     */
    private void dash(AbilityDefinition ability, CombatantSnapshot caster, Aim aim, CastSpec.Dash dash) {
        // Horizontal drive along the resolved direction, plus a touch of up so the caster
        // leaves the ground and first-tick friction does not eat the horizontal velocity.
        Combatant self = world.combatant(caster.id()).orElse(null);
        if (self != null) {
            Vec3 impulse = aim.direction().scale(dash.speed()).add(new Vec3(0, dash.lift(), 0));
            self.handle().applyImpulse(impulse);
        }

        Vec3 drive = aim.direction();
        Vec3 origin = caster.position();
        double reach = dash.distance();
        Vec3 midpoint = origin.add(drive.scale(reach / 2));
        var candidates = world.combatantsNear(midpoint, reach / 2 + DASH_HIT_RADIUS);

        List<Combatant> hits = SweptLine.enemiesAlong(
                origin, drive, reach, DASH_HIT_RADIUS, candidates, caster.id());

        // Directed untargeted effects (an ember fan) fire toward the caster's FACING. For a
        // reverse-facing dash that is the opposite of the drive: you throw forward, then
        // retreat away from what you threw. Origin is the caster's PRE-dash snapshot feet, so
        // the embers launch from where you stood, not from where the impulse is carrying you.
        Vec3 facing = dash.direction() == CastSpec.DashDirection.REVERSE_FACING ? drive.negate() : drive;
        effects.applyToSet(ability.onHit(), caster.id(), hits, origin, facing);
    }

    /**
     * The caster's own handle, or null if they are already gone -- a Self cast decided on
     * one frame and resolved on another. Targeted effects skip a null target, so a dead
     * man's heal simply does not land.
     */
    private Combatant self(CombatantSnapshot caster) {
        return world.combatant(caster.id()).orElse(null);
    }

    /**
     * Throw it. The caster is captured by UUID and never dereferenced again: a
     * grenade with a 100-tick fuse outlives its thrower's logout, and holding the
     * Combatant would pin a Bukkit entity for five seconds. Same rule as an Area.
     *
     * The flight itself is {@link ProjectileFlight}, shared with the throw_embers effect;
     * impact simply detonates the ability's onHit here, exactly as before the extraction.
     */
    private void launch(AbilityDefinition ability, UUID casterId, Aim aim, CastSpec.Projectile spec) {
        ProjectileFlight.launch(world, casterId, aim.origin(), aim.direction().scale(spec.speed()),
                spec.gravity(), spec.maxLifetimeTicks(), null, // a bare projectile leaves no trail
                (target, point) -> detonate(ability, casterId, target, point));
    }

    /**
     * Walk the aim to its first obstruction, or to its full range if there is none.
     *
     * Chunk column by chunk column, not all at once. A single trace over a 30-block range
     * reads every chunk it crosses, and a chunk belongs to exactly one region -- so one
     * trace could read several regions from a thread that owns only the first. Ending each
     * segment on a chunk plane confines it to one column, and therefore to one region. A
     * fixed segment length would not: it straddles a plane whatever length you pick.
     *
     * The first segment runs inline, on the cast frame, exactly as launch() calls step()
     * inline. So a ray that never leaves its column is still hitscan. Every segment after
     * the first costs a tick, which means A RAY IS NO LONGER HITSCAN in general, and its
     * cost varies with aim -- a diagonal crosses more planes than an axis-aligned shot.
     */
    private void launchRay(AbilityDefinition ability, UUID casterId, Aim aim, double range) {
        List<Vec3> endpoints = ChunkTraversal.segmentEndpoints(aim.origin(), aim.direction(), range);
        stepRay(ability, casterId, aim.origin(), endpoints, 0);
    }

    /**
     * One chunk column of a ray. Mirrors step(): trace, act, or hand the next segment to
     * the region that owns it. The caster is a UUID, never a handle -- a ray now outlives
     * the frame that fired it, so the rule that governs projectiles governs this too.
     *
     * The walk stops at the first body. Nothing here needs to remember who has already been
     * struck; if rays are ever made to PIERCE, that changes, and a set of already-hit ids
     * would have to be threaded through these calls.
     */
    private void stepRay(AbilityDefinition ability, UUID casterId, Vec3 from,
                         List<Vec3> endpoints, int index) {
        Vec3 to = endpoints.get(index);

        Optional<RayHit> hit = world.castRay(from, to, casterId);
        if (hit.isPresent()) {
            detonate(ability, casterId, hit.get().combatant(), hit.get().point());
            return;
        }

        boolean lastSegment = index == endpoints.size() - 1;
        if (lastSegment) {
            // A clean miss still goes off at the end of the aim, as it always has.
            detonate(ability, casterId, null, to);
            return;
        }

        world.schedule(to, 1, () -> stepRay(ability, casterId, to, endpoints, index + 1));
    }

    /**
     * The nearest living thing inside the swing. arcDegrees is the full width of
     * the cone, so a 90-degree swing reaches 45 degrees either side of the aim.
     */
    private Combatant meleeTarget(CombatantSnapshot caster, Aim aim, CastSpec.Melee melee) {
        double minimumDot = Math.cos(Math.toRadians(melee.arcDegrees() / 2.0));
        UUID casterId = caster.id();

        Combatant nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;

        for (Combatant candidate : world.combatantsNear(aim.origin(), melee.reach())) {
            if (candidate.id().equals(casterId)) continue;

            Vec3 toCandidate = candidate.state().position().subtract(aim.origin());
            // Both are unit vectors, so the dot product is the cosine of the
            // angle between them: larger means closer to straight ahead.
            if (toCandidate.normalize().dot(aim.direction()) < minimumDot) continue;

            double distanceSquared = toCandidate.lengthSquared();
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private void detonate(AbilityDefinition ability, UUID casterId, Combatant target, Vec3 impact) {
        effects.applyAll(ability.onHit(), casterId, target, impact);
    }
}
