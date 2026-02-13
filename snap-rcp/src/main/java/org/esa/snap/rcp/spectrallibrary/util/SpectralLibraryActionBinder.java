package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.glayer.support.ImageLayer;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.spectrallibrary.controller.SpectralLibraryController;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.AddAttributeDialog;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.speclib.model.*;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.esa.snap.ui.product.spectrum.SpectrumBand;
import org.esa.snap.ui.product.spectrum.SpectrumChooser;
import org.esa.snap.ui.product.spectrum.SpectrumStrokeProvider;

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
    private volatile Map<String, Set<String>> selectedBandsBySpectrum = null;
    private volatile Map<String, Set<String>> allBandsBySpectrum = null;
    private volatile String defaultSpectrumGroupName = null;

    private BandGroupsManager bandGroupsManager;

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
        wireBandFilter();
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

        List<Placemark> pins = selectedOnly
                ? Arrays.asList(view.getSelectedPins())
                : Arrays.asList(product.getPinGroup().toArray(new Placemark[0]));

        pins = pins.stream().filter(Objects::nonNull).toList();
        if (pins.isEmpty()) {
            vm.setStatus(UiStatus.warn(selectedOnly ? "No selected pins" : "No pins in product"));
            return;
        }

        int level = 0;
        Map<String, Set<String>> sel = selectedBandsBySpectrum;

        if (sel == null || sel.isEmpty()) {
            DisplayableSpectrum[] spectra = buildSpectraForChooser(product, view.getRaster());
            String group = defaultSpectrumGroupName;

            DisplayableSpectrum chosen = null;
            if (group != null) {
                for (DisplayableSpectrum s : spectra) {
                    if (s != null && group.equals(s.getName())) {
                        chosen = s; break;
                    }
                }
            }
            if (chosen == null) {
                for (DisplayableSpectrum s : spectra) {
                    if (s != null && s.isSelected()) {
                        chosen = s; break;
                    }
                }
            }
            if (chosen == null && spectra.length > 0) {
                chosen = spectra[0];
            }

            if (chosen == null) {
                vm.setStatus(UiStatus.warn("No spectral bands available"));
                return;
            }

            Set<String> names = new LinkedHashSet<>();
            for (Band b : chosen.getSelectedBands()) {
                if (b != null) {
                    names.add(b.getName());
                }
            }

            List<Band> bands = BandSelectionUtils.getSpectralBands(product, names);
            if (bands.isEmpty()) {
                vm.setStatus(UiStatus.warn("No spectral bands in default group"));
                return;
            }

            SpectralAxis axis = SpectralAxisUtils.axisFromBands(bands);
            String groupUnit = SpectralAxisUtils.defaultYUnitFromBands(bands);
            String unitToUse = (groupUnit == null || groupUnit.isBlank())
                    ? lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT)
                    : groupUnit;

            controller.extractPreviewFromPins(product, axis, unitToUse, bands, pins, level, null);
            return;
        }

        int totalBands = 0;

        for (var e : sel.entrySet()) {
            String groupName = e.getKey();
            Set<String> names = e.getValue();

            Set<String> allNames = (allBandsBySpectrum != null) ? allBandsBySpectrum.get(groupName) : null;
            List<Band> bands = BandSelectionUtils.getSpectralBands(product, allNames);

            if (bands.isEmpty()) {
                continue;
            }
            totalBands += bands.size();

            SpectralAxis axis = SpectralAxisUtils.axisFromBands(bands);
            String groupUnit = SpectralAxisUtils.defaultYUnitFromBands(bands);
            String unitToUse = (groupUnit == null || groupUnit.isBlank())
                    ? lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT)
                    : groupUnit;

            controller.extractPreviewFromPins(product, axis, unitToUse, bands, pins, level, names);
        }

        vm.setStatus(totalBands == 0
                ? UiStatus.warn("Band filter selected, but no matching spectral bands found")
                : UiStatus.info("Extracted " + sel.size() + " spectrum group(s)"));
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

        int level = (cursorLevel >= 0) ? cursorLevel : ImageLayer.getLevel(view.getRaster().getMultiLevelModel(), view.getViewport());
        Map<String, Set<String>> sel = selectedBandsBySpectrum;

        if (sel == null || sel.isEmpty()) {
            DisplayableSpectrum[] spectra = buildSpectraForChooser(product, view.getRaster());
            String group = defaultSpectrumGroupName;

            DisplayableSpectrum chosen = null;
            if (group != null) {
                for (DisplayableSpectrum s : spectra) {
                    if (s != null && group.equals(s.getName())) {
                        chosen = s; break;
                    }
                }
            }
            if (chosen == null) {
                for (DisplayableSpectrum s : spectra) {
                    if (s != null && s.isSelected()) {
                        chosen = s; break;
                    }
                }
            }
            if (chosen == null && spectra.length > 0) {
                chosen = spectra[0];
            }

            if (chosen == null) {
                vm.setStatus(UiStatus.warn("No spectral bands available"));
                return;
            }

            Set<String> names = new LinkedHashSet<>();
            for (Band b : chosen.getSelectedBands()) {
                if (b != null) {
                    names.add(b.getName());
                }
            }

            List<Band> bands = BandSelectionUtils.getSpectralBands(product, names);
            if (bands.isEmpty()) {
                vm.setStatus(UiStatus.warn("No spectral bands in default group"));
                return;
            }

            SpectralAxis axis = SpectralAxisUtils.axisFromBands(bands);
            String groupUnit = SpectralAxisUtils.defaultYUnitFromBands(bands);
            String unitToUse = (groupUnit == null || groupUnit.isBlank())
                    ? lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT)
                    : groupUnit;

            controller.extractPreviewAtCursor(product, axis, unitToUse, bands, cursorX, cursorY, level, null);
            return;
        }

        int extracted = 0;

        for (var e : sel.entrySet()) {
            String groupName = e.getKey();
            Set<String> names = e.getValue();

            Set<String> allNames = (allBandsBySpectrum != null) ? allBandsBySpectrum.get(groupName) : null;
            List<Band> bands = BandSelectionUtils.getSpectralBands(product, allNames);
            if (bands.isEmpty()) {
                continue;
            }

            SpectralAxis axis = SpectralAxisUtils.axisFromBands(bands);
            String groupUnit = SpectralAxisUtils.defaultYUnitFromBands(bands);
            String unitToUse = (groupUnit == null || groupUnit.isBlank())
                    ? lib.getDefaultYUnit().orElse(DEFAULT_Y_UNIT)
                    : groupUnit;

            controller.extractPreviewAtCursor(product, axis, unitToUse, bands, cursorX, cursorY, level, names);
            extracted++;
        }

        vm.setStatus(extracted == 0
                ? UiStatus.warn("Band filter selected, but no matching spectral bands found")
                : UiStatus.info("Extracted " + extracted + " spectrum group(s) at cursor"));

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

            DisplayableSpectrum[] spectra = buildSpectraForChooser(product, raster);
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
            allBandsBySpectrum = allBySpec.isEmpty() ? null : Collections.unmodifiableMap(allBySpec);

            vm.setStatus(selectedBandsBySpectrum == null
                    ? UiStatus.info("Band filter cleared (all spectral bands)")
                    : UiStatus.info("Band filter set: " + selectedBandsBySpectrum.size() + " spectrum group(s)"));
        });
    }

    private DisplayableSpectrum[] buildSpectraForChooser(Product product, RasterDataNode raster) {
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
                DisplayableSpectrum s = new DisplayableSpectrum(g.getName(), gi);
                s.setLineStyle(SpectrumStrokeProvider.getStroke(gi));

                Set<String> wanted = preSel != null ? preSel.get(s.getName()) : null;

                boolean isDefaultGroup = (defaultSpectrumGroupName != null)
                        ? defaultSpectrumGroupName.equals(s.getName())
                        : (gi == 0);

                for (String bandName : g.getMatchingBandNames(product)) {
                    Band b = product.getBand(bandName);
                    if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                        continue;
                    }
                    boolean selected = (preSel == null)
                            ? isDefaultGroup
                            : (wanted != null && wanted.contains(b.getName()));
                    s.addBand(new SpectrumBand(b, selected));
                    usedBandNames.add(b.getName());
                }

                if (preSel != null) {
                    s.setSelected(wanted != null && s.getSelectedBands().length > 0);
                } else {
                    s.setSelected(isDefaultGroup);
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
            int selectedSpectrumIndex = (raster != null) ? auto.indexOf(raster.getName()) : -1;

            int i = 0;
            Iterator<String[]> it = auto.iterator();

            while (it.hasNext()) {
                String name = String.join("_", it.next());
                DisplayableSpectrum s = new DisplayableSpectrum(name, base + i);
                s.setLineStyle(SpectrumStrokeProvider.getStroke(base + i));

                if (preSel != null) {
                    s.setSelected(false);
                } else {
                    boolean isDefault = (defaultSpectrumGroupName != null)
                            ? defaultSpectrumGroupName.equals(name)
                            : (selectedSpectrumIndex >= 0 ? (i == selectedSpectrumIndex) : (i == 0));
                    s.setSelected(isDefault);
                }

                autoSpectra.add(s);
                i++;
            }

            for (Band b : spectralBands) {
                int idx = auto.indexOf(b.getName());
                if (idx >= 0 && idx < autoSpectra.size()) {
                    DisplayableSpectrum s = autoSpectra.get(idx);
                    Set<String> wanted = preSel != null ? preSel.get(s.getName()) : null;
                    boolean isDefaultGroup = (defaultSpectrumGroupName != null)
                            ? defaultSpectrumGroupName.equals(s.getName())
                            : (selectedSpectrumIndex >= 0 ? (idx == selectedSpectrumIndex) : (idx == 0));
                    boolean selected = (preSel == null)
                            ? isDefaultGroup
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
            }

            spectra.addAll(autoSpectra);
        }

        DisplayableSpectrum rest = new DisplayableSpectrum("Ungrouped spectral bands", spectra.size());
        rest.setLineStyle(SpectrumStrokeProvider.getStroke(spectra.size()));
        Set<String> wantedRest = preSel != null ? preSel.get(rest.getName()) : null;

        for (Band b : spectralBands) {
            if (!usedBandNames.contains(b.getName())) {
                boolean isDefaultGroup = (defaultSpectrumGroupName != null)
                        ? defaultSpectrumGroupName.equals(rest.getName())
                        : false;
                boolean selected = (preSel == null)
                        ? isDefaultGroup
                        : (wantedRest != null && wantedRest.contains(b.getName()));
                rest.addBand(new SpectrumBand(b, selected));
            }
        }

        if (rest.getSpectralBands().length > 0) {
            if (preSel != null) {
                rest.setSelected(wantedRest != null && rest.getSelectedBands().length > 0);
            } else {
                rest.setSelected(false);
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

            if (chosenDefault != null) {
                defaultSpectrumGroupName = chosenDefault.getName();
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
}

