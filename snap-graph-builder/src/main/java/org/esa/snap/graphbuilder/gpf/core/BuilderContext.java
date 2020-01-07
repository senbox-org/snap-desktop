package org.esa.snap.graphbuilder.gpf.core;

import java.util.ArrayList;

import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;

public class BuilderContext {
    private ArrayList<GraphNode> nodes;

    public BuilderContext(){
        nodes = new ArrayList<GraphNode>();
    }

    public void addNode(GraphNode node){
        nodes.add(node);
    }
}