package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import io.github.butterflysmp.rpg.core.weapon.CraftResultToken;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Checks, once at startup, that every cross-reference between content files
 * actually resolves. Without this a typo in the 200th ability's visual_id is not
 * discovered until a player casts it, and then it is a silent no-visual plus one
 * runtime log line nobody reads.
 *
 * Warns; never throws. A typo in the 400th weapon must not take the server down.
 * The ability still loads and still deals its damage -- it just says so out loud.
 *
 * The registry lookups (does this potion effect exist? does this sound exist?)
 * need a live Bukkit Registry, so they arrive as predicates rather than being
 * called directly. That keeps the interesting logic -- the walk -- unit-testable
 * with no server, which is the same trade the loaders make.
 */
public final class ContentValidator {

    private final VisualRegistry visuals;
    private final StatusRegistry statuses;
    private final ElementRegistry elements;
    private final Predicate<NamespacedKey> potionEffectExists;
    private final Predicate<NamespacedKey> soundExists;

    public ContentValidator(VisualRegistry visuals, StatusRegistry statuses, ElementRegistry elements,
                            Predicate<NamespacedKey> potionEffectExists,
                            Predicate<NamespacedKey> soundExists) {
        this.visuals = visuals;
        this.statuses = statuses;
        this.elements = elements;
        this.potionEffectExists = potionEffectExists;
        this.soundExists = soundExists;
    }

    /** @return every problem found, each naming the file or id at fault. Empty is good. */
    public List<String> validate(AbilityRegistry abilities) {
        List<String> problems = new ArrayList<>();
        for (AbilityDefinition ability : abilities.all()) {
            checkElement(ability.element(), "ability '" + ability.id() + "'", problems);
            checkCast(ability.cast(), "ability '" + ability.id() + "'", problems);
            for (EffectSpec effect : ability.onHit()) {
                checkEffect(effect, "ability '" + ability.id() + "'", problems);
            }
            // on_cast dangles a visual_id exactly the way on_hit does, and is exactly as invisible
            // when it does: the cast still fires, it just makes no noise.
            for (EffectSpec effect : ability.onCast()) {
                checkEffect(effect, "ability '" + ability.id() + "' on_cast", problems);
            }
        }
        for (StatusDefinition status : statuses.all()) {
            if (status instanceof StatusDefinition.Potion potion
                    && !potionEffectExists.test(potion.potionType())) {
                problems.add("status '" + potion.id() + "' names potion_type '"
                        + potion.potionType() + "', which is not a potion effect");
            }
        }
        for (VisualDefinition visual : visuals.all()) {
            for (VisualSpec step : visual.steps()) {
                if (step instanceof VisualSpec.Sound sound && !soundExists.test(sound.namespacedKey())) {
                    problems.add("visual '" + visual.id() + "' names sound '"
                            + sound.key() + "', which is not a sound event");
                }
            }
        }
        return problems;
    }

    /**
     * The kit -> ability/weapon/element cross-reference. A kit is a (class, element) cell that
     * grants weapons and abilities; each grant fails most invisibly -- a dangling ability in a
     * kit is a permission gap that looks like intended design, a dangling weapon is a class you
     * pick and get nothing to swing.
     *
     * Problems reported per kit:
     *   - its element, if no element defines it (the same checkElement seam as damage);
     *   - each ability id no ability declares, and each weapon id no weapon declares;
     *   - a kit whose RESOLVED (existing-only) grants are zero -- a cell nobody can play. A
     *     per-id check alone passes that: every remaining id is fine because none remain.
     *
     * The existence checks arrive as predicates so the walk is unit-testable with no registries
     * and no server, exactly as the archetype check it replaces did.
     *
     * @return every problem found, each naming the kit at fault. Empty is good.
     */
    public List<String> validateKits(Collection<KitDefinition> kits,
                                     Predicate<String> abilityExists,
                                     Predicate<String> weaponExists) {
        List<String> problems = new ArrayList<>();
        for (KitDefinition kit : kits) {
            String label = "kit '" + kit.classId() + "/" + kit.elementId() + "'";
            checkElement(kit.elementId(), label, problems);

            int resolved = 0;
            for (String abilityId : kit.abilityIds()) {
                if (abilityExists.test(abilityId)) {
                    resolved++;
                } else {
                    problems.add(label + " grants ability '" + abilityId
                            + "', which no ability defines");
                }
            }
            for (WeaponGrant grant : kit.weapons()) {
                if (weaponExists.test(grant.weaponId())) {
                    resolved++;
                } else {
                    problems.add(label + " grants weapon '" + grant.weaponId()
                            + "', which no weapon defines");
                }
            }
            if (resolved == 0) {
                problems.add(label + " grants nothing that exists -- nobody can play this cell");
            }
        }
        return problems;
    }

