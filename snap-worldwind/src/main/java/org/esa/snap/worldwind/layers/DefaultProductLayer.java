/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.SurfaceImage;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.rcp.util.Dialogs;


import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Default Product Layer draws product outline
 */
public class DefaultProductLayer extends BaseLayer implements WWLayer {

    private boolean enableSurfaceImages;
    private final ConcurrentHashMap<String, ProductOutline> outlineTable = new ConcurrentHashMap<>();

    static class ProductOutline {
        Path[] productLineList;
        BandOutline[] bandOutlines;
        boolean showProductBoundary;
        PointPlacemark label;
        SurfaceImage image;
    }

    static class BandOutline {
        Path[] bandLineList;
        String bandName;
        PointPlacemark bandLabel;
        SurfaceImage bandImage;
    }

    public DefaultProductLayer() {
        this.setName("Products");
    }

    public void setEnableSurfaceImages(final boolean enableSurfaceImages) {
        this.enableSurfaceImages = enableSurfaceImages;
    }

    public String[] getProductNames() {
        final List<String> list = new ArrayList<>(outlineTable.keySet());
        Collections.sort(list);
        return list.toArray(new String[0]);
    }

    @Override
    public void setOpacity(double opacity) {
        super.setOpacity(opacity);

        for (String name : outlineTable.keySet()) {
            setOpacity(name, opacity);
        }
    }

    public void setOpacity(String name, double opacity) {
        final ProductOutline outline = outlineTable.get(name);
        if (outline != null) {
            if(outline.image != null) {
                outline.image.setOpacity(opacity);
            } else {
                for(BandOutline bandOutline : outline.bandOutlines) {
                    if(bandOutline.bandImage != null) {
                        bandOutline.bandImage.setOpacity(opacity);
                    }
                }
            }
        }
    }

    public double getOpacity(String name) {
        final ProductOutline outline = outlineTable.get(name);
        if (outline != null) {
            if(outline.image != null) {
                return outline.image.getOpacity();
            } else {
                for(BandOutline bandOutline : outline.bandOutlines) {
                    if(bandOutline.bandImage != null) {
                        return bandOutline.bandImage.getOpacity();
                    }
                }
            }
            return 1;
        }
        return 0;
    }

