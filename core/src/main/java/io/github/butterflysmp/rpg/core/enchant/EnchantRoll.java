package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.weapon.GearClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * The per-instance enchant roll: which candidates a piece of GEAR is born offering.
 *
 * <h2>THE SHAPE, 2026-11-23: ONE UNIQUE SLOT AND THREE NORMAL ONES</h2>
 *
 * Ben, verbatim: <i>"the first enchantment is a "Unique" enchantment in that the enchantments in that slot are tied to
 * the element more. The second and third enchantments are still from the normal pool"</i>; then, the same day, <i>"Instead
 * of 2 normal enchantment slots, I want 3 in addition to the unique enchantment."</i> and <i>"I want to allow a chance for 4
 * enchantments in each instead of 3"</i>. The seat's rulings U1-U6 and V1-V3.
 *
 * <ul>
 *   <li><b>{@link #SLOTS} = 4</b>, always. {@link #UNIQUE_SLOT} (0) draws ONLY from {@link #uniquePoolFor}: the enchants
 *       whose {@code element} is the gear's, then the class filter, then {@code requires}. Slots 1, 2 and 3 draw ONLY from
 *       {@link #normalPoolFor}: the enchants with NO element, by class then {@code requires}. A unique enchant never
 *       reaches slots 1-3, and a normal one never reaches slot 0.</li>
 *   <li><b>No element, or an empty unique pool, gives slot 0 NO candidates</b> (Ben's "A": "make it possible that an item
 *       has none if there are none in the pool"). No gear loses a normal slot.</li>
 *   <li><b>Each slot offers up to {@link #MAX_CANDIDATES} (4) distinct candidates</b>, unique and normal alike, counted by
 *       {@link CandidateCascade} (Ben's cascade, authored in content) and capped at the pool's size.</li>
 * </ul>
 *
 * <p>Every candidate arrives LOCKED at level 0 with nothing active: the roll decides what is on offer, the player decides
 * what to buy.
 *
 * <p><b>Decomposed by decision KIND, not by draw.</b> {@link CandidateCascade#lands} and {@link #pick} each take one
 * already-drawn double, so each real boundary is pinned at an exact value with no random source. {@link #roll} is a thin
 * orchestrator over them.
 *
 * <p>The {@code ThreadLocalRandom} draw and the PDC write stay in paper ({@code EnchantRollItems}), with the
 * once-per-item guard. Nothing here knows which enchants exist: the roster arrives as {@link Rollable}s that paper builds
 * from {@code EnchantDefinition}.
 */
public final class EnchantRoll {

    private EnchantRoll() {}

    /**
     * Every rollable piece of gear gets exactly this many slots: the unique slot and three normal ones (Ben, 2026-11-23,
     * V1). WAS 3 until then. Held equal to {@code EnchantMenuLayout.SLOTS} by a test in paper, so a roll the table cannot
     * show is caught at build time and not refused at the door in front of a player.
     */
    public static final int SLOTS = 4;

    /** The unique slot's index. The other slots are normal. */
    public static final int UNIQUE_SLOT = 0;

    /**
     * The most candidates one slot may offer (Ben's "a chance for 4 enchantments in each instead of 3", V2). WAS 3. Held
     * equal to {@code EnchantMenuLayout.CANDIDATES}; {@link CandidateCascade} refuses a tier above it.
     */
    public static final int MAX_CANDIDATES = 4;

    /**
     * One enchant, as the roll needs to see it: its id, the class it is valid on, the quantity it needs, and the element
     * that makes it UNIQUE.
     *
     * <p>{@code gearClass} is null for a {@code universal} enchant, valid on everything. {@code element} is null for a
     * NORMAL enchant, which every enchant was until 2026-11-23; non-null makes it unique to gear of that element (U1).
     */
    public record Rollable(String enchantId, GearClass gearClass, Requires requires, String element) {
        public Rollable {
            if (enchantId == null || enchantId.isBlank()) {
                throw new IllegalArgumentException("a rollable enchant needs an id");
            }
            if (requires == null) requires = Requires.NONE;
            if (element != null && element.isBlank()) {
                throw new IllegalArgumentException("enchant '" + enchantId + "': a blank element; absent means normal");
            }
        }

        /** A NORMAL enchant (no element), with a quantity requirement. */
        public Rollable(String enchantId, GearClass gearClass, Requires requires) {
            this(enchantId, gearClass, requires, null);
        }

        /** A NORMAL enchant with no quantity requirement. */
        public Rollable(String enchantId, GearClass gearClass) {
            this(enchantId, gearClass, Requires.NONE, null);
        }

        public boolean isUnique() {
            return element != null;
        }
    }

    /**
     * THE QUANTITY AN ENCHANT NEEDS THE HELD WEAPON TO HAVE (the seat's ruling H, 2026-11-22): AN ENCHANT ROLLS ONLY ON A
     * WEAPON THAT HAS ITS QUANTITY. Declared beside the enchant (its file's {@code requires:}), never as a list of weapon
     * ids, so the next weapon needs no edit. NAME THE QUANTITY's eligibility half, made a filter.
     */
    public enum Requires {
        /** No requirement beyond the class. */
        NONE,
        /** A reload: {@code reload_ticks} above 0 ({@code deft_hands}). */
        RELOAD,
        /** A quiver: {@code quiver_size} above 0 ({@code expanded_quiver}). */
        QUIVER;

        public boolean metBy(Has held) {
            return switch (this) {
                case NONE -> true;
                case RELOAD -> held.reload();
                case QUIVER -> held.quiver();
            };
        }
    }

    /** The quantities the held gear has, as {@link Requires} reads them. */
    public record Has(boolean reload, boolean quiver) {
        /** Every quantity: the pool is the class filter alone (how every pool was built before ruling H). */
        public static final Has EVERY = new Has(true, true);
        /** No quantity: armour, shields, tools, and a weapon with no magazine and no reload. */
        public static final Has NOTHING = new Has(false, false);
    }

    /**
     * THE NORMAL POOL: the enchants slots 1-3 may offer on gear of class {@code heldClass}, in roster order. Named
     * {@code poolFor} until 2026-11-23, when it was the only pool; renamed so no caller reads it as "every enchant".
     *
     * <p>A unique enchant (one with an element) is never in it. Order is preserved, so the pool is a deterministic
     * function of the registry and a fixed set of draws reproduces a fixed roll.
     *
     * <p>A null {@code heldClass} yields the universal normal enchants and nothing else (unreachable: {@code class} is
     * required on a weapon and a shield). An ACCESSORY's pool is EMPTY (ruling A4), not "every universal enchant".
     */
    public static List<Rollable> normalPoolFor(Has held, GearClass heldClass, List<Rollable> roster) {
        List<Rollable> pool = new ArrayList<>();
        if (roster == null) return pool;
        // Ruling A4, EXPLICIT: accessories take no part in enchanting. EnchantRollItems.rollOnAcquire also refuses an
        // accessory before it writes anything; this is the half a unit test can see.
        if (heldClass == GearClass.ACCESSORY) return pool;
        for (Rollable rollable : roster) {
            if (rollable == null) continue;
            // U3: a unique enchant is slot 0's alone.
            if (rollable.isUnique()) continue;
            // null class == universal: no gate, valid on whatever it is offered for.
            if (rollable.gearClass() != null && rollable.gearClass() != heldClass) continue;
            // RULING H: AFTER the class filter, the quantity the enchant reads must exist on the held weapon.
            if (!rollable.requires().metBy(held)) continue;
            pool.add(rollable);
        }
        return pool;
    }

    /**
     * THE UNIQUE POOL: the enchants slot 0 may offer, in roster order. Three filters, in this order (U2): the element must
     * be the gear's, then the class (or universal), then {@code requires}.
     *
     * @param element the gear's element, {@code Optional.empty()} for gear with none; NEVER null (U6: a caller cannot skip
     *                it by passing nothing). Empty gives an empty pool.
     */
    public static List<Rollable> uniquePoolFor(Optional<String> element, Has held, GearClass heldClass,
                                               List<Rollable> roster) {
        if (element == null) {
            throw new IllegalArgumentException("the gear's element is required: Optional.empty() for gear with none");
        }
        List<Rollable> pool = new ArrayList<>();
        if (roster == null || element.isEmpty()) return pool;
        if (heldClass == GearClass.ACCESSORY) return pool;   // ruling A4, as above
        for (Rollable rollable : roster) {
            if (rollable == null) continue;
            if (!element.get().equals(rollable.element())) continue;                                    // 1. element
            if (rollable.gearClass() != null && rollable.gearClass() != heldClass) continue;            // 2. class
            if (!rollable.requires().metBy(held)) continue;                                             // 3. requires
            pool.add(rollable);
        }
        return pool;
    }

    /**
     * The candidate a single draw selects from {@code remaining}: uniform over its indices.
     *
     * <p>Takes the pool it draws from rather than a fixed one, because the caller REMOVES each pick before drawing again,
     * which makes a slot's candidates distinct by construction rather than by a retry loop. {@code 0.0} is always the
     * first entry and a draw just under 1 always the last, whatever the pool has shrunk to. Null on an empty pool.
     */
    public static Rollable pick(List<Rollable> remaining, double roll) {
        if (remaining == null || remaining.isEmpty()) return null;
        int index = (int) (roll * remaining.size());
        return remaining.get(Math.max(0, Math.min(index, remaining.size() - 1)));
    }

    /**
     * A whole piece of gear's opening state: {@link #SLOTS} slots of locked candidates, slot 0 from the unique pool and
     * slots 1-3 from the normal pool.
     *
     * <p>Each slot draws from a FRESH copy of its pool: distinctness is within a slot (and {@code EnchantSlot}'s own
     * constructor enforces it), so two normal slots may both offer Sharpness, and {@code EnchantState.effective()}
     * resolves that to the highest level, never the sum.
     *
     * <p>Draws, slot by slot: the cascade's (one per tier rolled, none for an empty pool or the guaranteed tier), then one
     * per candidate picked. Stated for the end-to-end test only.
     *
     * @param element the gear's element (U5: {@code GearDefinition.gearElement()}); {@code Optional.empty()} for none;
     *                never null (U6)
     */
    public static EnchantState roll(Optional<String> element, Has held, GearClass heldClass, List<Rollable> roster,
                                    CandidateCascade cascade, DoubleSupplier draws) {
        if (element == null) {
            throw new IllegalArgumentException("the gear's element is required: Optional.empty() for gear with none");
        }
        if (cascade == null) throw new IllegalArgumentException("the candidate cascade is required");
        List<Rollable> unique = uniquePoolFor(element, held, heldClass, roster);
        List<Rollable> normal = normalPoolFor(held, heldClass, roster);
        List<EnchantSlot> slots = new ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) {
            List<Rollable> remaining = new ArrayList<>(slot == UNIQUE_SLOT ? unique : normal);
            int count = cascade.count(remaining.size(), draws);
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

    /**
     * THE DEV TALLY ({@code /rpg enchant rollstats}): for each slot index 0..{@link #SLOTS}-1, how many of {@code states}
     * offered 0..{@link #MAX_CANDIDATES} candidates there. A slot a state lacks counts as 0; a count past the bound is
     * clamped into the last column (no shipped roll makes one).
     */
    public static int[][] tally(List<EnchantState> states) {
        int[][] out = new int[SLOTS][MAX_CANDIDATES + 1];
        for (EnchantState state : states) {
            for (int slot = 0; slot < SLOTS; slot++) {
                int n = slot < state.slots().size() ? state.slots().get(slot).candidates().size() : 0;
                out[slot][Math.min(n, MAX_CANDIDATES)]++;
            }
        }
        return out;
    }
}
