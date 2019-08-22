package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReaderExposedParams;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

public class ProductAdvancedDialog {

    private final ModalDialog dialog;
    private final Window parent;

    private final JList bandList = new JList();

    private final JTextField bandListTextField = new JTextField("");
    private final JTextField regionX = new JTextField("");
    private final JTextField regionY = new JTextField("");
    private final JTextField width = new JTextField("");
    private final JTextField height = new JTextField("");
    private final JTextField subSamplingX = new JTextField("");
    private final JTextField subSamplingY = new JTextField("");
    private final JTextField onlyMasksTextField = new JTextField("");
    private final JTextField latitudeNorth = new JTextField("");
    private final JTextField longitudeWest = new JTextField("");
    private final JTextField latitudeSouth = new JTextField("");
    private final JTextField longitudeEast = new JTextField("");

    private final JCheckBox copyMetadata = new JCheckBox("Copy Metadata", true);
    private final JCheckBox copyMasks = new JCheckBox("Copy All Masks", true);
    private final JCheckBox onlyMasks = new JCheckBox("Only Masks", false);
    private final JCheckBox noneMask = new JCheckBox("Do not copy Masks", false);

    private final JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private final JRadioButton geoCoordRadio = new JRadioButton("Geographic Coordinates");

    private final JPanel pixelPanel = new JPanel(new GridBagLayout());
    private final JPanel geoPanel = new JPanel(new GridBagLayout());

    private ProductReaderExposedParams readerExposedParams;

    public ProductAdvancedDialog(Container parent, String title, File file) {
        this.parent = (Window) parent;
        this.dialog = new ModalDialog(this.parent, title, ModalDialog.ID_OK_CANCEL, "");

        final List<ProductOpener.PluginEntry> intendedPlugIns = ProductOpener.getPluginsForFile(file, DecodeQualification.INTENDED);
        List<ProductOpener.PluginEntry> suitablePlugIns = new ArrayList<>();
        if (intendedPlugIns.size() == 0) { // check for suitable readers only if no intended reader was found
            suitablePlugIns.addAll(ProductOpener.getPluginsForFile(file, DecodeQualification.SUITABLE));
        }

        String fileFormatName;
        boolean showUI = true;
        if (intendedPlugIns.isEmpty() && suitablePlugIns.isEmpty()) {
            Dialogs.showError(Bundle.LBL_NoReaderFoundText() + String.format("%nFile '%s' can not be opened.", file));
        } else if (intendedPlugIns.size() == 1) {
            ProductOpener.PluginEntry entry = intendedPlugIns.get(0);
            fileFormatName = entry.plugin.getFormatNames()[0];
             this.readerExposedParams = entry.plugin.createReaderInstance().getExposedParams();
        } else if (intendedPlugIns.size() == 0 && suitablePlugIns.size() == 1) {
            ProductOpener.PluginEntry entry = suitablePlugIns.get(0);
            fileFormatName = entry.plugin.getFormatNames()[0];
            this.readerExposedParams = entry.plugin.createReaderInstance().getExposedParams();
        } else {
            Collections.sort(intendedPlugIns);
            Collections.sort(suitablePlugIns);
            // ask user to select a desired reader plugin
            fileFormatName = ProductOpener.getUserSelection(intendedPlugIns, suitablePlugIns);
            if (fileFormatName == null) { // User clicked cancel
                showUI = false;
            }
            else
            {
                // TODO get user selected plugin
            }
        }

        if (showUI) {
            if (this.readerExposedParams != null && this.readerExposedParams.getBandNames() != null && this.readerExposedParams.getBandNames().size() > 0) {
                // set the possible selectable values
                this.bandList.setListData(this.readerExposedParams.getBandNames().toArray());
            }
            createUI();
        }
    }

    public void createUI() {
        dialog.setContent(createPanel());
        dialog.show();
    }

    private JComponent createPanel() {
        JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        //TODO: if there are no valueSet for bands add a text field
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(copyMetadata, gbc);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(copyMasks);
        buttonGroup.add(onlyMasks);
        buttonGroup.add(noneMask);

        copyMasks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (copyMasks.isSelected()) {
                    onlyMasksTextField.setEnabled(false);
                }
            }
        });
        onlyMasks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onlyMasks.isSelected()) {
                    onlyMasksTextField.setEnabled(true);
                }
            }
        });
        noneMask.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (noneMask.isSelected()) {
                    onlyMasksTextField.setEnabled(false);
                }
            }
        });

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(copyMasks, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(onlyMasks, gbc);
        gbc.gridx++;
        onlyMasksTextField.setEnabled(false);
        contentPane.add(onlyMasksTextField, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(noneMask, gbc);

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
}
