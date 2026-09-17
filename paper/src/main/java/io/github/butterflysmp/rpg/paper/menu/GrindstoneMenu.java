package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.GrindstoneRefund;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.xp.XpCurve;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import io.github.butterflysmp.rpg.paper.weapon.ArmorItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import io.github.butterflysmp.rpg.paper.weapon.ShieldItems;
import io.github.butterflysmp.rpg.paper.weapon.ToolItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static io.github.butterflysmp.rpg.paper.menu.GrindstoneMenuLayout.BACK_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.GrindstoneMenuLayout.CLOSE_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.GrindstoneMenuLayout.CONFIRM_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.GrindstoneMenuLayout.INFO_SLOT;

/**
 * The grindstone: strip every enchant off up to fourteen items at once, for 35% of list price.
 *
 * <p><b>IN PLACE.</b> The weapons in the tray ARE the weapons stripped, edited where they sit.
 * There is no output slot and no "take your result" step.
 *
 * <h2>THE BUTTON IS THE ENTIRE FEEDBACK SURFACE OF THIS MENU</h2>
 *
 * <b>A click on a button that is not LIME does nothing and says nothing, because the button has
 * already said it.</b> That is the rule the two silences share -- a click during the arming lockout
 * and a click on an empty tray -- and it is why neither is a Row 14 violation: Row 14's case was a
 * button that looked identical whether it worked or not, and {@link GrindstoneButton} never does.
 *
 * <h2>SHIFT-CLICK, NUMBER KEYS AND F NEED NOTHING FROM THIS CLASS</h2>
 *
 * <b>They look like one family and only one of them ever had a question to answer.</b>
 *
 * <ul>
 *   <li><b>Shift-click IN</b> resolves its destination through {@code MenuRouting.firstEmptyInput},
 *       which iterates {@code new TreeSet<>(menu.inputSlots())} -- <b>the first free tray cell in
 *       INDEX ORDER</b>, already. {@code topUpTarget} skips EXCLUSIVE slots outright, and every
 *       minted item is {@code maxStackSize(1)}, so the top-up path can never apply here.
 *   <li><b>Number keys and F</b> act on the <b>HOVERED</b> slot, which is unambiguous at any tray
 *       size: {@code hotbarMove} and {@code offhandMove} reject unless the hovered raw slot is in
 *       {@code inputSlots()}.
 * </ul>
 *
 * <p>So this class declares {@link #inputSlots()} and nothing else. <b>No target resolver, no new
 * arm in {@code MenuRouting}, and the router still knows nothing about grindstones.</b>
 */
public final class GrindstoneMenu extends Menu {

    private final WeaponRegistry weapons;
    private final ShieldRegistry shields;
    private final ArmorRegistry armor;
    private final ToolRegistry tools;
    private final AdapterContext adapters;

    /** The way back to the hub, or {@code null} when opened from a world block. */
    private final Supplier<Menu> hub;

    /**
     * Ticks left before the strip button arms. <b>THE MUTABLE DEADLINE.</b>
     *
     * <p><b>A re-arm MOVES this number; it never starts a second task.</b> One
     * {@link RepeatingTask} runs for the menu's whole life and reads this field -- starting a task
     * per change would leak one per placement, and fourteen placements would leave fourteen tasks
     * all repainting one cell.
     *
     * <p><b>INITIALISED AT OPEN TIME</b>, so the first placement's three seconds are measured from
     * that placement and never from an undefined value. Ben's ruling, closing the hole where a
     * freshly opened menu had no deadline at all.
     */
    private int remainingTicks = GrindstoneButton.ARM_TICKS;

    /** The last text painted on the button, so an unchanged face is not repainted. */
    private String lastConfirmText = null;

    private TaskHandle armingTask;

    /** Opened from a world block: no way back. */
    public GrindstoneMenu(Player viewer, WeaponRegistry weapons, ShieldRegistry shields,
                          ArmorRegistry armor, ToolRegistry tools, AdapterContext adapters) {
        this(viewer, weapons, shields, armor, tools, adapters, null);
    }

    /** Opened from the Nexus hub: {@code hub} rebuilds it for the Back button. */
    public GrindstoneMenu(Player viewer, WeaponRegistry weapons, ShieldRegistry shields,
                          ArmorRegistry armor, ToolRegistry tools, AdapterContext adapters,
                          Supplier<Menu> hub) {
        super(viewer, GrindstoneMenuLayout.SIZE,
                MenuIcons.line("Grindstone", NamedTextColor.DARK_GRAY));
        this.weapons = weapons;
        this.shields = shields;
        this.armor = armor;
        this.tools = tools;
        this.adapters = adapters;
        this.hub = hub;
        render();
        startArmingTask();
    }

