package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What content must declare to carry a magazine, and what each refusal is protecting against.
 *
 * <h2>EVERY ROW CAUSES THE CONDITION RATHER THAN ASSERTING THE ARM EXISTS</h2>
 *
 * <p>These guards have a property {@code CLAUDE.md} names specifically: <b>no shipped content
 * reaches any of them.</b> Every weapon in {@code content/weapons/} predates the quiver and declares
 * none, so production cannot produce a single one of these conditions, and the only exercise these
 * arms will ever get is this file. An arm in that position, tested by asserting it is PRESENT rather
 * than by making it FIRE, is a dead branch with a green suite around it -- which is exactly how
 * {@code ElementLoader.damageSymbol} shipped a {@code catch} that could never execute.
 *
 * <p>So each row authors the bad weapon, constructs it for real, and reads the message back by its
 * text. Redden any of them by deleting the matching guard from {@code WeaponDefinition}.
 */
class WeaponQuiverDefinitionTest {

    private static final CastSpec.Projectile PROJECTILE = new CastSpec.Projectile(2.5, 0.05, 60);

    private static AbilityDefinition ability(String id) {
        return new AbilityDefinition(id, "T", "kinetic", "none",
                0, ResourceCost.FREE, PROJECTILE, List.of(new EffectSpec.Damage(4.0, "kinetic")));
    }

    /** A weapon binding {@code input}, with whatever quiver numbers the row is staging. */
    private static WeaponDefinition weapon(int quiverSize, int reloadTicks, String input) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.RANGER,
                "crossbow", 0.0, 0.0, SweepShare.NONE, quiverSize, reloadTicks,
                List.of(new TriggerBinding(input, ability("w/" + input))), List.of());
    }

    // ---------------------------------------------------------------- the ordinary cases

    @Test
    void aWeaponWithNoQuiverIsTheOrdinaryCaseAndStaysLegal() {
        WeaponDefinition plain = weapon(WeaponDefinition.NO_QUIVER, 0, "right_click");
        assertFalse(plain.hasQuiver(), "0 is ABSENT, the same convention attack_damage and sweep use");
        assertEquals(0, plain.quiverSize());
        // AND IT MAY STILL BIND left_click, which is the whole point of the collision guard being
        // conditional: every weapon shipped before the quiver existed keeps its left-click trigger.
        assertDoesNotThrow(() -> weapon(WeaponDefinition.NO_QUIVER, 0, "left_click"));
    }

    @Test
    void aWellFormedQuiverWeaponLoads() {
        WeaponDefinition quivered = weapon(3, 7, "right_click");
        assertTrue(quivered.hasQuiver());
        assertEquals(3, quivered.quiverSize());
        assertEquals(7, quivered.reloadTicks());
    }

    // ---------------------------------------------------------------- the refusals

    @Test
    void aNegativeQuiverSizeIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> weapon(-1, 7, "right_click"));
        assertTrue(thrown.getMessage().contains("quiver_size must be >= 0"), thrown.getMessage());
    }

    @Test
    void aNegativeReloadIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> weapon(3, -1, "right_click"));
        assertTrue(thrown.getMessage().contains("reload_ticks must be >= 0"), thrown.getMessage());
    }

    /**
     * A reload with nothing to reload can never fire.
     *
     * <p>The silent-no-op shape the {@code sweep} guard beside it exists for: the field is authored,
     * it validates, it loads, and it governs nothing forever. Naming it is the difference between a
     * typo'd {@code quiver_size} being a boot failure and being a weapon that quietly never reloads.
     */
    @Test
    void aReloadWithoutAQuiverIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> weapon(WeaponDefinition.NO_QUIVER, 7, "right_click"));
        assertTrue(thrown.getMessage().contains("can never reload"), thrown.getMessage());
    }

    /**
     * A magazine with no reload empties once and is inert forever.
     *
     * <p>The mirror of the row above, and the more damaging of the two in play: the weapon works
     * perfectly for exactly {@code quiver_size} shots and then becomes a dead item with a tooltip
     * that still reads correctly. Refusing {@code reload_ticks: 0} does foreclose a deliberate
     * instant reload; that is the same trade the {@code attack_speed} guard makes, and it is
     * relaxable the day someone wants one -- deliberately, rather than by omission.
     */
    @Test
    void aQuiverWithoutAReloadIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> weapon(3, 0, "right_click"));
        assertTrue(thrown.getMessage().contains("could never be refilled"), thrown.getMessage());
    }

    /**
     * A quiver weapon may not also bind left_click, because the quiver already spends it.
     *
     * <p>Two owners for one press, and whichever ran first would win SILENTLY -- there is no error
     * state for it, just a reload that sometimes does not happen or a trigger that sometimes does
     * not fire. {@code hunters_bow.yml} reserves left-click for <i>"a future melee/special"</i>; a
     * quiver weapon is the case that spends it, and a refusal is the only way that stays a decision
     * rather than a race.
     */
    @Test
    void aQuiverWeaponMayNotAlsoBindLeftClick() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> weapon(3, 7, "left_click"));
        assertTrue(thrown.getMessage().contains("may not also"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("left_click"), thrown.getMessage());
    }
}
