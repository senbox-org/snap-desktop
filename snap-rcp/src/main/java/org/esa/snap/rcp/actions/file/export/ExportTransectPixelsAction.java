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

package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.TransectProfileData;
import org.esa.snap.core.datamodel.TransectProfileDataBuilder;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.pixelinfo.PixelInfoViewUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.SelectExportMethodDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
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
import javax.swing.JCheckBox;
import javax.swing.SwingWorker;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;


@ActionID(category = "File", id = "org.esa.snap.rcp.actions.file.export.ExportTransectPixelsAction" )
@ActionRegistration(
        displayName = "#CTL_ExportTransectPixelsAction_MenuText",
        popupText = "#CTL_ExportTransectPixelsAction_PopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other",position = 60 ),
        @ActionReference(path = "Menu/Raster/Export", position = 200),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 50, separatorAfter = 55),
        @ActionReference(path = "Context/ProductSceneView" , position = 40)
})
@NbBundle.Messages({
        "CTL_ExportTransectPixelsAction_MenuText=Transect Pixels",
        "CTL_ExportTransectPixelsAction_PopupText=Export Transect Pixels",
        "CTL_ExportTransectPixelsAction_DialogTitle=Export Transect Pixels",
        "CTL_ExportTransectPixelsAction_ShortDescription=Export Transect Pixels."
})

