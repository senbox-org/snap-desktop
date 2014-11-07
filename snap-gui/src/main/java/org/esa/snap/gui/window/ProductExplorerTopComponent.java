/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.esa.snap.gui.node.ProductChildFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;

import javax.swing.ActionMap;
import javax.swing.text.DefaultEditorKit;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "ProductExplorerTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "explorer",
        openAtStartup = true)
@ActionID(category = "Window", id = "org.snap.gui.ProductExplorerTopComponent")
@ActionReference(path = "Menu/View/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductExplorerAction",
        preferredID = "ProductExplorerTopComponent"
)
@Messages({
    "CTL_ProductExplorerAction=Product Explorer",
    "CTL_ProductExplorerTopComponent=Product Explorer",
    "HINT_ProductExplorerTopComponent=This is a Product Explorer window",
    "CTL_RootDisplayName=Active products",})
public class ProductExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private final ExplorerManager manager = new ExplorerManager();

    public ProductExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_ProductExplorerTopComponent());
        setToolTipText(Bundle.HINT_ProductExplorerTopComponent());
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        // 1. Add an explorer view, in this case BeanTreeView:
        final BeanTreeView treeView = new BeanTreeView();
        treeView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //Find the ExplorerManager for this explorer view:
                ExplorerManager mgr = ExplorerManager.find(treeView);
                //Get the selected node from the ExplorerManager:
                String selectedNode = mgr.getSelectedNodes()[0].getDisplayName();
                //Get the pressed key from the event:
                String pressedKey = KeyEvent.getKeyText(e.getKeyCode());
                //Put a message in the status bar:
                StatusDisplayer.getDefault().setStatusText(selectedNode
                        + " is being pressed by the " + pressedKey + " key!");
            }
        });
        add(new BeanTreeView(), BorderLayout.CENTER);
        // 2. Create a node hierarchy:
        Children productChildren = Children.create(ProductChildFactory.getInstance(), true);
        Node rootNode = new AbstractNode(productChildren);
        rootNode.setDisplayName(Bundle.CTL_RootDisplayName());
        // 3. Set the root of the node hierarchy on the ExplorerManager:
        manager.setRootContext(rootNode);

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(manager));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(manager));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(manager));
        map.put("delete", ExplorerUtils.actionDelete(manager, true)); // or false
        associateLookup(ExplorerUtils.createLookup(manager, map));

        final InputOutput io = IOProvider.getDefault().getIO("Selected Nodes", true);
        
        manager.addPropertyChangeListener(new PropertyChangeListener() {
            
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
        return manager;
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
