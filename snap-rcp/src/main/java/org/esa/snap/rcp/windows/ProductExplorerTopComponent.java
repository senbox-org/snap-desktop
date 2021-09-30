/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.windows;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DummyProductBuilder;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.OpenProductAction;
import org.esa.snap.rcp.nodes.ProductGroupNode;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;

import javax.swing.ActionMap;
import javax.swing.text.DefaultEditorKit;
import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * The product explorer tool window.
 *
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "ProductExplorerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/RsProduct16.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = PackageDefaults.PRODUCT_EXPLORER_MODE,
        openAtStartup = PackageDefaults.PRODUCT_EXPLORER_OPEN,
        position = PackageDefaults.PRODUCT_EXPLORER_POSITION)
@ActionID(category = "Window", id = "org.esa.snap.rcp.window.ProductExplorerTopComponent")
@ActionReference(path = "Menu/View/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductExplorerTopComponentName",
        preferredID = "ProductExplorerTopComponent"
)
@NbBundle.Messages({
        "CTL_ProductExplorerTopComponentName=" + PackageDefaults.PRODUCT_EXPLORER_NAME,
        "CTL_ProductExplorerTopComponentDescription=Lists all open products",
})
public class ProductExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private final ExplorerManager explorerManager = new ExplorerManager();
    private DndBeanTreeView treeView;

    public ProductExplorerTopComponent() {
        initComponents();
        setName(PackageDefaults.PRODUCT_EXPLORER_NAME);
        setDisplayName(Bundle.CTL_ProductExplorerTopComponentName());
        setToolTipText(Bundle.CTL_ProductExplorerTopComponentDescription());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        // 1. Add an explorer view, in this case BeanTreeView:
        // DndBeanTreeView has the purpose to enable Drag&Drop an the tree
        treeView = new DndBeanTreeView();
        treeView.setRootVisible(false);
        add(treeView, BorderLayout.CENTER);
        // 2. Create a node hierarchy:
        if (Config.instance().preferences().getBoolean("snap.debug.loadTestProducts", false)) {
            //Product[] products = TestProducts.createProducts();
            SystemUtils.LOG.info("Loading test products...");
            Product[] products = DummyProductBuilder.createTestProducts();
            for (Product product : products) {
                SystemUtils.LOG.info("Loading test product " + product.getName());
                SnapApp.getDefault().getProductManager().addProduct(product);
            }
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

    public boolean isNodeExpanded(Node node) {
        return treeView.isExpanded(node);
    }


    public void expandNode(Node node) {
        treeView.expandNode(node);
    }

    private static class DndBeanTreeView extends BeanTreeView {

        public DndBeanTreeView() {
            tree.setDropTarget(new DropTarget(tree, new ProductExplorerDropTarget()));
        }

    }

    private static class ProductExplorerDropTarget extends DropTargetAdapter {

        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                final Transferable transferable = dtde.getTransferable();
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    final List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (fileList.size() > 0) {
                        final OpenProductAction open = new OpenProductAction();
                        open.setFiles(fileList.toArray(new File[fileList.size()]));
                        dtde.dropComplete(Boolean.TRUE.equals(open.execute()));
                    }
                } else {
                    dtde.rejectDrop();
                }
            } catch (UnsupportedFlavorException | IOException e) {
                SystemUtils.LOG.log(Level.SEVERE, "Exception during drag-and-drop operation", e);
                dtde.rejectDrop();
            }
        }

    }


}
