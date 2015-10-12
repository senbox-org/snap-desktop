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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.PixelGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.ExpressionPane;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductExpressionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
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
@ActionReference(path = "Menu/Tools", position = 210, separatorBefore = 200 )
@Messages({
        "CTL_AttachPixelGeoCodingActionText=Attach Pixel Geo-Coding...",
        "CTL_AttachPixelGeoCodingDialogTitle=Attach Pixel Geo-Coding",
        "CTL_AttachPixelGeoCodingDialogDescription=Attach a pixel based geo-coding to the selected product"
})
public class AttachPixelGeoCodingAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private static final String HELP_ID = "pixelGeoCodingSetup";

    private final Lookup lkp;

    public AttachPixelGeoCodingAction() {
        this(Utilities.actionsGlobalContext());
    }

    public AttachPixelGeoCodingAction(Lookup lkp) {
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
        attachPixelGeoCoding(lkp.lookup(ProductNode.class).getProduct());
    }

    private void setEnableState() {
        ProductNode productNode = lkp.lookup(ProductNode.class);
        boolean state = false;
        if (productNode != null) {
            Product product = productNode.getProduct();
            final boolean hasPixelGeoCoding = product.getSceneGeoCoding() instanceof BasicPixelGeoCoding;
            final boolean hasSomeBands = product.getNumBands() >= 2;
            state = !hasPixelGeoCoding && hasSomeBands;
        }
        setEnabled(state);
    }


    private static void attachPixelGeoCoding(final Product product) {

        final SnapApp snapApp = SnapApp.getDefault();
        final Window mainFrame = snapApp.getMainFrame();
        String dialogTitle = Bundle.CTL_AttachPixelGeoCodingDialogTitle();
        final PixelGeoCodingSetupDialog setupDialog = new PixelGeoCodingSetupDialog(mainFrame,
                                                                                    dialogTitle,
                                                                                    HELP_ID,
                                                                                    product);
        if (setupDialog.show() != ModalDialog.ID_OK) {
            return;
        }
        final Band lonBand = setupDialog.getSelectedLonBand();
        final Band latBand = setupDialog.getSelectedLatBand();
        final int searchRadius = setupDialog.getSearchRadius();
        final String validMask = setupDialog.getValidMask();
        final String msgPattern = "New Pixel Geo-Coding: lon = ''{0}'' ; lat = ''{1}'' ; radius=''{2}'' ; mask=''{3}''";
        snapApp.getLogger().log(Level.INFO, MessageFormat.format(msgPattern,
                                                                 lonBand.getName(), latBand.getName(),
                                                                 searchRadius, validMask));


        final long requiredBytes = PixelGeoCoding.getRequiredMemory(product, validMask != null);
        final long requiredMegas = requiredBytes / (1024 * 1024);
        final long freeMegas = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        if (freeMegas < requiredMegas) {
            final String message = MessageFormat.format("This operation requires to load at least {0} M\n" +
                                                        "of additional data into memory.\n\n" +
                                                        "Do you really want to continue?",
                                                        requiredMegas);
            final SnapDialogs.Answer answer = SnapDialogs.requestDecision(dialogTitle, message, false, "load_latlon_band_data");
            if (answer != SnapDialogs.Answer.YES) {
                return;
            }
        }

        UIUtils.setRootFrameWaitCursor(mainFrame);
        final ProgressMonitorSwingWorker<Void, Void> swingWorker = new ProgressMonitorSwingWorker<Void, Void>(mainFrame, dialogTitle) {

            @Override
            protected Void doInBackground(ProgressMonitor pm) throws Exception {
                final BasicPixelGeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, validMask, searchRadius, pm);
                product.setSceneGeoCoding(pixelGeoCoding);
                UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(product);
                if (undoManager != null) {
                    undoManager.addEdit(new UndoableAttachGeoCoding<>(product, pixelGeoCoding));
                }

                return null;
            }

            @Override
            public void done() {
                try {
                    get();
                    SnapDialogs.showInformation(dialogTitle, "Pixel geo-coding has been attached.", null);
                } catch (Exception e) {
                    Throwable cause = e;
                    if (e instanceof ExecutionException) {
                        cause = e.getCause();
                    }
                    String msg = "An internal error occurred:\n" + e.getMessage();
                    if (cause instanceof IOException) {
                        msg = "An I/O error occurred:\n" + e.getMessage();
                    }
                    SnapDialogs.showError(dialogTitle, msg);
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
        private JTextField validMaskField;
        private JSpinner radiusSpinner;
        private final int defaultRadius = 6;
        private final int minRadius = 0;
        private final int maxRadius = 99;
        private final int bigRadiusStep = 0;
        private final int smallRadiusStep = 1;

        public PixelGeoCodingSetupDialog(final Window parent, final String title,
                                         final String helpID, final Product product) {
            super(parent, title, ModalDialog.ID_OK_CANCEL_HELP, helpID);
            this.product = product;
            final Band[] bands = product.getBands();
            bandNames = Arrays.stream(bands).map(Band::getName).toArray(String[]::new);
        }

        @Override
        public int show() {
            createUI();
            return super.show();
        }


        public Band getSelectedLonBand() {
            return product.getBand(selectedLonBand);
        }

        public Band getSelectedLatBand() {
            return product.getBand(selectedLatBand);
        }

        public int getSearchRadius() {
            return ((Number) radiusSpinner.getValue()).intValue();
        }

        public String getValidMask() {
            return validMaskField.getText();
        }

        @Override
        protected void onOK() {
            final String lonValue = (String) lonBox.getSelectedItem();
            selectedLonBand = findBandName(lonValue);
            final String latValue = (String) latBox.getSelectedItem();
            selectedLatBand = findBandName(latValue);

            if (selectedLatBand == null || selectedLonBand == null || Objects.equals(selectedLatBand, selectedLonBand)) {
                SnapDialogs.showWarning(Bundle.CTL_AttachPixelGeoCodingDialogTitle(),
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
            final JLabel radiusLabel = new JLabel("Search radius:");
            final JLabel maskLabel = new JLabel("Valid mask:");
            lonBox = new JComboBox<>(bandNames);
            latBox = new JComboBox<>(bandNames);
            doPreSelection(lonBox, "lon");
            doPreSelection(latBox, "lat");
            radiusSpinner = UIUtils.createSpinner(defaultRadius, minRadius, maxRadius,
                                                  smallRadiusStep, bigRadiusStep, "#0");
            validMaskField = new JTextField(createDefaultValidMask(product));
            validMaskField.setCaretPosition(0);
            final JButton exprDialogButton = new JButton("...");
            exprDialogButton.addActionListener(e -> {
                invokeExpressionEditor();
            });
            final int preferredSize = validMaskField.getPreferredSize().height;
            exprDialogButton.setPreferredSize(new Dimension(preferredSize, preferredSize));
            radiusSpinner.setPreferredSize(new Dimension(60, preferredSize));

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
            panel.add(maskLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            panel.add(validMaskField, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            panel.add(exprDialogButton, gbc);

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(radiusLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(radiusSpinner, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            panel.add(new JLabel("pixels"), gbc);
            setContent(panel);
        }

        private void invokeExpressionEditor() {
            SnapApp snapApp = SnapApp.getDefault();
            final Window window = SwingUtilities.getWindowAncestor(snapApp.getMainFrame());
            final ExpressionPane pane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product},
                                                                                          product,
                                                                                          snapApp.getPreferencesPropertyMap());
            pane.setCode(validMaskField.getText());
            final int status = pane.showModalDialog(window, "Edit Valid Mask Expression");
            if (status == ID_OK) {
                validMaskField.setText(pane.getCode());
                validMaskField.setCaretPosition(0);
            }
        }

        private void doPreSelection(final JComboBox comboBox, final String toFind) {
            final String bandToSelect = getBandNameContaining(toFind);

            if (StringUtils.isNotNullAndNotEmpty(bandToSelect)) {
                comboBox.setSelectedItem(bandToSelect);
            }
        }

        private String getBandNameContaining(final String toFind) {
            return Arrays.stream(bandNames).filter(s -> s.contains(toFind)).findFirst().orElseGet(() -> null);
        }

        private String findBandName(final String bandName) {
            return Arrays.stream(bandNames).filter(s -> s.equals(bandName)).findFirst().orElseGet(() -> null);
        }

        private static String createDefaultValidMask(final Product product) {
            String validMask = null;
            final String[] flagNames = product.getAllFlagNames();
            final String invalidFlagName = "l1_flags.INVALID";
            if (ArrayUtils.isMemberOf(invalidFlagName, flagNames)) {
                validMask = "NOT " + invalidFlagName;
            }
            return validMask;
        }

    }

    private static class UndoableAttachGeoCoding<T extends BasicPixelGeoCoding> extends AbstractUndoableEdit {

        private Product product;
        private T pixelGeoCoding;

        public UndoableAttachGeoCoding(Product product, T pixelGeoCoding) {
            Assert.notNull(product, "product");
            Assert.notNull(pixelGeoCoding, "pixelGeoCoding");
            this.product = product;
            this.pixelGeoCoding = pixelGeoCoding;
        }


        @Override
        public String getPresentationName() {
            return Bundle.CTL_AttachPixelGeoCodingDialogTitle();
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            if (product.getSceneGeoCoding() == pixelGeoCoding) {
                product.setSceneGeoCoding(pixelGeoCoding.getPixelPosEstimator());
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            product.setSceneGeoCoding(pixelGeoCoding);
        }

        @Override
        public void die() {
            pixelGeoCoding = null;
            product = null;
        }
    }
}
