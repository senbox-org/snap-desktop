/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.statistics;

import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.statistics.output.CsvStatisticsWriter;
import org.esa.snap.statistics.output.MetadataWriter;
import org.esa.snap.statistics.output.StatisticsOutputContext;
import org.esa.snap.ui.SnapFileChooser;

import javax.media.jai.Histogram;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Storm
 */
class ExportStatisticsAsCsvAction extends AbstractAction {

    private static final String PROPERTY_KEY_EXPORT_DIR = "user.statistics.export.dir";
    private Mask[] selectedMasks;
    private final StatisticsDataProvider dataProvider;

    public ExportStatisticsAsCsvAction(StatisticsDataProvider dataProvider) {
        super("Export as CSV");
        this.dataProvider = dataProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrintStream metadataOutputStream = null;
        PrintStream csvOutputStream = null;
        String exportDir = SnapApp.getDefault().getPreferences().get(PROPERTY_KEY_EXPORT_DIR, null);
        File baseDir = null;
        if (exportDir != null) {
            baseDir = new File(exportDir);
        }
        SnapFileChooser fileChooser = new SnapFileChooser(baseDir);
        final SnapFileFilter snapFileFilter = new SnapFileFilter("CSV", new String[]{".csv", ".txt"}, "CSV files");
        fileChooser.setFileFilter(snapFileFilter);
        File outputAsciiFile;
        int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            outputAsciiFile = fileChooser.getSelectedFile();
            SnapApp.getDefault().getPreferences().put(PROPERTY_KEY_EXPORT_DIR, outputAsciiFile.getParent());
        } else {
            return;
        }
        try {
            final File metadataFile = new File(outputAsciiFile.getParent(),
                                               FileUtils.getFilenameWithoutExtension(outputAsciiFile) + "_metadata.txt");
            metadataOutputStream = new PrintStream(new FileOutputStream(metadataFile));
            csvOutputStream = new PrintStream(new FileOutputStream(outputAsciiFile));

            CsvStatisticsWriter csvStatisticsWriter = new CsvStatisticsWriter(csvOutputStream);
            final MetadataWriter metadataWriter = new MetadataWriter(metadataOutputStream);

            String[] regionIds;
            if (selectedMasks != null) {
                regionIds = new String[selectedMasks.length];
                for (int i = 0; i < selectedMasks.length; i++) {
                    if (selectedMasks[i] != null) {
                        regionIds[i] = selectedMasks[i].getName();
                    } else {
                        regionIds[i] = "\t";
                    }
                }
            } else {
                regionIds = new String[]{"full_scene"};
            }
            final String[] algorithmNames = {
                    "minimum",
                    "maximum",
                    "median",
                    "average",
                    "sigma",
                    "p90_threshold",
                    "p95_threshold",
                    "total"
            };
            final StatisticsOutputContext outputContext = StatisticsOutputContext.create(
                    new Product[]{dataProvider.getRasterDataNode().getProduct()}, algorithmNames, regionIds);
            metadataWriter.initialiseOutput(outputContext);
            csvStatisticsWriter.initialiseOutput(outputContext);

            final Map<String, Object> statistics = new HashMap<>();
            final Histogram[] histograms = dataProvider.getHistograms();
            for (int i = 0; i < histograms.length; i++) {
                final Histogram histogram = histograms[i];
                statistics.put("minimum", histogram.getLowValue(0));
                statistics.put("maximum", histogram.getHighValue(0));
                statistics.put("median", histogram.getPTileThreshold(0.5)[0]);
                statistics.put("average", histogram.getMean()[0]);
                statistics.put("sigma", histogram.getStandardDeviation()[0]);
                statistics.put("p90_threshold", histogram.getPTileThreshold(0.9)[0]);
                statistics.put("p95_threshold", histogram.getPTileThreshold(0.95)[0]);
                statistics.put("total", histogram.getTotals()[0]);
                csvStatisticsWriter.addToOutput(dataProvider.getRasterDataNode().getName(), regionIds[i], statistics);
                metadataWriter.addToOutput(dataProvider.getRasterDataNode().getName(), regionIds[i], statistics);
                statistics.clear();
            }
            csvStatisticsWriter.finaliseOutput();
            metadataWriter.finaliseOutput();
        } catch (IOException exception) {
            Dialogs.showMessage("Statistics export", "Failed to export statistics.\nAn error occurred:" +
                                                     exception.getMessage(), JOptionPane.ERROR_MESSAGE, null);
        } finally {
            if (metadataOutputStream != null) {
                metadataOutputStream.close();
            }
            if (csvOutputStream != null) {
                csvOutputStream.close();
            }
        }
//        JOptionPane.showMessageDialog(VisatApp.getApp().getApplicationWindow(),
//                                      "The statistics have successfully been exported to '" + outputAsciiFile +
//                                              "'.",
//                                      "Statistics export",
//                                      JOptionPane.INFORMATION_MESSAGE);
        Dialogs.showMessage("Statistics export", "The statistics have successfully been exported to '" +
                                                 outputAsciiFile + "'.", JOptionPane.INFORMATION_MESSAGE, null);
    }

    public void setSelectedMasks(Mask[] selectedMasks) {
        this.selectedMasks = selectedMasks;
    }
}
