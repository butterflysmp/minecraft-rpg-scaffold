package io.github.butterflysmp.rpg.core.weapon;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * B2 (the seat, 2026-11-25): is a set active, from what the four armor slots hold. Pure.
 *
 * <p><b>ACTIVE exactly when all four slots hold pieces of the same set</b>, each in its own slot. Ben: "Full set only".
 *
 * <p><b>A BROKEN PIECE STILL COUNTS</b> (Ben: "No a broken piece doesn't break the set, but it does stop working like the
 * other items"). So {@link Worn#broken} is carried and deliberately NOT read here: what a broken piece loses (its own
 * Defense, enchants and wear) is decided where those are summed, never by the set.
 *
 * <p>Minted and untagged pieces are indistinguishable here by construction: both resolve to a definition through the
 * one lookup ({@link ArmorDefenseSource#resolve}) before the decision sees them (A3).
 */
public final class ArmorSetDecision {

    private ArmorSetDecision() {}

    /** What one slot holds: the definition it resolved to (empty: nothing of ours, or nothing), and whether it is broken. */
    public record Worn(Optional<ArmorDefinition> piece, boolean broken) {}

    /** The active set: its id (the tier) and its bonus list. */
    public record ActiveSet(String id, List<ArmorSetBonus> bonuses) {}

    public static Optional<ActiveSet> active(Map<ArmorSlot, Worn> worn) {
        String set = null;
        ArmorDefinition any = null;
        for (ArmorSlot slot : ArmorSlot.values()) {
            Worn w = worn.get(slot);
            if (w == null || w.piece().isEmpty()) return Optional.empty();
            ArmorDefinition piece = w.piece().get();
            if (piece.slot() != slot || piece.set().isEmpty()) return Optional.empty();
            if (set == null) {
                set = piece.set().get();
                any = piece;
            } else if (!set.equals(piece.set().get())) {
                return Optional.empty();
            }
        }
        return Optional.of(new ActiveSet(set, any.setBonus()));
    }
}
