package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.AccessoryBehaviour;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.accessory.BlinkedPlayers;
import io.github.butterflysmp.rpg.core.accessory.Blinks;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.paper.adapter.CombatSides;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.adapter.TargetGuard;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

/**
 * THE BLINK'S CLOCK AND ITS WIRING (the seat's K1 to K12, 2026-11-26). The decisions are core's ({@link Blinks}); this
 * class decides WHEN, reads the world on the wearer's own thread, teleports him, hides him and ends the state.
 *
 * <h2>When it runs (K1): exactly while the accessory CONTRIBUTES, the pulse's shape</h2>
 *
 * {@code PlayerHealthSystem}'s reconcile pass hands its ONE reading's active behaviours to {@link #reconcile}; at each
 * check the clock asks the SAME function again ({@code contributions}). One {@link RepeatingTask} per wearer on his
 * {@link EntityTaskTarget} (his own thread), every {@link Blinks#CHECK_PERIOD_TICKS} ticks (CC'S CALL: 5, so the check
 * runs within 5 ticks of the condition becoming true; the brief's bound is 10). Stopped when inactive, on quit and on
 * death (an entity task is inactive while dead).
 *
 * <h2>The blink</h2>
 *
 * <ol>
 *   <li><b>The trigger</b> ({@link Blinks#trigger}): the living entities within {@code trigger_radius} of his feet, each
 *       with its side ({@link CombatSides#of}) and whether {@code Mob#getTarget} is HIM. Held in creative and spectator,
 *       while blinked, on cooldown, and for 20 ticks after a search found nowhere safe.</li>
 *   <li><b>The destination</b> ({@link Blinks#search}): the world read through {@link #ground}; the hostiles are every
 *       {@link Mob} of the MOB side within {@code range + clear_radius} of him that is hostile ({@link Enemy}), one of
 *       our own mobs ({@code mob_id}), or targeting anyone now. A passive animal minding its own business does not block
 *       a spot (SEAT PLACEHOLDER, OPEN FOR BEN). The choice is uniform through {@code random}.</li>
 *   <li><b>The teleport</b>: {@code Player#teleportAsync}, the engine's existing way for a player (the selftest's RETURN,
 *       {@code FightFixtures}). READ: it calls {@code ServerLevel.loadChunksForMoveAsync} and its callback runs
 *       {@code teleport0} through {@code MinecraftServer.scheduleOnMain}, so the move is made by the server on its own
 *       thread, never inside this call; on Folia it is the cross-region teleport the API exists for (INFERRED: Folia's
 *       contract, not read here). The spot's chunk is already loaded (the search never reads an unloaded one). Facing as
 *       he was; fall distance cleared before the call and again when it lands.</li>
 *   <li><b>The state</b> ({@link BlinkedPlayers}), the invisibility ({@link Blinks#invisibilityAtBlink}), the cooldown
 *       ({@link CooldownTracker}, the engine's one cooldown store, key {@link #COOLDOWN_KEY}), the forget pass, the
 *       feedback at both ends, and the expiry scheduled on his own thread {@code invisibility_ticks} later.</li>
 * </ol>
 *
 * <h2>Losing aggression (K7)</h2>
 *
 * At the blink every {@link Mob} within {@link #FORGET_RADIUS} blocks of where he stood whose target is him forgets him,
 * on its own thread ({@link #forget}): {@code setTarget(null)}, and READ, the anger the target reading does not show: a
 * {@link Wolf}'s persistent anger ({@code CraftWolf.setAngry(false)} calls {@code Wolf.stopBeingAngry}), a zombified
 * piglin's and a bee's anger timer ({@code setAnger(0)} sets {@code setTimeToRemainAngry(0)}), a {@link Warden}'s anger
 * at him ({@code clearAnger}), and a brain's {@code ANGRY_AT} memory when it names him. A mob whose target is still him
 * after that (a BRAIN-DRIVEN mob: READ, {@code Mob.getTarget} returns the brain's {@code ATTACK_TARGET} for the axolotl,
 * frog, zoglin, breeze, creaking, hoglin, piglin and piglin brute and the warden, and the API exposes no key for that
 * memory) is logged {@code BLINK KEPT}. While blinked, no mob may take him ({@link #refuses}, {@link #hides}).
 */
