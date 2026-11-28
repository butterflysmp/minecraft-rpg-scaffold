package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.paper.build.BuildService;
import io.github.butterflysmp.rpg.storage.CellLoadout;
import io.github.butterflysmp.rpg.storage.FileBuildRepository;
import io.github.butterflysmp.rpg.storage.PlayerBuild;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S D2 (2026-11-27): a run that picks an ability in the Build screen puts his stored builds back EXACTLY, through
 * the build service's real write, and reads them back. The store here is the real {@link FileBuildRepository} on a
 * temporary folder, with a same-thread executor, so a write has landed when its call returns and the file's BYTES are
 * the reading.
 */
class SelfTestBuildsTest {

    @TempDir Path dir;

    private final UUID him = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private static final CellLoadout MAGE_SAVED = new CellLoadout("mage", "fire", "ultimate_placeholder_mage",
            Arrays.asList("solar_lance", null), Arrays.asList("lingering_sun", null), Arrays.asList("fragment_vigor", null, null, null));
    private static final CellLoadout MELEE_SAVED = new CellLoadout("melee", "fire", "ultimate_placeholder_melee",
            Arrays.asList("sunder", "void_slash"), null, null);

    private FileBuildRepository repo() {
        return new FileBuildRepository(dir, Runnable::run);
    }

    private BuildService joined(FileBuildRepository repo) {
        BuildService service = new BuildService(repo, Logger.getAnonymousLogger());
        service.onJoin(him).join();
        return service;
    }

    private SelfTestBuilds.Port port(BuildService service) {
        return new SelfTestBuilds.Port() {
            @Override public Optional<PlayerBuild> read() { return service.build(him); }
            @Override public boolean write(PlayerBuild whole) { return service.restore(him, whole, ok -> { }); }
        };
    }

    private Path file() throws IOException {
        try (var files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().contains(him.toString())
                    && !f.getFileName().toString().endsWith(".tmp")).findFirst().orElseThrow();
        }
    }

    /**
     * SM1 AND SF1, IN THE FAKE: his file holds a mage/fire loadout naming the DELETED placeholder (an unoffered id) and a
     * melee/fire one. A row picks Smolder into mage/fire and Solar Flare into ranger/fire, a cell he had NO loadout for.
     * The restore puts the file back BYTE FOR BYTE: the unoffered id is back, and ranger/fire is GONE, not left as a
     * default written to disk.
     */
    @Test
    void aRunThatSavesPicksPutsHisFileBackByteForByte() throws IOException {
        var repo = repo();
        repo.save(new PlayerBuild(PlayerBuild.CURRENT_SCHEMA_VERSION, him, List.of(MAGE_SAVED, MELEE_SAVED))).join();
        byte[] before = Files.readAllBytes(file());
        BuildService service = joined(repo);
        var port = port(service);
        SelfTestBuilds builds = SelfTestBuilds.capture(port);

        assertTrue(service.save(him, MAGE_SAVED.withUltimate("smolder"), ok -> { }));
        assertTrue(service.save(him, CellLoadout.empty("ranger", "fire").withUltimate("solar_flare"), ok -> { }));
        assertFalse(Arrays.equals(before, Files.readAllBytes(file())), "CONTROL: the picks reached the file");

        assertEquals(Optional.empty(), builds.restore(port));
        assertArrayEquals(before, Files.readAllBytes(file()), "his file, byte for byte");
        assertEquals(List.of(MAGE_SAVED, MELEE_SAVED), service.build(him).orElseThrow().cells(), "and the service holds it");
    }

    /** A run that changed nothing writes nothing: no file is created for a player who had none. */
    @Test
    void aRunThatChangedNothingWritesNothing() throws IOException {
        var repo = repo();
        BuildService service = joined(repo);
        SelfTestBuilds builds = SelfTestBuilds.capture(port(service));
        assertEquals("[]", builds.fields());
        assertEquals(Optional.empty(), builds.restore(port(service)));
        try (var files = Files.list(dir)) {
            assertEquals(0, files.filter(f -> f.getFileName().toString().contains(him.toString())).count(), "no file made");
        }
    }

    /** The CAPTURED and RESTORED field, whole, in stored order; a null slot as '-'. */
    @Test
    void theFieldNamesEachCellAndItsSlots() {
        assertEquals("[mage/fire:Q=ultimate_placeholder_mage,L=solar_lance,R=-,asp=lingering_sun|-,frag=fragment_vigor|-|-|-;"
                        + "melee/fire:Q=ultimate_placeholder_melee,L=sunder,R=void_slash,asp=-|-,frag=-|-|-|-]",
                SelfTestBuilds.summary(new PlayerBuild(1, him, List.of(MAGE_SAVED, MELEE_SAVED))));
    }

    /** NOT-RESTORED names every cell that differs, is missing or is extra; not captured and refused say so. */
    @Test
    void notRestoredNamesWhatDiffers() {
        assertEquals(List.of("mage/fire", "melee/fire"), SelfTestBuilds.differing(List.of(MAGE_SAVED, MELEE_SAVED),
                List.of(MELEE_SAVED, MAGE_SAVED)), "order counts");
        assertEquals(List.of("ranger/fire"), SelfTestBuilds.differing(List.of(MAGE_SAVED),
                List.of(MAGE_SAVED, CellLoadout.empty("ranger", "fire"))), "an extra cell");
        assertEquals(List.of(), SelfTestBuilds.differing(List.of(MAGE_SAVED), List.of(MAGE_SAVED)));

        SelfTestBuilds.Port unloaded = new SelfTestBuilds.Port() {
            @Override public Optional<PlayerBuild> read() { return Optional.empty(); }
            @Override public boolean write(PlayerBuild whole) { throw new AssertionError("never written"); }
        };
        SelfTestBuilds none = SelfTestBuilds.capture(unloaded);
        assertEquals("not-captured", none.fields());
        assertEquals(Optional.of("builds(not-captured)"), none.restore(unloaded));

        PlayerBuild[] store = {new PlayerBuild(1, him, List.of(MAGE_SAVED))};
        SelfTestBuilds.Port refusing = new SelfTestBuilds.Port() {
            @Override public Optional<PlayerBuild> read() { return Optional.of(store[0]); }
            @Override public boolean write(PlayerBuild whole) { return false; }
        };
        SelfTestBuilds held = SelfTestBuilds.capture(refusing);
        store[0] = store[0].with(MAGE_SAVED.withUltimate("smolder"));
        assertEquals(Optional.of("builds(store-refused)"), held.restore(refusing));
    }

    /** The snapshot captures before any row and restores in its one restore, after the cell, and both lines carry it. */
    @Test
    void theSnapshotCapturesAndRestoresBuilds() throws IOException {
        String s = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestSnapshot.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int ctor = s.indexOf("private SelfTestSnapshot(Player p,");
        assertTrue(s.indexOf("this.builds = SelfTestBuilds.capture(buildPort);", ctor) > ctor);
        assertTrue(s.contains("+ \" builds=\" + builds.fields();"), "CAPTURED carries builds=");
        int restore = s.indexOf("boolean cell = classId == null || profiles.setCell(id, classId, elementId);");
        int put = s.indexOf("java.util.Optional<String> buildsNot = builds.restore(buildPort);", restore);
        assertTrue(restore > 0 && put > restore, "after the cell");
        assertTrue(s.contains("accessoriesLine += \" builds=\" + builds.fields() + buildsNot.map(why -> \" NOT-RESTORED=\" + why)"),
                "RESTORED carries builds= and NOT-RESTORED=builds(...)");
        assertTrue(s.contains("return service.restore(id, whole,"), "through the build service's real write");
    }
}
