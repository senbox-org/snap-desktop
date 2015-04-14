/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.actions;

import org.esa.snap.dat.graphbuilder.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.IconUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;

@ActionID(
        category = "Tools",
        id = "GraphBuilderAction"
)
@ActionRegistration(
        displayName = "#CTL_GraphBuilderAction_MenuText",
        popupText = "#CTL_GraphBuilderAction_MenuText",
        iconBase = "org/esa/snap/icons/cog16.png",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Tools",
                position = 110
        ),
        @ActionReference(path = "Toolbars/GraphBuilder")
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
        final GraphBuilderDialog dialog = new GraphBuilderDialog(new SnapApp.SnapContext(), "Graph Builder", "graph_builder");
        dialog.getJDialog().setIconImage(IconUtils.esaPlanetIcon.getImage());
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        File graphFile = new File(graphPath, "ReadWriteGraph.xml");
        if (graphFile.exists()) {
            dialog.LoadGraph(graphFile);
        } else {
            InputStream graphFileStream = getClass().getClassLoader().getResourceAsStream("graphs/ReadWriteGraph.xml");

            dialog.LoadGraph(graphFileStream);
        }
        dialog.EnableInitialInstructions(true);
    }
}