    private boolean openedFromNexus() {
        return hub != null;
    }

    // ------------------------------------------------------------------ the tray

    @Override
    protected Set<Integer> inputSlots() {
        return GrindstoneMenuLayout.INPUT_SLOTS;
    }

    /**
     * Only strippable gear may enter the tray.
     *
     * <p><b>A weapon with NO enchants is ALLOWED IN and contributes zero.</b> Refusing a legal item
     * because it happens to be unenchanted is a confusing refusal -- the player would have to guess
     * whether the item was wrong or merely empty -- and the button already distinguishes the two.
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
            // close is what runs onClose and returns the tray BEFORE the hub replaces the screen.
            viewer.closeInventory();
            adapters.scheduler().onEntityLater(viewer, () -> hub.get().open(), 1);
            return;
        }
        if (click.slot() == CONFIRM_SLOT) {
            attemptStrip();
            return;
        }
        if (click.itemMoved()) {
            // THE TRAY CHANGED, SO THE DELAY RE-ARMS. Placement AND removal, with no "removals are
            // safe" exception: an exception is a boundary, and boundaries are what this project
            // keeps paying for. The alternative was considered and refused.
            //
            // The repaint is deferred a tick because InventoryClickEvent fires BEFORE the move
            // applies, so the tray does not yet hold what the player thinks it holds.
            rearm();
            adapters.scheduler().onEntityLater(viewer, this::refreshConfirm, 1);
        }
    }

    @Override
    protected void onDragPermitted() {
        rearm();
        adapters.scheduler().onEntityLater(viewer, this::refreshConfirm, 1);
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        if (armingTask != null) armingTask.cancel();
        returnEverything();
    }

    // ------------------------------------------------------------------ the arming delay

    private void startArmingTask() {
        EntityTaskTarget target = new EntityTaskTarget(viewer, adapters.scheduler());
        armingTask = RepeatingTask.start(target, GrindstoneButton.PERIOD_TICKS, () -> {
            if (remainingTicks > 0) {
                remainingTicks = Math.max(0, remainingTicks - GrindstoneButton.PERIOD_TICKS);
            }
            refreshConfirm();
            return true;               // runs for the menu's whole life; onClose cancels it
        }, () -> { });
    }

    /** Move the deadline. Never starts a second task -- see {@link #remainingTicks}. */
    private void rearm() {
        remainingTicks = GrindstoneButton.ARM_TICKS;
    }

    /**
     * *** THIS METHOD TOUCHES {@link GrindstoneMenuLayout#CONFIRM_SLOT} AND THE SEVEN BAR CELLS,
     * AND NOTHING ELSE. ***
     *
     * <p><b>DO NOT CALL {@link #render()} FROM THE TICK.</b> A full re-render twice a second would
     * paint filler over the tray and <b>DESTROY UP TO TWENTY-ONE ITEMS OF THE PLAYER'S GEAR.</b>
     * This is {@code MUTS5-BACK}'s shape -- a repaint clobbering a cell someone else owns -- with
     * items in place of chrome, and the failure is <b>unrecoverable</b> rather than cosmetic: there
     * is no output slot to take them back from and no undo.
     *
     * <p><b>THE BAR MADE THIS RULE WORSE, NOT SAFER.</b> The tick now has a SECOND writer and the
     * tray has grown from fourteen cells to twenty-one. Both writes go to <b>named constant
     * sets</b> -- {@code CONFIRM_SLOT} and {@code STATUS_SLOTS} -- and never to a loop over a range,
     * because a range is what someone later widens.
     *
     * <p>{@code GrindstoneMenuLayoutTest} asserts those eight cells are disjoint from the tray,
     * which takes the LAYOUT off the list of ways this can go wrong. This method is the other half.
     *
     * <p><b>And it writes only when the STATE or the TEXT would change</b>, so an armed menu is not
     * repainting eight static cells twice a second under the player's clicks. Over one countdown
     * that is three writes across six fires.
     */
    private void refreshConfirm() {
        GrindstoneButton.Face face = currentFace();
        if (face.text().equals(lastConfirmText)) return;
        lastConfirmText = face.text();
        getInventory().setItem(CONFIRM_SLOT, confirmIcon(face));
        paintStatus(face.state());
    }

