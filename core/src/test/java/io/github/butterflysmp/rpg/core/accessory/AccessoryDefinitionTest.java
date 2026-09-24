package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What an accessory definition refuses. Each refusal is a named, skipped file in the boot log, so each
 * row asserts the MESSAGE names the rule -- a refusal for the wrong reason is a row that cannot fail
 * for its stated one.
 */
class AccessoryDefinitionTest {

    private static AccessoryDefinition build(String material, AccessorySlotKind slot, WeaponClass cls,
                                             AccessoryType type, Map<AccessoryStat, Double> modifiers) {
        return new AccessoryDefinition("x", "X", Rarity.COMMON, material, slot, cls, type, modifiers,
                List.of());
    }

    private static String refusal(Runnable build) {
        return assertThrows(IllegalArgumentException.class, build::run).getMessage();
    }

    @Test
    void aUniversalAccessoryNamingAClassOrATypeIsRefused() {
        Map<AccessoryStat, Double> m = Map.of(AccessoryStat.DEFENSE, 3.0);
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.UNIVERSAL, WeaponClass.MAGE, null, m))
                .contains("slot: universal"));
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.UNIVERSAL, null, AccessoryType.SCROLL, m))
                .contains("slot: universal"));
    }

    @Test
    void aClassAccessoryMustNameBothAClassAndAType() {
        Map<AccessoryStat, Double> m = Map.of(AccessoryStat.DEFENSE, 3.0);
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.CLASS, null, AccessoryType.SCROLL, m))
                .contains("must name both"));
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.CLASS, WeaponClass.MAGE, null, m))
                .contains("must name both"));
    }

    /** The whole type/class table, both directions: every mismatched pair refuses, every match loads. */
    @Test
    void theTypeMustMatchTheClass_everyPairChecked() {
        Map<AccessoryStat, Double> m = Map.of(AccessoryStat.DEFENSE, 3.0);
        int loaded = 0;
        for (AccessoryType type : AccessoryType.values()) {
            for (WeaponClass cls : WeaponClass.values()) {
                if (type.weaponClass() == cls) {
                    build("echo_shard", AccessorySlotKind.CLASS, cls, type, m);
                    loaded++;
                } else {
                    assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.CLASS, cls, type, m))
                            .contains("belongs to"), type + " on " + cls);
                }
            }
        }
        assertEquals(3, loaded, "melee-gauntlet, ranger-quiver, mage-scroll");
        assertEquals(WeaponClass.MELEE, AccessoryType.GAUNTLET.weaponClass());
        assertEquals(WeaponClass.RANGER, AccessoryType.QUIVER.weaponClass());
        assertEquals(WeaponClass.MAGE, AccessoryType.SCROLL.weaponClass());
    }

    /**
     * The §3.7 allowlist. ARROW is named because {@code QuiverAmmo.consume} re-checks only the
     * material -- an arrow-material accessory would be eaten as ammunition.
     */
    @Test
    void onlyTheInertMaterialsLoad_andArrowIsRefused() {
        Map<AccessoryStat, Double> m = Map.of(AccessoryStat.DEFENSE, 3.0);
        for (String material : List.of("echo_shard", "netherite_scrap", "shulker_shell", "prismarine_crystals",
                "ECHO_SHARD")) {
            build(material, AccessorySlotKind.UNIVERSAL, null, null, m);
        }
        for (String refused : List.of("arrow", "paper", "leather", "rabbit_hide", "iron_helmet",
                "carved_pumpkin", "bread", "stone")) {
            assertTrue(refusal(() -> build(refused, AccessorySlotKind.UNIVERSAL, null, null, m))
                    .contains("must be one of"), refused);
        }
        assertTrue(refusal(() -> build(null, AccessorySlotKind.UNIVERSAL, null, null, m))
                .contains("must be one of"));
        // Mutation: add "arrow" to AccessoryDefinition.MATERIALS -> reddens.
    }

    @Test
    void theMaterialIsStoredLowercase() {
        assertEquals("echo_shard", build("Echo_Shard", AccessorySlotKind.UNIVERSAL, null, null,
                Map.of(AccessoryStat.DEFENSE, 3.0)).material());
    }

    @Test
    void anAccessoryWithNoModifiersOrAZeroOrNonFiniteOneIsRefused() {
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.UNIVERSAL, null, null, Map.of()))
                .contains("no modifiers"));
        for (double bad : new double[] {0.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.UNIVERSAL, null, null,
                    Map.of(AccessoryStat.DEFENSE, bad))).contains("finite, non-zero"), "" + bad);
        }
        Map<AccessoryStat, Double> withNull = new HashMap<>();
        withNull.put(AccessoryStat.DEFENSE, null);
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.UNIVERSAL, null, null, withNull))
                .contains("finite, non-zero"));
    }

    @Test
    void classDamageOnAUniversalAccessoryIsRefused_itHasNoClassToGrantTo() {
        assertTrue(refusal(() -> build("echo_shard", AccessorySlotKind.UNIVERSAL, null, null,
                Map.of(AccessoryStat.CLASS_DAMAGE, 3.0))).contains("only a class accessory"));
        // And on a class accessory it loads.
        build("shulker_shell", AccessorySlotKind.CLASS, WeaponClass.RANGER, AccessoryType.QUIVER,
                Map.of(AccessoryStat.CLASS_DAMAGE, 3.0));
    }

    @Test
    void theModifierMapIsCopiedAndUnmodifiable() {
        Map<AccessoryStat, Double> source = new HashMap<>(Map.of(AccessoryStat.DEFENSE, 3.0));
        AccessoryDefinition def = build("echo_shard", AccessorySlotKind.UNIVERSAL, null, null, source);
        source.put(AccessoryStat.MAX_HEALTH, 10.0);
        assertEquals(1, def.modifiers().size(), "a later write to the source must not reach the record");
        assertThrows(UnsupportedOperationException.class,
                () -> def.modifiers().put(AccessoryStat.MAX_HEALTH, 1.0));
    }

    @Test
    void anAccessoryNeverClaimsACraftResult() {
        assertTrue(AccessoryFixtures.wardCharm().craftResult().isEmpty(), "A3: admin give only");
    }
}
