package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY WEAPON ID A SCENARIO NAMES RESOLVES AGAINST SHIPPED CONTENT (the seat's brief of 2026-11-21, ruling 3, written
 * for the deletion of the four dev weapons).
 *
 * <p>Before this test a scenario naming a weapon that no longer ships failed only at run time, on a server: the
 * {@code rpg give} replied "Unknown weapon ..." and the next {@code hold} skipped the row with "no hotbar slot holds
 * ...". Nothing in the build could see it.
 *
 * <p><b>WHAT IS CHECKED, by step:</b>
 * <ul>
 *   <li>{@code cmd: "rpg give <id>"}: a weapon id, or a shield or accessory id. Shields and accessories take their id
 *       from the FILE NAME ({@code ShieldLoader} and {@code AccessoryLoader}'s {@code idOf}, READ), weapons from their
 *       {@code id:} key. Armor and tools are NOT resolved here, and no shipped scenario gives one: one that does
 *       reddens this test, and the resolver must be widened then, deliberately.</li>
 *   <li>{@code hold: <id>}: a weapon id, except {@code stone} and {@code empty} ({@code SelfTestDrivers.hold}'s two
 *       non-weapon arms).</li>
 *   <li>{@code probe_inventory: <id>}: a weapon id ({@code SelfTestDrivers} matches it with
 *       {@code WeaponItems.weaponId}).</li>
 *   <li>{@code shift_click: "<top|bottom> <id>"}: the id is a weapon id.</li>
 * </ul>
 *
 * <p>Content is read from the SOURCE tree, never the classpath, so a stale copy left in a warm {@code target/} cannot
 * answer for a file that was deleted ({@code ContentFreshnessTest}'s hazard). The scenarios come from the classpath
 * ({@code ScenarioLoader}, as {@link SelfTestScenariosTest} reads them): a clean build is assumed for those.
 */
class SelfTestWeaponIdsTest {

    private static final Path CONTENT = Path.of("src", "main", "resources", "content");
    private static final Pattern WEAPON_ID = Pattern.compile("(?m)^id:\\s*(\\S+)\\s*$");
    private static final Pattern GIVE = Pattern.compile("^rpg give (\\S+)");

    private static ScenarioLoader.Loaded shipped() {
        return ScenarioLoader.load(path -> SelfTestWeaponIdsTest.class.getClassLoader().getResourceAsStream(path));
    }

    private static List<Path> yaml(String kind) {
        try (Stream<Path> files = Files.list(CONTENT.resolve(kind))) {
            return files.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every shipped weapon's {@code id:}, at column 0. */
    static Set<String> weaponIds() {
        Set<String> ids = new TreeSet<>();
        for (Path p : yaml("weapons")) {
            try {
                Matcher m = WEAPON_ID.matcher(Files.readString(p, StandardCharsets.UTF_8));
                assertTrue(m.find(), p + " has no id: line -- the weapon id scan is blind to it");
                ids.add(m.group(1));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return ids;
    }

    /** Shields and accessories: the file name is the id. */
    static Set<String> fileNamedGearIds() {
        Set<String> ids = new TreeSet<>();
        for (String kind : List.of("shields", "accessories")) {
            for (Path p : yaml(kind)) {
                String name = p.getFileName().toString();
                ids.add(name.substring(0, name.length() - ".yml".length()));
            }
        }
        return ids;
    }

    /**
     * The weapon id a step names, or empty for a step that names none. {@code give} is reported with a {@code give }
     * prefix, because it resolves against the wider set.
     */
    static Optional<String> named(Scenario.Step step) {
        String a = step.arg().trim();
        return switch (step.verb()) {
            case CMD -> {
                Matcher m = GIVE.matcher(a);
                yield m.find() ? Optional.of("give " + m.group(1)) : Optional.empty();
            }
            case HOLD -> a.equals("stone") || a.equals("empty") ? Optional.empty() : Optional.of(a);
            case PROBE_INVENTORY -> Optional.of(a);
            case SHIFT_CLICK -> {
                String[] parts = a.split("\\s+");
                yield Optional.of(parts.length == 2 ? parts[1] : "<unparsed shift_click '" + a + "'>");
            }
            default -> Optional.empty();
        };
    }

    /** Every named id that does not resolve, as {@code gate#row id}. */
    static List<String> unresolved(Iterable<Scenario> scenarios, Set<String> weapons, Set<String> otherGear,
                                   List<String> seen) {
        List<String> bad = new ArrayList<>();
        for (Scenario s : scenarios) {
            for (Scenario.Step step : s.steps()) {
                Optional<String> id = named(step);
                if (id.isEmpty()) continue;
                String n = id.get();
                seen.add(s.gate() + "#" + s.row() + " " + n);
                boolean give = n.startsWith("give ");
                String bare = give ? n.substring("give ".length()) : n;
                boolean ok = weapons.contains(bare) || (give && otherGear.contains(bare));
                if (!ok) bad.add(s.gate() + "#" + s.row() + " " + step.verb() + " " + bare);
            }
        }
        return bad;
    }

    /**
     * THE ONE GATE ALLOWED TO NAME A WEAPON THAT DOES NOT SHIP, and only these ids: GATE-remove-dev-weapons.md reads,
     * on purpose, what a deleted id does (a held copy probed, a give refused). Pinned EXACTLY below, so a deleted id
     * cannot creep into any other gate, or into this one beyond the rows that read it.
     */
    static final String DELETION_GATE = "remove-dev-weapons";
    static final List<String> DELETION_GATE_STEPS = List.of(
            "remove-dev-weapons#DW1 PROBE_INVENTORY ironblade", "remove-dev-weapons#DW1 PROBE_INVENTORY emberblade",
            "remove-dev-weapons#DW1 PROBE_INVENTORY hunters_bow", "remove-dev-weapons#DW1 PROBE_INVENTORY quiver_stone",
            "remove-dev-weapons#DW2 CMD ironblade", "remove-dev-weapons#DW2 CMD emberblade",
            "remove-dev-weapons#DW2 CMD hunters_bow", "remove-dev-weapons#DW2 CMD quiver_stone");

    @Test
    void everyWeaponIdAScenarioNamesShips() {
        Set<String> weapons = weaponIds();
        Set<String> other = fileNamedGearIds();
        List<String> seen = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        List<String> deletionGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            List<String> found = unresolved(gate.getValue(), weapons, other, seen);
            if (gate.getKey().equals(DELETION_GATE)) deletionGate.addAll(found); else bad.addAll(found);
        }
        assertEquals(List.of(), bad, "scenario steps naming a weapon that does not ship; each would fail on a server, "
                + "not in the build");
        assertEquals(DELETION_GATE_STEPS, deletionGate,
                "the deletion gate names the four deleted ids in exactly its two reading rows, and nothing else unresolved");
        // THE POSITIVE CONTROL: the scan saw the steps. Every GIVE, HOLD (not stone/empty), PROBE_INVENTORY and
        // SHIFT_CLICK in the shipped scenarios, counted from the files on 2026-11-21 after the deletion:
        // grep -hE '^\s*- (hold|probe_inventory|shift_click):|rpg give ' paper/src/main/resources/selftest/*.yml
        // minus the 13 hold: stone and 1 hold: empty lines.
        assertTrue(seen.size() > 50, "the scan saw only " + seen.size() + " weapon-naming steps: " + seen);
        assertTrue(seen.stream().anyMatch(s -> s.endsWith(" give boltor")), "the control: a give of the Boltor is seen");
        assertTrue(seen.stream().anyMatch(s -> s.endsWith(" bottom boltor") || s.endsWith(" boltor")),
                "the control: a hold or probe of the Boltor is seen");
    }

    /** The resolver CAUSES the condition: a step naming a weapon that does not ship is reported, by verb. */
    @Test
    void aStepNamingAWeaponThatDoesNotShipIsReported() {
        Set<String> weapons = weaponIds();
        assertFalse(weapons.contains("emberblade"), "the fixture: emberblade no longer ships");
        assertTrue(weapons.contains("iron_sword"), "the control: iron_sword ships");
        Scenario s = new Scenario("probe", "X1", List.of(
                new Scenario.Step(Scenario.Verb.CMD, "rpg give emberblade"),
                new Scenario.Step(Scenario.Verb.HOLD, "emberblade"),
                new Scenario.Step(Scenario.Verb.PROBE_INVENTORY, "emberblade"),
                new Scenario.Step(Scenario.Verb.SHIFT_CLICK, "bottom emberblade"),
                new Scenario.Step(Scenario.Verb.CMD, "rpg give iron_sword"),
                new Scenario.Step(Scenario.Verb.HOLD, "iron_sword"),
                new Scenario.Step(Scenario.Verb.HOLD, "stone"),
                new Scenario.Step(Scenario.Verb.HOLD, "empty"),
                new Scenario.Step(Scenario.Verb.CMD, "rpg give shield"),
                new Scenario.Step(Scenario.Verb.CMD, "rpg give brawlers_gauntlet")));
        assertEquals(List.of("probe#X1 CMD emberblade", "probe#X1 HOLD emberblade", "probe#X1 PROBE_INVENTORY emberblade",
                        "probe#X1 SHIFT_CLICK emberblade"),
                unresolved(List.of(s), weapons, fileNamedGearIds(), new ArrayList<>()));
    }
}
