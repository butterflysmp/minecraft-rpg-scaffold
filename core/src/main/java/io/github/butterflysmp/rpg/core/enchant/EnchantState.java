package io.github.butterflysmp.rpg.core.enchant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An item's whole custom-enchant state: its slots, their candidates, each candidate's unlocked
 * level, and which candidate each slot has active.
 *
 * IMMUTABLE. Every transition returns a new state and leaves the receiver untouched. This is not
 * style: the state's whole lifecycle is decode from a PDC string, transform, encode back, with no
 * object to keep between them -- so there is nothing for mutability to buy, and copy-on-write makes
 * "a swap did not scribble on the levels" a one-line assertion rather than an argument. The
 * stateful precedents in core ({@code CooldownTracker}, {@code ResourcePool}) are long-lived
 * per-player services; this is a value.
 *
 * <p><b>Slot count is deliberately uncapped, and stays so now that the roll has decided it.</b>
 * The roll produces exactly three ({@code EnchantRoll.SLOTS}), but this is the model for whatever
 * an item actually CARRIES -- a hand-edited one, or a blob written by a build whose roll differed
 * -- so a cap here would turn a weird item into an exception thrown from a decode. The bounds live
 * at the reachable surfaces instead: the dev command's argument range, and {@code
 * EnchantMenuLayout}, which refuses an oversized item loudly rather than truncating it.
 *
 * <p>Paper owns the item I/O; this owns the decisions, the same split as {@code Durability} and
 * {@code ClassDamageModifiers}. An {@code ItemStack} cannot be built without a running server, so
 * everything expressed here is everything that does not have to wait for a boot to be checked.
 */
public record EnchantState(List<EnchantSlot> slots) {

    /**
     * The highest level any custom enchant reaches. One number, referenced by the candidate's bound,
     * the codec's clamp, Unbreaking's curve and the dev command's argument range, so none of them
     * can drift from the others. A per-enchant {@code max_level} in content may be LOWER (and is
     * validated against this); nothing may be higher.
     */
    public static final int MAX_LEVEL = 3;

    public EnchantState {
        if (slots == null) {
            throw new IllegalArgumentException("slots required (use EnchantState.empty())");
        }
        slots = List.copyOf(slots);
    }

