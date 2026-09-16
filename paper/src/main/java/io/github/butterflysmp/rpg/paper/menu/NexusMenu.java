package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.hud.StatsSheetProjection;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Set;

/**
 * The Nexus hub: the screen the Nexus star opens, and the first one a player sees.
 *
 * <p>Slice 2 is deliberately almost nothing -- a close button and a settings button that does not
 * work yet. What it establishes is the ROUTE: the star in the locked slot opens a screen of ours.
 * Everything the hub will hold hangs off that, and none of it can be built until the route is
 * confirmed in play.
 *
 * <h2>BOTH BUTTONS ARE {@code icon} NOW -- AND THE ARGUMENT IS KEPT, NOT DELETED</h2>
 *
 * <b>This section used to say the torch was a {@code placeholder} and the stats head must not be.
 * Slice 3 landed the head; slice 4b built the settings screen, so the torch GRADUATED and the
 * distinction no longer divides these two buttons.</b>
 *
 * <p>The argument is preserved rather than removed, for {@code MenuIcons.close}'s reason: an
 * argument can outlive the thing it argued for, and deleting it with its instance loses why the
 * instance was ever decided. <b>What follows is still the rule; what changed is that this screen no
 * longer has an example of the left-hand column.</b>
 *
 * <p>The settings button used {@link MenuIcons#placeholder}, whose lore reads <i>"Not implemented
 * yet."</i> That was CORRECT while the settings screen was <b>genuinely not built</b>: there was no
 * behaviour behind the torch, nothing to read off it, and nothing a click could do. <b>It is
 * correct no longer, and leaving it would be the defect below with the notice and the feature
 * inverted.</b>
 *
 * <p><b>The stats head is the opposite case and uses {@link MenuIcons#icon}.</b> It carries REAL
 * lore -- a working readout of live figures, from {@code StatsSheetProjection} -- and only its
 * CLICK is unbuilt. Rendering it with {@code placeholder} would print <i>"Not implemented yet."</i>
 * above correct, live stat numbers.
 *
 * <p><b>That is not hypothetical: it is the defect {@code MenuIcons.placeholder}'s own javadoc
 * records.</b> The recipe browser's empty state -- a feature that worked and had measured zero --
 * was built out of {@code placeholder} and announced itself as missing. <b>Gate row Q33 would have
 * passed on it</b>, because it named the notice and not its lore.
 *
 * <p>So the distinction is <b>WHETHER THE THING IS BUILT, not whether it is clickable</b>:
 *
 * <pre>
 *   NOTHING BEHIND IT AT ALL          placeholder()   "Not implemented yet."
 *   real lore, unbuilt click only     icon()          its own figures
 *
 *   settings torch   was the first    NOW icon()      the screen exists (4b)
 *   stats head       always icon()        icon()      live figures, click still unbuilt (3)
 * </pre>
 *
 * <p>Reaching for {@code placeholder} is a claim that something is NOT BUILT. A surface that
 * renders something true needs {@code icon}, however inert its click.
 */
