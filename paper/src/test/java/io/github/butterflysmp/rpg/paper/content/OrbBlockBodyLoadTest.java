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
                "d.setPersistent(false);"}) {
            assertTrue(spawn.contains(want), "spawnOrbBlock: " + want);
        }
        // CORRECTED 2026-11-23 (the seat's fix of #265): this test pinned BOTH tags INSIDE the spawn's consumer, the
        // fault the run of 78826389 found. The consumer runs before the body is added to the world, where the orphan
        // sweep runs (READ), so it now carries NO tag; the marker tag goes on after the spawn returns, the orb tag only
        // at claimOrbBody, after the store owns the body.
        String consumer = spawn.substring(spawn.indexOf("org.bukkit.entity.BlockDisplay.class, d -> {"), spawn.indexOf("        });"));
        assertTrue(!consumer.contains("ctx.keys().orb") && !consumer.contains("ctx.keys().markerEntity"),
                "no tag inside the spawn's consumer: " + consumer);
        assertTrue(spawn.indexOf("body.getPersistentDataContainer().set(ctx.keys().markerEntity, PersistentDataType.BYTE, (byte) 1);")
                > spawn.indexOf("        });"), "the marker tag after the spawn returns");
        assertTrue(!spawn.contains("ctx.keys().orb"), "spawnOrbBlock never tags the orb");
        String world = src("adapter/PaperCombatWorld.java");
        assertTrue(!method(world, "public UUID spawnOrbMarker(").contains("ctx.keys().orb"), "nor does spawnOrbMarker");
        assertTrue(method(world, "public void claimOrbBody(").contains(
                "body.getPersistentDataContainer().set(ctx.keys().orb, PersistentDataType.BYTE, (byte) 1);"),
                "the one orb-tag write is the claim");
        assertEquals(1, world.split("ctx\\.keys\\(\\)\\.orb,", -1).length - 1, "and it is the only one in the file");
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

    /**
     * THE SWEEP SAYS WHAT OCCURRED (the seat's fix of #265, 2026-11-23). At the add a removal is refused (READ), so the
     * sweep where a body appears defers to the body's own scheduler a tick later; and the {@code removed … cause=orphan}
     * line is written only when the body is gone ({@code !isValid()}). On 78826389 seven such lines reported removals
     * Paper had refused.
     */
    @Test
    void theSweepWaitsATickAtTheAddAndLogsOnlyARemovalThatHappened() throws IOException {
        String removals = src("weapon/OrbRemovals.java");
        String appeared = removals.substring(removals.indexOf("public static void sweepAppeared("),
                removals.indexOf("private static boolean orphan("));
        assertTrue(appeared.contains("if (!orphan(ctx, entity)) return;")
                && appeared.contains("ctx.scheduler().onEntityLater(entity, () -> sweep(ctx, entity), 1);")
                && !appeared.contains("entity.remove()"), "no removal at the add; the sweep a tick later: " + appeared);
        String sweep = removals.substring(removals.indexOf("public static void sweep("),
                removals.indexOf("public static void sweepAppeared("));
        int remove = sweep.indexOf("entity.remove();");
        int check = sweep.indexOf("if (!entity.isValid()) {");
        int line = sweep.indexOf("\"orphan\"");
        assertTrue(remove > 0 && check > remove && line > check, "the line only inside the check after the removal: " + sweep);
    }

    @Test
    void theLoadCheckNamesABodyThatIsNotABlock() throws IOException, InvalidConfigurationException {
        var grant = new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.GrantTrigger("lapis_staff", "left_click",
                new io.github.butterflysmp.rpg.core.ability.AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal",
                        "kinetic", 600, new io.github.butterflysmp.rpg.core.ability.ResourceCost("mana", 30),
                        parse(ORB + "body: block\nblock:\n  id: lapis_blok\n  scale: 0.5\n  beat_ticks: 10\n"
                                + "  particles: crystal_mote\n"),
                        java.util.List.of()));
        var enchant = new EnchantDefinition("lapis_crystal", "Lapis Crystal", 3,
                io.github.butterflysmp.rpg.core.enchant.EnchantEffect.GRANT_TRIGGER, null, java.util.List.of(),
                "heart_of_the_sea", null, null, "lapis_staff", grant);
        assertEquals(java.util.List.of("enchant 'lapis_crystal' grants an orb whose body block 'lapis_blok' is not a block:"
                        + " its throw will spawn no body",
                        "enchant 'lapis_crystal' grants an orb whose particles 'crystal_mote' is not a loaded visual: it will"
                                + " present nothing at rest"),
                ContentValidator.validateOrbBodies(java.util.List.of(enchant), id -> id.equals("lapis_block"),
                        id -> id.equals("crystal_motes")));
        assertEquals(java.util.List.of(), ContentValidator.validateOrbBodies(java.util.List.of(enchant), id -> true, id -> true));
    }

    /** Ben's changes (2026-11-23): no glow is a body that does not glow; the look keys parse, and refuse no beat by name. */
    @Test
    void noGlowAndTheLookKeysParse() throws InvalidConfigurationException {
        CastSpec.Orb orb = parse(ORB + "body: block\nblock:\n  id: lapis_block\n  scale: 0.5\n  bob_blocks: 0.1\n"
                + "  bob_period_ticks: 60\n  turn_ticks: 120\n  beat_ticks: 10\n  particles: crystal_motes\n");
        assertEquals(new CastSpec.OrbBlock("lapis_block", 0.5, null, 0.1, 60, 120, 10, "crystal_motes"), orb.block());
        assertEquals(-1, orb.block().glowRgb());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> parse(ORB
                + "body: block\nblock:\n  id: lapis_block\n  scale: 0.5\n  turn_ticks: 120\n")).getMessage()
                .contains("need a beat_ticks above 0"));
        assertEquals("ORBBODY owner=00000000-0000-0000-0000-000000000001 id=00000000-0000-0000-0000-000000000002"
                        + " type=block_display block=minecraft:lapis_block glowing=false glow=none scale=0.500"
                        + " teleport_duration=1 persistent=false tick=1",
                PaperCombatWorld.orbBodyLine(new UUID(0, 1), new UUID(0, 2), "block_display", "minecraft:lapis_block",
                        false, -1, 0.5f, 1, false, 1));
    }

    /**
     * THE POSE THROUGH THE SERVER'S OWN ARITHMETIC: the transformation {@code poseOrbBody} builds, composed as
     * {@code Transformation.compose} does (READ: {@code new Matrix4f().translation(t).rotate(left).scale(s).rotate(right)}),
     * puts the block model's centre {@code (1/2, 1/2, 1/2)} at {@code (0, bob, 0)}: over the rest point, at every tick of a
     * turn. And {@code poseOrbBody} is wired that way, with the interpolation set before the transformation.
     */
    @Test
    void thePoseThroughTheServersComposeKeepsTheCentreOverTheRestPoint() throws IOException {
        CastSpec.OrbBlock look = new CastSpec.OrbBlock("lapis_block", 0.5, null, 0.1, 60, 120, 10, null);
        for (long t = 0; t <= 120; t++) {
            CastSpec.OrbBlock.Pose p = look.pose(t);
            org.joml.Matrix4f m = new org.joml.Matrix4f()
                    .translation(new org.joml.Vector3f((float) p.tx(), (float) p.ty(), (float) p.tz()))
                    .rotate(new org.joml.Quaternionf(new org.joml.AxisAngle4f((float) p.yaw(), 0, 1, 0)))
                    .scale(new org.joml.Vector3f(0.5f, 0.5f, 0.5f));
            org.joml.Vector3f centre = m.transformPosition(new org.joml.Vector3f(0.5f, 0.5f, 0.5f));
            assertEquals(0, centre.x(), 1e-6, "t=" + t);
            assertEquals(0.1 * Math.sin(2 * Math.PI * t / 60), centre.y(), 1e-6, "t=" + t);
            assertEquals(0, centre.z(), 1e-6, "t=" + t);
        }
        String pose = method(src("adapter/PaperCombatWorld.java"), "public void poseOrbBody(");
        int delay = pose.indexOf("d.setInterpolationDelay(0);");
        int duration = pose.indexOf("d.setInterpolationDuration(interpolationTicks);");
        int set = pose.indexOf("d.setTransformation(new org.bukkit.util.Transformation(");
        assertTrue(delay > 0 && duration > delay && set > duration, pose);
        assertTrue(pose.contains("new org.joml.AxisAngle4f((float) pose.yaw(), 0, 1, 0), scale, new org.joml.AxisAngle4f()));"));
        assertTrue(!pose.contains("teleport"), "the position is not touched");
        assertTrue(src("weapon/OrbRemovals.java").contains("if (orb.lookTask() >= 0) ctx.work().take(orb.lookTask());"),
                "every paper removal ends the look beat");
    }
}
