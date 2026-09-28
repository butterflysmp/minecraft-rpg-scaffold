package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.OptionalDouble;

/**
 * F20, the vanilla mirror (PLAN-mob-scaling.md §7): what a tracked MOB's vanilla health is set to after
 * every write to its custom store.
 *
 * <pre>
 *   vanilla = clamp( newCurrent / customMax x vanillaMax ,  min(vanillaMax, LIVE_FLOOR) ,  vanillaMax )
 * </pre>
 *
 * <p><b>Why a mob's vanilla health must track the store at all.</b> Our own damage
 * ({@code BukkitCombatant.applyDamage}: every ability and weapon effect, scorch, Ignite, reflects)
 * raises no vanilla event, and the tokened riders move vanilla by 0.01. So vanilla health sat at or near
 * full, and every vanilla reader of it was wrong. The two heals slice 3 reroutes gate on
 * {@code getHealth() < getMaxHealth()} and never fired for a mob hurt only by our damage (F20). Once
 * fired, they never stopped (F18). The dragon's and the Wither's boss bars sat at full, and the Wither
 * never reached its half-health phase. <b>M26 (Ben, 2026-09-28): all of vanilla's health-keyed behaviour
 * comes back, keyed on this number.</b>
 *
 * <p><b>Four conditions, each pinned in {@code VanillaMirrorTest}:</b>
 * <ul>
 *   <li><b>Death is ours.</b> A store at or below 0 returns EMPTY: write nothing, and let
 *       {@code MobDeathSystem}'s {@code setHealth(0)} kill. A floor written here would revive the mob.
 *   <li><b>Floored above 0 while the store is above 0</b>, so a vanilla hit or a 0.01 token cannot kill a
 *       mob our store says is alive. {@link #LIVE_FLOOR} is also the token floor's value, and it lives
 *       here so both use one number.
 *   <li><b>Never above the vanilla max</b>, because {@code CraftLivingEntity.setHealth} throws above
 *       {@code getMaxHealth()} (read from the pinned jar), and a throw on the entity thread is the
 *       failure to rule out.
 *   <li><b>The attribute is never written (M9).</b> This returns a HEALTH; the caller calls
 *       {@code setHealth}, never the attribute.
 * </ul>
 */
public final class VanillaMirror {

    /** The lowest vanilla health a live mob is given: the token floor's value, now in one place. */
    public static final double LIVE_FLOOR = 1.0;

    private VanillaMirror() {}

    /**
     * The vanilla health to write, or EMPTY to write nothing.
     *
     * @param newCurrent the store's current health after the write. At or below 0 (or NaN) returns
     *                   EMPTY: death is ours.
     * @param customMax  the store's max. Non-positive or NaN returns EMPTY (fail soft).
     * @param vanillaMax the entity's own MAX_HEALTH attribute value. Non-positive or NaN returns EMPTY.
     */
    public static OptionalDouble healthFor(double newCurrent, double customMax, double vanillaMax) {
        // `!(x > 0)` is NaN-safe: every comparison with NaN is false.
        if (!(newCurrent > 0)) return OptionalDouble.empty();
        if (!(customMax > 0) || !(vanillaMax > 0)) return OptionalDouble.empty();
        double fraction = Math.min(1.0, newCurrent / customMax);
        double floor = Math.min(vanillaMax, LIVE_FLOOR);
        return OptionalDouble.of(Math.max(floor, fraction * vanillaMax));
    }

    /**
     * Whether a seam change is mirrored: every kind on a mob, because a HEAL must raise vanilla too (a
     * lowering-only mirror fixes F20 and leaves F18). Never a player: {@code HeartBarRenderer} owns
     * their bar. Never the killing change: {@code reachedZero} is {@code MobDeathSystem}'s.
     *
     * <p>The switch has no default arm, so a fourth {@link HealthChange.Kind} is a compile error here
     * rather than a silent skip. <i>(A {@code switch} used as an expression must cover every constant of
     * the enum; leaving out {@code default} is what makes the compiler check that.)</i>
     */
    public static boolean shouldMirror(HealthChange change) {
        if (change.targetIsPlayer() || change.reachedZero()) return false;
        return switch (change.kind()) {
            case DAMAGE, HEAL, MAX_CHANGE -> true;
        };
    }
}
