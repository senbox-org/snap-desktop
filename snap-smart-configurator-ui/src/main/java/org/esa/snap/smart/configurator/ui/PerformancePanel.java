/*
 * Copyright (C) 2015 CS SI
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
package org.esa.snap.smart.configurator.ui;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.smart.configurator.Benchmark;
import org.esa.snap.smart.configurator.BenchmarkOperatorProvider;
import org.esa.snap.smart.configurator.BenchmarkSingleCalculus;
import org.esa.snap.smart.configurator.ConfigurationOptimizer;
import org.esa.snap.smart.configurator.JavaSystemInfos;
import org.esa.snap.smart.configurator.PerformanceParameters;
import org.esa.snap.smart.configurator.VMParameters;
import org.esa.snap.ui.AppContext;

import javax.media.jai.JAI;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.regex.Pattern;

final class PerformancePanel extends javax.swing.JPanel {
    
    /**
     * Color for fields filed with values in place in the application
     */
    private static final Color CURRENT_VALUES_COLOR = Color.BLACK;
    

    /**
     * Color for error fields
     *
     */
    private static final Color ERROR_VALUES_COLOR = Color.RED;


    /**
     * Separator between values to be tested for the benchmark
     */
    private static final String BENCHMARK_SEPARATOR=";";

    private static final int nbCores = JavaSystemInfos.getInstance().getNbCPUs();

    /**
     * Tool for optimizing and setting the performance parameters
     */
    private final ConfigurationOptimizer confOptimizer;

    private final PerformanceOptionsPanelController controller;



    private static Path getUserDirPathFromString(String userDirString) {
        Path userDirPath = null;
        try {
            File userDirAsFile = new File(userDirString);
            userDirPath = FileUtils.getPathFromURI(userDirAsFile.toURI());
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.WARNING, "Cannot convert performance parameters to PATH: {0}", userDirString);
        }

        return userDirPath;
    }

    PerformancePanel(PerformanceOptionsPanelController controller) {

        this.controller = controller;

        confOptimizer = ConfigurationOptimizer.getInstance();

        initComponents();

        DocumentListener textFieldListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controller.changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                controller.changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                controller.changed();
            }
        };

        vmParametersTextField.getDocument().addDocumentListener(textFieldListener);
        cachePathTextField.getDocument().addDocumentListener(textFieldListener);
        nbThreadsTextField.getDocument().addDocumentListener(textFieldListener);
        tileSizeTextField.getDocument().addDocumentListener(textFieldListener);
        cacheSizeTextField.getDocument().addDocumentListener(textFieldListener);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {

        PerformanceParameters actualParameters = confOptimizer.getActualPerformanceParameters();

        java.awt.GridBagConstraints gridBagConstraints;

        systemParametersPanel = new javax.swing.JPanel();
        cachePathLabel = new javax.swing.JLabel();
        vmParametersTextField = new javax.swing.JTextField();
        editVMParametersButton = new javax.swing.JButton();
        cachePathTextField = new javax.swing.JTextField();
        browseUserDirButton = new javax.swing.JButton();
        vmParametersLabel = new javax.swing.JLabel();
        sysResetButton = new javax.swing.JButton();
        sysComputeButton = new javax.swing.JButton();
        largeCacheInfoLabel = new javax.swing.JLabel();
        vmParametersInfoLabel = new javax.swing.JLabel();
        processingParametersPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        tileSizeLabel = new javax.swing.JLabel();
        cacheSizeLabel = new javax.swing.JLabel();
        nbThreadsLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        tileSizeTextField = new javax.swing.JTextField();
        cacheSizeTextField = new javax.swing.JTextField();
        nbThreadsTextField = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        benchmarkTileSizeTextField = new javax.swing.JTextField();
        cacheSizeTextField = new javax.swing.JTextField();
        benchmarkNbThreadsTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        procGraphJComboBox = new javax.swing.JComboBox(getBenchmarkOperators());
        procGraphJComboBox.setSelectedItem("StoredGraph");
        jPanel3 = new javax.swing.JPanel();
        processingParamsComputeButton = new javax.swing.JButton();
        processingParamsResetButton = new javax.swing.JButton();

        BoxLayout perfPanelLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(perfPanelLayout);

        Box.createVerticalGlue();

        systemParametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.systemParametersPanel.border.title"))); 
        systemParametersPanel.setMinimumSize(new java.awt.Dimension(283, 115));
        systemParametersPanel.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(cachePathLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.jLabel2.text"));
        cachePathLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        cachePathLabel.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        systemParametersPanel.add(cachePathLabel, gridBagConstraints);


        org.openide.awt.Mnemonics.setLocalizedText(vmParametersLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.jLabel3.text"));
        vmParametersLabel.setMaximumSize(new java.awt.Dimension(200, 14));
        vmParametersLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        vmParametersLabel.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        systemParametersPanel.add(vmParametersLabel, gridBagConstraints);

        vmParametersTextField.setText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.vmParametersTextField.text"));
        vmParametersTextField.setToolTipText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.vmParametersTextField.toolTipText"));
        if (!VMParameters.canSave()) {
            vmParametersTextField.setEditable(false);
        }
        vmParametersTextField.setColumns(50);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        systemParametersPanel.add(vmParametersTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(editVMParametersButton, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.editVMParametersButton.text"));
        editVMParametersButton.addActionListener(this::editVMParametersButtonActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 0, 10);
        systemParametersPanel.add(editVMParametersButton, gridBagConstraints);

        if(!VMParameters.canSave()) {
            vmParametersLabel.setEnabled(false);
            vmParametersTextField.setEnabled(false);
            editVMParametersButton.setEnabled(false);
            String vmParameterDisableToolTip = "VM parameters can't be saved from SNAP, please use the snap-conf-optimiser application as an administrator to change them";
            vmParametersLabel.setToolTipText(vmParameterDisableToolTip);
            vmParametersTextField.setToolTipText(vmParameterDisableToolTip);
            editVMParametersButton.setToolTipText(vmParameterDisableToolTip);
        }

        cachePathTextField.setText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.userDirTextField.text"));
        cachePathTextField.setToolTipText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.userDirTextField.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        systemParametersPanel.add(cachePathTextField, gridBagConstraints);



        org.openide.awt.Mnemonics.setLocalizedText(cacheSizeLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.cacheSizeLabel.text"));
        cacheSizeLabel.setMaximumSize(new java.awt.Dimension(200, 14));
        cacheSizeLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        cacheSizeLabel.setPreferredSize(new java.awt.Dimension(80, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        systemParametersPanel.add(cacheSizeLabel, gridBagConstraints);

        cacheSizeTextField.setText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.cacheSizeTextField.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        systemParametersPanel.add(cacheSizeTextField, gridBagConstraints);







        org.openide.awt.Mnemonics.setLocalizedText(browseUserDirButton, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.browseUserDirButton.text")); 
        browseUserDirButton.addActionListener(evt -> browseCachePathButtonActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 0, 10);
        systemParametersPanel.add(browseUserDirButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(sysResetButton, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.sysResetButton.text")); 
        sysResetButton.addActionListener(evt -> sysResetButtonActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(10, 3, 0, 10);
        systemParametersPanel.add(sysResetButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(sysComputeButton, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.sysComputeButton.text")); 
        sysComputeButton.addActionListener(evt -> sysComputeButtonActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 3);
        systemParametersPanel.add(sysComputeButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(largeCacheInfoLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.largeCacheInfoLabel.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        systemParametersPanel.add(largeCacheInfoLabel, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(vmParametersInfoLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.vmParametersInfoLabel.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        systemParametersPanel.add(vmParametersInfoLabel, gridBagConstraints);

        add(systemParametersPanel);

        Box.createVerticalGlue();

        processingParametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.border.title"))); 
        processingParametersPanel.setName("");
        processingParametersPanel.setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridLayout(/*3*/2, 0, 0, 15));

        org.openide.awt.Mnemonics.setLocalizedText(tileSizeLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.tileSizeLabel.text"));
        tileSizeLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        tileSizeLabel.setPreferredSize(new java.awt.Dimension(100, 14));
        tileSizeLabel.setToolTipText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.tileSizeLabel.toolTipText"));
        jPanel2.add(tileSizeLabel);

        //org.openide.awt.Mnemonics.setLocalizedText(cacheSizeLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.cacheSizeLabel.text"));
        //cacheSizeLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        //cacheSizeLabel.setPreferredSize(new java.awt.Dimension(80, 14));
        //jPanel2.add(cacheSizeLabel);

        org.openide.awt.Mnemonics.setLocalizedText(nbThreadsLabel, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.nbThreadsLabel.text")); 
        nbThreadsLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        jPanel2.add(nbThreadsLabel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        processingParametersPanel.add(jPanel2, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.jPanel1.border.title"))); 
        jPanel1.setMinimumSize(new java.awt.Dimension(100, 100));
        jPanel1.setLayout(new java.awt.GridLayout(/*3*/2, 1, 0, 10));

        tileSizeTextField.setText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.defaultTileSizeTextField.text"));
        tileSizeTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        tileSizeTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        jPanel1.add(tileSizeTextField);

        //cacheSizeTextField.setText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.cacheSizeTextField.text"));
        //cacheSizeTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        //cacheSizeTextField.setName("");
        //cacheSizeTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        //jPanel1.add(cacheSizeTextField);

        nbThreadsTextField.setText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.nbThreadsTextField.text")); 
        nbThreadsTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        jPanel1.add(nbThreadsTextField);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        processingParametersPanel.add(jPanel1, gridBagConstraints);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.jPanel4.border.title"))); 
        jPanel4.setMinimumSize(new java.awt.Dimension(190, 107));
        jPanel4.setLayout(new java.awt.GridLayout(/*3*/2, 1, 0, 10));



        String tileSizeBenchmarkValues = getTileSizeValuesForBenchmark(Integer.toString(actualParameters.getDefaultTileSize()));
        benchmarkTileSizeTextField.setText(tileSizeBenchmarkValues);
        benchmarkTileSizeTextField.setPreferredSize(new java.awt.Dimension(150, 20));
        benchmarkTileSizeTextField.setToolTipText(org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.tileDimensionTextField.toolTipText"));
        jPanel4.add(benchmarkTileSizeTextField);


        //Move to SystemPanel
        //String cacheSizeBenchmarkValues = getDefaultCacheSizeValuesForBenchmark(actualParameters);
        //benchmarkCacheSizeTextField.setText(cacheSizeBenchmarkValues);
        //benchmarkCacheSizeTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        //benchmarkCacheSizeTextField.setName("");
        //benchmarkCacheSizeTextField.setPreferredSize(new java.awt.Dimension(150, 20));
        //jPanel4.add(benchmarkCacheSizeTextField);

        benchmarkNbThreadsTextField.setText(Integer.toString(actualParameters.getNbThreads()) + BENCHMARK_SEPARATOR);
        benchmarkNbThreadsTextField.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel4.add(benchmarkNbThreadsTextField);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        processingParametersPanel.add(jPanel4, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.jLabel1.text")); 
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        processingParametersPanel.add(jLabel1, gridBagConstraints);

        procGraphJComboBox.setMinimumSize(new java.awt.Dimension(180, 22));
        nbThreadsTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 10);
        processingParametersPanel.add(procGraphJComboBox, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(processingParamsComputeButton, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.text")); 
        processingParamsComputeButton.setName(""); 
        processingParamsComputeButton.addActionListener(this::processingParamsComputeButtonActionPerformed);
        jPanel3.add(processingParamsComputeButton);

        org.openide.awt.Mnemonics.setLocalizedText(processingParamsResetButton, org.openide.util.NbBundle.getMessage(PerformancePanel.class, "PerformancePanel.processingParamsResetButton.text")); 
        processingParamsResetButton.addActionListener(this::processingParamsResetButtonActionPerformed);
        jPanel3.add(processingParamsResetButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        processingParametersPanel.add(jPanel3, gridBagConstraints);

        add(processingParametersPanel);
    }

    private String getTileDimensionValuesForBenchmark(String tileDimension) {
        StringBuilder defaultTileSizeValues = new StringBuilder();

        defaultTileSizeValues.append("128");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);
        defaultTileSizeValues.append("256");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);
        defaultTileSizeValues.append("512");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);
        defaultTileSizeValues.append("*");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);

        //return defaultTileSizeValues.toString();
        return JAI.getDefaultTileSize().width + "," + JAI.getDefaultTileSize().height;
    }

    private String getTileSizeValuesForBenchmark(String tileDimension) {
        StringBuilder defaultTileSizeValues = new StringBuilder();

        defaultTileSizeValues.append("128");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);
        defaultTileSizeValues.append("256");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);
        defaultTileSizeValues.append("512");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);
        defaultTileSizeValues.append("*");
        defaultTileSizeValues.append(BENCHMARK_SEPARATOR);

        return defaultTileSizeValues.toString();
        //return JAI.getDefaultTileSize().width + "," + JAI.getDefaultTileSize().height;
    }

    private String getDefaultCacheSizeValuesForBenchmark(PerformanceParameters actualParameters) {
        StringBuilder defaultCacheSizeValues = new StringBuilder();

        int defaultCacheSize = actualParameters.getCacheSize();
        long xmx = actualParameters.getVmXMX();

        if(xmx == 0) {
            PerformanceParameters memoryParameters = new PerformanceParameters();
            ConfigurationOptimizer.getInstance().computeOptimisedRAMParams(memoryParameters);
            xmx = memoryParameters.getVmXMX();
        }

        defaultCacheSizeValues.append(defaultCacheSize);
        defaultCacheSizeValues.append(BENCHMARK_SEPARATOR);

        if(xmx != 0) {
            defaultCacheSizeValues.append(Math.round(xmx * 0.5));
            defaultCacheSizeValues.append(BENCHMARK_SEPARATOR);

            defaultCacheSizeValues.append(Math.round(xmx * 0.75));
            defaultCacheSizeValues.append(BENCHMARK_SEPARATOR);
        }


        return defaultCacheSizeValues.toString();
    }

    private void editVMParametersButtonActionPerformed(ActionEvent e) {
        Object source = e.getSource();
        Window window = null;
        if (source instanceof Component) {
            Component component = (Component) source;
            window = SwingUtilities.getWindowAncestor(component);
        }

        String vmParametersAsBlankSeparatedString = vmParametersTextField.getText();

        LineSplitTextEditDialog vmParamsEditDialog =
                new LineSplitTextEditDialog(window,
                                            vmParametersAsBlankSeparatedString,
                                            " ",
                                            "VM Parameters",
                                            VMParameters.canSave());
        vmParamsEditDialog.show();

        vmParametersTextField.setText(vmParamsEditDialog.getTextWithSeparators());
        controller.changed();
    }

    private Object[] getBenchmarkOperators() {
        ServiceRegistry<BenchmarkOperatorProvider> benchemarkOperatorServiceRegistry =
                ServiceRegistryManager.getInstance().getServiceRegistry(BenchmarkOperatorProvider.class);
        ServiceLoader.loadServices(benchemarkOperatorServiceRegistry);
        Set<BenchmarkOperatorProvider> providers = benchemarkOperatorServiceRegistry.getServices();

        TreeSet<String> externalOperatorsAliases = new TreeSet<>();
        for(BenchmarkOperatorProvider provider : providers) {
            Set<OperatorSpi> operatorSpis = provider.getBenchmarkOperators();
            for(OperatorSpi operatorSpi : operatorSpis) {
                    externalOperatorsAliases.add(operatorSpi.getOperatorAlias());
            }
        }

        return externalOperatorsAliases.toArray();
    }
                                                  

    private void sysResetButtonActionPerformed() {
        setSystemPerformanceParametersToActualValues();
    }

    private void sysComputeButtonActionPerformed() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        PerformanceParameters optimizedParameters = confOptimizer.computeOptimisedSystemParameters();

        if(VMParameters.canSave() && !vmParametersTextField.getText().equals(optimizedParameters.getVMParameters())) {
            vmParametersTextField.setText(optimizedParameters.getVMParameters());
            vmParametersTextField.setForeground(CURRENT_VALUES_COLOR);
            vmParametersTextField.setCaretPosition(0);
        }

        if(VMParameters.canSave() && !cacheSizeTextField.getText().equals(String.valueOf(optimizedParameters.getCacheSize()))) {
            cacheSizeTextField.setText(String.valueOf(optimizedParameters.getCacheSize()));
            cacheSizeTextField.setForeground(CURRENT_VALUES_COLOR);
            cacheSizeTextField.setCaretPosition(0);
        }

        if(!cachePathTextField.getText().equals(optimizedParameters.getCachePath().toString())) {
            cachePathTextField.setText(optimizedParameters.getCachePath().toString());
            cachePathTextField.setForeground(CURRENT_VALUES_COLOR);
        }

        setCursor(Cursor.getDefaultCursor());

        controller.changed();
    }

    private void browseCachePathButtonActionPerformed() {
        JFileChooser fileChooser = new JFileChooser(cachePathTextField.getText());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            cachePathTextField.setText(selectedDir.getAbsolutePath());
            cachePathTextField.setForeground(CURRENT_VALUES_COLOR);
            controller.changed();
        }
    }

    private void processingParamsComputeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if(validCompute()){
            //Create performance parameters benchmark lists
            java.util.List<Integer> tileSizeList = new ArrayList<>();
            java.util.List<String> tileDimensionList = new ArrayList<>();
            java.util.List<Integer> cacheSizesList = new ArrayList<>();
            java.util.List<Integer> nbThreadsList = new ArrayList<>();

            for(String tileSize : StringUtils.split(benchmarkTileSizeTextField.getText(), ';')){
                tileSizeList.add(Integer.parseInt(tileSize));
            }

            //for(String dimension : StringUtils.split(benchmarkTileSizeTextField.getText(), ';')){
            //    tileDimensionList.add(dimension);
            //}
            tileDimensionList.add(JAI.getDefaultTileSize().width + "," + JAI.getDefaultTileSize().height);

            for(String cacheSize : StringUtils.split(cacheSizeTextField.getText(), ';')){
                cacheSizesList.add(Integer.parseInt(cacheSize));
            }
            for(String nbThread : StringUtils.split(benchmarkNbThreadsTextField.getText(), ';')){
                nbThreadsList.add(Integer.parseInt(nbThread));
            }
            Benchmark benchmarkModel = new Benchmark(tileSizeList, tileDimensionList, cacheSizesList, nbThreadsList);
            String opName = procGraphJComboBox.getSelectedItem().toString();
            AppContext appContext = SnapApp.getDefault().getAppContext();
            //launch Benchmark dialog
            BenchmarkDialog productDialog = new BenchmarkDialog(this, opName, benchmarkModel, appContext);
            productDialog.show();
        }
    }

    private void processingParamsResetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        setProcessingPerformanceParametersToActualValues();
    }



    void load() {
        setSystemPerformanceParametersToActualValues();
        setProcessingPerformanceParametersToActualValues();
    }

    void updatePerformanceParameters(BenchmarkSingleCalculus benchmarkSingleCalcul){

        tileSizeTextField.setText(benchmarkSingleCalcul.getDimensionString());
        tileSizeTextField.setForeground(CURRENT_VALUES_COLOR);

        cacheSizeTextField.setText(Integer.toString(benchmarkSingleCalcul.getCacheSize()));
        cacheSizeTextField.setForeground(CURRENT_VALUES_COLOR);

        nbThreadsTextField.setText(Integer.toString(benchmarkSingleCalcul.getNbThreads()));
        nbThreadsTextField.setForeground(CURRENT_VALUES_COLOR);

        this.controller.changed();
    }

    void store() {
        if(valid()) {
            PerformanceParameters updatedPerformanceParams = getPerformanceParameters();
            confOptimizer.updateCustomisedParameters(updatedPerformanceParams);
            try {
                confOptimizer.saveCustomisedParameters();
            } catch (IOException|BackingStoreException e) {
                SystemUtils.LOG.severe("Could not save performance parameters: " + e.getMessage());
                setSystemPerformanceParametersToActualValues();
            }
        }
    }


    private PerformanceParameters getPerformanceParameters() {
        PerformanceParameters parameters = new PerformanceParameters();
        parameters.setVMParameters(vmParametersTextField.getText());
        Path userDirPath = getUserDirPathFromString(cachePathTextField.getText());
        parameters.setCachePath(userDirPath);
        parameters.setDefaultTileSize(Integer.parseInt(tileSizeTextField.getText()));
        parameters.setTileDimension(tileSizeTextField.getText() + "," + tileSizeTextField.getText());
        parameters.setCacheSize(Integer.parseInt(cacheSizeTextField.getText()));
        parameters.setNbThreads(Integer.parseInt(nbThreadsTextField.getText()));
        return parameters;
    }

    boolean valid() {
        boolean isValid = true;

        File userDir = new File(cachePathTextField.getText());
        if(userDir.exists() && !userDir.isDirectory()) {
            cachePathTextField.setForeground(ERROR_VALUES_COLOR);
            isValid = false;
        } else {
            cachePathTextField.setForeground(CURRENT_VALUES_COLOR);
        }

        //Commented because tiledimension has been removed temporary from the panel
        if(PerformanceParameters.isValidDimension(this.tileSizeTextField.getText())) {
            this.tileSizeTextField.setForeground(CURRENT_VALUES_COLOR);
        } else {
            this.tileSizeTextField.setForeground(ERROR_VALUES_COLOR);
            isValid = false;
        }
        
        String readerCacheSize = this.cacheSizeTextField.getText();
        try{
            Integer.parseInt(readerCacheSize);
            cacheSizeTextField.setForeground(CURRENT_VALUES_COLOR);
        } catch (NumberFormatException ex) {
            this.cacheSizeTextField.setForeground(ERROR_VALUES_COLOR);
            isValid = false;
        }


        String nbThreadsString = nbThreadsTextField.getText();
        try{
            int nbThreads = Integer.parseUnsignedInt(nbThreadsString);

            if(nbThreads > nbCores) {
                nbThreadsTextField.setForeground(ERROR_VALUES_COLOR);
                isValid = false;
            } else {
                nbThreadsTextField.setForeground(CURRENT_VALUES_COLOR);
            }
        } catch (NumberFormatException ex) {
            nbThreadsTextField.setForeground(ERROR_VALUES_COLOR);
            isValid = false;
        }

        return isValid;
    }

    private boolean validCompute() {
        boolean isValid = true;
        Pattern patternBenchmarkValues = Pattern.compile("([0-9]+[\\;]*)+");

        /*String[] dimensions = StringUtils.split(benchmarkTileDimensionTextField.getText(), ';');
        boolean isValidDimensions = true;
        for (String dimension : dimensions) {
            if (!PerformanceParameters.isValidDimension(dimension)) {
                isValidDimensions = false;
            }
        }
        if(isValidDimensions) {
            benchmarkTileDimensionTextField.setForeground(CURRENT_VALUES_COLOR);
        } else {
            isValid = false;
            benchmarkTileDimensionTextField.setForeground(ERROR_VALUES_COLOR);
        }*/

        if (!patternBenchmarkValues.matcher(benchmarkTileSizeTextField.getText()).matches()) {
            benchmarkTileSizeTextField.setForeground(ERROR_VALUES_COLOR);
            isValid = false;
        } else {
            benchmarkTileSizeTextField.setForeground(CURRENT_VALUES_COLOR);
        }
        //if (!patternBenchmarkValues.matcher(benchmarkCacheSizeTextField.getText()).matches()) {
        //    benchmarkCacheSizeTextField.setForeground(ERROR_VALUES_COLOR);
        //    isValid = false;
        //} else {
        //    benchmarkCacheSizeTextField.setForeground(CURRENT_VALUES_COLOR);
        //}
        if (!patternBenchmarkValues.matcher(benchmarkNbThreadsTextField.getText()).matches() || !validBenchmarkNbThreads()) {
            benchmarkNbThreadsTextField.setForeground(ERROR_VALUES_COLOR);
            isValid = false;
        } else {
            benchmarkNbThreadsTextField.setForeground(CURRENT_VALUES_COLOR);
        }
        return isValid;
    }

    private boolean validBenchmarkNbThreads(){
        boolean valid = true;
        for(String nbThread : StringUtils.split(benchmarkNbThreadsTextField.getText(), ';')){
            try {
                if(Integer.parseInt(nbThread) > nbCores){
                    valid = false;
                    break;
                }
            } catch (NumberFormatException e){
                valid = false;
                break;
            }
        }
        return valid;
    }

    private void setSystemPerformanceParametersToActualValues() {
        PerformanceParameters actualPerformanceParameters = confOptimizer.getActualPerformanceParameters();

        vmParametersTextField.setText(actualPerformanceParameters.getVMParameters());
        vmParametersTextField.setForeground(CURRENT_VALUES_COLOR);
        vmParametersTextField.setCaretPosition(0);

        cachePathTextField.setText(actualPerformanceParameters.getCachePath().toString());
        cachePathTextField.setForeground(CURRENT_VALUES_COLOR);

        cacheSizeTextField.setText(String.valueOf(actualPerformanceParameters.getCacheSize()));
        cacheSizeTextField.setForeground(CURRENT_VALUES_COLOR);

        tileSizeTextField.setText(String.valueOf(actualPerformanceParameters.getDefaultTileSize()));
        tileSizeTextField.setForeground(CURRENT_VALUES_COLOR);
    }

    private void setProcessingPerformanceParametersToActualValues() {
        PerformanceParameters actualPerformanceParameters = confOptimizer.getActualPerformanceParameters();

        tileSizeTextField.setText(Integer.toString(actualPerformanceParameters.getDefaultTileSize()));
        tileSizeTextField.setForeground(CURRENT_VALUES_COLOR);

        //cacheSizeTextField.setText(Integer.toString(actualPerformanceParameters.getCacheSize()));
        //cacheSizeTextField.setForeground(CURRENT_VALUES_COLOR);

        nbThreadsTextField.setText(Integer.toString(actualPerformanceParameters.getNbThreads()));
        nbThreadsTextField.setForeground(CURRENT_VALUES_COLOR);
    }

    private javax.swing.JTextField cacheSizeTextField;
    private javax.swing.JTextField benchmarkNbThreadsTextField;
    private javax.swing.JTextField benchmarkTileSizeTextField;
    private javax.swing.JButton editVMParametersButton;
    private javax.swing.JButton browseUserDirButton;
    private javax.swing.JLabel cacheSizeLabel;
    //private javax.swing.JTextField cacheSizeTextField;
    private javax.swing.JTextField tileSizeTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel cachePathLabel;
    private javax.swing.JLabel vmParametersLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel largeCacheInfoLabel;
    private javax.swing.JLabel nbThreadsLabel;
    private javax.swing.JTextField nbThreadsTextField;
    private javax.swing.JComboBox procGraphJComboBox;
    private javax.swing.JPanel processingParametersPanel;
    private javax.swing.JButton processingParamsComputeButton;
    private javax.swing.JButton processingParamsResetButton;
    private javax.swing.JButton sysComputeButton;
    private javax.swing.JButton sysResetButton;
    private javax.swing.JPanel systemParametersPanel;
    private javax.swing.JLabel tileSizeLabel;
    private javax.swing.JTextField cachePathTextField;
    private javax.swing.JLabel vmParametersInfoLabel;
    private javax.swing.JTextField vmParametersTextField;
}
