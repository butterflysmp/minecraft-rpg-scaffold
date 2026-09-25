package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearClass;

/**
 * The equivalence the anvil transfers ACROSS: two items may trade a gear score when their keys are
 * EQUAL, and never otherwise.
 *
 * <p>Ben's rule, in his words: <i>"same type only, melee to melee, ranged to ranged, helmets to
 * helmets, shield to shield, accessory to accessory"</i>.
 *
 * <h2>*** IT IS FINER THAN {@link GearClass}, AND THAT IS THE ONLY REASON IT EXISTS ***</h2>
 *
 * {@code GearClass.ARMOR} is <b>ONE constant, not four</b> -- deliberately, because every armor
 * enchant is armor-wide and nothing gates on WHICH piece. So {@code GearClass} alone <b>cannot say
 * helmets-to-helmets</b>: it would let a pair of boots hand its score to a helmet. This axis splits
 * that one constant into {@link #ARMOR_HEAD}, {@link #ARMOR_CHEST}, {@link #ARMOR_LEGS} and
 * {@link #ARMOR_FEET} by reading {@link ArmorSlot}, which is the enum that already distinguishes
 * four because the Defense scan keys its reconciler map by slot.
 *
 * <p><b>It is a THIRD axis, not a widening of either of the two.</b> {@code GearClass} answers
 * "what may this enchant sit on"; {@code ArmorSlot} answers "which limb does this cover"; this
 * answers "what may this trade scores with". Merging any two of them would force one question's
 * answer through another question's switch.
 *
 * <h2>ACCESSORIES TRADE WITH NOTHING, AND {@link #of} IS WHERE THAT WAS DECIDED</h2>
 *
 * {@code GearClass.ACCESSORY} landed with the accessories slice. <b>{@link #of} is an exhaustive
 * switch with NO default arm</b>, so its arrival was a COMPILE ERROR here rather than a new kind
 * quietly falling into a catch-all and matching everything -- and the answer given is ruling A4: an
 * accessory carries no gear score, so it has no key and {@link #of} throws for it, as for a tool.
 * Same discipline as {@code GearScore.scoreable} and {@code GearClass.of}, and for the same reason.
 *
 * @see AnvilTransfer for the comparison itself
 */
public enum TransferKey {

    MELEE("melee weapons"),
    RANGER("ranged weapons"),
    MAGE("magic weapons"),

    /** A shield. It has no {@code WeaponClass} and never will, so it presents itself directly. */
    SHIELD("shields"),

    ARMOR_HEAD("helmets"),
    ARMOR_CHEST("chestplates"),
    ARMOR_LEGS("leggings"),
    ARMOR_FEET("boots");

    private final String plural;

    TransferKey(String plural) {
        this.plural = plural;
    }

    /**
     * This kind of gear, named in the plural, for the sentence a mismatch shows the player.
     *
     * <h2>PLURAL BECAUSE THE SINGULAR DOES NOT SURVIVE ALL EIGHT</h2>
     *
     * The refusal reads <i>"Both items must be helmets."</i> A singular form would have to produce
     * <i>"A boots can only take a boots' score"</i>, because {@code boots} and {@code leggings} are
     * already plural and {@link #ARMOR_FEET} has no singular anyone says out loud. <b>One form that
     * works for eight beats seven that read well and one that does not</b> -- and the sentence is an
     * INSTRUCTION rather than a complaint, which is Ben's rule for every refusal on this screen.
     */
    public String plural() {
        return plural;
    }

