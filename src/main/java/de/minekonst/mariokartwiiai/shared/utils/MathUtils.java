package de.minekonst.mariokartwiiai.shared.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

public final class MathUtils {

    private static final Pattern VECTOR_PATTERN = Pattern.compile("\\{?\\s*(?<X1>\\-?\\d+)[.,]?(?<X2>\\d*)\\s*\\;\\s*"
            + "(?<Y1>\\-?\\d+)[.,]?(?<Y2>\\d*)\\s*\\;\\s*"
            + "(?<Z1>\\-?\\d+)[.,]?(?<Z2>\\d*)\\s*\\}?");

    //<editor-fold defaultstate="collapsed" desc="Min and Max">
    /**
     * Calculate min value
     *
     * @param values Array of values
     *
     * @return Minimum values
     */
    public static double min(Number... values) {
        return min(Arrays.asList(values));
    }

    /**
     * Calculate the min value
     *
     * @param values Collection of Numbers (e.g. a list)
     *
     * @return The minimum value
     */
    public static double min(Collection<? extends Number> values) {
        double min = Double.MAX_VALUE;
        for (Number v : values) {
            min = java.lang.Math.min(v.doubleValue(), min);
        }
        return min;
    }

    /**
     * Calculate max value
     *
     * @param values Array of values
     *
     * @return Maximum values
     */
    public static double max(Number... values) {
        return max(Arrays.asList(values));
    }

    /**
     * Calculate the max value
     *
     * @param values Collection of Numbers (e.g. a list)
     *
     * @return The maximum value
     */
    public static double max(Collection<? extends Number> values) {
        double max = Double.MIN_VALUE;
        for (Number v : values) {
            max = java.lang.Math.max(v.doubleValue(), max);
        }
        return max;
    }
    //</editor-fold>

    public static long ncr(long n, long k) {
        return fac(n) / (fac(n - k) * fac(k));
    }

    public static long fac(long f) {
        long v = 1;
        for (int x = 1; x <= f; x++) {
            v *= x;
        }

        return v;
    }

    /**
     * Get a random number between min and max (including min and max)
     *
     * @param min The min value
     * @param max The max value
     *
     * @return A random value bewteen min and max
     */
    public static double random(double min, double max) {
        return min + (Math.random() * ((max - min) + 1));
    }

    /**
     * Check if the value is greather or equal to min and less or equal to max
     *
     * @param value The value to check
     * @param min   The min value
     * @param max   The max value
     *
     * @return the value if min...value...max, min / max otherwise
     */
    public static double clamp(double value, double min, double max) {
        if (value >= max) {
            return max;
        }

        return value > min ? value : min;
    }

    public static Vector2D fromAngle(double degree) {
        return new Vector2D(FastMath.sin(Math.toRadians(degree)), FastMath.cos(Math.toRadians(degree)));
    }

    public static Vector3D parseVector3(String string) {
        if (string == null) {
            throw new IllegalArgumentException("String can no be null");
        }

        Matcher m = VECTOR_PATTERN.matcher(string);
        if (!m.matches()) {
            throw new IllegalArgumentException("String does not follow the vector format convention");
        }

        try {
            double x = Double.parseDouble(m.group("X1") + (m.group("X2") != null ? "." + m.group("X2") : ""));
            double y = Double.parseDouble(m.group("Y1") + (m.group("Y2") != null ? "." + m.group("Y2") : ""));
            double z = Double.parseDouble(m.group("Z1") + (m.group("Z2") != null ? "." + m.group("Z2") : ""));

            return new Vector3D(x, y, z);
        }
        catch (Exception ex) {
            throw new IllegalStateException();
        }
    }

    public static Vector2D rotate(Vector2D v, double degree) {
        if (degree == 0) {
            return v;
        }

        if (degree > 360) {
            degree %= 360.0;
        }

        if (degree < 0) {
            degree += 360;
        }

        double angle = Math.toRadians(360.0 - degree);

        double nx = v.getX() * FastMath.cos(angle) - v.getY() * FastMath.sin(angle);
        double ny = v.getX() * FastMath.sin(angle) + v.getY() * FastMath.cos(angle);

        return new Vector2D(nx, ny);
    }

    private MathUtils() {
    }
}
