package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.glayer.support.ImageLayer;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.spectrallibrary.controller.SpectralLibraryController;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.AddAttributeDialog;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.rcp.spectrallibrary.ui.noise.SpectralNoiseReductionProfilesDialog;
import org.esa.snap.rcp.spectrallibrary.ui.resampling.SpectralResamplingProfilesDialog;
import org.esa.snap.rcp.spectrallibrary.wiring.EngineAccess;
import org.esa.snap.speclib.io.CompositeSpectralLibraryIO;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.esa.snap.speclib.io.SpectralLibraryIODelegate;
import org.esa.snap.speclib.model.*;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.SimpleFeatureFigure;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.esa.snap.ui.product.spectrum.SpectrumBand;
import org.esa.snap.ui.product.spectrum.SpectrumChooser;
import org.esa.snap.ui.product.spectrum.SpectrumStrokeProvider;
import org.locationtech.jts.geom.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


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
    private volatile Map<String, Set<String>> selectedBandsBySpectrum = null;
    private volatile Set<String> defaultAxisBandNames = null;

    private BandGroupsManager bandGroupsManager;

    private final Map<UUID, FilterState> filterStateByLibrary = new HashMap<>();
    private UUID lastActiveLibraryId = null;

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
        wireAddProfilesCoordinatesToVectorLayer();
        wireApplyNoiseReduction();
        wireApplyResampling();
        wireProfileColorButton();
        wireTableEditing();
        wireImportExport();
        wireExtraction();
        wirePreviewAddButtons();
        wireBandFilter();
    }


    private void wireToolbarBasics() {
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

            controller.setPreviewProfilesLimited(selected);
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

            String name = JOptionPane.showInputDialog(panel, "Library name:", "Library");
            if (name == null) {
                return;
            }
            name = name.trim();
            if (name.isEmpty()) {
                return;
            }

            saveFilterState(lastActiveLibraryId != null ? lastActiveLibraryId : vm.getActiveLibraryId().orElse(null));

            List<Band> orderedCandidates = SpectralLibraryUtils.collectBandsInAutoGroupingOrder(product);
            defaultAxisBandNames = SpectralLibraryUtils.selectAxisBandsUniqueByWavelength(orderedCandidates).bandNames();
            selectedBandsBySpectrum = null;

            SpectralLibrary created = controller.createLibraryFromBands(name, orderedCandidates);
            UUID newId = created != null ? created.getId() : vm.getActiveLibraryId().orElse(null);

            saveFilterState(newId);
            lastActiveLibraryId = newId;
        });

        panel.getLibraryCombo().addActionListener(e -> {
            Object sel = panel.getLibraryCombo().getSelectedItem();
            if (!(sel instanceof SpectralLibrary lib) || lib.getId() == null) {
                return;
            }

            UUID newId = lib.getId();
            UUID curId = vm.getActiveLibraryId().orElse(null);
            if (Objects.equals(curId, newId)) {
                return;
            }

            saveFilterState(curId);
            controller.setActiveLibrary(newId);

            ProductSceneView view = viewProvider.getSelectedProductSceneView();
            Product product = view != null ? view.getProduct() : null;
            restoreOrInitFilterState(newId, product);

            lastActiveLibraryId = newId;
        });
    }

    private void wireAddProfilesCoordinatesToVectorLayer() {
        panel.getAddProfilesCoordinatesToVectorLayer().addActionListener(e -> {
            ProductSceneView view = viewProvider.getSelectedProductSceneView();
            Product product = view != null ? view.getProduct() : null;

            if (product == null) {
                vm.setStatus(UiStatus.warn("No product view selected"));
                return;
            }

            int[] viewRows = panel.getLibraryTable().getSelectedRows();
            if (viewRows == null || viewRows.length == 0) {
                vm.setStatus(UiStatus.warn("No profiles selected"));
                return;
            }

            final List<UUID> selectedProfiles = new ArrayList<>();
            for (int vr : viewRows) {
                int mr = panel.getLibraryTable().convertRowIndexToModel(vr);
                UUID pid = panel.getLibraryTableModel().getIdAt(mr);
                if (pid != null) {
                    selectedProfiles.add(pid);
                }
            }

            String defaultLayerName = "spectral_profiles";
            String layerName = JOptionPane.showInputDialog(
                    panel,
                    "Vector layer name:",
                    defaultLayerName
            );

            if (layerName == null) {
                return;
            }

            layerName = layerName.trim();
            if (layerName.isEmpty()) {
                vm.setStatus(UiStatus.warn("Layer name is empty"));
                return;
            }

            controller.addProfilesAsVectorLayer(product, selectedProfiles, layerName);
        });
    }

    private void wireApplyNoiseReduction() {
        panel.getApplySpectralNoiseReduction().addActionListener(e -> {
            UUID libId = vm.getActiveLibraryId().orElse(null);
            if (libId == null) {
                vm.setStatus(UiStatus.warn("No active library"));
                return;
            }

            int[] viewRows = panel.getLibraryTable().getSelectedRows();
            if (viewRows == null || viewRows.length == 0) {
                vm.setStatus(UiStatus.warn("No profiles selected"));
                return;
            }

            SpectralLibrary lib = getSelectedLibraryFromCombo();
            if (lib == null) {
                vm.setStatus(UiStatus.warn("No active library"));
                return;
            }

            List<UUID> profileIds = new ArrayList<>(viewRows.length);
            for (int viewRow : viewRows) {
                int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
                UUID id = panel.getLibraryTableModel().getIdAt(modelRow);
                if (id != null) {
                    profileIds.add(id);
                }
            }

            if (profileIds.isEmpty()) {
                vm.setStatus(UiStatus.warn("No valid profiles selected"));
                return;
            }

            Optional<SpectralNoiseReductionProfilesDialog.Result> resultOpt =
                    SpectralNoiseReductionProfilesDialog.showDialog(panel, lib.getName(), profileIds.size());

            if (resultOpt.isEmpty()) {
                return;
            }

            SpectralNoiseReductionProfilesDialog.Result result = resultOpt.get();

            if (result.settings() == null) {
                vm.setStatus(UiStatus.warn("No noise reduction settings provided"));
                return;
            }

            controller.applySpectralNoiseReduction(
                    libId,
                    profileIds,
                    result.settings(),
                    result.saveMode(),
                    result.nameSuffix(),
                    result.newLibraryName()
            );
        }
        );
    }

    private void wireApplyResampling() {
        panel.getApplySpectralResampling().addActionListener(e -> {
                    UUID libId = vm.getActiveLibraryId().orElse(null);
                    if (libId == null) {
                        vm.setStatus(UiStatus.warn("No active library"));
                        return;
                    }

                    int[] viewRows = panel.getLibraryTable().getSelectedRows();
                    if (viewRows == null || viewRows.length == 0) {
                        vm.setStatus(UiStatus.warn("No profiles selected"));
                        return;
                    }

                    SpectralLibrary lib = getSelectedLibraryFromCombo();
                    if (lib == null) {
                        vm.setStatus(UiStatus.warn("No active library"));
                        return;
                    }

                    List<UUID> profileIds = new ArrayList<>(viewRows.length);
                    for (int viewRow : viewRows) {
                        int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
                        UUID id = panel.getLibraryTableModel().getIdAt(modelRow);
                        if (id != null) {
                            profileIds.add(id);
                        }
                    }

                    if (profileIds.isEmpty()) {
                        vm.setStatus(UiStatus.warn("No valid profiles selected"));
                        return;
                    }

                    Optional<SpectralResamplingProfilesDialog.Result> resultOpt =
                            SpectralResamplingProfilesDialog.showDialog(panel, lib.getName(), profileIds.size());

                    if (resultOpt.isEmpty()) {
                        return;
                    }

                    SpectralResamplingProfilesDialog.Result result = resultOpt.get();

                    if (result.settings() == null) {
                        vm.setStatus(UiStatus.warn("No noise reduction settings provided"));
                        return;
                    }

                    controller.applySpectralResampling(
                            libId,
                            profileIds,
                            result.settings(),
                            result.saveMode(),
                            result.nameSuffix(),
                            result.newLibraryName()
                    );
                }
        );
    }

    private void wireProfileColorButton() {
        panel.getChangePreviewColorButton().addActionListener(e -> {
            UUID libId = vm.getActiveLibraryId().orElse(null);
            if (libId == null) {
                vm.setStatus(UiStatus.warn("No active library"));
                return;
            }

            int[] viewRows = panel.getLibraryTable().getSelectedRows();
            if (viewRows == null || viewRows.length == 0) {
                vm.setStatus(UiStatus.warn("No profiles selected"));
                return;
            }

            UUID firstId = panel.getLibraryTableModel().getIdAt(panel.getLibraryTable().convertRowIndexToModel(viewRows[0]));
            Color initial = vm.getProfileColors(libId).get(firstId);
            if (initial == null) {
                initial = Color.BLUE;
            }

            Color chosen = JColorChooser.showDialog(panel, "Choose Preview Color", initial);
            if (chosen == null) {
                return;
            }

            Map<UUID, Color> updates = new HashMap<>();
            for (int vr : viewRows) {
                int mr = panel.getLibraryTable().convertRowIndexToModel(vr);
                UUID pid = panel.getLibraryTableModel().getIdAt(mr);
                if (pid != null) {
                    updates.put(pid, chosen);
                }
            }

            vm.setProfileColors(libId, updates);
            vm.setStatus(UiStatus.info("Preview color set (" + updates.size() + ")"));
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

    private void wireExtraction() {
        panel.getExtractSelectedPinsButton().addActionListener(e -> extractPins(true));
        panel.getExtractAllPinsButton().addActionListener(e -> extractPins(false));
        panel.getExtractAtCursorToggle().addActionListener(e -> {
            boolean armed = panel.getExtractAtCursorToggle().isSelected();
            if (armed) {
                attachCursorHooks();
            }
            else {
                detachCursorHooks();
            }
        });
        panel.getExtractSelectedGeometryButton().addActionListener(e -> extractFromGeometry());
    }

    private void wirePreviewAddButtons() {
        panel.getAddAllPreviewButton().addActionListener(e -> controller.addAllPreviewToLibrary());
        panel.getAddSelectedPreviewButton().addActionListener(e -> controller.addSelectedPreviewToLibrary());
    }

    private void wireBandFilter() {
        panel.getFilterButton().addActionListener(e -> {
            ProductSceneView view = viewProvider.getSelectedProductSceneView();
            if (view == null || view.getProduct() == null) {
                vm.setStatus(UiStatus.warn("No product view selected"));
                return;
            }

            Product product = view.getProduct();
            RasterDataNode raster = view.getRaster();
            if (raster == null) {
                vm.setStatus(UiStatus.warn("No raster selected"));
                return;
            }

            DisplayableSpectrum[] spectra = buildSpectraForChooser(product, raster, getAllowedBands(product));
            if (spectra.length == 0) {
                vm.setStatus(UiStatus.warn("No spectral bands available"));
                return;
            }

            SpectrumChooser chooser = new SpectrumChooser(SwingUtilities.getWindowAncestor(panel), spectra);
            if (chooser.show() != AbstractDialog.ID_OK) {
                return;
            }

            DisplayableSpectrum[] chosen = chooser.getSpectra();
            Map<String, Set<String>> selectedBySpec = new LinkedHashMap<>();
            Map<String, Set<String>> allBySpec = new LinkedHashMap<>();

            for (DisplayableSpectrum s : chosen) {
                if (s == null) {
                    continue;
                }

                Set<String> all = new LinkedHashSet<>();
                for (Band sb : s.getSpectralBands()) {
                    if (sb != null) {
                        all.add(sb.getName());
                    }
                }
                if (!all.isEmpty()) {
                    allBySpec.put(s.getName(), Collections.unmodifiableSet(all));
                }

                if (!s.isSelected()) {
                    continue;
                }

                Band[] selBands = s.getSelectedBands();
                if (selBands == null || selBands.length == 0) {
                    continue;
                }

                Set<String> sel = new LinkedHashSet<>();
                for (Band b : selBands) {
                    if (b != null) {
                        sel.add(b.getName());
                    }
                }
                if (!sel.isEmpty()) {
                    selectedBySpec.put(s.getName(), Collections.unmodifiableSet(sel));
                }
            }

            selectedBandsBySpectrum = selectedBySpec.isEmpty() ? null : Collections.unmodifiableMap(selectedBySpec);
            saveFilterState(vm.getActiveLibraryId().orElse(null));
        });
    }

    private void extractPins(boolean selectedOnly) {
        ProductSceneView view = viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            return;
        }

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        Product product = view.getProduct();
        List<Placemark> pins = selectedOnly
                ? Arrays.asList(view.getSelectedPins())
                : Arrays.asList(product.getPinGroup().toArray(new Placemark[0]));
        pins = pins.stream().filter(Objects::nonNull).toList();

        if (pins.isEmpty()) {
            vm.setStatus(UiStatus.warn(selectedOnly ? "No selected pins" : "No pins in product"));
            return;
        }

        int level = 0;
        List<Placemark> finalPins = pins;
        final String prefix = panel.getProfileNamePrefix();
        withResolvedBandSelection(view, lib, (p, axis, unit, bands, selectedNames)
                -> controller.extractPreviewFromPins(p, axis, unit, bands, finalPins, level, selectedNames, prefix));
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

    private void extractFromGeometry() {
        ProductSceneView view = viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            return;
        }

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        SimpleFeatureFigure[] geometries = view.getFeatureFigures(true);
        if (geometries == null || geometries.length == 0) {
            vm.setStatus(UiStatus.warn("No Geometry selected in ProductSceneView."));
            return;
        }

        List<PixelPos> allPixels = new ArrayList<>();
        for (SimpleFeatureFigure f : geometries) {
            Geometry geometry = (Geometry) f.getSimpleFeature().getDefaultGeometry();
            List<PixelPos> pixels = SpectralLibraryUtils.pixelsFromGeometry(view, geometry);
            allPixels.addAll(pixels);
        }

        if (allPixels.isEmpty()) {
            vm.setStatus(UiStatus.warn("No pixels found in geometries."));
        }

        int level = 0;
        final String prefix = panel.getProfileNamePrefix();
        withResolvedBandSelection(view, lib, (p, axis, unit, bands, selectedNames)
                -> controller.extractPreviewFromPixels(p, axis, unit, bands, allPixels, level, selectedNames, prefix));
    }

    private void extractCursorOnce() {
        ProductSceneView view = currentView != null ? currentView : viewProvider.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) {
            vm.setStatus(UiStatus.warn("No product view selected"));
            return;
        }

        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library (axis required)"));
            return;
        }

        int level = (cursorLevel >= 0) ? cursorLevel : ImageLayer.getLevel(view.getRaster().getMultiLevelModel(), view.getViewport());
        final String prefix = panel.getProfileNamePrefix();
        withResolvedBandSelection(view, lib, (p, axis, unit, bands, selectedNames)
                -> controller.extractPreviewAtCursor(p, axis, unit, bands, cursorX, cursorY, level, selectedNames, prefix));
    }


    private SpectralLibrary getSelectedLibraryFromCombo() {
        Object sel = panel.getLibraryCombo().getSelectedItem();
        return (sel instanceof SpectralLibrary lib) ? lib : null;
    }

    private DisplayableSpectrum[] buildSpectraForChooser(Product product, RasterDataNode raster, Set<String> allowedBandNames) {
        List<Band> spectralBands = new ArrayList<>();

        for (Band b : product.getBands()) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                continue;
            }
            spectralBands.add(b);
        }
        if (spectralBands.isEmpty()) {
            return new DisplayableSpectrum[0];
        }

        Map<String, Set<String>> preSel = selectedBandsBySpectrum;
        List<DisplayableSpectrum> spectra = new ArrayList<>();
        Set<String> usedBandNames = new HashSet<>();


        BandGroupsManager bgm = getBandGroupsManagerOrNull();
        if (bgm != null) {
            BandGroup[] groups = bgm.getGroupsMatchingProduct(product);
            for (int gi = 0; gi < groups.length; gi++) {
                BandGroup g = groups[gi];
                if (g == null) {
                    continue;
                }
                DisplayableSpectrum s = new DisplayableSpectrum(g.getName(), gi);
                s.setLineStyle(SpectrumStrokeProvider.getStroke(gi));
                Set<String> wanted = preSel != null ? preSel.get(s.getName()) : null;

                for (String bandName : g.getMatchingBandNames(product)) {
                    Band b = product.getBand(bandName);
                    if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                        continue;
                    }

                    boolean selected = (preSel == null)
                            ? (allowedBandNames != null && allowedBandNames.contains(b.getName()))
                            : (wanted != null && wanted.contains(b.getName()));
                    s.addBand(new SpectrumBand(b, selected));
                    usedBandNames.add(b.getName());
                }

                if (preSel != null) {
                    s.setSelected(wanted != null && s.getSelectedBands().length > 0);
                } else {
                    s.setSelected(s.getSelectedBands().length > 0);
                }

                if (s.getSpectralBands().length > 0) {
                    spectra.add(s);
                }
            }
        }

        var auto = product.getAutoGrouping();
        List<DisplayableSpectrum> autoSpectra = new ArrayList<>();
        if (auto != null) {
            int base = spectra.size();
            int i = 0;
            Iterator<String[]> it = auto.iterator();

            while (it.hasNext()) {
                String name = String.join("_", it.next());
                DisplayableSpectrum s = new DisplayableSpectrum(name, base + i);
                s.setLineStyle(SpectrumStrokeProvider.getStroke(base + i));
                s.setSelected((false));

                autoSpectra.add(s);
                i++;
            }

            for (Band b : spectralBands) {
                int idx = auto.indexOf(b.getName());
                if (idx >= 0 && idx < autoSpectra.size()) {
                    DisplayableSpectrum s = autoSpectra.get(idx);
                    Set<String> wanted = preSel != null ? preSel.get(s.getName()) : null;

                    boolean selected = (preSel == null)
                            ? (allowedBandNames != null && allowedBandNames.contains(b.getName()))
                            : (wanted != null && wanted.contains(b.getName()));
                    s.addBand(new SpectrumBand(b, selected));
                    usedBandNames.add(b.getName());
                }
            }

            if (preSel != null) {
                for (DisplayableSpectrum s2 : autoSpectra) {
                    Set<String> wanted2 = preSel.get(s2.getName());
                    s2.setSelected(wanted2 != null && s2.getSelectedBands().length > 0);
                }
            } else {
                for (DisplayableSpectrum s2 : autoSpectra) {
                    s2.setSelected(s2.getSelectedBands().length > 0);
                }
            }

            spectra.addAll(autoSpectra);
        }

        DisplayableSpectrum rest = new DisplayableSpectrum("Ungrouped spectral bands", spectra.size());
        rest.setLineStyle(SpectrumStrokeProvider.getStroke(spectra.size()));
        Set<String> wantedRest = preSel != null ? preSel.get(rest.getName()) : null;

        for (Band b : spectralBands) {
            if (!usedBandNames.contains(b.getName())) {
                boolean selected = (preSel == null)
                        ? (allowedBandNames != null && allowedBandNames.contains(b.getName()))
                        : (wantedRest != null && wantedRest.contains(b.getName()));
                rest.addBand(new SpectrumBand(b, selected));
            }
        }

        if (rest.getSpectralBands().length > 0) {
            if (preSel != null) {
                rest.setSelected(wantedRest != null && rest.getSelectedBands().length > 0);
            } else {
                rest.setSelected(rest.getSelectedBands().length > 0);
            }
            spectra.add(rest);
        }

        if (preSel == null) {
            DisplayableSpectrum chosenDefault = null;

            if (auto != null) {
                int idx = auto.indexOf(raster.getName());
                if (idx >= 0) {
                    for (DisplayableSpectrum s : spectra) {
                        if (s.isSelected()) {
                            chosenDefault = s;
                            break;
                        }
                    }
                }
            }

            if (chosenDefault == null && !spectra.isEmpty()) {
                chosenDefault = spectra.get(0);
                chosenDefault.setSelected(true);
            }
        }

        return spectra.toArray(new DisplayableSpectrum[0]);
    }


    private BandGroupsManager getBandGroupsManagerOrNull() {
        if (bandGroupsManager != null) {
            return bandGroupsManager;
        }
        try {
            bandGroupsManager = BandGroupsManager.getInstance();
            return bandGroupsManager;
        } catch (Exception ex) {
            vm.setStatus(UiStatus.warn("BandGroupsManager not available: " + ex.getMessage()));
            return null;
        }
    }



    private void doImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Spectral Library");
        fileChooser.setApproveButtonText("Import");
        fileChooser.setAcceptAllFileFilterUsed(true);

        for (FileFilter filter : buildFormatFilters()) {
            fileChooser.addChoosableFileFilter(filter);
        }

        int ok = fileChooser.showOpenDialog(panel);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final File file = fileChooser.getSelectedFile();
        if (!EngineAccess.libraryIO().canRead(file.toPath())) {
            vm.setStatus(UiStatus.warn("Unsupported file format: " + file.getName()));
            return;
        }
        controller.importLibraryFromFile(file);
    }

    private void doExport() {
        if (vm.getActiveLibraryId().isEmpty()) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        List<FileNameExtensionFilter> filters = buildFormatFilters();
        if (filters.isEmpty()) {
            vm.setStatus(UiStatus.warn("No export format available"));
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Spectral Library");
        fileChooser.setApproveButtonText("Export");
        fileChooser.setAcceptAllFileFilterUsed(false);

        for (FileNameExtensionFilter filter : filters) {
            fileChooser.addChoosableFileFilter(filter);
        }
        fileChooser.setFileFilter(filters.getFirst());

        String base = resolveExportBaseName();
        String defaultExtension = filters.getFirst().getExtensions()[0];
        fileChooser.setSelectedFile(new File(base + "." + defaultExtension));

        int ok = fileChooser.showSaveDialog(panel);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = ensureExtension(fileChooser.getSelectedFile(), (FileNameExtensionFilter) fileChooser.getFileFilter());

        controller.exportActiveLibraryToFile(file);
    }



    private List<FileNameExtensionFilter> buildFormatFilters() {
        SpectralLibraryIO io = EngineAccess.libraryIO();
        List<FileNameExtensionFilter> filters = new ArrayList<>();

        if (io instanceof CompositeSpectralLibraryIO composite) {
            for (SpectralLibraryIODelegate delegate : composite.getDelegates()) {
                List<String> exts = delegate.getFileExtensions();

                if (!exts.isEmpty()) {
                    String label = formatLabel(delegate.getClass().getSimpleName(), exts);
                    filters.add(new FileNameExtensionFilter(label, exts.toArray(new String[0])));
                }
            }
        } else {
            List<String> exts = io.getFileExtensions();
            if (!exts.isEmpty()) {
                filters.add(new FileNameExtensionFilter(
                        "Spectral Libraries (" + String.join(", *.", exts) + ")",
                        exts.toArray(new String[0]))
                );
            }
        }
        return filters;
    }

    private static String formatLabel(String className, List<String> exts) {
        String extList = exts.stream()
                .map(e -> "*." + e)
                .collect(Collectors.joining(", "));
        String name = className
                .replace("SpectralLibraryIO", "")
                .replace("GeoJson", "GeoJSON")
                .replace("Envi", "ENVI");
        return name + " Spectral Library (" + extList + ")";
    }

    private String resolveExportBaseName() {
        Object sel = panel.getLibraryCombo().getSelectedItem();

        if (sel instanceof SpectralLibrary lib && lib.getName() != null && !lib.getName().isBlank()) {
            return lib.getName().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        }
        return "spectral-library";
    }

    private static File ensureExtension(File file, FileNameExtensionFilter filter) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        for (String ext : filter.getExtensions()) {
            if (name.endsWith("." + ext)) {
                return file;
            }
        }
        return new File(file.getParentFile(), file.getName() + "." + filter.getExtensions()[0]);
    }



    @FunctionalInterface
    private interface ExtractionInvoker {
        void invoke(Product product, SpectralAxis axis, String unitToUse, List<Band> bands, Set<String> selectedBandNamesOrNull);
    }


    private void withResolvedBandSelection(ProductSceneView view,
                                           SpectralLibrary lib,
                                           ExtractionInvoker invoker) {
        Product product = view.getProduct();
        Map<String, Set<String>> sel = selectedBandsBySpectrum;

        Set<String> axisNames = new LinkedHashSet<>(getAllowedBands(product));
        if (axisNames.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectral axis bands"));
            return;
        }

        Set<String> selectedAxisNamesOrNull = null;
        if (sel != null && !sel.isEmpty()) {
            LinkedHashSet<String> tmp = new LinkedHashSet<>();
            for (Set<String> v : sel.values()) {
                if (v != null) {
                    tmp.addAll(v);
                }
            }
            tmp.retainAll(axisNames);
            selectedAxisNamesOrNull = tmp.isEmpty() ? null : Collections.unmodifiableSet(tmp);
        }

        List<Band> bands = BandSelectionUtils.getSpectralBands(product, axisNames);
        bands = SpectralLibraryUtils.sortSpectralBandsByWavelength(bands);
        if (bands.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectral bands in axis selection"));
            return;
        }

        SpectralAxis axis = SpectralLibraryUtils.axisFromOrderedBands(bands);

        String unitToUse = Optional.ofNullable(SpectralLibraryUtils.defaultYUnitFromBands(bands))
                .filter(u -> !u.isBlank())
                .orElse(lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT));

        invoker.invoke(product, axis, unitToUse, bands, selectedAxisNamesOrNull);
    }

    private Set<String> getAllowedBands(Product product) {
        return defaultAxisBandNames != null
                ? defaultAxisBandNames
                : SpectralLibraryUtils.selectAxisBandsUniqueByWavelength(Arrays.asList(product.getBands())).bandNames();
    }

    private void saveFilterState(UUID libraryId) {
        if (libraryId == null) {
            return;
        }
        filterStateByLibrary.put(libraryId, new FilterState(SpectralLibraryUtils.copyMapOfSets(selectedBandsBySpectrum), SpectralLibraryUtils.copySet(defaultAxisBandNames)));
    }

    private void restoreOrInitFilterState(UUID libraryId, Product product) {
        if (libraryId == null) {
            selectedBandsBySpectrum = null;
            defaultAxisBandNames = null;
            return;
        }

        FilterState st = filterStateByLibrary.get(libraryId);
        if (st != null) {
            selectedBandsBySpectrum = st.selectedBandsBySpectrumOrNull;
            defaultAxisBandNames = st.defaultAxisBandNamesOrNull;
            return;
        }

        selectedBandsBySpectrum = null;
        defaultAxisBandNames = (product != null) ? SpectralLibraryUtils
                .selectAxisBandsUniqueByWavelength(SpectralLibraryUtils.collectBandsInAutoGroupingOrder(product))
                .bandNames()
                : null;

        saveFilterState(libraryId);
    }


    private static final class FilterState {
        final Map<String, Set<String>> selectedBandsBySpectrumOrNull;
        final Set<String> defaultAxisBandNamesOrNull;

        FilterState(Map<String, Set<String>> selected, Set<String> axisNames) {
            this.selectedBandsBySpectrumOrNull = selected;
            this.defaultAxisBandNamesOrNull = axisNames;
        }
    }
}

