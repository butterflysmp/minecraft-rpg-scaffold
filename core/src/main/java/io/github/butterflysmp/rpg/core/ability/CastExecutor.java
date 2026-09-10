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
import io.github.butterflysmp.rpg.core.ability.effect.DirectDamage;

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
        this(world, onBasicAttackUse, (amount, element) -> {});
    }

    /**
     * With a listener for DIRECT DAMAGE DEALT as well, which only the vanilla-driven basic melee
     * hit passes. The sweep rider needs the number the primary target was hit for, and
     * {@link EffectApplier} is the one place that number is built; this carries it out rather than
     * letting a second site re-derive it from the caster and drift.
     *
     * <p>The same THREADING rule as {@code onBasicAttackUse} above applies, for the same reason and
     * with the same force -- see {@link EffectApplier} for where it is reported from.
     */
    public CastExecutor(CombatWorld world, Runnable onBasicAttackUse, DirectDamage onDirectDamage) {
        this.world = world;
        this.effects = new EffectApplier(world, onDirectDamage);
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
        // The payload's headline damage rides the Caster so a status arm deep inside a burst can read
        // it as its CAP. Frozen here with everything else: a projectile is capped by what it was
        // fired with, not by what the caster holds when it lands.
        Caster source = Caster.of(caster)
                .withPayloadDamage(DamagePayload.headlineDamage(ability.onHit(), caster.attackDamage()));

        // WHAT COSTS A USE, AND WHEN -- the whole rule, here, rather than left to each caller to
        // remember. Only a BASIC ATTACK charges: an ability already spends mana, and charging it
        // as well would bill one press twice. The gate is structural for the same reason
        // Durability's maxDurability <= 0 guard is -- it is the ONLY thing separating the two
        // shipped projectiles from each other. hunters_bow's shot and emberblade's Fireball are
        // both `type: projectile`; nothing about the cast shape tells them apart, so a check left
        // to the wiring is a check a future call site can forget.
        boolean charges = DamagePayload.isBasicAttack(ability.onHit());

        // WHAT YOU HEAR WHEN YOU PRESS THE BUTTON. Fired here, before the switch, so it is
        // independent of cast shape and lands on the frame the cast was committed -- a projectile
        // must not wait for its impact to make a noise, which is the whole reason this hook exists.
        //
        // Only reached on a Success, so a cast refused by cooldown or mana is silent.
        //
        // It fires at aim.origin(). For a weapon that is the caster's EYE, and it is deliberately
        // NOT caster.position() -- note the Self arm below detonates at the FEET on purpose. The
        // two are ~1.5 blocks apart: irrelevant for a sound, visible for a particle. The origin is
        // right here because this is the muzzle, not the caster.
        //
        // THREADING: synchronous, on the thread execute() was entered on, which is the caster's
        // own region -- the same standing the onBasicAttackUse listener documents above. Nothing
        // here is scheduled, so there is no region hop to get wrong.
        effects.applyAll(ability.onCast(), source, null, aim.origin());

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

            case CastSpec.Ray ray -> launchRay(ability, source, aim, ray.range(), ray.beam());

            case CastSpec.Projectile projectile -> launch(ability, source, aim, projectile);

            case CastSpec.Dash dash -> dash(ability, caster, source, aim, dash);

            // NOTE WHAT IS NOT PASSED: `source`, nor `aim`. A volley re-projects its caster before
            // EVERY shot, so the cast-frame projection above reaches no shot at all -- see volley().
            case CastSpec.Volley volley -> beginVolley(ability, caster.id(), volley);
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
     * Land a basic melee hit on a target VANILLA already chose.
     *
     * <p>The sibling of the {@link CastSpec.Melee} arm in {@link #execute}, minus {@code meleeTarget}:
     * the player's own crosshair attack resolved the victim, so there is no cone to search, no arc to
     * test and no line of sight to trace -- vanilla did all three, and did them the way the player
     * expects. {@code meleeTarget} stays exactly as it is for ABILITIES like {@code void_slash},
     * where the arc is the feature rather than the bug.
     *
     * <p>Deliberately NOT taking a {@code CastResult.Success}: this path never went through
     * {@code AbilityService}'s check-spend-commit gate -- there is no cooldown to trip and no mana to
     * spend for a free swing -- and manufacturing a Success would claim it had.
     *
     * <p>The use-charge stays HERE, and outside {@link #detonate}, for the reason the Melee arm
     * documents: a payload that splashes is still ONE use. Keeping it in this class rather than in
     * the paper wiring is what keeps "what costs a use, and when" in a single place; a check left to
     * the caller is a check the next caller forgets.
     *
     * <p>{@code target} is never null here. Vanilla does not raise a damage event without a victim,
     * which is exactly why this arm has no miss case while the cone arm does.
     *
     * <p>MUST be called on the thread owning the victim's region, like every other entry point.
     */
    public void landBasicMelee(AbilityDefinition ability, CombatantSnapshot caster,
                               Combatant target, double chargeScale) {
        detonate(ability, Caster.of(caster, chargeScale)
                .withPayloadDamage(DamagePayload.headlineDamage(ability.onHit(), caster.attackDamage())),
                target, target.state().position());
        if (DamagePayload.isBasicAttack(ability.onHit())) onBasicAttackUse.run();
    }

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
                spec.gravity(), spec.maxLifetimeTicks(),
                new ProjectileFlight.Look(spec.trail(), spec.item()),
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
    /**
     * How far from the ray's ORIGIN the beam starts being drawn. The first blocks are skipped.
     *
     * <p><b>PROVISIONAL.</b> {@code GATE-beam-gap.md} is the authority that rules it -- specifically
     * G5a (the lapis muzzle, before/after, on both Particles settings) and G5b (CE4's re-run). Until
     * that gate runs this number is nobody's: changing it is housekeeping, not a decision. When the
     * gate rules it, this paragraph is replaced by an ADOPTED line, on the {@code Ignite} precedent.
     *
     * <p><b>WHY IT EXISTS.</b> {@code GATE-cursed-emerald.md} CE4 measured that the Cursed Emerald's
     * beam is hard to see through at BOTH 3 and 30 blocks. Ten times the particle count changing
     * nothing is evidence against density and for a fixed NEAR-FIELD cause: the beam starts at the
     * eye and runs down the view axis, so the nearest few metres dominate screen coverage and are
     * identical at every range. This fixes that cause for EVERY ray weapon rather than one weapon's
     * multiplier -- the Lapis Staff draws from the eye too, which is why the fix could not live in a
     * content file.
     *
     * <p><b>THERE IS A CEILING ON THIS NUMBER AND NOTHING ENFORCES IT, SO IT IS WRITTEN HERE.</b>
     * A beam shorter than the gap draws NOTHING AT ALL. The tightest gate stagings are CE4's near
     * shot at <b>3 blocks</b> and {@code GATE-lapis-staff.md} L0's at <b>~3</b>; L6's is 5. At a gap
     * of 3 those rows fire at a wall, see no beam whatsoever, and report "not blinding" -- <b>true,
     * and measuring nothing.</b> The row would pass by drawing zero particles, which is a pass
     * indistinguishable from never having run, in the rows whose whole job is verifying this
     * constant. At 1.0 CE4's near staging still has two blocks of beam. <b>Keep it well under 3.</b>
     */
    private static final double BEAM_ORIGIN_GAP = 1.0;

    private void launchRay(AbilityDefinition ability, Caster caster, Aim aim, double range,
                           String beam) {
        List<Vec3> endpoints = ChunkTraversal.segmentEndpoints(aim.origin(), aim.direction(), range);
        // THE GAP BOUNDARY IS COMPUTED ONCE, HERE, AND THREADED UNCHANGED. This is the whole of the
        // origin-relative property: `aim` dies at the end of this method -- neither origin nor
        // direction is available to stepRay -- so a gap derived per segment would be derived from a
        // chunk plane instead of from the muzzle. Two implementations that do exactly that, and why
        // they are wrong, are recorded at stepRay's call to presentAlong.
        stepRay(ability, caster, beam, aim.origin(), aim.pointAt(BEAM_ORIGIN_GAP), endpoints, 0);
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
    private void stepRay(AbilityDefinition ability, Caster caster, String beam, Vec3 from,
                         Vec3 beamStart, List<Vec3> endpoints, int index) {
        Vec3 to = endpoints.get(index);

        Optional<RayHit> hit = world.castRay(from, to, caster.id());

        // THE BEAM IS DRAWN AS THE RAY WALKS -- one segment per tick, not hitscan and not deferred
        // to the impact point. The operator's reasoning, recorded here because someone will
        // eventually read the delay as a bug and try to "fix" it:
        //
        //   "Particles only render about 30 blocks out, so particles visible to the caster will be
        //    drawn within about a tick -- imperceptible to the caster. For a non-caster observing
        //    the beam under optimal conditions the beam would take about 5 ticks to render, next to
        //    unobservable. The slight delay doesn't matter, especially in combat."
        //
        // MEASURED 2026-09-10 (GATE-volley.md, V3): THE ESTIMATE ABOVE IS RIGHT, AND THE NUMBER IS
        // 32 BLOCKS. It is a CLIENT cap, not a setting anything here tunes, so it does not move with
        // server config. The reasoning stands unchanged -- but note what it now implies, which the
        // estimate was never precise enough to say: A RAY WHOSE RANGE EXCEEDS 32 DRAWS ONLY ITS
        // NEAR HALF. It still hits at full range; the beam simply stops. That is invisible from this
        // file and belongs to whoever authors a `range:` -- see GATE-volley.md V3 for the
        // consequence, which is content's, not this method's.
        //
        // MAKING THE RAY HITSCAN TO REMOVE THAT DELAY WOULD REINTRODUCE THE FOLIA REGION PROBLEM
        // THE CHUNK-COLUMN WALK EXISTS TO PREVENT -- see launchRay above and CombatWorld.castRay.
        // The walk is not in the way of the beam; it is what makes a one-hop beam segment legal at
        // all, because a segment bounded by chunk planes lies inside one region by construction.
        //
        // DRAW TO WHERE THE RAY ACTUALLY REACHED, NOT TO THE SEGMENT'S FAR END. castRay traces
        // [from, to] and a hit lands at hit.point() SOMEWHERE INSIDE that, so a beam drawn from-to
        // would carry on THROUGH the wall or the body that just stopped it. Drawn BEFORE the
        // detonation so the line reads as arriving at the burst rather than trailing out of it.
        // THE FIRST BEAM_ORIGIN_GAP BLOCKS ARE NOT DRAWN, AND THE GAP IS MEASURED FROM THE RAY'S
        // ORIGIN RATHER THAN FROM THIS SEGMENT'S START. `beamStart` was computed once in launchRay
        // and has been threaded here unchanged, which is the entire mechanism.
        //
        // TWO IMPLEMENTATIONS THAT LOOK EQUIVALENT AND ARE NOT. Both were considered and refused:
        //
        //   * Applying the gap inside presentAlong or BeamSamples.along. Those see only a segment's
        //     own `from`/`to`, so the skip is re-applied per segment: a 30-block beam crossing two
        //     chunk planes draws THREE holes instead of one.
        //   * Applying it only when index == 0. The gap then silently becomes
        //     min(GAP, distance to the first chunk plane). A caster at x = 32.4 facing -x has a
        //     0.4-BLOCK first segment -- on an ordinary full-range shot, not a point-blank one -- so
        //     a 1.0 gap would collapse to 0.4 by standing position.
        //
        // Both fail BY WHERE THE CASTER HAPPENS TO STAND, and a boot gate is one observation from
        // one spot -- a control that succeeds for the wrong reason. Comparing against a fixed world
        // point cannot fail that way: no segment boundary enters the comparison.
        Vec3 beamEnd = hit.map(RayHit::point).orElse(to);
        Vec3 alongSegment = to.subtract(from);
        boolean gapEndsAhead = beamStart.subtract(from).dot(alongSegment) > 0;
        Vec3 drawFrom = gapEndsAhead ? beamStart : from;

        // A SEGMENT LYING ENTIRELY INSIDE THE GAP STILL CALLS presentAlong, WITH A ZERO-LENGTH SPAN.
        // BeamSamples.along returns List.of() for it, so nothing is drawn either way -- the call is
        // load-bearing for a TEST and for nothing else in production, which is exactly the shape of
        // a line a later cleanup deletes as dead. Removing it removes the WITNESS, not the
        // behaviour: FakeWorld records every call, so `presentedAlong.size()` positively witnesses
        // that the ray fired AND reached this line, and a span length of 0 only means something once
        // that is established. The alternative -- skipping the call -- asserts an ABSENCE, which
        // passes just as well when the cast never resolved, when the fixture authored no beam id, or
        // when a cost check tripped. A fixture wiring nothing passes it.
        //
        // aRayWithNoBeamDrawsNothing (CastExecutorTest) is this repo's own precedent, written before
        // this slice: it pairs its empty presentedAlong with a positive assertion on `presented` "so
        // this is not a test of a dead cast". A witness in the SAME list as the assertion cannot be
        // deleted separately from it.
        //
        // THE COLLAPSE POINT IS THIS SEGMENT'S OWN `from`, NOT `beamEnd`, AND THAT IS A REGION RULE
        // RATHER THAN A TASTE. PaperCombatWorld.presentAlong hops on ctx.scheduler().onRegion(from)
        // -- its javadoc's "the end the caller is already standing on".
        //
        // THE UNIVERSAL PROPERTY, stated as the guarantee rather than as an example: `from` is
        // ALWAYS in this segment's own column and is where the caller already stands. `beamEnd` is
        // guaranteed NEITHER -- for a non-final segment it is `to`, which lies exactly ON a chunk
        // plane, and a boundary coordinate belongs to the column on its POSITIVE side
        // (columnOf = floor(x / 16)). So collapsing to beamEnd schedules into a DIFFERENT region to
        // draw nothing, and falsifies that javadoc -- but ONLY when the ray travels +x or +z:
        //
        //   +x from 15.6 -> segment [15.6, 16.0], columnOf(16.0) = 1, segment column 0   HAZARD
        //   -x from 32.4 -> segment [32.4, 32.0], columnOf(32.0) = 2, segment column 2   none
        //
        // THE ASYMMETRY IS STATED BECAUSE THE FIRST DRAFT OF THIS COMMENT DID NOT. It cited the +x
        // instance under a universal that does not hold for -x/-z, and the test staged the rule on a
        // -x fixture where it could not bite. That is the same blind spot the paragraph above warns
        // about -- an implementation that fails BY AIM DIRECTION -- reappearing one layer up, inside
        // the warning about it. theSuppressedSegmentCollapsesToAPointInItsOwnCHUNKCOLUMN now stages
        // the +x case and carries a control proving its far end really is in the next column.
        //
        // COST, AND IT IS UNMEASURED: presentAlong schedules its region hop BEFORE BeamSamples
        // returns empty, so every suppressed segment costs one no-op hop. For a six-shot volley
        // fired point-blank that is six per cast. Nobody has timed it.
        boolean anythingToDraw = beamEnd.subtract(drawFrom).dot(alongSegment) > 0;
        if (beam != null) {
            if (anythingToDraw) world.presentAlong(drawFrom, beamEnd, beam);
            else world.presentAlong(from, from, beam);
        }

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

        // beamStart rides across the region hop unchanged. It is a frozen Vec3, never a handle, so
        // it obeys the same rule as everything else threaded through this walk.
        world.schedule(to, 1, () -> stepRay(ability, caster, beam, to, beamStart, endpoints, index + 1));
    }

    /**
     * The nearest living thing inside the swing. arcDegrees is the full width of
     * the cone, so a 90-degree swing reaches 45 degrees either side of the aim.
     *
     * A candidate the caster cannot SEE is skipped: in range and in arc is not enough,
     * or a swing at a wall damages whoever stands behind it. The check is block-only
     * (see CombatWorld.lineOfSightClear), so a mob behind another mob is still fair game.
     *
     * Note the ordering. The line-of-sight skip MUST happen before nearestDistanceSquared
     * is assigned, never between the assignment and the target being taken -- a blocked
     * candidate that lowers the bound on its way out would push a CLEAR candidate behind
     * it out of contention and the swing would whiff entirely. Gating the bound behind the
     * verdict is also the cheaper of the two correct orders: the trace, the only expensive
     * call here, then runs once per improvement rather than once per candidate in the arc.
     *
     * This gates the DIRECT target only. A Burst or area payload detonating at the impact
     * still splashes through walls -- EffectApplier.applyToNearby is a plain radius query --
     * as does a Dash sweep. Deliberately out of scope here; do not read this as covering them.
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
            if (distanceSquared >= nearestDistanceSquared) continue;
            if (!world.lineOfSightClear(aim.origin(), sightPoint(candidate.state()))) continue;

            nearestDistanceSquared = distanceSquared;
            nearest = candidate;
        }
        return nearest;
    }

    /**
     * The point on a combatant a sight line traces TO: its eye, not its feet.
     *
     * position() is the entity's feet, so a ray ending there hugs the floor for its whole
     * final stretch and any lip, slab or step in between reads as a wall. Vanilla's own
     * LivingEntity.hasLineOfSight traces eye to the TARGET'S eye; this is that point.
     */
    private static Vec3 sightPoint(CombatantSnapshot target) {
        return target.position().add(new Vec3(0, target.eyeHeight(), 0));
    }


    /**
     * Start a volley: fire shot 1 after the wind-up, or inline if there is none.
     *
     * <p>{@code windupTicks == 0} runs the first shot on the cast frame rather than a tick later,
     * exactly as {@link #launchRay} runs its first chunk-column segment inline and for the same
     * reason -- the scheduler cannot defer by less than a tick, so deferring a zero wind-up would
     * quietly make it 1 and a "no wind-up" volley would be indistinguishable from a 1-tick one.
     */
    private void beginVolley(AbilityDefinition ability, UUID casterId, CastSpec.Volley spec) {
        if (spec.windupTicks() == 0) {
            volley(ability, casterId, spec, 0);
            return;
        }
        world.scheduleOn(casterId, spec.windupTicks(), () -> volley(ability, casterId, spec, 0));
    }

    /**
     * One shot of a volley, and then the clock for the next.
     *
     * <p><b>THE RE-READ IS THE MECHANISM, NOT AN OPTIMISATION.</b> Every other cast shape freezes
     * its caster once, at commit, because everything it does afterwards resolves somewhere the
     * caster's own stats cannot legally be read. A volley is the exception: it is scheduled ON the
     * caster, so each shot runs on the caster's own thread and a fresh read is both legal and the
     * point. Six shots off ONE projection would share one crit roll, one price and one line of
     * sight -- six copies of a single decision rather than a burst the player steers.
     *
     * <p>So each shot gets a fresh aim (where they are looking NOW) and a fresh
     * {@link io.github.butterflysmp.rpg.core.combat.Caster} (a new crit roll, and whatever weapon
     * they are holding at that tick). <b>A volley's stats are deliberately NOT atomic:</b> a player
     * who swaps weapons after shot 3 has shots 4 onward priced off the new one. That is a
     * consequence of the ruling, not an oversight.
     *
     * <p><b>BOTH READS ANSWER THE SAME CONDITION AND ANSWER IT THE SAME WAY.</b> An unreadable aim
     * means an unreadable caster, so it STOPS the volley -- it does NOT fall back to the cast
     * frame's aim. A fallback there would silently disable the re-aim this whole shape exists for:
     * the burst would fire down a line the player abandoned ten ticks ago, with no signal, and
     * present as "sometimes it doesn't follow" -- unreproducible, and read as lag.
     *
     * <p><b>TRIGGER -- why {@code execute}'s cast-frame projection is inert for a volley, which is
     * a LOADER guarantee rather than anything visible here.</b> That projection carries a crit roll
     * and is consumed only by {@code on_cast}. The cast-frame projection's crit roll is inert
     * BECAUSE {@code AbilitySchema.parseCastVisuals} refuses non-visual effects in {@code on_cast},
     * and visuals read no stats. If that ever admits a {@code Damage} effect, this roll becomes live
     * and crits independently of every shot. Recorded because a reader who checks only this class
     * sees a value rolled and discarded, and would delete it correctly on the evidence in front of
     * them.
     */
    private void volley(AbilityDefinition ability, UUID casterId, CastSpec.Volley spec, int shotIndex) {
        Combatant self = world.combatant(casterId).orElse(null);
        if (self == null || !self.state().alive()) return;   // gone, or dead: the volley stops
        Aim live = world.aimOf(casterId).orElse(null);
        if (live == null) return;                            // same condition, same answer

        Caster source = Caster.of(self.state()).withPayloadDamage(
                DamagePayload.headlineDamage(ability.onHit(), self.state().attackDamage()));

        fireInner(ability, source, live, spec.of());

        if (shotIndex + 1 >= spec.shots()) return;
        world.scheduleOn(casterId, spec.intervalTicks(),
                () -> volley(ability, casterId, spec, shotIndex + 1));
    }

    /**
     * Dispatch one shot's inner cast. The RUNTIME half of the whitelist {@code AbilitySchema}
     * enforces at load.
     *
     * <p>It is an EXHAUSTIVE pattern switch and not an {@code if} chain so that a seventh
     * {@code CastSpec} kind cannot become repeatable by default: it will not compile until someone
     * states an answer here. A gate says what it CAN be, never what it cannot.
     *
     * <p>The refusals are not tidiness. {@code dash} in particular is load-bearing:
     * {@code paper.weapon.DashAim} resolves a dash's direction BEFORE the region hop and matches on
     * the OUTER cast only, so a volley of dashes would reach here having silently lost its direction
     * resolution. {@code self} and {@code melee} land at a point the caster already occupies, so
     * repeating them is a sound with no mechanism behind it. A nested volley never reaches this --
     * {@code CastSpec.Volley}'s compact constructor refuses it at construction.
     *
     * <p>Reaching a refusal here means the loader's whitelist was bypassed, which is a programming
     * error rather than a content one -- hence a throw, where the loader gives a named, skipped file.
     */
    private void fireInner(AbilityDefinition ability, Caster source, Aim aim, CastSpec inner) {
        switch (inner) {
            case CastSpec.Ray ray -> launchRay(ability, source, aim, ray.range(), ray.beam());
            case CastSpec.Projectile projectile -> launch(ability, source, aim, projectile);
            case CastSpec.Self ignored -> throw new IllegalStateException(notRepeatable("self"));
            case CastSpec.Melee ignored -> throw new IllegalStateException(notRepeatable("melee"));
            case CastSpec.Dash ignored -> throw new IllegalStateException(notRepeatable("dash"));
            case CastSpec.Volley ignored -> throw new IllegalStateException(notRepeatable("volley"));
        }
    }

    private static String notRepeatable(String type) {
        return "a volley cannot repeat cast type '" + type + "'; only ray and projectile are"
                + " repeatable, and the loader refuses the rest by name";
    }
    private void detonate(AbilityDefinition ability, Caster caster, Combatant target, Vec3 impact) {
        effects.applyAll(ability.onHit(), caster, target, impact);
    }
}
