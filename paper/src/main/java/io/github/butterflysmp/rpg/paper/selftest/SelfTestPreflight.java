package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * THE PREFLIGHT (the seat's brief B, 2026-11-23): the ground a gate declares, measured before ANY row of the run, and a
 * bad spot runs none. The boot of {@code e774d4b8} read 255 blocks of water in EC0's floor and went on: EC0 only
 * DESCRIBED the spot. Pure: the driver reads each block and hands this its kind; this counts, decides and words it.
 *
 * <p>The box is in whole blocks from the player's own block, along the WORLD's axes: east is +x, south is +z. He faces
 * east in every row of a gate that declares one, so south is his RIGHT and north his LEFT, and that is how he is told.
 * At every column of the box: his feet and head height (dy 0 and 1) must be AIR, and the floor under them (dy -1) SOLID.
 */
final class SelfTestPreflight {

    private SelfTestPreflight() {}

    /** The furthest a box may reach in any direction: the row-start clean's radius is 48, and nothing here needs more. */
    static final int MAX = 64;

    /** East from, south from, east to, south to: whole blocks from his block, each pair in order. */
    record Box(int x1, int z1, int x2, int z2) {
        String label() {
            return "box=" + x1 + ".." + x2 + " east " + z1 + ".." + z2 + " south";
        }
    }

    static Optional<Box> parse(String arg) {
        String[] f = arg.trim().split("\\s+");
        if (f.length != 4) return Optional.empty();
        int[] v = new int[4];
        for (int i = 0; i < 4; i++) {
            if (!f[i].matches("-?\\d{1,2}")) return Optional.empty();
            v[i] = Integer.parseInt(f[i]);
            if (Math.abs(v[i]) > MAX) return Optional.empty();
        }
        if (v[0] > v[2] || v[1] > v[3]) return Optional.empty();
        return Optional.of(new Box(v[0], v[1], v[2], v[3]));
    }

    /** What a floor block is. Only SOLID passes. */
    enum Floor { SOLID, LIQUID, HOLE, SOFT }

    /**
     * A floor block's kind: AIR is a HOLE; anything wet (a liquid, or a waterlogged block, however solid) is LIQUID;
     * a block whose material is solid is SOLID; anything else (snow layers, carpet, a plant) is SOFT.
     */
    static Floor floor(boolean air, boolean wet, boolean solid) {
        if (air) return Floor.HOLE;
        if (wet) return Floor.LIQUID;
        return solid ? Floor.SOLID : Floor.SOFT;
    }

    /** One kind of bad cell: how many, where (east and south extents), and the first material met. */
    private static final class Bad {
        int n;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        String first;

        void add(int dx, int dz, String material) {
            n++;
            minX = Math.min(minX, dx);
            maxX = Math.max(maxX, dx);
            minZ = Math.min(minZ, dz);
            maxZ = Math.max(maxZ, dz);
            if (first == null) first = material;
        }
    }

    /** The measure of one box, cell by cell. */
    static final class Tally {
        private final Box box;
        private int bodyCells;
        private int floorCells;
        private final Bad notAir = new Bad();
        private final Bad liquid = new Bad();
        private final Bad hole = new Bad();
        private final Bad soft = new Bad();

        Tally(Box box) {
            this.box = box;
        }

        /** One feet- or head-height cell: {@code air} or not, and its material's name. */
        void body(int dx, int dz, boolean air, String material) {
            bodyCells++;
            if (!air) notAir.add(dx, dz, material);
        }

        /** One floor cell, of its kind. */
        void floor(int dx, int dz, Floor kind, String material) {
            floorCells++;
            switch (kind) {
                case SOLID -> { }
                case LIQUID -> liquid.add(dx, dz, material);
                case HOLE -> hole.add(dx, dz, material);
                case SOFT -> soft.add(dx, dz, material);
            }
        }

        boolean ok() {
            return notAir.n == 0 && liquid.n == 0 && hole.n == 0 && soft.n == 0;
        }

        /** The log line, with every count, pass or fail. */
        String logLine() {
            int solid = floorCells - liquid.n - hole.n - soft.n;
            String head = "PREFLIGHT " + (ok() ? "ok" : "fail") + " " + box.label() + " air=" + (bodyCells - notAir.n) + "/"
                    + bodyCells + " floor solid=" + solid + "/" + floorCells;
            return ok() ? head : head + " liquid=" + liquid.n + " hole=" + hole.n + " soft=" + soft.n;
        }

        /** What he is told: nothing on a pass; on a fail, what is wrong and where, then what to do. Two lines. */
        List<String> tell() {
            if (ok()) return List.of();
            List<String> parts = new ArrayList<>();
            part(parts, notAir, "block at foot or head height is not air", "blocks at foot or head height are not air");
            part(parts, liquid, "floor block is water or another liquid", "floor blocks are water or another liquid");
            part(parts, hole, "floor block is a hole", "floor blocks are holes");
            part(parts, soft, "floor block is not solid", "floor blocks are not solid");
            return List.of("The selftest cannot start here: " + String.join("; ", parts) + ".",
                    "Move to solid flat ground and type the command again.");
        }

        private static void part(List<String> parts, Bad bad, String one, String many) {
            if (bad.n == 0) return;
            parts.add(bad.n + " " + (bad.n == 1 ? one : many) + " (" + bad.first + "), " + east(bad.minX, bad.maxX)
                    + ", " + side(bad.minZ, bad.maxZ));
        }
    }

    /** {@code "10 blocks east"}, or {@code "from 3 to 8 blocks east"}. */
    static String east(int from, int to) {
        if (from == to) return from + (from == 1 ? " block east" : " blocks east");
        return "from " + from + " to " + to + " blocks east";
    }

    /**
     * Where across his line, as he faces east: south (+z) is his right, north (-z) his left, 0 his line. Pure, so each
     * shape is a unit row.
     */
    static String side(int minZ, int maxZ) {
        if (minZ == 0 && maxZ == 0) return "on your line";
        if (minZ > 0) return (minZ == maxZ ? "" + minZ : minZ + " to " + maxZ) + " to your right";
        if (maxZ < 0) return (minZ == maxZ ? "" + -maxZ : -maxZ + " to " + -minZ) + " to your left";
        String left = minZ == 0 ? "your line" : -minZ + " to your left";
        String right = maxZ == 0 ? "your line" : maxZ + " to your right";
        return String.format(Locale.ROOT, "from %s to %s", left, right);
    }
}
