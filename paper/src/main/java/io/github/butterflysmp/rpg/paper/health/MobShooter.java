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
import io.github.butterflysmp.rpg.core.mob.MobBlink;
import io.github.butterflysmp.rpg.core.mob.BeamTeleport;
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
import org.bukkit.entity.Frog;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
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
 * <h2>THE CARRIER: A FROG SCALED TO THE HIT BOX, invisible, silent, no AI, no gravity, never saved</h2>
 * Our weapons reach only living bodies ({@code PaperCombatWorld.combatantsNear}, {@code castRay}: {@code LiveTargets}),
 * so the shot is a living entity. It was a bat until the boot of {@code 8ec16e89}; the seat's ruling 2 on #201 asks
 * for a hittable box 0.8 wide AND tall around the shape, and a bat (0.5 x 0.9) cannot be scaled to a cube. A frog is a
 * 0.5 cube (READ, EntityType), a CREATURE, no {@code Enemy}; with NoAI its brain never runs; {@code setSilent} stops its
 * sounds. It takes vanilla damage through the ordinary {@code LivingEntity} path. It moves only by our {@code teleport},
 * which carries its passenger, a glowing block display centred on its box ({@link #calibrate}). It is NOT made
 * non-collidable: that would make it unpickable, and no vanilla arrow could hit it (the seat's D2 on #201).
 *
 * <h2>A COMBATANT ONLY WHERE IT MUST BE (the seat's ruling 2)</h2>
 * <ul>
 *   <li><b>Every attack reaches it</b>: our pipeline's handles ask {@link ShotSink#strike} first
 *       ({@code BukkitCombatant}); vanilla's damage is met at LOWEST by {@code RpgListeners.onShotDamaged}, always
 *       cancelled, and classified by core ({@code ShotDamage}): an ATTACK (a combatant's melee, projectile, explosion
 *       or magic) destroys it, whatever the amount; {@code /kill} ENDS it; anything else is REFUSED and logged once per
 *       cause ({@link #refuse}). A wall never damages it: the swept-box check ends it "blocked" first.</li>
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
        final Frog carrier;
        /** The display's offset from the carrier's feet as last MEASURED on the server (C1), or null before the first. */
        Vector3f offset;
        /** The vanilla causes already refused for this shot: each is logged once. */
        final java.util.Set<String> refused = new java.util.HashSet<>();
        BlockDisplay display;
        MobShot.State state;
        LivingEntity target;
        boolean ended;
        Live(Owner owner, Frog carrier, MobShot.State state, LivingEntity target) {
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
        /** Its blink, or null (step R7b). */
        final MobBlink blink;
        /** The instrument's fixed draw for the NEXT blink, which it forces on the next tick; null otherwise. */
        MobBlink.Draw forced;
        /**
         * Its blink's cooldown (step R8), or null with no blink: held by its loop. F21 (the seat's queue, item 4,
         * 2026-11-19): its END is also saved in the mob's PDC ({@code Keys.blinkReadyAt}) when a blink lands, and a loop
         * that starts again resumes it ({@code resumeCooldown}). The shot clock still starts fresh (X-o).
         */
        final MobBlink.Cooldown cooldown;
        /**
         * A blink has LANDED since its last shot (step R8): until it fires, it chooses whom to fire at out to the
         * re-acquire range ({@code MobShot#chooseRange}), because Ben's 24-block blink can leave its trigger past the
         * acquire range. Live state, as the cooldown is.
         */
        boolean blinked;
        Owner(Mob mob, String mobId, MobShot shot, MobBlink blink) {
            this.mob = mob;
            this.mobId = mobId;
            this.shot = shot;
            this.blink = blink;
            this.clock = new MobShot.Clock(shot.periodTicks());
            this.cooldown = blink == null ? null : blink.cooldown();
        }
    }

    /**
     * F21 (the seat's queue, item 4, 2026-11-19): a Phalanx whose loop starts again (an unload and a load) resumes its
     * blink cooldown from the end saved on it, clamped to one length (a server tick from an earlier run may lie past now).
     * Logged when anything is resumed.
     */
    private void resumeCooldown(Owner o) {
        if (o.cooldown == null) return;
        Long readyAt = o.mob.getPersistentDataContainer().get(adapters.keys().blinkReadyAt,
                org.bukkit.persistence.PersistentDataType.LONG);
        if (readyAt == null) return;
        long left = readyAt - Bukkit.getCurrentTick();
        o.cooldown.resume((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, left)));
        if (o.cooldown.running()) {
            adapters.log().info("BLINK cooldown resumed mob=" + o.mobId + " id=" + o.mob.getUniqueId()
                    + " remaining=" + o.cooldown.remaining());
        }
    }

    /**
     * C4, CLOSED HERE (step R7b): the carrier never pushes and is never pushed, by a scoreboard team whose collision rule
     * is NEVER. READ, {@code EntitySelector.pushableBy} (offsets 57-85) and {@code LivingEntity#pushEntities}: either
     * body's team rule NEVER stops the push both ways. It does NOT touch {@code LivingEntity.collides}, which
     * {@code isPickable} reads, so a vanilla arrow still hits the carrier (D2 on #201). The team is the main scoreboard's;
     * each carrier joins it at its fire and leaves it at its end, and the enable sweep empties it of any entry left by a
     * crash.
     */
    static final String SHOT_TEAM = "rpg_shot";

    private static org.bukkit.scoreboard.Team shotTeam() {
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team t = board.getTeam(SHOT_TEAM);
        if (t == null) t = board.registerNewTeam(SHOT_TEAM);
        if (t.getOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE) != org.bukkit.scoreboard.Team.OptionStatus.NEVER) {
            t.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        }
        return t;
    }

    /** The enable sweep: a team entry no live carrier owns is a crash's leftover, and goes. */
    public void clearShotTeam() {
        org.bukkit.scoreboard.Team t = shotTeam();
        for (String entry : List.copyOf(t.getEntries())) t.removeEntry(entry);
    }

    /** A frog's box edge before any scale, BLOCKS (READ: EntityType frog sized(0.5f, 0.5f)). SCALE = hit box / this. */
    static final double FROG_EDGE = 0.5;

    private final Map<UUID, Owner> owners = new ConcurrentHashMap<>();
    private final Map<UUID, Live> carriers = new ConcurrentHashMap<>();
    private final Set<UUID> displays = ConcurrentHashMap.newKeySet();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Supplier<MobRegistry> mobs;
    private final AdapterContext adapters;

    /** The blink's randomness (step R7b): production draws uniformly; the instrument fixes one draw. */
    private final MobBlink.Source blinkSource;

    public MobShooter(Supplier<MobRegistry> mobs, AdapterContext adapters) {
        this(mobs, adapters, MobBlink.uniform(new java.util.Random()));
    }

    public MobShooter(Supplier<MobRegistry> mobs, AdapterContext adapters, MobBlink.Source blinkSource) {
        this.mobs = mobs;
        this.adapters = adapters;
        this.blinkSource = blinkSource;
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
        Owner o = new Owner(mob, mobId, def.shot(), def.blink());
        resumeCooldown(o);
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
                    s.state.position(), s.state.origin(), s.state.velocity(), box(s.carrier),
                    s.display == null ? box(s.carrier) : shape(s.display)));
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
        // The blink LAST in the tick (step R7b): after this tick's fire and every shot's step, so a blink can neither
        // swallow a fire nor touch a shot. The shots are the Phalanx's own list, and a blink is a same-world teleport,
        // which neither removes the Phalanx nor stops this loop: a blink cannot orphan a shot.
        blinkStep(o);
    }

    private void attempt(Owner o, boolean forced) {
        Mob mob = o.mob;
        Vec3 eye = vec(mob.getEyeLocation());
        LivingEntity target = choose(mob, o.shot, eye, o.blinked);
        if (target == null) {
            if (forced) adapters.log().info(MobShotLines.none(o.mobId, mob.getUniqueId(), Bukkit.getCurrentTick(),
                    o.shot.chooseRange(o.blinked)));
            return;
        }
        fire(o, forced, eye, target);
    }

    /** X8: its vanilla target if the predicate names it and it is in range; else the nearest such in range. The range is
     * the acquire range, or the re-acquire range from a landed blink until the next shot (MobShot#chooseRange, step R8). */
    private LivingEntity choose(Mob mob, MobShot shot, Vec3 eye, boolean blinked) {
        LivingEntity vanilla = mob.getTarget();
        if (vanilla != null && eligible(mob, vanilla) && shot.inChooseRange(eye, vec(vanilla.getEyeLocation()), blinked)) {
            return vanilla;
        }
        double r = shot.chooseRange(blinked);
        List<LivingEntity> found = new ArrayList<>();
        for (Entity e : mob.getWorld().getNearbyEntities(mob.getEyeLocation(), r, r, r)) {
            if (e instanceof LivingEntity l && eligible(mob, l) && shot.inChooseRange(eye, vec(l.getEyeLocation()), blinked)) {
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
        // THE CARRIER IS A FROG (the seat's ruling 2 on #201: a hittable box 0.8 wide AND tall around the shape). READ,
        // EntityType: frog sized(0.5f, 0.5f), a cube, so Attribute.SCALE makes it any cube (LivingEntity#getDimensions
        // scales by getScale(), §7 F-C); a bat (0.5 x 0.9) cannot be one. A frog is a CREATURE (no Peaceful despawn, no
        // Enemy, so no golem targets it); its brain runs only with AI (customServerAiStep); it is silent when setSilent.
        // NOT setCollidable(false): that clears LivingEntity.collides, which isPickable reads, and a vanilla arrow
        // asks isPickable through canBeHitByProjectile (READ, the seat's D2): the carrier would be unhittable by one.
        Frog carrier = world.spawn(feet(world, eye, look.hitBox()), Frog.class, b -> {
            // Registered INSIDE the pre-spawn consumer, before its add event, so onAppear never takes it for an orphan.
            b.getPersistentDataContainer().set(adapters.keys().mobShot, PersistentDataType.BYTE, (byte) 1);
            b.setAI(false);
            b.setSilent(true);
            b.setInvisible(true);
            b.setGravity(false);
            b.setPersistent(false);
            b.setRemoveWhenFarAway(false);
            b.setAdult();
            org.bukkit.attribute.AttributeInstance scale = b.getAttribute(org.bukkit.attribute.Attribute.SCALE);
            if (scale != null) scale.setBaseValue(look.hitBox() / FROG_EDGE);
            made[0] = new Live(o, b, state, target);
            carriers.put(b.getUniqueId(), made[0]);
        });
        Live s = made[0];
        // Its box is now hitBox on every edge: its feet go half of it below the shot's centre. Yaw 0 always, so its
        // passenger offset (below) never turns.
        carrier.teleport(feet(world, state.position(), carrier.getHeight()));
        float size = (float) look.size();
        s.display = world.spawn(carrier.getLocation(), BlockDisplay.class, d -> {
            d.getPersistentDataContainer().set(adapters.keys().mobShot, PersistentDataType.BYTE, (byte) 1);
            displays.add(d.getUniqueId());
            d.setPersistent(false);
            d.setBlock((block == null ? Material.AMETHYST_BLOCK : block).createBlockData());
            // A passenger rides at its vehicle's feet + the vehicle's PASSENGER attachment (READ: Entity#positionRider;
            // a Display's own vehicle attachment is ZERO). A frog's is (0, 0.375, -0.25) (READ, EntityType frog
            // passengerAttachments), rotated by its yaw (0 here) and scaled with it (INFERRED). The first translation
            // is from those numbers; the next tick re-measures the real offset on the server and corrects it
            // (calibrate), so the block's centre is the box's centre whatever the offset is.
            d.setTransformation(shapeAt(new Vector3f(0f, (float) (0.375 * look.hitBox() / FROG_EDGE),
                    (float) (-0.25 * look.hitBox() / FROG_EDGE)), (float) carrier.getHeight(), size));
            d.setBrightness(new Display.Brightness(15, 15));
            d.setGlowing(true);
            d.setGlowColorOverride(Color.fromRGB(look.glowRgb()));
        });
        carrier.addPassenger(s.display);
        shotTeam().addEntry(carrier.getUniqueId().toString());   // C4: it never pushes and is never pushed
        o.shots.add(s);
        o.blinked = false;   // its first shot after a blink puts it back on the acquire range
        // cfde822: "Firing clears invisibility" (READ, section 3.4), so it may blink again.
        if (mob.isInvisible()) {
            mob.setInvisible(false);
            adapters.log().info("BLINK visible mob=" + o.mobId + " id=" + mob.getUniqueId() + " tick="
                    + Bukkit.getCurrentTick() + " reason=fired");
        }

        int gs = gearScore(o);
        double dealt = o.shot.price(MobClassifier.isHostile(mob), gs);
        playSounds(world, mob.getLocation(), o.shot.fireSounds());
        adapters.log().info(MobShotLines.fired(o.mobId, mob.getUniqueId(), gs, forced, Bukkit.getCurrentTick(),
                carrier.getUniqueId(), kind(target), target.getUniqueId(), eye, aim, dealt));
        draw(world, s);
        for (Listener l : listeners) l.onFired(mob, carrier);
    }

    // --- the blink (step R7b) ----------------------------------------------------------------------------------------

    /** The instrument: the next tick forces a blink whose every attempt is this draw, frozen or not. Empty when it took. */
    public Optional<String> forceBlink(LivingEntity entity, MobBlink.Draw draw) {
        Owner o = owners.get(entity.getUniqueId());
        if (o == null || !o.task.isRunning()) return Optional.of("no shot loop");
        if (o.blink == null) return Optional.of("no blink");
        o.forced = draw;
        return Optional.empty();
    }

    /**
     * cfde822: "a non-creative, non-spectator player in the D4 box while it is not already invisible" (READ). Its OWN
     * blink needs AI, as its own fire does (a frozen Phalanx never acts of itself); the instrument's force does not.
     */
    private void blinkStep(Owner o) {
        if (o.blink == null) return;
        Mob mob = o.mob;
        // THE COOLDOWN (step R8), counted down FIRST each tick, so a blink that landed on tick L is followed by
        // cooldown_ticks - 1 ticks that cannot decide, and tick L + cooldown_ticks can.
        o.cooldown.tick();
        if (o.forced != null) {
            MobBlink.Draw d = o.forced;
            o.forced = null;
            List<MobBlink.Draw> draws = new ArrayList<>();
            for (int i = 0; i < o.blink.attempts(); i++) draws.add(d);
            blink(o, draws, true, null);
            return;
        }
        if (!mob.hasAI() || mob.isInvisible()) return;
        // While the cooldown runs, a combatant inside the trigger box causes no blink and no line: the Phalanx fights as
        // it stands (its shot's clock and its vanilla bow goal run on, untouched). Ben, 2026-11-17: "Say 12 seconds".
        if (o.cooldown.running()) return;
        Vec3 feet = vec(mob.getLocation());
        double r = o.blink.triggerRange() + 2.0;
        for (Entity e : mob.getWorld().getNearbyEntities(mob.getLocation(), r, r + 2.0, r)) {
            if (!(e instanceof LivingEntity l) || l.equals(mob) || marked(l) || !LiveTargets.isLive(l)) continue;
            if (o.blink.triggered(feet, box(l), side(l), exempt(l))) {
                blink(o, o.blink.draws(blinkSource), false, l);
                return;
            }
        }
    }

    /** The decision through the Knell's port (MobTeleporter.footing, no sight), one line, and the move if it lands. */
    private void blink(Owner o, List<MobBlink.Draw> draws, boolean forced, LivingEntity trigger) {
        Mob mob = o.mob;
        Location from = mob.getLocation();
        // R3 (step R8): the search is anchored on the whole block its feet are in (MobBlink.anchor's javadoc).
        double anchor = MobBlink.anchor(from.getY());
        MobBlink.Decision d = o.blink.decide(vec(from), anchor, draws, MobTeleporter.footing(mob, null));
        adapters.log().info(MobShotLines.blink(o.mobId, mob.getUniqueId(), Bukkit.getCurrentTick(), forced,
                trigger == null ? null : kind(trigger), trigger == null ? null : trigger.getUniqueId(), vec(from),
                anchor, o.cooldown.remaining(), d));
        if (d.landing().isEmpty()) return;   // a decision that lands nowhere leaves the cooldown as it was
        Vec3 l = d.landing().get();
        puff(o.blink, from);
        Location to = new Location(from.getWorld(), l.x(), l.y(), l.z(), from.getYaw(), from.getPitch());
        if (!mob.teleport(to)) {
            adapters.log().info("BLINK refused mob=" + o.mobId + " id=" + mob.getUniqueId() + " reason=teleport");
            return;
        }
        mob.getPathfinder().stopPathfinding();
        mob.setInvisible(true);
        puff(o.blink, to);
        // THE COOLDOWN STARTS HERE, at a blink that LANDS (forced or not), and nowhere else.
        o.cooldown.landed();
        o.blinked = true;
        // F21: and the cooldown's end is saved on the mob, so an unload and a load do not reset it.
        mob.getPersistentDataContainer().set(adapters.keys().blinkReadyAt,
                org.bukkit.persistence.PersistentDataType.LONG, (long) Bukkit.getCurrentTick() + o.cooldown.length());
        // READ BACK, after the act: where it stands and whether it is invisible, never the decision restated.
        Location now = mob.getLocation();
        adapters.log().info(String.format(java.util.Locale.ROOT,
                "BLINK moved mob=%s id=%s tick=%d at=%.3f %.3f %.3f invisible=%s cooldown=%d",
                o.mobId, mob.getUniqueId(), Bukkit.getCurrentTick(), now.getX(), now.getY(), now.getZ(), mob.isInvisible(),
                o.cooldown.remaining()));
    }

    private static void puff(MobBlink b, Location at) {
        Location mid = at.clone().add(0, 1.0, 0);
        at.getWorld().spawnParticle(Particle.valueOf(b.particle().toUpperCase(java.util.Locale.ROOT)), mid,
                b.particleCount(), 0.4, 0.8, 0.4, 0.02);
        playSounds(at.getWorld(), at, b.sounds());
    }

    /** One tick of one shot: turn, test the block, the hit, the life and the range; then move it, or end it. */
    private void advance(Live s) {
        if (s.ended) return;
        Owner o = s.owner;
        Frog carrier = s.carrier;
        if (!carrier.isValid()) {
            end(s, "removed");
            return;
        }
        // C1 (step R7b): measure the rider's REAL offset NOW, before this tick's teleport, while it stands where its last
        // rideTick put it, and correct the translation whenever it differs (calibrate's javadoc has the evidence).
        calibrate(s);
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
        // THE BLOCK RULE IS THE CARRIER'S BOX SWEPT ALONG THIS TICK'S PATH (the seat's ruling 1 on #201): the box at the
        // old centre and at the new, and all between (their union; at 0.2 a tick against a 0.8 box the union is the
        // sweep, a little more on a diagonal). Any block whose COLLISION shape overlaps it ends the shot "blocked", before
        // the move, so the body never enters a block. Fluids and passable blocks have no collision shape and do not stop it.
        Optional<org.bukkit.block.Block> wall = firstColliding(world, swept(carrier, from, to));
        boolean blocked = wall.isPresent();
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
                    next.origin(), wall.get().getType().getKey().getKey()));
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
        shotTeam().removeEntry(s.carrier.getUniqueId().toString());
        if (s.carrier.isValid()) s.carrier.remove();
    }

    /**
     * The display's transformation for a passenger whose origin sits {@code offset} from the carrier's feet: the block
     * of edge {@code size} centred on the carrier box's centre, {@code (0, height / 2, 0)} above its feet.
     */
    private static Transformation shapeAt(Vector3f offset, float height, float size) {
        return new Transformation(new Vector3f(-offset.x - size / 2, height / 2 - offset.y - size / 2, -offset.z - size / 2),
                new AxisAngle4f(), new Vector3f(size, size, size), new AxisAngle4f());
    }

    /**
     * C1, CLOSED HERE (step R7b). The passenger's REAL offset from the carrier's feet, measured EVERY tick before our
     * teleport, and the translation corrected whenever it moves by more than 1e-4 blocks on any axis.
     *
     * <p><b>Why the first build's one-time calibration did not take (the boot of 3dc0e184, SH1R2: the shape 0.45 above
     * and 0.40 toward -z of the box's centre):</b>
     * <ul>
     *   <li>It ran ONCE, right after our first teleport. READ, {@code Entity#calculatePassengerTransition}: a vehicle's
     *       teleport moves each passenger to the new position plus its CURRENT offset (position minus the vehicle's),
     *       {@code transitionAsPassenger}, so the passenger keeps whatever offset it had.</li>
     *   <li>The display was spawned at the carrier's own feet (offset 0), and the true offset is applied only in its
     *       rideTick ({@code Entity#positionRider}). That had not run yet (INFERRED from the reading), so the one
     *       measurement read 0 and centred the block for an offset of 0.</li>
     *   <li>The rideTick then placed it at the frog's real passenger point, (0, 0.45, -0.40) by the reading. The frog's
     *       READ attachment is (0, 0.375, -0.25), and the -0.40 is that z scaled by 1.6 exactly.</li>
     * </ul>
     *
     * <p><b>Measured now at the start of every step, the block is centred from the tick after its first rideTick,
     * and on any facing.</b> The carrier's yaw is always 0 (every teleport's Location has yaw 0), so the attachment
     * never turns, whatever direction the shot flies. The client places a passenger by the same rule (INFERRED:
     * {@code Entity#positionRider} is shared code), so the CLIENT draws the block where it draws its box. PROBE_SHOT
     * reads the shape from the display's own server position plus its translation, a measurement, not this
     * arithmetic.
     */
    private void calibrate(Live s) {
        if (s.display == null || !s.display.isValid() || !s.carrier.equals(s.display.getVehicle())) return;
        Location c = s.carrier.getLocation();
        Location d = s.display.getLocation();
        Vector3f offset = new Vector3f((float) (d.getX() - c.getX()), (float) (d.getY() - c.getY()),
                (float) (d.getZ() - c.getZ()));
        if (s.offset != null && Math.abs(s.offset.x - offset.x) <= 1e-4 && Math.abs(s.offset.y - offset.y) <= 1e-4
                && Math.abs(s.offset.z - offset.z) <= 1e-4) {
            return;
        }
        s.offset = offset;
        s.display.setTransformation(shapeAt(offset, (float) s.carrier.getHeight(), (float) s.owner.shot.look().size()));
        Owner o = s.owner;
        adapters.log().info(String.format(java.util.Locale.ROOT,
                "SHOT calibrated mob=%s id=%s shot=%s age=%d offset=%.3f %.3f %.3f", o.mobId, o.mob.getUniqueId(),
                s.carrier.getUniqueId(), s.state.age(), offset.x, offset.y, offset.z));
    }

    /** The carrier's box at the old centre and at the new, joined: what it sweeps through this tick. */
    private static org.bukkit.util.BoundingBox swept(Frog carrier, Vec3 from, Vec3 to) {
        double half = carrier.getWidth() / 2.0;
        double hh = carrier.getHeight() / 2.0;
        return new org.bukkit.util.BoundingBox(from.x() - half, from.y() - hh, from.z() - half,
                from.x() + half, from.y() + hh, from.z() + half)
                .union(new org.bukkit.util.BoundingBox(to.x() - half, to.y() - hh, to.z() - half,
                        to.x() + half, to.y() + hh, to.z() + half));
    }

    /**
     * The first block (lowest x, then y, then z) whose COLLISION shape overlaps {@code box}, or empty. A block's shape
     * is relative to the block, so each of its boxes is moved to the block's corner before the test; {@code overlaps} is
     * strict, so a box that only touches a face does not overlap it.
     */
    private static Optional<org.bukkit.block.Block> firstColliding(World world, org.bukkit.util.BoundingBox box) {
        for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.floor(box.getMaxX()); x++) {
            for (int y = (int) Math.floor(box.getMinY()); y <= (int) Math.floor(box.getMaxY()); y++) {
                for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.floor(box.getMaxZ()); z++) {
                    org.bukkit.block.Block b = world.getBlockAt(x, y, z);
                    for (org.bukkit.util.BoundingBox part : b.getCollisionShape().getBoundingBoxes()) {
                        if (part.clone().shift(x, y, z).overlaps(box)) return Optional.of(b);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** {@code /kill} (and anything else ruled to END it): the shot ends, named, without the destroyed sound. */
    public void end(Entity entity, String reason) {
        Live s = carriers.get(entity.getUniqueId());
        if (s != null) end(s, reason);
    }

    /** A vanilla cause that is not an attack: nothing happens to the shot; the first of each cause is logged. */
    public void refuse(Entity entity, String cause) {
        Live s = carriers.get(entity.getUniqueId());
        if (s == null || s.ended || !s.refused.add(cause)) return;
        Owner o = s.owner;
        adapters.log().info(MobShotLines.refused(o.mobId, o.mob.getUniqueId(), entity.getUniqueId(), s.state.age(),
                s.state.position(), s.state.origin(), cause));
    }

    /** A live shot as the instrument sees it: its carrier, centre, age and server box. */
    public record LiveShot(UUID id, Frog carrier, Vec3 centre, int age, BeamBox box) {}

    /** Every live shot, nearest {@code to} first. On the main thread (Paper). */
    public List<LiveShot> liveShots(Location to) {
        List<LiveShot> out = new ArrayList<>();
        for (Live s : carriers.values()) {
            if (s.ended || !s.carrier.isValid() || !s.carrier.getWorld().equals(to.getWorld())) continue;
            out.add(new LiveShot(s.carrier.getUniqueId(), s.carrier, s.state.position(), s.state.age(), box(s.carrier)));
        }
        out.sort(Comparator.comparingDouble(l -> l.centre().distanceSquared(vec(to))));
        return out;
    }

    /** The centre of the VISIBLE shape riding this carrier, on the server; empty when it carries none. */
    public static Optional<Vec3> shapeCentre(Entity carrier) {
        for (Entity p : carrier.getPassengers()) {
            if (p instanceof BlockDisplay d) {
                BeamBox s = shape(d);
                return Optional.of(s.center());
            }
        }
        return Optional.empty();
    }

    /** Is this carrier's shot still live? */
    public boolean isLive(UUID carrier) {
        Live s = carriers.get(carrier);
        return s != null && !s.ended;
    }

    private static BeamBox box(Entity e) {
        var b = e.getBoundingBox();
        return new BeamBox(new Vec3(b.getMinX(), b.getMinY(), b.getMinZ()), new Vec3(b.getMaxX(), b.getMaxY(), b.getMaxZ()));
    }

    /** The visible shape's box on the server: the display's position plus its translation, its scale on every edge. */
    private static BeamBox shape(BlockDisplay d) {
        Location l = d.getLocation();
        Transformation t = d.getTransformation();
        Vec3 min = new Vec3(l.getX() + t.getTranslation().x, l.getY() + t.getTranslation().y, l.getZ() + t.getTranslation().z);
        return new BeamBox(min, min.add(new Vec3(t.getScale().x, t.getScale().y, t.getScale().z)));
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
