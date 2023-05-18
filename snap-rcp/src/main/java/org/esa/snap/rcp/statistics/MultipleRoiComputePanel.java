/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.jidesoft.swing.CheckBoxList;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Zuehlkere
 */
class MultipleRoiComputePanel extends JPanel {
    private DefaultListModel<String> regionMaskNameListModel;
    private DefaultListModel<String> qualityMaskNameListModel;
    private DefaultListModel<String> bandNameListModel;
    private JTextField regionMaskNameSearchField;
    private JTextField qualityMaskNameSearchField;
    private JTextField bandNameSearchField;
    private int[] indexesInRegionMaskNameList;
    private int[] indexesInQualityMaskNameList;
    private int[] indexesInBandNameList;
    private JScrollPane bandNameScrollPane;
    private JScrollPane regionNameScrollPane;
    private  JScrollPane qualityMaskNameScrollPane;

    private JPanel maskQualityNameListPane;
    private int SCROLL_ROWS_MINIMUM = 4;

    private boolean qualityMaskSelectAllCheckBoxCanFire = true;

    //Careful this needs to sync up with MaskGroupingToolTips
    public enum MaskGrouping {
        COMPLEMENT("COMPLEMENT"),
        INTERSECTION("INTERSECTION"),
        UNION("UNION"),
        INDIVIDUAL("INDIVIDUAL");

        private MaskGrouping(String name) {
            this.name = name;
        }

        private final String name;

        public String toString() {
            return name;
        }
    }

    //Careful this needs to sync up with MaskGrouping
    public enum MaskGroupingToolTips {
        COMPLEMENT("<html>Complement of the selected masks<br>For example: !Mask1 && !Mask2</html>"),
        INTERSECTION("<html>Intersection of the selected masks<br>For example: Mask1 && Mask2</html>"),
        UNION("<html>Union of the selected masks<br>For example: Mask1 || Mask2</html>"),
        INDIVIDUAL("<html>Ungrouped.  Display each mask separately</html>");

        private MaskGroupingToolTips(String name) {
            this.name = name;
        }

        private final String name;

        public String toString() {
            return name;
        }
    }

    private ComputeMasks method;

    interface ComputeMasks {

        void compute(Mask[] selectedMasks, Mask[] selectedQualityMasks, Band[] selectedBands);
    }

    private final ProductNodeListener productNodeListener;
    private final ProductNodeListener productBandNodeListener;

    private AbstractButton refreshButton;
    private JCheckBox useRoiCheckBox;

    private JCheckBox includeFullSceneCheckBox;
    private JCheckBox includeNoQualityCheckBox;

    private CheckBoxList regionMaskNameList;
    private CheckBoxList qualityMaskNameList;
    private CheckBoxList bandNameList;

    private JCheckBox regionMaskSelectAllCheckBox;
    private JCheckBox qualityMaskSelectAllCheckBox;
    private JCheckBox bandNameselectAllCheckBox;


    private JComboBox qualityMaskGroupingComboBox;
    private JComboBox regionalMaskGroupingComboBox;
    private JLabel regionalMaskGroupingComboBoxLabel = new JLabel("Mask Grouping");
    private JLabel qualityMaskGroupingComboBoxLabel = new JLabel("Mask Grouping");

    private JTextField qualityGroupMaskNameTextfield = new JTextField("Stx_Quality_Mask");
    private JTextField regionalGroupMaskNameTextfield = new JTextField("Stx_Regional_Mask");

    private JPanel qualityGroupMaskNamePanel;
    private JPanel regionalGroupMaskNamePanel;

    private boolean validFields = true;

    private boolean includeFullScene = true;
    private boolean includeNoQuality = true;
    private MaskGrouping qualityMaskGrouping = MaskGrouping.INDIVIDUAL;

    private boolean isRunning = false;

    private RasterDataNode raster;
    private Product product;
    private boolean useViewBandRaster = true;

    private JPanel criteriaPanel = GridBagUtils.createPanel();;

    // This is complicated by creation of a MathBand (bands have been added since initialization)
    // in which case reset is needed so for now setting this to true which is probably better anyway
    // this forces a reset when another band view window is opened
    public boolean forceUpdate = true;


    MultipleRoiComputePanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {
        this.method = method;

        setLayout(new GridBagLayout());
        JPanel topPane = getTopPanel(method, rasterDataNode);
        topPane.setVisible(false);

        productNodeListener = new PNL();
        productBandNodeListener = new BandNamePNL();

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(topPane, gbc);

        gbc = GridBagUtils.restoreConstraints(gbc);
        includeFullSceneCheckBox = new JCheckBox("Include Unmasked (Full Scene)");
        includeFullSceneCheckBox.setSelected(includeFullScene);
        includeFullSceneCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                includeFullScene = includeFullSceneCheckBox.isSelected();
            }
        });
        includeFullSceneCheckBox.setMinimumSize(includeFullSceneCheckBox.getPreferredSize());
        includeFullSceneCheckBox.setPreferredSize(includeFullSceneCheckBox.getPreferredSize());

        gbc.insets.top = 5;
