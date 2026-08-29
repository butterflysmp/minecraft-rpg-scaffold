package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.weapon.GearClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * The per-instance enchant roll: which candidates a piece of GEAR is born offering.
 *
 * <p>Gear gets {@link #SLOTS} slots, always, and each slot offers 1..{@link #MAX_CANDIDATES}
 * distinct candidates drawn from the enchants VALID for it -- one whose {@link GearClass} matches
 * the gear's, or a universal one. Every candidate arrives LOCKED at level 0 with nothing active:
 * the roll decides what is on offer, and the player decides what to buy.
 *
 * <p><b>Gear, not weapons, since Slice 2.</b> The axis is {@link GearClass}, so a shield rolls from
 * the shield pool exactly as a sword rolls from the melee one. Nothing about the decisions here
 * changed for that -- the filter was already total over a class it did not recognise -- but the
 * SHAPE of the shipped roster did: see {@link #candidateCount}.
 *
 * <p><b>Decomposed by decision KIND, not by draw.</b> {@link #candidateCount} and {@link #pick} each
 * take one already-drawn double, the same split {@code Unbreaking.consumes} uses and for the same
 * payoff -- each real boundary (the 1-versus-2 cut, the first and last index of a pool that shrinks
 * as distinctness removes what is already picked) is pinned at an exact value, in isolation, with no
 * random source and no seeded fake. {@link #roll} is a thin orchestrator over them so ONE readable
 * end-to-end test can assert the shape; the draw ORDER is deliberately not what any boundary
 * assertion depends on, because a flat array of draws couples a test's meaning to index arithmetic
 * nobody can see.
 *
 * <p>The actual {@code ThreadLocalRandom} draw and the PDC write stay in paper
 * ({@code EnchantRollItems}), which is also where the once-per-item guard lives. Nothing here knows
 * which enchants exist: the roster arrives as {@link Rollable}s that paper builds from
 * {@code EnchantDefinition}, the same way {@code DamageEnchants.Grant} does.
 */
public final class EnchantRoll {

    private EnchantRoll() {}

    /**
     * Every rollable weapon gets exactly this many slots. Not tier-varied: three is what the table
     * renders, and tiering it would be a new decision needing a layout change to go with it. See
     * {@code Rarity}, whose reserved per-tier meaning is the CANDIDATE axis for that reason.
     *
     * <p>Held equal to {@code EnchantMenuLayout.SLOTS} by a test in paper. They stay separate
     * constants -- the layout's is a UI-side bound that also has to refuse a hand-edited item -- but
     * a roll the table cannot show would be refused at the door, so the pin catches the drift at
     * build time instead of in front of a player.
     */
    public static final int SLOTS = 3;

    /** The most candidates one slot may offer. Held equal to {@code EnchantMenuLayout.CANDIDATES}. */
    public static final int MAX_CANDIDATES = 3;

    /**
     * One enchant, as the roll needs to see it: its id, and the class it is valid on.
     *
     * <p>{@code gearClass} is null for a {@code universal} enchant, valid on everything -- the
     * same null-means-no-gate convention {@code DamageEnchants.Grant} uses, read from the same
     * {@code EnchantDefinition.isUniversal()}.
     */
    public record Rollable(String enchantId, GearClass gearClass) {
        public Rollable {
            if (enchantId == null || enchantId.isBlank()) {
                throw new IllegalArgumentException("a rollable enchant needs an id");
            }
        }
    }

    /**
     * The enchants that may be offered on gear of class {@code heldClass}, in roster order.
     *
     * <p>Order is preserved rather than shuffled here, so the pool is a deterministic function of
     * the registry (a {@code LinkedHashMap} filled from files the loader sorts). All the randomness
     * lives in the draws, which is what lets a fixed set of draws reproduce a fixed roll.
     *
     * <p>A null {@code heldClass} yields the universal enchants and nothing else, which falls out of
     * the filter rather than needing an arm of its own. Still unreachable -- {@code class} is
     * required on both a weapon and a shield file, and the loader skips a file without it -- but
     * total either way. Note this is NOT the shield case: a shield presents
     * {@link GearClass#SHIELD}, a real value, not the absence of one.
     */
    public static List<Rollable> poolFor(GearClass heldClass, List<Rollable> roster) {
        List<Rollable> pool = new ArrayList<>();
        if (roster == null) return pool;
        for (Rollable rollable : roster) {
            if (rollable == null) continue;
            // null class == universal: no gate, valid on whatever it is offered for.
            if (rollable.gearClass() != null && rollable.gearClass() != heldClass) continue;
            pool.add(rollable);
        }
        return pool;
    }

    /**
     * How many candidates one slot offers: uniform over {@code 1..min(poolSize, MAX_CANDIDATES)}.
     *
     * <p>Uniform and unweighted, named as a starting point rather than a tuned curve.
     *
     * <p>It is NOT sized by rarity: with a pool of exactly two per class (its own class enchant,
     * plus Unbreaking), a tier curve is unobservable -- you cannot tell a legendary from a common by
     * a 1-versus-2 count without a sample far larger than a boot gate -- and it would ship
     * unwitnessable. That is the roster pass's decision, on this axis.
     *
     * <p><b>That argument NO LONGER covers SHIELD, as of Slice 2b.</b> A shield's pool is Bulwark
     * plus Thorns plus Unbreaking -- THREE -- while every weapon class is still two. So a 1..3 count
     * is now genuinely observable on one kind of gear, and {@code EnchantMenuLayout.CANDIDATES == 3}
     * is exercised by a real roll rather than only by {@link #candidateCount} in isolation.
     *
     * <p>Rarity-weighting stays deferred anyway: one class with a big enough pool is not a reason to
     * design a tier curve for all of them. But it is now deferred BY CHOICE rather than by
     * impossibility, and that distinction is the thing worth re-reading before the next roster pass.
     *
     * <p><b>An empty pool offers nothing, and that is a real arm.</b> It returns 0 rather than 1, so
     * the slot is offered empty instead of the caller being asked for a candidate that cannot exist.
     * Unreachable while Unbreaking is universal, and written anyway because the alternative is an
     * exception thrown from inside a mint.
     *
     * <p>{@code roll} is half-open {@code [0, 1)}, matching {@code ThreadLocalRandom.nextDouble()}.
     * The clamps are for a hand-fed value at or past the ends, so this cannot return a count the
     * pool is unable to fill.
     */
    public static int candidateCount(int poolSize, double roll) {
        int cap = Math.min(poolSize, MAX_CANDIDATES);
        if (cap <= 0) return 0;
        int count = 1 + (int) (roll * cap);
        return Math.max(1, Math.min(count, cap));
    }

    /**
     * The candidate a single draw selects from {@code remaining}: uniform over its indices.
     *
     * <p>Takes the pool it draws from rather than a fixed one, because the caller REMOVES each pick
     * before drawing again -- that shrinking is what makes a slot's candidates distinct by
     * construction rather than by a retry loop that could spin. So the boundaries worth pinning are
     * per-pool-size: {@code 0.0} is always the first entry, and a draw just under 1 always the last,
     * whatever the pool has shrunk to.
     *
     * <p>Null on an empty pool. The caller stops; it does not substitute anything.
     */
    public static Rollable pick(List<Rollable> remaining, double roll) {
        if (remaining == null || remaining.isEmpty()) return null;
        int index = (int) (roll * remaining.size());
        return remaining.get(Math.max(0, Math.min(index, remaining.size() - 1)));
    }

    /**
     * A whole piece of gear's opening state: {@link #SLOTS} slots of locked, class-valid candidates.
     *
     * <p>Each slot draws from a FRESH copy of the pool, and that is the whole of the
     * same-enchant-across-slots rule: distinctness is a within-slot property (enforced by
     * {@code EnchantSlot}'s own constructor, so this is not the only thing holding it), and two
     * slots may perfectly well both offer Sharpness. {@code EnchantState.effective()} then resolves
     * that to the highest level either holds it at, never the sum.
     *
     * <p>Draw arity is {@link #SLOTS} draws for the counts plus one per candidate for the picks,
     * interleaved slot by slot. Stated for the end-to-end test's benefit only -- no boundary
     * assertion depends on it.
     *
     * <p>Builds the slots directly rather than through {@code EnchantState.addCandidate}, because
     * that route cannot append an EMPTY slot and the empty-pool arm needs one. It also puts every
     * slot through {@code EnchantSlot}'s constructor, so a distinctness slip throws here rather than
     * reaching an item.
     */
    public static EnchantState roll(GearClass heldClass, List<Rollable> roster, DoubleSupplier draws) {
        List<Rollable> pool = poolFor(heldClass, roster);
        List<EnchantSlot> slots = new ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) {
            List<Rollable> remaining = new ArrayList<>(pool);
            int count = candidateCount(remaining.size(), draws.getAsDouble());
            List<EnchantCandidate> candidates = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Rollable picked = pick(remaining, draws.getAsDouble());
                if (picked == null) break;
                remaining.remove(picked);
                candidates.add(new EnchantCandidate(picked.enchantId(), 0));
            }
            slots.add(new EnchantSlot(candidates, EnchantSlot.NONE));
        }
        return new EnchantState(slots);
    }
}
