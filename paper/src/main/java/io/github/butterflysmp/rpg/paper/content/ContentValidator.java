package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
            for (EffectSpec effect : ability.onHit()) {
                checkEffect(effect, "ability '" + ability.id() + "'", problems);
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
                for (EffectSpec effect : binding.ability().onHit()) {
                    checkEffect(effect, label, problems);
                }
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
                if (embers.trail() != null && visuals.find(embers.trail()).isEmpty()) {
                    problems.add(ownerLabel + " names trail visual '"
                            + embers.trail() + "', which no visual defines");
                }
                for (EffectSpec nested : embers.onImpact()) {
                    checkEffect(nested, ownerLabel, problems);
                }
            }
            case EffectSpec.DelayedBurst delayed -> {
                if (delayed.visual() != null && visuals.find(delayed.visual()).isEmpty()) {
                    problems.add(ownerLabel + " names delayed_burst visual '"
                            + delayed.visual() + "', which no visual defines");
                }
                checkEffect(delayed.burst(), ownerLabel, problems);
            }
            case EffectSpec.Damage damage -> checkElement(damage.element(), ownerLabel, problems);
            case EffectSpec.Heal ignored -> { }
            case EffectSpec.Knockback ignored -> { }
        }
    }
}
