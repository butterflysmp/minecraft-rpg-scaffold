package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The vanilla-melee suppressor, tested at the DECISION rather than on a constructed item.
 *
 * An ItemStack cannot be built in a unit test -- new ItemStack(...) throws "No RegistryAccess
 * implementation found" without a running server, and the project has no MockBukkit. So the
 * item actually carrying the modifier, and the absence of a double-hit, are witnessed on the
 * real-server boot. What IS unit-testable, and what actually matters, is the decision: the
 * suppressor targets the VANILLA attack-damage path -- not the trigger's -- and zeroes it.
 *
 * "Zeroed the wrong damage path" is the failure that passes review and fails in the world:
 * a suppressor pointed at the trigger would leave vanilla melee intact (double-hit) while
 * looking correct. mint() derives the Bukkit Attribute from ATTACK_DAMAGE_ATTRIBUTE, so this
 * assertion governs what the item receives -- the constant and the item cannot drift.
 */
class WeaponItemsTest {

    @Test
    void thePinnedAttributesAreTheVanillaMeleePathAndNotTheTriggerPath() {
        // Both vanilla attributes, by their real names. The trigger's own damage flows
        // EffectSpec.WeaponDamage -> CombatantHandle.applyDamage and never touches an attribute,
        // so pinning the wrong one is the review-passing, world-failing bug.
        assertEquals("attack_damage", WeaponItems.ATTACK_DAMAGE_ATTRIBUTE);
        assertEquals("attack_speed", WeaponItems.ATTACK_SPEED_ATTRIBUTE);
    }

    /**
     * A vanilla-driven melee weapon must leave vanilla's attack damage STRICTLY POSITIVE.
     *
     * This is Finding 1 turned into a test. Vanilla skips its whole attack path -- no damage event,
     * no i-frames, no durability charge -- when attack damage and enchantment bonus are both zero.
     * The old suppressor brought the total to exactly 0, which is why nine ironblade swings across
     * three Step 0 sessions produced zero EntityDamageByEntityEvents and the melee rider was dead
     * code. If this ever goes back to <= 0, basic melee silently stops happening again.
     */
    @Test
    void aMeleeWeaponPinsAPositiveVanillaAttackDamage() {
        // ironblade declares 8; a player's base is 1.0, so the modifier is 7.0 and the total is 8.
        assertEquals(7.0, WeaponItems.attackDamageModifier(8.0), 1e-9);
        assertEquals(6.0, WeaponItems.attackDamageModifier(7.0), 1e-9, "emberblade declares 7");

        for (double declared : new double[] {0.5, 1.0, 7.0, 8.0, 20.0, 1000.0}) {
            assertTrue(WeaponItems.VANILLA_BASE_ATTACK_DAMAGE + WeaponItems.attackDamageModifier(declared) > 0.0,
                    "vanilla must actually attack, at declared " + declared);
        }
        // Mutation: return -declared, or VANILLA_MELEE_SUPPRESSION, from attackDamageModifier ->
        // the total hits 0, vanilla stops attacking, and basic melee deals nothing -> reddens.
    }

    /**
     * The speed pin, derived from the SAME authored cooldown_ticks the tooltip renders, so the
     * charge meter and the "Attack Speed" line cannot disagree.
     *
     * Measured on the 2026-08-28 boot: an item carrying any explicit modifier loses its whole
     * default block, so without this pin a minted iron sword reads the player base 4.0 rather than
     * its native 1.6 -- a 5-tick charge period inside a 10-tick i-frame window, which would leave
     * every allowed swing fully charged and AttackCharge dead code.
     */
    @Test
    void theSpeedPinTurnsTheAuthoredCadenceIntoAVanillaAttackSpeed() {
        // ironblade / emberblade: cooldown_ticks 10 -> 2.0 attacks/sec -> a 10-tick charge period,
        // exactly the vanilla i-frame window.
        assertEquals(-2.0, WeaponItems.attackSpeedModifier(10).getAsDouble(), 1e-9);
        assertEquals(2.0, WeaponItems.VANILLA_BASE_ATTACK_SPEED + WeaponItems.attackSpeedModifier(10).getAsDouble(), 1e-9);
        // A slower weapon pins slower, and the sign stays negative against the fast player base.
        assertEquals(1.0, WeaponItems.VANILLA_BASE_ATTACK_SPEED + WeaponItems.attackSpeedModifier(20).getAsDouble(), 1e-9);
        // Mutation: drop the "- VANILLA_BASE_ATTACK_SPEED" -> a +2.0 modifier makes the total 6.0,
        // FASTER than the unpinned base, and the charge window collapses -> reddens.
    }

