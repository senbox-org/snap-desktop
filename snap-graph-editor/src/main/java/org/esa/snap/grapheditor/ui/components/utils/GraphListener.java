package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

public interface GraphListener {
    public void selected(NodeGui source);

    public void deselected(NodeGui source);

    public void updated(NodeGui source);

    public void created(NodeGui source);

    public void deleted(NodeGui source);

    

    // public void refresh(Graph graph);

}