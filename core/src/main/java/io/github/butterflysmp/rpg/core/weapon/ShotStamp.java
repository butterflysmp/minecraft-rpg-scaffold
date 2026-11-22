package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;

import java.util.Optional;

/**
 * WHAT A VANILLA-DRIVEN SHOT CARRIES TO ITS HIT (the seat's ruling 10, 2026-11-22): the weapon id, the shot's force, and
 * the damage terms and gear score snapshotted at the shot, as a cast snapshots them. The hit lands through the same sum
 * and the same PLAYERHIT line as every weapon hit; vanilla's own number and its critical-arrow bonus never apply.
 *
 * <p>Written into the arrow's persistent data as ONE string ({@link #encode}), so it travels with the arrow through a
 * chunk unload or a restart. A string that does not decode is ABSENT ({@link #decode}), never a guess: such an arrow keeps
 * today's path.
 *
 * <p>The terms are snapshotted at the shot because the hit can arrive seconds later, when he may hold another weapon: the
 * score and the attack damage at the hit would be that weapon's.
 */
public record ShotStamp(String weaponId, double force, double attackDamage, double classDamageBonus,
                        double enchantDamagePercent, double critMultiplier, double levelDamageBonus, int triggerScore) {

    private static final String VERSION = "v1";
    private static final String SEP = "|";
    private static final int FIELDS = 9;

    /** The stamp of a shot, from the shooter's snapshot taken at the shot and the score his held weapon had then. */
    public static ShotStamp of(String weaponId, double force, CombatantSnapshot atShot, int triggerScore) {
        return new ShotStamp(weaponId, force, atShot.attackDamage(), atShot.classDamageBonus(),
                atShot.enchantDamagePercent(), atShot.critMultiplier(), atShot.levelDamageBonus(), triggerScore);
    }

    /** The caster at the hit: the LIVE shooter (who and where he is) carrying the STAMPED damage terms. */
    public CombatantSnapshot applyTo(CombatantSnapshot live) {
        return new CombatantSnapshot(live.id(), live.position(), live.eyeHeight(), live.alive(), live.player(),
                live.attackSpeed(), attackDamage, classDamageBonus, enchantDamagePercent, critMultiplier,
                levelDamageBonus);
    }

    /** Double.toString round-trips every finite double exactly. Weapon ids are [a-z0-9_], so the separator never occurs. */
    public String encode() {
        return String.join(SEP, VERSION, weaponId, Double.toString(force), Double.toString(attackDamage),
                Double.toString(classDamageBonus), Double.toString(enchantDamagePercent),
                Double.toString(critMultiplier), Double.toString(levelDamageBonus), Integer.toString(triggerScore));
    }

    public static Optional<ShotStamp> decode(String encoded) {
        if (encoded == null) return Optional.empty();
        String[] f = encoded.split("\\" + SEP, -1);
        if (f.length != FIELDS || !VERSION.equals(f[0]) || f[1].isBlank()) return Optional.empty();
        try {
            double[] d = new double[6];
            for (int i = 0; i < 6; i++) {
                d[i] = Double.parseDouble(f[i + 2]);
                if (!Double.isFinite(d[i])) return Optional.empty();
            }
            return Optional.of(new ShotStamp(f[1], d[0], d[1], d[2], d[3], d[4], d[5], Integer.parseInt(f[8])));
        } catch (NumberFormatException malformed) {
            return Optional.empty();
        }
    }
}