public final class BlinkTomes implements TargetGuard, io.github.butterflysmp.rpg.paper.adapter.AttackSink {

    /** K9: the blink's cooldown in the engine's one cooldown store. Cleared on quit (that store's rule) and by clear_state. */
    public static final String COOLDOWN_KEY = "accessory:blink";

    /** K7 (CC'S CALL): how far from where he stood the forget pass looks for mobs targeting him. */
    public static final double FORGET_RADIUS = 64.0;

    /**
     * K3, READ in the 26.1.2 server jar (every block class overriding {@code stepOn}, {@code entityInside} or
     * {@code fallOn}): a floor that hurts whoever stands on it ({@code MagmaBlock.stepOn}; {@code CampfireBlock},
     * {@code CactusBlock}, {@code LavaCauldronBlock} {@code entityInside}; {@code PointedDripstoneBlock.fallOn}), and
     * (CC'S CALL) the sculk shrieker, whose {@code stepOn} can summon a warden, and the big dripleaf, which tips and drops
     * him.
     */
    static final Set<Material> HARMFUL_FLOOR = EnumSet.of(Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.CACTUS, Material.POINTED_DRIPSTONE, Material.LAVA_CAULDRON, Material.SCULK_SHRIEKER,
            Material.BIG_DRIPLEAF, Material.POWDER_SNOW);

    /**
     * K3, READ likewise: a block that hurts, freezes or carries off whoever stands in it ({@code BaseFireBlock},
     * {@code SweetBerryBushBlock}, {@code WitherRoseBlock}, {@code PowderSnowBlock}, {@code NetherPortalBlock},
     * {@code EndPortalBlock}, {@code EndGatewayBlock} {@code entityInside}); and (CC'S CALL) a cobweb, which holds him.
     */
    static final Set<Material> HARMFUL_BODY = EnumSet.of(Material.FIRE, Material.SOUL_FIRE, Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE, Material.POWDER_SNOW, Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_GATEWAY,
            Material.COBWEB, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.LAVA_CAULDRON);

    private final Scheduler scheduler;
    private final Keys keys;
    private final BlinkedPlayers blinked;
    private final CooldownTracker cooldowns;
    private final Logger log;
    private final BooleanSupplier trace;
    private final IntSupplier tick;
    private final Function<UUID, AccessoryContributions> contributions;
    private final RandomGenerator random;

    private final Map<UUID, TaskHandle> running = new ConcurrentHashMap<>();
    /** K3: the tick before which no search runs again for him, after one found nowhere safe. */
    private final Map<UUID, Integer> retryAt = new ConcurrentHashMap<>();

    public BlinkTomes(Scheduler scheduler, Keys keys, BlinkedPlayers blinked, CooldownTracker cooldowns, Logger log,
                      BooleanSupplier trace, IntSupplier tick, Function<UUID, AccessoryContributions> contributions,
                      RandomGenerator random) {
        this.scheduler = scheduler;
        this.keys = keys;
        this.blinked = blinked;
        this.cooldowns = cooldowns;
        this.log = log;
        this.trace = trace;
        this.tick = tick;
        this.contributions = contributions;
        this.random = random;
    }

    /** The first blink among a reading's active behaviours, if any. */
    static Optional<ActiveBehaviour> blinkOf(List<ActiveBehaviour> active) {
        for (ActiveBehaviour a : active) {
            if (a.behaviour() instanceof AccessoryBehaviour.Blink) return Optional.of(a);
        }
        return Optional.empty();
    }

    // --- THE CLOCK (K1, K2) -------------------------------------------------------------------------------------------

