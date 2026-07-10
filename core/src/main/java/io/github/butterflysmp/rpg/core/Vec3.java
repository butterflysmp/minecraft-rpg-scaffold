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
}
