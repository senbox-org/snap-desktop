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

package org.esa.snap.core.gpf.ui.reproject;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.ParameterUpdater;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.DefaultAppContext;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.HashMap;
import java.util.Map;

class ReprojectionDialog extends SingleTargetProductDialog {

    private static final String OPERATOR_NAME = "Reproject";
    private final ReprojectionForm form;

    public static void main(String[] args) {
        final DefaultAppContext context = new DefaultAppContext("Reproj");
        final ReprojectionDialog dialog = new ReprojectionDialog(true, "ReproTestDialog", null, context);
        dialog.show();

    }

    ReprojectionDialog(boolean orthorectify, final String title, final String helpID, AppContext appContext) {
        super(appContext, title, ID_APPLY_CLOSE, helpID);
        form = new ReprojectionForm(getTargetProductSelector(), orthorectify, appContext);

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(OPERATOR_NAME);

        ParameterUpdater parameterUpdater = new ReprojectionParameterUpdater();

        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                 null,
                                                                                 null,
                                                                                 parameterUpdater);
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     parameterSupport,
                                                     appContext,
                                                     helpID);

        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected boolean verifyUserInput() {
        if (form.getSourceProduct() == null) {
            showErrorDialog("No product to reproject selected.");
            return false;
        }

        final CoordinateReferenceSystem crs = form.getSelectedCrs();
        if (crs == null) {
            showErrorDialog("No 'Coordinate Reference System' selected.");
            return false;
        }

        String externalDemName = form.getExternalDemName();
        if (externalDemName != null) {
            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(externalDemName);
            if (demDescriptor == null) {
                showErrorDialog("The DEM '" + externalDemName + "' is not supported.");
                close();
                return false;
            }
        }
        return true;
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> productMap = form.getProductMap();
        final Map<String, Object> parameterMap = new HashMap<>();
        form.updateParameterMap(parameterMap);
        return GPF.createProduct(OPERATOR_NAME, parameterMap, productMap);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

    private class ReprojectionParameterUpdater implements ParameterUpdater {

        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) {
            form.updateParameterMap(parameterMap);
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            form.updateFormModel(parameterMap);
        }
    }
}
