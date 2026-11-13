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
}
