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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.layermanager.layersrc.shapefile.SLDUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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
import java.io.IOException;

@ActionID(category = "File", id = "ImportVectorDataNodeFromShapefileAction" )
@ActionRegistration(displayName = "#CTL_ImportVectorDataNodeFromShapefileActionText", lazy = false )
@ActionReferences({
        @ActionReference(path = "Menu/File/Import/Vector Data", position = 20),
        @ActionReference(path = "Menu/Vector/Import")
})
@NbBundle.Messages({
        "CTL_ImportVectorDataNodeFromShapefileActionText=ESRI Shapefile",
        "CTL_ImportVectorDataNodeFromShapefileActionDescription=Import Vector Data Node from Shapefile",
        "CTL_ImportVectorDataNodeFromShapefileActionHelp=importShapefile"
})
public class ImportVectorDataNodeFromShapefileAction extends AbstractImportVectorDataNodeAction implements ContextAwareAction, LookupListener {

    private VectorDataNodeImporter importer;
    private Lookup lookup;
    private final Lookup.Result<Product> result;
    private static final String vector_data_type = "SHAPEFILE";

    public ImportVectorDataNodeFromShapefileAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ImportVectorDataNodeFromShapefileAction(Lookup lookup) {
        this.lookup = lookup;
        result = lookup.lookupResult(Product.class);
        result.addLookupListener(
                WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
        setHelpId(Bundle.CTL_ImportVectorDataNodeFromShapefileActionHelp());
        putValue(Action.NAME, Bundle.CTL_ImportVectorDataNodeFromShapefileActionText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ImportVectorDataNodeFromShapefileActionDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ImportVectorDataNodeFromShapefileAction(lookup);
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

    @Override
    public void actionPerformed(ActionEvent event) {
        final SnapFileFilter filter = new SnapFileFilter(getVectorDataType(),
                                                         new String[]{".shp"},
                                                         "ESRI Shapefiles");
        importer = new VectorDataNodeImporter(getHelpId(), filter, new VdnShapefileReader(), "Import Shapefile", "shape.io.dir");
        importer.importGeometry(SnapApp.getDefault());
    }

    @Override
    protected String getDialogTitle() {
        return importer.getDialogTitle();
    }

    @Override
    protected String getVectorDataType() {
        return vector_data_type;
    }

    class VdnShapefileReader implements VectorDataNodeImporter.VectorDataNodeReader {

        @Override
        public VectorDataNode readVectorDataNode(File file, Product product, ProgressMonitor pm) throws IOException {

            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = FeatureUtils.loadShapefileForProduct(file,
                                                                                                                         product,
                                                                                                                         crsProvider,
                                                                                                                         pm);
            Style[] styles = SLDUtils.loadSLD(file);
            ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
            String name = VectorDataNodeImporter.findUniqueVectorDataNodeName(featureCollection.getSchema().getName().getLocalPart(),
                                                                              vectorDataGroup);
            if (styles.length > 0) {
                SimpleFeatureType featureType = SLDUtils.createStyledFeatureType(featureCollection.getSchema());


                VectorDataNode vectorDataNode = new VectorDataNode(name, featureType);
                FeatureCollection<SimpleFeatureType, SimpleFeature> styledCollection = vectorDataNode.getFeatureCollection();
                String defaultCSS = vectorDataNode.getDefaultStyleCss();
                SLDUtils.applyStyle(styles[0], defaultCSS, featureCollection, styledCollection);
                return vectorDataNode;
            } else {
                return new VectorDataNode(name, featureCollection);
            }
        }
    }

}