    /**
     * The weapon -> visual/status cross-reference, the same walk as abilities. A weapon's
     * triggers are ability bodies, so their on_hit effects can dangle a visual_id or
     * status_id exactly the way an ability can, and are checked the identical way. The
     * owner label names the weapon AND the trigger, so the warning points at the file and
     * the input, not just "somewhere in ironblade".
     *
     * @return every problem found, each naming the weapon and trigger at fault. Empty is good.
     */
    public List<String> validateWeapons(Collection<WeaponDefinition> weapons) {
        List<String> problems = new ArrayList<>();
        for (WeaponDefinition weapon : weapons) {
            checkElement(weapon.element(), "weapon '" + weapon.id() + "'", problems);
            for (TriggerBinding binding : weapon.triggers()) {
                String label = "weapon '" + weapon.id() + "' trigger '" + binding.input() + "'";
                checkCast(binding.ability().cast(), label, problems);
                for (EffectSpec effect : binding.ability().onHit()) {
                    checkEffect(effect, label, problems);
                }
                for (EffectSpec effect : binding.ability().onCast()) {
                    checkEffect(effect, label + " on_cast", problems);
                }
            }
        }
        return problems;
    }
    /**
     * An enchant's {@code icon} must name a real Material.
     *
     * <p>The loader cannot check this and deliberately does not try: resolving a name to a
     * {@code Material} needs the Bukkit registry, and {@link EnchantDefinition} is kept free of
     * Bukkit so it stays unit-testable. So the check arrives as a predicate seam, exactly as the
     * potion, sound and entity checks do.
     *
     * <p><b>Why this is worth a boot warning at all.</b> A misspelled icon is the most INVISIBLE
     * content typo in the repo: it does not throw, it does not skip the file, and the enchant keeps
     * working perfectly -- it just renders as the fallback book, which is a picture nobody can tell
     * apart from a deliberate choice. The same instinct that makes a dangling visual_id a named
     * warning rather than a silent no-visual.
     *
     * @param materialExists does this name resolve to a Material
     * @return every problem found, each naming the enchant at fault. Empty is good.
     */
    public List<String> validateEnchants(Collection<EnchantDefinition> enchants,
                                         Predicate<String> materialExists) {
        List<String> problems = new ArrayList<>();
        for (EnchantDefinition enchant : enchants) {
            if (!materialExists.test(enchant.icon())) {
                problems.add("enchant '" + enchant.id() + "' names icon '" + enchant.icon()
                        + "', which is not a material; it will render as "
                        + EnchantDefinition.DEFAULT_ICON);
            }
        }
        return problems;
    }


