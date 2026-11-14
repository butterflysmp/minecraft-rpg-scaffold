package io.github.butterflysmp.rpg.core.mining;

/**
 * The four wall faces a patch can sit on. UP and DOWN are not walls, so they are not here: the dev
 * placement command refuses them (PLAN-mining.md 5.4).
 *
 * <p>Each face carries its outward normal and its RIGHT axis as the player sees the face. For
 * NORTH the normal is -Z, and the player stands north of the wall facing south (+Z), so west (-X)
 * is on their right. The up axis is +Y on every face. {@code FaceFrameTest} pins all four.
 */
public enum WallFace {
    //     normal x, z   right x, z
    NORTH(  0, -1,       -1,  0),
    SOUTH(  0,  1,        1,  0),
    EAST(   1,  0,        0, -1),
    WEST(  -1,  0,        0,  1);

    /** The outward normal's X and Z components (Y is always 0 for a wall). */
    public final int nx, nz;
    /** The player's right-hand axis, X and Z components. */
    public final int rx, rz;

    WallFace(int nx, int nz, int rx, int rz) {
        this.nx = nx;
        this.nz = nz;
        this.rx = rx;
        this.rz = rz;
    }

    /**
     * The display's left rotation as a quaternion {@code {x, y, z, w}}: it turns a flat model whose
     * front faces SOUTH (+Z) to face this wall's outward normal. A rotation of angle {@code a} about
     * +Y turns +Z toward +X, so EAST is +90 degrees, WEST -90 and NORTH 180.
     *
     * <p>Carried from the spike's {@code FacePoint.rotation}. <b>How it looks in game is UNREAD</b>:
     * spike 1 never stated the facing (F-M18), and gate row M-LOOK reads it.
     */
    public float[] displayRotation() {
        float half = (float) Math.sqrt(0.5);
        return switch (this) {
            case SOUTH -> new float[]{0f, 0f, 0f, 1f};
            case NORTH -> new float[]{0f, 1f, 0f, 0f};
            case EAST -> new float[]{0f, half, 0f, half};
            case WEST -> new float[]{0f, -half, 0f, half};
        };
    }
}
