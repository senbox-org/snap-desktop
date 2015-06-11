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
package org.esa.snap.rcp.mask;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.Debug;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.geotiff.GeoTIFF;
import org.esa.snap.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.util.io.FileUtils;
import org.esa.snap.util.io.SnapFileChooser;
import org.esa.snap.util.io.SnapFileFilter;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.LookupListener;

import javax.media.jai.operator.BandSelectDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public abstract class AbstractExportImageAction extends AbstractAction implements LookupListener,ContextAwareAction, HelpCtx.Provider {

    public static final String EXPORT_IMAGE_CMD_ID = "exportImageFile";
    public static final String EXPORT_ROI_IMAGE_CMD_ID = "exportROIImageFile";
    public static final String EXPORT_LEGEND_IMAGE_CMD_ID = "exportLegendImageFile";

    protected static final String[] BMP_FORMAT_DESCRIPTION = {"BMP", "bmp", "BMP - Microsoft Windows Bitmap"};
    protected static final String[] PNG_FORMAT_DESCRIPTION = {"PNG", "png", "PNG - Portable Network Graphics"};
    protected static final String[] JPEG_FORMAT_DESCRIPTION = {
            "JPEG", "jpg,jpeg", "JPEG - Joint Photographic Experts Group"
    };

    // not yet used
//    private static final String[] JPEG2K_FORMAT_DESCRIPTION = {
//            "JPEG2000", "jpg,jpeg", "JPEG 2000 - Joint Photographic Experts Group"
//    };

    protected static final String[] TIFF_FORMAT_DESCRIPTION = {"TIFF", "tif,tiff", "TIFF - Tagged Image File Format"};
    protected static final String[] GEOTIFF_FORMAT_DESCRIPTION = {
            "GeoTIFF", "tif,tiff", "GeoTIFF - TIFF with geo-location"
    };


    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
    };

    private final static String[][] SCENE_IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
            GEOTIFF_FORMAT_DESCRIPTION,
    };
    private static final String[] TRANSPARENCY_IMAGE_FORMATS = new String[]{"TIFF", "PNG"};

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";
    private final String HELP_ID = "exportImageFile";


    private SnapFileFilter[] imageFileFilters;
    private SnapFileFilter[] sceneImageFileFilters;

    public AbstractExportImageAction(String name) {
        super(name);
        imageFileFilters = new SnapFileFilter[IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            imageFileFilters[i] = createFileFilter(IMAGE_FORMAT_DESCRIPTIONS[i]);
        }
        sceneImageFileFilters = new SnapFileFilter[SCENE_IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < SCENE_IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            sceneImageFileFilters[i] = createFileFilter(SCENE_IMAGE_FORMAT_DESCRIPTIONS[i]);
        }

    }


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }
    protected void exportImage(final SnapApp snapApp,
                               final SnapFileFilter[] filters) {
        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view == null) {
            return;
        }
//        final String lastDir = SnapApp.getDefault().getPreferences().getPropertyString(IMAGE_EXPORT_DIR_PREFERENCES_KEY,
//                SystemUtils.getUserHomeDir().getPath());

        final String lastDir = IMAGE_EXPORT_DIR_PREFERENCES_KEY; // Todo




        final File currentDir = new File(lastDir);

        final SnapFileChooser fileChooser = new SnapFileChooser();

        HelpCtx.setHelpIDString(fileChooser, getHelpCtx().getHelpID());
        fileChooser.setCurrentDirectory(currentDir);
        for (int i = 0; i < filters.length; i++) {
            SnapFileFilter filter = filters[i];
            Debug.trace("export image: supported format " + (i + 1) + ": " + filter.getFormatName());
            fileChooser.addChoosableFileFilter(filter); // note: also selects current file filter!
        }
        fileChooser.setAcceptAllFileFilterUsed(false);

        final String imageBaseName = FileUtils.getFilenameWithoutExtension(view.getProduct().getName()).replace('.',
                '_');
        configureFileChooser(fileChooser, view, imageBaseName);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        File file = fileChooser.getSelectedFile();
        fileChooser.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // @todo never comes here, why?
                Debug.trace(evt.toString());
            }
        });
        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            Config.instance().load().preferences().put(IMAGE_EXPORT_DIR_PREFERENCES_KEY, currentDirectory.getPath());
