package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.BeamSamples;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.paper.content.VisualDefinition;
import io.github.butterflysmp.rpg.paper.content.VisualSpec;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
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
        Location eye = living.getEyeLocation();
        Vector direction = eye.getDirection();
        return Optional.of(new Aim(
                new Vec3(eye.getX(), eye.getY(), eye.getZ()),
                new Vec3(direction.getX(), direction.getY(), direction.getZ())));
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
    @Override
    public UUID spawnBoltMarker(Vec3 at, Vec3 velocity, int expectedLifetimeTicks) {
        Arrow body = world.spawn(toLocation(at), Arrow.class, arrow -> {
            // THE ONE SWITCH. Block collision, entity collision, ProjectileHitEvent, in-ground
            // sticking and gravity, all off together -- see this method's javadoc for the gate.
            arrow.setNoPhysics(true);

            // NOT redundant with the line above, and not a substitute for it either: applyGravity
            // is suppressed by BOTH, and neither alone should have to be right.
            arrow.setGravity(false);

            // *** LOAD-BEARING. playerTouch's guard is `isInGround() OR isNoPhysics()`, so the line
            // above just OPENED a mid-air pickup path that no ordinary arrow has. Deleting this as
            // redundant mints a free arrow per shot. ***
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

            // Belt on the same trousers: a body that somehow resolved a hit would deal zero.
            arrow.setDamage(0.0);
            arrow.setKnockbackStrength(0);
            arrow.setCritical(false);

            // No shooter. castRay owns every hit; an owner would only give vanilla a reason to
            // treat this as somebody's arrow in whatever path we have not read.
            arrow.setShooter(null);
            arrow.setPersistent(false);              // unload backstop, exactly as configureMarker

            // THE LAUNCH VELOCITY IS PART OF CREATING THIS BODY, NOT SOMETHING DONE TO IT AFTER.
            // An arrow's rotation is derived from its own deltaMovement inside its own tick, so a
            // body spawned still has no direction on its first frame and visibly snaps into line a
            // tick later. This is the exact inverse of configureMarker's zeroing rule, which is
            // right for items and wrong here -- see CombatWorld.spawnBoltMarker.
            arrow.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z()));

            // The same tag every other marker carries, and the reason markerOf() below can find
            // this entity without knowing which kind it is.
            arrow.getPersistentDataContainer()
                    .set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);

            // ARMED SELF-DESTRUCT -- config-independent by construction. See ARMED_ARROW_LIFETIME.
            // expectedLifetimeTicks is deliberately NOT read: unlike the item path there is no
            // arithmetic to do, because we cannot influence WHEN the first despawn tick arrives,
            // only that it is fatal when it does.
            arrow.setLifetimeTicks(ARMED_ARROW_LIFETIME);
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

    @Override
    public void driveMarker(UUID markerId, Vec3 stepVelocity) {
        Entity marker = markerOf(markerId);
        if (marker != null) {
            marker.setVelocity(new Vector(stepVelocity.x(), stepVelocity.y(), stepVelocity.z()));
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
                }
            }
        });
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
     * the 7-arg one is provably a no-op for every visual that takes no data, rather than an argued
     * one -- in the pinned API the 7-arg default chain is literally
     * {@code spawnParticle(..., extra, null)}, and {@code p.dust()} is null for all five particles
     * in shipped content. See VisualSpec.Particles.
     */
    private void particles(VisualSpec.Particles p, Location at) {
        world.spawnParticle(p.particle(), at, p.count(),
                p.spread(), p.spread(), p.spread(), p.speed(), p.dust());
    }
}