    /**
     * A {@code craft_result} must name a real, DURABLE material, and must equal its own
     * {@code material}.
     *
     * <p><b>Both mistakes are completely silent at runtime</b>, which is what puts them here rather
     * than anywhere else. This is the same argument {@code ArmorConsistency} makes for the same
     * reason -- a defense mismatch "is invisible from every single vantage point ... nothing throws,
     * nothing logs, no test can see it ... the ONLY moment the two numbers are in the same JVM is
     * boot" -- and these two are that shape exactly. Read them as one pattern with it, not as two
     * new inventions.
     *
     * <p><b>1. It must EQUAL the definition's own material.</b> The mint builds the item from
     * {@code material()}, not from what was crafted: {@code ArmorItems.mint} does
     * {@code new ItemStack(materialOf(armor.material(), armor.slot()))}. So a definition claiming
     * {@code iron_chestplate} while rendering as {@code diamond_chestplate} means the player crafts
     * iron and RECEIVES DIAMOND. There is no legitimate reason for the two to differ -- the crafted
     * item must look like the thing you crafted, or the recipe lies -- so this is refused rather
     * than gated.
     *
     * <p><b>2. It must be DURABLE.</b> The index is opt-in, so a claim on a material with no
     * durability -- {@code ability_stone} is {@code amethyst_shard} -- would index perfectly
     * cleanly and then be dropped by the mint's durability gate, every time, forever. No error, no
     * warning, no mint: the author sees nothing at all. Named here instead.
     *
     * <p>Both arrive as predicate seams for the reason every check in this class does: resolving a
     * name to a {@code Material}, and asking that Material its maximum durability, both need the
     * Bukkit registry and a running server. {@code Material.getMaxDurability()} in particular throws
     * {@code ExceptionInInitializerError} headless. The WALK stays unit-testable.
     *
     * @param materialExists    does this name resolve to a Material at all
     * @param materialIsDurable does it resolve to one with durability (max &gt; 0)
     * @return every problem found, each naming the definition at fault. Empty is good.
     */
    public List<String> validateCraftResults(Collection<? extends GearDefinition> gear,
                                             Predicate<String> materialExists,
                                             Predicate<String> materialIsDurable) {
        List<String> problems = new ArrayList<>();
        for (GearDefinition definition : gear) {
            Optional<String> claim = definition.craftResult();
            if (claim.isEmpty()) continue;
            String result = claim.get();

            if (!materialExists.test(result)) {
                problems.add("gear '" + definition.id() + "' claims craft_result '" + result
                        + "', which is not a material; it can never be crafted and will never mint");
                continue;   // the two checks below cannot mean anything for a name that resolves to nothing
            }

            // Compared on the NORMALISED token, so a claim spelled 'minecraft:IRON_CHESTPLATE'
            // against a material spelled 'iron_chestplate' is agreement rather than a false alarm.
            if (!result.equals(CraftResultToken.token(definition.material()))) {
                problems.add("gear '" + definition.id() + "' claims craft_result '" + result
                        + "' but mints as material '" + definition.material()
                        + "'. Crafting the first would hand the player the second. "
                        + "Make them the same, or remove the claim.");
            }

            if (!materialIsDurable.test(result)) {
                problems.add("gear '" + definition.id() + "' claims craft_result '" + result
                        + "', which has no durability. Mint-on-craft only replaces durable results, "
                        + "so this claim would never fire and nothing would say so. Remove it.");
            }
        }
        return problems;
    }

    /**
     * A custom mob's {@code base_entity} must name a real, LIVING entity type.
     *
     * The loader cannot check this: resolving a name to an {@code EntityType} needs the Bukkit
     * registry, which needs a running server -- the same reason a dangling {@code visual_id} is
     * checked here rather than at parse time. So the check arrives as a predicate seam and this stays
     * unit-testable with no server.
     *
     * The {@code isAlive} half is the one that earns its keep. {@code base_entity: arrow} resolves to
     * a perfectly real EntityType and would sail past a mere existence check, then fail at spawn time
     * with a ClassCastException on the first {@code /rpg spawn} -- weeks later, to whoever tries it.
     * Named at boot instead.
     *
     * @param entityExists  does this name resolve to an entity type at all
     * @param entityIsAlive does it resolve to a LivingEntity (not an arrow, item or display)
     * @return every problem found, each naming the mob at fault. Empty is good.
     */
    public List<String> validateMobs(Collection<MobDefinition> mobs,
                                     Predicate<String> entityExists,
                                     Predicate<String> entityIsAlive) {
        List<String> problems = new ArrayList<>();
        for (MobDefinition mob : mobs) {
            String label = "mob '" + mob.id() + "'";
            if (!entityExists.test(mob.baseEntity())) {
                problems.add(label + " names base_entity '" + mob.baseEntity()
                        + "', which is not an entity type");
            } else if (!entityIsAlive.test(mob.baseEntity())) {
                problems.add(label + " names base_entity '" + mob.baseEntity()
                        + "', which is not a living entity and cannot carry health or a nameplate");
            }
        }
        return problems;
    }

