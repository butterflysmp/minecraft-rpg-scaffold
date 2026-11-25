package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ONE COMMAND, SEVERAL GATES (the seat's ruling of 2026-11-24, G1-G6): {@code /rpg selftest <gate>+<gate>[+...]} runs the
 * named gates in the order typed as ONE run (one snapshot, one restore, one set of pad tickets, released once). Pure; the
 * service asks it. The gate argument is a Brigadier {@code word()}, whose unquoted characters are {@code 0-9 A-Z a-z _ - .
 * +} (READ by javap, {@code StringReader.isAllowedInUnquotedString}, brigadier 1.3.10), so {@code +} reaches here and a
 * comma would not.
 */
final class SelfTestGateList {

    private SelfTestGateList() {}

    /** The gates in the order typed, or the refusal (G3) and no gates. */
    record Parsed(List<String> gates, String refusal) {}

    static boolean isList(String typed) {
        return typed.contains("+");
    }

    /**
     * G3: refused before anything starts, with the reason, for an empty part between two {@code +}, {@code all} in a
     * combination, an unknown gate (naming the gates there are), or a gate named twice.
     */
    static Parsed parse(String typed, Collection<String> known) {
        String[] parts = typed.split("\\+", -1);
        Set<String> seen = new LinkedHashSet<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                return refused("Refusing: an empty gate name between two + in '" + typed + "'.");
            }
        }
        for (String part : parts) {
            if (part.equals("all")) {
                return refused("Refusing: 'all' cannot be combined with other gates ('" + typed + "').");
            }
            if (!known.contains(part)) {
                return refused("Refusing: no selftest gate '" + part + "' in '" + typed + "'. Gates: " + known);
            }
            if (!seen.add(part)) {
                return refused("Refusing: gate '" + part + "' is named twice in '" + typed + "'.");
            }
        }
        return new Parsed(List.copyOf(seen), null);
    }

    private static Parsed refused(String why) {
        return new Parsed(List.of(), why);
    }

    /** G1, G2: each gate's own scenarios (its setup, then its rows, as the loader orders them), gate by gate as typed. */
    static List<Scenario> queue(List<String> gates, Map<String, List<Scenario>> loaded) {
        List<Scenario> out = new ArrayList<>();
        for (String g : gates) out.addAll(loaded.get(g));
        return List.copyOf(out);
    }

    /** No gate:row syntax (G5): a row is given only with one gate. Null when the gate argument is one gate. */
    static String rowRefusal(String typed) {
        return isList(typed) ? "Refusing: a row needs one gate, not '" + typed + "'." : null;
    }

    /**
     * G4: a step that SKIPS in a gate's SETUP ends the whole run (abort, restore, release), because the state after it is
     * unknown; a row's skip moves on to the next row, as it always has.
     */
    static boolean skipEndsRun(Scenario s) {
        return s.row().equals(Scenario.SETUP);
    }

    static String setupAbort(String gate, String why) {
        return "ABORTED setup of " + gate + ": " + why;
    }
}
