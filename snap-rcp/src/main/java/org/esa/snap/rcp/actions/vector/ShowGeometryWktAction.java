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

package org.esa.snap.rcp.actions.vector;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.support.DefaultFigureSelection;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.SimpleFeatureFigure;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

/**
 * An action that allows users to copy WKT from selected Geometries.
 *
 * @author Norman
 * @since BEAM 5
 */

@ActionID(category = "File", id = "org.esa.snap.rcp.actions.vector.ShowGeometryWktAction")
@ActionRegistration(displayName = "#CTL_ShowGeometryWktAction_MenuText", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Vector", position = 19, separatorAfter = 20),
        @ActionReference(path = "Context/ProductSceneView", position = 10)
})
@NbBundle.Messages({
        "CTL_ShowGeometryWktAction_MenuText=WKT from Geometry",
        "CTL_ShowGeometryWktAction_ShortDescription=Get the well-known-text (WKT) representation of a selected geometry."
})
public class ShowGeometryWktAction extends AbstractAction implements LookupListener, ContextAwareAction {

    private static final String DLG_TITLE = "WKT from Geometry";
    private Lookup.Result<FigureSelection> result;
    private Lookup lookup;

    public ShowGeometryWktAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ShowGeometryWktAction(Lookup lookup) {
        super(Bundle.CTL_ShowGeometryWktAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(FigureSelection.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        exportToWkt();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ShowGeometryWktAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        FigureSelection selection = this.lookup.lookup(FigureSelection.class);
        setEnabled(selection != null);
    }

    private void exportToWkt() {
        SimpleFeatureFigure selectedFeatureFigure = getSimpleFeatureFigure();
        if (selectedFeatureFigure == null) {
            SnapDialogs.showInformation(DLG_TITLE, "Please select a geometry.", null);
            return;
        }
        SimpleFeature simpleFeature = selectedFeatureFigure.getSimpleFeature();
        CoordinateReferenceSystem sourceCrs = simpleFeature.getDefaultGeometryProperty().getDescriptor().getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCrs = DefaultGeographicCRS.WGS84;

        Geometry sourceGeom = selectedFeatureFigure.getGeometry();
        Geometry targetGeom;
        try {
            targetGeom = transformGeometry(sourceGeom, sourceCrs, targetCrs);
        } catch (Exception e) {
            SnapDialogs.showWarning(DLG_TITLE, "Failed to transform geometry to " + targetCrs.getName() + ".\n" +
                    "Using " + sourceCrs.getName() + " instead.", null);
            targetGeom = sourceGeom;
            targetCrs = sourceCrs;
        }

        WKTWriter wktWriter = new WKTWriter();
        wktWriter.setFormatted(true);
        wktWriter.setMaxCoordinatesPerLine(2);
        wktWriter.setTab(3);
        String wkt = wktWriter.writeFormatted(targetGeom);

        JTextArea textArea = new JTextArea(16, 32);
        textArea.setEditable(false);
        textArea.setText(wkt);
        textArea.selectAll();

        JPanel contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.add(new JLabel("Geometry Well-Known-Text (WKT):"), BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        contentPanel.add(new JLabel("Geometry CRS: " + targetCrs.getName().toString()), BorderLayout.SOUTH);

        ModalDialog modalDialog = new ModalDialog(SnapApp.getDefault().getMainFrame(), DLG_TITLE, ModalDialog.ID_OK, null);
        modalDialog.setContent(contentPanel);
        modalDialog.center();
        modalDialog.show();
    }

    private SimpleFeatureFigure getSimpleFeatureFigure() {
        DefaultFigureSelection selection = this.lookup.lookup(DefaultFigureSelection.class);
        SimpleFeatureFigure selectedFeatureFigure = null;
        Figure[] figures = selection.getFigures();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                selectedFeatureFigure = (SimpleFeatureFigure) figure;
            }
        }
        return selectedFeatureFigure;
    }

    private Geometry transformGeometry(Geometry sourceGeom,
                                       CoordinateReferenceSystem sourceCrs,
                                       CoordinateReferenceSystem targetCrs) throws FactoryException, TransformException {
        MathTransform mt = CRS.findMathTransform(sourceCrs, targetCrs, true);
        GeometryCoordinateSequenceTransformer gcst = new GeometryCoordinateSequenceTransformer();
        gcst.setMathTransform(mt);
        return gcst.transform(sourceGeom);
    }

}
