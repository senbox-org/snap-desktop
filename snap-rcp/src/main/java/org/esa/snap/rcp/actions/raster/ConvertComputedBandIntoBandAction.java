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

package org.esa.snap.rcp.actions.raster;

import com.bc.ceres.multilevel.MultiLevelImage;
import eu.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

/**
 * Converts a virtual band into a "real" band.
 *
 * @author marcoz
 */
@ActionID(category = "Tools", id = "ConvertComputedBandIntoBandAction")
@ActionRegistration(
        displayName = "#CTL_ConvertComputedBandIntoBandAction_MenuText",
        popupText = "#CTL_ConvertComputedBandIntoBandAction_MenuText"
)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 20),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 30)
})
@NbBundle.Messages({
        "CTL_ConvertComputedBandIntoBandAction_MenuText=Convert Band",
        "CTL_ConvertComputedBandIntoBandAction_ShortDescription=Computes a \"real\" band from a virtual band or filtered band"
})
public class ConvertComputedBandIntoBandAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup lookup;
    @SuppressWarnings("FieldCanBeLocal")
    private final Lookup.Result<VirtualBand> vbResult;
    @SuppressWarnings("FieldCanBeLocal")
    private final Lookup.Result<FilterBand> fbResult;

    public ConvertComputedBandIntoBandAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ConvertComputedBandIntoBandAction(Lookup lookup) {
        super(Bundle.CTL_ConvertComputedBandIntoBandAction_MenuText());
        this.lookup = lookup;
        vbResult = lookup.lookupResult(VirtualBand.class);
        vbResult.addLookupListener(WeakListeners.create(LookupListener.class, this, vbResult));
        fbResult = lookup.lookupResult(FilterBand.class);
        fbResult.addLookupListener(WeakListeners.create(LookupListener.class, this, fbResult));
        setEnableState();
    }


    private void setEnableState() {
        VirtualBand virtualBand = lookup.lookup(VirtualBand.class);
        FilterBand filterBand = lookup.lookup(FilterBand.class);
        setEnabled(virtualBand != null || filterBand != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SnapApp snapApp = SnapApp.getDefault();
        ProductNode selectedProductNode = snapApp.getSelectedProductNode(EXPLORER);
        if (!isComputedBand(selectedProductNode)) {
            return;
        }

        Band computedBand = (Band) selectedProductNode;
        String bandName = computedBand.getName();
        int width = computedBand.getRasterWidth();
        int height = computedBand.getRasterHeight();

        Band realBand = new Band(bandName, computedBand.getDataType(), width, height);
        realBand.setDescription(createDescription(computedBand));
        realBand.setValidPixelExpression(computedBand.getValidPixelExpression());
        realBand.setUnit(computedBand.getUnit());
        realBand.setSpectralWavelength(computedBand.getSpectralWavelength());
        realBand.setAngularValue(computedBand.getAngularValue());
        realBand.setAngularBandIndex(computedBand.getAngularBandIndex());
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
        ProductSceneViewTopComponent topComponent = getProductSceneViewTopComponent(computedBand);
        if (topComponent != null) {
            topComponent.close();
        }

        ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        int bandIndex = bandGroup.indexOf(computedBand);
        bandGroup.remove(computedBand);
        bandGroup.add(bandIndex, realBand);

        realBand.setSourceImage(createSourceImage(computedBand, realBand));

        realBand.setModified(true);
    }

    MultiLevelImage createSourceImage(Band computedBand, Band realBand) {
        if (computedBand instanceof VirtualBand) {
            return VirtualBand.createSourceImage(realBand, ((VirtualBand) computedBand).getExpression());
        } else {
            return computedBand.getSourceImage();
        }
    }

    private String createDescription(Band computedBand) {
        if (computedBand instanceof VirtualBand) {
            VirtualBand virtualBand = (VirtualBand) computedBand;
            String oldDescription = virtualBand.getDescription();
            String newDescription = oldDescription == null ? "" : oldDescription.trim();
            String formerExpressionDescription = "(expression was '" + virtualBand.getExpression() + "')";
            newDescription = newDescription.isEmpty() ? formerExpressionDescription : newDescription + " " + formerExpressionDescription;
            return newDescription;
        } else {
            return computedBand.getDescription();
        }
    }

    //copied from TimeSeriesManagerForm
    private ProductSceneViewTopComponent getProductSceneViewTopComponent(RasterDataNode raster) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> raster == topComponent.getView().getRaster())
                .findFirst()
                .orElse(null);
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