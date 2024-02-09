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
package org.esa.snap.graphbuilder.rcp.dialogs.support;

import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Observer;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * GraphExecuter Tester.
 *
 * @author lveci
 * @version 1.0
 * @since 12/21/2007
 */
public class TestGraphExecuter implements Observer {

    private GraphExecuter graphEx;
    private String updateValue = "";

    @Before
    public void setUp() throws Exception {
        //TestUtils.initTestEnvironment();
        graphEx = new GraphExecuter();
        graphEx.addObserver(this);
    }

    @Test
    public void testGetOperators() {
        Set opList = graphEx.GetOperatorList();

        assertTrue(!opList.isEmpty());
    }

    @Test
    public void testAddOperator() {
        updateValue = "";
        graphEx.addOperator("testOp");

        GraphNode[] nodeList = graphEx.getGraphNodes();
        assertEquals(1, nodeList.length);
        assertEquals(updateValue, "Add");
    }

    @Test
    public void testClear() {
        graphEx.addOperator("testOp");

        GraphNode[] nodeList = graphEx.getGraphNodes();
        assertEquals(1, nodeList.length);

        graphEx.clearGraph();
        assertEquals(0, graphEx.getGraphNodes().length);
    }

    @Test
    public void testRemoveOperator() {
        GraphNode node = graphEx.addOperator("testOp");

        GraphNode[] nodeList = graphEx.getGraphNodes();
        assertEquals(1, nodeList.length);

        updateValue = "";
        graphEx.removeOperator(node);
        assertEquals(0, graphEx.getGraphNodes().length);
        assertEquals(updateValue, "Remove");
    }

    @Test
    public void testFindGraphNode() {
        GraphNode lostNode = graphEx.addOperator("lostOp");

        GraphNode foundNode = graphEx.getGraphNodeList().findGraphNode(lostNode.getID());
        assertTrue(foundNode.equals(lostNode));

        graphEx.clearGraph();
    }

    @Test
    public void testSetSelected() {
        GraphNode node = graphEx.addOperator("testOp");

        updateValue = "";
        graphEx.setSelectedNode(node);

        assertEquals(updateValue, "Selected");

        graphEx.clearGraph();
    }

    @Test
    public void testCreateGraph() throws GraphException {
        GraphNode nodeA = graphEx.addOperator("testOp");
        GraphNode nodeB = graphEx.addOperator("testOp");

        nodeB.connectOperatorSource(nodeA.getID());

        //graphEx.writeGraph("D:\\data\\testGraph.xml");

        //graphEx.executeGraph(new NullProgressMonitor());
    }

    /**
     * Implements the functionality of Observer participant of Observer Design Pattern to define a one-to-many
     * dependency between a Subject object and any number of Observer objects so that when the
     * Subject object changes state, all its Observer objects are notified and updated automatically.
     * <p>
     * Defines an updating interface for objects that should be notified of changes in a subject.
     *
     * @param subject The Observerable subject
     * @param data    optional data
     */
    public void update(java.util.Observable subject, Object data) {

        GraphExecuter.GraphEvent event = (GraphExecuter.GraphEvent) data;
        GraphNode node = (GraphNode) event.getData();
        String opID = node.getNode().getId();
        if (event.getEventType() == GraphExecuter.events.ADD_EVENT) {
            updateValue = "Add";
        } else if (event.getEventType() == GraphExecuter.events.REMOVE_EVENT) {
            updateValue = "Remove";
        } else if (event.getEventType() == GraphExecuter.events.SELECT_EVENT) {
            updateValue = "Selected";
        }
    }
}
