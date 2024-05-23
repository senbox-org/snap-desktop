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

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIRegistry;
import org.esa.snap.rcp.util.Dialogs;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Set;

public class GraphExecuter extends Observable {

    private final GPF gpf;
    private Graph graph;
    private GraphContext graphContext = null;
    private GraphProcessor processor;
    private String graphDescription = "";
    private File lastLoadedGraphFile = null;

    private final GraphNodeList graphNodeList = new GraphNodeList();
    private final static String LAST_GRAPH_PATH = "graphbuilder.last_graph_path";

    public enum events {ADD_EVENT, REMOVE_EVENT, SELECT_EVENT, CONNECT_EVENT, REFRESH_EVENT}

    public GraphExecuter() {

        gpf = GPF.getDefaultInstance();

        graph = new Graph("Graph");
    }

    public GraphNode[] getGraphNodes() {
        return graphNodeList.getGraphNodes();
    }

    public GraphNodeList getGraphNodeList() {
        return graphNodeList;
    }

    public void clearGraph() {
        graph = null;
        graph = new Graph("Graph");
        lastLoadedGraphFile = null;
        graphNodeList.clear();
    }

    public void setSelectedNode(GraphNode node) {
        if (node == null) return;

        notifyGraphEvent(new GraphEvent(events.SELECT_EVENT, node));
    }

    /**
     * Gets the list of operators
     *
     * @return set of operator names
     */
    public Set<String> GetOperatorList() {
        return gpf.getOperatorSpiRegistry().getAliases();
    }

