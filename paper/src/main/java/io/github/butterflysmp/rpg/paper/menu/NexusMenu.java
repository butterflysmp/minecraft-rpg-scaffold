package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.enchant.GrindstoneRefund;
import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.core.vault.VaultShape;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.hud.AccessorySheet;
import io.github.butterflysmp.rpg.paper.hud.StatsSheetProjection;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.GearScoreItems;
import io.github.butterflysmp.rpg.paper.vault.VaultService;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
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
    private final VaultService vaults;

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
     * @param vaults    the vault store, handed to the VAULT screen -- this one is not read here
     *                  either
     *
     * <p>The services are taken rather than a pre-built {@code StatsSheetValues} so that the slice
     * which makes the head clickable can REPAINT it. <b>Ten parameters of ten distinct types</b>, so
     * there is still no transposable adjacent pair -- which is the property that matters, not the
     * count. It said "nine" until the vault arrived.
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
                     ShieldRegistry shields, ArmorRegistry armor, ToolRegistry tools,
                     VaultService vaults) {
        // "NEXUS MENU", MATCHING THE ITEM -- and the ITEM was renamed to match this, not the other
        // way round. Ben ruled "both, they should match" WITHOUT giving the string; the string was
        // then picked here rather than asked for, and "The Nexus" went onto the item. He has now
        // given it. THIS IS AN OVERTURNED RULING, NOT A DEFECT: the code did what the brief said.
        //
        // THE ARGUMENT THAT LOST, KEPT BECAUSE IT IS THE ONE ANYONE WILL MAKE AGAIN: that "Nexus
        // Menu" is a name for a SCREEN rather than for an item, and a player holding a nether star
        // does not think about menus. It is a reasonable case and it was not the one asked for.
        //
        // THE BACK BUTTONS STAY "Back to the Nexus" on all four screens. Not "Back to the Nexus
        // Menu", which is clunky. THAT IS A GUESS AT AN UNASKED QUESTION rather than a decision --
        // flagged so it costs one word to correct instead of being found a fourth time.
        super(viewer, NexusMenuLayout.SIZE, MenuIcons.line("Nexus Menu", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.weapons = weapons;
        this.resources = resources;
        this.recipes = recipes;
        this.shields = shields;
        this.armor = armor;
        this.tools = tools;
        this.vaults = vaults;
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

    /**
     * This viewer's player level, as the hub sees it.
     *
     * <h2>AN UNAVAILABLE PROFILE READS AS LEVEL 1, WHICH LOCKS EVERYTHING. DELIBERATE, AND FLAGGED</h2>
     *
     * {@code profile()} is empty for a load still in flight and for one that failed. <b>Neither is
     * a level</b>, so the hub has to pick a direction, and it picks the conservative one: an unknown
     * level opens nothing.
     *
     * <p><b>The alternative -- treating unknown as unlocked -- fails towards the player and is
     * still wrong</b>, because it would open all three stations for anyone whose profile failed to
     * read, which is indistinguishable from the gate not existing.
     *
     * <p>In practice the window is tiny: the hub is reached by clicking a star that
     * {@code NexusSlots.converge} only places after the profile has SETTLED. A player who does see
     * everything locked has an unreadable profile, and {@code /rpg stats} says so in words.
     */
    private int viewerLevel() {
        return profiles.profile(viewer.getUniqueId())
                .map(profile -> PlayerLevel.levelFor(profile.lifetimeXp()))
                .orElse(1);
    }

    @Override
    protected void onClick(MenuClick click) {
        // *** THE GATE, AND IT IS ONE CHECK IN FRONT OF THREE BRANCHES RATHER THAN THREE CHECKS. ***
        // A per-station guard would be three places for one rule, and the fourth station added
        // later is the one that would be missed -- which is the hole slot 33 already cost once.
        //
        // *** IT SPEAKS, AND THAT IS COUPLED TO station() KEEPING THE MATERIAL. ***
        //
        // A dimmed name lives in the HOVER TOOLTIP, so without hovering a locked crafting station
        // is PIXEL-IDENTICAL to an unlocked one. Refusing silently would give a first-time player
        // at level 1 a normal-looking crafting table that does nothing and explains nothing --
        // indistinguishable from a broken menu.
        //
        // THE GRINDSTONE'S SILENCE RULING DOES NOT TRANSFER: what ITS button says unasked is
        // COLOUR, at a glance, on the cell being clicked. This one says nothing without a hover.
        // Same shape of rule, different premise, opposite answer. NexusStationGate.refusal carries
        // the full argument and the four-combination table.
        int level = viewerLevel();
        var station = NexusStationGate.at(click.slot());
        if (station.isPresent() && !NexusStationGate.unlocked(station.get(), level)) {
            viewer.sendMessage(MenuIcons.line(
                    NexusStationGate.refusal(station.get(), level), NamedTextColor.GRAY));
            return;
        }
        if (click.slot() == NexusMenuLayout.CLOSE_SLOT) {
            // Closes, and nothing else -- the same shape CraftingMenu uses, so the button and the
            // escape key cannot drift apart. There is nothing to hand back either way.
            viewer.closeInventory();
            return;
        }
        if (click.slot() == NexusMenuLayout.VAULT_SLOT) {
            // *** CLOSE FIRST IS NOT NEEDED HERE AND THE ASYMMETRY IS WORTH READING. ***
            //
            // Menu.open's rule is about the DEPARTING menu: close explicitly only when it holds
            // input slots, so returnEverything runs before the screen changes. The hub holds none.
            // The VAULT does -- which is why its own Back button closes first, and why this
            // direction does not have to.
            adapters.scheduler().onEntity(viewer, () ->
                    new NexusVaultMenu(viewer, adapters, profiles, vaults,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools, vaults)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.ANVIL_SLOT) {
            // THE FIFTH STATION, and the same breadcrumb the other four carry. Close-then-hop is
            // NOT needed from THIS side -- the hub holds no input slots; the anvil does its own
            // explicit close when its Back button is pressed, because it holds two.
            adapters.scheduler().onEntity(viewer, () ->
                    new AnvilMenu(viewer, weapons, shields, armor, tools, adapters,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools, vaults)).open());
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
                            shields, armor, tools, vaults)).open());
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
                                    recipes, shields, armor, tools, vaults)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.GRINDSTONE_SLOT) {
            // THE THIRD STATION, and the same breadcrumb the other two carry. Close-then-hop is
            // NOT needed from THIS side -- the hub holds no input slots; the grindstone does its
            // own explicit close when its Back button is pressed.
            adapters.scheduler().onEntity(viewer, () ->
                    new GrindstoneMenu(viewer, weapons, shields, armor, tools, adapters,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools, vaults)).open());
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
                                    recipes, shields, armor, tools, vaults)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.EQUIPMENT_SLOT) {
            // Hop a tick, no explicit close: the hub holds no input slots (Menu.open's rule). The
            // Equipment screen holds none either -- its slots are views of the player's equipment
            // and accessory store -- so its own Back button needs no close-first.
            adapters.scheduler().onEntity(viewer, () ->
                    new EquipmentMenu(viewer, adapters, profiles,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools, vaults)).open());
            return;
        }
        if (click.slot() == NexusMenuLayout.BUILD_SLOT) {
            // Hop a tick, no explicit close: neither the hub nor the Build screen holds input slots.
            adapters.scheduler().onEntity(viewer, () ->
                    new BuildMenu(viewer, adapters, profiles,
                            () -> new NexusMenu(viewer, adapters, profiles, weapons, resources,
                                    recipes, shields, armor, tools, vaults)).open());
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
        // READ ONCE. Every station asking profiles.profile() separately would be one read per cell
        // inside one paint, and a level that could differ between two cells of the same screen if
        // the load settled mid-render.
        //
        // NAMED, NOT COUNTED. This said "Three stations" and was falsified twice without anyone
        // touching it -- once by the vault and once by the anvil. The rule the comment carries is
        // about the READ, and it does not depend on how many cells there are.
        int level = viewerLevel();

        // THE VAULT, row 4 column 2. The cell 29 that GRINDSTONE_SLOT's javadoc says the row was
        // laid out expecting, filled at last.
        //
        // ITS THIRD LOCKED LINE IS NOT LIKE THE OTHER THREE. "An ender chest in the world still
        // works" would be false, because the hijack replaces the vanilla chest at every level. The
        // sentence each station authors is NexusStationGate.Station.worldRoute().
        getInventory().setItem(NexusMenuLayout.VAULT_SLOT, station(
                NexusStationGate.Station.VAULT, level, Material.ENDER_CHEST,
                List.of(MenuIcons.line("Seven pages, " + VaultShape.TOTAL_SLOTS + " slots.",
                                NamedTextColor.DARK_GRAY),
                        MenuIcons.line("Pages open as you level.", NamedTextColor.DARK_GRAY))));

        // THE FIFTH STATION, AND THE ROW IS NOW CONTIGUOUS 29-33. The gap at 30 was held open by
        // VAULT_SLOT's javadoc against exactly this day, and nothing had to move to fill it.
        //
        // PAINTED HERE BECAUSE IT IS SUBTRACTED FROM THE FILLER. That invariant -- every slot not
        // in FILLER_SLOTS must be painted by something -- is the one slot 33 shipped a hole
        // through, and PAINTED_SLOTS plus NexusMenuLayoutTest is what now makes the two lists one.
        getInventory().setItem(NexusMenuLayout.ANVIL_SLOT, station(
                NexusStationGate.Station.ANVIL, level, Material.ANVIL,
                List.of(MenuIcons.line("Move a gear score onto a better item.",
                                NamedTextColor.DARK_GRAY),
                        MenuIcons.line("Same kind only, and the sacrifice must score higher.",
                                NamedTextColor.DARK_GRAY))));

        // THE CRAFTING-TYPE BAND, row 4. Both are icon() and both are BUILT -- the screens behind
        // them exist and work; only the route through the hub is new.
        getInventory().setItem(NexusMenuLayout.CRAFTING_SLOT, station(
                NexusStationGate.Station.CRAFTING, level, Material.CRAFTING_TABLE,
                List.of(MenuIcons.line("The full grid, and the recipe book.",
                        NamedTextColor.DARK_GRAY))));

        // "Unpowered" is said HERE, on the button, and again as 0/30 on the screen itself. A player
        // who is about to walk to a real table should be able to learn that before opening this.
        getInventory().setItem(NexusMenuLayout.ENCHANT_SLOT, station(
                NexusStationGate.Station.ENCHANTING, level, Material.ENCHANTING_TABLE,
                List.of(MenuIcons.line("Unpowered -- no bookshelves here.",
                                NamedTextColor.DARK_GRAY),
                        MenuIcons.line("A real table with shelves reaches 30.",
                                NamedTextColor.DARK_GRAY))));

        // THE THIRD STATION. IT WAS SUBTRACTED FROM THE FILLER SET AND THEN PAINTED BY NOTHING --
        // an invisible, clickable hole at slot 33, whose click handler worked perfectly. The
        // set-subtraction filler has an invariant nothing checked: EVERY SLOT NOT IN FILLER_SLOTS
        // MUST BE PAINTED BY SOMETHING. NexusMenuLayoutTest now asserts it.
        getInventory().setItem(NexusMenuLayout.GRINDSTONE_SLOT, station(
                NexusStationGate.Station.GRINDSTONE, level, Material.GRINDSTONE,
                List.of(MenuIcons.line("Strip enchants from your gear.", NamedTextColor.DARK_GRAY),
                        MenuIcons.line("Refunds " + GrindstoneRefund.REFUND_PERCENT
                                + "% of what they cost.", NamedTextColor.DARK_GRAY))));

        getInventory().setItem(NexusMenuLayout.SETTINGS_SLOT, MenuIcons.icon(
                Material.REDSTONE_TORCH,
                MenuIcons.line("Settings", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Choose where the Nexus sits.", NamedTextColor.DARK_GRAY))));

        // THE EQUIPMENT SCREEN, row 3. Ungated, like Stats and Settings -- see EQUIPMENT_SLOT.
        getInventory().setItem(NexusMenuLayout.EQUIPMENT_SLOT, MenuIcons.icon(
                Material.ARMOR_STAND,
                MenuIcons.line("Equipment", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Your armour and accessories.", NamedTextColor.DARK_GRAY))));

        // THE BUILD SCREEN, row 3, directly left of Equipment. Ungated, like Equipment (ruling 16).
        getInventory().setItem(NexusMenuLayout.BUILD_SLOT, MenuIcons.icon(
                Material.LECTERN,
                MenuIcons.line("Build", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Your class, element and abilities.", NamedTextColor.DARK_GRAY))));

        // icon(), NOT placeholder() -- THE OTHER HALF OF THE PAIR THE TORCH ABOVE IS ONE OF, and
        // the class javadoc carries the argument. The lore below is REAL and WORKING; only the
        // click is unbuilt. placeholder() would print "Not implemented yet." above live stat
        // figures, which is the Q33 defect with the readout and the notice inverted.
        // THE PROGRESSION BLOCK RIDES ON THE SAME HEAD, above the eight combat stats. Its source is
        // the PROFILE, not StatsSheetValues -- a different store and a different formatter -- which
        // is why it is a second parameter rather than two more fields on the projection.
        ItemStack head = MenuIcons.icon(Material.PLAYER_HEAD, NexusStatsLore.name(),
                NexusStatsLore.lore(
                        StatsSheetProjection.of(viewer, adapters, weapons, resources),
                        profiles.profile(viewer.getUniqueId())
                                .map(PlayerProfile::lifetimeXp)
                                .map(OptionalLong::of)
                                .orElseGet(OptionalLong::empty),
                        // THE AVERAGE, read from the live inventory HERE rather than computed in the
                        // pure renderer -- NexusStatsLore cannot see a PlayerInventory and must not
                        // learn to. Present rather than empty because the viewer is online and their
                        // inventory is readable by construction at this point; empty is reserved for a
                        // read that could not happen, which on this path cannot arise.
                        OptionalInt.of(GearScoreItems.averageOf(viewer, adapters.keys(), adapters.weapons())),
                        AccessorySheet.lines(viewer.getUniqueId(), adapters.accessories(),
                                profiles.profile(viewer.getUniqueId())
                                        .map(PlayerProfile::archetypeId).orElse(null))));

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

    /**
     * One station icon, locked or open.
     *
     * <h2>THE MATERIAL DOES NOT CHANGE WHEN LOCKED, AND THE NAME DIMS INSTEAD</h2>
     *
     * A locked crafting station is still a crafting table, and swapping it for a barrier or a gray
     * pane would cost the player the one cue that says <b>what they are waiting for</b>. So the
     * material stays, the display name drops from {@code GRAY} to {@code DARK_GRAY}, and the lore
     * carries the three sentences.
     *
     * <p><b>*** THIS CHOICE IS WHAT MAKES THE CLICK'S MESSAGE MANDATORY. THEY ARE COUPLED. ***</b>
     * A dimmed name lives in the hover tooltip, so an un-hovered locked station is pixel-identical
     * to an open one. <b>Change the material here and the message becomes redundant; delete the
     * message and leave the material and you have shipped a crafting table that does nothing.</b>
     * {@code NexusStationGate.refusal} carries the four-combination table, and
     * {@code ProgressionWiringSignatureTest} fails if the pair comes apart.
     *
     * <p><b>PRESENTATION, AND IT CAN BE OVERRULED IN ONE WORD</b> -- it is this method, one test
     * row, and the coupling above. The same note {@code NexusStatsLore} carries about its header.
     *
     * <p>{@code icon()} and not {@code placeholder()} in both arms: a locked station is <b>built and
     * gated</b>, not unbuilt, and {@code placeholder}'s <i>"Not implemented yet."</i> would be the
     * Q33 defect for a third time on this screen.
     */
    private ItemStack station(NexusStationGate.Station station, int level, Material material,
                              List<net.kyori.adventure.text.Component> openLore) {
        if (NexusStationGate.unlocked(station, level)) {
            return MenuIcons.icon(material,
                    MenuIcons.line(station.displayName(), NamedTextColor.GRAY), openLore);
        }
        return MenuIcons.icon(material,
                MenuIcons.line(NexusStationGate.lockedName(station), NamedTextColor.DARK_GRAY),
                NexusStationGate.lockedLore(station, level).stream()
                        .map(text -> MenuIcons.line(text, NamedTextColor.DARK_GRAY))
                        .toList());
    }
}