//        gbc.gridy += 1;
//        panel.add(includeFullSceneCheckBox, gbc);

        gbc.gridy = 0;
        panel.add(getMaskAndBandTabbedPane(), gbc);

        GridBagConstraints gbcMain = GridBagUtils.createConstraints();
        gbcMain.fill = GridBagConstraints.HORIZONTAL;
        add(panel, gbcMain);

        setRaster(rasterDataNode);
    }

    private void getQualityComboBox(){
//        final String[] values = new String[MaskGrouping.values().length];
//        ArrayList<String> toolTips = new ArrayList<String>();
//
//        Iterator itr = validValues.iterator();
//        int i = 0;
//        ParamValidValueInfo paramValidValueInfo;
//        while (itr.hasNext()) {
//            paramValidValueInfo = (ParamValidValueInfo) itr.next();
//            values[i] = paramValidValueInfo.getValue();
//            toolTips.add(paramValidValueInfo.getDescription());
//            i++;
//        }

        qualityMaskGroupingComboBox = new JComboBox(MaskGrouping.values());
    }



    private JTabbedPane getMaskAndBandTabbedPane() {

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Bands", getBandsPanel());
        tabbedPane.setToolTipTextAt(0, "<html>Select bands on which to create statistics<br>By default current band is selected</html>");

        tabbedPane.addTab("Regional", getMaskROIPanel());
        tabbedPane.setToolTipTextAt(1, "Select regional masks for which to create statistics");

        tabbedPane.addTab("Quality", getQualityMaskPanel());
        tabbedPane.setToolTipTextAt(2, "Select quality masks for which to create statistics");

        tabbedPane.addTab("Criteria", criteriaPanel);
        tabbedPane.setToolTipTextAt(3, "Specify statistical criteria and formatting");

        int width = (int) (tabbedPane.getPreferredSize().width * 1.2);
        Dimension preferredSize = new Dimension(width, tabbedPane.getPreferredSize().height);
        tabbedPane.setPreferredSize(preferredSize);
        tabbedPane.setMinimumSize(preferredSize);
        return tabbedPane;

    }


    private JPanel getRegionalMaskGroupingPanel() {

        String maskGroupingToolTip = "<html>Specify how to logically group selected masks<br>NONE results in individual mask statistics</html>";

//        JLabel jLabel = new JLabel("Mask Grouping");

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.insets.right = 3;
        panel.add(regionalMaskGroupingComboBoxLabel, gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        regionalMaskGroupingComboBox.setSelectedItem(MaskGrouping.INTERSECTION);
        regionalMaskGroupingComboBox.setMinimumSize(regionalMaskGroupingComboBox.getPreferredSize());
        regionalMaskGroupingComboBox.setSelectedItem(MaskGrouping.INDIVIDUAL);
        panel.add(regionalMaskGroupingComboBox, gbc);

        regionalMaskGroupingComboBoxLabel.setToolTipText(maskGroupingToolTip);
        regionalMaskGroupingComboBox.setToolTipText(maskGroupingToolTip);

        return panel;
    }


    private JPanel getQualityMaskGroupingPanel() {

        String maskGroupingToolTip = "<html>Specify how to logically group selected masks<br>NONE results in individual mask statistics</html>";

//        JLabel jLabel = new JLabel("Mask Grouping");

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.insets.right = 3;
        panel.add(qualityMaskGroupingComboBoxLabel, gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        qualityMaskGroupingComboBox.setSelectedItem(MaskGrouping.INTERSECTION);
        qualityMaskGroupingComboBox.setMinimumSize(qualityMaskGroupingComboBox.getPreferredSize());
        qualityMaskGroupingComboBox.setSelectedItem(MaskGrouping.INDIVIDUAL);
        panel.add(qualityMaskGroupingComboBox, gbc);

        qualityMaskGroupingComboBoxLabel.setToolTipText(maskGroupingToolTip);
        qualityMaskGroupingComboBox.setToolTipText(maskGroupingToolTip);

        return panel;
    }

    private JPanel getQualityMaskPanel() {
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        includeNoQualityCheckBox = new JCheckBox("Include Unmasked");
        includeNoQualityCheckBox.setSelected(includeFullScene);
        includeNoQualityCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                includeNoQuality = includeNoQualityCheckBox.isSelected();
            }
        });
        includeNoQualityCheckBox.setMinimumSize(includeNoQualityCheckBox.getPreferredSize());
        includeNoQualityCheckBox.setPreferredSize(includeNoQualityCheckBox.getPreferredSize());


        JPanel maskFilterPane = getQualityFilterPanel();
        maskQualityNameListPane = getQualityNameListPanel();
        JPanel checkBoxPane = getQualitySelectAllNonePanel();



        qualityMaskGroupingComboBox = new JComboBox(MaskGrouping.values());

        final MyComboBoxRenderer myComboBoxRenderer = new MyComboBoxRenderer();
        myComboBoxRenderer.setTooltipList(MaskGroupingToolTips.values());

        qualityMaskGroupingComboBox.setRenderer(myComboBoxRenderer);
        qualityMaskGroupingComboBox.setEditable(false);


        gbc.weighty = 0;
        gbc.insets.top = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(includeNoQualityCheckBox, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(maskFilterPane, gbc);

        gbc.insets.top = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(maskQualityNameListPane, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(checkBoxPane, gbc);
        gbc.anchor = GridBagConstraints.WEST;


        gbc.insets.top = 10;
        gbc.gridy++;
        panel.add(getQualityMaskGroupingPanel(), gbc);
        gbc.insets.top = 0;

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        qualityGroupMaskNamePanel = getTextfieldPanel("Grouped Mask", qualityGroupMaskNameTextfield);
        panel.add(qualityGroupMaskNamePanel, gbc);




        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);



        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());



        qualityGroupMaskNamePanel.setVisible(true);
        qualityMaskGroupingComboBox.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                if (qualityMaskGroupingComboBox.getSelectedItem() == MaskGrouping.INDIVIDUAL) {
                    qualityGroupMaskNamePanel.setVisible(false);
                } else {
                    qualityGroupMaskNamePanel.setVisible(true);
                }
            }
        });
        qualityMaskGroupingComboBox.setSelectedItem(MaskGrouping.INDIVIDUAL);
        selectAndEnableQualityCheckBoxes();


        return panel;
    }


    private JPanel getMaskROIPanel() {
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        //  useRoiCheckBox = new JCheckBox("Use ROI mask(s):");
        useRoiCheckBox = new JCheckBox("Mask(ROI)");
        useRoiCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        });
        useRoiCheckBox.setMinimumSize(useRoiCheckBox.getPreferredSize());
        useRoiCheckBox.setPreferredSize(useRoiCheckBox.getPreferredSize());
        useRoiCheckBox.setSelected(true);

        JPanel maskFilterPane = getRegionMaskFilterPanel();
        JPanel maskNameListPane = getRegionMaskNameListPanel();
        JPanel checkBoxPane = getRegionSelectAllNonePanel();

        regionalMaskGroupingComboBox = new JComboBox(MaskGrouping.values());


        final MyComboBoxRenderer myComboBoxRenderer = new MyComboBoxRenderer();
        myComboBoxRenderer.setTooltipList(MaskGroupingToolTips.values());

        regionalMaskGroupingComboBox.setRenderer(myComboBoxRenderer);
        regionalMaskGroupingComboBox.setEditable(false);


        gbc.weighty = 0;
        gbc.insets.top = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(includeFullSceneCheckBox, gbc);

        gbc.gridy += 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(maskFilterPane, gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;
        panel.add(maskNameListPane, gbc);

        gbc.gridy += 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(checkBoxPane, gbc);
        gbc.anchor = GridBagConstraints.WEST;


        gbc.insets.top = 10;
        gbc.gridy += 1;
        panel.add(getRegionalMaskGroupingPanel(), gbc);
        gbc.insets.top = 0;

        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.NONE;

        regionalGroupMaskNamePanel = getTextfieldPanel("Grouped Mask", regionalGroupMaskNameTextfield);
        panel.add(regionalGroupMaskNamePanel, gbc);

        regionalGroupMaskNamePanel.setVisible(true);


        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        regionalMaskGroupingComboBox.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                if (regionalMaskGroupingComboBox.getSelectedItem() == MaskGrouping.INDIVIDUAL) {
                    regionalGroupMaskNamePanel.setVisible(false);
                } else {
                    regionalGroupMaskNamePanel.setVisible(true);
                }
            }
        });
        regionalMaskGroupingComboBox.setSelectedItem(MaskGrouping.INDIVIDUAL);
        selectAndEnableRegionCheckBoxes();

        return panel;
    }


    private JPanel getTextfieldPanel(String label, JTextField textfield) {

        JLabel jLabel = new JLabel(label);

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(jLabel, gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(textfield, gbc);

        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        return panel;
    }


    public JPanel getCriteriaPanel() {
        return criteriaPanel;
    }

    private JPanel getBandsPanel() {


        JPanel bandFilterPane = getBandFilterPanel();
        JPanel bandNameListPane = getBandNameListPanel();
        JPanel checkBoxPane = getBandNameSelectAllNonePanel();

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 0;
        gbc.insets.top = 10;
        panel.add(bandFilterPane, gbc);
        gbc.insets.top = 0;

        gbc.gridy++;
        panel.add(bandNameListPane, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(checkBoxPane, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);


        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        return panel;
    }

    private AbstractButton createShowMaskManagerButton() {
        final AbstractButton showMaskManagerButton =
                ToolButtonFactory.createButton(ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/MaskManager24.png", false), false);
        showMaskManagerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final TopComponent maskManagerTopComponent = WindowManager.getDefault().findTopComponent("MaskManagerTopComponent");
                        maskManagerTopComponent.open();
                        maskManagerTopComponent.requestActive();
                    }
                });
            }
        });
        return showMaskManagerButton;
    }

    void setRaster(final RasterDataNode newRaster) {
        if (!isRunning()) {
            if (this.raster != newRaster) {
                this.raster = newRaster;
                if (newRaster == null) {
                    if (product != null) {
                        product.removeProductNodeListener(productNodeListener);
                    }
                    product = null;
                } else if (product != newRaster.getProduct()) {
                    clearMaskSelections();
                    if (product != null) {
                        product.removeProductNodeListener(productNodeListener);

                    }
                    product = newRaster.getProduct();
                    if (product != null) {
                        product.addProductNodeListener(productNodeListener);
                    }
                }
                resetRegionMaskListState(false);
                resetQualityMaskListState(false);
                resetBandNameListState();
                refreshButton.setEnabled(raster != null);
            }
        }
    }

    public void reset() {
        resetRegionMaskListState(true);
        resetQualityMaskListState(true);
        resetBandNameListState();
    }

    private void resetRegionMaskListState(boolean hardReset) {
        regionMaskNameListModel = new DefaultListModel<>();
        final String[] currentSelectedMaskNames = getSelectedRegionMaskNames();
        if (product != null && raster != null) {
            //todo [multisize_products] compare scenerastertransform (or its successor) rather than size (tf)
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
//            for (int i = 0; i < maskGroup.getNodeCount(); i++) {
//                final Mask mask = maskGroup.get(i);
//                if (mask.getRasterSize().equals(raster.getRasterSize())) {
//                    regionMaskNameListModel.addElement(mask.getName());
//                }
//            }
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (Mask mask : masks) {
                if (mask != null && mask.getName() != null) {

                    String imageTypeName = null;
                    if (mask.getImageType() != null && mask.getImageType().getName() != null) {
                        imageTypeName = mask.getImageType().getName();
                    }

                    if ("Geometry".equals(imageTypeName) ||
                            mask.getName().toLowerCase().contains("region") ||
                            mask.getName().toLowerCase().contains("bathymetry") ||
                            mask.getName().toLowerCase().contains("topography") ||
                            mask.getName().toLowerCase().contains("elev") ||
                            mask.getName().toLowerCase().contains("land") ||
                            mask.getName().toLowerCase().contains("water")) {
                        regionMaskNameListModel.addElement(mask.getName());
                    }
                }
            }
            regionMaskNameList.setModel(regionMaskNameListModel);
        }
        final String[] allNames = StringUtils.toStringArray(regionMaskNameListModel.toArray());
        indexesInRegionMaskNameList = new int[allNames.length];
        for (int i = 0; i < allNames.length; i++) {
            String name = allNames[i];
            if (!hardReset) {
                if (StringUtils.contains(currentSelectedMaskNames, name)) {
                    regionMaskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
                }
            }
            indexesInRegionMaskNameList[i] = i;
        }



        int numLines = allNames.length;
        int maxLines = getScrollListMaxLines();
        int prefLines = (numLines < maxLines) ? numLines : maxLines;
        if (prefLines < SCROLL_ROWS_MINIMUM) {
            prefLines = SCROLL_ROWS_MINIMUM;
        }

        int minLines = (prefLines < SCROLL_ROWS_MINIMUM) ? prefLines : SCROLL_ROWS_MINIMUM;

        regionMaskNameList.setVisibleRowCount(minLines);
        regionNameScrollPane.setMinimumSize(regionNameScrollPane.getPreferredSize());
        regionMaskNameList.setVisibleRowCount(prefLines);
        regionNameScrollPane.setPreferredSize(regionNameScrollPane.getPreferredSize());
        regionNameScrollPane.setMaximumSize(regionNameScrollPane.getPreferredSize());


        updateEnablement();
    }

    private void resetQualityMaskListState(boolean hardReset) {
        qualityMaskNameListModel = new DefaultListModel<>();
        final String[] currentSelectedMaskNames = getSelectedQualityMaskNames();
        if (product != null && raster != null) {
            //todo [multisize_products] compare scenerastertransform (or its successor) rather than size (tf)
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
//            for (int i = 0; i < maskGroup.getNodeCount(); i++) {
//                final Mask mask = maskGroup.get(i);
//                if (mask.getRasterSize().equals(raster.getRasterSize())) {
//                    qualityMaskNameListModel.addElement(mask.getName());
//                }
//            }
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (Mask mask : masks) {
                if (mask != null && mask.getName() != null) {

                    String imageTypeName = null;
                    if (mask.getImageType() != null && mask.getImageType().getName() != null) {
                        imageTypeName = mask.getImageType().getName();
                    }

                    if (!"Geometry".equals(imageTypeName)) {
                        qualityMaskNameListModel.addElement(mask.getName());
                    }
                }
            }
            qualityMaskNameList.setModel(qualityMaskNameListModel);
        }
        final String[] allNames = StringUtils.toStringArray(qualityMaskNameListModel.toArray());
        indexesInQualityMaskNameList = new int[allNames.length];
        for (int i = 0; i < allNames.length; i++) {
            String name = allNames[i];
            if (!hardReset) {
                if (StringUtils.contains(currentSelectedMaskNames, name)) {
                    qualityMaskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
                }
            }
            indexesInQualityMaskNameList[i] = i;
        }
        updateEnablement();


        int numLines = allNames.length;
        int maxLines = getScrollListMaxLines();
        int prefLines = (numLines < maxLines) ? numLines : maxLines;
        if (prefLines < SCROLL_ROWS_MINIMUM) {
            prefLines = SCROLL_ROWS_MINIMUM;
        }

        int minLines = (prefLines < SCROLL_ROWS_MINIMUM) ? prefLines : SCROLL_ROWS_MINIMUM;

        qualityMaskNameList.setVisibleRowCount(minLines);
        qualityMaskNameScrollPane.setMinimumSize(qualityMaskNameScrollPane.getPreferredSize());
        qualityMaskNameList.setVisibleRowCount(prefLines);
        qualityMaskNameScrollPane.setPreferredSize(qualityMaskNameScrollPane.getPreferredSize());
        qualityMaskNameScrollPane.setMaximumSize(qualityMaskNameScrollPane.getPreferredSize());

//        qualityMaskNameScrollPane.repaint();
//        maskQualityNameListPane.repaint();
    }

    private void resetBandNameListState() {
        bandNameListModel = new DefaultListModel<>();
        final String[] currentSelectedBandNames = getSelectedBandNames();
        if (product != null && raster != null) {
            //todo [multisize_products] compare scenerastertransform (or its successor) rather than size (tf)
            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
            for (int i = 0; i < bandGroup.getNodeCount(); i++) {
                final Band band = bandGroup.get(i);
                if (band.getRasterSize().equals(raster.getRasterSize())) {
                    bandNameListModel.addElement(band.getName());
                }
            }
            bandNameList.setModel(bandNameListModel);
        }
        final String[] allNames = StringUtils.toStringArray(bandNameListModel.toArray());
        indexesInBandNameList = new int[allNames.length];
        for (int i = 0; i < allNames.length; i++) {
            String name = allNames[i];
            // The commented out line would retain selected bands
//                if (StringUtils.contains(currentSelectedBandNames, name)) {

            if (name != null && name.equals(raster.getName())) {
                bandNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
            }
            indexesInBandNameList[i] = i;
        }


        int numLines = allNames.length;
        int maxLines = getScrollListMaxLines();
        int prefLines = (numLines < maxLines) ? numLines : maxLines;
        if (prefLines < SCROLL_ROWS_MINIMUM) {
            prefLines = SCROLL_ROWS_MINIMUM;
        }

        int minLines = (prefLines < SCROLL_ROWS_MINIMUM) ? prefLines : SCROLL_ROWS_MINIMUM;

        bandNameList.setVisibleRowCount(minLines);
        bandNameScrollPane.setMinimumSize(bandNameScrollPane.getPreferredSize());
        bandNameList.setVisibleRowCount(prefLines);
        bandNameScrollPane.setPreferredSize(bandNameScrollPane.getPreferredSize());
        bandNameScrollPane.setMaximumSize(bandNameScrollPane.getPreferredSize());


        updateEnablement();
    }

    void updateEnablement() {
        if (!isRunning()) {

            boolean hasMasks = (product != null && product.getMaskGroup().getNodeCount() > 0);
            boolean canSelectMasks = hasMasks && useRoiCheckBox.isSelected();
            useRoiCheckBox.setEnabled(hasMasks);
            regionMaskNameSearchField.setEnabled(canSelectMasks);
            regionMaskNameList.setEnabled(canSelectMasks);
//            regionMaskSelectAllCheckBox.setEnabled(canSelectMasks && regionMaskNameList.getCheckBoxListSelectedIndices().length < regionMaskNameList.getModel().getSize());

            qualityMaskNameSearchField.setEnabled(canSelectMasks);
            qualityMaskNameList.setEnabled(canSelectMasks);
//            qualityMaskSelectAllCheckBox.setEnabled(canSelectMasks && qualityMaskNameList.getCheckBoxListSelectedIndices().length < qualityMaskNameList.getModel().getSize());


            refreshButton.setEnabled(raster != null);
        }
    }

    void updateRunButton(boolean enabled) {
        refreshButton.setEnabled(enabled);
        //    refreshButton.setEnabled(validFields && raster != null && (useRoiCheckBox.isSelected() || includeUnmaskedCheckBox.isSelected()));
    }

