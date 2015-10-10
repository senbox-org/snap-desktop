/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.dialogs.support;

import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.engine_utilities.util.ResourceUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Provides a dynamic graphs menu reflecting the graph file contents of ./snap/graphs
 *
 * @author Luis Veci
 */
public class GraphsMenu {

    private final Component parentComponent;
    private final GraphDialog graphDialog;
    private final Action loadAction;
    private final Action saveAction;
    private final Action viewGraphXMLAction;

    public GraphsMenu(final Component parentComponent, final GraphDialog graphDialog) {
        this.parentComponent = parentComponent;
        this.graphDialog = graphDialog;
        loadAction = new LoadAction();
        saveAction = new SaveAction();
        viewGraphXMLAction = new ViewGraphXMLAction();
    }

    /**
     * Creates the default menu.
     *
     * @return The menu
     */
    public JMenuBar createDefaultMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(loadAction);
        fileMenu.add(saveAction);
        fileMenu.addSeparator();
        fileMenu.add(viewGraphXMLAction);

        JMenu graphMenu = new JMenu("Graphs");
        createGraphMenu(graphMenu, ResourceUtils.getGraphFolder("").toFile());

        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(graphMenu);

        return menuBar;
    }

    private void createGraphMenu(final JMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        graphDialog.LoadGraph(file);
                    }
                });
                menu.add(item);
            }
        }
    }

    private class LoadAction extends AbstractAction {

        LoadAction() {
            super("Load Graph");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            graphDialog.LoadGraph();
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled();
        }
    }

    private class SaveAction extends AbstractAction {

        SaveAction() {
            super("Save Graph");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            graphDialog.SaveGraph();
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && graphDialog.canSaveGraphs();
        }
    }

    private class ViewGraphXMLAction extends AbstractAction {

        ViewGraphXMLAction() {
            super("View Graph XML");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String xml = "";
            try {
                xml = graphDialog.getGraphAsString();
            } catch (Exception e) {
                xml = "Unable to diaplay graph "+ e.toString();
            }

            JTextArea textArea = new JTextArea(xml);
            textArea.setEditable(false);
            JScrollPane textAreaScrollPane = new JScrollPane(textArea);
            textAreaScrollPane.setPreferredSize(new Dimension(360, 360));
            showInformationDialog("Graph XML", textAreaScrollPane);
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled();
        }
    }

    private void showInformationDialog(String title, Component component) {
        final ModalDialog modalDialog = new ModalDialog(UIUtils.getRootWindow(parentComponent),
                                                        title,
                                                        AbstractDialog.ID_OK,
                                                        null); /*I18N*/
        modalDialog.setContent(component);
        modalDialog.show();
    }
}
