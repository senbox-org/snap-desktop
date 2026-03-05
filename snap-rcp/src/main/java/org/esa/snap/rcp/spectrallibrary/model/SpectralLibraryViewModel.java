package org.esa.snap.rcp.spectrallibrary.model;

import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.List;


public class SpectralLibraryViewModel {


    public static final String PROP_LIBRARIES = "libraries";
    public static final String PROP_ACTIVE_LIBRARY_ID = "activeLibraryId";
    public static final String PROP_LIBRARY_PROFILES = "libraryProfiles";
    public static final String PROP_PREVIEW_PROFILES = "previewProfiles";
    public static final String PROP_SELECTED_LIBRARY_PROFILE_ID = "selectedLibraryProfileId";
    public static final String PROP_SELECTED_PREVIEW_PROFILE_ID = "selectedPreviewProfileId";
    public static final String PROP_STATUS = "status";
    public static final String PROP_PROFILE_COLORS = "profileColors";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private List<SpectralLibrary> libraries = List.of();
    private UUID activeLibraryId;

    private List<SpectralProfile> libraryProfiles = List.of();
    private List<SpectralProfile> previewProfiles = List.of();

    private UUID selectedLibraryProfileId;
    private UUID selectedPreviewProfileId;

    private UiStatus status = UiStatus.idle();

    private final Map<UUID, Map<UUID, Color>> profileColorsByLibrary = new HashMap<>();


    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public List<SpectralLibrary> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<SpectralLibrary> libraries) {
        List<SpectralLibrary> old = this.libraries;
        this.libraries = libraries == null ? List.of() : List.copyOf(libraries);
        pcs.firePropertyChange(PROP_LIBRARIES, old, this.libraries);
    }

    public Optional<UUID> getActiveLibraryId() {
        return Optional.ofNullable(activeLibraryId);
    }

    public void setActiveLibraryId(UUID id) {
        UUID old = this.activeLibraryId;
        this.activeLibraryId = id;
        pcs.firePropertyChange(PROP_ACTIVE_LIBRARY_ID, old, id);
    }

    public List<SpectralProfile> getLibraryProfiles() {
        return libraryProfiles;
    }

    public void setLibraryProfiles(List<SpectralProfile> profiles) {
        List<SpectralProfile> newList = profiles == null ? List.of() : profiles;
        this.libraryProfiles = newList;
        pcs.firePropertyChange(PROP_LIBRARY_PROFILES, null, newList);
    }

    public List<SpectralProfile> getPreviewProfiles() {
        return previewProfiles;
    }

    public void setPreviewProfiles(List<SpectralProfile> profiles) {
        List<SpectralProfile> old = this.previewProfiles;
        this.previewProfiles = profiles == null ? List.of() : List.copyOf(profiles);
        pcs.firePropertyChange(PROP_PREVIEW_PROFILES, old, this.previewProfiles);
    }

    public void setSelectedLibraryProfileId(UUID id) {
        UUID old = this.selectedLibraryProfileId;
        this.selectedLibraryProfileId = id;
        pcs.firePropertyChange(PROP_SELECTED_LIBRARY_PROFILE_ID, old, id);
    }

    public Optional<UUID> getSelectedPreviewProfileId() {
        return Optional.ofNullable(selectedPreviewProfileId);
    }

    public void setSelectedPreviewProfileId(UUID id) {
        UUID old = this.selectedPreviewProfileId;
        this.selectedPreviewProfileId = id;
        pcs.firePropertyChange(PROP_SELECTED_PREVIEW_PROFILE_ID, old, id);
    }

    public UiStatus getStatus() {
        return status;
    }

    public void setStatus(UiStatus status) {
        UiStatus old = this.status;
        this.status = status == null ? UiStatus.idle() : status;
        pcs.firePropertyChange(PROP_STATUS, old, this.status);
    }


    public Map<UUID, Color> getProfileColors(UUID libraryId) {
        if (libraryId == null) {
            return Map.of();
        }
        Map<UUID, Color> m = profileColorsByLibrary.get(libraryId);
        return (m == null) ? Map.of() : Map.copyOf(m);
    }

    public Map<UUID, Color> getActiveLibraryProfileColors() {
        return getActiveLibraryId().map(this::getProfileColors).orElse(Map.of());
    }

    public void setProfileColors(UUID libraryId, Map<UUID, Color> updates) {
        if (libraryId == null || updates == null || updates.isEmpty()) {
            return;
        }
        Map<UUID, Color> m = profileColorsByLibrary.computeIfAbsent(libraryId, k -> new HashMap<>());
        boolean changed = false;

        for (var e : updates.entrySet()) {
            UUID pid = e.getKey();
            Color c = e.getValue();

            if (pid == null) {
                continue;
            }

            if (c == null) {
                changed |= (m.remove(pid) != null);
            } else {
                Color prev = m.put(pid, c);
                if (!c.equals(prev)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            pcs.firePropertyChange(PROP_PROFILE_COLORS, null, null);
        }
    }
}
