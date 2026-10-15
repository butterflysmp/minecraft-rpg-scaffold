package io.github.butterflysmp.rpg.paper.adapter;

import java.util.Locale;
import java.util.UUID;

/**
 * One damage-over-time tick, as the {@code DOTTICK} trace line reports it (WITHER-STATUS; the seat's S3
 * ruling, 2026-09-30: <i>"a DOTTICK line under /rpg mobtrace, carrying the target's uuid and type, the
 * status id, the applier's uuid, the amount sent, the cap in force, and the tick time. It is emitted
 * for BOTH scorch and wither, from the shared store"</i>).
 *
 * <p><b>Why it exists:</b> a DoT tick reaches {@code applyDamage} from the store's sink, not from
 * {@code CastExecutor}, so it has no {@code PLAYERHIT} line. Before this the tick rows' only witness was
 * the damage popup, read by eye. With it the tick rows are LOG-LINE rows, and the popup is secondary.
 *
 * <p><b>{@code sent} is what the store SENT</b>, {@code min(rate x max, cap)}, before Defense (which a DoT
 * bypasses anyway) and before the custom-health clamp. It is logged BEFORE the damage is dealt, so the
 * tick that kills a mob still has its line, ahead of the death.
 *
 * @param applier null for a tick with no applier on record
 */
public record DotTick(UUID victim, String victimType, String statusId, UUID applier,
                     double sent, double cap) {

    /**
     * The log line, at server tick {@code tick}:
     * {@code DOTTICK <victim> <type> status=<id> applier=<uuid|-> sent=<%.3f> cap=<%.3f> tick=<n>}.
     * {@code Locale.ROOT}, so the decimal point is a point on every machine (MOBHEAL's reason).
     */
    public String line(long tick) {
        return String.format(Locale.ROOT, "DOTTICK %s %s status=%s applier=%s sent=%.3f cap=%.3f tick=%d",
                victim, victimType, statusId, applier == null ? "-" : applier, sent, cap, tick);
    }
}