    /** Called by the reconcile pass, on his thread, with what its one reading found active. */
    public void reconcile(Player player, List<ActiveBehaviour> active) {
        UUID id = player.getUniqueId();
        Optional<ActiveBehaviour> b = blinkOf(active);
        TaskHandle current = running.get(id);
        if (b.isEmpty() || player.isDead()) {
            if (current != null) stopClock(id, "inactive");
            return;
        }
        if (current != null && current.isRunning()) return;
        startClock(player, b.get());
    }

    /** Is a check clock running for him? For the selftest's probes and the wiring test. */
    public boolean runs(UUID id) {
        TaskHandle t = running.get(id);
        return t != null && t.isRunning();
    }

    private void startClock(Player player, ActiveBehaviour active) {
        UUID id = player.getUniqueId();
        TaskHandle[] self = new TaskHandle[1];
        TaskHandle task = RepeatingTask.start(new EntityTaskTarget(player, scheduler, log), Blinks.CHECK_PERIOD_TICKS,
                "blink-check", () -> {
                    // THE CHECK-TIME RE-CHECK, through the reconcile's own function (K1): inactive now is no check, and the end.
                    Optional<ActiveBehaviour> still = blinkOf(contributions.apply(id).behaviours());
                    if (still.isEmpty()) return false;
                    check(player, (AccessoryBehaviour.Blink) still.get().behaviour());
                    return true;
                }, () -> {
                    if (running.remove(id, self[0]) && trace.getAsBoolean()) {
                        log.info("BLINK STOP wearer=" + player.getName() + " tick=" + tick.getAsInt() + " reason=ended");
                    }
                });
        self[0] = task;
        running.put(id, task);
        if (trace.getAsBoolean()) {
            log.info("BLINK START wearer=" + player.getName() + " accessory=" + active.accessoryId() + " slot="
                    + active.slot() + " tick=" + tick.getAsInt() + " period=" + Blinks.CHECK_PERIOD_TICKS);
        }
    }

