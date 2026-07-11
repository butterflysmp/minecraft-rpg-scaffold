package io.github.butterflysmp.rpg.paper.content;

/**
 * One element: pure identity. An id and a display name, and nothing else -- an element
 * carries no logic now (the damage multiplier was deleted in Phase 2A). It is a tag a
 * weapon or ability wears, and what Phase 3 will gate a kit on.
 */
public record ElementDefinition(String id, String displayName) {}
