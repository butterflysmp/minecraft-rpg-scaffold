package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;
import io.github.butterflysmp.rpg.core.enchant.EnchantCurve;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;

/**
 * What an active enchant actually RESOLVES to on the weapon it is sitting on, as a suffix on the
 * "active:" line -- and whether it is inert there. ONE exhaustive switch on
 * {@code EnchantEffect}, shared by {@code /rpg enchant active}'s reply and by {@code show}.
 *
 * <p><b>The sharing is the point.</b> These were two describers, and they had drifted in opposite
 * directions: the activation reply appended Unbreaking's consume rate for EVERY enchant, so
 * activating Sharpness reported "consumes durability on 25% of uses", while show returned "" for a
 * durability enchant and so never printed the rate at all. Each covered exactly what the other
 * dropped. One switch, no default arm, cannot drift.
 *
 * <p>It exists so the boot gate can read the EXPECTED number off the screen BEFORE swinging,
 * rather than deciding afterwards what the number it got should have been. That matters most on
 * the damage arm: the popup ROUNDS, so at the shipped [5, 10, 15] curve a Sharpness I sword
 * renders "8" exactly like an unenchanted one. Without this line the gate could not tell "the
 * enchant is inert" from "the enchant applied and rounding hid it".
 *
 * <p>The class mismatch is called out explicitly for the same reason -- Sharpness on a bow is
 * SUPPOSED to do nothing, and a silent absence of change looks identical to a broken gate. It is
 * said at ACTIVATION and not only in show, because that is the moment the mistake is correctable:
 * the alternative to the inert note is not silence, it is "(+15% damage, x1.15)" on a weapon where
 * that multiplier never applies.
 */
public final class EnchantEffectLine {

    private EnchantEffectLine() {}

    /**
     * The parenthetical suffix, leading space included, ready to concatenate.
     *
     * @param definition the enchant's content file, or {@code null} for an id the registry no
     *                   longer knows -- reachable, because the loader fail-softs a malformed file
     *                   and the item's blob still names it.
     * @param level      the resolved (effective) level, not the candidate's own.
     * @param heldClass  the {@link GearClass} of the GEAR the enchant is sitting on. Still never
     *                   null, and now honestly so for both kinds: a weapon maps through
     *                   {@code GearClass.of} (its loader refuses a file without a class) and a
     *                   shield presents {@code SHIELD}.
     *                   <p>
     *                   <b>This is the Slice-1 descope reversed.</b> {@code /rpg enchant show}
     *                   refused shields, and {@code HeldGear.effectSuffix} returned "", because
     *                   there was no honest value to pass here -- not because anything crashed. No
     *                   caller ever passed null. Supplying a real value removes the reason those
     *                   refusals existed rather than fixing a bug they were hiding.
     */
    public static String of(EnchantDefinition definition, int level, GearClass heldClass) {
        return of(definition, level, heldClass, id -> java.util.Optional.empty());
    }

    /** The same, able to read the WEAPONS (D3: an orb's shot is shown as the number off its weapon's own beam). */
    public static String of(EnchantDefinition definition, int level, GearClass heldClass,
            java.util.function.Function<String, java.util.Optional<io.github.butterflysmp.rpg.core.weapon.WeaponDefinition>> weapons) {
        return " (" + bare(definition, level, heldClass, weapons) + ")";
    }

    /**
     * The same sentence with no brackets and no leading space -- what a LORE LINE needs, where a
     * parenthetical aside reads as an afterthought rather than as the description.
     *
     * <p>Extracted rather than stripped at the call site so there is still exactly ONE exhaustive
     * switch on {@code EnchantEffect}. Two describers is what this class was created to end, and a
     * caller trimming the brackets off itself is how the second one comes back.
     *
     * <p><b>Never called with level 0.</b> A LOCKED candidate is described at the level it would
     * BECOME, because the arms below are total over levels rather than guarded: level 0 would read
     * "+0% damage" for a damage enchant and, worse, "consumes durability on 100% of uses" for
     * Unbreaking -- which is backwards, and reads as a curse. The enchant menu passes
     * {@code Math.max(1, level)} and says "Click to unlock at I" beneath, so the two agree.
     */
    public static String bare(EnchantDefinition definition, int level, GearClass heldClass) {
        return bare(definition, level, heldClass, id -> java.util.Optional.empty());
    }

