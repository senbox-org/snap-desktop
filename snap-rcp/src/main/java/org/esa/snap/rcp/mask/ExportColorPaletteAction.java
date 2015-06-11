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
import org.esa.snap.framework.datamodel.ColorPaletteDef;
import org.esa.snap.framework.datamodel.ImageInfo;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.PropertyMap;
import org.esa.snap.util.StringUtils;
import org.esa.snap.util.io.FileUtils;
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This action exports the color palette of the selected product.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */

//
//<action>
//        <parent>exportOther</parent>
//        <id>exportColorPalette</id>
//        <class>org.esa.snap.visat.actions.ExportColorPaletteAction</class>
//<text>Colour Palette as File</text>
//<mnemonic>a</mnemonic>
//<shortDescr>Export the colour palette of the current view to a plain text.</shortDescr>
//<context>image</context>
//<popuptext>Export Colour Palette as File...</popuptext>
//<helpId>exportColorPalette</helpId>
//</


@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.mask.ExportColorPaletteAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportColorPaletteAction_MenuText",
        popupText = "#CTL_ExportColorPaletteAction_ShortDescription",
        lazy = false
)


@ActionReference(
        path = "Menu/File/Export Others",
        position = 8
)

@NbBundle.Messages({
        "CTL_ExportColorPaletteAction_MenuText=Colour Palette as File",
        "CTL_ExportColorPaletteAction_ShortDescription=Export Colour Palette as File..."
})

public class ExportColorPaletteAction extends AbstractAction implements LookupListener, ContextAwareAction, HelpCtx.Provider {

    private static final String KEY_LAST_OPEN = "ExportColorPaletteVPI.path";
    private static final String VPI_TEXT = "Export Color Palette";
    private final String HELP_ID = "exportColorPalette";
    private final Lookup lookup;
    private final Lookup.Result<Band> result;


    public ExportColorPaletteAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportColorPaletteAction(Lookup lookup) {
        super(Bundle.CTL_ExportColorPaletteAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(Band.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        SnapFileFilter fileFilter1 = new SnapFileFilter("CSV", ".csv", "CSV files"); // I18N
        SnapFileFilter fileFilter2 = new SnapFileFilter("TXT", ".txt", "Text files"); // I18N
        JFileChooser fileChooser = new JFileChooser();
        File lastDir = new File(getPreferences().getPropertyString(KEY_LAST_OPEN, "."));
        fileChooser.setCurrentDirectory(lastDir);
        RasterDataNode raster = getSelectedRaster();
        fileChooser.setSelectedFile(new File(lastDir, raster.getName() + "-palette.csv"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(fileFilter1);
        fileChooser.addChoosableFileFilter(fileFilter2);
        fileChooser.setFileFilter(fileFilter1);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle(VPI_TEXT);
        if (fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame()) == JFileChooser.APPROVE_OPTION
                && fileChooser.getSelectedFile() != null) {
            getPreferences().setPropertyString(KEY_LAST_OPEN,
                    fileChooser.getCurrentDirectory().getAbsolutePath());
            File file = fileChooser.getSelectedFile();
            if (fileChooser.getFileFilter() instanceof SnapFileFilter) {
                SnapFileFilter fileFilter = (SnapFileFilter) fileChooser.getFileFilter();
                file = FileUtils.ensureExtension(file, fileFilter.getDefaultExtension());
            }
            try {
                writeColorPalette(raster, file);
            } catch (IOException ie) {
                SnapDialogs.showError(VPI_TEXT, "Failed to export colour palette:\n" + ie.getMessage());
            }
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ExportColorPaletteAction(lookup);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnabled(getSelectedImageInfo() != null);
    }

    private static void writeColorPalette(RasterDataNode raster, File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        try {
            writeColorPalette(raster, writer);
        } finally {
            writer.close();
        }
    }

    private static void writeColorPalette(RasterDataNode raster, FileWriter writer) throws IOException {
        ImageInfo imageInfo = raster.getImageInfo();
        final ColorPaletteDef paletteDef = imageInfo.getColorPaletteDef();
        final Color[] colorPalette = ImageManager.createColorPalette(imageInfo);
//        Color[] colorPalette = paletteDef.createColorPalette(raster);
        double s1 = paletteDef.getMinDisplaySample();
        double s2 = paletteDef.getMaxDisplaySample();
        int numColors = colorPalette.length;
        writer.write("# Band: " + raster.getName() + "\n");
        writer.write("# Sample unit: " + raster.getUnit() + "\n");
        writer.write("# Minimum sample value: " + s1 + "\n");
        writer.write("# Maximum sample value: " + s2 + "\n");
        writer.write("# Number of colors: " + numColors + "\n");
        double sf = (s2 - s1) / (numColors - 1.0);
        writer.write("ID;Sample;RGB\n");
        for (int i = 0; i < numColors; i++) {
            Color color = colorPalette[i];
            double s = s1 + i * sf;
            writer.write(i + ";" + s + ";" + StringUtils.formatColor(color) + "\n");
        }
    }

    private static RasterDataNode getSelectedRaster() {
        ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
        if (sceneView != null) {
            return sceneView.getRaster();
        }
        return null;
    }

    private static ImageInfo getSelectedImageInfo() {
        RasterDataNode raster = getSelectedRaster();
        if (raster != null) {
            return raster.getImageInfo();
        }
        return null;
    }

    private static PropertyMap getPreferences() {
        return SnapApp.getDefault().getPreferencesPropertyMap();
    }


}
