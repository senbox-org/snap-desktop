package org.esa.snap.pixex.visat;

import org.esa.snap.core.datamodel.GenericPlacemarkDescriptor;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.pixex.PixExOpUtils;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.SnapFileChooser;
import org.opengis.feature.simple.SimpleFeature;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

class AddCsvFileAction extends AbstractAction {

    private static final String LAST_OPEN_CSV_DIR = "beam.pixex.lastOpenCsvDir";

    private final AppContext appContext;
    private final JPanel parent;
    private final CoordinateTableModel tableModel;

    AddCsvFileAction(AppContext appContext, CoordinateTableModel tableModel, JPanel parent) {
        super("Add measurements from CSV file...");
        this.appContext = appContext;
        this.parent = parent;
        this.tableModel = tableModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PropertyMap preferences = appContext.getPreferences();
        final SnapFileChooser fileChooser = getFileChooser(
                preferences.getPropertyString(LAST_OPEN_CSV_DIR, SystemUtils.getUserHomeDir().getPath()));
        int answer = fileChooser.showDialog(parent, "Select");
        if (answer == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            preferences.setPropertyString(LAST_OPEN_CSV_DIR, selectedFile.getParent());
            try {
                final List<SimpleFeature> extendedFeatures = PixExOpUtils.extractFeatures(selectedFile);
                for (SimpleFeature extendedFeature : extendedFeatures) {
                    final GenericPlacemarkDescriptor placemarkDescriptor = new GenericPlacemarkDescriptor(
                            extendedFeature.getFeatureType());
                    final Placemark placemark = placemarkDescriptor.createPlacemark(extendedFeature);
                    if (extendedFeature.getAttribute("Name") != null) {
                        placemark.setName(extendedFeature.getAttribute("Name").toString());
                    }
                    setPlacemarkGeoPos(extendedFeature, placemark);
                    tableModel.addPlacemark(placemark);
                }
            } catch (IOException exception) {
                appContext.handleError(String.format("Error occurred while reading file: %s \n" +
                                                             exception.getLocalizedMessage() +
                                                             "\nPossible reason: Other char separator than tabulator used",
                                                     selectedFile), exception);
            }
        }
    }

    private void setPlacemarkGeoPos(SimpleFeature extendedFeature, Placemark placemark) throws IOException {
        final GeoPos geoPos = PixExOpUtils.getGeoPos(extendedFeature);
        placemark.setGeoPos(geoPos);
    }

    private SnapFileChooser getFileChooser(String lastDir) {
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setFileFilter(new SnapFileFilter("CSV", new String[]{".csv", ".txt", ".ascii"}, "CSV files"));
        fileChooser.setCurrentDirectory(new File(lastDir));
        return fileChooser;
    }
}
