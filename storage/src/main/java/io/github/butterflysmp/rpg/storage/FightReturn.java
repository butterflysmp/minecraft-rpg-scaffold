package io.github.butterflysmp.rpg.storage;

/**
 * WHERE A PLAYER GOES BACK TO WHEN HE LEAVES A FIGHT'S OWN WORLD (the F8 brief, section 4 c), saved in his profile on
 * entry and cleared when he is returned. It is a place in a world the server keeps (the world's name as Bukkit knows
 * it), never in the fight's own world, which is deleted when the fight ends.
 *
 * <p>Plain numbers and a name, so the storage module stays free of the server's types.
 *
 * @param worldName the world's name, as {@code World#getName} gives it
 * @param x         blocks
 * @param y         blocks
 * @param z         blocks
 * @param yaw       degrees
 * @param pitch     degrees
 */
public record FightReturn(String worldName, double x, double y, double z, float yaw, float pitch) {

    public FightReturn {
        if (worldName == null || worldName.isBlank()) throw new IllegalArgumentException("no world name");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("a return position must be finite: " + x + " " + y + " " + z);
        }
    }
}
