/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.windows;

import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.nodes.ProductGroupNode;
import org.esa.snap.gui.util.TestProducts;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * The product explorer tool window.
 *
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "ProductExplorerTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "explorer",
        openAtStartup = true,
        position = 1)
@ActionID(category = "Window", id = "org.esa.snap.gui.window.ProductExplorerTopComponent")
@ActionReference(path = "Menu/Window/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductExplorerTopComponentName",
        preferredID = "ProductExplorerTopComponent"
)
@NbBundle.Messages({
        "CTL_ProductExplorerTopComponentName=Product Explorer",
        "CTL_ProductExplorerTopComponentDescription=Lists all open products",
})
public class ProductExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private static final Logger LOG = Logger.getLogger(ProductExplorerTopComponent.class.getName());

    private final ExplorerManager explorerManager = new ExplorerManager();

    public ProductExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_ProductExplorerTopComponentName());
        setToolTipText(Bundle.CTL_ProductExplorerTopComponentDescription());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        // 1. Add an explorer view, in this case BeanTreeView:
        final BeanTreeView treeView = new BeanTreeView();
        treeView.setRootVisible(false);
        add(treeView, BorderLayout.CENTER);
        // 2. Create a node hierarchy:
        Product[] products = TestProducts.createProducts();
        for (Product product : products) {
            SnapApp.getDefault().getProductManager().addProduct(product);
        }
        Node rootNode = new ProductGroupNode(SnapApp.getDefault().getProductManager());
        // 3. Set the root of the node hierarchy on the ExplorerManager:
        explorerManager.setRootContext(rootNode);

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(explorerManager));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(explorerManager));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(explorerManager));
        map.put("delete", ExplorerUtils.actionDelete(explorerManager, true));
        associateLookup(ExplorerUtils.createLookup(explorerManager, map));

        final InputOutput io = IOProvider.getDefault().getIO("Explorer Selection", true);
        explorerManager.addPropertyChangeListener(new PropertyChangeListener() {

            final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("selectedNodes")) {
                    Node[] selectedNode = (Node[]) evt.getNewValue();
                    for (Node node : selectedNode) {
                        String timeText = timeFormat.format(System.currentTimeMillis());
                        io.getOut().println(timeText + ": " + node.getDisplayName());
                    }
                }
            }
        });
    }

    @Override
    public UndoRedo getUndoRedo() {
        Node[] activatedNodes = getActivatedNodes();
        LOG.info("getUndoRedo: activatedNodes = " + Arrays.toString(activatedNodes));
        return super.getUndoRedo();
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    @Override
    public void componentOpened() {
        //  add custom code on component opening
    }

    @Override
    public void componentClosed() {
        //  add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        //  store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        //  read your settings according to their version
    }

}
