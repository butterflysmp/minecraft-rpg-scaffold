package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.Affordability;
import io.github.butterflysmp.rpg.core.anvil.AnvilDecision;
import io.github.butterflysmp.rpg.core.anvil.AnvilReconcile;
import io.github.butterflysmp.rpg.core.anvil.AnvilVerdict;
import io.github.butterflysmp.rpg.core.anvil.Side;
import io.github.butterflysmp.rpg.core.anvil.TransferKey;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.xp.XpCurve;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import io.github.butterflysmp.rpg.paper.weapon.ArmorItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import io.github.butterflysmp.rpg.paper.weapon.GearScoreItems;
import io.github.butterflysmp.rpg.paper.weapon.ShieldItems;
import io.github.butterflysmp.rpg.paper.weapon.ToolItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.BACK_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.CLOSE_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.CONFIRM_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.DONOR_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.INFO_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.OUTPUT_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.TARGET_SLOT;

/**
 * The anvil: sacrifice a higher-scored item of the same type to move its gear score onto a better
 * one.
 *
 * <h2>*** 13b PERFORMS. THE ONE IRREVERSIBLE OPERATION IN THIS FEATURE IS {@link #attemptTransfer}. ***</h2>
 *
 * <b>This javadoc said "IT PERFORMS NOTHING … no confirm button, no XP is spent, and nothing is
 * consumed" through 13a, and 13b falsified every clause.</b> The confirm cell is slot 31, it is
 * armed for three seconds, it spends XP and it destroys the donor.
 *
 * <p><b>The split was the point and it paid.</b> Every layout, refusal and preview row was read
 * while nothing could be lost; this slice adds only what cannot be undone, so its gate is
 * undivided.
 *
 * <h2>*** THE PREVIEW IS A READOUT THAT NEVER BECOMES CARGO -- AND THIS IS THE SLICE THAT COULD HAVE FALSIFIED IT ***</h2>
 *
 * {@link AnvilMenuLayout#OUTPUT_SLOT} holds a copy of what the target WOULD become. 13a promised
 * <i>"13b will not transfer by moving it"</i>, and it does not: {@link #attemptTransfer} mutates the
 * item resting in {@link AnvilMenuLayout#TARGET_SLOT} and clears the donor, and <b>nothing anywhere
 * in this class reads {@code OUTPUT_SLOT}.</b> It is written and never consulted.
 *
 * <p>Three separate things make the copy safe, and all three are needed:
 *
 * <ul>
 *   <li>the slot is <b>not in {@link #inputSlots()}</b>, so the router refuses every place, take,
 *       drag and double-click on it, and {@code Menu.returnEverything} never hands it over;
 *   <li>{@link #shiftClickDispatches} is <b>not overridden</b>, so the base's {@code false} stands
 *       and a shift-click on it is not even dispatched -- unlike {@code CraftingMenu}, which opts
 *       its result slot IN because taking a craft is a real gesture;
 *   <li>{@link #refreshPreview} is the <b>sole writer</b> of the cell.
 * </ul>
 *
 * <h2>*** THE SPEC ASKED FOR AN OUTPUT SLOT WITH A PREVIEW ITEM. THIS IS A PREVIEW PANE. ***</h2>
 *
 * <b>The difference is where the result appears.</b> This screen mutates the target IN PLACE, so a
 * player who confirms finds their upgraded item in slot 20 — <b>the cell they loaded</b> — and not
 * in slot 24, <b>the cell they were watching</b>. The lore names the destination for exactly that
 * reason; it is the half a player needs at the moment they press confirm.
 *
 * <p><b>MAKING 24 TAKEABLE IS A SLICE, NOT A PATCH, AND THE REASONS ARE STRUCTURAL.</b> Recorded
 * here because the spec says otherwise and the next reader will notice the same gap:
 *
 * <ul>
 *   <li><b>{@code returnedSlots()} defaults to {@code inputSlots()}.</b> A takeable result must be
 *       handed back on close, death, disconnect and shutdown <b>without being placeable</b> — so
 *       the two sets stop being equal <b>for the first time in this codebase</b>. Only
 *       {@code NexusVaultMenu} has ever separated them, and it did so in the other direction.
 *   <li><b>The cell becomes two-state: not-cargo before confirm, cargo after.</b> Duplication bugs
 *       live exactly there, and this repo has shipped one already.
 *   <li><b>The transfer stops being an in-place mutation</b> and becomes remove-from-20,
 *       write-to-24, consume, deduct — <b>FOUR writes</b>, where the three-write ordering argument
 *       in {@link #attemptTransfer} was already the first of its kind here, and with a window in
 *       which the result exists in <b>neither</b> slot.
 *   <li><b>Every row of {@code GATE-anvil.md}'s never-cargo section inverts</b> for the post-confirm
 *       state, and R17's six gestures each need a second reading.
 * </ul>
 *
 * <p><b>So it is 13c with its own gate if it is wanted, and a lore line until then.</b> The lore
 * fix costs one string and changes no mechanism; the alternative changes the base class's oldest
 * invariant.
 *
 * <h2>THE REFUSAL IS A BARRIER THAT IS ALL LORE. CLOSE IS A BARRIER WITH NO LORE AT ALL.</h2>
 *
 * <b>This screen carries TWO barriers, and the differentiator is stated because a boot row reads
 * both in one screenshot.</b> {@code MenuIcons.close()} is a barrier whose whole content is its
 * name; a refused preview is a barrier whose name is the instruction and whose lore is the detail.
 * They sit in different cells -- 49 and 24 -- and never in the same one.
 *
 * <p><b>Not {@code MenuIcons.placeholder}.</b> That method's own javadoc: <i>"Reaching for this
 * method is a claim that something is NOT BUILT."</i> A refusal is a WORKING readout that measured
 * a real answer, and the project has already shipped a working readout wearing placeholder clothes
 * once.
 *
 * <h2>SHIFT-CLICK, NUMBER KEYS AND F NEED NOTHING FROM THIS CLASS</h2>
 *
 * It declares {@link #inputSlots()} and nothing else. <b>No new arm in {@code MenuRouting}</b>, and
 * the router still knows nothing about anvils -- which is the reuse test {@code NEXT.md} sets for
 * this screen: <i>"if it needs changes to the base, the base was wrong."</i>
 */
