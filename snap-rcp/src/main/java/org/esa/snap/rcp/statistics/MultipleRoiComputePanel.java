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
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Zuehlke
 */
class MultipleRoiComputePanel extends JPanel {
    private DefaultListModel<String> maskNameListModel;
    private final JTextField maskNameSearchField;
    private int[] indexesInMaskNameList;

    interface ComputeMasks {

        void compute(Mask[] selectedMasks);
    }

    private final ProductNodeListener productNodeListener;

    private final AbstractButton refreshButton;
    private final JCheckBox useRoiCheckBox;
    private final CheckBoxList maskNameList;
    private final JCheckBox selectAllCheckBox;
    private final JCheckBox selectNoneCheckBox;

    private RasterDataNode raster;
    private Product product;

    MultipleRoiComputePanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {
        productNodeListener = new PNL();

        maskNameSearchField = new JTextField();
        maskNameSearchField.setEnabled(false);
        maskNameSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateMaskListState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateMaskListState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateMaskListState();
            }
        });

        maskNameList = new CheckBoxList(new DefaultListModel());
        maskNameList.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        maskNameList.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnablement();
                refreshButton.setEnabled(true);
                if (!e.getValueIsAdjusting()) {
                    selectAndEnableCheckBoxes();
                }
            }
        });

        useRoiCheckBox = new JCheckBox("Use ROI mask(s):");
        useRoiCheckBox.setMnemonic('R');
        useRoiCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        });

        final JPanel topPanel = new JPanel(new BorderLayout());
        refreshButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ViewRefresh22.png"),
                false);
        refreshButton.setEnabled(rasterDataNode != null);
        refreshButton.setToolTipText("Refresh View");
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean useRoi = useRoiCheckBox.isSelected();
                Mask[] selectedMasks;
                if (useRoi) {
                    int[] listIndexes = maskNameList.getCheckBoxListSelectedIndices();
                    if (listIndexes.length > 0) {
                        selectedMasks = new Mask[listIndexes.length];
                        for (int i = 0; i < listIndexes.length; i++) {
                            int listIndex = listIndexes[i];
                            String maskName = maskNameList.getModel().getElementAt(listIndex).toString();
                            selectedMasks[i] = raster.getProduct().getMaskGroup().get(maskName);
                        }
                    } else {
                        selectedMasks = new Mask[]{null};
                    }
                } else {
                    selectedMasks = new Mask[]{null};
                }
                method.compute(selectedMasks);
                refreshButton.setEnabled(false);
            }
        });
        topPanel.add(refreshButton, BorderLayout.WEST);

        //todo enable showMaskManagerButton
        AbstractButton showMaskManagerButton = createShowMaskManagerButton();
        selectAllCheckBox = new JCheckBox("Select all");
        selectAllCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (selectAllCheckBox.isSelected()) {
                    maskNameList.selectAll();
                }
                selectAndEnableCheckBoxes();
            }
        });
        selectNoneCheckBox = new JCheckBox("Select none");
        selectNoneCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (selectNoneCheckBox.isSelected()) {
                    maskNameList.selectNone();
                }
                selectAndEnableCheckBoxes();
            }
        });
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.add(selectAllCheckBox);
        checkBoxPanel.add(selectNoneCheckBox);

        final JPanel multiRoiComputePanel = GridBagUtils.createPanel();
        GridBagConstraints multiRoiComputePanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,weightx=1");
        GridBagUtils.addToPanel(multiRoiComputePanel, topPanel, multiRoiComputePanelConstraints, "gridx=0,gridy=0,gridwidth=3");
        GridBagUtils.addToPanel(multiRoiComputePanel, new JSeparator(), multiRoiComputePanelConstraints, "gridy=1,fill=HORIZONTAL");
        GridBagUtils.addToPanel(multiRoiComputePanel, useRoiCheckBox, multiRoiComputePanelConstraints, "gridy=2,weightx=0");
        GridBagUtils.addToPanel(multiRoiComputePanel, new JLabel(("Filter: ")), multiRoiComputePanelConstraints, "gridy=3,gridx=0,gridwidth=1,anchor=WEST");
        GridBagUtils.addToPanel(multiRoiComputePanel, maskNameSearchField, multiRoiComputePanelConstraints, "gridx=1,weightx=1");
        GridBagUtils.addToPanel(multiRoiComputePanel, showMaskManagerButton, multiRoiComputePanelConstraints, "gridy=3,gridx=2,weightx=0");
        GridBagUtils.addToPanel(multiRoiComputePanel, new JScrollPane(maskNameList), multiRoiComputePanelConstraints, "gridy=4,gridx=0,fill=HORIZONTAL,gridwidth=3,anchor=NORTHWEST");
        GridBagUtils.addToPanel(multiRoiComputePanel, checkBoxPanel, multiRoiComputePanelConstraints, "gridy=5,weighty=1,gridwidth=3");
        add(multiRoiComputePanel);

        setRaster(rasterDataNode);
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
        if (this.raster != newRaster) {
            this.raster = newRaster;
            if (newRaster == null) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = null;
            } else if (product != newRaster.getProduct()) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = newRaster.getProduct();
                if (product != null) {
                    product.addProductNodeListener(productNodeListener);
                }
            }
            resetMaskListState();
            refreshButton.setEnabled(raster != null);
        }
    }

    private void selectAndEnableCheckBoxes() {
        final int numEntries = maskNameList.getModel().getSize();
        final int numSelected = maskNameList.getCheckBoxListSelectedIndices().length;
        selectNoneCheckBox.setEnabled(numSelected > 0);
        selectAllCheckBox.setEnabled(numSelected < numEntries);
        selectNoneCheckBox.setSelected(numSelected == 0);
        selectAllCheckBox.setSelected(numSelected == numEntries);
    }

    private String[] getSelectedMaskNames() {
        final Object[] selectedValues = maskNameList.getCheckBoxListSelectedValues();
        return StringUtils.toStringArray(selectedValues);
    }

    private void resetMaskListState() {
        maskNameListModel = new DefaultListModel<String>();
        final String[] currentSelectedMaskNames = getSelectedMaskNames();
        if (product != null) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (Mask mask : masks) {
                maskNameListModel.addElement(mask.getName());
            }
            maskNameList.setModel(maskNameListModel);
        }
        final String[] allNames = StringUtils.toStringArray(maskNameListModel.toArray());
        indexesInMaskNameList = new int[allNames.length];
        for (int i = 0; i < allNames.length; i++) {
            String name = allNames[i];
            if (StringUtils.contains(currentSelectedMaskNames, name)) {
                maskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
            }
            indexesInMaskNameList[i] = i;
        }
        updateEnablement();
    }

    void updateEnablement() {
        boolean hasMasks = (product != null && product.getMaskGroup().getNodeCount() > 0);
        boolean canSelectMasks = hasMasks && useRoiCheckBox.isSelected();
        useRoiCheckBox.setEnabled(hasMasks);
        maskNameSearchField.setEnabled(canSelectMasks);
        maskNameList.setEnabled(canSelectMasks);
        selectAllCheckBox.setEnabled(canSelectMasks && maskNameList.getCheckBoxListSelectedIndices().length < maskNameList.getModel().getSize());
        selectNoneCheckBox.setEnabled(canSelectMasks && maskNameList.getCheckBoxListSelectedIndices().length > 0);
        refreshButton.setEnabled(raster != null);
    }

    private void updateMaskListState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final String text = maskNameSearchField.getText();
                final DefaultListModel<Object> updatedListModel = new DefaultListModel<>();
                List<Boolean> selected = new ArrayList<>();
                int[] newIndexesInMaskNameList = new int[maskNameListModel.getSize()];
                int counter = 0;
                for (int i = 0; i < maskNameListModel.getSize(); i++) {
                    if (maskNameListModel.get(i).toLowerCase().contains(text.toLowerCase())) {
                        updatedListModel.addElement(maskNameListModel.get(i));
                        if (indexesInMaskNameList[i] >= 0) {
                            selected.add(maskNameList.getCheckBoxListSelectionModel().isSelectedIndex(indexesInMaskNameList[i]));
                        } else {
                            selected.add(false);
                        }
                        newIndexesInMaskNameList[i] = counter++;
                    } else {
                        newIndexesInMaskNameList[i] = -1;
                    }
                }
                indexesInMaskNameList = newIndexesInMaskNameList;
                maskNameList.setModel(updatedListModel);
                for (int i = 0; i < selected.size(); i++) {
                    if (selected.get(i)) {
                        maskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
                    }
                }
            }
        });
    }

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
            if (!useRoiCheckBox.isSelected()) {
                return;
            }
            final ProductNode sourceNode = event.getSourceNode();
            if (!(sourceNode instanceof Mask)) {
                return;
            }
            final String maskName = ((Mask) sourceNode).getName();
            final String[] selectedNames = getSelectedMaskNames();

            if (StringUtils.contains(selectedNames, maskName)) {
                updateEnablement();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleEvent(event);
        }

        private void handleEvent(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Mask) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        resetMaskListState();
                    }
                });
            }
        }
    }
}
