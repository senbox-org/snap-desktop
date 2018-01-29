/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf.ui;

import org.esa.snap.classification.gpf.BaseClassifier;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.engine_utilities.util.VectorUtils;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User interface for classifiers
 */
public abstract class BaseClassifierOpUI extends BaseOperatorUI {

    private final JRadioButton loadBtn = new JRadioButton("Load and apply classifier", false);
    private final JRadioButton trainBtn = new JRadioButton("Train and apply classifier", true);

    private final JComboBox<String> classifierNameComboBox = new JComboBox();
    private final JButton deleteClassiferBtn = new JButton("X");
    private final JTextField newClassifierNameField = new JTextField("newClassifier");

    private final JRadioButton trainOnRasterBtn = new JRadioButton("Train on Raster", false);
    private final JRadioButton trainOnVectorsBtn = new JRadioButton("Train on Vectors", true);

    private final JTextField numTrainSamples = new JTextField("");

    private final JCheckBox evaluateClassifier = new JCheckBox("");
    private final JCheckBox evaluateFeaturePowerSet = new JCheckBox("");
    private final JTextField minPowerSetSize = new JTextField("");
    private final JTextField maxPowerSetSize = new JTextField("");
    private final JCheckBox doClassValQuantization = new JCheckBox();

    private final JTextField minClassValue = new JTextField("");
    private final JTextField classValStepSize = new JTextField("");
    private final JTextField classLevels = new JTextField("");
    private final JLabel maxClassValue = new JLabel("");

    private final JRadioButton labelSourceVectorName = new JRadioButton("Vector node name", true);
    private final JRadioButton labelSourceAttribute = new JRadioButton("Attribute value", false); // not used

    private final JList<String> trainingBands = new JList();
    private final JList<String> trainingVectors = new JList();
    private final JList<String> featureBandNames = new JList();

    protected JPanel classifierPanel, rasterPanel, vectorPanel, featurePanel;
    protected GridBagConstraints classifiergbc;

    private final String classifierType;

