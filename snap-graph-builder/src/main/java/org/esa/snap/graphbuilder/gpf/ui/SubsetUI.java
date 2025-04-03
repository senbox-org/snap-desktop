/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.gpf.ui;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.ui.product.ProductSubsetByPolygonUiComponents;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.NestWorldMapPane;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

/**
 * User interface for Subset
 */
public class SubsetUI extends BaseOperatorUI {

    private static final String POLYGON_REGION_PARAMETER = "polygonRegion";
    private static final String VECTOR_FILE_PARAMETER = "vectorFile";

    private final JList bandList = new JList();

    private final JComboBox referenceCombo = new JComboBox();
    private final JTextField regionX = new JTextField("");
    private final JTextField regionY = new JTextField("");
    private final JTextField width = new JTextField("");
    private final JTextField height = new JTextField("");
    private final JSpinner subSamplingX = new JSpinner();
    private final JSpinner subSamplingY = new JSpinner();
    private final JCheckBox copyMetadata = new JCheckBox("Copy Metadata", true);

    private final JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private final JRadioButton geoCoordRadio = new JRadioButton("Geographic Coordinates");
    private final JRadioButton vectorFileRadio = new JRadioButton("Polygon");
    private final JPanel pixelPanel = new JPanel(new GridBagLayout());
    private final JPanel geoPanel = new JPanel(new BorderLayout());
    private final ProductSubsetByPolygonUiComponents productSubsetByPolygonUiComponents = new ProductSubsetByPolygonUiComponents(this.geoPanel);
    private final JPanel vectorFilePanel = productSubsetByPolygonUiComponents.getImportVectorFilePanel();

    private final WorldMapUI worldMapUI = new WorldMapUI();
    private final JTextField geoText = new JTextField("");
    private final JButton geoUpdateButton = new JButton("Update");
    private Geometry geoRegion = null;
    private Polygon polygonRegion = null;
    private File vectorFile = null;
    private static final int MIN_SUBSET_SIZE = 1;
    private Dimension currentProductSize = new Dimension(0, 0);

    private int getRasterReferenceWidth()
    {
        int w=1;
        if(sourceProducts!=null && referenceCombo.getSelectedItem()!=null) {
            if(sourceProducts[0].isMultiSize()) {
                w = sourceProducts[0].getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
            } else {
                w = sourceProducts[0].getSceneRasterWidth();
            }
        }
        return w;
    }
    private int getRasterReferenceHeight()
    {
        int h = 1;
        if(sourceProducts!=null && referenceCombo.getSelectedItem()!=null) {
            if(sourceProducts[0].isMultiSize()) {
                h = sourceProducts[0].getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            } else {
                h = sourceProducts[0].getSceneRasterHeight();
            }
        }
        return h;
    }

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = createPanel();

        initParameters();

