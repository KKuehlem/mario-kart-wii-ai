package de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RewardMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<MemoryEntry> memory;
    private final double reward;
}
