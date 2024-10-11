/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.graphbuilder.rcp.dialogs;

import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.DefaultAppContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphBuilderDialogTest {

    @Test
    public void initUIInitializesComponentsCorrectly() {
        AppContext appContext = new DefaultAppContext("app");
        GraphBuilderDialog dialog = new GraphBuilderDialog(appContext, "Test Title", "Test Help ID");
        assertNotNull(dialog.tabbedPanel);
        assertNotNull(dialog.statusLabel);
        assertNotNull(dialog.progressPanel);
        assertNotNull(dialog.progressBar);
    }

    @Test
    public void clearGraphRemovesAllTabsAndClearsGraph() {
        AppContext appContext = new DefaultAppContext("app");
        GraphBuilderDialog dialog = new GraphBuilderDialog(appContext, "Test Title", "Test Help ID");
        dialog.clearGraph();
        assertEquals(0, dialog.tabbedPanel.getTabCount());
        assertEquals(0, dialog.graphEx.getGraphNodes().length);
    }

    @Test
    public void validateAllNodesReturnsFalseForInvalidNodes() {
        AppContext appContext = new DefaultAppContext("app");
        GraphBuilderDialog dialog = new GraphBuilderDialog(appContext, "Test Title", "Test Help ID");
        dialog.graphEx = mock(GraphExecuter.class);
        List<GraphNode> nodes = new ArrayList<>();
        GraphNode invalidNode = mock(GraphNode.class);
        nodes.add(invalidNode);
        when(dialog.graphEx.getGraphNodes()).thenReturn(nodes.toArray(new GraphNode[0]));
        assertFalse(dialog.validateAllNodes());
    }
}