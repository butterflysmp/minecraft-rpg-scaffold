package io.github.butterflysmp.rpg.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Every saved loadout of one player: ONE PER (class, element) CELL (PLAN-build-system.md section 2.1), in
 * its own file, {@code builds/<uuid>.json}. Switching Fire Ranger -> Fire Mage -> Fire Ranger restores the
 * Ranger build, because nothing is overwritten by a cell change.
 *
 * <p>A LIST of entries rather than a map keyed by a string, because the key is two fields: a
 * {@code "ranger_fire"} key would need splitting back into its halves, the collision {@code CellKey}
 * exists to avoid. Two entries for one cell is a STRUCTURAL fault -- refusing the file beats silently
 * discarding one of them, the rule {@code PlayerAccessories} set for two entries in one slot.
 *
 * <p>Its own store, not a profile field: a bad build write then costs the build file, never the profile
 * (class, XP, Nexus slot). The accessories trade, section 3.9 of PLAN-accessories.md.
 */
public record PlayerBuild(int schemaVersion, UUID playerId, List<CellLoadout> cells) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PlayerBuild {
        Objects.requireNonNull(playerId, "playerId");
        List<CellLoadout> source = cells == null ? List.of() : cells;
        for (int i = 0; i < source.size(); i++) {
            CellLoadout cell = Objects.requireNonNull(source.get(i), "cell loadout");
            for (int j = 0; j < i; j++) {
                if (source.get(j).isFor(cell.classId(), cell.elementId())) {
                    throw new IllegalArgumentException("build for " + playerId + " has two loadouts for "
                            + cell.classId() + "/" + cell.elementId() + "; refusing to load rather than"
                            + " silently discarding one");
                }
            }
        }
        cells = List.copyOf(source);
    }

    public static PlayerBuild empty(UUID playerId) {
        return new PlayerBuild(CURRENT_SCHEMA_VERSION, playerId, List.of());
    }

    /** The saved loadout for one cell, if this player ever saved one. */
    public Optional<CellLoadout> loadout(String classId, String elementId) {
        for (CellLoadout cell : cells) {
            if (cell.isFor(classId, elementId)) return Optional.of(cell);
        }
        return Optional.empty();
    }

    /** This build with {@code loadout} saved for its cell, replacing any earlier one for that cell only. */
    public PlayerBuild with(CellLoadout loadout) {
        List<CellLoadout> next = new ArrayList<>();
        for (CellLoadout cell : cells) {
            if (!cell.isFor(loadout.classId(), loadout.elementId())) next.add(cell);
        }
        next.add(loadout);
        return new PlayerBuild(schemaVersion, playerId, next);
    }

    public PlayerBuild withSchemaVersion(int version) {
        return new PlayerBuild(version, playerId, cells);
    }
}
