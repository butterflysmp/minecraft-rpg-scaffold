package io.github.butterflysmp.rpg.core.weapon;

import java.util.List;
import java.util.Optional;

/**
 * What every piece of authored gear has, whatever kind it is: an id, a name, a rarity, a material
 * and some flavour.
 *
 * <h2>Designed from three examples, deliberately, and not from two</h2>
 *
 * This interface is the thing {@link ShieldDefinition} and {@code NEXT.md} both promised and both
 * declined to write early. The reasoning they recorded was that designing a common gear type from
 * two examples is designing it from one and a half; armor is the third shape, and it is what let
 * this be CHECKED rather than guessed. The check paid for itself: the five members below are exactly
 * the intersection of the three records, and every candidate sixth member failed against one of them
 * --
 *
 * <ul>
 *   <li><b>Not a stat.</b> A weapon has attack damage, a shield has block DR, a piece of armor has
 *       defense. Three different numbers meaning three different things, and a shield's is a
 *       FRACTION where the other two are absolute. There is no honest {@code statValue()}.
 *   <li><b>Not a class.</b> {@link WeaponClass} is required on a weapon, absent on a shield, and
 *       absent on armor. {@link GearClass} is the enchant-gating axis and armor has no constant yet.
 *   <li><b>Not durability.</b> Weapons and shields own their wear; armor's is vanilla's.
 *   <li><b>Not a lore builder.</b> Each needs different inputs -- a weapon needs the element
 *       registry, a shield needs its Bulwark percent, armor needs nothing -- and they return
 *       Adventure Components, which cannot exist in {@code core} at all.
 * </ul>
 *
 * <p>So this is deliberately a thin IDENTITY interface. It is what a caller needs to say "mint this,
 * name it, colour it by tier and carry its tag", which is the whole of what the three mint paths had
 * in common, and nothing more. Everything with real per-kind logic in it stays on the record.
 *
 * <h2>The sixth member, and why it passed the test the other four failed</h2>
 *
 * {@link #craftResult()} was added by the mint-on-craft slice, and it is the first member admitted
 * since the three-example design above. It is here rather than on the three records because the
 * reverse index that reads it walks all four kinds as one collection, and a member the interface
 * does not declare cannot be read that way.
 *
 * <p>It passes each of the four tests the rejected candidates failed. It is not a stat -- it carries
 * no number and means the same thing for every kind. It is not a class -- it is absent on most gear
 * by design rather than absent on some KINDS. It is not durability -- it says nothing about wear.
 * And it is a plain {@code String} token, so unlike a lore builder it can exist in {@code core} at
 * all.
 *
 * <p><b>It is an OPT-IN identity claim, and deliberately not {@link #material()}.</b> A material is
 * PRESENTATION: {@code WeaponDefinition.DEFAULT_MATERIAL} is {@code iron_sword} and every
 * sword-shaped weapon leaves it there, so materials are contested by design and say nothing about
 * which weapon a crafted sword should become. A craft result is a claim, made once, by one
 * definition.
 *
 * <h2>Sealed, so a fourth kind is a compile error at every switch</h2>
 *
 * The same compiler-guided discipline {@link Rarity}/{@code RarityColors} and {@link GearClass}/
 * {@code GearClassLabel} already use. A future gear kind -- a trinket, a mount -- has to be admitted
 * here explicitly, and every exhaustive switch over gear then stops compiling until it is handled,
 * rather than falling through some default arm.
 */
public sealed interface GearDefinition
        permits WeaponDefinition, ShieldDefinition, ArmorDefinition, ToolDefinition {

    /** The content id, unique across ALL FOUR registries -- the boot warns when it is not. */
    String id();

    /** The authored name, before {@code WeaponItems.displayName} recolours it by rarity. */
    String displayName();

    /** The tier. Drives the item name's colour and the last line of the tooltip. */
    Rarity rarity();

    /** The Bukkit material token this mints onto. Each kind falls back differently on a miss. */
    String material();

    /** Authored flavour lines, already defensively copied by the record. Never null; may be empty. */
    List<String> flavor();

    /**
     * The vanilla craft result that mints this definition, if it opts in. Empty for gear that does
     * not participate in mint-on-craft, which is most of it.
     *
     * <p><b>An identity claim, not a presentation choice.</b> Crafting this vanilla item hands the
     * player THIS definition's minted item instead of vanilla's. Read only by
     * {@link CraftResultIndex}.
     *
     * <p>Boot REFUSES a claim that does not equal {@link #material()}, because the mint builds the
     * item from {@code material()} and not from what was crafted -- so a mismatched pair would let a
     * player craft iron and receive diamond. It also refuses a claim on a material with no
     * durability, which would otherwise index cleanly and then never mint, silently.
     *
     * <p>For ARMOR this is per PIECE, not per tier file: one {@code iron.yml} yields four
     * definitions and each claims its own slot's item.
     *
     * @return the material token, never blank when present. Never null.
     */
    Optional<String> craftResult();
}
