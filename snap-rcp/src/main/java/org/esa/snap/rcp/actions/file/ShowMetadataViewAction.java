/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.product.ProductMetadataView;
import org.esa.snap.framework.ui.product.metadata.MetadataView;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.MetadataViewTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * This action opens an Metadata View of the currently selected Metadata Node
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@ActionID(
        category = "File",
        id = "ShowMetadataViewAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowMetadataViewActionName"
)
@ActionReferences({
        @ActionReference(path = "Context/Product/MetadataAttribute", position = 110),
        @ActionReference(path = "Menu/View", position = 110)
})
@NbBundle.Messages("CTL_ShowMetadataViewActionName=Open Metadata View")
//public class ShowMetadataViewAction extends ExecCommand {
public class ShowMetadataViewAction extends AbstractAction implements ContextAwareAction, LookupListener {

//    public static String ID = "showMetadataView";
    private Lookup lookup;
    private final Lookup.Result<ProductNode> result;

    public ShowMetadataViewAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ShowMetadataViewAction(Lookup lookup) {
        this.lookup = lookup;
        result = lookup.lookupResult(ProductNode.class);
        result.addLookupListener(
                WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
//        setHelpId(org.esa.snap.rcp.actions.vector.Bundle.CTL_ImportVectorDataNodeFromShapefileActionHelp());
        putValue(Action.NAME, org.esa.snap.rcp.actions.file.Bundle.CTL_ShowMetadataViewActionName());
//        putValue(Action.SHORT_DESCRIPTION, org.esa.snap.rcp.actions.vector.Bundle.CTL_ImportVectorDataNodeFromShapefileActionName());
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ShowMetadataViewAction(lookup);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
    }

    private void setEnableState() {
//        boolean state = false;
        ProductNode productNode = lookup.lookup(ProductNode.class);
//        if (productNode != null) {
//            Product product = productNode.getProduct();
//            state = product != null && product.getGeoCoding() != null;
//        }
        setEnabled(productNode != null && productNode instanceof MetadataElement);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//
//    }
//    @Override
//    public void actionPerformed(final CommandEvent event) {
//        final VisatApp visatApp = VisatApp.getApp();
        openMetadataView((MetadataElement) SnapApp.getDefault().getSelectedProductNode());
    }
    
//    @Override
//    public void updateState(final CommandEvent event) {
//        setEnabled(SnapApp.getDefault().getSelectedProductNode() instanceof MetadataElement);
//    }

//    public ProductMetadataView openMetadataView(final MetadataElement element) {
    public void openMetadataView(final MetadataElement element) {
        MetadataView metadataView = new MetadataView(element);
//        ProductMetadataView metadataView = new ProductMetadataView(element);
        openDocumentWindow(metadataView);
//        openInternalFrame(metadataView);
//        return metadataView;
    }

//    private MetadataViewTopComponent openDocumentWindow(final ProductMetadataView view) {
    private MetadataViewTopComponent openDocumentWindow(final MetadataView view) {
//        return openDocumentWindow(view, true);
//    }
//
//    private ProductSceneViewTopComponent openDocumentWindow(final ProductMetadataView view, boolean configureByPreferences) {
//        if (configureByPreferences) {
//            view.setLayerProperties(SnapApp.getDefault().getCompatiblePreferences());
//        }

//        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(view.getProduct());
//        ProductSceneViewTopComponent productSceneViewWindow = new ProductSceneViewTopComponent(view, undoManager);
        final MetadataViewTopComponent metadataViewTopComponent = new MetadataViewTopComponent(view);

//        DocumentWindowManager.getDefault().openWindow(productSceneViewWindow);
        DocumentWindowManager.getDefault().openWindow(metadataViewTopComponent);
//        productSceneViewWindow.requestSelected();
        metadataViewTopComponent.requestSelected();

//        return productSceneViewWindow;
        return metadataViewTopComponent;
    }

//    public JInternalFrame openInternalFrame(ProductMetadataView metadataView) {
//        final SnapApp snapApp = SnapApp.getDefault();
//        snapApp.setStatusBarMessage("Creating metadata view...");
//        snapApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));


//        JInternalFrame metadataFrame = null;
//        try {
//            metadataView.setCommandUIFactory(visatApp.getCommandUIFactory());
//            final Icon icon = UIUtils.loadImageIcon("icons/RsMetaData16.gif");
//            final MetadataElement element = metadataView.getMetadataElement();
//            metadataFrame = visatApp.createInternalFrame(element.getDisplayName(),
//                                                         icon,
//                                                         metadataView, null,false);
//            final Product product = metadataView.getProduct();
//            final JInternalFrame internalFrame = metadataFrame;
//            product.addProductNodeListener(new ProductNodeListenerAdapter() {
//                @Override
//                public void nodeChanged(final ProductNodeEvent event) {
//                    if (event.getSourceNode() == element &&
//                            event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
//                        internalFrame.setTitle(element.getDisplayName());
//                    }
//                }
//            });
//            updateState();
//        } catch (Exception e) {
//            visatApp.handleUnknownException(e);
//        }

//        visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
//        visatApp.clearStatusBarMessage();

//        return metadataFrame;
//    }
}
