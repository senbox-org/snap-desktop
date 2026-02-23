package org.esa.snap.rcp.spectrallibrary.controller;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.PreviewPanel;
import org.esa.snap.rcp.spectrallibrary.util.SpectralAxisUtils;
import org.esa.snap.rcp.spectrallibrary.wiring.EngineAccess;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.esa.snap.speclib.model.*;

import javax.swing.*;
import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;


public class SpectralLibraryController {


    private static final String PROFILE_PREFIX = "Profile_";
    private static final Pattern NAME_PATTERN = Pattern.compile("^Profile_(\\d+)$");
    private static final String ATTR_WKT = "wkt";
    private static final String ATTR_PRODUCT_NAME = "product_name";
    private static final String ATTR_DATE_TIME = "time";

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

        try {
            service.addProfile(libId, sel);
            reloadLibraries();
            refreshActiveLibraryProfiles();
            vm.setStatus(UiStatus.info("Profile added"));
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

        List<SpectralProfile> preview = vm.getPreviewProfiles();
        if (preview.isEmpty()) {
            vm.setStatus(UiStatus.warn("No preview profiles"));
            return;
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

        int added = 0;
        int skipped = 0;
        int failed = 0;

        for (SpectralProfile p : preview) {
            if (p == null || p.getId() == null) {
                continue;
            }

            if (existingIds.contains(p.getId())) {
                skipped++;
                continue;
            }

            try {
                service.addProfile(libId, p);
                existingIds.add(p.getId());
                added++;
            } catch (Throwable t) {
                failed++;
            }
        }

        reloadLibraries();
        refreshActiveLibraryProfiles();

        if (added > 0) {
            String msg = "Profiles added (" + added + ")";
            if (skipped > 0) {
                msg += ", skipped (" + skipped + ")";
            }
            if (failed > 0) {
                msg += ", failed (" + failed + ")";
            }
            vm.setStatus(UiStatus.info(msg));
        } else if (skipped > 0 && failed == 0) {
            vm.setStatus(UiStatus.warn("Nothing added (all already existed: " + skipped + ")"));
        } else {
            vm.setStatus(UiStatus.warn("Nothing added"));
        }
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

    public SpectralLibrary createLibrary(String name, SpectralAxis axis, String defaultYUnit) {
        SpectralLibrary lib = service.createLibrary(name, axis, defaultYUnit);
        reloadLibraries();
        vm.setActiveLibraryId(lib.getId());
        refreshActiveLibraryProfiles();
        vm.setStatus(UiStatus.info("Library created"));
        return lib;
    }

    public SpectralLibrary createLibraryFromBands(String name, List<Band> bands) {
        SpectralAxis axis = SpectralAxisUtils.axisFromReferenceSpectralGroup(bands);
        String yUnit = SpectralAxisUtils.defaultYUnitFromBands(bands);
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


    public void setPreviewProfiles(List<SpectralProfile> profiles) {
        vm.setPreviewProfiles(safeCopyWithoutNulls(profiles));
        vm.setSelectedPreviewProfileId(null);
        vm.setStatus(UiStatus.info("Preview updated (" + vm.getPreviewProfiles().size() + ")"));
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
        vm.setPreviewProfiles(List.of());
        vm.setSelectedPreviewProfileId(null);
        vm.setStatus(UiStatus.idle());
    }


    public void extractPreviewFromPins(Product product,
                                       SpectralAxis axis,
                                       String yUnit,
                                       List<Band> bands,
                                       List<Placemark> pins,
                                       int level,
                                       Set<String> selectedBandNames) {
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
            String unit = normalizeUnit(yUnit);

            for (Placemark pin : pins) {
                if (pin == null || pin.getPixelPos() == null) {
                    continue;
                }

                int px = (int) Math.floor(pin.getPixelPos().getX());
                int py = (int) Math.floor(pin.getPixelPos().getY());

                String profileName = nextAutoProfileName(libId, out, used);

                Optional<SpectralProfile> pOpt = service.extractProfile(profileName, axis, bands, px, py, level, unit, product.getName());

                if (pOpt.isPresent()) {
                    SpectralProfile masked = maskUnselectedToNaN(pOpt.get(), bands, selectedBandNames);
                    masked = withDefaultAttributesIfPossible(product, px, py, masked);

                    out.add(masked);
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(safeCopyWithoutNulls(out), selectId, added);
        }, "Extracting spectra from pins...");
    }

    public void extractPreviewAtCursor(Product product,
                                       SpectralAxis axis,
                                       String yUnit,
                                       List<Band> bands,
                                       int x,
                                       int y,
                                       int level,
                                       Set<String> selectedBandNames) {
        if (product == null || axis == null || bands == null || bands.isEmpty()) {
            return;
        }

        String unit = normalizeUnit(yUnit);

        startExtractAsync(() -> {
            List<SpectralProfile> preview = vm.getPreviewProfiles();
            UUID libId = requireActiveLibraryIdOrWarn().orElse(null);

            if (libId == null) {
                return ExtractResult.error("No active library");
            }

            String profileName = nextAutoProfileName(libId, preview, null);

            Optional<SpectralProfile> pOpt = service.extractProfile(profileName, axis, bands, x, y, level, unit, product.getName());

            if (pOpt.isEmpty()) {
                return ExtractResult.noSpectrum();
            }

            SpectralProfile masked = maskUnselectedToNaN(pOpt.get(), bands, selectedBandNames);
            masked = withDefaultAttributesIfPossible(product, x, y, masked);

            List<SpectralProfile> out = new ArrayList<>(vm.getPreviewProfiles());
            out.add(masked);

            return ExtractResult.success(safeCopyWithoutNulls(out), masked.getId());
        }, "Extracting spectrum...");
    }

    public void extractPreviewFromPixels(Product product,
                                         SpectralAxis axis,
                                         String yUnit,
                                         List<Band> bands,
                                         List<PixelPos> pixels,
                                         int level,
                                         Set<String> selectedBandNames) {
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

            String unit = normalizeUnit(yUnit);
            Set<String> used = new HashSet<>();

            String baseName = nextAutoProfileName(libId, out, used);
            List<SpectralProfile> extractedProfiles = service.extractProfiles(baseName, axis, bands, pixels, level, unit, product.getName());

            if (!extractedProfiles.isEmpty()) {
                for (int ii = 0; ii < extractedProfiles.size(); ii++) {
                    SpectralProfile masked = maskUnselectedToNaN(extractedProfiles.get(ii), bands, selectedBandNames);
                    masked = withDefaultAttributesIfPossible(product, (int) pixels.get(ii).x, (int) pixels.get(ii).y, masked);

                    out.add(masked);
                    added++;
                }
            }

            UUID selectId = (added > 0) ? out.get(out.size() - 1).getId() : null;
            return ExtractResult.bulk(safeCopyWithoutNulls(out), selectId, added);
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

            int added = 0;
            for (SpectralProfile p : imported.getProfiles()) {
                if (p == null) {
                    continue;
                }
                try {
                    service.addProfile(persisted.getId(), p);
                    added++;
                } catch (Throwable ignored) {
                    // skip broken profiles, continue import
                }
            }

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


    private static String normalizeUnit(String yUnit) {
        String unit = (yUnit == null) ? "" : yUnit.trim();
        return unit.isEmpty() ? "value" : unit;
    }

    private static List<SpectralProfile> safeCopyWithoutNulls(List<SpectralProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return List.of();
        }
        List<SpectralProfile> out = new ArrayList<>(profiles.size());
        for (SpectralProfile p : profiles) {
            if (p != null) {
                out.add(p);
            }
        }
        return List.copyOf(out);
    }

    private static SpectralProfile maskUnselectedToNaN(SpectralProfile p, List<Band> bands, Set<String> selected) {
        if (p == null || selected == null || selected.isEmpty()) {
            return p;
        }

        var sig = p.getSignature();

        double[] y = sig.getValues();
        String yUnit = sig.getYUnitOrNull();

        for (int i = 0; i < bands.size() && i < y.length; i++) {
            Band b = bands.get(i);
            if (b != null && !selected.contains(b.getName())) {
                y[i] = Double.NaN;
            }
        }

        SpectralSignature maskedSig = SpectralSignature.of(y, yUnit);

        return new SpectralProfile(p.getId(), p.getName(), maskedSig, p.getAttributes(), p.getSourceRef().orElse(null));
    }


    private String nextAutoProfileName(UUID libId,
                                       Collection<SpectralProfile> preview,
                                       Set<String> reserved) {

        Set<String> used = new HashSet<>();
        int max = 0;

        if (libId != null) {
            SpectralLibrary lib = service.getLibrary(libId).orElse(null);
            if (lib != null) {
                for (SpectralProfile p : lib.getProfiles()) {
                    String n = nameOf(p);
                    if (n != null) {
                        used.add(n);
                        max = Math.max(max, extractAutoIndex(n));
                    }
                }
            }
        }

        if (preview != null) {
            for (SpectralProfile p : preview) {
                String n = nameOf(p);
                if (n != null) {
                    used.add(n);
                    max = Math.max(max, extractAutoIndex(n));
                }
            }
        }

        if (reserved != null) {
            for (String n : reserved) {
                if (n != null && !n.isBlank()) {
                    used.add(n.trim());
                }
            }
        }

        int next = max + 1;
        String candidate;
        do {
            candidate = PROFILE_PREFIX + next++;
        } while (used.contains(candidate));

        if (reserved != null) {
            reserved.add(candidate);
        }
        return candidate;
    }

    private static String nameOf(SpectralProfile p) {
        if (p == null || p.getName() == null) {
            return null;
        }
        String n = p.getName().trim();
        return n.isEmpty() ? null : n;
    }

    private int extractAutoIndex(String name) {
        if (name == null) {
            return 0;
        }
        var m = NAME_PATTERN.matcher(name.trim());
        if (!m.matches()) {
            return 0;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static SpectralProfile withDefaultAttributesIfPossible(Product product, int x, int y, SpectralProfile profile) {
        profile = withWktIfPossible(product, x, y, profile);
        profile = withProductNameIfPossible(product, profile);
        profile = withDateTimeIfPossible(product, profile);
        return profile;
    }

    private static SpectralProfile withWktIfPossible(Product product, int x, int y, SpectralProfile profile) {
        String wkt = wktPointForPixel(product, x, y);
        if (wkt != null && !wkt.isBlank()) {
            profile = profile.withAttribute(ATTR_WKT, AttributeValue.ofString(wkt));
        }
        return profile;
    }


    private static String wktPointForPixel(Product product, int x, int y) {
        var gc = product.getSceneGeoCoding();
        if (gc == null) {
            return null;
        }

        PixelPos px = new PixelPos(x, y);
        GeoPos gp = gc.getGeoPos(px, null);

        if (gp == null || !gp.isValid()) {
            return null;
        }
        return "POINT (" + gp.lon + " " + gp.lat + ")";
    }

    private static SpectralProfile withProductNameIfPossible(Product product, SpectralProfile profile) {
        String name = product.getName();
        if (name != null) {
            profile = profile.withAttribute(ATTR_PRODUCT_NAME, AttributeValue.ofString(name));
        }
        return profile;
    }

    private static SpectralProfile withDateTimeIfPossible(Product product, SpectralProfile profile) {
        ProductData.UTC startTime = product.getStartTime();
        if (startTime != null) {
            Instant instant = startTime.getAsCalendar().toInstant();
            profile = profile.withAttribute(ATTR_DATE_TIME, AttributeValue.ofInstant(instant));
        }
        return profile;
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
                        vm.setPreviewProfiles(r.profiles);
                        vm.setSelectedPreviewProfileId(r.selectedId);
                    }

                    vm.setStatus(r.status != null ? r.status : UiStatus.idle());
                } catch (Exception ex) {
                    vm.setStatus(UiStatus.error("Extract failed: " + ex.getMessage()));
                } finally {
                    previewPanel.clearBusyMessage();
                }
            }
        };

        currentExtractWorker.execute();
    }

    private static final class ExtractResult {
        final List<SpectralProfile> profiles;
        final UUID selectedId;
        final UiStatus status;

        private ExtractResult(List<SpectralProfile> profiles, UUID selectedId, UiStatus status) {
            this.profiles = profiles;
            this.selectedId = selectedId;
            this.status = status;
        }

        static ExtractResult success(List<SpectralProfile> profiles, UUID selectedId) {
            return new ExtractResult(profiles, selectedId, UiStatus.info("Preview added"));
        }

        static ExtractResult bulk(List<SpectralProfile> profiles, UUID selectedId, int added) {
            UiStatus st = added > 0 ? UiStatus.info("Preview extracted (" + added + ")") : UiStatus.warn("No spectra extracted");
            return new ExtractResult(profiles, selectedId, st);
        }

        static ExtractResult noSpectrum() {
            return new ExtractResult(null, null, UiStatus.warn("No spectrum extracted"));
        }

        static ExtractResult error(String msg) {
            return new ExtractResult(null, null, UiStatus.error(msg));
        }
    }
}
