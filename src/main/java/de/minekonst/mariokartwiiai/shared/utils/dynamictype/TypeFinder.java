package de.minekonst.mariokartwiiai.shared.utils.dynamictype;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

@Deprecated
public final class TypeFinder {

    public static final Pattern NUMBER = Pattern.compile("\\s*(?<Number>\\-?\\d+)\\s*");
    public static final Pattern FLOATING_POINT = Pattern.compile("\\s*(?<Number1>\\-?\\d*)([,.](?<Number2>\\d*)\\s*)?");

    private static final Map<Class<?>, Function<String, ?>> functions = new HashMap<>(10);

    static {
        functions.put(Integer.class, TypeFinder::toInt);//Integer::valueOf);
        functions.put(Double.class, TypeFinder::toDouble);
        functions.put(Boolean.class, TypeFinder::toBoolean);
        functions.put(String.class, String::toString);
    }

    private TypeFinder() {
    }

    public static <T> void registerType(Class<T> type, Function<String, T> converter) {
        functions.put(type, converter);
    }

    /**
     * Check if this class is supported to be parsed
     *
     * @param type The class to check
     *
     * @return True, if supported
     */
    public static boolean supports(Class<?> type) {
        return functions.containsKey(type);
    }

    /**
     * Find type of a string
     *
     * @param string The string to check
     *
     * @return The best matching type (using all types defined in
     *         SUPPORTED_TYPES). The result is the original string, if no better
     *         type could be found
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DynamicType<?> findType(String string) {
        for (Class<?> clazz : functions.keySet()) {
            try {
                Object parse = parse(string, clazz);

                if (parse != null) {
                    return new DynamicType(clazz, parse);
                }
            }
            catch (Exception ex) {

            }
        }

        return new DynamicType<>(String.class, string);
    }

    /**
     * Parse a string to a type (Must be supported or enum)
     *
     * @param <T>    The type to parse to
     * @param string The String to parse
     * @param clazz  Class of the type to parse to
     *
     * @return Null, if the string is not of type T
     *
     * @throws UnsupportedOperationException if class is not listed in
     *                                       TypeFinder.SUPPORTED_CLASSES and is
     *                                       not an enum
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T parse(String string, Class<T> clazz) {
        if (clazz.isEnum()) {
            return (T) TypeUtils.findEnumValue(string, (Class<? extends Enum>) clazz);
        }

        Function<String, ?> f = functions.get(clazz);
        if (f == null) {
            throw new UnsupportedOperationException("Unsupported Type: " + clazz.getName());
        }

        try {
            return (T) f.apply(string);
        }
        catch (Exception ex) {
            return null;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Converter">
    public static boolean toBoolean(String str) {
        if (str.equalsIgnoreCase("true")) {
            return true;
        }
        else if (str.equalsIgnoreCase("false")) {
            return false;
        }
        else {
            throw new IllegalArgumentException("\"" + str + "\" can not be converted to boolean");
        }
    }

    public static double toDouble(String str) {
        return Double.parseDouble(str.replace(',', '.'));
    }

    public static int toInt(String str) {
        return Integer.parseInt(str.replaceAll("\\.", ""));
    }

    public static boolean isNumber(String str) {
        return NUMBER.matcher(str).matches();
    }

    public static boolean isFloatingPoint(String str) {
        return FLOATING_POINT.matcher(str).matches();
    }
    //</editor-fold>
}
