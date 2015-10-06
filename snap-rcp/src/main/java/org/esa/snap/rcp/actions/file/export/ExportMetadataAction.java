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

package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.metadata.MetadataViewTopComponent;
import org.esa.snap.ui.SelectExportMethodDialog;
import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;


@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportMetadataAction"
)
@ActionRegistration(
        displayName = "#CTL_ ExportMetadataAction_MenuText",
        popupText = "#CTL_ ExportMetadataAction_PopupText",
        lazy = false
)
@ActionReference(path = "Menu/File/Export/Other", position = 60)
@NbBundle.Messages({
        "CTL_ExportMetadataAction_MenuText=Product Metadata",
        "CTL_ExportMetadataAction_PopupText=Export Product Metadata...",
        "CTL_ExportMetadataAction_HelpID=exportMetadata",
        "CTL_ExportMetadataAction_ShortDescription=Export the currently displayed metadata as tab-separated text."
})
public class ExportMetadataAction extends AbstractAction implements HelpCtx.Provider, LookupListener, ContextAwareAction {

    private static final String ERR_MSG_BASE = "Metadata could not be exported:\n";
    private static final String DLG_TITLE = "Export Product Metadata";
    private final Lookup.Result<MetadataElement> result;
    private final Lookup lookup;
    private MetadataElement productMetadata;


