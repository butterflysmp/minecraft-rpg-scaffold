package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
 * KILL CREDIT is split, and the split is narrower than it first looked. setKiller sets the mob's
 * lastHurtByPlayer, and vanilla reads that for DROPS, XP ORBS and -- MEASURED, see below -- ADVANCEMENT
 * CRITERIA. All three always worked. Only the kill STATISTICS come from elsewhere: vanilla awards those
 * from the killer it resolves off the DamageSource, and setHealth(0) carries no player-attributed
 * source, so the stats page showed zero kills for a player who had plainly killed things. They are
 * awarded explicitly below, and they are the ONLY thing this class awards by hand.
 *
 * > 2026-08-28: the statistics fix originally recorded here that the Monster Hunter advancement was
 * > "still uncredited" for the same reason. That was an INFERENCE from one measured fact -- the
 * > statistics being zero -- reasoning that vanilla awards both from the same resolved killer. It is
 * > WRONG, and the boot said so: revoke minecraft:adventure/kill_a_mob, kill a zombie, and the toast
 * > appears. The advancement criterion fires off lastHurtByPlayer, which setKiller had been setting all
 * > along. Two things vanilla awards near each other are not thereby awarded from the same place, and
 * > the shape of the mistake is worth keeping: a single measurement was generalised into a second
 * > claim that was never itself measured.
 *
 * A fuller change remains AVAILABLE, but it is no longer a fix for anything known to be broken: killing
 * through a player-attributed non-ENTITY_ATTACK DamageSource would let vanilla credit the statistics
 * naturally instead of by hand, and would cover anything future that keys on the DamageSource rather
 * than on lastHurtByPlayer. Wanted for tidiness and for unknown-unknowns, not to repair a symptom. It
 * is a real behaviour change to the death path -- it would re-enter the damage pipeline, which the melee
 * rider tokens -- so it stays its own decision. If it is ever taken, the explicit increment below must
 * be removed in the same change, or every kill counts twice.
 *
 * Scope: MOB death only. A player reaching 0 is skipped ({@code targetIsPlayer}) and still sits at the
 * half-heart floor, alive -- the respawn lifecycle is its own follow-up pass.
 */
public final class MobDeathSystem implements HealthListener {

    private final Scheduler scheduler;

    public MobDeathSystem(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void onChange(HealthChange change) {
        if (!shouldKill(change)) return;

        Entity target = Bukkit.getEntity(change.target());
        // isDead guard: belt-and-suspenders against a second delivery (reachedZero already fires once).
        if (!(target instanceof LivingEntity mob) || mob.isDead()) return;

        // Kill credit: an online player dealer gets the drops and XP orbs. Resolved exactly as
        // DamagePopupManager does -- a mob or offline dealer simply leaves the kill uncredited.
        Player killer = change.dealerIsPlayer() ? Bukkit.getPlayer(change.dealer()) : null;
        // The mob's type, read HERE on the thread that owns it. The statistic below is awarded after
        // a hop, by which point the mob is dead and reading anything off it would be a use-after-free
        // dressed as a getter.
        EntityType killedType = mob.getType();
        if (killer != null) mob.setKiller(killer);

        // REAL vanilla death via setHealth(0), NOT damage(): our own onPlayerMeleeAttack rider tokens
        // every player->mob EntityDamageByEntityEvent to 0.01, so a "lethal" damage kill gets neutered
        // and the mob survives. setHealth(0) fires EntityDeathEvent without a damage event, bypassing the
        // rider entirely. Cleanup is free: death -> EntityRemoveFromWorldEvent -> onMobRemove clears the
        // nameplate AND the custom HP store.
        mob.setHealth(0);

        // THE KILL STATISTIC, which setHealth(0) does not award.
        //
        // setKiller sets the mob's lastHurtByPlayer, and that is what vanilla consults for drops, XP
        // orbs and advancement criteria -- which is why all three already worked (the advancement half
        // measured, not assumed; see the class javadoc). The statistics are the ONE thing that does not
        // come from there: vanilla awards them from the killer resolved off the DamageSource, and
        // setHealth(0) carries no player-attributed source at all. So the stats page showed a zero kill
        // count for a player who had visibly killed things, which is the boot observation this fixes --
        // and it is the only gap, not one symptom of a broader one.
        //
        // Both, because they are separate counters and the stats page shows both: MOB_KILLS is the
        // total line, KILL_ENTITY the per-type breakdown. Awarding one leaves the other wrong.
        //
        // No double-count: the increment is the SOLE credit, precisely because the setHealth(0) path
        // awards nothing (measured -- the counter sat at zero). If this ever becomes a real
        // player-attributed kill (see the DamageSource note in the class javadoc), this block must go
        // in the same change, or every kill counts twice.
        if (killer != null) {
            // Hopped onto the KILLER's thread. This is a write to player state, and onChange runs on
            // the MOB's thread -- fine on Paper, a cross-region write on Folia the moment a kill comes
            // from something that is not a melee swing (a lingering Area, a projectile whose shooter
            // has walked away). The mob's type was captured above, before it died.
            scheduler.onEntity(killer, () -> {
                killer.incrementStatistic(Statistic.MOB_KILLS);
                killer.incrementStatistic(Statistic.KILL_ENTITY, killedType);
            });
        }
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
