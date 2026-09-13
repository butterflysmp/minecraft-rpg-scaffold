package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.QuiverSize;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantValues;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading the QUIVER-SIZE bonus the weapon in a player's MAIN HAND grants through Expanded Quiver.
 *
 * <p>The enchant-reading twin of {@link QuiverSizeModifierItems}, which reads the same stat from a
 * PDC instrument instead. That file predicted this one before it existed, under <i>"It reconciles
 * alone, for now"</i>, and both warnings it left are load-bearing here.
 *
 * <h2>Its output MUST be merged with {@link QuiverSizeModifierItems}, never reconciled separately</h2>
 *
 * {@code ModifierReconciler.reconcile} removes every applied source absent from the map it is
 * handed. There is exactly ONE {@code reconcileQuiverSizeModifiers} call per tick and there must stay
 * exactly one: two calls against the same target -- one for the instrument, one for this -- would
 * have each wipe the other's sources, leaving only whichever ran last. The magazine would then
 * silently hold half of what the player is carrying, forever, with nothing thrown and no event
 * missing. Same defect, same remedy, as {@link GrowthModifierItems} for max health -- and
 * {@code PlayerHealthSystem} carries the merge, exactly as it does for that pair.
 *
 * <h2>The keys are NAMESPACED, for a reason ONE SLOT makes sharper than four</h2>
 *
 * {@link QuiverSizeModifierItems} walks ALL slots on bare {@link EquipmentSlot#name()} keys, so it
 * will happily key {@code "HAND"}. This scanner reads the hand too. <b>A player holding a
 * quiver_size_boost instrument in the same hand as an Expanded Quiver weapon is ONE SLOT producing
 * TWO sources</b>, and unprefixed they collide on one key -- {@code Stat.putModifier} is
 * put-or-REPLACE, so one would silently erase the other.
 *
 * <p>That is not hypothetical here the way it is for armor: the instrument is mintable by
 * {@code /rpg give} and an enchanted Boltor is mintable beside it, so the collision is one command
 * away. {@code ExpandedQuiverTest.twoSourcesInOneSlotMustNotShareAKeyOrOneSILENTLYERASESTheOther}
 * pins it in core, where a unit test can reach {@code Stat} directly.
 *
 * <h2>MAIN HAND ONLY, which is narrower than both files this was modelled on</h2>
 *
 * {@link GrowthModifierItems} reads four armor slots; {@link QuiverSizeModifierItems} reads every
 * slot. <b>This reads exactly one.</b> A quiver belongs to the weapon being FIRED, and only the main
 * hand fires -- so a Boltor in the off-hand or the boots slot grants nothing, which is what
 * {@code Gate.RANGER_ONLY} already promises on the content side.
 *
 * <p>Said here because the template walks more than this does, and a reader arriving from either
 * sibling will expect a loop.
 *
 * <p>Absent-not-zeroed, like every sibling: a hand holding no Expanded Quiver contributes NO ENTRY,
 * so the reconciler's removal branch does the cleanup when the weapon leaves by any route -- swap,
 * drop, break, death, {@code /clear}.
 *
 * <h2>THE GATE IS {@code > NONE}, AND THE DIRECTION IS WHY</h2>
 *
 * {@link QuiverSize#boosts} is strictly {@code >}: <b>this stat's direction is POSITIVE -- more
 * arrows is better</b> -- so increase-only is the correct content rule and a 0 declares nothing
 * rather than writing a no-op source every scan.
 *
 * <p><b>{@code ReloadTime} in the same family is the OPPOSITE and gates on {@code != NONE}</b>,
 * because its sign is inverted: positive means slower, so {@code > NONE} there would mean content
 * could only make reloads WORSE. Slice A2 shipped that exact confusion by copying this file's
 * comparison into that one. <b>Check the direction before copying the operator.</b>
 */
public final class ExpandedQuiverModifierItems {

    private ExpandedQuiverModifierItems() {}

    /**
     * The prefix that keeps these sources disjoint from {@link QuiverSizeModifierItems}' bare slot
     * keys. Both scanners can produce a source for the main hand; see the class javadoc.
     */
    static final String SOURCE_PREFIX = "expandedquiver:";

    /**
     * The quiver-size modifiers the weapon in the player's main hand justifies right now.
     *
     * <p><b>Merge this into the instrument scan's map and reconcile ONCE.</b> See the class javadoc.
     *
     * <p>The bonus is narrowed to {@code int} HERE, at the boundary between the {@code double} an
     * enchant curve carries and a stat that means whole arrows -- so {@link QuiverSize#boosts} and
     * {@link QuiverSize#contribution} never see a fraction, and a curve authored {@code 2.5} is
     * floored once, at the edge, by the same {@link QuiverSize#arrows} rule that governs everywhere
     * else.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys,
                                                       EnchantRegistry enchants) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        // ONE SLOT, not a loop. See the class javadoc: only the main hand fires.
        double bonus = EnchantValues.totalFor(
                EnchantItems.read(equipment.getItem(EquipmentSlot.HAND), keys), enchants,
                EnchantEffect.QUIVER_SIZE);

        int arrows = QuiverSize.arrows(bonus);
        if (QuiverSize.boosts(arrows)) {
            desired.put(SOURCE_PREFIX + EquipmentSlot.HAND.name(),
                    (double) QuiverSize.contribution(arrows));
        }
        return desired;
    }

    /**
     * Both quiver-size sources as ONE map, for the single {@code reconcileQuiverSizeModifiers} call.
     *
     * <h2>THIS EXISTS TO MAKE DROPPING A SOURCE A COMPILE ERROR, AND IT WAS WRITTEN BECAUSE THE
     * MUTATION PROVED THE ALTERNATIVE WAS UNGUARDED</h2>
     *
     * <p>The merge began life inline in {@code PlayerHealthSystem} -- a {@code new HashMap<>(a)} then
     * {@code putAll(b)}, exactly as the max-health pair still does. <b>Measured 2026-09-13: deleting
     * that {@code putAll} reddened NOTHING across the whole suite.</b> {@code PlayerHealthSystem}
     * needs a live {@code Player}, so no unit test reaches its scan loop, and the core row that models
     * the defect asserts against {@code Stat} directly rather than against this wiring. The assertion
     * DOCUMENTED the trap and did not GUARD it.
     *
     * <p>A two-argument function fixes that structurally rather than by adding a test that cannot
     * exist: <b>you cannot drop a source without dropping an argument, and dropping an argument does
     * not compile.</b> What remains reachable -- someone editing this body to ignore one map -- is
     * what {@code ExpandedQuiverModifierItemsTest} guards, and that test CAN exist because this takes
     * plain maps and no {@code Player}.
     *
     * <p><b>The max-health pair above it is still inline and still unguarded by the same measurement.</b>
     * Not changed here: that is a second edit to a second stat, and it is recorded rather than
     * bundled into a slice about quivers.
     *
     * <h2>ORDER MATTERS, AND IT IS THE ENCHANT THAT WINS</h2>
     *
     * <p>{@code putAll} is put-or-REPLACE, so on a key collision the enchant's value survives. <b>That
     * ordering should never decide anything</b>, because {@link #SOURCE_PREFIX} makes the two key
     * spaces disjoint by construction -- and if it ever does decide something, the prefix has been
     * broken and the test above catches that first. The order is fixed here so the behaviour is not
     * accidental, not because either answer is correct.
     *
     * @param instrumentSources {@link QuiverSizeModifierItems#desiredModifiers}, keyed bare
     * @param enchantSources    {@link #desiredModifiers}, keyed with {@link #SOURCE_PREFIX}
     */
    public static Map<String, Double> mergedSources(Map<String, Double> instrumentSources,
                                                    Map<String, Double> enchantSources) {
        Map<String, Double> merged = new HashMap<>(instrumentSources);
        merged.putAll(enchantSources);
        return merged;
    }
}
