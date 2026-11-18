package io.github.butterflysmp.rpg.core.fight;

import java.util.ArrayList;
import java.util.List;

/**
 * F7: THE SEAT'S PLACEHOLDER CRYSTAL SPOTS, standing in for the pillars until the arena exists (PLAN-dragon-fight.md
 * section 2): {@code count} spots evenly round a circle about C, in order from angle 0, each at one height. The fight
 * asks a crystal-spot source, so the arena can supply real spots later; this is the source's default.
 */
public final class CrystalRing {

    private CrystalRing() {}

    /** Offsets from C's ground, blocks, {@code {dx, dy, dz}}; spot {@code n} (1-based) is element {@code n - 1}. */
    public static List<double[]> offsets(int count, double radiusBlocks, double heightBlocks) {
        List<double[]> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = 2.0 * Math.PI * i / count;
            out.add(new double[] {radiusBlocks * Math.cos(a), heightBlocks, radiusBlocks * Math.sin(a)});
        }
        return out;
    }
}
