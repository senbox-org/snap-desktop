package org.esa.snap.rcp.spectrallibrary.controller;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.AddPreviewAttributeDialog;
import org.esa.snap.rcp.spectrallibrary.ui.PreviewPanel;
import org.esa.snap.rcp.spectrallibrary.ui.noise.SpectralNoiseReductionProfilesDialog;
import org.esa.snap.rcp.spectrallibrary.ui.noise.SpectralNoiseSettings;
import org.esa.snap.rcp.spectrallibrary.util.ColorUtils;
import org.esa.snap.rcp.spectrallibrary.util.SpectralLibraryUtils;
import org.esa.snap.rcp.spectrallibrary.util.WktUtils;
import org.esa.snap.rcp.spectrallibrary.util.noise.SpectralNoiseUtils;
import org.esa.snap.rcp.spectrallibrary.wiring.EngineAccess;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.esa.snap.speclib.model.*;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;
import org.esa.snap.speclib.util.noise.SpectralNoiseKernelFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;


public class SpectralLibraryController {


    private List<SpectralProfile> lastExtractedAllProfiles = List.of();

    private final SpectralLibraryService service;
    private final SpectralLibraryIO io;
    private final SpectralLibraryViewModel vm;

    private final PreviewPanel previewPanel;
    private SwingWorker<ExtractResult, Void> currentExtractWorker;


    public SpectralLibraryController(SpectralLibraryViewModel vm, PreviewPanel previewPanel) {
        this(EngineAccess.libraryService(), EngineAccess.libraryIO(), vm, previewPanel);
    }

    public SpectralLibraryController(SpectralLibraryService service, SpectralLibraryIO io, SpectralLibraryViewModel vm, PreviewPanel previewPanel) {
        this.service = service;
        this.io = io;
        this.vm = vm;
        this.previewPanel = Objects.requireNonNull(previewPanel, "previewPanel must not be null");
    }


    public void init() {
        refresh();
    }

    public void refresh() {
        reloadLibraries();
        ensureActiveLibrary();
        refreshActiveLibraryProfiles();
        vm.setStatus(UiStatus.info("Refreshed"));
    }

    public void reloadLibraries() {
        List<SpectralLibrary> libs = service.listLibraries();
        vm.setLibraries(libs);
    }

    private void ensureActiveLibrary() {
        if (vm.getActiveLibraryId().isPresent()) {
            return;
        }

        var libs = vm.getLibraries();
        if (!libs.isEmpty()) {
            vm.setActiveLibraryId(libs.get(0).getId());
            return;
        }

        vm.setStatus(UiStatus.warn("No library yet. Create one from a product (axis required)."));
    }

    public void refreshActiveLibraryProfiles() {
        Optional<UUID> idOpt = vm.getActiveLibraryId();
        if (idOpt.isEmpty()) {
            vm.setLibraryProfiles(List.of());
            return;
        }
        Optional<SpectralLibrary> libOpt = service.getLibrary(idOpt.get());
        vm.setLibraryProfiles(libOpt.map(SpectralLibrary::getProfiles).orElse(List.of()));
    }

