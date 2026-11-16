package io.github.butterflysmp.rpg.paper.selftest;

import java.util.List;
import java.util.Optional;

/**
 * The SPAWN step's argument: {@code "<tag> <frozen|live> <mob> [stage <n>] [<gs>]"}.
 *
 * <p><b>Why it exists (GATE-dragon-fix.md, D2).</b> The dragon rows used to spawn with {@code /rpg spawn} and then tag
 * {@code @e[type=…,distance=..1.5,sort=nearest,limit=1]}. The row-start clean removes the last row's mobs through
 * each mob's OWN scheduler ({@code Scheduler.onEntity} -> {@code EntityScheduler#schedule} with delay 1, run at that
 * entity's next {@code executeTick}; read from the server jar), so in the tick the next row spawns, the last row's
 * mob is still in the world, on the same spot. The selector then picked it, and the new mob stayed untagged, kept its
 * AI and leaked. Tagging by identity inside the pre-spawn consumer leaves nothing to pick between.
 *
 * <p>A {@code record} is a class whose fields are fixed at construction, with accessors ({@code tag()}, …) for free.
 * {@code stage} and {@code gearScore} are {@code null} when not given: the command's own defaults (stage 1, a rolled
 * score) then apply, exactly as if typed without them.
 */
public final class SelfTestSpawn {

    private SelfTestSpawn() {}

    public record Spec(String tag, boolean frozen, String mob, Integer stage, Integer gearScore) {}

    /** Empty when malformed; the loader refuses the row by name. */
    public static Optional<Spec> parse(String arg) {
        List<String> w = List.of(arg.trim().split("\\s+"));
        if (w.size() < 3 || w.getFirst().isEmpty()) return Optional.empty();
        boolean frozen;
        switch (w.get(1)) {
            case "frozen" -> frozen = true;
            case "live" -> frozen = false;
            default -> { return Optional.empty(); }
        }
        Integer stage = null;
        Integer gs = null;
        int i = 3;
        if (i < w.size() && w.get(i).equals("stage")) {
            if (i + 1 >= w.size()) return Optional.empty();
            stage = number(w.get(i + 1));
            if (stage == null) return Optional.empty();
            i += 2;
        }
        if (i < w.size()) {
            gs = number(w.get(i));
            if (gs == null) return Optional.empty();
            i++;
        }
        if (i != w.size()) return Optional.empty();
        return Optional.of(new Spec(w.get(0), frozen, w.get(2), stage, gs));
    }

    private static Integer number(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
