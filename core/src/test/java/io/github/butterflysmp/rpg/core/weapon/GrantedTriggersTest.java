package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE ENCHANT-GRANTED TRIGGER (Lapis Crystal's D1, the seat's message of 2026-11-23): granted when the enchant is active,
 * absent when it is not, absent on ANOTHER weapon carrying the same state (the dev command can stage it there), and never
 * in place of a trigger the weapon authors.
 */
class GrantedTriggersTest {

    private static final AbilityDefinition BEAM = new AbilityDefinition("staff/right_click", "Beam", "kinetic", 20,
            ResourceCost.FREE, new CastSpec.Ray(26, null, false, 0), List.of(new EffectSpec.Damage(21, "kinetic")));
    private static final AbilityDefinition THROW = new AbilityDefinition("crystal/left_click", "Crystal", "kinetic", 600,
            new ResourceCost("mana", 30), new CastSpec.Self(), List.of());
    private static final ActiveBehaviour GRANT = new ActiveBehaviour("crystal", 1,
            new EnchantBehaviour.GrantTrigger("staff", "left_click", THROW));

    private static WeaponDefinition weapon(String id, TriggerBinding... triggers) {
        return new WeaponDefinition(id, id, "kinetic", Rarity.COMMON, List.of(triggers));
    }

    @Test
    void grantedWhenActive() {
        WeaponDefinition staff = weapon("staff", new TriggerBinding("right_click", BEAM));
        assertEquals(Optional.of(new TriggerBinding("left_click", THROW)),
                GrantedTriggers.resolve(staff, "left_click", List.of(GRANT)));
        assertEquals(List.of(new TriggerBinding("left_click", THROW)), GrantedTriggers.granted(staff, List.of(GRANT)));
        assertEquals(Optional.of(new TriggerBinding("right_click", BEAM)),
                GrantedTriggers.resolve(staff, "right_click", List.of(GRANT)), "the control: its own trigger still fires");
    }

    /** Not active (B2): the held weapon's behaviours do not hold it, so there is nothing to fire and nothing to show. */
    @Test
    void absentWhenNotActive() {
        WeaponDefinition staff = weapon("staff", new TriggerBinding("right_click", BEAM));
        assertEquals(Optional.empty(), GrantedTriggers.resolve(staff, "left_click", List.of()));
        assertEquals(List.of(), GrantedTriggers.granted(staff, List.of()));
    }

    /** The same active state on another weapon (as the dev command can stage it) grants that weapon nothing. */
    @Test
    void absentOnAnotherWeaponCarryingTheSameState() {
        WeaponDefinition sword = weapon("sword", new TriggerBinding("right_click", BEAM));
        assertEquals(Optional.empty(), GrantedTriggers.resolve(sword, "left_click", List.of(GRANT)));
        assertEquals(List.of(), GrantedTriggers.granted(sword, List.of(GRANT)));
    }

    /** A weapon that authors the gesture keeps its own trigger: a grant never replaces it, and is not shown beside it. */
    @Test
    void aWeaponsOwnTriggerIsNeverReplaced() {
        AbilityDefinition own = new AbilityDefinition("staff/left_click", "Own", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Self(), List.of());
        WeaponDefinition staff = weapon("staff", new TriggerBinding("right_click", BEAM), new TriggerBinding("left_click", own));
        assertEquals(Optional.of(new TriggerBinding("left_click", own)),
                GrantedTriggers.resolve(staff, "left_click", List.of(GRANT)));
        assertEquals(List.of(), GrantedTriggers.granted(staff, List.of(GRANT)));
    }

    /** Its own cooldown: the granted ability's id is not the weapon's right click's. */
    @Test
    void theGrantedAbilityHasItsOwnId() {
        assertEquals("crystal/left_click", GrantedTriggers.granted(weapon("staff", new TriggerBinding("right_click", BEAM)),
                List.of(GRANT)).get(0).ability().id());
    }
}
