package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamBox;
import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobGearScore;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.mob.MobShot;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import io.github.butterflysmp.rpg.paper.adapter.ShotSink;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * THE PHALANX'S SHOT, the Bukkit half (PLAN-dragon-mobs.md step R7a; the plan's N7). The rules are core's
 * ({@link MobShot}: the flight, the endings, the victim, the price, the clock); this class finds targets, carries the
 * shot as an entity, applies the result and draws it.
 *
 * <h2>THE CARRIER (the seat's ruling 1): a BAT, invisible, silent, no AI, no gravity, never saved</h2>
 * Our weapons reach only living bodies ({@code PaperCombatWorld.combatantsNear}, {@code castRay}: {@code LiveTargets}),
 * so the shot is a living entity. A bat, read in the pinned jar (the gate's S1): it is no {@code Enemy}, so no golem
 * targets it, and no vanilla goal names it but the Warden's (refused below); with NoAI it never moves, wakes or rests
 * of itself ({@code LivingEntity#aiStep} calls {@code travel} only for effective AI); {@code setSilent} stops its
 * ambient sound; it takes vanilla damage through the ordinary {@code LivingEntity} path, so a swing and an arrow raise
 * the event we meet (an invisible ARMOUR STAND's is raised already cancelled, which is why it is not one). It moves
 * only by our {@code teleport}, which carries its passenger. A small glowing block display rides it.
 *
 * <h2>A COMBATANT ONLY WHERE IT MUST BE (the seat's ruling 2)</h2>
 * <ul>
 *   <li><b>Every attack reaches it</b>: our pipeline's handles ask {@link ShotSink#strike} first
 *       ({@code BukkitCombatant}); vanilla's damage is met at LOWEST by {@code RpgListeners.onShotDamaged}. Either
 *       way it is destroyed, whatever the amount.</li>
 *   <li><b>It is never seeded</b> ({@code MobNameplateManager.seed}): no custom health, gear score or nameplate, so
 *       {@code MobDeathSystem} never sees it, and it never dies: it is {@code remove()}d, which raises no death event,
 *       so nothing a death does (XP, drops, the Phage's growth, the split) can happen for it. A vanilla death that
 *       somehow reached it pays nothing ({@code RpgListeners.onShotDeath}).</li>
 *   <li><b>No mob targets it</b> ({@code RpgListeners.onShotTargeted} cancels any target event naming one).</li>
 * </ul>
 *
 * <h2>NO ORPHAN (the seat's ruling 6)</h2>
 * <ul>
 *   <li><b>Owned by its Phalanx:</b> every shot is stepped inside its Phalanx's own loop, and that loop's
 *       {@code onStop} (the Phalanx dead, removed, unloaded, or the loop cancelled) ends every shot it owns:
 *       cfde822's "ends when the Phalanx dies", and an unload chosen to do the same.</li>
 *   <li><b>Never saved:</b> both entities are {@code setPersistent(false)}: READ, {@code Entity#saveAsPassenger}
 *       writes nothing for one, and a chunk's unload discards it; a passenger of an unsaved vehicle is lost with
 *       it.</li>
 *   <li><b>Never into an unloaded chunk, another world or a portal:</b> a move whose column is unloaded, or a carrier
 *       found in another world, ends it; {@code RpgListeners.onShotPortal} refuses a portal.</li>
 *   <li><b>And if one were ever written anyway</b> (a JVM killed mid-save): an entity carrying {@code mob_shot} that no
 *       live shot owns is removed where it appears ({@link #onAppear}), at a chunk load and in the enable sweep.</li>
 *   <li><b>A stop</b> ends every live shot ({@link #shutdown}, from {@code onDisable}).</li>
 * </ul>
 *
 * <p><b>Threading.</b> On Paper every entity is on the main thread. The shots are moved from their Phalanx's thread,
 * which on Folia need not own the shot's region; recorded in the plan's findings, not fixed (the project runs on
 * Paper), like F3's.
 */
public final class MobShooter implements ShotSink {

    /** Told of each shot fired, on the Phalanx's thread, with its carrier already in the world. */
    public interface Listener {
        void onFired(Mob owner, LivingEntity carrier);
    }

    private static final class Live {
        final Owner owner;
        final Bat carrier;
        BlockDisplay display;
        MobShot.State state;
        LivingEntity target;
        boolean ended;
        Live(Owner owner, Bat carrier, MobShot.State state, LivingEntity target) {
            this.owner = owner;
            this.carrier = carrier;
            this.state = state;
            this.target = target;
        }
    }

    private static final class Owner {
        final Mob mob;
        final String mobId;
        final MobShot shot;
        final MobShot.Clock clock;
        final List<Live> shots = new ArrayList<>();
        RepeatingTask task;
        Owner(Mob mob, String mobId, MobShot shot) {
            this.mob = mob;
            this.mobId = mobId;
            this.shot = shot;
            this.clock = new MobShot.Clock(shot.periodTicks());
        }
    }

    private final Map<UUID, Owner> owners = new ConcurrentHashMap<>();
    private final Map<UUID, Live> carriers = new ConcurrentHashMap<>();
    private final Set<UUID> displays = ConcurrentHashMap.newKeySet();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Supplier<MobRegistry> mobs;
    private final AdapterContext adapters;

    public MobShooter(Supplier<MobRegistry> mobs, AdapterContext adapters) {
        this.mobs = mobs;
        this.adapters = adapters;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    private boolean marked(Entity e) {
        return e.getPersistentDataContainer().has(adapters.keys().mobShot);
    }

    /** Is this entity a shot's carrier or its display (live or not)? The one test the listeners use. */
    public boolean isShot(Entity e) {
        return marked(e);
    }

    // --- appearance and removal ------------------------------------------------------------------------------------

    /**
     * Any entity entered the world (a spawn or a chunk load), on its own thread. A shot entity no live shot owns is an
     * ORPHAN and is removed at once; a mob whose file authors a {@code shot:} starts its loop (once).
     */
    public void onAppear(Entity entity) {
        if (marked(entity)) {
            if (!carriers.containsKey(entity.getUniqueId()) && !displays.contains(entity.getUniqueId())) {
                adapters.log().info(MobShotLines.orphan(entity.getType().getKey().getKey(), entity.getUniqueId()));
                for (Entity p : entity.getPassengers()) p.remove();
                entity.remove();
            }
            return;
        }
        if (!(entity instanceof Mob mob)) return;
        String mobId = mob.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        if (mobId == null) return;
        MobDefinition def = mobs.get().find(mobId).orElse(null);
        if (def == null || def.shot() == null) return;
        Owner running = owners.get(mob.getUniqueId());
        if (running != null && running.task.isRunning()) return;
        Owner o = new Owner(mob, mobId, def.shot());
        o.task = RepeatingTask.start(new EntityTaskTarget(mob, adapters.scheduler(), adapters.log()), 1, "phalanx-shot",
                () -> {
                    step(o);
                    return true;
                },
                () -> {
                    for (Live s : List.copyOf(o.shots)) end(s, "owner-gone");
                    owners.remove(o.mob.getUniqueId(), o);
                });
        owners.put(mob.getUniqueId(), o);
    }

    /** Any entity left the world. A Phalanx's loop stops (ending its shots); a carrier's shot ends as removed. */
    public void onRemove(UUID id) {
        Owner o = owners.get(id);
        if (o != null) o.task.cancel();
        Live s = carriers.get(id);
        if (s != null && !s.ended) end(s, "removed");
        displays.remove(id);
    }

    /** The server is stopping: every live shot ends now, on this (the main) thread. */
    public void shutdown() {
        for (Live s : List.copyOf(carriers.values())) end(s, "server-stop");
    }

    /** Does this mob author a shot? Then it never looses a vanilla arrow (the seat's ruling 7). */
    public boolean firesShots(LivingEntity e) {
        String mobId = e.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        if (mobId == null) return false;
        return mobs.get().find(mobId).map(d -> d.shot() != null).orElse(false);
    }

    /** The mob's id, for the bow line. */
    public String mobIdOf(LivingEntity e) {
        return e.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
    }

    // --- the instrument (the seat's ruling 10) -------------------------------------------------------------------

    /** Make this Phalanx fire on its next tick, frozen or not. On its own thread. Empty when it took. */
    public Optional<String> force(LivingEntity entity) {
        Owner o = owners.get(entity.getUniqueId());
        if (o == null || !o.task.isRunning()) return Optional.of("no shot loop");
        o.clock.force();
        return Optional.empty();
    }

    /** One PROBE line per live shot this Phalanx owns, oldest first; empty when it owns none. */
    public List<String> probe(LivingEntity entity) {
        Owner o = owners.get(entity.getUniqueId());
        if (o == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Live s : o.shots) {
            out.add(MobShotLines.probe(o.mobId, o.mob.getUniqueId(), s.carrier.getUniqueId(), s.state.age(),
                    s.state.position(), s.state.origin(), s.state.velocity()));
        }
        return out;
    }

    // --- the loop --------------------------------------------------------------------------------------------------

    private void step(Owner o) {
        switch (o.clock.tick()) {
            case WAIT -> { }
            // As the Knell's I23: a mob with no AI never fires of its own; the instrument's force still works.
            case ATTEMPT -> { if (o.mob.hasAI()) attempt(o, false); }
            case FORCED -> attempt(o, true);
        }
        for (Live s : List.copyOf(o.shots)) advance(s);
    }

    private void attempt(Owner o, boolean forced) {
        Mob mob = o.mob;
        Vec3 eye = vec(mob.getEyeLocation());
        LivingEntity target = choose(mob, o.shot, eye);
        if (target == null) {
            if (forced) adapters.log().info(MobShotLines.none(o.mobId, mob.getUniqueId(), Bukkit.getCurrentTick(),
                    o.shot.acquireRange()));
            return;
        }
        fire(o, forced, eye, target);
    }

    /** X8: its vanilla target if the predicate names it and it is in range; else the nearest such in range. */
    private LivingEntity choose(Mob mob, MobShot shot, Vec3 eye) {
        LivingEntity vanilla = mob.getTarget();
        if (vanilla != null && eligible(mob, vanilla) && shot.inAcquireRange(eye, vec(vanilla.getEyeLocation()))) {
            return vanilla;
        }
        double r = shot.acquireRange();
        List<LivingEntity> found = new ArrayList<>();
        for (Entity e : mob.getWorld().getNearbyEntities(mob.getEyeLocation(), r, r, r)) {
            if (e instanceof LivingEntity l && eligible(mob, l) && shot.inAcquireRange(eye, vec(l.getEyeLocation()))) {
                found.add(l);
            }
        }
        return found.stream().min(Comparator.comparingDouble(l -> eye.distanceSquared(vec(l.getEyeLocation()))))
                .orElse(null);
    }

    private boolean eligible(Entity from, LivingEntity l) {
        return !l.equals(from) && !marked(l) && LiveTargets.isLive(l) && l.getWorld().equals(from.getWorld())
                && MobHostility.hostileTo(side(l), exempt(l));
    }

    private void fire(Owner o, boolean forced, Vec3 eye, LivingEntity target) {
        Mob mob = o.mob;
        World world = mob.getWorld();
        Vec3 aim = vec(target.getEyeLocation());
        MobShot.State state = o.shot.launch(eye, aim);
        MobShot.ShotLook look = o.shot.look();
        Material block = Material.matchMaterial(look.block());
        Live[] made = new Live[1];
        Bat carrier = world.spawn(feet(world, eye, 0.9), Bat.class, b -> {
            // Registered INSIDE the pre-spawn consumer, before its add event, so onAppear never takes it for an orphan.
            b.getPersistentDataContainer().set(adapters.keys().mobShot, PersistentDataType.BYTE, (byte) 1);
            b.setAI(false);
            b.setSilent(true);
            b.setInvisible(true);
            b.setGravity(false);
            b.setPersistent(false);
            b.setRemoveWhenFarAway(false);
            b.setCollidable(false);
            b.setAwake(true);
            made[0] = new Live(o, b, state, target);
            carriers.put(b.getUniqueId(), made[0]);
        });
        Live s = made[0];
        // The carrier's real height (a bat's 0.9, READ): its feet go half of it below the shot's centre.
        carrier.teleport(feet(world, state.position(), carrier.getHeight()));
        float size = (float) look.size();
        float h = (float) carrier.getHeight();
        s.display = world.spawn(carrier.getLocation(), BlockDisplay.class, d -> {
            d.getPersistentDataContainer().set(adapters.keys().mobShot, PersistentDataType.BYTE, (byte) 1);
            displays.add(d.getUniqueId());
            d.setPersistent(false);
            d.setBlock((block == null ? Material.AMETHYST_BLOCK : block).createBlockData());
            // A passenger's origin sits at its vehicle's feet + (0, vehicle height, 0) (READ: Entity#positionRider,
            // EntityAttachment.PASSENGER's AT_HEIGHT; a Display's vehicle attachment is ZERO). So the block is moved down
            // by half the carrier's height and half its own edge: its centre is the carrier box's centre.
            d.setTransformation(new Transformation(new Vector3f(-size / 2, -h / 2 - size / 2, -size / 2),
                    new AxisAngle4f(), new Vector3f(size, size, size), new AxisAngle4f()));
            d.setBrightness(new Display.Brightness(15, 15));
            d.setGlowing(true);
            d.setGlowColorOverride(Color.fromRGB(look.glowRgb()));
        });
        carrier.addPassenger(s.display);
        o.shots.add(s);

        int gs = gearScore(o);
        double dealt = o.shot.price(MobClassifier.isHostile(mob), gs);
        playSounds(world, mob.getLocation(), o.shot.fireSounds());
        adapters.log().info(MobShotLines.fired(o.mobId, mob.getUniqueId(), gs, forced, Bukkit.getCurrentTick(),
                carrier.getUniqueId(), kind(target), target.getUniqueId(), eye, aim, dealt));
        draw(world, s);
        for (Listener l : listeners) l.onFired(mob, carrier);
    }

    /** One tick of one shot: turn, test the block, the hit, the life and the range; then move it, or end it. */
    private void advance(Live s) {
        if (s.ended) return;
        Owner o = s.owner;
        Bat carrier = s.carrier;
        if (!carrier.isValid()) {
            end(s, "removed");
            return;
        }
        World world = o.mob.getWorld();
        if (!carrier.getWorld().equals(world)) {
            end(s, "world");
            return;
        }
        // ruling 4: a lost target (dead, gone, another world, no longer one mobs are hostile to) gives way to the
        // nearest within the re-acquire range of the SHOT (cfde822); none, and it flies on straight.
        if (s.target != null && !eligible(o.mob, s.target)) {
            s.target = reacquire(o, s);
            adapters.log().info(MobShotLines.retarget(o.mobId, o.mob.getUniqueId(), carrier.getUniqueId(),
                    s.state.age(), s.target == null ? null : kind(s.target),
                    s.target == null ? null : s.target.getUniqueId()));
        }
        Optional<Vec3> aim = s.target == null ? Optional.empty() : Optional.of(vec(s.target.getEyeLocation()));
        MobShot.State next = o.shot.step(s.state, aim);
        Vec3 from = s.state.position();
        Vec3 to = next.position();
        if (!world.isChunkLoaded((int) Math.floor(to.x()) >> 4, (int) Math.floor(to.z()) >> 4)) {
            s.state = next;
            end(s, "unloaded");
            return;
        }
        // The block rule is the beam's (I13): the first block with a collision box; fluids and passable blocks do not.
        Vec3 d = to.subtract(from);
        RayTraceResult rt = world.rayTraceBlocks(loc(world, from), new Vector(d.x(), d.y(), d.z()), d.length(),
                FluidCollisionMode.NEVER, true);
        boolean blocked = rt != null && rt.getHitBlock() != null;
        Optional<MobShot.Candidate> victim = blocked ? Optional.empty() : o.shot.victim(to, world.getName(), near(world, to, o.shot));
        Optional<MobShot.Ending> ending = o.shot.ending(next, blocked, victim.isPresent());
        s.state = next;
        if (ending.isEmpty()) {
            if (!carrier.teleport(feet(world, to, carrier.getHeight()))) {
                end(s, "teleport-refused");
                return;
            }
            draw(world, s);
            return;
        }
        UUID shotId = carrier.getUniqueId();
        switch (ending.get()) {
            case BLOCKED -> finish(s, MobShotLines.blocked(o.mobId, o.mob.getUniqueId(), shotId, next.age(), to,
                    next.origin(), rt.getHitBlock().getType().getKey().getKey()));
            case EXPIRED_LIFE -> finish(s, MobShotLines.expired(o.mobId, o.mob.getUniqueId(), shotId, next.age(), to,
                    next.origin(), "life"));
            case EXPIRED_RANGE -> finish(s, MobShotLines.expired(o.mobId, o.mob.getUniqueId(), shotId, next.age(), to,
                    next.origin(), "range"));
            case HIT -> {
                LivingEntity body = (LivingEntity) world.getEntity(victim.get().id());
                int gs = gearScore(o);
                double dealt = o.shot.price(MobClassifier.isHostile(o.mob), gs);
                // ruling 5: the mob-hit path's final call, as the beam and the blast take it: defence applies; no
                // element, no crit, no knockback, no shield block (no vanilla event is raised).
                UUID dealer = o.mob.getUniqueId();
                if (Bukkit.isOwnedByCurrentRegion(body)) {
                    BukkitCombatant.of(body, adapters).handle().applyDamage(dealt, dealer);
                } else {
                    adapters.scheduler().onEntity(body, () -> BukkitCombatant.of(body, adapters).handle()
                            .applyDamage(dealt, dealer));
                }
                finish(s, MobShotLines.hit(o.mobId, o.mob.getUniqueId(), shotId, next.age(), to, next.origin(),
                        kind(body), body.getUniqueId(), dealt));
            }
        }
    }

    private LivingEntity reacquire(Owner o, Live s) {
        Vec3 at = s.state.position();
        double r = o.shot.reacquireRange();
        World world = o.mob.getWorld();
        LivingEntity best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (Entity e : world.getNearbyEntities(loc(world, at), r, r, r)) {
            if (!(e instanceof LivingEntity l) || !eligible(o.mob, l)) continue;
            Vec3 eye = vec(l.getEyeLocation());
            if (!o.shot.inReacquireRange(at, eye)) continue;
            double d = at.distanceSquared(eye);
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    /** The live bodies near a shot, its own carrier and every other shot excluded. */
    private List<MobShot.Candidate> near(World world, Vec3 at, MobShot shot) {
        double r = shot.tolerance() + 2.0;
        List<MobShot.Candidate> out = new ArrayList<>();
        for (Entity e : world.getNearbyEntities(loc(world, at), r, r + 1.0, r)) {
            if (!(e instanceof LivingEntity l) || marked(l) || !LiveTargets.isLive(l)) continue;
            var b = l.getBoundingBox();
            out.add(new MobShot.Candidate(l.getUniqueId(), side(l), exempt(l), l.getWorld().getName(),
                    new BeamBox(new Vec3(b.getMinX(), b.getMinY(), b.getMinZ()),
                            new Vec3(b.getMaxX(), b.getMaxY(), b.getMaxZ()))));
        }
        return out;
    }

    // --- endings ---------------------------------------------------------------------------------------------------

    /** THE SINK: any attack on a live shot's carrier destroys it. False for anything else. */
    @Override
    public boolean strike(Entity entity, String by) {
        if (!marked(entity)) return false;
        Live s = carriers.get(entity.getUniqueId());
        if (s == null) {
            // A display, or an orphan the sweep has not met yet: nothing to log, but it is still a shot.
            return true;
        }
        if (s.ended) return true;
        Owner o = s.owner;
        playSounds(entity.getWorld(), entity.getLocation(), o.shot.destroySounds());
        finish(s, MobShotLines.destroyed(o.mobId, o.mob.getUniqueId(), entity.getUniqueId(), s.state.age(),
                s.state.position(), s.state.origin(), by));
        return true;
    }

    private void end(Live s, String reason) {
        if (s.ended) return;
        Owner o = s.owner;
        finish(s, MobShotLines.ended(o.mobId, o.mob.getUniqueId(), s.carrier.getUniqueId(), s.state.age(),
                s.state.position(), s.state.origin(), reason));
    }

    /** Log the one ending line, forget the shot, and remove both its entities. Once. */
    private void finish(Live s, String line) {
        if (s.ended) return;
        s.ended = true;
        adapters.log().info(line);
        s.owner.shots.remove(s);
        carriers.remove(s.carrier.getUniqueId());
        if (s.display != null) {
            displays.remove(s.display.getUniqueId());
            if (s.display.isValid()) s.display.remove();
        }
        if (s.carrier.isValid()) s.carrier.remove();
    }

    // --- the look (the seat's ruling 8) ----------------------------------------------------------------------------

    private static void draw(World world, Live s) {
        MobShot.ShotLook look = s.owner.shot.look();
        Vec3 p = s.state.position();
        world.spawnParticle(Particle.DUST, p.x(), p.y(), p.z(), 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(look.trailRgb()), (float) look.trailSize()));
        if (look.flashes(s.state.age())) {
            world.spawnParticle(Particle.DUST, p.x(), p.y(), p.z(), 3, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.fromRGB(look.flashRgb()), (float) look.flashSize()));
        }
    }

    private static void playSounds(World world, Location at, List<BeamSound> sounds) {
        for (BeamSound s : sounds) {
            world.playSound(at, s.key(), SoundCategory.HOSTILE, (float) s.volume(), (float) s.pitch());
        }
    }

    // --- helpers ---------------------------------------------------------------------------------------------------

    private int gearScore(Owner o) {
        Integer stored = o.mob.getPersistentDataContainer().get(adapters.keys().mobGearScore, PersistentDataType.INTEGER);
        boolean valid = stored != null && MobGearScore.isValidStored(stored);
        if (!valid && MobClassifier.isHostile(o.mob)) {
            adapters.warnOnce("SHOT " + o.mobId + " " + o.mob.getUniqueId() + " has no valid gear score (stored "
                    + stored + "), so it is priced at GS " + MobScaling.GS_BASELINE);
        }
        return valid ? stored : MobScaling.GS_BASELINE;
    }

    private static MobHostility.Side side(LivingEntity l) {
        return l instanceof Player ? MobHostility.Side.PLAYER : MobHostility.Side.MOB;
    }

    private static boolean exempt(LivingEntity l) {
        return l instanceof Player p && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
    }

    private String kind(LivingEntity l) {
        String id = l.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        return l.getType().getKey().getKey() + (id == null ? "" : "/" + id);
    }

    /** The carrier's feet for a shot centred at {@code centre}: half its height below. */
    private static Location feet(World w, Vec3 centre, double height) {
        return new Location(w, centre.x(), centre.y() - height / 2.0, centre.z());
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }

    private static Location loc(World w, Vec3 v) {
        return new Location(w, v.x(), v.y(), v.z());
    }
}
