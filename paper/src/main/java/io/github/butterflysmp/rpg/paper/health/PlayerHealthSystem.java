package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.weapon.AttackSpeedModifierItems;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.weapon.ClassDamageModifierItems;
import io.github.butterflysmp.rpg.paper.weapon.DamageEnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponAttackItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Paper side of player custom health: the display listener, the per-player equip/unequip
 * reconcile loop, and the join/quit lifecycle. Mobs are not handled here -- their nameplate is the
 * next phase; this listener ignores non-player changes.
 *
 * It is the {@link HealthListener} the store emits to, so it renders the heart bar whenever custom
 * health moves. Construction is two-step to break the cycle (the store needs a listener, this needs
 * the store): build this, build the store with it, then {@link #bind}.
 */
public final class PlayerHealthSystem implements HealthListener {

    /**
     * How often the reconcile loop rescans a player's equipment. Equipment changes are rare, so a
     * few times a second is ample and cheap; the cost is a handful of slot reads per player per tick
     * of the period, and at most one tick of latency on a stat change (imperceptible).
     */
    private static final int RECONCILE_PERIOD_TICKS = 5;

    private final Scheduler scheduler;
    private final Keys keys;
    private final WeaponRegistry weapons;
    private final EnchantRegistry enchants;
    private final HeartBarRenderer renderer = new HeartBarRenderer();
    private CombatantStats stats;
    private ResourcePool resources;

    public PlayerHealthSystem(Scheduler scheduler, Keys keys, WeaponRegistry weapons,
                              EnchantRegistry enchants) {
        this.scheduler = scheduler;
        this.keys = keys;
        this.weapons = weapons;
        this.enchants = enchants;
    }

    /** Wire the store this renders. Called once in onEnable, right after the store is built. */
    public void bind(CombatantStats stats) {
        this.stats = stats;
    }

    /**
     * Wire the mana pool the reconcile loop pins on a max-mana change.
     *
     * <p>A second bind rather than a constructor parameter, because the pool is built AFTER this
     * system and cannot be built before it: the pool's own max resolver reads the stat store, and
     * the stat store's listener is this system. That is the same cycle {@link #bind} already breaks,
     * one step further along.
     */
    public void bindResources(ResourcePool resources) {
        this.resources = resources;
    }

    /** The store this owns, for the dev commands that damage/heal through the observable path. */
    public CombatantStats stats() {
        return stats;
    }

    /**
     * A health change: refresh the player's heart bar, OR kill the player if this change zeroed their
     * custom HP. Only players are handled here; a mob change (the nameplate/mob-death phase) is ignored.
     * Resolves the player and hops onto its own thread before touching Bukkit -- the change may have
     * been emitted from any thread.
     *
     * The kill lives here, not in a separate death listener, so it and the floored render never race on
     * the same reachedZero change: on reachedZero we kill INSTEAD OF rendering. setHealth(0) fires a
     * normal PlayerDeathEvent (keep-inventory forced in RpgListeners); the display floor stays correct
     * for every non-lethal render. onQuit does not run on death, so custom HP sits at 0 until onRespawn.
     */
    @Override
    public void onChange(HealthChange change) {
        if (!change.targetIsPlayer()) return;
        Player player = Bukkit.getPlayer(change.target());
        if (player == null) return;
        if (change.reachedZero()) {
            scheduler.onEntity(player, () -> player.setHealth(0));   // real death; no floor render competes
            return;
        }
        scheduler.onEntity(player, () ->
                renderer.render(new EntityHeartBar(player), change.newCurrent(), change.max()));
    }

    /**
     * Register the player at base 100 full, render once (resetting a vanilla bar left stale from a
     * previous session), and start the reconcile loop that tracks their equipped +HP items.
     */
    public void onJoin(Player player) {
        UUID id = player.getUniqueId();
        stats.register(id, CombatantStats.DEFAULT_PLAYER_BASE, true);
        scheduler.onEntity(player, () ->
                renderer.render(new EntityHeartBar(player), stats.current(id), stats.max(id)));
        startReconcileLoop(player);
    }

    /** Drop the player's health state on logout, so no modifier or entry leaks across sessions. */
    public void onQuit(UUID id) {
        stats.clear(id);
    }

    /**
     * Respawn after a custom-HP death: reset to full base and RESTART the reconcile loop. Mirrors
     * {@link #onJoin}. The reset is owned here because onQuit does not run on death -- custom HP sat at 0
     * through the death screen. The loop restart is the load-bearing part: it self-cancelled on the death
     * screen (EntityTaskTarget is inactive while dead), so without this a respawned player would never
     * track gear +HP again. Equipment headroom re-applies on the loop's next tick (the bar may show full
     * base for a tick, then dip as gear reconciles). Profile is NOT reloaded -- it persists across death.
     */
    public void onRespawn(Player player) {
        UUID id = player.getUniqueId();
        stats.register(id, CombatantStats.DEFAULT_PLAYER_BASE, true);
        scheduler.onEntity(player, () ->
                renderer.render(new EntityHeartBar(player), stats.current(id), stats.max(id)));
        startReconcileLoop(player);
    }

    /**
     * A per-player loop that rescans equipment and converges the store's max modifiers to it. Same
     * shape as the Soaked countdown -- {@link RepeatingTask} on an {@link EntityTaskTarget} -- so it
     * self-cancels when the player leaves and never touches a removed entity. The body never returns
     * false: it runs until the player is gone, at which point the target reports inactive and the
     * task stops. Cleanup of the store is the quit handler's job, not the loop's.
     */
    private void startReconcileLoop(Player player) {
        EntityTaskTarget target = new EntityTaskTarget(player, scheduler);
        UUID id = player.getUniqueId();
        RepeatingTask.start(target, RECONCILE_PERIOD_TICKS, () -> {
            // Ten stats converge on the same scan: max HP from +HP items, attack damage from the
            // held weapon's declared attack_damage (a MAIN_HAND modifier), attack speed from equipped
            // speed sources, the class-damage bonus from equipped "+N <Class> Damage" gear
            // MATCHING the held weapon's class, and the enchant-damage percent from the damage
            // enchants ON the held weapon matching THAT weapon's class. Same leak-proof diff for all
            // five, so a weapon swap/drop follows within a tick and respawn re-derives every one of
            // them for free.
            //
            // The class one is why a weapon swap needs no event of its own: the held weapon's class
            // is re-read every scan, so the same worn gear simply selects a different grant, and a
            // grant that stops matching is absent from the desired set rather than zeroed.
            // TWO SOURCES, ONE RECONCILE CALL, and that is not a tidiness preference.
            // ModifierReconciler.reconcile removes every applied source absent from the map it is
            // handed, so reconciling the fixture scan and the Growth scan separately would have
            // each wipe the other's sources -- the stat would hold whichever ran last, silently and
            // forever. Merged first, reconciled once.
            //
            // The Growth keys are namespaced ("growth:CHEST") because HealthModifierItems walks ALL
            // slots on bare slot names, so a fixture item and a Growth piece in the same slot would
            // otherwise collide on one key and Stat.putModifier would keep only one of them.
            Map<String, Double> desiredMax = new HashMap<>(HealthModifierItems.desiredModifiers(player, keys));
            desiredMax.putAll(GrowthModifierItems.desiredModifiers(player, keys, enchants));
            stats.reconcileMaxModifiers(id, desiredMax);
            Map<String, Double> desiredAttack = WeaponAttackItems.desiredAttackModifiers(player, keys, weapons);
            stats.reconcileAttackModifiers(id, desiredAttack);
            Map<String, Double> desiredSpeed = AttackSpeedModifierItems.desiredModifiers(player, keys);
            stats.reconcileAttackSpeedModifiers(id, desiredSpeed);
            // And REFLECT that stat onto vanilla's own attack-speed attribute, which is what
            // actually paces a basic melee swing now that vanilla's crosshair attack delivers it.
            // Placed immediately after the reconcile above, for the reason the defense pair below
            // states: it must draw the value that just converged, not the previous scan's. The
            // weapon's cadence comes from the held DEFINITION, never from the attribute we write.
            AttackSpeedAttributeOverride.apply(player, keys,
                    WeaponAttackItems.heldMeleeSpeed(player, keys, weapons), stats.attackSpeedValue(id));
            Map<String, Double> desiredClass =
                    ClassDamageModifierItems.desiredModifiers(player, keys, weapons);
            stats.reconcileClassDamageModifiers(id, desiredClass);
            // The fifth reads only the MAIN HAND, unlike the four above: a damage enchant is not
            // worn elsewhere and pointed at your weapon, it is ON the weapon, so the gate compares
            // the enchant's class against the class of the item carrying it.
            Map<String, Double> desiredEnchant =
                    DamageEnchantItems.desiredModifiers(player, keys, weapons, enchants);
            stats.reconcileEnchantDamageModifiers(id, desiredEnchant);

            // The seventh and eighth: crit chance and crit damage, from the two _TEMP fixtures. Same
            // slot scan and same leak-proof diff as the rest, so an item swapped out of a hand is
            // absent from the next scan and its source is dropped. They converge INDEPENDENTLY --
            // one item can raise how often you crit without touching how hard, which is the whole
            // reason crit is two stats rather than one.
            stats.reconcileCritChanceModifiers(id, CritModifierItems.desiredChanceModifiers(player, keys));
            stats.reconcileCritDamageModifiers(id, CritModifierItems.desiredDamageModifiers(player, keys));

            // The sixth is the only one whose source is SHIPPED VANILLA CONTENT rather than a dev
            // fixture or an authored weapon: it reads the armor value off whatever armor the player
            // happens to be wearing. The override runs AFTER the reconcile, so it draws the value
            // that just converged rather than the one from the previous scan.
            //
            // TWO NUMBERS FROM ONE WALK. The stat converges to what each slot CONTRIBUTES; the bar
            // cancels what the vanilla attribute actually HOLDS. They are equal today and will not
            // be once an enchant can add Defense, which is why they are read and passed separately
            // rather than one sum serving both -- see DefenseModifierItems.
            // The NINTH stat, and the only one whose current value lives in another store. Max
            // health's transition happens inside HealthState (headroom up, clamp down); mana's
            // cannot, because ResourcePool holds a spent amount and a tick rather than a value.
            //
            // So the transition is here, in three lines, and it is ONE mechanism for both
            // directions: read what the player has NOW, converge the ceiling, and if it actually
            // moved, pin the reading back. Rising, that is headroom -- crucially including for a
            // player with no pool entry, who would otherwise read the new ceiling instantly and get
            // the difference free. Falling, setCurrent's own clamp is the unequip clamp, stated
            // rather than left to emerge from the regen path's Math.min.
            //
            // Only on a real change, so a player not touching their gear still writes nothing and
            // "a pool nobody reads costs nothing" survives.
            double manaBefore = resources.current(id, ResourceCost.DEFAULT_RESOURCE);
            if (stats.reconcileMaxManaModifiers(id,
                    ManaBankModifierItems.desiredModifiers(player, keys, enchants))) {
                resources.setCurrent(id, ResourceCost.DEFAULT_RESOURCE, manaBefore);
            }

            // The TENTH, and the quietest: the passive regeneration RATE, in HP per second. No
            // event, no override, no pin -- unlike the ninth above it, this stat has no current
            // anywhere. HealthRegenSystem reads the resolved value fresh on every fire, so a piece
            // equipped here simply changes what the next second pays, and one removed changes it
            // back. That is the whole transition.
            stats.reconcileHealthRegenModifiers(id, HealthRegenModifierItems.desiredModifiers(player, keys));

            DefenseModifierItems.Worn worn = DefenseModifierItems.scan(player, keys, enchants);
            stats.reconcileDefenseModifiers(id, worn.defense());
            ArmorBarOverride.apply(player, keys, stats.defenseValue(id), worn.nativeArmor());
            return true;
        }, () -> { });
    }
}
