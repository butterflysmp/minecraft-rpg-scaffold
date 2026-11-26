package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.BehaviourCasts;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.CastTunes;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.Bulwark;
import io.github.butterflysmp.rpg.core.enchant.CritChanceEnchant;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.Growth;
import io.github.butterflysmp.rpg.core.enchant.ManaBank;
import io.github.butterflysmp.rpg.core.enchant.Protection;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.GrantedTriggers;
import io.github.butterflysmp.rpg.core.weapon.HeldWeaponCrit;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldLoreLines;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.ElementLoader;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.ShieldLoader;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T13, THE CONSISTENCY TEST (true tooltips, the seat's ruling of 2026-11-26): for EVERY shipped weapon, armor piece and
 * shield, EVERY enchant its four slots' pools admit, EVERY level, at TWO gear scores, the number on each tooltip line
 * equals the value the ENGINE resolves for that stack. Generated from content: no hand list.
 *
 * <p><b>The expectation is written from the engine's side, not by calling the renderer.</b> {@link #engineDamage}
 * walks the triggers the way {@code CastExecutor} fires them (the cast tune at commit; Focused Beam on a volley's ray;
 * Shatter Caps on a spread; Airburst on a body that reaches its split; a granted orb's shot) and prices each hit's ITEM
 * term with {@code HitDamage.hitBase} at the player's bonus 0, from the enchant curves themselves
 * ({@code damageAt}, {@code orbShotAmount}). Armor and shields are priced by the reconcile's own functions
 * ({@code Protection.effectiveDefense}, {@code Growth}/{@code ManaBank.contribution}, {@code Bulwark.effectiveDr}).
 *
 * <p><b>Nothing-else-moves:</b> where the engine says an enchant changes none of the item's numbers (Polished Scepter,
 * Early Toll, Deft Hands, Expanded Quiver, the conditional ones), the whole tooltip must equal the bare one. That is
 * Ben's "C" (range, wind-up, reload left alone) and T8, witnessed for every level of every such enchant.
 *
 * <p><b>What it does not see:</b> the enchant block (built by {@code EnchantLore.applied}, unchanged and pinned
 * elsewhere), the quiver line (the stamp, T10 withdrawn), and a BROKEN armor piece (the engine drops its Defense, Growth
 * and Mana Bank; its tooltip shows them, as it does today: the seat's ruling, recorded in the plan).
 */
class TrueTooltipsConsistencyTest {

    private static final Logger LOG = Logger.getLogger(TrueTooltipsConsistencyTest.class.getName());
    private static final int[] SCORES = {100, 250};
    /** PINNED: the count of cases this walk generates over shipped content. Recount, never adjust: see the plan. */
    static final int EXPECTED_CASES = 1302;   // weapons 612, armor 672, shields 18 (576 unmoved): 2026-11-26 + bone_club (sharpness 3 + unbreaking 3 + kinetic_tremors 3, x 2 scores = 18; unbreaking and kinetic_tremors 12 unmoved; 1284 at 305e1361); + cluster_bombs (3 levels x 2 scores, all moved); + fast_and_fungal (3 levels x 2 scores, all moved: the cooldown); + blast_fungus (attunement 3 + unbreaking 3, x 2 scores = 12; unbreaking 6 unmoved); 1260 at fcfbefbb
    private static final Pattern DAMAGE = Pattern.compile("^.* Damage: ([0-9.]+)( x ([0-9]+))?$");
    private static final Pattern CRIT = Pattern.compile("^Crit Chance: \\+([0-9]+)%$");

    record Content(ElementRegistry elements, List<WeaponDefinition> weapons, List<ArmorDefinition> armor,
                   List<ShieldDefinition> shields, EnchantRegistry enchants, List<EnchantRoll.Rollable> roster) {}

    static Content load() {
        String base = "src/main/resources/content/";
        ElementRegistry elements = new ElementLoader(LOG).loadAll(new File(base + "elements"));
        var weaponRegistry = new WeaponLoader(LOG).loadAll(new File(base + "weapons"));
        // THE LOADER THE SERVER USES (EnchantLoader.withWeapons): the plain constructor drops every weapon-specific and
        // element enchant (it read 11 of 24 here), which would leave the walk blind to the whole slice.
        EnchantRegistry enchants = EnchantLoader.withWeapons(LOG, id -> elements.find(id).isPresent(), weaponRegistry::find,
                false).loadAll(new File(base + "enchants"));
        List<WeaponDefinition> weapons = new ArrayList<>(weaponRegistry.all());
        List<ArmorDefinition> armor = new ArrayList<>(new ArmorLoader(LOG).loadAll(new File(base + "armor")).all());
        List<ShieldDefinition> shields = new ArrayList<>(new ShieldLoader(LOG).loadAll(new File(base + "shields")).all());
        weapons.sort((a, b) -> a.id().compareTo(b.id()));
        armor.sort((a, b) -> a.id().compareTo(b.id()));
        shields.sort((a, b) -> a.id().compareTo(b.id()));
        assertTrue(weapons.size() > 20, "CONTROL: the shipped weapons loaded, " + weapons.size());
        assertTrue(armor.size() > 10, "CONTROL: the shipped armor loaded, " + armor.size());
        assertFalse(shields.isEmpty(), "CONTROL: the shipped shields loaded");
        assertTrue(enchants.all().size() >= 20, "CONTROL: the shipped enchants loaded, " + enchants.all().size());
        return new Content(elements, weapons, armor, shields, enchants, EnchantRollItems.roster(enchants.all()));
    }

    /** Every enchant ANY of the item's four slots may hold: the union of the roll's own pools, sorted. */
    static List<String> poolsOf(GearDefinition gear, Content c) {
        TreeSet<String> ids = new TreeSet<>();
        var gearClass = GearItems.gearClassOf(gear);
        if (gearClass == null) return List.of();
        for (int slot = 0; slot < EnchantRoll.SLOTS; slot++) {
            for (EnchantRoll.Rollable r : EnchantRoll.poolForSlot(slot, EnchantRollItems.slotZeroKey(gear),
                    gear.gearElement(), EnchantRollItems.has(gear), gearClass, c.roster())) {
                ids.add(r.enchantId());
            }
        }
        return List.copyOf(ids);
    }

    /** One enchant, ACTIVE at {@code level}, in slot 0: the engine reads effective(), never the slot index. */
    static EnchantState activeAt(String enchantId, int level) {
        return EnchantState.empty().addCandidate(0, enchantId).withLevel(0, 0, level).withActive(0, 0);
    }

    static List<String> plain(List<Component> lore) {
        return lore.stream().map(l -> PlainTextComponentSerializer.plainText().serialize(l)).toList();
    }

    // ================================================================== the engine's side, weapons

    /** One damage the engine deals per press, the ITEM'S term: amount and the payloads one press delivers. */
    record Hit(double amount, int count) {}

    static List<Hit> engineDamage(WeaponDefinition w, EnchantState state, int score, EnchantRegistry enchants) {
        List<ActiveBehaviour> beh = BehaviourItems.activeOf(state, w.id(), enchants::find);
        double pct = DamageEnchantItems.percentOn(state, w, enchants);
        boolean scored = GearScore.carriesScore(GearItems.gearClassOf(w), w.unscored());
        java.util.function.DoubleUnaryOperator item =
                a -> HitDamage.hitBase(scored ? GearScore.scaledDamage(a, score) : a, pct, 0.0);

        List<AbilityDefinition> own = new ArrayList<>();
        for (TriggerBinding t : w.triggers()) own.add(CastTunes.tune(t.ability(), beh));   // the commit's tune
        List<AbilityDefinition> all = new ArrayList<>(own);
        for (TriggerBinding g : GrantedTriggers.granted(w, beh)) all.add(CastTunes.tune(g.ability(), beh));

        List<Hit> hits = new ArrayList<>();
        boolean basicPlaced = false;
        for (AbilityDefinition a : all) {
            if (DamagePayload.isBasicAttack(a.onHit())) {
                if (!basicPlaced) {
                    basicPlaced = true;
                    hits.add(new Hit(item.applyAsDouble(w.attackDamage()), 1));
                }
                continue;
            }
            Optional<Double> literal = firstLiteral(a.onHit());
            if (a.cast() instanceof CastSpec.Volley v && BehaviourCasts.focusedBeam(a.id(), v.of(), beh).isPresent()) {
                var active = BehaviourCasts.focusedBeam(a.id(), v.of(), beh).get();
                hits.add(new Hit(item.applyAsDouble(((EnchantBehaviour.FocusedBeam) active.behaviour()).damageAt(active.level())), 1));
            } else if (a.cast() instanceof CastSpec.Projectile p && BehaviourCasts.clusterBombs(a.id(), p, beh).isPresent()) {
                // CLUSTER BOMBS (F9, 2026-11-26): CastExecutor.launchCluster sets off ONE big chunk at the level's damage, then
                // splitCluster sets off splitCount of the weapon's own chunks, each its own literal.
                var active = BehaviourCasts.clusterBombs(a.id(), p, beh).get();
                var bombs = (EnchantBehaviour.ClusterBombs) active.behaviour();
                hits.add(new Hit(item.applyAsDouble(bombs.damageAt(active.level())), 1));
                literal.ifPresent(d -> hits.add(new Hit(item.applyAsDouble(d), bombs.splitCount())));
            } else if (a.cast() instanceof CastSpec.Projectile p && BehaviourCasts.shatter(a.id(), p, beh).isPresent()) {
                var active = BehaviourCasts.shatter(a.id(), p, beh).get();
                hits.add(new Hit(item.applyAsDouble(((EnchantBehaviour.Shatter) active.behaviour()).damageAt(active.level())), 1));
                literal.ifPresent(d -> hits.add(new Hit(item.applyAsDouble(d), p.spread().count())));
            } else if (bodyOf(a.cast()).isPresent() && BehaviourCasts.airburst(bodyOf(a.cast()).get(), beh).isPresent()) {
                var active = BehaviourCasts.airburst(bodyOf(a.cast()).get(), beh).get();
                var burst = (EnchantBehaviour.Airburst) active.behaviour();
                int bodies = payloads(a.cast());
                literal.ifPresent(d -> hits.add(new Hit(item.applyAsDouble(d), bodies)));
                hits.add(new Hit(item.applyAsDouble(burst.damageAt(active.level())), burst.count() * bodies));
            } else if (a.cast() instanceof CastSpec.Orb orb && orb.shot() != null) {
                literal.ifPresent(d -> hits.add(new Hit(item.applyAsDouble(d), 1)));
                int level = beh.stream().filter(b -> b.behaviour() instanceof EnchantBehaviour.GrantTrigger g
                        && g.ability().id().equals(a.id())).findFirst().map(ActiveBehaviour::level).orElse(1);
                own.stream().filter(o -> isRay(o.cast())).findFirst()
                        .flatMap(beam -> {
                            var amount = BehaviourCasts.orbShotAmount(beam.onHit(), orb.shot(), level);
                            return amount.isPresent() ? Optional.of(amount.getAsDouble()) : Optional.<Double>empty();
                        })
                        .ifPresent(d -> hits.add(new Hit(item.applyAsDouble(d), 1)));
            } else {
                literal.ifPresent(d -> hits.add(new Hit(item.applyAsDouble(d), payloads(a.cast()))));
            }
        }
        return hits;
    }

    /** The crit chance the stat holds while this stack is in hand: the weapon's own plus its enchant's contribution. */
    static double engineCrit(WeaponDefinition w, EnchantState state, EnchantRegistry enchants) {
        double own = HeldWeaponCrit.sources(Optional.of(w)).values().stream().mapToDouble(Double::doubleValue).sum();
        double pts = EnchantValues.totalFor(state, enchants, EnchantEffect.CRIT_CHANCE);
        return own + (CritChanceEnchant.boosts(pts) ? CritChanceEnchant.contribution(pts) : 0.0);
    }

    /**
     * The literal the Damage arm prices on a hit. A top-level damage; or, for a FUSED chunk's detonation (the Blast Fungus,
     * 2026-11-26), a burst's damage: READ, {@code EffectApplier.detonateFused} hands a burst's effects to the same
     * per-target loop and the same Damage arm, once per body in its radius.
     */
    /**
     * F8 (2026-11-26, Fast and Fungal): EVERY ability block's Cooldown line equals the ticks the press STAMPS, read from
     * the stamp's own function ({@code AbilityService.stampedCooldownTicks}, with this stack's active behaviours, at a
     * neutral attack speed), never from the renderer. A basic attack has no Cooldown line (its stat block), so it is
     * skipped; a free trigger with no cooldown prints none, and none is asked for.
     */
    /** Every trigger's stamped cooldown under {@code state}, the stamp's own function: what a cooldown tune moves. */
    static List<Integer> stamped(WeaponDefinition w, EnchantState state, EnchantRegistry enchants) {
        List<ActiveBehaviour> beh = BehaviourItems.activeOf(state, w.id(), enchants::find);
        List<Integer> out = new ArrayList<>();
        for (TriggerBinding t : w.triggers()) {
            out.add(io.github.butterflysmp.rpg.core.ability.AbilityService.stampedCooldownTicks(t.ability(), beh, 1.0));
        }
        return out;
    }

    static List<String> cooldownMismatches(String label, List<String> lore, WeaponDefinition w, EnchantState state,
                                           EnchantRegistry enchants) {
        List<ActiveBehaviour> beh = BehaviourItems.activeOf(state, w.id(), enchants::find);
        List<TriggerBinding> triggers = new ArrayList<>(w.triggers());
        triggers.addAll(GrantedTriggers.granted(w, beh));
        List<String> out = new ArrayList<>();
        for (TriggerBinding t : triggers) {
            if (DamagePayload.isBasicAttack(t.ability().onHit())) continue;
            int stamped = io.github.butterflysmp.rpg.core.ability.AbilityService.stampedCooldownTicks(t.ability(), beh, 1.0);
            if (stamped <= 0) continue;
            String want = "Cooldown: " + String.format(java.util.Locale.ROOT, "%.1fs", stamped / 20.0);
            if (lore.stream().noneMatch(l -> l.equals(want) || l.startsWith(want + " |"))) {
                out.add(label + ": " + t.ability().id() + " stamps " + stamped + " ticks, no line reads '" + want + "' " + lore);
            }
        }
        return out;
    }

    private static Optional<Double> firstLiteral(List<EffectSpec> onHit) {
        for (EffectSpec e : onHit) {
            if (e instanceof EffectSpec.Damage d) return Optional.of(d.amount());
            if (e instanceof EffectSpec.Burst b) {
                for (EffectSpec n : b.effects()) if (n instanceof EffectSpec.Damage d) return Optional.of(d.amount());
            }
        }
        return Optional.empty();
    }

    /** The payloads one press delivers, as the executor fires them: a volley's shots times its inner cast's own. */
    private static int payloads(CastSpec cast) {
        if (cast instanceof CastSpec.Volley v) return v.shots() * payloads(v.of());
        if (cast instanceof CastSpec.Projectile p && p.spread() != null) return p.spread().count();
        return 1;
    }

    private static Optional<CastSpec.Projectile> bodyOf(CastSpec cast) {
        if (cast instanceof CastSpec.Projectile p) return Optional.of(p);
        if (cast instanceof CastSpec.Volley v && v.of() instanceof CastSpec.Projectile p) return Optional.of(p);
        return Optional.empty();
    }

    private static boolean isRay(CastSpec cast) {
        return cast instanceof CastSpec.Ray || cast instanceof CastSpec.Volley v && v.of() instanceof CastSpec.Ray;
    }

    // ================================================================== the comparator

    /** Every disagreement between a weapon tooltip and the engine, or none. Its own control is {@link #theComparatorSeesAPlantedMismatch}. */
    /**
     * Ben's class labels (2026-11-26): every damage line's label is the weapon's CLASS word, from the weapon's class (the
     * label function's input), never its element. Its control is in {@link #theComparatorSeesAPlantedMismatch}.
     */
    static List<String> labelMismatches(String label, List<String> lore, WeaponDefinition w) {
        // FROM THE CLASS, NEVER FROM THE LABEL FUNCTION: a first draft read WeaponClassLabel.damageLabel here, and a
        // mutation of that function (every line "Melee") left this walk GREEN, because the expectation moved with it.
        String want = switch (w.weaponClass()) {
            case MELEE -> "Melee Damage: ";
            case RANGER -> "Ranged Damage: ";
            case MAGE -> "Magic Damage: ";
            case SUMMONER -> "Summoner Damage: ";
        };
        return lore.stream().filter(l -> DAMAGE.matcher(l).matches() && !l.startsWith(want))
                .map(l -> label + ": the line \"" + l + "\" is not labelled \"" + want + "\"").toList();
    }

    static List<String> weaponMismatches(String label, List<String> lore, List<Hit> hits, double crit,
                                         boolean hasStatBlock) {
        List<String> out = new ArrayList<>();
        List<Hit> shown = new ArrayList<>();
        Integer shownCrit = null;
        for (String line : lore) {
            Matcher m = DAMAGE.matcher(line);
            if (m.matches()) shown.add(new Hit(Double.parseDouble(m.group(1)), m.group(3) == null ? 1 : Integer.parseInt(m.group(3))));
            Matcher c = CRIT.matcher(line);
            if (c.matches()) shownCrit = Integer.parseInt(c.group(1));
        }
        if (shown.size() != hits.size()) {
            out.add(label + ": " + shown.size() + " damage lines shown, the engine deals " + hits.size() + " " + hits + " | " + lore);
        } else {
            for (int i = 0; i < hits.size(); i++) {
                if (Math.abs(shown.get(i).amount() - hits.get(i).amount()) > 0.005 + 1e-9 || shown.get(i).count() != hits.get(i).count()) {
                    out.add(label + ": line " + i + " shows " + shown.get(i) + ", the engine deals " + hits.get(i));
                }
            }
        }
        Integer expectCrit = hasStatBlock && crit > 0 ? (int) Math.round(crit * 100) : null;
        if (!java.util.Objects.equals(expectCrit, shownCrit)) {
            out.add(label + ": crit shown " + shownCrit + "%, the stat holds " + expectCrit + "%");
        }
        return out;
    }

    // ================================================================== the test

    @Test
    void everyTooltipNumberEqualsWhatTheEngineResolves() {
        Content c = load();
        List<String> mismatches = new ArrayList<>();
        int cases = 0, weaponCases = 0, armorCases = 0, shieldCases = 0, unmoved = 0;

        for (WeaponDefinition w : c.weapons()) {
            boolean statBlock = w.triggers().stream().anyMatch(t -> DamagePayload.isBasicAttack(t.ability().onHit()));
            for (int score : SCORES) {
                List<String> bare = plain(WeaponLore.build(w, c.elements(), OptionalInt.empty(), OptionalInt.empty(),
                        OptionalInt.of(score), ItemEnchantTermsReader.of(EnchantState.empty(), w, c.enchants())));
                List<Hit> bareHits = engineDamage(w, EnchantState.empty(), score, c.enchants());
                double bareCrit = engineCrit(w, EnchantState.empty(), c.enchants());
                mismatches.addAll(weaponMismatches(w.id() + " bare @" + score, bare, bareHits, bareCrit, statBlock));
                mismatches.addAll(labelMismatches(w.id() + " bare @" + score, bare, w));
                mismatches.addAll(cooldownMismatches(w.id() + " bare @" + score, bare, w, EnchantState.empty(), c.enchants()));
                for (String e : poolsOf(w, c)) {
                    EnchantDefinition def = c.enchants().find(e).orElseThrow();
                    for (int level = 1; level <= def.maxLevel(); level++) {
                        EnchantState state = activeAt(e, level);
                        List<String> lore = plain(WeaponLore.build(w, c.elements(), OptionalInt.empty(), OptionalInt.empty(),
                                OptionalInt.of(score), ItemEnchantTermsReader.of(state, w, c.enchants())));
                        List<Hit> hits = engineDamage(w, state, score, c.enchants());
                        double crit = engineCrit(w, state, c.enchants());
                        String label = w.id() + " + " + e + " " + level + " @" + score;
                        mismatches.addAll(weaponMismatches(label, lore, hits, crit, statBlock));
                        mismatches.addAll(labelMismatches(label, lore, w));
                        mismatches.addAll(cooldownMismatches(label, lore, w, state, c.enchants()));
                        boolean granted = !GrantedTriggers.granted(w, BehaviourItems.activeOf(state, w.id(), c.enchants()::find)).isEmpty();
                        // F8 (2026-11-26): and the cooldowns the press stamps, or a cooldown tune would read as moving nothing.
                        if (hits.equals(bareHits) && crit == bareCrit && !granted
                                && stamped(w, state, c.enchants()).equals(stamped(w, EnchantState.empty(), c.enchants()))) {
                            unmoved++;
                            if (!lore.equals(bare)) mismatches.add(label + ": the engine moves nothing, the tooltip moved " + lore);
                        }
                        cases++;
                        weaponCases++;
                    }
                }
            }
        }

        for (ArmorDefinition a : c.armor()) {
            for (int score : SCORES) {
                List<String> bare = plain(ArmorItems.statLore(a, EnchantState.empty(), c.enchants(), OptionalInt.of(score)));
                for (String e : poolsOf(a, c)) {
                    EnchantDefinition def = c.enchants().find(e).orElseThrow();
                    for (int level = 1; level <= def.maxLevel(); level++) {
                        EnchantState state = activeAt(e, level);
                        List<String> lore = plain(ArmorItems.statLore(a, state, c.enchants(), OptionalInt.of(score)));
                        String label = a.id() + " + " + e + " " + level + " @" + score;
                        double defense = Protection.effectiveDefense(GearScore.scaledDefense(a.defense(), score),
                                EnchantValues.totalFor(state, c.enchants(), EnchantEffect.DEFENSE));
                        expectLine(mismatches, label, lore, ArmorLoreLines.DEFENSE_LABEL + ArmorLoreLines.defenseValue(defense));
                        double health = EnchantValues.totalFor(state, c.enchants(), EnchantEffect.MAX_HEALTH);
                        expectBonus(mismatches, label, lore, ArmorLoreLines.MAX_HEALTH_LABEL,
                                Growth.boosts(health) ? Growth.contribution(health) : 0);
                        double mana = EnchantValues.totalFor(state, c.enchants(), EnchantEffect.MAX_MANA);
                        expectBonus(mismatches, label, lore, ArmorLoreLines.MAX_MANA_LABEL,
                                ManaBank.boosts(mana) ? ManaBank.contribution(mana) : 0);
                        boolean moves = EnchantValues.totalFor(state, c.enchants(), EnchantEffect.DEFENSE) > 0
                                || health > 0 || mana > 0;
                        if (!moves) {
                            unmoved++;
                            if (!lore.equals(bare)) mismatches.add(label + ": the engine moves nothing, the tooltip moved " + lore);
                        }
                        cases++;
                        armorCases++;
                    }
                }
            }
        }

        for (ShieldDefinition s : c.shields()) {
            for (int score : SCORES) {
                List<String> bare = plain(ShieldItems.statLore(s, EnchantState.empty(), c.enchants(), OptionalInt.of(score)));
                for (String e : poolsOf(s, c)) {
                    EnchantDefinition def = c.enchants().find(e).orElseThrow();
                    for (int level = 1; level <= def.maxLevel(); level++) {
                        EnchantState state = activeAt(e, level);
                        List<String> lore = plain(ShieldItems.statLore(s, state, c.enchants(), OptionalInt.of(score)));
                        String label = s.id() + " + " + e + " " + level + " @" + score;
                        double bulwark = EnchantValues.totalFor(state, c.enchants(), EnchantEffect.BLOCK_DR);
                        double dr = Bulwark.effectiveDr(s.blockDr(), bulwark);
                        expectLine(mismatches, label, lore, ShieldLoreLines.damageReductionLabel(dr));
                        if (bulwark == 0) {
                            unmoved++;
                            if (!lore.equals(bare)) mismatches.add(label + ": the engine moves nothing, the tooltip moved " + lore);
                        }
                        cases++;
                        shieldCases++;
                    }
                }
            }
        }

        System.out.println("TRUE TOOLTIPS T13: " + cases + " cases (weapons " + weaponCases + ", armor " + armorCases
                + ", shields " + shieldCases + "), " + unmoved + " where the engine moves nothing");
        mismatches.forEach(m -> System.out.println("T13 MISMATCH " + m));
        assertEquals(List.of(), mismatches, mismatches.size() + " tooltip lines disagree with the engine");
        assertEquals(EXPECTED_CASES, cases, "the generated count moved: recount it from this run's line and re-pin it");
    }

    private static void expectLine(List<String> out, String label, List<String> lore, String expected) {
        if (!lore.contains(expected)) out.add(label + ": expected the line \"" + expected + "\" in " + lore);
    }

    private static void expectBonus(List<String> out, String label, List<String> lore, String stat, double points) {
        List<String> found = lore.stream().filter(l -> l.startsWith(stat + ": ")).toList();
        if (points <= 0) {
            if (!found.isEmpty()) out.add(label + ": no " + stat + " granted, the tooltip shows " + found);
            return;
        }
        String want = stat + ": +" + io.github.butterflysmp.rpg.core.weapon.GearLoreLines.trimNumber(points);
        if (!found.equals(List.of(want))) out.add(label + ": expected \"" + want + "\", the tooltip shows " + found);
    }

    /**
     * THE SEAT'S FIVE EXAMPLES, pinned whole as plain text (2026-11-26), at score absent: what the report quotes is this
     * test's assertion, executed, never a hand-written rendering.
     */
    @Test
    void theSeatsExamplesReadAsQuoted() {
        Content c = load();
        java.util.function.BiFunction<String, EnchantState, List<String>> weapon = (id, state) -> {
            WeaponDefinition w = c.weapons().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
            return plain(WeaponLore.build(w, c.elements(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                    ItemEnchantTermsReader.of(state, w, c.enchants())));
        };
        assertEquals(List.of("Fire", "", "Fire Bolt  Right-Click", "Hurls a fragment of burning mana", "Magic Damage: 32",
                "Cooldown: 1.2s | Mana Cost: 5", "", "Early magicians learned you could",
                "create fire by striking flint with mana.", "", "Common Magic Weapon"),
                weapon.apply("flint_staff", activeAt("large_sparks", 3)));
        assertEquals(List.of("Kinetic", "", "Cursed Volley  Right-Click", "Channel, then loose six",
                "bolts down your line of sight.", "Magic Damage: 27 x 6", "Cooldown: 1.5s | Mana Cost: 40", "",
                "Nine facets, and every one of them looking back.", "It asks for a moment before it answers.", "",
                "Uncommon Magic Weapon"), weapon.apply("cursed_emerald", activeAt("echo_beam", 3)));
        assertEquals(List.of("Kinetic", "", "Cursed Volley  Right-Click", "Channel, then loose one focused",
                "beam down your line of sight.", "Magic Damage: 162", "Cooldown: 1.5s | Mana Cost: 40", "",
                "Nine facets, and every one of them looking back.", "It asks for a moment before it answers.", "",
                "Uncommon Magic Weapon"), weapon.apply("cursed_emerald", activeAt("focused_beam", 2)));
        assertEquals(List.of("Kinetic", "", "Melee Damage: 18", "Attack Speed: 2.0", "Crit Chance: +25%", "",
                "Common Melee Weapon"), weapon.apply("broken_bottle", activeAt("sharpened_glass", 3)));
        ArmorDefinition chest = c.armor().stream().filter(a -> a.id().equals("iron_chestplate")).findFirst().orElseThrow();
        List<String> armorLore = plain(ArmorItems.statLore(chest, activeAt("protection", 3), c.enchants(), OptionalInt.empty()));
        assertEquals("Defense: 25", armorLore.get(0), armorLore.toString());
    }

    /** THE PLANTED CONTROL: the comparator must report a tooltip whose number is off by one from the engine's. */
    @Test
    void theComparatorSeesAPlantedMismatch() {
        Content c = load();
        WeaponDefinition flint = c.weapons().stream().filter(w -> w.id().equals("flint_staff")).findFirst().orElseThrow();
        EnchantState sparks = activeAt("large_sparks", 3);
        List<String> lore = plain(WeaponLore.build(flint, c.elements(), OptionalInt.empty(), OptionalInt.empty(),
                OptionalInt.of(100), ItemEnchantTermsReader.of(sparks, flint, c.enchants())));
        List<Hit> truth = engineDamage(flint, sparks, 100, c.enchants());
        assertEquals(List.of(), weaponMismatches("truth", lore, truth, 0, false), "the control's baseline agrees");
        List<Hit> planted = List.of(new Hit(truth.get(0).amount() + 1, truth.get(0).count()));
        assertEquals(1, weaponMismatches("planted", lore, planted, 0, false).size(), "a planted +1 must be seen");
        List<Hit> plantedCount = List.of(new Hit(truth.get(0).amount(), 2));
        assertEquals(1, weaponMismatches("planted", lore, plantedCount, 0, false).size(), "a planted count must be seen");
        List<String> bare = plain(WeaponLore.build(flint, c.elements(), OptionalInt.empty(), OptionalInt.empty(),
                OptionalInt.of(100), ItemEnchantTerms0.none()));
        assertEquals(1, weaponMismatches("stale", bare, truth, 0, false).size(), "the pre-slice tooltip is a mismatch");
        assertEquals(List.of(), labelMismatches("truth", lore, flint), "the control's labels agree");
        List<String> elementLabelled = lore.stream().map(l -> l.replace("Magic Damage: ", "Fire Damage: ")).toList();
        assertEquals(1, labelMismatches("planted", elementLabelled, flint).size(), "an element label must be seen");
    }

    /** A name for the unenchanted terms that reads as what it is in the control above. */
    private static final class ItemEnchantTerms0 {
        static io.github.butterflysmp.rpg.core.weapon.ItemEnchantTerms none() {
            return io.github.butterflysmp.rpg.core.weapon.ItemEnchantTerms.NONE;
        }
    }
}
