package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.UUID;

/**
 * Everything worth knowing about a combatant, read once, on the thread that owns it.
 *
 * This exists because a read cannot hop a thread. Every mutator on CombatantHandle
 * dispatches onto the owning entity's thread and returns nothing; a reader has to return
 * a value, and blocking on a future from a region thread deadlocks. So the reads happen
 * exactly where they are legal -- at the moment the world hands the combatant over -- and
 * are frozen into this record.
 *
 * A record is an immutable data carrier, so this may safely cross a tick or a thread
 * boundary where a live entity may not. It may of course be STALE by then: a snapshot is a
 * photograph, not a window.
 *
 * {@code player} is the one piece of faction the engine currently needs: mob-only effects
 * (statuses, and a dash's payload) read it to leave players out of the target set. It is a
 * read like any other -- captured on the owning thread, frozen here -- so the rule that
 * excludes players can be exercised by a core unit test rather than living in adapter wiring
 * no test reaches.
 *
 * {@code attackSpeed} is the caster's resolved attack-speed multiplier, and it rides here for a
 * specific reason: {@code AbilityService.resolve} scales a basic attack's cooldown by it, and
 * resolve runs wherever the caster happened to call in from. Reading the stat store at that point
 * would be a cross-thread read of another combatant's state -- the exact defect the snapshot/handle
 * split exists to prevent. Captured on the caster's own thread at snapshot time, it is just
 * another frozen read. A photograph, so it may be stale by a tick; that is fine for a cadence.
 *
 * {@code attackDamage} is the caster's resolved ATTACK_DAMAGE stat, and it rides here for the same
 * reason one pass later. A {@code WeaponDamage} payload used to resolve its amount at HIT time
 * through a {@code CombatWorld.attackDamage(casterId)} port method: safe for melee, where the caster
 * is within reach of the target and so shares its region, but a Folia race for a PROJECTILE, whose
 * impact resolves on the TARGET'S region, cross-region from the caster. That port method no longer
 * exists -- the amount is frozen here at cast time and projected into {@link Caster}, so melee and
 * ranged read one identical, thread-legal value and the hit-time read is not merely discouraged but
 * unavailable. 0.0 when the combatant is untracked, matching {@code CombatantStats.attackValue}:
 * attack damage is a summand, so 0 correctly means "deals nothing" (unlike attack speed, a divisor,
 * which defaults to 1.0).
 *
 * {@code classDamageBonus} is the caster's resolved CLASS-DAMAGE stat: the sum of their equipped
 * {@code +N <Class> Damage} gear whose class matches the class of the weapon they are HOLDING. It
 * rides here for the third time for the same reason -- {@code EffectApplier} adds it to every direct
 * damage effect, and a projectile's payload lands on the target's region, cross-region from the
 * caster on Folia. Freezing it at cast time means a bow's {@code +Ranged} and a mage projectile's
 * {@code +Magic} are fixed at launch, exactly as {@code attackDamage} is, with no new threading rule
 * to remember. 0.0 when untracked -- a summand, like attack damage.
 *
 * Note what the FREEZE implies and the reconcile loop does not: swap weapons mid-flight and the
 * arrow already in the air keeps the bonus its launch-time class earned. That is the same rule the
 * attack-damage freeze established, and it is the correct one -- the shot was paid for when it was
 * taken.
 *
 * {@code enchantDamagePercent} is the caster's resolved ENCHANT-DAMAGE stat: the sum of the
 * percentages granted by the damage enchants active on the weapon they HOLD, whose class matches
 * that weapon's own. Fourth stat, same reason, same freeze -- a Sharpness III arrow is a 15% arrow
 * for its whole flight even if the bow is dropped before impact.
 *
 * It is a PERCENT rather than a multiplier, and 0.0 when untracked. That is not an oversight of the
 * summand/divisor rule but the reason the percent was chosen: {@code DamageEnchants.multiplier(0.0)}
 * is exactly 1.0, so the neutral absent value stays 0.0 and matches every other summand here. A
 * multiplier-valued component would have needed 1.0 -- a second convention beside
 * {@code attackSpeed}'s, on a field whose 0.0 default would silently zero all damage.
 *
 * {@code eyeHeight} is the one component here that does NOT follow the 0.0-is-neutral rule the
 * paragraphs above establish, and the compact constructor below rejects it rather than letting it
 * slide. It exists so melee target selection can trace a line of sight to a point on the target
 * instead of to the ground under it: {@code position} is the entity's FEET, so a sight ray ending
 * there hugs the floor for its whole final stretch and any lip, slab or step between caster and
 * target reads as a wall. Vanilla's own {@code LivingEntity.hasLineOfSight} traces eye to the
 * TARGET'S eye, and carrying the height here lets core do the same without knowing Bukkit exists --
 * self-correcting per mob size, so a chicken is not traced above its own head.
 *
 * A 0.0 here would not be neutral, it would silently restore exactly the eye-to-feet trace the
 * field was added to avoid, and nothing downstream would throw. That is the same shape of trap
 * {@code enchantDamagePercent} was made a percent to sidestep -- except that there the neutral
 * value genuinely was 0.0, and here there is no neutral value at all. So it fails loud instead.
 */
public record CombatantSnapshot(UUID id, Vec3 position, double eyeHeight, boolean alive,
                                boolean player, double attackSpeed, double attackDamage,
                                double classDamageBonus, double enchantDamagePercent) {

    public CombatantSnapshot {
        if (eyeHeight <= 0) {
            throw new IllegalArgumentException(
                    "eyeHeight must be > 0, was " + eyeHeight
                            + " -- 0.0 is not a neutral default here, it traces sight to the feet");
        }
    }
}