    /**
     * Every element named -- a weapon's, an ability's, a damage effect's -- must resolve to
     * a loaded element. Element is inert identity now, so a dangling one is not a crash, it
     * is a warning: the hit still lands, it just wears a colour nothing defines. This is the
     * same warn-not-skip shape as visual_id and status_id.
     */
    private void checkElement(String element, String ownerLabel, List<String> problems) {
        if (elements.find(element).isEmpty()) {
            problems.add(ownerLabel + " names element '" + element + "', which no element defines");
        }
    }

    /**
     * Descends into Area.effects() and Burst.effects(). solar_grenade nests its
     * status_id inside one of them, so a walk over only the top-level on_hit list
     * would check the visual, miss the status entirely, and pass while validating
     * nothing that matters.
     *
     * Today Area and Burst hold List<Targeted>, and no Targeted nests, so this bottoms
     * out one level down. It is written as a recursion over the sealed EffectSpec
     * anyway: if either ever admits untargeted children, the exhaustive switch drags
     * this method back into the light rather than silently skipping them.
     *
     * The switch below is checked whenever paper/ is compiled. That is the whole
     * mechanism -- there is nothing subtler to it.
     *
     * What makes a hole here survivable is that the daily loop, `./mvnw -pl core test`,
     * never compiles paper/ at all. So a missing arm sits undiscovered until somebody
     * runs a full build. That gap is what CI fills, by compiling paper/ on every push.
     *
     * This javadoc used to claim the error "only surfaces on a CLEAN build", because
     * Maven would not recompile this file when EffectSpec changed in another module.
     * That was measured on 2026-07-10 and is false. Adding a permitted record to
     * EffectSpec.Targeted (handled in EffectApplier, deliberately not handled here):
     *
     *   ./mvnw -pl paper -am compile   -> BUILD FAILURE, "does not cover all possible
     *                                     input values"
     *   ./mvnw -B compile              -> same, on a warm target/, having first printed
     *                                     "Compiling 24 source files"
     *   ./mvnw clean compile           -> same
     *
     * maven-compiler-plugin sees the changed dependency and recompiles the module.
     * `clean` catches nothing here that a plain build does not. Do not re-add the claim.
     */
    /**
     * The visual ids a CAST names, as opposed to the ones its effects name.
     *
     * <p>Both are equally invisible when they dangle -- the cast still fires, it just draws
     * nothing -- which is the same argument the on_cast walk above is written on.
     *
     * <p><b>A BEAM MAY NOT NAME A VISUAL CONTAINING A SOUND STEP, AND THIS IS THE ONLY PLACE THAT
     * CAN SAY SO.</b> {@code presentAlong} runs once per CHUNK-COLUMN SEGMENT, so a sound in a beam
     * would play one to three times depending on how many chunk planes the aim crossed: its
     * loudness would depend on which way the player was facing, intermittently, and it would look
     * like a bug in the sound engine rather than a content mistake. {@code VisualSpec} permits
     * Particles and Sound, and leaving the bound at the full sealed set because the one beam we
     * have happens not to carry a sound is how a schema grows behaviour nobody chose -- so it is
     * narrowed here, the same move as typing {@code AbilityDefinition.onCast} as
     * {@code List<EffectSpec.Visual>} rather than {@code Untargeted}.
     *
     * <p><b>Why HERE and not in VisualLoader, which could throw.</b> "Beam" is not a property of a
     * visual. It is a RELATIONSHIP declared by a {@code cast:} in a different file, so the loader
     * that could fail the file cannot see it. This class is where every cross-file reference check
     * already lives, and it warns rather than throws on purpose -- a typo in the 400th weapon must
     * not take the server down. One case does not justify breaking that.
     *
     * <p>{@code Projectile.trail} is checked in the same walk. It was NOT checked before, and
     * validating the new field while leaving the old one dangling would make the remaining gap
     * look deliberate -- AbilitySchema's own comment warns about exactly that asymmetry. The walk
     * costs nothing once written.
     */
    private void checkCast(CastSpec cast, String ownerLabel, List<String> problems) {
        switch (cast) {
            case CastSpec.Ray ray -> {
                if (ray.beam() == null) return;
                VisualDefinition beam = visuals.find(ray.beam()).orElse(null);
                if (beam == null) {
                    problems.add(ownerLabel + " names beam visual '" + ray.beam()
                            + "', which no visual defines");
                    return;
                }
                if (beam.steps().stream().anyMatch(s -> s instanceof VisualSpec.Sound)) {
                    problems.add(ownerLabel + " names beam visual '" + ray.beam()
                            + "', which contains a sound step. A beam is drawn once per chunk-column"
                            + " segment, so the sound would play once per segment and its loudness"
                            + " would depend on which way the caster is facing. Put the sound in"
                            + " on_cast or on_hit instead");
                }
            }
            case CastSpec.Projectile projectile -> {
                if (projectile.trail() != null && visuals.find(projectile.trail()).isEmpty()) {
                    problems.add(ownerLabel + " names trail visual '" + projectile.trail()
                            + "', which no visual defines");
                }
            }
            case CastSpec.Self ignored -> { }
            case CastSpec.Melee ignored -> { }
            case CastSpec.Dash ignored -> { }
        }
    }

