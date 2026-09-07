package io.github.butterflysmp.rpg.core.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * One victim's damage cadence, owned by us because vanilla can no longer keep it.
 *
 * <h2>Why this exists — and every alternative was eliminated by MEASUREMENT</h2>
 *
 * The environmental damage rider tokens the vanilla event so vanilla's own damage cannot double the
 * custom number. That token becomes {@code lastHurt}: {@code LivingEntity.hurtServer} writes the
 * POST-EVENT amount into it (offsets 239 and 311), so vanilla's {@code amount > lastHurt} ratchet
 * (offset 168) then admits every subsequent tick. Measured on the wire, 2026-09-06:
 *
 * <pre>
 * tick=191119 LAVA iFrames=0  lastDmg=0.0000 raw=4.0000 final=4.0000   first hit, window shut
 * tick=191120 LAVA iFrames=19 lastDmg=0.0100 raw=4.0000 final=3.9900   and every tick after
 * </pre>
 *
 * <p><b>Lava and suffocation have no clock of their own — vanilla's invulnerability window IS their
 * cadence</b> — so poisoning it does not accelerate them, it removes the only clock they had. They
 * run at 20 Hz, and on a large hitbox at <b>20 Hz × blocks contacted</b>: the same capture recorded
 * <b>eight {@code LAVA} events for one iron golem in one tick</b>, and a 100 HP golem died in 2-3.
 *
 * <p>The alternatives are dead, each for a read reason rather than a guessed one:
 * <ul>
 *   <li><b>Cancel</b> — {@code actuallyHurt} returns false when the event is cancelled (offsets
 *       9-18), and {@code hurtServer} then returns at 281, <b>before</b> {@code putfield lastHurt}
 *       (311) <b>and before</b> {@code putfield invulnerableTime} (319). The window never opens at
 *       all, so cancelling is 20 Hz too, by the opposite mechanism.
 *   <li><b>Leave the amount alone</b> — {@code ArmorBarOverride} writes our damage reduction into
 *       vanilla's ARMOR attribute, and vanilla runs its own non-linear curve over it. Measured on an
 *       armoured lava hit: {@code raw=4.0000 final=3.6704}, our 20% arriving as vanilla's 8.01%.
 *   <li><b>Any smaller token</b> — the comparison happens in VANILLA's scale, so any reduction
 *       poisons the ratchet. There is no token value that preserves cadence.
 * </ul>
 *
 * So: suppress the vanilla amount AND own the cadence. This is the owning half.
 *
 * <h2>KNOWN DELIBERATE DIVERGENCE: vanilla has ONE window, this project has THREE</h2>
 *
 * <b>This class does not cause that. It makes it legible, by being the third.</b>
 * {@code MeleeHits.claimWindow} is consulted at exactly two call sites — the sweep rider and the
 * player-melee rider — and <b>{@code onMobMeleeAttack} consults no window at all.</b> Adding a third
 * independent window, keyed on the same UUID, surfaces the split:
 *
 * <ul>
 *   <li><b>A player standing in lava, hit by a zombie.</b> Lava opens this window. The zombie's
 *       {@code ENTITY_ATTACK} is PASSed by {@code VanillaDamagePolicy} and handled by
 *       {@code onMobMeleeAttack}, which consults nothing — so it deals FULL damage where vanilla
 *       would have dealt only the difference. <b>We deal MORE than vanilla, and only while the victim
 *       is standing in something.</b>
 *   <li><b>A mob in lava that you hit.</b> {@code MeleeHits}' window and this one are independent maps
 *       on the same UUID; both land in the same tick. Vanilla shares one counter.
 * </ul>
 *
 * <p><b>Deliberately not fixed here</b> — the melee windows carry their own recorded justifications
 * and unifying them is its own change. It is written down because, unwritten, it returns as a bug
 * report about <i>lava making mobs hit harder</i>, and whoever finds it will not know it was a
 * decision.
 *
 * <h2>Denomination: VANILLA units, and why there is no {@code settle()}</h2>
 *
 * {@code amount} is the raw event damage — vanilla units, before the shield, before
 * {@link Defense#applyDefense}, before the vanilla-to-custom conversion. <b>Vanilla units are
 * max-independent</b>, so this window is immune to a victim's custom max changing mid-window (the
 * conversion factor is per-victim, and the heart scale is TIERED, stepping at 100 HP).
 *
 * <p>Storing pre-mitigation would normally make top-ups under-deal — a window holding more than
 * actually landed. <b>It does not here, and that is a proof rather than a hope:
 * {@link Defense#applyDefense} is LINEAR in damage</b> ({@code damage * SCALE/(SCALE+defense)}, and
 * the non-positive-defense branch returns {@code damage} unchanged), so {@code f(a) - f(b) = f(a-b)}
 * exactly. Ratcheting pre-mitigation is identical to ratcheting post-mitigation.
 *
 * <p><b>That is an invariant this design rests on, not an incidental fact.</b>
 * {@code DefenseTest.applyDefenseIsLINEARInDamageWhichIsWhatLetsDamageWindowRatchetPreMitigation}
 * guards it and reddens if anyone gives Defense a non-linear term.
 *
 * <p><i>Narrow residue, recorded rather than solved:</i> if a victim's DEFENSE changes mid-window the
 * function itself changes between claims. Same class as the conversion-factor hazard, an order of
 * magnitude rarer.
 *
 * <h2>Thread safety</h2>
 *
 * A {@link ConcurrentHashMap}, for {@code CooldownTracker}'s reason: under Folia two victims in
 * different regions are damaged on different threads at the same instant. One victim's entry is only
 * ever written by the thread owning that victim's region, so {@link #claim}'s read-modify-write is not
 * contended in practice.
 *
 * <h2>Bounded</h2>
 *
 * {@link #forget} must be called on mob removal, on player quit AND <b>on player respawn</b> — see its
 * javadoc for why the third is a correctness fix rather than a leak fix.
 */
public final class DamageWindow {

    /**
     * How long one victim is closed to further custom environmental damage.
     *
     * <p>Matches the gated region measured in the 2026-09-06 capture: {@code invulnerableTime} counts
     * down from 20, {@code hurtServer} takes the in-window branch only while it is above
     * {@code invulnerableDuration/2}, and the capture shows the counter ratcheting at 19..11 and the
     * normal path re-arming at exactly 10.
     *
     * <p><b>The same number and the same reason as {@code MeleeHits.WINDOW_TICKS}, deliberately
     * duplicated:</b> that constant lives in the paper module and {@code core} may not depend on it.
     * Unifying them means moving it down here, which is a change to a shipped melee path and not
     * this commit's business.
     */
    public static final int WINDOW_TICKS = 10;

    /** A victim's live window: when it closes, and the largest amount admitted inside it so far. */
    private record Window(long expiresAtTick, double appliedThisWindow) {}

    private final LongSupplier currentTick;

    /**
     * ONE map, holding both halves of the window.
     *
     * <p>Not a {@code CooldownTracker} for the timing beside a second map for the amounts.
     * {@code MeleeHits} may compose one because its window carries NO value; this one does, and two
     * structures is exactly the shape that class's javadoc warns against — it "leaves no second map to
     * leak, nothing for forget to miss, and no way for the two to disagree."
     */
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public DamageWindow(LongSupplier currentTick) {
        this.currentTick = currentTick;
    }

    /**
     * Claim the right to deal environmental damage to {@code victim} now, and get back WHAT TO DEAL.
     *
     * <p><b>Claiming is a MUTATION, not a question</b> — {@code MeleeHits.claimWindow}'s contract,
     * carried across: the caller must act on the return value and must not call this twice for one
     * event.
     *
     * <p>Three arms, mirroring {@code hurtServer}:
     * <ul>
     *   <li><b>BYPASS</b> — {@code bypassesCooldown} returns the full amount and <b>leaves the window
     *       untouched</b>, both halves of it. Vanilla's offset 152 jumps past the ratchet without
     *       touching {@code invulnerableTime}, and a bypassing source that wrote into the window would
     *       ratchet every ordinary claim after it against a value it had no business setting.
     *   <li><b>OPEN</b> — no live window: deal it all, and open one.
     *   <li><b>RATCHET</b> — a live window: deal only the excess over what has already landed, or
     *       nothing if this hit is no bigger.
     * </ul>
     *
     * <p><b>A ratchet must NOT extend the expiry, and the obvious implementation does.</b> Vanilla's
     * in-window branch writes {@code lastHurt} and jumps to 336 with <b>no {@code putfield
     * invulnerableTime} anywhere in it</b>; only the normal path re-arms, at 319. A window refreshed
     * by top-ups would never close under a rising stream.
     *
     * @param amount the RAW vanilla event damage. Non-positive returns 0 and opens nothing, mirroring
     *               vanilla's own zero-damage early return at offsets 283-308 — a zero-damage event
     *               must not open a window that then absorbs a real hit.
     * @return how much to deal now; {@code 0.0} means this hit was absorbed and nothing landed
     */
    public double claim(UUID victim, double amount, boolean bypassesCooldown) {
        if (amount <= 0) return 0.0;
        if (bypassesCooldown) return amount;

        long now = currentTick.getAsLong();
        Window live = windows.get(victim);
        if (live == null || now >= live.expiresAtTick()) {
            windows.put(victim, new Window(now + WINDOW_TICKS, amount));
            return amount;
        }
        if (amount <= live.appliedThisWindow()) return 0.0;

        // The excess only, and the SAME expiry: a top-up does not buy more window.
        windows.put(victim, new Window(live.expiresAtTick(), amount));
        return amount - live.appliedThisWindow();
    }

    /**
     * Drop a victim's window. Safe for a victim that has none.
     *
     * <p><b>Three call sites, and the third is not about leaking.</b>
     * <ul>
     *   <li><b>Mob removal</b> (death, despawn, chunk-unload) and <b>player quit</b> bound the map, or
     *       it grows for the lifetime of the server.
     *   <li><b>Player RESPAWN is a correctness fix.</b> Quit handling does not run on death, and the
     *       entity-removal handler filters players out — so without this, a player who dies mid-window
     *       respawns still holding it, and <b>the next environmental hit inside {@link #WINDOW_TICKS}
     *       is absorbed.</b> Bounded by online players, so not a leak; but respawning into lava or a
     *       wall is precisely when environmental damage arrives.
     * </ul>
     */
    public void forget(UUID victim) {
        windows.remove(victim);
    }

    /** Number of victims holding window state. The bounds check for tests. */
    public int trackedVictims() {
        return windows.size();
    }
}
