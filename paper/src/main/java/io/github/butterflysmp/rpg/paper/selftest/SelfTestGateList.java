package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ONE COMMAND, SEVERAL GATES (the seat's ruling of 2026-11-24, G1-G6): {@code /rpg selftest <gate>+<gate>[+...]} runs the
 * named gates in the order typed as ONE run (one snapshot, one restore, one set of pad tickets, released once). Pure; the
 * service asks it. The gate argument is a Brigadier {@code word()}, whose unquoted characters are {@code 0-9 A-Z a-z _ - .
 * +} (READ by javap, {@code StringReader.isAllowedInUnquotedString}, brigadier 1.3.10), so {@code +} and {@code .} reach
 * here and a comma or a colon would not.
 *
 * <p><b>NAMED ROWS BY REFERENCE (the seat's brief of 2026-11-25, which revises G5's "no gate:row syntax").</b> An item of
 * the list may be {@code <gate>.<ROW>[.<ROW>...]}: that gate's setup, then ONLY the rows named, in the GATE's order (CC'S
 * CALL; the alternative is the order typed), each spelled as the gate spells it (any letter case, as one gate's row is).
 * So {@code broken-bottle+summoner.SM9R.SM16R2+armor-sets.AS15R} runs the bottle's gate whole and three owed rows of two
 * others, by reference: their steps stay in their own files, never copied. A dot, because the word cannot carry a colon.
 */
final class SelfTestGateList {

    private SelfTestGateList() {}

    /**
     * The gates in the order typed, the rows named for each gate given by reference (absent: the whole gate), or the
     * refusal (G3) and no gates.
     */
    record Parsed(List<String> gates, Map<String, List<String>> rows, String refusal) {
        Parsed {
            gates = List.copyOf(gates);
            rows = Map.copyOf(rows);
        }
    }

    static boolean isList(String typed) {
        return typed.contains("+");
    }

    /**
     * G3: refused before anything starts, with the reason, for an empty part between two {@code +}, {@code all} in a
     * combination, an unknown gate (naming the gates there are), or a gate named twice (whole or by reference). A
     * reference is refused for an empty row name, a row the gate does not have, its setup, a row named twice, and a
     * CHAINED row (its first step is {@code after:}), which run alone would only ever skip.
     */
    static Parsed parse(String typed, Map<String, List<Scenario>> loaded) {
        String[] parts = typed.split("\\+", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                return refused("Refusing: an empty gate name between two + in '" + typed + "'.");
            }
        }
        Set<String> seen = new LinkedHashSet<>();
        Map<String, List<String>> rows = new LinkedHashMap<>();
        for (String part : parts) {
            String[] named = part.split("\\.", -1);
            String gate = named[0];
            if (gate.equals("all")) {
                return refused("Refusing: 'all' cannot be combined with other gates ('" + typed + "').");
            }
            List<Scenario> scenarios = loaded.get(gate);
            if (scenarios == null) {
                return refused("Refusing: no selftest gate '" + gate + "' in '" + typed + "'. Gates: " + loaded.keySet());
            }
            if (!seen.add(gate)) {
                return refused("Refusing: gate '" + gate + "' is named twice in '" + typed + "'.");
            }
            if (named.length == 1) continue;
            Set<String> mine = new LinkedHashSet<>();
            for (int i = 1; i < named.length; i++) {
                String typedRow = named[i];
                if (typedRow.isEmpty()) {
                    return refused("Refusing: an empty row name after '" + gate + "' in '" + typed + "'.");
                }
                if (typedRow.equalsIgnoreCase(Scenario.SETUP)) {
                    return refused("Refusing: 'setup' runs with every row of '" + gate + "'; name a row ('" + typed + "').");
                }
                Optional<String> row = SelfTestService.matchRow(scenarios, typedRow);
                if (row.isEmpty()) {
                    return refused("Refusing: no row '" + typedRow + "' in selftest gate '" + gate + "' ('" + typed + "').");
                }
                if (!mine.add(row.get())) {
                    return refused("Refusing: row '" + row.get() + "' of '" + gate + "' is named twice in '" + typed + "'.");
                }
                Scenario s = scenarios.stream().filter(x -> x.row().equals(row.get())).findFirst().orElseThrow();
                if (SelfTestStaging.chained(s)) {
                    return refused("Refusing: row '" + row.get() + "' of '" + gate + "' runs only after its chain ('after: "
                            + s.steps().get(0).arg() + "'); name its gate whole ('" + typed + "').");
                }
            }
            rows.put(gate, List.copyOf(mine));
        }
        return new Parsed(List.copyOf(seen), rows, null);
    }

    private static Parsed refused(String why) {
        return new Parsed(List.of(), Map.of(), why);
    }

    /**
     * G1, G2: gate by gate as typed, each gate's own scenarios as the loader orders them: its setup, then its rows, or,
     * for a gate given by reference, its setup and only the rows named.
     */
    static List<Scenario> queue(Parsed parsed, Map<String, List<Scenario>> loaded) {
        List<Scenario> out = new ArrayList<>();
        for (String g : parsed.gates()) {
            List<String> only = parsed.rows().get(g);
            for (Scenario s : loaded.get(g)) {
                if (only == null || s.row().equals(Scenario.SETUP) || only.contains(s.row())) out.add(s);
            }
        }
        return List.copyOf(out);
    }

    /** A row is given as a second word only with one gate; in a list it is given by reference. Null for one gate. */
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
