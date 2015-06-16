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

package org.esa.snap.rcp.session;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.FilterBand;
import org.esa.snap.framework.datamodel.ImageInfo;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.ProductNodeGroup;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
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
 * Converts a virtual band into a "real" band.
 *
 * @author marcoz
 */

@ActionID(
        category = "Tools",
        id = "ConvertComputedBandIntoBandAction"
)
@ActionRegistration(
        displayName = "#CTL_ConvertComputedBandIntoBandAction_MenuText",
        popupText = "#CTL_ConvertComputedBandIntoBandAction_MenuText"
)
@ActionReferences({
        @ActionReference(path = "Menu/Tools", position = 110),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 220)
})
@NbBundle.Messages({
        "CTL_ConvertComputedBandIntoBandAction_MenuText=Convert Computed Band",
        "CTL_ConvertComputedBandIntoBandAction_ShortDescription=Computes a \"real\" band from a virtual band or filtered ban"
})
public class ConvertComputedBandIntoBandAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup lookup;
    private final Lookup.Result<VirtualBand> result;

    public ConvertComputedBandIntoBandAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ConvertComputedBandIntoBandAction(Lookup lookup) {
        super(Bundle.CTL_ConvertComputedBandIntoBandAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(VirtualBand.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
    }


    private void setEnableState() {
        VirtualBand band = lookup.lookup(VirtualBand.class);
        setEnabled(band != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SnapApp snapApp = SnapApp.getDefault();
        ProductNode selectedProductNode = snapApp.getSelectedProductNode();
        if (!isComputedBand(selectedProductNode)) {
            return;
        }

        Band computedBand = (Band) selectedProductNode;
        String bandName = computedBand.getName();
        int width = computedBand.getSceneRasterWidth();
        int height = computedBand.getSceneRasterHeight();

        Band realBand;
        if (selectedProductNode instanceof VirtualBand) {
            VirtualBand virtualBand = (VirtualBand) selectedProductNode;
            String expression = virtualBand.getExpression();
            realBand = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
            realBand.setDescription(createDescription(virtualBand.getDescription(), expression));
            realBand.setSourceImage(virtualBand.getSourceImage());
        } else if (selectedProductNode instanceof FilterBand) {
            FilterBand filterBand = (FilterBand) selectedProductNode;
            realBand = new Band(bandName, filterBand.getDataType(), width, height);
            realBand.setDescription(filterBand.getDescription());
            realBand.setValidPixelExpression(filterBand.getValidPixelExpression());
            realBand.setSourceImage(filterBand.getSourceImage());
        } else {
            throw new IllegalStateException();
        }

        realBand.setUnit(computedBand.getUnit());
        realBand.setSpectralWavelength(computedBand.getSpectralWavelength());
        realBand.setGeophysicalNoDataValue(computedBand.getGeophysicalNoDataValue());
        realBand.setNoDataValueUsed(computedBand.isNoDataValueUsed());
        if (computedBand.isStxSet()) {
            realBand.setStx(computedBand.getStx());
        }

        ImageInfo imageInfo = computedBand.getImageInfo();
        if (imageInfo != null) {
            realBand.setImageInfo(imageInfo.clone());
        }

        //--- Check if all the frame with the raster data are close
        Product product = computedBand.getProduct();
        boolean productSceneViewOpen = false;
        ProductSceneViewTopComponent topComponent = getProductSceneViewTopComponent(computedBand);
        if (topComponent != null) {
            topComponent.close();
        }

        ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        int bandIndex = bandGroup.indexOf(computedBand);
        bandGroup.remove(computedBand);
        bandGroup.add(bandIndex, realBand);

        realBand.setModified(true);

        if (productSceneViewOpen) {
            snapApp.getSelectedProductSceneView();
        }
    }

    //copied from TimeSeriesManagerForm
    private ProductSceneViewTopComponent getProductSceneViewTopComponent(RasterDataNode raster) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> raster == topComponent.getView().getRaster())
                .findFirst()
                .orElse(null);
    }


    private String createDescription(String oldDescription, String expression) {
        String newDescription = oldDescription == null ? "" : oldDescription.trim();
        String formerExpressionDescription = "(expression was '" + expression + "')";
        newDescription = newDescription.isEmpty() ? formerExpressionDescription : newDescription + " " + formerExpressionDescription;
        return newDescription;
    }

    private boolean isComputedBand(ProductNode selectedProductNode) {
        return selectedProductNode instanceof VirtualBand || selectedProductNode instanceof FilterBand;
    }


    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ConvertComputedBandIntoBandAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }
}