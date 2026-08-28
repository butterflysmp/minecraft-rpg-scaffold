package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The two pieces of timing state a vanilla-driven basic melee hit needs, and nothing else.
 *
 * <p>Both exist because the hit is split across TWO vanilla events. {@code PrePlayerAttackEntityEvent}
 * fires at the top of {@code Player#attack} and is the only place the swing's charge can still be
 * read; {@code EntityDamageByEntityEvent} fires from inside {@code hurt()} and is the only place we
 * know the hit actually landed. This carries what the first knows to the second.
 *
 * <h2>1. The pending swing (per attacker)</h2>
 * One entry per attacker, overwritten each attack. The two events are the same tick, the same
 * thread, and the damage event fires nested inside {@code attack()}, so an entry can only go stale
 * when a swing never landed -- an i-framed re-hit, an unattackable target, a cancelled event. That
 * bounds it at one entry per online player; {@link #forgetAttacker} drops it on quit.
 *
 * <h2>2. The per-victim window: WHY IT DOES NOT READ noDamageTicks</h2>
 * Vanilla lets a re-hit through inside the i-frame window when {@code amount > lastHurt}, dealing
 * only the DIFFERENCE. We do not deal a difference -- we deal the weapon's full custom damage -- so
 * riding that rule directly would let a player click through a rising charge and land roughly five
 * full-damage hits inside one 10-tick window. Spam would beat timing, which inverts the entire
 * charge model.
 *
 * <p>The obvious guard is to ask the victim whether it is inside its i-frames
 * ({@code getNoDamageTicks() > 10}, vanilla's own test). <b>The 2026-08-28 Step 0 boot disproved
 * it.</b> The instrumentation logged 20 {@code FIRE_TICK} events plus {@code FALL} and {@code LAVA},
 * every one of them driving the same counter:
 *
 * <pre>
 * [STEP0] OTHER victim=ZOMBIE cause=FIRE_TICK rawDamage=1.0000 victimIFrames=0
 * [STEP0] OTHER victim=BAT    cause=LAVA      rawDamage=4.0000 victimIFrames=10
 * </pre>
 *
 * A burning mob takes a fire tick every second, so a noDamageTicks-based guard would read "not
 * fresh" for about ten of every twenty ticks and our melee would deal ZERO for half of every second
 * -- strictly worse than the vanilla it imitates. And that is not an edge case here: this project
 * ships a fire element and a Scorch DoT, so setting mobs alight is a designed interaction.
 *
 * <p>So the window keys on OUR OWN hit history instead. Environmental damage cannot suppress a
 * swing, and "one custom hit per victim per {@link #WINDOW_TICKS} ticks" still holds however fast
 * the player clicks. We inherit vanilla's i-frames for WHEN THE EVENT FIRES -- a re-hit vanilla
 * refuses outright never reaches us -- and own only WHEN WE APPLY DAMAGE.
 *
 * <p>The window has a second reader now: {@link #landedThisTick} derives "a real hit landed on this
 * victim this tick" from the stamp the claim already writes, and the knockback gate uses it to let
 * vanilla's melee push through on exactly the hits that dealt damage. Derived rather than stored, so
 * the two cannot drift and there is no second map to bound.
 *
 * <h2>Threading and bounds</h2>
 * Backed by {@link CooldownTracker}, which is written to be entered concurrently: under Folia two
 * victims in different regions are hit on different threads at the same instant. Each victim's entry
 * is only ever touched by the thread owning that victim's region, so no per-key synchronisation is
 * needed on top.
 *
 * <p>It is a PRIVATE tracker, not the shared ability-cooldown one, for a specific reason:
 * {@link #forget} calls {@code clear(uuid)}, and on the shared instance that would also wipe that
 * entity's ability cooldowns and broken-weapon notice throttle. Harmless while player victims are
 * skipped, and a live bug the day PvP lands. Bounded by {@link #forget} on death, despawn and
 * chunk-unload; {@link #trackedVictims} is the leak check.
 */
public final class MeleeHits {

    /**
     * How long one victim is closed to further custom melee damage. Matches vanilla's own i-frame
     * window (invulnerableTime counts down from 20 and re-hits are refused above 10), so the cadence
     * a player feels is the cadence vanilla would have given them.
     */
    public static final int WINDOW_TICKS = 10;

    /** The single key every victim's window is stored under. Namespaced like BrokenNotice's. */
    private static final String WINDOW_KEY = "__melee_window";

    private final Map<UUID, Swing> pending = new ConcurrentHashMap<>();
    private final CooldownTracker windows;

    public MeleeHits(LongSupplier currentTick) {
        this.windows = new CooldownTracker(currentTick);
    }

    /** What the pre-attack event knew and the damage event needs. */
    public record Swing(UUID victim, double charge) {}

    /** Remember this attacker's swing. Overwrites any previous one, which by then never landed. */
    public void record(UUID attacker, UUID victim, double charge) {
        pending.put(attacker, new Swing(victim, charge));
    }

    /**
     * Take this attacker's pending swing, if it is the one that just landed on {@code victim}.
     *
     * <p>Removes unconditionally: a swing is consumed by the attempt, not by the success, so a
     * mismatch cannot leave a stale entry to be misread by the next hit. The victim check is what
     * makes a mismatch fail CLOSED -- an absent or mismatched swing yields empty, and the caller
     * deals no custom damage rather than guessing at a charge.
     */
    public Optional<Swing> consume(UUID attacker, UUID victim) {
        Swing swing = pending.remove(attacker);
        if (swing == null || !swing.victim().equals(victim)) return Optional.empty();
        return Optional.of(swing);
    }

    /**
     * Claim the right to deal custom melee damage to {@code victim} now.
     *
     * <p>True at most once per {@link #WINDOW_TICKS}. Claiming is a mutation, not a question: the
     * caller must act on a true and must not call it twice for one hit.
     */
    public boolean claimWindow(UUID victim) {
        if (!windows.isReady(victim, WINDOW_KEY)) return false;
        windows.trigger(victim, WINDOW_KEY, WINDOW_TICKS);
        return true;
    }

    /**
     * Did a real melee hit land on {@code victim} on THIS tick?
     *
     * <p>The knockback gate reads this. It is deliberately NOT "is the window active": a mob hit
     * three ticks ago still has an active window, so a windowed-out click now would read as active
     * and release a push it did not earn. Tick-exact is the whole point of it.
     *
     * <p>For a SINGLE attacker that release turns out to be unreachable -- the 2026-08-28 boot showed
     * vanilla suppressing a windowed-out re-hit's knockback upstream, before the gate is consulted
     * (tick 12170: a re-hit reached the rider, claimed nothing, and raised no knockback event). The
     * tick-exactness earns its keep in the cases that boot did not cover -- co-op, and any desync
     * between our window and the victim's real state -- not in single-attacker spam.
     *
     * <p>DERIVED from the window rather than stored beside it. {@link #claimWindow} already stamps
     * the victim with {@code readyAt = now + WINDOW_TICKS}, so a FULL window remaining means that
     * claim happened on this very tick; nine means last tick; zero means never. That leaves no
     * second map to leak, nothing for {@link #forget} to miss, and no way for the two to disagree
     * about what counts as a hit -- the claim IS the hit.
     *
     * <p>A pure query, and unlike {@link #claimWindow} it may be asked any number of times for one
     * hit -- which it MUST be. Paper's {@code EntityPushedByEntityAttackEvent} warns that "some
     * entities might trigger this multiple times on the same entity as multiple acceleration
     * calculations are done", and the 2026-08-28 knockback boot MEASURED exactly that:
     *
     * <pre>
     * tick  sprinting  knockbackEvents
     * 534   false      1
     * 568   true       2      &lt;-- two ENTITY_ATTACK events for ONE hit
     * 647   true       2
     * 721   true       2
     * </pre>
     *
     * Eleven non-sprint hits raised one event each; three of four SPRINT hits raised two. So a
     * consume-on-read signal would have cancelled the second event on a sprint hit and eaten the
     * sprint bonus -- the exact feel this pass exists to deliver, lost to a guard meant to protect
     * it. The tick stamp is the bound instead, and it expires on its own.
     */
    public boolean landedThisTick(UUID victim) {
        return windows.ticksRemaining(victim, WINDOW_KEY) == WINDOW_TICKS;
    }

    /** Drop a victim's window. Call on death, despawn and chunk-unload, or the map grows forever. */
    public void forget(UUID victim) {
        windows.clear(victim);
    }

    /** Drop an attacker's un-landed swing. Call on quit. */
    public void forgetAttacker(UUID attacker) {
        pending.remove(attacker);
    }

    /** Victims holding window state. The leak check. */
    public int trackedVictims() {
        return windows.trackedPlayers();
    }

    /** Attackers holding an un-landed swing. Bounded by the online player count. */
    public int pendingSwings() {
        return pending.size();
    }
}
