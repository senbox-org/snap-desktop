package org.esa.snap.grapheditor.ui.components.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;

import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.esa.snap.core.gpf.internal.OperatorContext;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.grapheditor.gpf.ui.OperatorUI;
import org.esa.snap.grapheditor.gpf.ui.OperatorUIRegistry;
import org.esa.snap.grapheditor.ui.components.NodeGui;
import org.esa.snap.grapheditor.ui.components.interfaces.NodeListener;
import org.esa.snap.grapheditor.ui.components.interfaces.RefreshListener;
import org.esa.snap.ui.AppContext;

/**
 * The GraphManager implements most of the funcionality needed from the GraphBuilder and unify tools from different
 * parts of SNAP.
 * The main capabilities are:
 *  - create nodes
 *  - store the full graph
 *  - save and load graph
 *  - multi-threading graph validation
 *  - graph evaluation
 * It is also implemented as a *singleton*.
 *
 * @author Martino Ferrari (CS Group)
 */
public class GraphManager implements NodeListener {
    private final OperatorSpiRegistry opSpiRegistry;

    private final HashMap<String, UnifiedMetadata> simpleMetadata = new HashMap<>();

    private final ArrayList<NodeGui> nodes = new ArrayList<>();
    private final Graph graph = new Graph("");


    static private GraphManager instance = null;

    private final HashSet<RefreshListener> listeners = new HashSet<>();
    private AppContext appContext = null;

    private ValidateWorker currentJob = null;

    /**
     * Access the GraphManager instance,
     * @return the instance
     */
    static public GraphManager getInstance() {
        if (instance == null) {
            instance = new GraphManager();
        }
        return instance;
    }

