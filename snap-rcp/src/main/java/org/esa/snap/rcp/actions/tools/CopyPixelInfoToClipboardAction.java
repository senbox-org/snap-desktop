/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.MultiSizeIssue;
import org.esa.snap.ui.product.ProductSceneView;
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


@ActionID(category = "Tools", id = "org.esa.snap.rcp.actions.tools.CopyPixelInfoToClipboardAction")
@ActionRegistration(
        displayName = "#CTL_CopyPixelInfoToClipboardAction_MenuText",
        popupText = "#CTL_CopyPixelInfoToClipboardAction_PopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Export", position = 300),
        @ActionReference(path = "Context/ProductSceneView", position = 110)
})
@NbBundle.Messages({
        "CTL_CopyPixelInfoToClipboardAction_MenuText=Pixel-Info to Clipboard",
        "CTL_CopyPixelInfoToClipboardAction_PopupText=Copy Pixel-Info to Clipboard",
        "CTL_CopyPixelInfoToClipboardAction_DialogTitle=Copy Pixel-Info to Clipboard",
        "CTL_CopyPixelInfoToClipboardAction_ShortDescription=Copy Pixel-Info to Clipboard."
})

public class CopyPixelInfoToClipboardAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup.Result<ProductSceneView> result;

    public CopyPixelInfoToClipboardAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CopyPixelInfoToClipboardAction(Lookup lkp) {
        super(Bundle.CTL_CopyPixelInfoToClipboardAction_MenuText());
        putValue("popupText", Bundle.CTL_CopyPixelInfoToClipboardAction_PopupText());
        result = lkp.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        updateEnableState(getCurrentSceneView());
    }

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */

    @Override
    public void actionPerformed(ActionEvent event) {
        copyPixelInfoStringToClipboard();
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new CopyPixelInfoToClipboardAction(lkp);
    }

    @Override
    public void resultChanged(LookupEvent le) {
        updateEnableState(getCurrentSceneView());
    }

    private void copyPixelInfoStringToClipboard() {
        final ProductSceneView view = getCurrentSceneView();
        if (view != null) {
            Product product = view.getProduct();
            if (product != null) {
                final RasterDataNode viewRaster = view.getRaster();
                SystemUtils.copyToClipboard(product.createPixelInfoString(view.getCurrentPixelX(), view.getCurrentPixelY(),
                                                                          viewRaster));
            }
        }
    }

    private ProductSceneView getCurrentSceneView() {
        return result.allInstances().stream().findFirst().orElse(null);
    }

    private void updateEnableState(ProductSceneView sceneView) {
        setEnabled(sceneView != null);
    }

}
