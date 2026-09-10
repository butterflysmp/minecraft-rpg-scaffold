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
        return new ResourceCost(s.getString("resource", ResourceCost.DEFAULT_RESOURCE), s.getDouble("amount", 0));
    }

    static CastSpec parseCast(ConfigurationSection s) {
        if (s == null) return new CastSpec.Self();
        String type = s.getString("type", "self").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "self"       -> new CastSpec.Self();
            case "melee"      -> new CastSpec.Melee(s.getDouble("reach", 3.0), s.getDouble("arc_degrees", 90));
            // `beam` is OPTIONAL and absent means null -- a ray that draws nothing between the
            // muzzle and the impact, which is what solar_lance is and what every ray was before
            // this line. Mirrors `trail` on projectile below, and is validated the same way:
            // ContentValidator resolves the id at boot, so a typo is named rather than silent.
            case "ray"        -> new CastSpec.Ray(s.getDouble("range", 30), s.getString("beam"));
            // `trail` is OPTIONAL and absent means null -- a bare projectile that leaves nothing,
            // which is what hunters_bow and ember_staff get and what they have always had. The
            // flight loop has always presented a trail every tick; until this line there was no
            // way for a file to name one.
            // `item` is the material rendered as the bolt's BODY, likewise optional. Independent of
            // `trail`: a trail with no body is what the Flint Staff shipped as one slice earlier.
            // Not validated against a real Material here -- the adapter warns once and falls back,
            // exactly as it does for throw_embers' `item`. Validating one and not the other would
            // make the remaining gap look deliberate.
            case "projectile" -> new CastSpec.Projectile(
                    s.getDouble("speed", 1.0), s.getDouble("gravity", 0.03),
                    s.getInt("max_lifetime_ticks", 100), s.getString("trail"), s.getString("item"));
            case "dash"       -> new CastSpec.Dash(
                    s.getDouble("distance", 12), s.getDouble("speed", 1.6), s.getDouble("lift", 0.4),
                    parseDashDirection(s.getString("direction", "movement_else_forward")));
            // A cast that REPEATS another cast on a clock. `of` is required and is a whitelist:
            // see innerCast below. windup_ticks 0 means the first shot fires on the cast frame.
            case "volley"     -> new CastSpec.Volley(
                    s.getInt("windup_ticks", 0), s.getInt("shots", 1), s.getInt("interval_ticks", 1),
                    innerCast(s.getConfigurationSection("of")));
            default -> throw new IllegalArgumentException("Unknown cast type: " + type);
        };
    }


    /**
     * The cast a volley repeats -- parsed as any other cast, then ADMITTED BY WHITELIST.
     *
     * <p><b>A gate says what it CAN be, never what it cannot.</b> The admission is an exhaustive
     * pattern switch over the sealed {@code CastSpec} rather than a list of rejected strings, so a
     * seventh kind is refused BY DEFAULT and will not compile until someone decides whether it is
     * repeatable. The opposite shape -- naming the refusals -- admits every kind nobody has thought
     * of yet, which is how a schema grows behaviour nobody chose.
     *
     * <p>{@code projectile} is admitted on day one and exercised by {@code volley_stone}, not merely
     * asserted: a burst-fire projectile weapon is the second thing this grammar is for, and a
     * wrapper that only knew rays would have to be widened the first time anyone wanted one.
     *
     * <p><b>{@code dash}'s refusal is load-bearing rather than tidy.</b> {@code paper.weapon.DashAim}
     * resolves a dash's direction BEFORE the region hop and matches on the OUTER cast only, so a
     * volley of dashes would lose its direction resolution silently -- it would still cast, and it
     * would go the wrong way.
     *
     * <p>An absent {@code of:} THROWS rather than defaulting to {@code Self} the way
     * {@link #parseCast} does for an absent {@code cast:}. A cast section that is missing entirely
     * has a sensible reading; a volley of nothing does not.
     */
    private static CastSpec innerCast(ConfigurationSection s) {
        if (s == null) {
            throw new IllegalArgumentException("a volley needs an 'of:' section naming the cast it"
                    + " repeats; a volley of nothing has no sensible reading");
        }
        CastSpec inner = parseCast(s);
        switch (inner) {
            case CastSpec.Ray ignored -> { }
            case CastSpec.Projectile ignored -> { }
            case CastSpec.Self ignored -> throw notRepeatable("self",
                    "it lands where the caster already is, so repeating it is a sound with no"
                            + " mechanism behind it");
            case CastSpec.Melee ignored -> throw notRepeatable("melee",
                    "it lands where the caster already is, so repeating it is a sound with no"
                            + " mechanism behind it");
            case CastSpec.Dash ignored -> throw notRepeatable("dash",
                    "a dash's direction is resolved before the cast is dispatched and only for an"
                            + " OUTER dash, so a repeated one would go the wrong way rather than fail");
            case CastSpec.Volley ignored -> throw notRepeatable("volley",
                    "each shot would start its own burst and the count would multiply every interval");
        }
        return inner;
    }

    private static IllegalArgumentException notRepeatable(String type, String why) {
        return new IllegalArgumentException("a volley cannot repeat cast type '" + type + "': " + why
                + ". Only 'ray' and 'projectile' are repeatable");
    }
    private static CastSpec.DashDirection parseDashDirection(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "movement_else_forward" -> CastSpec.DashDirection.MOVEMENT_ELSE_FORWARD;
            case "reverse_facing"        -> CastSpec.DashDirection.REVERSE_FACING;
            default -> throw new IllegalArgumentException("Unknown dash direction: " + raw);
        };
    }

    /**
     * The {@code on_cast} list: VISUALS ONLY, and the narrowness is the point.
     *
     * <p>The case this hook was built for is a sound at the moment you press the button. The
     * obvious wider bound -- {@code EffectSpec.Untargeted} -- would also admit {@code burst}
     * (mob damage at the caster's own eye on every cast), {@code area} (a lingering field there)
     * and {@code throw_embers}, none of which has been designed, and the last of which would be
     * outright degenerate: the applier's four-argument entry point passes a ZERO direction, so a
     * fan would be computed around a zero vector. Enumerating one case and then picking a bound
     * that silently carries three more is how a schema grows behaviour nobody chose.
     *
     * <p>{@code AbilityDefinition.onCast} is typed {@code List<EffectSpec.Visual>}, so the
     * compiler states the rule for every call site in core. This is the YAML half of it, because
     * YAML is untyped and a file can write anything.
     */
    static List<EffectSpec.Visual> parseCastVisuals(List<Map<?, ?>> raw) {
        List<EffectSpec.Visual> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            EffectSpec spec = parseEffect(m);
            if (!(spec instanceof EffectSpec.Visual v)) {
                throw new IllegalArgumentException(
                        "Effect '" + m.get("type") + "' cannot appear in on_cast; only 'visual' can. "
                                + "on_cast fires at the caster the instant a cast is committed, and "
                                + "nothing else has been designed to happen there");
            }
            out.add(v);
        }
        return out;
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
            // The basic melee hit: no literal amount -- deals the caster's ATTACK_DAMAGE stat (the
            // weapon's declared attack_damage, as a MAIN_HAND modifier). element is identity, as for damage.
            case "weapon_damage" -> new EffectSpec.WeaponDamage(str(m, type, "element"));
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
