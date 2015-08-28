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

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.BufferFactory;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.util.DirectedPath;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.worldwind.ColorBarLegend;
import org.esa.snap.worldwind.ProductRenderablesInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;

/**
    S-1 L2 OCN visualization
 */
public class Level2ProductLayer extends BaseLayer implements WWLayer {

    private static double HUE_BLUE = 240d / 360d;
    private static double HUE_RED = 0d / 360d;
    private static double HUE_MAX_RED = 1.0;

    private static boolean theOWILimitChanged = false;

    private static boolean theRVLLimitChanged = false;

    //public double theCurrMinHue;
    //public double theCurrMaxHue;

    //private AnalyticSurface analyticSurface = null;
    //private BufferWrapper analyticSurfaceValueBuffer = null;

    private final HashMap<String, ColorBarLegend> theColorBarLegendHash = new HashMap<>();

    // product associated with the current colorBar legend
    private Product theColorBarLegendProduct = null;
    private String theSelectedComp = null;

    private final HashMap<DirectedPath, String> theObjectInfoHash = new HashMap<>();
    private final HashMap<Product, ProductRenderablesInfo> theProductRenderablesInfoHash = new HashMap<>();

    //public ShapeAttributes dpAttrs = null;
    //public ShapeAttributes dpHighlightAttrs = null;

    public ScreenAnnotation theInfoAnnotation;

    private DirectedPath theLastSelectedDP = null;

    public Level2ProductLayer() {
        this.setName("S-1 Level-2 OCN");

        //dpHighlightAttrs = new BasicShapeAttributes();
        //dpHighlightAttrs.setOutlineMaterial(Material.WHITE);
        //dpHighlightAttrs.setOutlineWidth(2d);

        // this is copied from gov.nasa.worldwindx.examples.util.LayerManagerLayer
        theInfoAnnotation = new ScreenAnnotation("", new Point(120, 520));

        // Set annotation so that it will not force text to wrap (large width) and will adjust it's width to
        // that of the text. A height of zero will have the annotation height follow that of the text too.
        theInfoAnnotation.getAttributes().setSize(new Dimension(Integer.MAX_VALUE, 0));
        theInfoAnnotation.getAttributes().setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);

        // Set appearance attributes
        theInfoAnnotation.getAttributes().setCornerRadius(0);
        //theInfoAnnotation.getAttributes().setFont(this.font);
        theInfoAnnotation.getAttributes().setHighlightScale(1);
        theInfoAnnotation.getAttributes().setTextColor(Color.WHITE);
        theInfoAnnotation.getAttributes().setBackgroundColor(new Color(0f, 0f, 0f, .5f));
        theInfoAnnotation.getAttributes().setInsets(new Insets(6, 6, 6, 6));
        theInfoAnnotation.getAttributes().setBorderWidth(1);

