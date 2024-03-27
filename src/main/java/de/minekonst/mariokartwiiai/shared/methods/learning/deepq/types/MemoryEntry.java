package de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MemoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double[] input;
    private final int actionTaken;
}
