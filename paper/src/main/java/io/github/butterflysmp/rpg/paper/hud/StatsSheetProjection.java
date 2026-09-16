package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.core.combat.QuiverSize;
import io.github.butterflysmp.rpg.core.combat.ReloadTime;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.StatsSheetValues;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * The ONE place a player's stat sheet is read off the live world.
 *
 * <p>Every number on it -- the eight build stats and the conditional quiver pair -- is obtained
 * here and nowhere else. {@link StatsSheet} renders it for chat, {@code NexusStatsLore} renders it
 * for a tooltip, and <b>neither reads a stat itself.</b>
 *
 * <h2>WHY THIS IS A CLASS RATHER THAN A PRIVATE METHOD ON {@code RpgCommand}</h2>
 *
 * <b>It was one, until the Nexus hub needed the same figures.</b> The alternative was for the hub
 * to build its own {@code StatsSheetValues} -- and the argument against that was already written,
 * by the author of {@code /rpg stats}, about two of these ten fields:
 *
 * <blockquote>
 * "Both numbers go through the SAME resolvers the write paths use -- {@code QuiverSize.resolve} and
 * {@code ReloadTime.resolve} -- rather than being re-derived as authored+bonus here. <b>That is the
 * difference between a readout and a second source of truth</b>, and it is why this file is on the
 * capacity guard's list rather than excluded from it."
 * </blockquote>
 *
 * <p>Two surfaces re-deriving these values is that defect on all ten, one screen over.
 *
 * <h2>THE DAMAGE LINE IS A REAL SWING, NOT A LOOKALIKE</h2>
 *
 * <b>It is the whole reason {@code /rpg stats} needed a refactor to exist.</b> It is
 * {@code HitDamage.hitBase(...)} -- the SAME function both {@code EffectApplier} damage arms call
 * -- fed the same three accessors {@code BukkitCombatant.snapshot} feeds them. Not a re-derivation
 * that resembles a swing: {@code HitDamage.dealt(hitBase, 1.0, 1.0) == hitBase} exactly, so this IS
 * a full-charge non-crit hit.
 *
 * <h2>SHARING A FORMULA IS NOT SHARING ITS INPUTS -- AND THIS IS THE HALF THAT WAS MISSING</h2>
 *
 * {@code HitDamage} already guaranteed the sheet and the combat path COMPUTE the same way.
 * <b>Nothing guaranteed two sheets were FED the same way</b>, and a drift there reddens nothing:
 * both callers would still be correct in isolation and the formula would still be shared. That is
 * the stats arc's recorded residual risk, and one input path is what closes it.
 *
 * <p><b>What it does NOT close:</b> combat reads its three damage summands off a snapshot frozen at
 * cast time and this reads them live. They agree because {@code BukkitCombatant.snapshot} is a
 * straight read with no transform at either hop. That seam is unchanged by this class, and
 * {@code GATE-nexus.md}'s swing-and-compare row is still its only check.
 *
 * <h2>NO UNIT TEST, AND IT IS NOT AN OVERSIGHT</h2>
 *
 * Every parameter is a live server object -- there is no MockBukkit here and {@code Player} cannot
 * be constructed -- so this class is boot-gate-only, the same constraint {@code NexusItems} and
 * {@code QuiverItems} record. <b>What IS unit-tested is everything downstream of it:</b>
 * {@code StatsSheetTest} over the chat rendering and {@code NexusStatsLoreTest} over the tooltip,
 * both fed a hand-built {@code StatsSheetValues}.
 */
public final class StatsSheetProjection {

    private StatsSheetProjection() {}

    /**
     * This player's stat sheet, or empty if the stat engine is not tracking them yet.
     *
     * <p><b>EMPTY IS A REAL STATE AND BOTH CALLERS MUST RENDER IT AS ONE.</b> A freshly-joined
     * player reaches it. It is {@code Optional} rather than a zeroed sheet precisely so that
     * neither surface can accidentally print zeroes: a readout showing {@code 0} when nothing was
     * counted is indistinguishable from a working readout that measured zero, which is
     * {@code MenuIcons.placeholder}'s own argument. Both callers say
     * {@link io.github.butterflysmp.rpg.core.combat.StatsSheetLines#UNTRACKED} instead, and they say
     * it in the same words because they read it from the same constant.
     *
     * <p>Guarded with {@code tracks} and NOT with the register-if-absent path {@code damageSelf}
     * and {@code healSelf} take -- that is a WRITE, and a readout must not have one.
     *
     * <p>Charge and crit are deliberately absent rather than sampled: a sheet showing a rolled crit
     * would print a different number every time it was read, and {@code snapshot} draws from
     * {@code ThreadLocalRandom}.
     *
     * <p><b>SELF-ONLY, and that is a threading decision rather than a scope one.</b> Reading YOUR
     * OWN stats runs on your own region thread, the same thread your reconcile loop runs on, so
     * nothing can mutate underneath. Reading another player's would iterate maps their loop mutates
     * four times a second. <b>Do not add an overload taking someone else's {@code Player}</b>
     * without a region hop first.
     */
    public static Optional<StatsSheetValues> of(Player player, AdapterContext adapters,
                                                WeaponRegistry weapons,
                                                ResourcePool resources) {
        UUID id = player.getUniqueId();
        CombatantStats stats = adapters.stats();
        if (!stats.tracks(id)) {
            return Optional.empty();
        }

        // The same three the snapshot projection reads, in the same units, so the Damage line below
        // composes exactly what a swing composes.
        double damage = HitDamage.hitBase(stats.attackValue(id),
                stats.enchantDamagePercentValue(id), stats.classDamageValue(id));

        // THE QUIVER PAIR IS CONDITIONAL, AND THE CONDITION IS WHAT IS IN YOUR HAND. Every other
        // line is a fact about the player; a capacity is a fact about a weapon. Absent rather than
        // zero, because "Quiver 0" reads as a broken magazine rather than as "you are holding a
        // sword".
        //
        // Both numbers go through the SAME resolvers the write paths use -- QuiverSize.resolve and
        // ReloadTime.resolve -- rather than being re-derived as authored+bonus here. That is the
        // difference between a readout and a second source of truth, and it is why this file is on
        // the capacity guard's list rather than excluded from it.
        StatsSheetValues.Builder values = StatsSheetValues.builder()
                .maxHealth(stats.max(id))
                .healthRegenPerSecond(stats.healthRegenValue(id))     // stored per second
                .maxMana(resources.max(id, ResourceCost.DEFAULT_RESOURCE))
                .manaRegenPerSecond(ManaRegen.perSecond(              // per TICK out of the pool
                        resources.regen(id, ResourceCost.DEFAULT_RESOURCE)))
                .defense(stats.defenseValue(id))
                .damage(damage)
                .critChance(stats.critChanceValue(id))
                .critDamageBonus(stats.critDamageValue(id));

        WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(weapons::find)
                .filter(WeaponDefinition::hasQuiver)
                .ifPresent(weapon -> values.quiver(
                        QuiverSize.resolve(weapon.quiverSize(), stats.quiverSizeBonusValue(id)),
                        ReloadTime.resolve(weapon.reloadTicks(), stats.reloadTimeBonusValue(id))));

        return Optional.of(values.build());
    }
}
