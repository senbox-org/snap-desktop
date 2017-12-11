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
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageLegend;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapTransformDescriptor;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
import javax.swing.JFileChooser;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportKmzFileAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportKmzFileAction_MenuText",
        popupText = "#CTL_ExportKmzFileAction_PopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other", position = 80),
        @ActionReference(path = "Context/ProductSceneView", position = 60)
})
@NbBundle.Messages({
        "CTL_ExportKmzFileAction_MenuText=View as Google Earth KMZ",
        "CTL_ExportKmzFileAction_PopupText=Export View as Google Earth KMZ",
        "CTL_ExportKmzFileAction_ShortDescription=Export View as Google Earth KMZ."
})
public class ExportKmzFileAction extends AbstractAction implements HelpCtx.Provider, ContextAwareAction, LookupListener {
    private static final String OVERLAY_KML = "overlay.kml";
    private static final String OVERLAY_PNG = "overlay.png";
    private static final String IMAGE_TYPE = "PNG";
    private static final String LEGEND_PNG = "legend.png";
    private static final String[] KMZ_FORMAT_DESCRIPTION = {"KMZ", "kmz", "KMZ - Google Earth File Format"};
    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";
    private static final String HELP_ID = "exportKmzFile";

    @SuppressWarnings("FieldCanBeLocal")
    private final Lookup.Result<ProductSceneView> result;


