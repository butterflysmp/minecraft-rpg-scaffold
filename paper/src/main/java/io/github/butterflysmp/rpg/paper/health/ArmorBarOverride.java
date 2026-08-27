package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.Defense;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/**
 * Commandeering the vanilla armor bar so it reads DAMAGE REDUCTION instead of material.
 *
 * The vanilla bar fills from the {@code armor} attribute -- 20 points, drawn as 10 icons -- which
 * worn armor fills by material. That number is the wrong one to show here: a full diamond set is 20
 * points and would fill the bar completely while actually turning away about a sixth of a hit. A full
 * bar that means "one sixth" is worse than no bar, because it reads as a promise.
 *
 * So the bar is driven to {@link Defense#armorBarPoints}, by adding a modifier that CANCELS the
 * native sum and re-adds the DR fraction: {@code native + modifier = armorBarPoints(defense)}. The
 * modifier is therefore normally negative -- about -16.67 in full diamond.
 *
 * <p>This is the same technique {@code WeaponItems.VANILLA_MELEE_SUPPRESSION} uses to zero a weapon's
 * vanilla swing, and the difference is deliberate: the melee suppressor is minted onto an ITEM once
 * and never revisited, because a weapon's suppression never changes. This one is a LIVE ENTITY
 * modifier recomputed on the reconcile scan, because DR changes every time a piece of armor does. The
 * add/remove-by-key mechanics follow {@code EntitySpeedAttribute}, which is the existing entity-side
 * precedent.
 *
 * <p>Not pure and not unit-tested -- every line needs a live {@code Player}. The arithmetic it
 * depends on lives in {@link Defense#barModifier} and IS tested; what remains here is attribute
 * plumbing, witnessed by the boot gate.
 */
public final class ArmorBarOverride {

    private ArmorBarOverride() {}

    /**
     * Drive {@code player}'s armor bar to the damage reduction {@code defense} actually provides.
     *
     * {@code nativeArmor} MUST be the sum read from the equipped pieces (see
     * {@link DefenseModifierItems#total}), never {@code attribute.getValue()}. The attribute is what
     * this method writes to, so reading it back as the input would have the value chase itself
     * downward on every scan.
     *
     * Idempotent, and that is load-bearing rather than tidy: this runs on the 5-tick reconcile loop,
     * four times a second, for every online player. Re-adding an identical modifier would spam an
     * attribute-sync packet to the client at that rate, and adding a second modifier under a key that
     * already exists is rejected outright. So an unchanged value writes nothing at all.
     *
     * At zero defense the modifier is REMOVED rather than set to zero, leaving the attribute
     * pristine -- the same absent-not-zeroed discipline the equipment scan follows. A player who has
     * never worn armor carries no trace of this plugin on their armor attribute.
     */
    public static void apply(Player player, Keys keys, double defense, double nativeArmor) {
        AttributeInstance attribute = player.getAttribute(Attribute.ARMOR);
        if (attribute == null) return;
        AttributeModifier existing = attribute.getModifier(keys.armorBarOverride);

        if (defense <= 0) {
            if (existing != null) attribute.removeModifier(keys.armorBarOverride);
            return;
        }

        double desired = Defense.barModifier(defense, nativeArmor);
        if (existing != null) {
            if (existing.getAmount() == desired) return;   // unchanged: write nothing, send nothing
            attribute.removeModifier(keys.armorBarOverride);
        }
        attribute.addModifier(new AttributeModifier(
                keys.armorBarOverride, desired, AttributeModifier.Operation.ADD_NUMBER));
    }

    /**
     * Drop the override, restoring the vanilla material-based bar.
     *
     * Called on quit. API-added attribute modifiers persist in player data, so without this a player
     * who logs out in armor keeps a large negative armor modifier written by a plugin that may not be
     * installed the next time they log in. The scan re-derives the correct value within 5 ticks of
     * rejoining, so this is hygiene rather than correctness -- but it is the difference between
     * leaving a trace and not.
     */
    public static void clear(Player player, Keys keys) {
        AttributeInstance attribute = player.getAttribute(Attribute.ARMOR);
        if (attribute == null) return;
        if (attribute.getModifier(keys.armorBarOverride) != null) {
            attribute.removeModifier(keys.armorBarOverride);
        }
    }
}
