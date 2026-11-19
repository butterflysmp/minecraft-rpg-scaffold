package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.CrystalRing;
import io.github.butterflysmp.rpg.core.fight.StandableColumn;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * THE DEV FIXTURE'S SITE (GATE-fight-ring.md, GATE-fight-mage.md, GATE-fight-site.md): a fight in an ordinary world,
 * centred on the block the command ran at. It changes no blocks.
 * <ul>
 *   <li><b>The crystal spots:</b> the fight's crystal count, evenly round a circle about C (the content's
 *       {@code crystals.ring_radius_blocks}, {@code height_above_ground_blocks}), standing in for the arena's
 *       pillars.</li>
 *   <li><b>Where the Mage may stand:</b> the highest standable block within {@link #STAND_BAND} of C's y in that
 *       column, with {@link #STAND_CLEARANCE} air above it.</li>
 *   <li><b>Who takes part:</b> {@link FightParticipants#takesPart}, a player in C's world within the content's
 *       participant radius, in survival or adventure.</li>
 * </ul>
 */
public final class OrdinaryWorldSite implements FightSite {

    /** The brief's: a standable block within this many blocks above or below C's y. */
    static final int STAND_BAND = 8;

    /** Air blocks above the floor the Mage needs (a wither skeleton is about 2.4 tall). */
    static final int STAND_CLEARANCE = 3;

    private final World world;
    private final Location centre;
    private final List<Location> crystalSpots;
    private final double participantRadiusBlocks;

    /** C is the block position of {@code at}: its x and z centred, its block y the ground. */
    public OrdinaryWorldSite(Location at, FightContent fight) {
        Location block = at.getBlock().getLocation();
        this.world = block.getWorld();
        double cx = block.getBlockX() + 0.5;
        double cz = block.getBlockZ() + 0.5;
        double groundY = block.getBlockY();
        this.centre = new Location(world, cx, groundY, cz);
        List<Location> spots = new ArrayList<>();
        for (double[] o : CrystalRing.offsets(fight.settings().crystalCount(), fight.crystalRingRadiusBlocks(),
                fight.crystalHeightBlocks())) {
            spots.add(new Location(world, cx + o[0], groundY + o[1], cz + o[2]));
        }
        this.crystalSpots = List.copyOf(spots);
        this.participantRadiusBlocks = fight.participantRadiusBlocks();
    }

    @Override public String kind() { return "ordinary"; }

    @Override public World world() { return world; }

    @Override public Location centre() { return centre.clone(); }

    @Override public List<Location> crystalSpots() {
        return crystalSpots.stream().map(Location::clone).toList();
    }

    @Override
    public OptionalInt mageFloorAt(double xBlocks, double zBlocks) {
        int bx = (int) Math.floor(xBlocks);
        int bz = (int) Math.floor(zBlocks);
        return StandableColumn.highest(centre.getBlockY(), STAND_BAND, STAND_CLEARANCE,
                y -> world.getBlockAt(bx, y, bz).isSolid());
    }

    @Override
    public boolean takesPart(Player player) {
        Location feet = player.getLocation();
        boolean sameWorld = feet.getWorld() == world;
        return FightParticipants.takesPart(sameWorld, sameWorld ? feet.distance(centre) : Double.POSITIVE_INFINITY,
                player.getGameMode(), participantRadiusBlocks);
    }
}
