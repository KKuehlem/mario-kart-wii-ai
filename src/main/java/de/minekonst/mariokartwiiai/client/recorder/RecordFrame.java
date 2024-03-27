package de.minekonst.mariokartwiiai.client.recorder;

import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import java.io.Serializable;

public class RecordFrame implements Serializable {
    
    private static byte A_BUTTON = 1;
    private static byte B_BUTTON = 2;
    private static byte TRICK_BUTTON = 4;
    
    private static final long serialVersionUID = 1L;

    private final byte x;
    private final byte y;
    private final byte buttons;

    public RecordFrame(boolean a, boolean b, boolean trick, int x, int y) {
        this(toByte(a, b, trick), (byte) x, (byte) y);
    }
    
    public RecordFrame(byte buttons, byte x, byte y) {
        this.x = x;
        this.y = y;
        this.buttons = buttons; 
    }
    
    public ButtonState toButtonState() {
        return new ButtonState(isA(), isB(), false, isTrick(), x, y);
    }
    
    public byte getButtons() {
        return buttons;
    }

    public boolean isA() {
        return (buttons & A_BUTTON) == A_BUTTON;
    }

    public boolean isB() {
        return (buttons & B_BUTTON) == B_BUTTON;
    }

    public boolean isTrick() {
        return (buttons & TRICK_BUTTON) == TRICK_BUTTON;
    }

    public byte getX() {
        return x;
    }

    public byte getY() {
        return y;
    }
    
    private static byte toByte(boolean a, boolean b, boolean trick) {
        byte r = 0;
        if (a) {
            r |= A_BUTTON;
        }
        if (b) {
            r |= B_BUTTON;
        }
        if (trick) {
            r |= TRICK_BUTTON;
        }
        
        return r;
    }

}
