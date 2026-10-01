package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.NamespacedKey;

/**
 * What a status id *means*, mechanically. Not how strong it is and not how long
 * it lasts: duration and amplifier come from the ability that applies it, via
 * EffectSpec.Status. A status definition maps an id to a mechanic.
 *
 * Sealed so BukkitCombatant.applyStatus switches exhaustively over the kinds.
 */
public sealed interface StatusDefinition
        permits StatusDefinition.Fire, StatusDefinition.Potion,
                StatusDefinition.Immobilize, StatusDefinition.Soaked, StatusDefinition.Scorch,
                StatusDefinition.Wither {

    String id();

    /** A burn. Bukkit has no "burning" potion effect, so this drives setFireTicks. */
    record Fire(String id) implements StatusDefinition {}

    /**
     * A stacking damage-over-time on an OWNED 20-tick clock: {@code min(5% of max health, the cap)}
     * per second, capped by the damage of whatever applied it, credited to the most recent applier.
     *
     * <p><b>A separate kind from {@link Fire}, not a replacement for it.</b> {@code Fire} means "make
     * this thing burn, vanilla-rated" and is still the right answer for anything that wants only the
     * look. Scorch keeps that look -- it still sets fire ticks for the visual -- but owns the damage,
     * which is why {@code RpgListeners.onEnvironmentalDamage} suppresses the {@code FIRE_TICK} of a
     * victim with live stacks. <b>A shared visual is a coupling</b>: use vanilla's burn for its
     * appearance and you import its damage, and since the vanilla damage boundary landed that damage
     * is real, uncapped, and credited to the VICTIM.
     *
     * <p>The mechanic is {@code ScorchStatus}; the arithmetic is {@code core.combat.Scorch}. Duration
     * comes from the ability, as every other status's does.
     */
    record Scorch(String id) implements StatusDefinition {}

    /**
     * OUR wither: a damage-over-time on an owned 40-tick clock, shaped like {@link Scorch} and run by
     * the same store, created a second time ({@code AdapterContext.wither()}). {@code min(5% of max,
     * the cap)} per tick, the cap half the applying hit, 200 ticks, a re-hit resets the window, credit to
     * the most recent applier, armour bypassed. The numbers are {@code core.combat.Wither}'s.
     *
     * <p><b>A NEW KIND, NOT A PARAMETERISED SCORCH (the seat's WS1 ruling), and the sealed type is why.</b>
     * Every exhaustive switch over this interface -- accrual, the boot validator, {@code applyStatus} --
     * became a compile error until it decided about Wither. Under a parameterised {@code kind: scorch}
     * those sites would each need an "is it really scorch" flag, and the one forgotten would make a
     * withered death explode, which looks exactly like the Ignite ruling working.
     *
     * <p><b>{@code immune}: entity type keys the status never lands on</b> (Q-W6, the seat's fill:
     * wither skeletons, the Wither, and the Knell, which is a {@code wither_skeleton}). Authored in
     * {@code withering.yml}, content as data. Matched against the victim's type key
     * ({@code zombie}, {@code wither_skeleton}), so every entity of a listed type is immune -- the ruling
     * is per type, which is why a type key is right here and not the per-entity trap
     * {@code MobDefinition} warns about.
     */
    record Wither(String id, java.util.Set<String> immune) implements StatusDefinition {

        public Wither {
            immune = java.util.Set.copyOf(immune);
        }

        /** True when an entity of type {@code typeKey} (e.g. {@code wither_skeleton}) is immune. */
        public boolean isImmune(String typeKey) {
            return immune.contains(typeKey);
        }
    }

    /**
     * A movement lock: MOVEMENT_SPEED to zero (kills the mob's AI drive) plus per-tick
     * velocity-zero (kills knockback/jumps). The two configurations of this one mechanic:
     *   - Rooted = {@code suppressAttacks=false} -- cannot move; can still turn and melee in range.
     *   - Freeze = {@code suppressAttacks=true}  -- cannot move OR attack; the listeners cancel a
     *              frozen mob's melee, projectiles, and creeper detonation.
     * The general kind name is deliberate -- Freeze reuses the immobilize rather than forcing a
     * second sealed kind. Duration comes from the ability.
     */
    record Immobilize(String id, boolean suppressAttacks) implements StatusDefinition {}

    /**
     * A vanilla potion effect. The key's syntax is validated at load, which needs
     * no server; whether it names a real effect is checked by ContentValidator at
     * startup, once Registry.MOB_EFFECT is reachable.
     */
    record Potion(String id, NamespacedKey potionType) implements StatusDefinition {}

    /**
     * A stacking, multiplicative movement-slow: each stack multiplies speed by 0.9, floored
     * at 0.6x base. The stack count is real per-target state, and the speed modifier must be
     * fully removed at expiry -- a leaked modifier is a permanently-slow mob. The 0.9/0.6
     * tuning is named-constant in SoakedStatus for now; it moves to YAML when the curve is
     * tuned in play. Config-named (not "StackingSlow") because nothing else reuses it yet.
     */
    record Soaked(String id) implements StatusDefinition {}
}
