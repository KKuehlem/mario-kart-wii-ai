package de.minekonst.mariokartwiiai.shared.utils.editortable;


import java.io.Serializable;

@FunctionalInterface
public interface Validator<T> extends Serializable {

    /**
     * Check if the value is valid
     *
     * @param value The value to check
     *
     * @return null, if valid. Otherwise the error message
     */
    public String check(T value);

    public static final Validator<Number> NON_NEG = greaterOrEqual(0);

    public static Validator<Number> greaterOrEqual(double value) {
        return (Number n) -> {
            return n.doubleValue() >= value ? null : "Value must be >= " + value;
        };
    }

    public static Validator<Number> lessOrEqual(double value) {
        return (Number n) -> {
            return n.doubleValue() <= value ? null : "Value must be <= " + value;
        };
    }

    public static Validator<Number> greaterThan(double value) {
        return (Number n) -> {
            return n.doubleValue() > value ? null : "Value must be > " + value;
        };
    }

    public static Validator<Number> lessThan(double value) {
        return (Number n) -> {
            return n.doubleValue() < value ? null : "Value must be < " + value;
        };
    }
}
