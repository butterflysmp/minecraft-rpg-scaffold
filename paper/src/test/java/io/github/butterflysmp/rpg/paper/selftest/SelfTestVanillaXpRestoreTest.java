package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RUN PUTS HIS VANILLA XP BACK (2026-11-23, GATE-unique-enchant-slot US7: a row stages vanilla XP to buy a candidate
 * at the enchanting table, which charges the vanilla wallet, not Lifetime XP). The snapshot needs a live Player, so this
 * reads its source, as {@code SelfTestDisableWiringTest} does: the capture at construction, the restore inside
 * {@code restore}, and the RESTORED line naming it.
 */
class SelfTestVanillaXpRestoreTest {

    @Test
    void theSnapshotCapturesAndRestoresVanillaLevelAndProgress() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestSnapshot.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        assertTrue(src.contains("this.vanillaLevel = p.getLevel();"), "captured: level");
        assertTrue(src.contains("this.vanillaProgress = p.getExp();"), "captured: progress");
        int restore = src.indexOf("String restore(Player p,");
        assertTrue(restore > 0, "restore is not found");
        String body = src.substring(restore);
        assertTrue(body.contains("p.setLevel(vanillaLevel);"), "restored: level");
        assertTrue(body.contains("p.setExp(vanillaProgress);"), "restored: progress");
        assertTrue(body.contains("\" vanilla_xp=\" + vanillaLevel"), "the RESTORED line names it");
    }
}
