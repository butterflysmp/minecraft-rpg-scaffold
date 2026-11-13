package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.IntBinaryOperator;
import java.util.random.RandomGenerator;

/**
 * A {@link RandomGenerator} for tests that answers {@code nextInt(origin, bound)} from a script,
 * so a row can say exactly which roll it wants.
 *
 * <p>Either a fixed rule ({@link #alwaysMin()}, {@link #alwaysMax()}) or a queue of values. A
 * queued value outside {@code [origin, bound)} throws, so a script can never smuggle in a roll the
 * real generator could not produce.
 */
final class ScriptedRandom implements RandomGenerator {

    private final Deque<Integer> queue = new ArrayDeque<>();
    private final IntBinaryOperator rule;

    private ScriptedRandom(IntBinaryOperator rule) {
        this.rule = rule;
    }

    /** Every {@code nextInt(origin, bound)} returns {@code origin}: the smallest legal roll. */
    static ScriptedRandom alwaysMin() {
        return new ScriptedRandom((origin, bound) -> origin);
    }

    /** Every {@code nextInt(origin, bound)} returns {@code bound - 1}: the largest legal roll. */
    static ScriptedRandom alwaysMax() {
        return new ScriptedRandom((origin, bound) -> bound - 1);
    }

    /** Returns these values in order, then fails if asked for more. */
    static ScriptedRandom of(int... values) {
        ScriptedRandom r = new ScriptedRandom(null);
        for (int v : values) r.queue.add(v);
        return r;
    }

    @Override
    public int nextInt(int origin, int bound) {
        int v;
        if (rule != null) {
            v = rule.applyAsInt(origin, bound);
        } else {
            Integer next = queue.poll();
            if (next == null) throw new IllegalStateException("script exhausted");
            v = next;
        }
        if (v < origin || v >= bound) {
            throw new IllegalStateException("scripted " + v + " is outside [" + origin + ", " + bound + ")");
        }
        return v;
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException("only nextInt(origin, bound) is scripted");
    }
}
