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

package org.esa.snap.rcp.actions.vector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.io.CsvReader;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

//import org.esa.snap.visat.VisatApp;


/**
 * Action that lets a user load text files that contain data associated with a geographic position,
 * e.g. some kind of track. The format is:
 * <pre>
 *     lat-1 TAB lon-1 TAB data-1 NEWLINE
 *     lat-2 TAB lon-2 TAB data-2 NEWLINE
 *     lat-3 TAB lon-3 TAB data-3 NEWLINE
 *     ...
 *     lat-n TAB lon-n TAB data-n NEWLINE
 * </pre>
 * <p>
 * This is the format that is also used by SeaDAS 6.x in order to import ship tracks.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
@ActionID(
        category = "File",
        id = "ImportTrackAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportSeadasTrackActionName",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Import/Vector Data", position = 50),
        @ActionReference(path = "Menu/Vector/Import")
})
@NbBundle.Messages({
        "CTL_ImportSeadasTrackActionText=SeaDAS 6.x Track",
        "CTL_ImportSeadasTrackActionName=Import SeaDAS Track",
        "CTL_ImportSeadasTrackActionHelp=importSeadasTrack"
})
public class ImportTrackAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private Lookup lookup;
    private final Lookup.Result<Product> result;

    public ImportTrackAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ImportTrackAction(Lookup lookup) {
        this.lookup = lookup;
        result = lookup.lookupResult(Product.class);
        result.addLookupListener(
                WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
        setHelpId(Bundle.CTL_ImportSeadasTrackActionHelp());
        putValue(Action.NAME, Bundle.CTL_ImportSeadasTrackActionText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ImportSeadasTrackActionName());
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ImportTrackAction(lookup);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        final File file =
                SnapDialogs.requestFileForOpen(Bundle.CTL_ImportSeadasTrackActionName(), false, null, "importTrack.lastDir");
        if (file == null) {
            return;
        }
        final Product product = SnapApp.getDefault().getSelectedProduct();
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        try {
            featureCollection = readTrack(file, product.getSceneGeoCoding());
        } catch (IOException e) {
            SnapDialogs.showError(Bundle.CTL_ImportSeadasTrackActionName(), "Failed to load track file:\n" + e.getMessage());
            return;
        }

        if (featureCollection.isEmpty()) {
            SnapDialogs.showError(Bundle.CTL_ImportSeadasTrackActionName(), "No records found.");
            return;
        }

        String name = FileUtils.getFilenameWithoutExtension(file);
        final PlacemarkDescriptor placemarkDescriptor =
                PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(featureCollection.getSchema());
        placemarkDescriptor.setUserDataOf(featureCollection.getSchema());
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection, placemarkDescriptor);

        product.getVectorDataGroup().add(vectorDataNode);

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null) {
            view.setLayersVisible(vectorDataNode);
        }
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
    }

    private void setEnableState() {
        boolean state = false;
        ProductNode productNode = lookup.lookup(ProductNode.class);
        if (productNode != null) {
            Product product = productNode.getProduct();
            state = product != null && product.getSceneGeoCoding() != null;
        }
        setEnabled(state);
    }

    private static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(File file, GeoCoding geoCoding) throws IOException {
        Reader reader = new FileReader(file);
        try {
            return readTrack(reader, geoCoding);
        } finally {
            reader.close();
        }
    }

    static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(Reader reader, GeoCoding geoCoding) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'\t', ' '}, true, "#");
        SimpleFeatureType trackFeatureType = createTrackFeatureType(geoCoding);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = new ListFeatureCollection(trackFeatureType);
        double[] record;
        int pointIndex = 0;
        while ((record = csvReader.readDoubleRecord()) != null) {
            if (record.length < 3) {
                throw new IOException("Illegal track file format.\n" +
                                              "Expecting tab-separated lines containing 3 values: lat, lon, data.");
            }

            float lat = (float) record[0];
            float lon = (float) record[1];
            double data = record[2];

            final SimpleFeature feature = createFeature(trackFeatureType, geoCoding, pointIndex, lat, lon, data);
            if (feature != null) {
                featureCollection.add(feature);
            }

            pointIndex++;
        }

        if (featureCollection.isEmpty()) {
            throw new IOException("No track point found or all of them are located outside the scene boundaries.");
        }

        final CoordinateReferenceSystem mapCRS = geoCoding.getMapCRS();
        if (!mapCRS.equals(DefaultGeographicCRS.WGS84)) {
            try {
                transformFeatureCollection(featureCollection, mapCRS);
            } catch (TransformException e) {
                throw new IOException("Cannot transform the ship track onto CRS '" + mapCRS.toWKT() + "'.", e);
            }
        }

        return featureCollection;
    }

    private static void transformFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, CoordinateReferenceSystem targetCRS) throws TransformException {
        final GeometryCoordinateSequenceTransformer transform = FeatureUtils.getTransform(DefaultGeographicCRS.WGS84, targetCRS);
        final FeatureIterator<SimpleFeature> features = featureCollection.features();
        final GeometryFactory geometryFactory = new GeometryFactory();
        while (features.hasNext()) {
            final SimpleFeature simpleFeature = features.next();
            final Point sourcePoint = (Point) simpleFeature.getDefaultGeometry();
            final Point targetPoint = transform.transformPoint(sourcePoint, geometryFactory);
            simpleFeature.setDefaultGeometry(targetPoint);
        }
    }

    private static SimpleFeatureType createTrackFeatureType(GeoCoding geoCoding) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("org.esa.snap.TrackPoint");
        /*0*/
        ftb.add("pixelPos", Point.class, geoCoding.getImageCRS());
        /*1*/
        ftb.add("geoPos", Point.class, DefaultGeographicCRS.WGS84);
        /*2*/
        ftb.add("data", Double.class);
        ftb.setDefaultGeometry(geoCoding instanceof CrsGeoCoding ? "geoPos" : "pixelPos");
        // GeoTools Bug: this doesn't work
        // ftb.userData("trackPoints", "true");
        final SimpleFeatureType ft = ftb.buildFeatureType();
        ft.getUserData().put("trackPoints", "true");
        return ft;
    }

    private static SimpleFeature createFeature(SimpleFeatureType type, GeoCoding geoCoding, int pointIndex, float lat, float lon, double data) {
        PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(lat, lon), null);
        if (!pixelPos.isValid()) {
            return null;
        }
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        GeometryFactory gf = new GeometryFactory();
        /*0*/
        fb.add(gf.createPoint(new Coordinate(pixelPos.x, pixelPos.y)));
        /*1*/
        fb.add(gf.createPoint(new Coordinate(lon, lat)));
        /*2*/
        fb.add(data);
        return fb.buildFeature(String.format("ID%08d", pointIndex));
    }

}
