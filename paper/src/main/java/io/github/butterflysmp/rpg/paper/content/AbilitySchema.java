package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
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
            case "dash"       -> new CastSpec.Dash(
                    s.getDouble("distance", 12), s.getDouble("speed", 1.6), s.getDouble("lift", 0.4),
                    parseDashDirection(s.getString("direction", "movement_else_forward")));
            default -> throw new IllegalArgumentException("Unknown cast type: " + type);
        };
    }

    private static CastSpec.DashDirection parseDashDirection(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "movement_else_forward" -> CastSpec.DashDirection.MOVEMENT_ELSE_FORWARD;
            case "reverse_facing"        -> CastSpec.DashDirection.REVERSE_FACING;
            default -> throw new IllegalArgumentException("Unknown dash direction: " + raw);
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
            // element is a plain content id now -- carried, not resolved. ContentValidator
            // checks it against the loaded element set at boot; a bad value warns, never skips.
            case "damage" -> new EffectSpec.Damage(num(m, type, "amount"), str(m, type, "element"));
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
            // A fan of thrown items, each tracked by a per-tick loop: draw the trail, count the
            // fuse, then a (mob-only) burst at its live position. The item IS the marker.
            case "throw_embers" -> new EffectSpec.ThrowEmbers(
                    numberList(m, type, "angles_degrees"),
                    num(m, type, "speed"),
                    num(m, type, "launch_lift"),
                    str(m, type, "item"),
                    (int) num(m, type, "fuse_ticks"),
                    parseBurst(mapOf(m, type, "burst")),
                    strOrNull(m, "visual"),    // optional boom/flash at detonation
                    strOrNull(m, "trail"));    // optional per-tick flame trail along the arc
            default -> throw new IllegalArgumentException("Unknown effect type: " + type);
        };
    }

    /** A nested burst section ({@code radius} + Targeted {@code effects}), for delayed_burst. */
    private static EffectSpec.Burst parseBurst(Map<?, ?> burst) {
        return new EffectSpec.Burst(num(burst, "burst", "radius"), parseNestedEffects(burst, "burst"));
    }

    private static List<Double> numberList(Map<?, ?> m, String type, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Effect '" + type + "' is missing its '" + key + "' list");
        }
        if (!(v instanceof List<?> list)) {
            throw new IllegalArgumentException("Effect '" + type + "' field '" + key + "' must be a list");
        }
        List<Double> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Number n)) {
                throw new IllegalArgumentException(
                        "Effect '" + type + "' field '" + key + "' must be a list of numbers, got: " + o);
            }
            out.add(n.doubleValue());
        }
        return out;
    }

    private static Map<?, ?> mapOf(Map<?, ?> m, String type, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Effect '" + type + "' is missing its '" + key + "' section");
        }
        if (!(v instanceof Map<?, ?> section)) {
            throw new IllegalArgumentException("Effect '" + type + "' field '" + key + "' must be a section");
        }
        return section;
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

    /** An optional string field: null when absent, rather than a thrown error. */
    private static String strOrNull(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
