package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.progression.LevelBonus;
import io.github.butterflysmp.rpg.core.progression.LevelBonusLines;
import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.core.progression.PlayerLevelLines;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
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

/**
 * THE LEVEL SCREEN, opened by clicking the stats head on the Nexus hub (Ben, 2026-09-29: <i>"make the
 * player head clickable and open into a new sub menu with info of what level the player is, how much XP
 * they need for the next level, a list of the levels available and the bonuses and unlocks for each
 * level"</i>).
 *
 * <p>The summary at the top, then levels 1 to {@link PlayerLevel#ACTIVE_CAP}, 28 to a page, each with what
 * that level-up grants ({@link LevelBonusLines#gainedAt}) and what it unlocks ({@link LevelUnlocks}). It
 * opens on the page the player's level is on.
 *
 * <p><b>Read-only, like the hub.</b> No input slots, so the router moves nothing and a close strands
 * nothing. A page flip repaints in place rather than reopening.
 *
 * <p><b>An unavailable profile reads as level 1 and zero XP</b>, the hub's conservative direction
 * ({@code NexusMenu.viewerLevel}). The hub is reached through a star that is placed only after the profile
 * has settled, so in practice this is a hand-edited or failed file.
 *
 * <p>Wording, colours and materials are defaults and Ben's to change. Names are green, per his
 * 2026-09-29 "Green names" ruling for Nexus buttons.
 */
public final class LevelMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final Supplier<Menu> hub;
    private int page;

    /** @param hub the breadcrumb: rebuilds the screen Back returns to, as every Nexus sub-screen takes. */
    public LevelMenu(Player viewer, AdapterContext adapters, ProfileService profiles, Supplier<Menu> hub) {
        super(viewer, LevelMenuLayout.SIZE, MenuIcons.line("Player Level", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.hub = hub;
        this.page = LevelMenuLayout.pageOf(PlayerLevel.effectiveLevel(lifetimeXp()));
        render();
    }

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
        if (click.slot() == LevelMenuLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (click.slot() == LevelMenuLayout.BACK_SLOT) {
            // Hop a tick, no explicit close: this screen holds no input slots (Menu.open's rule).
            adapters.scheduler().onEntity(viewer, () -> hub.get().open());
            return;
        }
        if (click.slot() == LevelMenuLayout.PREV_SLOT && page > 0) {
            page--;
            render();
            return;
        }
        if (click.slot() == LevelMenuLayout.NEXT_SLOT && page < LevelMenuLayout.PAGE_COUNT - 1) {
            page++;
            render();
        }
        // A level cell, the summary and the filler are read-only.
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty.
    }

    private long lifetimeXp() {
        return profiles.profile(viewer.getUniqueId()).map(PlayerProfile::lifetimeXp).orElse(0L);
    }

    private void render() {
        long xp = lifetimeXp();
        int level = PlayerLevel.effectiveLevel(xp);

        for (int slot : LevelMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(LevelMenuLayout.CLOSE_SLOT, MenuIcons.close());
        getInventory().setItem(LevelMenuLayout.BACK_SLOT, MenuIcons.back(Material.ARROW, "the Nexus"));
        getInventory().setItem(LevelMenuLayout.SUMMARY_SLOT, summary(xp, level));

        getInventory().setItem(LevelMenuLayout.PREV_SLOT, page > 0
                ? MenuIcons.icon(Material.PAPER, MenuIcons.line("Previous page", NamedTextColor.GREEN),
                        List.of(MenuIcons.line("Page " + page + " of " + LevelMenuLayout.PAGE_COUNT,
                                NamedTextColor.DARK_GRAY)))
                : MenuIcons.filler());
        getInventory().setItem(LevelMenuLayout.NEXT_SLOT, page < LevelMenuLayout.PAGE_COUNT - 1
                ? MenuIcons.icon(Material.PAPER, MenuIcons.line("Next page", NamedTextColor.GREEN),
                        List.of(MenuIcons.line("Page " + (page + 2) + " of " + LevelMenuLayout.PAGE_COUNT,
                                NamedTextColor.DARK_GRAY)))
                : MenuIcons.filler());

        for (int index = 0; index < LevelMenuLayout.PAGE_SIZE; index++) {
            int shown = LevelMenuLayout.levelAt(page, index);
            int slot = LevelMenuLayout.LEVEL_SLOTS.get(index);
            getInventory().setItem(slot, shown == 0 ? MenuIcons.filler() : levelCell(shown, level, xp));
        }
    }

    private ItemStack summary(long xp, int level) {
        List<Component> lore = new ArrayList<>();
        lore.add(MenuIcons.line("Lifetime XP: " + PlayerLevelLines.lifetime(xp), NamedTextColor.GRAY));
        lore.add(PlayerLevel.isAtActiveCap(xp)
                ? MenuIcons.line("You are at the level cap.", NamedTextColor.GRAY)
                : MenuIcons.line("To next level: " + PlayerLevelLines.toNext(xp) + " XP", NamedTextColor.GRAY));
        lore.add(MenuIcons.blank());
        List<String> totals = LevelBonusLines.totals(LevelBonus.at(level));
        if (totals.isEmpty()) {
            lore.add(MenuIcons.line("Your level grants nothing yet.", NamedTextColor.DARK_GRAY));
        } else {
            lore.add(MenuIcons.line("Your level grants:", NamedTextColor.GRAY));
            for (String line : totals) lore.add(MenuIcons.line(line, NamedTextColor.AQUA));
            lore.add(MenuIcons.line("Weapon Damage applies to weapon hits only.", NamedTextColor.DARK_GRAY));
        }
        return MenuIcons.icon(Material.EXPERIENCE_BOTTLE,
                MenuIcons.line("Level " + PlayerLevelLines.level(xp), NamedTextColor.GREEN), lore);
    }

    /** One level: reached (lime), the current one (a bottle), or still ahead (gray). */
    private ItemStack levelCell(int shown, int level, long xp) {
        boolean reached = shown <= level;
        List<Component> lore = new ArrayList<>();
        List<String> gained = LevelBonusLines.gainedAt(shown);
        List<String> unlocks = LevelUnlocks.at(shown);
        if (gained.isEmpty() && unlocks.isEmpty()) {
            lore.add(MenuIcons.line("Where everyone starts.", NamedTextColor.DARK_GRAY));
        }
        for (String line : gained) lore.add(MenuIcons.line(line, NamedTextColor.AQUA));
        for (String line : unlocks) lore.add(MenuIcons.line(line, NamedTextColor.GOLD));
        lore.add(MenuIcons.blank());
        lore.add(reached
                ? MenuIcons.line("Reached", NamedTextColor.GREEN)
                : MenuIcons.line(PlayerLevelLines.amount(PlayerLevel.totalForLevel(shown) - xp) + " XP to go",
                        NamedTextColor.DARK_GRAY));
        Material material = shown == level ? Material.EXPERIENCE_BOTTLE
                : reached ? Material.LIME_DYE : Material.GRAY_DYE;
        return MenuIcons.icon(material,
                MenuIcons.line("Level " + shown, reached ? NamedTextColor.GREEN : NamedTextColor.GRAY), lore);
    }
}
