package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.mining.FaceFrame;
import io.github.butterflysmp.rpg.core.mining.Spot;
import io.github.butterflysmp.rpg.core.mining.SpotBox;
import java.util.List;
import java.util.Locale;

/**
 * Where the mining verbs point the player's eye (GATE-mining-auto.md). Pure: no Bukkit type, so
 * {@code SelfTestMiningAimTest} casts the real ray (Bukkit's yaw and pitch convention, {@link SelfTestAim}) through
 * the real {@link FaceFrame} and asserts it lands where the scenario says, without a server.
 *
 * <p><b>Every target is a pixel CENTRE, never an edge.</b> A spot centred on pixel {@code cu} owns the box
 * {@code [cu - 2, cu + 3)} ({@link SpotBox}); its middle is {@code cu + 0.5}, 2.5 pixels from either side. A pixel
 * centre is also never on a block boundary (a block is 16 pixels), so the dig's block ray always meets the one block
 * that holds the point.
 */
final class SelfTestMiningAim {

    /** How far {@code beside} moves off the live spot's centre, in pixels: outside its box on either side. */
    static final double BESIDE_PX = 5.0;

    private SelfTestMiningAim() {}

    /**
     * The middle of one face of the block at {@code (bx, by, bz)}, given the face's unit normal {@code (mx, my, mz)}
     * ({@code BlockFace#getModX/Y/Z}): the point {@code AIM_BLOCK} turns the eye to.
     */
    static double[] faceCentre(int bx, int by, int bz, int mx, int my, int mz) {
        return new double[] {bx + 0.5 + 0.5 * mx, by + 0.5 + 0.5 * my, bz + 0.5 + 0.5 * mz};
    }

    /**
     * The patch pixel point {@code AIM_PATCH} turns the eye to, in the patch's own frame.
     *
     * <ul>
     *   <li>{@code live}: the live spot's box centre;</li>
     *   <li>{@code spot <i>}: spot {@code i}'s box centre, 1-based in reveal order, as the {@code show} line lists it;</li>
     *   <li>{@code pixel <u> <v>}: that pixel's centre, {@code (u + 0.5, v + 0.5)};</li>
     *   <li>{@code beside}: the live spot's centre moved {@link #BESIDE_PX} along the patch's u axis, to the right where
     *       that stays on the patch and to the left otherwise. 5 px from the centre is outside the box on both sides
     *       ({@code [cu - 2, cu + 3)}), and still on the patch, so a click there is a MISS, never a click-outside.</li>
     * </ul>
     *
     * @throws IllegalArgumentException naming what was wrong, which the driver logs as the row's SKIPPED reason
     */
    static double[] target(String arg, FaceFrame frame, List<Spot> spots, int liveIndex) {
        String[] f = arg.trim().split("\\s+");
        int widthPx = frame.width() * 16;
        int heightPx = frame.height() * 16;
        switch (f[0].toLowerCase(Locale.ROOT)) {
            case "live" -> {
                Spot s = spot(spots, liveIndex);
                return new double[] {s.cu() + 0.5, s.cv() + 0.5};
            }
            case "spot" -> {
                if (f.length != 2) throw new IllegalArgumentException("aim_patch spot wants 'spot <i>', got '" + arg + "'");
                Spot s = spot(spots, Integer.parseInt(f[1]) - 1);
                return new double[] {s.cu() + 0.5, s.cv() + 0.5};
            }
            case "pixel" -> {
                if (f.length != 3) throw new IllegalArgumentException("aim_patch pixel wants 'pixel <u> <v>', got '" + arg + "'");
                int u = Integer.parseInt(f[1]), v = Integer.parseInt(f[2]);
                if (u < 0 || u >= widthPx || v < 0 || v >= heightPx) {
                    throw new IllegalArgumentException("pixel " + u + "," + v + " is off a " + widthPx + " x " + heightPx + " patch");
                }
                return new double[] {u + 0.5, v + 0.5};
            }
            case "beside" -> {
                Spot s = spot(spots, liveIndex);
                double right = s.cu() + 0.5 + BESIDE_PX;
                double u = right < widthPx ? right : s.cu() + 0.5 - BESIDE_PX;
                return new double[] {u, s.cv() + 0.5};
            }
            default -> throw new IllegalArgumentException("aim_patch wants live, spot <i>, pixel <u> <v> or beside, got '" + arg + "'");
        }
    }

    private static Spot spot(List<Spot> spots, int index) {
        if (index < 0 || index >= spots.size()) {
            throw new IllegalArgumentException("no spot " + (index + 1) + " in a patch of " + spots.size());
        }
        return spots.get(index);
    }
}
