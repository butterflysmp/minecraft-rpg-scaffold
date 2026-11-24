package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.build.LandingHook;
import io.github.butterflysmp.rpg.core.build.Touchdown;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Caster;
import io.github.butterflysmp.rpg.core.combat.ChunkTraversal;
import io.github.butterflysmp.rpg.core.combat.ConeShell;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.ProjectileFlight;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.core.combat.RaySweep;
import io.github.butterflysmp.rpg.core.mob.BeamLine;
import io.github.butterflysmp.rpg.core.combat.SpreadPattern;
import io.github.butterflysmp.rpg.core.combat.SweptLine;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    /** A cast whose press is THIS tick: every caller with no hop between the press and the cast (a test, a dev cast). */
    public void execute(AbilityService.CastResult.Success success) {
        execute(success, world.currentTick());
    }

    /**
     * A cast PRESSED at {@code pressTick} (PLAN-enchant-columns.md section 19, the seat's ruling (c), 2026-11-24). READ:
     * {@code WeaponFire} charges at the press on tick T and hops the cast to the region scheduler, which runs it at T + 1,
     * so a tick read here is the CAST's, one after the press. The press's tick is stamped before the hop and carried in.
     * It reaches only the volley's trace ({@code FOCUSED pressed=}); nothing decides on it.
     */
    public void execute(AbilityService.CastResult.Success success, long pressTick) {
        AbilityDefinition ability = success.ability();
        boolean charges = charges(ability);
        Caster source = commit(ability, success.caster(), success.aim(), charges, success.weaponTrigger());
        AbilityDefinition tuned = tuned(ability, success.caster(), source);
        dispatch(ability, tuned, success.caster(), tunedSource(ability, tuned, success.caster(), source), success.aim(),
                charges, pressTick);
    }

    /**
     * THE WEAPON'S OWN CAST WITH ITS ACTIVE TUNES APPLIED (the seat's G4, 2026-11-24): {@link CastTunes}, over the
     * behaviours the commit just froze. A cast no tune touches comes back the same instance.
     */
    private static AbilityDefinition tuned(AbilityDefinition ability, CombatantSnapshot caster, Caster source) {
        return CastTunes.tune(ability, source.behaviours());
    }

    /**
     * The Caster for a tuned cast: its headline damage re-read off the TUNED on_hit, so a status capped by the hit (scorch's
     * cap is half of it) is capped by the damage the cast now deals, Large Sparks' +12 included. Unchanged when no tune
     * applied.
     */
    private static Caster tunedSource(AbilityDefinition ability, AbilityDefinition tuned, CombatantSnapshot caster,
                                      Caster source) {
        return tuned == ability ? source
                : source.withPayloadDamage(DamagePayload.headlineDamage(tuned.onHit(), caster.attackDamage()));
    }

    /**
     * ONE CAST, FIRED ALONG SEVERAL DIRECTIONS AT ONCE -- the Dragon's Plume's release.
     *
     * <p>Each offset in {@code yawOffsets} is a horizontal rotation of the aim, in degrees, applied
     * with {@link io.github.butterflysmp.rpg.core.Vec3#rotateAboutY}. {@code DrawFan.offsetsFor}
     * produces them; the same geometry {@code EffectSpec.ThrowEmbers.fan} already uses one layer
     * down.
     *
     * <h2>IT COMMITS ONCE AND DISPATCHES N TIMES, AND THAT SPLIT IS THE WHOLE POINT OF THE METHOD</h2>
     *
     * <p><b>Calling {@link #execute} five times would have been the obvious wiring and it is wrong
     * twice over</b>, because {@code execute} does two things per call that belong to the PRESS
     * rather than to the arrow:
     *
     * <pre>
     * on_cast effects     the sound you hear when you let go -- FIVE sounds for one release
     * onBasicAttackUse    durability -- FIVE uses billed for one release
     * </pre>
     *
     * <p>Both are already documented in {@code execute} as per-press: the cast hook <i>"lands on the
     * frame the cast was committed"</i>, and the wear comment reasons from vanilla, where <i>an arrow
     * costs the bow</i> -- one bow, one arrow, one use. <b>A five-arrow release is one press, so it
     * is one sound and one use.</b> The melee arm's own note makes the same argument from the other
     * side: a payload that splashes across five bodies is still ONE use.
     *
     * <p><b>THE FAN ROTATES THE AIM, so it means something only for a cast shape that READS the
     * aim's direction</b> -- {@link CastSpec.Ray} and {@link CastSpec.Projectile}. A {@code Self} or
     * a {@code Dash} fanned would be N identical casts, because neither consults the direction.
     * That is not guarded here: it is <b>loud rather than silent</b> -- five dashes is visible on the
     * first press -- and a runtime throw mid-release would be worse than the thing it prevents.
     *
     * <p><b>An empty array fires nothing AND COMMITS NOTHING</b> -- no sound, no wear. A release of
     * zero arrows is not a press that happened quietly; it is a press that produced no shot, and
     * billing the bow for it would be charging for an arrow nobody got.
     */
    public void executeFan(AbilityService.CastResult.Success success, double[] yawOffsets) {
        executeFan(success, yawOffsets, world.currentTick());
    }

    /** {@link #executeFan(AbilityService.CastResult.Success, double[])} PRESSED at {@code pressTick}: see {@link #execute(AbilityService.CastResult.Success, long)}. */
    public void executeFan(AbilityService.CastResult.Success success, double[] yawOffsets, long pressTick) {
        if (yawOffsets.length == 0) return;

        AbilityDefinition ability = success.ability();
        CombatantSnapshot caster = success.caster();
        Aim aim = success.aim();
        boolean charges = charges(ability);

        Caster committed = commit(ability, caster, aim, charges, success.weaponTrigger());
        AbilityDefinition tuned = tuned(ability, caster, committed);
        Caster source = tunedSource(ability, tuned, caster, committed);
        for (double yaw : yawOffsets) {
            // `pointing` rather than `new Aim(origin, rotated)`: the two-argument constructor would
            // RE-DERIVE the shooter's right from the rotated direction and discard the real one,
            // which is a silent loss for any inner cast that reads the view plane. The fan itself
            // does not care -- a yaw rotation needs no basis -- so this is carrying a field
            // across rather than fixing a live bug, and it is what stops a fanned SPREAD from
            // inheriting a basis that was quietly rebuilt from the wrong vector.
            dispatch(ability, tuned, caster, source, aim.pointing(aim.direction().rotateAboutY(yaw)), charges, pressTick);
        }
    }

    /**
     * WHAT COSTS A USE -- the whole rule, in one expression, rather than left to each caller to
     * remember.
     *
     * <p>Only a BASIC ATTACK charges: an ability already spends mana, and charging it as well would
     * bill one press twice. The gate is structural for the same reason {@code Durability}'s
     * {@code maxDurability <= 0} guard is -- it is the ONLY thing separating the two shipped
     * projectiles from each other. {@code hunters_bow}'s (deleted 2026-11) shot and {@code emberblade}'s (deleted 2026-11) Fireball are
     * both {@code type: projectile}; nothing about the cast shape tells them apart, so a check left
     * to the wiring is a check a future call site can forget.
     *
     * <p>A method rather than a local, now that {@link #execute} and {@link #executeFan} both need
     * it: two copies of this expression is two places for the rule to drift.
     */
    private static boolean charges(AbilityDefinition ability) {
        return DamagePayload.isBasicAttack(ability.onHit());
    }

    /**
     * EVERYTHING A PRESS PAYS FOR, ONCE, WHATEVER IT GOES ON TO FIRE: the cast-frame projection of
     * the caster, the {@code on_cast} effects, and the durability use.
     *
     * <p>Split out of {@link #execute} when {@link #executeFan} arrived. <b>The split is what makes
     * a five-arrow release one sound and one use</b>; before it there was no seam between the press
     * and the shot, because nothing had ever needed one.
     *
     * @return the cast-frame {@link Caster} projection, shared by every shot the press produces
     */
    private Caster commit(AbilityDefinition ability, CombatantSnapshot caster, Aim aim,
                          boolean charges, boolean weaponTrigger) {

        // Project the cast-time snapshot down to what an effect landing LATER may read: the id,
        // plus the stats frozen on the caster's own thread. Built once, here, because this is the
        // last point that is still unambiguously the caster's frame -- a projectile's impact and
        // an area's pulse both resolve on somebody else's region.
        // The payload's headline damage rides the Caster so a status arm deep inside a burst can read
        // it as its CAP. Frozen here with everything else: a projectile is capped by what it was
        // fired with, not by what the caster holds when it lands.
        Caster source = Caster.of(caster)
                .withPayloadDamage(DamagePayload.headlineDamage(ability.onHit(), caster.attackDamage()))
                // The held weapon's gear score, frozen here with everything else, for the same reason:
                // a staff's bolt is priced by the staff that FIRED it, not by what is in hand when it
                // lands. It scales the authored literal in EffectApplier's Damage arm.
                .withTriggerScore(world.triggerScoreOf(caster.id()))
                // Which ability this is, for the PLAYERHIT trace only (Caster.withSource).
                .withSource(ability.id());
        // A WEAPON TRIGGER is a weapon hit, and only a weapon hit carries the level damage bonus
        // (Caster.withWeaponHit; PLAN-level-bonuses.md, seat ruling L1). A stone cast arrives here with
        // weaponTrigger false and leaves with it false. Frozen with everything else, so a staff's bolt
        // lands as the weapon hit it was fired as.
        // B2 (2026-11-23): a weapon trigger also carries the behaviours of the weapon that made it, frozen here. A stone cast
        // carries none (it is not made by the held weapon).
        if (weaponTrigger) source = source.withWeaponHit().withBehaviours(world.behavioursOf(caster.id()));

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

        // A melee use is charged on CONNECT (in the Melee arm of dispatch); every other shape is
        // charged at COMMIT, here. An arrow costs the bow whether or not it lands, like vanilla; a
        // swing that touches nothing is free.
        //
        // AND THIS IS WHY A FANNED RELEASE BILLS ONCE: the wear is here, on the press, not in the
        // per-shot dispatch below. Five arrows, one use -- the same argument the Melee arm makes
        // when a splashing payload catches five bodies.
        if (charges && !(ability.cast() instanceof CastSpec.Melee)) onBasicAttackUse.run();

        return source;
    }

    /**
     * ONE SHOT, ALONG ONE AIM. The cast-shape switch, split out of {@link #execute} so
     * {@link #executeFan} can run it once per direction without repeating what the press already
     * paid for.
     *
     * <p>Everything here is per-ARROW. Everything in {@link #commit} is per-PRESS. <b>That is the
     * only rule this split encodes, and getting a line on the wrong side of it is how a release
     * starts billing five uses or playing five sounds.</b>
     */
    private void dispatch(AbilityDefinition base, AbilityDefinition ability, CombatantSnapshot caster, Caster source,
                          Aim aim, boolean charges, long pressTick) {
        // `ability` is the TUNED cast (G4); `base` the weapon's own, which only a volley needs: it re-tunes at each shot
        // off the behaviours that shot reads (ruling 1), so it must start from the untuned one.
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

            // NEVER CAST BY US: bound only to the shot trigger, which no input names (WeaponDefinition refuses it elsewhere),
            // so dispatch cannot be reached with it. Its hit is landVanillaShot, from where vanilla's arrow lands.
            case CastSpec.Vanilla ignored -> { }

            case CastSpec.Ray ray -> launchRay(ability, source, aim, ray);

            case CastSpec.Projectile projectile -> launch(ability, source, aim, projectile);

            case CastSpec.Dash dash -> dash(ability, caster, source, aim, dash);

            // NOTE WHAT IS NOT PASSED: `source`. A volley re-projects its caster before EVERY shot, so the
            // cast-frame projection above reaches no shot at all -- see volley(). `aim` IS passed, since
            // 2026-11-21, and is read ONLY by a locked-aim volley: the line captured at the cast.
            // The TUNED spec times the wind-up and its warning (read at the press: nothing can re-read a wind-up already
            // running); the BASE ability is what each shot re-tunes.
            case CastSpec.Volley volley -> beginVolley(base, caster.id(), volley, source.weaponHit(), aim, pressTick);

            case CastSpec.Cone cone -> beginCone(ability, source, aim, cone);

            case CastSpec.Orb orb -> throwOrb(ability, source, aim, orb);
        }
    }

    /**
     * How far past the front the cone's world query reaches. {@code combatantsNear} finds bodies by their position (in
     * production, a box about it), and the cone judges the SIGHT POINT, which can stand an eye height above the feet; so
     * the query is padded and {@link ConeShell#reached} judges the exact distance. Covers any body whose sight point is
     * within 4 blocks of its position.
     */
    private static final double CONE_QUERY_PAD = 4.0;

    /**
     * START A CONE (the seat's brief of 2026-11-22, the Living Lantern, rulings 1 to 4). {@code aim} is the line captured
     * at the click, a pair of values; it is the only aim the cone ever reads (ruling 2), so the caster turning or
     * walking afterwards moves nothing. Instant ({@code travel_ticks 0}) runs its one pass inline, on the cast frame;
     * otherwise the first pass is a tick later, with the front at {@code range / N}.
     */
    private void beginCone(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Cone cone) {
        if (cone.travelTicks() == 0) {
            conePass(ability, caster, aim, cone, 0, Set.of());
            return;
        }
        world.schedule(aim.origin(), 1, () -> conePass(ability, caster, aim, cone, 1, Set.of()));
    }

    /**
     * ONE TICK OF THE FRONT. It draws the front, then strikes, nearest first, every body {@link ConeShell#reached}
     * finds in this tick's shell that the eye can SEE ({@code lineOfSightClear} from the origin to the body's sight
     * point, ruling 3: a block stops it; a body behind another body does not). Each struck body takes the
     * {@code on_hit} at its feet, as the melee swing's target does. {@code struck} carries the ids already struck
     * across ticks, as values (the Knell Beam's rule 7), so a body is struck once per cast.
     *
     * <p><b>A MISS LANDS NOTHING.</b> Unlike a ray, which goes off at its end, a cone that reaches nobody detonates
     * nowhere: its front IS its presentation, and an {@code on_hit} visual at the range's end would be a puff at a
     * point the cone never singles out.
     *
     * <p><b>THREADING.</b> Every pass after the first is {@code world.schedule} at the ORIGIN, so it runs on the region
     * owning the eye's point. The query reaches {@code range + CONE_QUERY_PAD} from there (10 blocks for a range of 6),
     * which on Folia can read a neighbouring region's chunks: the same standing as the defect the adapter documents on
     * {@code combatantsNear} for a ray's walk, and the reason a long cone would need chunk-column hops. Paper runs one
     * thread, so it does not bite there. Recorded, not guarded.
     */
    private void conePass(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Cone cone, int tick,
                          Set<UUID> struck) {
        int n = cone.travelTicks();
        double outer = n == 0 ? cone.range() : cone.range() * tick / n;
        double inner = n == 0 || tick <= 1 ? Double.NEGATIVE_INFINITY : cone.range() * (tick - 1) / n;

        if (cone.front() != null) {
            for (Vec3 point : ConeShell.frontPoints(aim, outer, cone.arcDegrees())) world.present(point, cone.front());
        }

        Set<UUID> struckNow = new HashSet<>(struck);
        var candidates = world.combatantsNear(aim.origin(), outer + CONE_QUERY_PAD);
        for (ConeShell.Reached reached : ConeShell.reached(aim, cone.arcDegrees(), inner, outer, candidates,
                caster.id(), struck)) {
            Combatant body = reached.combatant();
            if (!world.lineOfSightClear(aim.origin(), body.state().sightPoint())) continue;
            struckNow.add(body.id());
            detonate(ability, caster, body, body.state().position());
        }

        if (n == 0 || tick >= n) return;
        Set<UUID> carried = Set.copyOf(struckNow);
        world.schedule(aim.origin(), 1, () -> conePass(ability, caster, aim, cone, tick + 1, carried));
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
        // The score is set here too, even though the WeaponDamage arm a basic melee usually lands
        // IGNORES it -- the stat already carries it. A melee weapon's ability can still author a
        // LITERAL Damage effect, and a site that silently carried BASELINE would price that literal
        // as unscored while every other route scaled it.
        // Always a weapon hit: this arm is reached only from a held weapon's vanilla-melee trigger.
        detonate(ability, Caster.of(caster, chargeScale)
                .withPayloadDamage(DamagePayload.headlineDamage(ability.onHit(), caster.attackDamage()))
                .withTriggerScore(world.triggerScoreOf(caster.id()))
                .withSource(ability.id())
                .withWeaponHit()
                // B2: the swing is made by the weapon in hand NOW, so its behaviours are read now.
                .withBehaviours(world.behavioursOf(caster.id())),
                target, target.state().position());
        if (DamagePayload.isBasicAttack(ability.onHit())) onBasicAttackUse.run();
    }

    /**
     * THE HIT OF A VANILLA-DRIVEN SHOT (the seat's ruling 10, 2026-11-22): the weapon's {@code shot} on_hit lands on the
     * target vanilla's arrow struck, through the same sum and the same PLAYERHIT line as every weapon hit. Unlike
     * {@link #landBasicMelee}, the SCORE is the one stamped at the shot, never the world's at the hit (he may hold another
     * weapon by then), and the basic-attack wear hook does NOT run: vanilla wore the bow at the shot.
     *
     * @param caster       the live shooter carrying the damage terms stamped at the shot ({@code ShotStamp.applyTo})
     * @param triggerScore the held weapon's gear score at the shot
     * @param chargeScale  the shot's force, clamped to 1 ({@code VanillaShot.chargeScale})
     */
    public void landVanillaShot(AbilityDefinition ability, CombatantSnapshot caster, int triggerScore,
                                Combatant target, double chargeScale) {
        landVanillaShot(ability, caster, triggerScore, List.of(), target, chargeScale);
    }

    /**
     * The same, with the BEHAVIOURS of the bow that released it (B2, 2026-11-23): stamped at the shot with the score,
     * never read at the hit.
     */
    public void landVanillaShot(AbilityDefinition ability, CombatantSnapshot caster, int triggerScore,
                                List<io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour> behaviours,
                                Combatant target, double chargeScale) {
        detonate(ability, Caster.of(caster, chargeScale)
                .withPayloadDamage(DamagePayload.headlineDamage(ability.onHit(), caster.attackDamage()))
                .withTriggerScore(triggerScore)
                .withSource(ability.id())
                .withWeaponHit()
                .withBehaviours(behaviours),
                target, target.state().position());
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
        Vec3 drive = aim.direction();
        // Directed untargeted effects (an ember fan) fire toward the caster's FACING. For a
        // reverse-facing dash that is the opposite of the drive: you throw forward, then
        // retreat away from what you threw.
        Vec3 facing = dash.direction() == CastSpec.DashDirection.REVERSE_FACING ? drive.negate() : drive;

        Combatant self = world.combatant(caster.id()).orElse(null);
        if (self != null) {
            Vec3 impulse = drive.scale(dash.speed()).add(new Vec3(0, dash.lift(), 0));
            self.handle().applyImpulse(impulse);
            // Ruling 31: queued AFTER the impulse on the same handle, so the mark exists before the take-off.
            // A slam is the same mark with a hook (the seat's ruling 5, F-d): one arm, never both.
            if (!dash.onLand().isEmpty()) self.handle().armLanding(slam(ability, caster, source, dash, facing));
            else if (dash.safeLanding()) self.handle().armSafeLanding();
        }

        Vec3 origin = caster.position();
        double reach = dash.distance();
        Vec3 midpoint = origin.add(drive.scale(reach / 2));
        var candidates = world.combatantsNear(midpoint, reach / 2 + DASH_HIT_RADIUS);

        List<Combatant> hits = SweptLine.enemiesAlong(
                origin, drive, reach, DASH_HIT_RADIUS, candidates, caster.id());

        // Origin is the caster's PRE-dash snapshot feet, so the embers launch from where you
        // stood, not from where the impulse is carrying you.
        effects.applyToSet(ability.onHit(), source, hits, origin, facing);
    }

    /**
     * SUNDER'S SLAM (PLAN-melee-class.md section 8.2, S1): the dash's {@code on_land} at the LIVE feet, when and
     * however the leap comes down. The {@code source} Caster is the one frozen at cast, like a grenade's: the slam
     * lands with the cast's numbers whatever the caster picked up mid-air. The trace goes first, so the log reads
     * the landing before the hits it caused. No target set: {@code on_land} is untargeted by schema (a burst finds
     * its own), and the caster is excluded from it as from every burst.
     */
    private LandingHook slam(AbilityDefinition ability, CombatantSnapshot caster, Caster source,
                             CastSpec.Dash dash, Vec3 facing) {
        return new LandingHook() {
            @Override public void landed(Vec3 feet, Touchdown how) {
                world.traceLanding(caster.id(), ability.id(), how, feet);
                effects.applyToSet(dash.onLand(), source, List.of(), feet, facing);
            }

            @Override public void ended(Touchdown how) {
                world.traceLanding(caster.id(), ability.id(), how, null);
            }
        };
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
        // ONE ROLL, N DELIVERIES -- AND THE ROLL IS NOT MADE HERE, WHICH IS THE POINT.
        //
        // Every body below shares the single `caster` this method was handed. That Caster was
        // built ONCE in commit(), from a snapshot taken ONCE on the player's own thread, and the
        // CRIT WAS DRAWN INTO THAT SNAPSHOT -- BukkitCombatant.snapshot says so in those words:
        // "drawn HERE and frozen -- once per cast, never per damage arm". So a seven-body spread
        // crits as a unit, the same way a Burst catching five bodies already does.
        //
        // *** SAY IT HERE BECAUSE A BUILD THAT RE-ROLLED PER BODY WOULD PASS EVERY TEST THAT DOES
        // NOT STAGE A CRIT. *** Without a crit the seven amounts are identical either way, so the
        // defect is invisible to any row that does not deliberately produce one. The property is
        // INHERITED from where the snapshot is taken, not built here -- which means the way to
        // break it is to move the expansion ABOVE commit(), not to change anything in this method.
        //
        // The loop is here rather than in paper for exactly that reason: WeaponFire's fan path
        // spends one quiver round PER OFFSET (`yawOffsets.length`), so a spread expanded there
        // would bill seven rounds for one press. Expanded below commit it bills one, by
        // construction rather than by a second rule that could drift.
        if (spec.spread() != null) {
            // SHATTER CAPS (2026-11-24): its weapon's spread is replaced by ONE stone, here, below the commit, so the
            // press still bills one round and one cooldown, as the spread does.
            // THE ability-id CHECK STAYS after the weapon filter (paper BehaviourItems.activeOf, 2026-11-24), because the
            // two ask different questions: the filter, "is this the weapon's ITEM"; this, "is this the weapon's CAST". They
            // differ only on a volley's per-shot re-read (the seat's ruling 1: the weapon in hand at each shot), where a
            // weapon swapped in mid-volley lends its behaviours to a cast another weapon pressed. No shipped volley fires a
            // spread, so here it is a belt; Focused Beam's identical check is not (knell_beam's volley is a ray).
            for (io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active : caster.behaviours()) {
                if (active.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Shatter caps
                        && ability.id().startsWith(caps.weaponId() + "/")) {
                    launchStone(ability, caster, aim, spec, active, caps);
                    return;
                }
            }
            for (Vec3 direction : SpreadPattern.directionsFor(aim.direction(), aim.right(), spec.spread())) {
                launchOne(ability, caster, aim.pointing(direction), spec);
            }
            return;
        }
        launchOne(ability, caster, aim, spec);
    }

    /** One body, down one direction. The spread above is N of these; everything else is one. */
    private void launchOne(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Projectile spec) {
        // AIRBURST (2026-11-23): a behaviour of the weapon that made this cast may end the flight at a TRAVEL distance
        // and split it. Only when that distance is reached inside the authored lifetime; otherwise the shot flies as
        // authored and never splits (it cannot travel that far).
        for (io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active : caster.behaviours()) {
            if (active.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst burst) {
                int splitTicks = io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst
                        .ticksToTravel(burst.travelBlocks(), spec.speed());
                if (splitTicks < spec.maxLifetimeTicks()) {
                    launchToSplit(ability, caster, aim, spec, active, burst, splitTicks);
                    return;
                }
            }
        }
        ProjectileFlight.launch(world, caster, aim.origin(), aim.direction().scale(spec.speed()),
                spec.gravity(), spec.maxLifetimeTicks(),
                // All three presentation fields, mapped straight across. The exclusion between
                // `item` and `body` is the RECORDS' invariant at both ends, so this is a copy and
                // not a decision -- and a Projectile that got past its own constructor cannot
                // produce a Look that fails its one.
                new ProjectileFlight.Look(spec.trail(), spec.item(), spec.body()),
                seekOf(spec),
                (target, point) -> detonate(ability, caster, target, point));
    }

    /**
     * HOW FAR SHORT OF A SOLID BLOCK AN ORB STOPS, in blocks (SEAT PLACEHOLDER 2026-11-23): its centre rests this far back
     * along the throw from the block's face, so its body is never inside the block.
     */
    static final double ORB_STANDOFF = 0.5;

    /**
     * THE THROWN ORB (Lapis Crystal's D2; Ben, 2026-11-23: "throws an orb out about 12 blocks in the direction the player
     * is looking and it floats there for 15s" / "One orb at a time"). Its rest point is the aim's {@code range} or, short
     * of that, {@link #ORB_STANDOFF} before the first solid block ({@code sweepRay}'s {@code blockAt}; bodies do not stop
     * it). A standing orb of his is removed first (cause {@code replaced}). The body flies there over
     * {@code flightTicks}, driven a step a tick as a projectile's marker is; on arrival the orb RESTS ({@code ORB rest}
     * line) and its {@code lifeTicks} start; at their end it is removed (cause {@code expired}), unless a newer throw or a
     * removal already took it (the generation).
     *
     * <p>The orb remembers the GRANT that threw it (the active behaviour whose ability this is): its weapon, enchant and
     * level, for D3's beam and shots. An orb no grant threw remembers none.
     */
    private void throwOrb(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Orb orb) {
        Vec3 direction = aim.direction().normalize();
        Vec3 end = aim.origin().add(direction.scale(orb.range()));
        Optional<Vec3> block = world.sweepRay(aim.origin(), end, caster.id(), 0.0).blockAt();
        Vec3 rest = io.github.butterflysmp.rpg.core.combat.OrbPath.restPoint(aim.origin(), direction, orb.range(),
                block, ORB_STANDOFF);

        String weaponId = null, enchantId = null;
        int level = 0;
        for (io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active : caster.behaviours()) {
            if (active.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.GrantTrigger g
                    && g.ability().id().equals(ability.id())) {
                weaponId = g.weaponId();
                enchantId = active.enchantId();
                level = active.level();
            }
        }

        io.github.butterflysmp.rpg.core.combat.Orbs orbs = world.orbs();
        // THE BODY, ONE OF TWO KINDS (Ben's item 3, 2026-11-23): the item marker D2 shipped, or a block display.
        boolean blockBody = orb.block() != null;
        UUID marker = blockBody ? world.spawnOrbBlock(caster.id(), aim.origin(), orb.block())
                : world.spawnOrbMarker(aim.origin(), orb.item(), orb.flightTicks() + orb.lifeTicks());
        // THE BACKSTOP (the seat's ruling (b)): a display has no vanilla despawn, so its own removal is scheduled at the
        // throw, past the orb's whole flight and life, and removes THAT body by id whatever the store says. Every proper
        // removal cancels it; it acts only when every other removal was skipped.
        long backstop = !blockBody ? -1 : world.scheduleCancellable(aim.origin(),
                orb.flightTicks() + orb.lifeTicks() + ORB_BACKSTOP_MARGIN_TICKS, () -> {
                    // And the body's look beat, if the store still holds its orb (Ben's changes, 2026-11-23): the
                    // backstop ends every task that animates the body it removes.
                    orbs.of(caster.id()).filter(o -> o.markerId().equals(marker)).filter(o -> o.lookTask() >= 0)
                            .ifPresent(o -> world.cancelScheduled(o.lookTask()));
                    world.removeMarker(marker);
                    world.traceOrb(caster.id(), "removed", rest, "backstop");
                });
        io.github.butterflysmp.rpg.core.combat.Orbs.PlaceResult placed = orbs.place(caster.id(), marker, rest,
                orb.radius(), orb.lifeTicks(), weaponId, enchantId, level, shotOf(orb), backstop);
        // THE CLAIM, ONLY NOW THE STORE OWNS IT (the seat's fix of #265, 2026-11-23): on 78826389 the block body carried
        // the orb tag from inside its spawn, and the sweep at its add found it unowned, every throw.
        world.claimOrbBody(marker);
        placed.replaced().ifPresent(old -> {
            // Every removal cancels the old orb's expiry (the seat's item 6) and its backstop (ruling (b)): its generation
            // guard already made the expiry harmless, and now nothing of it stays scheduled either.
            cancelOrbTasks(old);
            world.removeMarker(old.markerId());
            world.traceOrb(caster.id(), "removed", old.restPoint(), "replaced");
        });

        long generation = placed.placed().generation();
        Vec3 origin = aim.origin();
        Vec3 step = rest.subtract(origin).scale(1.0 / orb.flightTicks());
        // THE FLIGHT, ONE PATH FOR BOTH BODIES (the seat's ruling (a)): the item is handed this tick's step as velocity
        // and vanilla moves it by it; the display, which has no physics, is PLACED where that step ends, so after the
        // drive at tick t both stand at origin + (t + 1) x step, and both reach the rest point after the drive at tick
        // flightTicks - 1. The last placement is the rest point itself, not the sum of steps.
        flightStep(marker, blockBody, origin, step, rest, 0, orb.flightTicks());
        for (int tick = 1; tick < orb.flightTicks(); tick++) {
            int t = tick;
            world.schedule(origin, tick, () -> flightStep(marker, blockBody, origin, step, rest, t, orb.flightTicks()));
        }
        world.schedule(rest, orb.flightTicks(), () -> {
            if (blockBody) world.placeOrbBody(marker, rest);
            else world.driveMarker(marker, Vec3.ZERO);
            orbs.arrive(caster.id(), generation, world.currentTick()).ifPresent(arrived -> {
                world.traceOrb(caster.id(), "rest", arrived.restPoint(), "");
                // THE EXPIRY, by handle (the seat's item 6, 2026-11-23): the LINGER work=1 that opened LC2R2-LC4R2 on
                // efa38636 was this task, left scheduled by OrbRemovals.remove. It could not act (removeIf matches only
                // its own generation); the orb now carries its handle, and every removal cancels it.
                long expiry = world.scheduleCancellable(rest, orb.lifeTicks(),
                        () -> orbs.removeIf(caster.id(), generation).ifPresent(gone -> {
                            cancelOrbTasks(gone);   // its backstop and its look beat; its own handle is spent
                            world.removeMarker(gone.markerId());
                            world.traceOrb(caster.id(), "removed", gone.restPoint(), "expired");
                        }));
                orbs.expiring(caster.id(), generation, expiry);
                // ITS LOOK AT REST (Ben's changes, 2026-11-23): the bob, the turn and the particles, on a beat, about
                // the rest point, which does not move. A still body (no beat) has none.
                if (blockBody && orb.block().animated()) {
                    orbBeat(caster.id(), generation, marker, orb.block(), rest, arrived.arrivedTick());
                }
            });
        });
    }

    /** The orb cast's shot, in {@code Orbs}' own terms; null for an orb with none. */
    /**
     * How far past an orb's flight and life its body's BACKSTOP fires (the seat's ruling (b), 2026-11-23): 100 ticks, five
     * seconds, CC's figure, not a ruled one. Long enough that the proper expiry (at flight + life) always runs first, and it
     * changes nothing a player sees unless every other removal was skipped.
     */
    static final int ORB_BACKSTOP_MARGIN_TICKS = 100;

    /**
     * Step {@code tick} of an orb's flight (0 at the throw): an item body is handed {@code step} as velocity; a block body
     * is placed where that step ends, {@code origin + (tick + 1) x step}, or at {@code rest} itself on the last step.
     */
    private void flightStep(UUID marker, boolean block, Vec3 origin, Vec3 step, Vec3 rest, int tick, int flightTicks) {
        if (!block) {
            world.driveMarker(marker, step);
            return;
        }
        world.placeOrbBody(marker, tick + 1 >= flightTicks ? rest : origin.add(step.scale(tick + 1)));
    }

    /** Cancel an orb's expiry and its body's backstop, each if it has one: what every proper removal does. */
    private void cancelOrbTasks(io.github.butterflysmp.rpg.core.combat.Orbs.Orb orb) {
        if (orb.expiryTask() >= 0) world.cancelScheduled(orb.expiryTask());
        if (orb.backstopTask() >= 0) world.cancelScheduled(orb.backstopTask());
        if (orb.lookTask() >= 0) world.cancelScheduled(orb.lookTask());
    }

    /**
     * ONE BEAT OF A RESTING BODY'S LOOK (Ben, 2026-11-23: "Make it float up and down and spin slightly similar to a dropped
     * item"; "Add a few particles around the lapis crystal"). Scheduled {@code beatTicks} ahead at the rest point and
     * recorded on the orb ({@code Orbs.looking}), so every removal cancels it; when it runs it sets the display's
     * transformation to {@code look.pose(t)} interpolated over the beat, presents the particles at the rest point, and
     * schedules the next. It stops by itself if the orb is gone or replaced, or its body is gone. The ONE task does both
     * the motion and the particles. The rest point, what the beam meets and where the shots start, never moves.
     */
    private void orbBeat(UUID owner, long generation, UUID body, CastSpec.OrbBlock look, Vec3 rest, long restTick) {
        io.github.butterflysmp.rpg.core.combat.Orbs orbs = world.orbs();
        long handle = world.scheduleCancellable(rest, look.beatTicks(), () -> {
            boolean live = orbs.of(owner).filter(o -> o.generation() == generation).isPresent();
            if (!live || world.markerLocation(body).isEmpty()) return;
            world.poseOrbBody(body, look.pose(world.currentTick() - restTick), look.beatTicks());
            if (look.particles() != null) world.present(rest, look.particles());
            orbBeat(owner, generation, body, look, rest, restTick);
        });
        orbs.looking(owner, generation, handle);
    }

    private static io.github.butterflysmp.rpg.core.combat.Orbs.Shot shotOf(CastSpec.Orb orb) {
        CastSpec.OrbShot s = orb.shot();
        return s == null ? null
                : new io.github.butterflysmp.rpg.core.combat.Orbs.Shot(s.radius(), s.percentByLevel(), s.element(), s.visual());
    }

    /**
     * WHICH ORB THIS BEAM MAY MEET (D3; Ben: "Shooting this crystal with the Right Click ability"): HIS OWN orb only (the
     * orb is looked up by the beam's caster), RESTING, with a shot, and only when the beam is a trigger of the weapon that
     * GRANTED the orb (the ability's id is {@code <that weapon>/<input>}: the Lapis Staff's beam is
     * {@code lapis_staff/right_click}). No weapon or enchant id is written here; the orb remembers its grant.
     */
    private Optional<io.github.butterflysmp.rpg.core.combat.Orbs.Orb> meetsOrb(AbilityDefinition ability, Caster caster) {
        return world.orbs().of(caster.id())
                .filter(io.github.butterflysmp.rpg.core.combat.Orbs.Orb::resting)
                .filter(o -> o.shot() != null && o.weaponId() != null)
                .filter(o -> ability.id().startsWith(o.weaponId() + "/"));
    }

    /**
     * THE CRYSTAL'S SHOTS (D3; Ben: "the crystal to shoot all enemies within a 8 block range for 20/30/40% of the damage
     * of the right click"; "The orb stays for the entire duration": it is not used up). Every living MOB whose feet are
     * within the shot's radius of the orb's centre (a true sphere, as the shockwave's) and to whose sight point the orb has
     * a clear line, each struck once for {@code beam's authored damage x percent / 100}: the beam's FIRST Damage effect,
     * read from the beam's own ability NOW (never copied into the enchant). SECONDARY damage (B4): its own source
     * {@code <enchant>/crystal}, never a hit (so Kinetic Tremors never counts it), no behaviour, never himself, never a
     * player. It carries every term of the BEAM that set it off, exactly as a shockwave carries its hit's (ruling 2): the
     * beam's gear score, damage enchant, class and level bonus, charge and crit.
     *
     * <p><b>EACH SHOT IS DRAWN</b> (Ben, 2026-11-23: "Draw a line with particles from the crystal to the target with the
     * same particles the right click uses"): {@code beamVisual} is the RAY that met the orb's own {@code beam}, passed in
     * at the moment it fires (never copied into the enchant). One line from the rest point to each struck mob's sight
     * point, and an {@code ORBSHOT} trace line for each; a ray that names no beam draws and traces nothing (the loader
     * warns about such a weapon).
     */
    private void orbShots(AbilityDefinition beam, String beamVisual, Caster caster,
                          io.github.butterflysmp.rpg.core.combat.Orbs.Orb orb) {
        Optional<Double> beamAmount = beam.onHit().stream()
                .filter(e -> e instanceof io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage)
                .map(e -> ((io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage) e).amount())
                .findFirst();
        io.github.butterflysmp.rpg.core.combat.Orbs.Shot shot = orb.shot();
        if (beamAmount.isEmpty()) return;
        double percent = shot.percentByLevel().get(Math.max(1, Math.min(orb.level(), shot.percentByLevel().size())) - 1);
        double amount = beamAmount.get() * percent / 100.0;
        Vec3 centre = orb.restPoint();
        List<Combatant> struck = new java.util.ArrayList<>();
        for (Combatant c : world.combatantsNear(centre, shot.radius())) {
            if (c.id().equals(caster.id())) continue;                     // never himself
            if (c.state().player()) continue;                             // never a player
            if (!c.state().alive()) continue;
            if (c.state().position().distanceSquared(centre) > shot.radius() * shot.radius()) continue;   // the sphere
            if (!world.lineOfSightClear(centre, c.state().sightPoint())) continue;                        // a clear line
            struck.add(c);
        }
        if (beamVisual != null) {
            for (Combatant c : struck) {
                Vec3 to = c.state().sightPoint();
                world.presentAlong(centre, to, beamVisual);
                world.traceOrbShot(caster.id(), centre, to, beamVisual);
            }
        }
        List<io.github.butterflysmp.rpg.core.ability.effect.EffectSpec> specs = new java.util.ArrayList<>();
        if (shot.visual() != null) specs.add(new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Visual(shot.visual()));
        specs.add(new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage(amount, shot.element()));
        // A plain applier, as the shockwave's: secondary damage reports to no direct-damage listener (the melee sweep's).
        new io.github.butterflysmp.rpg.core.ability.effect.EffectApplier(world)
                .applyToSet(specs, caster.asSecondary(orb.enchantId() + "/crystal"), struck, centre, Vec3.ZERO);
    }

    /**
     * THE AIRBURST'S FLIGHT (Ben, 2026-11-23: "It splits into three faster not homing projectiles after 12 blocks of
     * travel", then 14; "The Air Burst Flames projectile goes the 14 blocks and then those projectiles go an additional 14
     * blocks"; "Doesn't split if it hits something first"). The shot is the authored one, homing and all, cut to
     * {@code splitTicks}. A HIT before then lands its authored on_hit and nothing more. Running out of those ticks having
     * hit nothing is {@link ProjectileFlight.Impact#expired}: the split.
     */
    private void launchToSplit(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Projectile spec,
                               io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active,
                               io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst burst, int splitTicks) {
        ProjectileFlight.launch(world, caster, aim.origin(), aim.direction().scale(spec.speed()),
                spec.gravity(), splitTicks,
                new ProjectileFlight.Look(spec.trail(), spec.item(), spec.body()),
                seekOf(spec),
                new ProjectileFlight.Impact() {
                    @Override public void at(Combatant target, Vec3 point) {
                        detonate(ability, caster, target, point);
                    }

                    @Override public void expired(Vec3 point, Vec3 velocity) {
                        split(ability, caster, aim, spec, active, burst, point, velocity);
                    }
                });
    }

    /**
     * The split: {@code count} bursts from {@code point}, fanned about the shot's direction of travel there
     * ({@link io.github.butterflysmp.rpg.core.combat.SplitBasis#sidewaysOf} for "sideways"), each seeking for itself when
     * the enchant declares the bursts' own homing block (Ben, 2026-11-23: "I want the Airburst Flames to seek as well";
     * none, and they fly straight), each landing
     * the ability's on_hit with every Damage replaced by the level's amount in the burst's element. They are PRIMARY hits
     * (the same caster; they count for any behaviour that counts hits), and they never split again: they are launched
     * here, not through {@link #launchOne}.
     */
    private void split(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Projectile spec,
                       io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active,
                       io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst burst, Vec3 point, Vec3 velocity) {
        double amount = burst.damageAt(active.level());
        List<io.github.butterflysmp.rpg.core.ability.effect.EffectSpec> onHit = new java.util.ArrayList<>();
        for (io.github.butterflysmp.rpg.core.ability.effect.EffectSpec effect : ability.onHit()) {
            onHit.add(effect instanceof io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage
                    ? new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage(amount, burst.element())
                    : effect);
        }
        // The burst's damage is the headline now, so a status capped by the payload (scorch) is capped by THIS hit.
        Caster burstCaster = caster.withPayloadDamage(amount);
        Vec3 forward = velocity.lengthSquared() == 0 ? aim.direction() : velocity.normalize();
        Vec3 sideways = io.github.butterflysmp.rpg.core.combat.SplitBasis.sidewaysOf(forward, aim.right());
        List<Vec3> directions = SpreadPattern.directionsFor(forward, sideways,
                new CastSpec.Spread(burst.count(), burst.angleDegrees(), 0));
        int burstTicks = io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst
                .ticksToTravel(burst.burstTravelBlocks(), burst.speed());
        world.traceSplit(caster.id(), caster.source(), point, directions.size());
        // The bursts' OWN homing block (the enchant's), never the shot's: each burst re-chooses the nearest visible live
        // mob every tick (ProjectileFlight.steer), so two may choose the same one. Null: straight, as before.
        ProjectileFlight.Seek seek = burst.homing() == null ? null
                : new ProjectileFlight.Seek(burst.homing().lerp(), burst.homing().activationBlocks(),
                        burst.homing().searchRadius());
        for (Vec3 direction : directions) {
            ProjectileFlight.launch(world, burstCaster, point, direction.scale(burst.speed()), spec.gravity(), burstTicks,
                    new ProjectileFlight.Look(spec.trail(), spec.item(), spec.body()), seek,
                    (target, at) -> effects.applyAll(onHit, burstCaster, target, at));
        }
    }

    /**
     * SHATTER CAPS' STONE (Ben, 2026-11-24: "it shoots 1 stone that splits into 4 cobble after traveling 5 blocks. The
     * damage of the Stone is 35/60/85"; "follow with gearscore"). ONE body along the aim at the enchant's speed, its
     * item, the weapon's trail, no homing, the weapon's gravity, flying {@code caps.splitTicks()}. Its hit is the
     * weapon's own on_hit with every damage replaced by the level's amount in the enchant's element, through the same
     * applier as every hit (so the gear score scales it, the knockback and the hit visual ride as authored, and it
     * counts as one primary hit). A BLOCK on its path ends it as it ends the weapon's chunk: {@code at(null, point)},
     * the untargeted effects (the hit visual) where it stopped, no damage, no split (READ, {@code ProjectileFlight}).
     * Running out of its ticks having hit nothing is the split ({@link #shatter}).
     */
    private void launchStone(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Projectile spec,
                             io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active,
                             io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Shatter caps) {
        double amount = caps.damageAt(active.level());
        List<io.github.butterflysmp.rpg.core.ability.effect.EffectSpec> onHit = new java.util.ArrayList<>();
        for (io.github.butterflysmp.rpg.core.ability.effect.EffectSpec effect : ability.onHit()) {
            onHit.add(effect instanceof io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage
                    ? new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage(amount, caps.element())
                    : effect);
        }
        Caster stoneCaster = caster.withPayloadDamage(amount);
        ProjectileFlight.launch(world, stoneCaster, aim.origin(), aim.direction().scale(caps.speed()), spec.gravity(),
                caps.splitTicks(), new ProjectileFlight.Look(spec.trail(), caps.item(), null), null,
                new ProjectileFlight.Impact() {
                    @Override public void at(Combatant target, Vec3 point) {
                        effects.applyAll(onHit, stoneCaster, target, point);
                    }

                    @Override public void expired(Vec3 point, Vec3 velocity) {
                        shatter(ability, caster, aim, spec, point, velocity);
                    }
                });
    }

    /**
     * THE SHATTER: the weapon's OWN cast leaves {@code point} (Ben: "the usual 40", "their full reach", "the same
     * pattern"). Its spread is the weapon's, READ from {@code spec} now (never copied into the enchant), built about the
     * stone's direction of travel with THE PRESS'S OWN RIGHT VECTOR ({@code aim.right()}, the shooter's, which the
     * {@link Aim} carried to here), so the basis is the one the press would have used: rotation 90 still puts the
     * triangle's point up, and straight up it is as well defined as at a press. Each chunk is the weapon's shot
     * ({@link #launchOne}): its speed, item, trail, homing, on_hit and full lifetime, as the original caster.
     */
    private void shatter(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Projectile spec, Vec3 point,
                         Vec3 velocity) {
        Vec3 forward = velocity.lengthSquared() == 0 ? aim.direction() : velocity.normalize();
        List<Vec3> directions = SpreadPattern.directionsFor(forward, aim.right(), spec.spread());
        world.traceSplit(caster.id(), caster.source(), point, directions.size());
        for (Vec3 direction : directions) {
            launchOne(ability, caster, new Aim(point, direction, aim.right()), spec);
        }
    }

    /**
     * The content's homing block in the flight's own vocabulary, or null for a bolt that flies where
     * it was aimed.
     *
     * <p><b>THE SAME MAPPING {@code Look} ALREADY GETS, AND FOR THE SAME REASON.</b>
     * {@code core.combat} imports nothing from {@code core.ability}, so the flight cannot take a
     * {@code CastSpec} type and this call site is where the two vocabularies meet. Null in, null
     * out: a projectile with no homing block is byte-identical to what it was before this field
     * existed.
     */
    private static ProjectileFlight.Seek seekOf(CastSpec.Projectile spec) {
        CastSpec.Homing homing = spec.homing();
        return homing == null ? null
                : new ProjectileFlight.Seek(homing.lerp(), homing.activationBlocks(),
                        homing.searchRadius());
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
     * <p><b>ADOPTED 2026-09-10</b>, on the gate. It began PROVISIONAL -- {@code cfde822}'s number
     * for {@code cfde822}'s geometry -- with {@code GATE-beam-gap.md} named as the authority that
     * would rule it. The gate ran, the operator ruled: <i>"it's much better."</i> An adopted value
     * changed is a DECISION; a provisional one changed is HOUSEKEEPING, which is why the label
     * moves rather than being left to disclaim something already settled.
     *
     * <p><b>AND THE EVIDENCE CLASS IS NAMED, BECAUSE "1.0, ADOPTED" WITH NO SOURCE IS A MAGIC
     * NUMBER.</b> This was adopted on <b>the operator's ruling from a boot ANSWERED BY BLANKET</b>
     * -- two sentences covering the whole gate -- <b>not on a per-setting measurement.</b> G5a's
     * design asked for BETTER/SAME/WORSE on {@code All} and {@code Decreased} separately, and those
     * figures were never taken. So: the direction is ruled and solid; the MAGNITUDE has never been
     * measured against an alternative. Nobody has compared 1.0 to 0.5 or 1.5. A future retune has a
     * ruling to argue with, not a number.
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

    private void launchRay(AbilityDefinition ability, Caster caster, Aim aim, CastSpec.Ray ray) {
        String beam = ray.beam();
        List<Vec3> endpoints = ChunkTraversal.segmentEndpoints(aim.origin(), aim.direction(), ray.range());
        if (ray.sweeps()) {
            // The Knell Beam's ray (pierce or tolerance, 2026-11-21): its own walk, below. Every other ray goes on to
            // stepRay, unchanged: the first body or block ends it, on castRay.
            stepSweep(ability, caster, ray, aim.origin(), aim.pointAt(BEAM_ORIGIN_GAP), endpoints, 0, Set.of());
            return;
        }
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
        // LAPIS CRYSTAL'S SEAM (D3, 2026-11-23): his own staff's beam that passes his resting orb BEFORE it reaches what it
        // struck stops AT the orb, and the orb fires (orbShots) instead of the beam detonating. See meetsOrb.
        Optional<io.github.butterflysmp.rpg.core.combat.Orbs.Orb> orb = meetsOrb(ability, caster);
        Optional<Vec3> atOrb = orb.flatMap(o -> io.github.butterflysmp.rpg.core.combat.OrbIntercept.along(from,
                hit.map(RayHit::point).orElse(to), o.restPoint(), o.radius()));

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
        Vec3 beamEnd = atOrb.orElse(hit.map(RayHit::point).orElse(to));
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

        if (atOrb.isPresent()) {
            orbShots(ability, beam, caster, orb.get());
            return;
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
     * One chunk column of a PIERCING or TOLERANT ray (the seat's brief of 2026-11-21, the Knell Beam, rulings 1 and 2).
     * Mirrors {@link #stepRay}: trace, act, or hand the next segment to the region that owns it.
     *
     * <ul>
     *   <li>The segment is clipped at the first block; nothing beyond it is struck.</li>
     *   <li>Who the clipped line strikes is {@link RaySweep#struck}: the Knell mob's {@code BeamLine.meets} on each
     *       body's box grown by the tolerance, nearest first. A body struck only in its MARGIN (not on its box) is
     *       refused unless the line can see it ({@code lineOfSightClear} from the struck point to the body's sight
     *       point), so a tolerance never reaches round a wall.</li>
     *   <li>{@code struck} is the set of ids already struck, carried across chunk-column hops as VALUES (ruling 7):
     *       a body met by two segments takes the {@code on_hit} once.</li>
     *   <li>Without {@code pierce} the first body struck ends the walk, as a plain ray's does. With it, the walk goes
     *       on to the range or the first block.</li>
     *   <li>The walk detonates at its END (the block or the range) only if it struck nobody, as a plain ray detonates
     *       once: a miss still lands, and a hit is not followed by a second, empty landing.</li>
     * </ul>
     */
    private void stepSweep(AbilityDefinition ability, Caster caster, CastSpec.Ray ray, Vec3 from, Vec3 beamStart,
                           List<Vec3> endpoints, int index, Set<UUID> struck) {
        Vec3 to = endpoints.get(index);
        RaySweep sweep = world.sweepRay(from, to, caster.id(), ray.tolerance());
        Vec3 segmentEnd = sweep.blockAt().orElse(to);

        Set<UUID> struckNow = new HashSet<>(struck);
        Vec3 drawTo = segmentEnd;
        boolean stopped = sweep.blockAt().isPresent();
        double length = segmentEnd.subtract(from).length();
        if (length > 0) {
            BeamLine line = new BeamLine(from, segmentEnd.subtract(from).normalize(), length);
            for (RaySweep.Struck s : RaySweep.struck(line, ray.tolerance(), sweep.bodies())) {
                UUID id = s.combatant().id();
                if (struckNow.contains(id)) continue;
                if (!s.exact() && !world.lineOfSightClear(s.point(), s.combatant().state().sightPoint())) continue;
                struckNow.add(id);
                detonate(ability, caster, s.combatant(), s.point());
                if (!ray.pierce()) {
                    drawTo = s.point();
                    stopped = true;
                    break;
                }
            }
        }

        Vec3 alongSegment = to.subtract(from);
        boolean gapEndsAhead = beamStart.subtract(from).dot(alongSegment) > 0;
        Vec3 drawFrom = gapEndsAhead ? beamStart : from;
        if (ray.beam() != null) {
            if (drawTo.subtract(drawFrom).dot(alongSegment) > 0) world.presentAlong(drawFrom, drawTo, ray.beam());
            else world.presentAlong(from, from, ray.beam());
        }

        boolean lastSegment = index == endpoints.size() - 1;
        if (stopped || lastSegment) {
            if (struckNow.isEmpty()) detonate(ability, caster, null, segmentEnd);
            return;
        }
        Set<UUID> carried = Set.copyOf(struckNow);
        world.schedule(to, 1, () -> stepSweep(ability, caster, ray, to, beamStart, endpoints, index + 1, carried));
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
        UUID casterId = caster.id();

        Combatant nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;

        for (Combatant candidate : world.combatantsNear(aim.origin(), melee.reach())) {
            if (candidate.id().equals(casterId)) continue;

            // The angle test is Aim.withinArc, shared with the cone cast (2026-11-22): the swing tests the FEET.
            if (!aim.withinArc(candidate.state().position(), melee.arcDegrees())) continue;

            Vec3 toCandidate = candidate.state().position().subtract(aim.origin());
            double distanceSquared = toCandidate.lengthSquared();
            if (distanceSquared >= nearestDistanceSquared) continue;
            if (!world.lineOfSightClear(aim.origin(), candidate.state().sightPoint())) continue;

            nearestDistanceSquared = distanceSquared;
            nearest = candidate;
        }
        return nearest;
    }

    // sightPoint MOVED to CombatantSnapshot.sightPoint(), where ProjectileFlight's homing sight
    // gate can reach it too -- core.combat cannot see into core.ability. One definition, because a
    // second opinion about WHERE ON A TARGET you sight to is a disagreement between the sweep and
    // the bolt about whether the same mob is visible.


    /**
     * Start a volley: fire shot 1 after the wind-up, or inline if there is none.
     *
     * <p>{@code windupTicks == 0} runs the first shot on the cast frame rather than a tick later,
     * exactly as {@link #launchRay} runs its first chunk-column segment inline and for the same
     * reason -- the scheduler cannot defer by less than a tick, so deferring a zero wind-up would
     * quietly make it 1 and a "no wind-up" volley would be indistinguishable from a 1-tick one.
     */
    private void beginVolley(AbilityDefinition ability, UUID casterId, CastSpec.Volley spec, boolean weaponHit,
                             Aim castAim, long pressTick) {
        // LOCKED AIM (2026-11-21, the Knell Beam, ruling 3): the line captured HERE, at the cast, as values -- an origin
        // and a direction, never a handle (ruling 7). Null for a volley that re-aims every shot, as every volley did.
        Aim locked = spec.lockedAim() ? castAim : null;
        // The press's tick, carried as a value for the FOCUSED trace only (Focused Beam, 2026-11-24): nothing decides on it.
        // STAMPED AT THE PRESS, before WeaponFire's hop, and handed in (section 19): read here it was the CAST's tick,
        // one after the press on the server, which is why every FOCUSED line read tick - pressed = 19.
        if (spec.warning() != null) warn(spec, locked, casterId);
        if (spec.windupTicks() == 0) {
            volley(ability, casterId, spec, 0, weaponHit, locked, pressTick);
            return;
        }
        world.scheduleOn(casterId, spec.windupTicks(),
                () -> volley(ability, casterId, spec, 0, weaponHit, locked, pressTick));
    }

    /**
     * THE WARNING LINE (2026-11-21, the Knell Beam, ruling 4): the warning beam visual drawn along the LOCKED line, on
     * each windup tick in {@code flash_at} (0 now, on the cast frame). The volley's constructor guarantees a locked aim
     * and a ray to take the length from.
     *
     * <p><b>NOT CLIPPED AT BLOCKS, AS THE KNELL MOB'S IS NOT</b> (READ: {@code MobBeamer} draws its warning on the
     * locked full line and clips only the fired beam). Drawn per chunk-column segment, each {@code presentAlong} hopping
     * to its own region, as a ray's beam is drawn. A flash is skipped if the caster is gone by its tick.
     */
    private void warn(CastSpec.Volley spec, Aim locked, UUID casterId) {
        CastSpec.Ray ray = (CastSpec.Ray) spec.of();
        List<Vec3> endpoints = ChunkTraversal.segmentEndpoints(locked.origin(), locked.direction(), ray.range());
        Runnable draw = () -> {
            Vec3 from = locked.pointAt(BEAM_ORIGIN_GAP);
            for (Vec3 to : endpoints) {
                if (to.subtract(locked.origin()).length() <= BEAM_ORIGIN_GAP) continue;
                world.presentAlong(from, to, spec.warning().beam());
                from = to;
            }
        };
        for (int tick : spec.warning().flashAt()) {
            if (tick == 0) draw.run();
            else world.scheduleOn(casterId, tick, draw);
        }
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
    private void volley(AbilityDefinition ability, UUID casterId, CastSpec.Volley spec, int shotIndex,
                        boolean weaponHit, Aim locked, long pressTick) {
        Combatant self = world.combatant(casterId).orElse(null);
        if (self == null || !self.state().alive()) return;   // gone, or dead: the volley stops
        // A LOCKED volley fires down the line captured at the cast, wherever the caster now stands or looks. An
        // unlocked one re-reads the live aim, and an unreadable aim stops it, as before.
        Aim live = locked != null ? locked : world.aimOf(casterId).orElse(null);
        if (live == null) return;                            // same condition, same answer

        // *** THIS IS WHERE A VOLLEY RE-PRICES, AND IT IS THIS LOOP THAT DOES IT -- NOT A LIVE FIELD.
        // *** Each shot rebuilds the WHOLE Caster from a fresh snapshot, an aim read this tick and a
        // gear score read this tick, so a player who swaps weapons mid-burst has the remaining shots
        // priced off whatever is now in hand. Every component of the Caster is still FROZEN once
        // built; what moves is that there is a new Caster per shot.
        //
        // INHERITED, NOT INTRODUCED. cursed_emerald.yml already documents exactly this for
        // enchantDamagePercent, classDamageBonus, chargeScale and critMultiplier -- "a player who
        // swaps weapons mid-burst has shots 4-6 priced off whatever is in hand at that tick". The
        // gear score joins a chain that already behaves this way. It is a volley-wrapper question and
        // it is UNRULED; do not "fix" it here.
        Caster source = Caster.of(self.state())
                .withPayloadDamage(DamagePayload.headlineDamage(ability.onHit(), self.state().attackDamage()))
                .withTriggerScore(world.triggerScoreOf(casterId))
                .withSource(ability.id());
        // The flag is NOT re-read per shot -- there is nothing to re-read it from. It is how the volley
        // was PRESSED (a weapon trigger or a stone), carried down from commit's Caster by dispatch.
        // B2: a weapon-pressed volley reads the held weapon's behaviours per shot, exactly as it re-reads the gear score per
        // shot above (the inherited, UNRULED volley re-pricing; flagged in PLAN-enchant-columns.md, not decided here).
        if (weaponHit) source = source.withWeaponHit().withBehaviours(world.behavioursOf(casterId));

        // THE CAST TUNES OF THIS SHOT (G4, 2026-11-24): `ability` is the weapon's own, re-tuned here off the behaviours
        // this shot just read (ruling 1), so a weapon swapped in or out mid-volley moves this shot's damage and range and
        // the count the volley runs to. The wind-up was the press's (beginVolley).
        AbilityDefinition shot = CastTunes.tune(ability, source.behaviours());
        if (shot != ability) {
            source = source.withPayloadDamage(DamagePayload.headlineDamage(shot.onHit(), self.state().attackDamage()));
        }
        CastSpec.Volley shotSpec = shot.cast() instanceof CastSpec.Volley v ? v : spec;
        // VOLLEYSHOT (the Early Toll row, 2026-11-24): this shot's tick beside the press's, under mobtrace.
        world.traceVolleyShot(casterId, ability.id(), shotIndex, pressTick);

        // FOCUSED BEAM (2026-11-24): at the FIRST shot, which is the wind-up's end, a focused beam active on the weapon
        // that pressed this volley (its ability id is <weapon>/...; the swap case is EnchantBehaviour.FocusedBeam's
        // javadoc) fires ONE ray in place of every shot, and no further shot is scheduled. The behaviours, the aim and
        // the Caster are the ones this shot just read, so the beam is aimed and priced exactly as the first bolt would be.
        if (shotIndex == 0 && shotSpec.of() instanceof CastSpec.Ray ray) {
            for (io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active : source.behaviours()) {
                if (active.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.FocusedBeam beam
                        && ability.id().startsWith(beam.weaponId() + "/")) {
                    focusedBeam(shot, source, live, ray, active, beam, pressTick);
                    return;
                }
            }
        }

        fireInner(shot, source, live, shotSpec.of());

        if (shotIndex + 1 >= shotSpec.shots()) return;
        world.scheduleOn(casterId, shotSpec.intervalTicks(),
                () -> volley(ability, casterId, spec, shotIndex + 1, weaponHit, locked, pressTick));
    }

    /**
     * FOCUSED BEAM'S ONE RAY (Ben, 2026-11-24: "Fires one stronger beam after the wind up." / "108/162/216" / "No piece" /
     * "increase the range to 48 blocks" / "Yes", where he looks when the wind-up ends). The volley's OWN inner ray, read
     * here from the weapon's spec and never copied: its beam visual and tolerance as authored, its range replaced by the
     * enchant's, and pierce OFF (Ben: no pierce; it stops at the first body, as every bolt does). Its hit is the weapon's
     * own on_hit with every damage replaced by the level's amount in the enchant's element, landed by {@link #launchRay}
     * on a derived ability that keeps the weapon's id (so the PLAYERHIT source is the weapon's), through the same applier
     * as every hit: the gear score scales it, the hit visual rides as authored, and it is one primary hit.
     */
    private void focusedBeam(AbilityDefinition ability, Caster source, Aim aim, CastSpec.Ray ray,
                             io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour active,
                             io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.FocusedBeam beam, long pressTick) {
        double amount = beam.damageAt(active.level());
        List<io.github.butterflysmp.rpg.core.ability.effect.EffectSpec> onHit = new java.util.ArrayList<>();
        for (io.github.butterflysmp.rpg.core.ability.effect.EffectSpec effect : ability.onHit()) {
            onHit.add(effect instanceof io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage
                    ? new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage(amount, beam.element())
                    : effect);
        }
        AbilityDefinition focused = new AbilityDefinition(ability.id(), ability.displayName(), ability.element(),
                ability.cooldownTicks(), ability.cost(), ability.cast(), onHit, ability.description(), ability.onCast());
        world.traceFocusedBeam(source.id(), ability.id(), aim.origin(), beam.range(), pressTick);
        launchRay(focused, source.withPayloadDamage(amount), aim,
                new CastSpec.Ray(beam.range(), ray.beam(), false, ray.tolerance()));
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
            case CastSpec.Ray ray -> launchRay(ability, source, aim, ray);
            case CastSpec.Projectile projectile -> launch(ability, source, aim, projectile);
            case CastSpec.Self ignored -> throw new IllegalStateException(notRepeatable("self"));
            case CastSpec.Melee ignored -> throw new IllegalStateException(notRepeatable("melee"));
            case CastSpec.Vanilla ignored -> throw new IllegalStateException(notRepeatable("vanilla"));
            case CastSpec.Dash ignored -> throw new IllegalStateException(notRepeatable("dash"));
            case CastSpec.Volley ignored -> throw new IllegalStateException(notRepeatable("volley"));
            case CastSpec.Cone ignored -> throw new IllegalStateException(notRepeatable("cone"));
            case CastSpec.Orb ignored -> throw new IllegalStateException(notRepeatable("orb"));
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
