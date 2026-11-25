package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Locale;
import java.util.UUID;

/**
 * THE SUMMON TRACE LINES (the seat, 2026-11-24): spawn, target, hit, teleport, removed, and D1(a)'s goals restored. Each
 * carries its owner, its weapon id, its body's UUID and its position. {@code hit} carries EVERY damage term: the authored
 * damage, the score it was cast at, what was sent, and the terms D3 rules out (no crit, class or level bonus), written as
 * their neutral values so the line says they were absent rather than forgotten. In 3b only spawn, removed and goals
 * restored are emitted; target, hit and teleport are PR 4's.
 */
public final class SummonTrace {

    private SummonTrace() {}

    /** O2 (the seat, 2026-11-24): a body that is already gone has no position: {@code at=unknown}, never 0 0 0. */
    private static String where(SummonStore.Summon s, Vec3 at) {
        String head = "owner=" + s.owner() + " weapon=" + s.weaponId() + " body=" + s.body();
        return at == null ? head + " at=unknown"
                : head + String.format(Locale.ROOT, " at=%.2f %.2f %.2f", at.x(), at.y(), at.z());
    }

    public static String spawn(SummonStore.Summon s, Vec3 at) {
        return String.format(Locale.ROOT, "SUMMON spawn %s summon=%s score=%d health=%.3f damage=%.3f", where(s, at),
                s.summonId(), s.score(), s.health(), s.damage());
    }

    /** A change of target: the new one (or none), the TIER that chose it (F3), and why the old one was dropped. */
    public static String target(SummonStore.Summon s, Vec3 at, java.util.Optional<UUID> target, SummonGuard.Why why,
                                java.util.Optional<SummonGuard.Dropped> dropped) {
        return "SUMMON target " + where(s, at) + " target=" + target.map(UUID::toString).orElse("none")
                + " why=" + why.name().toLowerCase(Locale.ROOT)
                + dropped.map(d -> " dropped=" + d.name().toLowerCase(Locale.ROOT)).orElse("");
    }

    public static String hit(SummonStore.Summon s, Vec3 at, UUID victim, double authored, String element, long tick) {
        return String.format(Locale.ROOT, "SUMMON hit %s victim=%s authored=%.3f score=%d sent=%.3f crit=none class=0"
                + " level=0 element=%s tick=%d", where(s, at), victim, authored, s.score(), s.damage(), element, tick);   // X2: the server tick
    }

    public static String teleport(SummonStore.Summon s, Vec3 to, Vec3 from) {
        return String.format(Locale.ROOT, "SUMMON teleport %s from=%.2f %.2f %.2f", where(s, to), from.x(), from.y(),
                from.z());
    }

    public static String removed(SummonStore.Summon s, Vec3 at, SummonRemoval cause) {
        return "SUMMON removed " + where(s, at) + " cause=" + cause.token();
    }

    public static String goalsRestored(SummonStore.Summon s, Vec3 at, int foreign) {
        return "SUMMON goals restored " + where(s, at) + " foreign=" + foreign;
    }
}
