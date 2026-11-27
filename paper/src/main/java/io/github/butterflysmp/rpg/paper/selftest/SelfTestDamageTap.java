package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;

/**
 * THE SELFTEST'S EAR ON THE STORE (dragon-mobs step R3, the instrument): every DAMAGE change to a player reaches the
 * running row, which logs its HURT line ({@link SelfTestBlast#hurtLine}). One more listener in the store's
 * {@code CompositeHealthListener}, so it hears exactly what the displays hear, and no second path.
 *
 * <p><b>Bound late, because of construction order:</b> the store is built before the selftest service exists
 * ({@code RpgPlugin}), so the tap is in the composite from the start and is handed the service afterwards. Until then,
 * and on a server where no run is active, it does nothing. It never judges, and it never writes.
 */
public final class SelfTestDamageTap implements HealthListener {

    private volatile SelfTestService service;

    public void bind(SelfTestService service) {
        this.service = service;
    }

    @Override
    public void onChange(HealthChange change) {
        if (!change.targetIsPlayer()) return;
        SelfTestService s = service;
        if (s == null) return;
        if (change.kind() == HealthChange.Kind.DAMAGE) s.onPlayerHurt(change);
        // AND EVERY HEAL TO HIM (the Healing Tome's gate, 2026-11-26): his regeneration's and the pulse's alike, so a health
        // reading names every term between two readings. A HEALED line; never judged.
        if (change.kind() == HealthChange.Kind.HEAL) s.onPlayerHealed(change);
    }
}
