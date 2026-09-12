package io.github.butterflysmp.rpg.core.combat;

import java.util.OptionalInt;

/**
 * The numbers a {@code /rpg stats} sheet is built from, each named at the call site.
 *
 * <h2>WHY THIS EXISTS: EIGHT ADJACENT DOUBLES WERE ABOUT TO BECOME TEN</h2>
 *
 * <p>{@code StatsSheet.build} took eight positional {@code double}s. Quiver size and reload time
 * would have made it ten, and the A2 plan flagged that in advance as <b>a real design decision
 * hiding inside a mechanical slice</b> rather than letting it arrive as a mechanical edit.
 *
 * <p>It is the exact shape {@code DamageSignatureTest} exists to forbid and the one
 * {@code QuiverState}'s javadoc argues against: same-typed parameters, adjacent, transposable, and
 * <b>every ordering compiling</b>. At eight it was already the worst case in this codebase; at ten
 * a swapped pair is a wrong number on a player's screen with nothing red anywhere.
 *
 * <h2>A RECORD ALONE WOULD NOT HAVE FIXED IT, WHICH IS WHY THERE IS A BUILDER</h2>
 *
 * <p>The obvious "parameter object" is a ten-component record -- and <b>a record's canonical
 * constructor is positional too.</b> {@code new Values(a, b, c, d, e, f, g, h, i, j)} is exactly as
 * transposable as the argument list it replaced; it would have moved the hazard, named it, and
 * changed nothing. {@code QuiverState} could solve its version with distinct component types; ten
 * doubles offer no such handle.
 *
 * <p>So construction goes through {@link Builder}, where <b>every value is named at the call site</b>
 * and a transposition is a compile error rather than a rendering one. The canonical constructor is
 * PRIVATE, on {@code QuiverState}'s precedent -- a door left open is a door somebody uses.
 *
 * <h2>The quiver pair is OPTIONAL, and set TOGETHER</h2>
 *
 * <p>Every other line on the sheet is a player stat that exists whatever they are holding. A quiver
 * capacity is not: with no quiver weapon in hand it is a number about a weapon they do not have. So
 * the two lines are absent unless {@link Builder#quiver(int, int)} is called -- and that one call
 * takes BOTH, because a sheet showing a capacity with no reload (or the reverse) is a state nothing
 * should be able to express.
 */
public final class StatsSheetValues {

    private final double maxHealth;
    private final double healthRegenPerSecond;
    private final double maxMana;
    private final double manaRegenPerSecond;
    private final double defense;
    private final double damage;
    private final double critChance;
    private final double critDamageBonus;
    private final OptionalInt quiverSize;
    private final OptionalInt reloadTicks;

    private StatsSheetValues(Builder b) {
        this.maxHealth = b.maxHealth;
        this.healthRegenPerSecond = b.healthRegenPerSecond;
        this.maxMana = b.maxMana;
        this.manaRegenPerSecond = b.manaRegenPerSecond;
        this.defense = b.defense;
        this.damage = b.damage;
        this.critChance = b.critChance;
        this.critDamageBonus = b.critDamageBonus;
        this.quiverSize = b.quiverSize;
        this.reloadTicks = b.reloadTicks;
    }

    /** A fresh builder. Every value defaults to 0; the quiver pair defaults to absent. */
    public static Builder builder() {
        return new Builder();
    }

    public double maxHealth() { return maxHealth; }

    /** ALREADY per second. {@code HealthState} stores it that way; nothing converts here. */
    public double healthRegenPerSecond() { return healthRegenPerSecond; }

    public double maxMana() { return maxMana; }

    /**
     * ALREADY per second -- and this is the one that has a conversion behind it.
     *
     * <p>{@code ResourcePool.regen} returns per TICK. The caller puts it through
     * {@code ManaRegen.perSecond}, which is the single home for that conversion. Passing a per-tick
     * value would silently report a rate twenty times too small, and the builder method's name is the
     * only thing that says so at the call site -- which is most of why the builder exists.
     */
    public double manaRegenPerSecond() { return manaRegenPerSecond; }

