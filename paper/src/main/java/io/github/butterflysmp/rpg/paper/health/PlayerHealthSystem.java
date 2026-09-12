package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.ManaTransition;
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
     * The kill lives here, not in a separate death listener, so on THIS change we kill INSTEAD OF
     * rendering. setHealth(0) fires a normal PlayerDeathEvent (keep-inventory forced in RpgListeners).
     * onQuit does not run on death, so custom HP sits at 0 until onRespawn.
     *
     * <h2>WHAT THIS GUARANTEES, AND WHAT IT USED TO CLAIM</h2>
     *
     * This branch guarantees that <b>this invocation</b> does not render. <b>It never guaranteed
     * anything about the next one</b>, and the previous wording -- "it and the floored render never
     * race", with the code comment "no floor render competes" -- read as a property of the system.
     *
     * <p>It is not one, because {@code reachedZero} fires only on the TRANSITION to zero. The very
     * next {@code HealthChange} on an already-zero player is not a transition, so it falls through to
     * the render below -- and the render used to floor at half a heart, landing on top of the queued
     * {@code setHealth(0)}. Both hops are next-tick ({@code EntityScheduler.run} is documented as
     * such), so the revival could beat vanilla's death check. Measured 2026-09-06 in lava: death
     * screen up, health 1, respawn button inert, relog the only recovery.
     *
     * <p><b>A DAMAGE TICK AND A PASSIVE HEAL ARE THE SAME EVENT TO THIS METHOD</b> -- both are
     * non-transitioning changes on a zero-HP player -- so there was one defect here, not two, and
     * fixing only the damage path would have left it live behind the regeneration loop.
     *
     * <p>What closes it is {@code HeartBarRenderer} writing ZERO at zero rather than the floor, so a
     * late render agrees with the death instead of undoing it. The floor still applies strictly above
     * zero, which is the case it was always for. This branch's job is unchanged; only its promise has
     * been corrected to the one it can keep.
     */
    @Override
    public void onChange(HealthChange change) {
        if (!change.targetIsPlayer()) return;
        Player player = Bukkit.getPlayer(change.target());
        if (player == null) return;
        if (change.reachedZero()) {
            scheduler.onEntity(player, () -> player.setHealth(0));   // see below: the RENDER is what used to compete
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
            // EVERY STAT THIS LOOP CONVERGES IS RECONCILED BELOW -- the reconcile calls in this body
            // ARE the list, so this sentence cannot disagree with itself. Five of them, to show the
            // shape: max HP from +HP items, attack damage from the held weapon's declared
            // attack_damage (a MAIN_HAND modifier), attack speed from equipped speed sources, the
            // class-damage bonus from equipped "+N <Class> Damage" gear MATCHING the held weapon's
            // class, and the enchant-damage percent from the damage enchants ON the held weapon
            // matching THAT weapon's class. Same leak-proof diff for every one, so a weapon
            // swap/drop follows within a tick and respawn re-derives them all for free.
            //
            // (THE COUNT IS GONE, AND THAT IS THE THIRD ATTEMPT AT THIS SENTENCE. It read "all five"
            // while the set had grown; it was bumped to "Eleven stats ... all eleven" with a note
            // saying it was "phrased now so it cannot go stale again" -- and that phrasing STILL LED
            // WITH A COUNT. The A2 quiver slice adds two stats to this loop, which would have made
            // both words wrong again, so the number came out one commit BEFORE it would have gone
            // stale rather than one after. A count is a claim about a set; the set is the calls in
            // this body; so the calls are what this sentence points at.)
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
            // The NINTH and ELEVENTH stats, converged TOGETHER because they share one current.
            // Max mana is the ceiling that current approaches; mana regen is the slope it approaches
            // at. Both live on HealthState, the current lives in ResourcePool, and a change to
            // either has to stamp it -- once.
            //
            // ManaTransition owns that, in core, rather than here. Not tidiness: written inline as
            // `if (reconcileMax(...) || reconcileManaRegen(...))` the || short-circuits, so on any
            // tick where the ceiling changed the RATE reconcile never runs and regen modifiers
            // silently stop converging. In this loop nothing can observe that. In core it is a unit
            // test. Same for the freeze guard -- pinning on a pass where nothing moved re-stamps
            // asOfTick four times a second, so elapsed never grows and mana stops regenerating
            // entirely, with a stat block that still reads correctly.
            ManaTransition.reconcile(stats, resources, id, ResourceCost.DEFAULT_RESOURCE,
                    ManaBankModifierItems.desiredModifiers(player, keys, enchants),
                    ManaRegenModifierItems.desiredModifiers(player, keys));

            // The TENTH, and the quietest: the passive regeneration RATE, in HP per second. No
            // event, no override, no pin.
            //
            // NOT because "a rate has no current" -- that was this comment's old reason and the
            // eleventh stat above disproves it, being a rate that needs a pin. The reason is that
            // health regen is EAGER: HealthRegenSystem pays rate x dt every second, so nothing is
            // ever accrued-but-unpaid for a rate change to re-price. Mana is LAZY-INTEGRATED
            // (amount + elapsed * rate, evaluated on read), so changing its rate re-prices ticks
            // that already passed. Eager versus lazy is the axis; having a current is not.
            stats.reconcileHealthRegenModifiers(id, HealthRegenModifierItems.desiredModifiers(player, keys));

            // QUIVER SIZE: whole arrows added to the held weapon's authored magazine. SILENT and
            // VOID, like health regen -- but for a different reason, and the difference is worth
            // one line because the usual one does not apply.
            //
            // Mana regen returns boolean because the pool accrues LAZILY, so a rate change re-prices
            // elapsed ticks and the caller must pin. A quiver accrues nothing: its count is a stored
            // integer that changes only when something writes it. And the DECREASE-CLAMP is not here
            // either -- it is at QuiverItems.setLoaded, which holds the new capacity and the count in
            // one call with Quiver.clamp between them. A clamp on this path would be a second
            // enforcement site for one capacity, which is the defect the whole quiver slice was
            // reorganised to make unrepresentable, and it would be an in-play write with no render,
            // which is boot row V1's defect in a new location.
            //
            // So capacity is as of the wielder's last shot or reload, which is the ENDORSED
            // consequence rather than a gap: you pack your quiver, and what you packed is what you
            // carry.
            stats.reconcileQuiverSizeModifiers(id, QuiverSizeModifierItems.desiredModifiers(player, keys));

            // RELOAD TIME: ticks added to the held weapon's authored reload. Silent and void, same
            // as the line above, and for one extra reason of its own -- a running reload's deadline
            // was STAMPED when it began and is never recomputed, so a change here cannot re-price a
            // timer already in flight. A1 made that structural by removing the duration from
            // Quiver.reloadComplete's parameters entirely.
            //
            // SEPARATE FROM THE LINE ABOVE, NOT MERGED INTO IT. Two scanners against two targets is
            // what buys the ability to watch one stat hold still while the other moves; merging them
            // would delete the only staging in which that observation exists.
            stats.reconcileReloadTimeModifiers(id, ReloadTimeModifierItems.desiredModifiers(player, keys));

            DefenseModifierItems.Worn worn = DefenseModifierItems.scan(player, keys, enchants);
            stats.reconcileDefenseModifiers(id, worn.defense());
            ArmorBarOverride.apply(player, keys, stats.defenseValue(id), worn.nativeArmor());
            return true;
        }, () -> { });
    }
}
