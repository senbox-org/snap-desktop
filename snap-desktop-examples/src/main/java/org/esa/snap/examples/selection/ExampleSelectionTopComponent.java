package org.esa.snap.examples.selection;

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
 * Example top component which displays infos about selected node(s).
 */
@TopComponent.Description(
        preferredID = "ExampleSelectionTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false, position = 2)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ExampleSelectionTopComponentName",
        preferredID = "ExampleSelectionTopComponent"
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.window.ExampleSelectionTopComponent")
@ActionReference(path = "Menu/View/Tool Windows/Examples", position = 0)
@NbBundle.Messages({
        "CTL_ExampleSelectionTopComponentName=Example Selection",
        "CTL_ExampleSelectionTopComponentDescription=Displays info about global selection",
})
public final class ExampleSelectionTopComponent extends TopComponent implements LookupListener {

    private JLabel infoLabel;
    private JTable infoTable;
    private Lookup.Result<Object> result;

    public ExampleSelectionTopComponent() {
        initComponents();
        setName(Bundle.CTL_ExampleSelectionTopComponentName());
        setToolTipText(Bundle.CTL_ExampleSelectionTopComponentDescription());
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
        Collection<?> selectedObjects = result.allInstances();
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
