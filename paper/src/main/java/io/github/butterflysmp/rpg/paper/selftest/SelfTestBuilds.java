package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.storage.CellLoadout;
import io.github.butterflysmp.rpg.storage.PlayerBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * HIS STORED BUILDS, CAPTURED AND PUT BACK (the seat's D2, 2026-11-27, on H15's pattern for his accessory slots): a gate
 * asks nothing of his state, and a row that picks an ability in the Build screen SAVES it into his build. So the run's
 * snapshot reads his whole build before any row runs, and at the run's end and on an abort writes it back whole and reads
 * it back. Pure: the store is behind a {@link Port}, which {@code SelfTestSnapshot} answers with {@code BuildService}.
 *
 * <p>Whole means the RECORD: every cell he has a loadout for, in its stored order, every slot as stored (an unreadable or
 * unoffered id included, a null slot as null). A cell with no loadout before the run has none after. When the store
 * already reads what was captured, nothing is written (so a run that changed nothing does not create a file).
 */
final class SelfTestBuilds {

    /** The store: read his build, or empty when it is not loaded; write a whole record back, false when refused. */
    interface Port {
        Optional<PlayerBuild> read();

        boolean write(PlayerBuild whole);
    }

    private final PlayerBuild captured;   // null: not captured (his build was not loaded)

    private SelfTestBuilds(PlayerBuild captured) {
        this.captured = captured;
    }

    static SelfTestBuilds capture(Port port) {
        return new SelfTestBuilds(port.read().orElse(null));
    }

    /**
     * The CAPTURED and RESTORED field's value: {@code [<class>/<element>:Q=<id>,L=<id>,R=<id>,asp=<a>|<b>,frag=<a>|<b>|<c>|<d>;
     * ...]} in stored order, {@code -} for an empty slot, {@code []} for none; {@code not-captured} when it was not read.
     */
    String fields() {
        return captured == null ? "not-captured" : summary(captured);
    }

    static String summary(PlayerBuild build) {
        List<String> cells = new ArrayList<>();
        for (CellLoadout c : build.cells()) {
            cells.add(c.classId() + "/" + c.elementId() + ":Q=" + slot(c.ultimate()) + ",L=" + slot(c.actives().get(0))
                    + ",R=" + slot(c.actives().get(1)) + ",asp=" + joined(c.aspects()) + ",frag=" + joined(c.fragments()));
        }
        return "[" + String.join(";", cells) + "]";
    }

    private static String slot(String id) {
        return id == null ? "-" : id;
    }

    private static String joined(List<String> ids) {
        List<String> out = new ArrayList<>();
        for (String id : ids) out.add(slot(id));
        return String.join("|", out);
    }

    /**
     * Put it back, then read it back. Empty when it reads as captured; otherwise the NOT-RESTORED value:
     * {@code builds(not-captured)}, {@code builds(store-refused)}, {@code builds(unreadable-after)}, or
     * {@code builds(cells <class>/<element>,...)} naming every cell that differs, is missing or is extra.
     */
    Optional<String> restore(Port port) {
        if (captured == null) return Optional.of("builds(not-captured)");
        Optional<PlayerBuild> before = port.read();
        if (before.isEmpty() || !Objects.equals(before.get().cells(), captured.cells())) {
            if (!port.write(captured)) return Optional.of("builds(store-refused)");
        }
        Optional<PlayerBuild> now = port.read();
        if (now.isEmpty()) return Optional.of("builds(unreadable-after)");
        List<String> differ = differing(captured.cells(), now.get().cells());
        return differ.isEmpty() ? Optional.empty() : Optional.of("builds(cells " + String.join(",", differ) + ")");
    }

    /** Every cell key whose loadout differs, is missing or is extra, in want's order then got's; order counts too. */
    static List<String> differing(List<CellLoadout> want, List<CellLoadout> got) {
        List<String> out = new ArrayList<>();
        int n = Math.max(want.size(), got.size());
        for (int i = 0; i < n; i++) {
            CellLoadout w = i < want.size() ? want.get(i) : null;
            CellLoadout g = i < got.size() ? got.get(i) : null;
            if (Objects.equals(w, g)) continue;
            String key = (w != null ? w : g).classId() + "/" + (w != null ? w : g).elementId();
            if (!out.contains(key)) out.add(key);
        }
        return out;
    }
}
