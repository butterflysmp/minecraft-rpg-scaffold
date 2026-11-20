package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F21 (PLAN-dragon-mobs.md; the seat's queue, item 4, 2026-11-19): THE PHALANX'S BLINK COOLDOWN SURVIVES AN UNLOAD. Its
 * end is saved on the mob when a blink lands; a loop that starts again resumes it. The pure half is core's
 * {@code MobBlinkCooldownTest} (the resume, clamped). Pinned from the source, comments stripped.
 */
class BlinkCooldownPersistTest {

    private static String code() throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/health/MobShooter.java"))
                .replace("\r\n", "\n").replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    void aLandedBlinkSavesItsEndOnTheMob() throws IOException {
        String c = code();
        int landed = c.indexOf("o.cooldown.landed();");
        int save = c.indexOf("mob.getPersistentDataContainer().set(adapters.keys().blinkReadyAt,\n"
                + "                org.bukkit.persistence.PersistentDataType.LONG, (long) Bukkit.getCurrentTick() + o.cooldown.length());");
        assertTrue(landed > 0 && save > landed, "saved after it lands");
    }

    @Test
    void aStartingLoopResumesWhatIsLeft() throws IOException {
        String c = code();
        assertTrue(c.contains("Owner o = new Owner(mob, mobId, def.shot(), def.blink());\n        resumeCooldown(o);"),
                "resumed as the loop starts");
        int from = c.indexOf("private void resumeCooldown(Owner o) {");
        String r = c.substring(from, c.indexOf("\n    }\n", from));
        assertTrue(r.contains(".get(adapters.keys().blinkReadyAt,"), r);
        assertTrue(r.contains("long left = readyAt - Bukkit.getCurrentTick();"), r);
        assertTrue(r.contains("o.cooldown.resume("), r);
    }
}
