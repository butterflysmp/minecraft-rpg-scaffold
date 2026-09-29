package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.vault.VaultService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * THE ONE PUBLIC OPENER for a Nexus screen from outside this package: the player slash commands (Ben,
 * 2026-09-29: <i>"Names are good"</i>; PLAN-nexus-polish.md RULINGS and section 9).
 *
 * <h2>WHY IT LIVES HERE AND NOT IN {@code command/}</h2>
 *
 * <p>The station gate is {@link NexusStationGate}, package-private, and it must stay the ONE place the rule
 * lives: a command that copied "anvil at 7" would drift from the hub the first time Ben retunes a level. So
 * the command asks this class, and this class asks the same gate, the same way the hub's click does
 * ({@code NexusMenu.onClick}): {@code unlocked}, then {@code refusal} for the sentence.
 *
 * <h2>WHAT A COMMAND CHECKS (the seat's N3; the plan's section 9.3)</h2>
 *
 * <ul>
 *   <li><b>The profile</b>, as {@code /menu} does: a level cannot be read without it.</li>
 *   <li><b>Death:</b> refused. Opening a screen over the death screen is not a state anything here was
 *       built for.</li>
 *   <li><b>The level gate, the hub's own:</b> vault 20, anvil 7, crafting 3, enchanting 10, grindstone 13.
 *       {@code /recipes} takes CRAFTING's gate: the browser has no hub button of its own, and its Back
 *       opens the crafting screen, so an ungated {@code /recipes} would be a route round the crafting
 *       gate.</li>
 *   <li><b>Creative: allowed</b>, as {@code /menu} is. The star's gesture excludes creative
 *       ({@code NexusOpenGesture}); a command never passes through the gesture.</li>
 *   <li><b>"In combat": not checked.</b> Nothing in the code tracks it (the seat's N3: out of scope).</li>
 * </ul>
 *
 * <p>Every screen opened here gets the hub as its breadcrumb, so its Back reads "Back to the Nexus", as
 * it does when the hub opened it.
 */
public final class NexusScreens {

    private NexusScreens() {}

    /** One command per screen, one lowercase word each -- Ben's names, 2026-09-29. */
    public enum Screen {
        LEVEL("level", "Open your level: bonuses and unlocks", null),
        BUILD("build", "Open the Build screen: class, element and abilities", null),
        GEAR("gear", "Open the Equipment screen", null),
        VAULT("vault", "Open your vault", NexusStationGate.Station.VAULT),
        ANVIL("anvil", "Open the Nexus anvil", NexusStationGate.Station.ANVIL),
        CRAFT("craft", "Open the Nexus crafting grid", NexusStationGate.Station.CRAFTING),
        RECIPES("recipes", "Browse what you can craft now", NexusStationGate.Station.CRAFTING),
        ENCHANTING("enchanting", "Open the Nexus enchanting table", NexusStationGate.Station.ENCHANTING),
        GRINDSTONE("grindstone", "Open the Nexus grindstone", NexusStationGate.Station.GRINDSTONE),
        SETTINGS("settings", "Open the Nexus settings", null);

        private final String command;
        private final String description;
        private final NexusStationGate.Station gate;

        Screen(String command, String description, NexusStationGate.Station gate) {
            this.command = command;
            this.description = description;
            this.gate = gate;
        }

        /** The command's one lowercase word. */
        public String command() { return command; }

        /** The {@code /help} line. Wording is a default. */
        public String description() { return description; }

        /** Its permission node, {@code default: true} in {@code paper-plugin.yml}. */
        public String permission() { return "rpg.command." + command; }

        /** The level this screen needs, or 1 when it has no gate. Read from the gate, never copied. */
        public int unlockLevel() { return gate == null ? 1 : gate.unlockLevel(); }

        NexusStationGate.Station gate() { return gate; }
    }

    /** The sentence the player sees when refused, from the checks above in their order; empty to open. */
    public static Optional<String> refusal(Player player, Screen screen, PlayerProfile profile) {
        if (player.isDead()) return Optional.of("You can't open that while dead.");
        if (screen.gate() == null) return Optional.empty();
        int level = PlayerLevel.effectiveLevel(profile.lifetimeXp());
        if (NexusStationGate.unlocked(screen.gate(), level)) return Optional.empty();
        return Optional.of(NexusStationGate.refusal(screen.gate(), level));
    }

    /**
     * Open {@code screen} for {@code player}, with the hub as its breadcrumb. The caller has already asked
     * {@link #refusal}. Opens directly, as {@code /menu} does: a command is not inside an inventory close.
     */
    public static void open(Player player, Screen screen, AdapterContext adapters, ProfileService profiles,
                            WeaponRegistry weapons, ResourcePool resources, RecipeCatalogue recipes,
                            ShieldRegistry shields, ArmorRegistry armor, ToolRegistry tools, VaultService vaults) {
        Supplier<Menu> hub = () -> new NexusMenu(player, adapters, profiles, weapons, resources, recipes,
                shields, armor, tools, vaults);
        Menu menu = switch (screen) {
            case LEVEL -> new LevelMenu(player, adapters, profiles, hub);
            case BUILD -> new BuildMenu(player, adapters, profiles, hub);
            case GEAR -> new EquipmentMenu(player, adapters, profiles, hub);
            case VAULT -> new NexusVaultMenu(player, adapters, profiles, vaults, hub);
            case ANVIL -> new AnvilMenu(player, weapons, shields, armor, tools, adapters, hub);
            case CRAFT -> new CraftingMenu(player, adapters, recipes, hub);
            case RECIPES -> new RecipeBrowserMenu(player, adapters, recipes, hub);
            case ENCHANTING -> new EnchantMenu(player, weapons, shields, armor, tools, adapters, hub);
            case GRINDSTONE -> new GrindstoneMenu(player, weapons, shields, armor, tools, adapters, hub);
            case SETTINGS -> new SettingsMenu(player, adapters, profiles, hub);
        };
        menu.open();
    }
}
