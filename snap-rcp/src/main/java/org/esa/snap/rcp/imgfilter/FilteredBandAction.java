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
package org.esa.snap.rcp.imgfilter;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenImageViewAction;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.ui.ModalDialog;
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


@ActionID(
        category = "Tools",
        id = "FilteredBandAction"
)
@ActionRegistration(
        displayName = "#CTL_FilteredBandAction_MenuText",
        popupText = "#CTL_FilteredBandAction_MenuText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 10),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 40,separatorAfter = 45)
})
@NbBundle.Messages({
        "CTL_FilteredBandAction_MenuText=Filtered Band...",
        "CTL_FilteredBandAction_ShortDescription=Applies a filter to the currently selected band and adds it as a new band."
})
public class FilteredBandAction extends AbstractAction  implements LookupListener, ContextAwareAction {

    private Lookup lookup;
    private Lookup.Result<RasterDataNode> result;


    public FilteredBandAction() {
        this(Utilities.actionsGlobalContext());
    }

    public FilteredBandAction(Lookup lookup){
        super(Bundle.CTL_FilteredBandAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(RasterDataNode.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        final RasterDataNode node = this.lookup.lookup(RasterDataNode.class);
        //todo [multisize_products] compare scenerastertransform rather than size
        setEnabled(node != null && node.getRasterSize().equals(node.getProduct().getSceneRasterSize()));
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new FilteredBandAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        final RasterDataNode node = this.lookup.lookup(RasterDataNode.class);
        //todo [multisize_products] compare scenerastertransform rather than size
        setEnabled(node != null && node.getRasterSize().equals(node.getProduct().getSceneRasterSize()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        createFilteredBand();
    }

    static GeneralFilterBand.OpType getOpType(Filter.Operation operation) {
        if (operation == Filter.Operation.OPEN) {
            return GeneralFilterBand.OpType.OPENING;
        } else if (operation == Filter.Operation.CLOSE) {
            return GeneralFilterBand.OpType.CLOSING;
        } else if (operation == Filter.Operation.ERODE) {
            return GeneralFilterBand.OpType.EROSION;
        } else if (operation == Filter.Operation.DILATE) {
            return GeneralFilterBand.OpType.DILATION;
        } else if (operation == Filter.Operation.MIN) {
            return GeneralFilterBand.OpType.MIN;
        } else if (operation == Filter.Operation.MAX) {
            return GeneralFilterBand.OpType.MAX;
        } else if (operation == Filter.Operation.MEAN) {
            return GeneralFilterBand.OpType.MEAN;
        } else if (operation == Filter.Operation.MEDIAN) {
            return GeneralFilterBand.OpType.MEDIAN;
        } else if (operation == Filter.Operation.STDDEV) {
            return GeneralFilterBand.OpType.STDDEV;
        } else {
            throw new IllegalArgumentException("illegal operation: " + operation);
        }
    }

    private void createFilteredBand() {
        RasterDataNode node = lookup.lookup(RasterDataNode.class);

        final CreateFilteredBandDialog.DialogData dialogData = promptForFilter();
        if (dialogData == null) {
            return;
        }

        final FilterBand filterBand = getFilterBand(node,
                dialogData.getBandName(),
                dialogData.getFilter(),
                dialogData.getIterationCount());

        OpenImageViewAction.openImageView(filterBand);
    }

    private static FilterBand getFilterBand(RasterDataNode sourceRaster, String bandName, Filter filter, int iterationCount) {
        FilterBand targetBand;
        Product product = sourceRaster.getProduct();

        if (filter.getOperation() == Filter.Operation.CONVOLVE) {
            targetBand = new ConvolutionFilterBand

                    (bandName, sourceRaster, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        } else {
            GeneralFilterBand.OpType opType = getOpType(filter.getOperation());
            targetBand = new GeneralFilterBand(bandName, sourceRaster, opType, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        }

        targetBand.setDescription(String.format("Filter '%s' (=%s) applied to '%s'", filter.getName(), filter.getOperation(), sourceRaster.getName()));
        if (sourceRaster instanceof Band) {
            ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
        }
        product.addBand(targetBand);
        targetBand.fireProductNodeDataChanged();
        return targetBand;
    }

    private static Kernel getKernel(Filter filter) {
        return new Kernel(filter.getKernelWidth(),
                filter.getKernelHeight(),
                filter.getKernelOffsetX(),
                filter.getKernelOffsetY(),
                1.0 / filter.getKernelQuotient(),
                filter.getKernelElements());
    }

    private CreateFilteredBandDialog.DialogData promptForFilter() {
        final ProductNode selectedNode = SnapApp.getDefault().getSelectedProductNode();
        final Product product = selectedNode.getProduct();
        final CreateFilteredBandDialog dialog = new CreateFilteredBandDialog(product, selectedNode.getName(), "createFilteredBand");
        if (dialog.show() == ModalDialog.ID_OK) {
            return dialog.getDialogData();
        }
        return null;
    }

}


