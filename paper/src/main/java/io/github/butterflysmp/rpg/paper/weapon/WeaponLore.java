package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearLoreLines;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.ability.BasicMelee;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.List;
import java.util.Locale;

/**
 * The colour/layout half of the weapon tooltip: wraps the plain strings/numbers from
 * {@link WeaponLoreLines} (core) in Adventure Components, plus the class label {@link WeaponClassLabel}
 * owns. Pure Adventure, no Bukkit / no ItemStack -- mirrors {@code NameplateText} -- so the string
 * logic stays reddening-tested in core and only the look-at-it layout is boot-witnessed.
 *
 * <b>Every number here is a function of the DEFINITION and the ITEM'S OWN STORED STATE, and never of
 * whoever is holding it.</b> This is the POINTER; the account -- what the invariant used to be, why
 * slice 12c retired it, and what the weaker form protects -- is {@link WeaponLoreLines}' class
 * javadoc. <b>It said "STATIC content ... mint-time only and cannot drift" until 2026-09-21</b>, and
 * that was surrendered on purpose rather than falsified by accident.
 * Layout top to bottom: element, the basic-attack STAT BLOCK, one ABILITY BLOCK per remaining
 * trigger (name+input, authored description, element-typed damage, cadence), the weapon-level
 * flavour, and the "<Rarity> <Class> Weapon" footer.
 *
 * A basic attack is a stat, not an ability, so it renders as two stat lines rather than a section
 * with a name and prose. The split is by {@link DamagePayload.DamageSource} -- an effect that
 * READS the attack-damage stat is a basic attack; one carrying its own literal is an ability --
 * never by the input name. That is also why the two damage lines are labelled differently: the
 * class label ("Melee Damage") goes only on the stat-reading line a "+N Melee Damage" modifier
 * could actually reach, and ability payloads are labelled by their element ("Fire Damage").
 *
 * Two colour axes, owned by two different places on purpose: the ELEMENT line wears the element's
 * own colour from its content file (open axis -> content owns it), and the footer wears the rarity
 * tier's colour from {@link RarityColors} (closed enum -> code owns it). The element line used to
 * be rarity-coloured, which meant a weapon's element never showed its own identity.
 */
public final class WeaponLore {

    private WeaponLore() {}

    /**
     * The tooltip for a weapon with no per-item state to show -- every caller that has a definition
     * but no item. Renders a quiver weapon's magazine as {@code --/N}, which is the honest answer
     * when there is no item to read a count from.
     */
    public static List<Component> build(WeaponDefinition weapon, ElementRegistry elements) {
        return build(weapon, elements, OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty());
    }

