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

    /** {@code "<n> <timeout ticks> <question>"}; the question is everything after the second word. */
    public record Ask(int number, int timeoutTicks, String question) {}

    public static Optional<Ask> parse(String arg) {
        String[] w = arg.trim().split("\\s+", 3);
        if (w.length < 3 || w[2].isBlank()) return Optional.empty();
        try {
            int n = Integer.parseInt(w[0]);
            int ticks = Integer.parseInt(w[1]);
            if (n < 1 || ticks < 1) return Optional.empty();
            return Optional.of(new Ask(n, ticks, w[2].trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** What the player sees. */
    public static String prompt(Ask a) {
        return "Selftest question " + a.number() + ": " + a.question() + " Answer yes or no in chat.";
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