    private void checkEffect(EffectSpec effect, String ownerLabel, List<String> problems) {
        switch (effect) {
            case EffectSpec.Visual visual -> {
                if (visuals.find(visual.visualId()).isEmpty()) {
                    problems.add(ownerLabel + " names visual_id '"
                            + visual.visualId() + "', which no visual defines");
                }
            }
            case EffectSpec.Status status -> {
                if (statuses.find(status.statusId()).isEmpty()) {
                    problems.add(ownerLabel + " names status_id '"
                            + status.statusId() + "', which no status defines");
                }
            }
            case EffectSpec.Area area -> {
                for (EffectSpec.Targeted nested : area.effects()) {
                    checkEffect(nested, ownerLabel, problems);
                }
            }
            case EffectSpec.Burst burst -> {
                for (EffectSpec.Targeted nested : burst.effects()) {
                    checkEffect(nested, ownerLabel, problems);
                }
            }
            case EffectSpec.ThrowEmbers embers -> {
                if (embers.visual() != null && visuals.find(embers.visual()).isEmpty()) {
                    problems.add(ownerLabel + " names throw_embers visual '"
                            + embers.visual() + "', which no visual defines");
                }
                if (embers.trail() != null && visuals.find(embers.trail()).isEmpty()) {
                    problems.add(ownerLabel + " names throw_embers trail visual '"
                            + embers.trail() + "', which no visual defines");
                }
                checkEffect(embers.burst(), ownerLabel, problems);
            }
            case EffectSpec.Damage damage -> checkElement(damage.element(), ownerLabel, problems);
            case EffectSpec.WeaponDamage weaponDamage -> checkElement(weaponDamage.element(), ownerLabel, problems);
            case EffectSpec.Heal ignored -> { }
            case EffectSpec.Knockback ignored -> { }
        }
    }
}
