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
package org.esa.snap.rcp.colormanip;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;

import javax.swing.*;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.NamingConvention;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.BandChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.esa.snap.core.util.NamingConventionSnap.COLOR_LOWER_CASE;


/**
 * The GUI for the color manipulation tool window.
 *
 * @author Brockmann Consult
 * @author Daniel Knowles (NASA)
 * @author Bing Yang (NASA)
 * @version $Revision$ $Date$
 */
// NOV 2019 - Knowles / Yang
//          - Added color scheme logic which enables setting of the parameters based on the band name or desired color scheme.
// DEC 2019 - Knowles / Yang
//          - Added capability to export color palette in cpt and pal formats.
// JAN 2020 - Yang
//          - Fixed cpd import button
// JAN 2020 - Knowles
//          - Added installers for the xml files in the color_schemes auxdata directory
//          - Minor color scheme revisions
// FEB 2020 - Knowles
//          - Wrapped this tool in a JScrollPane
//          - Changed arrangement of tool buttons to single column
// MAR 2020 - Fixed bug where More Options would not update when changing between 3band, 1band, and 1DiscreteBand forms
//          - Added installation of rgb_profiles auxdata

@NbBundle.Messages({
        "CTL_ColorManipulationForm_TitlePrefix=" + ColorManipulationDefaults.TOOLNAME_COLOR_MANIPULATION
})
class ColorManipulationFormImpl implements SelectionSupport.Handler<ProductSceneView>, ColorManipulationForm {


    private final static String PREFERENCES_KEY_IO_DIR = "snap.color_palettes.dir";

    private final static String FILE_EXTENSION_CPD = "cpd";
    private final static String FILE_EXTENSION_PAL = "pal";
    private final static String FILE_EXTENSION_CPT = "cpt";
    private final static String FILE_EXTENSION_RGB_PROFILES = "rgb";

    private AbstractButton resetButton;
    private AbstractButton multiApplyButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;

    private final TopComponent toolView;
    private final ColorFormModel formModel;
    private Band[] bandsToBeModified;
    private SnapFileFilter snapFileFilter;
    private SnapFileFilter palFileFilter;
    private SnapFileFilter cptFileFilter;
    private final ProductNodeListener productNodeListener;
    private boolean colorPalettesAuxFilesInstalled;
    private boolean colorSchemesAuxFilesInstalled;
    private boolean rgbProfilesFilesInstalled;
    private JPanel contentPanel;
    private JPanel innerContentPanel;
    private ColorManipulationChildForm childForm;
    private ColorManipulationChildForm continuous1BandSwitcherForm;
    private ColorManipulationChildForm discrete1BandTabularForm;
    private ColorManipulationChildForm continuous3BandGraphicalForm;
    private JPanel toolButtonsPanel;
    private AbstractButton helpButton;
    private Path ioDir;
    private JPanel editorPanel;
    private MoreOptionsPane moreOptionsPane;
    private SceneViewImageInfoChangeListener sceneViewChangeListener;
    private String titlePrefix;
    private ColorManipulationChildForm emptyForm;
    private BrightnessContrastPanel brightnessContrastPanel;
    private JTabbedPane tabbedPane;

    ColorManipulationFormImpl(TopComponent colorManipulationToolView, ColorFormModel formModel) {
        Assert.notNull(colorManipulationToolView);
        Assert.notNull(formModel);
        this.toolView = colorManipulationToolView;
        this.formModel = formModel;
        productNodeListener = new ColorManipulationPNL();
        sceneViewChangeListener = new SceneViewImageInfoChangeListener();
        titlePrefix = this.formModel.getTitlePrefix();
        emptyForm = new EmptyImageInfoForm(this);
    }

    @Override
    public ColorFormModel getFormModel() {
        return formModel;
    }

