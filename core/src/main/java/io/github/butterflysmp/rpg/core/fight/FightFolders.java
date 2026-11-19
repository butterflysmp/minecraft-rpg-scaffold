package io.github.butterflysmp.rpg.core.fight;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * WHEN A FIGHT WORLD'S FOLDER MAY BE DELETED (the F8 brief, section 4 f; the seat's ruling on the split, item 2). The
 * ONE judge for the startup orphan sweep and for every delete at a fight's end. A folder is deleted only if ALL FOUR
 * tests pass; any failure leaves it alone, and the caller logs why:
 * <ol>
 *   <li><b>{@link Verdict#NOT_IN_NAMESPACE}:</b> it does not sit directly in the fight's namespace folder
 *       ({@code <level>/dimensions/<namespace>/}, where build 74 keeps a world created with that namespace: READ,
 *       {@code DimensionType.getStorageFolder});</li>
 *   <li><b>{@link Verdict#NO_PREFIX}:</b> its name lacks the fight's prefix;</li>
 *   <li><b>{@link Verdict#NO_MARKER}:</b> it holds no marker FILE the fight wrote at creation;</li>
 *   <li><b>{@link Verdict#ESCAPES}:</b> its REAL path (symlinks resolved) is not strictly one level inside the
 *       namespace folder's real path, or lies under {@code dimensions/minecraft}.</li>
 * </ol>
 * Pure JDK ({@code java.nio}), so core's rule of no dependencies holds.
 */
public final class FightFolders {

    private FightFolders() {}

    /** The verdict: delete, or the first test that failed. */
    public enum Verdict { DELETE, NOT_IN_NAMESPACE, NO_PREFIX, NO_MARKER, ESCAPES }

    /** Judges {@code candidate} against the four tests, in order. */
    public static Verdict judge(Path namespaceDir, Path candidate, String prefix, String markerName) throws IOException {
        Path ns = namespaceDir.toAbsolutePath().normalize();
        Path c = candidate.toAbsolutePath().normalize();
        if (c.getParent() == null || !c.getParent().equals(ns)) return Verdict.NOT_IN_NAMESPACE;
        if (c.getFileName() == null || !c.getFileName().toString().startsWith(prefix)) return Verdict.NO_PREFIX;
        if (!Files.isRegularFile(c.resolve(markerName), LinkOption.NOFOLLOW_LINKS)) return Verdict.NO_MARKER;
        Path nsReal = ns.toRealPath();
        Path cReal = c.toRealPath();
        if (!strictlyInside(nsReal, cReal) || underVanilla(cReal) || underVanilla(nsReal)) return Verdict.ESCAPES;
        return Verdict.DELETE;
    }

    /** Is {@code candidate} exactly one level inside {@code namespace}, compared element by element? */
    public static boolean strictlyInside(Path namespace, Path candidate) {
        Path ns = namespace.normalize();
        Path c = candidate.normalize();
        return c.getParent() != null && c.getParent().equals(ns) && c.startsWith(ns) && !c.equals(ns);
    }

    /** Does the path pass through {@code dimensions/minecraft}, vanilla's own worlds? */
    public static boolean underVanilla(Path path) {
        Path p = path.normalize();
        for (int i = 0; i + 1 < p.getNameCount(); i++) {
            if (p.getName(i).toString().equals("dimensions") && p.getName(i + 1).toString().equals("minecraft")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the folder and everything in it, after {@link #judge} says {@link Verdict#DELETE}; refuses otherwise by
     * throwing {@link IllegalStateException} naming the verdict, having touched nothing. Never follows a link: a link
     * inside is removed as a link. Returns how many entries were removed, the folder itself excluded.
     */
    public static long deleteTree(Path namespaceDir, Path candidate, String prefix, String markerName)
            throws IOException {
        Verdict v = judge(namespaceDir, candidate, prefix, markerName);
        if (v != Verdict.DELETE) throw new IllegalStateException("refused to delete " + candidate + ": " + v);
        Path top = candidate.toAbsolutePath().normalize();
        long[] removed = {0};
        Files.walkFileTree(top, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                removed[0]++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException failed) throws IOException {
                if (failed != null) throw failed;
                Files.delete(dir);
                if (!dir.equals(top)) removed[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        return removed[0];
    }
}
