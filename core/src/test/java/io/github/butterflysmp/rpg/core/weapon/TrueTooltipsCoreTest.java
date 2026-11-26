package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.BehaviourCasts;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * True tooltips, the core half (the seat's T1, T7, D1, 2026-11-26): the extracted resolvers, the description tokens, the
 * item's enchant terms and the behaviour switch. Each fixture is a shipped weapon's shape, numbers READ from its YAML.
 */
class TrueTooltipsCoreTest {

    private static AbilityDefinition ability(String id, CastSpec cast, List<EffectSpec> onHit, List<String> description) {
        return new AbilityDefinition(id, "Cast", "kinetic", 30, new ResourceCost("mana", 40), cast, onHit, description);
    }

    private static AbilityDefinition emerald(List<String> description) {
        return ability("cursed_emerald/right_click", new CastSpec.Volley(20, 3, 2, new CastSpec.Ray(32, "emerald_beam")),
                List.of(new EffectSpec.Damage(27, "kinetic")), description);
    }

    private static AbilityDefinition breacher() {
        return ability("breacher/right_click", new CastSpec.Projectile(1.5, 0.0, 40, null, "cobblestone", null, null,
                        new CastSpec.Spread(4, 6, 90)),
                List.of(new EffectSpec.Damage(40, "kinetic"), new EffectSpec.Knockback(0.4)), List.of("Four chunks"));
    }

    private static AbilityDefinition lantern() {
        return ability("living_lantern/right_click",
                new CastSpec.Projectile(0.35, 0.0, 80, null, null, new CastSpec.Homing(0.45, 0, 7)),
                List.of(new EffectSpec.Visual("lantern_hit"), new EffectSpec.Damage(45, "fire")), List.of("flame"));
    }

    private static WeaponDefinition weapon(String id, AbilityDefinition... abilities) {
        List<TriggerBinding> triggers = java.util.Arrays.stream(abilities)
                .map(a -> new TriggerBinding(a.id().substring(a.id().indexOf('/') + 1), a)).toList();
        return new WeaponDefinition(id, id, "kinetic", Rarity.COMMON, WeaponClass.MAGE, id, 0.0, 0.0, 0.0, 0, 0,
                triggers, List.of(), Optional.empty(), false);
    }

    private static ActiveBehaviour active(String id, int level, EnchantBehaviour b) {
        return new ActiveBehaviour(id, level, b);
    }

    // ------------------------------------------------------------------ the extracted resolvers

    @Test
    void replaceDamageSwapsEveryLiteralDamageAndKeepsTheRest() {
        List<EffectSpec> out = BehaviourCasts.replaceDamage(breacher().onHit(), 35, "kinetic");
        assertEquals(List.of(new EffectSpec.Damage(35, "kinetic"), new EffectSpec.Knockback(0.4)), out);
    }

    @Test
    void focusedBeamActsOnlyOnARayOfItsOwnWeapon() {
        var beam = active("focused_beam", 2, new EnchantBehaviour.FocusedBeam("cursed_emerald", 48, List.of(108.0, 162.0, 216.0), "kinetic"));
        assertTrue(BehaviourCasts.focusedBeam("cursed_emerald/right_click", new CastSpec.Ray(32), List.of(beam)).isPresent());
        assertTrue(BehaviourCasts.focusedBeam("knell_beam/right_click", new CastSpec.Ray(32), List.of(beam)).isEmpty());
        assertTrue(BehaviourCasts.focusedBeam("cursed_emerald/right_click",
                new CastSpec.Projectile(1, 0, 20), List.of(beam)).isEmpty());
    }

    @Test
    void shatterNeedsASpread() {
        var caps = active("shatter_caps", 1, new EnchantBehaviour.Shatter("breacher", 5, 2.5, "stone", List.of(35.0, 60.0, 85.0), "kinetic"));
        assertTrue(BehaviourCasts.shatter("breacher/right_click", (CastSpec.Projectile) breacher().cast(), List.of(caps)).isPresent());
        assertTrue(BehaviourCasts.shatter("breacher/right_click", new CastSpec.Projectile(1.5, 0, 40), List.of(caps)).isEmpty());
    }

    @Test
    void airburstSplitsOnlyInsideTheAuthoredLifetime() {
        var burst = new EnchantBehaviour.Airburst(6, 3, 12, 0.7, 14, List.of(25.0, 30.0, 35.0), "fire", null);
        // 6 blocks at 0.35 a tick is ceil(17.14) = 18 ticks: inside 80, splits; inside 18 it does not.
        assertTrue(BehaviourCasts.airburst(new CastSpec.Projectile(0.35, 0, 80), List.of(active("a", 1, burst))).isPresent());
        assertTrue(BehaviourCasts.airburst(new CastSpec.Projectile(0.35, 0, 18), List.of(active("a", 1, burst))).isEmpty());
        assertTrue(BehaviourCasts.airburst(new CastSpec.Projectile(0.35, 0, 19), List.of(active("a", 1, burst))).isPresent());
    }

    @Test
    void orbShotIsTheBeamsFirstDamageTimesTheLevelsPercent() {
        var shot = new CastSpec.OrbShot(15, List.of(30.0, 50.0, 70.0), "kinetic", null);
        List<EffectSpec> beam = List.of(new EffectSpec.Visual("v"), new EffectSpec.Damage(21, "kinetic"));
        assertEquals(21 * 50.0 / 100.0, BehaviourCasts.orbShotAmount(beam, shot, 2).getAsDouble());
        assertEquals(21 * 70.0 / 100.0, BehaviourCasts.orbShotAmount(beam, shot, 9).getAsDouble());   // clamped to III
        assertTrue(BehaviourCasts.orbShotAmount(List.of(new EffectSpec.Visual("v")), shot, 1).isEmpty());
    }

    // ------------------------------------------------------------------ D1: tokens

    @Test
    void shotsTokensFillFromTheEffectiveCast() {
        List<String> authored = List.of("Channel, then loose {shots_word}", "bolts ({shots}).");
        assertEquals(List.of("Channel, then loose three", "bolts (3)."), DescriptionTokens.fill(authored, emerald(authored).cast()));
        CastSpec six = new CastSpec.Volley(20, 6, 2, new CastSpec.Ray(32));
        assertEquals(List.of("Channel, then loose six", "bolts (6)."), DescriptionTokens.fill(authored, six));
    }

    @Test
    void theWordsRunOneToTwelveThenNumerals() {
        assertEquals("one", DescriptionTokens.word(1));
        assertEquals("twelve", DescriptionTokens.word(12));
        assertEquals("13", DescriptionTokens.word(13));
    }

    @Test
    void anUnknownTokenAndATokenOnACastWithNoCountAreRefusedByName() {
        var unknown = assertThrows(IllegalArgumentException.class,
                () -> DescriptionTokens.validate(List.of("a {damage} b"), emerald(List.of()).cast()));
        assertTrue(unknown.getMessage().contains("{damage}"), unknown.getMessage());
        var noCount = assertThrows(IllegalArgumentException.class,
                () -> DescriptionTokens.validate(List.of("{shots} beams"), new CastSpec.Ray(32)));
        assertTrue(noCount.getMessage().contains("{shots}"), noCount.getMessage());
        DescriptionTokens.validate(List.of("{shots_word} chunks"), breacher().cast());   // a spread has a count
        DescriptionTokens.validate(List.of("no tokens at all"), new CastSpec.Ray(32));
    }

    // ------------------------------------------------------------------ the item's terms

    @Test
    void theItemTermIsTheEnginesHitBaseWithNoPlayerBonus() {
        assertEquals(17 * 1.15, new ItemEnchantTerms(List.of(), 15, 0, Map.of()).itemDamage(17));
        assertEquals(32.0, ItemEnchantTerms.NONE.itemDamage(32));
    }

    @Test
    void critIsTheWeaponsOwnPlusItsEnchantsPoints() {
        WeaponDefinition bottle = new WeaponDefinition("broken_bottle", "B", "kinetic", Rarity.COMMON, WeaponClass.MELEE,
                "glass_bottle", 18, 2.0, 0.0, 0, 0, weapon("x", emerald(List.of())).triggers(), List.of(),
                Optional.empty(), false, null, 0.10);
        assertEquals(0.10, ItemEnchantTerms.NONE.critChance(bottle));
        assertEquals("+25%", WeaponLoreLines.critChanceLabel(new ItemEnchantTerms(List.of(), 0, 15, Map.of()).critChance(bottle)));
    }

    @Test
    void damageNumberRoundsAwayFloatingPointNoiseOnly() {
        assertEquals("19.55", WeaponLoreLines.damageNumber(17 * 1.15));
        assertEquals("33.25", WeaponLoreLines.damageNumber(33.25));
        assertEquals("32", WeaponLoreLines.damageNumber(32.0));
    }

    // ------------------------------------------------------------------ T7: the switch

    @Test
    void focusedBeamRewritesTheVolleyToOneBeamWithItsProse() {
        AbilityDefinition a = emerald(List.of("three"));
        var beam = active("focused_beam", 2, new EnchantBehaviour.FocusedBeam("cursed_emerald", 48, List.of(108.0, 162.0, 216.0), "kinetic"));
        var terms = new ItemEnchantTerms(List.of(beam), 0, 0, Map.of("focused_beam", List.of("one beam")));
        var block = BehaviourBlocks.rewrite(weapon("cursed_emerald", a), a, terms, List.of(a)).orElseThrow();
        assertEquals(List.of("one beam"), block.description());
        assertEquals(List.of(new BehaviourBlocks.DamageRow(162, "kinetic", OptionalInt.empty())), block.rows());
    }

    @Test
    void shatterRewritesToTheStoneThenTheWeaponsOwnChunks() {
        AbilityDefinition a = breacher();
        var caps = active("shatter_caps", 3, new EnchantBehaviour.Shatter("breacher", 5, 2.5, "stone", List.of(35.0, 60.0, 85.0), "kinetic"));
        var block = BehaviourBlocks.rewrite(weapon("breacher", a), a, new ItemEnchantTerms(List.of(caps), 0, 0, Map.of()),
                List.of(a)).orElseThrow();
        assertEquals(List.of("Four chunks"), block.description(), "no authored prose: the ability's own stays");
        assertEquals(List.of(new BehaviourBlocks.DamageRow(85, "kinetic", OptionalInt.empty()),
                new BehaviourBlocks.DamageRow(40, "kinetic", OptionalInt.of(4))), block.rows());
    }

    @Test
    void airburstRewritesToTheFlameThenItsBursts() {
        AbilityDefinition a = lantern();
        var burst = active("airburst_flames", 1, new EnchantBehaviour.Airburst(6, 3, 12, 0.7, 14, List.of(25.0, 30.0, 35.0), "fire", null));
        var block = BehaviourBlocks.rewrite(weapon("living_lantern", a), a, new ItemEnchantTerms(List.of(burst), 0, 0, Map.of()),
                List.of(a)).orElseThrow();
        assertEquals(List.of(new BehaviourBlocks.DamageRow(45, "fire", OptionalInt.empty()),
                new BehaviourBlocks.DamageRow(25, "fire", OptionalInt.of(3))), block.rows());
    }

    @Test
    void lapisCrystalsGrantedBlockGainsItsShotAsANumber() {
        AbilityDefinition staffBeam = ability("lapis_staff/right_click", new CastSpec.Ray(26, "lapis_beam"),
                List.of(new EffectSpec.Damage(21, "kinetic")), List.of());
        AbilityDefinition orb = ability("lapis_crystal/left_click", new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea",
                new CastSpec.OrbShot(15, List.of(30.0, 50.0, 70.0), "kinetic", null)), List.of(), List.of("Throws"));
        var grant = active("lapis_crystal", 3, new EnchantBehaviour.GrantTrigger("lapis_staff", "left_click", orb));
        var block = BehaviourBlocks.rewrite(weapon("lapis_staff", staffBeam), orb,
                new ItemEnchantTerms(List.of(grant), 0, 0, Map.of()), List.of(staffBeam)).orElseThrow();
        assertEquals(List.of(new BehaviourBlocks.DamageRow(21 * 70.0 / 100.0, "kinetic", OptionalInt.empty())), block.rows());
        assertTrue(BehaviourBlocks.rewrite(weapon("lapis_staff", staffBeam), staffBeam,
                new ItemEnchantTerms(List.of(grant), 0, 0, Map.of()), List.of(staffBeam)).isEmpty(), "the beam is untouched");
    }

    @Test
    void conditionalAndTuneBehavioursRewriteNothing() {
        AbilityDefinition a = lantern();
        List<ActiveBehaviour> none = List.of(
                active("wildfire", 3, new EnchantBehaviour.HitChain("living_lantern", 60, 60, List.of(5.0, 10.0, 15.0))),
                active("kinetic_tremors", 3, new EnchantBehaviour.Shockwave(3, 60, 5, List.of(10.0), List.of(3.0), "kinetic", null)),
                active("mana_burn", 3, new EnchantBehaviour.ScorchBoost(List.of(10.0, 20.0, 30.0))),
                active("large_sparks", 3, new EnchantBehaviour.CastTune("living_lantern",
                        EnchantBehaviour.CastTune.Quantity.DAMAGE, List.of(4.0, 8.0, 12.0))));
        assertTrue(BehaviourBlocks.rewrite(weapon("living_lantern", a), a, new ItemEnchantTerms(none, 0, 0, Map.of()),
                List.of(a)).isEmpty());
    }
}
