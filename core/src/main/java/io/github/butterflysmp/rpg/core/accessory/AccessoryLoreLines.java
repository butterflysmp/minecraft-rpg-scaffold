package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The plain-text half of an accessory's tooltip and of its stats-sheet line. No Adventure, no
 * Bukkit -- the paper side only colours these. Same split, and same reason, as
 * {@code StatsSheetLines}: the text is the part worth testing.
 *
 * <h2>Numbers are printed from the AUTHORED decimal, not from double arithmetic</h2>
 *
 * {@code BigDecimal.valueOf(double)} goes through {@code Double.toString}, which gives the shortest
 * decimal that round-trips -- the number as authored. Scaling THAT by 100 is exact, so
 * {@code 0.05} prints {@code 5%}. Multiplying the double by 100 instead would be arithmetic whose
 * last digit this project has learned not to predict (see {@code StatsSheetLines} on
 * {@code 1.5000000000000002}); {@code AccessoryLoreLinesTest} asserts every shipped value.
 *
 * <p><b>Rates are the exception, deliberately:</b> health and mana regen go through
 * {@code StatsSheetLines.perFiveSeconds}, the formatter the stats sheet's own regen lines use, so an
 * accessory's "-0.20/5s Health Regen" and the sheet's "Health Regen: 0.80/5s" are one unit from one
 * formatter. That formatter rounds to two decimals, which is what absorbs the arithmetic.
 *
 * <h2>Drawbacks come last</h2>
 *
 * Bonuses first, then drawbacks, each group in {@link AccessoryStat} order. The paper side colours a
 * drawback red; the sign is in the text as well, so the tooltip does not rely on colour alone.
 */
public final class AccessoryLoreLines {

    private AccessoryLoreLines() {}

    /** A modifier's text, sign included: {@code +3 Defense}, {@code -10 Max Mana}. */
    public static String modifier(AccessoryStat stat, double amount, String classLabel) {
        String sign = amount < 0 ? "-" : "+";
        BigDecimal magnitude = BigDecimal.valueOf(Math.abs(amount));
        return switch (stat) {
            case CRIT_CHANCE, CRIT_DAMAGE -> sign + plain(magnitude.movePointRight(2)) + "% " + stat.label();
            // THE SHEET'S UNIT AND THE SHEET'S FORMATTER, not a per-second one of this class's own. The
            // stats sheet prints every rate over StatsSheetLines.RATE_WINDOW_SECONDS ("0.80/5s"), and
            // an item that said "-0.04/s" beside a total that says "0.80/5s" makes a player do the
            // conversion to check either. One unit, one formatter: the Quiver reads "-0.20/5s".
            case HEALTH_REGEN, MANA_REGEN ->
                    sign + StatsSheetLines.perFiveSeconds(Math.abs(amount)) + " " + stat.label();
            case CLASS_DAMAGE -> sign + plain(magnitude) + " " + classLabel + " " + stat.label();
            case MAX_HEALTH, MAX_MANA, DEFENSE -> sign + plain(magnitude) + " " + stat.label();
        };
    }

    /** Every modifier's text, bonuses first and drawbacks last. */
    public static List<String> modifiers(AccessoryDefinition accessory, String classLabel) {
        List<String> bonuses = new ArrayList<>();
        List<String> drawbacks = new ArrayList<>();
        for (Map.Entry<AccessoryStat, Double> m : accessory.modifiers().entrySet()) {
            (m.getValue() < 0 ? drawbacks : bonuses).add(modifier(m.getKey(), m.getValue(), classLabel));
        }
        bonuses.addAll(drawbacks);
        return bonuses;
    }

    /** True when a modifier is a drawback -- the paper side's colour decision, in one place. */
    public static boolean isDrawback(double amount) {
        return amount < 0;
    }

    /** The class a class accessory belongs to, as a player reads it: {@code Ranger}. */
    public static String className(AccessoryDefinition accessory) {
        String token = AccessorySlots.classToken(accessory.accessoryClass());
        return token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1);
    }

    /** The slot line under the modifiers: {@code Universal accessory}, {@code Ranger class accessory}. */
    public static String slotLine(AccessoryDefinition accessory) {
        return switch (accessory.slot()) {
            case UNIVERSAL -> "Universal accessory";
            case CLASS -> className(accessory) + " class accessory";
        };
    }

    /**
     * One stats-sheet line (ruling Q4): {@code Ward Charm: +3 Defense}, or for a class accessory
     * that is not contributing, {@code Sage's Scroll: inactive (requires Mage)}.
     */
    public static String sheetLine(String plainName, AccessoryDefinition accessory, boolean active,
                                   String classLabel) {
        if (!active) return plainName + ": inactive (requires " + className(accessory) + ")";
        return plainName + ": " + String.join(", ", modifiers(accessory, classLabel));
    }

    /** The heading of the stats-sheet block. */
    public static final String SHEET_HEADER = "Accessories";

    /** A slot whose stored item this server cannot read: named, never silently dropped. */
    public static String unreadableLine(int slot) {
        return "Slot " + slot + ": unreadable (kept, contributes nothing)";
    }

    /**
     * The whole stats-sheet block (ruling Q4): the header, then one line per OCCUPIED slot in slot
     * order. EMPTY when no slot is occupied -- a player wearing nothing gets no block, rather than a
     * header over nothing.
     *
     * @param worn      the definition per slot, {@code null} for an empty or unreadable slot
     * @param occupied  whether each slot holds a stored item -- what tells unreadable from empty
     * @param plainName an accessory's display name without formatting
     * @param classLabel an accessory's class-damage label ("Ranged")
     */
    public static List<String> sheetBlock(List<AccessoryDefinition> worn, List<Boolean> occupied,
                                          String profileClass,
                                          java.util.function.Function<AccessoryDefinition, String> plainName,
                                          java.util.function.Function<AccessoryDefinition, String> classLabel) {
        List<String> lines = new ArrayList<>();
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            if (!occupied.get(slot)) continue;
            AccessoryDefinition accessory = worn.get(slot);
            if (accessory == null) {
                lines.add(unreadableLine(slot));
                continue;
            }
            lines.add(sheetLine(plainName.apply(accessory), accessory,
                    AccessorySlots.contributes(accessory, profileClass), classLabel.apply(accessory)));
        }
        if (lines.isEmpty()) return lines;
        lines.add(0, SHEET_HEADER);
        return lines;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
