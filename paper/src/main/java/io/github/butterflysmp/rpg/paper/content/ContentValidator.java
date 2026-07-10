package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
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
    private final Predicate<NamespacedKey> potionEffectExists;
    private final Predicate<NamespacedKey> soundExists;

    public ContentValidator(VisualRegistry visuals, StatusRegistry statuses,
                            Predicate<NamespacedKey> potionEffectExists,
                            Predicate<NamespacedKey> soundExists) {
        this.visuals = visuals;
        this.statuses = statuses;
        this.potionEffectExists = potionEffectExists;
        this.soundExists = soundExists;
    }

    /** @return every problem found, each naming the file or id at fault. Empty is good. */
    public List<String> validate(AbilityRegistry abilities) {
        List<String> problems = new ArrayList<>();
        for (AbilityDefinition ability : abilities.all()) {
            for (EffectSpec effect : ability.onHit()) {
                checkEffect(effect, ability.id(), problems);
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
    private void checkEffect(EffectSpec effect, String abilityId, List<String> problems) {
        switch (effect) {
            case EffectSpec.Visual visual -> {
                if (visuals.find(visual.visualId()).isEmpty()) {
                    problems.add("ability '" + abilityId + "' names visual_id '"
                            + visual.visualId() + "', which no visual defines");
                }
            }
            case EffectSpec.Status status -> {
                if (statuses.find(status.statusId()).isEmpty()) {
                    problems.add("ability '" + abilityId + "' names status_id '"
                            + status.statusId() + "', which no status defines");
                }
            }
            case EffectSpec.Area area -> {
                for (EffectSpec.Targeted nested : area.effects()) {
                    checkEffect(nested, abilityId, problems);
                }
            }
            case EffectSpec.Burst burst -> {
                for (EffectSpec.Targeted nested : burst.effects()) {
                    checkEffect(nested, abilityId, problems);
                }
            }
            case EffectSpec.Damage ignored -> { }
            case EffectSpec.Heal ignored -> { }
            case EffectSpec.Knockback ignored -> { }
        }
    }
}
