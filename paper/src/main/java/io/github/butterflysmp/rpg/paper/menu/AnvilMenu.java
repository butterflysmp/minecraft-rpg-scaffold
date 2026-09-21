package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.AnvilTransfer;
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
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
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
import java.util.Set;
import java.util.function.Supplier;

import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.BACK_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.CLOSE_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.DONOR_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.INFO_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.OUTPUT_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.AnvilMenuLayout.TARGET_SLOT;

/**
 * The anvil: sacrifice a higher-scored item of the same type to move its gear score onto a better
 * one.
 *
 * <h2>*** 13a DISPLAYS AND REFUSES. IT PERFORMS NOTHING. ***</h2>
 *
 * <b>There is no confirm button, no XP is spent, and nothing is consumed.</b> Those are 13b,
 * deliberately -- the only irreversible operation in the feature gets its own undivided gate
 * instead of sharing one with twenty layout rows. 13b's confirm cell is slot 31, which is ordinary
 * filler today; see {@link AnvilMenuLayout}.
 *
 * <h2>*** THE PREVIEW IS A READOUT THAT NEVER BECOMES CARGO ***</h2>
 *
 * {@link AnvilMenuLayout#OUTPUT_SLOT} holds a copy of what the target WOULD become. <b>It is never
 * takeable, and 13b will not transfer by moving it</b> -- 13b mutates the item resting in
 * {@link AnvilMenuLayout#TARGET_SLOT} and consumes the donor. Three separate things make the copy
 * safe, and all three are needed:
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
        if (click.itemMoved()) {
            // DEFERRED A TICK, because InventoryClickEvent fires BEFORE the move applies -- a
            // handler reading the input slots now sees them as they were, not as the player
            // believes them to be. GrindstoneMenu defers for the same reason.
            adapters.scheduler().onEntityLater(viewer, this::refreshPreview, 1);
        }
    }

    @Override
    protected void onDragPermitted() {
        adapters.scheduler().onEntityLater(viewer, this::refreshPreview, 1);
    }

    /**
     * Hand both items back.
     *
     * <p><b>There is no task to cancel and no state to flush</b> -- this screen has no clock, no
     * arming delay and no wallet. {@code returnedSlots()} is left at its default of
     * {@link #inputSlots()}, which is the two input cells and NOT the preview.
     */
    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
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
     * <p><b>Unlike the grindstone's, this repaint is not on a clock</b> -- it runs only after a
     * gesture that could have changed an input slot -- so there is no unchanged-face early return
     * to write. There is nothing here that repeats.
     */
    private void refreshPreview() {
        ItemStack target = getInventory().getItem(TARGET_SLOT);
        ItemStack donor = getInventory().getItem(DONOR_SLOT);

        AnvilVerdict verdict = AnvilTransfer.evaluate(sideOf(target), sideOf(donor));
        AnvilFace.State state = AnvilFace.stateOf(verdict);

        getInventory().setItem(OUTPUT_SLOT, previewIcon(verdict, state, target));
        paintStatus(state);
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
    private ItemStack previewIcon(AnvilVerdict verdict, AnvilFace.State state, ItemStack target) {
        if (!(verdict instanceof AnvilVerdict.Ready ready)) {
            return MenuIcons.icon(Material.BARRIER,
                    MenuIcons.line(verdict.sentence(), AnvilFace.nameColorFor(state)),
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
            lore.add(MenuIcons.line("Preview only -- you cannot take this.",
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
