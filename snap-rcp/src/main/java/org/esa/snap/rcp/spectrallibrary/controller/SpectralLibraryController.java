package org.esa.snap.rcp.spectrallibrary.controller;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.AddPreviewAttributeDialog;
import org.esa.snap.rcp.spectrallibrary.ui.PreviewPanel;
import org.esa.snap.rcp.spectrallibrary.ui.noise.SpectralNoiseReductionProfilesDialog;
import org.esa.snap.rcp.spectrallibrary.ui.noise.SpectralNoiseSettings;
import org.esa.snap.rcp.spectrallibrary.ui.resampling.SpectralResamplingProfilesDialog;
import org.esa.snap.rcp.spectrallibrary.ui.resampling.SpectralResamplingSettings;
import org.esa.snap.rcp.spectrallibrary.util.ColorUtils;
import org.esa.snap.rcp.spectrallibrary.util.SpectralLibraryUtils;
import org.esa.snap.rcp.spectrallibrary.util.WktUtils;
import org.esa.snap.rcp.spectrallibrary.util.noise.SpectralNoiseUtils;
import org.esa.snap.rcp.spectrallibrary.util.resampling.SpectralResamplingUtils;
import org.esa.snap.rcp.spectrallibrary.wiring.EngineAccess;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.model.*;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;
import org.esa.snap.speclib.util.noise.SpectralNoiseKernelFactory;
import org.esa.snap.speclib.util.resampling.SpectralResamplingSensor;
import org.esa.snap.speclib.util.resampling.SpectralResponseFunction;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class SpectralLibraryController {


    private List<SpectralProfile> lastExtractedAllProfiles = List.of();
    private final Map<UUID, PreviewOrigin> previewOriginsByProfileId = new HashMap<>();

    private final SpectralLibraryService service;
    private final SpectralLibraryIO io;
    private final SpectralLibraryViewModel vm;

    private final PreviewPanel previewPanel;
    private SwingWorker<ExtractResult, Void> currentExtractWorker;
    private volatile Set<PreviewSourceKey> currentExtractSourceKeys = Set.of();


    public record GeometryPreviewSource(String vectorDataNodeName, String featureId, String geometryWkt, List<PixelPos> pixels) {
        public GeometryPreviewSource {
            if (pixels == null || pixels.isEmpty()) {
                pixels = List.of();
            } else {
                List<PixelPos> copy = new ArrayList<>(pixels.size());
                for (PixelPos pixel : pixels) {
                    if (pixel != null) {
                        copy.add(pixel);
                    }
                }
                pixels = List.copyOf(copy);
            }
        }
    }

    private enum PreviewOriginKind {
        PRODUCT,
        GEOMETRY,
        PIN
    }

    private record PreviewOrigin(Product product,
                                 PreviewOriginKind kind,
                                 String vectorDataNodeName,
                                 String featureId,
                                 String geometryWkt,
                                 String placemarkName) {
        static PreviewOrigin product(Product product) {
            return new PreviewOrigin(product, PreviewOriginKind.PRODUCT, null, null, null, null);
        }

        static PreviewOrigin geometry(Product product, String vectorDataNodeName, String featureId, String geometryWkt) {
            return new PreviewOrigin(product, PreviewOriginKind.GEOMETRY, vectorDataNodeName, featureId, geometryWkt, null);
        }

        static PreviewOrigin pin(Product product, String placemarkName) {
            return new PreviewOrigin(product, PreviewOriginKind.PIN, null, null, null, placemarkName);
        }
    }

    private record PreviewSourceKey(Product product,
                                    PreviewOriginKind kind,
                                    String vectorDataNodeName,
                                    String featureId,
                                    String geometryWkt,
                                    String placemarkName) {
        static PreviewSourceKey product(Product product) {
            return new PreviewSourceKey(product, PreviewOriginKind.PRODUCT, null, null, null, null);
        }

        static PreviewSourceKey geometry(Product product, String vectorDataNodeName, String featureId, String geometryWkt) {
            return new PreviewSourceKey(product, PreviewOriginKind.GEOMETRY, vectorDataNodeName, featureId, geometryWkt, null);
        }

        static PreviewSourceKey pin(Product product, String placemarkName) {
            return new PreviewSourceKey(product, PreviewOriginKind.PIN, null, null, null, placemarkName);
        }
    }

    private record PixelKey(int x, int y) {
    }


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
        List<SpectralProfile> profiles = libOpt.map(SpectralLibrary::getProfiles).orElse(List.of());
        vm.setLibraryProfiles(profiles);
        vm.replaceProfileColors(idOpt.get(), SpectralLibraryUtils.profileDisplayColors(profiles));
    }

    public void addSelectedPreviewToLibrary() {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        Set<UUID> selectedIds = vm.getSelectedPreviewProfileIds();
        if (selectedIds.isEmpty()) {
            Optional<SpectralProfile> singleOpt = findSelectedPreviewProfile();
            if (singleOpt.isEmpty()) {
                vm.setStatus(UiStatus.warn("No preview profile selected"));
                return;
            }
            selectedIds = Set.of(singleOpt.get().getId());
        }

        SpectralLibrary lib = service.getLibrary(libId).orElse(null);
        if (lib == null) {
            vm.setStatus(UiStatus.warn("No active library"));
            return;
        }

        Set<UUID> existingIds = new HashSet<>();
        for (SpectralProfile p : lib.getProfiles()) {
            if (p != null && p.getId() != null) {
                existingIds.add(p.getId());
            }
        }

        List<SpectralProfile> candidates = new ArrayList<>();
        for (SpectralProfile p : vm.getPreviewProfiles()) {
            if (p != null && p.getId() != null && selectedIds.contains(p.getId()) && !existingIds.contains(p.getId())) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            vm.setStatus(UiStatus.info("All selected profiles already exist (skipped)"));
            return;
        }

        Optional<Map<String, AttributeValue>> attrsOpt = askAttributesForPreviewAdd();
        if (attrsOpt.isEmpty()) {
            return;
        }

        Map<String, AttributeValue> attrs = attrsOpt.get();
        List<SpectralProfile> toAdd = new ArrayList<>(candidates.size());
        for (SpectralProfile p : candidates) {
            SpectralProfile enriched = SpectralLibraryUtils.applyAttributes(p, attrs);
            toAdd.add(enrichWithDisplayColor(enriched, libId));
        }

        try {
            SpectralLibraryService.BulkAddResult r = service.addProfiles(libId, toAdd);
            reloadLibraries();
            refreshActiveLibraryProfiles();
            int added = r.added();
            int skipped = r.skippedExisting();
            vm.setStatus(added > 0
                    ? UiStatus.info("Profiles added (" + added + ")" + (skipped > 0 ? ", skipped (" + skipped + ")" : ""))
                    : UiStatus.info("All profiles already exist (skipped)"));
        } catch (Throwable t) {
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.warn("Profiles could not be added: " + t.getMessage()));
        }
    }

    public void addAllPreviewToLibrary() {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        List<SpectralProfile> preview = getAllPreviewProfilesForAdd();
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
            SpectralProfile enriched = SpectralLibraryUtils.applyAttributes(p, attrs);
            toAdd.add(enrichWithDisplayColor(enriched, libId));
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
        previewOriginsByProfileId.clear();
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
        previewOriginsByProfileId.clear();
        vm.setPreviewProfiles(List.of());
        vm.setSelectedPreviewProfileId(null);
        vm.setStatus(UiStatus.idle());
    }

    List<SpectralProfile> getAllPreviewProfilesForAdd() {
        return !lastExtractedAllProfiles.isEmpty()
                ? lastExtractedAllProfiles
                : vm.getPreviewProfiles();
    }

    void trackProductPreviewOrigins(Product product, List<SpectralProfile> profiles) {
        trackPreviewOrigins(profiles, PreviewOrigin.product(product));
    }

    void trackGeometryPreviewOrigins(Product product, String vectorDataNodeName, String featureId, List<SpectralProfile> profiles) {
        trackGeometryPreviewOrigins(product, vectorDataNodeName, featureId, null, profiles);
    }

    void trackGeometryPreviewOrigins(Product product, String vectorDataNodeName, String featureId, String geometryWkt, List<SpectralProfile> profiles) {
        trackPreviewOrigins(profiles, PreviewOrigin.geometry(product, vectorDataNodeName, featureId, geometryWkt));
    }

    void trackPinPreviewOrigins(Product product, String placemarkName, List<SpectralProfile> profiles) {
        trackPreviewOrigins(profiles, PreviewOrigin.pin(product, placemarkName));
    }

    public int removePreviewProfilesForProduct(Product product) {
        if (product == null) {
            return 0;
        }
        return removePreviewProfiles(
                origin -> origin.product == product,
                key -> key.product == product,
                removed -> UiStatus.info("Preview updated: removed " + removed +
                        " profiles for closed product '" + product.getName() + "'")
        );
    }

    public int removePreviewProfilesForGeometryFeatures(Product product, String vectorDataNodeName, Set<String> featureIds) {
        if (product == null || vectorDataNodeName == null || featureIds == null || featureIds.isEmpty()) {
            return 0;
        }
        Set<String> ids = Set.copyOf(featureIds);
        return removePreviewProfiles(
                origin -> origin.product == product
                        && origin.kind == PreviewOriginKind.GEOMETRY
                        && Objects.equals(origin.vectorDataNodeName, vectorDataNodeName)
                        && ids.contains(origin.featureId),
                key -> key.product == product
                        && key.kind == PreviewOriginKind.GEOMETRY
                        && Objects.equals(key.vectorDataNodeName, vectorDataNodeName)
                        && ids.contains(key.featureId),
                removed -> UiStatus.info("Preview updated: removed " + removed +
                        " profiles for deleted or changed geometry")
        );
    }

    public int removePreviewProfilesForChangedGeometryFeatures(Product product, String vectorDataNodeName, Map<String, String> currentGeometryWktsByFeatureId) {
        if (product == null || vectorDataNodeName == null || currentGeometryWktsByFeatureId == null || currentGeometryWktsByFeatureId.isEmpty()) {
            return 0;
        }
        Map<String, String> currentWkts = new HashMap<>(currentGeometryWktsByFeatureId);
        return removePreviewProfiles(
                origin -> origin.product == product
                        && origin.kind == PreviewOriginKind.GEOMETRY
                        && Objects.equals(origin.vectorDataNodeName, vectorDataNodeName)
                        && geometryChanged(origin.featureId, origin.geometryWkt, currentWkts),
                key -> key.product == product
                        && key.kind == PreviewOriginKind.GEOMETRY
                        && Objects.equals(key.vectorDataNodeName, vectorDataNodeName)
                        && geometryChanged(key.featureId, key.geometryWkt, currentWkts),
                removed -> UiStatus.info("Preview updated: removed " + removed +
                        " profiles for deleted or changed geometry")
        );
    }

    public int removePreviewProfilesForVectorDataNode(Product product, VectorDataNode vectorDataNode) {
        if (product == null || vectorDataNode == null) {
            return 0;
        }
        Set<String> featureIds = collectFeatureIds(vectorDataNode);
        if (!featureIds.isEmpty()) {
            return removePreviewProfilesForGeometryFeatures(product, vectorDataNode.getName(), featureIds);
        }
        String vectorDataNodeName = vectorDataNode.getName();
        return removePreviewProfiles(
                origin -> origin.product == product
                        && origin.kind == PreviewOriginKind.GEOMETRY
                        && Objects.equals(origin.vectorDataNodeName, vectorDataNodeName),
                key -> key.product == product
                        && key.kind == PreviewOriginKind.GEOMETRY
                        && Objects.equals(key.vectorDataNodeName, vectorDataNodeName),
                removed -> UiStatus.info("Preview updated: removed " + removed +
                        " profiles for deleted or changed geometry")
        );
    }

    public int removePreviewProfilesForPin(Product product, String placemarkName) {
        if (product == null || placemarkName == null) {
            return 0;
        }
        return removePreviewProfiles(
                origin -> origin.product == product
                        && origin.kind == PreviewOriginKind.PIN
                        && Objects.equals(origin.placemarkName, placemarkName),
                key -> key.product == product
                        && key.kind == PreviewOriginKind.PIN
                        && Objects.equals(key.placemarkName, placemarkName),
                removed -> UiStatus.info("Preview updated: removed " + removed +
                        " profiles for deleted pin '" + placemarkName + "'")
        );
    }

    private int removePreviewProfiles(Predicate<PreviewOrigin> originPredicate,
                                      Predicate<PreviewSourceKey> sourceKeyPredicate,
                                      Function<Integer, UiStatus> statusFactory) {
        return runOnEdt(() -> removePreviewProfilesOnEdt(originPredicate, sourceKeyPredicate, statusFactory));
    }

    private int removePreviewProfilesOnEdt(Predicate<PreviewOrigin> originPredicate,
                                           Predicate<PreviewSourceKey> sourceKeyPredicate,
                                           Function<Integer, UiStatus> statusFactory) {
        cancelRunningExtractionIf(sourceKeyPredicate);

        Set<UUID> removeIds = new HashSet<>();
        for (Map.Entry<UUID, PreviewOrigin> entry : previewOriginsByProfileId.entrySet()) {
            if (originPredicate.test(entry.getValue())) {
                removeIds.add(entry.getKey());
            }
        }

        if (removeIds.isEmpty()) {
            return 0;
        }

        List<SpectralProfile> remaining = new ArrayList<>();
        for (SpectralProfile profile : getAllPreviewProfilesForAdd()) {
            if (profile != null && profile.getId() != null && !removeIds.contains(profile.getId())) {
                remaining.add(profile);
            }
        }

        updatePreviewAfterInvalidation(remaining);
        vm.setStatus(statusFactory.apply(removeIds.size()));
        return removeIds.size();
    }

    private static int runOnEdt(IntSupplier action) {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.getAsInt();
        }
        AtomicInteger result = new AtomicInteger();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(action.getAsInt()));
        } catch (Exception ex) {
            throw new IllegalStateException("Preview invalidation failed", ex);
        }
        return result.get();
    }

    private void updatePreviewAfterInvalidation(List<SpectralProfile> remainingProfiles) {
        lastExtractedAllProfiles = SpectralLibraryUtils.safeCopyWithoutNulls(remainingProfiles);
        Set<UUID> remainingIds = profileIds(lastExtractedAllProfiles);
        previewOriginsByProfileId.keySet().retainAll(remainingIds);

        List<SpectralProfile> display = SpectralLibraryUtils.limitForDisplay(lastExtractedAllProfiles);
        vm.setPreviewProfiles(display);

        UUID selectedId = vm.getSelectedPreviewProfileId().orElse(null);
        if (!SpectralLibraryUtils.containsId(display, selectedId)) {
            selectedId = display.isEmpty() ? null : display.get(display.size() - 1).getId();
        }
        vm.setSelectedPreviewProfileId(selectedId);
    }

    private void trackPreviewOrigins(List<SpectralProfile> profiles, PreviewOrigin origin) {
        if (origin.product == null || profiles == null || profiles.isEmpty()) {
            return;
        }
        for (SpectralProfile profile : profiles) {
            if (profile != null && profile.getId() != null) {
                previewOriginsByProfileId.put(profile.getId(), origin);
            }
        }
    }

    private void cancelRunningExtractionIf(Predicate<PreviewSourceKey> sourceKeyPredicate) {
        SwingWorker<ExtractResult, Void> worker = currentExtractWorker;
        if (worker == null || worker.isDone() || sourceKeyPredicate == null) {
            return;
        }
        for (PreviewSourceKey sourceKey : currentExtractSourceKeys) {
            if (sourceKeyPredicate.test(sourceKey)) {
                worker.cancel(true);
                return;
            }
        }
    }

    private static Set<UUID> profileIds(List<SpectralProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (SpectralProfile profile : profiles) {
            if (profile != null && profile.getId() != null) {
                ids.add(profile.getId());
            }
        }
        return ids;
    }

    private static Set<String> collectFeatureIds(VectorDataNode vectorDataNode) {
        if (vectorDataNode == null) {
            return Set.of();
        }
        Set<String> featureIds = new HashSet<>();
        try (FeatureIterator<SimpleFeature> iterator = vectorDataNode.getFeatureCollection().features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                if (feature != null && feature.getID() != null) {
                    featureIds.add(feature.getID());
                }
            }
        }
        return featureIds;
    }

    private static boolean geometryChanged(String featureId, String originalGeometryWkt, Map<String, String> currentGeometryWktsByFeatureId) {
        if (featureId == null || originalGeometryWkt == null || currentGeometryWktsByFeatureId == null) {
            return false;
        }
        if (!currentGeometryWktsByFeatureId.containsKey(featureId)) {
            return false;
        }
        return !Objects.equals(originalGeometryWkt, currentGeometryWktsByFeatureId.get(featureId));
    }

    private static PixelKey pixelKeyOf(SpectralProfile profile, PixelPos fallback) {
        if (profile != null && profile.getSourceRef().isPresent()) {
            SpectralProfile.SourceRef sourceRef = profile.getSourceRef().get();
            return new PixelKey(sourceRef.getX(), sourceRef.getY());
        }
        if (fallback != null) {
            return new PixelKey((int) fallback.x, (int) fallback.y);
        }
        return new PixelKey(0, 0);
    }

    private static PreviewOrigin pollOriginForPixel(PixelKey pixel, Map<PixelKey, Deque<PreviewOrigin>> originsByPixel) {
        if (pixel == null || originsByPixel == null) {
            return null;
        }
        Deque<PreviewOrigin> origins = originsByPixel.get(pixel);
        if (origins == null || origins.isEmpty()) {
            return null;
        }
        return origins.removeFirst();
    }


    public void extractPreviewFromPins(Product product, SpectralAxis axis, String yUnit, List<Band> bands, List<Placemark> pins, int level, Set<String> selectedBandNames, String namePrefix) {
        if (product == null || axis == null || bands == null || pins == null) {
            return;
        }
        if (bands.isEmpty() || pins.isEmpty()) {
            return;
        }

        Set<PreviewSourceKey> sourceKeys = new HashSet<>();
        sourceKeys.add(PreviewSourceKey.product(product));
        for (Placemark pin : pins) {
            if (pin != null && pin.getName() != null) {
                sourceKeys.add(PreviewSourceKey.pin(product, pin.getName()));
            }
        }

        startExtractAsync(() -> {
            List<SpectralProfile> out = new ArrayList<>(vm.getPreviewProfiles());
            int added = 0;
            Map<UUID, PreviewOrigin> origins = new HashMap<>();

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
                    origins.put(masked.getId(), PreviewOrigin.pin(product, pin.getName()));
                    overrides.put(masked.getId(), ColorUtils.colorFromPlacemark(pin));
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(SpectralLibraryUtils.safeCopyWithoutNulls(out), selectId, added, overrides, origins);
        }, "Extracting spectra from pins...", sourceKeys);
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

            return ExtractResult.success(SpectralLibraryUtils.safeCopyWithoutNulls(out), masked.getId(), overrides,
                    Map.of(masked.getId(), PreviewOrigin.product(product)));
        }, "Extracting spectrum...", Set.of(PreviewSourceKey.product(product)));
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
            Map<UUID, PreviewOrigin> origins = new HashMap<>();

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
                    PixelKey pixel = pixelKeyOf(masked, ii < pixels.size() ? pixels.get(ii) : null);
                    masked = SpectralLibraryUtils.withDefaultAttributesIfPossible(product, pixel.x(), pixel.y(), masked);

                    out.add(masked);
                    origins.put(masked.getId(), PreviewOrigin.product(product));
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(SpectralLibraryUtils.safeCopyWithoutNulls(out), selectId, added, Collections.emptyMap(), origins);
        }, "Extracting spectra from geometry...", Set.of(PreviewSourceKey.product(product)));
    }

    public void extractPreviewFromGeometrySources(Product product, SpectralAxis axis, String yUnit, List<Band> bands, List<GeometryPreviewSource> sources, int level, Set<String> selectedBandNames, String namePrefix) {
        if (product == null || axis == null || bands == null || sources == null) {
            return;
        }
        if (bands.isEmpty() || sources.isEmpty()) {
            return;
        }

        List<PixelPos> pixels = new ArrayList<>();
        Map<PixelKey, Deque<PreviewOrigin>> originsByPixel = new HashMap<>();
        Set<PreviewSourceKey> sourceKeys = new HashSet<>();
        sourceKeys.add(PreviewSourceKey.product(product));

        for (GeometryPreviewSource source : sources) {
            if (source == null) {
                continue;
            }
            PreviewOrigin origin;
            if (source.featureId() != null && source.vectorDataNodeName() != null) {
                origin = PreviewOrigin.geometry(product, source.vectorDataNodeName(), source.featureId(), source.geometryWkt());
                sourceKeys.add(PreviewSourceKey.geometry(product, source.vectorDataNodeName(), source.featureId(), source.geometryWkt()));
            } else {
                origin = PreviewOrigin.product(product);
            }
            for (PixelPos pixel : source.pixels()) {
                if (pixel == null) {
                    continue;
                }
                pixels.add(pixel);
                originsByPixel.computeIfAbsent(new PixelKey((int) pixel.x, (int) pixel.y), k -> new ArrayDeque<>()).add(origin);
            }
        }

        if (pixels.isEmpty()) {
            return;
        }

        startExtractAsync(() -> {
            List<SpectralProfile> out = new ArrayList<>(vm.getPreviewProfiles());
            int added = 0;
            Map<UUID, PreviewOrigin> origins = new HashMap<>();

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
                for (SpectralProfile extractedProfile : extractedProfiles) {
                    SpectralProfile masked = SpectralLibraryUtils.maskUnselectedToNaN(extractedProfile, bands, selectedBandNames);
                    PixelKey pixel = pixelKeyOf(masked, null);
                    masked = SpectralLibraryUtils.withDefaultAttributesIfPossible(product, pixel.x(), pixel.y(), masked);

                    out.add(masked);
                    PreviewOrigin origin = pollOriginForPixel(pixel, originsByPixel);
                    if (origin != null) {
                        origins.put(masked.getId(), origin);
                    }
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(SpectralLibraryUtils.safeCopyWithoutNulls(out), selectId, added, Collections.emptyMap(), origins);
        }, "Extracting spectra from geometry...", sourceKeys);
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
            SpectralLibrary lib = libOpt.get();
            List<SpectralProfile> enriched = new ArrayList<>(lib.getProfiles().size());
            for (SpectralProfile p : lib.getProfiles()) {
                if (p != null && p.getId() != null
                        && p.getAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR).isEmpty()) {
                    Color c = ColorUtils.defaultColor(p.getId());
                    if (c != null) {
                        p = p.withAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR,
                                AttributeValue.ofString(ColorUtils.toHex(c)));
                    }
                }
                enriched.add(p);
            }
            SpectralLibrary toWrite = new SpectralLibrary(
                    lib.getId(), lib.getName(), lib.getAxis(),
                    lib.getDefaultYUnit().orElse(null),
                    enriched, lib.getSchema());
            io.write(toWrite, file.toPath());
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

            List<SpectralProfile> raw = SpectralLibraryUtils.safeCopyWithoutNulls(imported.getProfiles());
            List<SpectralProfile> toAdd = new ArrayList<>(raw.size());
            for (SpectralProfile p : raw) {
                if (p.getId() != null
                        && p.getAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR).isEmpty()) {
                    Color c = ColorUtils.defaultColor(p.getId());
                    if (c != null) {
                        p = p.withAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR,
                                AttributeValue.ofString(ColorUtils.toHex(c)));
                    }
                }
                toAdd.add(p);
            }
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
            vm.setStatus(UiStatus.warn("No vector layer created: selected profiles have no valid 'wkt' geometry"));
            return;
        }

        VectorDataNode vectorDataNode = new VectorDataNode(layerName, collection.getSchema());
        for (SimpleFeature feature : collection) {
            vectorDataNode.getFeatureCollection().add(feature);
        }

        if (product.getVectorDataGroup().get(layerName) == null) {
            product.getVectorDataGroup().add(vectorDataNode);
            vm.setStatus(UiStatus.info("Vector layer '" + layerName +
                    "' added under Product Explorer > Vector Data (" + added + " features)"));
        } else {
            vm.setStatus(UiStatus.warn("Vector layer '" + layerName +
                    "' already exists under Product Explorer > Vector Data."));
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

    public void applySpectralResampling(UUID libraryId,
                                        List<UUID> profileIds,
                                        SpectralResamplingSettings settings,
                                        SpectralResamplingProfilesDialog.SaveMode saveMode,
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

        CsvTable fwhmTable;
        try {
            fwhmTable = SpectralResamplingUtils.readFwhmFromCsv(settings.targetSensorName());
        } catch (IOException | URISyntaxException e) {
            vm.setStatus(UiStatus.error("Spectral Resampling failed: " + e.getMessage()));
            return;
        }

        List<SpectralProfile> resampledProfiles = new ArrayList<>(selectedProfiles.size());
        for (SpectralProfile profile : selectedProfiles) {
            try {
                resampledProfiles.add(SpectralResamplingUtils.createResampledProfile(profile,
                        sourceLibrary.getAxis().getWavelengths(),
                        fwhmTable,
                        settings.targetSensorName(),
                        nameSuffix));
            } catch (IOException | URISyntaxException e) {
                vm.setStatus(UiStatus.error("Spectral Resampling failed: " + e.getMessage()));
                return;
            }
        }

        try {
            String libName = (newLibraryName == null || newLibraryName.isBlank())
                    ? sourceLibrary.getName() + "_resampled"
                    : newLibraryName.trim();

            final double[] resampledWvls = SpectralResponseFunction.getFwhmTableColumnsAsArrays(fwhmTable)[0];
            final SpectralAxis resampledAxis = new SpectralAxis(resampledWvls, sourceLibrary.getAxis().getXUnit());
            SpectralLibrary newLibrary = service.createLibrary(
                    libName,
                    resampledAxis,
                    sourceLibrary.getDefaultYUnit().orElse(null)
            );

            SpectralLibraryService.BulkAddResult r = service.addProfiles(newLibrary.getId(), resampledProfiles);

            clearPreview();
            reloadLibraries();
            vm.setActiveLibraryId(newLibrary.getId());
            refreshActiveLibraryProfiles();

            vm.setStatus(UiStatus.info(
                    "New library created: " + libName + " (" + r.added() + " profiles)"
            ));
        } catch (Exception ex) {
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.error("Spectral resamopling failed: " + ex.getMessage()));
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


    private SpectralProfile enrichWithDisplayColor(SpectralProfile profile, UUID libId) {
        if (profile == null || profile.getId() == null || libId == null) {
            return profile;
        }
        if (profile.getAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR).isPresent()) {
            return profile;
        }
        Color c = vm.getProfileColors(libId).get(profile.getId());
        if (c == null) {
            c = ColorUtils.defaultColor(profile.getId());
        }
        if (c == null) {
            return profile;
        }
        return profile.withAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR,
                AttributeValue.ofString(ColorUtils.toHex(c)));
    }

    private void startExtractAsync(Supplier<ExtractResult> job, String busyMsg) {
        startExtractAsync(job, busyMsg, Set.of());
    }

    private void startExtractAsync(Supplier<ExtractResult> job, String busyMsg, Set<PreviewSourceKey> sourceKeys) {
        if (currentExtractWorker != null && !currentExtractWorker.isDone()) {
            currentExtractWorker.cancel(true);
        }

        currentExtractSourceKeys = sourceKeys == null || sourceKeys.isEmpty() ? Set.of() : Set.copyOf(sourceKeys);
        previewPanel.showBusyMessage(busyMsg);

        SwingWorker<ExtractResult, Void> worker = new SwingWorker<>() {
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
                        previewOriginsByProfileId.keySet().retainAll(profileIds(lastExtractedAllProfiles));
                        if (r.previewOrigins != null && !r.previewOrigins.isEmpty()) {
                            previewOriginsByProfileId.putAll(r.previewOrigins);
                        }
                        List<SpectralProfile> display = SpectralLibraryUtils.limitForDisplay(r.profiles);

                        if (r.paintOverrides != null && !r.paintOverrides.isEmpty()) {
                            mergeProfileColors(r.paintOverrides);
                        }

                        vm.setPreviewProfiles(display);
                        UUID sel = SpectralLibraryUtils.containsId(display, r.selectedId) ? r.selectedId : (display.isEmpty() ? null : display.get(display.size() - 1).getId());
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
                    if (currentExtractWorker == this) {
                        currentExtractSourceKeys = Set.of();
                    }
                    previewPanel.clearBusyMessage();
                }
            }
        };

        currentExtractWorker = worker;
        worker.execute();
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
        final Map<UUID, PreviewOrigin> previewOrigins;

        private ExtractResult(List<SpectralProfile> profiles,
                              UUID selectedId,
                              UiStatus status,
                              Map<UUID, Color> paintOverrides,
                              Map<UUID, PreviewOrigin> previewOrigins) {
            this.profiles = profiles;
            this.selectedId = selectedId;
            this.status = status;
            this.paintOverrides = paintOverrides;
            this.previewOrigins = previewOrigins;
        }

        static ExtractResult success(List<SpectralProfile> profiles,
                                     UUID selectedId,
                                     Map<UUID, Color> paintOverrides,
                                     Map<UUID, PreviewOrigin> previewOrigins) {
            return new ExtractResult(profiles, selectedId, UiStatus.info("Preview added"), paintOverrides, previewOrigins);
        }

        static ExtractResult bulk(List<SpectralProfile> profiles,
                                  UUID selectedId,
                                  int added,
                                  Map<UUID, Color> paintOverrides,
                                  Map<UUID, PreviewOrigin> previewOrigins) {
            UiStatus st = added > 0 ? UiStatus.info("Preview extracted (" + added + ")") : UiStatus.warn("No spectra extracted");
            return new ExtractResult(profiles, selectedId, st, paintOverrides, previewOrigins);
        }

        static ExtractResult noSpectrum() {
            return new ExtractResult(null, null, UiStatus.warn("No spectrum extracted"), Collections.emptyMap(), Collections.emptyMap());
        }

        static ExtractResult error(String msg) {
            return new ExtractResult(null, null, UiStatus.error(msg), Collections.emptyMap(), Collections.emptyMap());
        }
    }
}