    /**
     * The tooltip, with the item's own magazine count.
     *
     * <p><b>{@code loaded} is the first per-ITEM value this builder has ever taken</b>, and it is the
     * reason the signature widened. Every other line here is a function of the definition, so two
     * copies of a weapon render identically forever; a quiver's count differs between two stacks of
     * the same weapon and changes as one is fired.
     *
     * <p>It arrives as an {@link OptionalInt} rather than an {@code int} for the reason that runs
     * through this whole feature: <b>absence is not emptiness.</b> An unstamped item renders
     * {@code --/N}, not {@code 0/N}, so a mint path that forgot to stamp looks wrong on the tooltip
     * instead of looking like a spent magazine.
     */
    public static List<Component> build(WeaponDefinition weapon, ElementRegistry elements,
                                        OptionalInt loaded, OptionalInt stampedCapacity,
                                        OptionalInt score) {
        List<Component> lore = new ArrayList<>();

        // *** THE COMPOSED DOOR, ASKED ONCE AND USED THREE TIMES: the score line, the class-damage
        // number and the element-damage number. *** A weapon that declares itself unscored must
        // render its AUTHORED damage and NO score line -- and neither falls out of scaledDamage,
        // which is honest arithmetic on whatever score it is handed.
        //
        // IT CANNOT BE `orAbsent` ALONE, AND THAT IS THE WHOLE POINT. Slice 12 shipped "absent key
        // means 100", so orAbsent collapses "declared unscored" with "carries no stamp" -- and the
        // exclusion lives on exactly that difference. A volley_stone minted since slice 12 carries
        // a REAL score in its PDC; scaledDamage(4, 400) is 16, and the stone deals 4.
        //
        // Same door the stamp and the average ask (GearScore.carriesScore), so a weapon cannot come
        // to render as one thing and behave as the other. scoreable() is package-private in core on
        // purpose, so this is the only question paper is able to ask.
        boolean scored = GearScore.carriesScore(GearItems.gearClassOf(weapon), weapon.unscored());

        // Element on its own line at the very top, in the ELEMENT's own colour -- not the rarity's.
        lore.add(elementLine(weapon.element(), elements));

        // THE GEAR SCORE, BELOW THE ELEMENT -- Ben's ruling, 2026-09-21.
        //
        // THE REASONING THAT WAS OVERTURNED, KEPT RATHER THAN DELETED BECAUSE A CALL WAS MADE HERE
        // AND A NEUTRAL COMMENT WOULD READ AS THOUGH THE ORDER HAD ALWAYS BEEN THIS WAY: the line
        // sat ABOVE EVERYTHING, on the argument that the score is the item POWER rating -- the
        // number a player compares two drops of the same weapon by -- so it outranked the element,
        // which is identity rather than power. That argument still describes what the score IS. It
        // was overruled on where the line BELONGS, not refuted. DO NOT RE-DERIVE THE OLD ORDER
        // FROM IT: reasoning from power-vs-identity reaches the pre-12d layout every time, which is
        // exactly why the argument is recorded here instead of being left to be rediscovered.
        //
        // Absent on an unstamped item and on every definitions-only rendering; see
        // GearLore.appendScore for why it must not print 100 there.
        //
        // AND ABSENT ON AN UNSCORED WEAPON EVEN WHEN THE PDC HOLDS ONE. The label promises a
        // contribution to the wearer's average, and candidateScore refuses this item there, so
        // printing the number would advertise something the item does not do.
        GearLore.appendScore(lore, scored ? score : OptionalInt.empty());

        // THE MAGAZINE, above the stat/ability blocks -- directly under the GEAR SCORE when one
        // prints, and under the ELEMENT when it does not, because that line is conditional. (It read
        // "directly under the element" until 12d moved the score between them.) It is the
        // weapon's most volatile number and the one a player checks mid-fight, so it goes where the
        // eye lands first rather than below prose. Empty string for a weapon with no quiver, which
        // is every weapon but quiver_stone (the fixture) and boltor (the first shipped one).
        //
        // NOTE THE BOLTOR IS THE FIRST WEAPON TO REACH THIS LINE **AND** THE STAT BLOCK BELOW. Its
        // shot is a weapon_damage basic attack, so it renders a quiver line AND a "Ranged Damage" /
        // "Attack Speed" pair; quiver_stone carries a literal damage payload and renders an ability
        // block instead. The quiver line is keyed on CAPACITY alone and sits above the trigger loop,
        // so the two are independent by construction rather than by luck -- but this is the first
        // weapon on which that independence is exercised at all.
        // THE DENOMINATOR IS THE STAMP, NOT THE DEFINITION -- resolved through QuiverState.capacityOf
        // so the tooltip and the refusal logic read the SAME number. A tooltip on the stamp while the
        // refusal resolved the holder live would lie by a new mechanism. With no item (a
        // definitions-only renderer, a browser icon) the stamp is absent and capacityOf returns the
        // authored value, which is why golden-lore.txt is byte-identical across this change.
        String quiver = WeaponLoreLines.quiverLine(
                loaded, QuiverState.capacityOf(stampedCapacity, weapon.quiverSize()));
        if (!quiver.isEmpty()) {
            lore.add(GearLore.plain(quiver, NamedTextColor.GRAY));
        }

        // A basic attack is a STAT, not an ability: it gets two stat lines directly under the
        // element, with no name, no prose and no cadence. Everything else is an ability block.
        // The split is by DamagePayload, never by input name -- and it is the SAME call the cooldown
        // scaler makes, so a weapon cannot render as one thing and behave as the other.
        boolean statBlockPlaced = false;
        for (TriggerBinding binding : weapon.triggers()) {
            AbilityDefinition ability = binding.ability();
            var damage = WeaponLoreLines.triggerDamage(ability.onHit(), weapon.attackDamage());
            boolean isBasicAttack = DamagePayload.isBasicAttack(ability.onHit());

            // Only the FIRST basic attack becomes the stat block; a second weapon_damage trigger
            // would have nowhere to go, and no shipped weapon declares one.
            //
            // damage.isPresent() is implied by isBasicAttack (the source comes OFF that damage), and
            // is asked anyway rather than orElseThrow: the two are computed separately, and a
            // cosmetic tooltip must never be what crashes a /rpg give if that ever stops holding.
            if (isBasicAttack && !statBlockPlaced && damage.isPresent()) {
                statBlockPlaced = true;
                lore.add(GearLore.blank());
                lore.add(GearLore.plain(WeaponClassLabel.of(weapon.weaponClass()) + " Damage: ", NamedTextColor.GRAY)
                        .append(GearLore.plain(number(shown(damage.get().amount(), scored, score)),
                                NamedTextColor.RED)));

                // Which number paces THIS basic attack? A vanilla-driven melee hit is paced by the
                // authored attack_speed written onto the wielder's vanilla attribute; a ranged one
                // is still paced by its trigger's cooldown through CooldownTracker. Branching on
                // BasicMelee -- the same predicate that routes the hit itself -- is what keeps the
                // displayed cadence and the real one from diverging for either kind.
                String speed = BasicMelee.isVanillaDriven(ability)
                        ? WeaponLoreLines.meleeAttackSpeedLabel(weapon.attackSpeed())
                        : WeaponLoreLines.rangedAttackSpeedLabel(ability.cooldownTicks());
                if (!speed.isBlank()) {
                    lore.add(GearLore.plain("Attack Speed: ", NamedTextColor.GRAY)
                            .append(GearLore.plain(speed, NamedTextColor.RED)));
                }
                continue;
            }
            if (isBasicAttack) continue;

            // An ability block: gold name + input, authored prose, the ELEMENT-typed damage number,
            // and the cadence. Each is preceded by a blank so they read as distinct abilities.
            lore.add(GearLore.blank());

            // Ability name (gold) with the click that fires it (yellow), e.g. "Fireball  Right-Click".
            lore.add(GearLore.plain(ability.displayName(), NamedTextColor.GOLD)
                    .append(GearLore.plain("  " + WeaponLoreLines.inputLabel(binding.input()), NamedTextColor.YELLOW)));

            for (String line : ability.description()) {
                lore.add(GearLore.plain(line, NamedTextColor.GRAY));
            }

            // Element-typed, NOT class-typed: this payload reads no stat, so no "+N Melee Damage"
            // modifier can reach it and claiming otherwise would be a lie the tooltip tells.
            if (damage.isPresent()) {
                DamagePayload.TriggerDamage d = damage.get();
                // "x N" ONLY WHEN ONE PRESS DELIVERS N PAYLOADS SEQUENTIALLY. The criterion, its
                // interruption caveat and the reason a fan is excluded all live in
                // WeaponLoreLines.deliveredShots -- this is the pointer, that is the account.
                //
                // THE NUMBER STAYS PER-SHOT and the count is rendered beside it rather than folded
                // in. A single figure of 162 would be true of the volley and false of every shot,
                // and a player comparing two weapons needs the per-hit number to compare AT ALL.
                OptionalInt shots = WeaponLoreLines.deliveredShots(ability.cast());
                String shotsLabel = shots.isPresent() ? "  x " + shots.getAsInt() : "";
                lore.add(GearLore.plain(elementName(d.element(), elements) + " Damage: ", NamedTextColor.GRAY)
                        .append(GearLore.plain(number(shown(d.amount(), scored, score)) + shotsLabel,
                                NamedTextColor.RED)));
            }

            String cadence = WeaponLoreLines.cadenceLine(ability.cooldownTicks(), ability.cost());
            if (!cadence.isBlank()) {
                lore.add(GearLore.plain(cadence, NamedTextColor.DARK_GRAY));
            }
        }

        // Authored weapon-level flavour, italic + gray. Coexists with the per-ability descriptions.
        GearLore.appendFlavor(lore, weapon);

        // Rarity + class footer at the very bottom, coloured by tier: "Rare Magic Weapon".
        GearLore.appendRarityFooter(lore, weapon.rarity(),
                WeaponClassLabel.of(weapon.weaponClass()) + " Weapon");

        return lore;
    }

