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

package org.esa.snap.rcp.actions.view;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.snap.framework.datamodel.PlainFeatureFactory;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.framework.ui.product.VectorDataLayer;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.interactors.InsertFigureInteractorInterceptor;
import org.esa.snap.util.FeatureUtils;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

/**
 * An action that allows users to insert a Geometry from WKT.
 *
 * @author MarcoZ
 * @since BEAM 5
 */

@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.view.InsertWktGeometryAction"
)
@ActionRegistration(
        displayName = "#CTL_InsertWktGeometryAction_MenuText",
        popupText = "#CTL_InsertWktGeometryAction_MenuText",
        lazy = true
)
@ActionReference(path = "Context/View", position = 10)

@NbBundle.Messages({
        "CTL_InsertWktGeometryAction_DialogTitle=Geometry from WKT...",
        "CTL_InsertWktGeometryAction_MenuText=Geometry from WKT...",
        "CTL_InsertWktGeometryAction_ShortDescription=Creates a geomtry from well-known-text (WKT) representation."
})


public class InsertWktGeometryAction extends AbstractAction {

    private static final String DLG_TITLE = "Geometry from WKT";
    private long currentFeatureId = System.nanoTime();


    public InsertWktGeometryAction(){
        setEnabled(SnapApp.getDefault().getSelectedProductSceneView() != null);
    }


    @Override
    public void actionPerformed(ActionEvent event) {
        JTextArea textArea = new JTextArea(16, 32);
        textArea.setEditable(true);

        JPanel contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.add(new JLabel("Geometry Well-Known-Text (WKT):"), BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        SnapApp snapApp = SnapApp.getDefault();
        ModalDialog modalDialog = new ModalDialog(snapApp.getMainFrame(),
                                                  Bundle.CTL_InsertWktGeometryAction_DialogTitle(),
                                                  ModalDialog.ID_OK_CANCEL, null);
        modalDialog.setContent(contentPanel);
        modalDialog.center();
        if (modalDialog.show() == ModalDialog.ID_OK) {
            String wellKnownText = textArea.getText();
            if (wellKnownText == null || wellKnownText.isEmpty()) {
                return;
            }
            ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
            VectorDataLayer vectorDataLayer = InsertFigureInteractorInterceptor.getActiveVectorDataLayer(sceneView);
            if (vectorDataLayer == null) {
                return;
            }

            SimpleFeatureType wktFeatureType = PlainFeatureFactory.createDefaultFeatureType(DefaultGeographicCRS.WGS84);
            ListFeatureCollection newCollection = new ListFeatureCollection(wktFeatureType);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(wktFeatureType);
            SimpleFeature wktFeature = featureBuilder.buildFeature("ID" + Long.toHexString(currentFeatureId++));
            Geometry geometry;
            try {
                geometry = new WKTReader().read(wellKnownText);
            } catch (ParseException e) {
                snapApp.handleError("Failed to convert WKT into geometry", e);
                return;
            }
            wktFeature.setDefaultGeometry(geometry);
            newCollection.add(wktFeature);

            FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(
                    newCollection,
                    sceneView.getProduct(),
                    null,
                    ProgressMonitor.NULL);
            if (productFeatures.isEmpty()) {
                SnapDialogs.showError(DLG_TITLE, "The geometry is not contained in the product.");
            } else {
                vectorDataLayer.getVectorDataNode().getFeatureCollection().addAll(productFeatures);
            }
        }
    }

}
