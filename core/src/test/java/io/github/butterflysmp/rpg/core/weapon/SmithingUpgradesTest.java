package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NETHERITE AT THE SMITHING TABLE (the seat's brief, part g; Ben, PLAN-weapon-tiers.md 1.2 answer 4: "Yes"). The pure
 * decision: a minted base + a template + an addition gives the declared upgrade, or the smith is REFUSED; an unminted
 * base is not ours and vanilla smiths it. Each row names the mutation it reddens.
 */
class SmithingUpgradesTest {

    private static final String TEMPLATE = "netherite_upgrade_smithing_template";
    private static final String INGOT = "netherite_ingot";

    private static SmithingUpgrades shipped() {
        return new SmithingUpgrades(List.of(new SmithingUpgrades.Upgrade("diamond_sword", TEMPLATE, INGOT, "netherite_sword")));
    }

    @Test
    void theDeclaredUpgradeResolves() {
        SmithingUpgrades.Decision d = shipped().decide("diamond_sword", TEMPLATE, INGOT);
        assertEquals(SmithingUpgrades.Kind.UPGRADE, d.kind());
        assertEquals("netherite_sword", d.result());
        // Mutation: return REFUSE for every minted base -> reddens.
    }

    @Test
    void aMintedBaseWithNoDeclaredUpgradeIsRefused() {
        // The survey's 2e: today a minted iron sword in a smithing table silently loses its identity. Now: no result,
        // so nothing can be taken and no ingot is spent.
        SmithingUpgrades.Decision d = shipped().decide("iron_sword", TEMPLATE, INGOT);
        assertEquals(SmithingUpgrades.Kind.REFUSE, d.kind());
        assertNull(d.result());
        // Mutation: treat an unmatched minted base as NOT_OURS -> reddens (vanilla would smith it).
    }

    @Test
    void theRightBaseWithTheWrongTemplateOrAdditionIsRefused() {
        assertEquals(SmithingUpgrades.Kind.REFUSE, shipped().decide("diamond_sword", "coast_armor_trim_smithing_template", INGOT).kind());
        assertEquals(SmithingUpgrades.Kind.REFUSE, shipped().decide("diamond_sword", TEMPLATE, "iron_ingot").kind());
        assertEquals(SmithingUpgrades.Kind.REFUSE, shipped().decide("diamond_sword", null, INGOT).kind(), "an empty slot");
        assertEquals(SmithingUpgrades.Kind.REFUSE, shipped().decide("diamond_sword", TEMPLATE, null).kind(), "an empty slot");
        // Mutation: match on the base alone -> the first two redden.
    }

    @Test
    void anUnmintedBaseIsNotOurs() {
        SmithingUpgrades.Decision d = shipped().decide(null, TEMPLATE, INGOT);
        assertEquals(SmithingUpgrades.Kind.NOT_OURS, d.kind(), "a plain sword or any vanilla item: vanilla's own smith");
        // Mutation: REFUSE an unminted base -> reddens (vanilla netherite armour upgrades would break).
    }

    @Test
    void materialTokensAreNormalisedAsCraftResultsAre() {
        assertEquals(SmithingUpgrades.Kind.UPGRADE,
                shipped().decide("diamond_sword", "minecraft:NETHERITE_UPGRADE_SMITHING_TEMPLATE", "Netherite_Ingot").kind());
    }

    @Test
    void twoRulesForOneBaseTemplateAndAdditionAreRefusedAtConstruction() {
        var a = new SmithingUpgrades.Upgrade("diamond_sword", TEMPLATE, INGOT, "netherite_sword");
        var b = new SmithingUpgrades.Upgrade("diamond_sword", TEMPLATE, INGOT, "other_sword");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SmithingUpgrades(List.of(a, b)));
        assertTrue(e.getMessage().contains("diamond_sword"), e.getMessage());
    }

    @Test
    void anEmptyTableRefusesEveryMintedBase() {
        SmithingUpgrades none = new SmithingUpgrades(List.of());
        assertEquals(SmithingUpgrades.Kind.REFUSE, none.decide("diamond_sword", TEMPLATE, INGOT).kind());
        assertEquals(0, none.size());
        assertEquals(1, shipped().size(), "the control");
    }
}
