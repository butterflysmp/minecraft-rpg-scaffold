package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.core.vault.VaultCell;
import io.github.butterflysmp.rpg.core.vault.VaultCloseDisposal;
import io.github.butterflysmp.rpg.core.vault.VaultMigrationPlan;
import io.github.butterflysmp.rpg.core.vault.VaultPageGate;
import io.github.butterflysmp.rpg.core.vault.VaultReturnPolicy;
import io.github.butterflysmp.rpg.core.vault.VaultShape;
import io.github.butterflysmp.rpg.core.weapon.PageMath;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.vault.VaultCodec;
import io.github.butterflysmp.rpg.paper.vault.VaultService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.github.butterflysmp.rpg.storage.PlayerVault;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The Nexus Vault: seven pages of thirty-six, and the first menu in this plugin that KEEPS what a
 * player puts in it.
 *
 * <h2>*** THE ONE RULE THIS SCREEN IS BUILT AROUND: HOP ALWAYS, THEN ENCODE ALL 36, THEN WRITE ***</h2>
 *
 * {@code InventoryClickEvent} fires <b>before</b> the click applies, and
 * {@code MenuRouting.inputClick}'s {@code PERMIT} arm un-cancels and returns immediately -- so
 * {@link #onClick} runs one tick EARLY, in both directions:
 *
 * <pre>
 *   put-in    written without the item just placed   the close returns nothing  -> ITEM DESTROYED
 *   take-out  written with the item still resting    the cursor is handed back  -> PLAYER HAS TWO
 * </pre>
 *
 * <b>The duplication arm is the one that survives review</b>, because every gate row is written
 * against loss and a duplicate satisfies all of them.
 *
 * <p>{@code MERGE_ALL}, {@code MERGE_ONE} and {@code SWAP} are performed synchronously by the router
 * and would be correct to write synchronously. <b>They are NOT special-cased</b>: a hop after a
 * synchronous perform still reads the settled state, so one rule covers all five
 * {@code GridClickIntent} results and there is no per-intent table for a later reader to get
 * half-right.
 *
 * <p><b>{@link #onDragPermitted()} is the same defect and the same fix.</b> That hook's own javadoc
 * says the contents have not changed yet.
 *
 * <h2>*** IT RETURNS NOTHING ON CLOSE, WHICH INVERTS THE BASE CLASS'S INVARIANT ***</h2>
 *
 * Every other input menu hands the player's items back. This one does not, because they are already
 * on disk -- {@code VaultService} writes through on every mutation. {@link #returnedSlots()} is that
 * opt-out, and it is <b>conditional</b>: the moment a write fails, this screen goes DEGRADED and
 * answers {@link #inputSlots()} again, so the close hands the page back rather than holding items
 * nothing persisted.
 *
 * <h2>WHAT CANNOT BE UNIT-TESTED HERE, AND WHERE IT WENT INSTEAD</h2>
 *
 * {@code Menu} cannot be constructed without a server, so everything in this class is boot-gate-only.
 * The decisions were therefore pushed out to where they are testable at the 2-second loop:
 * {@code VaultPageGate} (thresholds), {@code NexusVaultLayout} (cells), {@code VaultMigrationPlan}
 * (what the migration moves), {@code VaultWriteFailure} (what the failure says). <b>What is left here
 * is wiring</b>, and the gate rows are what read it.
 */
public final class NexusVaultMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final VaultService vaults;

    /** Back to the hub, or null when the ender chest opened this. */
    private final Supplier<Menu> hub;

    /** The page on screen, 0-based. The player is shown {@code PageMath.displayPage(page)}. */
    private int page;

    /**
     * A write failed, so this screen has stopped being storage.
     *
     * <p>Once set it is never cleared: the vault is poisoned for the session, so there is nothing a
     * later gesture could successfully write. A screen that recovered would be a screen claiming the
     * file is current again.
     */
    private boolean degraded;

    /**
     * A write is already scheduled for this tick.
     *
     * <h2>*** TWO GESTURES IN ONE TICK MUST NOT ISSUE TWO WRITES ***</h2>
     *
     * A fast player produces them, and the second write is not merely wasteful: both tasks run after
     * both gestures have landed, so they encode the SAME 36 cells and the second is a redundant disk
     * write racing the first through the I/O queue. Collapsing them is correct precisely because the
     * write is a whole-page snapshot -- the first task already includes whatever the second gesture
     * did.
     *
     * <p>Cleared inside the scheduled task, so the next tick queues again.
     */
    private boolean writeQueued;

    /**
     * Vault slots on the CURRENT page whose stored text this server could not decode, kept verbatim.
     *
     * <h2>*** WITHOUT THIS, ENCODING ALL 36 FROM THE SCREEN DESTROYS THEM ***</h2>
     *
     * PR 1's promise is that an entry which fails to decode <b>is never discarded</b> -- it stays
     * opaque and is written back verbatim, so a later server or a fixed codec can still recover it.
     * But this screen writes a page by encoding the 36 live cells, and an item that cannot be decoded
     * cannot be rendered into a cell to be re-encoded. <b>It would simply be absent from the
     * snapshot, and the next write would drop it.</b>
     *
     * <p>So those cells are held here, painted with a marker, kept out of {@link #inputSlots()} so
     * nothing can be placed on top of them, and written back from this map.
     */
    private final Map<Integer, String> opaque = new HashMap<>();

    /**
     * Has {@link #onClose} run? <b>Volatile: written on the main thread, read from a write callback.</b>
     *
     * <h2>*** THE DEGRADE MACHINERY ONLY PROTECTS A SCREEN THAT IS STILL OPEN ***</h2>
     *
     * A failure observed while the screen is up degrades it, and the eventual close hands back the
     * difference. <b>A failure that arrives AFTER the close has nothing left to degrade</b> -- the
     * close already returned nothing, because at that moment the write had only been ISSUED and the
     * screen was still healthy.
     *
     * <p>So the unpersisted cells would be in a poisoned cache, never on disk, never handed back,
     * and discarded at {@code /stop} because the shutdown flush deliberately skips a poisoned
     * vault. <b>Destruction, on the one path where the net was absent.</b> This flag is what routes
     * that case to the drop instead.
     */
    private volatile boolean closed;

    /**
     * Has something already taken responsibility for the unpersisted cells?
     *
     * <h2>*** EXACTLY ONE OF TWO PATHS MAY HAND THEM OVER, AND BOTH CAN BE IN FLIGHT ***</h2>
     *
     * The degraded close hands back the difference; the drop arm puts it on the ground. <b>If both
     * ran, every unpersisted stack would exist twice</b> -- the fix for a destruction turned into a
     * duplication, which is the shape this slice has already corrected twice.
     *
     * <p>A plain boolean is not enough: the close runs on the main thread and the drop arm on a
     * region task, and the failure can arrive on either side of the close. {@code compareAndSet} is
     * what makes "first one wins" true rather than likely.
     */
    private final AtomicBoolean unpersistedHandled = new AtomicBoolean();

    /**
     * Where to drop items whose owner may already be gone.
     *
     * <p>Captured on the main thread while the player is demonstrably present -- at every write and
     * again at the close -- because by the time a failed save is observed, {@code viewer.getLocation()}
     * may be a call on an offline player.
     */
    private Location lastKnownLocation;

    public NexusVaultMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                          VaultService vaults) {
        this(viewer, adapters, profiles, vaults, null);
    }

    public NexusVaultMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                          VaultService vaults, Supplier<Menu> hub) {
        super(viewer, NexusVaultLayout.SIZE, MenuIcons.line("Vault", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.vaults = vaults;
        this.hub = hub;
        this.page = 0;

        render();

        // AFTER the first render, deliberately. The migration places real stacks into the page's
        // EMPTY cells and then writes the page through the ordinary path, which encodes all 36 --
        // so the cells the vault already held have to be on screen first, or the write would clear
        // them. It is also why migration has no write path of its own to get wrong.
        migrateIfDue();
    }

    /**
     * The 36 storage cells -- but only when this page can actually be written.
     *
     * <h2>EVERY ARM THAT EMPTIES THIS SET IS A CELL NOTHING CAN BE MOVED INTO OR OUT OF</h2>
     *
     * <pre>
     *   DEGRADED        a write failed; further input is refused
     *   page LOCKED     the level has not opened it
     *   vault UNUSABLE  not loaded, unreadable, or poisoned -- a write would be refused anyway
     * </pre>
     *
     * <b>The empty set is what makes those three states safe by construction</b> rather than by
     * guarding: with no input slots the router performs no moves, {@code handleDrag} permits nothing,
     * and the shift-move, hotbar-move and offhand-move find no destination. Same property the hub
     * relies on.
     *
     * <p>The opaque cells are excluded in every arm: their contents are bytes this server cannot
     * render, and a cell holding a marker must not accept a gesture.
     */
    @Override
    protected Set<Integer> inputSlots() {
        if (degraded || !writable()) return Set.of();
        return storageCells();
    }

    /**
     * *** NOTHING, WHILE THE FILE IS CURRENT. {@link #inputSlots()} AGAIN ONCE IT IS NOT. ***
     *
     * <p>This is the whole reason {@code Menu.returnedSlots} exists, and the conditional is the whole
     * reason it is a method rather than a constant. A vault that returned nothing unconditionally
     * would, on a failed write, be holding items that are in no file and are handed to nobody --
     * the one outcome {@code MenuSafety} exists to make unreachable.
     *
     * <p><b>The cursor is not covered by this and must not be</b>: {@code returnEverything} hands a
     * cursor item back regardless, because an item in flight belongs to the player whatever the menu
     * thinks about its cells.
     */
    @Override
    protected Set<Integer> returnedSlots() {
        // DELEGATED TO core, so both answers are exercised by a unit test rather than read off this
        // method. VaultReturnPolicy's javadoc carries the argument; this class cannot be constructed
        // without a server, so a test here could only assert the method exists.
        return VaultReturnPolicy.returnedSlots(degraded, storageCells(), alreadyOnDisk());
    }

    /**
     * The storage cells whose CURRENT contents are already in the file.
     *
     * <h2>*** COMPARED BY CONTENT, NOT BY SLOT, AND THE BIAS IS DELIBERATE ***</h2>
     *
     * Slot presence alone is not enough: the file can hold item X at cell 5 while the screen holds
     * item Y there -- an unpersisted swap. <b>Handing back nothing for that cell would destroy Y</b>
     * while the player keeps an X they never took.
     *
     * <p>So each live cell is encoded and compared against the persisted text for the same slot.
     * <b>The comparison is biased towards handing back</b>: anything that does not match exactly is
     * treated as unpersisted. If {@code VaultCodec.encode} were ever non-deterministic for one
     * stack, this degrades into the single-cursor duplication the design already accepts -- whereas
     * the opposite bias would destroy an item. <b>Wrong one way costs a duplicate, wrong the other
     * way costs the thing itself.</b>
     *
     * <p>Empty while healthy is never asked for -- {@link #returnedSlots()} short-circuits -- so this
     * only runs on a close that is already handing a page back.
     */
    private Set<Integer> alreadyOnDisk() {
        if (!degraded) return Set.of();

        Map<Integer, String> onDisk = vaults.persistedPage(viewer.getUniqueId(), page);
        Set<Integer> matching = new HashSet<>();

        for (int screenSlot : NexusVaultLayout.STORAGE_SLOTS) {
            int vaultSlot = NexusVaultLayout.storageSlotAt(screenSlot);
            String persisted = onDisk.get(vaultSlot);
            if (persisted == null) continue;               // nothing in the file: hand it back

            String live = VaultCodec.encode(getInventory().getItem(screenSlot));
            if (persisted.equals(live)) matching.add(screenSlot);
        }
        return matching;
    }

    /** A vanilla-feeling grid in the storage cells, and today's conservative rule everywhere else. */
    @Override
    protected SlotPolicy slotPolicy(int slot) {
        return NexusVaultLayout.STORAGE_SLOTS.contains(slot) ? SlotPolicy.STACKING
                : SlotPolicy.EXCLUSIVE;
    }

    /**
     * Anything a player owns may go in. <b>The vault has no opinion about items</b>, which is the
     * difference between storage and the enchant slot.
     *
     * <p>It still refuses while the page cannot be written, as a second line behind
     * {@link #inputSlots()}: if a future change ever widens that set, this arm is what stops an item
     * entering a page that nothing will persist.
     */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return !degraded && writable();
    }

    @Override
    protected void onClick(MenuClick click) {
        // *** THE HOP. THE FIRST BRANCH, BEFORE ANY BUTTON, BECAUSE EVERY BUTTON BELOW RETURNS. ***
        //
        // itemMoved is true for exactly one family: a permitted put-in or take-out, or a merge/swap
        // the router performed. All five GridClickIntent results land here and all five get the same
        // treatment -- see the class javadoc for why the three synchronous ones are not special.
        if (click.itemMoved()) {
            scheduleWrite();
            return;
        }

        if (click.slot() == NexusVaultLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }

        if (click.slot() == NexusVaultLayout.BACK_SLOT && hub != null) {
            // CLOSE FIRST, THEN HOP -- Menu.open's rule for a departing menu that HAS input slots.
            // The close runs returnEverything, which hands back nothing while healthy and the whole
            // page while degraded; either way the screen must be finished with before the next one
            // opens.
            viewer.closeInventory();
            adapters.scheduler().onEntity(viewer, () -> hub.get().open());
            return;
        }

        // NO LEVEL READ HERE. Every branch below hands off to flipTo, which reads it inside its own
        // scheduled task -- a tick later, and that is the reading that decides. A local here would
        // be dead, and a dead local that looks load-bearing is how the next reader passes a stale
        // level back in.

        if (click.slot() == NexusVaultLayout.PREV_SLOT) {
            if (page > 0) flipTo(page - 1);
            return;
        }
        if (click.slot() == NexusVaultLayout.NEXT_SLOT) {
            if (page < VaultShape.PAGE_COUNT - 1) flipTo(page + 1);
            return;
        }

        int target = NexusVaultLayout.pageAt(click.slot());
        if (target >= 0) {
            if (target == page) return;
            flipTo(target);
            return;
        }

        // Filler. Inert, and falling through rather than branching on it: a no-op branch reads as a
        // wired button whose body somebody forgot to write.
    }

    /**
     * A permitted drag is about to land. <b>Same hop, same write.</b>
     *
     * <p>This hook exists because a permitted drag changes the contents and dispatches nothing else,
     * and its javadoc already prescribes the one-tick hop. A drag spreading a stack across the page
     * and then a close, written synchronously, persists the page as it was before the spread -- and
     * the close hands back nothing.
     */
    @Override
    protected void onDragPermitted() {
        scheduleWrite();
    }

    /**
     * Nothing to do while healthy: the page is already on disk.
     *
     * <p><b>{@code returnEverything} is called unconditionally rather than only when degraded</b>,
     * because it is also what returns the CURSOR -- and a cursor item is the player's on every close,
     * healthy or not. {@link #returnedSlots()} is what makes the cell loop empty in the healthy case,
     * so there is one call here and the decision lives in one place.
     *
     * <p>Idempotent, as the base class requires: {@code returnEverything} clears each cell before
     * handing it over, so a second close finds air.
     */
    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // *** THE SYNCHRONOUS FLUSH, AND IT IS THE ONLY THING THAT COVERS A QUIT. ***
        //
        // A gesture schedules its write for the next tick. If the player DISCONNECTS inside that
        // tick, the task never runs at all -- PaperScheduler passes `retired` as null, so Paper
        // drops an entity task whose entity is gone. Measured, not assumed: "if the entity is gone
        // before the delay, do nothing", in that class's own comment.
        //
        // An earlier draft guarded the scheduled task with `viewer.isOnline()` and claimed
        // returnEverything had already handed the items back. THAT WAS FALSE AND THIS FILE
        // CONTRADICTED IT TWO METHODS UP: returnedSlots() is EMPTY while healthy, so
        // returnEverything hands back the cursor and nothing else. Place an item, disconnect inside
        // the tick, and it was written nowhere and returned to nobody.
        //
        // WHY A SYNCHRONOUS WRITE IS CORRECT HERE AND NOWHERE ELSE: the hop exists because
        // InventoryClickEvent fires BEFORE the click applies. A close is not a gesture -- it arrives
        // after the click that preceded it has been fully applied -- so the cells are settled and
        // there is nothing to wait for. This is the one place the rule does not bite.
        //
        // ORDER: write FIRST, then hand back. returnEverything CLEARS the cells it returns, so the
        // reverse would write an empty page over a full one. It only matters while degraded, which
        // is exactly when a mistake here is unrecoverable.
        //
        // *** UNWITNESSED BY THE GATE BLOCK, AND SAID SO RATHER THAN LEFT TO BE ASSUMED. ***
        //
        // The case this exists for -- a permitted gesture, then a DISCONNECT INSIDE THE SAME TICK --
        // cannot be staged by hand: it needs the quit packet to arrive between the click and the
        // next tick, and no row in GATE-nexus.md can produce that reliably. So this path is covered
        // by VaultWiringSignatureTest (the write exists and precedes the return) and BY NOTHING
        // ELSE. It has never been observed working.
        //
        // Kept and flagged, not deleted: the same shape as the nativeArmor guard. A guard nobody has
        // watched fire is worth having and is NOT worth believing in, and the difference between
        // those two is written down here so the next reader does not have to guess which it is.
        lastKnownLocation = viewer.getLocation();

        if (!degraded && VaultPageGate.unlocked(page, viewerLevel()) && !writeCurrentPage()) {
            degrade();
        }

        // *** ON A DISCONNECT, A DEGRADED PAGE IS DROPPED RATHER THAN HANDED BACK. ***
        //
        // returnEverything goes through MenuSafety.give, which is addItem-first and reaches the
        // ground ONLY when the inventory is full. That preference is right for every other caller
        // and four menus depend on it -- IT IS NOT CHANGED HERE.
        //
        // It is wrong on THIS path for one reason: a quit's addItem races the player-data save, so
        // an item pushed in can vanish with no error anywhere. And this is the path where handing
        // back is the LAST copy -- the vault is poisoned, the file does not have these cells, and
        // there is no next session to fix it in. An item on the ground is visible and recoverable;
        // an item written into an inventory that is mid-save is neither.
        //
        // A documented exception on one path, with the reason beside it -- not a change to a shared
        // rule to suit one consumer.
        //
        // DEATH IS DELIBERATELY NOT INCLUDED, AND IS UNRULED RATHER THAN EXCLUDED. Vanilla death
        // handling drops the inventory at the death location, so addItem there is still
        // recoverable -- but nobody has measured the ordering against a poisoned vault, and
        // claiming it is safe would be asserting something unmeasured. Say so rather than picking.
        // *** THE CLAIM IS ON `degraded` ALONE. THE DISCONNECT ONLY PICKS THE DESTINATION. ***
        //
        // This was an &&-chain with DISCONNECT inside the claim, plus a separate
        // `if (degraded) unpersistedHandled.set(true)` after returnEverything. That was CORRECT --
        // the flag was claimed on every degraded close, and it was set before the `closed` write the
        // drop arm gates on, so the arm could never win the CAS.
        //
        // IT WAS ALSO UNREADABLE, AND A REVIEWER READ IT AS BROKEN. They walked the four paths and
        // reported the degraded-Esc case as leaving the flag free for a later callback to drop cells
        // this close had already handed back. The report was wrong and the reasoning was not: the
        // condition beside the drop named DISCONNECT, and the claim for the other case was eleven
        // lines further down, behind returnEverything.
        //
        // AN INVARIANT WHOSE CORRECTNESS RESTS ON THE ORDER OF TWO STATEMENTS AT THE END OF A LONG
        // METHOD IS A LATENT DEFECT WHILE IT IS STILL RIGHT. The next person to reorder them gets no
        // warning, and a reviewer who suspects it has nothing to be shown.
        //
        // So the decision is VaultCloseDisposal, where both halves are one expression and both are
        // exercised by a test -- and the rule it states is the one that matters: WHOEVER DISPOSES OF
        // THE CELLS CLAIMS THEM. returnEverything disposes of them just as surely as a drop does.
        VaultCloseDisposal disposal = VaultCloseDisposal.of(
                degraded, reason == InventoryCloseEvent.Reason.DISCONNECT);

        // NESTED, NOT A THREE-TERM &&-CHAIN. The CAS has a SIDE EFFECT, and burying it mid-chain
        // beside a test that decides something else is how the first version became unreadable.
        // The outer test is "does this close dispose of the cells"; the inner one is "where do they
        // go". Two questions, two lines.
        if (disposal.claimsTheCells() && unpersistedHandled.compareAndSet(false, true)) {
            if (disposal == VaultCloseDisposal.DROP) dropOwedOnDisconnect();
            // HAND_BACK: returnEverything below does it, and the claim is already made.
        }

        returnEverything();

        // LAST, and still last on purpose: the async drop arm takes `closed` as its licence to act,
        // so a failure observed mid-close must not find a half-returned page. The claim above no
        // longer depends on this ordering -- it is made before anything is disposed of -- but the
        // arm's OTHER gate still does.
        closed = true;
    }

    /**
     * Put the owed cells on the ground instead of into an inventory that is about to be saved.
     *
     * <p>Clears each cell as it goes, so {@link #returnEverything} -- which runs immediately after --
     * finds air and hands nothing over a second time. Same clear-then-give ordering that method uses,
     * and for the same reason: the reverse duplicates if anything throws.
     */
    private void dropOwedOnDisconnect() {
        int dropped = 0;
        for (int screenSlot : returnedSlots()) {
            ItemStack item = getInventory().getItem(screenSlot);
            if (MenuSafety.isEmpty(item)) continue;

            getInventory().setItem(screenSlot, null);
            MenuSafety.drop(lastKnownLocation, item);
            dropped++;
        }

        if (dropped > 0) {
            vaults.reportDropped(viewer.getUniqueId(), page, dropped, describe(lastKnownLocation));
        }
    }

    // ------------------------------------------------------------------ the write path

    /**
     * Queue the one write for this tick.
     *
     * <p><b>{@code Scheduler.onEntity} hops a tick</b>, measured in {@code Menu.open}'s javadoc
     * against the pinned {@code paper-api}: {@code EntityScheduler.run} is <i>"schedules a task to
     * execute on the next tick"</i>. So this is the hop, and there is no {@code onEntityLater(..., 1)}
     * variant needed.
     */
    private void scheduleWrite() {
        if (writeQueued) return;
        writeQueued = true;

        adapters.scheduler().onEntity(viewer, () -> {
            writeQueued = false;

            // *** NO isOnline GUARD, AND THE ONE THAT WAS HERE CARRIED A FALSE JUSTIFICATION. ***
            //
            // It read: "the items went with them -- vanilla returns the open screen's contents on
            // disconnect via the close event, which has already run returnEverything". That is
            // wrong twice over. returnedSlots() is EMPTY while healthy, so returnEverything hands
            // back the cursor and NOTHING ELSE; and this task does not even reach the guard on a
            // quit, because Paper drops an entity task whose entity is gone.
            //
            // A FALSE JUSTIFICATION IS WORSE THAN A BARE GUARD: it stops the next reader looking.
            //
            // The quit case is covered by the synchronous flush in onClose. The write below needs
            // no live player -- encoding reads the Inventory, which is still valid -- so if this
            // task ever does run for an offline viewer, writing is the right thing to do.

            // A LOCKED page is never written. Its cells are PANES, not items, and encoding them
            // would store chrome over whatever the page really holds. Input is refused there too, so
            // this arm is unreachable through a gesture -- it exists because a locked page is the one
            // state in which "nothing to write" is the correct answer rather than a problem.
            if (!VaultPageGate.unlocked(page, viewerLevel())) return;

            // *** EVERY OTHER REFUSAL DEGRADES, AND THE FIRST DRAFT OF THIS METHOD RETURNED. ***
            //
            // It tested writable(), which folds "page locked" together with "vault not loaded or
            // POISONED" -- and those need opposite answers. A poisoned vault means the cells on
            // screen are the player's only copy, so returning quietly would leave them unpersisted
            // behind a returnedSlots() that hands back nothing: destruction, silently.
            //
            // The vault can be poisoned by something other than this screen -- a failed write from
            // /rpg vault store, on a page this player is not even looking at -- so this is reachable
            // without this screen ever having failed at anything.
            if (!writeCurrentPage()) degrade();
        });
    }

    /**
     * Snapshot all 36 cells of the current page and write them.
     *
     * @return false if the write was REFUSED synchronously -- the vault is not loaded, unreadable or
     *         poisoned. The caller degrades, because the cells on screen are then the only copy.
     */
    private boolean writeCurrentPage() {
        Map<Integer, VaultCell> cells = new LinkedHashMap<>();

        for (int screenSlot : NexusVaultLayout.STORAGE_SLOTS) {
            int vaultSlot = NexusVaultLayout.storageSlotAt(screenSlot);

            // THE OPAQUE CELLS FIRST, and they are written back VERBATIM. The screen cannot render
            // them, so there is nothing in the inventory to encode -- without this arm every write
            // would drop them, which is the one thing PR 1 promises never happens.
            String kept = opaque.get(vaultSlot);
            if (kept != null) {
                cells.put(vaultSlot, new VaultCell(kept, VaultCell.UNDESCRIBED));
                continue;
            }

            ItemStack item = getInventory().getItem(screenSlot);
            String encoded = VaultCodec.encode(item);
            if (encoded == null) continue;      // an empty cell, which is the normal case

            cells.put(vaultSlot, new VaultCell(encoded, describe(item)));
        }

        // CAPTURED, not read inside the callback. The callback fires on the I/O thread, possibly
        // after a page flip, and it must report the page it actually failed to write.
        lastKnownLocation = viewer.getLocation();
        int written = page;
        return vaults.writePage(viewer.getUniqueId(), written, cells,
                owed -> onWriteFailed(written, owed));
    }

    /**
     * A write completed exceptionally. <b>Arrives on the I/O thread, so it hops first.</b>
     *
     * <p>{@code VaultService} has already poisoned the vault and logged the report naming the page
     * and every item; what is left is the part that touches Bukkit.
     */
    private void onWriteFailed(int failedPage, Set<Integer> unpersisted) {
        // PATH ONE: the screen, if it is still up. Entity-scheduled, so it lands where the viewer
        // is -- and if the viewer is gone the task is DROPPED, which is correct: there is no screen
        // to degrade and nobody to tell. Path two is what covers that case.
        adapters.scheduler().onEntity(viewer, () -> {
            if (closed || !viewer.isOnline()) return;
            degrade();
            viewer.sendMessage(MenuIcons.line(
                    "Vault page " + PageMath.displayPage(failedPage) + " could not be saved."
                            + " CLOSE THIS SCREEN and your items will be handed back to you.",
                    NamedTextColor.RED));
        });

        // PATH TWO: the items, when nothing will ever hand them back.
        //
        // *** REGION-SCHEDULED, NOT ENTITY-SCHEDULED, AND THAT IS THE SAME LESSON AGAIN. ***
        // An entity task for a player who has quit is dropped -- which is exactly what made the
        // close flush necessary. A task keyed to a LOCATION still runs.
        adapters.scheduler().onRegion(lastKnownLocation, () -> {
            // The screen is still open: its close owns these cells and will hand them back.
            if (!closed) return;

            // FIRST ONE WINS. If the close already handed them back -- degraded, difference
            // returned -- this must not put a second copy on the floor.
            if (!unpersistedHandled.compareAndSet(false, true)) return;

            dropUnpersisted(failedPage, unpersisted);
        });
    }

    /**
     * Put the cells that reached nobody on the ground, at the player's last known position.
     *
     * <h2>*** THIS IS THE ARM FOR A FAILURE OBSERVED AFTER THE SCREEN IS GONE ***</h2>
     *
     * The close issued the last write and then removed the thing that would have caught its failure.
     * Everything else in this class assumes a screen is there to degrade; here there is not one, so
     * the only remaining choice is the ground or the bin.
     *
     * <p><b>The items come from THIS MENU'S inventory, which still exists.</b> A closed
     * {@code Inventory} is a live object as long as something references it, and the storage cells
     * still hold what they held -- {@code returnEverything} cleared only {@link #returnedSlots()},
     * which was empty because the screen was healthy at that moment.
     *
     * <p><b>Only the cells the SERVICE said were unpersisted.</b> Not the page: the file keeps its
     * last good copy, and dropping a cell it already has is the full-page duplicator in a different
     * costume.
     */
    private void dropUnpersisted(int failedPage, Set<Integer> unpersisted) {
        int dropped = 0;
        for (int vaultSlot : unpersisted) {
            ItemStack item = getInventory().getItem(NexusVaultLayout.screenSlotFor(vaultSlot));
            if (MenuSafety.isEmpty(item)) continue;

            MenuSafety.drop(lastKnownLocation, item);
            getInventory().setItem(NexusVaultLayout.screenSlotFor(vaultSlot), null);
            dropped++;
        }

        if (dropped == 0) return;

        // SAID IN THE LOG, because the player may be offline and will otherwise find items on the
        // ground with no explanation -- or never find them at all. The SEVERE report from the write
        // already named the page and the cells; this says what was done about them.
        vaults.reportDropped(viewer.getUniqueId(), failedPage, dropped, describe(lastKnownLocation));

        if (viewer.isOnline()) {
            viewer.sendMessage(MenuIcons.line(
                    "Your vault could not be saved. " + dropped + " item(s) were dropped where you"
                            + " were standing rather than lost.", NamedTextColor.RED));
        }
    }

    /** A location an operator can walk to, for the one log line that has to name a place. */
    private static String describe(Location where) {
        if (where == null || where.getWorld() == null) return "an unknown location";
        return where.getWorld().getName() + " " + where.getBlockX() + "," + where.getBlockY()
                + "," + where.getBlockZ();
    }

    /**
     * Stop being storage: refuse further input, and hand the page back on close.
     *
     * <h2>*** IT REPAINTS THE SELECTOR AND THE CHROME AND MUST NOT REPAINT THE STORAGE ***</h2>
     *
     * The first draft called {@link #render()}, which was <b>the bug this whole state exists to
     * prevent.</b> {@code renderStorage} paints the cells from the VAULT -- and the entire premise
     * of being degraded is that the cells on screen are NOT in the vault. Repainting would overwrite
     * the player's only copy with the last good contents, destroying exactly the items the degrade
     * path was added to save.
     *
     * <p>The selector is repainted because the buttons must stop inviting a flip, and
     * {@link #flipTo} refuses one outright while degraded for the same reason.
     */
    private void degrade() {
        if (degraded) return;
        degraded = true;
        renderSelector(viewerLevel());
        renderChrome();
    }

    /**
     * Write the current page, then show the target.
     *
     * <h2>*** A REFUSED WRITE DOES NOT FLIP. THIS IS THE ARM THAT WOULD EAT A PAGE. ***</h2>
     *
     * The flip repaints the storage cells, which <b>discards whatever is in them</b>. That is
     * correct only if they are on disk. If the write is refused, the cells on screen are the
     * player's only copy, so the screen degrades and stays where it is -- and the close then hands
     * them back.
     *
     * <p>One scheduled task doing write-then-flip, rather than a hop of its own: on a flip click
     * nothing has moved, so the cells are already settled, and doing both in one task means no tick
     * exists in which the page index and the painted cells disagree.
     */
    private void flipTo(int target) {
        adapters.scheduler().onEntity(viewer, () -> {
            if (!viewer.isOnline()) return;

            // *** A DEGRADED SCREEN DOES NOT FLIP, AT ALL. ***
            //
            // The flip repaints the storage cells, and while degraded those cells hold the only copy
            // of the player's items. There is nothing to write them to -- the vault is poisoned --
            // so the only safe move is to stay put and let the close hand them back.
            if (degraded) {
                viewer.sendMessage(MenuIcons.line(
                        "This vault stopped saving. CLOSE THIS SCREEN and your items will be handed"
                                + " back to you.", NamedTextColor.RED));
                return;
            }

            // READ INSIDE THE TASK, not captured at the click. A tick separates the two, and a page
            // that unlocked in between should open rather than be refused on a stale reading. It
            // also means the refusal message below quotes the level the player is on NOW.
            int level = viewerLevel();

            // The current page is written before the target is shown, because the repaint discards
            // whatever is in the cells. A LOCKED current page has nothing to write -- its cells are
            // panes -- and every other refusal means the cells are unpersisted, so it degrades.
            if (VaultPageGate.unlocked(page, level) && !writeCurrentPage()) {
                degrade();
                viewer.sendMessage(MenuIcons.line(
                        "Your vault could not be saved, so the page did not change. CLOSE THIS"
                                + " SCREEN and your items will be handed back to you.",
                        NamedTextColor.RED));
                return;
            }

            if (!VaultPageGate.unlocked(target, level)) {
                // SPOKEN, for NexusStationGate.refusal's reason: the button dims its NAME, and a
                // dimmed name lives in the hover tooltip, so an un-hovered locked page is
                // pixel-identical to an open one. Refusing in silence is the "crafting table that
                // does nothing" case.
                viewer.sendMessage(MenuIcons.line(VaultPageGate.refusal(target, level),
                        NamedTextColor.GRAY));
                return;
            }

            page = target;
            render();
        });
    }

    // ------------------------------------------------------------------ the migration

    /**
     * Copy this player's vanilla ender chest into page 1, once, when page 1 opens.
     *
     * <h2>*** THE CHEST IS NOT CLEARED. THAT IS A STANDING RULING, NOT THIS METHOD'S CHOICE. ***</h2>
     *
     * So this is a COPY and for a while the items exist twice. The vanilla chest is unreachable once
     * the block is hijacked, so the copy is not something anybody can spend -- and if the hijack is
     * ever rolled back, the player's things are still where they left them, which a clear-on-migrate
     * would have made unrecoverable.
     *
     * <h2>DEFERRED UNTIL PAGE 1 UNLOCKS, WHICH IS SAFE ONLY BECAUSE OF THAT RULING</h2>
     *
     * A sub-20 player's 27 stacks sit untouched in a container they cannot open until the moment they
     * can reach them. Migrating earlier would put items on a page they cannot see.
     *
     * <h2>THE STAMP IS WRITTEN AFTER THE ITEMS, AND ONLY IF THE WRITE WAS ACCEPTED</h2>
     *
     * Reverse the two and a crash in between stamps a player as migrated with an empty page 1 --
     * their chest full, and nothing will ever copy it again. This order fails the other way: items
     * copied, stamp not written, so the next open copies again into cells that are now OCCUPIED,
     * which {@code VaultMigrationPlan} skips. The failure repeats harmlessly instead of hiding.
     */
    private void migrateIfDue() {
        Optional<PlayerProfile> profile = profiles.profile(viewer.getUniqueId());
        if (profile.isEmpty() || profile.get().vaultMigrated()) return;

        int level = PlayerLevel.levelFor(profile.get().lifetimeXp());
        if (!VaultPageGate.unlocked(0, level)) return;

        // The vault must be readable before anything is copied INTO it: a write would be refused,
        // and a refused migration write with the stamp already set would strand the chest.
        if (!writable()) return;

        Optional<PlayerVault> vault = vaults.vault(viewer.getUniqueId());
        if (vault.isEmpty()) return;

        Inventory chest = viewer.getEnderChest();
        Set<Integer> filled = new HashSet<>();
        for (int slot = 0; slot < chest.getSize() && slot < VaultMigrationPlan.ENDER_CHEST_SLOTS;
                slot++) {
            ItemStack item = chest.getItem(slot);
            if (item != null && !item.getType().isAir()) filled.add(slot);
        }

        Set<Integer> occupied = new HashSet<>(vault.get().page(0).keySet());
        // The cells this server could not decode count as OCCUPIED even though they render as a
        // marker: placing a stack on top of one would write over bytes we promised to keep.
        occupied.addAll(opaque.keySet());

        VaultMigrationPlan.Placement placed = VaultMigrationPlan.plan(occupied, filled);

        for (Map.Entry<Integer, Integer> move : placed.moves().entrySet()) {
            ItemStack item = chest.getItem(move.getKey());
            if (item == null) continue;
            // A CLONE. The chest keeps its own stack -- that is the copy, and handing the same
            // object to two inventories is how one edit changes both.
            getInventory().setItem(NexusVaultLayout.screenSlotFor(move.getValue()), item.clone());
        }

        // WRITTEN EVEN WHEN NOTHING MOVED, so an empty chest still stamps and the migration stops
        // being re-attempted on every open. writeCurrentPage encodes the cells the vault already
        // held as well as the ones just placed, which is why the render above had to come first.
        if (!writeCurrentPage()) {
            degrade();
            return;
        }

        profiles.setVaultMigrated(viewer.getUniqueId());

        if (!placed.moves().isEmpty()) {
            viewer.sendMessage(MenuIcons.line(
                    placed.moves().size() + " item(s) from your ender chest are now on vault page 1."
                            + " The ender chest still holds them too.", NamedTextColor.AQUA));
        }
        if (!placed.skipped().isEmpty()) {
            viewer.sendMessage(MenuIcons.line(
                    placed.skipped().size() + " could not be copied because those cells were already"
                            + " taken. They are still in your ender chest.", NamedTextColor.YELLOW));
        }
    }

    // ------------------------------------------------------------------ painting

    /**
     * Can the page on screen be written at all?
     *
     * <p>Two conditions, and neither is about the screen: the level has opened this page, and the
     * vault is loaded and not poisoned. <b>{@link #degraded} is deliberately NOT part of this</b> --
     * the degraded arms need to distinguish "this page was never writable" from "this page stopped
     * being writable", because only the second means the cells on screen are unpersisted.
     */
    private boolean writable() {
        return VaultPageGate.unlocked(page, viewerLevel())
                && !vaults.unusable(viewer.getUniqueId())
                && vaults.vault(viewer.getUniqueId()).isPresent();
    }

    /** The storage cells that hold a player's item, which is all of them bar the opaque ones. */
    private Set<Integer> storageCells() {
        if (opaque.isEmpty()) return NexusVaultLayout.STORAGE_SLOTS;
        Set<Integer> cells = new HashSet<>(NexusVaultLayout.STORAGE_SLOTS);
        for (int vaultSlot : opaque.keySet()) cells.remove(NexusVaultLayout.screenSlotFor(vaultSlot));
        return Set.copyOf(cells);
    }

    /**
     * This viewer's level, as the vault sees it.
     *
     * <p><b>An unavailable profile reads as level 1, which locks every page</b> -- the hub's ruling,
     * and the conservative direction. The alternative opens all seven for anyone whose profile failed
     * to read, which is indistinguishable from the gate not existing.
     */
    private int viewerLevel() {
        return profiles.profile(viewer.getUniqueId())
                .map(profile -> PlayerLevel.levelFor(profile.lifetimeXp()))
                .orElse(1);
    }

    private void render() {
        for (int slot : NexusVaultLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }

        int level = viewerLevel();
        renderSelector(level);
        renderChrome();
        renderStorage(level);
    }

    private void renderChrome() {
        getInventory().setItem(NexusVaultLayout.CLOSE_SLOT, MenuIcons.close());

        // PAINTED ONLY WHEN THERE IS SOMEWHERE TO GO, which is CraftingMenuLayout.BACK_SLOT's rule.
        // Opened from the ender chest there is no hub in the story, so the cell is filler.
        getInventory().setItem(NexusVaultLayout.BACK_SLOT,
                hub != null ? MenuIcons.back(Material.ARROW, "the Nexus") : MenuIcons.filler());
    }

    private void renderSelector(int level) {
        for (int index = 0; index < VaultShape.PAGE_COUNT; index++) {
            getInventory().setItem(NexusVaultLayout.PAGE_BUTTON_SLOTS[index],
                    pageButton(index, level));
        }

        // HIDDEN AT THE ENDS RATHER THAN DIMMED, which is RecipeBrowserMenu's ruling: a greyed-out
        // button a player can click and get nothing from is the same "did I break it" experience as
        // one that does nothing silently.
        getInventory().setItem(NexusVaultLayout.PREV_SLOT, page > 0
                ? MenuIcons.icon(Material.ARROW,
                        MenuIcons.line("Previous page", NamedTextColor.GRAY),
                        List.of(MenuIcons.line("Page " + PageMath.displayPage(page - 1),
                                NamedTextColor.DARK_GRAY)))
                : MenuIcons.filler());

        getInventory().setItem(NexusVaultLayout.NEXT_SLOT, page < VaultShape.PAGE_COUNT - 1
                ? MenuIcons.icon(Material.ARROW,
                        MenuIcons.line("Next page", NamedTextColor.GRAY),
                        List.of(MenuIcons.line("Page " + PageMath.displayPage(page + 1),
                                NamedTextColor.DARK_GRAY)))
                : MenuIcons.filler());
    }

    /**
     * One page button.
     *
     * <h2>THE SELECTED PAGE IS MARKED BY COLOUR, NEVER BY A GLINT</h2>
     *
     * Lime for the page you are on, white for one you can reach, gray for one you cannot. <b>A
     * glint would be ambiguous the moment a page button ever carried an enchanted item</b>, and the
     * grindstone's ruling already established colour as this project's vocabulary for state.
     */
    private ItemStack pageButton(int index, int level) {
        int shown = PageMath.displayPage(index);

        if (!VaultPageGate.unlocked(index, level)) {
            return MenuIcons.icon(Material.GRAY_STAINED_GLASS_PANE,
                    MenuIcons.line("Page " + shown + " -- locked", NamedTextColor.DARK_GRAY),
                    List.of(MenuIcons.line("Unlocks at level " + VaultPageGate.unlockLevel(index),
                                    NamedTextColor.DARK_GRAY),
                            MenuIcons.line("You are level " + level + ".",
                                    NamedTextColor.DARK_GRAY)));
        }

        if (index == page) {
            return MenuIcons.icon(Material.LIME_STAINED_GLASS_PANE,
                    MenuIcons.line("Page " + shown, NamedTextColor.GREEN),
                    List.of(MenuIcons.line("You are here.", NamedTextColor.DARK_GRAY)));
        }

        return MenuIcons.icon(Material.WHITE_STAINED_GLASS_PANE,
                MenuIcons.line("Page " + shown, NamedTextColor.GRAY),
                List.of(MenuIcons.line("Click to open.", NamedTextColor.DARK_GRAY)));
    }

    /**
     * Paint the page's contents, or say why there are none.
     *
     * <p><b>A LOCKED page is painted with panes and is NEVER WRITTEN</b>, which is the pair that
     * makes it safe. Painting chrome into cells that could later be encoded would store filler over
     * whatever the page really holds -- so {@link #writable()} refuses the write, and
     * {@link #inputSlots()} refuses the gesture.
     */
    private void renderStorage(int level) {
        opaque.clear();

        if (!VaultPageGate.unlocked(page, level)) {
            paintDeadStorage(Material.GRAY_STAINED_GLASS_PANE, "Locked",
                    "Unlocks at level " + VaultPageGate.unlockLevel(page));
            return;
        }

        Optional<PlayerVault> vault = vaults.vault(viewer.getUniqueId());
        if (vault.isEmpty()) {
            // The load is in flight, failed, or poisoned. NOT painted as empty storage: an empty
            // page and an unreadable one look identical, and only one of them is safe to put an
            // item into.
            paintDeadStorage(Material.RED_STAINED_GLASS_PANE, "Vault unavailable",
                    vaults.unusable(viewer.getUniqueId())
                            ? "A write failed. Nothing more will be saved this session."
                            : "Still loading -- try again in a moment.");
            return;
        }

        Map<Integer, String> contents = vault.get().page(page);
        for (int vaultSlot = 0; vaultSlot < VaultShape.SLOTS_PER_PAGE; vaultSlot++) {
            int screenSlot = NexusVaultLayout.screenSlotFor(vaultSlot);
            String stored = contents.get(vaultSlot);

            if (stored == null) {
                getInventory().setItem(screenSlot, null);
                continue;
            }

            Optional<ItemStack> decoded = VaultCodec.decode(stored);
            if (decoded.isPresent()) {
                getInventory().setItem(screenSlot, decoded.get());
                continue;
            }

            // KEPT, NOT DROPPED. The bytes stay in `opaque` and are written back verbatim; the cell
            // shows why it cannot be touched. A later server may be able to read it.
            opaque.put(vaultSlot, stored);
            getInventory().setItem(screenSlot, MenuIcons.icon(Material.BARRIER,
                    MenuIcons.line("Unreadable item", NamedTextColor.RED),
                    List.of(MenuIcons.line("This server cannot read it.", NamedTextColor.DARK_GRAY),
                            MenuIcons.line("It is KEPT, not deleted.", NamedTextColor.DARK_GRAY))));
        }
    }

    /** Fill every storage cell with an inert pane saying why the page holds nothing. */
    private void paintDeadStorage(Material material, String name, String why) {
        ItemStack pane = MenuIcons.icon(material,
                MenuIcons.line(name, NamedTextColor.DARK_GRAY),
                List.of(MenuIcons.line(why, NamedTextColor.DARK_GRAY)));
        for (int slot : NexusVaultLayout.STORAGE_SLOTS) {
            getInventory().setItem(slot, pane.clone());
        }
    }

    /**
     * What to call an item in a failure log, in a form an operator can match against what the player
     * is holding.
     *
     * <p>Material and amount, plus the display name when there is one -- which is what tells a
     * minted Boltor from a stick. The plain-text serializer strips the formatting, because a log line
     * carrying MiniMessage tags is a log line nobody can grep.
     */
    private static String describe(ItemStack item) {
        String base = item.getType() + " x" + item.getAmount();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return base;
        String name = PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        return base + " (" + name + ")";
    }
}
