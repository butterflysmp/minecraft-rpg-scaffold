package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.mob.MobHostility.Side;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2b's PROOF (the seat's H6 and H7, 2026-11-24): THE ONE CLASSIFIER on FAKE TAGGED BODIES (proxies, as LiveTargetsTest
 * stubs entities), the guard's decision, and the wiring that makes the damage port, the status port, the three vanilla
 * listeners and the targeting listener ask it before anything else. The listeners need a server to fire; their wiring is
 * read from the source, bounded by each method's own body.
 */
class CombatSidesTest {

    private static final NamespacedKey TAG = new NamespacedKey("rpg", "summon_owner");

    private static <T extends Entity> T body(Class<T> type, boolean tagged) {
        PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
                CombatSidesTest.class.getClassLoader(), new Class<?>[] {PersistentDataContainer.class},
                (p, m, a) -> m.getName().equals("has") ? tagged && TAG.equals(a[0]) : null);
        return type.cast(Proxy.newProxyInstance(CombatSidesTest.class.getClassLoader(), new Class<?>[] {type},
                (p, m, a) -> m.getName().equals("getPersistentDataContainer") ? pdc : null));
    }

    @Test
    void theClassifierOnFakeBodies() {
        assertEquals(Side.SUMMON, CombatSides.of(body(Wolf.class, true), TAG), "a tagged body is a summon");
        assertEquals(Side.MOB, CombatSides.of(body(Wolf.class, false), TAG), "an untagged wolf is a mob");
        assertEquals(Side.MOB, CombatSides.of(body(Zombie.class, false), TAG));
        assertEquals(Side.PLAYER, CombatSides.of(body(Player.class, true), TAG), "a player is never a summon");
        assertEquals(Side.PLAYER, CombatSides.of(body(Player.class, false), TAG));
    }

    @Test
    void theGuardRefusesOnlyTheThreeSummonNoCells() {
        assertTrue(CombatSides.refused(Side.PLAYER, Side.SUMMON));
        assertTrue(CombatSides.refused(Side.SUMMON, Side.PLAYER));
        assertTrue(CombatSides.refused(Side.SUMMON, Side.SUMMON));
        assertFalse(CombatSides.refused(Side.PLAYER, Side.PLAYER), "H3: PvP untouched");
        assertFalse(CombatSides.refused(Side.PLAYER, Side.MOB));
        assertFalse(CombatSides.refused(Side.MOB, Side.SUMMON), "H5: a mob may strike and target a summon");
        assertFalse(CombatSides.refused(Side.SUMMON, Side.MOB));
    }

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(Path file) throws IOException {
        return Files.readAllLines(MAIN.resolve(file), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*")))
                .toList();
    }

    private static int at(List<String> code, String needle, int from) {
        for (int i = Math.max(0, from); i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    @Test
    void theDamageAndStatusPortsAskTheGuardFirst() throws IOException {
        List<String> c = code(Path.of("adapter", "BukkitCombatant.java"));
        int now = at(c, "private void applyNow(", 0);
        int guard = at(c, "if (refusedBySides(sourceId, \"damage\")) return;", now);
        int shot = at(c, "if (ctx.shots().strike(entity, String.format(", now);
        assertTrue(now > 0 && guard > now && shot > guard, "applyNow: the guard, then the shot: " + now + " " + guard + " " + shot);
        int status = at(c, "public void applyStatus(String statusId, int durationTicks, int amplifier,", 0);
        int sGuard = at(c, "if (refusedBySides(applierId, \"status=\" + statusId)) return;", status);
        int sShot = at(c, "if (ctx.shots().strike(entity, \"ability status=\" + statusId)) return;", status);
        assertTrue(status > 0 && sGuard > status && sShot > sGuard, "applyStatus: the guard, then the shot");
        assertTrue(at(c, "if (!CombatSides.refused(source, victim)) return false;", 0) > 0, "read off the one table");
        assertTrue(at(c, "CombatSides.of(entity, keys),", 0) > 0, "the snapshot's side is the classifier's");
    }

    @Test
    void theThreeVanillaListenersAndTargetingAskTheGuardBeforeAnyDamage() throws IOException {
        List<String> c = code(Path.of("listener", "RpgListeners.java"));
        int melee = at(c, "public void onPlayerMeleeAttack(", 0);
        assertTrue(at(c, "if (refusedForPlayer(victim, event)) return;", melee) > melee
                && at(c, "if (refusedForPlayer(victim, event)) return;", melee) < at(c, "event.setDamage(TOKEN_DAMAGE);", melee),
                "the swing: before its damage");
        int sweep = at(c, "public void onPlayerSweepAttack(", 0);
        assertTrue(at(c, "if (refusedForPlayer(swept, event)) return;", sweep) > sweep, "the sweep");
        int shot = at(c, "public void onVanillaShotHit(", 0);
        int shotGuard = at(c, "if (refusedForPlayer(hit.victim(), event)) return;", shot);
        assertTrue(shotGuard > shot && shotGuard < at(c, "vanillaShots.land(hit);", shot), "the vanilla shot: before it lands");
        int target = at(c, "public void onSidesTarget(", 0);
        assertTrue(target > 0 && at(c, "CombatSides.refused(targeter, target)) event.setCancelled(true);", target) > target,
                "targeting");
    }

    /** Q3: the server's castRay, the one place a ray or projectile picks, passes through what the table refuses. */
    @Test
    void theServersCastRayPassesThroughARefusedBody() throws IOException {
        List<String> c = code(Path.of("adapter", "PaperCombatWorld.java"));
        int ray = at(c, "public Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId,", 0);
        int trace = at(c, "RayTraceResult result = world.rayTrace(", ray);
        int skip = at(c, "return body == null || !io.github.butterflysmp.rpg.core.mob.MobHostility.refusedSummonCell(attacker,", ray);
        int sides = at(c, "CombatSides.of(body, ctx.keys()));", skip);
        int next = at(c, "if (result == null) return Optional.empty();", ray);
        assertTrue(ray > 0 && trace > ray && skip > trace && sides == skip + 1 && next > sides,
                "inside castRay's own trace filter, by the classifier: " + ray + " " + trace + " " + skip + " " + next);
    }

    /** H1: no second copy. Every former `instanceof Player ? PLAYER : MOB` in paper main is gone. */
    @Test
    void noSideIsComputedOutsideTheClassifier() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            List<String> copies = files.filter(p -> p.toString().endsWith(".java")).flatMap(p -> {
                try {
                    return Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                            .filter(l -> l.contains("instanceof Player ?") && l.contains("Side.")).map(l -> p + ": " + l);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
            assertEquals(List.of(), copies);
        }
    }
}
