package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.ArenaLayout;
import io.github.butterflysmp.rpg.core.fight.ArenaShape;
import io.github.butterflysmp.rpg.core.fight.TemplateLayout;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

/**
 * THE TEMPLATE-FILE ARENA SOURCE (PLAN-dragon-mobs.md C3; the seat's queue, item 2), the second side of the arena seam.
 * A template is a saved structure, {@code <plugin>/arenas/<name>.nbt}, its podium and crystal spots marked with MARKER
 * BLOCKS ({@link TemplateLayout}): CC's proposal, flagged for Ben, the markers SEAT PLACEHOLDERS.
 *
 * <p>READ from the pinned jar (javap): {@code Structure.fill} refuses only a size below 1x1x1, and stores every block
 * but {@code structure_void}, air included. {@code Structure.place} runs on the calling thread, at integrity 1.0 here.
 * The 48-a-side limit is the in-game STRUCTURE BLOCK's ({@code StructureBlockEntity.MAX_SIZE_PER_AXIS}), not the
 * template's, so a 113-wide arena can be saved and placed by the API, but not saved with one structure block. That is
 * why {@link #export} exists: it is the builder's save, too.
 */
final class ArenaTemplates {

    /** The podium marker: one, standing ON the floor at the centre (SEAT PLACEHOLDER). */
    static final Material PODIUM_MARKER = Material.TARGET;
    /** A crystal marker: one per crystal, where the crystal will stand (SEAT PLACEHOLDER). */
    static final Material CRYSTAL_MARKER = Material.LODESTONE;

    /** A template read and checked, ready to place. */
    record Loaded(String name, Structure structure, TemplateLayout.Ok ok) {}

    private final Path dir;
    private final Logger log;

    ArenaTemplates(Path dir, Logger log) {
        this.dir = dir;
        this.log = log;
    }

    Path fileOf(String name) {
        return dir.resolve(name + ".nbt");
    }

    /** The template, or the reason it is refused (the caller logs it and builds the generated arena). */
    Object load(String name, int floorY, int crystalCount) {
        Path file = fileOf(name);
        if (!Files.isRegularFile(file)) return "no file " + file;
        Structure s;
        try {
            s = Bukkit.getStructureManager().loadStructure(file.toFile());
        } catch (IOException | RuntimeException e) {
            return "unreadable (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")";
        }
        if (s.getPalettes().isEmpty()) return "no blocks in it";
        List<TemplateLayout.Marker> markers = new ArrayList<>();
        for (BlockState b : s.getPalettes().get(0).getBlocks()) {
            if (b.getType() == PODIUM_MARKER) {
                markers.add(new TemplateLayout.Marker(b.getX(), b.getY(), b.getZ(), TemplateLayout.Kind.PODIUM));
            } else if (b.getType() == CRYSTAL_MARKER) {
                markers.add(new TemplateLayout.Marker(b.getX(), b.getY(), b.getZ(), TemplateLayout.Kind.CRYSTAL));
            }
        }
        BlockVector size = s.getSize();
        TemplateLayout.Result r = TemplateLayout.read(size.getBlockX(), size.getBlockY(), size.getBlockZ(), markers,
                floorY, crystalCount);
        if (r instanceof TemplateLayout.Refused refused) return refused.why();
        return new Loaded(name, s, (TemplateLayout.Ok) r);
    }

    /** Places the template and removes its markers. Returns the layout the fight reads. */
    ArenaLayout place(World w, Loaded t) {
        long t0 = System.nanoTime();
        TemplateLayout.Ok ok = t.ok();
        t.structure().place(new Location(w, ok.originX(), ok.originY(), ok.originZ()), false, StructureRotation.NONE,
                Mirror.NONE, 0, 1.0f, new Random(0));
        for (int[] m : ok.markersWorld()) w.getBlockAt(m[0], m[1], m[2]).setType(Material.AIR, false);
        BlockVector size = t.structure().getSize();
        log.info(String.format(Locale.ROOT, "FIGHTWORLD template placed name=%s world=%s size=%d %d %d origin=%d %d %d"
                        + " crystals=%d markers_removed=%d ms=%d", t.name(), w.getName(), size.getBlockX(),
                size.getBlockY(), size.getBlockZ(), ok.originX(), ok.originY(), ok.originZ(),
                ok.layout().crystalSpots().size(), ok.markersWorld().size(), (System.nanoTime() - t0) / 1_000_000L));
        return ok.layout();
    }

    /**
     * EXPORTS an arena to {@code arenas/<name>.nbt}: the box of the generated shape (its disc and the pillars, one block
     * above for the crystals), with a PODIUM marker on the floor at C and a CRYSTAL marker at each of the layout's
     * spots, written into the world only for the fill and put back after. So the file is a valid template of the arena
     * it was taken from, and a builder's starting point.
     */
    String export(World w, ArenaShape shape, ArenaLayout layout, String name) {
        if (!name.matches("[a-z0-9_]{1,64}")) return "A template name is a-z, 0-9 and _ (got '" + name + "').";
        long t0 = System.nanoTime();
        int r = shape.discRadius();
        Location corner = new Location(w, layout.centreX() - r, shape.floorBottomY(), layout.centreZ() - r);
        BlockVector size = new BlockVector(2 * r + 1, shape.pillarTopY() + 2 - shape.floorBottomY(), 2 * r + 1);
        List<int[]> marks = new ArrayList<>();
        marks.add(new int[] {layout.centreX(), layout.floorY() + 1, layout.centreZ()});
        for (double[] s : layout.crystalSpots()) {
            marks.add(new int[] {(int) Math.floor(s[0]), (int) Math.floor(s[1]), (int) Math.floor(s[2])});
        }
        List<Material> was = new ArrayList<>();
        for (int i = 0; i < marks.size(); i++) {
            int[] m = marks.get(i);
            was.add(w.getBlockAt(m[0], m[1], m[2]).getType());
            w.getBlockAt(m[0], m[1], m[2]).setType(i == 0 ? PODIUM_MARKER : CRYSTAL_MARKER, false);
        }
        Path file = fileOf(name);
        try {
            Structure s = Bukkit.getStructureManager().createStructure();
            s.fill(corner, size, false);
            Files.createDirectories(dir);
            Bukkit.getStructureManager().saveStructure(file.toFile(), s);
        } catch (IOException e) {
            return "The template could not be written: " + e.getMessage();
        } finally {
            for (int i = 0; i < marks.size(); i++) {
                int[] m = marks.get(i);
                w.getBlockAt(m[0], m[1], m[2]).setType(was.get(i), false);
            }
        }
        String line = String.format(Locale.ROOT, "FIGHTWORLD template exported name=%s file=%s size=%d %d %d"
                        + " corner=%d %d %d markers=%d ms=%d", name, file, size.getBlockX(), size.getBlockY(),
                size.getBlockZ(), corner.getBlockX(), corner.getBlockY(), corner.getBlockZ(), marks.size(),
                (System.nanoTime() - t0) / 1_000_000L);
        log.info(line);
        return line;
    }
}
