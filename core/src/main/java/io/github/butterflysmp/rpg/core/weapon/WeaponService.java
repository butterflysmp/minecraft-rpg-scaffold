package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;

import java.util.Optional;

/**
 * Firing a weapon trigger, end to end, with zero knowledge of Minecraft. The paper
 * side only resolves item -> weapon and input-event -> input string; the selection
 * and the cast decision live here, so they are unit-testable against FakeWorld with
 * no server.
 *
 * Like AbilityService.cast, fire() DECIDES; it does not EXECUTE. It selects the
 * trigger for an input, checks and consumes the cooldown and energy atomically, and
 * returns a description. The caller passes the Success to a CastExecutor on the
 * region thread.
 *
 * A weapon trigger is NOT archetype-gated: it goes through AbilityService.fireTrigger,
 * which skips the castable gate cast() applies. That is the one behavioral difference
 * from an ability cast, and it is the invariant WeaponServiceTest pins.
 */
public final class WeaponService {
    private final AbilityService abilities;

    public WeaponService(AbilityService abilities) {
        this.abilities = abilities;
    }

    /**
     * Fire the trigger bound to {@code input} on {@code weapon}.
     *
     * @return empty if the weapon has no binding for that input -- selection failed,
     *         nothing was checked or spent. Otherwise the outcome of firing: a Success,
     *         or a rejection (OnCooldown / InsufficientResource). Never UnknownAbility
     *         or Locked -- those are ability-registry and archetype concepts a weapon
     *         does not touch.
     */
    public Optional<CastResult> fire(CombatantSnapshot caster, WeaponDefinition weapon,
                                     String input, Aim aim) {
        return weapon.trigger(input)
                .map(binding -> abilities.fireTrigger(caster, binding.ability(), aim));
    }
}