public final class AnvilMenu extends Menu {

    private final WeaponRegistry weapons;
    private final ShieldRegistry shields;
    private final ArmorRegistry armor;
    private final ToolRegistry tools;
    private final AdapterContext adapters;

    /** The way back to the hub, or {@code null} when opened from a world block. */
    private final Supplier<Menu> hub;

    /**
     * Ticks left before the confirm button arms. <b>THE MUTABLE DEADLINE.</b>
     *
     * <p><b>A re-arm MOVES this number; it never starts a second task.</b> One {@link RepeatingTask}
     * runs for the menu's whole life and reads this field.
     *
     * <p><b>INITIALISED AT OPEN TIME</b>, so the first placement's three seconds are measured from
     * that placement and never from an undefined value.
     */
    private int remainingTicks = AnvilButton.ARM_TICKS;

    /**
     * *** THE DECISION THE PLAYER IS LOOKING AT. THE RECONCILE PIN. ***
     *
     * <p>Written by {@link #refreshPreview} and read by {@link #attemptTransfer}, so that what a
     * player pays for is what they were looking at. <b>It is a DECISION, not a rendering</b> -- see
     * {@code AnvilDecision}'s warning about comparing formatted text.
     *
     * <p><b>It also serves as the previous-state memory the arming deadline watches</b>, which is
     * why there is no second field: the repaint compares the new decision's actionability against
     * this one's before replacing it.
     */
    private AnvilDecision displayedDecision;

    /** The last text painted on the button, so an unchanged face is not repainted. */
    private String lastConfirmText = null;

    private TaskHandle armingTask;

    /** Opened from a world block: no way back. */
    public AnvilMenu(Player viewer, WeaponRegistry weapons, ShieldRegistry shields,
                     ArmorRegistry armor, ToolRegistry tools, AdapterContext adapters) {
        this(viewer, weapons, shields, armor, tools, adapters, null);
    }

    /** Opened from the Nexus hub: {@code hub} rebuilds it for the Back button. */
    public AnvilMenu(Player viewer, WeaponRegistry weapons, ShieldRegistry shields,
                     ArmorRegistry armor, ToolRegistry tools, AdapterContext adapters,
                     Supplier<Menu> hub) {
        super(viewer, AnvilMenuLayout.SIZE, MenuIcons.line("Anvil", NamedTextColor.DARK_GRAY));
        this.weapons = weapons;
        this.shields = shields;
        this.armor = armor;
        this.tools = tools;
        this.adapters = adapters;
        this.hub = hub;
        render();
        startArmingTask();
    }

    /**
     * Derived, never stored.
     *
     * <p>A stored origin and a stored breadcrumb can disagree; one field cannot disagree with
     * itself. {@code GrindstoneMenu} and {@code EnchantMenu} carry the same rule, and the
     * CONSTRUCTOR SIGNATURE is what guarantees a block-opened screen has no way back -- there is no
     * constructor that takes a supplier on that path.
     */
    private boolean openedFromNexus() {
        return hub != null;
    }

