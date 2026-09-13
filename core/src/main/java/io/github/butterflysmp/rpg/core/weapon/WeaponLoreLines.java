package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The plain-text half of the weapon tooltip: pure String/number formatters over the content model.
 * No Adventure, no Bukkit -- lives in core so it runs in the 2-second test loop, and the paper
 * {@code WeaponLore} builder only wraps these in colour/layout with the class label it owns.
 *
 * Every damage number the tooltip shows comes from the {@link WeaponDefinition} statically -- the
 * declared {@code attackDamage} for a basic (weapon_damage) hit, or an ability's literal
 * {@code Damage.amount} for a costed payload -- never the holder's resolved ATTACK_DAMAGE stat.
 * The tooltip describes the weapon, not whoever swings it, so it is mint-time only and cannot drift.
 *
 * {@link DamagePayload.DamageSource} is the load-bearing distinction here: it is what lets the
 * tooltip render a basic attack as a STAT BLOCK (class-labelled damage + attack speed, no prose) and
 * an ability as an ABILITY BLOCK (name, prose, element-labelled damage, cadence). A basic attack is a
 * stat, not an ability, and it should not need a whole section to say so. That distinction now lives
 * in {@link DamagePayload} rather than here, because the cooldown scaler asks the same question --
 * see its javadoc for why one shared answer matters.
 *
 * Note what this class does NOT do: the {@code Attack Speed} it formats is the weapon's BASE, never
 * the holder's resolved attack-speed stat. Lore describes the weapon, not whoever is holding it --
 * that is what makes it mint-time safe and non-drifting. A boosted player and an unboosted one
 * reading the same sword must see the same number.
 *
 * <p><b>THE QUIVER LINE IS THE ONE STATED EXCEPTION, AND THE RULE ABOVE IS NOT DELETED.</b> It still
 * governs attack damage and attack speed, which are the lines it was written for. A quiver's
 * denominator is different: once capacity is a stat, a player with +19 genuinely fires 28, so
 * rendering the authored 9 would satisfy the surface rule while being <b>permanently wrong</b>
 * rather than merely stale.
 *
 * <p>The deciding argument is this file's own closing sentence, four paragraphs down: <i>"each
 * formatter reads THE NUMBER THAT ACTUALLY GOVERNS its own weapon, so neither line can drift from
 * what the weapon really does."</i> Static-ness is the MECHANISM by which non-drifting is achieved
 * for attack damage -- there the definition IS what governs. For a quiver it is not, and the deeper
 * principle points the other way.
 *
 * <p><b>And the tooltip still does not cross the item-to-holder axis at render time.</b> It reads a
 * number off the ITEM's own PDC, exactly as the count already does; no {@code Player} is needed to
 * render it, and everyone looking at that item sees the same number. What is new is that an item's
 * stored value now derives from whoever last WROTE it -- so a weapon packed by a boosted player and
 * dropped reads {@code 8/28} in an unboosted player's hand until their next shot or reload. That is
 * named at {@code QuiverItems.setLoaded} and endorsed in {@code PLAN-quiver-a2.md}; it is the price
 * of the stamp being what ENFORCES, which is what stops the tooltip and the refusal disagreeing.
 *
 * <p>With no item at all -- a recipe-browser icon, a craft preview, the definitions-only golden
 * harness -- there is no stamp and the AUTHORED capacity is what renders, which is the true answer
 * for all three.
 *
 * That base now comes from TWO sources, because the two kinds of basic attack no longer share a
 * cadence. A vanilla-driven melee hit is paced by the vanilla attack-speed attribute, authored
 * directly as {@code attack_speed:}; a ranged basic attack is still paced by its trigger's
 * {@code cooldown_ticks} through {@code AttackSpeed.effectiveCooldownTicks}. Each formatter reads
 * the number that actually governs its own weapon, so neither line can drift from what the weapon
 * really does -- which is why this is two methods and not one with a converted argument.
 */
public final class WeaponLoreLines {

    private WeaponLoreLines() {}

    /** Ticks to a one-decimal second label: 10 -> "0.5s", 60 -> "3.0s". */
    public static String cooldownLabel(int ticks) {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
    }

