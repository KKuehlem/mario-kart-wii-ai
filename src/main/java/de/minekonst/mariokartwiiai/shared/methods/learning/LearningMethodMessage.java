package de.minekonst.mariokartwiiai.shared.methods.learning;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LearningMethodMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Serializable object;
}
