package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public void loadAvailableColorPalettes(File palettesDir) {
        cpdNames.clear();
        cpdList.clear();

        final File[] files = palettesDir.listFiles((dir, name) -> {
            return name.toLowerCase().endsWith(".cpd") || name.toLowerCase().endsWith(".cpt");
        });
        if (files != null) {
            for (File file : files) {
                try {
                    ColorPaletteDef newCpd;

                    if (file.getName().endsWith("cpt")) {
                        newCpd = ColorPaletteDef.loadCpt(file);
                    } else {
                        newCpd = ColorPaletteDef.loadColorPaletteDef(file);
                    }
                    cpdList.add(newCpd);
                    cpdNames.add(file.getName());
                } catch (IOException e) {
                    final Logger logger = SystemUtils.LOG;
                    logger.warning("Unable to load color palette definition from file '" + file.getAbsolutePath() + "'");
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
