package org.esa.snap.rcp.spectrallibrary.controller;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.util.SpectralAxisUtils;
import org.esa.snap.rcp.spectrallibrary.wiring.EngineAccess;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.esa.snap.speclib.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class SpectralLibraryController {


    private final SpectralLibraryService service;
    private final SpectralLibraryIO io;
    private final SpectralLibraryViewModel vm;


    public SpectralLibraryController(SpectralLibraryViewModel vm) {
        this(EngineAccess.libraryService(), EngineAccess.libraryIO(), vm);
    }

    public SpectralLibraryController(SpectralLibraryService service, SpectralLibraryIO io, SpectralLibraryViewModel vm) {
        this.service = service;
        this.io = io;
        this.vm = vm;
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

    public void addSelectedPreviewToLibrary() {
        addSelectedPreviewToActiveLibrary();
    }

    public void addAllPreviewToLibrary() {
        addAllPreviewToActiveLibrary();
    }

    public void reloadLibraries() {
        List<SpectralLibrary> libs = service.listLibraries();
        vm.setLibraries(libs);
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

    public void refreshActiveLibraryProfiles() {
        Optional<UUID> idOpt = vm.getActiveLibraryId();
        if (idOpt.isEmpty()) {
            vm.setLibraryProfiles(List.of());
            return;
        }
        Optional<SpectralLibrary> libOpt = service.getLibrary(idOpt.get());
        vm.setLibraryProfiles(libOpt.map(SpectralLibrary::getProfiles).orElse(List.of()));
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

    public void addSelectedPreviewToActiveLibrary() {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        Optional<SpectralProfile> selectedOpt = findSelectedPreviewProfile();
        if (selectedOpt.isEmpty()) {
            vm.setStatus(UiStatus.warn("No preview profile selected"));
            return;
        }
        service.addProfile(libId, selectedOpt.get());
        refreshActiveLibraryProfiles();
        vm.setStatus(UiStatus.info("Profile added"));
    }

    public void addAllPreviewToActiveLibrary() {
        UUID libId = requireActiveLibraryIdOrWarn().orElse(null);
        if (libId == null) {
            return;
        }

        List<SpectralProfile> preview = vm.getPreviewProfiles();
        if (preview.isEmpty()) {
            vm.setStatus(UiStatus.warn("No preview profiles"));
            return;
        }

        int added = 0;
        for (SpectralProfile p : preview) {
            if (p == null) continue;
            service.addProfile(libId, p);
            added++;
        }
        refreshActiveLibraryProfiles();
        vm.setStatus(UiStatus.info("Profiles added (" + added + ")"));
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
                                       String namePrefix) {
        if (product == null || axis == null || bands == null || pins == null) {
            return;
        }
        if (bands.isEmpty() || pins.isEmpty()) {
            return;
        }

        String unit = normalizeUnit(yUnit);
        String prefix = (namePrefix == null || namePrefix.isBlank()) ? "pin" : namePrefix.trim();

        List<SpectralProfile> preview = new ArrayList<>(vm.getPreviewProfiles());
        int added = 0;

        for (Placemark pin : pins) {
            if (pin == null || pin.getPixelPos() == null) {
                continue;
            }

            int x = (int) Math.floor(pin.getPixelPos().getX());
            int y = (int) Math.floor(pin.getPixelPos().getY());

            String label = pin.getLabel();
            if (label == null || label.isBlank()) label = "pin";
            String name = prefix + "_" + label;

            Optional<SpectralProfile> pOpt = service.extractProfile(
                    name,
                    axis,
                    bands,
                    x,
                    y,
                    level,
                    unit,
                    product.getName()
            );

            if (pOpt.isPresent()) {
                preview.add(pOpt.get());
                added++;
            }
        }

        vm.setPreviewProfiles(safeCopyWithoutNulls(preview));
        vm.setSelectedPreviewProfileId(null);
        vm.setStatus(added > 0 ? UiStatus.info("Preview extracted (" + added + ")")
                : UiStatus.warn("No spectra extracted"));
    }

    public void extractPreviewAtCursor(Product product,
                                       SpectralAxis axis,
                                       String yUnit,
                                       List<Band> bands,
                                       int x,
                                       int y,
                                       int level,
                                       String name) {
        if (product == null || axis == null || bands == null || bands.isEmpty()) {
            return;
        }
        if (name == null || name.isBlank()) {
            return;
        }

        String unit = normalizeUnit(yUnit);

        Optional<SpectralProfile> pOpt = service.extractProfile(
                name.trim(),
                axis,
                bands,
                x,
                y,
                level,
                unit,
                product.getName()
        );

        if (pOpt.isEmpty()) {
            vm.setStatus(UiStatus.warn("No spectrum extracted"));
            return;
        }

        List<SpectralProfile> preview = new ArrayList<>(vm.getPreviewProfiles());
        preview.add(pOpt.get());
        vm.setPreviewProfiles(safeCopyWithoutNulls(preview));
        vm.setSelectedPreviewProfileId(pOpt.get().getId());
        vm.setStatus(UiStatus.info("Preview added"));
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
}
