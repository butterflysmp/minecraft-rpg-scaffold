package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.mob.MeleeSeed.Modifier;
import io.github.butterflysmp.rpg.core.mob.MeleeSeed.Operation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The seat's melee ruling (2026-09-28, PLAN §6 F16): a mob's seeded attack ALWAYS counts its held
 * main-hand weapon, computed as base + the weapon's ADD modifiers. It is read from the item, never from
 * the live attribute, whose value depends on whether a player paired with the mob before the seed ran.
 *
 * <p>The numbers are the pinned jar's: a wither skeleton's default {@code ATTACK_DAMAGE} base is 2.0
 * (Attributes' RangedAttribute), and 4.0 after WitherSkeleton.finalizeSpawn. A stone sword's attack
 * modifier is 3.0 + STONE's 1.0 = 4.0, ADD_VALUE (ToolMaterial.createSwordAttributes).
 */
class MeleeSeedTest {

    private static final double EPS = 1e-9;
    private static final Modifier STONE_SWORD = new Modifier(Operation.ADD_VALUE, 4.0);

    @Test
    void theSeedIsTheBasePlusTheWeaponsAddModifiers() {
        assertEquals(6.0, MeleeSeed.attack(2.0, List.of(STONE_SWORD)).attack(), EPS,
                "a /rpg spawn wither skeleton: default base 2.0 + stone sword 4.0. "
                        + "Mutation: drop the weapon term -> 2.0 -> reddens");
        assertEquals(8.0, MeleeSeed.attack(4.0, List.of(STONE_SWORD)).attack(), EPS,
                "a natural wither skeleton: finalizeSpawn's base 4.0 + stone sword 4.0");
    }

    @Test
    void everyAddModifierIsSummed() {
        // Two ADD modifiers on one item (a custom item could carry them): both count.
        assertEquals(10.0, MeleeSeed.attack(2.0, List.of(STONE_SWORD, new Modifier(Operation.ADD_VALUE, 4.0)))
                .attack(), EPS);
    }

    @Test
    void noWeaponIsTheBase() {
        assertEquals(2.0, MeleeSeed.attack(2.0, List.of()).attack(), EPS);
        assertEquals(0, MeleeSeed.attack(2.0, List.of()).unpriced());
    }

    @Test
    void aNonAddModifierIsNotPricedAndIsCounted() {
        // No vanilla weapon has one (every builder read uses ADD_VALUE). Its ORDER relative to the ADD
        // modifiers is exactly what is not guessed, so it is left out, and the count tells the caller to
        // WARN rather than price silently.
        MeleeSeed.Seed seed = MeleeSeed.attack(2.0, List.of(STONE_SWORD, new Modifier(Operation.OTHER, 0.5)));
        assertEquals(6.0, seed.attack(), EPS);
        assertEquals(1, seed.unpriced());
    }
}
