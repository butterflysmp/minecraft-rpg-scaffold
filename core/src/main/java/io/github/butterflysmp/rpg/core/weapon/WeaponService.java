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
 * trigger for an input, checks and consumes the cooldown and mana atomically, and
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
    /**
     * Fire an already-resolved binding ({@code GrantedTriggers.resolve}): the weapon's own trigger or one an active
     * enchant grants it (Lapis Crystal's D1, 2026-11-23). A weapon trigger either way: it carries the level bonus and
     * the weapon's behaviours (B2), and its cooldown is keyed by its own ability id.
     */
    public CastResult fire(CombatantSnapshot caster, TriggerBinding binding, Aim aim) {
        return abilities.fireTrigger(caster, binding.ability(), aim);
    }

    /**
     * {@link #fire(CombatantSnapshot, TriggerBinding, Aim)} with the ACTIVE behaviours of the weapon in hand at the press
     * (B2): a cooldown tune (Fast and Fungal, F7, 2026-11-26) sets the length this press stamps.
     */
    public CastResult fire(CombatantSnapshot caster, TriggerBinding binding, Aim aim,
                           java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour> behaviours) {
        return abilities.fireTrigger(caster, binding.ability(), aim, behaviours);
    }

    public Optional<CastResult> fire(CombatantSnapshot caster, WeaponDefinition weapon,
                                     String input, Aim aim) {
        return weapon.trigger(input)
                .map(binding -> abilities.fireTrigger(caster, binding.ability(), aim));
    }
}
