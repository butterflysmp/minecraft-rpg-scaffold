package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobElementResolver;
import io.github.butterflysmp.rpg.core.mob.MobGearScore;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
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
 * THE PHAGE'S DETONATION (PLAN-dragon-mobs.md step R3). The rules are core's ({@link MobBlast},
 * {@link MobHostility}); this is the Bukkit half, called from the one place a Phage's fuse ends:
 * {@code RpgListeners.onCustomCreeperPrime}, AFTER it has cancelled the vanilla prime.
 *
 * <h2>WHAT IT DOES, IN ORDER</h2>
 * <ol>
 *   <li><b>The victims</b> (the seat's ruling 24): every living creature within the blast's REACH, feet to feet, that
 *       {@link MobHostility#hostileTo} names. Today that is a survival or adventure player; a mob (the Phage's own
 *       kind included) and a creative or spectator player are SPARED, and named in the log line as spared. The
 *       search is a cube of the reach; {@link MobBlast#reach} decides the sphere.</li>
 *   <li><b>The damage</b> (P-a and P-b, the seat's; the shape Ben's, "keep vanillas shap"): the stage's authored
 *       damage, x GS/100 ({@link MobBlast#price}), is the damage at ZERO distance; each victim takes it x
 *       {@link MobBlast#falloff} of its distance, vanilla's curve normalised. No cover (PLAN Q34). A
 *       victim whose share is 0 (exactly at the reach) is listed and not hit. Each other victim is hit through
 *       {@code BukkitCombatant.of(victim).handle().applyDamage(dealt, phageId)}, the two-argument form a mob's melee
 *       hit takes ({@code RpgListeners.onMobMeleeAttack}), so the store applies the victim's defence exactly as for a
 *       melee hit, and the hit wears no element and cannot crit. <b>A shield cannot block it</b>: our block is read
 *       from a vanilla damage event (ShieldBlock), and this hit raises none (PLAN Q32).</li>
 *   <li><b>The push</b> (Ben, 2026-11-17: "knockback sounds like a good addition"; PLAN-dragon-mobs.md section 7 F-B,
 *       READ): each victim hit is pushed by vanilla's strength, {@link MobBlast#knockbackStrength}, linear in
 *       distance, times content's multiplier and {@code 1 - } the victim's {@code EXPLOSION_KNOCKBACK_RESISTANCE},
 *       along the blast's centre to the victim's EYE ({@link MobBlast#pushVector}), added to its velocity with
 *       {@code setVelocity}, which marks it for the client. <b>Only victims</b>: a spared mob or creative player is
 *       never pushed (vanilla pushes mobs too; the seat ruled players only). <b>No knockback event is raised</b>:
 *       vanilla's chain is built inside {@code CraftEventFactory}, and Paper's one public constructor is
 *       {@code @ApiStatus.Internal} (the plan's F-B; the seat's to rule).</li>
 *   <li><b>The sight and sound</b>: the large explosion particle and the generic explosion sound, the pair a vanilla
 *       creeper's blast sends ({@code Level.explode} passes {@code ParticleTypes.EXPLOSION_EMITTER} and
 *       {@code SoundEvents.GENERIC_EXPLODE}). A particle and a sound touch no block and hurt nothing.</li>
 *   <li><b>One line</b>, {@code BLAST ...} (the seat's ruling 26), naming the mob, its uuid, stage, gear score, the
 *       zero-distance damage ({@code base}) and the reach; each victim with its distance, its factor and the damage it
 *       was dealt BEFORE its defence; and each spared creature with its distance and factor. The amount after defence
 *       is the store's, a tick later (the selftest's HURT line).</li>
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
 * edge) is hit from its own thread instead, a tick later; on Paper every victim is owned.
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
        double r = blast.reach();
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
        MobBlast.Reach reach = blast.reach(phageId, world.getName(), vec(at), bystanders);

        Map<UUID, double[]> pushes = new HashMap<>();   // victim -> {resistance, strength}, for the line
        for (MobBlast.Hit hit : reach.victims()) {
            double dealt = base * hit.factor();
            if (!(dealt > 0)) continue;   // exactly at the reach: listed, and nothing to deal (and no push: factor 0)
            LivingEntity victim = bodies.get(hit.who().id());
            // THE PUSH, vanilla's (PLAN-dragon-mobs.md section 7 F-B): the linear strength with the victim's own
            // explosion knockback resistance, from the blast's centre to the victim's EYE. Computed HERE, at the
            // blast, from the positions at the blast; applied with setVelocity, the plugin route that reaches a
            // player's client (hurtMarked). Only a victim is pushed: a spared creature is never in this loop.
            var attr = victim.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
            double resistance = attr == null ? 0.0 : attr.getValue();
            double strength = blast.knockbackAt(hit.distance(), resistance);
            pushes.put(victim.getUniqueId(), new double[] {resistance, strength});
            Vec3 push = MobBlast.pushVector(vec(at), vec(victim.getEyeLocation()), strength);
            Vector velocity = new Vector(push.x(), push.y(), push.z());
            if (Bukkit.isOwnedByCurrentRegion(victim)) {
                BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, phageId);
                if (strength > 0) victim.setVelocity(victim.getVelocity().add(velocity));
            } else {
                adapters.scheduler().onEntity(victim, () -> {
                    BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, phageId);
                    if (strength > 0) victim.setVelocity(victim.getVelocity().add(velocity));
                });
            }
        }

        world.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4.0f, 0.7f);

        adapters.log().info(head(mobId, phageId, stage, gs, base, r, blast.knockback())
                + " victims=" + reach.victims().size() + " " + names(reach.victims(), bodies, base, pushes, r, adapters)
                + " spared=" + reach.spared().size() + " "
                + names(reach.spared(), bodies, Double.NaN, Map.of(), r, adapters));

        MobGrower.onDeath(phage, mobs, elements, adapters.keys(), adapters.stats(), adapters.scheduler(),
                adapters.log());
        phage.remove();
        return reach.victims().size();
    }

    /**
     * {@code [<entry>, ...]}, in the order the rule kept ({@link #entry}). {@code dealt} and the push are printed for
     * victims only ({@code base} is NaN for the spared, who are neither hurt nor pushed). A victim exactly at the
     * reach was neither hit nor pushed and has no recorded push: its {@code knock} and {@code push} print as 0.
     */
    private static String names(List<MobBlast.Hit> list, Map<UUID, LivingEntity> bodies, double base,
                                Map<UUID, double[]> pushes, double reach, AdapterContext adapters) {
        List<String> out = new ArrayList<>();
        for (MobBlast.Hit hit : list) {
            LivingEntity e = bodies.get(hit.who().id());
            String id = e.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
            String kind = e.getType().getKey().getKey() + (id == null ? "" : "/" + id);
            if (Double.isNaN(base)) {
                out.add(entry(kind, hit.who().id(), hit.distance(), hit.factor(), Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN));
            } else {
                double[] p = pushes.getOrDefault(hit.who().id(), new double[] {0.0, 0.0});
                out.add(entry(kind, hit.who().id(), hit.distance(), hit.factor(), base * hit.factor(),
                        MobBlast.knockbackFactor(hit.distance(), reach), p[0], p[1]));
            }
        }
        return out.toString();
    }

    /** The BLAST line's head: the mob, its stage and gear score, the zero-distance damage, the reach, the multiplier. */
    static String head(String mobId, UUID id, int stage, int gs, double base, double reach, double knockback) {
        return "BLAST mob=" + mobId + " id=" + id + " stage=" + stage + " gs=" + gs
                + String.format(Locale.ROOT, " base=%.3f reach=%.2f knockback=%.2f", base, reach, knockback);
    }

    /**
     * One creature's entry: {@code <type>[/<mob id>] <uuid> d=<feet to feet> factor=<damage share>}, and for a victim
     * {@code dealt=<before defence> knock=<1 - d/reach> res=<explosion knockback resistance> push=<strength applied>}.
     * {@code dealt} NaN marks a spared creature, and then nothing after the factor is printed.
     */
    static String entry(String kind, UUID id, double distance, double factor, double dealt, double knock,
                        double resistance, double push) {
        String s = kind + " " + id + String.format(Locale.ROOT, " d=%.3f factor=%.6f", distance, factor);
        if (Double.isNaN(dealt)) return s;
        return s + String.format(Locale.ROOT, " dealt=%.3f knock=%.6f res=%.3f push=%.6f", dealt, knock, resistance,
                push);
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
