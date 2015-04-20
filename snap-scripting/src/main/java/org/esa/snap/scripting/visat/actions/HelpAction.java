/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.scripting.visat.actions;

import org.esa.snap.scripting.visat.ScriptConsoleTopComponent;
import org.esa.snap.tango.TangoIcons;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HelpAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.help";

    public HelpAction(ScriptConsoleTopComponent scriptConsoleTC) {
        super(scriptConsoleTC, "Help", ID, TangoIcons.apps_help_browser(TangoIcons.Res.R16));
    }


    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof Component) {
            Component component = (Component) source;
            JPopupMenu popupMenu = createHelpMenu();
            popupMenu.show(component, 0, component.getHeight());
        }
    }

 
    private JPopupMenu createHelpMenu() {
        final JPopupMenu jsMenu = new JPopupMenu();
        final String[][] entries = new String[][]{
                {"BEAM JavaScript (BEAM Wiki)", "http://www.brockmann-consult.de/beam-wiki/display/BEAM/BEAM+JavaScript+Examples"},
                {"JavaScript Introduction (Mozilla)", "http://developer.mozilla.org/en/docs/JavaScript"},
                {"JavaScript Syntax (Wikipedia)", "http://en.wikipedia.org/wiki/JavaScript_syntax"},
        };


        for (final String[] entry : entries) {
            final String text = entry[0];
            final String target = entry[1];
            final JMenuItem menuItem = new JMenuItem(text);
            menuItem.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(target));
                } catch (IOException | URISyntaxException e1) {
                    getScriptConsoleTopComponent().showErrorMessage(e1.getMessage());
                }
            });
            jsMenu.add(menuItem);
        }
        return jsMenu;
    }

}
