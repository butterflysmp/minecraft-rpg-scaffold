package io.github.butterflysmp.rpg.core.mining.spike;

import java.util.Locale;

/**
 * One {@code MINESPIKE} log line: the input spike's only witness (GATE-mining-spike.md).
 * SPIKE CODE: it lives on {@code spike/mining-input} and never merges.
 *
 * <p>A {@code record} is a final class whose fields are its constructor arguments, with the
 * getters, {@code equals} and {@code toString} generated for you.
 *
 * <p><b>{@code seq} is the ORDER, not {@code tick}.</b> A packet's seq is taken on the Netty thread
 * the moment the packet arrives. An event's seq is taken on the main thread when the event fires.
 * The two counters are one {@code AtomicLong}, so a packet always gets a lower seq than the event
 * it causes. Packet lines are logged AFTER the hop to the main thread, so they can appear in the log
 * out of seq order. Sort by seq; never by line position.
 *
 * <p><b>{@code cooldown} is {@code getAttackCooldown()}, read on the main thread when the line is
 * logged.</b> On a PACKET line that is after the hop, which can be after vanilla's swing reset
 * (handleAnimate resets at bytecode 340). On an EVENT line it is read inside the vanilla handler, so
 * {@code PlayerArmSwingEvent} reads BEFORE that reset. See PLAN-mining.md F1 and F-M3.
 *
 * @param seq      the shared monotonic order
 * @param tick     {@code Bukkit.getCurrentTick()} when the line is logged
 * @param src      where the line came from
 * @param type     the packet or event kind, e.g. {@code SWING}, {@code DIG_START}, {@code ATTACK}
 * @param cooldown the attack cooldown 0..1, or null where none was read
 * @param entity   the attacked entity id plus its role ({@code 1500000000:box}), or null
 * @param aim      where the server's ray met the spiked face, or null
 */
public record MineSpikeLine(long seq, long tick, Src src, String type, Float cooldown,
                            String entity, String aim) {

    /** Where a line came from. An {@code enum} is a fixed set of named values. */
    public enum Src { PACKET, EVENT }

    public MineSpikeLine {
        if (src == null) throw new IllegalArgumentException("src is required");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type is required");
    }

    public String line() {
        return String.format(Locale.ROOT,
                "MINESPIKE seq=%d tick=%d src=%s type=%s cooldown=%s entity=%s aim=%s",
                seq, tick, src, type,
                cooldown == null ? "-" : String.format(Locale.ROOT, "%.4f", cooldown),
                dash(entity), dash(aim));
    }

    /** The placement line, logged once per spawn. {@code boxId} is null when no box was spawned. */
    public static String place(String mode, int bx, int by, int bz, String face,
                               double sx, double sy, double sz, int spotId, Integer boxId) {
        return String.format(Locale.ROOT,
                "MINESPIKE PLACE mode=%s block=%d,%d,%d face=%s spot=%.4f,%.4f,%.4f spotId=%d boxId=%s",
                mode, bx, by, bz, face, sx, sy, sz, spotId, boxId == null ? "-" : boxId.toString());
    }

    /** The teardown line. {@code reason} is {@code second_run}, {@code quit} or {@code disable}. */
    public static String stop(String mode, String reason) {
        return String.format(Locale.ROOT, "MINESPIKE STOP mode=%s reason=%s", mode, reason);
    }

    /**
     * The test pickaxe line. {@code delayTicks} is vanilla's
     * {@code getCurrentItemAttackStrengthDelay}, {@code (float)(1.0 / speed * 20.0)}, printed
     * with {@code Float.toString} so the figure is the float the server computes, not a rounding of it.
     */
    public static String pick(double speed, double modifier, float delayTicks) {
        return String.format(Locale.ROOT, "MINESPIKE PICK speed=%.2f modifier=%.2f delay=%s",
                speed, modifier, Float.toString(delayTicks));
    }

    /** The pack-offer line: where the dev server serves the zip, its SHA-1, and its size. */
    public static String pack(String url, String sha1Hex, int bytes) {
        return String.format(Locale.ROOT, "MINESPIKE PACK url=%s sha1=%s bytes=%d", url, sha1Hex, bytes);
    }

    private static String dash(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }
}