    /**
     * The element's own name, in the element's own colour, taken from its content file --
     * {@code content/elements/fire.yml} declares {@code display_name: "<red>Fire</red>"}, and that
     * MiniMessage IS the mapping. There is deliberately no ElementColors switch mirroring
     * {@link RarityColors}: rarity is a CLOSED enum, so a colour per tier belongs in code where a
     * new tier is a compile error; element is an OPEN, content-driven string, so a switch would
     * have to be edited for every new element yml and would silently mis-colour until it was.
     * Content is data (CLAUDE.md invariant 2) -- ask the registry.
     *
     * Fails soft on a miss: ContentValidator already rejects a weapon naming a dangling element at
     * boot, so this cannot happen in production, but a cosmetic line must never crash a give.
     */
    private static Component elementLine(String elementId, ElementRegistry elements) {
        ElementDefinition element = elements.find(elementId).orElse(null);
        if (element == null) {
            return GearLore.plain(GearLore.titleCase(elementId), NamedTextColor.GRAY);
        }
        return MiniMessage.miniMessage().deserialize(element.displayName())
                // Lore renders italic by default; every other line here opts out, so this must too.
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * An element's name as PLAIN words for use inside a longer line ("Fire Damage: 12"), taken from
     * the same content {@code display_name} {@link #elementLine} renders -- so a two-word or
     * oddly-cased element reads the way its content authored it, not the way its id happens to be
     * spelled. The colour is stripped here on purpose: this is a fragment of a gray label line, and
     * the element already wears its own colour on its own line at the top.
     */
    private static String elementName(String elementId, ElementRegistry elements) {
        return elements.find(elementId)
                .map(e -> PlainTextComponentSerializer.plainText()
                        .serialize(MiniMessage.miniMessage().deserialize(e.displayName())))
                .orElseGet(() -> GearLore.titleCase(elementId));
    }

    /** A stat number with the trailing ".0" dropped: 8.0 -> "8", 7.5 -> "7.5". */
    private static String number(double n) {
        return GearLoreLines.trimNumber(n);
    }

    /**
     * The damage figure a player should SEE: the authored number scaled by this item's own score,
     * or the authored number untouched on a weapon that declares itself unscored.
     *
     * <h2>*** THE POINT OF THE WHOLE SLICE: THIS MUST EQUAL WHAT THE SYSTEM PRODUCES ***</h2>
     *
     * Both runtime arms scale by the same factor from the same source -- {@code WeaponAttackItems}
     * folds it into the ATTACK_DAMAGE stat for a basic hit, {@code EffectApplier} applies it to the
     * literal amount at cast -- so rendering the authored figure showed one number while the weapon
     * dealt another, from the day slice 12 merged.
     *
     * <p><b>It cannot double-scale, and that was MEASURED rather than assumed.</b> The lore reads
     * {@code WeaponDefinition.attackDamage()} / {@code EffectSpec.Damage.amount()} -- the raw
     * authored fields -- and each runtime arm multiplies a fresh local copy. There is no shared
     * mutated value, so this multiplication is the first one applied to what the tooltip shows.
     *
     * <h2>NO ROUNDING, DELIBERATELY</h2>
     *
     * {@code scaledDamage(19, 175)} is {@code 33.25} and {@code 33.25} is what the attribute
     * carries. <b>Rounding here would reintroduce the exact dishonesty this slice removes</b>, one
     * decimal place smaller. {@code trimNumber} drops a trailing {@code .0} and keeps a real
     * fraction, which is the honest rendering of both cases.
     *
     * <p><b>{@code orAbsent} is correct HERE and would be wrong as the only question.</b> An
     * unstamped item and a definitions-only rendering both resolve to the baseline, so the authored
     * number renders unchanged -- which is why {@code golden-lore.txt}'s damage figures do not move.
     * The exclusion is asked separately, by the caller, for the reason its comment gives.
     */
    private static double shown(double authored, boolean scored, OptionalInt score) {
        return scored ? GearScore.scaledDamage(authored, GearScore.orAbsent(score)) : authored;
    }

    /**
     * The tooltip with a magazine but no gear score -- a rendering driven by a DEFINITION plus a
     * stamped count, with no stack to read a score off.
     *
     * <p>Kept as its own overload for the reason {@code ArmorLore} states: the empty belongs in one
     * place rather than at every definitions-only call site, and an empty inside
     * {@code WeaponItems.applyLore} -- which always holds meta -- would be a bug rather than an
     * honest absence. An exact IDENTITY with the pre-gear-score renderer, which is why
     * {@code golden-lore.txt} is byte-identical across this slice.
     */
    public static List<Component> build(WeaponDefinition weapon, ElementRegistry elements,
                                        OptionalInt loaded, OptionalInt stampedCapacity) {
        return build(weapon, elements, loaded, stampedCapacity, OptionalInt.empty());
    }
}
