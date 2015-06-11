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

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;
import org.esa.snap.util.io.SnapFileChooser;
import org.esa.snap.util.io.SnapFileFilter;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This actions exports ground control points of the selected product in a ENVI format.
 *
 */

@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.mask.ExportEnviGcpFileAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportEnviGcpFileAction_MenuText",
        popupText = "#CTL_ExportEnviGcpFileAction_ShortDescription",
        lazy = false
)

@ActionReference(
        path = "Menu/File/Export Others",
        position = 10
)


@NbBundle.Messages({
        "CTL_ExportEnviGcpFileAction_MenuText=Geo-Coding as ENVI GCP File",
        "CTL_ExportEnviGcpFileAction_ShortDescription=Exports the currently selected geometry as ESRI Shapefile."
})
public class ExportEnviGcpFileAction extends AbstractAction implements LookupListener,ContextAwareAction,HelpCtx.Provider {

    private static final String GCP_FILE_DESCRIPTION = "ENVI Ground Control Points";
    private static final String GCP_FILE_EXTENSION = ".pts";
    private static final String GCP_LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String GCP_EXPORT_DIR_PREFERENCES_KEY = "user.gcp.export.dir";
    private static final String DIALOG_TITLE = "Export ENVI Ground Control Points";
    private static final String HELP_ID = "exportEnviGcpFile";
    private final Lookup lookup;
    private final Lookup.Result<Band> result;


    public ExportEnviGcpFileAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportEnviGcpFileAction(Lookup lookup) {
        super(Bundle.CTL_ExportEnviGcpFileAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(Band.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }


    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ExportEnviGcpFileAction(lookup);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        exportGroundControlPoints();
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnabled(SnapApp.getDefault().getSelectedProduct() != null);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }
    private void exportGroundControlPoints() {
        SnapApp snapApp = SnapApp.getDefault();
        final Product product = snapApp.getSelectedProduct();
        if (product == null) {
            return;
        }

        JFileChooser fileChooser = createFileChooser(snapApp);
        int result = fileChooser.showSaveDialog(snapApp.getMainFrame());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (file == null || file.getName().equals("")) {
            return;
        }
        final File absoluteFile = FileUtils.ensureExtension(file.getAbsoluteFile(), GCP_FILE_EXTENSION);
        String lastDirPath = absoluteFile.getParent();
        Config.instance().load().preferences().put(GCP_EXPORT_DIR_PREFERENCES_KEY, lastDirPath);

        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            return;
        }
        if (!SnapDialogs.requestOverwriteDecision(DIALOG_TITLE, absoluteFile)) {
            return;
        }
        if (absoluteFile.exists()) {
            absoluteFile.delete();
        }
        try {
            FileWriter writer = new FileWriter(absoluteFile);
            writer.write(createLineString("; ENVI Registration GCP File"));
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            final int resolution = Config.instance().load().preferences().getInt("gcp.resolution", new Integer(112));
            final int gcpWidth = Math.max(width / resolution + 1, 2); //2 minimum
            final int gcpHeight = Math.max(height / resolution + 1, 2);//2 minimum
            final float xMultiplier = 1f * (width - 1) / (gcpWidth - 1);
            final float yMultiplier = 1f * (height - 1) / (gcpHeight - 1);
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();
            for (int y = 0; y < gcpHeight; y++) {
                for (int x = 0; x < gcpWidth; x++) {
                    final float imageX = xMultiplier * x;
                    final float imageY = yMultiplier * y;
                    pixelPos.x = imageX + 0.5f;
                    pixelPos.y = imageY + 0.5f;
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    final double mapX = geoPos.lon; //longitude
                    final double mapY = geoPos.lat; //latitude
                    writer.write(createLineString(mapX, mapY,
                                                  pixelPos.x + 1,
                                                  // + 1 because ENVI uses a one-based pixel co-ordinate system
                                                  pixelPos.y + 1));
                }
            }
        } catch (IOException e) {
            SnapDialogs.showInformation(DIALOG_TITLE, "An I/O error occurred:\n" + e.getMessage());
        }
    }

    private static String createLineString(final String str) {
        return str.concat(GCP_LINE_SEPARATOR);
    }

    private static String createLineString(final double mapX, final double mapY, final double imageX, final double imageY) {
        return "" + mapX + "\t" + mapY + "\t" + imageX + "\t" + imageY + GCP_LINE_SEPARATOR;
    }

    private JFileChooser createFileChooser(final SnapApp snapApp) {
        String lastDirPath = Config.instance().load().preferences().get(GCP_EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        SnapFileChooser fileChooser = new SnapFileChooser();

        HelpCtx.setHelpIDString(fileChooser, getHelpCtx().getHelpID());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(new File(lastDirPath));

        fileChooser.setFileFilter(
                new SnapFileFilter(GCP_FILE_DESCRIPTION, GCP_FILE_EXTENSION, GCP_FILE_DESCRIPTION));
        fileChooser.setDialogTitle(DIALOG_TITLE);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return fileChooser;
    }


}
