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

package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.UIUtils;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportGeometryAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportGeometryAction_MenuText",
        popupText = "#CTL_ExportGeometryAction_PopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other", position = 40),
        @ActionReference(path = "Menu/Vector/Export"),
        @ActionReference(path = "Context/Product/VectorDataNode", position = 208)
})

@NbBundle.Messages({
        "CTL_ExportGeometryAction_MenuText=Geometry as Shape file",
        "CTL_ExportGeometryAction_PopupText=Export Geometry as Shape file",
        "CTL_ExportGeometryAction_DialogTitle=Export Geometry as ESRI Shapefile",
        "CTL_ExportGeometryAction_ShortDescription=Exports the currently selected geometry as ESRI Shapefile."
})

public class ExportGeometryAction extends AbstractAction implements ContextAwareAction, LookupListener, HelpCtx.Provider {

    private static final String ESRI_SHAPEFILE = "ESRI Shapefile";
    private static final String FILE_EXTENSION_SHAPEFILE = ".shp";
    private static final String HELP_ID = "exportShapefile";

    @SuppressWarnings("FieldCanBeLocal")
    // If converted to local the result gets garbage collected and the listener is not informed anymore
    private final Lookup.Result<VectorDataNode> result;
    private final Lookup lookup;
    private VectorDataNode vectorDataNode;


