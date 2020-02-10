package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

public interface GraphListener {
    void selected(NodeGui source);

    void deselected(NodeGui source);

    void updated(NodeGui source);

    void created(NodeGui source);

    void deleted(NodeGui source);

}