    private void stopClock(UUID id, String why) {
        TaskHandle t = running.remove(id);
        if (t == null) return;
        t.cancel();
        if (trace.getAsBoolean()) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            log.info("BLINK STOP wearer=" + (p == null ? id.toString() : p.getName()) + " tick=" + tick.getAsInt()
                    + " reason=" + why);
        }
    }

    /** One check, on his thread. */
    private void check(Player player, AccessoryBehaviour.Blink blink) {
        UUID id = player.getUniqueId();
        int now = tick.getAsInt();
        Location feet = player.getLocation();
        Blinks.Wearer wearer = new Blinks.Wearer(player.getWorld().getName(), vec(feet), !player.isDead(),
                player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR,
                blinked.isBlinked(id), cooldowns.ticksRemaining(id, COOLDOWN_KEY), now < retryAt.getOrDefault(id, 0));
        if (Blinks.held(wearer) != Blinks.Held.NONE) return;   // the cheap answer first: no entity is read
        double r = blink.triggerRadius();
        List<Blinks.Near> nearby = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof LivingEntity l)) continue;
            boolean targetsHim = l instanceof Mob m && player.equals(m.getTarget());
            nearby.add(new Blinks.Near(l.getUniqueId(), l.getType().getKey().getKey(), CombatSides.of(l, keys), targetsHim,
                    l.getWorld().getName(), vec(l.getLocation()), l.isValid() && !l.isDead()));
        }
        Optional<Blinks.Trigger> trigger = Blinks.trigger(blink, wearer, nearby);
        if (trigger.isEmpty()) return;
        blink(player, blink, trigger.get(), now);
    }

    // --- THE BLINK (K3, K4, K6, K7, K9, K11, K12) ---------------------------------------------------------------------

    private void blink(Player player, AccessoryBehaviour.Blink blink, Blinks.Trigger trigger, int now) {
        UUID id = player.getUniqueId();
        World world = player.getWorld();
        Location from = player.getLocation();
        double reach = blink.range() + blink.clearRadius();
        List<Vec3> hostiles = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(reach, reach, reach)) {
            if (e instanceof Mob m && blocksALanding(m)) hostiles.add(vec(m.getLocation()));
        }
        Blinks.Search search = Blinks.search(blink, vec(from), ground(world), hostiles);
        Optional<Blinks.Spot> spot = Blinks.choose(search.safe(), random);
        String triggerText = trigger.mob().type() + " " + trigger.mob().id();
        if (spot.isEmpty()) {
            retryAt.put(id, now + Blinks.NONE_SAFE_RETRY_TICKS);
            if (trace.getAsBoolean()) log.info(noneSafeLine(player.getName(), now, triggerText, search.tried()));
            return;   // K3: nothing happens, the cooldown is NOT spent
        }
        retryAt.remove(id);
        Location to = new Location(world, spot.get().feet().x(), spot.get().feet().y(), spot.get().feet().z(),
                from.getYaw(), from.getPitch());

        // The state first, so a target event during the teleport's hop is already refused.
        Optional<Blinks.Effect> before = invisibility(player);
        Blinks.InvisibilityAtBlink invis = Blinks.invisibilityAtBlink(blink, before);
        blinked.begin(id, new BlinkedPlayers.State(now, now + blink.invisibilityTicks(), invis));
        cooldowns.trigger(id, COOLDOWN_KEY, blink.cooldownTicks());
        if (invis.apply()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            Blinks.Effect ours = Blinks.ours(blink);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ours.durationTicks(), ours.amplifier(),
                    ours.ambient(), ours.particles(), ours.icon()));
        }

        // K7: every mob targeting him, read where he stood, forgets him on its own thread.
        int forgot = 0;
        for (Entity e : world.getNearbyEntities(from, FORGET_RADIUS, FORGET_RADIUS, FORGET_RADIUS)) {
            if (!(e instanceof Mob m) || !player.equals(m.getTarget())) continue;
            forgot++;
            scheduler.onEntity(m, () -> forget(m, player));
        }

        feedback(player, from, blink);
        player.setFallDistance(0f);
        player.teleportAsync(to).thenAccept(ok -> {
            player.setFallDistance(0f);
            feedback(player, player.getLocation(), blink);
        });
        if (trace.getAsBoolean()) {
            log.info(blinkLine(player.getName(), now, triggerText, trigger.distance(), vec(from), spot.get().feet(),
                    search.safe().size(), forgot));
        }
        // K4: the expiry, on his own thread; it ends only THIS blink (a cancelled one and a new one are told apart).
        scheduler.onEntityLater(player, () -> end(player, Blinks.EndReason.EXPIRED, now), blink.invisibilityTicks());
    }

    /** A mob near a landing spot that rules it out (K3): hostile by kind, one of ours, or after somebody now. */
    private boolean blocksALanding(Mob m) {
        if (!m.isValid() || m.isDead()) return false;
        if (CombatSides.of(m, keys) != MobHostility.Side.MOB) return false;
        return m instanceof Enemy || m.getPersistentDataContainer().has(keys.mobId) || m.getTarget() != null;
    }

    /** K7: on the mob's own thread. Logged when the target reading still names him (a brain-driven mob). */
    private void forget(Mob m, Player him) {
        if (!m.isValid()) return;
        if (him.equals(m.getTarget())) m.setTarget(null);
        if (m instanceof Wolf w && w.isAngry()) w.setAngry(false);
        if (m instanceof PigZombie z && z.isAngry()) z.setAngry(false);
        if (m instanceof Bee b && b.getAnger() > 0) b.setAnger(0);
        if (m instanceof Warden w) w.clearAnger(him);
        // A piglin's ANGRY_AT memory (INFERRED: Brain.setMemory with null erases it; CraftMemoryMapper.toNms maps null to
        // null, READ). Asked of piglins only: a brain without the module is not asked.
        if (m instanceof PiglinAbstract && him.getUniqueId().equals(m.getMemory(MemoryKey.ANGRY_AT))) {
            m.setMemory(MemoryKey.ANGRY_AT, null);
        }
        if (him.equals(m.getTarget()) && trace.getAsBoolean()) {
            log.info("BLINK KEPT mob=" + m.getType().getKey().getKey() + " " + m.getUniqueId() + " tick=" + tick.getAsInt()
                    + " reason=brain");
        }
    }

    // --- THE STATE'S END (K4, K5, K6) ---------------------------------------------------------------------------------

    /**
     * K5: his attack. Called at the one place a weapon trigger fires past its gates ({@code WeaponFire}) and the one place
     * his melee hit lands ({@code RpgListeners.onPlayerMeleeAttack}); a no-op when he is not blinked.
     */
    @Override
    public void attacked(Player player, Blinks.EndReason reason) {
        if (blinked.isBlinked(player.getUniqueId())) end(player, reason, -1);
    }

    /** K4: death. */
    public void onDeath(Player player) {
        if (blinked.isBlinked(player.getUniqueId())) end(player, Blinks.EndReason.DEATH, -1);
    }

    /** K4: quit, on his thread while he is still valid; his clock stops too. */
    public void onQuit(Player player) {
        UUID id = player.getUniqueId();
        if (blinked.isBlinked(id)) end(player, Blinks.EndReason.QUIT, -1);
        stopClock(id, "quit");
        retryAt.remove(id);
    }

    /** K9: the selftest's clear_state and row-start clean. The cooldown is cleared by its own store's clear. */
    public boolean clear(Player player) {
        retryAt.remove(player.getUniqueId());
        if (!blinked.isBlinked(player.getUniqueId())) return false;
        end(player, Blinks.EndReason.CLEARED, -1);
        return true;
    }

    /** End his state ({@code startTick} >= 0: only if it is still that blink). On his own thread. */
    private void end(Player player, Blinks.EndReason reason, long startTick) {
        UUID id = player.getUniqueId();
        Optional<BlinkedPlayers.State> s = startTick >= 0 ? blinked.endIfStarted(id, startTick) : blinked.end(id);
        if (s.isEmpty()) return;
        int now = tick.getAsInt();
        int elapsed = (int) (now - s.get().startTick());
        // The state's own span is the effect's length, so this holds even when the tome came off mid-blink (CC'S CALL:
        // unequipping or a class change does not end the state; it runs to its own end).
        int span = (int) (s.get().endTick() - s.get().startTick());
        Blinks.InvisibilityAtEnd act = Blinks.invisibilityAtEnd(span, s.get().invisibility(), elapsed, invisibility(player));
        if (act.action() != Blinks.EndAction.NOTHING) player.removePotionEffect(PotionEffectType.INVISIBILITY);
        act.restore().ifPresent(e -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                e.durationTicks(), e.amplifier(), e.ambient(), e.particles(), e.icon())));
        if (trace.getAsBoolean()) log.info(endLine(player.getName(), now, reason));
    }

    private static Optional<Blinks.Effect> invisibility(Player p) {
        PotionEffect e = p.getPotionEffect(PotionEffectType.INVISIBILITY);
        if (e == null) return Optional.empty();
        return Optional.of(new Blinks.Effect(e.isInfinite() ? Blinks.Effect.INFINITE : e.getDuration(), e.getAmplifier(),
                e.isAmbient(), e.hasParticles(), e.hasIcon()));
    }

    // --- THE GUARD (K7) -----------------------------------------------------------------------------------------------

    @Override
    public boolean hides(Entity entity) {
        return entity instanceof Player p && blinked.isBlinked(p.getUniqueId());
    }

    @Override
    public boolean refuses(Entity mob, LivingEntity target) {
        if (!(target instanceof Player p)) return false;
        Optional<BlinkedPlayers.State> s = blinked.state(p.getUniqueId());
        if (s.isEmpty()) return false;
        if (s.get().refusedLogged().add(mob.getUniqueId()) && trace.getAsBoolean()) {
            log.info(refusedLine(mob.getType().getKey().getKey(), mob.getUniqueId(), tick.getAsInt()));
        }
        return true;
    }

    // --- K11: FEEDBACK -----------------------------------------------------------------------------------------------

    private void feedback(Player player, Location at, AccessoryBehaviour.Blink blink) {
        if (blink.particle() != null) {
            at.getWorld().spawnParticle(Particle.valueOf(blink.particle().toUpperCase(Locale.ROOT)), at.clone().add(0, 1.0, 0),
                    blink.particleCount(), 0.3, 0.5, 0.3, 0.02);
        }
        if (blink.sound() != null) at.getWorld().playSound(at, blink.sound(), 1.0f, 1.0f);
        if (trace.getAsBoolean()) {
            log.info(String.format(Locale.ROOT, "BLINK feedback wearer=%s at=%.2f %.2f %.2f particle=%sx%d sound=%s",
                    player.getName(), at.getX(), at.getY(), at.getZ(), blink.particle(), blink.particleCount(), blink.sound()));
        }
    }

    // --- K3: THE WORLD AS THE SEARCH READS IT -------------------------------------------------------------------------

    /** Read on his thread; {@link Blinks#search} asks {@code loaded} before any block, so no chunk is ever loaded. */
    static Blinks.Ground ground(World world) {
        int min = world.getMinHeight();
        int max = world.getMaxHeight();
        return new Blinks.Ground() {
            @Override public boolean loaded(int x, int z) {
                return world.isChunkLoaded(x >> 4, z >> 4);
            }

            @Override public boolean insideBorder(int x, int z) {
                return world.getWorldBorder().isInside(new Location(world, x + 0.5, min, z + 0.5));
            }

            @Override public Blinks.Floor floor(int x, int y, int z) {
                if (y < min || y >= max) return Blinks.Floor.NOT_SOLID;
                Material m = world.getBlockAt(x, y, z).getType();
                return floorOf(m, m.isSolid());
            }

            @Override public Blinks.Body body(int x, int y, int z) {
                if (y < min || y >= max) return Blinks.Body.SOLID;
                Block b = world.getBlockAt(x, y, z);
                boolean wet = b.isLiquid() || (b.getBlockData() instanceof Waterlogged w && w.isWaterlogged());
                return bodyOf(b.getType(), wet, b.isPassable());
            }
        };
    }

    /** A floor block's kind. Pure over the material, so {@code BlinkGroundTest} pins it. */
    static Blinks.Floor floorOf(Material m, boolean solid) {
        if (HARMFUL_FLOOR.contains(m)) return Blinks.Floor.HARMFUL;
        return solid ? Blinks.Floor.SAFE : Blinks.Floor.NOT_SOLID;
    }

    /** A body block's kind: harmful first (fire is passable), then fluid, then collision. */
    static Blinks.Body bodyOf(Material m, boolean wet, boolean passable) {
        if (HARMFUL_BODY.contains(m)) return Blinks.Body.HARMFUL;
        if (wet) return Blinks.Body.FLUID;
        return passable ? Blinks.Body.CLEAR : Blinks.Body.SOLID;
    }

    // --- K12: THE TRACE LINES, pinned by BlinkTomesWiringTest ----------------------------------------------------------

    static String blinkLine(String wearer, int tick, String trigger, double dist, Vec3 from, Vec3 to, int safe, int forgot) {
        return String.format(Locale.ROOT, "BLINK wearer=%s tick=%d trigger=%s dist=%.3f from=%.3f %.3f %.3f to=%.3f %.3f %.3f"
                        + " safe=%d forgot=%d", wearer, tick, trigger, dist, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(),
                safe, forgot);
    }

    static String noneSafeLine(String wearer, int tick, String trigger, int searched) {
        return "BLINK NONE-SAFE wearer=" + wearer + " tick=" + tick + " trigger=" + trigger + " searched=" + searched;
    }

    static String refusedLine(String mobType, UUID mob, int tick) {
        return "BLINK REFUSED-TARGET mob=" + mobType + " " + mob + " tick=" + tick;
    }

    static String endLine(String wearer, int tick, Blinks.EndReason reason) {
        return "BLINK END wearer=" + wearer + " tick=" + tick + " reason=" + reason.token();
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
