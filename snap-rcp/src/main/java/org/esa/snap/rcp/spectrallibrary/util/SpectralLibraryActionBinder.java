package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.spectrallibrary.controller.SpectralLibraryController;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.AddAttributeDialog;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.speclib.model.*;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;


public class SpectralLibraryActionBinder {


    private static final String DEFAULT_Y_UNIT = "value";

    private final SpectralLibraryViewModel vm;
    private final SpectralLibraryController controller;
    private final SpectralLibraryPanel panel;
    private final ProductSceneViewProvider viewProvider;

    private volatile int cursorX = -1;
    private volatile int cursorY = -1;
    private volatile int cursorLevel = -1;
    private volatile boolean cursorValid = false;

    private ProductSceneView currentView;

    private final PixelPositionListener cursorTracker = new PixelPositionListener() {
        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    int pixelX, int pixelY, int level,
                                    boolean pixelPosValid, MouseEvent e) {
            cursorX = pixelX;
            cursorY = pixelY;
            cursorLevel = level;
            cursorValid = pixelPosValid;
        }

        @Override
        public void pixelPosNotAvailable() {
            cursorValid = false;
            cursorX = cursorY = cursorLevel = -1;
        }
    };

    private final MouseAdapter clickListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!panel.getExtractAtCursorToggle().isSelected()) {
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            if (!cursorValid || cursorX < 0 || cursorY < 0) {
                vm.setStatus(UiStatus.warn("Cursor position not available"));
                return;
            }

            extractCursorOnce();
        }
    };

    public interface ProductSceneViewProvider {
        ProductSceneView getSelectedProductSceneView();
    }


    public SpectralLibraryActionBinder(SpectralLibraryViewModel vm,
                                       SpectralLibraryController controller,
                                       SpectralLibraryPanel panel,
                                       ProductSceneViewProvider viewProvider) {
        this.vm = vm;
        this.controller = controller;
        this.panel = panel;
        this.viewProvider = viewProvider;
    }


    public void bind() {
        wireToolbarBasics();
        wireTableEditing();
        wireImportExport();
        wireExtractionPins();
        wireExtractionCursor();
        wirePreviewAddButtons();
    }


    private void wireToolbarBasics() {
        panel.getRefreshButton().addActionListener(e -> {
            controller.reloadLibraries();
            controller.refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.info("Refreshed"));
        });

        panel.getDeleteLibraryButton().addActionListener(e -> {
                int answer = JOptionPane.showConfirmDialog(panel, "Are you sure you want to delete this library?");
                if (answer == JOptionPane.YES_OPTION) {
                    vm.getActiveLibraryId().ifPresent(controller::deleteLibrary);
                }
            }
        );

        panel.getRenameLibraryButton().addActionListener(e -> {
            UUID uuid = vm.getActiveLibraryId().orElse(null);
            String name = JOptionPane.showInputDialog(panel, "New Library name:", "New_Library");
            if (name == null) {
                return;
            }
            name = name.trim();
            if (name.isEmpty()) {
                return;
            }

            controller.renameLibrary(uuid, name);
        });

        panel.getRemoveSelectedProfilesButton().addActionListener(e -> {
                UUID libId = vm.getActiveLibraryId().orElse(null);
                if (libId == null) {
                    return;
                }

                int[] viewRows = panel.getLibraryTable().getSelectedRows();
                if (viewRows == null || viewRows.length == 0) {
                    vm.setStatus(UiStatus.warn("No profiles selected"));
                    return;
                }

                List<UUID> ids = new ArrayList<>(viewRows.length);
                for (int viewRow : viewRows) {
                    int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
                    ids.add(panel.getLibraryTableModel().getIdAt(modelRow));
                }
                controller.removeLibraryProfiles(libId, ids);
            }
        );

        panel.getPreviewSelectedProfilesButton().addActionListener(e -> {
            int[] viewRows = panel.getLibraryTable().getSelectedRows();
            if (viewRows == null || viewRows.length == 0) {
                vm.setStatus(UiStatus.warn("No profiles selected"));
                return;
            }

            controller.clearPreview();

            List<SpectralProfile> selected = new ArrayList<>(viewRows.length);
            for (int viewRow : viewRows) {
                int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
                SpectralProfile p = panel.getLibraryTableModel().getAt(modelRow);
                if (p != null) {
                    selected.add(p);
                }
            }

            controller.setPreviewProfiles(selected);
        });

        panel.getAddAttributeButton().addActionListener(e -> {
                Optional<UUID> libId = vm.getActiveLibraryId();
                if (libId.isEmpty()) {
                    vm.setStatus(UiStatus.warn("No active library"));
                    return;
                }

                Optional<AttributeDialogResult> resOpt = AddAttributeDialog.show(panel);
                if (resOpt.isEmpty()) {
                    return;
                }

                AttributeDialogResult res = resOpt.get();

                if (res.key == null || res.key.isBlank()) {
                    vm.setStatus(UiStatus.warn("Attribute key is empty"));
                    return;
                }

                AttributeValue v;
                try {
                    v = SpectralLibraryAttributeValueParser.parseForType(res.type, res.defaultValueText);
                } catch (IllegalArgumentException ex) {
                    vm.setStatus(UiStatus.warn("Invalid default value: " + ex.getMessage()));
                    return;
                }

                AttributeDef def = new AttributeDef(
                        res.key.trim(),
                        res.type,
                        false,
                        v,
                        null,
                        null
                );

                controller.addAttributeToActiveLibrary(def, v);
            }
        );

        panel.getClearPreviewButton().addActionListener(e -> controller.clearPreview());

        panel.getCreateFromProductButton().addActionListener(e -> {
            ProductSceneView view = viewProvider.getSelectedProductSceneView();
            if (view == null || view.getProduct() == null) {
                vm.setStatus(UiStatus.warn("No product view selected"));
                return;
            }
            Product product = view.getProduct();

            List<Band> bands = Arrays.asList(product.getBands());
            String name = JOptionPane.showInputDialog(panel, "Library name:", "Library");
            if (name == null) {
                return;
            }
            name = name.trim();
            if (name.isEmpty()) {
                return;
            }

            controller.createLibraryFromBands(name, bands);
        });

        panel.getLibraryCombo().addActionListener(e -> {
            Object sel = panel.getLibraryCombo().getSelectedItem();
            if (sel instanceof SpectralLibrary lib) {
                controller.setActiveLibrary(lib.getId());
            }
        });
    }

    private void wireTableEditing() {
        panel.getLibraryTableModel().setEditHandler(
            new SpectralProfileTableModel.ProfileEditHandler() {
                @Override
                public void rename(UUID profileId, String newName) {
                    controller.renameProfileInActiveLibrary(profileId, newName);
                }
                @Override
                public void setAttr(UUID profileId, String key, AttributeValue value) {
                    controller.setAttributeInActiveLibrary(profileId, key, value);
                }
            }
        );
    }


    private void wireImportExport() {
        panel.getExportButton().addActionListener(e -> doExport());
        panel.getImportButton().addActionListener(e -> doImport());
    }


    private void wireExtractionPins() {
        panel.getExtractSelectedPinsButton().addActionListener(e -> extractPins(true));
        panel.getExtractAllPinsButton().addActionListener(e -> extractPins(false));
    }

    private void extractPins(boolean selectedOnly) {
        ProductSceneView view = viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            return;
        }
        Product product = view.getProduct();

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        List<Band> bands = BandSelectionUtils.getSpectralBands(product);
        if (bands.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectral bands in product"));
            return;
        }

        List<Placemark> pins = selectedOnly
                ? Arrays.asList(view.getSelectedPins())
                : Arrays.asList(product.getPinGroup().toArray(new Placemark[0]));

        pins = pins.stream().filter(p -> p != null).toList();
        if (pins.isEmpty()) {
            vm.setStatus(UiStatus.warn(selectedOnly ? "No selected pins" : "No pins in product"));
            return;
        }

        int level = 0;
        String yUnit = lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT);

        controller.extractPreviewFromPins(
                product,
                lib.getAxis(),
                yUnit,
                bands,
                pins,
                level,
                "pin"
        );
    }


    private void wireExtractionCursor() {
        panel.getExtractAtCursorToggle().addActionListener(e -> {
            boolean armed = panel.getExtractAtCursorToggle().isSelected();
            if (armed) {
                attachCursorHooks();
            }
            else {
                detachCursorHooks();
            }
        });
    }

    private void attachCursorHooks() {
        ProductSceneView view = viewProvider.getSelectedProductSceneView();
        if (view == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            panel.getExtractAtCursorToggle().setSelected(false);
            return;
        }

        detachCursorHooks();
        currentView = view;
        currentView.addPixelPositionListener(cursorTracker);
        currentView.getLayerCanvas().addMouseListener(clickListener);

        vm.setStatus(UiStatus.info("Cursor extraction armed (click in view)"));
    }

    private void detachCursorHooks() {
        if (currentView != null) {
            currentView.removePixelPositionListener(cursorTracker);
            currentView.getLayerCanvas().removeMouseListener(clickListener);
        }
        currentView = null;
        cursorValid = false;
        cursorX = cursorY = cursorLevel = -1;
    }

    private void extractCursorOnce() {
        ProductSceneView view = currentView != null ? currentView : viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            return;
        }

        Product product = view.getProduct();

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        List<Band> bands = BandSelectionUtils.getSpectralBands(product);
        if (bands.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectral bands in product"));
            return;
        }

        int level = (cursorLevel >= 0) ? cursorLevel : ImageLayer.getLevel(view.getRaster().getMultiLevelModel(), view.getViewport());
        String yUnit = lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT);

        controller.extractPreviewAtCursor(
                product,
                lib.getAxis(),
                yUnit,
                bands,
                cursorX,
                cursorY,
                level,
                "cursor_" + cursorX + "_" + cursorY
        );
    }


    private void wirePreviewAddButtons() {
        panel.getAddAllPreviewButton().addActionListener(e -> controller.addAllPreviewToLibrary());
        panel.getAddSelectedPreviewButton().addActionListener(e -> controller.addSelectedPreviewToLibrary());
    }


    private SpectralLibrary getSelectedLibraryFromCombo() {
        Object sel = panel.getLibraryCombo().getSelectedItem();
        return (sel instanceof SpectralLibrary lib) ? lib : null;
    }




    private void doImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Spectral Library");
        fileChooser.setApproveButtonText("Import");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Spectral Libraries (*.sli, *.hdr, *)", "sli", "hdr"));

        int ok = fileChooser.showOpenDialog(panel);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        controller.importLibraryFromFile(file);
    }

    private void doExport() {
        if (vm.getActiveLibraryId().isEmpty()) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Spectral Library");
        fileChooser.setApproveButtonText("Export");
        fileChooser.setFileFilter(new FileNameExtensionFilter("ENVI Spectral Library (*.sli)", "sli"));

        String base = "spectral-library";
        Object sel = panel.getLibraryCombo().getSelectedItem();
        if (sel instanceof SpectralLibrary lib && lib.getName() != null && !lib.getName().isBlank()) {
            base = lib.getName().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        }
        fileChooser.setSelectedFile(new File(base + ".sli"));

        int ok = fileChooser.showSaveDialog(panel);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".sli")) {
            file = new File(file.getParentFile(), file.getName() + ".sli");
        }

        controller.exportActiveLibraryToFile(file);
    }
}

