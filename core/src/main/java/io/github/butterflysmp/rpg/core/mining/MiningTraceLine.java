package io.github.butterflysmp.rpg.core.mining;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The dev trace's lines (PLAN-mining.md 5.2, 5.12), printed by the paper side only for a player who
 * turned the trace on, unless a method says it is always logged. Pinned exactly by
 * {@code MiningTraceLineTest}. {@code Locale.ROOT} keeps the decimal point a dot on every machine,
 * and an absent field is {@code -}.
 *
 * <p>S2 adds the patch's rolled {@code size=} to the show, break and regrow lines, the click line's
 * {@code miss_sound=} field, the hold line's {@code sound=} field, and three new lines:
 * {@code click-outside}, {@code roll} and {@code roll-refused}.
 */
public final class MiningTraceLine {

    private MiningTraceLine() {}

    /** The click line's {@code miss_sound=} value for any outcome other than MISS. */
    public static final String NO_MISS = "-";

    /** The click line's {@code miss_sound=} value for a MISS: whether the sound PLAYED (S2, ruling 9). */
    public static String missSound(boolean played) {
        return played ? "yes" : "no";
    }

    /**
     * @param liveIndex the live spot's 0-based index, or -1 when there is none
     * @param spotCount the patch's spot count, shown as {@code i/n}
     * @param missSound {@link #missSound(boolean)} for a MISS, else {@link #NO_MISS}
     */
    public static String click(String player, String slotId, String outcome, int liveIndex, int spotCount,
                               float cooldown, Optional<PatchPoint> aim, long tick, String missSound) {
        String spot = liveIndex < 0 ? "-" : (liveIndex + 1) + "/" + spotCount;
        String aimText = aim.map(p -> String.format(Locale.ROOT, "%.2f,%.2f", p.u(), p.v())).orElse("-");
        return String.format(Locale.ROOT, "MINING click player=%s slot=%s outcome=%s spot=%s cooldown=%.4f aim=%s tick=%d miss_sound=%s",
                player, slotId, outcome, spot, cooldown, aimText, tick, missSound);
    }

    /**
     * S2 (the seat's ruling 4): a click on a block of the slot OUTSIDE the current patch's footprint,
     * judged as nothing. Traced.
     */
    public static String clickOutside(String player, String slotId, String sizeId, BlockPos block, long tick) {
        return String.format(Locale.ROOT, "MINING click-outside player=%s slot=%s size=%s block=%d,%d,%d tick=%d",
                player, slotId, sizeId, block.x(), block.y(), block.z(), tick);
    }

    /** {@code kept + dropped} equals the payout total: what a full inventory left at the player's feet. */
    public static String breakLine(String player, String slotId, String sizeId, String item, Payout payout,
                                   int kept, int dropped, long regrowAt) {
        String rolls = payout.rolls().stream().map(String::valueOf).collect(Collectors.joining("+"));
        return String.format(Locale.ROOT, "MINING break player=%s slot=%s size=%s payout=%sx%d rolls=%s bonus=%d kept=%d dropped=%d regrow_at=%d",
                player, slotId, sizeId, item, payout.total(), rolls, payout.bonus(), kept, dropped, regrowAt);
    }

    /**
     * A patch shown to a player: its rolled size, every spot centre in reveal order, and the live spot
     * 1-based. The gate's witness for where the spots are and which size rolled.
     */
    public static String show(String player, String slotId, String sizeId, java.util.List<Spot> spots,
                              int liveIndex, long tick) {
        String centres = spots.stream().map(s -> s.cu() + "," + s.cv()).collect(Collectors.joining(";"));
        return String.format(Locale.ROOT, "MINING show player=%s slot=%s size=%s spots=%s live=%d/%d tick=%d",
                player, slotId, sizeId, centres, liveIndex + 1, spots.size(), tick);
    }

    /**
     * S1-c: a notice took the player's action bar from {@code start} until {@code until}. Traced. S2
     * (ruling 8): {@code sound=} says whether the DENIED sound played, i.e. no hold was live.
     */
    public static String hold(String player, String reason, long start, long until, boolean soundPlayed) {
        return String.format(Locale.ROOT, "MINING hold player=%s reason=%s start=%d until=%d sound=%s",
                player, reason, start, until, soundPlayed ? "yes" : "no");
    }

    /**
     * S1-c: {@code /rpg mining place} refused, with the chat text verbatim to the end of the line.
     * Always logged, like {@code MINING place}: M-PLACE's refusals are then a log reading.
     */
    public static String placeRefused(String player, String reason) {
        return "MINING place-refused player=" + player + " reason=" + reason;
    }

    /** S2 (ruling 7): {@code /rpg mining roll} forced a fresh patch. Always logged. */
    public static String roll(String player, String slotId, String sizeId, int spots) {
        return String.format(Locale.ROOT, "MINING roll player=%s slot=%s size=%s spots=%d",
                player, slotId, sizeId, spots);
    }

    /** S2 (ruling 7): {@code /rpg mining roll} refused, the chat text verbatim to the end. Always logged. */
    public static String rollRefused(String player, String reason) {
        return "MINING roll-refused player=" + player + " reason=" + reason;
    }

    public static String regrow(String player, String slotId, String sizeId, int spots, long tick) {
        return String.format(Locale.ROOT, "MINING regrow player=%s slot=%s size=%s spots=%d tick=%d",
                player, slotId, sizeId, spots, tick);
    }
}
