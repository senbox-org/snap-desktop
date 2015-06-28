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
package org.esa.snap.rcp.session;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductManager;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.product.ProductNodeView;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
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
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;


/**
 * Saves a VISAT session.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */


@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.session.SaveSessionAction"
)
@ActionRegistration(
        displayName = "#CTL_SaveSessionAction_MenuText",
        lazy = false
)
@ActionReference(path = "Menu/File", position = 50)

@NbBundle.Messages({
        "CTL_SaveSessionAction_MenuText=Save Session",
        "CTL_SaveSessionAction_ShortDescription=Save the current SNAP session."
})
public class SaveSessionAction extends AbstractAction implements ContextAwareAction, LookupListener {

    public static final String ID = "saveSession";
    private static final String TITLE = "Save Session As";
    private final Lookup.Result<ProductSceneView> result;
    private final Lookup lookup;
    private ProductManager productManager;


    public SaveSessionAction() {
        this(Utilities.actionsGlobalContext());
    }

    public SaveSessionAction(Lookup lookup) {
        super(Bundle.CTL_SaveSessionAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);

    }


    @Override
    public final void actionPerformed(ActionEvent event) {
        saveSession(false);
    }


    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new SaveSessionAction(actionContext);
    }


    @Override
    public void resultChanged(LookupEvent ev) {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        setEnabled(productNode != null);
    }

    public void saveSession(boolean saveAs) {
        final VisatApp app = VisatApp.getApp();

        File sessionFile = app.getSessionFile();
        if (sessionFile == null || saveAs) {
            sessionFile = SnapDialogs.requestFileForSave(TITLE, false,
                    OpenSessionAction.getSessionFileFilter(),
                    OpenSessionAction.getSessionFileFilter().getDefaultExtension(),
                    sessionFile != null ? sessionFile.getName() : System.getProperty("user.name", "noname"),
                    null,
                    OpenSessionAction.LAST_SESSION_DIR_KEY);

            if (sessionFile == null) {
                return;
            }
        }

        if (!saveProductsOrLetItBe(sessionFile)) {
            return;
        }

        app.setSessionFile(sessionFile);
        try {
            final Session session = createSession(app);
            SessionIO.getInstance().writeSession(session, sessionFile);
            SnapDialogs.showInformation(TITLE, "Session saved.", null);
        } catch (Exception e) {
            e.printStackTrace();
            SnapDialogs.showError(TITLE, e.getMessage());
        } finally {
//            app.updateState(); // to update menu entries e.g. 'Close Session'
        }
    }

    private boolean saveProductsOrLetItBe(File sessionFile) {
        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();

        for (Product product : products) {
            if (product.getFileLocation() == null) {
                String message = MessageFormat.format(
                        "The following product has not been saved yet:\n" +
                                "{0}.\n" +
                                "Do you want to save it now?\n\n" +
                                "Note: If you select 'No', the session cannot be saved.",
                        product.getDisplayName());
                // Here: No == Cancel, its because we need a file location in the session XML
                SnapDialogs.Answer answer = SnapDialogs.requestDecision(TITLE, message, false, null);
                if (answer == SnapDialogs.Answer.YES) {
                    File sessionDir = sessionFile.getAbsoluteFile().getParentFile();
                    product.setFileLocation(new File(sessionDir, product.getName() + ".dim"));
                    VisatApp.getApp().saveProduct(product);
                } else {
                    return false;
                }
            }
        }

        for (Product product : products) {
            if (product.isModified()) {
                String message = MessageFormat.format(
                        "The following product has been modified:\n" +
                                "{0}.\n" +
                                "Do you want to save it now?\n\n" +
                                "Note: It is recommended to save the product in order to \n" +
                                "fully restore the session later.",
                        product.getDisplayName());
                // Here: Yes, No + Cancel, its because we have file location for the session XML
                SnapDialogs.Answer answer = SnapDialogs.requestDecision(TITLE, message, false, null);
                if (answer == SnapDialogs.Answer.YES) {
                    VisatApp.getApp().saveProduct(product);
                } else if (answer == SnapDialogs.Answer.YES) {
                    return false;
                }
            }
        }

        return true;
    }

    private Session createSession(VisatApp app) {
        ArrayList<ProductNodeView> nodeViews = new ArrayList<ProductNodeView>();
//        final JInternalFrame[] internalFrames = app.getAllInternalFrames();
//        for (JInternalFrame internalFrame : internalFrames) {
//            final Container contentPane = internalFrame.getContentPane();
//            if (contentPane instanceof ProductNodeView) {
//                nodeViews.add((ProductNodeView) contentPane);
//            }
//        }
        return new Session(app.getSessionFile().getParentFile().toURI(),
                SnapApp.getDefault().getProductManager().getProducts(),
                nodeViews.toArray(new ProductNodeView[nodeViews.size()]));
    }


}
