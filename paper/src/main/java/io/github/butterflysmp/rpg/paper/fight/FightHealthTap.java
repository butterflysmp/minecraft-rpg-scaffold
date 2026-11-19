package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;

import java.util.function.Consumer;

/**
 * THE FIGHT'S EAR ON THE STORE (step F9). Every write to the custom store passes one seam, the composite
 * {@code HealthListener} {@code CombatantStats} is built with, so a hit that lands on a fight's Mage or dragon is seen
 * here whatever path dealt it. The seam is built before the fights exist, so this is a slot bound later; until it is
 * bound it does nothing.
 *
 * <p>Placed BEFORE {@code MobDeathSystem} in the composite, so a killing hit's line is written before the death it
 * causes.
 */
public final class FightHealthTap implements HealthListener {

    private volatile Consumer<HealthChange> sink;

    public void bind(Consumer<HealthChange> sink) {
        this.sink = sink;
    }

    @Override
    public void onChange(HealthChange change) {
        Consumer<HealthChange> s = sink;
        if (s != null) s.accept(change);
    }
}
