package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Binds a real player's vanilla heart bar to the {@link HeartBar} seam. Thin and boot-witnessed:
 * the tier math and the display floor live in {@link HeartBarRenderer}, tested against a fake bar;
 * all that is here is writing the two numbers onto the player.
 *
 * Must be called on the thread that owns the player (the caller hops via the Scheduler). Sets the
 * MAX_HEALTH base BEFORE current so that, when the max rises, the new current is not clamped down by
 * a stale cap; the setHealth is then clamped to the live attribute value for safety.
 */
public final class EntityHeartBar implements HeartBar {

    private final Player player;

    public EntityHeartBar(Player player) {
        this.player = player;
    }

    @Override
    public void render(int maxHealthPoints, double healthPoints) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) max.setBaseValue(maxHealthPoints);
        double cap = max != null ? max.getValue() : maxHealthPoints;
        player.setHealth(Math.max(0.0, Math.min(cap, healthPoints)));
    }
}
