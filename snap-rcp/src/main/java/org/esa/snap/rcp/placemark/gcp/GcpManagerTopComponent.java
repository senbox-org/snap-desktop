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

package org.esa.snap.rcp.placemark.gcp;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.rcp.placemark.PlacemarkManagerTopComponent;
import org.esa.snap.rcp.placemark.TableModelFactory;
import org.esa.snap.ui.DecimalTableCellRenderer;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.AbstractPlacemarkTableModel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.text.DecimalFormat;

@TopComponent.Description(
        preferredID = "GcpManagerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.GCP_MANAGER_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "output",
        openAtStartup = false,
        position = 20
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.placemark.gcp.GcpManagerTopComponent")
@ActionReferences({
                          @ActionReference(path = "Menu/View/Tool Windows"),
                          @ActionReference(
                                  path = "Toolbars/" + PackageDefaults.GCP_MANAGER_TOOLBAR_NAME,
                                  position = PackageDefaults.GCP_MANAGER_TOOLBAR_POSITION
                          )
                  })
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_GcpManagerTopComponent_Name",
        preferredID = "GcpManagerTopComponent"
)
@NbBundle.Messages({
                           "CTL_GcpManagerTopComponent_Name=" + PackageDefaults.GCP_MANAGER_NAME,
                           "CTL_GcpManagerTopComponent_HelpId=showGcpManagerWnd"
                   })
/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class GcpManagerTopComponent extends PlacemarkManagerTopComponent {

    public static final String ID = GcpManagerTopComponent.class.getName();
    private GcpGeoCodingForm geoCodingForm;
    private final ProductNodeListenerAdapter geoCodinglistener;

    public GcpManagerTopComponent() {
        super(GcpDescriptor.getInstance(), new TableModelFactory() {
            @Override
            public AbstractPlacemarkTableModel createTableModel(PlacemarkDescriptor placemarkDescriptor,
                                                                Product product,
                                                                Band[] selectedBands, TiePointGrid[] selectedGrids) {
                return new GcpTableModel(placemarkDescriptor, product, selectedBands, selectedGrids);
            }
        });
        geoCodinglistener = new ProductNodeListenerAdapter() {

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (Product.PROPERTY_NAME_SCENE_GEO_CODING.equals(event.getPropertyName())) {
                    updateUIState();
                }

            }
        };
    }

    @Override
    protected Component getSouthExtension() {
        geoCodingForm = new GcpGeoCodingForm();
        return geoCodingForm;
    }

    @Override
    public void setProduct(Product product) {
        final Product oldProduct = getProduct();
        if (oldProduct != product) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(geoCodinglistener);
            }
            if (product != null) {
                product.addProductNodeListener(geoCodinglistener);
            }
        }
        super.setProduct(product);
    }

    @Override
    protected void addCellRenderer(TableColumnModel columnModel) {
        super.addCellRenderer(columnModel);
        columnModel.getColumn(4).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000000")));
        columnModel.getColumn(5).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000000")));
    }

    @Override
    protected void updateUIState() {
        super.updateUIState();
        geoCodingForm.setProduct(getProduct());
        geoCodingForm.updateUIState();
    }

    @Override
    protected String getTitle() {
        return Bundle.CTL_GcpManagerTopComponent_Name();
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_GcpManagerTopComponent_HelpId();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_GcpManagerTopComponent_HelpId());
    }
}
