package io.github.butterflysmp.rpg.paper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.butterflysmp.rpg.core.loot.CascadeTable;
import io.github.butterflysmp.rpg.core.loot.LootRolls;
import io.github.butterflysmp.rpg.core.loot.LootTally;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.paper.content.LootContent;
import io.github.butterflysmp.rpg.paper.mining.MiningDevCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * THE DEV ROLL COMMAND (PLAN-loot.md: the wiring order's first step, RULED by Ben 2026-11-19; the seat's queue, item 6).
 * Dev only, as {@code /rpg mining} is: {@link Permissions#DEV} AND the dev-server marker.
 * <ul>
 *   <li>{@code /rpg loot roll <source|default> [count]}: rolls that table {@code count} times (1 by default, at most
 *       {@link LootRolls#MAX_ROLLS}), with a real random source. It tells the caller, per rarity, how many gave, how
 *       many were skipped for an empty pool, and the true odds beside each; how many rolls gave nothing; and, for a
 *       source, the items picked with their counts. One {@code LOOTROLL} log line per call carries the same figures.</li>
 *   <li>{@code /rpg loot odds <source|default>}: the written chance and the true odds per tier.</li>
 * </ul>
 * <b>IT GIVES NOTHING:</b> no item enters any inventory or the world. This class never touches either.
 */
public final class LootDevCommand {

    private LootDevCommand() {}

    /** The name that means the shipped default table. */
    static final String DEFAULT = "default";

    /** A resolved table: the table, its pools (null for the default), and its name. */
    record Resolved(String name, CascadeTable table, Map<Rarity, List<String>> pools) {}

    public static LiteralCommandNode<CommandSourceStack> build(Supplier<Map<String, LootContent>> loot, Logger log,
                                                             boolean devServer) {
        return Commands.literal("loot")
                .requires(source -> MiningDevCommand.permitted(source.getSender().hasPermission(Permissions.DEV), devServer))
                .then(Commands.literal("roll")
                        .then(Commands.argument("table", StringArgumentType.word())
                                .executes(ctx -> roll(ctx, loot, log, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, LootRolls.MAX_ROLLS))
                                        .executes(ctx -> roll(ctx, loot, log, IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("odds")
                        .then(Commands.argument("table", StringArgumentType.word())
                                .executes(ctx -> odds(ctx, loot))))
                .build();
    }

    /** {@code default} is the first file's default table; any other name is a source, in any file. */
    static Resolved resolve(Map<String, LootContent> loot, String name) {
        if (loot.isEmpty()) return null;
        if (name.equals(DEFAULT)) {
            LootContent c = loot.containsKey("cascade") ? loot.get("cascade") : loot.values().iterator().next();
            return new Resolved(DEFAULT, c.defaultTable(), null);
        }
        for (LootContent c : loot.values()) {
            LootContent.Source s = c.sources().get(name);
            if (s != null) return new Resolved(name, s.table(), s.pools());
        }
        return null;
    }

    private static int roll(CommandContext<CommandSourceStack> ctx, Supplier<Map<String, LootContent>> loot, Logger log,
                            int count) {
        String name = StringArgumentType.getString(ctx, "table");
        Resolved r = resolve(loot.get(), name);
        if (r == null) return say(ctx, List.of(unknown(loot.get(), name)), NamedTextColor.RED);
        SplittableRandom rng = new SplittableRandom();
        LootTally t = LootRolls.roll(r.table(), r.pools(), count, rng::nextDouble);
        log.info(logLine(r, t));
        return say(ctx, rollLines(r, t), NamedTextColor.GOLD);
    }

    private static int odds(CommandContext<CommandSourceStack> ctx, Supplier<Map<String, LootContent>> loot) {
        String name = StringArgumentType.getString(ctx, "table");
        Resolved r = resolve(loot.get(), name);
        if (r == null) return say(ctx, List.of(unknown(loot.get(), name)), NamedTextColor.RED);
        return say(ctx, oddsLines(r), NamedTextColor.GOLD);
    }

    private static String unknown(Map<String, LootContent> loot, String name) {
        List<String> names = new ArrayList<>();
        names.add(DEFAULT);
        for (LootContent c : loot.values()) names.addAll(c.sources().keySet());
        return "No loot table '" + name + "'. Tables: " + names;
    }

    private static int say(CommandContext<CommandSourceStack> ctx, List<String> lines, NamedTextColor colour) {
        for (String l : lines) ctx.getSource().getSender().sendMessage(Component.text(l, colour));
        return 1;
    }

    private static String rarity(Rarity r) {
        return r.name().toLowerCase(Locale.ROOT);
    }

    /** The odds: each tier's written chance and its true odds, then nothing's. */
    static List<String> oddsLines(Resolved r) {
        CascadeTable.Odds o = r.table().trueOdds();
        List<String> out = new ArrayList<>();
        out.add("Loot odds '" + r.name() + "':");
        for (CascadeTable.Tier t : r.table().tiers()) {
            out.add(String.format(Locale.ROOT, "  %s: written %.2f%%, truly %.3f%%", rarity(t.rarity()),
                    100.0 * t.chance(), 100.0 * o.of(t.rarity())));
        }
        out.add(String.format(Locale.ROOT, "  nothing: truly %.3f%%", 100.0 * o.nothing()));
        return out;
    }

    /** The roll's reply: per tier gave, skipped and the true odds; nothing; the items; and that it gave nothing. */
    static List<String> rollLines(Resolved r, LootTally t) {
        CascadeTable.Odds o = r.table().trueOdds();
        List<String> out = new ArrayList<>();
        out.add(String.format(Locale.ROOT, "Loot roll '%s' x%d (nothing given to anyone):", r.name(), t.rolls()));
        for (CascadeTable.Tier tier : r.table().tiers()) {
            Rarity x = tier.rarity();
            out.add(String.format(Locale.ROOT, "  %s: %s %d (%.3f%%), skipped %d; truly %.3f%%", rarity(x),
                    r.pools() == null ? "landed" : "gave", t.gave(x), 100.0 * t.gave(x) / t.rolls(), t.skipped(x),
                    100.0 * o.of(x)));
        }
        out.add(String.format(Locale.ROOT, "  nothing: %d (%.3f%%); truly %.3f%% before skips", t.nothing(),
                100.0 * t.nothing() / t.rolls(), 100.0 * o.nothing()));
        if (r.pools() != null) out.add("  items: " + t.items());
        return out;
    }

    /** The one log line per call, with the same figures. */
    static String logLine(Resolved r, LootTally t) {
        CascadeTable.Odds o = r.table().trueOdds();
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "LOOTROLL table=%s rolls=%d", r.name(), t.rolls()));
        for (CascadeTable.Tier tier : r.table().tiers()) {
            Rarity x = tier.rarity();
            b.append(String.format(Locale.ROOT, " %s=%d/skipped:%d/truly:%.3f%%", rarity(x), t.gave(x), t.skipped(x),
                    100.0 * o.of(x)));
        }
        b.append(String.format(Locale.ROOT, " nothing=%d/truly:%.3f%% items=%d given_to_players=0", t.nothing(),
                100.0 * o.nothing(), t.itemsGiven()));
        if (r.pools() != null) b.append(" picked=").append(t.items());
        return b.toString();
    }
}
