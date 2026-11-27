package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobHostility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * THE BLINK'S THREE PURE DECISIONS (the seat's K2, K3 and K6, 2026-11-26): when it fires, where it lands, and what it does
 * to his invisibility. No Bukkit: the paper side ({@code BlinkTomes}) reads the world on the wearer's own thread and hands
 * the readings here. {@code BlinksTest} pins each as a table.
 *
 * <h2>K2, the trigger</h2>
 *
 * A mob fires it when it is of the sides table's MOB side (never a player, Ben: "Other players don't count until we do a
 * pvp pass"; never a summon), its current target is the wearer, it is alive, in his world, and its feet are within
 * {@code trigger_radius} of his feet in three dimensions (inclusive, CC'S CALL). The wearer must be alive, not in creative
 * or spectator, not already blinked, off cooldown, and not inside the 20-tick wait after a search that found nowhere safe.
 * The nearest such mob is the trigger (the first found on a tie).
 *
 * <h2>K3, the destination: a bounded search</h2>
 *
 * Every integer column (dx, dz) with dx² + dz² ≤ ceil(range)² round his BLOCK, and in each the seven heights from three
 * below his feet's block to three above ({@link #VERTICAL_REACH}, CC'S CALL): 797 columns and 5,579 spots at range 16 (EXECUTED, BlinksTest). A
 * spot is SAFE when its chunk is loaded (an unloaded chunk is never read, so never loaded), its column is inside the world
 * border, the block under it is a SAFE floor, its two body blocks are CLEAR, its feet (the block's centre, on the floor)
 * are within {@code range} of his feet, and no hostile mob's feet are within {@code clear_radius} of its feet (inclusive).
 * The paper side classifies each block ({@link Floor}, {@link Body}); the harmful list is READ from the 26.1.2 jar and
 * lives there. The choice is uniform over the safe spots, by the random source passed in ({@link #choose}).
 *
 * <h2>K6, the invisibility: remove only what the blink applied</h2>
 *
 * {@link #invisibilityAtBlink} and {@link #invisibilityAtEnd}.
 */
public final class Blinks {

    private Blinks() {}

    /** K3 (CC'S CALL): how many whole blocks above and below his feet's block a landing may stand. */
    public static final int VERTICAL_REACH = 3;

    /** K2 (CC'S CALL): the trigger check's period, in server ticks. The brief's bound is 10. */
    public static final int CHECK_PERIOD_TICKS = 5;

    /** K3: after a search that found no safe spot, the next search is at least this many ticks later. */
    public static final int NONE_SAFE_RETRY_TICKS = 20;

    // --- K2: THE TRIGGER ----------------------------------------------------------------------------------------------

    /**
     * The wearer at a check: his world and feet, alive, in creative or spectator ({@code exemptMode}), already blinked,
     * the cooldown's remaining ticks, and whether a search that found nowhere safe still holds him.
     */
    public record Wearer(String world, Vec3 feet, boolean alive, boolean exemptMode, boolean blinked, long cooldownRemaining,
                         boolean retryHeld) {}

    /** One living entity near him, as the trigger sees it: its side, whether its current target is HIM, where it is. */
    public record Near(UUID id, String type, MobHostility.Side side, boolean targetsHim, String world, Vec3 feet,
                       boolean alive) {}

    /** The mob that fired it and how far it was, feet to feet. */
    public record Trigger(Near mob, double distance) {}

    /** Why the wearer's own state holds the blink, whatever is near him. NONE: it does not. */
    public enum Held { NONE, DEAD, EXEMPT, BLINKED, COOLDOWN, RETRY }

    public static Held held(Wearer w) {
        if (!w.alive()) return Held.DEAD;
        if (w.exemptMode()) return Held.EXEMPT;
        if (w.blinked()) return Held.BLINKED;
        if (w.cooldownRemaining() > 0) return Held.COOLDOWN;
        if (w.retryHeld()) return Held.RETRY;
        return Held.NONE;
    }

    /** Does this entity, on its own, fire the blink for a wearer who is not held? */
    public static boolean triggers(AccessoryBehaviour.Blink blink, Wearer w, Near n) {
        return n.side() == MobHostility.Side.MOB
                && n.targetsHim()
                && n.alive()
                && n.world() != null && n.world().equals(w.world())
                && n.feet().distanceSquared(w.feet()) <= blink.triggerRadius() * blink.triggerRadius();
    }

    /** The trigger, if the wearer is not held and any entity fires it: the nearest, the first found on a tie. */
    public static Optional<Trigger> trigger(AccessoryBehaviour.Blink blink, Wearer w, List<Near> nearby) {
        if (held(w) != Held.NONE) return Optional.empty();
        Near best = null;
        double bestSq = Double.MAX_VALUE;
        for (Near n : nearby) {
            if (!triggers(blink, w, n)) continue;
            double d = n.feet().distanceSquared(w.feet());
            if (d < bestSq) {
                best = n;
                bestSq = d;
            }
        }
        return best == null ? Optional.empty() : Optional.of(new Trigger(best, Math.sqrt(bestSq)));
    }

    // --- K3: THE DESTINATION ------------------------------------------------------------------------------------------

    /** The block under a spot. SAFE: solid and not harmful to stand on. */
    public enum Floor { SAFE, HARMFUL, NOT_SOLID }

    /** A block his body would occupy. CLEAR: no collision, no fluid, nothing that burns, hurts or carries him off. */
    public enum Body { CLEAR, SOLID, FLUID, HARMFUL }

    /** The world as the search reads it. {@code loaded} and {@code insideBorder} are asked first; a block only after. */
    public interface Ground {
        boolean loaded(int x, int z);

        boolean insideBorder(int x, int z);

        Floor floor(int x, int y, int z);

        Body body(int x, int y, int z);
    }

    /** A landing: the block his feet stand in. */
    public record Spot(int x, int y, int z) {
        /** His feet there: the block's centre, on its floor. */
        public Vec3 feet() {
            return new Vec3(x + 0.5, y, z + 0.5);
        }
    }

    /** What one search did: the columns it looked at, the spots it tried, and the safe ones, in listing order. */
    public record Search(int columns, int tried, List<Spot> safe) {
        public Search {
            safe = List.copyOf(safe);
        }
    }

    /**
     * THE SEARCH. Listed x, then z, then y ascending, so a seeded choice is a fixed spot. {@code hostiles} are the feet of
     * every hostile mob the paper side found near him (the trigger among them).
     */
    public static Search search(AccessoryBehaviour.Blink blink, Vec3 from, Ground ground, List<Vec3> hostiles) {
        int bx = (int) Math.floor(from.x());
        int by = (int) Math.floor(from.y());
        int bz = (int) Math.floor(from.z());
        int r = (int) Math.ceil(blink.range());
        double rangeSq = blink.range() * blink.range();
        double clearSq = blink.clearRadius() * blink.clearRadius();
        int columns = 0;
        int tried = 0;
        List<Spot> safe = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                columns++;
                int x = bx + dx;
                int z = bz + dz;
                if (!ground.loaded(x, z) || !ground.insideBorder(x, z)) continue;
                for (int dy = -VERTICAL_REACH; dy <= VERTICAL_REACH; dy++) {
                    tried++;
                    Spot spot = new Spot(x, by + dy, z);
                    Vec3 feet = spot.feet();
                    if (feet.distanceSquared(from) > rangeSq) continue;
                    if (ground.floor(x, spot.y() - 1, z) != Floor.SAFE) continue;
                    if (ground.body(x, spot.y(), z) != Body.CLEAR) continue;
                    if (ground.body(x, spot.y() + 1, z) != Body.CLEAR) continue;
                    boolean crowded = false;
                    for (Vec3 h : hostiles) {
                        if (h.distanceSquared(feet) <= clearSq) {
                            crowded = true;
                            break;
                        }
                    }
                    if (!crowded) safe.add(spot);
                }
            }
        }
        return new Search(columns, tried, safe);
    }

    /** Uniform over the safe spots, by the source passed in. Empty when there are none. */
    public static Optional<Spot> choose(List<Spot> safe, RandomGenerator random) {
        if (safe.isEmpty()) return Optional.empty();
        return Optional.of(safe.get(random.nextInt(safe.size())));
    }

    // --- K4/K5: THE STATE'S END ---------------------------------------------------------------------------------------

    /** Why a blinked state ended (K4, K5). CLEARED is the selftest's clear_state and row-start clean (K9). */
    public enum EndReason {
        EXPIRED, CAST, MELEE, DEATH, QUIT, CLEARED;

        public String token() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    // --- K6: THE INVISIBILITY -----------------------------------------------------------------------------------------

    /** An invisibility effect as the decision sees it. {@link #INFINITE} is vanilla's never-ending duration. */
    public record Effect(int durationTicks, int amplifier, boolean ambient, boolean particles, boolean icon) {
        public static final int INFINITE = -1;

        public boolean infinite() {
            return durationTicks == INFINITE;
        }
    }

    /** The blink's own effect: invisibility_ticks, amplifier 0, not ambient, NO particles, its icon shown (CC'S CALL). */
    public static Effect ours(AccessoryBehaviour.Blink blink) {
        return new Effect(blink.invisibilityTicks(), 0, false, false, true);
    }

    /** At the blink: whether to apply ours, and his own shorter one to give back at the end. */
    public record InvisibilityAtBlink(boolean apply, Optional<Effect> prior) {}

    /**
     * At the blink. He already has one lasting the whole state (or forever): it is NEVER touched, and ours is not
     * applied. Otherwise ours replaces his shorter one (remembered, given back at the end): the paper side removes his and
     * adds ours, so vanilla's merging of two instances never decides what the end removes.
     */
    public static InvisibilityAtBlink invisibilityAtBlink(AccessoryBehaviour.Blink blink, Optional<Effect> current) {
        if (current.isPresent()
                && (current.get().infinite() || current.get().durationTicks() >= blink.invisibilityTicks())) {
            return new InvisibilityAtBlink(false, current);
        }
        return new InvisibilityAtBlink(true, current);
    }

    /** What to do at the end. */
    public enum EndAction { NOTHING, REMOVE, REMOVE_AND_RESTORE }

    public record InvisibilityAtEnd(EndAction action, Optional<Effect> restore) {}

    /**
     * At the end, {@code elapsed} ticks after the blink, with {@code now} his invisibility at that moment. Remove it only
     * if it is still OURS (amplifier 0, not infinite, no longer than ours had left); then give back what remains of his
     * shorter one. Anything else (a longer or stronger one given meanwhile, milk having taken it off) is left alone.
     */
    public static InvisibilityAtEnd invisibilityAtEnd(AccessoryBehaviour.Blink blink, InvisibilityAtBlink start, int elapsed,
                                                      Optional<Effect> now) {
        if (!start.apply() || now.isEmpty()) return new InvisibilityAtEnd(EndAction.NOTHING, Optional.empty());
        Effect e = now.get();
        int oursLeft = Math.max(0, blink.invisibilityTicks() - elapsed);
        boolean isOurs = !e.infinite() && e.amplifier() == 0 && e.durationTicks() <= oursLeft;
        if (!isOurs) return new InvisibilityAtEnd(EndAction.NOTHING, Optional.empty());
        if (start.prior().isPresent()) {
            Effect p = start.prior().get();
            int left = p.durationTicks() - elapsed;
            if (left > 0) {
                return new InvisibilityAtEnd(EndAction.REMOVE_AND_RESTORE,
                        Optional.of(new Effect(left, p.amplifier(), p.ambient(), p.particles(), p.icon())));
            }
        }
        return new InvisibilityAtEnd(EndAction.REMOVE, Optional.empty());
    }
}
