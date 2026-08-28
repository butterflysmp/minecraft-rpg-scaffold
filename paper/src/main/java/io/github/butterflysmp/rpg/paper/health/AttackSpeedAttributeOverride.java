package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.AttackSpeedAttribute;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/**
 * Driving the player's vanilla ATTACK_SPEED attribute from the weapon they hold and the attack-speed
 * stat they carry.
 *
 * <p>This is what makes the stat mean anything for basic melee. Since vanilla's crosshair attack took
 * over the melee hit, the thing that paces a swing -- and fills the crosshair indicator, and sets the
 * charge {@code AttackCharge} scales by -- is this attribute and nothing else. Stage 1 pinned it
 * statically onto the item at mint, so a speed boost moved the stat and changed no swing.
 *
 * <p>The sibling of {@link ArmorBarOverride}, and built from it deliberately: that class is the only
 * other place in this project that writes an attribute modifier onto a live player, and its shape is
 * load-bearing rather than stylistic. The {@code MOVEMENT_SPEED} lifecycle in {@code
 * EntitySpeedAttribute} looks like a closer relative but is not one -- it is mob-only, and both its
 * call sites open by returning for a player.
 *
 * <p>Not pure and not unit-tested: every line needs a live {@code Player}. The arithmetic it depends
 * on lives in {@link AttackSpeedAttribute} and IS tested; what remains here is attribute plumbing,
 * witnessed by the boot gate. Same split, and same reason, as {@code Defense#barModifier}.
 */
public final class AttackSpeedAttributeOverride {

    private AttackSpeedAttributeOverride() {}

    /**
     * Drive {@code player}'s attack speed to the cadence their held weapon and stat justify.
     *
     * <p>{@code weaponBaseSpeed} MUST come from the held weapon's definition (see
     * {@code WeaponAttackItems.heldMeleeSpeed}), never from {@code attribute.getValue()}. The
     * attribute is what this method writes to, so reading it back as the input would have the value
     * chase itself on every scan -- the trap {@link ArmorBarOverride} names for armor, and it bites
     * identically here.
     *
     * <p>Idempotent, and that is load-bearing rather than tidy: this runs on the 5-tick reconcile
     * loop, four times a second, for every online player. Re-adding an identical modifier would spam
     * an attribute-sync packet at that rate, and adding a second modifier under a key that already
     * exists is rejected outright. So an unchanged value writes nothing at all.
     *
     * <p>With no melee weapon held the modifier is REMOVED rather than set to zero, leaving the
     * attribute pristine -- absent, not zeroed. That is not merely tidy here: the modifier for an
     * absent weapon computes to -4.0, which if written would drive attack speed to a flat zero and
     * stop the player attacking at all. Hence the branch tests the SPEED, never the modifier.
     */
    public static void apply(Player player, Keys keys, double weaponBaseSpeed, double statMultiplier) {
        AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute == null) return;
        AttributeModifier existing = attribute.getModifier(keys.attackSpeedOverride);

        double desiredSpeed = AttackSpeedAttribute.desiredSpeed(weaponBaseSpeed, statMultiplier);
        if (desiredSpeed <= 0) {
            if (existing != null) attribute.removeModifier(keys.attackSpeedOverride);
            return;
        }

        double desired = AttackSpeedAttribute.modifier(weaponBaseSpeed, statMultiplier);
        if (existing != null) {
            if (existing.getAmount() == desired) return;   // unchanged: write nothing, send nothing
            attribute.removeModifier(keys.attackSpeedOverride);
        }
        attribute.addModifier(new AttributeModifier(
                keys.attackSpeedOverride, desired, AttributeModifier.Operation.ADD_NUMBER));
    }

    /**
     * Drop the override, restoring vanilla's own attack speed.
     *
     * Called on quit. API-added attribute modifiers persist in player data, so without this a player
     * who logs out holding a boosted weapon keeps an attack-speed modifier written by a plugin that
     * may not be installed the next time they log in. The scan re-derives the correct value within 5
     * ticks of rejoining, so this is hygiene rather than correctness -- but it is the difference
     * between leaving a trace and not.
     */
    public static void clear(Player player, Keys keys) {
        AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute == null) return;
        if (attribute.getModifier(keys.attackSpeedOverride) != null) {
            attribute.removeModifier(keys.attackSpeedOverride);
        }
    }
}
