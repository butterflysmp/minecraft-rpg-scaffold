package io.github.butterflysmp.rpg.paper.menu;

/**
 * How an OWNED input slot accepts items.
 *
 * <p>A per-slot policy rather than a second {@code stackingInputSlots()} set, and the reason is
 * conservation rather than taste: {@link Menu#returnEverything}, {@link MenuRouting} shift-move,
 * hotbar-move and offhand-move ALL key off {@link Menu#inputSlots()}. A parallel set would leave
 * grid slots out of the return path, so every Esc would silently eat whatever rested in them.
 * {@code inputSlots()} stays the UNION of every slot holding a player's items; this says what each
 * one does.
 *
 * <p><b>There is deliberately no {@code OUTPUT} constant, not even for symmetry.</b> A crafting
 * menu's result slot is not owned by the player until they take it, and it is NOT in
 * {@code inputSlots()} precisely so {@code returnEverything} cannot hand out a preview nobody paid
 * for on close, death, disconnect or shutdown. An OUTPUT policy would put it back in that set. This
 * axis is about how an owned slot ACCEPTS; the result slot never accepts anything.
 *
 * <p>Every consumer switches over this as an EXHAUSTIVE SWITCH EXPRESSION with no default arm, so
 * a third constant is a compile error rather than a silent fall-through. That is the whole reason
 * it is an enum instead of a boolean -- see NEXT.md's first rule, and the {@code requireGate}
 * switch STATEMENT that covered nothing and compiled.
 */
public enum SlotPolicy {

    /**
     * One whole stack, into an EMPTY slot only. No merge, no partial move, no swap.
     *
     * <p>Named EXCLUSIVE rather than SINGLE on purpose: "single" reads as "one item", and a reader
     * guessing from that name guesses wrong. What it means is that the slot holds one arrival at a
     * time and refuses anything that would combine with, split, or displace what is already there.
     *
     * <p>The DEFAULT, so a menu is conservative until it says otherwise, and so {@code EnchantMenu}
     * -- whose weapon slot is exactly this, and whose behaviour must not move -- overrides nothing.
     */
    EXCLUSIVE,

    /**
     * A vanilla-feeling grid slot: stacks merge, dissimilar items swap, partial moves are fine.
     *
     * <p>Merge and swap are ONE decision, not two. Permitting the merge while refusing the swap
     * would make "place onto an occupied slot" work or do nothing depending on whether the items
     * happen to match, which is a rule a player cannot see.
     */
    STACKING
}
