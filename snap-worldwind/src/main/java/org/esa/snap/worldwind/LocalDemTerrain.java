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

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.LocalElevationModel;
import gov.nasa.worldwind.view.orbit.OrbitView;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.worldwind.model.DemGrid;
import org.esa.snap.worldwind.preferences.WorldWindPreferences;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Local DEM files (e.g. the Copernicus tiles SNAP downloads) as terrain of the globes, replacing WorldWind's own
 * elevations where they have data. The files are read in the background; the globes get the terrain when it is ready.
 * All methods except {@link #load} are called on the EDT.
 */
public final class LocalDemTerrain {

    /**
     * Memory for the heights of all files: with many files, each is sampled more coarsely.
     */
    static final long MAX_BYTES = 128L * 1024 * 1024;
    /**
     * Samples per row and column of one file at most; 1024 keeps a 1° Copernicus 30 m tile at about 100 m and 4 MB.
     */
    static final int MAX_SAMPLES = 1024;
    /**
     * Samples per row and column of one file at least (about 400 m for a 1° tile).
     */
    static final int MIN_SAMPLES = 256;
    /**
     * Files at most, each at {@link #MIN_SAMPLES} within {@link #MAX_BYTES}; beyond, those nearest the view are used.
     */
    static final int MAX_FILES = (int) (MAX_BYTES / (4L * MIN_SAMPLES * MIN_SAMPLES));
    static final float MISSING_DATA = -32768f;
    static final String GEOTIFF_FORMAT = "GeoTIFF";

    /**
     * Tells the user that the chosen folder gives no terrain; replaced in tests.
     */
    static Consumer<String> noTerrainMessage =
            message -> Dialogs.showWarning("Local DEM Terrain", message, "worldwind.localDem.noTerrain");

    // one load at a time: a reload cancels the running one, which stops before its next file
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "WorldWind local DEM");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    // the globes with their windows, to redraw them when the terrain arrives; both weakly held
    private static final Map<Globe, WeakReference<WorldWindow>> globes = new WeakHashMap<>();
    private static LocalElevationModel terrain;
    private static Future<?> loader;
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
            loader.cancel(true);
            loader = null;
        }
        loadedFolder = null;
        if (!WorldWindPreferences.isLocalDemEnabled() || globes.isEmpty()) {
            return;
        }
        final File folder = WorldWindPreferences.getLocalDemDir();
        final LatLon center = viewCenter();
        loader = LOADER.submit(() -> {
            LocalElevationModel model = null;
            boolean failed = false;
            try {
                model = load(folder, center);
            } catch (Throwable e) {
                // also errors such as OutOfMemoryError: the next reload must be able to start
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to load the DEM files of " + folder + " as terrain", e);
                failed = true;
            }
            final LocalElevationModel result = model;
            final boolean interrupted = Thread.currentThread().isInterrupted();
            final boolean loadFailed = failed;
            SwingUtilities.invokeLater(() -> {
                if (thisGeneration != generation) {
                    return;
                }
                loader = null;
                loadedFolder = folder;
                terrain = result;
                if (result != null) {
                    for (Globe globe : globes.keySet()) {
                        addTo(globe, result);
                    }
                    redrawAll();
                } else if (!interrupted) {
                    noTerrainMessage.accept(loadFailed ?
                            "The DEM files in " + folder + " could not be loaded as terrain (see the log)." :
                            "No DEM GeoTIFFs on a lat/lon grid were found in " + folder + ".\n" +
                            "The globes show WorldWind's own elevations. Choose another folder in the WorldWind options.");
                }
            });
        });
    }

    /**
     * The centre of the view of a globe, to choose the DEM files when there are too many; null if unknown.
     */
    private static LatLon viewCenter() {
        for (WeakReference<WorldWindow> reference : globes.values()) {
            final WorldWindow wwd = reference.get();
            if (wwd == null || wwd.getView() == null) {
                continue;
            }
            final View view = wwd.getView();
            final Position position = view instanceof OrbitView ? ((OrbitView) view).getCenterPosition() : view.getEyePosition();
            if (position != null) {
                return position;
            }
        }
        return null;
    }

    private static void redrawAll() {
        for (WeakReference<WorldWindow> reference : globes.values()) {
            final WorldWindow wwd = reference.get();
            if (wwd != null) {
                wwd.redraw();
            }
        }
    }

    static LocalElevationModel load(final File folder) {
        return load(folder, null);
    }

    /**
     * Reads the DEM GeoTIFFs of a folder. Files that cannot be read, are not on a lat/lon grid or cross the
     * antimeridian are skipped. Of more than {@link #MAX_FILES} usable files, those nearest {@code center} are used.
     * Returns null if no file could be used, or if the thread was interrupted.
     */
    static LocalElevationModel load(final File folder, final LatLon center) {
        // the usable files and where they lie, from their headers
        final List<File> usable = new ArrayList<>();
        final List<double[]> bounds = new ArrayList<>();
        for (File file : DemGrid.findDemFiles(folder, Integer.MAX_VALUE)) {
            if (Thread.currentThread().isInterrupted()) {
                return null; // superseded by a reload
            }
            Product product = null;
            try {
                product = readDem(file);
                if (product == null) {
                    SystemUtils.LOG.info("WorldWind: no reader for " + file.getName() + ", not used as terrain");
                    continue;
                }
                final double[] fileBounds = DemGrid.bounds(product);
                if (fileBounds == null) {
                    SystemUtils.LOG.info("WorldWind: " + file.getName() + " is not a DEM on a lat/lon grid within " +
                                         "-180..180 degrees, not used as terrain");
                    continue;
                }
                usable.add(file);
                bounds.add(fileBounds);
            } catch (Exception e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to use " + file + " as terrain", e);
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }
        }
        final List<File> files = nearest(usable, bounds, center, MAX_FILES);
        if (files.size() < usable.size()) {
            SystemUtils.LOG.warning("WorldWind: " + files.size() + " of the " + usable.size() + " DEM files of " + folder +
                                    " are used as terrain, those nearest the view" +
                                    (center != null ? " at " + center : ""));
        }
        final int samples = samplesPerFile(files.size());
        final LocalElevationModel model = new LocalElevationModel();
        model.setName("Local DEM");
        int used = 0;
        for (File file : files) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            Product product = null;
            try {
                product = readDem(file);
                final DemGrid grid = product != null ? DemGrid.fromProduct(product, samples) : null;
                if (grid == null) {
                    SystemUtils.LOG.info("WorldWind: " + file.getName() + " holds no heights, not used as terrain");
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
        SystemUtils.LOG.log(used > 0 ? Level.INFO : Level.WARNING,
                            "WorldWind: " + used + " DEM files of " + folder + " used as terrain");
        return used > 0 ? model : null;
    }

    /**
     * Samples per row and column of each of {@code fileCount} files, within {@link #MAX_BYTES}.
     */
    static int samplesPerFile(final int fileCount) {
        final int samples = (int) Math.sqrt(MAX_BYTES / (double) Float.BYTES / Math.max(1, fileCount));
        return Math.max(MIN_SAMPLES, Math.min(MAX_SAMPLES, samples));
    }

    /**
     * The {@code max} files whose bounds are nearest a position, or the first {@code max} without a position.
     */
    static List<File> nearest(final List<File> files, final List<double[]> bounds, final LatLon center, final int max) {
        if (files.size() <= max) {
            return files;
        }
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            order.add(i);
        }
        if (center != null) {
            order.sort(Comparator.comparingDouble(i -> distance(bounds.get(i), center)));
        }
        final List<File> nearest = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            nearest.add(files.get(order.get(i)));
        }
        return nearest;
    }

    /**
     * Squared distance in degrees, shrinking longitude towards the poles, from a position to the nearest point of
     * bounds {minLat, maxLat, minLon, maxLon}.
     */
    private static double distance(final double[] bounds, final LatLon center) {
        final double lat = center.getLatitude().degrees;
        final double lon = center.getLongitude().degrees;
        final double dLat = lat < bounds[0] ? bounds[0] - lat : lat > bounds[1] ? lat - bounds[1] : 0.0;
        double dLon = 0.0;
        if (lon < bounds[2] || lon > bounds[3]) {
            final double toWest = ((bounds[2] - lon) % 360.0 + 360.0) % 360.0;
            final double fromEast = ((lon - bounds[3]) % 360.0 + 360.0) % 360.0;
            dLon = Math.min(toWest, fromEast) * Math.cos(Math.toRadians(lat));
        }
        return dLat * dLat + dLon * dLon;
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