//    private void updateMaskListState() {
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                final String text = maskNameSearchField.getText();
//                final DefaultListModel<Object> updatedListModel = new DefaultListModel<>();
//                List<Boolean> selected = new ArrayList<>();
//                int[] newIndexesInMaskNameList = new int[maskNameListModel.getSize()];
//                int counter = 0;
//                for (int i = 0; i < maskNameListModel.getSize(); i++) {
//                    if (maskNameListModel.get(i).toLowerCase().contains(text.toLowerCase())) {
//                        updatedListModel.addElement(maskNameListModel.get(i));
//                        if (indexesInMaskNameList[i] >= 0) {
//                            selected.add(maskNameList.getCheckBoxListSelectionModel().isSelectedIndex(indexesInMaskNameList[i]));
//                        } else {
//                            selected.add(false);
//                        }
//                        newIndexesInMaskNameList[i] = counter++;
//                    } else {
//                        newIndexesInMaskNameList[i] = -1;
//                    }
//                }
//                indexesInMaskNameList = newIndexesInMaskNameList;
//                maskNameList.setModel(updatedListModel);
//                for (int i = 0; i < selected.size(); i++) {
//                    if (selected.get(i)) {
//                        maskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
//                    }
//                }
//            }
//        });
//    }

    private class PNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
