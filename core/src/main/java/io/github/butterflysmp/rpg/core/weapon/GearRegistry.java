package io.github.butterflysmp.rpg.core.weapon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable-after-load lookup for one kind of gear. The body the three registries were.
 *
 * <p>Kept as a base class with three thin subclasses rather than one registry parameterised at every
 * call site, and that is a deliberate trade. {@code GearRegistry<WeaponDefinition>} spelled out at
 * ~30 call sites would be noisier than {@code WeaponRegistry} and would let a caller declare a
 * registry of the wrong kind wherever the type is inferred. The subclasses cost three lines each and
 * keep every existing signature, every field, and every test untouched.
 *
 * <p>THREE registries and not one map keyed by kind, still: they hold different record types, and a
 * shared map would hand back something every caller downcasts. The cost is a resolve-order at the
 * one place that needs all three -- {@code /rpg give} -- plus the boot-time id-collision warning,
 * because no registry can see the others to reject a shared id.
 *
 * <p>{@code LinkedHashMap}, so two boots agree on suggestion order; the loaders sort their files for
 * the same reason. {@code register} refuses a duplicate rather than overwriting -- with one armor
 * tier file emitting four definitions, a copy-pasted material token inside a single file is a
 * realistic slip, and silently keeping the last one would cost a slot with nothing in the log.
 */
public abstract class GearRegistry<T extends GearDefinition> {

    private final Map<String, T> byId = new LinkedHashMap<>();

    /**
     * The word this kind is called in a duplicate-id message ("weapon", "shield", "armor").
     *
     * Carried rather than derived from the class name: the message is read in a boot log by someone
     * looking for the file to rename, and a reflective name would drift the moment a class is
     * renamed or the build starts obfuscating.
     */
    private final String noun;

    protected GearRegistry(String noun) {
        this.noun = noun;
    }

    public void register(T def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate " + noun + " id: " + def.id());
        }
    }

    public Optional<T> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<T> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
