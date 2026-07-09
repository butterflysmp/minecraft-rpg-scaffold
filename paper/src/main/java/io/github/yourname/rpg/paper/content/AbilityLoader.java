package io.github.yourname.rpg.paper.content;

import io.github.yourname.rpg.core.ability.*;
import io.github.yourname.rpg.core.ability.effect.EffectSpec;
import io.github.yourname.rpg.core.element.Element;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns YAML into AbilityDefinition. This is the ONLY place that knows the
 * on-disk schema. core stays ignorant of files.
 *
 * Adding a new ability = adding a .yml file. No recompile. That is the whole
 * point of the content pipeline; do not break it by special-casing abilities
 * in Java.
 */
public final class AbilityLoader {

    public AbilityRegistry loadAll(File abilitiesDir) {
        AbilityRegistry registry = new AbilityRegistry();
        File[] files = abilitiesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        for (File f : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
            try {
                registry.register(parse(yaml));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Failed to load ability: " + f.getName(), ex);
            }
        }
        return registry;
    }

    private AbilityDefinition parse(ConfigurationSection s) {
        return new AbilityDefinition(
                req(s, "id"),
                s.getString("display_name", req(s, "id")),
                Element.valueOf(req(s, "element").toUpperCase(Locale.ROOT)),
                s.getString("archetype", "none"),
                s.getInt("cooldown_ticks", 0),
                parseCost(s.getConfigurationSection("cost")),
                parseCast(s.getConfigurationSection("cast")),
                parseEffects(s.getMapList("on_hit"))
        );
    }

    private static String req(ConfigurationSection s, String path) {
        String v = s.getString(path);
        if (v == null) throw new IllegalArgumentException("Missing required field: " + path);
        return v;
    }

    private ResourceCost parseCost(ConfigurationSection s) {
        if (s == null) return ResourceCost.FREE;
        return new ResourceCost(s.getString("resource", "energy"), s.getDouble("amount", 0));
    }

    private CastSpec parseCast(ConfigurationSection s) {
        if (s == null) return new CastSpec.Self();
        String type = s.getString("type", "self").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "self"       -> new CastSpec.Self();
            case "melee"      -> new CastSpec.Melee(s.getDouble("reach", 3.0), s.getDouble("arc_degrees", 90));
            case "ray"        -> new CastSpec.Ray(s.getDouble("range", 30));
            case "projectile" -> new CastSpec.Projectile(
                    s.getDouble("speed", 1.0), s.getDouble("gravity", 0.03),
                    s.getInt("max_lifetime_ticks", 100));
            default -> throw new IllegalArgumentException("Unknown cast type: " + type);
        };
    }

    private List<EffectSpec> parseEffects(List<java.util.Map<?, ?>> raw) {
        List<EffectSpec> out = new ArrayList<>();
        for (java.util.Map<?, ?> m : raw) {
            out.add(parseEffect(m));
        }
        return out;
    }

    /**
     * An Area may only nest targeted effects. YAML is untyped, so what the
     * compiler enforces in core has to be checked here at load time.
     */
    @SuppressWarnings("unchecked")
    private List<EffectSpec.Targeted> parseNestedEffects(Object raw) {
        List<EffectSpec.Targeted> out = new ArrayList<>();
        for (java.util.Map<?, ?> m : (List<java.util.Map<?, ?>>) raw) {
            EffectSpec spec = parseEffect(m);
            if (!(spec instanceof EffectSpec.Targeted t)) {
                throw new IllegalArgumentException(
                        "Effect '" + m.get("type") + "' cannot be nested inside an area; "
                                + "only targeted effects (damage, heal, knockback, status) can");
            }
            out.add(t);
        }
        return out;
    }

    private EffectSpec parseEffect(java.util.Map<?, ?> m) {
        String type = String.valueOf(m.get("type")).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "damage" -> new EffectSpec.Damage(
                    num(m, "amount"), Element.valueOf(str(m, "element").toUpperCase(Locale.ROOT)));
            case "heal" -> new EffectSpec.Heal(num(m, "amount"));
            case "knockback" -> new EffectSpec.Knockback(num(m, "strength"));
            case "status" -> new EffectSpec.Status(
                    str(m, "status_id"), (int) num(m, "duration_ticks"), (int) num(m, "amplifier"));
            case "visual" -> new EffectSpec.Visual(str(m, "visual_id"));
            case "area" -> new EffectSpec.Area(
                    num(m, "radius"), (int) num(m, "duration_ticks"), (int) num(m, "tick_interval"),
                    parseNestedEffects(m.get("effects")));
            default -> throw new IllegalArgumentException("Unknown effect type: " + type);
        };
    }

    private static double num(java.util.Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing effect field: " + k);
        return ((Number) v).doubleValue();
    }

    private static String str(java.util.Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing effect field: " + k);
        return String.valueOf(v);
    }
}
