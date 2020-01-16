package org.esa.snap.graphbuilder.ui.components.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIRegistry;
import org.esa.snap.graphbuilder.ui.components.graph.NodeGui;

public class OperatorManager {
    
    public class SimplifiedMetadata {
        private String name;
        private final String name_lower;
        private final String description;
        private final String category;
        private final String category_lower;

        public SimplifiedMetadata(final OperatorMetadata metadata) {
            name = metadata.label();
            if (name == null || name.length() == 0) {
                name = metadata.alias();
            }

            description = metadata.description();
            category = metadata.category();

            category_lower = category.toLowerCase();
            name_lower = name.toLowerCase();
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }

        public boolean find(final String string) {
            if (name_lower.contains(string)) {
                return true;
            }
            if (category_lower.contains(string)) {
                return true;
            }
            return false;
        }
    }

    private final GPF gpf;
    private final OperatorSpiRegistry opSpiRegistry;

    private final ArrayList<OperatorMetadata> metadatas = new ArrayList<>();
    private final HashMap<String, SimplifiedMetadata> simpleMetadatas = new HashMap<>();

    private final ArrayList<Node> nodes = new ArrayList<>();

    public OperatorManager() {
        gpf = GPF.getDefaultInstance();
        opSpiRegistry = gpf.getOperatorSpiRegistry();
        for (final OperatorSpi opSpi : opSpiRegistry.getOperatorSpis()) {
            if (!opSpi.getOperatorDescriptor().isInternal()) {
                final OperatorMetadata operatorMetadata = opSpi.getOperatorClass()
                        .getAnnotation(OperatorMetadata.class);
                metadatas.add(operatorMetadata);
                simpleMetadatas.put(operatorMetadata.alias(), new SimplifiedMetadata(operatorMetadata));
            }
        }
    }

    public Collection<SimplifiedMetadata> getSimplifiedMetadatas() {
        return simpleMetadatas.values();
    }

    public ArrayList<OperatorMetadata> getMetadata() {
        return metadatas;
    }

    private Node createNode(final String operator) {
        final Node newNode = new Node(id(operator), operator);

        final XppDomElement parameters = new XppDomElement("parameters");
        newNode.setConfiguration(parameters);

        this.nodes.add(newNode);
        return newNode;
    }

    public Map<String, Object> getConfiguration(final Node node) {
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

    private String id(final String opName) {
        final String res = opName + " ";
        int counter = 0;
        for (final Node n : nodes) {
            if (n.getId().startsWith(res)) {
                counter++;
            }
        }

        return res + counter;
    }

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


    public JMenu createOperatorMenu(ActionListener listener) {
        JMenu addMenu = new JMenu("Add");
        for (SimplifiedMetadata metadata: getSimplifiedMetadatas()) {
            JMenu menu = getCategoryMenu(addMenu, metadata.getCategory());
            JMenuItem item = new JMenuItem(metadata.getName());
            item.setHorizontalTextPosition(JMenuItem.RIGHT);
            item.addActionListener(listener);
            menu.add(item);
        }
        return addMenu;
    }

    public NodeGui newNode(String opName){
        return newNode(simpleMetadatas.get(opName));
    }

    public NodeGui newNode(SimplifiedMetadata metadata) {
        OperatorUI ui = OperatorUIRegistry.CreateOperatorUI(metadata.getName());
        Node node = createNode(metadata.getName());
        return new NodeGui(node, getConfiguration(node), metadata, ui);
    }



}