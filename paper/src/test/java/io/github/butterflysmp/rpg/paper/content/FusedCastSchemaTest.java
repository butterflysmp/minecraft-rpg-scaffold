package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code fuse_ticks} (the seat's F1, 2026-11-26): parsed onto the projectile, absent is 0, and a lifetime beside it is refused. */
class FusedCastSchemaTest {

    private static YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text);
        return y;
    }

    @Test
    void aFuseIsReadAndAbsentIsNone() throws Exception {
        var fused = (CastSpec.Projectile) AbilitySchema.parseCast(yaml(
                "type: projectile\nspeed: 0.7\ngravity: 0.04\nfuse_ticks: 40\nitem: nether_wart_block\n"));
        assertEquals(40, fused.fuseTicks());
        assertTrue(fused.fused());
        var plain = (CastSpec.Projectile) AbilitySchema.parseCast(yaml("type: projectile\nspeed: 1.0\n"));
        assertEquals(0, plain.fuseTicks());
        assertFalse(plain.fused());
    }

    @Test
    void aLifetimeBesideAFuseIsRefusedByName() throws Exception {
        var e = assertThrows(IllegalArgumentException.class, () -> AbilitySchema.parseCast(yaml(
                "type: projectile\nfuse_ticks: 40\nmax_lifetime_ticks: 60\n")));
        assertTrue(e.getMessage().contains("max_lifetime_ticks"), e.getMessage());
    }
}
