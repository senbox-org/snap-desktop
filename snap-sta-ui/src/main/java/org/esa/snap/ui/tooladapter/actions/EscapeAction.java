package org.esa.snap.ui.tooladapter.actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *  Convenience Action to dispose of a Swing Window by using the Escape key.
 *  Before disposing of the window the Action will first attempt to hide
 *  any popups. In this case the user will need to invoke the Escape key a
 *  second time before the Window is disposed.
 */
public class EscapeAction extends AbstractAction {
    private static final String KEY_STROKE_AND_KEY = "ESCAPE";
    private static final KeyStroke ESCAPE_KEY_STROKE = KeyStroke.getKeyStroke( KEY_STROKE_AND_KEY );

    private static EscapeAction instance = new EscapeAction();

    /**
     *  Registers an EscapeAction on the specified JDialog
     *
     *  @param dialog the JDialog the EscapeAction is registered with
     */
    public static void register(JDialog dialog) {
        register(dialog.getRootPane());
    }

    /**
     *  Registers an EscapeAction on the specified JRootPane
     *
     *  @param rootPane the JRootPane the EscapeAction is registered with
     */
    public static void register(JRootPane rootPane) {
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ESCAPE_KEY_STROKE, KEY_STROKE_AND_KEY);
        rootPane.getActionMap().put(KEY_STROKE_AND_KEY, instance);
    }

    private EscapeAction()
    {
        super("Escape");
    }

    /**
     *  Implement the Escape Action. First attempt to hide a popup menu.
     *  If no popups are found then dispose the window.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        //  When a popup is visible the root pane of the Window will
        //  (generally) have focus
        Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JComponent rootPane = (JComponent)component;

        //  In some cases a component added to a popup menu may have focus, but
        //  we need the root pane to check for popup menu key bindings
        if (!(rootPane instanceof JRootPane)) {
            rootPane = (JComponent)SwingUtilities.getAncestorOfClass(JRootPane.class, component);
        }

        //  Hide the popup menu when an ESCAPE key binding is found, otherwise dispose the Window
        ActionListener escapeAction = getEscapeAction(rootPane);
        if (escapeAction != null) {
            escapeAction.actionPerformed(null);
        } else {
            Window window = SwingUtilities.windowForComponent(component);
            window.dispose();
        }
    }

    private ActionListener getEscapeAction(JComponent rootPane) {
        //  Search the parent InputMap to see if a binding for the ESCAPE key
        //  exists. This binding is added when a popup menu is made visible
        //  (and removed when the popup menu is hidden).
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (im == null || (im = im.getParent()) == null) {
            return null;
        }
        Object[] keys = im.keys();
        if (keys == null) {
            return null;
        }
        for (Object keyStroke : keys) {
            if (keyStroke.equals(ESCAPE_KEY_STROKE)) {
                Object key = im.get(ESCAPE_KEY_STROKE);
                return rootPane.getActionMap().get(key);
            }
        }
        return null;
    }

}