package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.Shield;

import java.util.List;
import java.util.Optional;

/**
 * A shield, as authored in {@code content/shields/&lt;id&gt;.yml}.
 *
 * THE FIRST NON-WEAPON GEAR, and deliberately its own record rather than a reuse of
 * {@link WeaponDefinition}. That is not a stylistic call -- {@code WeaponDefinition}'s constructor
 * REJECTS an empty trigger list ("weapon '...' has no triggers"), and a shield has no triggers at
 * all. It also requires a {@link WeaponClass}, an axis a shield has no answer for. Bending either
 * to fit would mean inventing a fake trigger and a fake class for every shield ever authored.
 *
 * <p><b>This is expected to be generalised, and not yet.</b> When armor lands there will be three
 * shapes sharing id/displayName/rarity/material/flavor, and THAT is the moment to factor a
 * {@code GearDefinition} out of them -- with three call sites to check the abstraction against
 * rather than two. Doing it now would be designing the shared shape from a single example.
 *
 * <p>What it deliberately does NOT carry:
 *
 * <ul>
 *   <li><b>No element.</b> A weapon's element types the damage it deals; a shield deals none.
 *       Element-typed MITIGATION -- a shield that blocks fire better than kinetic -- is a real
 *       design someone might want, and it is a Slice 2+ decision, not a field to reserve blank now.
 *   <li><b>No class.</b> Nothing gates a shield by melee/ranger/mage in this slice. The enchant
 *       roll and the enchant inert-check are both keyed on {@link WeaponClass}, which is exactly
 *       why shield enchant-GATING is out of scope here: a shield ships enchant-compatible (it
 *       carries the container) but not enchant-rolled.
 *   <li><b>No triggers.</b> Blocking is not an ability; it is vanilla's own item behaviour that we
 *       ride.
 * </ul>
 */
public record ShieldDefinition(
        String id,
        String displayName,
        Rarity rarity,
        String material,
        double blockDr,
        List<String> flavor,
        Optional<String> craftResult
) implements GearDefinition {

    /**
     * The material a shield gets when its content file does not name one.
     *
     * {@code shield} rather than anything else because vanilla's shield item is what supplies the
     * block behaviour we ride -- the raise animation, the block sound, the 90-degree arc and the
     * frontal validity. A shield authored onto some other material would mint and render fine and
     * then never block anything, which is the quietest possible way to ship a broken item.
     */
    public static final String DEFAULT_MATERIAL = "shield";

    public ShieldDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("shield id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("shield '" + id + "' has a blank display_name");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("shield '" + id + "' has no rarity");
        }
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("shield '" + id + "' has a blank material");
        }
        // The range guard, stated as a REFUSAL rather than a clamp. Shield.clamp also bounds this
        // value, and that is not redundant: the clamp guards the ARITHMETIC against an item already
        // in someone's inventory, where no loader will ever run again. This guards the CONTENT, and
        // it refuses instead of silently correcting so a typo is a named skipped file in the boot
        // log rather than a shield that quietly blocks a different amount than its author wrote.
        //
        // Written as a negated range so NaN is caught too: every comparison against NaN is false,
        // so `blockDr < 0 || blockDr > 1` would wave it straight through.
        if (!(blockDr >= Shield.NONE && blockDr <= Shield.FULL)) {
            throw new IllegalArgumentException("shield '" + id + "' has block_dr " + blockDr
                    + "; it must be between 0 and 1 (0.5 means half the damage is stopped)");
        }
        flavor = flavor == null ? List.of() : List.copyOf(flavor);
        craftResult = CraftResultToken.normalise(craftResult, "shield", id);
    }

    /**
     * The shape without a craft-result claim, so existing callers and tests keep compiling. Most
     * gear makes no claim.
     */
    public ShieldDefinition(String id, String displayName, Rarity rarity, String material,
                            double blockDr, List<String> flavor) {
        this(id, displayName, rarity, material, blockDr, flavor, Optional.empty());
    }

    /** Does this shield block anything at all? Zero is legal and means it does not. */
    public boolean blocks() {
        return Shield.blocks(blockDr);
    }

    /** The fraction of a hit that gets through this shield when a block is valid. */
    public double passThrough() {
        return Shield.passThrough(blockDr);
    }
}
