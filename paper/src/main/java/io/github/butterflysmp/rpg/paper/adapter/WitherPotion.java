package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Forgetting a wither strips the vanilla WITHER potion we put on for its look (the seat's ruling 6,
 * 2026-10-01).
 *
 * <p><b>The hazard.</b> Our wither's look is the vanilla potion ({@code BukkitCombatant.witherLook}), and
 * its own 1.0 damage is tokened only while {@code wither().isActive}. Forget the store and leave the
 * potion, and the potion's remaining ticks reroute into custom HP, uncapped and credited to the victim.
 * Vanilla SAVES effects, so a player who quits mid-wither rejoins with the potion and no store.
 *
 * <p><b>"If we applied it" is read as "if our store was running".</b> Every store application puts the
 * potion on with the same window, so a live store means a potion of ours. If the store had already
 * ended, so had the potion it applied. The cost, named: a vanilla wither (a real wither skeleton's hit)
 * that merged into ours is stripped with it, because the entity holds one WITHER instance. That is the
 * lesser harm; it is live only with PvP.
 *
 * <p><b>The quit ordering is read from the jar:</b> {@code PlayerList.remove} raises
 * {@code PlayerQuitEvent} (offset 57) before {@code save(ServerPlayer)} (offset 169), so the strip is
 * saved. <b>The mob-removal ordering is NOT read</b>: whether a strip during
 * {@code EntityRemoveFromWorldEvent} on a chunk unload reaches the saved entity is UNVERIFIED.
 */
public final class WitherPotion {

    private WitherPotion() {}

    /** Production's: forget {@code entity}'s wither, and strip the potion if the wither was live. */
    public static void forgetAndStrip(DotStatus wither, LivingEntity entity) {
        forgetAndStrip(wither, entity.getUniqueId(),
                () -> entity.removePotionEffect(PotionEffectType.WITHER));
    }

    /**
     * The decision, server-free: forget, and run {@code stripPotion} only when a live wither was dropped.
     *
     * @return whether the potion was stripped
     */
    static boolean forgetAndStrip(DotStatus wither, UUID id, Runnable stripPotion) {
        if (!wither.forget(id)) return false;
        stripPotion.run();
        return true;
    }
}
