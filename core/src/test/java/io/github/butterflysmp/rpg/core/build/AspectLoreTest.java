package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.build.AspectLore.Line;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Build screen's ability lore, rendered FROM THE DERIVED DEFINITION -- the same derive the cast uses --
 * with the base shown only where a number changed (section 2.4.1: "Damage: 9  (12)"). The plan's paper-side
 * BuildLoreTest lives here: the numbers are pure, and paper only colours them.
 */
class AspectLoreTest {

    private static final AbilityDefinition LANCE = new AbilityDefinition("solar_lance", "Solar Lance", "fire", 100,
            new ResourceCost("mana", 25), new CastSpec.Ray(30, null),
            List.of(new EffectSpec.Visual("solar_lance"), new EffectSpec.Damage(12, "fire")));
    private static final AbilityDefinition RECALL = new AbilityDefinition("recall", "Recall", "fire", 200,
            new ResourceCost("mana", 35), new CastSpec.Self(), List.of(new EffectSpec.Damage(8, "fire")));

    /** THE ROW THE "render the lore from the base definition" MUTATION REDDENS: 9, with the base 12. */
    @Test
    void searingLanceReadsNineWithTheBaseTwelve() {
        AspectDefinition searing = new AspectDefinition("searing_lance", "Searing Lance", List.of(), "solar_lance",
                List.of(new EffectSpec.Burst(2.5, List.of(new EffectSpec.Damage(4, "fire")))), List.of(),
                List.of(new NumberChange(AspectField.DAMAGE_AMOUNT, 0, -25)));
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE, List.of(searing));
        assertEquals(List.of(new Line("Damage", "9", "12"), new Line("Cost", "25", null), new Line("Cooldown", "5.0 s", null)),
                AspectLore.lines(LANCE, derived));
    }

    /** banked_embers, HAND-BUILT (its content file was deleted by ruling 26): cost 45 (35), cooldown 12.0 s (10.0 s). */
    @Test
    void bankedEmbersReadsCostAndCooldownWithTheirBases() {
        AspectDefinition banked = new AspectDefinition("banked_embers", "Banked Embers", List.of(), "recall",
                List.of(), List.of(), List.of(new NumberChange(AspectField.COST, 10, 0),
                new NumberChange(AspectField.COOLDOWN_TICKS, 0, 20)));
        AbilityDefinition derived = AspectApplication.deriveUncached(RECALL, List.of(banked));
        assertEquals(List.of(new Line("Damage", "8", null), new Line("Cost", "45", "35"),
                new Line("Cooldown", "12.0 s", "10.0 s")), AspectLore.lines(RECALL, derived));
    }

    @Test
    void noAspectShowsNoBase() {
        assertEquals(List.of(new Line("Damage", "12", null), new Line("Cost", "25", null), new Line("Cooldown", "5.0 s", null)),
                AspectLore.lines(LANCE, LANCE));
    }

    @Test
    void anAbilityWithNoDamageHasNoDamageLine() {
        AbilityDefinition heal = new AbilityDefinition("h", "H", "nature", 40, new ResourceCost("mana", 5),
                new CastSpec.Self(), List.of(new EffectSpec.Heal(6)));
        assertEquals(List.of(new Line("Cost", "5", null), new Line("Cooldown", "2.0 s", null)), AspectLore.lines(heal, heal));
    }
}
