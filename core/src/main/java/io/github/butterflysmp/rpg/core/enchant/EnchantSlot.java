package io.github.butterflysmp.rpg.core.enchant;

import java.util.List;
import java.util.Optional;

/**
 * One of an item's enchant slots: the candidates it offers, and which one is ACTIVE.
 *
 * The active choice is an INDEX into {@link #candidates()}, not a repeated id. An id stored
 * alongside the list can disagree with the list -- name something the slot does not offer, or name
 * it at a level the candidate does not have -- and every reader would then need to decide which of
 * the two to believe. An index cannot disagree; it is either in range or it is {@link #NONE}.
 *
 * <p><b>A locked candidate cannot be active.</b> Rejected in the constructor rather than tolerated,
 * because the alternative is an item whose active enchant resolves to level 0: it would render on
 * the tooltip and do nothing, or do nothing and not render, depending on which reader asked. Making
 * it unconstructible means no reader has to remember the check. {@link EnchantState#withLevel}
 * honours this by clearing active when it locks the active candidate, rather than refusing.
 */
public record EnchantSlot(List<EnchantCandidate> candidates, int activeIndex) {

    /** No candidate is active. The slot's candidates keep their levels regardless. */
    public static final int NONE = -1;

    public EnchantSlot {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates required (use List.of() for an empty slot)");
        }
        // Records do NOT copy their constructor arguments. Without this the caller keeps a live
        // handle into the "immutable" value and can edit it afterwards.
        candidates = List.copyOf(candidates);

        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                if (candidates.get(i).enchantId().equals(candidates.get(j).enchantId())) {
                    throw new IllegalArgumentException("slot offers '" + candidates.get(i).enchantId()
                            + "' twice; a slot's candidates must be distinct");
                }
            }
        }

        if (activeIndex != NONE) {
            if (activeIndex < 0 || activeIndex >= candidates.size()) {
                throw new IllegalArgumentException("active index " + activeIndex
                        + " is outside this slot's " + candidates.size() + " candidate(s)");
            }
            if (candidates.get(activeIndex).isLocked()) {
                throw new IllegalArgumentException("'" + candidates.get(activeIndex).enchantId()
                        + "' is locked (level 0) and cannot be the active candidate");
            }
        }
    }

    /** A slot offering nothing, with nothing active. */
    public static EnchantSlot empty() {
        return new EnchantSlot(List.of(), NONE);
    }

    /** The candidate taking effect, if any. Never a locked one -- the constructor forbids it. */
    public Optional<EnchantCandidate> active() {
        return activeIndex == NONE ? Optional.empty() : Optional.of(candidates.get(activeIndex));
    }
}