        theInfoAnnotation.getAttributes().setVisible(false);
    }

    public void updateInfoAnnotation(final SelectEvent event) {

        if (event.getEventAction().equals(SelectEvent.ROLLOVER) && (event.getTopObject() instanceof DirectedPath)) {
            SystemUtils.LOG.info("click " + event.getTopObject());

            SystemUtils.LOG.info("DirectedPath:::");
            DirectedPath dp = (DirectedPath) event.getTopObject();
            //dp.getAttributes().setOutlineMaterial(Material.WHITE);
            dp.setHighlighted(true);
            //dp.setAttributes(productLayer.dpHighlightAttrs);
            //theSelectedObjectLabel.setText("" + productLayer.theObjectInfoHash.get(dp));

            theInfoAnnotation.setText(theObjectInfoHash.get(dp));
            theInfoAnnotation.getAttributes().setVisible(true);
            theLastSelectedDP = dp;
            //SystemUtils.LOG.info("selectedProduct " + getSelectedProduct());
            //final ExecCommand command = datApp.getCommandManager().getExecCommand("showPolarWaveView");
            //command.execute(2);
        } else {

            if (theLastSelectedDP != null) {
                theLastSelectedDP.setHighlighted(false);
            }
            theInfoAnnotation.getAttributes().setVisible(false);
            //theSelectedObjectLabel.setText("");
        }
    }

    public void addProduct(final Product product, final WorldWindowGLCanvas wwd) {

        if(!product.getProductType().equalsIgnoreCase("OCN")) {
            return;
        }

        addRenderable(theInfoAnnotation);

        final String text = "First line<br />Second line";
        theInfoAnnotation.setText(text);
        theInfoAnnotation.getAttributes().setVisible(false);

        theColorBarLegendProduct = product;
        final ProductRenderablesInfo productRenderablesInfo = new ProductRenderablesInfo ();
        // There is code in LayerMagerLayer that updates the size
        //  it's re-rendered
        // Update current size and adjust annotation draw offset according to it's width
        //this.size = theInfoAnnotation.getPreferredSize(dc);
        //this.annotation.getAttributes().setDrawOffset(new Point(this.size.width / 2, 0));

        SystemUtils.LOG.info("product " + product.getName());

        String prefix = "vv";

        if (theColorBarLegendProduct.getBand(prefix + "_001_owiLon") == null) {
            prefix = "hh";
        }
        final Band owiLonBand = theColorBarLegendProduct.getBand(prefix + "_001_owiLon");
        final Band owiLatBand = theColorBarLegendProduct.getBand(prefix + "_001_owiLat");
        final Band owiIncAngleBand = theColorBarLegendProduct.getBand(prefix + "_001_owiIncidenceAngle");
        final Band owiWindSpeedBand = theColorBarLegendProduct.getBand(prefix + "_001_owiWindSpeed");
        final Band owiWindDirBand = theColorBarLegendProduct.getBand(prefix + "_001_owiWindDirection");
        final Band rvlRadVelBand = theColorBarLegendProduct.getBand(prefix + "_001_rvlRadVel");

        final Band waveLonBand = theColorBarLegendProduct.getBand(prefix + "_001_oswLon");
        final Band waveLatBand = theColorBarLegendProduct.getBand(prefix + "_001_oswLat");
        final Band waveHeightBand = theColorBarLegendProduct.getBand(prefix + "_001_oswHs");
        final Band waveLengthBand = theColorBarLegendProduct.getBand(prefix + "_001_oswWl");
        final Band waveDirBand = theColorBarLegendProduct.getBand(prefix + "_001_oswDirmet");

        //final Band oswLonBand = theColorBarLegendProduct.getBand("hh_001_oswLon");
        //final Band oswLatBand = theColorBarLegendProduct.getBand("hh_001_oswLat");
        //final Band oswWindDirBand = theColorBarLegendProduct.getBand("hh_001_oswWindDirection");

        //final Band rvlLonBand = theColorBarLegendProduct.getBand("hh_001_rvlLon");
        //final Band rvlLatBand = theColorBarLegendProduct.getBand("hh_001_rvlLat");
        //final Band rvlRadVelBand = theColorBarLegendProduct.getBand("hh_001_rvlRadVel");

        //final Band band = theColorBarLegendProduct.getBand();

        final GeoPos geoPos1 = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null);
        final GeoPos geoPos2 = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1,
                                                                             product.getSceneRasterHeight() - 1), null);

        try {
            if(owiLonBand != null) {
                final float[] lonValues = new float[owiLonBand.getRasterWidth() * owiLonBand.getRasterHeight()];
                owiLonBand.readPixels(0, 0, owiLonBand.getRasterWidth(), owiLonBand.getRasterHeight(), lonValues, ProgressMonitor.NULL);

                final float[] latValues = new float[owiLatBand.getRasterWidth() * owiLatBand.getRasterHeight()];
                owiLatBand.readPixels(0, 0, owiLatBand.getRasterWidth(), owiLatBand.getRasterHeight(), latValues, ProgressMonitor.NULL);

                final double[] incAngleValues = new double[owiIncAngleBand.getRasterWidth() * owiIncAngleBand.getRasterHeight()];
                owiIncAngleBand.readPixels(0, 0, owiIncAngleBand.getRasterWidth(), owiIncAngleBand.getRasterHeight(), incAngleValues, ProgressMonitor.NULL);

                final double[] windSpeedValues = new double[owiWindSpeedBand.getRasterWidth() * owiWindSpeedBand.getRasterHeight()];
                owiWindSpeedBand.readPixels(0, 0, owiWindSpeedBand.getRasterWidth(), owiWindSpeedBand.getRasterHeight(), windSpeedValues, ProgressMonitor.NULL);

                final double[] windDirValues = new double[owiWindDirBand.getRasterWidth() * owiWindDirBand.getRasterHeight()];
                owiWindDirBand.readPixels(0, 0, owiWindDirBand.getRasterWidth(), owiWindDirBand.getRasterHeight(), windDirValues, ProgressMonitor.NULL);

                final int[] cellSizeArr = {4, 8, 16, 24, 32, 40, 48, 64};

                for (int cellSizeInd = 0; cellSizeInd < cellSizeArr.length; cellSizeInd++) {
                    double minHeight = 0;
                    double maxHeight = cellSizeArr[cellSizeInd] * 0.5e6 / 16;
                    if (cellSizeInd > 0) {
                        minHeight = cellSizeArr[cellSizeInd - 1] * 0.5e6 / 16;
                    }
                    addWindSpeedArrows(latValues, lonValues, incAngleValues, windSpeedValues, windDirValues,
                                       owiLonBand.getRasterWidth(), owiLonBand.getRasterHeight(),
                                       cellSizeArr[cellSizeInd], minHeight, maxHeight,
                                       productRenderablesInfo.theRenderableListHash.get("owi"));
                    //addWindSpeedArrows(waveLatValues, waveLonValues, incAngleValues, windSpeedValues, windDirValues,
                    // waveLonBand.getRasterWidth(), waveLonBand.getRasterHeight(), cellSizeArr[cellSizeInd],
                    // minHeight, maxHeight, productRenderablesInfo.theRenderableListHash.get("osw"));
                }

                createColorSurfaceWithGradient(geoPos1, geoPos2, windSpeedValues,
                                               owiWindSpeedBand.getRasterWidth(), owiWindSpeedBand.getRasterHeight(),
                                               0, 10, false, productRenderablesInfo.theRenderableListHash.get("owi"),
                                               productRenderablesInfo, "owi");
            }

            if (rvlRadVelBand != null) {
                final double[] rvlRadVelValues = new double[rvlRadVelBand.getRasterWidth() * rvlRadVelBand.getRasterHeight()];
                rvlRadVelBand.readPixels(0, 0, rvlRadVelBand.getRasterWidth(), rvlRadVelBand.getRasterHeight(), rvlRadVelValues, ProgressMonitor.NULL);

                createColorSurfaceWithGradient(geoPos1, geoPos2,
                                               rvlRadVelValues, rvlRadVelBand.getRasterWidth(), rvlRadVelBand.getRasterHeight(),
                                               -6, 6, true, productRenderablesInfo.theRenderableListHash.get("rvl"),
                                               productRenderablesInfo, "rvl");
            }

            if (waveLonBand != null) {
                final double[] waveLonValues = new double[waveLonBand.getRasterWidth() * waveLonBand.getRasterHeight()];
                waveLonBand.readPixels(0, 0, waveLonBand.getRasterWidth(), waveLonBand.getRasterHeight(), waveLonValues, ProgressMonitor.NULL);

                final double[] waveLatValues = new double[waveLatBand.getRasterWidth() * waveLatBand.getRasterHeight()];
                waveLatBand.readPixels(0, 0, waveLatBand.getRasterWidth(), waveLatBand.getRasterHeight(), waveLatValues, ProgressMonitor.NULL);

                final double[] waveHeightValues = new double[waveHeightBand.getRasterWidth() * waveHeightBand.getRasterHeight()];
                waveHeightBand.readPixels(0, 0, waveHeightBand.getRasterWidth(), waveHeightBand.getRasterHeight(), waveHeightValues, ProgressMonitor.NULL);

                final double[] waveLengthValues = new double[waveLengthBand.getRasterWidth() * waveLengthBand.getRasterHeight()];
                waveLengthBand.readPixels(0, 0, waveLengthBand.getRasterWidth(), waveLengthBand.getRasterHeight(), waveLengthValues, ProgressMonitor.NULL);

                final double[] waveDirValues = new double[waveDirBand.getRasterWidth() * waveDirBand.getRasterHeight()];
                waveDirBand.readPixels(0, 0, waveDirBand.getRasterWidth(), waveDirBand.getRasterHeight(), waveDirValues, ProgressMonitor.NULL);

                addWaveLengthArrows(waveLatValues, waveLonValues, waveLengthValues, waveDirValues,
                                    waveLonBand.getRasterWidth(), waveLonBand.getRasterHeight(),
                                    productRenderablesInfo.theRenderableListHash.get("osw"));

                createColorSurfaceWithGradient(geoPos1, geoPos2, waveHeightValues, waveHeightBand.getRasterWidth(),
                                               waveHeightBand.getRasterHeight(), 0, 10, false,
                                               productRenderablesInfo.theRenderableListHash.get("osw"),
                                               productRenderablesInfo, "osw");
            }

            theProductRenderablesInfoHash.put(product, productRenderablesInfo);

            setComponentVisible(theSelectedComp, wwd);

        } catch (Exception e) {
            SnapApp.getDefault().handleError("L2ProductLayer unable to add product "+product.getName(), e);
        }
    }

    public void createColorSurfaceWithGradient(GeoPos geoPos1, GeoPos geoPos2, double[] values, int width, int height,
                                               double minValue, double maxValue, boolean whiteZero, ArrayList<Renderable> renderableList, ProductRenderablesInfo prodRenderInfo, String comp) {
        createColorSurface(geoPos2.getLat(), geoPos1.getLat(), geoPos1.getLon(), geoPos2.getLon(), values, width, height,
                           renderableList, prodRenderInfo, comp);
        //createColorSurface(geoPos2.getLat(), geoPos1.getLat(), geoPos1.getLon(), geoPos2.getLon(), rvlRadVelValues, 40, 40, minValue, maxValue, renderableList);

        //theCurrMinHue = minHue;
        //theCurrMaxHue = maxHue;

        //createRandomColorSurface(25, 35, -110, -100, HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
        //createRandomColorSurface(geoPos2.getLat(),  geoPos1.getLat(), 55, 57, HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
        System.out.println("geoPos1.getLat(), geoPos2.getLat(), geoPos1.getLon(), geoPos2.getLon() " + geoPos1.getLat() + " " + geoPos2.getLat() + " " + geoPos1.getLon() + " " + geoPos2.getLon());

        // don't create color legend if one already exists
        if (theColorBarLegendHash.get(comp) != null) {
            // use the existing limits
            minValue = theColorBarLegendHash.get(comp).getMinValue();
            maxValue = theColorBarLegendHash.get(comp).getMaxValue();
        }

        createColorGradient(minValue, maxValue, whiteZero, prodRenderInfo, comp);
    }

    public void createColorBarLegend(double minValue, double maxValue, String title, String comp) {
        //SystemUtils.LOG.info("createColorBarLegend " + minValue + " " + maxValue);

        final Format legendLabelFormat = new DecimalFormat("# m/s");

        final ColorBarLegend colorBarLegend = new ColorBarLegend();
        colorBarLegend.setColorGradient(32, 256, minValue, maxValue, HUE_RED, HUE_MAX_RED,
                Color.WHITE,
                ColorBarLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
                ColorBarLegend.createDefaultTitle(title),
                comp.equalsIgnoreCase("rvl"));

        /*
        colorBarLegend = ColorBarLegend.fromColorGradient(32, 256, minValue, maxValue, minHue, maxHue,
                Color.WHITE,
                ColorBarLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
                ColorBarLegend.createDefaultTitle(title),
                whiteZero);
                */
        colorBarLegend.setOpacity(0.8);
        colorBarLegend.setScreenLocation(new Point(900, 320));
        //addRenderable(colorBarLegend);

        theColorBarLegendHash.put(comp, colorBarLegend);
    }

    public void setComponentVisible(String comp, WorldWindowGLCanvas wwd) {
        SystemUtils.LOG.info("setComponentVisible " + comp);

        for (String currComp : theColorBarLegendHash.keySet()) {
            if (theColorBarLegendHash.get(currComp) != null) {
                removeRenderable(theColorBarLegendHash.get(currComp));
                if (currComp.equals(comp)) {
                    addRenderable(theColorBarLegendHash.get(currComp));
                }

                ProductRenderablesInfo productRenderablesInfo = theProductRenderablesInfoHash.get(theColorBarLegendProduct);
                if (productRenderablesInfo != null) {
                    ArrayList<Renderable> renderableList = productRenderablesInfo.theRenderableListHash.get(currComp);
                    for (Renderable renderable : renderableList) {
                        removeRenderable(renderable);
                        if (currComp.equals(comp)) {
                            //System.out.println("renderable " + renderable);
                            addRenderable(renderable);
                        }
                    }
                }
            }
        }

        wwd.redrawNow();
    }

    private void addWindSpeedArrows(float[] latValues,
                                     float[] lonValues,
                                     double[] incAngleValues,
                                     double[] windSpeedValues,
                                     double[] windDirValues,
                                     int width,
                                     int height,
                                     int cellSize,
                                     double minHeight,
                                     double maxHeight,
                                     ArrayList<Renderable> renderableList) {
        float pixelWidth = Math.abs(lonValues[0] - lonValues[lonValues.length - 1]) / width;
        float pixelHeight = Math.abs(latValues[0] - latValues[latValues.length - 1]) / height;

        //SystemUtils.LOG.info("pixelWidth " + pixelWidth + " pixelHeight " + pixelHeight);

        //System.out.println("pixelWidth " + pixelWidth + " pixelHeight " + pixelHeight);

        // take half of the smaller dimension
        float arrowLength = pixelWidth;
        if (pixelHeight < pixelWidth) {
            arrowLength = pixelHeight;
        }
        arrowLength = arrowLength * cellSize / 2;

        final ShapeAttributes dpAttrs = new BasicShapeAttributes();
        dpAttrs.setOutlineMaterial(Material.BLACK);
        dpAttrs.setOutlineWidth(2d);
        for (int row = 0; row < height; row=row+cellSize) {
            for (int col = 0; col < width; col=col+cellSize) {
                //int i = row*width + col;
                int globalInd = row*width + col;
                float avgLat = 0;
                float avgLon = 0;
                double avgIncAngle = 0;
                double avgWindSpeed = 0;
                double avgWindDir = 0;
                int finalCellRow = row +  cellSize;
                int finalCellCol = col +  cellSize;

                if (finalCellRow > height) {
                    finalCellRow = height;
                }
                if (finalCellCol > width) {
                    finalCellCol = width;
                }
                for (int currCellRow = row; currCellRow < finalCellRow; currCellRow++) {
                    for (int currCellCol = col; currCellCol < finalCellCol; currCellCol++) {
                        int i = currCellRow*width + currCellCol;
                        avgLat += latValues[i];
                        avgLon += lonValues[i];
                        avgIncAngle += incAngleValues[i];
                        avgWindSpeed += windSpeedValues[i];
                        avgWindDir += windDirValues[i];
                    }
                }

                avgLat = avgLat / ((finalCellRow - row) * (finalCellCol - col));
                avgLon = avgLon / ((finalCellRow - row) * (finalCellCol - col));
                avgIncAngle = avgIncAngle / ((finalCellRow - row) * (finalCellCol - col));
                avgWindSpeed = avgWindSpeed / ((finalCellRow - row) * (finalCellCol - col));
                avgWindDir = avgWindDir / ((finalCellRow - row) * (finalCellCol - col));

                           /*
                           avgLat = latValues[globalInd];
                           avgLon = lonValues[globalInd];
                           avgWindDir = windDirValues[globalInd];
                            */
                            /*
                           System.out.println("avgLat " + avgLat);
                           System.out.println("avgLon " + avgLon);
                           System.out.println("avgWindDir " + avgWindDir);
                            */
                //System.out.println("avgIncAngle " + avgIncAngle);
                //for (int i = 0; i < latValues.length; i=i+50) {
                //System.out.println(lonValues[i] + "::==::" + latValues[i] + "::==::" + incAngleValues[i] + "::==::" + windSpeedValues[i] + "::==::" + windDirValues[i] + "::==::");
                final Position startPos = new Position(Angle.fromDegreesLatitude(avgLat), Angle.fromDegreesLongitude(avgLon), 10.0);
                final Position endPos = new Position(LatLon.greatCircleEndPosition(startPos, Angle.fromDegrees(avgWindDir), Angle.fromDegrees(arrowLength)), 10.0);

                //System.out.println("startPos " + startPos + " endPos " + endPos);

                final ArrayList<Position> positions = new ArrayList<>();
                positions.add(startPos);
                positions.add(endPos);

                final DirectedPath directedPath = new DirectedPath(positions);
                Renderable renderable = new Renderable() {
                    public void render (DrawContext dc) {

                        directedPath.setAttributes(dpAttrs);
                        //directedPath.setHighlightAttributes(highlightAttrs);
                        directedPath.setVisible(true);
                        directedPath.setFollowTerrain(true);
                        directedPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
                        directedPath.setPathType(AVKey.GREAT_CIRCLE);
                        //directedPath.setHighlighted(true);
                        // this is the length of the arrow head actually
                        double arrowHeadLength = computeSegmentLength(directedPath, dc, startPos, endPos) / 4;
                        directedPath.setArrowLength(arrowHeadLength);
                        //double maxHeight = cellSize * 0.5e6 / 16;

                        if (dc.getView().getCurrentEyePosition().getAltitude() > minHeight && dc.getView().getCurrentEyePosition().getAltitude() < maxHeight) {
                            directedPath.render(dc);
                            //System.out.println("arrowHeadLength " + arrowHeadLength);
                        }

                        //System.out.println("eyePosition " + dc.getView().getCurrentEyePosition());
                    }
                };
                addRenderable(renderable);
                if (renderableList != null) {
                    renderableList.add(renderable);
                }
                String info = "Wind Speed: " + avgWindSpeed + "<br/>";
                info += "Wind Direction: " + avgWindDir + "<br/>";
                info += "Incidence Angle: " + avgIncAngle + "<br/>";
                theObjectInfoHash.put(directedPath, info);
            }
        }
    }

    private void addWaveLengthArrows(double[] latValues,
                                    double[] lonValues,
                                    double[] waveLengthValues,
                                    double[] waveDirValues,
                                    int width,
                                    int height,
                                    ArrayList<Renderable> renderableList) {

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {

                int i = row*width + col;
                double arrowLength = waveLengthValues[i] / 10000;
                final Position startPos = new Position(Angle.fromDegreesLatitude(latValues[i]), Angle.fromDegreesLongitude(lonValues[i]), 10.0);
                final Position endPos = new Position(LatLon.greatCircleEndPosition(startPos, Angle.fromDegrees(waveDirValues[i]), Angle.fromDegrees(arrowLength)), 10.0);

                //System.out.println("waveLengthValues[i] " + waveLengthValues[i]);

                final ArrayList<Position> positions = new ArrayList<>();
                positions.add(startPos);
                positions.add(endPos);

                final ShapeAttributes dpAttrs = new BasicShapeAttributes();
                dpAttrs.setOutlineMaterial(Material.BLACK);
                dpAttrs.setOutlineWidth(2d);


                final DirectedPath directedPath = new DirectedPath(positions);
                /*
                directedPath.setAttributes(dpAttrs);
                directedPath.setVisible(true);
                directedPath.setFollowTerrain(true);
                directedPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
                directedPath.setPathType(AVKey.GREAT_CIRCLE);
                double arrowHeadLength = computeSegmentLength(directedPath, getDrawContext(), startPos, endPos) / 4;
                directedPath.setArrowLength(arrowHeadLength);
                */


                Renderable renderable = new Renderable() {
                    public void render (DrawContext dc) {

                        directedPath.setAttributes(dpAttrs);
                        directedPath.setVisible(true);
                        directedPath.setFollowTerrain(true);
                        directedPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
                        directedPath.setPathType(AVKey.GREAT_CIRCLE);

                        // this is the length of the arrow head actually
                        double arrowHeadLength = computeSegmentLength(directedPath, dc, startPos, endPos) / 4;
                        directedPath.setArrowLength(arrowHeadLength);
                        directedPath.render(dc);


                        //System.out.println("eyePosition " + dc.getView().getCurrentEyePosition());
                    }
                };

                addRenderable(renderable);

                if (renderableList != null) {
                    renderableList.add(renderable);
                }
                String info = "Wave length: " + waveLengthValues[i] + "<br/>";
                //info += "Wind Direction: " + avgWindDir + "<br/>";
                //info += "Incidence Angle: " + avgIncAngle + "<br/>";
                theObjectInfoHash.put(directedPath, info);
            }
        }
    }


    private double computeSegmentLength(Path path, DrawContext dc, Position posA, Position posB)
    {
        final LatLon llA = new LatLon(posA.getLatitude(), posA.getLongitude());
        final LatLon llB = new LatLon(posB.getLatitude(), posB.getLongitude());

        Angle ang;
        String pathType = path.getPathType();
        if (pathType == AVKey.LINEAR) {
            ang = LatLon.linearDistance(llA, llB);
        } else if (pathType == AVKey.RHUMB_LINE || pathType == AVKey.LOXODROME) {
            ang = LatLon.rhumbDistance(llA, llB);
        } else { // Great circle
            ang = LatLon.greatCircleDistance(llA, llB);
        }

        if (path.getAltitudeMode() == WorldWind.CLAMP_TO_GROUND) {
            return ang.radians * (dc.getGlobe().getRadius());
        }

        final double height = 0.5 * (posA.getElevation() + posB.getElevation());
        return ang.radians * (dc.getGlobe().getRadius() + height * dc.getVerticalExaggeration());
    }

    // ADDED
    protected void createColorSurface(double minLat, double maxLat, double minLon, double maxLon, double[] values,
                                      int width, int height, ArrayList<Renderable> renderableList,
                                      ProductRenderablesInfo prodRenderInfo, String comp)
    {
        //double minValue = -200e3;
        //double maxValue = 200e3;
        SystemUtils.LOG.info("createColorSurface " + minLat + " " + maxLat + " " + minLon + " " + maxLon);

        AnalyticSurface analyticSurface = new AnalyticSurface();
        analyticSurface.setSector(Sector.fromDegrees(minLat, maxLat, minLon, maxLon));
        analyticSurface.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        analyticSurface.setDimensions(width, height);

        AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
        attr.setDrawShadow(false);
        attr.setInteriorOpacity(1.0);
        //attr.setOutlineWidth(3);
        attr.setDrawOutline(false);
        analyticSurface.setSurfaceAttributes(attr);

        analyticSurface.setClientLayer(this);
        //addRenderable(analyticSurface);

        //analyticSurfaceValueBuffer = randomGridValues(width, height, minValue, maxValue);
        BufferWrapper analyticSurfaceValueBuffer = (new BufferFactory.DoubleBufferFactory()).newBuffer(values.length);
        analyticSurfaceValueBuffer.putDouble(0, values, 0, values.length);

        //smoothValues(width, height, values, 0.5d);
        //scaleValues(values, values.length, minValue, maxValue);

        //mixValuesOverTime(2000L, firstBuffer, analyticSurfaceValueBuffer, minValue, maxValue, minHue, maxHue, analyticSurface);

        prodRenderInfo.setAnalyticSurfaceAndBuffer (analyticSurface, analyticSurfaceValueBuffer, comp);
        if (renderableList != null) {
            renderableList.add(analyticSurface);
        }
    }

    public void createColorGradient(double minValue, double maxValue, boolean whiteZero,
                                    ProductRenderablesInfo prodRenderInfo, String comp) {
        SystemUtils.LOG.info("createColorGradient " + minValue + " " + maxValue + " " + comp);
        AnalyticSurface analyticSurface = null;
        BufferWrapper analyticSurfaceValueBuffer = null;

        if (comp.equalsIgnoreCase("owi")) {
            analyticSurface = prodRenderInfo.owiAnalyticSurface;
            analyticSurfaceValueBuffer = prodRenderInfo.owiAnalyticSurfaceValueBuffer;
        }
        else if (comp.equalsIgnoreCase("osw")) {
            analyticSurface = prodRenderInfo.oswAnalyticSurface;
            analyticSurfaceValueBuffer = prodRenderInfo.oswAnalyticSurfaceValueBuffer;
        }
        else if (comp.equalsIgnoreCase("rvl")) {
            analyticSurface = prodRenderInfo.rvlAnalyticSurface;
            analyticSurfaceValueBuffer = prodRenderInfo.rvlAnalyticSurfaceValueBuffer;
        }

        if(analyticSurfaceValueBuffer != null) {
            final ArrayList<AnalyticSurface.GridPointAttributes> attributesList = new ArrayList<>();
            for (int i = 0; i < analyticSurfaceValueBuffer.length(); i++) {
                /*
                String s = analyticSurfaceValueBuffer.getDouble(i) + "";
                System.out.print(s.substring(0, 3) + " ");
                if (i % 82 == 0) {
                    System.out.println();
                }
                */
                double d = analyticSurfaceValueBuffer.getDouble(i);
                /*
                if (i % 82 == 0) {
                    d = 0.0;
                }
                */
                /*
                if (i / 93 == 0) {
                    d = 0.0;
                }
                */
                attributesList.add(
                        createColorGradientAttributes(d, minValue, maxValue, HUE_RED, HUE_MAX_RED, whiteZero));
            }

            analyticSurface.setValues(attributesList);
        }
    }

    // ADDED:
    // this method is copied from gov.nasa.worldwindx.examples.analytics.AnalyticSurface
    public static AnalyticSurface.GridPointAttributes createColorGradientAttributes(final double value,
                                                                                    double minValue, double maxValue,
                                                                                    double minHue, double maxHue,
                                                                                    boolean whiteZero)
    {
        final double hueFactor = WWMath.computeInterpolationFactor(value, minValue, maxValue);

        //double hue = WWMath.mixSmooth(hueFactor, minHue, maxHue);
        final double hue = WWMath.mix(hueFactor, minHue, maxHue);
        double sat = 1.0;
        if (whiteZero) {
            sat = Math.abs(WWMath.mixSmooth(hueFactor, -1, 1));
        }
        final Color color = Color.getHSBColor((float) hue, (float) sat, 1f);
        final double opacity = WWMath.computeInterpolationFactor(value, minValue, minValue + (maxValue - minValue) * 0.1);
        final Color rgbaColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * opacity));

        return AnalyticSurface.createGridPointAttributes(value, rgbaColor);
    }

    public void removeProduct(final Product product) {

        final ProductRenderablesInfo productRenderablesInfo = theProductRenderablesInfoHash.get(product);
        if (productRenderablesInfo != null) {

            for (ArrayList<Renderable> renderableList : productRenderablesInfo.theRenderableListHash.values()) {
                for (Renderable renderable : renderableList) {
                    SystemUtils.LOG.info(" renderable " + renderable);
                    removeRenderable(renderable);
                }
                renderableList.clear();
            }

            /*
            for (ColorBarLegend legend : theColorBarLegendHash.values()) {
                removeRenderable(legend);
            }

            theColorBarLegendHash.clear();
            */
        }

        //wwd.redrawNow();
    }

    public void redrawColorBar(double minValue, double maxValue, String comp, WorldWindowGLCanvas wwd) {
        SystemUtils.LOG.info("redrawColorBar " + minValue + " " + maxValue + " " + comp + " " + theColorBarLegendHash.get(comp));

        String title = "";
        if (comp.equalsIgnoreCase("owi")) {
            title = "OWI Wind Speed";
        }
        else if (comp.equalsIgnoreCase("osw")) {
            title = "OSW Wave Height.";
        }
        else if (comp.equalsIgnoreCase("rvl")) {
            title = "RVL Rad. Vel.";
        }

        removeRenderable(theColorBarLegendHash.get(comp));

        createColorBarLegend(minValue, maxValue, title, comp);
        addRenderable(theColorBarLegendHash.get(comp));

        createColorGradient(minValue, maxValue, false, theProductRenderablesInfoHash.get(theColorBarLegendProduct), comp);
        wwd.redrawNow();
    }

    public JPanel getControlPanel(final WorldWindowGLCanvas wwd) {
        final JPanel controlLevel2Panel = new JPanel(new GridLayout(5, 1, 5, 5));

        final JRadioButton owiBtn = new JRadioButton("OWI");
        owiBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                theSelectedComp = "owi";
                setComponentVisible("owi", wwd);
            }
        });

        final JRadioButton oswBtn = new JRadioButton("OSW");
        oswBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                theSelectedComp = "osw";
                setComponentVisible("osw", wwd);
            }
        });

        final JRadioButton rvlBtn = new JRadioButton("RVL");
        rvlBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                theSelectedComp = "rvl";
                //System.out.println("rvl:");
                //setComponentVisible("owi", false, getWwd());
                //setComponentVisible("osw", false, getWwd());
                setComponentVisible("rvl", wwd);
            }
        });

        final ButtonGroup group = new ButtonGroup();
        group.add(owiBtn);
        group.add(oswBtn);
        group.add(rvlBtn);
        owiBtn.setSelected(true);

        theSelectedComp = "owi";

        final JPanel componentTypePanel = new JPanel(new GridLayout(1, 4, 5, 5));
        componentTypePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        componentTypePanel.add(new JLabel("Component:"));
        componentTypePanel.add(owiBtn);
        componentTypePanel.add(oswBtn);
        componentTypePanel.add(rvlBtn);
        controlLevel2Panel.add(componentTypePanel);

        final JPanel maxPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        maxPanel.add(new JLabel("Max OWI Wind Speed:"));

        final JSpinner maxSP = new JSpinner(new SpinnerNumberModel(10, 0, 10, 1));
        maxSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newValue = (Integer) ((JSpinner) e.getSource()).getValue();

                theOWILimitChanged = true;
            }
        });
        maxPanel.add(maxSP);
        controlLevel2Panel.add(maxPanel);

        final JPanel minPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        minPanel.add(new JLabel("Min OWI Wind Speed:"));

        final JSpinner minSP = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        minSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                System.out.println("new value " + ((JSpinner) e.getSource()).getValue());

                theOWILimitChanged = true;
            }
        });
        minPanel.add(minSP);
        controlLevel2Panel.add(minPanel);

        final JPanel maxRVLPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        maxRVLPanel.add(new JLabel("Max RVL Rad Vel.:"));

        final JSpinner maxRVLSP = new JSpinner(new SpinnerNumberModel(6, 0, 10, 1));
        maxRVLSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newValue = (Integer) ((JSpinner) e.getSource()).getValue();
                theRVLLimitChanged = true;
            }
        });
        maxRVLPanel.add(maxRVLSP);
        controlLevel2Panel.add(maxRVLPanel);

        final JButton updateButton = new JButton("Update");
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                if (theOWILimitChanged) {

                    //double minValue = ((Integer) minSP.getValue()) * 1.0e4;
                    //double maxValue = ((Integer) maxSP.getValue()) * 1.0e4;
                    double minValue = ((Integer) minSP.getValue());
                    double maxValue = ((Integer) maxSP.getValue());
                    redrawColorBar(minValue, maxValue, "owi", wwd);
                }

                if (theRVLLimitChanged) {
                    System.out.println("theRVLLimitChanged");

                    //double minValue = ((Integer) minSP.getValue()) * 1.0e4;
                    //double maxValue = ((Integer) maxSP.getValue()) * 1.0e4;

                    double maxValue = ((Integer) maxRVLSP.getValue());
                    double minValue = -1*maxValue;

                    redrawColorBar(minValue, maxValue, "rvl", wwd);
                }

                theOWILimitChanged = false;
                theRVLLimitChanged = false;
            }
        });
        controlLevel2Panel.add(updateButton);

        createColorBarLegend(0, 10, "OWI Wind Speed", "owi");
        createColorBarLegend(0, 10, "OSW Wave Height.", "osw");
        createColorBarLegend(-6, 6, "RVL Rad. Vel.", "rvl");
        addRenderable(theColorBarLegendHash.get("owi"));

        return controlLevel2Panel;
    }
}