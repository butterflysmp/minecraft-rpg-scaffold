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
        int quiverSize,
        int reloadTicks,
        List<TriggerBinding> triggers,
        List<String> flavor,
        Optional<String> craftResult,
        boolean unscored,
        String ammo
) implements GearDefinition {
    /** The item a weapon renders as when its content does not say otherwise: a sword. */
    public static final String DEFAULT_MATERIAL = "iron_sword";

    /**
     * A weapon with no magazine, which is every weapon shipped before the quiver existed.
     *
     * <p>Named rather than written as a bare 0 because it appears in every convenience constructor,
     * where a literal would read as a capacity someone chose. The same 0-is-absent convention
     * {@code attack_damage}, {@code attack_speed} and {@code sweep} already use on this record.
     */
    public static final int NO_QUIVER = 0;

    /**
     * The item a reload takes from the inventory when the content names none: a plain arrow.
     *
     * <p>THE AMMUNITION IS PER WEAPON (the seat's ruling 1, 2026-11-22, the Breacher's brief). One key, {@code ammo:},
     * naming a vanilla item; absent, this default, so every weapon file shipped before the key existed reloads exactly
     * as it did. A token, like {@link #material}: core never resolves it, paper does ({@code QuiverAmmo.ammoOf}).
     */
    public static final String DEFAULT_AMMO = "arrow";

    /** The trigger a vanilla-driven bow or crossbow binds its hit to (see {@link #validateShot}). No input names it. */
    public static final String SHOT_TRIGGER = "shot";

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
        // THE QUIVER. quiver_size 0 means this weapon has no magazine, which is how every weapon
        // shipped before the quiver existed simply has none -- there is no opt-in flag and no
        // exclusion list. Negative is a content bug.
        if (quiverSize < 0) throw new IllegalArgumentException("weapon '" + id + "' quiver_size must be >= 0, got: " + quiverSize);
        if (reloadTicks < 0) throw new IllegalArgumentException("weapon '" + id + "' reload_ticks must be >= 0, got: " + reloadTicks);
        // A reload with nothing to reload can never fire, and a quiver with no reload empties once
        // and is inert forever. Both are silent no-ops on an authored mechanical axis, which is the
        // failure the attack_speed and sweep guards above exist for -- so they are named here on
        // exactly that precedent rather than left to be discovered in play.
        if (reloadTicks > 0 && quiverSize == 0) {
            throw new IllegalArgumentException("weapon '" + id
                    + "' declares reload_ticks but no quiver_size, so it can never reload");
        }
        if (quiverSize > 0 && reloadTicks <= 0) {
            throw new IllegalArgumentException("weapon '" + id
                    + "' declares quiver_size but no reload_ticks, so its magazine could never be refilled");
        }
        // THE INPUT COLLISION. A quiver weapon spends left-click on its reload, so a left_click
        // trigger on the same weapon would have two owners for one press and whichever ran first
        // would win silently. hunters_bow.yml (deleted 2026-11) reserves left-click for "a future melee/special"; a
        // quiver weapon is the case that spends it, and that has to be a refusal rather than a race.
        if (quiverSize > 0 && bindsInput(triggers, "left_click")) {
            throw new IllegalArgumentException("weapon '" + id
                    + "' declares a quiver, which binds left_click to the reload, so it may not also"
                    + " declare a left_click trigger");
        }
        validateShot(id, material, triggers);
        // THE AMMUNITION: absent (null) is the arrow, as every shipped file had it. Normalised like a material token
        // (lower case, "minecraft:" dropped). An item to load with and NOTHING to load it into is the reload_ticks
        // refusal above in another key: an authored mechanical value that could never act, so it is named.
        String ammoToken = CraftResultToken.token(ammo);
        if (ammoToken != null && ammoToken.isEmpty()) {
            throw new IllegalArgumentException("weapon '" + id + "' declares a blank ammo; name an item or remove the key");
        }
        if (ammoToken != null && quiverSize == 0 && !ammoToken.equals(DEFAULT_AMMO)) {
            throw new IllegalArgumentException("weapon '" + id + "' declares ammo '" + ammoToken
                    + "' but no quiver_size, so nothing ever loads it");
        }
        ammo = ammoToken == null ? DEFAULT_AMMO : ammoToken;
        triggers = List.copyOf(triggers);
        // Optional authored prose for the tooltip -- absent is empty, never null.
        flavor = flavor == null ? List.of() : List.copyOf(flavor);
        craftResult = CraftResultToken.normalise(craftResult, "weapon", id);
    }

    /**
     * The canonical shape WITHOUT the {@code ammo} key (2026-11-22): every caller written before per-weapon ammunition,
     * which reloads from arrows exactly as it did. The loader is the one caller that passes an ammo.
     */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            WeaponClass weaponClass, String material, double attackDamage,
                            double attackSpeed, double sweep, int quiverSize, int reloadTicks,
                            List<TriggerBinding> triggers, List<String> flavor,
                            Optional<String> craftResult, boolean unscored) {
        this(id, displayName, element, rarity, weaponClass, material, attackDamage, attackSpeed,
                sweep, quiverSize, reloadTicks, triggers, flavor, craftResult, unscored, null);
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
                sweep, NO_QUIVER, 0, triggers, flavor, Optional.empty(), false, DEFAULT_AMMO);
    }

    /**
     * Every shape above WITH a quiver, and without a craft-result claim.
     *
     * <p>A separate overload rather than widening the one above, for the reason that one exists:
     * the canonical constructor already takes fourteen arguments and most weapons carry no magazine
     * at all. A caller that wants a quiver says so by choosing this constructor, which reads better
     * at the call site than two more zeroes on every weapon that has none.
     */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            WeaponClass weaponClass, String material, double attackDamage,
                            double attackSpeed, double sweep, int quiverSize, int reloadTicks,
                            List<TriggerBinding> triggers, List<String> flavor) {
        this(id, displayName, element, rarity, weaponClass, material, attackDamage, attackSpeed,
                sweep, quiverSize, reloadTicks, triggers, flavor, Optional.empty(), false, DEFAULT_AMMO);
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

    /**
     * Does this trigger list bind {@code input}?
     *
     * Static and list-taking for {@link #hasVanillaMeleeTrigger}'s reason: the compact constructor
     * needs it before the components are assigned, so {@link #trigger(String)} cannot be called.
     */
    /**
     * THE {@code shot} TRIGGER (2026-11-22, the seat's Part 2): the hit of a weapon vanilla draws or loads and shoots. No
     * input names it, so nothing of ours ever casts it; its on_hit lands where vanilla's arrow lands. Every shape that
     * would bind it to nothing, or let it fight vanilla's own use of the item, is refused by name:
     * <ul>
     *   <li>a {@code type: vanilla} cast anywhere but under {@code shot} (an input would cast a shot that never comes);</li>
     *   <li>{@code shot} with any other cast (its hit would never land: no arrow of ours carries it);</li>
     *   <li>{@code shot} on a material vanilla does not shoot ({@code bow}, {@code crossbow});</li>
     *   <li>{@code shot} beside {@code right_click} (our right-click cancels vanilla's use, so the bow would never draw)
     *       or beside {@code draw} (PlumeDraw would take the draw over).</li>
     * </ul>
     */
    private static void validateShot(String id, String material, List<TriggerBinding> triggers) {
        boolean shot = false;
        for (TriggerBinding binding : triggers) {
            boolean vanilla = binding.ability().cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Vanilla;
            boolean named = binding.input().equals(SHOT_TRIGGER);
            if (vanilla && !named) {
                throw new IllegalArgumentException("weapon '" + id + "' binds a 'type: vanilla' cast to '"
                        + binding.input() + "'; it belongs only under the '" + SHOT_TRIGGER + "' trigger");
            }
            if (named && !vanilla) {
                throw new IllegalArgumentException("weapon '" + id + "' declares a '" + SHOT_TRIGGER
                        + "' trigger whose cast is not cast type vanilla, so its hit could never land");
            }
            shot |= named;
        }
        if (!shot) return;
        String token = CraftResultToken.token(material);
        if (!"bow".equals(token) && !"crossbow".equals(token)) {
            throw new IllegalArgumentException("weapon '" + id + "' declares a '" + SHOT_TRIGGER
                    + "' trigger on material '" + material + "'; only a bow or crossbow is shot by vanilla");
        }
        for (String forbidden : List.of("right_click", "draw")) {
            if (bindsInput(triggers, forbidden)) {
                throw new IllegalArgumentException("weapon '" + id + "' declares a '" + SHOT_TRIGGER
                        + "' trigger beside a '" + forbidden + "' trigger, which would take vanilla's own draw away");
            }
        }
    }

    /** The {@code shot} trigger's ability, when this weapon is one vanilla draws or loads and shoots. */
    public Optional<AbilityDefinition> vanillaShotTrigger() {
        return trigger(SHOT_TRIGGER).map(TriggerBinding::ability);
    }

    private static boolean bindsInput(List<TriggerBinding> triggers, String input) {
        for (TriggerBinding binding : triggers) {
            if (binding.input().equals(input)) return true;
        }
        return false;
    }

    /**
     * Does this weapon carry a magazine that empties and must be reloaded?
     *
     * <p>The one question every quiver call site asks, so the 0-is-absent convention is read in
     * exactly one place rather than re-expressed as {@code quiverSize() > 0} at each of them. Same
     * job {@code SweepShare.sweeps} does for the sweep fraction, and for the same reason: a
     * convention spelled out at N sites is a convention that will be spelled differently at one.
     */
    public boolean hasQuiver() {
        return quiverSize > NO_QUIVER;
    }

    /**
     * Does this weapon reload at all? The QUANTITY deft_hands reads exists (the seat's ruling H, 2026-11-22). A presence
     * question, not the duration: the duration is read only where {@code QuiversSignatureTest} lists.
     */
    public boolean hasReload() {
        return reloadTicks > 0;
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
     * melee basic is bound to is content's business, and {@code hunters_bow} (deleted 2026-11) already proves a basic
     * attack need not live on the obvious input. No shipped weapon declares two.
     */
    public Optional<AbilityDefinition> vanillaMeleeTrigger() {
        for (TriggerBinding binding : triggers) {
            if (BasicMelee.isVanillaDriven(binding.ability())) return Optional.of(binding.ability());
        }
        return Optional.empty();
    }
}
