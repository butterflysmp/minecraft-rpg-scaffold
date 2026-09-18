package io.github.butterflysmp.rpg.paper.weapon;

import com.mojang.brigadier.context.CommandContext;
import io.github.butterflysmp.rpg.core.weapon.GearLoreLines;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.GearScoreBand;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * {@code /rpg gearscore} -- the dev read-back, and the only way to put a chosen score on an item.
 *
 * <h2>*** WITHOUT THIS COMMAND, NOT ONE GATE ROW IN THIS SLICE IS STAGEABLE. PR 1's LESSON. ***</h2>
 *
 * <p>Every claim the gear-score slice makes is about a REAL {@code ItemStack} with a REAL PDC value,
 * and <b>no unit test in any module can construct one</b> -- there is no MockBukkit and
 * {@code new ItemStack(...)} needs a running server. The two claims that cost the most are worse than
 * that:
 *
 * <ul>
 *   <li><b>Two scores of one definition deal damage in the ratio of their scores.</b> Staging it needs
 *       two items of the same weapon at two chosen scores. A roll cannot produce that on demand --
 *       it bands on the player's average -- so {@link #set} is the only way to get there.
 *   <li><b>An item minted before this slice deals exactly the damage it dealt yesterday.</b> Staging
 *       it needs an item with NO STAMP AT ALL, which no acquisition path can now produce. {@link #clear}
 *       is the only way to manufacture a legacy item, and without it that row would require an
 *       inventory saved from before the build.
 * </ul>
 *
 * <p>So this is not convenience tooling. It is the instrument, and the same argument
 * {@code VaultDevCommand} makes for PR 1 applies word for word.
 *
 * <h2>GATED ON {@code Permissions.DEV}, AND THAT IS NOT A FORMALITY</h2>
 *
 * {@link #set} multiplies a weapon's damage and an armour piece's Defense by up to 5x on any item the
 * player is holding. <b>Ungated, that is a damage cheat</b> -- a narrower exposure than
 * {@code /rpg vault}'s item duplicator but the same kind, and it carries the same node. The gate is
 * asserted by {@code GearScoreWiringSignatureTest}.
 *
 * <h2>{@link #show} REPORTS THE SIX SLOTS, NOT JUST THE AVERAGE</h2>
 *
 * A single averaged number cannot distinguish the failures this slice can have. An average of 100
 * could be six baseline items, or one 400 weapon with five empty slots read as 100 apiece, or the
 * top-two selection picking the wrong two. <b>Printing the six contributions makes the arithmetic
 * checkable from the chat line</b>, which is what lets a gate row read a prediction off the screen
 * before taking a hit -- the same job {@code EnchantEffectLine} does for enchants.
 */
public final class GearScoreDevCommand {

    private GearScoreDevCommand() {}

    /** Usage, printed for any incomplete form. */
    public static int usage(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Usage: /rpg gearscore <show|set|clear> [score]",
                NamedTextColor.RED));
        sender.sendMessage(Component.text(
                "Scores are " + GearScore.MIN + ".." + GearScore.HARD_CAP + "; drops clamp at "
                        + GearScore.SOFT_CAP + ". set/clear act on the item in your MAIN HAND.",
                NamedTextColor.GRAY));
        return 0;
    }

    /**
     * Report the held item's score, the player's average, and the six contributions behind it.
     *
     * <p>The six are printed in the order core averages them -- four armour slots, then the two
     * highest hands -- so a reader can add them up and divide by six against the reported average. A
     * report that only stated the average would be unfalsifiable by eye.
     */
    public static int show(CommandContext<CommandSourceStack> ctx, AdapterContext adapters) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Run this as a player.", NamedTextColor.RED));
            return 0;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        OptionalInt stamped = GearScoreItems.read(held, adapters.keys());

        // ABSENT AND ZERO ARE REPORTED DIFFERENTLY, deliberately, and this is the one surface where
        // the difference is visible to an operator. "no stamp (reads 100)" is a legacy item behaving
        // correctly; a printed 100 is an item that was actually rolled at the floor. A report that
        // collapsed them would make the absent=100 gate row unreadable -- it would look identical to
        // a row staged on a genuine 100.
        player.sendMessage(Component.text("Held: ", NamedTextColor.AQUA)
                .append(Component.text(stamped.isPresent()
                                ? GearLoreLines.scoreValue(stamped.getAsInt())
                                : "no stamp (reads " + GearScore.ABSENT + ")",
                        NamedTextColor.WHITE)));

        int[] armor = GearScoreItems.armorScores(player, adapters.keys());
        int[] hands = GearScoreItems.handScores(player, adapters.keys());
        int[] six = GearScore.sixSlots(armor, hands);
        int average = GearScore.averageOf(six);

        player.sendMessage(Component.text("Slots: ", NamedTextColor.AQUA)
                .append(Component.text(Arrays.toString(six), NamedTextColor.WHITE))
                .append(Component.text("  (armour x4, then the two best hands)",
                        NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("Hand pool: ", NamedTextColor.AQUA)
                .append(Component.text(Arrays.toString(hands), NamedTextColor.WHITE))
                .append(Component.text("  (hotbar + offhand, scoreable only -- tools excluded)",
                        NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text(GearLoreLines.SCORE_LABEL, NamedTextColor.AQUA)
                .append(Component.text(GearLoreLines.scoreValue(average), NamedTextColor.WHITE))
                .append(Component.text("  average over " + GearScore.AVERAGE_SLOTS + " slots",
                        NamedTextColor.DARK_GRAY)));

        // THE WHOLE BAND IS REPORTED, NOT ONE DRAW FROM IT -- the range is what a gate row predicts
        // against, and a single sampled roll could not be told from a band that collapsed. Ben ruled
        // average-5 to average+15; both ends print clamped, so the floor and the soft cap are visible
        // exactly where they start to bite.
        player.sendMessage(Component.text("Next drop rolls in: ", NamedTextColor.AQUA)
                .append(Component.text(GearScore.roll(average, GearScoreBand.SPREAD,
                                GearScoreBand.SKEW, 0.0) + ".." + GearScore.roll(average,
                                GearScoreBand.SPREAD, GearScoreBand.SKEW, Math.nextDown(1.0)),
                        NamedTextColor.WHITE))
                .append(Component.text("  (band spread " + GearScoreBand.SPREAD + ", skew "
                                + GearScoreBand.SKEW + " -- RULED: average-5 to average+15, clamped)",
                        NamedTextColor.DARK_GRAY)));
        return 1;
    }

    /**
     * Stamp a chosen score onto the held item.
     *
     * <p><b>Re-renders the tooltip, and that is not cosmetic.</b> A per-item value that is RENDERED
     * has to be re-rendered wherever it is WRITTEN, or the display is only ever correct at mint --
     * which was boot row V1's failure for the quiver count, in this same codebase. Without the
     * re-mint below, {@code set} would move the damage and leave the tooltip saying the old score,
     * and a gate row reading the number off the screen would read a lie.
     *
     * <p>A re-mint rather than a lore patch, because {@code GearItems.remint} is the one call that
     * regenerates display from current content while carrying every piece of instance data -- the
     * score included, now that {@code carryInstanceData} moves it. Writing the PDC and calling
     * {@code refreshLore} directly would work for weapons and silently skip armour and shields,
     * which have no such door.
     */
    public static int set(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                          GearLookup lookup, int score) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Run this as a player.", NamedTextColor.RED));
            return 0;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(Component.text("Hold the item you want to score.",
                    NamedTextColor.YELLOW));
            return 0;
        }

        held.editMeta(meta -> GearScoreItems.write(meta, score, adapters.keys()));
        int written = GearScore.clamp(score);
        lookup.remint(player, held, adapters);

        player.sendMessage(Component.text("Set ", NamedTextColor.AQUA)
                .append(Component.text(GearLoreLines.SCORE_LABEL + GearLoreLines.scoreValue(written),
                        NamedTextColor.WHITE))
                .append(written == score ? Component.empty()
                        : Component.text(" (clamped from " + score + ")", NamedTextColor.GRAY)));
        return 1;
    }

    /**
     * Remove the held item's stamp, making it a LEGACY item again.
     *
     * <p><b>This is the only way to manufacture the absent=100 case, and that case is the slice's
     * least testable claim.</b> No acquisition path can produce an unstamped scoreable item any more,
     * so without this the row "an item minted before this slice deals exactly the damage it dealt
     * yesterday" could only be staged from an inventory saved before the build -- which is not a
     * repeatable gate row.
     *
     * <p>Reports the reading it now has rather than just "cleared": the whole point of the case is
     * that absence RESOLVES to something, and an operator has to see which number.
     */
    public static int clear(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                           GearLookup lookup) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Run this as a player.", NamedTextColor.RED));
            return 0;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir() || !held.hasItemMeta()) {
            player.sendMessage(Component.text("Hold the item you want to un-score.",
                    NamedTextColor.YELLOW));
            return 0;
        }

        held.editMeta(meta ->
                meta.getPersistentDataContainer().remove(adapters.keys().gearScore));
        lookup.remint(player, held, adapters);

        player.sendMessage(Component.text("Cleared the stamp -- this item is now a LEGACY item and"
                + " reads " + GearScore.ABSENT + ".", NamedTextColor.AQUA));
        return 1;
    }

    /**
     * How this command re-mints an arbitrary held item after writing its PDC.
     *
     * <p>An interface rather than a direct call, because resolving a stack to its
     * {@code GearDefinition} needs all four gear registries, and those live in {@code RpgCommand}
     * where the command tree is built. <b>Threading four registries into this class would be the
     * five-signature detour {@code AdapterContext}'s javadoc exists to refuse</b>; handing over the
     * one operation instead keeps the lookup where the registries already are.
     *
     * <p>A no-op implementation is legal and is what an item of no known kind gets: the PDC write
     * above still happened, and only the tooltip is stale. <b>Said explicitly because a silent no-op
     * is normally this project's enemy</b> -- here it is bounded, because {@code set} on something
     * that is not ours has nothing to render a score onto in the first place.
     */
    public interface GearLookup {
        void remint(Player player, ItemStack held, AdapterContext adapters);
    }
}
