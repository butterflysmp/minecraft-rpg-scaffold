package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import io.github.butterflysmp.rpg.paper.weapon.Quivers;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends each player their own action-bar stats line, on their own thread, often enough that it never
 * fades.
 *
 * Display only, with ONE exception that is named because the sentence used to be unqualified:
 * nothing here regenerates, spends, or clamps anything -- mana regenerates lazily inside
 * {@link ResourcePool} on read, and health is owned by the health system. This class reads three
 * stores and draws. <b>It also SETTLES a matured quiver reload</b>, and {@link #heldMagazine} carries
 * the argument for why a display surface is allowed to.
 *
 * <h2>Why per-player and not one global loop</h2>
 * The loop runs on a {@link EntityTaskTarget}, so each player's bar is built and sent on the thread
 * that owns THAT player -- which is what makes it correct under Folia. Iterating
 * {@code getOnlinePlayers()} from one global task and calling {@code sendActionBar} across regions is
 * exactly the pattern this avoids.
 *
 * <h2>Lifecycle</h2>
 * {@link EntityTaskTarget#isActive()} is {@code entity.isValid() && !entity.isDead()}, so the loop
 * SELF-CANCELS on the death screen and must be restarted on respawn -- see {@link #onRespawn}. That
 * is the trap this class is most likely to regress on, and it is why {@code RpgListeners} calls into
 * here from three places rather than one.
 */
public final class StatsBarSystem {

    /**
     * How often the bar is redrawn. The action bar fades after roughly three seconds, so half a
     * second keeps it solid with a wide margin while costing two store reads and one packet per
     * player per half second. Tunable: raise it and the bar starts to flicker as it fades between
     * sends; lower it and you pay for frames nobody can perceive.
     */
    private static final int BAR_PERIOD_TICKS = 10;

    private final Scheduler scheduler;
    private final CombatantStats stats;
    private final ResourcePool resources;
    /** Carries both halves of the magazine read: {@code keys()} for the item, {@code weapons()} for the definition. */
    private final AdapterContext adapters;

    /**
     * The live loop per player, kept so a second start cannot stack a second bar on one player.
     * The health and nameplate loops discard their handles and re-start unconditionally, which is
     * safe only because their targets have always already self-cancelled; two live loops here would
     * mean two sends racing on one action bar with no way to stop either.
     */
    private final Map<UUID, TaskHandle> tasks = new ConcurrentHashMap<>();

    public StatsBarSystem(Scheduler scheduler, CombatantStats stats, ResourcePool resources,
                          AdapterContext adapters) {
        this.scheduler = scheduler;
        this.stats = stats;
        this.resources = resources;
        this.adapters = adapters;
    }

    /** Start this player's bar on join. */
    public void onJoin(Player player) {
        start(player);
    }

    /**
     * Restart the bar after a death. Load-bearing: the loop self-cancelled while the player was dead
     * (the target reports inactive), so without this a respawned player's bar never returns for the
     * rest of the session. Mirrors {@link #onJoin}.
     */
    public void onRespawn(Player player) {
        start(player);
    }

    /** Drop the handle on logout so nothing leaks across sessions. */
    public void onQuit(UUID playerId) {
        TaskHandle task = tasks.remove(playerId);
        if (task != null) task.cancel();
    }

    private void start(Player player) {
        UUID id = player.getUniqueId();
        TaskHandle existing = tasks.get(id);
        if (existing != null && existing.isRunning()) return;

        EntityTaskTarget target = new EntityTaskTarget(player, scheduler);
        TaskHandle task = RepeatingTask.start(target, BAR_PERIOD_TICKS, () -> {
            // Not yet bootstrapped: skip this frame rather than inventing numbers. CombatantStats
            // .current/.max THROW for an untracked id -- they do not return 0 -- so an unguarded read
            // would throw every period rather than merely display something wrong. Health registers
            // synchronously in PlayerHealthSystem.onJoin, so this is the edge, not the norm.
            if (!stats.tracks(id)) return true;

            // THE MAGAZINE IS READ FRESH EVERY FRAME, because the held item can change between any
            // two of them and there is nowhere to cache it that a hotbar scroll would invalidate.
            // Absent for every player not holding a quiver weapon, which is almost all of them.
            Optional<QuiverState> quiver = heldMagazine(player);

            player.sendActionBar(StatsBarText.of(
                    stats.current(id), stats.max(id),
                    quiver.map(QuiverState::roundsRemaining).orElse(0),
                    quiver.map(QuiverState::capacity).orElse(0),
                    stats.defenseValue(id),
                    resources.current(id, ResourceCost.DEFAULT_RESOURCE),
                    resources.max(id, ResourceCost.DEFAULT_RESOURCE)));
            return true; // never done: runs until the player quits or dies
        }, () -> tasks.remove(id));

        // start() only SCHEDULES the first tick, so the task cannot have stopped -- and its onStop
        // cannot have run this remove -- before we store the handle. Same ordering as ImmobilizeStatus.
        tasks.put(id, task);
    }

    /**
     * The held weapon's magazine, or empty when the main hand holds no weapon with one.
     *
     * <h2>*** IT SETTLES A MATURED RELOAD, AND THIS IS A DISPLAY SURFACE. THE QUESTION WAS PUT AND
     * THIS IS THE ANSWER ***</h2>
     *
     * <p><b>Without the settle, the bar tells a lie with a known trigger.</b> Start a reload, switch
     * to another hotbar slot, let it mature while unheld, switch back. Nothing has settled it:
     * {@code QuiverReloadCue}'s task found a different weapon in hand and correctly declined,
     * {@code QuiverSweep.reassert} is display only, and no shot has been taken. The item still carries
     * its pending rounds, so {@code roundsRemaining()} is the PRE-reload count and the bar reads
     * <b>{@code ➹ 0/25}</b> until the player does something. With the settle it reads {@code 25/25}
     * within one frame of coming back.
     *
     * <p><b>AND A LYING HUD IS WORSE THAN A LYING DECISION, WHICH IS NOT THE ORDER YOU WOULD
     * GUESS.</b> This branch's own defect -- {@code PlumeDraw.cap} refusing a release on a stale
     * magazine -- at least presented as a weapon that would not fire, and a player pressing the button
     * again eventually settled it. <b>A bar reading {@code 0/25} is read and believed, and the player
     * does not press the button at all.</b> The failure is silent on both sides.
     *
     * <h2>THE LINE BETWEEN A SETTLE AND A SIDE EFFECT OF RENDERING, BECAUSE {@code QuiverSweep} SITS
     * ON THE OTHER SIDE OF IT AND MUST STAY THERE</h2>
     *
     * <p>{@code Quivers}' rule is <i>every site that DECIDES from the magazine settles first</i>, and
     * {@code QuiverSweep}'s javadoc forbids settling there in terms -- <i>"never resolveForShot, which
     * would commit a matured reload as a side effect of drawing a progress bar."</i> This class draws
     * too. The two are not in conflict, and the criterion that separates them is worth stating because
     * "display only" is not it:
     *
     * <pre>
     * SETTLE where settling CHANGES what you are about to show or decide.
     * DO NOT where it cannot.
     * </pre>
     *
     * <p><b>{@code QuiverSweep} draws from {@code reloadTicksRemaining}, which is 0 for a matured
     * reload whether or not it has been settled.</b> Settling there would be a write that changes
     * nothing drawn -- a pure side effect, which is exactly what its javadoc refuses. <b>This bar
     * draws the COUNT, and the count is 0 before the settle and 25 after.</b> Here the settle is not a
     * side effect of rendering; it is the thing that makes the render true.
     *
     * <p>So the rule widens from <i>deciders</i> to <i>deciders and honest readouts</i>, and
     * {@code QuiverSweep} is unaffected by the widening rather than grandfathered out of it.
     * Registered with its role in
     * {@code QuiversSignatureTest.everyExternalReaderOfTheMagazineDeclaresWhetherItSettles}.
     *
     * <h2>THE COST IS ONE WRITE PER RELOAD, NOT ONE PER FRAME</h2>
     *
     * <p>{@code settleMaturedReload} is idempotent: it clears the three reload keys, so the next
     * frame's {@code fireVerdict} is {@code FIRE} or {@code EMPTY} and nothing is written. At 10-tick
     * period that is one item write per reload and a pair of PDC reads per frame otherwise.
     *
     * <p><b>Thread: the loop body runs on an {@link EntityTaskTarget}</b>, which is the player's own
     * scheduler -- the thread {@code Quivers} requires. That is a property this class already had and
     * is the reason the settle can live here at all.
     *
     * <h2>*** THE try/catch IS FORWARD COVER, AND ITS BLAST RADIUS IS WHY IT EARNS ITS KEEP HERE WHEN
     * THE SAME CASE EARNS NOTHING ELSEWHERE ***</h2>
     *
     * <p>{@code QuiverState.from} REFUSES a half-written reload pair by throwing, and that case is
     * unreachable by construction -- all three keys are written in one {@code editMeta} and removed in
     * one, and {@code QuiverItems} copies the pair together at a re-mint. <b>Normally that would make a
     * guard here furniture.</b>
     *
     * <p><b>What is different is what a throw COSTS in this particular loop.</b> {@code
     * RepeatingTask.step} calls the body with no exception isolation, so a throw escapes before
     * {@code arm()} and the loop never re-arms -- <b>and {@code stop()} never runs either, so
     * {@code onStop} does not remove the handle, {@code isRunning()} stays true, and {@link #start}'s
     * own re-entry guard then REFUSES to restart it.</b> One throw permanently kills that player's
     * stats bar for the session, and not even a respawn brings it back.
     *
     * <p><b>So the guard is scoped to the magazine read alone</b>, and a failure costs the quiver
     * field for that frame while health, defense and mana still draw. {@code warnOnce}, so it is
     * visible exactly once if it ever happens rather than every 10 ticks forever.
     *
     * <p><b>THE PRECEDENT IS THREE LINES UP, AND IT IS THE SAME ARGUMENT.</b> {@code stats.tracks(id)}
     * guards reads that <i>"THROW for an untracked id -- they do not return 0 -- so an unguarded read
     * would throw every period"</i>. This loop already treats "a read in here can throw" as a hazard
     * worth a branch.
     *
     * <p><b>THE REAL DEFECT IS IN {@code RepeatingTask} AND IS NOT FIXED HERE.</b> Every repeating
     * loop in the project -- health, nameplates, this -- has the same unguarded body and the same
     * no-restart consequence. Named rather than swept: fixing it touches every user of that class,
     * which is not this slice's subject, and a change that wide is how a revision regresses what it
     * was not revising.
     */
    private Optional<QuiverState> heldMagazine(Player player) {
        Optional<WeaponDefinition> weapon = WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(adapters.weapons()::find)
                .filter(WeaponDefinition::hasQuiver);
        if (weapon.isEmpty()) return Optional.empty();
        try {
            // SETTLE FIRST, THEN READ THE HAND AGAIN -- the settle rewrites it, so a stack read before
            // the call would be the pre-settle copy. Same shape, and the same reason, as PlumeDraw.cap.
            Quivers.settleMaturedReload(player, weapon.get(), adapters);
            return Optional.of(Quivers.stateOf(
                    player.getInventory().getItemInMainHand(), adapters.keys(), weapon.get()));
        } catch (RuntimeException failed) {
            adapters.warnOnce("the stats bar could not read the magazine on weapon '"
                    + weapon.get().id() + "': " + failed
                    + " -- the quiver field is omitted. A half-written reload pair is the only known"
                    + " cause and is supposed to be unrepresentable; see QuiverState.from.");
            return Optional.empty();
        }
    }
}