public class ExportTransectPixelsAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private static final String ERR_MSG_BASE = "Transect pixels cannot be exported:\n";

    private final Lookup.Result<FigureSelection> result;

    public ExportTransectPixelsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportTransectPixelsAction(Lookup lkp) {
        super(Bundle.CTL_ExportTransectPixelsAction_MenuText());
        putValue("popupText", Bundle.CTL_ExportTransectPixelsAction_PopupText());
        result = lkp.lookupResult(FigureSelection.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        updateEnableState(getCurrentFigureSelection());
    }

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        exportTransectPixels();
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new ExportTransectPixelsAction(lkp);
    }

    @Override
    public void resultChanged(LookupEvent le) {
        updateEnableState(getCurrentFigureSelection());
    }

    private void exportTransectPixels() {

        // Get current view showing a product's band
        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view == null) {
            return;
        }
        final FigureSelection selection = getCurrentFigureSelection();

        // Get the displayed raster data node (band or tie-point grid)
        final RasterDataNode raster = view.getRaster();
        // Get the transect of the displayed raster data node
        ShapeFigure transect = null;
        if (selection.getFigureCount() > 0) {
            Figure figure = selection.getFigure(0);
            if (figure instanceof ShapeFigure) {
                transect = (ShapeFigure) figure;
            }
        }

        if (transect == null) {
            Dialogs.showError(Bundle.CTL_ExportTransectPixelsAction_DialogTitle(),
                    ERR_MSG_BASE + "There is no transect defined in the selected band.");
            return;
        }

        final TransectProfileData transectProfileData;
        try {
            transectProfileData = new TransectProfileDataBuilder()
                    .raster(raster)
                    .path(transect.getShape())
                    .build();
        } catch (IOException e) {
            Dialogs.showError(Bundle.CTL_ExportTransectPixelsAction_DialogTitle(),
                    ERR_MSG_BASE + "An I/O error occurred:\n" + e.getMessage());
            return;
        }

        // Compute total number of transect pixels
        final int numTransectPixels = getNumTransectPixels(raster.getProduct(), transectProfileData);

        String numPixelsText;
        if (numTransectPixels == 1) {
            numPixelsText = "One transect pixel will be exported.\n";
        } else {
            numPixelsText = numTransectPixels + " transect pixels will be exported.\n";
        }
        // Get export method from user
        final String questionText = "How do you want to export the pixel values?\n";
        final JCheckBox createHeaderBox = new JCheckBox("Create header");
        final JCheckBox exportTiePointsBox = new JCheckBox("Export tie-points");
        final JCheckBox exportWavelengthsAndSFBox = new JCheckBox("Export wavelengths + solar fluxes");
        final int method = SelectExportMethodDialog.run(SnapApp.getDefault().getMainFrame(), getWindowTitle(),
                questionText + numPixelsText, new JCheckBox[]{
                        createHeaderBox,
                        exportTiePointsBox,
                        exportWavelengthsAndSFBox
                }, "exportTransectPixels");

        final PrintWriter out;
        final StringBuffer clipboardText;
        final int initialBufferSize = 256000;
        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            final StringWriter stringWriter = new StringWriter(initialBufferSize);
            out = new PrintWriter(stringWriter);
            clipboardText = stringWriter.getBuffer();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            final File file = promptForFile(createDefaultFileName(raster));
            if (file == null) {
                return; // Cancel
            }
            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                Dialogs.showError(Bundle.CTL_ExportTransectPixelsAction_DialogTitle(),
                        ERR_MSG_BASE + "Failed to create file '" + file + "':\n" + e.getMessage());
                return; // Error
            }
            out = new PrintWriter(new BufferedWriter(fileWriter, initialBufferSize));
            clipboardText = null;
        } else {
            return; // Cancel
        }

        final SwingWorker<Exception, Object> swingWorker = new SwingWorker<Exception, Object>() {

            @Override
            protected Exception doInBackground() throws Exception {
                Exception returnValue = null;
                ProgressMonitor pm = new DialogProgressMonitor(SnapApp.getDefault().getMainFrame(), Bundle.CTL_ExportTransectPixelsAction_DialogTitle(),
                        Dialog.ModalityType.APPLICATION_MODAL);
                try {
                    final boolean mustCreateHeader = createHeaderBox.isSelected();
                    final boolean mustExportWavelengthsAndSF = exportWavelengthsAndSFBox.isSelected();
                    final boolean mustExportTiePoints = exportTiePointsBox.isSelected();
                    TransectExporter exporter = new TransectExporter(mustCreateHeader, mustExportWavelengthsAndSF, mustExportTiePoints);
                    boolean success = exporter.exportTransectPixels(out, view,
                            transectProfileData,
                            numTransectPixels,
                            pm);
                    if (success && clipboardText != null) {
                        SystemUtils.copyToClipboard(clipboardText.toString());
                        clipboardText.setLength(0);
                    }
                } catch (Exception e) {
                    returnValue = e;
                } finally {
                    out.close();
                }
                return returnValue;
            }

            @Override
            public void done() {
                // clear status bar
                SnapApp.getDefault().setStatusBarMessage("");
                // show default-cursor
                UIUtils.setRootFrameDefaultCursor(SnapApp.getDefault().getMainFrame());
                // On error, show error message
                Exception exception;
                try {
                    exception = get();
                } catch (Exception e) {
                    exception = e;
                }
                if (exception != null) {
                    Dialogs.showError(Bundle.CTL_ExportTransectPixelsAction_DialogTitle(),
                            ERR_MSG_BASE + exception.getMessage());
                }
            }
        };

        // show wait-cursor
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        // show message in status bar
        SnapApp.getDefault().setStatusBarMessage("Exporting transect pixels...");

        // Start separate worker thread.
        swingWorker.execute();
    }

    private FigureSelection getCurrentFigureSelection() {
        return result.allInstances().stream().findFirst().orElse(null);
    }

    private void updateEnableState(FigureSelection figureSelection) {
        setEnabled(figureSelection != null);
    }

    private static String createDefaultFileName(final RasterDataNode raster) {
        return FileUtils.getFilenameWithoutExtension(raster.getProduct().getName()) + "_TRANSECT.txt";
    }

    private static String getWindowTitle() {
        return SnapApp.getDefault().getInstanceName() + " - " + Bundle.CTL_ExportTransectPixelsAction_DialogTitle();
    }

    /**
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @return the selected file, {@code null} means "Cancel"
     */
    private static File promptForFile(String defaultFileName) {
        final SnapFileFilter fileFilter = new SnapFileFilter("TXT", "txt", "Text");
        return Dialogs.requestFileForSave(Bundle.CTL_ExportTransectPixelsAction_DialogTitle(),
                false,
                fileFilter,
                ".txt",
                defaultFileName,
                null,
                "exportTransectPixels.lastDir");
    }

    private static int getNumTransectPixels(final Product product,
                                            final TransectProfileData transectProfileData) {

        final Point2D[] pixelPositions = transectProfileData.getPixelPositions();
        int numTransectPixels = 0;
        for (Point2D pixelPosition : pixelPositions) {
            int x = (int) Math.floor(pixelPosition.getX());
            int y = (int) Math.floor(pixelPosition.getY());
            if (x >= 0 && x < product.getSceneRasterWidth()
                    && y >= 0 && y < product.getSceneRasterHeight()) {
                numTransectPixels++;
            }
        }
        return numTransectPixels;
    }


    static class TransectExporter {

        private final boolean mustCreateHeader;
        private final boolean mustExportWavelengthsAndSF;
        private final boolean mustExportTiePoints;

        TransectExporter(boolean mustCreateHeader, boolean mustExportWavelengthsAndSF, boolean mustExportTiePoints) {
            this.mustCreateHeader = mustCreateHeader;
            this.mustExportWavelengthsAndSF = mustExportWavelengthsAndSF;
            this.mustExportTiePoints = mustExportTiePoints;
        }

        /**
         * Writes all pixel values of the given product within the given ROI to the specified out.
         *
         * @param out     the data output writer
         * @param view    the product scene view providing the pixel values
         * @return {@code true} for success, {@code false} if export has been terminated (by user)
         */
        private boolean exportTransectPixels(final PrintWriter out,
                                             final ProductSceneView view,
                                             final TransectProfileData transectProfileData,
                                             final int numTransectPixels,
                                             ProgressMonitor pm) {

            final Product product = view.getProduct();
            final boolean isMultiSize = product.isMultiSize();
            final Band[] bands = product.getBands();
            final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
            if (mustCreateHeader) {
                writeFileHeader(out, bands);
            }
            writeTableHeader(out, view.getRaster(), isMultiSize, bands, mustExportTiePoints, tiePointGrids, mustExportWavelengthsAndSF);
            final Point2D[] pixelPositions = transectProfileData.getPixelPositions();

            pm.beginTask("Writing pixel data...", numTransectPixels);
            try {
                for (Point2D pixelPosition : pixelPositions) {
                    int x = MathUtils.floorInt(pixelPosition.getX());
                    int y = MathUtils.floorInt(pixelPosition.getY());
                    final PixelPos pixelPos = new PixelPos(x, y);
                    if (!view.getRaster().getGeoCoding().equals(product.getSceneGeoCoding())) {
                        final GeoPos geoPos = view.getRaster().getGeoCoding().getGeoPos(pixelPos, null);
                        // Update the pixel position to the product resolution
                        product.getSceneGeoCoding().getPixelPos(geoPos, pixelPos);
                    }
                    if (product.containsPixel(pixelPos)) { // Check if the pixel belongs to the product bounds
                        writeDataLine(out, isMultiSize, view, mustExportTiePoints, x, y);
                        pm.worked(1);
                        if (pm.isCanceled()) {
                            return false;
                        }
                    }
                }
            } finally {
                pm.done();
            }

            return true;
        }

        private void writeFileHeader(PrintWriter out, Band[] bands) {

            ProductData.UTC utc = ProductData.UTC.create(new Date(), 0);
            out.printf("# Exported transect on %s%n", utc.format());
            if (bands.length >= 0) {
                Product product = bands[0].getProduct();
                out.printf("# Product name: %s%n", product.getName());
                if (product.getFileLocation() != null) {
                    out.printf("# Product file location: %s%n", product.getFileLocation().getAbsolutePath());
                }
            }
            out.println();

        }

        private void writeTableHeader(final PrintWriter out,
                                      final RasterDataNode raster,
                                      final boolean isMultiSize,
                                      final Band[] bands,
                                      final boolean mustExportTiePoints,
                                      final TiePointGrid[] tiePointGrids,
                                      final boolean mustExportWavelengthsAndSF) {
            if (mustExportWavelengthsAndSF) {
                float[] wavelengthArray = new float[bands.length];
                for (int i = 0; i < bands.length; i++) {
                    wavelengthArray[i] = bands[i].getSpectralWavelength();
                }
                out.printf("# Wavelength:\t \t \t \t%s\n", StringUtils.arrayToString(wavelengthArray, "\t"));

                float[] solarFluxArray = new float[bands.length];
                for (int i = 0; i < bands.length; i++) {
                    solarFluxArray[i] = bands[i].getSolarFlux();
                }
                out.printf("# Solar flux:\t \t \t \t%s%n", StringUtils.arrayToString(solarFluxArray, "\t"));
            }

            if (!isMultiSize){
                out.print("Pixel-X");
                out.print("\t");
                out.print("Pixel-Y");
            } else {
                // Add the band name to the identification of the pixel
                out.print("Pixel-X." + raster.getName());
                out.print("\t");
                out.print("Pixel-Y." + raster.getName());
            }
            if (raster.getGeoCoding() != null) {
                out.print("\t");
                out.print("Longitude");
                out.print("\t");
                out.print("Latitude");
            }
            for (final Band band : bands) {
                out.print("\t");
                out.print(band.getName());
            }
            if (mustExportTiePoints) {
                for (final TiePointGrid grid : tiePointGrids) {
                    out.print("\t");
                    out.print(grid.getName());
                }
            }
            out.print("\n");
        }

        /**
         * Writes a data line of the dataset to be exported for the given pixel position.
         *
         * @param out                    the data output writer
         * @param isMultiSize            if product is multi-size
         * @param view                   the current view
         * @param mustExportTiePoints    if tie-points shall be exported
         * @param x                      the current pixel's X coordinate
         * @param y                      the current pixel's Y coordinate
         */
        private void writeDataLine(final PrintWriter out,
                                   final boolean isMultiSize,
                                   final ProductSceneView view,
                                   final boolean mustExportTiePoints,
                                   final int x, final int y) {

            // Compute the geo position according to the raster resolution
            final PixelPos pixelPosForGeo = new PixelPos(x + 0.5f, y + 0.5f);
            out.print(String.valueOf(pixelPosForGeo.x));
            out.print("\t");
            out.print(String.valueOf(pixelPosForGeo.y));

            final RasterDataNode viewRaster = view.getRaster();
            final GeoCoding rasterGeocoding = viewRaster.getGeoCoding();
            if (rasterGeocoding != null) {
                out.print("\t");
                final GeoPos geoPosForPrint = rasterGeocoding.getGeoPos(pixelPosForGeo, null);
                out.print(String.valueOf(geoPosForPrint.lon));
                out.print("\t");
                out.print(String.valueOf(geoPosForPrint.lat));
            }

            // Compute the scene position (only for multi-size product)
            Point2D.Double scenePos= null;
            if (isMultiSize) {
                scenePos = PixelInfoViewUtils.computeScenePos(view, x, y);
            }

            for (final Band band : view.getProduct().getBands()) {
                String pixelString;

                // Same resolution for band and the view raster: no conversion to be done
                if (band.getImageToModelTransform().equals(viewRaster.getImageToModelTransform()) &&
                        band.getSceneToModelTransform().equals(viewRaster.getSceneToModelTransform())) {
                    if (view.isCurrentPixelPosValid()) {
                        pixelString = band.getPixelString(x, y);
                    } else {
                        pixelString = RasterDataNode.INVALID_POS_TEXT;
                    }
                } else { // Not the same resolution
                    pixelString = PixelInfoViewUtils.getPixelValue(scenePos, band);
                }
                out.print("\t");
                out.print(pixelString);
            }

            if (mustExportTiePoints) {
                for (final TiePointGrid grid : view.getProduct().getTiePointGrids()) {
                    String pixelString;

                    // Same resolution for tie point grid and the view raster: no conversion to be done
                    if (grid.getImageToModelTransform().equals(viewRaster.getImageToModelTransform()) &&
                            grid.getSceneToModelTransform().equals(viewRaster.getSceneToModelTransform())) {
                        if (view.isCurrentPixelPosValid()) {
                            pixelString = grid.getPixelString(x, y);
                        } else {
                            pixelString = RasterDataNode.INVALID_POS_TEXT;
                        }
                    } else { // Not the same resolution
                        pixelString = PixelInfoViewUtils.getPixelValue(scenePos, grid);
                    }
                    out.print("\t");
                    out.print(grid.getPixelString(x, y));
                }
            } // must export Tie Points

            out.print("\n");
        }
    }
}
