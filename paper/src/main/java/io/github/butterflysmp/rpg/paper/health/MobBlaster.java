package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobElementResolver;
import io.github.butterflysmp.rpg.core.mob.MobGearScore;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.ImplosionDrags;
import io.github.butterflysmp.rpg.paper.adapter.ImplosionFooting;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * THE PHAGE'S DETONATION (PLAN-dragon-mobs.md step R3; since 2026-11-17, a VOID IMPLOSION). The rules are core's
 * ({@link MobBlast}, {@link Implosion}, {@link MobHostility}); this is the Bukkit half, called from the one place a
 * Phage's fuse ends: {@code RpgListeners.onCustomCreeperPrime}, AFTER it has cancelled the vanilla prime.
 *
 * <h2>WHAT IT DOES, IN ORDER</h2>
 * <ol>
 *   <li><b>The victims</b> (the seat's ruling 24): every living creature within the blast's REACH, feet to feet, that
 *       {@link MobHostility#hostileTo} names. Today that is a survival or adventure player; a mob (the Phage's own
 *       kind included) and a creative or spectator player are SPARED, and named in the log line as spared. The
 *       search is a cube of the reach; {@link MobBlast#reach} decides the sphere.</li>
 *   <li><b>The damage</b> (60 and 150, CONFIRMED BY PLAY; P-b the seat's; the shape Ben's, "keep vanillas shap"): the
 *       stage's authored damage, x GS/100 ({@link MobBlast#price}), is the damage at ZERO distance; each victim takes
 *       it x {@link MobBlast#falloff} of its distance, vanilla's curve normalised. No cover (PLAN Q34). A victim whose
 *       share is 0 (exactly at the reach) is listed and not hit. Each other victim is hit through
 *       {@code BukkitCombatant.of(victim).handle().applyDamage(dealt, phageId)}, the two-argument form a mob's melee
 *       hit takes ({@code RpgListeners.onMobMeleeAttack}), so the store applies the victim's defence exactly as for a
 *       melee hit, and the hit wears no element and cannot crit. <b>A shield cannot block it</b>: our block is read
 *       from a vanilla damage event (ShieldBlock), and this hit raises none (PLAN Q32).</li>
 *   <li><b>The pull</b> (Ben, 2026-11-17: "A void explosion that pulls things toward the origin", "swap the push for
 *       the pull"): each victim hit is pulled by the blast's {@link MobBlast#implosion}: vanilla's linear push curve x
 *       content's {@code pull} (1.25) x {@code 1 - } the victim's {@code EXPLOSION_KNOCKBACK_RESISTANCE}, HORIZONTAL,
 *       toward the Phage's feet, and CAPPED so the travel the jar's friction gives it stops half a block short of the
 *       Phage (core's {@link Implosion#pull}, the footing read by {@link ImplosionFooting}). ADDED to the victim's
 *       velocity with {@code setVelocity}, which marks it for the client. <b>Only victims</b>: a spared mob or creative
 *       player is never pulled. <b>No knockback event is raised</b>, as none was for the push (F7).</li>
 *   <li><b>The sight and sound</b>: the content visual the blast names ({@code void_implosion}: a black and a dark
 *       purple spiral drawn INWARD from the reach over 10 ticks, and a deep sound), drawn at the blast's reach, in
 *       place of vanilla's explosion emitter and generic explode. A particle and a sound touch no block and hurt
 *       nothing.</li>
 *   <li><b>One line</b>, {@code BLAST ...} (the seat's ruling 26), naming the mob, its uuid, stage, gear score, the
 *       zero-distance damage ({@code base}), the reach, the pull multiplier and the visual; each victim with its
 *       distance, its factor, the damage it was dealt BEFORE its defence, and its pull (the linear factor, its
 *       resistance, whether it stood on the ground, the model's per-tick multiplier, the uncapped strength, the cap,
 *       the speed applied, and whether the cap bound); and each spared creature with its distance and factor. The
 *       amount after defence is the store's, a tick later (the selftest's HURT line).</li>
 *   <li><b>Growth</b> (the seat's ruling 23): {@link MobGrower#onDeath}, growth's death path, which needs no event. It
 *       marks the Phage counted (once only), resolves its element (void) and calls the seam
 *       {@link MobGrower#reportDeath} with the PHAGE'S OWN id as the dead creature, which never grows itself. Called
 *       while the Phage still stands where it blew.</li>
 *   <li><b>Removal without a death</b> (the seat's ruling 22): {@code remove()} is {@code discard(PLUGIN)}
 *       ({@code CraftEntity#remove}), which raises no {@code EntityDeathEvent}, so the one death handler, and the split
 *       inside it, never sees the Phage. A stage 5 that blows leaves no children (Ben, E).</li>
 * </ol>
 *
 * <p><b>Threading.</b> Called on the Phage's own thread, inside its tick ({@code Creeper.tick} calls
 * {@code explodeCreeper()}, which fires the prime). A victim this thread does not own (possible on Folia at a region
 * edge) is hit and pulled from its own thread instead, a tick later, with the pull computed HERE from the footing at
 * the blast; on Paper every victim is owned.
 */
public final class MobBlaster {

    private MobBlaster() {}

    /**
     * Detonate {@code phage}, one of our creepers whose vanilla prime was just cancelled. A creeper of ours whose file
     * authors no blast does nothing here: its prime stays cancelled and it lives, as in step R1.
     *
     * @return how many victims were hit
     */
    public static int detonate(Creeper phage, String mobId, MobRegistry mobs, MobElementResolver elements,
                               AdapterContext adapters) {
        MobDefinition def = mobs.find(mobId).orElse(null);
        if (def == null || def.blast() == null) return 0;
        MobBlast blast = def.blast();
        var pdc = phage.getPersistentDataContainer();
        int stage = MobBodyApplier.stageOf(phage, def, adapters.keys(), adapters.log());
        boolean hostile = MobClassifier.isHostile(phage);
        Integer stored = pdc.get(adapters.keys().mobGearScore, PersistentDataType.INTEGER);
        boolean validScore = stored != null && MobGearScore.isValidStored(stored);
        int gs = validScore ? stored : MobScaling.GS_BASELINE;
        if (hostile && !validScore) {
            // Visible, never silent, as M16's ABSENT is: priced at the baseline, so it still deals its authored damage.
            adapters.log().warning("BLAST " + mobId + " " + phage.getUniqueId() + " has no valid gear score (stored "
                    + stored + "), so it is priced at GS " + gs);
        }
        double base = blast.price(stage, def.stages(), hostile, gs);   // at zero distance

        World world = phage.getWorld();
        Location at = phage.getLocation();
        UUID phageId = phage.getUniqueId();
        // THE STAGE'S REACH (the void implosion's Part C): stage 5's 6 blocks, stage 1's 2, by height.
        double r = blast.reachAt(stage, def.stages());
        List<MobBlast.Bystander> bystanders = new ArrayList<>();
        Map<UUID, LivingEntity> bodies = new HashMap<>();
        for (Entity e : world.getNearbyEntities(at, r, r, r)) {
            if (!LiveTargets.isLive(e)) continue;
            LivingEntity living = (LivingEntity) e;
            boolean exempt = living instanceof Player p
                    && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
            bystanders.add(new MobBlast.Bystander(living.getUniqueId(),
                    living instanceof Player ? MobHostility.Side.PLAYER : MobHostility.Side.MOB, exempt,
                    living.getWorld().getName(), vec(living.getLocation())));
            bodies.put(living.getUniqueId(), living);
        }
        MobBlast.Reach reach = MobBlast.reach(phageId, world.getName(), vec(at), r, bystanders);

        Implosion shape = blast.implosion(stage, def.stages());
        Map<UUID, Implosion.Pull> pulls = new HashMap<>();   // victim -> its pull, for the line
        for (MobBlast.Hit hit : reach.victims()) {
            double dealt = base * hit.factor();
            if (!(dealt > 0)) continue;   // exactly at the reach: listed, and nothing to deal (and nothing to pull)
            LivingEntity victim = bodies.get(hit.who().id());
            // THE PULL, the shape's: computed HERE, at the blast, from the victim's footing at the blast (feet, eye
            // height, ground, the block's friction, its explosion knockback resistance): the push's own lift, and
            // 1.25 x its horizontal toward the Phage, scaled down so the modelled rest point stops half a block short.
            Implosion.Pull pull = shape.pull(vec(at), ImplosionFooting.of(victim, shape.resistance())).orElse(null);
            if (pull != null) pulls.put(victim.getUniqueId(), pull);
            Vec3 i = pull == null ? Vec3.ZERO : pull.impulse();
            Vector velocity = new Vector(i.x(), i.y(), i.z());
            boolean moves = i.x() != 0 || i.y() != 0 || i.z() != 0;
            boolean safe = pull != null && pull.safeLanding();
            Runnable land = () -> {
                BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, phageId);
                // Near the Phage the lift carries a player above the safe fall: the dash's mark (ruling 31) covers
                // the landing, armed BEFORE the take-off, on the victim's own thread.
                if (safe) adapters.safeLandings().arm(victim, adapters.scheduler());
                if (moves) victim.setVelocity(victim.getVelocity().add(velocity));
            };
            if (Bukkit.isOwnedByCurrentRegion(victim)) land.run();
            else adapters.scheduler().onEntity(victim, land);
        }

        new PaperCombatWorld(world, adapters).present(vec(at), blast.visual(), r);

        adapters.log().info(head(mobId, phageId, stage, gs, base, r, blast.pull(), blast.lift(), blast.drag(),
                blast.durationTicks(), blast.visual())
                + " victims=" + reach.victims().size() + " " + names(reach.victims(), bodies, base, pulls, r, adapters)
                + " spared=" + reach.spared().size() + " "
                + names(reach.spared(), bodies, Double.NaN, Map.of(), r, adapters));

        // THE DRAG (2026-11-18; Ben's four yeses): the second after the blast, from the Phage's feet, on every player
        // it is hostile to, found afresh each tick (ImplosionDrags). The damage above was the blast's instant, once.
        // Its tick 0 runs here, on the Phage's thread, which owns the origin; the Phage itself is removed below and is
        // never one of its own victims.
        if (shape.drags()) {
            ImplosionDrags.start(adapters, world, vec(at), shape, l -> !l.getUniqueId().equals(phageId)
                    && MobHostility.hostileTo(l instanceof Player ? MobHostility.Side.PLAYER : MobHostility.Side.MOB,
                    ImplosionFooting.exempt(l)), "phage", phageId);
        }

        MobGrower.onDeath(phage, mobs, elements, adapters.keys(), adapters.stats(), adapters.scheduler(),
                adapters.log());
        phage.remove();
        return reach.victims().size();
    }

    /**
     * {@code [<entry>, ...]}, in the order the rule kept ({@link #entry}). {@code dealt} and the pull are printed for
     * victims only ({@code base} is NaN for the spared, who are neither hurt nor pulled). A victim exactly at the reach
     * was neither hit nor pulled and has no recorded pull: its pull fields print as 0.
     */
    private static String names(List<MobBlast.Hit> list, Map<UUID, LivingEntity> bodies, double base,
                                Map<UUID, Implosion.Pull> pulls, double reach, AdapterContext adapters) {
        List<String> out = new ArrayList<>();
        for (MobBlast.Hit hit : list) {
            LivingEntity e = bodies.get(hit.who().id());
            String id = e.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
            String kind = e.getType().getKey().getKey() + (id == null ? "" : "/" + id);
            if (Double.isNaN(base)) {
                out.add(spared(kind, hit.who().id(), hit.distance(), hit.factor()));
            } else {
                out.add(entry(kind, hit.who().id(), hit.distance(), hit.factor(), base * hit.factor(),
                        Implosion.factor(hit.distance(), reach), pulls.get(hit.who().id())));
            }
        }
        return out.toString();
    }

    /**
     * The BLAST line's head: the mob, its stage and gear score, the zero-distance damage, the stage's reach, the pull
     * and the lift, the drag and its duration in ticks (2026-11-18), the look.
     */
    static String head(String mobId, UUID id, int stage, int gs, double base, double reach, double pull, double lift,
                       double drag, int duration, String visual) {
        return "BLAST mob=" + mobId + " id=" + id + " stage=" + stage + " gs=" + gs
                + String.format(Locale.ROOT, " base=%.3f reach=%.2f pull=%.2f lift=%.2f drag=%.3f duration=%d", base,
                reach, pull, lift, drag, duration)
                + " visual=" + visual;
    }

    /** A SPARED creature's entry: {@code <type>[/<mob id>] <uuid> d=<feet to feet> factor=<damage share>}, no more. */
    static String spared(String kind, UUID id, double distance, double factor) {
        return kind + " " + id + String.format(Locale.ROOT, " d=%.3f factor=%.6f", distance, factor);
    }

    /**
     * A VICTIM's entry: {@link #spared}'s, then {@code dealt=<before defence> knock=<1 - d/reach> res=<resistance>
     * h=<horizontal distance, the cap's> ground=<on the ground> tf=<the model's blocks of rest per unit of horizontal
     * speed> strength=<the uncapped horizontal> cap=<cap speed> pull=<horizontal applied> capped=<the cap bound>
     * lift=<the vertical applied> apex=<its height> rest=<the modelled rest point, blocks> safe=<the mark armed>}.
     * {@code p} null (a victim exactly at the reach) prints every pull field as 0.
     */
    static String entry(String kind, UUID id, double distance, double factor, double dealt, double knock,
                        Implosion.Pull p) {
        return spared(kind, id, distance, factor) + String.format(Locale.ROOT,
                " dealt=%.3f knock=%.6f res=%.3f h=%.3f ground=%b tf=%.6f strength=%.6f cap=%.6f pull=%.6f capped=%b"
                        + " lift=%.6f apex=%.3f rest=%.3f safe=%b",
                dealt, knock, p == null ? 0.0 : p.resistance(), p == null ? 0.0 : p.horizontal(),
                p != null && p.onGround(), p == null ? 0.0 : p.travelFactor(), p == null ? 0.0 : p.strength(),
                p == null ? 0.0 : p.capSpeed(), p == null ? 0.0 : p.applied(), p != null && p.capped(),
                p == null ? 0.0 : p.vertical(), p == null ? 0.0 : p.apex(), p == null ? 0.0 : p.travel(),
                p != null && p.safeLanding());
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
