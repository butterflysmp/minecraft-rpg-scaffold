package io.github.yourname.rpg.core.combat;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.element.Element;
import java.util.UUID;

/**
 * Anything that can be hit. A player, a boss, a training dummy in a unit test.
 * The Paper adapter wraps org.bukkit.entity.LivingEntity in one of these.
 */
public interface Combatant {
    UUID id();
    Vec3 position();
    boolean isAlive();

    /** Shield element, or null if unshielded. Drives Element.multiplierAgainst. */
    Element shieldElement();

    void applyDamage(double amount, Element element);
    void applyHeal(double amount);
    void applyKnockback(Vec3 direction, double strength);
    void applyStatus(String statusId, int durationTicks, int amplifier);
}
