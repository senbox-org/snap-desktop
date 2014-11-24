/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.esa.beam.framework.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.Collection;

/**
 * Experimental top component which displays infos about selected node(s).
 */
@TopComponent.Description(
        preferredID = "NodeInfoTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 2)
@ActionID(category = "Window", id = "org.esa.snap.gui.window.NodeInfoTopComponent")
@ActionReference(path = "Menu/View/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_NodeInfoTopComponentName",
        preferredID = "NodeInfoTopComponent"
)
@NbBundle.Messages({
                           "CTL_NodeInfoTopComponentName=Node Info",
                           "CTL_NodeInfoTopComponentDescription=Displays info about active node(s)",
                   })
public final class NodeInfoTopComponent extends TopComponent implements LookupListener {

    private JLabel infoLabel;
    private JTable infoTable;
    private Lookup.Result<ProductNode> result;

    public NodeInfoTopComponent() {
        initComponents();
        setName(Bundle.CTL_NodeInfoTopComponentName());
        setToolTipText(Bundle.CTL_NodeInfoTopComponentDescription());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        infoLabel = new JLabel("");
        infoTable = new JTable();
        add(infoLabel, BorderLayout.NORTH);
        add(new JScrollPane(infoTable), BorderLayout.CENTER);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends ProductNode> nodes = result.allInstances();
        if (!nodes.isEmpty()) {
            String[][] data = new String[nodes.size()][2];
            int i = 0;
            for (ProductNode allNode : nodes) {
                data[i][0] = allNode.getName();
                data[i][1] = allNode.getClass().getSimpleName();
                i++;
            }
            infoLabel.setText(nodes.size() + " node(s) selected:");
            infoTable.setModel(new DefaultTableModel(data, new String[]{"Name", "Type"}));
        } else {
            infoLabel.setText("No nodes selected.");
            infoTable.setModel(new DefaultTableModel(new Object[0][2], new String[]{"Name", "Type"}));
        }
    }

    @Override
    public void componentOpened() {
        result = Utilities.actionsGlobalContext().lookupResult(ProductNode.class);
        result.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }
}
