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