    @Override
    public void setSelectedProduct(final Product product) {
        super.setSelectedProduct(product);

        if (selectedProduct != null) {
            final String selName = getUniqueName(selectedProduct);
            for (String name : outlineTable.keySet()) {
                final ProductOutline productOutline = outlineTable.get(name);
                final boolean highlight = name.equals(selName);
                if(productOutline.showProductBoundary) {
                    for (Path line : productOutline.productLineList) {
                        line.setHighlighted(highlight);
                    }
                } else {
                    for(BandOutline bandOutline : productOutline.bandOutlines) {
                        for (Path line : bandOutline.bandLineList) {
                            line.setHighlighted(highlight);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setSelectedRaster(final RasterDataNode raster) {
        super.setSelectedRaster(raster);

        if (selectedRaster != null && selectedProduct != null) {
            final String selName = getUniqueName(selectedProduct);
            for (String name : outlineTable.keySet()) {
                final ProductOutline productOutline = outlineTable.get(name);
                final boolean isProduct = name.equals(selName);

                for(BandOutline bandOutline : productOutline.bandOutlines) {
                    boolean highlight = isProduct && bandOutline.bandName.equals(selectedRaster.getName());
                    bandOutline.bandLabel.setVisible(highlight);
                    for (Path line : bandOutline.bandLineList) {
                        line.setHighlighted(highlight);
                    }
                }
            }
        }
    }

    @Override
    public Suitability getSuitability(Product product) {
        return Suitability.SUITABLE;
    }

    @Override
    public void addProduct(final Product product, WorldWindowGLCanvas wwd) {

        final String name = getUniqueName(product);
        if (this.outlineTable.get(name) != null)
            return;

        final String productType = product.getProductType();
        if (productType.equals("ASA_WVW_2P") || productType.equals("ASA_WVS_1P") || productType.equals("ASA_WVI_1P")) {
            addWaveProduct(product);
        } else {
            // add outline
            addOutline(product);

            if (enableSurfaceImages && canAddSurfaceImage(product)) {
                addSurfaceImage(product);
            }
        }
    }

    private boolean canAddSurfaceImage(final Product product) {
        return product.getSceneGeoCoding() != null && InputProductValidator.isMapProjected(product);
    }

    private void addSurfaceImage(final Product product) {
        final String name = getUniqueName(product);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected SurfaceImage doInBackground() {
                try {
                    final Product newProduct = createSubsampledProduct(product);
                    final Band band = newProduct.getBandAt(0);
                    final BufferedImage image = ProductUtils.createRgbImage(new RasterDataNode[]{band},
                                                                            band.getImageInfo(com.bc.ceres.core.ProgressMonitor.NULL),
                                                                            com.bc.ceres.core.ProgressMonitor.NULL);

                    final GeoPos geoPos1 = product.getSceneGeoCoding().getGeoPos(new PixelPos(0, 0), null);
                    final GeoPos geoPos2 = product.getSceneGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1,
                                                                                         product.getSceneRasterHeight() - 1),
                                                                            null
                    );

                    final Sector sector = new Sector(Angle.fromDegreesLatitude(geoPos1.getLat()),
                                                     Angle.fromDegreesLatitude(geoPos2.getLat()),
                                                     Angle.fromDegreesLongitude(geoPos1.getLon()),
                                                     Angle.fromDegreesLongitude(geoPos2.getLon()));

                    final SurfaceImage si = new SurfaceImage(image, sector);
                    si.setOpacity(getOpacity());
                    return si;
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                return null;
            }

            @Override
            public void done() {

                try {
                    final SurfaceImage si = (SurfaceImage) get();
                    if (si != null) {
                        addRenderable(si);

                        ProductOutline outline = outlineTable.get(name);
                        outline.image = si;
                    }
                } catch (Exception e) {
                    Dialogs.showError(e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void addOutline(final Product product) {

        final int prodWidth = product.getSceneRasterWidth();
        final int prodHeight = product.getSceneRasterHeight();
        final int step = Math.max(50, (prodWidth + prodHeight) / 10);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Future<List<GeneralPath>> productBoundaryPathsFuture = null;
        if(product.getSceneGeoCoding() != null) {
            productBoundaryPathsFuture = executor.submit(() ->
                    List.of(org.esa.snap.core.util.GeoUtils.createGeoBoundaryPaths(product, null, step, true)));
        }

        final List<BandOutline> bandOutlines = new ArrayList<>();
        for (Band band : product.getBands()) {
            if (band.getGeoCoding() != null &&
                    !(band.getRasterWidth() == prodWidth && band.getRasterHeight() == prodHeight)) {
                executor.submit(() -> {
                    List<GeneralPath> paths = List.of(org.esa.snap.core.util.GeoUtils.createGeoBoundaryPaths(band, null, step, true));
                    synchronized (bandOutlines) {
                        List<Path> bandBoundaryPaths = new ArrayList<>();
                        for (GeneralPath bandBoundaryPath : paths) {
                            bandBoundaryPaths.add(createPolyLine(bandBoundaryPath));
                        }

                        Position centrePos = calculateBoundingSphereCenter(bandBoundaryPaths.get(0));
                        PointPlacemark ppm = getLabelPlacemark(centrePos, band.getName());
                        ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                        ppm.setEnableDecluttering(true);
                        ppm.setVisible(false);

                        addRenderable(ppm);

                        BandOutline bandOutline = new BandOutline();
                        bandOutline.bandName = band.getName();
                        bandOutline.bandLineList = bandBoundaryPaths.toArray(new Path[0]);
                        bandOutline.bandLabel = ppm;
                        bandOutlines.add(bandOutline);
                    }
                });
            }
        }
        executor.shutdown();

        List<GeneralPath> boundaryPaths = null;
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            boundaryPaths = productBoundaryPathsFuture != null ? productBoundaryPathsFuture.get() : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }

        boolean showProductBoundary = bandOutlines.isEmpty() || bandOutlines.size() != product.getNumBands();

        for (BandOutline bandOutline : bandOutlines) {
            for(Path polyLine : bandOutline.bandLineList) {
                addRenderable(polyLine);
            }
        }

        Path mainPath = null;
        final List<Path> productLineList = new ArrayList<>();
        if(boundaryPaths != null) {
            for (GeneralPath boundaryPath : boundaryPaths) {
                Path polyLine = createPolyLine(boundaryPath);

                if (showProductBoundary) {
                    addRenderable(polyLine);
                }
                if(mainPath == null) {
                    mainPath = polyLine;
                }
                productLineList.add(polyLine);
            }
        } else {
            List<Position> unionPositions = new ArrayList<>();
            for (BandOutline bandOutline : bandOutlines) {
                for(Path polyLine : bandOutline.bandLineList) {
                    unionPositions.addAll((Collection<? extends Position>) polyLine.getPositions());
                }
            }
            Path unionPath = createPath(unionPositions, WHITE_MATERIAL, RED_MATERIAL);
            if (showProductBoundary) {
                addRenderable(unionPath);
            }
            mainPath = unionPath;
            productLineList.add(unionPath);
        }

        if(mainPath == null) {
            return;
        }

        Position centrePos = calculateBoundingSphereCenter(mainPath);
        PointPlacemark ppm = getLabelPlacemark(centrePos, String.valueOf(product.getRefNo()));
        ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        ppm.setEnableDecluttering(true);

        addRenderable(ppm);

        String name = getUniqueName(product);
        ProductOutline productOutline = new ProductOutline();
        productOutline.productLineList = productLineList.toArray(new Path[0]);
        productOutline.bandOutlines = bandOutlines.toArray(new BandOutline[0]);
        productOutline.label = ppm;
        productOutline.showProductBoundary = showProductBoundary;
        outlineTable.put(name, productOutline);
    }

    private Position calculateBoundingSphereCenter(final Path polyline) {

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (Position pos : polyline.getPositions()) {
            minLat = Math.min(minLat, pos.getLatitude().getRadians());
            maxLat = Math.max(maxLat, pos.getLatitude().getRadians());
            minLon = Math.min(minLon, pos.getLongitude().getRadians());
            maxLon = Math.max(maxLon, pos.getLongitude().getRadians());
        }

        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;

        return new Position(Angle.fromRadiansLatitude(centerLat),
                Angle.fromRadiansLongitude(centerLon), 0.0);
    }

    private Path createPolyLine(final GeneralPath boundaryPath) {
        final PathIterator it = boundaryPath.getPathIterator(null);
        final float[] floats = new float[2];
        final List<Position> positions = new ArrayList<>();

        it.currentSegment(floats);
        final Position firstPosition = new Position(Angle.fromDegreesLatitude(floats[1]),
                Angle.fromDegreesLongitude(floats[0]), 0.0);
        positions.add(firstPosition);
        it.next();

        while (!it.isDone()) {
            it.currentSegment(floats);
            positions.add(new Position(Angle.fromDegreesLatitude(floats[1]),
                    Angle.fromDegreesLongitude(floats[0]), 0.0));
            it.next();
        }
        // close the loop
        positions.add(firstPosition);

        return createPath(positions, WHITE_MATERIAL, RED_MATERIAL);
    }

    private void addWaveProduct(final Product product) {
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement ggADS = root.getElement("GEOLOCATION_GRID_ADS");
        if (ggADS == null) return;

        final MetadataElement[] geoElemList = ggADS.getElements();
        final Path[] lineList = new Path[geoElemList.length];
        int cnt = 0;

        int numPoints = 0;
        double centreLat = 0;
        double centreLon = 0;

        for (MetadataElement geoElem : geoElemList) {
            final double lat = geoElem.getAttributeDouble("center_lat", 0.0) / Constants.oneMillion;
            final double lon = geoElem.getAttributeDouble("center_long", 0.0) / Constants.oneMillion;
            final double heading = geoElem.getAttributeDouble("heading", 0.0);

            final GeoUtils.LatLonHeading r1 = GeoUtils.vincenty_direct(new GeoPos(lat, lon), 5000, heading);
            final GeoUtils.LatLonHeading corner1 = GeoUtils.vincenty_direct(new GeoPos(r1.lat, r1.lon), 2500, heading - 90.0);
            final GeoUtils.LatLonHeading corner2 = GeoUtils.vincenty_direct(new GeoPos(r1.lat, r1.lon), 2500, heading + 90.0);

            final GeoUtils.LatLonHeading r2 = GeoUtils.vincenty_direct(new GeoPos(lat, lon), 5000, heading + 180.0);
            final GeoUtils.LatLonHeading corner3 = GeoUtils.vincenty_direct(new GeoPos(r2.lat, r2.lon), 2500, heading - 90.0);
            final GeoUtils.LatLonHeading corner4 = GeoUtils.vincenty_direct(new GeoPos(r2.lat, r2.lon), 2500, heading + 90.0);

            final List<Position> positions = new ArrayList<>();
            positions.add(new Position(Angle.fromDegreesLatitude(corner1.lat), Angle.fromDegreesLongitude(corner1.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner2.lat), Angle.fromDegreesLongitude(corner2.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner4.lat), Angle.fromDegreesLongitude(corner4.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner3.lat), Angle.fromDegreesLongitude(corner3.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner1.lat), Angle.fromDegreesLongitude(corner1.lon), 0.0));

            centreLat += corner1.lat;
            centreLon += corner1.lon;

            centreLat += corner2.lat;
            centreLon += corner2.lon;

            centreLat += corner3.lat;
            centreLon += corner3.lon;

            centreLat += corner4.lat;
            centreLon += corner4.lon;

            numPoints += 4;

            final Path line = createPath(positions, WHITE_MATERIAL, RED_MATERIAL);

            addRenderable(line);
            lineList[cnt++] = line;
        }

        centreLat = centreLat / numPoints;
        centreLon = centreLon / numPoints;
        Position centrePos = new Position(Angle.fromDegreesLatitude(centreLat), Angle.fromDegreesLongitude(centreLon), 0.0);

        PointPlacemark ppm = getLabelPlacemark(centrePos, String.valueOf(product.getRefNo()));
        ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        ppm.setEnableDecluttering(true);
        addRenderable(ppm);

        String name = getUniqueName(product);
        ProductOutline productOutline = new ProductOutline();
        productOutline.productLineList = lineList;
        productOutline.label = ppm;
        outlineTable.put(name, productOutline);
    }

    private PointPlacemark getLabelPlacemark(Position pos, String label) {
        PointPlacemarkAttributes ppmAtttrs = new PointPlacemarkAttributes();
        ppmAtttrs.setLabelOffset(new Offset(0.0, 0.0, AVKey.PIXELS, AVKey.PIXELS));
        ppmAtttrs.setScale(0.0);

        PointPlacemark ppm = new PointPlacemark(pos);
        ppm.setAttributes(ppmAtttrs);
        ppm.setLabelText(label);
        return ppm;
    }

    @Override
    public void removeProduct(final Product product) {
        String imagePath = getUniqueName(product);

        final ProductOutline productOutline = this.outlineTable.get(imagePath);
        if (productOutline != null) {
            for (Path line : productOutline.productLineList) {
                removeRenderable(line);
            }
            for(BandOutline bandOutline : productOutline.bandOutlines) {
                for (Path line : bandOutline.bandLineList) {
                    removeRenderable(line);
                }
                removeRenderable(bandOutline.bandLabel);
                if(bandOutline.bandImage != null) {
                    removeRenderable(bandOutline.bandImage);
                }
            }
            removeRenderable(productOutline.label);
            if(productOutline.image != null) {
                removeRenderable(productOutline.image);
            }
            outlineTable.remove(imagePath);
        }
    }

    private static Product createSubsampledProduct(final Product product) throws IOException {

        final String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
        int scaleFactor = product.getSceneRasterWidth() / 1000;
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        productSubsetDef.setSubSampling(scaleFactor, scaleFactor);
        productSubsetDef.setTreatVirtualBandsAsRealBands(true);
        productSubsetDef.setNodeNames(new String[]{quicklookBandName});
        Product productSubset = product.createSubset(productSubsetDef, quicklookBandName, null);

        if (!InputProductValidator.isMapProjected(product) && productSubset.getSceneGeoCoding() != null) {
            try {
                final Map<String, Object> projParameters = new HashMap<>();
                Map<String, Product> projProducts = new HashMap<>();
                projProducts.put("source", productSubset);
                projParameters.put("crs", "WGS84(DD)");
                productSubset = GPF.createProduct("Reproject", projParameters, projProducts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return productSubset;
    }

    @Override
    public JPanel getControlPanel(final WorldWindowGLCanvas wwd) {
        final JSlider opacitySlider = new JSlider();
        opacitySlider.setMaximum(100);
        opacitySlider.setValue((int) (getOpacity() * 100));
        opacitySlider.setEnabled(true);
        opacitySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int value = opacitySlider.getValue();
                setOpacity(value / 100d);
                wwd.repaint();
            }
        });

        //theSelectedObjectLabel = new JLabel("Selected: ");

        final JPanel opacityPanel = new JPanel(new BorderLayout(5, 5));
        opacityPanel.add(new JLabel("Image Opacity"), BorderLayout.WEST);
        opacityPanel.add(opacitySlider, BorderLayout.CENTER);
        return opacityPanel;
    }
}