//            handleEvent(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            if (!isRunning()) {
                if (!useRoiCheckBox.isSelected()) {
                    return;
                }
                final ProductNode sourceNode = event.getSourceNode();
                if (!(sourceNode instanceof Mask)) {
                    return;
                }
                final String maskName = ((Mask) sourceNode).getName();
                final String[] selectedNames = getSelectedRegionMaskNames();

                if (StringUtils.contains(selectedNames, maskName)) {
                    updateEnablement();
                }
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleEvent(event);
        }

        private void handleEvent(ProductNodeEvent event) {
            if (!isRunning()) {

                ProductNode sourceNode = event.getSourceNode();

                if (sourceNode instanceof Mask) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
//                            resetBandNameListState();
                            resetRegionMaskListState(false);
                            resetQualityMaskListState(false);
                        }
                    });

                }
            }
        }
    }

    private JPanel getTopPanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {

        refreshButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ViewRefresh22.png"),
                false);
        refreshButton.setEnabled(rasterDataNode != null);
        refreshButton.setToolTipText("Refresh View");
        refreshButton.setName("refreshButton");

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run();
            }
        });


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(refreshButton, gbc);
        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        return panel;
    }

    public void run() {
        setRunning(true);
        boolean useRoi = useRoiCheckBox.isSelected();


        Mask[] selectedMasks;
        if (useRoi) {
            int[] listIndexes = regionMaskNameList.getCheckBoxListSelectedIndices();
            if (listIndexes.length > 0) {
                selectedMasks = new Mask[listIndexes.length];
                for (int i = 0; i < listIndexes.length; i++) {
                    int listIndex = listIndexes[i];
                    String maskName = regionMaskNameList.getModel().getElementAt(listIndex).toString();
                    selectedMasks[i] = raster.getProduct().getMaskGroup().get(maskName);
                }
            } else {
                selectedMasks = new Mask[]{null};
            }
        } else {
            selectedMasks = new Mask[]{null};
        }

        Band[] selectedBands;
        int[] bandListIndexes = bandNameList.getCheckBoxListSelectedIndices();
        if (bandListIndexes.length > 0) {
            selectedBands = new Band[bandListIndexes.length];
            for (int i = 0; i < bandListIndexes.length; i++) {
                int listIndex = bandListIndexes[i];
                String bandName = bandNameList.getModel().getElementAt(listIndex).toString();
                selectedBands[i] = raster.getProduct().getBandGroup().get(bandName);
            }
        } else {
            selectedBands = new Band[]{null};
        }


        Mask[] selectedQualityMasks;
        if (useRoi) {
            int[] listIndexes = qualityMaskNameList.getCheckBoxListSelectedIndices();
            if (listIndexes.length > 0) {
                selectedQualityMasks = new Mask[listIndexes.length];
                for (int i = 0; i < listIndexes.length; i++) {
                    int listIndex = listIndexes[i];
                    String maskName = qualityMaskNameList.getModel().getElementAt(listIndex).toString();
                    selectedQualityMasks[i] = raster.getProduct().getMaskGroup().get(maskName);
                }
            } else {
                selectedQualityMasks = new Mask[]{null};
            }
        } else {
            selectedQualityMasks = new Mask[]{null};
        }


        method.compute(selectedMasks, selectedQualityMasks, selectedBands);
    }


    private void selectAndEnableRegionCheckBoxes() {
        final int numEntries = regionMaskNameList.getModel().getSize();
        final int numSelected = regionMaskNameList.getCheckBoxListSelectedIndices().length;
//        regionMaskSelectAllCheckBox.setEnabled(numSelected < numEntries);

        regionMaskSelectAllCheckBox.setEnabled(false);
        regionMaskSelectAllCheckBox.setSelected(numEntries > 0 && numSelected == numEntries);
        regionMaskSelectAllCheckBox.setEnabled(true);

        regionalMaskGroupingComboBoxLabel.setVisible(numSelected > 0);
        regionalMaskGroupingComboBox.setVisible(numSelected > 0);
        if (numSelected == 0) {
            regionalGroupMaskNamePanel.setVisible(false);
        } else if (regionalMaskGroupingComboBox.getSelectedItem() != MaskGrouping.INDIVIDUAL) {
            regionalGroupMaskNamePanel.setVisible(true);
        }
    }


    private String[] getSelectedRegionMaskNames() {
        final Object[] selectedValues = regionMaskNameList.getCheckBoxListSelectedValues();
        return StringUtils.toStringArray(selectedValues);
    }

    private void updateRegionMaskListState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final String text = regionMaskNameSearchField.getText();
                final DefaultListModel<Object> updatedListModel = new DefaultListModel<>();
                List<Boolean> selected = new ArrayList<>();
                int[] newIndexesInMaskNameList = new int[regionMaskNameListModel.getSize()];
                int counter = 0;
                for (int i = 0; i < regionMaskNameListModel.getSize(); i++) {
                    if (regionMaskNameListModel.get(i).toLowerCase().contains(text.toLowerCase())) {
                        updatedListModel.addElement(regionMaskNameListModel.get(i));
                        if (indexesInRegionMaskNameList[i] >= 0) {
                            selected.add(regionMaskNameList.getCheckBoxListSelectionModel().isSelectedIndex(indexesInRegionMaskNameList[i]));
                        } else {
                            selected.add(false);
                        }
                        newIndexesInMaskNameList[i] = counter++;
                    } else {
                        newIndexesInMaskNameList[i] = -1;
                    }
                }
                indexesInRegionMaskNameList = newIndexesInMaskNameList;
                regionMaskNameList.setModel(updatedListModel);
                for (int i = 0; i < selected.size(); i++) {
                    if (selected.get(i)) {
                        regionMaskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
                    }
                }
            }
        });
    }


