package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A property editor for selected rasters.
 *
 * @author Norman Fomferra
 */
class RastersPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor, VetoableChangeListener {
    static final String SEPARATOR = ",";

    private RasterDataNode[] validNodes;
    private CustomEditor customEditor;

    public RastersPropertyEditor(RasterDataNode... validNodes) {
        this.validNodes = validNodes;
    }

    @Override
    public String getAsText() {
        RasterDataNode[] value = (RasterDataNode[]) getValue();
        if (value == null) {
            return "";
        }
        return Stream.of(value).map(ProductNode::getName).collect(Collectors.joining(SEPARATOR + " "));
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().isEmpty()) {
            setValue(new RasterDataNode[0]);
            return;
        }
        HashMap<String, RasterDataNode> validNames = getValidNodeNames();
        ArrayList<RasterDataNode> nodes = new ArrayList<>();
        String[] names = text.split(SEPARATOR);
        for (String name : names) {
            name = name.trim();
            RasterDataNode node = validNames.get(name);
            if (node == null) {
                throw new IllegalArgumentException("Illegal entry '" + name + "'!");
            }
            nodes.add(node);
        }
        setValue(nodes.toArray(new RasterDataNode[nodes.size()]));
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        customEditor = new CustomEditor();
        //System.out.println(":::::::::::::::: getCustomEditor: customEditor = " + customEditor);
        customEditor.setSelectedNodes((RasterDataNode[]) getValue());
        return customEditor;
    }

    @Override
    public void attachEnv(PropertyEnv propertyEnv) {
        //System.out.println(":::::::::::::::: attachEnv: propertyEnv = " + propertyEnv);
        propertyEnv.addVetoableChangeListener(this);
        propertyEnv.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
    }

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        //System.out.println(":::::::::::::::: vetoableChange: evt = " + evt);
        //System.out.println(":::::::::::::::: vetoableChange: customEditor = " + customEditor);
        if (PropertyEnv.PROP_STATE.equals(evt.getPropertyName())) {
            if (customEditor != null) {
                RasterDataNode[] selectedNodes = customEditor.getSelectedNodes();
                String collect = Stream.of(selectedNodes).map(n -> n.getName()).collect(Collectors.joining("; "));
                //System.out.println(":::::::::::::::: vetoableChange: collect = " + collect);
                if (PropertyEnv.STATE_VALID.equals(evt.getNewValue())) {
                    try {
                        setValue(selectedNodes);
                    } catch (IllegalArgumentException e) {
                        throw new PropertyVetoException(e.getMessage(), evt);
                    }
                }
                customEditor = null;
            }
        }
    }

    private HashMap<String, RasterDataNode> getValidNodeNames() {
        HashMap<String, RasterDataNode> validNames = new HashMap<>();
        for (RasterDataNode node : validNodes) {
            validNames.put(node.getName(), node);
        }
        return validNames;
    }


    private class CustomEditor extends JPanel {

        private final JList<String> list;
        private final DefaultListModel<String> listModel;

        public CustomEditor() {
            super(new BorderLayout(4, 4));
            setBorder(new EmptyBorder(8, 8, 8, 8));

            listModel = new DefaultListModel<>();
            list = new JList<>(listModel);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane scrollPane = new JScrollPane(list);

            add(new JLabel("Available rasters:"), BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public RasterDataNode[] getSelectedNodes() throws PropertyVetoException {
            HashMap<String, RasterDataNode> validNodeNames = getValidNodeNames();
            ArrayList<RasterDataNode> nodes = new ArrayList<>();
            int[] selectedIndices = list.getSelectedIndices();
            for (int index : selectedIndices) {
                String name = listModel.get(index);
                RasterDataNode node = validNodeNames.get(name);
                if (node != null) {
                    nodes.add(node);
                }
            }
            return nodes.toArray(new RasterDataNode[nodes.size()]);
        }

        public void setSelectedNodes(RasterDataNode[] selectedNodes) {

            HashMap<RasterDataNode, Integer> indexMap = new HashMap<>();

            listModel.clear();
            for (int i = 0; i < validNodes.length; i++) {
                RasterDataNode node = validNodes[i];
                listModel.addElement(node.getName());
                indexMap.put(node, i);
            }

            ArrayList<Integer> indexList = new ArrayList<>();
            for (RasterDataNode selectedNode : selectedNodes) {
                Integer index = indexMap.get(selectedNode);
                if (index != null) {
                    indexList.add(index);
                }
            }

            int[] indices = new int[indexList.size()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = indexList.get(i);
            }
            list.setSelectedIndices(indices);
        }
    }
}
