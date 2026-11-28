package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.BeamSamples;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.paper.weapon.GearScoreItems;
import io.github.butterflysmp.rpg.paper.weapon.ViewAim;
import io.github.butterflysmp.rpg.paper.content.VisualDefinition;
import io.github.butterflysmp.rpg.paper.content.VisualSpec;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class PaperCombatWorld implements CombatWorld {

    /** How much to inflate entity hitboxes when tracing. 0 = exact bounding box. */
    private static final double RAY_SIZE = 0.0;

    /**
     * How far ABOVE the caster's feet to release a thrown marker item, so it leaves from about
     * hand/eye height rather than the ground -- the old Blast Fungus threw from eye level and
     * kept the item moving, which is why it never rested inside a block and never popped. A
     * Y-only lift: X/Z stay the throw origin, so the item keeps the caster's column.
     */
    private static final double THROW_ORIGIN_LIFT = 1.4;

    private final World world;
    private final AdapterContext ctx;

    public PaperCombatWorld(World world, AdapterContext ctx) {
        this.world = world;
        this.ctx = ctx;
    }

    private Location toLocation(Vec3 v) {
        return new Location(world, v.x(), v.y(), v.z());
    }

    /**
     * MUST run on the thread that owns {@code center}'s region.
     * World#getNearbyEntities is illegal anywhere else.
     *
     * Three entry points reach here. Only one of them provably satisfies that:
     *
     *   - Rescheduled area pulses, via EffectApplier.tickArea -> schedule() ->
     *     onRegionLater(origin, ...). Correct: the hop names the area's own origin.
     *
     *   - EffectApplier's inline Burst, and CastExecutor.meleeTarget. Both run on
     *     whatever thread CastExecutor.execute was called on, which RpgCommand sets
     *     to the region owning the caster's EYE -- not the burst's origin.
     *
     * For Melee the eye and the target are within a few blocks, so they share a
     * region in practice. For a Burst at the far end of a 30-block Ray they need
     * not. This method is therefore called, today, on a thread that may not own
     * {@code center}.
     *
     * Do not read this as permission. It is a Folia-only defect: on Paper every
     * region scheduler runs on the main thread, so no test and no local server can
     * reproduce it. See NEXT.md, Commit C -- and the javadoc on present(), which
     * hops correctly and explains why.
     */
    @Override
    public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        return world.getNearbyEntities(toLocation(center), radius, radius, radius).stream()
                // Live bodies only (LiveTargets): melee, the dash's swept line, bursts and homing all pick from here,
                // and a mob in its death animation must not take a hit meant for a live one.
                // A part (an Ender Dragon's) stands for its dragon (step F9); a dragon found by its box AND its parts
                // is one combatant, not several.
                .map(LiveTargets::resolve)
                .filter(java.util.Objects::nonNull)
                .filter(onePerBody())
                .map(e -> BukkitCombatant.of(e, ctx)) // snapshot taken here, on this thread
                .toList();
    }

    /** A filter that passes each body once: the first time its UUID is seen. One per search; not thread-safe. */
    static java.util.function.Predicate<LivingEntity> onePerBody() {
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        return e -> seen.add(e.getUniqueId());
    }

    /**
     * Fetches the caster's handle for a Self cast, whose Success carries only a snapshot.
     * Null-safe by way of Optional: the caster may have died or logged out between deciding
     * the cast and resolving it.
     */
    @Override
    public Optional<Combatant> combatant(UUID id) {
        if (world.getEntity(id) instanceof LivingEntity living) {
            return Optional.of(BukkitCombatant.of(living, ctx));
        }
        return Optional.empty();
    }

    /**
     * MUST run on the thread owning every region the segment touches -- World#rayTrace
     * reads blocks and entities along its whole length, not just at its ends.
     *
     * A projectile's segment is one tick of flight, a block or two, so it lies inside
     * one region and CastExecutor.step re-enters the correct one each tick. A Ray's
     * segment is its entire range -- CastSpec.Ray defaults to 30 blocks -- and no
     * single thread owns all of it. That call is the Folia defect on combatantsNear.
     *
     * One trace covers blocks and entities together, so a grenade cannot pass
     * through a wall to reach someone standing behind it.
     */
    @Override
    public Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId,
                                    io.github.butterflysmp.rpg.core.mob.MobHostility.Side attacker) {
        Vec3 along = to.subtract(from);
        double distance = along.length();
        if (distance <= 0) return Optional.empty();

        Vector direction = new Vector(along.x(), along.y(), along.z()).normalize();
        RayTraceResult result = world.rayTrace(
                toLocation(from), direction, distance,
                FluidCollisionMode.NEVER, /* ignorePassableBlocks */ true, RAY_SIZE,
                // Live bodies only: a corpse on the line of fire ended the flight and dealt nothing (LiveTargets).
                // Q3: and PASSES THROUGH a body the one table refuses to this attacker (a summon, to a player).
                LiveTargets.except(ignoreId).and(e -> {
                    org.bukkit.entity.LivingEntity body = LiveTargets.resolve(e);
                    if (body == null || !io.github.butterflysmp.rpg.core.mob.MobHostility.refusedSummonCell(attacker,
                            CombatSides.of(body, ctx.keys()))) return true;
                    // X4 (the seat, 2026-11-25): ONE line when the table lets the ray through a body ON its line. READ,
                    // CraftWorld.rayTraceEntities: this predicate runs inside getNearbyEntities(sweptBox, ...) BEFORE
                    // each body's own box test, so the same test (box expanded by RAY_SIZE, rayTrace) gates the line.
                    if (ctx.mobTrace().getAsBoolean() && e.getBoundingBox().expand(RAY_SIZE)
                            .rayTrace(toLocation(from).toVector(), direction, distance) != null) {
                        ctx.log().info(sidesPassLine(attacker, body.getUniqueId()));
                    }
                    return false;
                }));

        if (result == null) return Optional.empty();

        Vector hit = result.getHitPosition();
        Vec3 point = new Vec3(hit.getX(), hit.getY(), hit.getZ());

        // A ray that stops on a part (an Ender Dragon's head, wing or tail) hits the dragon (step F9; LiveTargets.body).
        LivingEntity living = LiveTargets.resolve(result.getHitEntity());
        if (living != null) {
            return Optional.of(RayHit.ofCombatant(point, BukkitCombatant.of(living, ctx)));
        }
        return Optional.of(RayHit.ofBlock(point));
    }

    /** X4: the trace line for a body the sides table let a ray pass through, on the ray's line. Pinned by its test. */
    static String sidesPassLine(io.github.butterflysmp.rpg.core.mob.MobHostility.Side attacker, UUID body) {
        return "SIDES pass attacker=" + attacker + " body=" + body;
    }

    /**
     * The piercing / tolerant ray's segment (2026-11-21, the Knell Beam, rulings 1, 2, 5, 6): every live body near it,
     * each with its own bounding box, and where it first meets a block. Judged in core ({@code RaySweep.struck}), on the
     * Knell mob's geometry.
     *
     * <p><b>THE SAME TWO FILTERS AS {@link #castRay}, ON PURPOSE (READ):</b>
     * <ul>
     *   <li>blocks: {@code rayTraceBlocks} with {@code FluidCollisionMode.NEVER} and passable blocks ignored, the
     *       configuration {@code castRay}'s one trace and the Knell mob's {@code fire} both use;</li>
     *   <li>bodies: {@link LiveTargets#except}, live living bodies only, the caster excluded by id, a part (a dragon's
     *       head or wing) resolved to its body and reported once. <b>So an END CRYSTAL is never a body</b> (it is not
     *       a living entity), and the beam passes through one and does nothing to it, as a plain ray does. <b>Another
     *       player IS a body</b>, as for {@code castRay}; <b>the caster never is</b>, so a player walking into his own
     *       locked beam's line is not struck. None of that is changed here.</li>
     * </ul>
     *
     * <p>Only legal on the thread owning the segment's region, exactly as {@code castRay}: the chunk-column walk keeps
     * each segment inside one region and hops between them.
     */
    @Override
    public io.github.butterflysmp.rpg.core.combat.RaySweep sweepRay(Vec3 from, Vec3 to, UUID ignoreId, double reach) {
        Vec3 along = to.subtract(from);
        double distance = along.length();
        if (distance <= 0) return new io.github.butterflysmp.rpg.core.combat.RaySweep(java.util.List.of(), Optional.empty());

        Vector direction = new Vector(along.x(), along.y(), along.z()).normalize();
        RayTraceResult blocks = world.rayTraceBlocks(toLocation(from), direction, distance,
                FluidCollisionMode.NEVER, /* ignorePassableBlocks */ true);
        Optional<Vec3> blockAt = blocks == null ? Optional.empty()
                : Optional.of(new Vec3(blocks.getHitPosition().getX(), blocks.getHitPosition().getY(),
                        blocks.getHitPosition().getZ()));

        org.bukkit.util.BoundingBox around = new org.bukkit.util.BoundingBox(from.x(), from.y(), from.z(),
                to.x(), to.y(), to.z()).expand(reach + 1.0);
        java.util.function.Predicate<Entity> live = LiveTargets.except(ignoreId);
        java.util.Map<UUID, io.github.butterflysmp.rpg.core.combat.RaySweep.Body> bodies = new java.util.LinkedHashMap<>();
        for (Entity e : world.getNearbyEntities(around)) {
            if (!live.test(e)) continue;
            LivingEntity body = LiveTargets.resolve(e);
            if (body == null || bodies.containsKey(body.getUniqueId())) continue;
            org.bukkit.util.BoundingBox b = body.getBoundingBox();
            bodies.put(body.getUniqueId(), new io.github.butterflysmp.rpg.core.combat.RaySweep.Body(
                    BukkitCombatant.of(body, ctx),
                    new io.github.butterflysmp.rpg.core.mob.BeamBox(new Vec3(b.getMinX(), b.getMinY(), b.getMinZ()),
                            new Vec3(b.getMaxX(), b.getMaxY(), b.getMaxZ()))));
        }
        return new io.github.butterflysmp.rpg.core.combat.RaySweep(new java.util.ArrayList<>(bodies.values()), blockAt);
    }

    /**
     * MUST run on the thread owning the segment's region, like castRay -- World#rayTraceBlocks
     * reads blocks along the whole length, and the javadoc warns it may load chunks to do it.
     *
     * This is the one world read on the melee path that is genuinely safe under that rule today.
     * Its only caller traces at most a melee reach (3 to 3.5 in shipped content) from the caster's
     * eye, so the segment cannot leave the region CastExecutor was already entered on. Contrast
     * castRay's 30-block Ray, and the defect documented on combatantsNear above.
     *
     * Blocks ONLY -- deliberately rayTraceBlocks and not rayTrace. rayTrace traces blocks, then
     * traces entities out to the block hit and returns whichever is nearer, so it would report a
     * mob standing behind another mob as blocked. Melee wants what vanilla wants: you may hit a
     * mob through a mob, but not through a wall.
     *
     * FluidCollisionMode.NEVER with ignorePassableBlocks = true is exactly the configuration
     * vanilla's own LivingEntity#hasLineOfSight uses, so grass, water and other passable blocks
     * do not stop a swing.
     */
    @Override
    public boolean lineOfSightClear(Vec3 from, Vec3 to) {
        Vec3 along = to.subtract(from);
        double distance = along.length();
        // Clear, NOT blocked. castRay's mirror of this guard returns "nothing hit" for a
        // degenerate segment and that is the same verdict, spelled with the opposite boolean.
        // It also has to run: Vector#normalize on a zero vector yields NaN, and rayTraceBlocks
        // precondition-checks the direction, so an unguarded call throws rather than missing.
        if (distance <= 0) return true;

        Vector direction = new Vector(along.x(), along.y(), along.z()).normalize();
        return world.rayTraceBlocks(
                toLocation(from), direction, distance,
                FluidCollisionMode.NEVER, /* ignorePassableBlocks */ true) == null;
    }

    @Override
    public void schedule(Vec3 near, int delayTicks, Runnable task) {
        // Registered with its point, so a clean can end it there (step R7b, C2; PendingWork). It runs only if it is
        // still pending when its delay ends.
        scheduleCancellable(near, delayTicks, task);
    }

    /**
     * {@link #schedule}, returning its {@link PendingWork} id as the handle (2026-11-23, the seat's item 6). Cancelling
     * takes the id out of the pending set, so the task finds itself gone when its delay ends and does not run: the same
     * mechanism the selftest's row-start clean already uses ({@code cancelNear}).
     */
    @Override
    public long scheduleCancellable(Vec3 near, int delayTicks, Runnable task) {
        long id = ctx.work().add(world.getUID(), near.x(), near.y(), near.z());
        ctx.scheduler().onRegionLater(toLocation(near), () -> {
            if (ctx.work().take(id)) task.run();
        }, delayTicks);
        return id;
    }

    @Override
    public void cancelScheduled(long handle) {
        ctx.work().take(handle);
    }

    /**
     * The entity twin of {@link #schedule}: deferred work on the thread that owns the BODY, not
     * the thread that owns a point someone guessed the body would still be at.
     *
     * <p>A no-op if the combatant is not in this world -- which includes a player who has changed
     * world mid-volley, and that is the correct answer rather than a gap: their region is not ours
     * to schedule into. {@code onEntityLater} passes a null retired-runnable, so an entity that
     * goes between now and the delay simply never runs the task.
     */
    @Override
    public void scheduleOn(UUID combatantId, int delayTicks, Runnable task) {
        Entity entity = world.getEntity(combatantId);
        if (entity == null) return;
        ctx.scheduler().onEntityLater(entity, task, delayTicks);
    }

    /**
     * {@link #scheduleOn}, registered in {@link PendingWork} at {@code lastKnown} (Smolder's next pulse, the seat's Z6,
     * 2026-11-27): it runs on the entity's own scheduler, and only if its link is still pending when the delay ends, so
     * {@link #cancelScheduled}, a removal's {@code work().take} and the pad clean's {@code cancelIn} all end it. Nothing is
     * registered when the entity is not in this world ({@code -1}). A link whose entity goes (a quit) never runs; its
     * entry is taken by the removal path ({@code PulseRemovals}) or the next pad clean.
     */
    @Override
    public long scheduleOnCancellable(UUID combatantId, io.github.butterflysmp.rpg.core.Vec3 lastKnown, int delayTicks,
                                      Runnable task) {
        Entity entity = world.getEntity(combatantId);
        if (entity == null) return -1;
        long id = ctx.work().add(world.getUID(), lastKnown.x(), lastKnown.y(), lastKnown.z());
        ctx.scheduler().onEntityLater(entity, () -> {
            if (ctx.work().take(id)) task.run();
        }, delayTicks);
        return id;
    }

    @Override
    public io.github.butterflysmp.rpg.core.combat.PulseRuns pulseRuns() {
        return ctx.pulseRuns();
    }

    /** A pulse line (Z10), while {@code /rpg mobtrace} is on: the target's entity type (or {@code gone}) and the tick. */
    @Override
    public void tracePulse(io.github.butterflysmp.rpg.core.combat.PulseLine line) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        String type = "none";
        if (line.target() != null) {
            Entity target = world.getEntity(line.target());
            type = target == null ? "gone" : target.getType().getKey().getKey();
        }
        ctx.log().info(line.format(type, org.bukkit.Bukkit.getCurrentTick()));
    }

    /**
     * AN ABILITY'S IMPLOSION THAT DRAGS (2026-11-18): {@link ImplosionDrags}, the runner the Phage's blast uses, on the
     * origin's region. The use's predicate, as {@code BukkitCombatant#applyPull} applies it to a single pull: never the
     * caster, never a shot's carrier, never a creative or spectator player; {@code MOBS} every other non-player,
     * {@code PLAYERS} every other player.
     */
    @Override
    public void startDrag(Vec3 origin, io.github.butterflysmp.rpg.core.shape.Implosion shape,
                          io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Affects affects, UUID casterId) {
        boolean players = affects == io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Affects.PLAYERS;
        ctx.scheduler().onRegion(toLocation(origin), () -> ImplosionDrags.start(ctx, world, origin, shape,
                l -> !l.getUniqueId().equals(casterId) && !l.getPersistentDataContainer().has(ctx.keys().mobShot)
                        && !ImplosionFooting.exempt(l) && (l instanceof Player) == players,
                "ability", casterId));
    }

    /**
     * The live eye and look direction, read HERE, on the thread that owns the entity -- enforced by
     * {@link Regions#requireOwned}, exactly as {@code BukkitCombatant.snapshot} is.
     *
     * <p>This is the same pair {@code WeaponFire} builds on the press frame
     * ({@code getEyeLocation()} and its direction); the difference is only WHEN. A caster who has
     * turned since the cast frame gets the line they are looking down now.
     */
    @Override
    public Optional<Aim> aimOf(UUID combatantId) {
        if (!(world.getEntity(combatantId) instanceof LivingEntity living)) return Optional.empty();
        Regions.requireOwned(living);
        // Through ViewAim, like every other production aim, so the live re-read carries the
        // shooter's RIGHT as well as their direction. A volley of a spread re-aims per shot, and
        // rebuilding the pair by hand here would have handed those shots a basis derived from the
        // direction -- correct at every pitch but the two that matter.
        return Optional.of(ViewAim.of(living.getEyeLocation()));
    }

    /**
     * The held weapon's gear score. {@link GearScore#BASELINE} for anything that is not a player --
     * the PORT's stated total-function answer, quoted here rather than invented here.
     *
     * <p><b>A NON-PLAYER IS EVERY MOB IN THE GAME, and BASELINE is the answer that leaves them
     * dealing exactly their authored damage.</b> Getting it backwards -- a 0 that scaled DOWN rather
     * than clamping back to the baseline -- would quietly reduce every hit every mob in the game
     * lands, and nothing in any suite would go red. This is also the line where the decision gets
     * revisited the day mob gear arrives.
     *
     * <p>A READ on the owning thread, like {@link #aimOf} above.
     *
     * <h2>THE {@code volley_stone} (deleted 2026-11) EXCLUSION IS WIRED, AND IT IS WIRED INSIDE {@code heldScore}</h2>
     *
     * <p>{@code heldScore} resolves the held weapon's DEFINITION from the registry and returns
     * {@link GearScore#BASELINE} for one that declares itself unscored, <b>before</b> it reads the
     * PDC. That ordering is forced rather than stylistic: {@code heldScore}'s own body is
     * {@code GearScore.orAbsent(read(...))}, which resolves an absent stamp to {@code ABSENT} (100) --
     * so by the time a caller holds an {@code int} it has <b>collapsed "declared unscored" with
     * "carries no stamp"</b>, and the exclusion lives on exactly that difference. A filter on the
     * returned value could not tell them apart.
     *
     * <p><b>This javadoc recorded the gap while it was open, and that entry is discharged rather than
     * deleted:</b> between the port landing and the registry reaching {@code AdapterContext}, a
     * {@code volley_stone} (deleted 2026-11)'s payloads DID scale like every other trigger weapon. It was tracked in
     * {@code GATE-gearscore.md} under <i>SLICE 12b</i> because a comment cannot fail and this method
     * read as finished throughout.
     */
    @Override
    public int triggerScoreOf(UUID combatantId) {
        if (!(world.getEntity(combatantId) instanceof Player player)) return GearScore.BASELINE;
        Regions.requireOwned(player);
        return GearScoreItems.heldScore(player, ctx.keys(), ctx.weapons());
    }

    /**
     * B2 (2026-11-23): the behaviours of the MINTED WEAPON in his main hand, read where {@link #triggerScoreOf} reads the
     * score, on his own thread. Anything that is not a minted weapon (an empty hand, a shield, a tool) gives none.
     */
    @Override
    public java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour> behavioursOf(UUID combatantId) {
        if (!(world.getEntity(combatantId) instanceof Player player)) return java.util.List.of();
        Regions.requireOwned(player);
        java.util.Optional<String> held = io.github.butterflysmp.rpg.paper.weapon.WeaponItems.heldWeaponId(player, ctx.keys());
        if (held.isEmpty()) return java.util.List.of();
        // The weapon filter (2026-11-24): the held weapon's id goes with its state, so a weapon-specific behaviour on
        // any other weapon is dropped where it is read (BehaviourItems.activeOf).
        return io.github.butterflysmp.rpg.paper.weapon.BehaviourItems.activeOf(
                io.github.butterflysmp.rpg.paper.weapon.EnchantItems.read(player.getInventory().getItemInMainHand(),
                        ctx.keys()),
                held.get(), ctx.enchants()::find);
    }

    /** Wildfire (2026-11-24): the server's one {@code BehaviourHits} answers the chain's factor, on the target's thread. */
    @Override
    public double hitChainFactor(io.github.butterflysmp.rpg.core.combat.Caster caster,
                                 io.github.butterflysmp.rpg.core.combat.Combatant target) {
        return ctx.behaviourHits().chainFactor(this, caster, target);
    }

    /** B2-B4: every primary hit goes to the server's one {@code BehaviourHits}, on the target's thread. */
    @Override
    public void primaryHit(io.github.butterflysmp.rpg.core.combat.Caster caster,
                           io.github.butterflysmp.rpg.core.combat.Combatant target) {
        ctx.behaviourHits().onHit(this, caster, target);
    }

    /** The server's tick ({@code Bukkit.getCurrentTick}), the clock every cooldown here is already on. */
    @Override
    public long currentTick() {
        return org.bukkit.Bukkit.getCurrentTick();
    }

    /**
     * {@code SPLIT}: one log line per projectile split while {@code /rpg mobtrace} is on (the PLAYERHIT switch), so the
     * selftest can read where an Airburst split. The shape, pinned by {@code SplitLineTest}: {@code SPLIT caster=<uuid>
     * source=<ability> at=<x>,<y>,<z> bursts=<n> tick=<server tick>}, coordinates to three places.
     */
    @Override
    public void traceSplit(UUID casterId, String source, io.github.butterflysmp.rpg.core.Vec3 at, int bursts) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        ctx.log().info(splitLine(casterId, source, at, bursts, org.bukkit.Bukkit.getCurrentTick()));
    }

    @Override
    public io.github.butterflysmp.rpg.core.combat.Orbs orbs() {
        return ctx.orbs();
    }

    /**
     * D2: a marker exactly as {@link #spawnMarker} makes one ({@code setPersistent(false)}: never saved; pickup refused;
     * gravity off; vanilla's despawn armed after the expected life), TAGGED {@code Keys.orb} so the sweep can find a body
     * no live orb owns.
     */
    @Override
    public UUID spawnOrbMarker(io.github.butterflysmp.rpg.core.Vec3 at, String itemId, int expectedLifetimeTicks) {
        // Untagged as an orb: claimOrbBody tags it once the store owns it (the seat's fix of #265, 2026-11-23).
        return spawnMarker(at, itemId, expectedLifetimeTicks);
    }

    /**
     * Tag an orb body {@code Keys.orb}, so {@code OrbRemovals.sweep} can find it once no orb owns it. Called by
     * {@code throwOrb} AFTER {@code Orbs.place} owns the id (the seat's fix of #265, 2026-11-23), for both body kinds.
     */
    @Override
    public void claimOrbBody(UUID bodyId) {
        org.bukkit.entity.Entity body = world.getEntity(bodyId);
        if (body != null) {
            body.getPersistentDataContainer().set(ctx.keys().orb, PersistentDataType.BYTE, (byte) 1);
        }
    }

    /**
     * THE BLOCK-DISPLAY ORB BODY (Ben's item 3, 2026-11-23: "a Floating Lapis block with glowing"; the seat's rulings (a)
     * to (c), PR 2). Each setting below with what was READ for it, from the pinned jars:
     *
     * <ul>
     *   <li><b>Centred.</b> A display renders its model through {@code com.mojang.math.Transformation.compose}, READ:
     *       {@code new Matrix4f().translation(t).rotate(left).scale(s).rotate(right)}, so a model point {@code v} lands at
     *       {@code t + left(s * right(v))}. A block model spans 0 to 1 from the entity's position, its corner; scaled by
     *       {@code s} it spans 0 to {@code s}, so a translation of {@code -s/2} on each axis centres it on the position.</li>
     *   <li><b>Glow.</b> {@code setGlowing(true)} draws the outline; {@code setGlowColorOverride} colours it, READ in
     *       {@code CraftDisplay}: {@code handle.setGlowColorOverride(color.asRGB())}, or -1 for null (the team colour).</li>
     *   <li><b>Smoothing.</b> {@code setTeleportDuration(1)}: the API's own javadoc, READ, "1 means that the display entity
     *       will move from current position to the updated one over one tick" (0 to 59 allowed). The flight places it
     *       once a tick, so each placement is eased over that tick.</li>
     *   <li><b>Never saved.</b> {@code setPersistent(false)} writes {@code Entity.persist} ({@code CraftEntity}, READ), and
     *       {@code Entity.saveAsPassenger} returns false when {@code persist} is false unless its fourth argument forces
     *       it (READ; which callers force it, UNVERIFIED). Its orb tag (set at the claim) is the sweep's, whatever survives.</li>
     * </ul>
     *
     * <p>A block id that names no block (warned at load, {@code ContentValidator.validateOrbBodies}) spawns NOTHING: an
     * {@code ORBBODY none} line, and a fresh id that every later call finds absent.
     */
    @Override
    public UUID spawnOrbBlock(UUID ownerId, io.github.butterflysmp.rpg.core.Vec3 at,
                              io.github.butterflysmp.rpg.core.ability.CastSpec.OrbBlock look) {
        Material material = orbBlockMaterial(look.block());
        if (material == null) {
            ctx.log().warning("ORBBODY none owner=" + ownerId + " block=" + look.block()
                    + " -- not a block; this orb has no body");
            return UUID.randomUUID();
        }
        float s = (float) look.scale();
        org.bukkit.entity.BlockDisplay body = world.spawn(toLocation(at), org.bukkit.entity.BlockDisplay.class, d -> {
            d.setBlock(material.createBlockData());
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(-s / 2, -s / 2, -s / 2),
                    new org.joml.AxisAngle4f(), new org.joml.Vector3f(s, s, s), new org.joml.AxisAngle4f()));
            d.setTeleportDuration(1);
            // OPTIONAL (Ben, 2026-11-23: "Remove the glow, it is excessive"): a look with no colour does not glow. The
            // mechanism stays for a later orb.
            if (look.glow() != null) {
                d.setGlowing(true);
                d.setGlowColorOverride(org.bukkit.Color.fromRGB(look.glowRgb()));
            }
            d.setPersistent(false);
        });
        // NO TAG INSIDE THE SPAWN (the seat's fix of #265, 2026-11-23). READ, CraftRegionAccessor.addEntity: the consumer
        // above runs, THEN the body is added to the world (EntityAddToWorldEvent, where OrbRemovals.sweepAppeared runs), THEN
        // spawn returns. On 78826389 the orb tag set in the consumer let the sweep see every body unowned. The marker tag
        // goes on here, after the add; the orb tag only when throwOrb claims the body, after the store owns it.
        body.getPersistentDataContainer().set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);
        // READ BACK OFF THE BODY, not the spec: the gate reads what was spawned.
        org.bukkit.Color glow = body.getGlowColorOverride();
        ctx.log().info(orbBodyLine(ownerId, body.getUniqueId(), body.getType().getKey().getKey(),
                body.getBlock().getAsString(), body.isGlowing(), glow == null ? -1 : glow.asRGB(),
                body.getTransformation().getScale().x(), body.getTeleportDuration(), body.isPersistent(),
                org.bukkit.Bukkit.getCurrentTick()));
        return body.getUniqueId();
    }

    /** The material {@code id} names if it is a block, else null: the spawn's resolver, and the load check's. */
    public static Material orbBlockMaterial(String id) {
        Material m = Material.matchMaterial(id);
        return m != null && m.isBlock() && !m.isAir() ? m : null;
    }

    /**
     * {@code ORBBODY owner=<uuid> id=<uuid> type=<entity type> block=<block data> glowing=<bool> glow=#RRGGBB scale=<s>
     * teleport_duration=<n> persistent=<bool> tick=<n>} (pinned by {@code BensEnchantChangesTest}); {@code glow=none}
     * when no colour is set.
     */
    public static String orbBodyLine(UUID ownerId, UUID bodyId, String type, String block, boolean glowing, int glowRgb,
                                     float scale, int teleportDuration, boolean persistent, long tick) {
        return String.format(java.util.Locale.ROOT,
                "ORBBODY owner=%s id=%s type=%s block=%s glowing=%s glow=%s scale=%.3f teleport_duration=%d persistent=%s tick=%d",
                ownerId, bodyId, type, block, glowing, glowRgb < 0 ? "none" : String.format("#%06X", glowRgb), scale,
                teleportDuration, persistent, tick);
    }

    /**
     * THE BODY'S LOOK AT REST (Ben's changes to the crystal, 2026-11-23): its transformation set to {@code pose} (a
     * translation, and a turn about the vertical as the LEFT rotation), at its content scale, interpolated over
     * {@code interpolationTicks} starting now. READ: {@code CraftDisplay.setInterpolationDelay} and
     * {@code setInterpolationDuration} write the synced entity data {@code DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID}
     * and {@code DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID}; that the CLIENT eases the old transformation into the new
     * one over that duration is INFERRED (the easing is client code, not in the server jar). The entity's position, the
     * rest point, is not touched. The scale is read back off the body, so the pose and the spawn agree.
     */
    @Override
    public void poseOrbBody(UUID bodyId, io.github.butterflysmp.rpg.core.ability.CastSpec.OrbBlock.Pose pose,
                            int interpolationTicks) {
        if (!(markerOf(bodyId) instanceof org.bukkit.entity.Display d)) return;
        org.joml.Vector3f scale = d.getTransformation().getScale();
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(interpolationTicks);
        d.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f((float) pose.tx(), (float) pose.ty(), (float) pose.tz()),
                new org.joml.AxisAngle4f((float) pose.yaw(), 0, 1, 0), scale, new org.joml.AxisAngle4f()));
    }

    /** Teleport the block body (ruling (a)); gone is a no-op, through {@link #markerOf}, as every marker call is. */
    @Override
    public void placeOrbBody(UUID bodyId, io.github.butterflysmp.rpg.core.Vec3 at) {
        Entity body = markerOf(bodyId);
        if (body != null) body.teleport(toLocation(at));
    }

    @Override
    public void traceOrb(UUID ownerId, String event, io.github.butterflysmp.rpg.core.Vec3 at, String cause) {
        ctx.log().info(orbLine(ownerId, event, at, cause, org.bukkit.Bukkit.getCurrentTick()));
    }

    /**
     * {@code ORB rest owner=<uuid> at=<x>,<y>,<z> tick=<n>} or {@code ORB removed owner=<uuid> at=<x>,<y>,<z>
     * cause=<cause> tick=<n>}, three places (pinned by {@code OrbLineTest}).
     */
    @Override
    public io.github.butterflysmp.rpg.core.combat.FusedChunks fusedChunks() {
        return ctx.fused();
    }

    /**
     * Tag a fused chunk's body {@code Keys.fused}, AFTER the store owns its id (the orb's order, the seat's fix of #265),
     * so {@code FusedRemovals.sweep} can find it once no live chunk owns it. The body is an ordinary item marker
     * ({@link #spawnMarker}: never saved, no pickup, no merge, gravity off, the hopper refused by its marker tag, the
     * despawn armed at the fuse).
     */
    @Override
    public void claimFusedBody(UUID bodyId) {
        org.bukkit.entity.Entity body = world.getEntity(bodyId);
        if (body != null) body.getPersistentDataContainer().set(ctx.keys().fused, PersistentDataType.BYTE, (byte) 1);
    }

    @Override
    public void traceFused(UUID ownerId, long chunkId, String event, io.github.butterflysmp.rpg.core.Vec3 at, String cause) {
        ctx.log().info(fusedLine(ownerId, chunkId, event, at, cause, org.bukkit.Bukkit.getCurrentTick()));
    }

    /**
     * {@code FUSED <event> owner=<uuid> id=<n> at=<x>,<y>,<z> [cause=<cause>] tick=<n>}, three places, for {@code throw},
     * {@code rest}, {@code detonate} (cause {@code fuse} or {@code touch}) and {@code removed} (pinned by
     * {@code FusedLineTest}).
     */
    public static String fusedLine(UUID ownerId, long chunkId, String event, io.github.butterflysmp.rpg.core.Vec3 at,
                                   String cause, long tick) {
        String where = String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", at.x(), at.y(), at.z());
        return "FUSED " + event + " owner=" + ownerId + " id=" + chunkId + " at=" + where
                + (cause == null || cause.isEmpty() ? "" : " cause=" + cause) + " tick=" + tick;
    }

    public static String orbLine(UUID ownerId, String event, io.github.butterflysmp.rpg.core.Vec3 at, String cause, long tick) {
        String where = String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", at.x(), at.y(), at.z());
        return "ORB " + event + " owner=" + ownerId + " at=" + where
                + (cause == null || cause.isEmpty() ? "" : " cause=" + cause) + " tick=" + tick;
    }

    @Override
    public void traceOrbShot(UUID ownerId, io.github.butterflysmp.rpg.core.Vec3 from, io.github.butterflysmp.rpg.core.Vec3 to,
                             String visual) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        ctx.log().info(orbShotLine(ownerId, from, to, visual, org.bukkit.Bukkit.getCurrentTick()));
    }

    /**
     * {@code ORBSHOT owner=<uuid> from=<x>,<y>,<z> to=<x>,<y>,<z> visual=<id> tick=<n>}, three places (pinned by
     * {@code OrbLineTest}): one per line drawn from an orb to a mob it struck (Ben, 2026-11-23).
     */
    public static String orbShotLine(UUID ownerId, io.github.butterflysmp.rpg.core.Vec3 from, io.github.butterflysmp.rpg.core.Vec3 to,
                              String visual, long tick) {
        return String.format(java.util.Locale.ROOT, "ORBSHOT owner=%s from=%.3f,%.3f,%.3f to=%.3f,%.3f,%.3f visual=%s tick=%d",
                ownerId, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(), visual, tick);
    }

    @Override
    public void traceFocusedBeam(UUID casterId, String source, io.github.butterflysmp.rpg.core.Vec3 from, double range,
                                 long pressedTick) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        ctx.log().info(focusedLine(casterId, source, from, range, pressedTick, org.bukkit.Bukkit.getCurrentTick()));
    }

    @Override
    public void traceVolleyShot(UUID casterId, String source, int shotIndex, long pressedTick) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        ctx.log().info(volleyShotLine(casterId, source, shotIndex, pressedTick, org.bukkit.Bukkit.getCurrentTick()));
    }

    /**
     * {@code VOLLEYSHOT} (Early Toll, 2026-11-24): one line per volley shot, the shot's tick beside the PRESS's (stamped in
     * {@code WeaponFire} before its hop, so {@code tick - pressed} of shot 0 is the wind-up from the press). Shape pinned by
     * {@code FocusedLineTest}.
     */
    static String volleyShotLine(UUID casterId, String source, int shotIndex, long pressedTick, long tick) {
        return String.format(java.util.Locale.ROOT, "VOLLEYSHOT caster=%s source=%s shot=%d pressed=%d tick=%d",
                casterId, source, shotIndex, pressedTick, tick);
    }

    /** {@code FOCUSED} (Focused Beam, 2026-11-24): the press's tick and the beam's, on one line ({@code FocusedLineTest}). */
    static String focusedLine(UUID casterId, String source, io.github.butterflysmp.rpg.core.Vec3 from, double range,
                              long pressedTick, long tick) {
        return String.format(java.util.Locale.ROOT, "FOCUSED caster=%s source=%s from=%.3f,%.3f,%.3f range=%.3f pressed=%d tick=%d",
                casterId, source, from.x(), from.y(), from.z(), range, pressedTick, tick);
    }

    static String splitLine(UUID casterId, String source, io.github.butterflysmp.rpg.core.Vec3 at, int bursts, long tick) {
        return String.format(java.util.Locale.ROOT, "SPLIT caster=%s source=%s at=%.3f,%.3f,%.3f bursts=%d tick=%d",
                casterId, source, at.x(), at.y(), at.z(), bursts, tick);
    }

    /**
     * {@code PLAYERHIT}: one log line per traced hit while {@code /rpg mobtrace} is on, beside
     * {@code MOBHIT} (mob-to-player) on the same switch. The target's type is read here because core
     * has no entity types; the lookup is the {@code Attribution} shape, and a target this thread does
     * not own prints {@code ?} rather than being read off-thread.
     */
    @Override
    public void traceHit(TracedHit hit) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        Entity target = world.getEntity(hit.targetId());
        String type = target == null ? "gone"
                : Bukkit.isOwnedByCurrentRegion(target) ? target.getType().key().value() : "?";
        ctx.log().info(hit.line(type));
    }

    @Override
    public void traceLanding(java.util.UUID casterId, String source, io.github.butterflysmp.rpg.core.build.Touchdown how,
                             Vec3 feet) {
        if (!ctx.mobTrace().getAsBoolean()) return;
        ctx.log().info(how.line(casterId, source, feet));
    }

    /**
     * The project's only spawned entity: a thrown ember. A real Item launched from the caster
     * with {@code velocity}; vanilla physics flies and lands it, so it arcs, bounces, and rolls
     * to rest like any thrown item. The item IS the marker -- the fuse detonates at its LIVE
     * position (see {@link #markerLocation}), so where or how it settles does not matter, and
     * there is no landing detection and no separate display entity.
     *
     * It is released {@link #THROW_ORIGIN_LIFT} above {@code origin} (the caster's feet) so it
     * leaves from about hand height, not the ground. This is the whole reason the earlier
     * resting-marker approach could be thrown out: that one PLACED an item at a computed landing
     * point on a block face, and vanilla ejected it upward to resolve the intersection -- the
     * pop that six rounds of velocity-zeroing and settle-fighting never cured. A thrown item is
     * never set down inside a block, so the pop cannot arise. We do NOT zero the velocity here:
     * the point is that it flies.
     *
     * setPickupDelay(MAX) keeps it un-collectible and non-mergable (see {@link #configureMarker},
     * which records what that value does and, more importantly, what it does NOT do).
     * setPersistent(false) is the unload backstop. Its normal removal is the fuse task, which
     * calls removeMarker below -- a leaked real Item is the leak-on-death hazard one more time.
     *
     * A world write, so only legal on the thread owning {@code origin} -- the caller (a cast
     * resolving on the caster's region) already satisfies that.
     */
    @Override
    public UUID throwMarker(Vec3 origin, Vec3 velocity, String itemId) {
        Location spawnAt = toLocation(origin).add(0, THROW_ORIGIN_LIFT, 0);
        Item marker = world.spawn(spawnAt, Item.class, item -> {
            configureMarker(item, itemId);
            item.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z())); // thrown -- it flies
        });
        return marker.getUniqueId();
    }

    /**
     * How long vanilla lets a dropped item live before {@code discard()}s it, in ticks --
     * {@code ItemEntity.LIFETIME}. Read out of the pinned server jar rather than remembered.
     */
    private static final int VANILLA_ITEM_LIFETIME_TICKS = 6000;

    /**
     * Slack between when a driven marker's owner SHOULD have removed it and when vanilla removes it
     * anyway. Three seconds: long enough that a normally-resolving bolt is never killed out from
     * under its own flight by a tick of scheduling jitter, short enough that an orphan is gone
     * before anybody walks over to look at it.
     */
    private static final int ORPHAN_GRACE_TICKS = 60;

    /**
     * The shared item configuration behind both marker kinds, and the one place the meaning of
     * {@code setPickupDelay(Integer.MAX_VALUE)} is written down.
     *
     * <p><b>WHAT THAT VALUE DOES.</b> {@code CraftItem.setPickupDelay} is {@code Math.min(v, 32767)},
     * and 32767 is {@code ItemEntity.INFINITE_PICKUP_DELAY}. In {@code ItemEntity.tick()} the
     * countdown is skipped at that value, and {@code isMergable()} returns false for it. So the item
     * is permanently un-collectible and never merges with a neighbouring stack.
     *
     * <p><b>WHAT IT DOES NOT DO, and this javadoc used to claim otherwise.</b> It does NOT stop the
     * item despawning. The despawn is gated on a different field entirely:
     *
     * <pre>
     *   if (age != -32768) age++;                                  // -32768 = INFINITE_LIFETIME
     *   if (!level.isClientSide &amp;&amp; age >= 6000) discard();         //  6000 = LIFETIME
     * </pre>
     *
     * <p>{@code age}, not {@code pickupDelay}. The earlier wording here said "non-mergable and
     * non-despawning"; the first half is right and the second was false, which matters because it is
     * exactly the claim the next person writing a marker would inherit -- or "fix" by reaching for
     * {@code setUnlimitedLifetime}, turning a bounded exposure into permanent world litter.
     *
     * <p><b>So: never call {@code setUnlimitedLifetime(true)} or {@code setWillAge(false)} on a
     * marker.</b> Both exist on the Item API, both set or preserve that {@code -32768}, and either
     * one removes the only backstop these entities have.
     */
    private void configureMarker(Item item, String itemId) {
        Material material = Material.matchMaterial(itemId);
        if (material == null || !material.isItem()) {
            ctx.warnOnce("Unknown marker material '" + itemId + "'; using BLAZE_POWDER");
            material = Material.BLAZE_POWDER;
        }
        item.setItemStack(new ItemStack(material));
        item.setPersistent(false);               // unload backstop

        // THREE COLLECTORS -- BUT ONLY TWO FIELDS, AND THAT IS THE PLATFORM'S CHOICE, NOT OURS.
        //
        // A marker is a REAL item stack that nobody paid for, so anything that picks one up mints
        // it out of nothing. Three things do:
        //
        //   players           -> pickupDelay. ItemEntity.playerTouch returns early while it is > 0.
        //   mobs              -> canMobPickup, a genuinely separate boolean, and one that was never
        //                        set here at all until the W3 gate row.
        //   hoppers and
        //   hopper minecarts  -> consult NEITHER. Checked, not assumed:
        //                        HopperBlockEntity.getItemsAtAndAbove filters only on
        //                        ENTITY_STILL_ALIVE, and addItem(Container, ItemEntity) copies the
        //                        stack in and discards the entity -- no pickup-delay check anywhere
        //                        on that path. Refused at InventoryPickupItemEvent instead, which is
        //                        what the markerEntity tag below is for.
        //
        // PLAYER REFUSAL AND NON-MERGABILITY ARE THE SAME WRITE AND CANNOT BE STATED SEPARATELY.
        // An earlier version of this comment claimed they could, and called both
        // setPickupDelay(MAX) "for non-mergability" and setCanPlayerPickup(false) "for players".
        // Decompiled from the pinned jar, CraftItem.setCanPlayerPickup(b) is literally
        //     getHandle().pickupDelay = b ? 0 : 32767;
        // -- the same field, the same value. The two calls were one fact written twice.
        //
        // So there is ONE call, on the field we actually depend on. 32767 is
        // ItemEntity.INFINITE_PICKUP_DELAY: the countdown is skipped at that value, so players are
        // refused forever, AND isMergable() early-returns on it, so a marker never merges into a
        // neighbouring stack. Both properties, one write, by the platform's construction.
        //
        // DO NOT "restore" setCanPlayerPickup(false) as a clearer spelling of the player half: it
        // is an alias for this line, not an addition to it. And never call it with TRUE -- that
        // writes pickupDelay = 0, which does not merely let players collect the marker, it silently
        // makes it MERGABLE as well.
        //
        // In the SHARED path, exactly where setVelocity(zero) went and for the same reason: this
        // closes both marker kinds at once, and a third kind inherits the protection instead of
        // having to remember it. A MARKER HAS TO SAY IT IS COLLECTIBLE RATHER THAN FORGET TO SAY
        // IT ISN'T.
        item.setPickupDelay(Integer.MAX_VALUE);  // clamped to 32767: no player pickup, no merging
        item.setCanMobPickup(false);
        item.getPersistentDataContainer().set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);

        // ZERO THE VELOCITY HERE, AS THE BASELINE, RATHER THAN AT EACH CALL SITE.
        //
        // A FRESH ItemEntity IS NOT STATIONARY. Its constructor ends with
        //     setDeltaMovement(random*0.2 - 0.1, 0.2, random*0.2 - 0.1)
        // -- read out of the pinned server jar. That constant 0.2 on Y is the little POP a dropped
        // item makes, and it is applied to every item this method will ever configure.
        //
        // Combined with a driven marker's setGravity(false), which removes the only force that
        // would ever bring it back down, that pop becomes a bolt that rises gently forever and
        // never goes anywhere near its flight path. Gravity on with a stray velocity is merely a
        // wrong arc that still lands; velocity zeroed with gravity off is correct. ONLY THE TWO
        // TOGETHER FLOAT, which is why neither alone looks like a bug worth writing down.
        //
        // It lives in the SHARED configuration and not in spawnMarker because that is what makes
        // the mistake unrepeatable. spawnMarker was specified as a diff from throwMarker -- "no
        // lift, no velocity, gravity off, pre-aged" -- and three of those four are a line REMOVED
        // while "no velocity" needed a line ADDED. The two read identically in a spec and do not
        // behave identically in a platform. A future third marker kind inherits stillness here and
        // has to opt OUT of it, which is the direction that fails safe.
        //
        // throwMarker overrides this immediately afterwards with its throw velocity: the point is
        // that a marker must now SAY it moves, not merely forget to say it does not.
        item.setVelocity(new Vector(0, 0, 0));
    }

    /**
     * A body rendered at a position CORE computes, going nowhere on its own -- the Flint Staff's
     * flint chunk. Its counterpart {@link #throwMarker} hands an item to physics and reads back
     * where physics took it; this one is the opposite arrangement, because a projectile that
     * resolves on a traced segment cannot let physics own the position or the thing you see and the
     * thing you hit are two different objects.
     *
     * <p>Four deliberate differences from {@code throwMarker}, each of which would be a defect if
     * copied across: <b>no {@link #THROW_ORIGIN_LIFT}</b> (core gives an exact point, not a
     * thrower's feet -- the caller's origin is already an eye), <b>no velocity</b>, <b>gravity
     * off</b> (we own the position; physics must not fight the teleports for it), and <b>pre-aged</b>.
     *
     * <p><b>THE PRE-AGE IS THE POINT, AND IT IS THE THIRD EXIT.</b> A driven marker has three ways
     * to end, not two: the flight hits something, the flight's fuse expires, or <b>the scheduled
     * continuation never runs at all</b> -- a region unloads, or the server stops. There is no
     * {@code finally} on a chain of scheduled callbacks, and what is left behind is a real entity
     * that only our code removes.
     *
     * <p>So rather than adding a mechanism that could itself fail to run, we arm vanilla's own
     * timer: {@code CraftItem.setTicksLived} writes straight into {@code ItemEntity.age}, so
     * spawning the item pre-aged makes vanilla {@code discard()} it {@code expectedLifetimeTicks +
     * }{@link #ORPHAN_GRACE_TICKS} after birth, whether or not anything of ours ever runs again.
     * For the Flint Staff's 40-tick fuse that is about five seconds instead of the five minutes
     * plain {@code LIFETIME} would give.
     *
     * <p>{@code CraftEntity.setTicksLived} requires a value {@code > 0} ("Age value (%s) must be
     * greater than 0"), hence the clamp -- which also keeps an absurdly long authored fuse from
     * producing a negative age rather than a long-lived marker.
     *
     * <p><b>THE INTERACTION AXIS, ENUMERATED ONCE HERE RATHER THAN DISCOVERED ONE REPORT AT A TIME.</b>
     * A driven marker is a FULLY PARTICIPATING vanilla item entity in motion, and vanilla does a
     * great deal to those. The predecessor design participated in nothing -- no velocity, so nothing
     * pushed it; repositioned every tick, so nothing could carry it away -- so this whole axis was
     * CREATED by the move to velocity. That is the easiest kind to miss: there was no prior exposure
     * to carry forward and notice.
     *
     * <p>Accepted, on the record, unless a gate row says otherwise:
     * <ul>
     *   <li><b>Water and lava</b> give an item buoyancy and heavy drag ({@code setUnderwaterMovement}
     *       in {@code ItemEntity.tick}). A bolt fired across a pond diverges from the computed path
     *       immediately and visibly. Not exotic -- a normal shot on a normal map. <b>Gate row.</b></li>
     *   <li><b>Fire, lava and cactus DESTROY items.</b> {@code ItemEntity.fireImmune()} is true only
     *       when the STACK resists fire, and flint does not -- so on a FIRE weapon the body can be
     *       destroyed mid-flight. Harmless to resolution: the flight continues, {@link #removeMarker}
     *       finds nothing and no-ops, and the bolt simply loses its body. <b>Gate row.</b></li>
     *   <li><b>Hoppers eat it, and this one is an ECONOMY LEAK rather than a cosmetic quirk.</b>
     *       Checked rather than assumed: {@code HopperBlockEntity.getItemsAtAndAbove} filters only on
     *       {@code EntitySelector.ENTITY_STILL_ALIVE}, and {@code addItem(Container, ItemEntity)}
     *       copies the stack in and discards the entity -- <b>no pickup-delay check anywhere on that
     *       path</b>. {@code setPickupDelay(MAX)} stops players, not hoppers. Since the marker is a
     *       real flint nobody paid for, a hopper under the flight line CREATES flint. Bounded by a
     *       ~2 second flight at roughly eye height, so it needs a hopper almost directly under the
     *       shot. A mitigation exists and is cheap -- {@code InventoryPickupItemEvent} is cancellable,
     *       and these markers can be tagged through {@code Keys} -- and is deliberately NOT taken
     *       here: it is its own decision, not a thing to smuggle into a movement change.</li>
     *   <li><b>Explosions and pistons</b> push item entities. Same class as the fluids: the body
     *       leaves the path and the flight does not.</li>
     * </ul>
     *
     * <p>None of these affect RESOLUTION. {@code castRay} owns what the bolt hits and never consults
     * the body, so the worst case throughout is a body that is somewhere other than the flames.
     *
     * <p>A world write, so only legal on the thread owning {@code at}.
     */
    @Override
    public UUID spawnMarker(Vec3 at, String itemId, int expectedLifetimeTicks) {
        int ticksLived = Math.max(1,
                VANILLA_ITEM_LIFETIME_TICKS - expectedLifetimeTicks - ORPHAN_GRACE_TICKS);
        Item marker = world.spawn(toLocation(at), Item.class, item -> {
            configureMarker(item, itemId);
            item.setGravity(false);            // core owns the position; physics must not compete
            item.setTicksLived(ticksLived);    // armed self-destruct -- see the javadoc above
        });
        return marker.getUniqueId();
    }

    /**
     * The value handed to {@code AbstractArrow.setLifetimeTicks} so that the FIRST despawn tick the
     * arrow is ever given discards it, whatever the server's despawn rates are set to.
     *
     * <h2>WHY A SENTINEL AND NOT AN ARITHMETIC PRE-AGE, WHICH IS WHAT THE ITEM MARKER DOES</h2>
     *
     * <p>{@link #spawnMarker} computes {@code 6000 - lifetime - grace} against
     * {@link #VANILLA_ITEM_LIFETIME_TICKS}, a CONSTANT compiled into the server. The arrow's
     * equivalent limit is not a constant: {@code AbstractArrow.tickDespawn} reads it from config at
     * runtime -- {@code non-player-arrow-despawn-rate}, falling back to spigot's
     * {@code arrow-despawn-rate} -- so the same arithmetic would bake this server's 1200 into the
     * jar and give a seventy-second orphan on a server that raised it.
     *
     * <p>Read out of the pinned jar, {@code tickDespawn} is:
     *
     * <pre>
     *   life++;  if (life &gt;= rate) discard(DESPAWN);
     * </pre>
     *
     * <p>so arming {@code life} such that {@code life + 1} is at least the largest value {@code rate}
     * can hold makes the comparison true on the first call <b>for every configuration</b>, and turns
     * two config dependencies into one.
     *
     * <h2>*** IT IS {@code MAX_VALUE - 1} AND {@code MAX_VALUE} IS EXACTLY WRONG ***</h2>
     *
     * <p>{@code life} is an {@code int} and the increment is unguarded. At {@code MAX_VALUE} the
     * {@code life++} OVERFLOWS to {@code Integer.MIN_VALUE}, and {@code MIN_VALUE >= rate} is false
     * for every non-negative rate -- so the obvious "even safer" value is the one that disarms the
     * trap completely, and disarms it SILENTLY: the body would simply live out the full configured
     * rate instead.
     *
     * <p>At {@code MAX_VALUE - 1} the increment lands exactly on {@code MAX_VALUE} and stops there.
     * {@code PlumeBodyLifetimeTest} asserts both halves of that, so the off-by-one cannot be tidied
     * back in.
     *
     * <p><b>{@code CraftAbstractArrow.setLifetimeTicks} does NOT validate its argument</b> -- it is a
     * bare {@code putfield} into {@code AbstractArrow.life}, verified from the jar. Contrast
     * {@code CraftEntity.setTicksLived}, which rejects anything {@code <= 0}. So no clamp is needed
     * here, and none should be added: a clamp would be guarding against a value this constant is
     * chosen to be.
     */
    static final int ARMED_ARROW_LIFETIME = Integer.MAX_VALUE - 1;

    /**
     * This server's {@code max-arrow-despawn-invulnerability}, in ticks -- the number of ticks an
     * airborne arrow gets before {@code tickDespawn} is called on it at all.
     *
     * <p><b>THIS IS AN ASSUMPTION ABOUT THE OPERATOR'S CONFIG, NOT A PLATFORM CONSTANT, AND IT IS
     * NOT READABLE FROM THE API.</b> The value lives at
     * {@code paper-world-defaults.yml: entities.spawning.max-arrow-despawn-invulnerability} and is
     * {@code 200} in this repo. The server reads it through
     * {@code Level.paperConfig().entities.spawning.maxArrowDespawnInvulnerability}, and
     * {@code WorldConfiguration} <b>is not in the API jar at all</b>: the only type under
     * {@code io.papermc.paper.configuration} that the API exposes is {@code ServerConfiguration},
     * whose entire surface is {@code isProxyOnlineMode()} and {@code isProxyEnabled()}. There is no
     * {@code World#getWorldConfig()}. Measured against the pinned API build; reaching the real value
     * would need NMS, which this repo bans.
     *
     * <p>So this constant is <b>used for nothing but the log line and this documentation</b> -- no
     * arithmetic depends on it, which is the deliberate consequence of the sentinel above. <b>If an
     * operator raises that config, our orphan window grows with it and nothing of ours notices.</b>
     * The key is named here so the next person finds the LEVER rather than trying to correct the
     * NUMBER.
     */
    private static final int ASSUMED_ARROW_DESPAWN_INVULNERABILITY_TICKS = 200;

    /**
     * An arrow rendered along the path core computes -- the Dragon's Plume's body. The sibling of
     * {@link #spawnMarker}, and the reason it is a sibling is in {@code CombatWorld}: an arrow is
     * not an item and has no item id.
     *
     * <h2>ONE SWITCH DOES THE WORK, AND IT IS NOT THE ONE THE API ADVERTISES</h2>
     *
     * <p>Measured from the pinned jar rather than assumed. {@code AbstractArrow.tick()} opens by
     * computing {@code flag = !isNoPhysics()} and gates on it:
     *
     * <pre>
     *   in-ground / collision-shape detection   gated on flag
     *   if (isInGround() &amp;&amp; flag)  ...          the in-ground branch
     *   if (flag) clipIncludingBorder(..) -&gt; stepMoveAndHit(hit)
     *   else      setPos(position + delta)
     *   if (flag &amp;&amp; !isInGround()) applyGravity()
     * </pre>
     *
     * <p>{@code stepMoveAndHit} is the ONLY route to {@code onHitBlock}, {@code findHitEntities},
     * {@code hitTargetsOrDeflectSelf} and {@code CraftEventFactory.callProjectileHitEvent}. So
     * {@code setNoPhysics(true)} turns off block collision, entity collision, the hit event,
     * in-ground sticking AND gravity together.
     *
     * <p><b>{@code setGravity(false)} IS NOT A SUBSTITUTE AND IS NOT REDUNDANT EITHER.</b>
     * {@code Entity.applyGravity()} is {@code if (getGravity() != 0) deltaMovement.add(0, -g, 0)} --
     * it suppresses the fall and NOTHING ELSE. A gravity-less arrow still clips blocks, still finds
     * hit entities and still fires {@code ProjectileHitEvent}. It is set here anyway because
     * {@code noPhysics} is the thing a later reader is most likely to question, and the two must not
     * both have to be right for the body to stop falling.
     *
     * <p><b>{@code Projectile.canHitEntity(Entity)} IS A QUERY WITH NO SETTER ANYWHERE ON THE
     * API</b> -- checked, so nobody spends an hour looking for one. There is no third route.
     *
     * <h2>*** setPickupStatus(DISALLOWED) IS LOAD-BEARING AND READS REDUNDANT ***</h2>
     *
     * <p>{@code AbstractArrow.playerTouch(Player)} guards on
     * <b>{@code isInGround() OR isNoPhysics()}</b>, then on {@code pickup == ALLOWED}. An ordinary
     * flying arrow is neither in-ground nor no-physics, which is why nobody can pick one out of the
     * air. <b>Turning on {@code noPhysics} satisfies that disjunction in MID-AIR and OPENS a pickup
     * path that does not otherwise exist.</b>
     *
     * <p>So this call is not belt-and-braces: deleting it as redundant mints a free arrow into a
     * player's inventory on every shot they walk into. {@code RpgListeners.onPlumeBodyPickup} is the
     * loud detector for that, and it is a detector rather than the fix.
     *
     * <h2>WHAT REACHES THIS BODY, ENUMERATED FROM THE JAR -- AND THE ITEM LIST DOES NOT APPLY</h2>
     *
     * <p>{@link #spawnMarker} enumerates water buoyancy, fire and lava destroying the stack, hoppers
     * eating it, pistons and explosions pushing it. <b>That list is about an ITEM ENTITY and is
     * deliberately NOT carried across</b>; it is re-derived here for an arrow. Each entry says what
     * was read, not what was assumed:
     *
     * <ul>
     *   <li><b>Hoppers: GONE, structurally.</b> {@code HopperBlockEntity} collects
     *       {@code ItemEntity} only. An arrow is not one, so the economy leak {@code spawnMarker}
     *       has to refuse at {@code InventoryPickupItemEvent} cannot arise here at all.</li>
     *   <li><b>Every "inside block" effect: GONE.</b> {@code Entity.isAffectedByBlocks()} is
     *       {@code !isRemoved() &amp;&amp; !noPhysics}, and {@code applyEffectsFromBlocks(List)} gates its
     *       entire body on it. That one guard removes {@code checkInsideBlocks}, {@code stepOn},
     *       cobwebs, powder snow, honey, berry bushes, magma, climbables and rails in one go.</li>
     *   <li><b>Deflection (wind charges) and block-hit side effects: GONE</b>, because
     *       {@code preHitTargetOrDeflectSelf} is reached only from {@code stepMoveAndHit}.</li>
     *   <li><b>WATER: SURVIVES, AND IT BITES HARDER THAN IT DOES ON AN ITEM.</b>
     *       {@code Projectile.tick() -&gt; Entity.tick() -&gt; baseTick()} runs unconditionally at the
     *       END of {@code AbstractArrow.tick()}, and {@code baseTick} calls
     *       {@code updateFluidInteraction()}. The resulting {@code isInWater()} is read back at the
     *       TOP of the next arrow tick, OUTSIDE the noPhysics gate, and applies
     *       {@code getWaterInertia() = 0.6f} -- a 40% velocity cut per tick. It is cosmetic only:
     *       {@code castRay} owns resolution and never consults the body, so a bolt fired across a
     *       pond resolves on its computed segment while the body falls behind it.</li>
     *   <li><b>Inertia: SURVIVES, and it is new.</b> {@code applyInertia(0.99f)} is gated only on
     *       {@code isInWater()}, never on {@code noPhysics}. Harmless while the flight is driving --
     *       {@link #driveMarker} overwrites the velocity every tick before it can accumulate -- and
     *       it is the whole reason an ORPHAN travels rather than hangs. See below.</li>
     *   <li><b>Rotation: SURVIVES, and it is why we use an arrow at all.</b> {@code atan2} over
     *       {@code deltaMovement} into {@code setXRot}/{@code setYRot}, outside the gate. The body
     *       points along the velocity {@link #driveMarker} gave it -- for free, every tick.</li>
     *   <li><b>Fire and lava: the body can BURN but is not destroyed.</b> {@code baseTick} handles
     *       fire ticks and lava; an arrow is not a stack that can be consumed, so unlike a flint
     *       marker there is no "the body simply vanishes" case from this axis.</li>
     *   <li><b>Below the world: {@code baseTick -&gt; checkBelowWorld()}</b> removes it, as it removes
     *       anything. A free extra exit rather than a hazard.</li>
     * </ul>
     *
     * <h2>*** THE ORPHAN TRAVELS. IT DOES NOT HANG. ***</h2>
     *
     * <p>An orphaned ITEM marker stops -- gravity off, velocity never renewed, nothing moves it. An
     * orphaned ARROW keeps flying on its last velocity, because the {@code else} branch is
     * {@code setPos(position + delta)} and the only thing acting on {@code delta} is the 1%/tick
     * inertia. Computed over the {@value #ASSUMED_ARROW_DESPAWN_INVULNERABILITY_TICKS}-tick window:
     *
     * <pre>
     *   sum of 0.99^n, n = 0..200   =  86.74 tick-lengths
     *   at the Plume's speed 2.5    =  216.8 blocks
     *   bounded above, any window   =  1/0.01 = 100 tick-lengths = 250 blocks at 2.5
     * </pre>
     *
     * <p><b>So the accepted cost is not "a stray body hangs for ten seconds". It is "a stray body
     * drifts up to ~217 blocks THROUGH TERRAIN for ten seconds."</b> While it does, it cannot hit,
     * damage, stick to anything or be picked up, and it dies on vanilla's own timer with no code of
     * ours running -- which is a stronger guarantee than any mechanism of ours could make, and is
     * why this exit was accepted over the two alternatives (see {@code PLAN-dragons-plume.md}).
     *
     * <h2>NEVER CALL {@code shoot()} OR {@code lerpMotion()} ON THIS BODY</h2>
     *
     * <p>{@code AbstractArrow.shoot(DDDFF)} ends with {@code life = 0}, and {@code lerpMotion}
     * zeroes it too when {@code max-arrow-despawn-invulnerability} is DISABLED. Either would disarm
     * {@link #ARMED_ARROW_LIFETIME} silently. {@code CraftEntity.setVelocity} is a plain
     * {@code setDeltaMovement} and touches neither -- verified from the jar -- which is why the
     * velocity goes in that way both here and in {@link #driveMarker}.
     */
    private UUID spawnArrowBody(Vec3 at, Vec3 velocity) {
        // *** world.spawnArrow, NOT world.spawn -- AND THE DIFFERENCE IS NOT STYLE. ***
        //
        // CraftWorld.spawnArrow is a different construction path end to end, read from the pinned
        // jar: EntityType.ARROW.create directly rather than the CraftEntityTypes table, snapTo WITH
        // the Location's rotation rather than snapTo(x, y, z, 0.0F, 0.0F), and AbstractArrow.shoot,
        // which writes setYRot(atan2(x, z)) / setXRot(atan2(y, h)) ITSELF from the direction.
        //
        // SO NOTHING HERE COMPUTES A ROTATION, AND NOTHING SHOULD. Two earlier fixes did -- one onto
        // a Location, which MOVE_EMPTY_ROT discards for an arrow, and one onto the entity, which
        // landed and was still wrong. shoot() supplies it. See GATE-plume-vanilla-body.md.
        //
        // The direction and the speed reproduce the old displacement exactly: shoot normalises the
        // direction and scales it by the speed, so passing the velocity as the direction and its own
        // length as the speed gives the same vector back, with spread 0.
        Arrow body = world.spawnArrow(toLocation(at),
                new Vector(velocity.x(), velocity.y(), velocity.z()),
                (float) Math.sqrt(velocity.lengthSquared()), 0.0f, Arrow.class);

        // *** DO NOT ADD setNoPhysics(true) BACK. IT IS THE DEFECT, MEASURED. ***
        //
        // Spike modes B and E differ in that one line -- both world.spawnArrow, both driven by
        // driveMarker every tick -- and the operator read B as "still has the problem" and E as
        // "perfect, exactly what we're looking for". A one-variable pair with opposite readings is
        // an attribution, not an inference.
        //
        // AND NO UNIT TEST GUARDS THIS. Measured: splicing setNoPhysics(true) back in reddens
        // NOTHING in a 2000-row suite, because every consequence is a rendered frame. Its sole
        // witness is GATE-plume-vanilla-body.md's EAST row. That is why this comment is long: it is
        // the only thing standing between the next tidy-up and a re-shipped defect.
        //
        // WHAT THE LINE USED TO BUY, AND WHERE EACH GUARANTEE WENT:
        //   block collision       GONE. The body now collides and sticks -- see the stick note below.
        //   entity collision      replaced by RpgListeners.onPlumeBodyHit, which cancels the event.
        //   ProjectileHitEvent    now RAISED, and cancelled for a tagged body. That is the mechanism.
        //   in-ground sticking    now HAPPENS, and the armed lifetime removes the body -- see below.
        //   gravity               now APPLIES, and is inert: driveMarker overwrites the velocity
        //                         before the arrow's own gravity can reach a move.

        // *** LOAD-BEARING, AND MORE SO THAN IT WAS. ***
        //
        // playerTouch's guard is `isInGround() OR isNoPhysics()`. Under the old switch the SECOND
        // disjunct was always true, so the body was pickable in MID-AIR and this line was what
        // refused it. Now the second disjunct is always FALSE and the FIRST one goes live the instant
        // the body sticks in a block: a stuck arrow is a pickable arrow, and without this line every
        // bolt that meets a wall mints a free arrow for whoever walks into it.
        //
        // The hazard moved from mid-air to in-ground. It did not go away.
        body.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

        // Belt on the same trousers: a body that somehow resolved a hit would deal zero. These are
        // not the mechanism -- onPlumeBodyHit is -- and they are kept because the mechanism is a
        // cancelled event and a cancelled event is one listener registration away from not happening.
        body.setDamage(0.0);
        body.setKnockbackStrength(0);
        body.setCritical(false);

        // No shooter. castRay owns every hit; an owner would only give vanilla a reason to treat this
        // as somebody's arrow in whatever path we have not read.
        body.setShooter(null);
        body.setPersistent(false);              // unload backstop, exactly as configureMarker

        // The same tag every other marker carries, and the reason markerOf() below can find this
        // entity without knowing which kind it is. *** AND onPlumeBodyHit READS IT ***, so it is now
        // load-bearing for the hit cancel as well as for the lookup.
        body.getPersistentDataContainer()
                .set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);

        // *** ARMED SELF-DESTRUCT, AND IT IS NOW THE STICK CLEANUP TOO. ***
        //
        // shoot() sets life = 0, so this must come AFTER the spawn rather than inside a consumer.
        //
        // READ BEFORE CHOOSING, rather than reaching for a poll: when a driven arrow sticks,
        // AbstractArrow.tick takes the in-ground branch, calls tickDespawn() and RETURNS EARLY --
        // before the move, before the rotation, before the inertia. tickDespawn is `life++; if (life
        // >= <config despawn rate>) discard(DESPAWN)`, and life is armed at Integer.MAX_VALUE - 1,
        // so the FIRST in-ground tick discards the body whatever that config says.
        //
        // AND DRIVING DOES NOT UN-STICK IT: CraftEntity.setVelocity is a plain setDeltaMovement plus
        // hurtMarked, and it does not touch inGround. Only lerpMotion and startFalling clear that,
        // and we call neither.
        //
        // SO NO STICK-CLEANUP CODE IS NEEDED. The body is visible in the wall for ONE TICK. A poll,
        // a listener or a scheduled sweep would all be a second mechanism for something the armed
        // lifetime already does in one tick, and the second mechanism is the one that rots.
        body.setLifetimeTicks(ARMED_ARROW_LIFETIME);
        return body.getUniqueId();
    }

    /**
     * A bolt body of the authored kind. <b>This switch and {@code AbilitySchema.parseBody}'s set are
     * one list in two files</b>: the schema refuses any id that is not here, so the {@code default}
     * arm is reachable only by a kind added to the schema and not to this switch, and it throws on
     * the cast rather than flying a bolt with no body.
     *
     * <p>{@code expectedLifetimeTicks} is not read by either kind. The arrow arms its own lifetime
     * ({@link #ARMED_ARROW_LIFETIME}); the fireball has no vanilla timer to arm at all (see
     * {@link #spawnFireballBody}), so its normal end is the flight's fuse calling {@link #removeMarker}.
     */
    @Override
    public UUID spawnBoltMarker(Vec3 at, Vec3 velocity, int expectedLifetimeTicks, String body) {
        return switch (body) {
            case "arrow" -> spawnArrowBody(at, velocity);
            case "fireball" -> spawnFireballBody(at, velocity);
            default -> throw new IllegalArgumentException("no bolt body named '" + body
                    + "' -- AbilitySchema.parseBody accepted an id this adapter cannot build");
        };
    }

    /**
     * The Blaze King's Staff's fireball (LEGACY-B): a real {@code SmallFireball}, DRIVEN along the path
     * core computes, with core's {@code castRay} owning every hit (Q-B2, the seat: option 1-prime).
     * {@code PLAN-legacy-b.md} sections 2 to 5 carry the jar readings this cites.
     *
     * <h2>WHAT THE BODY WOULD DO ON ITS OWN, AND WHAT STOPS EACH</h2>
     *
     * <pre>
     *   vanilla 5 damage + 5 s ignite on an entity   RpgListeners.onPlumeBodyHit cancels the entity hit
     *                                                 (backstops: onPlumeBodyDamage, onMarkerBodyCombust)
     *   TNT primed, campfire lit, bell, target        onPlumeBodyHit cancels the BLOCK hit too, for a
     *                                                 fireball only (the arrow must still stick)
     *   fire placed on a block                        setIsIncendiary(false), below
     *                                                 (backstop: RpgListeners.onMarkerBodyIgnite)
     *   speeding up toward 1.9 blocks a tick          driveMarker's compensated velocity (FireballDrive)
     *   explosion                                     nothing to stop: only LargeFireball explodes
     *   punched back                                  nothing to stop: small_fireball is not in the
     *                                                 redirectable_projectile tag
     *   gravity                                       nothing to stop: the tick never applies it
     * </pre>
     *
     * <h2>*** setIsIncendiary(false) IS LOAD-BEARING AND WILL READ AS REDUNDANT BESIDE THE CANCEL ***</h2>
     *
     * <p>{@code SmallFireball.onHitBlock} places fire when {@code isIncendiary}, and that branch is
     * <b>not</b> gated on {@code hitCancelled}: a cancelled block hit still reaches it (read from the
     * jar, PLAN section 3). Cancelling the hit event stops TNT and campfires and does NOT stop the fire.
     * The default is {@code true} for a fireball with no Mob owner, which ours is. <b>Its sole witness
     * is gate row LB5</b>: no unit test can see a fire block.
     *
     * <h2>NO SHOOTER, NOT PERSISTENT, TAGGED -- AS THE ARROW</h2>
     *
     * <p>{@code setShooter(null)}: castRay owns every hit, and an owner would only give vanilla a reason
     * to treat this as somebody's fireball. It also means the owner-removed despawn in
     * {@code AbstractHurtingProjectile.tick} never fires. The tag is what {@link #markerOf}, the hit
     * cancel and both backstops read.
     *
     * <h2>NO VANILLA TIMER, SO THE ORPHAN FLIES (Q-B5)</h2>
     *
     * <p>A hurting projectile has no lifetime: it is discarded only at its first block or entity
     * contact, when its chunk is not loaded, or when its owner is removed. <b>The normal end is the
     * flight's fuse</b> ({@code max_lifetime_ticks}, 160 on the staff), which calls
     * {@link #removeMarker}. An orphan -- a step chain that never reaches its fuse, as when a region
     * unloads mid-flight -- keeps flying until a block, an unloaded chunk or a save, which
     * {@code setPersistent(false)} covers. The seat accepted that (Q-B5); {@code NEXT.md} records it.
     *
     * <p><b>NOT {@code launchProjectile}</b>: that makes the player the owner. {@code world.spawn} with a
     * consumer configures the body BEFORE it is added to the world, so no event can see it untagged.
     * The velocity is the compensated drive, the same one {@link #driveMarker} will set on this frame.
     */
    private UUID spawnFireballBody(Vec3 at, Vec3 velocity) {
        Vec3 drive = FireballDrive.velocityFor(velocity);
        SmallFireball body = world.spawn(toLocation(at), SmallFireball.class, fireball -> {
            fireball.setIsIncendiary(false);   // LOAD-BEARING: see the javadoc. Sole witness LB5.
            fireball.setShooter(null);
            fireball.setPersistent(false);
            fireball.getPersistentDataContainer()
                    .set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);
            fireball.setVelocity(new Vector(drive.x(), drive.y(), drive.z()));
        });
        return body.getUniqueId();
    }

    /**
     * Drive a marker: hand the platform's own mover this tick's displacement and let IT move the
     * entity. Deliberately NOT a reposition.
     *
     * <p><b>REPOSITIONING WAS TRIED AND IS NOT AVAILABLE.</b> {@code teleportAsync} was verified to
     * move this entity server-side -- 23 repositions, zero target-vs-actual mismatches, corroborated
     * by {@link #removeMarker}'s independent read finding it at the final target -- and verified NOT
     * to reach the client's entity tracker: a straight-up shot, the one flight where the body
     * decelerates to nearly nothing around 20 blocks and hangs there, showed nothing at all. The
     * MECHANISM behind that is unknown and no guess about it belongs in this file.
     *
     * <p>Scope of that finding, stated narrowly on purpose: observed for an <b>Item</b> entity with
     * gravity disabled, spawned via {@code World#spawn}, repositioned per-tick and per-4-ticks, on
     * the pinned Paper build. NOT established for other entity types, other spawn paths, or Folia.
     * A finding stated wider than its evidence is how this class's own {@code non-despawning} claim
     * happened.
     *
     * <p>Setting the velocity instead routes the body through {@code move(MoverType.SELF, …)}, which
     * is the path every ordinary thrown item uses and the only one observed to render -- the same
     * mechanism that makes {@code throw_embers}' blaze powder visibly fly and spin.
     *
     * <p><b>Why this lands exactly on the computed path.</b> The caller's vector is one tick's
     * displacement, already carrying the ability's own gravity. {@link #spawnMarker} disables the
     * entity's gravity, and {@code Entity.getGravity()} returns 0 when it is disabled, so
     * {@code applyGravity()} adds nothing before the move. Vanilla's drag is applied AFTER the move,
     * so overwriting the velocity next tick discards it before it can matter. The displacement is
     * therefore precisely what was asked for -- not corrected toward it.
     *
     * <p><b>Only legal on the thread owning WHERE THE MARKER IS</b>, the same unhopped contract
     * {@link #removeMarker} and {@link #markerLocation} keep. It is an entity write like any other.
     * {@code ProjectileFlight} places the call at the END of a step for that reason; do not move it.
     *
     * <p><b>THIS MECHANISM IS PHASE-SENSITIVE, AND THE PHASE IS VERIFIED ON ONE OF TWO TARGET
     * PLATFORMS.</b> Setting a velocity is only correct if the platform moves the entity by it on
     * the SAME tick it is set; if the caller ran after the entity had already ticked, the body would
     * sit exactly one computed step behind the flight, permanently. That is not divergence -- it
     * does not accumulate or grow with range -- and no unit test can see it, because
     * {@code FakeWorld} has no tick order.
     *
     * <p>On Paper it is verified from the jar: {@code PaperScheduler.onRegionLater} reaches
     * {@code GlobalRegionScheduler}, and in {@code MinecraftServer.tickChildren} the scheduler ticks
     * at bytecode offset 37 while {@code ServerLevel.tick} runs at offset 431 -- same server tick,
     * scheduler first, so the velocity is consumed later that tick.
     *
     * <p><b>On Folia it is UNESTABLISHED.</b> Region tasks run on per-region threads there and that
     * ordering does not carry over. The predecessor mechanism -- repositioning -- was not
     * phase-sensitive at all, so this is a property the design ACQUIRED, knowingly, on the strength
     * of a check that covers one platform. Re-establish it before trusting this on Folia.
     */
    /**
     * The entity behind a marker id, whatever KIND of marker it is, or null if it is gone.
     *
     * <h2>*** THIS EXISTS BECAUSE {@code instanceof Item} WAS THE GATE ON ALL THREE MARKER
     * OPERATIONS, AND AN ARROW BODY IS NOT AN {@code Item} ***</h2>
     *
     * <p>{@link #driveMarker}, {@link #removeMarker} and {@link #markerLocation} each opened with
     * {@code if (world.getEntity(markerId) instanceof Item marker)}. That test was correct while
     * there was one marker kind and <b>silently false</b> for the arrow body, which is the same
     * failure shape as {@code ProjectileFlight}'s {@code look.item() == null} gate -- a check whose
     * meaning quietly narrowed when a second kind arrived.
     *
     * <p><b>The three failures are not equally visible, and the invisible one is the worst.</b>
     * A no-op {@code driveMarker} is a body that never gets its velocity renewed -- noticeable, and
     * it still drifts roughly along the path. A no-op {@code removeMarker} is <b>a body that is
     * never cleaned up on a NORMAL resolve</b>, so every shot leaks one for the full orphan window,
     * and nothing anywhere goes red.
     *
     * <p><b>Matched on the TAG rather than widened to {@code Entity}.</b> Widening would let any id
     * that happened to name an entity be driven or removed; the tag is set by
     * {@link #configureMarker} and by {@link #spawnBoltMarker} and by nothing else, so it is a
     * STRICTLY stronger guard than the type test it replaces, not a looser one. A third marker kind
     * that forgets the tag fails loudly at its first drive rather than half-working.
     */
    private Entity markerOf(UUID markerId) {
        Entity entity = world.getEntity(markerId);
        if (entity == null) return null;
        return entity.getPersistentDataContainer()
                .has(ctx.keys().markerEntity, PersistentDataType.BYTE) ? entity : null;
    }

    /**
     * THE SCALED ITEM BODY (Solar Flare, the seat's F9, 2026-11-27): an {@code ItemDisplay} carrying the item at the
     * scale, made as the Lapis orb's {@code BlockDisplay} is ({@link #spawnOrbBlock}, READ there): scaled by its
     * transformation (an item display is drawn about its position, so no translation), eased over one tick by
     * {@code setTeleportDuration(1)}, never saved ({@code setPersistent(false)}), and tagged {@code markerEntity} AFTER the
     * add, so {@link #markerOf} finds it and the selftest's clean counts and removes a leaked one. A display has no
     * physics: it is PLACED each step ({@link #driveMarker}), and it cannot be picked up, burned or despawned, so the
     * flight's own end (or its run's) is the only thing that removes it. Its expected lifetime is not needed for that.
     * An unknown item falls back as {@link #configureMarker}'s does.
     */
    @Override
    public UUID spawnDisplayMarker(Vec3 at, String itemId, double scale, int expectedLifetimeTicks) {
        Material material = Material.matchMaterial(itemId);
        if (material == null || !material.isItem()) {
            ctx.warnOnce("Unknown marker material '" + itemId + "'; using BLAZE_POWDER");
            material = Material.BLAZE_POWDER;
        }
        ItemStack stack = new ItemStack(material);
        float s = (float) scale;
        org.bukkit.entity.ItemDisplay body = world.spawn(toLocation(at), org.bukkit.entity.ItemDisplay.class, d -> {
            d.setItemStack(stack);
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(),
                    new org.joml.AxisAngle4f(), new org.joml.Vector3f(s, s, s), new org.joml.AxisAngle4f()));
            d.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            d.setTeleportDuration(1);
            d.setPersistent(false);
        });
        body.getPersistentDataContainer().set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);
        return body.getUniqueId();
    }

    @Override
    public void driveMarker(UUID markerId, Vec3 stepVelocity) {
        Entity marker = markerOf(markerId);
        // A DISPLAY IS PLACED, NOT PUSHED (F9): it has no mover, so it is put where the step ends, eased over the tick.
        if (marker instanceof org.bukkit.entity.Display display) {
            display.teleport(display.getLocation().add(stepVelocity.x(), stepVelocity.y(), stepVelocity.z()));
            return;
        }
        if (marker != null) {
            // A FIREBALL'S OWN TICK PUSHES AND DRAGS BEFORE IT MOVES, so it is handed the velocity
            // that its applyInertia turns back into this step (FireballDrive; Q-B3 COMPENSATE). An
            // arrow and an item drag AFTER the move, so they take the step as it is.
            Vec3 drive = marker instanceof SmallFireball
                    ? FireballDrive.velocityFor(stepVelocity) : stepVelocity;
            marker.setVelocity(new Vector(drive.x(), drive.y(), drive.z()));
        }
        // Silently absent is CORRECT here and is a reachable state, not a defensive one: a driven
        // ITEM body is a fully participating item entity, and fire, lava and cactus destroy those.
        // An ARROW body has a shorter list -- see spawnBoltMarker -- but checkBelowWorld still
        // reaches it. Either way the flight continues and resolves normally with no body.
    }

    @Override
    public void removeMarker(UUID markerId) {
        Entity marker = markerOf(markerId);
        if (marker != null) {
            marker.remove();
        }
    }

    /**
     * The marker's live location, so a fuse can detonate where the thrown item actually IS at
     * fuse-end -- wherever physics carried it -- rather than where it was thrown. Empty when the
     * item is gone (removed, or unloaded with its chunk), which sends the fuse back to its throw
     * origin. A read of the entity's own position; getEntity mirrors removeMarker above.
     */
    @Override
    public Optional<Vec3> markerLocation(UUID markerId) {
        Entity marker = markerOf(markerId);
        if (marker != null) {
            Location loc = marker.getLocation();
            return Optional.of(new Vec3(loc.getX(), loc.getY(), loc.getZ()));
        }
        return Optional.empty();
    }

    /**
     * Play a named visual at a point. An unknown id is a content mistake, not a
     * programming error: warn once and let the rest of the detonation land.
     *
     * The onRegion hop is not redundant. Callers reach here already on a region
     * thread, but not necessarily the one owning {@code at}: RpgCommand hops onto
     * the region of the caster's EYE, and a Ray can land its impact thirty blocks
     * away, in another region. spawnParticle and playSound are world writes and
     * are only legal on the thread owning this location.
     */
    @Override
    public void present(Vec3 at, String visualId) {
        present(at, visualId, Double.NaN);
    }

    /**
     * {@link #present}, with a {@code spiral} step drawn at {@code radius} (NaN: at its own authored radius). The void
     * implosion (2026-11-17) passes its reach, so a stage's swirl is its own reach wide. Every other step is drawn as
     * {@link #present} always drew it, on the same region hop, so the sound lands with the swirl's first tick.
     */
    @Override
    public void present(Vec3 at, String visualId, double radius) {
        VisualDefinition visual = ctx.visuals().find(visualId).orElse(null);
        if (visual == null) {
            ctx.warnOnce("Unknown visual_id '" + visualId + "'; nothing presented");
            return;
        }
        Location loc = toLocation(at);
        ctx.scheduler().onRegion(loc, () -> {
            for (VisualSpec step : visual.steps()) {
                switch (step) {
                    case VisualSpec.Particles p -> particles(p, loc);
                    case VisualSpec.Sound s ->
                            world.playSound(loc, s.key(), s.volume(), s.pitch());
                    case VisualSpec.Spiral sp -> {
                        double r = Double.isNaN(radius) ? sp.radius() : radius;
                        // The swirl's width AS THE SERVER DRAWS IT, for a gate (the void implosion: a stage's reach).
                        ctx.log().info(String.format(java.util.Locale.ROOT,
                                "SWIRL visual=%s radius=%.3f arms=%d ticks=%d at=%.2f %.2f %.2f", visualId, r,
                                sp.shape().arms(), sp.shape().durationTicks(), loc.getX(), loc.getY(), loc.getZ()));
                        spiral(sp, loc, r, 0);
                    }
                }
            }
        });
    }

    /**
     * ONE TICK OF A SPIRAL, then the next a tick later on the same region, until its last tick (core's
     * {@link io.github.butterflysmp.rpg.core.shape.Spiral#arm} decides every point). One particle per point, no spread,
     * {@code extra} 0, so a DUST particle sits where it is drawn. The origin's region owns every point only while the
     * radius stays inside one region; an implosion's 2 to 6 blocks do on Paper, where every entity and block is on the
     * main thread (on Folia a swirl across a region edge would draw off-thread there: the same note as F3).
     */
    private void spiral(VisualSpec.Spiral sp, Location origin, double radius, int tick) {
        io.github.butterflysmp.rpg.core.shape.Spiral shape = sp.shape();
        double y = origin.getY() + sp.height();
        for (int arm = 0; arm < shape.arms(); arm++) {
            for (Vec3 off : shape.arm(radius, tick, arm)) {
                world.spawnParticle(sp.particle(), origin.getX() + off.x(), y, origin.getZ() + off.z(), 1,
                        0.0, 0.0, 0.0, 0.0, sp.dust());
            }
        }
        if (tick + 1 < shape.durationTicks()) {
            // Through schedule(), the world's ONE point-scheduling site (C2): registered with its point, so the
            // selftest's LINGER counts a swirl still drawing and a row-start clean ends it.
            schedule(new Vec3(origin.getX(), origin.getY(), origin.getZ()), 1,
                    () -> spiral(sp, origin, radius, tick + 1));
        }
    }

    /**
     * Draw a named visual ALONG a segment. A beam.
     *
     * <p><b>ONE REGION HOP FOR THE WHOLE SEGMENT.</b> {@link #present} hops per call, so drawing a
     * 26-block beam by calling it a hundred times would be a hundred hops in one tick. This hops
     * once, and that is legal only because {@code CastExecutor} hands it a segment already bounded
     * by chunk planes: the segment lies inside one chunk column by construction, and a column
     * belongs to exactly one region. See CombatWorld.presentAlong for the full argument.
     *
     * <p>The hop is on {@code from}, which is the end the caller is already standing on -- it is
     * where {@code castRay} just traced from, on this same thread.
     *
     * <p><b>SOUND STEPS ARE NOT PLAYED HERE, AND THAT IS NOT AN OVERSIGHT.</b> This runs once per
     * chunk-column SEGMENT, so a sound inside a beam visual would play one to three times depending
     * on how many chunk planes the aim happened to cross -- its loudness would depend on which way
     * the player was facing, intermittently. ContentValidator rejects a Sound step in any visual
     * named as a beam, so reaching this switch with one is already a content error that was
     * reported at boot; ignoring it here is the quiet half of that same refusal.
     */
    @Override
    public void presentAlong(Vec3 from, Vec3 to, String visualId) {
        VisualDefinition visual = ctx.visuals().find(visualId).orElse(null);
        if (visual == null) {
            ctx.warnOnce("Unknown visual_id '" + visualId + "'; no beam drawn");
            return;
        }
        ctx.scheduler().onRegion(toLocation(from), () -> {
            for (VisualSpec step : visual.steps()) {
                if (!(step instanceof VisualSpec.Particles p)) continue;
                for (Vec3 point : BeamSamples.along(from, to, p.samplesPerBlock())) {
                    particles(p, toLocation(point));
                }
            }
        });
    }

    /**
     * One particle step, at one point. Shared by {@link #present} and {@link #presentAlong} so a
     * beam and a burst cannot drift in how they read the same authored fields.
     *
     * <p>The 8-arg overload: offsets, then {@code extra}, then the DATA OBJECT. Moving here from
     * the 7-arg one was provably a no-op for every visual that takes no data, rather than an argued
     * one -- in the pinned API the 7-arg default chain is literally
     * {@code spawnParticle(..., extra, null)}. See VisualSpec.Particles.
     *
     * <p><b>AND {@code p.dust()} IS NO LONGER NULL FOR EVERYTHING SHIPPED, WHICH IS WHY THIS
     * PARAGRAPH IS NOT THE ONE IT USED TO BE.</b> It read "{@code p.dust()} is null for all five
     * particles in shipped content", which was true when the overload changed and is false now.
     * Measured 2026-09-24, needle anchored to the YAML key so prose naming DUST is not counted as
     * authoring it: <b>seven</b> DUST steps ship in {@code content/visuals/} -- five beams, and
     * {@code emerald_impact} and {@code lapis_impact} on the POINT path, which is this method's
     * other caller. So the data object reaching {@link #present} is load-bearing today, and a
     * reader must not conclude from this javadoc that nothing exercises it.
     */
    private void particles(VisualSpec.Particles p, Location at) {
        world.spawnParticle(p.particle(), at, p.count(),
                p.spread(), p.spread(), p.spread(), p.speed(), p.dust());
    }
}