//    private void updateRegionMaskListState() {
//
//        final DefaultListModel maskNameListModel = new DefaultListModel();
//        final String[] currentSelectedMaskNames = getSelectedRegionMaskNames();
//
//        if (product != null) {
//            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
//            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
//            for (Mask mask : masks) {
//                if (mask != null && mask.getName() != null) {
//
//                    String imageTypeName = null;
//                    if (mask.getImageType() != null && mask.getImageType().getName() != null) {
//                        imageTypeName = mask.getImageType().getName();
//                    }
//
//                    if ("Geometry".equals(imageTypeName) ||
//                            mask.getName().toLowerCase().contains("region") ||
//                            mask.getName().toLowerCase().contains("bathymetry") ||
//                            mask.getName().toLowerCase().contains("topography") ||
//                            mask.getName().toLowerCase().contains("elev") ||
//                            mask.getName().toLowerCase().contains("land") ||
//                            mask.getName().toLowerCase().contains("water")) {
//                        maskNameListModel.addElement(mask.getName());
//                    }
//                }
//            }
//        }
//
//        try {
////            regionMaskNameSearchField.setListModel(maskNameListModel);
//            if (product != null) {
//                regionMaskNameList.setModel(maskNameListModel);
//            }
//        } catch (Throwable e) {
//
//            /*
//            We catch everything here, because there seems to be a bug in the combination of
//            JIDE QuickListFilterField and FilteredCheckBoxList:
//             java.lang.IndexOutOfBoundsException: bitIndex < 0: -1
//             	at java.util.BitSet.get(BitSet.java:441)
//             	at javax.swing.DefaultListSelectionModel.clear(DefaultListSelectionModel.java:257)
//             	at javax.swing.DefaultListSelectionModel.setState(DefaultListSelectionModel.java:567)
//             	at javax.swing.DefaultListSelectionModel.removeIndexInterval(DefaultListSelectionModel.java:635)
//             	at com.jidesoft.list.CheckBoxListSelectionModelWithWrapper.removeIndexInterval(Unknown Source)
///             */
//            Debug.trace(e);
//        }
//
//        final String[] allNames = StringUtils.toStringArray(maskNameListModel.toArray());
//        for (int i = 0; i < allNames.length; i++) {
//            String name = allNames[i];
//            if (StringUtils.contains(currentSelectedMaskNames, name)) {
//                regionMaskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
//            }
//        }
//
//        updateEnablement();
//    }

    public void clearMaskSelections() {
        if (regionMaskNameList != null) {
            regionMaskNameList.selectNone();
        }
        if (qualityMaskNameList != null) {
            qualityMaskNameList.selectNone();
        }

    }

    private JPanel getRegionMaskFilterPanel() {

        regionMaskNameSearchField = new JTextField(10);
        regionMaskNameSearchField.setEnabled(false);
        regionMaskNameSearchField.setToolTipText("Mask Search Filter");
        regionMaskNameSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateRegionMaskListState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateRegionMaskListState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRegionMaskListState();
            }
        });

        JLabel jlabel = new JLabel("Mask Search Filter");

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.insets.top = 3;
        gbc.insets.bottom = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(jlabel, gbc);
        gbc.gridx++;
        panel.add(regionMaskNameSearchField, gbc);

        return panel;
    }


    private JPanel getRegionMaskNameListPanel() {
        regionMaskNameList = new CheckBoxList(new DefaultListModel());

        regionMaskNameList.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        regionMaskNameList.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnablement();
                refreshButton.setEnabled(true);
                if (!e.getValueIsAdjusting()) {
                    selectAndEnableRegionCheckBoxes();
                }
            }
        });

        regionMaskNameList.setVisibleRowCount(getScrollListMaxLines());
        regionNameScrollPane = new JScrollPane(regionMaskNameList);

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        panel.add(regionNameScrollPane, gbc);

        return panel;

    }


    private JPanel getRegionSelectAllNonePanel() {

        regionMaskSelectAllCheckBox = new JCheckBox("Select All/None");
        regionMaskSelectAllCheckBox.setToolTipText("Select all/none toggle");

        regionMaskSelectAllCheckBox.setSelected(false);
        regionMaskSelectAllCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (regionMaskSelectAllCheckBox.isEnabled()) {
                    if (regionMaskSelectAllCheckBox.isSelected()) {
                        regionMaskNameList.selectAll();
                    } else {
                        regionMaskNameList.selectNone();
                    }
                }
                selectAndEnableRegionCheckBoxes();

            }
        });
        regionMaskSelectAllCheckBox.setMinimumSize(regionMaskSelectAllCheckBox.getPreferredSize());
        regionMaskSelectAllCheckBox.setPreferredSize(regionMaskSelectAllCheckBox.getPreferredSize());



        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

