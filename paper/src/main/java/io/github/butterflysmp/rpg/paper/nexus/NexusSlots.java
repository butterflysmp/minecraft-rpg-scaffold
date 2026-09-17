package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.menu.MenuSafety;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * The boundary between Bukkit's view space and {@link NexusLock}'s index space, plus the join-time
 * convergence that makes the lock's job possible.
 *
 * <p>Everything that needs a live {@code Player}, {@code InventoryView} or {@code Inventory} lives
 * here, so that {@link NexusLock} needs none of them and has real unit tests. This class is the
 * part that cannot be unit-tested and is therefore kept as thin as it can be made -- the same trade
 * {@code GridClickIntent} makes against {@code MenuRouting}.
 *
 * <h2>THIS CLASS HAS NO TEST FILE, DELIBERATELY. WHAT CARRIES IT IS {@code GATE-nexus.md}</h2>
 *
 * <b>Said here because a class with no test and no explanation reads to the next auditor as an
 * oversight</b>, and they will either write a hollow test for it or assume it is covered elsewhere.
 * It is neither.
 *
 * <p>The one branch that IS unit-testable -- {@link #touchedOf} returning before the view is
 * consulted -- has a row in {@code NexusLockTest}. Nothing else here can have one:
 *
 * <ul>
 *   <li><b>The conversion is not stubbable, and that is measured.</b> Probed against the pinned
 *       paper-api, both {@code InventoryView.convertSlot(int)} and
 *       {@code InventoryView.getInventory(int)} are <b>ABSTRACT, not default</b> -- <i>"abstract
 *       method convertSlot(int) in InventoryView cannot be accessed directly"</i>. The raw-to-index
 *       arithmetic lives in the server's {@code CraftInventoryView}, off the test classpath. A stub
 *       would have to implement it, and the test would then assert its own fake arithmetic.
 *   <li><b>{@link #converge} is the riskiest code in this slice</b> -- it is the only thing here
 *       that MOVES and DESTROYS items -- and it needs a real {@code PlayerInventory}.
 *       {@code new ItemStack(...)} throws without a running server and there is no MockBukkit.
 * </ul>
 *
 * <p><b>{@code GATE-nexus.md} rows 4 and 5 are the whole of this class's coverage.</b> Row 4 reads
 * the conversion in both view shapes; row 5 stages four joins, including a full inventory, against
 * the displacement path. If those rows are ever deleted, this class is unguarded.
 */
public final class NexusSlots {

    private NexusSlots() {}

    // ------------------------------------------------------------------ the verdicts

    /**
     * This player's locked slot, or {@link NexusLock#NO_LOCKED_SLOT} if it cannot be known yet.
     *
     * <h2>THE ONE PLACE A STORED SLOT IS VALIDATED, AND IT MUST BE HERE</h2>
     *
     * {@code PlayerProfile} deliberately does not bound the value -- it is a data carrier and knows
     * nothing about inventories. <b>A slot reaches this method from a JSON file</b>, which may have
     * been hand-edited, written by a future build, or corrupted, so the bound cannot be enforced
     * only at the point a player chooses one.
     *
     * <p><b>Out of range is treated as NOT CHOSEN, not as an error.</b> The alternative -- passing
     * it through -- puts an arbitrary integer into {@code inventory.setItem} inside a join handler,
     * where the throw takes out every listener after it. Falling back is also the honest reading:
     * a slot that is not a hotbar cell is not a slot this feature can mean.
     *
     * <p>The bound is the HOTBAR, {@code 0..8}, not the whole inventory. The star is a held item;
     * putting it in the backpack or on the player's head would be a different feature.
     */
    public static int lockedSlotOf(Player player, ProfileService profiles) {
        return profiles.profile(player.getUniqueId())
                .map(PlayerProfile::nexusSlot)
                .map(slot -> validSlotOr(slot, NexusLock.NO_LOCKED_SLOT))
                .orElse(NexusLock.NO_LOCKED_SLOT);
    }

    /**
     * A stored slot if it is a hotbar cell, otherwise the caller's fallback.
     *
     * <h2>ONE HOME FOR THE BOUND, AND IT WAS WRITTEN TWICE BEFORE THIS EXISTED</h2>
     *
     * {@link #lockedSlotOf} and {@link #converge} both have to reject an out-of-range slot, for
     * different reasons -- the first so the lock does not protect a slot that cannot exist, the
     * second so {@code setItem} does not throw from inside a join handler -- and each carried its
     * own {@code >= 0 && <= 8}. <b>Two copies of a bound is how the two come to disagree</b>, and
     * the disagreement would be invisible: the lock guarding one slot while the star is placed in
     * another is precisely the defect this whole slice exists to prevent, arriving by a second
     * route.
     *
     * <p><b>It is also the only part of this file a unit test can reach.</b> Everything around it
     * needs a live {@code Player}. Extracting it was not tidying: a mutation removing the range
     * check killed NOTHING while it was inline, measured, because no test could get at it.
     *
     * <p>The bound is the HOTBAR, {@code 0..8}. The star is a held item; the backpack, the armour
     * slots and the offhand are not places this feature can mean.
     */
    static int validSlotOr(int slot, int fallback) {
        return slot >= 0 && slot <= 8 ? slot : fallback;
    }

    /** Would this click move the Nexus star? Pure translation; it changes nothing. */
    public static boolean refuses(InventoryClickEvent event, Keys keys, ProfileService profiles) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        return NexusLock.refusesClick(
                event.getClick(),
                event.getAction(),
                touchedOf(event.getView(), event.getRawSlot()),
                event.getHotbarButton(),
                NexusItems.isNexus(event.getCursor(), keys),
                lockedSlotOf(player, profiles),
                starAt(player, keys));
    }

    /**
     * Would this drag move the Nexus star?
     *
     * <p>{@code getOldCursor()} rather than {@code getCursor()}: the old cursor is what is being
     * distributed, and it is the drag's SOURCE. {@code getRawSlots()} names only destinations.
     */
    public static boolean refuses(InventoryDragEvent event, Keys keys, ProfileService profiles) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;

        Set<NexusLock.Touched> dragged = new HashSet<>();
        for (int raw : event.getRawSlots()) dragged.add(touchedOf(event.getView(), raw));

        return NexusLock.refusesDrag(dragged,
                NexusItems.isNexus(event.getOldCursor(), keys),
                lockedSlotOf(player, profiles),
                starAt(player, keys));
    }

    // ------------------------------------------------------------------ the conversion

    /**
     * One raw slot, converted into {@link NexusLock}'s {@code PlayerInventory} index space.
     *
     * <p><b>THIS IS THE ONLY PLACE THE TWO SPACES MEET, AND IT USES THE SANCTIONED PAIR.</b>
     * {@code view.getInventory(raw)} says WHICH inventory a raw slot belongs to and
     * {@code view.convertSlot(raw)} says its index within that inventory. Hand-rolled arithmetic
     * gets the own-inventory screen wrong, where the top is a 5-slot CRAFTING inventory and the
     * armour and offhand slots sit between the crafting grid and the storage rows.
     *
     * <p>A negative raw slot, or one belonging to no inventory, is an outside-the-window click --
     * it names no slot at all. Returned as {@code (false, -1)}, which the lock treats as untouched.
     * The gesture that matters on that path is the cursor DROP, and {@code NexusLock} identifies it
     * by {@code InventoryAction} rather than by slot for exactly this reason.
     */
    public static NexusLock.Touched touchedOf(InventoryView view, int rawSlot) {
        if (rawSlot < 0) return new NexusLock.Touched(false, -1);

        Inventory inventory = view.getInventory(rawSlot);
        if (inventory == null) return new NexusLock.Touched(false, -1);

        // The bottom inventory of ANY view is the player's own. Compared with equals rather than
        // instanceof, matching MenuRouting's own `clicked.equals(getView().getTopInventory())`.
        boolean isPlayers = inventory.equals(view.getBottomInventory());
        return new NexusLock.Touched(isPlayers, view.convertSlot(rawSlot));
    }

    /**
     * Is the OPEN SCREEN the player's own inventory, as opposed to a chest or one of our menus?
     *
     * <h2>THIS PREDICATE DID NOT EXIST IN THE PROJECT BEFORE, AND THE THREE NEAR-MISSES ARE WHY IT
     * HAD TO BE WRITTEN RATHER THAN REUSED</h2>
     *
     * Measured before adding it: {@code InventoryType} appeared <b>nowhere</b> in {@code paper/} or
     * {@code core/}. Three adjacent predicates exist and <b>none of them answers this question</b>:
     *
     * <pre>
     *   touchedOf's `inventory.equals(view.getBottomInventory())`
     *       "is this SLOT in the player's half" -- TRUE for a chest's bottom half too
     *   MenuRouting's `clicked.equals(view.getTopInventory())`
     *       the same shape from the other side, and only reached when a Menu is open
     *   RpgListeners.onMenuClick's `getHolder() instanceof Menu`
     *       distinguishes OUR menus from everything else -- not own-inventory from chest
     * </pre>
     *
     * <p><b>The distinction matters here and nowhere else so far</b>, which is why it was never
     * needed: every other caller asks about a SLOT, and this asks about the SCREEN.
     *
     * <p><b>{@code InventoryType.CRAFTING} is the player's own screen</b> -- the 2x2 grid at the top
     * of it -- and it is the top inventory only when nothing else is open. A chest view's top is
     * {@code CHEST}; ours is {@code CHEST} as well, and is additionally caught by the holder test
     * above.
     *
     * <p><b>Row 6's account is the only other place in this repo that names this distinction</b>,
     * and it names it in prose: 6.4 and 6.5 reproduce in the own-inventory screen and <i>"with a
     * CHEST open they do not reproduce at all"</i>.
     */
    public static boolean isOwnInventoryScreen(InventoryView view) {
        return view != null && view.getTopInventory().getType() == InventoryType.CRAFTING;
    }

    /**
     * Does the given player-inventory index hold a Nexus star?
     *
     * <p>LAZY BY CONSTRUCTION. The lock asks about at most two indices per click, so this is at
     * most two {@code getItem} calls and two PDC reads -- not the 41-slot scan a {@code Set}
     * parameter would have made the natural implementation, on every click by every player.
     */
    public static IntPredicate starAt(Player player, Keys keys) {
        PlayerInventory inventory = player.getInventory();
        return index -> index >= 0 && index < inventory.getSize()
                && NexusItems.isNexus(inventory.getItem(index), keys);
    }

    // ------------------------------------------------------------------ convergence

    /**
     * Assert the invariant: EXACTLY ONE Nexus star, and it is at the player's own locked slot.
     *
     * <p><b>THE TARGET IS AN ARGUMENT, AND THE CALLER MUST KNOW IT BEFORE CALLING.</b> Out of range
     * -- including {@link NexusLock#NO_LOCKED_SLOT} -- falls back to
     * {@link NexusLock#DEFAULT_LOCKED_SLOT} rather than throwing from inside a join handler. That
     * fallback is a LAST RESORT and not the way to handle "not loaded yet": a caller that converges
     * before the profile arrives places the star at the default and then has to move it, which the
     * player sees. {@code RpgListeners.onJoin} waits instead.
     *
     * <p><b>NO UNIT TEST GUARDS THAT THIS METHOD USES {@code lockedSlot} AT ALL, AND THAT IS
     * MEASURED.</b> {@code MUTWELDDEFAULT} -- replacing the target with
     * {@link NexusLock#DEFAULT_LOCKED_SLOT} outright, so the argument is ignored and every player's
     * star goes to 8 -- left the whole suite GREEN.
     *
     * <h2>NAMED DEBT: THE PLAN/EXECUTE SEAM, DEFERRED 2026-09-16 RATHER THAN ABSENT</h2>
     *
     * <b>This javadoc first said "there is nothing to extract". That was wrong, and the operator
     * refuted it with two precedents in this repo</b>:
     *
     * <pre>
     *   CollectPlan.plan(sources, ...) -&gt; List&lt;Draw&gt;   and collectToCursor EXECUTES the plan
     *   GridClickIntent.of(...)        -&gt; an intent    and MenuRouting PERFORMS it
     * </pre>
     *
     * <p><b>This method has the same seam.</b> Given the star indices found, the target slot, and
     * whether the target is occupied, the WRITES are a pure function -- which indices to clear,
     * where the surviving star comes from or whether to mint, whether to displace. Only the
     * execution needs a {@code PlayerInventory}. Splitting it would make {@code MUTWELDDEFAULT}
     * killable and give <b>the only code in the Nexus that destroys items</b> its first unit
     * coverage.
     *
     * <p><b>DEFERRED, NOT DECLINED.</b> Slice 4a already carried a schema bump, a join race, a
     * signature change to the decision class and 108 compile errors of test rewriting; widening it
     * further is the trade this project avoids. <b>The trigger is the next slice that opens this
     * method for any other reason</b> -- at that point the split is nearly free, and this note is
     * what says to take it rather than rediscovering the seam.
     *
     * <p><b>{@code GATE-nexus.md}'s slice 4a rows are the only thing standing between this method
     * and silently ignoring the setting.</b> If those rows are deleted, this is unguarded --
     * stated here rather than in the gate, because the person deleting a row is reading the row and
     * the person breaking this is reading this.
     *
     * <h2>WHY THIS IS NOT "MINT IF ABSENT"</h2>
     *
     * Refusals keep a correct state correct; they cannot repair a wrong one. A star that reaches
     * any other slot -- by an admin {@code /give}, a creative edit, a direct server-side
     * {@code setItem} (which raises no event at all and so cannot be refused), or simply a build
     * that predates this code -- would be welded THERE by the same rule, while the locked slot
     * refuses everything into it. Both halves would then work against the player, and mint-if-absent
     * would never fire because the star is not absent.
     *
     * <p>So this converges on the desired state instead, which is the shape
     * {@code HealthModifierItems.desiredModifiers} already uses here: <i>"whatever an item's
     * departure route -- drop, swap, break, death, /clear -- the slot simply no longer yields an
     * amount next scan. No departure event to miss."</i>
     *
     * <h2>THE GUARANTEE IS: NEVER DESTROYS A PLAYER'S ITEM</h2>
     *
     * Surplus stars ARE deleted, and that is not an exception to it. A second Nexus star is not the
     * player's property -- it is ours, plugin-minted, worth nothing, and re-minted free on the next
     * join. The DISPLACED OCCUPANT of the locked slot is the player's, and it goes through
     * {@code MenuSafety.give}, which finds a free slot, and failing that drops it at their feet
     * <i>and tells them so</i>. Mirrors {@code RpgCommand.grantWeapons} -- "never overwrites a held
     * item".
     *
     * <p>Called on join AND on respawn. Respawn because {@code onQuit} does not run on death and a
     * keepInventory failure, a cursor drop at death, or any future death path that loses the star
     * would otherwise leave the player without one until their next reconnect.
     */
    public static void converge(Player player, Keys keys, int lockedSlot) {
        PlayerInventory inventory = player.getInventory();

        // READ ONCE INTO A LOCAL, AND THE TARGET IS NEVER RE-DERIVED BELOW. With a constant it did
        // not matter; with a per-player value, resolving it twice could split the decision -- the
        // "already correct" test comparing against one slot and the write landing in another, which
        // would move the star every join and displace whatever it found.
        int target = validSlotOr(lockedSlot, NexusLock.DEFAULT_LOCKED_SLOT);

        List<Integer> stars = new ArrayList<>();
        for (int index = 0; index < inventory.getSize(); index++) {
            if (NexusItems.isNexus(inventory.getItem(index), keys)) stars.add(index);
        }

        // Surplus first, so the "already correct" test below cannot pass while a duplicate sits in
        // the backpack. Keep the lowest index and delete the rest; which one survives is arbitrary
        // because they are identical.
        //
        // STILL "LOWEST INDEX", NOT "THE ONE ALREADY IN THE TARGET", AND THAT IS UNCHANGED ON
        // PURPOSE. The survivor is lifted into the target a few lines below whichever one it is, so
        // preferring the target would change which identical item is kept and nothing else. The
        // existing behaviour is documented in GATE-nexus.md and re-deciding it here would be a
        // silent change riding along with this one.
        for (int i = 1; i < stars.size(); i++) inventory.setItem(stars.get(i), null);

        int held = stars.isEmpty() ? -1 : stars.get(0);
        if (held == target && stars.size() == 1) return;   // nothing to do

        // Take the existing star rather than minting a second, so a star that has been renamed,
        // re-tagged or otherwise touched is preserved as the one the player has.
        ItemStack star;
        if (held >= 0) {
            star = inventory.getItem(held);
            inventory.setItem(held, null);      // clear FIRST, so the slot counts as free below
        } else {
            star = NexusItems.mint(keys);
        }

        // Captured BEFORE the write, or it is gone.
        ItemStack occupant = inventory.getItem(target);
        inventory.setItem(target, star);

        // MenuSafety.isEmpty is the canonical copy -- absent, AIR, and zero-count husks all mean
        // nothing here, and testing only one of them is how a slot ends up holding an invisible
        // unclickable item. Do not write a fourth copy of it.
        if (!MenuSafety.isEmpty(occupant)) MenuSafety.give(player, occupant);

        player.updateInventory();
    }
}
