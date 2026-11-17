package io.github.butterflysmp.rpg.core.mob;

import java.util.Set;

/**
 * WHICH VANILLA DAMAGE IS AN ATTACK ON THE PHALANX'S SHOT (dragon-mobs step R7a; the seat's ruling 1 on #201). Ben:
 * "Any attack destroys it". The boot of {@code 8ec16e89} read a wall's SUFFOCATION destroying a shot, which is not an
 * attack. So:
 * <ul>
 *   <li><b>ATTACK</b>: a melee hit or sweep, a projectile, an entity's explosion, magic or a sonic boom, CAUSED BY A
 *       COMBATANT (the damage source's causing entity is a living body: a player, or a mob). It destroys the shot.
 *       Every one of OUR ability paths is an attack too, but it never raises a vanilla event, so it never reaches this
 *       table ({@code BukkitCombatant}'s handles strike first).</li>
 *   <li><b>END</b>: {@code /kill} ({@code KILL}). It ends the shot, as before.</li>
 *   <li><b>REFUSED</b>: everything else, and any attack-shaped cause with no combatant behind it (a dispenser's arrow, a
 *       bed's blast): the event is cancelled, nothing happens to the shot, and paper logs the refusal once per cause per
 *       shot. A block ends the shot by OUR check (the carrier's box swept along its path), never by damage.</li>
 * </ul>
 * The names are Bukkit's {@code EntityDamageEvent.DamageCause} names, so core needs no Bukkit type. An unknown cause
 * (a later Paper's) is REFUSED, never an attack.
 */
public final class ShotDamage {

    private ShotDamage() {}

    public enum Verdict { ATTACK, REFUSED, END }

    /** The causes that are an attack when a combatant caused them. */
    static final Set<String> ATTACK_CAUSES = Set.of("ENTITY_ATTACK", "ENTITY_SWEEP_ATTACK", "PROJECTILE",
            "ENTITY_EXPLOSION", "MAGIC", "SONIC_BOOM");

    /**
     * @param cause        the damage cause's name
     * @param byCombatant  the damage source's causing entity is a living body
     */
    public static Verdict classify(String cause, boolean byCombatant) {
        if ("KILL".equals(cause)) return Verdict.END;
        if (byCombatant && ATTACK_CAUSES.contains(cause)) return Verdict.ATTACK;
        return Verdict.REFUSED;
    }
}
