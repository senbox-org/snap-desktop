package org.esa.snap.grapheditor.ui.components.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GraphKeyEventDispatcher implements KeyEventDispatcher {
    private KeyListener defaultListener;

    @Contract(pure = true)
    public GraphKeyEventDispatcher(KeyListener listener){
        defaultListener = listener;
    }

    @Override
    public boolean dispatchKeyEvent(@NotNull KeyEvent event) {
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