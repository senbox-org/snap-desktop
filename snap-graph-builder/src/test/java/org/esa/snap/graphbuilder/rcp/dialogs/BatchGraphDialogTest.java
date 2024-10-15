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

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class BatchGraphDialogTest {

    @Test
    @STTM("SNAP-3626")
    public void testEnsureWriteNodeTargetReset() {
        final GraphNode[] graphNodes = new GraphNode[3];

        graphNodes[0] = new GraphNode(new Node("Read", "theReader"));
        graphNodes[1] = new GraphNode(new Node("Process", "the_processor"));

        final GraphNode node = new GraphNode(new Node("Write", "WriteOp"));
        node.getParameterMap().put("file", new File("whatever"));
        graphNodes[2] = node;

        BatchGraphDialog.ensureWriteNodeTargetReset(graphNodes);

        Map<String, Object> parameterMap = graphNodes[2].getParameterMap();
        assertFalse(parameterMap.containsKey("file"));
    }

    @Test
    @STTM("SNAP-3626")
    public void testEnsureWriteNodeTargetReset_emptyNodeArray() {
        try {
            BatchGraphDialog.ensureWriteNodeTargetReset(new GraphNode[0]);
        } catch (Exception e) {
            fail("no exception expected");
        }
    }

    @Test
    @STTM("SNAP-3626")
    public void testEnsureWriteNodeTargetReset_noWriteNode() {
        final GraphNode[] graphNodes = new GraphNode[2];

        graphNodes[0] = new GraphNode(new Node("Read", "theReader"));
        graphNodes[1] = new GraphNode(new Node("Process", "the_processor"));

        try {
            BatchGraphDialog.ensureWriteNodeTargetReset(graphNodes);
        } catch (Exception e) {
            fail("no exception expected");
        }
    }
}