    /** The same, able to read the WEAPONS (D3). */
    public static String bare(EnchantDefinition definition, int level, GearClass heldClass,
            java.util.function.Function<String, java.util.Optional<io.github.butterflysmp.rpg.core.weapon.WeaponDefinition>> weapons) {
        if (definition == null) return "unknown enchant -- grants nothing";

        // No default arm, deliberately -- the same discipline as WeaponClassLabel.of. A third
        // EnchantEffect constant is a compile error here until someone gives it words.
        return switch (definition.effect()) {
            case DURABILITY -> "consumes durability on "
                    + Math.round(Unbreaking.consumeChance(level) * 100) + "% of uses";
            case DAMAGE -> {
                if (!definition.isUniversal() && definition.gearClass() != heldClass) {
                    // The held side is a NOUN PHRASE, not a bare label with "weapon" glued on.
                    // The old wording hardcoded "... on a X weapon", which reads correctly for the
                    // three fighting classes and absurdly for the fourth -- "on a Shield weapon".
                    // Byte-identical for every weapon case, so the existing assertions are unchanged.
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                double percent = DamageEnchants.percentAt(definition.valueByLevel(), level);
                yield String.format("+%.0f%% damage, x%.2f", percent,
                        DamageEnchants.multiplier(percent));
            }
            case BLOCK_DR -> {
                // The gate is enforced at the content boundary (a BLOCK_DR enchant must be
                // class: shield), so the only way to be holding one on the wrong gear is the dev
                // command or a hand-edited item -- reachable, so it is described rather than assumed
                // away.
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // THE SAME WORDS THE ITEM USES. ShieldLoreLines.DAMAGE_REDUCTION_LABEL says "Damage
                // Reduction" on the shield itself, so the enchant that modifies that stat must not
                // call it something else -- a player reading "+15% block" above "Damage Reduction:
                // 50%" has to work out that those are one number.
                //
                // POINTS, not a multiplier: Bulwark is additive on the fraction, so "+15%" means the
                // shield stops fifteen more POINTS of the hit (0.35 -> 0.50), not fifteen percent
                // more of what it already stopped. Saying "x1.15" would describe the rejected
                // reading. The gate reads this line before blocking, so it must be the real number.
                double percent = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f%% Damage Reduction", percent);
            }
            case REFLECT -> {
                // Same gate story as BLOCK_DR: refused at the content boundary, so the only way to
                // hold one on the wrong gear is the dev command or a hand-edited item. Reachable,
                // so it is described rather than assumed away.
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // "to the attacker" is not decoration -- it is the one word that stops this reading
                // as a damage bonus to your own hits. And the percent is of the INCOMING blow, not
                // of what got through, which is why the wording says nothing about blocking.
                double percent = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f%% reflected to the attacker", percent);
            }
            case DEFENSE -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // NO PERCENT SIGN, and that is the whole difference from the three arms above.
                // Defense is a SUMMAND in armor points -- the piece's own Defense line says
                // "Defense: 8", and this adds to that number, not to a fraction of it. Writing
                // "+9%" here would describe an enchant that does not exist and would disagree with
                // the item two lines up.
                //
                // THE SAME WORD THE ITEM USES: ArmorLoreLines.DEFENSE_LABEL prints "Defense" on the
                // piece itself, so the enchant that modifies that stat must not call it something
                // else. Same rule the BLOCK_DR arm follows for "Damage Reduction".
                double points = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f Defense", points);
            }
            case MAX_HEALTH -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // Points again, for the same reason. "Max Health" rather than "Health": the enchant
                // raises the CEILING and grants no current health at all -- equipping is headroom,
                // never a heal -- and a line reading "+30 Health" would promise the heal.
                double points = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f Max Health", points);
            }
            case MAX_MANA -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // Points, and "Max Mana" rather than "Mana", for the identical reasons the arm
                // above gives: the enchant raises the CEILING of a pool and grants no current mana,
                // so "+30 Mana" would promise a top-up the pin deliberately withholds. Equipping is
                // headroom here too -- ResourcePool.setCurrent is what makes that true even for a
                // player who has never cast.
                double points = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f Max Mana", points);
            }
            case MANA_REGEN -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // Mana Battery (2026-11-27): mana per FIVE SECONDS added to the regeneration rate, the authored number,
                // which is what the sheet's Mana Regen line moves by (C4). "Mana Regen" is the sheet's word for the stat.
                // U1 (the seat, 2026-11-27; Ben: "Yes the mana battery's tooltip should read /5"): the number through
                // the scan's conversion and contribution and back out through the sheet's formatter, with the sheet's
                // suffix: "+3 Mana Regen/5s". Wording otherwise SEAT PLACEHOLDER 2026-11-27.
                double perFiveSeconds = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield "+" + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSecondsAmount(
                        io.github.butterflysmp.rpg.core.combat.ManaRegen.contribution(
                                io.github.butterflysmp.rpg.core.combat.ManaRegen.perSecondFromPerFiveSeconds(perFiveSeconds)))
                        + " " + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.MANA_REGEN_LABEL
                        + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.RATE_SUFFIX;
            }
            case QUIVER_SIZE -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // WHOLE ARROWS, AND THE UNIT IS SAID OUT LOUD. Every arm above renders a percentage
                // or a points figure; this is the only one whose number is a COUNT OF THINGS, and a
                // bare "+2" beside "+15% damage" would read as another percentage.
                //
                // "Quiver" is the word the item already uses -- WeaponLoreLines renders the magazine
                // as a Quiver line on the weapon itself -- so the enchant that modifies that stat
                // must not call it something else, the same rule the BLOCK_DR arm follows for
                // "Damage Reduction".
                //
                // SINGULAR AT ONE. "+1 Arrows" is the kind of thing nobody notices in review and
                // every player notices immediately.
                int arrows = (int) EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield "+" + arrows + " Quiver " + (arrows == 1 ? "Arrow" : "Arrows");
            }
            case RELOAD_TIME -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // DEFT HANDS, Ben's ruling (a): the reload TIME is cut by this percent, so the line says the time and
                // a MINUS -- "-30% Reload Time", never "+30% Reload Speed", which is ruling (b) and was not chosen.
                // "Reload" is the word the stats sheet already uses (StatsSheetLines.RELOAD_TIME_LABEL), the same
                // rule the quiver arm above follows for "Quiver". The PERCENT is the tier's, not the ticks it buys:
                // that depends on the weapon, and the stats sheet's Reload line shows the resolved seconds.
                double percent = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("-%.0f%% Reload Time", percent);
            }
            // A SHOCKWAVE (B5, 2026-11-23): the REAL numbers for the level shown, read from the file's block. The damage is
            // the authored amount, which the gear score then scales as it scales every literal (the weapon's own damage
            // line does the same). WORDING: SEAT PLACEHOLDER 2026-11-23, OPEN FOR BEN.
            case SHOCKWAVE -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Shockwave w)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield w.hits() + " hits on one foe in " + number(w.windowTicks() / 20.0) + "s: a "
                        + number(w.damageAt(level)) + " damage shockwave, " + number(w.radiusAt(level)) + " blocks";
            }
            // A GRANTED TRIGGER (D1, B5): the gesture and the ability's name, its cost and cooldown as the tooltip's own
            // cadence line prints them. WORDING: SEAT PLACEHOLDER 2026-11-23, OPEN FOR BEN.
            case GRANT_TRIGGER -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.GrantTrigger g)) {
                    yield "unknown enchant -- grants nothing";
                }
                String cadence = io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines.cadenceLine(
                        g.ability().cooldownTicks(), g.ability().cost());
                yield "grants " + io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines.inputLabel(g.input()) + ": "
                        + g.ability().displayName() + (cadence.isBlank() ? "" : " (" + cadence + ")") + orbShotClause(g, level, weapons);
            }
            // AN AIRBURST (B5): the level's burst damage and the two distances, as authored. WORDING: SEAT PLACEHOLDER
            // 2026-11-23, OPEN FOR BEN.
            case AIRBURST -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst a)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "splits after " + number(a.travelBlocks()) + " blocks into " + a.count() + " bursts of "
                        + number(a.damageAt(level)) + " damage";
            }
            // SHATTER (B5): the level's stone damage and its distance, as authored (the gear score scales it in play).
            // WORDING: SEAT PLACEHOLDER 2026-11-24, OPEN FOR BEN.
            case SHATTER -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Shatter s)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "fires one stone of " + number(s.damageAt(level)) + " damage that shatters after "
                        + number(s.travelBlocks()) + " blocks into the usual chunks";
            }
            // CLUSTER BOMBS (B5, F9): the level's big blast and its radius, as authored (the gear score scales it in play).
            // WORDING: SEAT PLACEHOLDER 2026-11-26, OPEN FOR BEN.
            case CLUSTER_BOMBS -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.ClusterBombs c)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "throws one big chunk of " + number(c.damageAt(level)) + " damage in " + number(c.radius())
                        + " blocks that splits into " + c.splitCount() + " of the usual";
            }
            // A CAST TUNE (G4, B5): the level's REAL number, in the quantity's own unit. Damage is the authored addition,
            // which the gear score scales in play with the base it is added to (G5). A wind-up cut is shown in seconds,
            // Ben's unit ("0.1/0.2/0.4"). WORDING: SEAT PLACEHOLDER 2026-11-24, OPEN FOR BEN.
            case CAST_TUNE -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune t)) {
                    yield "unknown enchant -- grants nothing";
                }
                double v = t.valueAt(level);
                yield switch (t.quantity()) {
                    case DAMAGE -> "+" + number(v) + " damage";
                    case RANGE -> "+" + number(v) + " blocks of range";
                    case SHOTS -> "volleys of " + number(v) + " shots";
                    case WINDUP -> "-" + number(v / 20.0) + "s wind-up";
                    // FAST AND FUNGAL (F7, 2026-11-26): in Ben's unit, seconds ("1/2/3s"). WORDING: SEAT PLACEHOLDER 2026-11-26.
                    case COOLDOWN -> "-" + number(v / 20.0) + "s cooldown";
                };
            }
            // SHARPENED GLASS (K9, 2026-11-25): the level's REAL number (B5), FLAT. WORDING: SEAT PLACEHOLDER 2026-11-25, OPEN FOR
            // BEN.
            case CRIT_CHANCE -> "+" + number(EnchantCurve.valueAt(definition.valueByLevel(), level)) + "% Crit Chance";
            // MANA BURN (F3): the level's percent, as authored. WORDING: SEAT PLACEHOLDER 2026-11-24, OPEN FOR BEN.
            case SCORCH_BOOST -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.ScorchBoost b)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "+" + number(b.percentFor(level)) + "% Scorch and Ignite damage";
            }
            // WILDFIRE (B5): the level's step, the cap and the window, as authored. WORDING: SEAT PLACEHOLDER 2026-11-24,
            // OPEN FOR BEN.
            case HIT_CHAIN -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.HitChain h)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "+" + number(h.stepPercentByLevel().get(Math.max(1, Math.min(level, h.levels())) - 1))
                        + "% per hit on one foe within " + number(h.windowTicks() / 20.0) + "s of the last (max +"
                        + number(h.capPercent()) + "%)";
            }
            // DRUMMING BEAT (M9, 2026-11-26): the level's REAL step, in points, per consecutive hit (B5). CONDITIONAL (T8): the
            // club's stat lines never move for it. WORDING: SEAT PLACEHOLDER 2026-11-26, OPEN FOR BEN.
            case DRUMMING_BEAT -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.DrummingBeat d)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "+" + number(d.stepPercentFor(level)) + "% Crit Chance per consecutive hit";
            }
            // FOCUSED BEAM (B5): the level's beam damage and its range, as authored (the gear score scales it in play).
            // WORDING: SEAT PLACEHOLDER 2026-11-24, OPEN FOR BEN.
            case FOCUSED_BEAM -> {
                if (!(definition.behaviour() instanceof io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.FocusedBeam f)) {
                    yield "unknown enchant -- grants nothing";
                }
                yield "after the wind-up, fires one beam of " + number(f.damageAt(level)) + " damage reaching "
                        + number(f.range()) + " blocks";
            }
        };
    }

    /**
     * THE ORB'S SHOT, AS THE NUMBER (D3; Ben: "That number should be displayed in the actual number not the percentage").
     * The weapon's OWN right-click damage, read from its loaded definition now (never copied into the enchant file), times
     * the level's percent: at the Lapis Staff's 21, 4.2 / 6.3 / 8.4 (EXECUTED: 21 x 20 / 100.0 and its siblings are exact
     * in doubles, so {@link #number} prints them as they are, with no rounding rule needed). A renderer that cannot see the
     * weapons shows the percent instead, and says so. The authored amount, as every line here shows; the gear score scales
     * it in play. WORDING: SEAT PLACEHOLDER 2026-11-23.
     */
    static String orbShotClause(io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.GrantTrigger g, int level,
            java.util.function.Function<String, java.util.Optional<io.github.butterflysmp.rpg.core.weapon.WeaponDefinition>> weapons) {
        if (!(g.ability().cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Orb orb) || orb.shot() == null) {
            return "";
        }
        java.util.Optional<Double> beam = weapons.apply(g.weaponId())
                .flatMap(w -> w.trigger("right_click"))
                .flatMap(t -> t.ability().onHit().stream()
                        .filter(e -> e instanceof io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage)
                        .map(e -> ((io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage) e).amount())
                        .findFirst());
        String each = beam.map(amount -> number(orb.shot().amountFor(amount, level)) + " damage")
                .orElse(number(orb.shot().percentAt(level)) + "% of the beam");
        return "; a beam into it strikes each foe within " + number(orb.shot().radius()) + " for " + each;
    }

    /** A whole number without a point (15.0 -> "15"); otherwise as Java prints it (22.5 -> "22.5"). */
    static String number(double v) {
        return v == Math.rint(v) && Math.abs(v) < 1e15 ? Long.toString((long) v) : Double.toString(v);
    }
}