    /**
     * Repaint the bar in this state's colour.
     *
     * <p>Iterates {@link GrindstoneMenuLayout#STATUS_SLOTS}, which is the bottom row MINUS Back and
     * Close. <b>There is no skip here to forget</b>: the set cannot contain 48 or 49, so this loop
     * has no way to paint over either button however often it runs.
     *
     * <p><b>The colour is a LOOKUP on the button's state, not a second decision.</b> The bar and the
     * button cannot disagree, because only one of them decides anything.
     */
    private void paintStatus(GrindstoneButton.State state) {
        ItemStack pane = MenuIcons.pane(GrindstoneButton.paneFor(state));
        // THE SET FORKS BY ORIGIN: seven cells from the hub, EIGHT from a block, where 48 has no
        // Back button on it and is part of the readout instead. The fork is in the layout, not
        // here -- this loop asks for the right set and paints it.
        for (int slot : GrindstoneMenuLayout.statusSlots(openedFromNexus())) {
            getInventory().setItem(slot, pane.clone());
        }
    }

    private GrindstoneButton.Face currentFace() {
        List<EnchantState> tray = trayStates();
        return GrindstoneButton.faceFor(tray.isEmpty(),
                GrindstoneRefund.strippableCount(tray),
                GrindstoneRefund.points(tray),
                remainingTicks);
    }

    private ItemStack confirmIcon(GrindstoneButton.Face face) {
        return MenuIcons.icon(face.material(),
                MenuIcons.line(face.text(), NamedTextColor.WHITE),
                List.of(MenuIcons.line("Strips EVERY enchant from every item here.",
                                NamedTextColor.GRAY),
                        MenuIcons.line("The roll is kept; the levels are not.", NamedTextColor.GRAY),
                        MenuIcons.blank(),
                        MenuIcons.line("Refunds " + GrindstoneRefund.REFUND_PERCENT
                                + "% of list price.", NamedTextColor.DARK_GRAY)));
    }

    // ------------------------------------------------------------------ the strip

    /**
     * Grant, THEN strip.
     *
     * <h2>*** THE ORDERING IS THE INVERSE OF {@code EnchantMenu}'S, AND ITS COMMENT WILL PUSH YOU THE WRONG WAY ***</h2>
     *
     * {@code EnchantMenu.applyCandidateClick} says <b>the deduction is the last mutation in the
     * method</b>, and gives the reason: if the item write threw, no XP has moved, so the failure
     * grants a free enchant -- towards the player, and visible on the item. Charging first would
     * fail towards a player charged for nothing, which is visible nowhere.
     *
     * <p><b>THAT IS A RULE ABOUT CHARGING, NOT A RULE ABOUT WALLET WRITES GOING LAST.</b> Applied
     * verbatim to a REFUND it yields the unsafe order:
     *
     * <pre>
     *   strip then grant   a throw between them costs the player fourteen weapons' levels AND
     *                      pays nothing. AGAINST the player, and INVISIBLE.
     *   grant then strip   a throw leaves XP paid and some items unstripped. TOWARDS the player,
     *                      and VISIBLE on the items.
     * </pre>
     *
     * <p>Same reasoning, opposite conclusion, because the money moves the other way. Both orderings
     * are written out here because the next person to read that comment and this method together
     * will otherwise conclude one of them is a bug.
     *
     * <p><b>No rollback</b>, for the same reason as there: unreachable by any shipped path, and a
     * compensating write on an error path no test can reach is worse than the residual.
     */
    private void attemptStrip() {
        GrindstoneButton.Face face = currentFace();
        // ONLY LIME ACTS. Gray and yellow are silent no-ops -- the button has already said why.
        if (!GrindstoneButton.acts(face)) return;

        List<EnchantState> tray = trayStates();
        int refund = GrindstoneRefund.points(tray);

        // ONE grant for the whole tray, never fourteen.
        //
        // WALLET-SYMMETRIC, NOT giveExp. The refund is the arithmetic INVERSE of the table's
        // deduction: that computes `wallet - cost` in POINTS and writes it back through XpCurve's
        // exact inverse of the read. Granting through a different mechanism would mean enchanting
        // and immediately stripping does not round-trip -- two halves of one economy on two
        // arithmetics, with an unbounded one-directional error between them. giveExp would also
        // accumulate through float inside NMS and bump the EXPERIENCE scoreboard criterion, which
        // is the objection EnchantMenu records; neither half of it was specific to the sign.
        int wallet = XpCurve.totalPoints(viewer.getLevel(), viewer.getExp());
        // SATURATING, mirroring EnchantMenu's max(0, ..) on the way down. It CANNOT FIRE -- a full
        // tray of fourteen maxed items is about 61k points against a wallet that would need
        // billions -- and is kept rather than argued away.
        int after = Math.max(wallet, wallet + refund);
        viewer.setLevel(XpCurve.levelFor(after));
        viewer.setExp(XpCurve.progressFor(after));

        int changed = stripTray();

        rearm();                 // the tray mutated, so the delay re-arms. No exception for this.
        refreshConfirm();
        viewer.updateInventory();

        say("Stripped " + changed + (changed == 1 ? " item" : " items") + " for " + refund
                + " XP.");
    }