    /** An unenchanted item: no slots at all. What every mint and every absent PDC key produces. */
    public static EnchantState empty() {
        return new EnchantState(List.of());
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    /**
     * Offer {@code enchantId} on {@code slot}, LOCKED at level 0.
     *
     * {@code slot} may be an existing slot or exactly one past the end, which appends a new slot.
     * Skipping slots is refused: slot 3 on a one-slot item would have to invent two empty slots to
     * sit past, and an item silently growing slots nobody asked for is how a roster bug hides.
     */
    public EnchantState addCandidate(int slot, String enchantId) {
        if (slot < 0 || slot > slots.size()) {
            throw new IllegalArgumentException("slot " + slot + " does not exist and is not the next"
                    + " one; this item has " + slots.size() + " slot(s)");
        }
        List<EnchantSlot> next = new ArrayList<>(slots);
        if (slot == slots.size()) next.add(EnchantSlot.empty());

        EnchantSlot target = next.get(slot);
        List<EnchantCandidate> candidates = new ArrayList<>(target.candidates());
        // A duplicate id is caught by EnchantSlot's constructor, which is the one place that rule
        // lives -- so it holds for every route in, not just this one.
        candidates.add(new EnchantCandidate(enchantId, 0));
        next.set(slot, new EnchantSlot(candidates, target.activeIndex()));
        return new EnchantState(next);
    }

    /**
     * Set a candidate's unlocked level: the unlock, the upgrade, and the re-lock, in one transition.
     *
     * <b>Locking the ACTIVE candidate also clears active.</b> A documented consequence rather than a
     * refusal, because the alternative is asking every caller to clear active first and remember
     * why -- and the state EnchantSlot forbids (a locked candidate active) would be reachable the
     * moment one of them forgot. Every OTHER candidate's level is untouched, always.
     */
    public EnchantState withLevel(int slot, int candidate, int level) {
        requireCandidate(slot, candidate);
        EnchantSlot target = slots.get(slot);

        List<EnchantCandidate> candidates = new ArrayList<>(target.candidates());
        candidates.set(candidate, new EnchantCandidate(candidates.get(candidate).enchantId(), level));

        int active = target.activeIndex();
        if (level == 0 && active == candidate) active = EnchantSlot.NONE;

        List<EnchantSlot> next = new ArrayList<>(slots);
        next.set(slot, new EnchantSlot(candidates, active));
        return new EnchantState(next);
    }

    /**
     * Make a candidate the active one -- THE SWAP, and the transition this whole model exists for.
     *
     * Every candidate keeps its unlocked level across it, including the one being swapped away
     * from. Swapping to B and back to A must leave A exactly as it was, or the player has been
     * charged twice for the same unlock.
     */
    public EnchantState withActive(int slot, int candidate) {
        requireCandidate(slot, candidate);
        EnchantSlot target = slots.get(slot);
        EnchantCandidate chosen = target.candidates().get(candidate);
        if (chosen.isLocked()) {
            throw new IllegalArgumentException("'" + chosen.enchantId() + "' is locked (level 0) in"
                    + " slot " + slot + "; unlock it before making it active");
        }
        List<EnchantSlot> next = new ArrayList<>(slots);
        next.set(slot, new EnchantSlot(target.candidates(), candidate));
        return new EnchantState(next);
    }

    /** Clear a slot's active choice. Every candidate keeps its level. */
    public EnchantState withoutActive(int slot) {
        requireSlot(slot);
        EnchantSlot target = slots.get(slot);
        List<EnchantSlot> next = new ArrayList<>(slots);
        next.set(slot, new EnchantSlot(target.candidates(), EnchantSlot.NONE));
        return new EnchantState(next);
    }

    /**
     * The enchants taking effect right now: one entry per DISTINCT id, at the HIGHEST level any
     * active slot holds it at, in order of first appearance.
     *
     * <p><b>MAXIMUM, and that is the DECIDED rule rather than a placeholder</b> (the rolls pass).
     * Never additive, and there is deliberately no mutual exclusion: the same enchant may be
     * offered in more than one slot and active in more than one at once, and it resolves to the
     * highest level any active slot holds it at. The three reasons it was provisionally chosen are
     * the three reasons it was kept -- it can never exceed {@link #MAX_LEVEL}, so it cannot hand a
     * player a level no tooltip ever promised, which summing can; it is order-independent, where
     * first-wins would make the outcome depend on slot order the player cannot see; and duplicating
     * an enchant is therefore never a gain, so a roll that offers Sharpness twice has not quietly
     * made that weapon stronger than one that offers it once.
     *
     * <p>The aggregation lives HERE, once, rather than at the seam -- so the cap holds against a
     * duplicate arriving from any source (a hand-edited item, a future roll, an older build's
     * blob), not merely one the dev command could produce. The tooltip and the effect then read the
     * same list by construction and cannot disagree about the number.
     */
    public List<ActiveEnchant> effective() {
        Map<String, Integer> best = new LinkedHashMap<>();
        for (EnchantSlot slot : slots) {
            EnchantCandidate active = slot.active().orElse(null);
            if (active == null || active.isLocked()) continue;
            best.merge(active.enchantId(), active.level(), Math::max);
        }
        List<ActiveEnchant> out = new ArrayList<>(best.size());
        best.forEach((id, level) -> out.add(new ActiveEnchant(id, Math.min(level, MAX_LEVEL))));
        return List.copyOf(out);
    }

    /**
     * The level {@code enchantId} is taking effect at, or 0 if it is not.
     *
     * Implemented in terms of {@link #effective()} rather than as its own scan, so the number the
     * durability seam acts on is literally the number the tooltip rendered. Two scans would be two
     * chances to apply the max rule differently.
     */
    public int activeLevel(String enchantId) {
        for (ActiveEnchant active : effective()) {
            if (active.enchantId().equals(enchantId)) return active.level();
        }
        return 0;
    }

    private void requireSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            throw new IllegalArgumentException("slot " + slot + " does not exist; this item has "
                    + slots.size() + " slot(s)");
        }
    }

    private void requireCandidate(int slot, int candidate) {
        requireSlot(slot);
        int size = slots.get(slot).candidates().size();
        if (candidate < 0 || candidate >= size) {
            throw new IllegalArgumentException("candidate " + candidate + " does not exist; slot "
                    + slot + " has " + size + " candidate(s)");
        }
    }
}
