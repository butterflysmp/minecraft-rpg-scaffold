package io.github.butterflysmp.rpg.paper.selftest;

import java.util.List;

/**
 * One gate row's scenario: the steps the instrument drives, keyed by the gate's OWN row id
 * ({@code GATE-withered-shortbow.md}'s {@code ### WB2} is {@code withered-shortbow}/{@code WB2}).
 *
 * <p>A {@code record} is a class whose fields are fixed at construction and which gets {@code equals},
 * {@code hashCode} and accessors ({@code gate()}, {@code row()}, {@code steps()}) for free.
 *
 * <p>{@link #SETUP} is not a row: it is the gate file's {@code setup:} block, run once before that gate's first row.
 */
public record Scenario(String gate, String row, List<Step> steps) {

    public static final String SETUP = "setup";

    public Scenario {
        steps = List.copyOf(steps);
    }

    /** Where the scenario is written, as the START line names it: {@code selftest/GATE-level.yml#WD1}. */
    public String source() {
        return "selftest/GATE-" + gate + ".yml#" + row;
    }

    /** One step. {@code arg} is the YAML scalar as written; each {@link Verb} says what it means. */
    public record Step(Verb verb, String arg) {}

    /**
     * The fixed vocabulary. Each verb is implemented ONCE, in {@link SelfTestDrivers}, and the drivers are the only
     * code that reaches a shipped path. A row is added in YAML; a verb is added in Java.
     */
    public enum Verb {
        /** {@code Player#performCommand}: the server's one Brigadier dispatcher, as typed. Logged as a CMD line. */
        CMD,
        /** Real ticks on the player's own scheduler. The only way a scenario waits. */
        WAIT,
        /** Turn {@code /rpg mobtrace} ON (it has no setter; the toggle is read back). */
        TRACE,
        /** {@code Player#attack} on the nearest entity carrying this scoreboard tag. */
        ATTACK,
        /** A synthetic {@code PlayerInteractEvent RIGHT_CLICK_AIR, HAND} into every listener, ours included. */
        USE,
        /** A synthetic {@code PlayerArmSwingEvent(HAND)}: the stone's Left. */
        SWING,
        /** {@code HumanEntity#dropItem(false)}: the Q key's own {@code ServerPlayer.drop}. */
        DROP,
        /** Select a hotbar slot holding this ({@code stone}, {@code empty} or a weapon id), firing {@code PlayerItemHeldEvent}. */
        HOLD,
        /** A synthetic left {@code InventoryClickEvent} on the open menu's first slot whose name starts with this text. */
        CLICK,
        /** The same, on the first slot of this material. */
        CLICK_MATERIAL,
        /** {@code Player#closeInventory}. */
        CLOSE,
        /** Log the open screen ({@code menu}) or the held item ({@code held}): names, lore, colours as the server built them. */
        DUMP,
        /** Log every entity within 16 blocks carrying this tag: uuid, type, custom HP, fire ticks, distance. */
        PROBE,
        /** Log the inventory slots holding this weapon id. */
        PROBE_INVENTORY,
        /** {@code "<MATERIAL> dx1 dy1 dz1 dx2 dy2 dz2"}: count that block in a box relative to the player's block. */
        PROBE_BLOCKS,
        /** {@code "<entity type> <radius>"}: count those entities near the player. */
        PROBE_ENTITIES,
        /** {@code "dx1 dy1 dz1 dx2 dy2 dz2"}: remember those blocks, so the run's restore puts them back. */
        SAVE_BLOCKS,
        /** {@code "<class> <element>"}: SKIPPED unless the profile's Build cell is this one. */
        EXPECT_CELL,
        /** {@code "<row>"}: SKIPPED unless that row ran, unskipped, immediately before this one in the same run. */
        AFTER,
        /** A line for the reader. Changes nothing. */
        NOTE
    }
}
