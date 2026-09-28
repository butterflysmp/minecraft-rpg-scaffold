package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** M16: A MOB'S HIT ON A PLAYER IS PRICED FLAT, INSTEAD OF DamageScale, NEVER AFTER IT. ***
 *
 * <p>{@code RerouteDamagePriceTest} proves the core chooser takes one price or the other. It cannot
 * see a paper-side wrapper -- {@code DamageScale.toCustom(RerouteDamagePrice.of(...), ...)} in the
 * listener would stack the two (x25 for a GS 100 arrow) with every unit test green. So this scans the
 * source: <b>no code in {@code paper/src/main} calls {@code DamageScale.toCustom}</b>, and the rider
 * calls the chooser exactly once.
 *
 * <p>Comment lines are filtered, as in {@code MobAttributeUntouchedSignatureTest}, because the rider
 * explains the rule in prose beside the call.
 */
class MobDamageWiringSignatureTest {

    private static final Path MAIN = Path.of("src", "main", "java");

    @Test
    void noPaperCodeConvertsWithDamageScaleDirectly() throws IOException {
        List<String> toCustom = new ArrayList<>();
        List<String> chooser = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                scanned++;
                for (String line : codeLines(file)) {
                    if (line.contains("DamageScale.toCustom(")) toCustom.add(file.getFileName() + ": " + line.trim());
                    if (line.contains("RerouteDamagePrice.of(")) chooser.add(file.getFileName() + ": " + line.trim());
                }
            }
        }
        assertTrue(scanned > 100, "the scan must have read paper/src/main, read " + scanned + " files");
        // POSITIVE CONTROL: the chooser call is found, so a needle that matched nothing cannot pass.
        assertEquals(1, chooser.size(), "the rider calls RerouteDamagePrice.of exactly once: " + chooser);
        assertTrue(chooser.get(0).startsWith("RpgListeners.java"), chooser.get(0));
        assertEquals(List.of(), toCustom,
                "M16: DamageScale.toCustom is reached only through RerouteDamagePrice.of. A direct call"
                        + " here either converts a second time (k squared) or stacks on the flat mob price");
    }

    @Test
    void theLaunchHandlerStampsProjectiles() throws IOException {
        Path listeners = MAIN.resolve(Path.of("io", "github", "butterflysmp", "rpg", "paper", "listener",
                "RpgListeners.java"));
        List<String> code = codeLines(listeners);
        assertTrue(code.size() > 1000, "must have read RpgListeners, read " + code.size() + " code lines");
        assertTrue(code.stream().anyMatch(l -> l.contains("nameplates.stampProjectile(event.getEntity())")),
                "G10: without the launch stamp an arrow whose shooter died prices from nothing");
    }

    /**
     * Melee is priced at the SEED (plan §3 slice 2): the stat onMobMeleeAttack reads must be the scaled
     * one. The seed needs a live entity, so no unit test reaches it; without this row, seeding the raw
     * attribute again would leave every mob's melee at x1 and every test green.
     */
    @Test
    void theSeedScalesTheAttackAndStoresTheScaledValue() throws IOException {
        Path seedFile = MAIN.resolve(Path.of("io", "github", "butterflysmp", "rpg", "paper", "health",
                "MobNameplateManager.java"));
        List<String> code = codeLines(seedFile);
        assertTrue(code.size() > 100, "must have read MobNameplateManager, read " + code.size() + " code lines");
        assertTrue(code.stream().anyMatch(l -> l.contains("MobScaling.attackDamage(attackDamageOf(mob)")),
                "the seed must scale the vanilla attack through MobScaling.attackDamage (M16)");
        assertEquals(1, code.stream().filter(l -> l.contains("stats.bootstrapIfAbsent(")).count(),
                "one bootstrap in the seed");
        assertTrue(code.stream().anyMatch(l -> l.contains("stats.bootstrapIfAbsent(mob.getUniqueId(), max, attack,")),
                "the bootstrap must store the SCALED attack, not attackDamageOf(mob) raw");
    }

    /**
     * M24: the rider undoes vanilla's difficulty scaling on the RAW amount and feeds THAT to the window.
     * {@code VanillaDifficultyTest} proves the reversal; only the listener can forget to call it, or call
     * it after the claim, where EASY's affine inverse would be applied to a partial amount.
     */
    @Test
    void theRiderUndoesDifficultyBeforeTheWindow() throws IOException {
        List<String> code = codeLines(listeners());
        int undo = indexOf(code, "VanillaDifficulty.undifficulted(eventAmount");
        int claim = indexOf(code, "damageWindow.claim(id, vanillaAmount, false)");
        assertTrue(undo >= 0, "the rider must undo difficulty (M24)");
        assertTrue(claim >= 0, "the window must claim the undone amount, not event.getDamage()");
        assertTrue(undo < claim, "the reversal must come BEFORE the window claim");
    }

    /** M24's arrow half: the launch handler removes setBaseDamageFromMob's difficulty shift. */
    @Test
    void theLaunchHandlerUndoesTheMobArrowShift() throws IOException {
        assertTrue(indexOf(codeLines(listeners()), "VanillaDifficulty.mobArrowBaseDamage(arrow.getDamage()") >= 0,
                "a skeleton's arrow keeps +0.11 per difficulty level without this (M24)");
    }

    private static Path listeners() {
        return MAIN.resolve(Path.of("io", "github", "butterflysmp", "rpg", "paper", "listener", "RpgListeners.java"));
    }

    private static int indexOf(List<String> code, String needle) {
        for (int i = 0; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !isComment(l)).toList();
    }

    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }
}
