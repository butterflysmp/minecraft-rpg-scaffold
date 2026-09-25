package io.github.butterflysmp.rpg.core.equipment;

import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;

import java.util.Objects;
import java.util.Optional;

/**
 * May this armour gesture happen in the Equipment screen? The one decision, pure, so every rule is a
 * unit test rather than a boot row.
 *
 * <p>The Equipment screen's armour slots are the player's REAL armour slots, and the menu framework
 * cancels every click first -- so the screen is doing VANILLA'S job there, and this class is the
 * statement of what vanilla's job is. Each rule below cites the vanilla code it reproduces, read from
 * the 26.1.2 server jar (javap), never from memory.
 *
 * <ul>
 *   <li><b>Fit</b> -- {@code LivingEntity.isEquippableInSlot}: the item's equippable component must
 *       name THIS slot, and its allowed entities (if any) must admit a player. An item with no
 *       equippable component fits only the main hand, so never an armour slot.
 *   <li><b>Binding</b> -- {@code ArmorSlot.mayPickup}: a bound item may not LEAVE the slot unless the
 *       player is in creative. Vanilla keys this on the enchantment EFFECT {@code prevent_armor_change};
 *       we key it on Curse of Binding by name. In 26.1.2's data only {@code binding_curse} carries
 *       that effect, so they agree -- a datapack adding the effect elsewhere would diverge, and that
 *       divergence is recorded, not hidden.
 *   <li><b>A stack does not swap onto an occupied slot</b> -- {@code AbstractContainerMenu.doClick}'s
 *       pickup path: {@code if (carried.getCount() > slot.getMaxStackSize(carried))} it ends without
 *       swapping, and an armour slot's max is 1 ({@code ArmorSlot.getMaxStackSize}).
 *   <li><b>One from a stack</b> -- that same max of 1: placing or shift-clicking 16 heads moves one.
 * </ul>
 *
 * <h2>The one DELIBERATE divergence: shift-in onto an occupied slot is REFUSED</h2>
 *
 * Vanilla ({@code InventoryMenu.quickMoveStack}) does not refuse: with the armour slot occupied it
 * falls through to its ordinary main-inventory/hotbar hop. That hop belongs to vanilla's OWN screen;
 * in ours a shift-click aims at the armour column, so we move nothing ({@link Outcome#REFUSE_OCCUPIED})
 * rather than reshuffle the player's inventory. Ruled by the seat, and recorded in
 * {@code GATE-accessories-b.md}'s divergence section.
 *
 * <p>Nothing here reads an ItemStack: the paper side reduces the live item to the facts in
 * {@link Input}, at click time.
 */
public final class ArmorPlacement {

    private ArmorPlacement() {}

    /** What the click is trying to do, derived from the cursor and the slot at click time. */
    public enum Gesture {
        /** Cursor onto an EMPTY armour slot. */
        PLACE,
        /** An empty cursor onto an OCCUPIED armour slot. */
        TAKE,
        /** Cursor onto an OCCUPIED armour slot. */
        SWAP,
        /** A shift-click from the player's half of the screen. */
        SHIFT_IN,
        /** A shift-click on the armour slot itself. */
        SHIFT_OUT
    }

    /** The answer. Every refusal is named, so a gate row can say which one it saw. */
    public enum Outcome {
        PLACE_ONE,
        TAKE,
        SWAP,
        SHIFT_IN_ONE,
        SHIFT_OUT,
        /** The gesture has nothing to act on (an empty slot to take from, an occupied one to place into). */
        NOTHING,
        REFUSE_WRONG_SLOT,
        REFUSE_NOT_FOR_PLAYER,
        REFUSE_BOUND,
        REFUSE_OCCUPIED,
        REFUSE_STACK,
        REFUSE_STAR
    }

    /**
     * The facts one decision needs, read live at click time.
     *
     * @param target           the armour slot clicked, or the one a shift-in aims at
     * @param gesture          what the click is trying to do
     * @param itemSlot         the INCOMING item's equippable slot, when it names an armour slot;
     *                         empty when it has no equippable component or names another slot.
     *                         Ignored for TAKE and SHIFT_OUT, which bring nothing in.
     * @param allowedForPlayer the incoming item's allowed entities admit a player (true when unset)
     * @param restingBound     the item already in the slot carries Curse of Binding
     * @param creative         the player is in creative mode -- the one mode vanilla lets remove a bound item
     * @param incomingAmount   the incoming stack's size
     * @param slotOccupied     the armour slot holds an item now
     * @param incomingIsStar   the incoming item is the Nexus star
     */
    public record Input(ArmorSlot target, Gesture gesture, Optional<ArmorSlot> itemSlot,
                        boolean allowedForPlayer, boolean restingBound, boolean creative,
                        int incomingAmount, boolean slotOccupied, boolean incomingIsStar) {
        public Input {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(gesture, "gesture");
            itemSlot = itemSlot == null ? Optional.empty() : itemSlot;
        }
    }

    public static Outcome decide(Input in) {
        return switch (in.gesture()) {
            case PLACE -> {
                if (in.slotOccupied()) yield Outcome.NOTHING;
                Outcome refused = incomingRefusal(in);
                yield refused != null ? refused : Outcome.PLACE_ONE;
            }
            case TAKE -> {
                if (!in.slotOccupied()) yield Outcome.NOTHING;
                yield boundRefusal(in) ? Outcome.REFUSE_BOUND : Outcome.TAKE;
            }
            case SWAP -> {
                if (!in.slotOccupied()) yield Outcome.NOTHING;
                Outcome refused = incomingRefusal(in);
                if (refused != null) yield refused;
                if (boundRefusal(in)) yield Outcome.REFUSE_BOUND;
                // Vanilla's pickup path: a carried stack larger than the slot's max does not swap.
                yield in.incomingAmount() > 1 ? Outcome.REFUSE_STACK : Outcome.SWAP;
            }
            case SHIFT_IN -> {
                Outcome refused = incomingRefusal(in);
                if (refused != null) yield refused;
                // The deliberate divergence -- see the class note.
                yield in.slotOccupied() ? Outcome.REFUSE_OCCUPIED : Outcome.SHIFT_IN_ONE;
            }
            case SHIFT_OUT -> {
                if (!in.slotOccupied()) yield Outcome.NOTHING;
                yield boundRefusal(in) ? Outcome.REFUSE_BOUND : Outcome.SHIFT_OUT;
            }
        };
    }

    /** Null when the incoming item may enter this slot; otherwise the named refusal. */
    private static Outcome incomingRefusal(Input in) {
        if (in.incomingIsStar()) return Outcome.REFUSE_STAR;
        if (in.itemSlot().isEmpty() || in.itemSlot().get() != in.target()) return Outcome.REFUSE_WRONG_SLOT;
        if (!in.allowedForPlayer()) return Outcome.REFUSE_NOT_FOR_PLAYER;
        return null;
    }

    /** A bound resting item may not leave, except in creative -- vanilla's ArmorSlot.mayPickup. */
    private static boolean boundRefusal(Input in) {
        return in.restingBound() && !in.creative();
    }
}