//        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(regionMaskSelectAllCheckBox, gbc);

        return panel;
    }


    public boolean isIncludeFullScene() {
        return includeFullScene;
    }


    private void selectAndEnableQualityCheckBoxes() {
        final int numEntries = qualityMaskNameList.getModel().getSize();
        final int numSelected = qualityMaskNameList.getCheckBoxListSelectedIndices().length;
//        qualityMaskSelectAllCheckBox.setEnabled(numSelected < numEntries);

        qualityMaskSelectAllCheckBox.setEnabled(false);
        qualityMaskSelectAllCheckBox.setSelected(numEntries > 0 && numSelected == numEntries);
        qualityMaskSelectAllCheckBox.setEnabled(true);

        qualityMaskGroupingComboBoxLabel.setVisible(numSelected > 0);
        qualityMaskGroupingComboBox.setVisible(numSelected > 0);
        if (numSelected == 0) {
            qualityGroupMaskNamePanel.setVisible(false);
        } else if (qualityMaskGroupingComboBox.getSelectedItem() != MaskGrouping.INDIVIDUAL) {
            qualityGroupMaskNamePanel.setVisible(true);
        }

    }


    private String[] getSelectedQualityMaskNames() {
        final Object[] selectedValues = qualityMaskNameList.getCheckBoxListSelectedValues();
        return StringUtils.toStringArray(selectedValues);
    }

    private void updateQualityMaskListState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final String text = qualityMaskNameSearchField.getText();
                final DefaultListModel<Object> updatedListModel = new DefaultListModel<>();
                List<Boolean> selected = new ArrayList<>();
                int[] newIndexesInQualityMaskNameList = new int[qualityMaskNameListModel.getSize()];
                int counter = 0;
                for (int i = 0; i < qualityMaskNameListModel.getSize(); i++) {
                    if (qualityMaskNameListModel.get(i).toLowerCase().contains(text.toLowerCase())) {
                        updatedListModel.addElement(qualityMaskNameListModel.get(i));
                        if (indexesInQualityMaskNameList[i] >= 0) {
                            selected.add(qualityMaskNameList.getCheckBoxListSelectionModel().isSelectedIndex(indexesInQualityMaskNameList[i]));
                        } else {
                            selected.add(false);
                        }
                        newIndexesInQualityMaskNameList[i] = counter++;
                    } else {
                        newIndexesInQualityMaskNameList[i] = -1;
                    }
                }
                indexesInQualityMaskNameList = newIndexesInQualityMaskNameList;
                qualityMaskNameList.setModel(updatedListModel);
                for (int i = 0; i < selected.size(); i++) {
                    if (selected.get(i)) {
                        qualityMaskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
                    }
                }
            }
        });
    }


