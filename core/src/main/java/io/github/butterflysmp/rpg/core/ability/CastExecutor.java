package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Caster;
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
 * The caster arrives as a snapshot and is immediately projected to a {@link Caster} -- an id plus
 * the stats frozen on the caster's own thread -- which is what every arm carries from there. Nothing
 * here holds a live handle across a tick, nor the full snapshot: a projectile's fuse and a lingering
 * area both outlive the frame that started them, so a position or a liveness flag would be stale by
 * the time they land, while a frozen stat stays true.
 */
public final class CastExecutor {

    private final CombatWorld world;
    private final EffectApplier effects;

    /**
     * Notified each time a BASIC ATTACK is USED. Default no-op, so a caster with nothing to charge
     * -- an ability cast from a command, every existing test -- needs no listener and no change.
     */
    private final Runnable onBasicAttackUse;

    public CastExecutor(CombatWorld world) {
        this(world, () -> {});
    }

    /**
     * With a listener for basic-attack use. WHEN it fires is decided in {@link #execute}, beside
     * the connect/commit logic, so the whole "what costs a use and when" rule reads in one place.
     * WHAT a use costs is the caller's business -- in production a durability charge against the
     * caster's held item, which core neither has nor should.
     *
     * <p><b>THREADING -- do not move either call site.</b> This may only ever be run
     * SYNCHRONOUSLY within {@code execute}, and both calls are. {@code execute} is entered on the
     * thread owning the aim's origin, which for a weapon is the caster's own eye, so anything
     * synchronous there is on the caster's thread. A scheduled continuation is NOT: a projectile
     * impact resolves on the TARGET'S region, a ray's later segments on whatever regions they
     * cross, an area's pulse a second later. Running this from one of those would write the
     * caster's inventory from a foreign thread -- the exact bug the snapshot/handle split exists
     * to prevent.
     */
    public CastExecutor(CombatWorld world, Runnable onBasicAttackUse) {
        this.world = world;
        this.effects = new EffectApplier(world);
        this.onBasicAttackUse = onBasicAttackUse;
    }

    public void execute(AbilityService.CastResult.Success success) {
        AbilityDefinition ability = success.ability();
        CombatantSnapshot caster = success.caster();
        Aim aim = success.aim();

        // Project the cast-time snapshot down to what an effect landing LATER may read: the id,
        // plus the stats frozen on the caster's own thread. Built once, here, because this is the
        // last point that is still unambiguously the caster's frame -- a projectile's impact and
        // an area's pulse both resolve on somebody else's region.
        Caster source = Caster.of(caster);

        // WHAT COSTS A USE, AND WHEN -- the whole rule, here, rather than left to each caller to
        // remember. Only a BASIC ATTACK charges: an ability already spends mana, and charging it
        // as well would bill one press twice. The gate is structural for the same reason
        // Durability's maxDurability <= 0 guard is -- it is the ONLY thing separating the two
        // shipped projectiles from each other. hunters_bow's shot and emberblade's Fireball are
        // both `type: projectile`; nothing about the cast shape tells them apart, so a check left
        // to the wiring is a check a future call site can forget.
        boolean charges = DamagePayload.isBasicAttack(ability.onHit());

        // A melee use is charged on CONNECT (in the Melee arm below); every other shape is charged
        // at COMMIT, here. An arrow costs the bow whether or not it lands, like vanilla; a swing
        // that touches nothing is free.
        if (charges && !(ability.cast() instanceof CastSpec.Melee)) onBasicAttackUse.run();

        switch (ability.cast()) {
            // The caster is their own target: heals, buffs, self-detonations. Their handle
            // is fetched here rather than carried in the Success, which holds a snapshot.
            // The detonation lands at their FEET -- caster.position(), not the aim's
            // origin, which in production is an eye a metre and a half higher.
            case CastSpec.Self ignored ->
                    detonate(ability, source, self(caster), caster.position());

            case CastSpec.Melee melee -> {
                Combatant target = meleeTarget(caster, aim, melee);
                Vec3 impact = target != null ? target.state().position() : aim.pointAt(melee.reach());
                detonate(ability, source, target, impact);
                // ONCE per connecting swing. Outside detonate deliberately: a payload that splashes
                // -- a Burst catching five bodies -- is still ONE use, matching vanilla. Move this
                // inside the effect's per-entity loop and a sweep bills per body.
                if (charges && target != null) onBasicAttackUse.run();
            }

            case CastSpec.Ray ray -> launchRay(ability, source, aim, ray.range());

            case CastSpec.Projectile projectile -> launch(ability, source, aim, projectile);

            case CastSpec.Dash dash -> dash(ability, caster, source, aim, dash);
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
    private void dash(AbilityDefinition ability, CombatantSnapshot caster, Caster source,
                      Aim aim, CastSpec.Dash dash) {
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
        effects.applyToSet(ability.onHit(), source, hits, origin, facing);
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
     * Throw it. The caster is captured as a frozen value and never dereferenced again: a
     * grenade with a 100-tick fuse outlives its thrower's logout, and holding the
     * Combatant would pin a Bukkit entity for five seconds. Same rule as an Area.
     *
     * The flight itself is {@link ProjectileFlight}; impact simply detonates the ability's onHit
     * here, exactly as before the extraction. The Caster rides the flight rather than being closed
     * over, so the freeze is explicit at the boundary where the per-tick region hop happens.
     */
    private void launch(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Projectile spec) {
        ProjectileFlight.launch(world, caster, aim.origin(), aim.direction().scale(spec.speed()),
                spec.gravity(), spec.maxLifetimeTicks(), null, // a bare projectile leaves no trail
                (target, point) -> detonate(ability, caster, target, point));
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
    private void launchRay(AbilityDefinition ability, Caster caster, Aim aim, double range) {
        List<Vec3> endpoints = ChunkTraversal.segmentEndpoints(aim.origin(), aim.direction(), range);
        stepRay(ability, caster, aim.origin(), endpoints, 0);
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
    private void stepRay(AbilityDefinition ability, Caster caster, Vec3 from,
                         List<Vec3> endpoints, int index) {
        Vec3 to = endpoints.get(index);

        Optional<RayHit> hit = world.castRay(from, to, caster.id());
        if (hit.isPresent()) {
            detonate(ability, caster, hit.get().combatant(), hit.get().point());
            return;
        }

        boolean lastSegment = index == endpoints.size() - 1;
        if (lastSegment) {
            // A clean miss still goes off at the end of the aim, as it always has.
            detonate(ability, caster, null, to);
            return;
        }

        world.schedule(to, 1, () -> stepRay(ability, caster, to, endpoints, index + 1));
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

    private void detonate(AbilityDefinition ability, Caster caster, Combatant target, Vec3 impact) {
        effects.applyAll(ability.onHit(), caster, target, impact);
    }
}
