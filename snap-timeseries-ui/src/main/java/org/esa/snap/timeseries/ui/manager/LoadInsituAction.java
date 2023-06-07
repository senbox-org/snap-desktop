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

package org.esa.snap.timeseries.ui.manager;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.timeseries.core.insitu.InsituLoader;
import org.esa.snap.timeseries.core.insitu.InsituLoaderFactory;
import org.esa.snap.timeseries.core.insitu.InsituSource;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.prefs.Preferences;

/**
 * Action for loading in situ data.
 *
 * @author Thomas Storm
 * @author Sabine Embacher
 */
class LoadInsituAction extends AbstractAction {

    private static final String PROPERTY_KEY_LAST_OPEN_INSITU_DIR = "timeseries.file.lastInsituOpenDir";

    private final AbstractTimeSeries currentTimeSeries;
    private InsituSource insituSource;

    public LoadInsituAction(AbstractTimeSeries currentTimeSeries) {
        putValue(SHORT_DESCRIPTION, "Import in-situ source file");
        putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/Import24.gif"));
        this.currentTimeSeries = currentTimeSeries;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        String lastDir = preferences.get(PROPERTY_KEY_LAST_OPEN_INSITU_DIR, SystemUtils.getUserHomeDir().getPath());
        final SnapFileChooser fileChooser = new SnapFileChooser(new File(lastDir));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogTitle("Select in-situ source file");
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
        fileChooser.setFileFilter(actualFileFilter);

        int result = fileChooser.showDialog(SnapApp.getDefault().getMainFrame(), "Select in-situ source file");    /*I18N*/
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final File selectedFile = fileChooser.getSelectedFile();
        try {
            final InsituLoader insituLoader = InsituLoaderFactory.createInsituLoader(selectedFile);
            if(insituSource != null) {
                insituSource.close();
            }
            insituSource = new InsituSource(insituLoader.loadSource());
            currentTimeSeries.setInsituSource(insituSource);
        } catch (IOException exception) {
            SystemUtils.LOG.log(Level.WARNING, "Unable to load in-situ data from '" + selectedFile + "'.", exception);
            return;
        }

        File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            preferences.put(PROPERTY_KEY_LAST_OPEN_INSITU_DIR, currentDirectory.getAbsolutePath());
        }
    }


}
