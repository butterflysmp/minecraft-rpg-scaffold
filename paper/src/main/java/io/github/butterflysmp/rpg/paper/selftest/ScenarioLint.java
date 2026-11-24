package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * THE SCENARIO LINT (the seat's brief A, 2026-11-23): what a scenario file can be checked for before any boot, read off
 * its steps alone. Every rule is one the boot of {@code e774d4b8} ran into, and the seat had read the scenario and passed
 * all of them. It is REQUIRED green for {@code GATE-enchant-columns.yml}; every older gate's findings are REPORTED, not
 * fixed ({@code SelfTestScenariosTest}).
 *
 * <p>Pure: a list of findings from a list of scenarios. It never judges a reading, and it cannot see the world: a row
 * that passes it can still meet a wall. That is the preflight's to catch.
 */
final class ScenarioLint {

    private ScenarioLint() {}

    enum Rule {
        /** A row gives an item ({@code rpg give}) or selects an empty slot ({@code hold: empty}) before clearing the inventory. */
        HOTBAR,
        /** A press follows a turn with no {@code wait:} between: the press reads the facing before the turn (EC12). */
        TURN_THEN_PRESS,
        /** A probe's mob stands beyond the probe's reach, less one block of margin (EC8's F at 30, a reach of 16). */
        PROBE_REACH,
        /** A row presses without turning him or holding a weapon itself, probes or aims at a mob it did not summon, or chains. */
        SELF_STAGED,
        /** A summon outside the ground the gate's preflight measures, or summons in a gate that declares no preflight. */
        GROUND,
        /** {@code rpg enchant candidate <s>} for a slot the cleared item does not have yet ({@code EnchantState.addCandidate}). */
        ENCHANT_ORDER
    }

    record Finding(String gate, String row, Rule rule, String detail) {
        String line() {
            return "LINT " + gate + "#" + row + " " + rule + " " + detail;
        }
    }

    /** A summon's offset from where the row stands him, and its scoreboard tags. */
    record Summon(double dx, double dy, double dz, List<String> tags) {}

    private static final Pattern SUMMON = Pattern.compile(
            "^summon\\s+\\S+\\s+(~\\S*)\\s+(~\\S*)\\s+(~\\S*)(?:\\s+(.*))?$");
    private static final Pattern TAGS = Pattern.compile("Tags:\\[([^\\]]*)\\]");
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");
    /** A {@code tp} that carries a rotation: {@code tp @s <x> <y> <z> <yaw> <pitch>}, alone or after {@code run}. */
    private static final Pattern TP_TURN = Pattern.compile("(?:^|\\s)tp\\s+@s\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+");
    private static final Pattern CANDIDATE = Pattern.compile("^rpg enchant candidate\\s+(\\d+)\\s+\\S+$");

    /** A relative summon's offset and tags; empty for anything but {@code summon <type> ~ ~ ~ [nbt]}. */
    static Optional<Summon> summon(String cmd) {
        Matcher m = SUMMON.matcher(cmd.trim());
        if (!m.matches()) return Optional.empty();
        double[] d = new double[3];
        for (int i = 0; i < 3; i++) {
            String rel = m.group(i + 1).substring(1);
            try {
                d[i] = rel.isEmpty() ? 0 : Double.parseDouble(rel);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        List<String> tags = new ArrayList<>();
        if (m.group(4) != null) {
            Matcher t = TAGS.matcher(m.group(4));
            if (t.find()) {
                Matcher q = QUOTED.matcher(t.group(1));
                while (q.find()) tags.add(q.group(1));
            }
        }
        return Optional.of(new Summon(d[0], d[1], d[2], List.copyOf(tags)));
    }

    /** A step that turns him: an aim of any kind, a return to the run's start, or a {@code tp} with a rotation. */
    static boolean turns(Scenario.Step step) {
        return switch (step.verb()) {
            case AIM, AIM_BLOCK, AIM_PATCH, AIM_SHAPE, RETURN -> true;
            case CMD -> TP_TURN.matcher(step.arg().trim()).find() || step.arg().contains(" facing ");
            default -> false;
        };
    }

    /** A step that presses: fires, swings, attacks, digs or shoots, reading his facing as it stands. */
    static boolean presses(Scenario.Verb verb) {
        return switch (verb) {
            case USE, RELOAD, SWING, ATTACK, DIG, STRIKE_LOOK, SHOOT_ARROW -> true;
            default -> false;
        };
    }

    /** Every finding in one gate's scenarios, in file order (setup first, as the loader puts it). */
    static List<Finding> lint(String gate, List<Scenario> scenarios) {
        List<Finding> out = new ArrayList<>();
        Optional<SelfTestPreflight.Box> box = Optional.empty();
        boolean pad = false;
        for (Scenario s : scenarios) {
            if (!s.row().equals(Scenario.SETUP)) continue;
            for (Scenario.Step st : s.steps()) {
                if (st.verb() == Scenario.Verb.PREFLIGHT) box = SelfTestPreflight.parse(st.arg());
                if (st.verb() == Scenario.Verb.PAD) pad = true;
            }
        }
        // THE PAD (the seat's P9, 2026-11-23): a gate that stands on it measures, and summons, only inside its interior.
        if (pad && box.isPresent()) {
            SelfTestPreflight.Box b = box.get();
            if (!SelfTestPadShape.inInterior(b.x1(), b.z1()) || !SelfTestPadShape.inInterior(b.x2(), b.z2())) {
                out.add(new Finding(gate, Scenario.SETUP, Rule.GROUND, "the preflight's " + b.label()
                        + " is not inside the pad's interior"));
            }
        }
        boolean anySummon = false;
        for (Scenario s : scenarios) {
            anySummon |= row(gate, s, box, pad, out);
        }
        if (anySummon && box.isEmpty()) {
            out.add(new Finding(gate, Scenario.SETUP, Rule.GROUND, "the gate summons mobs and declares no preflight"));
        }
        return List.copyOf(out);
    }

    /** One row's findings, added to {@code out}; true when the row summons anything. */
    private static boolean row(String gate, Scenario s, Optional<SelfTestPreflight.Box> box, boolean pad, List<Finding> out) {
        String r = s.row();
        boolean cleared = false;
        boolean turned = false;
        boolean held = false;
        boolean turnPending = false;
        boolean summoned = false;
        boolean pressed = false;
        int enchantSlots = -1;   // unknown until a clear, then the slots the dev command has built
        Map<String, double[]> mobs = new HashMap<>();
        for (Scenario.Step st : s.steps()) {
            String a = st.arg().trim();
            Scenario.Verb v = st.verb();
            if (v == Scenario.Verb.AFTER) {
                out.add(new Finding(gate, r, Rule.SELF_STAGED, "chains on " + a + ": it reads what that row left"));
            }
            if (v == Scenario.Verb.CLEAR_INVENTORY) cleared = true;
            if (v == Scenario.Verb.HOLD && a.equals("empty") && !cleared) {
                out.add(new Finding(gate, r, Rule.HOTBAR, "hold: empty before clear_inventory"));
            }
            if (v == Scenario.Verb.CMD && a.startsWith("rpg give ") && !cleared) {
                out.add(new Finding(gate, r, Rule.HOTBAR, "'" + a + "' before clear_inventory"));
            }
            if ((v == Scenario.Verb.HOLD || v == Scenario.Verb.HOLD_ONLY) && !a.equals("empty")) held = true;
            if (v == Scenario.Verb.WAIT && Long.parseLong(a) >= 1) turnPending = false;
            if (turns(st)) {
                turned = true;
                turnPending = true;
            }
            if (v == Scenario.Verb.CMD) {
                if (a.equals("rpg enchant clear")) enchantSlots = 0;
                Matcher c = CANDIDATE.matcher(a);
                if (c.matches() && enchantSlots >= 0) {
                    int slot = Integer.parseInt(c.group(1));
                    if (slot > enchantSlots) {
                        out.add(new Finding(gate, r, Rule.ENCHANT_ORDER, "'" + a + "' after rpg enchant clear: the item"
                                + " has " + enchantSlots + " slot(s), and only slot " + enchantSlots + " can be added"));
                    } else {
                        enchantSlots = Math.max(enchantSlots, slot + 1);
                    }
                }
                Optional<Summon> sm = summon(a);
                if (sm.isPresent()) {
                    summoned = true;
                    Summon m = sm.get();
                    for (String tag : m.tags()) mobs.put(tag, new double[] {m.dx(), m.dy(), m.dz()});
                    if (box.isPresent() && !inside(box.get(), m)) {
                        out.add(new Finding(gate, r, Rule.GROUND, "summon at " + fmt(m.dx()) + " east " + fmt(m.dz())
                                + " south, outside the preflight's " + box.get().label()));
                    }
                    if (pad && !SelfTestPadShape.inInterior((int) Math.floor(m.dx()), (int) Math.floor(m.dz()))) {
                        out.add(new Finding(gate, r, Rule.GROUND, "summon at " + fmt(m.dx()) + " east " + fmt(m.dz())
                                + " south, outside the pad's interior"));
                    }
                }
            }
            if (v == Scenario.Verb.SPAWN) {
                summoned = true;
                SelfTestSpawn.parse(a).ifPresent(spec -> mobs.put(spec.tag(), new double[] {0, 0, 0}));
            }
            if (presses(v)) {
                if (turnPending) {
                    out.add(new Finding(gate, r, Rule.TURN_THEN_PRESS, v.name().toLowerCase(Locale.ROOT)
                            + " in the same pass as the turn before it: wait at least 1 tick between"));
                }
                if (!pressed && !turned) {
                    out.add(new Finding(gate, r, Rule.SELF_STAGED, "presses without turning him in the row"));
                }
                if (!pressed && !held) {
                    out.add(new Finding(gate, r, Rule.SELF_STAGED, "presses without holding a weapon in the row"));
                }
                pressed = true;
            }
            if (v == Scenario.Verb.PROBE || v == Scenario.Verb.AIM) {
                String tag;
                int reach;
                if (v == Scenario.Verb.PROBE) {
                    Optional<SelfTestStaging.ProbeArg> pa = SelfTestStaging.probeArg(a);
                    if (pa.isEmpty()) continue;   // the loader refuses it by name
                    tag = pa.get().tag();
                    reach = pa.get().reach();
                } else {
                    tag = a;
                    reach = SelfTestStaging.PROBE_DEFAULT_REACH;
                }
                double[] d = mobs.get(tag);
                if (d == null) {
                    out.add(new Finding(gate, r, Rule.SELF_STAGED, v.name().toLowerCase(Locale.ROOT) + " " + tag
                            + ": no mob of that tag was summoned in the row"));
                } else {
                    double far = Math.max(Math.abs(d[0]), Math.max(Math.abs(d[1]), Math.abs(d[2])));
                    if (far + 1 > reach) {
                        out.add(new Finding(gate, r, Rule.PROBE_REACH, v.name().toLowerCase(Locale.ROOT) + " " + tag
                                + ": " + fmt(far) + " out, reach " + reach + " (one block of margin)"));
                    }
                }
            }
        }
        return summoned;
    }

    private static boolean inside(SelfTestPreflight.Box b, Summon m) {
        return m.dx() >= b.x1() && m.dx() <= b.x2() && m.dz() >= b.z1() && m.dz() <= b.z2();
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
