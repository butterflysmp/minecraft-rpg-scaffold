package io.github.butterflysmp.rpg.paper.selftest;

import java.util.Optional;

/**
 * The ASK step (GATE-dragon-fix.md, C6; Ben's proposal of 2026-11-15, accepted by the seat as mechanism): a numbered
 * yes/no question shown to the player in chat, and his NEXT chat line logged beside it, verbatim, on one line inside
 * the row's bracket.
 *
 * <p><b>It never interprets and never judges.</b> "yes", "yeah, crawling" and "not sure" are each logged exactly as
 * typed; a person reads the line against the gate's prediction. No answer before the timeout is logged as
 * {@code NOT STATED}, the gate's own word for an answer that was not given.
 */
public final class SelfTestAsk {

    private SelfTestAsk() {}

    /**
     * Which answer the question asks for (GATE-fight-ring.md's finding, 2026-11-18: appending "Answer yes or no" to an
     * open or two-part question made an answer that could not be read). The scenario says which.
     */
    public enum Prompt {
        /** A yes/no question: " Answer yes or no in chat." */
        YESNO(" Answer yes or no in chat."),
        /** An open question: " Answer in your own words in chat." */
        WORDS(" Answer in your own words in chat.");

        private final String tail;

        Prompt(String tail) {
            this.tail = tail;
        }
    }

    /**
     * {@code "<n> <timeout ticks> [yesno|words] <question>"}. The mode word is optional and defaults to {@code yesno},
     * so every row written before it was added asks exactly what it asked when it was read.
     */
    public record Ask(int number, int timeoutTicks, Prompt prompt, String question) {}

    public static Optional<Ask> parse(String arg) {
        String[] w = arg.trim().split("\\s+", 3);
        if (w.length < 3 || w[2].isBlank()) return Optional.empty();
        try {
            int n = Integer.parseInt(w[0]);
            int ticks = Integer.parseInt(w[1]);
            if (n < 1 || ticks < 1) return Optional.empty();
            String rest = w[2].trim();
            Prompt mode = Prompt.YESNO;
            String[] r = rest.split("\\s+", 2);
            if (r[0].equals("yesno") || r[0].equals("words")) {
                if (r.length < 2 || r[1].isBlank()) return Optional.empty();
                mode = r[0].equals("words") ? Prompt.WORDS : Prompt.YESNO;
                rest = r[1].trim();
            }
            return Optional.of(new Ask(n, ticks, mode, rest));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** What the player sees: the question and the tail its mode names. */
    /**
     * The window an ask that has him DO something first (hover, click, right-click, look) must allow: 3600 ticks, three
     * minutes. SEAT'S MECHANISM NUMBER, 2026-11-26, after TT19 timed out at 1200 on the boot of 88f1d4eb. Enforced on
     * scenarios by {@code ScenarioLint.Rule.ASK_WINDOW_SHORT}.
     */
    public static final int DO_FIRST_TICKS = 3600;

    /**
     * The question as he reads it, ending with how long he has (the seat, 2026-11-26: "The prompt tells him how long he
     * has"), in whole seconds at 20 ticks a second: 1200 ticks is "You have 60 seconds.".
     */
    public static String prompt(Ask a) {
        return "Selftest question " + a.number() + ": " + a.question() + a.prompt().tail
                + " You have " + (a.timeoutTicks() / 20) + " seconds.";
    }

    /** The log line for an answer, as typed. {@code ticks} is how long the question waited. */
    public static String answered(Ask a, String answer, long ticks) {
        return "ASK " + a.number() + " question=\"" + a.question() + "\" answer=\"" + answer + "\" ticks=" + ticks;
    }

    /** The log line when the timeout passed with no chat line. */
    public static String notStated(Ask a) {
        return "ASK " + a.number() + " question=\"" + a.question() + "\" answer=NOT STATED after " + a.timeoutTicks()
                + " ticks";
    }
}
