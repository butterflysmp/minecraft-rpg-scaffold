package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * THE PAD, BUILT (Ben's ruling A, 2026-11-23; the seat's P1-P5). {@link SelfTestPadShape} says what every cell is; this
 * puts the world right cell by cell and touches nothing else. DEV SERVER ONLY: its two callers are the selftest run and
 * the dev command, and both stand behind {@link SelfTestGuard}'s dev marker.
 *
 * <p><b>Threading.</b> The chunks are loaded with {@code World#getChunkAtAsync} (never a blocking load), and the cells are
 * then read and written on the REGION that owns the stand point ({@code Scheduler#onRegion}): the Folia rule. Before a
 * single block is touched every chunk is confirmed still loaded; if one is not, nothing is written and the result says
 * so. The pad spans {@code 50 x 29} blocks, at most 4 x 3 chunks: on Folia one region, as regions are far larger than
 * that (NOT READ for Folia; this repo runs Paper).
 */
final class SelfTestPad {

    private SelfTestPad() {}

    /** What a build, a check or a removal did: the PAD line on success, the reason otherwise; and where he stands. */
    record Result(boolean ok, String line, String why, Location stand) {}

    static Material material(SelfTestPadShape.Kind k) {
        return switch (k) {
            case FLOOR -> Material.STONE;
            case RIM -> Material.BARRIER;
            case AIR -> Material.AIR;
        };
    }

    static SelfTestPadShape.Stand standOf(World world) {
        Location spawn = world.getSpawnLocation();
        return SelfTestPadShape.stand(spawn.getBlockX(), spawn.getBlockZ());
    }

    /** Where he is put: the stand block's centre, his feet on the floor, facing due EAST (yaw -90, pitch 0). */
    static Location standLocation(World world, SelfTestPadShape.Stand s) {
        return new Location(world, s.x() + 0.5, s.feetY(), s.z() + 0.5, -90f, 0f);
    }

    /** Build what is missing and repair what is wrong. Completes on the stand point's region thread. */
    static CompletableFuture<Result> ensure(World world, Scheduler scheduler) {
        return onLoaded(world, scheduler, s -> build(world, s));
    }

    /** The dev command's removal: every FLOOR and RIM cell back to air. The interior's air cells are left as they are. */
    static CompletableFuture<Result> remove(World world, Scheduler scheduler) {
        return onLoaded(world, scheduler, s -> {
            int removed = 0;
            int already = 0;
            for (int[] c : solidCells()) {
                Block b = world.getBlockAt(s.x() + c[0], s.floorY() + c[1], s.z() + c[2]);
                if (b.getType().isAir()) {
                    already++;
                } else {
                    b.setType(Material.AIR, false);
                    removed++;
                }
            }
            String line = "PAD removed world=" + world.getName() + " stand=" + s.x() + "," + s.feetY() + "," + s.z()
                    + " removed=" + removed + " already_air=" + already;
            return new Result(true, line, null, standLocation(world, s));
        });
    }

    private static List<int[]> solidCells() {
        List<int[]> out = new ArrayList<>();
        SelfTestPadShape.forEachCell((dx, dy, dz, k) -> {
            if (k != SelfTestPadShape.Kind.AIR) out.add(new int[] {dx, dy, dz});
        });
        return out;
    }

    private static Result build(World world, SelfTestPadShape.Stand s) {
        int[] t = new int[3];
        SelfTestPadShape.forEachCell((dx, dy, dz, k) -> {
            Block b = world.getBlockAt(s.x() + dx, s.floorY() + dy, s.z() + dz);
            Material want = material(k);
            Material is = b.getType();
            SelfTestPadShape.Verdict v = SelfTestPadShape.verdict(k, is.isAir(), is == want);
            if (v != SelfTestPadShape.Verdict.OK) b.setType(want, false);
            t[v.ordinal()]++;
        });
        String line = SelfTestPadShape.line(world.getName(), s, new SelfTestPadShape.Tally(t[1], t[2], t[0]));
        Block under = world.getBlockAt(s.x(), s.floorY(), s.z());
        if (!under.getType().isSolid()) {
            return new Result(false, line, "the block under the stand point is " + under.getType().getKey().getKey()
                    + ", not solid", null);
        }
        return new Result(true, line, null, standLocation(world, s));
    }

    private static CompletableFuture<Result> onLoaded(World world, Scheduler scheduler,
                                                      java.util.function.Function<SelfTestPadShape.Stand, Result> work) {
        SelfTestPadShape.Stand s = standOf(world);
        int[] c = SelfTestPadShape.chunks(s);
        List<CompletableFuture<?>> loads = new ArrayList<>();
        for (int cx = c[0]; cx <= c[2]; cx++) {
            for (int cz = c[1]; cz <= c[3]; cz++) loads.add(world.getChunkAtAsync(cx, cz, true));
        }
        CompletableFuture<Result> out = new CompletableFuture<>();
        CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).whenComplete((v, err) -> {
            if (err != null) {
                out.complete(new Result(false, null, "its chunks did not load (" + err + ")", null));
                return;
            }
            scheduler.onRegion(new Location(world, s.x(), s.floorY(), s.z()), () -> {
                try {
                    for (int cx = c[0]; cx <= c[2]; cx++) {
                        for (int cz = c[1]; cz <= c[3]; cz++) {
                            if (!world.isChunkLoaded(cx, cz)) {
                                out.complete(new Result(false, null, "chunk " + cx + "," + cz + " did not stay loaded", null));
                                return;
                            }
                        }
                    }
                    out.complete(work.apply(s));
                } catch (RuntimeException e) {
                    out.complete(new Result(false, null, e.toString(), null));
                }
            });
        });
        return out;
    }
}
