package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;
import io.github.butterflysmp.rpg.core.fight.RingFixture;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The dev fixture's world ({@link RingFixture.Port}), on a real server. Every decision is the core fixture's; this
 * only reads players and performs the four acts.
 *
 * <ul>
 *   <li><b>The drawing:</b> {@code Player#spawnParticle(..., force = true)}, one call per point per viewer. READ from
 *       the pinned server jar ({@code CraftPlayer.spawnParticle}, javap, build 74): it builds a
 *       {@code ClientboundLevelParticlesPacket} with {@code overrideLimiter} = force and {@code alwaysShow} = false and
 *       sends it straight to that player's connection, with no distance check. So the Bukkit call is enough and no
 *       PacketEvents route is needed; {@code alwaysShow} stays false (Ben: "I don't care about the minimal particle
 *       setting").</li>
 *   <li><b>The bolt:</b> {@code World#strikeLightningEffect}, an EFFECT ONLY: no vanilla damage, no fire
 *       (PLAN-dragon-fight.md section 4 g, READ). Never {@code strikeLightning}, which cfde822 used and which landed
 *       vanilla's own damage and fire on top of its own. {@code FightPortSourceTest} pins which one this calls.</li>
 *   <li><b>The damage:</b> the store's one entry, {@code BukkitCombatant#handle().applyDamage}, the same entry a mob's
 *       hit on a player uses, so the selftest's HURT line is logged. With {@code DefenseRule.APPLIES} (armour reduces it;
 *       Ben, 2026-11-18: "yes") and NO element (whether any element applies is OPEN). The ramp's ceiling keeps the
 *       defence curve finite ({@code StrikeRamp.CEILING_DAMAGE}). The dealer is the
 *       player himself (a bolt has no entity), as {@code RpgListeners.attributableId} attributes sourceless harm.</li>
 * </ul>
 *
 * <p><b>THREADING, AND IT IS A DEV FIXTURE'S SHORTCUT:</b> the loop runs on C's region thread and reads each
 * participant's position from there. On Paper every region is the main thread, so that is safe here. On Folia a
 * participant in another region would be read off his thread; the real fight (one temporary world, PLAN section 4 a)
 * must read positions on each player's own scheduler. Recorded in the PLAN.
 */
final class BukkitFightPort implements RingFixture.Port {

    private final FightSite site;
    private final World world;
    private final FightContent fight;
    private final AdapterContext adapters;
    private final Logger log;
    private final Particle.DustOptions dust;

    /** The site says where (its world, its crystal spots) and who takes part; the fight content says how it looks. */
    BukkitFightPort(FightSite site, FightContent fight, AdapterContext adapters, Logger log) {
        this.site = site;
        this.world = site.world();
        this.fight = fight;
        this.adapters = adapters;
        this.log = log;
        this.dust = new Particle.DustOptions(
                Color.fromRGB(fight.look().red(), fight.look().green(), fight.look().blue()),
                (float) fight.look().dustSize());
        this.crystalSpots = site.crystalSpots();
    }

    /** Every player in the site's world whom the SITE says takes part (its rule, not the port's). */
    @Override
    public List<RingFixture.Participant> participants() {
        List<RingFixture.Participant> out = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (!site.takesPart(p)) continue;
            Location feet = p.getLocation();
            out.add(new RingFixture.Participant(p.getUniqueId(), p.getName(), feet.getX(), feet.getY(), feet.getZ()));
        }
        return out;
    }

    @Override
    public void draw(List<UUID> viewers, List<double[]> points) {
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            for (double[] pt : points) {
                p.spawnParticle(Particle.DUST, pt[0], pt[1], pt[2], 1, 0.0, 0.0, 0.0, 0.0, dust, true);
            }
        }
    }

    @Override
    public void strikeEffect(UUID player, double xBlocks, double yBlocks, double zBlocks) {
        world.strikeLightningEffect(new Location(world, xBlocks, yBlocks, zBlocks));
    }

    @Override
    public void damage(UUID player, double amount) {
        Player p = Bukkit.getPlayer(player);
        if (p == null) return;
        BukkitCombatant.of(p, adapters).handle()
                .applyDamage(amount, player, CritState.NORMAL, DefenseRule.APPLIES, null);
    }

    @Override
    public void log(String line) {
        log.info(line);
    }

    /** The site's answer: a failure kills in the arena, and kills nobody in the ordinary world. */
    @Override
    public boolean failureKills() {
        return site.failureKills();
    }

    @Override
    public void tell(String line) {
        Component text = Component.text(line, NamedTextColor.GOLD);
        for (RingFixture.Participant p : participants()) {
            Player player = Bukkit.getPlayer(p.id());
            if (player != null) player.sendMessage(text);
        }
    }

    // --- step F7: the crystals --------------------------------------------------------------------------------------

    /** The crystals this fixture lit, in order; the port owns them, so a stop removes every one. */
    private final List<org.bukkit.entity.EnderCrystal> crystals = new ArrayList<>();
    private java.util.function.Supplier<org.bukkit.entity.LivingEntity> mage = () -> null;
    /** The site's crystal spots (spot n is index n - 1), read once at construction. */
    private final List<Location> crystalSpots;

    /** Bound by the instance at the start: where its Mage is now. */
    void bind(java.util.function.Supplier<org.bukkit.entity.LivingEntity> mage) {
        this.mage = mage;
    }

    /**
     * Crystal {@code index} appears at its spot, its bottom hidden, its beam on the Mage, unbreakable (its damage event is
     * cancelled by {@code RpgListeners.onFightCrystalDamage}; {@code setInvulnerable} the backstop), and NOT PERSISTED:
     * READ from the server jar, {@code CraftEntity.setPersistent} writes {@code Entity.persist}, and
     * {@code Entity.saveAsPassenger} writes nothing when it is false (its force flag aside), so a stop or a crash leaves
     * no crystal in the world file. Every participant hears the cue at the crystal.
     */
    @Override
    public void lightCrystal(int index) {
        if (index < 1 || index > crystalSpots.size()) return;
        Location spot = crystalSpots.get(index - 1);
        org.bukkit.entity.LivingEntity target = mage.get();
        org.bukkit.entity.EnderCrystal crystal = world.spawn(spot, org.bukkit.entity.EnderCrystal.class, c -> {
            c.setShowingBottom(false);
            c.setInvulnerable(true);
            c.setPersistent(false);
            c.getPersistentDataContainer().set(adapters.keys().fightCrystal,
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            c.addScoreboardTag("fight_crystal");
            if (target != null && target.isValid()) c.setBeamTarget(target.getLocation());
        });
        crystals.add(crystal);
        // The witness that crystal n stands at spot n (on pillar n in the arena): its spot, in the world's coordinates.
        log.info(String.format(java.util.Locale.ROOT, "CRYSTALSPOT index=%d at=%.2f %.2f %.2f world=%s", index,
                spot.getX(), spot.getY(), spot.getZ(), world.getName()));
        FightContent.Cue cue = fight.litCue();
        for (RingFixture.Participant p : participants()) {
            Player player = Bukkit.getPlayer(p.id());
            if (player != null) player.playSound(spot, cue.sound(), cue.volume(), cue.pitch());
        }
    }

    @Override
    public void clearCrystals() {
        for (org.bukkit.entity.EnderCrystal c : crystals) {
            if (c.isValid()) c.remove();
        }
        crystals.clear();
    }

    /** The refused-hit cue at the Mage, heard by whoever is near him (the attacker among them). */
    @Override
    public void refusedCue() {
        org.bukkit.entity.LivingEntity m = mage.get();
        if (m == null || !m.isValid()) return;
        FightContent.Cue cue = fight.refusedCue();
        world.playSound(m.getLocation(), cue.sound(), cue.volume(), cue.pitch());
    }

    /** The live crystals' count, for the CRYSTALPROBE line. */
    List<org.bukkit.entity.EnderCrystal> crystals() {
        crystals.removeIf(c -> !c.isValid());
        return List.copyOf(crystals);
    }
}
