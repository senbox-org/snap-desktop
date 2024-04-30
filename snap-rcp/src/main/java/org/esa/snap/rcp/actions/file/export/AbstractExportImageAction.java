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
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.LookupListener;

import javax.media.jai.operator.BandSelectDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public abstract class AbstractExportImageAction extends AbstractAction implements LookupListener, ContextAwareAction, HelpCtx.Provider {
    public static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";

    protected static final String[] BMP_FORMAT_DESCRIPTION = {"BMP", "bmp", "BMP - Microsoft Windows Bitmap"};
    protected static final String[] PNG_FORMAT_DESCRIPTION = {"PNG", "png", "PNG - Portable Network Graphics"};
    protected static final String[] JPEG_FORMAT_DESCRIPTION = {"JPEG", "jpg,jpeg", "JPEG - Joint Photographic Experts Group"};
    protected static final String[] GEOTIFF_FORMAT_DESCRIPTION = {"GeoTIFF", "tif,tiff", "GeoTIFF - TIFF with geo-location"};

    // not yet used
//    private static final String[] JPEG2K_FORMAT_DESCRIPTION = {"JPEG2000", "jpg,jpeg", "JPEG 2000 - Joint Photographic Experts Group"};
    protected static final String[] TIFF_FORMAT_DESCRIPTION = {"TIFF", "tif,tiff", "TIFF - Tagged Image File Format"};
    private static final String[] TRANSPARENCY_IMAGE_FORMATS = new String[]{"TIFF", "PNG"};
    private String dialogTitle;
    private final String helpId;

    public AbstractExportImageAction(String name, String helpId) {
        super(name);
        this.dialogTitle = name;
        this.helpId = helpId;
        Config.instance().load();
    }

    protected static boolean isTransparencySupportedByFormat(String formatName) {
        for (final String format : TRANSPARENCY_IMAGE_FORMATS) {
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

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(helpId);
    }

    protected void exportImage(final SnapFileFilter[] filters) {
        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view == null) {
            return;
        }
        final String lastDir = Config.instance().preferences().get(IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());

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

        String name = view.getProduct().getName();
        final String imageBaseName = FileUtils.getFilenameWithoutExtension(name).replace('.', '_');
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
        fileChooser.addPropertyChangeListener(evt -> {
            // @todo never comes here, why?
            Debug.trace(evt.toString());
        });
        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            Config.instance().preferences().put(IMAGE_EXPORT_DIR_PREFERENCES_KEY, currentDirectory.getPath());
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
            final String msg = "GeoTIFF is not applicable to image clippings. Please select 'Full scene' option." +
                    "\nShall TIFF format be used instead?";
            Dialogs.Answer status = Dialogs.requestDecision(dialogTitle, msg, true, null);
            if (status == Dialogs.Answer.YES) {
                imageFormat = "TIFF";
            } else {
                return;
            }
        }


        if (Boolean.TRUE.equals(Dialogs.requestOverwriteDecision(dialogTitle, file))) {
            exportImage(imageFormat, view, entireImageSelected, file);
        }
    }

    protected void exportImage(final String imageFormat, final ProductSceneView view, final boolean entireImageSelected, final File file) {
        final SaveImageSwingWorker worker = new SaveImageSwingWorker(SnapApp.getDefault(), "Save Image", imageFormat, view,
                entireImageSelected, file);
        worker.executeWithBlocking();
    }

    protected abstract RenderedImage createImage(String imageFormat, ProductSceneView view);

    protected abstract boolean isEntireImageSelected();

    protected abstract void configureFileChooser(SnapFileChooser fileChooser, ProductSceneView view,
                                                 String imageBaseName);

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
                    try (OutputStream stream = new FileOutputStream(file)) {
                        ImageEncoder encoder = ImageCodec.createImageEncoder(imageFormat, stream, null);
                        encoder.encode(image);
                    }
                }
            } catch (OutOfMemoryError e) {
                Dialogs.showOutOfMemoryError("The image could not be exported.");
            } catch (Throwable e) {
                snapApp.handleError("The image could not be exported.", e); //handleUnknownException(e);
            } finally {
                snapApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
                snapApp.setStatusBarMessage("");
                pm.done();
            }
            return null;
        }
    }
}
