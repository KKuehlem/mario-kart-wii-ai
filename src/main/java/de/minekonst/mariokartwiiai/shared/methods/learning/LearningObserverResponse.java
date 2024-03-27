package de.minekonst.mariokartwiiai.shared.methods.learning;

import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LearningObserverResponse {

    private final ButtonState buttons;
    private final String title;
}
