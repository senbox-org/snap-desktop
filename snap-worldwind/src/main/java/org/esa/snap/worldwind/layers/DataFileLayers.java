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
package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.formats.shapefile.ShapefileLayerFactory;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.nasa.worldwind.ogc.kml.impl.KMLController;
import gov.nasa.worldwindx.examples.GeoJSONLoader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Layers from vector data files: KML/KMZ, ESRI shapefiles and GeoJSON. Loading reads the whole file, so it is done
 * off the EDT; the returned layer is then added to the globe on the EDT.
 */
public final class DataFileLayers {

    public enum Type {
        KML("KML/KMZ", "kml", "kmz"),
        SHAPEFILE("Shapefile", "shp"),
        GEOJSON("GeoJSON", "geojson", "json");

        public final String description;
        public final String[] extensions;

        Type(String description, String... extensions) {
            this.description = description;
            this.extensions = extensions;
        }
    }

    private static final long SHAPEFILE_TIMEOUT_MINUTES = 10;

    private DataFileLayers() {
    }

    /**
     * The type of a data file from its extension, or null if it is not supported.
     */
    public static Type typeOf(final File file) {
        final String name = file.getName().toLowerCase(Locale.ENGLISH);
        for (Type type : Type.values()) {
            for (String extension : type.extensions) {
                if (name.endsWith("." + extension)) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * Reads a data file into a layer named after the file. Blocks until the file is read; not to be called on the
     * EDT.
     *
     * @throws IOException if the file type is not supported or the file cannot be read
     */
    public static Layer load(final File file) throws IOException {
        final Type type = typeOf(file);
        if (type == null) {
            throw new IOException("Unsupported data file: " + file.getName());
        }
        if (!file.isFile()) {
            throw new IOException("File not found: " + file);
        }
        final Layer layer;
        switch (type) {
            case KML:
                layer = loadKml(file);
                break;
            case SHAPEFILE:
                layer = loadShapefile(file);
                break;
            default:
                layer = loadGeoJson(file);
                break;
        }
        layer.setName(file.getName());
        return layer;
    }

    private static Layer loadKml(final File file) throws IOException {
        final KMLRoot root;
        try {
            root = KMLRoot.createAndParse(file);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Invalid KML file " + file.getName() + ": " + e.getMessage(), e);
        }
        if (root == null) {
            throw new IOException("Not a KML file: " + file.getName());
        }
        final RenderableLayer layer = new RenderableLayer();
        layer.addRenderable(new KMLController(root));
        return layer;
    }

    private static Layer loadShapefile(final File file) throws IOException {
        // the factory reads the shapes in the background; wait for it so that errors reach the caller
        final CompletableFuture<Object> done = new CompletableFuture<>();
        new ShapefileLayerFactory().createFromShapefileSource(file, new ShapefileLayerFactory.CompletionCallback() {
            @Override
            public void completion(Object result) {
                done.complete(result);
            }

            @Override
            public void exception(Exception e) {
                done.completeExceptionally(e);
            }
        });
        try {
            final Object result = done.get(SHAPEFILE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!(result instanceof Layer)) {
                throw new IOException("No layer created from " + file.getName());
            }
            return (Layer) result;
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IOException("Invalid shapefile " + file.getName() + ": " + cause.getMessage(), cause);
        } catch (TimeoutException e) {
            throw new IOException("Reading " + file.getName() + " takes too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Reading " + file.getName() + " interrupted", e);
        }
    }

    private static Layer loadGeoJson(final File file) throws IOException {
        try {
            final Layer layer = new GeoJSONLoader().createLayerFromSource(file);
            // the loader logs parse errors and returns null, and returns an empty layer for JSON that is not GeoJSON
            if (layer == null || (layer instanceof RenderableLayer && ((RenderableLayer) layer).getNumRenderables() == 0)) {
                throw new IOException("No GeoJSON geometries in " + file.getName());
            }
            return layer;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Invalid GeoJSON file " + file.getName() + ": " + e.getMessage(), e);
        }
    }
}
