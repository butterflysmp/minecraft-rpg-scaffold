package io.github.butterflysmp.rpg.paper.fight;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.OptionalInt;

/**
 * WHERE A FIGHT RUNS (the F8 brief, section 3: one runtime, two sites; PLAN-dragon-fight.md section 6.4). The fight's
 * logic exists once, in {@link FightInstance}. A site supplies only the place:
 * <ul>
 *   <li>the world;</li>
 *   <li>the centre C and its ground height;</li>
 *   <li>the crystal spots;</li>
 *   <li>where the Mage may stand;</li>
 *   <li>who takes part.</li>
 * </ul>
 *
 * <p>Two sites are planned:
 * <ul>
 *   <li>{@link OrdinaryWorldSite}, the dev fixture in whatever world the caller stands in;</li>
 *   <li>the arena, step F8b, the fight's own world.</li>
 * </ul>
 *
 * <p>A site holds no fight state: the ring, the strikes, the Mage and the crystals are the instance's.
 */
public interface FightSite {

    /** A short name for the log: {@code ordinary} for the dev fixture's site. */
    String kind();

    World world();

    /** C: block-centred x and z, and the ground's block y (the y the wall's heights are measured from). */
    Location centre();

    /** The crystals' spots: spot n is element n - 1, one per crystal the fight lights. */
    List<Location> crystalSpots();

    /**
     * The block y of the floor a Mage may stand on in the column at ({@code xBlocks}, {@code zBlocks}), or empty where
     * he may not stand there.
     */
    OptionalInt mageFloorAt(double xBlocks, double zBlocks);

    /** Whether this player takes part right now: drawn to, struck, and healed in the play row. */
    boolean takesPart(Player player);

    /**
     * Whether a failure is REAL here (the F8 brief, section 5 i; Ben: "the players die and the fight ends"): POWERED
     * kills every participant through the store, and the fight fails once nobody is alive inside. The ordinary-world
     * dev fixture keeps "nobody is killed".
     */
    default boolean failureKills() {
        return false;
    }

    /** Whether nobody who entered is still in the fight. Only a site with entrants can say yes. */
    default boolean nobodyInside() {
        return false;
    }

    /**
     * F9d: who is DOWN (died inside, spectating, not a participant), in the order they went down. Only the arena has
     * anyone down; they still see the ring, the bar and the fight's chat.
     */
    default java.util.Set<java.util.UUID> downed() {
        return java.util.Set.of();
    }

    /** F9e e: how many times he has been revived in this fight (only the arena revives). */
    default int revives(java.util.UUID id) {
        return 0;
    }

    /**
     * F9d c: ALL DOWN, asked once a tick with how many participants are alive inside. Only the arena can fail this way.
     */
    default io.github.butterflysmp.rpg.core.fight.DownRoster.Check allDownCheck(long tick, int aliveInside) {
        return io.github.butterflysmp.rpg.core.fight.DownRoster.Check.NONE;
    }
}
