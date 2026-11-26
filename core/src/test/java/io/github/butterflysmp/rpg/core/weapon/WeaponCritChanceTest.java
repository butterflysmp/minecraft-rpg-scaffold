package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K2 (the seat's brief, 2026-11-25, the Broken Bottle): a weapon may declare a FLAT crit chance, held in its definition
 * and handed to the ONE crit-chance reconcile as one source, "the held weapon", while it is the weapon in the main hand.
 * Each test names the mutation it forces red.
 */
class WeaponCritChanceTest {

    private static final List<TriggerBinding> MELEE = List.of(new TriggerBinding("left_click",
            new AbilityDefinition("w/left_click", "T", "kinetic", 0, ResourceCost.FREE,
                    new CastSpec.Melee(3.0, 120), List.of(new EffectSpec.WeaponDamage("kinetic")))));

    private static WeaponDefinition withCrit(double critChance) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.MELEE, "glass_bottle",
                18, 2.0, SweepShare.NONE, WeaponDefinition.NO_QUIVER, 0, MELEE, List.of(), Optional.empty(), false,
                null, critChance);
    }

    /** Mutation: the convenience constructors pass anything but NO_CRIT. Every shipped weapon but one rides them. */
    @Test
    void aWeaponDeclaresNoCritChanceUnlessItSaysSo() {
        WeaponDefinition plain = new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.MELEE,
                "iron_sword", 23, 1.6, 0.5, WeaponDefinition.NO_QUIVER, 0, MELEE, List.of(), Optional.empty(), false,
                null);
        assertEquals(WeaponDefinition.NO_CRIT, plain.critChance());
        assertEquals(0.0, WeaponDefinition.NO_CRIT);
        assertEquals(0.10, withCrit(0.10).critChance());
    }

    /** Mutation: either bound dropped. A negative is a content bug; above 1 is a chance no roll can mean. */
    @Test
    void aCritChanceOutsideZeroToOneIsRefusedByName() {
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class, () -> withCrit(-0.05));
        assertTrue(negative.getMessage().contains("crit_chance"), negative.getMessage());
        assertThrows(IllegalArgumentException.class, () -> withCrit(1.5));
        assertThrows(IllegalArgumentException.class, () -> withCrit(Double.NaN));
        withCrit(1.0);   // the bound itself loads
        withCrit(0.0);
    }

    /** Mutation: the source is emitted for a weapon that declares none (0 must be ABSENT, never a zero entry). */
    @Test
    void theHeldWeaponIsOneSourceOnlyWhileItDeclaresAChance() {
        assertEquals(Map.of(), HeldWeaponCrit.sources(Optional.empty()), "nothing in hand");
        assertEquals(Map.of(), HeldWeaponCrit.sources(Optional.of(withCrit(0.0))), "a weapon with none: absent, not 0");
        assertEquals(Map.of(HeldWeaponCrit.SOURCE, 0.10), HeldWeaponCrit.sources(Optional.of(withCrit(0.10))));
        assertEquals("weapon:MAIN_HAND", HeldWeaponCrit.SOURCE, "its own key, distinct from every slot-named fixture key");
    }

    /**
     * FLAT (Ben: "Not it's crit chance"). STRUCTURAL: the source takes no score at all, so no score can move it. Pinned by
     * the signature, which a later "scale it too" edit has to break.
     */
    @Test
    void theSourceIsFlatBecauseItTakesNoScore() throws NoSuchMethodException {
        var m = HeldWeaponCrit.class.getMethod("sources", Optional.class);
        assertEquals(1, m.getParameterCount());
        assertEquals(1, java.util.Arrays.stream(HeldWeaponCrit.class.getDeclaredMethods())
                .filter(x -> java.lang.reflect.Modifier.isPublic(x.getModifiers())).count(), "one public method");
    }
}
