package org.esa.snap.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductSubsetByPolygon;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.loading.SwingUtils;
import org.locationtech.jts.geom.Polygon;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.logging.Logger;

/**
 * This class provides the core UI components for the new subset feature, that is Polygon Subset.
 * It provides the functionality to create and control the UI components which is used to build a subset polygon for a product which can be defined by the one of the following:
 * - a WKT polygon with geographic coordinates (Lat,Lon)
 * - a WKT polygon with pixel coordinates (X,Y)
 * - a vector file in supported formats (SHAPEFILE, KML, KMZ, GEOJSON, WKT, TXT, PLACEMARK, PNX)
 *
 * @author Adrian Draghici
 */
public class ProductSubsetByPolygonUiComponents {
    private static final String IMPORT_VECTOR_FILE_BUTTON_TEXT = "Import from Vector file";
    private static final String IMPORT_WKT_STRING_BUTTON_TEXT = "Import from WKT string";

    private final ProductSubsetByPolygon productSubsetByPolygon = new ProductSubsetByPolygon();
    private final JButton importVectorFileButton = new JButton(IMPORT_VECTOR_FILE_BUTTON_TEXT);
    private final JButton importWktStringButton = new JButton(IMPORT_WKT_STRING_BUTTON_TEXT);
    private final JTextField vectorFileField = new JTextField();
    private final JTextField pixelPolygonField = new JTextField();
    private final JTextField geoPolygonField = new JTextField();
    private final JPanel vectorFilePanel = new JPanel(new GridBagLayout());
    private final JPanel wktStringPanel = new JPanel(new GridBagLayout());
    private final JTextArea wktStringInput = new JTextArea(16, 32);
    private final JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private final Component hostComponent;
    private MetadataInspector.Metadata targetProductMetadata;

    /**
     * Creates and initialises all the internal UI components which will be used to build a subset polygon
     *
     * @param hostComponent the host UI component to which the internal UI components will be linked
     */
    public ProductSubsetByPolygonUiComponents(Component hostComponent) {
        this.hostComponent = hostComponent;
        setupVectorFilePanel();
        setupWKTStringPanel();
        setupImportVectorFileButton();
        setupImportWKTStringButton();
    }

