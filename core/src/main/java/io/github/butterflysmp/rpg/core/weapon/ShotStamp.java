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
                        double enchantDamagePercent, double critMultiplier, double levelDamageBonus, int triggerScore,
                        java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveEnchant> behaviourEnchants) {

    private static final String VERSION = "v2";
    /** The shape before B2 (2026-11-23): nine fields, no behaviour enchants. Still read, as carrying none. */
    private static final String VERSION_ONE = "v1";
    private static final String SEP = "|";
    private static final int FIELDS = 10;
    private static final int FIELDS_ONE = 9;

    /**
     * B2 (2026-11-23): the BEHAVIOURAL enchants active on the bow at the shot, as id and level; the paper side resolves
     * them against the registry at the hit, so the arrow lands with the bow that RELEASED it. Never null.
     */
    public ShotStamp {
        behaviourEnchants = behaviourEnchants == null ? java.util.List.of() : java.util.List.copyOf(behaviourEnchants);
    }

    /** The stamp of a shot, from the shooter's snapshot taken at the shot and the score his held weapon had then. */
    public static ShotStamp of(String weaponId, double force, CombatantSnapshot atShot, int triggerScore) {
        return of(weaponId, force, atShot, triggerScore, java.util.List.of());
    }

    /** The same, with the bow's behavioural enchants at the shot (B2). */
    public static ShotStamp of(String weaponId, double force, CombatantSnapshot atShot, int triggerScore,
                               java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveEnchant> behaviourEnchants) {
        return new ShotStamp(weaponId, force, atShot.attackDamage(), atShot.classDamageBonus(),
                atShot.enchantDamagePercent(), atShot.critMultiplier(), atShot.levelDamageBonus(), triggerScore,
                behaviourEnchants);
    }

    /** {@code id:level;id:level}, or empty. An enchant id is a file name, so it holds neither separator. */
    private static String encodeEnchants(java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveEnchant> enchants) {
        StringBuilder b = new StringBuilder();
        for (var e : enchants) {
            if (b.length() > 0) b.append(';');
            b.append(e.enchantId()).append(':').append(e.level());
        }
        return b.toString();
    }

    /** The inverse; throws NumberFormatException or IllegalArgumentException on a malformed entry (decode fails closed). */
    private static java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveEnchant> decodeEnchants(String raw) {
        if (raw.isEmpty()) return java.util.List.of();
        java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveEnchant> out = new java.util.ArrayList<>();
        for (String part : raw.split(";", -1)) {
            int colon = part.lastIndexOf(':');
            if (colon <= 0) throw new NumberFormatException("no level in '" + part + "'");
            out.add(new io.github.butterflysmp.rpg.core.enchant.ActiveEnchant(part.substring(0, colon),
                    Integer.parseInt(part.substring(colon + 1))));
        }
        return out;
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
                Double.toString(critMultiplier), Double.toString(levelDamageBonus), Integer.toString(triggerScore), encodeEnchants(behaviourEnchants));
    }

    public static Optional<ShotStamp> decode(String encoded) {
        if (encoded == null) return Optional.empty();
        String[] f = encoded.split("\\" + SEP, -1);
        boolean one = f.length == FIELDS_ONE && VERSION_ONE.equals(f[0]);
        boolean two = f.length == FIELDS && VERSION.equals(f[0]);
        if (!(one || two) || f[1].isBlank()) return Optional.empty();
        try {
            double[] d = new double[6];
            for (int i = 0; i < 6; i++) {
                d[i] = Double.parseDouble(f[i + 2]);
                if (!Double.isFinite(d[i])) return Optional.empty();
            }
            return Optional.of(new ShotStamp(f[1], d[0], d[1], d[2], d[3], d[4], d[5], Integer.parseInt(f[8]),
                    two ? decodeEnchants(f[9]) : java.util.List.of()));
        } catch (IllegalArgumentException malformed) {   // NumberFormatException is one; so is a bad ActiveEnchant
            return Optional.empty();
        }
    }
}
