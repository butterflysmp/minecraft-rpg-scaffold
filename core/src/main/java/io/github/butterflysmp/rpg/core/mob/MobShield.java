package io.github.butterflysmp.rpg.core.mob;

import java.util.List;

/**
 * THE PHALANX'S ONE-HIT SHIELD (PLAN-dragon-mobs.md step R7b). Authored in the mob file's {@code shield:} block. Ben,
 * 2026-11-17: "1. yes the first damage of any kind". cfde822 (section 3.4, D5): "the first EntityDamageEvent of any
 * cause while it holds PHALANX_SHIELD is cancelled; shield-break sound; off hand cleared; byte removed". ONE per life:
 * nothing gives it back (READ: nothing in the record re-adds the byte).
 *
 * <p><b>NAME THE QUANTITY:</b> {@code item}: the vanilla ITEM key held in the off hand while the shield is up (cfde822:
 * a shield), never dropped. {@code sounds}, {@code particle}, {@code particleCount}: played and drawn when it absorbs
 * (cfde822: the shield-break sound; the particle NOT IN THE RECORD).
 */
public record MobShield(String item, List<BeamSound> sounds, String particle, int particleCount) {

    public MobShield {
        if (item == null || item.isBlank()) throw new IllegalArgumentException("shield.item is required");
        if (particle == null || particle.isBlank()) throw new IllegalArgumentException("shield.particle is required");
        if (particleCount < 0) throw new IllegalArgumentException("shield.particle_count must be 0 or more, was " + particleCount);
        sounds = List.copyOf(sounds);
    }

    /**
     * Does a hit of {@code amount} (the custom damage reaching the Phalanx, before its defence) spend the shield? Any
     * positive amount; a hit of zero (or a nonsense amount) does not, and the shield stays up for the next.
     *
     * <p><b>NO SHIPPED INSTRUMENT REACHES THE ZERO ARM</b> (step R7b, measured): {@code /rpg mobdamage} takes 1 or more,
     * and no gate row stages a zero hit. Its only exercise is {@code MobBlinkTest}'s shield row.
     */
    public static boolean spends(double amount) {
        return amount > 0;
    }
}
