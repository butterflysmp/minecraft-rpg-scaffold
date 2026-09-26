package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.core.build.ConvergePlan;
import io.github.butterflysmp.rpg.core.build.LockedSlots;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.build.StoneItems;
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
 *       <b>Its DECISION no longer lives here</b>: since the Ability Stone slice it is
 *       {@code ConvergePlan.of} in core, with {@code ConvergePlanTest}. What remains here is the
 *       execution of that plan, which still needs a real inventory.
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
     * a slot that is not a main inventory cell is not a slot this feature can mean.
     *
     * <p>The bound is the 36 MAIN SLOTS, {@code 0..35} -- hotbar and storage. It was the hotbar
     * alone until the star was allowed anywhere in the inventory; see {@link #MAX_SLOT}. The armour
     * slots and the offhand are still out.
     */
    public static int lockedSlotOf(Player player, ProfileService profiles) {
        // *** A SWITCHED-OFF STAR LOCKS NOTHING. THIS IS THE OTHER HALF OF THE TOGGLE. ***
        //
        // NexusLock refuses the locked slot WHETHER OR NOT IT HOLDS A STAR -- deliberately, so a
        // star that has been lost cannot have its slot taken before the next join restores it. That
        // is right while the feature is ON and exactly wrong while it is off: the player would be
        // left a cell they can neither fill nor use, containing nothing, with no explanation.
        //
        // Gating HERE rather than at each of the lock's call sites, because this is the one place
        // that already answers "which slot is protected" and a second copy of the condition is how
        // the lock and the picker come to disagree.
        if (!profiles.starEnabled(player.getUniqueId())) return NexusLock.NO_LOCKED_SLOT;
        return chosenSlotOf(player, profiles);
    }

    /**
     * The slot the player has CHOSEN, whether or not the star is currently switched on.
     *
     * <h2>NOT THE SAME QUESTION AS {@link #lockedSlotOf}, AND THE DIFFERENCE IS THE TOGGLE</h2>
     *
     * {@code lockedSlotOf} answers <i>"which slot does the lock protect"</i> and is
     * {@code NO_LOCKED_SLOT} while the star is off. <b>This answers "where would it go", which the
     * settings screen and the picker both need while it IS off</b> -- to name the cell in a message,
     * to highlight it, and to know where to put the star back when it is switched on again.
     *
     * <p><b>Using {@code lockedSlotOf} for that would make the toggle un-re-enableable</b>: it would
     * report no slot, the enable arm would refuse as though the profile were unreadable, and the
     * player would be stuck off. That is a real bug this split exists to prevent, not a tidy-up.
     */
    public static int chosenSlotOf(Player player, ProfileService profiles) {
        return profiles.profile(player.getUniqueId())
                .map(PlayerProfile::nexusSlot)
                .map(slot -> validSlotOr(slot, NexusLock.NO_LOCKED_SLOT))
                .orElse(NexusLock.NO_LOCKED_SLOT);
    }

    /**
     * A stored slot if it is a main inventory cell, otherwise the caller's fallback.
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
     * <p>The bound is the 36 MAIN SLOTS, {@code 0..35}, and it lives in {@link #MAX_SLOT} -- ONE
     * constant, which is what let "the star goes anywhere in the inventory" be a one-line change.
     * The armour slots and the offhand are not places this feature can mean.
     */
    public static int validSlotOr(int slot, int fallback) {
        return slot >= 0 && slot <= MAX_SLOT ? slot : fallback;
    }

    /**
     * The highest inventory index the star may live in: <b>35, the last storage cell.</b>
     *
     * <h2>THIS ONE CONSTANT IS THE WHOLE OF "THE NEXUS GOES ANYWHERE"</h2>
     *
     * It was {@code 8} -- the hotbar only -- and widening it to 35 is the entire coordinate change.
     * <b>Nothing else had to move</b>, because slice 4a already put {@link NexusLock} in
     * {@code PlayerInventory} index space, where <b>0-8 is the hotbar and 9-35 is storage</b>. The
     * lock, the convergence and the raw-slot conversion were all written against that space and
     * never against the hotbar.
     *
     * <p><b>36 MAIN SLOTS ONLY -- no armour (36-39), no offhand (40).</b> The star has no business
     * in an armour slot, and the predecessor's picker drew the same line.
     *
     * <h2>NO MIGRATION, AND THAT IS A PROPERTY OF THE WIDENING RATHER THAN A DECISION</h2>
     *
     * <b>Every stored value today is 0-8, and every one of them is still legal in 0-35.</b> A widened
     * bound cannot invalidate anything it previously accepted. <b>Do not write a migration for
     * this</b> -- there is no value that needs rewriting and no version that needs stamping.
     *
     * <p>The reverse would not be true: narrowing this back would strand every player who had chosen
     * a storage slot, and {@link #validSlotOr} would hand them the fallback without saying so.
     */
    static final int MAX_SLOT = 35;

    /**
     * Would this click move EITHER locked item -- the Nexus star or the Ability Stone? Pure translation;
     * it changes nothing. The same {@link NexusLock} decision, asked once per item and OR-ed.
     */
    public static boolean refuses(InventoryClickEvent event, Keys keys, ProfileService profiles) {
        return refusesStar(event, keys, profiles) || refusesStone(event, keys, profiles);
    }

    /** Would this click move the Nexus star? */
    public static boolean refusesStar(InventoryClickEvent event, Keys keys, ProfileService profiles) {
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

    /** Would this click move the Ability Stone? {@link #refusesStar} with the stone's key and slot. */
    public static boolean refusesStone(InventoryClickEvent event, Keys keys, ProfileService profiles) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        return NexusLock.refusesClick(
                event.getClick(),
                event.getAction(),
                touchedOf(event.getView(), event.getRawSlot()),
                event.getHotbarButton(),
                StoneItems.isStone(event.getCursor(), keys),
                stoneLockedSlotOf(player, profiles),
                itemAt(player, item -> StoneItems.isStone(item, keys)));
    }

    /**
     * The stone's lock half: its effective slot while it is switched on, and {@link NexusLock#NO_LOCKED_SLOT}
     * while it is off or the profile is not readable -- {@link #lockedSlotOf}'s shape exactly. With no
     * locked slot the lock still follows the item itself, so a stone is never loose.
     */
    public static int stoneLockedSlotOf(Player player, ProfileService profiles) {
        if (!profiles.stoneEnabled(player.getUniqueId())) return NexusLock.NO_LOCKED_SLOT;
        return stoneChosenSlotOf(player, profiles);
    }

    /**
     * The stone's EFFECTIVE slot, whatever the toggle says -- the picker and Settings read this.
     * {@code LockedSlots.stoneSlot} decides it from the stored value and the star's slot, so it is on the
     * hotbar and never the star's. {@link NexusLock#NO_LOCKED_SLOT} while the profile is unreadable.
     */
    public static int stoneChosenSlotOf(Player player, ProfileService profiles) {
        return profiles.profile(player.getUniqueId())
                .map(NexusSlots::stoneSlotOf)
                .orElse(NexusLock.NO_LOCKED_SLOT);
    }

    /** The stone's effective slot for a loaded profile. The star's slot is its CHOSEN one, toggle or not. */
    public static int stoneSlotOf(PlayerProfile profile) {
        return LockedSlots.stoneSlot(profile.stoneSlotOrNull(),
                validSlotOr(profile.nexusSlot(), NexusLock.DEFAULT_LOCKED_SLOT));
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
                starAt(player, keys))
                || NexusLock.refusesDrag(dragged,
                StoneItems.isStone(event.getOldCursor(), keys),
                stoneLockedSlotOf(player, profiles),
                itemAt(player, item -> StoneItems.isStone(item, keys)));
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
        return itemAt(player, item -> NexusItems.isNexus(item, keys));
    }

    /** {@link #starAt} for any locked item's identity. Lazy for the same reason. */
    public static IntPredicate itemAt(Player player, java.util.function.Predicate<ItemStack> is) {
        PlayerInventory inventory = player.getInventory();
        return index -> index >= 0 && index < inventory.getSize() && is.test(inventory.getItem(index));
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
     * <p><b>{@code MUTWELDDEFAULT} -- replacing the target with {@link NexusLock#DEFAULT_LOCKED_SLOT}
     * outright, so the argument is ignored and every player's star goes to 8 -- USED to leave the whole
     * suite GREEN.</b> That was measured, and it was the reason for the debt below.
     *
     * <h2>THE PLAN/EXECUTE SEAM -- A NAMED DEBT FROM 2026-09-16, PAID IN THE ABILITY STONE SLICE</h2>
     *
     * <b>This javadoc once said "there is nothing to extract". That was wrong</b>, and the operator refuted
     * it with two precedents ({@code CollectPlan.plan} / {@code collectToCursor}, and
     * {@code GridClickIntent.of} / {@code MenuRouting}). The debt was deferred with a trigger: "the next
     * slice that opens this method for any other reason". The Ability Stone opened it (convergence now
     * runs for two items), so it was paid: the writes are {@code ConvergePlan.of}, in core, and
     * {@code ConvergePlanTest.theChosenSlotIsTheTargetNotTheDefault} is what kills {@code MUTWELDDEFAULT}
     * now. The execution in {@link #converge(Player, LockedItem, int)} still needs a real inventory, so
     * {@code GATE-nexus.md}'s slice 4a rows remain its in-play coverage.
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
     * <i>and tells them so</i>.
     *
     * <p>Called on join AND on respawn. Respawn because {@code onQuit} does not run on death and a
     * keepInventory failure, a cursor drop at death, or any future death path that loses the star
     * would otherwise leave the player without one until their next reconnect.
     */
    /**
     * Delete every Nexus star this player is carrying. <b>The other half of switching it off.</b>
     *
     * <h2>REMOVING THE ITEM IS ONLY HALF, AND THE HALF ON ITS OWN IS A BUG REPORT</h2>
     *
     * <b>{@code NexusLock} refuses the locked slot whether or not it holds a star</b> -- its own
     * javadoc says so. So deleting the item while the profile still says the slot is locked leaves
     * the player a <b>permanently unusable empty cell</b> with nothing on screen to explain it.
     * <b>The caller must write {@code starEnabled = false} too</b>, which is what takes the lock
     * off; {@code SettingsMenu.setStar} does both and says why.
     *
     * <p><b>Deleting a star is not destroying the player's property</b> -- the same argument
     * {@link #converge} makes about surplus stars. It is ours, plugin-minted, worth nothing, and
     * re-minted free the moment they switch it back on. <b>Nothing else in the inventory is
     * touched</b>, which is what separates this from a clear.
     */
    public static void removeStars(Player player, Keys keys) {
        removeAll(player, LockedItem.star(keys));
    }

    /** {@link #removeStars} for any locked item: every copy, anywhere in the inventory, and nothing else. */
    public static void removeAll(Player player, LockedItem item) {
        PlayerInventory inventory = player.getInventory();
        // THE WHOLE INVENTORY, not just the locked slot. A star can sit anywhere -- carried from a
        // previous slot choice, or duplicated by a path converge has not run since -- and leaving
        // one behind would let a player open the hub from an item the setting says is gone.
        for (int index = 0; index < inventory.getSize(); index++) {
            if (item.is().test(inventory.getItem(index))) inventory.setItem(index, null);
        }
    }

    public static void converge(Player player, Keys keys, int lockedSlot) {
        converge(player, LockedItem.star(keys), lockedSlot);
    }

    /**
     * {@link #converge(Player, Keys, int)} for any locked item. <b>The decision is
     * {@code ConvergePlan.of}, in core; this only executes it</b> -- the plan/execute split this
     * javadoc used to name as debt, taken when the Ability Stone opened the method (PLAN-build-system.md
     * section 1.3). The rules are the star's, unchanged: surplus deleted keeping the lowest index, a
     * single copy at the target is a no-op, the survivor is lifted rather than re-minted, and the
     * displaced occupant is handed back through {@code MenuSafety.give}.
     *
     * <p><b>TWO ITEMS, AND THEIR ORDER.</b> Callers converge the star FIRST, then the stone.
     * {@code LockedSlots} keeps their targets distinct, so neither converge can displace the other.
     */
    public static void converge(Player player, LockedItem item, int lockedSlot) {
        PlayerInventory inventory = player.getInventory();

        List<Integer> found = new ArrayList<>();
        for (int index = 0; index < inventory.getSize(); index++) {
            if (item.is().test(inventory.getItem(index))) found.add(index);
        }
        ConvergePlan plan = ConvergePlan.of(found, lockedSlot, item.defaultSlot(), item.maxSlot());

        for (int index : plan.clear()) inventory.setItem(index, null);
        if (plan.noop()) return;   // nothing to do

        // Take the existing copy rather than minting a second, so one that has been renamed,
        // re-tagged or otherwise touched is preserved as the one the player has.
        ItemStack held;
        if (plan.source() != ConvergePlan.MINT) {
            held = inventory.getItem(plan.source());
            inventory.setItem(plan.source(), null);   // clear FIRST, so the slot counts as free below
        } else {
            held = item.mint().get();
        }

        // Captured BEFORE the write, or it is gone.
        int target = plan.target();
        ItemStack occupant = inventory.getItem(target);
        inventory.setItem(target, held);

        // MenuSafety.isEmpty is the canonical copy -- absent, AIR, and zero-count husks all mean
        // nothing here, and testing only one of them is how a slot ends up holding an invisible
        // unclickable item. Do not write a fourth copy of it.
        if (!MenuSafety.isEmpty(occupant)) MenuSafety.give(player, occupant);

        player.updateInventory();
    }
}
