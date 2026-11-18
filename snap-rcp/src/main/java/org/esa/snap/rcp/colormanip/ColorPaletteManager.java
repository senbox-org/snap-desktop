package org.esa.snap.rcp.colormanip;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class ColorPaletteManager {

    static ColorPaletteManager manager = new ColorPaletteManager();

    private List<ColorPaletteDef> cpdList;
    private List<String> cpdNames;

    public static ColorPaletteManager getDefault() {
        return manager;
    }

    ColorPaletteManager() {
        cpdList = new ArrayList<>();
        cpdNames = new ArrayList<>();
    }

    public void loadAvailableColorPalettes(Path palettesDir) {
        cpdNames.clear();
        cpdList.clear();

        final Collection<Path> files = IOHelper.listFiles(palettesDir, dir -> dir.getFileName().toString().toLowerCase().endsWith(".cpd") || dir.getFileName().toString().toLowerCase().endsWith(".cpt"));
        if (files != null) {
            for (Path file : files) {
                try {
                    ColorPaletteDef newCpd;

                    if (file.getFileName().toString().endsWith("cpt")) {
                        newCpd = ColorPaletteDef.loadCpt(file);
                    } else {
                        newCpd = ColorPaletteDef.loadColorPaletteDef(file);
                    }
                    cpdList.add(newCpd);
                    cpdNames.add(file.getFileName().toString());
                } catch (IOException e) {
                    final Logger logger = SystemUtils.LOG;
                    logger.warning("Unable to load color palette definition from file '" + file.toAbsolutePath() + "'");
                    logger.log(Level.INFO, e.getMessage(), e);
                }
            }
        }
    }

    public List<ColorPaletteDef> getColorPaletteDefList() {
        return Collections.unmodifiableList(cpdList);
    }

    public String getNameFor(ColorPaletteDef cpdForRaster) {
        for (int i = 0; i < cpdList.size(); i++) {
            ColorPaletteDef colorPaletteDef = cpdList.get(i);
            if (colorPaletteDef == cpdForRaster)
                return cpdNames.get(i);
        }
        return null;
    }
}
