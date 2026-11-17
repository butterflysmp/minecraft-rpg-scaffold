package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phage's blast (PLAN-dragon-mobs.md step R3): the wiring that needs a live server and so has no other unit row.
 * The rules are core's ({@code MobBlastTest}: the damage table, the pricing, the radius, the predicate). Comment lines
 * are filtered from the scans, so a javadoc that quotes a call cannot satisfy a needle.
 */
class DragonBlastWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    /**
     * THE PRIME STAYS CANCELLED (the seat's ruling 21): our creeper's {@code ExplosionPrimeEvent} is cancelled FIRST,
     * so no vanilla explosion and no block damage, ever; then a frozen Phage stops there; only then is our detonation
     * run. The per-prime CREEPERGUARD line is gone (ruling 26); the ignite and power lines stay.
     */
    @Test
    void thePrimeIsCancelledBeforeOurDetonationRuns() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        int handler = indexOf(code, "public void onCustomCreeperPrime(ExplosionPrimeEvent event) {");
        int cancel = indexAfter(code, handler, "event.setCancelled(true);");
        int frozen = indexAfter(code, handler, "if (isFrozen(creeper)) return;");
        int detonate = indexAfter(code, handler, "MobBlaster.detonate(creeper,");
        assertTrue(handler > 0 && handler < cancel && cancel < frozen && frozen < detonate && detonate <= handler + 12,
                "handler < cancel < frozen < detonate: " + handler + " " + cancel + " " + frozen + " " + detonate);
        assertTrue(code.stream().noneMatch(l -> l.contains("setCancelled(false)")), "nothing un-cancels an event");
        assertTrue(code.stream().noneMatch(l -> l.contains("CREEPERGUARD prime cancelled")), "the per-prime line is gone");
        assertTrue(code.stream().anyMatch(l -> l.contains("CREEPERGUARD ignite cancelled")), "the ignite line stays");
        assertTrue(code.stream().anyMatch(l -> l.contains("CREEPERGUARD power cancelled")), "the power line stays");
    }

    /**
     * NO SPLIT ON A BLAST (Ben, E; the seat's ruling 22): the Phage is REMOVED, which raises no death event, so the one
     * death handler, and the split inside it, never sees it. The blaster never kills it, never damages it, and never
     * touches the split.
     */
    @Test
    void theBlastRemovesThePhageWithoutADeath() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobBlaster.java")));
        assertTrue(code.size() > 40, "must have read MobBlaster, read " + code.size());
        assertTrue(code.stream().anyMatch(l -> l.contains("phage.remove();")), "removed");
        for (String never : List.of("setHealth(", "MobSplitter", "mobSplit", "EntityDeathEvent", "phage.damage(",
                "createExplosion(")) {
            assertFalse(code.stream().anyMatch(l -> l.contains(never)), "MobBlaster must not contain " + never);
        }
    }

    /**
     * A BLAST IS ONE VOID DEATH FOR GROWTH (the seat's ruling 23): the blaster calls growth's death path, which marks
     * the Phage counted (once only) and passes the PHAGE'S OWN id as the dead creature, so it never grows itself; and
     * it does so BEFORE the removal, while the Phage still stands where it blew.
     */
    @Test
    void theBlastFeedsGrowthOnceBeforeThePhageIsRemoved() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobBlaster.java")));
        int grow = indexOf(code, "MobGrower.onDeath(phage,");
        int remove = indexOf(code, "phage.remove();");
        assertTrue(grow > 0 && grow < remove, "growth, then removal: " + grow + " " + remove);
        assertTrue(code.stream().filter(l -> l.contains("MobGrower.")).count() == 1, "one report, not two");
    }

    /**
     * THE SAME PATH AS A MOB'S MELEE HIT (the seat's ruling 25): each victim is hit through
     * {@code BukkitCombatant.of(victim, ...).handle().applyDamage(dealt, phageId)}, the two-argument form
     * {@code RpgListeners.onMobMeleeAttack} uses, so the store applies the victim's defence and the hit wears no element.
     */
    @Test
    void eachVictimIsHitThroughTheMeleePath() throws IOException {
        List<String> blaster = codeLines(MAIN.resolve(Path.of("health", "MobBlaster.java")));
        assertTrue(blaster.stream().anyMatch(l -> l.contains(".handle().applyDamage(dealt, phageId)")), "two-arg hit");
        assertTrue(blaster.stream().anyMatch(l -> l.contains("double dealt = base * hit.factor();")),
                "each victim takes the zero-distance damage times its share of vanilla's curve");
        List<String> listeners = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        assertTrue(listeners.stream().anyMatch(l -> l.contains(".applyDamage(exchange.applied(), attacker.getUniqueId());")),
                "the melee hit this mirrors is still the two-argument form");
    }

    /** The fuse is content: every spawn of a mob with a blast writes its fuse through the API. */
    @Test
    void theFuseIsWrittenAtSpawn() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobSpawner.java")));
        assertTrue(code.stream().anyMatch(l -> l.contains("creeper.setMaxFuseTicks(def.blast().fuseTicks());")),
                "the fuse from content");
    }

    private static int indexOf(List<String> code, String needle) {
        return indexAfter(code, -1, needle);
    }

    private static int indexAfter(List<String> code, int from, String needle) {
        for (int i = from + 1; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(l -> {
                    String t = l.trim();
                    return !(t.startsWith("//") || t.startsWith("*") || t.startsWith("/*"));
                })
                .toList();
    }
}
