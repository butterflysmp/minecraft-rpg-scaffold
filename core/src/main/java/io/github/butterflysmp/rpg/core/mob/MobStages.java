package io.github.butterflysmp.rpg.core.mob;

/**
 * A mob whose body comes in stages: the Phage (PLAN-dragon-mobs.md, RULINGS: G3 and Ben's answer 6). Content gives
 * the stage count and the FIRST and LAST stage's bodies; every stage between is LINEAR in each field.
 *
 * <p>A stage is state on the LIVE mob, never a separate mob file (seat ruling 5). This record only answers "what does
 * stage N look like"; which stage a given mob is at is its {@code mob_stage} tag, read by paper.
 */
public record MobStages(int count, StageBody first, StageBody last) {

    public MobStages {
        if (count < 2) throw new IllegalArgumentException("stages.count must be at least 2, was " + count);
        if (first == null || last == null) throw new IllegalArgumentException("stages need a first and a last body");
    }

    public boolean isValid(int stage) {
        return stage >= 1 && stage <= count;
    }

    /** Stage {@code stage}'s body, 1 to {@code count}. Anything else is refused, never clamped. */
    public StageBody at(int stage) {
        if (!isValid(stage)) {
            throw new IllegalArgumentException("stage " + stage + " is outside 1.." + count);
        }
        double t = (double) (stage - 1) / (count - 1);
        return new StageBody(
                lerp(first.height(), last.height(), t),
                lerp(first.speedMultiplier(), last.speedMultiplier(), t),
                lerp(first.maxHealth(), last.maxHealth(), t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
