package io.github.yourname.rpg.core.ability;

public record ResourceCost(String resourceId, double amount) {
    public static final ResourceCost FREE = new ResourceCost("none", 0);
}
