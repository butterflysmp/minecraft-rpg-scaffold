package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.mob.MobBlink;

import java.util.Optional;

/**
 * The BLINK step's grammar (dragon-mobs step R7b): {@code "<tag> <angle degrees> <distance blocks>"}. The tagged
 * Phalanx blinks on its next tick, frozen or not, and EVERY one of its attempts is this draw, so a row predicts exact
 * coordinates, or exactly the refusal it stages.
 */
final class SelfTestBlink {

    private SelfTestBlink() {}

    record Forced(String tag, MobBlink.Draw draw) {}

    static Optional<Forced> parse(String arg) {
        String[] w = arg.trim().split("\\s+");
        if (w.length != 3) return Optional.empty();
        try {
            double angle = Double.parseDouble(w[1]);
            double distance = Double.parseDouble(w[2]);
            if (!(angle >= 0 && angle < 360) || !(distance >= 0 && Double.isFinite(distance))) return Optional.empty();
            return Optional.of(new Forced(w[0], new MobBlink.Draw(angle, distance)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