    /** An input id as a readable label: "left_click" -> "Left-Click". */
    public static String inputLabel(String input) {
        String[] parts = input.split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append('-');
            out.append(capitalize(parts[i]));
        }
        return out.toString();
    }

    /**
     * The trigger's cadence folded onto one line: "Cooldown: 0.5s", or with a cost
     * "Cooldown: 3.0s | Mana Cost: 40". The resource name comes from the cost's resourceId
     * ("mana" -> "Mana Cost"). A free, instant trigger yields "" -- the caller drops the line.
     */
    public static String cadenceLine(int cooldownTicks, ResourceCost cost) {
        List<String> parts = new ArrayList<>();
        if (cooldownTicks > 0) parts.add("Cooldown: " + cooldownLabel(cooldownTicks));
        if (!isFree(cost)) parts.add(capitalize(cost.resourceId()) + " Cost: " + trimNumber(cost.amount()));
        return String.join(" | ", parts);
    }

    /**
     * The magazine, as the tooltip shows it: {@code "Quiver: 8/9"}. Empty string for a weapon that
     * carries no quiver, which the caller drops.
     *
     * <p><b>THE FIRST TOOLTIP LINE IN THIS FILE THAT IS PER-ITEM RATHER THAN PER-DEFINITION.</b>
     * Every other line here is a function of the weapon's content, so two copies of the same weapon
     * render identically forever. This one is not: two Quiver Stones in one inventory show different
     * numbers, and the same stack changes as it is fired. That is why {@code loaded} arrives as a
     * parameter read off the ITEM rather than off {@code weapon} -- and why the stamp has to precede
     * {@code applyLore} at the mint, which was a free ordering until this line existed and is
     * load-bearing now.
     *
     * <p><b>An ABSENT count renders "{@code Quiver: --/9}", not "{@code 0/9}".</b> Absence is not
     * emptiness anywhere else in this feature and it is not here either: a missing stamp is a defect
     * in a mint path, and showing it as a spent magazine would hide that behind a tooltip a player
     * would read as ordinary. The dashes are meant to look wrong.
     *
     * <p>Worked: {@code (9, 9) -> "Quiver: 9/9"}; {@code (0, 9) -> "Quiver: 0/9"};
     * {@code (absent, 9) -> "Quiver: --/9"}; {@code (anything, 0) -> ""} (no quiver).
     */
    public static String quiverLine(OptionalInt loaded, int capacity) {
        if (capacity <= WeaponDefinition.NO_QUIVER) return "";
        return "Quiver: " + (loaded.isPresent() ? String.valueOf(loaded.getAsInt()) : "--")
                + "/" + capacity;
    }

    /**
     * The damage a trigger deals. Delegates to {@link DamagePayload}, which owns the walk and the
     * basic-attack/ability distinction -- the tooltip and the cooldown scaler both ask it, so the
     * two cannot disagree about the same weapon. Kept here so the lore call sites read naturally.
     */
    public static Optional<DamagePayload.TriggerDamage> triggerDamage(List<EffectSpec> onHit,
                                                                      double weaponAttackDamage) {
        return DamagePayload.of(onHit, weaponAttackDamage);
    }

    /**
     * A RANGED basic attack's rate as ATTACKS PER SECOND: 16 ticks between shots -> "1.3". Vanilla
     * Minecraft states attack speed this way on its own item tooltips, so a player already knows
     * that higher is better -- which is why this is not just {@link #cooldownLabel} reused. An
     * ability's cadence still reads as a cooldown in seconds, where lower is better.
     *
     * Derived from the trigger's cooldown because that is what gates the shot: it is scaled by
     * {@code AttackSpeed.effectiveCooldownTicks} and checked by {@code CooldownTracker}. Deriving it
     * rather than authoring a second number is what stops the two disagreeing the first time someone
     * edits one and forgets the other.
     *
     * BUT THE AUTHORED COOLDOWN IS NOT THE DELIVERED INTERVAL UNDER HELD FIRE, SO THIS NUMBER CAN BE
     * FALSE. A held right-click's inputs are quantised onto a 4-tick grid (Q7, {@code GATE-q7.md}), so
     * the real interval is the cooldown rounded UP to the next multiple of 4 -- and this method does
     * not round. (This said "delivers an input only every 4 ticks" until 2026-09-13, when
     * {@code GATE-locust.md} row 1 read {@code INPUTS min 3t} twice: the stream is not periodic, the
     * grid and its arithmetic are unaffected.) Executed across authored 4..40, 18 of 37 values print a rate the weapon does not
     * deliver; authored 5 prints "4.0" against a delivered 2.5/s. The digit is true by construction
     * only when the cooldown is on the 4-tick grid, which both shipped callers happen to satisfy or
     * to miss harmlessly. This sentence used to read "for a ranged basic attack that IS the cadence"
     * and was falsified by that measurement. The full account, and why it is not fixed here, is at
     * {@code WeaponLoader}'s {@code cooldown_ticks} section.
     *
     * A non-positive cooldown yields "" and the caller drops the line: no shipped ranged basic
     * attack has one, and the guard is what stops a zero-cooldown weapon_damage trigger dividing by
     * zero and printing "Infinity" on someone's tooltip.
     */
    public static String rangedAttackSpeedLabel(int cooldownTicks) {
        if (cooldownTicks <= 0) return "";
        return String.format(Locale.ROOT, "%.1f", 20.0 / cooldownTicks);
    }

    /**
     * A MELEE basic attack's rate, read straight off the weapon's authored {@code attack_speed}.
     *
     * Not derived from anything: since vanilla's crosshair attack took over the melee hit, the
     * weapon's cadence IS this number -- it is written onto the wielder's vanilla attack-speed
     * attribute and paces the attack-strength meter the charge curve reads. Authoring it directly
     * rather than deriving it from a tick count is also what makes true vanilla values expressible:
     * every vanilla sword is 1.6, and {@code 20 / n} cannot produce 1.6 for any integer n.
     *
     * A non-positive speed yields "" and the caller drops the line. In practice unreachable for a
     * vanilla-driven melee weapon -- {@code WeaponDefinition} rejects one outright -- so this guard
     * covers the weapon that has no melee basic at all rather than a legal-but-odd number.
     */
    public static String meleeAttackSpeedLabel(double attackSpeed) {
        if (attackSpeed <= 0) return "";
        return String.format(Locale.ROOT, "%.1f", attackSpeed);
    }

    private static boolean isFree(ResourceCost cost) {
        return cost.amount() == 0 || cost.resourceId().equals("none");
    }

    /** A double with the trailing ".0" dropped: 12.0 -> "12", 12.5 -> "12.5". */
    private static String trimNumber(double n) {
        return GearLoreLines.trimNumber(n);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
