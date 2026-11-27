package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.BehaviourCasts;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * WHAT A BEHAVIOUR ENCHANT MAKES AN ABILITY DO, AS ITS TOOLTIP BLOCK (true tooltips, the seat's T7, 2026-11-26; Ben:
 * "3. B", the ability lines are rewritten). Damage and counts only (the seat's narrowing after Ben's "C": no range).
 *
 * <p><b>ONE EXHAUSTIVE SWITCH over every {@link EnchantBehaviour} type</b> ({@link #rowsFor}): a new behaviour type that
 * says nothing about its lines does not compile here. Every number comes from the SAME resolvers the press uses
 * ({@link BehaviourCasts}, {@link DamagePayload}), never a second formula; the WORDS come from the enchant's own YAML
 * ({@code ability_description:}), carried in {@link ItemEnchantTerms#prose}, or stay the ability's own when it authors
 * none. No enchant id and no weapon id is written here.
 */
public final class BehaviourBlocks {

    private BehaviourBlocks() {}

    /**
     * One damage line of a block, BEFORE the item's terms: the authored amount (the renderer scales it by the score and
     * the item's damage enchants, as the Damage arm does), its element, and the count one press delivers (empty for 1).
     */
    public record DamageRow(double authored, String element, OptionalInt count) {}

    /** A rewritten ability: the prose to show (tokens not yet filled) and its damage lines, top to bottom. */
    public record Block(List<String> description, List<DamageRow> rows) {}

    /**
     * The block for {@code ability} under the item's FIRST active behaviour that changes it, or empty when none does (then
     * the ability renders as authored, its cast already tuned by {@code CastTunes}).
     *
     * @param ability     the ability as the press fires it: the weapon's own trigger tuned by {@code CastTunes.tune}, or a
     *                    trigger an enchant grants
     * @param ownAbilities the weapon's own triggers, tuned, for a granted orb whose shot reads its beam
     */
    public static Optional<Block> rewrite(WeaponDefinition weapon, AbilityDefinition ability, ItemEnchantTerms terms,
                                          List<AbilityDefinition> ownAbilities) {
        for (ActiveBehaviour active : terms.behaviours()) {
            Optional<List<DamageRow>> rows = rowsFor(weapon, ability, active, ownAbilities);
            if (rows.isPresent()) {
                List<String> prose = terms.proseOf(active.enchantId()).orElse(ability.description());
                return Optional.of(new Block(prose, rows.get()));
            }
        }
        return Optional.empty();
    }

    /** THE SWITCH: what each behaviour type does to this ability's damage lines, or empty when it does not change it. */
    static Optional<List<DamageRow>> rowsFor(WeaponDefinition weapon, AbilityDefinition ability, ActiveBehaviour active,
                                             List<AbilityDefinition> ownAbilities) {
        List<ActiveBehaviour> one = List.of(active);
        double attack = weapon.attackDamage();
        return switch (active.behaviour()) {
            // FOCUSED BEAM: the volley's first shot becomes ONE beam at the level's damage; no "x N".
            case EnchantBehaviour.FocusedBeam beam -> {
                if (!(ability.cast() instanceof CastSpec.Volley v)
                        || BehaviourCasts.focusedBeam(ability.id(), v.of(), one).isEmpty()) yield Optional.empty();
                yield Optional.of(rows(row(BehaviourCasts.replaceDamage(ability.onHit(), beam.damageAt(active.level()),
                        beam.element()), attack, OptionalInt.empty())));
            }
            // SHATTER CAPS: one stone at the level's damage, then the weapon's own chunks, its own count.
            case EnchantBehaviour.Shatter caps -> {
                if (!(ability.cast() instanceof CastSpec.Projectile p)
                        || BehaviourCasts.shatter(ability.id(), p, one).isEmpty()) yield Optional.empty();
                yield Optional.of(rows(
                        row(BehaviourCasts.replaceDamage(ability.onHit(), caps.damageAt(active.level()), caps.element()),
                                attack, OptionalInt.empty()),
                        row(ability.onHit(), attack, WeaponLoreLines.deliveredShots(p))));
            }
            // AIRBURST FLAMES: the shot as authored (a hit before the split), then the bursts it splits into.
            case EnchantBehaviour.Airburst burst -> {
                Optional<CastSpec.Projectile> body = bodyOf(ability.cast());
                if (body.isEmpty() || BehaviourCasts.airburst(body.get(), one).isEmpty()) yield Optional.empty();
                int bodies = WeaponLoreLines.shotCount(ability.cast());
                yield Optional.of(rows(
                        row(ability.onHit(), attack, WeaponLoreLines.deliveredShots(ability.cast())),
                        row(BehaviourCasts.replaceDamage(ability.onHit(), burst.damageAt(active.level()), burst.element()),
                                attack, OptionalInt.of(burst.count() * bodies))));
            }
            // LAPIS CRYSTAL: the granted orb's block gains its shot, a real number (B5): the beam's damage x the percent.
            case EnchantBehaviour.GrantTrigger grant -> {
                if (!grant.ability().id().equals(ability.id()) || !(ability.cast() instanceof CastSpec.Orb orb)
                        || orb.shot() == null) yield Optional.empty();
                Optional<AbilityDefinition> beam = ownAbilities.stream().filter(a -> isRay(a.cast())).findFirst();
                if (beam.isEmpty()) yield Optional.empty();
                OptionalDouble shot = BehaviourCasts.orbShotAmount(beam.get().onHit(), orb.shot(), active.level());
                if (shot.isEmpty()) yield Optional.empty();
                List<DamageRow> rows = new ArrayList<>();
                row(ability.onHit(), attack, OptionalInt.empty()).ifPresent(rows::add);   // the orb's own hit, if it has one
                rows.add(new DamageRow(shot.getAsDouble(), orb.shot().element(), OptionalInt.empty()));
                yield Optional.of(List.copyOf(rows));
            }
            // CLUSTER BOMBS (F10, 2026-11-26): the big chunk at the level's damage, then the weapon's own chunks, as many as
            // split (its own count, never the weapon's spread). Both through the same resolvers the press uses.
            case EnchantBehaviour.ClusterBombs bombs -> {
                if (!(ability.cast() instanceof CastSpec.Projectile p)
                        || BehaviourCasts.clusterBombs(ability.id(), p, one).isEmpty()) yield Optional.empty();
                yield Optional.of(rows(
                        row(BehaviourCasts.clusterBig(ability.onHit(), bombs.damageAt(active.level()), bombs.element(),
                                bombs.radius()), attack, OptionalInt.empty()),
                        row(ability.onHit(), attack, OptionalInt.of(bombs.splitCount()))));
            }
            // A CAST TUNE changes the cast itself (CastTunes.tune, before this is asked): no block of its own.
            case EnchantBehaviour.CastTune ignored -> Optional.empty();
            // CONDITIONAL (T8): each acts only when hits land or on a target's state, so no line moves.
            case EnchantBehaviour.HitChain ignored -> Optional.empty();     // Wildfire: a chain of hits on one target
            case EnchantBehaviour.DrummingBeat ignored -> Optional.empty(); // Drumming Beat: a streak of charged hits (M9)
            case EnchantBehaviour.Shockwave ignored -> Optional.empty();    // Kinetic Tremors: three hits in a window
            case EnchantBehaviour.ScorchBoost ignored -> Optional.empty();  // Mana Burn: a scorch the hit applies
        };
    }

    private static Optional<CastSpec.Projectile> bodyOf(CastSpec cast) {
        if (cast instanceof CastSpec.Projectile p) return Optional.of(p);
        if (cast instanceof CastSpec.Volley v && v.of() instanceof CastSpec.Projectile p) return Optional.of(p);
        return Optional.empty();
    }

    private static boolean isRay(CastSpec cast) {
        return cast instanceof CastSpec.Ray || cast instanceof CastSpec.Volley v && v.of() instanceof CastSpec.Ray;
    }

    private static Optional<DamageRow> row(List<EffectSpec> onHit, double attack, OptionalInt count) {
        return DamagePayload.of(onHit, attack).map(d -> new DamageRow(d.amount(), d.element(), count));
    }

    @SafeVarargs
    private static List<DamageRow> rows(Optional<DamageRow>... rows) {
        List<DamageRow> out = new ArrayList<>();
        for (Optional<DamageRow> r : rows) r.ifPresent(out::add);
        return List.copyOf(out);
    }
}