    public ExportMetadataAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportMetadataAction(Lookup lookup) {
        super(Bundle.CTL_ExportMetadataAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(MetadataElement.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }

    private static String getWindowTitle() {
        return SnapApp.getDefault().getInstanceName() + " - " + DLG_TITLE;
    }

    /**
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @param visatApp        An instance of the VISAT application.
     * @param defaultFileName The default file name.
     * @return The selected file, <code>null</code> means "Cancel".
     */
    private static File promptForFile(final SnapApp snapApp, String defaultFileName) {
        // Loop while the user does not want to overwrite a selected, existing file
        // or if the user presses "Cancel"
        File file = null;
        while (file == null) {
            file = SnapDialogs.requestFileForSave(DLG_TITLE,
                                                  false,
                                                  null,
                                                  ".txt",
                                                  defaultFileName, null,
                                                  "exportMetadata.lastDir");
            if (file == null) {
                return null; // Cancel
            } else if (file.exists()) {
                int status = JOptionPane.showConfirmDialog(SnapApp.getDefault().getMainFrame(),
                                                           "The file '" + file + "' already exists.\n" + /*I18N*/
                                                                   "Overwrite it?",
                                                           MessageFormat.format("{0} - {1}", SnapApp.getDefault().getInstanceName(), DLG_TITLE),
                                                           JOptionPane.YES_NO_CANCEL_OPTION,
                                                           JOptionPane.WARNING_MESSAGE);
                if (status == JOptionPane.CANCEL_OPTION) {
                    return null; // Cancel
                } else if (status == JOptionPane.NO_OPTION) {
                    file = null; // No, do not overwrite, let user select other file
                }
            }
        }
        return file;
    }

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        exportMetadata();
    }

    /**
     * Called when a command should update its state.
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_ExportMetadataAction_MenuText());
    }
    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "Export Metadata" command
    /////////////////////////////////////////////////////////////////////////

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ExportMetadataAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        MetadataElement metadataElement = lookup.lookup(MetadataElement.class);
        setEnabled(metadataElement != null);
    }

    private void exportMetadata() {

        productMetadata = (MetadataElement) SnapApp.getDefault().getSelectedProductNode();
        if (!(productMetadata instanceof MetadataElement)) {
            return;
        }
        final String msgText = "How do you want to export the metadata?\n" +
                productMetadata.getName() + "Element  will be exported.\n"; /*I18N*/

        final int method = SelectExportMethodDialog.run(SnapApp.getDefault().getMainFrame(), getWindowTitle(),
                                                        msgText, getHelpCtx().getHelpID());

        final PrintWriter out;
        final StringBuffer clipboardText;
        final int initialBufferSize = 256000;
        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            final StringWriter stringWriter = new StringWriter(initialBufferSize);
            out = new PrintWriter(stringWriter);
            clipboardText = stringWriter.getBuffer();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            MetadataViewTopComponent metadataViewTopComponent = new MetadataViewTopComponent(productMetadata);
            final File file = promptForFile(SnapApp.getDefault(), createDefaultFileName(metadataViewTopComponent));
            if (file == null) {
                return; // Cancel
            }
            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                SnapDialogs.showError(DLG_TITLE,
                                      ERR_MSG_BASE + "Failed to create file '" + file + "':\n" + e.getMessage()); /*I18N*/
                return; // Error
            }
            out = new PrintWriter(new BufferedWriter(fileWriter, initialBufferSize));
            clipboardText = null;
        } else {
            return; // Cancel
        }

        // Create a progress monitor and adds them to the progress controller pool in order to show export progress
        // Create a new swing worker instance (as instance of an anonymous class).
        // When the swing worker's start() method is called, a new separate thread is started.
        // The swing worker's construct() method is executed in that thread, so that
        // VISAT can keep on handling user events.
        //
        final SwingWorker swingWorker = new SwingWorker<Exception, Object>() {

            @Override
            protected Exception doInBackground() throws Exception {
                boolean success;
                Exception returnValue = null;
                try {
                    ProgressMonitor pm = new DialogProgressMonitor(SnapApp.getDefault().getMainFrame(), DLG_TITLE,
                                                                   Dialog.ModalityType.APPLICATION_MODAL);
                    final MetadataExporter exporter = new MetadataExporter(productMetadata);
                    success = exporter.exportMetadata(out, pm);
                    if (success && clipboardText != null) {
                        SystemUtils.copyToClipboard(clipboardText.toString());
                        clipboardText.setLength(0);
                    }
                } catch (Exception e) {
                    returnValue = e;
                } finally {
                    out.close();
                }
                return returnValue;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread) after the <code>construct</code> method
             * has returned.
             */
            @Override
            public void done() {
                // clear status bar
                SnapApp.getDefault().setStatusBarMessage("");
                // show default-cursor
                UIUtils.setRootFrameDefaultCursor(SnapApp.getDefault().getMainFrame());
                // On error, show error message
                Exception exception;
                try {
                    exception = get();
                } catch (Exception e) {
                    exception = e;
                }
                if (exception != null) {
                    SnapDialogs.showError(DLG_TITLE, ERR_MSG_BASE + exception.getMessage());
                }
            }
        };

        // show wait-cursor
        // show message in status bar
        SnapApp.getDefault().setStatusBarMessage("Exporting Product Metadata..."); /*I18N*/
        // Start separate worker thread.
        swingWorker.execute();
    }

    private String createDefaultFileName(MetadataViewTopComponent productMetadataView) {
        return FileUtils.getFilenameWithoutExtension(productMetadataView.getDocument().getProduct().getName()) + "_" +
                productMetadata.getName() +
                ".txt";
    }

    private static class MetadataExporter {

        private final MetadataElement rootElement;

        private MetadataExporter(MetadataElement rootElement) {
            this.rootElement = rootElement;
        }

        public boolean exportMetadata(final PrintWriter out, ProgressMonitor pm) {
            pm.beginTask("Export Metadata", 1);
            try {
                writeHeaderLine(out);
                writeAttributes(out, rootElement);
                pm.worked(1);
            } finally {
                pm.done();
            }
            return true;
        }

        private void writeHeaderLine(final PrintWriter out) {
            out.print("Value\t");
            out.print("Type\t");
            out.print("Unit\t");
            out.print("Description\t\n");
        }

        private void writeAttributes(PrintWriter out, MetadataElement element) {
            final MetadataAttribute[] attributes = element.getAttributes();
            for (MetadataAttribute attribute : attributes) {
                out.print(createAttributeName(attribute) + "\t");
                out.print(attribute.getData().getElemString() + "\t");
                out.print(attribute.getUnit() + "\t");
                out.print(attribute.getDescription() + "\t\n");
            }
            final MetadataElement[] subElements = element.getElements();
            for (MetadataElement subElement : subElements) {
                writeAttributes(out, subElement);
            }
        }

        private String createAttributeName(MetadataAttribute attribute) {
            StringBuilder sb = new StringBuilder();
            MetadataElement metadataElement = attribute.getParentElement();
            if (metadataElement != null) {
                prependParentName(metadataElement, sb);
            }
            sb.append(attribute.getName());
            return sb.toString();
        }

        private void prependParentName(MetadataElement element, StringBuilder sb) {
            final MetadataElement owner = element.getParentElement();
            if (owner != null) {
                if (owner != rootElement) {
                    prependParentName(owner, sb);
                } else if (owner.getName() != null) {
                    sb.insert(0, owner.getName()).append(".");
                }
            }
            if (element.getName() != null) {
                sb.append(element.getName()).append(".");
            }
        }
    }
}
