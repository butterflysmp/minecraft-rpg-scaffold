package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.ArrivalClaims;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;

import java.util.TreeMap;

/**
 * THE TWO BOOT LINES A GATE'S R0c READS, BUILT IN ONE PLACE (the seat's follow-up 4, 2026-11-22). {@code RpgPlugin} logs
 * them, and {@code BootCountLinesTest} prints them from the same methods over the real content, so R0c's predicted
 * {@code Mint-on-craft} and {@code Convert-on-arrival} lines are MEASURED before a boot rather than derived by hand. An
 * R0 miss costs Ben a boot; a test miss costs a build.
 */
public final class BootLines {

    private BootLines() {}

    /**
     * THREE NUMBERS, AND THE THIRD IS THE ONLY REAL CONTROL. {@code size} and {@code claimed} both derive from the SAME
     * parse; {@code gearDefinitions} comes from the registries, so a parse that dropped every claim of one kind reads
     * wrong at a glance rather than self-consistent.
     */
    public static String mintOnCraft(CraftResultIndex index, int gearDefinitions) {
        return "Mint-on-craft: " + index.size() + " indexed, "
                + index.claimed() + " claiming, of " + gearDefinitions + " gear definitions"
                + (index.contested() > 0 ? ", " + index.contested() + " dropped as contested" : "")
                + (index.refused() > 0 ? ", " + index.refused() + " refused" : "");
    }

    /** The materials weapons claim when a plain item reaches a player, sorted (a TreeMap) so the line is stable. */
    public static String convertOnArrival(ArrivalClaims arrivals) {
        return "Convert-on-arrival: " + arrivals.size() + " materials " + new TreeMap<>(arrivals.all());
    }
}
