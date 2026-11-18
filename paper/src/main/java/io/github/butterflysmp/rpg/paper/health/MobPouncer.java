package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobGearScore;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobPounce;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.mob.PounceCycle;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * THE SKREEL AND ITS MOUNT, the Bukkit half (PLAN-dragon-mobs.md step R8; the plan's MB5). The rules are core's: the
 * cycle ({@link PounceCycle}), the numbers, the leap, the landing and the push ({@link MobPounce}), the predicate
 * ({@link MobHostility}). This class makes the pair, finds the target, supplies positions, applies results, and ends the
 * pair when either half is gone.
 *
 * <h2>WHO STEERS, READ FROM THE PINNED SERVER JAR</h2>
 * {@code javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar net.minecraft.world.entity.Mob}:
 * <ul>
 *   <li>{@code Mob#getControllingPassenger}: the FIRST passenger, when it is a {@code Mob} that
 *       {@code canControlVehicle} (true unless its type is tagged {@code NON_CONTROLLING_RIDER}) and the VEHICLE is not
 *       NoAI. So the Skreel steers its mount, and a FROZEN mount is steered by nobody.</li>
 *   <li>{@code Mob#getNavigation} and {@code Mob#getMoveControl}: a mob whose controlled vehicle is a {@code Mob}
 *       returns THE VEHICLE'S. So the rider's goals move the mount through the mount's own navigation.</li>
 *   <li>{@code MoveControl#tick} sets the speed from {@code this.mob.getAttributeValue(MOVEMENT_SPEED)}, its mob being
 *       the vehicle, so <b>the MOUNT's movement speed moves the pair</b>, which is where the ridden 1.3x and the
 *       wind-up's x0 go.</li>
 *   <li>{@code Mob#updateControlFlags}: a mob whose controlling passenger is a {@code Mob} has its MOVE, JUMP and LOOK
 *       goals switched off. So while ridden, the spider's own {@code LeapAtTargetGoal} (flags JUMP and MOVE, READ:
 *       {@code net.minecraft.world.entity.ai.goal.LeapAtTargetGoal.<init>}), {@code Spider$SpiderAttackGoal} and
 *       {@code WaterAvoidingRandomStrollGoal} cannot run.</li>
 * </ul>
 *
 * <h2>THE VANILLA GOALS THAT WOULD FIGHT THE CYCLE, AND HOW EACH IS BLOCKED</h2>
 * Read from {@code Spider#registerGoals} and {@code AbstractSkeleton#registerGoals} / {@code #reassessWeaponGoal}:
 * <ul>
 *   <li>the SPIDER'S {@code LeapAtTargetGoal(0.4)} (its own hop at a target) and its {@code SpiderAttackGoal}: switched
 *       off by the control flags above while it is ridden, and they need a target, which is blocked below;</li>
 *   <li>the RIDER'S {@code MeleeAttackGoal} (any main-hand item but a bow, {@code reassessWeaponGoal}): needs a target;</li>
 *   <li>every TARGET goal on both ({@code HurtByTargetGoal}, {@code NearestAttackableTargetGoal}, the spider's
 *       {@code SpiderTargetGoal}): each sets its target through the event, and {@link #blocksTarget} cancels every
 *       {@code EntityTargetEvent} of a paired mob that names a target ({@code RpgListeners.onPairTargeted}), so neither
 *       ever has a target while paired. cfde822 nulled both targets every tick (READ); the event does it at the source.
 *       <b>Nothing is removed</b>: a goal removed from a live mob comes back on a reload, and a survivor must fight as a
 *       plain mob again, so blocking the target is the one rule that needs no undoing.</li>
 *   <li>the rider's {@code WaterAvoidingRandomStrollGoal}, {@code LookAtPlayerGoal}, {@code RandomLookAroundGoal}: not
 *       blocked. A stroll can set a path through the mount's navigation; the cycle re-paths or stops the path every tick
 *       it moves or roots, and the root's x0 speed holds it still whatever the path says.</li>
 * </ul>
 *
 * <h2>ONE LOOP PER PAIR, ON THE MOUNT'S SCHEDULER, AND IT DIES WITH THE PAIR</h2>
 * The mount is the body that moves (its navigation, its speed, its velocity, its ground), and the rider rides with it,
 * so the loop is a {@link RepeatingTask} of period 1 on the MOUNT'S entity scheduler ({@link EntityTaskTarget}), as the
 * Knell's and the Phalanx's are on theirs. It starts the tick after the pair is made, or after a paired mount appears
 * (a chunk load). It ends:
 * <ul>
 *   <li>the tick the mount is dead or invalid ({@code isActive} false: the body never runs again) and at its
 *       {@code EntityRemoveFromWorldEvent} ({@link #onRemove}: death, removal, unload), whichever is first;</li>
 *   <li>the tick it finds the rider dead, invalid, or no longer its passenger: the body returns false.</li>
 * </ul>
 * {@code onStop} runs on every ending: it removes the root, drops the pair from both maps, and when the MOUNT died with
 * its rider still on it, dismounts the rider where it stands. <b>No task outlives either mob</b>: the loop holds no task
 * but itself, and the victims' hits are one-shot.
 *
 * <h2>THE RIDDEN SPEED</h2>
 * {@code ridden_speed} (the mount's file) is a TRANSIENT {@code ADD_SCALAR} modifier on the mount's MOVEMENT_SPEED,
 * {@code ridden_speed - 1} of its base ({@code addTransientModifier}: never written to the entity's saved data, READ in
 * the pinned API's {@code AttributeInstance} javadoc), added when the pair is made and again whenever a paired or
 * widowed mount appears. It is never removed from a living mount: a widowed mount keeps it (the seat's reading of Ben's
 * "plain fast spider"). A mount spawned alone never has it.
 */
public final class MobPouncer {

    private static final class Pair {
        final Mob mount;
        final Mob rider;
        final String riderId;
        final String mountId;
        final MobPounce pounce;
        final PounceCycle cycle;
        RepeatingTask task;
        /** The point the current wind-up and leap aim at, fixed at the wind-up's first tick. */
        Vec3 aim;
        /** The instrument's aim for a forced wind-up, used on its first tick; null otherwise. */
        Vec3 forcedAim;
        /** The current leap's held horizontal velocity (y unused), set at the launch. */
        Vec3 held;
        Vec3 windupFrom;
        long launchTick;
        String brokenReason;
        Pair(Mob mount, Mob rider, String riderId, String mountId, MobPounce pounce) {
            this.mount = mount;
            this.rider = rider;
            this.riderId = riderId;
            this.mountId = mountId;
            this.pounce = pounce;
            this.cycle = new PounceCycle(pounce);
        }
    }

    private final Map<UUID, Pair> byMount = new ConcurrentHashMap<>();
    /** Every live pair's two uuids: the target block's set. */
    private final Set<UUID> paired = ConcurrentHashMap.newKeySet();
    private final Supplier<MobRegistry> mobs;
    private final AdapterContext adapters;

    public MobPouncer(Supplier<MobRegistry> mobs, AdapterContext adapters) {
        this.mobs = mobs;
        this.adapters = adapters;
    }

    // --- the pairing (called from the one spawn path) ---------------------------------------------------------------

    /**
     * MARK A MOUNT WITH ITS RIDER'S UUID, inside its pre-spawn consumer, BEFORE its add event: that event's
     * {@link #onAppear} sees the mark and looks again on the mount's next tick, by when the rider has mounted it. Written
     * any later, the add event would find no mark and the pair's loop would never start (only a reload would start it).
     */
    public static void mark(LivingEntity mount, UUID rider, Keys keys) {
        mount.getPersistentDataContainer().set(keys.mountRider, PersistentDataType.STRING, rider.toString());
    }

    /**
     * MAKE THE PAIR, on the spawning thread, the tick both are spawned: the rider becomes the mount's passenger and the
     * mount's ridden speed is added. The loop starts on the mount's next tick ({@link #mark}).
     */
    public static void pair(Mob mount, Mob rider, MobDefinition mountDef, Keys keys) {
        mount.addPassenger(rider);
        ridden(mount, mountDef, keys);
    }

    /** Add the mount's ridden-speed modifier, once (transient: never saved). No ridden speed authored, nothing. */
    static void ridden(Mob mount, MobDefinition mountDef, Keys keys) {
        if (mountDef == null || mountDef.riddenSpeed() == null) return;
        AttributeInstance speed = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null || speed.getModifier(keys.mountRidden) != null) return;
        speed.addTransientModifier(new AttributeModifier(keys.mountRidden, mountDef.riddenSpeed() - 1.0,
                AttributeModifier.Operation.ADD_SCALAR));
    }

    // --- appearance and removal -------------------------------------------------------------------------------------

    /** Any entity entered the world, on its own thread. A paired or widowed mount of ours is looked at on its next tick. */
    public void onAppear(Entity entity) {
        if (!(entity instanceof Mob mob)) return;
        var pdc = mob.getPersistentDataContainer();
        if (!pdc.has(adapters.keys().mountRider) && !pdc.has(adapters.keys().mountWidowed)) return;
        adapters.scheduler().onEntity(mob, () -> resume(mob));
    }

    private MobDefinition defOf(LivingEntity e) {
        String id = e.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        return id == null ? null : mobs.get().find(id).orElse(null);
    }

    private void resume(Mob mount) {
        if (!mount.isValid() || mount.isDead()) return;
        Keys keys = adapters.keys();
        MobDefinition mountDef = defOf(mount);
        var pdc = mount.getPersistentDataContainer();
        if (pdc.has(keys.mountWidowed)) {
            ridden(mount, mountDef, keys);
            return;
        }
        Pair running = byMount.get(mount.getUniqueId());
        if (running != null && running.task.isRunning()) return;
        String riderUuid = pdc.get(keys.mountRider, PersistentDataType.STRING);
        Mob rider = null;
        for (Entity p : mount.getPassengers()) {
            if (p instanceof Mob m && p.getUniqueId().toString().equals(riderUuid) && !m.isDead()) rider = m;
        }
        MobDefinition riderDef = rider == null ? null : defOf(rider);
        if (rider == null || riderDef == null || riderDef.pounce() == null) {
            // Its rider left while it was away (or its file no longer pounces): as if the rider had died.
            widow(mount, mountDef);
            adapters.log().info(String.format(Locale.ROOT, "SKREEL broken mount=%s rider=%s tick=%d reason=%s",
                    mount.getUniqueId(), riderUuid, Bukkit.getCurrentTick(), rider == null ? "rider-absent" : "no-pounce"));
            return;
        }
        start(mount, rider, riderDef, mountDef);
    }

    private void start(Mob mount, Mob rider, MobDefinition riderDef, MobDefinition mountDef) {
        ridden(mount, mountDef, adapters.keys());
        Pair p = new Pair(mount, rider, riderDef.id(), mountDef == null ? "?" : mountDef.id(), riderDef.pounce());
        p.task = RepeatingTask.start(new EntityTaskTarget(mount, adapters.scheduler(), adapters.log()), 1, "skreel-pounce",
                () -> step(p),
                () -> stopped(p));
        byMount.put(mount.getUniqueId(), p);
        paired.add(mount.getUniqueId());
        paired.add(rider.getUniqueId());
        AttributeInstance speed = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        adapters.log().info(String.format(Locale.ROOT,
                "SKREEL paired mount=%s rider=%s tick=%d gs=%d speed=%.4f base=%.4f",
                mount.getUniqueId(), rider.getUniqueId(), Bukkit.getCurrentTick(), gearScore(rider),
                speed == null ? Double.NaN : speed.getValue(), speed == null ? Double.NaN : speed.getBaseValue()));
    }

    /** Any entity left the world: a pair's loop on that mount stops. */
    public void onRemove(UUID id) {
        Pair p = byMount.get(id);
        if (p != null) p.task.cancel();
    }

    /** Is this mob half of a live pair? Its target events are then cancelled ({@code RpgListeners.onPairTargeted}). */
    public boolean blocksTarget(Entity e) {
        return paired.contains(e.getUniqueId());
    }

    private void widow(Mob mount, MobDefinition mountDef) {
        Keys keys = adapters.keys();
        var pdc = mount.getPersistentDataContainer();
        pdc.remove(keys.mountRider);
        pdc.set(keys.mountWidowed, PersistentDataType.BYTE, (byte) 1);
        ridden(mount, mountDef, keys);
    }

    private void stopped(Pair p) {
        byMount.remove(p.mount.getUniqueId(), p);
        paired.remove(p.mount.getUniqueId());
        paired.remove(p.rider.getUniqueId());
        if (p.mount.isValid()) unroot(p.mount);
        if (p.brokenReason != null) return;   // the body ended it and said why
        // The mount ended it: dead, removed or unloaded. A dead mount's rider dismounts where it is (vanilla keeps it
        // seated on the corpse until the body is removed, 20 ticks of death animation later, INFERRED from
        // LivingEntity#tickDeath); an unloaded pair is saved together and resumes on its next load.
        boolean mountDead = p.mount.isDead();
        String riderState = p.rider.isDead() ? "dead" : p.rider.isValid() ? "alive" : "gone";
        String at = "";
        if (mountDead && !p.rider.isDead() && p.rider.isValid() && p.mount.getPassengers().contains(p.rider)) {
            p.mount.removePassenger(p.rider);
            at = " dismounted=" + v(vec(p.rider.getLocation()));
        }
        adapters.log().info(String.format(Locale.ROOT, "SKREEL broken mount=%s rider=%s tick=%d reason=%s rider_state=%s%s",
                p.mount.getUniqueId(), p.rider.getUniqueId(), Bukkit.getCurrentTick(),
                mountDead ? "mount-died" : "mount-gone", riderState, at));
    }

    // --- the instrument -------------------------------------------------------------------------------------------

    private Pair pairOf(LivingEntity e) {
        Pair p = byMount.get(e.getUniqueId());
        if (p != null) return p;
        for (Pair q : byMount.values()) if (q.rider.getUniqueId().equals(e.getUniqueId())) return q;
        return null;
    }

    /**
     * THE INSTRUMENT: begin a wind-up on the pair's next tick, aimed at {@code aim}, whatever its clock, band, target or
     * AI. Called on the tagged mob's own thread (the mount's, or the rider's, which rides with it). Empty when it took.
     */
    public Optional<String> force(LivingEntity e, Vec3 aim) {
        Pair p = pairOf(e);
        if (p == null || !p.task.isRunning()) return Optional.of("no pair loop");
        if (!p.cycle.force()) return Optional.of("busy: " + p.cycle.stage() + " " + p.cycle.inStage());
        p.forcedAim = aim;
        return Optional.empty();
    }

    /** One PAIR line about this mob (either half): what it rides or carries, the cycle's stage, its speed. */
    public String probe(String tag, LivingEntity e) {
        Keys keys = adapters.keys();
        Pair p = pairOf(e);
        AttributeInstance speed = e.getAttribute(Attribute.MOVEMENT_SPEED);
        List<String> riders = new ArrayList<>();
        for (Entity r : e.getPassengers()) riders.add(r.getUniqueId().toString());
        Entity vehicle = e.getVehicle();
        boolean ridden = speed != null && speed.getModifier(keys.mountRidden) != null;
        boolean rooted = speed != null && speed.getModifier(keys.pounceRoot) != null;
        return String.format(Locale.ROOT,
                "PAIR tag=%s %s type=%s vehicle=%s passengers=%s stage=%s since=%s speed=%.4f base=%.4f ridden=%s"
                        + " rooted=%s widowed=%s ai=%s",
                tag, e.getUniqueId(), kind(e), vehicle == null ? "none" : vehicle.getUniqueId(), riders,
                p == null ? "none" : p.cycle.stage(), p == null ? "-" : String.valueOf(p.cycle.sinceWindup()),
                speed == null ? Double.NaN : speed.getValue(), speed == null ? Double.NaN : speed.getBaseValue(), ridden,
                rooted, e.getPersistentDataContainer().has(keys.mountWidowed),
                e instanceof Mob m && m.hasAI());
    }

    // --- the loop --------------------------------------------------------------------------------------------------

    /** One tick. False ends the loop (the pair broke). */
    private boolean step(Pair p) {
        Mob mount = p.mount;
        Mob rider = p.rider;
        String broke = null;
        if (rider.isDead()) broke = "rider-died";
        else if (!rider.isValid()) broke = "rider-gone";
        else if (!mount.getPassengers().contains(rider)) broke = "separated";
        if (broke != null) {
            p.brokenReason = broke;
            widow(mount, defOf(mount));
            unroot(mount);
            adapters.log().info(String.format(Locale.ROOT, "SKREEL broken mount=%s rider=%s tick=%d reason=%s",
                    mount.getUniqueId(), rider.getUniqueId(), Bukkit.getCurrentTick(), broke));
            return false;
        }
        Vec3 feet = vec(mount.getLocation());
        LivingEntity target = choose(p, feet);
        OptionalDouble d = target == null ? OptionalDouble.empty()
                : OptionalDouble.of(Math.sqrt(feet.distanceSquared(vec(target.getLocation()))));
        boolean forced = p.forcedAim != null && (p.cycle.stage() == PounceCycle.Stage.RECOVER
                || p.cycle.stage() == PounceCycle.Stage.HOLD);
        PounceCycle.Step s = p.cycle.tick(mount.hasAI(), d, mount.isOnGround());
        switch (s) {
            case STAND -> mount.getPathfinder().stopPathfinding();
            case CLOSE_IN -> {
                Vec3 to = p.pounce.goal(vec(target.getLocation()), feet, MobPounce.waypointTolerance(mount.getWidth()));
                mount.getPathfinder().moveTo(new Location(mount.getWorld(), to.x(), to.y(), to.z()), p.pounce.pathSpeed());
            }
            case BEGIN -> begin(p, forced, target, feet);
            case ROOT -> {
                mount.getPathfinder().stopPathfinding();
                if (p.cycle.inStage() % p.pounce.windupParticleEvery() == 0) warn(p);
            }
            case LAUNCH -> launch(p, feet);
            case FLY -> {
                mount.getPathfinder().stopPathfinding();
                mount.setVelocity(new Vector(p.held.x(), mount.getVelocity().getY(), p.held.z()));
                mount.setFallDistance(0f);
            }
            case LANDED -> land(p, feet, "landed");
            case TIMED_OUT -> land(p, feet, "timeout");
        }
        return true;
    }

    /** cfde822: "the nearest non-creative, non-spectator player within 50 blocks of the spider" (the seat's 32). */
    private LivingEntity choose(Pair p, Vec3 feet) {
        double r = p.pounce.acquireRange();
        LivingEntity best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (Entity e : p.mount.getWorld().getNearbyEntities(p.mount.getLocation(), r, r, r)) {
            if (!(e instanceof LivingEntity l) || l.equals(p.mount) || l.equals(p.rider) || !LiveTargets.isLive(l)) continue;
            if (!MobHostility.hostileTo(side(l), exempt(l))) continue;
            double d2 = feet.distanceSquared(vec(l.getLocation()));
            if (d2 > r * r || d2 >= bestD) continue;
            best = l;
            bestD = d2;
        }
        return best;
    }

    private void begin(Pair p, boolean forced, LivingEntity target, Vec3 feet) {
        Mob mount = p.mount;
        p.aim = forced && p.forcedAim != null ? p.forcedAim : vec(target.getLocation());
        p.forcedAim = null;
        p.windupFrom = feet;
        root(mount);
        warn(p);
        playSounds(mount, p.pounce.windupSounds());
        adapters.log().info(String.format(Locale.ROOT,
                "SKREEL windup rider=%s mount=%s tick=%d forced=%s at=%s target=%s aim=%s d=%.3f",
                p.rider.getUniqueId(), mount.getUniqueId(), Bukkit.getCurrentTick(), forced, v(feet),
                target == null ? "none" : kind(target) + " " + target.getUniqueId(), v(p.aim),
                Math.hypot(p.aim.x() - feet.x(), p.aim.z() - feet.z())));
    }

    private void warn(Pair p) {
        Location mid = p.mount.getLocation().add(0, p.mount.getHeight() * 0.5, 0);
        p.mount.getWorld().spawnParticle(Particle.valueOf(p.pounce.windupParticle().toUpperCase(Locale.ROOT)), mid,
                p.pounce.windupParticleCount(), 0.6, 0.4, 0.6, 0.1);
    }

    private void launch(Pair p, Vec3 feet) {
        Mob mount = p.mount;
        unroot(mount);
        AttributeInstance g = mount.getAttribute(Attribute.GRAVITY);
        double gravity = g == null ? 0.08 : g.getValue();
        MobPounce.Leap leap = p.pounce.leap(feet, p.aim, gravity);
        p.held = leap.velocity();
        p.launchTick = Bukkit.getCurrentTick();
        mount.setVelocity(new Vector(leap.velocity().x(), leap.velocity().y(), leap.velocity().z()));
        mount.setFallDistance(0f);
        playSounds(mount, p.pounce.launchSounds());
        adapters.log().info(String.format(Locale.ROOT,
                "SKREEL leap rider=%s mount=%s tick=%d from=%s windup_from=%s aim=%s v=%.4f %.4f %.4f ticks=%d"
                        + " horizontal=%.3f gravity=%.4f",
                p.rider.getUniqueId(), mount.getUniqueId(), p.launchTick, v(feet), v(p.windupFrom), v(p.aim),
                leap.velocity().x(), leap.velocity().y(), leap.velocity().z(), leap.ticks(), leap.horizontal(), gravity));
    }

    /**
     * THE LANDING HIT, where the mount's feet now are. Every combatant within the reach is hit through the mob-hit path's
     * final call ({@code applyDamage(dealt, rider)}, as the beam, the blast and the shot take it: defence applies, no
     * element, no crit) and pushed by {@code setVelocity(velocity + push)}, the blast's route, cut by its
     * KNOCKBACK_RESISTANCE. The rider is the damager (cfde822, READ: "with the rider as damager").
     */
    private void land(Pair p, Vec3 feet, String how) {
        Mob mount = p.mount;
        mount.setFallDistance(0f);
        World world = mount.getWorld();
        double r = p.pounce.reach();
        List<MobBlast.Bystander> bystanders = new ArrayList<>();
        Map<UUID, LivingEntity> bodies = new HashMap<>();
        for (Entity e : world.getNearbyEntities(mount.getLocation(), r, r, r)) {
            if (!(e instanceof LivingEntity l) || l.equals(mount) || l.equals(p.rider) || !LiveTargets.isLive(l)) continue;
            bystanders.add(new MobBlast.Bystander(l.getUniqueId(), side(l), exempt(l), l.getWorld().getName(),
                    vec(l.getLocation())));
            bodies.put(l.getUniqueId(), l);
        }
        MobPounce.Landing landing = p.pounce.landing(feet, world.getName(), bystanders);
        int gs = gearScore(p.rider);
        double dealt = p.pounce.price(MobClassifier.isHostile(p.rider), gs);
        UUID dealer = p.rider.getUniqueId();
        List<String> hit = new ArrayList<>();
        for (MobPounce.Struck s : landing.victims()) {
            LivingEntity victim = bodies.get(s.who().id());
            var attr = victim.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            double res = attr == null ? 0.0 : attr.getValue();
            Vec3 push = p.pounce.push(feet, vec(victim.getLocation()), res);
            Vector velocity = new Vector(push.x(), push.y(), push.z());
            Runnable strike = () -> {
                BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, dealer);
                victim.setVelocity(victim.getVelocity().add(velocity));
            };
            if (Bukkit.isOwnedByCurrentRegion(victim)) strike.run(); else adapters.scheduler().onEntity(victim, strike);
            hit.add(String.format(Locale.ROOT, "%s %s d=%.3f res=%.3f push=%.4f %.4f %.4f", kind(victim),
                    victim.getUniqueId(), s.distance(), res, push.x(), push.y(), push.z()));
        }
        List<String> spared = new ArrayList<>();
        for (MobPounce.Struck s : landing.spared()) {
            LivingEntity body = bodies.get(s.who().id());
            spared.add(String.format(Locale.ROOT, "%s %s d=%.3f", kind(body), body.getUniqueId(), s.distance()));
        }
        playSounds(mount, p.pounce.landSounds());
        long tick = Bukkit.getCurrentTick();
        adapters.log().info(String.format(Locale.ROOT,
                "SKREEL land rider=%s mount=%s tick=%d how=%s age=%d at=%s aim=%s off=%.3f gs=%d dealt=%.3f reach=%.2f"
                        + " victims=%d %s spared=%d %s",
                dealer, mount.getUniqueId(), tick, how, tick - p.launchTick, v(feet), v(p.aim),
                Math.hypot(feet.x() - p.aim.x(), feet.z() - p.aim.z()), gs, dealt, r, hit.size(), hit, spared.size(),
                spared));
        p.held = null;
    }

    // --- the root (the Knell's method, I11) -----------------------------------------------------------------------

    private void root(Mob mount) {
        mount.getPathfinder().stopPathfinding();
        Vector v = mount.getVelocity();
        mount.setVelocity(new Vector(0, v.getY(), 0));
        AttributeInstance speed = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(adapters.keys().pounceRoot) == null) {
            // x0: MULTIPLY_SCALAR_1 adds (factor - 1) of the total, so -1 is "times zero" (the Knell's).
            speed.addTransientModifier(new AttributeModifier(adapters.keys().pounceRoot, -1.0,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    private void unroot(Mob mount) {
        AttributeInstance speed = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(adapters.keys().pounceRoot) != null) {
            speed.removeModifier(adapters.keys().pounceRoot);
        }
    }

    // --- helpers ---------------------------------------------------------------------------------------------------

    private int gearScore(LivingEntity e) {
        Integer stored = e.getPersistentDataContainer().get(adapters.keys().mobGearScore, PersistentDataType.INTEGER);
        boolean valid = stored != null && MobGearScore.isValidStored(stored);
        if (!valid && MobClassifier.isHostile(e)) {
            adapters.warnOnce("SKREEL " + e.getUniqueId() + " has no valid gear score (stored " + stored
                    + "), so it is priced at GS " + MobScaling.GS_BASELINE);
        }
        return valid ? stored : MobScaling.GS_BASELINE;
    }

    private static void playSounds(Mob at, List<BeamSound> sounds) {
        for (BeamSound s : sounds) {
            at.getWorld().playSound(at.getLocation(), s.key(), SoundCategory.HOSTILE, (float) s.volume(), (float) s.pitch());
        }
    }

    private static MobHostility.Side side(LivingEntity l) {
        return l instanceof Player ? MobHostility.Side.PLAYER : MobHostility.Side.MOB;
    }

    private static boolean exempt(LivingEntity l) {
        return l instanceof Player pl && (pl.getGameMode() == GameMode.CREATIVE || pl.getGameMode() == GameMode.SPECTATOR);
    }

    private String kind(LivingEntity l) {
        String id = l.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        return l.getType().getKey().getKey() + (id == null ? "" : "/" + id);
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }

    private static String v(Vec3 p) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", p.x(), p.y(), p.z());
    }
}