    @Override
    public JPanel getContentPanel() {
        if (contentPanel == null) {
            initContentPanel();
        }
        if (!colorPalettesAuxFilesInstalled) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new InstallColorPalettesAuxFiles());
        }

        if (!colorSchemesAuxFilesInstalled) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new InstallColorSchemesAuxFiles());
        }

        if (!rgbProfilesFilesInstalled) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new InstallRGBAuxFiles());
        }

        return contentPanel;
    }

    public void revalidateToolViewPaneControl() {
        getToolViewPaneControl().invalidate();
        getToolViewPaneControl().validate();
        getToolViewPaneControl().repaint();
        updateToolButtons();
    }

    private static AbstractButton createButton(final String iconPath) {
        return ImageInfoEditorSupport.createButton(iconPath);
    }

    private Component getToolViewPaneControl() {
        return toolView;
    }

    private void setProductSceneView(final ProductSceneView productSceneView) {
        ProductSceneView productSceneViewOld = getFormModel().getProductSceneView();
        if (productSceneViewOld != null) {
            Product product = productSceneViewOld.getProduct();
            if (product != null) {
                // Product may already been gone.
                product.removeProductNodeListener(productNodeListener);
            }
            productSceneViewOld.removePropertyChangeListener(sceneViewChangeListener);
        }
        getFormModel().setProductSceneView(productSceneView);
        if (getFormModel().isValid()) {
            getFormModel().getProductSceneView().getProduct().addProductNodeListener(productNodeListener);
            getFormModel().getProductSceneView().addPropertyChangeListener(sceneViewChangeListener);

            if (getFormModel().isContinuous3BandImage() || getFormModel().isDiscrete1BandImage()) {
                getFormModel().setModifiedImageInfo(getFormModel().getOriginalImageInfo());
            } else {
                PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

                if (productSceneView.getImageInfo().getColorSchemeInfo() == null) {
                    ColorManipulationDefaults.debug("In ColorManipulationFormImpl: colorSchemeInfo =null (setToDefault)");
                    ColorSchemeUtils.setImageInfoToDefaultColor(configuration, createDefaultImageInfo(), productSceneView);
                } else {
                    ColorManipulationDefaults.debug("In ColorManipulationFormImpl: colorSchemeInfo =" + productSceneView.getImageInfo().getColorSchemeInfo().toString());
                }

                ColorManipulationDefaults.debug("In ColorManipulationFormImpl: about to do setModifiedImageInfo ");
                getFormModel().setModifiedImageInfo(getFormModel().getProductSceneView().getImageInfo());
                ColorManipulationDefaults.debug("In ColorManipulationFormImpl: finished setModifiedImageInfo ");
            }
        }

        installChildForm();

        updateTitle();
        updateToolButtons();

        ColorManipulationDefaults.debug("In ColorManipulationFormImpl: about to do updateMultiApplyState ");
        updateMultiApplyState();
        ColorManipulationDefaults.debug("In ColorManipulationFormImpl: finished updateMultiApplyState ");

        if (getFormModel().isValid()) {
            ColorManipulationDefaults.debug("In ColorManipulationFormImpl: apply changes");
            applyChanges();
        }
    }

    private void installChildForm() {
        final ColorManipulationChildForm oldForm = getChildForm();
        ColorManipulationChildForm newForm = emptyForm;

        if (getFormModel().isValid()) {
            if (getFormModel().isContinuous3BandImage()) {
                if (oldForm instanceof Continuous3BandGraphicalForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous3BandGraphicalForm();
                }
            } else if (getFormModel().isContinuous1BandImage()) {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            } else if (getFormModel().isDiscrete1BandImage()) {
                if (oldForm instanceof Discrete1BandTabularForm) {
                    newForm = oldForm;
                } else {
                    newForm = getDiscrete1BandTabularForm();
                }
            } else {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            }
        }

        if (newForm != oldForm) {
            setChildForm(newForm);
            updateMoreOptions();

            boolean installAllButtons = !(newForm instanceof Continuous1BandGraphicalForm || newForm instanceof Continuous3BandGraphicalForm);

            installToolButtons(installAllButtons);
            installMoreOptions();

            editorPanel.removeAll();
            editorPanel.add(getChildForm().getContentPanel(), BorderLayout.CENTER);
            if (!(getChildForm() instanceof EmptyImageInfoForm)) {
                editorPanel.add(moreOptionsPane.getContentPanel(), BorderLayout.SOUTH);
            }
            revalidateToolViewPaneControl();

            if (oldForm != null) {
                oldForm.handleFormHidden(getFormModel());
            }
            getChildForm().handleFormShown(getFormModel());
        } else {
            getChildForm().updateFormModel(getFormModel());
        }
    }

    private void updateTitle() {
        String titlePostfix = "";
        if (getFormModel().isValid()) {
            titlePostfix = " - " + getFormModel().getModelName();
        }
        toolView.setDisplayName(titlePrefix + titlePostfix);
    }

    private void updateToolButtons() {
        resetButton.setEnabled(getFormModel().isValid());
        importButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
        exportButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
    }

    private ColorManipulationChildForm getContinuous3BandGraphicalForm() {
        if (continuous3BandGraphicalForm == null) {
            continuous3BandGraphicalForm = new Continuous3BandGraphicalForm(this);
        }
        return continuous3BandGraphicalForm;
    }

    private ColorManipulationChildForm getContinuous1BandSwitcherForm() {
        if (continuous1BandSwitcherForm == null) {
            continuous1BandSwitcherForm = new Continuous1BandSwitcherForm(this);
        }
        return continuous1BandSwitcherForm;
    }

    private ColorManipulationChildForm getDiscrete1BandTabularForm() {
        if (discrete1BandTabularForm == null) {
            discrete1BandTabularForm = new Discrete1BandTabularForm(this);
        }
        return discrete1BandTabularForm;
    }

    @Override
    public ActionListener wrapWithAutoApplyActionListener(final ActionListener actionListener) {
        return e -> {
            actionListener.actionPerformed(e);
            applyChanges();
        };
    }

    private void initContentPanel() {
        this.moreOptionsPane = new MoreOptionsPane(this, formModel.isMoreOptionsFormCollapsedOnInit());
        this.brightnessContrastPanel = new BrightnessContrastPanel(this);

        resetButton = createButton("org/esa/snap/rcp/icons/Undo24.gif");
        resetButton.setName("ResetButton");
        resetButton.setToolTipText("Reset to defaults"); /*I18N*/
        resetButton.addActionListener(wrapWithAutoApplyActionListener(e -> resetToDefaults()));

        multiApplyButton = createButton("org/esa/snap/rcp/icons/MultiAssignBands24.gif");
        multiApplyButton.setName("MultiApplyButton");
        multiApplyButton.setToolTipText("Apply to other bands"); /*I18N*/
        multiApplyButton.addActionListener(e -> applyMultipleColorPaletteDef());

        importButton = createButton("tango/22x22/actions/document-open.png");
        importButton.setName("ImportButton");
        importButton.setToolTipText("Import " + NamingConvention.COLOR_LOWER_CASE + " palette from text file."); /*I18N*/
        importButton.addActionListener(e -> {
            importColorPaletteDef();
            applyChanges();
        });
        importButton.setEnabled(true);

        exportButton = createButton("tango/22x22/actions/document-save-as.png");
        exportButton.setName("ExportButton");
        exportButton.setToolTipText("Save " + NamingConvention.COLOR_LOWER_CASE + " palette to text file."); /*I18N*/
        exportButton.addActionListener(e -> {
            exportColorPaletteDef();
            getChildForm().updateFormModel(getFormModel());
        });
        exportButton.setEnabled(true);

        helpButton = createButton("tango/22x22/apps/help-browser.png");
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");
        helpButton.addActionListener(e -> toolView.getHelpCtx().display());

        editorPanel = new JPanel(new BorderLayout(4, 4));
        editorPanel.add(moreOptionsPane.getContentPanel(), BorderLayout.SOUTH);

        toolButtonsPanel = GridBagUtils.createPanel();


        innerContentPanel = new JPanel(new BorderLayout(4, 4));
        innerContentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        innerContentPanel.setPreferredSize(new Dimension(280, 275));
        innerContentPanel.add(editorPanel, BorderLayout.CENTER);
        innerContentPanel.add(toolButtonsPanel, BorderLayout.EAST);

        JScrollPane jScrollPane = new JScrollPane(innerContentPanel);

        contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel.setPreferredSize(new Dimension(280, 200));
        contentPanel.add(jScrollPane, BorderLayout.CENTER);

        setProductSceneView(SnapApp.getDefault().getSelectedProductSceneView());

        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler(this);
    }

    private void updateMultiApplyState() {
        multiApplyButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
    }



    @Override
    public void installToolButtons(boolean installAllButtons) {

        toolButtonsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.insets.bottom = 2;
        gbc.gridwidth = 1;

        toolButtonsPanel.add(resetButton, gbc);
        gbc.gridy += 1;

        AbstractButton[] additionalButtons = getChildForm().getToolButtons();
        for (int i = 0; i < additionalButtons.length; i++) {
            AbstractButton button = additionalButtons[i];
            toolButtonsPanel.add(button, gbc);
                gbc.gridy += 1;
        }

        if (installAllButtons) {
            toolButtonsPanel.add(multiApplyButton, gbc);
            gbc.gridy += 1;

            toolButtonsPanel.add(importButton, gbc);
            gbc.gridy += 1;

            toolButtonsPanel.add(exportButton, gbc);
            gbc.gridy += 1;
        }


        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;

        toolButtonsPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.gridy += 1;
        toolButtonsPanel.add(helpButton, gbc);
    }




