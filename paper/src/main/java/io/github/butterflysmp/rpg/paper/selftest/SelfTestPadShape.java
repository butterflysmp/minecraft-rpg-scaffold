package io.github.butterflysmp.rpg.paper.selftest;

/**
 * THE PAD'S SHAPE, pure and in one place (Ben's ruling A, 2026-11-23: "The test builds its own ground. A one-time flat
 * stone platform high in the sky of your dev world"; the seat's P2-P3). No Bukkit type: {@link SelfTestPad} builds what
 * this says, and nothing else knows the shape.
 *
 * <p>Every coordinate is in blocks from the STAND POINT's column; {@code dy} 0 is the FLOOR's level, his feet stand at
 * {@code dy} 1. The INTERIOR is x {@value #X1}..{@value #X2} and z {@value #Z1}..{@value #Z2}: a stone FLOOR at dy 0 under
 * all of it and AIR above it to dy {@value #RIM_HEIGHT}. Round it, one block wide on all four sides (corners included),
 * a BARRIER RIM from dy 0 to dy {@value #RIM_HEIGHT}, so he cannot walk, slide or be knocked off.
 */
final class SelfTestPadShape {

    private SelfTestPadShape() {}

    /** SEAT PLACEHOLDERS, OPEN FOR BEN (P2): the stand point is this far east of world spawn's x, at spawn's z. */
    static final int EAST_OF_SPAWN = 200;
    /** SEAT PLACEHOLDER (P2): the floor's Y. Well inside the overworld's build height (top block Y 319, READ). */
    static final int FLOOR_Y = 200;

    static final int X1 = -2;
    static final int X2 = 45;
    static final int Z1 = -13;
    static final int Z2 = 13;
    /** The rim runs from the floor's level to this many above it; the interior's air runs from 1 to this. */
    static final int RIM_HEIGHT = 4;

    /** What one pad cell is. Only these cells are ever touched. */
    enum Kind { FLOOR, AIR, RIM }

    /** Every cell of the pad: interior columns x layers, plus the rim's ring x layers. */
    static final int CELLS = (X2 - X1 + 1) * (Z2 - Z1 + 1) * (RIM_HEIGHT + 1)
            + ((X2 - X1 + 3) * (Z2 - Z1 + 3) - (X2 - X1 + 1) * (Z2 - Z1 + 1)) * (RIM_HEIGHT + 1);

    /** The stand point's BLOCK: x and z of the column he stands in, and the floor's Y under him. */
    record Stand(int x, int floorY, int z) {
        int feetY() {
            return floorY + 1;
        }
    }

    /** The one pad's stand point, derived from world spawn's block x and z (P2). */
    static Stand stand(int spawnX, int spawnZ) {
        return new Stand(spawnX + EAST_OF_SPAWN, FLOOR_Y, spawnZ);
    }

    static boolean inInterior(int dx, int dz) {
        return dx >= X1 && dx <= X2 && dz >= Z1 && dz <= Z2;
    }

    /**
     * THE PAD'S OWN REGION (the seat's P2 and F2, 2026-11-27): what the run's world-touching cleanups may reach once he is
     * on the pad, and nothing beyond it. In blocks from the stand's column: the SHELL (the interior and its one-block rim,
     * x {@code X1 - 1}..{@code X2 + 1}, z {@code Z1 - 1}..{@code Z2 + 1}) and, in height, {@link #REGION_BELOW} to
     * {@link #HEADROOM} from the floor's level. CC'S CALL: the headroom is 16, above every arc the pad's rows throw; the
     * alternative is the rim's own height, 4, which a lobbed chunk's arc can clear.
     */
    static final int REGION_BELOW = -1;
    static final int HEADROOM = 16;

    static boolean inRegion(int dx, int dy, int dz) {
        return dx >= X1 - 1 && dx <= X2 + 1 && dz >= Z1 - 1 && dz <= Z2 + 1 && dy >= REGION_BELOW && dy <= HEADROOM;
    }

    /** A world point's block, against the stand's: inside the pad's region? */
    static boolean inRegion(Stand s, double x, double y, double z) {
        return inRegion((int) Math.floor(x) - s.x(), (int) Math.floor(y) - s.floorY(), (int) Math.floor(z) - s.z());
    }

    /** The region as world bounds for a scan: {minX, minY, minZ, maxX, maxY, maxZ}, the max EXCLUSIVE (block edges). */
    static double[] bounds(Stand s) {
        return new double[] {s.x() + X1 - 1, s.floorY() + REGION_BELOW, s.z() + Z1 - 1,
                s.x() + X2 + 2, s.floorY() + HEADROOM + 1, s.z() + Z2 + 2};
    }

    /** The cell's kind, or null when it is not a pad cell. */
    static Kind kindAt(int dx, int dy, int dz) {
        if (dy < 0 || dy > RIM_HEIGHT) return null;
        if (inInterior(dx, dz)) return dy == 0 ? Kind.FLOOR : Kind.AIR;
        boolean ring = dx >= X1 - 1 && dx <= X2 + 1 && dz >= Z1 - 1 && dz <= Z2 + 1;
        return ring ? Kind.RIM : null;
    }

    interface CellVisitor {
        void visit(int dx, int dy, int dz, Kind kind);
    }

    /** Every pad cell once, in a fixed order (x, then z, then y). */
    static void forEachCell(CellVisitor v) {
        for (int dx = X1 - 1; dx <= X2 + 1; dx++) {
            for (int dz = Z1 - 1; dz <= Z2 + 1; dz++) {
                for (int dy = 0; dy <= RIM_HEIGHT; dy++) {
                    Kind k = kindAt(dx, dy, dz);
                    if (k != null) v.visit(dx, dy, dz, k);
                }
            }
        }
    }

    /** The chunk span the pad covers, inclusive, for a stand point: {minCx, minCz, maxCx, maxCz}. */
    static int[] chunks(Stand s) {
        return new int[] {Math.floorDiv(s.x() + X1 - 1, 16), Math.floorDiv(s.z() + Z1 - 1, 16),
                Math.floorDiv(s.x() + X2 + 1, 16), Math.floorDiv(s.z() + Z2 + 1, 16)};
    }

    /** What a build or a check did: cells placed into air, cells repaired from a wrong block, cells already right. */
    record Tally(int placed, int repaired, int ok) {}

    /**
     * One cell's verdict: {@code want} the kind, {@code isAir} and {@code isWant} what the world holds there. A cell that
     * holds what it should is OK; a FLOOR or RIM cell holding air is PLACED; anything else wrong is REPAIRED (a stray
     * block in the air, a wrong block in the floor or the rim).
     */
    enum Verdict { OK, PLACED, REPAIRED }

    static Verdict verdict(Kind want, boolean isAir, boolean isWant) {
        if (isWant) return Verdict.OK;
        return want != Kind.AIR && isAir ? Verdict.PLACED : Verdict.REPAIRED;
    }

    /** The PAD line: where, and what the build did. */
    static String line(String world, Stand s, Tally t) {
        return "PAD world=" + world + " stand=" + s.x() + "," + s.feetY() + "," + s.z() + " floorY=" + s.floorY()
                + " interior=" + X1 + ".." + X2 + " east " + Z1 + ".." + Z2 + " south cells=" + CELLS
                + " placed=" + t.placed() + " repaired=" + t.repaired() + " ok=" + t.ok();
    }
}
