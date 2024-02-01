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

import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.NodeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of GraphNodes
 */
public class GraphNodeList {

    private final List<GraphNode> nodeList = new ArrayList<>(10);

    GraphNode[] getGraphNodes() {
        return nodeList.toArray(new GraphNode[nodeList.size()]);
    }

    void clear() {
        nodeList.clear();
    }

    public void add(final GraphNode newGraphNode) {
        nodeList.add(newGraphNode);
    }

    public void remove(final GraphNode node) {
        // remove as a source from all nodes
        for (GraphNode n : nodeList) {
            n.disconnectOperatorSources(node.getID());
        }

        nodeList.remove(node);
    }

    public GraphNode findGraphNode(String id) {
        for (GraphNode n : nodeList) {
            if (n.getID().equals(id)) {
                return n;
            }
        }
        return null;
    }

    public GraphNode findGraphNodeByOperator(String operatorName) {
        for (GraphNode n : nodeList) {
            if (n.getOperatorName().equals(operatorName)) {
                return n;
            }
        }
        return null;
    }

    public GraphNode[] findAllGraphNodeByOperator(String operatorName) {
        final List<GraphNode> resultList = new ArrayList<>();
        for (GraphNode n : nodeList) {
            if (n.getOperatorName().equals(operatorName)) {
                resultList.add(n);
            }
        }
        return resultList.toArray(new GraphNode[0]);
    }

    boolean isGraphComplete() {
        int nodesWithoutSources = 0;
        for (GraphNode n : nodeList) {
            if (!n.hasSources()) {
                ++nodesWithoutSources;
                if (!IsNodeASource(n))
                    return false;
            }
        }
        return nodesWithoutSources != nodeList.size();
    }

    void assignParameters(final XppDom presentationXML) throws GraphException {
        for (GraphNode n : nodeList) {
            if (n.getOperatorUI() != null) {
                n.assignParameters(presentationXML);
            }
        }
    }

    void updateGraphNodes(final GraphContext graphContext) throws GraphException {
        if (graphContext != null) {
            for (GraphNode n : nodeList) {
                final NodeContext context = graphContext.getNodeContext(n.getNode());
                if(context.getOperator() != null) {
                    Product[] sourceProducts = context.getSourceProducts();
                    for (final Product product : sourceProducts) {
                        System.out.println("5 - " + product.getName());
                    }
                    n.setSourceProducts(sourceProducts);
                }
                n.updateParameters();
            }
        }
    }

    private boolean IsNodeASource(final GraphNode sourceNode) {
        for (GraphNode n : nodeList) {
            if (n.isNodeSource(sourceNode))
                return true;
        }
        return false;
    }

    GraphNode[] findConnectedNodes(final GraphNode sourceNode) {
        final List<GraphNode> connectedNodes = new ArrayList<>();
        for (GraphNode n : nodeList) {
            if (n.isNodeSource(sourceNode))
                connectedNodes.add(n);
        }
        return connectedNodes.toArray(new GraphNode[connectedNodes.size()]);
    }

    void switchConnections(final GraphNode oldNode, final String newNodeID) {
        final GraphNode[] connectedNodes = findConnectedNodes(oldNode);
        for (GraphNode node : connectedNodes) {
            node.connectOperatorSource(newNodeID);
        }
    }
}
