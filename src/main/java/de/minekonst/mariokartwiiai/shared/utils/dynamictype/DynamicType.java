package de.minekonst.mariokartwiiai.shared.utils.dynamictype;

@Deprecated
public final class DynamicType<T> {

    private final Class<?> clazz;
    private final T value;

    /**
     * New dynamic type
     *
     * @param clazz The class of the type
     * @param value The value
     */
    public DynamicType(Class<T> clazz, T value) {
        this.clazz = clazz;
        this.value = value;
    }

    public Class<?> getType() {
        return clazz;
    }

    public T getValue() {
        return value;
    }

}
