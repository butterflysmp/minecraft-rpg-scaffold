package io.github.butterflysmp.rpg.core.mining;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The dev trace's lines (PLAN-mining.md 5.2), printed by the paper side only for a player who
 * turned the trace on. Pinned exactly by {@code MiningTraceLineTest}. {@code Locale.ROOT} keeps the
 * decimal point a dot on every machine, and an absent field is {@code -}.
 */
public final class MiningTraceLine {

    private MiningTraceLine() {}

    /**
     * @param liveIndex the live spot's 0-based index, or -1 when there is none
     * @param spotCount the patch's spot count, shown as {@code i/n}
     */
    public static String click(String player, String slotId, String outcome, int liveIndex, int spotCount,
                               float cooldown, Optional<PatchPoint> aim, long tick) {
        String spot = liveIndex < 0 ? "-" : (liveIndex + 1) + "/" + spotCount;
        String aimText = aim.map(p -> String.format(Locale.ROOT, "%.2f,%.2f", p.u(), p.v())).orElse("-");
        return String.format(Locale.ROOT, "MINING click player=%s slot=%s outcome=%s spot=%s cooldown=%.4f aim=%s tick=%d",
                player, slotId, outcome, spot, cooldown, aimText, tick);
    }

    /** {@code kept + dropped} equals the payout total: what a full inventory left at the player's feet. */
    public static String breakLine(String player, String slotId, String item, Payout payout,
                                   int kept, int dropped, long regrowAt) {
        String rolls = payout.rolls().stream().map(String::valueOf).collect(Collectors.joining("+"));
        return String.format(Locale.ROOT, "MINING break player=%s slot=%s payout=%sx%d rolls=%s bonus=%d kept=%d dropped=%d regrow_at=%d",
                player, slotId, item, payout.total(), rolls, payout.bonus(), kept, dropped, regrowAt);
    }

    /**
     * A patch shown to a player: every spot centre in reveal order, and the live spot 1-based. The
     * gate's witness for where the spots are (M-HIT's aim, M-REGROW's re-roll).
     */
    public static String show(String player, String slotId, java.util.List<Spot> spots, int liveIndex, long tick) {
        String centres = spots.stream().map(s -> s.cu() + "," + s.cv()).collect(Collectors.joining(";"));
        return String.format(Locale.ROOT, "MINING show player=%s slot=%s spots=%s live=%d/%d tick=%d",
                player, slotId, centres, liveIndex + 1, spots.size(), tick);
    }

    public static String regrow(String player, String slotId, int spots, long tick) {
        return String.format(Locale.ROOT, "MINING regrow player=%s slot=%s spots=%d tick=%d",
                player, slotId, spots, tick);
    }
}