//    @Override
//    public void installToolButtons(boolean installAllButtons) {
//
//        boolean singleColumn = true;  // otherwise 2 columns
//
//        toolButtonsPanel.removeAll();
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.anchor = GridBagConstraints.CENTER;
//        gbc.fill = GridBagConstraints.NONE;
//        gbc.weightx = 1.0;
//        gbc.gridy = 0;
//        gbc.insets.bottom = 2;
//        gbc.gridwidth = 1;
//
//        toolButtonsPanel.add(resetButton, gbc);
//
//
//        if (installAllButtons) {
//            if (singleColumn) {
//                gbc.gridy += 1;
//            }
//
//            toolButtonsPanel.add(multiApplyButton, gbc);
//            gbc.gridy += 1;
//
//            toolButtonsPanel.add(importButton, gbc);
//            if (singleColumn) {
//                gbc.gridy += 1;
//            }
//
//            toolButtonsPanel.add(exportButton, gbc);
//            gbc.gridy += 1;
//        } else {
//            gbc.gridy += 1;
//        }
//
//        AbstractButton[] additionalButtons = getChildForm().getToolButtons();
//        for (int i = 0; i < additionalButtons.length; i++) {
//            AbstractButton button = additionalButtons[i];
//            toolButtonsPanel.add(button, gbc);
//            if (singleColumn || (i % 2 == 1)) {
//                gbc.gridy += 1;
//            }
//        }
//
//        gbc.gridy += 1;
//        gbc.fill = GridBagConstraints.VERTICAL;
//        gbc.weighty = 1.0;
//
//        gbc.gridwidth = (singleColumn) ? 1 : 2;
//        toolButtonsPanel.add(new JLabel(" "), gbc); // filler
//        gbc.fill = GridBagConstraints.NONE;
//        gbc.weighty = 0.0;
//        gbc.gridwidth = 1;
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        toolButtonsPanel.add(helpButton, gbc);
//    }


    @Override
    public void installMoreOptions() {
        final MoreOptionsForm moreOptionsForm = getChildForm().getMoreOptionsForm();
        if (moreOptionsForm != null) {
            moreOptionsForm.updateForm();

            if (this.tabbedPane == null) {
                this.tabbedPane = new JTabbedPane();
                this.tabbedPane.addTab("Histogram", moreOptionsForm.getContentPanel());
                this.tabbedPane.addTab("Brightness/Contrast", this.brightnessContrastPanel);
            }

            this.moreOptionsPane.setComponent(this.tabbedPane);
        }
    }


    public void updateMoreOptions() {
        final MoreOptionsForm moreOptionsForm = getChildForm().getMoreOptionsForm();
        if (moreOptionsForm != null) {
            if (this.tabbedPane != null) {
                this.tabbedPane.setComponentAt(0, moreOptionsForm.getContentPanel());
                this.tabbedPane.repaint();
            }
        }
    }

    @Override
    public void applyChanges() {
        ColorManipulationDefaults.debug("Applying changes in ColorManipulationFormImpl");
        updateMultiApplyState();
        if (getFormModel().isValid()) {
            try {
                getToolViewPaneControl().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (getFormModel().isContinuous3BandImage()) {
                    getFormModel().setRasters(getChildForm().getRasters());
                } else {
                    getFormModel().getRaster().setImageInfo(getFormModel().getModifiedImageInfo());
                }
                getFormModel().applyModifiedImageInfo();
            } finally {
                getToolViewPaneControl().setCursor(Cursor.getDefaultCursor());
            }
        }
        updateMultiApplyState();
    }

    private void resetToDefaults() {
        if (getFormModel().isValid()) {
            PropertyMap configuration = getFormModel().getProductSceneView().getSceneImage().getConfiguration();

            ColorSchemeUtils.setImageInfoToDefaultColor(configuration, createDefaultImageInfo(), getFormModel().getProductSceneView());
            getFormModel().setModifiedImageInfo(getFormModel().getProductSceneView().getImageInfo());

            getChildForm().resetFormModel(getFormModel());
        }
    }


    private void applyMultipleColorPaletteDef() {
        if (!getFormModel().isValid()) {
            return;
        }

        final Product selectedProduct = getFormModel().getProduct();
        final ProductManager productManager = selectedProduct.getProductManager();
        final RasterDataNode[] protectedRasters = getFormModel().getRasters();
        final ArrayList<Band> availableBandList = new ArrayList<>();
        for (int i = 0; i < productManager.getProductCount(); i++) {
            final Product product = productManager.getProduct(i);
            final Band[] bands = product.getBands();
            for (final Band band : bands) {
                boolean validBand = false;
                if (band.getImageInfo() != null) {
                    validBand = true;
                    for (RasterDataNode protectedRaster : protectedRasters) {
                        if (band == protectedRaster) {
                            validBand = false;
                        }
                    }
                }
                if (validBand) {
                    availableBandList.add(band);
                }
            }
        }
        final Band[] availableBands = new Band[availableBandList.size()];
        availableBandList.toArray(availableBands);
        availableBandList.clear();

        if (availableBands.length == 0) {
            AbstractDialog.showWarningDialog(getToolViewPaneControl(), "No other bands available.", titlePrefix);
            return;
        }

        final BandChooser bandChooser = new BandChooser(SwingUtilities.getWindowAncestor(toolView),
                "Apply to other bands",
                toolView.getHelpCtx().getHelpID(),
                availableBands,
                bandsToBeModified, false);

        final Set<RasterDataNode> modifiedRasters = new HashSet<>(availableBands.length);
        if (bandChooser.show() == BandChooser.ID_OK) {
            bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : bandsToBeModified) {
                applyColorPaletteDef(getFormModel().getModifiedImageInfo().getColorPaletteDef(), band, band.getImageInfo());
                modifiedRasters.add(band);
            }
        }


        // This code replaces visatApp.updateImages(rasters) from BEAM code.
        WindowUtilities.getOpened(ProductSceneViewTopComponent.class).forEach(tc -> {
            final ProductSceneView view = tc.getView();
            for (RasterDataNode raster : view.getRasters()) {
                if (modifiedRasters.contains(raster)) {
                    view.updateImage();
                    return;
                }
            }
        });
    }

    private void setIODir(final File dir) {
        ioDir = dir.toPath();
        Config.instance().preferences().put(PREFERENCES_KEY_IO_DIR, ioDir.toString());
    }

    @Override
    public Path getIODir() {
        if (ioDir == null) {
            ioDir = Paths.get(Config.instance().preferences().get(PREFERENCES_KEY_IO_DIR, getColorPalettesAuxDataDir().toString()));
        }
        return ioDir;
    }

    private SnapFileFilter getOrCreateCpdFileFilter() {
        if (snapFileFilter == null) {
            final String formatName = "COLOR_PALETTE_DEFINITION_FILE";
            final String description = FILE_EXTENSION_CPD.toUpperCase() + " - Default " + COLOR_LOWER_CASE + " palette format (*" + FILE_EXTENSION_CPD + ")";  /*I18N*/
            snapFileFilter = new SnapFileFilter(formatName, "." + FILE_EXTENSION_CPD, description);
        }
        return snapFileFilter;
    }

    private SnapFileFilter getOrCreatePalFileFilter() {
        if (palFileFilter == null) {
            final String formatName = "GENERIC_COLOR_PALETTE_FILE";
            final String description = FILE_EXTENSION_PAL.toUpperCase() + " - Generic 256 point " + COLOR_LOWER_CASE + " palette format (*." + FILE_EXTENSION_PAL + ")";  /*I18N*/
            palFileFilter = new SnapFileFilter(formatName, "." + FILE_EXTENSION_PAL, description);
        }
        return palFileFilter;
    }

    private SnapFileFilter getOrCreateCptFileFilter() {
        if (cptFileFilter == null) {
            final String formatName = "CPT_COLOR_PALETTE_FILE";
            final String description = FILE_EXTENSION_CPT.toUpperCase() + " - Generic mapping tools format (*." + FILE_EXTENSION_CPT + ")";  /*I18N*/
            cptFileFilter = new SnapFileFilter(formatName, "." + FILE_EXTENSION_CPT, description);
        }
        return cptFileFilter;
    }


    private void importColorPaletteDef() {
        final ImageInfo targetImageInfo = getFormModel().getModifiedImageInfo();
        if (targetImageInfo == null) {
            // Normally this code is unreachable because, the export Button
            // is disabled if the _contrastStretchPane has no ImageInfo.
            return;
        }
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setDialogTitle("Import " + NamingConvention.COLOR_MIXED_CASE + " Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateCpdFileFilter());
        fileChooser.addChoosableFileFilter(getOrCreateCptFileFilter());
        fileChooser.setCurrentDirectory(getIODir().toFile());


        final JPanel optionsPanel = new JPanel(new GridLayout(2, 1));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("CPT Options"));

        JRadioButton buttonSourceLogScaled = new JRadioButton("Source Log Scaled");
        buttonSourceLogScaled.setSelected(false);
        buttonSourceLogScaled.setEnabled(false);
        buttonSourceLogScaled.setToolTipText("The source cpt values are log scaled");

        JRadioButton buttonCptValue = new JRadioButton("Use CPT Value");
        buttonCptValue.setSelected(false);
        buttonCptValue.setEnabled(false);
        buttonCptValue.setToolTipText("The use cpt values instead of current min/max range settings");

        optionsPanel.add(buttonSourceLogScaled);
        optionsPanel.add(buttonCptValue);
        optionsPanel.setEnabled(false);


        fileChooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, evt -> {
            final SnapFileFilter snapFileFilter = fileChooser.getSnapFileFilter();

//            snapFileFilter.getFileSelectionMode().getValue();

            if (snapFileFilter != null) {
                String format = snapFileFilter.getFormatName();
                System.out.println("FORMAT=" + format);
                boolean cptEnabled = "CPT_COLOR_PALETTE_FILE".equals(snapFileFilter.getFormatName());
                System.out.println("enabled=" + cptEnabled);

                buttonSourceLogScaled.setEnabled(cptEnabled);
                buttonCptValue.setEnabled(cptEnabled);
                optionsPanel.setEnabled(cptEnabled);
            }
        });


        JComponent commentsPanel = new JLabel("");