//            SnapApp.getDefault().getPreferences().setPropertyString(IMAGE_EXPORT_DIR_PREFERENCES_KEY, currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (file == null || file.getName().equals("")) {
            return;
        }
        final boolean entireImageSelected = isEntireImageSelected();

        final SnapFileFilter fileFilter = fileChooser.getSnapFileFilter();
        String imageFormat = fileFilter != null ? fileFilter.getFormatName() : "TIFF";
        if (imageFormat.equals(GEOTIFF_FORMAT_DESCRIPTION[0]) && !entireImageSelected) {
            SnapDialogs.Answer status = SnapDialogs.requestDecision(Bundle.CTL_ComputeMaskAreaAction_MenuText(),
                    "GeoTIFF is not applicable to image clippings.Shall TIFF format be used instead?",
                    true, null);
            if (status == SnapDialogs.Answer.YES) {
                imageFormat = "TIFF";
            } else {
                return;
            }
        }


//        if (!SnapApp.getDefault().promptForOverwrite(file)) {
//            return;
//        }

        exportImage(imageFormat, view, entireImageSelected, file);
    }

    protected void exportImage( final String imageFormat, final ProductSceneView view,
                                final boolean entireImageSelected, final File file) {
        final SaveImageSwingWorker worker = new SaveImageSwingWorker(SnapApp.getDefault(), "Save Image", imageFormat, view,
                entireImageSelected, file);
        worker.executeWithBlocking();
    }

    protected abstract RenderedImage createImage(String imageFormat, ProductSceneView view);

    protected abstract boolean isEntireImageSelected();

    protected abstract void configureFileChooser(SnapFileChooser fileChooser, ProductSceneView view,
                                                 String imageBaseName);

    protected SnapFileFilter[] getImageFileFilters() {
        return imageFileFilters;
    }

    protected SnapFileFilter[] getSceneImageFileFilters() {
        return sceneImageFileFilters;
    }

//    protected VisatApp getVisatApp() {
//        return VisatApp.getApp();
//    }

    protected static boolean isTransparencySupportedByFormat(String formatName) {
        final String[] formats = TRANSPARENCY_IMAGE_FORMATS;
        for (final String format : formats) {
            if (format.equalsIgnoreCase(formatName)) {
                return true;
            }
        }
        return false;
    }

    protected static SnapFileFilter createFileFilter(String[] description) {
        final String formatName = description[0];
        final String formatExt = description[1];
        final String formatDescr = description[2];
        return new SnapFileFilter(formatName, formatExt, formatDescr);
    }

    private class SaveImageSwingWorker extends ProgressMonitorSwingWorker {

        private final String imageFormat;
        private final ProductSceneView view;
        private final boolean entireImageSelected;
        private final File file;
        private final SnapApp snapApp;

        SaveImageSwingWorker(SnapApp snapApp, String message, String imageFormat, ProductSceneView view,
                             boolean entireImageSelected, File file) {
            super(snapApp.getMainFrame(), message);
            this.snapApp = snapApp;
            this.imageFormat = imageFormat;
            this.view = view;
            this.entireImageSelected = entireImageSelected;
            this.file = file;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            try {
                final String message = "Saving image as " + file.getPath() + "...";
                pm.beginTask(message, 1);
                snapApp.setStatusBarMessage(message);
                snapApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                RenderedImage image = createImage(imageFormat, view);

                boolean geoTIFFWritten = false;
                if (imageFormat.equals("GeoTIFF") && entireImageSelected) {
                    final GeoTIFFMetadata metadata = ProductUtils.createGeoTIFFMetadata(view.getProduct());
                    if (metadata != null) {
                        GeoTIFF.writeImage(image, file, metadata);
                        geoTIFFWritten = true;
                    }
                }
                if (!geoTIFFWritten) {
                    if ("JPEG".equalsIgnoreCase(imageFormat)) {
                        image = BandSelectDescriptor.create(image, new int[]{0, 1, 2}, null);
                    }
                    final OutputStream stream = new FileOutputStream(file);
                    try {
                        ImageEncoder encoder = ImageCodec.createImageEncoder(imageFormat, stream, null);
                        encoder.encode(image);
                    } finally {
                        stream.close();
                    }
                }
            } catch (OutOfMemoryError e) {
                SnapDialogs.showOutOfMemoryError("The image could not be exported.");
            } catch (Throwable e) {
                snapApp.handleError("The image exportation is not possible\n Please check the documentation.", e); //handleUnknownException(e);
            } finally {
                snapApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
                snapApp.setStatusBarMessage(""); //clearStatusBarMessage();
                pm.done();
            }
            return null;
        }
    }
}