//    private void updateQualityMaskListState() {
//
//        final DefaultListModel maskNameListModel = new DefaultListModel();
//        final String[] currentSelectedMaskNames = getSelectedQualityMaskNames();
//
//        if (product != null) {
//            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
//            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
//            for (Mask mask : masks) {
//                if (mask != null && mask.getName() != null) {
//
//                    String imageTypeName = null;
//                    if (mask.getImageType() != null && mask.getImageType().getName() != null) {
//                        imageTypeName = mask.getImageType().getName();
//                    }
//
//                    if (!"Geometry".equals(imageTypeName)) {
//                        maskNameListModel.addElement(mask.getName());
//                    }
//                }
//            }
//        }
//
//        try {
////            qualityMaskNameSearchField.setListModel(maskNameListModel);
//            if (product != null) {
//                qualityMaskNameList.setModel(maskNameListModel);
//            }
//        } catch (Throwable e) {
//
//            /*
//            We catch everything here, because there seems to be a bug in the combination of
//            JIDE QuickListFilterField and FilteredCheckBoxList:
//             java.lang.IndexOutOfBoundsException: bitIndex < 0: -1
//             	at java.util.BitSet.get(BitSet.java:441)
//             	at javax.swing.DefaultListSelectionModel.clear(DefaultListSelectionModel.java:257)
//             	at javax.swing.DefaultListSelectionModel.setState(DefaultListSelectionModel.java:567)
//             	at javax.swing.DefaultListSelectionModel.removeIndexInterval(DefaultListSelectionModel.java:635)
//             	at com.jidesoft.list.CheckBoxListSelectionModelWithWrapper.removeIndexInterval(Unknown Source)
///             */
//            Debug.trace(e);
//        }
//
//        final String[] allNames = StringUtils.toStringArray(maskNameListModel.toArray());
//        for (int i = 0; i < allNames.length; i++) {
//            String name = allNames[i];
//            if (StringUtils.contains(currentSelectedMaskNames, name)) {
//                qualityMaskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
//            }
//        }
//
//        updateEnablement();
//    }


    private JPanel getQualityFilterPanel() {

        qualityMaskNameSearchField = new JTextField(10);
        qualityMaskNameSearchField.setEnabled(false);
        qualityMaskNameSearchField.setToolTipText("Mask Search Filter");
        qualityMaskNameSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateQualityMaskListState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateQualityMaskListState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateQualityMaskListState();
            }
        });

        JLabel jlabel = new JLabel("Mask Search Filter");

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.insets.top = 3;
        gbc.insets.bottom = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(jlabel, gbc);
        gbc.gridx++;
        panel.add(qualityMaskNameSearchField, gbc);

        return panel;
    }


    private JPanel getQualityNameListPanel() {
        qualityMaskNameList = new CheckBoxList(new DefaultListModel());

        qualityMaskNameList.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        qualityMaskNameList.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnablement();
                refreshButton.setEnabled(true);
                if (!e.getValueIsAdjusting()) {
                    selectAndEnableQualityCheckBoxes();
                }
            }
        });


        qualityMaskNameList.setVisibleRowCount(getScrollListMaxLines());
        qualityMaskNameScrollPane = new JScrollPane(qualityMaskNameList);

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        panel.add(qualityMaskNameScrollPane, gbc);

        return panel;

    }



    public int getScrollListMaxLines() {
        Preferences preferences = SnapApp.getDefault().getPreferences();
        return preferences.getInt(StatisticsTopComponent.PROPERTY_SCROLL_LINES_KEY, StatisticsTopComponent.PROPERTY_SCROLL_LINES_DEFAULT);
    }

    private JPanel getQualitySelectAllNonePanel() {

        qualityMaskSelectAllCheckBox = new JCheckBox("Select All/None");
        qualityMaskSelectAllCheckBox.setToolTipText("Select all/none toggle");
        qualityMaskSelectAllCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (qualityMaskSelectAllCheckBox.isEnabled()) {
                    if (qualityMaskSelectAllCheckBox.isSelected()) {
                        qualityMaskNameList.selectAll();
                    } else {
                        qualityMaskNameList.selectNone();
                    }
                }
                selectAndEnableQualityCheckBoxes();
            }
        });
        qualityMaskSelectAllCheckBox.setMinimumSize(qualityMaskSelectAllCheckBox.getPreferredSize());
        qualityMaskSelectAllCheckBox.setPreferredSize(qualityMaskSelectAllCheckBox.getPreferredSize());


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

//        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(qualityMaskSelectAllCheckBox, gbc);

        return panel;
    }


    public boolean isIncludeNoQuality() {
        return includeNoQuality;
    }

    public MaskGrouping getQualityMaskGrouping() {
        return (MaskGrouping) qualityMaskGroupingComboBox.getSelectedItem();
        //  return qualityMaskGrouping;
    }


    public MaskGrouping getRegionalMaskGrouping() {
        return (MaskGrouping) regionalMaskGroupingComboBox.getSelectedItem();
        //  return qualityMaskGrouping;
    }


    //---------------------- Bandnames List ----------------------------------------------

//    void setRaster(final RasterDataNode newRaster) {
//        if (this.raster != newRaster) {
//            this.raster = newRaster;
//            if (newRaster == null) {
//                if (product != null) {
//                    product.removeProductNodeListener(productNodeListener);
//                }
//                product = null;
//            } else if (product != newRaster.getProduct()) {
//                if (product != null) {
//                    product.removeProductNodeListener(productNodeListener);
//                }
//                product = newRaster.getProduct();
//                if (product != null) {
//                    product.addProductNodeListener(productNodeListener);
//                }
//            }
//            updateBandNameListState();
//            refreshButton.setEnabled(raster != null);
//        }
//    }


    private class BandNamePNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
//            handleEvent(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (!(sourceNode instanceof Band)) {
                return;
            }
            final String bandName = ((Band) sourceNode).getName();
            final String[] selectedNames = getSelectedBandNames();

            if (StringUtils.contains(selectedNames, bandName)) {
                updateEnablement();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleEvent(event);
        }

        private void handleEvent(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Band) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        resetBandNameListState();
                    }
                });
            }
        }
    }


    private void selectAndEnableBandNameCheckBoxes() {
        final int numEntries = bandNameList.getModel().getSize();
        final int numSelected = bandNameList.getCheckBoxListSelectedIndices().length;

        bandNameselectAllCheckBox.setEnabled(false);
        bandNameselectAllCheckBox.setSelected(numEntries > 0 && numSelected == numEntries);
        bandNameselectAllCheckBox.setEnabled(true);

//        bandNameselectAllCheckBox.setEnabled(numSelected < numEntries);
//        bandNameselectAllCheckBox.setSelected(numSelected == numEntries);
    }

    private String[] getSelectedBandNames() {
        final Object[] selectedValues = bandNameList.getCheckBoxListSelectedValues();
        return StringUtils.toStringArray(selectedValues);
    }

    private void updateBandNameListState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final String text = bandNameSearchField.getText();
                final DefaultListModel<Object> updatedListModel = new DefaultListModel<>();
                List<Boolean> selected = new ArrayList<>();
                int[] newIndexesInBandNameList = new int[bandNameListModel.getSize()];
                int counter = 0;
                for (int i = 0; i < bandNameListModel.getSize(); i++) {
                    if (bandNameListModel.get(i).toLowerCase().contains(text.toLowerCase())) {
                        updatedListModel.addElement(bandNameListModel.get(i));
                        if (indexesInBandNameList[i] >= 0) {
                            selected.add(bandNameList.getCheckBoxListSelectionModel().isSelectedIndex(indexesInBandNameList[i]));
                        } else {
                            selected.add(false);
                        }
                        newIndexesInBandNameList[i] = counter++;
                    } else {
                        newIndexesInBandNameList[i] = -1;
                    }
                }
                indexesInBandNameList = newIndexesInBandNameList;
                bandNameList.setModel(updatedListModel);
                for (int i = 0; i < selected.size(); i++) {
                    if (selected.get(i)) {
                        bandNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
                    }
                }
            }


        });
    }

