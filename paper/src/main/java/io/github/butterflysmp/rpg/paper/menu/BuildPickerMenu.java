package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.build.BuildRules;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;
import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.build.BuildService;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.CellLoadout;
import io.github.butterflysmp.rpg.storage.PlayerBuild;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

/**
 * One of the Build screen's five choices: the class, the element, the Ultimate, Active 1 or Active 2
 * (PLAN-build-system.md section 3.3). ONE picker for all five, as {@code NexusSlotPickerMenu} is one for
 * both locked items; {@link Kind} says which.
 *
 * <p><b>What is offered is {@code BuildRules}' answer, never this class's</b>: the pools' classes, the
 * elements with a pool for the class (FIRE only at ship, ruling 7), and for a loadout slot only what the
 * cell's pool lists in that role. A pick is saved, then the Build screen reopens showing it.
 *
 * <p>Like the Build screen it has <b>no input slots</b> and every option is an icon rendered here, so
 * nothing can leave it -- see {@code BuildMenu}.
 */
public final class BuildPickerMenu extends Menu {

    /** Which choice this picker makes. */
    public enum Kind {
        CLASS, ELEMENT, ULTIMATE, ACTIVE_1, ACTIVE_2;

        static Kind of(LoadoutSlot slot) {
            return switch (slot) {
                case ULTIMATE -> ULTIMATE;
                case ACTIVE_1 -> ACTIVE_1;
                case ACTIVE_2 -> ACTIVE_2;
            };
        }

        /** The loadout slot this picks, or empty for the class and element pickers. */
        Optional<LoadoutSlot> loadoutSlot() {
            return switch (this) {
                case CLASS, ELEMENT -> Optional.empty();
                case ULTIMATE -> Optional.of(LoadoutSlot.ULTIMATE);
                case ACTIVE_1 -> Optional.of(LoadoutSlot.ACTIVE_1);
                case ACTIVE_2 -> Optional.of(LoadoutSlot.ACTIVE_2);
            };
        }
    }

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final Kind kind;

    /** How to rebuild the Build screen, for Back and after a pick. */
    private final Supplier<Menu> build;

    /** The ids on offer, in option order. Read once per render; a click indexes into it. */
    private List<String> options = List.of();

