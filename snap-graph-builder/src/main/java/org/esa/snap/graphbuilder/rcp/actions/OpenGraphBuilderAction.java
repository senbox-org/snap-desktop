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
package org.esa.snap.graphbuilder.rcp.actions;

import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.InputStream;

@ActionID(
        category = "Tools",
        id = "GraphBuilderAction"
)
@ActionRegistration(
        displayName = "#CTL_GraphBuilderAction_MenuText",
        popupText = "#CTL_GraphBuilderAction_MenuText",
        iconBase = "org/esa/snap/graphbuilder/icons/graph.png",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Menu/Tools",position = 310, separatorBefore = 300),
        @ActionReference(path = "Toolbars/" + PackageDefaults.GPT_GRAPH_BUILDER_TOOLBAR, position = 10)
})
@NbBundle.Messages({
        "CTL_GraphBuilderAction_MenuText=GraphBuilder",
        "CTL_GraphBuilderAction_ShortDescription=Create a custom processing graph"
})
public class OpenGraphBuilderAction extends AbstractAction {

    public OpenGraphBuilderAction() {
        super("GraphBuilder");
    }

    public void actionPerformed(ActionEvent event) {
        final GraphBuilderDialog dialog = new GraphBuilderDialog(SnapApp.getDefault().getAppContext(), "Graph Builder", "graph_builder");
        //dialog.getJDialog().setIconImage(IconUtils.esaPlanetIcon.getImage());
        dialog.show();

        InputStream graphFileStream = getClass().getClassLoader().getResourceAsStream("graphs/ReadWriteGraph.xml");

        dialog.loadGraph(graphFileStream, null);
        dialog.enableInitialInstructions(true);
    }
}