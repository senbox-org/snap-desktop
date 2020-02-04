package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

public interface NodeListener {
    /**
     * Update the inputs node of the connected nodes.
     * @param source: source of the event
     */
    public void outputChanged(NodeGui source);
}
