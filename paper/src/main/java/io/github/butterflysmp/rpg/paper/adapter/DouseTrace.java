package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.DouseLine;

/**
 * Where a {@link DouseLine} goes (the seat's Y7 and E1, 2026-11-27): the log while {@code /rpg mobtrace} is on, with
 * the server's tick, as {@link DotTrace} carries {@code DOTTICK}. {@code AdapterContext.douseTrace} builds the real one.
 */
@FunctionalInterface
public interface DouseTrace {

    void line(DouseLine line);

    DouseTrace NONE = line -> { };
}
