package io.github.butterflysmp.rpg.core.mob;

/**
 * M24: MOB DAMAGE IGNORES DIFFICULTY. Every mob-to-player hit is exactly vanilla x 5 x GS/100 (a custom
 * mob: authored x GS/100), whatever the server's difficulty. The gear score is the difficulty dial.
 *
 * <p>Vanilla lets difficulty into a mob's hit on a player through three channels that change the
 * AMOUNT. Each is read from the pinned {@code paper-26.1.2.jar} with {@code javap}, and each is moved
 * here to its value at <b>NORMAL, the one reference difficulty</b> ("vanilla" in M24 means vanilla at
 * NORMAL). NORMAL itself is untouched by all three:
 *
 * <pre>
 *   channel                                   where                                    undone by
 *   the player's own scaling, every source    Player.hurtServer, before the Bukkit      undifficulted()
 *     with scalesWithDifficulty():              event: EASY min(a/2+1, a), HARD a*3/2
 *   a guardian's beam                         Guardian$GuardianAttackGoal.tick:          undifficulted(.., guardianBeam)
 *                                               1, +2 on HARD, +2 for an elder
 *   a skeleton's or illusioner's arrow        AbstractArrow.setBaseDamageFromMob:         mobArrowBaseDamage(), at launch
 *                                               v*2 + triangle(id * 0.11, 0.57425)
 * </pre>
 *
 * <p><b>Melee needs none of this</b>: it is priced from the seeded {@code ATTACK_DAMAGE} attribute,
 * which difficulty never touches (F6). <b>What this cannot undo</b> (PLAN §6 F6, closed by M24):
 * PEACEFUL, where vanilla drops a scaled hit before any event exists; how OFTEN a mob hits (inaccuracy,
 * the Wither's extra skulls); effect DURATIONS (wither, poison), whose ticks name no entity (F5); and
 * equipment rolled from local difficulty.
 *
 * <p>(An {@code enum} with a field, like {@link Difficulty}, is a fixed set of named constants that
 * each carry a value.)
 */
public final class VanillaDifficulty {

    private VanillaDifficulty() {}

    /** Vanilla's four difficulties, with vanilla's ids (Difficulty's static initialiser). */
    public enum Difficulty {
        PEACEFUL(0), EASY(1), NORMAL(2), HARD(3);

        private final int id;

        Difficulty(int id) { this.id = id; }

        public int id() { return id; }
    }

    /** {@code setBaseDamageFromMob}'s shift of the triangle's mode, per difficulty level. */
    public static final double MOB_ARROW_SHIFT_PER_LEVEL = 0.11;

    /** The guardian beam's HARD-only bonus. The elder's own +2 is not difficulty, and it stays. */
    public static final double GUARDIAN_HARD_BEAM_BONUS = 2.0;

    /**
     * The amount of a mob's hit on a player as vanilla deals it at NORMAL, which is M24's "vanilla" on
     * every path. NORMAL is the fixed point of all three channels.
     *
     * <p>It runs on the RAW event amount, before the damage window, because the EASY reversal is affine
     * ({@code 2(x - 1)}), not a scale: applied to a window's partial amount it would be wrong.
     *
     * @param eventAmount          the event's {@code getDamage()}
     * @param difficulty           the victim's world difficulty when the hit landed
     * @param scalesWithDifficulty the source's {@code scalesWithDifficulty()}. When false, vanilla never
     *                             scaled it, and it is returned untouched
     * @param guardianBeam         the hit is a guardian's beam (MAGIC, direct and causing the guardian)
     */
    public static double undifficulted(double eventAmount, Difficulty difficulty,
                                       boolean scalesWithDifficulty, boolean guardianBeam) {
        double amount = scalesWithDifficulty ? unscale(eventAmount, difficulty) : eventAmount;
        if (guardianBeam && difficulty == Difficulty.HARD) amount -= GUARDIAN_HARD_BEAM_BONUS;
        return amount;
    }

    /**
     * A mob arrow's base damage moved to NORMAL's: NORMAL is untouched, and EASY and HARD move to it by
     * one 0.11 step per level. Applied at LAUNCH, because the hit is {@code ceil(speed x base)}, and a
     * ceiling cannot be reversed at impact.
     *
     * <p><b>Measured from NORMAL, not from zero.</b> The first version subtracted {@code id x 0.11}, which
     * also removed NORMAL's own 0.22, so mob arrows normalised to PEACEFUL while every other channel
     * normalised to NORMAL (the seat's review of {@code 0f8fd81}).
     */
    public static double mobArrowBaseDamage(double vanillaBase, Difficulty difficulty) {
        return vanillaBase - (difficulty.id() - Difficulty.NORMAL.id()) * MOB_ARROW_SHIFT_PER_LEVEL;
    }

    /**
     * The inverse of {@code Player.hurtServer}'s scaling. EASY is {@code min(a/2 + 1, a)}: the identity
     * below 2 and {@code a/2 + 1} from 2 up. Both halves increase and meet at 2, so it inverts exactly:
     * below 2 unchanged, else {@code 2(x - 1)}. HARD is {@code a x 3/2}. PEACEFUL is never reached with
     * a scaled source, because vanilla returns before raising the event.
     */
    private static double unscale(double x, Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> x < 2.0 ? x : 2.0 * (x - 1.0);
            case HARD -> x * 2.0 / 3.0;
            case NORMAL, PEACEFUL -> x;
        };
    }
}
