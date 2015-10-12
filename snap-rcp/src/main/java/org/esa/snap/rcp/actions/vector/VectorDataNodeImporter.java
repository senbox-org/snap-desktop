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
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.vividsolutions.jts.geom.Polygonal;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.opengis.feature.type.GeometryDescriptor;
import org.openide.util.HelpCtx;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

// todo - test with shapefile that has no CRS (nf, 2012-04-05)

public class VectorDataNodeImporter implements HelpCtx.Provider {

    private final String dialogTitle;
    private final String shapeIoDirPreferencesKey;
    private String helpId;
    private SnapFileFilter filter;
    private final VectorDataNodeReader reader;

    public VectorDataNodeImporter(String helpId, SnapFileFilter filter, VectorDataNodeReader reader, String dialogTitle, String shapeIoDirPreferencesKey) {
        this.helpId = helpId;
        this.filter = filter;
        this.reader = reader;
        this.dialogTitle = dialogTitle;
        this.shapeIoDirPreferencesKey = shapeIoDirPreferencesKey;
    }

    public void importGeometry(final SnapApp snapApp) {
        final Preferences preferences = snapApp.getPreferences();
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(getIODir(preferences));
        final int result = fileChooser.showOpenDialog(snapApp.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(preferences, file.getAbsoluteFile().getParentFile());
                importGeometry(snapApp, file);
            }
        }
    }

    private void importGeometry(final SnapApp snapApp, final File file) {
        final Product product = snapApp.getSelectedProduct();
        if (product == null) {
            return;
        }

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null || !geoCoding.canGetPixelPos()) {
            SnapDialogs.showError(dialogTitle, "Failed to import vector data.\n"
                    +
                    "Current geo-coding cannot convert from geographic to pixel coordinates."); /* I18N */
            return;
        }

        VectorDataNode vectorDataNode;
        try {
            vectorDataNode = readGeometry(snapApp, file, product);
            if (vectorDataNode == null) {
                return;
            }
        } catch (Exception e) {
            SnapDialogs.showError(dialogTitle, "Failed to import vector data.\n" + "An I/O Error occurred:\n"
                    + e.getMessage()); /* I18N */
            Debug.trace(e);
            return;
        }

        if (vectorDataNode.getFeatureCollection().isEmpty()) {
            SnapDialogs.showError(dialogTitle, "The vector data was loaded successfully,\n"
                    + "but no part is located within the scene boundaries."); /* I18N */
            return;
        }

        boolean individualShapes = false;
        String attributeName = null;
        GeometryDescriptor geometryDescriptor = vectorDataNode.getFeatureType().getGeometryDescriptor();
        int featureCount = vectorDataNode.getFeatureCollection().size();
        if (featureCount > 1
                && geometryDescriptor != null
                && Polygonal.class.isAssignableFrom(geometryDescriptor.getType().getBinding())) {

            String text = "<html>" +
                    "The vector data set contains <b>" +
                    featureCount + "</b> polygonal shapes.<br>" +
                    "Shall they be imported separately?<br>" +
                    "<br>" +
                    "If you select <b>Yes</b>, the polygons can be used as individual masks<br>" +
                    "and they will be displayed on individual layers.</i>";
            SeparateGeometriesDialog dialog = new SeparateGeometriesDialog(snapApp.getMainFrame(), vectorDataNode, helpId,
                    text);

            int response = dialog.show();
            if (response == ModalDialog.ID_CANCEL) {
                return;
            }

            individualShapes = response == ModalDialog.ID_YES;
            attributeName = dialog.getSelectedAttributeName();
        }

        VectorDataNode[] vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, individualShapes, attributeName);
        for (VectorDataNode vectorDataNode1 : vectorDataNodes) {
            product.getVectorDataGroup().add(vectorDataNode1);
        }

        setLayersVisible(vectorDataNodes);
    }

    private void setLayersVisible(VectorDataNode[] vectorDataNodes) {
        final ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
        if (sceneView != null) {
            sceneView.setLayersVisible(vectorDataNodes);
        }
    }

    public static String findUniqueVectorDataNodeName(String suggestedName, ProductNodeGroup<VectorDataNode> vectorDataGroup) {
        String name = suggestedName;
        int index = 1;
        while (vectorDataGroup.contains(name)) {
            name = suggestedName + "_" + index;
            index++;
        }
        return name;
    }

    private File getIODir(final Preferences preferences) {
        final File dir = SystemUtils.getUserHomeDir();
        return new File(preferences.get(shapeIoDirPreferencesKey, dir.getPath()));
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(helpId);
    }

    public interface VectorDataNodeReader {

        VectorDataNode readVectorDataNode(File file, Product product, ProgressMonitor pm) throws IOException;
    }

    private VectorDataNode readGeometry(final SnapApp snapApp,
                                        final File file,
                                        final Product product)
            throws IOException, ExecutionException, InterruptedException {

        ProgressMonitorSwingWorker<VectorDataNode, Object> worker = new ProgressMonitorSwingWorker<VectorDataNode, Object>(snapApp.getMainFrame(), "Loading vector data") {
            @Override
            protected VectorDataNode doInBackground(ProgressMonitor pm) throws Exception {
                return reader.readVectorDataNode(file, product, pm);
            }

            @Override
            protected void done() {
                super.done();
            }
        };

        worker.executeWithBlocking();
        return worker.get();
    }

    private void setIODir(final Preferences preferences, final File dir) {
        if (dir != null) {
            preferences.put(shapeIoDirPreferencesKey, dir.getPath());
        }
    }

}