    public BaseClassifierOpUI(final String classifierType) {
        this.classifierType = classifierType;
    }

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        doClassValQuantization.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableQuantization(doClassValQuantization.isSelected());
            }
        });

        minClassValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMaxClassValue();
            }
        });

        classValStepSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMaxClassValue();
            }
        });

        classLevels.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMaxClassValue();
            }
        });

        evaluateClassifier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enablePowerSet();
            }
        });

        evaluateFeaturePowerSet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enablePowerSet();
            }
        });

        loadBtn.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean doTraining = e.getStateChange() != ItemEvent.SELECTED;
                enableTraining(doTraining);
                enableTrainOnRaster(doTraining, trainOnRasterBtn.isSelected());
            }
        });

        labelSourceAttribute.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final AttributeDialog dlg = new AttributeDialog("Labels from Attribute",
                                                                    VectorUtils.getAttributesList(sourceProducts), null);
                    dlg.show();
                    if (dlg.IsOK()) {
                        labelSourceAttribute.setText(dlg.getValue());
                    }
                }
            }
        });

        populateClassifierNames();
        classifierNameComboBox.setEditable(false);
        classifierNameComboBox.setMaximumRowCount(5);

        deleteClassiferBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestDeleteClassifier();
            }
        });

        trainingBands.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trainingVectors.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        trainOnRasterBtn.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableTrainOnRaster(trainBtn.isSelected(), e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        initParameters();
        return new JScrollPane(panel);
    }

    private Path getClassifierFolder() {
        return SystemUtils.getAuxDataPath().
                resolve(BaseClassifier.CLASSIFIER_ROOT_FOLDER).resolve(classifierType);
    }

    private void populateClassifierNames() {
        final Path classifierDir = getClassifierFolder();

        final File folder = new File(classifierDir.toString());
        final File[] listOfFiles = folder.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(BaseClassifier.CLASSIFIER_FILE_EXTENSION);
            }
        });
        if (listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                classifierNameComboBox.addItem(FileUtils.getFilenameWithoutExtension(file));
            }
            classifierNameComboBox.setSelectedIndex(0);
        } else {
            trainBtn.setSelected(true);
        }
    }

    private void requestDeleteClassifier() {
        String name = (String) classifierNameComboBox.getSelectedItem();
        if (name != null) {
            Dialogs.Answer answer = Dialogs.requestDecision("Delete Classifier",
                                                            "Are you sure you want to delete classifier " + name,
                                                            true, null);
            if (answer.equals(Dialogs.Answer.YES)) {
                final Path classifierDir = getClassifierFolder();
                final File classiferFile = classifierDir.resolve(name + BaseClassifier.CLASSIFIER_FILE_EXTENSION).toFile();
                if (classiferFile.exists()) {
                    if (deleteClassifier(classiferFile, name)) {
                        classifierNameComboBox.removeItem(name);
                        if (classifierNameComboBox.getItemCount() == 0) {
                            trainBtn.setSelected(true);
                        }
                    } else {
                        Dialogs.showError("Unable to delete classifier " + classiferFile);
                    }
                } else {
                    Dialogs.showError("Unable to find classifier " + classiferFile);
                }
            }
        }
    }

    private static boolean deleteClassifier(final File classifierFile, final String classifierName) {
        boolean ok = classifierFile.delete();
        // find other associated files
        final File[] files = classifierFile.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return FileUtils.getFilenameWithoutExtension(pathname).equals(classifierName);
            }
        });
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        return ok;
    }

    protected abstract void setEnabled(final boolean enabled);

    private void enablePowerSet() {
        final boolean evalEnabled = evaluateClassifier.isEnabled() && evaluateClassifier.isSelected();
        evaluateFeaturePowerSet.setEnabled(evalEnabled);
        final boolean psEnabled = evaluateFeaturePowerSet.isSelected();
        minPowerSetSize.setEnabled(evalEnabled && psEnabled);
        maxPowerSetSize.setEnabled(evalEnabled && psEnabled);
    }

    private void enableTrainOnRaster(final boolean doTraining, final boolean trainOnRaster) {
        //System.out.println("BaseClassifierOpUI.enableTrainOnRaster: doTraining = " + doTraining + " trainOnRaster = " + trainOnRaster);
        if (doTraining) {
            if (trainOnRaster) {
                OperatorUIUtils.initParamList(trainingBands, getTrainingBands());
            } else {
                OperatorUIUtils.initParamList(trainingVectors, getPolygons());
            }
        }
        rasterPanel.setVisible(doTraining && trainOnRaster);
        vectorPanel.setVisible(doTraining && !trainOnRaster);
        featurePanel.setVisible(doTraining);
    }

    private void enableQuantization(final boolean enable) {
        minClassValue.setEnabled(enable);
        classValStepSize.setEnabled(enable);
        classLevels.setEnabled(enable);
        maxClassValue.setEnabled(enable);
    }

    private void setEnableDoClassValQuantization(final boolean enable) {
        doClassValQuantization.setEnabled(enable);
        minClassValue.setEnabled(enable && doClassValQuantization.isSelected());
        classValStepSize.setEnabled(enable && doClassValQuantization.isSelected());
        classLevels.setEnabled(enable && doClassValQuantization.isSelected());
        maxClassValue.setEnabled(enable && doClassValQuantization.isSelected());
    }

    private void enableTraining(boolean doTraining) {

        classifierNameComboBox.setEnabled(!doTraining);
        deleteClassiferBtn.setEnabled(!doTraining);
        newClassifierNameField.setEnabled(doTraining);

        setEnableDoClassValQuantization(doTraining);

        trainOnRasterBtn.setEnabled(doTraining);
        trainOnVectorsBtn.setEnabled(doTraining);

        numTrainSamples.setEnabled(doTraining);

        trainingBands.setEnabled(doTraining);
        if (!trainingBands.isEnabled()) {
            trainingBands.clearSelection();
        }

        trainingVectors.setEnabled(doTraining);
        if (!trainingVectors.isEnabled()) {
            trainingVectors.clearSelection();
        }

        featureBandNames.setEnabled(doTraining);

        evaluateClassifier.setEnabled(doTraining);
        evaluateFeaturePowerSet.setEnabled(doTraining);

        setEnabled(doTraining);
    }

    private void updateMaxClassValue() {
        final double minVal = Double.parseDouble(minClassValue.getText());
        final double stepSize = Double.parseDouble(classValStepSize.getText());
        final int levels = Integer.parseInt(classLevels.getText());
        final double maxClassVal = BaseClassifier.getMaxValue(minVal, stepSize, levels);
        maxClassValue.setText(String.valueOf(maxClassVal));
    }

    @Override
    public void initParameters() {
        String newClassifierName = (String) paramMap.get("savedClassifierName");
        if (DialogUtils.contains(classifierNameComboBox, newClassifierName)) {
            classifierNameComboBox.setSelectedItem(newClassifierName);
        }

        String numSamples = String.valueOf(paramMap.get("numTrainSamples"));
        numTrainSamples.setText(numSamples);

        Boolean eval = (Boolean) (paramMap.get("evaluateClassifier"));
        if (eval != null) {
            evaluateClassifier.setSelected(eval);
        }
        Boolean evalPS = (Boolean) (paramMap.get("evaluateFeaturePowerSet"));
        if (evalPS != null) {
            evaluateFeaturePowerSet.setSelected(evalPS);
        }
        Integer minPS = (Integer) (paramMap.get("minPowerSetSize"));
        if (minPS != null) {
            minPowerSetSize.setText(String.valueOf(minPS));
        }
        Integer maxPS = (Integer) (paramMap.get("maxPowerSetSize"));
        if (maxPS != null) {
            maxPowerSetSize.setText(String.valueOf(maxPS));
        }

        Boolean doQuant = (Boolean) (paramMap.get("doClassValQuantization"));
        if (doQuant != null) {
            doClassValQuantization.setSelected(doQuant);
        }

        minClassValue.setText(String.valueOf(paramMap.get("minClassValue")));
        classValStepSize.setText(String.valueOf(paramMap.get("classValStepSize")));
        classLevels.setText(String.valueOf(paramMap.get("classLevels")));

        final Double minVal = (Double) paramMap.get("minClassValue");
        final Double stepSize = (Double) paramMap.get("classValStepSize");
        final Integer levels = (Integer) paramMap.get("classLevels");
        if(minVal != null && stepSize != null && levels != null) {
            final double maxClassVal = BaseClassifier.getMaxValue(minVal, stepSize, levels);
            maxClassValue.setText(String.valueOf(maxClassVal));
        }

        Boolean trainOnRastersVal = (Boolean) paramMap.get("trainOnRaster");
        boolean trainOnRasters = trainOnRastersVal != null && trainOnRastersVal;
        trainOnRasterBtn.setSelected(trainOnRasters);

        String labelSource = (String) paramMap.get("labelSource");
        if (labelSource == null || labelSource.equals(BaseClassifier.VectorNodeNameLabelSource)) {
            labelSourceVectorName.setSelected(true);
        }

        boolean doTraining = trainBtn.isSelected();
        enableTraining(doTraining);
        enableTrainOnRaster(doTraining, trainOnRasters);
        enablePowerSet();

        paramMap.put("bandsOrVectors", null);

        OperatorUIUtils.initParamList(featureBandNames, getFeatures());
    }

    @Override
    public UIValidation validateParameters() {

        if (!loadBtn.isSelected()) {
            if (DialogUtils.contains(classifierNameComboBox, newClassifierNameField.getText())) {
                //  return new UIValidation(UIValidation.State.ERROR, "Name already in use. Please select a unique classifier name");
            }
        }

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        //System.out.println("BaseClassifierOpUI.updateParameters: called");
        paramMap.put("numTrainSamples", Integer.parseInt(numTrainSamples.getText()));
        paramMap.put("evaluateClassifier", evaluateClassifier.isSelected());
        paramMap.put("evaluateFeaturePowerSet", evaluateFeaturePowerSet.isSelected());

        if(evaluateClassifier.isSelected() && evaluateFeaturePowerSet.isSelected()) {
            if(!minPowerSetSize.getText().isEmpty()) {
                paramMap.put("minPowerSetSize", Integer.parseInt(minPowerSetSize.getText()));
            }
            if(!maxPowerSetSize.getText().isEmpty()) {
                paramMap.put("maxPowerSetSize", Integer.parseInt(maxPowerSetSize.getText()));
            }
        }

        paramMap.put("doLoadClassifier", loadBtn.isSelected());
        paramMap.put("doClassValQuantization", doClassValQuantization.isSelected());
        paramMap.put("minClassValue", Double.parseDouble(minClassValue.getText()));
        paramMap.put("classValStepSize", Double.parseDouble(classValStepSize.getText()));
        paramMap.put("classLevels", Integer.parseInt(classLevels.getText()));
        paramMap.put("trainOnRaster", trainOnRasterBtn.isSelected());

        String classifierName = loadBtn.isSelected() ?
                (String) classifierNameComboBox.getSelectedItem() :
                newClassifierNameField.getText();
        paramMap.put("savedClassifierName", classifierName);

        if (labelSourceAttribute.isSelected()) {
            paramMap.put("labelSource", labelSourceAttribute.getText());
        } else {
            paramMap.put("labelSource", BaseClassifier.VectorNodeNameLabelSource);
        }

        updateParamList(trainingBands, paramMap, "trainingBands");
        //dumpSelectedValues("trainingBands", trainingBands);

        updateParamList(trainingVectors, paramMap, "trainingVectors");
        //dumpSelectedValues("trainingVectors", trainingVectors);

        updateParamList(featureBandNames, paramMap, "featureBands");
        //dumpSelectedValues("features", featureBandNames);
    }

    private static void dumpSelectedValues(final String name, final JList<String> paramList) {

        SystemUtils.LOG.info(name + " selected values:");
        final List<String> selectedValues = paramList.getSelectedValuesList();
        for (Object selectedValue : selectedValues) {
            SystemUtils.LOG.info(' ' + (String) selectedValue);
        }
    }

    private static void updateParamList(final JList paramList, final Map<String, Object> paramMap, final String paramName) {
        final List selectedValues = paramList.getSelectedValuesList();
        final String names[] = new String[selectedValues.size()];
        int i = 0;
        for (Object selectedValue : selectedValues) {
            names[i++] = (String) selectedValue;
        }
        if (names.length == 0 && paramMap.get(paramName) != null) {
            // Remove previously selected values, so that nothing is selected
            paramMap.remove(paramName);
        } else {
            // Replace previously selected values
            paramMap.put(paramName, names);
        }
    }

    protected JPanel createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        classifierPanel = createClassifierPanel();
        gbc.gridy++;
        contentPane.add(classifierPanel, gbc);

        rasterPanel = createRasterPanel();
        gbc.gridy++;
        contentPane.add(rasterPanel, gbc);

        vectorPanel = createVectorPanel();
        contentPane.add(vectorPanel, gbc);

        featurePanel = createFeaturePanel();
        gbc.gridy++;
        contentPane.add(featurePanel, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        enablePowerSet();

        return contentPane;
    }

    private JPanel createClassifierPanel() {
        final JPanel classifierPanel = new JPanel();
        classifierPanel.setLayout(new GridBagLayout());
        classifierPanel.setBorder(new TitledBorder("Classifier"));
        classifiergbc = DialogUtils.createGridBagConstraints();

        final ButtonGroup group1 = new ButtonGroup();
        group1.add(trainBtn);
        group1.add(loadBtn);

        classifierPanel.add(trainBtn, classifiergbc);
        classifiergbc.gridx = 1;
        classifierPanel.add(newClassifierNameField, classifiergbc);
        classifiergbc.gridx = 0;

        classifiergbc.gridx = 0;
        classifiergbc.gridy++;
        classifierPanel.add(loadBtn, classifiergbc);
        classifiergbc.gridx = 1;
        classifierPanel.add(classifierNameComboBox, classifiergbc);
        classifiergbc.gridx = 2;
        classifierPanel.add(deleteClassiferBtn, classifiergbc);

        final ButtonGroup group2 = new ButtonGroup();
        group2.add(trainOnRasterBtn);
        group2.add(trainOnVectorsBtn);

        final JPanel radioPanel = new JPanel(new FlowLayout());
        radioPanel.add(trainOnRasterBtn);
        radioPanel.add(trainOnVectorsBtn);

        classifiergbc.gridy++;
        classifiergbc.gridx = 1;
        classifierPanel.add(radioPanel, classifiergbc);
        classifiergbc.gridx = 0;

        classifiergbc.gridy++;
        DialogUtils.addComponent(classifierPanel, classifiergbc, "Evaluate classifier", evaluateClassifier);
        classifiergbc.gridy++;
        DialogUtils.addComponent(classifierPanel, classifiergbc, "Evaluate Feature Power Set", evaluateFeaturePowerSet);
        classifiergbc.gridy++;

        final JPanel powerSetPanel = new JPanel(new FlowLayout());
        minPowerSetSize.setColumns(4);
        maxPowerSetSize.setColumns(4);
        powerSetPanel.add(new JLabel("Min Power Set Size:"));
        powerSetPanel.add(minPowerSetSize);
        powerSetPanel.add(new JLabel("Max Power Set Size:"));
        powerSetPanel.add(maxPowerSetSize);

        classifiergbc.gridy++;
        classifiergbc.gridx = 1;
        classifierPanel.add(powerSetPanel, classifiergbc);
        classifiergbc.gridx = 0;

        classifiergbc.gridy++;
        DialogUtils.addComponent(classifierPanel, classifiergbc, "Number of training samples", numTrainSamples);

        DialogUtils.fillPanel(classifierPanel, classifiergbc);

        return classifierPanel;
    }

    private JPanel createRasterPanel() {
        final JPanel rasterPanel = new JPanel();
        rasterPanel.setLayout(new GridBagLayout());
        rasterPanel.setBorder(new TitledBorder("Raster Training"));
        GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(rasterPanel, gbc, "Quantize class value", doClassValQuantization);

        gbc.gridy++;
        DialogUtils.addComponent(rasterPanel, gbc, "Min class value", minClassValue);

        gbc.gridy++;
        DialogUtils.addComponent(rasterPanel, gbc, "Class value step size", classValStepSize);

        gbc.gridy++;
        DialogUtils.addComponent(rasterPanel, gbc, "Class levels", classLevels);

        gbc.gridy++;
        DialogUtils.addComponent(rasterPanel, gbc, "Max class value", maxClassValue);

        gbc.gridy++;
        DialogUtils.addComponent(rasterPanel, gbc, "Training band:", new JScrollPane(trainingBands));

        DialogUtils.fillPanel(rasterPanel, gbc);

        return rasterPanel;
    }

    private JPanel createVectorPanel() {
        final JPanel vectorPanel = new JPanel();
        vectorPanel.setLayout(new GridBagLayout());
        vectorPanel.setBorder(new TitledBorder("Vector Training"));
        GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(vectorPanel, gbc, "Training vectors:     ", new JScrollPane(trainingVectors));
        gbc.gridy++;
        gbc.gridx = 0;
        //vectorPanel.add(new JLabel("Labels:"), gbc);

        //final ButtonGroup group3 = new ButtonGroup();
        //group3.add(labelSourceVectorName);
        //group3.add(labelSourceAttribute);

        //JPanel radioPanel = new JPanel(new FlowLayout());
        //radioPanel.add(labelSourceVectorName);
        //radioPanel.add(labelSourceAttribute);

        gbc.gridx = 1;
        //vectorPanel.add(radioPanel, gbc);

        DialogUtils.fillPanel(vectorPanel, gbc);

        return vectorPanel;
    }

    private JPanel createFeaturePanel() {
        final JPanel featurePanel = new JPanel();
        featurePanel.setBorder(new TitledBorder("Feature Selection"));
        featurePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(featurePanel, gbc, "Feature bands:       ", new JScrollPane(featureBandNames));

        DialogUtils.fillPanel(featurePanel, gbc);

        return featurePanel;
    }

    private String[] getPolygons() {
        // Get polygons from the first product which is assumed to be maskProduct in BaseClassifier
        final ArrayList<String> geometryNames = new ArrayList<>(5);
        if (sourceProducts != null) {
            if (sourceProducts.length > 1) {
                final ProductNodeGroup<VectorDataNode> vectorDataNodes = sourceProducts[0].getVectorDataGroup();
                for(int i=0; i< vectorDataNodes.getNodeCount(); ++i) {
                    VectorDataNode node = vectorDataNodes.get(i);
                    if(!node.getFeatureCollection().isEmpty()) {
                        geometryNames.add(node.getName() + "::" + sourceProducts[0].getName());
                    }
                }
            } else {
                final ProductNodeGroup<VectorDataNode> vectorDataNodes = sourceProducts[0].getVectorDataGroup();
                for(int i=0; i< vectorDataNodes.getNodeCount(); ++i) {
                    VectorDataNode node = vectorDataNodes.get(i);
                    if(!node.getFeatureCollection().isEmpty()) {
                        geometryNames.add(node.getName());
                    }
                }
            }
        }
        return geometryNames.toArray(new String[geometryNames.size()]);
    }

    private String[] getTrainingBands() {
        final ArrayList<String> bandNames = new ArrayList<>(5);
        if (sourceProducts != null) {
            if (sourceProducts.length > 1) {
                for (String name : sourceProducts[0].getBandNames()) {
                    bandNames.add(name + "::" + sourceProducts[0].getName());
                }
            } else {
                bandNames.addAll(Arrays.asList(sourceProducts[0].getBandNames()));
            }
        }
        return bandNames.toArray(new String[bandNames.size()]);
    }

    private String[] getFeatures() {
        final ArrayList<String> featureNames = new ArrayList<>(5);

        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                for (String name : prod.getBandNames()) {
                    if (BaseClassifier.excludeBand(name))
                        continue;
                    if (sourceProducts.length > 1) {
                        featureNames.add(name + "::" + prod.getName());
                    } else {
                        featureNames.add(name);
                    }
                }
            }
        }

        return featureNames.toArray(new String[featureNames.size()]);
    }
}
