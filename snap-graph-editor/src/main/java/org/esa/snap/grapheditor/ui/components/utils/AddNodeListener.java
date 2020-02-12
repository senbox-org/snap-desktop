package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

import java.awt.*;

public interface AddNodeListener {
    void newNodeAdded(NodeGui node);
    void newNodeAddedAtCurrentPosition(NodeGui node);
}
