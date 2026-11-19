package io.github.butterflysmp.rpg.core.fight;

import java.util.ArrayList;
import java.util.List;

/**
 * F9, THE DRAGON'S PHASES as a pure queue (the F9 brief, section 3 f). Ben: "let's do 80 60 40 and 20 percent".
 *
 * <p>The fight tells it the dragon's health as a fraction of its maximum after every change ({@link #observe}). Each
 * threshold the fraction falls STRICTLY BELOW is crossed once, for good: a heal un-crosses nothing. Every crossing owes
 * one Mage.
 *
 * <p><b>THE SEAT MECHANISM RULING: crossings QUEUE.</b> One hit across two thresholds owes two Mages, landed one after
 * the other. A Mage is {@link #due} only when the fight says it is idle: no Mage alive and the ring home (the cycle's
 * IDLE). The crossing is paid only when its Mage has actually {@link #landed}, so a landing refused for want of ground
 * stays owed and is tried again.
 */
public final class DragonCrossings {

    private final List<Double> thresholds;
    private int crossed;
    private int landed;

    /** @param thresholds fractions of the maximum, strictly falling, each strictly between 0 and 1 */
    public DragonCrossings(List<Double> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("the dragon needs at least one threshold");
        }
        double previous = 1.0;
        for (double t : thresholds) {
            if (!(t > 0.0 && t < 1.0)) {
                throw new IllegalArgumentException("a threshold must lie strictly between 0 and 1: " + t);
            }
            if (!(t < previous)) {
                throw new IllegalArgumentException("thresholds must fall strictly: " + thresholds);
            }
            previous = t;
        }
        this.thresholds = List.copyOf(thresholds);
    }

    /**
     * The dragon's health is now this fraction of its maximum.
     *
     * @return the thresholds crossed by this change, highest first; empty when none
     */
    public List<Double> observe(double fraction) {
        if (Double.isNaN(fraction)) throw new IllegalArgumentException("the health fraction is not a number");
        List<Double> now = new ArrayList<>();
        while (crossed < thresholds.size() && fraction < thresholds.get(crossed)) {
            now.add(thresholds.get(crossed));
            crossed++;
        }
        return now;
    }

    /** Whether a Mage is owed and may land now: {@code idle} is the fight's "no Mage alive and the ring home". */
    public boolean due(boolean idle) {
        return idle && pending() > 0;
    }

    /** The owed Mage has landed. */
    public void landed() {
        if (pending() == 0) throw new IllegalStateException("no Mage is owed");
        landed++;
    }

    /**
     * Why a hit on the dragon is refused, or {@link Refusal#NONE} (F9b, section 1; Ben: "No it can't be hurt while a
     * mage is owed"). A living Mage refuses it, and so does a Mage that is owed and has not yet landed: from the
     * crossing until that phase's Mage is dead. The gate asks BEFORE a hit, so the hit that crosses a threshold is
     * judged with nothing owed and lands whole; every hit after it waits for the Mage.
     *
     * @param mageAlive whether a Mage is alive now (channelling, landed or held)
     */
    public Refusal refusal(boolean mageAlive) {
        if (mageAlive) return Refusal.MAGE_ALIVE;
        if (pending() > 0) return Refusal.MAGE_OWED;
        return Refusal.NONE;
    }

    /** Why the dragon's gate refuses a hit. A living Mage is named first, since he is what the player must kill. */
    public enum Refusal {
        NONE("none"),
        MAGE_ALIVE("mage-alive"),
        MAGE_OWED("mage-owed");

        private final String logName;

        Refusal(String logName) { this.logName = logName; }

        /** The word in the gate's DRAGONGATE line. */
        public String logName() { return logName; }
    }

    /** Mages owed and not yet landed. */
    public int pending() {
        return crossed - landed;
    }

    /** Thresholds crossed so far. */
    public int crossed() {
        return crossed;
    }

    public List<Double> thresholds() {
        return thresholds;
    }
}
