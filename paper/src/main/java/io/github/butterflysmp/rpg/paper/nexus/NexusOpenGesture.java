package io.github.butterflysmp.rpg.paper.nexus;

import org.bukkit.event.inventory.ClickType;

/**
 * Does this already-refused click ALSO open the Nexus hub?
 *
 * <p>Separated from the listener for the reason {@code GridClickIntent} is: this is the decidable
 * part. {@code ClickType} is a plain enum that loads without a server, and the other three inputs
 * are booleans the caller derives -- so the whole rule is unit-testable and the boot gate only has
 * to confirm the four booleans are computed from the right things.
 *
 * <h2>THIS IS NOT "UNLOCK THE STAR". {@link NexusLock} IS UNTOUCHED.</h2>
 *
 * <b>The star still does not move.</b> There is still no chat line, no sound and no title -- Ben's
 * silence ruling survives whole, and every refusal in {@link NexusLock} refuses exactly what it
 * refused before. <b>What changes is that ONE already-refused gesture gains a SIDE EFFECT.</b>
 *
 * <p>The hook goes where the refusal already lives, and that is the point rather than a
 * convenience: <b>the star's slot is the one cell in a player's inventory where a click is already
 * guaranteed to be intercepted.</b> Nothing new has to reach in.
 *
 * <h2>THE GESTURE SET, AND THE NARROWNESS IS THE POINT</h2>
 *
 * <pre>
 *   OPENS     LEFT or RIGHT click, on the star's OWN slot, with an EMPTY CURSOR,
 *             in the OWN-INVENTORY SCREEN
 *   REFUSES   everything else, silently, exactly as today -- drag, Q, F, number-key,
 *             creative, and any click with an item on the cursor
 * </pre>
 *
 * <p><b>A CLICK WITH A LOADED CURSOR IS AN ATTEMPTED PLACE, NOT A CLICK ON THE STAR.</b> It stays a
 * refusal. Opening a menu while the player holds an item drops them into the hub with a full
 * cursor, which is {@code PLAN-enchant-table-ui}'s row 10c arriving somewhere it was never solved.
 *
 * <p><b>ONLY THE OWN-INVENTORY SCREEN.</b> With a chest or one of our menus open, the click stays a
 * silent refusal. Opening the hub from inside another menu is a nested transition, and our menus
 * with input slots have {@code returnEverything} obligations on close.
 *
 * <h2>CREATIVE IS EXCLUDED BY THE CLICK TYPE, NOT BY A SEPARATE ARM -- AND THAT IS LOAD-BEARING</h2>
 *
 * <b>In creative, a click on an own-inventory slot arrives as {@link ClickType#CREATIVE}</b>, not
 * {@code LEFT} or {@code RIGHT} -- which is the whole basis of the payload guard in
 * {@link NexusLock}'s step 2b. Keying this arm on {@code LEFT}/{@code RIGHT} therefore excludes
 * creative <b>by construction</b>, with no arm to write and none to go stale.
 *
 * <p><b>THAT IS WHY {@code GATE-nexus.md} ROW 6.1 IS NOT SUPERSEDED BY THIS SLICE.</b> 6.1 read
 * <i>"pick up the star -> silent refusal"</i> and its reading is scoped
 * <i>"CONDITIONS: CREATIVE mode, own-inventory screen"</i>. In creative this arm does not fire, so
 * that reading is <b>still true</b>. Row 6 has no SURVIVAL reading at all -- it named no game mode
 * and was booted creative-only -- so what this slice changes is a gesture the file has never read.
 *
 * <p><b>AND THE DAY SOMEONE WIDENS THIS ARM TO INCLUDE {@code CREATIVE}, 6.1 BECOMES FALSE WITH
 * NOTHING GOING RED.</b> A creative player probably does want the hub, so it is a reasonable thing
 * to want -- and it is <b>Ben's question, not this slice's.</b> {@code GATE-nexus.md}'s slice 7
 * carries a CREATIVE CONTROL row for exactly that reason: it makes the widening a visible change
 * rather than a silent contradiction, and it makes 6.1's continued truth <b>observed</b> rather
 * than inferred from an argument about click types.
 */
public final class NexusOpenGesture {

    private NexusOpenGesture() {}

    /**
     * The whole rule, as one expression.
     *
     * @param click               the click type. Only {@code LEFT} and {@code RIGHT} open.
     * @param ownInventoryScreen  is the OPEN SCREEN the player's own inventory? Not "is this slot in
     *                            the player's half" -- that is true of a chest's bottom half too.
     * @param starsOwnSlot        did this click land on the slot the star actually occupies?
     * @param cursorEmpty         is the cursor holding nothing at all?
     */
    public static boolean opensHub(ClickType click, boolean ownInventoryScreen,
                                   boolean starsOwnSlot, boolean cursorEmpty) {
        if (click != ClickType.LEFT && click != ClickType.RIGHT) return false;
        return ownInventoryScreen && starsOwnSlot && cursorEmpty;
    }
}
