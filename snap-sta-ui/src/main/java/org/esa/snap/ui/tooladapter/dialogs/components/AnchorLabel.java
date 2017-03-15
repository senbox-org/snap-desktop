/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui.tooladapter.dialogs.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * Simple hyperlink-like label that navigates to the given tab index and component.
 *
 * @author  Cosmin Cara
 */
public class AnchorLabel extends JLabel {
    private JTabbedPane parentTabControl;
    private int tabIndex;
    private JComponent component;

    public AnchorLabel(String text, JTabbedPane parent, int index, JComponent anchoredComponent) {
        super(text);
        this.parentTabControl = parent;
        this.tabIndex = index;
        this.component = anchoredComponent;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enableEvents(MouseEvent.MOUSE_EVENT_MASK);
    }

    @Override
    public void setText(String text) {
        super.setText("<html><font color=\"#FF0000\">" + text + "</font></html>");
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            parentTabControl.setSelectedIndex(tabIndex);
            if (component instanceof JPanel &&
                    component.getComponents() != null && component.getComponents().length > 0) {
                component.getComponent(0).requestFocusInWindow();
            } else {
                component.requestFocusInWindow();
            }
            if (component instanceof JTextField)
                SwingUtilities.invokeLater(() -> {
                    ((JTextField) component).setCaretPosition(((JTextField) component).getDocument().getLength());
                });
        }
    }
}
