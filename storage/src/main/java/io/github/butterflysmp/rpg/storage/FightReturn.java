package io.github.butterflysmp.rpg.storage;

import java.util.Set;

/**
 * WHERE A PLAYER GOES BACK TO WHEN HE LEAVES A FIGHT'S OWN WORLD (the F8 brief, section 4 c), saved in his profile on
 * entry and cleared when he is returned. It is a place in a world the server keeps (the world's name as Bukkit knows
 * it), never in the fight's own world, which is deleted when the fight ends.
 *
 * <p>Plain numbers and names, so the storage module stays free of the server's types.
 *
 * <p><b>THE MODE HE ENTERED IN</b> (step F9d d): saved with the place, so a player who went DOWN (spectator) in the fight
 * comes home in the mode he entered in, by every route: the fight's end, a quit, a server stop, a crash. A return
 * written before this field existed has none, and {@link #restoreMode()} reads that as survival: no migration step is
 * needed, and none is run.
 *
 * @param worldName the world's name, as {@code World#getName} gives it
 * @param x         blocks
 * @param y         blocks
 * @param z         blocks
 * @param yaw       degrees
 * @param pitch     degrees
 * @param entryMode the game mode's name, as {@code GameMode#name} gives it, or null when none was saved (F8b's returns)
 */
public record FightReturn(String worldName, double x, double y, double z, float yaw, float pitch, String entryMode) {

    /** The modes a return may restore. Any other saved value, and none, restores survival. */
    private static final Set<String> RESTORABLE = Set.of("SURVIVAL", "ADVENTURE", "CREATIVE");

    public FightReturn {
        if (worldName == null || worldName.isBlank()) throw new IllegalArgumentException("no world name");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("a return position must be finite: " + x + " " + y + " " + z);
        }
    }

    /** A return with no mode saved (as F8b wrote them). */
    public FightReturn(String worldName, double x, double y, double z, float yaw, float pitch) {
        this(worldName, x, y, z, yaw, pitch, null);
    }

    /**
     * The mode he goes home in: the one he entered in when it is survival, adventure or creative, and SURVIVAL
     * otherwise. Spectator is never restored: he must never be left in spectator in the normal world (F9d d).
     */
    public String restoreMode() {
        return entryMode != null && RESTORABLE.contains(entryMode) ? entryMode : "SURVIVAL";
    }
}
