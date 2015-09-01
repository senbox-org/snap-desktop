/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.developer;

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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.Collection;

/**
 * Displays the state of SNAP's global action context.
 *
 * @author Norman Fomferra
 */
@TopComponent.Description(
        preferredID = "ActionContextMonitorTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false, position = 100, roles={"developer"})
@ActionID(category = "Window", id = "org.esa.snap.rcp.developer.ActionContextMonitorTopComponent")
@ActionReference(path = "Menu/View/Tool Windows/Developer", position = 10)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ActionContextMonitorTopComponentName",
        preferredID = "ActionContextMonitorTopComponent"
)
@NbBundle.Messages({
        "CTL_ActionContextMonitorTopComponentName=Action Context Monitor",
        "CTL_ActionContextMonitorTopComponentDescription=Displays the state of SNAP's global action context.",
})
public final class ActionContextMonitorTopComponent extends TopComponent implements LookupListener {

    private JLabel infoLabel;
    private JTable infoTable;
    private Lookup.Result<Object> result;

    public ActionContextMonitorTopComponent() {
        initComponents();
        setName(Bundle.CTL_ActionContextMonitorTopComponentName());
        setToolTipText(Bundle.CTL_ActionContextMonitorTopComponentDescription());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
    }

    private void initComponents() {
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        infoLabel = new JLabel("");
        infoTable = new JTable();
        add(infoLabel, BorderLayout.NORTH);
        add(new JScrollPane(infoTable), BorderLayout.CENTER);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends Object> selectedObjects = result.allInstances();
        if (!selectedObjects.isEmpty()) {
            Object[][] data = new Object[selectedObjects.size()][3];
            int i = 0;
            for (Object selectedObject : selectedObjects) {
                data[i][0] = i + 1;
                data[i][1] = selectedObject.getClass().getSimpleName();
                data[i][2] = String.valueOf(selectedObject);
                i++;
            }
            infoLabel.setText(selectedObjects.size() + " objects(s) selected:");
            infoTable.setModel(new DefaultTableModel(data, new String[]{"#", "Type", "Name"}));
            infoTable.getColumnModel().getColumn(0).setPreferredWidth(20);
            infoTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        } else {
            infoLabel.setText("No objects selected.");
            infoTable.setModel(new DefaultTableModel());
        }
    }

    @Override
    public void componentOpened() {
        result = Utilities.actionsGlobalContext().lookupResult(Object.class);
        result.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
    }
}
