package io.github.butterflysmp.rpg.core.mob;

/**
 * WHO A MOB IS HOSTILE TO: the ONE named predicate (PLAN-dragon-mobs.md section 4.2 N3; the seat's ruling 24).
 *
 * <p>Ben: the Phage's blast hurts "Only players and Summons" (G), and every new mob capability takes a COMBATANT as
 * its victim or target, never specifically a player (seat ruling 3). Today the engine has one side marker, the
 * {@code player} boolean on {@code HealthState} and {@code CombatantSnapshot} (section 7 F-E), so today the answer
 * is exactly "a player". <b>A Summoner widens it HERE, by adding a {@link Side}</b>, and every capability that asks
 * this predicate follows without a rewrite.
 *
 * <p>A creative or spectator player is not a mob's target, as in vanilla, whose swell already skips them
 * ({@code EntitySelector.NO_CREATIVE_OR_SPECTATOR}, section 7 F-C).
 *
 * <p>(An {@code enum} is a fixed set of named values; {@code Side.PLAYER} is one of them.)
 */
public final class MobHostility {

    private MobHostility() {}

    /** Which side a combatant is on. A summon will be a third value. */
    public enum Side { PLAYER, MOB }

    /**
     * True when a mob treats this combatant as an enemy: its blast hurts it, its shot seeks it.
     *
     * @param exemptMode the combatant is a player in creative or spectator
     */
    public static boolean hostileTo(Side side, boolean exemptMode) {
        return side == Side.PLAYER && !exemptMode;
    }
}
