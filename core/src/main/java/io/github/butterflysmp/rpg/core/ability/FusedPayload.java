package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.List;

/**
 * WHAT A FUSED PROJECTILE MAY CARRY (the seat's F3, 2026-11-26, the Blast Fungus). A fused chunk does not land ON a body:
 * it goes off WHERE IT IS, so its on_hit is a DETONATION, applied by {@code EffectApplier.detonateFused} at the chunk's
 * point: a {@code visual} plays there, and a {@code burst} lands on every body in its radius that the sides table admits
 * on {@code PvpPath.FUSED_BURST}. A top-level targeted effect (damage, knockback, a status) would name a single victim a
 * detonation does not have, so it is refused here, by the ability's id, at load.
 *
 * <p>CC'S CALL (2026-11-26): visuals and bursts only, at least one burst. The alternative, landing the targeted effects on
 * the mob a chunk touched in flight, gives a touch a second hit the fuse does not, which no ruling covers. Also refused:
 * a fused projectile as a volley's repeated shot (no ruling covers a fused volley).
 */
public final class FusedPayload {

    private FusedPayload() {}

    /** Throws, naming {@code abilityId}, when {@code cast} is fused and {@code onHit} is not a detonation. */
    public static void check(String abilityId, CastSpec cast, List<EffectSpec> onHit) {
        if (cast instanceof CastSpec.Volley v && v.of() instanceof CastSpec.Projectile p && p.fused()) {
            throw new IllegalArgumentException("ability '" + abilityId + "': a volley cannot repeat a fused projectile");
        }
        if (!(cast instanceof CastSpec.Projectile p) || !p.fused()) return;
        boolean burst = false;
        for (EffectSpec effect : onHit) {
            if (effect instanceof EffectSpec.Burst) {
                burst = true;
            } else if (!(effect instanceof EffectSpec.Visual)) {
                throw new IllegalArgumentException("ability '" + abilityId + "': a fused projectile's on_hit is a"
                        + " detonation, visuals and bursts only; '" + effect.getClass().getSimpleName()
                        + "' names a single victim a detonation does not have");
            }
        }
        if (!burst) {
            throw new IllegalArgumentException("ability '" + abilityId + "': a fused projectile's on_hit needs a burst,"
                    + " or it goes off and touches nothing");
        }
    }
}
