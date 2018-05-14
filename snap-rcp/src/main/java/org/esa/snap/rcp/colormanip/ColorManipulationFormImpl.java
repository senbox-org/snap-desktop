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
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
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


/**
 * The GUI for the colour manipulation tool window.
 */
@NbBundle.Messages({
        "CTL_ColorManipulationForm_TitlePrefix=Colour Manipulation"
})
class ColorManipulationFormImpl implements SelectionSupport.Handler<ProductSceneView>, ColorManipulationForm {

    private final static String PREFERENCES_KEY_IO_DIR = "snap.color_palettes.dir";

    private final static String FILE_EXTENSION = ".cpd";
    private AbstractButton resetButton;
    private AbstractButton multiApplyButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;

    private final TopComponent toolView;
    private final ColorFormModel formModel;
    private Band[] bandsToBeModified;
    private SnapFileFilter snapFileFilter;
    private final ProductNodeListener productNodeListener;
    private boolean defaultColorPalettesInstalled;
    private JPanel contentPanel;
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
        if (!defaultColorPalettesInstalled) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new InstallDefaultColorPalettes());
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
        }

        if (getFormModel().isValid()) {
            getFormModel().setModifiedImageInfo(getFormModel().getOriginalImageInfo());
        }

        installChildForm();

        updateTitle();
        updateToolButtons();

        updateMultiApplyState();
    }

    private void installChildForm() {
        final ColorManipulationChildForm oldForm = childForm;
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
            childForm = newForm;

            installToolButtons();
            installMoreOptions();

            editorPanel.removeAll();
            editorPanel.add(childForm.getContentPanel(), BorderLayout.CENTER);
            if (!(childForm instanceof EmptyImageInfoForm)) {
                editorPanel.add(moreOptionsPane.getContentPanel(), BorderLayout.SOUTH);
            }
            revalidateToolViewPaneControl();

            if (oldForm != null) {
                oldForm.handleFormHidden(getFormModel());
            }
            childForm.handleFormShown(getFormModel());
        } else {
            childForm.updateFormModel(getFormModel());
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
        importButton.setToolTipText("Import colour palette from text file."); /*I18N*/
        importButton.addActionListener(e -> {
            importColorPaletteDef();
            applyChanges();
        });
        importButton.setEnabled(true);

        exportButton = createButton("tango/22x22/actions/document-save-as.png");
        exportButton.setName("ExportButton");
        exportButton.setToolTipText("Save colour palette to text file."); /*I18N*/
        exportButton.addActionListener(e -> {
            exportColorPaletteDef();
            childForm.updateFormModel(getFormModel());
        });
        exportButton.setEnabled(true);

        helpButton = createButton("tango/22x22/apps/help-browser.png");
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");
        helpButton.addActionListener(e -> toolView.getHelpCtx().display());

        editorPanel = new JPanel(new BorderLayout(4, 4));
        toolButtonsPanel = GridBagUtils.createPanel();

        contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel.setPreferredSize(new Dimension(320, 200));
        contentPanel.add(editorPanel, BorderLayout.CENTER);
        contentPanel.add(toolButtonsPanel, BorderLayout.EAST);

        setProductSceneView(SnapApp.getDefault().getSelectedProductSceneView());

        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler(this);
    }

    public void updateMultiApplyState() {
        multiApplyButton.setEnabled(getFormModel().isValid() && !getFormModel().isContinuous3BandImage());
    }

    @Override
    public void installToolButtons() {
        toolButtonsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        toolButtonsPanel.add(resetButton, gbc);
        toolButtonsPanel.add(multiApplyButton, gbc);
        gbc.gridy++;
        toolButtonsPanel.add(importButton, gbc);
        toolButtonsPanel.add(exportButton, gbc);
        gbc.gridy++;
        AbstractButton[] additionalButtons = childForm.getToolButtons();
        for (int i = 0; i < additionalButtons.length; i++) {
            AbstractButton button = additionalButtons[i];
            toolButtonsPanel.add(button, gbc);
            if (i % 2 == 1) {
                gbc.gridy++;
            }
        }

        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        toolButtonsPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 1;
        toolButtonsPanel.add(helpButton, gbc);
    }

    @Override
    public void installMoreOptions() {
        final MoreOptionsForm moreOptionsForm = childForm.getMoreOptionsForm();
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

    @Override
    public void applyChanges() {
        updateMultiApplyState();
        if (getFormModel().isValid()) {
            try {
                getToolViewPaneControl().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (getFormModel().isContinuous3BandImage()) {
                    getFormModel().setRasters(childForm.getRasters());
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
            getFormModel().setModifiedImageInfo(createDefaultImageInfo());
            childForm.resetFormModel(getFormModel());
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
            ioDir = Paths.get(Config.instance().preferences().get(PREFERENCES_KEY_IO_DIR, getColorPalettesDir().toString()));
        }
        return ioDir;
    }

    private SnapFileFilter getOrCreateColorPaletteDefinitionFileFilter() {
        if (snapFileFilter == null) {
            final String formatName = "COLOR_PALETTE_DEFINITION_FILE";
            final String description = "Colour palette files (*" + FILE_EXTENSION + ")";  /*I18N*/
            snapFileFilter = new SnapFileFilter(formatName, FILE_EXTENSION, description);
        }
        return snapFileFilter;
    }

    private void importColorPaletteDef() {
        final ImageInfo targetImageInfo = getFormModel().getModifiedImageInfo();
        if (targetImageInfo == null) {
            // Normally this code is unreachable because, the export Button
            // is disabled if the _contrastStretchPane has no ImageInfo.
            return;
        }
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setDialogTitle("Import Colour Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir().toFile());
        final int result = fileChooser.showOpenDialog(getToolViewPaneControl());
        final File file = fileChooser.getSelectedFile();
        if (file != null && file.getParentFile() != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null && file.canRead()) {
                try {
                    final ColorPaletteDef colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(file);
                    colorPaletteDef.getFirstPoint().setLabel(file.getName());
                    applyColorPaletteDef(colorPaletteDef, getFormModel().getRaster(), targetImageInfo);
                    getFormModel().setModifiedImageInfo(targetImageInfo);
                    childForm.updateFormModel(getFormModel());
                    updateMultiApplyState();
                } catch (IOException e) {
                    showErrorDialog("Failed to import colour palette:\n" + e.getMessage());
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
                                                           "colour palette between min/max?",
                                                   "Import Colour Palette",
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
        fileChooser.setDialogTitle("Export Colour Palette"); /*I18N*/
        fileChooser.setFileFilter(getOrCreateColorPaletteDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir().toFile());
        final int result = fileChooser.showSaveDialog(getToolViewPaneControl());
        File file = fileChooser.getSelectedFile();
        if (file != null && file.getParentFile() != null) {
            setIODir(file.getParentFile());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            if (file != null) {
                if (Boolean.TRUE.equals(Dialogs.requestOverwriteDecision(titlePrefix, file))) {
                    file = FileUtils.ensureExtension(file, FILE_EXTENSION);
                    try {
                        final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
                        ColorPaletteDef.storeColorPaletteDef(colorPaletteDef, file);
                    } catch (IOException e) {
                        showErrorDialog("Failed to export colour palette:\n" + e.getMessage());  /*I18N*/
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

    private class InstallDefaultColorPalettes implements Runnable {

        private InstallDefaultColorPalettes() {
        }

        @Override
        public void run() {
            try {
                Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(GridBagUtils.class);
                Path auxdataDir = getColorPalettesDir();
                Path sourceDirPath = sourceBasePath.resolve("auxdata/color_palettes");
                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDir);

                resourceInstaller.install(".*.cpd", ProgressMonitor.NULL);
                defaultColorPalettesInstalled = true;
            } catch (IOException e) {
                SnapApp.getDefault().handleError("Unable to install colour palettes", e);
            }
        }
    }

    private Path getColorPalettesDir() {
        return SystemUtils.getAuxDataPath().resolve("color_palettes");
    }

    private ImageInfo createDefaultImageInfo() {
        try {
            return ProductUtils.createImageInfo(getFormModel().getRasters(), false, ProgressMonitor.NULL);
        } catch (IOException e) {
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

            final RasterDataNode[] rasters = childForm.getRasters();
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
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_ANCILLARY_VARIABLES.equalsIgnoreCase(propertyName)) {
                    updateTitle();
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_UNIT.equalsIgnoreCase(propertyName)) {
                    childForm.handleRasterPropertyChange(event, raster);
                } else if (RasterDataNode.PROPERTY_NAME_STX.equalsIgnoreCase(propertyName)) {
                    childForm.handleRasterPropertyChange(event, raster);
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

        public SceneViewImageInfoChangeListener() {
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProductSceneView.PROPERTY_NAME_IMAGE_INFO.equals(evt.getPropertyName())) {
                boolean correctFormForRaster = (getFormModel().getRaster() == getFormModel().getProductSceneView().getRaster());
                if (correctFormForRaster) {
                    ImageInfo modifiedImageInfo = (ImageInfo) evt.getNewValue();
                    getFormModel().setModifiedImageInfo(modifiedImageInfo);
                    childForm.updateFormModel(getFormModel());
                }
            }
        }
    }
}
