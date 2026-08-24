package io.github.butterflysmp.rpg.core.weapon;

import java.util.HashMap;
import java.util.Map;

/**
 * Which equipped {@code +N <Class> Damage} grants are ACTIVE right now: the ones whose class
 * matches the class of the weapon currently held. A {@code +3 Magic} ring does nothing while you
 * hold a sword; a {@code +5 Ranged} charm does nothing on a staff.
 *
 * This is the class-typed gate, and it keys on the HELD WEAPON'S CLASS -- deliberately not on
 * {@code DamagePayload.isBasicAttack}. Keying on the payload would mean a class bonus reached only
 * stat-reading basic attacks, which is exactly the dead end recorded in NEXT.md: {@code ember_staff}
 * carries a LITERAL damage and declares {@code attack_damage: 0}, so a payload-keyed
 * {@code +Magic Damage} would have had nothing to grip and would have shipped silently doing
 * nothing. Keying on the held weapon lets the bonus reach every direct damage effect the caster
 * deals -- literal and stat-reading alike -- with no weapon conversion required.
 *
 * <p>The rule lives in {@code core} and is pure because it is the part worth testing. Its paper
 * counterpart ({@code ClassDamageModifierItems}) needs a live {@code Player} to scan equipment, and
 * a Bukkit {@code Player} cannot be constructed in a unit test -- which is why neither
 * {@code HealthModifierItems} nor {@code AttackSpeedModifierItems} has one. Same split as
 * {@link io.github.butterflysmp.rpg.core.mob.MobSeeding}: core owns the decision, paper owns the read.
 */
public final class ClassDamageModifiers {

    private ClassDamageModifiers() {}

    /**
     * One equipped item's class-damage grant: which class it boosts, and by how much.
     *
     * A record is an immutable data carrier -- a fixed set of components with the accessors,
     * {@code equals} and {@code hashCode} generated -- so this is the whole declaration.
     */
    public record ClassGrant(WeaponClass weaponClass, double amount) {}

    /**
     * The class-damage modifiers active for a caster holding a {@code heldClass} weapon, given every
     * equipped grant keyed by its slot. The returned map is the "desired" set a
     * {@code CombatantStats.reconcileClassDamageModifiers} converges to: same source keys in, so the
     * leak-proof diff drops a grant the moment its slot stops yielding one.
     *
     * <p>{@code heldClass} is null when the hand holds nothing, or nothing of ours. That returns an
     * EMPTY map, and that is what makes "a class bonus cannot resurrect an unarmed hit" STRUCTURAL
     * rather than a convention someone has to remember: no weapon means no class means no matching
     * grant means a bonus of 0, so weapon-only melee survives this pass untouched.
     *
     * <p>Grants for other classes are simply absent from the result -- not zeroed -- so the
     * reconciler removes their sources rather than leaving a 0-valued modifier behind. Two slots
     * granting the SAME class both survive, under their own source keys, and {@code Stat} sums them.
     */
    public static Map<String, Double> matching(WeaponClass heldClass, Map<String, ClassGrant> equipped) {
        Map<String, Double> desired = new HashMap<>();
        if (heldClass == null) return desired;
        for (Map.Entry<String, ClassGrant> entry : equipped.entrySet()) {
            ClassGrant grant = entry.getValue();
            if (grant != null && grant.weaponClass() == heldClass) {
                desired.put(entry.getKey(), grant.amount());
            }
        }
        return desired;
    }
}