package io.github.butterflysmp.rpg.paper.content;

import java.util.List;

/**
 * A named sequence of visual steps, played in order at an impact point.
 * Referenced from ability content by id: {@code visual_id: solar_detonation}.
 */
public record VisualDefinition(String id, List<VisualSpec> steps) {

    /** A compact constructor runs before the fields are assigned, so it can normalise them. */
    public VisualDefinition {
        steps = List.copyOf(steps);
    }
}
