package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

import java.util.Set;

/**
 * What a single click DOES to one menu input slot.
 *
 * <p>Extracted for the reason {@code EnchantClickIntent} was: {@link MenuRouting} cannot be built
 * in a unit test -- it needs an {@code InventoryClickEvent}, a {@code Player} and a live
 * {@code Inventory} -- but this decision can. {@code InventoryAction} and {@code ClickType} are
 * plain enums that load without a server, the same property {@code VanillaHealPolicyTest} already
 * relies on for {@code RegainReason}. Nothing unconstructable crosses this boundary.
 *
 * <p><b>The payoff is the exhaustive test.</b> Because the decision is a pure function of a Bukkit
 * enum, its test can iterate {@code InventoryAction.values()} and assert every constant lands
 * somewhere NAMED. {@code InventoryAction} is Bukkit's enum, not ours, and it grows in Minecraft
 * drops -- this build carries 25 constants including six {@code *_BUNDLE} additions that did not
 * exist a few versions ago. A new constant now fails a test instead of falling through a whitelist
 * nobody re-read.
 *
 * <p><b>Whitelist, never denylist.</b> {@link #INBOUND} and {@link #OUTBOUND} name what is
 * permitted; everything else reaches {@link #REFUSE} by construction. A denylist here would be
 * {@code ANY_BUT_SHIELD} in a new costume -- see NEXT.md's first rule for the autopsy of the one
 * that shipped.
 */
public enum GridClickIntent {

    /** Nothing moves. The click stays cancelled, so a refusal is a move that never happened. */
    REFUSE,

    /**
     * Un-cancel and let vanilla apply it.
     *
     * <p>Safe ONLY because both endpoints are fixed in the event: the cursor and this one slot.
     * The server picks no destination, which is the property {@link MenuRouting} actually protects
     * -- the same reasoning that makes an enumerated drag safe. Contrast
     * {@code MOVE_TO_OTHER_INVENTORY}, where the server scans a whole inventory for a slot, and
     * the number key, where it performs a two-way swap.
     */
    PERMIT,

    /** Performed by us: top the resting stack up from the cursor, as much as fits. */
    MERGE_ALL,

    /** Performed by us: move exactly one item from the cursor onto the resting stack. */
    MERGE_ONE,

    /** Performed by us: exchange the cursor and the resting stack. */
    SWAP;

    /**
     * Putting something IN. Every one of these consults {@code acceptsInput} through the
     * {@code accepted} flag, so no entry path can admit an item another would refuse.
     */
    private static final Set<InventoryAction> INBOUND = Set.of(
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE);

    /**
     * Taking something OUT. Deliberately NOT gated on {@code accepted}: that asks what may come in,
     * and the only rule going the other way is that the item is the player's to take. Same
     * asymmetry {@code MenuRouting.swapWithInput}'s OUT branch already documents.
     */
    private static final Set<InventoryAction> OUTBOUND = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE);

    /**
     * Does the answer for this click actually depend on {@code acceptsInput}?
     *
     * <p><b>Asked because {@code acceptsInput} is not a pure query.</b> {@code EnchantMenu}'s
     * version says a sentence in chat when it refuses, so calling it speculatively would tell a
     * player "That is not one of your weapons" as they took their own weapon back OUT. Today only
     * a LEFT-click PLACE_ALL reaches it; this preserves that exactly for EXCLUSIVE, and widens it
     * to the inbound arms for STACKING.
     *
     * <p>One source of truth with {@link #of}: when this returns false, {@code of} gives the same
     * answer for either value of {@code accepted}, and its test asserts precisely that rather than
     * trusting the two to stay in step.
     */
    public static boolean consultsAcceptance(InventoryAction action, ClickType click,
                                             SlotPolicy policy) {
        return switch (policy) {
            case EXCLUSIVE -> click == ClickType.LEFT && action == InventoryAction.PLACE_ALL;
            case STACKING -> INBOUND.contains(action) || action == InventoryAction.SWAP_WITH_CURSOR;
        };
    }

    /**
     * What this click means for this slot.
     *
     * @param action        what the server resolved the click to.
     * @param click         the button. The EXCLUSIVE arm is LEFT-only, exactly as it is today.
     * @param policy        the slot's policy. Never null.
     * @param restingEmpty  is the slot empty RIGHT NOW? Reliable: {@code InventoryClickEvent} fires
     *                      BEFORE the click applies, so the slot still holds its resting occupant.
     * @param cursorSimilar does the cursor hold something that would stack with the resting item?
     *                      Meaningless when {@code restingEmpty}; pass false.
     * @param accepted      did the menu's {@code acceptsInput} pass for the incoming item? Gates
     *                      every INBOUND arm and no OUTBOUND one.
     */
    public static GridClickIntent of(InventoryAction action, ClickType click, SlotPolicy policy,
                                     boolean restingEmpty, boolean cursorSimilar, boolean accepted) {
        // Exhaustive switch EXPRESSION, no default arm: a third SlotPolicy constant is a compile
        // error here rather than a silent fall-through to whichever arm happened to be last.
        return switch (policy) {
            case EXCLUSIVE -> exclusive(action, click, restingEmpty, accepted);
            case STACKING -> stacking(action, restingEmpty, cursorSimilar, accepted);
        };
    }

    /**
     * Today's rule, unchanged, and it must stay unchanged: one whole stack into an EMPTY slot, or
     * one whole stack out, LEFT-click only.
     *
     * <p>{@code EnchantMenu} is the only consumer and its behaviour must not move. PLACE_ONE and
     * PICKUP_HALF stay refused because a slot holding half a weapon is a state nothing downstream
     * is written for.
     */
    private static GridClickIntent exclusive(InventoryAction action, ClickType click,
                                             boolean restingEmpty, boolean accepted) {
        if (click != ClickType.LEFT) return REFUSE;
        if (action == InventoryAction.PICKUP_ALL) return PERMIT;
        if (action == InventoryAction.PLACE_ALL) {
            return restingEmpty && accepted ? PERMIT : REFUSE;
        }
        return REFUSE;
    }

    /**
     * A vanilla-feeling grid slot.
     *
     * <p>Merge and swap are ONE decision. Permitting the merge and refusing the dissimilar swap
     * would make "place onto an occupied slot" work or do nothing depending on whether the items
     * happen to match -- a rule with no visible form.
     */
    private static GridClickIntent stacking(InventoryAction action, boolean restingEmpty,
                                            boolean cursorSimilar, boolean accepted) {
        if (OUTBOUND.contains(action)) return PERMIT;

        if (INBOUND.contains(action)) {
            if (!accepted) return REFUSE;
            // Into an empty slot there is nothing to combine with, so vanilla's own arithmetic is
            // exactly what we want and both endpoints are fixed.
            if (restingEmpty) return PERMIT;
            if (!cursorSimilar) return REFUSE;   // dissimilar arrives as SWAP_WITH_CURSOR, below
            return action == InventoryAction.PLACE_ONE ? MERGE_ONE : MERGE_ALL;
        }

        if (action == InventoryAction.SWAP_WITH_CURSOR) {
            if (!accepted) return REFUSE;
            // Nothing to exchange with, and a similar pair is a merge rather than a swap. Vanilla
            // does not produce SWAP_WITH_CURSOR for either, so both arms are belt and braces.
            if (restingEmpty || cursorSimilar) return REFUSE;
            return SWAP;
        }

        return REFUSE;
    }
}
