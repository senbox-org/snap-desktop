/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.esa.beam.framework.datamodel.ProductNode;
import org.openide.util.lookup.Lookups;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.Action;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "ProductNodeTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(
        mode = "editor",
        openAtStartup = false)
public class ProductNodeTopComponent extends TopComponent {

    private final ProductNode productNode;
    private final Container view;

    public ProductNodeTopComponent(ProductNode productNode, Container view) {
        super(Lookups.fixed(productNode));
        this.productNode = productNode;
        this.view = view;
        initComponents();
        setName(productNode.getName());
        setDisplayName(productNode.getName());
        setToolTipText(productNode.getProduct().getName() + " - " + productNode.getName());
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);
    }

    @Override
    public Action[] getActions() {
        ArrayList<Action> actions = new ArrayList<>(Arrays.asList(super.getActions()));
        actions.add(null);
        actions.addAll(Arrays.asList(WorkspaceTopComponent.getInstance().getExtraWorkspaceActions(this)));
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public List<Mode> availableModes(List<Mode> modes) {
        return Arrays.asList(WindowManager.getDefault().findMode("editor"));
    }

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening       
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

}
