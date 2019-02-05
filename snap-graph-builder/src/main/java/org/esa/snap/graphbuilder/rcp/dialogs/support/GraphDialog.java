package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.io.File;

/**
 * dialogs which are able to handle graphs
 */
public interface GraphDialog {

    void loadGraph();

    void loadGraph(final File file);

    boolean canSaveGraphs();

    void saveGraph();

    String getGraphAsString() throws Exception;
}