    public BuildPickerMenu(Player viewer, AdapterContext adapters, ProfileService profiles, Kind kind,
                           Supplier<Menu> build) {
        super(viewer, BuildMenuLayout.SIZE, MenuIcons.line(title(kind), NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.kind = kind;
        this.build = build;
        render();
    }

    private static String title(Kind kind) {
        return switch (kind) {
            case CLASS -> "Choose a Class";
            case ELEMENT -> "Choose an Element";
            case ULTIMATE -> "Choose an Ultimate (Q)";
            case ACTIVE_1 -> "Choose an Active (Left)";
            case ACTIVE_2 -> "Choose an Active (Right)";
        };
    }

    /** NONE, and that is the anti-dupe rule: see {@code BuildMenu}. */
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
        if (click.slot() == BuildMenuLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (click.slot() == BuildMenuLayout.BACK_SLOT) {
            backToBuild();
            return;
        }
        OptionalInt option = BuildMenuLayout.optionAt(click.slot());
        if (option.isEmpty() || option.getAsInt() >= options.size()) return;   // filler; inert
        String id = options.get(option.getAsInt());
        boolean done = switch (kind) {
            case CLASS -> chooseClass(id);
            case ELEMENT -> chooseElement(id);
            case ULTIMATE, ACTIVE_1, ACTIVE_2 -> chooseAbility(kind.loadoutSlot().orElseThrow(), id);
        };
        if (done) backToBuild();
    }

    /** Hop a tick, no explicit close: this screen holds no input slots (Menu.open's rule). */
    private void backToBuild() {
        adapters.scheduler().onEntity(viewer, () -> build.get().open());
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    // ------------------------------------------------------------------ the three writes

    /**
     * Set the class. The element is kept where the new class has a pool for it, else cleared
     * ({@code BuildRules.elementAfterClassPick}), so a profile never names a cell with no pool.
     * <b>Nothing is granted</b> (ruling 6), and cooldowns are kept (section 2.6).
     */
    private boolean chooseClass(String classId) {
        Optional<PlayerProfile> profile = profiles.profile(viewer.getUniqueId());
        if (profile.isEmpty()) return profileUnavailable();
        String element = BuildRules.elementAfterClassPick(adapters.stones().pools().all(), classId,
                profile.get().elementId(), PlayerProfile.NONE);
        return applyCell(classId, element);
    }

    /** Set the element; the class is carried unchanged. */
    private boolean chooseElement(String elementId) {
        Optional<PlayerProfile> profile = profiles.profile(viewer.getUniqueId());
        if (profile.isEmpty()) return profileUnavailable();
        return applyCell(profile.get().archetypeId(), elementId);
    }

    /**
     * Write (class, element) and say where it left the player. The stone's lore is refreshed here:
     * convergence keeps an existing stone rather than re-minting it, so without this it would keep
     * naming the old cell's abilities until the next join.
     */
    private boolean applyCell(String classId, String elementId) {
        if (!profiles.setCell(viewer.getUniqueId(), classId, elementId)) return profileUnavailable();
        Optional<PlayerProfile> updated = profiles.profile(viewer.getUniqueId());
        adapters.stones().refreshLore(viewer, updated);
        Optional<PoolDefinition> pool = adapters.stones().pools().find(classId, elementId);
        if (pool.isPresent()) {
            viewer.sendMessage(Component.text("You are now ", NamedTextColor.AQUA)
                    .append(MiniMessage.miniMessage().deserialize(pool.get().displayName())));
        } else {
            viewer.sendMessage(Component.text("Now choose your "
                    + (kind == Kind.CLASS ? "element." : "class."), NamedTextColor.YELLOW));
        }
        return true;
    }

    /**
     * Save one slot of the current cell's loadout: {@code BuildRules.pick} from what is equipped now
     * (the saved loadout, or the pool default), so picking one slot never empties the other two, and
     * picking the other Active's ability swaps the two. The cell's aspects and fragments are carried.
     *
     * <p>{@code BuildService.save} replaces the cached build BEFORE the write, so the Build screen that
     * reopens a tick later already shows the pick; the file write settles on the I/O thread.
     */
    private boolean chooseAbility(LoadoutSlot slot, String abilityId) {
        Optional<PlayerProfile> profile = profiles.profile(viewer.getUniqueId());
        if (profile.isEmpty()) return profileUnavailable();
        String classId = profile.get().archetypeId();
        String elementId = profile.get().elementId();
        Optional<PoolDefinition> pool = adapters.stones().pools().find(classId, elementId);
        if (pool.isEmpty()) {
            viewer.sendMessage(Component.text("Choose a class and an element first.", NamedTextColor.YELLOW));
            return true;
        }
        BuildService builds = adapters.stones().builds();
        Optional<PlayerBuild> stored = builds.build(viewer.getUniqueId());
        if (stored.isEmpty()) {
            viewer.sendMessage(Component.text(builds.unusable(viewer.getUniqueId())
                    ? "Your build is unavailable this session (see the server log); nothing can be saved."
                    : "Your build is still loading -- try again in a moment.", NamedTextColor.RED));
            return false;
        }
        Equipped current = adapters.stones().equippedFor(viewer.getUniqueId(), profile).orElseThrow();
        Optional<BuildRules.Picked> picked = BuildRules.pick(pool.get(), current, slot, abilityId);
        if (picked.isEmpty()) {
            // Unreachable from the rendered options, which ARE BuildRules.choices. Refused, not trusted.
            viewer.sendMessage(Component.text(abilityId + " is not offered there.", NamedTextColor.RED));
            return false;
        }
        CellLoadout existing = stored.get().loadout(classId, elementId)
                .orElseGet(() -> CellLoadout.empty(classId, elementId));
        CellLoadout next = new CellLoadout(classId, elementId, picked.get().ultimate(),
                Arrays.asList(picked.get().active1(), picked.get().active2()),
                existing.aspects(), existing.fragments());
        Component name = abilityName(abilityId);
        boolean accepted = builds.save(viewer.getUniqueId(), next, ok ->
                adapters.scheduler().onEntity(viewer, () -> {
                    if (!viewer.isOnline()) return;
                    if (ok) {
                        adapters.stones().refreshLore(viewer, profiles.profile(viewer.getUniqueId()));
                        viewer.sendMessage(Component.text("Saved: " + BuildMenuLayout.inputLabel(slot) + " = ",
                                NamedTextColor.AQUA).append(name));
                    } else {
                        viewer.sendMessage(Component.text("The build could not be written; see the server log.",
                                NamedTextColor.RED));
                    }
                }));
        if (!accepted) {
            viewer.sendMessage(Component.text("Your build is still loading -- try again in a moment.",
                    NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean profileUnavailable() {
        viewer.sendMessage(profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                ? Component.text(ProfileService.UNREADABLE_PROFILE, NamedTextColor.RED)
                : Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
        return false;
    }

    // ------------------------------------------------------------------ render

    private void render() {
        for (int slot : BuildMenuLayout.PICKER_FILLER_SLOTS) getInventory().setItem(slot, MenuIcons.filler());
        getInventory().setItem(BuildMenuLayout.CLOSE_SLOT, MenuIcons.close());
        getInventory().setItem(BuildMenuLayout.BACK_SLOT, MenuIcons.back(Material.ARROW, "Build"));
        getInventory().setItem(BuildMenuLayout.PICKER_TITLE_SLOT, MenuIcons.icon(material(),
                MenuIcons.line(title(kind), NamedTextColor.GRAY), List.of()));

        Optional<PlayerProfile> profile = profiles.profile(viewer.getUniqueId());
        Optional<Equipped> equipped = adapters.stones().equippedFor(viewer.getUniqueId(), profile);
        options = optionsFor(profile);
        String current = currentChoice(profile, equipped);
        String otherActive = otherActive(equipped);

        for (int i = 0; i < BuildMenuLayout.OPTION_SLOTS.size(); i++) {
            int slot = BuildMenuLayout.OPTION_SLOTS.get(i);
            if (i >= options.size()) {
                getInventory().setItem(slot, MenuIcons.filler());
                continue;
            }
            String id = options.get(i);
            getInventory().setItem(slot, optionIcon(id, id.equals(current), id.equals(otherActive)));
        }
    }

    /** What this picker offers -- BuildRules' answer, and nothing else. */
    private List<String> optionsFor(Optional<PlayerProfile> profile) {
        var pools = adapters.stones().pools();
        return switch (kind) {
            case CLASS -> BuildRules.classes(pools.all());
            case ELEMENT -> BuildRules.elementsFor(pools.all(),
                    profile.map(PlayerProfile::archetypeId).orElse(PlayerProfile.NONE));
            case ULTIMATE, ACTIVE_1, ACTIVE_2 -> profile
                    .flatMap(p -> pools.find(p.archetypeId(), p.elementId()))
                    .map(pool -> BuildRules.choices(pool, kind.loadoutSlot().orElseThrow()))
                    .orElse(List.of());
        };
    }

    private String currentChoice(Optional<PlayerProfile> profile, Optional<Equipped> equipped) {
        return switch (kind) {
            case CLASS -> profile.map(PlayerProfile::archetypeId).orElse(null);
            case ELEMENT -> profile.map(PlayerProfile::elementId).orElse(null);
            case ULTIMATE, ACTIVE_1, ACTIVE_2 ->
                    equipped.flatMap(e -> e.idFor(kind.loadoutSlot().orElseThrow())).orElse(null);
        };
    }

    /** For an Active picker, the ability in the OTHER Active: picking it swaps the two. */
    private String otherActive(Optional<Equipped> equipped) {
        return switch (kind) {
            case ACTIVE_1 -> equipped.flatMap(e -> e.idFor(LoadoutSlot.ACTIVE_2)).orElse(null);
            case ACTIVE_2 -> equipped.flatMap(e -> e.idFor(LoadoutSlot.ACTIVE_1)).orElse(null);
            default -> null;
        };
    }

    private Material material() {
        return switch (kind) {
            case CLASS -> Material.NAME_TAG;
            case ELEMENT -> Material.GLOWSTONE_DUST;
            case ULTIMATE -> Material.AMETHYST_CLUSTER;
            case ACTIVE_1, ACTIVE_2 -> Material.PRISMARINE_SHARD;
        };
    }

    /**
     * One option. The current choice is named in GREEN, reads "Current", and glints; nothing else here
     * glints, because none of these materials does on its own.
     */
    private ItemStack optionIcon(String id, boolean current, boolean inOtherActive) {
        List<Component> lore = new ArrayList<>();
        Component name = switch (kind) {
            case CLASS -> Component.text(BuildMenu.capitalised(id), NamedTextColor.WHITE);
            case ELEMENT -> adapters.elements().find(id)
                    .map(def -> MiniMessage.miniMessage().deserialize(def.displayName()))
                    .orElse(Component.text(id, NamedTextColor.WHITE));
            case ULTIMATE, ACTIVE_1, ACTIVE_2 -> abilityName(id);
        };
        if (kind.loadoutSlot().isPresent()) {
            adapters.stones().abilities().find(id).map(BuildMenu::abilityLore).ifPresent(lore::addAll);
        }
        if (inOtherActive) {
            lore.add(MenuIcons.line("In " + (kind == Kind.ACTIVE_1 ? "Right" : "Left")
                    + " now -- choosing it swaps the two.", NamedTextColor.YELLOW));
        }
        lore.add(current
                ? MenuIcons.line("Current", NamedTextColor.GREEN)
                : MenuIcons.line("Click to choose.", NamedTextColor.DARK_GRAY));
        ItemStack icon = MenuIcons.icon(material(),
                name.decoration(TextDecoration.ITALIC, false).colorIfAbsent(current ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                lore);
        if (current) icon.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        return icon;
    }

    private Component abilityName(String id) {
        return adapters.stones().abilities().find(id)
                .map(AbilityDefinition::displayName)
                .map(MiniMessage.miniMessage()::deserialize)
                .orElse(Component.text(id, NamedTextColor.RED));
    }
}
