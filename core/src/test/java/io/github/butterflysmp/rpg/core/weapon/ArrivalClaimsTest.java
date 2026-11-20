package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CONVERT ON ARRIVAL (the seat's ruling, 2026-11-20; Ben's answer 5, "Yes", any route). The pure decision: an untagged
 * vanilla item whose material a weapon claims is MINTED; a tagged one is LEFT; an unclaimed one is LEFT. A weapon claims
 * its material by its craft_result, or by being the RESULT of a smithing upgrade (the netherite sword, which is not
 * crafted). Each row names the mutation it reddens.
 */
class ArrivalClaimsTest {

    private static WeaponDefinition sword(String id, String craftResult) {
        return new WeaponDefinition(id, id, "kinetic", Rarity.COMMON, WeaponClass.MELEE, id, 10.0, 1.6, 0.0, 0, 0,
                List.of(new TriggerBinding("left_click", new AbilityDefinition(id + "/left_click", id, "kinetic", 0,
                        null, null, List.of(), List.of()))),
                List.of(), Optional.ofNullable(craftResult), false);
    }

    private static final WeaponDefinition IRON = sword("iron_sword", "iron_sword");
    private static final WeaponDefinition DIAMOND = sword("diamond_sword", "diamond_sword");
    private static final WeaponDefinition NETHERITE = sword("netherite_sword", null);

    private static final SmithingUpgrades UPGRADES = new SmithingUpgrades(List.of(new SmithingUpgrades.Upgrade(
            "diamond_sword", "netherite_upgrade_smithing_template", "netherite_ingot", "netherite_sword")));

    private static ArrivalClaims claims(List<WeaponDefinition> weapons, SmithingUpgrades upgrades, List<String> problems) {
        CraftResultIndex crafts = CraftResultIndex.build(weapons, List.of(), problems::add);
        return ArrivalClaims.build(weapons, crafts, upgrades, problems::add);
    }

    private static ArrivalClaims shipped() {
        return claims(List.of(IRON, DIAMOND, NETHERITE), UPGRADES, new ArrayList<>());
    }

    @Test
    void anUntaggedClaimedSwordIsMinted() {
        ArrivalClaims.Decision d = shipped().decide(false, "iron_sword");
        assertEquals(ArrivalClaims.Kind.MINT, d.kind());
        assertEquals("iron_sword", d.weaponId());
        // Mutation: never MINT -> reddens.
    }

    @Test
    void aTaggedItemIsLeftWhateverItsMaterial() {
        assertEquals(ArrivalClaims.Kind.LEAVE_TAGGED, shipped().decide(true, "iron_sword").kind(),
                "a minted sword (any weapon, ironblade included) is never re-minted on arrival");
        // Mutation: drop the tagged arm -> reddens (every minted sword would be re-rolled on every slot change).
    }

    @Test
    void anUnclaimedItemIsLeft() {
        assertEquals(ArrivalClaims.Kind.LEAVE_UNCLAIMED, shipped().decide(false, "stick").kind());
        assertEquals(ArrivalClaims.Kind.LEAVE_UNCLAIMED, shipped().decide(false, null).kind(), "an empty slot");
        // Mutation: MINT an unclaimed item -> reddens.
    }

    @Test
    void theNetheriteSwordIsClaimedAsTheResultOfItsUpgrade() {
        ArrivalClaims.Decision d = shipped().decide(false, "netherite_sword");
        assertEquals(ArrivalClaims.Kind.MINT, d.kind(), "a netherite sword from loot must convert though it is not crafted");
        assertEquals("netherite_sword", d.weaponId());
        assertEquals(ArrivalClaims.Kind.LEAVE_UNCLAIMED,
                claims(List.of(IRON, DIAMOND, NETHERITE), new SmithingUpgrades(List.of()), new ArrayList<>())
                        .decide(false, "netherite_sword").kind(), "the control: with no upgrade, nothing claims it");
        // Mutation: skip the upgrade results -> reddens on the first assertion.
    }

    @Test
    void materialsNormaliseAsCraftResultsDo() {
        assertEquals("iron_sword", shipped().decide(false, "minecraft:IRON_SWORD").weaponId());
    }

    @Test
    void anUpgradeResultWhoseMaterialAnotherWeaponCraftClaimsIsContestedAndClaimsNothing() {
        // Fail closed, as craft_result is: two weapons for one arriving material means neither.
        WeaponDefinition rival = sword("netherite_sword_rival", "netherite_sword");
        WeaponDefinition rivalOnNetherite = new WeaponDefinition("netherite_sword_rival", "rival", "kinetic", Rarity.COMMON,
                WeaponClass.MELEE, "netherite_sword", 10.0, 1.6, 0.0, 0, 0, rival.triggers(), List.of(),
                Optional.of("netherite_sword"), false);
        List<String> problems = new ArrayList<>();
        ArrivalClaims c = claims(List.of(IRON, DIAMOND, NETHERITE, rivalOnNetherite), UPGRADES, problems);
        assertEquals(ArrivalClaims.Kind.LEAVE_UNCLAIMED, c.decide(false, "netherite_sword").kind());
        assertTrue(problems.stream().anyMatch(p -> p.startsWith("Refusing arrival claim 'netherite_sword'")), problems.toString());
        assertEquals(ArrivalClaims.Kind.MINT, c.decide(false, "iron_sword").kind(), "the control: the rest still claim");
    }

    @Test
    void onlyWeaponsClaimOnArrival() {
        // A shield's or tool's craft_result mints on craft only; arrival conversion is the swords' (the brief, part h).
        ShieldDefinition shield = new ShieldDefinition("shield", "shield", Rarity.COMMON, "shield", 0.35, List.of(),
                Optional.of("shield"));
        List<String> problems = new ArrayList<>();
        CraftResultIndex crafts = CraftResultIndex.build(List.of(IRON, shield), List.of(), problems::add);
        ArrivalClaims c = ArrivalClaims.build(List.of(IRON), crafts, new SmithingUpgrades(List.of()), problems::add);
        assertEquals(ArrivalClaims.Kind.LEAVE_UNCLAIMED, c.decide(false, "shield").kind());
        assertEquals(1, c.size(), "iron_sword only");
        assertEquals(3, shipped().size(), "the control: iron, diamond, netherite");
    }
}
