package de.minekonst.mariokartwiiai.tracks;

import java.util.NoSuchElementException;
import lombok.Getter;

public enum Hardness {

    SPECIAL(1.2),
    ROAD(1),
    HARD_GROUND(0.4),
    OFF_ROAD(0.2),
    OUT_OF_BOUNDS(0);
    
    @Getter private final double value;

    private Hardness(double value) {
        this.value = value;
    }
    
    public static Hardness fromValue(double value) {
        for (Hardness h : values()) {
            if (h.getValue() == value) {
                return h;
            }
        }
        
        throw new NoSuchElementException();
    }
}
