package io.github.butterflysmp.rpg.core.mining;

/**
 * A fixed place on a wall where a patch can appear, shared by every player (PLAN-mining.md 5.1).
 * Placed and removed by dev commands in slice 1; held in memory only.
 *
 * @param id     {@code <world>:<x>,<y>,<z>:<FACE>}, unique per anchor and face
 * @param world  the world's id as the paper side names it (its UUID string)
 * @param frame  the wall geometry, at the maximum size
 * @param oreId  the ore a patch here is made of
 * @param sizeId the slot's MAXIMUM vein size (S2, the seat's ruling 1): the footprint is this size's,
 *               and each patch rolls a size that fits inside it ({@link SizeRoll})
 */
public record Slot(String id, String world, FaceFrame frame, String oreId, String sizeId) {

    public static Slot of(String world, FaceFrame frame, String oreId, String sizeId) {
        String id = world + ":" + frame.ax() + "," + frame.ay() + "," + frame.az() + ":" + frame.face().name();
        return new Slot(id, world, frame, oreId, sizeId);
    }
}
