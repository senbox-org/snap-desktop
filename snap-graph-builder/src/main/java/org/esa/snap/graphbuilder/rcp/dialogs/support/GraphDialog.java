package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.io.File;

/**
 * dialogs which are able to handle graphs
 */
public interface GraphDialog {

    void LoadGraph();

    void LoadGraph(final File file);

    boolean canSaveGraphs();

    void SaveGraph();

    String getGraphAsString() throws Exception;
}
