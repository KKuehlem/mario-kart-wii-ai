package de.minekonst.mariokartwiiai.shared.utils;

import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class GlobalKeyListener implements NativeKeyListener {
    
    private volatile boolean spaceDown;

    @Override
    public void nativeKeyTyped(NativeKeyEvent n) {
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent n) {
        if (n.getKeyCode() == NativeKeyEvent.VC_SPACE) {
            spaceDown = true;
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent n) {
        if (n.getKeyCode() == NativeKeyEvent.VC_SPACE) {
            spaceDown = false;
        }
    }

    public boolean isSpaceDown() {
        return spaceDown;
    }

}
