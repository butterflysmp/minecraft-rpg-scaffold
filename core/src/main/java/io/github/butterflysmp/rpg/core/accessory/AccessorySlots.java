package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;

import java.util.Locale;

/**
 * The shape of a player's accessory slots, and the one decision about who may put what where.
 *
 * <p>Four slots: index {@value #CLASS_SLOT} is the class slot, indices 1-3 are universal. Stored
 * zero-based and typed zero-based -- there is no 1-based surface to convert at.
 *
 * <h2>The class gate reads the PROFILE class (ruling A1), compared EXACTLY</h2>
 *
 * {@code /rpg class} accepts only a kit's class token and compares it case-sensitively
 * ({@code RpgCommand.chooseClass}); nothing lowercases a profile's class. So this compares against
 * the same lowercase token, exactly -- a looser comparison here would let the two disagree about what
 * "ranger" means. A null class (a profile file missing the key survives load, see
 * {@code PlayerProfile}'s compact constructor) and {@code "none"} both mean no class.
 */
public final class AccessorySlots {

    private AccessorySlots() {}

    /** How many accessory slots a player has: one class slot and three universal ones. */
    public static final int COUNT = 4;

    /** The class slot's index. */
    public static final int CLASS_SLOT = 0;

    /** The profile's class value for "no class chosen" -- {@code PlayerProfile.NONE}'s spelling. */
    public static final String NO_CLASS = "none";

    /** Refuse an index outside {@code 0..COUNT-1}; returns it for chaining. */
    public static int requireSlot(int slot) {
        if (slot < 0 || slot >= COUNT) {
            throw new IllegalArgumentException("accessory slot " + slot + " is out of range 0.."
                    + (COUNT - 1));
        }
        return slot;
    }

    /** Which kind of accessory a slot index holds. */
    public static AccessorySlotKind kindOf(int slot) {
        return requireSlot(slot) == CLASS_SLOT ? AccessorySlotKind.CLASS : AccessorySlotKind.UNIVERSAL;
    }

    /** The profile token for a class: {@code ranger}, as a kit authors it. */
    public static String classToken(WeaponClass weaponClass) {
        return weaponClass.name().toLowerCase(Locale.ROOT);
    }

    /** True when the profile has a chosen class at all. */
    public static boolean hasClass(String profileClass) {
        return profileClass != null && !profileClass.equals(NO_CLASS);
    }

    /**
     * Does a class accessory CONTRIBUTE for this profile right now? True only when the profile's
     * class is exactly the accessory's class token. A universal accessory always contributes.
     *
     * <p>This is what makes a worn class accessory go INERT on a class change rather than moving
     * (the §2 ruling): nothing is taken off, the next reconcile simply stops emitting it, and
     * changing back reactivates it with no action.
     */
    public static boolean contributes(AccessoryDefinition accessory, String profileClass) {
        if (accessory.slot() == AccessorySlotKind.UNIVERSAL) return true;
        return hasClass(profileClass)
                && profileClass.equals(classToken(accessory.accessoryClass()));
    }

    /** The answer to "may this accessory go into this slot?" -- one verdict, each refusal named. */
    public enum Verdict {
        OK,
        /** A class item into a universal slot, or a universal item into the class slot. */
        WRONG_SLOT_KIND,
        /** The class slot, and the profile has no class ("none" or null). The slot is LOCKED. */
        CLASS_SLOT_LOCKED,
        /** The class slot, and the profile's class is not the accessory's class. */
        WRONG_CLASS
    }

    /**
     * May {@code accessory} be equipped into {@code slot} by a player whose profile class is
     * {@code profileClass}? Equipping a class item requires the matching class; KEEPING one after a
     * class change does not -- that is {@link #contributes}' job, and the two differ on purpose.
     */
    public static Verdict canEquip(int slot, AccessoryDefinition accessory, String profileClass) {
        AccessorySlotKind kind = kindOf(slot);
        if (kind == AccessorySlotKind.CLASS && !hasClass(profileClass)) {
            return Verdict.CLASS_SLOT_LOCKED;
        }
        if (accessory.slot() != kind) return Verdict.WRONG_SLOT_KIND;
        if (kind == AccessorySlotKind.CLASS
                && !profileClass.equals(classToken(accessory.accessoryClass()))) {
            return Verdict.WRONG_CLASS;
        }
        return Verdict.OK;
    }
}