    boolean isOperatorInternal(String alias) {
        final OperatorSpiRegistry registry = gpf.getOperatorSpiRegistry();
        final OperatorSpi operatorSpi = registry.getOperatorSpi(alias);
        final OperatorMetadata operatorMetadata = operatorSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);
        return !(operatorMetadata != null && !operatorMetadata.internal());
    }

    String getOperatorCategory(String alias) {
        final OperatorSpiRegistry registry = gpf.getOperatorSpiRegistry();
        final OperatorSpi operatorSpi = registry.getOperatorSpi(alias);
        final OperatorMetadata operatorMetadata = operatorSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);
        if (operatorMetadata != null)
            return operatorMetadata.category();
        return "";
    }

    public GraphNode addOperator(final String opName) {

        String id = opName;
        int cnt = 1;
        while (graphNodeList.findGraphNode(id) != null) {
            ++cnt;
            id = opName + '(' + cnt + ')';
        }
        final GraphNode newGraphNode = createNewGraphNode(graph, opName, id);

        notifyGraphEvent(new GraphEvent(events.ADD_EVENT, newGraphNode));

        return newGraphNode;
    }

    static GraphNode createNewGraphNode(final Graph graph, final GraphNodeList graphNodeList,
                                        final String opName, final String id) {
        final Node newNode = new Node(id, opName);

        final XppDomElement parameters = new XppDomElement("parameters");
        newNode.setConfiguration(parameters);

        graph.addNode(newNode);

        final GraphNode newGraphNode = new GraphNode(newNode);
        graphNodeList.add(newGraphNode);

        newGraphNode.setOperatorUI(OperatorUIRegistry.CreateOperatorUI(newGraphNode.getOperatorName()));

        return newGraphNode;
    }

    private GraphNode createNewGraphNode(final Graph graph, final String opName, final String id) {
        final Node newNode = new Node(id, opName);

        final XppDomElement parameters = new XppDomElement("parameters");
        newNode.setConfiguration(parameters);

        graph.addNode(newNode);

        final GraphNode newGraphNode = new GraphNode(newNode);
        graphNodeList.add(newGraphNode);

        newGraphNode.setOperatorUI(OperatorUIRegistry.CreateOperatorUI(newGraphNode.getOperatorName()));

        moveWriterToLast(graph);

        return newGraphNode;
    }

    private void moveWriterToLast(final Graph graph) {
        final String writeOperatorAlias = OperatorSpi.getOperatorAlias(WriteOp.class);
        final GraphNode writerNode = graphNodeList.findGraphNode(writeOperatorAlias);
        if (writerNode != null) {
            removeNode(writerNode);

            graphNodeList.add(writerNode);
            graph.addNode(writerNode.getNode());
        }
    }

    public void removeOperator(final GraphNode node) {

        notifyGraphEvent(new GraphEvent(events.REMOVE_EVENT, node));

        removeNode(node);
    }

    private void removeNode(final GraphNode node) {
        graphNodeList.remove(node);
        graph.removeNode(node.getID());
    }

    void autoConnectGraph() {
        final List<GraphNode> nodeList = Arrays.asList(getGraphNodes());
        Collections.sort(nodeList, new GraphNodePosComparator());
        final GraphNode[] nodes = nodeList.toArray(new GraphNode[nodeList.size()]);

        for (int i = 0; i < nodes.length - 1; ++i) {
            if (!nodes[i].hasSources()) {
                nodes[i].connectOperatorSource(nodes[i + 1].getID());
            }
        }
        notifyConnection();
    }

    void notifyConnection() {
        if(graphNodeList.getGraphNodes().length > 0) {
            notifyGraphEvent(new GraphEvent(events.CONNECT_EVENT, graphNodeList.getGraphNodes()[0]));
        }
    }

    private void notifyGraphEvent(final GraphEvent event) {
        setChanged();
        notifyObservers(event);
        clearChanged();
    }

    public void setOperatorParam(final String id, final String paramName, final String value) {
        final Node node = graph.getNode(id);
        DomElement xml = node.getConfiguration().getChild(paramName);
        if (xml == null) {
            xml = new XppDomElement(paramName);
            node.getConfiguration().addChild(xml);
        }
        xml.setValue(value);
    }

    public String getOperatorParam(final String id, final String paramName) {
        final Node node = graph.getNode(id);
        DomElement xml = node.getConfiguration().getChild(paramName);
        return xml == null ? null : xml.getValue();
    }

    private void assignAllParameters() throws GraphException {

        final XppDom presentationXML = new XppDom("Presentation");

        // save graph description
        final XppDom descXML = new XppDom("Description");
        descXML.setValue(graphDescription);
        presentationXML.addChild(descXML);

        graphNodeList.assignParameters(presentationXML);
        graph.setAppData("Presentation", presentationXML);
    }

    public boolean initGraph() throws GraphException {
        if (graphNodeList.isGraphComplete()) {
            assignAllParameters();

            ProductSetUIHandler productSetHandler = new ProductSetUIHandler(graph, graphNodeList);
            SubGraphHandler subGraphHandler = new SubGraphHandler(graph, graphNodeList);

            try {
                recreateGraphContext();
                graphNodeList.registerGraphNodeUpdaters(graphContext);
                graphContext.init(null);
                //todo recreateGraphContext();
            } catch (Exception e) {
                e.printStackTrace();
                throw new GraphException(e.getMessage());
            } finally {
                subGraphHandler.restore();
                productSetHandler.restore();
            }
            return true;
        }
        return false;
    }

    private void recreateGraphContext() throws GraphException {
        if (graphContext != null)
            graphContext.dispose();

        processor = new GraphProcessor();
        graphContext = new GraphContext(graph, false);
    }

    public void disposeGraphContext() {
        graphContext.dispose();
    }

    /**
     * Begins graph processing
     *
     * @param pm The ProgressMonitor
     */
    public void executeGraph(ProgressMonitor pm) {
        processor.executeGraph(graphContext, pm);
    }

    public File[] getPotentialOutputFiles() {
        final List<File> fileList = new ArrayList<>();
        final Node[] nodes = graph.getNodes();
        for (Node n : nodes) {
            if (n.getOperatorName().startsWith(OperatorSpi.getOperatorAlias(WriteOp.class))) {
                final DomElement config = n.getConfiguration();
                final DomElement fileParam = config.getChild("file");
                if (fileParam != null) {
                    final String filePath = fileParam.getValue();
                    if (filePath != null && !filePath.isEmpty()) {
                        final File file = new File(filePath);
                        fileList.add(file);
                    }
                }
            }
        }
        return fileList.toArray(new File[fileList.size()]);
    }

    public File saveGraph() throws GraphException {

        String filename = "myGraph";
        if (lastLoadedGraphFile != null)
            filename = lastLoadedGraphFile.getAbsolutePath();
        final SnapFileFilter fileFilter = new SnapFileFilter("XML", "xml", "Graph");
        final File filePath = Dialogs.requestFileForSave("Save Graph", false, fileFilter, ".xml", filename,
                                                         null, LAST_GRAPH_PATH);
        if (filePath != null)
            writeGraph(filePath.getAbsolutePath());
        return filePath;
    }

    private void writeGraph(final String filePath) throws GraphException {

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            assignAllParameters();
            GraphIO.write(graph, fileWriter);
        } catch (Exception e) {
            throw new GraphException("Unable to write graph to " + filePath + '\n' + e.getMessage());
        }
    }

    public String getGraphAsString() throws GraphException, IOException {
        final StringWriter stringWriter = new StringWriter();
        try {
            assignAllParameters();
            GraphIO.write(graph, stringWriter);
        } catch (Exception e) {
            throw new GraphException("Unable to write graph to string" + '\n' + e.getMessage());
        } finally {
            stringWriter.close();
        }
        return stringWriter.toString();
    }

    public void loadGraph(final InputStream fileStream, final File file, final boolean addUI, final boolean wait) throws GraphException {

        try {
            if (fileStream == null) return;

            final LoadGraphThread loadGraphThread = new LoadGraphThread(fileStream, addUI);
            loadGraphThread.execute();
            if(wait) {
                loadGraphThread.get();
            }

            lastLoadedGraphFile = file;

        } catch (Throwable e) {
            throw new GraphException("Unable to load graph " + fileStream + '\n' + e.getMessage());
        }
    }

    private class LoadGraphThread extends SwingWorker<Graph, Object> {
        private final InputStream fileStream;
        private final boolean addUI;

        LoadGraphThread(final InputStream fileStream, final boolean addUI) {
            this.fileStream = fileStream;
            this.addUI = addUI;
        }

        @Override
        protected Graph doInBackground() throws Exception {
            final Graph graphFromFile = GPFProcessor.readGraph(new InputStreamReader(fileStream), null);

            setGraph(graphFromFile, addUI);
            notifyGraphEvent(new GraphEvent(events.REFRESH_EVENT, null));
            return graphFromFile;
        }
    }

    private void setGraph(final Graph graphFromFile, final boolean addUI) throws GraphException {
        if (graphFromFile != null) {
            graph = graphFromFile;
            graphNodeList.clear();

            final XppDom presentationXML = graph.getApplicationData("Presentation");
            if (presentationXML != null) {
                // get graph description
                final XppDom descXML = presentationXML.getChild("Description");
                if (descXML != null && descXML.getValue() != null) {
                    graphDescription = descXML.getValue();
                }
            }

            final Node[] nodes = graph.getNodes();
            for (Node n : nodes) {
                final GraphNode newGraphNode = new GraphNode(n);
                if (presentationXML != null)
                    newGraphNode.setDisplayParameters(presentationXML);
                graphNodeList.add(newGraphNode);

                if (addUI) {
                    OperatorUI ui = OperatorUIRegistry.CreateOperatorUI(newGraphNode.getOperatorName());
                    if (ui == null) {
                        throw new GraphException("Unable to load " + newGraphNode.getOperatorName());
                    }
                    newGraphNode.setOperatorUI(ui);
                }

                notifyGraphEvent(new GraphEvent(events.ADD_EVENT, newGraphNode));
            }
        }
    }

    public String getGraphDescription() {
        return graphDescription;
    }

    public void setGraphDescription(final String text) {
        graphDescription = text;
    }

    public List<File> getProductsToOpenInDAT() {
        final List<File> fileList = new ArrayList<>(2);
        final Node[] nodes = graph.getNodes();
        for (Node n : nodes) {
            if (n.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(WriteOp.class))) {
                final DomElement config = n.getConfiguration();
                final DomElement fileParam = config.getChild("file");
                if (fileParam != null) {
                    final String filePath = fileParam.getValue();
                    if (filePath != null && !filePath.isEmpty()) {
                        final File file = new File(filePath);
                        if (file.exists()) {
                            fileList.add(file);
                        } else {
                            final DomElement formatParam = config.getChild("formatName");
                            final String format = formatParam.getValue();

                            final String ext = ReaderUtils.findExtensionForFormat(format);

                            File newFile = new File(file.getAbsolutePath() + ext);
                            if (newFile.exists()) {
                                fileList.add(newFile);
                            } else {
                                final String name = FileUtils.getFilenameWithoutExtension(file);
                                newFile = new File(name + ext);
                                if (newFile.exists())
                                    fileList.add(newFile);
                            }
                        }
                    }
                }
            }
        }
        return fileList;
    }

    /**
     * Update the nodes in the graph with the given reader file and writer file
     */
    public static void setGraphIO(final GraphExecuter graphEx,
                                  final String readID, final File readPath,
                                  final String writeID, final File writePath,
                                  final String format) {
        final GraphNode readNode = graphEx.getGraphNodeList().findGraphNode(readID);
        if (readNode != null) {
            graphEx.setOperatorParam(readNode.getID(), "file", readPath.getAbsolutePath());
        }

        if (writeID != null) {
            final GraphNode writeNode = graphEx.getGraphNodeList().findGraphNode(writeID);
            if (writeNode != null) {
                graphEx.setOperatorParam(writeNode.getID(), "formatName", format);
                graphEx.setOperatorParam(writeNode.getID(), "file", writePath.getAbsolutePath());
            }
        }
    }

    public static class GraphEvent {

        private final events eventType;
        private final Object data;

        GraphEvent(events type, Object d) {
            eventType = type;
            data = d;
        }

        public Object getData() {
            return data;
        }

        public events getEventType() {
            return eventType;
        }
    }

    static class GraphNodePosComparator implements Comparator<GraphNode> {

        public int compare(GraphNode o1, GraphNode o2) {
            double x1 = o1.getPos().getX();
            double y1 = o1.getPos().getY();
            double x2 = o2.getPos().getX();
            double y2 = o2.getPos().getY();

            double h1 = FastMath.hypot(x1, y1);
            double h2 = FastMath.hypot(x2, y2);

            return Double.compare(h2, h1);
        }
    }
}
