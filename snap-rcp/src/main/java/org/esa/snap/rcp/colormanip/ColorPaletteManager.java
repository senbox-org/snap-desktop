package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ColorPaletteManager {
    public final static String FILE_EXTENSION = ".cpd";
    private static final String PREFERENCES_KEY_IO_DIR = "snap.color_palettes.dir";

    private static final ColorPaletteManager manager = new ColorPaletteManager();

    private final List<ColorPaletteDef> cpdList;
    private final List<String> cpdNames;
    private Path ioDir;

    private ColorPaletteManager() {
        cpdList = new ArrayList<>();
        cpdNames = new ArrayList<>();
    }

    public static ColorPaletteManager getDefault() {
        return manager;
    }

    private void reloadAvailableColorPalettes() {
        File palettesDir = getIODir().toFile();
        this.cpdNames.clear();
        this.cpdList.clear();

        final File[] files = palettesDir.listFiles((dir, name) -> {
            return name.toLowerCase().endsWith(ColorPaletteManager.FILE_EXTENSION);
        });
        if (files != null) {
            for (File file : files) {
                loadColorPaletteFromFile(file);
            }
        }
    }

    private void loadColorPaletteFromFile(File file) {
        try {
            ColorPaletteDef newCpd = ColorPaletteDef.loadColorPaletteDef(file);
            this.cpdList.add(newCpd);
            this.cpdNames.add(file.getName());
        } catch (IOException e) {
            final Logger logger = SystemUtils.LOG;
            logger.warning("Unable to load color palette definition from file '" + file.getAbsolutePath() + "'");
            logger.log(Level.INFO, e.getMessage(), e);
        }
    }

    public List<ColorPaletteDef> getColorPaletteDefList() {
        return Collections.unmodifiableList(cpdList);
    }

    public String getFileNameWithoutExtension(ColorPaletteDef colorPaletteDef) {
        for (int i = 0; i < cpdList.size(); i++) {
            ColorPaletteDef existingColorPaletteDef = cpdList.get(i);
            if (existingColorPaletteDef == colorPaletteDef) {
                String nameFor = cpdNames.get(i);
                if (nameFor.toLowerCase().endsWith(ColorPaletteManager.FILE_EXTENSION)) {
                    return nameFor.substring(0, nameFor.length() - ColorPaletteManager.FILE_EXTENSION.length());
                }
                return nameFor;
            }
        }
        return null;
    }

    public void copyColorPaletteFileFromResources(ClassLoader classLoader, String resourcesFolderPath, String fileName) {
        boolean canLoadAllFiles = (this.ioDir == null);
        File destinationFile = null;
        if (fileName.toLowerCase().endsWith(ColorPaletteManager.FILE_EXTENSION)) {
            URL url = classLoader.getResource(resourcesFolderPath + fileName);
            if (url != null) {
                try {
                    InputStream inputStream = url.openConnection().getInputStream();
                    try {
                        destinationFile = new File(getIODir().toFile(), fileName);
                        FileOutputStream outStream = new FileOutputStream(destinationFile);
                        try {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0){
                                outStream.write(buffer, 0, length);
                            }
                        } finally {
                            outStream.close();
                        }
                    } finally {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    SystemUtils.LOG.log(Level.SEVERE, "Unable to copy the color palette definition file '" + fileName +"' from the resources.", e);
                }
            }
        }
        if (canLoadAllFiles) {
            reloadAvailableColorPalettes();
        } else if (destinationFile != null) {
            loadColorPaletteFromFile(destinationFile);
        }
    }

    public ColorPaletteDef findColorPaletteByFileName(String fileName) {
        if (fileName.toLowerCase().endsWith(ColorPaletteManager.FILE_EXTENSION)) {
            for (int i = 0; i < cpdNames.size(); i++) {
                String nameFor = cpdNames.get(i);
                if (nameFor.equalsIgnoreCase(fileName)) {
                    return cpdList.get(i);
                }
            }
        }
        return null;
    }

    public void setIODir(File dir) {
        this.ioDir = dir.toPath();
        Config.instance().preferences().put(PREFERENCES_KEY_IO_DIR, this.ioDir.toString());

        reloadAvailableColorPalettes();
    }

    public Path getIODir() {
        if (this.ioDir == null) {
            this.ioDir = Paths.get(Config.instance().preferences().get(PREFERENCES_KEY_IO_DIR, getColorPalettesDir().toString()));
        }
        return this.ioDir;
    }

    private Path getColorPalettesDir() {
        return SystemUtils.getAuxDataPath().resolve("color_palettes");
    }
}
