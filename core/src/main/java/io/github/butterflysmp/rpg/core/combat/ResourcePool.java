package io.github.butterflysmp.rpg.core.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Ability resources -- mana, and whatever else content asks for -- keyed by
 * (owner, resourceId). Shaped like CooldownTracker on purpose: a tick supplier
 * rather than Bukkit, so it can be tested with a fake clock.
 *
 * Regeneration is LAZY. Nothing ticks; the current value is computed on read
 * from the amount last written and how many ticks have passed since. That means
 * no repeating task, no per-player scheduler entry, and no drift -- a pool that
 * nobody reads costs nothing.
 *
 * Thread-safe, for the same reason CooldownTracker is: under Folia two players
 * in different regions cast on different threads at the same instant. Consumption
 * is a single atomic compute, so two concurrent casts cannot both spend the last
 * 40 mana.
 *
 * Bounded: clear(owner) drops the owner's pools. Call it when a player leaves.
 *
 * <h2>The ceiling is PER OWNER as of Armor Slice 2b</h2>
 *
 * It was a single {@code final double} shared by every player, which was right for exactly as long
 * as nothing could raise a maximum. Mana Bank can, so the ceiling arrives as a {@link MaxResolver}
 * and every read asks it for the owner in hand. See that interface for why it is a function rather
 * than a second map this class owns -- the short version is that {@link #clear} means REFILL, so a
 * map here would have had {@code /rpg mana refill} strip a player's enchant.
 *
 * <p>There were FOUR reads of the old constant, not three: {@link #current}'s absent-owner branch,
 * {@link #regenerated}'s ceiling, {@link #tryConsume}'s never-satisfiable guard, and a duplicate of
 * the first INSIDE {@code tryConsume}'s {@code compute} lambda. That last one is why
 * {@code tryConsume} resolves the ceiling ONCE at the top and passes the local down.
 *
 * <p><b>Though not for the reason first written here.</b> That claimed a resolve inside the
 * {@code compute} lambda would break {@code concurrentSpendsCannotOverdrawThePool}. It does not --
 * measured, with the resolve moved in: all 23 pool tests stayed green. The reasons it stays OUT are
 * that a resolver is a LIVE read of a player's stats, so two reads straddling a gear change make
 * "the guard passed, then the spend refused" reachable -- which reports on screen as "needs 110, you
 * have 130" -- and that {@code ConcurrentHashMap} forbids a mapping function from touching the map
 * it is computing on, which arbitrary caller code cannot promise. What actually holds it is
 * {@code tryConsumeAsksTheResolverEXACTLYONCESoTheGuardAndTheSpendCannotDISAGREE}, which counts the
 * calls, because the arity is the observable part and the deadlock story was not.
 */
public final class ResourcePool {

    /** Amount as of a tick. Everything between then and now is regeneration. */
    private record Entry(double amount, long asOfTick) {}

    private final LongSupplier currentTick;
    private final MaxResolver max;
    private final double regenPerTick;
    private final Map<UUID, Map<String, Entry>> pools = new ConcurrentHashMap<>();

    public ResourcePool(LongSupplier currentTick, MaxResolver max, double regenPerTick) {
        if (max == null) throw new IllegalArgumentException("a max resolver is required");
        if (regenPerTick < 0) throw new IllegalArgumentException("regenPerTick must not be negative");
        this.currentTick = currentTick;
        this.max = max;
        this.regenPerTick = regenPerTick;
    }

    /**
     * One ceiling for every owner -- the pre-2b shape, kept because it is still the right one for a
     * pool with no per-player stat behind it.
     *
     * <p>Every test in the tree constructs through this, which is deliberate: leaving those
     * constructions byte-identical is what makes the resolver change provably behaviour-preserving.
     */
    public ResourcePool(LongSupplier currentTick, double max, double regenPerTick) {
        this(currentTick, MaxResolver.fixed(requirePositive(max)), regenPerTick);
    }

    private static double requirePositive(double max) {
        if (max <= 0) throw new IllegalArgumentException("max must be positive");
        return max;
    }

    /**
     * The ceiling for this owner's resource.
     *
     * <p>Takes an owner and a resource where it used to take nothing. Both callers of the old
     * no-arg version were displays -- the action bar and the refill message -- and both were showing
     * every player the same number.
     */
    public double max(UUID owner, String resourceId) {
        return max.maxFor(owner, resourceId);
    }

    /** An owner nobody has charged anything to is full. */
    public double current(UUID owner, String resourceId) {
        Map<String, Entry> owned = pools.get(owner);
        Entry entry = owned == null ? null : owned.get(resourceId);
        double ceiling = max.maxFor(owner, resourceId);
        return entry == null ? ceiling : regenerated(entry, ceiling);
    }

    /**
     * Write {@code amount} as this owner's current value, clamped to their ceiling.
     *
     * <h2>This is the max-change transition, and it exists to make both directions STATED</h2>
     *
     * Called by the reconcile loop when a max-mana modifier actually changes, with the value read
     * BEFORE the change. It is one mechanism serving two rules:
     *
     * <ul>
     *   <li><b>Max ROSE</b> -- the pre-change amount is pinned, so the ceiling moves and the amount
     *       does not. HEADROOM, never a free top-up. Without this an owner with NO entry would read
     *       the new ceiling instantly (absent means full), so equipping a Mana Bank piece would be
     *       free mana for a player who had never cast and headroom for one who had -- the same
     *       enchant behaving two ways depending on state nobody can see.
     *   <li><b>Max FELL</b> -- the clamp is the {@code Math.min} below, at the point of writing.
     *       {@link #regenerated} would have produced the same number on the next read, and that is
     *       the point: emergent from a {@code Math.min} in the regen path, a refactor there drops
     *       the unequip clamp with no test naming it. Here it is a decision.
     * </ul>
     *
     * <p>Writes nothing when {@code amount} is not finite or negative -- a caller with no reading to
     * pin should not be able to zero someone's pool through this door.
     */
    public void setCurrent(UUID owner, String resourceId, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return;
        double clamped = Math.min(max.maxFor(owner, resourceId), amount);
        pools.computeIfAbsent(owner, id -> new ConcurrentHashMap<>())
                .put(resourceId, new Entry(clamped, currentTick.getAsLong()));
    }

    /**
     * The regenerated value of one entry against a ceiling already resolved by the caller.
     *
     * <p>Takes the ceiling rather than reading it, because both callers had to resolve it anyway for
     * their own absent-entry branch -- and because {@code tryConsume} calls this from inside a
     * {@code compute} lambda, where a fresh resolver call would re-enter a map under a bin lock.
     */
    private double regenerated(Entry entry, double ceiling) {
        long elapsed = Math.max(0, currentTick.getAsLong() - entry.asOfTick());
        return Math.min(ceiling, entry.amount() + elapsed * regenPerTick);
    }

    /**
     * Spend {@code amount} if it is available. All-or-nothing: on failure not a
     * drop is taken, so a caller that reports "not enough mana" has not
     * quietly drained the player.
     *
     * @return true if the full amount was consumed
     */
    public boolean tryConsume(UUID owner, String resourceId, double amount) {
        if (amount <= 0) return true;            // a free ability always casts

        // ONCE, and before the compute below. Two of the four old reads of the ceiling were in this
        // method -- the guard here and the absent-entry fallback inside the lambda -- and resolving
        // it separately in each would both duplicate the work and put a map read inside a mapping
        // function. It is also a correctness point: the guard and the spend must agree about the
        // ceiling, or "the guard passed but the spend refused" becomes reachable.
        double ceiling = max.maxFor(owner, resourceId);
        if (amount > ceiling) return false;      // never satisfiable FOR THIS OWNER; do not wait forever

        Map<String, Entry> owned = pools.computeIfAbsent(owner, id -> new ConcurrentHashMap<>());

        // compute() applies the function atomically for this key, so the
        // read-modify-write below cannot interleave with another caster. A plain
        // get/put here lets 4-6 of 16 concurrent 40-mana casts through a pool
        // that fits 2 -- verified, not theoretical.
        boolean[] consumed = {false};
        owned.compute(resourceId, (id, entry) -> {
            double available = entry == null ? ceiling : regenerated(entry, ceiling);
            if (available < amount) {
                return entry; // untouched; null stays absent, which reads as full
            }
            consumed[0] = true;
            return new Entry(available - amount, currentTick.getAsLong());
        });
        return consumed[0];
    }

    /**
     * Drop every pool for this owner. O(1). Safe for an unknown owner.
     *
     * <p>This IS a refill, not merely cleanup: an owner with no entry reads as full, because the
     * pool stores a spent amount and a tick to regenerate from rather than a current value. It drops
     * pools and nothing else -- the ceiling lives behind a {@link MaxResolver} the pool does not
     * own, so a refill cannot strip a player's Mana Bank.
     */
    public void clear(UUID owner) {
        pools.remove(owner);
    }

    /** Number of owners holding resource state. Bounds check for tests. */
    public int trackedOwners() {
        return pools.size();
    }
}
