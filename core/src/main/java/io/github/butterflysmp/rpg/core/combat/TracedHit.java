package io.github.butterflysmp.rpg.core.combat;

import java.util.Locale;
import java.util.UUID;

/**
 * ONE ABILITY OR WEAPON HIT ON A NON-PLAYER TARGET, as {@code EffectApplier} landed it -- the fact behind
 * the {@code PLAYERHIT} line that {@code /rpg mobtrace} prints (PLAN-melee-class.md, the seat's ruling 2
 * on Q8).
 *
 * <h2>WHAT {@code amount} IS, NAMED BECAUSE THE POPUP SHOWS A DIFFERENT NUMBER</h2>
 *
 * <p>{@code amount} is what the applier SENT to the target's port: after the gear score, the enchant
 * percentage, the class bonus, the charge and the crit -- and <b>BEFORE the target's Defense</b>, which
 * {@code CombatantStats.damage} applies downstream on the target's own thread. The popup and the
 * nameplate show the post-Defense figure. So two {@code PLAYERHIT} lines on the same mob compare
 * exactly, and a {@code PLAYERHIT} line against a popup compares only when the target has no Defense.
 * The line says {@code sent=} rather than {@code amount=} so a reader cannot mistake which one it is.
 *
 * <p>Recomputing the post-Defense number here would be a second site applying the curve, which
 * {@code CombatantStats.damage}'s javadoc names as the thing not to do.
 *
 * <h2>WHAT DOES NOT PRODUCE ONE</h2>
 *
 * <p>Only {@code EffectApplier}'s two DIRECT damage arms report, and only inside their gates, so a
 * refused hit (a non-positive amount, a dead target) reports nothing. Scorch's burn tick and Ignite's
 * blast do not pass through them and are not traced. A hit on a PLAYER is not traced either: the line is
 * player-to-mob, the twin of {@code MOBHIT}.
 *
 * @param casterId     who cast it
 * @param source       the ability id, or {@code <weapon>/<input>} for a weapon trigger. Null only for a
 *                     {@code Caster} no cast path built; printed as {@code -}
 * @param targetId     who took it
 * @param element      the element the damage effect wore
 * @param sent         the amount sent to the target, before its Defense (above)
 * @param crit         whether the cast rolled a crit -- a crit changes {@code sent}, so a row comparing two
 *                     lines has to see it
 * @param triggerScore the gear score that scaled a literal {@code damage}. The {@code weapon_damage} arm
 *                     ignores it (the stat already carries the score), and the line prints it anyway
 */
public record TracedHit(UUID casterId, String source, UUID targetId, String element,
                        double sent, boolean crit, int triggerScore) {

    /**
     * The log line. {@code targetType} is the adapter's (a Minecraft entity key such as {@code zombie}),
     * because core does not know what a zombie is.
     */
    public String line(String targetType) {
        return String.format(Locale.ROOT,
                "PLAYERHIT caster=%s source=%s target=%s %s element=%s sent=%.3f crit=%s triggerScore=%d",
                casterId, source == null ? "-" : source, targetId, targetType, element, sent, crit,
                triggerScore);
    }
}
