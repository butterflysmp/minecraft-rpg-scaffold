package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The colour/layout half of the weapon tooltip: wraps the plain strings/numbers from
 * {@link WeaponLoreLines} (core) in Adventure Components, plus the class label {@link WeaponClassLabel}
 * owns. Pure Adventure, no Bukkit / no ItemStack -- mirrors {@code NameplateText} -- so the string
 * logic stays reddening-tested in core and only the look-at-it layout is boot-witnessed.
 *
 * Every number here is the weapon's STATIC content (declared attack_damage, or an ability's literal
 * Damage amount), never the holder's resolved stat, so the lore is mint-time only and cannot drift.
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

    public static List<Component> build(WeaponDefinition weapon, ElementRegistry elements) {
        List<Component> lore = new ArrayList<>();

        // Element on its own line at the very top, in the ELEMENT's own colour -- not the rarity's.
        lore.add(elementLine(weapon.element(), elements));

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
                lore.add(blank());
                lore.add(plain(WeaponClassLabel.of(weapon.weaponClass()) + " Damage: ", NamedTextColor.GRAY)
                        .append(plain(number(damage.get().amount()), NamedTextColor.RED)));

                String speed = WeaponLoreLines.attackSpeedLabel(ability.cooldownTicks());
                if (!speed.isBlank()) {
                    lore.add(plain("Attack Speed: ", NamedTextColor.GRAY)
                            .append(plain(speed, NamedTextColor.RED)));
                }
                continue;
            }
            if (isBasicAttack) continue;

            // An ability block: gold name + input, authored prose, the ELEMENT-typed damage number,
            // and the cadence. Each is preceded by a blank so they read as distinct abilities.
            lore.add(blank());

            // Ability name (gold) with the click that fires it (yellow), e.g. "Fireball  Right-Click".
            lore.add(plain(ability.displayName(), NamedTextColor.GOLD)
                    .append(plain("  " + WeaponLoreLines.inputLabel(binding.input()), NamedTextColor.YELLOW)));

            for (String line : ability.description()) {
                lore.add(plain(line, NamedTextColor.GRAY));
            }

            // Element-typed, NOT class-typed: this payload reads no stat, so no "+N Melee Damage"
            // modifier can reach it and claiming otherwise would be a lie the tooltip tells.
            if (damage.isPresent()) {
                DamagePayload.TriggerDamage d = damage.get();
                lore.add(plain(elementName(d.element(), elements) + " Damage: ", NamedTextColor.GRAY)
                        .append(plain(number(d.amount()), NamedTextColor.RED)));
            }

            String cadence = WeaponLoreLines.cadenceLine(ability.cooldownTicks(), ability.cost());
            if (!cadence.isBlank()) {
                lore.add(plain(cadence, NamedTextColor.DARK_GRAY));
            }
        }

        // Authored weapon-level flavour, italic + gray. Coexists with the per-ability descriptions.
        if (!weapon.flavor().isEmpty()) {
            lore.add(blank());
            for (String line : weapon.flavor()) {
                lore.add(Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, true));
            }
        }

        // Rarity + class footer at the very bottom, coloured by tier: "Rare Magic Weapon".
        lore.add(blank());
        lore.add(plain(titleCase(weapon.rarity().name()) + " "
                        + WeaponClassLabel.of(weapon.weaponClass()) + " Weapon",
                RarityColors.of(weapon.rarity())));

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
            return plain(titleCase(elementId), NamedTextColor.GRAY);
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
                .orElseGet(() -> titleCase(elementId));
    }

    /** A non-italic lore line in one colour. */
    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component blank() {
        return Component.empty().decoration(TextDecoration.ITALIC, false);
    }

    /** "SOMETHING" -> "Something"; a plain lowercase word -> Titlecase. */
    private static String titleCase(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }

    /** A stat number with the trailing ".0" dropped: 8.0 -> "8", 7.5 -> "7.5". */
    private static String number(double n) {
        if (n == Math.floor(n) && !Double.isInfinite(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }
}