    @SuppressWarnings("unused")
    public ExportGeometryAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportGeometryAction(Lookup lookup) {
        super(Bundle.CTL_ExportGeometryAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(VectorDataNode.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        vectorDataNode = lookup.lookup(VectorDataNode.class);
        setEnabled(vectorDataNode != null);
    }

    /*
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @param visatApp the VISAT application
     * @return the selected file, <code>null</code> means "Cancel"
     */
    private static File promptForFile(String defaultFileName) {
        return Dialogs.requestFileForSave(Bundle.CTL_ExportGeometryAction_DialogTitle(), false,
                                          new SnapFileFilter(ESRI_SHAPEFILE, FILE_EXTENSION_SHAPEFILE, ESRI_SHAPEFILE),
                                          FILE_EXTENSION_SHAPEFILE,
                                          defaultFileName,
                                          null,
                                          "exportVectorDataNode.lastDir");
    }

    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "export Mask Pixels" command
    /////////////////////////////////////////////////////////////////////////

    private static void exportVectorDataNode(VectorDataNode vectorNode, File file, ProgressMonitor pm) throws Exception {


        Map<Class<?>, List<SimpleFeature>> featureListMap = createGeometryToFeaturesListMap(vectorNode);
        if (featureListMap.size() > 1) {
            final String msg = "The selected geometry contains different types of shapes.\n" +
                    "Each type of shape will be exported as a separate shapefile.";
            Dialogs.showInformation(Bundle.CTL_ExportGeometryAction_DialogTitle(),
                                    msg, ExportGeometryAction.class.getName() + ".exportInfo");
        }

        Set<Map.Entry<Class<?>, List<SimpleFeature>>> entries = featureListMap.entrySet();
        pm.beginTask("Writing ESRI Shapefiles...", featureListMap.size());
        try {
            for (Map.Entry<Class<?>, List<SimpleFeature>> entry : entries) {
                writeEsriShapefile(entry.getKey(), entry.getValue(), file);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    static void writeEsriShapefile(Class<?> geomType, List<SimpleFeature> features, File file) throws IOException {
        String geomName = geomType.getSimpleName();
        String basename = file.getName();
        if (basename.endsWith(FILE_EXTENSION_SHAPEFILE)) {
            basename = basename.substring(0, basename.length() - 4);
        }
        File file1 = new File(file.getParentFile(), basename + "_" + geomName + FILE_EXTENSION_SHAPEFILE);

        SimpleFeature simpleFeature = features.get(0);
        SimpleFeatureType simpleFeatureType = changeGeometryType(simpleFeature.getType(), geomType);

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map<String, Serializable> map = Collections.singletonMap("url", file1.toURI().toURL());
        ShapefileDataStore dataStore = (ShapefileDataStore) factory.createNewDataStore(map);
        dataStore.createSchema(simpleFeatureType);
        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        DefaultTransaction transaction = new DefaultTransaction("X");
        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            SimpleFeatureCollection collection = new ListFeatureCollection(simpleFeatureType, features);
            featureStore.setTransaction(transaction);
            // I'm not sure why the next line is necessary (mp/20170627)
            // Without it is not working, the wrong feature type is used for writing
            // But it is not mentioned in the tutorials
            dataStore.getEntry(featureSource.getName()).getState(transaction).setFeatureType(simpleFeatureType);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception problem) {
                transaction.rollback();
                throw new IOException(problem);
            } finally {
                transaction.close();
            }
        } else {
            throw new IOException(typeName + " does not support read/write access");
        }
    }

    private static Map<Class<?>, List<SimpleFeature>> createGeometryToFeaturesListMap(VectorDataNode vectorNode) throws TransformException,
                                                                                                                        SchemaException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = vectorNode.getFeatureCollection();
        CoordinateReferenceSystem crs = vectorNode.getFeatureType().getCoordinateReferenceSystem();
        if (crs == null) {   // for pins and GCPs crs is null
            crs = vectorNode.getProduct().getSceneCRS();
        }
        final CoordinateReferenceSystem modelCrs;
        if (vectorNode.getProduct().getSceneGeoCoding() instanceof CrsGeoCoding) {
            modelCrs = vectorNode.getProduct().getSceneCRS();
        } else {
            modelCrs = DefaultGeographicCRS.WGS84;
        }

        // Not using ReprojectingFeatureCollection - it is reprojecting all geometries of a feature
        // but we want to reproject the default geometry only
        GeometryCoordinateSequenceTransformer transformer = createTransformer(crs, modelCrs);

        Map<Class<?>, List<SimpleFeature>> featureListMap = new HashMap<>();
        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        // The schema needs to be reprojected. We need to build a new feature be cause we can't change the schema.
        // It is necessary to have this reprojected schema, because otherwise the shapefile is not correctly georeferenced.
        SimpleFeatureType schema = featureCollection.getSchema();
        SimpleFeatureType transformedSchema = FeatureTypes.transform(schema, modelCrs);
        while (featureIterator.hasNext()) {
            SimpleFeature feature = featureIterator.next();
            Object defaultGeometry = feature.getDefaultGeometry();
            feature.setDefaultGeometry(transformer.transform((Geometry) defaultGeometry));

            Class<?> geometryType = defaultGeometry.getClass();
            List<SimpleFeature> featureList = featureListMap.computeIfAbsent(geometryType, k -> new ArrayList<>());
            SimpleFeature exportFeature = SimpleFeatureBuilder.build(transformedSchema, feature.getAttributes(), feature.getID());
            featureList.add(exportFeature);
        }
        return featureListMap;
    }

    private static GeometryCoordinateSequenceTransformer createTransformer(CoordinateReferenceSystem crs, CoordinateReferenceSystem modelCrs) {
        GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
        try {
            MathTransform reprojTransform = CRS.findMathTransform(crs, modelCrs, true);
            transformer.setMathTransform(reprojTransform);
            return transformer;
        } catch (FactoryException e) {
            throw new IllegalStateException("Could not create math transform", e);
        }
    }


    static SimpleFeatureType changeGeometryType(SimpleFeatureType original, Class<?> geometryType) {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.setCRS(original.getCoordinateReferenceSystem());
        sftb.setDefaultGeometry(original.getGeometryDescriptor().getLocalName());
        boolean defaultGeometryAdded = false;
        for (AttributeDescriptor descriptor : original.getAttributeDescriptors()) {
            if (original.getGeometryDescriptor().getLocalName().equals(descriptor.getLocalName())) {
                sftb.add(descriptor.getLocalName(), geometryType);
                defaultGeometryAdded = true;
            }else {
                sftb.add(descriptor);
            }
        }
        if(!defaultGeometryAdded) {
            sftb.add(original.getGeometryDescriptor().getLocalName(), geometryType);
        }
        sftb.setName("FT_" + geometryType.getSimpleName());
        return sftb.buildFeatureType();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        exportVectorDataNode();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ExportGeometryAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        vectorDataNode = lookup.lookup(VectorDataNode.class);
        setEnabled(vectorDataNode != null);
    }

    /**
     * Performs the actual "export Mask Pixels" command.
     */
    private void exportVectorDataNode() {
        SnapApp snapApp = SnapApp.getDefault();
        if (vectorDataNode.getFeatureCollection().isEmpty()) {
            Dialogs.showInformation(Bundle.CTL_ExportGeometryAction_DialogTitle(),
                                    "The selected geometry is empty. Nothing to export.", null);
            return;
        }

        final File file = promptForFile(vectorDataNode.getName());
        if (file == null) {
            return;
        }
        final SwingWorker<Exception, Object> swingWorker = new ExportVectorNodeSwingWorker(snapApp, vectorDataNode, file);

        UIUtils.setRootFrameWaitCursor(snapApp.getMainFrame());
        snapApp.setStatusBarMessage("Exporting Geometry...");

        swingWorker.execute();
    }


    private static class ExportVectorNodeSwingWorker extends ProgressMonitorSwingWorker<Exception, Object> {

        private final SnapApp snapApp;
        private final VectorDataNode vectorDataNode;
        private final File file;

        private ExportVectorNodeSwingWorker(SnapApp snapApp, VectorDataNode vectorDataNode, File file) {
            super(snapApp.getMainFrame(), Bundle.CTL_ExportGeometryAction_DialogTitle());
            this.snapApp = snapApp;
            this.vectorDataNode = vectorDataNode;
            this.file = file;
        }

        @Override
        protected Exception doInBackground(ProgressMonitor pm) throws Exception {
            try {
                exportVectorDataNode(vectorDataNode, file, pm);
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread) after the <code>construct</code> method
         * has returned.
         */
        @Override
        public void done() {
            Exception exception = null;
            try {
                UIUtils.setRootFrameDefaultCursor(SnapApp.getDefault().getMainFrame());
                snapApp.setStatusBarMessage("");
                exception = get();
            } catch (InterruptedException | ExecutionException e) {
                exception = e;
            } finally {
                if (exception != null) {
                    exception.printStackTrace();
                    Dialogs.showError(Bundle.CTL_ExportGeometryAction_DialogTitle(),
                                          "Can not export geometry.\n" + exception.getMessage());
                }
            }
        }

    }
}
