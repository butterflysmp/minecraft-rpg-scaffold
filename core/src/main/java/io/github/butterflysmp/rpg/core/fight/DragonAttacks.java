package io.github.butterflysmp.rpg.core.fight;

import java.util.List;
import java.util.Optional;

/**
 * F9b, section 4: THE DRAGON'S DAMAGE, as content numbers (Ben, 2026-11-19: "Yes those numbers are fine to start
 * with"; the seat suggested bite 80, wing 40, breath 30 a second). Each is a mob's base hit: the fight scales it by gear
 * score as any mob's hit is scaled, and it goes through armour.
 *
 * <p><b>Which attack a hit was</b> is decided by the dragon's own boxes, the way the server chose whom to hit. READ,
 * the pinned server jar, {@code EnderDragon.aiStep}: each tick the dragon is not flinching, everything in either WING's
 * box inflated by (4, 2, 4) and moved down 2 is pushed and (not sitting) hurt for 5; then everything in the HEAD's and
 * the NECK's boxes inflated by 1 is hurt for 10. Both hurts are {@code mobAttack(dragon)}, so the event's cause cannot
 * tell them apart. A player inside both a bite box and a wing box is called a bite.
 *
 * @param bite            a hit from the head or the neck, before gear score and armour
 * @param wing            a hit from a wing
 * @param breathPerSecond the perched breath, per second of standing in it
 */
public record DragonAttacks(double bite, double wing, double breathPerSecond) {

    /** How far the server inflates the head's and the neck's boxes before hurting what is inside, blocks (READ). */
    static final double BITE_INFLATE = 1.0;
    /** And each wing's box: inflated by these, then moved down by {@link #WING_DROP} (READ). */
    static final double WING_INFLATE_XZ = 4.0;
    static final double WING_INFLATE_Y = 2.0;
    static final double WING_DROP = 2.0;
    private static final double TICKS_PER_SECOND = 20.0;

    public DragonAttacks {
        check("bite", bite);
        check("wing", wing);
        check("breath_per_second", breathPerSecond);
    }

    private static void check(String name, double v) {
        if (!(v >= 0.0 && Double.isFinite(v))) {
            throw new IllegalArgumentException("dragon " + name + " must be a finite number, 0 or more: " + v);
        }
    }

    /** The content number for this attack: the breath's is its figure a second. */
    public double base(Attack attack) {
        return switch (attack) {
            case BITE -> bite;
            case WING -> wing;
            case BREATH -> breathPerSecond;
        };
    }

    /**
     * One application of the breath, so that standing in it costs {@code perSecond} a second. READ: the breath cloud
     * hits each player once per its reapplication delay (the {@code AreaEffectCloud} default, 20 ticks; the sitting
     * phase does not change it). The size and life of the cloud are not touched.
     */
    public static double breathPerApplication(double perSecond, int intervalTicks) {
        if (intervalTicks <= 0) throw new IllegalArgumentException("a cloud's interval must be positive: " + intervalTicks);
        return perSecond * intervalTicks / TICKS_PER_SECOND;
    }

    /**
     * Which attack hit a player whose box is {@code victim}, from the dragon's part boxes as they are now; empty when
     * the player is in none of them.
     */
    public static Optional<Attack> classify(Box victim, Box head, Box neck, List<Box> wings) {
        if (victim.intersects(head.inflate(BITE_INFLATE, BITE_INFLATE, BITE_INFLATE))
                || victim.intersects(neck.inflate(BITE_INFLATE, BITE_INFLATE, BITE_INFLATE))) {
            return Optional.of(Attack.BITE);
        }
        for (Box wing : wings) {
            if (victim.intersects(wing.inflate(WING_INFLATE_XZ, WING_INFLATE_Y, WING_INFLATE_XZ).down(WING_DROP))) {
                return Optional.of(Attack.WING);
            }
        }
        return Optional.empty();
    }

    /** The dragon's three attacks, by the word the log uses. */
    public enum Attack {
        BITE("bite"),
        WING("wing"),
        BREATH("breath");

        private final String logName;

        Attack(String logName) { this.logName = logName; }

        public String logName() { return logName; }
    }

    /** An axis-aligned box, blocks. Overlap is strict, as the server's own {@code AABB.intersects} is. */
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        /** A box centred on (x, z), standing on {@code minY}: width across x and z, height up y. */
        public static Box centred(double x, double minY, double z, double width, double height) {
            double h = width / 2.0;
            return new Box(x - h, minY, z - h, x + h, minY + height, z + h);
        }

        public Box inflate(double x, double y, double z) {
            return new Box(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
        }

        public Box down(double y) {
            return new Box(minX, minY - y, minZ, maxX, maxY - y, maxZ);
        }

        public boolean intersects(Box o) {
            return minX < o.maxX && maxX > o.minX && minY < o.maxY && maxY > o.minY && minZ < o.maxZ && maxZ > o.minZ;
        }
    }
}
