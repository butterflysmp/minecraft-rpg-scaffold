package io.github.butterflysmp.rpg.paper.summon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 3b's WIRING, read from the source (the body, the cast and the hooks need a server): the cap is decided BEFORE the
 * charge; the body's set-up runs inside the spawn's pre-add consumer with the tag AFTER the add; every removal cause has
 * its hook; the backstop, the enable sweep and the stop are wired; and no Java in the summon code names a wolf.
 * Each scan is bounded by its method's own body (the next member), never by a line count.
 */
class SummonWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(Path file) throws IOException {
        return Files.readAllLines(MAIN.resolve(file), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*")))
                .toList();
    }

    private static int at(List<String> c, String needle, int from) {
        for (int i = Math.max(0, from); i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    /** The end of the member that starts at {@code from}: the next line at member indent that opens one. */
    private static int end(List<String> c, int from) {
        for (int i = from + 1; i < c.size(); i++) {
            String l = c.get(i);
            if (l.startsWith("    @") || (l.startsWith("    ") && !l.startsWith("     ") && l.trim().matches(
                    "(public|private|static|protected|boolean|void|int|String).*\\(.*"))) {
                return i;
            }
        }
        return c.size();
    }

    /** THE CONTROL on {@link #end}: it stops at the NEXT member, or every "inside its method" bound above is vacuous. */
    @Test
    void theMemberBoundStopsAtTheNextMember() throws IOException {
        List<String> s = code(Path.of("summon", "SummonService.java"));
        int cast = at(s, "public CastResult cast(Player player,", 0);
        assertTrue(end(s, cast) <= at(s, "private void spawn(Player owner,", cast), "cast ends before spawn begins");
        List<String> l = code(Path.of("listener", "RpgListeners.java"));
        int death = at(l, "public void onSummonOwnerDeath(PlayerDeathEvent event) {", 0);
        assertTrue(end(l, death) < at(l, "public void onSummonOwnerChangedWorld(", death),
                "the owner-death hook ends before the world-change hook");
    }

    @Test
    void theCapIsDecidedBeforeTheCharge() throws IOException {
        List<String> c = code(Path.of("summon", "SummonService.java"));
        int cast = at(c, "public CastResult cast(Player player,", 0);
        int decide = at(c, "SummonCap.Verdict verdict = SummonCap.decide(weapon.id(), store().liveWeapons(", cast);
        int gate = at(c, "if (!verdict.charges()) {", decide);
        int refused = at(c, "return new CastResult.SummonRefused(notice(verdict, max));", gate);
        int charge = at(c, "CastResult charged = charge.get();", refused);
        int spawn = at(c, "if (charged instanceof CastResult.Success) spawn(", charge);
        assertTrue(cast > 0 && decide > cast && gate > decide && refused > gate && charge > refused && spawn > charge
                && spawn < end(c, cast), "decide, refuse uncharged, then charge, then spawn on success");
    }

    @Test
    void theBodyIsSetUpBeforeItIsAddedAndTaggedAfter() throws IOException {
        List<String> c = code(Path.of("summon", "SummonService.java"));
        int spawn = at(c, "private void spawn(Player owner,", 0);
        int open = at(c, "Mob body = (Mob) at.getWorld().spawn(at, type.getEntityClass(), e -> {", spawn);
        int close = at(c, "});", open);
        for (String step : List.of("mob.setPersistent(false);", "Bukkit.getMobGoals().removeAllGoals(mob);",
                "t.setTamed(false);", "t.setOwner(null);", "if (attack != null) attack.setBaseValue(0.0);",
                "replaced[0] = store().place(placed[0]);",
                "ctx.stats().bootstrapIfAbsent(mob.getUniqueId(), placed[0].health(), 0.0, false);")) {
            int s = at(c, step, open);
            assertTrue(s > open && s < close, "inside the pre-add consumer: " + step);
        }
        int tag = at(c, "body.getPersistentDataContainer().set(ctx.keys().summonOwner,", close);
        assertTrue(tag > close && tag < end(c, spawn), "the tag goes on AFTER the add, once the store owns it");
    }

    @Test
    void theWeaponPathHandsASummonCastOverBeforeItIsCharged() throws IOException {
        List<String> c = code(Path.of("weapon", "WeaponFire.java"));
        int intercept = at(c, "if (binding.get().ability().cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Summon summonCast) {", 0);
        int handed = at(c, "return Optional.of(summons.cast(player, weapon, summonCast, score,", intercept);
        int engine = at(c, "Optional<CastResult> result = Optional.of(weaponService.fire(caster, binding.get(), aim));", handed);
        assertTrue(intercept > 0 && handed > intercept && engine > handed, "handed over before the engine's charge");
    }

    @Test
    void everyRemovalCauseHasItsHook() throws IOException {
        List<String> l = code(Path.of("listener", "RpgListeners.java"));
        String[][] hooks = {
                {"public void onSummonOwnerDeath(PlayerDeathEvent event) {", "SummonRemoval.OWNER_DEATH"},
                {"public void onSummonOwnerChangedWorld(", "SummonRemoval.WORLD_CHANGE"},
                {"public void onSummonBodyDeath(", "SummonRemoval.DEATH"},
                {"public void onQuit(PlayerQuitEvent event) {", "SummonRemoval.QUIT"},
                {"public void onSummonDamaged(EntityDamageEvent event) {", "SummonRemoval.VOID"},
                {"public void onSummonDamaged(EntityDamageEvent event) {", "SummonRemoval.KILLED"},
                {"public void onSummonBodyAppear(EntityAddToWorldEvent event) {", "SummonRemovals.sweepAppeared(adapters,"},
        };
        for (String[] h : hooks) {
            int m = at(l, h[0], 0);
            int cause = at(l, h[1], m);
            assertTrue(m > 0 && cause > m && cause < end(l, m), h[1] + " inside " + h[0]);
        }
        int damaged = at(l, "public void onSummonDamaged(EntityDamageEvent event) {", 0);
        assertTrue(l.get(damaged - 1).contains("@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)"),
                "Q4 is decided FIRST, before every other damage handler meets a summon");
        List<String> d = code(Path.of("selftest", "SelfTestDrivers.java"));
        assertTrue(at(d, "SummonRemoval.CLEAR);", 0) > 0, "clear_state");
        List<String> p = code(Path.of("RpgPlugin.java"));
        assertTrue(at(p, "scheduler.onGlobalLater(summonBackstop[0], io.github.butterflysmp.rpg.core.summon.SummonBackstop.PERIOD_TICKS);", 0) > 0,
                "the backstop, every 20 ticks");
        assertTrue(at(p, "SummonRemovals.sweep(adapters, e));", 0) > 0, "the enable sweep");
        int disable = at(p, "public void onDisable() {", 0);
        assertTrue(at(p, "SummonRemovals.removeAll(adapters);", disable) > disable, "stop");
    }

    /**
     * O1, AS FAR AS A TEST REACHES: its customHP is bootstrapped as a NON-player, so MobDeathSystem.shouldKill
     * (reachedZero && !targetIsPlayer) kills it by setHealth(0), which (READ) raises EntityDeathEvent and no damage event;
     * the hook lets the store go only for a death that stands (MONITOR, ignoreCancelled). What is left: the boot.
     */
    @Test
    void aSummonCanDieAndOnlyAStandingDeathRemovesIt() throws IOException {
        List<String> s = code(Path.of("summon", "SummonService.java"));
        assertTrue(at(s, "ctx.stats().bootstrapIfAbsent(mob.getUniqueId(), placed[0].health(), 0.0, false);", 0) > 0,
                "bootstrapped as a non-player");
        List<String> d = code(Path.of("health", "MobDeathSystem.java"));
        assertTrue(at(d, "return change.reachedZero() && !change.targetIsPlayer();", 0) > 0, "shouldKill");
        assertTrue(at(d, "mob.setHealth(0);", 0) > 0, "the kill");
        List<String> l = code(Path.of("listener", "RpgListeners.java"));
        int hook = at(l, "public void onSummonBodyDeath(", 0);
        assertTrue(l.get(hook - 1).contains("@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)"),
                "only a death that stands");
    }

    /** O4: both weapon listeners show a refusal through the ONE throttle (the cooldown tracker's reserved key). */
    @Test
    void aRefusedSummonsNoticeIsThrottledByTheExistingMechanism() throws IOException {
        assertTrue(at(code(Path.of("listener", "RpgListeners.java")),
                "SummonNotice.notify(event.getPlayer(), cooldowns, refused.notice());", 0) > 0);
        assertTrue(at(code(Path.of("packet", "WeaponSwingListener.java")),
                "SummonNotice.notify(player, cooldowns, refused.notice());", 0) > 0);
        List<String> n = code(Path.of("summon", "SummonNotice.java"));
        int ready = at(n, "if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;", 0);
        assertTrue(ready > 0 && at(n, "cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);", ready) > ready);
    }

    /** The seat: "no Java names a wolf except what D1 must switch off, each behind a check of the entity's type". */
    @Test
    void noSummonCodeNamesAWolf() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN.resolve("summon"))) {
            List<String> named = files.filter(f -> f.toString().endsWith(".java")).flatMap(f -> {
                try {
                    return code(MAIN.relativize(f)).stream().filter(x -> x.contains("Wolf") || x.contains("WOLF"))
                            .map(x -> f.getFileName() + ": " + x.trim());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
            assertEquals(List.of(), named);
        }
    }
}
