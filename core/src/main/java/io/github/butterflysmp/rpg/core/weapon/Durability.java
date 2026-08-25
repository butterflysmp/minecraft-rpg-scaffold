package io.github.butterflysmp.rpg.core.weapon;

/**
 * The durability arithmetic for a weapon that MUST NEVER BREAK.
 *
 * An RPG weapon is earned, so losing one to a durability bar feels cheap. Instead of shattering at
 * 0, a weapon floors at {@link #MIN_USES} and goes inert until it is repaired -- so the item always
 * survives, and "broken" is a state the gate reads rather than an item that is gone.
 *
 * This rides VANILLA durability rather than adding a stat: {@code maxDurability} is the material's
 * own maximum (iron_sword 250, bow 384), and the damage value is the one the item already carries.
 * Paper owns that I/O; this owns the decisions, which is the half worth testing. Same split as
 * {@link RefreshVerdict} and {@link ClassDamageModifiers}: an {@code ItemStack} cannot be built
 * without a running server, so anything expressed as arithmetic here is anything that does not have
 * to wait for a boot to be checked.
 *
 * <p><b>A non-damageable material ({@code maxDurability <= 0}) is handled HERE, in every entry
 * point, not by convention at each call site.</b> {@code ember_staff} (blaze_rod) and
 * {@code ability_stone} (amethyst_shard) have no vanilla durability, and they are exactly the two
 * weapons that should not have any -- so the exemption is free, and making it structural is what
 * stops a future caller forgetting it. Without the guard {@code maxDurability - MIN_USES} is
 * {@code -1}, and a staff would both read as broken and be written a negative damage value.
 */
public final class Durability {

    private Durability() {}

    /**
     * The uses a weapon always keeps. Durability floors here instead of reaching the maximum, and
     * reaching the maximum is precisely what destroys a vanilla item -- so this constant IS the
     * no-break promise. At the floor the weapon is {@link #isBroken}: present, repairable, inert.
     */
    public static final int MIN_USES = 1;

    /**
     * The damage value actually safe to write, for a proposed one.
     *
     * Two guards, each load-bearing:
     *
     *  - the upper clamp to {@code maxDurability - MIN_USES} is the never-destroys floor. Writing
     *    {@code maxDurability} itself is a BROKEN item in the vanilla sense -- gone, not inert.
     *  - the lower clamp to 0 stops a negative damage value, which is not "extra durability" but a
     *    malformed item.
     *
     * Also the clamp {@code WeaponItems.carryWear} applies across a re-mint: copying a raw damage
     * value onto a lower-maximum material (iron 250 to gold 32) would otherwise land past the new
     * maximum, and a display refresh would destroy the weapon it was refreshing.
     *
     * Worked: {@code (9999, 250) -> 249}; {@code (0, 250) -> 0}; {@code (-5, 250) -> 0};
     * {@code (50, 32) -> 31} (the re-mint case); {@code (50, 0) -> 0} (not damageable).
     */
    public static int clamp(int proposedDamage, int maxDurability) {
        if (maxDurability <= 0) return 0;
        return Math.min(Math.max(proposedDamage, 0), maxDurability - MIN_USES);
    }

    /**
     * Is this weapon spent -- at the floor, with its last use gone?
     *
     * {@code >=} rather than {@code >} deliberately: the floor is the broken state, not the last
     * usable point. Off by one here and a weapon at 0 uses still swings.
     *
     * A non-damageable material is NEVER broken, whatever damage value it somehow carries. That is
     * the staff-and-stone exemption, and it is why this cannot be written as a bare comparison.
     */
    public static boolean isBroken(int currentDamage, int maxDurability) {
        if (maxDurability <= 0) return false;
        return currentDamage >= maxDurability - MIN_USES;
    }

    /**
     * Wear a weapon by {@code amount}, floored so it can never be destroyed.
     *
     * Delegates its clamping -- and therefore its non-damageable guard -- to {@link #clamp}, so the
     * floor lives in exactly one place.
     *
     * The widening to {@code long} before clamping is not decoration. {@code currentDamage + amount}
     * in {@code int} overflows to a NEGATIVE for a large enough amount, which clamps to 0 -- so a
     * huge wear would silently become a FULL REPAIR. A debuff looping around into the strongest
     * possible buff is the same failure {@code AttackSpeed.MIN_SPEED} exists to prevent.
     *
     * Worked: {@code (240, 100, 250) -> 249}; {@code (0, 10, 250) -> 10};
     * {@code (200, Integer.MAX_VALUE, 250) -> 249} (no wraparound); {@code (5, 10, 0) -> 0}.
     */
    public static int wear(int currentDamage, int amount, int maxDurability) {
        long proposed = (long) currentDamage + (long) amount;
        return clamp((int) Math.max(Integer.MIN_VALUE, Math.min(proposed, Integer.MAX_VALUE)),
                maxDurability);
    }

    /**
     * Repair a weapon by {@code amount}. Floors at 0, which is a fully repaired item; an amount at
     * or beyond the current damage is a full repair rather than a negative.
     *
     * Takes no {@code maxDurability}: repairing only ever moves damage DOWN, so no maximum can be
     * exceeded and the non-damageable case needs no guard -- 0 repairs to 0.
     *
     * Worked: {@code (100, 40) -> 60}; {@code (10, 999) -> 0}; {@code (0, 5) -> 0}.
     */
    public static int repair(int currentDamage, int amount) {
        return Math.max(currentDamage - amount, 0);
    }
}
