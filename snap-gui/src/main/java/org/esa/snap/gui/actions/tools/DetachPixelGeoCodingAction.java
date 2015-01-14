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

package org.esa.snap.gui.actions.tools;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;

import org.esa.beam.framework.datamodel.BasicPixelGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.snap.gui.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

@ActionID(
        category = "Tools",
        id = "org.esa.snap.gui.actions.tools.DetachPixelGeoCodingAction"
)
@ActionRegistration(
        displayName = "#CTL_DetachPixelGeoCodingActionText",
        popupText = "#CTL_DetachPixelGeoCodingActionText"
)
@ActionReference(
        path = "Menu/Tools",
        position = 156,
        separatorAfter = 160
)
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
                    GeoCoding geoCoding = product.getGeoCoding();
                    if (geoCoding instanceof BasicPixelGeoCoding) {
                        final BasicPixelGeoCoding pixelGeoCoding = (BasicPixelGeoCoding) product.getGeoCoding();
                        final GeoCoding delegate = pixelGeoCoding.getPixelPosEstimator();
                        product.setGeoCoding(delegate);
                    } else {
                        product.setGeoCoding(null);
                    }
                    geoCoding.dispose();
                } catch (Throwable e) {
                    return e;
                }
                return null;
            }

            @Override
            public void done() {
                SnapApp snapApp = SnapApp.getDefault();
                try {
                    Throwable value;
                    try {
                        value = get();
                    } catch (Exception e) {
                        value = e;
                    }
                    String dialogTitle = Bundle.CTL_DetachPixelGeoCodingDialogTitle();
                    if (value != null) {
                        snapApp.showErrorDialog(dialogTitle, "An internal error occurred:\n" + value.getMessage());
                    } else {
                        snapApp.showInfoDialog(dialogTitle, "Pixel geo-coding has been detached.", null);
                    }
                } finally {
                    UIUtils.setRootFrameDefaultCursor(snapApp.getMainFrame());
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
            state = product.getGeoCoding() instanceof BasicPixelGeoCoding;
        }
        setEnabled(state);
    }

}