    private void setupVectorFilePanel() {
        GridBagConstraints geobc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 10);
        vectorFilePanel.add(this.importVectorFileButton, geobc);
        geobc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 10);
        vectorFilePanel.add(this.importWktStringButton, geobc);
        geobc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 10, 0);
        vectorFilePanel.add(new JLabel("Vector file:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 10, 0);
        vectorFilePanel.add(vectorFileField, geobc);
        geobc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 10, 0);
        vectorFilePanel.add(new JLabel("Polygon (pixel/image coordinates [X,Y]):"), geobc);
        geobc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 10, 0);
        vectorFilePanel.add(pixelPolygonField, geobc);
        geobc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 10, 0);
        vectorFilePanel.add(new JLabel("Polygon (geographic/map coordinates [Lat,Lon]):"), geobc);
        geobc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 10, 0);
        vectorFilePanel.add(geoPolygonField, geobc);
        vectorFileField.setEditable(false);
        vectorFileField.setPreferredSize(new Dimension(100, 25));
        pixelPolygonField.setEditable(false);
        pixelPolygonField.setPreferredSize(new Dimension(100, 25));
        geoPolygonField.setEditable(false);
        geoPolygonField.setPreferredSize(new Dimension(100, 25));
    }

    private void setupWKTStringPanel() {
        wktStringInput.setEditable(true);
        final JRadioButton geoCoordRadio = new JRadioButton("Geo Coordinates");
        final ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        GridBagConstraints geobc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 2, 1, 0, 0);
        wktStringPanel.add(new JLabel("Geometry Well-Known-Text (WKT):"), geobc);
        geobc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 2, 1, 0, 0);
        wktStringPanel.add(new JScrollPane(wktStringInput), geobc);
        geobc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        wktStringPanel.add(pixelCoordRadio, geobc);
        geobc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        wktStringPanel.add(geoCoordRadio, geobc);
        pixelCoordRadio.setSelected(true);
    }

    private void setupImportVectorFileButton() {
        this.importVectorFileButton.setMnemonic('v');
        this.importVectorFileButton.setToolTipText("Use coordinates from a Vector file");
        this.importVectorFileButton.addActionListener(e -> executeImportVectorFileButtonAction());
    }

    private void setupImportWKTStringButton() {
        this.importWktStringButton.setMnemonic('w');
        this.importWktStringButton.setToolTipText("Use coordinates from a WKT String");
        this.importWktStringButton.addActionListener(e -> executeImportWKTStringButtonAction());
    }

    /**
     * Provides the instance of the class {@code ProductSubsetByPolygon} which contains the core components for the polygon subset feature
     *
     * @return the instance of the class {@code ProductSubsetByPolygon} which contains the core components for the polygon subset feature
     */
    public ProductSubsetByPolygon getProductSubsetByPolygon() {
        return productSubsetByPolygon;
    }

    private void executeImportVectorFileButtonAction() {
        unloadPolygon();
        importGeometryFromVectorFile(this.targetProductMetadata);
    }

    private void executeImportWKTStringButtonAction() {
        unloadPolygon();
        importGeometryFromWKTString(this.targetProductMetadata);
    }

    /**
     * Clears the polygon and all the related UI components
     */
    public void unloadPolygon() {
        this.productSubsetByPolygon.clear();
        this.wktStringInput.setText("");
        this.vectorFileField.setText("");
        this.pixelPolygonField.setText("");
        this.geoPolygonField.setText("");
    }

    private void importGeometryFromVectorFile(MetadataInspector.Metadata targetProductMetadata) {
        final SnapFileFilter filter = this.productSubsetByPolygon.getVectorFileFilter();
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setDialogTitle("Import Vector File");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(true);
        final int result = fileChooser.showOpenDialog(this.hostComponent);
        if (result == JFileChooser.APPROVE_OPTION) {
            final File vectorFile = fileChooser.getSelectedFile();
            importGeometryFromVectorFile(vectorFile, targetProductMetadata);
        }
    }

    /**
     * Imports the polygon from the provided vector file for the specified product metadata
     *
     * @param vectorFile            the vector file from which the polygon will be loaded, with the one of the following supported formats:
     *                              <ul>SHAPEFILE (*.shp)</ul>
     *                              <ul>KML (*.kml)</ul>
     *                              <ul>KMZ (*.kmz)</ul>
     *                              <ul>GEOJSON (*.json)</ul>
     *                              <ul>WKT (*.wkt)</ul>
     *                              <ul>TXT (*.txt)</ul>
     *                              <ul>PLACEMARK (*.placemark)</ul>
     *                              <ul>PNX (*.pnx)</ul>
     * @param targetProductMetadata the metadata of the product for which the polygon will be loaded
     */
    public void importGeometryFromVectorFile(File vectorFile, MetadataInspector.Metadata targetProductMetadata) {
        try {
            readGeometryFromVectorFile(this.productSubsetByPolygon, vectorFile, targetProductMetadata);
            this.vectorFileField.setText(vectorFile.getAbsolutePath());
            printPixelPolygonOnField(this.productSubsetByPolygon.getSubsetPolygon());
            printGeoPolygonOnField(this.productSubsetByPolygon.getSubsetGeoPolygon());
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                final String msg = "Importing the polygon from a vector file, failed. Reason: " + e.getCause().getMessage();
                Logger.getLogger(ProductSubsetByPolygonUiComponents.class.getName()).warning(msg);
                if (this.hostComponent != null) {
                    JOptionPane.showMessageDialog(null, msg, "Import polygon from a vector file, failed", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void importGeometryFromWKTString(MetadataInspector.Metadata targetProductMetadata) {
        final ModalDialog modalDialog = new ModalDialog(null, "Import WKT string", ModalDialog.ID_OK_CANCEL, null);
        modalDialog.setContent(wktStringPanel);
        modalDialog.center();
        wktStringInput.setText("");
        if (modalDialog.show() == ModalDialog.ID_OK) {
            final String wktString = wktStringInput.getText();
            importGeometryFromWKTString(wktString, targetProductMetadata);
        }
    }

    /**
     * Imports the polygon from the WKT string
     *
     * @param wktString             the WKT string from which the polygon will be loaded, in which all the coordinates can be pixel coordinates (X,Y) or geo coordinates (Lat,Lon), but not mixed
     * @param targetProductMetadata the metadata of the product for which the polygon will be loaded
     */
    public void importGeometryFromWKTString(String wktString, MetadataInspector.Metadata targetProductMetadata) {
        try {
            readGeometryFromWKTString(this.productSubsetByPolygon, wktString, pixelCoordRadio.isSelected(), targetProductMetadata);
            this.vectorFileField.setText("");
            printPixelPolygonOnField(this.productSubsetByPolygon.getSubsetPolygon());
            printGeoPolygonOnField(this.productSubsetByPolygon.getSubsetGeoPolygon());
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                final String msg = "Importing the polygon from a WKT string, failed. Reason: " + e.getCause().getMessage();
                Logger.getLogger(ProductSubsetByPolygonUiComponents.class.getName()).warning(msg);
                if (this.hostComponent != null) {
                    JOptionPane.showMessageDialog(null, msg, "Import polygon from WKT string, failed", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void printPixelPolygonOnField(Polygon polygon) {
        if (polygon != null && !polygon.isEmpty()) {
            this.pixelPolygonField.setText(polygon.toText());
        }
    }

    private void printGeoPolygonOnField(Polygon polygon) {
        if (polygon != null && !polygon.isEmpty()) {
            this.geoPolygonField.setText(polygon.toText());
        }
    }

    private void readGeometryFromVectorFile(final ProductSubsetByPolygon productSubsetByPolygon, final File vectorFile, final MetadataInspector.Metadata targetProductMetadata) throws Exception {
        final ProgressMonitorSwingWorker<Object, Object> worker = new ProgressMonitorSwingWorker<>(this.hostComponent, "Loading vector data") {
            @Override
            protected Object doInBackground(ProgressMonitor pm) throws Exception {
                productSubsetByPolygon.loadPolygonFromVectorFile(vectorFile, targetProductMetadata, pm);
                return null;
            }

            @Override
            protected void done() {
                super.done();
            }
        };

        worker.executeWithBlocking();
        worker.get();
    }

    private void readGeometryFromWKTString(final ProductSubsetByPolygon productSubsetByPolygon, final String wktString, final boolean pixelCoordinates, final MetadataInspector.Metadata targetProductMetadata) throws Exception {
        final ProgressMonitorSwingWorker<Object, Object> worker = new ProgressMonitorSwingWorker<>(this.hostComponent, "Loading WKT string") {
            @Override
            protected Object doInBackground(ProgressMonitor pm) throws Exception {
                productSubsetByPolygon.loadPolygonFromWKTString(wktString, pixelCoordinates, targetProductMetadata, pm);
                return null;
            }

            @Override
            protected void done() {
                super.done();
            }
        };

        worker.executeWithBlocking();
        worker.get();
    }

    /**
     * Sets the target product metadata which will be used when loading the polygon
     *
     * @param targetProductMetadata the target product metadata which will be used when loading the polygon
     */
    public void setTargetProductMetadata(MetadataInspector.Metadata targetProductMetadata) {
        this.targetProductMetadata = targetProductMetadata;
        unloadPolygon();
    }

    /**
     * Provides the {@code JPanel} object which contains all the internal UI components used to load and control the polygon for subset
     *
     * @return the {@code JPanel} object which contains all the internal UI components used to load and control the polygon for subset
     */
    public JPanel getImportVectorFilePanel() {
        return this.vectorFilePanel;
    }
}
