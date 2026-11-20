package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9e, THE COPY AND THE REVIVE (the seat's go-ahead of 2026-11-19). The wiring sits behind a real server, so it is
 * pinned from the source, comments stripped, each method bounded STRUCTURALLY; the pure halves are core's
 * {@code CopyPlaceTest} and {@code ReviveTrackerTest}. EVERY PLACE THE COPY IS LEFT OUT is a test here, the explicit
 * ones by their line and the ones it is left out of by its nature by the jar's own class hierarchy.
 */
class FightReviveSourceTest {

    private static final Path PAPER = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static String strip(String src) {
        src = src.replace("\r\n", "\n").replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static String code(String file) throws IOException {
        return strip(Files.readString(PAPER.resolve(file)));
    }

    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@Override", "@EventHandler", "\n    public ", "\n    private ", "\n    void ",
                "\n    static ", "\n    String ", "\n    boolean "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    private static String instance() throws IOException { return code("fight/FightInstance.java"); }

    // --- the exclusions, explicit ------------------------------------------------------------------------------------

    /** 1. Every target picker (areas, rays, drags, beams, shots, pounces, blasts, growth's candidates) asks isLive. */
    @Test
    void theCopyIsNeverALiveBody() throws IOException {
        assertTrue(body(code("adapter/LiveTargets.java"), "public static boolean isLive(Entity entity)")
                .contains("&& !io.github.butterflysmp.rpg.paper.fight.FightCopies.isCopy(entity);"));
    }

    /** 2. The store, the gear score and the nameplate are reached only through the seed. */
    @Test
    void theCopyIsNeverSeeded() throws IOException {
        assertTrue(body(code("health/MobNameplateManager.java"), "private Seeded seed(LivingEntity mob)")
                .contains("if (io.github.butterflysmp.rpg.paper.fight.FightCopies.isCopy(mob)) return null;"));
    }

    /** 3. Nobody can hit it: the attack is refused first. */
    @Test
    void theCopyCannotBeAttacked() throws IOException {
        String l = code("listener/RpgListeners.java");
        assertTrue(l.contains("@EventHandler(priority = EventPriority.LOWEST)\n    public void onCopyAttacked(PrePlayerAttackEntityEvent event)"));
        assertTrue(body(l, "public void onCopyAttacked(PrePlayerAttackEntityEvent event)")
                .contains("if (io.github.butterflysmp.rpg.paper.fight.FightCopies.isCopy(event.getAttacked())) event.setCancelled(true);"));
    }

    /** 4. The fight's kill counting never counts it. */
    @Test
    void theCopyIsNeverAKill() throws IOException {
        assertTrue(body(instance(), "public void onDeath(EntityDeathEvent event)")
                .contains("if (dead instanceof Player || FightCopies.isCopy(dead)) return;"));
    }

    /** 5 and 6. The selftest's LEFTOVER count and its aim ray leave it out. */
    @Test
    void theSelftestsLivingProbesLeaveItOut() throws IOException {
        assertTrue(body(code("selftest/SelfTestRun.java"), "private List<String> leftovers()")
                .contains(".filter(e -> !io.github.butterflysmp.rpg.paper.fight.FightCopies.isCopy(e))"));
        assertTrue(code("selftest/SelfTestDrivers.java").contains("e -> e instanceof LivingEntity && !e.equals(p)\n"
                + "                        && !io.github.butterflysmp.rpg.paper.fight.FightCopies.isCopy(e));"));
    }

    // --- the exclusions by its nature: the jar's own hierarchy ---------------------------------------------------------

    /**
     * READ from the pinned paper-api (javap): a Mannequin is a LivingEntity, and is NOT a Mob, a Creeper, a Player or an
     * ArmorStand. So the beamer, the shooter and the pouncer (each {@code instanceof Mob}), the Phage's fuse
     * ({@code instanceof Creeper}) and the player-only paths never take it; growth and split need a {@code mob_id}, which
     * a copy never has; the DamageGate is keyed by the dragon's and the Mage's ids only.
     */
    @Test
    void aMannequinIsLivingButNoMobCreeperPlayerOrStand() {
        assertTrue(org.bukkit.entity.LivingEntity.class.isAssignableFrom(org.bukkit.entity.Mannequin.class));
        for (Class<?> c : new Class<?>[] {org.bukkit.entity.Mob.class, org.bukkit.entity.Creeper.class,
                org.bukkit.entity.Player.class, org.bukkit.entity.ArmorStand.class}) {
            assertFalse(c.isAssignableFrom(org.bukkit.entity.Mannequin.class), "a Mannequin is not a " + c.getSimpleName());
        }
    }

    @Test
    void everyMannequinIsACopy() throws IOException {
        assertTrue(body(code("fight/FightCopies.java"), "public static boolean isCopy(Entity entity)")
                .contains("return entity instanceof Mannequin;"));
    }

    // --- the copy itself ------------------------------------------------------------------------------------------

    /** a: his look and name, lying down; cannot be hurt, pushed or picked up; not saved; tagged; the label from content. */
    @Test
    void theCopysBody() throws IOException {
        String s = body(instance(), "private org.bukkit.entity.Mannequin spawnCopy(");
        for (String n : new String[] {"m.setProfile(profile);", "m.setImmovable(true);", "m.setInvulnerable(true);",
                "m.setPersistent(false);", "m.setGravity(false);", "m.setCollidable(false);",
                "m.setPose(org.bukkit.entity.Pose.SLEEPING, true);", "m.customName(net.kyori.adventure.text.Component.text(name));",
                "m.setDescription(net.kyori.adventure.text.Component.text(label));", "m.addScoreboardTag(FightCopies.TAG);",
                "fight.revive().label().replace(\"{player}\", name)"}) {
            assertTrue(s.contains(n), "the copy " + n + ": " + s);
        }
        assertTrue(s.contains("new Location(world, x, centre.getY(), z);"), "on the floor");
    }

    /** a and b: laid on his down, placed by CopyPlace, his profile; removed at the end, when he is out, and revived. */
    @Test
    void theCopyIsLaidOnDownAndRemovedAtEveryEnd() throws IOException {
        String down = body(code("fight/FightFixtures.java"), "public boolean onPlayerDeath(PlayerDeathEvent event)");
        assertTrue(down.indexOf("a.fight().layCopy(p, at);") > down.indexOf("a.site().markDown(p.getUniqueId())"), down);
        String lay = body(instance(), "public void layCopy(Player p, Location died)");
        assertTrue(lay.contains("CopyPlace.place(died.getX(), died.getZ(), ringNow());"), lay);
        assertTrue(lay.contains("ResolvableProfile.resolvableProfile(p.getPlayerProfile())"), "his look");
        assertTrue(body(instance(), "private void finish(String why)").contains("removeCopy(c, \"end\");"), "the end");
        String fx = code("fight/FightFixtures.java");
        assertTrue(body(fx, "public void onQuit(Player p)").contains("a.fight().removeCopyOf(p.getUniqueId(), \"out\");"));
        assertTrue(body(fx, "public void onWorldChange(Player p, World from)").contains("a.fight().removeCopyOf(p.getUniqueId(), \"out\");"));
        assertTrue(body(instance(), "private void offerRevive(Copy c, Location at)").contains("removeCopy(c, \"revived\");"));
    }

    /** b: every tick each copy is placed again against this tick's ring; a closed ring leaves it unrevivable. */
    @Test
    void eachTickTheCopyIsPlacedAgainstTheRing() throws IOException {
        String t = body(instance(), "private void tickCopies()");
        int place = t.indexOf("CopyPlace.place(l.getX(), l.getZ(), ring);");
        int move = t.indexOf("if (p.moved()) {");
        int revivable = t.indexOf("c.revivable = p.revivable() && (c.player == null");
        int offer = t.indexOf("offerRevive(c, l);");
        assertTrue(place > 0 && move > place && revivable > move && offer > revivable, t);
        assertTrue(t.contains(".mayRevive(site.revives(c.player), fight.revive().maxRevives()));"), "e: ONE rule");
        assertTrue(instance().contains("            ring.tick();\n            tickCopies();"), "after the ring's tick");
    }

    /** c: only living participants are offered, never the copy's own player; DONE restores him through the holder. */
    @Test
    void theReviveOffersLivingParticipantsAndRestoresThroughTheHolder() throws IOException {
        String o = body(instance(), "private void offerRevive(Copy c, Location at)");
        assertTrue(o.contains("for (RingFixture.Participant part : port.participants()) {"), "living participants only");
        assertTrue(o.contains("if (pl == null || pl.getUniqueId().equals(c.player)) continue;"), o);
        assertTrue(o.contains("pl.getLocation().distance(at), pl.isSneaking()));"), o);
        assertTrue(o.contains("\"FIGHT revived practice name=%s by=%s t=%d at=%.2f %.2f %.2f\""), "f: the practice line");
        assertTrue(o.contains("if (reviveHook != null && reviveHook.revive(c.player, by, at.clone())) removeCopy(c, \"revived\");"), o);
        assertTrue(!o.contains("Damage") && !o.contains("damage"), "c: taking damage is not a break, so it is no input");
    }

    /** d: the mode he entered in, at the copy, the fraction of his maximum, a participant again. */
    @Test
    void revivedIsHisEntryModeAtTheCopyAtTheFraction() throws IOException {
        String r = body(code("fight/FightFixtures.java"), "private boolean revive(ArenaFight a, FightContent f, UUID downId, Player by, Location at)");
        int down = r.indexOf("if (p == null || !p.isOnline() || p.getWorld() != a.world() || !a.site().isDown(downId)) {");
        int off = r.indexOf("a.site().revive(downId);");
        int mode = r.indexOf("p.setGameMode(org.bukkit.GameMode.valueOf(back.map(FightReturn::restoreMode).orElse(\"SURVIVAL\")));");
        int tp = r.indexOf("p.teleport(at.clone().add(0.0, 0.1, 0.0));");
        int hp = r.indexOf("adapters.stats().setCurrent(downId, health, by.getUniqueId(), true);");
        assertTrue(down > 0 && off > down && mode > off && tp > mode && hp > tp, r);
        assertTrue(r.contains("ReviveRules.revivedHealth(max,\n                f.revive().healthFraction());"), r);
        assertTrue(r.contains("\"FIGHT revived player=%s by=%s t=%d at=%.2f %.2f %.2f\"\n"
                + "                        + \" health=%.3f/%.3f revives=%d mode=%s\""), r);
    }

    /** c: the progress shows on the reviver's own action bar, held so the stats bar does not draw over it. */
    @Test
    void theProgressHoldsTheRevieversActionBar() throws IOException {
        String fx = code("fight/FightFixtures.java");
        assertTrue(fx.contains("bars.hold(who.getUniqueId(), line, \"revive\", Bukkit.getCurrentTick(), REVIVE_BAR_HOLD_TICKS, false);"));
        assertTrue(fx.contains("who.sendActionBar(line);"));
    }

    /** f: the dev command lays a practice copy. */
    @Test
    void thePracticeCopyCommand() throws IOException {
        String cmd = code("fight/FightCommand.java");
        assertTrue(cmd.contains("Commands.literal(\"copy\")") && cmd.contains("fixtures.practiceCopy("), "the command");
        assertTrue(body(instance(), "public String layPractice(String name, double dxBlocks, double dzBlocks)")
                .contains("ResolvableProfile.resolvableProfile().name(name).build()"));
    }
}
