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

    /**
     * THE ORB'S SHOT (Lapis Crystal's D3, 2026-11-23): OPTIONAL (absent: an orb that only stands). Present, every key
     * but {@code visual} is required, and {@code CastSpec.OrbShot} refuses a bad value by name.
     *
     * <pre>
     * shot:
     *   radius: 8
     *   percent_by_level: [20, 30, 40]
     *   element: kinetic
     *   visual: crystal_shot      # optional
     * </pre>
     */
    static CastSpec.OrbShot parseOrbShot(ConfigurationSection s) {
        if (s == null) return null;
        if (!s.isList("percent_by_level")) {
            throw new IllegalArgumentException("orb shot: is missing the list 'percent_by_level' (one per level)");
        }
        return new CastSpec.OrbShot(reqDouble(s, "radius"), s.getDoubleList("percent_by_level"), req(s, "element"),
                s.getString("visual"));
    }

    static CastSpec parseCast(ConfigurationSection s) {
        if (s == null) return new CastSpec.Self();
        String type = s.getString("type", "self").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "self"       -> new CastSpec.Self();
            case "melee"      -> new CastSpec.Melee(s.getDouble("reach", 3.0), s.getDouble("arc_degrees", 90));
            // VANILLA'S OWN SHOT (2026-11-22): no keys; vanilla draws, loads and shoots. Legal only under a weapon's shot
            // trigger (WeaponDefinition refuses it elsewhere). Its on_hit lands where vanilla's arrow lands.
            case "vanilla"    -> new CastSpec.Vanilla();
            // THE SUMMON CAST (3a): names a content/summons file; run by the summon service, never by the engine.
            case "summon"     -> new CastSpec.Summon(s.getString("summon"));
            // THE LEFT CLICK'S ORDER (4c, D5): reach is REQUIRED (no default is invented); 0 or absent is refused by the record.
            case "direct_summon" -> new CastSpec.DirectSummon(s.getDouble("reach", 0.0));
            // `beam` is OPTIONAL and absent means null -- a ray that draws nothing between the
            // muzzle and the impact, which is what solar_lance is and what every ray was before
            // this line. Mirrors `trail` on projectile below, and is validated the same way:
            // ContentValidator resolves the id at boot, so a typo is named rather than silent.
            // `pierce` and `tolerance` (2026-11-21, the Knell Beam): both OPTIONAL and default OFF, so every ray
            // authored before them parses to the ray it always was. tolerance is in BLOCKS; CastSpec.Ray refuses a
            // negative one by name. See CastSpec.Ray for what each does.
            case "ray"        -> new CastSpec.Ray(s.getDouble("range", 30), s.getString("beam"),
                    s.getBoolean("pierce", false), s.getDouble("tolerance", 0.0));
            // `trail` is OPTIONAL and absent means null -- a bare projectile that leaves nothing,
            // which is what hunters_bow (deleted 2026-11) and ember_staff (deleted 2026-11) get and what they have always had. The
            // flight loop has always presented a trail every tick; until this line there was no
            // way for a file to name one.
            // `item` is the material rendered as the bolt's BODY, likewise optional. Independent of
            // `trail`: a trail with no body is what the Flint Staff shipped as one slice earlier.
            // Not validated against a real Material here -- the adapter warns once and falls back,
            // exactly as it does for throw_embers' `item`. Validating one and not the other would
            // make the remaining gap look deliberate.
            // `homing` is OPTIONAL and absent means null -- a bolt that flies where it was aimed,
            // which is every projectile this repo shipped before the Dragon's Plume. Unlike `trail`
            // and `item` it is a SECTION rather than a scalar, and unlike them it has no partial
            // form: see parseHoming.
            // `spread` is OPTIONAL and absent means null -- ONE body down the aim vector, which is
            // every projectile this repo shipped before the Dragon's Breath. A SECTION with no partial
            // form, exactly like `homing`, and orthogonal to it: a spread of homing bodies is
            // authorable and means what it says.
            // `body` is OPTIONAL and absent means null. It is the OTHER kind of body: `item` renders
            // a dropped item driven along the flight, `body` renders a real projectile entity (an
            // arrow oriented along its travel, or a fireball) with every interaction switched off.
            // MUTUALLY EXCLUSIVE with `item` -- a bolt has one body -- and refused here rather than
            // resolved, because whichever the flight picked the other key would silently do nothing.
            //
            // The VALUE is checked because the body kinds are ours, a set of two (parseBody), and a
            // typo must be a named file rather than a bolt that quietly renders nothing. That is a
            // different standard from `item`, which is NOT checked here (the adapter warns once and
            // falls back to BLAZE_POWDER), and the difference is deliberate: `item` names a Material
            // out of a huge open set, `body` names one of ours out of a set we wrote.
            // `fuse_ticks` (the seat's F1, 2026-11-26, the Blast Fungus) is OPTIONAL and absent means 0, no fuse: every
            // projectile before it. A fused projectile's life IS its fuse, so `max_lifetime_ticks` beside it is refused by
            // name rather than silently ignored (a number nobody could see doing nothing).
            case "projectile" -> {
                int fuse = s.getInt("fuse_ticks", 0);
                if (fuse > 0 && s.contains("max_lifetime_ticks")) {
                    throw new IllegalArgumentException("a fused projectile (fuse_ticks " + fuse + ") lives until its fuse;"
                            + " remove max_lifetime_ticks");
                }
                yield new CastSpec.Projectile(
                        s.getDouble("speed", 1.0), s.getDouble("gravity", 0.03),
                        s.getInt("max_lifetime_ticks", 100), s.getString("trail"), s.getString("item"),
                        parseHoming(s.getConfigurationSection("homing")),
                        parseBody(s.getString("body")),
                        parseSpread(s.getConfigurationSection("spread")), fuse,
                        // THE BURST (Solar Flare, the seat's F3, 2026-11-27): OPTIONAL, absent is null (every projectile
                        // before it). Present, its radius and travel_ticks are REQUIRED and its ring and pulse visuals
                        // optional, as a pulses cast's; it is ONE front, so it has no count or interval to author.
                        parseBurst(s.getConfigurationSection("burst")),
                        // THE SCALED BODY (F9): OPTIONAL, absent is 0 (the dropped item every item body was before).
                        s.getDouble("body_scale", 0.0));
            }
            case "dash"       -> new CastSpec.Dash(
                    s.getDouble("distance", 12), s.getDouble("speed", 1.6), s.getDouble("lift", 0.4),
                    parseDashDirection(s.getString("direction", "movement_else_forward")),
                    // Ruling 31 (PLAN-build-system.md section 7.6): no fall damage from the first landing after it.
                    s.getBoolean("safe_landing", false),
                    // Sunder (PLAN-melee-class.md section 8.2, S1): detonated where the leap comes down.
                    parseOnLand(s.getMapList("on_land")));
            // A cast that REPEATS another cast on a clock. `of` is required and is a whitelist:
            // see innerCast below. windup_ticks 0 means the first shot fires on the cast frame.
            // `locked_aim` and `warning:` (2026-11-21, the Knell Beam): both OPTIONAL and default OFF. A warning
            // without locked_aim, or on a volley of anything but a ray, is refused by CastSpec.Volley, by name, so the
            // weapon file is skipped and named rather than drawing a line the shot will not take.
            case "volley"     -> new CastSpec.Volley(
                    s.getInt("windup_ticks", 0), s.getInt("shots", 1), s.getInt("interval_ticks", 1),
                    innerCast(s.getConfigurationSection("of")),
                    s.getBoolean("locked_aim", false), parseWarning(s.getConfigurationSection("warning")));
            // THE CONE (2026-11-22, the Living Lantern). The keys are the schema's own words: `range` as a ray's,
            // `arc_degrees` as melee's (the full angle), `travel_ticks` (0 = instant), and `front`, a visual id drawn
            // each tick as `beam` and `trail` are (optional; ContentValidator resolves it). The three numbers are
            // REQUIRED: a default would be a number nobody ruled, arriving silently in a file that omitted the key.
            // CastSpec.Cone judges the values, by name.
            case "cone"       -> new CastSpec.Cone(reqDouble(s, "range"), reqDouble(s, "arc_degrees"),
                    reqInt(s, "travel_ticks"), s.getString("front"));
            // THE THROWN ORB (Lapis Crystal's D2, 2026-11-23): every key required, CastSpec.Orb refuses a bad value by name.
            case "orb"        -> parseOrb(s);
            // PULSES FROM THE CASTER (Smolder, the seat's Z1, 2026-11-27). The four numbers are REQUIRED (a default would
            // be a number nobody ruled); `ring` (drawn round the ring each tick) and `pulse` (once at each origin) are
            // OPTIONAL visual ids, resolved by ContentValidator. CastSpec.Pulses judges the values, by name.
            // `spiral:` (Smolder, M2, 2026-11-28) is OPTIONAL: arms, turns and the visual drawn at each point, all three
            // required inside it.
            case "pulses"     -> new CastSpec.Pulses(reqInt(s, "count"), reqInt(s, "interval_ticks"),
                    reqDouble(s, "radius"), reqInt(s, "travel_ticks"), s.getString("ring"), s.getString("pulse"),
                    parseSpiralArms(s.getConfigurationSection("spiral")));
            default -> throw new IllegalArgumentException("Unknown cast type: " + type);
        };
    }

    /**
     * A projectile's {@code burst:} block, or null (Solar Flare, F3): ONE pulse front where the shot stops, as a
     * {@link CastSpec.Pulses} of count 1 (its interval is never used and is 1). {@code radius} and {@code travel_ticks}
     * are required, {@code ring} and {@code pulse} optional; a {@code count} or {@code interval_ticks} key is refused by
     * name, because a burst is one front and an authored number nobody could see doing nothing is the worse file.
     */
    /** A pulses cast's {@code spiral:} block (M2), or null: {@code arms}, {@code turns} and {@code visual}, each required. */
    static CastSpec.Pulses.SpiralArms parseSpiralArms(org.bukkit.configuration.ConfigurationSection s) {
        if (s == null) return null;
        if (!s.isString("visual")) {
            throw new IllegalArgumentException("a pulses spiral needs 'visual:', the visual drawn at each of its points");
        }
        return new CastSpec.Pulses.SpiralArms(reqInt(s, "arms"), reqDouble(s, "turns"), s.getString("visual"));
    }

    static CastSpec.Pulses parseBurst(org.bukkit.configuration.ConfigurationSection s) {
        if (s == null) return null;
        for (String refused : java.util.List.of("count", "interval_ticks")) {
            if (s.contains(refused)) {
                throw new IllegalArgumentException("a projectile's burst is ONE pulse front: remove '" + refused + "'");
            }
        }
        return new CastSpec.Pulses(1, 1, reqDouble(s, "radius"), reqInt(s, "travel_ticks"), s.getString("ring"),
                s.getString("pulse"));
    }

    /**
     * THE ORB'S BODY KIND (Ben's item 3, 2026-11-23, PR 2): {@code body: item} (the default, D2's shape) needs {@code item:};
     * {@code body: block} needs a {@code block:} section and takes no {@code item:}. Each refuses the other's key, so a file
     * cannot carry a look it does not use. Whether {@code block.id} names a block is the load check's
     * ({@code ContentValidator.validateOrbBodies}), because it needs the server's material registry.
     *
     * <pre>
     * body: block
     * block:
     *   id: lapis_block
     *   scale: 0.5          # its edge in blocks, centred on the orb's position
     *   glow: "#2A5CFF"     # OPTIONAL: the glow outline's colour (absent: no glow)
     *   bob_blocks: 0.1     # OPTIONAL, and the four below: its look at rest (0 or absent: none), on a beat
     *   bob_period_ticks: 60
     *   turn_ticks: 120
     *   beat_ticks: 10
     *   particles: crystal_motes   # a visual presented at the rest point each beat
     * </pre>
     *
     * <p>{@code CastSpec.OrbBlock} refuses a motion or particles with no beat, and a bob with no period, by name; whether
     * {@code particles} names a loaded visual is the load check's ({@code ContentValidator.validateOrbBodies}).
     */
    static CastSpec.Orb parseOrb(ConfigurationSection s) {
        double range = reqDouble(s, "range");
        int flight = reqInt(s, "flight_ticks");
        int life = reqInt(s, "life_ticks");
        double radius = reqDouble(s, "radius");
        CastSpec.OrbShot shot = parseOrbShot(s.getConfigurationSection("shot"));
        String body = s.getString("body", "item");
        return switch (body) {
            case "item" -> {
                if (s.contains("block")) throw new IllegalArgumentException("orb body: item takes no 'block:' section");
                yield new CastSpec.Orb(range, flight, life, radius, req(s, "item"), shot, null);
            }
            case "block" -> {
                if (s.contains("item")) throw new IllegalArgumentException("orb body: block takes no 'item:'");
                ConfigurationSection b = s.getConfigurationSection("block");
                if (b == null) throw new IllegalArgumentException("orb body: block needs a 'block:' section (id, scale)");
                yield new CastSpec.Orb(range, flight, life, radius, null, shot,
                        new CastSpec.OrbBlock(req(b, "id"), reqDouble(b, "scale"), b.getString("glow"),
                                b.getDouble("bob_blocks", 0), b.getInt("bob_period_ticks", 0), b.getInt("turn_ticks", 0),
                                b.getInt("beat_ticks", 0), b.getString("particles")));
            }
            default -> throw new IllegalArgumentException("orb body must be 'item' or 'block', was '" + body + "'");
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
     * <p>{@code projectile} is admitted on day one and exercised by {@code volley_stone} (deleted 2026-11), not merely
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
    /**
     * A volley's warning line, {@code warning: { beam: <visual id>, flash_at: [ticks] }}, or null when absent. The
     * keys are the Knell mob's own names for the same thing ({@code flash_at}) and the ray's for a drawn line
     * ({@code beam}). A section with a key missing is refused by {@code CastSpec.Volley.Warning}, by name.
     */
    static CastSpec.Volley.Warning parseWarning(ConfigurationSection s) {
        if (s == null) return null;
        // getIntegerList, as MobLoader reads the Knell mob's own flash_at.
        return new CastSpec.Volley.Warning(s.getString("beam"), s.getIntegerList("flash_at"));
    }

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
            case CastSpec.Vanilla ignored -> throw notRepeatable("vanilla",
                    "it is vanilla's own bow or crossbow shot, which we never cast; it belongs only under a weapon's"
                            + " shot trigger");
            case CastSpec.Dash ignored -> throw notRepeatable("dash",
                    "a dash's direction is resolved before the cast is dispatched and only for an"
                            + " OUTER dash, so a repeated one would go the wrong way rather than fail");
            case CastSpec.Volley ignored -> throw notRepeatable("volley",
                    "each shot would start its own burst and the count would multiply every interval");
            case CastSpec.Orb ignored -> throw notRepeatable("orb", "one orb per caster: a repeated throw replaces itself");
            case CastSpec.Summon ignored -> throw notRepeatable("summon", "one summon per weapon: a repeated cast replaces itself");
            case CastSpec.DirectSummon ignored -> throw notRepeatable("direct_summon", "an order is one click: a volley of orders gives one");
            case CastSpec.Cone ignored -> throw notRepeatable("cone",
                    "a cone already travels over ticks from a line captured at the click; repeating it is a volley of"
                            + " fronts nobody has ruled");
            case CastSpec.Pulses ignored -> throw notRepeatable("pulses",
                    "pulses already repeat on their own clock; a volley of them is a run of runs nobody has ruled");
        }
        return inner;
    }

    /**
     * A projectile's {@code homing:} block -- the Dragon's Plume's chase, and the first thing in
     * this grammar to map onto {@link CastSpec.Homing}.
     *
     * <p><b>ABSENT MEANS NULL, AND THAT IS WHAT MAKES THIS NOT A CHANGE TO ANY SHIPPED WEAPON.</b>
     * {@code hunters_bow} (deleted 2026-11), {@code ember_staff} (deleted 2026-11), {@code flint_staff}, {@code emberblade} (deleted 2026-11) and
     * {@code volley_stone} (deleted 2026-11)'s inner projectile all author no such block, get null, and behave
     * byte-identically to before this line existed. Slice F built the core shape and the flight's
     * homing branch; <b>until now nothing could put a number into it from a file</b>, so
     * {@code CastSpec.Homing} existed and was unreachable from content.
     *
     * <h2>ALL THREE KEYS ARE REQUIRED, AND THAT IS THE RECORD'S OWN RULE RATHER THAN A CHOICE MADE
     * HERE</h2>
     *
     * <p>{@code CastSpec.Homing}'s javadoc states it: <i>"the record deliberately holds no
     * defaults: a caller states all three or authors no homing."</i> The numbers are
     * {@code PLAN-dragons-plume.md} §5's, carried <b>INHERITED AND UNJUDGED</b> from another game
     * with a gate row each -- and <b>a default would be a fourth number nobody ruled</b>, arriving
     * silently in any file that omitted a key. That is exactly the descent-launders shape
     * {@code CLAUDE.md} records: a placeholder becomes a precedent because nobody re-decides an
     * inherited figure.
     *
     * <p><b>So a partial block is a NAMED, SKIPPED FILE and never a half-homing bolt.</b> The
     * loader's {@code catch(RuntimeException)} turns the throw into exactly that, which is this
     * grammar's contract for every malformed field.
     *
     * <p><b>THE VALUES ARE JUDGED IN {@code core}, NOT HERE</b>, and the split is deliberate: this
     * asks whether the three keys are PRESENT, a question about a file; {@link CastSpec.Homing}'s
     * compact constructor asks whether the numbers MEAN anything, a question a unit test reaches
     * without a server. Both throws land in the same {@code catch} and read the same to an operator.
     */
    /**
     * The body kinds {@code body:} may name: {@code arrow} (the Dragon's Plume) and {@code fireball}
     * (the Blaze King's Staff, LEGACY-B).
     *
     * <p><b>THIS WAS A SET OF ONE, AND IT SAID WHAT WOULD HAPPEN THE DAY A SECOND BODY EXISTED</b>:
     * it becomes a set, and {@code CombatWorld.spawnBoltMarker} grows the id as a parameter. Both
     * happened in LEGACY-B. <b>The set here and the switch in {@code PaperCombatWorld.spawnBoltMarker}
     * are one list in two files</b>: an id accepted here that the adapter cannot build would reach
     * its {@code default} arm and throw on the cast, so a third kind is added to both or to neither.
     *
     * <p>A typo -- {@code body: fireballl} -- is a NAMED, SKIPPED FILE, which is this grammar's
     * contract for every malformed field, rather than a bolt that silently renders no body at all.
     *
     * <p>Absent means null, which is the ordinary case for every projectile in the repo.
     */
    private static String parseBody(String raw) {
        if (raw == null) return null;
        String body = raw.toLowerCase(Locale.ROOT);
        if (!BODIES.contains(body)) {
            throw new IllegalArgumentException(
                    "Unknown projectile body '" + raw + "'; the bodies are " + BODIES);
        }
        return body;
    }

    /** {@link #parseBody}'s set, in a fixed order so the refusal message reads the same every boot. */
    private static final List<String> BODIES = List.of("arrow", "fireball");

    private static CastSpec.Homing parseHoming(ConfigurationSection s) {
        if (s == null) return null;
        return new CastSpec.Homing(reqDouble(s, "lerp"), reqDouble(s, "activation_blocks"),
                reqDouble(s, "search_radius"));
    }

    /**
     * The {@code spread:} block, or null for a projectile that fires ONE body down the aim vector.
     *
     * <p><b>Presence only, and the values are judged in {@code core}</b> -- the split
     * {@link #parseHoming} already uses, and for the stated reason: a value rule belongs where a
     * unit test reaches it without a server, and a question about what is in a FILE belongs here.
     *
     * <p><b>Both keys are required because {@link CastSpec.Spread} holds no defaults.</b> A count
     * with no angle would resolve to a ring at zero degrees -- N bodies down one line, advertised
     * on the tooltip as a spread -- and an angle with no count would resolve to nothing at all.
     * Neither has a reading worth inventing, so both are {@code reqInt}/{@code reqDouble} and a
     * half-authored block is a named, skipped file.
     */
    private static CastSpec.Spread parseSpread(ConfigurationSection s) {
        if (s == null) return null;
        // rotation_degrees is OPTIONAL (2026-11-22, the seat's ruling 1): absent is 0, today's ring, so every file written
        // before it is byte-identical in behaviour. Present, it must be a number (reqDouble), never silently 0.
        double rotation = s.contains("rotation_degrees") ? reqDouble(s, "rotation_degrees") : 0.0;
        return new CastSpec.Spread(reqInt(s, "count"), reqDouble(s, "angle_degrees"), rotation);
    }

    /**
     * An int that has no sensible default. {@link #reqDouble}'s sibling, same contract.
     *
     * <p>An INT rather than a rounded double, for {@code QuiverSize}'s reason: a body count is a
     * count of bodies. {@code getInt} on an authored {@code 7.5} truncates silently, so the
     * fractional form is refused here rather than becoming a seven nobody typed.
     */
    private static int reqInt(ConfigurationSection s, String path) {
        if (!s.isSet(path)) {
            throw new IllegalArgumentException("Missing required field: " + s.getName() + "." + path);
        }
        double raw = s.getDouble(path);
        if (raw != Math.rint(raw)) {
            throw new IllegalArgumentException(s.getName() + "." + path + " must be a whole number,"
                    + " got " + raw + "; it counts bodies, and a fraction of a body is not a thing"
                    + " this cast can fire");
        }
        return s.getInt(path);
    }

    /**
     * A double that has no sensible default -- present or the file is named and skipped.
     *
     * <p>{@link #req} for a String, and the same fail-loud contract. {@code isSet} rather than a
     * sentinel return: {@code getDouble} answers {@code 0.0} for both an absent key and an authored
     * {@code 0}, and those are different files.
     */
    private static double reqDouble(ConfigurationSection s, String path) {
        if (!s.isSet(path)) {
            throw new IllegalArgumentException("Missing required field: " + s.getName() + "." + path);
        }
        return s.getDouble(path);
    }

    private static IllegalArgumentException notRepeatable(String type, String why) {
        return new IllegalArgumentException("a volley cannot repeat cast type '" + type + "': " + why
                + ". Only 'ray' and 'projectile' are repeatable");
    }
    /**
     * A dash's {@code on_land}: UNTARGETED effects only (a {@code burst}, a {@code visual}). A landing has no target
     * -- the executor detonates it with an empty target set -- so a top-level {@code damage} or {@code knockback}
     * would load, read as a slam, and hit nobody. Refused here with the fix in the message, rather than shipped
     * silent. Absent means an empty list: every dash before Sunder.
     */
    private static List<EffectSpec> parseOnLand(List<Map<?, ?>> raw) {
        List<EffectSpec> onLand = parseEffects(raw);
        for (EffectSpec e : onLand) {
            if (!(e instanceof EffectSpec.Untargeted)) {
                throw new IllegalArgumentException("on_land takes untargeted effects only (a burst or a visual): a"
                        + " landing has no target, so a top-level '" + e.getClass().getSimpleName().toLowerCase(Locale.ROOT)
                        + "' would hit nobody. Put it inside a burst");
            }
        }
        return onLand;
    }

    private static CastSpec.DashDirection parseDashDirection(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "movement_else_forward" -> CastSpec.DashDirection.MOVEMENT_ELSE_FORWARD;
            case "reverse_facing"        -> CastSpec.DashDirection.REVERSE_FACING;
            case "facing"                -> CastSpec.DashDirection.FACING;
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
            // The void implosion (2026-11-17): core's reusable shape, named by a use. See EffectSpec.Implosion.
            case "implosion" -> parseImplosion(m, type);
            default -> throw new IllegalArgumentException("Unknown effect type: " + type);
        };
    }

    /**
     * {@code type: implosion}: {@code reach} and {@code pull} required, {@code affects} required ({@code mobs} or
     * {@code players}); {@code falloff} ({@code vanilla_explosion}, the default, or {@code flat}), {@code lift} (a
     * multiplier of the push's lift; 0, the default, keeps them on the ground), {@code resistance}
     * ({@code explosion_knockback}, {@code knockback} or {@code none}, the default), {@code visual} and {@code damage}
     * ({@code { amount, element }}) optional. An unknown word in any of the three enums is refused by name.
     * Since 2026-11-18, {@code duration_ticks} (a whole number of ticks, default 1: a single pull) and {@code drag}
     * (blocks per tick, default 0) optional; a duration above 1 is a drag, and the shape refuses one with a pull or a
     * lift, or with no drag, by name.
     */
    private static EffectSpec.Implosion parseImplosion(Map<?, ?> m, String type) {
        double duration = numOr(m, type, "duration_ticks", 1.0);
        if (duration != Math.rint(duration)) {
            throw new IllegalArgumentException("Effect '" + type + "' field 'duration_ticks' must be a whole number of"
                    + " ticks, got: " + m.get("duration_ticks"));
        }
        var shape = new io.github.butterflysmp.rpg.core.shape.Implosion(
                num(m, type, "reach"),
                word(m, type, "falloff", "vanilla_explosion", io.github.butterflysmp.rpg.core.shape.Implosion.Falloff.class),
                num(m, type, "pull"),
                numOr(m, type, "lift", 0.0),
                word(m, type, "resistance", "none", io.github.butterflysmp.rpg.core.shape.Implosion.Resistance.class),
                (int) duration,
                numOr(m, type, "drag", 0.0));
        EffectSpec.Affects affects = word(m, type, "affects", null, EffectSpec.Affects.class);
        EffectSpec.Damage damage = null;
        if (m.get("damage") != null) {
            Map<?, ?> d = mapOf(m, type, "damage");
            damage = new EffectSpec.Damage(num(d, type + ".damage", "amount"), str(d, type + ".damage", "element"));
        }
        return new EffectSpec.Implosion(shape, affects, strOrNull(m, "visual"), damage);
    }

    /** A lower-case content word naming one of {@code kind}'s constants; {@code fallback} when absent, null = required. */
    private static <E extends Enum<E>> E word(Map<?, ?> m, String type, String k, String fallback, Class<E> kind) {
        Object v = m.get(k);
        if (v == null && fallback == null) {
            throw new IllegalArgumentException("Effect '" + type + "' is missing field: " + k);
        }
        String raw = String.valueOf(v == null ? fallback : v);
        for (E e : kind.getEnumConstants()) {
            if (e.name().toLowerCase(Locale.ROOT).equals(raw)) return e;
        }
        List<String> known = new java.util.ArrayList<>();
        for (E e : kind.getEnumConstants()) known.add(e.name().toLowerCase(Locale.ROOT));
        throw new IllegalArgumentException("Effect '" + type + "' field '" + k + "' must be one of " + known
                + ", got: " + raw);
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
