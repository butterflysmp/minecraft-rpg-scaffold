package io.github.butterflysmp.rpg.core.mob;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * M16: every mob-to-player hit that is not melee (arrows, blasts, magic) is priced FLAT, as
 * vanilla x 5 x GS/100, the same as melee. It REPLACES {@code DamageScale}'s share-of-player-max
 * conversion for mob-dealt damage and never multiplies on top of it. PLAN-mob-scaling.md §1.2 shows why
 * stacking would be wrong: the old conversion is already x5 at max 100, so stacking would make an
 * arrow x25.
 *
 * <p><b>There is no player max anywhere in this class, and its absence is the rule.</b>
 * {@code MobDamagePricingTest.noPublicMethodCanTakeAPlayerMax} pins it.
 *
 * <p>Two steps, kept apart so each is testable on its own:
 * <ol>
 *   <li>{@link #resolve}: WHERE the gear score comes from. First the direct entity's stamp (a
 *       projectile carries its shooter's score, written at launch), then the causing mob, and
 *       otherwise ABSENT.
 *   <li>{@link #price}: HOW MUCH, which is {@link MobScaling#attackDamage} itself, not a second copy of
 *       it.
 * </ol>
 *
 * <p>(A {@code record} is a small immutable data class: Java generates the constructor, the getters
 * and {@code equals} from the component list.)
 */
public final class MobDamagePricing {

    private MobDamagePricing() {}

    /**
     * What one entity in a damage source says about the mob behind it.
     *
     * <p>For a living mob these are its own facts. For a stamped projectile they are the facts its
     * shooter had at launch. {@code gearScore} is the raw stored value, which may be invalid; that is
     * why {@link #resolve} checks it and does not trust it.
     */
    public record MobFacts(boolean custom, boolean hostile, OptionalInt gearScore) {}

    /** Which entity supplied the score. ABSENT is a hostile source with no trustworthy score. */
    public enum From { DIRECT, CAUSING, ABSENT }

    /**
     * The resolved source. {@code gearScore} is meaningful only when {@code hostile}; ABSENT carries
     * {@link MobScaling#GS_BASELINE}, so an unresolved score costs the GS factor and nothing else.
     */
    public record Resolved(From from, boolean custom, boolean hostile, int gearScore) {}

    /**
     * Where this hit's gear score comes from, or empty when the hit is not mob-sourced at all (a fall,
     * lava, or a damage-over-time tick that names no entity, §6 F5). An empty result keeps
     * {@code DamageScale}.
     *
     * @param direct         the direct entity's facts (a living mob, or a stamped projectile), or null
     * @param causing        the causing entity's facts when it is a living non-player mob, or null
     * @param causedByPlayer the causing entity is a player. Then the hit is never mob-sourced, even
     *                       through a stamped projectile (a batted-back ghast fireball)
     */
    public static Optional<Resolved> resolve(MobFacts direct, MobFacts causing, boolean causedByPlayer) {
        if (causedByPlayer) return Optional.empty();
        // 1. The stamp. It is first because it is the score the shot was FIRED at: the shooter may have
        //    died (G10), or been re-rolled, before impact.
        if (direct != null && direct.hostile() && isValid(direct.gearScore())) {
            return Optional.of(new Resolved(From.DIRECT, direct.custom(), true, direct.gearScore().getAsInt()));
        }
        // 2. The causing mob, and then 3. a direct mob that names no causing one.
        for (MobFacts mob : new MobFacts[] {causing, direct}) {
            if (mob == null) continue;
            From from = mob == causing ? From.CAUSING : From.DIRECT;
            if (!mob.hostile()) return Optional.of(new Resolved(from, mob.custom(), false, 0));   // M7: no GS, decided
            if (isValid(mob.gearScore())) {
                return Optional.of(new Resolved(from, mob.custom(), true, mob.gearScore().getAsInt()));
            }
            // A hostile source with no score the seed would trust. It is priced at the baseline so it
            // still gets x5, and the caller WARNs: an unresolved score is visible, never silently x1.
            return Optional.of(new Resolved(From.ABSENT, mob.custom(), true, MobScaling.GS_BASELINE));
        }
        return Optional.empty();
    }

    /**
     * The flat M16 price of a mob-sourced hit: {@link MobScaling#attackDamage} of the vanilla amount.
     * A non-positive or NaN amount deals nothing.
     */
    public static double price(double vanillaAmount, Resolved source) {
        if (!(vanillaAmount > 0)) return 0.0;
        return MobScaling.attackDamage(vanillaAmount, source.custom(), source.hostile(), source.gearScore());
    }

    private static boolean isValid(OptionalInt gs) {
        return gs.isPresent() && MobGearScore.isValidStored(gs.getAsInt());
    }
}
