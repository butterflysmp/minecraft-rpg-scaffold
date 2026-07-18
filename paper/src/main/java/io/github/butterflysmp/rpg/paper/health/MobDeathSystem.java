package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * The death consumer on the {@link HealthChange} seam: when a MOB's custom HP crosses to 0 it finally
 * dies for real (vanilla death animation, drops, XP, kill credit), instead of sitting alive at the
 * puppet-health floor. This is the first consumer of {@code reachedZero}, the death hook built
 * unconsumed since damage pass 1a. Like the popup it is a pure {@link HealthListener} -- it reads
 * everything off the seam event, so no store binding and no mob-lifecycle hooks.
 *
 * Threading: {@code onChange} runs synchronously on the TARGET entity's owning thread (applyDamage ->
 * scheduler.onEntity(target) -> stats.damage -> onChange), the same contract {@link DamagePopupManager}
 * relies on, so resolving and killing the mob here is legal. The dealer is only a UUID; {@link
 * Bukkit#getPlayer} returns the online Player or null (mirrors the popup's resolution -- no
 * package-private Attribution reach).
 *
 * Scope: MOB death only. A player reaching 0 is skipped ({@code targetIsPlayer}) and still sits at the
 * half-heart floor, alive -- the respawn lifecycle is its own follow-up pass.
 */
public final class MobDeathSystem implements HealthListener {

    @Override
    public void onChange(HealthChange change) {
        if (!shouldKill(change)) return;

        Entity target = Bukkit.getEntity(change.target());
        // isDead guard: belt-and-suspenders against a second delivery (reachedZero already fires once).
        if (!(target instanceof LivingEntity mob) || mob.isDead()) return;

        // Kill credit: an online player dealer gets the drops + XP orbs + advancement. Resolved exactly
        // as DamagePopupManager does -- a mob or offline dealer simply leaves the kill uncredited.
        Player killer = change.dealerIsPlayer() ? Bukkit.getPlayer(change.dealer()) : null;
        if (killer != null) mob.setKiller(killer);

        // REAL vanilla death via setHealth(0), NOT damage(): our own onPlayerMeleeAttack rider tokens
        // every player->mob EntityDamageByEntityEvent to 0.01, so a "lethal" damage kill gets neutered
        // and the mob survives. setHealth(0) fires EntityDeathEvent without a damage event, bypassing the
        // rider entirely. Cleanup is free: death -> EntityRemoveFromWorldEvent -> onMobRemove clears the
        // nameplate AND the custom HP store.
        mob.setHealth(0);
    }

    /**
     * The pure gate: kill only on the DAMAGE hit that zeroed a MOB's custom HP. HEAL / MAX_CHANGE never
     * carry {@code reachedZero}; a player target is skipped (player death is the follow-up pass). Pure and
     * static so it is reddening-testable without Bukkit types.
     */
    static boolean shouldKill(HealthChange change) {
        return change.reachedZero() && !change.targetIsPlayer();
    }
}
