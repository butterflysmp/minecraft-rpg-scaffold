package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The summing rule that turns a shield's enchant state into one percentage.
 *
 * <p>Reachable without a server only because the PDC decode was split off: {@code EnchantState},
 * {@code EnchantDefinition} and {@code EnchantRegistry} are all plain values. Its sibling
 * {@code DamageEnchantItems} has no such overload and is boot-witnessed entirely, so this is the
 * first coverage the effect-scan pattern has ever had.
 */
class BlockEnchantItemsTest {

    private static final double EPS = 1e-9;

    private static EnchantRegistry registry(EnchantDefinition... definitions) {
        EnchantRegistry registry = new EnchantRegistry();
        for (EnchantDefinition definition : definitions) registry.register(definition);
        return registry;
    }

    private static EnchantDefinition bulwark() {
        return new EnchantDefinition("bulwark", "Bulwark", 3,
                EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, 10, 15));
    }

    private static EnchantDefinition unbreaking() {
        return new EnchantDefinition("unbreaking", "Unbreaking", 3,
                EnchantEffect.DURABILITY, null, List.of());
    }

    /** One slot, one candidate, unlocked to {@code level} and made active. */
    private static EnchantState activeAt(String enchantId, int level) {
        return EnchantState.empty()
                .addCandidate(0, enchantId)
                .withLevel(0, 0, level)
                .withActive(0, 0);
    }

    @Test
    void anUnenchantedShieldContributesExactlyZero() {
        // THE branch every shield in the game takes, and zero has to be an exact identity because
        // Bulwark.effectiveDr(dr, 0) is what an unenchanted block composes through.
        assertEquals(0.0, BlockEnchantItems.percentFor(
                EnchantState.empty(), registry(bulwark()), EnchantEffect.BLOCK_DR));
    }

    @Test
    void theLevelReachesTheCurveRatherThanTheTopOfIt() {
        EnchantRegistry enchants = registry(bulwark());
        assertEquals(5.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 1), enchants, EnchantEffect.BLOCK_DR), EPS);
        assertEquals(10.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 2), enchants, EnchantEffect.BLOCK_DR), EPS);
        assertEquals(15.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 3), enchants, EnchantEffect.BLOCK_DR), EPS);
    }

    @Test
    void aLockedCandidateGrantsNothingBecauseItIsNotActive() {
        // Rolled but unpaid-for. The candidate is on the item and renders in the table; it must not
        // block anything until the player buys it.
        EnchantState rolled = EnchantState.empty().addCandidate(0, "bulwark");
        assertEquals(0.0, BlockEnchantItems.percentFor(
                rolled, registry(bulwark()), EnchantEffect.BLOCK_DR));
    }

    @Test
    void anEnchantBindingAnotherMechanismIsSkippedRatherThanCountedAsZero() {
        // Unbreaking on a shield is the combination Slice 1 shipped working. It must contribute
        // NOTHING to the block, and the filter is effect() -- not "it has no curve", which would be
        // a different rule that happens to agree today.
        assertEquals(0.0, BlockEnchantItems.percentFor(
                activeAt("unbreaking", 3), registry(unbreaking()), EnchantEffect.BLOCK_DR));

        // And the effect asked for is genuinely the discriminator: ask for DURABILITY and the same
        // state still yields 0, because Unbreaking's curve is Java and its percent list is empty.
        assertEquals(0.0, BlockEnchantItems.percentFor(
                activeAt("unbreaking", 3), registry(unbreaking()), EnchantEffect.DURABILITY));
    }

    @Test
    void aDanglingIdGrantsNothingRatherThanThrowing() {
        // Reachable: the loader fail-softs a malformed file and the item's blob still names it, and
        // EnchantLore deliberately still RENDERS it. So the tooltip can promise Bulwark while the
        // registry has never heard of it -- and the block must fail toward granting nothing.
        assertEquals(0.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 3), registry(unbreaking()), EnchantEffect.BLOCK_DR));
        assertEquals(0.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 3), new EnchantRegistry(), EnchantEffect.BLOCK_DR));
    }

    @Test
    void theSameEnchantInTwoSlotsCountsONCE_AtTheHigherLevel() {
        // THE reason summing is safe. EnchantState.effective() resolves a duplicate id to the
        // HIGHEST level either slot holds it at, never the sum -- so this sums over DISTINCT ids.
        //
        // Were that not so, two columns of Bulwark III would contribute 30% and walk a 0.5 shield
        // to dr 0.8; four columns would reach 1.0 and make it invulnerable. This test is what pins
        // the assumption the summing rests on, in the class that does the summing.
        EnchantState both = EnchantState.empty()
                .addCandidate(0, "bulwark").withLevel(0, 0, 1).withActive(0, 0)
                .addCandidate(1, "bulwark").withLevel(1, 0, 3).withActive(1, 0);

        assertEquals(15.0, BlockEnchantItems.percentFor(
                both, registry(bulwark()), EnchantEffect.BLOCK_DR), EPS,
                "a duplicate id must resolve to the higher level, never 5 + 15");
    }

    @Test
    void twoDIFFERENTBlockEnchantsSumAdditively() {
        // The composition rule for the day a second BLOCK_DR enchant ships. Percentages are
        // genuinely additive -- the same rule DamageEnchants documents -- so this is a sum, and
        // filtering by effect() rather than by a hardcoded id is what lets it happen with no
        // recompile.
        EnchantDefinition aegis = new EnchantDefinition("aegis", "Aegis", 3,
                EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(2, 4, 6));

        EnchantState both = EnchantState.empty()
                .addCandidate(0, "bulwark").withLevel(0, 0, 3).withActive(0, 0)
                .addCandidate(1, "aegis").withLevel(1, 0, 2).withActive(1, 0);

        assertEquals(19.0, BlockEnchantItems.percentFor(
                both, registry(bulwark(), aegis), EnchantEffect.BLOCK_DR), EPS,
                "15 from Bulwark III plus 4 from Aegis II");
    }

    @Test
    void nullsAreTotalRatherThanThrowingFromInsideABlock() {
        // This runs inside the mob->player damage rider. An exception there loses the whole hit.
        assertEquals(0.0, BlockEnchantItems.percentFor(
                (EnchantState) null, registry(bulwark()), EnchantEffect.BLOCK_DR));
        assertEquals(0.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 3), null, EnchantEffect.BLOCK_DR));
        assertEquals(0.0, BlockEnchantItems.percentFor(
                activeAt("bulwark", 3), registry(bulwark()), null));
    }
}
