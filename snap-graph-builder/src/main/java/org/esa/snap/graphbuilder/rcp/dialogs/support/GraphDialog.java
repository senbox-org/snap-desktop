package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.nio.file.Path;

/**
 * dialogs which are able to handle graphs
 */
public interface GraphDialog {

    void loadGraph();

    void loadGraph(final Path file);

    boolean canSaveGraphs();

    void saveGraph();

    String getGraphAsString() throws Exception;
}