    /**
     * Lock every candidate on every tray item, re-minting each one.
     *
     * <p>The re-mint is not optional: lore is generated at mint, so an item whose enchants changed
     * must be rebuilt or its tooltip keeps advertising what it no longer has.
     *
     * @return how many items actually changed.
     */
    private int stripTray() {
        int changed = 0;
        for (int slot : GrindstoneMenuLayout.INPUT_SLOTS) {
            ItemStack item = getInventory().getItem(slot);
            GearDefinition gear = resolve(item);
            if (gear == null) continue;

            EnchantState before = EnchantItems.read(item, adapters.keys());
            EnchantState after = GrindstoneRefund.stripped(before);
            if (after.equals(before)) continue;

            item.editMeta(meta -> EnchantItems.write(meta, after, adapters.keys()));
            getInventory().setItem(slot, GearItems.remint(item, gear, adapters));
            changed++;
        }
        return changed;
    }

    /** Every tray item's enchant state, in index order. Empty cells contribute nothing. */
    private List<EnchantState> trayStates() {
        List<EnchantState> states = new ArrayList<>();
        for (int slot : GrindstoneMenuLayout.INPUT_SLOTS) {
            ItemStack item = getInventory().getItem(slot);
            if (resolve(item) == null) continue;
            states.add(EnchantItems.read(item, adapters.keys()));
        }
        return states;
    }

    // ------------------------------------------------------------------ chrome

    /**
     * Paint the chrome. <b>NEVER writes a tray cell.</b>
     *
     * <p>Called ONCE, from the constructor. The tray holds the player's items and the only writers
     * of those cells are {@link #stripTray} and {@code returnEverything}; a repaint that touched
     * them would blank or duplicate something the player is looking at -- the same rule
     * {@code EnchantMenu.render} states about its single input slot, over fourteen.
     */
    private void render() {
        for (int slot : GrindstoneMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(CLOSE_SLOT, MenuIcons.close());

        // SLOT 48 IS PAINTED ON BOTH PATHS, BY DIFFERENT THINGS, AND NEITHER IS FILLER ANY MORE.
        //
        //   from the hub     the Back arrow, here
        //   from a block     a BAR CELL, by paintStatus -- Ben's ruling after seeing the screen
        //
        // It used to be filler on the block path, and before that it was painted by nothing at all
        // (an invisible, clickable hole). The filler arm is gone rather than kept as a fallback:
        // with 48 inside STATUS_FROM_BLOCK, a filler write here would be overpainted by the first
        // bar repaint anyway, and a line that is overwritten a tick later is worse than absent --
        // it reads as the thing responsible for the cell.
        if (openedFromNexus()) {
            getInventory().setItem(BACK_SLOT, MenuIcons.back(Material.ARROW, "the Nexus"));
        }
        getInventory().setItem(INFO_SLOT, MenuIcons.icon(Material.GRINDSTONE,
                MenuIcons.line("Grindstone", NamedTextColor.WHITE),
                List.of(MenuIcons.line("Place weapons, shields, armor or tools below.",
                                NamedTextColor.GRAY),
                        MenuIcons.line("Stripping clears every enchant they carry", NamedTextColor.GRAY),
                        // THE FIGURE IS DERIVED, NEVER TYPED. A literal "35%" here is a figure
                        // maintained by delta in the one place a player reads it -- it would go
                        // stale the first time the constant moved, silently, with nothing red.
                        // The button's number already comes from the same constant.
                        MenuIcons.line("and refunds " + GrindstoneRefund.REFUND_PERCENT
                                + "% of the XP they cost.", NamedTextColor.GRAY),
                        MenuIcons.blank(),
                        MenuIcons.line("The slot roll is kept.", NamedTextColor.DARK_GRAY))));

        refreshConfirm();
    }

    /**
     * The gear this item is, or {@code null}.
     *
     * <p><b>SILENT, unlike {@code EnchantMenu.resolveGear}, and deliberately a separate method.</b>
     * That one messages the player when it refuses, which is right for one input slot and wrong for
     * fourteen -- a tray walk would produce fourteen chat lines every time the button repainted.
     * The one place a refusal SHOULD be spoken is {@link #acceptsInput}, which says it once, for
     * the item the player is actually holding.
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
