package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.ArenaShape;
import io.github.butterflysmp.rpg.core.fight.DownRoster;
import io.github.butterflysmp.rpg.core.fight.StandableColumn;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * THE ARENA'S SITE (step F8b): a fight in its own temporary world, on the generated stand-in arena.
 * <ul>
 *   <li><b>C</b> is the arena's centre column; its ground is the floor's top plus one.</li>
 *   <li><b>Crystal spot n</b> stands on pillar n ({@link ArenaShape#crystalSpot}).</li>
 *   <li><b>The Mage</b> may stand where the ordinary site's rule allows: the highest standable block within
 *       {@link OrdinaryWorldSite#STAND_BAND} of C's ground in that column. That is the floor, never a pillar's top (12
 *       up) and never the void beyond the disc.</li>
 *   <li><b>Who takes part:</b> an ENTRANT (entered, and not yet out by leaving or quitting) who is in this world,
 *       alive, NOT DOWN, in survival or adventure. A death inside makes him DOWN (step F9d): still an entrant, so he is
 *       returned at the end, but not a participant.</li>
 * </ul>
 * A failure is REAL here: {@link #failureKills()}.
 */
public final class ArenaSite implements FightSite {

    private final World world;
    private final ArenaShape shape;
    private final Location centre;
    private final List<Location> crystalSpots;

    /** Who entered and is not yet out. The holder ({@code FightFixtures}) adds and removes; the site only reads. */
    private final Set<UUID> entrants = new LinkedHashSet<>();

    /** F9d: who is down, and the ALL-DOWN clock. */
    private final DownRoster roster;

    ArenaSite(World world, ArenaShape shape, int allDownDelayTicks) {
        this.roster = new DownRoster(allDownDelayTicks);
        this.world = world;
        this.shape = shape;
        this.centre = new Location(world, shape.centreX() + 0.5, shape.groundY(), shape.centreZ() + 0.5);
        List<Location> spots = new ArrayList<>();
        for (int n = 1; n <= shape.pillarCount(); n++) {
            double[] s = shape.crystalSpot(n);
            spots.add(new Location(world, s[0], s[1], s[2]));
        }
        this.crystalSpots = List.copyOf(spots);
    }

    @Override public String kind() { return "arena"; }

    @Override public World world() { return world; }

    @Override public Location centre() { return centre.clone(); }

    @Override public List<Location> crystalSpots() {
        return crystalSpots.stream().map(Location::clone).toList();
    }

    @Override
    public OptionalInt mageFloorAt(double xBlocks, double zBlocks) {
        int bx = (int) Math.floor(xBlocks);
        int bz = (int) Math.floor(zBlocks);
        return StandableColumn.highest(centre.getBlockY(), OrdinaryWorldSite.STAND_BAND,
                OrdinaryWorldSite.STAND_CLEARANCE, y -> world.getBlockAt(bx, y, bz).isSolid());
    }

    @Override
    public boolean takesPart(Player player) {
        if (!entrants.contains(player.getUniqueId()) || roster.isDown(player.getUniqueId())) return false;
        if (player.getWorld() != world || player.isDead()) return false;
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    @Override public boolean failureKills() { return true; }

    @Override public boolean nobodyInside() { return entrants.isEmpty(); }

    ArenaShape shape() { return shape; }

    boolean isEntrant(UUID id) { return entrants.contains(id); }

    void addEntrant(UUID id) { entrants.add(id); }

    /** Out of the fight (left, quit, or a down spectator who left its world). True when he was in it. */
    boolean removeEntrant(UUID id) {
        roster.out(id);
        return entrants.remove(id);
    }

    /** F9d a: an entrant died inside and is DOWN. True when he was an entrant and not down already. */
    boolean markDown(UUID id) {
        return entrants.contains(id) && roster.down(id);
    }

    boolean isDown(UUID id) { return roster.isDown(id); }

    /** F9e d: he was revived; a participant again (if he is in the world, alive, in survival or adventure). */
    boolean revive(UUID id) { return roster.revive(id); }

    @Override public int revives(UUID id) { return roster.revives(id); }

     public Set<UUID> downed() { return roster.downed(); }

    
    public DownRoster.Check allDownCheck(long tick, int aliveInside) {
        return roster.check(tick, aliveInside);
    }

    Set<UUID> entrants() { return Set.copyOf(entrants); }
}
