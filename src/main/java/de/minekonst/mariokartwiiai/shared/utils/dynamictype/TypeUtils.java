package de.minekonst.mariokartwiiai.shared.utils.dynamictype;

import java.util.Objects;

@Deprecated
public final class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Get enum value by string egnoring cases
     *
     * @param <T>   The Enumtype, so you get same type returned as given
     * @param text  The text to check
     * @param clazz The clazz of T
     *
     * @return The Value or null if there is no such enum value
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T findEnumValue(String text, Class<T> clazz) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(clazz);

        for (Enum<? super T> s : clazz.getEnumConstants()) {
            if (s.toString().equalsIgnoreCase(text)) {
                return (T) s;
            }
        }

        return null;
    }
}
