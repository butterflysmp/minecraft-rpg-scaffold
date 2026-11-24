package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ORB'S BODY KIND, LOADED AND WIRED (Ben's item 3, 2026-11-23, PR 2). The schema ({@code AbilitySchema.parseOrb}); the
 * ORBBODY line's shape; and, read off the source because the server's half cannot run in a unit test, that the display is
 * spawned centred, smoothed, glowing, never saved and carrying BOTH tags, and that every B6 removal reaches it.
 */
class OrbBlockBodyLoadTest {

    private static final String ORB = """
            type: orb
            range: 12
            flight_ticks: 10
            life_ticks: 300
            radius: 0.6
            """;

    private static CastSpec.Orb parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(yaml);
        return AbilitySchema.parseOrb(y);
    }

    @Test
    void aBlockBodyParsesAndAnItemBodyIsTheDefault() throws InvalidConfigurationException {
        CastSpec.Orb block = parse(ORB + "body: block\nblock:\n  id: lapis_block\n  scale: 0.5\n  glow: \"#2A5CFF\"\n");
        assertEquals(new CastSpec.OrbBlock("lapis_block", 0.5, "#2A5CFF"), block.block());
        assertNull(block.item());
        CastSpec.Orb item = parse(ORB + "item: heart_of_the_sea\n");
        assertNull(item.block(), "no body: key is an item body, D2's shape");
        assertEquals("heart_of_the_sea", item.item());
    }

    @Test
    void eachBodyRefusesTheOthersKeyAndAnUnknownKind() {
        String section = "block:\n  id: lapis_block\n  scale: 0.5\n  glow: \"#2A5CFF\"\n";
        assertEquals("orb body: block takes no 'item:'", assertThrows(IllegalArgumentException.class,
                () -> parse(ORB + "body: block\nitem: heart_of_the_sea\n" + section)).getMessage());
        assertEquals("orb body: item takes no 'block:' section", assertThrows(IllegalArgumentException.class,
                () -> parse(ORB + "item: heart_of_the_sea\n" + section)).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> parse(ORB + "body: block\n")).getMessage()
                .contains("needs a 'block:' section"));
        assertEquals("orb body must be 'item' or 'block', was 'blok'", assertThrows(IllegalArgumentException.class,
                () -> parse(ORB + "body: blok\n" + section)).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> parse(ORB + "body: block\nblock:\n  id: lapis_block\n  scale: 0.5\n  glow: blue\n")).getMessage()
                .contains("#RRGGBB"));
    }

    @Test
    void theOrbBodyLineNamesWhatWasSpawned() {
        assertEquals("ORBBODY owner=00000000-0000-0000-0000-000000000001 id=00000000-0000-0000-0000-000000000002"
                        + " type=block_display block=minecraft:lapis_block glowing=true glow=#2A5CFF scale=0.500"
                        + " teleport_duration=1 persistent=false tick=6680",
                PaperCombatWorld.orbBodyLine(new UUID(0, 1), new UUID(0, 2), "block_display", "minecraft:lapis_block",
                        true, 0x2A5CFF, 0.5f, 1, false, 6680));
        assertTrue(PaperCombatWorld.orbBodyLine(new UUID(0, 1), new UUID(0, 2), "block_display", "x", false, -1, 1f, 0,
                true, 1).contains(" glow=none "));
    }

    private static String src(String path) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper", path), StandardCharsets.UTF_8)
                .replace("\r", "");
    }

    /** The body of the method whose signature starts at {@code signature}, to the next {@code @Override}. */
    private static String method(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(at > 0, "not found: " + signature);
        int end = src.indexOf("\n    @Override", at);
        return src.substring(at, end < 0 ? src.length() : end);
    }

    @Test
    void theDisplayIsCentredSmoothedGlowingNeverSavedAndCarriesBothTags() throws IOException {
        String spawn = method(src("adapter/PaperCombatWorld.java"), "public UUID spawnOrbBlock(");
        for (String want : new String[]{
                "new org.bukkit.util.Transformation(new org.joml.Vector3f(-s / 2, -s / 2, -s / 2),",
                "new org.joml.AxisAngle4f(), new org.joml.Vector3f(s, s, s), new org.joml.AxisAngle4f()));",
                "d.setTeleportDuration(1);",
                "d.setGlowing(true);",
                "d.setGlowColorOverride(org.bukkit.Color.fromRGB(look.glowRgb()));",
                "d.setPersistent(false);",
                "d.getPersistentDataContainer().set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);",
                "d.getPersistentDataContainer().set(ctx.keys().orb, PersistentDataType.BYTE, (byte) 1);"}) {
            assertTrue(spawn.contains(want), "spawnOrbBlock: " + want);
        }
        String place = method(src("adapter/PaperCombatWorld.java"), "public void placeOrbBody(");
        assertTrue(place.contains("Entity body = markerOf(bodyId);") && place.contains("body.teleport(toLocation(at));"));
    }

    /**
     * EACH B6 PATH REMOVES A BLOCK-DISPLAY BODY: quit, death, world change and stop go through {@code OrbRemovals.discard},
     * which finds the body by its id as an {@code Entity}; the sweep reads the orb tag off any {@code Entity}; expiry,
     * replacement and the backstop go through {@code removeMarker}, which finds it by the marker tag the display carries.
     * None of them asks for an {@code Item}. And discard cancels the backstop.
     */
    @Test
    void everyRemovalReachesADisplayAndCancelsItsBackstop() throws IOException {
        String removals = src("weapon/OrbRemovals.java");
        assertTrue(!removals.contains("Item"), "no removal of an orb asks for an Item");
        assertTrue(removals.contains("Entity body = Bukkit.getEntity(orb.markerId());"), "discard: by id, any entity");
        assertTrue(removals.contains("public static void sweep(AdapterContext ctx, Entity entity) {"), "the sweep: any entity");
        assertTrue(removals.contains("if (orb.backstopTask() >= 0) ctx.work().take(orb.backstopTask());"),
                "discard cancels the backstop");
        String world = src("adapter/PaperCombatWorld.java");
        assertTrue(method(world, "public void removeMarker(").contains("Entity marker = markerOf(markerId);"));
    }

    @Test
    void theLoadCheckNamesABodyThatIsNotABlock() throws IOException, InvalidConfigurationException {
        var grant = new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.GrantTrigger("lapis_staff", "left_click",
                new io.github.butterflysmp.rpg.core.ability.AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal",
                        "kinetic", 600, new io.github.butterflysmp.rpg.core.ability.ResourceCost("mana", 30),
                        parse(ORB + "body: block\nblock:\n  id: lapis_blok\n  scale: 0.5\n  glow: \"#2A5CFF\"\n"),
                        java.util.List.of()));
        var enchant = new EnchantDefinition("lapis_crystal", "Lapis Crystal", 3,
                io.github.butterflysmp.rpg.core.enchant.EnchantEffect.GRANT_TRIGGER, null, java.util.List.of(),
                "heart_of_the_sea", null, null, "lapis_staff", grant);
        assertEquals(java.util.List.of("enchant 'lapis_crystal' grants an orb whose body block 'lapis_blok' is not a block:"
                        + " its throw will spawn no body"),
                ContentValidator.validateOrbBodies(java.util.List.of(enchant), id -> id.equals("lapis_block")));
        assertEquals(java.util.List.of(), ContentValidator.validateOrbBodies(java.util.List.of(enchant), id -> true));
    }
}
