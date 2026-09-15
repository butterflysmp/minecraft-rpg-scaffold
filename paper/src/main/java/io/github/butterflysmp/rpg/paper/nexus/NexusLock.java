package io.github.butterflysmp.rpg.paper.nexus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Would this gesture move the Nexus star out of its locked slot? The whole decision, and nothing
 * that needs a running server.
 *
 * <h2>COORDINATE SPACE -- {@code PlayerInventory} INDEX SPACE, AND NOTHING ELSE</h2>
 *
 * Every slot number reaching this class is an index into {@code PlayerInventory}:
 * <b>0-8 hotbar, 9-35 storage, 36-39 armour, 40 offhand</b>. {@link #LOCKED_SLOT} is 8, the
 * rightmost hotbar slot, and that is unambiguous here.
 *
 * <p><b>IT IS NOT RAW / VIEW SPACE, AND THE NUMBER 8 IS WRONG IN EVERY VIEW.</b> In the player's
 * own inventory screen (top is {@code CRAFTING}, size 5) raw 8 is the <b>BOOTS</b> slot and the
 * locked slot is raw <b>44</b>; with a 54-slot chest open the locked slot is raw <b>89</b>. A set
 * of slots built from raw numbers would therefore refuse clicks on a player's boots while
 * permitting every click on the slot it exists to protect -- wrong in both views, differently.
 *
 * <p>The danger is specifically that the spaces MIX: {@code getHotbarButton()} is already in index
 * space and needs no conversion, while every other slot does. A reader who checks one member of
 * such a set concludes the set is fine. So there is one space, it is named here, and
 * {@code NexusSlots} converts at the boundary with {@code view.getInventory(raw)} +
 * {@code view.convertSlot(raw)} -- the sanctioned pair, which gets the own-inventory
 * armour/offhand layout right where hand-rolled arithmetic does not.
 *
 * <h2>THE DEFAULT IS "DO NOTHING". THIS CLASS BEING NARROW IS ITS WHOLE SAFETY PROPERTY</h2>
 *
 * The guard that consults this runs at {@code EventPriority.LOWEST}, and a cancel there makes
 * {@code RpgListeners.onMenuClick} -- which is {@code ignoreCancelled = true} -- <b>skip
 * entirely</b>. So a false positive here is not a Nexus bug. It is every button in the crafting
 * menu, the enchant menu and the recipe browser silently ceasing to respond, with nothing logged
 * and nothing red, presenting as "the menus just don't work sometimes".
 *
 * <p><b>Refuse only on a positively identified gesture that could move the star. Everything not
 * named below is PERMITTED and falls through untouched.</b> That is why the rule is a
 * biconditional rather than a denylist, and why {@code NexusLockTest} asserts the PERMIT rows as
 * hard as the REFUSE ones.
 *
 * <h2>WHY THIS IS TESTABLE AT ALL</h2>
 *
 * {@code ClickType} and {@code InventoryAction} are plain enums that load without a server, which
 * is the same thing {@code GridClickIntentTest} relies on. There is no {@code InventoryView}, no
 * {@code Player} and no {@code Inventory} in any signature here, and the project has no MockBukkit.
 */
public final class NexusLock {

    private NexusLock() {}

    /**
     * The one slot the Nexus star lives in, in {@code PlayerInventory} INDEX space.
     *
     * <p>Fixed for this slice by decision, not by discovery: there is no choosing it and no
     * remembering it. It is ONE constant so that moving it later is one edit rather than a sweep.
     *
     * <p><b>EXACTLY ONE TEST ROW PINS THIS VALUE, AND IT IS NOT THE ONE YOU WOULD GUESS.</b> Every
     * other row in {@code NexusLockTest} names this constant symbolically, so a mutation of it
     * moves the code AND the expectation together and bites nothing -- measured, {@code 8 -> 7} left
     * all twenty rows green. {@code theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE}
     * asserts the literal and is the sole guard. <b>This javadoc used to claim the opposite</b>, that
     * the constant was guarded BECAUSE the tests referred to it symbolically, which is backwards.
     */
    public static final int LOCKED_SLOT = 8;

    /** The offhand, in the same index space. Reached by F, which is a two-way swap. */
    public static final int OFFHAND_SLOT = 40;

    /**
     * One slot a gesture touches, already converted out of view space by {@code NexusSlots}.
     *
     * @param playerInventory whether this slot is in the PLAYER's inventory rather than in an open
     *                        container or menu. A menu's own slot 8 is not the locked slot, and
     *                        this flag is the only thing that distinguishes them.
     * @param index           the index WITHIN that inventory. {@code -1} where a click landed
     *                        outside the window entirely and names no slot.
     */
    public record Touched(boolean playerInventory, int index) {}

    /**
     * Is this click refused?
     *
     * @param hotbarButton {@code getHotbarButton()}, already in index space. <b>{@code -1} when the
     *                     click was not a hotbar press</b>, and it must never reach the touched set
     *                     -- {@code MenuRouting.hotbarMove} guards the same value the same way.
     * @param cursorIsStar whether the item on the CURSOR is a Nexus star.
     * @param starAt       whether the given player-inventory index currently holds a Nexus star.
     *                     <b>An {@code IntPredicate} rather than a {@code Set}</b>: a set is eager
     *                     by construction, so the signature would make a 41-slot PDC scan --
     *                     deserialising item meta -- the natural implementation, on every click by
     *                     every player in every menu, to answer a question about at most two slots.
     */
    public static boolean refusesClick(ClickType click, InventoryAction action, Touched clicked,
                                       int hotbarButton, boolean cursorIsStar, IntPredicate starAt) {

        // 1. THE TWO GESTURES THAT SEND THE CURSOR TO THE WORLD FLOOR, and the only reason this
        //    method reads InventoryAction at all.
        //
        //    They are named by ACTION because no ClickType identifies them: an outside-the-window
        //    drop arrives as an ordinary LEFT or RIGHT with a raw slot of -999, so a rule keyed on
        //    the click type cannot see it, and the slot it nominally names is not a slot.
        //
        //    Refused whenever the cursor holds a star -- unlike a cursor PLACE, which is permitted
        //    below. The difference is not squeamishness: a place relocates the star inside the
        //    inventory where join convergence can find it, and a drop puts it on the ground where
        //    it despawns or is picked up by somebody else. Refusing the drop wedges nothing,
        //    because a plain left-click into any free slot remains available.
        if (action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR) {
            return cursorIsStar;
        }

        // 2. Double-click collect. NARROW ON PURPOSE, and the breadth is the hazard here rather
        //    than the leniency: refusing every double-click would kill MenuRouting.collectToCursor
        //    for every menu AND ordinary collect in the player's own inventory, everywhere, which
        //    is precisely the plugin-wide outage this class's header is about.
        //
        //    It does not need to be broad. collectToCursor's own javadoc settles it: "isSimilar
        //    compares item meta, so a tagged item never matches a plain stack of the same
        //    Material." A collect draws ONLY from stacks similar to the cursor, so the star is
        //    reachable exactly when the cursor is itself a star -- which also covers the genuine
        //    second-star case without a special arm for it.
        //
        //    Returned before the switch so no slot work happens at all on this path.
        if (click == ClickType.DOUBLE_CLICK) return cursorIsStar;

        // 3. Every other gesture: which PLAYER-inventory slots could it move an item into or out of?
        //
        //    EXHAUSTIVE SWITCH EXPRESSION, NO DEFAULT ARM. ClickType is Bukkit's enum and it grows
        //    in Minecraft drops, so a new constant must be a compile error rather than a silent
        //    fall-through into "permitted" -- the requireGate defect, which was a switch STATEMENT
        //    that covered nothing and compiled.
        Set<Touched> touched = switch (click) {
            // The clicked slot and nothing else. A shift-click's DESTINATION is chosen by vanilla
            // and is not named here, which is sound: refusing at the SOURCE is enough to stop the
            // star leaving, and the star's slot is always the source when the star is what moves.
            case LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, MIDDLE, DROP, CONTROL_DROP, CREATIVE,
                 WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT, UNKNOWN -> Set.of(clicked);

            // THE HOTBAR BUTTON IS A SECOND SLOT, AND IT IS NOT THE CLICKED ONE. Hovering a
            // crafting-grid cell and pressing 9 swaps hotbar index 8 into that cell: the raw slot
            // is in the MENU and the locked slot is never clicked. A rule reading only the clicked
            // slot cannot see this route at all, which is how it was missed the first time.
            //
            // Set.copyOf rather than Set.of, because the two members COLLIDE whenever a player
            // presses the number key for the slot they are already hovering. Set.of throws on a
            // duplicate, and it would throw from inside an event handler.
            case NUMBER_KEY -> hotbarButton < 0 || hotbarButton > 8
                    ? Set.of(clicked)
                    : Set.copyOf(List.of(clicked, new Touched(true, hotbarButton)));

            // F. Enumerated explicitly even though {clicked} happens to cover the case that
            // matters, because an auditor comparing this switch against the gesture list must be
            // able to tell CONSIDERED from FORGOTTEN. The offhand is the other end of the swap.
            case SWAP_OFFHAND -> Set.copyOf(List.of(clicked, new Touched(true, OFFHAND_SLOT)));

            // Unreachable: returned at step 2. Present because the switch is exhaustive and has no
            // default arm, which is the property being bought.
            case DOUBLE_CLICK -> Set.of();
        };

        return touchesTheStar(touched, starAt);
    }

    /**
     * Is this drag refused?
     *
     * <p><b>A drag REFUSES a star on the cursor where a click PERMITS one, and the asymmetry is
     * deliberate.</b> {@code getRawSlots()} enumerates a drag's DESTINATIONS; its source is the
     * cursor and is never in that set, so without {@code cursorIsStar} a star could be dragged into
     * any slot that is neither the locked slot nor already holding a star.
     *
     * <p>It is refused rather than permitted for the same reason the cursor-drop is: refusing
     * traps nobody. A star on the cursor is already outside the locked slot -- a state these
     * refusals cannot have produced -- and a plain left-click into a free slot is still available
     * to put it down, after which join convergence returns it. Refusing every gesture would wedge
     * it on the cursor with no legal destination at all, which is the failure mode a lock is most
     * likely to create and least likely to be blamed for.
     */
    public static boolean refusesDrag(Set<Touched> dragged, boolean cursorIsStar,
                                      IntPredicate starAt) {
        return cursorIsStar || touchesTheStar(dragged, starAt);
    }

    /**
     * The biconditional's second half, shared so the click and drag paths cannot drift apart.
     *
     * <p>Two independent arms, which is why one mutation cannot certify this method: the LOCKED
     * SLOT is refused whether or not it currently holds a star (so nothing can be put INTO it, and
     * a momentarily empty locked slot is still locked), and a star is refused wherever it actually
     * sits (so a stray one is not freely movable while convergence has not yet run).
     */
    private static boolean touchesTheStar(Set<Touched> touched, IntPredicate starAt) {
        for (Touched t : touched) {
            if (!t.playerInventory()) continue;          // a menu's or a chest's slot 8 is not ours
            if (t.index() == LOCKED_SLOT) return true;
            if (t.index() >= 0 && starAt.test(t.index())) return true;
        }
        return false;
    }
}
