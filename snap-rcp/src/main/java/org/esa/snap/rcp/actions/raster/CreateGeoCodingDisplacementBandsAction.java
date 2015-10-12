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

package org.esa.snap.rcp.actions.raster;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.preferences.general.UiBehaviorController;
import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@ActionID(
        category = "Tools",
        id = "CreateGeoCodingDisplacementBandsAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateGeoCodingDisplacementBandsActionText",
        popupText = "#CTL_CreateGeoCodingDisplacementBandsActionText"
)
@ActionReference(path = "Menu/Raster", position = 30 )
@NbBundle.Messages({
                  "CTL_CreateGeoCodingDisplacementBandsActionText=Geo-Coding Displacement Bands...",
                  "CTL_CreateGeoCodingDisplacementBandsDialogTitle=Geo-Coding Displacement Bands",
                  "CTL_CreateGeoCodingDisplacementBandsDescription=&lt;html&gt;Computes actual pixel position minus pixel position computed from inverse\n" +
                          "                geo-coding&lt;br/&gt;\n" +
                          "                and adds displacements as new bands (test for geo-coding accuracy)."
          })
/**
 * An action that lets users add a number of bands that can be used to assess the performance/accuracy
 * of the current geo-coding.
 */
public class CreateGeoCodingDisplacementBandsAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup lookup;
    public static final float[][] OFFSETS = new float[][]{

            {0.00f, 0.00f},
            {0.25f, 0.25f},
            {0.50f, 0.50f},
            {0.75f, 0.75f},

            {0.25f, 0.75f},
            {0.75f, 0.25f},
    };

    public CreateGeoCodingDisplacementBandsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CreateGeoCodingDisplacementBandsAction(Lookup lookup) {
        super(Bundle.CTL_CreateGeoCodingDisplacementBandsActionText());
        this.lookup = lookup;
        Lookup.Result<ProductNode> lkpContext = lookup.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_CreateGeoCodingDisplacementBandsDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CreateGeoCodingDisplacementBandsAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        createXYDisplacementBands(lookup.lookup(ProductNode.class).getProduct());
    }

    private void setEnableState() {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        boolean state = false;
        if (productNode != null) {
            Product product = productNode.getProduct();
            state = product != null && product.getSceneGeoCoding() != null &&
                    product.getSceneGeoCoding().canGetGeoPos() && product.getSceneGeoCoding().canGetPixelPos();
        }
        setEnabled(state);
    }

    private void createXYDisplacementBands(Product product) {
        final SnapApp snapApp = SnapApp.getDefault();
        final Frame mainFrame = snapApp.getMainFrame();
        String dialogTitle = Bundle.CTL_CreateGeoCodingDisplacementBandsDialogTitle();
        final ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker<Band[], Object>(mainFrame, dialogTitle) {
            @Override
            protected Band[] doInBackground(ProgressMonitor pm) throws Exception {
                final Band[] xyDisplacementBands = createXYDisplacementBands(product, pm);
                UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
                if (undoManager != null) {
                    undoManager.addEdit(new UndoableDisplacementBandsCreation(product, xyDisplacementBands));
                }
                return xyDisplacementBands;
            }

            @Override
            public void done() {
                if (snapApp.getPreferences().getBoolean(UiBehaviorController.PREFERENCE_KEY_AUTO_SHOW_NEW_BANDS, true)) {
                    try {
                        Band[] bands = get();
                        if (bands == null) {
                            return;
                        }
                        for (Band band : bands) {
                            Band oldBand = product.getBand(band.getName());
                            if (oldBand != null) {
                                product.removeBand(oldBand);
                            }
                            product.addBand(band);
                        }
                    } catch (Exception e) {
                        Throwable cause = e;
                        if (e instanceof ExecutionException) {
                            cause = e.getCause();
                        }
                        String msg = "An internal error occurred:\n" + e.getMessage();
                        if (cause instanceof IOException) {
                            msg = "An I/O error occurred:\n" + e.getMessage();
                        }
                        SnapDialogs.showError(dialogTitle, msg);
                    } finally {
                        UIUtils.setRootFrameDefaultCursor(mainFrame);
                    }
                }
            }
        };

        swingWorker.execute();
    }

    private static Band[] createXYDisplacementBands(final Product product, ProgressMonitor pm) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        ImageInfo blueToRedGrad = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(-1.0, Color.BLUE),
                new ColorPaletteDef.Point(0.0, Color.WHITE),
                new ColorPaletteDef.Point(1.0, Color.RED),
        }));
        ImageInfo amplGrad = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(0.0, Color.WHITE),
                new ColorPaletteDef.Point(1.0, Color.RED),
        }));
        ImageInfo phaseGrad = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(-Math.PI, Color.WHITE),
                new ColorPaletteDef.Point(0.0, Color.BLUE),
                new ColorPaletteDef.Point(+Math.PI, Color.WHITE),
        }));

        final Band bandX = new Band("gc_displ_x", ProductData.TYPE_FLOAT64, width, height);
        configureBand(bandX, blueToRedGrad.clone(), "pixels", "Geo-coding X-displacement");

        final Band bandY = new Band("gc_displ_y", ProductData.TYPE_FLOAT64, width, height);
        configureBand(bandY, blueToRedGrad.clone(), "pixels", "Geo-coding Y-displacement");

        final Band bandAmpl = new VirtualBand("gc_displ_ampl",
                                              ProductData.TYPE_FLOAT64, width, height,
                                              "ampl(gc_displ_x, gc_displ_y)");
        configureBand(bandAmpl, amplGrad.clone(), "pixels", "Geo-coding displacement amplitude");

        final Band bandPhase = new VirtualBand("gc_displ_phase",
                                               ProductData.TYPE_FLOAT64, width, height,
                                               "phase(gc_displ_x, gc_displ_y)");
        configureBand(bandPhase, phaseGrad.clone(), "radians", "Geo-coding displacement phase");

        final double[] dataX = new double[width * height];
        final double[] dataY = new double[width * height];

        bandX.setRasterData(ProductData.createInstance(dataX));
        bandY.setRasterData(ProductData.createInstance(dataY));

        pm.beginTask("Computing geo-coding displacements for product '" + product.getName() + "'...", height);
        try {
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos1 = new PixelPos();
            final PixelPos pixelPos2 = new PixelPos();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double maxX = 0;
                    double maxY = 0;
                    double valueX = 0;
                    double valueY = 0;
                    for (float[] offset : OFFSETS) {
                        pixelPos1.setLocation(x + offset[0], y + offset[1]);
                        product.getSceneGeoCoding().getGeoPos(pixelPos1, geoPos);
                        product.getSceneGeoCoding().getPixelPos(geoPos, pixelPos2);
                        double dx = pixelPos2.x - pixelPos1.x;
                        double dy = pixelPos2.y - pixelPos1.y;
                        if (Math.abs(dx) > maxX) {
                            maxX = Math.abs(dx);
                            valueX = dx;
                        }
                        if (Math.abs(dy) > maxY) {
                            maxY = Math.abs(dy);
                            valueY = dy;
                        }
                    }
                    dataX[y * width + x] = valueX;
                    dataY[y * width + x] = valueY;
                }
                if (pm.isCanceled()) {
                    return null;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        return new Band[]{bandX, bandY, bandAmpl, bandPhase};
    }

    private static void configureBand(Band band05X, ImageInfo imageInfo, String unit, String description) {
        band05X.setUnit(unit);
        band05X.setDescription(description);
        band05X.setImageInfo(imageInfo);
        band05X.setNoDataValue(Double.NaN);
        band05X.setNoDataValueUsed(true);
    }

    private static class UndoableDisplacementBandsCreation extends AbstractUndoableEdit {

        private Band[] displacementBands;
        private Product product;


        public UndoableDisplacementBandsCreation(Product product, Band[] displacementBands) {
            Assert.notNull(product, "product");
            Assert.notNull(displacementBands, "displacementBands");
            this.product = product;
            this.displacementBands = displacementBands;
        }


        @Override
        public String getPresentationName() {
            return Bundle.CTL_CreateGeoCodingDisplacementBandsDialogTitle();
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            for (Band displacementBand : displacementBands) {
                if (product.containsBand(displacementBand.getName())) {
                    product.removeBand(displacementBand);
                }
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            for (Band displacementBand : displacementBands) {
                if (!product.containsBand(displacementBand.getName())) {
                    product.addBand(displacementBand);
                    product.fireProductNodeChanged(displacementBand.getName());
                }
            }
        }

        @Override
        public void die() {
            product = null;
            displacementBands = null;
        }

    }

}
