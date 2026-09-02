package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.BasicMelee;
import io.github.butterflysmp.rpg.core.combat.SweepShare;

import java.util.List;
import java.util.Optional;

/**
 * One weapon, fully described. Constructed from YAML by the content loader in the
 * paper module -- core never reads files.
 *
 * A weapon is a container of triggers. element and rarity are inert reserved data
 * in Phase 1: element flavors a kit and gates use in Phase 3, rarity parameterizes
 * a loot roll in Phase 4. Here they only color the item name. element is mandatory
 * and never null -- an unflavored weapon is KINETIC, not absent.
 *
 * material is the item the weapon is carried in ("iron_sword", "bow", ...), an opaque
 * presentation string core never interprets -- paper resolves it to a Bukkit Material,
 * exactly as it resolves the MiniMessage displayName. The bow needs a non-sword item, so
 * this became a field; every sword-shaped weapon leaves it at the DEFAULT_MATERIAL.
 */
public record WeaponDefinition(
        String id,
        String displayName,
        String element,
        Rarity rarity,
        WeaponClass weaponClass,
        String material,
        double attackDamage,
        double attackSpeed,
        double sweep,
        List<TriggerBinding> triggers,
        List<String> flavor,
        Optional<String> craftResult
) implements GearDefinition {
    /** The item a weapon renders as when its content does not say otherwise: a sword. */
    public static final String DEFAULT_MATERIAL = "iron_sword";

    public WeaponDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("weapon id required");
        if (element == null || element.isBlank()) throw new IllegalArgumentException("weapon element required (use kinetic, never absent)");
        if (rarity == null) throw new IllegalArgumentException("weapon rarity required");
        // A required mechanical axis -- future class-typed modifiers key on it, so a silent default
        // would misapply them. The loader rejects a missing/bad class, so this never fires in
        // production; it guards the convenience constructors and any future direct caller.
        if (weaponClass == null) throw new IllegalArgumentException("weapon '" + id + "' class required (melee, ranger, mage)");
        if (material == null || material.isBlank()) throw new IllegalArgumentException("weapon material required");
        // Attack damage is a stat the basic melee hit reads (via WeaponDamage). 0 is legal -- a
        // ranged/costed weapon (bow, staff) has no melee and declares none; negative is a content bug.
        if (attackDamage < 0) throw new IllegalArgumentException("weapon '" + id + "' attack_damage must be >= 0, got: " + attackDamage);
        // Attack speed is the weapon's cadence in ATTACKS PER SECOND, driving vanilla's
        // attack-strength period directly (1.6 is every vanilla sword). 0 is legal and means "not
        // declared" -- a ranged or costed weapon has no melee cadence to state; negative is a bug.
        if (attackSpeed < 0) throw new IllegalArgumentException("weapon '" + id + "' attack_speed must be >= 0, got: " + attackSpeed);
        if (triggers == null || triggers.isEmpty()) {
            throw new IllegalArgumentException("weapon '" + id + "' has no triggers");
        }
        // But a weapon whose basic hit VANILLA delivers must declare one, because the failure is
        // otherwise silent and was measured on the 2026-08-28 boot: no authored speed means no
        // reconciled modifier, which leaves the player's base 4.0 -- a 5-tick charge period inside
        // a 10-tick i-frame window, where every allowed swing is already fully charged and
        // AttackCharge is dead code. Gated on the SAME condition WeaponItems.mint uses to decide a
        // weapon is vanilla-driven, so the item's attributes and this validation cannot disagree --
        // and so the convenience constructors below (attack damage 0.0) never trip it.
        if (attackDamage > 0 && attackSpeed <= 0 && hasVanillaMeleeTrigger(triggers)) {
            throw new IllegalArgumentException("weapon '" + id
                    + "' has a vanilla-driven melee trigger, so attack_speed must be > 0, got: " + attackSpeed);
        }
        // The SWEEP fraction: what a bystander caught by vanilla's sweeping swing takes, as a share
        // of the number the primary target took. Absent (0) means this weapon does not sweep, which
        // is how a non-blade simply has none -- there is no hard-coded exclusion list anywhere.
        // Negative is a content bug.
        if (sweep < 0) throw new IllegalArgumentException("weapon '" + id + "' sweep must be >= 0, got: " + sweep);
        // A declared sweep with nothing to sweep FROM can never fire, so it is named rather than
        // silently ignored -- the same standing as the attack_speed guard above, and for the same
        // reason: a silent no-op on an authored mechanical axis is the failure this project keeps
        // writing guards against. Asks the SAME predicate mint, meleeCadence and that guard ask, so
        // a weapon cannot validate as sweeping here and then find no trigger to sweep from there.
        if (SweepShare.sweeps(sweep) && !hasVanillaMeleeTrigger(triggers)) {
            throw new IllegalArgumentException("weapon '" + id
                    + "' declares sweep but has no vanilla-driven melee trigger, so it can never sweep");
        }
        triggers = List.copyOf(triggers);
        // Optional authored prose for the tooltip -- absent is empty, never null.
        flavor = flavor == null ? List.of() : List.copyOf(flavor);
        craftResult = CraftResultToken.normalise(craftResult, "weapon", id);
    }

    /**
     * Every shape above, without a craft-result claim. Most gear does not make one, and this keeps
     * the ten-argument canonical constructor from reaching every existing caller and test.
     */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            WeaponClass weaponClass, String material, double attackDamage,
                            double attackSpeed, double sweep, List<TriggerBinding> triggers,
                            List<String> flavor) {
        this(id, displayName, element, rarity, weaponClass, material, attackDamage, attackSpeed,
                sweep, triggers, flavor, Optional.empty());
    }

    /** A sword-shaped MELEE weapon with no declared attack damage: the shape older tests use. */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            List<TriggerBinding> triggers) {
        this(id, displayName, element, rarity, WeaponClass.MELEE, DEFAULT_MATERIAL, 0.0, 0.0, SweepShare.NONE, triggers, List.of());
    }

    /** A MELEE weapon with an explicit material but no declared attack damage (kept for existing callers). */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            String material, List<TriggerBinding> triggers) {
        this(id, displayName, element, rarity, WeaponClass.MELEE, material, 0.0, 0.0, SweepShare.NONE, triggers, List.of());
    }

    /**
     * The cadence this weapon should actually pace a wielder's swings at, or 0.0 if it has no melee
     * hit for vanilla to deliver.
     *
     * <p>{@link #attackSpeed()} is what content DECLARED; this is what it MEANS. The two differ for
     * a weapon that authors a speed but carries no vanilla-driven melee trigger, where the declared
     * number governs nothing and must not reach the wielder's attribute -- writing it would pace a
     * staff or a bow as though it were a sword.
     *
     * <p>0.0 is the ABSENT signal the attribute override reads as "write no modifier", never a
     * speed to write. Gating here rather than at the paper call site is what makes the rule
     * testable: the resolution around it needs a live Player and can only be boot-witnessed.
     */
    public double meleeCadence() {
        return vanillaMeleeTrigger().isPresent() ? attackSpeed : 0.0;
    }

    /**
     * Does this trigger list contain a hit vanilla's crosshair attack delivers?
     *
     * Static, and taking the list rather than reading the field, because the compact constructor
     * needs it before the record's components are assigned -- {@link #vanillaMeleeTrigger()} cannot
     * be called from there. Both go through {@link BasicMelee#isVanillaDriven}, so they answer the
     * same question.
     */
    private static boolean hasVanillaMeleeTrigger(List<TriggerBinding> triggers) {
        for (TriggerBinding binding : triggers) {
            if (BasicMelee.isVanillaDriven(binding.ability())) return true;
        }
        return false;
    }

    /** The binding fired by this input, if the weapon has one. */
    public Optional<TriggerBinding> trigger(String input) {
        for (TriggerBinding binding : triggers) {
            if (binding.input().equals(input)) return Optional.of(binding);
        }
        return Optional.empty();
    }

    /**
     * The trigger a VANILLA crosshair attack now delivers, if this weapon has one.
     *
     * <p>One resolution shared by everything that needs to ask: {@code WeaponItems.mint} (which
     * attributes to pin), and the melee rider (which payload to land). Both go through
     * {@link BasicMelee#isVanillaDriven}, so a weapon cannot be minted as a vanilla-driven melee
     * weapon and then fail to resolve one at hit time, or the reverse.
     *
     * <p>FIRST-WINS across the trigger list rather than keyed on {@code "left_click"}: the input a
     * melee basic is bound to is content's business, and {@code hunters_bow} already proves a basic
     * attack need not live on the obvious input. No shipped weapon declares two.
     */
    public Optional<AbilityDefinition> vanillaMeleeTrigger() {
        for (TriggerBinding binding : triggers) {
            if (BasicMelee.isVanillaDriven(binding.ability())) return Optional.of(binding.ability());
        }
        return Optional.empty();
    }
}
