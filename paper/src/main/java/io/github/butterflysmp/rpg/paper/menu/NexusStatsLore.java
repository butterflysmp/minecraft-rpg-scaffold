package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.combat.StatsSheetValues;
import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.core.progression.PlayerLevelLines;
import io.github.butterflysmp.rpg.core.weapon.GearLoreLines;
import io.github.butterflysmp.rpg.paper.hud.StatsSheet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * The text on the Nexus hub's stats head: its name, and its lore.
 *
 * <p>Pure, and that is the whole reason it is a separate class. The head itself needs a live
 * {@code Player}, a {@code SkullMeta} and {@code Bukkit.createInventory}, none of which exist in a
 * unit test -- <b>but what the tooltip SAYS is a function of a {@code StatsSheetValues} and nothing
 * else</b>, so it is tested here at the 2-second loop instead of costing a boot. Same split
 * {@code GridClickIntent} and {@code CollectPlan} make.
 *
 * <h2>THIS IS NOT A THIRD STAT RENDERER, AND IT MUST NEVER BECOME ONE</h2>
 *
 * <b>It computes no figure.</b> Every line comes from {@link StatsSheet#statLines}, the same method
 * {@code /rpg stats} renders to chat, fed from the same {@code StatsSheetProjection}. There is
 * <b>one input path and one formatter</b>; this class chooses between two presentations of them and
 * does nothing else.
 *
 * <p>{@code RpgCommand} calls the stat sheet <i>"the eight build stats, read-only, self-only. The
 * ONE player-facing command in this arc"</i>. <b>That singularity is the design.</b> A head that
 * computed its own figures would be a second answer to one question, and the drift would not
 * present as a failure -- it would present as two screens disagreeing, months later, with no test
 * in a position to see it.
 *
 * <p>So: if a future shape does not fit a tooltip, <b>adapt the PRESENTATION here. Never recompute
 * the values.</b>
 *
 * <h2>WHY THERE IS NO HEADER LINE</h2>
 *
 * The header is chat CHROME. A chat message has no frame, so it needs a line saying where it starts
 * and what it is; <b>a tooltip is nothing but frame</b> -- it has an edge, and the item's display
 * name at the top already says "Your Stats". Carrying the header would make the tooltip state its
 * own name twice.
 *
 * <p>The same instinct as {@code MenuIcons.close()} losing its lore line on the operator's
 * instruction, for a quieter screen. <b>It is presentation, and Ben can overrule it in one word</b>
 * -- it is one method.
 */
final class NexusStatsLore {

    private NexusStatsLore() {}

    /**
     * The head's display name, which is where the header went.
     *
     * <p>GOLD, and {@link StatsSheetLines#HEADER} rather than a literal, so the button and the chat
     * report cannot come to call the same screen two different things. That constant carries a note
     * saying it is now a button name as well as a chat header -- <b>decorating it decorates this
     * button.</b>
     */
    static Component name() {
        return MenuIcons.line(StatsSheetLines.HEADER, NamedTextColor.GOLD);
    }

    /**
     * The head's lore: the stat lines, or the untracked notice.
     *
     * <p><b>THE BRANCH IS IN HERE RATHER THAN AT THE CALL SITE, DELIBERATELY.</b> The empty case is
     * a real state -- a freshly-joined player whose reconcile loop has not registered them yet, and
     * the hub is the surface they are most likely to meet it on. Put the branch in {@code NexusMenu}
     * and it becomes boot-gate-only; put it here and a unit test can cause it.
     *
     * <p><b>It must NOT render zeroes.</b> A readout showing {@code 0} when nothing was counted is
     * indistinguishable from a working readout that measured zero -- {@code MenuIcons.placeholder}'s
     * own argument, in a place a player can see. It says
     * {@link StatsSheetLines#UNTRACKED}, <b>in the same words {@code /rpg stats} uses</b>, because
     * the two surfaces agreeing about the failure matters as much as agreeing about the numbers.
     *
     * <p><b>The quiver pair rides along, and dropping it would be a bug.</b> It is conditional on
     * the weapon in hand, so it is present here exactly when it is present in chat. Rendering eight
     * of ten would reintroduce at the presentation layer the drift the projection eliminates at the
     * data layer -- one input path and two different answers -- and the gate row comparing the two
     * surfaces would pass whenever no quiver weapon is held, which is most of the time. <b>A
     * conditional line is not an optional line.</b>
     *
     * <p><b>KNOWN STALENESS, ACCEPTED FOR THIS SLICE.</b> The menu paints once, on open. Eight of
     * the ten lines are maxima and rates and cannot move while a screen is up; <b>the quiver pair is
     * the exception</b>, because it is keyed to the HELD weapon and {@code MenuRouting} deliberately
     * permits a player to move items around their own inventory with a menu open. So the head can
     * go on showing a capacity for a weapon no longer selected. <b>Not fixed, because this button
     * does not open anything yet</b> -- the slice that makes it clickable decides whether to
     * repaint, and {@code CraftingMenu} already has the one-tick-on-inventory-change shape if it
     * becomes worth it. Recorded so that slice finds the answer instead of rediscovering the
     * question.
     */
    /** The same lore with NO accessory block -- a player wearing none. */
    static List<Component> lore(Optional<StatsSheetValues> values, OptionalLong lifetimeXp,
                                OptionalInt averageScore) {
        return lore(values, lifetimeXp, averageScore, List.of());
    }

    static List<Component> lore(Optional<StatsSheetValues> values, OptionalLong lifetimeXp,
                                OptionalInt averageScore, List<Component> accessoryLines) {
        List<Component> lines = new ArrayList<>(progressionLines(lifetimeXp));
        lines.addAll(gearScoreLines(averageScore));
        lines.addAll(values
                .map(StatsSheet::statLines)
                .orElseGet(() -> List.of(
                        MenuIcons.line(StatsSheetLines.UNTRACKED, NamedTextColor.RED))));
        // Ruling Q4: the accessories' own lines, UNDER the totals they feed -- the same block
        // /rpg stats prints, from the same AccessorySheet, so the two surfaces cannot disagree.
        lines.addAll(accessoryLines);
        return List.copyOf(lines);
    }

    /**
     * The progression block: level, lifetime XP, and XP to the next level.
     *
     * <h2>IT GOES FIRST, ABOVE THE EIGHT COMBAT STATS</h2>
     *
     * A level is the coarsest thing on the tooltip and the one a player checks most often; the
     * eight stats are the detail underneath it. The column is shared -- both blocks pad through
     * {@code StatsSheetLines.label}, which is why {@code PlayerLevelLines} deliberately has no
     * padder of its own.
     *
     * <h2>*** TWO SOURCES, TWO INDEPENDENT EMPTY CASES, AND THEY MUST NOT BE MERGED ***</h2>
     *
     * The stat lines come from {@code CombatantStats}; this comes from the PROFILE. <b>Either can
     * be absent without the other</b> -- a registered player whose profile failed to read, or a
     * loaded profile for someone the reconcile loop has not registered yet -- so the head renders
     * whichever halves it has.
     *
     * <p><b>An absent profile renders NOTHING here rather than "Level 1".</b> Same argument as the
     * untracked notice one method up: a readout showing level 1 when nothing was read is
     * indistinguishable from a working readout of a genuinely new player, and this one is worse,
     * because a level-40 player would be told they are level 1 by a screen that looks fine.
     *
     * <h2>NO "To Next" LINE AT THE CAP</h2>
     *
     * {@code PlayerLevelLines.toNext} THROWS there, on purpose, so this branch is not optional --
     * see that class on why a word in that column would restate the predecessor's
     * {@code Long.MAX_VALUE} defect.
     */
    private static List<Component> progressionLines(OptionalLong lifetimeXp) {
        if (lifetimeXp.isEmpty()) return List.of();
        long total = lifetimeXp.getAsLong();

        List<Component> lines = new ArrayList<>();
        lines.add(statLine(PlayerLevelLines.LEVEL_LABEL, PlayerLevelLines.level(total),
                NamedTextColor.GOLD));
        lines.add(statLine(PlayerLevelLines.LIFETIME_LABEL, PlayerLevelLines.lifetime(total),
                NamedTextColor.GRAY));
        if (!PlayerLevel.isMaxed(total)) {
            lines.add(statLine(PlayerLevelLines.TO_NEXT_LABEL, PlayerLevelLines.toNext(total),
                    NamedTextColor.GRAY));
        }
        return lines;
    }

    /**
     * The AVERAGE gear score across the six slots -- one line, between progression and the combat
     * stats.
     *
     * <h2>IT SITS BETWEEN THE TWO BLOCKS BECAUSE IT BELONGS TO BOTH</h2>
     *
     * A level is what the player has EARNED and the eight stats are what they currently ARE; the
     * average gear score is what their EQUIPMENT is worth, which drives the next of the first and
     * scales two of the second. Under the level, above the stats.
     *
     * <h2>*** AN ABSENT AVERAGE RENDERS NOTHING, AND A ZERO AVERAGE RENDERS ZERO ***</h2>
     *
     * These are two different states and the distinction is the same one the untracked notice makes
     * one method up. <b>Empty means nobody could read the inventory</b> -- no line, because a readout
     * showing 0 when nothing was counted is indistinguishable from a working readout of a naked
     * player. <b>Zero means the read SUCCEEDED and the player is wearing nothing scored</b>, which is
     * a true and useful fact and is printed.
     *
     * <p>So the { OptionalInt} is carrying the read FAILING, not the value being uninteresting --
     * exactly as { progressionLines} treats an unreadable profile, and for the same reason.
     *
     * <p><b>KNOWN STALENESS, same as the quiver pair.</b> The menu paints once, on open, and
     * { MenuRouting} deliberately permits a player to move items around their own inventory with
     * a menu open -- so swapping a weapon while this screen is up leaves the average as it was. Not
     * fixed for the same reason the quiver pair is not: the slice that makes this button clickable
     * decides whether to repaint.
     */
    private static List<Component> gearScoreLines(OptionalInt averageScore) {
        if (averageScore.isEmpty()) return List.of();
        return List.of(statLine(GearLoreLines.SCORE_NOUN,
                GearLoreLines.scoreValue(averageScore.getAsInt()), NamedTextColor.AQUA));
    }

    /** Label padded through the stat sheet.s own padder, then the value. One column, one owner. */
    private static Component statLine(String label, String value, NamedTextColor valueColor) {
        return MenuIcons.line(StatsSheetLines.label(label), NamedTextColor.DARK_GRAY)
                .append(MenuIcons.line(value, valueColor));
    }
}