//    private void updateBandNameListState() {
//
//        final DefaultListModel bandNameListModel = new DefaultListModel();
//        final String[] currentSelectedBandNames = getSelectedBandNames();
//
//        if (product != null) {
//            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
//            final Band[] bands = bandGroup.toArray(new Band[bandGroup.getNodeCount()]);
//            for (Band band : bands) {
//                bandNameListModel.addElement(band.getName());
//            }
//        }
//
//        try {
////            bandNameSearchField.setText(String.valueOf(bandNameListModel));
//            if (product != null) {
//                bandNameList.setModel(bandNameListModel);
//            }
//        } catch (Throwable e) {
//
//            /*
//            We catch everything here, because there seems to be a bug in the combination of
//            JIDE QuickListFilterField and FilteredCheckBoxList:
//             java.lang.IndexOutOfBoundsException: bitIndex < 0: -1
//             	at java.util.BitSet.get(BitSet.java:441)
//             	at javax.swing.DefaultListSelectionModel.clear(DefaultListSelectionModel.java:257)
//             	at javax.swing.DefaultListSelectionModel.setState(DefaultListSelectionModel.java:567)
//             	at javax.swing.DefaultListSelectionModel.removeIndexInterval(DefaultListSelectionModel.java:635)
//             	at com.jidesoft.list.CheckBoxListSelectionModelWithWrapper.removeIndexInterval(Unknown Source)
///             */
//            Debug.trace(e);
//        }
//
//        final String[] allNames = StringUtils.toStringArray(bandNameListModel.toArray());
//        for (int i = 0; i < allNames.length; i++) {
//            String name = allNames[i];
//            if (StringUtils.contains(currentSelectedBandNames, name)) {
//                bandNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
//            }
//        }
//
//        if (forceUpdate && raster != null) {
//            bandNameList.selectNone();
//            bandNameList.clearCheckBoxListSelection();
//            bandNameList.clearSelection();
//            String[] selectedBandNames = {raster.getName()};
//            bandNameList.setSelectedObjects(selectedBandNames);
//        }
//
//        updateEnablement();
//    }


    private JPanel getBandNameListPanel() {
        bandNameList = new CheckBoxList(new DefaultListModel());

        bandNameList.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        bandNameList.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnablement();
                refreshButton.setEnabled(true);
                if (!e.getValueIsAdjusting()) {
                    selectAndEnableBandNameCheckBoxes();
                }
            }
        });


        bandNameList.setVisibleRowCount(getScrollListMaxLines());
        bandNameScrollPane = new JScrollPane(bandNameList);

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        panel.add(bandNameScrollPane, gbc);

        return panel;

    }

    private JPanel getBandNameSelectAllNonePanel() {

        bandNameselectAllCheckBox = new JCheckBox("Select All/None");
        bandNameselectAllCheckBox.setToolTipText("Select all/none toggle");

        bandNameselectAllCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (bandNameselectAllCheckBox.isEnabled()) {
                    if (bandNameselectAllCheckBox.isSelected()) {
                        bandNameList.selectAll();
                    } else {
                        bandNameList.selectNone();
                    }
                }
                selectAndEnableBandNameCheckBoxes();
            }
        });
        bandNameselectAllCheckBox.setMinimumSize(bandNameselectAllCheckBox.getPreferredSize());
        bandNameselectAllCheckBox.setPreferredSize(bandNameselectAllCheckBox.getPreferredSize());



        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

//        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(bandNameselectAllCheckBox, gbc);

        return panel;
    }


    private JPanel getBandFilterPanel() {

        bandNameSearchField = new JTextField(10);
//        bandNameSearchField.setEnabled(false);
        bandNameSearchField.setToolTipText("Band Search Filter");
        bandNameSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateBandNameListState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateBandNameListState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateBandNameListState();
            }
        });

        JLabel jlabel = new JLabel("Band Search Filter");

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.insets.top = 3;
        gbc.insets.bottom = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(jlabel, gbc);
        gbc.gridx++;
        panel.add(bandNameSearchField, gbc);

        return panel;
    }


    //--------------------- General-------------------------------------------


    public boolean isUseViewBandRaster() {
        return useViewBandRaster;
    }

    public void setUseViewBandRaster(boolean useViewBandRaster) {
        this.useViewBandRaster = useViewBandRaster;
    }


    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
        updateRunButton(!running);
    }


    public JTextField getQualityGroupMaskNameTextfield() {
        return qualityGroupMaskNameTextfield;
    }

    public void setQualityGroupMaskNameTextfield(JTextField qualityGroupMaskNameTextfield) {
        this.qualityGroupMaskNameTextfield = qualityGroupMaskNameTextfield;
    }

    public JTextField getRegionalGroupMaskNameTextfield() {
        return regionalGroupMaskNameTextfield;
    }

    public void setRegionalGroupMaskNameTextfield(JTextField regionalGroupMaskNameTextfield) {
        this.regionalGroupMaskNameTextfield = regionalGroupMaskNameTextfield;
    }


    class MyComboBoxRenderer extends BasicComboBoxRenderer {

        private MaskGroupingToolTips[] tooltips;
        private Boolean[] enabledList;




        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {


            if (isSelected) {

                if (-1 < index && index < tooltips.length) {
                    list.setToolTipText(tooltips[index].toString());

                    setBackground(Color.blue);
                    setForeground(Color.white);
                }


            } else {
                setBackground(Color.white);
                setForeground(Color.black);
            }


            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());
            return this;
        }

        public void setTooltipList(MaskGroupingToolTips[] tooltipList) {
            this.tooltips = tooltipList;
        }

        public void setEnabledList(Boolean[] enabledList) {
            this.enabledList = enabledList;
        }
    }

}
