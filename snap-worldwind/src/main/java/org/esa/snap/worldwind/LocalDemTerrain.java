/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.worldwind;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.LocalElevationModel;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.model.DemGrid;
import org.esa.snap.worldwind.preferences.WorldWindPreferences;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

/**
 * Local DEM files (e.g. the Copernicus tiles SNAP downloads) as terrain of the globes, replacing WorldWind's own
 * elevations where they have data. The files are read in the background; the globes get the terrain when it is ready.
 * All methods except {@link #load} are called on the EDT.
 */
public final class LocalDemTerrain {

    static final int MAX_FILES = 64;
    /**
     * Samples per row and column of one file; 1024 keeps a 1° Copernicus 30 m tile at about 100 m and 4 MB.
     */
    static final int MAX_SAMPLES = 1024;
    static final float MISSING_DATA = -32768f;
    static final String GEOTIFF_FORMAT = "GeoTIFF";

    // the globes with their windows, to redraw them when the terrain arrives; both weakly held
    private static final Map<Globe, WeakReference<WorldWindow>> globes = new WeakHashMap<>();
    private static LocalElevationModel terrain;
    private static Thread loader;
    // the folder last loaded, also when it gave no terrain, so that new views do not read it again
    private static File loadedFolder;
    private static int generation;

    private LocalDemTerrain() {
    }

    /**
     * Gives a globe the local terrain, now or once it is loaded, as long as the preference is set.
     */
    static void attach(final Globe globe, final WorldWindow wwd) {
        globes.put(globe, new WeakReference<>(wwd));
        if (terrain != null) {
            addTo(globe, terrain);
        } else if (loader == null && !WorldWindPreferences.getLocalDemDir().equals(loadedFolder)) {
            reload();
        }
    }

    /**
     * Reloads the terrain after the preferences changed.
     */
    public static void reload() {
        if (terrain != null) {
            for (Globe globe : globes.keySet()) {
                removeFrom(globe, terrain);
            }
            terrain = null;
            redrawAll();
        }
        final int thisGeneration = ++generation;
        if (loader != null) {
            // a load of the previous settings is still running: stop it, its result is discarded anyway
            loader.interrupt();
            loader = null;
        }
        loadedFolder = null;
        if (!WorldWindPreferences.isLocalDemEnabled() || globes.isEmpty()) {
            return;
        }
        final File folder = WorldWindPreferences.getLocalDemDir();
        loader = new Thread(() -> {
            final LocalElevationModel model = load(folder);
            SwingUtilities.invokeLater(() -> {
                if (thisGeneration != generation) {
                    return;
                }
                loader = null;
                loadedFolder = folder;
                terrain = model;
                if (model != null) {
                    for (Globe globe : globes.keySet()) {
                        addTo(globe, model);
                    }
                    redrawAll();
                }
            });
        }, "WorldWind local DEM");
        loader.setDaemon(true);
        loader.start();
    }

    private static void redrawAll() {
        for (WeakReference<WorldWindow> reference : globes.values()) {
            final WorldWindow wwd = reference.get();
            if (wwd != null) {
                wwd.redraw();
            }
        }
    }

    /**
     * Reads the DEM GeoTIFFs of a folder. Files that cannot be read or are not on a lat/lon grid are skipped. Returns
     * null if no file could be used.
     */
    static LocalElevationModel load(final File folder) {
        final List<File> files = DemGrid.findDemFiles(folder, Integer.MAX_VALUE);
        final LocalElevationModel model = new LocalElevationModel();
        model.setName("Local DEM");
        int used = 0;
        for (File file : files) {
            if (Thread.currentThread().isInterrupted()) {
                return null; // superseded by a reload
            }
            if (used == MAX_FILES) {
                SystemUtils.LOG.warning("WorldWind: only " + MAX_FILES + " DEM files of " + folder + " are used as terrain");
                break;
            }
            Product product = null;
            try {
                product = readDem(file);
                if (product == null) {
                    SystemUtils.LOG.info("WorldWind: no reader for " + file.getName() + ", not used as terrain");
                    continue;
                }
                final DemGrid grid = DemGrid.fromProduct(product, MAX_SAMPLES);
                if (grid == null) {
                    SystemUtils.LOG.info("WorldWind: " + file.getName() + " is not a DEM on a lat/lon grid, not used as terrain");
                    continue;
                }
                addGrid(model, grid);
                used++;
            } catch (Exception e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to use " + file + " as terrain", e);
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }
        }
        SystemUtils.LOG.info("WorldWind: " + used + " DEM files of " + folder + " used as terrain");
        return used > 0 ? model : null;
    }