    /**
     * The key an item of this kind and slot presents.
     *
     * <h2>*** EVERY ARM THAT THROWS IS AN ASSERTION ABOUT A BROKEN INVARIANT, NOT ERROR HANDLING ***</h2>
     *
     * <b>Eligibility is decided BEFORE a key is derived.</b> {@code GearScore.carriesScore} refuses
     * a tool, refuses an item that is none of ours, and refuses an instance whose own definition
     * declares it unscored; only what survives all three becomes a {@code Side.Scored} and reaches
     * this method. So the three refusals below cannot be produced by any shipped path, and reaching
     * one means <b>that ordering was violated</b>.
     *
     * <h2>*** WHY A THROW AND NOT A NULL. BEN RULED IT, AND IT IS NOT STYLE. ***</h2>
     *
     * <b>A null key makes two tools compare EQUAL.</b> Any {@code Objects.equals}-shaped match --
     * and the transfer rule IS such a match -- reads two absent keys as one satisfied condition, so
     * tool-to-tool transfer becomes LEGAL by the exact mechanism the rule exists to forbid. That is
     * the <i>absent read as present</i> family this project has already been bitten by twice.
     * <b>{@code Optional} has the identical hole</b>: {@code empty().equals(empty())} is {@code true}.
     *
     * <p><b>And the two options fail in opposite directions, which is what decides it:</b>
     *
     * <pre>
     *   throw   the screen does not open.      Visible, harmless, points at the defect.
     *   null    the screen offers a transfer that should be impossible -- and in 13b the
     *           confirm CONSUMES THE DONOR.
     * </pre>
     *
     * <b>A wrong transfer is destructive; a broken screen is not.</b>
     *
     * <h2>THESE ARMS ARE NOT DEAD GUARDS, AND THE DIFFERENCE IS THAT THEY FIRE</h2>
     *
     * A guard with no instances is not automatically a guard that cannot fire -- {@code CLAUDE.md}'s
     * own rule -- so {@code TransferKeyTest} feeds each of the three the bad input on purpose and
     * watches it throw. The ORDERING is separately proven by {@code AnvilTransferTest}, which
     * asserts that an unscoreable pair returns {@code AnvilVerdict.NotScoreable} and <b>not</b>
     * {@code AnvilVerdict.KeyMismatch}: both refuse, only one is right, and a row that accepts
     * either is a row that cannot fail for its stated reason.
     *
     * @param kind the gear kind. Never {@code null} here -- an item that is none of ours is
     *             {@code Side.Unscoreable} and never reaches this method.
     * @param slot which limb, read only when {@code kind} is {@link GearClass#ARMOR}. Ignored, and
     *             permitted to be {@code null}, for every other kind.
     * @throws IllegalArgumentException if the eligibility ordering above was violated.
     */
    public static TransferKey of(GearClass kind, ArmorSlot slot) {
        if (kind == null) {
            throw new IllegalArgumentException(
                    "no gear kind: an item that is none of ours is Unscoreable and must never reach"
                            + " a key. carriesScore refuses it before this is called.");
        }
        return switch (kind) {
            case MELEE -> MELEE;
            case RANGER -> RANGER;
            case MAGE -> MAGE;
            case SHIELD -> SHIELD;
            case ARMOR -> ofArmor(slot);
            case TOOL -> throw new IllegalArgumentException(
                    "a tool carries no gear score, so GearScore.carriesScore refused it before a key"
                            + " was derived; reaching here means that ordering was violated");
            case ACCESSORY -> throw new IllegalArgumentException(
                    "an accessory carries no gear score (ruling A4), so GearScore.carriesScore"
                            + " refused it before a key was derived; reaching here means that"
                            + " ordering was violated");
        };
    }

    /**
     * The key for one armour slot.
     *
     * <p>A second exhaustive switch with no default arm, so a fifth {@link ArmorSlot} -- which would
     * be a Minecraft event rather than a content edit -- stops the build here too.
     *
     * <p><b>The four arms are written out rather than derived from the slot's own name.</b> A
     * derivation would make the four keys agree with {@link ArmorSlot} BY CONSTRUCTION, and then a
     * mutation that collapses them would have nothing to disagree with. {@code MUT13A-KEYARMOR}
     * collapses all four to {@link #ARMOR_HEAD} precisely to prove they are distinguished.
     */
    private static TransferKey ofArmor(ArmorSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException(
                    "armor with no slot: every ArmorDefinition carries one, so a null here is a"
                            + " programming error rather than bad data");
        }
        return switch (slot) {
            case HEAD -> ARMOR_HEAD;
            case CHEST -> ARMOR_CHEST;
            case LEGS -> ARMOR_LEGS;
            case FEET -> ARMOR_FEET;
        };
    }
}