    // ------------------------------------------------------------------ the two inputs

    @Override
    protected Set<Integer> inputSlots() {
        return AnvilMenuLayout.INPUT_SLOTS;
    }

    /**
     * Only scoreable-looking gear may enter either slot.
     *
     * <p><b>It speaks</b>, because it is asked once for the item on the player's cursor -- unlike a
     * tray walk, which would produce one chat line per cell per repaint.
     *
     * <p><b>It admits TOOLS, and that is deliberate.</b> The filter here is "is this ours at all",
     * not "can this transfer": a tool refused at the door would leave the player guessing whether
     * the item was wrong or the pair was, where letting it in produces a named refusal on the
     * preview cell that says exactly what is wrong. Same call {@code GrindstoneMenu} makes for an
     * unenchanted weapon, and the reason is the same: <b>the readout already distinguishes the two,
     * so the door does not have to.</b>
     */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        if (cursor == null || cursor.getType().isAir()) return false;

        if (resolve(cursor) == null) {
            say("That is not one of your weapons, shields, armor or tools.");
            return false;
        }
        if (cursor.getAmount() != 1) {
            say("One item at a time.");
            return false;
        }
        return true;
    }

    @Override
    protected void onClick(MenuClick click) {
        if (click.slot() == CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (click.slot() == BACK_SLOT && openedFromNexus()) {
            // CLOSE FIRST, THEN HOP -- Menu.open's rule. inputSlots() is non-empty, so the explicit
            // close is what runs onClose and returns both items BEFORE the hub replaces the screen.
            viewer.closeInventory();
            adapters.scheduler().onEntityLater(viewer, () -> hub.get().open(), 1);
            return;
        }
        if (click.slot() == CONFIRM_SLOT) {
            attemptTransfer();
            return;
        }
        if (click.itemMoved()) {
            // THE PAIR CHANGED, SO THE DELAY RE-ARMS. Placement AND removal, with no "removals are
            // safe" exception -- the grindstone considered and refused that exception, and an
            // exception is a boundary this project keeps paying for.
            //
            // rearm() runs SYNCHRONOUSLY and only the repaint is deferred, and the split matters:
            // rearm touches an int and needs no knowledge of the new contents, while a handler
            // reading the slots now would see them as they were, because InventoryClickEvent fires
            // BEFORE the move applies. Moving rearm() into the deferred lambda would let a click in
            // that tick act on a stale deadline.
            rearm();
            adapters.scheduler().onEntityLater(viewer, this::refreshPreview, 1);
        }
    }

    @Override
    protected void onDragPermitted() {
        rearm();
        adapters.scheduler().onEntityLater(viewer, this::refreshPreview, 1);
    }

    /**
     * Hand both items back, and stop the clock.
     *
     * <p><b>This javadoc said "There is no task to cancel and no state to flush -- this screen has
     * no clock, no arming delay and no wallet." 13b falsified every clause of it.</b>
     *
     * <p>{@code returnedSlots()} is still left at its default of {@link #inputSlots()}, which is the
     * two input cells and NOT the preview -- that half survives, and it is what makes a close
     * mid-arm return both items and consume nothing.
     *
     * <p>Idempotent, as {@code Menu.onClose}'s contract requires: {@code TaskHandle.cancel()} is
     * safe after the task has already stopped, and {@code returnEverything} clears each slot before
     * handing it over. Esc, the Close button, death, disconnect and shutdown all arrive here
     * independently.
     */
    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        if (armingTask != null) armingTask.cancel();
        returnEverything();
    }

    // ------------------------------------------------------------------ painting

    /**
     * Painted ONCE, from the constructor.
     *
     * <p><b>It never writes {@link AnvilMenuLayout#TARGET_SLOT}, {@link AnvilMenuLayout#DONOR_SLOT}
     * or {@link AnvilMenuLayout#OUTPUT_SLOT}</b>, and the filler loop cannot reach them because
     * {@link AnvilMenuLayout#FILLER_SLOTS} is built by subtracting all three. Painting filler over
     * an input cell would destroy a player's gear, not hide a button.
     *
     * <p><b>No filler is written to 48 on the block path.</b> The bar owns that cell there, and a
     * pane overwritten a tick later is worse than one never drawn.
     */
    private void render() {
        for (int slot : AnvilMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(CLOSE_SLOT, MenuIcons.close());
        if (openedFromNexus()) {
            getInventory().setItem(BACK_SLOT, MenuIcons.back(Material.ARROW, "the Nexus"));
        }
        getInventory().setItem(INFO_SLOT, MenuIcons.icon(Material.ANVIL,
                MenuIcons.line("Anvil", NamedTextColor.WHITE),
                List.of(MenuIcons.line("Move a gear score onto a better item.",
                                NamedTextColor.GRAY),
                        MenuIcons.line("Same kind only, and the sacrifice must score higher.",
                                NamedTextColor.GRAY),
                        MenuIcons.blank(),
                        MenuIcons.line("Preview only -- nothing is spent or consumed yet.",
                                NamedTextColor.DARK_GRAY))));
        refreshPreview();
    }

    /**
     * *** THIS METHOD TOUCHES {@link AnvilMenuLayout#OUTPUT_SLOT} AND THE BAR CELLS, AND NOTHING
     * ELSE. ***
     *
     * <p><b>DO NOT CALL {@link #render()} FROM HERE.</b> A full re-render would paint filler over
     * the two input cells and destroy the player's gear -- {@code GrindstoneMenu.refreshConfirm}
     * records the same rule against a twenty-one cell tray, and the failure is unrecoverable rather
     * than cosmetic. Both writes below go to <b>named constant sets</b>, never to a loop over a
     * range, because a range is what someone later widens.
     *
     * <p><b>This javadoc said "this repaint is not on a clock … There is nothing here that repeats."
     * 13b falsified it</b> -- a {@link RepeatingTask} now drives it twice a second, so the
     * unchanged-face early return below is no longer optional.
     *
     * <h2>*** IT ALSO RE-ARMS, AND ONLY IN ONE DIRECTION ***</h2>
     *
     * <b>The deadline resets whenever the decision transitions INTO an actionable state, and never
     * on a transition OUT of one.</b> Ben's ruling, closing a path the arm did not otherwise watch:
     * a player looking at a RED cannot-afford button kills a mob, the wallet crosses the cost, and
     * without this the face would become <b>LIME with no countdown</b> -- a click already in flight
     * landing straight on an irreversible action.
     *
     * <p><b>The one-directional part is load-bearing.</b> If a decision getting WORSE also re-armed,
     * then "the wallet drops while the face is LIME" would be caught by the re-arm as well as by
     * re-evaluation, and the two guards would be entangled. <b>A decision that becomes refused needs
     * no arm; it is already refused.</b>
     */
    private void refreshPreview() {
        ItemStack target = getInventory().getItem(TARGET_SLOT);
        ItemStack donor = getInventory().getItem(DONOR_SLOT);

        AnvilDecision current = decide(target, donor);

        // INTO actionable only. See the javadoc: the other direction must NOT reset, or axis 1 and
        // axis 3 stop being separable.
        if (current.actionable() && (displayedDecision == null || !displayedDecision.actionable())) {
            rearm();
        }
        displayedDecision = current;

        AnvilButton.Face face = AnvilButton.faceFor(current, remainingTicks);
        if (face.text().equals(lastConfirmText)) return;
        lastConfirmText = face.text();

        getInventory().setItem(OUTPUT_SLOT, previewIcon(current, face.state(), target));
        getInventory().setItem(CONFIRM_SLOT, confirmIcon(face));
        paintStatus(face.state());
    }

    /**
     * *** THE ONE PRODUCER, AND IT IS CALLED FROM EXACTLY TWO PLACES. ***
     *
     * <p>{@link #refreshPreview} renders what it returns; {@link #attemptTransfer} recomputes with
     * it and reconciles. <b>If the rendered value were built by one path and the recomputed value by
     * another, a mismatch would stop meaning "something changed" and start meaning "the two paths
     * disagree"</b> -- a different fact wearing the same face.
     *
     * <p>The wallet is read here, in POINTS, from the two numbers on the player's screen.
     * {@code Player.getTotalExperience()} is never the input: it does not track spends and drifts
     * from what the client displays.
     */
    private AnvilDecision decide(ItemStack target, ItemStack donor) {
        return AnvilDecision.of(sideOf(target), sideOf(donor),
                XpCurve.totalPoints(viewer.getLevel(), viewer.getExp()));
    }

    // ------------------------------------------------------------------ the arming delay

    private void startArmingTask() {
        EntityTaskTarget target = new EntityTaskTarget(viewer, adapters.scheduler());
        armingTask = RepeatingTask.start(target, AnvilButton.PERIOD_TICKS, () -> {
            if (remainingTicks > 0) {
                remainingTicks = Math.max(0, remainingTicks - AnvilButton.PERIOD_TICKS);
            }
            refreshPreview();
            return true;               // runs for the menu's whole life; onClose cancels it
        }, () -> { });
    }

    /** Move the deadline. Never starts a second task -- see {@link #remainingTicks}. */
    private void rearm() {
        remainingTicks = AnvilButton.ARM_TICKS;
    }

    // ------------------------------------------------------------------ the transfer

    /**
     * *** THE ONLY IRREVERSIBLE OPERATION IN THIS FEATURE. ***
     *
     * <h2>THE GATE IS SPLIT, AND EACH HALF READS THE SOURCE THAT MAKES IT TRUE</h2>
     *
     * <pre>
     *   armed                 the LIVE deadline. remainingTicks is state, never a rendering.
     *   displayed actionable  the DISPLAYED decision. What the player was LOOKING AT.
     * </pre>
     *
     * <p><b>THIS IS A DEVIATION FROM {@code GrindstoneMenu}, AND IT IS DELIBERATE.</b> That screen
     * does {@code Face face = currentFace(); if (!acts(face)) return;} -- it gates on the
     * <b>recomputed</b> face. Copied literally here, a wallet that dropped since the last repaint
     * would recompute to {@code CANNOT_AFFORD}, {@code acts()} would be false, and the click would
     * be <b>silent</b> -- which is exactly the case Ben ruled must SPEAK. The player did nothing
     * wrong; the screen misled them, and they are owed a sentence.
     *
     * <p>So the silence gate asks what the player could SEE, and the reconcile below asks what is
     * actually true. <b>A non-actionable displayed face is a silent no-op</b> -- the button has
     * already said why, and twenty clicks on it produce twenty nothings rather than twenty chat
     * lines.
     *
     * <h2>THE WINDOW THE RECONCILE ACTUALLY COVERS IS SMALL, AND THAT IS NOT A WEAKNESS</h2>
     *
     * The repaint runs every {@link AnvilButton#PERIOD_TICKS} ticks and re-decides, so a wallet
     * change turns the face RED within half a second and every click after that is silent.
     * <b>The reconcile therefore covers exactly the window in which the screen is lying</b> -- up to
     * ten ticks -- which is the only window in which it could. Its unit rows are the primary
     * witness; the boot row is best-effort by construction.
     *
     * <h2>THREE WRITES, ADJACENT, WITH NOTHING FALLIBLE BETWEEN THEM</h2>
     *
     * <pre>
     *   1  stamp the target    the score it was shown as gaining
     *   2  consume the donor
     *   3  deduct the wallet   LAST
     * </pre>
     *
     * <p><b>THE ORDERING IS {@code EnchantMenu}'S, AND THE FILE NEXT DOOR ARGUES THE OPPOSITE.</b>
     * {@code GrindstoneMenu.attemptStrip} is <i>grant-then-strip</i> because it REFUNDS, and its
     * javadoc exists to stop the next reader copying it. <b>The anvil CHARGES</b>, so it takes the
     * charge-last side; copying the grindstone literally would give <i>charge-then-consume</i>,
     * which fails AGAINST the player and invisibly.
     *
     * <p><b>THE THREE RESIDUALS, STATED BECAUSE THERE IS NO PRECEDENT FOR THREE WRITES:</b>
     *
     * <pre>
     *   throw after 1, before 2   target upgraded, donor still there, nothing paid
     *                             -> TOWARDS the player, VISIBLE on both items
     *   throw after 2, before 3   target upgraded, donor gone, nothing paid
     *                             -> TOWARDS the player, VISIBLE on the target
     *   (rejected) deduct first   paid, nothing changed -> AGAINST, visible NOWHERE
     *   (rejected) consume first  donor destroyed, nothing upgraded -> AGAINST, no trace at all
     * </pre>
     *
     * <p><b>No rollback</b>, for the reason both existing spenders record: a compensating write on
     * an error path no test can reach is worse than the residual, and it double-refunds if the throw
     * lands after the wallet was already restored.
     *
     * <p><b>*** AND THE ORDERING ARGUMENT IS ONLY VALID BECAUSE NOTHING HOPS A SCHEDULER BETWEEN THE
     * THREE. ***</b> {@code CraftingMenu.craftFromSuggestion} states it: <i>"Crafting first is
     * correct only because nothing hops a scheduler in between … so nothing can move underneath
     * them."</i> One synchronous click handler, or the argument evaporates.
     */
    private void attemptTransfer() {
        // THE LIVE DEADLINE. Not a rendering -- a click during the lockout is a silent no-op.
        if (remainingTicks > 0) return;
        // WHAT THE PLAYER WAS LOOKING AT. A non-actionable face has already said why.
        if (displayedDecision == null || !displayedDecision.actionable()) return;

        ItemStack target = getInventory().getItem(TARGET_SLOT);
        ItemStack donor = getInventory().getItem(DONOR_SLOT);
        AnvilDecision current = decide(target, donor);

        // *** RE-EVALUATED AT THE MOMENT OF THE CLICK, AND COMPARED AS A DECISION. ***
        // Never as text: two different decisions format identically -- see AnvilDecision.
        AnvilReconcile reconcile = AnvilReconcile.of(displayedDecision, current);
        Optional<String> complaint = reconcile.sentence();
        if (complaint.isPresent()) {
            say(complaint.get());
            rearm();
            refreshPreview();
            return;
        }

        // DEFENCE IN DEPTH, and NOT a duplicate of acceptsInput's identical-looking check. THIS is
        // the operation that mutates and destroys: an amount above one would stamp a whole stack
        // and consume a whole stack. The operation that can destroy guards itself instead of
        // trusting the insert seam to have held.
        if (target == null || target.getAmount() != 1 || donor == null || donor.getAmount() != 1) {
            say("One item at a time.");
            return;
        }
        GearDefinition definition = resolve(target);
        if (definition == null) {
            // REFUSE rather than half-edit. Writing a score we cannot re-render leaves an item
            // whose PDC and whose lore disagree, which is worse than nothing.
            say("That is not one of your weapons, shields, armor or tools.");
            return;
        }
        AnvilVerdict.Ready ready = (AnvilVerdict.Ready) current.verdict();

        // ---- the three writes, adjacent, nothing fallible between them --------------------------

        // 1. THE TARGET, MUTATED IN PLACE. Not remint: that returns a FRESH stack whose reference
        //    must be swapped in, and refreshLore is the compiler-policed door slice 12c added for
        //    exactly this -- a per-item value that is RENDERED has to be re-rendered wherever it is
        //    WRITTEN. The same two lines previewIcon already runs, on the real stack this time.
        target.editMeta(meta -> {
            GearScoreItems.write(meta, ready.newScore(), adapters.keys());
            GearItems.refreshLore(meta, definition, adapters);
        });
        // 2. THE DONOR. *** THE FIRST DELIBERATE DELETION OF A PLAYER'S ITEM IN THIS PACKAGE. ***
        //    MenuSafety has no discarding branch on purpose -- "there is no path through here that
        //    ends with the item gone" -- so there is no door to route this through, and that is
        //    stated rather than left for the next reader to go looking for one.
        //    Clearing the whole slot, not decrementing: the amount is one by two independent
        //    checks, and a clear is idempotent where a decrement re-read is not.
        getInventory().setItem(DONOR_SLOT, null);
        // 3. THE WALLET, LAST. Points in, points out. NOT giveExp(-n), which walks the levels down
        //    through float accumulation inside NMS and bumps the EXPERIENCE scoreboard criterion;
        //    NOT setLevel(getLevel() - levels), which discards the part-full bar. setLevel/setExp
        //    are only how the computed total is written back, through XpCurve's exact inverse of
        //    the read.
        int wallet = XpCurve.totalPoints(viewer.getLevel(), viewer.getExp());
        // max(0, ..) cannot fire -- the affordability check passed and nothing between here and it
        // yields this thread -- and is kept rather than argued away.
        int remaining = Math.max(0, wallet - ready.xpPoints());
        viewer.setLevel(XpCurve.levelFor(remaining));
        viewer.setExp(XpCurve.progressFor(remaining));

        // ---- and the screen catches up ----------------------------------------------------------

        // THE DONOR SLOT IS NOW EMPTY, so the next decision is SlotEmpty and the bar goes GRAY.
        // *** THAT SELF-LIMITS A DOUBLE-APPLY AND IT IS INTENDED, NOT INCIDENTAL. *** A second
        // confirm finds a non-actionable face and is a silent no-op, so the guard costs nothing and
        // needs no flag.
        rearm();
        refreshPreview();
        viewer.updateInventory();

        say("Raised to " + ready.newScore() + " for " + ready.xpPoints() + " XP.");
    }

    /**
     * The confirm button.
     *
     * <p><b>The name is the face's own text</b>, so the button and the bar are two renderings of one
     * state and cannot disagree about which state they are showing.
     *
     * <p>The lore says what the click does and that it cannot be undone. <b>It names no number</b> --
     * the numbers are in the name, which changes with them, and a figure repeated in two places is
     * a figure that can drift.
     */
    private ItemStack confirmIcon(AnvilButton.Face face) {
        return MenuIcons.icon(AnvilFace.dyeFor(face.state()),
                MenuIcons.line(face.text(), AnvilFace.nameColorFor(face.state())),
                List.of(MenuIcons.line("Moves the sacrifice's score onto the item above.",
                                NamedTextColor.GRAY),
                        MenuIcons.line("The sacrifice is consumed.", NamedTextColor.GRAY),
                        MenuIcons.blank(),
                        MenuIcons.line("This cannot be undone.", NamedTextColor.DARK_GRAY)));
    }

    /**
     * Repaint the bar in this state's colour.
     *
     * <p>Iterates {@link AnvilMenuLayout#statusSlots}, which is the bottom row MINUS Close and,
     * from the hub, Back. <b>There is no skip here to forget</b>: the set cannot contain 49, nor 48
     * on the path where a button is drawn there, so this loop has no way to paint over either
     * however often it runs.
     *
     * <p>{@code .clone()} per cell: one {@code ItemStack} written to eight slots would alias.
     */
    private void paintStatus(AnvilFace.State state) {
        ItemStack pane = MenuIcons.pane(AnvilFace.paneFor(state));
        for (int slot : AnvilMenuLayout.statusSlots(openedFromNexus())) {
            getInventory().setItem(slot, pane.clone());
        }
    }

    /**
     * What the preview cell shows.
     *
     * <p><b>READY renders the TARGET as it would become</b>: a copy carrying the donor's score, with
     * its lore rebuilt through {@code GearItems.refreshLore} -- the compiler-policed door slice 12c
     * added, so a fifth gear kind stops the build rather than showing stale lore. <b>The lore is not
     * hand-built</b>; the cost and the warning are appended after the door has run, because they
     * describe the OPERATION rather than the item.
     *
     * <p><b>Everything else renders a BARRIER whose display name is the verdict's own sentence.</b>
     * The sentence lives in {@code core} -- {@code VaultPageGate.refusal}'s precedent -- because
     * this class cannot be constructed without a server and a string written here has no unit test
     * that can read it.
     */
    private ItemStack previewIcon(AnvilDecision decision, AnvilFace.State state, ItemStack target) {
        // THE UNAFFORDABLE FACE IS ITS OWN, and it is a REFUSAL rather than a preview: the pair is
        // legal, so rendering the upgraded item would show a result the player cannot buy and the
        // barrier is what says the problem is the wallet.
        if (decision.affordability() instanceof Affordability.CannotAfford shortfall) {
            return MenuIcons.icon(Material.BARRIER,
                    MenuIcons.line(shortfall.sentence(), AnvilFace.nameColorFor(state)),
                    List.of(MenuIcons.line("Earn the XP and come back -- nothing is spent yet.",
                            NamedTextColor.DARK_GRAY)));
        }
        if (!(decision.verdict() instanceof AnvilVerdict.Ready ready)) {
            return MenuIcons.icon(Material.BARRIER,
                    MenuIcons.line(decision.verdict().sentence(), AnvilFace.nameColorFor(state)),
                    List.of(MenuIcons.line("Nothing is spent until you confirm.",
                            NamedTextColor.DARK_GRAY)));
        }

        GearDefinition definition = resolve(target);
        ItemStack preview = target.clone();
        preview.editMeta(meta -> {
            GearScoreItems.write(meta, ready.newScore(), adapters.keys());
            GearItems.refreshLore(meta, definition, adapters);

            List<Component> lore = new ArrayList<>(
                    meta.lore() == null ? List.of() : meta.lore());
            lore.add(MenuIcons.blank());
            lore.add(MenuIcons.line(ready.sentence(), AnvilFace.nameColorFor(state)));
            lore.add(MenuIcons.line("Costs " + ready.xpPoints() + " XP.", NamedTextColor.GRAY));
            lore.add(MenuIcons.blank());
            // *** THE DESTINATION, AND IT IS NOT DECORATION. ***
            //
            // Ben's spec asked for "an output slot with a preview item", and what this screen
            // actually does is mutate the TARGET in place: the result appears in the slot the
            // player LOADED, not the one they were WATCHING. Saying "you cannot take this" names
            // what the cell is NOT and leaves where the upgrade goes unstated, which is the half a
            // player needs at the moment they press confirm.
            //
            // THE LORE IS THE FIX, NOT THE MECHANISM. Making 24 takeable is a slice, not a patch --
            // see the class javadoc and GATE-anvil.md's never-cargo section.
            lore.add(MenuIcons.line("Preview only -- you cannot take this.",
                    NamedTextColor.DARK_GRAY));
            lore.add(MenuIcons.line("Confirm upgrades the item on the LEFT.",
                    NamedTextColor.DARK_GRAY));
            meta.lore(lore);
        });
        return preview;
    }

    // ------------------------------------------------------------------ gathering

    /**
     * One inventory slot, as {@code core} needs to see it.
     *
     * <h2>THIS IS THE WHOLE OF PAPER'S JOB IN THIS FEATURE</h2>
     *
     * <b>No {@code ItemStack} crosses the seam</b>, and no rule is decided here: this maps a slot to
     * one of {@code Side}'s three named states and hands it over. "The slot has nothing in it" is an
     * inventory fact -- paper's to observe -- and which refusal that produces is core's to name.
     *
     * <h2>*** IT ASKS THE COMPOSED DOOR, AND ASKING ONLY THE KIND-LEVEL RULE IS THE DEFECT ***</h2>
     *
     * {@code GearScore.carriesScore} composes TWO independent refusals: {@code scoreable(kind)} is
     * the kind-level rule (a pickaxe is not gear, whatever its content file says), and
     * {@code declaredUnscored} is the instance-level exception ({@code volley_stone} is a dev
     * fixture whose class is a fighting one). <b>That class's javadoc names the failure exactly: a
     * silent collapse from two refusals to one.</b> Here it would let a declared-unscored item into
     * a transfer and hand its score away.
     *
     * <p><b>An item whose definition cannot be found is UNSCOREABLE, not scored.</b> That is the
     * opposite default from {@code GearRefresher}'s -- which keeps a dangling item's score rather
     * than destroying it -- and the asymmetry is deliberate: <b>refusing to TRANSFER costs a player
     * nothing but a refusal sentence, where refusing to READ would silently lower their average.</b>
     * A content file renamed under an item must not make that item tradeable on a key nobody can
     * derive.
     */
    private Side sideOf(ItemStack item) {
        if (item == null || item.getType().isAir()) return Side.EMPTY;

        GearDefinition definition = resolve(item);
        if (definition == null) return Side.UNSCOREABLE;

        if (!GearScore.carriesScore(GearItems.gearClassOf(definition), declaredUnscored(definition))) {
            return Side.UNSCOREABLE;
        }

        return new Side.Scored(
                TransferKey.of(GearItems.gearClassOf(definition), armorSlotOf(definition)),
                GearScore.orAbsent(GearScoreItems.read(item, adapters.keys())),
                definition.rarity());
    }

    /**
     * Does this item's own definition declare that it carries no score?
     *
     * <p><b>Only a weapon can declare it today</b>, which is why this is an {@code instanceof}
     * rather than an accessor on {@code GearDefinition}: {@code unscored} is a component of
     * {@code WeaponDefinition} alone. {@code GearScoreItems} keeps a private twin of this method for
     * the stamp path; the two are separate because neither module may reach the other's private.
     */
    private static boolean declaredUnscored(GearDefinition definition) {
        return definition instanceof WeaponDefinition weapon && weapon.unscored();
    }

    /**
     * Which limb, for armour, and {@code null} for everything else.
     *
     * <p>{@code TransferKey.of} reads it only on the {@code ARMOR} arm and permits a null on every
     * other, so this does not have to know which kinds care.
     */
    private static ArmorSlot armorSlotOf(GearDefinition definition) {
        return definition instanceof ArmorDefinition piece ? piece.slot() : null;
    }

    /**
     * The gear this item is, or {@code null}.
     *
     * <p><b>Silent</b>, unlike {@code EnchantMenu.resolveGear}: it is called twice per repaint and a
     * version that messaged would chat at the player every time they moved an item.
     */
    private GearDefinition resolve(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        var keys = adapters.keys();
        GearDefinition found = WeaponItems.weaponId(item, keys)
                .<GearDefinition>flatMap(id -> weapons.find(id).map(d -> d)).orElse(null);
        if (found != null) return found;

        found = ShieldItems.shieldId(item, keys)
                .<GearDefinition>flatMap(id -> shields.find(id).map(d -> d)).orElse(null);
        if (found != null) return found;

        found = ArmorItems.armorId(item, keys)
                .<GearDefinition>flatMap(id -> armor.find(id).map(d -> d)).orElse(null);
        if (found != null) return found;

        return ToolItems.toolId(item, keys)
                .<GearDefinition>flatMap(id -> tools.find(id).map(d -> d)).orElse(null);
    }

    private void say(String message) {
        viewer.sendMessage(MenuIcons.line(message, NamedTextColor.GRAY));
    }
}
