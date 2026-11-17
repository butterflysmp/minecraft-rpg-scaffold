package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobShield;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.adapter.ShieldSink;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * THE PHALANX'S ONE-HIT SHIELD, the Bukkit half (dragon-mobs step R7b). Ben: "1. yes the first damage of any kind".
 * cfde822 (section 3.4, D5): the first damage of any cause is cancelled, a shield-break sound, the off hand cleared, the
 * byte removed, and nothing gives it back (READ).
 *
 * <ul>
 *   <li><b>Up</b> while the mob carries {@code mob_shield} (set at its spawn, {@code MobSpawner.stampCustom}). In the
 *       PDC, so it survives an unload and a restart.</li>
 *   <li><b>Spent</b> by the first hit of a POSITIVE amount to reach {@code BukkitCombatant}'s {@code applyDamage}, the one
 *       point every damage path passes ({@link ShieldSink}). Absorbed WHOLE: no health lost. A hit of zero leaves it up
 *       ({@link MobShield#spends}) and logs that it did.</li>
 *   <li><b>Nothing rides through it</b> (CHOSEN, for Ben): a status, a push or an impulse on the mob later in the
 *       SAME tick is dropped and logged; and a vanilla push on a mob whose shield is up is cancelled
 *       ({@code RpgListeners.onShieldedKnockback}), because that push belongs to the hit the shield will eat. A status or
 *       push that a payload applies BEFORE its damage, in that tick, is not caught: the absorb has not happened yet.</li>
 *   <li><b>{@code /kill} and the void ignore it</b>: they are passed to vanilla before the point.</li>
 * </ul>
 */
public final class MobShielder implements ShieldSink {

    private final Supplier<MobRegistry> mobs;
    private final Keys keys;
    private final Logger log;
    /** The tick each mob's shield absorbed, for its riders; a window from an earlier tick is dropped when next seen. */
    private final Map<UUID, Long> absorbedAt = new ConcurrentHashMap<>();

    public MobShielder(Supplier<MobRegistry> mobs, Keys keys, Logger log) {
        this.mobs = mobs;
        this.keys = keys;
        this.log = log;
    }

    @Override
    public boolean up(Entity entity) {
        return entity.getPersistentDataContainer().has(keys.mobShield);
    }

    private String mobIdOf(Entity e) {
        String id = e.getPersistentDataContainer().get(keys.mobId, PersistentDataType.STRING);
        return id == null ? e.getType().getKey().getKey() : id;
    }

    @Override
    public boolean absorb(LivingEntity entity, double amount, String by) {
        if (!up(entity)) return false;
        long tick = Bukkit.getCurrentTick();
        if (!MobShield.spends(amount)) {
            log.info(String.format(Locale.ROOT, "SHIELD unspent mob=%s id=%s tick=%d amount=%.3f by=%s",
                    mobIdOf(entity), entity.getUniqueId(), tick, amount, by));
            return false;
        }
        entity.getPersistentDataContainer().remove(keys.mobShield);
        absorbedAt.values().removeIf(t -> t != tick);   // a window from an earlier tick is closed; nothing leaks
        absorbedAt.put(entity.getUniqueId(), tick);
        MobDefinition def = mobs.get().find(mobIdOf(entity)).orElse(null);
        MobShield shield = def == null ? null : def.shield();
        if (entity.getEquipment() != null) entity.getEquipment().setItemInOffHand(new ItemStack(org.bukkit.Material.AIR));
        if (shield != null) {
            for (BeamSound s : shield.sounds()) {
                entity.getWorld().playSound(entity.getLocation(), s.key(), SoundCategory.HOSTILE, (float) s.volume(),
                        (float) s.pitch());
            }
            entity.getWorld().spawnParticle(Particle.valueOf(shield.particle().toUpperCase(Locale.ROOT)),
                    entity.getLocation().add(0, entity.getHeight() / 2, 0), shield.particleCount(), 0.3, 0.4, 0.3, 0.05);
        }
        log.info(String.format(Locale.ROOT, "SHIELD absorbed mob=%s id=%s tick=%d amount=%.3f by=%s",
                mobIdOf(entity), entity.getUniqueId(), tick, amount, by));
        return true;
    }

    @Override
    public boolean suppressRider(LivingEntity entity, String what) {
        Long at = absorbedAt.get(entity.getUniqueId());
        if (at == null) return false;
        long tick = Bukkit.getCurrentTick();
        if (at != tick) {
            absorbedAt.remove(entity.getUniqueId(), at);
            return false;
        }
        log.info("SHIELD rider suppressed mob=" + mobIdOf(entity) + " id=" + entity.getUniqueId() + " tick=" + tick
                + " what=" + what);
        return true;
    }

    /** The instrument's read (PROBE_SHIELD): is the byte up, and what is in the off hand (its look). Pure. */
    public static String probeLine(String tag, UUID mob, boolean up, String offhand) {
        return "SHIELD tag=" + tag + " " + mob + " up=" + up + " offhand=" + (offhand == null ? "none" : offhand);
    }
}
