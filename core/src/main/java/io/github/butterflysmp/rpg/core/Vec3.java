package io.github.butterflysmp.rpg.core;

/** A position or direction. Deliberately not org.bukkit.Location. */
public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
    public Vec3 subtract(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 scale(double f) { return new Vec3(x * f, y * f, z * f); }

    public double distanceSquared(Vec3 o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double lengthSquared() { return x * x + y * y + z * z; }

    public double length() { return Math.sqrt(lengthSquared()); }

    public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }

    /** Unit vector in the same direction. ZERO normalises to ZERO rather than NaN. */
    public Vec3 normalize() {
        double len = length();
        return len == 0 ? ZERO : new Vec3(x / len, y / len, z / len);
    }

    /** The opposite direction. A reverse-facing dash is a facing, negated. */
    public Vec3 negate() { return new Vec3(-x, -y, -z); }

    /**
     * The vector perpendicular to both, right-handed: {@code this x o}.
     *
     * <p>Added for {@code SpreadPattern}, which needs a view-plane UP and derives it as
     * {@code right x forward}. <b>Not normalised, deliberately</b> -- the length carries
     * information the caller may need. For two unit vectors it is {@code sin(angle between)}, so a
     * caller can tell a well-conditioned basis from a degenerate one by reading the length, and
     * normalising here would throw that away.
     *
     * <p><b>PARALLEL INPUTS GIVE ZERO, WHICH IS CORRECT AND IS THE WHOLE HAZARD.</b> Crossing a
     * look vector with world up is the obvious way to build a basis and it collapses at pitch
     * +/-90, where the two are the same vector. {@code SpreadPattern} therefore takes its right
     * vector as a PARAMETER rather than deriving one here -- see its javadoc, which carries the
     * measurement.
     */
    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    /**
     * Rotate about the vertical (Y) axis by {@code degrees}, leaving Y untouched -- a
     * horizontal fan-out. The ember spread rotates the caster's facing by each of its
     * angles; a purely horizontal rotation keeps a flat facing flat.
     */
    public Vec3 rotateAboutY(double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r), sin = Math.sin(r);
        return new Vec3(x * cos + z * sin, y, -x * sin + z * cos);
    }
}
