package org.esa.snap.graphbuilder.ui.components.helpers;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GraphKeyEventDispatcher implements KeyEventDispatcher {
    private KeyListener defaultListener;

    public GraphKeyEventDispatcher(KeyListener listener){
        defaultListener = listener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch(event.getID()) {
            case (KeyEvent.KEY_PRESSED):
                defaultListener.keyPressed(event);
                break;
            case (KeyEvent.KEY_RELEASED):
                defaultListener.keyReleased(event);
                break;
            case (KeyEvent.KEY_TYPED):
                defaultListener.keyPressed(event);
                break;
        }
        return true;
    }

}