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

package org.esa.snap.timeseries.export.kmz;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapTransformDescriptor;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.core.util.kmz.KmlFeature;
import org.esa.snap.core.util.kmz.KmlFolder;
import org.esa.snap.core.util.kmz.KmlGroundOverlay;
import org.esa.snap.core.util.kmz.KmzExporter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.timeseries.core.TimeSeriesMapper;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.snap.timeseries.export.util.TimeSeriesExportHelper;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.BoundingBox;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipOutputStream;

import static org.esa.snap.timeseries.export.util.TimeSeriesExportHelper.getOutputFileWithLevelOption;

@ActionID(
        category = "File",
        id = "ExportTimeBasedKmz"
)
@ActionRegistration(
        displayName = "#CTL_ExportTimeBasedKmzName"
)
@ActionReference(path = "Menu/File/Export", position = 500)
@NbBundle.Messages({ "CTL_ExportTimeBasedKmzName=Time Series as Google Earth KMZ" })
public class ExportTimeBasedKmz extends AbstractAction implements ContextAwareAction, LookupListener{

    private static final String HELP_ID = "exportTimeBasedKmz";

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";
    private final SnapFileFilter kmzFileFilter = new SnapFileFilter("KMZ", "kmz", "KMZ - Google Earth File Format");
    private int level = 2;
    private ProductSceneView view;

    public ExportTimeBasedKmz() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportTimeBasedKmz(Lookup lkp) {
        super(Bundle.CTL_ExportTimeBasedKmzName());
        Lookup.Result<ProductSceneViewTopComponent> result = lkp.lookupResult(ProductSceneViewTopComponent.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ExportTimeBasedKmz(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnabled(SnapApp.getDefault().getSelectedProductSceneView() != null);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        view = SnapApp.getDefault().getSelectedProductSceneView();
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
            final File output = fetchOutputFile(view);
            if (output == null) {
                return;
            }
            final String title = "KMZ Export";
            final ProgressMonitorSwingWorker worker = new KmzSwingWorker(title, output);
            worker.executeWithBlocking();
        } else {
            String message = "Product must be in ''Geographic Lat/Lon'' projection.";
            Dialogs.showInformation(message, null);
        }
    }


    protected File fetchOutputFile(ProductSceneView sceneView) {
        TimeSeriesExportHelper.FileWithLevel fileWithLevel = getOutputFileWithLevelOption(sceneView.getRaster(),
                "Export time series as time based KMZ",
                "time_series_",
                IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                kmzFileFilter,
                HELP_ID);
        level = fileWithLevel.level;
        return fileWithLevel.file;
    }


    private KmlFeature createKmlFeature() {
        if (view.isRGB()) {
            return null;
        }
        TimeSeriesMapper timeSeriesMapper = TimeSeriesMapper.getInstance();
        AbstractTimeSeries timeSeries = timeSeriesMapper.getTimeSeries(view.getProduct());
        List<Band> bands = timeSeries.getBandsForVariable(
                AbstractTimeSeries.rasterToVariableName(view.getRaster().getName()));

        if (bands.isEmpty()) {
            return null;
        }
        RasterDataNode refRaster = bands.get(0);
        final KmlFolder folder = new KmlFolder(refRaster.getName(), refRaster.getDescription());
        for (RasterDataNode raster : bands) {
            final GeoCoding geoCoding = raster.getGeoCoding();
            final PixelPos upperLeftPP = new PixelPos(0, 0);
            final PixelPos lowerRightPP = new PixelPos(raster.getRasterWidth(),
                    raster.getRasterHeight());
            final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
            final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
            double north = upperLeftGP.getLat();
            double south = lowerRightGP.getLat();
            double east = lowerRightGP.getLon();
            double west = upperLeftGP.getLon();
            if (geoCoding.isCrossingMeridianAt180()) {
                east += 360;
            }

            final BoundingBox referencedEnvelope = new ReferencedEnvelope(west, east, north, south,
                    DefaultGeographicCRS.WGS84);

            TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(raster);
            if (timeCoding != null) {
                final ProductData.UTC startTime = timeCoding.getStartTime();
                final ProductData.UTC endTime = timeCoding.getEndTime();

                final ImageManager imageManager = ImageManager.getInstance();
                final ImageInfo imageInfo = raster.getImageInfo(ProgressMonitor.NULL);
                final RenderedImage levelImage = imageManager.createColoredBandImage(new RasterDataNode[]{raster}, imageInfo, level);
                final String name = raster.getName();
                final KmlGroundOverlay groundOverlay = new KmlGroundOverlay(name,
                        levelImage,
                        referencedEnvelope,
                        startTime, endTime);
                groundOverlay.setIconName(name + raster.getProduct().getRefNo());
                folder.addChild(groundOverlay);
            }
        }
        return folder;
    }

    private class KmzSwingWorker extends ProgressMonitorSwingWorker {

        private final String title;
        private final File output;
        private static final int ONE_MEGABYTE = 1012 * 1024;

        KmzSwingWorker(String title, File output) {
            super(ExportTimeBasedKmz.this.view, title);
            this.title = title;
            this.output = output;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            KmlFeature kmlFeature = createKmlFeature();
            final FileOutputStream fileOutputStream = new FileOutputStream(output);
            try (ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream, 5 * ONE_MEGABYTE))) {
                final KmzExporter exporter = new KmzExporter();
                exporter.export(kmlFeature, zipStream, pm);
            }
            return null;
        }

        @Override
        protected void done() {
            Throwable exception = null;
            try {
                get();
            } catch (InterruptedException e) {
                exception = e;
            } catch (ExecutionException e) {
                exception = e.getCause();
            }
            if (exception != null) {
                String message = String.format("Error occurred while exporting to KMZ.%n%s",
                        exception.getMessage());
                Dialogs.showError(title, message);
            }

        }
    }
}