    public void addSelectedPreviewToLibrary() {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        Optional<SpectralProfile> selectedOpt = findSelectedPreviewProfile();
        if (selectedOpt.isEmpty()) {
            vm.setStatus(UiStatus.warn("No preview profile selected"));
            return;
        }

        SpectralProfile sel = selectedOpt.get();
        UUID selId = sel.getId();
        if (selId == null) {
            vm.setStatus(UiStatus.warn("Selected profile has no ID"));
            return;
        }

        SpectralLibrary lib = service.getLibrary(libId).orElse(null);
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        for (SpectralProfile p : lib.getProfiles()) {
            if (p != null && selId.equals(p.getId())) {
                vm.setStatus(UiStatus.info("Profile already exists (skipped)"));
                return;
            }
        }

        Optional<Map<String, AttributeValue>> attrsOpt = askAttributesForPreviewAdd();
        if (attrsOpt.isEmpty()) {
            return;
        }

        SpectralProfile toAdd = SpectralLibraryUtils.applyAttributes(sel, attrsOpt.get());

        try {
            SpectralLibraryService.BulkAddResult r = service.addProfiles(libId, List.of(toAdd));
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(r.added() > 0 ? UiStatus.info("Profile added") : UiStatus.info("Profile already exists (skipped)"));
        } catch (Throwable t) {
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.warn("Profile could not be added (skipped): " + t.getMessage()));
        }
    }

    public void addAllPreviewToLibrary() {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        List<SpectralProfile> preview = !lastExtractedAllProfiles.isEmpty()
                ? lastExtractedAllProfiles
                : vm.getPreviewProfiles();
        if (preview.isEmpty()) {
            vm.setStatus(UiStatus.warn("No preview profiles"));
            return;
        }

        SpectralLibrary lib = service.getLibrary(libId).orElse(null);
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        Optional<Map<String, AttributeValue>> attrsOpt = askAttributesForPreviewAdd();
        if (attrsOpt.isEmpty()) {
            return;
        }

        Map<String, AttributeValue> attrs = attrsOpt.get();

        Set<UUID> existingIds = new HashSet<>();
        for (SpectralProfile p : lib.getProfiles()) {
            if (p != null && p.getId() != null) {
                existingIds.add(p.getId());
            }
        }

        List<SpectralProfile> toAdd = new ArrayList<>(preview.size());
        for (SpectralProfile p : preview) {
            if (p == null) {
                continue;
            }
            toAdd.add(SpectralLibraryUtils.applyAttributes(p, attrs));
        }

        SpectralLibraryService.BulkAddResult r = service.addProfiles(libId, toAdd);

        reloadLibraries();
        refreshActiveLibraryProfiles();

        int added = r.added();
        int skipped = r.skippedExisting();
        vm.setStatus(added > 0
                ? UiStatus.info("Profiles added (" + added + ")" + (skipped > 0 ? ", skipped (" + skipped + ")" : ""))
                : (skipped > 0 ? UiStatus.warn("Nothing added (all already existed: " + skipped + ")") : UiStatus.warn("Nothing added")));

    }

    private Optional<UUID> requireActiveLibraryIdOrWarn() {
        Optional<UUID> idOpt = vm.getActiveLibraryId();
        if (idOpt.isEmpty()) {
            vm.setStatus(UiStatus.warn("No active library selected"));
        }
        return idOpt;
    }

    private Optional<SpectralProfile> findSelectedPreviewProfile() {
        Optional<UUID> selId = vm.getSelectedPreviewProfileId();
        if (selId.isEmpty()) {
            return Optional.empty();
        }

        UUID id = selId.get();
        for (SpectralProfile p : vm.getPreviewProfiles()) {
            if (p != null && id.equals(p.getId())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    private Optional<Map<String, AttributeValue>> askAttributesForPreviewAdd() {
        Optional<List<AddPreviewAttributeDialog.AttributeSpec>> specsOpt = AddPreviewAttributeDialog.show(previewPanel);

        if (specsOpt.isEmpty()) {
            return Optional.empty();
        }

        List<AddPreviewAttributeDialog.AttributeSpec> specs = specsOpt.get();
        if (specs.isEmpty()) {
            return Optional.of(Collections.emptyMap());
        }

        Map<String, AttributeValue> out = new LinkedHashMap<>();
        for (var s : specs) {
            if (s == null || s.key() == null || s.key().trim().isEmpty()) {
                continue;
            }

            AttributeType t = (s.type() != null) ? s.type() : AttributeType.STRING;
            try {
                AttributeValue v = SpectralLibraryAttributeValueParser.parseForType(t, s.valueText());
                out.put(s.key().trim(), v);
            } catch (IllegalArgumentException ex) {
                vm.setStatus(UiStatus.warn("Invalid value for '" + s.key().trim() + "': " + ex.getMessage()));
                return Optional.empty();
            }
        }
        return Optional.of(out.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(out));
    }


    private SpectralLibrary createLibrary(String name, SpectralAxis axis, String defaultYUnit) {
        SpectralLibrary lib = service.createLibrary(name, axis, defaultYUnit);
        reloadLibraries();
        vm.setActiveLibraryId(lib.getId());
        refreshActiveLibraryProfiles();
        vm.setStatus(UiStatus.info("Library created"));
        return lib;
    }

    public SpectralLibrary createLibraryFromBands(String name, List<Band> bands) {
        SpectralLibraryUtils.AxisBandSelection sel = SpectralLibraryUtils.selectAxisBandsUniqueByWavelength(bands);
        SpectralAxis axis = SpectralLibraryUtils.axisFromOrderedBands(sel.bandsOrdered());
        String yUnit = SpectralLibraryUtils.defaultYUnitFromBands(sel.bandsOrdered());

        if (yUnit == null || yUnit.isBlank()) {
            yUnit = "value";
        }
        return createLibrary(name, axis, yUnit);
    }

    public boolean deleteLibrary(UUID id) {
        boolean ok = service.deleteLibrary(id);
        reloadLibraries();
        if (ok) {
            if (vm.getActiveLibraryId().isPresent() && vm.getActiveLibraryId().get().equals(id)) {
                vm.setActiveLibraryId(null);
                ensureActiveLibrary();
            }
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.info("Library deleted"));
        }
        return ok;
    }

    public boolean renameLibrary(UUID id, String newName) {
        SpectralLibrary lib = service.renameLibrary(id, newName).orElse(null);
        reloadLibraries();
        if (lib != null) {
            vm.setActiveLibraryId(lib.getId());
            ensureActiveLibrary();
            refreshActiveLibraryProfiles();

            vm.setStatus(UiStatus.info("Library renamed to " + newName));
            return true;
        } else {
            vm.setStatus(UiStatus.error("Library could not be renamed"));
            return false;
        }
    }

    public void setActiveLibrary(UUID id) {
        vm.setActiveLibraryId(id);
        refreshActiveLibraryProfiles();
    }

    public void removeLibraryProfiles(UUID libraryId, List<UUID> profileIds) {
        if (libraryId == null || profileIds == null || profileIds.isEmpty()) {
            return;
        }
        int removed = 0;
        for (UUID pid : profileIds) {
            if (pid != null && service.removeProfile(libraryId, pid)) {
                removed++;
            }
        }
        vm.setSelectedLibraryProfileId(null);
        refreshActiveLibraryProfiles();
        vm.setStatus(removed > 0 ? UiStatus.info("Removed (" + removed + ")") : UiStatus.warn("Nothing removed"));
    }


    public void setPreviewProfilesLimited(List<SpectralProfile> allProfiles) {
        lastExtractedAllProfiles = SpectralLibraryUtils.safeCopyWithoutNulls(allProfiles);
        List<SpectralProfile> display = SpectralLibraryUtils.limitForDisplay(lastExtractedAllProfiles);

        vm.setPreviewProfiles(display);
        vm.setSelectedPreviewProfileId(display.isEmpty() ? null : display.get(display.size() - 1).getId());

        if (lastExtractedAllProfiles.size() > display.size()) {
            vm.setStatus(UiStatus.warn("Preview limited: showing " + display.size() + " of " + lastExtractedAllProfiles.size()));
        } else {
            vm.setStatus(UiStatus.info("Preview updated (" + display.size() + ")"));
        }
    }

    public void addAttributeToActiveLibrary(AttributeDef def, AttributeValue fillValue) {
        if (def == null || fillValue == null) {
            vm.setStatus(UiStatus.warn("Attribute definition/value missing"));
            return;
        }

        Optional<UUID> idOpt = vm.getActiveLibraryId();
        if (idOpt.isEmpty()) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        try {
            service.addAttributeToLibrary(idOpt.get(), def, fillValue);
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.info("Attribute added: " + def.getKey()));
        } catch (Exception ex) {
            vm.setStatus(UiStatus.error("Add attribute failed: " + ex.getMessage()));
        }
    }

    public void renameProfileInActiveLibrary(UUID profileId, String newName) {
        if (profileId == null) {
            vm.setStatus(UiStatus.warn("No profile selected"));
            return;
        }
        if (newName == null || newName.isBlank()) {
            vm.setStatus(UiStatus.warn("Name is empty"));
            return;
        }

        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        try {
            boolean ok = service.renameProfile(libId, profileId, newName.trim());
            if (ok) {
                refreshActiveLibraryProfiles();
                refresh();
                vm.setStatus(UiStatus.info("Profile renamed"));
            } else {
                vm.setStatus(UiStatus.warn("Profile not changed"));
            }
        } catch (Exception ex) {
            vm.setStatus(UiStatus.error("Rename failed: " + ex.getMessage()));
        }
    }

    public void setAttributeInActiveLibrary(UUID profileId, String key, AttributeValue value) {
        if (profileId == null) {
            vm.setStatus(UiStatus.warn("No profile selected"));
            return;
        }
        if (key == null || key.isBlank()) {
            vm.setStatus(UiStatus.warn("Attribute key is empty"));
            return;
        }
        if (value == null) {
            vm.setStatus(UiStatus.warn("Attribute value missing"));
            return;
        }

        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        try {
            boolean ok = service.setProfileAttribute(libId, profileId, key.trim(), value);
            if (ok) {
                refreshActiveLibraryProfiles();
                refresh();
                vm.setStatus(UiStatus.info("Attribute updated: " + key.trim()));
            } else {
                vm.setStatus(UiStatus.warn("Attribute not changed"));
            }
        } catch (Exception ex) {
            vm.setStatus(UiStatus.error("Update attribute failed: " + ex.getMessage()));
        }
    }

    public void clearPreview() {
        lastExtractedAllProfiles = List.of();
        vm.setPreviewProfiles(List.of());
        vm.setSelectedPreviewProfileId(null);
        vm.setStatus(UiStatus.idle());
    }


    public void extractPreviewFromPins(Product product, SpectralAxis axis, String yUnit, List<Band> bands, List<Placemark> pins, int level, Set<String> selectedBandNames, String namePrefix) {
        if (product == null || axis == null || bands == null || pins == null) {
            return;
        }
        if (bands.isEmpty() || pins.isEmpty()) {
            return;
        }

        startExtractAsync(() -> {
            List<SpectralProfile> out = new ArrayList<>(vm.getPreviewProfiles());
            int added = 0;

            UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
            if (libId == null) {
                return ExtractResult.error("No active library");
            }

            Set<String> used = new HashSet<>();
            String unit = SpectralLibraryUtils.normalizeUnit(yUnit);
            Map<UUID, Color> overrides = new HashMap<>();

            for (Placemark pin : pins) {
                if (pin == null || pin.getPixelPos() == null) {
                    continue;
                }

                int px = (int) Math.floor(pin.getPixelPos().getX());
                int py = (int) Math.floor(pin.getPixelPos().getY());

                String prefix = SpectralLibraryUtils.normalizePrefix(namePrefix);
                String profileName = nextAutoProfileName(prefix, libId, out, used, false);

                Optional<SpectralProfile> pOpt = service.extractProfile(profileName, axis, bands, px, py, level, unit, product.getName());

                if (pOpt.isPresent()) {
                    SpectralProfile masked = SpectralLibraryUtils.maskUnselectedToNaN(pOpt.get(), bands, selectedBandNames);
                    masked = SpectralLibraryUtils.withDefaultAttributesIfPossible(product, px, py, masked);

                    out.add(masked);
                    overrides.put(masked.getId(), ColorUtils.colorFromPlacemark(pin));
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(SpectralLibraryUtils.safeCopyWithoutNulls(out), selectId, added, overrides);
        }, "Extracting spectra from pins...");
    }

    public void extractPreviewAtCursor(Product product, SpectralAxis axis, String yUnit, List<Band> bands, int x, int y, int level, Set<String> selectedBandNames, String namePrefix) {
        if (product == null || axis == null || bands == null || bands.isEmpty()) {
            return;
        }

        String unit = SpectralLibraryUtils.normalizeUnit(yUnit);

        startExtractAsync(() -> {
            List<SpectralProfile> preview = vm.getPreviewProfiles();
            UUID libId = requireActiveLibraryIdOrWarn().orElse(null);

            if (libId == null) {
                return ExtractResult.error("No active library");
            }

            Map<UUID, Color> overrides = new HashMap<>();
            String prefix = SpectralLibraryUtils.normalizePrefix(namePrefix);
            String profileName = nextAutoProfileName(prefix, libId, preview, null, false);

            Optional<SpectralProfile> pOpt = service.extractProfile(profileName, axis, bands, x, y, level, unit, product.getName());

            if (pOpt.isEmpty()) {
                return ExtractResult.noSpectrum();
            }

            SpectralProfile masked = SpectralLibraryUtils.maskUnselectedToNaN(pOpt.get(), bands, selectedBandNames);
            masked = SpectralLibraryUtils.withDefaultAttributesIfPossible(product, x, y, masked);
            overrides.put(masked.getId(), Color.BLACK);

            List<SpectralProfile> out = new ArrayList<>(vm.getPreviewProfiles());
            out.add(masked);

            return ExtractResult.success(SpectralLibraryUtils.safeCopyWithoutNulls(out), masked.getId(), overrides);
        }, "Extracting spectrum...");
    }

    public void extractPreviewFromPixels(Product product, SpectralAxis axis, String yUnit, List<Band> bands, List<PixelPos> pixels, int level, Set<String> selectedBandNames, String namePrefix) {
        if (product == null || axis == null || bands == null || pixels == null) {
            return;
        }
        if (bands.isEmpty() || pixels.isEmpty()) {
            return;
        }

        startExtractAsync(() -> {
            List<SpectralProfile> out = new ArrayList<>(vm.getPreviewProfiles());
            int added = 0;

            UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
            if (libId == null) {
                return ExtractResult.error("No active library");
            }

            String unit = SpectralLibraryUtils.normalizeUnit(yUnit);
            Set<String> used = new HashSet<>();

            String prefix = SpectralLibraryUtils.normalizePrefix(namePrefix);
            String baseName = nextAutoProfileName(prefix, libId, out, used, true);
            List<SpectralProfile> extractedProfiles = service.extractProfiles(baseName, axis, bands, pixels, level, unit, product.getName());

            if (!extractedProfiles.isEmpty()) {
                for (int ii = 0; ii < extractedProfiles.size(); ii++) {
                    SpectralProfile masked = SpectralLibraryUtils.maskUnselectedToNaN(extractedProfiles.get(ii), bands, selectedBandNames);
                    masked = SpectralLibraryUtils.withDefaultAttributesIfPossible(product, (int) pixels.get(ii).x, (int) pixels.get(ii).y, masked);

                    out.add(masked);
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(SpectralLibraryUtils.safeCopyWithoutNulls(out), selectId, added, Collections.emptyMap());
        }, "Extracting spectra from geometry...");
    }

    public boolean exportActiveLibraryToFile(File file) {
        Optional<UUID> idOpt = vm.getActiveLibraryId();
        if (idOpt.isEmpty() || file == null) {
            return false;
        }

        Optional<SpectralLibrary> libOpt = service.getLibrary(idOpt.get());
        if (libOpt.isEmpty()) {
            vm.setStatus(UiStatus.warn("No active library to export"));
            return false;
        }

        try {
            io.write(libOpt.get(), file.toPath());
            vm.setStatus(UiStatus.info("Exported: " + file.getName()));
            return true;
        } catch (Exception ex) {
            vm.setStatus(UiStatus.error("Export failed: " + ex.getMessage()));
            return false;
        }
    }

    public Optional<SpectralLibrary> importLibraryFromFile(File file) {
        if (file == null) {
            return Optional.empty();
        }
        try {
            SpectralLibrary imported = io.read(file.toPath());
            String yUnit = imported.getDefaultYUnit().orElse("value");
            SpectralLibrary persisted = service.createLibrary(
                    imported.getName(),
                    imported.getAxis(),
                    yUnit
            );

            List<SpectralProfile> toAdd = SpectralLibraryUtils.safeCopyWithoutNulls(imported.getProfiles());
            SpectralLibraryService.BulkAddResult r = service.addProfiles(persisted.getId(), toAdd);
            int added = r.added();

            reloadLibraries();
            vm.setActiveLibraryId(persisted.getId());
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.info("Imported: " + file.getName() + " (" + added + " profiles)"));
            return Optional.of(persisted);
        } catch (Exception ex) {
            vm.setStatus(UiStatus.error("Import failed: " + ex.getMessage()));
            return Optional.empty();
        }
    }


    public void addProfilesAsVectorLayer(Product product, List<UUID> profileIds, String layerName) {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        SpectralLibrary lib = service.getLibrary(libId).orElse(null);
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        Map<UUID, SpectralProfile> profilesById = new LinkedHashMap<>();
        for (SpectralProfile profile : lib.getProfiles()) {
            if (profile != null && profile.getId() != null) {
                profilesById.put(profile.getId(), profile);
            }
        }

        List<SpectralProfile> selectedProfiles = new ArrayList<>();
        for (UUID profileId : profileIds) {
            SpectralProfile profile = profilesById.get(profileId);
            if (profile != null) {
                selectedProfiles.add(profile);
            }
        }

        if (selectedProfiles.isEmpty()) {
            vm.setStatus(UiStatus.warn("Selected profiles not found in active library"));
            return;
        }

        DefaultFeatureCollection collection = WktUtils.createEmptyFeaturecollection(layerName);
        SimpleFeatureType featureType = collection.getSchema();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        WKTReader reader = new WKTReader();

        int added = 0;
        for (SpectralProfile profile : selectedProfiles) {
            if (WktUtils.addProfileGeometryToCollection(collection, profile, layerName, added, reader, featureBuilder)) {
                added++;
            }
        }

        if (added == 0) {
            vm.setStatus(UiStatus.warn("No valid WKT geometry could be added to the vector layer"));
            return;
        }

        VectorDataNode vectorDataNode = new VectorDataNode(layerName, collection.getSchema());
        for (SimpleFeature feature : collection) {
            vectorDataNode.getFeatureCollection().add(feature);
        }

        if (product.getVectorDataGroup().get(layerName) == null) {
            product.getVectorDataGroup().add(vectorDataNode);
            vm.setStatus(UiStatus.info("Vector layer added (" + added + " features)"));
        } else {
            vm.setStatus(UiStatus.warn("Vector layer '" + layerName + "' already exists."));
        }
    }

    public void applySpectralNoiseReduction(UUID libraryId,
                                            List<UUID> profileIds,
                                            SpectralNoiseSettings settings,
                                            SpectralNoiseReductionProfilesDialog.SaveMode saveMode,
                                            String nameSuffix,
                                            String newLibraryName) {

        SpectralLibrary sourceLibrary = service.getLibrary(libraryId).orElse(null);

        Map<UUID, SpectralProfile> profilesById = new LinkedHashMap<>();
        for (SpectralProfile profile : sourceLibrary.getProfiles()) {
            if (profile != null && profile.getId() != null) {
                profilesById.put(profile.getId(), profile);
            }
        }

        List<SpectralProfile> selectedProfiles = new ArrayList<>();
        for (UUID profileId : profileIds) {
            SpectralProfile profile = profilesById.get(profileId);
            if (profile != null) {
                selectedProfiles.add(profile);
            }
        }

        final double[] kernel;
        try {
            SpectralNoiseKernelFactory kernelFactory = new SpectralNoiseKernelFactory(
                    settings.filterType(),
                    settings.kernelSize(),
                    settings.gaussianSigma(),
                    settings.sgPolynomialOrder()
            );
            kernelFactory.validateFilterParameters();
            kernelFactory.ensureKernelSize(sourceLibrary.getAxis().size());
            kernel = kernelFactory.createKernel();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            vm.setStatus(UiStatus.warn(ex.getMessage()));
            return;
        }

        List<SpectralProfile> smoothedProfiles = new ArrayList<>(selectedProfiles.size());
        for (SpectralProfile profile : selectedProfiles) {
            smoothedProfiles.add(SpectralNoiseUtils.createSmoothedProfile(profile, kernel, nameSuffix));
        }

        try {
            if (saveMode == SpectralNoiseReductionProfilesDialog.SaveMode.ACTIVE_LIBRARY) {
                SpectralLibraryService.BulkAddResult r = service.addProfiles(libraryId, smoothedProfiles);
                reloadLibraries();
                refreshActiveLibraryProfiles();
                vm.setStatus(UiStatus.info(
                        "Smoothed profiles added (" + r.added() + ")" +
                                (r.skippedExisting() > 0 ? ", skipped (" + r.skippedExisting() + ")" : "")
                ));
            } else {
                String libName = (newLibraryName == null || newLibraryName.isBlank())
                        ? sourceLibrary.getName() + "_smoothed"
                        : newLibraryName.trim();

                SpectralLibrary newLibrary = service.createLibrary(
                        libName,
                        sourceLibrary.getAxis(),
                        sourceLibrary.getDefaultYUnit().orElse(null)
                );

                SpectralLibraryService.BulkAddResult r = service.addProfiles(newLibrary.getId(), smoothedProfiles);

                reloadLibraries();
                vm.setActiveLibraryId(newLibrary.getId());
                refreshActiveLibraryProfiles();

                vm.setStatus(UiStatus.info(
                        "New library created: " + libName + " (" + r.added() + " profiles)"
                ));
            }
        } catch (Exception ex) {
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.error("Spectral noise reduction failed: " + ex.getMessage()));
        }
    }



    private String nextAutoProfileName(String prefix,
                                       UUID libId,
                                       Collection<SpectralProfile> preview,
                                       Set<String> reserved,
                                       boolean isGeometry) {
        prefix = SpectralLibraryUtils.normalizePrefix(prefix);

        Set<String> used = new HashSet<>();
        int max = 0;

        if (libId != null) {
            SpectralLibrary lib = service.getLibrary(libId).orElse(null);
            if (lib != null) {
                for (SpectralProfile p : lib.getProfiles()) {
                    String n = SpectralLibraryUtils.nameOf(p);
                    if (n != null) {
                        used.add(n);
                        max = Math.max(max, SpectralLibraryUtils.extractAutoIndex(prefix, n));
                    }
                }
            }
        }

        if (preview != null) {
            for (SpectralProfile p : preview) {
                String n = SpectralLibraryUtils.nameOf(p);
                if (n != null) {
                    used.add(n);
                    max = Math.max(max, SpectralLibraryUtils.extractAutoIndex(prefix, n));
                }
            }
        }

        if (reserved != null) {
            for (String n : reserved) {
                if (n != null && !n.isBlank()) used.add(n.trim());
            }
        }

        int next = max + 1;
        String candidate;
        do {
            if (isGeometry) {
                candidate = prefix;
            } else {
                candidate = prefix + next++;
            }
        } while (used.contains(candidate));

        if (reserved != null) {
            reserved.add(candidate);
        }
        return candidate;
    }


    private void startExtractAsync(Supplier<ExtractResult> job, String busyMsg) {
        if (currentExtractWorker != null && !currentExtractWorker.isDone()) {
            currentExtractWorker.cancel(true);
        }

        previewPanel.showBusyMessage(busyMsg);

        currentExtractWorker = new SwingWorker<>() {
            @Override
            protected ExtractResult doInBackground() {
                try {
                    return job.get();
                } catch (Throwable t) {
                    return ExtractResult.error("Extract failed: " + t.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        return;
                    }

                    ExtractResult r = get();

                    if (r.profiles != null) {
                        lastExtractedAllProfiles = r.profiles;
                        List<SpectralProfile> display = SpectralLibraryUtils.limitForDisplay(r.profiles);

                        if (r.paintOverrides != null && !r.paintOverrides.isEmpty()) {
                            mergeProfileColors(r.paintOverrides);
                        }

                        vm.setPreviewProfiles(display);
                        UUID sel = SpectralLibraryUtils.containsId(display, r.selectedId) ? r.selectedId : (display.isEmpty() ? null : display.get(display.size()-1).getId());
                        vm.setSelectedPreviewProfileId(sel);

                        if (r.profiles.size() > display.size()) {
                            vm.setStatus(UiStatus.warn("Preview limited: showing " + display.size() + " of " + r.profiles.size()));
                        } else {
                            vm.setStatus(r.status != null ? r.status : UiStatus.idle());
                        }
                    } else {
                        vm.setStatus(r.status != null ? r.status : UiStatus.idle());
                    }
                } catch (Exception ex) {
                    vm.setStatus(UiStatus.error("Extract failed: " + ex.getMessage()));
                } finally {
                    previewPanel.clearBusyMessage();
                }
            }
        };

        currentExtractWorker.execute();
    }


    private void mergeProfileColors(Map<UUID, Color> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        UUID libId = vm.getActiveLibraryId().get();
        Map<UUID, Color> merged = new HashMap<>(vm.getProfileColors(libId));
        merged.putAll(updates);
        vm.setProfileColors(libId, Collections.unmodifiableMap(merged));
    }


    private static final class ExtractResult {
        final List<SpectralProfile> profiles;
        final UUID selectedId;
        final UiStatus status;
        final Map<UUID, Color> paintOverrides;

        private ExtractResult(List<SpectralProfile> profiles, UUID selectedId, UiStatus status, Map<UUID, Color> paintOverrides) {
            this.profiles = profiles;
            this.selectedId = selectedId;
            this.status = status;
            this.paintOverrides = paintOverrides;
        }

        static ExtractResult success(List<SpectralProfile> profiles, UUID selectedId, Map<UUID, Color> paintOverrides) {
            return new ExtractResult(profiles, selectedId, UiStatus.info("Preview added"), paintOverrides);
        }

        static ExtractResult bulk(List<SpectralProfile> profiles, UUID selectedId, int added, Map<UUID, Color> paintOverrides) {
            UiStatus st = added > 0 ? UiStatus.info("Preview extracted (" + added + ")") : UiStatus.warn("No spectra extracted");
            return new ExtractResult(profiles, selectedId, st, paintOverrides);
        }

        static ExtractResult noSpectrum() {
            return new ExtractResult(null, null, UiStatus.warn("No spectrum extracted"), Collections.emptyMap());
        }

        static ExtractResult error(String msg) {
            return new ExtractResult(null, null, UiStatus.error(msg), Collections.emptyMap());
        }
    }
}
