package org.esa.snap.graphbuilder.gpf.core;

import java.util.List;
import java.util.ArrayList;

import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeContext;
import org.esa.snap.core.gpf.graph.Graph;

public class BuilderContext {
    private ArrayList<NodeContext> nodes;

    public BuilderContext(){

    }

    public Boolean verify(Node node) {
        return true;    
    }
}