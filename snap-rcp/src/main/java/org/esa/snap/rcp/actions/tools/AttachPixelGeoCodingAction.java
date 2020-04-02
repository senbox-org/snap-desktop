/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.actions.tools;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@ActionID(
        category = "Tools",
        id = "AttachPixelGeoCodingAction"
)
@ActionRegistration(
        displayName = "#CTL_AttachPixelGeoCodingActionText",
        popupText = "#CTL_AttachPixelGeoCodingActionText",
        lazy = false
)
@ActionReference(path = "Menu/Tools", position = 210, separatorBefore = 200)
@Messages({
        "CTL_AttachPixelGeoCodingActionText=Attach Pixel Geo-Coding...",
        "CTL_AttachPixelGeoCodingDialogTitle=Attach Pixel Geo-Coding",
        "CTL_AttachPixelGeoCodingDialogDescription=Attach a pixel based geo-coding to the selected product"
})
public class AttachPixelGeoCodingAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private static final String HELP_ID = "pixelGeoCodingSetup";
    private static final int ONE_MB = 1024 * 1024;

    private final Lookup lkp;

    public AttachPixelGeoCodingAction() {
        this(Utilities.actionsGlobalContext());
    }

    private AttachPixelGeoCodingAction(Lookup lkp) {
        super(Bundle.CTL_AttachPixelGeoCodingActionText());
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_AttachPixelGeoCodingDialogDescription());
        setEnableState();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AttachPixelGeoCodingAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final Product selectedProduct = lkp.lookup(ProductNode.class).getProduct();
        final int validBandCount = getValidBandCount(selectedProduct);
        if (validBandCount < 2) {
            Dialogs.showError("Pixel Geo-Coding cannot be attached: Too few bands of product scene size");
            return;
        }

        attachPixelGeoCoding(selectedProduct);
    }

    static int getValidBandCount(Product product) {
        final Band[] bands = product.getBands();
        int validBandsCount = 0;
        for (Band band : bands) {
            if (band.getRasterSize().equals(product.getSceneRasterSize())) {
                validBandsCount++;
                if (validBandsCount == 2) {
                    break;
                }
            }
        }

        return validBandsCount;
    }

    static long getRequiredMemory(Product product) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        final int sizeOfDouble = 8;

        return width * height * 2 * sizeOfDouble;
    }

    private void setEnableState() {
        ProductNode productNode = lkp.lookup(ProductNode.class);
        boolean state = false;
        if (productNode != null) {
            Product product = productNode.getProduct();
            if (product != null) {
                state = product.getNumBands() >= 2;
            }
        }
        setEnabled(state);
    }

    private static void attachPixelGeoCoding(final Product product) {
        final SnapApp snapApp = SnapApp.getDefault();
        final Window mainFrame = snapApp.getMainFrame();
        final String dialogTitle = Bundle.CTL_AttachPixelGeoCodingDialogTitle();
        final PixelGeoCodingSetupDialog setupDialog = new PixelGeoCodingSetupDialog(mainFrame,
                dialogTitle,
                HELP_ID,
                product);
        if (setupDialog.show() != ModalDialog.ID_OK) {
            return;
        }

        final Band lonBand = setupDialog.getSelectedLonBand();
        final Band latBand = setupDialog.getSelectedLatBand();
        final String groundResString = setupDialog.getGroundResString();
        final double groundResInKm = Double.parseDouble(groundResString);
        final String msgPattern = "New Pixel Geo-Coding: lon = ''{0}'' ; lat = ''{1}'' ; ground resolution=''{2}''";
        snapApp.getLogger().log(Level.INFO, MessageFormat.format(msgPattern,
                lonBand.getName(), latBand.getName(),
                groundResString));

        final long requiredBytes = getRequiredMemory(product);
        final long requiredMegas = requiredBytes / ONE_MB;
        final long freeMegas = Runtime.getRuntime().freeMemory() / ONE_MB;
        if (freeMegas < requiredMegas) {
            final String message = MessageFormat.format("This operation requires to load at least {0} M\n" +
                            "of additional data into memory.\n\n" +
                            "Do you really want to continue?",
                    requiredMegas);
            final Dialogs.Answer answer = Dialogs.requestDecision(dialogTitle, message, false, "load_latlon_band_data");
            if (answer != Dialogs.Answer.YES) {
                return;
            }
        }

        UIUtils.setRootFrameWaitCursor(mainFrame);
        final ProgressMonitorSwingWorker<Void, Void> swingWorker = new ProgressMonitorSwingWorker<Void, Void>(mainFrame, dialogTitle) {

            @Override
            protected Void doInBackground(ProgressMonitor pm) throws Exception {
                final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
                final double[] latitudes = RasterUtils.loadDataScaled(latBand);
                final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonBand.getName(), latBand.getName(),
                        product.getSceneRasterWidth(), product.getSceneRasterHeight(), groundResInKm);
                final ForwardCoding forward = ComponentFactory.getForward(PixelForward.KEY);
                final InverseCoding inverse = ComponentFactory.getInverse(PixelQuadTreeInverse.KEY);

                final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.POLES);
                geoCoding.initialize();

                final GeoCoding oldGeoCoding = product.getSceneGeoCoding();
                product.setSceneGeoCoding(geoCoding);
                UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
                if (undoManager != null) {
                    undoManager.addEdit(new UndoableAttachGeoCoding(product, geoCoding, oldGeoCoding));
                }

                return null;
            }

            @Override
            public void done() {
                try {
                    get();
                    Dialogs.showInformation(dialogTitle, "Pixel geo-coding has been attached.", null);
                } catch (Exception e) {
                    Throwable cause = e;
                    if (e instanceof ExecutionException) {
                        cause = e.getCause();
                    }
                    String msg = "An internal error occurred:\n" + e.getMessage();
                    if (cause instanceof IOException) {
                        msg = "An I/O error occurred:\n" + e.getMessage();
                    }
                    Dialogs.showError(dialogTitle, msg);
                } finally {
                    UIUtils.setRootFrameDefaultCursor(mainFrame);
                }
            }
        };

        swingWorker.executeWithBlocking();
    }

    private static class PixelGeoCodingSetupDialog extends ModalDialog {

        private Product product;
        private String selectedLonBand;
        private String selectedLatBand;
        private String[] bandNames;
        private JComboBox<String> lonBox;
        private JComboBox<String> latBox;
        private JTextField groundResField;
        private final double defaultResolution = 1.0;

        PixelGeoCodingSetupDialog(final Window parent, final String title,
                                  final String helpID, final Product product) {
            super(parent, title, ModalDialog.ID_OK_CANCEL_HELP, helpID);
            this.product = product;
            final Band[] bands = product.getBands();
            if (product.isMultiSize()) {
                List<String> bandNameList = new ArrayList<>();
                for (Band band : bands) {
                    if (band.getRasterSize().equals(product.getSceneRasterSize())) {
                        bandNameList.add(band.getName());
                    }
                }
                bandNames = bandNameList.toArray(new String[0]);
            } else {
                bandNames = Arrays.stream(bands).map(Band::getName).toArray(String[]::new);
            }
        }

        @Override
        public int show() {
            createUI();
            return super.show();
        }


        Band getSelectedLonBand() {
            return product.getBand(selectedLonBand);
        }

        Band getSelectedLatBand() {
            return product.getBand(selectedLatBand);
        }

        String getGroundResString() {
            return groundResField.getText();
        }

        @Override
        protected void onOK() {
            final String lonValue = (String) lonBox.getSelectedItem();
            selectedLonBand = findBandName(lonValue);
            final String latValue = (String) latBox.getSelectedItem();
            selectedLatBand = findBandName(latValue);

            if (selectedLatBand == null || selectedLonBand == null || Objects.equals(selectedLatBand, selectedLonBand)) {
                Dialogs.showWarning(Bundle.CTL_AttachPixelGeoCodingDialogTitle(),
                        "You have to select two different bands for the pixel geo-coding.",
                        null);
            } else {
                super.onOK();
            }
        }

        @Override
        protected void onCancel() {
            selectedLatBand = null;
            selectedLonBand = null;
            super.onCancel();
        }

        private void createUI() {
            final JPanel panel = new JPanel(new GridBagLayout());
            final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
            final JLabel lonLabel = new JLabel("Longitude band:");
            final JLabel latLabel = new JLabel("Latitude band:");
            final JLabel groundResLabel = new JLabel("Ground Resolution in km:");
            lonBox = new JComboBox<>(bandNames);
            latBox = new JComboBox<>(bandNames);
            doPreSelection(lonBox, "lon");
            doPreSelection(latBox, "lat");
            groundResField = new JTextField(Double.toString(defaultResolution));
            groundResField.setCaretPosition(0);

            gbc.insets = new Insets(3, 2, 3, 2);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(lonLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            gbc.gridwidth = 1;
            panel.add(lonBox, gbc);

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(latLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            gbc.gridwidth = 1;
            panel.add(latBox, gbc);

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(groundResLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            panel.add(groundResField, gbc);

            setContent(panel);
        }

        private void doPreSelection(final JComboBox comboBox, final String toFind) {
            final String bandToSelect = getBandNameContaining(toFind);

            if (StringUtils.isNotNullAndNotEmpty(bandToSelect)) {
                comboBox.setSelectedItem(bandToSelect);
            }
        }

        private String getBandNameContaining(final String toFind) {
            return Arrays.stream(bandNames).filter(s -> s.contains(toFind)).findFirst().orElse(null);
        }

        private String findBandName(final String bandName) {
            return Arrays.stream(bandNames).filter(s -> s.equals(bandName)).findFirst().orElseGet(() -> null);
        }
    }

    private static class UndoableAttachGeoCoding extends AbstractUndoableEdit {

        private Product product;
        private GeoCoding oldGeoCoding;
        private GeoCoding currentGeoCoding;

        UndoableAttachGeoCoding(Product product, GeoCoding currentGeoCoding, GeoCoding oldGeoCoding) {
            this.product = product;
            this.currentGeoCoding = currentGeoCoding;
            this.oldGeoCoding = oldGeoCoding;
        }

        @Override
        public String getPresentationName() {
            return Bundle.CTL_AttachPixelGeoCodingDialogTitle();
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            product.setSceneGeoCoding(oldGeoCoding);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            product.setSceneGeoCoding(currentGeoCoding);
        }

        @Override
        public void die() {
            oldGeoCoding = null;
            currentGeoCoding = null;
            product = null;
        }
    }
}
