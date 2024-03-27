package de.minekonst.mariokartwiiai.shared.utils.editortable;

import de.minekonst.mariokartwiiai.shared.utils.dynamictype.TypeFinder;
import de.minekonst.mariokartwiiai.shared.utils.dynamictype.TypeUtils;
import java.awt.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.JOptionPane;

public class EditorValue<T extends Serializable> extends EditorBase {

    private static final long serialVersionUID = 1L;

    private volatile T value;
    private final Class<T> type;
    private final String description;

    private final T min;
    private final T max;

    private final List<Validator<? super T>> validators;
    
    private volatile transient boolean dialogOpen;

    /**
     * Create a new EdítorValue to use in a {@link EditorTable} or
     * {@link EditorValueCollection}
     *
     * @param name  The name of this value
     * @param value The default value of this EditorValue
     * @param type  The class of the value. Must be supported by
     *              {@link TypeFinder}
     *
     * @throws IllegalArgumentException if type is not supported by
     *                                  {@link TypeFinder}
     */
    public EditorValue(String name, T value, Class<T> type) {
        this(name, value, type, "");
    }

    /**
     * Create a new EdítorValue to use in a {@link EditorTable} or
     * {@link EditorValueCollection}
     *
     * @param name       The name of this value
     * @param value      The default value of this EditorValue
     * @param type       The class of the value. Must be supported by
     *                   {@link TypeFinder}
     * @param validators The validators to check new values with
     *
     * @throws IllegalArgumentException if type is not supported by
     *                                  {@link TypeFinder}
     */
    @SafeVarargs
    public EditorValue(String name, T value, Class<T> type, Validator<? super T>... validators) {
        this(name, value, type, "", validators);
    }

    /**
     * Create a new EdítorValue to use in a {@link EditorTable} or
     * {@link EditorValueCollection}
     *
     * @param name        The name of this value
     * @param value       The default value of this EditorValue
     * @param type        The class of the value. Must be supported by
     *                    {@link TypeFinder}
     * @param description The description for this value
     *
     * @throws IllegalArgumentException if type is not supported by
     *                                  {@link TypeFinder}
     */
    public EditorValue(String name, T value, Class<T> type, String description) {
        this(name, value, type, description, null, null, (Validator<? super T>[]) null);
    }

    /**
     * Create a new EdítorValue to use in a {@link EditorTable} or
     * {@link EditorValueCollection}
     *
     * @param name        The name of this value
     * @param value       The default value of this EditorValue
     * @param type        The class of the value. Must be supported by
     *                    {@link TypeFinder}
     * @param description The description for this value
     * @param validators  The validators to check new values with
     *
     * @throws IllegalArgumentException if type is not supported by
     *                                  {@link TypeFinder}
     */
    @SafeVarargs
    public EditorValue(String name, T value, Class<T> type, String description, Validator<? super T>... validators) {
        this(name, value, type, description, null, null, validators);
    }

    /**
     * Create a new EdítorValue to use in a {@link EditorTable} or
     * {@link EditorValueCollection}
     *
     * @param name        The name of this value
     * @param value       The default value of this EditorValue
     * @param type        The class of the value. Must be supported by
     *                    {@link TypeFinder} or be an enum
     * @param description The description for this value
     * @param min         If not null it is checked, if the value greather or
     *                    equal to this value. The type must be comperable if
     *                    min is not null
     * @param max         If not null it is checked, if the value less or equal
     *                    to this value. The type must be comperable if max is
     *                    not null
     * @param validators  The validators to check new values with
     *
     * @throws IllegalArgumentException if type is not supported by
     *                                  {@link TypeFinder}
     * @throws IllegalArgumentException if min or max is not null but the type
     *                                  is not Comperable
     */
    @SafeVarargs
    public EditorValue(String name, T value, Class<T> type, String description, T min, T max, Validator<? super T>... validators) {
        super(name);

        Objects.requireNonNull(value);
        Objects.requireNonNull(type);
        Objects.requireNonNull(description);

        if (!type.isEnum() && !TypeFinder.supports(type)) {
            throw new IllegalArgumentException("Type must be supported by TypeFinder");
        }

        if (min != null || max != null) {
            if (!type.isAssignableFrom(Comparable.class)) {
                throw new IllegalArgumentException("Type must be Comperable to have min and / or max");
            }
        }

        this.min = min;
        this.max = max;

        this.value = value;
        this.type = type;
        this.description = description;
        this.validators = new ArrayList<>();
        if (validators != null) {
            this.validators.addAll(Arrays.asList(validators));
        }
    }

    public boolean setValue(T value) {
        return setValue(value, null);
    }

    public boolean setValue(T value, Component errorParent) {
        if (validators != null) {
            for (Validator<? super T> v : validators) {
                String r = v.check(value);
                if (r != null && errorParent != null) {
                    JOptionPane.showMessageDialog(errorParent,
                            r,
                            String.format("Error setting Paramter \"%s\"", name), JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        String b = boundsCheck(value);
        if (b != null) {
            if (errorParent != null) {
                JOptionPane.showMessageDialog(errorParent,
                        b,
                        String.format("Error setting Paramter \"%s\"", name), JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        this.value = value;
        return true;
    }

    public boolean fromString(String str) {
        return fromString(str, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean fromString(String str, Component errorParent) {
        if (dialogOpen) {
            return false;
        }
        
        String a = fromStringNoGui(str);
        if (a != null) {
            dialogOpen = true;
            JOptionPane.showMessageDialog(errorParent,
                    a,
                    String.format("Error setting Paramter \"%s\"", name), JOptionPane.ERROR_MESSAGE);
            dialogOpen = false;
        }

        return a == null;
    }

    /**
     * Set value from string
     *
     * @param str The string to parse
     *
     * @return null if successful, else the error message
     */
    @SuppressWarnings("unchecked")
    public String fromStringNoGui(String str) {
        T val;
        if (type.isEnum()) {
            val = (T) TypeUtils.findEnumValue(str, (Class<? extends Enum>) type);
        }
        else {
            val = TypeFinder.parse(str, type);
        }

        if (val != null) {
            if (validators != null) {
                for (Validator<? super T> v : validators) {
                    String r = v.check(val);
                    if (r != null) {
                        return r;
                    }
                }
            }

            String bound = boundsCheck(value);
            if (bound != null) {
                return bound;
            }

            this.value = val;
            return null; // OK
        }
        else {
            return String.format("\"%s\" cannot be converted to %s", str, type.getSimpleName());
        }
    }

    /**
     * Add Validators
     *
     * @param validators The validators
     */
    @SuppressWarnings("unchecked")
    public void addValidators(Validator<? super T>... validators) {
        this.validators.addAll(Arrays.asList(validators));
    }

    @SuppressWarnings("unchecked")
    private String boundsCheck(T value) {
        if (min != null || max != null) {
            Comparable<T> comp = (Comparable<T>) value;
            if (min != null) {
                if (comp.compareTo(min) < 0) {
                    return String.format("Parameter \"%s\" is not allowed to be < %s", name, min.toString());
                }
            }
            if (max != null) {
                if (comp.compareTo(max) > 0) {
                    return String.format("Parameter \"%s\" is not allowed to be > %s", name, max.toString());
                }
            }
        }

        return null;
    }

    //<editor-fold defaultstate="collapsed" desc="Getter">
    public T getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public Class<T> getType() {
        return type;
    }
    //</editor-fold>

}
