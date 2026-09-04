package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.paper.content.VisualDefinition;
import io.github.butterflysmp.rpg.paper.content.VisualSpec;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
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
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(e -> BukkitCombatant.of(e, ctx)) // snapshot taken here, on this thread
                .toList();
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
    public Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId) {
        Vec3 along = to.subtract(from);
        double distance = along.length();
        if (distance <= 0) return Optional.empty();

        Vector direction = new Vector(along.x(), along.y(), along.z()).normalize();
        RayTraceResult result = world.rayTrace(
                toLocation(from), direction, distance,
                FluidCollisionMode.NEVER, /* ignorePassableBlocks */ true, RAY_SIZE,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(ignoreId));

        if (result == null) return Optional.empty();

        Vector hit = result.getHitPosition();
        Vec3 point = new Vec3(hit.getX(), hit.getY(), hit.getZ());

        if (result.getHitEntity() instanceof LivingEntity living) {
            return Optional.of(RayHit.ofCombatant(point, BukkitCombatant.of(living, ctx)));
        }
        return Optional.of(RayHit.ofBlock(point));
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
        ctx.scheduler().onRegionLater(toLocation(near), task, delayTicks);
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
        item.setPickupDelay(Integer.MAX_VALUE);  // never collectible, never merges
        item.setPersistent(false);               // unload backstop

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
     */
    @Override
    public void driveMarker(UUID markerId, Vec3 stepVelocity) {
        if (world.getEntity(markerId) instanceof Item marker) {
            marker.setVelocity(new Vector(stepVelocity.x(), stepVelocity.y(), stepVelocity.z()));
        }
        // Silently absent is CORRECT here and is a reachable state, not a defensive one: a driven
        // body is a fully participating item entity, and fire, lava and cactus destroy those. The
        // flight continues and resolves normally with no body -- see spawnMarker's interaction note.
    }

    @Override
    public void removeMarker(UUID markerId) {
        if (world.getEntity(markerId) instanceof Item marker) {
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
        if (world.getEntity(markerId) instanceof Item marker) {
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
        VisualDefinition visual = ctx.visuals().find(visualId).orElse(null);
        if (visual == null) {
            ctx.warnOnce("Unknown visual_id '" + visualId + "'; nothing presented");
            return;
        }
        Location loc = toLocation(at);
        ctx.scheduler().onRegion(loc, () -> {
            for (VisualSpec step : visual.steps()) {
                switch (step) {
                    // The 7-arg overload: offsets, then `extra`. The 6-arg one this used to call
                    // hardcoded extra = 1.0 (its default chain ends at dconst_1), so passing
                    // p.speed() with a 1.0 loader default leaves every existing visual identical
                    // while letting a file ask for something else. See VisualSpec.Particles.
                    case VisualSpec.Particles p ->
                            world.spawnParticle(p.particle(), loc, p.count(),
                                    p.spread(), p.spread(), p.spread(), p.speed());
                    case VisualSpec.Sound s ->
                            world.playSound(loc, s.key(), s.volume(), s.pitch());
                }
            }
        });
    }
}
