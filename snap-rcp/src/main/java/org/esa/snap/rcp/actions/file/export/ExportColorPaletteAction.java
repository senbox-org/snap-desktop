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

import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.NamingConvention;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.product.ProductSceneView;
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

/**
 * This action exports the color palette of the selected product.
 *
 * @author Marco Peters
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportColorPaletteAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportColorPaletteAction_MenuText",
        popupText = "#CTL_ExportColorPaletteAction_PopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other", position = 20),
        @ActionReference(path = "Context/ProductSceneView" ,position = 80)
})
@NbBundle.Messages({
        "CTL_ExportColorPaletteAction_MenuText=" + NamingConvention.COLOR_MIXED_CASE + " Palette as File",
        "CTL_ExportColorPaletteAction_PopupText=Export " + NamingConvention.COLOR_MIXED_CASE + " Palette as File",
        "CTL_ExportColorPaletteAction_DialogTitle=Export " + NamingConvention.COLOR_MIXED_CASE + " Palette",
        "CTL_ExportColorPaletteAction_ShortDescription=Export " + NamingConvention.COLOR_MIXED_CASE + " Palette as File."
})
public class ExportColorPaletteAction extends AbstractAction implements LookupListener, ContextAwareAction, HelpCtx.Provider {

    private static final String KEY_LAST_OPEN = "ExportColorPaletteVPI.path";
    private static final String HELP_ID = "exportColorPalette";
    private final Lookup.Result<ProductSceneView> result;


    public ExportColorPaletteAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportColorPaletteAction(Lookup lookup) {
        super(Bundle.CTL_ExportColorPaletteAction_MenuText());
        putValue("popupText", Bundle.CTL_ExportColorPaletteAction_PopupText());
        result = lookup.lookupResult(ProductSceneView.class);
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
        fileChooser.setDialogTitle(Bundle.CTL_ExportColorPaletteAction_DialogTitle());
        if (fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame()) == JFileChooser.APPROVE_OPTION
                && fileChooser.getSelectedFile() != null) {
            getPreferences().setPropertyString(KEY_LAST_OPEN, fileChooser.getCurrentDirectory().getAbsolutePath());
            File file = fileChooser.getSelectedFile();
            if (fileChooser.getFileFilter() instanceof SnapFileFilter) {
                SnapFileFilter fileFilter = (SnapFileFilter) fileChooser.getFileFilter();
                file = FileUtils.ensureExtension(file, fileFilter.getDefaultExtension());
            }
            try {
                writeColorPalette(raster, file);
            } catch (IOException ie) {
                Dialogs.showError(Bundle.CTL_ExportColorPaletteAction_DialogTitle(),
                                      "Failed to export " + NamingConvention.COLOR_LOWER_CASE + " palette:\n" + ie.getMessage());
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
        try (FileWriter writer = new FileWriter(file)) {
            writeColorPalette(raster, writer);
        }
    }

    private static void writeColorPalette(RasterDataNode raster, FileWriter writer) throws IOException {
        ImageInfo imageInfo = raster.getImageInfo();
        final ColorPaletteDef paletteDef = imageInfo.getColorPaletteDef();
        final Color[] colorPalette = ImageManager.createColorPalette(imageInfo);
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

    private RasterDataNode getSelectedRaster() {
        Optional<? extends ProductSceneView> first = result.allInstances().stream().findFirst();
        if (first.isPresent()) {
            return first.get().getRaster();
        }
        return null;
    }

    private ImageInfo getSelectedImageInfo() {
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
