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

package org.esa.snap.timeseries.ui.manager;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.timeseries.core.TimeSeriesMapper;
import org.esa.snap.timeseries.core.insitu.InsituSource;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;


@TopComponent.Description(
        preferredID = "TimeSeriesManagerTopComponent",
        iconBase = "org/esa/snap/timeseries/ui/icons/timeseries-manager.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TimeSeriesManagerTopComponent_Name",
        preferredID = "TimeSeriesManagerTopComponent"
)
@ActionID(category = "Window", id = "org.esa.snap.timeseries.ui.manager.TimeSeriesManagerTopComponent")
//@ActionReferences({
//        @ActionReference(path = "Menu/Raster/Time Series", position = 1210, separatorBefore = 1200),
//        @ActionReference(path = "Toolbars/Time Series", position = 10)
//})
@NbBundle.Messages({
        "CTL_TimeSeriesManagerTopComponent_Name=Time Series Manager",
        "CTL_TimeSeriesManagerTopComponent_ComponentName=Time_Series_Manager"
})
/**
 * Main class for the manager tool.
 *
 * @author Marco Peters
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class TimeSeriesManagerTopComponent extends TopComponent {

    private static final String HELP_ID = "timeSeriesManager";

    private Product selectedProduct;
    private String prefixTitle;

    private final WeakHashMap<Product, TimeSeriesManagerForm> formMap;
    private TimeSeriesManagerForm activeForm;
    private final TimeSeriesManagerTSL timeSeriesManagerTSL;

    public TimeSeriesManagerTopComponent() {
        setName(Bundle.CTL_TimeSeriesManagerTopComponent_ComponentName());
        formMap = new WeakHashMap<>();
        timeSeriesManagerTSL = new TimeSeriesManagerTSL();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));

        prefixTitle = Bundle.CTL_TimeSeriesManagerTopComponent_Name();

        setSelectedProduct(SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO));

        realizeActiveForm();
        updateTitle();
        SnapApp.getDefault().getSelectionSupport(ProductNode.class).addHandler((oldValue, newValue) -> {
            if(newValue != null) {
                setSelectedProduct(newValue.getProduct());
            } else {
                setSelectedProduct(null);
            }
        });
        SnapApp.getDefault().getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {

            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                productClosed(event.getProduct());
            }
        });
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    Product getSelectedProduct() {
        return selectedProduct;
    }

    private void productClosed(Product product) {
        formMap.remove(product);
        setSelectedProduct(null);
    }

    private void updateTitle() {
        final String suffix;
        final Product product = getSelectedProduct();
        if (product != null) {
            suffix = " - " + product.getDisplayName();
        } else {
            suffix = "";
        }
        setDisplayName(prefixTitle + suffix);
    }

    private void setSelectedProduct(Product newProduct) {
        Product oldProduct = selectedProduct;
        if (newProduct != oldProduct) {
            if (oldProduct != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(oldProduct);
                if (timeSeries != null) {
                    timeSeries.removeTimeSeriesListener(timeSeriesManagerTSL);
                }
            }

            selectedProduct = newProduct;
            realizeActiveForm();
            updateTitle();

            if (newProduct != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(newProduct);
                if (timeSeries != null) {
                    timeSeries.addTimeSeriesListener(timeSeriesManagerTSL);
                }
            }
        }
    }

    private void realizeActiveForm() {
        if (getComponentCount() > 0) {
            remove(0);
        }

        activeForm = getOrCreateActiveForm(getSelectedProduct());
        add(activeForm.getControl(), BorderLayout.CENTER);

        validate();
        repaint();
    }

    private TimeSeriesManagerForm getOrCreateActiveForm(Product product) {
        if (formMap.containsKey(product)) {
            activeForm = formMap.get(product);
        } else {
            activeForm = new TimeSeriesManagerForm(HELP_ID);
            formMap.put(product, activeForm);
        }
        activeForm.updateFormControl(product);
        return activeForm;
    }

    private void updateInsituPins() {
        final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(selectedProduct);
        timeSeries.clearInsituPlacemarks();
        addPlacemarks(timeSeries);
    }

    private void addPlacemarks(AbstractTimeSeries timeSeries) {
        final InsituSource insituSource = timeSeries.getInsituSource();
        final List<String> selectedInsituVariables = getSelectedInsituVariables(timeSeries, insituSource);
        final Set<GeoPos> geoPoses = new TreeSet<>(createGeoPosComparator());
        for (String selectedInsituVariable : selectedInsituVariables) {
            geoPoses.addAll(insituSource.getInsituPositionsFor(selectedInsituVariable));
        }

        final Product tsProduct = timeSeries.getTsProduct();
        final GeoCoding geoCoding = tsProduct.getSceneGeoCoding();

        final PixelPos pixelPos = new PixelPos();
        for (GeoPos geoPos : geoPoses) {
            geoCoding.getPixelPos(geoPos, pixelPos);
            if (!AbstractTimeSeries.isPixelValid(tsProduct, pixelPos)) {
                continue;
            }
            String name;
            if (insituSource.hasStationNames()) {
                name = insituSource.getNameFor(geoPos);
            } else {
                name = geoPos.getLatString() + "_" + geoPos.getLonString();
            }

            final String pinName = "Insitu_" + name;
            final String pinLabel = name;
            final String pinDescription = name;
            final Placemark placemark = Placemark.createPointPlacemark(
                    PinDescriptor.getInstance(),
                    pinName, pinLabel, pinDescription,
                    null, new GeoPos(geoPos), geoCoding);
            timeSeries.registerRelation(placemark, geoPos);
        }
    }

    private Comparator<GeoPos> createGeoPosComparator() {
        return (o1, o2) -> o1.toString().compareTo(o2.toString());
    }

    private List<String> getSelectedInsituVariables(AbstractTimeSeries timeSeries, InsituSource insituSource) {
        final String[] parameterNames = insituSource.getParameterNames();
        final List<String> selectedInsituVariables = new ArrayList<>();
        for (String parameterName : parameterNames) {
            if (timeSeries.isInsituVariableSelected(parameterName)) {
                selectedInsituVariables.add(parameterName);
            }
        }
        return selectedInsituVariables;
    }

    private class TimeSeriesManagerTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            final int type = event.getType();
            if (type == TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME ||
                    type == TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME) {
                activeForm.updateFormControl(getSelectedProduct());
            } else if (type == TimeSeriesChangeEvent.PROPERTY_INSITU_VARIABLE_SELECTION) {
                updateInsituPins();
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            activeForm.updateFormControl(getSelectedProduct());
        }
    }
}
