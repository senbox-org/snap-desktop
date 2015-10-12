/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.statistics;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.CombinedFXYGeoCoding;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FXYGeoCoding;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.math.FXYSum;
import org.openide.windows.TopComponent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;

/**
 * @author Thomas Storm
 * @author Tonio Fincke
 */
class GeoCodingPanel extends PagePanel {

    private static final String DEFAULT_INFORMATION_TEXT = "No geo-coding information available."; /*I18N*/
    private static final String TITLE_PREFIX = "Geo-Coding";   /*I18N*/

    private GeoCoding geoCoding;
    private JPanel contentPanel;
    private TableLayout contentLayout;
    private int currentRow;
    private StringBuilder dataAsTextBuilder;

    public GeoCodingPanel(TopComponent topComponent, String helpId) {
        super(topComponent, helpId, TITLE_PREFIX);
    }

    @Override
    protected boolean mustHandleSelectionChange() {
        final RasterDataNode raster = getRaster();
        return super.mustHandleSelectionChange() || (raster != null && geoCoding != raster.getGeoCoding());
    }

    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        if (Product.PROPERTY_NAME_SCENE_GEO_CODING.equals(event.getPropertyName())) {
            if (event.getSourceNode() instanceof Product) {
                geoCoding = getProduct().getSceneGeoCoding();
            } else {
                geoCoding = getRaster().getGeoCoding();
            }
            updateComponents();
        }
    }

    @Override
    protected void initComponents() {
        contentPanel = new JPanel();
        resetContentPanel();
        final JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected void updateComponents() {
        if (isVisible()) {
            currentRow = 0;
            updateContent();
            if (geoCoding == null) {
                contentLayout.setColumnWeightX(0, 1.0);
                showNoInformationAvailableMessage();
            }
            updateUI();
        }
    }

    private void resetContentPanel() {
        contentPanel.removeAll();
        contentLayout = new TableLayout(6);
        contentLayout.setTablePadding(2, 2);
        contentLayout.setTableFill(TableLayout.Fill.BOTH);
        contentLayout.setColumnWeightX(0, 0.0);
        contentLayout.setTableWeightX(1.0);
        contentLayout.setTableWeightY(0.0);
        contentLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        contentPanel.setLayout(contentLayout);
    }

    private void showNoInformationAvailableMessage() {
        contentPanel.add(new JLabel(DEFAULT_INFORMATION_TEXT));
        contentPanel.add(contentLayout.createVerticalSpacer());
        dataAsTextBuilder.append(DEFAULT_INFORMATION_TEXT);
    }

    @Override
    protected String getDataAsText() {
        return dataAsTextBuilder.toString();
    }

    private void updateContent() {
        resetContentPanel();
        dataAsTextBuilder = new StringBuilder();
        final RasterDataNode raster = getRaster();
        final Product product = getProduct();

        boolean usingUniformGeoCodings = false;
        if (product != null) {
            usingUniformGeoCodings = product.isUsingSingleGeoCoding();
        }

        final PixelPos sceneCenter;
        final PixelPos sceneUL;
        final PixelPos sceneUR;
        final PixelPos sceneLL;
        final PixelPos sceneLR;
        final String nodeType;
        if (usingUniformGeoCodings) {
            nodeType = "product";
            geoCoding = product.getSceneGeoCoding();
            sceneCenter = new PixelPos(product.getSceneRasterWidth() / 2 + 0.5f,
                                       product.getSceneRasterHeight() / 2 + 0.5f);
            sceneUL = new PixelPos(0 + 0.5f, 0 + 0.5f);
            sceneUR = new PixelPos(product.getSceneRasterWidth() - 1 + 0.5f, 0 + 0.5f);
            sceneLL = new PixelPos(0 + 0.5f, product.getSceneRasterHeight() - 1 + 0.5f);
            sceneLR = new PixelPos(product.getSceneRasterWidth() - 1 + 0.5f,
                                   product.getSceneRasterHeight() - 1 + 0.5f);
        } else {
            if (raster == null) {
                return;
            }
            assert product != null;

            nodeType = "band";
            geoCoding = raster.getGeoCoding();
            sceneCenter = new PixelPos(raster.getSceneRasterWidth() / 2 + 0.5f,
                                       raster.getSceneRasterHeight() / 2 + 0.5f);
            sceneUL = new PixelPos(0 + 0.5f, 0 + 0.5f);
            sceneUR = new PixelPos(raster.getSceneRasterWidth() - 1 + 0.5f, 0 + 0.5f);
            sceneLL = new PixelPos(0 + 0.5f, product.getSceneRasterHeight() - 1 + 0.5f);
            sceneLR = new PixelPos(raster.getSceneRasterWidth() - 1 + 0.5f,
                                   raster.getSceneRasterHeight() - 1 + 0.5f);
        }
        writeGeoCoding(geoCoding, sceneCenter, sceneUL, sceneUR, sceneLL, sceneLR, nodeType);
    }

    private void writeGeoCoding(final GeoCoding geoCoding,
                                final PixelPos sceneCenter, final PixelPos sceneUpperLeft,
                                final PixelPos sceneUpperRight, final PixelPos sceneLowerLeft,
                                final PixelPos sceneLowerRight, final String nodeType) {
        if (geoCoding != null) {
            GeoPos gp = new GeoPos();

            gp = geoCoding.getGeoPos(sceneCenter, gp);

            addRow("Center latitude", gp.getLatString());
            addRow("Center longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneUpperLeft, gp);
            addRow("Upper left latitude", gp.getLatString());
            addRow("Upper left longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneUpperRight, gp);
            addRow("Upper right latitude", gp.getLatString());
            addRow("Upper right longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneLowerLeft, gp);
            addRow("Lower left latitude", gp.getLatString());
            addRow("Lower left longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneLowerRight, gp);
            addRow("Lower right latitude", gp.getLatString());
            addRow("Lower right longitude", gp.getLonString());

            addEmptyRow();

            addRowWithTextField("WKT of the image CRS", geoCoding.getImageCRS().toString());
            addRowWithTextField("WKT of the geographical CRS", geoCoding.getGeoCRS().toString());

            addEmptyRow();
        }

        if (geoCoding instanceof TiePointGeoCoding) {
            writeTiePointGeoCoding((TiePointGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof BasicPixelGeoCoding) {
            writePixelGeoCoding((BasicPixelGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof MapGeoCoding) {
            writeMapGeoCoding((MapGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof FXYGeoCoding) {
            writeFXYGeoCoding((FXYGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof CombinedFXYGeoCoding) {
            writeCombinedFXYGeoCoding((CombinedFXYGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof GcpGeoCoding) {
            writeGcpGeoCoding((GcpGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof CrsGeoCoding) {
            writeCrsGeoCoding((CrsGeoCoding) geoCoding, nodeType);
        } else if (geoCoding != null) {
            writeUnknownGeoCoding(geoCoding, nodeType);
        } else {
            addRow("The " + nodeType + " has no geo-coding information.");
        }
    }

    private void addHeaderRow(String content) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            b.append('=');
        }
        contentLayout.setCellColspan(currentRow++, 0, 6);
        contentPanel.add(getCorrectlyColouredLabel(b.toString()));
        contentLayout.setCellColspan(currentRow++, 0, 6);
        contentPanel.add(getCorrectlyColouredLabel(content));
        contentLayout.setCellColspan(currentRow++, 0, 6);
        contentPanel.add(getCorrectlyColouredLabel(b.toString()));
        dataAsTextBuilder.append(b.toString()).append("/n").append(content).append("/n").append(b.toString()).append("/n");
    }

    private void addRow(String content) {
        contentLayout.setCellColspan(currentRow++, 0, 6);
        contentPanel.add(getCorrectlyColouredLabel(content));
        dataAsTextBuilder.append(content).append("/n");
    }

    private void addRow(String name, String value) {
        contentLayout.setCellColspan(currentRow++, 1, 5);
        contentPanel.add(getCorrectlyColouredLabel(name));
        contentPanel.add(getCorrectlyColouredLabel(value));
        dataAsTextBuilder.append(name).append("/t").append(value).append("/n");
    }

    private void addRowWithTextField(String name, String value) {
        contentLayout.setCellColspan(currentRow++, 1, 5);
        contentPanel.add(getCorrectlyColouredLabel(name));
        final JTextArea textArea = new JTextArea(value);
        textArea.setBackground(getBackgroundColor());
        textArea.setEditable(false);
        contentPanel.add(textArea);
        dataAsTextBuilder.append(name).append("/t").append(value).append("/n");
    }

    private void addEmptyRow() {
        contentPanel.add(contentLayout.createVerticalSpacer());
        currentRow++;
        dataAsTextBuilder.append("/n");
    }

    private void addRow(String... values) {
        for (String value : values) {
            contentPanel.add(getCorrectlyColouredLabel(value));
            dataAsTextBuilder.append(value).append("/t");
        }
        currentRow++;
        dataAsTextBuilder.append("/n");
    }

    private JLabel getCorrectlyColouredLabel(String value) {
        final JLabel label = new JLabel(value);
        label.setBackground(getBackgroundColor());
        label.setOpaque(true);
        return label;
    }

    private Color getBackgroundColor() {
        final Color white = Color.WHITE;
        if (currentRow % 2 == 0) {
            return new Color((14 * white.getRed()) / 15,
                             (14 * white.getGreen()) / 15,
                             (14 * white.getBlue()) / 15);
        }
        return white;
    }

    private void writeGcpGeoCoding(GcpGeoCoding gcpGeoCoding, String nodeType) {
        addEmptyRow();
        addRow("The " + nodeType + " uses a geo-coding which is based on ground control points (GCPs).");
        addEmptyRow();

        ProductNodeGroup<Placemark> gcpGroup = getProduct().getGcpGroup();
        addRow("Number Of GCPs", String.valueOf(gcpGroup.getNodeCount()));
        addRow("Function", String.valueOf(gcpGeoCoding.getMethod()));
        addRow("Datum", String.valueOf(gcpGeoCoding.getDatum().getName()));
        addRow("Latitude RMSE", String.valueOf(gcpGeoCoding.getRmseLat()));
        addRow("Longitude RMSE", String.valueOf(gcpGeoCoding.getRmseLon()));
        addEmptyRow();

        addRow("Table of used GCPs");
        Placemark[] gcps = gcpGroup.toArray(new Placemark[0]);
        addRow("Number", "Label", "X", "Y", "Latitude", "Longitude");
        for (int i = 0; i < gcps.length; i++) {
            Placemark gcp = gcps[i];
            PixelPos pixelPos = gcp.getPixelPos();
            GeoPos geoPos = gcp.getGeoPos();
            addRow(String.valueOf(i), gcp.getLabel(),
                   String.valueOf(pixelPos.getX()), String.valueOf(pixelPos.getY()),
                   geoPos.getLatString(), geoPos.getLonString());
        }
//        setFirstColumnWidth(40);
    }

    private void writeCrsGeoCoding(CrsGeoCoding geoCoding, String nodeType) {
        addRow("The " + nodeType + " uses a geo-coding based on a cartographic map CRS.");
        addEmptyRow();
        addRow("WKT of the map CRS", geoCoding.getMapCRS().toString());
        addEmptyRow();
        addRow("Image-to-map transformation", geoCoding.getImageToMapTransform().toString());
    }

    private void writeUnknownGeoCoding(GeoCoding geoCoding, String nodeType) {
        addRow("The " + nodeType + " uses an unknown geo-coding implementation.");
        addRow("Class", geoCoding.getClass().getName());
        addRow("Instance", geoCoding.toString());
    }

    private void writeCombinedFXYGeoCoding(CombinedFXYGeoCoding combinedGeoCoding, String nodeType) {
        final CombinedFXYGeoCoding.CodingWrapper[] codingWrappers = combinedGeoCoding.getCodingWrappers();

        addEmptyRow();
        addRow("The " + nodeType + " uses a geo-coding which consists of multiple polynomial based geo-coding.");
        addEmptyRow();

        addRow("The geo-coding uses " + codingWrappers.length + " polynomial based geo-codings");

        for (int i = 0; i < codingWrappers.length; i++) {
            final CombinedFXYGeoCoding.CodingWrapper codingWrapper = codingWrappers[i];
            final Rectangle region = codingWrapper.getRegion();
            addHeaderRow("Geo-coding[" + (i + 1) + "]");
            addRow("The region in the scene which is covered by this geo-coding is defined by:");
            addRow("Location: X = " + region.x + ", Y = " + region.y + "\n");
            addRow("Dimension: W = " + region.width + ", H = " + region.height);
            addEmptyRow();

            final FXYGeoCoding fxyGeoCoding = codingWrapper.getGeoGoding();
            addRow("<html>Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)<br/>" +
                           "by using following polynomial equations</html>");
            addRow(fxyGeoCoding.getLatFunction().createCFunctionCode("latitude", "x", "y"));
            addRow(fxyGeoCoding.getLonFunction().createCFunctionCode("longitude", "x", "y"));
            addEmptyRow();

            addRow("<html>Pixels (x,y) are computed from geographic coordinates (lat,lon)<br/>" +
                           "by using the following polynomial equations</html>");
            addRow(fxyGeoCoding.getPixelXFunction().createCFunctionCode("x", "lat", "lon"));
            addRow(fxyGeoCoding.getPixelYFunction().createCFunctionCode("y", "lat", "lon"));
        }
    }

    private void writeFXYGeoCoding(FXYGeoCoding fxyGeoCoding, String nodeType) {
        addEmptyRow();
        addRow("The" + nodeType + " uses a polynomial based geo-coding.");
        addEmptyRow();

        addRow("<html>Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)<br/>" +
                       "by using following polynomial equations</html>");
        addRow(fxyGeoCoding.getLatFunction().createCFunctionCode("latitude", "x", "y"));
        addRow(fxyGeoCoding.getLonFunction().createCFunctionCode("longitude", "x", "y"));
        addEmptyRow();

        addRow("<html>Pixels (x,y) are computed from geographic coordinates (lat,lon)<br/>" +
                       "by using the following polynomial equations</html>");
        addRow(fxyGeoCoding.getPixelXFunction().createCFunctionCode("x", "lat", "lon"));
        addRow(fxyGeoCoding.getPixelYFunction().createCFunctionCode("y", "lat", "lon"));
    }

    private void writeMapGeoCoding(MapGeoCoding mgc, String nodeType) {
        final MapInfo mi = mgc.getMapInfo();

        addEmptyRow();
        addRow("The " + nodeType + " uses a map-projection based geo-coding.");
        addEmptyRow();

        addRow("Projection", mi.getMapProjection().getName());

        addRow("Projection parameters");
        final Parameter[] parameters = mi.getMapProjection().getMapTransform().getDescriptor().getParameters();
        final double[] parameterValues = mi.getMapProjection().getMapTransform().getParameterValues();
        for (int i = 0; i < parameters.length; i++) {
            addRow(parameters[i].getName(),
                   String.valueOf(parameterValues[i]) + " " + parameters[i].getProperties().getPhysicalUnit());

        }
        addEmptyRow();

        addRow("Map CRS Name", mgc.getMapCRS().getName().toString());
        addRow("Map CRS WKT");
        addRow(mgc.getMapCRS().toWKT());

        addEmptyRow();

        addRow("Output parameters");
        addRow("Datum", mi.getDatum().getName());
        addRow("Reference pixel X", String.valueOf(mi.getPixelX()));
        addRow("Reference pixel Y", String.valueOf(mi.getPixelY()));
        addRow("Orientation", String.valueOf(mi.getOrientation()) + " degree");

        String mapUnit = mi.getMapProjection().getMapUnit();
        addRow("Northing", String.valueOf(mi.getNorthing()) + " " + mapUnit);
        addRow("Easting", String.valueOf(mi.getEasting()) + " " + mapUnit);
        addRow("Pixel size X", String.valueOf(mi.getPixelSizeX()) + " " + mapUnit);
        addRow("Pixel size Y", String.valueOf(mi.getPixelSizeY()) + " " + mapUnit);
    }

    private void writePixelGeoCoding(BasicPixelGeoCoding gc, String nodeType) {
        addEmptyRow();
        addRow("The " + nodeType + " uses a pixel based geo-coding.");
        addEmptyRow();
        addRow("Name of latitude band", gc.getLatBand().getName());
        addRow("Name of longitude band", gc.getLonBand().getName());

        addRow("Search radius", gc.getSearchRadius() + " pixels");
        final String validMask = gc.getValidMask();
        addRow("Valid pixel mask", validMask != null ? validMask : "");
        addRow("Crossing 180 degree meridian", String.valueOf(gc.isCrossingMeridianAt180()));

        addEmptyRow();
        addRow("<html>Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)<br/>" +
                       "by linear interpolation between pixels.</html>");

        addEmptyRow();
        addRow("<html>Pixel coordinates (x,y) are computed from geographic coordinates (lat,lon)<br/>" +
                       "by a search algorithm.</html>");
        addEmptyRow();
    }

    private void writeTiePointGeoCoding(TiePointGeoCoding tgc, String nodeType) {
        addRow("The " + nodeType + " uses a tie-point based geo-coding.");
        addEmptyRow();
        addRow("Name of latitude tie-point grid", tgc.getLatGrid().getName());
        addRow("Name of longitude tie-point grid", tgc.getLonGrid().getName());
        addRow("Crossing 180 degree meridian", String.valueOf(tgc.isCrossingMeridianAt180()));
        addEmptyRow();
        addRow("<html>Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)<br/>" +
                       "by linear interpolation between tie points.</html>");

        final int numApproximations = tgc.getNumApproximations();
        if (numApproximations > 0) {
            addRow("<html>Pixel coordinates (x,y) are computed from geographic coordinates (lat,lon)<br/>" +
                           "by polynomial approximations for " + numApproximations + " tile(s).</html>");
            addEmptyRow();

            for (int i = 0; i < numApproximations; i++) {

                final TiePointGeoCoding.Approximation approximation = tgc.getApproximation(i);
                final FXYSum fX = approximation.getFX();
                final FXYSum fY = approximation.getFY();

                addHeaderRow("Approximation for tile " + (i + 1));
                addRow("Center latitude", String.valueOf(approximation.getCenterLat()) + " degree");
                addRow("Center longitude", String.valueOf(approximation.getCenterLon()) + " degree");
                addRow("RMSE for X", String.valueOf(fX.getRootMeanSquareError()) + " pixels");
                addRow("RMSE for Y", String.valueOf(fY.getRootMeanSquareError()) + " pixels");
                addRow("Max. error for X", String.valueOf(fX.getMaxError()) + " pixels");
                addRow("Max. error for Y", String.valueOf(fY.getMaxError()) + " pixels");
            }
        } else {
            addEmptyRow();
            addRow(
                    "<html>WARNING: Pixel coordinates (x,y) cannot be computed from geographic coordinates (lat,lon)<br/>" +
                            "because appropriate polynomial approximations could not be found.</html>");
        }
    }
}
