/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.windows;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.util.CollapsibleItemsPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Experimental top component which displays information about selected pixel.
 */
@TopComponent.Description(
        preferredID = "PixelInfoTopComponent",
        iconBase = "org/esa/snap/gui/icons/PixelInfo16.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer",
                           openAtStartup = true,
                           position = 3
)
@ActionID(category = "Window", id = "org.esa.snap.gui.window.PixelInfoTopComponent")
@ActionReference(path = "Menu/Window/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PixelInfoTopComponentName",
        preferredID = "PixelInfoTopComponent"
)
@NbBundle.Messages({
                           "CTL_PixelInfoTopComponentName=Pixel Info",
                           "CTL_PixelInfoTopComponentDescription=Displays information about current pixel",
                   })
public final class PixelInfoTopComponent extends TopComponent implements LookupListener {

    private Lookup.Result<ProductSceneView> productSceneViewResult;
    private PixelPositionListener pixelPositionListener;
    private CollapsibleItemsPanel.Item<JTable> positionItem;
    private CollapsibleItemsPanel.Item<JTable> timeItem;
    private CollapsibleItemsPanel.Item<JTable> bandsItem;

    public PixelInfoTopComponent() {
        initComponents();
        setName(Bundle.CTL_PixelInfoTopComponentName());
        setToolTipText(Bundle.CTL_PixelInfoTopComponentDescription());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        pixelPositionListener = new MyPixelPositionListener();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        positionItem = CollapsibleItemsPanel.createTableItem("Position", 6, 2);
        timeItem = CollapsibleItemsPanel.createTableItem("Time", 2, 2);
        bandsItem = CollapsibleItemsPanel.createTableItem("Bands", 18, 3);
        CollapsibleItemsPanel collapsibleItemsPanel = new CollapsibleItemsPanel(
                positionItem,
                timeItem,
                bandsItem);

        JScrollPane scrollPane = new JScrollPane(collapsibleItemsPanel,
                                                 ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends ProductSceneView> productSceneViews2 = productSceneViewResult.allInstances();
        ProductSceneView productSceneView;
        if (!productSceneViews2.isEmpty()) {
            productSceneView = new ArrayList<>(productSceneViews2).get(0);
            productSceneView.removePixelPositionListener(pixelPositionListener);
            productSceneView.addPixelPositionListener(pixelPositionListener);
        }
    }

    @Override
    public void componentOpened() {
        productSceneViewResult = Utilities.actionsGlobalContext().lookupResult(ProductSceneView.class);
        productSceneViewResult.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        productSceneViewResult.removeLookupListener(this);
        productSceneViewResult = null;
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    private class MyPixelPositionListener implements PixelPositionListener {
        @Override
        public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel, boolean pixelPosValid, MouseEvent e) {
            JTable table = positionItem.getComponent();
            table.getModel().setValueAt("X", 0, 0);
            table.getModel().setValueAt(pixelX, 0, 1);
            table.getModel().setValueAt("Y", 1, 0);
            table.getModel().setValueAt(pixelY, 1, 1);
        }

        @Override
        public void pixelPosNotAvailable() {
            JTable table = positionItem.getComponent();
            table.getModel().setValueAt("X", 0, 0);
            table.getModel().setValueAt("NaN", 0, 1);
            table.getModel().setValueAt("Y", 1, 0);
            table.getModel().setValueAt("NaN", 1, 1);

        }
    }
}