    public ExportKmzFileAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportKmzFileAction(Lookup lookup) {
        super(Bundle.CTL_ExportKmzFileAction_MenuText());
        putValue("popupText",Bundle.CTL_ExportKmzFileAction_PopupText());
        result = lookup.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        final GeoCoding geoCoding = view.getProduct().getSceneGeoCoding();
        boolean isGeographic = false;
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                isGeographic = true;
            }
        } else if (geoCoding instanceof CrsGeoCoding) {
            isGeographic = CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }

        if (isGeographic) {
            exportImage(view);
        } else {
            String message = "Product must be in ''Geographic Lat/Lon'' projection.";
            Dialogs.showInformation(message, null);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }


    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ExportKmzFileAction(lookup);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnabled(SnapApp.getDefault().getSelectedProductSceneView() != null);
    }


    private void exportImage(ProductSceneView sceneView) {
        SnapApp snapApp = SnapApp.getDefault();
        final String lastDir = Config.instance().load().preferences().get(
                IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final SnapFileChooser fileChooser = new SnapFileChooser();

        HelpCtx.setHelpIDString(fileChooser, getHelpCtx().getHelpID());

        SnapFileFilter kmzFileFilter = new SnapFileFilter(KMZ_FORMAT_DESCRIPTION[0],
                                                          KMZ_FORMAT_DESCRIPTION[1],
                                                          KMZ_FORMAT_DESCRIPTION[2]);

        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(kmzFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(snapApp.getInstanceName() + " - " + "export KMZ");
        final String currentFilename = sceneView.isRGB() ? "RGB" : sceneView.getRaster().getName();
        fileChooser.setCurrentFilename(currentFilename);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int result = fileChooser.showSaveDialog(snapApp.getMainFrame());
        File file = fileChooser.getSelectedFile();
        fileChooser.addPropertyChangeListener(evt -> {
            // @todo never comes here, why?
            Debug.trace(evt.toString());
        });
        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            Config.instance().load().preferences().get(
                    IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                    currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (file == null || file.getName().isEmpty()) {
            return;
        }

        if (!Dialogs.requestOverwriteDecision("h", file)) {
            return;
        }

        final SaveKMLSwingWorker worker = new SaveKMLSwingWorker(snapApp, "Save KMZ", sceneView, file);
        worker.executeWithBlocking();
    }

    private static RenderedImage createImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = initImageLegend(raster);
        return imageLegend.createImage();
    }

    private static String formatKML(ProductSceneView view, String imageName) {
        final RasterDataNode raster = view.getRaster();
        final Product product = raster.getProduct();
        final GeoCoding geoCoding = raster.getGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0, 0);
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight());
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
        double eastLon = lowerRightGP.getLon();
        if (geoCoding.isCrossingMeridianAt180()) {
            eastLon += 360;
        }

        String pinKml = "";
        ProductNodeGroup<Placemark> pinGroup = product.getPinGroup();
        Placemark[] pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        for (Placemark placemark : pins) {
            GeoPos geoPos = placemark.getGeoPos();
            if (geoPos != null && product.containsPixel(placemark.getPixelPos())) {
                pinKml += String.format(
                        "<Placemark>\n"
                                + "  <name>%s</name>\n"
                                + "  <Point>\n"
                                + "    <coordinates>%f,%f,0</coordinates>\n"
                                + "  </Point>\n"
                                + "</Placemark>\n",
                        placemark.getLabel(),
                        geoPos.lon,
                        geoPos.lat);
            }
        }

        String name;
        String description;
        String legendKml = "";
        if (view.isRGB()) {
            name = "RGB";
            description = view.getSceneName() + "\n" + product.getName();
        } else {
            name = raster.getName();
            description = raster.getDescription() + "\n" + product.getName();
            legendKml = "  <ScreenOverlay>\n"
                    + "    <name>Legend</name>\n"
                    + "    <Icon>\n"
                    + "      <href>legend.png</href>\n"
                    + "    </Icon>\n"
                    + "    <overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                    + "    <screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                    + "  </ScreenOverlay>\n";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<kml xmlns=\"http://earth.google.com/kml/2.0\">\n"
                + "<Document>\n"
                + "  <name>" + name + "</name>\n"
                + "  <description>" + description + "</description>\n"
                + "  <GroundOverlay>\n"
                + "    <name>Raster data</name>\n"
                + "    <LatLonBox>\n"
                + "      <north>" + upperLeftGP.getLat() + "</north>\n"
                + "      <south>" + lowerRightGP.getLat() + "</south>\n"
                + "      <east>" + eastLon + "</east>\n"
                + "      <west>" + upperLeftGP.getLon() + "</west>\n"
                + "    </LatLonBox>\n"
                + "    <Icon>\n"
                + "      <href>" + imageName + "</href>\n"
                + "    </Icon>\n"
                + "  </GroundOverlay>\n"
                + legendKml
                + pinKml
                + "</Document>\n"
                + "</kml>\n";
    }

    private static ImageLegend initImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = new ImageLegend(raster.getImageInfo(), raster);

        imageLegend.setHeaderText(getLegendHeaderText(raster));
        imageLegend.setOrientation(ImageLegend.VERTICAL);
        imageLegend.setBackgroundTransparency(0.0f);
        imageLegend.setBackgroundTransparencyEnabled(true);
        imageLegend.setAntialiasing(true);

        return imageLegend;
    }

    private static String getLegendHeaderText(RasterDataNode raster) {
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        return "(" + unit + ")";
    }


    private static class SaveKMLSwingWorker extends ProgressMonitorSwingWorker {

        private final SnapApp snapApp;
        private final ProductSceneView view;
        private final File file;

        SaveKMLSwingWorker(SnapApp snapApp, String message, ProductSceneView view, File file) {
            super(snapApp.getMainFrame(), message);
            this.snapApp = snapApp;
            this.view = view;
            this.file = file;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            try {
                final String message = String.format("Saving image as %s...", file.getPath());
                pm.beginTask(message, view.isRGB() ? 4 : 3);
                snapApp.setStatusBarMessage(message);
                snapApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                final Dimension dimension = new Dimension(view.getProduct().getSceneRasterWidth(),
                                                          view.getProduct().getSceneRasterHeight());
                RenderedImage image = ExportImageAction.createImage(view, true, dimension, true, true);
                pm.worked(1);
                try (ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(file))) {
                    outStream.putNextEntry(new ZipEntry(OVERLAY_KML));
                    final String kmlContent = formatKML(view, OVERLAY_PNG);
                    outStream.write(kmlContent.getBytes());
                    pm.worked(1);

                    outStream.putNextEntry(new ZipEntry(OVERLAY_PNG));
                    ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                    encoder.encode(image);
                    pm.worked(1);

                    if (!view.isRGB()) {
                        outStream.putNextEntry(new ZipEntry(LEGEND_PNG));
                        encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                        encoder.encode(createImageLegend(view.getRaster()));
                        pm.worked(1);
                    }
                }
            } catch (OutOfMemoryError ignored) {
                Dialogs.showOutOfMemoryError("The image could not be exported."); /*I18N*/
            } catch (Throwable e) {
                snapApp.handleError("The image could not be exported", e);
            } finally {
                snapApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
                snapApp.setStatusBarMessage("");
                pm.done();
            }
            return null;
        }
    }

}
