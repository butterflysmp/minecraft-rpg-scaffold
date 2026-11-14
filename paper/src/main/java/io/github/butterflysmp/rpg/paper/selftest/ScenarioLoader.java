package io.github.butterflysmp.rpg.paper.selftest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Reads the scenarios out of the jar: {@code selftest/index.yml} names the gates in BOOT ORDER, and each
 * {@code selftest/GATE-<gate>.yml} holds that gate's {@code setup:} block and one entry per row, in file order.
 *
 * <p><b>A malformed step is REFUSED at load, by name, and the rest of its gate still loads</b> -- the immune-key
 * shape (`StatusLoader`). A scenario that silently dropped a step would drive a different row than the one its key
 * names, and nothing downstream could tell.
 */
public final class ScenarioLoader {

    private ScenarioLoader() {}

    /** Every gate's scenarios, in boot order; each gate's list starts with its setup (if any). */
    public record Loaded(Map<String, List<Scenario>> gates, List<String> refusals) {
        public int rows() {
            int n = 0;
            for (List<Scenario> scenarios : gates.values()) {
                for (Scenario s : scenarios) if (!s.row().equals(Scenario.SETUP)) n++;
            }
            return n;
        }
    }

    /** {@code resource} opens a jar resource by path, or returns null when it is absent. */
    public static Loaded load(Function<String, InputStream> resource) {
        List<String> refusals = new ArrayList<>();
        Map<String, List<Scenario>> gates = new LinkedHashMap<>();
        YamlConfiguration index = read(resource, "selftest/index.yml", refusals);
        if (index == null) return new Loaded(gates, refusals);
        for (String gate : index.getStringList("gates")) {
            YamlConfiguration file = read(resource, "selftest/GATE-" + gate + ".yml", refusals);
            if (file != null) gates.put(gate, parse(gate, file, refusals));
        }
        return new Loaded(gates, refusals);
    }

    /** One gate file. Keys in file order; {@code setup} first whatever its position. */
    public static List<Scenario> parse(String gate, YamlConfiguration file, List<String> refusals) {
        List<Scenario> out = new ArrayList<>();
        for (String key : file.getKeys(false)) {
            List<Scenario.Step> steps = new ArrayList<>();
            boolean ok = true;
            for (Object raw : file.getList(key, List.of())) {
                if (!(raw instanceof Map<?, ?> map) || map.size() != 1) {
                    refusals.add("Refusing selftest/GATE-" + gate + ".yml#" + key
                            + ": a step must be one 'verb: value' pair, got " + raw);
                    ok = false;
                    continue;
                }
                var entry = map.entrySet().iterator().next();
                String verbName = String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT);
                Scenario.Verb verb;
                try {
                    verb = Scenario.Verb.valueOf(verbName);
                } catch (IllegalArgumentException e) {
                    refusals.add("Refusing selftest/GATE-" + gate + ".yml#" + key + ": unknown verb '" + entry.getKey() + "'");
                    ok = false;
                    continue;
                }
                steps.add(new Scenario.Step(verb, entry.getValue() == null ? "" : String.valueOf(entry.getValue())));
            }
            // A row with a refused step is not loaded at all: running the steps that DID parse would drive a
            // different row than the key names.
            if (!ok) continue;
            Scenario scenario = new Scenario(gate, key, steps);
            if (key.equals(Scenario.SETUP)) out.addFirst(scenario);
            else out.add(scenario);
        }
        return out;
    }

    private static final Pattern HEADING = Pattern.compile("(?m)^### (\\S+) ");

    /** The row ids a gate file declares: every {@code ### <id> } heading. */
    public static Set<String> headings(String gateMarkdown) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        Matcher m = HEADING.matcher(gateMarkdown);
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    /** Scenario keys that name no row of the gate. Empty is the only correct answer. */
    public static List<String> unpaired(List<Scenario> scenarios, Set<String> headings) {
        List<String> missing = new ArrayList<>();
        for (Scenario s : scenarios) {
            if (!s.row().equals(Scenario.SETUP) && !headings.contains(s.row())) missing.add(s.row());
        }
        return missing;
    }

    private static YamlConfiguration read(Function<String, InputStream> resource, String path, List<String> refusals) {
        InputStream in = resource.apply(path);
        if (in == null) {
            refusals.add("Refusing selftest: " + path + " is not in the jar");
            return null;
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            refusals.add("Refusing selftest: " + path + " could not be read (" + e.getMessage() + ")");
            return null;
        }
    }
}