        geoText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGeoRegion();
            }
        });
        geoUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGeoRegion();
            }
        });
        width.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateROIx();
            }
        });
        height.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateROIy();
            }
        });
        regionX.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateROIx();
            }
        });
        regionY.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateROIy();
            }
        });
        //enable or disable referenceCombo depending on sourceProduct
        referenceCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (referenceCombo.isEnabled()) {
                    updateParametersReferenceBand();
                }
            }
        });
        subSamplingX.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateParametersSubSamplingX();
            }
        });
        subSamplingY.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateParametersSubSamplingY();
            }
        });
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {
        OperatorUIUtils.initParamList(bandList, getBandNames(), (Object[])paramMap.get("sourceBands"));

        if(paramMap.get("copyMetadata") != null){
            copyMetadata.setSelected((Boolean)paramMap.get("copyMetadata"));
        }

        String _oldSelected = (String) referenceCombo.getSelectedItem();
        referenceCombo.removeAllItems();
        for (int i = 0 ; i < bandList.getModel().getSize() ; i++) {
            String string = (String) bandList.getModel().getElementAt(i);
            referenceCombo.addItem(string);
            if (string.equals(_oldSelected)) {
                referenceCombo.setSelectedItem(string);
            }
        }

        final Rectangle region = (Rectangle)paramMap.get("region");
        if(region != null) {
            regionX.setText(String.valueOf(region.x));
            regionY.setText(String.valueOf(region.y));
            width.setText(String.valueOf(region.width));
            height.setText(String.valueOf(region.height));
        }
        if (sourceProducts != null && sourceProducts.length > 0) {
            final int currentSceneRasterWidth = currentProductSize.width;
            final int currentSceneRasterHeight = currentProductSize.height;
            final int productSceneRasterWidth = sourceProducts[0].getSceneRasterWidth();
            final int productSceneRasterHeight = sourceProducts[0].getSceneRasterHeight();
            if (region == null || region.width == 0 || currentSceneRasterWidth != productSceneRasterWidth)
                width.setText(String.valueOf(productSceneRasterWidth));
            if (region == null || region.height == 0 || currentSceneRasterHeight != productSceneRasterHeight)
                height.setText(String.valueOf(productSceneRasterHeight));

            currentProductSize = sourceProducts[0].getSceneRasterSize();
            worldMapUI.getModel().setAutoZoomEnabled(true);
            worldMapUI.getModel().setProducts(sourceProducts);
            worldMapUI.getModel().setSelectedProduct(sourceProducts[0]);
            worldMapUI.getWorlMapPane().zoomToProduct(sourceProducts[0]);
        }

        Integer subSamplingXVal = (Integer) paramMap.get("subSamplingX");
        if(subSamplingXVal != null) {
            subSamplingX.setValue(subSamplingXVal);
        }
        Integer subSamplingYVal = (Integer) paramMap.get("subSamplingY");
        if(subSamplingYVal != null) {
            subSamplingY.setValue(subSamplingYVal);
        }
        geoRegion = (Geometry) paramMap.get("geoRegion");
        if (geoRegion != null) {

            final Coordinate coord[] = geoRegion.getCoordinates();
            worldMapUI.setSelectionStart((float) coord[0].y, (float) coord[0].x);
            worldMapUI.setSelectionEnd((float) coord[2].y, (float) coord[2].x);

            getGeoRegion();

            geoCoordRadio.setSelected(true);
            pixelPanel.setVisible(false);
            geoPanel.setVisible(true);
            vectorFilePanel.setVisible(false);
        }
        polygonRegion = (Polygon) paramMap.get(POLYGON_REGION_PARAMETER);
        vectorFile = (File) paramMap.get(VECTOR_FILE_PARAMETER);
        if (polygonRegion != null || vectorFile != null) {
            getPolygonRegion();
            vectorFileRadio.setSelected(true);
            pixelPanel.setVisible(false);
            geoPanel.setVisible(false);
            vectorFilePanel.setVisible(true);
        }
        //enable or disable referenceCombo depending on sourceProduct
        if(sourceProducts == null || sourceProducts.length == 0 || !sourceProducts[0].isMultiSize()) {
            referenceCombo.setEnabled(false);
        } else {
            referenceCombo.setEnabled(true);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        OperatorUIUtils.updateParamList(bandList, paramMap, "bandNames");
        paramMap.put("copyMetadata", copyMetadata.isSelected());

        paramMap.remove("referenceBand");
        if(sourceProducts != null ) {
            for (Product prod : sourceProducts) {
                if (prod.isMultiSize()) {
                    paramMap.put("referenceBand", (String) referenceCombo.getSelectedItem());
                    break;
                }
            }
        }
        int x=0, y=0, rasterReferenceWidth=1, rasterReferenceHeight=1,w=1,h=1;
        if(sourceProducts!=null && referenceCombo.getSelectedItem()!=null) {
            if(sourceProducts[0].isMultiSize()) {
                rasterReferenceWidth = sourceProducts[0].getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                rasterReferenceHeight = sourceProducts[0].getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            } else {
                rasterReferenceWidth = sourceProducts[0].getSceneRasterWidth();
                rasterReferenceHeight = sourceProducts[0].getSceneRasterHeight();
            }
        }
        Integer subSamplingXVal = (Integer) paramMap.get("subSamplingX");
        Integer subSamplingYVal = (Integer) paramMap.get("subSamplingY");

        SpinnerModel subSamplingXmodel = new SpinnerNumberModel(1,1, rasterReferenceWidth/MIN_SUBSET_SIZE+1,1);
        subSamplingX.setModel(subSamplingXmodel);
        SpinnerModel subSamplingYmodel = new SpinnerNumberModel(1,1, rasterReferenceHeight/MIN_SUBSET_SIZE+1,1);
        subSamplingY.setModel(subSamplingYmodel);
        if(subSamplingXVal != null) {
            subSamplingX.setValue(subSamplingXVal);
        }
        if(subSamplingYVal != null) {
            subSamplingY.setValue(subSamplingYVal);
        }
        final String subSamplingXStr = subSamplingX.getValue().toString();
        if (subSamplingXStr != null && !subSamplingXStr.isEmpty())
            paramMap.put("subSamplingX", Integer.parseInt(subSamplingXStr));
        final String subSamplingYStr = subSamplingY.getValue().toString();
        if (subSamplingYStr != null && !subSamplingYStr.isEmpty())
            paramMap.put("subSamplingY", Integer.parseInt(subSamplingYStr));

        final String regionXStr = regionX.getText();
        if (regionXStr != null && !regionXStr.isEmpty())
            x = Integer.parseInt(regionXStr);
        final String regionYStr = regionY.getText();
        if (regionYStr != null && !regionYStr.isEmpty())
            y = Integer.parseInt(regionYStr);
        final String widthStr = width.getText();
        if (widthStr != null && !widthStr.isEmpty())
            w = Integer.parseInt(widthStr);
        final String heightStr = height.getText();
        if (heightStr != null && !heightStr.isEmpty())
            h = Integer.parseInt(heightStr);

        paramMap.remove("geoRegion");
        paramMap.remove("region");
        paramMap.remove(POLYGON_REGION_PARAMETER);
        getGeoRegion();
        getPolygonRegion();
        if (geoCoordRadio.isSelected() && geoRegion != null) {
            paramMap.put("geoRegion", geoRegion);
        } else if (vectorFileRadio.isSelected() && polygonRegion != null) {
            paramMap.put(POLYGON_REGION_PARAMETER, polygonRegion);
        } else if(sourceProducts!=null) {
            paramMap.put("region", new Rectangle(x,y,w,h));
        }
    }

    public void updateROIorGeoRegion(int x, int y, int w, int h)
    {
        paramMap.remove("geoRegion");
        paramMap.remove("region");
        getGeoRegion();
        if (geoCoordRadio.isSelected() && geoRegion != null) {
            paramMap.put("geoRegion", geoRegion);
        } else {
            paramMap.put("region", new Rectangle(x,y,w,h));
        }
    }

    public void updateParametersSubSamplingX() {
        final String subSamplingXStr = subSamplingX.getValue().toString();
        if (subSamplingXStr != null && !subSamplingXStr.isEmpty()){
            paramMap.put("subSamplingX", Integer.parseInt(subSamplingXStr));
        }
    }
    public void updateParametersSubSamplingY() {
        final String subSamplingYStr = subSamplingY.getValue().toString();
        if (subSamplingYStr != null && !subSamplingYStr.isEmpty()){
            paramMap.put("subSamplingY", Integer.parseInt(subSamplingYStr));
        }
    }

    public void updateROIx() {
        int rasterRefWidth = getRasterReferenceWidth();
        int w = rasterRefWidth;
        int h = getRasterReferenceHeight();
        int x = 0, y=0;
        final String widthStr = width.getText();
        if (widthStr != null && !widthStr.isEmpty())
            w = Integer.parseInt(widthStr);
        final String regionXStr = regionX.getText();
        if (regionXStr != null && !regionXStr.isEmpty())
            x = Integer.parseInt(regionXStr);
        if (w < 1) {
            w = 1;
        }
        if (x > w - 2) {
            x = w - 2;
        }
        if(w+x>rasterRefWidth)
            w=rasterRefWidth-x;
        width.setText(String.valueOf(w));
        regionX.setText(String.valueOf(x));
        final String regionYStr = regionY.getText();
        if (regionYStr != null && !regionYStr.isEmpty())
            y = Integer.parseInt(regionYStr);
        final String heightStr = height.getText();
        if (heightStr != null && !heightStr.isEmpty())
            h = Integer.parseInt(heightStr);
        updateROIorGeoRegion(x,y,w,h);
    }

    public void updateROIy() {
        int rasterRefHeight = getRasterReferenceHeight();
        int h = rasterRefHeight;
        int w = getRasterReferenceWidth();
        int y = 0, x = 0;
        final String heightStr = height.getText();
        if (heightStr != null && !heightStr.isEmpty())
            h = Integer.parseInt(heightStr);
        final String regionYStr = regionY.getText();
        if (regionYStr != null && !regionYStr.isEmpty())
            y = Integer.parseInt(regionYStr);
        if (h < 1) {
            h = 1;
        }
        if (y > h - 2) {
            y = h - 2;
        }
        if(h+y>rasterRefHeight)
            h=rasterRefHeight-y;

        height.setText(String.valueOf(h));
        regionY.setText(String.valueOf(y));
        final String regionXStr = regionX.getText();
        if (regionXStr != null && !regionXStr.isEmpty())
            x = Integer.parseInt(regionXStr);
        final String widthStr = width.getText();
        if (widthStr != null && !widthStr.isEmpty())
            w = Integer.parseInt(widthStr);
        updateROIorGeoRegion(x,y,w,h);
    }


    public void updateParametersReferenceBand() {
        paramMap.remove("referenceBand");
        if(sourceProducts != null ) {
            for (Product prod : sourceProducts) {
                if (prod.isMultiSize()) {
                    paramMap.put("referenceBand", (String) referenceCombo.getSelectedItem());
                    break;
                }
            }
        }

        int x=0, y=0, w=1, h=1;
        if(sourceProducts!=null && referenceCombo.getSelectedItem()!=null) {
            if(sourceProducts[0].isMultiSize()) {
                w = sourceProducts[0].getBand((String) referenceCombo.getSelectedItem()).getRasterWidth();
                h = sourceProducts[0].getBand((String) referenceCombo.getSelectedItem()).getRasterHeight();
            } else {
                w = sourceProducts[0].getSceneRasterWidth();
                h = sourceProducts[0].getSceneRasterHeight();
            }
        }
        Integer subSamplingXVal = (Integer) paramMap.get("subSamplingX");
        Integer subSamplingYVal = (Integer) paramMap.get("subSamplingY");
        SpinnerModel subSamplingXmodel = new SpinnerNumberModel(1,1, w/MIN_SUBSET_SIZE+1,1);
        subSamplingX.setModel(subSamplingXmodel);
        SpinnerModel subSamplingYmodel = new SpinnerNumberModel(1,1, h/MIN_SUBSET_SIZE+1,1);
        subSamplingY.setModel(subSamplingYmodel);
        if(subSamplingXVal != null) {
            subSamplingX.setValue(subSamplingXVal);
        }
        if(subSamplingYVal != null) {
            subSamplingY.setValue(subSamplingYVal);
        }
        paramMap.remove("geoRegion");
        paramMap.remove("region");
        final String regionXStr = regionX.getText();
        if (regionXStr != null && !regionXStr.isEmpty())
            x = Integer.parseInt(regionXStr);
        final String regionYStr = regionY.getText();
        if (regionYStr != null && !regionYStr.isEmpty())
            y = Integer.parseInt(regionYStr);
        final String widthStr = width.getText();
        if (widthStr != null && !widthStr.isEmpty())
            w = Integer.parseInt(widthStr);
        final String heightStr = height.getText();
        if (heightStr != null && !heightStr.isEmpty())
            h = Integer.parseInt(heightStr);

        getGeoRegion();
        if (geoCoordRadio.isSelected() && geoRegion != null) {
            paramMap.put("geoRegion", geoRegion);
        } else {
            paramMap.put("region", new Rectangle(x,y,w,h));
        }

        final Rectangle region = (Rectangle)paramMap.get("region");
        if(region != null) {
            regionX.setText(String.valueOf(x));
            regionY.setText(String.valueOf(y));
            width.setText(String.valueOf(w));
            height.setText(String.valueOf(h));
        }
        if (sourceProducts != null && sourceProducts.length > 0) {
            worldMapUI.getModel().setAutoZoomEnabled(true);
            worldMapUI.getModel().setProducts(sourceProducts);
            worldMapUI.getModel().setSelectedProduct(sourceProducts[0]);
            worldMapUI.getWorlMapPane().zoomToProduct(sourceProducts[0]);
        }

        geoRegion = (Geometry) paramMap.get("geoRegion");
        if (geoRegion != null) {

            final Coordinate coord[] = geoRegion.getCoordinates();
            worldMapUI.setSelectionStart((float) coord[0].y, (float) coord[0].x);
            worldMapUI.setSelectionEnd((float) coord[2].y, (float) coord[2].x);

            getGeoRegion();

            geoCoordRadio.setSelected(true);
            pixelPanel.setVisible(false);
            geoPanel.setVisible(true);
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(copyMetadata, gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        final JPanel regionTypePanel = new JPanel(new GridLayout(1, 3));
        regionTypePanel.add(pixelCoordRadio);
        regionTypePanel.add(geoCoordRadio);
        regionTypePanel.add(vectorFileRadio);
        contentPane.add(regionTypePanel, gbc);
        gbc.gridwidth = 1;

        gbc.gridy++;
        gbc.gridx = 0;
        contentPane.add(new JLabel("Reference band:"), gbc);
        gbc.gridx = 1;
        contentPane.add(referenceCombo, gbc);

        pixelCoordRadio.setSelected(true);
        pixelCoordRadio.setActionCommand("pixelCoordRadio");
        geoCoordRadio.setActionCommand("geoCoordRadio");
        vectorFileRadio.setActionCommand("vectorFileRadio");
        ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        group.add(vectorFileRadio);
        RadioListener myListener = new RadioListener();
        pixelCoordRadio.addActionListener(myListener);
        geoCoordRadio.addActionListener(myListener);
        vectorFileRadio.addActionListener(myListener);

        final GridBagConstraints pixgbc = DialogUtils.createGridBagConstraints();
        pixgbc.gridwidth = 1;
        pixgbc.fill = GridBagConstraints.BOTH;
        addComponent(pixelPanel, pixgbc, "X:", regionX, 0);
        addComponent(pixelPanel, pixgbc, "Y:", regionY, 2);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Width:", width, 0);
        addComponent(pixelPanel, pixgbc, "height:", height, 2);
        pixgbc.gridy++;
        SpinnerModel subSamplingXmodel = new SpinnerNumberModel(1,1, 2,1);
        subSamplingX.setModel(subSamplingXmodel);
        SpinnerModel subSamplingYmodel = new SpinnerNumberModel(1,1, 2,1);
        subSamplingY.setModel(subSamplingYmodel);
        addComponent(pixelPanel, pixgbc, "Sub-sampling X:", subSamplingX, 0);
        addComponent(pixelPanel, pixgbc, "Sub-sampling Y:", subSamplingY, 2);
        pixelPanel.add(new JPanel(), pixgbc);

        final NestWorldMapPane worldPane = worldMapUI.getWorlMapPane();
        worldPane.setPreferredSize(new Dimension(500, 130));

        final JPanel geoTextPanel = new JPanel(new BorderLayout());
        geoText.setColumns(45);
        geoTextPanel.add(geoText, BorderLayout.CENTER);
        geoTextPanel.add(geoUpdateButton, BorderLayout.EAST);

        geoPanel.add(worldPane, BorderLayout.CENTER);
        geoPanel.add(geoTextPanel, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        contentPane.add(pixelPanel, gbc);
        geoPanel.setVisible(false);
        contentPane.add(geoPanel, gbc);
        vectorFilePanel.setVisible(false);
        contentPane.add(vectorFilePanel, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    public static JLabel addComponent(JPanel contentPane, GridBagConstraints gbc, String text, JComponent component, int pos) {
        gbc.gridx = pos;
        gbc.weightx = 0.5;
        final JLabel label = new JLabel(text);
        contentPane.add(label, gbc);
        gbc.gridx = pos+1;
        gbc.weightx = 2.0;
        contentPane.add(component, gbc);
        gbc.gridx = pos;
        gbc.weightx = 1.0;
        return label;
    }

    private class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().contains("pixelCoordRadio")) {
                pixelPanel.setVisible(true);
                geoPanel.setVisible(false);
                vectorFilePanel.setVisible(false);

                //reset geoRegion
                geoRegion = null;
                paramMap.put("geoRegion", geoRegion);
            } else if (e.getActionCommand().contains("geoCoordRadio")){
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
                vectorFilePanel.setVisible(false);
            } else {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(false);
                vectorFilePanel.setVisible(true);

                //reset geoRegion
                geoRegion = null;
                paramMap.put("geoRegion", geoRegion);
            }
        }
    }

    private void getGeoRegion() {
        geoRegion = null;
        geoText.setText("");
        if (geoCoordRadio.isSelected()) {
            final GeoPos[] selectionBox = worldMapUI.getSelectionBox();
            if (selectionBox != null) {
                final Coordinate[] coords = new Coordinate[selectionBox.length + 1];
                for (int i = 0; i < selectionBox.length; ++i) {
                    coords[i] = new Coordinate(selectionBox[i].getLon(), selectionBox[i].getLat());
                }
                coords[selectionBox.length] = new Coordinate(selectionBox[0].getLon(), selectionBox[0].getLat());

                final GeometryFactory geometryFactory = new GeometryFactory();
                final LinearRing linearRing = geometryFactory.createLinearRing(coords);

                geoRegion = geometryFactory.createPolygon(linearRing, null);
                geoText.setText(geoRegion.toText());
            }
        }
    }

    private void updateGeoRegion() {
        try {
            geoRegion = new WKTReader().read(geoText.getText());

            final Coordinate coord[] = geoRegion.getCoordinates();
            worldMapUI.setSelectionStart((float) coord[0].y, (float) coord[0].x);
            worldMapUI.setSelectionEnd((float) coord[2].y, (float) coord[2].x);
            worldMapUI.getWorlMapPane().revalidate();
            worldMapUI.getWorlMapPane().getLayerCanvas().updateUI();
        } catch (Exception e) {
            SnapApp.getDefault().handleError("UpdateGeoRegion error reading wkt", e);
        }
    }

    private void getPolygonRegion() {
        if (productSubsetByPolygonUiComponents.getProductSubsetByPolygon().isLoaded()) {
            this.polygonRegion = productSubsetByPolygonUiComponents.getProductSubsetByPolygon().getSubsetGeoPolygon();
        } else {
            if (sourceProducts != null && sourceProducts.length > 0) {
                final MetadataInspector.Metadata productMetadata = new MetadataInspector.Metadata(sourceProducts[0].getSceneRasterWidth(), sourceProducts[0].getSceneRasterHeight());
                productMetadata.setGeoCoding(sourceProducts[0].getSceneGeoCoding());
                productSubsetByPolygonUiComponents.setTargetProductMetadata(productMetadata);
                if (polygonRegion != null) {
                    productSubsetByPolygonUiComponents.importGeometryFromWKTString(polygonRegion.toText(), productMetadata);
                } else if (vectorFile != null) {
                    productSubsetByPolygonUiComponents.importGeometryFromVectorFile(vectorFile, productMetadata);
                }
            }
        }
    }
}
