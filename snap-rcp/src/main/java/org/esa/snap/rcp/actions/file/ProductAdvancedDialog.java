package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.MetadataInspector;
import org.esa.snap.core.dataio.ProductReaderExposedParams;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductAdvancedDialog extends ModalDialog {

    private final JList bandList = new JList();
    private final JList maskList = new JList();

    private final JTextField bandListTextField = new JTextField("");
    private final JTextField regionX = new JTextField("");
    private final JTextField regionY = new JTextField("");
    private final JTextField width = new JTextField("");
    private final JTextField height = new JTextField("");
    private final JTextField subSamplingX = new JTextField("");
    private final JTextField subSamplingY = new JTextField("");
    private final JTextField copyMasksTextField = new JTextField("");
    private final JTextField latitudeNorth = new JTextField("");
    private final JTextField longitudeWest = new JTextField("");
    private final JTextField latitudeSouth = new JTextField("");
    private final JTextField longitudeEast = new JTextField("");

    private JCheckBox copyMetadata = new JCheckBox("Copy Metadata", true);
    private final JCheckBox copyMasks = new JCheckBox("Copy Masks", true);

    private final JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private final JRadioButton geoCoordRadio = new JRadioButton("Geographic Coordinates");

    private final JPanel pixelPanel = new JPanel(new GridBagLayout());
    private final JPanel geoPanel = new JPanel(new GridBagLayout());

    private ProductReaderExposedParams readerExposedParams;

    private MetadataInspector.Metadata readerInspectorExposeParameters;

    private ProductSubsetDef productSubsetDef = null;

    private ProductReaderPlugIn plugin;

    public ProductAdvancedDialog(Window window, String title, File file) throws Exception {
        super(window, title, ID_OK | ID_CANCEL | ID_HELP, "advancedDialog");
        final List<ProductOpener.PluginEntry> intendedPlugIns = ProductOpener.getPluginsForFile(file, DecodeQualification.INTENDED);
        List<ProductOpener.PluginEntry> suitablePlugIns = new ArrayList<>();
        if (intendedPlugIns.isEmpty()) { // check for suitable readers only if no intended reader was found
            suitablePlugIns.addAll(ProductOpener.getPluginsForFile(file, DecodeQualification.SUITABLE));
        }

        String fileFormatName;
        boolean showUI = true;
        if (intendedPlugIns.isEmpty() && suitablePlugIns.isEmpty()) {
            showUI = false;
        } else if (intendedPlugIns.size() == 1) {
            ProductOpener.PluginEntry entry = intendedPlugIns.get(0);
            plugin = entry.plugin;
        } else if (intendedPlugIns.isEmpty() && suitablePlugIns.size() == 1) {
            ProductOpener.PluginEntry entry = suitablePlugIns.get(0);
            plugin = entry.plugin;
        } else {
            Collections.sort(intendedPlugIns);
            Collections.sort(suitablePlugIns);
            // ask user to select a desired reader plugin
            fileFormatName = ProductOpener.getUserSelection(intendedPlugIns, suitablePlugIns);
            if (fileFormatName == null) { // User clicked cancel
                showUI = false;
            } else {
                if (!suitablePlugIns.isEmpty() && suitablePlugIns.stream()
                        .anyMatch(entry -> entry.plugin.getFormatNames()[0].equals(fileFormatName))) {
                    ProductOpener.PluginEntry entry = suitablePlugIns.stream()
                            .filter(entry1 -> entry1.plugin.getFormatNames()[0].equals(fileFormatName))
                            .findAny()
                            .orElse(null);
                    plugin = entry.plugin;
                } else {
                    ProductOpener.PluginEntry entry = intendedPlugIns.stream()
                            .filter(entry1 -> entry1.plugin.getFormatNames()[0].equals(fileFormatName))
                            .findAny()
                            .orElse(null);
                    plugin = entry.plugin;
                }
            }
        }
        if (plugin != null) {
            this.readerExposedParams = plugin.createReaderInstance().getExposedParams();
            MetadataInspector metadatainsp = plugin.createReaderInstance().getMetadataInspector();
            if (metadatainsp != null) {
                Path input = convertInputToPath(file);
                readerInspectorExposeParameters = metadatainsp.getMetadata(input);
            }
        }
        //if the user does not support Advanced option action
        if (showUI && this.readerExposedParams == null && this.readerInspectorExposeParameters == null) {
            int confirm = JOptionPane.showConfirmDialog(null, "The reader does not support Open with advanced options!\nDo you want to open the product normally?", null, JOptionPane.YES_NO_OPTION);
            //if the user want to open the product normally the Advanced Options window will not be displayed
            if (confirm == JOptionPane.YES_OPTION) {
                showUI = false;
            } else {//if the user choose not to open the product normally the Advanced Option window components are removed
                getJDialog().removeAll();
                showUI = false;
            }

        }
        if (showUI) {
            if (this.readerInspectorExposeParameters == null) {
                if (this.readerExposedParams != null && this.readerExposedParams.getBandNames() != null && this.readerExposedParams.getBandNames().isEmpty()) {
                    // set the possible selectable values
                    this.bandList.setListData(this.readerExposedParams.getBandNames().toArray());
                }
                if (this.readerExposedParams != null && this.readerExposedParams.getMaskNames() != null && this.readerExposedParams.getMaskNames().isEmpty()) {
                    // set the possible selectable values
                    this.maskList.setListData(this.readerExposedParams.getMaskNames().toArray());
                }
                if (this.readerExposedParams != null && !this.readerExposedParams.isHasMasks()) {
                    copyMasks.setSelected(false);
                }
            } else {
                if (this.readerInspectorExposeParameters.getBandList() != null && !this.readerInspectorExposeParameters.getBandList().isEmpty()) {
                    // set the possible selectable values
                    this.bandList.setListData(this.readerInspectorExposeParameters.getBandList().toArray());
                }
                if (this.readerInspectorExposeParameters.isHasMasks() && this.readerInspectorExposeParameters.getMaskList() != null && !this.readerInspectorExposeParameters.getMaskList().isEmpty()) {
                    // set the possible selectable values
                    this.maskList.setListData(this.readerInspectorExposeParameters.getMaskList().toArray());
                }
                if (!this.readerInspectorExposeParameters.isHasMasks()) {
                    copyMasks.setSelected(false);
                }
            }
            createUI();
        }
    }

    public void createUI() throws Exception {
        setContent(createPanel());
        if (show() == ID_OK) {
            updateSubsetDefNodeNameList();
        }
    }

    @Override
    protected void onCancel() {
        getJDialog().removeAll();
        super.onCancel();
    }

    private void updateSubsetDefNodeNameList() {
        productSubsetDef = new ProductSubsetDef();
        //if the user specify the bands that want to be added in the product add only them, else mark the fact that the product must have all the bands
        if (!bandList.isSelectionEmpty()) {
            productSubsetDef.addNodeNames((String[]) bandList.getSelectedValuesList().stream().toArray(String[]::new));
        } else if (!bandListTextField.getText().replaceAll(" ", "").equals("")) {
            if (bandListTextField.getText().contains(",")) {
                //remove all blank spaces
                bandListTextField.setText(bandListTextField.getText().replaceAll(" ", ""));
                //if there are blank values remove them
                bandListTextField.setText(bandListTextField.getText().replaceAll(",,", ","));
                //split the content by comma
                String[] bandAddedValues = bandListTextField.getText().split(",");
                //add all values into productSubsetDef
                productSubsetDef.addNodeNames(bandAddedValues);
            }
        } else {
            productSubsetDef.addNodeName("allBands");
        }

        //if the user specify the masks that want to be added in the product add only them, else mark the fact that the product must have all the masks
        if (!maskList.isSelectionEmpty()) {
            productSubsetDef.addNodeNames((String[]) maskList.getSelectedValuesList().stream().toArray(String[]::new));
        } else if (!copyMasksTextField.getText().equals("")) {
            if (copyMasksTextField.getText().contains(",")) {
                //remove all blank spaces
                copyMasksTextField.setText(copyMasksTextField.getText().replaceAll(" ", ""));
                //if there are blank values remove them
                copyMasksTextField.setText(copyMasksTextField.getText().replaceAll(",,", ","));
                //split the content by comma
                String[] maskAddedValues = copyMasksTextField.getText().split(",");
                //add all values into productSubsetDef
                productSubsetDef.addNodeNames(maskAddedValues);
            }
        } else if (copyMasks.isSelected()) {
            productSubsetDef.addNodeName("allMasks");
        }
        if (!copyMetadata.isSelected()) {
            productSubsetDef.setIgnoreMetadata(true);
        }
    }

    private JComponent createPanel() {

        JPanel contentPane = new JPanel(new GridBagLayout());
        JScrollPane scrollPaneMask = new JScrollPane(maskList);
        final GridBagConstraints gbc = createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        if (bandList.getModel().getSize() > 0) {
            contentPane.add(new JScrollPane(bandList), gbc);
        } else {
            contentPane.add(bandListTextField, gbc);
        }


        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(copyMetadata, gbc);

        if ((this.readerExposedParams != null && this.readerExposedParams.isHasMasks())
                || (this.readerInspectorExposeParameters != null
                && this.readerInspectorExposeParameters.isHasMasks())) {
            copyMasks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (copyMasks.isSelected()) {
                        if (!scrollPaneMask.isVisible()) {
                            scrollPaneMask.setVisible(true);
                        }
                        if (!copyMasksTextField.isVisible()) {
                            copyMasksTextField.setVisible(true);
                        }
                    } else {
                        if (scrollPaneMask.isVisible()) {
                            maskList.clearSelection();
                            scrollPaneMask.setVisible(false);
                        }
                        if (copyMasksTextField.isVisible()) {
                            copyMasksTextField.setText(null);
                            copyMasksTextField.setVisible(false);
                        }
                    }
                }
            });
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy++;
            contentPane.add(copyMasks, gbc);
            gbc.gridx++;
            if (maskList.getModel().getSize() > 0) {
                contentPane.add(scrollPaneMask, gbc);
            } else {
                contentPane.add(copyMasksTextField, gbc);
            }
        }

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(pixelCoordRadio, gbc);
        gbc.gridx = 1;
        contentPane.add(geoCoordRadio, gbc);

        pixelCoordRadio.setSelected(true);
        pixelCoordRadio.setActionCommand("pixelCoordRadio");
        geoCoordRadio.setActionCommand("geoCoordRadio");
        ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        RadioListener myListener = new RadioListener();
        pixelCoordRadio.addActionListener(myListener);
        geoCoordRadio.addActionListener(myListener);

        final GridBagConstraints pixgbc = createGridBagConstraints();
        pixgbc.gridwidth = 1;
        pixgbc.fill = GridBagConstraints.BOTH;
        addComponent(pixelPanel, pixgbc, "Scene X:", regionX, 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "SceneY:", regionY, 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Scene width:", width, 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Scene height:", height, 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Scene Step X:", subSamplingX, 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Scene Step Y:", subSamplingY, 0);
        pixelPanel.add(new JPanel(), pixgbc);

        final GridBagConstraints geobc = createGridBagConstraints();
        geobc.gridwidth = 1;
        geobc.fill = GridBagConstraints.BOTH;
        addComponent(geoPanel, geobc, "North latitude bound:", latitudeNorth, 0);
        geobc.gridy++;
        addComponent(geoPanel, geobc, "West longitude bound:", longitudeWest, 0);
        geobc.gridy++;
        addComponent(geoPanel, geobc, "South latitude bound:", latitudeSouth, 0);
        geobc.gridy++;
        addComponent(geoPanel, geobc, "East longitude bound:", longitudeEast, 0);
        geoPanel.add(new JPanel(), geobc);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        contentPane.add(pixelPanel, gbc);
        geoPanel.setVisible(false);
        contentPane.add(geoPanel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        contentPane.add(new JPanel(), gbc);
        return contentPane;
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 3, 0, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.top = 1;
        gbc.insets.bottom = 1;
        gbc.insets.right = 1;
        gbc.insets.left = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }

    public static JLabel addComponent(JPanel contentPane, GridBagConstraints gbc, String text, JComponent component, int pos) {
        gbc.gridx = pos;
        gbc.weightx = 0.5;
        final JLabel label = new JLabel(text);
        contentPane.add(label, gbc);
        gbc.gridx = pos + 1;
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
            } else {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
            }
        }
    }

    public ProductSubsetDef getProductSubsetDef() {
        return productSubsetDef;
    }

    public ProductReaderPlugIn getPlugin() {
        return plugin;
    }

    public static Path convertInputToPath(Object input) {
        if (input == null) {
            throw new NullPointerException();
        } else if (input instanceof File) {
            return ((File) input).toPath();
        } else if (input instanceof Path) {
            return (Path) input;
        } else if (input instanceof String) {
            return Paths.get((String) input);
        } else {
            throw new IllegalArgumentException("Unknown input '" + input + "'.");
        }
    }
}