    /**
     * private GraphManager initializer. It extract all the Operator from the Operator SPI registry and fill the
     * metadata lists.
     */
    private GraphManager() {
        GPF gpf = GPF.getDefaultInstance();
        opSpiRegistry = gpf.getOperatorSpiRegistry();
        for (final OperatorSpi opSpi : opSpiRegistry.getOperatorSpis()) {
            OperatorDescriptor descriptor = opSpi.getOperatorDescriptor();
            if (descriptor != null && !descriptor.isInternal()) {
                OperatorMetadata operatorMetadata = opSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);

                ArrayList<OperatorMetadata> metadata = new ArrayList<>();
                metadata.add(operatorMetadata);
                simpleMetadata.put(operatorMetadata.alias(), new UnifiedMetadata(operatorMetadata, descriptor));
                              
            }
        }
    }

    /**
     * Sets the default AppContex.
     * @param context default app context
     */
    public void setAppContext(AppContext context) {
        this.appContext = context;
    }

    /**
     * Add a RefreshListener, it will be notify when a refresh is needed.
     * @param l listener to add
     */
    public void addEventListener(RefreshListener l) {
        listeners.add(l);
    }

    /**
     * Notify all listeners to refresh the UI.
     */
    private void triggerEvent() {
        for (RefreshListener l : listeners) {
            l.refresh();
        }
    }

    /**
     * Create an Operator from metadata.
     * @param metadata input metadata
     * @return the new operator
     */
    private Operator getOperator(UnifiedMetadata metadata) {
        OperatorSpi spi = opSpiRegistry.getOperatorSpi(metadata.getName());
        if (spi != null) {
            return spi.createOperator();
        }
        return null;
    }

    /**
     * Retrive the collection of metadata loaded from the register.
     * @return all the available metadata
     */
    public Collection<UnifiedMetadata> getSimplifiedMetadata() {
        return simpleMetadata.values();
    }

    /**
     * Creates a new graph Node from the operator name.
     * @param operator operator name
     * @return the new graph Node
     */
    private Node createNode(final String operator) {
        final Node newNode = new Node(id(operator), operator);

        final XppDomElement parameters = new XppDomElement("parameters");
        newNode.setConfiguration(parameters);

        return newNode;
    }

    /**
     * Extract the configuration from a graph Node.
     * @param node input node
     * @return node configuration map
     */
    private Map<String, Object> getConfiguration(final Node node) {
        final HashMap<String, Object> parameterMap = new HashMap<>();
        final String opName = node.getOperatorName();
        final OperatorSpi operatorSpi = opSpiRegistry.getOperatorSpi(opName);

        final ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer valueContainer = PropertyContainer.createMapBacked(parameterMap,
                operatorSpi.getOperatorClass(), parameterDescriptorFactory);

        final DomElement config = node.getConfiguration();
        final int count = config.getChildCount();
        for (int i = 0; i < count; ++i) {
            final DomElement child = config.getChild(i);
            final String name = child.getName();
            final String value = child.getValue();
            try {
                if (name == null || value == null || value.startsWith("$")) {
                    continue;
                }
                if (child.getChildCount() == 0) {
                    final Converter<?> converter = getConverter(valueContainer, name);
                    if (converter != null) {
                        parameterMap.put(name, converter.parse(value));
                    }
                } else {
                    final DomConverter domConverter = getDomConverter(valueContainer, name);
                    if (domConverter != null) {
                        try {
                            final Object obj = domConverter.convertDomToValue(child, null);
                            parameterMap.put(name, obj);
                        } catch (final Exception e) {
                            SystemUtils.LOG.warning(e.getMessage());
                        }
                    } else {
                        final Converter<?> converter = getConverter(valueContainer, name);
                        final Object[] objArray = new Object[child.getChildCount()];
                        int c = 0;
                        for (final DomElement ch : child.getChildren()) {
                            final String v = ch.getValue();

                            if (converter != null) {
                                objArray[c++] = converter.parse(v);
                            } else {
                                objArray[c++] = v;
                            }
                        }
                        parameterMap.put(name, objArray);
                    }
                }
            } catch (final ConversionException e) {
                SystemUtils.LOG.info(e.getMessage());
            }
        }
        return parameterMap;
    }

    /**
     * Create a unique ID for a given operator
     * @param opName operator name
     * @return unique ID
     */
    private String id(final String opName) {
        final String res = opName + " ";
        int counter = 0;
        int N = res.length();
        for (NodeGui n : nodes) {

            if (n.getName().startsWith(res)) {
                String postfix = n.getName().substring(N);
                try {
                    int id = Integer.parseInt(postfix);
                    if (id >= counter) {
                        counter = id + 1;
                    }
                } catch (NumberFormatException e) {
                    // not a problem
                }
            }
        }

        return res + counter;
    }

    /**
     * Get propertyConverter for the given property.
     * @param valueContainer property container
     * @param name name of the property to be converted
     * @return the propertyConverter if it is found (null otherwise)
     */
    private static Converter<?> getConverter(final PropertyContainer valueContainer, final String name) {
        final Property[] properties = valueContainer.getProperties();

        for (final Property p : properties) {

            final PropertyDescriptor descriptor = p.getDescriptor();
            if (descriptor != null && (descriptor.getName().equals(name)
                    || (descriptor.getAlias() != null && descriptor.getAlias().equals(name)))) {
                return descriptor.getConverter();
            }
        }
        return null;
    }

    /**
     * Get the DOM property converter for a given poperty.
     * @param valueContainer property container
     * @param name property name
     * @return dom converter
     */
    private static DomConverter getDomConverter(final PropertyContainer valueContainer, final String name) {
        final Property[] properties = valueContainer.getProperties();

        for (final Property p : properties) {

            final PropertyDescriptor descriptor = p.getDescriptor();
            if (descriptor != null && (descriptor.getName().equals(name) ||
                    (descriptor.getAlias() != null && descriptor.getAlias().equals(name)))) {
                return descriptor.getDomConverter();
            }
        }
        return null;
    }

    /**
     * Retrieve the sub-menu for the given category
     * @param menu root menu
     * @param category searched category
     * @return category sub-menu
     */
    static private JMenu getCategoryMenu(JMenu menu, String category) {
        if (category == null || category.length() == 0) 
            return menu;
        String first = category.split("/")[0];
        
        String rest = "";
        if (first.length() < category.length()) {
            rest = category.substring(first.length()+1);
        }
        int menusCounter = 0;
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item instanceof JMenu) {
                if (item.getText().equals(first)) {
                    return getCategoryMenu((JMenu) item, rest);
                }
                menusCounter ++;
            }
        }
        JMenu newMenu = new JMenu(first);
        menu.insert(newMenu, menusCounter);
        if (rest.length() > 0)
            return getCategoryMenu(newMenu, rest);
        return newMenu;
    }

    /**
     * Create the Operator Menu using the available metadata.
     *
     * @return new operators menu
     */
    public JMenu createOperatorMenu(ActionListener listener) {
        JMenu addMenu = new JMenu("Add");
        for (UnifiedMetadata metadata: getSimplifiedMetadata()) {
            JMenu menu = getCategoryMenu(addMenu, metadata.getCategory());
            JMenuItem item = new JMenuItem(metadata.getName());
            item.setHorizontalTextPosition(JMenuItem.RIGHT);
            item.addActionListener(listener);
            menu.add(item);
        }
        return addMenu;
    }

    /**
     * Creates a new node from the operator name.
     * @param opName operator name
     * @return new node
     */
    public NodeGui newNode(String opName){
        if (simpleMetadata.containsKey(opName))
            return newNode(simpleMetadata.get(opName));
        return null;
    }

    /**
     * Creates a new node from a given operator metadata
     * @param metadata operator metadata
     * @return new node
     */
    public NodeGui newNode(UnifiedMetadata metadata) {
        OperatorUI ui = OperatorUIRegistry.CreateOperatorUI(metadata.getName());
        Node node = createNode(metadata.getName());
        this.graph.addNode(node);
        Operator operator = GraphManager.getInstance().getOperator(metadata);
        assert operator != null;
        NodeGui newNode = new NodeGui(node, getConfiguration(node), metadata, ui, new OperatorContext(operator));
        this.nodes.add(newNode);
        newNode.addNodeListener(this);
        NotificationManager.getInstance().info(newNode.getName(), "Created");
        return newNode;
    }

    /**
     * Get the list of all nodes of the current graph
     * @return list of current nodes
     */
    public List<NodeGui> getNodes() {
        return this.nodes;
    }

    /**
     * Evaluates the current graph.
     */
    public void evaluate() {
        if (currentJob != null && !currentJob.isDone()) {
            currentJob.cancel(true);
        }
        GraphProcessor processor = new GraphProcessor();
        try {
            processor.executeGraph(graph, NotificationManager.getInstance());
        } catch (GraphException e) {
            NotificationManager.getInstance().error("Graph Execution", e.getMessage());
        }
    }

    /**
     * Validate the full graph.
     * The validation will be done on a separate thread.
     */
    private void validate() {
        ArrayList<NodeGui> sources = new ArrayList<>();
        for (NodeGui n: nodes ){
            if (n.isSource()) {
                sources.add(n);
            }
        }
        for (NodeGui n: sources) {
            n.updateSources();
            validate(n, true);
        }
    }

    /**
     * Validate a subset of the graph starting from the given node, including the node.
     * The validation will be done on a separate thread.
     * @param source root node of the sub-graph
     */
    private void validate(NodeGui source) {
        validate(source, true);
    }

    /**
     * Validate a subset of the graph from the given node, including or not the given node.
     * The validation will be done on a separate thread.
     *
     * @param source root node of the sub-graph
     * @param sourceFlag validate root node or not
     */
    private void validate(NodeGui source, boolean sourceFlag) {
        if (currentJob != null && !currentJob.isDone()) {
            currentJob.cancel(true);
        }
        currentJob = new ValidateWorker(nodes, source, sourceFlag);
        currentJob.execute();
    }

    /**
     * remove a node and revalidate the part of graph affected.
     * @param source source of the event
     */
    @Override
    public void sourceDeleted(Object source) {
        NodeGui srcNode = (NodeGui) source;
        NotificationManager.getInstance().info(srcNode.getName(), "Deleted");
        this.nodes.remove(srcNode);
        validate(srcNode, false);
    }

    /**
     * add a new connection and try to revalidate the part of the graph affected.
     * @param source source of the event
     */
    @Override
    public void connectionAdded(Object source) {
        NodeGui srcNode = (NodeGui) source;
        NotificationManager.getInstance().info(srcNode.getName(), "Connected");
        // Try to revalidate graph
        validate(srcNode, true);
    }

    @Override
    public void validateNode(Object node) {
        validate((NodeGui)node);
    }

    /**
     * Clean up current graph.
     */
    private void clearGraph() {
        for (NodeGui n: nodes) {
            n.removeNodeListener(this);
            this.graph.removeNode(n.getName());
        }
        this.nodes.clear();
    }

    /***
     * Initialize a simple graph composed by a Reader and a Writer
     */
    public void createEmptyGraph() {
        clearGraph();
        NodeGui n = newNode("Read");
        n.setPosition(90, 30);
        n = newNode("Write");
        n.setPosition(390, 30);
        NotificationManager.getInstance().info("Graph", "empty graph created.");
    }

    /**
     * Open an existing Graph.
     * @param selectedFile file to open
     */
    public void openGraph(File selectedFile) {
        clearGraph();
        // TODO implement XML loading
        NotificationManager.getInstance().processStart();
        GraphLoadWorker worker = new GraphLoadWorker(selectedFile);
        worker.execute();
    }

    /**
     * Load a list of nodes and re-validate current graph.
     * @param nodes nodes to be loaded
     */
    private void loadGraph(ArrayList<NodeGui> nodes) {
        for (NodeGui n: nodes) {
            n.addNodeListener(this);
            this.nodes.add(n);
            this.graph.addNode(n.getNode());
        }
        NotificationManager.getInstance().processEnd();
        NotificationManager.getInstance().info("Graph", "Loaded and ready");
        triggerEvent();
        validate();
    }

    /**
     * Save the current graph to file
     * @param f file where the graph will be saved
     * @return the success of the operation
     */
    public boolean saveGraph(File f) {
        NotificationManager.getInstance().processStart();
        XppDom presentationEl = new XppDom("applicationData");
        presentationEl.setAttribute("id", "Presentation");
        for (NodeGui n: nodes) {
            presentationEl.addChild(n.saveParameters());
        }
        graph.setAppData("Presentation", presentationEl);
        try {
            OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(f));
            GraphIO.write(graph, fileWriter);
            NotificationManager.getInstance().processEnd();
            NotificationManager.getInstance().info("Graph Saver", "file saved `"+f.getName()+"`");
            return true;
        } catch (FileNotFoundException e){
            NotificationManager.getInstance().processEnd();
            NotificationManager.getInstance().error("Graph Saver", "file saving error `" + e.getMessage() + "`");
        }
        return  false;
    }

    /**
     * Gets the default app context.
     * @return the default app context
     */
    public AppContext getContext() {
        return appContext;
    }

    /**
     * Worker used to validate the graph or a part of it.
     * @author Martino Ferrari (CS Group)
     */
    private class ValidateWorker extends  SwingWorker<Boolean, Object> {
        private final boolean validateSource;
        private final ArrayList<NodeGui> nodes;
        private final NodeGui source;


        /**
         * Initialize the graph with all the needed informations.
         * @param nodes current graph nodes
         * @param source source of the changes to validate
         * @param validateSource validate the source
         */
        ValidateWorker(ArrayList<NodeGui> nodes, NodeGui source, boolean validateSource) {
            this.nodes = new ArrayList<>(nodes);
            this.source = source;
            this.validateSource = validateSource;
        }

        @Override
        protected Boolean doInBackground() {
            NotificationManager.getInstance().processStart();
            NotificationManager.getInstance().info("Graph", "validation started");
            if (this.validateSource) {
                source.validate();
            }
            HashMap<Integer, HashSet<NodeGui>> orderedGraph = new HashMap<>();
            int total = 0;
            for (NodeGui n: nodes) {
                if (n != source) {
                    int dist = n.distance(source);
                    if (dist > 0) {
                        Integer key = dist;
                        if (!orderedGraph.containsKey(key)) {
                            orderedGraph.put(key, new HashSet<>());
                        }
                        orderedGraph.get(key).add(n);
                        total ++;
                    }
                }
            }
            NotificationManager.getInstance().processEnd();
            boolean status = true;
            if (source.getValidationStatus() == NodeGui.ValidationStatus.ERROR) {
                int i = 0;
                for (Integer key: orderedGraph.keySet()) {
                    for (NodeGui n: orderedGraph.get(key)) {
                        i ++;
                        NotificationManager.getInstance().progress((int)(i /(float) total) * 100);
                        n.invalidate();
                    }
                }
                status =false;
            } else {
                int i = 0;
                ArrayList<Integer> indexes = new ArrayList<>(orderedGraph.keySet());
                Collections.sort(indexes);
                for (Integer key: indexes) {
                    for (NodeGui n: orderedGraph.get(key)) {
                        i ++;
                        NotificationManager.getInstance().progress((int)(i /(float) total) * 100);
                        n.updateSources();
                        n.validate();
                    }
                }
            }

            NotificationManager.getInstance().processEnd();
            triggerEvent();
            return status;
        }
    }


    /**
     * Worker used to load a graph from file.
     *
     * @author Martino Ferrari (CS Group)
     */
    private class GraphLoadWorker extends SwingWorker<ArrayList<NodeGui>, Object> {
        private final File source;

        /**
         * Initialize the worker with the file to load.
         * @param file file to load
         */
        GraphLoadWorker(File file) {
            source = file;
        }

        @Override
        protected ArrayList<NodeGui> doInBackground() throws Exception {
            ArrayList<NodeGui> nodes = new ArrayList<>();
            AtomicReference<Graph> graph = new AtomicReference<>();
            try (InputStreamReader fileReader = new InputStreamReader(new FileInputStream(source))) {
                graph.set(GraphIO.read(fileReader));
            }

            if (graph.get() != null) {

                for (Node n : graph.get().getNodes()) {
                    if (simpleMetadata.containsKey(n.getOperatorName())) {
                        UnifiedMetadata meta = simpleMetadata.get(n.getOperatorName());
                        OperatorUI ui = OperatorUIRegistry.CreateOperatorUI(meta.getName());
                        Operator operator = GraphManager.getInstance().getOperator(meta);
                        assert operator != null;
                        NodeGui ng = new NodeGui(copyNode(n), getConfiguration(n), meta, ui, new OperatorContext(operator));
                        nodes.add(ng);
                    } else {
                        NotificationManager.getInstance().error("Graph",
                                                                "Operator '" + n.getOperatorName() +"' not known.");
                    }
                }
                // Load position
                final XppDom presentationXML = graph.get().getApplicationData("Presentation");
                if (presentationXML != null) {
                    for (XppDom el : presentationXML.getChildren()) {
                        if (el.getName().equals("node")) {
                            String id = el.getAttribute("id");
                            for (NodeGui n: nodes) {
                                if (n.getName().equals(id)) {
                                    n.loadParameters(el);
                                    break;
                                }
                            }
                        }
                    }
                }
                //Connect nodes
                for (Node n: graph.get().getNodes()) {
                    int index = 0;

                    NodeGui trgNode = null;
                    for (NodeGui ng: nodes) {
                        if (ng.getName().equals(n.getId())){
                            trgNode = ng;
                            break;
                        }
                    }
                    if (trgNode != null) {
                        for (NodeSource src: n.getSources()) {
                            String id = src.getSourceNodeId();
                            NodeGui srcNode = null;
                            for (NodeGui ns: nodes) {
                                if (ns.getName().equals(id)) {
                                    srcNode = ns;
                                    break;
                                }
                            }
                            if (srcNode != null) {
                                trgNode.addConnection(srcNode, index);
                                index ++;
                            }
                        }
                    }
                }
                loadGraph(nodes);
            }
            return nodes;
        }

        private Node copyNode(Node n) {
            Node copy = new Node(n.getId(), n.getOperatorName());
            copy.setConfiguration(n.getConfiguration());

            return copy;
        }
    }
}