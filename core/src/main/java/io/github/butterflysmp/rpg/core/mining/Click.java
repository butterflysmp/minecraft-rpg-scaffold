package io.github.butterflysmp.rpg.core.mining;

import java.util.Optional;

/**
 * One left-click on a patch's block, as the paper side reads it at {@code BlockDamageEvent}
 * (the seat's ruling 1, 2026-11-13).
 *
 * @param cooldown       {@code getAttackCooldown()}, read before the swing's reset
 * @param aim            where the eye ray meets the patch plane, or empty if it does not
 * @param holdingPickaxe whether the item in hand is in {@code #minecraft:pickaxes}
 * @param tick           the server tick
 */
public record Click(float cooldown, Optional<PatchPoint> aim, boolean holdingPickaxe, long tick) {}