    public double defense() { return defense; }

    /** The COMPOSED hit, not the raw attack value: {@code HitDamage.hitBase(...)}. */
    public double damage() { return damage; }

    /** A probability in {@code [0,1]}, not a percent. {@code StatsSheetLines.critChance} converts. */
    public double critChance() { return critChance; }

    /** A BONUS, not a multiplier. {@code StatsSheetLines.critDamage} adds the {@code 1 +}. */
    public double critDamageBonus() { return critDamageBonus; }

    /**
     * The RESOLVED capacity of the held quiver weapon, or empty when none is held.
     *
     * <p><b>"resolved" is in the NAME, not only the javadoc, and a guard is why.</b> These were
     * {@code quiverSize()} and {@code reloadTicks()} -- the same names {@code WeaponDefinition} uses
     * for the AUTHORED values. The reload-supply scan in {@code QuiversSignatureTest} looks for the
     * bare accessor name (deliberately: a qualified needle is what a static import walks past) and it
     * put this file on the supply list on its first run. The collision was real for a READER too, not
     * only for a scan: one name, two quantities, one file apart -- authored versus resolved, which is
     * the distinction this whole slice is organised around.
     */
    public OptionalInt resolvedQuiverSize() { return quiverSize; }

    /** The RESOLVED reload duration in ticks, or empty when no quiver weapon is held. */
    public OptionalInt resolvedReloadTicks() { return reloadTicks; }

    /** True when the quiver pair is present, so the sheet's two extra lines are rendered. */
    public boolean hasQuiver() { return quiverSize.isPresent(); }

    /**
     * Names every value at its call site. Each setter returns {@code this}.
     *
     * <p>There is no setter that takes two same-typed values except {@link #quiver(int, int)}, whose
     * two are an {@code int} pair that must move together -- and whose values are 28 and 48 in the
     * shipped instrument staging, so a transposition is visible rather than plausible.
     */
    public static final class Builder {

        private double maxHealth;
        private double healthRegenPerSecond;
        private double maxMana;
        private double manaRegenPerSecond;
        private double defense;
        private double damage;
        private double critChance;
        private double critDamageBonus;
        private OptionalInt quiverSize = OptionalInt.empty();
        private OptionalInt reloadTicks = OptionalInt.empty();

        private Builder() {}

        public Builder maxHealth(double value) { this.maxHealth = value; return this; }

        /** Pass a rate ALREADY in per-second. */
        public Builder healthRegenPerSecond(double value) {
            this.healthRegenPerSecond = value;
            return this;
        }

        public Builder maxMana(double value) { this.maxMana = value; return this; }

        /** Pass a rate ALREADY in per-second -- {@code ManaRegen.perSecond(pool.regen(...))}. */
        public Builder manaRegenPerSecond(double value) {
            this.manaRegenPerSecond = value;
            return this;
        }

        public Builder defense(double value) { this.defense = value; return this; }

        /** The COMPOSED hit, not the raw attack value. */
        public Builder damage(double value) { this.damage = value; return this; }

        /** A probability in {@code [0,1]}. */
        public Builder critChance(double value) { this.critChance = value; return this; }

        /** A BONUS: 1.0 means double damage. */
        public Builder critDamageBonus(double value) { this.critDamageBonus = value; return this; }

        /**
         * Both quiver numbers, RESOLVED, in one call -- or neither.
         *
         * <p>One call rather than two setters because a sheet showing a capacity with no reload is a
         * state nothing should be able to express. Absence is the default and means "no quiver weapon
         * held", which is the honest answer rather than a zero.
         *
         * @param capacity    resolved rounds, {@code QuiverSize.resolve}
         * @param reloadTicks resolved ticks, {@code ReloadTime.resolve}
         */
        public Builder quiver(int capacity, int reloadTicks) {
            this.quiverSize = OptionalInt.of(capacity);
            this.reloadTicks = OptionalInt.of(reloadTicks);
            return this;
        }

        public StatsSheetValues build() {
            return new StatsSheetValues(this);
        }
    }
}