    /**
     * A declared cooldown of 0 means "ungated" and has no cadence to express, so nothing is pinned
     * and the player base stands -- the same reading AttackSpeed.effectiveCooldownTicks gives it.
     * Without the guard this divides by zero and pins an infinite speed.
     */
    @Test
    void anUngatedTriggerPinsNoSpeedRatherThanDividingByZero() {
        assertTrue(WeaponItems.attackSpeedModifier(0).isEmpty());
        assertTrue(WeaponItems.attackSpeedModifier(-5).isEmpty());
        // Mutation: drop the cooldownTicks <= 0 guard -> Infinity, and a modifier of Infinity makes
        // the attribute NaN -> reddens.
    }

    /**
     * The suppressor survives, scoped to weapons with no melee hit of ours to deliver -- ember_staff,
     * ability_stone, hunters_bow. It must still REDUCE vanilla melee to zero, never add to it, so a
     * staff cannot be swung as a club.
     */
    @Test
    void aNonMeleeWeaponStillSuppressesVanillaMeleeToZero() {
        assertEquals(-1.0, WeaponItems.VANILLA_MELEE_SUPPRESSION, 1e-9);
        assertEquals(0.0, WeaponItems.VANILLA_BASE_ATTACK_DAMAGE + WeaponItems.VANILLA_MELEE_SUPPRESSION, 1e-9,
                "base 1.0 plus the suppressor is a flat 0 -- vanilla declines to attack at all");
    }

    /** The effective colour: MiniMessage may hang the colour on a child rather than the root. */
    private static TextColor colorOf(Component component) {
        if (component.color() != null) return component.color();
        for (Component child : component.children()) {
            TextColor found = colorOf(child);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Rarity owns the item-name colour outright. This was previously colorIfAbsent(), under which
     * an authored colour won -- so Hunter's Bow (authored gold, rarity UNCOMMON) minted gold, and
     * the weapons that looked right did so by coincidence rather than by rule.
     *
     * The authored gold here is deliberate: it is the exact input the old behaviour got wrong, and
     * it also guards the MiniMessage trap that makes this non-trivial. "<gold>X</gold>" puts the
     * colour on a CHILD component, so a .color(rarity) on the root would leave the gold in place
     * and this assertion would still see GOLD.
     */
    @Test
    void theItemNameIsColouredByRarityEvenWhenTheContentAuthorsItsOwnColour() {
        Component name = WeaponItems.displayName("<gold>Hunter's Bow</gold>", Rarity.UNCOMMON);

        assertEquals("Hunter's Bow", PlainTextComponentSerializer.plainText().serialize(name),
                "the authored text survives; only its colour is overridden");
        assertEquals(RarityColors.of(Rarity.UNCOMMON), colorOf(name));
        assertNotEquals(RarityColors.of(Rarity.RARE), colorOf(name),
                "the tier must be the weapon's own, not another's");
    }

    /**
     * The case that rules out the cheaper fix. A tag wrapping the WHOLE name is compacted onto the
     * root component, so a plain .color(rarity) would override it and pass -- but a tag around only
     * PART of the name leaves the colour on a child, where a root-level .color never reaches. This
     * is why displayName flattens to plain text and rebuilds instead. No shipped weapon authors a
     * name this way; the point is that one could, and the rule would still hold.
     */
    @Test
    void aPartiallyColouredNameIsAlsoFullyRecolouredByRarity() {
        Component name = WeaponItems.displayName("Hunter's <gold>Bow</gold>", Rarity.UNCOMMON);

        assertEquals("Hunter's Bow", PlainTextComponentSerializer.plainText().serialize(name));
        assertEquals(RarityColors.of(Rarity.UNCOMMON), colorOf(name));
        assertFalse(hasAnyColour(name, RarityColors.of(Rarity.LEGENDARY)),
                "no gold may survive anywhere in the tree, root or child");
    }

    /** True if the colour appears anywhere in the component tree -- root or any descendant. */
    private static boolean hasAnyColour(Component component, TextColor colour) {
        if (colour.equals(component.color())) return true;
        for (Component child : component.children()) {
            if (hasAnyColour(child, colour)) return true;
        }
        return false;
    }

    @Test
    void anUncolouredNameAlsoTakesItsRaritysColour() {
        // Ironblade's shape: no authored tag at all. This path worked before too -- pinned so the
        // fix for the authored case cannot regress it.
        Component name = WeaponItems.displayName("Ironblade", Rarity.COMMON);

        assertEquals("Ironblade", PlainTextComponentSerializer.plainText().serialize(name));
        assertEquals(RarityColors.of(Rarity.COMMON), colorOf(name));
    }
}
