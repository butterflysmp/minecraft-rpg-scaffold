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
        ENCHANT_ORDER,
        /**
         * L1 (the seat's ruling, 2026-11-24): a command that reads or writes the held item through a scheduled task
         * ({@link #readsHeldLater}) followed, with no {@code wait:} between, by a step that changes the held item or the
         * inventory ({@link #changesHeld}). The task runs a tick later and sees the changed hand: MG1's Flint Staff.
         */
        HELD_READ_THEN_CHANGE,
        /**
         * 4d (the seat, 2026-11-24): a row that lets a mob ACT names its staged attack. A {@code cmd: summon} with no
         * {@code NoAI:1b} must carry {@code attack_damage} in its NBT, so what it can deal him is written, not hoped for.
         */
        MOB_ACT_UNSTAGED,
        /** 4d: a {@code probe_summon} names its reach (a number), so a moving summon's line does not depend on where it stands. */
        PROBE_SUMMON_REACH,
        /**
         * X1 (the seat, 2026-11-25): EVERY ROW IS SELF-STAGED. A row that reads a summon ({@link #usesSummon}) with no cast of
         * its own earlier in the row (a {@code use} after a {@code clear_state}) reads one an earlier row left.
         */
        SUMMON_UNSTAGED,
        /**
         * THE LEVEL SEAM (the seat, 2026-11-25, after the boot of {@code 732c4e02}): his RPG level is carried from row to
         * row, and nothing at a row start resets it. Once a row sets a level ({@code rpg playerxp set @s ...}) other than
         * the setup's, every later row that is not chained stages its own. AS13 set 13 and AS14-AS19 ran at it.
         */
        LEVEL_UNSTAGED,
        /**
         * A LEVEL-GATED MENU IS A STAGING HAZARD (the seat, 2026-11-26, after AS15R SKIPPED at a locked {@code /recipes}): a
         * command that opens a level-gated screen ({@code NexusScreens.Screen#unlockLevel}) runs at a staged level at least
         * that screen's, set BEFORE it, in the row or carried. Only commands are read; a station clicked in the hub is not.
         */
        LEVEL_GATED_MENU
    }

    /** The level a step stages: the text after {@code rpg playerxp set @s}, such as {@code 13 levels}. */
    private static final Pattern LEVEL = Pattern.compile("^rpg playerxp set @s\\s+(.+)$");

    /** The level this row leaves him at: its LAST {@code rpg playerxp set @s}, or empty when it sets none. */
    static Optional<String> levelSet(Scenario s) {
        String last = null;
        for (Scenario.Step st : s.steps()) {
            if (st.verb() != Scenario.Verb.CMD) continue;
            Matcher m = LEVEL.matcher(st.arg().trim());
            if (m.matches()) last = m.group(1).trim();
        }
        return Optional.ofNullable(last);
    }

    /**
     * LEVEL_UNSTAGED over one gate, in file order. The baseline is the setup's level (none when the setup sets none, which
     * is then his own level as the run found it). A row starting at a carried level that differs from the baseline, and
     * setting none of its own, is a finding. A chained row ({@code after:}) is not: it reads what the row before it left,
     * on purpose, and SELF_STAGED already names it. CC'S CALL: the row's set may sit anywhere in the row (AS13 sets first).
     */
    private static void levelSeam(String gate, List<Scenario> scenarios, List<Finding> out) {
        String baseline = null;
        String carried = null;
        String from = Scenario.SETUP;
        for (Scenario s : scenarios) {
            Optional<String> set = levelSet(s);
            if (s.row().equals(Scenario.SETUP)) {
                baseline = set.orElse(null);
                carried = baseline;
                continue;
            }
            if (!java.util.Objects.equals(carried, baseline) && set.isEmpty() && !SelfTestStaging.chained(s)) {
                out.add(new Finding(gate, s.row(), Rule.LEVEL_UNSTAGED, "starts at level '" + carried + "', left by "
                        + from + ", not the setup's '" + baseline + "', and stages none of its own"));
            }
            if (set.isPresent()) {
                carried = set.get();
                from = s.row();
            }
        }
    }

    /**
     * LEVEL_GATED_MENU over one gate, in file order and step order, carrying the staged level across rows as the run does.
     * The level is the number of the last {@code rpg playerxp set @s <n> levels} before the step; any other form is
     * unknown, and so is "never set".
     */
    private static void levelGatedMenu(String gate, List<Scenario> scenarios, List<Finding> out) {
        Integer level = null;
        for (Scenario s : scenarios) {
            for (Scenario.Step st : s.steps()) {
                if (st.verb() != Scenario.Verb.CMD) continue;
                String c = st.arg().trim();
                Matcher m = LEVEL.matcher(c);
                if (m.matches()) {
                    Matcher n = Pattern.compile("^(\\d+)\\s+levels?$").matcher(m.group(1).trim());
                    level = n.matches() ? Integer.valueOf(n.group(1)) : null;
                    continue;
                }
                String first = (c.startsWith("/") ? c.substring(1) : c).split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT);
                for (io.github.butterflysmp.rpg.paper.menu.NexusScreens.Screen screen
                        : io.github.butterflysmp.rpg.paper.menu.NexusScreens.Screen.values()) {
                    // NexusScreens.Screen.unlockLevel(): the station's level, or 1 for an ungated screen, which any level meets.
                    int needs = screen.unlockLevel();
                    if (!screen.command().equals(first) || needs <= 1) continue;
                    if (level == null) {
                        out.add(new Finding(gate, s.row(), Rule.LEVEL_GATED_MENU, "'" + first + "' needs level " + needs
                                + ", and no level is staged"));
                    } else if (level < needs) {
                        out.add(new Finding(gate, s.row(), Rule.LEVEL_GATED_MENU, "'" + first + "' needs level " + needs
                                + ", and the staged level is " + level));
                    }
                }
            }
        }
    }

    /**
     * L2: THE ONE LIST OF HELD-ITEM COMMANDS THAT RUN LATER. READ, {@code RpgCommand}: exactly three of its handlers hop
     * through {@code adapters.scheduler().onEntity(player, …)} and touch the hand or the inventory there: {@code enchant}
     * (every op: show, pool, rollstats, clear, candidate, level, active, …; each reads {@code getItemInMainHand()} in the
     * task), {@code durability} (damage, repair, set; the same), and {@code refresh} ({@code GearRefresher.refresh} over
     * his inventory). No other command class hops (READ by grep over {@code paper/.../command/}).
     */
    static boolean readsHeldLater(String cmd) {
        return readsHeldLaterImpl(cmd);
    }

    /** MOB_ACT_UNSTAGED: a vanilla {@code summon} that leaves the mob its AI and names no attack_damage. */
    static boolean mobActsUnstaged(String cmd) {
        String c = cmd.trim().toLowerCase(java.util.Locale.ROOT);
        if (!c.startsWith("summon ")) return false;
        boolean frozen = c.replace(" ", "").contains("noai:1b") || c.replace(" ", "").contains("noai:true");
        return !frozen && !c.contains("attack_damage");
    }

    /**
     * X1: the entity selectors that name a shipped summon, one per summon entity (`SummonLintSelectorsTest` pins it equal to
     * the shipped summons' entities). CC'S CALL: a fixed list, so the lint stays pure (the alternative: pass the registry).
     */
    static final java.util.Set<String> SUMMON_SELECTORS = java.util.Set.of("type=minecraft:wolf");

    /** X1: does this step read a summon: a {@code probe_summon}, or a command whose selector names a summon's entity. */
    static boolean usesSummon(Scenario.Verb v, String a) {
        if (v == Scenario.Verb.PROBE_SUMMON) return true;
        return v == Scenario.Verb.CMD && SUMMON_SELECTORS.stream().anyMatch(a::contains);
    }

    static boolean isPositiveNumber(String a) {
        try {
            return Double.parseDouble(a.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean readsHeldLaterImpl(String cmd) {
        String c = cmd.trim();
        return c.startsWith("rpg enchant ") || c.equals("rpg enchant") || c.startsWith("rpg durability ")
                || c.equals("rpg durability") || c.equals("rpg refresh") || c.startsWith("rpg refresh ");
    }

    /**
     * L1's other half: the steps that change the held item or the inventory AT ONCE, in the pass they run in (READ,
     * {@code SelfTestDrivers}): {@code hold} and {@code hold_only} (the slot), {@code clear_inventory}, {@code drop}
     * ({@code Player#dropItem}), {@code click}, {@code click_material} and {@code shift_click} (a menu moves items); and the
     * commands {@code rpg give}, vanilla {@code give}, {@code clear} and {@code item}, and {@code rpg gearscore} (it writes
     * the held item at once: READ, it does not hop). A scheduled command is NOT a changer: it queues behind the read.
     */
    static boolean changesHeld(Scenario.Verb v, String arg) {
        return switch (v) {
            case HOLD, HOLD_ONLY, CLEAR_INVENTORY, DROP, CLICK, CLICK_MATERIAL, SHIFT_CLICK -> true;
            case CMD -> {
                String c = arg.trim();
                yield c.startsWith("rpg give ") || c.startsWith("give ") || c.startsWith("clear ") || c.equals("clear")
                        || c.startsWith("item ") || c.startsWith("rpg gearscore ");
            }
            default -> false;
        };
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
        levelSeam(gate, scenarios, out);
        levelGatedMenu(gate, scenarios, out);
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
        boolean clearedState = false;   // X1: clear_state seen in this row
        boolean castOwn = false;        // X1: a use after that clear_state: the row's own summon
        int enchantSlots = -1;   // unknown until a clear, then the slots the dev command has built
        String heldReadPending = null;   // L1: the last scheduled held-item command not yet followed by a wait
        Map<String, double[]> mobs = new HashMap<>();
        for (Scenario.Step st : s.steps()) {
            String a = st.arg().trim();
            Scenario.Verb v = st.verb();
            if (heldReadPending != null && changesHeld(v, a)) {
                out.add(new Finding(gate, r, Rule.HELD_READ_THEN_CHANGE, describe(v, a) + " after '" + heldReadPending
                        + "' with no wait: the read runs a tick later (scheduler.onEntity) and would see the changed hand"));
                heldReadPending = null;   // one finding per read
            }
            if (v == Scenario.Verb.WAIT && Long.parseLong(a) >= 1) heldReadPending = null;
            if (v == Scenario.Verb.CMD && readsHeldLater(a)) heldReadPending = a;
            if (v == Scenario.Verb.CMD && mobActsUnstaged(a)) {
                out.add(new Finding(gate, r, Rule.MOB_ACT_UNSTAGED, "'" + a + "' lets a mob act with no staged attack_damage"));
            }
            if (v == Scenario.Verb.PROBE_SUMMON && !isPositiveNumber(a)) {
                out.add(new Finding(gate, r, Rule.PROBE_SUMMON_REACH, "probe_summon '" + a + "' names no reach"));
            }
            if (v == Scenario.Verb.AFTER) {
                out.add(new Finding(gate, r, Rule.SELF_STAGED, "chains on " + a + ": it reads what that row left"));
            }
            if (v == Scenario.Verb.CLEAR_INVENTORY) cleared = true;
            // X1 (the seat, 2026-11-25): a summon the row reads is one the row cast itself, after clearing any it carried.
            if (v == Scenario.Verb.CLEAR_STATE) clearedState = true;
            if (v == Scenario.Verb.USE && clearedState) castOwn = true;
            if (!castOwn && usesSummon(v, a)) {
                out.add(new Finding(gate, r, Rule.SUMMON_UNSTAGED, describe(v, a)
                        + ": reads a summon with no cast of its own earlier in the row (clear_state, then use)"));
            }
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

    /** A step as the finding names it: the verb, and for a command its text. */
    private static String describe(Scenario.Verb v, String a) {
        return v == Scenario.Verb.CMD ? "'" + a + "'" : v.name().toLowerCase(Locale.ROOT);
    }

    private static boolean inside(SelfTestPreflight.Box b, Summon m) {
        return m.dx() >= b.x1() && m.dx() <= b.x2() && m.dz() >= b.z1() && m.dz() <= b.z2();
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