    /**
     * Reads a DEM file. SNAP's GeoTIFF reader declines Copernicus DEM tiles (cloud-optimised GeoTIFFs) in its
     * qualification although it reads them, so, like SNAP's DEM module, they are read with it directly if no reader
     * qualifies.
     */
    private static Product readDem(final File file) throws IOException {
        final Product product = ProductIO.readProduct(file);
        if (product != null) {
            return product;
        }
        final Iterator<ProductReaderPlugIn> plugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(GEOTIFF_FORMAT);
        if (!plugIns.hasNext()) {
            return null;
        }
        final ProductReader reader = plugIns.next().createReaderInstance();
        try {
            return reader.readProductNodes(file, null);
        } catch (IOException | RuntimeException e) {
            // the reader keeps the file open until closed
            reader.close();
            throw e;
        }
    }

    static void addGrid(final LocalElevationModel model, final DemGrid grid) {
        final ByteBuffer buffer = ByteBuffer.allocate(grid.heights.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float height : grid.heights) {
            buffer.putFloat(Float.isNaN(height) ? MISSING_DATA : height);
        }
        buffer.rewind();
        final AVList params = new AVListImpl();
        params.setValue(AVKey.DATA_TYPE, AVKey.FLOAT32);
        params.setValue(AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN);
        params.setValue(AVKey.MISSING_DATA_SIGNAL, (double) MISSING_DATA);
        model.addElevations(buffer, Sector.fromDegrees(grid.minLat, grid.maxLat, grid.minLon, grid.maxLon),
                            grid.width, grid.height, params);
    }

    static void addTo(final Globe globe, final ElevationModel model) {
        final ElevationModel current = globe.getElevationModel();
        if (current instanceof LocalFirstElevationModel) {
            if (!((LocalFirstElevationModel) current).containsElevationModel(model)) {
                ((LocalFirstElevationModel) current).addElevationModel(model);
            }
            return;
        }
        final LocalFirstElevationModel compound = new LocalFirstElevationModel(current);
        compound.addElevationModel(model);
        globe.setElevationModel(compound);
    }

    static void removeFrom(final Globe globe, final ElevationModel model) {
        final ElevationModel current = globe.getElevationModel();
        if (current instanceof LocalFirstElevationModel) {
            final LocalFirstElevationModel compound = (LocalFirstElevationModel) current;
            compound.removeElevationModel(model);
            if (compound.getElevationModels().size() == 1 && compound.original != null) {
                globe.setElevationModel(compound.original);
            }
        }
    }

    /**
     * The globe's own elevations plus local DEMs, the local DEMs taking precedence wherever they have data.
     * WorldWind's compound model sorts its models by resolution and lets the finest win, so a local DEM sampled
     * coarser than WorldWind's 30 m elevations would otherwise never be used.
     */
    static final class LocalFirstElevationModel extends CompoundElevationModel {

        final ElevationModel original;

        LocalFirstElevationModel(final ElevationModel original) {
            this.original = original;
            if (original != null) {
                addElevationModel(original);
            }
        }

        @Override
        protected void sortElevationModels() {
            super.sortElevationModels();
            // the models are looked up from last to first: local DEMs last
            final List<ElevationModel> local = new ArrayList<>();
            for (ElevationModel model : elevationModels) {
                if (model instanceof LocalElevationModel) {
                    local.add(model);
                }
            }
            elevationModels.removeAll(local);
            elevationModels.addAll(local);
        }
    }
}
