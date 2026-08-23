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
    void theSuppressorTargetsVanillaAttackDamageNotTheTriggerPath() {
        // The vanilla melee attribute -- a separate path from the trigger, which flows
        // EffectSpec.Damage -> CombatantHandle.applyDamage and never touches an attribute.
        assertEquals("attack_damage", WeaponItems.ATTACK_DAMAGE_ATTRIBUTE);
    }

    @Test
    void theSuppressorZeroesTheBaseSwingRatherThanAddingToIt() {
        // A player's base attack_damage is 1.0; -1.0 brings a held swing to a flat 0.
        // Negative, not positive: it must REDUCE vanilla melee, never buff it.
        assertEquals(-1.0, WeaponItems.VANILLA_MELEE_SUPPRESSION, 1e-9);
        assertTrue(WeaponItems.VANILLA_MELEE_SUPPRESSION <= 0.0,
                "a suppressor must cancel vanilla melee, not add to it");
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