//        commentsPanel.setBorder(BorderFactory.createTitledBorder("Comments")); /*I18N*/
//        commentsPanel.setToolTipText("Some comments");

        final JPanel accessory = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = .5;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(3, 3, 3, 3);


//        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(optionsPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        accessory.add(commentsPanel, gbc);
        fileChooser.setAccessory(accessory);


        final int result = fileChooser.showOpenDialog(getToolViewPaneControl());
        final File file = fileChooser.getSelectedFile();
        if (file != null && file.getParentFile() != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null && file.canRead()) {
                try {
                    ColorPaletteDef colorPaletteDef;

                    if (file.getName().endsWith(FILE_EXTENSION_CPT)) {
                        colorPaletteDef = ColorPaletteDef.loadCpt(file);
                    } else {
                        colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(file);
                    }


                    final ColorPaletteDef currentCPD = targetImageInfo.getColorPaletteDef();
                    final double min = currentCPD.getMinDisplaySample();
                    final double max = currentCPD.getMaxDisplaySample();

                    final boolean isSourceLogScaled = buttonSourceLogScaled.isSelected();

//                    final boolean isSourceLogScaled = colorPaletteDef.isLogScaled();
                    final boolean isTargetLogScaled = targetImageInfo.isLogScaled();

                    final boolean autoDistribute = !buttonCptValue.isSelected();
//                    final boolean autoDistribute = true;
                    if (ColorUtils.checkRangeCompatibility(min, max, isTargetLogScaled)) {
                        targetImageInfo.setColorPaletteDef(colorPaletteDef, min, max, autoDistribute, isSourceLogScaled, isTargetLogScaled);
                        ColorSchemeInfo colorSchemeInfo = ColorSchemeManager.getDefault().getNoneColorSchemeInfo();
                        targetImageInfo.setColorSchemeInfo(colorSchemeInfo);
                    }


                    currentCPD.getFirstPoint().setLabel(file.getName());
                    getFormModel().setModifiedImageInfo(targetImageInfo);
                    getFormModel().applyModifiedImageInfo();
                    getChildForm().updateFormModel(getFormModel());
                    updateMultiApplyState();
                } catch (IOException e) {
                    showErrorDialog("Failed to import " + NamingConvention.COLOR_LOWER_CASE + " palette:\n" + e.getMessage());
                }
            }
        }
    }

    private void applyColorPaletteDef(ColorPaletteDef colorPaletteDef,
                                      RasterDataNode targetRaster,
                                      ImageInfo targetImageInfo) {
        if (isIndexCoded(targetRaster)) {
            targetImageInfo.setColors(colorPaletteDef.getColors());
        } else {
            Stx stx = targetRaster.getStx(false, ProgressMonitor.NULL);
            Boolean autoDistribute = getAutoDistribute(colorPaletteDef);
            if (autoDistribute == null) {
                return;
            }
            targetImageInfo.setColorPaletteDef(colorPaletteDef,
                    stx.getMinimum(),
                    stx.getMaximum(),
                    autoDistribute);
        }
    }

    private Boolean getAutoDistribute(ColorPaletteDef colorPaletteDef) {
        if (colorPaletteDef.isAutoDistribute()) {
            return Boolean.TRUE;
        }
        int answer = JOptionPane.showConfirmDialog(getToolViewPaneControl(),
                "Automatically distribute points of\n" +
                        NamingConvention.COLOR_LOWER_CASE + " palette between min/max?",
                "Import " + NamingConvention.COLOR_MIXED_CASE + " Palette",
                JOptionPane.YES_NO_CANCEL_OPTION
        );
        if (answer == JOptionPane.YES_OPTION) {
            return Boolean.TRUE;
        } else if (answer == JOptionPane.NO_OPTION) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    private boolean isIndexCoded(RasterDataNode targetRaster) {
        return targetRaster instanceof Band && ((Band) targetRaster).getIndexCoding() != null;
    }

    private void exportColorPaletteDef() {
        final ImageInfo imageInfo = getFormModel().getModifiedImageInfo();
        if (imageInfo == null) {
            // Normally this code is unreachable because, the export Button should be
            // disabled if the color manipulation form has no ImageInfo.
            return;
        }
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setDialogTitle("Export " +  NamingConvention.COLOR_MIXED_CASE + " Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateCpdFileFilter());
        fileChooser.addChoosableFileFilter(getOrCreatePalFileFilter());
        fileChooser.addChoosableFileFilter(getOrCreateCptFileFilter());
        fileChooser.setCurrentDirectory(getIODir().toFile());
        final int result = fileChooser.showSaveDialog(getToolViewPaneControl());
        File file = fileChooser.getSelectedFile();
        if (file != null && file.getParentFile() != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null) {
                if (Boolean.TRUE.equals(Dialogs.requestOverwriteDecision(titlePrefix, file))) {
//                    file = FileUtils.ensureExtension(file, FILE_EXTENSION);
                    try {
                        final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
                        String path = file.getPath();

                        if (path.endsWith("." + FILE_EXTENSION_PAL)) {
                            ColorPaletteDef.storePal(colorPaletteDef, file);
                        } else if (path.endsWith("." + FILE_EXTENSION_CPT)) {
                            ColorPaletteDef.storeCpt(colorPaletteDef, file);
                        } else {
                            file = FileUtils.ensureExtension(file, "." + FILE_EXTENSION_CPD);
                            ColorPaletteDef.storeColorPaletteDef(colorPaletteDef, file);
                        }
                    } catch (IOException e) {
                        showErrorDialog("Failed to export " + NamingConvention.COLOR_LOWER_CASE + " palette:\n" + e.getMessage());  /*I18N*/
                    }
                }
            }
        }
    }

    private void showErrorDialog(final String message) {
        if (message != null && message.trim().length() > 0) {
            if (SnapApp.getDefault() != null) {
                Dialogs.showError(message);
            } else {
                Dialogs.showError("Error", message);
            }
        }
    }

    public ColorManipulationChildForm getChildForm() {
        return childForm;
    }

    public void setChildForm(ColorManipulationChildForm childForm) {
        this.childForm = childForm;
    }


    // Installs the color palette files resources
    private class InstallColorPalettesAuxFiles implements Runnable {

        private InstallColorPalettesAuxFiles() {
        }

        @Override
        public void run() {
            try {
                Path auxdataDir = getColorPalettesAuxDataDir();
                Path sourceDirPath = getColorPalettesAuxDataSourceDir();

                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDir);

                resourceInstaller.install(".*." + FILE_EXTENSION_CPD, ProgressMonitor.NULL);
                resourceInstaller.install(".*." + FILE_EXTENSION_CPT, ProgressMonitor.NULL);

                // these file get overwritten as we do not encourage that these standard files to be altered
                resourceInstaller.install(".*oceancolor_.*." + FILE_EXTENSION_CPD, ProgressMonitor.NULL);

                colorPalettesAuxFilesInstalled = true;
            } catch (IOException e) {
                SnapApp.getDefault().handleError("Unable to install auxdata/" + ColorManipulationDefaults.DIR_NAME_COLOR_PALETTES, e);
            }
        }
    }


    // Installs the color scheme xml files resources
    private class InstallColorSchemesAuxFiles implements Runnable {

        private InstallColorSchemesAuxFiles() {
        }

        @Override
        public void run() {
            try {
                Path auxdataDir = getColorSchemesAuxDataDir();
                Path sourceDirPath = getColorSchemesAuxDataSourceDir();

                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDir);

                resourceInstaller.install(".*." + ColorManipulationDefaults.COLOR_SCHEMES_FILENAME, ProgressMonitor.NULL);
                resourceInstaller.install(".*" + ColorManipulationDefaults.COLOR_SCHEME_LOOKUP_FILENAME, ProgressMonitor.NULL);

                colorSchemesAuxFilesInstalled = true;
            } catch (IOException e) {
                SnapApp.getDefault().handleError("Unable to install auxdata/" + ColorManipulationDefaults.DIR_NAME_COLOR_SCHEMES, e);
            }
        }
    }



    // Installs the RGB profile resources
    private class InstallRGBAuxFiles implements Runnable {

        private InstallRGBAuxFiles() {
        }

        @Override
        public void run() {
            try {
                Path auxdataDir = getRgbProfilesAuxDataDir();
                Path sourceDirPath = getRgbProfilesAuxDataSourceDir();

                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDir);

                resourceInstaller.install(".*." + FILE_EXTENSION_RGB_PROFILES, ProgressMonitor.NULL);

                rgbProfilesFilesInstalled = true;
            } catch (IOException e) {
                SnapApp.getDefault().handleError("Unable to install auxdata/" + ColorManipulationDefaults.DIR_NAME_RGB_PROFILES, e);
            }
        }
    }



    public Path getColorPalettesAuxDataSourceDir() {
        Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(GridBagUtils.class);
        Path auxdirSource = sourceBasePath.resolve(ColorManipulationDefaults.DIR_NAME_AUX_DATA);
        return auxdirSource.resolve(ColorManipulationDefaults.DIR_NAME_COLOR_PALETTES);
    }

    public Path getColorSchemesAuxDataSourceDir() {
        Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(GridBagUtils.class);
        Path auxdirSource = sourceBasePath.resolve(ColorManipulationDefaults.DIR_NAME_AUX_DATA);
        return auxdirSource.resolve(ColorManipulationDefaults.DIR_NAME_COLOR_SCHEMES);
    }

    public Path getRgbProfilesAuxDataSourceDir() {
        Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(GridBagUtils.class);
        Path auxdirSource = sourceBasePath.resolve(ColorManipulationDefaults.DIR_NAME_AUX_DATA);
        return auxdirSource.resolve(ColorManipulationDefaults.DIR_NAME_RGB_PROFILES);
    }


    private Path getColorPalettesAuxDataDir() {
        return ColorSchemeUtils.getColorPalettesAuxDataDir();
    }

    private Path getColorSchemesAuxDataDir() {
        return ColorSchemeUtils.getColorSchemesAuxDataDir();
    }

    private Path getRgbProfilesAuxDataDir() {
        return ColorSchemeUtils.getRgbProfilesAuxDataDir();
    }


    private ImageInfo createDefaultImageInfo() {
        try {
            return ProductUtils.createImageInfo(getFormModel().getRasters(), false, ProgressMonitor.NULL);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                    "Failed to create default image settings:\n" + e.getMessage(),
                    "I/O Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    @Override
    public Stx getStx(RasterDataNode raster) {
        return raster.getStx(false, ProgressMonitor.NULL);
    }

    private class ColorManipulationPNL extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {

            final RasterDataNode[] rasters = getChildForm().getRasters();
            RasterDataNode raster = null;
            for (RasterDataNode dataNode : rasters) {
                if (event.getSourceNode() == dataNode) {
                    raster = (RasterDataNode) event.getSourceNode();
                }
            }
            if (raster != null) {
                final String propertyName = event.getPropertyName();
                if (ProductNode.PROPERTY_NAME_NAME.equalsIgnoreCase(propertyName)) {
                    updateTitle();
                    getChildForm().handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_ANCILLARY_VARIABLES.equalsIgnoreCase(propertyName)) {
                    updateTitle();
                    getChildForm().handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_UNIT.equalsIgnoreCase(propertyName)) {
                    getChildForm().handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_STX.equalsIgnoreCase(propertyName)) {
                    getChildForm().handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.isValidMaskProperty(propertyName)) {
                    getStx(raster);
                }
            }
        }
    }

    @Override
    public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
        if (getFormModel().getProductSceneView() == oldValue) {
            setProductSceneView(null);
        }
        setProductSceneView(newValue);
        if (this.brightnessContrastPanel != null) {
            if (oldValue != null) {
                this.brightnessContrastPanel.productSceneViewDeselected(oldValue);
            }
            if (newValue != null) {
                this.brightnessContrastPanel.productSceneViewSelected(newValue);
            }
        }
    }

    private class SceneViewImageInfoChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProductSceneView.PROPERTY_NAME_IMAGE_INFO.equals(evt.getPropertyName())) {
                boolean correctFormForRaster = (getFormModel().getRaster() == getFormModel().getProductSceneView().getRaster());
                if (correctFormForRaster) {
                    ImageInfo modifiedImageInfo = (ImageInfo) evt.getNewValue();
                    getFormModel().setModifiedImageInfo(modifiedImageInfo);
                    getChildForm().updateFormModel(getFormModel());
                }
            }
        }
    }
}
