package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.build.AspectApplication;
import io.github.butterflysmp.rpg.core.build.AspectDefinition;
import io.github.butterflysmp.rpg.core.build.AspectField;
import io.github.butterflysmp.rpg.core.build.AspectRegistry;
import io.github.butterflysmp.rpg.core.build.NumberChange;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Loads {@code content/aspects/<id>.yml} (PLAN-build-system.md sections 2.4 and 2.4.1). The id is the filename.
 *
 * <pre>
 *   display_name: "&lt;gold&gt;Searing Lance&lt;/gold&gt;"
 *   description: ["Solar Lance detonates on impact."]
 *   target: solar_lance                   one ability id; the pools that list this aspect must offer it
 *   add_on_hit: [ ... ]                   any effect an ability's on_hit may carry (AbilitySchema's parser)
 *   add_on_cast: [ ... ]                  visuals only (AbilitySchema's on_cast parser)
 *   modify:
 *     - { field: damage.amount, percent: -25 }   a whitelisted field (AspectField); flat and/or percent
 * </pre>
 *
 * <p>TWO STAGES. {@link #loadAll} parses each file alone, skipping a bad one by name. {@link #checkAgainstPools}
 * runs after the pools load: every aspect a pool lists is resolved against its target alone, and every PAIR
 * that pool lists on the same target is resolved together (two slots make a pair the worst case). An illegal
 * resolution REFUSES THE ASPECT -- both of a pair's files -- by name, and it is removed from the registry.
 */
public final class AspectLoader {

    private final Logger log;

    public AspectLoader(Logger log) {
        this.log = log;
    }

    public AspectRegistry loadAll(File aspectsDir) {
        AspectRegistry registry = new AspectRegistry();
        File[] files = aspectsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;
        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        List<String> retired = new ArrayList<>();
        for (File f : files) {
            if (RETIRED_FILES.contains(f.getName())) {
                retired.add(f.getName());
                continue;
            }
            try {
                String id = f.getName().substring(0, f.getName().length() - ".yml".length());
                registry.register(parse(id, YamlConfiguration.loadConfiguration(f)));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping aspect '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (!retired.isEmpty()) {
            log.warning(retired.size() + " aspect file(s) in " + aspectsDir.getPath() + " " + retired
                    + " were DELETED by a ruling and are skipped. They are stale copies; nothing deletes them.");
        }
        if (skipped > 0) {
            log.warning(skipped + " aspect file(s) were skipped. The server is still running, but a pool"
                    + " naming one of them will be refused.");
        }
        return registry;
    }

    /**
     * Aspect files DELETED by a ruling, skipped if a stale copy survives in a data folder -- AbilityLoader's
     * RETIRED_FILES handling: named once at boot, never deleted, and not loaded.
     *
     * <p>{@code banked_embers.yml}: ruling 26 (2026-09-27), PLAN-build-system.md. Without the skip, a stale copy
     * would be refused at boot for targeting {@code rekindle} -- loud, but it reads as a fault, not a retirement.
     */
    static final java.util.Set<String> RETIRED_FILES = java.util.Set.of("banked_embers.yml");

    static AspectDefinition parse(String id, ConfigurationSection s) {
        String target = s.getString("target");
        List<NumberChange> modify = new ArrayList<>();
        for (Map<?, ?> m : s.getMapList("modify")) {
            Object token = m.get("field");
            AspectField field = token == null ? null : AspectField.fromToken(token.toString()).orElse(null);
            if (field == null) {
                throw new IllegalArgumentException("modify names field '" + token + "', which is not on the whitelist "
                        + Arrays.stream(AspectField.values()).map(AspectField::token).toList());
            }
            modify.add(new NumberChange(field, number(m, "flat", field), number(m, "percent", field)));
        }
        return new AspectDefinition(id, s.getString("display_name"), s.getStringList("description"), target,
                AbilitySchema.parseEffects(s.getMapList("add_on_hit")),
                AbilitySchema.parseCastVisuals(s.getMapList("add_on_cast")),
                modify, parseRecast(s.getConfigurationSection("recast")));
    }

    /** Section 7.4: {@code recast: { ability, from_ticks, window_ticks }}, all three required. Null when absent. */
    private static AspectDefinition.Recast parseRecast(ConfigurationSection r) {
        if (r == null) return null;
        for (String key : List.of("ability", "from_ticks", "window_ticks")) {
            if (!r.contains(key)) throw new IllegalArgumentException("recast needs `" + key + ":`");
        }
        if (!r.isInt("from_ticks") || !r.isInt("window_ticks")) {
            throw new IllegalArgumentException("recast from_ticks and window_ticks must be whole ticks");
        }
        return new AspectDefinition.Recast(r.getString("ability"), r.getInt("from_ticks"), r.getInt("window_ticks"));
    }

    private static double number(Map<?, ?> m, String key, AspectField field) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (!(v instanceof Number n)) {
            throw new IllegalArgumentException("modify " + field.token() + "." + key + " is not a number: " + v);
        }
        return n.doubleValue();
    }

    /**
     * The pool-level checks (section 2.4.1): each aspect a pool lists, alone and in every same-target PAIR,
     * resolved against its target. Returns the registry minus every refused aspect; each refusal is logged by
     * name.
     *
     * @param statusDurationTraced is this status id's kind traced continuous in its duration
     */
    /**
     * Section 7.4: a recast's follow-up must be a loaded ability, and must be in NO pool. Its only door is the
     * recast, which casts it free and with no cooldown; offered as an Active it would be a second, priced door
     * to the same cast.
     */
    static Optional<String> recastRefusal(AspectDefinition a, PoolRegistry pools,
                                          Function<String, Optional<AbilityDefinition>> abilities) {
        if (a.recast() == null) return Optional.empty();
        String followUp = a.recast().ability();
        if (abilities.apply(followUp).isEmpty()) {
            return Optional.of("its recast names '" + followUp + "', which is no loaded ability");
        }
        for (PoolDefinition pool : pools.all()) {
            if (pool.ultimates().contains(followUp) || pool.actives().contains(followUp)) {
                return Optional.of("its recast's follow-up '" + followUp + "' is offered by pool "
                        + pool.cell().classId() + "/" + pool.cell().elementId()
                        + "; a follow-up must be in NO pool (section 7.4)");
            }
        }
        return Optional.empty();
    }

    public AspectRegistry checkAgainstPools(AspectRegistry parsed, PoolRegistry pools,
                                            Function<String, Optional<AbilityDefinition>> abilities,
                                            Predicate<String> statusDurationTraced) {
        Map<String, String> refused = new LinkedHashMap<>();
        for (PoolDefinition pool : pools.all()) {
            List<AspectDefinition> listed = new ArrayList<>();
            for (String id : pool.aspects()) parsed.find(id).ifPresent(listed::add);
            for (int i = 0; i < listed.size(); i++) {
                AspectDefinition a = listed.get(i);
                Optional<AbilityDefinition> target = abilities.apply(a.target());
                if (target.isEmpty()) {
                    refused.putIfAbsent(a.id(), "its target '" + a.target() + "' is no loaded ability");
                    continue;
                }
                for (String why : AspectApplication.refusals(target.get(), List.of(a), statusDurationTraced)) {
                    refused.putIfAbsent(a.id(), why);
                }
                recastRefusal(a, pools, abilities).ifPresent(why -> refused.putIfAbsent(a.id(), why));
                for (int j = i + 1; j < listed.size(); j++) {
                    AspectDefinition b = listed.get(j);
                    if (!b.target().equals(a.target())) continue;
                    // Section 7.4: one follow-up per cast, so two recast-granting aspects on one target are a
                    // pair that cannot be equipped together -- refused by name, like any illegal pair.
                    if (a.recast() != null && b.recast() != null) {
                        String pair = "equipped together with '%s' (pool " + pool.cell().classId() + "/"
                                + pool.cell().elementId() + "): both grant a recast of " + a.target()
                                + ", and a cast carries ONE follow-up";
                        refused.putIfAbsent(a.id(), pair.formatted(b.id()));
                        refused.putIfAbsent(b.id(), pair.formatted(a.id()));
                    }
                    for (String why : AspectApplication.refusals(target.get(), List.of(a, b), statusDurationTraced)) {
                        String pair = "equipped together with '" + b.id() + "' (pool " + pool.cell().classId() + "/"
                                + pool.cell().elementId() + "): " + why;
                        refused.putIfAbsent(a.id(), pair);
                        refused.putIfAbsent(b.id(), pair.replace("with '" + b.id() + "'", "with '" + a.id() + "'"));
                    }
                }
            }
        }
        AspectRegistry kept = new AspectRegistry();
        for (AspectDefinition aspect : parsed.all()) {
            if (refused.containsKey(aspect.id())) {
                log.warning("Refusing aspect '" + aspect.id() + ".yml': " + refused.get(aspect.id()));
            } else {
                kept.register(aspect);
            }
        }
        return kept;
    }
}