public final class NexusMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final WeaponRegistry weapons;
    private final ResourcePool resources;
    private final RecipeCatalogue recipes;
    private final ShieldRegistry shields;
    private final ArmorRegistry armor;
    private final ToolRegistry tools;

    /**
     * @param adapters  stats and keys, for the stats head's figures
     * @param profiles  the profile store, carried for the SETTINGS screen rather than used here
     * @param weapons   the registry, for the held weapon's quiver pair
     * @param resources the mana pool
     *
     * @param recipes   the shared catalogue, handed to the CRAFTING screen
     * @param shields   } the three registries the ENCHANT screen needs, carried for the same reason
     * @param armor     }
     * @param tools     }
     *
     * <p>The services are taken rather than a pre-built {@code StatsSheetValues} so that the slice
     * which makes the head clickable can REPAINT it. Nine parameters of nine distinct types, so
     * there is no transposable adjacent pair.
     *
     * <p><b>MOST OF THESE ARE NOT READ BY THIS SCREEN AT ALL</b> -- {@code profiles} goes to
     * {@link SettingsMenu}, {@code recipes} to {@link CraftingMenu}, and the three registries to
     * {@link EnchantMenu}. Threading them rather than reaching for a static is the third
     * architecture invariant: no static mutable singletons holding player state.
     *
     * <h2>THE THREADING STOPS HERE, AND THE BREADCRUMB IS WHY</h2>
     *
     * <b>This is the only screen that carries the full set.</b> A first draft of this slice was
     * about to give {@link SettingsMenu} the same nine and {@link CraftingMenu} ten, each of them
     * only so a Back button could rebuild the hub -- at which point a {@code MenuServices} record
     * was the obvious next move, and Ben's instruction on this arc is explicitly not to over-design
     * it.
     *
     * <p><b>A {@code Supplier<Menu>} settled it without either.</b> A screen that needs a way back
     * takes ONE parameter -- a lambda capturing what the OPENER already had -- rather than the
     * services to rebuild something it has no other use for. {@code SettingsMenu} lost two
     * parameters to it and {@code CraftingMenu} gained one instead of six.
     *
     * <p>So the bundle is not owed: the pressure that would have created it came entirely from
     * reconstruction, and reconstruction is now somebody else's captured variable.
     */
    public NexusMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                     WeaponRegistry weapons, ResourcePool resources, RecipeCatalogue recipes,
                     ShieldRegistry shields, ArmorRegistry armor, ToolRegistry tools) {
        // "THE NEXUS", MATCHING THE ITEM. The hub's title and the star's name are one name now:
        // the item has always been "The Nexus", and the Back buttons on every screen that leads
        // here already read "Back to the Nexus". "Nexus Menu" was the odd one out, and it is not a
        // name for an item -- it was a name for a screen, which is the thing a player does not
        // think about. The item's gradient is untouched; only this line changed.
        super(viewer, NexusMenuLayout.SIZE, MenuIcons.line("The Nexus", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.weapons = weapons;
        this.resources = resources;
        this.recipes = recipes;
        this.shields = shields;
        this.armor = armor;
        this.tools = tools;
        render();
    }

    /**
     * Nothing. Every slot is chrome.
     *
     * <p>Ruled by the operator in the first dialog: <b>the hub holds nothing.</b> It is also what
     * makes the screen safe by construction rather than by guarding -- with no input slots the
     * router performs no moves, the drag handler permits nothing, and there is no state a close
     * could strand. Same property {@code RecipeBrowserMenu} relies on.
     */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return false;
    }

    @Override
    protected void onClick(MenuClick click) {
        if (click.slot() == NexusMenuLayout.CLOSE_SLOT) {
            // Closes, and nothing else -- the same shape CraftingMenu uses, so the button and the
            // escape key cannot drift apart. There is nothing to hand back either way.
            viewer.closeInventory();
            return;
        }
        if (click.slot() == NexusMenuLayout.CRAFTING_SLOT) {
            // Menu.open's rule: hop a tick, no explicit close -- the hub holds no input slots.
            // FROM_NEXUS is what gives the crafting screen its Back button; see CraftingMenu.
            // THE SUPPLIER IS THE BREADCRUMB, and it captures what THIS screen already holds rather
            // than handing CraftingMenu eight services it would only use to rebuild us. A third
            // screen with a Back button costs one lambda, not another constructor widening.
            adapters.scheduler().onEntity(viewer, () -> new CraftingMenu(
                    viewer, adapters, recipes,
                    () -> new NexusMenu(viewer, adapters, profiles, weapons, resources, recipes,
                            shields, armor, tools)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.ENCHANT_SLOT) {
            // THE NO-BLOCK CONSTRUCTOR. An unpowered table: Ben's ruling, and the bookshelf slot
            // reads 0/30 because it MEASURED zero rather than because there is nothing to measure.
            // THE SUPPLIER IS THE BREADCRUMB, same as the crafting station above -- and it is what
            // gives this screen its Back button, which it did not have until now.
            adapters.scheduler().onEntity(viewer, () ->
                    new EnchantMenu(viewer, weapons, shields, armor, tools, adapters,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.GRINDSTONE_SLOT) {
            // THE THIRD STATION, and the same breadcrumb the other two carry. Close-then-hop is
            // NOT needed from THIS side -- the hub holds no input slots; the grindstone does its
            // own explicit close when its Back button is pressed.
            adapters.scheduler().onEntity(viewer, () ->
                    new GrindstoneMenu(viewer, weapons, shields, armor, tools, adapters,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.SETTINGS_SLOT) {
            // HOP A TICK, NO EXPLICIT CLOSE. Menu.open's javadoc carries the measured rule and the
            // reason: both Scheduler entity methods land on the next tick, and the close exists only
            // to run returnEverything for a menu holding the player's items. The hub holds none --
            // inputSlots() is empty -- so openInventory's implicit close is sufficient.
            adapters.scheduler().onEntity(viewer,
                    () -> new SettingsMenu(viewer, adapters, profiles,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools)).open());
            return;
        }
        // Every filler pane is inert, and the stats head's click is still unbuilt -- slice 3's
        // decision, unchanged. Falling through rather than branching on STATS_SLOT deliberately: a
        // no-op branch would read as a wired button whose body someone forgot to write.
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private void render() {
        for (int slot : NexusMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }

        // THE SHARED BARRIER. Not a second close button minted here -- chrome that drifts between
        // screens reads as two different plugins, which is MenuIcons' whole reason for existing.
        getInventory().setItem(NexusMenuLayout.CLOSE_SLOT, MenuIcons.close());

        // icon() NOW, AND IT WAS placeholder() UNTIL SLICE 4b BUILT THE SCREEN BEHIND IT. The class
        // javadoc's table is the argument; this is the graduation it predicted, and it is the THIRD
        // instance of that distinction in this plugin -- the enchant table's bookshelf readout and
        // the recipe browser's empty state are the other two.
        //
        // The rule that decided it: reaching for placeholder is a claim that something is NOT BUILT.
        // A settings screen exists now, so saying "Not implemented yet." above a working button
        // would be the Q33 defect with the notice and the feature inverted.
        // THE CRAFTING-TYPE BAND, row 4. Both are icon() and both are BUILT -- the screens behind
        // them exist and work; only the route through the hub is new.
        getInventory().setItem(NexusMenuLayout.CRAFTING_SLOT, MenuIcons.icon(
                Material.CRAFTING_TABLE,
                MenuIcons.line("Crafting", NamedTextColor.GRAY),
                List.of(MenuIcons.line("The full grid, and the recipe book.",
                        NamedTextColor.DARK_GRAY))));

        // "Unpowered" is said HERE, on the button, and again as 0/30 on the screen itself. A player
        // who is about to walk to a real table should be able to learn that before opening this.
        getInventory().setItem(NexusMenuLayout.ENCHANT_SLOT, MenuIcons.icon(
                Material.ENCHANTING_TABLE,
                MenuIcons.line("Enchanting", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Unpowered -- no bookshelves here.",
                                NamedTextColor.DARK_GRAY),
                        MenuIcons.line("A real table with shelves reaches 30.",
                                NamedTextColor.DARK_GRAY))));

        getInventory().setItem(NexusMenuLayout.SETTINGS_SLOT, MenuIcons.icon(
                Material.REDSTONE_TORCH,
                MenuIcons.line("Settings", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Choose where the Nexus sits.", NamedTextColor.DARK_GRAY))));

        // icon(), NOT placeholder() -- THE OTHER HALF OF THE PAIR THE TORCH ABOVE IS ONE OF, and
        // the class javadoc carries the argument. The lore below is REAL and WORKING; only the
        // click is unbuilt. placeholder() would print "Not implemented yet." above live stat
        // figures, which is the Q33 defect with the readout and the notice inverted.
        ItemStack head = MenuIcons.icon(Material.PLAYER_HEAD, NexusStatsLore.name(),
                NexusStatsLore.lore(
                        StatsSheetProjection.of(viewer, adapters, weapons, resources)));

        // THE SKIN. Cheap HERE AND ONLY HERE: the viewer is online, so their profile is already
        // resolved and nothing fetches.
        //
        // *** DO NOT GENERALISE THIS CALL. *** The same setOwningPlayer for an OFFLINE or
        // third-party player can block on a texture fetch -- on the region thread, inside a menu
        // open, with the client waiting on the screen. The next head added to this hub will be
        // somebody else's, which is why this warning is here and not in a design note.
        //
        // It returns a boolean and can fail; the failure renders as Steve, which is what
        // GATE-nexus.md's own-skin row is reading.
        head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(viewer));

        getInventory().setItem(NexusMenuLayout.STATS_SLOT, head);
    }
}
