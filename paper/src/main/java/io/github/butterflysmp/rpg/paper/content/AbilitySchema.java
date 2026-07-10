package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.element.Element;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The cast/cost/effect grammar shared by every triggerable thing on disk. Extracted
 * from AbilityLoader so a weapon trigger -- which IS an ability's cast plus an input --
 * reuses it rather than duplicating it. If a weapon trigger and an ability ever needed
 * to parse a cast differently, that would be the smell CLAUDE.md warns about; they do
 * not, so there is one parser.
 *
 * All static, all fail-loud: a malformed field throws, and each loader's
 * catch(RuntimeException) turns that into a named, skipped file. core stays ignorant
 * of files; this is the only place that knows the schema.
 */
final class AbilitySchema {

    private AbilitySchema() {}

    static String req(ConfigurationSection s, String path) {
        String v = s.getString(path);
        if (v == null) throw new IllegalArgumentException("Missing required field: " + path);
        return v;
    }

    /**
     * The one caller of Element.fromName that wants a throw. Failing the file is right
     * here: a misspelled element in content is an authoring mistake, and the loader's
     * catch(RuntimeException) turns it into a named, skipped file.
     *
     * BukkitCombatant calls the same lookup and does NOT throw, because it reads the
     * same enum on the damage path. One place knows the names; two decide what a miss means.
     */
    static Element element(String raw) {
        Element parsed = Element.fromName(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unknown element '" + raw + "'; expected one of " + Arrays.toString(Element.values()));
        }
        return parsed;
    }

    static ResourceCost parseCost(ConfigurationSection s) {
        if (s == null) return ResourceCost.FREE;
        return new ResourceCost(s.getString("resource", "energy"), s.getDouble("amount", 0));
    }

    static CastSpec parseCast(ConfigurationSection s) {
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

    static List<EffectSpec> parseEffects(List<Map<?, ?>> raw) {
        List<EffectSpec> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            out.add(parseEffect(m));
        }
        return out;
    }

    /**
     * An Area or a Burst may only nest targeted effects. YAML is untyped, so what the
     * compiler enforces in core has to be checked here at load time.
     */
    private static List<EffectSpec.Targeted> parseNestedEffects(Map<?, ?> parent, String parentType) {
        List<EffectSpec.Targeted> out = new ArrayList<>();
        for (Map<?, ?> m : mapList(parent, parentType, "effects")) {
            EffectSpec spec = parseEffect(m);
            if (!(spec instanceof EffectSpec.Targeted t)) {
                throw new IllegalArgumentException(
                        "Effect '" + m.get("type") + "' cannot be nested inside " + parentType + "; "
                                + "only targeted effects (damage, heal, knockback, status) can");
            }
            out.add(t);
        }
        return out;
    }

    private static EffectSpec parseEffect(Map<?, ?> m) {
        Object rawType = m.get("type");
        if (rawType == null) throw new IllegalArgumentException("Effect is missing its 'type' field");
        String type = String.valueOf(rawType).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "damage" -> new EffectSpec.Damage(num(m, type, "amount"), element(str(m, type, "element")));
            case "heal" -> new EffectSpec.Heal(num(m, type, "amount"));
            case "knockback" -> new EffectSpec.Knockback(num(m, type, "strength"));
            case "status" -> new EffectSpec.Status(
                    str(m, type, "status_id"),
                    (int) num(m, type, "duration_ticks"),
                    // Optional: most statuses have a single tier.
                    (int) numOr(m, type, "amplifier", 0));
            case "visual" -> new EffectSpec.Visual(str(m, type, "visual_id"));
            // A blast: lands once, on the detonation frame. Contrast 'area', a field.
            case "burst" -> new EffectSpec.Burst(
                    num(m, type, "radius"),
                    parseNestedEffects(m, type));
            case "area" -> new EffectSpec.Area(
                    num(m, type, "radius"),
                    (int) num(m, type, "duration_ticks"),
                    (int) num(m, type, "tick_interval"),
                    parseNestedEffects(m, type));
            default -> throw new IllegalArgumentException("Unknown effect type: " + type);
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> mapList(Map<?, ?> m, String type, String k) {
        Object v = m.get(k);
        if (v == null) {
            throw new IllegalArgumentException("Effect '" + type + "' is missing its '" + k + "' list");
        }
        if (!(v instanceof List<?> list)) {
            throw new IllegalArgumentException("Effect '" + type + "' field '" + k + "' must be a list");
        }
        return (List<Map<?, ?>>) list;
    }

    private static double num(Map<?, ?> m, String type, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Effect '" + type + "' is missing field: " + k);
        if (!(v instanceof Number n)) {
            throw new IllegalArgumentException(
                    "Effect '" + type + "' field '" + k + "' must be a number, got: " + v);
        }
        return n.doubleValue();
    }

    private static double numOr(Map<?, ?> m, String type, String k, double fallback) {
        return m.get(k) == null ? fallback : num(m, type, k);
    }

    private static String str(Map<?, ?> m, String type, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Effect '" + type + "' is missing field: " + k);
        return String.valueOf(v);
    }
}
