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

package org.esa.snap.timeseries.export.text;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.export.util.TimeSeriesExportHelper;
import org.esa.snap.ui.SelectExportMethodDialog;
import org.esa.snap.ui.SnapFileChooser;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExportTimeBasedText extends ProgressMonitorSwingWorker<String, Void> {

    private static final String DLG_TITLE = "Exporting time series pin pixels";
    private static final String ERR_MSG_BASE = "Time series pin pixels cannot be exported:\n";
    private static final String EXPORT_DIR_PREFERENCES_KEY = "user.export.dir";
    private static final SnapFileFilter csvFileFilter = new SnapFileFilter("CSV", "csv", "Comma separated values");

    private final AbstractTimeSeries timeSeries;
    private final PrintWriter writer;
    private final StringBuffer clipboardText;

    private ExportTimeBasedText(Component parentComponent, AbstractTimeSeries timeSeries, PrintWriter writer,
                                StringBuffer clipboardText) {
        super(parentComponent, DLG_TITLE);
        this.timeSeries = timeSeries;
        this.writer = writer;
        this.clipboardText = clipboardText;
    }

    @Override
    protected String doInBackground(ProgressMonitor pm) throws Exception {
        List<List<Band>> bandList = new ArrayList<>();
        final List<String> timeVariables = timeSeries.getEoVariables();
        for (String timeVariable : timeVariables) {
            bandList.add(timeSeries.getBandsForVariable(timeVariable));
        }
        final PlacemarkGroup pinGroup = timeSeries.getTsProduct().getPinGroup();
        final ProductNode[] placemarkArray = pinGroup.toArray();
        if (placemarkArray.length == 0) {
            return "There are no pins which could be exported.";
        }
        List<Placemark> placemarks = new ArrayList<>();
        for (ProductNode placemark : placemarkArray) {
            placemarks.add((Placemark) placemark);
        }
        CsvExporter exporter = new TimeCsvExporter(bandList, placemarks, writer);
        exporter.exportCsv(pm);
        return null;
    }

    @Override
    public void done() {
        // On error, show error message
        String errorMessage;
        try {
            errorMessage = get();
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
        if (errorMessage != null) {
            Dialogs.showError(DLG_TITLE, ERR_MSG_BASE + errorMessage);
        } else {
            if (clipboardText != null) {
                SystemUtils.copyToClipboard(clipboardText.toString());
                clipboardText.setLength(0);
            }
        }
    }

    public static void export(Component parent, AbstractTimeSeries timeSeries, String helpID) {
        // Get export method from user
        final String questionText = "How do you want to export the pixel values?\n"; /*I18N*/
        final int method = SelectExportMethodDialog.run(SnapApp.getDefault().getMainFrame(),
                DLG_TITLE, questionText, helpID);

        final PrintWriter writer;
        final StringBuffer clipboardText;
        final int initialBufferSize = 256000;
        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            final StringWriter stringWriter = new StringWriter(initialBufferSize);
            writer = new PrintWriter(stringWriter);
            clipboardText = stringWriter.getBuffer();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            final File file = fetchOutputFile(helpID);
            if (file == null) {
                return; // Cancel
            }
            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                Dialogs.showError(DLG_TITLE, ERR_MSG_BASE + "Failed to create file '" + file + "':\n" + e.getMessage());
                return; // Error
            }
            writer = new PrintWriter(new BufferedWriter(fileWriter, initialBufferSize));
            clipboardText = null;
        } else {
            return; // Cancel
        }
        ExportTimeBasedText exportTimeBasedText = new ExportTimeBasedText(parent, timeSeries, writer, clipboardText);
        exportTimeBasedText.executeWithBlocking();
    }

    private static File fetchOutputFile(String helpID) {
        SnapApp snapApp = SnapApp.getDefault();
        final String lastDir = snapApp.getPreferences().get(EXPORT_DIR_PREFERENCES_KEY, SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final SnapFileChooser fileChooser = new SnapFileChooser();
        HelpCtx.setHelpIDString(fileChooser, helpID);
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(csvFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(snapApp.getInstanceName() + " - " + "Export time series as CSV file...");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int result = fileChooser.showSaveDialog(snapApp.getMainFrame());
        File file = fileChooser.getSelectedFile();

        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            snapApp.getPreferences().put(EXPORT_DIR_PREFERENCES_KEY, currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        if (file == null || file.getName().isEmpty()) {
            return null;
        }

        if (!TimeSeriesExportHelper.promptForOverwrite(file)) {
            return null;
        }
        return file;
    }
}
