package io.github.butterflysmp.rpg.core.recipe;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable-after-load lookup for every custom recipe the server knows about.
 *
 * <p>Same shape as {@code StatusRegistry} and the four gear registries, deliberately -- a twelfth
 * content kind should not need a reader to learn a twelfth pattern.
 *
 * <p><b>The duplicate-id throw is load-bearing here in a way it is not elsewhere.</b> A recipe's id
 * becomes the key half of the {@code NamespacedKey} paper registers, so two definitions sharing one
 * id would be two recipes claiming one key -- and the second {@code addRecipe} would silently
 * replace the first, leaving one authored file with no effect and nothing saying so. In practice
 * the filesystem prevents it (the id IS the filename), which is exactly why the recipe axis needs
 * no contested-claim policy of the kind {@code CraftResultIndex} carries for materials. This throw
 * is what makes that argument true rather than merely likely.
 */
public final class RecipeRegistry {
    private final Map<String, RecipeDefinition> byId = new LinkedHashMap<>();

    public void register(RecipeDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate recipe id: " + def.id());
        }
    }

    public Optional<RecipeDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<RecipeDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
