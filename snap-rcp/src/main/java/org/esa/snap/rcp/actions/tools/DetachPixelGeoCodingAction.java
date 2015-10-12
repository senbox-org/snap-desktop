/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.actions.tools;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Tools",
        id = "DetachPixelGeoCodingAction"
)
@ActionRegistration(
        displayName = "#CTL_DetachPixelGeoCodingActionText",
        popupText = "#CTL_DetachPixelGeoCodingActionText",
        lazy = false
)
@ActionReference(path = "Menu/Tools", position = 220)
@Messages({
        "CTL_DetachPixelGeoCodingActionText=Detach Pixel Geo-Coding...",
        "CTL_DetachPixelGeoCodingDialogTitle=Detach Pixel Geo-Coding"

})
public class DetachPixelGeoCodingAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup lookup;

    public DetachPixelGeoCodingAction() {
        this(Utilities.actionsGlobalContext());
    }

    public DetachPixelGeoCodingAction(Lookup lkp) {
        super(Bundle.CTL_DetachPixelGeoCodingActionText());
        this.lookup = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.SHORT_DESCRIPTION, "Detach a pixel based geo-coding from the selected product");
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        detachPixelGeoCoding(lookup.lookup(ProductNode.class).getProduct());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DetachPixelGeoCodingAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    private static void detachPixelGeoCoding(final Product product) {

        final SwingWorker swingWorker = new SwingWorker<Throwable, Object>() {
            @Override
            protected Throwable doInBackground() throws Exception {
                try {
                    GeoCoding geoCoding = product.getSceneGeoCoding();
                    if (geoCoding instanceof BasicPixelGeoCoding) {
                        final BasicPixelGeoCoding pixelGeoCoding = (BasicPixelGeoCoding) geoCoding;
                        final GeoCoding delegate = pixelGeoCoding.getPixelPosEstimator();
                        product.setSceneGeoCoding(delegate);
                        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
                        if (undoManager != null) {
                            undoManager.addEdit(new UndoableDetachGeoCoding<>(product, pixelGeoCoding));
                        }
                    }
                } catch (Throwable e) {
                    return e;
                }
                return null;
            }

            @Override
            public void done() {
                try {
                    Throwable value;
                    try {
                        value = get();
                    } catch (Exception e) {
                        value = e;
                    }
                    String dialogTitle = Bundle.CTL_DetachPixelGeoCodingDialogTitle();
                    if (value != null) {
                        SnapDialogs.showError(dialogTitle, "An internal error occurred:\n" + value.getMessage());
                    } else {
                        SnapDialogs.showInformation(dialogTitle, "Pixel geo-coding has been detached.", null);
                    }
                } finally {
                    UIUtils.setRootFrameDefaultCursor(SnapApp.getDefault().getMainFrame());
                }
            }
        };

        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        swingWorker.execute();
    }

    private void setEnableState() {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        boolean state = false;
        if (productNode != null) {
            Product product = productNode.getProduct();
            state = product.getSceneGeoCoding() instanceof BasicPixelGeoCoding;
        }
        setEnabled(state);
    }

    private static class UndoableDetachGeoCoding<T extends BasicPixelGeoCoding> extends AbstractUndoableEdit {

        private Product product;
        private T pixelGeoCoding;

        public UndoableDetachGeoCoding(Product product, T pixelGeoCoding) {
            Assert.notNull(product, "product");
            Assert.notNull(pixelGeoCoding, "pixelGeoCoding");
            this.product = product;
            this.pixelGeoCoding = pixelGeoCoding;
        }


        @Override
        public String getPresentationName() {
            return Bundle.CTL_DetachPixelGeoCodingDialogTitle();
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            product.setSceneGeoCoding(pixelGeoCoding);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            if (product.getSceneGeoCoding() == pixelGeoCoding) {
                product.setSceneGeoCoding(pixelGeoCoding.getPixelPosEstimator());
            }
        }

        @Override
        public void die() {
            pixelGeoCoding = null;
            product = null;
        }
    }

}
